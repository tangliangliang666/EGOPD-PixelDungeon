# -*- coding: utf-8 -*-
"""测量 icons.png 上「趣味挑战」三枚图标的实际不透明包围盒。
锚点（用户口径）：灰化(128,48) / 点亮(144,48) / 计数(160,48)。
只读，不改文件。"""
import sys
from PIL import Image

P = r"D:\PD\core\src\main\assets\interfaces\icons.png"


def bbox_in(im, x0, y0, w, h):
    px = im.load()
    xs, ys = [], []
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if px[x, y][3] != 0:
                xs.append(x)
                ys.append(y)
    if not xs:
        return None
    return (min(xs) - x0, min(ys) - y0, max(xs) - x0, max(ys) - y0)


def main():
    im = Image.open(P).convert("RGBA")
    print("icons.png size =", im.size)
    for name, x0, y0 in (("FUN_GREY", 128, 48), ("FUN_COLOR", 144, 48), ("FUN_COUNT", 160, 48)):
        b = bbox_in(im, x0, y0, 16, 16)
        if b is None:
            print("%-10s @(%d,%d)  空白（整格透明）" % (name, x0, y0))
        else:
            bw = b[2] - b[0] + 1
            bh = b[3] - b[1] + 1
            print("%-10s @(%d,%d)  bbox x%d..%d y%d..%d  => %dx%d  (相对格原点偏移 +%d,+%d)"
                  % (name, x0, y0, b[0], b[2], b[1], b[3], bw, bh, b[0], b[1]))
    # 相邻参考：同屏右邻格是否有内容，确认没有越界重叠
    for name, x0, y0 in (("ref@(176,48) 右邻", 176, 48), ("ref@(128,32) 上邻", 128, 32), ("ref@(128,64) 下邻", 128, 64)):
        b = bbox_in(im, x0, y0, 16, 16)
        print("%-16s => %s" % (name, "空" if b is None else b))
    return 0


if __name__ == "__main__":
    sys.exit(main())
