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

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.noosa.TextureFilm;

/**
 * 环指自动人偶的专属贴图（doll.png，256×64）。
 * <p>帧大小 13×14，共 8 帧，帧距 13、起点 x=1（非标准网格，故用 TextureFilm.add 手工注册帧矩形）：
 * 帧 0 = 默认帧（无待机动画）；帧 1、2 = 攻击动画；帧 3、4 = 移动动画；帧 5、6、7 = 死亡动画。</p>
 */
public class DollSprite extends MobSprite {

	public DollSprite() {
		super();

		texture( Assets.Sprites.DOLL );

		//doll.png 的帧从 x=1 开始、帧距 13，不是整齐的 13px 网格，手工注册 8 个 13×14 帧矩形
		TextureFilm frames = new TextureFilm( texture );
		for (int i = 0; i < 8; i++){
			frames.add( i, 1 + 13 * i, 0, 14 + 13 * i, 14 );
		}

		idle = new Animation( 1, true );
		idle.frames( frames, 0 ); //无待机动画：仅默认帧

		run = new Animation( 10, true );
		run.frames( frames, 3, 4 );

		attack = new Animation( 15, false );
		attack.frames( frames, 1, 2, 0 ); //末帧回到默认姿势

		die = new Animation( 10, false );
		die.frames( frames, 5, 6, 7 );

		play( idle );
	}
}
