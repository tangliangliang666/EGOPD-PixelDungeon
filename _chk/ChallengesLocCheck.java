import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndFunChallenges;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * 挑战系统 + 「水仙追迹」+ 「趣味挑战」分类的装载核验。
 *
 * <p>与 NarcissusLocCheck 同款思路：**用真 Properties 装载**，因为孤立反斜杠 / 十六进制转义写错
 * 会在 {@code load} 直接崩，或把两行并成一行（「文本只剩第一段」那类断行事故）——正则看不出来。</p>
 *
 * <p>本检查器刻意**引用真 {@code Challenges} 类**（而不是把 NAME_IDS 抄一份）：
 * 这样将来任何人新增一条挑战却忘了补文本，这里会立刻报出来。</p>
 *
 * 覆盖：
 *  ① 位与常量：位号不重复、NAME_IDS/MASKS 等长、MAX_VALUE 装得下所有位、
 *     MAX_CHALS == 常规挑战数、activeChallenges 只数常规挑战；
 *  ② **趣味挑战分类（2026-09-24）**：FUN_MASK 恰为那四条、常规 9 / 趣味 4 恰好互补、
 *     四条在 isFun 侧为真、在 isRegular 侧为假、合并位域也判得对、nameId 可反查；
 *  ③ **得分倍率口径**：scoredChallenges 保留旧语义（除调试模式外全算），
 *     与 activeChallenges 的差恰好等于「趣味挑战 − 调试模式」那几条；
 *  ④ 每个挑战在 zh/en 两侧都有非空的 name + desc，且无 U+FFFD、无误写的 `/n/`、下划线成对；
 *  ⑤ 四条趣味挑战的固定中文名硬断言（用户 2026-09-24 点名的名字）；
 *  ⑥ 新挑战「水仙追迹」描述**真的能解析出换行**，且要点齐全；
 *  ⑦ 新增窗口 / 按钮文本键（zh + en）存在且非空，且键名与真类名推出来的一致；
 *  ⑧ 邻键回归：旧的 debug_mode / jelly_person / demolition_squad 文案没被碰坏。
 *
 * 运行（需要游戏 classpath，见 skill egopd-source-verify §1；**新编产物目录要排在 classpath 最前**）：
 *   java -Dstdout.encoding=UTF-8 -cp "D:/PD/_chk/_javachk;$(cat _chk/_cp2.txt)" _chk/ChallengesLocCheck.java
 */
public class ChallengesLocCheck {

	static int pass = 0;
	static int fails = 0;

	static void fail( String msg ){
		fails++;
		System.out.println("FAIL  " + msg);
	}

	static void chk( boolean cond, String msg ){
		if (cond){ pass++; }
		else { fail(msg); }
	}

	static final String MISC    = "D:/PD/core/src/main/assets/messages/misc/";
	static final String WINDOWS = "D:/PD/core/src/main/assets/messages/windows/";

	/**
	 * 「趣味挑战」标题键，由**真类名**推出来（{@code Messages.get(context, k)} 的键＝类全名去
	 * 包前缀后整体小写 + "." + k）。这样「类名 / 键名拼错」这类只会显示成键名、不报错的坑
	 * 就变成一条硬断言。类加载失败（headless 下 Window 的静态依赖）时回落字面键名并打印 NOTE。
	 */
	static final String FUN_TITLE_KEY;
	static {
		String k;
		try {
			k = WndFunChallenges.class.getName()
					.replace("com.shatteredpixel.shatteredpixeldungeon.", "")
					.toLowerCase(Locale.ENGLISH) + ".title";
		} catch (Throwable t){
			k = "windows.wndfunchallenges.title";
			System.out.println("[NOTE] 无法加载 WndFunChallenges 取类名（" + t + "），改用字面键名 " + k);
		}
		FUN_TITLE_KEY = k;
	}

	public static void main( String[] args ) throws Exception {

		Properties zh = load( MISC + "misc_zh.properties" );
		Properties en = load( MISC + "misc.properties" );
		Properties wzh = load( WINDOWS + "windows_zh.properties" );
		Properties wen = load( WINDOWS + "windows.properties" );

		System.out.println("== ① 位与常量 ==");
		chk( Challenges.NARCISSUS_TRACING == 4096,
				"NARCISSUS_TRACING == 4096（实为 " + Challenges.NARCISSUS_TRACING + "）" );
		chk( Challenges.DEBUG_MODE == 2048,
				"DEBUG_MODE 位号没被改动（实为 " + Challenges.DEBUG_MODE + "）" );
		chk( Challenges.MAX_VALUE == 8191,
				"MAX_VALUE == 8191（13 位全 1；实为 " + Challenges.MAX_VALUE + "）" );
		chk( (Challenges.MAX_VALUE & (Challenges.MAX_VALUE + 1)) == 0,
				"MAX_VALUE 是 2^n-1 形状（掩码语义，不能是别的数）" );

		chk( Challenges.NAME_IDS.length == Challenges.MASKS.length,
				"NAME_IDS 与 MASKS 等长（" + Challenges.NAME_IDS.length + " vs " + Challenges.MASKS.length + "）" );

		Set<Integer> seen = new HashSet<>();
		boolean dup = false;
		int or = 0;
		for (int m : Challenges.MASKS){
			if (!seen.add(m)) dup = true;
			or |= m;
			if (m == 0 || (m & (m - 1)) != 0) dup = true;   // 不是 2 的幂
		}
		chk( !dup, "MASKS 里每个位都是 2 的幂、且互不重复" );
		chk( or == Challenges.MAX_VALUE,
				"所有挑战位并起来恰好 == MAX_VALUE（13 位一个不多一个不少；or=" + or + "）" );
		chk( Challenges.regularMasks().length == Challenges.MAX_CHALS,
				"MAX_CHALS == 常规挑战数（" + Challenges.MAX_CHALS + " vs "
						+ Challenges.regularMasks().length + "）—— 两者必须同步" );

		chk( Challenges.activeChallenges( Challenges.MAX_VALUE ) == Challenges.MAX_CHALS,
				"掩码全开时常规挑战数 == MAX_CHALS（说明 MAX_CHALS 与实际条数一致）" );

		System.out.println("== ② 趣味挑战分类 ==");
		chk( Challenges.FUN_MASK == (Challenges.DEMOLITION_SQUAD | Challenges.JELLY_PERSON
						| Challenges.NARCISSUS_TRACING | Challenges.DEBUG_MODE),
				"FUN_MASK 恰为「拆迁办|依旧果冻人|水仙追迹|调试模式」（实为 " + Challenges.FUN_MASK + "）" );
		chk( Challenges.funMask() == Challenges.FUN_MASK, "funMask() 与 FUN_MASK 同值" );
		chk( Challenges.funMasks().length == 4,
				"趣味挑战恰好 4 条（实为 " + Challenges.funMasks().length + "）" );
		chk( Challenges.regularMasks().length + Challenges.funMasks().length == Challenges.MASKS.length,
				"常规 + 趣味 恰好覆盖全部挑战（" + Challenges.regularMasks().length + " + "
						+ Challenges.funMasks().length + " vs " + Challenges.MASKS.length + "）" );
		chk( (Challenges.regularMask() | Challenges.funMask()) == Challenges.MAX_VALUE,
				"常规位 ∪ 趣味位 == MAX_VALUE（两类互补、无遗漏）" );
		chk( (Challenges.regularMask() & Challenges.funMask()) == 0,
				"常规位 ∩ 趣味位 == 0（两类互斥、无重复）" );

		for (int[] pair : new int[][]{
				{Challenges.DEMOLITION_SQUAD, Challenges.JELLY_PERSON},
				{Challenges.NARCISSUS_TRACING, Challenges.DEBUG_MODE}}){
			for (int m : pair){
				chk( Challenges.isFun( m ), "位 " + m + " 属趣味挑战（isFun）" );
				chk( !Challenges.isRegular( m ), "位 " + m + " 不算常规挑战（isRegular）" );
			}
		}
		for (int[] pair : new int[][]{
				{Challenges.CHAMPION_ENEMIES, Challenges.STRONGER_BOSSES},
				{Challenges.NO_FOOD, Challenges.DARKNESS}}){
			for (int m : pair){
				chk( !Challenges.isFun( m ), "位 " + m + " 不属趣味挑战" );
				chk( Challenges.isRegular( m ), "位 " + m + " 算常规挑战" );
			}
		}
		// 判据必须同时吃「单个位」与「合并位域」（isFun 用位与而不是相等）
		chk( Challenges.isFun( Challenges.MAX_VALUE ), "isFun 吃合并位域：全开 ⇒ true" );
		chk( !Challenges.isRegular( Challenges.MAX_VALUE ), "isRegular 吃合并位域：全开 ⇒ false" );
		chk( !Challenges.isFun( Challenges.regularMask() ), "全是常规位 ⇒ isFun false" );
		chk( !Challenges.isRegular( Challenges.funMask() ), "全是趣味位 ⇒ isRegular false" );

		chk( Challenges.activeChallenges( Challenges.DEBUG_MODE ) == 0,
				"只开调试模式时常规挑战数 = 0（实为 " + Challenges.activeChallenges( Challenges.DEBUG_MODE ) + "）" );
		chk( Challenges.activeChallenges( Challenges.funMask() ) == 0,
				"只开趣味挑战时常规挑战数 = 0（实为 "
						+ Challenges.activeChallenges( Challenges.funMask() ) + "）" );
		chk( Challenges.activeFun( Challenges.DEBUG_MODE | Challenges.NARCISSUS_TRACING ) == 2,
				"调试模式 + 水仙追迹 ⇒ 趣味挑战数 = 2（实为 "
						+ Challenges.activeFun( Challenges.DEBUG_MODE | Challenges.NARCISSUS_TRACING ) + "）" );
		chk( Challenges.activeFun( Challenges.MAX_VALUE ) == 4,
				"掩码全开时趣味挑战数 == 4（实为 " + Challenges.activeFun( Challenges.MAX_VALUE ) + "）" );
		chk( Challenges.activeFun( Challenges.regularMask() ) == 0,
				"只开常规挑战时趣味挑战数 = 0" );
		chk( Challenges.activeFun( 0 ) == 0, "掩码为 0 时趣味挑战数 = 0" );

		System.out.println("== ③ 得分倍率口径（scoredChallenges 保留旧语义）==");
		// 旧 activeChallenges 的语义：除「调试模式」外全部挑战都计入（趣味挑战也在内）
		chk( Challenges.scoredChallenges( 0 ) == 0, "scoredChallenges(0) == 0" );
		chk( Challenges.scoredChallenges( Challenges.DEBUG_MODE ) == 0,
				"scoredChallenges(调试模式) == 0（调试局不发分，计数时排除它）" );
		chk( Challenges.scoredChallenges( Challenges.NARCISSUS_TRACING ) == 1,
				"scoredChallenges(水仙追迹) == 1（它仍计入计数，倍率置 0 由 Rankings 单独做）" );
		chk( Challenges.scoredChallenges( Challenges.MAX_VALUE ) == Challenges.MASKS.length - 1,
				"掩码全开时 scoredChallenges == 12（全部 13 条去掉调试模式；实为 "
						+ Challenges.scoredChallenges( Challenges.MAX_VALUE ) + "）" );
		// 交叉校验：scored == 常规 + 趣味 − (调试模式是否开着)
		for (int m : new int[]{0, Challenges.DEBUG_MODE, Challenges.NARCISSUS_TRACING,
				Challenges.funMask(), Challenges.regularMask(), Challenges.MAX_VALUE,
				Challenges.DEMOLITION_SQUAD | Challenges.NO_FOOD | Challenges.DARKNESS}){
			int want = Challenges.activeChallenges( m ) + Challenges.activeFun( m )
					- ((m & Challenges.DEBUG_MODE) != 0 ? 1 : 0);
			chk( Challenges.scoredChallenges( m ) == want,
					"scoredChallenges(" + m + ") == 常规+趣味−调试（期望 " + want
							+ "，实为 " + Challenges.scoredChallenges( m ) + "）" );
		}

		System.out.println("== ④ nameId 反查（NAME_IDS / MASKS 同位同长）==");
		int nameMiss = 0;
		for (int i = 0; i < Challenges.MASKS.length; i++){
			String got = Challenges.nameId( Challenges.MASKS[i] );
			if (got == null || !got.equals( Challenges.NAME_IDS[i] )){
				nameMiss++;
				fail("nameId(" + Challenges.MASKS[i] + ") 应为 " + Challenges.NAME_IDS[i] + "，实为 " + got);
			}
		}
		chk( nameMiss == 0, "13 个挑战位都能用 nameId 反查到对应文本键" );
		chk( Challenges.nameId( 0 ) == null, "nameId(0) 返回 null（非法位不静默给错名）" );
		chk( Challenges.nameId( 8192 ) == null, "nameId(8192) 返回 null（超出 MAX_VALUE 的位）" );
		chk( Challenges.nameId( Challenges.MAX_VALUE ) == null,
				"nameId(合并位域) 返回 null（只接受单个位）" );

		System.out.println("== ⑤ 每条挑战的 name / desc（zh + en） ==");
		for (int i = 0; i < Challenges.NAME_IDS.length; i++){
			String id = Challenges.NAME_IDS[i];
			checkText("zh " + id, zh.getProperty("challenges." + id));
			checkText("en " + id, en.getProperty("challenges." + id));
			checkText("zh " + id + "_desc", zh.getProperty("challenges." + id + "_desc"));
			checkText("en " + id + "_desc", en.getProperty("challenges." + id + "_desc"));
		}

		System.out.println("== ⑥ 四条趣味挑战的固定中文名 ==");
		// 用户 2026-09-24 点名移入「趣味挑战」的四条；挑战名属「稳定的概念名」，可硬断言
		String[][] funNames = {
				{"challenges.demolition_squad", "拆迁办"},
				{"challenges.jelly_person", "依旧果冻人"},
				{"challenges.narcissus_tracing", "水仙追迹"},
				{"challenges.debug_mode", "调试模式"},
		};
		for (String[] p : funNames){
			String got = zh.getProperty( p[0] );
			chk( p[1].equals( got ), "zh " + p[0] + " == 「" + p[1] + "」（实为「" + got + "」）" );
		}

		System.out.println("== ⑦ 「水仙追迹」描述能解析出换行，且要点齐全 ==");
		String zhDesc = zh.getProperty("challenges.narcissus_tracing_desc");
		String enDesc = en.getProperty("challenges.narcissus_tracing_desc");
		chk( zhDesc != null && zhDesc.contains("\n"),
				"zh 描述里有**真换行**（说明字面 \\n 生效，不是原样显示）" );
		chk( enDesc != null && enDesc.contains("\n"), "en 描述里有**真换行**" );
		chk( zhDesc != null && zhDesc.contains("恒为 0"), "zh 描述写明了得分倍率归零" );
		chk( zhDesc != null && zhDesc.contains("水仙十字圣剑"), "zh 描述写明了开局携带水仙十字圣剑" );
		chk( zhDesc != null && zhDesc.contains("财富戒指"), "zh 描述写明了开局携带财富戒指" );
		chk( enDesc != null && enDesc.contains("forced to _0_"), "en 描述写明了得分倍率归零" );
		chk( enDesc != null && enDesc.contains("Narcissus Cross Sword"), "en 描述写明了开局武器" );
		chk( enDesc != null && enDesc.contains("Ring of Wealth"), "en 描述写明了财富戒指" );
		//2026-09-24：开局发放方式由「装备主手」改为「放进背包」，描述必须跟着改口径
		chk( zhDesc != null && zhDesc.contains("放在背包里"), "zh 描述写明了圣剑放在背包里" );
		chk( enDesc != null && enDesc.contains("in your backpack"), "en 描述写明了放在背包里" );
		chk( zhDesc != null && !zhDesc.contains("已装备在主手"),
				"zh 描述里「已装备在主手」已彻底退场（否则玩家看到的还是旧说法）" );
		chk( enDesc != null && !enDesc.contains("(equipped)"),
				"en 描述里 \"(equipped)\" 已彻底退场" );
		chk( zhDesc != null && enDesc != null && zhDesc.split("\n", -1).length == enDesc.split("\n", -1).length,
				"zh / en 的段落数一致（" + (zhDesc == null ? -1 : zhDesc.split("\n", -1).length) + " vs "
						+ (enDesc == null ? -1 : enDesc.split("\n", -1).length) + "）" );

		System.out.println("== ⑧ 新增窗口 / 按钮文本键（趣味挑战）==");
		chk( "windows.wndfunchallenges.title".equals( FUN_TITLE_KEY ),
				"由 WndFunChallenges 类名推出的标题键 == windows.wndfunchallenges.title（实为 " + FUN_TITLE_KEY + "）" );
		String[][] wkeys = {
				{FUN_TITLE_KEY, "趣味挑战"},
				{"windows.wndgame.funchallenges", "趣味挑战"},
				{"windows.wndgameinprogress.funchallenges", "趣味挑战"},
		};
		for (String[] p : wkeys){
			String z = wzh.getProperty( p[0] );
			String e = wen.getProperty( p[0] );
			chk( nonEmpty( z ), "zh " + p[0] + " 存在且非空" );
			chk( nonEmpty( e ), "en " + p[0] + " 存在且非空" );
			chk( p[1].equals( z ), "zh " + p[0] + " == 「" + p[1] + "」（实为「" + z + "」）" );
			checkText("zh " + p[0], z);
			checkText("en " + p[0], e);
		}
		// 三个按钮共用「趣味挑战」这个叫法，zh 侧必须完全一致（否则同一类东西三个名字）
		chk( wzh.getProperty( FUN_TITLE_KEY ) != null
						&& wzh.getProperty( FUN_TITLE_KEY ).equals( wzh.getProperty("windows.wndgame.funchallenges") )
						&& wzh.getProperty( FUN_TITLE_KEY ).equals( wzh.getProperty("windows.wndgameinprogress.funchallenges") ),
				"三个入口的 zh 名称完全一致（窗口标题 / 局内菜单 / 存档槽）" );

		System.out.println("== ⑨ 邻键回归（本次插入没碰坏旧文案） ==");
		for (String id : new String[]{ "challenges.debug_mode", "challenges.jelly_person",
				"challenges.demolition_squad" }){
			chk( nonEmpty( zh.getProperty(id) ), "zh " + id + " 仍非空" );
			chk( nonEmpty( zh.getProperty(id + "_desc") ), "zh " + id + "_desc 仍非空" );
			chk( nonEmpty( en.getProperty(id) ), "en " + id + " 仍非空" );
			chk( nonEmpty( en.getProperty(id + "_desc") ), "en " + id + "_desc 仍非空" );
		}
		chk( zh.getProperty("challenges.debug_mode_desc").contains("\n"),
				"zh debug_mode_desc 仍有真换行（插入点就在它后面，最容易顺手弄坏）" );
		for (String k : new String[]{ "windows.wndchallenges.title", "windows.wndtrials.title",
				"windows.wndgame.challenges", "windows.wndgame.trials",
				"windows.wndgameinprogress.challenges", "windows.wndgameinprogress.trials" }){
			chk( nonEmpty( wzh.getProperty(k) ), "邻键 zh " + k + " 仍非空" );
			chk( nonEmpty( wen.getProperty(k) ), "邻键 en " + k + " 仍非空" );
		}

		System.out.println("");
		System.out.println(fails == 0 ? ("ALL PASS（共 " + pass + " 条断言）")
				: ("共 " + fails + " 处问题 / PASS " + pass));
		if (fails != 0) System.exit(1);
	}

	static boolean nonEmpty( String v ){
		return v != null && !v.trim().isEmpty();
	}

	static void checkText( String tag, String v ){
		if (v == null){ fail("[MISS] " + tag); return; }
		if (v.trim().isEmpty()){ fail("[EMPTY] " + tag); return; }
		if (v.indexOf('\uFFFD') >= 0){ fail("[REPL] " + tag + " 含 U+FFFD"); return; }
		if (v.contains("/n/")){ fail("[NL] " + tag + " 含误写的 /n/"); return; }
		int us = 0;
		for (int i = 0; i < v.length(); i++) if (v.charAt(i) == '_') us++;
		if (us % 2 != 0){ fail("[MARK] " + tag + " 下划线标记为奇数个(" + us + ")"); return; }
		pass++;
	}

	static Properties load( String path ) throws Exception {
		Properties p = new Properties();
		try (Reader r = new InputStreamReader( new FileInputStream( path ), "UTF-8" )){
			p.load( r );
		}
		return p;
	}
}
