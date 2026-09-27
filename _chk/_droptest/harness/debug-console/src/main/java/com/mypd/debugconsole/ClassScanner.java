/*
 * EGOPD Debug Console — standalone debug console system
 */

package com.mypd.debugconsole;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Finds concrete, instantiable subclasses of a base type under a set of packages, at runtime.
 *
 * <p>Three sources are supported, and results are merged and de-duplicated:
 * <ul>
 *   <li><b>directories</b> on {@code java.class.path} (desktop: compiled {@code .class} trees)</li>
 *   <li><b>jars / zips</b> on {@code java.class.path}</li>
 *   <li><b>dex</b> entries inside the running APK (Android, where no {@code .class} files exist)</li>
 * </ul>
 *
 * <p>Everything here is engine-agnostic; the only environment probe is
 * {@link com.watabou.utils.DeviceCompat#isAndroid()} to decide whether the dex fallback is needed.
 *
 * <p>Filtering applied to every candidate: assignable to the base type, not an interface, not
 * abstract, not a nested/inner class, and possessing a public no-arg constructor (so the caller
 * can actually instantiate it).
 */
public final class ClassScanner {

	private ClassScanner() {
	}

	/** Convenience: recursive scan for a single package. */
	public static List<Class<?>> find(String pkg, Class<?> base) {
		return find(new String[]{pkg}, base);
	}

	/** Convenience: recursive scan, keeping declaration order of the package array. */
	public static List<Class<?>> find(String[] pkgs, Class<?> base) {
		return find(pkgs, base, false);
	}

	/**
	 * Scans the given packages.
	 *
	 * @param base    required supertype (or interface) of every returned class
	 * @param shallow when true, only classes sitting <em>directly</em> in the named package are
	 *                returned; sub-packages are not descended into
	 */
	public static List<Class<?>> find(String[] pkgs, Class<?> base, boolean shallow) {
		Set<String> names = new LinkedHashSet<>();

		if (pkgs != null) {
			for (String pkg : pkgs) {
				if (pkg == null || pkg.isEmpty()) continue;
				collectFromClasspath(pkg, names, shallow);
				collectFromDex(pkg, names, shallow);
			}
		}

		return resolve(names, base);
	}

	/**
	 * Resolves already-known binary class names to instantiable subclasses of {@code base}.
	 * Useful when the host supplies explicit class names instead of relying on scanning.
	 */
	public static List<Class<?>> resolve(Iterable<String> binaryNames, Class<?> base) {
		List<Class<?>> result = new ArrayList<>();
		ClassLoader loader = ClassScanner.class.getClassLoader();
		if (loader == null) loader = ClassLoader.getSystemClassLoader();

		for (String name : binaryNames) {
			try {
				Class<?> c = Class.forName(name, false, loader);
				if (isUsable(c, base)) result.add(c);
			} catch (Throwable ignored) {
				//skip classes that can't be linked (missing optional dependencies, etc.)
			}
		}

		Collections.sort(result, new Comparator<Class<?>>() {
			@Override
			public int compare(Class<?> a, Class<?> b) {
				return a.getSimpleName().compareToIgnoreCase(b.getSimpleName());
			}
		});
		return result;
	}

	/** True if {@code c} is a concrete, instantiable, non-nested subclass of {@code base}. */
	public static boolean isUsable(Class<?> c, Class<?> base) {
		if (c == null || base == null) return false;
		if (!base.isAssignableFrom(c)) return false;
		if (c.isInterface()) return false;
		if (Modifier.isAbstract(c.getModifiers())) return false;
		if (c.getEnclosingClass() != null) return false;
		try {
			c.getConstructor();
		} catch (NoSuchMethodException e) {
			return false;
		}
		return true;
	}

	//================================================================================
	//   classpath (desktop / JVM)
	//================================================================================

	private static void collectFromClasspath(String pkg, Set<String> names, boolean shallow) {
		String pkgPath = pkg.replace('.', '/');
		String[] cp = System.getProperty("java.class.path", "").split(File.pathSeparator);

		for (String entry : cp) {
			if (entry == null || entry.isEmpty()) continue;
			File f = new File(entry);
			try {
				if (f.isDirectory()) {
					collectDir(f, f, pkgPath, names, shallow);
				} else if (f.getName().endsWith(".jar") || f.getName().endsWith(".zip")) {
					collectJar(f, pkgPath, names, shallow);
				}
			} catch (Exception ignored) {
				//skip unreadable classpath entry
			}
		}
	}

	private static void collectDir(File root, File dir, String pkgPath, Set<String> names, boolean shallow) {
		File[] files = dir.listFiles();
		if (files == null) return;

		//path of this directory relative to the classpath root ("" for the root itself)
		String dirRel = dir.equals(root) ? ""
				: dir.getAbsolutePath().substring(root.getAbsolutePath().length())
						.replace(File.separatorChar, '/');
		boolean atPkgDir = dirRel.equals("/" + pkgPath);

		for (File f : files) {
			if (f.isDirectory()) {
				//shallow: stop descending once we're inside the target package
				if (shallow && atPkgDir) continue;
				collectDir(root, f, pkgPath, names, shallow);
			} else if (f.getName().endsWith(".class")
					&& !f.getName().contains("$")
					&& !f.getName().equals("module-info.class")) {
				String rel = f.getAbsolutePath().substring(root.getAbsolutePath().length() + 1)
						.replace(File.separatorChar, '/');
				if (!rel.startsWith(pkgPath + "/")) continue;
				//shallow: class must sit directly in pkgPath (no further separators)
				if (shallow && rel.indexOf('/', pkgPath.length() + 1) >= 0) continue;
				names.add(rel.substring(0, rel.length() - 6).replace('/', '.'));
			}
		}
	}

	private static void collectJar(File jar, String pkgPath, Set<String> names, boolean shallow) {
		try (JarFile jf = new JarFile(jar)) {
			Enumeration<JarEntry> en = jf.entries();
			while (en.hasMoreElements()) {
				String name = en.nextElement().getName();
				if (!name.endsWith(".class")
						|| name.contains("$")
						|| name.endsWith("module-info.class")
						|| !name.startsWith(pkgPath + "/")) {
					continue;
				}
				if (shallow && name.indexOf('/', pkgPath.length() + 1) >= 0) continue;
				names.add(name.substring(0, name.length() - 6).replace('/', '.'));
			}
		} catch (Exception ignored) {
			//not a readable jar
		}
	}

	//================================================================================
	//   dex fallback (Android: all game code lives in classes*.dex inside the APK)
	//================================================================================

	private static ArrayList<String> dexNamesCache;

	/** Clears the cached dex class list. Call if the APK can change at runtime (it normally can't). */
	public static synchronized void clearDexCache() {
		dexNamesCache = null;
	}

	/** Reads every class name declared in the running APK's dex files. Cached after first call. */
	public static synchronized List<String> dexClassNames() {
		if (dexNamesCache != null) return dexNamesCache;

		ArrayList<String> result = new ArrayList<>();
		try {
			String apk = apkPath();
			if (apk != null) {
				try (ZipFile zip = new ZipFile(apk)) {
					Enumeration<? extends ZipEntry> en = zip.entries();
					while (en.hasMoreElements()) {
						ZipEntry e = en.nextElement();
						String n = e.getName();
						if (n.startsWith("classes") && n.endsWith(".dex")) {
							collectDexClassNames(readAll(zip.getInputStream(e)), result);
						}
					}
				}
			}
		} catch (Throwable ignored) {
			//unreadable APK: return whatever we have (providers fall back to explicit classes)
		}

		dexNamesCache = result;
		return result;
	}

	private static void collectFromDex(String pkg, Set<String> names, boolean shallow) {
		if (!isAndroid()) return;

		String prefix = pkg + ".";
		for (String dotted : dexClassNames()) {
			if (!dotted.startsWith(prefix)) continue;
			if (dotted.contains("$")) continue; //nested/anonymous, matching the classpath scan
			String rest = dotted.substring(prefix.length());
			if (shallow && rest.indexOf('.') >= 0) continue;
			names.add(dotted);
		}
	}

	/**
	 * Locates the installed APK. Tries {@code java.class.path} first, then asks the libGDX
	 * application object for its {@code sourceDir} (reflection, so no Android SDK dependency).
	 */
	public static String apkPath() {
		String[] cp = System.getProperty("java.class.path", "").split(File.pathSeparator);
		for (String entry : cp) {
			if (entry != null && entry.endsWith(".apk") && new File(entry).exists()) return entry;
		}
		try {
			Object app = com.badlogic.gdx.Gdx.app;
			if (app == null) return null;
			java.lang.reflect.Method m = app.getClass().getMethod("getApplicationInfo");
			Object ai = m.invoke(app);
			java.lang.reflect.Field f = ai.getClass().getField("sourceDir");
			Object v = f.get(ai);
			return v == null ? null : v.toString();
		} catch (Throwable t) {
			return null;
		}
	}

	private static boolean isAndroid() {
		try {
			return com.watabou.utils.DeviceCompat.isAndroid();
		} catch (Throwable t) {
			//DeviceCompat unavailable: infer from the VM
			return System.getProperty("java.vm.name", "").toLowerCase().contains("dalvik");
		}
	}

	//================================================================================
	//   minimal dex reader
	//================================================================================

	private static void collectDexClassNames(byte[] dex, ArrayList<String> out) {
		if (dex == null || dex.length < 0x70) return;
		//magic: "dex\n"
		if (!(dex[0] == 'd' && dex[1] == 'e' && dex[2] == 'x' && dex[3] == '\n')) return;

		int stringIdsSize = leInt(dex, 0x38);
		int stringIdsOff  = leInt(dex, 0x3C);
		int typeIdsSize   = leInt(dex, 0x40);
		int typeIdsOff    = leInt(dex, 0x44);
		int classDefsSize = leInt(dex, 0x60);
		int classDefsOff  = leInt(dex, 0x64);

		if (stringIdsOff <= 0 || typeIdsOff <= 0 || classDefsOff <= 0) return;
		if (classDefsOff + classDefsSize * 32L > dex.length) return;
		if (typeIdsOff + typeIdsSize * 4L > dex.length) return;
		if (stringIdsOff + stringIdsSize * 4L > dex.length) return;

		for (int i = 0; i < classDefsSize; i++) {
			try {
				int classIdx = leInt(dex, classDefsOff + i * 32);
				if (classIdx < 0 || classIdx >= typeIdsSize) continue;
				int descIdx = leInt(dex, typeIdsOff + classIdx * 4);
				if (descIdx < 0 || descIdx >= stringIdsSize) continue;
				int strOff = leInt(dex, stringIdsOff + descIdx * 4);
				String desc = readDexString(dex, strOff);
				if (desc == null) continue;
				//descriptor looks like "Lcom/foo/Bar;"; skip arrays and primitives
				if (desc.length() < 2 || desc.charAt(0) != 'L' || !desc.endsWith(";")) continue;
				String name = desc.substring(1, desc.length() - 1).replace('/', '.');
				if (name.indexOf('$') >= 0) continue; //inner/anonymous
				if (!out.contains(name)) out.add(name);
			} catch (Throwable ignored) {
				//malformed entry: skip
			}
		}
	}

	/** Reads a dex string: skip the uleb128 utf16 length prefix, then read up to the NUL. */
	private static String readDexString(byte[] dex, int off) {
		if (off < 0 || off >= dex.length) return null;
		int p = off;
		int b;
		do {
			if (p >= dex.length) return null;
			b = dex[p++] & 0xFF;
		} while ((b & 0x80) != 0);

		int start = p;
		while (p < dex.length && dex[p] != 0) p++;
		if (p == start) return "";
		return new String(dex, start, p - start, StandardCharsets.UTF_8);
	}

	private static int leInt(byte[] b, int off) {
		return (b[off] & 0xFF)
				| ((b[off + 1] & 0xFF) << 8)
				| ((b[off + 2] & 0xFF) << 16)
				| ((b[off + 3] & 0xFF) << 24);
	}

	private static byte[] readAll(InputStream in) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		byte[] tmp = new byte[8192];
		int read;
		while ((read = in.read(tmp)) != -1) buf.write(tmp, 0, read);
		return buf.toByteArray();
	}
}
