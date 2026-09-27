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

package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 饰物「沉默的代价」（用户自建，2026-09-08）：一只绿色的时钟。
 * 持有（背包内即生效，无需装备）时提升持有者 X% 的伤害、免伤、精准与闪避，
 * 但每次英雄就绪等待输入超过 B 秒（现实时间）未执行任何"消耗回合"的操作时，
 * 会自动执行一次等待跳过该回合。
 * <p>
 * X（等级 0..3 → 5/10/15/20%）；B 秒（等级 0..3 → 5/4/3/2）。
 * <p>
 * "操作"的唯一定义是消耗回合时间的动作：Hero.spend（回合时间消耗的统一入口）调用
 * {@link #resetIdleClock()} 清零空闲时钟。因此打开背包/查看说明/检查/瞄准取消等
 * 不消耗回合的界面行为都不算操作——点开背包时同样会持续计时并自动跳回合。
 * 例外：瞄准/蓄力进行中（cellSelector 为非默认监听，属已开始执行的操作）会暂停计时，
 * 避免瞄准到一半被强制等待。
 * 空闲时钟的逐帧累计由 {@code GameScene.update()} 调用 {@link #tickIdle(float)} 驱动。
 */
public class SilentPrice extends Trinket {

	{
		image = ItemSpriteSheet.SILENT_PRICE;
	}

	@Override
	protected int upgradeEnergyCost() {
		//6 -> 8(14) -> 10(24) -> 12(36)
		return 6+2*level();
	}

	//该饰物从催化剂获得时即被识别；为保持面板数值可读（含调试台生成），恒视为已识别
	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public String statsDesc() {
		if (isIdentified()){
			return Messages.get(this, "stats_desc", 5*(buffedLvl()+1), 5-buffedLvl());
		} else {
			return Messages.get(this, "typical_stats_desc", 5, 5);
		}
	}

	//================================================================================
	//  数值
	//================================================================================

	/** 该饰物是否正被英雄持有（背包任意位置）。 */
	public static boolean isActive(){
		return trinketLevel(SilentPrice.class) >= 0;
	}

	/** A% 加成乘数：1.05 / 1.10 / 1.15 / 1.20（伤害/精准/闪避）。 */
	private static float statMultiplier( int level ){
		if (level < 0) return 1f;
		return 1f + 0.05f*(level+1);
	}

	/** 免伤乘数：0.95 / 0.90 / 0.85 / 0.80（受伤减少 A%）。 */
	private static float damageTakenMultiplier( int level ){
		if (level < 0) return 1f;
		return 1f - 0.05f*(level+1);
	}

	/** 空闲触发秒数 B：5 / 4 / 3 / 2；未持有返回 -1（禁用）。 */
	private static int idleSeconds( int level ){
		if (level < 0) return -1;
		return 5 - level;
	}

	//================================================================================
	//  供战斗结算 / 帧循环调用的静态钩子（未持有一律返回中性值）
	//================================================================================

	/** 英雄造成伤害提升乘数（挂 Char.damage：src 为英雄时乘）。 */
	public static float heroDamageMultiplier(){
		return statMultiplier(trinketLevel(SilentPrice.class));
	}

	/** 英雄攻击精准提升乘数（挂 Char.hit 的 acuRoll）。 */
	public static float heroAccuracyMultiplier(){
		return statMultiplier(trinketLevel(SilentPrice.class));
	}

	/** 英雄闪避提升乘数（挂 Char.hit 的 defRoll）。 */
	public static float heroEvasionMultiplier(){
		return statMultiplier(trinketLevel(SilentPrice.class));
	}

	/** 英雄受到的伤害减免乘数（挂 Hero.damage，与十戒同乘区）。 */
	public static float damageTakenMultiplier(){
		return damageTakenMultiplier(trinketLevel(SilentPrice.class));
	}

	/** 英雄空闲等待超时秒数；未持有返回 -1（由 GameScene.update 判断不启用）。 */
	public static int idleSeconds(){
		return idleSeconds(trinketLevel(SilentPrice.class));
	}

	//================================================================================
	//  空闲自动等待的时钟
	//  GameScene.update 在"英雄就绪等待输入"的帧里调用 tickIdle 累计；
	//  Hero.spend（一切消耗回合时间的动作入口）调用 resetIdleClock 清零。
	//================================================================================

	//英雄"就绪等待输入"期间累计的现实秒数；饰品不在场时恒为 0。
	private static float idleAccum = 0;

	/**
	 * 空闲计时驱动：每次游戏帧传入本帧现实流逝秒数。
	 * 持有饰品且累计达到 B 秒时清零并返回 true（调用方应让英雄执行一次等待跳过回合）；
	 * 未持有饰品恒返回 false 且累计保持 0。
	 */
	public static boolean tickIdle( float elapsed ){
		if (idleSeconds() < 0){
			idleAccum = 0;
			return false;
		}
		idleAccum += elapsed;
		if (idleAccum >= idleSeconds()){
			idleAccum = 0;
			return true;
		}
		return false;
	}

	/**
	 * 英雄执行了一次消耗回合时间的动作后调用：空闲间隔从头重新计算。
	 * 该饰品不在场时同样安全（累计本为 0）。
	 */
	public static void resetIdleClock(){
		idleAccum = 0;
	}

}
