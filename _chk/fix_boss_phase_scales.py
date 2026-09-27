# -*- coding: utf-8 -*-
"""
一次性补丁（幂等，字节级 IO）：把「用绝对生命值表达 HT 刻度」的阶段机制改为由 HT 反推。

背景（2026-09-24）：修完矮人国王二阶段（CHESED 给敌方 HT +25% ⇒ 屏障刻度恒等式崩塌 ⇒ 卡死）之后，
用户要求通查其余「有阶段转换、且阶段依赖写死绝对生命值」的 Boss，一并收口。

本次收口的三处（全部保证「在原版 HT 处与原写死值逐个相同 ⇒ 不开考验时行为分毫不变」）：

  YogDzewa（亚戈·德泽瓦，HT=1000）
    · 阶段推进量  HT - 300*phase        → HT - phaseStep()*phase   （300 = HT×3/10）
    · 末段生命下限 Math.max(HP, 100)     → Math.max(HP, finalPhaseFloor())（100 = HT/10）
    · 光束伤害刻度 (HT - HP)/400         → (HT - HP)/beamStep()      （400 = HT×2/5）

  DwarfKing（矮人国王，HT=300 / 强化 450）
    · 一阶段转二阶段 HP <= 100/50        → HP <= phase2EntryHP()      （450×2/9=100、300/6=50）

  SmilingCorpseMountain（微笑的尸山，HT=3000，纯外观换皮）
    · HP >= 2000 / >= 1000              → HP >= HT*2/3 / HT/3

刻意**不改**的两处绝对量（都是「非闸门」，改动反而会破坏原版行为）：
    · DwarfKing 三阶段那句「奄奄一息」台词扳机 HP<20（原版两难度共用同一常数，无法用单一比例还原）；
    · GnollGeomancer 的 RockArmor.setShield(25)（固定 25 点护甲，与 HT 无关，本就不是 HT 刻度）。

用法：python _chk/fix_boss_phase_scales.py     （跑第二遍应全部 [SKIP]）
"""

import os
import shutil
import sys

BASE = 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/'
BAK = '_chk/_bak_boss_phase_scales'

YOG = BASE + 'YogDzewa.java'
KING = BASE + 'DwarfKing.java'
SCM = BASE + 'SmilingCorpseMountain.java'


def nl_of(text):
    return '\r\n' if '\r\n' in text else '\n'


def indent(lines):
    """给每一行整体前移一层缩进（类成员是 1 个 tab）；空行保持为空行。

    块内容按「相对缩进」书写（内部方法体只写 1 个 tab），由这里统一补最外层，
    避免手写时漏掉某个 tab 导致插入的代码顶格（语法没错但风格不一致）。
    """
    return [('\t' + l) if l else '' for l in lines]


# ---------------------------------------------------------------- YogDzewa

YOG_HELPERS = [
    '//--- 阶段刻度：由 HT 反推，不再写死绝对生命值（2026-09-24）-------------------------',
    '//原版把「每推进一个阶段所需的伤害量」写死成 300（恰好是 HT=1000 的 3/10），',
    '//把最后一段（phase 4）的生命下限写死成 100（HT 的 1/10），光束数里的伤害刻度写死成 400（HT 的 2/5）',
    '//——三者都是**绝对生命值刻度**。考验 CHESED 给敌方生命上限 +25%（Trials.CHESED_HP_MULT）后',
    '//HT 变成 1250，而 300 / 100 / 400 不动 ⇒ 四个阶段的掉血窗口从「300 / 300 / 300 / 100」',
    '//漂成「300 / 300 / 300 / 350」，末段被拉长到 3.5 倍，与设计意图脱钩。',
    '//（⚠️ 与矮人国王那处**不同**：这里四个阈值仍严格递减，原本不会卡死；本处属「同一类刻度」一并归并。）',
    '//⇒ 一律从 HT 现算。HT = 1000 时三式与原写死值**逐个相同**（300 / 100 / 400）⇒ 不开本考验分毫不变。',
    '//⚠️ 不碰 Random、不新增字段 ⇒ 同 seed 的随机流与改动前逐字节一致。',
    'private static final int PHASE_STEP_NUM\t= 3;\t\t//阶段步长分子',
    'private static final int PHASE_STEP_DEN\t= 10;\t//阶段步长分母（同时是末段下限的分母）',
    'private static final int BEAM_STEP_NUM\t= 2;\t\t//光束伤害刻度分子',
    'private static final int BEAM_STEP_DEN\t= 5;\t\t//光束伤害刻度分母',
    '',
    '/** 每推进一个阶段所需的伤害量（＝原版写死的 300；HT = 1000 时 = 1000×3/10）。 */',
    'private int phaseStep(){',
    '\treturn HT * PHASE_STEP_NUM / PHASE_STEP_DEN;',
    '}',
    '',
    '/** 最后一段（phase 4）的生命下限（＝原版写死的 100；HT = 1000 时 = 1000/10）。 */',
    'private int finalPhaseFloor(){',
    '\treturn HT / PHASE_STEP_DEN;',
    '}',
    '',
    '/** 每多一道光束所需的已损失生命量（＝原版写死的 400；HT = 1000 时 = 1000×2/5）。 */',
    'private int beamStep(){',
    '\treturn HT * BEAM_STEP_NUM / BEAM_STEP_DEN;',
    '}',
    '',
]

# ---------------------------------------------------------------- DwarfKing

KING_HELPER = [
    '',
    '/**',
    ' * 一阶段 → 二阶段的入场血量（＝原版写死的 50；强化挑战 HT=450 时为 100）。',
    ' * <p>原版是**绝对量**（普通 50、强化 100）。改为按 HT 的同一比例现算 —— 普通 {@code HT/6}、',
    ' * 强化 {@code HT×2/9} ⇒ HT 变大时入场点同比例后移，而不是固定卡在 50 / 100。',
    ' * <p>HT = 300 / 450 时与原写死值**逐个相同**（50 / 100）⇒ 原版行为分毫不变。',
    ' * <p>说明：本值**不是**死局来源（血一定能掉到 50），2026-09-24 那次 CHESED 卡死与它无关；',
    ' * 这里只是把它从「绝对刻度」归并成「HT 刻度」，与二阶段屏障的两条刻度保持同一口径。',
    ' */',
    'private int phase2EntryHP(){',
    '\treturn Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? (HT * 2 / 9) : (HT / 6);',
    '}',
]


def sub(text, old, new, path, label, expect=1):
    """纯字符串替换 + 命中数断言（不按行号，天然免疫「插行导致行号漂移」）。"""
    got = text.count(old)
    if got != expect:
        raise AssertionError('%s / %s：期望命中 %d 次，实得 %d' % (path, label, expect, got))
    return text.replace(old, new)


def patch_yog(text, path):
    N = nl_of(text)
    join = lambda ls: N.join(ls)

    old_anchor = '\tprivate int phase = 0;' + N + N + '\tprivate float abilityCooldown;'
    new_anchor = ('\tprivate int phase = 0;' + N + N + join(indent(YOG_HELPERS)) + N
                  + '\tprivate float abilityCooldown;')
    text = sub(text, old_anchor, new_anchor, path, '插入阶段刻度助手')

    text = sub(text,
               'HP = Math.max(HP, HT - 300 * phase);',
               'HP = Math.max(HP, HT - phaseStep() * phase);',
               path, '阶段推进下限')
    text = sub(text,
               'HP = Math.max(HP, 100);',
               'HP = Math.max(HP, finalPhaseFloor());',
               path, '末段生命下限')
    text = sub(text,
               'if (phase < 4 && HP <= HT - 300*phase){',
               'if (phase < 4 && HP <= HT - phaseStep()*phase){',
               path, '阶段推进判定')
    text = sub(text,
               'int beams = 1 + (HT - HP)/400;',
               'int beams = 1 + (HT - HP)/beamStep();',
               path, '光束伤害刻度')
    return text


def patch_king(text, path):
    N = nl_of(text)

    old_anchor = '\tprivate int barrierThreshold( int num ){' + N + '\t\treturn HT * num / 3;' + N + '\t}'
    new_anchor = old_anchor + N + N.join(indent(KING_HELPER))
    text = sub(text, old_anchor, new_anchor, path, '插入二阶段入场血量助手')

    old_gate = ('\t\t\tif (HP <= (Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 100 : 50)) {' + N +
                '\t\t\t\tHP = (Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 100 : 50);')
    new_gate = '\t\t\tif (HP <= phase2EntryHP()) {' + N + '\t\t\t\tHP = phase2EntryHP();'
    text = sub(text, old_gate, new_gate, path, '一阶段转二阶段入场血量')

    old_yell = ('\t\t} else if (phase == 3 && preHP > 20 && HP < 20 && isAlive()){' + N +
                '\t\t\tyell( Messages.get(this, "losing") );')
    yell_note = [
        '\t\t//⚠️ 这里的 20 刻意**保留为绝对量**（不随 HT 缩放）：它只是「血快见底时喊一句」的台词扳机，',
        '\t\t//  不参与任何阶段推进 / 无敌 / 出怪判定；而原版在两个难度下都用同一个常数，想缩放就得给',
        '\t\t//  普通与强化各配一个任意分母（20 在 HT=300 与 450 上并不同比例），反而会改掉原版行为。',
        '\t\t//  与上方「二阶段屏障」「一阶段转二阶段」那两处不同：那两处会**卡住流程**，必须 HT 化。',
    ]
    new_yell = N.join(yell_note) + N + old_yell
    text = sub(text, old_yell, new_yell, path, '三阶段台词扳机加注')
    return text


def patch_scm(text, path):
    N = nl_of(text)

    text = sub(text,
               '\t\tif (HP >= 2000) return 3;' + N + '\t\tif (HP >= 1000) return 2;',
               '\t\tif (HP >= HT * 2 / 3) return 3;' + N + '\t\tif (HP >= HT / 3) return 2;',
               path, '换皮形态阈值')

    # 类注释里那三段 HP 区间表也要改成 HT 口径
    old_tbl = [
        ' *   <li><b>血量上限 3000</b>，随 HP 区间切换外观（{@link SmilingCorpseMountainSprite} 换贴图+帧网格）：',
        ' *       <ul>',
        ' *         <li>HP 2000~3000 → 形态 3（m3.png，64×48 帧）；</li>',
        ' *         <li>HP 1000~1999 → 形态 2（m2.png，48×48 帧）；</li>',
        ' *         <li>HP 0~999    → 形态 1（m1.png，32×24 帧）。</li>',
        ' *       </ul></li>',
    ]
    new_tbl = [
        ' *   <li><b>血量上限 3000</b>，随 HP 区间切换外观（{@link SmilingCorpseMountainSprite} 换贴图+帧网格）：',
        ' *       <ul>',
        ' *         <li>HP ≥ HT×2/3 → 形态 3（m3.png，64×48 帧）；</li>',
        ' *         <li>HP ≥ HT×1/3 → 形态 2（m2.png，48×48 帧）；</li>',
        ' *         <li>其余        → 形态 1（m1.png，32×24 帧）。</li>',
        ' *       </ul>',
        ' *       阈值自 2026-09-24 起由 HT 现算：原版写死 2000 / 1000，恰为 3000 的 2/3 与 1/3，',
        ' *       而考验 CHESED 会把 HT 抬到 3750，写死值就不再是 2/3·1/3 了。</li>',
    ]
    text = sub(text, N.join(old_tbl), N.join(new_tbl), path, '类注释区间表')

    # 方法上的 javadoc 也要改口径
    text = sub(text,
               '\t * 区间：≥2000 → 3；≥1000 → 2；其余 → 1。',
               '\t * 区间由 HT 现算：≥ HT×2/3 → 3；≥ HT×1/3 → 2；其余 → 1。',
               path, 'phase() 的 javadoc')
    return text


def main():
    os.makedirs(BAK, exist_ok=True)
    jobs = [
        (YOG, patch_yog, 'phaseStep()'),
        (KING, patch_king, 'phase2EntryHP'),
        (SCM, patch_scm, 'HT * 2 / 3'),
    ]
    rc = 0
    for path, fn, marker in jobs:
        raw = open(path, 'rb').read()
        text = raw.decode('utf-8')

        if marker in text:
            print('[SKIP] %s ：已应用过（命中标记 %r）' % (path.split('/')[-1], marker))
            continue

        bak = os.path.join(BAK, os.path.basename(path) + '.bak')
        if not os.path.exists(bak):
            shutil.copyfile(path, bak)

        crlf_before = raw.count(b'\r\n')
        bare_before = raw.count(b'\n') - crlf_before

        try:
            new_text = fn(text, path.split('/')[-1])
        except AssertionError as e:
            print('[FAIL] %s' % e)
            rc = 1
            continue

        raw2 = new_text.encode('utf-8')
        crlf_after = raw2.count(b'\r\n')
        bare_after = raw2.count(b'\n') - crlf_after

        # 换行风格必须原地保持：CRLF 文件只允许「新增行」带来的等量 CRLF 增量、不得混入裸 LF；
        # 纯 LF 文件（本项目自建的 mod 文件）则必须继续一个 CR 都没有。
        sep = nl_of(text)
        added = new_text.count(sep) - text.count(sep)
        if sep == '\r\n':
            ok = (crlf_after - crlf_before == added) and (bare_after == bare_before)
            detail = 'CRLF %d -> %d（新增 %d 行），裸 LF %d -> %d' % (
                crlf_before, crlf_after, added, bare_before, bare_after)
        else:
            ok = (crlf_after == 0) and (bare_after - bare_before == added)
            detail = '纯 LF 文件：行数 +%d，CRLF 仍为 0' % added
        if not ok:
            print('[FAIL] %s ：换行风格被破坏 —— %s' % (path, detail))
            rc = 1
            continue

        open(path, 'wb').write(raw2)
        print('[OK]   %s ：已改写（%s）' % (path.split('/')[-1], detail))

    if rc:
        print('\n有文件未通过，请人工检查（备份在 %s）' % BAK)
    return rc


if __name__ == '__main__':
    sys.exit(main())
