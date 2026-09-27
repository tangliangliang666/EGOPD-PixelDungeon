# -*- coding: utf-8 -*-
"""
把 WndTrials 的窗口尺寸对齐「原版挑战窗」WndChallenges。

背景（用户 2026-09-25 反馈）：内容高 326（外框 338）在机上把上下沿顶出屏幕。
量得 WndChallenges = 120x236 内容（13 条挑战：16 + 13*16 + 12*1），外框 132x248。

做法：**不动图标**（ICON_SCALE 仍是 1.40，即用户要的「翻倍」），
只把树的纵向步长 A 从 70 收到 47.5 —— 这是「4A + 最大图标高 ≤ 212」卡出来的上限，
于是内容高恰为 236，与挑战窗逐像素同规格。

幂等：已改过则跳过；每条替换断言「恰好命中一次」。
"""
import os
import sys

P = 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndTrials.java'

raw = open(P, 'rb').read()
EOL = '\r\n' if b'\r\n' in raw else '\n'
TXT = raw.decode('utf-8')

# (名字, 旧, 新) —— 全部单行或「用 EOL 拼好的多行」
REPL = [
    (
        'javadoc 数字',
        ' * = **143.2 × 302**（a = 70、图标 1.40 缩放时），装进 148 的内容宽（左右各留约 2.4px）。',
        ' * = **104.3 × 212**（a = 47.5、图标 1.40 缩放时），装进 120 的内容宽（左右各留约 7.9px）。',
    ),
    (
        'javadoc 新增窗口尺寸节',
        ' * <h3>为什么几何能全写死</h3>',
        EOL.join([
            ' * <h3>窗口尺寸（2026-09-25 与挑战窗对齐）</h3>',
            ' * 内容 **120 × 236**，与 {@code WndChallenges}（13 条挑战 ⇒ 16 + 13×16 + 12×1 = 236）',
            ' * **逐像素同规格**，外框同为 132 × 248。前一版按 a = 70 排 ⇒ 内容高 326（外框 338），',
            ' * 在机上把上下沿顶出屏幕（用户 2026-09-25 反馈「占据整个屏幕」）。',
            ' * 现在 a 收到 47.5 —— 这是「4a + 最大图标显示高 ≤ 212」卡出来的上限 —— 内容高恰为 236。',
            ' * 树仍是「同一张图」的等比缩小，**图标显示尺寸（1.40）一个像素没动**。',
            ' *',
            ' * <h3>为什么几何能全写死</h3>',
        ]),
    ),
    (
        'WIDTH',
        '\tprivate static final int WIDTH\t\t= 148;\t//内容宽（140 → 148：图标翻倍后要给左右让出余量）',
        '\tprivate static final int WIDTH\t\t= 120;\t//内容宽（与 WndChallenges 齐平；树只占 104.3，左右各余 7.9）',
    ),
    (
        'A',
        '\tprivate static final float A\t= 70f;',
        '\tprivate static final float A\t= 47.5f;',
    ),
    (
        'H 注释',
        '\tprivate static final float H\t= A * 1.7320508f / 2f;\t//≈ 60.62',
        '\tprivate static final float H\t= A * 1.7320508f / 2f;\t//≈ 41.14',
    ),
    (
        'ICON_SCALE 判据注释',
        EOL.join([
            '\t//装得下的判据：树宽 = a·√3 + 最大图标显示宽 ≤ WIDTH ⇒ a ≤ (WIDTH - 最大显示宽)/√3；',
            '\t//148 宽下 a ≤ 72.7，a 取 70 时左右各留约 2.4px（图标翻倍后余量从 3.9px 收到 2.4px）。',
        ]),
        EOL.join([
            '\t//两个「装得下」的判据（verify_trials_tree_ui.py 都钉住）：',
            '\t//  宽度：a·√3 + 最大图标显示宽 ≤ WIDTH ⇒ 120 宽下 a ≤ 56.6（不是瓶颈）',
            '\t//  高度：4a + 最大图标显示高 ≤ 212（= 挑战窗内容高 236 − 标题 16 − 上下留白 8）⇒ a ≤ 47.5',
            '\t//所以 a 取 47.5 是**高度**卡出来的上限，此时内容高恰 236，与 WndChallenges 同规格。',
        ]),
    ),
    (
        'TOUCH 注释',
        '\t//质点热点的边长：图标本体最大 22px，命中区取 26 把图标整个圈住。质点最近距离 = A = 70。',
        '\t//质点热点的边长：图标本体最大 22px，命中区取 26 把图标整个圈住。质点最近距离 = A = 47.5 > 26，不互压。',
    ),
]


def main():
    txt = TXT
    changed = 0
    skipped = 0
    for name, old, new in REPL:
        if new in txt and old not in txt:
            print('  [跳过] %s（已是新值）' % name)
            skipped += 1
            continue
        n = txt.count(old)
        if n != 1:
            print('  [FAIL] %s：命中 %d 次（要求恰好 1 次）' % (name, n))
            return 1
        txt = txt.replace(old, new, 1)
        changed += 1
        print('  [OK]   %s' % name)

    if changed == 0:
        print('无改动。')
        return 0

    # 落盘前把话说完：这次改动的「意图」必须能在源码里读出来
    must = ['int WIDTH\t\t= 120;', 'float A\t= 47.5f;', 'float ICON_SCALE\t= 1.40f;',
            '4a + 最大图标显示高 ≤ 212']
    for m in must:
        if m not in txt:
            print('  [FAIL] 落盘前自检：源码里找不到 %r' % m)
            return 1

    # 行尾断言：本次全是等行数替换 / 净增 6 行（javadoc 节 8 行换 1 行 = +7，
    # 判据注释 2 行换 4 行 = +2），故裸 LF 增量 = 替换行数差之和
    open(P, 'w', encoding='utf-8', newline='').write(txt)
    nb = open(P, 'rb').read()
    print('\n写入完成：%d 处改动，%d 处跳过。行尾 CRLF=%d 裸LF=%d'
          % (changed, skipped, nb.count(b'\r\n'), nb.count(b'\n') - nb.count(b'\r\n')))
    return 0


if __name__ == '__main__':
    sys.exit(main())
