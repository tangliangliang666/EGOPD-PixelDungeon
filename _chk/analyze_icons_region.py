# 分析 icons.png 上指定矩形区域的实际内容（包围盒 / 是否越界 / 是否为空）
# 用途：配置 Icons.java 坐标前，先确认「图真的画在这儿、且没画到格子外」。
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__))))
from verify_sprite_frames import decode_png

PATH = sys.argv[1] if len(sys.argv) > 1 else 'core/src/main/assets/interfaces/icons.png'

REGIONS = [
    # (标签, x, y, w, h)
    ('TRIAL_GREY   新位置', 208, 32, 16, 16),
    ('TRIAL_COLOR  新位置', 224, 32, 16, 16),
    ('TRIAL_COUNT  新位置', 160, 88, 7, 7),   # 与 CHAL_COUNT 同规格 7×7（尺寸不同会让计数数字错位 1px）
    ('CHAL_COUNT   (参照)', 160, 80, 7, 7),
    ('CHALLENGE_GREY(参照)', 144, 16, 15, 12),
    ('CHALLENGE_COLOR(参照)', 144, 32, 15, 12),
]

img = decode_png(PATH)
w, h = img[0], img[1]
px = img[-1]
nch = len(px) // (w * h)

print('文件 %s  (%d 字节)' % (PATH, os.path.getsize(PATH)))
print('尺寸 %dx%d  nch=%d' % (w, h, nch))
print()


def alpha(x, y):
    return px[(y * w + x) * nch + (nch - 1)]


def probe(tag, x, y, rw, rh, pad=8):
    # 在「格子 + pad」范围内找非透明像素，判断有无越界
    minx = miny = 10 ** 9
    maxx = maxy = -1
    cnt = 0
    for yy in range(max(0, y - pad), min(h, y + rh + pad)):
        for xx in range(max(0, x - pad), min(w, x + rw + pad)):
            if alpha(xx, yy) > 0:
                cnt += 1
                minx = min(minx, xx); maxx = max(maxx, xx)
                miny = min(miny, yy); maxy = max(maxy, yy)
    if cnt == 0:
        print('%-22s (%3d,%3d) %2dx%-2d  —— 完全空（±%d 邻域内无任何非透明像素）'
              % (tag, x, y, rw, rh, pad))
        return
    inside = True
    for yy in range(max(0, y - pad), min(h, y + rh + pad)):
        for xx in range(max(0, x - pad), min(w, x + rw + pad)):
            if alpha(xx, yy) > 0 and not (x <= xx < x + rw and y <= yy < y + rh):
                inside = False
    print('%-22s (%3d,%3d) %2dx%-2d  实测包围盒 x=%d y=%d w=%d h=%d  非透明=%d  %s'
          % (tag, x, y, rw, rh, minx, miny, maxx - minx + 1, maxy - miny + 1, cnt,
             '完全落在格内' if inside else '!! 有像素越出格子'))

    # 顺带：把格子内每一行/列的分布打出来（窄图才有意义）
    if cnt and (rw <= 16 and rh <= 16):
        for yy in range(y, y + rh):
            row = ''.join('#' if alpha(xx, yy) > 0 else '.' for xx in range(x, x + rw))
            print('      y=%3d  %s' % (yy, row))


for tag, x, y, rw, rh in REGIONS:
    probe(tag, x, y, rw, rh)
    print()
