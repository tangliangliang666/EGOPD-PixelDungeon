# -*- coding: utf-8 -*-
"""把 WndTrials 的图标显示尺寸翻倍（0.70 → 1.40），并让窗口宽 140 → 148 以容纳。

背景：用户在机上验证后反馈「图标显示的过小，需要翻倍」。只动**显示尺寸**（Image.scale），
贴图一个像素都不重采样 —— 取样矩形仍是 tree.png 里那一帧的原生 13~16px。

顺带做的三件事（都是翻倍的必然后果，不是新功能）：
  · WIDTH 140 → 148：树宽 = a*sqrt3 + 最大图标显示宽 = 121.24 + 22 = 143.24，140 装不下了；
  · TOUCH 20 → 26：图标本体最大 22px，命中区要能把图标整个圈住；
  · 抽一个 scaleToDisplay()：树上的质点与详情窗的图标共用，避免两处 scale 各写一遍走样。

幂等：每条替换都断言「恰好命中 1 处」，跑第二遍会因命中 0 处而停下（不会重复插入）。
行尾：源文件是纯 LF（CRLF=0），脚本只做子串替换、不碰换行，结尾断言「仍是纯 LF 且无裸 CR」。
注意：下面多行的字符串一律写成相邻**字面量**（Python 只对相邻字面量做隐式拼接，
`T + 'x'` 换行再 `T + 'y'` 是语法错误 —— 本脚本第一版就栽在这里）。
"""
import os
import sys

PATH = os.path.join(
    'D:/PD',
    'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndTrials.java')

REPLACEMENTS = [
    # R1 类注释：整树外包数字（132.2 x 291 @0.70 -> 143.2 x 302 @1.40）
    ('R1 类注释·树外包',
     " * 空掉的 Da'at 位」的直连。整树外包 = 2h + 最大图标显示宽 × 4a + 最大图标显示高\n"
     ' * = **132.2 × 291**（a = 70、图标 0.70 缩放时），恰好装进 140 的内容宽（左右各留约 3.9px）。',
     " * 空掉的 Da'at 位」的直连。整树外包 = (2h + 最大图标显示宽) × (4a + 最大图标显示高)\n"
     ' * = **143.2 × 302**（a = 70、图标 1.40 缩放时），装进 148 的内容宽（左右各留约 2.4px）。'),

    # R2 类注释：溢出警告措辞对齐（图标显示宽 -> 最大图标显示宽）
    ('R2 类注释·溢出警告',
     ' * ⚠️ 别把 a 放大到「a·√3 + 图标显示宽 &gt; WIDTH」，那会让树溢出窗口。',
     ' * ⚠️ 别把 a 放大到「a·√3 + 最大图标显示宽 &gt; WIDTH」，那会让树溢出窗口。'),

    # R3 类注释·图标节：把「只缩小」扩成「可大可小」＋补 NEAREST 说明
    ('R3 类注释·图标节',
     ' * 需要显示得更小时**只改 {@code Image.scale}（＝只改显示尺寸）**，贴图本身一个像素都不重采样。\n'
     ' * 三行分别对应「已开启 / 未解锁 / 已解锁未开启」，判据在 {@link Trials#iconRow}。',
     ' * 需要显示得更大或更小时**只改 {@code Image.scale}（＝只改显示尺寸）**，贴图本身一个像素都不重采样。\n'
     ' * {@code ICON_SCALE = 1.40} 是原 0.70 的两倍（用户 2026-09-25：0.70 在机上偏小，故翻倍）；\n'
     ' * 取样走 NEAREST（{@code SmartTexture} 的默认），所以放大不会糊，代价是非整数倍会让少数行列宽一像素。\n'
     ' * 三行分别对应「已开启 / 未解锁 / 已解锁未开启」，判据在 {@link Trials#iconRow}。'),

    # R4 窗口内容宽 140 -> 148
    ('R4 WIDTH',
     '\tprivate static final int WIDTH\t\t= 140;\t//内容宽（与改造前一致）',
     '\tprivate static final int WIDTH\t\t= 148;\t//内容宽（140 → 148：图标翻倍后要给左右让出余量）'),

    # R5 ICON_SCALE 0.70 -> 1.40 ＋ 刷新「装得下」判据
    ('R5 ICON_SCALE',
     '\t//显示缩放：**只改显示尺寸、不重采样**（见类注释）。0.70 是「把树塞进 140 内容宽」的唯一约束\n'
     '\t//（树宽 = a·√3 + 最大图标显示宽 ≤ WIDTH ⇒ a ≤ 71.6）；a 取 70 是为了左右各留约 4px 呼吸位。\n'
     '\tprivate static final float ICON_SCALE\t= 0.70f;',
     '\t//显示缩放：**只改显示尺寸、不重采样**（见类注释）。1.40 = 原 0.70 的两倍（用户 2026-09-25）。\n'
     '\t//装得下的判据：树宽 = a·√3 + 最大图标显示宽 ≤ WIDTH ⇒ a ≤ (WIDTH - 最大显示宽)/√3；\n'
     '\t//148 宽下 a ≤ 72.7，a 取 70 时左右各留约 2.4px（图标翻倍后余量从 3.9px 收到 2.4px）。\n'
     '\tprivate static final float ICON_SCALE\t= 1.40f;'),

    # R6 命中区 20 -> 26
    ('R6 TOUCH',
     '\t//质点热点的边长：图标本体只有 9~11px，命中区放大到 20x20。质点间距 ≥ 60，不会互相压住。\n'
     '\tprivate static final float TOUCH\t\t= 20f;',
     '\t//质点热点的边长：图标本体最大 22px，命中区取 26 把图标整个圈住。质点最近距离 = A = 70。\n'
     '\tprivate static final float TOUCH\t\t= 26f;'),

    # R7 详情窗图标：改成与树上同尺寸（必须在交给 WndTrialInfo 之前设 scale）
    ('R7 详情窗图标尺寸',
     '\t\tupdateIcon( icon, index );\n'
     '\t\t//详情窗里的图标按**原生尺寸**显示：那里没有「塞进 140 内容宽」的约束，1:1 最清晰\n',
     '\t\tupdateIcon( icon, index );\n'
     '\t\t//详情窗里的图标与树上那枚**同一显示尺寸**（点谁看谁，大小连着）：必须在交给\n'
     '\t\t//WndTrialInfo 之前设好 scale —— {@code IconTitle.layout()} 读的是\n'
     '\t\t//{@code width()} = 原生宽 × scale，设晚了标题位置会压在图标上。\n'
     '\t\tscaleToDisplay( icon, index );\n'),

    # R8 树上质点：内联的 scale.set 换成共用方法
    ('R8 树上质点缩放',
     '\t\t\t//只改显示尺寸：取样矩形仍是原生 13~16px，缩放发生在 quad 上（贴图不重采样）\n'
     '\t\t\ticon.scale.set(\n'
     '\t\t\t\t\tICON_DW[idx] / (float)Trials.ICON_W[idx],\n'
     '\t\t\t\t\tICON_DH[idx] / (float)Trials.ICON_H[idx] );\n',
     '\t\t\t//只改显示尺寸：取样矩形仍是原生 13~16px，缩放发生在 quad 上（贴图不重采样）\n'
     '\t\t\tscaleToDisplay( icon, idx );\n'),

    # R9 抽出 scaleToDisplay()（放在 updateIcon 之后）
    ('R9 抽出 scaleToDisplay',
     '\t\t\t\tTrials.ICON_W[index], Trials.ICON_H[index] ) );\n\t}\n\n\t/** 点开某个质点的详情窗',
     '\t\t\t\tTrials.ICON_W[index], Trials.ICON_H[index] ) );\n'
     '\t}\n'
     '\n'
     '\t/**\n'
     '\t * 把图标切到「显示尺寸」（＝原生像素 × {@code ICON_SCALE}）。\n'
     '\t *\n'
     '\t * <p>**只动 {@code scale}，贴图一个像素都不重采样**：取样矩形仍旧是原生 13~16px，\n'
     '\t * 放大/缩小都发生在 quad 上（用户 2026-09-25 口径）。树上的质点与详情窗的图标共用它，\n'
     '\t * 保证「点谁看谁」两处尺寸一致。</p>\n'
     '\t */\n'
     '\tprivate static void scaleToDisplay( Image icon, int index ){\n'
     '\t\ticon.scale.set(\n'
     '\t\t\t\tICON_DW[index] / (float)Trials.ICON_W[index],\n'
     '\t\t\t\tICON_DH[index] / (float)Trials.ICON_H[index] );\n'
     '\t}\n'
     '\n'
     '\t/** 点开某个质点的详情窗'),
]


def main():
    with open(PATH, 'rb') as f:
        raw = f.read()
    assert b'\r' not in raw, '源文件里出现了 CR，本脚本假定纯 LF，先查清楚再来'

    src = raw.decode('utf-8')
    lines_before = src.count('\n')
    delta_expected = 0

    for tag, old, new in REPLACEMENTS:
        n = src.count(old)
        assert n == 1, '%s：期望命中 1 处，实得 %d 处（可能已打过这个补丁）' % (tag, n)
        src = src.replace(old, new, 1)
        d = new.count('\n') - old.count('\n')
        delta_expected += d
        print('  OK %s  (%+d 行)' % (tag, d))

    assert '\r' not in src, '替换后混进了 CR'
    lines_after = src.count('\n')
    assert lines_after - lines_before == delta_expected, \
        '行数增量 %d != 各条替换增量之和 %d' % (lines_after - lines_before, delta_expected)

    # 终态断言：改完必须是这个样子，否则宁可不写盘
    T = '\t'
    must = [('WIDTH' + T + T + '= 148;', 1),
            ('ICON_SCALE' + T + '= 1.40f;', 1),
            ('TOUCH' + T + T + '= 26f;', 1),
            ('icon.scale.set(', 1),             # 只剩 scaleToDisplay 里那一处
            ('scaleToDisplay', 3),              # 1 处声明 + 树上 1 处调用 + 详情窗 1 处调用
            ('Trials.ICON_W[idx]', 0),          # 内联缩放已收口
            ('= 140;', 0),
            ('0.70f', 0)]
    for needle, want in must:
        got = src.count(needle)
        assert got == want, '终态断言失败：%r 期望 %d 次、实得 %d 次' % (needle, want, got)

    with open(PATH, 'wb') as f:
        f.write(src.encode('utf-8'))
    print('\n已写回 %s' % PATH)
    print('行数 %d → %d（增量 %+d）；CRLF 计 0，纯 LF 保持' % (lines_before, lines_after, lines_after - lines_before))
    return 0


if __name__ == '__main__':
    sys.exit(main())
