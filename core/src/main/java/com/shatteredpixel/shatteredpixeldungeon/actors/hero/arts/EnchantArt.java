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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.arts;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.UnstableBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfHoneyedHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfMagicalSight;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfSirensSong;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blooming;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Chilling;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Corrupting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Elastic;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Grim;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Projecting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Unstable;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Vampiric;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBodyEnchant;

import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * 专精技艺「附魔」（tier 2，艺术之巅专精，天赋「学习未知之物」+1 解锁）：
 * 打开附魔窗口，消耗 1 个魔质素材 + 1 个配方物品，为一件人体派作品附加附魔。
 * <p>天赋门控（2026-09-03 调整）：+1 解锁_普通_附魔（common）；
 * +2 额外解锁_稀有_附魔（uncommon）；+3 额外解锁_罕见_附魔（rare）。
 * 诅咒附魔不在本技艺提供范围内。</p>
 * <p>附魔可叠加：目标已有主附魔槽（enchantment）时，新附魔追加进
 * {@link com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BodyArtWeapon}
 * 的附加附魔列表（extraEnchants），每次攻击在主附魔后依次结算。</p>
 * <p>配方映射：{@link #RECIPES}（配方物品类 → 附魔类）；未注册的配方物品按当前
 * 天赋允许的级别随机施加。</p>
 */
public class EnchantArt extends RingMasterArt {

	public static final EnchantArt INSTANCE = new EnchantArt();

	private EnchantArt(){}

	/**
	 * 附魔配方注册表：配方物品类 → 附魔类（由用户指定，2026-09-03 填充 13 种）。
	 * 未注册的配方物品触发按天赋级别的随机附魔。
	 */
	public static final LinkedHashMap<Class<? extends Item>, Class<? extends Weapon.Enchantment>> RECIPES
			= new LinkedHashMap<>();

	static {
		// --- 普通附魔（common，+1 解锁） ---
		RECIPES.put( PotionOfLiquidFlame.class,  Blazing.class );   //烈焰 → 火焰药水
		RECIPES.put( PotionOfFrost.class,        Chilling.class );  //寒霜 → 冰霜药水
		RECIPES.put( PotionOfParalyticGas.class, Kinetic.class );   //恒动 → 麻痹药水
		RECIPES.put( ScrollOfRecharging.class,   Shocking.class );  //电击 → 充能卷轴
		// --- 稀有附魔（uncommon，+2 解锁） ---
		RECIPES.put( PotionOfShielding.class,        Blocking.class );    //招架 → 奥术护盾合剂
		RECIPES.put( PotionOfHealing.class,         Blooming.class );    //繁茂 → 生命药水
		RECIPES.put( PotionOfLevitation.class,      Elastic.class );     //弹性 → 浮空药水
		RECIPES.put( ElixirOfHoneyedHealing.class,  Lucky.class );       //幸运 → 圣愈蜜药
		RECIPES.put( PotionOfMagicalSight.class,    Projecting.class );  //索敌 → 魔能透视合剂
		RECIPES.put( UnstableBrew.class,            Unstable.class );    //紊乱 → 紊乱魔药
		// --- 罕见附魔（rare，+3 解锁） ---
		RECIPES.put( ScrollOfSirensSong.class,  Corrupting.class );  //腐化 → 魅音秘卷
		RECIPES.put( ScrollOfPsionicBlast.class, Grim.class );        //死神 → 灵爆秘卷
		RECIPES.put( PotionOfCleansing.class,   Vampiric.class );     //血饮 → 全面净化合剂
	}

	@Override
	public int tier() {
		return 2;
	}

	@Override
	public boolean canPerform( Hero hero ) {
		return hero.pointsInTalent( Talent.LEARN_UNKNOWN ) >= 1;
	}

	@Override
	public void onPerform( Hero hero ) {
		GameScene.show( new WndBodyEnchant( hero ) );
	}

	@Override
	public String name() {
		return "附魔";
	}

	@Override
	public String shortDesc() {
		return "消耗魔质与配方物品，为人体派作品定向附加附魔";
	}

	@Override
	public String desc() {
		return "艺术之巅钻研作品中蕴藏的未知之性，能将物品的特性以附魔的形式融入作品。\n\n"
				+ "花费_1_个魔质素材和_1_个对应配方物品，为一件人体派作品附加附魔：\n"
				+ "_+1：_普通附魔；_+2：_普通、稀有附魔；_+3：_普通、稀有、罕见附魔。\n\n"
				+ "_附魔可以叠加_：为已有附魔的作品再次附魔时，新旧附魔会同时生效。\n\n"
				+ "需要天赋「学习未知之物」_+1_。";
	}

	@Override
	public int icon() {
		//暂复用艺术之巅职业图标（hero_icons 第 2 行第 8 列），待绘制专属图标后替换
		return HeroIcon.ART_PINNACLE;
	}

	/**
	 * 附魔级别：1=普通（common） 2=稀有（uncommon） 3=罕见（rare）。
	 * 诅咒附魔返回 -1（本技艺不提供）；未知类型同样返回 -1（拦截）。
	 * 天赋门控：+1 允许 1；+2 允许 1~2；+3 允许 1~3（maxTier = 天赋点数）。
	 */
	public static int enchantTier( Class<? extends Weapon.Enchantment> c ){
		if (Arrays.asList( Weapon.Enchantment.common ).contains( c ))   return 1;
		if (Arrays.asList( Weapon.Enchantment.uncommon ).contains( c )) return 2;
		if (Arrays.asList( Weapon.Enchantment.rare ).contains( c ))     return 3;
		return -1; //诅咒附魔及其他：艺术之巅附魔技艺不提供
	}
}
