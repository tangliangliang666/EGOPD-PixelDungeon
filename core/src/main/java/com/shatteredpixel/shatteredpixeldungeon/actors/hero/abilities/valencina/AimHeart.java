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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.valencina;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AimHeartMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.PalermoFencing;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Callback;

/**
 * 拇指 前二老板的盔甲技能「瞄准心脏」（2026-09-08）。
 * <p>消耗 50% 盔甲充能，指定视野内一个敌方单位施加 10 回合 {@link AimHeartMark}：
 * 标记期间目标失去对燃烧与麻痹的免疫、并被前二老板攻击时承受额外伤害
 * （基础 20%，「弱点贯穿」+1~+4 → 40/60/80/100%）。</p>
 * <p>「碾杀虫豸」在使用时向前二老板向目标突进 3/4/5/6 格；若突进后目标贴身，
 * 则自动补一次_无回合消耗_的免费巴勒莫剑术。「荣耀凯旋」在击杀被标记目标时恢复生命。</p>
 */
public class AimHeart extends ArmorAbility {

	{
		baseChargeUse = 50f; //消耗 50% 盔甲充能
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public int targetedPos( Char user, int dst ) {
		return dst;
	}

	@Override
	protected void activate(final ClassArmor armor, final Hero hero, Integer target) {
		if (target == null) return;

		final Char ch = Actor.findChar( target );
		if (ch == null || !Dungeon.level.heroFOV[target]){
			GLog.w( Messages.get(this, "no_target") );
			return;
		} else if (ch.alignment != Char.Alignment.ENEMY){
			GLog.w( Messages.get(this, "ally_target") );
			return;
		}

		//消耗充能 + 施加 10 回合瞄准心脏标记
		armor.charge -= chargeUse( hero );
		armor.updateQuickslot();
		AimHeartMark.mark( ch );
		hero.sprite.zap( ch.pos );

		//可免费挥击的近战武器（碾杀虫豸触发）
		final MeleeWeapon wep = (hero.belongings.attackingWeapon() instanceof MeleeWeapon)
				? (MeleeWeapon) hero.belongings.attackingWeapon() : null;
		//碾杀虫豸：突进 3/4/5/6 格（+1~+4 → 点数+2）
		final int crush = hero.pointsInTalent( Talent.CRUSH_BUGS );
		final int dash = 2 + crush;

		hero.busy();

		//计算直线突进落点（最远 dash 格；撞墙/被占/接近目标时提前停）
		int landing = hero.pos;
		if (crush > 0 && !hero.rooted){
			Ballistica line = new Ballistica( hero.pos, ch.pos, Ballistica.PROJECTILE );
			int steps = 0;
			for (int i = 1; i < line.path.size(); i++){
				int c = line.path.get( i );
				if (c == ch.pos) break;
				if (!Dungeon.level.passable[c]) break;
				if (Actor.findChar( c ) != null) break;
				landing = c;
				if (++steps >= dash) break;
			}
		}
		final int dest = landing;

		final Callback finish = new Callback() {
			@Override
			public void call() {
				//突进后目标贴身 → 自动释放一次无消耗的巴勒莫剑术（不占回合、不消耗「加速的未来」）
				if (wep != null && ch.isAlive() && Dungeon.level.adjacent( hero.pos, ch.pos )){
					PalermoFencing.freeSwing( hero, ch, wep );
				}
				Invisibility.dispel();
				if (hero.isAlive()){
					hero.spendAndNext( 1f );
				}
			}
		};

		if (dest != hero.pos){
			hero.sprite.jump( hero.pos, dest, new Callback() {
				@Override
				public void call() {
					hero.move( dest );
					Dungeon.level.occupyCell( hero );
					Dungeon.observe();
					GameScene.updateFog();
					finish.call();
				}
			} );
		} else {
			finish.call();
		}
	}

	@Override
	public int icon() {
		return HeroIcon.AIM_HEART;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.WEAKNESS_PIERCE, Talent.CRUSH_BUGS, Talent.GLORIOUS_TRIUMPH, Talent.HEROIC_ENERGY};
	}
}
