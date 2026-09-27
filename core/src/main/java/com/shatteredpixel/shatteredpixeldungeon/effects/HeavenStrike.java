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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.Random;

/**
 * 失乐园命中特效：长矛从下往上贯穿目标。
 * <p>贴图 heaven.png 内两张 32×32 帧（帧1：(0,0)-(31,31)，帧2：(32,0)-(63,31)），
 * 每次播放时随机选取其中一张。</p>
 * <p>特效对齐怪物脚下：初始高度折叠为 0（底部不动），随后迅速向上展开恢复正常，
 * 并整体淡出，呈现出"长矛穿出"的视觉效果。</p>
 */
public class HeavenStrike extends Image {

	//展开耗时（秒）：高度从 0 迅速恢复到 1
	private static final float EXPAND_TIME = 0.18f;
	//整体存在时长（秒）：展开 + 停留 + 淡出
	private static final float TOTAL_TIME  = 0.45f;

	private float time;

	public HeavenStrike() {
		super(Assets.Sprites.HEAVEN);
		//默认帧1：(0,0)-(31,31)
		frame(texture.uvRect(0, 0, 32, 32));

		//锚点设在底部中心：缩放时底部不动、顶部向上伸展
		origin.set(16f, 32f);
		scale.y = 0f;
	}

	public HeavenStrike reset(int p) {
		revive();

		x = (p % Dungeon.level.width()) * DungeonTilemap.SIZE + (DungeonTilemap.SIZE - 32) / 2;
		//对齐怪物脚下（格子底部边缘），整体上移36像素使其居中于角色
		y = (p / Dungeon.level.width()) * DungeonTilemap.SIZE + DungeonTilemap.SIZE - 36;

		scale.y = 0f;
		alpha(1f);
		//随机选取两张帧中的一张
		if (Random.Int(2) == 0){
			frame(texture.uvRect(0, 0, 32, 32));
		} else {
			frame(texture.uvRect(32, 0, 64, 32));
		}

		time = TOTAL_TIME;
		return this;
	}

	@Override
	public void update() {
		super.update();

		if ((time -= Game.elapsed) <= 0) {
			kill();
		} else {
			float total = TOTAL_TIME;

			//阶段1：高度迅速展开（0 → 1），底部不动顶部向上伸展
			if (time > total - EXPAND_TIME) {
				float p = (total - time) / EXPAND_TIME;
				scale.y = Math.min(1f, p);
			}

			//末尾淡出
			if (time < total * 0.3f) {
				alpha(time / (total * 0.3f));
			}
		}
	}

	//在目标脚下播放特效（挂载到特效层，渲染在角色贴图之上，不会被遮挡）
	public static void hit(Char ch) {
		if (ch != null && ch.sprite != null && ch.sprite.visible){
			GameScene.effect( new HeavenStrike().reset(ch.pos) );
		}
	}

	//在指定格子播放特效
	public static void hit(int pos) {
		GameScene.effect( new HeavenStrike().reset(pos) );
	}
}
