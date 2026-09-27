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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMorph;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The ranged alternate form of the shape-shifting weapon: twin revolvers and a shotgun
 * with their ammunition.
 * <p>Tier 5 ranged weapon. This is a standalone implementation that borrows the spirit bow's
 * growth numbers (1 base min / 6 base max) and its special "throw without losing the weapon"
 * projectile attack, but scales with the weapon's own upgrade level instead of the hero's level,
 * and is fully decoupled from {@link com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow}.
 * It does <b>not</b> override {@code level()}/{@code buffedLvl()} — that override is the <b>only</b>
 * source of "auto-upgrade with the hero's level", and it must stay absent here.
 * Cannot extend {@link MorphWeapon} due to single inheritance, so it wires the same
 * "切换" action itself and delegates the swap to {@link MorphWeapon#morphInto}.
 * <p>本形态的快捷栏点击被"切换"占用（{@link #defaultAction()}），无法从快捷栏发起射击，
 * 因此<b>直接攻击（点按敌人）会被接管为射击</b>：{@link Hero#actAttack} 检测到装备本武器时
 * 改为调用 {@link #shootAt(Hero, int)} 发射 {@link Shot}，不再做普通近战结算。
 */
public class LogicStudio extends Weapon {

	public static final String AC_SHOOT = "SHOOT";

	{
		image = ItemSpriteSheet.LOGIC_STUDIO;
		hitSound = Assets.Sounds.HIT_ARROW;
		hitSoundPitch = 1f;

		RCH = 9; //ranged reach: tapping enemies attacks them from afar

		bones = false;
	}

	@Override
	public int STRReq(int lvl) {
		int req = STRReq(5, lvl); //tier 5
		//muscle-memory potion bonus, same as the melee forms (MeleeWeapon.STRReq)
		if (masteryPotionBonus){
			req -= 2;
		}
		return req;
	}

	//借用了灵能弓（SpiritBow）的成长数值，但成长来源是"武器自身的强化等级"（即下面 lvl 形参），与英雄等级无关。
	//⚠️ 绝不能在此覆写 level() / buffedLvl()：灵能弓正是靠覆写 level() 返回 Dungeon.hero.lvl/5 来实现
	//"随英雄等级自动升级"（见 SpiritBow.java:272，配套还有 buffedLvl() 覆写与 isUpgradable()=false）。
	//本形态一旦照抄那套覆写，伤害就会跟英雄等级走、且升级卷轴不再生效。成长只走 min(int)/max(int)。
	@Override
	public int min(int lvl) {
		return 1 + lvl;
	}

	@Override
	public int max(int lvl) {
		return 6 + 2*lvl;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_SHOOT);
		actions.add(MorphWeapon.AC_MORPH);
		return actions;
	}

	//quickslot/quick-use clicks trigger the switch action (map-tap shots are unaffected)
	@Override
	public String defaultAction() {
		return MorphWeapon.AC_MORPH;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_SHOOT)) {
			return "射击";
		} else if (action.equals(MorphWeapon.AC_MORPH)) {
			return "切换";
		} else {
			return super.actionName(action, hero);
		}
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_SHOOT)) {
			curUser = hero;
			curItem = this;
			GameScene.selectCell(shooter);
		} else if (action.equals(MorphWeapon.AC_MORPH)) {
			GameScene.show(new WndMorph(this, hero));
		}
	}

	//命中时推进漆黑噤默连击（与近战形态共用同一逻辑，见 MorphWeapon.recordMorphCombo）
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		int dmg = super.proc(attacker, defender, damage);
		MorphWeapon.recordMorphCombo(this, attacker, defender);
		return dmg;
	}

	/**
	 * <b>直接攻击改射击</b>：由 {@link Hero#actAttack} 在玩家点按敌人（以及其它发起普通攻击的路径）时调用。
	 * <p>不再进行普通近战结算，而是直接发射一枚 {@link Shot} 投射物：英雄忙碌、投射物飞行、
	 * 命中/落空结算与回合消耗（{@code spendAndNext}）全部由 {@link Item#cast} 内部完成，
	 * 因此调用方接管后应立即清空自身的攻击动作状态、不要再走 melee 流程。
	 * <p>射击距离仍由近战攻击距离（{@link com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon#RCH} = 9）
	 * 把关：{@link Hero#canAttack} 已在调用本方法前校验过目标可达与可见。
	 */
	public void shootAt(Hero hero, int target){
		new Shot().cast(hero, target);
	}

	private CellSelector.Listener shooter = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer target) {
			if (target != null) {
				new Shot().cast(curUser, target);
			}
		}

		@Override
		public String prompt() {
			return "选择射击目标";
		}
	};

	@Override
	public String name() {
		return "逻辑工作室";
	}

	@Override
	public String desc() {
		return "隐藏在漆黑噤默手套中的武器，可以随时方便而无声的取用。\n\n逻辑工作室的产品，两把左轮与一把猎枪，包含配套弹药。\n\n直接攻击即为射击。";
	}

	/**
	 * The temporary projectile fired by the shoot action. It is a fresh instance per shot,
	 * never stored in the inventory, and never removes the equipped weapon.
	 */
	public class Shot extends MissileWeapon {

		{
			image = ItemSpriteSheet.LOGIC_BULLET;
			hitSound = Assets.Sounds.HIT_ARROW;

			setID = 0;
		}

		@Override
		public ArrayList<String> actions(Hero hero) {
			return new ArrayList<>();
		}

		@Override
		public String defaultAction() {
			return null;
		}

		@Override
		public int defaultQuantity() {
			return 1;
		}

		@Override
		public Item split(int amount) {
			return null;
		}

		//落空时的水花颜色：灰白（Splash 只取低 24 位，按 0xRRGGBB 解析）
		private static final int MISS_SPLASH_COLOR = 0xDDDDDD;

		@Override
		protected void onThrow(int cell) {
			Char enemy = Actor.findChar(cell);
			if (enemy == null || enemy == curUser) {
				parent = null;
				Splash.at(cell, MISS_SPLASH_COLOR, 1);
			} else {
				if (!curUser.shoot(enemy, this)) {
					Splash.at(cell, MISS_SPLASH_COLOR, 1);
				}
			}
		}

		@Override
		public int damageRoll(Char owner) {
			return LogicStudio.this.damageRoll(owner);
		}

		@Override
		public int proc(Char attacker, Char defender, int damage) {
			return LogicStudio.this.proc(attacker, defender, damage);
		}

		@Override
		public float delayFactor(Char user) {
			return LogicStudio.this.delayFactor(user);
		}

		@Override
		public int STRReq(int lvl) {
			return LogicStudio.this.STRReq();
		}

		/**
		 * 贴脸命中修正：投掷物默认对相邻目标有 0.5x 的"贴脸"命中惩罚
		 * （见 {@link MissileWeapon#adjacentAccFactor}）。但本形态的<b>直接攻击已被接管为射击</b>，
		 * 相邻敌人也会走投射物——若不修正，原先稳中的近战平砍会突变成 50% 命中，手感如同 bug。
		 * 因此相邻时取消该惩罚（按 1x 计），远距离仍保留 1.5x 的射击命中加成。
		 */
		@Override
		protected float adjacentAccFactor(Char owner, Char target) {
			if (Dungeon.level.adjacent(owner.pos, target.pos)) {
				return 1f;
			}
			return super.adjacentAccFactor(owner, target);
		}

		@Override
		public void throwSound() {
			Sample.INSTANCE.play(Assets.Sounds.ATK_SPIRITBOW, 1, Random.Float(0.87f, 1.15f));
		}
	}
}
