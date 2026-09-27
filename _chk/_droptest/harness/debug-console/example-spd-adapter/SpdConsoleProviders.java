/*
 * EGOPD — SPD-side adapter for the standalone debug console library.
 */

package com.shatteredpixel.shatteredpixeldungeon.debug;

import com.mypd.debugconsole.ClassScanner;
import com.mypd.debugconsole.spi.ConsoleProvider;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.LogicStudio;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;

import java.util.ArrayList;
import java.util.List;

/**
 * One {@link ConsoleProvider} per console tab, each describing which package to scan and which
 * base type the results must extend.
 *
 * <p>Kept separate from {@link SpdConsoleHost} so the "what to show" concern (this file) stays
 * distinct from the "how to spawn it" concern (the host).
 */
public final class SpdConsoleProviders {

	private static final String PKG = "com.shatteredpixel.shatteredpixeldungeon.";

	private SpdConsoleProviders() {
	}

		/** A provider backed by a classpath scan of one or more packages. */
	public static class ScanProvider implements ConsoleProvider {

		private final String tab;
		private final Class<?> base;
		private final String[] pkgs;
		private final boolean shallow;
		private final List<Class<?>> extra;

		public ScanProvider(String tab, Class<?> base, String pkg) {
			this(tab, base, new String[]{pkg}, false, new Class<?>[0]);
		}

		@SafeVarargs
		public ScanProvider(String tab, Class<?> base, String pkg, Class<?>... extra) {
			this(tab, base, new String[]{pkg}, false, extra);
		}

		/** Scans several packages at once (e.g. wands + thrown weapons for the "ranged" tab). */
		public ScanProvider(String tab, Class<?> base, String[] pkgs) {
			this(tab, base, pkgs, false, new Class<?>[0]);
		}

		@SafeVarargs
		public ScanProvider(String tab, Class<?> base, String[] pkgs, boolean shallow, Class<?>... extra) {
			this.tab = tab;
			this.base = base;
			this.pkgs = pkgs;
			this.shallow = shallow;
			this.extra = new ArrayList<>();
			for (Class<?> c : extra) this.extra.add(c);
		}

		/** Shallow scan: only classes sitting directly in {@code pkg}, sub-packages excluded. */
		public static ScanProvider shallow(String tab, Class<?> base, String pkg) {
			return new ScanProvider(tab, base, new String[]{pkg}, true, new Class<?>[0]);
		}

		@Override
		public String tabName() {
			return tab;
		}

		@Override
		public List<Class<?>> classes() {
			List<Class<?>> found = ClassScanner.find(pkgs, base, shallow);
			for (Class<?> c : extra) {
				if (!found.contains(c)) found.add(c);
			}
			return found;
		}
	}

	/** Full tab list, in display order. */
	public static List<ConsoleProvider> all() {
		List<ConsoleProvider> list = new ArrayList<>();

		list.add(new ScanProvider("药水", Potion.class, PKG + "items.potions"));
		list.add(new ScanProvider("卷轴", Scroll.class, PKG + "items.scrolls"));
		//LogicStudio extends Weapon directly, so it isn't caught by the MeleeWeapon scan
		list.add(new ScanProvider("武器", MeleeWeapon.class, PKG + "items.weapon", LogicStudio.class));
		//ranged: all wands plus thrown weapons
		list.add(new ScanProvider("远程", Item.class,
				new String[]{PKG + "items.wands", PKG + "items.weapon.missiles"}));
		list.add(new ScanProvider("防具", Armor.class, PKG + "items.armor"));
		list.add(new ScanProvider("神器", Artifact.class, PKG + "items.artifacts"));
		list.add(new ScanProvider("饰品", Trinket.class, PKG + "items.trinkets"));
		//misc: shallow scan of the items root for key items not in a sub-package
		//(Tengu's mask, King's crown, amulet, ...)
		list.add(ScanProvider.shallow("杂项", Item.class, PKG + "items"));
		list.add(new ScanProvider("Buff", Buff.class, PKG + "actors.buffs"));
		list.add(new ScanProvider("怪物", Mob.class, PKG + "actors.mobs"));

		return list;
	}
}
