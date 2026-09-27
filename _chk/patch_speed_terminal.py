# 幂等补丁：把「移动速度」改成单一出口，让考验的延迟夹取无法被绕过。
#
# 背景：Char 里原来的 `public float speed()` 被 10 个子类覆写，其中
#   · Ghost / RatKing 直接返回硬编码值（不调 super）
#   · CrystalGuardian / Thief / DM300 在 super 之后再乘系数
#   · PowerOfMany / ShadowClone / DriedRose 在 super 之后再乘系数
# 所以「把夹取放进某个覆写点」一定会有路径漏夹。正确做法是把基类的方法改名、
# 只留一个 final 的出口（见 Char.speed / Char.speedRaw，已手工改好）。
#
# 本脚本做三件事（全部按「数量断言」校验，不是比存在）：
#   ① 9 个文件里 `public float speed() {`  → `public float speedRaw() {`
#   ② 同文件里 `super.speed()`              → `super.speedRaw()`
#   ③ CrystalGuardian 里那处「破水晶额外一步」的 spend：
#      `1/super.speed()` → `1/Trials.modifyMoveSpeed(this, super.speedRaw())`
#      （必须显式过考验夹取，但又不能改用 speed()——那会连带吃上它自己在狭窄空间的 /4 惩罚，
#        那就改了原版行为。脚本顺带补 Trials 的 import。）
#
# 已应用则跳过（幂等）；改动前把每个文件的原文拷贝到 _chk/_bak_speed/。
import os
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon')
BAK = os.path.join(ROOT, '_chk/_bak_speed')

# 相对 SRC 的路径 -> (期望的 speed() 声明数, 期望的 super.speed() 数)
FILES = {
    'actors/mobs/Mob.java': (1, 1),
    'actors/mobs/CrystalGuardian.java': (1, 3),
    'actors/mobs/DM300.java': (1, 1),
    'actors/mobs/npcs/Ghost.java': (1, 0),
    'actors/mobs/npcs/RatKing.java': (1, 0),
    'actors/mobs/Thief.java': (1, 2),
    'actors/hero/abilities/cleric/PowerOfMany.java': (1, 1),
    'actors/hero/abilities/rogue/ShadowClone.java': (1, 1),
    'items/artifacts/DriedRose.java': (1, 1),
}

# 已手工改好的文件：只断言、不修改
DONE = {
    'actors/hero/Hero.java': (0, 0),
}

DECL_OLD = 'public float speed() {'
DECL_NEW = 'public float speedRaw() {'
SUPER_OLD = 'super.speed()'
SUPER_NEW = 'super.speedRaw()'

CG = 'actors/mobs/CrystalGuardian.java'
CG_OLD = 'spend(1/super.speedRaw());'
CG_NEW = 'spend(1/Trials.modifyMoveSpeed(this, super.speedRaw()));'
CG_IMPORT_ANCHOR = 'import com.shatteredpixel.shatteredpixeldungeon.Dungeon;\n'
CG_IMPORT_NEW = 'import com.shatteredpixel.shatteredpixeldungeon.Dungeon;\nimport com.shatteredpixel.shatteredpixeldungeon.Trials;\n'

fails = []


def fail(msg):
    fails.append(msg)
    print('  [FAIL] ' + msg)


def ok(msg):
    print('  [OK]   ' + msg)


print('=' * 78)
print('补丁：移动速度单一出口（Char.speed final + 子类覆写改名 speedRaw）')
print('=' * 78)

if not os.path.isdir(BAK):
    os.makedirs(BAK)

print('\n--- ① 已手工改好的文件（只检查） ---')
for rel, (decl, sup) in DONE.items():
    p = os.path.join(SRC, rel)
    t = open(p, encoding='utf-8').read()
    d = t.count(DECL_OLD)
    s = t.count(SUPER_OLD)
    if d == decl and s == sup:
        ok('%s：speed() 覆写 %d 处、super.speed() %d 处（符合预期）' % (rel, d, s))
    else:
        fail('%s：期望 (%d,%d)，实得 (%d,%d)' % (rel, decl, sup, d, s))
    # Hero 必须已经有 attackDelay 收口（由本文件之外的改动负责，这里仅提示，不判失败）
    if 'speedRaw()' not in t:
        fail('%s：没有找到 speedRaw()，手工改名可能未生效' % rel)

print('\n--- ② 机械改名（幂等） ---')
for rel, (decl, sup) in FILES.items():
    p = os.path.join(SRC, rel)
    t = open(p, encoding='utf-8').read()

    d = t.count(DECL_OLD)
    s = t.count(SUPER_OLD)

    if d == 0 and s == 0 and DECL_NEW in t:
        ok('%s：已应用过，跳过' % rel)
        continue

    if d != decl or s != sup:
        fail('%s：期望 (%d,%d)，实得 (%d,%d) —— 不改，人工确认' % (rel, decl, sup, d, s))
        continue

    bak = os.path.join(BAK, rel.replace('/', '__'))
    shutil.copy2(p, bak)

    n = t.replace(DECL_OLD, DECL_NEW).replace(SUPER_OLD, SUPER_NEW)
    open(p, 'w', encoding='utf-8', newline='').write(n)
    ok('%s：改名 %d 处声明 + %d 处 super（原文备份 %s）' % (rel, d, s, os.path.relpath(bak, ROOT)))

print('\n--- ③ CrystalGuardian 破水晶的额外一步改走考验夹取 ---')
p = os.path.join(SRC, CG)
t = open(p, encoding='utf-8').read()

if CG_NEW in t:
    ok('已应用过，跳过')
else:
    if CG_OLD not in t:
        fail('找不到锚点 %r' % CG_OLD)
    else:
        t = t.replace(CG_OLD, CG_NEW)
        ok('spend 锚点已替换')
    if 'import com.shatteredpixel.shatteredpixeldungeon.Trials;' in t:
        ok('Trials import 已存在')
    elif CG_IMPORT_ANCHOR in t:
        t = t.replace(CG_IMPORT_ANCHOR, CG_IMPORT_NEW)
        ok('已补 Trials import')
    else:
        fail('找不到 import 锚点，无法补 Trials import')
    open(p, 'w', encoding='utf-8', newline='').write(t)

print('\n--- ④ 终局断言：全仓不应再有覆写 speed()、也不应再有 super.speed() ---')
import subprocess
for pat, desc in ((DECL_OLD, '覆写 speed()'), (SUPER_OLD, 'super.speed()')):
    r = subprocess.run(['grep', '-rn', '--include=*.java', pat.replace('(', r'\(').replace(')', r'\)'),
                        os.path.join(ROOT, 'core/src/main/java')],
                       capture_output=True, text=True)
    hits = [l for l in r.stdout.splitlines() if l.strip()]
    if hits:
        for h in hits[:10]:
            fail('仍有 %s：%s' % (desc, h.replace(ROOT, '.')))
    else:
        ok('全仓已无 %s' % desc)

print('\n--- ⑤ 出口唯一性：Char.speed() 必须是 final ---')
char = open(os.path.join(SRC, 'actors/Char.java'), encoding='utf-8').read()
if 'public final float speed(){' in char or 'public final float speed() {' in char:
    ok('Char.speed() 已是 final')
else:
    fail('Char.speed() 不是 final（或被改坏）')
if 'public float speedRaw() {' in char:
    ok('Char.speedRaw() 在位')
else:
    fail('Char.speedRaw() 缺失')

print('\n' + '=' * 78)
if fails:
    print('!! %d 条 FAIL：' % len(fails))
    for f in fails:
        print('   - ' + f)
    sys.exit(1)
print('ALL PASS —— 移动速度已收口到唯一的 final speed()')
print('=' * 78)
