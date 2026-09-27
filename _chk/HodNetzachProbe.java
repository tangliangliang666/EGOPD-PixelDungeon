// 考验 HOD（荣耀）/ NETZACH（胜利）的行为探针：真调真方法，打印实测值 + 断言。
//
// 覆盖七段：
//   ① 区域换算与倍率（深度 1..26 ⇒ region / multiplier）
//   ② 命中 / 闪避倍率的门控（只认敌方；未开考验恒 1）
//   ③ 取数收口 finalAccuracy / finalEvasion（= 自身属性 × 考验倍率）
//   ④ enemyFade 的距离阈值（≤1 / 2~3 / >3；英雄与盟友恒 1）
//   ⑤ grantHodElite 的「不可叠加」
//   ⑥ HodGlory 计时器的边界（150 不给 / 151 给 + 自摘；不再适用即清零）
//   ⑦ randomChampionClass 的取值域（恰六种，且都是 ChampionEnemy 子类）
//
// 编译运行见 _chk/_build_hod_netzach.sh（classpath 必须带 gdx 与 gdx-controllers：
// Buff.affect 走 com.watabou.utils.Reflection ⇒ 会连带加载 gdx 的 ClassReflection）。
//
// headless 注意事项（踩过的坑）：
//   · com.watabou.noosa.Game.version 必须先赋值（某些 <clinit> 会读它）；
//   · new Hero() 在 headless 下可以跑（构造器只做 HP/HT/STR 与 belongings）；
//   · Mob 的子类只要有具体 act() 即可实例化（Mob.act 是具体方法），
//     自带实例块会把 alignment 置 ENEMY；
//   · 自己造 Level 要手工填 width/height/length（字段是 protected，子类可直接写）。

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Trials;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HodGlory;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.watabou.noosa.Game;

import java.util.HashSet;
import java.util.Set;

public class HodNetzachProbe {

	static int fails = 0;
	static int total = 0;

	static void check( boolean cond, String label ){
		total++;
		if (!cond) fails++;
		System.out.println( (cond ? "  [OK]   " : "  [FAIL] ") + label );
	}

	static void eq( Object got, Object want, String label ){
		check( String.valueOf(got).equals(String.valueOf(want)),
				label + "（实测 " + got + "，期望 " + want + "）" );
	}

	static void section( String name, Runnable r ){
		System.out.println( "\n" + name );
		try {
			r.run();
		} catch (Throwable t){
			fails++;
			total++;
			System.out.println( "  [INFO] 本段无法在脱离游戏的环境下跑：" + t );
		}
	}

	/** 探针怪：只关心 attackSkill / defenseSkill 两个数。
	 *  ⚠️ HP 必须手工给（真实流程里是 Mob.onAdd 按 HT 回算的），否则 isAlive() 为假、
	 *     HodGlory 每回合都会把计时清零；enemySeen 也要置真，否则 Mob.defenseSkill 会以
	 *     「被英雄偷袭」为由直接返回 0，测不出闪避倍率。 */
	static class ProbeMob extends Mob {
		{
			defenseSkill = 10;
			HP = HT = 10;
			enemySeen = true;
		}
		@Override public int attackSkill( Char target ){ return 20; }
		/** 打上 Boss 标记（{@code Char.Property.BOSS}），用来核验 HOD 的 Boss 阈值与普通阈值真的分流。 */
		ProbeMob markBoss(){ properties.add( Char.Property.BOSS ); return this; }
	}

	/** 探针关卡：只为给 Dungeon.level.distance 提供 width。 */
	static class ProbeLevel extends Level {
		{ width = 16; height = 16; length = 256; }
		@Override protected boolean build(){ return true; }
		@Override protected void createMobs(){}
		@Override protected void createItems(){}
	}

	public static void main( String[] args ){

		Game.version = "3.3.0";     // 见文件头「headless 注意事项」

		Dungeon.trials = 0;
		Dungeon.depth  = 1;
		Dungeon.hero   = null;
		Dungeon.level  = null;

		// ---------------------------------------------------------------- ①
		section( "① 区域换算与倍率（深度 1..26）", () -> {
			int[] wantRegion = {1,1,1,1,1, 2,2,2,2,2, 3,3,3,3,3, 4,4,4,4,4, 5,5,5,5,5, 5};
			boolean regionOK = true, multOK = true;
			StringBuilder bad = new StringBuilder();
			for (int d = 1; d <= 26; d++){
				Dungeon.depth = d;
				int r = Trials.currentRegion();
				float m = Trials.regionBonusMultiplier();
				if (r != wantRegion[d-1]) { regionOK = false; bad.append(" d=").append(d).append("→").append(r); }
				float wantM = 1f + 0.10f * wantRegion[d-1];
				if (Math.abs(m - wantM) > 1e-6f) { multOK = false; bad.append(" m(d=").append(d).append(")=").append(m); }
			}
			check( regionOK, "深度→区域 映射正确（1-5/6-10/11-15/16-20/21-25 各一区，升天 26 封顶五区）" + bad );
			check( multOK, "倍率 = 1 + 0.10×区域（一区 1.10 …… 五区 1.50）" );
			Dungeon.depth = 25;
			eq( Trials.regionBonusMultiplier(), 1.50f, "五区（深度 25）倍率 = 1.50" );
			Dungeon.depth = 1;
			eq( Trials.regionBonusMultiplier(), 1.10f, "一区（深度 1）倍率 = 1.10" );
		} );

		// ---------------------------------------------------------------- ②
		section( "② 命中 / 闪避倍率的门控", () -> {
			ProbeMob mob = new ProbeMob();
			Hero hero = new Hero();
			Dungeon.hero = hero;
			Dungeon.depth = 6;                       // 二区 ⇒ 1.20

			Dungeon.trials = 0;
			eq( Trials.accuracyFactor( mob ), 1f, "未开考验：敌方命中倍率 = 1" );
			eq( Trials.evasionFactor( mob ),  1f, "未开考验：敌方闪避倍率 = 1" );
			eq( Trials.enemyFade( mob ),      1f, "未开考验：淡化系数 = 1" );

			Dungeon.trials = Trials.HOD;
			eq( Trials.accuracyFactor( mob ), 1.20f, "HOD 开：敌方（二区）命中倍率 = 1.20" );
			eq( Trials.accuracyFactor( hero ), 1f,   "HOD 开：英雄（非敌方）命中倍率 = 1" );
			eq( Trials.evasionFactor( mob ), 1f,     "HOD 开：闪避倍率不受影响 = 1" );

			Dungeon.trials = Trials.NETZACH;
			eq( Trials.evasionFactor( mob ), 1.20f, "NETZACH 开：敌方（二区）闪避倍率 = 1.20" );
			eq( Trials.evasionFactor( hero ), 1f,   "NETZACH 开：英雄（非敌方）闪避倍率 = 1" );
			eq( Trials.accuracyFactor( mob ), 1f,   "NETZACH 开：命中倍率不受影响 = 1" );

			Dungeon.trials = Trials.HOD | Trials.NETZACH;
			eq( Trials.accuracyFactor( mob ), 1.20f, "两条同开：命中倍率仍 1.20" );
			eq( Trials.evasionFactor( mob ), 1.20f, "两条同开：闪避倍率仍 1.20" );
		} );

		// ---------------------------------------------------------------- ③
		section( "③ 取数收口 finalAccuracy / finalEvasion", () -> {
			Dungeon.depth = 11;                       // 三区 ⇒ 1.30
			ProbeMob mob = new ProbeMob();
			Hero hero = new Hero();
			Dungeon.hero = hero;

			Dungeon.trials = 0;
			eq( Trials.finalAccuracy( mob, hero ), 20f,  "未开考验：敌方命中取数 = 自身 20" );
			eq( Trials.finalEvasion( hero, mob ), (float) hero.defenseSkill( mob ),
					"未开考验：英雄闪避取数 = 其自身 defenseSkill（原样不动）" );

			Dungeon.trials = Trials.HOD;
			eq( Trials.finalAccuracy( mob, hero ), 26f,  "HOD 开：敌方命中取数 = 20 × 1.30 = 26" );

			Dungeon.trials = Trials.NETZACH;
			eq( Trials.finalEvasion( mob, hero ), 13f,   "NETZACH 开：敌方闪避取数 = 10 × 1.30 = 13" );
			eq( Trials.finalAccuracy( mob, hero ), 20f,  "NETZACH 开：命中取数不受影响 = 20" );
		} );

		// ---------------------------------------------------------------- ④
		section( "④ enemyFade 的距离阈值", () -> {
			ProbeLevel lv = new ProbeLevel();
			Dungeon.level = lv;
			Hero hero = new Hero();
			Dungeon.hero = hero;
			hero.pos = 0;

			ProbeMob mob = new ProbeMob();
			mob.pos = 0;
			Dungeon.trials = Trials.NETZACH;

			mob.pos = 0; check( Trials.enemyFade( mob ) == 1f,    "距离 0 ⇒ 完全不透明" );
			mob.pos = 1; check( Trials.enemyFade( mob ) == 1f,    "距离 1 ⇒ 完全不透明（阈值含 1）" );
			mob.pos = 2; check( Trials.enemyFade( mob ) == 0.5f,  "距离 2 ⇒ 半透明 0.5" );
			mob.pos = 3; check( Trials.enemyFade( mob ) == 0.5f,  "距离 3 ⇒ 半透明 0.5（阈值含 3）" );
			mob.pos = 4; check( Trials.enemyFade( mob ) == 0f,    "距离 4 ⇒ 完全不可见 0" );
			mob.pos = 5; check( Trials.enemyFade( mob ) == 0f,    "距离 5 ⇒ 完全不可见 0" );
			// 切比雪夫距离：dx=2, dy=2 ⇒ max=2 ⇒ 落在中间带
			mob.pos = 2*16+2; check( Trials.enemyFade( mob ) == 0.5f, "切比雪夫 (dx,dy)=(2,2) ⇒ 距离 2 ⇒ 半透明 0.5" );
			// dx=5, dy=0 ⇒ 距离 5 ⇒ 全透明
			mob.pos = 5; check( Trials.enemyFade( mob ) == 0f,    "切比雪夫 (dx,dy)=(5,0) ⇒ 距离 5 ⇒ 全透明" );

			eq( Trials.enemyFade( hero ), 1f, "英雄自身恒 1（不受影响）" );

			Dungeon.trials = 0;
			mob.pos = 5;
			eq( Trials.enemyFade( mob ), 1f, "未开考验：再远也恒 1" );
		} );

		// ---------------------------------------------------------------- ⑤
		section( "⑤ grantHodElite 的「不可叠加」", () -> {
			Hero hero = new Hero();
			Dungeon.hero = hero;
			Dungeon.depth = 1;

			Dungeon.trials = 0;
			ProbeMob m0 = new ProbeMob();
			check( !Trials.grantHodElite( m0 ), "未开考验 ⇒ 不发" );
			check( m0.buffs( ChampionEnemy.class ).isEmpty(), "未开考验 ⇒ 身上没有精英" );

			Dungeon.trials = Trials.HOD;
			ProbeMob m1 = new ProbeMob();
			check( Trials.grantHodElite( m1 ), "首次 ⇒ 发下一个精英" );
			eq( m1.buffs( ChampionEnemy.class ).size(), 1, "身上恰好 1 个精英" );
			check( !Trials.grantHodElite( m1 ), "再次调用 ⇒ 拒绝（不可叠加）" );
			eq( m1.buffs( ChampionEnemy.class ).size(), 1, "仍是恰好 1 个精英" );

			ProbeMob ally = new ProbeMob();
			ally.alignment = Char.Alignment.ALLY;
			check( !Trials.grantHodElite( ally ), "非敌方阵营的怪（盟友）⇒ 不发" );
			check( !Trials.grantHodElite( null ), "null ⇒ 不发" );

			// 预置一个精英（模拟挑战刷出来的），再调 HOD ⇒ 不得叠加
			ProbeMob m2 = new ProbeMob();
			Buff.affect( m2, ChampionEnemy.Blazing.class );
			eq( m2.buffs( ChampionEnemy.class ).size(), 1, "预置 Blazing ⇒ 1 个" );
			check( !Trials.grantHodElite( m2 ), "已有精英（挑战刷的）⇒ HOD 拒绝叠加" );
			eq( m2.buffs( ChampionEnemy.class ).size(), 1, "仍是 1 个（没变成 2 个）" );
		} );

		// ---------------------------------------------------------------- ⑥
		section( "⑥ HodGlory 计时器的边界", () -> {
			Hero hero = new Hero();
			Dungeon.hero = hero;

			Dungeon.trials = Trials.HOD;
			ProbeMob m = new ProbeMob();
			check( Buff.affect( m, HodGlory.class ).type == Buff.buffType.POSITIVE,
					"HodGlory 的 type 是 POSITIVE（绝不能是 NEGATIVE，否则全图怪物会被惊醒）" );

			check( Trials.HOD_ELITE_TURNS == 300, "普通怪阈值 HOD_ELITE_TURNS = 300" );
			check( Trials.HOD_ELITE_TURNS_BOSS == 450, "Boss 阈值 HOD_ELITE_TURNS_BOSS = 450" );
			check( Trials.hodEliteTurns( new ProbeMob() ) == 300, "hodEliteTurns(普通怪) = 300" );

			for (int i = 0; i < Trials.HOD_ELITE_TURNS; i++) m.buff( HodGlory.class ).act();
			check( m.buffs( ChampionEnemy.class ).isEmpty(),
					"普通怪恰好 300 回合 ⇒ 还不发（需求是「超过 300」）" );

			m.buff( HodGlory.class ).act();
			eq( m.buffs( ChampionEnemy.class ).size(), 1, "第 301 回合 ⇒ 发下 1 个精英" );
			check( m.buff( HodGlory.class ) == null, "发完即自摘（本 buff 不再计时）" );

			// Boss 阈值 450：数到普通怪的 300 仍不发，数满 450 才发 —— 证明两档真的分流
			ProbeMob boss = new ProbeMob().markBoss();
			check( Trials.hodEliteTurns( boss ) == 450, "hodEliteTurns(Boss) = 450（按 Char.Property.BOSS 分流）" );
			HodGlory gb = Buff.affect( boss, HodGlory.class );
			for (int i = 0; i < Trials.HOD_ELITE_TURNS; i++) gb.act();
			check( boss.buffs( ChampionEnemy.class ).isEmpty(),
					"Boss 数了 300 回合（＝普通怪阈值）⇒ 仍不发" );
			for (int i = 0; i < Trials.HOD_ELITE_TURNS_BOSS - Trials.HOD_ELITE_TURNS; i++) gb.act();
			check( boss.buffs( ChampionEnemy.class ).isEmpty(), "Boss 恰好 450 回合 ⇒ 还不发（需求是「超过 450」）" );
			gb.act();
			eq( boss.buffs( ChampionEnemy.class ).size(), 1, "第 451 回合 ⇒ Boss 发下 1 个精英" );
			check( boss.buff( HodGlory.class ) == null, "Boss 发完即自摘" );

			// 不再适用 ⇒ 计时清零：先在「非敌方」下空转，再回敌方，阈值内不应触发
			ProbeMob m2 = new ProbeMob();
			HodGlory g2 = Buff.affect( m2, HodGlory.class );
			m2.alignment = Char.Alignment.ALLY;
			for (int i = 0; i < 400; i++) g2.act();
			check( m2.buffs( ChampionEnemy.class ).isEmpty(), "非敌方阵营空转 400 回合 ⇒ 一次也不发（远超 300 阈值）" );
			m2.alignment = Char.Alignment.ENEMY;
			for (int i = 0; i < Trials.HOD_ELITE_TURNS; i++) g2.act();
			check( m2.buffs( ChampionEnemy.class ).isEmpty(), "回到敌方后只数了 300 回合 ⇒ 仍未发（说明计数确实被清零过）" );
			g2.act();
			eq( m2.buffs( ChampionEnemy.class ).size(), 1, "第 301 回合 ⇒ 发下精英" );

			// 关掉考验 ⇒ 计时停摆，永不发
			Dungeon.trials = 0;
			ProbeMob m3 = new ProbeMob();
			HodGlory g3 = Buff.affect( m3, HodGlory.class );
			for (int i = 0; i < 500; i++) g3.act();
			check( m3.buffs( ChampionEnemy.class ).isEmpty(), "未开考验空转 500 回合 ⇒ 一次也不发" );
		} );

		// ---------------------------------------------------------------- ⑦
		section( "⑦ randomChampionClass 的取值域", () -> {
			Set<Class<?>> got = new HashSet<>();
			boolean allSub = true;
			for (int i = 0; i < 600; i++){
				Class<?> c = ChampionEnemy.randomChampionClass();
				got.add( c );
				if (!ChampionEnemy.class.isAssignableFrom( c )) allSub = false;
			}
			eq( got.size(), 6, "600 次抽取恰好覆盖 6 种精英" );
			check( allSub, "抽出来的都是 ChampionEnemy 的子类" );
			Set<Class<?>> want = new HashSet<>();
			want.add( ChampionEnemy.Blazing.class );
			want.add( ChampionEnemy.Projecting.class );
			want.add( ChampionEnemy.AntiMagic.class );
			want.add( ChampionEnemy.Giant.class );
			want.add( ChampionEnemy.Blessed.class );
			want.add( ChampionEnemy.Growing.class );
			check( got.equals( want ), "取值域 = 原版六种（Blazing/Projecting/AntiMagic/Giant/Blessed/Growing）" );
		} );

		// ---------------------------------------------------------------- 收尾
		System.out.println( "\n============================================================" );
		if (fails == 0){
			System.out.println( "全部符合预期（" + total + " 条断言）。" );
		} else {
			System.out.println( "有 " + fails + " 项与预期不符（共 " + total + " 条）。" );
		}
		System.out.println( "============================================================" );
	}
}
