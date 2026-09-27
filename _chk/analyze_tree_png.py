# 分析 tree.png：尺寸、16x16 帧网格、逐帧非透明像素包围盒
import sys
sys.path.insert(0, '_chk')
from verify_sprite_frames import decode_png


def main(path, fw=16, fh=16):
    img = decode_png(path)
    # decode_png 的返回结构探测：优先取 (w,h,pixels)
    if isinstance(img, tuple):
        print('decode_png 返回元组长度', len(img), [type(x).__name__ for x in img])
        w, h = img[0], img[1]
        px = img[-1]
    else:
        raise SystemExit('未知返回结构')
    nch = len(px) // (w * h) if hasattr(px, '__len__') else 4
    print('尺寸 %dx%d  nch=%d' % (w, h, nch))

    cols, rows = w // fw, h // fh
    print('帧网格 %d 列 x %d 行 = %d 帧（每帧 %dx%d）' % (cols, rows, cols * rows, fw, fh))
    if w % fw or h % fh:
        print('!! 尺寸不能被帧宽/帧高整除')

    def alpha(x, y):
        return px[(y * w + x) * nch + (nch - 1)]

    for r in range(rows):
        for c in range(cols):
            idx = r * cols + c
            x0, y0 = c * fw, r * fh
            minx, miny, maxx, maxy, cnt = fw, fh, -1, -1, 0
            for yy in range(y0, y0 + fh):
                for xx in range(x0, x0 + fw):
                    if alpha(xx, yy) > 0:
                        cnt += 1
                        if xx - x0 < minx:
                            minx = xx - x0
                        if yy - y0 < miny:
                            miny = yy - y0
                        if xx - x0 > maxx:
                            maxx = xx - x0
                        if yy - y0 > maxy:
                            maxy = yy - y0
            if cnt == 0:
                print('帧 %-2d (列%d 行%d)  空' % (idx, c, r))
            else:
                print('帧 %-2d (列%d 行%d)  包围盒 x=%d y=%d w=%d h=%d  非透明像素=%d'
                      % (idx, c, r, minx, miny, maxx - minx + 1, maxy - miny + 1, cnt))


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else 'tree.png')
