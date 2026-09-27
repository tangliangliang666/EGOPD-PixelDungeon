import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Trials;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * [EGOPD] 考验通关成就的**真实类反射探针**（不是源码解析）。
 *
 * 编译/运行（classpath 必须把本次 javac 的产物 `_chk/_javachk` 放**最前**，
 * 否则会读到 Gradle 上次构建的旧 Badges.class，里面还没有 TRIAL_* ）：
 *
 *   JDK/bin/javac.exe -encoding UTF-8 -cp "_chk/_javachk;<gdx>" -d _chk/_javachk _chk/TrialBadgesProbe.java
 *   JDK/bin/java.exe  -cp "_chk/_javachk;<gdx>" TrialBadgesProbe > _chk/_trialbadges.out
 */
public class TrialBadgesProbe {

	static int pass = 0, fail = 0;

	static void chk(boolean cond, String msg) {
		if (cond) { pass++; System.out.println("[OK] " + msg); }
		else      { fail++; System.out.println("[FAIL] " + msg); }
	}

	//与 Trials.MASKS 同序：卡巴拉之树自上而下
	static final String[] NAMES = {
			"KETER", "HOKMA", "BINAH", "CHESED", "GEBURA",
			"TIPHERETH", "NETZACH", "HOD", "YESOD", "MALKUTH"
	};

	public static void main(String[] args) throws Exception {

		Class<?> badge = Badges.Badge.class;
		Class<?> badgeType = Badges.BadgeType.class;

		System.out.println("=== ① 十个常量存在、image 索引连号 128~137、类型 LOCAL ===");
		Object[] vals = new Object[10];
		for (int i = 0; i < NAMES.length; i++) {
			String cn = "TRIAL_" + NAMES[i];
			Object c;
			try {
				c = Enum.valueOf((Class) badge, cn);
			} catch (IllegalArgumentException e) {
				chk(false, "常量 Badge." + cn + " 不存在");
				continue;
			}
			vals[i] = c;

			int img = ((Badges.Badge) c).image;
			chk(img == 128 + i, String.format("Badge.%-16s image=%d（期望 %d）", cn, img, 128 + i));

			Object t = badge.getField("type").get(c);
			chk(t.equals(Enum.valueOf((Class) badgeType, "LOCAL")),
					String.format("Badge.%-16s type=LOCAL（实际 %s）", cn, t));
		}

		System.out.println();
		System.out.println("=== ② 声明顺序：这 10 个必须是 Badge 的**最后 10 个**，且顺序 KETER→MALKUTH ===");
		Object[] all = badge.getEnumConstants();
		chk(all.length == 10 + (all.length - 10), "Badge 常量总数 = " + all.length);
		for (int i = 0; i < 10; i++) {
			Object last10 = all[all.length - 10 + i];
			chk(last10 == vals[i], String.format("倒数第 %d 个是 %s", 10 - i, ((Badges.Badge) last10).name()));
		}

		System.out.println();
		System.out.println("=== ③ 私有数组 TRIAL_BADGES 的下标顺序 == Trials.MASKS 的顺序 ===");
		Field f = Badges.class.getDeclaredField("TRIAL_BADGES");
		f.setAccessible(true);
		chk(Modifier.isPrivate(f.getModifiers()) && Modifier.isStatic(f.getModifiers())
				&& Modifier.isFinal(f.getModifiers()), "TRIAL_BADGES 是 private static final");
		Badges.Badge[] arr = (Badges.Badge[]) f.get(null);
		chk(arr != null && arr.length == 10, "TRIAL_BADGES.length = " + (arr == null ? "null" : arr.length));
		for (int i = 0; i < 10; i++) {
			chk(arr[i] == vals[i], String.format("TRIAL_BADGES[%d] = %s（期望 TRIAL_%s）",
					i, arr[i].name(), NAMES[i]));
		}

		System.out.println();
		System.out.println("=== ④ Trials.MASKS 与常量同序（1<<i），且与 TRIAL_BADGES 一一对应 ===");
		int[] masks = Trials.MASKS;
		chk(masks.length == 10, "Trials.MASKS.length = " + masks.length);
		for (int i = 0; i < 10; i++) {
			chk(masks[i] == (1 << i), String.format("Trials.MASKS[%d] = %d（期望 %d）", i, masks[i], 1 << i));
		}
		int okPairs = 0;
		for (int i = 0; i < 10; i++) if (arr[i].image == 128 + i) okPairs++;
		chk(okPairs == 10, "TRIAL_BADGES[i].image == 128+i 对全部 i 成立（" + okPairs + "/10）");

		System.out.println();
		System.out.println("=== ⑤ validateTrialVictory 签名 ===");
		Method m = Badges.class.getDeclaredMethod("validateTrialVictory");
		chk(Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers()),
				"validateTrialVictory 是 public static");
		chk(m.getReturnType() == void.class, "返回 void");
		chk(m.getParameterCount() == 0, "无参数");

		System.out.println();
		System.out.println("=== ⑥ image 索引全库唯一、且 128~137 独占（不与既有徽章撞车）===");
		java.util.Map<Integer, String> seen = new java.util.HashMap<>();
		int dup = 0, negatives = 0, legacyMax = -1;
		for (Object o : all) {
			int img = ((Badges.Badge) o).image;
			if (img < 0) { negatives++; continue; }        // HIDDEN（无图）用 -1
			if (img < 128 && img > legacyMax) legacyMax = img;
			if (seen.containsKey(img)) { dup++; System.out.println("     撞车 image=" + img + " : " + seen.get(img) + " / " + ((Badges.Badge) o).name()); }
			else seen.put(img, ((Badges.Badge) o).name());
		}
		chk(dup == 0, "有图徽章的 image 索引无重复");
		chk(legacyMax == 127, "旧的最高 image 索引 = 127（实际 " + legacyMax + "）");
		int missing = 0;
		for (int i = 128; i <= 137; i++) if (!seen.containsKey(i)) missing++;
		chk(missing == 0, "128~137 十个索引都被占用（缺 " + missing + " 个）");

		System.out.println();
		System.out.println("PASS=" + pass + "  FAIL=" + fail);
		System.out.println(fail == 0 ? "ALL PASS" : "SOME FAILED");
		if (fail != 0) System.exit(1);
	}
}
