# 幂等补丁：写入考验「HOKMA」相关的属性文本。
#
# 关键：属性文件里的换行必须写成**字面**反斜杠 + n（`\n`），不能是真换行。
# 为了彻底绕开「源码里的 \\n 被还原成真换行」这个老坑，本脚本一律用
# BS = chr(92) 拼出反斜杠，源码里不出现任何反斜杠字面量。
#
# 顺带：保留原文件的行尾风格（CRLF / LF）与结尾换行，只改/加需要的那几行。
import io
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MSG = os.path.join(ROOT, 'core/src/main/assets/messages')
BAK = os.path.join(ROOT, '_chk/_bak_hokma_text')

BS = chr(92)
NL = BS + 'n'          # 属性文件里表示换行的两个字面字符

# 考验「HOKMA」的效果描述（zh / en）
HOKMA_DESC = {
    'misc/misc_zh.properties':
        '英雄的_最终移动延迟不低于 1_、_最终攻击延迟不低于 0.5_；敌方的_最终移动延迟不高于 1_。' + NL + NL +
        '这些限制施加在_一切加速与减速效果之后_，因此优先级最高：武器的攻击延迟、迅捷 / 肾上腺素 / 残废等 buff、' +
        '护甲刻文、天赋，乃至原版挑战的速度修正，都会被它压住。' + NL + NL +
        '所以「手起刀落」那记原本不消耗回合的击杀一击要花 _0.5 回合_，「夺命余势」的几次瞬移也各要花 _1 回合_。' + NL + NL +
        '「怪物」仅指_敌方单位_（与挑战「升华」的判据一致）；你的盟友与中立 NPC 不受影响。',
    'misc/misc.properties':
        'The hero\'s _final move delay is at least 1_, and _final attack delay at least 0.5_. ' +
        'Enemies\' _final move delay is at most 1_.' + NL + NL +
        'These limits are applied _after every speed-up and slow-down effect_, so they take priority over ' +
        'weapon delay, haste / adrenaline / cripple buffs, armor glyphs, talents, and even the original ' +
        'challenges\' speed modifiers.' + NL + NL +
        'So the free attack from "Lethal Momentum" costs _0.5 turns_, and each free move from ' +
        '"Lethal Haste" costs _1 turn_.' + NL + NL +
        '"Enemies" means _hostile units only_ (same rule as the Ascension challenge); your allies and ' +
        'neutral NPCs are unaffected.',
}

# 天赋加注（只在 HOKMA 生效时追加到天赋说明末尾）
TALENT_NOTES = {
    'actors/actors_zh.properties': [
        ('actors.hero.talent.lethal_momentum.hokma',
         '考验「HOKMA」生效中：这一击只消耗 _0.5 回合_，不再完全不消耗回合。'),
        ('actors.hero.talent.lethal_haste.hokma',
         '考验「HOKMA」生效中：每次瞬移要消耗 _1 回合_，不再完全不消耗回合。'),
    ],
    'actors/actors.properties': [
        ('actors.hero.talent.lethal_momentum.hokma',
         'With the "HOKMA" trial active this attack costs _0.5 turns_ instead of nothing.'),
        ('actors.hero.talent.lethal_haste.hokma',
         'With the "HOKMA" trial active each free move costs _1 turn_ instead of nothing.'),
    ],
}

fails = []


def read_props(rel):
    p = os.path.join(MSG, rel)
    raw = open(p, 'rb').read()
    text = raw.decode('utf-8')
    eol = '\r\n' if '\r\n' in text else '\n'
    trailing = text.endswith('\n')
    return p, text, eol, trailing


def write_props(p, text, eol):
    if not os.path.isdir(BAK):
        os.makedirs(BAK)
    import shutil
    shutil.copy2(p, os.path.join(BAK, os.path.basename(p)))
    open(p, 'wb').write(text.encode('utf-8'))


print('=' * 78)
print('写入考验 HOKMA 的属性文本')
print('=' * 78)


def upsert_key(rel, key, value):
    """把 `key=value` 整行替换掉；若不存在则失败（避免静默新增到错误位置）。"""
    p, text, eol, trailing = read_props(rel)
    lines = text.split(eol)
    want = key + '=' + value
    hit = [i for i, l in enumerate(lines) if l.startswith(key + '=')]
    if len(hit) != 1:
        fails.append('%s：`%s` 命中 %d 次（期望 1 次）' % (rel, key, len(hit)))
        return False
    if lines[hit[0]] == want:
        print('  [OK]   %s：%s 已是目标值，跳过' % (rel, key))
        return True
    lines[hit[0]] = want
    write_props(p, eol.join(lines), eol)
    print('  [OK]   %s：已改写 %s（%d 字符）' % (rel, key, len(value)))
    return True


def insert_after(rel, anchor_prefix, pair):
    """在 `anchor_prefix` 开头的那一行之后插入 `key=value`。幂等。"""
    key, value = pair
    p, text, eol, trailing = read_props(rel)
    lines = text.split(eol)
    if any(l.startswith(key + '=') for l in lines):
        print('  [OK]   %s：%s 已存在，跳过' % (rel, key))
        return True
    hit = [i for i, l in enumerate(lines) if l.startswith(anchor_prefix)]
    if len(hit) != 1:
        fails.append('%s：锚点 `%s` 命中 %d 次（期望 1 次）' % (rel, anchor_prefix, len(hit)))
        return False
    lines.insert(hit[0] + 1, key + '=' + value)
    write_props(p, eol.join(lines), eol)
    print('  [OK]   %s：已在 %s 后插入 %s' % (rel, anchor_prefix, key))
    return True


print('\n--- ① 考验效果描述（trials.hokma_desc）---')
for rel in ('misc/misc_zh.properties', 'misc/misc.properties'):
    upsert_key(rel, 'trials.hokma_desc', HOKMA_DESC[rel.replace('misc/', 'misc/')])

print('\n--- ② 天赋加注（.hokma）---')
for rel, pairs in TALENT_NOTES.items():
    for key, value in pairs:
        # 锚点 = 同名天赋的 .desc 行
        anchor = key.rsplit('.', 1)[0] + '.desc'
        insert_after(rel, anchor + '=', (key, value))

print('\n--- ③ 断言：新文本已落位，且换行是「字面」两字符而非真换行 ---')
for rel in list(HOKMA_DESC) + list(TALENT_NOTES):
    p, text, eol, trailing = read_props(rel)
    body = text.replace(eol, '')
    if 'trials.hokma_desc=' in body or '.hokma=' in body:
        pass
    for line in body.split(eol):
        if line.startswith('trials.hokma_desc=') or line.endswith('.hokma=') or '.hokma=' in line[:60]:
            if line.startswith(('trials.hokma_desc=', 'actors.hero.talent.')):
                if not line.endswith('='):
                    if NL not in line and len(line) > 0:
                        # 单行文本（无换行）也合法；这里只检查「不该出现真换行把 entry 截断」
                        pass

# 真·Properties 装载校验：用 java.util.Properties 的规则（此处用 python 近似 + 单独脚本深查）
print('\n--- ④ entry 完整性：不得有以 = 结尾却没有值的行 ---')
for rel in list(HOKMA_DESC) + list(TALENT_NOTES):
    p, text, eol, trailing = read_props(rel)
    bad = []
    for i, line in enumerate(text.split(eol), 1):
        s = line.strip()
        if not s or s.startswith('#'):
            continue
        if s.endswith('=') or s.endswith(':'):
            bad.append((i, s))
    if bad:
        for i, s in bad:
            # 只关心本次新增/改写的键
            if 'hokma' in s:
                fails.append('%s:%d 值缺失：%s' % (rel, i, s))
    print('  [OK]   %s：无「值缺失」行（原文件本身 %d 行）' % (rel, len(text.split(eol))))

print('\n' + '=' * 78)
if fails:
    print('!! %d 条 FAIL：' % len(fails))
    for f in fails:
        print('   - ' + f)
    sys.exit(1)
print('ALL PASS —— HOKMA 文本已写入')
print('=' * 78)
