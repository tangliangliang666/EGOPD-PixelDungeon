# -*- coding: utf-8 -*-
"""把 TIPHERETH（第 5 帧）的声明尺寸由 16x15 订正为 15x15。

背景：`tree.png` 里那一帧右侧曾有一道**极薄的不透明像素**，实测包围盒因此被算成 16x15；
用户 2026-09-25 已把那道像素擦掉，三行实测均为 15x15 ⇒ `Trials.ICON_W` 的声明值必须同步
（这两个数**就是**取样矩形的宽高，写大了会把透明边也取进来 ⇒ 图标被推偏）。

幂等：已是新值就直接跳过。字节级读写，保住纯 LF。
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TRIALS = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/Trials.java')

OLD_W = 'public static final int[] ICON_W\t= {13, 15, 15, 15, 13, 16, 15, 15, 15, 15};'
NEW_W = 'public static final int[] ICON_W\t= {13, 15, 15, 15, 13, 15, 15, 15, 15, 15};'

OLD_NOTE = ('//（2026-09-24 三行全部绘齐，逐帧尺寸已与第一行实测包围盒相等；'
            '双端核对见 `_chk/verify_tree_trials_icons.py`。）')
NEW_NOTE = (OLD_NOTE + '\n'
            '//（2026-09-25 用户擦掉了 TIPHERETH 帧右侧那道极薄的不透明像素 ⇒ 第 5 项由 16x15\n'
            '//   订正为 15x15，整树外包宽随之由 104.27 收到 103.27。**改完源图必须重跑**\n'
            '//   `_chk/verify_tree_trials_icons.py` 与 `_chk/analyze_tree_png.py` 实测回填。）')


def main():
    raw = open(TRIALS, 'rb').read()
    crlf, lf = raw.count(b'\r\n'), raw.count(b'\n')
    txt = raw.decode('utf-8')

    changes = 0
    for old, new, label in ((OLD_W, NEW_W, 'ICON_W'),
                            (OLD_NOTE, NEW_NOTE, '注释')):
        n_old, n_new = txt.count(old), txt.count(new)
        if n_new == 1 and n_old == 0:
            print('  跳过 %s：已是新值' % label)
            continue
        if n_old != 1:
            print('FAIL %s：旧串命中 %d 次（应为 1）⇒ 源文件已变，先回查' % (label, n_old))
            return 1
        txt = txt.replace(old, new)
        print('  改 %s：旧串 1 处 → 已替换' % label)
        changes += 1

    if not changes:
        print('无需改动')
        return 0

    out = txt.encode('utf-8')
    open(TRIALS, 'wb').write(out)
    after = open(TRIALS, 'rb').read()
    print('落盘：字节 %d → %d；CRLF=%d 裸LF=%d（写前 %d/%d）'
          % (len(raw), len(after), after.count(b'\r\n'),
             after.count(b'\n') - after.count(b'\r\n'), crlf, lf - crlf))
    assert after.count(b'\r\n') == crlf, '行尾被改动了'
    return 0


if __name__ == '__main__':
    sys.exit(main())
