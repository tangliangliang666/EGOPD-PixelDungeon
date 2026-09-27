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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.TempStrength;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

/**
 * 账簿残页：中指长兄的遗物（原套用战士的纹章残蜡）。
 * <p>效果＝获得 {@link #BONUS} 点临时力量、持续 {@link #TURNS} 回合
 * （{@link TempStrength}，覆盖式刷新；计入伤害、装备需求等一切力量判定）。
 * 使用后遗物随之消散（{@link RemainsItem} 通用使用流程）。</p>
 */
public class LedgerPage extends RemainsItem {

	/** 临时力量点数。 */
	public static final int BONUS = 1;
	/** 临时力量持续回合数。 */
	public static final int TURNS = 100;

	{
		image = ItemSpriteSheet.LEDGER_PAGE;
	}

	@Override
	protected void doEffect(Hero hero) {
		TempStrength.apply( hero, BONUS, TURNS );
		hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(BONUS), FloatingText.STRENGTH );
		Sample.INSTANCE.play( Assets.Sounds.UNLOCK );
	}

}
