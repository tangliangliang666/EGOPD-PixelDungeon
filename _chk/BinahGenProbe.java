// 考验 BINAH（理解）「生成时等级规则」的行为真值表探针（纯读，不改游戏代码）
//
// 为什么要有它：verify_binah_gen.py 证明的是「收口唯一、顺序正确、字节码里没 Random」——
// 全是**结构**证据。但「(等级, 诅咒) → 结果等级」这张表到底对不对，只有**真构造道具、真调钩子、
// 真读等级**才算数。本探针就是这个：把三层断言的结论钉到行为上。
//
// 另外实证两条容易被想当然的性质：
//   ① 钩子**不消耗随机数**：同一 seed 下，中间穿插钩子调用与不穿插，取出的随机序列必须逐位相同
//      （这是「关卡种子布局不漂移」的直接证据，不是靠读源码推的）。
//   ② 钩子是**生成时**规则、不是全局规则：钩子跑完之后再 upgrade() 照样生效，不会被再夹一次。
//
// 编译/运行：见 skill egopd-source-verify §1；classpath 必须把 _chk/_javachk 排在最前
//（新编的 Trials.modifyGeneratedItem / final Item.random 在那里），再 core/SPD-classes/services + gdx + gdx-controllers。
// 说明：脱离游戏没有 Gdx.files，戒指类的实例初始化块引用贴图帧，故用 Unsafe.allocateInstance 绕过。
import java.lang.reflect.Field;

import sun.misc.Unsafe;

import com.watabou.utils.Random;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Trials;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;

public class BinahGenProbe {

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

	/** 造一件「已生成」的道具：设好等级与诅咒，但**不**调钩子。 */
	static Item mk( String kind, int lvl, boolean cursed ) throws Exception {
		Item it;
		switch (kind){
			case "武器":  it = new Shortsword();        break;
			case "护甲":  it = new LeatherArmor();      break;
			case "法杖":  it = new WandOfMagicMissile();break;
			case "投掷":  it = new ThrowingKnife();     break;
			case "戒指":  it = newRingBypass();         break;
			default: throw new IllegalArgumentException(kind);
		}
		it.level( lvl );
		it.cursed = cursed;
		it.cursedKnown = cursed;
		return it;
	}

	static Ring newRingBypass() throws Exception {
		if (unsafe == null){
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			unsafe = (Unsafe) f.get(null);
		}
		return (Ring) unsafe.allocateInstance(RingOfMight.class);
	}

	/** 走一遍「生成」：Item.random() 的收口就是这一个调用。返回最终等级。 */
	static int generate( Item it ){
		Trials.modifyGeneratedItem( it );
		return it.trueLevel();
	}

	public static void main( String[] args ) throws Exception {

		say("==============================================================");
		say("考验 BINAH 生成时等级规则 · 行为真值表探针");
		say("==============================================================");

		int savedTrials = Dungeon.trials;
		Dungeon.trials = 0;

		//--------------------------------------------------------------
		say("");
		say("--- ① 不开 BINAH：一切照旧（回归基线）---");
		Dungeon.trials = 0;
		check( Dungeon.isTrialled( Trials.BINAH ) == false, "Dungeon.trials=0 时 isTrialled(BINAH) 为 false" );
		int[][] base = { {0,0}, {1,0}, {2,0}, {3,0}, {1,1}, {2,1}, {3,1} };
		for (int[] b : base){
			for (String kind : new String[]{"武器","护甲","法杖","投掷","戒指"}){
				Item it = mk( kind, b[0], b[1] == 1 );
				int got = generate( it );
				check( got == b[0], String.format("不开：%s 等级 %+d 诅咒=%s -> %+d（期望 %+d）",
						kind, b[0], b[1] == 1, got, b[0]) );
			}
		}

		//--------------------------------------------------------------
		say("");
		say("--- ② 开 BINAH：真值表 ---");
		Dungeon.trials = Trials.BINAH;
		check( Dungeon.isTrialled( Trials.BINAH ), "Dungeon.trials=BINAH 时 isTrialled(BINAH) 为 true" );

		// {生成时等级, 是否诅咒, 期望结果}
		// 顺序语义：先「因诅咒反转」再「正等级归零」⇒ 诅咒的取负、不诅咒的归零、0 取负仍是 0。
		int[][] table = {
				{ 0, 0,  0 },   // 未诅咒 +0 -> 0
				{ 0, 1,  0 },   // 诅咒 +0 -> 0  ★用户备注点名的这一格
				{ 1, 0,  0 },   // 未诅咒 +1 -> 0（不带正等级）
				{ 2, 0,  0 },
				{ 3, 0,  0 },
				{ 1, 1, -1 },   // 诅咒 +1 -> -1
				{ 2, 1, -2 },
				{ 3, 1, -3 },
		};
		String[] kinds = { "武器", "护甲", "法杖", "投掷", "戒指" };
		for (int[] c : table){
			StringBuilder sb = new StringBuilder();
			for (String kind : kinds){
				Item it = mk( kind, c[0], c[1] == 1 );
				int got = generate( it );
				check( got == c[2], String.format("开：%s 生成时 %+d 诅咒=%-5s -> %+d（期望 %+d）",
						kind, c[0], c[1] == 1, got, c[2]) );
				sb.append(String.format("%s=%+d ", kind, got));
			}
			info( String.format("表格行 %+d/诅咒=%s 五类实得：%s", c[0], c[1] == 1, sb.toString().trim()) );
		}

		// 恒等式交叉检查：诅咒列的绝对值 == 生成时等级；未诅咒列恒为 0
		boolean ident = true;
		for (int[] c : table){
			if (c[1] == 1) ident &= (c[2] == -c[0]);
			else           ident &= (c[2] == 0);
		}
		check( ident, "恒等式：诅咒行结果 == -生成时等级；未诅咒行 == 0" );

		//--------------------------------------------------------------
		say("");
		say("--- ③ 法杖派生状态：压到 0 后 maxCharges 必须跟着重算 ---");
		// 证明走的是 level(int) -> Wand.updateLevel()，不是只改字段了事
		Wand w = (Wand) mk( "法杖", 2, false );
		int init = w.initialCharges();
		w.updateLevel();
		int before = w.maxCharges;
		int after = generate( w );
		check( w.maxCharges == init, String.format("压平后 maxCharges 回到 initialCharges（%d -> %d，期望 %d）",
				before, w.maxCharges, init) );
		check( after == 0, "法杖最终等级为 0（实得 " + after + "）" );
		check( w.curCharges <= w.maxCharges, "curCharges 被夹到 maxCharges 之内（"
				+ w.curCharges + " <= " + w.maxCharges + "）" );

		Wand wc = (Wand) mk( "法杖", 3, true );
		int neg = generate( wc );
		check( neg == -3, "诅咒法杖 +3 -> -3（实得 " + neg + "）" );

		//--------------------------------------------------------------
		say("");
		say("--- ④ 这是「生成时」规则、不是全局规则 ---");
		Item a = mk( "武器", 3, false );
		check( generate( a ) == 0, "生成时 +3 未诅咒 -> 0" );
		a.upgrade( 2 );
		check( a.trueLevel() == 2, "钩子跑完之后再 upgrade(2) -> +2，不会被再夹一次（实得 "
				+ a.trueLevel() + "）" );
		Item b = mk( "护甲", 2, true );
		check( generate( b ) == -2, "生成时 +2 诅咒 -> -2" );
		b.degrade( 1 );
		check( b.trueLevel() == -3, "之后再 degrade(1) -> -3（实得 " + b.trueLevel() + "）" );

		//--------------------------------------------------------------
		say("");
		say("--- ⑤ 白名单：四种「任务 NPC 赠礼」的压平 ---");
		// 压平只改等级、不动诅咒
		Item imp = mk( "戒指", 2, true );
		Trials.flattenQuestReward( imp );
		check( imp.trueLevel() == 0, "小恶魔戒指 +2（诅咒）压平 -> 0（实得 " + imp.trueLevel() + "）" );
		check( imp.cursed, "压平**不动**诅咒：仍是诅咒（小恶魔的戒指本就该被诅咒）" );

		Item ghost = mk( "武器", 3, false );
		Trials.flattenQuestReward( ghost );
		check( ghost.trueLevel() == 0, "幽灵武器 +3 压平 -> 0（实得 " + ghost.trueLevel() + "）" );

		Item wand2 = mk( "法杖", 1, false );
		Trials.flattenQuestReward( wand2 );
		check( wand2.trueLevel() == 0, "制杖人法杖 +1 压平 -> 0（实得 " + wand2.trueLevel() + "）" );

		Item smith = mk( "护甲", 0, false );
		Trials.flattenQuestReward( smith );
		check( smith.trueLevel() == 0, "铁匠 +0 压平 -> 0（已是 0 不动，实得 " + smith.trueLevel() + "）" );

		// 关掉之后必须完全不动
		Dungeon.trials = 0;
		Item off = mk( "戒指", 2, true );
		Trials.flattenQuestReward( off );
		check( off.trueLevel() == 2, "不开 BINAH 时压平是空操作（+2 保持，实得 " + off.trueLevel() + "）" );

		//--------------------------------------------------------------
		say("");
		say("--- ⑥ 钩子不消耗随机数 ⇒ 关卡种子布局不漂移 ---");
		// 同一 seed 下取 200 个随机数；第二轮在每次取样之间穿插钩子调用。
		// 两轮序列必须逐位相同 —— 这是「不碰 Random」的直接证据。
		final int N = 200;
		long[] seqPlain = new long[N];
		long[] seqHooked = new long[N];

		Random.pushGenerator( 0xC0FFEEL );
		for (int i = 0; i < N; i++) seqPlain[i] = Random.Long();
		Random.popGenerator();

		Dungeon.trials = Trials.BINAH;
		Random.pushGenerator( 0xC0FFEEL );
		for (int i = 0; i < N; i++){
			//穿插：既要真跑到「会改写等级」的分支，也要跑到「无需改写」的分支
			Trials.modifyGeneratedItem( mk( "武器", 2, false ) );   // -> 0，要写回
			Trials.modifyGeneratedItem( mk( "武器", 3, true ) );    // -> -3，要写回
			Trials.modifyGeneratedItem( mk( "护甲", 0, false ) );   // -> 0，无需写回
			Trials.flattenQuestReward( mk( "戒指", 2, true ) );     // -> 0，要写回
			Trials.curseReverseLevel( mk( "武器", 1, true ) );      // 房间赠品：+1 -> -1，要写回
			Trials.curseReverseLevel( mk( "护甲", -2, true ) );     // 房间赠品：−2 保持，无需写回
			seqHooked[i] = Random.Long();
		}
		Random.popGenerator();

		boolean same = true;
		int firstDiff = -1;
		for (int i = 0; i < N; i++){
			if (seqPlain[i] != seqHooked[i]){ same = false; firstDiff = i; break; }
		}
		check( same, "同 seed 下两轮随机序列逐位相同（" + N + " 个数"
				+ (firstDiff < 0 ? "" : "，首个差异在第 " + firstDiff + " 个") + "）" );
		info( String.format("plain[0..2]=%d,%d,%d  hooked[0..2]=%d,%d,%d",
				seqPlain[0], seqPlain[1], seqPlain[2], seqHooked[0], seqHooked[1], seqHooked[2]) );

		//--------------------------------------------------------------
		say("");
		say("--- ⑦ 房间赠品（祭坛 / 墓室）的「因诅咒取负」 ---");
		// 祭坛（SacrificeRoom）与墓室（CryptRoom）把诅咒补在生成**之后**，所以规则② 在生成出口
		// 看不到它；同时「未诅咒就白送一次 upgrade()」也发生在生成之后。这里补的钩子只处理正等级：
		// 负数必须**原样保留**（那是生成时就掷中诅咒、规则② 已经算过的），
		// 若在这里再取一次负就会 −1 → +1 —— 这是本机制最容易踩的坑。
		int[][] room = {
				{  0,  0 },   // +0 -> 0（「诅咒道具生成时为 0 则不变」）
				{  1, -1 },   // +1（房间白送的 upgrade）-> -1
				{  2, -2 },
				{  3, -3 },
				{ -1, -1 },   // ★ 已是负等级：必须原样保留，不能二次取负
				{ -2, -2 },
				{ -3, -3 },
		};
		for (int[] c : room){
			StringBuilder sb = new StringBuilder();
			for (String kind : kinds){
				Item it = mk( kind, c[0], true );   // 祭坛 / 墓室的赠品必定诅咒
				Trials.curseReverseLevel( it );
				int got = it.trueLevel();
				check( got == c[1], String.format("房间：%s 加工后 %+d（诅咒）-> %+d（期望 %+d）",
						kind, c[0], got, c[1]) );
				sb.append(String.format("%s=%+d ", kind, got));
			}
			info( String.format("房间行 %+d 五类实得：%s", c[0], sb.toString().trim()) );
		}

		Item m1 = mk( "武器", -1, true );
		Trials.curseReverseLevel( m1 );
		check( m1.trueLevel() == -1, "★ 陷阱回归：−1 不得被二次取负成 +1（实得 " + m1.trueLevel() + "）" );

		// 与 flattenQuestReward 的语义差异：那个会把负等级也一起抹成 0
		Item negKeep = mk( "武器", -2, true );
		Trials.curseReverseLevel( negKeep );
		check( negKeep.trueLevel() == -2, "curseReverseLevel 保留 −2（实得 " + negKeep.trueLevel() + "）" );
		Item negFlat = mk( "武器", -2, true );
		Trials.flattenQuestReward( negFlat );
		check( negFlat.trueLevel() == 0, "对照：flattenQuestReward 把 −2 抹成 0（实得 " + negFlat.trueLevel() + "）" );

		Dungeon.trials = 0;
		Item offRoom = mk( "武器", 1, true );
		Trials.curseReverseLevel( offRoom );
		check( offRoom.trueLevel() == 1, "不开 BINAH 时取负是空操作（+1 保持，实得 " + offRoom.trueLevel() + "）" );
		Dungeon.trials = Trials.BINAH;

		Trials.curseReverseLevel( null );
		check( true, "curseReverseLevel(null) 安全返回（未抛异常）" );

		//--------------------------------------------------------------
		Dungeon.trials = savedTrials;

		say("");
		say("==============================================================");
		say( String.format("断言 %d 条：PASS %d / FAIL %d", checks, checks - failures, failures) );
		say( failures == 0 ? "ALL PASS" : "HAS FAILURES" );
		say("==============================================================");
		System.exit( failures == 0 ? 0 : 1 );
	}
}
