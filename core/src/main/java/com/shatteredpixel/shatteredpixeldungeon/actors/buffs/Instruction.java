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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * 指令任务 buff，与「业」（{@link Karma}）交互。
 * <p>接收指令时先随机难度（1~5），再从该难度随机选取一条具体任务：
 * <ul>
 *   <li>难度 0 特殊指令：仅命运宠儿 +3 时由特殊触发出现（背水一战 / 隐藏秘密）；</li>
 *   <li>难度 1 必定完成：没有实际任务要求，到期自动完成；</li>
 *   <li>难度 2 简单：吃食物 / 搜索三次 / 击杀两名敌人；</li>
 *   <li>难度 3 普通：击杀指令目标 / 离开本层 / 使用两次药水或卷轴；</li>
 *   <li>难度 4 困难：限时内击杀 3 名敌人 / 点燃自己 / 不被任何怪物发现；</li>
 *   <li>难度 5 不可完成：限时到后自动失败。</li>
 * </ul>
 * 完成显示 _CLEAR_；失败（含不可完成）则「业」+{@link #KARMA_PENALTY}（不显示失败信息）。
 * 所有相关提示均为淡蓝色（{@link GLog#c}）。</p>
 */
public class Instruction extends Buff {

	{
		//正在执行的指令任务：十字架复活后保留（避免任务中断/业惩罚状态丢失）
		revivePersists = true;
	}

	//指令系统提示的淡蓝色（信息栏与角色头顶跳字共用）
	public static final int LIGHT_BLUE = 0x8FE3FF;

	public static final int KARMA_PENALTY = 5;   //失败时业 +5

	//指令难度
	public static final int DIFF_SPECIAL    = 0; //特殊（命运宠儿+3）
	public static final int DIFF_GUARANTEED = 1; //必定完成
	public static final int DIFF_EASY       = 2; //简单
	public static final int DIFF_NORMAL     = 3; //普通
	public static final int DIFF_HARD       = 4; //困难
	public static final int DIFF_IMPOSSIBLE = 5; //不可完成

	//任务类型（每个具体任务独立的完成条件）
	public static final int TASK_NONE = -1;
	//难度1（到期自动完成）
	public static final int TASK_DONT_SEE_SKY     = 0;
	public static final int TASK_DONT_GOUGE_LIVER = 1;
	public static final int TASK_STAND_FIRM       = 2;
	//难度2
	public static final int TASK_EAT_FOOD  = 3;
	public static final int TASK_SEARCH_3  = 4;
	public static final int TASK_KILL_2    = 5;
	//难度3
	public static final int TASK_KILL_TARGET = 6;
	public static final int TASK_LEAVE_FLOOR = 7;
	public static final int TASK_USE_2_ITEMS = 8;
	//难度4
	public static final int TASK_KILL_3       = 9;
	public static final int TASK_IGNITE       = 10;
	public static final int TASK_NOT_DETECTED = 11;
	//难度5（无解，到期自动失败）
	public static final int TASK_BOIL_VOICEBOX = 12;
	public static final int TASK_FIND_SEED     = 13;
	public static final int TASK_READ_33550336 = 14;
	//难度0（命运宠儿+3 特殊指令）
	public static final int TASK_SPECIAL_BOSS   = 15;
	public static final int TASK_SPECIAL_HIDDEN = 16;

	//各难度的任务文本与任务类型（同难度随机选取）
	private static final String[][] TEXTS = new String[6][];
	private static final int[][] TASKS = new int[6][];
	static {
		TEXTS[DIFF_SPECIAL] = new String[]{
				"致……击杀你所目睹的强大生物，时间不限",
				"致……这一层具有隐藏的秘密。"};
		TASKS[DIFF_SPECIAL] = new int[]{TASK_SPECIAL_BOSS, TASK_SPECIAL_HIDDEN};

		TEXTS[DIFF_GUARANTEED] = new String[]{
				"致……50回合之内不要目睹天空",
				"致……50回合之内，不要用小刀挖去自己的左侧肝脏",
				"致……50回合之内，直立行走、漂浮或站定"};
		TASKS[DIFF_GUARANTEED] = new int[]{TASK_DONT_SEE_SKY, TASK_DONT_GOUGE_LIVER, TASK_STAND_FIRM};

		TEXTS[DIFF_EASY] = new String[]{
				"致……150回合之内吃下一口干粮",
				"致……150回合之内，搜索周围环境三次",
				"致……150回合之内，击杀两位敌人"};
		TASKS[DIFF_EASY] = new int[]{TASK_EAT_FOOD, TASK_SEARCH_3, TASK_KILL_2};

		TEXTS[DIFF_NORMAL] = new String[]{
				"致……100回合之内击杀一次指令目标",
				"致……100回合之内，离开这一层",
				"致……100回合之内，使用两次药水或卷轴"};
		TASKS[DIFF_NORMAL] = new int[]{TASK_KILL_TARGET, TASK_LEAVE_FLOOR, TASK_USE_2_ITEMS};

		TEXTS[DIFF_HARD] = new String[]{
				"致……50回合之内击杀三名敌人",
				"致……50回合之内，点燃自己",
				"致……50回合之内，不被任何怪物发现"};
		TASKS[DIFF_HARD] = new int[]{TASK_KILL_3, TASK_IGNITE, TASK_NOT_DETECTED};

		TEXTS[DIFF_IMPOSSIBLE] = new String[]{
				"致……50回合之内，将自己的声带煮沸之后食用",
				"致……50回合之内，找到何橄草中的辛香籽",
				"致……50回合之内，阅读33550336次卷轴"};
		TASKS[DIFF_IMPOSSIBLE] = new int[]{TASK_BOIL_VOICEBOX, TASK_FIND_SEED, TASK_READ_33550336};
	}

	private int difficulty = DIFF_HARD;
	private int taskType = TASK_NONE;
	private int turnsLeft = 50;
	private int killsLeft = 3;      //各任务复用为计数/倒计数
	private boolean targetKilled;   //（保留，兼容存档）
	private boolean foodEaten;      //（保留，兼容存档）
	private boolean detected;       //难度4：是否被怪物发现过
	public boolean refreshed;       //从心所欲+2：刷新限次（每次指令任务限一次）

	//命运宠儿+3 特殊指令：当前待接的特殊任务类型（接取时由 startInstruction 消费）
	private static int pendingSpecial = TASK_NONE;

	//待接的特殊指令过期/消费时清除
	public static void clearPendingSpecial(){
		pendingSpecial = TASK_NONE;
	}

	//====== 提示音（2026-09-18）======
	//与信息栏的 *哔哔* / _CLEAR_ 文本同步播放。
	//去抖用 Game.timeTotal（累计秒，注意 Game.elapsed 是单帧增量、不能当时间戳用）：
	//同一回合内若多条路径同时汇报（如背水一战封层与指令计时器同回合），只响一次，避免叠音。
	private static final float SFX_DEBOUNCE = 0.25f;
	private static float lastBuzzAt  = -SFX_DEBOUNCE;
	private static float lastClearAt = -SFX_DEBOUNCE;

	//*哔哔* 提示音（计时器到点 / 命运宠儿+3 特殊指令）
	public static void playBuzz(){
		if (Game.timeTotal > lastBuzzAt && Game.timeTotal - lastBuzzAt < SFX_DEBOUNCE) return;
		lastBuzzAt = Game.timeTotal;
		Sample.INSTANCE.play(Assets.Sounds.INSTRUCTION_BUZZ);
	}

	//_CLEAR_ 提示音（指令完成）
	public static void playClear(){
		if (Game.timeTotal > lastClearAt && Game.timeTotal - lastClearAt < SFX_DEBOUNCE) return;
		lastClearAt = Game.timeTotal;
		Sample.INSTANCE.play(Assets.Sounds.INSTRUCTION_CLEAR);
	}

	//触发特殊指令的*哔哔*（命运宠儿+3；触发时覆盖当前待接，接取后覆盖正在执行的任务）
	public static void triggerSpecial(Hero hero, int specialTask){
		if (hero == null || hero.pointsInTalent(Talent.FORTUNE_FAVORITE) < 3) return;
		pendingSpecial = specialTask;
		GLog.c("*哔哔*");
		if (hero.sprite != null){
			hero.sprite.showStatus(LIGHT_BLUE, "*哔哔*");
		}
		playBuzz();
		Buff.affect(hero, PendingInstruction.class, PendingInstruction.DURATION);
		hero.interrupt();
	}

	//触发任务（由指令终端接取时调用）
	public static void startInstruction(Hero hero){
		//命运宠儿+3 特殊指令：接取时必定执行对应特殊任务
		if (pendingSpecial != TASK_NONE){
			int special = pendingSpecial;
			pendingSpecial = TASK_NONE;
			startSpecial(hero, special);
			return;
		}

		int difficulty = rollDifficulty(hero);
		int idx = Random.Int(TEXTS[difficulty].length);
		int task = TASKS[difficulty][idx];
		String text = TEXTS[difficulty][idx];

		GLog.c(text);
		if (hero.sprite != null){
			hero.sprite.showStatus(LIGHT_BLUE, text);
		}
		//指令成瘾：接受到新的指令时
		Talent.onNewInstruction(hero);

		Instruction instruction = Buff.affect(hero, Instruction.class);
		instruction.difficulty = difficulty;
		instruction.taskType = task;
		instruction.turnsLeft = turnsFor(difficulty);
		instruction.killsLeft = initialCount(task);
		instruction.detected = false;
		instruction.spend(TICK); //下个回合开始计数
	}

	//各任务的初始计数
	private static int initialCount(int task){
		switch (task){
			case TASK_SEARCH_3: case TASK_USE_2_ITEMS:
				return 0; //累加到 3 / 2
			case TASK_KILL_2:
				return 2; //倒计数
			case TASK_KILL_3:
				return 3; //倒计数
			default:
				return 3;
		}
	}

	//命运宠儿+3 特殊指令（难度0）
	private static void startSpecial(Hero hero, int task){
		if (task == TASK_SPECIAL_BOSS){
			//覆盖正在执行的任务（如果有）
			Instruction old = hero.buff(Instruction.class);
			if (old != null) old.detach();

			String text = TEXTS[DIFF_SPECIAL][0];
			GLog.c(text);
			if (hero.sprite != null){
				hero.sprite.showStatus(LIGHT_BLUE, text);
			}
			//触发此指令时立刻获得一层指令加护
			Buff.affect(hero, DivineBlessing.class).gainStack();
			//指令成瘾：接受到新的指令时
			Talent.onNewInstruction(hero);

			Instruction instruction = Buff.affect(hero, Instruction.class);
			instruction.difficulty = DIFF_SPECIAL;
			instruction.taskType = TASK_SPECIAL_BOSS;
			instruction.turnsLeft = 999999; //时间不限
			instruction.spend(TICK);
		} else if (task == TASK_SPECIAL_HIDDEN){
			//仅提示信息，不视为真正的任务
			String text = TEXTS[DIFF_SPECIAL][1];
			GLog.c(text);
			if (hero.sprite != null){
				hero.sprite.showStatus(LIGHT_BLUE, text);
			}
		}
	}

	//随机难度（1~5；难度0 特殊指令仅由特殊触发出现）
	//命运宠儿：+1 排除必然失败(5)、+2 排除困难(4/5)
	private static int rollDifficulty(Hero hero){
		int maxDiff = DIFF_IMPOSSIBLE;
		if (hero.hasTalent(Talent.FORTUNE_FAVORITE)){
			int points = hero.pointsInTalent(Talent.FORTUNE_FAVORITE);
			if (points >= 2){
				maxDiff = DIFF_NORMAL; //排除 4、5
			} else if (points == 1){
				maxDiff = DIFF_HARD; //排除 5
			}
		}
		return 1 + Random.Int(maxDiff); //1~maxDiff
	}

	//各难度的限时回合数
	private static int turnsFor(int difficulty){
		switch (difficulty){
			case DIFF_EASY:
				return 150;
			case DIFF_NORMAL:
				return 100;
			default:
				return 50;
		}
	}

	//========== 完成判定钩子 ==========

	//吃下食物（难度2 吃下一口干粮，由 Talent.onFoodEaten 挂钩调用）
	public static void onFoodEaten(Hero hero){
		Instruction instruction = hero.buff(Instruction.class);
		if (instruction != null && instruction.taskType == TASK_EAT_FOOD){
			complete(instruction, hero);
		}
	}

	//搜索周围环境（难度2 搜索三次，由 Hero.search 挂钩调用）
	public static void onSearched(Hero hero){
		Instruction instruction = hero.buff(Instruction.class);
		if (instruction != null && instruction.taskType == TASK_SEARCH_3
				&& ++instruction.killsLeft >= 3){
			complete(instruction, hero);
		}
	}

	//击杀指令目标（难度3 普通指令，由 Mob.die 挂钩调用）
	public static void onTargetKilled(Hero hero){
		Instruction instruction = hero.buff(Instruction.class);
		if (instruction != null && instruction.taskType == TASK_KILL_TARGET){
			complete(instruction, hero);
		}
	}

	//英雄击杀一名敌人（难度2 击杀两位 / 难度4 击杀三名，由 Mob.die 挂钩调用）
	public static void onEnemyKilled(Hero hero){
		Instruction instruction = hero.buff(Instruction.class);
		if (instruction == null) return;
		if (instruction.taskType == TASK_KILL_2 && --instruction.killsLeft <= 0){
			complete(instruction, hero);
		} else if (instruction.taskType == TASK_KILL_3 && --instruction.killsLeft <= 0){
			complete(instruction, hero);
		}
	}

	//击杀boss（难度0 特殊指令：击杀目睹的强大生物，由 Mob.die 挂钩调用）
	public static void onBossKilled(Hero hero){
		Instruction instruction = hero.buff(Instruction.class);
		if (instruction != null && instruction.taskType == TASK_SPECIAL_BOSS){
			complete(instruction, hero);
		}
	}

	//离开这一层（难度3 普通指令，由 InterlevelScene 挂钩调用）
	public static void onFloorExit(Hero hero){
		Instruction instruction = hero.buff(Instruction.class);
		if (instruction != null && instruction.taskType == TASK_LEAVE_FLOOR){
			complete(instruction, hero);
		}
	}

	//使用药水或卷轴（难度3 使用两次，由 Scroll.readAnimation / Potion.drink 挂钩调用）
	public static void onItemUsed(Hero hero){
		Instruction instruction = hero.buff(Instruction.class);
		if (instruction != null && instruction.taskType == TASK_USE_2_ITEMS
				&& ++instruction.killsLeft >= 2){
			complete(instruction, hero);
		}
	}

	//任务完成
	private static void complete(Instruction instruction, Hero hero){
		GLog.c("_CLEAR_");
		if (hero.sprite != null){
			hero.sprite.showStatus(LIGHT_BLUE, "_CLEAR_");
		}
		playClear();
		instruction.detach();
		//神谕庇佑：完成指令时触发
		Talent.onOracleBlessing(hero);
		//剑刃解放：完成指令后获得 1 层解放
		Talent.onInstructionCompleted(hero);
		//神的宠儿专精：完成指令时指令加护层数+1，并必定获得一次按层数财富等级的特殊掉落
		if (hero.subClass == HeroSubClass.DIVINE_FAVORITE){
			DivineBlessing blessing = Buff.affect(hero, DivineBlessing.class);
			blessing.gainStack();
			//财富等级 = 加护层数减半（1/3/5/7/9层→财富1/2/3/4/5级，削弱）；若装备财富戒指则叠加其财富等级
			int wealthLevel = (blessing.stacks() + 1) / 2
					+ RingOfWealth.getBuffedBonus(hero, RingOfWealth.Wealth.class);
			Item drop = RingOfWealth.genDivineDrop(Math.max(1, wealthLevel));
			if (drop != null){
				//掉落在角色脚下（不直接收进背包，避免金币直接入账等问题）
				Dungeon.level.drop(drop, hero.pos).sprite.drop();
				RingOfWealth.showFlareForBonusDrop(hero.sprite);
			}
		}
		//业报消转：完成指令时业 -5/-10/-15
		if (hero.hasTalent(Talent.KARMA_TRANSFER)){
			Karma karma = hero.buff(Karma.class);
			if (karma != null){
				karma.addKarma(-5 * hero.pointsInTalent(Talent.KARMA_TRANSFER));
			}
		}
		//下一次指令由 InstructionTimer（150~200 回合）驱动
	}

	@Override
	public boolean act() {
		//指令成瘾 +3：只要存在尚未完成的指令，就持续获得激素涌动+易伤
		if (target instanceof Hero){
			Hero hero = (Hero) target;
			if (hero.hasTalent(Talent.INSTRUCTION_ADDICTION)
					&& hero.pointsInTalent(Talent.INSTRUCTION_ADDICTION) == 3){
				Buff.prolong(hero, Adrenaline.class, 2f);
				Buff.prolong(hero, Vulnerable.class, 2f);
			}
		}

		//难度0 特殊（boss）：时间不限，不倒数
		if (taskType == TASK_SPECIAL_BOSS){
			spend(TICK);
			return true;
		}

		//难度4 点燃自己：点燃即完成
		if (taskType == TASK_IGNITE && target.buff(Burning.class) != null){
			complete(this, (Hero) target);
			return true;
		}

		//难度4 不被任何怪物发现：每回合检查是否被敌对怪物看见
		if (taskType == TASK_NOT_DETECTED){
			for (Mob m : Dungeon.level.mobs){
				if (m.isAlive() && m.alignment == Char.Alignment.ENEMY
						&& m.fieldOfView != null && m.fieldOfView[target.pos]){
					detected = true;
					break;
				}
			}
		}

		turnsLeft--;
		if (turnsLeft <= 0){
			if (difficulty == DIFF_GUARANTEED && target instanceof Hero){
				//难度1：到期自动完成
				complete(this, (Hero) target);
			} else if (taskType == TASK_NOT_DETECTED && !detected){
				//难度4 不被发现：限时内从未被怪物发现则完成
				complete(this, (Hero) target);
			} else {
				//限时未完成（含难度5 不可完成、被发现）：业 +5，不显示失败信息
				Karma karma = target.buff(Karma.class);
				if (karma != null) karma.addKarma(KARMA_PENALTY);
				detach();
				//下一次指令由 InstructionTimer（150~200 回合）驱动
			}
		} else {
			spend(TICK);
		}
		return true;
	}

	private static final String DIFFICULTY  = "difficulty";
	private static final String TASK_TYPE   = "taskType";
	private static final String TURNS_LEFT  = "turnsLeft";
	private static final String KILLS_LEFT  = "killsLeft";
	private static final String TARGET_KILLED = "targetKilled";
	private static final String FOOD_EATEN  = "foodEaten";
	private static final String DETECTED    = "detected";
	private static final String REFRESHED   = "refreshed";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(DIFFICULTY, difficulty);
		bundle.put(TASK_TYPE, taskType);
		bundle.put(TURNS_LEFT, turnsLeft);
		bundle.put(KILLS_LEFT, killsLeft);
		bundle.put(TARGET_KILLED, targetKilled);
		bundle.put(FOOD_EATEN, foodEaten);
		bundle.put(DETECTED, detected);
		bundle.put(REFRESHED, refreshed);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		difficulty = bundle.getInt(DIFFICULTY);
		taskType = bundle.getInt(TASK_TYPE);
		if (!bundle.contains(TASK_TYPE)){
			//旧存档兼容：按旧难度映射为对应任务类型（难度2→吃食物、3→击杀目标、4→击杀三名）
			switch (difficulty){
				case DIFF_GUARANTEED: taskType = TASK_DONT_SEE_SKY; break;
				case DIFF_EASY:       taskType = TASK_EAT_FOOD; break;
				case DIFF_NORMAL:     taskType = TASK_KILL_TARGET; break;
				case DIFF_HARD:       taskType = TASK_KILL_3; break;
				default:              taskType = TASK_BOIL_VOICEBOX; break;
			}
		}
		turnsLeft = bundle.getInt(TURNS_LEFT);
		killsLeft = bundle.getInt(KILLS_LEFT);
		targetKilled = bundle.getBoolean(TARGET_KILLED);
		foodEaten = bundle.getBoolean(FOOD_EATEN);
		detected = bundle.getBoolean(DETECTED);
		refreshed = bundle.getBoolean(REFRESHED);
	}
}
