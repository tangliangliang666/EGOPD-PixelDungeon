import java.io.*;
import java.util.*;

/**
 * 职业介绍文本（heroclass.*）实装载核验。
 *
 * 比正则强的地方：用 java.util.Properties 真解析整份文件（孤立反斜杠 / 十六进制转义写错会在这里直接崩
 * 或把两行并成一行），再检查 RenderedTextBlock 的两种标记是否成对。
 *
 * 运行：java _chk/ClassDescCheck.java
 */
public class ClassDescCheck {

    static int fail = 0;

    static void fail(String msg) {
        fail++;
        System.out.println("FAIL  " + msg);
    }

    static final String ZH = "D:/PD/core/src/main/assets/messages/actors/actors_zh.properties";
    static final String EN = "D:/PD/core/src/main/assets/messages/actors/actors.properties";

    public static void main(String[] args) throws Exception {
        for (String path : new String[]{ ZH, EN }) {
            Properties p = new Properties();
            try (Reader r = new InputStreamReader(new FileInputStream(path), "UTF-8")) {
                p.load(r);
            } catch (Exception e) {
                fail(path + " 解析失败: " + e);
                continue;
            }
            System.out.println("--- " + path + "（键数 " + p.size() + "）---");

            // 中指长兄四个键必须存在且非空
            String[] keys = {
                    "actors.hero.heroclass.middle_finger",
                    "actors.hero.heroclass.middle_finger_desc_short",
                    "actors.hero.heroclass.middle_finger_desc",
                    "actors.hero.heroclass.middle_finger_unlock",
            };
            for (String k : keys) {
                String v = p.getProperty(k);
                if (v == null || v.trim().isEmpty()) {
                    fail("缺键或空值: " + k);
                    continue;
                }
                System.out.println("[" + k.substring(k.lastIndexOf('.') + 1) + "] " + v.replace("\n", "⏎"));
                // RenderedTextBlock 的标记：'_' 与 '~~' 都必须成对
                int underscore = 0;
                for (char c : v.toCharArray()) if (c == '_') underscore++;
                if (underscore % 2 != 0) fail(k + " 的 '_' 标记不成对（" + underscore + " 个）");
                int tilde = 0;
                for (int i = 0; i + 1 < v.length(); i++) if (v.charAt(i) == '~' && v.charAt(i + 1) == '~') { tilde++; i++; }
                if (tilde % 2 != 0) fail(k + " 的 '~~' 标记不成对（" + tilde + " 个）");
                if (v.indexOf('\uFFFD') >= 0) fail(k + " 含 U+FFFD");

                // 中指长兄的职业介绍必须是「三段式」：
                //   ① 基础特色机制（复仇账簿/仇怨 + 封印之剑解封）
                //   ② 初始携带的物品
                //   ③ 开局鉴定的物品（三条清单）
                // 段数同时受 WndHeroInfo.HeroInfoTab 约束：icons[i] 与 desc_entries[i] 一一配对，
                // 段数多于图标数会直接数组越界。
                if (k.endsWith("middle_finger_desc")) {
                    String[] parts = v.split("\n\n");
                    if (parts.length != 3) {
                        fail(k + " 段数应为 3（机制 / 初始携带 / 开局鉴定），实为 " + parts.length);
                    } else {
                        boolean zh = path.equals(ZH);
                        // ① 机制段：只要求「确实在讲本职业的招牌机制」，不硬编码可选措辞。
                        //    ⚠️ 别把 buff 名这类词写进断言（如「仇怨」/「Rancor」）——用户自己重写文案时
                        //    会把它删掉，断言就会跟着一起红（2026-09-18 实际踩到一次）。只认「稳定的概念名」：
                        //    神器与武器是固定游戏对象，名字不会随文案改动。
                        String[] mechanics = zh ? new String[]{ "复仇账簿", "封印之剑" }
                                                : new String[]{ "Revenge Ledger", "Sealed Sword" };
                        boolean mentionsMechanic = false;
                        for (String must : mechanics) if (parts[0].contains(must)) mentionsMechanic = true;
                        if (!mentionsMechanic) {
                            fail(k + " 第 1 段不像「核心机制」段（未出现 " + String.join(" / ", mechanics) + "）");
                        }
                        // ② 初始携带段：必须有「携带」字样
                        if (!parts[1].contains(zh ? "携带" : "starts with")) fail(k + " 第 2 段不像「初始携带」段");
                        // ③ 开局鉴定段：三条 _-_ 清单
                        int bullets = 0;
                        for (String line : parts[2].split("\n")) if (line.startsWith("_-_")) bullets++;
                        if (bullets != 3) fail(k + " 第 3 段的鉴定清单应为 3 条，实为 " + bullets);
                        for (String must : (zh ? new String[]{ "力量", "复仇", "恐惧" }
                                              : new String[]{ "Strength", "Retribution", "Terror" })) {
                            if (!parts[2].contains(must)) fail(k + " 第 3 段缺少鉴定物品: " + must);
                        }
                    }
                }
            }

            // 不得再残留「沿用战士 / placeholder for the Warrior」这类陈述
            String desc = p.getProperty("actors.hero.heroclass.middle_finger_desc", "");
            String shortDesc = p.getProperty("actors.hero.heroclass.middle_finger_desc_short", "");
            for (String bad : new String[]{ "暂时沿用战士", "暂用战士", "仍在设计中", "placeholder for the Warrior", "still being designed", "borrowed from the Warrior" }) {
                if (desc.contains(bad) || shortDesc.contains(bad)) fail("残留旧占位文案: " + bad);
            }
        }

        // 全文件行级自检：不得出现「缺 = 的非空非注释行」（properties 断行的典型症状）
        for (String path : new String[]{ ZH, EN }) {
            int lineNo = 0;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    lineNo++;
                    String t = line.trim();
                    if (t.isEmpty() || t.startsWith("#") || t.startsWith("!")) continue;
                    if (t.indexOf('=') < 0) fail(path + " 第 " + lineNo + " 行缺 '='（疑似断行）: " + t);
                }
            }
        }

        System.out.println();
        System.out.println(fail == 0 ? "=== 全部通过 ===" : "=== 失败 " + fail + " 项 ===");
    }
}
