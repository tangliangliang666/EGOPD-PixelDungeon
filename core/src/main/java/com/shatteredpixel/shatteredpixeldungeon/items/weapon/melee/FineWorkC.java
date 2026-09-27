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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 良作其三：脊骨链枷（图标：item.png 39行第5个，14x14）。
 * ——数值套用链枷（Flail）：基础上限 35、成长 8 每级；ACC=0.8f（精准惩罚）。
 *   注：无法进行突袭攻击的限制由 Hero.canSurpriseAttack 基于武器类判定，
 *   由于 BodyArtWeapon 不满足原版 Flail 的类检查，此处不复用；
 *   精准惩罚与高伤害面板即代表链枷特性的核心差异。
 */
public class FineWorkC extends FineWork {
	{
		image = ItemSpriteSheet.FINE_WORK_3;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 0.8f;

		ACC = 0.8f; //0.8× 精准（同链枷）
	}

	@Override
	public int max(int lvl) {
		return Math.round(( Math.round(7f * (tier + 1)) +        //35 base（从默认 25 上调，同链枷）
				lvl * Math.round(1.6f * (tier + 1))) * boneMultiplier());  //+8 每级（从默认 +5 上调，同链枷）
	}
}
