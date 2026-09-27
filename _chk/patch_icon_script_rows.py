# -*- coding: utf-8 -*-
"""修 verify_tree_trials_icons.py 里一条**过期判据**：整图高按「单行 16」写死。

tree.png 2026-09-24 起就是 **3 行 x 10 帧 = 160x48**，脚本却仍断言 `h == ICON_FRAME(16)`，
于是永久报红一条 FAIL —— 陈旧判据会把真回归埋掉，所以必须修（而不是"习惯性忽略"）。
改成 `h == ICON_ROWS * ICON_FRAME`，并让 ICON_ROWS 也参与读取/打印。

幂等：已改过就跳过。字节级读写。
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SC = os.path.join(ROOT, '_chk/verify_tree_trials_icons.py')

PAIRS = (
    ('#  ① 整图尺寸 = ICON_COLS * ICON_FRAME 宽、ICON_FRAME 高（10 帧一行、每帧 16x16）；',
     '#  ① 整图尺寸 = ICON_COLS * ICON_FRAME 宽、ICON_ROWS * ICON_FRAME 高（10 帧一行、'
     '每帧 16x16、共 3 行）；\n'
     '#     ⚠️ 本脚本只逐帧核对**第 1 行**的包围盒；「三行逐格一致」由 '
     '_chk/verify_tree_rows_bbox.py 单独钉。'),

    ("    return arr('W'), arr('H'), const('ICON_FRAME'), const('ICON_COLS')",
     "    return (arr('W'), arr('H'), const('ICON_FRAME'), const('ICON_COLS'), const('ICON_ROWS'))"),

    ("    iw, ih, frame, cols = read_java()\n"
     "    n = len(iw)\n"
     "    print('Trials.java: ICON_FRAME=%d ICON_COLS=%d 帧数=%d' % (frame, cols, n))",
     "    iw, ih, frame, cols, nrows = read_java()\n"
     "    n = len(iw)\n"
     "    print('Trials.java: ICON_FRAME=%d ICON_COLS=%d ICON_ROWS=%d 质点帧数=%d'\n"
     "          % (frame, cols, nrows, n))"),

    ("    check(h == frame, '高应为一帧高 %d，实得 %d' % (frame, h))",
     "    check(h == nrows * frame,\n"
     "          '高应为 ICON_ROWS*ICON_FRAME = %d，实得 %d（tree.png 现为 %d 行）'\n"
     "          % (nrows * frame, h, h // frame))"),
)


def main():
    raw = open(SC, 'rb').read()
    crlf, lf = raw.count(b'\r\n'), raw.count(b'\n')
    txt = raw.decode('utf-8')

    for old, new in PAIRS:
        if txt.count(new) == 1 and txt.count(old) == 0:
            print('  跳过：已是新值')
            continue
        if txt.count(old) != 1:
            print('FAIL：旧串命中 %d 次 ⇒ 回查' % txt.count(old))
            return 1
        txt = txt.replace(old, new)
        print('  改 ✓')

    if 'h == frame,' in txt:
        print('FAIL：仍残留按单行写死的高断言')
        return 1

    open(SC, 'wb').write(txt.encode('utf-8'))
    after = open(SC, 'rb').read()
    print('落盘：%d 行 → %d 行；CRLF=%d 裸LF=%d'
          % (lf - crlf, after.count(b'\n') - after.count(b'\r\n'),
             after.count(b'\r\n'), after.count(b'\n') - after.count(b'\r\n')))
    assert after.count(b'\r\n') == crlf, '行尾被改动了'
    return 0


if __name__ == '__main__':
    sys.exit(main())
