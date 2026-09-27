/*
 * EGOPD — SPD-side adapter for the standalone debug console library.
 */

package com.shatteredpixel.shatteredpixeldungeon.debug;

import com.mypd.debugconsole.DebugConsole;
import com.mypd.debugconsole.spi.ConsoleHost;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Karma;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Release;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.TearSword;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIcon;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import com.watabou.noosa.Image;
import com.watabou.utils.Reflection;

/**
 * The game-side half of the console: tells the standalone library how to name, icon, and spawn
 * this game's items, mobs, and buffs.
 *
 * <p>Everything SPD-specific about the console lives here and in the providers, so the library
 * module itself stays free of game types.
 *
 * <p>Install with {@link SpdDebugConsole#install()}.
 */
public final class SpdConsoleHost implements ConsoleHost {

	@Override
	public String displayName(Class<?> clazz) {
		try {
			//buff mob/item instances all expose name(); instantiate a sample to read it
			Object sample = Reflection.newInstance(clazz);
			if (sample == null) return clazz.getSimpleName();

			if (sample instanceof Mob) {
				return Messages.titleCase(((Mob) sample).name());
			}
			if (sample instanceof Buff) {
				return Messages.titleCase(((Buff) sample).name());
			}
			if (sample instanceof Item) {
				return Messages.titleCase(((Item) sample).name());
			}
		} catch (Throwable ignored) {
			//fall through to the class name when the object can't be constructed or named
		}
		return clazz.getSimpleName();
	}

	@Override
	public Image icon(Class<?> clazz) {
		try {
			Object sample = Reflection.newInstance(clazz);
			if (sample == null) return null;

			if (sample instanceof Mob) {
				CharSprite sprite = ((Mob) sample).sprite();
				sprite.idle();
				return new Image(sprite);
			}
			if (sample instanceof Buff) {
				return new BuffIcon((Buff) sample, false);
			}
			if (sample instanceof Item) {
				return new ItemSprite((Item) sample);
			}
		} catch (Throwable ignored) {
			//icon is optional; a text-only row is fine
		}
		return null;
	}

	@Override
	public String spawn(Class<?> clazz) {
		try {
			Object sample = Reflection.newInstance(clazz);
			if (sample == null) return null;

			if (sample instanceof Item) {
				return spawnItem((Item) sample);
			}
			if (sample instanceof Mob) {
				//mobs need a map tile: hand off to the placement flow instead
				return null;
			}
			if (sample instanceof Buff) {
				return applyBuff((Class<? extends Buff>) clazz);
			}
		} catch (Throwable t) {
			GLog.i("生成失败: " + clazz.getSimpleName());
		}
		return null;
	}

	@Override
	public boolean hidesBeforeSpawning(Class<?> clazz) {
		if (clazz == null) return false;
		return Mob.class.isAssignableFrom(clazz);
	}

	/**
	 * Mobs need a target tile, so we enter click-to-place mode: the console hides, the player
	 * clicks the map, and the mob spawns there.
	 */
	@Override
	public boolean requestPlacement(final Class<?> clazz, final Runnable onPlaced) {
		if (clazz == null || !Mob.class.isAssignableFrom(clazz)) return false;
		if (Dungeon.level == null) return false;

		final Class<? extends Mob> mobClass = clazz.asSubclass(Mob.class);

		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect(Integer cell) {
				if (cell != null) spawnMobAt(mobClass, cell);
				if (onPlaced != null) onPlaced.run();
			}

			@Override
			public void onRightClick(Integer cell) {
				if (cell != null) spawnMobAt(mobClass, cell);
				if (onPlaced != null) onPlaced.run();
			}

			@Override
			public String prompt() {
				return "点击地图放置「" + mobDisplayName(mobClass) + "」（右键同样放置）";
			}
		});
		return true;
	}

	//================================================================================
	//   spawning
	//================================================================================

	private String spawnItem(Item item) {
		if (Dungeon.hero == null || Dungeon.level == null) return null;

		if (!item.collect()) {
			Dungeon.level.drop(item, Dungeon.hero.pos);
		}
		GLog.i("已生成: " + item.name());
		return item.name();
	}

	private String applyBuff(Class<? extends Buff> buffClass) {
		if (Dungeon.hero == null) return null;

		//FlavourBuff expires instantly without a duration
		if (FlavourBuff.class.isAssignableFrom(buffClass)) {
			Buff.affect(Dungeon.hero, (Class<? extends FlavourBuff>) buffClass, 30f);
		} else {
			Buff.affect(Dungeon.hero, buffClass);
		}

		applyStackingConvenience(buffClass);

		Buff sample = Reflection.newInstance(buffClass);
		String name = sample != null ? Messages.titleCase(sample.name()) : buffClass.getSimpleName();
		GLog.i("已附加: " + name);
		return name;
	}

	/**
	 * A few stackable buffs are tedious to build up by hand during testing, so one click adds a
	 * layer. This mirrors the game's own debug affordances.
	 */
	private void applyStackingConvenience(Class<? extends Buff> buffClass) {
		//Release: gainStack() caps by talent points, so testing needs the uncapped entry point
		if (buffClass == Release.class) {
			Release release = Dungeon.hero.buff(Release.class);
			if (release != null) release.debugGainStack();

		} else if (buffClass == Karma.class) {
			Karma karma = Dungeon.hero.buff(Karma.class);
			if (karma != null) karma.addKarma(10);

		} else if (buffClass == TearSword.class) {
			//add one orbiting blade per click, making the arrangement easy to inspect
			TearSword.addStacks(Dungeon.hero, 1);
		}
	}

	private String mobDisplayName(Class<? extends Mob> mobClass) {
		Mob m = Reflection.newInstance(mobClass);
		return m != null ? Messages.titleCase(m.name()) : mobClass.getSimpleName();
	}

	private void spawnMobAt(Class<? extends Mob> mobClass, int cell) {
		if (Dungeon.level == null) return;
		try {
			Mob mob = Reflection.newInstance(mobClass);
			if (mob == null) return;
			mob.pos = findSpawnCell(cell);
			mob.state = mob.WANDERING;
			GameScene.add(mob, 0);
			GLog.i("已生成: " + mob.name());
		} catch (Throwable t) {
			GLog.i("生成失败: " + mobClass.getSimpleName());
		}
	}

	/** Prefers the clicked tile; falls back to an adjacent walkable, unoccupied one. */
	private int findSpawnCell(int cell) {
		if (Dungeon.level == null) return cell;

		if (isFree(cell)) return cell;

		int w = Dungeon.level.width();
		int[] offs = { -w - 1, -w, -w + 1, -1, +1, +w - 1, +w, +w + 1 };
		for (int off : offs) {
			int c = cell + off;
			if (isFree(c)) return c;
		}
		return cell;
	}

	private boolean isFree(int cell) {
		return Dungeon.level != null
				&& cell >= 0 && cell < Dungeon.level.length()
				&& Dungeon.level.passable[cell]
				&& Actor.findChar(cell) == null;
	}

	/** Sprite for a mob that failed to produce one, so the row still shows something. */
	static Image fallbackMobIcon() {
		return new ItemSprite(ItemSpriteSheet.MOB_HOLDER);
	}
}
