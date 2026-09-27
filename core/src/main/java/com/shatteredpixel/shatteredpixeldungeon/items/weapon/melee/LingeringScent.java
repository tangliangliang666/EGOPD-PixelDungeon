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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PetalParticle;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;

//余香：四阶近战武器，武技与音效套用十字弩（Crossbow），强化飞镖并可将附魔赋予飞镖。
//效果：装备时进行等待会获得"花矢"buff——拥有花矢时使用涂药飞镖不消耗耐久，
//且角色身上向下飘落粉色、淡蓝色相间的花瓣粒子。
public class LingeringScent extends Crossbow {

	{
		image = ItemSpriteSheet.LINGERING_SCENT;

		tier = 4;
	}

	@Override
	public int min(int lvl) {
		return  4 +  //base
				lvl;   //level scaling
	}

	@Override
	public int max(int lvl) {
		return  20 +  //base
				4*lvl;   //level scaling
	}

	//花矢：等待获得，持续期间涂药飞镖不消耗耐久（触发一次后消失），角色身上持续飘落花瓣
	public static class FlowerArrow extends Buff {

		public static final int DURATION = 10;

		private static final float PETAL_INTERVAL = 0.2f; //花瓣飘落间隔（秒），实时效果

		private int turnsLeft = DURATION;
		private Emitter petalEmitter;

		{
			announced = true;
			type = buffType.POSITIVE;
		}

		//等待刷新：重置剩余回合数（不叠加）
		public void refresh(){
			turnsLeft = DURATION;
		}

		@Override
		public boolean attachTo(Char target) {
			if (super.attachTo(target)){
				startParticles();
				return true;
			}
			return false;
		}

		//启动常驻花瓣发射器（按真实时间持续飘落，站立不动时也显示）
		private void startParticles(){
			if (petalEmitter == null && target.sprite != null){
				petalEmitter = target.sprite.emitter();
				petalEmitter.pour( PetalParticle.FALLING, PETAL_INTERVAL );
			}
		}

		@Override
		public boolean act() {
			startParticles(); //读档等场景兜底
			if (--turnsLeft <= 0){
				detach();
			} else {
				spend( TICK );
			}
			return true;
		}

		@Override
		public void detach() {
			if (petalEmitter != null){
				petalEmitter.on = false;
				petalEmitter.kill();
				petalEmitter = null;
			}
			super.detach();
		}

		@Override
		public int icon() {
			return BuffIndicator.HEART_FATE + 1; //心-命运buff的下一个（图标已绘制）
		}

		@Override
		public String name() {
			return "花矢";
		}

		@Override
		public String desc() {
			return "花之箭矢缠绕在你的飞镖上。\n\n拥有花矢效果时，使用涂药飞镖不会消耗耐久。\n\n剩余时长：" + turnsLeft + "回合";
		}

		private static final String TURNS_LEFT = "turns_left";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(TURNS_LEFT, turnsLeft);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			turnsLeft = bundle.getInt(TURNS_LEFT);
		}
	}

}
