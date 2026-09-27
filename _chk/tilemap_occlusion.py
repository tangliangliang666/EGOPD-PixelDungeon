# -*- coding: utf-8 -*-
"""把角色画成纯品红(255,0,255)，再逐像素判定"是否被上层草叶覆盖"，输出精确的遮挡掩码。
H=角色像素可见，x=被上层贴图覆盖，' '=透明/无关
"""
import sys
sys.path.insert(0, 'D:/PD/_chk')
from tilemap_sim import Sheet, SHEET, FEAT, terrain_visual, raised_visual, walls_visual, features_visual, EMPTY, WALL, HIGH_GRASS

tiles = Sheet(SHEET); feats = Sheet(FEAT)
MW, MH = 3, 5
m = [EMPTY]*(MW*MH)
m[1*MW+1] = HIGH_GRASS          # 草格 (1,1)
m[3*MW+1] = WALL                # 墙 (1,3)

def render(hero_cell):
    CW, CH = MW*16, MH*16
    c = [[(0, 0, 0, 0) for _ in range(CW)] for _ in range(CH)]
    def blend(frame, ox, oy, magenta=False):
        if frame is None: return
        fh, fw = len(frame), len(frame[0])
        for y in range(fh):
            for x in range(fw):
                r, g, b, a = (255, 0, 255, 255) if magenta else frame[y][x]
                if a == 0: continue
                cy, cx = oy+y, ox+x
                if not (0 <= cy < CH and 0 <= cx < CW): continue
                dr, dg, db, da = c[cy][cx]
                al = a/255.0; na = al + da/255.0*(1-al)
                c[cy][cx] = (int(r*al+dr*(1-al)*da/255.0), int(g*al+dg*(1-al)*da/255.0),
                             int(b*al+db*(1-al)*da/255.0), int(na*255))
    for p in range(MW*MH): blend(tiles.frame(terrain_visual(m, MW, p, m[p])), (p % MW)*16, (p//MW)*16)
    for p in range(MW*MH): blend(feats.frame(features_visual(m, MW, p, m[p])), (p % MW)*16, (p//MW)*16)
    hx, hy = hero_cell
    hero = [[(255, 0, 255, 255)]*12 for _ in range(15)]
    blend(hero, hx*16+2, hy*16+1)
    for p in range(MW*MH): blend(tiles.frame(raised_visual(m, MW, p, m[p])), (p % MW)*16, (p//MW)*16)
    for p in range(MW*MH): blend(tiles.frame(walls_visual(m, MW, p, m[p])), (p % MW)*16, (p//MW)*16)
    return c

for hy in (0, 1, 2):
    c = render((1, hy))
    print('\n=== 角色在 (%d,%d)  %s ===' % (1, hy,
          '草格上方那格' if hy == 0 else ('草格本体' if hy == 1 else '草格下方那格')))
    for y in range(hy*16 - 16, hy*16 + 32):
        row = ''
        for x in range(16, 32):
            r, g, b, a = c[y][x]
            if a < 24: row += ' '
            elif abs(r-255) < 8 and g < 8 and abs(b-255) < 8: row += 'H'
            elif abs(r-255) < 40 and g < 30 and abs(b-255) < 40: row += 'h'   # 半透明混合
            else: row += 'x'
        print('%3d|%s|' % (y, row))
