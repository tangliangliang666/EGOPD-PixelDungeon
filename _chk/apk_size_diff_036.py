# -*- coding: utf-8 -*-
"""EGOPD 发布核验 ④（v0.3.6）：新旧包体积变化必须能解释。
① 按顶层目录统计条目数与压缩后体积；
② 逐条找「新增/删除」的 assets 条目；
③ 找压缩体积变化最大的条目；
④ 量「增量打包留下的垃圾空洞」：遍历 local file header 的 header_offset，
   与「上一节 data 结束位置」的差 = 空洞字节（ZipFlinger 原地重写小条目所致）。
   ⇒ 有了空洞量就不必用文件大小判断内容变没变。"""
import os
import struct
import sys
import zipfile

BASE = r'D:/PD'
NEW = os.path.join(BASE, 'android/build/outputs/apk/debug/android-debug.apk')
OLD = os.path.join(BASE, 'EGOPD_0.3.5.APK')


def top(name):
    parts = name.split('/')
    return parts[0] if len(parts) > 1 else '(root)'


def entries(path):
    z = zipfile.ZipFile(path)
    d = {}
    for i in z.infolist():
        if i.is_dir():
            continue
        d[i.filename] = i.compress_size
    z.close()
    return d


en = entries(NEW)
eo = entries(OLD)

# ① 顶层目录汇总
stat_n, stat_o = {}, {}
for k, v in en.items():
    t = top(k)
    a, b = stat_n.get(t, (0, 0))
    stat_n[t] = (a + 1, b + v)
for k, v in eo.items():
    t = top(k)
    a, b = stat_o.get(t, (0, 0))
    stat_o[t] = (a + 1, b + v)

print('%-14s %10s %10s | %14s %14s | %s' % ('顶层目录', '新条目', '旧条目', '新压缩字节', '旧压缩字节', '差值'))
for t in sorted(set(stat_n) | set(stat_o)):
    a = stat_n.get(t, (0, 0))
    b = stat_o.get(t, (0, 0))
    print('%-14s %10d %10d | %14d %14d | %+d' % (t, a[0], b[0], a[1], b[1], a[1] - b[1]))

# ② 新增/删除条目
added = sorted(set(en) - set(eo))
removed = sorted(set(eo) - set(en))
print()
print('新增条目 %d 个：' % len(added))
for k in added:
    print('   + %-60s %d' % (k, en[k]))
print('删除条目 %d 个：' % len(removed))
for k in removed:
    print('   - %-60s %d' % (k, eo[k]))

# ③ 变动最大的条目
deltas = sorted(((en[k] - eo[k], k) for k in set(en) & set(eo)), key=lambda x: -abs(x[0]))
print()
print('压缩体积变动最大的 12 条：')
for dv, k in deltas[:12]:
    if dv == 0:
        break
    print('   %+9d  %s' % (dv, k))

# ④ 空洞
def holes(path):
    data = open(path, 'rb').read()
    pos = expect = total = cnt = 0
    n = len(data)
    while pos + 4 <= n:
        sig = data[pos:pos + 4]
        if sig == b'PK\x03\x04':
            (_, _, _, _, _, _, _, csize, _, nlen, elen) = struct.unpack('<IHHHHHIIIHH', data[pos:pos + 30])
            end = pos + 30 + nlen + elen + csize
            if pos > expect:
                total += pos - expect
                cnt += 1
            expect = end
            pos = end
        elif sig == b'PK\x01\x02':
            break
        else:
            pos += 1
    return total, cnt


print()
print('新包文件大小 : %d' % os.path.getsize(NEW))
print('旧包文件大小 : %d' % os.path.getsize(OLD))
print('大小差       : %+d' % (os.path.getsize(NEW) - os.path.getsize(OLD)))
hn, cn = holes(NEW)
ho, co = holes(OLD)
print('新包空洞     : %d 字节 / %d 处' % (hn, cn))
print('旧包空洞     : %d 字节 / %d 处' % (ho, co))
print('空洞净差     : %+d' % (hn - ho))

sys.exit(0)
