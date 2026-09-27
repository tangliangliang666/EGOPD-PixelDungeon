# -*- coding: utf-8 -*-
"""
回退「矿洞任务层小怪不吃 CHESED 回血 / GEBURA 豁免」这一轮改动（2026-09-26 傍晚）。

口径：用户实测效果不理想，要求**完全回退**到「仍然只用字段上的 EXP 判无敌与回血」。
本脚本只撤销 2026-09-26 那一轮加进 Trials.java 的东西，**不碰**其它无关改动
（Ring/Lucky 的 import、ICEB_W、gebura 掉落凭据抄写等）。

做法：按**当前文件行号**从高到低做删除/替换（降序 ⇒ 前面的行号不会被后面的编辑推偏），
每一步都先断言该行的锚点文本符合预期，不符即中止。改前副本留在 _chk/_bak_revert_noexp/。
行尾：本文件是纯 LF，写入时保持 LF。
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TARGET = os.path.join(ROOT, 'core', 'src', 'main', 'java', 'com', 'shatteredpixel',
                      'shatteredpixeldungeon', 'Trials.java')
BAK_DIR = os.path.join(ROOT, '_chk', '_bak_revert_noexp')
BAK = os.path.join(BAK_DIR, 'Trials.java')

# ---- 原始文本（逐字取自 _chk/_bak_toggle_fix/Trials.java.bak）----
ORIG_MEND_DOC = [
    "\t * <p>⚠️ EXP=0 的敌一律不进这里（2026-09-24）：这类怪物打死了也不给经验，回血只会拉长\n",
    "\t * 战斗、把「跑尸」的负反馈放大。生命上限 +25% 仍由 {@link #modifyMobHT} 单独处理，\n",
    "\t * 那是另一条数值线。</p>\n",
]
ORIG_MEND_GUARD = "\t\tif (mob.EXP <= 0) return;        //EXP=0 的敌（中立/盟友/不奖励怪）一律不进回血计时器\n"


def main():
    assert os.path.isfile(TARGET), '找不到 %s' % TARGET
    raw = open(TARGET, 'r', encoding='utf-8', newline='').read()
    assert '\r' not in raw, '文件含 CR，本脚本只处理纯 LF'
    lines = raw.split('\n')          # 尾元素是最后一个 \n 之后的空串
    # lines[i] 即第 i+1 行（不含换行符）

    def L(no):                        # 1-based
        return lines[no - 1]

    # ---------- 0) 先做锚点自检（全部通过后才动手）----------
    checks = [
        (273, '共用判据：这只怪「打死也不给经验」吗'),
        (295, 'public static boolean grantsNoExp( Mob mob )'),
        (298, '\t}'),
        (299, ''),
        (369, '「打死不给经验」的敌一律不进这里（2026-09-24 起'),
        (374, '单独处理，那是另一条数值线。</p>'),
        (378, 'if (grantsNoExp( mob )) return;  //打死不给经验的敌'),
        (423, '作用对象再收一道：打死不给经验的怪不吃豁免'),
        (426, '与 CHESED 回血那条口径**完全一致**'),
        (451, '「打死不给经验」的怪不吃本豁免**（2026-09-26'),
        (456, '\t *'),
        (457, '\t * @return 是否**接管**了这次死亡'),
        (469, 'if (grantsNoExp( mob )) return false;'),
        (470, 'if (mob.geburaUsed) return false;'),
    ]
    bad = [(no, L(no), anchor) for no, anchor in checks if anchor not in L(no)]
    if bad:
        for no, got, anchor in bad:
            print('锚点不符：第 %d 行\n  期望含: %r\n  实际  : %r' % (no, anchor, got))
        sys.exit(2)

    # ---------- 1) 降序执行 ----------
    # 1) 删第 469 行（GEBURA 闸门的 grantsNoExp 让路）
    del lines[469 - 1]

    # 2) 删第 451..456 行（interceptLethalDamage 的 javadoc 新增段 + 其后空 *）
    assert L(451).startswith('\t * <p>⚠️ **「打死不给经验」的怪不吃本豁免**')
    del lines[451 - 1:456]

    # 3) 删第 423..426 行（GEBURA 段落注释里的新增块）
    del lines[423 - 1:426]

    # 4) 第 378 行 → 恢复 `if (mob.EXP <= 0) return;`（必须先于 369 那一步：降序）
    assert L(378).lstrip().startswith('if (grantsNoExp( mob )) return;')
    lines[378 - 1] = ORIG_MEND_GUARD[:-1]

    # 5) 第 369..374 行 → 恢复原始 3 行 javadoc
    assert L(369).startswith('\t * <p>⚠️ 「打死不给经验」的敌一律不进这里')
    lines[369 - 1:374] = [s[:-1] if s.endswith('\n') else s for s in ORIG_MEND_DOC]

    # 6) 删第 273..299 行（共用判据注释块 + grantsNoExp 方法 + 其后空行）
    del lines[273 - 1:299]

    out = '\n'.join(lines)

    # ---------- 2) 落盘前断言 ----------
    assert 'grantsNoExp' not in out, '回退不干净：仍存在 grantsNoExp'
    assert 'maxLvl' not in out, '回退不干净：仍存在 maxLvl'
    assert 'if (mob.EXP <= 0) return;' in out, '未恢复 EXP<=0 回血闸门'
    assert out.count('if (mob.EXP <= 0) return;') == 1
    # 闸门 return 计数回到 1 true / 7 false
    import re
    start = out.index('public static boolean interceptLethalDamage( Char ch, Object src ){')
    end = out.index('\n\t}', start)
    gate = out[start:end]
    n_t = len(re.findall(r'return\s+true\s*;', gate))
    n_f = len(re.findall(r'return\s+false\s*;', gate))
    assert (n_t, n_f) == (1, 7), '闸门 return 计数应回到 1 true / 7 false，实得 %d/%d' % (n_t, n_f)
    assert '2026-09-26' not in out, '仍残留 09-26 的痕迹'
    # 无关改动保持不动（这些不是本轮回退对象）
    for must in ['import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;',
                 'import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth;',
                 'import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky;',
                 'geburaLuckyProc', 'geburaWealthBonus', 'public static final int[] ICON_W']:
        assert must in out, '误删了无关内容：%s' % must

    os.makedirs(BAK_DIR, exist_ok=True)
    open(BAK, 'w', encoding='utf-8', newline='').write(raw)
    open(TARGET, 'w', encoding='utf-8', newline='').write(out)

    nb = open(TARGET, 'rb').read()
    print('OK 已回退 Trials.java：%d 行 -> %d 行，字节 %d，裸LF=%d，CRLF=%d'
          % (len(raw.split('\n')), len(out.split('\n')), len(nb),
             nb.count(b'\n') - nb.count(b'\r\n'), nb.count(b'\r\n')))
    print('改前副本：%s' % BAK)


if __name__ == '__main__':
    main()
