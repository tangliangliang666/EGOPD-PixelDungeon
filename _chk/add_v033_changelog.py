# -*- coding: utf-8 -*-
"""
一次性补丁（幂等）：往 ui/changelist/EGOPD_Changes.java 插入 v0.3.3 改动栏。

为什么用「占位符 + 脚本」而不是直接编辑：
改动栏正文里大量出现 **字面量 \\n**（Java 字符串里的换行转义）。任何一层转义被还原成真换行，
Java 字符串字面量就会「跨行」而编译失败；反过来写成双反斜杠又会显示成 \\n 两个字符。
所以正文写在 _chk/_v033_block.txt 里、换行一律用 @NL@ 占位，由本脚本现拼 chr(92)+'n'。

幂等标记：文件里已出现 v033 即跳过。字节级读写 + 断言 CRLF 增量 == 新增行数。
"""

import os
import shutil
import sys

TARGET = 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/changelist/EGOPD_Changes.java'
BLOCK = '_chk/_v033_block.txt'
BAK = '_chk/_bak_v033_changelog'

ANCHOR = '\t\t//[EGOPD] v0.3.2'
MARK = 'EGOPD v0.3.3'

BS = chr(92)
NL_ESC = BS + 'n'          # Java 源码里的字面量换行转义
PLACEHOLDER = '@NL@'


def main():
    raw = open(TARGET, 'rb').read()
    text = raw.decode('utf-8')

    if MARK in text:
        print('[SKIP] 已存在 %s，跳过' % MARK)
        return 0

    block = open(BLOCK, encoding='utf-8').read()
    if PLACEHOLDER not in block:
        print('[FAIL] 块文件里找不到占位符 %s' % PLACEHOLDER)
        return 1

    # 占位符 → 字面量转义；正文里的制表符缩进原样保留
    block = block.replace(PLACEHOLDER, NL_ESC)

    # 缩进：块文件里「语句」顶格、"..."续行写 2 个 tab（相对缩进）。
    # 统一补两层 ⇒ 语句 2 tab、续行 4 tab，与 v0.3.2 那批的排版逐列一致。
    sep = '\r\n' if '\r\n' in text else '\n'   # ⚠️ 必须用**目标文件自己的**换行，否则整块新行都是裸 LF
    lines = [('\t' * 2 + l) if l.strip() else '' for l in block.split('\n')]
    block = sep.join(lines)

    if ANCHOR not in text:
        print('[FAIL] 找不到锚点 %r' % ANCHOR)
        return 1
    if text.count(ANCHOR) != 1:
        print('[FAIL] 锚点出现 %d 次（期望 1）' % text.count(ANCHOR))
        return 1

    text2 = text.replace(ANCHOR, block + ANCHOR, 1)

    # 自检：块内不得出现真换行混进 Java 字符串（即每个 "..." 字面量都在同一行内收尾）
    for i, l in enumerate(text2.split('\n')):
        q = l.count('"') - l.count(BS + '"')
        if q % 2 != 0:
            print('[FAIL] 第 %d 行的双引号不成对，可能有字符串跨行：%s' % (i + 1, l[:100]))
            return 1

    crlf_before = raw.count(b'\r\n')
    bare_before = raw.count(b'\n') - crlf_before
    raw2 = text2.encode('utf-8')
    crlf_after = raw2.count(b'\r\n')
    bare_after = raw2.count(b'\n') - crlf_after
    added = text2.count('\r\n') - text.count('\r\n')

    if crlf_after - crlf_before != added or bare_after != bare_before:
        print('[FAIL] 换行风格被破坏：CRLF %d->%d（新增 %d 行），裸 LF %d->%d'
              % (crlf_before, crlf_after, added, bare_before, bare_after))
        return 1

    os.makedirs(BAK, exist_ok=True)
    bak = os.path.join(BAK, os.path.basename(TARGET) + '.bak')
    if not os.path.exists(bak):
        shutil.copyfile(TARGET, bak)

    open(TARGET, 'wb').write(raw2)
    print('[OK]   已插入 v0.3.3 改动栏（新增 %d 行，CRLF %d -> %d，裸 LF %d）'
          % (added, crlf_before, crlf_after, bare_after))
    return 0


if __name__ == '__main__':
    sys.exit(main())
