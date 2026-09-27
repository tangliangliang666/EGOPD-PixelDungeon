// 考验 CHESED（慈悲）/ GEBURA（严厉）的行为探针（真跑游戏类，不改任何游戏文件）
//
// 为什么要有它：verify_chesed_gebura.py 证明的是「钩子挂在哪、顺序对不对」——**结构**证据。
// 但「+25% 到底加没加」「是不是第 5 回合才回血」「无敌几回合后才真的死、而且死时带着原始致死来源」
// 这三件事只有**真造怪物、真调钩子、真数回合**才算数。本探针就是这个。
//
// 另外钉三条容易被想当然的性质：
//   ① 两个钩子**不消耗随机数**：同一 seed 下，穿插钩子调用与不穿插，取出的随机序列必须逐位相同
//      （这是「关卡种子布局不漂移」的直接证据，不是靠读源码推的）。
//   ② Brute 那类「靠覆写 isAlive() 撑住狂暴」的战续**在狂暴期间**走不到致死闸门（用同型 stub 实证）；
//      而**战续耗尽**那一刻是 BruteRage 自己调 die(null) 的，所以另接了一个调用点 —— ⑥ 用**真 Brute/
//      ArmoredBrute 类**端到端跑一遍（含「关掉 GEBURA 时与原版逐字节一致」的反向断言）。
//   ③ Ghoul 的让路判据是**真对象、真判据**：孤身一人（真的会死，要豁免）与身边有同类
//      （转入倒地待复活，不能让路）两种情形各验一次。
//
// 编译/运行：见 skill egopd-source-verify §1；classpath 必须把 _chk/_javachk 排在最前
//（新编的 Trials / Mob / Ghoul / Brute / ArmoredBrute / 两个 buff 在那里），再 core/SPD-classes/services
// + gdx + gdx-controllers。
// 说明：脱离游戏没有 Gdx.files，故用 Unsafe.allocateInstance 造「只有 pit[] 与宽高的空关卡」，
// 让 Ghoul 的真判据能在不加载贴图的前提下跑起来。
import java.lang.reflect.Field;

import sun.misc.Unsafe;

import com.watabou.utils.Random;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Trials;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChesedMend;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GeburaGrace;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ArmoredBrute;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Brute;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Ghoul;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.levels.DeadEndLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;

public class ChesedGeburaProbe {

	static int failures = 0;
	static int checks = 0;
	static Unsafe unsafe;

	static void say( String s ){ System.out.println(s); }

	static void check( boolean ok, String label ){
		checks++;
		System.out.println( (ok ? "  [OK]   " : "  [FAIL] ") + label );
		if (!ok) failures++;
	}

	static void info( String s ){ System.out.println("  [INFO] " + s ); }

	/** 假怪物：只记录 die() 调用，不碰关卡/统计/精灵（探针里这些都不存在）。 */
	static class StubMob extends Mob {
		{
			spriteClass = null;
			HP = HT = 100;
			EXP = 5;
		}
		int dieCalls = 0;
		Object diedWith = null;
		@Override
		public void die( Object cause ){	//刻意不调 super：真跑会去碰关卡/统计
			dieCalls++;
			diedWith = cause;
		}
	}

	/** 同型 stub：模仿 Brute「狂暴护盾没耗尽就始终算活着」的做法（靠覆写 isAlive）。 */
	static class RageStubMob extends StubMob {
		boolean raging = false;
		@Override
		public boolean isAlive(){ return super.isAlive() || raging; }
	}

	/** 同型 stub：显式声明「本次致死后会转入自有战续」。 */
	static class DeferringStubMob extends StubMob {
		@Override
		public boolean deathIsDeferred( Object cause ){ return true; }
	}

	/**
	 * **真 Brute 类**（不是同型 stub）：只把 die() 接下来记录，其余（含 isAlive() 的狂暴逻辑、
	 * 真 BruteRage）全部走原版代码。脱离游戏没有精灵，所以真 triggerEnrage 里那两句 sprite 调用跑不了
	 * ⇒ 用反射直接把 hasRaged 置位（实战中战续耗尽时它本来就是 true）。
	 */
	static class ProbeBrute extends Brute {
		{ spriteClass = null; }
		int dieCalls = 0;
		Object diedWith = null;
		@Override
		public void die( Object cause ){	//刻意不调 super：真跑会去碰关卡/统计
			dieCalls++;
			diedWith = cause;
		}
	}

	/** 同前，但走 ArmoredBrute：它覆写了 triggerEnrage，且 ArmoredRage **覆写了 act()**（另一个调用点）。 */
	static class ProbeArmoredBrute extends ArmoredBrute {
		{ spriteClass = null; }
		int dieCalls = 0;
		Object diedWith = null;
		@Override
		public void die( Object cause ){
			dieCalls++;
			diedWith = cause;
		}
	}

	static Unsafe unsafe() throws Exception {
		if (unsafe == null){
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			unsafe = (Unsafe) f.get(null);
		}
		return unsafe;
	}

	static void setField( Object o, String name, Object value ) throws Exception {
		Class<?> c = o.getClass();
		Field f = null;
		while (c != null && f == null){
			try { f = c.getDeclaredField(name); }
			catch (NoSuchFieldException e){ c = c.getSuperclass(); }
		}
		if (f == null) throw new NoSuchFieldException(name);
		f.setAccessible(true);
		f.set(o, value);
	}

	public static void main( String[] args ) throws Exception {

		say("==============================================================");
		say("考验 CHESED（慈悲）· GEBURA（严厉）行为探针");
		say("==============================================================");

		int savedTrials = Dungeon.trials;
		Level savedLevel = Dungeon.level;
		Dungeon.trials = 0;

		//------------------------------------------------------------------
		say("");
		say("--- ① CHESED · 敌方生命上限 +25%（真跑 Mob.onAdd 这条链）---");

		Dungeon.trials = 0;
		Rat plain = new Rat();
		Actor.add( plain );
		check( plain.HT == 8, "关闭 CHESED：鼠出厂 HT 保持 8（实得 " + plain.HT + "）" );
		check( plain.buff( ChesedMend.class ) == null, "关闭 CHESED：不挂回血计时器" );

		Dungeon.trials = Trials.CHESED;
		Rat boosted = new Rat();
		Actor.add( boosted );
		check( boosted.HT == 10, "CHESED 开启：8 × 1.25 = 10（实得 " + boosted.HT + "）" );
		check( boosted.HP == boosted.HT, "以满血入场：HP=" + boosted.HP + " == HT=" + boosted.HT );
		check( boosted.buff( ChesedMend.class ) != null, "CHESED 开启：onAdd 挂上了回血计时器" );

		check( Trials.modifyMobHT( boosted, 40 ) == 50, "纯函数：40 → 50" );
		check( Trials.modifyMobHT( boosted, 10 ) == 13, "纯函数：10 → 13（12.5 四舍五入，不截断）" );
		check( Trials.modifyMobHT( boosted, 1 ) == 1, "纯函数：1 → 1（round(1.25)=1）" );

		boosted.alignment = Char.Alignment.ALLY;
		check( Trials.modifyMobHT( boosted, 40 ) == 40, "非敌方（ALLY）不加成：40 保持" );
		boosted.alignment = Char.Alignment.ENEMY;

		Dungeon.trials = 0;
		check( Trials.modifyMobHT( boosted, 40 ) == 40, "关闭 CHESED 时空操作" );

		//------------------------------------------------------------------
		say("");
		say("--- ② CHESED · 每 5 回合回 10% 生命上限，且只在不满血时计时 ---");

		Dungeon.trials = Trials.CHESED;

		StubMob mend = new StubMob();		// 出厂 HT = 100
		mend.HT = 100;
		mend.HP = 100;
		ChesedMend m1 = Buff.affect( mend, ChesedMend.class );
		check( m1 != null, "回血计时器能挂到怪物身上" );
		for (int i = 0; i < 6; i++) m1.act();
		check( mend.HP == 100, "满血时不计时：6 个回合过去 HP 仍是 100（实得 " + mend.HP + "）" );

		mend.HP = 40;
		for (int i = 0; i < 4; i++) m1.act();
		check( mend.HP == 40, "不满血之后第 4 回合仍未回血（实得 " + mend.HP + "）" );
		m1.act();
		check( mend.HP == 50, "第 5 回合回 10%（100×10%）：40 → 50（实得 " + mend.HP + "）" );
		for (int i = 0; i < 4; i++) m1.act();
		check( mend.HP == 50, "回完血重新数 5 回合（再数到第 4 回合仍 50）" );
		m1.act();
		check( mend.HP == 60, "第 10 回合再回一次：50 → 60（实得 " + mend.HP + "）" );

		// 计时被「满血」打断后必须重新数满
		mend.HP = 45;
		m1.act(); m1.act(); m1.act();		// 数到 3
		mend.HP = 100;						// 第 4 回合时已满血
		m1.act();							// 这一步发现满血 ⇒ 计时归零
		mend.HP = 90;
		for (int i = 0; i < 4; i++) m1.act();
		check( mend.HP == 90, "满血会打断计时：重新数到第 4 回合仍 90（实得 " + mend.HP + "）" );
		m1.act();
		check( mend.HP == 100, "打断后必须重新数满 5 回合才回血：90 + 10 = 100（实得 " + mend.HP + "）" );

		// 回血量按 HT 现算，不写死数值
		StubMob mend2 = new StubMob();
		mend2.HT = 33;
		mend2.HP = 1;
		ChesedMend m2 = Buff.affect( mend2, ChesedMend.class );
		for (int i = 0; i < 5; i++) m2.act();
		check( mend2.HP == 1 + Math.round(33 * 0.10f),
				"HT=33 时回 round(3.3)=3 点（实得 " + (mend2.HP - 1) + "）" );

		// 回血不超过缺失血量
		StubMob mend3 = new StubMob();
		mend3.HT = 100;
		mend3.HP = 98;
		ChesedMend m3 = Buff.affect( mend3, ChesedMend.class );
		for (int i = 0; i < 5; i++) m3.act();
		check( mend3.HP == 100, "回血不超过缺失血量（98 + 2 = 100，实得 " + mend3.HP + "）" );

		// 门控
		Dungeon.trials = 0;
		StubMob mend4 = new StubMob();
		mend4.HT = 100;
		mend4.HP = 10;
		ChesedMend m4 = Buff.affect( mend4, ChesedMend.class );
		for (int i = 0; i < 6; i++) m4.act();
		check( mend4.HP == 10, "关闭 CHESED：不回血（实得 " + mend4.HP + "）" );

		Dungeon.trials = Trials.CHESED;
		StubMob mend5 = new StubMob();
		mend5.HT = 100;
		mend5.HP = 10;
		mend5.alignment = Char.Alignment.ALLY;
		ChesedMend m5 = Buff.affect( mend5, ChesedMend.class );
		for (int i = 0; i < 6; i++) m5.act();
		check( mend5.HP == 10, "友方/非敌方单位不回血（实得 " + mend5.HP + "）" );

		//------------------------------------------------------------------
		say("");
		say("--- ③ GEBURA · 致死闸门：无敌、不倒、血停在 0 ---");

		Dungeon.trials = Trials.GEBURA;

		StubMob gm = new StubMob();		// EXP = 5
		Actor.add( gm );
		gm.HP = 0;						// 模拟「伤害已经打进血量、HP 已归零」
		check( !gm.isAlive(), "介入之前：HP=0 ⇒ isAlive() 为假（闸门确实问得出来）" );
		Trials.interceptLethalDamage( gm, "the-killing-blow" );
		check( gm.geburaUsed, "豁免门闩已置位（一次性）" );
		check( gm.buff( GeburaGrace.class ) != null, "挂上了濒死无敌 buff" );
		check( gm.isAlive(), "无敌期间 isAlive() 为真 ⇒ Char.damage 不会紧接着补一次 die()" );
		check( gm.HP == 0, "血量按设计停在 0（空血条）" );
		check( gm.isActive(), "isActive() 同步为真（回合调度与各类技能判定都要它）" );
		check( gm.isInvulnerable( Char.class ), "无敌期间 isInvulnerable() 为真 ⇒ 伤害被原版无敌分支吃掉" );
		check( gm.isInvulnerable( com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.class ),
				"无敌与伤害来源无关：任何 src 类别都免疫" );

		//------------------------------------------------------------------
		say("");
		say("--- ④ GEBURA · 数满 EXP 个回合才真正倒下，且带着原始致死来源 ---");

		GeburaGrace grace = gm.buff( GeburaGrace.class );
		for (int i = 0; i < 4; i++) grace.act();
		check( gm.dieCalls == 0, "EXP=5：前 4 个回合仍不倒（实得 die 调用 " + gm.dieCalls + " 次）" );
		check( gm.isAlive(), "第 4 回合仍算活着" );
		grace.act();
		check( gm.dieCalls == 1, "第 5 回合（=EXP）真正倒下一次（实得 " + gm.dieCalls + " 次）" );
		check( "the-killing-blow".equals( gm.diedWith ),
				"倒下时用的是**原始致死来源**（击杀归属/经验/掉落口径与原版一致），实得 " + gm.diedWith );
		check( !gm.geburaGrace, "倒下后无敌缓存已清（不会留成幽灵状态）" );
		check( gm.buff( GeburaGrace.class ) == null, "倒下后 buff 已摘" );

		Trials.interceptLethalDamage( gm, "second-blow" );
		check( gm.buff( GeburaGrace.class ) == null && gm.dieCalls == 1,
				"豁免是一次性的：第二次致死不再救" );

		// 回合数 = 该单位的固有 EXP（换一只 EXP=7 的实证）
		StubMob g7 = new StubMob();
		g7.EXP = 7;
		g7.HP = 0;
		Trials.interceptLethalDamage( g7, "x" );
		GeburaGrace gr7 = g7.buff( GeburaGrace.class );
		check( gr7 != null, "EXP=7 ⇒ 拿到无敌" );
		for (int i = 0; i < 6; i++) gr7.act();
		check( g7.dieCalls == 0, "EXP=7：第 6 回合仍未倒下" );
		gr7.act();
		check( g7.dieCalls == 1, "EXP=7：第 7 回合倒下（回合数随 EXP 变）" );

		// 跨存档：回合数与「来源类别」要能存下来
		StubMob gs = new StubMob();
		gs.EXP = 3;
		gs.HP = 0;
		Trials.interceptLethalDamage( gs, Dungeon.trials == 0 ? "x" : "y" );
		GeburaGrace saved = gs.buff( GeburaGrace.class );
		com.watabou.utils.Bundle bundle = new com.watabou.utils.Bundle();
		saved.storeInBundle( bundle );
		GeburaGrace reloaded = new GeburaGrace();
		reloaded.restoreFromBundle( bundle );
		check( reloaded.attachTo( gs ), "读档后的无敌 buff 能重新挂上（缓存随之重建）" );
		check( gs.geburaGrace, "重新挂上后 Mob.geburaGrace 缓存为真" );
		for (int i = 0; i < 2; i++) reloaded.act();
		check( gs.buff( GeburaGrace.class ) != null, "读档后前 2 回合仍不倒（剩余回合数确实存下来了）" );

		//------------------------------------------------------------------
		say("");
		say("--- ⑤ GEBURA · 门控与「战续让路」---");

		Dungeon.trials = 0;
		StubMob off = new StubMob();
		off.HP = 0;
		Trials.interceptLethalDamage( off, "x" );
		check( off.buff( GeburaGrace.class ) == null && !off.isAlive(), "关闭 GEBURA：完全不介入" );

		Dungeon.trials = Trials.GEBURA;
		StubMob ally = new StubMob();
		ally.alignment = Char.Alignment.ALLY;
		ally.HP = 0;
		Trials.interceptLethalDamage( ally, "x" );
		check( ally.buff( GeburaGrace.class ) == null, "英雄盟友 / 中立单位不享受（只认 ENEMY）" );

		StubMob noExp = new StubMob();
		noExp.EXP = 0;
		noExp.HP = 0;
		Trials.interceptLethalDamage( noExp, "x" );
		check( noExp.buff( GeburaGrace.class ) == null && !noExp.isAlive(),
				"EXP=0 ⇒ 0 回合无敌 = 不豁免，正常死亡" );

		// 「靠 isAlive() 撑住」的战续（Brute 同型）：闸门根本问不到它
		RageStubMob rage = new RageStubMob();
		rage.raging = true;
		rage.HP = 0;
		check( rage.isAlive(), "狂暴中的同型 stub：HP=0 但 isAlive() 为真（与 Brute 同构）" );
		Trials.interceptLethalDamage( rage, "x" );
		check( rage.buff( GeburaGrace.class ) == null,
				"Brute 同型战续不经过致死闸门 ⇒ GEBURA 不介入，狂暴流程与护盾时长不受影响" );

		// 「自带让路声明」的战续（Ghoul 同型）
		DeferringStubMob def = new DeferringStubMob();
		def.HP = 0;
		Trials.interceptLethalDamage( def, "x" );
		check( def.buff( GeburaGrace.class ) == null, "声明了 deathIsDeferred 的单位：GEBURA 让路" );

		// 真 Ghoul 的真判据：需要关卡，但只用到 pit[] 与宽高
		Level fake = (Level) unsafe().allocateInstance( DeadEndLevel.class );
		setField( fake, "width", 8 );
		setField( fake, "height", 8 );
		setField( fake, "length", 64 );
		fake.pit = new boolean[64];
		Dungeon.level = fake;

		Ghoul lone = new Ghoul();
		lone.pos = 0;
		lone.fieldOfView = new boolean[64];
		Actor.add( lone );
		check( !lone.deathIsDeferred( "x" ), "孤身一人的尸群：deathIsDeferred = false（这次是真死）" );
		lone.HP = 0;
		Trials.interceptLethalDamage( lone, "x" );
		check( lone.buff( GeburaGrace.class ) != null, "孤身尸群：GEBURA 正常豁免（EXP=5 ⇒ 5 回合）" );

		Ghoul host = new Ghoul();
		host.pos = 1;
		host.fieldOfView = new boolean[64];
		host.fieldOfView[32] = true;		// 只让 searchForHost 命中，不去碰关卡其余部分
		Actor.add( host );

		Ghoul linked = new Ghoul();
		linked.pos = 32;					// 与 lone 距离 4：不受 lone 的「距离<4」兜底影响
		check( linked.deathIsDeferred( "x" ), "身边有同类：deathIsDeferred = true（会转入倒地待复活）" );
		linked.HP = 0;
		Trials.interceptLethalDamage( linked, "x" );
		check( linked.buff( GeburaGrace.class ) == null,
				"尸群让路：原版倒地待复活完整保留，GEBURA 一行都不插手" );

		Dungeon.level = savedLevel;

		//------------------------------------------------------------------
		say("");
		say("--- ⑥ GEBURA · 豺狼暴徒/装甲暴徒：战续（狂暴护盾）结束后仍能吃无敌 ---");

		Dungeon.trials = Trials.GEBURA;

		// 真 Brute 出场。先让它处于「狂暴中、护盾还厚」的状态。
		ProbeBrute b0 = new ProbeBrute();
		setField( b0, "hasRaged", true );		// 实战中战续耗尽时它本就是 true；这里手动置位以避开精灵调用
		b0.HP = 0;
		Brute.BruteRage r0 = Buff.affect( b0, Brute.BruteRage.class );
		r0.setShield( 50 );
		check( b0.isAlive(), "狂暴中：HP=0 但 isAlive() 为真（真 Brute 类跑真 isAlive()，不是 stub）" );
		check( !Trials.interceptLethalDamage( b0, "hit-during-rage" ),
				"狂暴期间闸门返回 false（不接管）⇒ 护盾时长与狂暴流程分毫不动" );
		check( !b0.geburaUsed, "狂暴期间不消耗那唯一一次豁免 ⇒ 豁免留到战续结束后才用" );

		// 护盾耗尽 —— 这就是「战续额外血量结束」的那一刻
		r0.decShield( 50 );
		r0.act();
		check( b0.buff( Brute.BruteRage.class ) == null,
				"护盾耗尽时 BruteRage 已自行摘除（detachesAtZero）⇒ 无敌期间不会一边无敌一边回盾" );
		check( b0.dieCalls == 0, "战续结束被 GEBURA 接管：没有立即 die(null)（实得 " + b0.dieCalls + " 次）" );
		check( b0.geburaUsed, "豁免门闩此刻才置位（一次性）" );
		check( b0.buff( GeburaGrace.class ) != null, "战续结束后挂上了濒死无敌" );
		check( b0.geburaGrace && b0.isAlive(), "血停在 0 也不倒：isAlive() 为真" );
		check( b0.HP == 0, "血量停在 0（空血条），实得 " + b0.HP );
		check( b0.isInvulnerable( Char.class ), "无敌期间照常免疫伤害" );

		GeburaGrace gb = b0.buff( GeburaGrace.class );
		for (int i = 0; i < 7; i++) gb.act();
		check( b0.dieCalls == 0, "EXP=8：前 7 个回合仍不倒（实得 die 调用 " + b0.dieCalls + " 次）" );
		gb.act();
		check( b0.dieCalls == 1, "第 8 回合（=EXP）真正倒下" );
		check( b0.diedWith == null,
				"倒下时沿用原版战续的致死来源口径（null），不臆造击杀归属（实得 " + b0.diedWith + "）" );
		check( !b0.geburaGrace, "倒下后无敌缓存已清（不会留成幽灵状态）" );

		// 关闭 GEBURA：这条路径必须与原版完全一致
		Dungeon.trials = 0;
		ProbeBrute b1 = new ProbeBrute();
		setField( b1, "hasRaged", true );
		b1.HP = 0;
		Brute.BruteRage r1 = Buff.affect( b1, Brute.BruteRage.class );
		r1.setShield( 4 );
		r1.act();
		check( b1.dieCalls == 1 && b1.diedWith == null && !b1.geburaGrace,
				"关闭 GEBURA：战续耗尽照原版立即 die(null)、无无敌（原版保真）" );

		// 装甲暴徒覆写了 act() ⇒ 是**另一个**调用点，必须单独验（它同属豺狼暴徒一系）
		Dungeon.trials = Trials.GEBURA;
		ProbeArmoredBrute ab = new ProbeArmoredBrute();
		setField( ab, "hasRaged", true );
		ab.HP = 0;
		ArmoredBrute.ArmoredRage ar = Buff.affect( ab, ArmoredBrute.ArmoredRage.class );
		ar.setShield( 1 );
		ar.act();
		check( ab.dieCalls == 0 && ab.buff( GeburaGrace.class ) != null,
				"装甲暴徒（覆写了 act()、调用点独立）：战续结束同样被 GEBURA 接管" );
		check( ab.buff( ArmoredBrute.ArmoredRage.class ) == null && ab.isAlive(),
				"装甲暴徒：护盾已摘、血停在 0 也不倒" );

		// 矮人尸群「保持不触发」的口径不变，真 Ghoul 的真判据已在 ⑤ 验过
		//（孤身一人 ⇒ 豁免；身旁有同类 ⇒ 转入倒地待复活、GEBURA 让路）。

		//------------------------------------------------------------------
		say("");
		say("--- ⑦ 两个钩子都不消耗随机数 ⇒ 关卡种子布局不漂移 ---");

		final int N = 200;
		long[] seqPlain = new long[N];
		long[] seqHooked = new long[N];

		Dungeon.trials = 0;
		Random.pushGenerator( 0xC5ED6EB4L );	// 固定 seed：只要求两次相同，值本身无意义
		for (int i = 0; i < N; i++) seqPlain[i] = Random.Long();
		Random.popGenerator();

		Dungeon.trials = Trials.CHESED | Trials.GEBURA;
		Random.pushGenerator( 0xC5ED6EB4L );	// 固定 seed：只要求两次相同，值本身无意义
		for (int i = 0; i < N; i++){
			StubMob rnd = new StubMob();
			Trials.modifyMobHT( rnd, 40 );					// 会改写 HT 的分支
			rnd.HP = 0;
			Trials.interceptLethalDamage( rnd, "x" );		// 会挂 buff 的分支
			GeburaGrace gg = rnd.buff( GeburaGrace.class );
			if (gg != null) gg.act();						// 会真的走一次回合
			StubMob never = new StubMob();
			never.EXP = 0;
			never.HP = 0;
			Trials.interceptLethalDamage( never, "x" );		// 不介入的分支
			seqHooked[i] = Random.Long();
		}
		Random.popGenerator();

		int diff = -1;
		for (int i = 0; i < N; i++){
			if (seqPlain[i] != seqHooked[i]){ diff = i; break; }
		}
		check( diff == -1, "同 seed 下 " + N + " 个随机数逐位相同"
				+ (diff == -1 ? "" : "（第 " + diff + " 个开始不同）") );

		//------------------------------------------------------------------
		say("");
		say("--- ⑧ GEBURA · 锁血那一击的僵直（2026-09-24 削弱）：刚被打成濒死的那一回合不行动 ---");
		//「本回合不行动」这个动作本身发生在 Mob.act() 里 —— 本探针跑不动它
		//（Mob.act() 一开始就是 super.act() = Char.act()，要真关卡与精灵），
		//所以这里钉的是**一次性语义**那一半：谁置位、问几次、会不会自己复活、与豁免是否两件事。
		//「消费点确实在 Mob.act() 最前面（paralysed 之后、chooseEnemy/state.act 之前）」
		//由 verify_chesed_gebura.py 的源码层「比位置」+ 字节码层「偏移比较」负责。

		Dungeon.trials = Trials.GEBURA;
		StubMob stag = new StubMob();		// EXP = 5
		check( !stag.geburaStagger, "出厂不带僵直标记（默认不影响任何单位）" );
		check( !stag.consumeGeburaStagger(), "没标记时问「本回合要不要僵直」= false（不消费）" );

		stag.HP = 0;
		Trials.interceptLethalDamage( stag, "x" );
		check( stag.geburaStagger, "锁血发下的同时也置上了僵直标记" );
		check( stag.buff( GeburaGrace.class ) != null && stag.isAlive(),
				"僵直与豁免是**两件事**：豁免照旧（挂着 GeburaGrace、血停在 0 也不倒）" );
		check( stag.consumeGeburaStagger(), "第一次问 = true ⇒ 这一回合空过，它不会行动" );
		check( !stag.geburaStagger, "问过即清 ⇒ 标记已被消费" );
		check( !stag.consumeGeburaStagger() && !stag.consumeGeburaStagger(),
				"之后再问都是 false（一次性：不会变成永久眩晕）" );
		check( stag.buff( GeburaGrace.class ) != null && stag.isAlive(),
				"空过一个回合之后无敌仍在：豁免的计时照常走，僵直不会延长它" );

		Dungeon.trials = 0;
		StubMob stagOff = new StubMob();
		stagOff.HP = 0;
		Trials.interceptLethalDamage( stagOff, "x" );
		check( !stagOff.geburaStagger, "关闭 GEBURA：完全不介入 ⇒ 不置僵直" );

		Dungeon.trials = Trials.GEBURA;
		StubMob stagAlly = new StubMob();
		stagAlly.alignment = Char.Alignment.ALLY;
		stagAlly.HP = 0;
		Trials.interceptLethalDamage( stagAlly, "x" );
		check( !stagAlly.geburaStagger, "英雄盟友不吃僵直（只认 ENEMY）" );

		// 战续结束那条路径（BruteRage 里的第二个调用点）走的是同一个闸门 ⇒ 同样会僵直
		ProbeBrute stagBrute = new ProbeBrute();
		setField( stagBrute, "hasRaged", true );
		stagBrute.HP = 0;
		Brute.BruteRage stagRage = Buff.affect( stagBrute, Brute.BruteRage.class );
		stagRage.setShield( 1 );
		stagRage.act();
		check( stagBrute.geburaStagger && stagBrute.buff( GeburaGrace.class ) != null,
				"战续耗尽被接管时同样置上僵直（同一闸门、两个调用点，行为一致）" );
		check( stagBrute.consumeGeburaStagger() && !stagBrute.consumeGeburaStagger(),
				"战续路径的僵直也是一次性" );

		//------------------------------------------------------------------
		say("");
		say("==============================================================");
		say("断言总数 = " + checks + "，失败 = " + failures);
		say("==============================================================");

		Dungeon.trials = savedTrials;
		System.exit( failures == 0 ? 0 : 1 );
	}
}
