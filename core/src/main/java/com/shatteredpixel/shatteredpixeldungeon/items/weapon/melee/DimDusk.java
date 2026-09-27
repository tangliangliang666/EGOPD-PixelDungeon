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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Enkephalin;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRage;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

import java.util.ArrayList;

/**
 * 薄暝：六阶E.G.O武器（超大贴图20×20）。
 * <p>面板 (6+L ~ 10+2×L)。每次攻击造成4次伤害：物理（正常计算防御）、
 * 法术（无视防御）、火焰（无视防御、不引燃、水中伤害减少）、
 * 雷电（无视防御、水中伤害增加）。装备时视野中的所有敌人每回合受到
 * 2点物理/法术/火焰/雷电伤害。</p>
 * <p>不加入生成池，仅可通过炼金合成：30脑啡肽 + 一把五阶武器 + 复仇卷轴。</p>
 */
public class DimDusk extends MeleeWeapon {

	{
		image = ItemSpriteSheet.DIM_DUSK;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1f;

		tier = 6;
	}

	@Override
	public int min(int lvl) {
		return 6 + lvl;
	}

	@Override
	public int max(int lvl) {
		return 10 + 2*lvl;
	}

	//法术伤害来源标记（抗魔可减免，伤害浮字显示法术图标）
	public static class MagicStrike {}
	//火焰伤害来源标记（伤害浮字显示火焰图标）
	public static class FlameStrike {}

	@Override
	public void activate(Char ch) {
		super.activate(ch);
		//装备时挂载灼伤光环
		if (ch instanceof Hero){
			Buff.affect(ch, DimDuskAura.class);
		}
	}

	@Override
	public boolean doUnequip(Hero hero, boolean collect, boolean single) {
		if (super.doUnequip(hero, collect, single)){
			Buff.detach(hero, DimDuskAura.class);
			return true;
		} else {
			return false;
		}
	}

	//每次攻击造成4次伤害：物理（主攻击）+ 法术/火焰/雷电
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		if (attacker.buff(MagicImmune.class) == null && defender.isAlive()){
			int strike = Math.max(1, damageRoll(attacker));
			boolean inWater = Dungeon.level.water[defender.pos];

			//法术伤害（无视防御）
			defender.damage( strike, new MagicStrike() );
			//目标已死亡则停止后续伤害（避免对尸体继续造成伤害，
			//防止 boss 的 damage() 覆写/击杀逻辑被重复触发）
			if (!defender.isAlive()) return damage;
			//火焰伤害（无视防御，不引燃；水中伤害减少）
			defender.damage( inWater ? Math.round(strike * 0.5f) : strike, new FlameStrike() );
			if (!defender.isAlive()) return damage;
			//雷电伤害（无视防御；水中伤害增加）
			defender.damage( inWater ? Math.round(strike * 1.5f) : strike, new Electricity() );
		}

		return damage;
	}

	//灼伤光环：装备时视野中的所有敌人每回合受到 2 点物理/法术/火焰/雷电伤害
	public static class DimDuskAura extends Buff {

		{
			type = buffType.NEUTRAL;
		}

		@Override
		public boolean act() {
			if (target instanceof Hero){
				Hero hero = (Hero) target;
				//若薄暝已不在身上则自行移除（兜底）
				if (!(hero.belongings.weapon() instanceof DimDusk)
						&& !(hero.belongings.secondWep() instanceof DimDusk)){
					detach();
					return true;
				}

				for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
					if (mob.alignment == Char.Alignment.ENEMY
							&& Dungeon.level.heroFOV[mob.pos]
							&& mob.isAlive()){
						boolean inWater = Dungeon.level.water[mob.pos];
						//物理2点（正常计算防御）
						mob.damage( Math.max(0, 2 - mob.drRoll()), hero );
						//目标已死亡则停止后续伤害（避免对尸体继续造成伤害，
						//防止 boss 的 damage() 覆写（如 Goo 重新 seal()）被重复触发）
						if (!mob.isAlive()) continue;
						//法术/火焰/雷电各2点（火焰水中减少，雷电水中增加）
						mob.damage( 2, new MagicStrike() );
						if (!mob.isAlive()) continue;
						mob.damage( inWater ? 1 : 2, new FlameStrike() );
						if (!mob.isAlive()) continue;
						mob.damage( inWater ? 3 : 2, new Electricity() );
					}
				}
			}
			spend( TICK );
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.NONE;
		}
	}

	@Override
	public String name() {
		return "薄暝";
	}

	//合成配方：30脑啡肽 + 五阶武器 + 复仇卷轴
	public static class CraftRecipe extends Recipe {

		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			if (ingredients.size() != 3) return false;
			Enkephalin e = null;
			MeleeWeapon weapon = null;
			Scroll scroll = null;
			for (Item it : ingredients){
				if (it instanceof Enkephalin){
					e = (Enkephalin) it;
				} else if (it instanceof MeleeWeapon && it.isIdentified() && !it.cursed
						&& ((MeleeWeapon) it).tier == 5){
					weapon = (MeleeWeapon) it;
				} else if (it instanceof ScrollOfRage && !it.cursed){
					scroll = (Scroll) it;
				} else {
					return false;
				}
			}
			return e != null && weapon != null && scroll != null && e.quantity() >= 30;
		}

		@Override
		public int cost(ArrayList<Item> ingredients) {
			return 0;
		}

		@Override
		public Item brew(ArrayList<Item> ingredients) {
			if (!testIngredients(ingredients)) return null;
			Enkephalin e = null;
			MeleeWeapon weapon = null;
			Item scroll = null;
			for (Item it : ingredients){
				if (it instanceof Enkephalin) e = (Enkephalin) it;
				else if (it instanceof MeleeWeapon) weapon = (MeleeWeapon) it;
				else scroll = it;
			}
			e.quantity(e.quantity() - 30);
			weapon.quantity(0);
			scroll.quantity(0);
			return new DimDusk();
		}

		@Override
		public Item sampleOutput(ArrayList<Item> ingredients) {
			return new DimDusk();
		}
	}

}
