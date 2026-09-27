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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.middlefinger.MiddleFingerVoice;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.RevengeLedger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Laevateinn;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SealedSwordBase;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndVengeanceSkill;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Visual;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.Locale;

/**
 * 「复仇技艺」——中指 长兄 转职「<b>忠义巡礼者</b>」的分支特性（2026-09-16 实现）。
 *
 * <p>本类是<b>常驻 buff</b>（转职时由 {@code TengusMask.choose} 施加），承载右下角
 * {@link ActionIndicator} 的技能按钮，实现模式照抄 {@link ArtTechniques}（艺术之巅）与
 * {@link ScorchingWound}（命运弃子）：注册在 {@code attachTo}（含读档）与
 * {@code restoreFromBundle}，回收在 {@code detach}。</p>
 *
 * <h3>为什么按钮必须「常时显示」</h3>
 * <p>角斗士的连击按钮只在攒够连击时才冒出来——因为连击数在<b>自己</b>身上，一眼可见。
 * 本系统的「弹药」是敌人身上的 {@link GrudgeMark 怨恨标记}，从自己的状态栏完全看不出
 * 能不能出手，所以按钮<b>从转职那一刻起就一直占着右下角</b>，不再隐藏（哪怕场上一个
 * 带标记的敌人都没有，进去后招数也会因为不满足而置灰）。</p>
 *
 * <p><b>与「切换主副」共存</b>：本 buff 常时占用 {@link ActionIndicator} 的<b>主槽</b>，
 * 封印之剑的「切换主副」按钮 {@code SwordSwap} 占用<b>副槽</b>——两个按钮各占一行、
 * 同时显示，谁也不会把谁挤掉（双槽位的机制见 {@link ActionIndicator} 的类注释）。</p>
 *
 * <h3>怨恨标记</h3>
 * <p>转职后，<b>任何攻击命中中指长兄本人或任意友方单位</b>的敌人都会获得 1 层怨恨标记。
 * 两个入口按攻击路径分工、各收一次口：<b>近战与投掷武器</b>走 {@link #onAttackLanded}
 * （挂在 {@code Char.attack} 的命中分支，与 T2「永不遗忘」的 {@code RevengeTarget} 同一处，
 * 并且能按本击伤害追加「如数奉还」的层数）；<b>远程 / 法术弹道</b>走
 * {@link #onRangedAttackHit}（挂在 {@code Char.hit} 的 3 参重载——那一路压根不走
 * {@code Char.attack}，只叠基础 1 层）。标记本身没有数值影响，只是下面五式的弹药。</p>
 * <p>持续时间默认 {@value GrudgeMark#DURATION} 回合，重复挨打只刷新、不叠加
 * （见 {@link GrudgeMark#gain}）。专精天赋「永志不忘」+1 会让它<b>永不消退</b>。</p>
 *
 * <h3>五式复仇技能</h3>
 * <table>
 *   <tr><th>层数</th><th>名称</th><th>要求</th><th>效果</th></tr>
 *   <tr><td>1</td><td>踏碎</td><td>目标相邻</td>
 *       <td>对<b>选中的那个</b>敌人造成 {@code S-5 ~ S+15} 的伤害（S＝当前力量，护甲照常减免）。
 *           <b>不消耗回合</b>；冲击波特效照旧，但不波及周围其它敌人。</td></tr>
 *   <tr><td>2</td><td>踢飞</td><td>8 格内</td>
 *       <td>跳到目标身边，对其造成一次攻击，并把落点周围八格的敌人全部击退 2 格。
 *           <b>已经贴着目标时不必移动</b>，原地直接结算。</td></tr>
 *   <tr><td>3</td><td>穿腹</td><td>目标在攻击范围内</td>
 *       <td>对攻击范围内所有敌人造成一次攻击，并造成等同于该次伤害的流血。</td></tr>
 *   <tr><td>4</td><td>仇怨重踏</td><td>目标在攻击范围内</td>
 *       <td>对攻击范围内所有敌人造成三段攻击，最后一段把它们各击退 3 格。</td></tr>
 *   <tr><td>5</td><td>即刻处刑</td><td>目标相邻</td>
 *       <td>主手若未持莱瓦汀则立刻换为莱瓦汀；对目标造成 50%/100%/150% 三段攻击，
 *           本次攻击无视力量惩罚，且力量已满足需求时三段各额外计入一倍「超出需求的力量」加成。</td></tr>
 * </table>
 *
 * <p>每次使用会从<b>被选中的那个目标</b>身上扣掉等同于该式要求层数的怨恨标记
 * （层数不足则整式不成立、也不消耗），避免同一只敌人被同一招反复刷。</p>
 *
 * <h3>专精天赋「加倍清算！」</h3>
 * <ul>
 *   <li><b>+1</b>：踏碎额外给目标 5 回合<b>易伤</b>；踢飞击退后额外给 3 回合<b>麻痹</b>。</li>
 *   <li><b>+2</b>：穿腹的流血量改为 1.5 倍，对<b>免疫流血</b>的目标改为再补 0.5 倍伤害（合计 1.5 倍）；
 *       仇怨重踏结束后额外给 10 回合<b>眩晕 / 恍惚 / 失明</b>。</li>
 *   <li><b>+3</b>：即刻处刑获得 3 回合「处刑余威」——期间继续无视莱瓦汀的力量惩罚；
 *       同时清空目标身上<b>剩余</b>的全部怨恨标记，每清掉一层余威再多 5 回合。</li>
 * </ul>
 * <p>所有加成都在 {@link #reckoning} 一处取点（{@code pointsInTalent(DOUBLE_RECKONING)}），
 * 所以以后再改「加倍清算！」只需要动这一处。</p>
 *
 * <h3>专精天赋「如数奉还」</h3>
 * <p>让<b>下手越重的敌人多还一笔账</b>：单次攻击打出的伤害跨过阈值时，这一击额外多叠层。
 * 阈值同时取决于「伤害」和天赋点数，逻辑集中在 {@link #extraMarks}。</p>
 * <ul>
 *   <li><b>+1</b>：伤害 &gt; 10 点 → 额外 +1 层。</li>
 *   <li><b>+2</b>：伤害 &gt; 5 点 → 额外 +1 层。</li>
 *   <li><b>+3</b>：伤害 &gt; 5 点 → 额外 +1 层；&gt; 10 点 → 改为额外 +2 层。</li>
 * </ul>
 *
 * <h3>专精天赋「永志不忘」</h3>
 * <ul>
 *   <li><b>+1</b>：怨恨标记的持续时间变为<b>无限</b>（判定点 {@link #marksLastForever()}，
 *       由 {@link GrudgeMark#act()} 在每次到期时就地续期）。</li>
 *   <li><b>+2</b>：在 +1 基础上，<b>击杀</b>带有怨恨标记的单位时额外恢复 5% 的复仇账簿充能
 *       （入口 {@link #onEnemyKilled}，挂在 {@code Mob.die} 的英雄击杀分支）。</li>
 *   <li><b>+3</b>：在 +1、+2 基础上，获得<b>所有</b>带有怨恨标记的单位的<b>灵视感知</b>
 *       （入口在 {@code Level.updateFieldOfView}，把带标记的怪物写进 {@code heroMindFov}）。</li>
 * </ul>
 */
public class VengeanceArts extends Buff implements ActionIndicator.Action {

	{
		type = buffType.POSITIVE;
		//安卡复活后按钮仍在
		revivePersists = true;
	}

	//==========================================================================
	// 数值常量（改平衡只动这里）
	//==========================================================================

	/** 踏碎伤害区间的基数与力量系数：{@code (S-5) ~ (S+15)}。 */
	public static final int STOMP_MIN_BASE = -5, STOMP_MIN_PER_STR = 1;
	public static final int STOMP_MAX_BASE = 15, STOMP_MAX_PER_STR = 1;

	/** 踢飞的跳跃搜索距离上限（格）。 */
	public static final int KICK_RANGE = 8;
	/** 踢飞把落点周围敌人击退的格数。 */
	public static final int KICK_KNOCKBACK = 2;
	/** 仇怨重踏的段数，以及最后一段的击退格数。 */
	public static final int RESTOMP_HITS = 3;
	public static final int RESTOMP_KNOCKBACK = 3;

	/** 即刻处刑三段伤害的倍率（50% / 100% / 150%）。 */
	public static final float[] EXECUTION_MULTI = { 0.5f, 1f, 1.5f };

	/** 「处刑余威」的基础回合数与「每清掉一层怨恨标记」的追加回合数（专精天赋 +3）。 */
	public static final float AFTERMATH_BASE = 3f;
	public static final float AFTERMATH_PER_MARK = 5f;

	/** 「加倍清算！」各层的时长（回合）：踏碎易伤 / 踢飞麻痹 / 仇怨重踏的眩晕·恍惚·失明。 */
	public static final float RECKONING_VULNERABLE = 5f;
	public static final float RECKONING_PARALYSIS  = 3f;
	public static final float RECKONING_SEAL       = 10f;

	/** 「加倍清算！」+2 把穿腹的流血量放大到的倍数（免疫流血时改为补足同等差额的伤害）。 */
	public static final float RECKONING_BLEED_MULTI = 1.5f;

	/** 「如数奉还」的两个伤害阈值（点）：超过前者多叠一笔，超过后者再多叠一笔。 */
	public static final int REPAYMENT_HIGH_DAMAGE = 10;
	public static final int REPAYMENT_LOW_DAMAGE  = 5;

	/** 「永志不忘」+2：击杀带标记单位时恢复的复仇账簿充能（百分点，100 点 = 基础满格）。 */
	public static final float UNFORGOTTEN_LEDGER_CHARGE = 5f;

	/** 「加倍清算！」的投入点数（0 = 未点）。全部加成只在这一个取点里读。 */
	private static int reckoning( Hero hero ){
		return talentPoints( hero, Talent.DOUBLE_RECKONING );
	}

	/** 「如数奉还」的投入点数（0 = 未点）。 */
	private static int repayment( Hero hero ){
		return talentPoints( hero, Talent.FULL_REPAYMENT );
	}

	/** 「永志不忘」的投入点数（0 = 未点）。 */
	private static int unforgotten( Hero hero ){
		return talentPoints( hero, Talent.EVERLASTING_GRUDGE );
	}

	/** 读某个转职天赋的投入点数（非忠义巡礼者一律按 0 处理）。 */
	private static int talentPoints( Hero hero, Talent talent ){
		if (hero == null || hero.subClass != HeroSubClass.LOYAL_PILGRIM) return 0;
		return hero.pointsInTalent( talent );
	}

	/**
	 * 「如数奉还」：这一击额外多叠几层怨恨标记。
	 *
	 * @param damage 敌人这一击<b>实际打出</b>的伤害（{@code Char.attack} 里交给
	 *               {@code enemy.damage} 的那个数值，已含护甲减免与易伤）
	 * @return 在基础 1 层之外额外追加的层数（0 ~ 2）
	 */
	public static int extraMarks( Hero hero, int damage ){
		int points = repayment( hero );
		if (points <= 0) return 0;

		//+1：只认「大于 10 点」
		if (points == 1) return damage > REPAYMENT_HIGH_DAMAGE ? 1 : 0;
		//+2：只认「大于 5 点」
		if (points == 2) return damage > REPAYMENT_LOW_DAMAGE ? 1 : 0;
		//+3：大于 5 点 → +1，大于 10 点 → +2（两条是「改为」，不是叠加）
		if (damage > REPAYMENT_HIGH_DAMAGE) return 2;
		if (damage > REPAYMENT_LOW_DAMAGE)  return 1;
		return 0;
	}

	/**
	 * 「永志不忘」+1：怨恨标记是否永不消退。
	 *
	 * <p>标记长在敌人身上、读的却是英雄的天赋，所以这里直接从 {@code Dungeon.hero} 取——
	 * 本作英雄是单例，不需要把天赋点数复制到每个敌人身上去。</p>
	 */
	public static boolean marksLastForever(){
		return unforgotten( Dungeon.hero ) >= 1;
	}

	//==========================================================================
	// 怨恨标记来源：敌人攻击命中中指长兄 / 任意友方单位
	//==========================================================================

	/**
	 * 「谁能给谁记仇」的统一判据（两个钩子共用，避免口径各写一套日后跑偏）。
	 *
	 * <p>三条同时成立才算：出手方是敌人、承伤方是<b>中指长兄本人或任意友方单位</b>、
	 * 且当前英雄确实是忠义巡礼者。</p>
	 */
	private static boolean grudgeEligible( Char attacker, Char victim ){
		if (attacker == null || victim == null) return false;
		if (attacker.alignment != Char.Alignment.ENEMY) return false;

		Hero hero = Dungeon.hero;
		if (hero == null || hero.subClass != HeroSubClass.LOYAL_PILGRIM) return false;

		//「角色和友方单位」：中指长兄本人（Hero.alignment == ALLY），或任意友方单位
		return victim == hero || victim.alignment == Char.Alignment.ALLY;
	}

	/**
	 * 攻击命中钩子（调用点：{@code Char.attack} 的命中分支，紧挨着 T2「永不遗忘」那行）。
	 *
	 * <p>近战、投掷武器等一切走 {@code Char.attack} 的攻击都在这里收口；命中英雄的<b>护盾</b>
	 * 也算命中（走的还是这条路）。</p>
	 *
	 * <p>基础叠 1 层；专精天赋「如数奉还」会按这一击的伤害再加 0~2 层（见 {@link #extraMarks}）。</p>
	 *
	 * @param attacker 出手方（{@code Char.attack} 里的 {@code this}）
	 * @param victim   被命中方（{@code Char.attack} 里的 {@code enemy}）
	 * @param damage   这一击交给 {@code victim.damage} 的伤害值（已含护甲减免 / 易伤，可能为 0）
	 */
	public static void onAttackLanded( Char attacker, Char victim, int damage ){
		if (!grudgeEligible( attacker, victim )) return;
		GrudgeMark.gain( attacker, 1 + extraMarks( Dungeon.hero, damage ) );
	}

	/**
	 * 远程 / 法术攻击的命中钩子（调用点：{@code Char.hit} 的 <b>3 参重载</b>）。
	 *
	 * <p>近战与投掷走 4 参重载，只有「法术弹」那一类攻击才走 3 参重载
	 * （{@code hit( this, enemy, true )} ⇒ {@code Char.attack} 之外的独立伤害路径）：
	 * 萨满、术士、DM 系列、元素、眼魔、尤格之拳、哨戒室的哨兵都是这一套，
	 * 它们把伤害直接交给 {@code enemy.damage(...)}，压根不走 {@code Char.attack}，
	 * 所以在 3 参重载里收一次口就等于把所有远程弹道一并管住，日后新怪也不会漏。</p>
	 *
	 * <p><b>只叠基础 1 层</b>：命中判定发生在伤害结算之前，这一刻还不知道这一击打了多少，
	 * 「如数奉还」那种按伤害判档的追加层数只能在 {@link #onAttackLanded} 里做。
	 * 想要远程也吃满追加层数，就得把判档挪到伤害之后（届时再改）。</p>
	 */
	public static void onRangedAttackHit( Char attacker, Char victim ){
		if (!grudgeEligible( attacker, victim )) return;
		GrudgeMark.gain( attacker, 1 );
	}

	/**
	 * 击杀钩子（调用点：{@code Mob.die} 的英雄击杀分支，紧挨着复仇账簿那次充能）。
	 *
	 * <p>专精天赋「永志不忘」+2：<b>击杀</b>带有怨恨标记的单位时，额外恢复 5% 的复仇账簿充能。
	 * 与「永不遗忘」的报复对象不同，这里只认「英雄击杀」——调用点本身已经是在
	 * {@code Mob.die} 的击杀归属白名单分支里，所以不需要再判一次。</p>
	 *
	 * <p>必须在 {@code super.die()} 之前调用：那之后 buff 会被一并清理。</p>
	 */
	public static void onEnemyKilled( Hero hero, Mob mob ){
		if (hero == null || mob == null) return;
		if (unforgotten( hero ) < 2) return;
		if (mob.buff( GrudgeMark.class ) == null) return;

		RevengeLedger.chargeByTalent( hero, UNFORGOTTEN_LEDGER_CHARGE );
	}

	//==========================================================================
	// 常驻 buff 本体
	//==========================================================================

	/** 上一次画在按钮右下角的层数，用来判断「要不要重建按钮上的数字」。 */
	private int shownMarks = -1;

	@Override
	public boolean act(){
		//子职业变了（理论上不会）就自我回收
		if (!(target instanceof Hero) || ((Hero) target).subClass != HeroSubClass.LOYAL_PILGRIM){
			detach();
			return true;
		}

		//常时占主槽：只有在别人（狂暴 / 连击 / 武技…）拿着槽位时才抢回来，
		//已经是自己就什么都不做——否则每回合都会触发一次按钮重建
		//（副槽上的「切换主副」按钮与本槽互不相干，见 ActionIndicator 的双槽位说明）
		if (ActionIndicator.action != this){
			ActionIndicator.setAction( this );
			shownMarks = highestMarksInView();
		} else {
			int marks = highestMarksInView();
			if (marks != shownMarks){
				shownMarks = marks;
				ActionIndicator.refresh();
			}
		}

		spend( TICK );
		return true;
	}

	//==========================================================================
	// 右下角按钮（ActionIndicator.Action）
	//==========================================================================

	private boolean shouldShowAction(){
		return target instanceof Hero && ((Hero) target).subClass == HeroSubClass.LOYAL_PILGRIM;
	}

	@Override
	public String actionName(){
		return Messages.get( this, "action_name" );
	}

	@Override
	public int actionIcon(){
		return HeroIcon.LOYAL_PILGRIM;
	}

	/** 右下角小字：视野内敌人的最高怨恨标记层数（＝当前最高可用招式等级）。 */
	@Override
	public Visual secondaryVisual(){
		BitmapText txt = new BitmapText( PixelScene.pixelFont );
		txt.text( Integer.toString( Math.max( 0, highestMarksInView() ) ) );
		txt.hardlight( CharSprite.NEGATIVE );
		txt.measure();
		return txt;
	}

	@Override
	public int indicatorColor(){
		//与怨恨标记图标同色系的紫
		return 0x9922CC;
	}

	@Override
	public void doAction(){
		GameScene.show( new WndVengeanceSkill( this ) );
	}

	@Override
	public boolean attachTo( Char target ){
		if (super.attachTo( target )){
			if (shouldShowAction()){
				ActionIndicator.setAction( this );
			}
			return true;
		}
		return false;
	}

	@Override
	public void detach(){
		super.detach();
		ActionIndicator.clearAction( this );
	}

	@Override
	public String name(){
		return Messages.get( this, "name" );
	}

	@Override
	public String desc(){
		return Messages.get( this, "desc" );
	}

	//无自有字段需要存；保留空存档方法以明确该 buff 随存档持久化（基类已处理）
	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		//读档兜底注册右下角按钮：Hero.restoreFromBundle 先恢复 buffs（attachTo）后恢复 subClass，
		//attachTo 里依赖 subClass 的判断必然失败；本 buff 只会在转职 LOYAL_PILGRIM 时被施加，
		//buff 存在即代表子类正确，故在此直接注册（同 ArtTechniques / ScorchingWound 的写法）
		ActionIndicator.setAction( this );
	}

	//==========================================================================
	// 招数定义
	//==========================================================================

	/**
	 * 五式复仇技能。{@link #cost} ＝ 使用该式需要（并会消耗）目标身上的怨恨标记层数，
	 * 同时也是它的解锁层数。
	 */
	public enum Move {

		/** 1 层：踏碎——对选中目标的一次重踏（单体），不消耗回合。 */
		STOMP( 1 ),
		/** 2 层：踢飞——跳到目标身边打一下，并击退落点周围的敌人。 */
		KICK( 2 ),
		/** 3 层：穿腹——攻击范围内的目标各吃一次攻击 + 同等流血。 */
		PIERCE( 3 ),
		/** 4 层：仇怨重踏——攻击范围内三段伤害，末段击退 3 格。 */
		RESTOMP( 4 ),
		/** 5 层：即刻处刑——换出莱瓦汀，对目标三段（50/100/150%）。 */
		EXECUTION( 5 );

		public final int cost;

		Move( int cost ){
			this.cost = cost;
		}

		public String title(){
			//键式：actors.buffs.vengeancearts$move.<枚举小写>.name
			return Messages.get( VengeanceArts.Move.class, name().toLowerCase( Locale.ENGLISH ) + ".name" );
		}

		public String desc(){
			return Messages.get( VengeanceArts.Move.class, name().toLowerCase( Locale.ENGLISH ) + ".desc", cost );
		}
	}

	//==========================================================================
	// 可用性
	//==========================================================================

	/** 视野内是否有敌人带着足够本式消耗的怨恨标记（招数列表按钮是否可点）。 */
	public boolean canUse( Move move ){
		return highestMarksInView() >= move.cost;
	}

	/** 视野内敌人的最高怨恨标记层数。 */
	public static int highestMarksInView(){
		if (Dungeon.level == null) return 0;
		int best = 0;
		for (Char ch : Actor.chars()){
			if (ch == null || ch == Dungeon.hero || !ch.isAlive()) continue;
			if (ch.alignment != Char.Alignment.ENEMY) continue;
			if (!Dungeon.level.heroFOV[ch.pos]) continue;
			best = Math.max( best, GrudgeMark.stacksOn( ch ) );
		}
		return best;
	}

	//==========================================================================
	// 选目标 → 结算
	//==========================================================================

	/** 点击招数：先选目标（带标记的敌人），再做距离检查与扣层。 */
	public void useMove( final Move move ){
		if (!(target instanceof Hero)) return;
		final Hero hero = (Hero) target;

		GameScene.selectCell( new CellSelector.Listener() {
			@Override
			public void onSelect( Integer cell ){
				if (cell == null) return;

				Char enemy = Actor.findChar( cell );
				if (enemy == null || enemy == hero
						|| enemy.alignment != Char.Alignment.ENEMY
						|| !Dungeon.level.heroFOV[cell]
						|| hero.isCharmedBy( enemy )){
					GLog.w( Messages.get( VengeanceArts.class, "bad_target" ) );
					return;
				}

				if (GrudgeMark.stacksOn( enemy ) < move.cost){
					GLog.w( Messages.get( VengeanceArts.class, "need_marks", move.cost ) );
					return;
				}

				//各式的距离要求
				switch (move){
					case STOMP:
						//踏碎要求目标相邻（脚下八格），与武器攻击距离无关
						if (Dungeon.level.distance( hero.pos, enemy.pos ) > 1){
							GLog.w( Messages.get( VengeanceArts.class, "too_far" ) );
							return;
						}
						break;
					case KICK: {
						if (Dungeon.level.distance( hero.pos, enemy.pos ) > KICK_RANGE){
							GLog.w( Messages.get( VengeanceArts.class, "too_far" ) );
							return;
						}
						int dest = kickDest( hero, enemy );
						if (dest == -1){
							GLog.w( Messages.get( VengeanceArts.class, "no_space" ) );
							return;
						}
						//只有真的需要跳过去时才受「定身」限制；已经贴着目标就地出手，不移动
						if (dest != hero.pos && hero.rooted){
							PixelScene.shake( 1, 1f );
							GLog.w( Messages.get( VengeanceArts.class, "rooted" ) );
							return;
						}
						break;
					}
					case PIERCE:
					case RESTOMP:
						if (Dungeon.level.distance( hero.pos, enemy.pos ) > weaponReach( hero )){
							GLog.w( Messages.get( VengeanceArts.class, "too_far" ) );
							return;
						}
						break;
					case EXECUTION:
						//处刑换出来的莱瓦汀是普通近战武器（攻击距离 1），要求相邻
						if (Dungeon.level.distance( hero.pos, enemy.pos ) > 1){
							GLog.w( Messages.get( VengeanceArts.class, "too_far" ) );
							return;
						}
						break;
				}

				useMoveOnTarget( hero, enemy, move );
			}

			@Override
			public String prompt(){
				return Messages.get( VengeanceArts.class, "prompt" );
			}
		} );
	}

	private void useMoveOnTarget( final Hero hero, final Char enemy, final Move move ){
		//① 扣标记：扣不动就整式中止（绝不空放，也绝不扣到负数）
		if (GrudgeMark.consume( enemy, move.cost ) < move.cost){
			GLog.w( Messages.get( VengeanceArts.class, "need_marks", move.cost ) );
			return;
		}

		GLog.i( Messages.get( VengeanceArts.class, "invoked", move.title() ) );

		//中指长兄的台词：「犯规！！」——一击即出的制式喊话，五式共用一条。
		//放在这里＝标记已经扣掉、这一式确定成立的那一刻（下面的 switch 只管演出与结算）。
		MiddleFingerVoice.playCounter();

		final int points = reckoning( hero );

		switch (move){
			case STOMP:   doStomp( hero, enemy, points ); break;
			case KICK:    doKick( hero, enemy, points ); break;
			case PIERCE:  doPierce( hero, enemy, points ); break;
			case RESTOMP: doRestomp( hero, enemy, points ); break;
			case EXECUTION: doExecution( hero, enemy, points ); break;
		}
	}

	//--------------------------------------------------------------------------
	// 1 层：踏碎
	//--------------------------------------------------------------------------

	/**
	 * 踏碎：对<b>被选中的那一个</b>敌人造成 {@code (S-5) ~ (S+15)}（S＝当前力量），护甲照常减免，
	 * <b>不消耗回合</b>——所以这里刻意不调 {@code hero.busy()}、也不调 {@code spendAndNext}，
	 * 只用表现（震屏 + 冲击波 + 血花）交代这一下。
	 *
	 * <p><b>只打单体</b>：脚下的冲击波与震屏照旧演出，但伤害只落在选中的那个目标身上，
	 * 相邻的其它敌人不会被波及（2026-09-16 由「周围八格全体」改为单体）。</p>
	 */
	private void doStomp( final Hero hero, final Char enemy, int points ){
		final int str = hero.STR();

		if (hero.sprite != null) hero.sprite.operate( hero.pos );
		WandOfBlastWave.BlastWave.blast( hero.pos );
		PixelScene.shake( 1, 0.4f );
		Invisibility.dispel();

		//只有被选中的目标吃伤害；它中途死了（理论上不会）就只剩演出
		if (enemy == null || !enemy.isAlive() || enemy.alignment != Char.Alignment.ENEMY) return;

		int dmg = Hero.heroDamageIntRange(
				STOMP_MIN_BASE + STOMP_MIN_PER_STR * str,
				STOMP_MAX_BASE + STOMP_MAX_PER_STR * str );
		dmg -= enemy.drRoll();

		//踏碎的打击音：本式不走 hero.attack（无命中判定、必中），所以自己补上——
		//原版 HIT_STRONG 那一套在这里是空的，之前只有语音没有攻击音。
		MiddleFingerVoice.playStrike();

		if (dmg > 0){
			enemy.damage( dmg, hero );
			if (enemy.sprite != null && hero.sprite != null){
				enemy.sprite.bloodBurstA( hero.sprite.center(), dmg );
			}
		} else if (enemy.sprite != null){
			enemy.sprite.flash();
		}

		//加倍清算 +1：踏碎的攻击对目标造成 5 回合易伤
		if (points >= 1 && enemy.isAlive()){
			Buff.prolong( enemy, Vulnerable.class, RECKONING_VULNERABLE );
		}
	}

	//--------------------------------------------------------------------------
	// 2 层：踢飞
	//--------------------------------------------------------------------------

	/**
	 * 踢飞：跳到目标身边，对其造成一次攻击，并击退落点周围八格的全部敌人。
	 *
	 * <p>落点由 {@link #kickDest} 决定；<b>已经贴着目标时落点就是原地</b>，此时不跳、不移动、
	 * 也不检查「定身」，直接原地结算这一脚。</p>
	 */
	private void doKick( final Hero hero, final Char enemy, final int points ){
		final int dest = kickDest( hero, enemy );
		if (dest == -1){
			GLog.w( Messages.get( VengeanceArts.class, "no_space" ) );
			return;
		}

		hero.busy();

		//真正踢出去的那一下（不论有没有先跳过去，都走这里）
		final Callback strike = new Callback(){
			@Override
			public void call(){
				hero.sprite.attack( enemy.pos, new Callback(){
					@Override
					public void call(){
						AttackIndicator.target( enemy );
						if (hero.attack( enemy, 1f, 0, Char.INFINITE_ACCURACY )){
							MiddleFingerVoice.playStrike();
						}

						//击退落点周围八格内的所有敌人——主目标必定与落点相邻，天然包含在内
						for (int i : PathFinder.NEIGHBOURS8){
							Char ch = Actor.findChar( hero.pos + i );
							if (ch == null || ch == hero || !ch.isAlive()
									|| ch.alignment != Char.Alignment.ENEMY) continue;

							Ballistica trajectory = new Ballistica( ch.pos, ch.pos + i, Ballistica.MAGIC_BOLT );
							WandOfBlastWave.throwChar( ch, trajectory, KICK_KNOCKBACK, true, false, hero );

							//加倍清算 +1：踢飞在击退周围目标后还会施加 3 回合麻痹
							if (points >= 1 && ch.isAlive()){
								Buff.prolong( ch, Paralysis.class, RECKONING_PARALYSIS );
							}
						}

						WandOfBlastWave.BlastWave.blast( hero.pos );
						PixelScene.shake( 1, 0.5f );
						Invisibility.dispel();
						hero.spendAndNext( hero.attackDelay() );
					}
				} );
			}
		};

		if (dest == hero.pos){
			//已经临近目标：省掉跳跃，原地起脚
			strike.call();
			return;
		}

		hero.sprite.jump( hero.pos, dest, new Callback(){
			@Override
			public void call(){
				hero.move( dest );
				Dungeon.level.occupyCell( hero );
				Dungeon.observe();
				GameScene.updateFog();
				strike.call();
			}
		} );
	}

	//--------------------------------------------------------------------------
	// 3 层：穿腹
	//--------------------------------------------------------------------------

	/** 穿腹：攻击范围内所有敌人各吃一次攻击，并各获得等同于该次伤害的流血。 */
	private void doPierce( final Hero hero, final Char enemy, final int points ){
		hero.busy();
		hero.sprite.attack( enemy.pos, new Callback() {
			@Override
			public void call(){
				for (Char ch : targetsInReach( hero )){
					AttackIndicator.target( ch );

					int before = ch.HP;
					if (hero.attack( ch, 1f, 0, Char.INFINITE_ACCURACY )){
						MiddleFingerVoice.playStrike();
					}
					int dealt = Math.max( 0, before - ch.HP );
					if (dealt <= 0 || !ch.isAlive()) continue;

					if (points >= 2 && ch.isImmune( Bleeding.class )){
						//加倍清算 +2：免疫流血的目标改为再补 0.5 倍伤害（合计 1.5 倍）
						ch.damage( Math.round( dealt * (RECKONING_BLEED_MULTI - 1f) ), hero );
					} else {
						float bleed = (points >= 2) ? dealt * RECKONING_BLEED_MULTI : dealt;
						Buff.affect( ch, Bleeding.class ).set( bleed );
					}
				}

				Invisibility.dispel();
				hero.spendAndNext( hero.attackDelay() );
			}
		} );
	}

	//--------------------------------------------------------------------------
	// 4 层：仇怨重踏
	//--------------------------------------------------------------------------

	/** 仇怨重踏：攻击范围内所有敌人各吃三段攻击，最后一段把它们各击退 3 格。 */
	private void doRestomp( final Hero hero, final Char enemy, final int points ){
		hero.busy();
		hero.sprite.attack( enemy.pos, new Callback() {
			@Override
			public void call(){
				//先把目标快照下来：击退会改变位置，边打边筛会漏人
				ArrayList<Char> targets = targetsInReach( hero );

				for (Char ch : targets){
					AttackIndicator.target( ch );

					for (int hit = 0; hit < RESTOMP_HITS; hit++){
						if (!ch.isAlive()) break;
						if (hero.attack( ch, 1f, 0, Char.INFINITE_ACCURACY )){
							MiddleFingerVoice.playStrike();
						}
					}

					if (!ch.isAlive()) continue;

					//最后一段伤害将目标击退 3 格（沿英雄→目标的直线）
					int dir = ch.pos - hero.pos;
					Ballistica trajectory = new Ballistica( ch.pos, ch.pos + dir, Ballistica.MAGIC_BOLT );
					WandOfBlastWave.throwChar( ch, trajectory, RESTOMP_KNOCKBACK, false, false, hero );

					//加倍清算 +2：攻击结束后施加 10 回合眩晕 / 恍惚 / 失明
					if (points >= 2 && ch.isAlive()){
						Buff.prolong( ch, Vertigo.class,   RECKONING_SEAL );
						Buff.prolong( ch, Daze.class,      RECKONING_SEAL );
						Buff.prolong( ch, Blindness.class, RECKONING_SEAL );
					}
				}

				Invisibility.dispel();
				hero.spendAndNext( hero.attackDelay() );
			}
		} );
	}

	//--------------------------------------------------------------------------
	// 5 层：即刻处刑
	//--------------------------------------------------------------------------

	/**
	 * 即刻处刑：主手若未持莱瓦汀则立刻把它换上来（绕过解封门槛），随后 50%/100%/150% 三段。
	 *
	 * <p><b>「无视力量惩罚」</b>：莱瓦汀的力量需求是 22，力量不足时的惩罚是
	 * 「精准 ÷1.5ⁿ、攻击延迟 ×1.2ⁿ、不能偷袭、不能用武技」（{@code Weapon.accuracyFactor} /
	 * {@code Weapon.baseDelay} / {@code Hero.canSurpriseAttack}）。本式通过临时挂上
	 * {@link ExecutionAftermath 处刑余威} 把这条惩罚整体豁免掉——
	 * 三段攻击期间一直是生效状态，所以连攻速代价都是在豁免期内取的。</p>
	 *
	 * <p><b>力量加成</b>：力量已满足需求时，{@code MeleeWeapon.damageRoll} 本就会给
	 * {@code 0 ~ (力量−需求)} 的掷值加成；本式在此基础上<b>再加同样一份</b>
	 * （{@code dmgBonus}），所以超出 3 点力量时三段各自获得 {@code 0~6} 的额外伤害。</p>
	 */
	private void doExecution( final Hero hero, final Char enemy, final int points ){
		//① 换出莱瓦汀（主手未持有时）
		SealedSwordBase sword = SealedSwordBase.forceIntoMainHand( hero, Laevateinn.class );
		if (sword == null){
			GLog.w( Messages.get( VengeanceArts.class, "no_laevateinn" ) );
		}

		//② 超出力量需求的部分（未满足需求则为 0，只有基础面板）
		final int exStr = (sword == null) ? 0 : Math.max( 0, hero.STR() - sword.STRReq() );

		//③ 本式的力量惩罚豁免：借余威 buff 取一次「无惩罚」的攻击延迟
		final float delay = executionDelay( hero );

		hero.busy();
		hero.sprite.attack( enemy.pos, new Callback() {
			@Override
			public void call(){
				for (float multi : EXECUTION_MULTI){
					if (!enemy.isAlive()) break;
					AttackIndicator.target( enemy );

					//「额外计入一倍的力量加成」：再加一份与 damageRoll 同分布的掷值
					int bonus = (exStr > 0) ? Hero.heroDamageIntRange( 0, exStr ) : 0;

					if (hero.attack( enemy, multi, bonus, Char.INFINITE_ACCURACY )){
						MiddleFingerVoice.playStrike();
					}
				}

				//④ 加倍清算 +3：清空目标剩余的怨恨标记，3 回合基础 + 每层 5 回合的处刑余威
				if (points >= 3){
					int cleared = GrudgeMark.clearAll( enemy );
					Buff.affect( hero, ExecutionAftermath.class,
							AFTERMATH_BASE + AFTERMATH_PER_MARK * cleared );
				}

				Invisibility.dispel();
				hero.spendAndNext( delay );
			}
		} );
	}

	/**
	 * 取「即刻处刑」这一下的攻击延迟：必须在力量惩罚被豁免的状态下取。
	 *
	 * <p>惩罚是在 {@code Weapon.baseDelay()} 内部按力量差额算的，从外面绕不过去，所以这里
	 * 临时挂上 {@link ExecutionAftermath} 取一次数，取完立刻撤掉（同一个回合内挂上又撤掉，
	 * {@code act()} 根本不会跑到，不会有残留）。<b>已经有余威时不动它</b>——那说明刚用过
	 * 即刻处刑（天赋 +3），本次直接沿用既有的豁免。</p>
	 */
	private static float executionDelay( Hero hero ){
		if (hero.buff( ExecutionAftermath.class ) != null){
			return hero.attackDelay();
		}
		ExecutionAftermath temp = Buff.affect( hero, ExecutionAftermath.class, Actor.TICK );
		float delay = hero.attackDelay();
		if (temp != null) temp.detach();
		return delay;
	}

	//==========================================================================
	// 工具
	//==========================================================================

	/** 当前的武器攻击距离（不小于 1）。 */
	private static int weaponReach( Hero hero ){
		KindOfWeapon wep = hero.belongings.attackingWeapon();
		return (wep == null) ? 1 : Math.max( 1, wep.reachFactor( hero ) );
	}

	/** 攻击范围内（武器攻击距离内、可见、存活）的全部敌方单位快照。 */
	private static ArrayList<Char> targetsInReach( Hero hero ){
		ArrayList<Char> targets = new ArrayList<>();
		int reach = weaponReach( hero );
		for (Mob ch : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (!ch.isAlive() || ch.alignment != Char.Alignment.ENEMY) continue;
			if (!Dungeon.level.heroFOV[ch.pos]) continue;
			if (Dungeon.level.distance( hero.pos, ch.pos ) > reach) continue;
			targets.add( ch );
		}
		return targets;
	}

	/**
	 * 踢飞的落点判定，按「能不移动就不移动、能近就近」的顺序放宽：
	 * <ol>
	 *   <li><b>已经贴着目标</b>（距离 ≤ 1）→ 直接返回 {@code hero.pos}，原地起脚；</li>
	 *   <li>照英勇之跃 {@link #leapCell} 的弹道碰撞点；</li>
	 *   <li>上面都不行时，在目标周围八格里挑一格<b>可通行且没人占</b>的空地（取离英雄最近的一格）。</li>
	 * </ol>
	 * 都失败才返回 -1（目标被彻底围死且英雄贴不上去）。
	 */
	private static int kickDest( Hero hero, Char enemy ){
		//① 本来就相邻：不用跳
		if (Dungeon.level.distance( hero.pos, enemy.pos ) <= 1) return hero.pos;

		//② 照英勇之跃的弹道
		int direct = leapCell( hero, enemy );
		if (direct != -1) return direct;

		//③ 放宽：目标四周任意一格空地，取离英雄最近的
		int best = -1;
		int bestDist = Integer.MAX_VALUE;
		for (int i : PathFinder.NEIGHBOURS8){
			int cell = enemy.pos + i;
			if (cell < 0 || cell >= Dungeon.level.length()) continue;
			if (!Dungeon.level.passable[cell]) continue;
			if (Actor.findChar( cell ) != null) continue;

			int d = Dungeon.level.distance( hero.pos, cell );
			if (d < bestDist){
				bestDist = d;
				best = cell;
			}
		}
		return best;
	}

	/**
	 * 找「跳到目标身边」的落点（照英勇之跃 {@code HeroicLeap.activate}）：
	 * 沿英雄→目标的弹道推到碰撞点，若该格被占则再往回退，退无可退返回 -1。
	 *
	 * <p>这只是 {@link #kickDest} 的第二步，目标被围死时还有「八格空地」那条兜底。</p>
	 */
	private static int leapCell( Hero hero, Char enemy ){
		Ballistica route = new Ballistica( hero.pos, enemy.pos, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID );
		int cell = route.collisionPos;
		int backTrace = route.dist - 1;

		while (Actor.findChar( cell ) != null && cell != hero.pos){
			if (backTrace < 0) return -1;
			cell = route.path.get( backTrace );
			backTrace--;
		}

		if (cell == hero.pos || !Dungeon.level.passable[cell]) return -1;
		return cell;
	}

	//==========================================================================
	// 「处刑余威」：暂存「无视莱瓦汀力量惩罚」的状态
	//==========================================================================

	/**
	 * 「处刑余威」（即刻处刑的专精天赋「加倍清算！」+3 产物）：{@value #AFTERMATH_BASE} 回合起步、
	 * 每清掉一层怨恨标记再加 {@value #AFTERMATH_PER_MARK} 回合。
	 *
	 * <p>它本身什么都不做，只是「力量惩罚豁免」的判据——{@code Weapon.accuracyFactor} /
	 * {@code Weapon.baseDelay} / {@code Hero.canSurpriseAttack} 都通过
	 * {@link SealedSwordBase#ignoresStrengthPenalty} 读它。</p>
	 */
	public static class ExecutionAftermath extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon(){
			return com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.GRUDGE_MARK;
		}
	}

	/** 是否处于「处刑余威」（＝封印之剑系列暂时无视力量惩罚）。 */
	public static boolean ignoresStrengthPenalty( Char ch ){
		return ch != null && ch.buff( ExecutionAftermath.class ) != null;
	}
}
