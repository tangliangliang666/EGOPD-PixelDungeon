#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""反向自测：把 render.js 的陷阱优先级故意改错，verify_trap_render.js 必须报错。

为什么要做这件事
----------------
`verify_trap_render.js` 全绿只能说明「现在是对的」，不能说明「它真的会报警」。
如果脚本里的断言写反了、或者根本没走到断言，它照样全绿。

做法：把 render.js 复制到临时目录，把「陷阱优先于植物」这一条**换序**
（先查植物、后查陷阱），再跑核验脚本 —— 期望它**失败**且失败项正是优先级那条。
另外再测一个更隐蔽的破坏：把 `shape * 16` 改成 `shape`（帧号公式写错），
期望「全部 33 个陷阱帧号 = color+shape*16」这条挂掉。
"""
import os
import re
import shutil
import subprocess
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RENDER = os.path.join(ROOT, 'tools/terrain-editor/render.js')
VERIFY = os.path.join(ROOT, '_chk/verify_trap_render.js')

# 找 node：优先托管版
CAND = [
    r'C:\Users\14675\.workbuddy\binaries\node\versions\22.22.2-2\node.exe',
    'node',
]
NODE = None
for c in CAND:
    if c == 'node' or os.path.exists(c):
        NODE = c
        break
if NODE is None:
    print('找不到 node');
    sys.exit(2)


def run_verify(render_path):
    env = dict(os.environ)
    # 让核验脚本改从指定路径加载（脚本里已支持 TE_BROKEN 环境变量）
    env['TE_BROKEN'] = render_path
    # 核验脚本里 RENDER 是常量，这里改用「直接替换源文件」的做法更可靠：
    # 所以本函数只负责跑，源替换由调用方完成。
    p = subprocess.run([NODE, VERIFY], capture_output=True, text=True, env=env)
    return p.returncode, (p.stdout or '') + (p.stderr or '')


def mutate(patches):
    """把 render.js 打补丁写到临时文件，返回临时路径。"""
    src = open(RENDER, 'r', encoding='utf-8').read()
    for old, new in patches:
        if old not in src:
            print('  ✗ 破坏点没找到：' + old[:60])
            return None
        src = src.replace(old, new, 1)
    fd, tmp = tempfile.mkstemp(suffix='.js', prefix='render_broken_')
    os.close(fd)
    with open(tmp, 'w', encoding='utf-8') as f:
        f.write(src)
    return tmp


def try_case(name, patches, expect_substr):
    print('=== [反向自测] ' + name + ' ===')
    tmp = mutate(patches)
    if tmp is None:
        return False
    # 备份原文件，把破坏版放到原位跑（因为核验脚本里路径是写死的）
    bak = RENDER + '.bak_selftest'
    shutil.copy2(RENDER, bak)
    try:
        shutil.copy2(tmp, RENDER)
        code, out = run_verify(RENDER)
        broke = (code != 0)
        hit = expect_substr in out
        if broke and hit:
            print('  ✓ 核验失败（退出码 %d）且命中预期失败项：%s' % (code, expect_substr))
            return True
        elif broke and not hit:
            print('  ✗ 核验失败但不是预期的那条；实际输出：')
            print('\n'.join('      ' + l for l in out.strip().split('\n')[-12:]))
            return False
        else:
            print('  ✗ 破坏了却没报错 ⇒ 核验脚本对这类破坏无效')
            return False
    finally:
        shutil.copy2(bak, RENDER)
        os.remove(bak)
        os.remove(tmp)


def main():
    ok = []

    # 破坏 1：把「陷阱优先于植物」改成「植物优先于陷阱」
    # 原文里陷阱分支在植物分支之前，交换两段的 if 条件体即可视为换序。
    # 最简做法：把陷阱分支的 `if (S.traps && S.traps[pos])` 改成永不命中，
    # 这样植物就会抢先 —— 等价于优先级写反。
    ok.append(try_case(
        '把陷阱分支改成永不命中（等价于植物抢先）',
        [('if (S.traps && S.traps[pos]) {', 'if (false && S.traps && S.traps[pos]) {')],
        '陷阱与植物同格时陷阱优先'))

    # 破坏 2：帧号公式写错（shape*16 变成 shape）
    ok.append(try_case(
        '帧号公式 shape*16 写成 shape',
        [('return (tp.active ? tp.color : TRAP_COLOR.BLACK) + tp.shape * 16;',
          'return (tp.active ? tp.color : TRAP_COLOR.BLACK) + tp.shape;')],
        '帧号'))

    # 破坏 3：visible 判断反了（未发现也画）
    ok.append(try_case(
        'visible 判断反向（未发现的陷阱也画）',
        [('if (!tp.visible) return -1;', 'if (tp.visible) return -1;')],
        'visible=false'))

    # 破坏 4：active 判断反了
    ok.append(try_case(
        'active 判断反向（失效陷阱仍用原色）',
        [('return (tp.active ? tp.color : TRAP_COLOR.BLACK)',
          'return (tp.active ? TRAP_COLOR.BLACK : tp.color)')],
        'active=false'))

    print('')
    if all(ok):
        print('✅ 反向自测全部通过（%d/%d）：核验脚本对每类破坏都会报警' % (sum(ok), len(ok)))
        return 0
    print('❌ 反向自测有 %d 项未通过' % (len(ok) - sum(ok)))
    return 1


if __name__ == '__main__':
    sys.exit(main())
