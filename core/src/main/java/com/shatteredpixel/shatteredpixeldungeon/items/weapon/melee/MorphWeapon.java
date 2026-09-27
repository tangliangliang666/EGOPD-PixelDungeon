/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MorphCombo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Enchanting;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMorph;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * Base class for the shape-shifting weapon "漆黑噤默" and its nine alternate forms.
 * <p>
 * Every melee form is its own class (so each appears separately in the debug-spawn list),
 * but all of them extend this class, which provides:
 * <ul>
 *   <li>a "切换" (switch) action that opens a 3x3 grid of the OTHER ten forms,</li>
 *   <li>a static {@link #morphInto} helper that replaces any weapon of the set with
 *       another form while preserving level, enchantment, curse state and identification.</li>
 * </ul>
 * The ranged form {@link LogicStudio} cannot extend this class due to single inheritance
 * (it extends {@link com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon} directly — it is
 * <b>not</b> a {@link com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow} subclass and
 * does not follow the hero's level), so it wires the same "切换" action itself
 * and delegates the swap to the static {@link #morphInto}.
 * The full set of forms is {@link #ALL_FORMS}, ordered to match the 3x3 grid.
 */
public abstract class MorphWeapon extends MeleeWeapon {

	public static final String AC_MORPH = "MORPH";

	//All ten forms, ordered to fill the 3x3 grid (the current form is always skipped/darkened,
	//so exactly nine alternatives are shown). The set includes the ranged LogicStudio.
	public static final Class<? extends Weapon>[] ALL_FORMS = new Class[]{
			DarkSilence.class,
			Durandal.class,
			ElmWorkshop.class,
			LangyaWorkshop.class,
			OldBoyWorkshop.class,
			ArasWorkshop.class,
			InkWorkshop.class,
			LogicStudio.class,
			CalistaStudio.class,
			RouletteHeavy.class
	};

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.remove(MeleeWeapon.AC_ABILITY); //no duelist ability for this set
		actions.add(AC_MORPH);
		return actions;
	}

	//命中时推进漆黑噤默连击：不同形态叠层并刷新持续时间（漆黑噤默本体不参与）
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		int dmg = super.proc(attacker, defender, damage);
		recordMorphCombo(this, attacker, defender);
		return dmg;
	}

	/**
	 * 命中时推进漆黑噤默连击（静态，供 {@link MorphWeapon} 各形态与远程形态
	 * {@link LogicStudio} 共用）：使用不同形态命中时叠层并刷新持续时间，
	 * 同一形态重复命中不叠层不刷新，漆黑噤默本体不参与。
	 */
	public static void recordMorphCombo(Weapon weapon, Char attacker, Char defender){
		if (attacker instanceof Hero){
			Hero hero = (Hero) attacker;
			if (!(weapon instanceof DarkSilence)){
				MorphCombo combo = hero.buff(MorphCombo.class);
				if (combo == null){
					//首次命中：创建buff并记录该形态
					combo = Buff.affect(hero, MorphCombo.class, MorphCombo.DURATION);
					combo.recordHit(weapon.getClass());
					BuffIndicator.refreshHero();
				} else if (combo.recordHit(weapon.getClass())){
					//新形态命中：叠层并刷新持续时间
					Buff.affect(hero, MorphCombo.class, MorphCombo.DURATION);
					BuffIndicator.refreshHero();
				}
				//同一形态重复命中：不叠层、不刷新
			}

			//自动切换（本体同样生效：便于一路自动叠层）
			scheduleAutoSwitch(hero, weapon);
		}
	}

	/**
	 * 自动切换：攻击（命中）后，若开关已开启，则随机切换到某个<b>尚未被连击 buff 记录</b>的形态，
	 * 省去实战中反复手动切换的麻烦。本体（{@link DarkSilence}）不计入连击，也不会作为随机目标。
	 * <p><b>连击集齐（九层）后改为切回本体</b>：Furioso 只能由本体释放，此时把漆黑噤默送回手中，
	 * 玩家不必再手动翻背包/切换窗口——本体的快捷栏点击也随之变为"释放 Furioso"
	 * （见 {@link DarkSilence#defaultAction()}）。
	 * <p><b>为什么延后执行</b>：本方法在 {@code proc()} 期间被调用（攻击结算中途）。若此时直接换武器，
	 * 同一击随后的 {@code Hero.attackDelay()} 会改用新形态的攻速，连击/天赋判定也会读到新武器。
	 * 故改用 {@link Actor#VFX_PRIO} 优先级的临时 Actor，等本次攻击完全结算后（动画早已播完）再换，
	 * 并顺带播放切换虚影（见 {@link #morphInto}）。
	 */
	public static void scheduleAutoSwitch(final Hero hero, final Weapon used){
		if (hero == null || used == null || !SPDSettings.morphAutoSwitch()){
			return;
		}

		//候选 = 九种参与连击的形态（本体不计层，排除）中尚未被记录的
		final MorphCombo combo = hero.buff(MorphCombo.class);
		final ArrayList<Class<? extends Weapon>> pool = new ArrayList<>();
		for (Class<? extends Weapon> cls : ALL_FORMS){
			if (cls == DarkSilence.class) continue;
			if (combo != null && combo.hasUsed(cls)) continue;
			pool.add(cls);
		}
		//连击集齐（九层）：不再随机，直接切回本体，方便紧接着释放 Furioso
		final boolean comboFull = combo != null && combo.comboCount() >= MorphCombo.MAX_COMBO;
		if (pool.isEmpty() && !comboFull){
			return; //九种形态已全部用过但连击未满（理论上不会发生）：无需切换
		}

		Actor.add(new Actor() {
			{
				actPriority = VFX_PRIO;
			}

			@Override
			protected boolean act() {
				//期间可能已被玩家手动换下/替换，只有该武器仍在手时才自动切换
				if (hero.belongings.weapon() == used || hero.belongings.secondWep() == used){
					Class<? extends Weapon> target;
					if (comboFull){
						//集齐九层：切回本体（此时本体的快捷栏点击即为"释放 Furioso"）
						target = DarkSilence.class;
					} else {
						target = pool.get(Random.Int(pool.size()));
					}
					if (target != used.getClass()){
						morphInto(used, hero, target);
					}
				}
				Actor.remove(this);
				return true;
			}
		});
	}

	//quickslot/quick-use clicks trigger the switch action (map-tap attacks are unaffected)
	@Override
	public String defaultAction() {
		return AC_MORPH;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_MORPH)) {
			return "切换";
		} else {
			return super.actionName(action, hero);
		}
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_MORPH)) {
			GameScene.show(new WndMorph(this, hero));
		}
	}

	//hide inherited weapon-flavor/ability text; each form supplies its own desc() instead
	@Override
	public String statsInfo() {
		return "";
	}

	@Override
	public String abilityInfo() {
		return "";
	}

	/**
	 * 形态的显示名（用于连击 buff 展示"已使用 / 尚未使用"的形态清单）。
	 * 取实例的 name()，与切换窗口、武器面板显示的文字完全一致；
	 * 实例化失败时退回类名，避免展示层因单个形态异常而崩溃。
	 */
	public static String formName(Class<? extends Weapon> cls) {
		Weapon sample = Reflection.newInstance(cls);
		return sample != null ? sample.name() : cls.getSimpleName();
	}

	/**
	 * Switches the given weapon of the set into the given form, preserving level, enchantment,
	 * curse state, identification and quantity. Works for any {@link Weapon} in {@link #ALL_FORMS},
	 * melee or ranged.
	 *
	 * @return true if the switch succeeded.
	 */
	public static boolean morphInto(Weapon current, Hero hero, Class<? extends Weapon> targetClass) {
		if (targetClass == current.getClass()) {
			GLog.i("已经是当前形态");
			return false;
		}

		Weapon replacement = Reflection.newInstance(targetClass);
		if (replacement == null) {
			return false;
		}

		//preserve all persistent data
		//等级必须取 trueLevel()（纯升级等级）而不是 level()：后者会带上「诅咒菱晶」这类加成
		//（Weapon.level() 里 curseInfusionBonus 时 +1+level/6），一旦写进新形态的真实等级，
		//加成就被固化一次，每切换一次再叠加一次 —— 切几轮等级就会雪崩式膨胀。
		//bonus 本身仍由下面的 curseInfusionBonus 复制带走，所以在哪个形态上都照常生效。
		replacement.level(current.trueLevel());
		replacement.enchantment = current.enchantment;
		replacement.cursed = current.cursed;
		replacement.cursedKnown = current.cursedKnown;
		replacement.levelKnown = current.levelKnown;
		replacement.quantity(current.quantity());
		//one-time persistent bonuses (potion of mastery, hardened enchant, curse infusion)
		replacement.masteryPotionBonus = current.masteryPotionBonus;
		replacement.enchantHardened  = current.enchantHardened;
		replacement.curseInfusionBonus = current.curseInfusionBonus;
		//强化符石选定的方向（伤害 / 速度）：2026-09-18 补。此前漏搬 ⇒ 每次切换形态都会把强化符石的
		//效果悄悄洗掉（近战走 MeleeWeapon.damageRoll、远程走 MissileWeapon.damageRoll，两者都读 augment），
		//看上去像"符石白用了"。搬运口径与 SealedSwordBase.copyState 保持一致。
		replacement.augment = current.augment;

		//update the quick-slot reference if this weapon was in a slot
		int slot = Dungeon.quickslot.getSlot(current);
		if (slot != -1) {
			Dungeon.quickslot.setSlot(slot, replacement);
		} else {
			Dungeon.quickslot.clearItem(current);
		}

		boolean equipped    = (hero.belongings.weapon    == current);
		boolean secondary   = (hero.belongings.secondWep == current);

		if (equipped || secondary) {
			//swapped in place inside a weapon slot (no turn cost)
			if (equipped)  hero.belongings.weapon    = replacement;
			if (secondary) hero.belongings.secondWep = replacement;

			replacement.activate(hero);
			((com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite) hero.sprite).updateArmor();
		} else {
			//lying in the inventory: detach the old one (frees a slot), then collect the new one
			current.detachAll(hero.belongings.backpack);
			if (!replacement.collect(hero.belongings.backpack)) {
				//backpack full: drop it on the floor
				Dungeon.level.drop(replacement, hero.pos);
			}
		}

		Item.updateQuickslot();

		//视觉：英雄头顶浮现所切换到形态的武器虚影（复现松脂涂层/神圣武器的附魔视觉效果）
		//sprite 未挂到场景（切场景中）时跳过，避免 parent 为 null 时空指针
		if (hero.sprite != null && hero.sprite.parent != null){
			Enchanting.show(hero, replacement);
		}

		GLog.i("切换为：%s", replacement.name());
		return true;
	}
}
