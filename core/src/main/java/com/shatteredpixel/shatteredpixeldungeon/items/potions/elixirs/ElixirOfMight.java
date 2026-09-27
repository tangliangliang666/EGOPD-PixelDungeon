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

package com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

public class ElixirOfMight extends Elixir {

	{
		image = ItemSpriteSheet.ELIXIR_MIGHT;

		unique = true;

		talentFactor = 2f;
	}
	
	@Override
	public void apply( Hero hero ) {
		identify();
		
		hero.STR++;
		hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, "1", FloatingText.STRENGTH);
		
		Buff.affect(hero, HTBoost.class).reset();
		HTBoost boost = Buff.affect(hero, HTBoost.class);
		boost.reset();
		
		hero.updateHT( true );
		GLog.p( Messages.get(this, "msg", hero.STR()) );

		Badges.validateStrengthAttained();
		Badges.validateDuelistUnlock();
	}
	
	public String desc() {
		return Messages.get(this, "desc", HTBoost.boost(Dungeon.hero != null ? Dungeon.hero.HT : 20));
	}
	
	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {
		
		{
			inputs =  new Class[]{PotionOfStrength.class};
			inQuantity = new int[]{1};
			
			cost = 16;
			
			output = ElixirOfMight.class;
			outQuantity = 1;
		}
		
	}
	
	public static class HTBoost extends Buff {
		
		{
			type = buffType.POSITIVE;
		}
		
		private int left;
		
		/** 显式加成（>0 时覆盖默认公式）。由 {@link #setExplicitBonus(int)} 写入，{@link #reset()} 清回公式形态。 */
		private int explicitBonus = 0;
		
		public void reset(){
			left = 5;
			explicitBonus = 0;
		}
		
		/**
		 * 覆盖式写入显式加成（不叠加），并把剩余等级刷回 5 级。
		 *
		 * <p>供中指长兄 T2「纹身铭刻」使用：它给的是「当前生命上限的 25%/50%」这一<b>具体数值</b>，
		 * 无法用本类的固定公式表达，故由外部算好后写入 {@link #explicitBonus}。</p>
		 */
		public void setExplicitBonus( int amount ){
			left = 5;
			explicitBonus = Math.max( 0, amount );
		}
		
		public int boost(){
			if (explicitBonus > 0){
				return explicitBonus;
			}
			return Math.round(left*boost(15 + 5*((Hero)target).lvl)/5f);
		}
		
		public static int boost(int HT){
			return Math.round(4 + HT/20f);
		}
		
		public void onLevelUp(){
			left --;
			if (left <= 0){
				detach();
			}
		}
		
		@Override
		public int icon() {
			return BuffIndicator.HEALING;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(1f, 0.5f, 0f);
		}

		@Override
		public float iconFadePercent() {
			return (5f - left) / 5f;
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(left);
		}
		
		@Override
		public String desc() {
			return Messages.get(this, "desc", boost(), left);
		}
		
		private static String LEFT = "left";
		private static String EXPLICIT_BONUS = "explicit_bonus";
		
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put( LEFT, left );
			bundle.put( EXPLICIT_BONUS, explicitBonus );
		}
		
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			left = bundle.getInt(LEFT);
			//旧存档无该键 → 默认 0（回到公式形态）
			explicitBonus = bundle.getInt(EXPLICIT_BONUS);
		}
	}
}
