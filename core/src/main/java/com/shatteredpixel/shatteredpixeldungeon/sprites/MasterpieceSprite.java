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
 * 「杰作」单位的专属贴图（masterpiece.png，2026-09-04）。
 * <p>注意：masterpiece.png 现为 <b>128×128</b> POT 画布，杰作绘制在<b>左上 80×64</b> 区域
 * （单帧、无动画）——原 80×64 非 2 的幂纹理在部分设备上传失败会渲染空白，必须保持 POT。</p>
 */
public class MasterpieceSprite extends MobSprite {

	public MasterpieceSprite() {
		super();

		texture( Assets.Sprites.MASTERPIECE );

		//单帧：杰作位于 128×128 画布的左上 80×64 区域
		TextureFilm frames = new TextureFilm( texture );
		frames.add( 0, 0, 0, 80, 64 );

		//杰作不动、不攻击、不会死亡：各动画均用独立的单帧循环静帧
		//（不共享同一 Animation 实例，避免被外部当作非循环攻击/死亡动画触发完成回调）
		idle = new Animation( 1, true );
		idle.frames( frames, 0 );

		run = new Animation( 1, true );
		run.frames( frames, 0 );

		attack = new Animation( 1, true );
		attack.frames( frames, 0 );

		die = new Animation( 1, true );
		die.frames( frames, 0 );

		play( idle );
	}
}
