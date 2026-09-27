# 核验「趣味挑战」三枚图标：Icons.java 里的 rect 是否与 icons.png 上真实绘制的像素对齐、是否与别的图标格冲突
#
# 用户口径（2026-09-24）：灰化按钮 (128,48) / 点亮按钮 (144,48) / 游戏内右上角计数 (160,48)。
#
# 断言链（任一失败即 [FAIL] 且退出码 1）：
#   ① Icons.java 里 FUN_GREY / FUN_COLOR / FUN_COUNT 三处 rect 等于预期值；
#   ② icons.png 尺寸 256×128、能被 16 整除；
#   ③ 三个矩形内**非空**（图真的画了）、且内容**没有越出**矩形；
#   ④ 灰/彩两枚的内容包围盒**恰好等于**整格（满格），计数那枚落在格内；
#   ⑤ 三个矩形与**其它任何** case 的矩形都**不相交**；
#   ⑥ 计数那枚不与 CHAL_COUNT / TRIAL_COUNT 重叠（三者在 MenuPane 里是横排的同一组计数）；
#   ⑦ **三枚计数图标同规格守卫**：FUN_COUNT ≡ CHAL_COUNT ≡ TRIAL_COUNT
#      （同为 7×7、且字形相对偏移一致 —— 尺寸差 1px 就会让数字基线/图标间距错开，见
#        skill egopd-source-verify §6 与 _chk/verify_trial_icons.py 的同类守卫）
#
# --selftest：喂假数据，确认「重叠判定」与「同规格守卫」既不空转也不过敏感。
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)
from verify_sprite_frames import decode_png

ICONS_JAVA = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/ui/Icons.java')
ICONS_PNG = os.path.join(ROOT, 'core/src/main/assets/interfaces/icons.png')

EXPECT = {
    'FUN_GREY':  (128, 48, 14, 16),
    'FUN_COLOR': (144, 48, 14, 16),
    'FUN_COUNT': (160, 48, 7, 7),
}
# 灰/彩两枚是「满格」画法（不透明包围盒 == 声明矩形）；计数那枚字形小于格子，不能满格断言
FULL_BLEED = {'FUN_GREY', 'FUN_COLOR'}
# 计数图标必须与之同规格的邻居（MenuPane 横排计数：挑战 / 考验 / 趣味）
COUNT_PEERS = ('CHAL_COUNT', 'TRIAL_COUNT')

ok = True


def chk(cond, msg):
    global ok
    print(('  [OK]   ' if cond else '  [FAIL] ') + msg)
    if not cond:
        ok = False


def parse_rects(text):
    out = {}
    cur = None
    for line in text.splitlines():
        m = re.match(r'\s*case\s+([A-Za-z_]\w*)\s*:', line)
        if m:
            cur = m.group(1)
            continue
        m = re.search(r'uvRectBySize\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)', line)
        if m and cur is not None:
            out[cur] = tuple(int(v) for v in m.groups())
            cur = None
    return out


def overlap(a, b):
    ax, ay, aw, ah = a
    bx, by, bw, bh = b
    return ax < bx + bw and bx < ax + aw and ay < by + bh and by < ay + ah


def rel_glyph(rect, bb):
    """字形相对所在矩形的偏移与尺寸（与矩形绝对位置无关，可直接跨图标比较）"""
    return (bb[0] - rect[0], bb[1] - rect[1], bb[2], bb[3])


def same_spec(a_rect, a_bb, b_rect, b_bb):
    return a_rect[2:] == b_rect[2:] and rel_glyph(a_rect, a_bb) == rel_glyph(b_rect, b_bb)


def selftest():
    print('== selftest：确认「重叠判定」能抓到冲突 ==')
    r = {'A': (16, 16, 16, 16), 'B': (24, 24, 8, 8)}
    bad = [k for k, v in r.items() if k != 'A' and overlap(v, r['A'])]
    print('  假数据 A(16,16,16,16) 与 B(24,24,8,8) -> 检出的冲突方：%s' % bad)
    assert bad == ['B'], '重叠判定失效：%s' % bad
    r2 = {'A': (16, 16, 16, 16), 'B': (32, 32, 8, 8)}
    bad2 = [k for k, v in r2.items() if k != 'A' and overlap(v, r2['A'])]
    print('  假数据 A(16,16,16,16) 与 B(32,32,8,8) [仅贴角] -> 检出的冲突方：%s（应为空）' % bad2)
    assert bad2 == [], '重叠判定过敏：贴边被误判为重叠 %s' % bad2

    print('== selftest：确认「同规格守卫」抓得住尺寸错配与字形错位 ==')
    ch_r, ch_b = (160, 80, 7, 7), (161, 81, 5, 6)          # 挑战：7×7，字形 5×6 @ +1,+1
    ok_r, ok_b = (160, 48, 7, 7), (161, 49, 5, 6)          # 趣味：同规格 -> 应判同规格
    print('  7×7 / 7×7 同字形相对位置 -> same_spec=%s' % same_spec(ch_r, ch_b, ok_r, ok_b))
    assert same_spec(ch_r, ch_b, ok_r, ok_b), '守卫过敏：同规格被误判为不同规格'
    bug_r, bug_b = (160, 48, 8, 8), (161, 49, 5, 6)        # 复现历史 bug 形态：8×8
    print('  7×7 / 8×8（历史 bug 形态）  -> same_spec=%s' % same_spec(ch_r, ch_b, bug_r, bug_b))
    assert not same_spec(ch_r, ch_b, bug_r, bug_b), '守卫失效：8×8 被误判为同规格'
    off_r, off_b = (160, 48, 7, 7), (162, 49, 5, 6)        # 尺寸同、字形画歪
    print('  7×7 / 7×7 但字形偏移 +2   -> same_spec=%s' % same_spec(ch_r, ch_b, off_r, off_b))
    assert not same_spec(ch_r, ch_b, off_r, off_b), '守卫失效：字形错位未被检出'
    print('  selftest 通过：重叠判定既不空转也不过敏；同规格守卫抓得住尺寸错配与字形错位。\n')


print('=' * 74)
print('核验：趣味挑战三枚图标（Icons.java rect ↔ icons.png 实际像素）')
print('=' * 74)

if '--selftest' in sys.argv:
    selftest()

java = open(ICONS_JAVA, encoding='utf-8').read()
rects = parse_rects(java)
print('\n从 Icons.java 解出 %d 个「纯数字」rect（带表达式如 DEPTH 的会自动跳过）' % len(rects))
for name in ('FUN_GREY', 'FUN_COLOR', 'FUN_COUNT'):
    print('  %-12s %s' % (name, rects.get(name)))

print('\n【① 三处 rect 等于预期值】')
for name, want in EXPECT.items():
    chk(rects.get(name) == want, '%s 期望 %s，实得 %s' % (name, want, rects.get(name)))

print('\n【② icons.png 尺寸与网格】')
w, h, ct, nch, plte, trns, px = decode_png(ICONS_PNG)
print('  尺寸 %d×%d  colorType=%d  通道=%d' % (w, h, ct, nch))
chk((w, h) == (256, 128), '尺寸应为 256×128，实得 %d×%d' % (w, h))
chk(w % 16 == 0 and h % 16 == 0, '宽高应被 16 整除（否则 rect 不落在像元网格上）')


def alpha(x, y):
    if x < 0 or y < 0 or x >= w or y >= h:
        return 0
    return px[(y * w + x) * nch + (nch - 1)]


def bbox(x, y, rw, rh):
    minx = miny = 10 ** 9
    maxx = maxy = -1
    cnt = 0
    for yy in range(y, y + rh):
        for xx in range(x, x + rw):
            if alpha(xx, yy) > 0:
                cnt += 1
                minx = min(minx, xx); maxx = max(maxx, xx)
                miny = min(miny, yy); maxy = max(maxy, yy)
    if cnt == 0:
        return None
    return minx, miny, maxx - minx + 1, maxy - miny + 1, cnt


print('\n【③④ 图内容：非空、不越界、灰彩是否满格】')
boxes = {}
for name, (x, y, rw, rh) in EXPECT.items():
    b = bbox(x, y, rw, rh)
    boxes[name] = b
    chk(b is not None, '%s (%d,%d,%d,%d) 区域内**有像素**' % ((name,) + (x, y, rw, rh)))
    if b is None:
        continue
    print('        实测包围盒 x=%d y=%d w=%d h=%d  非透明=%d' % b)
    chk(b[0] >= x and b[1] >= y and b[0] + b[2] <= x + rw and b[1] + b[3] <= y + rh,
        '%s 内容完全落在声明矩形内' % name)
    if name in FULL_BLEED:
        chk((b[0], b[1], b[2], b[3]) == (x, y, rw, rh),
            '%s 应满格（包围盒 == 矩形 %d,%d,%d,%d），实得 %d,%d,%d,%d'
            % ((name, x, y, rw, rh) + b[:4]))
    else:
        chk(b[2] <= rw and b[3] <= rh, '%s 内容尺寸不超过格子' % name)

print('\n【⑤ 与其它 case 的矩形是否相交】')
conflicts = []
for tname, trect in EXPECT.items():
    for oname, orect in rects.items():
        if oname in EXPECT or orect == trect:
            continue
        if overlap(trect, orect):
            conflicts.append((tname, oname, trect, orect))
if conflicts:
    for t, o, tr, orc in conflicts:
        chk(False, '%s %s 与 %s %s **重叠**' % (t, tr, o, orc))
else:
    chk(True, '三个矩形与其它 %d 个 rect 均不相交' % (len([k for k in rects if k not in EXPECT])))

print('\n【⑥ 计数图标不与 CHAL_COUNT / TRIAL_COUNT 重叠】')
for peer in COUNT_PEERS:
    prect = rects.get(peer)
    chk(prect is not None, 'Icons.java 里解出了 %s' % peer)
    if prect:
        chk(not overlap(EXPECT['FUN_COUNT'], prect),
            'FUN_COUNT %s 与 %s %s 不重叠' % (EXPECT['FUN_COUNT'], peer, prect))

print('\n【⑦ 三枚计数图标同规格（FUN_COUNT ≡ CHAL_COUNT ≡ TRIAL_COUNT）】')
fu_rect = EXPECT['FUN_COUNT']
fu_bb = boxes.get('FUN_COUNT')
for peer in COUNT_PEERS:
    prect = rects.get(peer)
    if prect is None:
        chk(False, 'Icons.java 里没解出 %s，无法做同规格比对' % peer)
        continue
    chk(prect[2:] == fu_rect[2:],
        '尺寸应一致：%s %d×%d  vs  FUN_COUNT %d×%d（差 %d px ⇒ 数字基线与图标间距都会错开）'
        % (peer, prect[2], prect[3], fu_rect[2], fu_rect[3],
           abs(prect[3] - fu_rect[3]) + abs(prect[2] - fu_rect[2])))
    pbb = bbox(*prect)
    if pbb and fu_bb:
        print('        %-11s 字形相对位置 dx=%d dy=%d %d×%d' % ((peer,) + rel_glyph(prect, pbb)))
        print('        %-11s 字形相对位置 dx=%d dy=%d %d×%d' % (('FUN_COUNT',) + rel_glyph(fu_rect, fu_bb)))
        chk(same_spec(prect, pbb, fu_rect, fu_bb),
            'FUN_COUNT 与 %s 字形相对偏移应一致（这一条才是「数字对齐」的真正保证）' % peer)
    else:
        chk(False, '取不到 FUN_COUNT 或 %s 的字形包围盒，同规格比对不完整' % peer)

print('\n' + '=' * 74)
print('ALL PASS —— 三枚趣味挑战图标坐标已正确接入且无冲突' if ok else '!! 存在 FAIL，见上')
print('=' * 74)
sys.exit(0 if ok else 1)
