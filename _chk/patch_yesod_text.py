# -*- coding: utf-8 -*-
"""
YESOD（根基）与 NETZACH 阈值改动的文本补丁（zh + en）。

改动内容：
  1) trials.netzach_desc      淡出阈值 2 格 / 4 格  ->  1 格 / 3 格（行内最小替换）
  2) trials.yesod_desc        占位  ->  真实描述
  3) 新增 trials.yesod_unknown  地面未知物品的检视描述（用户逐字给定）
  4) 新增 trials.id_blocked     「自然鉴定被禁」的反馈（对应 ShardOfOblivion.identify_ready 的位置）

幂等：二次运行会断言「已是目标值」并跳过。
断言：① 目标键恰好出现 1 次；② 已存在的键值必须等于期望值（否则报冲突）；
      ③ 文件行尾结构不变（CRLF 计数不变、无裸 LF）；④ 描述里的换行是**字面** \n 两个字符。
备份：_chk/_bak_yesodtext/
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BAK  = os.path.join(ROOT, '_chk', '_bak_yesodtext')

FILES = [
    ('core/src/main/assets/messages/misc/misc.properties',    'en'),
    ('core/src/main/assets/messages/misc/misc_zh.properties', 'zh'),
]

# ---- 1) netzach_desc 的行内替换（旧 -> 新），每个文件各一组 -------------------
NETZACH_SUBS = {
    'en': [('_more than 2 tiles_', '_more than 1 tile_'),
           ('_more than 4 tiles_', '_more than 3 tiles_')],
    'zh': [('_距离英雄超过 2 格_', '_距离英雄超过 1 格_'),
           ('_超过 4 格_',         '_超过 3 格_')],
}

# ---- 2) 新值（字面 \n = 两个字符）------------------------------------------
YESOD_DESC = {
    'en': ('trials.yesod_desc='
           'Items _cannot be identified naturally_: the automatic identification of '
           '_weapons, armor, rings and wands_ as they are used, and the identification '
           'that comes with _decomposing scrolls and potions into alchemical energy_, '
           '_no longer happen_. _Scrolls of identify_, _scrolls of divination_ and '
           '_stones of intuition_ are unaffected.\\n\\n'
           'The shop _sells one extra scroll of identify_.\\n\\n'
           '_Items on the ground_ are always shown as _unknown_: just a "?" icon, and '
           'examining one tells you nothing but "You don\'t know what this is.". '
           '_Containers (chests, skull piles and the like)_ and the _inventory_ are unaffected.'),
    'zh': ('trials.yesod_desc='
           '物品_无法自然鉴定_：武器、护甲、戒指、法杖在_使用过程中_的自动鉴定，以及'
           '卷轴与药水_分解为炼金能量_时附带的那次鉴定，都_不会发生_。'
           '_鉴定卷轴_、_占卜卷轴_与_感知符石_的鉴定不受影响。\\n\\n'
           '商店中_额外出售一张鉴定卷轴_。\\n\\n'
           '_地面上的物品_一律显示为_未知_：只有「？」贴图，检视时也只会告诉你'
           '「你不知道这里是什么。」。_宝箱、骷髅堆等容器_与_背包内_的显示不受影响。'),
}

YESOD_UNKNOWN = {
    'en': 'trials.yesod_unknown=You don\'t know what this is.',
    'zh': 'trials.yesod_unknown=你不知道这里是什么。',
}

ID_BLOCKED = {
    'en': ('trials.id_blocked=Natural identification is disabled by the _YESOD_ trial: '
           '%s can only be identified with a scroll of identify, a scroll of divination, '
           'or a stone of intuition.'),
    'zh': ('trials.id_blocked=自然鉴定已被_根基考验_禁用：%s 只能借助鉴定卷轴、占卜卷轴'
           '或感知符石来鉴定。'),
}


def find_line(lines, key):
    """返回 (行号, 行内容)；找不到返回 (-1, None)。key 形如 'trials.yesod_desc'。"""
    prefix = key + '='
    hits = [(i, ln) for i, ln in enumerate(lines) if ln.startswith(prefix)]
    if len(hits) > 1:
        raise AssertionError('键 %s 出现 %d 次（应为 0 或 1）' % (key, len(hits)))
    return hits[0] if hits else (-1, None)


def patch(path, lang):
    full = os.path.join(ROOT, path)
    raw = open(full, 'rb').read()
    old_crlf = raw.count(b'\r\n')
    old_bare = raw.count(b'\n') - old_crlf
    assert old_bare == 0, '%s 已有裸 LF，拒绝继续' % path
    text = raw.decode('utf-8')
    assert not text.startswith('\ufeff'), '%s 有 BOM' % path
    lines = text.split('\r\n')

    # ---- 1) netzach 行内替换 ----
    idx, ln = find_line(lines, 'trials.netzach_desc')
    assert idx >= 0, '找不到 trials.netzach_desc'
    for old, new in NETZACH_SUBS[lang]:
        n = ln.count(old)
        if n == 0 and new in ln:
            continue                      # 幂等：已经是新值
        assert n == 1, 'netzach 替换锚点 %r 在行内出现 %d 次' % (old, n)
        ln = ln.replace(old, new)
    lines[idx] = ln

    # ---- 2) yesod_desc ----
    want = YESOD_DESC[lang]
    idx2, ln2 = find_line(lines, 'trials.yesod_desc')
    assert idx2 >= 0, '找不到 trials.yesod_desc'
    if ln2 == want:
        pass
    else:
        assert ln2.startswith('trials.yesod_desc=（占位）') or ln2.startswith('trials.yesod_desc=(Placeholder)'), \
            'trials.yesod_desc 既不是占位也不是目标值，拒绝覆盖：%r' % ln2[:60]
        lines[idx2] = want

    # ---- 3) + 4) 新增两条（插在 yesod_desc 之后）----
    added = 0
    cursor = idx2 + 1
    for key, value in (('trials.yesod_unknown', YESOD_UNKNOWN[lang]),
                       ('trials.id_blocked',    ID_BLOCKED[lang])):
        j, cur = find_line(lines, key)
        if j >= 0:
            assert cur == value, '%s 已存在但值不同：%r' % (key, cur[:60])
            continue
        lines.insert(cursor, value)
        cursor += 1
        added += 1

    out = '\r\n'.join(lines).encode('utf-8')
    new_crlf = out.count(b'\r\n')
    new_bare = out.count(b'\n') - new_crlf
    assert new_bare == 0, '产生了裸 LF'
    assert new_crlf == old_crlf + added, \
        'CRLF 增量应为新增行数 %d，实测 %d' % (added, new_crlf - old_crlf)
    # 描述里的换行必须是**字面** \n（两个字符），不能是真换行
    assert '\\n\\n' in YESOD_DESC[lang]

    if out == raw:
        print('  [skip] %s 已是目标状态' % path)
        return
    os.makedirs(BAK, exist_ok=True)
    bak = os.path.join(BAK, os.path.basename(path) + '.bak')
    if not os.path.exists(bak):
        open(bak, 'wb').write(raw)
    open(full, 'wb').write(out)
    print('  [ok]   %-58s CRLF %d -> %d（新增 %d 行）' % (path, old_crlf, new_crlf, added))


def main():
    print('== patch_yesod_text ==')
    for path, lang in FILES:
        patch(path, lang)
    print('== done ==')
    return 0


if __name__ == '__main__':
    sys.exit(main())
