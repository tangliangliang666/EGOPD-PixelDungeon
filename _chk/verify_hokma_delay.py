# 核验考验「HOKMA（智慧）」的延迟上下限实现。
#
# 为什么需要它：HOKMA 的三条规则都作用在**延迟**上，而延迟散落在很多站点
# （`1/speed()` 有 12+ 处、`delay/speed()` 1 处、攻击另有出口）。所以核验不能只看
# 「函数写没写」，必须证明**收口是唯一的、且没人能绕过**。本脚本分三层：
#
#   ① 源码结构层：Char.speed() 必须 final、全仓不得再有别的 `float speed()` 覆写、
#      所有覆写必须叫 speedRaw()、Hero.attackDelay() 必须 final 且经 Trials 终局、
#      Hero 那条 `delay / speed()` 的 spend 必须过 Trials.modifyHeroMoveDelay。
#   ② 编译产物层：用 javap 看签名，证明编出来的东西真的长这样（源码解析可能被注释骗到）。
#   ③ 字节码层：用 javap -c 看 Trials 三个方法的**指令**，证明
#      英雄分支真的调了 Math.min、敌方分支真的调了 Math.max、常量真的是 1f/1f/0.5f。
#      —— 这一层专门防「min/max 写反」「常量抄错」，是纯源码断言抓不到的。
#
# 数值结论由 ①③ 两条已证事实推导：延迟 = 1/速度 ⇒ 英雄 min(速度,1) 给出延迟 ≥1、
# 敌方 max(速度,1) 给出延迟 ≤1；攻击 max(原值,0.5) 给出 ≥0.5。
#
# 用法：
#   python _chk/verify_hokma_delay.py            # 需先按 skill 的单文件 javac 命令编到 _chk/_javachk
#   python _chk/verify_hokma_delay.py --selftest # 反例自测：证明判据不恒真
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SRC = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon')
CLASSES = os.path.join(HERE, '_javachk')
JAVAP = 'D:/PD/tools/jdk-21.0.12.1+1/bin/javap'

ok = True


def chk(cond, msg):
    global ok
    print(('  [OK]   ' if cond else '  [FAIL] ') + msg)
    if not cond:
        ok = False


def read(rel):
    return open(os.path.join(SRC, rel), encoding='utf-8').read()


def javap(args):
    r = subprocess.run([JAVAP] + args, capture_output=True, text=True, encoding='utf-8',
                       errors='replace')
    return r.stdout + r.stderr


# ---------------------------------------------------------------- 判据（供 selftest 复用）
# 注意写法：必须把修饰符一起匹配、再从捕获组判断有没有 final。
# 早先写成 `\b(?!final\b)float\s+speed` 是错的 —— 前瞻与 `float` 同位置，
# 而 `final` 在 `float` **之前**，所以 `public final float speed()` 会被误判成非 final。
# （这个 bug 就是被下面的 --selftest 抓出来的。）
SPEED_DECL_RE = re.compile(r'(?:public|protected|private)\s+(final\s+)?float\s+speed\s*\(\s*\)')


def scan_speed_decls(text):
    """返回 (非 final 的 float speed() 声明数, final 的声明数)。

    `float speedRaw()` 不会命中（speed 后跟的是 R 而非 `(`）。"""
    nf = f = 0
    for m in SPEED_DECL_RE.finditer(text):
        if m.group(1):
            f += 1
        else:
            nf += 1
    return nf, f


def selftest():
    print('== selftest：确认「非 final 覆写」判据不是恒真 ==')
    cases = [
        ('public final float speed(){ return 0f; }\n', (0, 1), '只有 final 版'),
        ('public float speed(){ return 0f; }\n', (1, 0), '只有非 final 版（要防的绕过路径）'),
        ('float speedRaw(){ return 0f; }\npublic final float speed(){ return 0f; }\n',
         (0, 1), 'speedRaw + final speed'),
        ('public float speed(){}\npublic final float speed(){}\n', (1, 1), '两者混在一起'),
        ('protected final float speed(){ return 0f; }\n', (0, 1), 'protected final 也算 final'),
    ]
    for src, want, desc in cases:
        got = scan_speed_decls(src)
        print('  %-28s -> %s（期望 %s）' % (desc, got, want))
        assert got == want, '判据错：%s 实得 %s 期望 %s' % (desc, got, want)
    print('  selftest 通过：抓得住非 final 覆写、不误伤 final 与 speedRaw。\n')


print('=' * 78)
print('核验：考验 HOKMA 的延迟上下限')
print('=' * 78)

if '--selftest' in sys.argv:
    selftest()

trials = read('Trials.java')
char = read('actors/Char.java')
hero = read('actors/hero/Hero.java')

print('\n【① 源码结构层】')

print('\n  a) HOKMA 的位值与数组位置')
chk(re.search(r'\bHOKMA\s*=\s*2\s*;', trials) is not None, 'Trials.HOKMA == 2（即 1<<1，与 NAME_IDS[1] 对齐）')
chk(re.search(r'NAME_IDS\s*=\s*\{[^}]*"hokma"', trials, re.S) is not None, 'NAME_IDS 里含 "hokma"')

print('\n  b) 三个数值常量')
for name, val in (('HERO_MOVE_DELAY_MIN', '1f'), ('ENEMY_MOVE_DELAY_MAX', '1f'),
                  ('HERO_ATTACK_DELAY_MIN', '0.5f')):
    pat = re.compile(r'float\s+' + name + r'\s*=\s*' + re.escape(val) + r'\s*;')
    chk(pat.search(trials) is not None, 'Trials.%s == %s' % (name, val))

print('\n  c) 三个钩子都在，且都受 HOKMA 门控')
for sig in ('modifyMoveSpeed', 'modifyHeroMoveDelay', 'modifyHeroAttackDelay'):
    body = re.search(r'float\s+' + sig + r'\s*\([^)]*\)\s*\{(.*?)\n\t\}', trials, re.S)
    has = body is not None and 'Dungeon.isTrialled( HOKMA )' in body.group(1)
    chk(has, 'Trials.%s 内部有 Dungeon.isTrialled( HOKMA ) 门控' % sig)

print('\n  d) 移动速度出口唯一')
nf, fcount = scan_speed_decls(char)
chk(fcount == 1 and nf == 0, 'Char.java 里有且仅有 1 个 final 的 speed()（实得 final=%d 非final=%d）'
    % (fcount, nf))
chk('public float speedRaw()' in char, 'Char.java 有 public float speedRaw()')

others = []
raw_defs = 0
for dirpath, _dirs, files in os.walk(SRC):
    for fn in files:
        if not fn.endswith('.java'):
            continue
        p = os.path.join(dirpath, fn)
        t = open(p, encoding='utf-8').read()
        if 'float speedRaw()' in t:
            raw_defs += 1
        a, b = scan_speed_decls(t)
        if a or b:
            rel = os.path.relpath(p, SRC).replace(os.sep, '/')
            if rel == 'actors/Char.java':
                if a:
                    others.append((rel, 'Char.speed 不是 final'))
            else:
                others.append((rel, '其它文件也声明了 float speed()（final=%d 非final=%d）' % (b, a)))
for rel, why in others:
    chk(False, '绕过风险：%s —— %s' % (rel, why))
chk(not others, '全仓只有 Char.speed() 一个 float speed() 声明，且它是 final（其余一律 speedRaw）')

chk(raw_defs == 11, '全仓 speedRaw() 定义数为 11（Char 1 + 子类 10），实得 %d' % raw_defs)

chk('Trials.modifyMoveSpeed( this, speedRaw() )' in char,
    'Char.speed() 的返回式确实是 Trials.modifyMoveSpeed( this, speedRaw() )')

print('\n  e) 英雄攻击延迟收口')
chk('public final float attackDelay()' in hero, 'Hero.attackDelay() 是 public final')
chk('protected float attackDelayRaw()' in hero, 'Hero.attackDelayRaw() 存在（原逻辑搬进来了）')
chk('Trials.modifyHeroAttackDelay( attackDelayRaw() )' in hero,
    'Hero.attackDelay() 经 Trials.modifyHeroAttackDelay 终局')
chk(re.search(r'return 0;\s*\n\t\t\}\s*\n\s*\n\t\tfloat delay = 1f;', hero) is not None,
    '「手起刀落」的 return 0 仍在 attackDelayRaw() 内（因此必然被终局夹到 0.5）')

print('\n  f) 英雄移动延迟兜底（0/n 情形）')
chk('Trials.modifyHeroMoveDelay( delay / speed() )' in hero,
    'Hero 的 spend( delay / speed() ) 已过 Trials.modifyHeroMoveDelay')

print('\n【② 编译产物层：javap 签名】')
if not os.path.isdir(CLASSES):
    chk(False, '找不到 %s，请先按 skill 的单文件 javac 命令编译本轮文件' % CLASSES)
else:
    t_sig = javap(['-p', '-classpath', CLASSES, 'com.shatteredpixel.shatteredpixeldungeon.Trials'])
    chk('public static float modifyMoveSpeed(' in t_sig, 'Trials.modifyMoveSpeed 已编入')
    chk('public static float modifyHeroMoveDelay(' in t_sig, 'Trials.modifyHeroMoveDelay 已编入')
    chk('public static float modifyHeroAttackDelay(' in t_sig, 'Trials.modifyHeroAttackDelay 已编入')

    c_sig = javap(['-p', '-classpath', CLASSES, 'com.shatteredpixel.shatteredpixeldungeon.actors.Char'])
    chk('public final float speed();' in c_sig, 'Char.speed() 编译后带 final 标志（子类无法覆写）')
    chk('public float speedRaw();' in c_sig, 'Char.speedRaw() 已编入')

    h_sig = javap(['-p', '-classpath', CLASSES,
                   'com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero'])
    chk('public final float attackDelay();' in h_sig, 'Hero.attackDelay() 编译后带 final 标志')
    chk('protected float attackDelayRaw();' in h_sig, 'Hero.attackDelayRaw() 已编入')

    print('\n【③ 字节码层：min/max 方向与常量值】')
    tc = javap(['-c', '-p', '-classpath', CLASSES,
                'com.shatteredpixel.shatteredpixeldungeon.Trials'])

    def method_bytecode(name):
        m = re.search(r'\n\s+public static float ' + name + r'\(.*?\n(.*?)(?=\n\s+(?:public|private|protected|static)|\Z)',
                      tc, re.S)
        return m.group(1) if m else ''

    mm = method_bytecode('modifyMoveSpeed')
    chk('Dungeon.isTrialled' in mm, 'modifyMoveSpeed 字节码里调了 Dungeon.isTrialled')
    # HOKMA 是编译期常量，javac 会**内联**成 iconst_2，所以断言的是「isTrialled 前压入常量 2」
    # 而不是 getstatic Trials.HOKMA（早先写成后者是错的，会假报 FAIL）。
    chk(re.search(r'iconst_2\s*\n\s*\d+:\s+invokestatic\s+#\d+\s+// Method .*Dungeon\.isTrialled',
                  mm) is not None,
        'modifyMoveSpeed 把 HOKMA 的位值 2 内联压栈后交给 isTrialled（证明门控的是 HOKMA）')
    chk('Char$Alignment.ENEMY' in mm, 'modifyMoveSpeed 字节码里比较了 Char$Alignment.ENEMY')
    chk('java/lang/Math.min' in mm, 'modifyMoveSpeed 用了 Math.min（英雄：速度封顶 ⇒ 延迟托底）')
    chk('java/lang/Math.max' in mm, 'modifyMoveSpeed 用了 Math.max（敌方：速度托底 ⇒ 延迟封顶）')
    # 两侧的上下限值本身必须是 1.0f（fconst_1），防「常量抄成别的数」
    chk(re.search(r'fconst_1\s*\n\s*\d+:\s+invokestatic\s+#\d+\s+// Method java/lang/Math\.min', mm)
        is not None, '英雄分支的上限值确实是 1.0f（fconst_1 → Math.min）')
    chk(re.search(r'fconst_1\s*\n\s*\d+:\s+invokestatic\s+#\d+\s+// Method java/lang/Math\.max', mm)
        is not None, '敌方分支的下限值确实是 1.0f（fconst_1 → Math.max）')
    # 英雄分支必须排在敌方分支之前：否则一旦将来 Hero.alignment 变成 ENEMY，英雄会走错分支
    i_hero = mm.find('Dungeon.hero')
    i_enemy = mm.find('Char$Alignment.ENEMY')
    chk(i_hero != -1 and i_enemy != -1 and i_hero < i_enemy,
        '英雄分支（Dungeon.hero 判定）排在敌方分支之前，优先级正确')

    md = method_bytecode('modifyHeroMoveDelay')
    chk('java/lang/Math.max' in md, 'modifyHeroMoveDelay 用了 Math.max（延迟下限）')

    ma = method_bytecode('modifyHeroAttackDelay')
    chk('java/lang/Math.max' in ma, 'modifyHeroAttackDelay 用了 Math.max')
    chk('0.5' in ma, 'modifyHeroAttackDelay 字节码里出现常量 0.5（实得 ldc 可能是 0.5f）')

print('\n' + '=' * 78)
print('ALL PASS —— HOKMA 的延迟上下限已收口且不可绕过' if ok else '!! 存在 FAIL，见上')
print('=' * 78)
sys.exit(0 if ok else 1)
