# EGOPD 地形编辑器 使用指南

> **一句话**：在浏览器里画房间地形 + 摆陷阱/植物/道具 → 点「生成 Java 代码」→
> 拿到一个可直接编译的 `StandardRoom` 子类 `paint()`，再按文末清单注册进
> `StandardRoom.rooms` 即可。
>
> 入口：双击 `tools/terrain-editor/index.html`（**无需服务器**，图集已 base64 内联）。
> 生成逻辑的正确性有自动化核验兜底（含真 `javac` 编译），见第 6 节。

---

## 1. 快速上手（60 秒）

1. 双击 `tools/terrain-editor/index.html`，浏览器直接打开。
2. 左栏「工具」选 **▭ 矩形**，「房间」里把内部尺寸改成想要的大小，点 **应用尺寸**。
3. 在中间画布上拖框：先框出**地面区**（调色板点 `EMPTY`），再画墙 / 草 / 水 / 深渊。
4. 点 **⌫ 擦除** 或选 `EMPTY` 修边。
5. 想摆陷阱或植物：左栏「覆盖层（陷阱 / 植物 / 道具）」选 **陷阱层** 或 **植物层**，
   下拉挑类型，然后在画布上点。
6. 想摆道具：同一面板切 **道具层**，下拉挑物品（71 种）与**堆类型**（地面 / 宝箱 / 上锁宝箱…）。
   道具**不改地形**，占位图统一是「口粮」。
7. （可选）想让某片地形**每次生成时随机排布**：选 **⚄ 区域随机**，在画布上拖框圈出来
   （可框多片）。区域内地形会保持配比不变、只换位置；区域里有陷阱/植物/道具时，
   勾选 **连同覆盖层一起打乱** 让它们跟着走。
8. （可选）想用**自己画的图集**预览：左栏「外部图集」→ **导入楼层图集 .png**，
   挑一张 16 倍数尺寸的图。导入后下拉会多一项带 ★ 的名字，画布立刻换图。
   **刷新页面也不会丢**（存在浏览器 IndexedDB 里），不想要了点 **清空外部图集**。
9. 右上三件事：
   - 「房间检查」看有没有结构问题（外圈破了、门没开、孤岛、陷阱放在水上等）；
   - 「生成配置」填类名（如 `MushroomCaveRoom`）；
   - 「Java 代码」框里就是成品，点 **下载 .java**。
10. 把文件放进 `core/src/main/java/.../levels/rooms/standard/`，按第 5 节注册。

> 起步状态已经是一个「12×12 空房 + 四向开门 + 中间一片草 + 一个毒镖陷阱 + 一丛向阳草
> + 两个道具（含一个宝箱）」的样板，直接改比从零画快。

---

## 2. 界面导航

```
┌──────────────────────────────────────────────────────────────────────────┐
│ 图集▾  草叶区▾   ↶撤销 ↷重做  保存.json  打开.json  导出PNG  生成Java→  │
├────────────┬──────────────────────────────────┬──────────────────────────┤
│ 工具        │                                  │ 房间检查                 │
│ 房间尺寸    │            画布                   │ 生成配置                 │
│ 一键操作    │   （游戏真实渲染：水层 + 地形     │ Java 代码                │
│ 显示        │   + 草叶/陷阱/植物 + 道具标记）   │                          │
│ 地形调色板  │                                  │                          │
│ 覆盖层      │  状态栏：格 / 地形 / 房间尺寸      │                          │
│ 区域随机    │                                  │                          │
│ 外部图集    │                                  │                          │
│ 快捷键      │                                  │                          │
└────────────┴──────────────────────────────────┴──────────────────────────┘
```

### 2.1 顶部：图集 / 草叶区

这两个下拉框决定**画布预览长什么样**，与生成的代码无关（代码只写 `Terrain` 常量）：

| 控件 | 作用 |
|---|---|
| **图集** | 选 `tiles_sewers / prison / caves / city / halls / lob`。对应游戏里 `Level.tilesTex()` 返回的那张 256×256 图集 |
| **草叶区** | 选 0–4。**这是两个下拉框里更容易错的那个**：草叶细节来自**另一张全局图集** `environment/terrain_features.png`，按「区」取帧。`0 下水道 / 1 监狱 / 2 矿洞 / 3 都市 / 4 恶魔大厅` |

> **为什么草叶要单独选？** 草是**两张图叠出来的**：草皮在 `tiles_*.png`（跟随「图集」），
> 草叶花纹在 `terrain_features.png`（跟随「草叶区」，全局唯一一张）。
> 这就是 `AGENTS.md` §6 里那条「草地显示成五区蘑菇形」坑的本体 ——
> 编辑 27 层这类自定义楼层时，如果画布上草的叶子形状和图集不搭，先来这里换「草叶区」。

### 2.2 左栏：工具

| 工具 | 快捷键 | 说明 |
|---|---|---|
| ✎ 画笔 | `B` | 按住拖动连续画，**整段算一次撤销** |
| ╱ 直线 | `L` | 拖出两端，Bresenham 直线 |
| ▭ 矩形 | `R` | 拖框；勾「实心」= 填充，不勾 = 只画边框 |
| ◯ 椭圆 | `E` | 拖框内切椭圆；同样受「实心」影响 |
| ▨ 填充 | `F` | 4 连通洪水填充（等价 `Painter.fill` 的逐格语义） |
| ◎ 吸管 | `I` | 拾取格子的地形为当前笔刷 |
| ⌫ 擦除 | `X` | 直接画成 `EMPTY` |
| ✥ 拖动 | `空格` | 画布比窗口大时平移视图 |
| ⚄ 区域随机 | — | 拖框标记「生成时随机排布」的区域（可多片，重叠会合并） |
| ⚄ 擦区域 | — | 拖框擦掉**整片**区域（不是挖洞，见 2.7） |

- **右键 = 吸管**（任何工具下都生效）。
- **中键 = 平移**。
- **Ctrl + 滚轮 = 缩放**，或左上「显示 → 缩放」选 1×–4×。
- 「**锁定外圈**」默认勾上：外圈（第 0 行/列、最后一行/列）只允许放墙类地形，
  防止手滑把墙刷成草导致房间漏风。

### 2.3 左栏：一键操作

| 按钮 | 作用 |
|---|---|
| **铺骨架** | 外圈全 `WALL` + 内圈全 `EMPTY` —— 就是 `paint()` 两步骨架的可视版 |
| **四向开门** | 内圈四边中点各开一扇 `DOOR` |
| **左右 / 上下镜像** | 对称房间省一半工。**注意：镜像/旋转只是画图辅助**，不是模拟引擎行为 —— SPD 生成房间时**不旋转也不镜像**（见下方注） |
| **旋转 90°** | 要求宽高相等，否则提示并自动撤销 |
| **铺满空地** | 内圈全填 `EMPTY`，保留外圈 |

> **房间方向是锁死的**：`RegularPainter.paint()` 对每个房间只做 `r.shift(-leftMost, -topMost)`，
> 全项目 grep `rotate|mirror|flip|transpose` 在 `levels/` 下**零命中**。
> 只画了朝下开门的房间，生成时就只会有朝下开门的版本。
> 想要多方向，只能画多个房间类（或让生成权重覆盖多个深度）。

### 2.4 左栏：显示

| 开关 | 作用 |
|---|---|
| **草叶/陷阱/植物层** | 关掉只剩草皮与纯地形，方便确认底层地形对不对 |
| **道具层（口粮占位图）** | 关掉不画物品标记 |
| **水体层** | 关掉不画水（用于对照：水是**独立图层**，不关掉才能看出和水渠里有没有水） |
| **网格线** | 16px 格线 + 外圈红框 |
| **房间语义底衬** | 外圈染红（=墙语义，不可随便改）、内圈染蓝（=可编辑）。**对齐 `StandardRoom` 的约定：外圈是边界、内圈是内容** |
| **区域标记** | 画上「区域随机」的斜纹底 + 虚线框（纯编辑期辅助，不影响生成结果） |

### 2.5 左栏：覆盖层（陷阱 / 植物 / 道具）

陷阱、植物、道具**都不是地形**。三条分别是挂在 `Level.traps` / `Level.plants` /
`Level.heaps` 上的**独立对象层**。把它们和地形分开管，是因为踩过一个很深的坑：

> **只写 `Terrain.TRAP` 不会产生陷阱。** 那只会得到一块「陷阱地板皮」，
> 真正让陷阱存在的是 `level.setTrap(new XxxTrap(), pos)`。
> `TerrainFeaturesTilemap.getTileVisual()` 的判断顺序是：
> ① `traps.get(pos)` 命中 ⇒ 用陷阱的颜色/形状画；② `plants.get(pos)` 命中 ⇒ 画植物；
> ③ 才轮到 `GRASS` / `HIGH_GRASS` / `FURROWED_GRASS` / `EMBERS`。

| 控件 | 说明 |
|---|---|
| 四个单选：**不画层 / 陷阱层 / 植物层 / 道具层** | 决定画笔落在哪一层。选「不画层」就是原来的纯地形画笔 |
| **陷阱类型** | 33 种（对齐 `levels/traps/`），下拉项显示「中文名 (类名)」 |
| **已发现** | 勾上 ⇒ 地形写 `TRAP`；取消 ⇒ 写 `SECRET_TRAP`，游戏里**看不见**，踩上去才触发 |
| **生效中** | 取消后生成代码会附注「如何保持失活」（`trap.active = false`）。编辑器里用灰色标记 |
| **植物类型** | 13 种（对齐 `plants/`），帧号 = `image + 7*16` |
| **道具类型** | 71 种（对齐 `items/` 全包），按 7 组下拉分类；下拉项显示「中文名 (类名)」 |
| **堆类型** | 7 种：`HEAP`（默认地面散落）/ `FOR_SALE` / `CHEST` / `LOCKED_CHEST` / `CRYSTAL_CHEST` / `TOMB` / `SKELETON` |

画布上每格会叠**小标记方块**（游戏里区分不出是哪一种陷阱/物品，编辑时必须能认）：
陷阱在**左下角**（绿=已发现且生效 / 灰=已发现但失活 / 黄=未发现），植物在**右下角**（青），
**道具在正中间**（黄点=普通堆 / `#ffcc44`=宝箱 / `#e08adf`=墓穴·骸骨等特殊堆）。
道具的**占位图就是游戏里的口粮**（见 3.2），所以你看到的形状和游戏里一致。

> `SECRET_TRAP` 与 `INACTIVE_TRAP` 用的都是陷阱自己的颜色帧 —— 只是**不画**而已
> （`visible` 为假时 `getTileVisual` 返回 `-1`），所以编辑器在预览里也不画它，
> 只留那个黄色小方块提示「这里有个隐藏陷阱」。
>
> **道具为什么不改地形**：物品存在 `Level.heaps` 里，`Heap` 自带一个 `ItemSprite` 挂在
> `GameScene` 的 objects 组里 —— **地图格本身还是普通地板**。所以画道具**不会**把地形刷成
> 任何东西；同一格可以既是草地又是宝箱。

### 2.6 左栏：外部图集

用来预览**你自己画的**图集（比如给新楼层做了一套贴图，想先看看画出来长什么样）。

| 按钮 | 影响的东西 | 尺寸要求 |
|---|---|---|
| **导入楼层图集 .png** | 画布上**地形/墙体**的贴图（对应 `Level.tilesTex()`） | 16 的倍数 |
| **导入草叶图 .png** | 草的**叶子花纹**那一层（对应全局唯一的 `environment/terrain_features.png`） | 16 的倍数 |
| **导入道具图 .png** | 道具占位图的来源（`items.png`）；换掉它就能看到你改过的物品图标 | 16 的倍数 |
| **清空外部图集** | 以上全部恢复成内置的 6 张 | — |

导入成功后：

- 「图集」下拉里会**多出一项带 ★ 的名字**（★ = 外部图集）并自动选中；
- 画布立刻换成新图；「外部图集」面板底下的提示行会写成
  「楼层图集 1 张 · 草叶图 0 张 · 道具图 0 张」；
- 生成的代码注释里写的是**你那张图的名字**（人话），不是内部 key；
- **刷新页面不会丢** —— 存在浏览器 IndexedDB 里，下次打开自动恢复。

几个约定：

- **同类只保留一张**：再导入一张楼层图集，会把上一张**卸下并删除**（不是叠着放）。
  所以「导入错了再导入一次」就是正确的改法。
- **尺寸必须是 16 的倍数**，否则直接拒绝（弹提示，不进下拉）—— 因为引擎按
  `(idx % 16, idx / 16) * 16px` 取帧，非整倍图会整体错位。
- **楼层图集之间是「换整张皮」**：只导入楼层图集时，草叶细节仍用内置那份，
  于是就会出现「草皮是新图、叶子是旧形状」的混搭。想彻底预览新风格，**三张一起导入**。

> **为什么不用 localStorage 存？** 图集动辄 100–200 KB，localStorage 只能存字符串
> （要 base64，体积膨胀 33%）且配额约 5 MB，还要同步读写。IndexedDB 能直接存 **Blob**、
> 异步、配额大得多 —— 所以外部图集走 IndexedDB。

### 2.7 左栏：区域随机

「框一块地方，让里面的地形在**每次实际生成时**重新排布」—— 比如一片碎石地、一片
野草丛，你不在乎哪一格是哪一种，只在乎**配比**和**整体观感**。

| 控件 | 作用 |
|---|---|
| 工具 **⚄ 区域随机** | 拖框即标记（可框多片，重复框会**合并成一片**） |
| 工具 **⚄ 擦区域** | 拖框擦掉**整片**区域（不是擦出洞 —— 见下方约定） |
| **连同覆盖层一起打乱**（默认勾选） | 勾上 = 区域里的陷阱/植物/道具跟着地形一起移动；取消 = 只动地形 |
| **清空全部区域** | 一键删掉所有标记（可撤销） |
| 显示面板的 **区域标记** | 画布上是否画那片斜纹底 + 虚线框（默认开） |

标记后提示行会写「已标记 N 片区域，共 M 格（生成时打乱 地形 + 覆盖层 / 仅地形）」。
**如果区域里有覆盖层对象而你没勾那个开关**，提示行会变黄并明确写出「⚠ 区域内还有 N 个
覆盖层对象，勾上才不错位」—— 因为那种情况下地形换了位置、陷阱还留在原地。

### 语义：打乱的是**多重集**，不是逐格重掷

这是本功能最要紧的一条，也是唯一「看不出来但会毁掉设计」的地方：

- **正确做法（本编辑器的做法）**：把区域内的地形值按行优先**收集成数组** → `Random.shuffle`
  → 按同一顺序**写回原格**。区域里 5 草 / 2 水 / 1 墙，生成后还是 5 草 / 2 水 / 1 墙，
  **只是位置变了**。
- **错误做法（已否决）**：对每一格 `Random.oneOf(...)` 重掷。那样**配比会被随机掉** ——
  作者精心安排的「一小片水」可能变成一片汪洋。

所以生成的代码是：

```java
int[] r0 = new int[]{ /* 区域内的地形值，行优先 */ };
Random.shuffle( r0 );
int[] r0Pos = new int[]{ level.pointToCell(new Point(left + 1, top + 1)), /* ... */ };
for (int k = 0; k < r0.length; k++) Painter.set( level, r0Pos[k], r0[k] );
```

### 顺序不变量：区域段必须在**所有地形填充之后**

区域写回用的是 `Painter.set`，所以如果它被排在骨架铺墙/掏空**之前**，就会**被后面的
填充整片覆盖** —— 而且**完全不报错**，生成物字符串层面也照样有 `shuffle` 和写回，
肉眼看不出问题。本编辑器把区域固定成生成流程的**第 4 步**（地形之后、覆盖层之前），
并由 `_chk/verify_random_regions.js` 的 D 节**按行号**钉住这条（反例自测会真的把这一块
挪到填充之前，验证该断言确实会红）。

### 覆盖层联动：为什么不用 `Random.shuffle(u, v)`

`Random` 有 `shuffle(U[] u, V[] v)` 的同步重载，但这里**故意不用** —— 三个覆盖层的数组
类型各不相同（`Trap[]` / `Plant.Seed[]` / `Item[]`），配上地形就是四种，重载根本不匹配。
改用一张**显式置换表** `int[] ord`：地形和每一层都用**同一个 `ord`** 重排，联动关系一眼可见，
也便于核验。生成的代码形如：

```java
int[] r0Ord = new int[16];
for (int k = 0; k < r0Ord.length; k++) r0Ord[k] = k;
Random.shuffle( r0Ord );
int[] r0Old = r0.clone();
for (int k = 0; k < r0.length; k++) r0[k] = r0Old[r0Ord[k]];
/* 覆盖层各层也用 r0Ord 重排（此处略） */
```

### 区域拓扑的三条约定

- **区域是矩形、闭区间**（与 `Painter.fill` 的 `(l, t, r, b)` 同语义）。
- **两片区域永不相交**：`addRegion` 会把重叠区域**迭代合并**成一片，直到稳定。所以
  「先框一块小的、再框一块大的把它包住」的净效果是**一片大的**，不是两片。这条是为了让
  「打乱单位」始终明确定义 —— 一旦允许相交，同一格可能被两次打乱，语义就崩了。
- **擦除是整片删**：擦框落在哪片区域上，整片就没了（不做「挖洞」）。同样是因为「区域
  是打乱单位」，一个残缺形状没法表达成一次 shuffle。想改形状就「擦掉重框」。

> **退化区域的两种提示**（检查器给，不是报错）：只有 1 格的区域（打乱等于没打乱，生成时
> 会被**跳过**）和整片同一种地形的区域（打乱后画面不变）。两种都不影响编译，只是让作者
> 知道「你标了但它没用」。

> **JSON 版本**：区域标记存为 `randoms` 数组，从 **v4** 起写入。老档（v1~v3）读进来
> `randoms` 是空数组；越界矩形会被**夹进图内**（应对「先框区域后改小房间」），
> 反向矩形（`r < l`）直接剔除。

---

### 2.8 左栏：尺寸模糊化

「让这个房间**每次生成时尺寸都不一样**」—— 同一个房间设计可以是大一点或小一点的变体，
从而在关卡里不显得重复。

> ⚠️ **不支持复杂的具体地形。** 房间缩小时**只有右/下边界在收**（左上角不动），
> 所以靠右、靠下的内容会被裁掉。主图案请画在画布上的**绿框安全区**里。

| 控件 | 作用 |
|---|---|
| **启用尺寸模糊化**（默认关） | 关 = 生成的代码里不出现 `sizeCatProbs` / `setSize`，房间尺寸固定 |
| **最小/最大 宽、高** | 随机范围，单位是**总宽高（含外圈墙）**，可填 4~18 |
| **快捷 小 / 中 / 大** | 一键填 `7~9` / `8~10` / `10~14`（宽高同步） |
| **缓冲** | 安全区内缩几格（0~8）。0 = 内容可以贴满最小尺寸的房间，没有容错余量 |
| **显示安全区参考线**（默认开） | 画布上是否画下面的三层指示 |

画布上会叠三层（语义从强到弱）：

1. **绿色实框 + 浅绿底 + 四角直角** —— **安全区**。框内**任何尺寸下都存在**。
2. **橙色虚线框** —— **最大尺寸边界**。房间最大时到这里；比它更右/更下的内容**永远不存在**。
3. **淡红遮罩** —— 安全区之外的那一圈，提示「这些格可能被切」。

面板底部的提示行是**实时**的，会写出随机范围、选中的 `SizeCategory`、安全区的行列范围，
以及三类警告：

- ⚠ **范围无解** —— 没有任何尺寸档能装下你填的范围（见下方「装不下的判断」）。
- ⚠ **画布比上限还大** —— 超出的部分永不存在，那里的内容会被整体丢弃。
- ⚠ **画布比下限还小** —— 房间最小时也放不下整个画布的内容。
- **安全区之外有 N 格非空内容** —— 会全部被夹进安全区，可能被压变形。

#### 为什么安全区由**最小**尺寸决定

`Room.resize(w, h)` 只改 `right` / `bottom`（`Rect.resize` = `set(left, top, left+w, top+h)`），
`left` / `top` **恒定**。所以房间变小时是**从右下往左上收**：

```
left,top ────────────────┐        ← 左上角永远不动
   │  安全区（一定会存在）  │
   │        ┌─────────────┤ ← 最小尺寸时的右下边界
   │        │  可能被切掉   │
   └────────┴─────────────┘
```

安全区的右下边界因此是 `min(画布, minW) - 1 - guarded`，而不是画布尺寸。
`editor.js` 的 `safeRect()` 与 `codegen.js` 的 `fuzzSafeRight()/fuzzSafeBottom()` 是
**同一套算术的两份实现**，`_chk/verify_size_fuzz.js` 的 A 节会交叉比对两者 —— 包括
**关闭模糊化时**都要退化成整块画布。两处一旦漂移，编辑器画的框与生成器夹的边界就不是
同一个，作者看着「在框内」的地方反而被夹。

#### 越界内容：夹取（clamp），不是重排

生成代码时，所有地形矩形、覆盖层坐标、区域随机范围都会被**夹进安全区**，并在那一行末尾
加注 `// ⚠ 模糊化：已夹进安全区（原 l=…, t=…, r=…, b=…）`。

**为什么选夹取而不是「按最小尺寸重排整个图案」**：重排会彻底改变作者的构图（原本靠右的墙
可能跑到中间），而且对**连通性**的破坏不可预测（可能把一条走廊拆成两段）。夹取至少保证：

1. 内容仍然贴在同一个**方向**上（靠右的东西仍然靠右，只是不再超出）；
2. 不存在越界写入（不会画到房间外面去）；
3. 结果**可预测**，作者在编辑器里看到的安全区就是最终的安全区。

**代价**：夹取会**合并**相邻格（原本 3 格宽的装饰可能被压成 1 格）。所以
**夹取是兜底，不是推荐做法** —— 面向模糊化的房间应当把主图案画在安全区内、把「溢出部分」
当作可有可无的装饰。

#### 生成物：`sizeCatProbs()` 与 `setSize()` **必须成对**

生成的类里会多出两块，缺一不可：

```java
/* ⚠️ 尺寸模糊化：下面的 sizeCatProbs() 覆写与 paint() 开头的
 * setSize(...) 必须**成对存在**，缺一不可 —— … */
@Override
public float[] sizeCatProbs(){
	// 只允许 NORMAL（4~10），其余类别权重为 0
	return new float[]{1, 0, 0};
}

@Override
public void paint( Level level ) {
	// 0) 尺寸模糊化：在类别允许的范围内随机取一个尺寸。
	//    ⚠ 必须与下面的 sizeCatProbs() 覆写成对出现 ——
	setSize( 8, 10, 8, 10 );
	…
}
```

- **只写 `setSize`、不覆写 `sizeCatProbs()`** ⇒ `sizeCat` 仍是 `NORMAL`，
  `setSize` 里的 `minW < minWidth() || maxW > maxWidth()` 检查失败，
  **静默返回 `false`**（什么都不发生、也绝不报错）。这行代码等于没写。
  —— `Room.setSize(int,int,int,int)` 的返回类型就是 `boolean`（字节码
  `setSize:(IIII)Z`），这正是它「静默失败」的形态。
- **只覆写 `sizeCatProbs()`、不写 `setSize`** ⇒ 尺寸由类别随机，但**范围是类别的**
  `4~10` / `10~14` / `14~18`，不是你填的那四个数。

#### 「装不下」的判断

`SizeCategory` 对宽高共用同一个 `[min, max]`，所以编辑器把四个数**合成一个区间**看待
（`needMin = min(minW, minH)`、`needMax = max(maxW, maxH)`），去找能同时装下两端的类别：

| 填写范围 | 结果 |
|---|---|
| `8~10` | ✅ `NORMAL(4~10)` |
| `10~14` | ✅ `LARGE(10~14)` |
| `14~18` | ✅ `GIANT(14~18)` |
| `6~12` | ❌ `NORMAL` 装不下 12、`LARGE` 装不下 6 ⇒ **无解** |
| `8~14` | ❌ `NORMAL` 装不下 14、`LARGE` 装不下 8 ⇒ **无解** |
| `9~13` | ❌ 同上 ⇒ **无解** |

无解时 `fuzzCategoryFor()` 会退到能覆盖**上界**的类别（保证生成的代码不会因为
`maxW` 超界而整体失效），但提示行会明确报 ⚠「没有任何尺寸档能装下…生成的 `setSize`
会被静默拒绝，房间尺寸不会生效」。**请把范围收进单一档内。**

> 边界是**闭区间**：`10~10` 在 `NORMAL` 与 `LARGE` 里都合法（选范围更窄的那个），
> 而 `4~10` / `10~14` / `14~18` 都能完整装下自己那一档。

#### 两个易踩的细节

- **尺寸的单位是总宽高（含外圈墙）**，与左栏「房间尺寸」的**内部值差 2**。
  「内部 12」⇒ 画布 14×14 ⇒ 落在 `maxW = 10` 之外，会报 ⚠ 画布比上限大。
- **翻转过的 min/max 会被悄悄换回来**，并**回写到输入框** —— 所以面板上显示的值
  永远等于实际生效的值（不留「看着是 99、实际是 18」的坑）。

> **JSON 版本**：模糊化配置存为 `fuzz` 对象，从 **v5** 起写入。老档（v1~v4）读进来是
> 「关闭」；四个尺寸与 `guarded` 逐个取且有默认值（不是 spread，否则老档会写出
> `undefined`）；越界值夹进 `4~18` / `0~8`，颠倒的 min/max 交换。

---

## 3. 地形调色板

按用途分了 6 组，共 39 种地形，全部与 `levels/Terrain.java` 的常量一一对应
（按钮 tooltip 里会显示 `常量名 = 数值`）。

| 分组 | 含 |
|---|---|
| 基础地面 | `EMPTY` `GRASS` `HIGH_GRASS` `FURROWED_GRASS` `EMBERS` `WATER` `CHASM` |
| 墙体 | `WALL` `WALL_DECO` `BOOKSHELF` `STATUE` `STATUE_SP` |
| 门与出入口 | `DOOR` `OPEN_DOOR` `LOCKED_DOOR` `CRYSTAL_DOOR` `SECRET_DOOR` `LOCKED_EXIT` `UNLOCKED_EXIT` `ENTRANCE` `EXIT` |
| 交互物 | `PEDESTAL` `ALCHEMY` `WELL` `EMPTY_WELL` `BARRICADE` `CUSTOM_DECO` `CUSTOM_DECO_EMPTY` `EMPTY_DECO` `REGION_DECO` `REGION_DECO_ALT` `EMPTY_SP` `ENTRANCE_SP` |
| 陷阱与矿脉 | `TRAP` `INACTIVE_TRAP` `SECRET_TRAP` `MINE_CRYSTAL` `MINE_BOULDER` `MINE_DIAMOND` |
| 英雄相关 | `HERO_LKD_DR` |

> 画布渲染的是**游戏真实贴图**（层 z 序完全复刻 `GameScene` 的图层装配），
> 所以你看到的就是进游戏后看到的样子 —— 包括墙面自动缝合、草叶跨格拼接、
> 水/深渊边缘过渡。

### 3.1 水为什么单独一层（修过的 bug）

早期版本**水看起来和深渊一模一样**（都是黑的），原因是照抄了 Java 的
`DungeonTerrainTilemap.needsRender()`：

```java
return super.needsRender(pos) && data[pos] != DungeonTileSheet.WATER;
```

**纯水格是被跳过的** —— 因为水根本不是 tilemap 画的，而是 `GameScene` 里一个单独的
`SkinnedBlock`（`new SkinnedBlock(w*16, h*16, level.waterTex())`）铺满整张地图、
带 5 帧动画（`water0..water4`，每帧 32×32 = 2×2 格）。

编辑器只学会了「跳过」没学会「垫底」，于是水格下面是黑的。

**现在的做法**：`render()` 先铺一层 **layer 0**（32×32 平铺水帧），再走地形 pass；
地形 pass 里水格只在**是纯水基础帧**（`wv === F.WATER`，即缝合位 `r === 0`）时跳过，
**缝合边缘帧照画**（否则岸线会缺一圈）。

> 缝合位：上=+1 右=+2 下=+4 左=+8；`r === 0` 就是「四周都是水」。
> 地图边界外 `at()` 返回 `-1`，**不可缝合**，所以边上的水格会带缝合位 —— 这是对的。

### 3.2 道具占位图为什么是「口粮」

道具**不渲染真实图标**（用户要求：统一用口粮占位）—— 因为 71 种物品各画各的图，画布会乱成
一锅粥，而且编辑阶段只需要知道「这里有个东西」。

占位帧号是**算出来的，不是猜的**：

```
ItemSpriteSheet.SIZE = 16, TX_WIDTH = 256  ⇒  WIDTH = 256 / 16 = 16
xy(x, y) { x -= 1; y -= 1; return x + WIDTH * y; }   // 1-based 输入
FOOD   = xy(1, 28) = 0 + 16*27 = 432
RATION = FOOD + 5  = 437   ⇒  col 5, row 27  ⇒  像素 (80, 432)
```

`items.png` 与地形图集**不是同一张图**（它在 `core/src/main/assets/sprites/items.png`，
不在 `environment/` 下），所以编辑器把它当**独立图集** `S.itemSheet` 单独加载，
并在 `assets.js` 里单独内联。

> **两个坑（都踩过）**：
> ① **帧号是 0-based 的列偏移**：`assignItemRect` 用 `(item % WIDTH) * SIZE`，
>    我第一版按 `(col-1)*16` 算，于是探针报告「437 是空帧」—— **是探针错了，不是推导错**。
> ② **`Sheet.prototype.draw()` 对任何合法索引都返回 `true`**（哪怕这一帧全透明），
>    所以 `if (!drew)` **不是**有效的空帧判据，必须用 `solidCount(idx) > 0`。
>    编辑器里就靠这条决定「能画口粮就画口粮，不能就退化成橙黄十字」。

这套推导有**三层独立证明**兜底（`_chk/verify_item_placeholder.py`，16 项）：
① Java 的 `xy()` 公式算出的 437 与硬编码常量一致；② 拿**真 `items.png`**（256×800）
自写 PNG 解码器逐像素验帧 437 非空、邻帧空作对照；③ 假 canvas 里断言 `render()` 真的发了
`drawImage(src 80,432 → dst 32,32 / 64,64)`。

---

## 4. 房间检查（左上「重新检查」）

编辑器不只画图，还会把 `StandardRoom` 的常见踩坑点做成即时提示。九类：

| 级别 | 触发条件 | 为什么要管 |
|---|---|---|
| ⚠ 外圈破损 | 外圈有格既不是墙也不是门 | `StandardRoom.paint` 默认把外圈当墙；刷成草会让玩家走出地图 |
| ⚠ 门不在外圈 | 门出现在内圈 | **门是房间边界的语义**；中间开门不会生成关卡连接，`placeDoors` 找不到它 |
| ⚠ 孤岛 | 可通行格被墙隔成不连通 | 玩家走不过去 = 白画 |
| ⚠ 全深渊 | 内圈全是 `CHASM` | 玩家无处落脚 |
| ⚠ 图集缺帧 | 当前图集在所选「草叶区」下没画某地形的地基帧 | 手绘新图集常只画部分槽位；选中空槽位会显示成空白格（看着像花屏） |
| ⚠ 陷阱位置可疑 | 陷阱被放在 `WALL` / `WATER` / `CHASM` 上 | 玩家走不到的地方放陷阱 = 白放（`WATER` 上更是永远触发不了） |
| ⚠ 道具位置可疑 | 道具被放在 `WALL` 上 | 墙里的物品玩家永远拿不到（`Heap` 无法被拾取） |
| ℹ 贴墙高草 | 高草被墙完全包住 | 游戏里会显示成贴墙草叶，确认是否刻意 |
| ℹ 无门 | 一个门都没有 | 若该房间要参与关卡连接，需要至少一扇门 |
| ℹ 陷阱 / 植物 / 道具计数 | 统计各层对象数 | 提醒玩家「它们是独立对象层，不是地形」，顺便确认没漏画 |

全绿时显示「✓ 未发现结构问题」。

> **几条判据的边界（踩过，已写成单测）**：
> - 门在**四条边**上都算合法外圈内容 —— 早期只给左右两列放行，四向开门会被误报「外圈破损」。
> - **`WATER` 与门/出入口类不参与「图集缺帧」检查**：水不走 tilemap（`needsRender` 跳过纯 WATER 帧，
>   水体是 `SkinnedBlock` 动画层），门的图来自 raised 层的 DOOR 帧 ⇒ 它们的「地基帧」是空槽位属正常，
>   拿来判缺失必然是假报。
> - 陷阱检查是**反着来的**：陷阱对象存在、但该格地形不是 `TRAP`/`SECRET_TRAP`/`INACTIVE_TRAP` ⇒ 警告
>   （说明层与地形不一致，生成出来的代码会「有陷阱对象却没有陷阱地板皮」）。
> - **道具只查 `WALL`**，不像陷阱那样要求地面 `TRAP` 系：物品本来就落在**普通地板**上
>   （`Level.heaps` 与 `map[]` 平行），所以「道具所在格地形没变」才是**正确**的 ——
>   这是最容易搞反的一条。`CHASM`/`WATER` 上都允许放（浮空道具与水上道具在 SPD 里都存在）。

---


## 5. 生成代码 → 落地注册

### 5.1 生成配置

| 字段 | 说明 |
|---|---|
| **类名** | 生成 `public class <类名>`。必须是合法 Java 标识符，非法会被静默回退成 `MyTerrainRoom` |
| **父类** | `StandardRoom`（参与随机房型，需权重）或 `Room`（纯地形房，一般配合 `TERRAIN_ONLY_ROOMS`） |
| **包名** | 默认 `...levels.rooms.standard` |
| **附带注册清单说明** | 勾上会在文件末尾追加一段 `//` 注释，写明下面 5.3 的四步 |

### 5.2 生成出来的代码长什么样

生成器**不是**逐格展开 `Painter.set`（那会是一个 200 行的怪物），而是做**矩形合并**：
同地形的最大矩形合并成一条 `Painter.fill`，零星单格才退化成 `Painter.set`。
一个 12×12、含 5×4 草地 + 3×3 深渊 + 1 格宽水渠的复杂房间，
实测只生成 **17 条语句**（6 个 fill + 11 个 set），且与网格**逐格等价**（第 6 节有证明）。

```java
package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.watabou.utils.Point;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.FrostTrap;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;

public class MushroomCaveRoom extends StandardRoom {

	@Override
	public void paint( Level level ) {

		// 1) 骨架：整间铺墙，再把内部掏空 —— 与上游每个 StandardRoom 完全一致
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY );

		// 2) 外围墙上的开口 / 特殊墙（门、雕饰、隐藏门等）
		Painter.set( level, left + 7, top + 0, Terrain.DOOR );
		Painter.set( level, left + 3, top + 0, Terrain.SECRET_DOOR );

		// 3) 内部地形（按面积从大到小，已自动合并成矩形填充）
		// 草地 × 20 格
		Painter.fill( level, left + 2, top + 2, 5, 4, Terrain.GRASS );
		// 深渊 × 9 格
		Painter.fill( level, left + 7, top + 8, 3, 3, Terrain.CHASM );
		// 陷阱（可见） × 1 格
		Painter.set( level, left + 3, top + 3, Terrain.TRAP );

		// 4) 陷阱：陷阱**不是地形**。只写 Terrain.TRAP 只会得到一块地板皮，
		//    真正让陷阱存在的是下面的 level.setTrap(...)。
		//    setTrap 内部已做 traps.put + GameScene.updateMap，无需再 Painter.set。
		// 冰霜陷阱（FrostTrap：色 WHITE / 形 STARS）
		level.setTrap( new FrostTrap(), level.pointToCell( new Point( left + 3, top + 3 ) ) );

	// 5) 植物：入参是 Plant.Seed，Level.plant() 内部会 couch() 出 Plant 并补地形。
	//    上游 13 种植物都带同名内部 Seed 类（Rotberry.Seed / Firebloom.Seed …）。
	// 向阳草（Sungrass）
	level.plant( new Sungrass.Seed(), level.pointToCell( new Point( left + 5, top + 5 ) ) );

	// 6) 道具：物品存在 Level.heaps 里（与 map[] 平行），**不改动地形**。
	//    必须用 level.drop(item, cell) —— 这是唯一定点投放入口（返回 Heap，可链式改堆类型）。
	//    ⚠️ 不要用 addItemToSpawn：那只把物品推进 itemsToSpawn 队列，
	//       随后由 RegularLevel.createItems() 通过 randomDropCell() 消费 ⇒ 落点是**随机的**。
	// 口粮（地面散落）
	level.drop( new Food(), level.pointToCell( new Point( left + 4, top + 2 ) ) );
	// 升级卷轴（宝箱）
	level.drop( new ScrollOfUpgrade(), level.pointToCell( new Point( left + 7, top + 8 ) ) ).type = Heap.Type.CHEST;
	// 铁钥匙
	level.drop( new IronKey( Dungeon.depth ), level.pointToCell( new Point( left + 2, top + 6 ) ) );

	// 7) 统一设置门的类型（可按需改为 Door.Type.LOCKED 等）
	for (Room.Door door : connected.values()) {
		door.set( Room.Door.Type.REGULAR );
	}
}
}
```

**坐标为什么写 `left + x` / `top + y`？** 因为 `left/top/right/bottom` 是
`RegularPainter` 实际分配给你的房间矩形，画的时候只能用**相对偏移**。
生成器把画布上的 `(x, y)` 直译成 `left + x, top + y`，所以你画在哪就落在哪。

> **`right` / `bottom` 是闭区间**（`Room extends Rect`），宽度 = `right - left + 1`。
> 这也是生成器用 `fill(level, l, t, w, h, v)` 的**宽高版重载**而不是 `l,t,r,b` 版的原因：
> 宽高从画布直接得来，不需要再做一次 ±1 换算，少一个出错点。

**陷阱 / 植物 / 道具为什么不能省掉那几段？** 这几句的**签名很容易写错**，而且是静默失败：

| 层 | ✅ 正确 | ❌ 常见错法 | 错法后果 |
|---|---|---|---|
| 陷阱 | `level.setTrap( new FrostTrap(), pos )` | `level.setTrap( new FrostTrap() )` / 只写 `Terrain.TRAP` | 编译错 / 有地板没陷阱 |
| 植物 | `level.plant( new Sungrass.Seed(), pos )` | `level.plant( new Sungrass(), pos )` | **编译错**：`不兼容的类型: Sungrass无法转换为Seed` |
| 道具 | `level.drop( new Food(), pos )` | `level.addItemToSpawn( new Food() )` | **静默落点随机**（不是定点，编译还过） |

> 生成器已把这三句的**正确姿势固化**，并有 `_chk/verify_codegen_compiles.js`
> 拿真 `javac` 编译一遍来兜底（见 6.2 / 6.6）。`Point` 的 import 只在任一层非空时才写；
> `Random` / `Dungeon` / `Heap` 的 import 按实际用到的表达式**自动补**
> （例如 `IronKey( Dungeon.depth )` 会带上 `Dungeon`；`quantity(Random.NormalIntRange(4,5))`
> 会带上 `Random`）。

### 5.3 落地清单（四步，缺一不可）

```java
// 1) 新建文件
//    core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels/rooms/standard/MushroomCaveRoom.java

// 2) 注册进 StandardRoom.rooms 静态列表（当前 34 项）
static {
	...
	rooms.add( MushroomCaveRoom.class );
}

// 3) 为**每一个**深度的 chances[depth] 数组追加一项权重
//    ⚠ chances 是 float[27][]，每行长度**必须**等于 rooms.size()
//    少一个 ⇒ ArrayIndexOutOfBounds；多/少不一致 ⇒ 权重整体错位静默生效
static {
	chances[0]  = new float[]{ ..., 0.5f };   // ← 追加
	chances[1]  = new float[]{ ..., 0.5f };
	... // 共 27 行
}

// 4) 若该房间只能由地形生成、不参与随机房型：
//    类名加进 StandardRoom.TERRAIN_ONLY_ROOMS 白名单
```

> 这三处的**长度一致性**是最容易静默出错的地方：`rooms` 加了一项而某个 `chances[d]`
> 忘加，编译不报错、运行不报错，只是权重整体错位 —— 新房间永远不会出现，或某个老房间
> 概率异常。加完务必逐个数一遍行长度。

### 5.4 存档 / 导出

- **保存 .json** / **打开 .json** —— 编辑器工程文件，含尺寸、网格、**陷阱/植物/道具三层**、
  图集、草叶区、类名。可回读继续改，也可以丢进版本库当房间设计的「源文件」。
  - 当前格式 `version: 3`（v1 = 纯地形，v2 = 加陷阱/植物，v3 = 加道具）。
    **三个版本都能读**：老文件里没有的层按空层处理。
  - 只存**非空格**（`{i: idx, ...}`）⇒ 文件大小与房间内容成正比，不是 `w*h` 个条目。
- **导出 PNG** —— 按当前缩放导出，方便贴文档或讨论。

---

## 6. 正确性是怎么保证的

编辑器里的渲染引擎（`render.js`）和代码生成器（`codegen.js`）都是
**手抄** `java` 侧算法的 JS 重写版。手抄就有抄错的风险，所以配了两套独立核验：

### 6.1 渲染引擎：帧号逐格比对

```bash
python _chk/verify_terrain_editor_frames.py
```

- 纯 Python **独立重实现**同一套 4 层（terrain / features / raised / walls）算法，
  再通过 Node 加载 `render.js` 跑同一批地图，逐 (格 × 层) 比对帧号。
- 当前规模：**5 张测试地图、1710 格、6840 个 (格 × 层) 组合，全部一致**。
- 含 **反例自测**：故意把 `RAISED_HIGH_GRASS` 挪 1 帧，脚本必须报出 22 处不一致
  —— 证明判据不是恒真的空测。

### 6.2 代码生成器：反向解释 + 真 javac

```bash
node _chk/verify_terrain_codegen.js
```

关键在于**不比对字符串**（比对字符串证明不了语义对不对），而是：

1. 造一张有代表性的房间（墙/草/高草/水/深渊/门/隐藏门/上锁门/基座/雕像/陷阱/孤点）；
2. 生成 Java；
3. **反向解释执行**生成出来的每条 `Painter.*` 调用，重算出一张网格；
4. 与原始网格**逐格比对** —— 196 格全等。
5. 反例自测：改掉生成的某一行，必须报不一致。

再用真 JDK 编译一遍（按项目约定，AI 只做源码级核验、不代跑 Gradle）：

```bash
# 生成样例并 javac（classpath / sourcepath 见 skill egopd-source-verify）
javac -proc:none -Xlint:all -encoding UTF-8 \
  -cp "D:/PD/core/build/classes/java/main;D:/PD/SPD-classes/build/classes/java/main;$GDX" \
  -sourcepath "D:/PD/core/src/main/java" -d _chk/_javachk \
  _chk/_javachk/EgoTerrainEditorSampleRoom.java
# → EXIT=0，本文件告警/错误 0 行
```

> **「未使用的 import」javac 不报**：生成器曾多写一行 `import com.watabou.utils.Point;`，
> `-Xlint:all` 也不吭声。只能靠「逐个 import 数在文件里出现几次」查出来（`Point` 只有 1 次＝import 行本身）。
> 改生成器的 import 列表后请顺手复核。

### 6.3 房间检查器：判据单测

```bash
node _chk/verify_terrain_checker.js
```

检查器（第五节）的判据在浏览器里很难用眼睛发现错 —— 假报和漏报都像"正常"。
所以给每条判据配了正例 + **反向构造**（把条件破坏掉，断言必须报）：

```
=== 1. 外圈判定：门在四条边上都不该算「破损」 ===
  ✓ 四向开门（含上下两边的门）不报「外圈破损」
  ✓ 底边门被换成草地 ⇒ 报「外圈破损」（反向构造有效）
=== 3. 连通性与深渊 ===
  ✓ 一堵墙横切整个内部 ⇒ 报「可通行格不连通」
=== 4. 图集覆盖：WATER 必须被跳过 ===
  ✓ WATER 帧为空 ⇒ 不报（水不由 tilemap 画）
  ✓ GRASS 帧为空 ⇒ 报「图集没画草地」（反向构造有效）
=== 5. 正常房间不产生任何提示 ===
  ✓ 纯空房 + 四向开门 ⇒ 检查器零提示
✅ 房间检查器判据全部通过
```

> **本测试也修过大意**：第一版「一堵墙横切」只铺到 `x=6`，而 8×8 内圈的 x 到 8 ⇒ 右侧留了一条缝、
> 两半仍连通，于是"漏报"。**是测试写错，不是检查器漏报** —— 反向构造时要把条件真正做严格
> （这里改用 `x <= Ed.interiorW()` 铺满整个内部宽度）。

### 6.4 陷阱 / 植物表：与 Java 源逐项比对

```bash
python _chk/verify_trap_tables.py
python _chk/selftest_trap_render.py     # 反例自测，4 类破坏都要被抓到
```

`render.js` 里那两张表（33 陷阱 + 13 植物）是**手抄** `levels/traps/*.java` 与
`plants/*.java` 的，抄错一个颜色/形状，画布上的陷阱就变成另一种 —— 而「另一种陷阱」
看起来同样是合法的陷阱贴图，肉眼根本分不出。所以：

1. **解析 Java 源**：连**继承**一起解（最多上溯 5 层）—— `GnollRockfallTrap extends RockfallTrap`
   与 `TenguDartTrap extends PoisonDartTrap` 的颜色/形状是从父类继承的，不解析继承就会误报缺失。
2. 与 JS 表逐项比：类名集合、颜色、形状、植物 `image`、帧号公式。
3. 再拿**真图集**验：每个帧号在 `terrain_features.png` 上非空，且**同一形状的所有颜色帧像素数一致**
   （DOTS=108 / WAVES=136 / GRILL=128 / STARS=66 / DIAMOND=92 / CROSSHAIR=132 / LARGE_DOT=77）。
   这条是强结构判据 —— 如果某个颜色抄错了，它的像素数会跳到别的形状去。

`verify_trap_render.js` 则在假 DOM 里跑真的 `featuresVisual()`，断言：
裸 `Terrain.TRAP` **不画任何东西**、`makeTrap` 的帧号（`FrostTrap`=54、`DisintegrationTrap`=85、`ToxicTrap`=35）、
`visible:false` ⇒ `-1`、`active:false` ⇒ 用 `BLACK` 色、以及**三级优先级**（陷阱 > 植物 > 草）。

`selftest_trap_render.py` 会就地改 `render.js`（改完还原）造 4 种破坏，断言核验脚本
**各自命中预期的那条断言** —— 证明这套核验不是在自我催眠。

### 6.5 覆盖层端到端：纯 Node 桩 + 真浏览器

```bash
node _chk/verify_editor_layers_ui.js      # 80 项：DOM 桩，可断言细节
node _chk/verify_editor_browser.js        # 51 项：系统 Edge 无头，真 DOM / 真事件
```

**为什么要两套**：

| | DOM 桩（`verify_editor_layers_ui.js`） | 真浏览器（`verify_editor_browser.js`） |
|---|---|---|
| 图集解码 | 自己写的 PNG 解码器（RGBA8 + 调色板） | 浏览器原生 |
| 事件 | 自己派发 + `f.call(el)` 保证 `this` 正确 | 真 `MouseEvent`，走 `cellFromEvent` 的坐标换算 |
| 能抓什么 | 图层路由、撤销/重做原子性、JSON 往返（含 v2 老档兼容）、codegen 字符串 | UI 是否真绑上、下拉项数、点击是否真落笔、代码面板是否真更新 |
| 抓不到 | 「桩里对、浏览器里错」 | 细节分支（跑得慢，断言粗） |

实测**两次都是真浏览器这边先抓到问题**：一次是注入过早导致满屏假失败，一次是
`#codeBox` 其实是 `<textarea>`（要读 `.value` 而不是 `.textContent`）。

> **⚠️ 最大的那颗雷：首帧发生在图集加载之前（已修）**
>
> `app.js` 的 `boot()` 是**同步**跑到底的：它调用 `fullRefresh() → draw() → render()`，
> 而楼层图集是 `new Image(); im.onload = ...` **异步**加载的 —— **首帧必定早于 `setSheets()`**。
> 原来 `render()` 没有空值保护，于是 `S.sheet.draw()` 直接抛
> `TypeError: Cannot read properties of null (reading 'draw')`，**整个 `boot()` 中断**：
> 下拉框全空、画布 16×16、代码框空白。
>
> 以前没炸纯属**运气**（图集加载够快）。C.4 把 `items.png`（84KB → `assets.js` 从 216KB 涨到 332KB）
> 加进内联图集后**时序一变就必炸**。
>
> 修法：`render()` 在 `ctx.scale` 之后直接 `if (!S.sheet) return;`（`S.features` 同样加保护），
> 让行为**与加载速度无关**。核验侧相应改成显式 `R.setSheets(fakeSheetImg, fakeSheetImg)`。

> **⚠️ 第二颗雷：IndexedDB 在 `file://` 下会把启动整个卡住（已修）**
>
> C.3 加外部图集时，`boot()` 末尾写成「`restoreExt(把收尾工作放进回调)`」——
> 逻辑上没问题，但在 `file://` 这种 opaque origin 下，**Edge 的 `indexedDB.open()`
> 可能既不触发 `onsuccess` 也不触发 `onerror`**，于是那个回调**永远不执行**：
> 画布 0×0、`Ed.E.w` 停在 14 —— 而且**控制台一声不响**。
>
> 定位靠一个临时探针页（`_diag.html`，用完即删）打印 `cvSize` 与 `boot()` 的返回值。
>
> 修法三件套（缺一不可）：
> ① `openDB` 加 **250 ms 超时 + `onblocked`**；
> ② `restoreExt` 加 **300 ms 强制放行 + `done` 闩**，保证回调**至多执行一次**；
> ③ **`boot()` 的必需初始化改回同步**，`restoreExt` 只做**追加式**补充：
>
> ```js
> rebuildSheetOptions(); applySheet(); fullRefresh();   // 同步，绝不依赖 IndexedDB
> msg('编辑器就绪 —— …', 'var(--ok)');
> restoreExt(function () {                              // 异步，恢复到了才重刷
>     if (!EXT.length) return;                          // 没有外部图集 ⇒ 上面那次已够
>     rebuildSheetOptions(); applySheet(); refreshExtHint();
> });
> ```
>
> 教训：**「能让页面用起来」的那部分初始化，绝不能挂在异步存储的回调里。**
> 这条已同步进 `AGENTS.md` §6。

> **无头浏览器的坑（都踩过，已固化进脚本）**：
> ① **注入时机**：`index.html` 用多个 `<script src>` 按序加载，`--dump-dom` 会在脚本跑完前快照。
>    必须先轮询到 `typeof TE_EDITOR === 'object'` 再注入，否则拿到的全是「UI 没绑上」的假失败。
> ② **跨 realm 注入**：从父页 `d.createElement('script')` 塞进 iframe **有时不执行**；
>    改用 `iframe.contentWindow.eval(src)` 才稳。
> ③ **javac 中文输出乱码**：Windows 上 javac 按 GBK 写诊断，Node 默认按 UTF-8 解会得到
>    `�����ݵ�����`。要么拿 Buffer 手动 GBK 解码，要么别用中文关键词做断言。
> ④ **`--dump-dom` 会忽略定时器与 `load` 事件**：`setTimeout` / `setInterval` / `window.onload`
>    里做的事它**永远不会观察到**，`--virtual-time-budget` 也救不了。
>    唯一可靠的时机是**紧跟 `app.js` 之后同步执行**。所以现在的探针（`_chk/te_probe.js`）
>    是**注入进同一目录的 `index.html` 副本**（`replace('</body>', '<script>…')`），
>    而不是用 iframe + 轮询。
> ⑤ **`file://` 是 opaque origin**：跨源被屏蔽时错误只剩一句 `Script error.`（没有行号）。
>    排障时加 `--allow-file-access-from-files --disable-web-security` 才看得到真实消息。
> ⑥ **`<head>` 带属性**：注入用 `replace('<head>', ...)` 会**静默不生效**
>    （实际是 `<head data-page-node-id="…">`）⇒ 必须用 `re.subn(r'<head[^>]*>', ...)`。
> ⑦ **探针脚本里不能出现 `</` + `script>`**：它是被拼进 `<script>` 标签的，
>    注释里写这个字面量会**提前闭合标签**，脚本整段不执行（title 不变，看起来像没注入）。
> ⑧ **`spawnSync` 必须给 `timeout`**：Edge 若被**上一次的残留实例**顶住，无头进程会
>    **永久挂起**，而 `spawnSync` 默认无限等待 ⇒ 整个套件卡死（实测卡了 4 分钟以上，
>    表现为「脚本没输出也没退出」）。现在固定 `timeout: 120000, killSignal: 'SIGKILL'`，
>    并在 `r.error`/`r.signal` 时打印「先 `taskkill /IM msedge.exe /F` 再重跑」。
>    探针文件放在**独立子目录**（`_probe/`）也能大幅降低撞名残留的概率。
> ⑨ **`--dump-dom` 抓不到 IndexedDB 的异步结果**：把探针放在 `app.js` 之后**同步**跑，
>    此时 `TE_APP.ext()` 必然还是空的 —— 所以浏览器侧的 H 段只断言
>    「入口存在 + 初始态正确 + 浏览器支持 IndexedDB」，**真的导入/持久化逻辑交给
>    `verify_atlas_import.js`**（它能自己造 `File` 与 Blob URL，真浏览器造不出来）。
> ⑩ **反例自测里「删注释」不等于「注入反例」**：C.5 的 D 节断言的是**区域段的行号**，
>    而最初的反例自测只删掉了那行段落注释 —— 断言看的是 `Random.shuffle(` 自己所在的行，
>    注释删不删都不影响它，于是 `--selftest` **照样全绿**（假绿：反例根本没注入成功）。
>    改法：反例必须真的**把区域段搬到填充之前**（现在用 `indexOf` 定位发射点 + 锚点重排）。
>    **每条反例自测都要自问一句「这段破坏真的会被我的断言看见吗」**；更稳的做法是让注入
>    失败时**直接 `exit(2)` 报错**，而不是静默跳过 —— 否则反例自测本身就是个假保险。
> ⑪ **测试里别硬写画布宽度**：`regionIndices` 用的是 `Ed.E.w`，而前面几节会改房间尺寸
>    （`setRoomSize`），所以断言必须现取 `const W = Ed.E.w`。硬写 `1*10+1` 会在房间不是
>    10 宽时**假红**，看起来像生产代码错了，其实是测试的期望过期了。
> ⑫ **测试数据要绕开「刻意的合并语义」**：区域标记会把重叠区域**迭代合并**成一片，
>    所以想造「1 格区域 + 一片大区域」两个样本时，两片**必须互不相邻** ——
>    早先写成 `(5,5,5,5)` + `(2,2,5,5)` 会在 (5,5) 相撞被并成 1 片，
>    导致「1 格区域」那条检查器提示永远测不到（假红）。

### 6.5.1 外部图集（C.3）：纯 Node 桩里做完整导入/持久化

```bash
node _chk/verify_atlas_import.js              # 59 项
node _chk/verify_atlas_import.js --selftest   # 61 项（+2 条反例）
```

**浏览器里根本没法自动化「从文件选择器挑一个 png」**，所以导入链路必须用桩来验。
这个桩做得比较重（都写在脚本注释里）：

| 桩件 | 为什么需要 |
|---|---|
| **真 PNG 编/解码**（`crc32` / `chunk` / `encodePng` / `decodePng`） | 要能断言「渲染真的换成了新图的像素」，不能只断言 URL 变了 |
| **真 DOM 桩** | `app.js` 会 `getElementById`，缺一个就抛 |
| **IndexedDB 桩**（底层就是一张 `Map`，实现 put/getAll/delete/clear） | 验「刷新页面能恢复」这条唯一路径 |
| **`Blob` 桩 + `URL.createObjectURL` 桩 + 同步解码的 `Image` 桩** | `app.js` 把 blob URL 交给 `Image`，桩必须同步 `onload`，否则测试要到处 `await` |

覆盖面：**A** 接线 → **B** 导入一张楼层图集 → **C** 像素级确认真的换了图 →
**D** 非 16 倍数被拒 → **E** 同类替换（只留一张）→ **F** 草叶图/道具图覆盖内置 →
**G** IndexedDB 真的落了 Blob → **H** 生成代码里是**人类可读名**（不含 `ext:sheet:`）→
**I** 清空后全部回退 → **J** 模拟刷新后恢复 → **K** 反例自测（把 `% 16` 校验去掉，必须被抓到）。

> 桩自己也踩了两个坑，都写进脚本注释了：
> ① **`createObjectURL` 的去重键不能用「长度 + 前 32 字节」** —— 两张同尺寸图集会撞车，
>    表现为「§E 拿到的是第一张图」。要用**完整 md5**。
> ② **`revokeObjectURL` 之后必须让去重缓存失效** —— 否则重新导入同一张图会拿到
>    **已吊销的死 URL**，`Image.onerror` ⇒ 静默导入失败（§J 就这么红过）。

### 6.5.2 区域随机（C.5）：三重语义 + 一条顺序不变量

```bash
node _chk/verify_random_regions.js              # 108 项
node _chk/verify_random_regions.js --selftest   # 按设计失败 2 项（顺序断言）
```

这套的核心不是「有没有生成 shuffle」，而是三个**肉眼看不出来**的性质：

| 性质 | 为什么必须验 | 怎么验 |
|---|---|---|
| **多重集不变** | 逐格重掷会毁掉作者定的配比，但代码「看起来也在打乱」 | 从生成的 `int[] r0 = new int[]{...}` 里解析字面量，排序后与原区域对比 |
| **顺序正确** | 排在填充之前会被**整片覆盖**且**完全不报错**，字符串层面毫无异常 | 按**行号**断言区域段在每一条 `Painter.fill/set` 之后、门循环之前 |
| **覆盖层联动** | 只打乱地形会让陷阱/道具留在原地（错位），且编译能过 | 按开关分别验两条生成路径，并断言置换表初始化为恒等、`r0Old = r0.clone()` |

`--selftest` 的注入方式值得单独记一笔：**必须真的把区域段搬到填充之前**，
只删注释是无效的（见 §6.5 陷阱 ⑩）。

> **另有一层兜底**：`verify_codegen_compiles.js` 的 A3/C 节会把**两种 `withLayers` 变体
> 一起交给真 `javac`**，并反汇编断言 `Random.shuffle:([I)V` —— 因为 `Random` 有
> `int[]` / `T[]` / `(U[],V[])` **三个重载**，用错重载编译能过但语义全错。
> 字符串断言抓不到这个层次，只有字节码能。

### 6.6 生成的 Java：真 `javac` 编译

```bash
node _chk/verify_codegen_compiles.js              # 正例：必须零错误
node _chk/verify_codegen_compiles.js --selftest   # 反例：把 Seed 去掉，必须编译失败
```

字符串断言只能证明生成物「长得像」Java。**最容易出的事故恰恰是签名/包名**：

- 植物写成 `new Sungrass()` 而非 `new Sungrass.Seed()` ⇒
  `不兼容的类型: Sungrass无法转换为Seed`
- 忘了 `import com.watabou.utils.Point;` ⇒ 找不到符号

所以这一步真的调 `javac`：

```bash
javac -encoding UTF-8 \
  -cp "core/build/classes/java/main;SPD-classes/build/classes/java/main" \
  -d _chk/probe/out _chk/probe/ProbeLayerRoom.java
```

生成物必须写成 `levels.rooms.standard` 包（与 `StandardRoom` 同包，否则 `extends StandardRoom` 解析不到）。
编译过后再用 `javap -c` 反汇编，断言字节码里真的出现：

```
invokevirtual  Level.setTrap:(...levels/traps/Trap;I)...Trap;
invokevirtual  Level.plant:(...plants/Plant$Seed;I)...Plant;
new            ...plants/Firebloom$Seed        ← 内部类，不是 Firebloom
invokevirtual  Level.pointToCell:(...watabou/utils/Point;)I
invokevirtual  Level.drop:(...items/Item;I)...items/Heap;    ← 道具走这条，不是 addItemToSpawn
putfield       ...items/Heap.type:...items/Heap$Type;
getstatic      ...items/Heap$Type.CHEST
invokevirtual  ...items/DarkGold.quantity:(I)...items/DarkGold;
invokestatic   ...utils/Random.NormalIntRange:(II)I
getstatic      ...Dungeon.depth:I              ← IronKey( Dungeon.depth )
```

> **为什么必须反汇编**：`Level.drop` 的**返回值**才是关键 —— 堆类型靠
> `level.drop(...).type = Heap.Type.CHEST` 链式设置，而字符串层面
> `addItemToSpawn` 与 `drop` 都只是「一个方法调用」。字节码里能同时确认
> **签名是 `(Item,int)`、返回值是 `Heap`、随后真的 `putfield Heap.type`**。
> 字符断言的**反例自测**也顺带覆盖：把 `new X.Seed()` 破坏成 `new X()` ⇒ 编译失败且
> 报 `不兼容的类型: Sungrass无法转换为Seed`。

### 6.7 道具表 vs Java 源：71/71 存在性 + 占位图三层证明

```bash
python _chk/verify_item_tables.py        # 8 项 + 反例自测
python _chk/verify_item_placeholder.py   # 16 项（三层独立证明）
```

`render.js` 的 `ITEMS` 表有 **71 个类名 + 分组 + 中文名**，是手抄 `items/` 全包的。
抄错一个类名，画布上照样画出「一个口粮」，但**生成的代码编译不过**（类不存在）——
所以必须逐个对文件核验：

1. `items/<pkg>/<cls>.java` **真实存在**；
2. 是 `public class` 且**不是 abstract**（abstract 的 `Item` 造不出来）；
3. 无重复项；
4. 每一项都能产出一个 `ctor`（生成器必须写出 `new <cls>()`）。
5. **反例自测**（`--selftest`）：把一个假类名 `TotallyFakeItem` 注入表里，
   断言脚本必须报错（实测抓到 2 处）。

`verify_item_placeholder.py` 则是**口粮帧号 437** 的三层证明，见 3.2 ——
这是「占位图」这一层唯一的正确性依据，错了会**静默画成另一件物品**（同样是合法贴图，肉眼看不出）。

> 前置：需要 `core` / `SPD-classes` 已编译过（用户侧 Gradle 跑过即可）。
> 目录不存在时脚本打印 `SKIP` 并以 0 退出 —— **不能因为环境缺失就误报失败**。

### 6.8 改了编辑器之后要重跑

改了 `render.js` / `editor.js` / `codegen.js` 任何一个，**下面这套全都要重跑**：

```bash
node   _chk/verify_terrain_checker.js       # 检查器判据
node   _chk/verify_terrain_codegen.js       # codegen 反向解释
python _chk/verify_terrain_editor_frames.py # 渲染帧号（含反例自测）
python _chk/verify_trap_tables.py           # 陷阱/植物表 vs Java 源
node   _chk/verify_trap_render.js           # 陷阱层渲染
python _chk/selftest_trap_render.py         # 陷阱层反例自测
node   _chk/verify_water_layer.js           # 水体层
node   _chk/verify_editor_layers_ui.js      # 覆盖层端到端（DOM 桩）
node   _chk/verify_editor_browser.js        # 覆盖层端到端（真浏览器）
node   _chk/verify_atlas_import.js          # 外部图集导入 / 持久化（纯 Node 桩）
node   _chk/verify_random_regions.js        # 区域随机：拓扑 / 多重集 / 顺序 / 联动（含反例自测）
node   _chk/verify_size_fuzz.js             # 尺寸模糊化：安全区 / 夹取 / 类别选择 / 成对性（含反例自测）
node   _chk/verify_size_fuzz_ui.js          # 尺寸模糊化端到端（DOM 桩，含反例自测）
node   _chk/verify_codegen_compiles.js      # 生成的 Java 真编译 + 反汇编（含模糊化变体）
python _chk/verify_item_tables.py           # 道具表 vs Java 源（71 项）
python _chk/verify_item_placeholder.py      # 口粮占位图帧号三层证明
python _chk/verify_terrain_browser.py       # 真浏览器端到端（无外部依赖版）
python _chk/check_utf8_all.py               # 全仓编码自检
```

**当前全绿状态**（18 套）：

| 脚本 | 结果 |
|---|---|
| `verify_terrain_editor_frames.py` | 6840/6840 + 反例 22 处 |
| `verify_terrain_codegen.js` | 196 格全等 |
| `verify_terrain_checker.js` | 14/14 |
| `verify_trap_tables.py` | 33/33 + 13/13 |
| `verify_trap_render.js` | 17/17 |
| `selftest_trap_render.py` | 4/4 |
| `verify_water_layer.js` | 13/13 |
| `verify_editor_layers_ui.js` | **80/80** |
| `verify_editor_browser.js` | **51/51** |
| `verify_atlas_import.js` | **59/59**（`--selftest` 61/61） |
| `verify_random_regions.js` | **108/108**（`--selftest` 按设计失败 2 项） |
| `verify_size_fuzz.js` | **全部通过**（含 16 项反例自测） |
| `verify_size_fuzz_ui.js` | **63/63**（`--selftest` 按设计失败 6 项） |
| `verify_codegen_compiles.js` | **49/49**（真 javac，含区域段两份 + 模糊化变体） |
| `verify_item_tables.py` | **8/8** + 反例 |
| `verify_item_placeholder.py` | **16/16** |
| `verify_terrain_browser.py` | **20/20** |
| `check_utf8_all.py` | 全通过 |

> `verify_terrain_browser.py` 与 `verify_editor_browser.js` 是**两条互补的真浏览器链路**：
> 前者不依赖任何外部图集文件之外的输入、断言面更宽（全局对象 / 6 张图集 / 下拉项数 /
> 画布尺寸 / 代码非空），后者带 `_chk/te_probe.js` 做**逐项交互**（点击落笔、撤销、
> 检查器、道具不改地形、去重跳过）。两个都是「纯 Node 逻辑核验抓不到的 UI 层」的兜底。

另外 `assets.js` 是从真实图集重新生成的：

```bash
python _chk/terrain_editor_assets.py     # 重新内联 6 张图集 + terrain_features.png + 5 帧水
```

（换了 `tiles_*.png` / `terrain_features.png` / `water*.png` 之后必须重跑，
否则画布预览与游戏不一致。生成后可以顺手核对内联数据与仓库文件的 md5。）

> **「草地看起来像电路板」不是 bug**（2026-09-19 实际困惑过一次）：
> `GRASS` 在 `terrain_features.png` 层是**一撮稀疏的小草叶**（stage 4 那帧只有 38 个实心像素，
> 呈散点状）。连成一大片时，这些散点会规律重复，看起来像横平竖直的红色花纹。
> 而 `HIGH_GRASS` 那一帧（115 像素）是**一整丛密草**，成片才好看。
> 想看「密集草丛」就画 `HIGH_GRASS`；`GRASS` 本来就是点缀式的浅草。
> —— 编辑器的起步示范因此改成「各放一格 + 一排」，不再用一大片 `GRASS`。

---

## 7. 已知未做（下一步）

| 项 | 状态 |
|---|---|
| 用户自行导入图集（文件选择器 + 登记进 `SHEETS`，标 `builtin:false`） | ✅ **已完成**（C.3：楼层图集 / 草叶图 / 道具图，IndexedDB 持久化，见 2.6） |
| 道具生成 + 生成位置（统一用**口粮**作占位图） | ✅ **已完成**（71 种物品 × 7 种堆类型，`level.drop` 定点投放） |
| 区域随机排列（选中一块区域，生成时地形随机打乱） | ✅ **已完成**（C.5：多重集打乱 + 可选覆盖层联动，见 2.7） |
| 模糊化（房间尺寸在一定范围内变化） | ✅ **已完成**（C.6：`sizeCatProbs()` + `setSize()` 成对生成、安全区参考线 + 越界自动夹取，见 2.8） |
| 独立于 EGOPD（去掉 EGOPD 专属包名默认值、`tiles_lob`/27 层引用） | ✅ **已完成**（C.7：全面改名为中性「SPD 地形编辑器」；`tiles_lob` 移出内置清单、项目特有图集走外部导入；包名可配置 + 空值回退通用 SPD 路径；注册清单改说**判据**而非写死数字。**旧的 `egopd-terrain-editor` 存档仍能读入**，见 7.1） |
| **整层绘制模式**（特殊层地形生成，**不设尺寸上限**） | 未做 —— 需要模式开关、无上限画布、生成 `Level` 子类级代码而非 `StandardRoom.paint()`、检查器分模式、隐藏房间专属 UI |
| **撤销/重做栈深度可配置**（当前固定上限，大画布上偏低） | 未做 —— 加一个「最多保留 N 步」输入框；注意每步快照含 `randoms` 与三个覆盖层数组，**内存占用随画布面积线性上升**，所以既要能调大、也要在下调时**立即截断已有栈**（否则用户改小了设置却不释放内存） |
| 道具堆形的**宝箱/墓碑/尸骨等贴图**在编辑器里显示，且**不被口粮占位图替换**（口粮只替换道具内容本身） | 未做 —— 现在 `Heap.Type != HEAP` 的格子仍画口粮；需按 `selHeap` 的堆型查 `items.png` 的堆型帧（`HEAP`/`CHEST`/`LOCKED_CHEST`/`CRYSTAL_CHEST`/`TOMB`/`SKELETON`/`REMAINS`），只对 `HEAP` 用口粮 |

### 7.1 C.7 解耦（独立于 EGOPD）落地要点

> **范围（用户确认）**：保留全部 EGOPD 相关功能，**只改名**；项目特有图集移出内置、走外部导入；
> 包名做成可配置 + 注册清单通用化。**不是**把功能删掉。

| 项 | 改前 | 改后 |
|---|---|---|
| 标题 / 页签 | `EGOPD 地形编辑器` | `SPD 地形编辑器` |
| 三个内核文件头注释 | `EGOPD 地形编辑器 —— …` | `SPD 地形编辑器 —— …` |
| 生成代码 banner | `由 EGOPD 地形编辑器生成。` | `由 SPD 地形编辑器生成。` |
| 存档 `format` 键 | `'egopd-terrain-editor'` | `'spd-terrain-editor'`（**读侧白名单兼容旧名**） |
| IndexedDB 库名 | `egopd-terrain-editor` | `spd-terrain-editor` |
| 内置图集 | 6 张（含项目特有的 `tiles_lob`） | **5 张**（SPD 原版 5 区域）；`tiles_lob` 走外部导入 |
| `package` 默认值 | 硬编码 EGOPD 包路径 | `DEFAULT_PKG` 常量 = 通用 SPD 路径；**面板空/纯空格时回退**到它，非空则原样采纳 |
| 注册清单注释 | 写死 `35` 项、`float[27]` | 用 `<深度数>` 占位；改说**判据**「`chances` 每行长度必须等于 `rooms.size()`」 |

**两个静默陷阱（本次踩到 / 需长期留意）：**

1. **改名存档键 ⇒ 必须同时改读侧为白名单**。`fromJSON` 原来是 `o.format !== 'egopd-terrain-editor'` 就抛错；
   若只改写侧，用户硬盘上存量 `.json` **全部打不开**（报「不是地形编辑器文件」）。现改为
   `isSaveFormat(f)` 白名单收两个名字。反例断言在 `verify_size_fuzz.js` F 节：老名能读入、**无关名必须被拒**
   （否则白名单写成「一律放行」也算通过）。
   - 现存三个套件的 fixture 刻意保留旧名（`verify_random_regions.js` v3/v4 档、`verify_editor_layers_ui.js` v2 档）
     —— 它们同时充当**向后兼容的活体回归**，别去「顺手统一」成新名。

2. **内置图集数量变了 ⇒ 一串断言会静默过时**。删 `lob` 后 `TE_SHEETS` 从 6 变 5，
   **4 处**写死 6 的断言全红：`verify_atlas_import.js`（6 处）、`verify_editor_browser.js`（内置数 + 下拉项数 2 处）。
   这类「数量耦合」断言在加减内置资源时必然过期，属**合法过时**而非产品 bug —— 但必须逐条核对是不是真的只有数量变了。
   - 顺带确认判据有效：把 `lob` 临时塞回 `TE_SHEETS` ⇒ `verify_terrain_codegen.js` **恰好 2 条**失败
     （「不含 `tiles_lob`」+「恰好 5 张」），恢复后全绿。

**浏览器实证**（无头 Edge 注入探针，抓 `#codeBox` 的 `package` 行）：

```
PKG_INITIAL = com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard
PKG_EMPTY   = com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard   ← 空 ⇒ 回退
PKG_SPACE   = com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard   ← 纯空格 ⇒ trim 后回退
PKG_CUSTOM  = com.example.mymod.rooms                                          ← 自定义 ⇒ 原样采纳
```

> ⚠ 探针坑：代码面板是 `<textarea id="codeBox">`，取内容要用 **`.value`** 而不是 `.textContent`
> （`document.querySelector('pre')` 会取到别的元素、恒得「未找到」）。

> **「模糊化」的设计约束（已查清源码）**：`Room.setSize(minW,maxW,minH,maxH)` 内部是> `resize( Random.NormalIntRange(minW,maxW)-1, Random.NormalIntRange(minH,maxH)-1 )` ——
> **宽高各自独立**随机、**正态分布**（偏中间值）；`-1` 是因为 `right/bottom` 闭区间。
> 取值范围来自 `SizeCategory`：`NORMAL(4,10,1)` / `LARGE(10,14,2)` / `GIANT(14,18,3)`，
> 且 `StandardRoom.sizeCatProbs()` 默认 `{1,0,0}`（永远是 NORMAL），要更大的房间得覆写。
> ⇒ **在 `paint()` 里写死尺寸会在尺寸变化的房间上错位**；复杂图案要锚定到
> min/max 边界或改用中心相对坐标。
> **落地形态（C.6 实际做法）**：编辑器生成
> `@Override public float[] sizeCatProbs(){ return new float[]{1,0,0}; }` **与**
> `paint()` 开头的 `setSize(minW,maxW,minH,maxH)` —— **两者必须成对**（只写 `setSize`
> 会被 `minW < minWidth()` 静默拒绝）。所有地形/覆盖层坐标在生成时**夹进安全区**
> （`min(画布, min) - 1 - guarded`），并逐行加 `// ⚠ 模糊化：已夹进安全区（原 …）` 注释。

> **「撤销栈深度」的设计约束（落地前先读）**：`Ed.pushUndo()` 的快照 = `map` + `traps` +
> `plants` + `items` + `randoms` 的**全量深拷贝**（不是差分），步数 × 画布格数 × 数组条数
> 决定内存。整层模式（C.8）下画布会显著大于房间，所以这一项**应与 C.8 一起做**：
> 提供上限输入框（默认沿用现值），并在**下调时立刻 `stack.length = N` 截断**。
> 另一个方向（改成命令式/差分撤销）收益更大但改动面太宽，暂不做。

### 7.1 整层绘制模式的设计要点（已调研，未实现）

用户明确要求：「支持除房间绘制之外的整层绘制，用于特殊层的地形生成，这种绘制**不要设置大小上限**」。
与现有房间模式的关键差异（落地时按这份清单核对）：

| 维度 | 房间模式（现状） | 整层模式（待做） |
|---|---|---|
| 尺寸 | 「房间尺寸」面板限制（`SizeCategory` 量级） | **不设上限** —— 画布需虚拟滚动/分块渲染 |
| 坐标基准 | `left + x` / `top + y`（相对分配矩形） | **绝对格号**（整层没有「分配矩形」） |
| 生成产物 | `StandardRoom` 子类的 `paint(Level)` | `Level` 子类的 `build()` / `paint()`（自行 `setSize` + 铺图） |
| 骨架 | 固定 `fill(this, WALL)` + `fill(this, 1, EMPTY)` | 需自行决定边界与不可通行区（如 `LobTestLevel` 那种 `GEN_DEPTH` 驱动） |
| 门 | `placeDoors` 在外圈找门位 | 层与层的连接由 `Level.create()` / `RegularLevel` 负责，不在本次绘制范围 |
| 检查器 | 外圈破损 / 门位置 / 孤岛 / 全深渊 | 应换成整层判据（可通行区是否连通、出入口是否可达、边界是否封闭） |

> 已有先例可抄：`levels/LobTestLevel.java`（自定义测试层，`GEN_DEPTH` + `TerrainFeaturesTilemap.FeaturesTexProvider`）
> 就是「整层地形生成」的样板之一；`docs/terrain-creation-guide.md` 记录了新增地形贴图的完整接线。

---

## 8. 快捷操作总表

| 操作 | 快捷方式 |
|---|---|
| 撤销 / 重做 | `Ctrl+Z` / `Ctrl+Y`（或 `Ctrl+Shift+Z`） |
| 切工具 | `B` `L` `R` `E` `F` `I` `X` `空格` |
| 吸管 | 右键（任何工具下） |
| 平移 | 中键拖动 / 选「✥ 拖动」 |
| 缩放 | `Ctrl` + 滚轮 |

---

## 9. 相关文件

| 文件 | 作用 |
|---|---|
| `tools/terrain-editor/index.html` | 入口，双击即用 |
| `tools/terrain-editor/app.js` | UI 装配（画布交互、工具栏、覆盖层面板、**外部图集导入/IndexedDB 持久化**、**区域随机面板**、检查器、导出） |
| `tools/terrain-editor/editor.js` | 编辑内核（网格、**陷阱/植物/道具三层**、**区域随机标记 + 拓扑合并**、撤销、工具算法、宏、不变量检查） |
| `tools/terrain-editor/render.js` | 渲染引擎（水层 + 4 层地形 + 草叶/陷阱/植物 + 道具占位图 + **区域标记叠加层**，帧号与 Java 对齐） |
| `tools/terrain-editor/codegen.js` | Java 代码生成器（矩形合并 + **setTrap/plant/drop 三层覆盖层** + **区域随机多重集打乱** + 落地清单） |
| `tools/terrain-editor/assets.js` | **生成物**，勿手改；base64 内联的图集、水帧与 `items.png` |
| `_chk/terrain_editor_assets.py` | 生成 `assets.js` |
| `_chk/verify_terrain_editor_frames.py` | 渲染引擎帧号核验（含反例自测） |
| `_chk/verify_terrain_codegen.js` | 代码生成器反向解释核验（含反例自测） |
| `_chk/verify_terrain_checker.js` | 房间检查器判据单测（每条含反向构造） |
| `_chk/verify_trap_tables.py` | 陷阱/植物表 vs Java 源（含继承解析）+ 真图集像素核验 |
| `_chk/verify_trap_render.js` | 陷阱/植物层渲染核验（假 DOM，17 项） |
| `_chk/selftest_trap_render.py` | 上述核验的反例自测（4 类破坏） |
| `_chk/verify_water_layer.js` | 水体层独立成层核验（自带 PNG 解码器，13 项） |
| `_chk/verify_editor_layers_ui.js` | 覆盖层端到端（DOM 桩，80 项，含 v2 老档兼容与三层 JSON 往返） |
| `_chk/verify_editor_browser.js` | 覆盖层端到端（系统 Edge 无头，51 项） |
| `_chk/verify_atlas_import.js` | 外部图集导入 / 替换 / 持久化（纯 Node 桩，59 项，`--selftest` 61 项） |
| `_chk/verify_random_regions.js` | 区域随机：拓扑 / 多重集不变 / **顺序不变量** / 覆盖层联动 / JSON v4 兼容（108 项，含反例自测） |
| `_chk/verify_terrain_browser.py` | 真浏览器冒烟（独立一条链路，20 项） |
| `_chk/verify_codegen_compiles.js` | 生成的 Java 真 `javac` 编译 + 反汇编核验（42 项，含区域段两种变体与**重载验证**，含反例自测） |
| `_chk/verify_item_tables.py` | 道具表 vs Java 源（71 项存在性/可见性/可构造，含反例自测） |
| `_chk/verify_item_placeholder.py` | 口粮占位图帧号 437 的三层独立证明（16 项） |
| `_chk/te_probe.js` | 浏览器探针脚本（被 `verify_editor_browser.js` 注入；**不得含 `</`+`script>`**） |
| `_chk/gen_probe_room.js` | 生成 `_chk/probe/ProbeLayerRoom.java` 供人工查看 |
| `_chk/terrain_editor_screenshot.py` | 无头 Edge 截图助手（视觉取证） |

**相关文档**：`docs/terrain-creation-guide.md`（地形贴图创作）、
`AGENTS.md` §3 / §6（分层 tilemap 机制与陷阱表）。

