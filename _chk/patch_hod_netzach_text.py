# -*- coding: utf-8 -*-
"""把考验 NETZACH / HOD 的描述从「占位」改成正式文案（中英各一份）。

为什么单写一个脚本而不是直接用编辑器改：
  · `misc*.properties` 是 **CRLF**，且正文里的换行必须是**字面 `\\n`**（两个字符）而不是真换行 ——
    用普通文本编辑极易把 `\\n` 写成真换行，那样读出来会断行、且多半还顺手把行尾改成 LF。
  · 本工程约定：改 `.properties` 走**字节级 IO + 幂等 + 改前备份 + 断言**（见 AGENTS.md §6）。

断言：
  ① 每个键**恰好替换 1 处**（找不到或多于 1 处都算失败）；
  ② 替换后 **CRLF 计数不变**（本文件本来就是纯 CRLF，不许混进裸 LF）；
  ③ 新增正文里的换行是**字面 `\\n`**（断言新行里不含裸 CR/LF）；
  ④ 幂等：已是新文案时直接跳过。
用法：python _chk/patch_hod_netzach_text.py
"""

import os
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MSG = os.path.join(ROOT, 'core/src/main/assets/messages/misc')
BAK = os.path.join(ROOT, '_chk/_bak_hodnetzach')

# zh / en 两份新文案。r'' 保证 \n 是**两个字面字符**，写进 .properties 里就是转义换行。
ZH = {
    'trials.netzach_desc':
        r'所有_敌方单位_的闪避属性随所在_区域_提升：_一区 +10%_，每深入一区再 +10%，至_五区 +50%_。'
        r'\n\n_距离英雄超过 2 格_的敌方单位会变得半透明，_超过 4 格_时_完全不可见_。',
    'trials.hod_desc':
        r'所有_敌方单位_的命中属性随所在_区域_提升：_一区 +10%_，每深入一区再 +10%，至_五区 +50%_。'
        r'\n\n某个敌方单位_存活超过 150 回合_后，会获得一次_随机精英效果_（_不可叠加_）。',
}

EN = {
    'trials.netzach_desc':
        r'All _hostile units_ gain evasion based on the current _region_: _+10% in region 1_, '
        r'rising by _+10% per region_ up to _+50% in region 5_.'
        r'\n\nHostile units _more than 2 tiles_ from the hero become translucent,'
        r' and _more than 4 tiles_ away become _completely invisible_.',
    'trials.hod_desc':
        r'All _hostile units_ gain accuracy based on the current _region_: _+10% in region 1_, '
        r'rising by _+10% per region_ up to _+50% in region 5_.'
        r'\n\nA hostile unit that _survives for more than 150 turns_ gains'
        r' a _random champion effect_ (_does not stack_).',
}

# 占位原文（用于定位要替换的整行；**不含**行尾，行尾单独拼）
OLD_ZH = {
    'trials.netzach_desc': 'trials.netzach_desc=（占位）卡巴拉质点「胜利」。本考验的具体效果尚未确定。',
    'trials.hod_desc':     'trials.hod_desc=（占位）卡巴拉质点「荣耀」。本考验的具体效果尚未确定。',
}
OLD_EN = {
    'trials.netzach_desc': 'trials.netzach_desc=(Placeholder) The sephirah "Victory". The effect of this trial is not yet decided.',
    'trials.hod_desc':     'trials.hod_desc=(Placeholder) The sephirah "Splendor". The effect of this trial is not yet decided.',
}

fail = False


def bad(msg):
    global fail
    fail = True
    print('  [FAIL] ' + msg)


def good(msg):
    print('  [OK]   ' + msg)


def patch(fname, old_map, new_map):
    path = os.path.join(MSG, fname)
    raw = open(path, 'rb').read()

    # 备份（只留一份，幂等脚本重复跑不会覆盖掉更早的备份之外的东西）
    os.makedirs(BAK, exist_ok=True)
    dst = os.path.join(BAK, fname)
    if not os.path.exists(dst):
        shutil.copy2(path, dst)

    crlf_before = raw.count(b'\r\n')

    for key, newval in new_map.items():
        newline = (key + '=' + newval).encode('utf-8') + b'\r\n'
        oldline = (old_map[key]).encode('utf-8') + b'\r\n'

        if newline in raw:
            good('%s: 已是新文案，跳过（幂等）' % key)
            continue

        n = raw.count(oldline)
        if n != 1:
            bad('%s: 占位原文出现 %d 次（应为 1）' % (key, n))
            continue

        raw = raw.replace(oldline, newline)
        good('%s: 已替换（1 处）' % key)

        # 断言新行本身不含裸 CR/LF（换行只能是字面 \n）
        body = (key + '=' + newval).encode('utf-8')
        if b'\r' in body or b'\n' in body:
            bad('%s: 新正文含真实换行符，应为字面 \\n' % key)

    crlf_after = raw.count(b'\r\n')
    if crlf_after != crlf_before:
        bad('%s: CRLF 计数 %d → %d（应不变）' % (fname, crlf_before, crlf_after))
    else:
        good('%s: CRLF 计数不变（%d）' % (fname, crlf_before))

    if raw.count(b'\n') != raw.count(b'\r\n'):
        bad('%s: 出现裸 LF（本文件应为纯 CRLF）' % fname)
    else:
        good('%s: 仍是纯 CRLF（无裸 LF）' % fname)

    open(path, 'wb').write(raw)
    print('     → 写出 %s（%d 字节）' % (fname, len(raw)))


print('=== 中英两份考验文案 ===')
patch('misc_zh.properties', OLD_ZH, ZH)
patch('misc.properties', OLD_EN, EN)

print('\n=== 结果 ===')
if fail:
    print('有问题，见上方 [FAIL]。')
    sys.exit(1)
print('全部通过。')
