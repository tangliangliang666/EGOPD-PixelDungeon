# -*- coding: utf-8 -*-
"""
地形编辑器 · 贴图资产提取（2026-09-19）

把游戏真实图集编码成 base64，产出一个自包含的 JS 数据文件，
供 tools/terrain-editor/index.html 直接加载（不需要起服务器、不需要读本地文件）。

产出：tools/terrain-editor/assets.js
    window.TE_ASSETS = {
      sewers: "data:image/png;base64,...",
      prison: ...,
      ...
      features: ...,      # terrain_features.png（草叶细节，全局唯一，不分区域）
      items: ...,         # items.png（道具图集，道具占位图取的就是这张）
      water0..water4: ... # 水体动画帧
    }

用法：
    python _chk/terrain_editor_assets.py
"""
import base64
import os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ENV = os.path.join(ROOT, 'core', 'src', 'main', 'assets', 'environment')
# 道具图集不在 environment/ 下，而在 sprites/ 下，单独给一个目录常量
SPR = os.path.join(ROOT, 'core', 'src', 'main', 'assets', 'sprites')
OUT_DIR = os.path.join(ROOT, 'tools', 'terrain-editor')
OUT_JS = os.path.join(OUT_DIR, 'assets.js')

# key 用区域短名（与编辑器下拉框的 value 一致）
#
# ⚠️ 这里**只收 SPD 原版自带的 5 张区域图集**。任何本 mod 特有的图集
#    （例如 27 层测试层的 tiles_lob.png）都**不进内置清单** —— 编辑器的定位是
#    通用工具，项目特有的图集请用左栏「外部图集」导入（会存进 IndexedDB，
#    刷新后自动恢复）。这样 assets.js 的体积也只随原版图集走。
SHEETS = [
    ('sewers', 'tiles_sewers.png', '下水道 (1-5)'),
    ('prison', 'tiles_prison.png', '监狱 (6-10)'),
    ('caves',  'tiles_caves.png',  '矿洞 (11-15)'),
    ('city',   'tiles_city.png',   '都市 (16-20)'),
    ('halls',  'tiles_halls.png',  '恶魔大厅 (21-26)'),
]
FEATURES = ('features', 'terrain_features.png')

# 道具图集：道具占位图（口粮）取自这里。16px 宫格，256×800。
# ⚠️ 与楼层图集**不是同一张**，编辑器必须单独持有（S.itemSheet）。
# RATION = xy(1,28)+5 = 437，见 render.js 的 RATION_FRAME 推导。
ITEM_SHEET = ('items', 'items.png', '道具图集 items.png（口粮占位图来源）')

# 水体动画帧：water0..water4.png，每张 32×32（2×2 格），6 色调色板。
# ⚠️ 水面**不走 tilemap**（`DungeonTerrainTilemap.needsRender()` 会跳过纯 WATER 帧），
#    它是 GameScene 里一个独立的 `SkinnedBlock`，用 waterTex() 指定的贴图平铺全图。
#    编辑器若不单独铺这一层，水格就是空的（看着和深渊一样黑）。
WATERS = ['water%d.png' % i for i in range(5)]


def main():
    os.makedirs(OUT_DIR, exist_ok=True)

    parts = []
    manifest = []
    total = 0

    def emit(key, fname, label, base=None):
        d = base or ENV
        path = os.path.join(d, fname)
        if not os.path.exists(path):
            raise SystemExit('缺文件: %s' % path)
        raw = open(path, 'rb').read()
        b64 = base64.b64encode(raw).decode('ascii')
        parts.append('  %s: "data:image/png;base64,%s"' % (key, b64))
        manifest.append((key, fname, label, len(raw), len(b64)))
        print('  %-9s %-24s %7d B  ->  %8d B base64' % (key, fname, len(raw), len(b64)))
        return len(raw)

    for key, fname, label in SHEETS + [(FEATURES[0], FEATURES[1], '草叶细节（全局唯一）')]:
        total += emit(key, fname, label)

    # 道具图集（口粮占位图的来源，在 sprites/ 下）
    total += emit(ITEM_SHEET[0], ITEM_SHEET[1], ITEM_SHEET[2], base=SPR)

    # 水体动画帧（water0..water4）
    for i, fname in enumerate(WATERS):
        total += emit('water%d' % i, fname, '水体动画第 %d 帧' % i)

    js = []
    js.append('/* 由 _chk/terrain_editor_assets.py 生成，请勿手改。')
    js.append(' * 内容：SPD 真实楼层图集 + terrain_features + items + 水体动画帧 的 base64 内联。')
    js.append(' * 重新生成：python _chk/terrain_editor_assets.py')
    js.append(' */')
    js.append('window.TE_ASSETS = {')
    js.append(',\n'.join(parts).rstrip(','))
    js.append('};')
    js.append('')
    js.append('/* 水体动画帧数量 */')
    js.append('window.TE_WATER_FRAMES = %d;' % len(WATERS))
    js.append('')
    js.append('/* 图集清单（供 UI 建下拉框） */')
    js.append('window.TE_SHEETS = [')

    rows = []
    for key, fname, label in SHEETS:
        rows.append('  { key: "%s", label: "%s", file: "%s" }' % (key, label, fname))
    js.append(',\n'.join(rows))
    js.append('];')
    js.append('')

    open(OUT_JS, 'w', encoding='utf-8', newline='\n').write('\n'.join(js))
    print()
    print('写入 %s' % OUT_JS)
    print('原始合计 %d B，base64 后文件 %d B' % (total, os.path.getsize(OUT_JS)))


if __name__ == '__main__':
    main()
