# -*- coding: utf-8 -*-
"""EGOPD 发布核验 ③：APK 内 assets/* 与工作区 core/src/main/assets 全量逐条 md5 比对。
zipfile 对「非 UTF-8 标记」的中文条目名按 cp437 解码 ⇒ 先还原再比。"""
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

# 关键文本抽验
print()
print('=== 关键资产抽验（zh 属性文件是否含本版新增内容）===')
for key, needle in [('messages/items/items_zh.properties', '血宴圣杯'),
                    ('messages/items/items_zh.properties', '迫近之日'),
                    ('messages/actors/actors_zh.properties', '中指')]:
    try:
        data = z.read('assets/' + key).decode('utf-8')
        print('  %-40s 含「%s」= %s' % (key, needle, needle in data))
    except KeyError:
        print('  %-40s 条目缺失' % key)

sys.exit(0 if ok else 1)
