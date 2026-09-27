# -*- coding: utf-8 -*-
"""出 EGOPD v0.3.1：改版本号 + 写 EGOPD_Changes 更新日志。
一次到位、逐条断言；任一断言失败即中止（不落盘）。"""
import os
import sys

BASE = r'D:/PD'
GRADLE = os.path.join(BASE, 'build.gradle')
CHANGES = os.path.join(BASE, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/changelist/EGOPD_Changes.java')
BAK = os.path.join(BASE, '_chk/_bak_release_031')


def read(p):
    with open(p, 'rb') as f:
        raw = f.read()
    crlf = b'\r\n' in raw
    assert b'\r' not in raw.replace(b'\r\n', b''), '存在裸 CR：' + p
    return raw.decode('utf-8').replace('\r\n', '\n'), crlf


def write(p, s, crlf):
    out = s.replace('\n', '\r\n') if crlf else s
    with open(p, 'wb') as f:
        f.write(out.encode('utf-8'))


def backup(p):
    os.makedirs(BAK, exist_ok=True)
    with open(p, 'rb') as f:
        data = f.read()
    with open(os.path.join(BAK, os.path.basename(p)), 'wb') as f:
        f.write(data)


ok = True


def chk(cond, msg):
    global ok
    print(('  [OK]   ' if cond else '  [FAIL] ') + msg)
    if not cond:
        ok = False


# =====================================================================
# ① build.gradle 版本号
# =====================================================================
print('[1] build.gradle')
g, g_crlf = read(GRADLE)
backup(GRADLE)
chk("appVersionCode = 930" in g, '改动前 appVersionCode = 930')
chk("appVersionName = '0.3.0'" in g, "改动前 appVersionName = '0.3.0'")

g2 = g.replace('appVersionCode = 930', 'appVersionCode = 931', 1)
g2 = g2.replace("appVersionName = '0.3.0'", "appVersionName = '0.3.1'", 1)
chk('appVersionCode = 931' in g2, '写入 appVersionCode = 931')
chk("appVersionName = '0.3.1'" in g2, "写入 appVersionName = '0.3.1'")
chk(g2 != g, 'build.gradle 确实发生变化')
# 只改一处
chk(g2.count('appVersionCode') == g.count('appVersionCode'), 'appVersionCode 出现次数不变（未误伤）')
write(GRADLE, g2, g_crlf)
print('  已写入 build.gradle')

# =====================================================================
# ② EGOPD_Changes.java
# =====================================================================
print('[2] EGOPD_Changes.java')
c, c_crlf = read(CHANGES)
backup(CHANGES)

OLD_IMPORT = ("package com.shatteredpixel.shatteredpixeldungeon.ui.changelist;\n"
              "\n"
              "import java.util.ArrayList;\n")
NEW_IMPORT = ("package com.shatteredpixel.shatteredpixeldungeon.ui.changelist;\n"
              "\n"
              "import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;\n"
              "import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;\n"
              "import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;\n"
              "\n"
              "import java.util.ArrayList;\n")
chk(c.count(OLD_IMPORT) == 1, '定位 import 段（唯一）')
c = c.replace(OLD_IMPORT, NEW_IMPORT, 1)

ANCHOR = "\t\t//[EGOPD] v0.3.0：本栏目自 0.3.0 起启用"
chk(c.count(ANCHOR) == 1, '定位 v0.3.0 注释行（唯一）')

BLOCK = (
    "\t\t//[EGOPD] v0.3.1\n"
    "\t\tChangeInfo v031 = new ChangeInfo(\"EGOPD v0.3.1\", true, \"\");\n"
    "\t\tv031.hardlight(0xB3001E);\n"
    "\t\tchangeInfos.add(v031);\n"
    "\n"
    "\t\tv031.addButton( new ChangeButton( Icons.get(Icons.SHPX), \"Bug 修复\",\n"
    "\t\t\t\t\"_-_ 修复了若干已发现的 Bug。\" ) );\n"
    "\n"
    "\t\tv031.addButton( new ChangeButton( new ItemSprite(ItemSpriteSheet.WRIST_SLIT), \"中指长兄：割腕穿透无敌\",\n"
    "\t\t\t\t\"_-_ 中指长兄的盔甲技能在_无敌_期间，_割腕_与_蓄血圣杯_可以无限升级，等于白送成长。\\n\" +\n"
    "\t\t\t\t\"\\n\" +\n"
    "\t\t\t\t\"_-_ 现在，_割腕_、_蓄血圣杯_这类_以自伤换取成长_的行为会_绕过无敌的免死判定_，自伤与成长照常结算。\" ) );\n"
    "\n"
    "\t\tv031.addButton( new ChangeButton( new ItemSprite(ItemSpriteSheet.ARTIFACT_BLOOD_FEAST_CHALICE1), \"EGO 特化神器\",\n"
    "\t\t\t\t\"_-_ 新增 5 件_EGO 特化神器_：原版神器的强化形态，在原版效果之上各附加一条新效果——\" +\n"
    "\t\t\t\t\"_血宴圣杯_、_一生炖菜_、_他人之锁_、_9章2节_、_迫近之日_。\\n\" +\n"
    "\t\t\t\t\"\\n\" +\n"
    "\t\t\t\t\"_-_ 在_炼金台_以_原版神器 + 60 脑啡肽_（12 点炼金能量）合成，保留原神器的等级、充能与状态。\" ) );\n"
    "\n"
)
c = c.replace(ANCHOR, BLOCK + ANCHOR, 1)

write(CHANGES, c, c_crlf)

# 回读复核
c2, _ = read(CHANGES)
chk('import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;' in c2, 'Icons import 到位')
chk('import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;' in c2, 'ItemSpriteSheet import 到位')
chk('import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;' in c2, 'ItemSprite import 到位')
chk(c2.count('new ChangeInfo("EGOPD v0.3.1"') == 1, 'v0.3.1 条目唯一')
chk(c2.count('new ChangeInfo("EGOPD v0.3.0"') == 1, 'v0.3.0 条目仍在')
chk(c2.index('EGOPD v0.3.1') < c2.index('EGOPD v0.3.0'), 'v0.3.1 排在 v0.3.0 之前（新的在上）')
chk(c2.count('v031.addButton') == 3, 'v0.3.1 下恰有 3 条改动')
chk(c2.count('{') == c2.count('}'), '花括号配平 %d/%d' % (c2.count('{'), c2.count('}')))
chk(c2.count('(') == c2.count(')'), '圆括号配平 %d/%d' % (c2.count('('), c2.count(')')))
chk('/n/' not in c2, '无 /n/ 误写')
chk('\ufffd' not in c2, '无 U+FFFD')
# 三个图标常量必须真实存在
sheet = open(os.path.join(BASE, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/ItemSpriteSheet.java'), encoding='utf-8').read()
chk('WRIST_SLIT' in sheet, 'ItemSpriteSheet.WRIST_SLIT 存在')
chk('ARTIFACT_BLOOD_FEAST_CHALICE1' in sheet, 'ItemSpriteSheet.ARTIFACT_BLOOD_FEAST_CHALICE1 存在')
ico = open(os.path.join(BASE, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/Icons.java'), encoding='utf-8').read()
chk('\tSHPX,' in ico, 'Icons.SHPX 存在')
# 中文名硬断言
items_zh = open(os.path.join(BASE, 'core/src/main/assets/messages/items/items_zh.properties'), encoding='utf-8').read()
for cn in ['血宴圣杯', '一生炖菜', '他人之锁', '9章2节', '迫近之日']:
    chk(cn in items_zh, 'items_zh 含 %s' % cn)

print()
print('===== 结果：' + ('ALL PASS' if ok else 'FAIL（未完整落盘，请检查）') + ' =====')
sys.exit(0 if ok else 1)
