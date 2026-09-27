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

package com.shatteredpixel.shatteredpixeldungeon.items.remains;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PhysicalEmpower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

/**
 * 断裂骨刃：环指大师的遗物（原套用战士的纹章残蜡）。
 * <p>效果套用断折剑柄（{@link BrokenHilt}）：使用后获得物理强化
 * （{@link PhysicalEmpower}），接下来近战武器命中时附加额外伤害，
 * 使用后遗物随之消散。</p>
 */
public class BrokenBoneBlade extends RemainsItem {

	{
		image = ItemSpriteSheet.BROKEN_BONE_BLADE;
	}

	@Override
	protected void doEffect(Hero hero) {
		Buff.affect( hero, PhysicalEmpower.class).set(Math.max(2, hero.lvl/3), 2);
		Sample.INSTANCE.play(Assets.Sounds.UNLOCK);
	}

}
