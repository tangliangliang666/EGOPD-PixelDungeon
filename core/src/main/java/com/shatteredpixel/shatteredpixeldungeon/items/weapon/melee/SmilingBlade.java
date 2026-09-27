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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlackAshParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * 笑靥：五阶武器。
 * <p>伤害公式 (5+L ~ 20+6×L)，1.12 倍命中（ACC）。
 * 成长系统：使用它击杀敌人时按怪物经验值累积"经验计数"；
 * 第一次达到 75 经验、第二次达到 150 经验时可使用"溶解"，
 * 从六种成长效果中选择一种（攻击距离+1 / 最大伤害+10 / 最小伤害+10 /
 * 命中施加残废 / 命中施加腐蚀淤泥 / 视为拥有粘稠刻印）。
 * 可成长两次，之后不再获取经验值。</p>
 */
public class SmilingBlade extends MeleeWeapon {

	public static final String AC_DISSOLVE = "DISSOLVE";

	//成长效果类型
	public static final int GROW_REACH     = 0;
	public static final int GROW_MAX       = 1;
	public static final int GROW_MIN       = 2;
	public static final int GROW_CRIPPLE   = 3;
	public static final int GROW_OOZE      = 4;
	public static final int GROW_VISCOSITY = 5;

	private static final String[] GROWTH_NAMES = {
			"攻击距离+1",
			"最大伤害+10",
			"最小伤害+10",
			"命中时施加残废",
			"命中时施加腐蚀淤泥",
			"视为拥有粘稠刻印"
	};

	//第一次/第二次成长所需经验（累计计数）
	public static final int EXP_FIRST  = 75;
	public static final int EXP_SECOND = 150;

	private int exp = 0;        //经验计数
	private int growths = 0;    //已溶解次数（0~2）
	private int growthOne = -1; //第一次选择的成长
	private int growthTwo = -1; //第二次选择的成长

	{
		image = ItemSpriteSheet.SMILING_BLADE;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1f;

		tier = 5;
		ACC = 1.12f; //1.12倍命中
	}

	@Override
	public int min(int lvl) {
		return 5 + lvl + (hasGrowth(GROW_MIN) ? 10 : 0);
	}

	@Override
	public int max(int lvl) {
		return 20 + 6*lvl + (hasGrowth(GROW_MAX) ? 10 : 0);
	}

	@Override
	public int reachFactor(Char owner) {
		return super.reachFactor(owner) + (hasGrowth(GROW_REACH) ? 1 : 0);
	}

	public boolean hasGrowth(int type){
		return growthOne == type || growthTwo == type;
	}

	//是否达到成长条件（可溶解）
	private boolean readyToGrow(){
		if (growths >= 2) return false;
		int need = growths == 0 ? EXP_FIRST : EXP_SECOND;
		return exp >= need;
	}

	//累积经验（成长两次后不再获取）
	public void gainExp(int amount){
		if (growths >= 2) return;
		exp += amount;
		updateQuickslot();
	}

	//击杀敌人时按怪物经验值累积（由 Mob.die 调用）
	public static void onEnemyKilled(Hero hero, Mob mob){
		SmilingBlade wep = null;
		if (hero.belongings.weapon() instanceof SmilingBlade){
			wep = (SmilingBlade) hero.belongings.weapon();
		} else if (hero.belongings.secondWep() instanceof SmilingBlade){
			wep = (SmilingBlade) hero.belongings.secondWep();
		}
		if (wep != null){
			wep.gainExp(mob.EXP);
		}
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (readyToGrow()){
			actions.add(AC_DISSOLVE);
		}
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_DISSOLVE)){

			GameScene.show(new WndOptions(new ItemSprite(this),
					Messages.titleCase(name()),
					"选择一种成长效果：",
					GROWTH_NAMES) {
				@Override
				protected void onSelect(int index) {
					if (index >= 0 && index < GROWTH_NAMES.length){
						dissolve(Dungeon.hero, index);
					}
				}
			});

		}
	}

	private void dissolve(Hero hero, int type){
		if (!readyToGrow()) return;

		if (growths == 0){
			growthOne = type;
		} else if (growths == 1){
			growthTwo = type;
		} else {
			return;
		}
		growths++;

		hero.spendAndNext(Actor.TICK);
		updateQuickslot();
		GLog.p( "笑靥从尸体中获得了" + GROWTH_NAMES[type] + "的能力" );
	}

	private String growthName(int type){
		if (type < 0 || type >= GROWTH_NAMES.length) return "";
		return GROWTH_NAMES[type];
	}

	//功能描述 + 溶解相关动态文本（排在功能文本之后）
	@Override
	public String statsInfo(){
		String info = Messages.get(this, "stats_desc");

		if (growths >= 2){
			info += "\n\n" + "这把武器已经成长到了极限。";
		} else if (readyToGrow()){
			info += "\n\n" + "这把武器已经吸收了足够多的尸体，可以进行一次成长";
		}
		if (growthOne != -1){
			info += "\n\n" + "这把武器从尸体中获得了" + growthName(growthOne) + "的能力";
		}
		if (growthTwo != -1){
			info += "\n\n" + "这把武器从尸体中获得了" + growthName(growthTwo) + "的能力";
		}

		return info;
	}

	//成长可用时，武器显示向下掉落的黑色粒子特效
	@Override
	public Emitter emitter() {
		if (readyToGrow()){
			Emitter e = new Emitter();
			e.pos( 8, 8 );
			e.fillTarget = false;
			e.pour( BlackAshParticle.FALLING, 0.25f );
			return e;
		}
		return null;
	}

	//命中时按已选择的成长施加效果
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		if (attacker.buff(MagicImmune.class) == null && defender.isAlive()){
			if (hasGrowth(GROW_CRIPPLE)){
				Buff.prolong(defender, Cripple.class, 1 + buffedLvl());
			}
			if (hasGrowth(GROW_OOZE)){
				Buff.affect(defender, Ooze.class).set(Ooze.DURATION);
			}
		}

		return damage;
	}

	@Override
	public String name() {
		return "笑靥";
	}

	private static final String EXP_SAVE         = "exp";
	private static final String GROWTHS_SAVE     = "growths";
	private static final String GROWTH_ONE_SAVE  = "growth_one";
	private static final String GROWTH_TWO_SAVE  = "growth_two";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(EXP_SAVE, exp);
		bundle.put(GROWTHS_SAVE, growths);
		bundle.put(GROWTH_ONE_SAVE, growthOne);
		bundle.put(GROWTH_TWO_SAVE, growthTwo);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		exp = bundle.getInt(EXP_SAVE);
		growths = bundle.getInt(GROWTHS_SAVE);
		growthOne = bundle.getInt(GROWTH_ONE_SAVE);
		growthTwo = bundle.getInt(GROWTH_TWO_SAVE);
	}
}
