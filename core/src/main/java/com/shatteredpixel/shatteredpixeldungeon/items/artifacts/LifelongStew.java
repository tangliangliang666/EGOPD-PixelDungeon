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

package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 一生炖菜：原版「丰饶号角」的强化形态（2026-09-20）。
 *
 * <h3>新增效果</h3>
 * <p>使用时<b>每消耗 1 充能就恢复 3 点血量</b>（见 {@link #doEatEffect}）。
 * 号角原本只把充能换成饱食度，强化后变成「同时管饱与回血」的应急口粮。</p>
 *
 * <h3>为什么只需要覆写三个方法</h3>
 * <p>贴图在父类里有四处硬编码（吃、被动充能、充能、读档），本次已统一抽成虚方法
 * {@link HornOfPlenty#updateImage()}。所以本类只要覆写它，四处会**一起**换成炖菜的四帧 ——
 * 不必去追那四个点，也不会漏掉某一处导致「吃了之后图标跳回号角」。</p>
 *
 * <p>文本键 {@code items.artifacts.lifelongstew.*}；号角原有的
 * {@code ac_snack/ac_eat/ac_store/eat/prompt/no_food/full/desc_hint/desc_cursed}
 * 等键沿父类链自动继承。</p>
 */
public class LifelongStew extends HornOfPlenty {

	/** 每消耗 1 点充能恢复的血量。 */
	public static final int HEAL_PER_CHARGE = 3;

	{
		image = ItemSpriteSheet.ARTIFACT_LIFELONG_STEW1;
	}

	/**
	 * 换上炖菜的四帧（阈值与父类一致：充能 ≥8/≥5/≥2/其余）。
	 * <p>父类的四处调用点（{@code doEatEffect} / {@code charge} / {@code restoreFromBundle} /
	 * {@code hornRecharge.gainCharge}）全部走这个方法，所以覆写一处即全覆盖。</p>
	 */
	@Override
	protected void updateImage(){
		if (charge >= 8)        image = ItemSpriteSheet.ARTIFACT_LIFELONG_STEW4;
		else if (charge >= 5)   image = ItemSpriteSheet.ARTIFACT_LIFELONG_STEW3;
		else if (charge >= 2)   image = ItemSpriteSheet.ARTIFACT_LIFELONG_STEW2;
		else                    image = ItemSpriteSheet.ARTIFACT_LIFELONG_STEW1;
	}

	@Override
	public void doEatEffect(Hero hero, int chargesToUse){
		//父类负责：满足饱食度、扣充能、进食耗时、天赋/成就、换贴图
		super.doEatEffect(hero, chargesToUse);

		//新增：每消耗 1 充能回 3 血（满血时不回，避免弹一个「+0」的浮字）
		if (chargesToUse > 0 && hero.HP < hero.HT){
			int heal = Math.min(chargesToUse * HEAL_PER_CHARGE, hero.HT - hero.HP);
			int healed = hero.heal( heal );
			if (healed > 0 && hero.sprite != null){
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE,
						Integer.toString(healed), FloatingText.HEALING);
			}
		}
	}

	@Override
	public String desc() {
		return super.desc() + "\n\n" + Messages.get(this, "desc_enhanced");
	}

	//==========================================================================
	// 炼金合成：丰饶号角 + 30 脑啡肽，12 能量
	//==========================================================================

	public static class CraftRecipe extends ArtifactEnhanceRecipe<HornOfPlenty, LifelongStew> {

		@Override
		protected Class<HornOfPlenty> acceptedArtifact(){
			return HornOfPlenty.class;
		}

		@Override
		protected LifelongStew createEnhanced(){
			return new LifelongStew();
		}

		@Override
		protected void transferState(HornOfPlenty source, LifelongStew enhanced){
			enhanced.level( source.level() );
			enhanced.exp = source.exp;
			enhanced.chargeCap = source.chargeCap;
			enhanced.charge = source.charge;
			enhanced.partialCharge = source.partialCharge;
			enhanced.cooldown = source.cooldown;

			enhanced.cursed = source.cursed;
			enhanced.cursedKnown = source.cursedKnown;
			enhanced.levelKnown = source.levelKnown;

			//号角把「已塞进去的食物能量」单独存了一个字段，这里一并搬走，
			//否则玩家喂进去的一半食物会在合成瞬间蒸发（父类的 Level() 会重算 chargeCap）
			enhanced.storedFoodEnergy = source.storedFoodEnergy;

			enhanced.updateImage();
		}
	}
}
