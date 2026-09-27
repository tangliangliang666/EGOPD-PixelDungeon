# -*- coding: utf-8 -*-
"""2026-09-23：把「封印之剑不可售」+「场景特效坐标速查」追加进 docs/features.md，
并新建今日工作日志 .workbuddy/memory/2026-09-23.md。幂等：已追加则跳过。"""
import io
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FEATURES = os.path.join(ROOT, 'docs/features.md')
LOG = os.path.join(ROOT, '.workbuddy/memory/2026-09-23.md')

FEATURE_TEXT = u'''# 2026-09-23 封印之剑系列「不可出售」+ 场景特效坐标速查

## 一、需求与根因

需求：封印之剑系列（封印之剑 / 一阶段 / 二阶段 / 莱瓦汀）目前能在商店被卖掉，需要禁止。

根因是两层，缺一不可：

1. `Item.sellable()` 的默认规则＝`!unique || stackable`，而本系列**没有**标 `unique`
   （它是普通六阶武器，标了会顺带被禁掉锻造/附魔等一系列东西）⇒ 默认就是可售的。
2. `doUnequip()` 那层「锁死在双手」的保护**挡不住出售** —— 商店的收购窗口是 `WndBag`，
   它在布局里把 `belongings.weapon`（主手）与 `belongings.secondWep`（副手）**一起** `placeItem`，
   所以剑只要挂在副手，就会出现在收购列表里、被当成普通六阶武器换钱。

## 二、改动（1 个文件）

`items/weapon/melee/SealedSwordBase.java` 新增覆写：

```java
@Override
public boolean sellable() {
    return false;
}
```

- 覆写的是**谓词**，不是去加 `unique` —— `Item` 的注释已把这条约定写死：
  「要单独放开/关掉某一项出口，就覆写 `sellable()` / `transmutable()`，**别动** `unique`」。
- 放基类一处即覆盖四个形态，与 `isUpgradable()` 同一个理由（同一件事写四遍迟早会漏一个）。
- `Shopkeeper.canSell` 是全仓**唯一**消费 `sellable()` 的地方（实测 1 处），所以这一处即封锁整条出售路径。

## 三、封印之剑场景特效坐标速查（滤镜 / 火焰粒子 / 屏幕扭曲）

> 设计与历史见本文件「2026-09-14（补2）」（滤镜）与「（补3）」（减弱 + 四周火焰 + 热浪扭曲）。
> 下表是**按行号定位的调参入口**，只想微调观感时照这张表改即可；行号会随编辑漂移，动手前用 `grep -n` 复核。

### ① 温度滤镜（屏温色罩）— `core/src/main/java/.../effects/HeatVignette.java`

| 行 | 成员 | 作用 / 怎么调 |
|---|---|---|
| 73 | `TEXTURE_KEY` | 渐变贴图的全局缓存键（全局只一张，颜色靠染色） |
| 76 | `TEXTURE_SIZE` | 渐变分辨率 64×64，仅一次性生成 |
| 79 | `INNER_RADIUS` | 中心留白圈半径，越大中央越清透 |
| 82 | `CENTER_FLOOR` | 中心底噪（中心仍留的一丁点色度） |
| 89 | `HEAT_COLORS` | **色罩颜色**，下标＝形态序号 0封印/1黄/2橙/3红 |
| 92 | `FLAME_COLORS` | **火焰粒子颜色**，比色罩更亮更饱和 |
| 95 | `HEAT_ALPHA` | **边缘不透明度** 0 / 0.20 / 0.28 / 0.36 |
| 98 | `HEAT_WARP` | **边缘最大像素偏移** 0 / 0.9 / 1.4 / 2.0 |
| 101 | `WARP_EDGE_BIAS` | 扭曲的近中心衰减：0＝全屏一致、1＝只在边缘明显 |
| 104 | `WAVE_PERIOD` | 热浪循环周期 1.2s（shader 里时间系数只能取整数，保证回绕连续） |
| 109 | `FADE_TIME` | 冷热切换的平滑时长 0.35s |
| 112 | `MIN_HEAT` | 低于它视为无特效 |
| 123-139 | 构造函数 | 用 `uiCamera`、贴合屏幕、初始直接对齐（避免换层闪淡入） |
| 141-157 | `update()` | 每帧指数逼近 `targetHeat()`（所以换形态是渐变而非硬切） |
| 160-164 | `draw()` | **第一行关掉扭曲** ⇒ 世界层终点 / UI 起点 |
| 179-201 | `heat()` / `flameColor()` / `targetHeat()` | 对外接口；`targetHeat()` 读手持剑的 `stage()`，受设置开关约束 |
| 209-218 | `applyHeat()` | 把热度换算成颜色 + alpha 套到本 Image 上 |
| 230-241 | `beginWorldPass()` | **推扭曲参数**（由 `GameScene.draw()` 第一句调用） |
| 258-275 | `sample()` / `lerpColor()` | 色板与强度表的线性插值 |
| 301-328 | `makeTexture()` | **生成 64×64 白色径向渐变**（`smoothstep` 从中心 0 升到边缘 1） |

### ② 屏幕四周火焰（火苗）— `core/src/main/java/.../effects/HeatFlames.java`

| 行 | 成员 | 作用 / 怎么调 |
|---|---|---|
| 53 | `MIN_INTENSITY` | 低于它一条火线都不点 |
| 56 | `RISE_FRACTION` | 火苗漂出距离占屏幕高的比例 18% |
| 59 | `BASE_LIFE` | 基准寿命 0.9s |
| 62-63 | `BASE_SIZE` / `SIZE_GAIN` | 粒子基准边长 3.2px、随强度增长 2.4px |
| 66-90 | `Line` 内部类 | 一条火线：归一化位置/范围 + 漂移方向 + 缩放 + 基准间隔 |
| 99-114 | 构造函数 | **布局**：底边 4 段最盛、左右各 2 段、顶边 3 段，共 11 条火线 |
| 117-127 | `line(...)` | 建一条火线（`autoKill=false`、`camera=uiCamera`） |
| 133-149 | `factoryFor(...)` | 每条线一个匿名工厂：方向固化、颜色/速度在 `emit()` 那一刻现读、`lightMode()=true` |
| 152-191 | `update()` | 每帧按热度刷新颜色/大小/速度与各线发射间隔；**160 行**是 `pow(heat/3, 1.4)` 强度曲线 |

### ③ 火焰粒子本体 — `core/src/main/java/.../effects/particles/HeatFlameParticle.java`

| 行 | 成员 | 作用 / 怎么调 |
|---|---|---|
| 36 | `extends PixelParticle.Shrinking` | 会一边飘一边缩小的像素方块 |
| 40 | `lifespan` 默认值 | 0.9s |
| 50-66 | `reset(...)` | 颜色/加速度/大小/寿命全由发射方喂；62 行加横向抖动防「栅栏感」 |
| 68-81 | `update()` | alpha 曲线：出生 15% 快速亮起 → 中段实心 → 末期 35% 淡出 |

### ④ 屏幕扭曲（热浪折射 shader）— `SPD-classes/src/main/java/com/watabou/noosa/NoosaScriptWarp.java`

| 行 | 成员 | 作用 / 怎么调 |
|---|---|---|
| 46 | `extends NoosaScript` | 只比默认脚本多一段「算抖动 → 偏移 vUV」，光照完全保留 |
| 49 | `enabled` | **「谁被扭曲」的唯一开关**，由核心侧每帧驱动 |
| 59-66 | 构造函数 | 取 4 个 uniform：时间 / 幅度 / 分辨率 / 边缘偏置 |
| 68-71 | `get()` | `Script.use(...)` 取全局唯一实例 |
| 80-84 | `resetCameraCache()` | `Game.render()` 每帧调用（不调则画面不再跟随镜头） |
| 96-102 | `upload(time, ampPixels, edgeBias)` | 上传一次扭曲参数，每帧一次即可 |
| 109-153 | `SHADER` | **波形本体**：顶点 112-123 输出 `vScreen`；片元 129-153，抖动公式在 **145-151**，`gl_FragColor` 在 152 |

### ⑤ 挂载与驱动

| 位置 | 内容 |
|---|---|
| `scenes/GameScene.java:54-55` | import 两个特效类 |
| `scenes/GameScene.java:207` / `210` | 成员 `heatVignette` / `heatFlames` |
| `scenes/GameScene.java:386-391` | `add()`：滤镜在前、火苗紧随其后；**都在世界层最后、所有 UI 之前** |
| `scenes/GameScene.java:817-820` | 覆写 `draw()`：**第一句** `HeatVignette.beginWorldPass()` |
| `noosa/NoosaScript.java:187-188`、`NoosaScriptNoLighting.java:41-42` | `enabled` 为真时 `get()` 返回扭曲脚本 |
| `noosa/Game.java:165` | 每帧 `NoosaScriptWarp.resetCameraCache()` |
| `SPDSettings.java:298-306` | `KEY_LAEVATEIN_FX="laevateinn_fx"` + getter/setter（默认 true） |
| `windows/WndSettings.java:230` / `318-326` / `367-368` | 显示页复选框 `chkHeatFx`「莱瓦汀场景特效」 |
| `windows_zh.properties:322` / `windows.properties:322` | 该开关的文本键 |
| `items/weapon/melee/Laevateinn.java:103-130` | **另一套「世界内火场」**（`burnAura`：每回合给自身与 5x5 圆形内的地块 `Blob.seed(Fire)`）——那是地图上真实燃烧的火，不属于屏幕特效；它的每回合心跳借 `SealedSwordBase.SwordSwap.act()` |

## 四、核验

- 新增 `_chk/verify_sealed_sellable.py`：**35 条断言 + 3 组反例自测 ALL PASS**。
  反例覆盖三条「看起来也行但其实是错的」写法：① 方法体改回 `return true`；② 整段删掉覆写
  （改回继承 `Item` 默认值）；③ 拿 `unique = true` 当杠杆 —— 三条都被判据抓出。
- **字节码级实证**（`javap -c`）：新编译的 `SealedSwordBase.sellable()` 是 `iconst_0; ireturn`
  （＝返回 false）；四个形态类**均无**自己的 `sellable()`（继承基类的 false）；
  对照组 `Item.sellable()` 仍是 `!unique || stackable`；`TearSwordBlessing` / `Admiration`
  仍返回 `true`（未被误伤）。
- 单文件 `javac -proc:none -Xlint:all`（SealedSwordBase + HeatVignette）**EXIT=0、0 错误**；
  3 条告警全部落在**未改动行**（`SealedSwordBase:100` 的 `new Class[]` rawtypes、
  `HeatVignette:138/215` 的 `this-escape`），均属改动前既有。
- 顺手修掉一处**失效的 javadoc 链接**：`HeatVignette` 类注释里写着 `{@link #pushWarp()}`，
  而该方法早已改名，改为 `{@link #beginWorldPass()}`。
- `check_utf8_all.py` 1452 文件 OK；改动文件换行符（两处均 LF）与括号配平未破坏。

## 五、待人工验证

① 商店收购列表里（含副手那把）不再出现封印之剑系列任何形态；
② 同一界面里普通武器、以及「泪剑祝福 / 爱慕」这类**刻意放开出售**的神器照旧可卖；
③ 解封切换形态后（一阶段/二阶段/莱瓦汀）同样不可售。
'''

LOG_TEXT = u'''# 2026-09-23 封印之剑不可售 + 场景特效坐标速查

## 一、封印之剑系列「不可出售」

**症状**：封印之剑系列（四形态）能在商店被卖掉。

**根因（两层，缺一不可）**：
1. `Item.sellable()` 默认＝`!unique || stackable`，本系列**没标** `unique`（普通六阶武器，标了会顺带
   被禁掉锻造/附魔等）⇒ 默认可售；
2. `doUnequip()` 的「锁死在双手」**挡不住出售** —— 商店收购窗口是 `WndBag`，它的布局把
   `belongings.weapon`（主手）与 `belongings.secondWep`（副手）**一起** `placeItem`，
   所以剑挂在副手时就会出现在收购列表里、按六阶武器换钱。

**改动**：`items/weapon/melee/SealedSwordBase.java` 新增 `@Override public boolean sellable(){ return false; }`。
覆写**谓词**而不是加 `unique`（`Item` 注释已把这条约定写死）；基类一处覆盖四形态；
`Shopkeeper.canSell` 是全仓唯一消费点（实测 1 处）。

**核验**：新增 `_chk/verify_sealed_sellable.py`（35 断言 + 3 组反例 ALL PASS）；
`javap -c` 字节码实证新编译的 `sellable()` 是 `iconst_0; ireturn`，四个形态均无自己的覆写，
`Item` 默认与 `TearSwordBlessing`/`Admiration` 均未被误伤。

## 二、顺手修掉一处失效 javadoc 链接

`effects/HeatVignette.java` 类注释里 `{@link #pushWarp()}` —— 该方法早已改名 `beginWorldPass()`，
链接是死的（javadoc 工具会报未解析引用）。已改为 `{@link #beginWorldPass()}`。
**教训**：重命名方法时要顺手全仓 grep 旧名 —— 注释里的 `{@link #旧名()}` 编译期不报错（只有
`javadoc` 任务才会，而本项目不跑它），靠 `javac` 永远抓不到。

## 三、封印之剑场景特效坐标速查（已落 docs/features.md）

三层特效各自的**调参入口**与**挂载点**已按「文件:行」整理进 `docs/features.md`
新增的 §「2026-09-23 … 三、封印之剑场景特效坐标速查」：

- **滤镜**：`effects/HeatVignette.java`（色板 89/92、透明度 95、扭曲幅度 98、渐变贴图生成 301-328）；
- **火焰粒子**：`effects/HeatFlames.java`（布局 99-114 共 11 条线、强度曲线 160、工厂 133-149）
  ＋ `effects/particles/HeatFlameParticle.java`（粒子本体 50-81）；
- **屏幕扭曲**：`SPD-classes/.../noosa/NoosaScriptWarp.java`（开关 49、上传 96-102、shader 109-153，
  抖动公式 145-151）＋ 每帧开关点在 `GameScene.draw()` 817-820；
- 另有**世界内火场**（`Laevateinn.burnAura` 103-130，地图上真实燃烧的火）与之无关，单独标注。

**方便日后复核的入口**：`python _chk/docfind.py 热浪扭曲` / `grep -n "HEAT_ALPHA" <HeatVignette>`。

## 四、待人工验证

① 商店收购列表（含副手那把）不再出现封印之剑系列任何形态；② 普通武器与「刻意放开出售」的神器照旧可卖；
③ 解封到一阶段/二阶段/莱瓦汀后同样不可售。
'''

MODE = 'a'


def append_prepare(path):
    """返回 (后缀处理函数, 标签)；文件不存在就标记为新建。"""
    if not os.path.exists(path):
        return None
    with open(path, 'rb') as f:
        raw = f.read()
    crlf = b'\r\n' in raw
    text = raw.decode('utf-8').replace('\r\n', '\n')
    # 尾部空行是允许的（features.md 一直留着几行空行）；重复追加由调用方的 SKIP 检测兜住
    return crlf, text


def write_text(path, text, crlf):
    out = text.replace('\n', '\r\n') if crlf else text
    with open(path, 'wb') as f:
        f.write(out.encode('utf-8'))


# ---- features.md ----
if '2026-09-23 封印之剑系列' in open(FEATURES, encoding='utf-8').read():
    print('SKIP docs/features.md（已追加过）')
else:
    crlf, text = append_prepare(FEATURES)
    text = text.rstrip('\n') + '\n' + FEATURE_TEXT.replace('\r\n', '\n')
    if not text.endswith('\n'):
        text += '\n'
    write_text(FEATURES, text, crlf)
    print('OK 已追加 docs/features.md（%s，现 %d 字符）' % ('CRLF' if crlf else 'LF', len(text)))

# ---- 今日工作日志 ----
if os.path.exists(LOG):
    body = open(LOG, encoding='utf-8').read()
    assert '# 2026-09-23' not in body, '今日日志里已有同名条目，需人工确认'
    crlf, text = append_prepare(LOG)
    text = text.rstrip('\n') + '\n\n' + LOG_TEXT.replace('\r\n', '\n')
else:
    crlf, text = False, ''
    text = LOG_TEXT.replace('\r\n', '\n')
    print('（今日日志不存在，新建）')
if not text.endswith('\n'):
    text += '\n'
write_text(LOG, text, crlf)
print('OK 已写入 .workbuddy/memory/2026-09-23.md（%s，现 %d 字符）' % ('CRLF' if crlf else 'LF', len(text)))
