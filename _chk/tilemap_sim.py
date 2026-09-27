# -*- coding: utf-8 -*-
"""复刻 SPD 分层 tilemap 的渲染，验证「两格高的草」是怎么拼出来的。
读取 tiles_sewers.png / terrain_features.png，按 GameScene 的 z 序逐层合成，输出 ASCII（每 1px = 1 字符）。
"""
import zlib, struct

SHEET = 'core/src/main/assets/environment/tiles_sewers.png'
FEAT = 'core/src/main/assets/environment/terrain_features.png'

# ---- Terrain 常量 ----
CHASM, EMPTY, GRASS, EMPTY_WELL, WALL, DOOR, OPEN_DOOR = 0, 1, 2, 3, 4, 5, 6
EMBERS, LOCKED_DOOR, WALL_DECO, BARRICADE, EMPTY_SP, HIGH_GRASS = 9, 10, 12, 13, 14, 15
FURROWED_GRASS, BOOKSHELF, ALCHEMY, STATUE, STATUE_SP, WATER = 30, 27, 28, 25, 26, 29

def xy(x, y): return (x - 1) + 16 * (y - 1)

GROUND = xy(1, 1); CHASM_T = xy(9, 2); WATER_T = xy(1, 3)
FLAT_WALLS = xy(1, 4); FLAT_OTHER = xy(1, 5)
RAISED_WALLS = xy(1, 6); RAISED_OTHER = xy(9, 8)
WALLS_INTERNAL = xy(1, 10); WALLS_OVERHANG = xy(1, 13); OTHER_OVERHANG = xy(9, 15)
FLOOR = GROUND + 0; FLOOR_DECO = GROUND + 1; GRASS_T = GROUND + 2; FLOOR_SP = GROUND + 4
FLAT_HIGH_GRASS = FLAT_OTHER + 2
RAISED_WALL = RAISED_WALLS + 0; RAISED_WALL_DECO = RAISED_WALLS + 4; RAISED_WALL_DOOR = RAISED_WALLS + 8; RAISED_WALL_BOOKSHELF = RAISED_WALLS + 12
RAISED_HIGH_GRASS = RAISED_OTHER + 2; RAISED_FURROWED_GRASS = RAISED_OTHER + 3
WALL_INTERNAL = WALLS_INTERNAL + 0; WALL_INTERNAL_DECO = WALLS_INTERNAL + 16; WALL_INTERNAL_WOODEN = WALLS_INTERNAL + 32
WALL_OVERHANG = WALLS_OVERHANG + 0; WALL_OVERHANG_DECO = WALLS_OVERHANG + 4; WALL_OVERHANG_WOODEN = WALLS_OVERHANG + 8
HIGH_GRASS_OVERHANG = OTHER_OVERHANG + 2; HIGH_GRASS_UNDERHANG = OTHER_OVERHANG + 18
WALL_STITCH = (WALL, WALL_DECO, 38, 11, 40, 41, BOOKSHELF, -1)

def wall_stitchable(t): return t in WALL_STITCH

# ---- PNG 解码 ----
def load_png(path):
    d = open(path, 'rb').read(); pos = 8; idat = b''; plte = None; trns = None
    while pos < len(d):
        ln = struct.unpack('>I', d[pos:pos+4])[0]; typ = d[pos+4:pos+8]; data = d[pos+8:pos+8+ln]
        if typ == b'IHDR': w, h, bitd, ctype = struct.unpack('>IIBB', data[:10])
        elif typ == b'PLTE': plte = data
        elif typ == b'tRNS': trns = data
        elif typ == b'IDAT': idat += data
        elif typ == b'IEND': break
        pos += 12 + ln
    assert bitd == 8, 'only 8-bit supported, got %d' % bitd
    nch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]; raw = zlib.decompress(idat); stride = w * nch
    out = bytearray(h * stride); prev = bytearray(stride); p = 0
    for y in range(h):
        f = raw[p]; p += 1; line = bytearray(raw[p:p+stride]); p += stride
        if f == 1:
            for i in range(nch, stride): line[i] = (line[i] + line[i-nch]) & 255
        elif f == 2:
            for i in range(stride): line[i] = (line[i] + prev[i]) & 255
        elif f == 3:
            for i in range(stride):
                a = line[i-nch] if i >= nch else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
        elif f == 4:
            for i in range(stride):
                a = line[i-nch] if i >= nch else 0; b = prev[i]; c = prev[i-nch] if i >= nch else 0
                pp = a + b - c; pa, pb, pc = abs(pp-a), abs(pp-b), abs(pp-c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        out[y*stride:(y+1)*stride] = line; prev = line
    if ctype == 3:  # 调色板：展开成 RGBA
        assert plte is not None
        exp = bytearray(w*h*4)
        for i in range(w*h):
            idx = out[i]
            exp[i*4]   = plte[idx*3]
            exp[i*4+1] = plte[idx*3+1]
            exp[i*4+2] = plte[idx*3+2]
            exp[i*4+3] = trns[idx] if (trns is not None and idx < len(trns)) else 255
        return w, h, 4, exp
    return w, h, nch, out

class Sheet:
    def __init__(self, path):
        self.w, self.h, self.nch, self.buf = load_png(path)
        self.cols = self.w // 16
    def frame(self, idx):
        return self.frame_sz(idx, 16, 16)
    def frame_sz(self, idx, cw, ch):
        if idx is None or idx < 0: return None
        cols = self.w // cw
        cx, cy = idx % cols, idx // cols
        if (cy + 1) * ch > self.h: return None
        f = []
        for y in range(ch):
            row = []
            for x in range(cw):
                o = ((cy*ch + y) * self.w + cx*cw + x) * self.nch
                b = self.buf
                if self.nch == 4: row.append((b[o], b[o+1], b[o+2], b[o+3]))
                else: row.append((b[o], b[o+1], b[o+2], 255))
            f.append(row)
        return f

tiles = Sheet(SHEET); feats = Sheet(FEAT)

# ---- 复刻 getTileVisual ----
def terrain_visual(m, W, pos, tile):
    """DungeonTerrainTilemap.getTileVisual(pos,tile,flat=false)"""
    if tile == EMPTY: return FLOOR
    if tile == GRASS: return GRASS_T
    if tile == WATER: return WATER_T
    if tile == CHASM: return CHASM_T
    if wall_stitchable(tile):
        right = m[pos+1] if (pos+1) % W != 0 else -1
        below = m[pos+W] if pos + W < len(m) else -1
        left = m[pos-1] if pos % W != 0 else -1
        if below == -1: return None
        if wall_stitchable(below): return None
        base = RAISED_WALL
        if tile == WALL_DECO: base = RAISED_WALL_DECO
        elif tile == BOOKSHELF: base = RAISED_WALL_BOOKSHELF
        if not wall_stitchable(right): base += 1
        if not wall_stitchable(left): base += 2
        return base
    if tile == STATUE: return RAISED_OTHER + 8
    if tile == HIGH_GRASS: return RAISED_HIGH_GRASS
    if tile == FURROWED_GRASS: return RAISED_FURROWED_GRASS
    if tile == ALCHEMY: return RAISED_OTHER + 0
    if tile == BARRICADE: return RAISED_OTHER + 1
    return -1

def raised_visual(m, W, pos, tile):
    """RaisedTerrainTilemap.getTileVisual"""
    if tile == HIGH_GRASS: return HIGH_GRASS_UNDERHANG
    if tile == FURROWED_GRASS: return OTHER_OVERHANG + 19
    return -1

def walls_visual(m, W, pos, tile):
    """DungeonWallsTilemap.getTileVisual"""
    if wall_stitchable(tile):
        if pos + W < len(m) and not wall_stitchable(m[pos+W]):
            pass  # 露底的那面：本格不再画，交给 terrain 层的 RAISED_*
        else:
            if tile == BOOKSHELF: return WALL_INTERNAL_WOODEN
            return WALL_INTERNAL
    below = m[pos+W] if pos + W < len(m) else -1
    right_below = m[pos+1+W] if (pos+1) % W != 0 and pos+1+W < len(m) else -1
    left_below = m[pos-1+W] if pos % W != 0 and pos-1+W < len(m) else -1
    if below == HIGH_GRASS: return HIGH_GRASS_OVERHANG
    if below == FURROWED_GRASS: return OTHER_OVERHANG + 3
    if wall_stitchable(below):
        base = WALL_OVERHANG
        if below == WALL_DECO: base = WALL_OVERHANG_DECO
        elif below == BOOKSHELF: base = WALL_OVERHANG_WOODEN
        if not wall_stitchable(right_below): base += 1
        if not wall_stitchable(left_below): base += 2
        return base
    return -1

def features_visual(m, W, pos, tile):
    if tile == HIGH_GRASS: return 9
    if tile == FURROWED_GRASS: return 11
    if tile == GRASS: return 13
    return -1

# ---- 合成 ----
W, H = 3, 5
m = [EMPTY]*15
m[1*W+1] = HIGH_GRASS      # (x=1,y=1) 一格高草（经典高草）
m[3*W+1] = WALL            # (x=1,y=3) 一面墙

CANVAS_W, CANVAS_H = W*16, H*16
def blend(canvas, frame, ox, oy):
    if frame is None: return
    for y in range(16):
        for x in range(16):
            r, g, b, a = frame[y][x]
            if a == 0: continue
            cy, cx = oy + y, ox + x
            if 0 <= cy < CANVAS_H and 0 <= cx < CANVAS_W:
                dr, dg, db, da = canvas[cy][cx]
                al = a / 255.0; na = al + da/255.0*(1-al)
                canvas[cy][cx] = (int(r*al + dr*(1-al)*da/255.0), int(g*al + dg*(1-al)*da/255.0),
                                  int(b*al + db*(1-al)*da/255.0), int(na*255))

def new_canvas(): return [[(0, 0, 0, 0) for _ in range(CANVAS_W)] for _ in range(CANVAS_H)]

def draw_all(hero_cell=None):
    c = new_canvas()
    for p in range(W*H):   # 1. terrain
        blend(c, tiles.frame(terrain_visual(m, W, p, m[p])), (p % W)*16, (p//W)*16)
    for p in range(W*H):   # 2. terrain features（草叶/地板细节）
        blend(c, feats.frame(features_visual(m, W, p, m[p])), (p % W)*16, (p//W)*16)
    if hero_cell is not None:  # 3. mobs（角色，12x15，底对齐格底）
        hx, hy = hero_cell
        for y in range(15):
            for x in range(12):
                cx, cy = hx*16 + 2 + x, hy*16 + 1 + y
                c[cy][cx] = (110, 110, 110, 255)
    for p in range(W*H):   # 4. raisedTerrain
        blend(c, tiles.frame(raised_visual(m, W, p, m[p])), (p % W)*16, (p//W)*16)
    for p in range(W*H):   # 5. walls
        blend(c, tiles.frame(walls_visual(m, W, p, m[p])), (p % W)*16, (p//W)*16)
    return c

CH = ' .:-=+*#%@'
def show(canvas, title, x0, x1, y0, y1):
    print('\n### %s  (x %d..%d, y %d..%d; 每字符=1px, 一格=16px)' % (title, x0, x1, y0, y1))
    for y in range(y0, y1):
        row = ''
        for x in range(x0, x1):
            r, g, b, a = canvas[y][x]
            if a < 24: row += ' '
            else:
                lum = (r*299 + g*587 + b*114)//1000
                row += CH[min(9, max(1, (255-lum)*10//256))]
        print('%3d|%s|' % (y, row))

if __name__ == '__main__':
    # 只导出 x=1 那一格（16px 宽），上面多带半格
    show(draw_all(), '草格无角色：地形+特征+草叶（+骨架/内壁）', 16, 32, 24, 48)
    show(draw_all(hero_cell=(1, 0)), '角色站在草格「上方那格」(1,0)：被草叶盖住下半身', 16, 32, 0, 48)
    show(draw_all(hero_cell=(1, 1)), '角色站在草格 (1,1) 上：草皮在身下、草叶盖腿', 16, 32, 16, 48)
