package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfDragonsBlood;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Embers;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * 传火大剑（firelinkgreatsword）：六阶近战，用户自建（2026-09-05）。
 * <p>面板：6+L ~ 35+5L；**内置**烈焰能力（不占用附魔槽，覆盖附魔后仍生效，
 * 见 {@link #proc(Char, Char, int)}，每次攻击按烈焰附魔结算）。
 * 炼金合成：元素余烬 + 巨剑 + 龙血秘药，花费 12 能量（见 {@link CraftRecipe}）。
 * 通关继承：胜利通关时若装备此剑，下一局开局将持有此剑，并继承原剑强化等级的
 * 1/{@link #INHERIT_LEVEL_DIVISOR}（向下取整）与附魔（死亡不触发）。
 * 文本键 items.weapon.melee.firelinkgreatsword.name/desc/stats_desc。</p>
 */
public class FirelinkGreatsword extends MeleeWeapon {

	private static final ItemSprite.Glowing FLAME = new ItemSprite.Glowing( 0xFF4400 );

	{
		image = ItemSpriteSheet.FIRELINK_GREATSWORD;   //图标：37行第12格，24×24
		hitSoundPitch = 0.8f;

		tier = 6;
	}

	@Override
	public int min(int lvl) {
		return 6 + lvl;
	}

	@Override
	public int max(int lvl) {
		return 35 + 5*lvl;
	}

	//内置烈焰：每击按烈焰附魔结算（点燃/对燃烧目标附加火伤），与附魔槽无关——
	//即使后续用附魔卷轴覆盖/更换了附魔，烈焰能力依然生效（可与其它附魔叠加）
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);
		if (defender.isAlive()){
			damage = new Blazing().proc(this, attacker, defender, damage);
		}
		return damage;
	}

	//常驻烈焰橙光（无附魔时也发光；有其它附魔时优先显示该附魔的辉光）
	@Override
	public ItemSprite.Glowing glowing() {
		ItemSprite.Glowing ench = super.glowing();
		return ench != null ? ench : FLAME;
	}

	// ---- 通关继承：胜利通关时装备 → 下一局开局持有，并继承「原剑等级/5（向下取整）+ 附魔」；死亡不触发 ----

	//继承的强化等级 = 原剑等级 / 本常量（整数除法天然向下取整；等级非负，无负数取整问题）
	public static final int INHERIT_LEVEL_DIVISOR = 5;

	public static boolean equippedBy(Hero hero) {
		return equippedInstance(hero) != null;
	}

	//英雄当前装备的传火大剑（主手优先，其次副手）；未装备返回 null
	public static FirelinkGreatsword equippedInstance(Hero hero) {
		if (hero == null) return null;
		if (hero.belongings.weapon() instanceof FirelinkGreatsword) {
			return (FirelinkGreatsword) hero.belongings.weapon();
		}
		if (hero.belongings.secondWep() instanceof FirelinkGreatsword) {
			return (FirelinkGreatsword) hero.belongings.secondWep();
		}
		return null;
	}

	//胜利点（SewerLevel 上地面切换到胜利场景前 / Amulet 使用护符结束游戏）调用：
	//置「下一局继承」标记，并把原剑的强化等级与附魔类名一并写进设置
	public static void markNextRun() {
		FirelinkGreatsword wep = equippedInstance(Dungeon.hero);
		//取 trueLevel()（纯升级等级）而不是 level()——后者含 curseInfusionBonus 这类持久加成，
		//而继承品不带该加成，用 level() 等于白送等级（通用规律：把自身等级当另一表达式基准时必须区分两者）
		int srcLevel = wep != null ? wep.trueLevel() : 0;
		String enchantClass = "";
		if (wep != null && wep.enchantment != null) {
			enchantClass = wep.enchantment.getClass().getName();
		}
		SPDSettings.firelinkNextRun(true);
		SPDSettings.firelinkLevel(srcLevel);
		SPDSettings.firelinkEnchant(enchantClass);
		DeviceCompat.log("FIRELINK", "markNextRun: srcLevel=" + srcLevel + " enchant=" + enchantClass);
	}

	//Dungeon.init 开局初始化（initHero）之后调用：有继承标记则发放并清除标记
	public static void grantAtRunStart() {
		DeviceCompat.log("FIRELINK", "grantAtRunStart: flag=" + SPDSettings.firelinkNextRun());
		if (!SPDSettings.firelinkNextRun()) return;
		SPDSettings.firelinkNextRun(false);

		//取出记录并立即清零，避免残留到再下一局
		int srcLevel = SPDSettings.firelinkLevel();
		String enchantClass = SPDSettings.firelinkEnchant();
		SPDSettings.firelinkLevel(0);
		SPDSettings.firelinkEnchant("");

		FirelinkGreatsword firelink = new FirelinkGreatsword();
		int inheritLevel = srcLevel / INHERIT_LEVEL_DIVISOR;
		if (inheritLevel > 0) firelink.upgrade(inheritLevel);
		Enchantment ench = enchantFromName(enchantClass);
		if (ench != null) firelink.enchant(ench);
		firelink.identify();

		if (firelink.collect(Dungeon.hero.belongings.backpack)){
			DeviceCompat.log("FIRELINK", "grantAtRunStart: granted to backpack, lvl=" + firelink.level()
					+ " enchant=" + (firelink.enchantment != null
							? firelink.enchantment.getClass().getSimpleName() : "none"));
			GLog.p("上一位薪王的余烬化作新的火焰：你获得了_传火大剑_。");
		} else {
			DeviceCompat.log("FIRELINK", "grantAtRunStart: collect failed!");
		}
	}

	//按类名反射重建附魔；空名 / 类不存在 / 不是附魔 / 构造失败 一律返回 null（静默退回无附魔）
	private static Enchantment enchantFromName(String className) {
		if (className == null || className.isEmpty()) return null;
		Class<?> cls = Reflection.forName(className);
		if (cls == null || !Enchantment.class.isAssignableFrom(cls)) return null;
		try {
			Object o = Reflection.newInstance(cls);
			if (o instanceof Enchantment) return (Enchantment) o;
		} catch (Exception e) {
			DeviceCompat.log("FIRELINK", "enchantFromName failed: " + className);
		}
		return null;
	}

	// ---- 炼金合成：元素余烬 + 巨剑 + 龙血秘药，12 能量 ----
	public static class CraftRecipe extends Recipe {

		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			if (ingredients.size() != 3) return false;
			Embers embers = null;
			Greatsword greatsword = null;
			ElixirOfDragonsBlood elixir = null;
			for (Item it : ingredients){
				if (it instanceof Embers){
					embers = (Embers) it;
				} else if (it instanceof Greatsword && it.isIdentified() && !it.cursed){
					greatsword = (Greatsword) it;
				} else if (it instanceof ElixirOfDragonsBlood && !it.cursed){
					elixir = (ElixirOfDragonsBlood) it;
				} else {
					return false;
				}
			}
			return embers != null && greatsword != null && elixir != null;
		}

		@Override
		public int cost(ArrayList<Item> ingredients) {
			return 12;
		}

		@Override
		public Item brew(ArrayList<Item> ingredients) {
			if (!testIngredients(ingredients)) return null;
			Embers embers = null;
			Item greatsword = null;
			Item elixir = null;
			for (Item it : ingredients){
				if (it instanceof Embers) embers = (Embers) it;
				else if (it instanceof Greatsword) greatsword = it;
				else if (it instanceof ElixirOfDragonsBlood) elixir = it;
			}
			embers.quantity(embers.quantity() - 1);   //消耗 1 份元素余烬
			greatsword.quantity(0);
			elixir.quantity(0);
			return new FirelinkGreatsword();
		}

		@Override
		public Item sampleOutput(ArrayList<Item> ingredients) {
			return new FirelinkGreatsword();
		}
	}

}
