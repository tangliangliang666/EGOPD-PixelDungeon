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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.ArcaneBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Firebomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.FlashBangBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.FrostBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.HolyBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Noisemaker;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.RegrowthBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.ShrapnelBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.SmokeBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.WoollyBomb;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 四阶武器「胎儿博士」。
 * <p>近战伤害固定 4~4（不随升级成长）。装备时具有特殊能力「投放炸弹」
 * （快捷栏点击即用）：无消耗地向指定位置投放一次炸弹，伤害按本武器的强化等级计算
 * （随机 4+3L ~ 12+9L，扣目标护甲），投掷过程受武器攻击延迟影响。
 * 被诅咒时，使用后有 33% 概率在脚下也产生一次投放炸弹判定。</p>
 * <p>像自然之靴吸收种子那样吸收特殊炸弹（仅合成特殊弹，普通炸弹/双倍炸弹不可吸收，
 * 不可重复吸收）。每吸收 n 种，投放时有 1-(1/2)^n 的概率投出特殊炸弹，
 * 种类在已吸收的炸弹中等概率随机。</p>
 */
public class FetalDoctor extends MeleeWeapon {

	{
		image = ItemSpriteSheet.FETAL_DOCTOR;

		defaultAction = AC_DEPLOY; //快捷栏点击 = 投放炸弹

		tier = 4;
	}

	public static final String AC_DEPLOY = "DEPLOY";
	public static final String AC_ABSORB = "ABSORB";

	//已吸收的特殊炸弹种类（不可重复）
	public ArrayList<Class<? extends Bomb>> absorbed = new ArrayList<>();

	//可吸收的特殊炸弹（仅合成特殊弹；普通炸弹 Bomb / 双倍炸弹 DoubleBomb 不可吸收）
	private static final ArrayList<Class<? extends Bomb>> ABSORBABLE = new ArrayList<>();
	static {
		ABSORBABLE.add(Firebomb.class);
		ABSORBABLE.add(FrostBomb.class);
		ABSORBABLE.add(WoollyBomb.class);
		ABSORBABLE.add(Noisemaker.class);
		ABSORBABLE.add(SmokeBomb.class);
		ABSORBABLE.add(FlashBangBomb.class);
		ABSORBABLE.add(RegrowthBomb.class);
		ABSORBABLE.add(HolyBomb.class);
		ABSORBABLE.add(ArcaneBomb.class);
		ABSORBABLE.add(ShrapnelBomb.class);
	}

	//「拆迁办」挑战（2026-09-04）：开局胎儿博士力量需求固定为 10（否则沿用 tier 默认）
	@Override
	public int STRReq(int lvl) {
		return Dungeon.isChallenged(Challenges.DEMOLITION_SQUAD) ? 10 : super.STRReq(lvl);
	}

	//「拆迁办」挑战：胎儿博士作为初始武器时无法卸下（替换/丢弃均被拦截）
	@Override
	public boolean doUnequip( Hero hero, boolean collect, boolean single ) {
		if (Dungeon.isChallenged(Challenges.DEMOLITION_SQUAD)){
			return false;
		}
		return super.doUnequip( hero, collect, single );
	}

	//近战伤害固定 4~4（不随升级成长；升级只增强投放炸弹的伤害）
	@Override
	public int min(int lvl) {
		return 4;
	}

	@Override
	public int max(int lvl) {
		return 4;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero)){
			actions.add(AC_DEPLOY);
			if (!cursed) actions.add(AC_ABSORB);
		}
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_DEPLOY)) return "投放炸弹";
		if (action.equals(AC_ABSORB)) return "吸收炸弹";
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_DEPLOY)){
			if (!isEquipped(hero)){
				GLog.w("需要装备胎儿博士才能投放炸弹");
				return;
			}
			deployBomb(hero);
		} else if (action.equals(AC_ABSORB)){
			if (!isEquipped(hero)){
				GLog.w("需要装备胎儿博士才能吸收炸弹");
				return;
			}
			if (cursed){
				GLog.w("被诅咒的胎儿博士无法吸收炸弹");
				return;
			}
			GameScene.selectItem(itemSelector);
		}
	}

	//========== 投放炸弹 ==========

	private void deployBomb(final Hero hero){
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect(Integer dst) {
				if (dst == null) return;
				//投掷弹道：与投掷物品一致，可被地形与怪物拦截
				final int cell = new Ballistica(hero.pos, dst, Ballistica.PROJECTILE).collisionPos;
				final Bomb bomb = createBombWithSpecial();

				//扔出炸弹物品到指定格的动画
				hero.sprite.zap(cell);
				hero.busy();
				Sample.INSTANCE.play(Assets.Sounds.MISS, 0.6f, 0.6f, 1.5f);
				((MissileSprite) hero.sprite.parent.recycle(MissileSprite.class)).
						reset(hero.sprite, cell, bomb, new Callback() {
							@Override
							public void call() {
								//先点燃引信再落地：堆贴图渲染时才能显示红色闪烁（同原版投掷顺序）
								bomb.lightFuse();
								Dungeon.level.drop(bomb, cell).sprite.drop();

								//被诅咒时：33% 概率在脚下也投放一颗炸弹实体
								if (cursed && Random.Float() < 0.33f){
									Bomb feetBomb = createBombWithSpecial();
									feetBomb.lightFuse();
									Dungeon.level.drop(feetBomb, hero.pos).sprite.drop();
								}

								//投掷过程受武器本身攻击延迟的影响
								hero.spendAndNext(hero.attackDelay());
							}
						});
			}
			@Override
			public String prompt() {
				return "选择投放炸弹的位置";
			}
		});
	}

	//生成一颗炸弹实体（无消耗；若触发特殊炸弹则随机一种已吸收类型，不可拾取）
	private Bomb createBombWithSpecial(){
		Class<? extends Bomb> special = null;
		if (!absorbed.isEmpty()){
			//每吸收 n 种，投出特殊炸弹的概率 = 1-(1/2)^n
			if (Random.Float() < 1 - Math.pow(0.5, absorbed.size())){
				special = (Class<? extends Bomb>) Random.element(absorbed);
			}
		}
		Bomb bomb = createBomb(special);
		bomb.pickable = false; //胎儿博士的炸弹实体不可拾取
		bomb.bombLvl = buffedLvl(); //快照当前强化等级，供爆炸伤害/描述/读档后使用
		return bomb;
	}

	//========== 吸收炸弹 ==========

	public boolean canAbsorb(Item item){
		return ABSORBABLE.contains(item.getClass()) && !absorbed.contains(item.getClass());
	}

	protected WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return "选择要吸收的炸弹";
		}

		@Override
		public boolean itemSelectable(Item item) {
			return canAbsorb(item);
		}

		@Override
		public void onSelect(Item item) {
			if (item != null){
				//canAbsorb 已保证是可吸收的特殊炸弹
				absorbed.add((Class<? extends Bomb>) item.getClass());
				item.detach(Dungeon.hero.belongings.backpack);
				GLog.i("胎儿博士吸收了" + item.name() + "的力量！");
				Dungeon.hero.sprite.operate(Dungeon.hero.pos);
				Dungeon.hero.spendAndNext(Dungeon.hero.cooldown()); //1回合
			}
		}
	};

	//========== 炸弹实例（爆炸伤害按胎儿博士强化等级计算） ==========

	//伤害公式：随机(6+2L, 15+6L)，L = 本武器强化等级（投掷瞬间快照 bombLvl）
	private static int doctorDamage(int L){
		return Random.NormalIntRange(6 + 2*L, 15 + 6*L);
	}

	private static String doctorDesc(int L, boolean burning){
		String desc = "胎儿博士投出的炸弹。\n\n它可以造成_" + (6 + 2*L) + "~" + (15 + 6*L) + "点伤害_，且无法被拾取。";
		if (burning) {
			return desc + "\n\n" + Messages.get(Bomb.class, "desc_burning");
		} else {
			return desc + "\n\n" + Messages.get(Bomb.class, "desc_fuse");
		}
	}

	private Bomb createBomb(Class<? extends Bomb> special){
		if (special == Firebomb.class)      return new DoctorFirebomb();
		if (special == FrostBomb.class)     return new DoctorFrostBomb();
		if (special == WoollyBomb.class)    return new DoctorWoollyBomb();
		if (special == Noisemaker.class)    return new DoctorNoisemaker();
		if (special == SmokeBomb.class)     return new DoctorSmokeBomb();
		if (special == FlashBangBomb.class) return new DoctorFlashBangBomb();
		if (special == RegrowthBomb.class)  return new DoctorRegrowthBomb();
		if (special == HolyBomb.class)      return new DoctorHolyBomb();
		if (special == ArcaneBomb.class)    return new DoctorArcaneBomb();
		if (special == ShrapnelBomb.class)  return new DoctorShrapnelBomb();
		return new DoctorBomb();
	}

	//注：均为静态内部类——落地炸弹会作为 Heap 物品独立存档，
	//非静态内部类无法被 Bundle 反序列化（读档后炸弹会丢失），见 Bundle.get() 的跳过逻辑
	public static class DoctorBomb extends Bomb {
		@Override protected int explosionDamage(){ return doctorDamage(bombLvl); }
		@Override public String desc(){ return doctorDesc(bombLvl, fuse != null); }
	}
	public static class DoctorFirebomb extends Firebomb {
		@Override protected int explosionDamage(){ return doctorDamage(bombLvl); }
		@Override public String desc(){ return doctorDesc(bombLvl, fuse != null); }
	}
	public static class DoctorFrostBomb extends FrostBomb {
		@Override protected int explosionDamage(){ return doctorDamage(bombLvl); }
		@Override public String desc(){ return doctorDesc(bombLvl, fuse != null); }
	}
	public static class DoctorWoollyBomb extends WoollyBomb {
		@Override protected int explosionDamage(){ return doctorDamage(bombLvl); }
		@Override public String desc(){ return doctorDesc(bombLvl, fuse != null); }
	}
	public static class DoctorNoisemaker extends Noisemaker {
		@Override protected int explosionDamage(){ return doctorDamage(bombLvl); }
		@Override public String desc(){ return doctorDesc(bombLvl, fuse != null); }
	}
	public static class DoctorSmokeBomb extends SmokeBomb {
		@Override protected int explosionDamage(){ return doctorDamage(bombLvl); }
		@Override public String desc(){ return doctorDesc(bombLvl, fuse != null); }
	}
	public static class DoctorFlashBangBomb extends FlashBangBomb {
		@Override protected int explosionDamage(){ return doctorDamage(bombLvl); }
		@Override public String desc(){ return doctorDesc(bombLvl, fuse != null); }
	}
	public static class DoctorRegrowthBomb extends RegrowthBomb {
		@Override protected int explosionDamage(){ return doctorDamage(bombLvl); }
		@Override public String desc(){ return doctorDesc(bombLvl, fuse != null); }
	}
	public static class DoctorHolyBomb extends HolyBomb {
		@Override protected int explosionDamage(){ return doctorDamage(bombLvl); }
		@Override public String desc(){ return doctorDesc(bombLvl, fuse != null); }
	}
	public static class DoctorArcaneBomb extends ArcaneBomb {
		@Override protected int explosionDamage(){ return doctorDamage(bombLvl); }
		@Override public String desc(){ return doctorDesc(bombLvl, fuse != null); }
	}
	public static class DoctorShrapnelBomb extends ShrapnelBomb {
		@Override protected int explosionDamage(){ return doctorDamage(bombLvl); }
		@Override public String desc(){ return doctorDesc(bombLvl, fuse != null); }
	}

	//========== 存档 ==========

	private static final String ABSORBED = "absorbed";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(ABSORBED, absorbed.toArray(new Class[0]));
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		absorbed.clear();
		if (bundle.contains(ABSORBED) && bundle.getClassArray(ABSORBED) != null){
			for (Class<?> c : bundle.getClassArray(ABSORBED)){
				if (c != null) absorbed.add((Class<? extends Bomb>) c);
			}
		}
	}

	//========== 文本 ==========

	@Override
	public String info() {
		String info = Messages.get(this, "desc"); //开场白

		//原版武器面板模板
		if (levelKnown) {
			info += "\n\n" + Messages.get(MeleeWeapon.class, "stats_known", tier,
					augment.damageFactor(min()), augment.damageFactor(max()), STRReq());
			if (Dungeon.hero != null) {
				if (STRReq() > Dungeon.hero.STR()) {
					info += " " + Messages.get(Weapon.class, "too_heavy");
				} else if (Dungeon.hero.STR() > STRReq()) {
					info += " " + Messages.get(Weapon.class, "excess_str", Dungeon.hero.STR() - STRReq());
				}
			}
		} else {
			info += "\n\n" + Messages.get(MeleeWeapon.class, "stats_unknown", tier, min(0), max(0), STRReq(0));
			if (Dungeon.hero != null && STRReq(0) > Dungeon.hero.STR()) {
				info += " " + Messages.get(MeleeWeapon.class, "probably_too_heavy");
			}
		}

		//炸弹信息（伤害范围随强化等级变化，用下划线突出）
		int L = buffedLvl();
		info += "\n\n一般来说，胎儿博士投放的炸弹可以造成_" + (6 + 2*L) + "~" + (15 + 6*L) + "点伤害_。";

		if (absorbed.isEmpty()){
			info += "\n\n它尚未吸收任何炸弹的力量。";
		} else {
			String names = "";
			for (int i = 0; i < absorbed.size(); i++){
				if (i > 0) names += "、";
				names += Messages.titleCase(Messages.get(absorbed.get(i), "name"));
			}
			int chance = Math.round((1f - (float)Math.pow(0.5, absorbed.size())) * 100);
			info += "\n\n它已经灌注了_" + names + "_的力量，目前有" + chance + "%的概率投出特殊炸弹。";
		}

		//附魔/诅咒（复用原版文本）
		if (enchantment != null && (cursedKnown || !enchantment.curse())){
			info += "\n\n" + Messages.capitalize(Messages.get(Weapon.class, "enchanted", enchantment.name())) + " " + enchantment.desc();
		}
		if (cursed && isEquipped(Dungeon.hero)){
			info += "\n\n" + Messages.get(Weapon.class, "cursed_worn");
		} else if (cursedKnown && cursed){
			info += "\n\n" + Messages.get(Weapon.class, "cursed");
		}

		return info;
	}
}
