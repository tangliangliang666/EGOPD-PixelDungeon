# -*- coding: utf-8 -*-
"""
修复「考验 CHESED 导致矮人国王二阶段卡死」。

根因（算术，不是随机）：
  DwarfKing 二阶段的王座屏障是**写死绝对量**的一套刻度，三者靠恒等式咬合：
    屏障满值 = HT、每击杀一名仆从削减 HT/12、波次阈值 200 / 100（强化挑战 300 / 150）
    HT=300 ⇒ 4/8/12 次击杀后屏障为 200/100/0，正好触发第二波/第三波/三阶段。
  唯一前提是 **HT 能被 12（强化 18）整除**。
  CHESED 给敌方生命上限 +25%（Trials.CHESED_HP_MULT）⇒ HT=375：
    · 12 次击杀只削掉 12×(375/12)=372 ⇒ 屏障永远剩 3 点，三阶段进不去；
    · 4 次击杀后剩 251 > 200 ⇒ 第二波永远不出（用户报的现象）。
  而二阶段的国王除 KingDamager 外免疫一切伤害 ⇒ 玩家只能干等，整场卡死。

改法：两条刻度改为**从 HT 现算**，并把削减量**向上取整**，使恒等式对任意 HT 成立。
  HT=300/450 时新算式与原写死值逐个相同 ⇒ 原版行为分毫不变。

本脚本是**字节级**读写（本工程源是 CRLF，用 newline='\\n' 会把整文件写成 LF），
**幂等**（已应用则跳过，第二遍全 SKIP），并在末尾断言 CRLF 计数不变。
"""
import io
import os
import sys

P = 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/DwarfKing.java'

# ---------------------------------------------------------------- 插入块（刻度）
ANCHOR_OLD = (
    "\t\tif (phase == 3) BossHealthBar.bleed(true);\r\n"
    "\t}\r\n"
    "\r\n"
    "\t@Override\r\n"
    "\tprotected boolean act() {"
)

HELPERS = (
    "\t//--- 二阶段「王座屏障」的刻度（2026-09-24 修 CHESED 引发的卡死）--------------------\r\n"
    "\t//原版把这套数字写死成**绝对量**，三者靠一个算术恒等式咬合：\r\n"
    "\t//  屏障满值 = HT、每击杀一名仆从削减 HT/12、波次阈值 200 / 100（挑战强化下 300 / 150）：\r\n"
    "\t//  HT=300 ⇒ 第 4 次击杀后剩 200（= 300×2/3，放第二波）、第 8 次后剩 100（= 300×1/3，放第三波）、\r\n"
    "\t//  第 12 次恰好为 0（进三阶段）；强化挑战 HT=450 ⇒ 18 次击杀、阈值 300/150，同理。\r\n"
    "\t//于是整套节奏唯一依赖的前提是「**HT 能被 12（强化 18）整除**」。\r\n"
    "\t//\r\n"
    "\t//考验 CHESED 给敌方生命上限 +25%（Trials.CHESED_HP_MULT：300 → 375）后整除不成立，\r\n"
    "\t//而二阶段的国王除 KingDamager 外**免疫一切伤害**、波次又被绝对阈值卡住 ⇒ 死局：\r\n"
    "\t//  · 12 次击杀只能削掉 12×(375/12) = 372 ⇒ 屏障永远剩 3 点、三阶段永远进不去；\r\n"
    "\t//  · 第一波（4 次击杀）之后屏障剩 251 > 200 ⇒ **第二波永远不出**（用户报的「二阶段卡死不出怪」）。\r\n"
    "\t//\r\n"
    "\t//⇒ 把两条刻度改成**从 HT 现算**，恒等式对任意 HT 都成立：\r\n"
    "\t//  ① 削减量 = ceil(HT / kills) —— 向上取整 ⇒ kills 次击杀必定削掉 ≥ HT\r\n"
    "\t//     （多出的部分由 ShieldBuff.absorbDamage 夹掉）；\r\n"
    "\t//  ② 阈值 = HT×2/3、HT/3 —— 恰好对应「第 kills/3 次、第 2×kills/3 次击杀」。\r\n"
    "\t//HT = 300 / 450 时 ①② 与原写死值**逐个相同**（25 / 200 / 100 / 300 / 150）⇒ 原版行为分毫不变。\r\n"
    "\t//⚠️ 不碰 Random、不新增字段 ⇒ 同 seed 的随机流与改动前逐字节一致。\r\n"
    "\r\n"
    "\t/** 打空二阶段屏障所需的击杀次数（＝原版写死的 12 / 强化挑战 18）。 */\r\n"
    "\tprivate static int barrierKills(){\r\n"
    "\t\treturn Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 18 : 12;\r\n"
    "\t}\r\n"
    "\r\n"
    "\t/**\r\n"
    "\t * 每击杀一名二阶段仆从对屏障的削减量。<b>向上取整</b>：保证 {@link #barrierKills()} 次击杀\r\n"
    "\t * 必定把满值为 {@code HT} 的屏障打空（原版写死 {@code HT/12} 是向下取整，HT 不被整除时凑不到 0）。\r\n"
    "\t */\r\n"
    "\tprivate int barrierChip(){\r\n"
    "\t\tint kills = barrierKills();\r\n"
    "\t\treturn (HT + kills - 1) / kills;\r\n"
    "\t}\r\n"
    "\r\n"
    "\t/**\r\n"
    "\t * 二阶段波次推进阈值：屏障降到满值的 num/3 时放下一波。\r\n"
    "\t * @param num 2 → 满值的 2/3（放第二波）；1 → 满值的 1/3（放第三波）\r\n"
    "\t */\r\n"
    "\tprivate int barrierThreshold( int num ){\r\n"
    "\t\treturn HT * num / 3;\r\n"
    "\t}\r\n"
    "\r\n"
)

ANCHOR_NEW = (
    "\t\tif (phase == 3) BossHealthBar.bleed(true);\r\n"
    "\t}\r\n"
    "\r\n"
    + HELPERS +
    "\t@Override\r\n"
    "\tprotected boolean act() {"
)

# ---------------------------------------------------------------- 逐条替换
REPL = [
    # ① 插入刻度助手
    (ANCHOR_OLD, ANCHOR_NEW, 'INSERT 屏障刻度助手'),
    # ② 强化挑战分支的两个阈值
    ("} else if (shielding() <= 300 && summonsMade < 12){",
     "} else if (shielding() <= barrierThreshold( 2 ) && summonsMade < 12){",
     '阈值 300 → HT×2/3（强化挑战）'),
    ("} else if (shielding() <= 150 && summonsMade < 18) {",
     "} else if (shielding() <= barrierThreshold( 1 ) && summonsMade < 18) {",
     '阈值 150 → HT/3（强化挑战）'),
    # ③ 常规分支的两个阈值
    ("} else if (shielding() <= 200 && summonsMade < 8) {",
     "} else if (shielding() <= barrierThreshold( 2 ) && summonsMade < 8) {",
     '阈值 200 → HT×2/3'),
    ("} else if (shielding() <= 100 && summonsMade < 12) {",
     "} else if (shielding() <= barrierThreshold( 1 ) && summonsMade < 12) {",
     '阈值 100 → HT/3'),
    # ④ KingDamager 的削减量改走唯一出口
    ("\t\t\t\tif (m instanceof DwarfKing){\r\n"
     "\t\t\t\t\tint damage = m.HT / (Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 18 : 12);\r\n"
     "\t\t\t\t\tm.damage(damage, this);\r\n"
     "\t\t\t\t}",
     "\t\t\t\tif (m instanceof DwarfKing){\r\n"
     "\t\t\t\t\tDwarfKing king = (DwarfKing) m;\r\n"
     "\t\t\t\t\tking.damage( king.barrierChip(), this );\r\n"
     "\t\t\t\t}",
     'KingDamager.detach 走 barrierChip()'),
    # ⑤ 占位被挡时的削减量同样走唯一出口
    ("\t\t\t\t\tif (((DwarfKing)target).phase == 2){\r\n"
     "\t\t\t\t\t\tif (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)){\r\n"
     "\t\t\t\t\t\t\ttarget.damage(target.HT/18, new KingDamager());\r\n"
     "\t\t\t\t\t\t} else {\r\n"
     "\t\t\t\t\t\t\ttarget.damage(target.HT/12, new KingDamager());\r\n"
     "\t\t\t\t\t\t}\r\n"
     "\t\t\t\t\t}",
     "\t\t\t\t\tif (((DwarfKing)target).phase == 2){\r\n"
     "\t\t\t\t\t\ttarget.damage(((DwarfKing)target).barrierChip(), new KingDamager());\r\n"
     "\t\t\t\t\t}",
     'Summoning 被挡时的削减量走 barrierChip()'),
]

MARKER = 'private int barrierChip(){'


def main():
    if not os.path.exists(P):
        print('FAIL 找不到 %s' % P)
        return 1

    raw = io.open(P, 'rb').read()
    crlf_before = raw.count(b'\r\n')
    lf_before = raw.count(b'\n')
    text = raw.decode('utf-8')

    if lf_before != crlf_before:
        print('FAIL 改前文件就有 %d 处裸 LF（应为 0）' % (lf_before - crlf_before))
        return 1

    applied = MARKER in text
    if applied:
        print('[SKIP] 已应用过（找到 %s），本脚本不做任何改动' % MARKER)
        # 幂等：仍做一遍「旧写法是否已彻底消失」的自检
        stale = [n for _, _, n in REPL if _find_stale(text, n)]
        if stale:
            print('FAIL 已标记应用，但以下旧写法仍残留：%s' % stale)
            return 1
        print('[OK] 旧写法已彻底退场，文件保持原样')
        return 0

    for old, new, name in REPL:
        c = text.count(old)
        if c != 1:
            print('FAIL 「%s」期望命中 1 次，实际 %d 次' % (name, c))
            return 1
        text = text.replace(old, new)
        print('[OK] %s' % name)

    out = text.encode('utf-8')
    crlf_after = out.count(b'\r\n')
    lf_after = out.count(b'\n')
    # 不变量：① 一处裸 LF 都不许有（整文件必须仍是纯 CRLF）；
    #         ② CRLF 的**增量**必须恰好等于各条替换自身的行数差之和（插入型为 +N，删行为 -N）。
    #            用「逐条求和」而不是写死数字，改动替换清单时不必同步维护期望值。
    delta_lines = sum(nw.count('\r\n') - od.count('\r\n') for od, nw, _ in REPL)
    if lf_after != crlf_after:
        print('FAIL 产生了 %d 处裸 LF' % (lf_after - crlf_after))
        return 1
    if crlf_after != crlf_before + delta_lines:
        print('FAIL CRLF 增量不对：%d → %d（期望 %+d）' % (crlf_before, crlf_after, delta_lines))
        return 1
    if b'\xef\xbf\xbd' in out:
        print('FAIL 写回内容含 U+FFFD')
        return 1

    io.open(P, 'wb').write(out)
    print('[OK] 已写回 %s（CRLF %d 保持不变，字节 %d → %d）' % (P, crlf_after, len(raw), len(out)))

    # 写回后回读一遍：确认旧写法彻底消失
    back = io.open(P, 'rb').read().decode('utf-8')
    stale = [n for _, _, n in REPL if _find_stale(back, n)]
    if stale:
        print('FAIL 回读发现旧写法残留：%s' % stale)
        return 1
    print('[OK] 回读确认：6 处旧写法全部退场')
    return 0


def _find_stale(text, name):
    """name → 该条替换的「旧写法特征串」；用于幂等/回读自检。"""
    table = {
        'INSERT 屏障刻度助手': None,   # 插入型无旧写法（用 MARKER 判）
        '阈值 300 → HT×2/3（强化挑战）': 'shielding() <= 300 &&',
        '阈值 150 → HT/3（强化挑战）': 'shielding() <= 150 &&',
        '阈值 200 → HT×2/3': 'shielding() <= 200 &&',
        '阈值 100 → HT/3': 'shielding() <= 100 &&',
        'KingDamager.detach 走 barrierChip()': 'int damage = m.HT / (Dungeon.isChallenged',
        'Summoning 被挡时的削减量走 barrierChip()': 'target.damage(target.HT/18, new KingDamager());',
    }
    frag = table.get(name)
    if frag is None:
        return False
    return frag in text


if __name__ == '__main__':
    sys.exit(main())
