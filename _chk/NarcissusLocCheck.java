import java.io.*;
import java.util.*;

/**
 * 水仙十字圣剑（三形态武器）+ 两个新 buff 的文本实装载核验（2026-09-20）。
 *
 * 覆盖：
 *  ① 新键（zh + en）非空、无 U+FFFD、无 `/n/` 误写、`_` 标记成对；
 *  ② **真 String.format**：两条带参文本（护盾剩余数 %d / 冷却剩余 %s）用真实参数跑一遍
 *     —— 占位符数量不符、字面 `%` 没写 `%%` 都会在这里直接崩；
 *  ③ 固定对象名硬断言：三个形态的中文名、两个 buff 的中文名（这些是用户点名的固定名字，
 *     可以硬断言；效果描述属可重写文案，只做 ①② 的通用检查）；
 *  ④ 回归：`meleeweapon.stats_desc` 仍是空串（常态武器不写 stats_desc 时靠它回退成「不显示」，
 *     这条一断，所有没写 stats_desc 的武器都会变成 !!!NO TEXT FOUND!!!）；
 *     以及邻键（黑天鹅 / 猩红创痕）没被本次插入碰坏 —— **zh 侧严格断言**；
 *     en 侧只做「存在则必须非空」的软断言：本项目英文侧对早期魔改武器（黑天鹅 / 猩红创痕）
 *     尚未补齐，属已知状态，不能拿它当作失败。
 *
 * 为什么必须用真 Properties：孤立反斜杠 / 十六进制转义写错会在 load 直接崩，
 * 或把两行并成一行（「文本只剩第一段」那类断行事故）。正则看不出来。
 *
 * 运行：java -Dfile.encoding=UTF-8 _chk/NarcissusLocCheck.java
 */
public class NarcissusLocCheck {

    static int fail = 0;

    static void fail(String msg) {
        fail++;
        System.out.println("FAIL  " + msg);
    }

    static final String ITEMS_DIR  = "D:/PD/core/src/main/assets/messages/items/";
    static final String ACTORS_DIR = "D:/PD/core/src/main/assets/messages/actors/";

    /** 无参文本键：拿到就直接显示，不走 String.format。 */
    static final String[] ITEM_KEYS = {
            "items.weapon.melee.narcissuscrosssword.name",
            "items.weapon.melee.narcissuscrosssword.stats_desc",
            "items.weapon.melee.narcissuscrosssword.desc",
            "items.weapon.melee.narcissusswordmang.name",
            "items.weapon.melee.narcissusswordmang.stats_desc",
            "items.weapon.melee.narcissusswordmang.desc",
            "items.weapon.melee.narcissusswordmang.counter",
            "items.weapon.melee.narcissusswordhuang.name",
            "items.weapon.melee.narcissusswordhuang.stats_desc",
            "items.weapon.melee.narcissusswordhuang.desc",
            "items.weapon.melee.narcissusswordhuang.hollow_strike",
    };

    static final String[] ACTOR_KEYS = {
            "actors.buffs.narcissusshield.name",
            "actors.buffs.narcissusshield.desc",
            "actors.buffs.narcissuscrosscooldown.name",
            "actors.buffs.narcissuscrosscooldown.desc",
    };

    /** 带参文本：{键, 实参类型}；I=int（%d），S=String（%s）。 */
    static final String[][] FMT_KEYS = {
            { "actors.buffs.narcissusshield.desc", "I" },
            { "actors.buffs.narcissuscrosscooldown.desc", "S" },
    };

    /** 固定对象名（用户点名的名字 ⇒ 可硬断言）。 */
    static final String[][] ZH_NAMES = {
            { "items.weapon.melee.narcissuscrosssword.name", "水仙十字圣剑" },
            { "items.weapon.melee.narcissusswordmang.name", "水仙十字圣剑·芒性" },
            { "items.weapon.melee.narcissusswordhuang.name", "水仙十字圣剑·荒性" },
            { "actors.buffs.narcissusshield.name", "芒性护盾" },
            { "actors.buffs.narcissuscrosscooldown.name", "圣剑冷却" },
    };

    public static void main(String[] args) throws Exception {
        Properties itemZh  = load(ITEMS_DIR  + "items_zh.properties");
        Properties itemEn  = load(ITEMS_DIR  + "items.properties");
        Properties actorZh = load(ACTORS_DIR + "actors_zh.properties");
        Properties actorEn = load(ACTORS_DIR + "actors.properties");

        Properties[] itemSides  = { itemZh, itemEn };
        Properties[] actorSides = { actorZh, actorEn };

        // —— ① 通用体检 ——
        for (Object[] side : new Object[][]{ { "zh", itemZh }, { "en", itemEn } }) {
            for (String k : ITEM_KEYS) check(tag(side[0], k), ((Properties) side[1]).getProperty(k));
        }
        for (Object[] side : new Object[][]{ { "zh", actorZh }, { "en", actorEn } }) {
            for (String k : ACTOR_KEYS) check(tag(side[0], k), ((Properties) side[1]).getProperty(k));
        }

        // —— ② 带参文本：真 String.format ——
        for (Object[] side : new Object[][]{ { "zh", actorZh }, { "en", actorEn } }) {
            Properties p = (Properties) side[1];
            for (String[] e : FMT_KEYS) {
                String key = e[0];
                String v = p.getProperty(key);
                if (v == null) { fail("[MISS] " + tag(side[0], key)); continue; }
                Object arg = e[1].equals("I") ? (Object) Integer.valueOf(37) : (Object) "12";
                try {
                    String out = String.format(v, arg);
                    if (out.indexOf('%') >= 0 && out.contains("%d")) {
                        fail("[FMT] " + tag(side[0], key) + " 格式化后仍残留 %d：" + out);
                    }
                    System.out.println("  [fmt] " + side[0] + " " + key + " -> " + out.replace('\n', '|'));
                } catch (Exception ex) {
                    fail("[FMT] " + tag(side[0], key) + " String.format 抛异常：" + ex);
                }
            }
        }

        // —— ③ 固定中文名 ——
        for (String[] e : ZH_NAMES) {
            Properties p = e[0].startsWith("actors.") ? actorZh : itemZh;
            String v = p.getProperty(e[0]);
            if (v == null || !v.equals(e[1])) {
                fail("[NAME] zh " + e[0] + " 期望「" + e[1] + "」实为「" + v + "」");
            }
        }

        // —— ④ 回归：meleeweapon.stats_desc 仍是空串 ——
        for (Object[] side : new Object[][]{ { "zh", itemZh }, { "en", itemEn } }) {
            String v = ((Properties) side[1]).getProperty("items.weapon.melee.meleeweapon.stats_desc");
            if (v == null || v.length() != 0) {
                fail("[REG] " + side[0] + " meleeweapon.stats_desc 不再是空串（实为「" + v
                        + "」）⇒ 未写 stats_desc 的武器会显示垃圾文本");
            }
        }
        // 敌对回归：这两个键必须存在且非空，否则本次新增把邻键碰坏了也看不出来。
        // zh 侧严格（权威文案）；en 侧本项目对早期魔改武器尚未补齐 ⇒ 只查「存在则非空」。
        int enGap = 0;
        for (String k : new String[]{ "items.weapon.melee.blackswan.name",
                                      "items.weapon.melee.crimsonscar.name" }) {
            String zv = itemZh.getProperty(k);
            if (zv == null || zv.trim().isEmpty()) fail("[REG] zh " + k + " 缺失/为空");
            String ev = itemEn.getProperty(k);
            if (ev == null) enGap++;
            else if (ev.trim().isEmpty()) fail("[REG] en " + k + " 存在但为空");
        }
        if (enGap > 0) {
            System.out.println("NOTE  英文侧尚有 " + enGap + " 个早期魔改武器名未补齐（已知状态，不计失败）");
        }

        System.out.println("=== 三层新增文本 ===");
        for (Object[] side : new Object[][]{ { "zh", itemSides[0] }, { "en", itemSides[1] } }) {
            Properties p = (Properties) side[1];
            for (String k : ITEM_KEYS) System.out.println("[" + side[0] + "] " + k + " -> " + p.getProperty(k));
        }
        for (Object[] side : new Object[][]{ { "zh", actorSides[0] }, { "en", actorSides[1] } }) {
            Properties p = (Properties) side[1];
            for (String k : ACTOR_KEYS) System.out.println("[" + side[0] + "] " + k + " -> " + p.getProperty(k));
        }

        System.out.println("=== 检查完毕 ===");
        int total = (ITEM_KEYS.length + ACTOR_KEYS.length) * 2 + FMT_KEYS.length * 2 + ZH_NAMES.length
                + 2 /* meleeweapon.stats_desc */ + 2 /* 敌对回归 zh 严格（en 侧为软断言） */;
        System.out.println(fail == 0 ? ("ALL PASS（共 " + total + " 条断言）") : ("共 " + fail + " 处问题"));
        if (fail != 0) System.exit(1);
    }

    static String tag(Object lang, String key) {
        return lang + " " + key;
    }

    static void check(String tag, String v) {
        if (v == null) { fail("[MISS] " + tag); return; }
        if (v.trim().isEmpty()) { fail("[EMPTY] " + tag); return; }
        if (v.indexOf('\uFFFD') >= 0) fail("[REPL] " + tag + " 含 U+FFFD");
        if (v.contains("/n/")) fail("[NL] " + tag + " 含误写的 /n/");
        int us = 0;
        for (int i = 0; i < v.length(); i++) if (v.charAt(i) == '_') us++;
        if (us % 2 != 0) fail("[MARK] " + tag + " 下划线标记为奇数个(" + us + ")： " + v);
        int til = 0;
        for (int i = 0; i + 1 < v.length(); i++) if (v.charAt(i) == '~' && v.charAt(i + 1) == '~') { til++; i++; }
        if (til % 2 != 0) fail("[MARK] " + tag + " 删除线标记为奇数个(" + til + ")： " + v);
    }

    static Properties load(String path) throws Exception {
        Properties p = new Properties();
        try (Reader r = new InputStreamReader(new FileInputStream(path), "UTF-8")) {
            p.load(r);
        }
        return p;
    }
}
