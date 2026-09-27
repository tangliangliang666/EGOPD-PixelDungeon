#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""渲染标题界面的**模拟预览图**，供人工检查视觉效果（不进游戏）。

预览里哪些是**精确**的、哪些是**近似**的，必须说清楚：

  ✅ 精确：标题/闪烁帧的裁切（直接取 banners.png 的真实像素）、
          每个按钮的矩形（复用 `title_layout_calc.layout()`，与真机同一套公式）、
          标题与按钮的相对位置、可用区底边、下折箭头与版本号的位置
  ⚠️ 近似：滚动背景。真机是 `TitleBackground` 的 6 层视差 + 程序生成的暗化渐变；
          这里用真实贴图（已改深红的那 4 张）按**静态**方式铺一层，只求色调与构图接近。
          所以**不要**用这张图判断背景的滚动/视差手感，只看色与构图。
  ❌ 无字：按钮里的中文无法用位图字体渲染（`pixel_font.png` 只有拉丁字形），
          所以按钮内画**编号**，中文名见脚本打印的图例。

用法：
    python _chk/render_title_preview.py                 # 横屏 426×240 + 竖屏 180×320
    python _chk/render_title_preview.py --devices       # 额外渲染几档常见机型的相机尺寸
"""

import argparse
import os
import struct
import sys
import zlib

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, '_chk'))

import title_layout_calc as TLC   # noqa: E402  复用同一套排版公式

ASSETS = os.path.join(ROOT, 'core', 'src', 'main', 'assets')
BANNERS = os.path.join(ASSETS, 'interfaces', 'banners.png')
TITLE_DIR = os.path.join(ASSETS, 'splashes', 'title')

NEW_RECTS = {
    'TITLE_PORT': (0, 0, 169, 94),
    'TITLE_LAND': (176, 0, 427, 65),
    'TITLE_GLOW_PORT': (253, 66, 422, 160),
    'TITLE_GLOW_LAND': (253, 66, 504, 131),
}

LEGEND = [('play', '进入地牢'), ('support', '支持游戏开发'), ('rankings', '排行榜'),
          ('journal', '日志'), ('news', '游戏新闻（未 add，空槽）'), ('changes', '改动'),
          ('settings', '设置'), ('about', '关于')]

DIGITS = {  # 3x5 点阵，只为了在按钮里画编号
    '0': ['111', '101', '101', '101', '111'], '1': ['010', '110', '010', '010', '111'],
    '2': ['111', '001', '111', '100', '111'], '3': ['111', '001', '111', '001', '111'],
    '4': ['101', '101', '111', '001', '001'], '5': ['111', '100', '111', '001', '111'],
    '6': ['111', '100', '111', '101', '111'], '7': ['111', '001', '001', '001', '001'],
    '8': ['111', '101', '111', '101', '111'], '9': ['111', '101', '111', '001', '111'],
    'v': ['000', '101', '101', '101', '010'],
}


# ------------------------------------------------------------------ PNG

def decode_rgba(path):
    raw = open(path, 'rb').read()
    w, h = struct.unpack('>II', raw[16:24])
    ct = raw[25]
    i, idat, plte, trns = 8, b'', None, b''
    while i < len(raw):
        ln = struct.unpack('>I', raw[i:i + 4])[0]
        t = raw[i + 4:i + 8]
        d = raw[i + 8:i + 8 + ln]
        if t == b'IDAT':
            idat += d
        elif t == b'PLTE':
            plte = d
        elif t == b'tRNS':
            trns = d
        i += 12 + ln
        if t == b'IEND':
            break
    data = zlib.decompress(idat)
    if ct == 6:
        bpp, stride = 4, w * 4
    elif ct == 3:
        bpp, stride = 1, w
    elif ct == 2:
        bpp, stride = 3, w * 3
    else:
        raise SystemExit('%s 颜色类型 %d 不支持' % (path, ct))
    rows, prev, pos = [], bytearray(stride), 0
    for _ in range(h):
        ft = data[pos]; pos += 1
        line = bytearray(data[pos:pos + stride]); pos += stride
        if ft == 1:
            for k in range(bpp, stride):
                line[k] = (line[k] + line[k - bpp]) & 255
        elif ft == 2:
            for k in range(stride):
                line[k] = (line[k] + prev[k]) & 255
        elif ft == 3:
            for k in range(stride):
                a = line[k - bpp] if k >= bpp else 0
                line[k] = (line[k] + ((a + prev[k]) >> 1)) & 255
        elif ft == 4:
            for k in range(stride):
                a = line[k - bpp] if k >= bpp else 0
                c = prev[k - bpp] if k >= bpp else 0
                b = prev[k]
                pp = a + b - c
                pa, pb, pc = abs(pp - a), abs(pp - b), abs(pp - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[k] = (line[k] + pr) & 255
        rows.append(line)
        prev = line

    px = []
    if ct == 6:
        for line in rows:
            for k in range(0, stride, 4):
                px.append((line[k], line[k + 1], line[k + 2], line[k + 3]))
    elif ct == 2:
        for line in rows:
            for k in range(0, stride, 3):
                px.append((line[k], line[k + 1], line[k + 2], 255))
    else:
        for line in rows:
            for k in range(stride):
                idx = line[k]
                r, g, b = plte[idx * 3:idx * 3 + 3]
                a = trns[idx] if idx < len(trns) else 255
                px.append((r, g, b, a))
    return w, h, px


def write_rgb_png(path, w, h, rows):
    def chunk(t, d):
        return (struct.pack('>I', len(d)) + t + d +
                struct.pack('>I', zlib.crc32(t + d) & 0xffffffff))
    raw = b''.join(b'\x00' + r for r in rows)
    open(path, 'wb').write(b'\x89PNG\r\n\x1a\n'
                           + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0))
                           + chunk(b'IDAT', zlib.compress(raw, 6)) + chunk(b'IEND', b''))


# ------------------------------------------------------------------ 画布

class Canvas:
    def __init__(self, w, h, bg=(8, 6, 8)):
        self.w, self.h = w, h
        self.px = bytearray(bytes(bg) * (w * h))

    def blend(self, x, y, rgb, a):
        if a <= 0 or not (0 <= x < self.w and 0 <= y < self.h):
            return
        o = (y * self.w + x) * 3
        if a >= 255:
            self.px[o:o + 3] = bytes(rgb)
            return
        for k in range(3):
            self.px[o + k] = (self.px[o + k] * (255 - a) + rgb[k] * a) // 255

    def rect(self, x, y, w, h, rgb, a=255):
        for yy in range(int(y), int(y + h)):
            for xx in range(int(x), int(x + w)):
                self.blend(xx, yy, rgb, a)

    def frame(self, x, y, w, h, rgb):
        self.rect(x, y, w, 1, rgb)
        self.rect(x, y + h - 1, w, 1, rgb)
        self.rect(x, y, 1, h, rgb)
        self.rect(x + w - 1, y, 1, h, rgb)

    def blit(self, imgw, imgh, px, dx, dy, scale=1.0, alpha=1.0):
        for y in range(imgh):
            sy = int(y * scale)
            ty = int(dy + y * scale)
            if not (0 <= ty < self.h):
                continue
            for x in range(imgw):
                sx = int(x * scale)
                tx = int(dx + x * scale)
                if not (0 <= tx < self.w):
                    continue
                r, g, b, a = px[sy * imgw + sx]
                self.blend(tx, ty, (r, g, b), int(a * alpha))

    def tile(self, imgw, imgh, px, scale, y0, y1, alpha=1.0, offset_x=0):
        tw = max(1, int(imgw * scale))
        th = max(1, int(imgh * scale))
        y = y0
        while y < y1:
            x = offset_x - tw
            while x < self.w:
                self.blit(imgw, imgh, px, x, y, scale, alpha)
                x += tw
            y += th

    def text3x5(self, s, x, y, rgb):
        cx = x
        for ch in s:
            g = DIGITS.get(ch)
            if not g:
                cx += 4
                continue
            for ry, rowbits in enumerate(g):
                for rx, bit in enumerate(rowbits):
                    if bit == '1':
                        self.blend(cx + rx, y + ry, rgb, 255)
            cx += 4

    def upscale(self, k):
        out = bytearray(self.w * k * self.h * k * 3)
        W = self.w * k
        for y in range(self.h * k):
            sy = y // k
            for x in range(W):
                sx = x // k
                o = (y * W + x) * 3
                s = (sy * self.w + sx) * 3
                out[o:o + 3] = self.px[s:s + 3]
        return W, self.h * k, out


# ------------------------------------------------------------------ 渲染

def render(cam_w, cam_h, out_name, upscale=3):
    L = TLC.layout(cam_w, cam_h)
    c = Canvas(int(cam_w), int(cam_h))

    # 1) 背景：真实贴图静态铺底（近似，见文件头说明）
    arch = decode_rgba(os.path.join(TITLE_DIR, 'archs.png'))
    back = decode_rgba(os.path.join(TITLE_DIR, 'back_clusters.png'))
    mid = decode_rgba(os.path.join(TITLE_DIR, 'mid_mixed.png'))
    small = decode_rgba(os.path.join(TITLE_DIR, 'front_small.png'))
    s = cam_h / 450.0
    if cam_w <= cam_h:
        s /= 1.5
    c.tile(arch[0], arch[1], arch[2], s, -int(100 * s), cam_h, alpha=1.0)
    # 暗化渐变（真机是 TextureCache.createGradient，这里近似成向下渐暗）
    for y in range(int(cam_h)):
        a = int(0x88 * (y / max(1.0, cam_h)))
        c.rect(0, y, cam_w, 1, (0, 0, 0), a)
    c.tile(back[0], back[1], back[2], s, int(cam_h * 0.15), cam_h, alpha=1.0)
    c.tile(mid[0], mid[1], mid[2], s, int(cam_h * 0.35), cam_h, alpha=1.0)
    c.tile(small[0], small[1], small[2], s, int(cam_h * 0.55), cam_h, alpha=1.0)

    # 2) 标题横幅（**精确**：直接取 banners.png 的真实帧）
    bw, bh, bpx = decode_rgba(BANNERS)
    rect = NEW_RECTS['TITLE_LAND' if L['land'] else 'TITLE_PORT']
    x0, y0, x1, y1 = rect
    fw, fh = x1 - x0, y1 - y0
    sub = [bpx[(y0 + yy) * bw + (x0 + xx)] for yy in range(fh) for xx in range(fw)]

    tx, ty = int(L['title_x']), int(L['title_y'])
    for yy in range(fh):
        for xx in range(fw):
            r, g, b, a = sub[yy * fw + xx]
            c.blend(tx + xx, ty + yy, (r, g, b), a)
    # 若横幅超出屏幕，用红框标出被裁的部分
    if tx < 0 or tx + fw > cam_w or ty < 0 or ty + fh > cam_h:
        c.frame(max(0, tx), max(0, ty), min(fw, cam_w - max(0, tx)),
                min(fh, cam_h - max(0, ty)), (255, 0, 0))

    # 3) 空闪烁帧的位置（画个空心框示意它「在哪但画不出东西」）
    grect = NEW_RECTS['TITLE_GLOW_LAND' if L['land'] else 'TITLE_GLOW_PORT']
    gw, gh = grect[2] - grect[0], grect[3] - grect[1]
    gx = int(tx + (fw - gw) / 2.0)
    gy = ty
    c.frame(max(0, gx), max(0, gy), min(gw, cam_w - max(0, gx)), min(gh, cam_h - max(0, gy)),
            (70, 70, 110))

    # 4) 按钮（**精确**坐标），编号 + 空槽用虚线感标出
    for i, (key, cn) in enumerate(LEGEND):
        x, y, bwid, bhei = L['rects'][key]
        x, y, bwid, bhei = int(x), int(y), int(bwid), int(bhei)
        added = dict((k, a) for k, _, a in TLC.BUTTONS)[key]
        if added:
            c.rect(x, y, bwid, bhei, (40, 34, 44), 235)
            c.frame(x, y, bwid, bhei, (150, 140, 160))
            c.text3x5(str(i), x + 4, y + max(1, (bhei - 5) // 2), (255, 236, 200))
        else:
            c.frame(x, y, bwid, bhei, (150, 60, 60))
            c.text3x5('0', x + 4, y + max(1, (bhei - 5) // 2), (150, 60, 60))

    # 5) 可用区底边 + 下折箭头 + 版本号
    c.rect(0, int(L['h']) - 1, cam_w, 1, (0, 200, 120), 200)
    c.frame(int(L['fade_x']), int(L['fade_y']), 16, 16, (200, 200, 200))
    c.text3x5('v', int(L['fade_x']) + 6, int(L['fade_y']) + 5, (240, 240, 240))
    vw = 30
    c.rect(cam_w - vw - 4, cam_h - 8, vw, 6, (136, 136, 136), 200)

    W, H, buf = c.upscale(upscale)
    rows = [bytes(buf[i * W * 3:(i + 1) * W * 3]) for i in range(H)]
    out = os.path.join(ROOT, '_chk', out_name)
    write_rgb_png(out, W, H, rows)

    return out, L


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--devices', action='store_true')
    args = ap.parse_args()

    jobs = [(426, 240, '_title_preview_land.png'), (180, 320, '_title_preview_port.png')]
    if args.devices:
        jobs += [(400, 180, '_title_preview_land_short.png'),
                 (240, 160, '_title_preview_land_min.png'),
                 (154, 343, '_title_preview_port_phone.png'),
                 (135, 225, '_title_preview_port_min.png')]

    print('渲染模拟预览（横屏/竖屏各一张，按钮内是编号，中文名见下方图例）')
    print()
    for cw, ch, name in jobs:
        out, L = render(cw, ch, name)
        banner_w = int(L['title'][0])
        fit = L['title_x'] >= 0 and L['title_x'] + banner_w <= cw
        print('  %-34s 相机 %dx%d  %s  横幅 %dx%d 左上=(%.0f,%.0f)  %s'
              % (os.path.relpath(out, ROOT), cw, ch, '横' if L['land'] else '竖',
                 banner_w, int(L['title'][1]), L['title_x'], L['title_y'],
                 '✅ 横幅完整' if fit else '⚠️ 横幅被裁 %d px' % int(banner_w - cw)))
    print()
    print('按钮编号 → 中文名：')
    for i, (key, cn) in enumerate(LEGEND):
        added = dict((k, a) for k, _, a in TLC.BUTTONS)[key]
        print('  %d = %-22s %s' % (i, cn, '已显示' if added else '**未 add（空槽，图上用红框）**'))
    print()
    print('图例：绿线=可用区底边（超出即被安全区/屏幕裁掉）｜蓝框=空闪烁帧的位置（画不出东西）')
    print('      红框=横幅被屏幕裁切的边界｜右下灰块=版本号位置')
    print()
    print('⚠️ 背景是**静态近似**（真机是 6 层视差滚动），只看色调与构图，别据此判断滚动手感。')
    return 0


if __name__ == '__main__':
    sys.exit(main())
