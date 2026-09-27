# -*- coding: utf-8 -*-
"""向 EGOPD_Changes.java 的 v0.3.5 条目追加一条「测试层草叶细节改取第二区」（幂等，字节级 IO）。

EGOPD_Changes.java 是 CRLF 文件，故全程按字节改写，新增行统一用 \r\n，
并在改后断言「CRLF 增量 == 新增行数、裸 LF 计数不变」。
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(ROOT, 'core', 'src', 'main', 'java', 'com', 'shatteredpixel',
                    'shatteredpixeldungeon', 'ui', 'changelist', 'EGOPD_Changes.java')
BAK = os.path.join(ROOT, '_chk', '_bak_lobstage', 'EGOPD_Changes.java.bak')

MARK = '测试层草地：草叶细节改取第二区'
ANCHOR = b'\t\t//[EGOPD] v0.3.4'

NEW_LINES = [
    '\t\tv035.addButton( new ChangeButton( Icons.get(Icons.SHPX), "' + MARK + '",',
    '\t\t\t\t"_-_ 27 层测试层的草地草叶细节改取第二区（监狱），与 tiles_lob 的监狱风格草皮对齐。" ) );',
    '',
]


def main():
    d = open(JAVA, 'rb').read()
    orig_crlf, orig_lf = d.count(b'\r\n'), d.count(b'\n')
    orig_bare = orig_lf - orig_crlf

    # 幂等：已写过就跳过
    if MARK.encode('utf-8') in d:
        print('已存在该条目，跳过（幂等）')
        return 0

    if b'EGOPD v0.3.5' not in d:
        print('失败：找不到 v0.3.5 条目，锚点假设有误')
        return 1
    if d.count(ANCHOR) != 1:
        print('失败：锚点 //[EGOPD] v0.3.4 出现 %d 次（应为 1 次）' % d.count(ANCHOR))
        return 1

    block = b''.join(l.encode('utf-8') + b'\r\n' for l in NEW_LINES)

    os.makedirs(os.path.dirname(BAK), exist_ok=True)
    with open(BAK, 'wb') as f:
        f.write(d)
    print('备份 →', os.path.relpath(BAK, ROOT))

    new = d.replace(ANCHOR, block + ANCHOR)
    added = len(NEW_LINES)

    new_crlf, new_lf = new.count(b'\r\n'), new.count(b'\n')
    new_bare = new_lf - new_crlf
    print('新增行 %d ⇒ CRLF 增量 %d（应相等）、裸 LF %d→%d（应不变）'
          % (added, new_crlf - orig_crlf, orig_bare, new_bare))
    if new_crlf - orig_crlf != added or new_bare != orig_bare:
        print('失败：行尾记账不符，放弃写入')
        return 1

    with open(JAVA, 'wb') as f:
        f.write(new)
    print('写入完成：', os.path.relpath(JAVA, ROOT))
    print('CRLF=%d 裸LF=%d' % (new_crlf, new_bare))
    return 0


if __name__ == '__main__':
    sys.exit(main())
