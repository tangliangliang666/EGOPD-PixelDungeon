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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDragonsBreath;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 蜡翼 —— 五阶E.G.O武器（合成限定，不进入掉落池与E.G.O重构随机池，可被分解）。
 *
 * <p>面板：5+L ~ 24+6L。
 * 效果：
 * <ol>
 *   <li>对燃烧目标攻击时，本次伤害上限额外提升目标剩余燃烧回合数
 *       （伤害掷点发生在命中前，命中结算时按"0~剩余回合"随机附加等效近似）；</li>
 *   <li>持有（主手或副手）时受到攻击会点燃攻击者（见 Char.defenseProc 的调用）；</li>
 *   <li>每次命中会点燃目标；</li>
 *   <li>若带烈焰附魔（Blazing），则额外获得 200% 附魔强度
 *       （结算见 Weapon.Enchantment#genericProcChanceMultiplier）。</li>
 *   <li>烈焰附魔强度解禁（2026-09-08 修订）：原版中 Blazing 的额外火焰附加伤害需要
 *       附魔强度>1（通常靠奥术之戒）才会对燃烧目标结算；蜡翼凭借上述 +200% 强度
 *       分支（3×）即可达到该门槛，因此_带烈焰附魔的蜡翼_即使不佩戴奥术之戒，
 *       也能由 Blazing.proc 自身对已燃烧目标造成额外火焰伤害。仍要求武器带烈焰附魔。</li>
 * </ol>
 * 炼金合成：烙印工坊 + 火龙吐息合剂 + 嬗变卷轴，花费 20 能量（见 {@link CraftRecipe}）。</p>
 */
public class WaxWing extends MeleeWeapon {

	{
		image = ItemSpriteSheet.WAX_WING;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1f;

		tier = 5;
	}

	@Override
	public int min(int lvl) {
		return 5 + lvl;
	}

	@Override
	public int max(int lvl) {
		return 24 + 6*lvl;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		if (!defender.isAlive()){
			return damage;
		}

		//目标已处于燃烧：伤害上限提升其剩余燃烧回合数 → 掷 0~剩余回合 附加（等价于抬高上限的统计口径）
		Burning burn = defender.buff(Burning.class);
		if (burn != null){
			int turns = (int) burn.turnsLeft();
			if (turns > 0){
				damage += Random.IntRange( 0, turns );
			}
		}

		//每次命中点燃目标
		ignite( defender );

		return damage;
	}

	/** 点燃单位（带火焰粒子）；对火焰免疫单位静默跳过。供命中点燃与受击反燃两处复用。 */
	public static void ignite( Char ch ){
		if (ch != null && ch.isAlive() && !ch.isImmune(Burning.class)){
			Buff.affect( ch, Burning.class ).reignite( ch, 8f );
			ch.sprite.emitter().burst( FlameParticle.FACTORY, 6 );
		}
	}

	// ---- 炼金合成：烙印工坊 + 火龙吐息合剂 + 嬗变卷轴，20 能量 ----
	public static class CraftRecipe extends Recipe {

		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			if (ingredients.size() != 3) return false;
			BrandWorkshop brand = null;
			PotionOfDragonsBreath breath = null;
			ScrollOfTransmutation scroll = null;
			for (Item it : ingredients){
				if (it instanceof BrandWorkshop && it.isIdentified() && !it.cursed){
					brand = (BrandWorkshop) it;
				} else if (it instanceof PotionOfDragonsBreath && !it.cursed){
					breath = (PotionOfDragonsBreath) it;
				} else if (it instanceof ScrollOfTransmutation && !it.cursed){
					scroll = (ScrollOfTransmutation) it;
				} else {
					return false;
				}
			}
			return brand != null && breath != null && scroll != null;
		}

		@Override
		public int cost(ArrayList<Item> ingredients) {
			return 20;
		}

		@Override
		public Item brew(ArrayList<Item> ingredients) {
			if (!testIngredients(ingredients)) return null;
			Item brand = null;
			Item breath = null;
			Item scroll = null;
			for (Item it : ingredients){
				if (it instanceof BrandWorkshop) brand = it;
				else if (it instanceof PotionOfDragonsBreath) breath = it;
				else if (it instanceof ScrollOfTransmutation) scroll = it;
			}
			brand.quantity( brand.quantity() - 1 );
			breath.quantity( breath.quantity() - 1 );
			scroll.quantity( scroll.quantity() - 1 );
			return new WaxWing();
		}

		@Override
		public Item sampleOutput(ArrayList<Item> ingredients) {
			return new WaxWing();
		}
	}

}
