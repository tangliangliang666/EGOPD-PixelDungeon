// 考验 YESOD（根基）的行为探针：真调真方法，打印实测值 + 断言。
//
// 覆盖七段：
//   ① 未知贴图常量与取样矩形（ItemSpriteSheet.UNKNOWN_ITEM == 31 = xy(16,2)，film 10×15）
//   ② naturalIdDisabled() 门控（只看 YESOD 位）
//   ③ hidesGroundHeap() 真值表（Heap.Type 全部 8 种取值都中；空堆/null 不中；关掉考验全不中）
//   ④ Heap.title() 隐名（HEAP 与容器都变未知文本；FOR_SALE 的价签保持不变）  ← 软段（可能受环境限制）
//   ⑤ unknownGroundText() 非空                                            ← 软段（可能受环境限制）
//   ⑥ NETZACH 淡出阈值（1 格内 1.0 / 2~3 格 0.5 / >3 格 0；英雄与盟友恒 1）
//   ⑦ 鉴定闸门实跑：Weapon.proc 数满后，YESOD 开则不鉴定、关则鉴定（同一路径对照）
//
// 编译运行见 _chk/_build_yesod.sh（classpath 必须带 gdx 与 gdx-controllers）。
//
// headless 注意事项（本次新增的坑）：
//   · com.watabou.noosa.Game.version 必须先赋值；
//   · new Hero() 在 headless 下可以跑；自己造 Level 要手工填 width/height/length；
//   · ⚠️ **Messages 无法初始化**：Messages.<clinit> 会读 SPDSettings.language()，后者依赖
//     Gdx 的 Preferences ⇒ 任何走 Messages 的字符串（Item.title / Heap.title / 语言包）
//     在纯 classpath 环境下必然抛异常。语言相关内容改由 _chk/verify_yesod.py 直接读
//     .properties 校验（那才是权威来源），故 ④⑤ 两段是「软段」：跑不了只打 INFO，不记 FAIL。
//   · ⚠️ ItemSpriteSheet.Icons 的 <clinit> 需要 GL（TextureCache → Gdx.files）⇒ 很多
//     物品子类的构造器会连带炸掉（如 RingOfWealth）。所以 ⑦ 用不碰 Icons 的 Dagger。
//   · Weapon.proc 在「武器无附魔、攻击者身上没有任何树脂/圣光 buff」时不会碰 defender.damage，
//     是这条链上最轻的一条路；usesLeftToID 是 protected，用反射直接置 0。

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Trials;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.Game;

public class YesodProbe {

	static int fails = 0;
	static int total = 0;
	static int softFails = 0;

	static void check( boolean cond, String label ){
		total++;
		if (!cond) fails++;
		System.out.println( (cond ? "  [OK]   " : "  [FAIL] ") + label );
	}

	static void eq( Object got, Object want, String label ){
		check( String.valueOf(got).equals(String.valueOf(want)),
				label + "（实测 " + got + "，期望 " + want + "）" );
	}

	static void near( float got, float want, String label ){
		check( Math.abs(got - want) < 1e-6f, label + "（实测 " + got + "，期望 " + want + "）" );
	}

	/** 硬段：跑不了一律记 FAIL。 */
	static void section( String name, Runnable r ){
		System.out.println( "\n" + name );
		try {
			r.run();
		} catch (Throwable t){
			fails++;
			total++;
			System.out.println( "  [FAIL] 本段无法在脱离游戏的环境下跑：" + t );
		}
	}

	/** 软段：受 headless 环境限制（Messages / GL）跑不了时只提示，不计 FAIL，同样不计入断言总数。 */
	static void softSection( String name, Runnable r ){
		System.out.println( "\n" + name );
		try {
			r.run();
		} catch (Throwable t){
			softFails++;
			System.out.println( "  [INFO] 本段在脱离游戏的环境下跑不了（不记 FAIL）：" + t );
		}
	}

	/** 探针怪：只关心 alignment / pos。 */
	static class ProbeMob extends Mob {
		{ HP = HT = 10; enemySeen = true; }
	}

	/** 探针关卡：只为给 Dungeon.level.distance 提供 width/height。 */
	static class ProbeLevel extends Level {
		{ width = 16; height = 16; length = 256; }
		@Override protected boolean build(){ return true; }
		@Override protected void createMobs(){}
		@Override protected void createItems(){}
	}

	static Heap heapOf( Heap.Type type, Object item ){
		Heap h = new Heap();
		h.type = type;
		h.pos = 40;
		if (item != null) h.items.add( (com.shatteredpixel.shatteredpixeldungeon.items.Item) item );
		return h;
	}

	/** usesLeftToID 是 Weapon 的 protected 字段，反射置 0 = 「已经用够了」。
	 *  包一层 RuntimeException：探针段是 Runnable，声明不了受检异常。 */
	static void exhaustUsesToID( Weapon w ) {
		try {
			java.lang.reflect.Field f = Weapon.class.getDeclaredField( "usesLeftToID" );
			f.setAccessible( true );
			f.setFloat( w, 0f );
		} catch (Exception e){
			throw new RuntimeException( e );
		}
	}

	public static void main( String[] args ){

		Game.version = "3.3.0";

		Dungeon.trials = 0;
		Dungeon.depth  = 1;
		Dungeon.hero   = null;
		Dungeon.level  = null;

		// ---------------------------------------------------------------- ①
		section( "① 未知贴图常量与取样矩形", () -> {
			eq( ItemSpriteSheet.UNKNOWN_ITEM, 31, "UNKNOWN_ITEM == 31（= xy(16,2)，第2行第16列）" );
			near( ItemSpriteSheet.film.width( ItemSpriteSheet.UNKNOWN_ITEM ), 10f, "取样矩形宽 = 10" );
			near( ItemSpriteSheet.film.height( ItemSpriteSheet.UNKNOWN_ITEM ), 15f, "取样矩形高 = 15" );
			eq( ItemSpriteSheet.GEO_BOULDER, 28, "邻位 GEO_BOULDER（占 28）与 31 不冲突" );
			eq( ItemSpriteSheet.CHEST, 36, "下一行 CONTAINERS = xy(1,3) = 32，CHEST = +4 = 36（29/30/31 是空号）" );
		} );

		// ---------------------------------------------------------------- ②
		section( "② naturalIdDisabled() 门控", () -> {
			Dungeon.trials = 0;
			check( !Trials.naturalIdDisabled(), "未开考验 ⇒ false" );
			Dungeon.trials = Trials.NETZACH;
			check( !Trials.naturalIdDisabled(), "只开 NETZACH ⇒ false（各考验互不牵连）" );
			Dungeon.trials = Trials.YESOD;
			check( Trials.naturalIdDisabled(), "开 YESOD ⇒ true" );
			Dungeon.trials = Trials.YESOD | Trials.HOD | Trials.GEBURA;
			check( Trials.naturalIdDisabled(), "YESOD 与其它考验同时开 ⇒ 仍 true" );
			Dungeon.trials = 0;
		} );

		// ---------------------------------------------------------------- ③
		section( "③ hidesGroundHeap() 真值表", () -> {
			Heap plain    = heapOf( Heap.Type.HEAP,         new Gold(10) );
			Heap forSale  = heapOf( Heap.Type.FOR_SALE,     new Gold(10) );
			Heap chest    = heapOf( Heap.Type.CHEST,        new Gold(10) );
			Heap locked   = heapOf( Heap.Type.LOCKED_CHEST, new Gold(10) );
			Heap crystal  = heapOf( Heap.Type.CRYSTAL_CHEST,new Gold(10) );
			Heap tomb     = heapOf( Heap.Type.TOMB,         new Gold(10) );
			Heap skeleton = heapOf( Heap.Type.SKELETON,     new Gold(10) );
			Heap remains  = heapOf( Heap.Type.REMAINS,      new Gold(10) );
			Heap empty    = heapOf( Heap.Type.HEAP,         null );

			Dungeon.trials = 0;
			boolean anyTrue = false;
			for (Heap h : new Heap[]{ plain, forSale, chest, locked, crystal, tomb, skeleton, remains, empty }){
				if (Trials.hidesGroundHeap( h )) anyTrue = true;
			}
			check( !anyTrue, "关掉考验 ⇒ 任何堆都不隐藏（不改变旧版表现）" );
			check( !Trials.hidesGroundHeap( null ), "null 堆 ⇒ false（不 NPE）" );

			Dungeon.trials = Trials.YESOD;
			check( Trials.hidesGroundHeap( plain ),     "YESOD 开：普通掉落堆 HEAP ⇒ 隐藏" );
			check( Trials.hidesGroundHeap( forSale ),   "YESOD 开：商店货架 FOR_SALE ⇒ 隐藏（用户确认）" );
			check( Trials.hidesGroundHeap( chest ),     "YESOD 开：宝箱 CHEST ⇒ 隐藏（2026-09-26 用户追加）" );
			check( Trials.hidesGroundHeap( locked ),    "YESOD 开：上锁宝箱 ⇒ 隐藏" );
			check( Trials.hidesGroundHeap( crystal ),   "YESOD 开：水晶宝箱 ⇒ 隐藏" );
			check( Trials.hidesGroundHeap( tomb ),      "YESOD 开：坟墓 TOMB ⇒ 隐藏" );
			check( Trials.hidesGroundHeap( skeleton ),  "YESOD 开：骷髅堆 SKELETON ⇒ 隐藏" );
			check( Trials.hidesGroundHeap( remains ),   "YESOD 开：英雄遗骸 REMAINS ⇒ 隐藏" );
			check( !Trials.hidesGroundHeap( empty ),    "YESOD 开：空堆 ⇒ 不隐藏（无物品可藏）" );

			// 全 8 种 Heap.Type 都要中（枚举漏一种就是「那种容器照旧显形」的静默 bug）
			Heap[] all = new Heap[]{ plain, forSale, chest, locked, crystal, tomb, skeleton, remains };
			check( all.length == Heap.Type.values().length,
					"上面枚举的 8 个堆覆盖了 Heap.Type 的全部 " + Heap.Type.values().length + " 种取值" );
			String missed = "";
			for (Heap h : all) if (!Trials.hidesGroundHeap( h )) missed += h.type + " ";
			check( missed.isEmpty(), "YESOD 开：8 种类型**无一遗漏**（漏掉的是「" + missed.trim() + "」）" );

			// 金币也一样被藏（用户确认「金币也变问号」）
			Heap gold = heapOf( Heap.Type.HEAP, new Gold(99) );
			check( Trials.hidesGroundHeap( gold ), "YESOD 开：地上的金币堆 ⇒ 也隐藏（用户确认）" );
		} );

		// ---------------------------------------------------------------- ④
		softSection( "④ Heap.title() 隐名（依赖 Messages，见文件头）", () -> {
			Heap plain   = heapOf( Heap.Type.HEAP,     new Gold(10) );
			Heap forSale = heapOf( Heap.Type.FOR_SALE, new Gold(10) );
			Heap chest   = heapOf( Heap.Type.CHEST,    new Gold(10) );

			Dungeon.trials = 0;
			String plainVanilla = plain.title();
			String saleVanilla  = forSale.title();
			String chestVanilla = chest.title();

			Dungeon.trials = Trials.YESOD;
			check( !plain.title().equals( plainVanilla ),
					"YESOD 开：普通掉落堆的名字被换成未知文本（原为「" + plainVanilla + "」）" );
			eq( plain.title(), Trials.unknownGroundText(),
					"普通堆的 title 就是 unknownGroundText()（检视描述与列表标题同一口径）" );
			eq( forSale.title(), saleVanilla,
					"YESOD 开：货架 title 不变 —— 它是那条价签（用户要求价格正常显示）" );
			check( !chest.title().equals( chestVanilla ),
					"YESOD 开：容器的名字也被换掉（原为「" + chestVanilla + "」）" );
			eq( chest.title(), Trials.unknownGroundText(),
					"容器 title 与普通堆同一口径（检视窗里不会再出现「宝箱」这种字样）" );
		} );

		// ---------------------------------------------------------------- ⑤
		softSection( "⑤ 未知描述文本（依赖 Messages，见文件头）", () -> {
			String t = Trials.unknownGroundText();
			check( t != null && t.length() > 0, "unknownGroundText() 非空（长度 " + (t == null ? -1 : t.length()) + "）" );
			System.out.println( "   原文 = " + t );
		} );

		// ---------------------------------------------------------------- ⑥
		section( "⑥ NETZACH 淡出阈值（1 格 / 3 格）", () -> {
			Dungeon.level = new ProbeLevel();
			Hero hero = new Hero();
			hero.pos = 40;
			Dungeon.hero = hero;

			ProbeMob[] mobs = new ProbeMob[6];
			for (int d = 1; d <= 5; d++){
				mobs[d] = new ProbeMob();
				mobs[d].pos = 40 + d;          // 同一行 ⇒ 切比雪夫距离就是列差
			}

			Dungeon.trials = 0;
			boolean allOne = true;
			for (int d = 1; d <= 5; d++) if (Trials.enemyFade( mobs[d] ) != 1f) allOne = false;
			check( allOne, "关掉考验 ⇒ 所有距离都不淡化" );

			Dungeon.trials = Trials.NETZACH;
			near( Trials.enemyFade( mobs[1] ), 1.0f, "距离 1 格 ⇒ 1.0（完全不透明）" );
			near( Trials.enemyFade( mobs[2] ), 0.5f, "距离 2 格 ⇒ 0.5（半透明）" );
			near( Trials.enemyFade( mobs[3] ), 0.5f, "距离 3 格 ⇒ 0.5（半透明）" );
			near( Trials.enemyFade( mobs[4] ), 0.0f, "距离 4 格 ⇒ 0.0（完全不可见）" );
			near( Trials.enemyFade( mobs[5] ), 0.0f, "距离 5 格 ⇒ 0.0" );
			eq( Trials.NETZACH_FADE_NEAR, 1, "常量 NETZACH_FADE_NEAR == 1" );
			eq( Trials.NETZACH_FADE_FAR,  3, "常量 NETZACH_FADE_FAR == 3" );

			near( Trials.enemyFade( hero ), 1f, "英雄自己恒 1.0" );
			ProbeMob ally = new ProbeMob();
			ally.alignment = Char.Alignment.ALLY;
			ally.pos = 44;
			near( Trials.enemyFade( ally ), 1f, "盟友（非敌方）恒 1.0" );

			// 相邻性复核：距离 3 与 4 是「半透明 / 不可见」的分界，别写成 4 格才变淡
			Dungeon.trials = Trials.NETZACH | Trials.YESOD;
			near( Trials.enemyFade( mobs[3] ), 0.5f, "YESOD 同时开不影响 NETZACH 的阈值" );
			Dungeon.trials = 0;
		} );

		// ---------------------------------------------------------------- ⑦
		section( "⑦ 鉴定闸门实跑（Weapon.proc 数满）", () -> {
			Hero hero = new Hero();
			hero.HP = hero.HT = 20;
			Dungeon.hero = hero;

			ProbeMob target = new ProbeMob();

			// 被测：YESOD 开 → 用够次数后不再自动鉴定
			Dagger blocked = new Dagger();
			hero.belongings.weapon = blocked;
			check( !blocked.isIdentified(), "前置：新武器本来未鉴定" );
			exhaustUsesToID( blocked );
			Dungeon.trials = Trials.YESOD;
			blocked.proc( hero, target, 1 );
			check( !blocked.isIdentified(), "YESOD 开：武器用够次数后**没有**被自动鉴定" );
			check( blocked.readyToIdentify(), "YESOD 开：只被标记为「用够了」（readyToIdentify）" );
			check( !blocked.levelKnown, "YESOD 开：levelKnown 仍为 false（等级未泄露）" );

			// 对照：关掉考验 → 同一路径确实会鉴定（证明闸门是唯一变量）
			Dagger control = new Dagger();
			hero.belongings.weapon = control;
			exhaustUsesToID( control );
			Dungeon.trials = 0;
			Throwable controlErr = null;
			try {
				control.proc( hero, target, 1 );
			} catch (Throwable t){
				controlErr = t;
			}
			if (controlErr == null){
				check( control.isIdentified(), "对照：关掉 YESOD 后同一条路径确实会鉴定" );
			} else {
				// headless 下 Messages 初始化不了 ⇒ identify() 之后那句 GLog 会炸；但 identify() 本体
				// 已经跑完（levelKnown 置真）⇒ 仍能证明「走的是鉴定那一支，而不是守卫那一支」。
				check( control.levelKnown,
						"对照：关掉 YESOD 后同一条路径确实走进了 identify()（headless 由 levelKnown 旁证；"
								+ "异常 " + controlErr + "）" );
			}

			Dungeon.trials = 0;
		} );

		System.out.println( "\n== YesodProbe 结果：OK " + (total - fails) + " / FAIL " + fails
				+ "（断言共 " + total + "；另有 " + softFails + " 个软段因环境限制跳过）==" );
		if (fails > 0) System.exit(1);
	}
}
