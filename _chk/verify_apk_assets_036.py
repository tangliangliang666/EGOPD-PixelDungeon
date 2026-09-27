# -*- coding: utf-8 -*-
"""EGOPD 发布核验 ③（v0.3.6）：APK 内 assets/* 与工作区 core/src/main/assets 全量逐条 md5 比对。
zipfile 对「非 UTF-8 标记」的中文条目名按 cp437 解码 ⇒ 先还原再比。
本版抽验项＝YESOD 未知文本（zh/en）＋ 英文考验描述里「容器不受影响」那半句已退场
＋ NETZACH 的 1/3 格 ＋ 趣味挑战 ＋ **容器自己的名字与描述没被动过**（用户要求只换贴图）。"""
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
    # YESOD 的未知文本（zh / en 逐字；贴图与检视描述、战利品指示器三处共用这一条）
    ('messages/misc/misc_zh.properties', 'trials.yesod_unknown=你不知道这里是什么。', True),
    ('messages/misc/misc.properties', "trials.yesod_unknown=You don't know what this is.", True),
    # 本轮顺手修的英文考验描述：容器**不再**「不受影响」（旧句必须已退场）
    ('messages/misc/misc.properties', '_Items in the inventory_ are unaffected.', True),
    ('messages/misc/misc.properties', 'Containers (chests, skull piles and the like)', False),
    # NETZACH 淡出阈值收紧到 1 / 3 格
    ('messages/misc/misc_zh.properties', '距离英雄超过 1 格', True),
    ('messages/misc/misc.properties', 'more than 3 tiles', True),
    # 三条新考验的描述都不再是占位（0.3.5 的包里它们还是「（占位）」）
    ('messages/misc/misc_zh.properties', 'trials.hod_desc=（占位）', False),
    ('messages/misc/misc_zh.properties', 'trials.yesod_desc=（占位）', False),
    # 趣味挑战
    ('messages/windows/windows_zh.properties', 'windows.wndfunchallenges.title=趣味挑战', True),
    ('messages/windows/windows.properties', 'windows.wndfunchallenges.title=Fun Challenges', True),
    # 容器自己的文本**一字未动**（用户明示：只换贴图，名字与描述保持原样）
    ('messages/items/items_zh.properties', 'items.heap.chest=宝箱', True),
    ('messages/items/items_zh.properties', 'items.heap.skeleton=遗骸', True),
    ('messages/items/items.properties', "items.heap.chest_desc=You won't know what's inside until you open it!", True),
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
