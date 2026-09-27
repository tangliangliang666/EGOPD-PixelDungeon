import java.io.*;
import java.util.*;

/**
 * 「咬紧牙关」文本的实装载核验（2026-09-20）。
 *
 * 为什么不能只用正则查源码：`properties` 的转义只有**真解析**才说得清——
 * 多段文本必须写成字面量 `\n`，写成真换行时 `Properties.load` 会把 entry 断成两截
 * （后一段变成缺 `=` 的孤立行），游戏里表现为「描述只剩第一段」，而且**不报错**。
 * 这里用 java.util.Properties + String.format 真跑一遍。
 *
 * 覆盖：
 *  ① 三条键（技能 short_desc / 技能 desc / buff desc）× zh+en：非空、无 U+FFFD、
 *     无 `/n/` 误写、`_` 标记成对；
 *  ② **分段断言**：desc 与 buff desc 解析后必须含 `\n\n`（两段），否则说明转义被写成真换行；
 *  ③ **占位符断言**：buff desc 必须含 `%s` 且 `String.format` 能成功渲染（%
 *     写错会抛 UnknownFormatConversionException / MissingFormatArgumentException）；
 *  ④ **语义断言**：描述必须点名「解离射线」与「血祭 / 割腕」（否则玩家不知道自伤换成长也能击穿）；
 *  ⑤ 回归：相邻键（name / cast / buff name / 隔壁技能的键）仍在。
 *
 * 运行：java _chk/GritLocCheck.java
 */
public class GritLocCheck {

    static int fail = 0;

    static void fail(String msg) {
        fail++;
        System.out.println("FAIL  " + msg);
    }

    static final String DIR = "D:/PD/core/src/main/assets/messages/actors/";

    static final String K_SHORT = "actors.hero.abilities.middlefinger.gritteeth.short_desc";
    static final String K_DESC = "actors.hero.abilities.middlefinger.gritteeth.desc";
    static final String K_BUFF = "actors.buffs.gritteethbuff.desc";

    /** 回归键：改文本时最容易把邻键碰坏（只挑 zh/en 都有的）。 */
    static final String[] REG_KEYS = {
            "actors.hero.abilities.middlefinger.gritteeth.name",
            "actors.hero.abilities.middlefinger.gritteeth.cast",
            "actors.buffs.gritteethbuff.name",
            "actors.hero.abilities.middlefinger.neverforget.name",
    };

    /**
     * 已知的 en 缺口（本作英文文本长期滞后，zh 有而 en 无的键共 179 条）——
     * 这里只登记与本技能同族的那几条，**不算 FAIL**，只是提醒别把它当成「本次改坏了」。
     */
    static final String[] KNOWN_EN_GAPS = {
            "actors.hero.abilities.middlefinger.neverforget.cast",
            "actors.hero.talent.blade_release.title",
            "actors.hero.talent.blade_release.desc",
    };

    /** 语义：描述必须让玩家看懂「谁也能击穿免死」。 */
    static final String[][] MUST = {
            { "zh", K_SHORT, "解离射线" }, { "zh", K_SHORT, "血祭" }, { "zh", K_SHORT, "割腕" },
            { "zh", K_DESC, "解离射线" }, { "zh", K_DESC, "血祭" }, { "zh", K_DESC, "割腕" },
            { "zh", K_BUFF, "解离射线" }, { "zh", K_BUFF, "血祭" }, { "zh", K_BUFF, "割腕" },
            { "en", K_SHORT, "disintegration rays" }, { "en", K_SHORT, "blood rites" }, { "en", K_SHORT, "wrist-slitting" },
            { "en", K_DESC, "disintegration rays" }, { "en", K_DESC, "blood rites" }, { "en", K_DESC, "wrist-slitting" },
            { "en", K_BUFF, "disintegration rays" }, { "en", K_BUFF, "blood rites" }, { "en", K_BUFF, "wrist-slitting" },
    };

    public static void main(String[] args) throws Exception {
        Properties zh = load(DIR + "actors_zh.properties");
        Properties en = load(DIR + "actors.properties");

        String[] keys = { K_SHORT, K_DESC, K_BUFF };

        for (Object[] side : new Object[][]{ { "zh", zh }, { "en", en } }) {
            String tag = (String) side[0];
            Properties p = (Properties) side[1];
            for (String k : keys) {
                String v = p.getProperty(k);
                String id = tag + " " + k;
                if (v == null) { fail("[MISS] " + id); continue; }
                if (v.trim().isEmpty()) { fail("[EMPTY] " + id); continue; }
                if (v.indexOf('\uFFFD') >= 0) fail("[REPL] " + id + " 含 U+FFFD");
                if (v.contains("/n/")) fail("[NL] " + id + " 含误写的 /n/");
                int us = 0;
                for (int i = 0; i < v.length(); i++) if (v.charAt(i) == '_') us++;
                if (us % 2 != 0) fail("[MARK] " + id + " 下划线标记为奇数个(" + us + ")：" + v);
            }

            // ② 分段：解析后必须真的有两段
            for (String k : new String[]{ K_DESC, K_BUFF }) {
                String v = p.getProperty(k);
                if (v != null && !v.contains("\n\n")) {
                    fail("[PARA] " + tag + " " + k + " 解析后没有两段（\\n\\n 可能被写成了真换行）");
                }
            }

            // ③ 占位符：buff desc 的 %s 必须能被 format 吃掉
            String b = p.getProperty(K_BUFF);
            if (b == null) {
                fail("[MISS] " + tag + " " + K_BUFF);
            } else {
                if (!b.contains("%s")) fail("[FMT] " + tag + " " + K_BUFF + " 丢了 %s 占位符");
                try {
                    String out = String.format(b, 7);
                    if (!out.contains("7")) fail("[FMT] " + tag + " " + K_BUFF + " format 后没带上实参");
                } catch (RuntimeException e) {
                    fail("[FMT] " + tag + " " + K_BUFF + " String.format 抛异常：" + e);
                }
            }
        }

        // ④ 语义
        for (String[] e : MUST) {
            Properties p = e[0].equals("zh") ? zh : en;
            String v = p.getProperty(e[1]);
            if (v == null) { fail("[MISS] " + e[0] + " " + e[1]); continue; }
            if (!v.toLowerCase().contains(e[2].toLowerCase())) {
                fail("[DESC] " + e[0] + " " + e[1] + " 未点名「" + e[2] + "」：" + v);
            }
        }

        // ⑤ 回归
        for (String k : REG_KEYS) {
            for (Object[] side : new Object[][]{ { "zh", zh }, { "en", en } }) {
                String v = ((Properties) side[1]).getProperty(k);
                if (v == null || v.trim().isEmpty()) fail("[REG] " + side[0] + " " + k + " 缺失/为空");
            }
        }

        // ⑥ 已知 en 缺口：只提示，不算 FAIL
        System.out.println("=== 已知英文缺口（本作 en 长期滞后，非本次改动造成）===");
        for (String k : KNOWN_EN_GAPS) {
            String v = en.getProperty(k);
            System.out.println("  [" + (v == null ? "缺" : "有") + "] " + k);
        }

        System.out.println("=== 三条文本解析结果 ===");
        for (Object[] side : new Object[][]{ { "zh", zh }, { "en", en } }) {
            String tag = (String) side[0];
            Properties p = (Properties) side[1];
            for (String k : keys) {
                String v = p.getProperty(k);
                String shown = v == null ? "(缺失)" : v.replace("\n", "\\n");
                System.out.println("[" + tag + "] " + k);
                System.out.println("      " + shown);
            }
        }

        System.out.println("=== 检查完毕 ===");
        System.out.println(fail == 0
                ? ("ALL PASS (3 键 x 2 语言；分段/占位符/语义/回归 全绿)")
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
