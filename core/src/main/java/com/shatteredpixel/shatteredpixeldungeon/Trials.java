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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AimHeartMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChesedMend;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GeburaGrace;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HodGlory;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.InstructionTarget;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;

/**
 * 考验系统（占位版）。
 *
 * 与 {@link Challenges} 完全同构但**互相独立**：自己的位段、自己的设置键、自己的窗口、
 * 自己的存档字段。两边可以同时开启，任何一方都不读也不写对方的位掩码。
 *
 * 十条考验对应卡巴拉之树的十个质点，顺序恒为「从上到下」＝树上的位置顺序，
 * 也就是选择界面里的显示顺序。
 *
 * 效果接在散点的 `Dungeon.isTrialled(常量)` 判定处，而不是去复用挑战的位。
 * 当前进度：HOKMA（智慧）、BINAH（理解）、CHESED（慈悲）、GEBURA（严厉）、
 * NETZACH（胜利）、HOD（荣耀）、YESOD（根基）已实现（见下方「已实现的效果」），其余三条仍是占位。
 */
public class Trials {

	//十质点。位号即数组下标对应的 2 的幂，恒为「常规考验占满最低连续位」
	public static final int KETER		= 1;		//王冠
	public static final int HOKMA		= 2;		//智慧
	public static final int BINAH		= 4;		//理解
	public static final int CHESED		= 8;		//慈悲
	public static final int GEBURA		= 16;		//严厉
	public static final int TIPHERETH	= 32;		//美丽
	public static final int NETZACH		= 64;		//胜利
	public static final int HOD		= 128;		//荣耀
	public static final int YESOD		= 256;		//根基
	public static final int MALKUTH		= 512;		//王国

	//2^10 − 1（用于 SPDSettings 的取值校验）
	public static final int MAX_VALUE	= 1023;
	//可选的常规考验数（不含将来可能加入的「特殊考验」，如调试类）
	public static final int MAX_TRIALS	= 10;

	public static final String[] NAME_IDS = {
			"keter",
			"hokma",
			"binah",
			"chesed",
			"gebura",
			"tiphereth",
			"netzach",
			"hod",
			"yesod",
			"malkuth"
	};

	public static final int[] MASKS = {
			KETER, HOKMA, BINAH, CHESED, GEBURA, TIPHERETH, NETZACH, HOD, YESOD, MALKUTH
	};

	//=== 图标：assets/interfaces/tree.png ===
	//整图现为 **3 行 × _10_ 帧、每帧 _16x16_**（160x48），行序见下方 ICON_ROW_*。
	//帧内图标的实际像素尺寸各不相同（其余像素透明，画面起点恒为 (0,0)）；
	//下表是该帧内非透明像素的包围盒宽高 —— 重画任何一帧后都必须用 `_chk/analyze_tree_png.py` 实测回填，
	//因为这两个数**就是**取出该图标时的取样矩形（`WndTrials` 按它从 16x16 帧里裁出图形本体，
	//再让包围盒中心落在质点上；写错就会裁掉或错位）。
	//⚠️ **三行的包围盒逐格完全一致**（`_chk/verify_tree_rows_bbox.py` 实测 0/10 不一致）⇒
	//   换状态行只改取样矩形的 y 偏移，尺寸表三行共用一套。
	//（2026-09-24 三行全部绘齐，逐帧尺寸已与第一行实测包围盒相等；双端核对见 `_chk/verify_tree_trials_icons.py`。）
//（2026-09-25 用户擦掉了 TIPHERETH 帧右侧那道极薄的不透明像素 ⇒ 第 5 项由 16x15
//   订正为 15x15，整树外包宽随之由 104.27 收到 103.27。**改完源图必须重跑**
//   `_chk/verify_tree_trials_icons.py` 与 `_chk/analyze_tree_png.py` 实测回填。）
	public static final int ICON_FRAME	= 16;
	public static final int ICON_COLS	= 10;
	public static final int ICON_ROWS	= 3;
	public static final int[] ICON_W	= {13, 15, 15, 15, 13, 15, 15, 15, 15, 15};
	public static final int[] ICON_H	= {16, 13, 14, 14, 16, 15, 15, 14, 15, 16};

	//三行的用途（用户 2026-09-25 口径）
	public static final int ICON_ROW_ENABLED	= 0;	//彩色 —— 考验**已开启**
	public static final int ICON_ROW_LOCKED		= 1;	//浅灰 —— 考验**尚未解锁**（界面里不允许开启）
	public static final int ICON_ROW_READY		= 2;	//深灰 —— 已解锁但**未开启**（初始态）

	/**
	 * 该考验是否已解锁。**目前恒为 true**（用户口径：考验默认初始即解锁）。
	 * <p>留这个收口是为了将来加「通关 / 成就前置」：未解锁的质点在选择界面上用
	 * {@link #ICON_ROW_LOCKED} 的灰化图标，并且不允许开启。</p>
	 *
	 * @param index 质点在 {@link #NAME_IDS} 里的下标（0 = KETER）
	 */
	public static boolean isUnlocked( int index ){
		return true;
	}

	/**
	 * 该质点在选择界面上该用哪一行图标。判据只有「是否已开启」与「是否已解锁」两条。
	 * <p>刻意做成**纯函数**（只看入参、不读 SPDSettings）：选择窗口里的勾选是「待提交」状态，
	 * 提交前的图标必须跟着待提交值走，而不是跟着已存档值走。</p>
	 *
	 * @param index   质点在 {@link #NAME_IDS} 里的下标（0 = KETER）
	 * @param enabled 待提交的「是否开启」
	 */
	public static int iconRow( int index, boolean enabled ){
		if (enabled) return ICON_ROW_ENABLED;
		return isUnlocked( index ) ? ICON_ROW_READY : ICON_ROW_LOCKED;
	}

	//=== 已实现的效果 =======================================================

	//--- HOKMA（智慧）-------------------------------------------------------
	//把「最终移动/攻击延迟」夹在固定上下限内：
	//  · 英雄最终移动延迟：不低于 1（等价于速度不高于 1）
	//  · 敌方最终移动延迟：不高于 1（等价于速度不低于 1）
	//  · 英雄最终攻击延迟：不低于 0.5
	//「最终」的含义：夹取发生在**一切 buff / 装备 / 天赋 / 挑战修正叠加完之后**，
	//因此它压过所有加速与减速来源，也压过原版挑战（如 AscensionChallenge 的速度修正）。
	//
	//收口点只有两处，缺一不可：
	//  ① {@code Char.speed()} —— 全作唯一的移动速度出口（已 final，子类只能覆写 speedRaw()）。
	//     延迟恒由 `1/speed()` 或 `delay/speed()` 算出，所以在速度上夹一次就覆盖了**全部**移动延迟站点，
	//     包括将来新增的站点。敌方那几处 `1/speed()` / `-1/speed()` 的分子恒为 ±1，故速度下限即延迟上限。
	//  ② {@code Hero.attackDelay()} 与 Hero 移动的 spend 处 —— 攻击延迟不走 speed()，须单独收口。
	//⚠️ 只夹 speed() 是不够的：`Hero.move` 里 `float delay = 1;` 会被天赋「致命迅捷」改成 `0`，
	//   而 **0 除以任何数仍是 0**，封顶速度拦不住「免费移动」⇒ 最终值还必须在 spend 处再夹一次。
	public static final float HERO_MOVE_DELAY_MIN	= 1f;
	public static final float ENEMY_MOVE_DELAY_MAX	= 1f;
	public static final float HERO_ATTACK_DELAY_MIN	= 0.5f;

	/**
	 * 最终移动速度（移动延迟 = 1/本值）。全作唯一出口，语义见上方 HOKMA 说明。
	 * <p>判据与原版 {@code AscensionChallenge.enemySpeedModifier} 一致：只有 {@code alignment == ENEMY}
	 * 才算「怪物」，英雄的盟友（分身、玫瑰幽魂、灵鹰等）与中立 NPC（幽灵、王鼠）不受本考验影响。</p>
	 */
	public static float modifyMoveSpeed( Char ch, float speed ){
		if (ch == null || !Dungeon.isTrialled( HOKMA )) return speed;

		if (ch == Dungeon.hero){
			//英雄：延迟 ≥ 1 ⇔ 速度 ≤ 1
			return Math.min( speed, 1f / HERO_MOVE_DELAY_MIN );
		}

		if (ch.alignment == Char.Alignment.ENEMY){
			//敌方：延迟 ≤ 1 ⇔ 速度 ≥ 1
			return Math.max( speed, 1f / ENEMY_MOVE_DELAY_MAX );
		}

		return speed;
	}

	/**
	 * 英雄最终移动延迟的下限。必须夹在 spend 处而不是只看速度：
	 * 「致命迅捷」（{@code GreaterHaste}）会把 {@code Hero.move} 里的 delay 置 0，而 0/n 恒为 0。
	 */
	public static float modifyHeroMoveDelay( float delay ){
		if (!Dungeon.isTrialled( HOKMA )) return delay;
		return Math.max( delay, HERO_MOVE_DELAY_MIN );
	}

	/**
	 * 英雄最终攻击延迟的下限。含战士天赋「手起刀落」（{@code LethalMomentumTracker}）那记
	 * 原本零消耗的击杀一击 —— 按设计该天赋此击只省一半时间，天赋文案会同步注明。
	 */
	public static float modifyHeroAttackDelay( float delay ){
		if (!Dungeon.isTrialled( HOKMA )) return delay;
		return Math.max( delay, HERO_ATTACK_DELAY_MIN );
	}

	//--- BINAH（理解）-------------------------------------------------------
	//「**生成时**」的等级规则 —— 与 HOKMA 那种「一切结算之后再夹」的全局规则不同，
	//它只在道具**刚刚被生成**的那一瞬间生效，之后的一切强化都不受影响。
	//
	//  · 自然生成的道具不会带正等级；
	//  · 若这次生成掷出了诅咒，则把生成时掷到的等级**整体取负**；
	//  · 0 级道具即使带诅咒也仍是 0（-0 == 0）。
	//
	//判定顺序不可颠倒 —— **先判「因诅咒反转」，再判「正等级归零」**：
	//  ① if (cursed) lvl = -lvl;   ② if (lvl > 0) lvl = 0;
	//两步读的是同一个「生成时掷出的等级」。若把 ② 提到 ① 前面，等级会先被清零，
	//① 再反转 0 仍是 0 ⇒ **负等级永远不会出现**，整条考验退化成「所有道具都是 +0」。
	//
	//收口点在 Item.random()（已 final，子类只能覆写 randomRaw()）：
	//它是全作唯一「自然生成」出口 —— Generator 的六个分支、雕像与装甲兽的装备、
	//宝箱 / 房间 / 商店的道具全都经过它，因此将来新写的生成逻辑也自动被覆盖。
	//⚠️ 它覆盖不到「生成之后被人为改等级」的地方，这正是需求里「生成时行为、不是全局行为」
	//   的含义：铁匠重铸、升级卷轴、附魔、雕像按层数加成、角色自己的装备都不受影响。
	//⚠️ 但它**确实**覆盖雕像（Statue）与装甲兽（ArmoredBrute）的装备 —— 那两处也是
	//   Generator.random() / .random() 生成的。其中 Statue 生成后会自行 weapon.cursed = false，
	//   于是「掷出诅咒 ⇒ 负等级」的武器会以**无诅咒的负等级**形式被掉落（上游行为，已记入 docs）。
	//⚠️ 全程不碰 Random，只读 trueLevel()/cursed、只写 level(int)
	//   ⇒ **随机流不变，关卡种子布局与不开本考验时逐字节一致**。

	/**
	 * 自然生成道具的等级收口（BINAH）。语义见上方说明。
	 * <p>调用点在 {@code Item.random()} 的 final 包装里，那里已经跑完子类全部的掷点逻辑，
	 * 所以 level 与 cursed 都已是最终值。</p>
	 */
	public static Item modifyGeneratedItem( Item item ){
		if (item == null || !Dungeon.isTrialled( BINAH )) return item;

		//读**原始**存储等级：Wand 等覆写了 level()，会把咒能灌注 / 树脂加成算进去，
		//而这里要判的是「生成时掷出的那个等级」。
		int lvl = item.trueLevel();

		//① 先判「因诅咒反转」——有等级的诅咒道具整体取负（0 取负仍是 0）。
		if (item.cursed) lvl = -lvl;

		//② 再判「正等级归零」——走完 ① 之后仍为正，说明它没被诅咒。
		if (lvl > 0) lvl = 0;

		//只在真的需要改时写回：level(int) 会连带 updateQuickslot()、
		//Wand.updateLevel() 重算充能，等级没变就别白跑一遍。
		if (lvl != item.trueLevel()) item.level( lvl );

		return item;
	}

	/**
	 * 任务 NPC 赠礼的等级压平（BINAH）：一律归零。
	 * <p>必须在该 NPC 完成**自身的**等级调整之后再调用 —— 幽灵会 +0~+3、制杖人会 +1、
	 * 铁匠会 +0~+3、小恶魔会 +2，这些都发生在 {@code Generator.random()} 之后，
	 * 所以 {@link #modifyGeneratedItem} 拦不到。</p>
	 * <p>只改等级，不动诅咒（小恶魔的戒指该是诅咒的仍是诅咒）。</p>
	 */
	public static void flattenQuestReward( Item item ){
		if (item == null || !Dungeon.isTrialled( BINAH )) return;
		if (item.trueLevel() != 0) item.level( 0 );
	}

	/**
	 * 房间赠品的「因诅咒取负」（BINAH）：把正等级取负，0 与负等级保持不变。
	 * <p>祭坛（{@code SacrificeRoom}）与墓室石棺（{@code CryptRoom}）的诅咒是<b>房间补的</b> ——
	 * {@code prize.cursed = true} 写在 {@code Generator.random()} <b>之后</b>，所以
	 * {@link #modifyGeneratedItem} 在生成出口读到的 {@code cursed} 仍是 false，规则② 看不到它；
	 * 同时「未诅咒就白送一次 {@code upgrade()}」也发生在生成之后。两者合起来会让约七成的赠品
	 * 变成「+1 诅咒道具」，绕过「生成时不带正等级」。</p>
	 * <p>必须在该房间完成<b>自身的</b>等级调整之后再调用（放在 {@code prize.cursed = true} 之后最稳）。</p>
	 * <p>语义取「只取负、不归零」：负等级来自那三成<b>生成时就掷中诅咒</b>的赠品，规则② 已经算过，
	 * 这里不能再取一次负（否则 −1 → +1）；而 +0 取负仍是 0，正好对应
	 * 「诅咒道具生成时为 0 则等级不变，还是 0」。</p>
	 * <p>与 {@link #flattenQuestReward} 的区别：那个是「一律归 0」（会把负等级也抹平），
	 * 只适用于 NPC 赠礼；这里保留负等级，只处理正等级。</p>
	 */
	public static void curseReverseLevel( Item item ){
		if (item == null || !Dungeon.isTrialled( BINAH )) return;
		int lvl = item.trueLevel();
		if (lvl > 0) item.level( -lvl );
	}

	//--- CHESED（慈悲）------------------------------------------------------
	//「敌方单位」的生存强化。两条效果都只认 alignment == ENEMY（与 HOKMA 同判据，
	//所以英雄的盟友、被腐蚀/招募的怪物、中立 NPC 都不吃）。
	//  · 生命上限 +25%；
	//  · 每 5 回合恢复 10% 生命上限的血量，且**只在血量不满时推进计时**。
	//
	//收口点一：生命上限 —— Mob.onAdd()（原版给 AscensionChallenge 改血用的「首次入场」唯一收口）。
	//它在一个 firstAdded 守卫里先乘挑战倍率、再由 percent 回算 HP；我们把 +25% 夹在两者之间，
	//于是怪物仍以满血入场。存档里存的是**放大过的** HT，而 restoreFromBundle 会把 firstAdded
	//置 false ⇒ 读档不会二次放大。
	//⚠️ 局限（原版收口自带的，与原版挑战待遇一致）：少数单位的 HT 是**入场之后**才定的
	//（Bee.spawn、Boss 换阶段重算、Swarm 分裂等），这些拿不到 +25%。
	//
	//收口点二：回血 —— 一只常驻计时 buff（actors.buffs.ChesedMend），在 Mob.onAdd() 里幂等重挂。
	//挂 buff 而不是写在 Mob.act() 里，是因为一部分单位的 act() 覆写不调 super.act()
	//（CrystalSpire、Masterpiece、MobSpawner、Sheep 等），而 buff 有自己的回合，谁都漏不掉。
	//⚠️ 重挂必须放在 firstAdded 守卫**之外** —— 读档时 firstAdded 已是 false，放里面会漏挂。
	//⚠️ buff 的 type 只能是 POSITIVE/NEUTRAL：Mob.Sleeping 把 NEGATIVE 的 buff 当成「被打醒」，
	//   标成 NEGATIVE 会让全图怪物立刻醒来并警戒。
	//
	//⚠️ **已知耦合：矮人国王二阶段的「王座屏障」刻度**（2026-09-24 修，用户报的卡死）。
	//   原版二阶段的屏障满值 = HT、每击杀一名二阶段仆从削减 HT/12、波次阈值写死 200 / 100
	//   （强化挑战下 HT=450、削减 HT/18、阈值 300 / 150）—— 三者靠一个恒等式咬合，
	//   **唯一前提是 HT 能被 12（强化 18）整除**（300 = 12×25，且 200 = 300×2/3、100 = 300×1/3）。
	//   +25% 把 HT 变成 375（强化 563）后整除不成立：12 次击杀只削 12×31 = 372、永远剩 3 点，
	//   第 4 次击杀后屏障 251 > 200 ⇒ 第二波永不出；而二阶段的国王除 KingDamager 外免疫一切伤害
	//   ⇒ 整场**死局**（玩家无解可打、也不出怪）。
	//   修法在 `DwarfKing` 内（详见该类的「王座屏障的刻度」注释）：屏障满值仍 = HT，
	//   但削减量改为**上取整** ceil(HT/kills)、阈值改为 HT×2/3 与 HT/3。
	//   HT = 300 / 450 时新算式与原字面量**逐个相同**（25 / 200 / 100 / 300 / 150）⇒ 不开本考验分毫不变。
	//   ⇒ **通用红线**：以后凡新增「改敌方 HT」的效果，都要回查一遍**用绝对数字表达 HT 刻度**的 Boss。
	//     2026-09-24 已把全仓「有阶段转换 / 按 HP 阈值分支」的单位逐个过完一遍，结论与处置如下：
	//       · `DwarfKing` 二阶段屏障（上面这处）—— **真死局**（屏障永远削不到 0、第二波永不出），已改 HT 刻度；
	//       · `YogDzewa` —— 阶段推进量写死 300、末段生命下限写死 100、光束伤害刻度写死 400
	//         （HT=1000 的 3/10、1/10、2/5）；HT 变 1250 后四个掉血窗口从 300/300/300/100
	//         漂成 300/300/300/350。**不会卡死**（阈值仍严格递减），但同属该类刻度，已一并改成 HT 刻度；
	//       · `DwarfKing` 一阶段转二阶段的入场血量 50 / 100 —— 绝对量，已改成 `HT/6` 与 `HT×2/9`；
	//       · `SmilingCorpseMountain` —— 换皮阈值 2000 / 1000（HT=3000 的 2/3、1/3），纯外观，已改 HT 刻度。
	//     其余**本来就是 HT 派生或相对量**，无需改动：`Goo` / `CrystalSpire` / `YogFist` / `Brute` /
	//     `ArmoredBrute` / `MeltingLove` / `CrystalGuardian`（`HP*2 <= HT`、`2*HT/3f`、`HT/2` 这类）、
	//     `DM300`（`HT/4*(3-p)`、`HT/3*(2-p)`）、`Tengu`（`HT/8`、`HT/2`）、`GnollGeomancer`（`HT/3`）。
	//     ⚠️ 另有**刻意保留**的两处绝对量，它们都**不是闸门**，改了反而会破坏原版行为：
	//       · `DwarfKing` 三阶段「奄奄一息」的台词扳机 `HP < 20` —— 原版在两个难度下共用同一个常数，
	//         没有任何单一比例能同时还原（20 在 HT=300 与 450 上并非同比例），且它不参与任何阶段推进；
	//       · `GnollGeomancer` 的 `RockArmor.setShield(25)` —— 固定 25 点岩石护甲，与 HT 无关，本就不是 HT 刻度。
	//     上面每一处改动都保证「在**不改 HT 的原版数值下**与原写死值逐个相同」⇒ 不开本考验时分毫不变。

	/** 敌方生命上限倍率（CHESED）。 */
	public static final float CHESED_HP_MULT		= 1.25f;
	/** 回血间隔（CHESED）：每多少回合回一次。 */
	public static final int CHESED_MEND_TURNS		= 5;
	/** 每次恢复的生命上限百分比（CHESED）。回血量由它乘 HT 现算，不写死数值。 */
	public static final float CHESED_MEND_PERCENT	= 0.10f;

	/**
	 * 敌方生命上限的收口（CHESED）。
	 * <p>调用点在 {@code Mob.onAdd()} 的首次入场守卫里，位置必须在挑战倍率**之后**、HP 回算**之前**。</p>
	 */
	public static int modifyMobHT( Mob mob, int ht ){
		if (mob == null || !Dungeon.isTrialled( CHESED )) return ht;
		if (mob.alignment != Char.Alignment.ENEMY) return ht;
		return Math.round( ht * CHESED_HP_MULT );
	}

	/**
	 * 常驻状态的重挂（CHESED 的回血计时器 / HOD 的精英晋升计时器）。幂等：已有该 buff 时
	 * {@code Buff.affect} 返回既有实例。
	 * <p>调用点在 {@code Mob.onAdd()} 的 firstAdded 守卫**之外** —— 首次入场、读档、倒地复活
	 * 都要重挂得到（尸体群倒地时会连带摘掉 buff，复活时正是靠这一行挂回来）。</p>
	 *
	 * <p>⚠️ CHESED 那条：EXP=0 的敌一律不进（2026-09-24）：这类怪物打死了也不给经验，回血只会拉长
	 * 战斗、把「跑尸」的负反馈放大。生命上限 +25% 仍由 {@link #modifyMobHT} 单独处理，
	 * 那是另一条数值线。</p>
	 * <p>⚠️ HOD 那条**不**按 EXP 过滤 —— 需求是「任意敌方单位存活超过 N 回合」（普通 300 / Boss 450，
	 * 见 {@link #hodEliteTurns}），与「给不给经验」
	 * 无关，所以它必须排在 CHESED 的两处提前返回**之前**；是否敌方的判定在 {@code HodGlory.act()}
	 * 里每回合现判。</p>
	 */
	public static void bindMobPassives( Mob mob ){
		if (mob == null) return;

		//HOD 精英晋升计时器：不按 EXP 过滤（见上），故必须排在下面两行提前返回之前。
		if (Dungeon.isTrialled( HOD )){
			Buff.affect( mob, HodGlory.class );
		}

		//CHESED 回血计时器：
		if (!Dungeon.isTrialled( CHESED )) return;
		if (mob.EXP <= 0) return;        //EXP=0 的敌（中立/盟友/不奖励怪）一律不进回血计时器
		Buff.affect( mob, ChesedMend.class );
	}

	//--- GEBURA（严厉）------------------------------------------------------
	//「所有敌方单位即将死亡时进入无敌状态，一定回合内不会倒下；回合数 = 该单位的固有经验值（EXP）；
	//  计时结束才真正死亡。」
	//
	//判定点：Char.damage 的致死闸门 —— 即 `if (HP < 0) HP = 0;` 与 `if (!isAlive()) die(src);`
	//之间那一行（原版 GritTeethBuff.checkBypass 就钉在那里）。为什么非要是这一行：
	//  ① **靠后**：伤害已算完、护盾已结算、HP 已归零，只差宣布死亡 ⇒ 这是「这一击是否致命」的最晚可撤回点；
	//  ② **在一切死亡副作用之前**：此时子类的 die() 还没跑，所以 DM300 不会在还活着的时候就
	//     bossSlain()+unseal()+掉碎片、矮人国王不会提前把王座战利品丢出来、装甲雕像不会原地掉装备
	//     —— 子类 die()（含 loot / 经验 / 统计 / 剧情善后）全程只跑一次，且跑在真正该死的那一刻；
	//  ③ 原版两种「战续」各有各的接法（用户口径：**豺狼暴徒接、矮人尸群不接**）：
	//     · Ghoul 在 die() 里、调 super.die() **之前**就 return 转入倒地待复活 ⇒ 它有自己的判据，
	//       我们通过 Mob.deathIsDeferred() 显式让路（该判据与它 die() 里的分支同源，见 Ghoul 覆写）；
	//     · Brute / ArmoredBrute 覆写 isAlive()（狂暴护盾没耗尽就始终算「活着」）⇒ 战续**期间**
	//       闸门天然问不到它（狂暴流程与护盾时长分毫不动）；但战续**耗尽**那一刻是 BruteRage
	//       自己调 die(null) 的，绕开了 Char.damage ⇒ 为它另设一个调用点（BruteRage.act() 里紧贴
	//       die(null) 之前，即该路径最靠后的位置）。于是豺狼暴徒＝「狂暴扛完 → 再吃 GEBURA 无敌」。
	//
	//「无敌」直接复用原版机制：覆写 Mob.isInvulnerable() ⇒ Char.damage 既有的无敌分支会吃掉伤害，
	//Char.attack 会以「弹刀音效 + 无敌浮字」结算（文案键 actors.char.invulnerable 已存在），
	//英雄的盟友也会自动不去打它（Mob.chooseEnemy 本来就跳过无敌目标）——一行判定都不用新写。
	//血量按用户要求**停在 0**（空血条仍要「不倒」），所以还必须让 Mob.isAlive() 为真，
	//否则 Char.damage 紧接着的 `if (!isAlive())` 会立刻补一次 die()。
	//
	//「僵直一回合」（2026-09-24 削弱）：豁免发下的那一刻它**不会立刻行动** —— 用户口径是
	//「触发锁血后，怪物等待一回合不行动，随后才正常行动」。实现只是在 Mob 上多挂一个一次性瞬态标记
	//geburaStagger，由 Mob.act() 在**下一次行动机会**上消费（花掉一个 TICK 后直接 return，与
	//paralysed / FeintConfusion 同型）⇒ 它整整空过一个回合，无敌的计时照常走。
	//⚠️ 为什么不是「在锁血那一刻扣掉它的出手机会」：本闸门是在**别人的回合**里被调到的
	//   （英雄的攻击、或 BruteRage 的战续结算），那一刻的「这一回合」并不属于它；若改成去动时间轴
	//   （如 Actor.delayChar），连 GeburaGrace 的计时也会被一起推后 ⇒ 无敌期间照样出手，等于没削。
	//   记成「下一次行动机会空过」既符合用户口径，也不受调度顺序影响。
	//⚠️ 原版在 Char.deathMarked 上明确注释过：isAlive() 会在**绘制期**被调用，不要在里面调 buff(...)
	//   （性能 + 线程约束）⇒ 无敌状态缓存成 Mob 上的布尔字段，由 buff 的 attachTo/detach 维护。
	//⚠️ 全程不碰 Random ⇒ 随机流与不开本考验时逐字节一致。
	//⚠️ **作用范围＝「被打死」＋被点名的战续结束，不含「被机制直接终结」**（刻意的划界，不是漏做）：
	//  闸门本体只钉在 Char.damage 一处；全库 grep `.die(` 逐个过筛后，只有 BruteRage（战续耗尽，
	//  用户点名）额外接了一个调用点。其余一切绕过伤害、由代码直接调 die() 的终结都照原版立即生效：
	//  Preparation.canKO / COMBINED_LETHALITY 的处决必杀、DeathMark / NeverForget、落坑（Chasm）、
	//  献祭召唤物、剧情演出、Boss 阶段清场、GhoulLifeLink 超时等。
	//  判据：**没有**「这一击是否致命」这个语义的终结，都不该硬接（接了就是把机制改成别的机制）。

	/**
	 * GEBURA 的无敌回合数 = 该单位的固有经验值。要换口径只改这一处。
	 * <p>刻意取 {@code EXP} 本身，而不是「实际结算给英雄的经验」—— 后者在英雄等级超过该单位上限时为 0，
	 * 会让本考验在后期直接失效。</p>
	 */
	public static int geburaGraceTurns( Mob mob ){
		return Math.max( 0, mob.EXP );
	}

	/**
	 * 致死闸门上的 GEBURA 判定。两个调用点：
	 * <ul>
	 *   <li>{@code Char.damage} 里、{@code GritTeethBuff.checkBypass} 之后（一切「被打死」的伤害）；</li>
	 *   <li>{@code Brute.BruteRage.act()} 里紧贴 {@code target.die(null)} 之前（战续耗尽的战续结束）。</li>
	 * </ul>
	 * <p>这一击确实会致死、且该单位该吃本考验时，对它做**两件事**：<br>
	 * ① 挂上 {@code GeburaGrace}：它让 {@code Mob.isAlive()} 变真 ⇒ 紧随其后的
	 * {@code if (!isAlive()) die(src);} 不执行；同时让 {@code Mob.isInvulnerable()} 变真 ⇒ 之后的伤害
	 * 被原版无敌分支吃掉。计时结束时由 buff 自己调 {@code die(致死来源)} 真正倒下
	 * （用原始来源，击杀归属/经验/掉落与原版一致）。<br>
	 * ② 置上 {@code Mob.geburaStagger}：锁血那一击的**僵直** —— 这只怪下一次行动机会要空过一回合
	 * （2026-09-24 削弱，消费点在 {@code Mob.act()}）。</p>
	 *
	 * @return 是否**接管**了这次死亡（＝真的发下了豁免）。返回 {@code false} 时调用方必须照原版把死亡落地：
	 *         {@code Char.damage} 那侧靠紧随其后的 {@code if (!isAlive())} 自行落地、不看返回值；
	 *         {@code BruteRage} 那侧没有这句兜底，必须看返回值。用返回值而不是「回头看 isAlive()」还有个好处：
	 *         本方法每一处提前返回都精确对应「原版那句 die() 该照常执行」，不会与 {@code Char.deathMarked}
	 *         之类的「血量归零但仍算活着」的机制纠缠。
	 */
	public static boolean interceptLethalDamage( Char ch, Object src ){
		if (!Dungeon.isTrialled( GEBURA ) || !(ch instanceof Mob)) return false;
		if (ch.isAlive()) return false;								// 只在「这一击致死」时介入
		if (ch.alignment != Char.Alignment.ENEMY) return false;		// 只作用于敌方单位

		Mob mob = (Mob) ch;
		if (mob.geburaUsed) return false;							// 豁免是一次性的：用过就不再救
		if (mob.deathIsDeferred( src )) return false;				// 自带战续/复活 ⇒ 原版接管
		if (Dungeon.level != null && Dungeon.level.pit[ch.pos]) return false;	// 站在深渊上不值得豁免

		int turns = geburaGraceTurns( mob );
		if (turns <= 0) return false;								// 无经验值 ⇒ 无豁免

		mob.geburaUsed = true;

		//「带着标记倒下」的击杀判定（2026-09-26）：真正倒下被推迟了 turns 个回合，而
		//「荣耀凯旋」/「击杀指令目标」现在都改判「死亡时身上带着对应 buff」（见 Mob.die 的收口点）。
		//限时标记（AimHeartMark 只有 10 回合）可能在锁血期间就过期 ⇒ 在这里把**致死那一击那一刻**
		//的携带状态抄一份到 Mob 上，真正倒下时 live 优先、取不到才回落（同 geburaLuckyProc 的写法）。
		//⚠️ 只是两次**纯读取**，不碰 Random —— 与本考验「不改随机流」的硬验收一致。
		mob.geburaAimMarked = mob.buff( AimHeartMark.class ) != null;
		mob.geburaInstrTarget = mob.buff( InstructionTarget.class ) != null;

		//「僵直一回合」（2026-09-24 削弱）：锁血发下的同时，也把这一击记成它的一次「愣神」——
		//刚被打成濒死无敌的**这一回合它不行动**，愣过一回合之后才照常行动。
		//实现上只是多挂一个一次性瞬态标记，真正的消费点在 Mob.act()（下一次行动机会上花掉一个 TICK）。
		//⚠️ 标记与豁免是**两件事**：豁免能跨存档（GeburaGrace 存 turns_left / source_class），
		//   僵直是一次性瞬态、刻意不进存档（见 Mob.geburaStagger 的注释）。
		mob.geburaStagger = true;

		//掉落判定的凭据也要跟着「致死那一击」走（2026-09-24 修复）：真正倒下被推迟了 turns 个回合，
		//而 Mob.die / Mob.rollToDropLoot 里那两处「额外掉落」判定读的却是**真正倒下那一刻**的状态 ——
		//不在这里抄一份，它们必然落空：
		//  · LuckProc（幸运附魔）是**只活到下一个 buff tick 的瞬态标记**（act() 第一句就是 detach），
		//    vanilla 靠「攻击 → 伤害 → die 同一调用栈」才读得到；推迟若干回合后它早已不在。
		//  · 财富戒指的等级取的是 hero 侧的 buff，推迟期间同样可能已经变了（复活 / 卸下等）。
		//消费点：Mob.die() 的 luckyProc 参数、Mob.rollToDropLoot() 的财富块与幸运块
		//（一律 live 优先、取不到才回落这两份留存 ⇒ 未开启本考验时行为逐字不变）。
		//⚠️ 只是两次**纯读取**，不碰 Random —— 与本考验「不改随机流」的硬验收一致。
		mob.geburaLuckyProc = mob.buff( Lucky.LuckProc.class );
		mob.geburaWealthBonus = (Dungeon.hero == null) ? 0
				: Ring.getBuffedBonus( Dungeon.hero, RingOfWealth.Wealth.class );

		Buff.affect( mob, GeburaGrace.class ).set( turns, src );

		//起手给一次「无敌」提示：之后每一击原版都会自己弹，但第一次玩家需要知道「为什么没死」。
		if (mob.sprite != null){
			mob.sprite.showStatus( CharSprite.POSITIVE, Messages.get( Char.class, "invulnerable" ) );
		}

		return true;
	}

	//--- NETZACH / HOD 共用的「按区递增」口径 ---------------------------------
	//命中 / 闪避加成**随区域递增**：一区 +10%，之后每区再 +10%，五区 +50%。
	//「区域」＝地牢的五章：深度 1-5 一区、6-10 二区 …… 21-25 五区；升天（scalingDepth()==26）
	//按五区封顶。倍数由 regionBonusMultiplier() 现算，将来要调只改 REGION_BONUS_STEP 一处。
	public static final float REGION_BONUS_STEP	= 0.10f;
	public static final int MAX_REGION			= 5;

	/**
	 * 当前区域（1~5）。唯一口径，HOD 的命中与 NETZACH 的闪避都读它。
	 * <p>升天时 {@code Dungeon.scalingDepth()} 返回 26，被 {@code MAX_REGION} 封顶到五区。</p>
	 */
	public static int currentRegion(){
		return Math.min( MAX_REGION, Math.max( 1, (Dungeon.scalingDepth() - 1) / 5 + 1 ) );
	}

	/** 本区域的加成倍率：一区 1.10 …… 五区 1.50。 */
	public static float regionBonusMultiplier(){
		return 1f + REGION_BONUS_STEP * currentRegion();
	}

	//--- NETZACH（胜利）-----------------------------------------------------
	//  · 所有敌方单位的**闪避属性**按区递增（一区 +10% …… 五区 +50%）；
	//  · 距离英雄超过 1 格的单位**透明度降低**，超过 3 格**完全不可见**（纯视觉）。
	//
	//收口点一（数值）：命中 / 闪避取数的唯一收口 —— {@link #finalEvasion}。
	//  ⚠️ 全作有**三处**在算「命中 vs 闪避」，且各自复刻了一整套乘区（各自的 copy-pasta 注释也承认了）：
	//    ① {@code Char.hit}（真正的命中判定）
	//    ② {@code Talent.dodgeAsDamageReduction}（把本可闪避的概率折算成减伤）
	//    ③ {@code Stone.proc}（磐岩附魔，同样把闪避折算成减伤）
	//    三处都必须改调同一个取数收口，否则「换算出的概率」会与真实命中率漂移。
	//  ⚠️ 只对 alignment == ENEMY 生效（与 HOKMA / CHESED 同判据）。
	//
	//收口点二（视觉）：{@code CharSprite.draw()} —— 每帧在**绘制前**把 alpha 临时乘一个淡化系数、
	//  绘制完立刻还原。为什么在 draw() 而不是 update()/resetColor()：
	//    · 真正的隐形走 AlphaTweener（它会持续写 alpha），受击闪光走 hardlight（只碰颜色不碰 alpha）
	//      ⇒ 只有「只影响这一帧、不落任何持久状态」才不会与这两者打架（见 {@link #enemyFade}）。
	//  阈值只有这一处：≤1 格完全不透明 / 2~3 格半透明 / >3 格全透明。
	//  ⚠️ **只淡化精灵本体是不够的**：怪身上还挂着两类「浮层」，它们不继承精灵的 alpha，
	//    各自都得拿同一个 {@link #enemyFade} 去乘，否则「看不见的怪」会被浮层点名：
	//      ① 血条 —— {@code CharHealthIndicator}（常驻）与 {@code TargetHealthIndicator}（瞄准中），
	//         两条都走 {@code HealthBar 的 setAlpha(fade)}。
	//         常驻那条 fade == 0 时**连 visible 一并掐掉**（它的 visible 只服务自己）；
	//         ⚠️ 瞄准那条**只压 alpha、绝不碰 visible** —— {@code ChaoticCenser}（混沌香炉）拿
	//         {@code TargetHealthIndicator.instance.isVisible()} 当「英雄当前是否锁定了目标」在用，
	//         掐掉它 = 让香炉在本考验下静默失效，那就不是「纯视觉」了。
	//      ② 状态标记 —— {@code EmoIcon.draw()}（睡眠 / 警觉 / 搜索 / 迷失），
	//         做法与 {@code CharSprite.draw()} 同构：本帧乘、画完还原。
	//  ⚠️ 仍未收口（刻意留着，见 docs/features.md §20.5「仍未收口的浮层」）：护盾光环 ShieldHalo、
	//     光环 Flare aura、冰封 IceBlock、火把光 TorchHalo、各类状态粒子 Emitter。

	/** NETZACH 淡出：距离 ≤ 此值（格）时**完全不透明**。 */
	public static final int NETZACH_FADE_NEAR		= 1;
	/** NETZACH 淡出：距离 > 此值（格）时**完全不可见**。 */
	public static final int NETZACH_FADE_FAR		= 3;
	/** NETZACH 淡出：中间带（NEAR < d ≤ FAR）的透明度。 */
	public static final float NETZACH_FADE_ALPHA	= 0.5f;

	/** 敌方闪避倍率（NETZACH）：按区递增；非敌方、或本考验未开时为 1。 */
	public static float evasionFactor( Char ch ){
		if (ch == null || ch.alignment != Char.Alignment.ENEMY) return 1f;
		if (!Dungeon.isTrialled( NETZACH )) return 1f;
		return regionBonusMultiplier();
	}

	/**
	 * 敌方单位的**淡化系数**（NETZACH）：1 = 完全不透明，0 = 完全不可见。
	 *
	 * <p><b>纯视觉</b>：只给 {@code CharSprite.draw()} 用，不改任何游戏内状态 —— 不写 invisible、
	 * 不碰命中判定与 AI（「看不见」是给玩家的难度，不是给系统的隐身）。</p>
	 * <p>度量用 {@code Dungeon.level.distance}（切比雪夫距离），与原版其余「格数」口径一致。</p>
	 */
	public static float enemyFade( Char ch ){
		if (ch == null || ch == Dungeon.hero) return 1f;
		if (ch.alignment != Char.Alignment.ENEMY) return 1f;
		if (!Dungeon.isTrialled( NETZACH )) return 1f;
		if (Dungeon.hero == null || Dungeon.level == null) return 1f;

		int d = Dungeon.level.distance( ch.pos, Dungeon.hero.pos );
		if (d <= NETZACH_FADE_NEAR) return 1f;
		if (d <= NETZACH_FADE_FAR) return NETZACH_FADE_ALPHA;
		return 0f;
	}

	//--- HOD（荣耀）---------------------------------------------------------
	//  · 所有敌方单位的**命中属性**按区递增（一区 +10% …… 五区 +50%）；
	//  · 某个敌方单位**存活超过 HOD_ELITE_TURNS 回合**（Boss 走 HOD_ELITE_TURNS_BOSS）即获得一次
	//    **随机精英效果**（不可叠加）。
	//
	//收口点一（数值）：与 NETZACH 共用 {@link #finalAccuracy}（见上一节的「三处复刻乘区」说明）。
	//收口点二（计时）：{@code HodGlory} —— 由 {@link #bindMobPassives} 在 {@code Mob.onAdd()} 里
	//  幂等重挂的常驻 buff，数满即调 {@link #grantHodElite} 并自摘。用 buff 而不是 {@code Mob.act()}
	//  的理由见 {@code HodGlory} 的类注释（有些单位的 act() 覆写不调 super.act()）。
	//收口点三（晋升）：{@link #grantHodElite} —— 「随机精英」复用原版冠军系统的六种效果
	//  （{@code ChampionEnemy.randomChampionClass()}，与原版掷点同一个 6 分支映射）。
	//  ⚠️「不可叠加」＝ 身上已有任一 ChampionEnemy（挑战刷出来的、或本考验早先发过的）就不再发第二个。
	//  ⚠️ 这里**会**消耗一次 Random.Int(6)（运行时事件）。与 BINAH「不动随机流」的口径并不冲突：
	//     那条红线针对**关卡生成那一刻**（保证同种子布局逐字节一致），而本效果发生在生成之后，
	//     与怪物掉落 / 攻击掷点同属运行时随机 —— 不存在「开了本考验、布局就变」的问题。

	/** HOD：**普通**敌方存活**超过**这么多回合后获得随机精英效果。 */
	public static final int HOD_ELITE_TURNS	= 300;

	/** HOD：**Boss** 敌方（{@code Char.Property.BOSS}）存活**超过**这么多回合后获得随机精英效果。 */
	public static final int HOD_ELITE_TURNS_BOSS	= 450;

	/**
	 * HOD 的晋升阈值：Boss（{@code Char.Property.BOSS}）走 {@link #HOD_ELITE_TURNS_BOSS}，
	 * 其余（MINIBOSS / 普通怪 / 召唤物）一律走 {@link #HOD_ELITE_TURNS}。
	 * **唯一取数收口** —— 计时器 {@code HodGlory} 只调它，别在两处各写一遍分支。
	 */
	public static int hodEliteTurns( Mob mob ){
		if (mob != null && mob.properties().contains( Char.Property.BOSS )) return HOD_ELITE_TURNS_BOSS;
		return HOD_ELITE_TURNS;
	}

	/** 敌方命中倍率（HOD）：按区递增；非敌方、或本考验未开时为 1。 */
	public static float accuracyFactor( Char ch ){
		if (ch == null || ch.alignment != Char.Alignment.ENEMY) return 1f;
		if (!Dungeon.isTrialled( HOD )) return 1f;
		return regionBonusMultiplier();
	}

	/**
	 * 命中侧的**唯一取数收口**：先读单位自身的属性，再乘上各考验的倍率（先 HOD 命中）。
	 *
	 * <p>存在的理由见 NETZACH 段的「三处复刻乘区」：把「读属性 + 乘考验倍率」收进这两个方法，
	 * {@code Char.hit} / {@code Talent.dodgeAsDamageReduction} / {@code Stone.proc} 一律改调它们，
	 * 才不会出现「真实命中率被改了、换算出的概率没改」的漂移。</p>
	 */
	public static float finalAccuracy( Char attacker, Char defender ){
		if (attacker == null) return 0f;
		return attacker.attackSkill( defender ) * accuracyFactor( attacker );
	}

	/** 闪避侧的取数收口（参数顺序是「防守方, 攻击方」）。语义见 {@link #finalAccuracy}。 */
	public static float finalEvasion( Char defender, Char attacker ){
		if (defender == null) return 0f;
		return defender.defenseSkill( attacker ) * evasionFactor( defender );
	}

	/**
	 * HOD 的晋升：给该单位一个**随机精英效果**。
	 *
	 * <p>「不可叠加」：身上已经有任一 {@code ChampionEnemy} 就什么都不做（既包括原版挑战
	 * {@code CHAMPION_ENEMIES} 刷出来的，也包括本考验早先发过的）。
	 * ⚠️ 必须用 {@code buffs(ChampionEnemy.class)} 而不是 {@code buff(ChampionEnemy.class)} ——
	 * 后者的实现是 {@code b.getClass() == c} 的**精确类匹配**，对抽象基类永远返回 null。</p>
	 *
	 * @return 是否真的发下了一个精英效果（false ＝ 已有精英、或不是敌方、或本考验未开）
	 */
	public static boolean grantHodElite( Mob mob ){
		if (mob == null || !Dungeon.isTrialled( HOD )) return false;
		if (mob.alignment != Char.Alignment.ENEMY) return false;
		if (!mob.buffs( ChampionEnemy.class ).isEmpty()) return false;	// 不可叠加

		ChampionEnemy champ = Buff.affect( mob, ChampionEnemy.randomChampionClass() );

		//反馈：让玩家看懂「它为什么突然变了」。用精英自己的名字（原版文本键已存在，
		//如 actors.buffs.championenemy$blazing.name = 「烈焰精英」）。
		if (champ != null && mob.sprite != null){
			mob.sprite.showStatus( CharSprite.POSITIVE, Messages.titleCase( champ.name() ) );
		}

		return true;
	}

	//--- YESOD（根基）-------------------------------------------------------
	//  · 「自然鉴定」被禁：武器 / 护甲 / 戒指 / 法杖**用够次数后**的自动鉴定不再发生；
	//    卷轴与药水**分解为炼金能量**时附带的鉴定也被拦下。
	//  · 商店里**额外出售一张鉴定卷轴**（ShopRoom.placeItems）。
	//  · 地面上的**堆**一律显示为**未知贴图 + 未知名字与描述**：散落物、商店货架、金币，
	//    以及**六类容器**（宝箱 / 上锁宝箱 / 水晶宝箱 / 坟墓 / 骷髅堆 / 英雄遗骸）都含在内；
	//    战利品指示器（站在堆上时 HUD 显示的那个图标）同样变问号。背包不受影响。
	//
	//⚠️ 范围边界（2026-09-26 用户逐条确认）—— 只禁「四类装备用够次数后的自动鉴定」+「炼金分解的附加鉴定」，
	//   **不**禁：卷轴/药水使用时与投掷时的鉴定；卷轴拆分为符石时的鉴定；鉴定卷轴 / 占卜卷轴 / 感知符石；
	//   三个「直觉」天赋满级时的自动鉴定；神器装备时的自动鉴定；英雄初始装备的鉴定。
	//
	//收口点一（装备自然鉴定）：{@link #naturalIdDisabled()} —— 4 处「数满即 identify()」的站点
	//  （{@code Weapon.proc} / {@code Armor.proc} / {@code Ring.onHeroGainExp} / {@code Wand.wandUsed}）。
	//  ⚠️ 这里**故意不**写进 {@code Item.identify()}：那个方法是全作鉴定总闸，同时被英雄初始装备
	//    （{@code HeroClass.initHero} 里几十处）、鉴定卷轴、感知符石、任务奖励调用，做成总闸会连
	//    「开局自带的武器是已知的」这种事一起拦掉（且开局时 Dungeon.trials 已经生效）。
	//  ⚠️ 与「遗忘碎片」{@code ShardOfOblivion.passiveIDDisabled()} 同口径：数满后只 {@code setIDReady()}，
	//    自己永不鉴定（碎片是转为「待鉴定」，本考验是转为「永不鉴定」，只差一句提示）。
	//收口点二（炼金分解）：{@code WndEnergizeItem.energize} —— 全作唯一的「分解成炼金能量」入口。
	//  能量照给，只拦附加的那次鉴定（AlchemyScene 里是 showIdentify、场景外是 item.identify()）。
	//收口点三（地面显示）：贴图 {@code ItemSprite.view(Heap)} / 名字 {@code Heap.title()} /
	//  描述 {@code WndInfoItem(Heap)} / 战利品指示器 {@code LootIndicator}，四处都读 {@link #hidesGroundHeap}。
	//  ⚠️ 四处是**同一判据的四个出口**，不是四条独立规则 —— 将来若只想改「某一种容器」，改判据本体即可。
	//  ⚠️ 唯一提前放行的是商店货架的**价签**：{@code Heap.title()} 对 {@code FOR_SALE} 先返回价签，
	//    否则「<物品名>：<价格>金币」会连价格一起消失（2026-09-26 用户要求价格正常显示）。

	/**
	 * YESOD：四类装备（武器 / 护甲 / 戒指 / 法杖）的「用够次数后自动鉴定」是否被禁用。
	 * <p>4 个调用点见类注释的「收口点一」。返回 true 时调用方**不要** {@code identify()}。</p>
	 */
	public static boolean naturalIdDisabled(){
		return Dungeon.isTrialled( YESOD );
	}

	/**
	 * YESOD：该地面堆是否应显示为「未知」（未知贴图 + 未知名字与描述）。
	 *
	 * <p>覆盖 {@code Dungeon.level.heaps} 上的**每一个堆**：散落物 {@code HEAP}、商店货架
	 * {@code FOR_SALE}（2026-09-26 用户确认连货架一起变问号），以及**六类容器** ——
	 * {@code CHEST} / {@code LOCKED_CHEST} / {@code CRYSTAL_CHEST} / {@code TOMB} /
	 * {@code SKELETON} / {@code REMAINS}（2026-09-26 用户追加：容器贴图一并变问号）。
	 * {@code Dungeon.level.heaps} 之外的地方（背包、掉落物选择窗、炼金窗）不受影响。</p>
	 *
	 * <p>唯一提前放行的是商店货架的**价签**：{@link Heap#title()} 对 {@code FOR_SALE}
	 * 先返回「&lt;物品名&gt;：&lt;价格&gt;金币」，拦了就没价格了。</p>
	 *
	 * <p>⚠️ 空堆一律返回 false（宝箱被打开后 {@code Heap.open()} 已把 type 改回 {@code HEAP}）。</p>
	 */
	public static boolean hidesGroundHeap( Heap heap ){
		if (heap == null || heap.isEmpty()) return false;
		return Dungeon.isTrialled( YESOD );
	}

	/** YESOD：地面未知物品的提示文本（检视描述与列表标题共用同一条，避免两处口径漂移）。 */
	public static String unknownGroundText(){
		return Messages.get( Trials.class, "yesod_unknown" );
	}

	/** 本局实际开启的考验条数 */
	public static int activeTrials(){
		return activeTrials( Dungeon.trials );
	}

	public static int activeTrials( int mask ){
		int trCount = 0;
		for (int tr : Trials.MASKS){
			//将来若加入「不计入常规数」的特殊考验（如挑战里的 DEBUG_MODE），在此 continue 跳过
			if ((mask & tr) != 0) trCount++;
		}
		return trCount;
	}

}
