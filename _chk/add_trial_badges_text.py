#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""新增 10 条「考验通关成就」的徽章文本（zh + en）。

- 插入位置：徽章文本段的**末尾** —— 锚点行 `badges$badge.victory_random.desc=...` 之后、
  紧随其后的那个空行之前。
- 字节级、幂等：检测 marker `badges$badge.trial_keter.title` 已存在则整体 SKIP。
- 行尾断言：插入后「裸 LF == 0」且「CRLF 增量 == 插入的行数」。
- ⚠️ **键名自带下划线**（`trial_keter` 里的 `_`），所以 `_` 成对校验**只数值部分**（`=` 右侧），
  否则整行数 `_` 永远奇数 —— 这是 `fix_gebura_stagger_text.py` 踩过的同一个坑。

用法：
  python _chk/add_trial_badges_text.py            # 应用
  python _chk/add_trial_badges_text.py --check    # 只检查（不写盘）
"""

import os
import shutil
import sys

BASE = r'D:/PD'
ZH = os.path.join(BASE, 'core/src/main/assets/messages/misc/misc_zh.properties')
EN = os.path.join(BASE, 'core/src/main/assets/messages/misc/misc.properties')
BAK = os.path.join(BASE, '_chk/_bak_trial_badges')

ANCHOR = 'badges$badge.victory_random.desc='
MARKER = 'badges$badge.trial_keter.title'

# 顺序 ＝ badges.png 最后两行的排列顺序 ＝ 卡巴拉之树自上而下（KETER → MALKUTH）
# (键前缀, 中文标题, 中文考验名, 英文标题, 英文考验名)
ROWS = [
    ('trial_keter',     '纯真的自我',           'KETER',      'Innocent Self',                    'KETER'),
    ('trial_hokma',     '拥抱过去，创造未来',   'HOKMA',      'Embrace the Past, Create the Future', 'HOKMA'),
    ('trial_binah',     '直面恐惧，斩断循环',   'BINAH',      'Face the Fear, Break the Cycle',   'BINAH'),
    ('trial_chesed',    '值得托付的信任',       'CHESED',     'Trust Worth Entrusting',           'CHESED'),
    ('trial_gebura',    '守护他人的决意',       'GEBURA',     'Resolve to Protect Others',        'GEBURA'),
    ('trial_tiphereth', '存在意义的憧憬',       'TIPHERETH',  'Yearning for Meaning',             'TIPHERETH'),
    ('trial_netzach',   '生存下去的勇气',       'NETZACH',    'Courage to Survive',               'NETZACH'),
    ('trial_hod',       '愈加善良的希望',       'HOD',        'Hope Ever Kinder',                 'HOD'),
    ('trial_yesod',     '卓尔不凡的理性',       'YESOD',      'Extraordinary Reason',             'YESOD'),
    ('trial_malkuth',   '昂首阔步的信念',       'MALKUTH',    'Confident Conviction',             'MALKUTH'),
]

# 中文考验名（描述里跟在罗马名后的括号标注）
ZH_CN_NAME = {
    'KETER': '王冠', 'HOKMA': '智慧', 'BINAH': '理解', 'CHESED': '慈悲', 'GEBURA': '严厉',
    'TIPHERETH': '美丽', 'NETZACH': '胜利', 'HOD': '荣耀', 'YESOD': '根基', 'MALKUTH': '王国',
}

FAILS = []


def fail(msg):
    FAILS.append(msg)
    print('  [FAIL] ' + msg)


def ok(msg):
    print('  [OK] ' + msg)


def check_value_underscores(line, where):
    """值部分（`=` 右侧）的 `_` 必须成对。"""
    val = line.split('=', 1)[1] if '=' in line else line
    if val.count('_') % 2 != 0:
        fail('%s 值部分 `_` 不成对（%d 个）：%r' % (where, val.count('_'), val))
        return False
    return True


def build_lines():
    zh, en = [], []
    for k, zt, tr, et, en_tr in ROWS:
        cn = ZH_CN_NAME[tr]
        zh.append('badges$badge.%s.title=%s' % (k, zt))
        zh.append('badges$badge.%s.desc=在开启考验 _%s（%s）_ 的情况下通关' % (k, tr, cn))
        en.append('badges$badge.%s.title=%s' % (k, et))
        en.append('badges$badge.%s.desc=Beat the game with the _%s_ trial enabled' % (k, en_tr))
    return zh, en


def patch(path, lines, tag, apply):
    print('\n== %s ==' % tag)
    if not os.path.exists(path):
        fail('文件不存在：%s' % path)
        return
    raw = open(path, 'rb').read()
    crlf0 = raw.count(b'\r\n')
    lone0 = raw.count(b'\n') - crlf0
    txt = raw.decode('utf-8')

    if MARKER in txt:
        ok('[SKIP] 已存在（幂等）：%s' % MARKER)
        # 幂等路径也要复核行尾，防止上一次写坏
        if lone0 != 0:
            fail('裸 LF %d 个（应为 0）' % lone0)
        else:
            ok('裸 LF = 0（CRLF = %d）' % crlf0)
        return

    # 锚点唯一性
    if txt.count(ANCHOR) != 1:
        fail('锚点 `%s` 出现 %d 次（应为 1）' % (ANCHOR, txt.count(ANCHOR)))
        return
    ok('锚点唯一：%s' % ANCHOR)

    # 每条都要自检值部分
    for l in lines:
        check_value_underscores(l, tag)

    L = txt.split('\r\n')
    i = next(idx for idx, l in enumerate(L) if l.startswith(ANCHOR))
    ok('锚点在第 %d 行' % (i + 1))

    newL = L[:i + 1] + lines + L[i + 1:]
    out = '\r\n'.join(newL).encode('utf-8')

    crlf1 = out.count(b'\r\n')
    lone1 = out.count(b'\n') - crlf1
    if lone1 != 0:
        fail('插入后出现裸 LF %d 个' % lone1)
        return
    if crlf1 - crlf0 != len(lines):
        fail('CRLF 增量 %d != 插入行数 %d' % (crlf1 - crlf0, len(lines)))
        return
    ok('CRLF 增量 %d == 插入行数 %d，裸 LF = 0' % (crlf1 - crlf0, len(lines)))

    if not apply:
        ok('[CHECK] 未写盘')
        return

    os.makedirs(BAK, exist_ok=True)
    bak = os.path.join(BAK, os.path.basename(path))
    if not os.path.exists(bak):
        shutil.copy2(path, bak)
        ok('备份 -> %s' % os.path.relpath(bak, BASE))

    with open(path, 'wb') as f:
        f.write(out)
    ok('写入 %d 行' % len(lines))

    # 回读
    back = open(path, 'rb').read()
    if back.count(b'\r\n') != crlf1 or (back.count(b'\n') - back.count(b'\r\n')) != 0:
        fail('回读行尾不一致')
    else:
        ok('回读一致（CRLF = %d，裸 LF = 0）' % crlf1)


def main():
    apply = '--check' not in sys.argv
    zh, en = build_lines()
    print('共 %d 条成就 × 2 行 = %d 行/语言' % (len(ROWS), len(zh)))
    patch(ZH, zh, 'misc_zh.properties', apply)
    patch(EN, en, 'misc.properties', apply)

    print('\n' + '=' * 60)
    if FAILS:
        print('失败 %d 项：' % len(FAILS))
        for f in FAILS:
            print('  - ' + f)
        sys.exit(1)
    print('全部通过')


if __name__ == '__main__':
    main()
