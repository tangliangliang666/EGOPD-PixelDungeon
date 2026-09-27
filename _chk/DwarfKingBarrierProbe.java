// 矮人国王二阶段「王座屏障」刻度的行为探针（真跑游戏类，不改任何游戏文件）
//
// 为什么要有它：verify_dwarfking_chesed.py 的算术模拟是**我用 Python 重写了一遍分支逻辑**，
// 它证明的是「这套刻度算得对」；但「**Java 里那三行算式到底算出什么数**」「波次门控在真 act() 里
// 是不是真的按新阈值放行」只有**真 new 一个 DwarfKing、反射调真方法、真跑 act()**才算数。
//
// 三层：
//   ① 真算式：反射调 private barrierKills() / barrierChip() / barrierThreshold(int)，
//      在 HT=300/450 上必须与**原版写死的字面量**逐个相同（25 / 200 / 100 / 300 / 150），
//      在 CHESED 的 HT=375、CHESED+强化挑战的 HT=563 上给出正确的放大值。
//   ② 真门控：把 phase 置 2、真调 act()，用 summonsMade 是否推进来判断走了哪条分支 ——
//      这正是用户报的「二阶段不出怪」：CHESED 场景下第 4 次击杀后屏障 247，
//      新阈值 250 放行、旧阈值 200 卡死。两边都验（放行 + 仍能拦住）。
//   ③ 真随机流：phase 2 的整条 act() 路径与三条算式都不碰 Random —— 同一 seed 前后取数逐位相同。
//
// 编译/运行：见 skill egopd-source-verify §1。classpath 把 _chk/_javachk 排最前（新编的 DwarfKing 在那里），
// 然后 core/SPD-classes/services + gdx + gdx-controllers。
// 说明：脱离游戏没有 Gdx，故用 Unsafe.allocateInstance 造「只有 getSummoningPos() 恒返 -1 的空关卡」；
// 探针只驱动 **phase 2**（它的每条分支都 return true，不会落进 super.act() 去碰精灵/敌人）。
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import sun.misc.Unsafe;

import com.watabou.utils.Random;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;

public class DwarfKingBarrierProbe {

	static int checks = 0;
	static int failures = 0;
	static Unsafe unsafe;

	static void check( boolean ok, String label ){
		checks++;
		System.out.println( (ok ? "  [OK]   " : "  [FAIL] ") + label );
		if (!ok) failures++;
	}

	static void info( String s ){ System.out.println( "  [INFO] " + s ); }

	/** 只覆写召唤位查询：恒返 -1（没有空位）⇒ summonSubject 不成功但 summonsMade 照旧推进，
	 *  于是可以不加载贴图/关卡数据就观察「走了哪条分支」。 */
	public static class StubCityBoss extends CityBossLevel {
		@Override
		public int getSummoningPos(){
			return -1;
		}
	}

	// ------------------------------------------------------------------ 反射小工具
	static void setInt( Object o, String name, int v ) throws Exception {
		Field f = findField( o.getClass(), name );
		f.setAccessible( true );
		f.setInt( o, v );
	}

	static int getInt( Object o, String name ) throws Exception {
		Field f = findField( o.getClass(), name );
		f.setAccessible( true );
		return f.getInt( o );
	}

	static void setBool( Object o, String name, boolean v ) throws Exception {
		Field f = findField( o.getClass(), name );
		f.setAccessible( true );
		f.setBoolean( o, v );
	}

	static Field findField( Class<?> c, String name ) throws Exception {
		while (c != null){
			try { return c.getDeclaredField( name ); }
			catch (NoSuchFieldException e){ c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException( name );
	}

	static int callInt( Object o, String name, Class<?>[] types, Object... args ) throws Exception {
		Class<?> c = o.getClass();
		while (c != null){
			try {
				Method m = c.getDeclaredMethod( name, types );
				m.setAccessible( true );
				return (Integer) m.invoke( o, args );
			} catch (NoSuchMethodException e){ c = c.getSuperclass(); }
		}
		throw new NoSuchMethodException( name );
	}

	/** 造一只「干净的」国王：真 new 出来，pos 避开王座（throne 默认 0），phase 置 2，装着指定盾量。 */
	static DwarfKing makeKing( int ht, int shield ) throws Exception {
		DwarfKing k = new DwarfKing();
		k.pos = 12345;                 //≠ CityBossLevel.throne（它默认 0）⇒ 不会走 throwItems()
		setInt( k, "phase", 2 );
		setInt( k, "summonsMade", 0 );
		k.HT = ht;
		k.HP = ht;
		if (shield > 0){
			Buff.affect( k, DwarfKing.DKBarrior.class ).setShield( shield );
		}
		return k;
	}

	/** 真调一次 act()，返回之后 summonsMade 的值。 */
	static int runAct( DwarfKing k ) throws Exception {
		Method act = null;
		Class<?> c = DwarfKing.class;
		while (c != null && act == null){
			try { act = c.getDeclaredMethod( "act" ); }
			catch (NoSuchMethodException e){ c = c.getSuperclass(); }
		}
		act.setAccessible( true );
		act.invoke( k );
		return getInt( k, "summonsMade" );
	}

	// ------------------------------------------------------------------ ① 真算式
	static void part1() throws Exception {
		info( "-- ① 真 Java 算式（反射调 private 方法）--" );

		int saved = Dungeon.challenges;

		Dungeon.challenges = 0;
		DwarfKing k = makeKing( 300, 0 );
		check( callInt( k, "barrierKills", new Class<?>[]{} ) == 12,
				"常规：barrierKills() == 12" );
		Dungeon.challenges = Challenges.STRONGER_BOSSES;
		check( callInt( k, "barrierKills", new Class<?>[]{} ) == 18,
				"强化挑战：barrierKills() == 18（说明它读的是**调用时**的挑战位，不是构造时）" );

		// 原版取值必须与写死的字面量逐个相同（⚠️ 每个用例前显式置挑战位，别把上一组的位留过来）
		Dungeon.challenges = 0;
		check( callInt( makeKing( 300, 0 ), "barrierChip", new Class<?>[]{} ) == 25,
				"常规 HT=300：barrierChip() == 25（原版字面量 HT/12 == 25）" );
		check( callInt( makeKing( 300, 0 ), "barrierThreshold", new Class<?>[]{ int.class }, 2 ) == 200,
				"常规 HT=300：barrierThreshold(2) == 200（原版字面量 200）" );
		check( callInt( makeKing( 300, 0 ), "barrierThreshold", new Class<?>[]{ int.class }, 1 ) == 100,
				"常规 HT=300：barrierThreshold(1) == 100（原版字面量 100）" );

		Dungeon.challenges = Challenges.STRONGER_BOSSES;
		check( callInt( makeKing( 450, 0 ), "barrierChip", new Class<?>[]{} ) == 25,
				"强化 HT=450：barrierChip() == 25（原版字面量 HT/18 == 25）" );
		check( callInt( makeKing( 450, 0 ), "barrierThreshold", new Class<?>[]{ int.class }, 2 ) == 300,
				"强化 HT=450：barrierThreshold(2) == 300（原版字面量 300）" );
		check( callInt( makeKing( 450, 0 ), "barrierThreshold", new Class<?>[]{ int.class }, 1 ) == 150,
				"强化 HT=450：barrierThreshold(1) == 150（原版字面量 150）" );

		// CHESED 后的实际值（挑战位归 0）
		Dungeon.challenges = 0;
		check( callInt( makeKing( 375, 0 ), "barrierChip", new Class<?>[]{} ) == 32,
				"常规 HT=375（CHESED 300×1.25）：barrierChip() == 32（上取整，>HT/12=31）" );
		check( callInt( makeKing( 375, 0 ), "barrierThreshold", new Class<?>[]{ int.class }, 2 ) == 250,
				"常规 HT=375：barrierThreshold(2) == 250" );
		check( callInt( makeKing( 375, 0 ), "barrierThreshold", new Class<?>[]{ int.class }, 1 ) == 125,
				"常规 HT=375：barrierThreshold(1) == 125" );

		Dungeon.challenges = Challenges.STRONGER_BOSSES;
		check( callInt( makeKing( 563, 0 ), "barrierChip", new Class<?>[]{} ) == 32,
				"强化 HT=563（CHESED × 强化挑战）：barrierChip() == 32" );
		check( callInt( makeKing( 563, 0 ), "barrierThreshold", new Class<?>[]{ int.class }, 2 ) == 375,
				"强化 HT=563：barrierThreshold(2) == 375" );
		check( callInt( makeKing( 563, 0 ), "barrierThreshold", new Class<?>[]{ int.class }, 1 ) == 187,
				"强化 HT=563：barrierThreshold(1) == 187" );

		// 不变量：kills × chip ≥ HT —— 任何 HT 都打得空（两种挑战位各扫一遍）
		for (int mode = 0; mode < 2; mode++){
			Dungeon.challenges = (mode == 0) ? 0 : Challenges.STRONGER_BOSSES;
			int kills = (mode == 0) ? 12 : 18;
			int bad = 0;
			for (int ht = 1; ht <= 2000; ht++){
				int c2 = callInt( makeKing( ht, 0 ), "barrierChip", new Class<?>[]{} );
				if (kills * c2 < ht) bad++;
			}
			check( bad == 0, (mode == 0 ? "常规" : "强化") + "不变量：HT∈[1,2000] 上 "
					+ kills + "×barrierChip() ≥ HT（打不空的 HT 有 " + bad + " 例）" );
		}

		// 边界翻转证据（用户报的 bug 本体）
		Dungeon.challenges = 0;
		int thr2 = callInt( makeKing( 375, 0 ), "barrierThreshold", new Class<?>[]{ int.class }, 2 );
		int chip = callInt( makeKing( 375, 0 ), "barrierChip", new Class<?>[]{} );
		int shieldAfter4 = 375 - 4 * chip;
		check( chip == 32 && shieldAfter4 == 247, "CHESED 场景：第 4 次击杀后屏障 = 375 - 4×32 = 247" );
		check( shieldAfter4 <= thr2, "新阈值 250 ≥ 247 ⇒ **放行第二波**" );
		check( shieldAfter4 > 200, "旧阈值 200 < 247 ⇒ 原版写法在此**永远卡住**（这就是用户报的现象）" );
		check( 12 * chip >= 375, "12×32 = 384 ≥ 375 ⇒ 12 次击杀必定打空屏障（旧写法 12×31=372 < 375，永远剩 3 点）" );

		Dungeon.challenges = saved;
	}

	// ------------------------------------------------------------------ ② 真门控
	static void part2() throws Exception {
		info( "-- ② 真跑 act()：phase 2 波次门控 --" );

		int saved = Dungeon.challenges;
		Dungeon.challenges = 0;
		Dungeon.level = (Level) unsafe.allocateInstance( StubCityBoss.class );

		// ⚠️ 可驱动的分支有限：非强化挑战的「第三波」分支**无条件**先 sprite.centerEmitter() +
		//    Sample.INSTANCE.play(...)，脱离 Gdx 必 NPE ⇒ 它只验「拦住」那一侧（走 else 的 spend(TICK)），
		//    放行那一侧由 ① 的阈值数值 + Python 模拟器覆盖。起始 summonsMade 也要避开喊话点
		//    （第一波 ==0、第二波 ==4（常规）/==6（强化）、第三波 ==12）—— 那些都会碰精灵。
		check( runAct( makeKingSetMade( 375, 0, 1 ) ) == 2,
				"常规·第一波：summonsMade 1 → 2（不受阈值限制）" );
		check( runAct( makeKingSetMade( 375, 247, 5 ) ) == 6,
				"常规·第二波放行：shield=247 ≤ 250 ⇒ summonsMade 5 → 6" );
		check( runAct( makeKingSetMade( 375, 251, 5 ) ) == 5,
				"常规·第二波仍拦得住：shield=251 > 250 ⇒ summonsMade 停在 5" );
		check( runAct( makeKingSetMade( 375, 126, 9 ) ) == 9,
				"常规·第三波仍拦得住：shield=126 > 125 ⇒ summonsMade 停在 9" );

		// 强化挑战分支用的是另一组阈值（HT=563 ⇒ 375 / 187）
		Dungeon.challenges = Challenges.STRONGER_BOSSES;
		check( runAct( makeKingSetMade( 563, 0, 2 ) ) == 4,
				"强化·第一波：summonsMade 2 → 4" );
		check( runAct( makeKingSetMade( 563, 370, 7 ) ) == 10,
				"强化·第二波放行：shield=370 ≤ 375 ⇒ summonsMade 7 → 10" );
		check( runAct( makeKingSetMade( 563, 380, 7 ) ) == 7,
				"强化·第二波仍拦得住：shield=380 > 375 ⇒ summonsMade 停在 7" );
		check( runAct( makeKingSetMade( 563, 187, 13 ) ) == 15,
				"强化·第三波放行：shield=187 ≤ 187 ⇒ summonsMade 13 → 15" );
		check( runAct( makeKingSetMade( 563, 188, 13 ) ) == 13,
				"强化·第三波仍拦得住：shield=188 > 187 ⇒ summonsMade 停在 13" );

		// 同一个 shield 在两种挑战状态下判决不同 ⇒ 阈值确实随挑战位现算
		Dungeon.challenges = 0;
		int normal = runAct( makeKingSetMade( 375, 200, 5 ) );
		Dungeon.challenges = Challenges.STRONGER_BOSSES;
		int stronger = runAct( makeKingSetMade( 563, 200, 7 ) );
		check( normal == 6 && stronger == 10,
				"同一 shield=200：常规（≤250，5→6）与强化（≤375，7→10）都放行 —— 两组阈值各自生效" );

		Dungeon.challenges = saved;
	}

	static DwarfKing makeKingSetMade( int ht, int shield, int made ) throws Exception {
		DwarfKing k = makeKing( ht, shield );
		setInt( k, "summonsMade", made );
		return k;
	}

	// ------------------------------------------------------------------ ③ 真随机流
	static void part3() throws Exception {
		info( "-- ③ 不碰随机流 --" );

		int saved = Dungeon.challenges;
		Dungeon.challenges = 0;
		Dungeon.level = (Level) unsafe.allocateInstance( StubCityBoss.class );

		Random.pushGenerator( 20260924L );
		long[] a = new long[6];
		for (int i = 0; i < a.length; i++) a[i] = Random.Long();

		Random.pushGenerator( 20260924L );
		DwarfKing k = makeKingSetMade( 375, 247, 5 );
		callInt( k, "barrierChip", new Class<?>[]{} );
		callInt( k, "barrierThreshold", new Class<?>[]{ int.class }, 2 );
		callInt( k, "barrierKills", new Class<?>[]{} );
		runAct( k );                                   // 真跑一次波次分支
		long[] b = new long[6];
		for (int i = 0; i < b.length; i++) b[i] = Random.Long();

		boolean same = true;
		for (int i = 0; i < a.length; i++) if (a[i] != b[i]) same = false;
		check( same, "同一 seed：穿插三条算式 + 一次 act() 后，随机序列逐位相同（不消耗随机数）" );

		Dungeon.challenges = saved;
	}

	public static void main( String[] args ) throws Exception {
		Field f = Unsafe.class.getDeclaredField( "theUnsafe" );
		f.setAccessible( true );
		unsafe = (Unsafe) f.get( null );

		part1();
		part2();
		part3();

		System.out.println();
		System.out.println( "断言 " + checks + " 条：OK " + (checks - failures) + "，FAIL " + failures );
		System.out.println( failures == 0 ? "RUN_EXIT=0" : "RUN_EXIT=1" );
		System.exit( failures == 0 ? 0 : 1 );
	}
}
