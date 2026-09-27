import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.SmilingCorpseMountain;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 行为真值探针：**真 new 出三个 Boss**，反射调它们私有的阶段刻度方法，断言数值。
 *
 * 与 verify_boss_phase_scales.py 的分工：那边是「读源码 + 按常量重算」，这边是「跑真方法」——
 * 后者能抓到「常量写对了但方法体用错变量 / 少了个括号」这类读源码看不出的错。
 *
 * 关键判据：**在原版 HT 处与原写死值逐个相同**
 *   YogDzewa   HT=1000 ⇒ 300 / 100 / 400
 *   DwarfKing  HT=300（普通）⇒ 50；HT=450（强化挑战）⇒ 100
 *   SCM        HT=3000 ⇒ 换皮点 2000 / 1000
 * 以及开 CHESED 后（HT 被抬高）它们**跟着同比例变**，而不是卡在旧数字上。
 */
public class BossPhaseScaleProbe {

	static int pass = 0;
	static int fail = 0;

	static void check( boolean cond, String label ){
		if (cond) { pass++; System.out.println("  [OK]   " + label); }
		else      { fail++; System.out.println("  [FAIL] " + label); }
	}

	static void info( String label ){
		System.out.println("  [INFO] " + label);
	}

	/** 反射调无参私有 int 方法。 */
	static int callInt( Object target, String name ){
		try {
			Method m = target.getClass().getDeclaredMethod( name );
			m.setAccessible( true );
			return (Integer) m.invoke( target );
		} catch (Throwable t) {
			throw new RuntimeException( "调用 " + name + "() 失败: " + t, t );
		}
	}

	static void setInt( Object target, String field, int value ){
		try {
			Field f = null;
			Class<?> c = target.getClass();
			while (c != null && f == null) {
				try { f = c.getDeclaredField( field ); } catch (NoSuchFieldException e) { c = c.getSuperclass(); }
			}
			if (f == null) throw new NoSuchFieldException( field );
			f.setAccessible( true );
			f.setInt( target, value );
		} catch (Throwable t) {
			throw new RuntimeException( "写字段 " + field + " 失败: " + t, t );
		}
	}

	public static void main( String[] args ) throws Exception {

		System.out.println("Boss 阶段刻度探针（2026-09-24）");

		//================================================================== YogDzewa
		System.out.println("\n-- YogDzewa：阶段步长 / 末段下限 / 光束刻度 --");
		YogDzewa yog = new YogDzewa();

		setInt( yog, "HT", 1000 );
		int ys = callInt( yog, "phaseStep" );
		int yf = callInt( yog, "finalPhaseFloor" );
		int yb = callInt( yog, "beamStep" );
		info( "HT=1000 ⇒ step=" + ys + " floor=" + yf + " beam=" + yb );
		check( ys == 300, "HT=1000 ⇒ 阶段步长 300（＝原版写死值）" );
		check( yf == 100, "HT=1000 ⇒ 末段下限 100（＝原版写死值）" );
		check( yb == 400, "HT=1000 ⇒ 光束刻度 400（＝原版写死值）" );

		setInt( yog, "HT", 1250 );
		int ys2 = callInt( yog, "phaseStep" );
		int yf2 = callInt( yog, "finalPhaseFloor" );
		int yb2 = callInt( yog, "beamStep" );
		info( "HT=1250（开 CHESED）⇒ step=" + ys2 + " floor=" + yf2 + " beam=" + yb2 );
		check( ys2 == 375, "HT=1250 ⇒ 阶段步长同比例变为 375（旧写法会卡在 300）" );
		check( yf2 == 125, "HT=1250 ⇒ 末段下限同比例变为 125（旧写法会卡在 100）" );
		check( yb2 == 500, "HT=1250 ⇒ 光束刻度同比例变为 500（旧写法会卡在 400）" );

		setInt( yog, "HT", 999 );
		int ys3 = callInt( yog, "phaseStep" );
		int yb3 = callInt( yog, "beamStep" );
		info( "HT=999 ⇒ step=" + ys3 + " beam=" + yb3 + "（验证整除截断不越界）" );
		check( ys3 == 299, "HT=999 ⇒ 阶段步长 299（999×3/10 截断）" );
		check( yb3 == 399, "HT=999 ⇒ 光束刻度 399（999×2/5 截断）" );
		check( callInt( yog, "finalPhaseFloor" ) == 99, "HT=999 ⇒ 末段下限 99" );

		// 任意 HT 都成立：step*3 ≤ HT 且末段下限 ≥ 1
		int bad = 0;
		for (int ht = 200; ht <= 3000; ht++) {
			setInt( yog, "HT", ht );
			int step = callInt( yog, "phaseStep" );
			int floor = callInt( yog, "finalPhaseFloor" );
			int beam = callInt( yog, "beamStep" );
			if (step * 3 > ht || floor < 1 || beam < 1) bad++;
			// 三个阶段的下限必须严格递减（否则某个阶段会被跳过 = 卡死）
			int f1 = ht - step, f2 = ht - step * 2, f3 = ht - step * 3;
			if (!(f1 > f2 && f2 >= f3)) bad++;
		}
		check( bad == 0, "HT∈[200,3000] 全部 2801 个取值：三阶段下限严格递减、末段下限与光束刻度 ≥ 1"
				+ "（反例 " + bad + " 个）" );

		//================================================================== DwarfKing
		System.out.println("\n-- DwarfKing：一阶段转二阶段的入场血量 --");
		DwarfKing king = new DwarfKing();

		Dungeon.challenges = 0;
		setInt( king, "HT", 300 );
		int k1 = callInt( king, "phase2EntryHP" );
		info( "普通挑战 HT=300 ⇒ " + k1 );
		check( k1 == 50, "普通 HT=300 ⇒ 入场血量 50（＝原版写死值）" );
		setInt( king, "HT", 375 );
		int k2 = callInt( king, "phase2EntryHP" );
		info( "普通 + CHESED HT=375 ⇒ " + k2 );
		check( k2 == 62, "普通 + CHESED HT=375 ⇒ 62（不再是固定的 50）" );

		Dungeon.challenges = Challenges.STRONGER_BOSSES;
		setInt( king, "HT", 450 );
		int k3 = callInt( king, "phase2EntryHP" );
		info( "强化挑战 HT=450 ⇒ " + k3 );
		check( k3 == 100, "强化 HT=450 ⇒ 入场血量 100（＝原版写死值）" );
		setInt( king, "HT", 563 );
		int k4 = callInt( king, "phase2EntryHP" );
		info( "强化 + CHESED HT=563 ⇒ " + k4 );
		check( k4 == 125, "强化 + CHESED HT=563 ⇒ 125（不再是固定的 100）" );

		// 难度位必须**在调用时**读取（不是构造时缓存）
		setInt( king, "HT", 450 );
		Dungeon.challenges = 0;
		int k5 = callInt( king, "phase2EntryHP" );
		Dungeon.challenges = Challenges.STRONGER_BOSSES;
		int k6 = callInt( king, "phase2EntryHP" );
		check( k5 == 75 && k6 == 100,
				"同一实例、同一 HT=450 上切换难度位会立即改变结果（" + k5 + " vs " + k6
						+ "）⇒ 判据读的是调用时的挑战位，不是构造时缓存" );

		// 单调性：HT 越大入场血量越大（不会出现「血更多反而更早进二阶段」）
		boolean mono = true;
		int prev = 0;
		for (int ht = 150; ht <= 900; ht++) {
			setInt( king, "HT", ht );
			int v = callInt( king, "phase2EntryHP" );
			if (v < prev) { mono = false; break; }
			prev = v;
		}
		check( mono, "入场血量随 HT 非递减（HT∈[150,900] 全枚举）" );

		//================================================================== SmilingCorpseMountain
		System.out.println("\n-- SmilingCorpseMountain：换皮形态阈值 --");
		SmilingCorpseMountain scm = new SmilingCorpseMountain();

		setInt( scm, "HT", 3000 );
		check( scm.phase() == 3, "HT=3000 满血 ⇒ 形态 3" );
		setInt( scm, "HP", 2000 );
		check( scm.phase() == 3, "HT=3000 HP=2000 ⇒ 形态 3（原阈值边界）" );
		setInt( scm, "HP", 1999 );
		check( scm.phase() == 2, "HT=3000 HP=1999 ⇒ 形态 2" );
		setInt( scm, "HP", 1000 );
		check( scm.phase() == 2, "HT=3000 HP=1000 ⇒ 形态 2（原阈值边界）" );
		setInt( scm, "HP", 999 );
		check( scm.phase() == 1, "HT=3000 HP=999 ⇒ 形态 1" );

		setInt( scm, "HT", 3750 );
		setInt( scm, "HP", 2500 );
		check( scm.phase() == 3, "HT=3750（CHESED）HP=2500 ⇒ 形态 3（换皮点同比例后移）" );
		setInt( scm, "HP", 2499 );
		check( scm.phase() == 2, "HT=3750 HP=2499 ⇒ 形态 2" );
		setInt( scm, "HP", 1250 );
		check( scm.phase() == 2, "HT=3750 HP=1250 ⇒ 形态 2" );
		setInt( scm, "HP", 1249 );
		check( scm.phase() == 1, "HT=3750 HP=1249 ⇒ 形态 1" );

		// 与原写死阈值等价：HT=3000 时逐点一致
		int diff = 0;
		setInt( scm, "HT", 3000 );
		for (int hp = 0; hp <= 3000; hp++) {
			setInt( scm, "HP", hp );
			int expect = hp >= 2000 ? 3 : (hp >= 1000 ? 2 : 1);
			if (scm.phase() != expect) diff++;
		}
		check( diff == 0, "HT=3000 时与旧阈值在 0..3000 每一个 HP 上都一致（差异 " + diff + " 个）" );

		System.out.println();
		System.out.println( fail == 0
				? "全部符合预期（共 " + pass + " 条断言）"
				: ("有 " + fail + " 项与预期不符 / PASS " + pass) );
		if (fail != 0) System.exit(1);
	}
}
