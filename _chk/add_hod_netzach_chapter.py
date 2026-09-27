# -*- coding: utf-8 -*-
"""把 HOD/NETZACH 章节追加到 docs/features.md（幂等、字节级、保 LF）。"""
import io, os, shutil, sys

FEAT = 'docs/features.md'
CHAP = '_chk/_hod_netzach_chapter.md'
MARK = '# 2026-09-26（续）考验 NETZACH（胜利）＋ HOD（荣耀）'
BAK  = '_chk/_bak_hodnetzach'

def main():
    feat = io.open(FEAT, 'rb').read()
    chap = io.open(CHAP, 'rb').read()

    # 幂等：已经追加过就退出
    if MARK.encode('utf-8') in feat:
        print('[SKIP] features.md 已含本章，未重复追加')
        return 0

    # 断言：章节文件自身是纯 LF
    assert b'\r\n' not in chap, '章节文件含 CRLF'

    # 断言：目标文件在改动前是纯 LF
    before_crlf = feat.count(b'\r\n')
    before_bare = feat.count(b'\n') - before_crlf
    assert before_crlf == 0, 'features.md 原本就含 CRLF，先查清再改'

    # 备份
    if not os.path.isdir(BAK):
        os.makedirs(BAK)
    bak = os.path.join(BAK, 'features.md.bak')
    if not os.path.exists(bak):
        shutil.copyfile(FEAT, bak)

    # 追加：确保原文件以 \n 结尾，再接章节（章节自己以 \n 开头做空行分隔）
    if not feat.endswith(b'\n'):
        feat += b'\n'
    out = feat + chap

    after_crlf = out.count(b'\r\n')
    after_bare = out.count(b'\n') - after_crlf
    assert after_crlf == 0, '追加后出现 CRLF ⇒ 行尾被污染'
    # 行尾增量 = 章节里的裸 LF 数（原文件 CRLF 与裸 LF 均未变）
    added = after_bare - before_bare
    assert added == chap.count(b'\n'), 'LF 增量与章节不符'

    io.open(FEAT, 'wb').write(out)
    print('[OK] 追加 %d 行（裸 LF +%d，CRLF 恒 0）' % (chap.count(b'\n'), added))
    print('[OK] 备份 %s' % bak)
    return 0

sys.exit(main())
