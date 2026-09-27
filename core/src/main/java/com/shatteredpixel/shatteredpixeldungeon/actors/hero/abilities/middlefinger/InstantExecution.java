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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.middlefinger;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ExecutionUnleashed;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.RevengeLedger;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Laevateinn;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SealedSwordBase;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Door;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

/**
 * 中指 长兄 盔甲技能「<b>即刻处刑[莱瓦汀]</b>」（2026-09-17）。
 *
 * <p>消耗 <b>80</b> 点盔甲充能（「只属于我的传说之剑」会按血量下调，见 {@link #chargeUse}），
 * 对<b>相邻</b>的一个敌人打出一套五段的处刑连段：</p>
 *
 * <table>
 *   <tr><th>段</th><th>内容</th></tr>
 *   <tr><td>1~3</td><td>共 {@value #COMBO_HITS} 段攻击，每段 <b>30%</b> 倍率（{@link #COMBO_MULTI}），每段各播一次挥砍动画。</td></tr>
 *   <tr><td>3（附加）</td><td>第三段命中后，沿「长兄 → 目标」的直线把目标<b>击飞 {@value #KNOCKBACK} 格</b>。</td></tr>
 *   <tr><td>4</td><td><b>投出莱瓦汀追击</b>：莱瓦汀旋转飞出、命中目标，造成一次基于<b>莱瓦汀面板</b>的攻击伤害。</td></tr>
 *   <tr><td>5</td><td><b>跃至目标身边</b>，以一记基于力量的收尾重击终结：
 *       {@code (5+力量) ~ (15+3×力量)}，并打出<b>震击</b>演出。</td></tr>
 * </table>
 *
 * <h3>开打前的三件事</h3>
 * <ol>
 *   <li><b>换出莱瓦汀</b>：主手未持有时立刻换上（{@link SealedSwordBase#forceIntoMainHand}，
 *       绕过解封门槛）——与「复仇技艺 · 即刻处刑」同一条路径。手上根本没有封印之剑系列时
 *       只给一条提示，连段退化为「用当前武器打三段 + 投当前武器 + 收尾」。</li>
 *   <li><b>吞账簿</b>：天赋「没有打开账簿的必要」把复仇账簿的<b>全部</b>充能一次性折成伤害加成
 *       （{@link #devourLedger}）。</li>
 *   <li><b>临时等级</b>：天赋「好久没解放到这种程度了」给莱瓦汀系列
 *       {@link ExecutionUnleashed}（1/2/3 级、10/15/20 回合）。</li>
 * </ol>
 *
 * <h3>为什么整段连段由「真实时间」推进，而不靠贴图回调、也不排 Actor</h3>
 *
 * <p>（第一版是「贴图回调 + 保底计时器」双重保险，实测<b>击退之后就没下文</b>；
 * 根因不在动画回调，而在<b>回合线程</b>——这一节是本技能最需要记住的东西。）</p>
 *
 * <ol>
 *   <li>英雄一 {@code busy()}，回合线程就<b>停在英雄身上</b>：{@code Hero.act()} 在
 *       {@code curAction == null} 时只 {@code ready()} 一下就返回 {@code false}，
 *       于是 {@code Actor.process()} 走进 {@code wait()} 挂起，而 {@code Actor.current} 仍指着英雄
 *       （{@code Actor.processing()} 恒为 true）。</li>
 *   <li>{@code GameScene.update()} 只在 {@code !Actor.processing()} 时才去 {@code notify()}
 *       唤醒回合线程 ⇒ <b>英雄持有回合期间回合线程不会醒，任何 Actor 都不会动。</b></li>
 *   <li>而 {@code WandOfBlastWave.throwChar} 恰恰是<b>排一个 {@code Pushing} Actor</b> 去落位：
 *       要等回合线程跑到它的 {@code act()}，内部的 {@code Effect} 才会被创建、敌人的 {@code pos}
 *       才会写回。⇒ 在英雄自己的回合里调用它，<b>击退得等这一回合交出去之后才真正发生</b>。</li>
 * </ol>
 *
 * <p>第一版的症状就是这么来的：三段伤害照打，但敌人<b>原地不动</b>，后面两步读到的是击飞<b>前</b>
 * 的位置 ⇒ 投刀时目标还贴脸（导弹几乎不用飞，一闪而过看不见）、
 * {@link #leapDest} 判定「已经相邻」⇒ 收尾就地补刀、<b>不跳</b>；
 * 等回合结束，Pushing 才把那一记击飞补上（看起来像「击退发生在最后」）。</p>
 *
 * <p>现在的做法：<b>整段连段只依赖渲染线程</b>——由挂在场景里的透明 {@link Visual} 按
 * {@code Game.elapsed} 逐拍驱动（{@code effects.Pushing.Effect} 就是这么走的，回合线程停着它照样跑）。
 * 连击退也自己算落点、自己演、自己写回 {@code pos}（{@code Pushing.Effect} 原本也是渲染线程写
 * {@code ch.pos}），<b>全程不排任何 Actor</b>。于是整段连段只有一条推进路径：
 * 既不依赖 {@code CharSprite.animCallback}（单槽，{@code attack/operate/zap} 谁后调谁覆盖），
 * 也不会被回合线程的停摆卡死。</p>
 *
 * <p>贴图动画照播（三段各挥一次、投掷时 {@code zap}、收尾落地再挥一次），但它们只是装饰：
 * 一律传空回调 {@link #NO_OP}，避免走进 {@code CharSprite.onComplete} 里
 * 「自动 {@code idle()} + 调 {@code ch.onXxxComplete()}」那条岔路——对英雄来说，
 * 那条路会附带<b>一次真正的攻击</b>并提前把回合结算掉。</p>
 *
 * <h3>与「复仇技艺 · 即刻处刑」同名</h3>
 * <p>两者都叫「即刻处刑」，本技能加了 {@code [莱瓦汀]} 后缀以示区分；
 * 技艺那一式在 {@code actors.buffs.vengeancearts$move.execution}，与本技能的键互不相干。</p>
 */
public class InstantExecution extends ArmorAbility {

	/** 前三段的段数。 */
	public static final int COMBO_HITS = 3;

	/** 前三段每段的攻击倍率。 */
	public static final float COMBO_MULTI = 0.30f;

	/** 第三段命中后把目标击飞的格数。 */
	public static final int KNOCKBACK = 5;

	/** 收尾重击的伤害区间：{@code (5+力量) ~ (15+3×力量)}（力量取 {@code Hero.STR()}，已含「仇怨」等加成）。 */
	public static final int FINISH_MIN_BASE = 5,  FINISH_MIN_PER_STR = 1;
	public static final int FINISH_MAX_BASE = 15, FINISH_MAX_PER_STR = 3;

	/** 两次挥击之间的间隔（秒）。英雄的攻击动画是 15fps×4 帧 ≈ 0.27 秒，取 0.30 让它播完。 */
	private static final float BEAT_HIT = 0.30f;

	/** 第三段之后、投刀之前的间隔（秒）：击飞演出本身 0.15 秒，再留一点余韵。 */
	private static final float BEAT_KNOCKBACK = 0.55f;

	/** 投刀的最短飞行时间（秒）：贴脸时也得看得见那一记旋转的飞刀。 */
	private static final float MIN_MISSILE_FLIGHT = 0.25f;

	/** {@code MissileSprite} 的飞行速度（像素/秒），与它内部的 {@code SPEED} 一致。 */
	private static final float MISSILE_SPEED = 240f;

	/**
	 * 空回调：本技能给所有贴图动画都传它。
	 *
	 * <p>{@code CharSprite.onComplete} 在 {@code animCallback == null} 时会自动
	 * {@code idle()} 并回调 {@code ch.onAttackComplete()} / {@code onOperateComplete()}；
	 * 对英雄来说 {@code onAttackComplete()} 会<b>再打一次真攻击并结算回合</b>，
	 * 所以这里统一压掉那条岔路——动画只当装饰。</p>
	 */
	private static final Callback NO_OP = new Callback() {
		@Override
		public void call(){ }
	};

	{
		baseChargeUse = 80f;
	}

	//==========================================================================
	// 充能
	//==========================================================================

	/**
	 * 「只属于我的传说之剑」（{@link Talent#instantExecutionChargeFactor}）：
	 * 血量低于 50% / 40% / 30% 时把消耗依次再压低 20% / 10% / 10%。
	 */
	@Override
	public float chargeUse( Hero hero ){
		return super.chargeUse( hero ) * Talent.instantExecutionChargeFactor( hero );
	}

	//==========================================================================
	// 使用
	//==========================================================================

	@Override
	public String targetingPrompt(){
		return Messages.get( this, "prompt" );
	}

	@Override
	protected void activate( ClassArmor armor, Hero hero, Integer target ){

		if (target == null) return;

		Char enemy = Actor.findChar( target );
		if (enemy == null || enemy == hero || !enemy.isAlive()
				|| enemy.alignment != Char.Alignment.ENEMY){
			GLog.w( Messages.get( this, "bad_target" ) );
			return;
		}
		//处刑是近身连段（换出来的莱瓦汀攻击距离就是 1），所以要求相邻
		if (Dungeon.level.distance( hero.pos, enemy.pos ) > 1){
			GLog.w( Messages.get( this, "too_far" ) );
			return;
		}

		//① 把莱瓦汀换上主手（绕过解封门槛；与「复仇技艺 · 即刻处刑」同一条路径）
		SealedSwordBase sword = SealedSwordBase.forceIntoMainHand( hero, Laevateinn.class );
		if (sword == null){
			GLog.w( Messages.get( this, "no_laevateinn" ) );
		}

		//② 天赋「没有打开账簿的必要」：吞掉复仇账簿的全部充能，折算成这一次的伤害加成
		final int ledgerBonus = devourLedger( hero );

		//③ 天赋「好久没解放到这种程度了」：莱瓦汀系列获得临时等级
		ExecutionUnleashed.apply( hero,
				Talent.executionUnleashLevels( hero ),
				Talent.executionUnleashDuration( hero ) );

		//④ 扣充能（「只属于我的传说之剑」的减免已含在 chargeUse 里）
		armor.charge -= chargeUse( hero );
		Item.updateQuickslot();

		//⑤ 投掷用的「莱瓦汀」：真的投出去的是个贴图，背包里的武器一动不动
		final Item blade = (sword != null) ? sword : hero.belongings.attackingWeapon();

		GLog.p( Messages.get( this, "cast" ) );
		hero.busy();

		if (hero.sprite == null || hero.sprite.parent == null){
			//没有贴图（理论上只在自检 / 无场景时）⇒ 连段无处可演，直接把回合交出去
			endTurn( hero );
			return;
		}

		//整套连段交给一个挂在场景里的计时器逐拍推进（见类注释：不排 Actor、不押贴图回调）
		hero.sprite.parent.add( new Execution( hero, enemy, blade, ledgerBonus ) );
	}

	//==========================================================================
	// 五段连段
	//==========================================================================

	/**
	 * 整套五段连段。它是一个挂在场景里的透明 {@link Visual}，<b>按真实时间逐拍推进</b>
	 * （与 {@code effects.Pushing.Effect} 同一路数：{@code Game.elapsed} 累加，到点做一件事再排下一拍）。
	 *
	 * <p>之所以不写成「一层套一层的贴图回调」：那样每深一层就多一个可能失约的环节，
	 * 而英雄 {@code busy()} 期间回合线程是停的，任何一次失约都会让世界永远停在英雄手里
	 * （见类注释）。这里只有一条推进路径，且每一步都自己检查前置条件；
	 * 另加一条总时长保险丝（{@link #WATCHDOG}），真遇上意料之外的情况也会强制收场交回合。</p>
	 */
	private static class Execution extends Visual {

		private final Hero hero;
		private final Char victim;
		private final Item blade;
		private final int bonus;

		/** 当前演到第几拍。 */
		private int phase = 0;

		/** 距离下一拍的剩余秒数。 */
		private float left = 0f;

		/**
		 * 整套连段的「保险丝」：正常对局约 2.4 秒走完，这里给足 2.5 倍余量。
		 *
		 * <p>它的作用不是演出，而是<b>保命</b>：万一某一拍的前置条件因为意料之外的情况
		 * 永远不成立（例如贴图被换掉、目标进了某种特殊状态），到点也要强制 {@link #finish()}——
		 * 英雄持有回合期间世界是停的（见类注释），一次没能收场就是永久卡死。</p>
		 */
		private static final float WATCHDOG = 6f;

		/** 已演出的总秒数（只为 {@link #WATCHDOG} 服务）。 */
		private float total = 0f;

		/** 收尾一击的落点（第 4 拍算出来，第 5 拍才用）。 */
		private int dest = -1;

		private boolean done = false;

		Execution( Hero hero, Char victim, Item blade, int bonus ){
			super( 0, 0, 0, 0 );
			this.hero = hero;
			this.victim = victim;
			this.blade = blade;
			this.bonus = bonus;
		}

		@Override
		public void update(){
			super.update();
			if (done) return;
			total += Game.elapsed;
			if (left > 0f){
				left -= Game.elapsed;
				if (left > 0f) return;
			}
			step();
		}

		private void step(){

			if (done) return;

			//场景没了 / 长兄没了 ⇒ 收场，务必把回合交出去（否则世界会一直停在长兄手里）
			if (hero.sprite == null || hero.sprite.parent == null || !hero.isAlive()){
				finish();
				return;
			}

			//保险丝烧断：不管演到哪一拍，立刻收场交回合
			if (total > WATCHDOG){
				finish();
				return;
			}

			switch (phase++){

				case 0:		//—— 第一段 ——
					swing();
					strike();
					left = BEAT_HIT;
					break;

				case 1:		//—— 第二段 ——
					swing();
					strike();
					left = BEAT_HIT;
					break;

				case 2:		//—— 第三段 + 击飞 ——
					swing();
					strike();
					if (!victim.isAlive()){ finish(); return; }
					knockBack();
					left = BEAT_KNOCKBACK;
					break;

				case 3:		//—— 第四段：投出莱瓦汀 ——
					if (!victim.isAlive()){ finish(); return; }
					left = throwBlade();
					break;

				case 4:		//—— 第四段命中 + 起跳 ——
					if (!victim.isAlive()){ finish(); return; }
					AttackIndicator.target( victim );
					hero.attack( victim, 1f, bonus, Char.INFINITE_ACCURACY );
					dest = leapDest( hero, victim );
					left = leap( dest );
					break;

				case 5:		//—— 第五段：落地 + 收尾重击 ——
					land( dest );
					if (victim.isAlive()){
						swing();
						finishStrike( hero, victim );
					}
					//「正中靶心啊！」——台词要等技能真的演完才出，所以挂在这一拍而不是 activate()。
					//放在 finish() 之前：此刻连段已经打完、长兄还压着目标，收尾的震屏也同步发生。
					MiddleFingerVoice.playExecution();
					finish();
					break;

				default:
					finish();
					break;
			}
		}

		/** 挥砍动画（纯装饰，见 {@link #NO_OP}）。 */
		private void swing(){
			hero.sprite.attack( victim.pos, NO_OP );
		}

		/** 一次 30% 倍率的斩击。 */
		private void strike(){
			if (!victim.isAlive()) return;
			AttackIndicator.target( victim );
			hero.attack( victim, COMBO_MULTI, bonus, Char.INFINITE_ACCURACY );
		}

		/**
		 * 第三段的附加：沿「长兄 → 目标」的直线把目标击飞，返回实际落点（没能移动则返回原格）。
		 *
		 * <p>方向用的就是原版那套两段式弹道——先从长兄打向目标、再从目标延伸到射线末端
		 * （{@code WandOfBlastWave.zap} / 长矛的 {@code doAbility} 都是这个写法），
		 * 这样「击飞方向远离长兄」是弹道本身保证的，不必自己拿格子下标去加。</p>
		 *
		 * <p>落位由本类自己演、自己写回：<b>不能</b>用 {@code WandOfBlastWave.throwChar}，
		 * 它排的 {@code Pushing} 是个 Actor，而英雄持有回合时回合线程是停的（见类注释）。
		 * 位移规则（老板减半、大型单位要求开阔地、落点被占则少推一格、离开门格顺手关门）
		 * 与原版 {@code throwChar} 保持一致；这里不需要碰撞伤害，所以那一支直接省略。</p>
		 */
		private void knockBack(){

			Ballistica route = new Ballistica( hero.pos, victim.pos, Ballistica.STOP_TARGET );
			route = new Ballistica( route.collisionPos,
					route.path.get( route.path.size() - 1 ), Ballistica.PROJECTILE );

			int power = KNOCKBACK;
			//体量大的老板只推一半（同原版）
			if (victim.properties().contains( Char.Property.BOSS )) power = (power + 1) / 2;

			int dist = Math.min( route.dist, power );
			if (dist <= 0 || victim.rooted
					|| victim.properties().contains( Char.Property.IMMOVABLE )){
				return;
			}

			//大型单位只能被推进开阔地
			if (Char.hasProp( victim, Char.Property.LARGE )){
				for (int i = 1; i <= dist; i++){
					if (!Dungeon.level.openSpace[route.path.get( i )]){
						dist = i - 1;
						break;
					}
				}
			}

			if (dist <= 0 || Actor.findChar( route.path.get( dist ) ) != null) dist--;
			if (dist <= 0) return;

			final int from = victim.pos;
			final int to = route.path.get( dist );
			if (to == from) return;

			if (victim.sprite == null || victim.sprite.parent == null){
				//没有贴图（理论上不会）⇒ 直接落位，别把连段卡在这
				victim.pos = to;
				Dungeon.level.occupyCell( victim );
			} else {
				//被推进视野时得让它显形（同 Pushing.act 开头那两行）
				if (Dungeon.level.heroFOV[from] || Dungeon.level.heroFOV[to]){
					victim.sprite.visible = true;
				}
				victim.sprite.parent.add( new Knockback( victim.sprite, from, to, new Callback() {
					@Override
					public void call(){
						//演出期间被占了位置就放弃位移（同原版 Pushing 的保险）
						if (Actor.findChar( to ) != null){
							victim.sprite.place( victim.pos );
							return;
						}
						victim.pos = to;
						if (Dungeon.level.map[from] == Terrain.OPEN_DOOR) Door.leave( from );
						Dungeon.level.occupyCell( victim );
						if (Dungeon.level.heroFOV[from] != Dungeon.level.heroFOV[to]){
							Dungeon.observe();
						}
						GameScene.sortMobSprites();
					}
				} ) );
			}

			PixelScene.shake( 1, 0.4f );
		}

		/** 第四段：投出手中的莱瓦汀追击目标（纯演出、不动背包），返回飞行秒数。 */
		private float throwBlade(){

			hero.sprite.zap( victim.pos, NO_OP );
			Sample.INSTANCE.play( Assets.Sounds.MISS, 0.6f, 0.6f, 1.5f );

			//莱瓦汀只是被 MissileSprite 借去当贴图（默认 720°/秒 ⇒ 自带旋转），
			//不从 belongings 里摘、也不改任何武器状态；伤害在第 4 拍用 hero.attack 结算，
			//那一刻主手正是莱瓦汀，所以读到的是它自己的面板（含火焰附加与「融化」proc）。
			((MissileSprite) hero.sprite.parent.recycle( MissileSprite.class ))
					.reset( hero.sprite, victim.pos, blade, null );

			//飞行距离按「格 × 16 像素 ÷ 240 像素每秒」估：导弹从长兄中心飞到目标格
			float flight = DungeonTilemap.SIZE
					* Dungeon.level.trueDistance( hero.pos, victim.pos ) / MISSILE_SPEED;
			return Math.max( MIN_MISSILE_FLIGHT, flight );
		}

		/** 起跳；返回腾空秒数（0 = 原地不动，下一拍立刻就到）。 */
		private float leap( int dest ){
			if (dest < 0 || dest == hero.pos) return 0f;
			float dist = Math.max( 1f, Dungeon.level.trueDistance( hero.pos, dest ) );
			//照原版 CharSprite.jump 的比例：高度 = 距离×2，时长 = 距离×0.1 秒
			hero.sprite.jump( hero.pos, dest, dist * 2, dist * 0.1f, null );
			return dist * 0.1f;
		}

		/** 落地：真的把长兄挪过去（与「英勇之跃」同一套写法）。 */
		private void land( int dest ){
			if (dest < 0 || dest == hero.pos) return;
			hero.move( dest );
			Dungeon.level.occupyCell( hero );
			Dungeon.observe();
			GameScene.updateFog();
		}

		/** 收尾：把长兄从挥砍姿势里放回待机，再把回合交出去。<b>全流程只允许走一次</b>。 */
		private void finish(){
			if (done) return;
			done = true;
			killAndErase();
			//别把长兄留在挥砍/腾空的最后一帧（挥砍与跳跃都不带回调，不会自己回待机）
			if (hero.sprite != null) hero.sprite.idle();
			endTurn( hero );
		}
	}

	/**
	 * 击飞演出：把精灵从原格滑到落点，播完调回调。
	 *
	 * <p>参数与节奏完全照 {@code effects.Pushing.Effect}（0.15 秒、先加速后减速的抛物线），
	 * 区别只在<b>它由渲染线程直接驱动</b>——不经过 {@code Pushing} 那个 Actor，
	 * 所以英雄持有回合时照样能演完、能落位。</p>
	 */
	private static class Knockback extends Visual {

		private static final float DELAY = 0.15f;

		private final CharSprite sprite;
		private final PointF end;
		private final Callback then;

		private float delay = 0f;
		private boolean finished = false;

		Knockback( CharSprite sprite, int from, int to, Callback then ){
			super( 0, 0, 0, 0 );

			this.sprite = sprite;
			this.then = then;

			point( sprite.worldToCamera( from ) );
			end = sprite.worldToCamera( to );

			//匀减速：初速 2×位移/时长、加速度 -初速/时长，正好在时长处停住（同 Pushing.Effect）
			speed.set( 2 * (end.x - x) / DELAY, 2 * (end.y - y) / DELAY );
			acc.set( -speed.x / DELAY, -speed.y / DELAY );
		}

		@Override
		public void update(){
			super.update();
			if (finished) return;

			if (sprite == null || sprite.parent == null){
				finish();
			} else if ((delay += Game.elapsed) < DELAY){
				sprite.x = x;
				sprite.y = y;
			} else {
				sprite.point( end );
				finish();
			}
		}

		private void finish(){
			finished = true;
			killAndErase();
			if (then != null) then.call();
		}
	}

	//==========================================================================
	// 伤害 / 落点
	//==========================================================================

	/**
	 * 收尾重击：{@code (5+力量) ~ (15+3×力量)} 的伤害（护甲照常减免），
	 * 并打出「震击」——脚下的冲击波 + 震屏，与「复仇技艺 · 踏碎」同一套写法。
	 *
	 * <p>不走 {@code hero.attack}：这一击的数值由设计指定为固定的力量公式，与武器面板无关。
	 * 力量取 {@code hero.STR()} ⇒ 「仇怨」等加成自动计入。</p>
	 */
	private static void finishStrike( Hero hero, Char victim ){

		if (victim == null || !victim.isAlive()) return;

		int str = hero.STR();
		int dmg = Hero.heroDamageIntRange(
				FINISH_MIN_BASE + FINISH_MIN_PER_STR * str,
				FINISH_MAX_BASE + FINISH_MAX_PER_STR * str );
		dmg -= victim.drRoll();

		AttackIndicator.target( victim );
		if (dmg > 0){
			victim.damage( dmg, hero );
			if (victim.sprite != null && hero.sprite != null){
				victim.sprite.bloodBurstA( hero.sprite.center(), dmg );
			}
		} else if (victim.sprite != null){
			victim.sprite.flash();
		}

		//震击：冲击波 + 震屏
		WandOfBlastWave.BlastWave.blast( hero.pos );
		PixelScene.shake( 1, 0.4f );
		Sample.INSTANCE.play( Assets.Sounds.HIT_STRONG );
	}

	/**
	 * 收尾一击的落点：跳到目标身边（照「英勇之跃」的弹道写法，落到目标前一格）。
	 *
	 * <p>返回 {@code hero.pos} 表示<b>不移动</b>——已经贴着目标、被定身、或目标被彻底围死，
	 * 这三种情况都直接<b>就地补刀</b>。整套连段的价值集中在这一击上，
	 * 宁可不走那两步，也不要让它因为地形卡掉。</p>
	 */
	private static int leapDest( Hero hero, Char victim ){

		if (Dungeon.level.distance( hero.pos, victim.pos ) <= 1) return hero.pos;
		if (hero.rooted) return hero.pos;

		Ballistica route = new Ballistica( hero.pos, victim.pos, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID );
		int cell = route.collisionPos;
		int backTrace = route.dist - 1;

		while (Actor.findChar( cell ) != null && cell != hero.pos){
			if (backTrace < 0) return hero.pos;
			cell = route.path.get( backTrace );
			backTrace--;
		}

		if (cell == hero.pos || !Dungeon.level.passable[cell]) return hero.pos;
		return cell;
	}

	/**
	 * 天赋「没有打开账簿的必要」：立刻<b>吞掉复仇账簿的全部充能</b>，按点数折算成伤害加成。
	 *
	 * <p>{@link RevengeLedger#consumeCharge} 本身就是「整笔付清」的语义（不够就一点都不扣），
	 * 这里传的正是当前全部点数，所以要么全额吞下、要么一分不动。账簿未装备 / 已诅咒时
	 * {@code find} 直接返回 null，天赋静默作废。</p>
	 *
	 * @return 这一次连段的每一段伤害各自获得的额外伤害（0 = 没得吃或没点天赋）
	 */
	private static int devourLedger( Hero hero ){

		float multi = Talent.ledgerDevourMultiplier( hero );
		if (multi <= 0f) return 0;

		RevengeLedger ledger = RevengeLedger.find( hero );
		if (ledger == null) return 0;

		int devoured = RevengeLedger.consumeCharge( hero, ledger.chargePoints() );
		if (devoured <= 0) return 0;

		int bonus = Math.round( devoured * multi );
		GLog.i( Messages.get( InstantExecution.class, "devour", devoured, bonus ) );
		return bonus;
	}

	/** 收走这一回合：显形 + 推进时间。 */
	private static void endTurn( Hero hero ){
		Invisibility.dispel();
		hero.spendAndNext( Actor.TICK );
	}

	//==========================================================================
	// 图标 / 天赋
	//==========================================================================

	@Override
	public int icon(){
		//专属槽位：hero_icons.png 第 17 行第 4 列 = 帧 131
		return HeroIcon.MIDDLE_FINGER_ABILITY_3;
	}

	@Override
	public Talent[] talents(){
		return new Talent[]{
				Talent.NO_LEDGER_NEEDED, Talent.OVERDUE_RELEASE, Talent.LEGENDARY_BLADE,
				Talent.HEROIC_ENERGY };
	}
}
