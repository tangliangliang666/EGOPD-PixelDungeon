# -*- coding: utf-8 -*-
"""把 NETZACH 浮层淡出章节追加到 docs/features.md（幂等、字节级、保 LF）。

与 add_yesod_chapter.py / add_hod_netzach_chapter.py 同款：只做
「读字节 → 断言行尾 → 备份 → 拼接 → 断言行尾 → 写回」。
"""
import io, os, shutil, sys

FEAT = 'docs/features.md'
CHAP = '_chk/_netzach_overlay_chapter.md'
MARK = '# 2026-09-26（续五）考验 NETZACH 浮层淡出'
BAK  = '_chk/_bak_netzachoverlaychap'

def main():
    feat = io.open(FEAT, 'rb').read()
    chap = io.open(CHAP, 'rb').read()

    if MARK.encode('utf-8') in feat:
        print('[SKIP] features.md 已含本章，未重复追加')
        return 0

    assert b'\r\n' not in chap, '章节文件含 CRLF'
    assert chap.startswith(b'\n---\n'), '章节文件开头不是「空行 + 分隔线」'

    before_crlf = feat.count(b'\r\n')
    before_bare = feat.count(b'\n') - before_crlf
    assert before_crlf == 0, 'features.md 原本就含 CRLF，先查清再改'

    if not os.path.isdir(BAK):
        os.makedirs(BAK)
    bak = os.path.join(BAK, 'features.md.bak')
    if not os.path.exists(bak):
        shutil.copyfile(FEAT, bak)

    if not feat.endswith(b'\n'):
        feat += b'\n'
    out = feat + chap

    after_crlf = out.count(b'\r\n')
    after_bare = out.count(b'\n') - after_crlf
    assert after_crlf == 0, '追加后出现 CRLF ⇒ 行尾被污染'
    added = after_bare - before_bare
    assert added == chap.count(b'\n'), 'LF 增量与章节不符'

    io.open(FEAT, 'wb').write(out)
    print('[OK] 追加 %d 行（裸 LF +%d，CRLF 恒 0）' % (chap.count(b'\n'), added))
    print('[OK] 备份 %s' % bak)
    return 0

sys.exit(main())
