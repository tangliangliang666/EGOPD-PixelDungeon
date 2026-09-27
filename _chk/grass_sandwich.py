# -*- coding: utf-8 -*-
"""具体实例：角色站在「上下都是高草」的地块中，逐图层拆解 + 真实贴图渲染。

复刻 DungeonTerrainTilemap / TerrainFeaturesTilemap / RaisedTerrainTilemap /
DungeonWallsTilemap 四个 getTileVisual，对英雄所在格及其上下邻格打印
「哪一层 → 哪一帧 → 图集第几行第几列 → 哪个 png → 帧内不透明行」，
并按 GameScene 的 z 序渲染分层面板 PNG + 遮挡掩码。

角色落点复刻 CharSprite.worldToCamera：x = (col+0.5)*16 - 12/2
                                        y = (row+1)*16 - 15 - 16*perspectiveRaise(=6/16)

输出: _chk/grass_sandwich.png       场景 A：英雄站在高草上，上/下邻格也是高草
      _chk/grass_sandwich_floor.png 场景 B（对照）：英雄站在普通地面，上/下邻格是高草
"""
import zlib, struct, os
from tilemap_sim import Sheet

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
TILES_PNG = os.path.join(ROOT, 'core/src/main/assets/environment/tiles_sewers.png')
FEAT_PNG  = os.path.join(ROOT, 'core/src/main/assets/environment/terrain_features.png')
WAR_PNG   = os.path.join(ROOT, 'core/src/main/assets/sprites/warrior.png')

# ---------------- Terrain 常量（levels/Terrain.java） ----------------
EMPTY, WALL, HIGH_GRASS, FURROWED_GRASS, GRASS = 1, 4, 15, 30, 2

def xy(x, y): return (x - 1) + 16 * (y - 1)
def idx2xy(i): return (i % 16 + 1, i // 16 + 1)
def xyname(i):
    c, r = idx2xy(i); return 'xy(%d,%d)' % (c, r)

# ---------------- 帧常量（tiles/DungeonTileSheet.java） ----------------
GROUND = xy(1, 1); FLAT_OTHER = xy(1, 5)
RAISED_OTHER = xy(9, 8); OTHER_OVERHANG = xy(9, 15)

FLOOR                 = GROUND + 0            # 0
FLOOR_ALT_1           = GROUND + 6            # 6
FLOOR_ALT_2           = GROUND + 12           # 12
GRASS_T               = GROUND + 2            # 2
GRASS_ALT_T           = GROUND + 8            # 8
RAISED_HIGH_GRASS     = RAISED_OTHER + 2      # 122
RAISED_HIGH_GRASS_ALT = RAISED_OTHER + 5      # 125
HIGH_GRASS_OVERHANG     = OTHER_OVERHANG + 2   # 234
HIGH_GRASS_OVERHANG_ALT = OTHER_OVERHANG + 5   # 237
HIGH_GRASS_UNDERHANG     = OTHER_OVERHANG + 18 # 250
HIGH_GRASS_UNDERHANG_ALT = OTHER_OVERHANG + 21 # 253

COMMON_ALT = {FLOOR: FLOOR_ALT_1, GRASS_T: GRASS_ALT_T,
              RAISED_HIGH_GRASS: RAISED_HIGH_GRASS_ALT,
              HIGH_GRASS_OVERHANG: HIGH_GRASS_OVERHANG_ALT,
              HIGH_GRASS_UNDERHANG: HIGH_GRASS_UNDERHANG_ALT}
RARE_ALT = {FLOOR: FLOOR_ALT_2}

def with_alts(visual, pos, var):
    if var[pos] >= 95 and visual in RARE_ALT:   return RARE_ALT[visual]
    if var[pos] >= 50 and visual in COMMON_ALT: return COMMON_ALT[visual]
    return visual

# ---------------- 四层 getTileVisual 复刻 ----------------
def terrain_visual(m, W, pos, var):
    t = m[pos]
    if t == EMPTY: return with_alts(FLOOR, pos, var)
    if t == GRASS: return with_alts(GRASS_T, pos, var)
    if t == HIGH_GRASS: return with_alts(RAISED_HIGH_GRASS, pos, var)
    return -1

def features_visual(m, W, pos, var, stage=0):
    t = m[pos]
    if t == HIGH_GRASS:     return 9  + 16*stage + (1 if var[pos] >= 50 else 0)
    if t == FURROWED_GRASS: return 11 + 16*stage + (1 if var[pos] >= 50 else 0)
    if t == GRASS:          return 13 + 16*stage + (1 if var[pos] >= 50 else 0)
    return -1

def raised_visual(m, W, pos, var):
    if m[pos] == HIGH_GRASS: return with_alts(HIGH_GRASS_UNDERHANG, pos, var)
    return -1

def walls_visual(m, W, pos, var):
    # 注意：种子用「下方草格」自己的 pos，所以同一丛草在「本格」和「上一格」
    # 拿到的 ALT 变体是一致的（上下两块拼起来才不出现错版）
    if pos + W < len(m) and m[pos + W] == HIGH_GRASS:
        return with_alts(HIGH_GRASS_OVERHANG, pos + W, var)
    return -1

# ---------------- 渲染工具 ----------------
tiles = Sheet(TILES_PNG); feats = Sheet(FEAT_PNG); war = Sheet(WAR_PNG)

def rows_of(frame):
    if frame is None: return '（空）'
    used = [y for y in range(len(frame)) if any(p[3] > 0 for p in frame[y])]
    if not used: return '（整帧透明）'
    out, s, p = [], used[0], used[0]
    for y in used[1:]:
        if y == p + 1: p = y
        else: out.append((s, p)); s = p = y
    out.append((s, p))
    return '、'.join(('%d' % a) if a == b else ('%d~%d' % (a, b)) for a, b in out)

def write_png(path, canvas):
    h = len(canvas); w = len(canvas[0]); raw = bytearray()
    for row in canvas:
        raw.append(0)
        for (r, g, b, a) in row: raw += bytes((r, g, b, a))
    def ch(t, d):
        c = struct.pack('>I', len(d)) + t + d
        return c + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    png = b'\x89PNG\r\n\x1a\n'
    png += ch(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
    png += ch(b'IDAT', zlib.compress(bytes(raw), 9)) + ch(b'IEND', b'')
    open(path, 'wb').write(png)

def run(on_grass, out_name, title):
    W, H = 5, 7
    m = [EMPTY] * (W * H)
    for y in range(1, 6):
        m[y * W + 2] = HIGH_GRASS
    HERO = (2, 3)
    if not on_grass:
        m[HERO[1] * W + HERO[0]] = EMPTY
    var = [10] * (W * H)          # 全 <50：统一用非 ALT 帧，便于逐帧对照

    print('\n' + '=' * 76)
    print(title)
    print('地图 %dx%d；x=2 的 y=1..5 为 HIGH_GRASS(15)；英雄在 (2,3)。' % (W, H))
    print('注：地图索引 +W = 屏幕下一行 = 更靠近镜头（南）。')
    print('=' * 76)

    def table(pos, label):
        c, r = pos % W, pos // W
        print('\n【格子 (x=%d, y=%d)】%s   Terrain=%d  tileVariance=%d'
              % (c, r, label, m[pos], var[pos]))
        rows = [
            ('1 地形  DungeonTerrainTilemap', 'tiles_sewers.png', '本格', terrain_visual),
            ('2 特征  TerrainFeaturesTilemap', 'terrain_features.png', '本格', features_visual),
            ('3 角色  HeroSprite（mobs 组）', 'sprites/warrior.png', '—', None),
            ('4 下悬  RaisedTerrainTilemap', 'tiles_sewers.png', '本格', raised_visual),
            ('5 草檐  DungeonWallsTilemap', 'tiles_sewers.png', '下方格', walls_visual),
        ]
        for name, png, src, fn in rows:
            if fn is None:
                y0 = r * 16 + 1 - 6
                print('   %-32s → frame 0（12x15 帧）  画在 world y=%d~%d（格内 y=%d~%d）'
                      % (name, y0, y0 + 14, -5, 9))
                continue
            v = fn(m, W, pos, var)
            if v is None or v < 0:
                print('   %-32s → 返回 -1，本格不画' % name)
                continue
            sh = tiles if 'tiles_sewers' in png else feats
            alt = COMMON_ALT.get(v, -1)
            altxt = '' if alt < 0 else '   ALT→%d=%s' % (alt, xyname(alt))
            print('   %-32s → frame %3d = %-10s 自 %-22s 取；帧内不透明行 %s%s'
                  % (name, v, xyname(v), png, rows_of(sh.frame(v)), altxt))

    table(HERO[1] * W + HERO[0], '★ 英雄所在格')
    table((HERO[1] - 1) * W + HERO[0], '英雄上方一格（离镜头更远）')
    table((HERO[1] + 1) * W + HERO[0], '英雄下方一格（离镜头更近）')

    CW, CH = W * 16, H * 16
    def new_canvas(): return [[(0, 0, 0, 0) for _ in range(CW)] for _ in range(CH)]
    def blend(c, frame, ox, oy):
        if frame is None: return
        for y in range(len(frame)):
            for x in range(len(frame[0])):
                r, g, b, a = frame[y][x]
                if a == 0: continue
                cy, cx = oy + y, ox + x
                if 0 <= cy < CH and 0 <= cx < CW:
                    dr, dg, db, da = c[cy][cx]
                    al = a / 255.0; na = al + da / 255.0 * (1 - al)
                    c[cy][cx] = (int(r*al + dr*(1-al)*da/255.0), int(g*al + dg*(1-al)*da/255.0),
                                 int(b*al + db*(1-al)*da/255.0), int(na*255))
    HY = HERO[1] * 16 + 1 - 6          # 角色贴图左上角 world y
    HX = HERO[0] * 16 + 2
    HF = war.frame_sz(0, 12, 15)

    def paint(c, upto, magenta=False):
        if upto >= 1:
            for p in range(W*H): blend(c, tiles.frame(terrain_visual(m, W, p, var)), (p % W)*16, (p//W)*16)
        if upto >= 2:
            for p in range(W*H): blend(c, feats.frame(features_visual(m, W, p, var)), (p % W)*16, (p//W)*16)
        if upto >= 3:
            if magenta:
                for y in range(15):
                    for x in range(12):
                        if HF[y][x][3] > 0: c[HY + y][HX + x] = (255, 0, 255, 255)
            else:
                blend(c, HF, HX, HY)
        if upto >= 4:
            for p in range(W*H): blend(c, tiles.frame(raised_visual(m, W, p, var)), (p % W)*16, (p//W)*16)
        if upto >= 5:
            for p in range(W*H): blend(c, tiles.frame(walls_visual(m, W, p, var)), (p % W)*16, (p//W)*16)
        return c

    panels = [paint(new_canvas(), i) for i in range(1, 6)]
    mask = paint(paint(new_canvas(), 2), 5, magenta=True)
    hero_px = set()
    for y in range(15):
        for x in range(12):
            if HF[y][x][3] > 0: hero_px.add((HX + x, HY + y))
    covered = sum(1 for (cx, cy) in hero_px if mask[cy][cx] != (255, 0, 255, 255))
    print('\n英雄贴图不透明像素 %d 个，被第 4/5 层盖掉 %d 个（%.0f%%）'
          % (len(hero_px), covered, 100.0 * covered / len(hero_px)))
    # 分方向统计
    up = sum(1 for (cx, cy) in hero_px if cy < HERO[1]*16 and mask[cy][cx] != (255,0,255,255))
    dn = covered - up
    print('  其中：位于「本格顶边以上」的像素被盖 %d 个（来自下方草格的草檐）；格内被盖 %d 个（本格下悬）' % (up, dn))
    line = []
    for y in range(15):
        n = sum(1 for x in range(12) if HF[y][x][3] > 0)
        if n == 0: continue
        cv = sum(1 for x in range(12) if HF[y][x][3] > 0 and mask[HY+y][HX+x] != (255, 0, 255, 255))
        line.append('r%d(%d):%d/%d' % (y, HY + y - HERO[1]*16, cv, n))
    print('  角色贴图逐行遮挡（r=贴图行号，( )=所在格内 y）：\n    ' + '  '.join(line))

    panels.append(mask)

    # 差异图：绿=角色仍可见的像素，红=被第4/5层盖住的像素，深灰=背景
    diff = [[(40, 40, 48, 255)] * CW for _ in range(CH)]
    for y in range(15):
        for x in range(12):
            if HF[y][x][3] > 0:
                px = mask[HY + y][HX + x]
                diff[HY + y][HX + x] = (255, 70, 70, 255) if px != (255, 0, 255, 255) \
                                       else (60, 255, 60, 255)

    # ---- 拼图：上排总览 6 面板 2x，下排放大 3 面板 6x（带格子边界线） ----
    S, GAP = 2, 8
    PW, PH = CW * S, CH * S
    COLS, ROWS = 3, 2
    Z, ZGAP = 6, 8
    cx0, cx1, cy0, cy1 = 24, 64, 24, 88      # 格 (2,2)~(2,4) 那一列，左右各多半格
    ZW, ZH = (cx1 - cx0) * Z, (cy1 - cy0) * Z
    GRID = (250, 220, 90, 255)               # 格子边界线（1px）

    base = paint(new_canvas(), 3)            # 背景 + 角色（还没有草盖上来）
    ZOOM = [base, panels[4], diff]

    top_w = COLS * PW + (COLS + 1) * GAP
    top_h = ROWS * PH + (ROWS + 1) * GAP
    bot_w = len(ZOOM) * ZW + (len(ZOOM) + 1) * ZGAP
    outw = max(top_w, bot_w)
    outh = top_h + ZH + 3 * GAP
    BGC = (20, 20, 24, 255)
    out = [[BGC] * outw for _ in range(outh)]
    for i, p in enumerate(panels):
        ox = GAP + (i % COLS) * (PW + GAP)
        oy = GAP + (i // COLS) * (PH + GAP)
        for y in range(CH):
            for x in range(CW):
                px = p[y][x]
                if px[3] == 0: px = BGC
                for dy in range(S):
                    for dx in range(S): out[oy + y*S + dy][ox + x*S + dx] = px
    for k, p in enumerate(ZOOM):
        ox = ZGAP + k * (ZW + ZGAP)
        oy = top_h + 2 * GAP
        for y in range(cy0, cy1):
            for x in range(cx0, cx1):
                px = p[y][x]
                if px[3] == 0: px = BGC
                gx = (x % 16 == 0) or (y % 16 == 0)
                if gx: px = GRID
                for dy in range(Z):
                    for dx in range(Z):
                        out[oy + (y-cy0)*Z + dy][ox + (x-cx0)*Z + dx] = px
    write_png(os.path.join(HERE, out_name), out)
    print('wrote _chk/%s  %dx%d' % (out_name, outw, outh))
    print('上排 6 面板: 1地形 / 2+特征 / 3+角色 / 4+下悬 / 5+草檐=最终 / 6遮挡掩码')
    print('下排放大 3 面板: 左=背景+角色(层1~3) / 中=最终(层1~5) / 右=差异图(绿=可见,红=被草盖)')
    print('下排黄线 = 格子边界（每 16px 一格）')

run(True,  'grass_sandwich.png',       '场景 A：英雄站在高草上，其上一格、下一格也都是高草')
run(False, 'grass_sandwich_floor.png', '场景 B（对照）：英雄站在普通地面，上一格、下一格是高草')
