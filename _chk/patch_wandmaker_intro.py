# -*- coding: utf-8 -*-
"""老杖匠开场白：按职业六选一 → 不分职业一句（占位待填）。

① Wandmaker.java：删掉 switch(Dungeon.hero.heroClass){...}（含 ORACLE/VALENCINA/MIDDLE_FINGER 三处复用），
   改为一条 Messages.get(this, "intro_hero", Messages.titleCase(Dungeon.hero.name()))。
② actors_zh.properties：六条按职业的台词（当前已整块被 # 注释掉）替换为一条 active 的 intro_hero 占位。
③ actors.properties（基包/en）：同样把六条 active 的 intro_* 换成一条 intro_hero 占位。

三个文件换行符各自保持原样（Java=CRLF、properties=LF），落盘前逐项断言，不满足即中止。
"""
import os
import shutil
import sys

ROOT = 'D:/PD'
BAK = os.path.join(ROOT, '_chk', '_bak_2026-09-22')

JAVA = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/npcs/Wandmaker.java')
ZH = os.path.join(ROOT, 'core/src/main/assets/messages/actors/actors_zh.properties')
EN = os.path.join(ROOT, 'core/src/main/assets/messages/actors/actors.properties')

OLD_KEYS = ['intro_warrior', 'intro_rogue', 'intro_mage',
            'intro_huntress', 'intro_duelist', 'intro_cleric']

ZH_PLACEHOLDER = '（占位）请在此填写老杖匠的开场白。'
EN_PLACEHOLDER = '(PLACEHOLDER) Write the old wandmaker\'s opening line here.'

fails = []


def read(path):
    with open(path, 'rb') as f:
        raw = f.read()
    crlf = b'\r\n' in raw
    nl = '\r\n' if crlf else '\n'
    return raw.decode('utf-8').replace('\r\n', '\n'), crlf, nl


def write(path, text, crlf):
    out = text.replace('\n', '\r\n') if crlf else text
    with open(path, 'wb') as f:
        f.write(out.encode('utf-8'))


def backup(path):
    os.makedirs(BAK, exist_ok=True)
    dst = os.path.join(BAK, os.path.basename(path))
    if not os.path.exists(dst):          # 只留第一次（未改动）的原始副本
        shutil.copy2(path, dst)
    return dst


# =====================================================================
# ① Wandmaker.java
# =====================================================================
print('[1] Wandmaker.java')
src, crlf, nl = read(JAVA)
backup(JAVA)

anchor_head = 'switch(Dungeon.hero.heroClass){'
anchor_tail = '\t\t\tmsg1 += Messages.get(this, "intro_1");'

if src.count(anchor_head) == 0 and src.count('"intro_hero"') == 1:
    print('  已应用过，跳过（幂等）')
else:
    assert src.count(anchor_head) == 1, 'switch 锚点命中 %d 次' % src.count(anchor_head)
    assert src.count(anchor_tail) == 1, 'intro_1 锚点命中 %d 次' % src.count(anchor_tail)

    i0 = src.index(anchor_head)
    # 从 switch 行退回它所在行首
    i0 = src.rindex('\n', 0, i0) + 1
    i1 = src.index(anchor_tail)
    i1 = src.rindex('\n', 0, i1) + 1      # 保留 intro_1 那一行
    removed = src[i0:i1]

    for k in OLD_KEYS:
        assert removed.count('"' + k + '"') >= 1, '待删块里没有 %s' % k
    assert removed.count('暂时复用战士台词') == 3, '三处「暂时复用战士台词」注释不在预期位置'
    for cls in ('WARRIOR', 'ROGUE', 'MAGE', 'HUNTRESS', 'DUELIST', 'CLERIC',
                'ORACLE', 'VALENCINA', 'MIDDLE_FINGER'):
        assert 'case %s:' % cls in removed, '待删块里缺 case %s' % cls
    print('  待删块 %d 行，覆盖 9 个职业 case' % (removed.count('\n'),))

    new_block = (
        '\t\t\t//开场白不再按职业区分，所有角色共用同一句（文本键 intro_hero）\n'
        '\t\t\t//若文本里写了 %s，会替换为英雄名字；不写也不会报错\n'
        '\t\t\tmsg1 += Messages.get(this, "intro_hero", Messages.titleCase(Dungeon.hero.name()));\n'
    )
    src2 = src[:i0] + new_block + src[i1:]

    assert 'Dungeon.hero.heroClass' not in src2, '文件里仍残留 heroClass 引用'
    for k in OLD_KEYS:
        assert '"' + k + '"' not in src2, '改动后仍残留 %s' % k
    assert src2.count('"intro_hero"') == 1
    assert 'Messages.titleCase(Dungeon.hero.name())' in src2
    assert src2.count('"intro_1"') == 1
    for k in ('intro_dust', 'intro_ember', 'intro_berry', 'intro_2',
              'reminder_dust', 'reminder_ember', 'reminder_berry'):
        assert src2.count('"' + k + '"') == 1, '%s 取用点被误动' % k
    # 删掉的 switch 本身带一对花括号 ⇒ 只允许各减 1，且改完全文仍然平衡
    assert src2.count('{') == src.count('{') - 1, '左花括号减少量 != 1'
    assert src2.count('}') == src.count('}') - 1, '右花括号减少量 != 1'
    assert src2.count('{') == src2.count('}'), '改完花括号不平衡'
    write(JAVA, src2, crlf)
    print('  OK 已改写（%s，现 %d 字符）' % ('CRLF' if crlf else 'LF', len(src2)))


# =====================================================================
# ② actors_zh.properties
# =====================================================================
def patch_props(path, tag, placeholder, comment):
    print('[%s] %s' % (tag, path.split('/')[-1]))
    text, crlf, nl = read(path)
    backup(path)
    lines = text.split('\n')

    # 定位六条旧键所在的整块（可被 # 注释，也可能是 active）
    idx = []
    for i, l in enumerate(lines):
        body = l[1:].lstrip() if l.startswith('#') else l
        for k in OLD_KEYS:
            if body.startswith('actors.mobs.npcs.wandmaker.' + k + '='):
                idx.append(i)
    assert len(idx) in (0, len(OLD_KEYS)), '找到 %d 条旧键（期望 0 或 6）' % len(idx)
    if not idx:
        active = [l for l in lines if l.startswith('actors.mobs.npcs.wandmaker.intro_hero=')]
        assert len(active) == 1, '无旧键但 intro_hero active 行数 = %d' % len(active)
        print('  已应用过，跳过（幂等）；现值 = %r' % active[0].split('=', 1)[1])
        return active[0].split('=', 1)[1]
    assert idx == list(range(idx[0], idx[0] + len(idx))), '六条旧键不连续：%s' % idx
    first, last = idx[0], idx[-1]
    print('  旧键位于第 %d-%d 行' % (first + 1, last + 1))

    key_line = 'actors.mobs.npcs.wandmaker.intro_hero=' + placeholder
    assert lines[first - 1].startswith('actors.mobs.npcs.wandmaker.name='), \
        '替换点上一行不是 name 键：' + lines[first - 1][:60]
    assert lines[last + 1].startswith('actors.mobs.npcs.wandmaker.intro_1='), \
        '替换点下一行不是 intro_1 键：' + lines[last + 1][:60]

    lines[first:last + 1] = [comment, key_line]
    text2 = '\n'.join(lines)

    # —— 落盘前断言 ——
    body2 = [l for l in text2.split('\n')
             if not l.startswith('#')]
    for k in OLD_KEYS:
        assert not any(l.startswith('actors.mobs.npcs.wandmaker.' + k + '=')
                       for l in body2), '改动后 %s 仍 active' % k
    hits = [l for l in body2 if l.startswith('actors.mobs.npcs.wandmaker.intro_hero=')]
    assert len(hits) == 1, 'intro_hero active 行数 = %d' % len(hits)
    val = hits[0].split('=', 1)[1]
    assert val.strip(), 'intro_hero 值为空'
    assert '\ufffd' not in text2, '出现 U+FFFD'
    assert text2.count('{') == text.count('{'), '花括号数量变化'
    write(path, text2, crlf)
    print('  OK 已改写（%s，现 %d 字符），intro_hero 值 = %r' % (
        'CRLF' if crlf else 'LF', len(text2), val))
    return val


zh_val = patch_props(
    ZH, '2', ZH_PLACEHOLDER,
    '# 老杖匠开场白（所有职业共用一句；%s = 英雄名字，字面百分号要写 %% ）')
en_val = patch_props(
    EN, '3', EN_PLACEHOLDER,
    '# old wandmaker opening line (shared by all classes; %s = hero name, literal percent needs %% )')


# =====================================================================
# ④ 双份核对
# =====================================================================
print('[4] 双份核对')
assert zh_val.strip() != en_val.strip(), 'zh/en 内容雷同（应各自是占位文案）'
print('  ok   zh/en 各有一条 active 的 intro_hero 占位')

print()
if fails:
    print('FAIL')
    sys.exit(1)
print('ALL PASS   （改动前副本：%s）' % BAK)
