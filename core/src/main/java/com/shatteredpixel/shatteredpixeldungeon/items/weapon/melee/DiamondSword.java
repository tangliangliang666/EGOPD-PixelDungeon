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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Crab;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Scorpio;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Spinner;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Swarm;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 四阶武器「钻石剑」（2026-09-04 MC 彩蛋）。
 * 铁匠任务结算时若背包中有 2 颗钻石则奖励。伤害公式：5+L ~ 30+6L。
 *
 * <p>2026-09-06 MC 专属附魔系统：
 * 这把剑无法适用任何普通附魔（附魔卷轴不可选、enchant() 被拦截为 no-op），
 * 只能通过巨魔铁匠处消耗 1 颗钻石执行一次「Minecraft 附魔台工艺」
 * （附魔等级 30、附魔能力 10 → 修正附魔等级约 26~40）来获得/成长专属附魔。
 *
 * <p>六种专属附魔（等级与上限）：
 * 锋利 1~5（额外伤害 等级×5%）、
 * 亡灵杀手 1~5（对亡灵/恶魔额外 等级×8%，范围同棱光法杖增伤判定）、
 * 节肢杀手 1~5（对螃蟹/蜘蛛/苍蝇/蝎子额外 等级×8% 并施加 5 回合残废）、
 * 火焰附加 1~2（等效同等级烈焰附魔）、
 * 击退 1~2（等效同等级弹性附魔）、
 * 抢夺 1~3（等效同等级幸运附魔）。
 * 锋利/亡灵杀手/节肢杀手三选一互斥；抽取时不会选中与已有附魔冲突的魔咒。
 */
public class DiamondSword extends MeleeWeapon {

	{
		image = ItemSpriteSheet.DIAMOND_SWORD;

		tier = 4;
	}

	@Override
	public int min(int lvl) {
		return 5 + lvl;   //5+L
	}

	@Override
	public int max(int lvl) {
		return 30 + 6*lvl; //30+6L
	}

	//============ MC 专属附魔等级（0 = 无） ============

	public int sharpness;      //锋利，上限5
	public int smite;          //亡灵杀手，上限5
	public int baneArthropod;  //节肢杀手，上限5
	public int fireAspect;     //火焰附加，上限2
	public int knockback;      //击退，上限2
	public int looting;        //抢夺，上限3

	private static final String SHARPNESS   = "sharpness";
	private static final String SMITE       = "smite";
	private static final String BANE        = "baneArthropod";
	private static final String FIRE_ASPECT = "fireAspect";
	private static final String KNOCKBACK   = "knockback";
	private static final String LOOTING     = "looting";

	//每类附魔的等级上限（与需求一致）
	private static int cap(int type){
		switch (type){
			case 0: return 5; //锋利
			case 1: return 5; //亡灵杀手
			case 2: return 5; //节肢杀手
			case 3: return 2; //火焰附加
			case 4: return 2; //击退
			default: return 3; //抢夺
		}
	}

	//============ MC 附魔工艺的候选构建 ============

	//MC 剑类稀有度权重：锋利10 / 亡灵杀手5 / 节肢杀手5 / 击退5 / 火焰附加2 / 抢夺2
	private static final int[] ENCHANT_WEIGHTS = {10, 5, 5, 5, 2, 2};
	//每类各级的 [min,max] 修正附魔等级范围（MC Java 版）；行=类型(0锋利..5抢夺)，列按等级 1..cap 排列
	private static final int[][] ENCHANT_RANGES = {
			//锋利 1~5
			{1,21, 12,32, 23,43, 34,54, 45,65},
			//亡灵杀手 1~5（同节肢杀手）
			{5,25, 13,33, 21,41, 29,49, 37,57},
			//节肢杀手 1~5
			{5,25, 13,33, 21,41, 29,49, 37,57},
			//火焰附加 1~2
			{10,60, 30,80},
			//击退 1~2
			{5,55, 25,75},
			//抢夺 1~3
			{15,65, 24,74, 33,83}
	};

	/** 某魔咒是否与"剑上当前拥有的附魔"冲突（锋利/亡灵杀手/节肢杀手三选一互斥） */
	private boolean conflictsWithOwned(int type){
		switch (type){
			case 0: return smite > 0 || baneArthropod > 0;     //锋利 × 亡灵杀手/节肢杀手
			case 1: return sharpness > 0 || baneArthropod > 0; //亡灵杀手 × 锋利/节肢杀手
			case 2: return sharpness > 0 || smite > 0;         //节肢杀手 × 锋利/亡灵杀手
			default: return false;
		}
	}

	/** 给定修正附魔等级 cPrime，某类型在等级上限内可出现的最高等级；cPrime 不在任何区间内则返回 0 */
	private static int rolledLevelAt(int type, int cPrime){
		int rolled = 0;
		int[] range = ENCHANT_RANGES[type];
		for (int lvl = 1; lvl <= cap(type); lvl++){
			if (cPrime >= range[(lvl-1)*2] && cPrime <= range[(lvl-1)*2 + 1]){
				rolled = lvl;
			}
		}
		return rolled;
	}

	/**
	 * 按"剑上当前附魔状态 + 修正附魔等级 cPrime"构建候选池 {type, rolledLevel}。
	 * 每一轮（含附魔中途的继续轮）都必须重建：剔除本次已抽类型、与剑上当前附魔冲突
	 * （含本轮刚获得/升级的）的类型、已达上限的类型，并按最新 cPrime 重算可出现的等级。
	 */
	private ArrayList<int[]> enchantPool(int cPrime, boolean[] pickedThisOp){
		ArrayList<int[]> pool = new ArrayList<>();
		for (int type = 0; type < 6; type++){
			if (pickedThisOp[type]) continue;       //本次操作已抽过，不再重复
			if (conflictsWithOwned(type)) continue; //与已拥有（含本次新增）冲突 → 不可出现
			if (levelOf(type) >= cap(type)) continue; //已达等级上限 → 无法再成长
			int rolled = rolledLevelAt(type, cPrime);
			if (rolled > 0){
				pool.add(new int[]{type, rolled});
			}
		}
		return pool;
	}

	//当前等级（按 type 索引）
	private int levelOf(int type){
		switch (type){
			case 0: return sharpness;
			case 1: return smite;
			case 2: return baneArthropod;
			case 3: return fireAspect;
			case 4: return knockback;
			default: return looting;
		}
	}

	private void addLevel(int type, int delta){
		switch (type){
			case 0: sharpness += delta; break;
			case 1: smite += delta; break;
			case 2: baneArthropod += delta; break;
			case 3: fireAspect += delta; break;
			case 4: knockback += delta; break;
			default: looting += delta; break;
		}
	}

	//============ 序列化 ============

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(SHARPNESS, sharpness);
		bundle.put(SMITE, smite);
		bundle.put(BANE, baneArthropod);
		bundle.put(FIRE_ASPECT, fireAspect);
		bundle.put(KNOCKBACK, knockback);
		bundle.put(LOOTING, looting);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		sharpness      = bundle.getInt(SHARPNESS);
		smite          = bundle.getInt(SMITE);
		baneArthropod  = bundle.getInt(BANE);
		fireAspect     = bundle.getInt(FIRE_ASPECT);
		knockback      = bundle.getInt(KNOCKBACK);
		looting        = bundle.getInt(LOOTING);
		//互斥自愈（2026-09-06 修复）：旧版本可能在同一次附魔里同时抽到锋利/亡灵杀手/节肢杀手，
		//读档时同组只保留优先级最高者（锋利 > 亡灵杀手 > 节肢杀手，与 proc 的 else-if 一致），其余清零
		if (sharpness > 0){
			smite = 0;
			baneArthropod = 0;
		} else if (smite > 0){
			baneArthropod = 0;
		}
	}

	//============ 封禁普通附魔 ============

	//本剑不适用专属附魔之外的任何附魔：拦截一切普通附魔写入
	@Override
	public Weapon enchant(Enchantment ench) {
		return this;
	}

	@Override
	public Weapon enchant() {
		return this;
	}

	//============ 战斗结算：专属附魔 proc ============

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		if (defender == null || !defender.isAlive()) return damage;
		//附魔效果在魔法免疫时同普通附魔一样失效
		if (attacker.buff(MagicImmune.class) != null) return damage;

		//—— 攻击系增伤（锋利/亡灵杀手/节肢杀手 三选一互斥）——
		int extra = 0;
		if (sharpness > 0){
			extra = Math.round(damage * 0.05f * sharpness);
		} else if (smite > 0){
			if (hasProp(defender, Char.Property.UNDEAD) || hasProp(defender, Char.Property.DEMONIC)){
				extra = Math.round(damage * 0.08f * smite);
			}
		} else if (baneArthropod > 0){
			if (defender instanceof Crab
					|| defender instanceof Spinner
					|| defender instanceof Swarm
					|| defender instanceof Scorpio){
				extra = Math.round(damage * 0.08f * baneArthropod);
				Buff.affect(defender, Cripple.class, 5f);
			}
		}
		damage += extra;

		if (damage > 0){
			//—— 火焰附加：等效同等级烈焰附魔 ——
			if (fireAspect > 0){
				int lvl = fireAspect;
				// lvl 0 - 33% / 1 - 50% / 2 - 60%（与原版烈焰附魔同公式，等级 = 火焰附加等级）
				float procChance = (lvl+1f)/(lvl+3f) * Weapon.Enchantment.genericProcChanceMultiplier(attacker);
				if (Random.Float() < procChance){
					float powerMulti = Math.max(1f, procChance);
					if (defender.buff(Burning.class) == null){
						Buff.affect(defender, Burning.class).reignite(defender, 8f);
						powerMulti -= 1;
					}
					if (powerMulti > 0){
						int burnDamage = Random.NormalIntRange(1, 3 + Dungeon.scalingDepth()/4);
						burnDamage = Math.round(burnDamage * 0.67f * powerMulti);
						if (burnDamage > 0) defender.damage(burnDamage, this);
					}
					defender.sprite.emitter().burst(FlameParticle.FACTORY, lvl + 1);
				}
			}

			//—— 击退：等效同等级弹性附魔 ——
			if (knockback > 0 && defender.isAlive()){
				int lvl = knockback;
				// lvl 0 - 20% / 1 - 33% / 2 - 43%
				float procChance = (lvl+1f)/(lvl+5f) * Weapon.Enchantment.genericProcChanceMultiplier(attacker);
				if (Random.Float() < procChance){
					float powerMulti = Math.max(1f, procChance);
					Ballistica trajectory = new Ballistica(attacker.pos, defender.pos, Ballistica.STOP_TARGET);
					trajectory = new Ballistica(trajectory.collisionPos, trajectory.path.get(trajectory.path.size()-1), Ballistica.PROJECTILE);
					WandOfBlastWave.throwChar(defender, trajectory, Math.round(2 * powerMulti), true, true, this);
				}
			}

			//—— 抢夺：等效同等级幸运附魔 ——
			if (looting > 0 && defender.isAlive()){
				int lvl = looting;
				// lvl 0 - 10% / 1 - 12% / 2 - 14% / 3 - 16%
				float procChance = (lvl+4f)/(lvl+40f) * Weapon.Enchantment.genericProcChanceMultiplier(attacker);
				if (Random.Float() < procChance){
					float powerMulti = Math.max(1f, procChance);
					//与幸运附魔 LuckProc 完全相同的标记（ringLevel 越大掉落越好）
					Lucky.LuckProc lp = Buff.affect(defender, Lucky.LuckProc.class);
					lp.setRingLevel(-10 + Math.round(5*powerMulti));
				} else {
					if (defender.buff(Lucky.LuckProc.class) != null){
						defender.buff(Lucky.LuckProc.class).detach();
					}
				}
			}
		}

		return damage;
	}

	private static boolean hasProp(Char ch, Char.Property p){
		return ch.properties().contains(p);
	}

	//============ MC 附魔台工艺（铁匠处 1 钻石触发一次） ============

	/**
	 * 复刻 Minecraft 附魔台一次附魔的完整随机工序（Java 版规则）：
	 * 附魔等级 c = 30、附魔能力 l = 10（钻石），
	 * 修正附魔等级 c' ≈ 26~40（含附魔能力三角修正与 ±15% 波动）；
	 * 按「修正等级范围表」与稀有度权重加权抽取；
	 * 有 (c'+1)/50 概率继续（Java 版：随机 [0,49]，≤ c' 则继续），继续则 c' 折半。
	 * 每一轮都用最新 c' 重建候选池：剔除"本次已抽"、与剑上当前附魔（含本轮刚获得/
	 * 升级的）冲突的类型——锋利/亡灵杀手/节肢杀手三选一互斥由此保证同轮绝不同时出现。
	 *
	 * <p>成长规则：剑上已有同种附魔 → 等级 +1（不超过上限）；
	 * 尚未拥有 → 直接赋予本次抽到的等级。
	 *
	 * @return 本次附魔产生的描述行（每行一条结果）；无可附魔时返回空列表。
	 */
	public ArrayList<String> enchantOnce(){
		ArrayList<String> lines = new ArrayList<>();

		//1. 修正附魔等级 c'（c=30, l=10）
		int c = 30;
		int l = 10;
		int cPrime = c + 1 + Random.Int(l/4 + 1) + Random.Int(l/4 + 1);          //+randInt(3)+randInt(3) → 31~35
		cPrime = Math.round(cPrime * (1f + 0.15f*(Random.Float() + Random.Float() - 1f))); //±15% 三角波动
		cPrime = Math.max(1, cPrime);

		//2. 加权抽取 + 有几率继续（MC Java 版：随机 [0,49] ≤ c' 则继续，随后 c' 折半）
		//   候选池每轮都重建——互斥（锋利/亡灵杀手/节肢杀手三选一）与"本次已抽不重复"
		//   均在重建时判定：拿到锋利后，亡灵杀手/节肢杀手会立即从候选中剔除，
		//   因此一次附魔过程里绝不会同时出现互斥的两种攻击附魔
		boolean[] pickedThisOp = new boolean[6];
		ArrayList<int[]> pool = enchantPool(cPrime, pickedThisOp);

		while (!pool.isEmpty()){
			//按稀有度权重随机抽一个
			int totalW = 0;
			int[] typeWs = new int[pool.size()];
			for (int i = 0; i < pool.size(); i++){
				typeWs[i] = ENCHANT_WEIGHTS[pool.get(i)[0]];
				totalW += typeWs[i];
			}
			int roll = Random.Int(totalW);
			int idx = 0;
			for (int i = 0; i < typeWs.length; i++){
				roll -= typeWs[i];
				if (roll < 0){ idx = i; break; }
			}

			int type = pool.get(idx)[0];
			int rolledLvl = pool.get(idx)[1];
			pickedThisOp[type] = true;
			int cur = levelOf(type);

			if (cur <= 0){
				//尚未拥有：赋予本次抽中的等级
				addLevel(type, rolledLvl);
				lines.add(enchantName(type) + (rolledLvl > 1 ? " " + toRoman(rolledLvl) : "") + "（新获得）");
			} else if (cur < cap(type)){
				//已有同种：等级 +1（重复成长，不超过上限）；1 级同样不显示罗马数字
				addLevel(type, 1);
				lines.add(enchantName(type) + (cur > 1 ? " " + toRoman(cur) : "") + " → " + toRoman(cur+1));
			}

			//是否有机会继续：随机 [0,49] ≤ c' 则继续，随后 c' 折半并重建候选池
			if (Random.Int(50) > cPrime) break;
			cPrime = cPrime / 2;
			pool = enchantPool(cPrime, pickedThisOp);
		}

		Item.updateQuickslot();
		return lines;
	}

	/** 是否仍可附魔（任一专属附魔未满级且互斥允许） */
	public boolean canEnchantMore(){
		for (int type = 0; type < 6; type++){
			if (conflictsWithOwned(type)) continue; //互斥组内已有其它攻击附魔
			if (levelOf(type) < cap(type)) return true;
		}
		return false;
	}

	/** 是否已拥有任意专属附魔（用于显示 MC 流光） */
	public boolean hasEnchants(){
		return sharpness > 0 || smite > 0 || baneArthropod > 0
				|| fireAspect > 0 || knockback > 0 || looting > 0;
	}

	//============ 展示 ============

	public static String enchantName(int type){
		switch (type){
			case 0: return "锋利";
			case 1: return "亡灵杀手";
			case 2: return "节肢杀手";
			case 3: return "火焰附加";
			case 4: return "击退";
			default: return "抢夺";
		}
	}

	/** 附魔等级的罗马数字后缀：2→II 3→III 4→IV 5→V；1级不显示 */
	public static String toRoman(int level){
		switch (level){
			case 2: return "II";
			case 3: return "III";
			case 4: return "IV";
			case 5: return "V";
			default: return "";
		}
	}

	//附魔说明（显示在属性统计区；无附魔时返回空串）
	public String enchantInfo(){
		StringBuilder b = new StringBuilder("这把武器具有附魔：");

		boolean any = false;
		String[] names = {"锋利", "亡灵杀手", "节肢杀手", "火焰附加", "击退", "抢夺"};
		int[] levels = {sharpness, smite, baneArthropod, fireAspect, knockback, looting};

		for (int i = 0; i < 6; i++){
			if (levels[i] > 0){
				b.append("\n\n_").append(names[i]).append(toRoman(levels[i])).append("_");
				any = true;
			}
		}

		return any ? b.toString() : "";
	}

	@Override
	public String statsInfo() {
		String ench = enchantInfo();
		if (!ench.equals("")){
			return ench;
		}
		return super.statsInfo();
	}

}
