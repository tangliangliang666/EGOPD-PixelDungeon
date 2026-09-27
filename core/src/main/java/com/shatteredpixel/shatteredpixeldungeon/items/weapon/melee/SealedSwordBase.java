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
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ExecutionUnleashed;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Melting;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.VengeanceArts;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.middlefinger.MiddleFingerVoice;
import com.shatteredpixel.shatteredpixeldungeon.effects.Enchanting;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.RevengeLedger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * 「封印之剑」系列的共同基类（2026-09-13 建，2026-09-14 改为锁手 + 主副切换）。
 *
 * <p>四段形态各为一个独立类（{@link #STAGES}，从「封印」到全解）：
 * {@link SealedSword} → {@link UnsealedSword} → {@link UnsealedSword2} → {@link Laevateinn}。</p>
 *
 * <h3>规则</h3>
 * <ul>
 *   <li><b>六阶武器、力量需求固定 22</b>：{@code tier = 6}；{@link #STRReq(int)} 单独覆写为 22 基础，
 *       并且<b>不随等级下降</b>（数值上等于七阶公式的 +0 档，只有「力量药剂」/「得心应手」才会减）。</li>
 *   <li><b>不能被升级卷轴强化，而是随英雄等级成长</b>（2026-09-16 起，照女猎人的灵能弓）：
 *       {@link #isUpgradable()} 返回 false，升级卷轴不会把它列为目标；面板等级改由
 *       {@link #level()} 按「英雄等级 ÷ 5」给出，所以它跟着英雄一起变强、不需要消耗任何卷轴。
 *       <b>但「不吃升级卷轴」不等于「不能附魔」</b>：注魔秘卷 / 附魔符石 / 强化符石改的是
 *       {@code enchantment} 与 {@code augment}，与等级无关，所以仍然可用——见
 *       {@code ScrollOfEnchantment.enchantable()} 里的显式放行（2026-09-18）。</li>
 *   <li><b>锁死在双手</b>：只要在武器栏（主手 {@code belongings.weapon} 或副手 {@code belongings.secondWep}）里，
 *       {@link #doUnequip(Hero, boolean, boolean)} 一律拒绝——不能卸下、不能被别的武器顶替、不能丢弃/投掷，
 *       也不会因解除武装陷阱离手。</li>
 *   <li><b>只能主副切换</b>：照决斗家的主副手体系，右下角会出现一个「切换主副」按钮
 *       （{@link SwordSwap}，复用 {@code MeleeWeapon.Charger} 的 Champion 交换按钮写法），
 *       点了就把本剑与另一只手上的武器对调。</li>
 *   <li><b>副手即封印</b>：剑每落进一次副手，都会立刻还原成 {@link SealedSword}（等级/附魔等数据全部保留）。</li>
 *   <li><b>解封</b>：剑在<b>主手</b>且未全解时，<b>点击快捷栏</b>即可推进到下一形态（也保留在动作菜单里）。
 *       形态切换沿用漆黑噤默的 {@code MorphWeapon.morphInto} 逻辑——<b>等级、附魔、诅咒状态、鉴定状态、
 *       强化符石选定的方向、一次性加成（升级药剂/硬化附魔/诅咒菱晶）全部原样带走</b>
 *       （搬运点只有一处：{@link #copyState}，漏搬的字段会静默归零）。</li>
 * </ul>
 *
 * <p>本系列<b>不入武器池</b>（与磨损对剑、空酒瓶一致），目前作为「中指 长兄」的初始武器发放。</p>
 */
public abstract class SealedSwordBase extends MeleeWeapon {

	/** 「解封」动作：主手时推进到下一形态。 */
	public static final String AC_UNSEAL = "UNSEAL";

	/** 「切换主副」动作：与另一只手上的武器对调（落进副手即还原为封印之剑）。 */
	public static final String AC_SWAP = "SWAP";

	/** 由封印到全解的四段形态，数组顺序即「解封」的推进顺序。 */
	@SuppressWarnings("unchecked")
	public static final Class<? extends SealedSwordBase>[] STAGES = new Class[]{
			SealedSword.class,
			UnsealedSword.class,
			UnsealedSword2.class,
			Laevateinn.class
	};

	{
		tier = 6; //六阶武器（力量需求固定 22 基础，见 STRReq）
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 0.9f;

		//系列专属内容：不进入遗骨生成
		bones = false;
	}

	/** 指定形态类在 {@link #STAGES} 中的序号；不在表中返回 -1。 */
	public static int stageOf(Class<? extends SealedSwordBase> cls) {
		for (int i = 0; i < STAGES.length; i++) {
			if (STAGES[i] == cls) return i;
		}
		return -1;
	}

	/** 本形态在 {@link #STAGES} 中的序号；不在表中（理论不会发生）按 0（封印）处理。 */
	public int stage() {
		return Math.max(0, stageOf(getClass()));
	}

	//==========================================================================
	// 等级：随英雄等级成长、不可用卷轴强化（2026-09-16 起，照女猎人的灵能弓）
	//==========================================================================

	/** 每多少点英雄等级折算 1 级武器等级（与灵能弓的 {@code SpiritBow.level()} 一致）。 */
	public static final int HERO_LEVELS_PER_UPGRADE = 5;

	/**
	 * 本系列的面板等级＝<b>英雄等级 ÷ {@value #HERO_LEVELS_PER_UPGRADE}</b>，与灵能弓同一套成长方式。
	 *
	 * <p>为什么覆写 {@code level()} 而不是在英雄升级时去调 {@code upgrade()}：这是灵能弓的做法——
	 * 等级是「按当前英雄等级现算」的，不落进物品的真实等级字段（{@link #trueLevel()} 恒为 0）。
	 * 于是它既不会随着存档累积、也不会和 {@code copyState} 的等级搬运搅在一起。</p>
	 *
	 * <p>仍然取 {@code Math.max(trueLevel(), ...)}：万一日后有别的途径真的把等级写进了字段，
	 * 也不会被这里的折算值吃掉。</p>
	 */
	@Override
	public int level(){
		int lvl = (Dungeon.hero == null) ? 0 : Dungeon.hero.lvl / HERO_LEVELS_PER_UPGRADE;
		if (curseInfusionBonus) lvl += 1 + lvl/6;
		return Math.max( trueLevel(), lvl );
	}

	/**
	 * 结算面板用的等级＝真实成长等级 + 「拆开包装」的额外等级 + 「莱瓦汀解放」的临时等级。
	 *
	 * <p>{@code KindOfWeapon.min()/max()} 取的就是本方法（{@code min(buffedLvl())}），
	 * 所以这些「额外等级」只体现在掷值区间上，<b>不会被写回物品等级</b>——
	 * 否则每切换一次形态都会把这份加成固化一次，等级雪崩。</p>
	 *
	 * <p>两个来源各自的含义：{@link #unwrapLevelBonus}（背叛家人者 T3「拆开包装」，看形态与副手），
	 * {@link ExecutionUnleashed#bonusFor}（中指长兄 T4「好久没解放到这种程度了」，
	 * 由盔甲技能「即刻处刑[莱瓦汀]」施加的限时等级）。</p>
	 */
	@Override
	public int buffedLvl(){
		return level()
				+ unwrapLevelBonus( this, Dungeon.hero )
				+ ExecutionUnleashed.bonusFor( Dungeon.hero );
	}

	/**
	 * 不能被升级卷轴选中 / 强化：升级卷轴的目标筛选走的正是这个谓词
	 * （{@code ScrollOfUpgrade.usableOnItem} 直接返回 {@code item.isUpgradable()}）。
	 * 本系列的强化途径改成「随英雄等级成长」，见 {@link #level()}。
	 */
	@Override
	public boolean isUpgradable(){
		return false;
	}

	/**
	 * 在武器描述的末尾补一句「升级方式」。
	 *
	 * <p>为什么不逐形态去改各自的 {@code stats_desc}：这句话对四个形态完全相同，
	 * 写四遍迟早会出现「有的改了、有的没改」。四个形态都继承本类，这里挂一次就全覆盖了。</p>
	 */
	@Override
	public String info() {
		return super.info() + "\n\n" + Messages.get(SealedSwordBase.class, "level_note");
	}

	//==========================================================================
	// 背叛家人者（FAMILY_BETRAYER）的形态数值点
	//==========================================================================

	/** 另一只手上的武器（剑在主手时＝副手，剑在副手时＝主手）；都空着返回 null。 */
	public KindOfWeapon otherHandWeapon( Hero hero ){
		if (hero == null) return null;
		if (inMainHand(hero)) return hero.belongings.secondWep;
		if (inOffHand(hero))  return hero.belongings.weapon;
		return null;
	}

	/** 「得心应手」的投入点数（0 = 未点 / 不是背叛家人者）。 */
	public static int effortlessGripPoints( Hero hero ){
		if (hero == null || hero.subClass != HeroSubClass.FAMILY_BETRAYER) return 0;
		return hero.pointsInTalent( Talent.EFFORTLESS_GRIP );
	}

	/**
	 * 「得心应手」给出的力量需求减免：每点 <b>-2</b>（+1/-2、+2/-4、+3/-6）。
	 * 消费点在 {@link #STRReq(int)}，这是它唯一的取点。
	 */
	public static int effortlessGripReduction( Hero hero ){
		return 2 * effortlessGripPoints( hero );
	}

	/** 「拆开包装」的投入点数（0 = 未点 / 不是背叛家人者）。 */
	public static int unwrapPoints( Hero hero ){
		if (hero == null || hero.subClass != HeroSubClass.FAMILY_BETRAYER) return 0;
		return hero.pointsInTalent( Talent.UNWRAP );
	}

	/**
	 * 「拆开包装」给出的<b>额外面板等级</b>（临时值，只走 {@link #buffedLvl()}）。
	 *
	 * <ul>
	 *   <li><b>+1</b>：等于本形态在 {@link #STAGES} 里的序号——封印 +0、一阶段 +1、二阶段 +2、
	 *       莱瓦汀 +3。也就是「每解开一层包装就多一级」，越接近全解越强。</li>
	 *   <li><b>+2</b>：在 +1 基础上，若<b>另一只手上</b>的武器等级比本剑<b>高至少 1 级</b>，再 +1。
	 *       （比较用的是本剑当前的成长等级；「莱瓦汀」形态下面板等级就等于莱瓦汀等级。）</li>
	 * </ul>
	 *
	 * <p>+3 的作用不在这里，而是 {@link #canReachStage} 里「取消解封条件限制」。</p>
	 */
	public static int unwrapLevelBonus( SealedSwordBase sword, Hero hero ){
		int points = unwrapPoints( hero );
		if (points <= 0 || sword == null) return 0;

		int bonus = sword.stage();
		if (points >= 2){
			KindOfWeapon other = sword.otherHandWeapon( hero );
			if (other != null && other != sword && other.level() >= sword.level() + 1){
				bonus += 1;
			}
		}
		return bonus;
	}

	//==========================================================================
	// 攻击时的火焰附加（2026-09-15）
	//==========================================================================

	/**
	 * 本形态攻击时附带的<b>火焰伤害</b>占本次伤害的比例；0 表示不附带。
	 * <ul>
	 *   <li>封印之剑：0（无特殊效果）</li>
	 *   <li>一阶段解封之剑：{@code 0.10f}</li>
	 *   <li>二阶段解封之剑：{@code 0.30f}，并且点燃目标（{@link #ignitesTarget()}）</li>
	 *   <li>莱瓦汀：{@code 0.50f}，并且每回合点燃自身与周围的火场（{@link Laevateinn#burnAura}）</li>
	 * </ul>
	 */
	public float fireDamagePercent() {
		return 0f;
	}

	/** 本形态攻击是否<b>点燃目标</b>（二阶段起为 true）。 */
	public boolean ignitesTarget() {
		return false;
	}

	/**
	 * 近战命中钩子：附带的火焰伤害 + 命中火焰特效（+ 点燃目标）。
	 *
	 * <p>挂在 {@code Weapon.proc} 上（武器附魔、松脂涂层都挂这个 hook），调用点在
	 * {@code Hero.attackProc} 里、主伤害 {@code enemy.damage()} <b>之前</b>——
	 * 与「焦炭松脂」的附加伤害顺序完全一致，所以本系列与松脂涂层会各自结算一次、互不影响。</p>
	 *
	 * <p>火焰伤害的 <b>src 是武器本身</b>（照 {@code ResinCoatingBuff.bonusDamage} 用 {@code this}）：
	 * 不被算作「以英雄为源的伤害」，因此不会二次吃到中指长兄 T2「报复对象」的 +20%。
	 * 也正因为 src 是武器，{@code Char.damage} 里需要专门认一类
	 * {@code SealedSwordBase} 才会把浮动伤害的图标画成火焰（见那边的注释）。</p>
	 *
	 * <p>命中后还会给目标挂「融化」（背叛家人者专精天赋「融化而死」；
	 * {@link Melting#apply} 内部已判天赋点数，未点则静默跳过）。</p>
	 */
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		//只有「英雄握着这把剑打出去」才触发本系列的效果：幻影/影分身拿着英雄的武器攻击时不触发
		//（与 Weapon.proc 里松脂涂层的判定条件保持一致）
		if (!(attacker instanceof Hero)
				|| !isEquipped((Hero) attacker)
				|| !defender.isAlive()) {
			return damage;
		}

		//「融化而死」：本系列的攻击命中后把目标拖进「融化」（未点天赋则什么都不做）。
		//刻意放在「有没有火伤」之前——需求说的是「莱瓦汀系列武器的攻击」，四个形态都算在内；
		//封印之剑自己没有火伤，但它挂上的融化仍会被其它火源（火场、松脂、附魔）放大。
		Melting.apply(defender);

		float pct = fireDamagePercent();
		if (pct <= 0f) {
			return damage;
		}

		int burn = Math.round(damage * pct);
		if (burn > 0) {
			defender.damage(burn, this);
		}

		//命中火焰特效：照「焦炭松脂」的 onBonusDamageFX（目标身上炸开一簇火星）
		if (defender.sprite != null && defender.isAlive()) {
			defender.sprite.emitter().burst(FlameParticle.FACTORY, 4);
		}

		//二阶段起：点燃目标
		if (ignitesTarget() && defender.isAlive() && !defender.isImmune(Burning.class)) {
			Buff.affect(defender, Burning.class).reignite(defender);
		}

		return damage;
	}

	/**
	 * 力量需求固定为 <b>22</b>，<b>不随等级下降</b>（2026-09-16 起）。
	 *
	 * <p>数值上等于「七阶」武器 +0 档的公式（{@code 8 + 7*2 = 22}），故仍借用 {@code STRReq(7, 0)}，
	 * 但<b>传死 0</b>：本系列的等级随英雄等级一起涨（{@link #level()}），若照普通武器那样按
	 * {@code lvl} 递减，力量需求会随着英雄变强一路往下掉——那就等于白送，正是这条需求要避免的。
	 * 传参 {@code lvl} 因此被刻意忽略（保留形参是为了满足 {@code Weapon.STRReq(int)} 的签名）。</p>
	 *
	 * <p>能往下调的只有两处：背叛家人者专精天赋「得心应手」（-2/-4/-6，
	 * 见 {@link #effortlessGripReduction}），以及「肌肉记忆合剂」（{@code PotionOfMastery}）的
	 * 一次性 -2 加成（它就是 {@code masteryPotionBonus} 的唯一来源）。</p>
	 */
	@Override
	public int STRReq(int lvl) {
		int req = STRReq(7, 0);
		req -= effortlessGripReduction(Dungeon.hero);
		if (masteryPotionBonus) {
			req -= 2;
		}
		return Math.max(1, req);
	}

	/**
	 * 锁死在英雄手上：即使因安卡复活而「失去全部物品」（{@code Belongings.lostInventory()}），
	 * 这把剑也不会从武器栏里消失——它已经不属于背包了。
	 */
	@Override
	public boolean keptThroughLostInventory() {
		return true;
	}

	/**
	 * <b>不可出售</b>：本系列是「中指 长兄」的专属武器，不该变成商店里的现钱。
	 *
	 * <p>为什么非补这一条不可 —— {@link Item#sellable()} 的默认规则是
	 * 「不可堆叠的 {@code unique} 才不可售」，而本系列<b>没有</b>标 {@code unique}
	 * （它是普通武器，标了会顺带被禁掉锻造/附魔等一系列东西），于是默认就是可售的。
	 * 偏偏「锁死在双手」并不能挡住这条：商店的收购窗口用的 {@code WndBag} 会把
	 * <b>主手与副手</b>两件装备一并摆进列表（{@code WndBag.layout()} 里的
	 * {@code belongings.weapon} / {@code belongings.secondWep} 两个 {@code placeItem}），
	 * 所以只要剑在副手，它就会出现在收购列表里、被当成普通六阶武器换钱。</p>
	 *
	 * <p>覆写的是<b>谓词</b>而不是去加 {@code unique}：{@code Item} 的注释已经把这条
	 * 约定写死了 —— 「要单独放开/关掉某一项出口，就覆写 {@code sellable()} /
	 * {@code transmutable()}，别动 {@code unique}」。本方法即
	 * {@code Shopkeeper.canSell} 唯一咨询的准入判定（{@code Shopkeeper.canSell}
	 * → {@code item.sellable()}），所以这一处就封锁了整条出售路径。</p>
	 *
	 * <p>四个形态都继承本类，所以「封印之剑 / 一阶段 / 二阶段 / 莱瓦汀」一次全覆盖，
	 * 与 {@link #isUpgradable()} 同一个理由（同一件事写四遍迟早会漏一个）。</p>
	 */
	@Override
	public boolean sellable() {
		return false;
	}

	//==========================================================================
	// 主手 / 副手判定
	//==========================================================================

	/** 取英雄手上（主手优先）的那把封印之剑系列武器；两手都没有时返回 null。 */
	public static SealedSwordBase swordInHands(Hero hero) {
		if (hero == null) return null;
		if (hero.belongings.weapon instanceof SealedSwordBase) return (SealedSwordBase) hero.belongings.weapon;
		if (hero.belongings.secondWep instanceof SealedSwordBase) return (SealedSwordBase) hero.belongings.secondWep;
		return null;
	}

	/** 是否正握在英雄的主手上。 */
	public boolean inMainHand(Hero hero) {
		return hero != null && hero.belongings.weapon == this;
	}

	/** 是否正挂在英雄的副手上。 */
	public boolean inOffHand(Hero hero) {
		return hero != null && hero.belongings.secondWep == this;
	}

	/** 是否握在英雄的任意一只手上。 */
	public boolean inHands(Hero hero) {
		return inMainHand(hero) || inOffHand(hero);
	}

	//==========================================================================
	// 「即刻处刑」专用接口（忠义巡礼者的第 5 式，2026-09-16）
	//==========================================================================

	/**
	 * 把英雄手上的封印之剑系列武器<b>无条件</b>变成 {@code targetClass} 形态，
	 * 并确保它落在<b>主手</b>——「即刻处刑」的「若当前主手未持有莱瓦汀，则立刻切换主武器为莱瓦汀」。
	 *
	 * <p>与 {@link #unsealInto} 的两点区别，都是这条需求明确要的：</p>
	 * <ul>
	 *   <li><b>绕过形态门槛</b>：不查 {@link #canReachStage}（账簿充能 / 残血）——
	 *       这是技能自带的换装，不是玩家点「解封」；</li>
	 *   <li><b>直接对调槽位引用</b>而<b>不</b>走 {@link #swapHands}：后者只要剑落进副手就会把它
	 *       封回 {@link SealedSword}，正好会把刚变出来的莱瓦汀打回原形。</li>
	 * </ul>
	 *
	 * @return 变成目标形态并已位于主手的剑；英雄手上根本没有本系列武器时返回 null（技能退化为普通三段攻击）。
	 */
	public static SealedSwordBase forceIntoMainHand( Hero hero, Class<? extends SealedSwordBase> targetClass ) {
		if (hero == null) return null;

		SealedSwordBase sword = swordInHands(hero);
		if (sword == null) return null;

		//① 形态不对就原地变形（数据照 copyState 原样带走，与解封同一条路径）
		if (sword.getClass() != targetClass) {
			SealedSwordBase replacement = copyOf(sword, targetClass);
			if (replacement == null) return null;

			int slot = Dungeon.quickslot.getSlot(sword);
			if (slot != -1) {
				Dungeon.quickslot.setSlot(slot, replacement);
			} else {
				Dungeon.quickslot.clearItem(sword);
			}

			if (hero.belongings.weapon == sword)    hero.belongings.weapon = replacement;
			if (hero.belongings.secondWep == sword) hero.belongings.secondWep = replacement;

			sword = replacement;
		}

		//② 还在副手就与主手直接对调（两个槽位的引用互换，不经过 doUnequip / swapHands）
		if (hero.belongings.secondWep == sword) {
			KindOfWeapon other = hero.belongings.weapon;
			hero.belongings.weapon = sword;
			hero.belongings.secondWep = other;
			//对调过去的另一把武器补一次 activate（普通武器是空实现，武技职业会挂充能 buff）
			if (other != null) other.activate(hero);
		}

		sword.activate(hero);

		Item.updateQuickslot();
		//形态/手位都变了，两个动作按钮上的武器图标都要重建
		ActionIndicator.refreshAll();
		AttackIndicator.updateState();

		return sword;
	}

	/**
	 * 「即刻处刑」处刑余威期间是否<b>豁免力量惩罚</b>。
	 *
	 * <p>莱瓦汀的力量需求是 22，力量不足时的惩罚是精准 ÷{@code 1.5ⁿ}、攻击延迟 ×{@code 1.2ⁿ}、
	 * 不能偷袭、不能用武技——分散在 {@code Weapon.accuracyFactor()}、{@code Weapon.baseDelay()}
	 * 与 {@code Hero.canSurpriseAttack()} 三处。判据统一收在这里，三处各调一次即可
	 * （{@code VengeanceArts.ExecutionAftermath} 是唯一的开关）。</p>
	 *
	 * @param weapon 正在结算的武器（只对封印之剑系列生效）
	 * @param owner  武器的持有者
	 */
	public static boolean ignoresStrengthPenalty( Weapon weapon, Char owner ) {
		return weapon instanceof SealedSwordBase
				&& VengeanceArts.ignoresStrengthPenalty( owner );
	}

	//==========================================================================
	// 装备钩子：挂上右下角「切换主副」按钮
	//==========================================================================

	@Override
	public void activate(Char ch) {
		super.activate(ch);
		//主手、副手都会走 activate（Belongings.restoreFromBundle 里 secondWep 也会 activate），
		//所以两处都能保证按钮 buff 在（Buff.affect 同一类只会有一份）
		if (ch instanceof Hero) {
			Buff.affect(ch, SwordSwap.class);
		}
	}

	//==========================================================================
	// 动作：解封（主手）/ 切换主副
	//==========================================================================

	/**
	 * 快捷栏点击（以及 QuickSlotButton 的默认行为）：
	 * <ul>
	 *   <li>主手 → 解封（推进到下一形态）；</li>
	 *   <li>副手 → 换回主手（副手形态恒为封印，所以这一步同时也是「取回」）；</li>
	 *   <li>都不在（躺在背包里）→ 交给父类（普通武器＝装备），无默认动作时退化为「装备」。</li>
	 * </ul>
	 */
	@Override
	public String defaultAction() {
		Hero hero = Dungeon.hero;
		if (hero != null && inMainHand(hero)) return AC_UNSEAL;
		if (hero != null && inOffHand(hero)) return AC_SWAP;
		String def = super.defaultAction();
		return def != null ? def : AC_EQUIP;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		//解封只在主手有效（副手恒为封印态，见 swapHands）
		if (inMainHand(hero) && stage() < STAGES.length - 1) {
			actions.add(AC_UNSEAL);
		}
		if (inHands(hero)) {
			actions.add(AC_SWAP);
		}
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_UNSEAL)) {
			return Messages.get(SealedSwordBase.class, "ac_unseal");
		} else if (action.equals(AC_SWAP)) {
			return Messages.get(SealedSwordBase.class, "ac_swap");
		}
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_UNSEAL)) {
			if (!inMainHand(hero)) {
				GLog.w(Messages.get(SealedSwordBase.class, "need_main_hand"));
				return;
			}
			int next = stage() + 1;
			if (next >= STAGES.length) {
				GLog.i(Messages.get(SealedSwordBase.class, "full"));
				return;
			}
			if (unsealInto(this, hero, STAGES[next])) {
				hero.spendAndNext(1f); //解封占 1 回合
			}
		} else if (action.equals(AC_SWAP)) {
			swapHands(hero);
		}
	}

	//==========================================================================
	// 锁定：不能卸下
	//==========================================================================

	/**
	 * 被锁死在双手之间：只要还在主手或副手槽里，任何卸下请求一律拒绝。
	 * <p>这一处就同时挡住了所有「离手」途径——它们最终都会回到这里：</p>
	 * <ul>
	 *   <li>取消装备 / 丢到地上 / 投掷：{@code EquipableItem.doDrop()}、{@code cast()} 都以本方法的返回值为门槛；</li>
	 *   <li>被别的武器顶替：{@code KindOfWeapon.doEquip()}、{@code equipSecondary()} 同样先要求本方法成功；</li>
	 *   <li>背包/装备界面的「取消装备」动作（{@code AC_UNEQUIP}）。</li>
	 * </ul>
	 * <p>主副切换不走这里——{@link #swapHands(Hero)} 直接对调两个槽位的引用，否则会被自己挡住。</p>
	 */
	@Override
	public boolean doUnequip(Hero hero, boolean collect, boolean single) {
		if (inHands(hero)) {
			GLog.w(Messages.get(SealedSwordBase.class, "bound"));
			return false;
		}
		return super.doUnequip(hero, collect, single);
	}

	//==========================================================================
	// 主副切换（决斗家式交换 + 副手还原）
	//==========================================================================

	/**
	 * 把本剑与英雄另一只手上的武器对调；剑一旦落进副手，立即还原成 {@link SealedSword}。
	 *
	 * <p>实现要点：</p>
	 * <ul>
	 *   <li>直接对调 {@code belongings.weapon} / {@code belongings.secondWep} 的引用，
	 *       <b>不</b>走 {@code doUnequip}——本剑被 {@link #doUnequip} 锁住，走正常流程会被自己拒绝；
	 *       这正是决斗家 Champion 的交换按钮（{@code MeleeWeapon.Charger.doAction()}）的做法。</li>
	 *   <li>剑进副手要换形态，所以可能是<b>另一个实例</b>：快捷栏里指向旧实例的引用要一并换掉。</li>
	 *   <li>对调后新落位的实例要补一次 {@code activate}（按钮 buff 是挂在英雄身上的，幂等）。</li>
	 * </ul>
	 *
	 * <p><b>回合</b>：默认消耗 <b>1 回合</b>（2026-09-15 起，此前是完全不消耗回合的）。
	 * 中指长兄持有 T2「趁手玩具」且不在冷却中时，本次切换<b>不消耗回合</b>，
	 * 并立刻开始 20（+1）/10（+2）回合的冷却——判定统一在 {@code Talent.handyToySwapCost} 里。</p>
	 */
	public void swapHands(Hero hero) {
		if (hero == null) return;

		boolean fromMain = inMainHand(hero);
		boolean fromOff = inOffHand(hero);
		if (!fromMain && !fromOff) return;

		//剑每进一次副手都重新封回「封印之剑」；进主手则维持当前形态
		final SealedSwordBase landed;
		if (fromMain && stage() != 0) {
			SealedSwordBase sealed = copyOf(this, SealedSword.class);
			landed = (sealed != null) ? sealed : this;
		} else {
			landed = this;
		}

		KindOfWeapon other = fromMain ? hero.belongings.secondWep : hero.belongings.weapon;

		//快捷栏里的旧引用要跟着换到替身上，否则点击快捷栏会操作到一个已经离场的实例
		if (landed != this) {
			int slot = Dungeon.quickslot.getSlot(this);
			if (slot != -1) {
				Dungeon.quickslot.setSlot(slot, landed);
			} else {
				Dungeon.quickslot.clearItem(this);
			}
		}

		if (fromMain) {
			hero.belongings.weapon = other;
			hero.belongings.secondWep = landed;
		} else {
			hero.belongings.weapon = landed;
			hero.belongings.secondWep = other;
		}

		landed.activate(hero);
		//对调过去的另一把武器也要走一次 activate（普通武器这里是空实现，武技职业会挂充能 buff）
		if (other != null) {
			other.activate(hero);
		}

		//视觉与音效：照决斗家的交换按钮
		if (hero.sprite != null) {
			hero.sprite.operate(hero.pos);
		}
		Sample.INSTANCE.play(Assets.Sounds.UNLOCK);

		Item.updateQuickslot();
		//两只手里的武器都换了，「切换主副」按钮上的两个图标要重建
		ActionIndicator.refreshAll();
		AttackIndicator.updateState();

		GLog.i(Messages.get(SealedSwordBase.class, "swapped", landed.name()));

		//切换主副的回合代价：默认 <b>1 回合</b>；中指长兄 T2「趁手玩具」在冷却外能让它不消耗回合
		//（由 {@code Talent.handyToySwapCost} 统一判定并开始冷却）。
		//放在最末尾：对调、快捷栏/按钮重建、音画都做完了才收走回合，按钮点击（doAction）与
		//菜单动作（execute）两条入口由此各自结算一次，不会重复。
		float swapCost = Talent.handyToySwapCost( hero );
		if (swapCost <= 0f) {
			GLog.i(Messages.get(SealedSwordBase.class, "free_swap"));
		}
		hero.spendAndNext( swapCost );
	}

	//==========================================================================
	// 右下角「切换主副」按钮（决斗家的交换按钮 / 苦痛技艺的同类写法）
	//==========================================================================

	/**
	 * 承载右下角 ActionIndicator 按钮的常驻 buff。
	 *
	 * <p>为什么是 Buff：{@code ActionIndicator} 的动作槽位，游戏里所有候选
	 * （狂暴、角斗士连击、武技充能、苦痛技艺…）都是靠 Buff 实现 {@code ActionIndicator.Action}
	 * 并自己注册/回收的（参考 {@code MeleeWeapon.Charger}、{@code ScorchingWound}）。
	 * 本 buff 不显示在 buff 条上（{@code icon()} 默认 {@code BuffIndicator.NONE}）。</p>
	 *
	 * <p><b>槽位</b>：占用 <b>副槽（槽位 1）</b>——由 {@code ActionIndicator} 扩展出来的第二个槽位，
	 * 与主槽上的职业技能按钮（狂暴 / 连击 / 复仇技艺…）<b>同时显示</b>，两者不再互相顶掉。
	 * 也就是说本按钮常驻，只要封印之剑系列还在手上就一直看得见。</p>
	 */
	public static class SwordSwap extends Buff implements ActionIndicator.Action {

		{
			//安卡复活后按钮仍在
			revivePersists = true;
		}

		/** 上一次画在按钮上的主手 / 副手物品，用来判断「手里的东西变了，图标要重建」。 */
		private Item shownMain;
		private Item shownOff;

		/** 英雄手上是否还握着封印之剑系列武器。 */
		private boolean holdsSword() {
			return target instanceof Hero && swordInHands((Hero) target) != null;
		}

		/**
		 * 按钮的两个图标（主手 + 副手）是<b>在重建时现读</b> {@code belongings.weapon/secondWep} 的，
		 * {@code ActionIndicator} 只在自己被 {@code setSecondAction} / {@code refreshSecond} 时重建，所以
		 * 「手里的东西变了」必须由这边主动报一次：换武器、解封换形态、主副对调、读档后首次刷新，
		 * 都靠每回合这一次比对兜住。
		 */
		private void refreshIfHandsChanged() {
			if (!(target instanceof Hero)) return;
			Hero hero = (Hero) target;
			Item main = hero.belongings.weapon;
			Item off = hero.belongings.secondWep;
			if (main != shownMain || off != shownOff) {
				shownMain = main;
				shownOff = off;
				//自己就在副槽里才重建；槽位被别人拿走时等它自己还回来再说
				if (ActionIndicator.secondAction == this) {
					ActionIndicator.refreshSecond();
				}
			}
		}

		@Override
		public boolean act() {
			//剑已不在手上（理论上不会发生）：自我回收，按钮随之消失
			if (!holdsSword()) {
				detach();
				return true;
			}
			//常时占副槽：只有槽位不是自己时才占位——已经是自己就什么都不做，
			//否则每回合都会触发一次按钮重建（与主槽上的职业技能按钮互不干扰）
			if (ActionIndicator.secondAction != this) {
				ActionIndicator.setSecondAction(this);
			}
			refreshIfHandsChanged();
			//莱瓦汀的常驻火场：主手握莱瓦汀时，每回合点燃自身与周围 5×5 圆形内的地块。
			//借本 buff 当心跳——只要剑在手上它就一定存在、每回合必然 act 一次。
			if (target instanceof Hero) {
				Laevateinn.burnAura((Hero) target);
			}
			spend(TICK);
			return true;
		}

		@Override
		public boolean attachTo(Char target) {
			if (super.attachTo(target)) {
				if (ActionIndicator.secondAction != this) {
					ActionIndicator.setSecondAction(this);
				}
				return true;
			}
			return false;
		}

		@Override
		public void fx(boolean on) {
			//场景重建 / 英雄精灵刷新后把按钮重新挂回右下角（照 MeleeWeapon.Charger 的写法）
			if (on) {
				shownMain = null;
				shownOff = null;
				if (ActionIndicator.secondAction != this) {
					ActionIndicator.setSecondAction(this);
				}
			}
		}

		@Override
		public void detach() {
			super.detach();
			ActionIndicator.clearAction(this);
		}

		@Override
		public String actionName() {
			return Messages.get(SealedSwordBase.class, "ac_swap");
		}

		@Override
		public int actionIcon() {
			//复用决斗家「交换武器」的图标（hero_icons.png 帧 109）
			return HeroIcon.WEAPON_SWAP;
		}

		/**
		 * 按钮主图标＝<b>主手武器</b>；主手空着时退回「交换」图标。
		 * <p>照决斗家的交换按钮（{@code MeleeWeapon.Charger.primaryVisual()}）：图标加宽 4px
		 * 让视觉重心左移，给右下角叠着的副手图标腾位置。</p>
		 */
		@Override
		public Visual primaryVisual() {
			Hero hero = Dungeon.hero;
			Image ico;
			if (hero == null || hero.belongings.weapon == null) {
				ico = new HeroIcon(this);
			} else {
				ico = new ItemSprite(hero.belongings.weapon);
			}
			ico.width += 4; //shift slightly to the left to separate from smaller icon
			return ico;
		}

		/**
		 * 按钮右下角的小图标＝<b>副手武器</b>，缩小到 0.51 倍并压暗 40%。
		 * <p>与决斗家交换按钮的 {@code secondaryVisual()} 完全一致；副手空着时退回「交换」图标。</p>
		 */
		@Override
		public Visual secondaryVisual() {
			Hero hero = Dungeon.hero;
			Image ico;
			if (hero == null || hero.belongings.secondWep == null) {
				ico = new HeroIcon(this);
			} else {
				ico = new ItemSprite(hero.belongings.secondWep);
			}
			ico.scale.set(PixelScene.align(0.51f));
			ico.brightness(0.6f);
			return ico;
		}

		@Override
		public int indicatorColor() {
			//与决斗家交换按钮同色
			return 0x5500BB;
		}

		@Override
		public void doAction() {
			if (!(target instanceof Hero)) return;
			Hero hero = (Hero) target;
			SealedSwordBase sword = swordInHands(hero);
			if (sword != null) {
				sword.swapHands(hero);
			}
		}
	}

	//==========================================================================
	// 形态门槛（2026-09-15）
	//==========================================================================

	/**
	 * 是否满足推进到第 {@code targetStage} 形态的条件：
	 * <b>复仇账簿充能 &gt; 25/50/75%</b> 或 <b>当前生命低于 75/50/25%</b>，二者满足其一即可。
	 *
	 * <table>
	 *   <tr><th>目标形态</th><th>充能门槛</th><th>残血门槛</th></tr>
	 *   <tr><td>一阶段解封之剑</td><td>&gt; 25%</td><td>&lt; 75%</td></tr>
	 *   <tr><td>二阶段解封之剑</td><td>&gt; 50%</td><td>&lt; 50%</td></tr>
	 *   <tr><td>莱瓦汀</td><td>&gt; 75%</td><td>&lt; 25%</td></tr>
	 * </table>
	 *
	 * <p>充能按<b>点数</b>计（100 点 = 基础满格；账簿随英雄等级升级后上限最高 200 点），
	 * 所以账簿越强、这条门槛越松；残血那条给的是「拿命换剑」的第二条路，
	 * 与中指长兄 T2「会很烫的！」（残血免疫燃烧）正好配套。</p>
	 *
	 * <p>账簿未装备 / 被诅咒时读不到充能，此时只剩残血那条路。</p>
	 *
	 * <p><b>例外</b>：背叛家人者专精天赋「拆开包装」+3 会<b>取消形态切换的全部条件限制</b>——
	 * 见 {@link #unwrapPoints}，此时本方法恒为 true。</p>
	 */
	public static boolean canReachStage(Hero hero, int targetStage) {
		if (hero == null || targetStage <= 0) return true;

		//「拆开包装」+3：取消解封条件限制（充能与残血两条门槛一并作废）
		if (unwrapPoints(hero) >= 3) return true;

		int chargeNeed = 25 * targetStage;      //25 / 50 / 75
		int hpNeed = 100 - 25 * targetStage;    //75 / 50 / 25

		RevengeLedger ledger = RevengeLedger.find(hero);
		boolean charged = ledger != null && !ledger.cursed
				&& ledger.chargePoints() > chargeNeed;
		boolean bleeding = hero.HP * 100 < hero.HT * hpNeed;

		return charged || bleeding;
	}

	//==========================================================================
	// 形态切换（与 MorphWeapon.morphInto 同一套「保留数据」逻辑）
	//==========================================================================

	/**
	 * 把 {@code from} 的持久化数据原样复制进 {@code to}
	 * （等级 / 附魔 / 诅咒 / 鉴定 / 数量 / 强化符石的强化方向 / 三项一次性加成）。
	 *
	 * <p>⚠️ <b>这里是「形态切换会丢数据」的唯一漏点</b>：{@link #STAGES 四个形态}是四个独立类、
	 * 每次解封（{@link #unsealInto}）、每次落进副手封回（{@link #swapHands}）、每次「即刻处刑」强制换装
	 * （{@link #forceIntoMainHand}）都靠 {@code Reflection.newInstance} 造一个<b>全新实例</b>再接替槽位，
	 * 所以<b>凡是没在这里搬过去的字段都会静默归零</b>，且不会有任何报错。</p>
	 *
	 * <p>反过来说，{@link Weapon} 上每新增一个「属于物品自身」的持久字段，都要回来补一行。
	 * 参考口径是 {@code Weapon.storeInBundle} / {@code restoreFromBundle} 里列出的那些键
	 * （{@code enchantment} / {@code enchant_hardened} / {@code curse_infusion_bonus} /
	 * {@code mastery_potion_bonus} / {@code augment}）——那份清单就是「一个武器实例都带哪些状态」的权威定义。</p>
	 */
	private static void copyState(Weapon from, Weapon to) {
		//等级必须取 trueLevel()（纯升级等级）而不是 level()：后者会带上「诅咒菱晶」这类加成
		//（Weapon.level() 在 curseInfusionBonus 时 +1+level/6），一旦写进新形态的真实等级，
		//加成就被固化一次，每切换一次再叠加一次 —— 切几轮等级就会雪崩式膨胀。
		to.level(from.trueLevel());
		to.enchantment = from.enchantment;
		to.cursed = from.cursed;
		to.cursedKnown = from.cursedKnown;
		to.levelKnown = from.levelKnown;
		to.quantity(from.quantity());
		to.masteryPotionBonus = from.masteryPotionBonus;
		to.enchantHardened = from.enchantHardened;
		to.curseInfusionBonus = from.curseInfusionBonus;
		//强化符石选的方向（伤害 / 速度）：2026-09-18 补。此前漏搬 ⇒ 每次形态切换（解封、落副手封回、
		//「即刻处刑」强制换装）都会把强化符石的效果悄悄洗掉，看上去像"符石白用了"。
		to.augment = from.augment;
	}

	/** 生成一个与 {@code from} 数据一致的目标形态实例（不动槽位/背包，供副手还原用）。 */
	private static SealedSwordBase copyOf(Weapon from, Class<? extends SealedSwordBase> targetClass) {
		SealedSwordBase replacement = Reflection.newInstance(targetClass);
		if (replacement != null) copyState(from, replacement);
		return replacement;
	}

	/**
	 * 解封推进到下一形态：装备中则原地替换（不额外花回合，回合数由调用方结算），
	 * 不在装备槽里则先卸旧再收新（与 {@code MorphWeapon.morphInto} 一致）。
	 *
	 * <p><b>门槛</b>：目标形态若是解封形态（一阶段 / 二阶段 / 莱瓦汀），必须先过
	 * {@link #canReachStage}——账簿充能 &gt; 25/50/75%，或当前生命低于 75/50/25%。
	 * 门槛不满足时在这里就拒绝并给出提示，所以<b>任何调用方都绕不过去</b>。</p>
	 *
	 * @return true 表示切换成功。
	 */
	public static boolean unsealInto(Weapon current, Hero hero, Class<? extends SealedSwordBase> targetClass) {
		if (targetClass == current.getClass()) return false;

		//形态门槛：账簿充能 > 25/50/75%，或当前生命低于 75/50/25%
		int targetStage = stageOf(targetClass);
		if (!canReachStage(hero, targetStage)) {
			GLog.w(Messages.get(SealedSwordBase.class, "need_charge_or_hp",
					25 * targetStage, 100 - 25 * targetStage));
			return false;
		}

		SealedSwordBase replacement = Reflection.newInstance(targetClass);
		if (replacement == null) return false;
		copyState(current, replacement);

		//快捷栏引用：有槽位就换掉，没有就清掉旧引用
		int slot = Dungeon.quickslot.getSlot(current);
		if (slot != -1) {
			Dungeon.quickslot.setSlot(slot, replacement);
		} else {
			Dungeon.quickslot.clearItem(current);
		}

		boolean equipped = (hero.belongings.weapon == current);
		boolean secondary = (hero.belongings.secondWep == current);

		if (equipped || secondary) {
			//在武器槽里：原地替换（不花额外回合）
			if (equipped) hero.belongings.weapon = replacement;
			if (secondary) hero.belongings.secondWep = replacement;

			replacement.activate(hero);
		} else {
			//躺在背包里：先卸旧（腾出格子）再收新
			current.detachAll(hero.belongings.backpack);
			if (!replacement.collect(hero.belongings.backpack)) {
				Dungeon.level.drop(replacement, hero.pos);
			}
		}

		Item.updateQuickslot();
		//形态换了，「切换主副」按钮上的主手图标要跟着重建
		ActionIndicator.refreshAll();

		//视觉：英雄头顶浮现所解封形态的武器虚影（复现松脂涂层/神圣武器的附魔视觉效果）
		//sprite 未挂到场景（切场景中）时跳过，避免 parent 为 null 时空指针
		if (hero.sprite != null && hero.sprite.parent != null) {
			Enchanting.show(hero, replacement);
		}

		GLog.i(Messages.get(SealedSwordBase.class, "unsealed"), replacement.name());

		//中指长兄的台词：每推进一段形态一句，与头顶浮字成对（上一条没播完则整条跳过）。
		//放在最末尾：形态已经真的换完、按钮也重建过了，语音与画面同一时刻发生。
		//门槛失败 / 形态没变在上面就 return 了，所以这里只可能是「成功解封」。
		MiddleFingerVoice.playUnseal( targetStage );

		return true;
	}
}
