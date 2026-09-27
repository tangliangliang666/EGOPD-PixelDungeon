import java.io.*;
import java.util.*;

import com.shatteredpixel.shatteredpixeldungeon.Trials;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTrials;

/**
 * 考验系统（占位版）文本 + 常量实装载核验（2026-09-23）。
 *
 * 覆盖：
 *  ① `Trials` 的常量骨架：10 条、位掩码是 1<<i 且互不相同、MAX_VALUE/MAX_TRIALS 自洽；
 *  ② 图标表 ICON_W/ICON_H 与 tree.png 的单帧约束（每条 ≤16、10 帧排得进一行）；
 *     **已绘制的第 2~5 帧（1 基）尺寸必须等于实测值**（15x13 / 15x14 / 15x14 / 13x16）；
 *  ③ 文本键：`trials.<id>` / `trials.<id>_desc`（zh + en）非空、无 U+FFFD、无 `/n/` 误写、`_` 成对；
 *  ④ **用真实类名反推键名**（`Trials.class` / `WndTrials.class` 去前缀小写）——专抓「键名拼写/大小写写错
 *     导致界面显示成键名、且不报错」这一类；
 *  ⑤ 硬断言用户点名的 10 个名称（KETER…MALKUTH）与顺序；
 *  ⑥ 回归：挑战那边的同名邻键（`challenges.champion_enemies` / `windows.wndchallenges.title`）没被碰坏；
 *  ⑦ **HOKMA（智慧）的延迟上下限**：三个数值常量与门控位，`trials.hokma_desc` 必须能解析出**真换行**
 *     （证明属性文件里写的是字面 `\n` 且 entry 没被截成两段）、占位文案已被真效果替换，
 *     以及两个受影响天赋（手起刀落 / 夺命余势）的 `.hokma` 加注键存在且原有 `.desc` 没被吃掉
 *     —— 键名同样由真实类名 + 枚举常量名反推。
 *  ⑧ **BINAH（理解）的生成时等级规则**：位号与门控位、`Item.random()` 在**运行期确实是 final**
 *     （反射读 ACC_FINAL —— 这条只有真编译产物才答得出来，是 `verify_binah_gen.py` 源码层断言的
 *     交叉验证）、`Item.randomRaw()` 存在、`trials.binah_desc` 必须解析出真换行且不再是占位文案。
 *     注意：本项要用**新编的** class，所以 `_chk/_javachk` 必须排在 classpath 最前。
 *
 * 运行（`_chk/_javachk` 必须排 classpath 最前，否则读到旧产物、看不到新符号）：
 *   javac -d _chk/_javachk Trials.java WndTrials.java …
 *   java -cp "_chk/_javachk;core/build/classes/java/main;…" _chk/TrialsLocCheck.java
 */
public class TrialsLocCheck {

    static int fail = 0;
    static int pass = 0;

    static void chk(boolean cond, String msg) {
        if (cond) { pass++; }
        else { fail++; System.out.println("FAIL  " + msg); }
    }

    static final String MISC_DIR = "D:/PD/core/src/main/assets/messages/misc/";
    static final String WIN_DIR = "D:/PD/core/src/main/assets/messages/windows/";
    static final String ACT_DIR = "D:/PD/core/src/main/assets/messages/actors/";

    /** 用户点名的 10 个质点名，顺序 = 树上从上到下 = 界面显示顺序。 */
    static final String[] EXPECT_NAMES = {
            "KETER", "HOKMA", "BINAH", "CHESED", "GEBURA",
            "TIPHERETH", "NETZACH", "HOD", "YESOD", "MALKUTH",
    };

    /** 已经画好的帧（0 基）与其帧内图标实测包围盒。 */
    static final int[][] DRAWN = { {1, 15, 13}, {2, 15, 14}, {3, 15, 14}, {4, 13, 16} };

    /** 本轮新增的窗口键。 */
    static final String[] WIN_KEYS = {
            "windows.wndtrials.title",
            "windows.wndgame.trials",
            "windows.wndgameinprogress.trials",
            "windows.wndvictorycongrats.trials",
    };

    static Properties load(String p) throws IOException {
        Properties pr = new Properties();
        try (Reader r = new InputStreamReader(new FileInputStream(p), "UTF-8")) {
            pr.load(r);
        }
        return pr;
    }

    public static void main(String[] args) throws Exception {
        Properties mz = load(MISC_DIR + "misc_zh.properties");
        Properties me = load(MISC_DIR + "misc.properties");
        Properties wz = load(WIN_DIR + "windows_zh.properties");
        Properties we = load(WIN_DIR + "windows.properties");
        Properties az = load(ACT_DIR + "actors_zh.properties");
        Properties ae = load(ACT_DIR + "actors.properties");

        //=== ① Trials 常量骨架 ===
        int n = Trials.NAME_IDS.length;
        chk(n == 10, "NAME_IDS 长度应为 10，实得 " + n);
        chk(Trials.MASKS.length == n, "MASKS 长度应与 NAME_IDS 一致");
        chk(Trials.ICON_W.length == n, "ICON_W 长度应与 NAME_IDS 一致");
        chk(Trials.ICON_H.length == n, "ICON_H 长度应与 NAME_IDS 一致");
        chk(Trials.MAX_TRIALS == n, "MAX_TRIALS 应等于考验条数 " + n + "，实得 " + Trials.MAX_TRIALS);
        chk(Trials.MAX_VALUE == (1 << n) - 1,
                "MAX_VALUE 应为 2^" + n + "-1 = " + ((1 << n) - 1) + "，实得 " + Trials.MAX_VALUE);
        chk(Trials.ICON_COLS == n, "ICON_COLS 应为 " + n + "（tree.png 一行 " + n + " 帧）");

        boolean bitsOk = true;
        HashSet<Integer> seen = new HashSet<>();
        for (int i = 0; i < n; i++) {
            if (Trials.MASKS[i] != (1 << i)) bitsOk = false;   // 常规考验占最低连续位
            if (!seen.add(Trials.MASKS[i])) bitsOk = false;     // 不得重复
        }
        chk(bitsOk, "MASKS 必须是各不相同的 1<<i（常规考验占最低连续位）");
        chk(Trials.MASKS[n - 1] == 512, "最后一条的位应为 512，实得 " + Trials.MASKS[n - 1]);

        //=== ② 图标表与 tree.png 单帧约束 ===
        boolean iconOk = true;
        StringBuilder iconBad = new StringBuilder();
        for (int i = 0; i < n; i++) {
            int x = (i % Trials.ICON_COLS) * Trials.ICON_FRAME;
            if (Trials.ICON_W[i] < 1 || Trials.ICON_W[i] > Trials.ICON_FRAME) {
                iconOk = false; iconBad.append(" 帧").append(i).append(" 宽越界=").append(Trials.ICON_W[i]);
            }
            if (Trials.ICON_H[i] < 1 || Trials.ICON_H[i] > Trials.ICON_FRAME) {
                iconOk = false; iconBad.append(" 帧").append(i).append(" 高越界=").append(Trials.ICON_H[i]);
            }
            if (x + Trials.ICON_W[i] > Trials.ICON_COLS * Trials.ICON_FRAME) {
                iconOk = false; iconBad.append(" 帧").append(i).append(" 横向溢出帧外");
            }
        }
        chk(iconOk, "图标表越界：" + iconBad);
        for (int[] d : DRAWN) {
            int i = d[0];
            chk(Trials.ICON_W[i] == d[1] && Trials.ICON_H[i] == d[2],
                    "已绘制帧 " + i + " 的尺寸应实测为 " + d[1] + "x" + d[2]
                            + "，实得 " + Trials.ICON_W[i] + "x" + Trials.ICON_H[i]);
        }

        //=== ③④ 文本键（键名由真实类名反推）===
        String clsKey = Trials.class.getName()
                .replace("com.shatteredpixel.shatteredpixeldungeon.", "").toLowerCase(Locale.ENGLISH);
        chk("trials".equals(clsKey), "Trials 的文本键前缀应为 trials，实得 " + clsKey);
        String winKey = WndTrials.class.getName()
                .replace("com.shatteredpixel.shatteredpixeldungeon.", "").toLowerCase(Locale.ENGLISH);
        chk("windows.wndtrials".equals(winKey), "WndTrials 的文本键前缀应为 windows.wndtrials，实得 " + winKey);

        for (int i = 0; i < n; i++) {
            String id = Trials.NAME_IDS[i];
            String[] keys = { clsKey + "." + id, clsKey + "." + id + "_desc" };
            for (String k : keys) {
                Object[][] sides = { { "zh", mz }, { "en", me } };
                for (Object[] side : sides) {
                    String tag = side[0] + " " + k;
                    String v = ((Properties) side[1]).getProperty(k);
                    if (v == null) { chk(false, "[MISS] " + tag); continue; }
                    chk(!v.trim().isEmpty(), "[EMPTY] " + tag);
                    chk(v.indexOf('\uFFFD') < 0, "[REPL] " + tag + " 含 U+FFFD");
                    chk(!v.contains("/n/"), "[NL] " + tag + " 含误写的 /n/");
                    int us = 0;
                    for (int c = 0; c < v.length(); c++) if (v.charAt(c) == '_') us++;
                    chk(us % 2 == 0, "[MARK] " + tag + " 的 _ 标记不成对（" + us + " 个）");
                }
            }
        }

        //=== ⑤ 名称硬断言（用户点名，稳定概念名）===
        for (int i = 0; i < n; i++) {
            String k = clsKey + "." + Trials.NAME_IDS[i];
            chk(EXPECT_NAMES[i].equals(mz.getProperty(k)),
                    "zh 名称应为 " + EXPECT_NAMES[i] + "，实得 " + mz.getProperty(k));
            chk(EXPECT_NAMES[i].equals(me.getProperty(k)),
                    "en 名称应为 " + EXPECT_NAMES[i] + "，实得 " + me.getProperty(k));
        }

        //=== ⑥ 窗口键 + 回归邻键 ===
        for (String k : WIN_KEYS) {
            String vz = wz.getProperty(k), ve = we.getProperty(k);
            chk(vz != null && !vz.trim().isEmpty(), "[MISS/EMPTY] zh " + k);
            chk(ve != null && !ve.trim().isEmpty(), "[MISS/EMPTY] en " + k);
        }
        chk(wz.getProperty("windows.wndchallenges.title") != null, "回归：windows.wndchallenges.title 丢了");
        chk(mz.getProperty("challenges.champion_enemies") != null, "回归：challenges.champion_enemies 丢了");
        chk(mz.getProperty("challenges.stronger_bosses_desc") != null, "回归：challenges.stronger_bosses_desc 丢了");

        //=== ⑦ HOKMA（智慧）的延迟上下限：常量 + 文本 ===
        //常量这边与 _chk/verify_hokma_delay.py 的「源码结构 / javap 签名 / 字节码 min-max」断言
        //互为交叉验证：那边证明**收口与方向**，这边证明**数值与门控位**，都不是自证。
        chk(Trials.HOKMA == 2, "HOKMA 的位应为 2(1<<1)，实得 " + Trials.HOKMA);
        chk("hokma".equals(Trials.NAME_IDS[1]), "NAME_IDS[1] 应为 hokma，实得 " + Trials.NAME_IDS[1]);
        chk(Trials.HERO_MOVE_DELAY_MIN == 1f,
                "英雄移动延迟下限应为 1f，实得 " + Trials.HERO_MOVE_DELAY_MIN);
        chk(Trials.ENEMY_MOVE_DELAY_MAX == 1f,
                "敌方移动延迟上限应为 1f，实得 " + Trials.ENEMY_MOVE_DELAY_MAX);
        chk(Trials.HERO_ATTACK_DELAY_MIN == 0.5f,
                "英雄攻击延迟下限应为 0.5f，实得 " + Trials.HERO_ATTACK_DELAY_MIN);

        //文本：hokma_desc 必须**真的解析出换行** —— 属性文件里写的是字面 \n，
        //      Properties.load 会把它变成真换行。这是「entry 没被截成两段」的权威证据：
        //      Python 侧只能看到两个字符，判不了这一点。
        String hokmaDescKey = clsKey + "." + Trials.NAME_IDS[1] + "_desc";
        for (Object[] side : new Object[][] { { "zh", mz }, { "en", me } }) {
            String tag = side[0] + " " + hokmaDescKey;
            String v = ((Properties) side[1]).getProperty(hokmaDescKey);
            chk(v != null && !v.trim().isEmpty(), "[MISS/EMPTY] " + tag);
            if (v == null) continue;
            chk(v.indexOf('\n') >= 0, "[NOLF] " + tag + " 未解析出换行（\\n 可能被写成真换行或漏写）");
            chk(v.contains("0.5"), "[SPEC] " + tag + " 没提到 0.5（攻击延迟下限）");
            chk(v.contains("1"), "[SPEC] " + tag + " 没提到 1（移动延迟上下限）");
            chk(v.indexOf('\uFFFD') < 0, "[REPL] " + tag + " 含 U+FFFD");
            chk(!v.contains("/n/"), "[NL] " + tag + " 含误写的 /n/");
            int us = 0;
            for (int c = 0; c < v.length(); c++) if (v.charAt(c) == '_') us++;
            chk(us % 2 == 0, "[MARK] " + tag + " 的 _ 标记不成对（" + us + " 个）");
        }
        chk(!mz.getProperty(hokmaDescKey, "").contains("占位"), "[STALE] zh hokma_desc 仍是占位文案");
        chk(!me.getProperty(hokmaDescKey, "").contains("Placeholder"), "[STALE] en hokma_desc 仍是占位文案");

        //天赋加注键：键名由**真实类名 + 枚举常量名**反推，专抓拼写/大小写写错（界面会显示成键名且不报错）
        String talentKey = Talent.class.getName()
                .replace("com.shatteredpixel.shatteredpixeldungeon.", "").toLowerCase(Locale.ENGLISH);
        chk("actors.hero.talent".equals(talentKey),
                "Talent 的文本键前缀应为 actors.hero.talent，实得 " + talentKey);
        Talent[] affected = { Talent.LETHAL_MOMENTUM, Talent.LETHAL_HASTE };
        for (Talent t : affected) {
            String base = talentKey + "." + t.name().toLowerCase(Locale.ENGLISH);
            for (Object[] side : new Object[][] { { "zh", az }, { "en", ae } }) {
                String tag = side[0] + " " + base + ".hokma";
                String v = ((Properties) side[1]).getProperty(base + ".hokma");
                chk(v != null && !v.trim().isEmpty(), "[MISS/EMPTY] " + tag);
                if (v != null) chk(v.indexOf('\uFFFD') < 0, "[REPL] " + tag);
            }
            //加注是**追加**，该天赋自己的 .desc 必须仍在
            chk(az.getProperty(base + ".desc") != null, "回归：" + t.name() + " 的 desc 丢了（zh）");
            chk(ae.getProperty(base + ".desc") != null, "回归：" + t.name() + " 的 desc 丢了（en）");
        }

        //=== ⑧ BINAH（理解）的生成时等级规则：位号 + 运行期 final 收口 + 文本 ===
        //「生成时」规则与 HOKMA 那种「一切结算之后再夹」的全局规则不同：它只在
        //Item.random() 那一瞬间生效。收口是否**真的唯一**，光看源码不够（源码解析会被注释骗到），
        //这里反射读 ACC_FINAL —— 只有真编译产物答得出来，正好与 verify_binah_gen.py 的
        //「源码结构 / javap 签名 / 字节码 ineg 先于 iconst_0」三层互为交叉验证。
        chk(Trials.BINAH == 4, "BINAH 的位应为 4(1<<2)，实得 " + Trials.BINAH);
        chk("binah".equals(Trials.NAME_IDS[2]), "NAME_IDS[2] 应为 binah，实得 " + Trials.NAME_IDS[2]);

        boolean randomIsFinal = false;
        boolean hasRandomRaw = false;
        try {
            randomIsFinal = java.lang.reflect.Modifier.isFinal(
                    Item.class.getDeclaredMethod("random").getModifiers());
            hasRandomRaw = Item.class.getDeclaredMethod("randomRaw") != null;
        } catch (NoSuchMethodException e) {
            //下面两条断言会各自报 FAIL，这里不再重复报
        }
        chk(randomIsFinal, "Item.random() 运行期必须带 ACC_FINAL（否则存在第二个生成出口）");
        chk(hasRandomRaw, "Item.randomRaw() 必须存在（子类生成逻辑的落点）");

        //文本：binah_desc 同样必须**解析出真换行**（证明写的是字面 \n、entry 没被截成两段）
        String binahDescKey = clsKey + "." + Trials.NAME_IDS[2] + "_desc";
        for (Object[] side : new Object[][] { { "zh", mz }, { "en", me } }) {
            String tag = side[0] + " " + binahDescKey;
            String v = ((Properties) side[1]).getProperty(binahDescKey);
            chk(v != null && !v.trim().isEmpty(), "[MISS/EMPTY] " + tag);
            if (v == null) continue;
            chk(v.indexOf('\n') >= 0, "[NOLF] " + tag + " 未解析出换行（\\n 可能被写成真换行或漏写）");
            chk(v.contains("0"), "[SPEC] " + tag + " 没提到 0（0 级带诅咒仍是 0）");
            chk(v.contains("负") || v.contains("negative"), "[SPEC] " + tag + " 没提到负等级");
            chk(v.indexOf('\uFFFD') < 0, "[REPL] " + tag + " 含 U+FFFD");
            chk(!v.contains("/n/"), "[NL] " + tag + " 含误写的 /n/");
            int us = 0;
            for (int c = 0; c < v.length(); c++) if (v.charAt(c) == '_') us++;
            chk(us % 2 == 0, "[MARK] " + tag + " 的 _ 标记不成对（" + us + " 个）");
        }
        chk(!mz.getProperty(binahDescKey, "").contains("占位"), "[STALE] zh binah_desc 仍是占位文案");
        chk(!me.getProperty(binahDescKey, "").contains("Placeholder"), "[STALE] en binah_desc 仍是占位文案");

        //=== ⑨ CHESED（慈悲） + GEBURA（严厉）：位号 + 效果常量 + 运行期钩子 + 文本 ===
        //这两条考验作用于**敌方单位**，与「改道具」的 BINAH 又不同类，故单独钉：
        //  · CHESED：生命上限倍率与回血口径必须是**常量**（后续要能一处调整、不写死数值）；
        //  · GEBURA：致死闸门靠 Mob 的两个运行期成员撑住 —— 这两个成员存不存在、可见性对不对，
        //    只有真编译产物答得出来（源码断言可能被注释骗到）。
        chk(Trials.CHESED == 8, "CHESED 的位应为 8(1<<3)，实得 " + Trials.CHESED);
        chk(Trials.GEBURA == 16, "GEBURA 的位应为 16(1<<4)，实得 " + Trials.GEBURA);
        chk("chesed".equals(Trials.NAME_IDS[3]), "NAME_IDS[3] 应为 chesed，实得 " + Trials.NAME_IDS[3]);
        chk("gebura".equals(Trials.NAME_IDS[4]), "NAME_IDS[4] 应为 gebura，实得 " + Trials.NAME_IDS[4]);

        chk(Trials.CHESED_HP_MULT == 1.25f,
                "CHESED 生命上限倍率应为 1.25f，实得 " + Trials.CHESED_HP_MULT);
        chk(Trials.CHESED_MEND_TURNS == 5,
                "CHESED 回血间隔应为 5 回合，实得 " + Trials.CHESED_MEND_TURNS);
        chk(Trials.CHESED_MEND_PERCENT == 0.10f,
                "CHESED 每次回血应为生命上限的 10%(0.10f)，实得 " + Trials.CHESED_MEND_PERCENT);

        boolean mobGuard = false;
        try {
            java.lang.reflect.Field f = Mob.class.getDeclaredField("geburaGrace");
            mobGuard = f.getType() == boolean.class
                    && java.lang.reflect.Modifier.isPublic(f.getModifiers());
        } catch (NoSuchFieldException e) {
            //下面这条断言会报 FAIL，这里不重复报
        }
        chk(mobGuard, "Mob.geburaGrace 必须是 public boolean（Trials 与两个 buff 都要跨包读写）");

        boolean mobDefer = false;
        try {
            mobDefer = Mob.class.getDeclaredMethod("deathIsDeferred", Object.class) != null;
        } catch (NoSuchMethodException e) {
            //同上
        }
        chk(mobDefer, "Mob.deathIsDeferred(Object) 必须存在（战续单位让路的钩子）");

        //文本：两条 desc 都必须**解析出真换行**（证明写的是字面 \n、entry 没被截成两段）且不再是占位
        String[] newDescKeys = {
                clsKey + "." + Trials.NAME_IDS[3] + "_desc",
                clsKey + "." + Trials.NAME_IDS[4] + "_desc" };
        String[][] newDescSpecs = {
                { "25", "5", "10" },        // CHESED：+25% / 每 5 回合 / 恢复 10%
                { "0" } };                  // GEBURA：无敌期间血停在 0
        for (int d = 0; d < newDescKeys.length; d++) {
            for (Object[] side : new Object[][] { { "zh", mz }, { "en", me } }) {
                String tag = side[0] + " " + newDescKeys[d];
                String v = ((Properties) side[1]).getProperty(newDescKeys[d]);
                chk(v != null && !v.trim().isEmpty(), "[MISS/EMPTY] " + tag);
                if (v == null) continue;
                chk(v.indexOf('\n') >= 0, "[NOLF] " + tag + " 未解析出换行（\\n 可能被写成真换行或漏写）");
                for (String need : newDescSpecs[d]) {
                    chk(v.contains(need), "[SPEC] " + tag + " 没提到 " + need);
                }
                chk(v.indexOf('\uFFFD') < 0, "[REPL] " + tag + " 含 U+FFFD");
                chk(!v.contains("/n/"), "[NL] " + tag + " 含误写的 /n/");
                int us = 0;
                for (int c = 0; c < v.length(); c++) if (v.charAt(c) == '_') us++;
                chk(us % 2 == 0, "[MARK] " + tag + " 的 _ 标记不成对（" + us + " 个）");
            }
            chk(!mz.getProperty(newDescKeys[d], "").contains("占位"),
                    "[STALE] zh " + newDescKeys[d] + " 仍是占位文案");
            chk(!me.getProperty(newDescKeys[d], "").contains("Placeholder"),
                    "[STALE] en " + newDescKeys[d] + " 仍是占位文案");
        }

        System.out.println("---- 断言 " + (pass + fail) + " 条：PASS " + pass + " / FAIL " + fail);
        System.out.println(fail == 0 ? "ALL PASS" : "HAS FAILURES");
    }
}
