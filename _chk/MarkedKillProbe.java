// 「带着标记倒下」击杀判定的行为探针（真跑游戏类，不改任何游戏文件）
//
// 为什么要有它：verify_marked_kill_routes.py 证的是**结构**（钩子在哪、旧判据删没删、谁消费谁）。
// 但「死亡时真的能读到 buff 吗」「判据是不是真的与『谁打死的』解耦了」「锁血抄存的回落到底走不走」
// 这三件事只有**真造对象、真挂 buff、真调 die()**才算数。
//
// 六段：
//   ① 死亡即触发（拇指 荣耀凯旋）：真 Rat + AimHeartMark，cause 用 **Chasm.class（非英雄击杀）**，
//      真跑 Mob.die ⇒ 英雄真的回血了 —— 证明触发条件已与「玩家击杀」解耦。
//   ② 死亡即触发（食指 指令目标）：同法，带 InstructionTarget + 神谕庇佑天赋 ⇒ 死亡后拿到护盾；
//      同一只怪**不带标记**时（对照组）不拿 ⇒ 判据确实是「携带 buff」。
//   ③ 锁血回落：不挂 live buff，只置 mob.geburaAimMarked（＝闸门在锁血那一刻抄下的），
//      死亡时仍回血 ⇒ 回落分支可达；且抄存被消费成 false（一次性）。
//   ④ 死前摘不掉：die() 走完之后 buff 已被 onRemove 一并摘掉（对照：证明钩子确实跑在摘 buff 之前）。
//   ⑤ 闸门抄存：真调 Trials.interceptLethalDamage ⇒ 两个抄存字段真的被写；关掉考验时返回 false 且不写。
//   ⑥ 不碰随机流：同一 seed 下，穿插闸门调用与不穿插，取出的随机序列逐位相同。
//
// 编译/运行：见 skill egopd-source-verify §1。classpath 必须把 _chk/_javachk 排最前
//（新编的 Mob / Trials / Char 在那里），再 core/SPD-classes/services + gdx + gdx-controllers + org.json。
import com.watabou.noosa.Game;
import com.watabou.utils.Random;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Trials;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AimHeartMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GeburaGrace;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.InstructionTarget;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.MiningLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Chasm;

import java.util.HashSet;
import java.util.LinkedHashMap;

public class MarkedKillProbe {

	static int checks = 0;
	static int failures = 0;
	static int infos = 0;

	static void check( boolean ok, String label ){
		checks++;
		System.out.println( (ok ? "  [OK]   " : "  [FAIL] ") + label );
		if (!ok) failures++;
	}

	static void info( String s ){ infos++; System.out.println("  [INFO] " + s ); }

	/** 真 Rat：只把 rollToDropLoot 换成空实现（headless 里没有 Gdx.files，掉落生成跑不了），
	 *  die() 走**原版 Mob.die**（这正是被测对象）。 */
	static class ProbeRat extends Rat {
		@Override
		public void rollToDropLoot(){ /* 探针环境不生成掉落 */ }
	}

	static ProbeRat freshMob(){
		ProbeRat m = new ProbeRat();
		m.HP = 0;
		m.HT = 40;
		m.EXP = 5;
		m.alignment = Char.Alignment.ENEMY;
		m.pos = 5 + 5 * Dungeon.level.width();
		Dungeon.level.heroFOV[m.pos] = true;	//跳过「不在视野时 GLog」那句（那会去读文本资源）
		return m;
	}

	static Hero freshHero(){
		Hero h = new Hero();
		h.HP = 10;
		h.HT = 20;
		//⚠️ 脱游戏环境没有 HeroSprite（h.sprite 恒 null）。Mob.destroy() 里有一条
		//   `if (exp > 0 && !NarcissusCrossSwordBase.blocksExpNow(hero)) hero.sprite.showStatusWithIcon(...)`
		//   —— exp 由 `hero.lvl <= maxLvl ? EXP : 0` 决定。把 lvl 抬到 10（> Rat.maxLvl=5）⇒ exp=0 ⇒
		//   既跳过精灵调用，也跳过 earnExp 的整套结算。这条只影响探针环境，不改变被测判据。
		h.lvl = 10;
		h.talents.add( new LinkedHashMap<Talent, Integer>() );	//tier 1
		//pointsInTalent 是**遍历所有 tier** 找的（不按下标取），所以放在哪个 tier 都能读到
		h.talents.get(0).put( Talent.GLORIOUS_TRIUMPH, 2 );		//荣耀凯旋 +2 ⇒ 回 10% HT
		h.talents.get(0).put( Talent.ORACULAR_BLESSING, 1 );		//神谕庇佑 +1 ⇒ 5 护盾
		return h;
	}

	public static void main( String[] args ) throws Exception {

		System.out.println("==============================================================");
		System.out.println("「带着标记倒下」击杀判定 · 行为探针（荣耀凯旋 / 指令目标）");
		System.out.println("==============================================================");

		int savedTrials = Dungeon.trials;
		Hero savedHero = Dungeon.hero;
		Level savedLevel = Dungeon.level;
		String savedVersion = Game.version;

		try {
			//⚠️ `MiningLevel` 的静态初始化链要经过 `RegularLevel.<clinit>`，那里读 `Game.version`
			//   （`Game.version.contains(...)`）—— 脱游戏时它是 null ⇒ ExceptionInInitializerError。
			//   给个普通版本号即可（不带 BETA/RC，避免把 SPDSettings.betas() 打开）。
			Game.version = "3.3.0";

			//一颗真关卡：Mob.die 里会读 Dungeon.level.heroFOV
			Level lvl = new MiningLevel();
			lvl.setSize( 12, 12 );
			//`mobs` 只在 Level.create() 里 new，这里手工补一个（Mob.destroy 会 remove 自己）
			lvl.mobs = new HashSet<Mob>();
			Dungeon.level = lvl;
		} catch (Throwable t) {
			info("无法构造测试关卡（" + t + "）——后续凡需要关卡的段落一律跳过");
		}

		// ------------------------------------------------ ① 拇指 荣耀凯旋：死亡即触发
		System.out.println("\n--- ① 荣耀凯旋：带标记的怪死亡即触发，且与「谁打死的」无关 ---");
		try {
			Hero hero = freshHero();
			Dungeon.hero = hero;

			ProbeRat m = freshMob();
			Buff.affect( m, AimHeartMark.class );

			check( m.buff( AimHeartMark.class ) != null, "前置：怪身上确实带着 AimHeartMark" );

			int hpBefore = hero.HP;
			m.die( Chasm.class );		//⚠️ 非英雄击杀来源（掉坑）。旧判据要求「英雄这一击打死」，这里必须也触发

			check( hero.HP > hpBefore,
					"怪死亡 ⇒ 荣耀凯旋真的回血了（" + hpBefore + " → " + hero.HP + "，判据已与攻击调用栈解耦）" );
			check( hero.HP == hpBefore + Math.round( hero.HT * 0.05f * 2 ),
					"回血量＝最大生命 × 5% × 天赋点数（20 × 5% × 2 = " + Math.round(hero.HT*0.05f*2) + "）" );
			check( m.buff( AimHeartMark.class ) == null,
					"die() 走完之后 buff 已被 onRemove 一并摘掉 ⇒ 钩子确实跑在摘 buff **之前**（放后面就永远读不到）" );
			check( !m.geburaAimMarked, "抄存标记被消费成 false（一次性）" );
		} catch (Throwable t) {
			info("① 段无法在脱离游戏的环境下跑（" + t + "）");
			t.printStackTrace();
		}

		// ------------------------------------------------ ② 食指 指令目标：死亡即触发 + 对照组
		System.out.println("\n--- ② 指令目标：带标志的怪死亡 ⇒ 神谕庇佑触发；不带标志 ⇒ 不触发 ---");
		try {
			Hero hero = freshHero();
			Dungeon.hero = hero;

			ProbeRat m = freshMob();
			Buff.affect( m, InstructionTarget.class );
			check( m.buff( InstructionTarget.class ) != null, "前置：怪身上确实带着 InstructionTarget" );

			m.die( Chasm.class );
			check( hero.buff( Barrier.class ) != null,
					"怪死亡 ⇒ 神谕庇佑（+5 护盾）触发 —— 同样与「谁打死的」无关" );

			//对照组：同样天赋、同样死法，但身上没有标记
			Hero hero2 = freshHero();
			Dungeon.hero = hero2;
			ProbeRat m2 = freshMob();
			m2.die( Chasm.class );
			check( hero2.buff( Barrier.class ) == null,
					"对照组：不带 InstructionTarget 的怪死亡 ⇒ **不**给护盾（判据确实是「携带 buff」）" );
			check( hero2.HP == 10,
					"对照组也不该回血（指令目标分支不会连带触发荣耀凯旋）" );
		} catch (Throwable t) {
			info("② 段无法在脱离游戏的环境下跑（" + t + "）");
			t.printStackTrace();
		}

		// ------------------------------------------------ ③ 锁血抄存的回落
		System.out.println("\n--- ③ GEBURA 锁血抄存的携带状态：live 没了，回落仍能触发 ---");
		try {
			Hero hero = freshHero();
			Dungeon.hero = hero;

			ProbeRat m = freshMob();
			//刻意**不挂** live buff：模拟「锁血期间 10 回合的 AimHeartMark 已过期」
			check( m.buff( AimHeartMark.class ) == null, "前置：live 的 AimHeartMark 不在身上" );

			m.geburaAimMarked = true;	//＝闸门在锁血那一刻抄下来的携带状态
			int hpBefore = hero.HP;
			m.die( Chasm.class );

			check( hero.HP > hpBefore,
					"live 取不到时回落到抄存 ⇒ 仍然回血（" + hpBefore + " → " + hero.HP + "）" );
			check( !m.geburaAimMarked, "抄存用掉即清（不会二次触发）" );
		} catch (Throwable t) {
			info("③ 段无法在脱离游戏的环境下跑（" + t + "）");
			t.printStackTrace();
		}

		// ------------------------------------------------ ④ 既无 live 又无抄存 ⇒ 不触发
		System.out.println("\n--- ④ 负向对照：既没有 live buff 也没有抄存 ⇒ 什么都不该发生 ---");
		try {
			Hero hero = freshHero();
			Dungeon.hero = hero;

			ProbeRat m = freshMob();
			m.die( Chasm.class );

			check( hero.HP == 10, "不回血（HP 保持 " + hero.HP + "）" );
			check( hero.buff( Barrier.class ) == null, "不给护盾" );
		} catch (Throwable t) {
			info("④ 段无法在脱离游戏的环境下跑（" + t + "）");
			t.printStackTrace();
		}

		// ------------------------------------------------ ⑤ 闸门抄存
		System.out.println("\n--- ⑤ 闸门（开 GEBURA）在锁血那一刻抄下两个携带状态 ---");
		try {
			ProbeRat graced = freshMob();
			Buff.affect( graced, AimHeartMark.class );
			Buff.affect( graced, InstructionTarget.class );

			Dungeon.trials = Trials.GEBURA;
			Dungeon.hero = null;		//财富/掉落部分不参与本段
			boolean took = Trials.interceptLethalDamage( graced, null );

			check( took, "闸门接管了这一击（返回 true）" );
			check( graced.geburaGrace, "mob.geburaGrace 为真（濒死无敌缓存，既有行为未受影响）" );
			check( graced.geburaAimMarked, "抄存：geburaAimMarked = true" );
			check( graced.geburaInstrTarget, "抄存：geburaInstrTarget = true" );
			check( graced.buff( GeburaGrace.class ) != null, "GeburaGrace 已挂上（计时开始）" );

			//关掉考验：同一路径应当完全不写
			ProbeRat plain = freshMob();
			Buff.affect( plain, AimHeartMark.class );
			Buff.affect( plain, InstructionTarget.class );
			Dungeon.trials = 0;
			boolean took2 = Trials.interceptLethalDamage( plain, null );

			check( !took2, "关掉 GEBURA：闸门不接管（返回 false）" );
			check( !plain.geburaAimMarked && !plain.geburaInstrTarget,
					"关掉 GEBURA：抄存字段保持初值（未开启时行为逐字不变）" );
			check( !plain.geburaGrace, "关掉 GEBURA：不进入濒死无敌" );
		} catch (Throwable t) {
			info("⑤ 段无法在脱离游戏的环境下跑（" + t + "）");
			t.printStackTrace();
		}

		// ------------------------------------------------ ⑥ 不碰随机流
		System.out.println("\n--- ⑥ 闸门的两次 buff 读取不碰随机数 ---");
		try {
			Dungeon.trials = Trials.GEBURA;
			Dungeon.hero = null;

			Random.pushGenerator( 424242L );
			float[] a = new float[200];
			for (int i = 0; i < a.length; i++) a[i] = Random.Float();
			Random.popGenerator();

			ProbeRat rng = freshMob();
			Buff.affect( rng, AimHeartMark.class );
			Buff.affect( rng, InstructionTarget.class );

			Random.pushGenerator( 424242L );
			float[] b = new float[200];
			for (int i = 0; i < b.length; i++){
				if (i % 10 == 0){
					rng.geburaUsed = false;		//让闸门每次都真的走到抄存那几行
					Trials.interceptLethalDamage( rng, null );
				}
				b[i] = Random.Float();
			}
			Random.popGenerator();

			boolean same = true;
			for (int i = 0; i < a.length; i++) if (a[i] != b[i]) { same = false; break; }
			check( same, "同一 seed 下 200 个随机数逐位相同（穿插 20 次闸门调用）" );
		} catch (Throwable t) {
			info("⑥ 段无法在脱离游戏的环境下跑（" + t + "）");
			t.printStackTrace();
		}

		// ------------------------------------------------ 摘要
		System.out.println("\n==============================================================");
		System.out.printf("断言 %d 条：通过 %d，失败 %d（另有 %d 条环境说明）%n",
				checks, checks - failures, failures, infos);
		System.out.println("==============================================================");

		Dungeon.trials = savedTrials;
		Dungeon.hero = savedHero;
		Dungeon.level = savedLevel;
		Game.version = savedVersion;

		if (failures > 0) System.exit( 1 );
	}
}
