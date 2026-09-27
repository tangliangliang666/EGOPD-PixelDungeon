# -*- coding: utf-8 -*-
"""用 SPD 真实贴图复刻分层 tilemap 渲染并输出 PNG，用于肉眼核验「两格高的草」的成因。
输出: D:/PD/_chk/tilemap_render.png
"""
import zlib, struct
from tilemap_sim import (Sheet, SHEET, FEAT, xy, terrain_visual, raised_visual, walls_visual,
                         features_visual, EMPTY, GRASS, WALL, HIGH_GRASS, CANVAS_W)

WAR = 'core/src/main/assets/sprites/warrior.png'

def write_png(path, canvas):
    h = len(canvas); w = len(canvas[0])
    raw = bytearray()
    for row in canvas:
        raw.append(0)
        for (r, g, b, a) in row:
            raw += bytes((r, g, b, a))
    def chunk(t, d):
        c = struct.pack('>I', len(d)) + t + d
        return c + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(bytes(raw), 9))
    png += chunk(b'IEND', b'')
    open(path, 'wb').write(png)

tiles = Sheet(SHEET); feats = Sheet(FEAT)
war = Sheet(WAR)

MW, MH = 12, 8
m = [EMPTY] * (MW * MH)
def put(x, y, t): m[y*MW + x] = t
for x in range(MW):
    put(x, 0, WALL); put(x, MH-1, WALL)
for y in range(MH):
    put(0, y, WALL); put(MW-1, y, WALL)
# 3x3 高草（抬高地形）
for y in (3, 4, 5):
    for x in (3, 4, 5):
        put(x, y, HIGH_GRASS)
# 2x2 独立墙体（看墙面/檐/内部黑）
for y in (2, 3):
    for x in (8, 9):
        put(x, y, WALL)
# 平地草（Terrain.GRASS：只有 terrain_features 草叶，不抬高）
for x in range(2, 5):
    put(x, 6, GRASS)

CW, CH = MW*16, MH*16
def new_canvas(): return [[(0, 0, 0, 0) for _ in range(CW)] for _ in range(CH)]

def blend(canvas, frame, ox, oy):
    if frame is None: return
    fh, fw = len(frame), len(frame[0])
    for y in range(fh):
        for x in range(fw):
            r, g, b, a = frame[y][x]
            if a == 0: continue
            cy, cx = oy+y, ox+x
            if 0 <= cy < CH and 0 <= cx < CW:
                dr, dg, db, da = canvas[cy][cx]
                al = a/255.0; na = al + da/255.0*(1-al)
                canvas[cy][cx] = (int(r*al + dr*(1-al)*da/255.0),
                                  int(g*al + dg*(1-al)*da/255.0),
                                  int(b*al + db*(1-al)*da/255.0), int(na*255))

HEROES = [(3, 4), (3, 2), (2, 6), (9, 4)]   # 草上 / 草上方(被草盖腿) / 平地草上 / 墙下方
def hero_frame(): return war.frame_sz(0, 12, 15)   # warrior idle 帧 0（12x15）

c = new_canvas()
for p in range(MW*MH):
    blend(c, tiles.frame(terrain_visual(m, MW, p, m[p])), (p % MW)*16, (p//MW)*16)
for p in range(MW*MH):
    blend(c, feats.frame(features_visual(m, MW, p, m[p])), (p % MW)*16, (p//MW)*16)
hf = hero_frame()
for (hx, hy) in HEROES:                     # mobs 层（角色在 raised/walls 之下）
    blend(c, hf, hx*16 + 2, hy*16 + 1)
for p in range(MW*MH):
    blend(c, tiles.frame(raised_visual(m, MW, p, m[p])), (p % MW)*16, (p//MW)*16)
for p in range(MW*MH):
    blend(c, tiles.frame(walls_visual(m, MW, p, m[p])), (p % MW)*16, (p//MW)*16)

S = 3
big = [[c[y//S][x//S] for x in range(CW*S)] for y in range(CH*S)]
write_png('D:/PD/_chk/tilemap_render.png', big)
print('wrote _chk/tilemap_render.png  %dx%d (原图 %dx%d, 放大 %dx)' % (CW*S, CH*S, CW, CH, S))
print('角色位置: ' + ', '.join('(%d,%d)' % h for h in HEROES))
