# 考验图标（tree.png）与 Trials.java 的尺寸表互查（2026-09-23）
#
# 断言：
#  ① 整图尺寸 = ICON_COLS * ICON_FRAME 宽、ICON_ROWS * ICON_FRAME 高（10 帧一行、每帧 16x16、共 3 行）；
#     ⚠️ 本脚本只逐帧核对**第 1 行**的包围盒；「三行逐格一致」由 _chk/verify_tree_rows_bbox.py 单独钉。
#  ② 每帧画面**不得越出本帧的 16x16 格**（越界＝画超格、会串到邻帧）；
#  ③ `Trials.ICON_W/H[i]` 声明的矩形必须**完整包住**该帧实测包围盒（声明小了 ⇒ 图标被裁）；
#  ④ 已绘制的帧：声明尺寸必须**恰好等于**实测包围盒（这一步就是在防「重画了却没回填常量」）；
#  ⑤ 未绘制的帧：仅 NOTE（历史上曾按 16x16 占位；2026-09-24 十帧已全部绘齐，此段应为空）。
#
# 用法：python _chk/verify_tree_trials_icons.py [--selftest]
#       --selftest 用「把第 1 帧声明宽度改小」的反例证明 ③ 真的会红（不是恒真断言）。
import re
import sys

sys.path.insert(0, '_chk')
from verify_sprite_frames import decode_png  # noqa: E402

TREE = 'core/src/main/assets/interfaces/tree.png'
TRIALS_JAVA = 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/Trials.java'

fails = []
notes = []


def check(cond, msg):
    if cond:
        print('  ok   ' + msg)
    else:
        fails.append(msg)
        print('  FAIL ' + msg)


def read_java():
    src = open(TRIALS_JAVA, encoding='utf-8').read()

    def arr(name):
        m = re.search(r'ICON_' + name + r'\s*=\s*\{([^}]*)\}', src)
        assert m, 'Trials.java 里找不到 ICON_' + name
        return [int(v.strip()) for v in m.group(1).split(',') if v.strip()]

    def const(name):
        m = re.search(r'\b' + name + r'\s*=\s*(\d+)\s*;', src)
        assert m, 'Trials.java 里找不到 ' + name
        return int(m.group(1))

    return (arr('W'), arr('H'), const('ICON_FRAME'), const('ICON_COLS'), const('ICON_ROWS'))


def measure(path, fw, fh, cols):
    w, h, bd, ct, plte, trns, px = decode_png(path)
    nch = len(px) // (w * h)
    rows = h // fh

    def alpha(x, y):
        return px[(y * w + x) * nch + (nch - 1)]

    frames = []
    for r in range(rows):
        for c in range(cols):
            x0, y0 = c * fw, r * fh
            minx, miny, maxx, maxy, cnt = fw, fh, -1, -1, 0
            for yy in range(y0, y0 + fh):
                for xx in range(x0, x0 + fw):
                    if alpha(xx, yy) > 0:
                        cnt += 1
                        minx = min(minx, xx - x0)
                        miny = min(miny, yy - y0)
                        maxx = max(maxx, xx - x0)
                        maxy = max(maxy, yy - y0)
            if cnt == 0:
                frames.append(None)
            else:
                frames.append((minx, miny, maxx - minx + 1, maxy - miny + 1, cnt))
    return (w, h), frames


def main(selftest=False):
    iw, ih, frame, cols, nrows = read_java()
    n = len(iw)
    print('Trials.java: ICON_FRAME=%d ICON_COLS=%d ICON_ROWS=%d 质点帧数=%d'
          % (frame, cols, nrows, n))

    (w, h), frames = measure(TREE, frame, frame, cols)
    print('tree.png   : %dx%d  →  %d 列 x %d 行' % (w, h, cols, h // frame))

    print('[①] 整图尺寸')
    check(w == cols * frame, '宽应为 ICON_COLS*ICON_FRAME = %d，实得 %d' % (cols * frame, w))
    check(h == nrows * frame,
          '高应为 ICON_ROWS*ICON_FRAME = %d，实得 %d（tree.png 现为 %d 行）'
          % (nrows * frame, h, h // frame))
    check(w % frame == 0 and h % frame == 0, '尺寸必须能被帧宽/帧高整除')
    check(len(frames) >= n, '图里帧数 %d 少于 Trials 的 %d 条' % (len(frames), n))

    print('[②] 逐帧越界（不得画超格）')
    over = []
    for i, fr in enumerate(frames[:n]):
        if fr is None:
            continue
        x, y, fw_, fh_, _ = fr
        if x < 0 or y < 0 or x + fw_ > frame or y + fh_ > frame:
            over.append(i)
    check(not over, '有帧画超了 16x16 格：%s' % (over if over else '无'))

    print('[③④] 声明尺寸 vs 实测包围盒')
    drawn, undrawn = [], []
    for i in range(n):
        fr = frames[i] if i < len(frames) else None
        dw, dh = iw[i], ih[i]
        if fr is None:
            undrawn.append(i)
            check(1 <= dw <= frame and 1 <= dh <= frame,
                  '帧 %d（未绘制）占位尺寸 %dx%d 应在 1..%d 内' % (i, dw, dh, frame))
            continue
        drawn.append(i)
        x, y, fw_, fh_, cnt = fr
        check(dw >= x + fw_ and dh >= y + fh_,
              '帧 %d 声明的 %dx%d 必须完整包住实测 %dx%d（起点 %d,%d）'
              % (i, dw, dh, fw_, fh_, x, y))
        check(x == 0 and y == 0, '帧 %d 的画面起点应为 (0,0)，实得 (%d,%d)' % (i, x, y))

    print('[④] 已绘制帧必须与声明精确一致')
    exact_ok = True
    for i in drawn:
        x, y, fw_, fh_, cnt = frames[i]
        if iw[i] != fw_ or ih[i] != fh_:
            exact_ok = False
            print('       帧 %d：声明 %dx%d ≠ 实测 %dx%d' % (i, iw[i], ih[i], fw_, fh_))
    check(exact_ok, '已绘制帧（%s）的声明尺寸与实测包围盒逐一相等' % drawn)

    print('[⑤] 未绘制帧')
    for i in undrawn:
        notes.append('帧 %d 还是空的（当前按 %dx%d 占位）' % (i, iw[i], ih[i]))
    if undrawn:
        print('  note ' + '；'.join(notes))
    else:
        print('  note 十帧已全部绘齐，无占位帧（声明尺寸＝实测包围盒）')

    if selftest:
        print('---- 反例自测')
        bad = list(iw)
        bad[1] = max(1, iw[1] - 4)
        ok = all(bad[i] >= frames[i][2] for i in drawn)
        check(not ok, '把帧 1 的声明宽度改小后，判据 ③ 必须判 FAIL（证明它不是恒真断言）')

    print('---- FAIL %d 条' % len(fails))
    print('ALL PASS' if not fails else 'HAS FAILURES')
    return 0 if not fails else 1


if __name__ == '__main__':
    sys.exit(main('--selftest' in sys.argv))
