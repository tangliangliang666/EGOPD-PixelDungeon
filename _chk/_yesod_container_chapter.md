
---

# 2026-09-26（续六）考验 YESOD：容器贴图也变问号 ＋ 战利品指示器跟随 ＋ 出 v0.3.6

> 需求原文：「现在请你再进行一处改动：各类宝箱和骷髅堆的贴图也会被替换为问号（不用改动描述，
> 我已经手动改过），随后更新版本并打包APK」。
>
> 经 `AskUserQuestion` 追加确认三条：① 未知贴图覆盖 **`Heap.Type` 全部 6 类容器**；
> ② 「不用改动描述」指的是**考验描述**（`trials.yesod_desc`，由用户自行维护），**检视描述**仍走
> YESOD 惯例的「你不知道这里是什么。」；③ 屏幕左下角的**战利品指示器**也一起变问号。

## 21.1 判据本体：从「枚举两种类型」改成「非空堆一律」

上一版（§17.4）的 `hidesGroundHeap` 显式排除了容器：

```java
return heap.type == Heap.Type.HEAP || heap.type == Heap.Type.FOR_SALE;   // 旧
```

现在反过来 —— 判据里**不再出现任何 `Heap.Type`**：

```java
// Trials.java
public static boolean hidesGroundHeap( Heap heap ){
    if (heap == null || heap.isEmpty()) return false;
    return Dungeon.isTrialled( YESOD );
}
```

为什么不写成「枚举 6 类容器」：那样**将来每漏一种就静默漏一种**（`Heap.Type` 加新成员时不会报错，
只是那种容器照旧显形、把答案写在脸上）。写成「非空堆一律」把口径收成**一条不变量**。
安全性来自 `Heap.open()` —— 它在打开时把 `type` 改回 `Type.HEAP`，所以「已开箱的堆」本来就是普通掉落，
不需要在判据里单独照顾。

## 21.2 三个老出口 ＋ 一个新出口

| 出口 | 落点 | 本轮改了什么 |
|---|---|---|
| 地图贴图 | `sprites/ItemSprite.view(Heap)` | 闸门**提到 `switch` 之前**并罩住整个 `switch` —— 六类容器的贴图只在 `switch` 里被设置，只有这样才真正覆盖它们 |
| 名字 / 标题 | `items/Heap.title()` | `type == Type.HEAP` → `type != Type.FOR_SALE`（容器也隐名；货架仍提前放行价签） |
| 检视描述 | `windows/WndInfoItem(Heap)` | **无需改动** —— 它本来就读同一个判据（只是现在容器也落进这个分支了） |
| **战利品指示器** | `ui/LootIndicator.update()` | 原本是 `heap.type == CHEST ? ItemSlot.CHEST : …` 一串三元，**等于把刚藏起来的答案重新写在 HUD 上**；现在这个判断排在最前，命中时改取新增的 `ItemSlot.UNKNOWN` |

`ItemSlot.UNKNOWN` 与既有的 `CHEST` / `LOCKED_CHEST` / `SKELETON` 等「虚拟物品」同一套写法：

```java
public static final Item UNKNOWN = new Item() {
    public int image() { return ItemSpriteSheet.UNKNOWN_ITEM; }
    public String name() { return Trials.unknownGroundText(); }
};
```

⇒ 图标与文案都与地图**同源**（同一个 `UNKNOWN_ITEM` 格位、同一条 `trials.yesod_unknown`），
不会出现「地图上是问号、HUD 上是另一个问号」这种两份实现各自漂移的隐患。

## 21.3 为什么四个出口必须同源

`hidesGroundHeap` 是**一个判据的四个出口**。漏任何一个都会得到「半吊子」表现：

- 只改贴图 ⇒ 检视窗里还写着「宝箱」；
- 只改贴图与描述 ⇒ 左下角 HUD 仍在点名「脚下是个宝箱」。

所以 `_chk/verify_yesod.py` 里的 `callers_of('Trials.hidesGroundHeap')` 是**硬判据**：
引用清单必须**恰好**等于「贴图 + 名字 + 描述 + 战利品指示器」四个文件，**多一处少一处都是回归**。

## 21.4 核验

| 层 | 钉住了什么 |
|---|---|
| ① 源码结构 | `hidesGroundHeap` 里**不出现任何 `Heap.Type` / `type ==` / `switch`**；`view(Heap)` 的六个容器 `case` 全排在闸门之后；`LootIndicator` 的问号分支排在三元链最前；`ItemSlot.UNKNOWN` 的两个取值同源 |
| ② `javap -p` | `ItemSlot.UNKNOWN` 字段类型为 `items.Item` |
| ③ `javap -c` | `hidesGroundHeap` 字节码里**没有 `Heap$Type`**；`Heap.title()` 只比对 `Heap$Type.FOR_SALE`（`HEAP` 已退场）；六个容器 `getstatic` 全在闸门之后；`LootIndicator.update` 的 6 个类型图标也全在闸门之后；`ItemSlot$7` 的 `image()` / `name()` 分别取 `UNKNOWN_ITEM` 与 `unknownGroundText()` |
| ④ 文本 | `trials.yesod_unknown` 逐字；**zh/en 对「容器」的口径一致**（见 §21.5） |
| ⑤ PNG | `items.png` 上 `xy(16,2)` 的实测包围盒 = 10×15 @ (0,0)（与抽样矩形逐像素相等） |
| ⑥ 探针 | `YesodProbe` ③ 扩成「`Heap.Type` **全部 8 种取值都中**」＋断言枚举了一个不落；④ 断容器的名字也变未知 |

新写的助手 `after_offset(ins, gate, token)`：问「token 的**首个**引用是否落在闸门之后」，
**缺项返回 False**（「缺项也算通过」是这类顺序判据最常见的假绿来源）。layer 与 selftest 共用同一份实现。

- `_chk/verify_yesod.py`：**158 条 OK / 0 FAIL / 0 SKIP**；`--selftest` **27 条**全过。
- `_chk/YesodProbe.java`：**39 / 0**。
- `javac -Xlint:all`：13 文件（`_build_yesod.sh`，新增 `ui/ItemSlot` / `ui/LootIndicator`）与
  5 文件（`_build_yesod_container.sh`）**EXIT=0**。
- `_chk/check_utf8_all.py` 1474 文件 OK；`_chk/check_unused_imports.py` ALL PASS。
- 全量回归 14 个核验脚本全绿。

## 21.5 顺手抓到一处「只改一半」：英文考验描述

`trials.yesod_desc` 的**中文**版由用户手工重写过（删掉了「_宝箱、骷髅堆等容器_与_背包内_
的显示不受影响。」这半句），但**英文**版当时没跟着删 —— 英文环境下面板会显示一句
与实际行为**相反**的说明：「_Containers (chests, skull piles and the like)_ and the
_inventory_ are unaffected.」。

按用户确认，把这半句也改成 `_Items in the inventory_ are unaffected.`
（`_chk/patch_yesod_desc_en.py`：字节级、幂等、CRLF 计数不变、备份留在 `_chk/_bak_yesoddescen/`）。

⇒ 由此在核验里加了一条**跨语言口径断言**：`zh 提到「容器」` 与 `en 提到 Container/chest`
必须**同真同假**。它专抓「只改了半边」的文案漂移 —— 而不是逐字比对（描述归用户维护，
逐字比对会在用户每次润色时假红）。

## 21.6 版本与打包（v0.3.6）

- 根 `build.gradle`：`appVersionCode 935 → 936`、`appVersionName '0.3.5' → '0.3.6'`。
- `ui/changelist/EGOPD_Changes.java`：新增 **v0.3.6 段（10 条，每条一句话）**，
  覆盖 0.3.5 打包之后的全部内容 —— 三条新考验（NETZACH / HOD / YESOD）、容器问号、
  NETZACH 浮层淡出、趣味挑战、GEBURA 额外掉落修复、标记击杀判据、矿洞虚空地形、四项二层天赋改写。
- `AGENTS.md` §1 版本现状一句同步为 936 / `0.3.6`。
- 打包 `:android:assembleDebug`（38s，`:core:compileJava` 与 `:android:packageDebug` 均 executed）
  → 归档 `EGOPD_0.3.6.APK`。

### 四道核验

| # | 项 | 结果 |
|---|---|---|
| ① | `aapt dump badging` | `versionCode='936'` / `versionName='0.3.6-INDEV'` / `application-label:'EGOPD'` / 图标指向 `mipmap-anydpi-v26/ic_launcher.xml` |
| ② | dex 内新内容 | `EGOPD_Changes` ×3、`容器也变问号` ×1、`hidesGroundHeap` ×1 |
| ③ | 资产全量比对 | APK 内 **542** 个 assets 与工作区 **逐个 md5 全等**（仅 APK 0 / 仅工作区 0 / 不一致 0）；13 条关键文本抽验全中 |
| ④ | 体积变化 | 49,528,642 B（+69,606）；增量可解释：`classes2.dex` +13,200、`terrain_features.png` +11,248、`tree.png` +4,193、`items_zh.properties` +2,074、`items.png` +1,660、新增 `tiles_lob_3.png` 一条；**新包与旧包空洞均为 0** |

## 21.7 请实机验证

1. 开 YESOD：三类宝箱、骷髅堆、坟墓、英雄遗骸**全变问号**，检视显示「？」＋ 未知文本。
2. 站在任意一堆（含金币、散落物）上：左下角战利品指示器也是问号。
3. 商店货架：贴图问号、**价格照常显示**（价签那条 `items.heap.for_sale` 提前放行）。
4. 打开一个宝箱后再看：箱内物品仍是问号（`open()` 把 `type` 改回 `HEAP`），拾取后恢复原贴图。
5. **不开** YESOD：一切与改动前完全一致。
6. 英文环境：考验面板 YESOD 的最后一句应为「Items in the inventory are unaffected.」

## 21.8 已知边界（本轮没碰，供后续决定）

- **名字与描述仍是原生文本**：容器的 `items.heap.chest` / `skeleton` 等**一字未动**（用户明示
  「只换贴图」）。因为 `WndInfoItem` 走的是未知分支、`Heap.title()` 也返回未知文本，这些原生文本
  在 YESOD 下**根本不会被显示**；它们只在不开考验时出现。
- **`LootIndicator` 只覆盖英雄脚下那一格**：这是它原本的职责范围（相邻格不显示战利品提示），
  不是本轮引入的缺口。
- **问号贴图 10×15 比容器原贴图 16×16 小**：`CellSelector.overlapsPoint` 用精灵尺寸做命中判定，
  理论上点选面积会小一点；实际点击走的是「格子 → `Dungeon.level.heaps.get(cell)`」那条路，
  所以不影响拾取/开箱操作。
