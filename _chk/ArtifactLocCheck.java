import java.io.*;
import java.util.*;

/**
 * 原版神器强化形态（5 件）文本的**实装载**核验（2026-09-20）。
 *
 * 为什么不能只用正则查源码：`properties` 的转义只有**真解析**才说得清 ——
 * 多段文本必须写成字面量 `\n`，写成真换行时 `Properties.load` 会把 entry 断成两截
 * （后一段变成缺 `=` 的孤立行），游戏里表现为「描述只剩第一段」，而且**不报错**。
 * 带参文本同理：字面 `%` 没写成 `%%`、或占位符与实参数量不符，
 * `Messages.get` 会**整串回退成原文**（玩家看到 `%d` 或键名）。
 *
 * 覆盖：
 *  ① 16 键（5 件 × name/desc/desc_enhanced + 血宴的 prick_mitigated）× zh/en：
 *     非空、无 U+FFFD、无 `/n/` 误写、`_` 强调标记成对；
 *  ② **分段断言**：多段描述解析后必须含 `\n\n`（真换行 ⇒ 只剩第一段）；
 *  ③ **占位符断言**：血宴的两条带参文本必须含 `%d`，且 `String.format` 能真渲染；
 *  ④ **中文名硬断言**（稳定的概念名，允许被重写的措辞一律不认）；
 *  ⑤ 回归：5 个原版神器的 name 仍在（强化形态靠 `Messages.get` 父类链继承它们的功能文本，
 *     但它们自己仍是**独立物品**，键不能被碰坏）。
 *
 * 运行：D:/PD/tools/jdk-21.0.12.1+1/bin/java -Dfile.encoding=UTF-8 _chk/ArtifactLocCheck.java
 */
public class ArtifactLocCheck {

    static int fail = 0;

    static void fail(String msg) {
        fail++;
        System.out.println("FAIL  " + msg);
    }

    static final String DIR = "D:/PD/core/src/main/assets/messages/items/";

    /** 强化形态的键前缀（= 包路径 items.artifacts + 类简单名小写）。 */
    static final String[] SLUGS = {
            "bloodfeastchalice", "lifelongstew", "chainofothers",
            "chapternineversetwo", "approachingday",
    };

    /** 每件要查的键（血宴多一条 prick_mitigated）。 */
    static String[] keysOf(String slug) {
        if (slug.equals("bloodfeastchalice")) {
            return new String[]{ "name", "desc", "desc_enhanced", "prick_mitigated" };
        }
        return new String[]{ "name", "desc", "desc_enhanced" };
    }

    static final String[] KEY_BASE = {
            "items.artifacts.bloodfeastchalice.", "items.artifacts.lifelongstew.",
            "items.artifacts.chainofothers.", "items.artifacts.chapternineversetwo.",
            "items.artifacts.approachingday.",
    };

    /** ④ 中文名硬断言。 */
    static final String[][] ZH_NAMES = {
            { "items.artifacts.bloodfeastchalice.name",   "血宴圣杯" },
            { "items.artifacts.lifelongstew.name",        "一生炖菜" },
            { "items.artifacts.chainofothers.name",       "他人之锁" },
            { "items.artifacts.chapternineversetwo.name", "9章2节" },
            { "items.artifacts.approachingday.name",      "迫近之日" },
    };

    /** ③ 带参文本：(键, 实参) —— 必须能 String.format 成功并且带出实参。 */
    static final String[] FORMAT_KEYS = {
            "items.artifacts.bloodfeastchalice.desc_enhanced",
            "items.artifacts.bloodfeastchalice.prick_mitigated",
    };

    /** ② 解析后必须含 `\n\n`（真两段）的键。 */
    static final String[] MULTI_PARA = {
            "items.artifacts.bloodfeastchalice.desc",
            "items.artifacts.bloodfeastchalice.desc_enhanced",
            "items.artifacts.chainofothers.desc",
            "items.artifacts.approachingday.desc_enhanced",
    };

    /** ⑤ 回归：5 个原版神器的 name（强化形态的功能文本沿父类链继承它们）。 */
    static final String[] REG_KEYS = {
            "items.artifacts.chaliceofblood.name",
            "items.artifacts.hornofplenty.name",
            "items.artifacts.etherealchains.name",
            "items.artifacts.unstablespellbook.name",
            "items.artifacts.talismanofforesight.name",
    };

    public static void main(String[] args) throws Exception {
        Properties zh = load(DIR + "items_zh.properties");
        Properties en = load(DIR + "items.properties");

        Properties[][] sides = { { zh }, { en } };
        String[] tags = { "zh", "en" };

        // ---------- ① 16 键 × 2 语言：基本卫生 ----------
        int seen = 0;
        for (int s = 0; s < SLUGS.length; s++) {
            for (String k : keysOf(SLUGS[s])) {
                String full = KEY_BASE[s] + k;
                seen++;
                for (int t = 0; t < 2; t++) {
                    String v = sides[t][0].getProperty(full);
                    if (v == null) { fail("[MISS] " + tags[t] + " " + full + " 缺失"); continue; }
                    if (v.trim().isEmpty()) fail("[EMPTY] " + tags[t] + " " + full);
                    if (v.indexOf('\uFFFD') >= 0) fail("[ENC] " + tags[t] + " " + full + " 含 U+FFFD");
                    if (v.contains("/n/")) fail("[ESC] " + tags[t] + " " + full + " 把 \\n 误写成 /n/（不会换行，会原样显示）");
                    int us = 0; for (int i = 0; i < v.length(); i++) if (v.charAt(i) == '_') us++;
                    if (us % 2 != 0) fail("[MARK] " + tags[t] + " " + full + " 的 _ 强调标记有 " + us + " 个（非偶数）");
                    if (v.equals(full)) fail("[FALLBACK] " + tags[t] + " " + full + " 的值就是键名本身");
                }
            }
        }

        // ---------- ② 分段：真解析后必须有两段 ----------
        for (String k : MULTI_PARA) {
            for (int t = 0; t < 2; t++) {
                String v = sides[t][0].getProperty(k);
                if (v == null) { fail("[MISS] " + tags[t] + " " + k); continue; }
                if (!v.contains("\n\n"))
                    fail("[SEG] " + tags[t] + " " + k + " 解析后只有一段（\\n 被写成真换行？）");
            }
        }

        // ---------- ③ 占位符：真 String.format ----------
        for (String k : FORMAT_KEYS) {
            for (int t = 0; t < 2; t++) {
                String v = sides[t][0].getProperty(k);
                if (v == null) { fail("[MISS] " + tags[t] + " " + k); continue; }
                if (!v.contains("%d")) { fail("[FMT] " + tags[t] + " " + k + " 丢了 %d 占位符"); continue; }
                try {
                    String out = String.format(v, 7);
                    if (!out.contains("7")) fail("[FMT] " + tags[t] + " " + k + " format 后没带上实参");
                } catch (RuntimeException e) {
                    fail("[FMT] " + tags[t] + " " + k + " String.format 抛异常：" + e);
                }
            }
        }

        // ---------- ④ 中文名硬断言 ----------
        for (String[] e : ZH_NAMES) {
            String v = zh.getProperty(e[0]);
            if (v == null) { fail("[NAME] zh " + e[0] + " 缺失"); continue; }
            if (!v.equals(e[1])) fail("[NAME] zh " + e[0] + " 应为「" + e[1] + "」，实际「" + v + "」");
        }

        // ---------- ⑤ 回归 ----------
        for (String k : REG_KEYS) {
            for (int t = 0; t < 2; t++) {
                String v = sides[t][0].getProperty(k);
                if (v == null || v.trim().isEmpty()) fail("[REG] " + tags[t] + " " + k + " 缺失/为空");
            }
        }

        // ---------- 打印解析结果（肉眼看分段与标记） ----------
        System.out.println("=== 解析后的文本（\\n 已还原成可见的 \\\\n）===");
        for (int s = 0; s < SLUGS.length; s++) {
            System.out.println("--- " + SLUGS[s] + " ---");
            for (String k : keysOf(SLUGS[s])) {
                String full = KEY_BASE[s] + k;
                String v = zh.getProperty(full);
                System.out.println("  [zh] " + k + ": "
                        + (v == null ? "(缺失)" : v.replace("\n", "\\n")));
            }
        }

        System.out.println("=== 检查完毕 ===");
        System.out.println(fail == 0
                ? ("ALL PASS（" + seen + " 键 x 2 语言；分段/占位符/名称/回归 全绿）")
                : ("共 " + fail + " 处问题"));
        if (fail != 0) System.exit(1);
    }

    static Properties load(String path) throws Exception {
        Properties p = new Properties();
        try (Reader r = new InputStreamReader(new FileInputStream(path), "UTF-8")) {
            p.load(r);
        }
        return p;
    }
}
