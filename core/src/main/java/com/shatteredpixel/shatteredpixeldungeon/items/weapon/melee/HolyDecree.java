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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

//圣宣：四阶近战武器，攻击延迟0.25（极其快速）、攻击距离8（极远），打击音效，武技配置套用长鞭（Whip）。
//效果：造成伤害时有50%概率无视防御（见 Char.attack 的 dr 处理）；
//命中时由目标自身的受击逻辑（Char.defenseProc）播放黑、白相间的蝴蝶飞出特效
//（参考原版怪物受击特效的触发方式：效果由"生效单位"自身触发）。
public class HolyDecree extends Whip {

	//本次攻击是否触发了魔法伤害（用于伤害浮字图标显示，每击后复位）
	public static boolean magicHit = false;

	{
		image = ItemSpriteSheet.HOLY_DECREE;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1f;

		tier = 4;
		DLY = 0.25f; //4x speed
		RCH = 8;     //极远的攻击距离
	}

	@Override
	public int min(int lvl) {
		return  1 +  //base
				lvl;   //level scaling
	}

	@Override
	public int max(int lvl) {
		return  5 +  //base
				lvl;   //level scaling
	}

	//黑白蝴蝶混合发射器：每个粒子随机黑（帧16）或白（帧17）
	private static final Emitter.Factory HOLY_BUTTERFLIES = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			Speck p = (Speck)emitter.recycle( Speck.class );
			p.reset( 0, x, y, Random.Int( 2 ) == 0 ? Speck.BUTTERFLY_BLACK : Speck.BUTTERFLY_WHITE );
		}
	};

	//武器显示特效：物品图标（地面/背包/快捷栏）上持续飘出黑白蝴蝶，如同附魔光效
	@Override
	public Emitter emitter() {
		Emitter e = new Emitter();
		e.pos( 8, 8 );
		e.fillTarget = false;
		e.pour( HOLY_BUTTERFLIES, 0.6f );
		return e;
	}

	//目标受击时由其自身播放黑白蝴蝶（由 Char.defenseProc 调用）
	public static void emitButterflies(Char target){
		if (target.sprite != null){
			target.sprite.centerEmitter().start( HOLY_BUTTERFLIES, 0.15f, Random.IntRange( 4, 6 ) );
		}
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		return super.proc(attacker, defender, damage);
	}

}
