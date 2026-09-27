import java.io.*;
import java.util.*;

/**
 * 封印之剑系列（含莱瓦汀）的物品文本实装载核验（2026-09-18）。
 *
 * 覆盖两件事：
 *  ① 本轮改动的 `sealedswordbase.level_note` —— 新增「注魔秘卷 / 附魔符石 / 强化符石 仍然有效」一句，
 *     并断言它确实点名了三件物品（否则玩家会以为连附魔也一起被挡）；
 *  ② 顺带把四个形态的物品文本（name / desc / stats_desc）与系列动作文本全部过一遍，
 *     确认改 properties 时没有把别的键碰坏。
 *
 * 比正则强的地方：用 java.util.Properties 真解析整份文件（孤立反斜杠 / 十六进制转义写错会在这里直接崩
 * 或把两行并成一行 —— 「文本只剩第一段」那类断行事故就是这么发生的），再用真实参数类型跑 String.format。
 *
 * 运行：java _chk/SealedSwordLocCheck.java
 */
public class SealedSwordLocCheck {

    static int fail = 0;

    static void fail(String msg) {
        fail++;
        System.out.println("FAIL  " + msg);
    }

    static final String DIR = "D:/PD/core/src/main/assets/messages/items/";

    static final Object[] NO_ARGS = null;

    static final LinkedHashMap<String, Object[]> KEYS = new LinkedHashMap<>();
    static {
        // —— 系列基类：动作与提示（只有 need_charge_or_hp / unsealed / swapped 带参）——
        KEYS.put("items.weapon.melee.sealedswordbase.level_note", NO_ARGS);
        KEYS.put("items.weapon.melee.sealedswordbase.ac_unseal", NO_ARGS);
        KEYS.put("items.weapon.melee.sealedswordbase.ac_swap", NO_ARGS);
        KEYS.put("items.weapon.melee.sealedswordbase.need_main_hand", NO_ARGS);
        KEYS.put("items.weapon.melee.sealedswordbase.need_charge_or_hp", new Object[]{ 50, 50 });
        KEYS.put("items.weapon.melee.sealedswordbase.bound", NO_ARGS);
        KEYS.put("items.weapon.melee.sealedswordbase.full", NO_ARGS);
        KEYS.put("items.weapon.melee.sealedswordbase.unsealed", new Object[]{ "莱瓦汀 / Laevateinn" });
        KEYS.put("items.weapon.melee.sealedswordbase.swapped", new Object[]{ "指虎 / Knuckle Duster" });
        KEYS.put("items.weapon.melee.sealedswordbase.free_swap", NO_ARGS);

        // —— 四个形态的 名 / 风味 / 面板说明 ——
        for (String form : new String[]{ "sealedsword", "unsealedsword", "unsealedsword2", "laevateinn" }) {
            for (String suffix : new String[]{ "name", "desc", "stats_desc" }) {
                KEYS.put("items.weapon.melee." + form + "." + suffix, NO_ARGS);
            }
        }
    }

    /** level_note 必须点名的三件物品（修的是「选不中」，文案必须让玩家知道现在能选中）。 */
    static final String[][] LEVEL_NOTE_MUST = {
            { "zh", "注魔秘卷" }, { "zh", "附魔符石" }, { "zh", "强化符石" },
            { "en", "scrolls of enchantment" }, { "en", "stones of enchantment" }, { "en", "stones of augmentation" },
    };

    /** 顺带确认「不能吃升级卷轴」这句还在（本系列的核心限制，不能被这轮改动写丢）。 */
    static final String[][] LEVEL_NOTE_KEEPS = {
            { "zh", "升级卷轴" }, { "zh", "22" },
            { "en", "scrolls of upgrade" }, { "en", "22" },
    };

    public static void main(String[] args) throws Exception {
        Properties zh = load(DIR + "items_zh.properties");
        Properties en = load(DIR + "items.properties");

        for (Map.Entry<String, Object[]> e : KEYS.entrySet()) {
            String k = e.getKey();
            Object[] fmt = e.getValue();

            for (Object[] side : new Object[][]{ { "zh", zh }, { "en", en } }) {
                String tag = side[0] + " " + k;
                String v = ((Properties) side[1]).getProperty(k);
                if (v == null) { fail("[MISS] " + tag); continue; }
                if (v.isEmpty()) { fail("[EMPTY] " + tag); continue; }
                if (v.indexOf('\uFFFD') >= 0) fail("[REPL] " + tag + " 含 U+FFFD");
                if (v.contains("/n/")) fail("[NL] " + tag + " 含误写的 /n/");

                //斜体标记 `_` 必须成对：奇数个会把后面半句整段吞成斜体
                int us = 0;
                for (int i = 0; i < v.length(); i++) if (v.charAt(i) == '_') us++;
                if (us % 2 != 0) fail("[MARK] " + tag + " 下划线标记为奇数个(" + us + ")： " + v);

                if (fmt == NO_ARGS) {
                    //不分式化的键：出现形似占位符的写法才可疑（本系列只有 3 个键带参）
                    if (v.matches("(?s).*%\\d+\\$.*")) fail("[FMT] " + tag + " 不分式化却含 %n$ 占位符: " + v);
                } else {
                    String out;
                    try {
                        out = String.format(v, fmt);
                    } catch (Exception ex) {
                        fail("[FMT] " + tag + " 格式化抛异常: " + ex);
                        continue;
                    }
                    if (out.matches("(?s).*%\\d+\\$.*") || out.matches("(?s).*%[sdf].*")) {
                        fail("[FMT] " + tag + " 格式化后仍有未替换的占位符: " + out);
                    }
                    if (out.equals(v)) fail("[FMT] " + tag + " 格式化后与原文相同(未被替换): " + v);
                    for (Object a : fmt) {
                        if (!out.contains(String.valueOf(a))) {
                            fail("[FMT] " + tag + " 格式化结果里找不到实参 " + a + " : " + out);
                        }
                    }
                }
            }
        }

        // —— level_note 的语义断言 ——
        for (String[] must : LEVEL_NOTE_MUST) {
            Properties p = must[0].equals("zh") ? zh : en;
            String v = p.getProperty("items.weapon.melee.sealedswordbase.level_note");
            if (v != null && !v.contains(must[1])) {
                fail("[NOTE] " + must[0] + " level_note 未点名「" + must[1]
                        + "」—— 玩家会以为附魔/强化也一并被挡");
            }
        }
        for (String[] keep : LEVEL_NOTE_KEEPS) {
            Properties p = keep[0].equals("zh") ? zh : en;
            String v = p.getProperty("items.weapon.melee.sealedswordbase.level_note");
            if (v != null && !v.contains(keep[1])) {
                fail("[NOTE] " + keep[0] + " level_note 丢了「" + keep[1] + "」这句（本系列的核心限制）");
            }
        }

        System.out.println("=== 改后文案 ===");
        for (Properties p : new Properties[]{ zh, en }) {
            System.out.println("[" + (p == zh ? "zh" : "en") + "] level_note -> "
                    + p.getProperty("items.weapon.melee.sealedswordbase.level_note"));
        }

        System.out.println("=== 检查完毕 ===");
        System.out.println(fail == 0 ? ("ALL PASS (" + KEYS.size() + " 个键 x 2 语言)") : ("共 " + fail + " 处问题"));
        if (fail != 0) System.exit(1);
    }

    static Properties load(String path) throws Exception {
        Properties p = new Properties();
        try (Reader r = new InputStreamReader(new FileInputStream(path), "UTF-8")) {
            p.load(r); // 转义写错会在这里抛异常
        }
        return p;
    }
}
