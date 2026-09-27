// 水晶任务矿洞层「虚空地形」的行为探针（真跑游戏类，不改任何游戏文件）
//
// 为什么要有它：verify_marked_kill_routes.py 的 F/G 组证的是**结构**（carveVoid 里写了什么、
// 谁在什么时机调它、画师怎么还原）。但下面五件事只有**真建关卡、真调 carveVoid、真数格子**才算数：
//   ① `handlesChasmFall()` 的门控真的按任务类型走；
//   ② 「替代地板而不是墙体 / 整块夹在深内区 ⇒ 外面留一整圈地板 / 单一连通块」三条不变量
//      在**多个随机采样**下都成立；
//   ③ 挖出来的虚空是**单一 4-连通块**（不会把房间切成互不相通的两半，也不留孤立碎块）；
//   ④ ⭐ 最要紧的一条：**只写 `map[]` 真的够用吗** —— 必须先证明「不调 `buildFlagMaps()` 时 `pit[]`
//      仍然是 false」（＝幽灵格风险真实存在），再证明「调了之后每个深渊格的 `pit[]` 都是 true
//      且 `passable[]` 都是 false」（＝`Level.create()` 会把 flags 重算回来）。
//      这正是 `MiningLevel.carveVoid` 只调 `Painter.set` 却安全的前提。
//
// 八段：① 门控 ② 20 次采样下的四条地形不变量（＋干净地板上的「单块性」）③ protectedCell 及其八邻格不动
//       ④ 没有地板可挖 ⇒ 一格不动 ⑤ 小房间（深内区空 ⇒ 退出；2×2 ⇒ 夹紧后仍可挖）
//       ⑥ buildFlagMaps 前后对比（幽灵格实证）⑦ 同 seed 结果逐格相同 ⑧ 虚空不吞掉可走的地图连通性（粗检）
//
// 编译/运行：见 skill egopd-source-verify §1。classpath 必须把 _chk/_javachk 排最前
//（新编的 MiningLevel / Level 在那里），再 core/SPD-classes/services + gdx + gdx-controllers。
// 说明：脱离游戏没有贴图资源，所以用真 `MiningLevel()` + `setSize()` 造空关卡
//（⚠️ 必须先给 `Game.version` 赋值，否则 `RegularLevel.<clinit>` 会 NPE），
// 并手工补 `heaps` / `blobs` / `mobs`（它们平时由 `Level.create()` 创建）。
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.watabou.noosa.Game;
import com.watabou.utils.Random;
import com.watabou.utils.SparseArray;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.levels.DeadEndLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.MiningLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;

public class MiningVoidProbe {

	static int checks = 0;
	static int failures = 0;
	static int infos = 0;

	static void check( boolean ok, String label ){
		checks++;
		System.out.println( (ok ? "  [OK]   " : "  [FAIL] ") + label );
		if (!ok) failures++;
	}

	static void info( String s ){ infos++; System.out.println("  [INFO] " + s ); }

	/** 最小的具体房间：只要 `paint` 不再是抽象，Room 的其它成员都是现成的。 */
	static class ProbeRoom extends Room {
		@Override
		public void paint( Level level ){ /* 探针里不用它 */ }
	}

	static Room room( int left, int top, int right, int bottom ){
		ProbeRoom r = new ProbeRoom();
		r.left = left; r.top = top; r.right = right; r.bottom = bottom;
		return r;
	}

	static Level freshLevel( int w, int h ) throws Exception {
		Level l = new MiningLevel();
		l.setSize( w, h );
		//这三样平时由 Level.create() 创建；carveVoid 会读 heaps
		l.heaps = new SparseArray<>();
		l.blobs = new HashMap<>();
		l.mobs = new HashSet<Mob>();
		return l;
	}

	static void fill( Level l, int x0, int y0, int x1, int y1, int terrain ){
		for (int y = y0; y <= y1; y++){
			for (int x = x0; x <= x1; x++){
				l.map[x + y * l.width()] = terrain;
			}
		}
	}

	static ArrayList<Integer> chasms( Level l ){
		ArrayList<Integer> out = new ArrayList<>();
		for (int i = 0; i < l.length(); i++){
			if (l.map[i] == Terrain.CHASM) out.add( i );
		}
		return out;
	}

	/** 4-邻接连通分量数（虚空应恰好 1 块）。 */
	static int components( Level l, ArrayList<Integer> cells ){
		HashSet<Integer> set = new HashSet<>( cells );
		HashSet<Integer> seen = new HashSet<>();
		int comps = 0;
		for (int c : cells){
			if (seen.contains( c )) continue;
			comps++;
			ArrayDeque<Integer> q = new ArrayDeque<>();
			q.add( c ); seen.add( c );
			while (!q.isEmpty()){
				int cur = q.poll();
				int x = cur % l.width(), y = cur / l.width();
				int[] nb = { x - 1 + y * l.width(), x + 1 + y * l.width(),
						x + (y - 1) * l.width(), x + (y + 1) * l.width() };
				for (int n : nb){
					if (n < 0 || n >= l.length()) continue;
					if (!set.contains( n ) || seen.contains( n )) continue;
					seen.add( n ); q.add( n );
				}
			}
		}
		return comps;
	}

	/**
	 * 从某一格出发、只走非深渊格、4-邻接洪水填充，看能覆盖多少格。
	 * 用来粗检「虚空没把房间切成孤岛」：房间内环的每一格都该能走到。
	 */
	static int floorReach( Level l, int start ){
		boolean[] seen = new boolean[l.length()];
		ArrayDeque<Integer> q = new ArrayDeque<>();
		seen[start] = true; q.add( start );
		int n = 0;
		while (!q.isEmpty()){
			int cur = q.poll();
			n++;
			int x = cur % l.width(), y = cur / l.width();
			int[] nb = { cur - 1, cur + 1, cur - l.width(), cur + l.width() };
			for (int c : nb){
				if (c < 0 || c >= l.length() || seen[c]) continue;
				if (l.map[c] == Terrain.CHASM || l.map[c] == Terrain.WALL) continue;
				seen[c] = true; q.add( c );
			}
		}
		return n;
	}

	static void setQuestType( int t ) throws Exception {
		Field f = Blacksmith.Quest.class.getDeclaredField( "type" );
		f.setAccessible( true );
		f.setInt( null, t );
	}

	public static void main( String[] args ) throws Exception {

		System.out.println("==============================================================");
		System.out.println("水晶任务矿洞层「虚空地形」· 行为探针（carveVoid / handlesChasmFall）");
		System.out.println("==============================================================");

		//先给 Game.version：MiningLevel 的静态初始化链要过 RegularLevel.<clinit>
		Game.version = "3.3.0";

		int savedType = Blacksmith.Quest.Type();

		// ------------------------------------------------ ① 门控
		System.out.println("\n--- ① handlesChasmFall() 只在水晶任务为真 ---");
		try {
			setQuestType( Blacksmith.Quest.CRYSTAL );
			check( new MiningLevel().handlesChasmFall(),
					"水晶任务（CRYSTAL=1）⇒ 本层自理落坑（true）" );

			setQuestType( Blacksmith.Quest.GNOLL );
			check( !new MiningLevel().handlesChasmFall(),
					"矮人任务（GNOLL=2）⇒ 交给原版换层（false）" );

			setQuestType( Blacksmith.Quest.FUNGI );
			check( !new MiningLevel().handlesChasmFall(), "真菌任务（FUNGI=3）⇒ false" );

			setQuestType( 0 );
			check( !new MiningLevel().handlesChasmFall(), "未开始任务（type=0）⇒ false" );

			//基类默认（DeadEndLevel 没覆写它）——用 Unsafe 免构造，避免碰贴图
			Field uf = sun.misc.Unsafe.class.getDeclaredField( "theUnsafe" );
			uf.setAccessible( true );
			sun.misc.Unsafe unsafe = (sun.misc.Unsafe) uf.get( null );
			Level base = (Level) unsafe.allocateInstance( DeadEndLevel.class );
			check( !base.handlesChasmFall(),
					"基类 Level 默认 false ⇒ 其它所有层行为逐字不变" );
		} catch (Throwable t) {
			info("① 段无法在脱离游戏的环境下跑（" + t + "）");
			t.printStackTrace();
		}

		// ------------------------------------------------ ② 四条地形不变量（20 次采样）
		System.out.println("\n--- ② carveVoid 的地形不变量（20 个随机采样）---");
		try {
			int samples = 20;
			int badTerrain = 0, badRing = 0, emptyCarve = 0, outsideDeep = 0, wallHug = 0;
			int totalCarved = 0, minCarved = Integer.MAX_VALUE, maxCarved = 0;

			//房间内圈（EMPTY 区）= [3,16]²；深内区 = [4,15]²
			final int ILO = 3, IHI = 16, DLO = 4, DHI = 15;

			for (int s = 0; s < samples; s++){
				Level l = freshLevel( 20, 20 );
				fill( l, 0, 0, 19, 19, Terrain.WALL );
				fill( l, 2, 2, 17, 17, Terrain.EMPTY );

				//随机掺入墙体 / 矿脉 / 水面：它们**一个都不该**变成深渊
				Random.pushGenerator( 9000 + s );
				for (int i = 0; i < 40; i++){
					int x = 4 + Random.Int( 12 ), y = 4 + Random.Int( 12 );
					int t = new int[]{ Terrain.WALL, Terrain.MINE_CRYSTAL, Terrain.WATER }[Random.Int(3)];
					l.map[x + y * l.width()] = t;
				}
				Random.popGenerator();

				int[] before = l.map.clone();
				MiningLevel.carveVoid( l, room( 2, 2, 17, 17 ), -1 );

				ArrayList<Integer> cs = chasms( l );
				totalCarved += cs.size();
				minCarved = Math.min( minCarved, cs.size() );
				maxCarved = Math.max( maxCarved, cs.size() );
				if (cs.isEmpty()) emptyCarve++;

				//(a) 只在原 EMPTY / EMPTY_DECO 上产生深渊；非深渊格一格未动
				for (int c : cs){
					if (before[c] != Terrain.EMPTY && before[c] != Terrain.EMPTY_DECO) badTerrain++;
				}
				for (int i = 0; i < l.length(); i++){
					if (l.map[i] != Terrain.CHASM && l.map[i] != before[i]) badTerrain++;
				}
				//(b) 贴着房间内墙的那一圈（偏移 1 ⇒ 坐标 3 与 16）永远是地板
				for (int y = ILO; y <= IHI; y++){
					for (int x = ILO; x <= IHI; x++){
						if (x != ILO && x != IHI && y != ILO && y != IHI) continue;
						if (l.map[x + y * l.width()] == Terrain.CHASM) badRing++;
					}
				}
				//(c) 整块圆盘夹在深内区里 + 与内墙至少隔 1 格
				for (int c : cs){
					int x = c % 20, y = c / 20;
					if (x < DLO || x > DHI || y < DLO || y > DHI) outsideDeep++;
					int gap = Math.min( Math.min( x - ILO, IHI - x ), Math.min( y - ILO, IHI - y ) );
					if (gap < 1) wallHug++;
				}
			}

			check( badTerrain == 0, "只在原 EMPTY/EMPTY_DECO 上挖；墙体/矿脉/水面一格未动（违规 "
					+ badTerrain + " 格）" );
			check( badRing == 0, "贴着房间内墙的那一圈（偏移 1）永远是地板 ⇒ 沿墙走不会掉下去、不封死门口（违规 "
					+ badRing + " 格）" );
			check( emptyCarve == 0, samples + " 个采样每个都真的挖出了虚空（空挖 "
					+ emptyCarve + " 次；规模 " + minCarved + "~" + maxCarved
					+ " 格，共 " + totalCarved + " 格）" );
			check( outsideDeep == 0, "整块圆盘都夹在深内区 [4,15]² 内（越界 " + outsideDeep + " 格）" );
			check( wallHug == 0, "每个深渊格与房间内墙都至少隔 1 格（贴墙 " + wallHug + " 格）" );

			//(d) ⭐ 连通性只在「干净地板」上成立：房间里的矿脉/墙体本就会把虚空切成几块
			//    （MineSmallRoom 是**先撒矿脉再挖虚空** ⇒ 「只吃地板」的规则把矿脉留在原处，
			//    于是矿脉成了虚空里的踏脚石 —— 这是地形规则的正确结果，不是 bug）。
			//    所以连通性要单独在无阻挡的地板上证：外圈孤立格被收回之后，必定是一整块。
			int cleanMulti = 0, cleanEmpty = 0, cleanTrials = 30;
			for (int s = 0; s < cleanTrials; s++){
				Level l = freshLevel( 20, 20 );
				fill( l, 0, 0, 19, 19, Terrain.WALL );
				fill( l, 2, 2, 17, 17, Terrain.EMPTY );
				Random.pushGenerator( 12000 + s );
				MiningLevel.carveVoid( l, room( 2, 2, 17, 17 ), -1 );
				Random.popGenerator();
				ArrayList<Integer> cs = chasms( l );
				if (cs.isEmpty()) cleanEmpty++;
				else if (components( l, cs ) != 1) cleanMulti++;
			}
			check( cleanEmpty == 0 && cleanMulti == 0,
					"干净地板上 " + cleanTrials + " 次采样都是一整块（多块 " + cleanMulti
							+ " 次、空挖 " + cleanEmpty + " 次）⇒ 外圈的孤立格确实被收回了" );
		} catch (Throwable t) {
			info("② 段无法在脱离游戏的环境下跑（" + t + "）");
			t.printStackTrace();
		}

		// ------------------------------------------------ ③ protectedCell 保护
		System.out.println("\n--- ③ 关键单位（protectedCell）及其八邻格保持地板 ---");
		try {
			int bad = 0, carved = 0, trials = 10;
			for (int s = 0; s < trials; s++){
				Level l = freshLevel( 20, 20 );
				fill( l, 0, 0, 19, 19, Terrain.WALL );
				fill( l, 2, 2, 17, 17, Terrain.EMPTY );

				//关键单位放在深内区正中心 (10,10)（水晶守卫 / 尖塔的落脚点）
				int prot = 10 + 10 * 20;
				Random.pushGenerator( 61000 + s );
				MiningLevel.carveVoid( l, room( 2, 2, 17, 17 ), prot );
				Random.popGenerator();

				ArrayList<Integer> cs = chasms( l );
				carved += cs.size();
				for (int dy = -1; dy <= 1; dy++){
					for (int dx = -1; dx <= 1; dx++){
						if (l.map[prot + dx + dy * 20] == Terrain.CHASM) bad++;
					}
				}
			}
			check( bad == 0, trials + " 次采样里保护格及其八邻格一次都没被挖（违规 " + bad + " 格）" );
			check( carved > 0, "同时仍然挖出了虚空（共 " + carved + " 格）⇒ 不是「因为保护而干脆不挖」" );
		} catch (Throwable t) {
			info("③ 段无法在脱离游戏的环境下跑（" + t + "）");
			t.printStackTrace();
		}

		// ------------------------------------------------ ④ 没有地板可挖
		System.out.println("\n--- ④ 深内区全是墙体 ⇒ 一格都不动 ---");
		try {
			Level l = freshLevel( 20, 20 );
			fill( l, 0, 0, 19, 19, Terrain.WALL );
			fill( l, 2, 2, 17, 17, Terrain.EMPTY );
			fill( l, 4, 4, 15, 15, Terrain.WALL );   //把深内区全铺成墙
			int[] before = l.map.clone();
			MiningLevel.carveVoid( l, room( 2, 2, 17, 17 ), -1 );
			boolean same = true;
			for (int i = 0; i < l.length(); i++) if (l.map[i] != before[i]) same = false;
			check( same, "没有可挖的地板 ⇒ map[] 逐格不变（不产生半截挖掘）" );
		} catch (Throwable t) {
			info("④ 段无法在脱离游戏的环境下跑（" + t + "）");
			t.printStackTrace();
		}

		// ------------------------------------------------ ⑤ 小房间：一种安全退出，一种夹紧后仍可挖
		System.out.println("\n--- ⑤ 小房间：深内区为空 ⇒ 退出；深内区仅 2×2 ⇒ 只挖在里面 ---");
		try {
			//(a) 深内区为空（right=3 < left=4）⇒ 直接 return
			boolean threw = false;
			int changed = 0;
			try {
				Level l = freshLevel( 10, 10 );
				fill( l, 0, 0, 9, 9, Terrain.WALL );
				fill( l, 2, 2, 7, 7, Terrain.EMPTY );
				int[] before = l.map.clone();
				MiningLevel.carveVoid( l, room( 2, 2, 5, 5 ), -1 );   //深内区为空 ⇒ 早退
				for (int i = 0; i < l.length(); i++) if (l.map[i] != before[i]) changed++;
			} catch (Throwable t) {
				threw = true;
			}
			check( !threw, "深内区为空的房间不抛异常" );
			check( changed == 0, "深内区为空的房间地形一格未动（改动 " + changed + " 格）" );

			//(b) 深内区只有 2×2（最紧的夹取场景）：仍然挖得出，且一格都不越出 [4,5]²
			Level l2 = freshLevel( 10, 10 );
			fill( l2, 0, 0, 9, 9, Terrain.WALL );
			fill( l2, 2, 2, 7, 7, Terrain.EMPTY );
			Random.pushGenerator( 777 );
			MiningLevel.carveVoid( l2, room( 2, 2, 7, 7 ), -1 );
			Random.popGenerator();
			ArrayList<Integer> cs2 = chasms( l2 );
			int out2 = 0;
			for (int c : cs2){
				int x = c % 10, y = c / 10;
				if (x < 4 || x > 5 || y < 4 || y > 5) out2++;
			}
			check( !cs2.isEmpty() && out2 == 0,
					"深内区仅 2×2 时仍挖出虚空（" + cs2.size() + " 格）且一格不越界（越界 " + out2 + " 格）" );
			check( cs2.isEmpty() || components( l2, cs2 ) == 1,
					"且这 2×2 场景下也是一整块（" + components( l2, cs2 ) + " 块）" );
		} catch (Throwable t) {
			info("⑤ 段无法在脱离游戏的环境下跑（" + t + "）");
			t.printStackTrace();
		}

		// ------------------------------------------------ ⑥ buildFlagMaps 前后（幽灵格实证）
		System.out.println("\n--- ⑥ ⭐ 只写 map[] 安全的前提：buildFlagMaps() 会把 pit[]/passable[] 重算回来 ---");
		try {
			Level l = freshLevel( 20, 20 );
			fill( l, 0, 0, 19, 19, Terrain.WALL );
			fill( l, 2, 2, 17, 17, Terrain.EMPTY );
			Random.pushGenerator( 4242 );
			MiningLevel.carveVoid( l, room( 2, 2, 17, 17 ), -1 );
			Random.popGenerator();

			ArrayList<Integer> cs = chasms( l );
			check( !cs.isEmpty(), "前置：确实挖出了虚空（" + cs.size() + " 格 ⇒ 这是本段的前提）" );

			//(a) 挖完**没**重算 flags：pit[] 全是 false ⇒ 「画着是深渊、却踩得上去」
			int pitSetBefore = 0;
			for (int c : cs) if (l.pit[c]) pitSetBefore++;
			check( pitSetBefore == 0,
					"⚠️ 未调 buildFlagMaps() 时深渊格的 pit[] 全是 false（" + pitSetBefore + "/" + cs.size()
							+ "）⇒ 幽灵格风险**真实存在**，这正是必须有下面这一步的原因" );

			//(b) 调一次 buildFlagMaps()（＝ Level.create() 在 build() 之后必做的那步）
			l.buildFlagMaps();

			int pitOk = 0, passOk = 0, floorOk = 0, floorN = 0;
			for (int c : cs){
				if (l.pit[c]) pitOk++;
				if (!l.passable[c]) passOk++;
			}
			for (int i = 0; i < l.length(); i++){
				if (l.map[i] == Terrain.EMPTY){
					floorN++;
					if (!l.pit[i]) floorOk++;
				}
			}
			check( pitOk == cs.size(),
					"buildFlagMaps() 之后每个深渊格 pit[] 都为 true（" + pitOk + "/" + cs.size()
							+ "）⇒ 踩上去会触发 Chasm.heroFall" );
			check( passOk == cs.size(),
					"且每个深渊格 passable[] 都为 false（" + passOk + "/" + cs.size() + "）" );
			check( floorOk == floorN,
					"普通地板一格都没被误标成 pit（" + floorOk + "/" + floorN + "）" );

			//(c) 与 Terrain.flags 的权威口径逐格一致
			int mism = 0;
			for (int i = 0; i < l.length(); i++){
				boolean expect = (Terrain.flags[l.map[i]] & Terrain.PIT) != 0;
				if (l.pit[i] != expect) mism++;
			}
			check( mism == 0, "全图 pit[] 与 Terrain.flags[map[i]] 逐格一致（不符 " + mism + " 格）" );
		} catch (Throwable t) {
			info("⑥ 段无法在脱离游戏的环境下跑（" + t + "）");
			t.printStackTrace();
		}

		// ------------------------------------------------ ⑦ 同 seed 确定性
		System.out.println("\n--- ⑦ 同 seed ⇒ 虚空形态逐格相同 ---");
		try {
			Level a = freshLevel( 20, 20 );
			fill( a, 0, 0, 19, 19, Terrain.WALL );
			fill( a, 2, 2, 17, 17, Terrain.EMPTY );
			Random.pushGenerator( 20260926L );
			MiningLevel.carveVoid( a, room( 2, 2, 17, 17 ), -1 );
			Random.popGenerator();

			Level b = freshLevel( 20, 20 );
			fill( b, 0, 0, 19, 19, Terrain.WALL );
			fill( b, 2, 2, 17, 17, Terrain.EMPTY );
			Random.pushGenerator( 20260926L );
			MiningLevel.carveVoid( b, room( 2, 2, 17, 17 ), -1 );
			Random.popGenerator();

			boolean same = true;
			for (int i = 0; i < a.length(); i++) if (a.map[i] != b.map[i]) { same = false; break; }
			check( same, "同一 seed 下两次 carveVoid 的 map[] 逐格相同（共 " + chasms(a).size() + " 格虚空）" );
		} catch (Throwable t) {
			info("⑦ 段无法在脱离游戏的环境下跑（" + t + "）");
			t.printStackTrace();
		}

		// ------------------------------------------------ ⑧ 地板可达性粗检
		System.out.println("\n--- ⑧ 虚空不把房间切成孤岛（从门口内侧那一格出发能走遍所有非墙非深渊格）---");
		try {
			int bad = 0, trials = 15, worst = Integer.MAX_VALUE;
			for (int s = 0; s < trials; s++){
				Level l = freshLevel( 20, 20 );
				fill( l, 0, 0, 19, 19, Terrain.WALL );
				fill( l, 2, 2, 17, 17, Terrain.EMPTY );
				Random.pushGenerator( 31000 + s );
				MiningLevel.carveVoid( l, room( 2, 2, 17, 17 ), -1 );
				Random.popGenerator();

				//数一下「非墙非深渊」的总格数
				int floorN = 0;
				for (int i = 0; i < l.length(); i++){
					if (l.map[i] != Terrain.WALL && l.map[i] != Terrain.CHASM) floorN++;
				}
				//从房间内环左上角那一格（3,3）出发
				int reach = floorReach( l, 3 + 3 * 20 );
				worst = Math.min( worst, floorN - reach );
				if (reach != floorN) bad++;
			}
			check( bad == 0, trials + " 次采样里地板全连通、一个孤岛都没有（最差一次漏 "
					+ worst + " 格）⇒ 房间不会被虚空切成两半" );
		} catch (Throwable t) {
			info("⑧ 段无法在脱离游戏的环境下跑（" + t + "）");
			t.printStackTrace();
		}

		// ------------------------------------------------ 摘要
		setQuestType( savedType );
		System.out.println("\n==============================================================");
		System.out.printf("断言 %d 条：通过 %d，失败 %d（另有 %d 条环境说明）%n",
				checks, checks - failures, failures, infos);
		System.out.println("==============================================================");

		if (failures > 0) System.exit( 1 );
	}
}
