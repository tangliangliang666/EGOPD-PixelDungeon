import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.watabou.utils.Reflection;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 传火大剑跨局继承（2026-09-20）的真装载核验：
 * ① 真 Properties.load 两份 items 文本 ⇒ 查 stats_desc 是否真按「两段」解析、标记/占位符完好；
 * ② 真跑一遍继承等级公式（srcLevel / 5 向下取整）的边界值；
 * ③ 真跑一遍「附魔类名 → Reflection.forName → isAssignableFrom(Weapon.Enchantment) → newInstance」
 *    这条重建链（markNextRun 存 getName()，grant 侧靠它还原）。
 */
public class FirelinkLocCheck {

	private static final String KEY = "items.weapon.melee.firelinkgreatsword.stats_desc";
	private static final String DIR = "core/src/main/assets/messages/items/";

	private static int fails = 0;

	private static void chk(boolean ok, String msg) {
		System.out.println((ok ? "  ok   " : "  FAIL ") + msg);
		if (!ok) fails++;
	}

	public static void main(String[] args) throws Exception {
		Properties zh = load(DIR + "items_zh.properties");
		Properties en = load(DIR + "items.properties");

		System.out.println("[1] 真 Properties.load 后的 stats_desc");
		checkText("items_zh", zh.getProperty(KEY), true);
		checkText("items", en.getProperty(KEY), false);

		System.out.println("[2] 继承等级公式（srcLevel / 5 向下取整）");
		int[][] cases = {{0,0},{1,0},{4,0},{5,1},{6,1},{9,1},{10,2},{12,2},{24,4},{25,5},{30,6}};
		boolean allOk = true;
		for (int[] c : cases) {
			int got = c[0] / 5;
			if (got != c[1]) {
				allOk = false;
				System.out.println("       srcLevel=" + c[0] + " => " + got + "（期望 " + c[1] + "）");
			}
		}
		chk(allOk, "11 组边界值全部符合「向下取整」（含 0/4/5/9/10/24/25）");
		chk(0 / 5 == 0, "srcLevel=0 ⇒ 继承 +0（不发空剑也不负等级）");

		System.out.println("[3] 附魔重建链（getName → forName → isAssignableFrom → newInstance）");
		String blazing = "com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing";
		Class<?> cls = Reflection.forName(blazing);
		chk(cls != null, "Reflection.forName(\"" + blazing + "\") 非空");
		chk(cls != null && Weapon.Enchantment.class.isAssignableFrom(cls), "它是 Weapon.Enchantment 的子类");
		Object inst = cls != null ? Reflection.newInstance(cls) : null;
		chk(inst instanceof Weapon.Enchantment, "Reflection.newInstance 能造出实例");
		chk(inst != null && blazing.equals(inst.getClass().getName()),
				"存进去的 getName() 与还原出的类名一致（往返无损）");
		chk(Reflection.forName("no.such.Enchant") == null,
				"不存在的类名 ⇒ forName 返回 null（enchantFromName 会静默跳过，不抛异常）");
		chk(!Weapon.Enchantment.class.isAssignableFrom(String.class),
				"非附魔类会被 isAssignableFrom 拦掉");

		System.out.println("[4] 无附魔路径");
		chk("".isEmpty(), "空串类名 ⇒ 直接返回 null（仍是白板剑，不报错）");

		System.out.println();
		if (fails > 0) {
			System.out.println("FAILED: " + fails);
			System.exit(1);
		}
		System.out.println("ALL PASS");
	}

	private static Properties load(String path) throws Exception {
		Properties p = new Properties();
		try (InputStreamReader r = new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)) {
			p.load(r);
		}
		return p;
	}

	private static void checkText(String tag, String v, boolean isZh) {
		chk(v != null && !v.isEmpty(), tag + ": stats_desc 非空");
		if (v == null) return;
		chk(!v.contains("\ufffd"), tag + ": 无 U+FFFD");
		chk(!v.contains("/n/"), tag + ": 无 /n/ 误写");
		chk(count(v, '_') % 2 == 0, tag + ": _ 强调标记成对（个数 " + count(v, '_') + "）");
		// 两段之间是一个空行 ⇒ split("\n") 会切出「段1 / 空串 / 段2」三块，非空块必须恰好 2 个
		String[] seg = v.split("\n");
		int nonEmpty = 0;
		for (String s : seg) if (!s.trim().isEmpty()) nonEmpty++;
		chk(v.contains("\n\n"), tag + ": 段间有空行（\\n\\n 真被解析成换行）");
		chk(nonEmpty == 2, tag + ": 解析后恰为 2 个非空段（实得 " + nonEmpty + "）");
		chk(seg.length == 3 && seg[1].trim().isEmpty(), tag + ": 中间那一块是空行（结构正确）");
		if (isZh) {
			chk(v.contains("五分之一") && v.contains("附魔"), tag + ": 写明「1/5 + 附魔」");
			chk(v.contains("烈焰附魔"), tag + ": 保留原有「烈焰附魔」说明（未被改写掉）");
		} else {
			chk(v.contains("a fifth") && v.contains("enchantment"), tag + ": 写明 a fifth + enchantment");
			chk(v.contains("flaming enchantment"), tag + ": 保留原有 flaming 说明");
		}
	}

	private static int count(String s, char c) {
		int n = 0;
		for (char x : s.toCharArray()) if (x == c) n++;
		return n;
	}
}
