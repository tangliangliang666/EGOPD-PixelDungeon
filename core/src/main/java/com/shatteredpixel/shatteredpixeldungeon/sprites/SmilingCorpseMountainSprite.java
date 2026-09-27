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
 * 微笑的尸山精灵：同一只精灵在 3 套不同尺寸的贴图/帧网格间切换（验证换皮用）。
 * <ul>
 *   <li>形态 3（满血 2000~3000）：{@code m3.png}，帧 64×48，共 5 帧；</li>
 *   <li>形态 2（1000~1999）：{@code m2.png}，帧 48×48，共 5 帧；</li>
 *   <li>形态 1（0~999）：{@code m1.png}，帧 32×24，共 6 帧。</li>
 * </ul>
 * 各形态帧分配一致（2026-09-09 用户确认）：第 1 帧=idle（单帧循环）、第 2 帧=run（单帧循环）、
 * attack=第 2→3 帧两帧推进、die=套用第 1 帧单帧；其余帧（m2/m3 的 4~5、m1 的 4~6）预留不使用。
 * <p>切换实现：直接更换底层 {@code texture} 并按新尺寸重建 {@link TextureFilm} 与四个动画槽
 * （protected 槽位可重赋值，参照 DM300Sprite.updateChargeState 的模式）。随后<b>先 {@code play(idle)}
 * 再 {@code place}</b>——顺序不可颠倒：{@code Image.texture()} 会把 width/height 临时置为
 * <b>整张贴图</b>尺寸，只有 {@code MovieClip.play()} 才把帧矩形（与宽高）切到<b>单帧</b>；
 * 而 {@code CharSprite.place()}→{@code worldToCamera()} 按当前 width/height 做水平居中与底边贴地，
 * 若在切帧前 place 会按整图/旧帧尺寸对位，导致换皮后贴图与头顶血条整体错位。
 * 死亡动画进行中不会触发形态切换（damage 仅在存活时同步），故无打断 die 的风险。</p>
 */
public class SmilingCorpseMountainSprite extends MobSprite {

	//当前已加载的形态（1~3）；-1 表示尚未建立
	private int phase = -1;

	public SmilingCorpseMountainSprite() {
		super();
		//满血（3000）默认形态 3；link 后会由 Mob.updateSpriteState 按实际 HP 校正
		loadPhase(3);
		play(idle);
	}

	/**
	 * 由 Mob 按 HP 区间驱动：切换到形态 p（1~3）。幂等——形态未变则直接返回。
	 */
	public void setPhase(int p){
		if (p == phase) return;
		loadPhase(p);
		//顺序关键：先 play 切到新形态单帧（刷新 Image 宽高），再 place 重新对位。
		//loadPhase 的 texture() 会把宽高置为整张贴图尺寸，play() 才切回单帧；
		//place()→worldToCamera() 按当前宽高水平居中 + 底边贴地，顺序反了会整体错位。
		play(idle);
		if (ch != null){
			place(ch.pos);      //帧高变化：底边重新贴地
		}
	}

	//载入指定形态的贴图、帧网格与四态动画
	private void loadPhase(int p){
		phase = p;

		String asset;
		int w, h;
		switch (p){
			case 1:
				asset = Assets.Sprites.SMILE_CORPSE_M1;
				w = 32; h = 24;
				break;
			case 2:
				asset = Assets.Sprites.SMILE_CORPSE_M2;
				w = 48; h = 48;
				break;
			case 3:
			default:
				asset = Assets.Sprites.SMILE_CORPSE_M3;
				w = 64; h = 48;
				break;
		}

		texture( asset );
		TextureFilm frames = new TextureFilm( texture, w, h );

		idle = new Animation( 10, true );
		idle.frames( frames, 0 );

		run = new Animation( 10, true );
		run.frames( frames, 1 );

		attack = new Animation( 12, false );
		attack.frames( frames, 1, 2 );

		die = new Animation( 30, false );
		die.frames( frames, 0 );
	}

}
