# 新地形创作指南（自用流程）

> 本指南供**新增一种地形贴图（重点讲草地/植被类）**时按步骤执行：先定「要画几帧、每帧画在哪几行」，
> 再按清单接线。
> 渲染管线的通用机制见根目录 `AGENTS.md` §3「地图贴图＝分层 tilemap + 跨格拼帧」条目；
> 数值与结论均已在 2026-09-18 用像素级复刻核验过（工具见第 6 节）。

---

## 0. 先选路线：新图集还是借空白帧

| | **A. 全新图集（推荐）** | **B. 借现有图集的空白帧** |
|---|---|---|
| 做法 | 复制一份分区图集 → 改名 → 在**原高度下面追加行**，在新帧里画 | 直接在 `tiles_sewers.png` 等原图集的空白帧里画 |
| 适用 | 新分区 / 新楼层主题 | 新增一个地形，且希望**各分区都能用** |
| 优点 | 帧号绝对安全、原图一字不动 | 不新增文件 |
| 代价 | 只有覆写 `tilesTex()` 的那一层会用；要多个分区共用就得每张图集各放一份 | 帧号在**所有**分区图集里语义必须一致，且要避开保留帧 |

**无论哪条路，都必须保持「标准 16 行布局」不变**——`DungeonTileSheet` 的常量把行号写死了。

先例：`tiles_lob.png`（`LobTestLevel`，整张手绘新图集）、`tiles_caves_crystal.png` / `tiles_caves_gnoll.png`
（在 `tiles_caves.png` 基础上高度 256→288，用**追加的 2 行**画钻石矿墙，`MiningLevel` 按任务类型切图集）。

---

## 1. 决定画几帧：先看清渲染链路

### 1.1 一张图集被三个渲染层共用

`DungeonTerrainTilemap` / `RaisedTerrainTilemap` / `DungeonWallsTilemap` 三者**都**执行
`super(Dungeon.level.tilesTex())`（分别见 `tiles/DungeonTerrainTilemap.java:34`、`RaisedTerrainTilemap.java:30`、
`DungeonWallsTilemap.java:34`）。

> **你不能让「草来自 A 图、墙来自 B 图」**。一个楼层只有一张图集，它必须同时供地面、抬高地形、墙、门、草使用。

三层的分工与 z 序（`scenes/GameScene.java:258-352`，`add(...)` 的先后＝绘制先后）：

| 画序 | 层 | 类 | 相对角色 | 这一层画什么 |
|---|---|---|---|---|
| 1 | `terrain` 组 | `DungeonTerrainTilemap` | **之下** | 地面 / 抬高地形本体 / 墙正面 |
| 2 | `terrain` 组 | `TerrainFeaturesTilemap` | **之下** | 草丛草叶、植物、陷阱 |
| 3 | `mobs` 组 | `HeroSprite` / 各 Mob | — | 角色 |
| 4 | 独立组 | `RaisedTerrainTilemap` | **之上** | 抬高地形的「下悬」（草叶前景） |
| 5 | 独立组 | `DungeonWallsTilemap` | **之上** | 墙内部黑块 + **上一格**的墙檐/草檐 |

### 1.2 ⚠️ 草叶细节层是**全局唯一**的，不跟你换图集

`TerrainFeaturesTilemap` 用的是全项目唯一一张 `environment/terrain_features.png`
（256×128 = 16 列 × 8 行 = 128 帧，全分区共用），按 `stage = (Dungeon.depth-1)/5` 选行
（`tiles/TerrainFeaturesTilemap.java:60-77`）：

| stage | 楼层 | 高草主/ALT | 平地草主/ALT |
|---|---|---|---|
| 0 | 1-5 下水道 | 9 / 10 | 13 / 14 |
| 1 | 6-10 监狱 | 25 / 26 | 29 / 30 |
| 2 | 11-15 洞穴 | 41 / 42 | 45 / 46 |
| 3 | 16-20 城市 | 57 / 58 | 61 / 62 |
| 4 | 21+ 大厅 | 73 / 74 | 77 / 78 |

**后果**：如果你沿用原版 `Terrain.HIGH_GRASS` ID，草叶细节**永远**来自这张表——换新图集也换不掉。

**要「草叶也完全自己画」，正确做法是新增一个独立的地形 ID，并且不要给它加 `TerrainFeaturesTilemap` 分支**
（该方法末尾 `return -1`＝该格子在这一层什么都不画），把草叶全部画进你自己的图集。
`Terrain.MINE_DIAMOND` 就是这么做的（它没有 features 分支）。

### 1.3 平面帧（`FLAT_*`）只给 examine 小图用

点格子时 `WndInfoCell.cellImage` → `DungeonTerrainTilemap.tile(cell, tile)` → `getTileVisual(pos, tile, **true**)`
（`tiles/DungeonTerrainTilemap.java:108-112`）走 `flat` 分支，只查 `DungeonTileSheet.directFlatVisuals`。
所以**每种新地形都必须给一张平面帧**，否则：

```java
// directFlatVisuals.get(tile) 返回 Integer=null ⇒ 传给 int 形参自动拆箱 ⇒ NullPointerException
else return DungeonTileSheet.getVisualWithAlts( DungeonTileSheet.directFlatVisuals.get(tile), pos );
```

**点一下这个格子就闪退**。这是最容易漏、且报错现场（`WndInfoCell`）与病因（`DungeonTileSheet`）离得最远的一处。

---

## 2. 图集规格

### 2.1 尺寸与坐标

- `DungeonTilemap` 用 `new TextureFilm(tex, 16, 16)`，所以**宽高必须都是 16 的整数倍**；
  宽固定 **256**（=16 列），高度决定行数。**推荐 256×256（256 帧）**，需要更多帧就 +16 往上加（256×288 = 288 帧，同 crystal/gnoll）。
- `DungeonTileSheet.xy(列, 行)` 是 **1 基**，**第一参＝列、第二参＝行**（内部 `x-=1; y-=1`）。
- **帧号 = (行-1)×16 + (列-1)**，即 `xy(1,1)=0`、`xy(16,1)=15`、`xy(1,2)=16`。
- 每帧就是一块 16×16 的方块，`Tilemap.updateVertices()` 把第 (列,行) 帧贴在 `(列×16, 行×16)` 上，**帧永不越格**。

### 2.2 标准 16 行布局（必须保持）

| 行 | `xy` 基址 | 常量组 | 内容 |
|---|---|---|---|
| 1 | `xy(1,1)=0` | `GROUND`（24 槽） | 地面 / 草皮 / 余烬 / 出入口 / 井 / 底座 |
| 2 | `xy(9,2)=24` | `CHASM`（8 槽） | 深渊 + 4 种缝合（地板/SP/墙/水） |
| 3 | `xy(1,3)=32` | `WATER`（16 槽） | 水的 15 种缝合（**第 1 槽＝纯水，故意留空**，`needsRender` 会跳过它，水本体是 `SkinnedBlock`） |
| 4 | `xy(1,4)=48` | `FLAT_WALLS`（16 槽） | 墙/门/书架的**平面**帧（examine 用） |
| 5 | `xy(1,5)=64` | `FLAT_OTHER`（16 槽） | 药锅/路障/草/雕像/区段装饰/矿的平面帧 |
| 6-7 | `xy(1,6)=80` | `RAISED_WALLS`（32 槽） | 墙的抬高本体（`+1` 右开口、`+2` 左开口；`+16` 起是 ALT） |
| 8 | `xy(1,8)=112` | `RAISED_DOORS`（8 槽） | 门的抬高本体 + 顶/底门洞的地板 |
| 9 | `xy(9,8)=120` | `RAISED_OTHER`（24 槽） | 药锅/路障/**高草皮**/犁过的草/雕像/装饰/矿 |
| 10-12 | `xy(1,10)=144` | `WALLS_INTERNAL`（48 槽） | 墙内部（`+0..+15` 四方向缝合；`+16` DECO；`+32` 木制） |
| 13-14 | `xy(1,13)=192` | `WALLS_OVERHANG`（32 槽） | 墙檐（`+1` 右下开口、`+2` 左下开口；门檐在 `+16` 起） |
| 15 | `xy(1,15)=224` | `DOOR_OVERHANG`（8 槽） | 门檐 + 门侧立面 + 出口下悬 |
| 15-16 | `xy(9,15)=232` | `OTHER_OVERHANG`（24 槽） | 药锅/路障/**草檐 234**/**草下悬 250**/雕像/装饰/矿 |

### 2.3 追加行的做法（路线 A）

1. 复制目标分区的图集为新文件，例如 `tiles_sewers.png` → `tiles_myregion.png`。
2. 把 PNG 高度 +16（或 +32），**上面 256 帧原样保留**，新帧从 `xy(1,17)=256` 开始。
3. 在 `DungeonTileSheet` 里给新帧写**绝对帧号**常量（照 `FLAT_MINE_DIAMOND = 256;` 那段，`DungeonTileSheet.java:319-330`）。

### 2.4 空白帧（路线 B：借现有图集的空白帧）

六张分区图集（`sewers`/`prison`/`caves`/`city`/`halls`/`lob`）里**全部空白**的帧共 52 个。
其中真正**可以随便用**的是下面这 31 个：

```
  5=xy( 6, 1)  11=xy(12, 1)  13=xy(14, 1)  14=xy(15, 1)  15=xy(16, 1)  21=xy( 6, 2)  23=xy( 8, 2)  29=xy(14, 2)
 30=xy(15, 2)  31=xy(16, 2)  51=xy( 4, 4)  55=xy( 8, 4)  62=xy(15, 4)  63=xy(16, 4)  68=xy( 5, 5)  71=xy( 8, 5)
117=xy( 6, 8) 118=xy( 7, 8) 124=xy(13, 8) 127=xy(16, 8) 204=xy(13,13) 205=xy(14,13) 206=xy(15,13) 207=xy(16,13)
231=xy( 8,15) 236=xy(13,15) 239=xy(16,15) 247=xy( 8,16) 248=xy( 9,16) 249=xy(10,16) 255=xy(16,16)
```

以及**剩下 21 个「空白但不能用」的帧**——它们在下水道等其他分区是空的，但在 crystal/gnoll 或语义上被占用，**不可挪用**：
`32`（纯水槽，必须保持空白）、`76~79`（`FLAT_MINE_*`）、`132~143`（`RAISED_MINE_*` 及钻石矿备用槽）、
`244~246`（`MINE_CRYSTAL_OVERHANG`）、`252`。

### 2.5 PNG 格式

- 8bit、**非隔行**；RGBA 或**调色板 PNG**都可以（`TextureCache` 直接读，本作 `sprites/*.png` 就是调色板）。
- 帧内不要画到 16×16 之外，也不要靠「向下多画 1 行」来加高——那行会被算进下一格的帧。
- 透明处请用 **alpha=0**；`~~` 半透明在「地面层」会漏出背景，除草叶外不要用。

---

## 3. 草地怎么画（核心）

### 3.1 平地草（沿用 `Terrain.GRASS`）：1 帧草皮 + 几笔草叶

| 内容 | 画在哪 | 实测（`tiles_sewers.png` / `terrain_features.png`） |
|---|---|---|
| 草皮 | 图集 `GRASS = GROUND+2 = xy(3,1) = 帧2` | **满格 256px 不透明**（row0..15 各 16px，alpha 全 255） |
| 草皮 ALT | `GRASS_ALT = GROUND+8 = xy(11,1) = 帧10` | 同上；50% 概率替换（`tileVariance[pos] >= 50`） |
| 草叶点缀 | `terrain_features.png` 帧 13/14（stage 0） | **稀疏几笔**：每行只有 1~4 个不透明像素 |

平地草**不抬高**、不参与遮挡：`GRASS` 走 `directVisuals`，由 `DungeonTerrainTilemap` 在角色之下画一格就完事。
想更丰富可以多画几组 ALT，但 `commonAltVisuals` 只支持**一对一**映射（`DungeonTileSheet.java:507-536`），
要「三选一」得自己加一层按 `tileVariance` 取模的分支。

### 3.2 高草（沿用 `Terrain.HIGH_GRASS`）：**4 个来源、跨 2 格**

这是「两格高立体草」的完整配方。假设草地格在第 R 行，格子顶边 `T = R × 16`：

| # | 层 / 类 | 帧常量（帧号 = xy） | 来源文件 | 画在哪一格 | 帧内有效行 | 世界 y | 相对角色 |
|---|---|---|---|---|---|---|---|
| ① | `DungeonTerrainTilemap` | `RAISED_HIGH_GRASS` = **122** = `xy(11,8)` | `tiles_*.png` | 草格自己 | 0..15（**满格**） | T..T+15 | **之下** |
| ② | `TerrainFeaturesTilemap` | 帧 **9**（stage 0） | `terrain_features.png` | 草格自己 | 0..14（整株草丛） | T..T+14 | **之下** |
| ③ | `RaisedTerrainTilemap` | `HIGH_GRASS_UNDERHANG` = **250** = `xy(11,16)` | `tiles_*.png` | 草格自己 | 0..10（**实心**） | T..T+10 | **之上** |
| ④ | `DungeonWallsTilemap` | `HIGH_GRASS_OVERHANG` = **234** = `xy(11,15)` | `tiles_*.png` | **上一格** | 8..15（半透明） | T-8..T-1 | **之上** |

ALT 变体（`tileVariance[pos] >= 50`）：`122→125 = xy(14,8)`、`250→253 = xy(14,16)`、`234→237 = xy(14,15)`、`9→10 = xy(11,1)`。

#### 关键发现 1：③ 和 ② 的草叶是**同一批像素**

把 250 与 features 帧 9 逐行比对，**row 0..10 的不透明掩码完全相同**（颜色均值 72,121,60 vs 72,122,60）。
即：同一株草丛先在角色**之下**画一遍当背景，再在角色**之上**画一遍当前景——所以站进草里时上半身会「糊」到草叶前面。

**画法建议**：照原版做——把 features 草丛的**前 11 行原样复制**到 UNDERHANG 槽，前景与背景自然对齐，不会有重影错位。

#### 关键发现 2：高度可以自由分配，只有「行→世界 y」的映射是固定的

```
OVERHANG  帧内 row k  →  世界 y = T - (16 - k)      row15 紧贴草地格顶边，row0 在它上方 16px
UNDERHANG 帧内 row k  →  世界 y = T + k             row0 紧贴草地格顶边，row15 在下方 15px
features  帧内 row k  →  世界 y = T + k             （同 UNDERHANG，画在角色之下）
```

设你想要的草总高 H（**上限 32px**），只要满足：
- **落在草地格内的 H₁ 行** → 画进 `UNDERHANG` 的 row `0..H₁-1`；（同时把整株画进 features 当背景）
- **高出草地格顶边的 H₂ 行** → 画进 `OVERHANG` 的 row `16-H₂..15`。

原版取值 **H₁ = 11、H₂ = 8、H = 19**（实测：UNDERHANG 有效行 0..10，OVERHANG 有效行 8..15）。

> ⚠️ `OVERHANG` 的有效行**别越到 0..7**：那一段会被画到**再上面一格**的中上部，看起来像草长到了远处。
> 想加高就加 `H₂`（最多把 row0..15 全用满＝顶格 16px），而不是挪 `H₁` 的边界。

#### 关键发现 3：OVERHANG 的「顶檐」画法

OVERHANG 帧 234 的逐行实测（不透明像素数 / 最大 alpha / 平均色）：

```
row 8:  4px  a=51   ( 0, 0, 0)      ← 草叶尖端（很淡）
row 9: 10px  a=91   (16,27,13)
row10: 12px  a=102  (22,38,18)
row11: 11px  a=143  (37,63,31)
row12: 12px  a=108  (22,38,18)
row13: 16px  a=119  (19,32,15)      ← 满宽，深——草皮顶边的一道暗线
row14: 16px  a=188  (48,81,40)      ← 满宽，亮绿——草皮顶檐
row15: 16px  a=180  (41,69,34)      ← 满宽，亮绿——紧贴草地格顶边，与草皮无缝衔接
```

**画法**：lowest 3 行（13~15）用**满宽**的半透明深绿/亮绿做成「草皮往上鼓起的顶檐」，
中间几行（8~12）才是草叶的尖端剪影（宽度收窄、alpha 更低）。
`:13` 那行是暗线（把顶檐和草皮分开），`:14/:15` 是亮绿（承接草皮颜色）。这样草叶才是从草皮里「长出来」的。

#### 关键发现 4：草叶在草地格内的「实心」要求

`UNDERHANG` 帧 250 的有效像素 **alpha 全部 = 255**（硬边），而 `OVERHANG` 帧 234 是半透明。
原因：前景草叶会挡住角色，若它半透明就会透出角色的身体，看着像「幽灵」。

### 3.3 角色落点：为什么草能「藏人」

- 角色贴图被整体**抬高 6px**：`CharSprite.perspectiveRaise = 6/16`（`sprites/CharSprite.java:77`），
  所以 12×15 的角色占 `world y = 行×16-5 ~ 行×16+9`，**头探进上一格 5px**。
- 那 5px 正好落进 ④ 的 8px 带（`T-8..T-1`），于是**头顶那条草叶是画在「上一格」里的**。
- 实测（把角色画成纯品红逐像素判定）：
  - 站**高草格本体**（上下也是草）：被盖 **88 / 122 px ≈ 72%**（r0~r4 来自④、r5~r14 来自③）；
  - 站**普通地面**、上下都是高草：只被盖 **7 / 122 ≈ 6%**（仅 r13~r14 脚尖，来自**下方**那格）；
  - **上方那格的草对角色零影响**（它的 UNDERHANG 到 `(行-1)×16+10` 为止，与角色顶边差 1px 擦过）。

### 3.4 让新草地拥有独立（不乱）的 z 序

- `RaisedTerrainTilemap`（第 4 层）与 `DungeonWallsTilemap`（第 5 层）**都在角色之上**，顺序固定，
  所以 ④ 一定画在 ③ 之后 —— 顶檐会盖住你画在 UNDERHANG 里的最上面几行。**别让两个帧的有效内容重复**。
- `DungeonWallsTilemap.skipCells` 是留给「占两格的大型 Boss 立绘」用的（`CrystalSpireSprite` / `FungalCoreSprite`
  往里加格子），会造成 `RaisedTerrainTilemap` 与 walls 层**同时跳过该格**。新地形不需要动它。

---

## 4. 接线清单（照抄）

以「新增 `Terrain.MY_GRASS`，图集 `tiles_myregion.png`，草皮帧 256 起」为例。

### Step 1 放图片 + 注册资源路径

```
core/src/main/assets/environment/tiles_myregion.png     ← 256 宽 × 16 的倍数高
```

`core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/Assets.java` 的 `Environment` 内部类（`:34` 起）加：

```java
public static final String TILES_MYREGION = "environment/tiles_myregion.png";
```

> 不需要任何「预载名单」：`Tilemap` 构造里 `TextureCache.get(asset)` 按路径直接加载（`SPD-classes/.../noosa/Tilemap.java:62`）。

### Step 2 `DungeonTileSheet` 加帧常量

`tiles/DungeonTileSheet.java`，照现有「钻石矿墙」那段（`:319-330`）追加：

```java
	/**********************************************************************
	 * 我的草（Terrain.MY_GRASS）—— 帧号写在 tiles_myregion.png 的追加行
	 *   FLAT 256，RAISED 主 257（+0/+1 两种 ALT），
	 *   OVERHANG 260，UNDERHANG 261
	 **********************************************************************/
	public static final int FLAT_MY_GRASS             = 256;
	public static final int RAISED_MY_GRASS           = 257;
	public static final int RAISED_MY_GRASS_ALT       = 258;
	public static final int MY_GRASS_OVERHANG         = 260;
	public static final int MY_GRASS_OVERHANG_ALT     = 262;
	public static final int MY_GRASS_UNDERHANG        = 261;
	public static final int MY_GRASS_UNDERHANG_ALT    = 263;
```

并登记 ALT 映射（跟在 `commonAltVisuals` 的草那几行后面，`:527-534`）：

```java
		commonAltVisuals.put(RAISED_MY_GRASS,       RAISED_MY_GRASS_ALT);
		commonAltVisuals.put(MY_GRASS_OVERHANG,     MY_GRASS_OVERHANG_ALT);
		commonAltVisuals.put(MY_GRASS_UNDERHANG,    MY_GRASS_UNDERHANG_ALT);
```

> **ALT 必须两边都登记**：`RaisedTerrainTilemap` 与 `DungeonWallsTilemap` 各自独立调 `getVisualWithAlts`，
> 只登记一半会让「本格下悬」和「上一格草檐」随机到不同变体，上下两块**错版**。

### Step 3 `Terrain.java` 加地形 ID 与 flags

`levels/Terrain.java`（`MINE_DIAMOND = 39` 之后，`:70`）：

```java
	//我的草（2026-09-18）：比高草更矮、不挡视线的装饰草
	public static final int MY_GRASS = 40;
```

`flags[MINE_DIAMOND] = flags[WALL];` 之后（`:128`）加：

```java
		flags[MY_GRASS] = PASSABLE | FLAMABLE;      // 视需要：| LOS_BLOCKING
```

> `flags` 是 `new int[256]`（`:83`），地形 ID 可用 40..255；`Level.map` 以 `int[]` 存档
> （`Level.java:475` `bundle.put(MAP, map)` / `:391` `getIntArray`），**新 ID 对旧存档安全**（旧档不会出现该值）。

### Step 4 三处 `getTileVisual` 加分支（**一个都不能少**）

**① 地形层** `tiles/DungeonTerrainTilemap.java` 的 `getTileVisual`（`:42-106`），
插在 `HIGH_GRASS`/`FURROWED_GRASS` 分支之后（`:89-96`）、`else { return NULL_TILE; }` 之前：

```java
			} else if (tile == Terrain.MY_GRASS) {
				return DungeonTileSheet.getVisualWithAlts(
						DungeonTileSheet.RAISED_MY_GRASS,
						pos);
```

**② 下悬层** `tiles/RaisedTerrainTilemap.java` 的 `getTileVisual`（`:35-54`），
插在 `FURROWED_GRASS` 分支之后（`:47-51`）、`return -1;` 之前：

```java
		if (tile == Terrain.MY_GRASS){
			return DungeonTileSheet.getVisualWithAlts(
					DungeonTileSheet.MY_GRASS_UNDERHANG,
					pos);
		}
```

**③ 草檐层** `tiles/DungeonWallsTilemap.java` 的 `getTileVisual`（`:40-120`），
在 `HIGH_GRASS`/`FURROWED_GRASS` 那两条（`:113-117`）后面接着写：

```java
		} else if (pos + mapWidth < size && map[pos+mapWidth] == Terrain.MY_GRASS){
			return DungeonTileSheet.getVisualWithAlts(DungeonTileSheet.MY_GRASS_OVERHANG, pos + mapWidth);
```

> 注意这里**条件查的是「下面那一格」`map[pos+mapWidth]`，但种子传的也是 `pos + mapWidth`**
> ——即取**草格自己**的 `tileVariance`。原版就是这么写的，这样「本格 UNDERHANG」与「上一格 OVERHANG」
> 才会拿到同一个 ALT 变体（122/125 ↔ 250/253 ↔ 234/237 是三三配对的）。

### Step 5 `directFlatVisuals` 必须加（否则 examine 闪退）

`tiles/DungeonTileSheet.java` 的 `directFlatVisuals` static 块（`:459-484`）加：

```java
		directFlatVisuals.put(Terrain.MY_GRASS,         FLAT_MY_GRASS);
```

理由见 §1.3：不加 ⇒ `directFlatVisuals.get(tile)` 返回 `null` ⇒ 拆箱 NPE。

### Step 6（按需）缝合三类表

| 表 | 位置 | 不加会怎样 |
|---|---|---|
| `chasmStitcheable` | `DungeonTileSheet.java:81-121` | 深渊边上的新草不会画「缝合沿」 |
| `waterStitcheable` | `DungeonTileSheet.java:143-151` | 水边的新草不会画接缝 |
| `wallStitcheable` | `DungeonTileSheet.java:238-241` | 新地形**当墙**用时不算墙（如果你的新地形是墙/矿才需要） |
| `GridTileMap` | `tiles/GridTileMap.java:49` | 网格视图下该格不显示地格网格 |
| `Level` 踩踏 | `levels/Level.java:1253-1256` | 踩上去不会触发 `HighGrass.trample`（想「踩倒普通草」就加 `case`） |

### Step 7 覆写 `tilesTex()` 让楼层用新图集

`levels/Level.java:509-511` 默认返回 `null`，各楼层覆写。最简（照 `LobTestLevel.java:170`）：

```java
	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_MYREGION;
	}
```

按条件切（照 `MiningLevel.java:73-81`）：

```java
	@Override
	public String tilesTex() {
		switch (Blacksmith.Quest.Type()){
			default:  return Assets.Environment.TILES_CAVES;
			case Blacksmith.Quest.CRYSTAL: return Assets.Environment.TILES_CAVES_CRYSTAL;
		}
	}
```

> ⚠️ **一套图集要匹配一套语义**。若只改 `tilesTex()` 而不改帧常量，那么在**新图集里没重画的位置**
> 会显示成新图的空白/杂图。路线 A（复制原图集再追加）能规避这点。

### Step 7.5（易漏，必看）草叶细节是**另一张全局图集**，要单独对齐区域

改完 `tilesTex()` **还不够**。地图上的草由**两张图**叠加而成：

| 层 | 图集 | 内容 |
|---|---|---|
| 草皮本体 | 本层 `tilesTex()`（如 `tiles_lob.png`） | 地块底色、长草立绘、上/下悬垂帧 |
| **草叶细节** | **全局唯一** `environment/terrain_features.png` | 草尖、草簇阴影（每格随机 alt） |

`terrain_features.png` 是 256×128、**每 16 个帧槽一组对应一个区域**（槽 9/11/13 + 16×stage 依次为
HIGH_GRASS / FURROWED / GRASS，stage 0~4 = 下水道/监狱/矿洞/都市/恶魔大厅）。选段逻辑在
`TerrainFeaturesTilemap.featuresStage()`，**默认只认 `Dungeon.depth`**：

```java
int stage = (Dungeon.depth-1)/5;   // 深度 >25 一律被下面这行夹到 4 = 恶魔大厅
stage = Math.min(stage, 4);
```

**症状**：自定义楼层贴图的草是 A 区风格，草叶细节却盖着 E 区（恶魔大厅）的**蘑菇形**草叶——
27 层踩过这个坑（深度 27 ⇒ stage 5 ⇒ 夹成 4）。注意**这不是图片配错，是能力边界**。

**解法**：让关卡实现 `TerrainFeaturesTilemap.FeaturesTexProvider`，**申报本层草皮材质实际取自哪个区**：

```java
public class MyLevel extends RegularLevel
        implements TestLevel, TerrainFeaturesTilemap.FeaturesTexProvider {

	/** 本层 tilesTex() 的草尖取自哪个区（0~4 = 下水道/监狱/矿洞/都市/恶魔大厅） */
	public static final int FEATURES_STAGE = 1;   // 例：tiles 用的是监狱材质

	@Override
	public int featuresStage() {
		return FEATURES_STAGE;
	}
}
```

⚠️ **判据是「本层 `tilesTex()` 的草皮材质来自哪个区」，不是「生成期借用了哪个深度」**。
27 层踩过一次：生成期借 `GEN_DEPTH = 4`（下水道）只为房间池/水/BGM，而 `tiles_lob.png` 的草尖其实是
**监狱（第二区）**材质，于是 `(GEN_DEPTH - 1) / 5` 报出的第一区也是错的——**别把布局借位当材质所属区**，
写成具名常量、并用主色指纹核对（见下）。

未实现该接口的关卡**行为与上游完全一致**（原版逻辑原样保留）。核验：
`_chk/verify_lob_grass_stage.py`（源码一致性 + 逐像素 + 主色指纹 + 反例自测 + 回归守卫）、
`_chk/grass_zone_mismatch.py`（terrain_features 五段 vs tiles_lob/tiles_prison 主色对照）。

### Step 8 把新地形铺到地图上

`RegularPainter.paintGrass`（`levels/painters/RegularPainter.java:383-425`）只会写 `GRASS` / `HIGH_GRASS`。
要铺新地形，两条路：

**A. 在自定义 Painter 的 `decorate()` 里后处理**（推荐，照 `MiningLevelPainter` 放钻石矿的写法
`levels/painters/MiningLevelPainter.java:124-143`）：

```java
	// 例：把房间内的高草按 20% 概率换成 MY_GRASS
	for (Room r : rooms){
		for (Point p : r.getPoints()){
			int i = level.pointToCell(p);
			if (level.insideMap(i) && map[i] == Terrain.HIGH_GRASS && Random.Int(5) == 0){
				map[i] = Terrain.MY_GRASS;
			}
		}
	}
```

**B. 在 `Level` 子类的 `paint()` 里、`super.paint()` 之后整体扫一遍**（更粗暴，适合「整层都是这种草」的主题楼层）。

---

## 5. 手绘规范速查（照抄这张表就不会画错）

| 帧角色 | 常量示例 | 不透明像素 | alpha | 有效行 | 说明 |
|---|---|---|---|---|---|
| 地面 / 草皮 | `RAISED_MY_GRASS` | **必须 256**（满格） | 255 | 0..15 | 半透明会漏背景 |
| 抬高地块本体 | `RAISED_WALL*` | **必须 256** | 255 | 0..15 | 同上 |
| examine 平面帧 | `FLAT_MY_GRASS` | **必须 256** | 255 | 0..15 | 只用于小图 |
| 草叶上段 | `MY_GRASS_OVERHANG` | 约 90~100 | 半透明 | **8..15** | row13..15 满宽顶檐，row8..12 草尖 |
| 草叶下段 / 前景 | `MY_GRASS_UNDERHANG` | 约 85~90 | **255 实心** | **0..10** | 与 features 草丛前 11 行同像素 |
| 草叶背景 | `terrain_features.png` | 约 105 | 255 | 0..14 | 整株；行由 `stage` 决定 |

**绘制顺序建议**：
1. 先画草皮帧（满格，先定草的底色）。
2. 再在 `terrain_features.png` 里画整株草丛（0..14 行），确认和草皮颜色接得上。
3. 把草丛 0..10 行**原样复制**到 UNDERHANG 槽。
4. 最后单独画 OVERHANG：row15 贴草地格顶边（＝帧底边），往上画 3 行满宽顶檐 + 几行草尖。

---

## 6. 自检

### 6.1 逐帧核验（新增工具）

```bat
rem 总览：图集尺寸/帧网格，并列出所有空白帧
python _chk/terrain_tileset_check.py core/src/main/assets/environment/tiles_myregion.png

rem 按「地形角色」逐帧断言，并打出 ASCII 供目检
python _chk/terrain_tileset_check.py core/src/main/assets/environment/tiles_myregion.png ^
    ground:257 overhang:260 underhang:261 flat:256
```

工具会按角色报错，例如：`ground` 不足 256px、`overhang` 有效行越到 0..7、`underhang` 从 row≥8 才开始。

### 6.2 草帧结构比对（复刻核验）

```bat
python _chk/grass_frame_audit.py
```

会打印 `tiles_sewers.png` / `terrain_features.png` 里所有草帧的逐行不透明像素数、平均色，
并判定 OVERHANG 与 UNDERHANG 是否互为「同一丛草的上下两半」、UNDERHANG 与 features 是否同像素。
**画完新草后，用它跑一遍做对照**（把路径指向你的新图集与帧号即可）。

### 6.3 编码自检（改完 `.java` 立刻跑）

```bat
python _chk/check_utf8_all.py
```

新写入的中文注释有被工具按 GBK 落盘、行尾汉字末字节被写成 `?` 的风险，会让 Gradle 的
`options.encoding='UTF-8'` 直接编译失败。详见 `AGENTS.md` §2。

### 6.4 编译

按项目约定，编译与打包由**用户**执行；AI 只做源码级核验。

---

## 7. 常见坑

| 现象 | 病因 |
|---|---|
| 点新地形的格子**闪退** | 漏了 `directFlatVisuals` 条目 → 拆箱 NPE（§1.3） |
| 草叶上下两块**花纹不接** | ALT 只登记了一半；或 OVERHANG/UNDERHANG 用了不同 `pos` 取种子 |
| 草**浮在空中 / 长到上一格中上部** | OVERHANG 的有效行越到了 0..7 |
| 草叶半透明处**透出角色的腿** | UNDERHANG 用了非 255 的 alpha（前景应为实心） |
| 换新图集后**别处地形变空白/花屏** | `tilesTex()` 换了但新图集没重画标准 16 行布局 |
| **全物品图标错位**（若同时改过 items.png） | `ItemSpriteSheet.TX_HEIGHT` 没同步实际高度（`AGENTS.md` §3） |
| 网格视图下**该格没有网格线** | 忘了加 `GridTileMap:49` 的判断 |
| 踩上去**不掉露珠 / 不触发踩草** | `RegularLevel` 的植物生成、`Level:1253` 的 `trample` 分派都只认老 ID |
| 新地形当墙用却**不参与墙面缝合** | 忘了加进 `wallStitcheable`（`DungeonTileSheet.java:238`） |

---

## 8. 已实现示例

| 示例 | 时间 | 做法 | 参考点 |
|---|---|---|---|
| `Terrain.MINE_DIAMOND`（钻石矿墙） | 2026-09-04 | 路线 A：`tiles_caves.png` 复制成 crystal/gnoll 两份，高度 256→288，用追加行画 FLAT/RAISED/OVERHANG/INTERNAL 四组 | `DungeonTileSheet.java:319-330`、`Terrain.java:70,128`、`MiningLevelPainter.java:124-143` |
| `tiles_lob.png`（27 层测试层整张新图集） | 2026-09-10 | 整张手绘 256×256 新图，覆写 `tilesTex()` 即用 | `Assets.java:46`、`LobTestLevel.java:170` |
| 高草（原版，本文档的分析对象） | — | 4 个来源跨 2 格：草皮 122 ／ features 帧9 ／ 下悬 250 ／ 草檐 234 | `DungeonTileSheet.java:301,304,404,407,422,425` |

---

## 9. 相关文件与行号速查

| 文件 | 关键位置 |
|---|---|
| `tiles/DungeonTileSheet.java` | `xy()`:36、帧常量 50-426、`wallStitcheable`:238、`directVisuals`:433、`directFlatVisuals`:459、`tileVariance`:491、`commonAltVisuals`:505、`rareAltVisuals`:540、`getVisualWithAlts`:551 |
| `tiles/DungeonTerrainTilemap.java` | `getTileVisual`:42、`tile()`（examine）:108、`needsRender`:115 |
| `tiles/RaisedTerrainTilemap.java` | `getTileVisual`:35 |
| `tiles/DungeonWallsTilemap.java` | `getTileVisual`:40、`skipCells`:31,72 |
| `tiles/TerrainFeaturesTilemap.java` | `getTileVisual`:48-82 |
| `tiles/DungeonTilemap.java` | `SIZE=16`:36、`tileToWorld`:153、`raisedTileCenterToWorld`:163 |
| `levels/Terrain.java` | 地形 ID 26-72、`flags[256]`:83、`static` 块 84-130 |
| `levels/Level.java` | `tilesTex()`:509、`trample` 分派:1253、`map` 存档 391/475 |
| `scenes/GameScene.java` | 图层装配 258-352 |
| `Assets.java` | `Environment` 38-66 |
