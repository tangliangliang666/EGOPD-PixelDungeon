import java.io.*;
import java.util.*;

/** 用 java.util.Properties 实装载 + 按真实参数类型 String.format 实跑，验证新增文本。 */
public class LocCheck {
    public static void main(String[] args) throws Exception {
        String base = "D:/PD/core/src/main/assets/messages/actors/";
        check(base + "actors_zh.properties", true);
        check(base + "actors.properties", false);

        String ibase = "D:/PD/core/src/main/assets/messages/items/";
        checkItem(ibase + "items_zh.properties", true);
        checkItem(ibase + "items.properties", false);

        System.out.println("=== 结果演示 ===");
        Properties zh = load(base + "actors_zh.properties");
        System.out.println("[zh melting]        " + String.format(zh.getProperty("actors.buffs.melting.desc"), 150, "5"));
        System.out.println("[zh familybetrayal] " + String.format(zh.getProperty("actors.buffs.familybetrayal.desc"), 7, "1"));
        System.out.println("[zh grip]           " + zh.getProperty("actors.hero.talent.effortless_grip.desc"));
        System.out.println("[zh unwrap]         " + zh.getProperty("actors.hero.talent.unwrap.desc"));
        Properties en = load(base + "actors.properties");
        System.out.println("[en melting]        " + String.format(en.getProperty("actors.buffs.melting.desc"), 150, "5"));
        System.out.println("[en familybetrayal] " + String.format(en.getProperty("actors.buffs.familybetrayal.desc"), 7, "1"));
    }

    static Properties load(String path) throws Exception {
        Properties p = new Properties();
        try (Reader r = new InputStreamReader(new FileInputStream(path), "UTF-8")) {
            p.load(r); // 若 \n 转义写错（真换行/孤立反斜杠）会在这里抛异常
        }
        return p;
    }

    static void check(String path, boolean zh) throws Exception {
        String name = new File(path).getName();
        try {
            Properties p = load(path);
            // 新键：逐条实取
            String[] keys = {
                "actors.hero.herosubclass.family_betrayer",
                "actors.hero.herosubclass.family_betrayer_short_desc",
                "actors.hero.herosubclass.family_betrayer_desc",
                "actors.hero.talent.effortless_grip.title",
                "actors.hero.talent.effortless_grip.desc",
                "actors.hero.talent.melt_to_death.title",
                "actors.hero.talent.melt_to_death.desc",
                "actors.hero.talent.unwrap.title",
                "actors.hero.talent.unwrap.desc",
                "actors.buffs.melting.name",
                "actors.buffs.melting.desc",
                "actors.buffs.familybetrayal.name",
                "actors.buffs.familybetrayal.desc",
            };
            int nulls = 0;
            for (String k : keys) if (p.getProperty(k) == null) { System.out.println("[" + name + "] 缺键 " + k); nulls++; }
            if (nulls == 0) System.out.println("[" + name + "] OK：13 个新键全部装载成功");
            // 实跑带参两条
            String m = String.format(p.getProperty("actors.buffs.melting.desc"), 150, "5");
            String f = String.format(p.getProperty("actors.buffs.familybetrayal.desc"), 7, "1");
            if (m.contains("%") || f.contains("%")) System.out.println("[" + name + "] 格式化后仍有裸 %：" + m + " | " + f);
        } catch (Exception e) {
            System.out.println("[" + name + "] 装载失败: " + e);
        }
    }

    static void checkItem(String path, boolean zh) throws Exception {
        String name = new File(path).getName();
        try {
            Properties p = load(path);
            String v = p.getProperty("items.weapon.melee.sealedswordbase.level_note");
            System.out.println("[" + name + "] level_note " + (v == null ? "缺失" : "OK (" + v.length() + " 字符)"));
            String s = p.getProperty("items.weapon.melee.laevateinn.stats_desc");
            System.out.println("[" + name + "] laevateinn.stats_desc " + (s == null ? "缺失" : "OK"));
        } catch (Exception e) {
            System.out.println("[" + name + "] 装载失败: " + e);
        }
    }
}
