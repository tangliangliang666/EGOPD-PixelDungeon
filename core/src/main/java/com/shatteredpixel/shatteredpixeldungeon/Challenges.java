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

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;

public class Challenges {

	//Some of these internal IDs are outdated and don't represent what these challenges do
	public static final int NO_FOOD				= 1;
	public static final int NO_ARMOR			= 2;
	public static final int NO_HEALING			= 4;
	public static final int NO_HERBALISM		= 8;
	public static final int SWARM_INTELLIGENCE	= 16;
	public static final int DARKNESS			= 32;
	public static final int NO_SCROLLS		    = 64;
	public static final int CHAMPION_ENEMIES	= 128;
	public static final int STRONGER_BOSSES 	= 256;
	public static final int DEMOLITION_SQUAD	= 512; //拆迁办（2026-09-04）
	public static final int JELLY_PERSON		= 1024; //依旧果冻人（2026-09-15）：纯视觉挑战，受击/物品落地时以脚底为基准左右扭曲摆动
	public static final int DEBUG_MODE			= 2048; //调试模式（2026-09-06）：分数倍率恒为 0，且启用后调试窗口按钮/F2 才可用
	public static final int NARCISSUS_TRACING	= 4096; //水仙追迹（2026-09-23）：分数倍率恒为 0；开局携带水仙十字圣剑（背包）+ 财富戒指（背包）

	/**
	 * 「趣味挑战」的位掩码：拆迁办 / 依旧果冻人 / 水仙追迹 / 调试模式。
	 *
	 * <p>2026-09-24 起把这四条从「常规挑战」移出、单列为**趣味挑战**分类。位域本身没有分家——
	 * 存档 / 设置 / 选人界面仍共用同一个 {@code challenges} 字段（{@link Dungeon#challenges}、
	 * {@link SPDSettings#challenges()}），只是「分类」控制三处口径：</p>
	 * <ul>
	 *   <li><b>不计入常规挑战数</b>：{@link #activeChallenges(int)} 走 {@link #isRegular(int)}，
	 *       于是不影响随机挑战池、局内计数与 {@code CHAMPION_*} 徽章；</li>
	 *   <li><b>仍计入得分倍率</b>：{@link #scoredChallenges(int)} 保留旧口径（除调试模式外全算），
	 *       各条按各自设定生效——调试模式 / 水仙追迹在 {@code Rankings} 里把倍率直接置 0；</li>
	 *   <li><b>独立入口</b>：{@code WndFunChallenges} 只列这四条。</li>
	 * </ul>
	 */
	public static final int FUN_MASK = DEMOLITION_SQUAD | JELLY_PERSON | NARCISSUS_TRACING | DEBUG_MODE;

	public static final int MAX_VALUE           = 8191;
	//常规挑战的最大可选数（用于选人界面的随机挑战数与滑块上限）——**不含趣味挑战**（见 FUN_MASK）
	public static final int MAX_CHALS           = 9;

	public static final String[] NAME_IDS = {
			"champion_enemies",
			"stronger_bosses",
			"no_food",
			"no_armor",
			"no_healing",
			"no_herbalism",
			"swarm_intelligence",
			"darkness",
			"no_scrolls",
			"demolition_squad",
			"jelly_person",
			"narcissus_tracing",
			"debug_mode"
	};

	public static final int[] MASKS = {
			CHAMPION_ENEMIES, STRONGER_BOSSES, NO_FOOD, NO_ARMOR, NO_HEALING, NO_HERBALISM, SWARM_INTELLIGENCE, DARKNESS, NO_SCROLLS, DEMOLITION_SQUAD, JELLY_PERSON, NARCISSUS_TRACING, DEBUG_MODE
	};

	/**
	 * 是否为「趣味挑战」（{@link #FUN_MASK} 里的任一条）。
	 *
	 * <p>判据用**位与**而非相等：既接受单个挑战位（{@link #MASKS} 的元素），
	 * 也接受 {@code Dungeon.challenges} 这类**合并位域**（只要含任一趣味位即为真）。</p>
	 */
	public static boolean isFun( int mask ){
		return (mask & FUN_MASK) != 0;
	}

	/**
	 * 是否为「常规挑战」——趣味挑战（含调试模式）是特殊挑战：不计入常规挑战数（徽章/计数），也不进随机挑战池。
	 *
	 * <p>新增挑战时只改这里的判据：选人界面的随机挑战池与 {@link #activeChallenges(int)} 都走本方法。
	 * （原来随机池写死成 {@code for (i < MAX_CHALS) 1<<i}，那要求「常规挑战刚好占满低若干位」；
	 * 加入水仙追迹后该假设不再成立——按位取会把 {@code DEBUG_MODE} 也拉进随机池。）</p>
	 */
	public static boolean isRegular( int mask ){
		return !isFun( mask );
	}

	/** 常规挑战的位掩码（顺序即用户界面顺序）。 */
	public static int[] regularMasks(){
		int n = 0;
		for (int ch : MASKS) if (isRegular( ch )) n++;
		int[] out = new int[n];
		int i = 0;
		for (int ch : MASKS) if (isRegular( ch )) out[i++] = ch;
		return out;
	}

	/** 趣味挑战的位掩码（顺序即用户界面顺序）。 */
	public static int[] funMasks(){
		int n = 0;
		for (int ch : MASKS) if (isFun( ch )) n++;
		int[] out = new int[n];
		int i = 0;
		for (int ch : MASKS) if (isFun( ch )) out[i++] = ch;
		return out;
	}

	/** 全部常规挑战位的并集（窗口写回时用来「只清掉自己负责的那几位」，趣味挑战位原样保留）。 */
	public static int regularMask(){
		int m = 0;
		for (int ch : MASKS) if (isRegular( ch )) m |= ch;
		return m;
	}

	/** 全部趣味挑战位的并集（即 {@link #FUN_MASK}）。 */
	public static int funMask(){
		return FUN_MASK;
	}

	/**
	 * 反查某一位挑战的文本键（{@link #NAME_IDS} 与 {@link #MASKS} 严格同位同长）。
	 * <p>让「按分类取挑战」的窗口不必再按下标配对。传入非法位返回 {@code null}。</p>
	 */
	public static String nameId( int mask ){
		for (int i = 0; i < MASKS.length; i++){
			if (MASKS[i] == mask) return NAME_IDS[i];
		}
		return null;
	}

	public static int activeChallenges(){
		return activeChallenges(Dungeon.challenges);
	}

	/**
	 * 已开启的**常规**挑战数。
	 * <p>⚠️ 「常规」＝非趣味挑战，用于随机挑战池、局内计数与 {@code CHAMPION_*} 徽章；
	 * 得分倍率另有口径，见 {@link #scoredChallenges(int)}。</p>
	 */
	public static int activeChallenges(int mask){
		int chCount = 0;
		for (int ch : Challenges.MASKS){
			if (!isRegular(ch)) continue; //趣味挑战（含调试模式）不计入常规挑战数
			if ((mask & ch) != 0) chCount++;
		}
		return chCount;
	}

	public static int activeFun(){
		return activeFun(Dungeon.challenges);
	}

	/** 已开启的趣味挑战数（选人界面按钮图标 / 局内右上角计数 / 菜单计数用）。 */
	public static int activeFun(int mask){
		int chCount = 0;
		for (int ch : Challenges.MASKS){
			if (!isFun(ch)) continue;
			if ((mask & ch) != 0) chCount++;
		}
		return chCount;
	}

	/**
	 * 计入**得分倍率**的挑战数（{@code Rankings} 用）：
	 * 口径＝**除「调试模式」外的全部挑战（含趣味挑战）**，即拆分趣味挑战之前 {@code activeChallenges} 的旧语义。
	 *
	 * <p>趣味挑战虽不计入常规挑战数，但按各自设定仍计入倍率；调试模式本身即「不发分局」，
	 * 水仙追迹同理（两者在 {@code Rankings} 里把倍率置 0），故把它们排除在计数外不影响结果。</p>
	 */
	public static int scoredChallenges(){
		return scoredChallenges(Dungeon.challenges);
	}

	public static int scoredChallenges(int mask){
		int chCount = 0;
		for (int ch : Challenges.MASKS){
			if (ch == DEBUG_MODE) continue;
			if ((mask & ch) != 0) chCount++;
		}
		return chCount;
	}

	public static boolean isItemBlocked( Item item ){

		if (Dungeon.isChallenged(NO_HERBALISM) && item instanceof Dewdrop){
			return true;
		}

		return false;

	}

}