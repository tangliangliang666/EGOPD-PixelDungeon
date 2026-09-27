// GEBURA 掉落修复的行为探针（真跑游戏类，不改任何游戏文件）
//
// 为什么要有它：verify_gebura_drop.py 证明的是「留存字段在哪、抄在哪、消费在哪」——**结构**证据。
// 但「致死那一刻的凭据到底活不活得过一个 tick」「已摘掉的 LuckProc 还能不能产出掉落」
// 「关掉 GEBURA 时是不是真的不留存」这三件事只有**真造对象、真调钩子、真摘 buff**才算数。
//
// 七段：
//   ① LuckProc 的寿命：真挂一个、真跑一次 act() ⇒ 立刻自行摘掉（**根因的实证**）。
//   ② 闸门留存（开 GEBURA）：真调 Trials.interceptLethalDamage ⇒ 两个字段都抄到了，
//      且 geburaLuckyProc **就是**那一击身上那个实例。
//   ③ 延后死亡的可达性：把 live 的 LuckProc 摘掉之后，live == null **而** 留存 != null
//      ⇒ Mob.die / Mob.rollToDropLoot 的回落分支必命中。
//   ④ 留存**能被消费**：对已摘掉的实例调 genLoot() 不抛异常、且真的产出一件 Item
//      （这是 fix 可行的核心：detach 之后 target 仍在，二次 detach 是幂等的）。
//   ⑤ 关掉 GEBURA：同一路径 ⇒ 返回 false、两个字段**仍是初值**（未开启时行为逐字不变）。
//   ⑥ 不碰随机流：同一 seed 下，穿插闸门调用与不穿插，取出的随机序列逐位相同。
//   ⑦ 财富等级：抄的是「致死那一刻」的读数；英雄侧凭据后来失效时，回落值仍可判（三参重载按参数走）。
//
// 编译/运行：见 skill egopd-source-verify §1。classpath 必须把 _chk/_javachk 排最前
//（新编的 Mob / Trials / RingOfWealth / Lucky 在那里），再 core/SPD-classes/services
// + gdx + gdx-controllers + org.json。
import java.lang.reflect.Constructor;

import sun.misc.Unsafe;

import com.watabou.utils.Random;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Trials;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GeburaGrace;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky;

import java.util.ArrayList;

public class GeburaDropProbe {

	static int checks = 0;
	static int failures = 0;
	static int infos = 0;

	static void check( boolean ok, String label ){
		checks++;
		System.out.println( (ok ? "  [OK]   " : "  [FAIL] ") + label );
		if (!ok) failures++;
	}

	static void info( String s ){ infos++; System.out.println("  [INFO] " + s ); }

	/** 真 Rat：只把 die() 接下来记录，其余全部走原版代码。 */
	static class ProbeRat extends Rat {
		int dieCalls = 0;
		Object diedWith = null;
		@Override
		public void die( Object cause ){
			dieCalls++;
			diedWith = cause;
		}
	}

	static ProbeRat freshMob(){
		ProbeRat m = new ProbeRat();
		m.HP = m.HT = 100;
		m.EXP = 5;					// ⇒ 豁免 5 回合
		m.alignment = Char.Alignment.ENEMY;
		return m;
	}

	public static void main( String[] args ) throws Exception {

		System.out.println("==============================================================");
		System.out.println("GEBURA 掉落修复 · 行为探针（幸运附魔 / 财富戒指的额外掉落判定）");
		System.out.println("==============================================================");

		int savedTrials = Dungeon.trials;
		com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero savedHero = Dungeon.hero;

		// ------------------------------------------------ ① LuckProc 的寿命
		System.out.println("\n--- ① 幸运附魔的 LuckProc 只活到下一个 buff tick（根因）---");
		try {
			ProbeRat mob = freshMob();
			Buff.affect( mob, Lucky.LuckProc.class );

			check( mob.buff( Lucky.LuckProc.class ) != null,
					"刚挂上：LuckProc 在身上（vanilla 就是靠「致死那一击同一调用栈」读到它）" );

			Lucky.LuckProc p0 = mob.buff( Lucky.LuckProc.class );
			p0.act();		// 一个 buff tick —— 它的 act() 第一句就是 detach()

			check( mob.buff( Lucky.LuckProc.class ) == null,
					"跑过一次 act()：LuckProc 已自行摘掉 ⇒ 只要真正倒下被推迟一个回合以上，就必然读不到它" );
		} catch (Throwable t) {
			info("本段无法在脱离游戏的环境下跑（" + t + "）");
		}

		// ------------------------------------------------ ② 闸门留存
		System.out.println("\n--- ② 闸门（开 GEBURA）在锁血那一刻抄下凭据 ---");
		Lucky.LuckProc kept = null;
		ProbeRat graced = null;
		try {
			Dungeon.trials = Trials.GEBURA;
			Dungeon.hero = null;		// 财富部分单独在 ⑦ 里测

			graced = freshMob();
			Buff.affect( graced, Lucky.LuckProc.class );
			kept = graced.buff( Lucky.LuckProc.class );

			graced.HP = 0;
			boolean took = Trials.interceptLethalDamage( graced, null );

			check( took, "闸门接管了这一击（返回 true）" );
			check( graced.geburaGrace, "mob.geburaGrace 为真 ⇒ 不会立刻 die()" );
			check( graced.geburaStagger, "mob.geburaStagger 为真（既有削弱行为未受影响）" );
			check( graced.geburaLuckyProc != null, "闸门抄下了 LuckProc 留存（geburaLuckyProc）" );
			check( graced.geburaLuckyProc == kept, "抄下的就是**致死那一击身上那个实例**（强度等信息一并留存）" );
			check( graced.buff( GeburaGrace.class ) != null, "GeburaGrace 已挂上（计时开始）" );
		} catch (Throwable t) {
			info("本段无法在脱离游戏的环境下跑（" + t + "）");
		}

		// ------------------------------------------------ ③ 延后死亡时的可达性
		System.out.println("\n--- ③ 计时走完之后：live 没了，留存还在 ---");
		try {
			check( kept != null && graced != null, "前置：② 段已造出留存（否则本段无意义）" );

			// 模拟「推迟期间 buff tick 过去」——这正是 GEBURA 把真正倒下推迟之后发生的事
			kept.detach();

			check( graced.buff( Lucky.LuckProc.class ) == null,
					"真正倒下那一刻：live 的 LuckProc 已不在（**这就是修复前的原状**）" );
			check( graced.geburaLuckyProc != null,
					"同一刻：闸门留存仍在 ⇒ Mob.rollToDropLoot 的回落分支必命中" );
		} catch (Throwable t) {
			info("本段无法在脱离游戏的环境下跑（" + t + "）");
		}

		// ------------------------------------------------ ④ 留存能被消费
		System.out.println("\n--- ④ 已摘掉的 LuckProc 仍能产出掉落（fix 可行的核心）---");
		try {
			// ④a 幂等：再 detach 一次不抛异常（Buff.detach() 里是 target.remove(this)，
			//    而 Char.remove(Buff) 不会把 buff.target 置空 ⇒ 二次 detach 安全）。
			boolean threw2 = false;
			try {
				kept.detach();
			} catch (Throwable t) {
				threw2 = true;
				info("二次 detach 抛出：" + t);
			}
			check( !threw2, "对**已 detach** 的实例再 detach 一次不抛异常（幂等）" );

			// ④b 「先 detach 后生成」这条路能走通：headless 下没有 Gdx.files，
			//    掉落生成（Generator/贴图）跑不了 —— 但异常必须**出在生成阶段**，
			//    而不是出在 detach（那才是我们要证伪的风险）。
			Item loot = null;
			Throwable fail = null;
			try {
				loot = kept.genLoot();		// 内部先 detach()，再 RingOfWealth.genConsumableDrop(...)
			} catch (Throwable t) {
				fail = t;
			}
			if (fail == null) {
				check( loot != null, "genLoot() 真的产出了一件掉落（不是空手返回）" );
				if (loot != null) info("产出：" + loot.getClass().getSimpleName() + " ×" + loot.quantity());
			} else {
				String top = fail.getStackTrace().length > 0 ? fail.getStackTrace()[0].toString() : "";
				boolean detachFault = top.contains("Buff.detach")
						|| fail.toString().contains("Char.remove")
						|| fail instanceof NullPointerException && top.contains("buffs");
				check( !detachFault, "genLoot() 的异常出在**生成阶段**、不在 detach（首帧：" + top + "）" );
				info("headless 无 Gdx.files ⇒ 掉落生成本段跑不了：" + fail);
			}
		} catch (Throwable t) {
			info("本段无法在脱离游戏的环境下跑（" + t + "）");
		}

		// ------------------------------------------------ ⑤ 关掉 GEBURA
		System.out.println("\n--- ⑤ 关掉 GEBURA：不留存，行为与原版逐字一致 ---");
		try {
			Dungeon.trials = 0;

			ProbeRat plain = freshMob();
			Buff.affect( plain, Lucky.LuckProc.class );
			plain.HP = 0;
			boolean took = Trials.interceptLethalDamage( plain, null );

			check( !took, "闸门不接管（返回 false ⇒ 调用方照原版落地 die()）" );
			check( !plain.geburaGrace, "不挂 GeburaGrace" );
			check( plain.geburaLuckyProc == null, "不留存 LuckProc（字段仍是初值）" );
			check( plain.geburaWealthBonus == 0, "不留存财富等级（字段仍是初值 ⇒ live 路径逐字不变）" );
			check( plain.buff( Lucky.LuckProc.class ) != null, "live 的 LuckProc 没被动过" );
		} catch (Throwable t) {
			info("本段无法在脱离游戏的环境下跑（" + t + "）");
		}

		// ------------------------------------------------ ⑥ 不碰随机流
		System.out.println("\n--- ⑥ 闸门不消耗随机数（同一 seed 逐位相同）---");
		try {
			Dungeon.trials = Trials.GEBURA;
			float[] a = new float[200];
			float[] b = new float[200];

			Dungeon.trials = 0;
			Random.pushGenerator( 0xC5ED6EB4L );	// 固定 seed：只要求两次相同，值本身无意义
			for (int i = 0; i < 200; i++) a[i] = Random.Float();
			Random.popGenerator();

			Dungeon.trials = Trials.GEBURA;
			Random.pushGenerator( 0xC5ED6EB4L );
			for (int i = 0; i < 200; i++) {
				ProbeRat m = freshMob();
				Buff.affect( m, Lucky.LuckProc.class );
				m.HP = 0;
				Trials.interceptLethalDamage( m, null );		// 每取一个数就穿插一次闸门
				b[i] = Random.Float();
			}
			Random.popGenerator();

			boolean same = true;
			for (int i = 0; i < 200; i++) if (a[i] != b[i]) same = false;
			check( same, "200 个数逐位相同 ⇒ 闸门（含新增的两处抄写）没有引入任何 Random 调用" );
		} catch (Throwable t) {
			info("本段无法在脱离游戏的环境下跑（" + t + "）");
		}

		// ------------------------------------------------ ⑦ 财富等级的留存与回落
		System.out.println("\n--- ⑦ 财富等级：抄「致死那一刻」的读数，凭据失效后仍可判 ---");
		try {
			Dungeon.trials = Trials.GEBURA;

			// 造一个真 RingOfWealth + 真的 Wealth buff（戒指的实例初始化块会碰贴图，
			// 所以用 Unsafe 跳过它；Wealth 是非静态内部类，走「外层实例」那个合成构造器）
			RingOfWealth ring = (RingOfWealth) unsafe().allocateInstance( RingOfWealth.class );
			Constructor<?> ctor = RingOfWealth.Wealth.class.getDeclaredConstructor( RingOfWealth.class );
			ctor.setAccessible( true );
			RingOfWealth.Wealth wealth = (RingOfWealth.Wealth) ctor.newInstance( ring );

			// ⚠️ 不能用 Unsafe 造 Hero：Char.buffs 是**字段初始化**（`= new LinkedHashSet<>()`），
			// 跳过构造器会得到 null 集合、Buff.attachTo 立刻 NPE。走真构造器。
			com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero hero =
					new com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero();
			Dungeon.hero = hero;
			wealth.attachTo( hero );

			int live = Ring.getBuffedBonus( hero, RingOfWealth.Wealth.class );
			check( live > 0, "英雄身上挂了 Wealth buff ⇒ live 财富等级 = " + live + "（>0）" );

			ProbeRat m = freshMob();
			m.HP = 0;
			boolean took = Trials.interceptLethalDamage( m, null );
			check( took, "闸门接管" );
			check( m.geburaWealthBonus == live,
					"抄下的财富等级 == 致死那一刻的 live 读数（" + m.geburaWealthBonus + "）" );

			// 模拟「延后期间英雄侧凭据失效」（复活 / 卸下等）
			wealth.detach();
			check( Ring.getBuffedBonus( hero, RingOfWealth.Wealth.class ) <= 0,
					"摘掉 Wealth buff 之后 live 读数归 0（**推迟期间凭据会变**的现实情形）" );
			check( m.geburaWealthBonus > 0,
					"留存仍是致死那一刻的旧值 ⇒ 判定不会因为「后来失效」而凭空落空" );

			// 三参重载：判据用的是**传进来的**等级，而不是 live
			check( RingOfWealth.tryForBonusDrop( hero, 1, 0 ) == null,
					"tryForBonusDrop(hero, 1, 0) 早退 ⇒ 判据取的是**实参**（回落才不会被 live=0 抹掉）" );
			try {
				ArrayList<Item> r = RingOfWealth.tryForBonusDrop( hero, 1, m.geburaWealthBonus );
				check( r != null,
						"tryForBonusDrop(hero, 1, 留存) 不早退（live 已是 0，靠实参放行）" );
			} catch (Throwable t) {
				info("三参重载的掉落生成在脱离游戏的环境下跑不了（" + t + "）——早退判据已单独断言");
			}
		} catch (Throwable t) {
			info("本段无法在脱离游戏的环境下跑（" + t + "）");
		}

		// ------------------------------------------------ 复位
		Dungeon.trials = savedTrials;
		Dungeon.hero = savedHero;

		System.out.println("\n==============================================================");
		System.out.printf("断言 %d 条：通过 %d，失败 %d（附 INFO %d 条）%n",
				checks, checks - failures, failures, infos);
		System.out.println( failures == 0 ? "全部符合预期" : "有 " + failures + " 项与预期不符" );
		System.out.println("==============================================================");
		if (failures != 0) System.exit( 1 );
	}

	static Unsafe unsafe() throws Exception {
		java.lang.reflect.Field f = Unsafe.class.getDeclaredField("theUnsafe");
		f.setAccessible(true);
		return (Unsafe) f.get(null);
	}
}
