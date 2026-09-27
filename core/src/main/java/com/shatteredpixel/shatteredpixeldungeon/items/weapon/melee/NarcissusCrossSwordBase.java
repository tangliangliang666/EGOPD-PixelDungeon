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
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MindVision;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.NarcissusCrossCooldown;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;

import java.util.ArrayList;

/**
 * 「水仙十字圣剑」系列（2026-09-20 建）的共同基类。
 *
 * <h3>三形态</h3>
 * <p>三个形态各为一个独立类（{@link #FORMS}），并且<b>由英雄等级自动决定、玩家无法手动切换</b>：
 * <ul>
 *   <li>{@link NarcissusCrossSword 常态}：既不是 1 级也不是 30 级的中间等级；</li>
 *   <li>{@link NarcissusSwordMang 芒性}：英雄恰好 <b>1 级</b>（旅途的起始）；</li>
 *   <li>{@link NarcissusSwordHuang 荒性}：英雄达到 <b>30 级</b>（旅途的终点）。</li>
 * </ul>
 * 两个特殊形态都会<b>彻底阻断经验值</b>，所以一旦进入就不会因为升级而自行退出——
 * 「芒性」把英雄永久锁在 1 级，「荒性」本就已到顶。要脱离形态只有一条路：把剑从手上拿下来。</p>
 *
 * <h3>为什么用三个类 + 变形，而不是一个类 + 一个 form 字段</h3>
 * <p>图标（{@code image}）、名称与描述在三个形态之间各不相同，而 {@code Item.image} 是在实例构造时
 * 写进 {@code ItemSprite} 的、之后不会自动重读。沿用本作已有的「一个形态一个类 + 换实例」写法
 * （{@link MorphWeapon}、{@link SealedSwordBase}）可以让快捷栏 / 装备栏 / 动作按钮上的图标与文字
 * 一次全部重建，不必自己去找每一个 ItemSprite 刷新。</p>
 *
 * <h3>数据搬运（改形态最容易踩的坑）</h3>
 * <p>形态切换＝{@code Reflection.newInstance} 造一个<b>全新实例</b>再接替槽位
 * （见 {@link MorphWeapon#morphInto}，本系列复用它的搬运口径）。因此
 * <b>凡是没被搬过去的字段都会静默归零且不报任何错</b>。{@code Weapon.storeInBundle} 里列出的键
 * （{@code enchantment} / {@code enchant_hardened} / {@code curse_infusion_bonus} /
 * {@code mastery_potion_bonus} / {@code augment}）就是「一个武器实例带哪些状态」的权威清单，
 * 日后 {@code Weapon} 上新增持久字段要回到 {@code MorphWeapon.morphInto} 补一行。</p>
 *
 * <h3>引擎接线（改效果时按图索骥）</h3>
 * <ul>
 *   <li><b>经验阻断</b>：{@code Hero.earnExp} 开头调 {@link #interceptExp}；</li>
 *   <li><b>「受到攻击时」</b>（芒性）：{@code Hero.defenseProc} 里调
 *       {@link NarcissusSwordMang#tryCounter}，返回 -1 化解本次伤害；</li>
 *   <li><b>闪避加成</b>（芒性）：{@code Hero.defenseSkill} 里调 {@link #heldEvasionMultiplier}；</li>
 *   <li><b>精准加成</b>（芒性）：{@link NarcissusSwordMang#accuracyFactor} 自行覆写，无需改引擎；</li>
 *   <li><b>「造成攻击时」</b>（荒性）：{@link NarcissusSwordHuang#proc}。</li>
 * </ul>
 */
public abstract class NarcissusCrossSwordBase extends MeleeWeapon {

	/** 三个形态的类；顺序即「常态 → 芒性 → 荒性」。 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static final Class<? extends NarcissusCrossSwordBase>[] FORMS = new Class[]{
			NarcissusCrossSword.class,
			NarcissusSwordMang.class,
			NarcissusSwordHuang.class
	};

	/** 「芒性」：英雄恰好 1 级时进入（{@code Hero.lvl} 最低就是 1，所以判断写 &lt;=）。 */
	public static final int MANG_LEVEL = 1;

	/** 「荒性」：英雄达到满级 30 级（{@code Hero.MAX_LEVEL}）时进入。 */
	public static final int HUANG_LEVEL = 30;

	/** 受击反击 / 攻击扩张 的冷却回合数。 */
	public static final float COOLDOWN = 60f;

	/** 「芒性」反击时给视野内敌人挂的致盲回合数。 */
	public static final float BLIND_DURATION = 12f;

	/** 「荒性」攻击后给自己挂的灵视回合数。 */
	public static final float VISION_DURATION = 12f;

	/** 「芒性」每 1 层最深层数提供的精准 / 闪避加成（0.20 = 20%）。 */
	public static final float BONUS_PER_DEPTH = 0.20f;

	/** 「芒性」把经验折算成「神器充能 / 充能 / 祝福」回合数时的除数（经验值 ÷ 5）。 */
	public static final int EXP_PER_TURN = 5;

	{
		tier = 2; //二阶武器：默认面板就是 2+L ~ 15+3L；芒性/荒性的六阶面板各自覆写
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 0.9f;

		//系列专属内容：不进入遗骨生成
		bones = false;
	}

	//==========================================================================
	// 形态判定与自动变形
	//==========================================================================

	/** 英雄当前等级应当处于的形态（{@code hero} 为 null 时按常态处理）。 */
	public static Class<? extends NarcissusCrossSwordBase> formFor(Hero hero) {
		if (hero == null) return NarcissusCrossSword.class;
		if (hero.lvl <= MANG_LEVEL) return NarcissusSwordMang.class;
		if (hero.lvl >= HUANG_LEVEL) return NarcissusSwordHuang.class;
		return NarcissusCrossSword.class;
	}

	/**
	 * 英雄手上（主手优先）的那把水仙十字圣剑；两只手都没有时返回 null。
	 * <p>读 {@code belongings.weapon/secondWep} <b>字段</b>而不是 {@code weapon()} 方法：
	 * 后者在空手时会返回拳套替身，用它做 {@code instanceof} 判断会误判。</p>
	 */
	public static NarcissusCrossSwordBase held(Hero hero) {
		if (hero == null) return null;
		if (hero.belongings.weapon instanceof NarcissusCrossSwordBase) {
			return (NarcissusCrossSwordBase) hero.belongings.weapon;
		}
		if (hero.belongings.secondWep instanceof NarcissusCrossSwordBase) {
			return (NarcissusCrossSwordBase) hero.belongings.secondWep;
		}
		return null;
	}

	/**
	 * 把手上这把剑同步成「英雄等级对应的形态」；已经正确（或剑不在手上）时什么都不做。
	 *
	 * <p>搬运沿用 {@link MorphWeapon#morphInto}——它已经处理了等级 / 附魔 / 诅咒 / 鉴定 /
	 * 一次性加成 / 强化方向的保留，以及快捷栏引用改指、槽位原地替换、按钮重建与头顶虚影。
	 * 自己再写一份只会多出一个「漏搬字段」的漏点。</p>
	 *
	 * @return true 表示真的发生了变形。
	 */
	public static boolean syncForm(Hero hero) {
		NarcissusCrossSwordBase sword = held(hero);
		if (sword == null) return false;

		Class<? extends NarcissusCrossSwordBase> want = formFor(hero);
		if (sword.getClass() == want) return false;

		return MorphWeapon.morphInto(sword, hero, want);
	}

	/**
	 * 装上 / 读档时的挂钩：只负责挂上 {@link FormKeeper}，真正的变形留给它下一次 {@code act()}。
	 *
	 * <p><b>为什么不在 activate 里直接变形</b>：{@code activate} 会在装备流程与
	 * {@code Belongings.restoreFromBundle} 中途被调用，此时替换 {@code belongings.weapon} 的实例
	 * 会把「正在恢复的那件东西」从脚下抽走。延迟一拍既安全，又不会丢失同步——
	 * 看护 buff 在英雄的下一个回合相位必然 act 一次。</p>
	 */
	@Override
	public void activate(Char ch) {
		super.activate(ch);
		if (ch instanceof Hero) {
			Buff.affect(ch, FormKeeper.class);
		}
	}

	/**
	 * 形态看护：只要剑还在手上，每回合把「英雄等级 ↔ 形态」对一次，不一致就地变形；
	 * 剑离开手就自我回收（再次装备时由 {@link #activate} 重新挂上）。
	 *
	 * <p>常驻但不显示在 buff 条上（{@code icon()} 沿用 {@code BuffIndicator.NONE}）。
	 * {@code revivePersists}：安卡复活后剑仍在手上，看护不该被 {@code Hero.live()} 摘掉。</p>
	 */
	public static class FormKeeper extends Buff {

		{
			revivePersists = true;
		}

		@Override
		public boolean act() {
			if (!(target instanceof Hero) || held((Hero) target) == null) {
				detach();
				return true;
			}
			syncForm((Hero) target);
			spend(TICK);
			return true;
		}
	}

	//==========================================================================
	// 「芒性」的精准 / 闪避加成
	//==========================================================================

	/** 英雄本次冒险到达过的最深层数（取历史最深与当前层的较大者）。 */
	public static int deepestReached() {
		int deepest = Math.max(Statistics.deepestFloor, Dungeon.depth);
		return Math.max(0, deepest);
	}

	/** 「每 1 层最深层数 +20%」对应的倍率，即 {@code 1 + 0.2 × 层数}。 */
	public static float depthMultiplier() {
		return 1f + BONUS_PER_DEPTH * deepestReached();
	}

	/**
	 * 本形态提供的<b>闪避倍率</b>（默认 1 = 无加成，只有「芒性」覆写它）。
	 *
	 * <p><b>唯一取点</b>：{@code Hero.defenseSkill} 里紧跟着「闪避戒指」那句调一次
	 * {@link #heldEvasionMultiplier}。武器没有原生的「闪避系数」钩子
	 * （{@code KindOfWeapon.defenseFactor} 给的是<b>减伤区间</b>而不是闪避），所以这条加成只能挂在引擎侧。</p>
	 */
	public float evasionMultiplier() {
		return 1f;
	}

	/** 英雄手上那把剑给出的闪避倍率；手上没有本系列武器时返回 1。 */
	public static float heldEvasionMultiplier(Hero hero) {
		NarcissusCrossSwordBase sword = held(hero);
		return sword != null ? sword.evasionMultiplier() : 1f;
	}

	//==========================================================================
	// 常驻光效
	//==========================================================================

	/**
	 * 本形态的光效颜色；返回 null 表示不发光（只有「常态」）。
	 * <p>芒性 / 荒性各自返回自己的颜色，{@link #glowing()} 会<b>优先用它</b>——
	 * 这就是需求里那句「覆盖附魔的光效」。</p>
	 */
	public ItemSprite.Glowing formGlow() {
		return null;
	}

	/** 光效：有形态光就显示形态光，否则交回父类（附魔辉光 / 松脂涂层 / 神圣武器那套）。 */
	@Override
	public ItemSprite.Glowing glowing() {
		ItemSprite.Glowing glow = formGlow();
		return glow != null ? glow : super.glowing();
	}

	//==========================================================================
	// 经验阻断
	//==========================================================================

	/** 装备期间是否彻底阻断经验值获取（「芒性」「荒性」为 true，「常态」为 false）。 */
	public boolean blocksExp() {
		return false;
	}

	/** 当前手上的剑是否正在吞经验（给 {@code Mob.die} 判断要不要弹「+经验」浮字用）。 */
	public static boolean blocksExpNow(Hero hero) {
		NarcissusCrossSwordBase sword = held(hero);
		return sword != null && sword.blocksExp();
	}

	/** 经验被吞掉时该形态给出的替代收益，由各形态各自实现。 */
	public void onExpGained(Hero hero, int exp) {
		//默认什么都不给（「常态」根本不会走到这里）
	}

	/**
	 * {@code Hero.earnExp} 的<b>唯一</b>入口：装备期间把这一笔经验整笔吞掉，改为该形态的收益。
	 *
	 * <p>在 {@code earnExp} 最开头 return，等于连坐掐掉同方法内的：5 件神器的充能
	 * （幻影锁链 / 丰饶之角 / 炼金工具箱 / 盗贼臂章 / 狂怒）、背包物品的 {@code onHeroGainExp}
	 * 回调、以及 3 个天赋计数器。这是「无法再获得经验值」的应有之义——
	 * 作为替代，芒性自己发放「神器充能 / 充能 / 祝福」，荒性发放祝福。</p>
	 *
	 * @return true 表示经验已被吞掉，调用方应立即从 {@code earnExp} 返回。
	 */
	public static boolean interceptExp(Hero hero, int exp) {
		NarcissusCrossSwordBase sword = held(hero);
		if (sword == null || !sword.blocksExp()) return false;

		sword.onExpGained(hero, exp);
		return true;
	}

	//==========================================================================
	// 视野内全体打击 / 冷却
	//==========================================================================

	/**
	 * 视野内的所有存活敌人。
	 * <p>判据与 {@code MasterpieceShow} / {@code VengeanceArts} 一致：{@code heroFOV} 已经含遮挡与隐形过滤，
	 * 这里只再加「敌对阵营」。遍历前先 {@code toArray} 取快照——边打边死会改动 Actor 列表，
	 * 直接迭代 {@code Actor.chars()} 会踩并发修改。</p>
	 */
	public static ArrayList<Char> enemiesInView(Hero hero) {
		ArrayList<Char> result = new ArrayList<>();
		if (hero == null || Dungeon.level == null) return result;

		for (Char ch : Actor.chars().toArray(new Char[0])) {
			if (ch == null || ch == hero || !ch.isAlive()) continue;
			if (ch.alignment != Char.Alignment.ENEMY) continue;
			if (!Dungeon.level.heroFOV[ch.pos]) continue;
			result.add(ch);
		}
		return result;
	}

	/**
	 * 对视野内每个敌人各造成<b>一次</b>武器掷值的伤害。
	 *
	 * <p>伤害来源写的是<b>英雄本人</b>（不是武器也不是某个 buff）：这样击杀归属、经验、以及
	 * 「英雄造成的伤害」类天赋都照常生效——需求里「获得经验时改为……」正是靠这条链路触发的。
	 * 每一击独立掷值（{@link #damageRoll}），所以语义是「对每个敌人造成一次伤害」，
	 * 而不是「全体吃同一个数字」。</p>
	 *
	 * @return 实际打到的目标数。
	 */
	public int strikeAllInView(Hero hero) {
		int hit = 0;
		for (Char ch : enemiesInView(hero)) {
			if (!ch.isAlive()) continue;
			ch.damage(damageRoll(hero), hero);
			hit++;
		}
		return hit;
	}

	/** 是否不在冷却中（没有 {@link NarcissusCrossCooldown} 就是就绪）。 */
	public static boolean offCooldown(Hero hero) {
		return hero != null && hero.buff(NarcissusCrossCooldown.class) == null;
	}

	/** 开始一轮冷却。 */
	public static void startCooldown(Hero hero) {
		if (hero != null) Buff.affect(hero, NarcissusCrossCooldown.class, COOLDOWN);
	}

	/** 「芒性」的反击：给视野内所有敌人补一次 12 回合致盲（免疫者跳过）。 */
	protected void blindAllInView(Hero hero) {
		for (Char ch : enemiesInView(hero)) {
			if (!ch.isAlive() || ch.isImmune(Blindness.class)) continue;
			Buff.affect(ch, Blindness.class, BLIND_DURATION);
		}
	}

	/** 「荒性」攻击后给英雄自己挂 12 回合灵视。 */
	protected void grantVision(Hero hero) {
		Buff.affect(hero, MindVision.class, VISION_DURATION);
	}

	/** 「芒性」把一笔经验折算成「神器充能 / 充能 / 祝福」的回合数（不足 5 点也给 1 回合）。 */
	public static int expToTurns(int exp) {
		return Math.max(1, exp / EXP_PER_TURN);
	}
}
