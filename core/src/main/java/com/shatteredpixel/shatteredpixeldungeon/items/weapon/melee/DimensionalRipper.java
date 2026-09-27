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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RiftWindow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.RiftParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

/**
 * 次元撕裂者：三阶近战武器。数值与武技配置整套照抄原版三阶武器「弯刀」（{@link Scimitar}）——
 * tier 3、攻击延迟 0.8（1.25 倍速）、面板 16+4L、力量需求 13。
 *
 * <h3>「转移」：命中时的双向空间错位</h3>
 * <p>自带原版<b>转移诅咒</b>（{@code items/weapon/curses/Displacing}）与护甲<b>定相诅咒</b>
 * （{@code items/armor/curses/Displacement}）的同源效果，但这里是<b>两者合一、双向随机</b>：
 * 每次命中掷一次判定，命中后 <b>50% 把目标拽走、50% 把自己拽走</b>。</p>
 *
 * <ul>
 *   <li><b>概率随等级成长</b>（2026-09-22 用户定）：{@code +0} 时为 {@code 1/8}（原版转移诅咒
 *       固定 1/12、护甲定相固定 1/20，本武器起点取更高的一档），此后每 +1 级把「与 {@code 1/2}
 *       的差距」乘 {@code 7/8} ⇒ 等级越高越靠近、但<b>永不到达</b> 1/2
 *       （公式与取值表见 {@link #procChance(int)}）。这是本武器与那两条原版诅咒最大的不同：
 *       <b>强化它会同时提高转移频率</b>，也就是提高「空间撕裂」的开窗频率。仍乘
 *       {@link Weapon.Enchantment#genericProcChanceMultiplier}——奥术之环、狂暴等作用在
 *       <b>附魔概率</b>上的加成同样作用在它身上。</li>
 *   <li><b>不是真诅咒</b>：本类不碰 {@code enchantment} 字段，{@code isCursed()} 为 false，
 *       照常可以卸下、可以附魔。转移是硬编码被动，<b>不会</b>被解除诅咒卷轴洗掉。</li>
 *   <li><b>不可传送者不硬传</b>：照原版先判 {@code IMMOVABLE}（雕像、炮台等）与免疫传送，
 *       直接跳过。掷到哪一方就<b>只判哪一方，不回退给另一方</b>——两边都没传成就不开窗。</li>
 * </ul>
 *
 * <h3>空间撕裂</h3>
 * <p>本武器造成的任意一次传送（不论被传的是敌人还是英雄自己）都会给英雄开一扇
 * {@link RiftWindow}：3 回合内右下角副槽出现「空间撕裂」按钮，点一下即
 * <b>传送至触发那次传送时被攻击的那个敌人身旁并发动一次攻击</b>，用掉即关窗。</p>
 *
 * <h3>粒子特效</h3>
 * <p>配色（六种紫，白 → 深紫）与三种粒子行为都在 {@link RiftParticle} 里，取色直接采自本武器贴图。
 * 一共三处挂点：</p>
 * <ul>
 *   <li><b>命中</b>（{@link #proc}）：只要这一下真打出了伤害，就在被打中的目标身上炸开一蓬
 *       {@link RiftParticle#HIT}，与是否触发转移无关——「撕」这个动作本身该有反馈。</li>
 *   <li><b>转移</b>（{@link #warp}）与 <b>空间撕裂</b>（{@link RiftWindow#doAction}）：
 *       两端共用 {@link #teleportFx}（起点炸散 + 终点汇聚）。</li>
 * </ul>
 * <p>顺带补上了一处原版缺口：{@code ScrollOfTeleportation.appear} 只在<b>非英雄</b>传送时
 * 往起点撒 3 颗白色光点，英雄自己传送是「起点没有、终点也没有」；{@link #teleportFx}
 * 两端都管，英雄的传送终于也有了像样的出入场。</p>
 */
public class DimensionalRipper extends Scimitar {

	/** 转移触发概率的<b>下限</b>，即 {@code +0} 时的概率：{@code 1/8}（2026-09-22 用户定）。 */
	public static final float PROC_CHANCE_BASE = 1/8f;

	/** 转移触发概率的<b>渐近上限</b>：{@code 1/2}。等级再高也只是无限接近，永远到不了。 */
	public static final float PROC_CHANCE_MAX = 1/2f;

	/** 每 +1 级，「与上限的差距」乘这个系数（＝每级固定吃掉 1/8 的差距）。 */
	public static final float PROC_CHANCE_DECAY = 7/8f;

	{
		image = ItemSpriteSheet.DIMENSIONAL_RIPPER;

		//数值与武技配置整套照抄弯刀（父类已设同样的值，这里重列一遍是为了让本文件自解释）
		tier = 3;
		DLY = 0.8f; //1.25x speed
	}

	/**
	 * 等级 {@code lvl} 时的转移触发概率：{@code +0} 为 {@code 1/8}，随等级<b>指数式趋近</b>
	 * {@code 1/2}（1/2 是渐近线，永不达到）。
	 *
	 * <p>公式 {@code p = MAX - (MAX - BASE) * DECAY^lvl}，读作「与上限的差距每升一级乘 7/8」。</p>
	 *
	 * <pre>
	 *   +0 → 12.5%    +3 → 24.9%    +6 → 33.2%    +10 → 40.1%    +15 → 44.9%
	 *   （相邻两级之间的增幅 4.69 → 4.10 → 3.59 … 个百分点，单调递减）
	 * </pre>
	 *
	 * <p>用指数而不用线性，是为了让<b>低强化的每一级都值钱</b>（+0→+1 就多出 4.7 个百分点，
	 * 收益最大），而高强化自然饱和——这才叫「趋向 1/2」，而不是「升到某级就封顶在 1/2」。</p>
	 *
	 * <p>等级一律取 {@link #buffedLvl()}（与 {@code BattleAxe}、{@code DaCapo} 等原版武器
	 * 「按等级算数值」的惯例一致）。</p>
	 */
	public static float procChance(int lvl) {
		float gap = (PROC_CHANCE_MAX - PROC_CHANCE_BASE) * (float) Math.pow(PROC_CHANCE_DECAY, lvl);
		//负等级（被诅咒的武器可能低于 +0）会让差距超过 3/8 ⇒ 概率低于 1/8，兜底截到 0
		return Math.max(0f, PROC_CHANCE_MAX - gap);
	}

	/**
	 * 命中结算：先让父类走完原版那套（附魔、护盾、击杀归属……），再判自己的「转移」。
	 *
	 * <p>判定条件照原版诅咒的惯例：攻击者没有 {@code MagicImmune}、目标还活着、这一下
	 * 真的造成了伤害（{@code damage > 0}）。</p>
	 */
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		//命中特效：只要这一下真打出了伤害，就在目标身上炸开一蓬裂隙粒子
		//（与是否触发下面的转移无关，也不受 MagicImmune 影响——那是机制层的判据）
		if (damage > 0){
			hitFx(defender);
		}

		if (attacker.buff(MagicImmune.class) == null
				&& defender.isAlive() && damage > 0
				&& Random.Float() < procChance(buffedLvl()) * Weapon.Enchantment.genericProcChanceMultiplier(attacker)) {

			//50/50：掷到谁就只判谁，不回退
			Char warped;
			if (Random.Int(2) == 0){
				warped = warp(defender) ? defender : null;
			} else {
				warped = warp(attacker) ? attacker : null;
			}

			//按钮长在英雄身上，只有英雄亲手挥这把武器时才开窗
			if (warped != null && attacker == Dungeon.hero){
				Buff.affect(Dungeon.hero, RiftWindow.class).open(defender);
			}
		}

		return damage;
	}

	/**
	 * 把 {@code ch} 随机传送到本层的一处安全格。
	 *
	 * <p>{@code IMMOVABLE} 与免疫传送的单位先判掉（照原版转移诅咒的做法，同时也是为了
	 * 避开 {@code teleportChar} 里那条「无法传送」的警告）；传成功后再把怪物的状态从
	 * 「追踪」退回「游荡」——否则它下一回合会直线跑回来，等于没传。</p>
	 *
	 * @return 是否真的传送成功
	 */
	private static boolean warp(Char ch){
		if (ch == null
				|| Char.hasProp(ch, Char.Property.IMMOVABLE)
				|| ch.isImmune(ScrollOfTeleportation.class)){
			return false;
		}

		int oldPos = ch.pos;
		if (!ScrollOfTeleportation.teleportChar(ch)){
			return false;
		}

		//起点炸散 + 终点汇聚。原版诅咒那 3 颗白色光点由这层裂隙粒子接手
		//（顺带补上英雄自己的传送：appear() 只给非英雄撒光点，英雄两头都是空的）
		teleportFx(oldPos, ch.pos);

		if (ch instanceof Mob){
			Mob mob = (Mob) ch;
			//传走之后别让它下一回合直线跑回来
			//（本作的 HUNTING/WANDERING 是每个怪物各自一份的 AiState 实例字段，只能经实例比）
			if (mob.state == mob.HUNTING){
				mob.state = mob.WANDERING;
			}
		}
		return true;
	}

	//==========================================================================
	// 粒子特效（六种配色与三种行为见 RiftParticle，取色直接采自本武器贴图）
	//==========================================================================

	/**
	 * 每次命中在目标身上炸开的粒子数。
	 * 取 {@code == RiftParticle.COLORS.length}：粒子是按发射序号循环取色的，
	 * 喷满 6 颗就正好把「白 → 深紫」六色各走一遍（少一颗就永远看不到最深那两档）。
	 */
	public static final int HIT_PARTICLES = 6;

	/** 传送时起点／终点各出的粒子数（同样取 ≥ 6，保证六色全出场）。 */
	public static final int WARP_PARTICLES = 8;

	/**
	 * 命中特效：在被打中的目标身上炸开一蓬裂隙粒子。
	 * 目标没有贴图（理论上不会），或 {@code GameScene} 不在 ⇒ 拿不到发射器，安静跳过。
	 */
	private static void hitFx(Char defender){
		if (defender == null || defender.sprite == null){
			return;
		}
		Emitter emitter = defender.sprite.centerEmitter();
		if (emitter != null){
			emitter.burst(RiftParticle.HIT, HIT_PARTICLES);
		}
	}

	/**
	 * 传送演出：起点向外炸散（人被打散）、终点向心汇聚（人重新拼起来）。
	 *
	 * <p>本武器的「转移」（{@link #warp}）与窗口的「空间撕裂」
	 * （{@link RiftWindow#doAction}）共用这一处，两边看起来才是同一件事。</p>
	 *
	 * <p>两端各自只在英雄视野内才出——精灵是给玩家看的，看不到的地方不必浪费粒子。
	 * 注意终点用的是 {@code CellEmitter.center}（汇聚到格心的一个点）而不是
	 * {@code get}（撒满整格）：{@code IMPLODE} 的速度是按「到中心的距离 ÷ 存活期」算的，
	 * 得保证这个「中心」就是粒子要奔的那个点。</p>
	 */
	public static void teleportFx(int fromPos, int toPos){
		if (Dungeon.level == null || Dungeon.level.heroFOV == null){
			//传送必然发生在关卡里，这里只是兜底：特效不该有机会把主逻辑带崩
			return;
		}
		if (Dungeon.level.heroFOV[fromPos]){
			CellEmitter.get(fromPos).burst(RiftParticle.SCATTER, WARP_PARTICLES);
		}
		if (Dungeon.level.heroFOV[toPos]){
			CellEmitter.center(toPos).burst(RiftParticle.IMPLODE, WARP_PARTICLES);
		}
	}

	/**
	 * 面板文本：把<b>当前等级的真实概率</b>填进 {@code stats_desc}。
	 * 不覆写就永远显示文本里写死的那个数——强化了也看不出概率在涨。
	 *
	 * <p>和 {@code BlackSwan} 一样不区分鉴定状态：概率是这把武器的卖点，直接摆给玩家看。</p>
	 */
	@Override
	public String statsInfo() {
		return Messages.get(this, "stats_desc", Math.round(procChance(buffedLvl()) * 100));
	}

	/** 英雄手上（主手或副手）的次元撕裂者；没有则返回 null。 */
	public static DimensionalRipper equipped(Hero hero){
		if (hero == null || hero.belongings == null){
			return null;
		}
		if (hero.belongings.weapon() instanceof DimensionalRipper){
			return (DimensionalRipper) hero.belongings.weapon();
		}
		if (hero.belongings.secondWep() instanceof DimensionalRipper){
			return (DimensionalRipper) hero.belongings.secondWep();
		}
		return null;
	}

	@Override
	public boolean doUnequip(Hero hero, boolean collect, boolean single) {
		if (super.doUnequip(hero, collect, single)){
			//武器彻底离手 ⇒ 窗口立即作废（另一只手上还有一把就不动）
			if (equipped(hero) == null){
				Buff.detach(hero, RiftWindow.class);
			}
			return true;
		}
		return false;
	}

}
