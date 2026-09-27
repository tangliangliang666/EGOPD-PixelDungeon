# -*- coding: utf-8 -*-
"""离线渲染「次元撕裂者」粒子特效预览图（纯 Python，无 PIL）。

用途：用户编译前先看一眼配色与效果。**物理与配色规则严格照抄**
`effects/particles/RiftParticle.java`——`COLORS` 直接从该 .java 里解析（唯一权威来源），
粒子的起点/速度/加速度/寿命/尺寸曲线也按同一套公式积分。

输出：`_chk/_out/rift_fx_preview.png`
  第 1 行：贴图 xy(6,41) 那格放大 ×12（配色出处） + 6 个色块（白 → 深紫）
  第 2 行：HIT / SCATTER / IMPLODE 在 t=0.15s
  第 3 行：同三者在 t=0.30s（看收缩 / 淡出 / 色阶下沉）

只读不改仓库源码，属临时工具。用法：
    python _chk/_rift_fx_preview.py
"""
import math
import os
import random
import re
import struct
import sys
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import verify_sprite_frames as vsf

PART_JAVA = ("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
             "effects/particles/RiftParticle.java")
ITEMS_PNG = "core/src/main/assets/sprites/items.png"
OUT = "_chk/_out/rift_fx_preview.png"

CELL, CELL_PX = (6, 41), 16
SPRITE_W, SPRITE_H = 13, 15      # assignItemRect 声明尺寸
ZOOM_SPRITE = 12
ZOOM_FX = 2

# ---- 与 RiftParticle.java 同步的物理常量（改那边记得改这里）-------------------
MAX_SIZE, MIN_SIZE = 3.2, 1.1
SPEC = {
    # name: (粒子数常量名, speed 区间, acc_y, lifespan 区间, 是否向心)
    "HIT":     ("HIT_PARTICLES",  (24, 52), -12, (0.30, 0.50), False),
    "SCATTER": ("WARP_PARTICLES", (50, 95), -16, (0.35, 0.55), False),
    "IMPLODE": ("WARP_PARTICLES", None,       0, (0.25, 0.40), True),
}
IMPLODE_R = (9, 15)


def parse_colors():
    src = open(PART_JAVA, encoding="utf-8").read()
    m = re.search(r"COLORS\s*=\s*\{([^}]*)\}", src)
    if not m:
        raise SystemExit("RiftParticle.COLORS 解析不到")
    return [int(x, 16) for x in re.findall(r"0x([0-9A-Fa-f]{6})", m.group(1))]


def parse_count(name):
    src = open(PART_JAVA, encoding="utf-8").read()
    w = open("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
             "items/weapon/melee/DimensionalRipper.java", encoding="utf-8").read()
    m = re.search(re.escape(name) + r"\s*=\s*(\d+)", w)
    if not m:
        raise SystemExit("DimensionalRipper.%s 解析不到" % name)
    return int(m.group(1))


class Canvas(object):
    """RGBA 画布；粒子按「加色混合」近似 lightMode（叠加混合）。"""

    def __init__(self, w, h, bg=(18, 14, 28)):
        self.w, self.h = w, h
        self.px = bytearray((bytes(bg) + b"\xff") * (w * h))

    def _idx(self, x, y):
        return (y * self.w + x) * 4

    def rect(self, x, y, w, h, rgb):
        for yy in range(max(0, y), min(self.h, y + h)):
            for xx in range(max(0, x), min(self.w, x + w)):
                i = self._idx(xx, yy)
                self.px[i:i + 3] = bytes(rgb)

    def add(self, cx, cy, side, rgb, alpha):
        """加色叠一个边长 side 的方块（照 PixelParticle：1×1 方块按 size 缩放，origin 0.5）。"""
        half = side / 2.0
        x0, x1 = int(math.floor(cx - half)), int(math.ceil(cx + half))
        y0, y1 = int(math.floor(cy - half)), int(math.ceil(cy + half))
        for yy in range(y0, y1):
            for xx in range(x0, x1):
                if 0 <= xx < self.w and 0 <= yy < self.h:
                    i = self._idx(xx, yy)
                    for c in range(3):
                        self.px[i + c] = min(255, self.px[i + c] + int(rgb[c] * alpha))

    def png_bytes(self):
        rows = b""
        for y in range(self.h):
            rows += b"\x00" + bytes(self.px[y * self.w * 4:(y + 1) * self.w * 4])
        def chunk(typ, data):
            return (struct.pack(">I", len(data)) + typ + data
                    + struct.pack(">I", zlib.crc32(typ + data) & 0xFFFFFFFF))
        return (b"\x89PNG\r\n\x1a\n"
                + chunk(b"IHDR", struct.pack(">IIBBBBB", self.w, self.h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(rows, 9))
                + chunk(b"IEND", b""))


def simulate(kind, colors, rnd):
    """按 RiftParticle 的规则生成一蓬粒子，返回 [(base_idx, x0, y0, vx, vy, ax, ay, lifespan)]。

    出发点都取 (0,0)（调用方再平移到画布中心）。
    """
    count_const, spd, acc_y, life, inward = SPEC[kind]
    n = parse_count(count_const)
    out = []
    for i in range(n):
        base = i % len(colors)
        lifespan = rnd.uniform(*life)
        if inward:
            ang = rnd.uniform(0, 2 * math.pi)
            r = rnd.uniform(*IMPLODE_R)
            x0, y0 = math.cos(ang) * r, math.sin(ang) * r
            # 速度＝位移 ÷ 存活期（照 Java：speed.set(cx-x, cy-y); speed.scale(1f/lifespan)）
            vx, vy = (-x0) / lifespan, (-y0) / lifespan
            ax, ay = 0.0, 0.0
        else:
            ang = rnd.uniform(0, 2 * math.pi)
            v = rnd.uniform(*spd)
            x0, y0 = 0.0, 0.0
            vx, vy = math.cos(ang) * v, math.sin(ang) * v
            ax, ay = 0.0, float(acc_y)
        out.append((base, x0, y0, vx, vy, ax, ay, lifespan))
    return out


def draw_burst(canvas, ox, oy, size, ps, colors, t):
    """把一蓬粒子画到画布上 (ox,oy) 为格子中心，边长为 size 的方框内，采样时刻 t（秒）。"""
    cx, cy = ox + size / 2.0, oy + size / 2.0
    sc = size / 90.0                       # 世界 90×90 px 映射到 size
    used = set()
    for (base, x0, y0, vx, vy, ax, ay, lifespan) in ps:
        left = lifespan - t
        if left <= 0:
            continue
        a = left / lifespan
        px = x0 + vx * t + 0.5 * ax * t * t
        py = y0 + vy * t + 0.5 * ay * t * t
        idx = min(base + (1 if a < 0.5 else 0), len(colors) - 1)
        used.add(idx)
        col = colors[idx]
        side = MIN_SIZE + (MAX_SIZE - MIN_SIZE) * a
        canvas.add(cx + px * sc, cy + py * sc, max(1.0, side * sc),
                   ((col >> 16) & 255, (col >> 8) & 255, col & 255), a)
    return used


def main():
    colors = parse_colors()
    print("COLORS 解析自 RiftParticle.java：%s" % ["#%06X" % c for c in colors])

    # ---- 数值自检（顺便证明公式自洽）----
    rnd = random.Random(20260922)
    for kind in ("HIT", "SCATTER", "IMPLODE"):
        ps = simulate(kind, colors, rnd)
        used = set()
        for (base, x0, y0, vx, vy, ax, ay, life) in ps:
            used.add(base)
            used.add(min(base + (1 if 0 < 0.5 else 0), len(colors) - 1))
        print("  %-8s 粒子 %d 颗；起始色阶覆盖 %d/%d %s"
              % (kind, len(ps), len(used), len(colors),
                 "OK（六色全出场）" if len(used) == len(colors) else "!! 有缺"))
    # IMPLODE：t = lifespan 时应正好落在中心
    worst = max(abs(x0 + vx * life + 0.5 * ax * life * life)
                + abs(y0 + vy * life + 0.5 * ay * life * life)
                for (_, x0, y0, vx, vy, ax, ay, life) in simulate("IMPLODE", colors,
                                                                  random.Random(1)))
    print("  IMPLODE 寿终残差（|dx|+|dy|，应 ≈ 0）= %.6f" % worst)

    # ---- 画布布局 ----
    pad, fx = 12, 180
    row1_h = SPRITE_H * ZOOM_SPRITE
    W = pad + SPRITE_W * ZOOM_SPRITE + pad + 6 * 46 + pad
    H = pad + row1_h + pad + 2 * (fx + 34) + pad
    cv = Canvas(max(W, pad + 3 * fx + 2 * pad + pad), H)

    # 第 1 行左：贴图那格放大
    w, h, ct, nch, plte, trns, px = vsf.decode_png(ITEMS_PNG)
    ox0, oy0 = (CELL[0] - 1) * CELL_PX, (CELL[1] - 1) * CELL_PX
    for dy in range(SPRITE_H):
        for dx in range(SPRITE_W):
            r, g, b, a = vsf.pixel(px, ct, nch, plte, trns, ox0 + dx, oy0 + dy, w)
            if a > 0:
                ba = a / 255.0
                cv.rect(pad + dx * ZOOM_SPRITE, pad + dy * ZOOM_SPRITE,
                        ZOOM_SPRITE, ZOOM_SPRITE, (int(r * ba), int(g * ba), int(b * ba)))
    # 第 1 行右：6 个色块（白 → 深紫）
    sx = pad + SPRITE_W * ZOOM_SPRITE + pad
    for k, c in enumerate(colors):
        cv.rect(sx, pad + k * 46 + k * 2, 200, 44,
                ((c >> 16) & 255, (c >> 8) & 255, c & 255))
        cv.rect(sx + 5, pad + k * 46 + k * 2 + 5, 44, 34,
                ((c >> 16) & 255, (c >> 8) & 255, c & 255))

    # 第 2/3 行：三种 Factory 各两帧
    y = pad + row1_h + pad
    for r_i, t in enumerate((0.15, 0.30)):
        for c_i, kind in enumerate(("HIT", "SCATTER", "IMPLODE")):
            ox = pad + c_i * fx
            oy = y + r_i * (fx + 34)
            cv.rect(ox, oy, fx, fx, (10, 8, 18))
            ps = simulate(kind, colors, random.Random(20260922 + hash(kind) % 977 + r_i))
            used = draw_burst(cv, ox, oy, fx, ps, colors, t)
            print("  帧 t=%.2fs %-8s 用色 %s" % (t, kind, sorted("#%06X" % colors[i] for i in used)))

    if not os.path.isdir(os.path.dirname(OUT)):
        os.makedirs(os.path.dirname(OUT))
    data = cv.png_bytes()
    open(OUT, "wb").write(data)
    print("已写出 %s（%dx%d，%d 字节）" % (OUT, cv.w, cv.h, len(data)))


if __name__ == "__main__":
    main()
