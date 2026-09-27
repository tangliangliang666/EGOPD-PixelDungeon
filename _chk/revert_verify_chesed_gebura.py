# -*- coding: utf-8 -*-
"""
回退 _chk/verify_chesed_gebura.py 里 2026-09-26 那一轮「grantNoExp」相关的核验。

四处：
  R1 闸门 return 计数期望 8 -> 7（含注释去 09-26 说明）
  R2 删掉 09-26 新增的 selftest「确认「闸门 return 计数」判据不恒真」
  R3 把「打死不给经验的怪」整段断言换回「CHESED 回血闸门按 mob.EXP <= 0 拦」这一条
  R4 主断言去掉 09-26 附注 + 删掉 grantsNoExp 的让路/顺序检查

每一步都是**精确替换**（count 必为 1），不符即中止，绝不半途落盘。
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TARGET = os.path.join(ROOT, '_chk', 'verify_chesed_gebura.py')
BAK_DIR = os.path.join(ROOT, '_chk', '_bak_revert_noexp')
BAK = os.path.join(BAK_DIR, 'verify_chesed_gebura.py')

R1_OLD = """# GEBURA 闸门的 return 计数期望：1 个 true（真接管）+ N 个 false（每一处 false 都精确对应
# 「原版那句 die() 该照常执行」）。2026-09-26 由 7 增至 8 —— 新增「打死不给经验的怪不吃豁免」
# （Trials.grantsNoExp）那一处提前返回。selftest 与主断言共用这个常量，免得两处漂移。
GATE_EXPECT_FALSE = 8
"""
R1_NEW = """# GEBURA 闸门的 return 计数期望：1 个 true（真接管）+ 7 个 false（每一处 false 都精确对应
# 「原版那句 die() 该照常执行」）。
GATE_EXPECT_FALSE = 7
"""

R2_OLD = """    print('== selftest：确认「闸门 return 计数」判据不恒真 ==')
    def counts(s):
        n_t = len(re.findall(r'return\\s+true\\s*;', s))
        n_f = len(re.findall(r'return\\s+false\\s*;', s))
        return (n_t == 1 and n_f == GATE_EXPECT_FALSE)
    rc2 = [
        ('return true;' + 'return false;' * GATE_EXPECT_FALSE, True, '真形态（1 true / %d false）' % GATE_EXPECT_FALSE),
        ('return true;' + 'return false;' * (GATE_EXPECT_FALSE - 1), False,
         '少一处让路（2026-09-26 之前的旧形态，必须抓出来）'),
        ('return true;return true;' + 'return false;' * GATE_EXPECT_FALSE, False, '多一个接管出口（必须抓出来）'),
    ]
    for body, want, desc in rc2:
        got = counts(body)
        print('  %-44s -> %-5s（期望 %-5s）' % (desc, got, want))
        assert got == want, '判据错：%s 实得 %s 期望 %s' % (desc, got, want)
    print('  selftest 通过：抓得住 return 数量的漂移。\\n')

"""

R2_NEW = ""

R3_OLD = """# --- 「打死不给经验的怪」共用一个判据（2026-09-26）---
# 矿洞任务层的小怪（CrystalWisp / GnollGuard / FungalSpinner）EXP 字段是正的 7，却因 maxLvl=-2
# 从不结算经验 ⇒ 旧判据 EXP<=0 漏判、白吃回血与无敌。判据收进 Trials.grantsNoExp，两条效果共用。
# 完整断言的**反例自测**在 _chk/verify_noexp_trial_exempt.py（29 条判据 + 29 个反例）；
# 这里只钉「本套件关心的两个消费点确实走的是同一个判据」，免得改动在回归里悄悄溜过去。
_mend = trials_body('public static void bindMobPassives( Mob mob ){')
chk(_mend is not None and 'grantsNoExp( mob )' in _mend,
    'CHESED 回血走共用判据 Trials.grantsNoExp（不是裸的 EXP<=0）')
chk(_mend is not None and 'mob.EXP <= 0' not in _mend,
    '回血那条已无裸 mob.EXP <= 0（旧判据会漏过 maxLvl=-2 的矿洞小怪）')

"""
R3_NEW = """# CHESED 回血的闸门：EXP<=0 的敌一律不进回血计时器（中立/盟友/不奖励怪）。
_mend = trials_body('public static void bindMobPassives( Mob mob ){')
chk(_mend is not None and 'mob.EXP <= 0' in _mend, 'CHESED 回血闸门按 mob.EXP <= 0 拦')

"""

R4_OLD = """        '（实得 true=%d / false=%d）—— false 必须精确对应「原版那句 die() 该照常执行」'
        '【2026-09-26 由 7 增至 8：新增「打死不给经验的怪不吃豁免」那一处提前返回】'
        % (GATE_EXPECT_FALSE, n_true, n_false))
    # 新增的那一处让路：判据须早于 geburaUsed 一次性门闩置位（不许先接管再判）
    chk('grantsNoExp( mob )' in geb, 'GEBURA 闸门对「打死不给经验的怪」让路（Trials.grantsNoExp）')
    _i_gr = geb.find('grantsNoExp( mob )')
    _i_used = geb.find('mob.geburaUsed = true;')
    chk(0 <= _i_gr < _i_used, '该让路判据早于 mob.geburaUsed = true 置位')
"""
R4_NEW = """        '（实得 true=%d / false=%d）—— false 必须精确对应「原版那句 die() 该照常执行」'
        % (GATE_EXPECT_FALSE, n_true, n_false))
"""


def main():
    assert os.path.isfile(TARGET), '找不到 %s' % TARGET
    raw = open(TARGET, 'r', encoding='utf-8', newline='').read()
    assert '\r' not in raw, '文件含 CR，本脚本只处理纯 LF'

    out = raw
    for tag, old, new in [('R1', R1_OLD, R1_NEW), ('R2', R2_OLD, R2_NEW),
                          ('R3', R3_OLD, R3_NEW), ('R4', R4_OLD, R4_NEW)]:
        n = out.count(old)
        if n != 1:
            print('[%s] 期望命中 1 次，实得 %d 次；**未落盘**' % (tag, n))
            # 帮助定位
            key = old.strip().split('\n')[0][:60]
            print('  首行锚点: %r  在文中出现 %d 次' % (key, out.count(key)))
            sys.exit(2)
        out = out.replace(old, new, 1)

    # 落盘前断言：09-26 的痕迹全无
    for kw in ['grantsNoExp', 'maxLvl', '2026-09-26', '无经验', 'rc2']:
        assert kw not in out, '仍残留 %s' % kw
    assert 'GATE_EXPECT_FALSE = 7' in out
    assert out.count('GATE_EXPECT_FALSE') == 3, 'GATE_EXPECT_FALSE 引用数异常：%d' % out.count('GATE_EXPECT_FALSE')
    assert 'mob.EXP <= 0' in out

    os.makedirs(BAK_DIR, exist_ok=True)
    open(BAK, 'w', encoding='utf-8', newline='').write(raw)
    open(TARGET, 'w', encoding='utf-8', newline='').write(out)
    nb = open(TARGET, 'rb').read()
    print('OK 已回退 verify_chesed_gebura.py：%d 行 -> %d 行，CRLF=%d'
          % (len(raw.split('\n')), len(out.split('\n')), nb.count(b'\r\n')))
    print('改前副本：%s' % BAK)


if __name__ == '__main__':
    main()
