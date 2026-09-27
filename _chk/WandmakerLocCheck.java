import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Properties;

/**
 * 老杖匠开场白（2026-09-22）的真装载核验：
 * ① 真 java.util.Properties.load 两份 actors 文本，取出 intro_hero 与六个旧键的存亡；
 * ② 用**真实类名**生成文本键（Wandmaker → actors.mobs.npcs.wandmaker.intro_hero），
 *    验证键名拼写与文本键逐字一致（防「文本显示成键名」那类坑）；
 * ③ 用**真 JDK String.format** 实测三条口径（多余实参忽略 / %s 替换 / 裸 % 抛异常并回退原文）；
 * ④ 列出缺 intro_hero 的语言文件 —— 它们会回落到基包（英文），不是 NO_TEXT_FOUND。
 */
public class WandmakerLocCheck {

	private static final String DIR = "core/src/main/assets/messages/actors/";
	private static final String SUFFIX = "actors";
	private static final String K = "intro_hero";
	private static final String[] OLD = {
			"intro_warrior", "intro_rogue", "intro_mage",
			"intro_huntress", "intro_duelist", "intro_cleric"};

	private static int fails = 0;

	private static void chk(boolean ok, String msg) {
		System.out.println((ok ? "  ok   " : "  FAIL ") + msg);
		if (!ok) fails++;
	}

	private static Properties load(String path) throws Exception {
		Properties p = new Properties();
		try (InputStreamReader r = new InputStreamReader(
				new FileInputStream(path), StandardCharsets.UTF_8)) {
			p.load(r);
		}
		return p;
	}

	/** 复刻 Messages.format：抛 IllegalFormatException 就回退原文。 */
	private static String format(String fmt, Object... args) {
		try {
			return String.format(java.util.Locale.ROOT, fmt, args);
		} catch (IllegalFormatException e) {
			return fmt;
		}
	}

	public static void main(String[] args) throws Exception {
		// —— 用真实类名生成键（与 Messages.get 同一套规则）——
		String clsKey = Wandmaker.class.getName()
				.replace("com.shatteredpixel.shatteredpixeldungeon.", "")
				.toLowerCase(java.util.Locale.ENGLISH);
		String fullKey = clsKey + "." + K;
		System.out.println("[1] 文本键（由真实类名生成）");
		chk(fullKey.equals("actors.mobs.npcs.wandmaker." + K), "类名 → 键 = " + fullKey);

		Properties zh = load(DIR + "actors_zh.properties");
		Properties base = load(DIR + SUFFIX + ".properties");

		System.out.println("[2] 真 Properties.load");
		String zhVal = zh.getProperty(fullKey);
		String enVal = base.getProperty(fullKey);
		chk(zhVal != null && !zhVal.trim().isEmpty(), "zh: " + fullKey + " = " + zhVal);
		chk(enVal != null && !enVal.trim().isEmpty(), "base(en): " + fullKey + " = " + enVal);
		for (String k : OLD) {
			chk(zh.getProperty(clsKey + "." + k) == null, "zh 已无 " + k);
		}
		for (String k : OLD) {
			chk(base.getProperty(clsKey + "." + k) == null, "base 已无 " + k);
		}
		// 相邻的保留键还在（防误删）
		for (String k : new String[]{"intro_1", "intro_dust", "intro_ember", "intro_berry",
				"intro_2", "reminder_dust", "name", "desc"}) {
			chk(zh.getProperty(clsKey + "." + k) != null, "zh 保留键在： " + k);
		}
		chk(zhVal != null && zhVal.indexOf((char) 0xFEFF) < 0, "zh 取值无 BOM 字符");

		System.out.println("[3] 真 JDK String.format 三条口径（intro_hero 恒带 1 个实参＝英雄名）");
		String extra = "no placeholder here";
		chk(format(extra, "Hero").equals(extra),
				"无占位符 + 1 实参 ⇒ 不抛错、原样返回（多余实参被忽略）");
		chk(format("你好，%s！", "Hero").equals("你好，Hero！"), "%s ⇒ 被英雄名替换");
		String bare = "完成度 100% 的台词";
		chk(format(bare, "Hero").equals(bare), "裸 % ⇒ 抛 IllegalFormatException 后回退原文");
		chk(zhVal != null && format(zhVal, "Hero").equals(zhVal),
				"当前 zh 占位文本经 format 后原样返回（无非法转换）");
		chk(zhVal != null && format(zhVal, "Hero").indexOf('\ufffd') < 0, "格式化结果无 U+FFFD");

		System.out.println("[4] 缺 intro_hero 的语言文件（会回落基包英文，不是 NO_TEXT_FOUND）");
		List<String> missing = new ArrayList<>();
		File[] files = new File(DIR).listFiles();
		int scanned = 0;
		if (files != null) {
			for (File f : files) {
				String n = f.getName();
				if (!n.startsWith(SUFFIX) || !n.endsWith(".properties")) continue;
				scanned++;
				if (n.equals(SUFFIX + ".properties")) continue;
				if (load(f.getPath()).getProperty(fullKey) == null) missing.add(n);
			}
		}
		chk(scanned >= 20, "扫描到 " + scanned + " 份 actors 语言文件");
		chk(!missing.contains("actors_zh.properties"), "zh 不在回落名单里");
		System.out.println("       缺键文件 " + missing.size() + " 份：" + missing);

		System.out.println();
		if (fails > 0) {
			System.out.println("FAIL: " + fails + " 条未通过");
			System.exit(1);
		}
		System.out.println("ALL PASS");
	}
}
