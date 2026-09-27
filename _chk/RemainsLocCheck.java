import java.io.*;
import java.util.*;

/**
 * 两位职业遗物的文本实装载核验（2026-09-18）：
 *   拇指·前二老板「破损义眼」/ 中指长兄「账簿残页」。
 *
 * 覆盖：
 *  ① 新键 `items.remains.brokeneye.*` / `items.remains.ledgerpage.*`（zh + en）非空、无 U+FFFD、
 *     无 `/n/` 误写、`_` 标记成对；
 *  ② **语义断言**：义眼描述必须点名「明示符石」（否则玩家不知道它和符石同效）、
 *     残页描述必须点名「力量」（否则不知道给的是力量）；
 *  ③ 回归：另外 8 件遗物的名称键仍然完好（改 properties 时最容易把邻键碰坏）。
 *
 * 用 java.util.Properties 真解析整份文件：孤立反斜杠 / 十六进制转义写错会在这里直接崩，
 * 或把两行并成一行（「文本只剩第一段」那类断行事故）。比正则强。
 *
 * 运行：java _chk/RemainsLocCheck.java
 */
public class RemainsLocCheck {

    static int fail = 0;

    static void fail(String msg) {
        fail++;
        System.out.println("FAIL  " + msg);
    }

    static final String DIR = "D:/PD/core/src/main/assets/messages/items/";

    /** 本轮新增的 4 个键。 */
    static final String[] NEW_KEYS = {
            "items.remains.brokeneye.name", "items.remains.brokeneye.desc",
            "items.remains.ledgerpage.name", "items.remains.ledgerpage.desc",
    };

    /** 回归：既有的 8 件遗物名称键（不含本轮新增的两件）。 */
    static final String[] OLD_NAME_KEYS = {
            "items.remains.sealshard.name", "items.remains.brokenstaff.name",
            "items.remains.cloakscrap.name", "items.remains.bowfragment.name",
            "items.remains.brokenhilt.name", "items.remains.tornpage.name",
            "items.remains.brokenterminal.name", "items.remains.brokenboneblade.name",
    };

    /** 用户点名的中文名（固定游戏对象名 ⇒ 可以硬断言）。 */
    static final String[][] ZH_NAMES = {
            { "items.remains.brokeneye.name", "破损义眼" },
            { "items.remains.ledgerpage.name", "账簿残页" },
    };

    /** 语义：描述必须让玩家看懂效果。 */
    static final String[][] DESC_MUST = {
            { "zh", "items.remains.brokeneye.desc", "明示符石" },
            { "en", "items.remains.brokeneye.desc", "stone of clairvoyance" },
            { "zh", "items.remains.ledgerpage.desc", "力量" },
            { "en", "items.remains.ledgerpage.desc", "strength" },
    };

    public static void main(String[] args) throws Exception {
        Properties zh = load(DIR + "items_zh.properties");
        Properties en = load(DIR + "items.properties");

        for (String k : NEW_KEYS) {
            for (Object[] side : new Object[][]{ { "zh", zh }, { "en", en } }) {
                String tag = side[0] + " " + k;
                String v = ((Properties) side[1]).getProperty(k);
                if (v == null) { fail("[MISS] " + tag); continue; }
                if (v.trim().isEmpty()) { fail("[EMPTY] " + tag); continue; }
                if (v.indexOf('\uFFFD') >= 0) fail("[REPL] " + tag + " 含 U+FFFD");
                if (v.contains("/n/")) fail("[NL] " + tag + " 含误写的 /n/");
                int us = 0;
                for (int i = 0; i < v.length(); i++) if (v.charAt(i) == '_') us++;
                if (us % 2 != 0) fail("[MARK] " + tag + " 下划线标记为奇数个(" + us + ")： " + v);
            }
        }

        // —— 中文名硬断言 ——
        for (String[] e : ZH_NAMES) {
            String v = zh.getProperty(e[0]);
            if (v == null || !v.equals(e[1])) {
                fail("[NAME] zh " + e[0] + " 期望「" + e[1] + "」实为「" + v + "」");
            }
        }

        // —— 描述语义断言 ——
        for (String[] e : DESC_MUST) {
            Properties p = e[0].equals("zh") ? zh : en;
            String v = p.getProperty(e[1]);
            if (v == null) { fail("[MISS] " + e[0] + " " + e[1]); continue; }
            if (!v.toLowerCase().contains(e[2].toLowerCase())) {
                fail("[DESC] " + e[0] + " " + e[1] + " 未点名「" + e[2] + "」：" + v);
            }
        }

        // —— 回归：既有遗物名称键还在 ——
        for (String k : OLD_NAME_KEYS) {
            for (Object[] side : new Object[][]{ { "zh", zh }, { "en", en } }) {
                String v = ((Properties) side[1]).getProperty(k);
                if (v == null || v.trim().isEmpty()) fail("[REG] " + side[0] + " " + k + " 缺失/为空");
            }
        }

        System.out.println("=== 新增文案 ===");
        for (Properties p : new Properties[]{ zh, en }) {
            String tag = (p == zh ? "zh" : "en");
            for (String k : NEW_KEYS) {
                System.out.println("[" + tag + "] " + k + " -> " + p.getProperty(k));
            }
        }

        int total = (NEW_KEYS.length + OLD_NAME_KEYS.length) * 2;
        System.out.println("=== 检查完毕 ===");
        System.out.println(fail == 0 ? ("ALL PASS (" + NEW_KEYS.length + " 新键 + " + OLD_NAME_KEYS.length + " 回归键, x2 语言 = " + total + ")") : ("共 " + fail + " 处问题"));
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
