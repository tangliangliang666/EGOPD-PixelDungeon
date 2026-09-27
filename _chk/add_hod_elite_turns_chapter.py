# -*- coding: utf-8 -*-
"""把 _chk/_hod_elite_turns_chapter.md 追加到 docs/features.md 末尾（幂等）。

与 add_yesod_container_chapter.py / add_netzach_overlay_chapter.py 同型：
纯追加，靠标题里的日期标记判重，重复跑只会打 [SKIP]。
"""
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CH   = os.path.join(ROOT, '_chk', '_hod_elite_turns_chapter.md')
DST  = os.path.join(ROOT, 'docs', 'features.md')
MARK = '2026-09-27 考验 HOD（荣耀）精英化阈值调整'

ch = open(CH, 'rb').read()
d  = open(DST, 'rb').read()

if MARK.encode('utf-8') in d:
    print('[SKIP] docs/features.md 已含本章（未改动）')
else:
    if not d.endswith(b'\n'):
        d += b'\n'
    open(DST, 'wb').write(d + ch)
    print('[OK] 已追加 %d 字节 → docs/features.md（%d 行 → %d 行）'
          % (len(ch), d.count(b'\n'), (d + ch).count(b'\n')))
