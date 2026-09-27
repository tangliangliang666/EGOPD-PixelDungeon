import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 反例自测（2026-09-24）：证明 TrialsLocCheck 新增的两条判据**不是恒真断言**。
 *
 * 判据 ① {@code checkNoTruncatedEntries}：properties 里任何一处把字面 `\n` 落成**真换行**，
 *        加载时会截断 entry（续下那段变成没有 `=` 的裸行）⇒ 该判据必须报 FAIL。
 * 判据 ② {@code checkShapeParity}：同一个键的 zh/en 段落结构必须一致
 *        （同为单段或同为多段）⇒ 「zh 单段 / en 多段」必须报 FAIL，两边都单段则不报。
 *
 * 跑法（`_chk/_javachk` 必须排 classpath 最前，读到刚编的 TrialsLocCheck）：
 *   javac -cp "_chk/_javachk" -d _chk/_javachk _chk/TrialTextGuardProbe.java
 *   java  -cp "_chk/_javachk;core/build/classes/java/main;…" TrialTextGuardProbe
 */
public class TrialTextGuardProbe {

    static final String SRC = "D:/PD/core/src/main/assets/messages/misc/misc_zh.properties";
    static final String TMP_DIR = "D:/PD/_chk/_tmp_trunc";
    static final String TMP = TMP_DIR + "/misc_zh.properties";

    static int bad = 0;

    static void chk(boolean cond, String msg) {
        if (cond) {
            System.out.println("[OK]   " + msg);
        } else {
            bad++;
            System.out.println("[FAIL] " + msg);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== ① checkNoTruncatedEntries：正向（真实文件不该报） ===");
        int before = TrialsLocCheck.fail;
        TrialsLocCheck.checkNoTruncatedEntries("real misc_zh", SRC);
        chk(TrialsLocCheck.fail == before, "真实 misc_zh.properties 不应触发 TRUNC（fail " + before + " -> " + TrialsLocCheck.fail + "）");

        System.out.println("=== ② checkNoTruncatedEntries：反例（字面 \\n 被写成真换行） ===");
        new File(TMP_DIR).mkdirs();
        String t = new String(Files.readAllBytes(Paths.get(SRC)), "UTF-8");
        int i = t.indexOf("trials.hokma_desc=");
        chk(i >= 0, "反例前置：找得到 trials.hokma_desc");
        int j = (i < 0) ? -1 : t.indexOf("\\n", i);   // 字面「反斜杠 + n」
        chk(j >= 0, "反例前置：该条目里找得到字面 \\n");
        if (j >= 0) {
            // 把 2 个字符的字面「\n」换成一个**真换行** ⇒ 模拟编辑工具/heredoc 意外
            String injected = t.substring(0, j) + "\n" + t.substring(j + 2);
            Files.write(Paths.get(TMP), injected.getBytes("UTF-8"));
            before = TrialsLocCheck.fail;
            TrialsLocCheck.checkNoTruncatedEntries("injected", TMP);
            chk(TrialsLocCheck.fail == before + 1,
                    "反例：真换行截断必须被抓到（fail " + before + " -> " + TrialsLocCheck.fail + "，期望 +1）");
        }

        System.out.println("=== ③ checkShapeParity：反例（zh 单段 / en 多段） ===");
        Properties pz = new Properties();
        Properties pe = new Properties();
        pz.setProperty("k", "单段文案");
        pe.setProperty("k", "first paragraph\n\nsecond paragraph");
        before = TrialsLocCheck.fail;
        TrialsLocCheck.checkShapeParity("k", pz, pe);
        chk(TrialsLocCheck.fail == before + 1,
                "反例：zh 单段 / en 多段 必须判不一致（fail " + before + " -> " + TrialsLocCheck.fail + "）");

        System.out.println("=== ④ checkShapeParity：正向（两边都单段 / 都多段） ===");
        pe.setProperty("k", "single paragraph");
        before = TrialsLocCheck.fail;
        TrialsLocCheck.checkShapeParity("k", pz, pe);
        chk(TrialsLocCheck.fail == before, "正向：两边都单段不应报错");
        pe.setProperty("k", "a\n\nb");
        pz.setProperty("k", "甲\n\n乙");
        before = TrialsLocCheck.fail;
        TrialsLocCheck.checkShapeParity("k", pz, pe);
        chk(TrialsLocCheck.fail == before, "正向：两边都多段不应报错");

        //清理临时文件
        try {
            Files.deleteIfExists(Paths.get(TMP));
            Files.deleteIfExists(Paths.get(TMP_DIR));
        } catch (IOException ignored) {
            // 删不掉不影响判定
        }

        System.out.println("==================================================");
        System.out.println(bad == 0 ? "ALL PASS（两条新判据都非恒真，且不误伤正确形态）"
                : ("HAS FAILURES = " + bad));
        if (bad != 0) System.exit(1);
    }
}
