/*
 * EGOPD — debug window.
 *
 * Restored to the game's own UI toolkit (WndTabbed + ui.ScrollingListPane) so it reads as a
 * native window rather than a foreign overlay. See the class comment for why.
 */

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Karma;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Release;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.TearSword;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.food.BlueMilk;
import com.shatteredpixel.shatteredpixeldungeon.items.food.DeathCap;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Earthstar;
import com.shatteredpixel.shatteredpixeldungeon.items.food.GoldenJelly;
import com.shatteredpixel.shatteredpixeldungeon.items.food.JackOLantern;
import com.shatteredpixel.shatteredpixeldungeon.items.food.LichenMushroom;
import com.shatteredpixel.shatteredpixeldungeon.items.food.PixieParasol;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.LogicStudio;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.RectF;
import com.watabou.utils.Reflection;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The in-game debug window: one scrollable list of spawnable entries per tab.
 *
 * <h3>Why this lives in the game (and not in the standalone console library)</h3>
 *
 * <p>This window is built out of the game's <em>own</em> UI toolkit — {@link WndTabbed} for the
 * tab bar and {@link ScrollingListPane} for each list. That is what makes it look and behave
 * exactly like every other window in the game: the same {@code Chrome} nine-patches, the same
 * 18px list stride, the same 9pt title tint, the same tap/scroll handling, and the same
 * click-to-collect behaviour on items.
 *
 * <p>A game-agnostic reimplementation of those widgets inevitably drifts from the original on
 * every one of those axes at once, which is why the generic console was unable to match it. The
 * generic implementation still exists — see the separate {@code :debug-console} module and its
 * drop-in zip — for projects that want a console without adopting this toolkit. This window is
 * the SPD-native one.
 *
 * <p>Class discovery is deliberately dumb and robust: it walks the local classpath (plus, on
 * Android, the APK's {@code classes*.dex} string tables) looking for concrete, no-arg
 * constructible subclasses of each tab's base type. Everything is wrapped in per-entry
 * try/catch — one unconstructible class must never take down a tab.
 */
public class WndDebug extends WndTabbed {

	//window geometry, matching the original debug window
	private static final int WIDTH_P  = 150;
	private static final int HEIGHT_P = 200;
	private static final int WIDTH_L  = 230;
	private static final int HEIGHT_L = 150;

	private static final String PKG_POTIONS   = "com.shatteredpixel.shatteredpixeldungeon.items.potions";
	private static final String PKG_SCROLLS   = "com.shatteredpixel.shatteredpixeldungeon.items.scrolls";
	private static final String PKG_WEAPONS   = "com.shatteredpixel.shatteredpixeldungeon.items.weapon";
	private static final String PKG_MISSILES  = "com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles";
	private static final String PKG_WANDS     = "com.shatteredpixel.shatteredpixeldungeon.items.wands";
	private static final String PKG_ARMOR     = "com.shatteredpixel.shatteredpixeldungeon.items.armor";
	private static final String PKG_ARTIFACTS = "com.shatteredpixel.shatteredpixeldungeon.items.artifacts";
	private static final String PKG_TRINKETS  = "com.shatteredpixel.shatteredpixeldungeon.items.trinkets";
	private static final String PKG_ITEMS_ROOT= "com.shatteredpixel.shatteredpixeldungeon.items";
	private static final String PKG_BUFFS     = "com.shatteredpixel.shatteredpixeldungeon.actors.buffs";
	private static final String PKG_MOBS      = "com.shatteredpixel.shatteredpixeldungeon.actors.mobs";

	private ItemTab potionsTab;
	private ItemTab scrollsTab;
	private ItemTab weaponsTab;
	private ItemTab rangedTab;
	private ItemTab armorTab;
	private ItemTab artifactsTab;
	private ItemTab trinketsTab;
	private ItemTab miscTab;
	private BuffTab buffsTab;
	private MobTab  mobsTab;

	/** Android: class names harvested from the APK's dex files. Cached (parsing is not cheap). */
	private static ArrayList<String> dexNamesCache = null;

	public WndDebug() {
		super();

		//ten tabs do not fit on one row at this window width
		tabRows = 2;

		int w = PixelScene.landscape() ? WIDTH_L : WIDTH_P;
		int h = PixelScene.landscape() ? HEIGHT_L : HEIGHT_P;
		resize( w, h );

		potionsTab   = new ItemTab( Potion.class,      PKG_POTIONS );
		scrollsTab   = new ItemTab( Scroll.class,      PKG_SCROLLS );
		//LogicStudio extends Weapon directly, so the MeleeWeapon scan misses it
		weaponsTab   = new ItemTab( MeleeWeapon.class, PKG_WEAPONS, LogicStudio.class );
		//ranged: wands plus thrown weapons, in one tab
		rangedTab    = new ItemTab( Item.class,        new String[]{ PKG_WANDS, PKG_MISSILES } );
		armorTab     = new ItemTab( Armor.class,       PKG_ARMOR );
		artifactsTab = new ItemTab( Artifact.class,    PKG_ARTIFACTS );
		trinketsTab  = new ItemTab( Trinket.class,     PKG_TRINKETS );
		//misc: a shallow scan of the items root, for the few items with no sub-package
		//(Tengu's mask, King's crown, the amulet, ...)
		//the seven edible mushrooms live in items.food (a sub-package), so a shallow scan
		//cannot reach them -- list them explicitly, same trick as LogicStudio on the weapons tab.
		miscTab      = new ItemTab( Item.class,        PKG_ITEMS_ROOT, true,
				JackOLantern.class, Earthstar.class, LichenMushroom.class, DeathCap.class,
				BlueMilk.class, GoldenJelly.class, PixieParasol.class );
		buffsTab     = new BuffTab();
		mobsTab      = new MobTab();

		add( potionsTab );   potionsTab.setRect(   0, 0, w, h );
		add( scrollsTab );   scrollsTab.setRect(   0, 0, w, h );
		add( weaponsTab );   weaponsTab.setRect(   0, 0, w, h );
		add( rangedTab );    rangedTab.setRect(    0, 0, w, h );
		add( armorTab );     armorTab.setRect(     0, 0, w, h );
		add( artifactsTab ); artifactsTab.setRect( 0, 0, w, h );
		add( trinketsTab );  trinketsTab.setRect(  0, 0, w, h );
		add( miscTab );      miscTab.setRect(      0, 0, w, h );
		add( buffsTab );     buffsTab.setRect(     0, 0, w, h );
		add( mobsTab );      mobsTab.setRect(      0, 0, w, h );

		Tab[] tabs = {
				new WndTabbed.LabeledTab( "药水" ) {
					@Override protected void select( boolean value ) {
						super.select( value );
						potionsTab.visible = potionsTab.active = value;
						if (value) potionsTab.refresh();
					}
				},
				new WndTabbed.LabeledTab( "卷轴" ) {
					@Override protected void select( boolean value ) {
						super.select( value );
						scrollsTab.visible = scrollsTab.active = value;
						if (value) scrollsTab.refresh();
					}
				},
				new WndTabbed.LabeledTab( "武器" ) {
					@Override protected void select( boolean value ) {
						super.select( value );
						weaponsTab.visible = weaponsTab.active = value;
						if (value) weaponsTab.refresh();
					}
				},
				new WndTabbed.LabeledTab( "远程" ) {
					@Override protected void select( boolean value ) {
						super.select( value );
						rangedTab.visible = rangedTab.active = value;
						if (value) rangedTab.refresh();
					}
				},
				new WndTabbed.LabeledTab( "防具" ) {
					@Override protected void select( boolean value ) {
						super.select( value );
						armorTab.visible = armorTab.active = value;
						if (value) armorTab.refresh();
					}
				},
				new WndTabbed.LabeledTab( "神器" ) {
					@Override protected void select( boolean value ) {
						super.select( value );
						artifactsTab.visible = artifactsTab.active = value;
						if (value) artifactsTab.refresh();
					}
				},
				new WndTabbed.LabeledTab( "饰品" ) {
					@Override protected void select( boolean value ) {
						super.select( value );
						trinketsTab.visible = trinketsTab.active = value;
						if (value) trinketsTab.refresh();
					}
				},
				new WndTabbed.LabeledTab( "杂项" ) {
					@Override protected void select( boolean value ) {
						super.select( value );
						miscTab.visible = miscTab.active = value;
						if (value) miscTab.refresh();
					}
				},
				new WndTabbed.LabeledTab( "Buff" ) {
					@Override protected void select( boolean value ) {
						super.select( value );
						buffsTab.visible = buffsTab.active = value;
						if (value) buffsTab.refresh();
					}
				},
				new WndTabbed.LabeledTab( "怪物" ) {
					@Override protected void select( boolean value ) {
						super.select( value );
						mobsTab.visible = mobsTab.active = value;
						if (value) mobsTab.refresh();
					}
				},
		};

		for (Tab tab : tabs) {
			add( tab );
		}

		layoutTabs();
		select( tabs[0] );
	}

	//================================================================================
	//   spawning
	//================================================================================

	/**
	 * Adds one instance of the item to the hero's inventory, or drops it at the hero's feet if
	 * the pack is full. This is the click behaviour for every item tab.
	 */
	private static void spawnItem( Class<? extends Item> clazz ) {
		if (Dungeon.hero == null || Dungeon.level == null) return;

		try {
			Item item = Reflection.newInstance( clazz );
			if (item == null) return;

			if (!item.collect()) {
				Dungeon.level.drop( item, Dungeon.hero.pos );
			}
			GLog.i( "已生成: " + item.name() );

		} catch (Throwable t) {
			GLog.i( "生成失败: " + clazz.getSimpleName() );
		}
	}

	/** Mobs need a map tile, so the window hides and the player clicks where to place it. */
	private static void spawnMob( final Class<? extends Mob> clazz ) {
		GameScene.selectCell( new CellSelector.Listener() {
			@Override
			public void onSelect( Integer cell ) {
				if (cell != null) spawnMobAt( clazz, cell );
			}

			@Override
			public void onRightClick( Integer cell ) {
				if (cell != null) spawnMobAt( clazz, cell );
			}

			@Override
			public String prompt() {
				return "点击地图放置「" + mobDisplayName( clazz ) + "」（右键同样放置）";
			}
		} );
	}

	private static String mobDisplayName( Class<? extends Mob> clazz ) {
		Mob mob = Reflection.newInstance( clazz );
		return mob != null ? Messages.titleCase( mob.name() ) : clazz.getSimpleName();
	}

	private static void spawnMobAt( Class<? extends Mob> clazz, int cell ) {
		if (Dungeon.level == null) return;
		try {
			Mob mob = Reflection.newInstance( clazz );
			if (mob == null) return;

			mob.pos = findSpawnCell( cell );
			mob.state = mob.WANDERING;
			GameScene.add( mob, 0 );
			GLog.i( "已生成: " + mob.name() );

		} catch (Throwable t) {
			GLog.i( "生成失败: " + clazz.getSimpleName() );
		}
	}

	/** Prefers the clicked tile; falls back to an adjacent walkable, unoccupied one. */
	private static int findSpawnCell( int cell ) {
		if (Dungeon.level == null) return cell;

		if (isFree( cell )) return cell;

		int w = Dungeon.level.width();
		int[] offs = { -w - 1, -w, -w + 1, -1, +1, +w - 1, +w, +w + 1 };
		for (int off : offs) {
			int c = cell + off;
			if (isFree( c )) return c;
		}
		return cell;
	}

	private static boolean isFree( int cell ) {
		return Dungeon.level != null
				&& cell >= 0 && cell < Dungeon.level.length()
				&& Dungeon.level.passable[cell]
				&& Actor.findChar( cell ) == null;
	}

	/** Applies one buff to the hero, with a little convenience for the stackable ones. */
	private static void spawnBuff( Class<? extends Buff> clazz ) {
		if (Dungeon.hero == null) return;

		try {
			//FlavourBuff expires instantly without a duration; the cast is needed because the
			//duration overload is bounded by FlavourBuff, which the wildcard cannot prove
			if (FlavourBuff.class.isAssignableFrom( clazz )) {
				Buff.affect( Dungeon.hero, (Class<? extends FlavourBuff>) clazz, 30f );
			} else {
				Buff.affect( Dungeon.hero, clazz );
			}

			//a few stackable buffs are tedious to build up by hand, so one click adds a layer
			if (clazz == Release.class) {
				Release release = Dungeon.hero.buff( Release.class );
				if (release != null) release.debugGainStack();

			} else if (clazz == Karma.class) {
				Karma karma = Dungeon.hero.buff( Karma.class );
				if (karma != null) karma.addKarma( 10 );

			} else if (clazz == TearSword.class) {
				TearSword.addStacks( Dungeon.hero, 1 );
			}

			Buff sample = Reflection.newInstance( clazz );
			GLog.i( "已附加: " + (sample != null ? Messages.titleCase( sample.name() ) : clazz.getSimpleName()) );

		} catch (Throwable t) {
			GLog.i( "附加失败: " + clazz.getSimpleName() );
		}
	}

	/**
	 * Trims an oversized icon down to 17px and centres the crop.
	 *
	 * <p>Mob and buff sprites are drawn larger than a list row, so showing them uncropped would
	 * blow the row's 16px icon column out of alignment.
	 */
	private static void clipIcon( Image image ) {
		if (image.width() >= 17 || image.height() >= 17) {
			RectF frame = image.frame();
			float dw = frame.width() * (1f - 17f / image.width());
			if (dw > 0) {
				frame.left  += dw / 2f;
				frame.right -= dw / 2f;
			}
			float dh = frame.height() * (1f - 17f / image.height());
			if (dh > 0) {
				frame.top    += dh / 2f;
				frame.bottom -= dh / 2f;
			}
			image.frame( frame );
		}
		PixelScene.align( image );
	}

	//================================================================================
	//   class discovery
	//================================================================================

	private static ArrayList<Class<?>> findClasses( String pkg, Class<?> base ) {
		return findClasses( pkg, base, false );
	}

	/** Shallow scan: only classes directly in {@code pkg}, sub-packages excluded. */
	private static ArrayList<Class<?>> findClassesShallow( String pkg, Class<?> base ) {
		return findClasses( pkg, base, true );
	}

	/**
	 * Every concrete, no-arg-constructible subclass of {@code base} found under {@code pkg}.
	 *
	 * <p>Scans the local classpath (directories and jars) and, on Android, the APK's dex string
	 * tables — on-device the app's classes live in dex, not on the classpath, so without that
	 * second source the window would come up empty on a phone.
	 *
	 * @param shallow when true, stop at the package boundary instead of recursing
	 */
	private static ArrayList<Class<?>> findClasses( String pkg, Class<?> base, boolean shallow ) {
		String path = pkg.replace( '.', '/' );
		Set<String> names = new LinkedHashSet<>();

		for (String entry : System.getProperty( "java.class.path" ).split( File.pathSeparator )) {
			if (entry.isEmpty()) continue;
			File file = new File( entry );
			try {
				if (file.isDirectory()) {
					collectDir( file, file, path, names, shallow );
				} else if (file.getName().endsWith( ".jar" ) || file.getName().endsWith( ".zip" )) {
					collectJar( file, path, names, shallow );
				}
			} catch (Exception ignored) {
				//an unreadable classpath entry simply contributes nothing
			}
		}

		if (DeviceCompat.isAndroid()) {
			String prefix = pkg + ".";
			for (String name : androidDexClassNames()) {
				if (!name.startsWith( prefix ) || name.contains( "$" )) continue;
				String rest = name.substring( prefix.length() );
				if (shallow && rest.indexOf( '.' ) >= 0) continue;
				names.add( name );
			}
		}

		ArrayList<Class<?>> result = new ArrayList<>();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();

		for (String name : names) {
			try {
				Class<?> c = Class.forName( name, false, loader );
				if (!base.isAssignableFrom( c ) || c.isInterface() || Modifier.isAbstract( c.getModifiers() )) continue;
				//inner classes are not independently spawnable
				if (c.getEnclosingClass() != null) continue;
				try {
					c.getConstructor();
				} catch (NoSuchMethodException e) {
					continue;
				}
				result.add( c );
			} catch (Throwable ignored) {
				//class present in the listing but not loadable here; skip it
			}
		}

		Collections.sort( result, new Comparator<Class<?>>() {
			@Override
			public int compare( Class<?> a, Class<?> b ) {
				return a.getSimpleName().compareToIgnoreCase( b.getSimpleName() );
			}
		} );
		return result;
	}

	private static void collectDir( File root, File dir, String path, Set<String> out, boolean shallow ) {
		File[] files = dir.listFiles();
		if (files == null) return;

		String rel = dir.equals( root ) ? ""
				: dir.getAbsolutePath().substring( root.getAbsolutePath().length() ).replace( File.separatorChar, '/' );
		//once we are standing in the package itself, sub-directories are out of scope
		boolean inPkg = rel.equals( "/" + path );

		for (File f : files) {
			if (f.isDirectory()) {
				if (shallow && inPkg) continue;
				collectDir( root, f, path, out, shallow );
				continue;
			}
			if (!f.getName().endsWith( ".class" ) || f.getName().contains( "$" ) || f.getName().equals( "module-info.class" )) continue;

			String name = f.getAbsolutePath().substring( root.getAbsolutePath().length() + 1 ).replace( File.separatorChar, '/' );
			if (!name.startsWith( path + "/" )) continue;
			if (shallow && name.indexOf( '/', path.length() + 1 ) >= 0) continue;

			out.add( name.substring( 0, name.length() - 6 ).replace( '/', '.' ) );
		}
	}

	private static void collectJar( File jar, String path, Set<String> out, boolean shallow ) {
		try (JarFile jf = new JarFile( jar )) {
			Enumeration<JarEntry> entries = jf.entries();
			while (entries.hasMoreElements()) {
				String name = entries.nextElement().getName();
				if (!name.endsWith( ".class" ) || name.contains( "$" ) || name.endsWith( "module-info.class" )) continue;
				if (!name.startsWith( path + "/" )) continue;
				if (shallow && name.indexOf( '/', path.length() + 1 ) >= 0) continue;

				out.add( name.substring( 0, name.length() - 6 ).replace( '/', '.' ) );
			}
		} catch (Exception ignored) {
			//a corrupt jar on the classpath contributes nothing
		}
	}

	/** Class names from the running APK's dex files. Android only; cached after the first call. */
	private static ArrayList<String> androidDexClassNames() {
		if (dexNamesCache != null) return dexNamesCache;

		ArrayList<String> names = new ArrayList<>();
		try {
			String apk = androidApkPath();
			if (apk != null) {
				try (ZipFile zip = new ZipFile( apk )) {
					Enumeration<? extends ZipEntry> entries = zip.entries();
					while (entries.hasMoreElements()) {
						ZipEntry entry = entries.nextElement();
						String name = entry.getName();
						if (!name.startsWith( "classes" ) || !name.endsWith( ".dex" )) continue;
						collectDexClassNames( readAll( zip.getInputStream( entry ) ), names );
					}
				}
			}
		} catch (Throwable ignored) {
			//fall through with whatever was collected
		}

		dexNamesCache = names;
		return names;
	}

	/** Locates the APK: first a {@code .apk} on the classpath, then the Android app's sourceDir. */
	private static String androidApkPath() {
		for (String entry : System.getProperty( "java.class.path" ).split( File.pathSeparator )) {
			if (entry.endsWith( ".apk" ) && new File( entry ).exists()) return entry;
		}

		try {
			Application app = Gdx.app;
			if (app == null) return null;

			Method getInfo = app.getClass().getMethod( "getApplicationInfo" );
			Object info = getInfo.invoke( app );
			Object src = info.getClass().getField( "sourceDir" ).get( info );
			return src == null ? null : src.toString();
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * Harvests class names from one dex file's string table.
	 *
	 * <p>Rather than a full dex parser we walk the type-id table and read the "L...;" descriptors
	 * out of the string pool, which is all the console needs. Every offset is bounds-checked
	 * against the real file length — a malformed or truncated dex must not throw here, and the
	 * caller's only failure mode should be "found fewer classes".
	 */
	private static void collectDexClassNames( byte[] dex, ArrayList<String> out ) {
		if (dex == null || dex.length < 112) return;
		//magic: "dex\n"
		if (dex[0] != 'd' || dex[1] != 'e' || dex[2] != 'x' || dex[3] != 10) return;

		int stringIdsSize = leInt( dex, 56 );
		int stringIdsOff  = leInt( dex, 60 );
		int typeIdsSize   = leInt( dex, 64 );
		int typeIdsOff    = leInt( dex, 68 );
		int classDefsSize = leInt( dex, 96 );
		int classDefsOff  = leInt( dex, 100 );

		if (stringIdsSize <= 0 || typeIdsSize <= 0 || classDefsSize <= 0) return;
		if ((long) classDefsOff + (long) classDefsSize * 32 > dex.length) return;
		if ((long) typeIdsOff + (long) typeIdsSize * 4 > dex.length) return;
		if ((long) stringIdsOff + (long) stringIdsSize * 4 > dex.length) return;

		for (int i = 0; i < classDefsSize; i++) {
			try {
				int typeIdx = leInt( dex, classDefsOff + i * 32 );
				if (typeIdx < 0 || typeIdx >= typeIdsSize) continue;

				int stringIdx = leInt( dex, typeIdsOff + typeIdx * 4 );
				if (stringIdx < 0 || stringIdx >= stringIdsSize) continue;

				String descriptor = readDexString( dex, leInt( dex, stringIdsOff + stringIdx * 4 ) );
				if (descriptor == null || descriptor.length() < 2) continue;
				if (descriptor.charAt( 0 ) != 'L' || !descriptor.endsWith( ";" )) continue;

				String name = descriptor.substring( 1, descriptor.length() - 1 ).replace( '/', '.' );
				if (name.indexOf( '$' ) >= 0 || out.contains( name )) continue;
				out.add( name );
			} catch (Throwable ignored) {
				//one bad entry is not worth abandoning the whole table
			}
		}
	}

	/** Reads a length-prefixed UTF-8 string from a dex string-data item. */
	private static String readDexString( byte[] dex, int offset ) {
		if (offset < 0 || offset >= dex.length) return null;

		int p = offset;
		//ULEB128 length prefix
		while (true) {
			if (p >= dex.length) return null;
			if ((dex[p++] & 0x80) == 0) break;
		}

		int start = p;
		while (p < dex.length && dex[p] != 0) p++;
		if (p == start) return "";

		return new String( dex, start, p - start, StandardCharsets.UTF_8 );
	}

	private static int leInt( byte[] b, int i ) {
		return (b[i] & 0xFF)
				| ((b[i + 1] & 0xFF) << 8)
				| ((b[i + 2] & 0xFF) << 16)
				| ((b[i + 3] & 0xFF) << 24);
	}

	private static byte[] readAll( InputStream in ) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int n;
		while ((n = in.read( buf )) != -1) {
			out.write( buf, 0, n );
		}
		return out.toByteArray();
	}

	//================================================================================
	//   tabs
	//================================================================================

	/**
	 * Shared base for a tab's content: a {@link ScrollingListPane} plus lazy, once-only building.
	 *
	 * <p>The list is created in {@link #createChildren()}, which {@link Component}'s constructor
	 * calls for us — safe here because {@code list} is assigned by this class's own
	 * {@code createChildren()}, not by a subclass field initializer.
	 */
	private abstract class DebugTab extends Component {

		protected ScrollingListPane list;
		private boolean built = false;

		abstract void build();

		/** Builds the tab's rows on first open. A broken entry must not take down the window. */
		void refresh() {
			if (!built) {
				built = true;
				try {
					build();
				} catch (Throwable t) {
					Game.reportException( t );
				}
			}
		}

		@Override
		protected void createChildren() {
			list = new ScrollingListPane();
			add( list );
		}

		@Override
		protected void layout() {
			super.layout();
			list.setRect( x, y, width, height );
		}
	}

	/** One tab listing spawnable items. Clicking a row adds that item to the hero. */
	private class ItemTab extends DebugTab {

		private final Class<? extends Item> base;
		private final String[] pkgs;
		private final Class<? extends Item>[] extra;
		private final boolean shallow;

		@SafeVarargs
		ItemTab( Class<? extends Item> base, String pkg, Class<? extends Item>... extra ) {
			this( base, new String[]{ pkg }, false, extra );
		}

		@SafeVarargs
		ItemTab( Class<? extends Item> base, String pkg, boolean shallow, Class<? extends Item>... extra ) {
			this( base, new String[]{ pkg }, shallow, extra );
		}

		@SafeVarargs
		ItemTab( Class<? extends Item> base, String[] pkgs, Class<? extends Item>... extra ) {
			this( base, pkgs, false, extra );
		}

		@SafeVarargs
		ItemTab( Class<? extends Item> base, String[] pkgs, boolean shallow, Class<? extends Item>... extra ) {
			super();
			this.base = base;
			this.pkgs = pkgs;
			this.shallow = shallow;
			this.extra = extra;
		}

		@Override
		void build() {
			ArrayList<Class<?>> found = new ArrayList<>();
			for (String pkg : pkgs) {
				for (Class<?> c : (shallow ? findClassesShallow( pkg, base ) : findClasses( pkg, base ))) {
					if (!found.contains( c )) found.add( c );
				}
			}
			for (Class<? extends Item> c : extra) {
				if (!found.contains( c )) found.add( c );
			}

			for (Class<?> raw : found) {
				try {
					final Class<? extends Item> clazz = raw.asSubclass( Item.class );

					Item item = Reflection.newInstance( clazz );
					if (item == null) continue;

					ItemSprite sprite = new ItemSprite( item );
					final String name = Messages.titleCase( item.name() );

					list.addItem( new ScrollingListPane.ListItem( sprite, null, name ) {
						@Override
						public boolean onClick( float x, float y ) {
							if (inside( x, y )) {
								spawnItem( clazz );
								return true;
							}
							return false;
						}
					} );

				} catch (Throwable ignored) {
					//this class cannot be shown; the rest of the tab still builds
				}
			}
		}
	}

	/** One tab listing every buff. Clicking a row applies it to the hero. */
	private class BuffTab extends DebugTab {

		@Override
		void build() {
			for (Class<?> raw : findClasses( PKG_BUFFS, Buff.class )) {
				try {
					final Class<? extends Buff> clazz = raw.asSubclass( Buff.class );

					Buff buff = Reflection.newInstance( clazz );
					if (buff == null) continue;

					String name = Messages.titleCase( buff.name() );

					BuffIcon icon = null;
					try {
						icon = new BuffIcon( buff, false );
					} catch (Throwable ignored) {
						//a buff with no icon still gets a (text-only) row
					}
					if (icon != null) {
						clipIcon( icon );
					}

					list.addItem( new ScrollingListPane.ListItem( icon, null, name ) {
						@Override
						public boolean onClick( float x, float y ) {
							if (inside( x, y )) {
								spawnBuff( clazz );
								return true;
							}
							return false;
						}
					} );

				} catch (Throwable ignored) {
					//skip this buff, keep the tab
				}
			}
		}
	}

	/** One tab listing every mob. Clicking a row hides the window and asks for a target tile. */
	private class MobTab extends DebugTab {

		@Override
		void build() {
			for (Class<?> raw : findClasses( PKG_MOBS, Mob.class )) {
				try {
					final Class<? extends Mob> clazz = raw.asSubclass( Mob.class );

					Mob mob = Reflection.newInstance( clazz );
					if (mob == null) continue;

					Image icon;
					String name;
					try {
						//spriteClass is null for mobs that are only given one once placed (VaultMob,
						//DemonSpawner, ...), and sprite() then throws from inside libGDX
						CharSpriteShim sprite = new CharSpriteShim( mob );
						icon = sprite.image();
						name = Messages.titleCase( mob.name() );
					} catch (Throwable t) {
						icon = new ItemSprite( ItemSpriteSheet.MOB_HOLDER );
						name = raw.getSimpleName();
					}

					clipIcon( icon );

					list.addItem( new ScrollingListPane.ListItem( icon, null, name ) {
						@Override
						public boolean onClick( float x, float y ) {
							if (inside( x, y )) {
								hide();
								spawnMob( clazz );
								return true;
							}
							return false;
						}
					} );

				} catch (Throwable ignored) {
					//skip this mob, keep the tab
				}
			}
		}
	}

	/**
	 * Small helper that turns a mob into a row icon, tolerating mobs whose sprite is not yet
	 * available. Kept as a nested type so the tab body stays readable.
	 */
	private static class CharSpriteShim {

		private final Image image;

		CharSpriteShim( Mob mob ) {
			com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite s = mob.sprite();
			if (s == null) throw new IllegalStateException( "mob has no sprite yet" );
			s.idle();
			image = new Image( s );
		}

		Image image() {
			return image;
		}
	}

	/** Exposed for regression checks: how many tabs were registered. */
	public int debugTabCount() {
		return tabs == null ? 0 : tabs.size();
	}
}
