# -*- coding: utf-8 -*-
"""
地形编辑器 · 渲染帧号交叉核验（2026-09-19）

目的：编辑器里的 JS 渲染引擎（tools/terrain-editor/render.js）是**手抄** Java 的
`DungeonTileSheet` / 四个 Tilemap 的帧号与缝合算法的。手抄必有笔误风险，而笔误
表现为「某块贴图不对」——肉眼很难发现（尤其是墙檐/内壁这类只在特定邻接才出现的帧）。

本脚本用**纯 Python 独立重写**同一套算法（不读 JS），然后在若干张构造地图上
逐格比对 terrain / raised / walls / features 四层的帧号。同时做反例自测：
故意改坏一个常量，须能报出不一致，否则判脚本失效。

用法：
    python _chk/verify_terrain_editor_frames.py
"""
import json
import os
import re
import struct
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
NODE = os.path.join(os.environ.get('USERPROFILE', r'C:\Users\14675'),
                    '.workbuddy', 'binaries', 'node', 'versions', '22.22.2-2', 'node.exe')
if not os.path.exists(NODE):
    NODE = 'node'

# ---------------------------------------------------------------- Python 侧实现
# 与 Java 源码逐条对应（行号见 tools/terrain-editor/render.js 的注释）
WIDTH = 16
def xy(x, y): return (x - 1) + WIDTH * (y - 1)

T = dict(
    CHASM=0, EMPTY=1, GRASS=2, EMPTY_WELL=3, WALL=4, DOOR=5, OPEN_DOOR=6,
    ENTRANCE=7, EXIT=8, EMBERS=9, LOCKED_DOOR=10, PEDESTAL=11, WALL_DECO=12,
    BARRICADE=13, EMPTY_SP=14, HIGH_GRASS=15, SECRET_DOOR=16, SECRET_TRAP=17,
    TRAP=18, INACTIVE_TRAP=19, EMPTY_DECO=20, LOCKED_EXIT=21, UNLOCKED_EXIT=22,
    CUSTOM_DECO=23, WELL=24, STATUE=25, STATUE_SP=26, BOOKSHELF=27, ALCHEMY=28,
    WATER=29, FURROWED_GRASS=30, CRYSTAL_DOOR=31, CUSTOM_DECO_EMPTY=32,
    REGION_DECO=33, REGION_DECO_ALT=34, MINE_CRYSTAL=35, MINE_BOULDER=36,
    ENTRANCE_SP=37, HERO_LKD_DR=38, MINE_DIAMOND=39)

GROUND = xy(1, 1); CHASM_T = xy(9, 2); WATER_T = xy(1, 3)
FLAT_WALLS = xy(1, 4); FLAT_OTHER = xy(1, 5)
RAISED_WALLS = xy(1, 6); RAISED_DOORS = xy(1, 8); RAISED_OTHER = xy(9, 8)
WALLS_INTERNAL = xy(1, 10); WALLS_OVERHANG = xy(1, 13)
DOOR_OVERHANG = xy(1, 15); OTHER_OVERHANG = xy(9, 15)

F = dict(
    FLOOR=GROUND + 0, FLOOR_DECO=GROUND + 1, GRASS=GROUND + 2, EMBERS=GROUND + 3,
    FLOOR_SP=GROUND + 4, FLOOR_ALT_1=GROUND + 6, FLOOR_DECO_ALT=GROUND + 7,
    GRASS_ALT=GROUND + 8, EMBERS_ALT=GROUND + 9, FLOOR_SP_ALT=GROUND + 10,
    FLOOR_ALT_2=GROUND + 12, ENTRANCE=GROUND + 16, EXIT=GROUND + 17,
    WELL=GROUND + 18, EMPTY_WELL=GROUND + 19, PEDESTAL=GROUND + 20,
    ENTRANCE_SP=GROUND + 22,
    CHASM=CHASM_T, CHASM_FLOOR=CHASM_T + 1, CHASM_FLOOR_SP=CHASM_T + 2,
    CHASM_WALL=CHASM_T + 3, CHASM_WATER=CHASM_T + 4, WATER=WATER_T,
    FLAT_WALL=FLAT_WALLS + 0, FLAT_WALL_DECO=FLAT_WALLS + 1,
    FLAT_BOOKSHELF=FLAT_WALLS + 2, FLAT_WALL_ALT=FLAT_WALLS + 4,
    FLAT_WALL_DECO_ALT=FLAT_WALLS + 5, FLAT_BOOKSHELF_ALT=FLAT_WALLS + 6,
    FLAT_DOOR=FLAT_WALLS + 8, FLAT_DOOR_OPEN=FLAT_WALLS + 9,
    FLAT_DOOR_LOCKED=FLAT_WALLS + 10, FLAT_DOOR_CRYSTAL=FLAT_WALLS + 11,
    UNLOCKED_EXIT=FLAT_WALLS + 12, LOCKED_EXIT=FLAT_WALLS + 13,
    FLAT_ALCHEMY_POT=FLAT_OTHER + 0, FLAT_BARRICADE=FLAT_OTHER + 1,
    FLAT_HIGH_GRASS=FLAT_OTHER + 2, FLAT_FURROWED_GRASS=FLAT_OTHER + 3,
    FLAT_HIGH_GRASS_ALT=FLAT_OTHER + 5, FLAT_FURROWED_ALT=FLAT_OTHER + 6,
    FLAT_STATUE=FLAT_OTHER + 8, FLAT_STATUE_SP=FLAT_OTHER + 9,
    FLAT_REGION_DECO=FLAT_OTHER + 10, FLAT_REGION_DECO_ALT=FLAT_OTHER + 11,
    FLAT_MINE_CRYSTAL=FLAT_OTHER + 12, FLAT_MINE_DIAMOND=256,
    RAISED_WALL=RAISED_WALLS + 0, RAISED_WALL_DECO=RAISED_WALLS + 4,
    RAISED_WALL_DOOR=RAISED_WALLS + 8, RAISED_WALL_BOOKSHELF=RAISED_WALLS + 12,
    RAISED_WALL_ALT=RAISED_WALLS + 16, RAISED_WALL_DECO_ALT=RAISED_WALLS + 20,
    RAISED_WALL_BOOKSHELF_ALT=RAISED_WALLS + 28, RAISED_MINE_DIAMOND=258,
    RAISED_DOOR=RAISED_DOORS + 0, RAISED_DOOR_OPEN=RAISED_DOORS + 1,
    RAISED_DOOR_LOCKED=RAISED_DOORS + 2, RAISED_DOOR_CRYSTAL=RAISED_DOORS + 3,
    RAISED_DOOR_SIDEWAYS=RAISED_DOORS + 4,
    RAISED_ALCHEMY_POT=RAISED_OTHER + 0, RAISED_BARRICADE=RAISED_OTHER + 1,
    RAISED_HIGH_GRASS=RAISED_OTHER + 2, RAISED_FURROWED_GRASS=RAISED_OTHER + 3,
    RAISED_HIGH_GRASS_ALT=RAISED_OTHER + 5, RAISED_FURROWED_ALT=RAISED_OTHER + 6,
    RAISED_STATUE=RAISED_OTHER + 8, RAISED_STATUE_SP=RAISED_OTHER + 9,
    RAISED_REGION_DECO=RAISED_OTHER + 10, RAISED_REGION_DECO_ALT=RAISED_OTHER + 11,
    RAISED_MINE_CRYSTAL=RAISED_OTHER + 12,
    WALL_INTERNAL=WALLS_INTERNAL + 0, WALL_INTERNAL_DECO=WALLS_INTERNAL + 16,
    WALL_INTERNAL_WOODEN=WALLS_INTERNAL + 32, WALL_INTERNAL_MINE_DIAMOND=272,
    WALL_OVERHANG=WALLS_OVERHANG + 0, WALL_OVERHANG_DECO=WALLS_OVERHANG + 4,
    WALL_OVERHANG_WOODEN=WALLS_OVERHANG + 8, WALL_OVERHANG_MINE_DIAMOND=266,
    DOOR_SIDEWAYS_OVERHANG=WALLS_OVERHANG + 16,
    DOOR_SIDEWAYS_OVERHANG_CLOSED=WALLS_OVERHANG + 20,
    DOOR_SIDEWAYS_OVERHANG_LOCKED=WALLS_OVERHANG + 24,
    DOOR_SIDEWAYS_OVERHANG_CRYSTAL=WALLS_OVERHANG + 28,
    DOOR_OVERHANG=DOOR_OVERHANG + 0, DOOR_OVERHANG_OPEN=DOOR_OVERHANG + 1,
    DOOR_OVERHANG_CRYSTAL=DOOR_OVERHANG + 2, DOOR_SIDEWAYS=DOOR_OVERHANG + 3,
    DOOR_SIDEWAYS_LOCKED=DOOR_OVERHANG + 4, DOOR_SIDEWAYS_CRYSTAL=DOOR_OVERHANG + 5,
    EXIT_UNDERHANG=DOOR_OVERHANG + 6,
    ALCHEMY_POT_OVERHANG=OTHER_OVERHANG + 0, BARRICADE_OVERHANG=OTHER_OVERHANG + 1,
    HIGH_GRASS_OVERHANG=OTHER_OVERHANG + 2, FURROWED_OVERHANG=OTHER_OVERHANG + 3,
    HIGH_GRASS_OVERHANG_ALT=OTHER_OVERHANG + 5, FURROWED_OVERHANG_ALT=OTHER_OVERHANG + 6,
    STATUE_OVERHANG=OTHER_OVERHANG + 8, STATUE_SP_OVERHANG=OTHER_OVERHANG + 9,
    REGION_DECO_OVERHANG=OTHER_OVERHANG + 10, REGION_DECO_ALT_OVERHANG=OTHER_OVERHANG + 11,
    MINE_CRYSTAL_OVERHANG=OTHER_OVERHANG + 12,
    HIGH_GRASS_UNDERHANG=OTHER_OVERHANG + 18, FURROWED_UNDERHANG=OTHER_OVERHANG + 19,
    HIGH_GRASS_UNDERHANG_ALT=OTHER_OVERHANG + 21, FURROWED_UNDERHANG_ALT=OTHER_OVERHANG + 22)

WALL_STITCH = [T['WALL'], T['WALL_DECO'], T['MINE_DIAMOND'], T['SECRET_DOOR'],
               T['LOCKED_EXIT'], T['UNLOCKED_EXIT'], T['BOOKSHELF'], -1]
def wall_stitchable(t): return t in WALL_STITCH

DOOR_TILES = [T['DOOR'], T['LOCKED_DOOR'], T['HERO_LKD_DR'], T['CRYSTAL_DOOR'], T['OPEN_DOOR']]
def door_tile(t): return t in DOOR_TILES

DIRECT = {T['EMPTY']: F['FLOOR'], T['GRASS']: F['GRASS'], T['EMPTY_WELL']: F['EMPTY_WELL'],
          T['ENTRANCE']: F['ENTRANCE'], T['EXIT']: F['EXIT'], T['EMBERS']: F['EMBERS'],
          T['PEDESTAL']: F['PEDESTAL'], T['EMPTY_SP']: F['FLOOR_SP'],
          T['ENTRANCE_SP']: F['ENTRANCE_SP'], T['SECRET_TRAP']: F['FLOOR'],
          T['TRAP']: F['FLOOR'], T['INACTIVE_TRAP']: F['FLOOR'],
          T['CUSTOM_DECO']: F['FLOOR'], T['CUSTOM_DECO_EMPTY']: F['FLOOR'],
          T['EMPTY_DECO']: F['FLOOR_DECO'], T['LOCKED_EXIT']: F['LOCKED_EXIT'],
          T['UNLOCKED_EXIT']: F['UNLOCKED_EXIT'], T['WELL']: F['WELL']}

COMMON_ALT = {F['FLOOR']: F['FLOOR_ALT_1'], F['GRASS']: F['GRASS_ALT'],
              F['FLAT_WALL']: F['FLAT_WALL_ALT'], F['EMBERS']: F['EMBERS_ALT'],
              F['FLOOR_SP']: F['FLOOR_SP_ALT'], F['FLOOR_DECO']: F['FLOOR_DECO_ALT'],
              F['RAISED_WALL']: F['RAISED_WALL_ALT'],
              F['RAISED_WALL_DECO']: F['RAISED_WALL_DECO_ALT'],
              F['RAISED_HIGH_GRASS']: F['RAISED_HIGH_GRASS_ALT'],
              F['RAISED_FURROWED_GRASS']: F['RAISED_FURROWED_ALT'],
              F['HIGH_GRASS_OVERHANG']: F['HIGH_GRASS_OVERHANG_ALT'],
              F['FURROWED_OVERHANG']: F['FURROWED_OVERHANG_ALT'],
              F['HIGH_GRASS_UNDERHANG']: F['HIGH_GRASS_UNDERHANG_ALT'],
              F['FURROWED_UNDERHANG']: F['FURROWED_UNDERHANG_ALT']}
RARE_ALT = {F['FLOOR']: F['FLOOR_ALT_2']}

WATER_STITCH = [T['EMPTY'], T['GRASS'], T['EMPTY_WELL'], T['ENTRANCE'], T['EXIT'],
                T['EMBERS'], T['BARRICADE'], T['HIGH_GRASS'], T['FURROWED_GRASS'],
                T['SECRET_TRAP'], T['TRAP'], T['INACTIVE_TRAP'], T['EMPTY_DECO'],
                T['CUSTOM_DECO'], T['WELL'], T['STATUE'], T['REGION_DECO'],
                T['ALCHEMY'], T['CUSTOM_DECO_EMPTY'], T['MINE_CRYSTAL'],
                T['MINE_BOULDER'], T['DOOR'], T['OPEN_DOOR'], T['LOCKED_DOOR'],
                T['HERO_LKD_DR'], T['CRYSTAL_DOOR']]
def water_stitchable(t, depth):
    if t == T['REGION_DECO_ALT']: return depth > 20
    return t in WATER_STITCH

CHASM_STITCH = {}
for _t in (T['EMPTY'], T['GRASS'], T['EMBERS'], T['EMPTY_WELL'], T['HIGH_GRASS'],
           T['FURROWED_GRASS'], T['EMPTY_DECO'], T['CUSTOM_DECO'], T['WELL'],
           T['STATUE'], T['REGION_DECO'], T['SECRET_TRAP'], T['INACTIVE_TRAP'],
           T['TRAP'], T['BOOKSHELF'], T['BARRICADE'], T['PEDESTAL'],
           T['CUSTOM_DECO_EMPTY'], T['MINE_BOULDER'], T['MINE_CRYSTAL']):
    CHASM_STITCH[_t] = F['CHASM_FLOOR']
CHASM_STITCH[T['EMPTY_SP']] = F['CHASM_FLOOR_SP']
CHASM_STITCH[T['STATUE_SP']] = F['CHASM_FLOOR_SP']
for _t in (T['WALL'], T['DOOR'], T['OPEN_DOOR'], T['LOCKED_DOOR'], T['HERO_LKD_DR'],
           T['SECRET_DOOR'], T['WALL_DECO']):
    CHASM_STITCH[_t] = F['CHASM_WALL']
CHASM_STITCH[T['WATER']] = F['CHASM_WATER']


class M:
    """一张地图 + 复刻的四层 getTileVisual"""
    def __init__(self, w, h, fill, depth=1, stage=0, seed=12345):
        self.w, self.h = w, h
        self.map = [fill] * (w * h)
        self.depth = depth
        self.stage = stage
        self.var = []
        s = seed & 0xFFFFFFFF
        for _ in range(w * h):
            s = (s * 1664525 + 1013904223) & 0xFFFFFFFF
            self.var.append((s >> 16) % 100)

    def at(self, x, y):
        if x < 0 or y < 0 or x >= self.w or y >= self.h: return -1
        return self.map[y * self.w + x]

    def with_alts(self, v, pos):
        if v is None or v < 0: return -1
        if self.var[pos] >= 95 and v in RARE_ALT: return RARE_ALT[v]
        if self.var[pos] >= 50 and v in COMMON_ALT: return COMMON_ALT[v]
        return v

    def terrain(self, pos):
        tile = self.map[pos]
        x, y = pos % self.w, pos // self.w
        if tile in DIRECT: return self.with_alts(DIRECT[tile], pos)
        if tile == T['WATER']:
            r = 0
            if water_stitchable(self.at(x, y - 1), self.depth): r += 1
            if water_stitchable(self.at(x + 1, y), self.depth): r += 2
            if water_stitchable(self.at(x, y + 1), self.depth): r += 4
            if water_stitchable(self.at(x - 1, y), self.depth): r += 8
            return F['WATER'] + r
        if tile == T['CHASM']:
            above = self.map[pos - self.w] if pos > self.w else -1
            if above == T['REGION_DECO_ALT']:
                if self.depth <= 5: return F['CHASM_FLOOR_SP']
                if self.depth <= 10: return F['CHASM']
                if self.depth <= 20: return F['CHASM_FLOOR_SP']
                return F['CHASM_FLOOR']
            return CHASM_STITCH.get(above, F['CHASM'])
        if door_tile(tile):
            return self.raised_door(tile, self.at(x, y - 1))
        if wall_stitchable(tile):
            return self.raised_wall(tile, pos, self.at(x + 1, y), self.at(x, y + 1), self.at(x - 1, y))
        simple = {T['STATUE']: F['RAISED_STATUE'], T['STATUE_SP']: F['RAISED_STATUE_SP'],
                  T['REGION_DECO']: F['RAISED_REGION_DECO'],
                  T['REGION_DECO_ALT']: F['RAISED_REGION_DECO_ALT'],
                  T['MINE_CRYSTAL']: F['RAISED_MINE_CRYSTAL'],
                  T['MINE_BOULDER']: F['RAISED_MINE_CRYSTAL'],
                  T['ALCHEMY']: F['RAISED_ALCHEMY_POT'],
                  T['BARRICADE']: F['RAISED_BARRICADE'],
                  T['HIGH_GRASS']: F['RAISED_HIGH_GRASS'],
                  T['FURROWED_GRASS']: F['RAISED_FURROWED_GRASS']}
        if tile in simple: return self.with_alts(simple[tile], pos)
        return -1

    def raised_door(self, tile, below):
        if wall_stitchable(below): return F['RAISED_DOOR_SIDEWAYS']
        return {T['DOOR']: F['RAISED_DOOR'], T['OPEN_DOOR']: F['RAISED_DOOR_OPEN'],
                T['LOCKED_DOOR']: F['RAISED_DOOR_LOCKED'],
                T['HERO_LKD_DR']: F['RAISED_DOOR_LOCKED'],
                T['CRYSTAL_DOOR']: F['RAISED_DOOR_CRYSTAL']}.get(tile, -1)

    def raised_wall(self, tile, pos, right, below, left):
        if below == -1 or wall_stitchable(below): return -1
        if door_tile(below): r = F['RAISED_WALL_DOOR']
        elif tile in (T['WALL'], T['SECRET_DOOR']): r = F['RAISED_WALL']
        elif tile == T['WALL_DECO']: r = F['RAISED_WALL_DECO']
        elif tile == T['MINE_DIAMOND']: r = F['RAISED_MINE_DIAMOND']
        elif tile == T['BOOKSHELF']: r = F['RAISED_WALL_BOOKSHELF']
        else: return -1
        r = self.with_alts(r, pos)
        if not wall_stitchable(right): r += 1
        if not wall_stitchable(left): r += 2
        return r

    def raised(self, pos):
        tile = self.map[pos]
        if tile == T['HIGH_GRASS']: return self.with_alts(F['HIGH_GRASS_UNDERHANG'], pos)
        if tile == T['FURROWED_GRASS']: return self.with_alts(F['FURROWED_UNDERHANG'], pos)
        return -1

    def walls(self, pos):
        tile = self.map[pos]
        x, y = pos % self.w, pos // self.w
        below = self.at(x, y + 1)
        if wall_stitchable(tile):
            if below != -1 and not wall_stitchable(below):
                if below == T['DOOR']: return F['DOOR_SIDEWAYS']
                if below == T['LOCKED_DOOR']: return F['DOOR_SIDEWAYS_LOCKED']
                if below == T['HERO_LKD_DR']: return F['DOOR_SIDEWAYS_LOCKED']
                if below == T['CRYSTAL_DOOR']: return F['DOOR_SIDEWAYS_CRYSTAL']
                if below == T['OPEN_DOOR']: return -1
            else:
                return self.internal_wall(tile, self.at(x + 1, y), self.at(x + 1, y + 1),
                                          below, self.at(x - 1, y + 1), self.at(x - 1, y))
        if tile in (T['LOCKED_EXIT'], T['UNLOCKED_EXIT']): return F['EXIT_UNDERHANG']
        if below != -1 and wall_stitchable(below):
            return self.overhang(tile, self.at(x + 1, y + 1), below, self.at(x - 1, y + 1))
        if below in (T['DOOR'], T['LOCKED_DOOR'], T['HERO_LKD_DR']): return F['DOOR_OVERHANG']
        if below == T['OPEN_DOOR']: return F['DOOR_OVERHANG_OPEN']
        if below == T['CRYSTAL_DOOR']: return F['DOOR_OVERHANG_CRYSTAL']
        m2 = {T['STATUE']: F['STATUE_OVERHANG'], T['STATUE_SP']: F['STATUE_SP_OVERHANG'],
              T['REGION_DECO']: F['REGION_DECO_OVERHANG'],
              T['REGION_DECO_ALT']: F['REGION_DECO_ALT_OVERHANG'],
              T['MINE_CRYSTAL']: F['MINE_CRYSTAL_OVERHANG'],
              T['MINE_BOULDER']: F['MINE_CRYSTAL_OVERHANG'],
              T['ALCHEMY']: F['ALCHEMY_POT_OVERHANG'],
              T['BARRICADE']: F['BARRICADE_OVERHANG'],
              T['HIGH_GRASS']: F['HIGH_GRASS_OVERHANG'],
              T['FURROWED_GRASS']: F['FURROWED_OVERHANG']}
        if below in m2:
            if below in (T['MINE_CRYSTAL'], T['MINE_BOULDER'], T['HIGH_GRASS'], T['FURROWED_GRASS']):
                return self.with_alts(m2[below], pos + self.w)
            return m2[below]
        return -1

    def internal_wall(self, tile, right, right_below, below, left_below, left):
        if tile == T['BOOKSHELF'] or below == T['BOOKSHELF']: r = F['WALL_INTERNAL_WOODEN']
        elif tile == T['WALL_DECO']: r = F['WALL_INTERNAL_DECO']
        elif tile == T['MINE_DIAMOND']: r = F['WALL_INTERNAL_MINE_DIAMOND']
        else: r = F['WALL_INTERNAL']
        if not wall_stitchable(right): r += 1
        if not wall_stitchable(right_below): r += 2
        if not wall_stitchable(left_below): r += 4
        if not wall_stitchable(left): r += 8
        return r

    def overhang(self, tile, right_below, below, left_below):
        if tile == T['OPEN_DOOR']: v = F['DOOR_SIDEWAYS_OVERHANG']
        elif tile == T['DOOR']: v = F['DOOR_SIDEWAYS_OVERHANG_CLOSED']
        elif tile == T['LOCKED_DOOR']: v = F['DOOR_SIDEWAYS_OVERHANG_LOCKED']
        elif tile == T['HERO_LKD_DR']: v = F['DOOR_SIDEWAYS_OVERHANG_LOCKED']
        elif tile == T['CRYSTAL_DOOR']: v = F['DOOR_SIDEWAYS_OVERHANG_CRYSTAL']
        elif below == T['WALL_DECO']: v = F['WALL_OVERHANG_DECO']
        elif below == T['MINE_DIAMOND']: v = F['WALL_OVERHANG_MINE_DIAMOND']
        elif below == T['BOOKSHELF']: v = F['WALL_OVERHANG_WOODEN']
        else: v = F['WALL_OVERHANG']
        if not wall_stitchable(right_below): v += 1
        if not wall_stitchable(left_below): v += 2
        return v

    def features(self, pos):
        tile = self.map[pos]
        alt = 1 if self.var[pos] >= 50 else 0
        if tile == T['HIGH_GRASS']: return 9 + 16 * self.stage + alt
        if tile == T['FURROWED_GRASS']: return 11 + 16 * self.stage + alt
        if tile == T['GRASS']: return 13 + 16 * self.stage + alt
        if tile == T['EMBERS']: return 9 + 16 * 5 + alt
        return -1


# ---------------------------------------------------------------- 构造测试地图
def build_cases():
    """返回 [(名字, M 实例), ...]，覆盖四层会分叉的所有邻接组合"""
    cases = []
    E, W, G, HG, FG, WA, CH, D, OD, LD, SP, ST, STSP, RD, RDA, DP, BK, MB, MC, AL, BA, EX = (
        T['EMPTY'], T['WALL'], T['GRASS'], T['HIGH_GRASS'], T['FURROWED_GRASS'],
        T['WATER'], T['CHASM'], T['DOOR'], T['OPEN_DOOR'], T['LOCKED_DOOR'],
        T['EMPTY_SP'], T['STATUE'], T['STATUE_SP'], T['REGION_DECO'],
        T['REGION_DECO_ALT'], T['EMPTY_DECO'], T['BOOKSHELF'], T['MINE_BOULDER'],
        T['MINE_CRYSTAL'], T['ALCHEMY'], T['BARRICADE'], T['EXIT'])

    # 1) 一间房 + 门 + 各种内部地形（一次性覆盖最多分支）
    w, h = 24, 20
    m = M(w, h, W)
    for y in range(1, h - 1):
        for x in range(1, w - 1):
            m.map[y * w + x] = E
    # 上边一排特殊地形
    row = [G, HG, FG, WA, CH, SP, ST, STSP, RD, RDA, DP, AL, BA, MC, MB, BK]
    for i, t in enumerate(row):
        m.map[1 * w + (2 + i)] = t
    # 下方：门 / 开着的门 / 锁门
    m.map[6 * w + 3] = D; m.map[6 * w + 5] = OD; m.map[6 * w + 7] = LD
    # 内嵌一小块墙（含四角，触发 WALL_INTERNAL 16 缝合）
    for yy in (9, 10, 11):
        for xx in (5, 6, 7):
            m.map[yy * w + xx] = W
    m.map[10 * w + 6] = T['WALL_DECO']
    m.map[9 * w + 5] = BK
    # 草与墙相邻（触发 overhang / underhang）
    for xx in range(12, 18):
        m.map[9 * w + xx] = HG
    for xx in range(12, 16):
        m.map[10 * w + xx] = FG
    m.map[8 * w + 13] = W     # 草上方的墙 → HIGH_GRASS_OVERHANG
    m.map[8 * w + 14] = W
    # 门下方是墙（RAISED_WALL_DOOR / DOOR_SIDEWAYS）
    m.map[12 * w + 3] = D
    m.map[13 * w + 3] = W
    m.map[13 * w + 4] = W
    # exit
    m.map[15 * w + 4] = EX
    m.map[16 * w + 4] = W
    cases.append(('综合房间 depth=1 stage=0', m))

    # 2) 同一张图换深度与草叶段（查 REGION_DECO_ALT 与 stage 分支）
    m2 = M(w, h, W, depth=24, stage=4)
    m2.map = list(m.map)
    m2.var = list(m.var)
    cases.append(('综合房间 depth=24 stage=4', m2))
    m3 = M(w, h, W, depth=9, stage=1)
    m3.map = list(m.map)
    m3.var = list(m.var)
    cases.append(('综合房间 depth=9 stage=1', m3))

    # 3) 纯草 + 水 + 深渊拼接（水/深渊 4 方向缝合）
    w3, h3 = 12, 12
    m4 = M(w3, h3, W)
    for y in range(1, h3 - 1):
        for x in range(1, w3 - 1):
            m4.map[y * w3 + x] = G
    for x in range(3, 8): m4.map[4 * w3 + x] = WA
    for x in range(3, 8): m4.map[8 * w3 + x] = CH
    for y in range(5, 8): m4.map[y * w3 + 5] = G   # 草插进水/深渊之间
    cases.append(('水/深渊缝合', m4))

    # 4) 全地形枚举：每种 Terrain 单独一格，四周 EMPTY
    all_t = sorted(set([v for k, v in T.items()]))
    w5 = len(all_t) + 2
    m5 = M(w5, 3, W)
    for i, t in enumerate(all_t):
        m5.map[1 * w5 + (1 + i)] = t
        m5.map[0 * w5 + (1 + i)] = W   # 上方是墙 → 触发 overhang 分支
    cases.append(('全地形单格枚举', m5))

    return cases


def dump_case(m, name):
    """把某张图的四层帧号导出为 JSON，交给 Node 侧的 JS 引擎比对"""
    terr, rais, wall, feat, tiles = [], [], [], [], []
    for p in range(m.w * m.h):
        terr.append(m.terrain(p)); rais.append(m.raised(p))
        wall.append(m.walls(p)); feat.append(m.features(p)); tiles.append(m.map[p])
    return {'name': name, 'w': m.w, 'h': m.h, 'depth': m.depth, 'stage': m.stage,
            'seed': 12345, 'map': tiles,
            'terrain': terr, 'raised': rais, 'walls': wall, 'features': feat}


JS_HARNESS = r'''
// 在 Node 里跑：把 render.js 的纯计算部分接上假 canvas，比对帧号
// 用法：node _te_harness.js <render.js绝对路径> <cases.json绝对路径>
const fs = require('fs');
const vm = require('vm');

// 假 DOM：Sheet 需要 createElement('canvas') / getContext('2d') / getImageData
function fakeCtx() {
  return { drawImage(){}, getImageData(w,h){ return {data:new Uint8ClampedArray(4)}; },
           clearRect(){}, save(){}, restore(){}, scale(){} };
}
const sandbox = {
  window: {},
  document: { createElement(){ return { width:0, height:0, getContext(){ return fakeCtx(); } }; } },
  console
};
sandbox.window = sandbox;
vm.createContext(sandbox);
// 显式吃绝对路径，别用 __dirname（harness 与 render.js 不在同一目录）
const RENDER_JS = process.argv[2];
vm.runInContext(fs.readFileSync(RENDER_JS, 'utf8'), sandbox);
const R = sandbox.TE_RENDER;

const cases = JSON.parse(fs.readFileSync(process.argv[3], 'utf8'));
let bad = 0, checked = 0;
const failDetail = [];

for (const c of cases) {
  const S = R.state;
  S.w = c.w; S.h = c.h; S.depth = c.depth; S.featuresStage = c.stage;
  S.map = Int32Array.from(c.map);
  S.variance = new Uint8Array(S.w * S.h);
  // 用与 Python 相同的 LCG 重算 variance
  let s = c.seed >>> 0;
  for (let i = 0; i < S.variance.length; i++) {
    s = (Math.imul(s, 1664525) + 1013904223) >>> 0;
    S.variance[i] = (s >>> 16) % 100;
  }

  for (let p = 0; p < c.map.length; p++) {
    const got = {
      terrain: R.terrainVisual(p, S.map[p]),
      raised:  R.raisedVisual(p, S.map[p]),
      walls:   R.wallsVisual(p, S.map[p], null),
      features:R.featuresVisual(p, S.map[p]),
    };
    for (const k of ['terrain','raised','walls','features']) {
      checked++;
      const exp = c[k][p];
      const g = (got[k] === undefined) ? -1 : got[k];
      if (g !== exp) {
        bad++;
        if (failDetail.length < 25) {
          failDetail.push(`  [${c.name}] pos=${p}(${p%c.w},${Math.floor(p/c.w)}) ` +
            `tile=${S.map[p]} ${k}: JS=${g} PY=${exp}`);
        }
      }
    }
  }
}
console.log(JSON.stringify({checked, bad, failDetail}));
'''


def main():
    cases = build_cases()
    data = [dump_case(m, n) for n, m in cases]
    tmp = os.path.join(HERE, '_te_cases.json')
    with open(tmp, 'w', encoding='utf-8') as f:
        json.dump(data, f)

    harness = os.path.join(HERE, '_te_harness.js')
    with open(harness, 'w', encoding='utf-8', newline='\n') as f:
        f.write(JS_HARNESS.lstrip('\n'))

    print('构造 %d 张测试地图，共 %d 格' % (len(data), sum(d['w'] * d['h'] for d in data)))
    for d in data:
        print('  %-24s %dx%d  depth=%-3d stage=%d' % (d['name'], d['w'], d['h'], d['depth'], d['stage']))
    print()

    render_js = os.path.join(ROOT, 'tools', 'terrain-editor', 'render.js')
    out = subprocess.run([NODE, harness, render_js, tmp], capture_output=True, timeout=180,
                         cwd=HERE)
    if out.returncode != 0:
        print('Node 侧失败：')
        print(out.stderr.decode('utf-8', 'replace')[:3000])
        sys.exit(1)
    res = json.loads(out.stdout.decode('utf-8').strip().splitlines()[-1])

    print('比对 %d 个 (格 × 层) 组合：一致 %d，不一致 %d'
          % (res['checked'], res['checked'] - res['bad'], res['bad']))
    if res['bad']:
        print('\n不一致明细（最多 25 条）：')
        for l in res['failDetail']:
            print(l)
        print('\n❌ 编辑器渲染引擎与 Java 算法不一致，请核对 render.js')
        sys.exit(1)

    print('✅ 编辑器渲染引擎四层帧号与 Java 算法逐格一致')

    # ---- 反例自测：故意歪掉一个常量，必须报不一致 ----
    print()
    print('反例自测：把 render.js 的 RAISED_HIGH_GRASS 挪 1 帧，应报不一致…')
    src = open(os.path.join(ROOT, 'tools', 'terrain-editor', 'render.js'), encoding='utf-8').read()
    broken = src.replace('RAISED_HIGH_GRASS: RAISED_OTHER + 2,',
                         'RAISED_HIGH_GRASS: RAISED_OTHER + 3,', 1)
    if broken == src:
        print('  ⚠ 反例替换没生效（找不到目标串），本项自测无效')
    else:
        bdir = os.path.join(HERE, '_te_broken')
        os.makedirs(bdir, exist_ok=True)
        with open(os.path.join(bdir, 'render.js'), 'w', encoding='utf-8', newline='\n') as f:
            f.write(broken)
        broken_js = os.path.join(bdir, 'render.js')
        out2 = subprocess.run([NODE, harness, broken_js, tmp], capture_output=True, timeout=180, cwd=HERE)
        if out2.returncode != 0:
            print('  ⚠ 反例运行异常，视为自测无效')
        else:
            r2 = json.loads(out2.stdout.decode('utf-8').strip().splitlines()[-1])
            if r2['bad'] > 0:
                print('  ✓ 反例被检出（%d 处不一致）⇒ 判据有效' % r2['bad'])
            else:
                print('  ❌ 反例未被检出 ⇒ 判据是恒真的空测，必须修脚本')
                sys.exit(1)

    # 清理
    for p in (tmp,):
        if os.path.exists(p): os.remove(p)


if __name__ == '__main__':
    main()
