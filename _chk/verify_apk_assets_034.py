# -*- coding: utf-8 -*-
"""EGOPD 发布核验 ③（v0.3.4）：APK 内 assets/* 与工作区 core/src/main/assets 全量逐条 md5 比对。
zipfile 对「非 UTF-8 标记」的中文条目名按 cp437 解码 ⇒ 先还原再比。
本版抽验项换成「GEBURA 僵直」的两条玩家文案（zh/en 各查新措辞在、旧措辞已退场）。"""
import hashlib
import os
import sys
import zipfile

BASE = r'D:/PD'
APK = os.path.join(BASE, 'android/build/outputs/apk/debug/android-debug.apk')
SRC = os.path.join(BASE, 'core/src/main/assets')

z = zipfile.ZipFile(APK)


def fix(name):
    if name.startswith('assets/') and not name.isascii():
        try:
            return name.encode('cp437').decode('utf-8')
        except Exception:
            return name
    return name


apk_assets = {}
for info in z.infolist():
    if info.is_dir():
        continue
    fixed = fix(info.filename)
    if fixed.startswith('assets/'):
        apk_assets[fixed[len('assets/'):]] = hashlib.md5(z.read(info.filename)).hexdigest()

ws_assets = {}
for root, dirs, files in os.walk(SRC):
    for fn in files:
        p = os.path.join(root, fn)
        rel = os.path.relpath(p, SRC).replace('\\', '/')
        with open(p, 'rb') as f:
            ws_assets[rel] = hashlib.md5(f.read()).hexdigest()

only_apk = sorted(set(apk_assets) - set(ws_assets))
only_ws = sorted(set(ws_assets) - set(apk_assets))
diff = sorted(k for k in set(apk_assets) & set(ws_assets) if apk_assets[k] != ws_assets[k])

print('APK 内 assets 文件 %d 个 / 工作区 %d 个' % (len(apk_assets), len(ws_assets)))
print('仅 APK 有 : %d' % len(only_apk))
for k in only_apk[:20]:
    print('   +', k)
print('仅工作区有 : %d' % len(only_ws))
for k in only_ws[:20]:
    print('   -', k)
print('md5 不一致 : %d' % len(diff))
for k in diff[:20]:
    print('   !', k)

ok = not only_apk and not only_ws and not diff
print()
print('===== 资产全量比对：' + ('ALL PASS' if ok else 'FAIL') + ' =====')

# 关键资产抽验：本版改动必须真的进了包
print()
print('=== 关键资产抽验（本版新增/改动内容）===')
checks = [
    ('messages/misc/misc_zh.properties', '僵直不动', True),
    ('messages/misc/misc_zh.properties', '并且仍会照常行动', False),
    ('messages/misc/misc.properties', 'staggering in place', True),
    ('messages/misc/misc.properties', 'and they keep taking their turns as usual.', False),
]
sub_ok = True
for key, needle, want in checks:
    try:
        data = z.read('assets/' + key).decode('utf-8')
        got = needle in data
    except KeyError:
        got = None
    flag = 'OK ' if got == want else 'FAIL'
    if got != want:
        sub_ok = False
    print('  [%s] %-40s 含「%s」= %s (期望 %s)' % (flag, key, needle, got, want))

print()
print('===== 关键资产抽验：' + ('ALL PASS' if sub_ok else 'FAIL') + ' =====')

sys.exit(0 if (ok and sub_ok) else 1)
