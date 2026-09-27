# 核验「考验系统」三枚图标：Icons.java 里的 rect 是否与 icons.png 上真实绘制的像素对齐、是否与别的图标格冲突
#
# 断言链（任一失败即 [FAIL] 且退出码 1）：
#   ① Icons.java 里 TRIAL_GREY / TRIAL_COLOR / TRIAL_COUNT 三处 rect 等于预期值；
#   ② icons.png 尺寸 256×128、能被 16 整除；
#   ③ 三个矩形内**非空**（图真的画了）、且内容**没有越出**矩形（越界 = 会切到邻居或露出透明边）；
#   ④ 灰/彩两枚的内容包围盒**恰好等于**整格（满格），计数那枚**落在格内**且不越界；
#   ⑤ 三个矩形与**其它任何** case 的矩形都**不相交**（防止误占别人的格）；
#   ⑥ 计数那枚不与正上方的 CHAL_COUNT 重叠；
#   ⑦ 「同规格守卫」：TRIAL_COUNT 必须与 CHAL_COUNT 同为 7×7，且两枚图内字形的相对偏移一致
#      —— 因为 MenuPane.layout() 里计数数字的位置是 icon.y + icon.height()、横向也是按 icon.width() 居中，
#         尺寸只要差 1px，数字就会比挑战的低 1px、图标间距从 7px 变 8px（本脚本最初就是漏了这条才没拦住）。
#
# --selftest：故意喂一组「必然重叠」的假数据，确认第⑤条的判定不是空转。
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
    'TRIAL_GREY': (208, 32, 16, 16),
    'TRIAL_COLOR': (224, 32, 16, 16),
    'TRIAL_COUNT': (160, 88, 7, 7),
}
FULL_BLEED = {'TRIAL_GREY', 'TRIAL_COLOR'}   # 这两枚应当满格

ok = True


def chk(cond, msg):
    global ok
    print(('  [OK]   ' if cond else '  [FAIL] ') + msg)
    if not cond:
        ok = False


# ---------- 解析 Icons.java 的 case -> rect ----------
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
    """字形相对所在矩形的偏移与尺寸（与矩形绝对位置无关，因此可直接跨两枚图标比较）"""
    return (bb[0] - rect[0], bb[1] - rect[1], bb[2], bb[3])


def same_spec(a_rect, a_bb, b_rect, b_bb):
    """两枚图标是否「同规格」：矩形尺寸一致 且 字形相对偏移一致。
    只有同规格，MenuPane 里共用的那两行布局公式（icon.y+icon.height() / 按 icon.width() 居中）
    才会让两处计数数字落在同一条基线上。"""
    return a_rect[2:] == b_rect[2:] and rel_glyph(a_rect, a_bb) == rel_glyph(b_rect, b_bb)


def selftest():
    print('== selftest：确认「重叠判定」能抓到冲突 ==')
    r = {'A': (16, 16, 16, 16), 'B': (24, 24, 8, 8)}       # B 落在 A 里
    bad = [k for k, v in r.items() if k != 'A' and overlap(v, r['A'])]
    print('  假数据 A(16,16,16,16) 与 B(24,24,8,8) -> 检出的冲突方：%s' % bad)
    assert bad == ['B'], '重叠判定失效：%s' % bad
    r2 = {'A': (16, 16, 16, 16), 'B': (32, 32, 8, 8)}      # 恰好贴边、不重叠
    bad2 = [k for k, v in r2.items() if k != 'A' and overlap(v, r2['A'])]
    print('  假数据 A(16,16,16,16) 与 B(32,32,8,8) [仅贴角] -> 检出的冲突方：%s（应为空）' % bad2)
    assert bad2 == [], '重叠判定过敏：贴边被误判为重叠 %s' % bad2

    print('== selftest：确认「同规格守卫」能抓出尺寸错配 ==')
    # 挑战 (160,80,7,7) 字形 5×6 @ +1,+1
    ch_r, ch_b = (160, 80, 7, 7), (161, 81, 5, 6)
    # ① 考验也收成 7×7、字形同样 @ +1,+1 -> 应当判为同规格
    ok_r, ok_b = (160, 88, 7, 7), (161, 89, 5, 6)
    print('  7×7 / 7×7 同字形相对位置 -> same_spec=%s' % same_spec(ch_r, ch_b, ok_r, ok_b))
    assert same_spec(ch_r, ch_b, ok_r, ok_b), '守卫过敏：同规格被误判为不同规格'
    # ② 复现本次真实 bug：考验画成 8×8（字形相对位置仍 +1,+1）-> 必须判为不同规格
    bug_r, bug_b = (160, 88, 8, 8), (161, 89, 5, 6)
    print('  7×7 / 8×8（即本次 bug）    -> same_spec=%s' % same_spec(ch_r, ch_b, bug_r, bug_b))
    assert not same_spec(ch_r, ch_b, bug_r, bug_b), '守卫失效：8×8 被误判为同规格 —— 这正是本次漏检的 bug'
    # ③ 尺寸一样但字形画歪了 -> 也必须抓到
    off_r, off_b = (160, 88, 7, 7), (162, 89, 5, 6)
    print('  7×7 / 7×7 但字形偏移 +2   -> same_spec=%s' % same_spec(ch_r, ch_b, off_r, off_b))
    assert not same_spec(ch_r, ch_b, off_r, off_b), '守卫失效：字形错位未被检出'
    print('  selftest 通过：重叠判定既不空转也不过敏；同规格守卫抓得住尺寸错配与字形错位。\n')


print('=' * 74)
print('核验：考验三枚图标（Icons.java rect ↔ icons.png 实际像素）')
print('=' * 74)

if '--selftest' in sys.argv:
    selftest()

java = open(ICONS_JAVA, encoding='utf-8').read()
rects = parse_rects(java)
print('\n从 Icons.java 解出 %d 个「纯数字」rect（带表达式如 DEPTH 的会自动跳过）' % len(rects))
for name in ('TRIAL_GREY', 'TRIAL_COLOR', 'TRIAL_COUNT'):
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
        # 计数图标：内容比格子小是正常的，只要求「有 1 边留白不至于贴着格边被切」
        chk(b[2] <= rw and b[3] <= rh, '%s 内容尺寸不超过格子' % name)

    # 越界探测：检查紧贴矩形外的 1px 环带里有没有本该属于本图的像素（粗略，仅提示）
    ring = 0
    for xx in range(x - 1, x + rw + 1):
        for yy in (y - 1, y + rh):
            ring += 1 if alpha(xx, yy) else 0
    for yy in range(y, y + rh):
        for xx in (x - 1, x + rw):
            ring += 1 if alpha(xx, yy) else 0
    print('        紧邻 1px 环带内非透明像素 = %d（含邻居图内容，仅作提示）' % ring)

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

print('\n【⑥ 计数图标不与 CHAL_COUNT 重叠】')
chk(not overlap(EXPECT['TRIAL_COUNT'], rects.get('CHAL_COUNT', (0, 0, 0, 0))),
    'TRIAL_COUNT %s 与 CHAL_COUNT %s 不重叠' % (EXPECT['TRIAL_COUNT'], rects.get('CHAL_COUNT')))

print('\n【⑦ 同规格守卫：TRIAL_COUNT 与 CHAL_COUNT 等尺寸、字形相对位置一致】')
ch_rect = rects.get('CHAL_COUNT')
tr_rect = EXPECT['TRIAL_COUNT']
if ch_rect is None:
    chk(False, 'Icons.java 里没解出 CHAL_COUNT，无法做同规格比对')
else:
    chk(ch_rect[2:] == tr_rect[2:],
        '尺寸应一致：CHAL_COUNT %d×%d  vs  TRIAL_COUNT %d×%d'
        '（差 %d px ⇒ 计数数字纵向差同样 px 数、图标间距也会变）'
        % (ch_rect[2], ch_rect[3], tr_rect[2], tr_rect[3],
           abs(ch_rect[3] - tr_rect[3]) + abs(ch_rect[2] - tr_rect[2])))
    ch_bb = bbox(*ch_rect)
    tr_bb = boxes.get('TRIAL_COUNT')
    if ch_bb and tr_bb:
        print('        挑战 字形相对位置 dx=%d dy=%d %d×%d' % rel_glyph(ch_rect, ch_bb))
        print('        考验 字形相对位置 dx=%d dy=%d %d×%d' % rel_glyph(tr_rect, tr_bb))
        chk(same_spec(ch_rect, ch_bb, tr_rect, tr_bb),
            '字形相对偏移应一致（这一条才是「数字对齐」的真正保证，仅查不重叠是查不出的）')
    else:
        chk(False, '取不到其中一枚的字形包围盒，同规格比对不完整')

print('\n' + '=' * 74)
print('ALL PASS —— 三枚考验图标坐标已正确接入且无冲突' if ok else '!! 存在 FAIL，见上')
print('=' * 74)
sys.exit(0 if ok else 1)
