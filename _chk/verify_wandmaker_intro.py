# -*- coding: utf-8 -*-
"""老杖匠开场白「按职业六选一 → 不分职业一句」的源码级核验。

用法：python _chk/verify_wandmaker_intro.py
判据分三组：A=Wandmaker.java 调用点；B=actors_zh.properties；C=actors.properties（基包）。
另有反例自测：把改动前的 git HEAD 内容喂进同一套判据，必须判 FAIL（防「假绿」）。
"""
import os
import re
import subprocess
import sys

ROOT = 'D:/PD/'
JAVA = ROOT + 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/npcs/Wandmaker.java'
ZH = ROOT + 'core/src/main/assets/messages/actors/actors_zh.properties'
EN = ROOT + 'core/src/main/assets/messages/actors/actors.properties'

PK = 'actors.mobs.npcs.wandmaker.'
OLD_KEYS = ['intro_warrior', 'intro_rogue', 'intro_mage',
            'intro_huntress', 'intro_duelist', 'intro_cleric']
KEEP_KEYS = ['name', 'intro_1', 'intro_dust', 'intro_ember', 'intro_berry', 'intro_2',
             'reminder_dust', 'reminder_ember', 'reminder_berry', 'def_verb', 'desc']

fails = []


def chk(ok, msg):
    print(('  ok   ' if ok else '  FAIL ') + msg)
    if not ok:
        fails.append(msg)
    return ok


def load(p):
    with open(p, encoding='utf-8') as f:
        return f.read()


def strip_comments(text):
    s = re.sub(r'/\*.*?\*/', '', text, flags=re.S)
    return re.sub(r'//[^\n]*', '', s)


def code_of(text, pat):
    """取某个方法（或任意锚点）的大括号平衡块，取不到返回 None。"""
    m = re.search(pat, text)
    if not m:
        return None
    i = text.find('{', m.end() - 1)
    if i < 0:
        return None
    depth = 0
    for j in range(i, len(text)):
        if text[j] == '{':
            depth += 1
        elif text[j] == '}':
            depth -= 1
            if depth == 0:
                return text[i:j + 1]
    return None


def active_lines(text):
    """非注释行（注释以 # 或 ! 开头）。"""
    return [l for l in text.split('\n') if l.strip() and not l.lstrip()[0] in '#!']


def prop_vals(text, key):
    """返回该键的 active 取值列表（按 properties 语义，键名不区分大小写、可带前后空白）。"""
    out = []
    for l in active_lines(text):
        name, sep, val = l.partition('=')
        if sep and name.strip() == key:
            out.append(val)
    return out


# =====================================================================
print('[A] Wandmaker.java')
java = load(JAVA)
code = strip_comments(java)

chk('switch(Dungeon.hero.heroClass)' not in code,
    'A1 已无 switch(Dungeon.hero.heroClass)')
chk('heroClass' not in code, 'A2 全文件不再引用 heroClass')
left = [k for k in OLD_KEYS if '"%s"' % k in code]
chk(not left, 'A3 六个按职业的键名已全部消失（残留：%s）' % (left or '无'))

hero_stmt = re.findall(r'msg1 \+= Messages\.get\(this, "intro_hero", (.*?)\);', code)
chk(len(hero_stmt) == 1, 'A4 intro_hero 取用恰好 1 处（实得 %d）' % len(hero_stmt))
chk(bool(hero_stmt) and 'Messages.titleCase(Dungeon.hero.name())' in hero_stmt[0],
    'A5 取用点仍传入英雄名字（实得：%s）' % (hero_stmt[0] if hero_stmt else '取不到'))
chk('Dungeon.hero.heroClass' not in code,
    'A6 不再按职业分支（heroClass 引用清零）')

i_hero = code.find('"intro_hero"')
i_1 = code.find('"intro_1"')
chk(0 <= i_hero < i_1, 'A7 顺序正确：intro_hero 先于 intro_1（%d < %d）' % (i_hero, i_1))
chk(code.count('"intro_1"') == 1, 'A8 intro_1 取用仍为 1 处')
for k in ('intro_dust', 'intro_ember', 'intro_berry', 'intro_2'):
    chk(code.count('"%s"' % k) == 1, 'A9 %s 取用仍为 1 处' % k)
for k in ('reminder_dust', 'reminder_ember', 'reminder_berry'):
    chk(code.count('"%s"' % k) == 1, 'A10 %s 取用仍为 1 处' % k)

chk('msg1Final' in code and 'msg2Final' in code and 'WndQuest' in code,
    'A11 两段式 WndQuest 流程未被破坏')
chk('Quest.given = true' in code, 'A12 对话后 Quest.given = true 仍在')
chk(java.count('{') == java.count('}'), 'A13 花括号平衡（%d/%d）' % (java.count('{'), java.count('}')))
chk(java.count('(') == java.count(')'), 'A14 圆括号平衡（%d/%d）' % (java.count('('), java.count(')')))

# =====================================================================
def format_ok(val):
    """裸露 % 检测（口径＝ Messages.format → String.format）。

    注意区别（已用 JDK 实测，见 _chk/WandmakerLocCheck.java）：
      · 有多余实参、文本里没有占位符 → String.format 直接忽略，**不报错**；
      · 文本里有非法转换（如「100%的」）→ 抛 IllegalFormatException，
        Messages.format 捕获后 reportException 并**返回原文**（即整串回退）。
    所以这里只判「有无非法转换」，不判实参个数。"""
    return [m.start() for m in re.finditer(r'%(?![bBhHsScCdoxXeEfgGaAnt%])', val)]


def check_props(tag, path, other_val):
    print('[%s] %s' % (tag, path.split('/')[-1]))
    text = load(path)
    val_list = prop_vals(text, PK + 'intro_hero')
    chk(len(val_list) == 1, '%s1 intro_hero 恰好 1 条 active（实得 %d）' % (tag, len(val_list)))
    val = val_list[0] if val_list else ''
    chk(bool(val.strip()), '%s2 intro_hero 取值非空：%r' % (tag, val))
    chk(val.strip() != other_val.strip(), '%s3 与另一语言不雷同（确实是各自文案）' % tag)
    chk('\ufffd' not in val, '%s4 取值无 U+FFFD' % tag)
    chk('/n/' not in val, '%s5 取值无 /n/ 误写' % tag)
    chk(val.count('_') % 2 == 0,
        '%s6 _ 强调标记成对（%d 个）' % (tag, val.count('_')))

    bad = format_ok(val)
    chk(not bad, '%s7 无裸 %% 转换（非法位置：%s；字面百分号须写 %%%%）'
        % (tag, bad or '无'))

    stayed = [k for k in OLD_KEYS if prop_vals(text, PK + k)]
    chk(not stayed, '%s8 六个旧键均已 inactive（残留：%s）' % (tag, stayed or '无'))
    missing = [k for k in KEEP_KEYS if len(prop_vals(text, PK + k)) != 1]
    chk(not missing, '%s9 其余 %d 个 wandmaker 键各 1 条（缺/重复：%s）'
        % (tag, len(KEEP_KEYS), missing or '无'))
    # 段落结构：intro_1 / intro_2 必须以字面 \n\n 开头（保持原样）
    for k in ('intro_1', 'intro_2'):
        v = prop_vals(text, PK + k)[0]
        chk(v.startswith('\\n\\n'), '%s10 %s 仍以字面 \\n\\n 开头' % (tag, k))
    # 结构等价性（**抗后续改动**）：副本里出现过的键名在现文件里必须仍然存在（值可改、键不许整条消失），
    # 且那 6 条按职业旧键必须**彻底移除**（不只是注释掉——副本里它们已是 `#` 注释态，C8 因此是空测）。
    # ⚠️ 不要再写「全局行数恰好 -4」：本文件随后还被别的改动改过（intro_1/intro_2 文案重写、
    #    新增无关键，如 2026-09-22 的 `actors.buffs.riftwindow.*` 两键 + 1 行注释），
    #    任何「相对快照恰好 ±N 行」的判据都会因此**误报**（实测：2356→2355，期望 -4 实得 -1）。
    bak = ROOT + '_chk/_bak_2026-09-22/' + path.split('/')[-1]
    if os.path.exists(bak):
        def key_names(t):
            ks = []
            for l in t.split('\n'):
                if '=' not in l:
                    continue
                k = l.partition('=')[0].strip()
                if k and not k.startswith('#'):
                    ks.append(k)
            return set(ks)
        intended = {PK + k for k in OLD_KEYS}   # 这 6 条的移除是本补丁的目的，由 12 单独把关
        lost = sorted(k for k in key_names(load(bak)) - key_names(text) if k not in intended)
        chk(not lost, '%s11 副本里已有的键没有整条消失（缺失：%s）' % (tag, lost or '无'))
        purged = [k for k in OLD_KEYS if PK + k in key_names(text)]
        chk(not purged, '%s12 六条按职业旧键已从文件中彻底移除（残留：%s）' % (tag, purged or '无'))
    else:
        print('  skip  %s11/12 未找到改动前副本，跳过键集核对' % tag)
    return val


zh_val = check_props('B', ZH, '')
en_val = check_props('C', EN, zh_val)

# =====================================================================
# 反例自测：改动前的 HEAD 版本必须判 FAIL（且打印中间量，确认判据真的跑到了）
print('[D] 反例自测（git HEAD 改动前版本）')
refs = {
    'java': 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/npcs/Wandmaker.java',
    'zh': 'core/src/main/assets/messages/actors/actors_zh.properties',
    'en': 'core/src/main/assets/messages/actors/actors.properties',
}
old = {}
for k, p in refs.items():
    r = subprocess.run(['git', '-C', ROOT, 'show', 'HEAD:' + p],
                       capture_output=True)
    old[k] = r.stdout.decode('utf-8')
    print('  HEAD:%s 取到 %d 字符（stdout %d 字节）' % (k, len(old[k]), len(r.stdout)))

old_code = strip_comments(old['java'])
print('  HEAD java：switch 命中 = %d，intro_hero 命中 = %d'
      % (old_code.count('switch(Dungeon.hero.heroClass)'), old_code.count('"intro_hero"')))
chk(old_code.count('switch(Dungeon.hero.heroClass)') == 1,
    'D1 反例：HEAD 版确有 switch（%d 处）⇒ A1 判据不是空转'
    % old_code.count('switch(Dungeon.hero.heroClass)'))
chk(all('"%s"' % k in old_code for k in OLD_KEYS),
    'D2 反例：HEAD 版六个旧键全在（各计数 %s）'
    % [old_code.count('"%s"' % k) for k in OLD_KEYS])

for tag, txt in (('zh', old['zh']), ('en', old['en'])):
    hit = prop_vals(txt, PK + 'intro_hero')
    n_active_old = len([k for k in OLD_KEYS if prop_vals(txt, PK + k)])
    print('  HEAD %s：intro_hero active = %d，六个旧键 active = %d'
          % (tag, len(hit), n_active_old))
    chk(len(hit) == 0, 'D3 %s 反例：HEAD 版无 intro_hero ⇒ B1/C1 判据不是空转' % tag)
    chk(n_active_old == len(OLD_KEYS),
        'D4 %s 反例：HEAD 版六个旧键全部 active（= %d）⇒ B8/C8 判据不是空转' % (tag, n_active_old))

# =====================================================================
print()
if fails:
    print('FAIL：%d 条' % len(fails))
    for f in fails:
        print('   -', f)
    sys.exit(1)
print('ALL PASS')
