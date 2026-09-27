import java.io.*;
import java.util.*;

/**
 * 本轮（即刻处刑[莱瓦汀] + 3 个 T4 天赋 + 莱瓦汀解放 buff）的文本实装载核验。
 *
 * 比正则核验强的地方：用 java.util.Properties 真解析整份文件（孤立反斜杠 / 十六进制转义写错
 * 会在这里直接崩或把两行并成一行），再用真实的参数类型跑一遍 String.format。
 *
 * 运行：java _chk/InstExecLocCheck.java
 */
public class InstExecLocCheck {

    static int fail = 0;

    static void fail(String msg) {
        fail++;
        System.out.println("FAIL  " + msg);
    }

    static final String DIR = "D:/PD/core/src/main/assets/messages/actors/";

    /** 键 -> Java 侧实际传入的实参（null = 不分式化；{@code STALE} = 必须已删除） */
    static final Object[] NO_ARGS = null;
    static final Object[] STALE = new Object[0];

    static final LinkedHashMap<String, Object[]> KEYS = new LinkedHashMap<>();
    static {
        // InstantExecution（无参的都是 Messages.get(this, key)，只有 devour 传 2 个 int）
        KEYS.put("actors.hero.abilities.middlefinger.instantexecution.name", NO_ARGS);
        KEYS.put("actors.hero.abilities.middlefinger.instantexecution.short_desc", NO_ARGS);
        KEYS.put("actors.hero.abilities.middlefinger.instantexecution.desc", NO_ARGS);
        KEYS.put("actors.hero.abilities.middlefinger.instantexecution.prompt", NO_ARGS);
        KEYS.put("actors.hero.abilities.middlefinger.instantexecution.bad_target", NO_ARGS);
        KEYS.put("actors.hero.abilities.middlefinger.instantexecution.too_far", NO_ARGS);
        KEYS.put("actors.hero.abilities.middlefinger.instantexecution.no_laevateinn", NO_ARGS);
        KEYS.put("actors.hero.abilities.middlefinger.instantexecution.cast", NO_ARGS);
        KEYS.put("actors.hero.abilities.middlefinger.instantexecution.devour", new Object[]{ 137, 205 });
        // 三个 T4 天赋（Talent.title()/desc() 都不带参）
        KEYS.put("actors.hero.talent.no_ledger_needed.title", NO_ARGS);
        KEYS.put("actors.hero.talent.no_ledger_needed.desc", NO_ARGS);
        KEYS.put("actors.hero.talent.overdue_release.title", NO_ARGS);
        KEYS.put("actors.hero.talent.overdue_release.desc", NO_ARGS);
        KEYS.put("actors.hero.talent.legendary_blade.title", NO_ARGS);
        KEYS.put("actors.hero.talent.legendary_blade.desc", NO_ARGS);
        // 莱瓦汀解放 buff（desc() 传 (int) 等级 + dispTurns() 一个 String）
        KEYS.put("actors.buffs.executionunleashed.name", NO_ARGS);
        KEYS.put("actors.buffs.executionunleashed.desc", new Object[]{ 3, "17" });
        // 旧的占位键必须已经清掉
        KEYS.put("actors.hero.abilities.middlefinger.middlefingerabilitythree.name", STALE);
        KEYS.put("actors.hero.abilities.middlefinger.middlefingerabilitythree.desc", STALE);
    }

    public static void main(String[] args) throws Exception {
        Properties zh = load(DIR + "actors_zh.properties");
        Properties en = load(DIR + "actors.properties");

        for (Map.Entry<String, Object[]> e : KEYS.entrySet()) {
            String k = e.getKey();
            Object[] fmt = e.getValue();

            for (Object[] side : new Object[][]{ {"zh", zh}, {"en", en} }) {
                String tag = side[0] + " " + k;
                String v = ((Properties) side[1]).getProperty(k);

                if (fmt == STALE) {
                    if (v != null) fail("[STALE] " + tag + " 占位键仍然存在");
                    continue;
                }
                if (v == null) { fail("[MISS] " + tag); continue; }
                if (v.isEmpty()) { fail("[EMPTY] " + tag); continue; }

                if (fmt == NO_ARGS) {
                    // 不分式化的键：出现形似占位符的写法才可疑
                    if (v.matches("(?s).*%\\d+\\$.*") || v.matches("(?s).*%[sd\\d].*")) {
                        fail("[FMT] " + tag + " 不分式化却含占位符: " + v);
                    }
                } else {
                    String out;
                    try {
                        out = String.format(v, fmt);
                    } catch (Exception ex) {
                        fail("[FMT] " + tag + " 格式化抛异常: " + ex);
                        continue;
                    }
                    if (out.contains("%")) fail("[FMT] " + tag + " 格式化后仍有裸 %: " + out);
                    if (out.equals(v)) fail("[FMT] " + tag + " 格式化后与原文相同(未被替换): " + v);
                    //实参必须逐个出现（防「值被截断成只剩第一段」这类断行事故）
                    for (Object a : fmt) {
                        if (!out.contains(String.valueOf(a))) {
                            fail("[FMT] " + tag + " 格式化结果里找不到实参 " + a + " : " + out);
                        }
                    }
                }
            }
        }

        // 顺带演示格式化结果
        System.out.println("=== 格式化结果 ===");
        for (Properties p : new Properties[]{ zh, en }) {
            System.out.println("[" + (p == zh ? "zh" : "en") + "] devour  -> "
                    + String.format(p.getProperty("actors.hero.abilities.middlefinger.instantexecution.devour"), 137, 205));
            System.out.println("[" + (p == zh ? "zh" : "en") + "] unleash -> "
                    + String.format(p.getProperty("actors.buffs.executionunleashed.desc"), 3, "17"));
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
