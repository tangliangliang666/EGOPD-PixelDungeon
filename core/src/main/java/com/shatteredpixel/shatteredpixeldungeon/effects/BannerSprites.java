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

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.noosa.Image;

public class BannerSprites {

	public enum  Type {
		TITLE_PORT,
		TITLE_GLOW_PORT,
		TITLE_LAND,
		TITLE_GLOW_LAND,
		BOSS_SLAIN,
		GAME_OVER,
	}

	public static Image get( Type type ) {
		Image icon = new Image( Assets.Interfaces.BANNERS );
		switch (type) {
			case TITLE_PORT:
				icon.frame( icon.texture.uvRect( 0, 0, 169, 94 ) );
				break;
			case TITLE_GLOW_PORT:
				//闪烁帧暂为空帧：指向图内一块**完全透明**的区域（尺寸与对应标题帧一致，
				//以后画好闪烁素材时，把这两个 uvRect 改到真实位置即可）。
				//空帧仍会参与 TitleScene 的 alpha 动画与 Blending，只是画不出东西。
				icon.frame( icon.texture.uvRect( 253, 66, 422, 160 ) );
				break;
			case TITLE_LAND:
				icon.frame( icon.texture.uvRect( 176, 0, 427, 65 ) );
				break;
			case TITLE_GLOW_LAND:
				icon.frame( icon.texture.uvRect( 253, 66, 504, 131 ) );
				break;
			case BOSS_SLAIN:
				icon.frame( icon.texture.uvRect( 0, 157, 127, 225 ) );
				break;
			case GAME_OVER:
				icon.frame( icon.texture.uvRect( 128, 157, 256, 192 ) );
				break;
		}
		return icon;
	}
}
