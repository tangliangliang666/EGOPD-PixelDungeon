# -*- coding: utf-8 -*-
"""给 windows.properties / windows_zh.properties 追加「开启该考验」这条开关文案。

字节级改写（本仓 .properties 是 CRLF，用文本模式写会把整个文件改成 LF），
幂等：已经加过就跳过。
"""
import io
import os

BASE = r'D:/PD/core/src/main/assets/messages/windows'

TARGETS = [
    ('windows.properties',    'windows.wndtrials.title=Trials',
     'windows.wndtrials.enable=Enable this trial'),
    ('windows_zh.properties', 'windows.wndtrials.title=考验',
     'windows.wndtrials.enable=开启该考验'),
]

for name, anchor, newline in TARGETS:
    path = os.path.join(BASE, name)
    raw = open(path, 'rb').read()

    key = newline.split('=')[0].encode('utf-8')
    if key + b'=' in raw:
        print('[SKIP] %-24s 已存在 %s' % (name, key.decode('utf-8')))
        continue

    a = anchor.encode('utf-8') + b'\r\n'
    assert raw.count(a) == 1, '%s 里锚点命中 %d 次（应为 1）' % (name, raw.count(a))

    raw2 = raw.replace(a, a + newline.encode('utf-8') + b'\r\n')
    assert raw2.count(b'\r\n') == raw.count(b'\r\n') + 1, 'CRLF 计数不符'

    open(path, 'wb').write(raw2)
    print('[OK]   %-24s +1 行：%s' % (name, newline))
