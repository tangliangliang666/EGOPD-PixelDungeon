
---

# 2026-09-26（续二）考验 YESOD（根基）：禁自然鉴定 ＋ 商店补一张鉴定卷轴 ＋ 地面物品一律「未知」；NETZACH 淡出阈值收紧到 1 / 3 格

## 17.1 需求原文

> 请对 NETZACH 进行一些修改：变淡 / 完全不可见的距离从 2 格 / 4 格之外变为 1 格 / 3 格之外（同步描述文本）。
> 随后请实现 YESOD 考验。描述：物品无法自然鉴定（鉴定卷轴和感知符石的鉴定不受影响），商店中额外出售
> 一张鉴定卷轴。地面上的所有物品均显示为未知贴图和描述。
>
> 备注：前半段效果中，被禁止的鉴定包括武器、护甲、戒指、法杖的随使用过程自然鉴定；**不**包括卷轴、
> 药水等消耗品的使用时、投掷时鉴定，也**不**包括卷轴的拆分为符石时鉴定，但是**包括**卷轴和药水的
> 直接分解成炼金能量的附加鉴定；后半段效果中，未知的贴图位置为 xy(16,2)，大小 10×15（形状为问号），
> 使用检视功能查看时显示描述为「你不知道这里是什么。」效果仅影响地面上的物品，不影响宝箱和骷髅堆等
> 容器，不影响背包中物品贴图的显示和查看。

三处边界由用户在实现前逐条确认：

| 待定项 | 用户选择 |
|---|---|
| 商店货架（`Heap.Type.FOR_SALE`）是否也变问号 | **也变问号，但价格正常显示** |
| 地面的金币堆是否变问号 | **也变**（`GOLD` 就是普通 `HEAP`，无需特判） |
| 「自然鉴定」的范围 | **只禁四类装备**（武器 / 护甲 / 戒指 / 法杖）；三个「直觉」天赋与神器装备时的自动鉴定**保留** |

## 17.2 定位：这是「三条互相独立的收口线」，不是一条

YESOD 的三段效果落点完全不同，任一段漏掉都**不会报错**（只是效果不全），所以要按收口线逐个封：

| 效果 | 收口线 | 唯一入口 |
|---|---|---|
| 禁自然鉴定 | **闸门式**（在既有判定处插 `else if`） | `Trials.naturalIdDisabled()` |
| 炼金分解附带的鉴定 | **闸门式** | `WndEnergizeItem.energize()`（全作唯一「分解成炼金能量」入口） |
| 地面物品显示为未知 | **显示出口收口**（贴图 / 名字 / 描述三处共用同一判据） | `Trials.hidesGroundHeap( Heap )` |
| 商店补一张鉴定卷轴 | **生成期注入** | `ShopRoom.placeItems()`（**不是** `generateItems()`） |

**故意不碰**（反向判据，写进核验脚本）：

- `Item.identify()`：全作鉴定**总闸**，连英雄初始装备（`HeroClass.initHero` 里几十处）都走它。做成总闸会把
  「开局自带的武器本该是已知的」一起拦掉（而且开局时 `Dungeon.trials` 已经生效）⇒ **绝不能**在这里加闸门。
- `Scroll.ScrollToStone`（卷轴拆符石）、`ScrollOfIdentify` / `ScrollOfDivination` / `StoneOfIntuition`；
- 三个「直觉」天赋（`Talent`）与 `Artifact` 装备时的自动鉴定；
- `ShopRoom.spacesNeeded()`：它读 `itemsToSpawn.size()` 来算**房间最小尺寸** ⇒ 额外那张卷轴若写进
  `generateItems()`，开关这个考验就会改变整层房间布局。

## 17.3 NETZACH：淡出阈值 2 / 4 → 1 / 3

只动两个常量与一条描述，`enemyFade()` 的判定顺序一字未改（`≤NEAR` ⇒ 1.0 / `≤FAR` ⇒ 0.5 / 其余 ⇒ 0）：

```java
public static final int NETZACH_FADE_NEAR = 1;   // 原 2：≤1 格完全不透明
public static final int NETZACH_FADE_FAR  = 3;   // 原 4：>3 格完全不可见
```

⇒ 中间带从「3~4 格」变成「2~3 格」。描述里的「超过 2 格 / 超过 4 格」（en：`more than 2 tiles` /
`more than 4 tiles`）同步改为 1 / 3。**「看不见」仍是纯视觉**：不写 `invisible`、不碰命中与 AI。

## 17.4 YESOD 收口点一：四类装备的自然鉴定

原版在四个地方「数够次数就自动鉴定」，形态完全一致（先判遗忘碎片，再 `identify()`）：

| 站点 | 行 |
|---|---|
| `items/weapon/Weapon.java` | `proc()`（L222） |
| `items/armor/Armor.java` | `proc()`（L553） |
| `items/rings/Ring.java` | `onHeroGainExp()`（L341） |
| `items/wands/Wand.java` | `wandUsed()`（L469） |

⚠️ **护甲那处不在 `absorb()` 里**：原版把「数满即鉴定」写在 `Armor.proc()`（它覆写 `Item.proc`），
`javap -p Armor` 里**根本没有** `absorb` 方法 —— 找站点时别按直觉搜签名。

四处统一插一个 `else if`，与遗忘碎片 `ShardOfOblivion.passiveIDDisabled()` 同口径（数满后只 `setIDReady()`）：

```java
} else if (Trials.naturalIdDisabled()){
    if (usesLeftToID > -1){
        GLog.p( Messages.get( Trials.class, "id_blocked" ), name() );   // ← 只差这一句提示
    }
    setIDReady();
} else {
    identify(); ...
}
```

**为什么是「再加一个 `else if`」而不是改 `setIDReady()` 或 `identify()`**：`setIDReady()` 是「数够了但还没鉴定」
的**共享状态标记**，`identify()` 是所有鉴定路径的共用出口（卷轴 / 符石 / 任务奖励都走）。在这两处动手会连带
影响本考验**不该**管的东西；只在「数满」这一条分支上加判别，作用域最窄。`ShardOfOblivion` 的分支**保持在前**
（它的优先级更高：碎片是「转为待鉴定」，本考验是「永不鉴定」）。

## 17.5 YESOD 收口点二：炼金分解附带的鉴定

`WndEnergizeItem.energize()` 是全作**唯一**的「分解成炼金能量」入口，它顺带做了一次鉴定（场景内是
`AlchemyScene.showIdentify(item)`，场景外是 `item.identify()`）。两处各加一个守卫：

```java
if (!item.isIdentified() && !Trials.naturalIdDisabled()){   // L161：不再弹「鉴定」提示
    ((AlchemyScene) ShatteredPixelDungeon.scene()).showIdentify(item);
}
...
if (!Trials.naturalIdDisabled()){                           // L171：场景外也不再自行鉴定
    item.identify();
}
```

**能量照给**：`Dungeon.energy += item.energyVal();` 在守卫**之前**，分解的收益一点不少 —— 变的只是
「不再顺手告诉你这是啥」。这与「卷轴的直接使用 / 投掷鉴定不受影响」并不冲突：用户区分的是
**分解**这条路径，而不是卷轴这个品类。

## 17.6 YESOD 收口点三：地面物品一律显示为「未知」

三处显示出口读同一个判据，缺一处就会出现「图标是问号、名字却照旧」这类半吊子状态：

| 出口 | 文件 | 做法 |
|---|---|---|
| 贴图 | `sprites/ItemSprite.java` L268 | `view( heap )` 的 `case HEAP: case FOR_SALE:` 里，藏则 `view( ItemSpriteSheet.UNKNOWN_ITEM, null )`，否则照旧 `view( heap.peek() )` |
| 名字（列表 / 检视标题） | `items/Heap.java` L373 | `title()` 开头：`type == HEAP && hidesGroundHeap(this)` ⇒ 返回未知文本（**排在 `switch(type)` 之前**） |
| 描述（检视窗） | `windows/WndInfoItem.java` L52 | `WndInfoItem( Heap )` 的未知分支：`IconTitle(heap)` ＋ 未知文本，**排在「普通堆取 `peek()`」之前** |

判据本身（`Trials.hidesGroundHeap`）只认两种堆型：

```java
if (heap == null || heap.isEmpty()) return false;
if (!Dungeon.isTrialled( YESOD )) return false;
return heap.type == Heap.Type.HEAP || heap.type == Heap.Type.FOR_SALE;
```

⇒ 宝箱 / 上锁宝箱 / 水晶箱 / 墓穴 / 骷髅堆 / 遗骸（`CHEST`、`LOCKED_CHEST`、`CRYSTAL_CHEST`、`TOMB`、
`SKELETON`、`REMAINS`）**全部不受影响**；背包、掉落窗、炼金窗里的物品也都不走这三处出口。

### 贴图：`UNKNOWN_ITEM = xy(16, 2)`，取样矩形 10×15

`ItemSpriteSheet.java` L111 加常量、L126 加矩形：

```java
public static final int UNKNOWN_ITEM    =  xy(16, 2);
assignItemRect(UNKNOWN_ITEM,    10, 15);
```

实测 `items.png`（256×800）第 16 列第 2 行：非透明像素 121 个，包围盒 **正好 x 0..9 / y 0..14 = 10×15**，
**没有透明外边距** ⇒ 10×15 的矩形与图形自然尺寸**逐像素相等**，显示时不会被推偏、也不会被裁。
（为什么必须精确：`assignItemRect` 的 `w/h` 就是 `Image.width()/height()`，与所有按自然尺寸排版的地方耦合。）

**问号图标不会误触发附魔流光**：`view(Heap)` 在 `switch` 之前就把 `viewItem` 置 `null`，我们走的是
`view(UNKNOWN_ITEM, null)` 这条两参重载，`viewItem` 保持 `null`（核心素材本来也没有流光帧）。

### 两个「刻意保留」的细节

1. **金币也是 `Type.HEAP`** ⇒ 一并变问号，无需为它写任何特判。
2. **商店货架的价签仍显示物品名**：`Heap.title()` 的隐名分支只对 `Type.HEAP` 生效；`FOR_SALE` 保留
   `items.heap.for_sale` = 「**%2$s：%1$d金币**」（`%2$s` 就是物品名）。这正是用户选的「货架变问号、但价格
   正常显示」—— SPD 的价格标签字符串天然把物品名写在里面，想连名字一起藏得另改这条本地化文本。
   （`WndTradeItem extends WndInfoItem`，购买窗走 `super(heap)` ⇒ 标题是那条价签、图标是问号、
   正文是未知文本，**购买 / 偷窃按钮照常**，交易流程不受影响。）

## 17.7 商店额外出售一张鉴定卷轴

`ShopRoom.placeItems()` L138（**不是** `generateItems()`）：

```java
if (itemsToSpawn == null) itemsToSpawn = generateItems();
if (Dungeon.isTrialled( Trials.YESOD )){
    itemsToSpawn.add( new ScrollOfIdentify() );
}
```

⚠️ **必须写在 `placeItems()`**：`spacesNeeded()` 读 `itemsToSpawn.size()` 决定房间最小尺寸，而它在
**房间尺寸计算**阶段就被调用 ⇒ 卷轴若加进 `generateItems()`，开关这个考验就会**改变整层布局**
（商店变大 / 与邻房挤压）。放在 `placeItems()` 里，房间尺寸与考验**完全无关**（核验脚本专门钉住
「`spacesNeeded()` 的字节码里不出现 `Trials`」）。

## 17.8 文本（zh ＋ en）

`assets/messages/misc/misc{,_zh}.properties` 三处：`trials.yesod_desc` 从占位换成正式描述，
新增 `trials.yesod_unknown`（检视描述**与**列表标题共用同一条，避免两处口径漂移）与
`trials.id_blocked`（`GLog` 提示，带 `%s` 占位符），并同步改 `trials.netzach_desc` 的 1 格 / 3 格。

- zh：`物品_无法自然鉴定_：武器、护甲、戒指、法杖在_使用过程中_的自动鉴定，以及卷轴与药水_分解为炼金能量_时附带的那次鉴定，都_不会发生_。…`
- 换行是**字面 `\n`**（两个字符），分段用 `\n\n`；两份都保持 CRLF。

## 17.9 落点

| 文件 | 改动 |
|---|---|
| `Trials.java` | `NETZACH_FADE_NEAR 2→1`、`NETZACH_FADE_FAR 4→3`；新增 YESOD 段（类注释里的范围边界与三条收口线 ＋ `naturalIdDisabled()` L659 / `hidesGroundHeap()` L671 / `unknownGroundText()` L678）＋ `Heap` import |
| `sprites/ItemSpriteSheet.java` | `UNKNOWN_ITEM = xy(16,2)`（L111）＋ `assignItemRect(UNKNOWN_ITEM, 10, 15)`（L126） |
| `sprites/ItemSprite.java` | `view( Heap )` 的 `case HEAP: case FOR_SALE:` 加未知贴图分支 ＋ `Trials` import |
| `items/Heap.java` | `title()` 开头加隐名分支（只对 `Type.HEAP`）＋ `Trials` import |
| `windows/WndInfoItem.java` | `WndInfoItem( Heap )` 加未知分支（排在 `fillFields(heap.peek())` 之前）＋ `Trials` import |
| `items/weapon/Weapon.java` | `proc()` 加 `else if (Trials.naturalIdDisabled())`（L222） |
| `items/armor/Armor.java` | `proc()` 加同款分支（L553） |
| `items/rings/Ring.java` | `onHeroGainExp()` 加同款分支（L341） |
| `items/wands/Wand.java` | `wandUsed()` 加同款分支（L469） |
| `windows/WndEnergizeItem.java` | `energize()` 两处守卫（L161 / L171），能量照给 |
| `levels/rooms/special/ShopRoom.java` | `placeItems()` 里 YESOD 时补一张 `ScrollOfIdentify`（L138） |
| `assets/messages/misc/misc{,_zh}.properties` | `trials.yesod_desc` 换正式文案；新增 `trials.yesod_unknown` / `trials.id_blocked`；`trials.netzach_desc` 同步 1 / 3 格 |
| `_chk/patch_yesod_text.py` | **新**：幂等字节级文本批改器（断言每条键恰好命中一次、CRLF 不变、正文只有字面 `\n`） |
| `_chk/YesodProbe.java` | **新**：行为探针（37 断言） |
| `_chk/_build_yesod.sh` | **新**：11 文件单文件 `javac` ＋ 探针运行（并同步刷 `_chk/_javachk`） |
| `_chk/verify_yesod.py` | **新**：六层回归核验（131 断言 ＋ 15 条反例自测） |

## 17.10 核验

| 项 | 结果 |
|---|---|
| `_chk/verify_yesod.py`（新） | **131 条**断言全过 ＋ **23 条反例自测**全过，**0 条 SKIP**。六层：① 源码结构（阈值常量、`callers_of` 引用清单唯一、`seq_ok` 顺序、守卫块里**有** `setIDReady()` 而**无** `identify()`、商店三方法各自该不该含 `YESOD`）② `javap -p` 签名 ③ `javap -c` 字节码（掩码**内联值**、4 站点「取数早于 `identify()`」、`WndEnergizeItem` 的 2 处守卫罩住 `showIdentify`、`Item.identify` 反向无 `Trials`）④ 文本层 ⑤ `items.png` 实测包围盒 ⑥ 行为探针 |
| `_chk/YesodProbe.java`（新，行为探针） | **37 条断言全过 / 0 FAIL**（2 段因 headless 无 `Gdx` 偏好而软跳过，以 `[INFO]` 留痕，**不静默**）：① 贴图常量与取样矩形 ② `naturalIdDisabled()` 门控 ③ `hidesGroundHeap` 真值表（含 6 种容器全 false）④ `Heap.title()`（软段）⑤ `unknownGroundText()`（软段）⑥ NETZACH 淡出阈值 1 / 3 ⑦ `Weapon.proc` 数满后的实跑（YESOD 开：只 `setIDReady()`、`levelKnown` 仍 false；关：确实走进 `identify()`） |
| 单文件 `javac`（`-Xlint:all`，11 文件） | **EXIT=0**（`_chk/_ie_yesod.log`） |
| `_chk/patch_yesod_text.py` | 幂等；en CRLF 285→287、zh 286→288；重跑自动 `[SKIP]` |
| `check_utf8_all.py` | 1474 文件全绿 |
| `check_unused_imports.py` | ALL PASS |
| 回归 | `verify_hod_netzach.py`（阈值已同步为 1 / 3）、`verify_trials_tree_ui.py`（101/0）、`verify_tree_trials_icons.py`（FAIL 0）全绿 |
| 行尾 | `Trials.java` 保持 LF；其余 `.java` 保持 CRLF；`misc{,_zh}.properties` 保持 CRLF 且无裸 LF |

⚠️ **三条核验踩坑（已写回脚本注释，勿重犯）**

1. **常量会被内联**：`Trials.YESOD` 是编译期常量（= 256）⇒ `naturalIdDisabled()` 的字节码是
   `sipush 256` ＋ `invokestatic Dungeon.isTrialled`，**没有** `getstatic Trials.YESOD`。判据要落在
   「内联值 == 源码读出的值」（见 `mask_value` / `has_int_const`）。
2. **`first_invoke(body, 'identify')` 会判反**：它先撞上前面那句 `ldc // String identify_ready`
   （字符串常量，不是调用）⇒ 必须用带冒号的 `'identify:()'`。
3. **`javap` 的 classpath 要顺延到构建产物**：`items/Item.class` 只存在于 `core/build/classes`
   （它被隐式引用，没被显式编进 `_chk/_javachk2`）⇒ 少了它就报「找不到类」，反向判据会静默变成
   `None` 而假 FAIL。另外 `callers_of` 数的是**引用**不是**定义**，所以清单里**不含**定义处 `Trials.java`。

## 17.11 待人工验证

1. 开 YESOD ⇒ 捡到未鉴定武器，一直用到「用够了」：日志应出现「自然鉴定已被_根基考验_禁用…」而**不是**
   直接鉴定；戒指按升级经验、法杖按挥动次数同理。
2. 开 YESOD ⇒ 打怪 / 升级后，武器 / 护甲 / 戒指 / 法杖的等级**始终**是未知（`?`），直到用鉴定卷轴 /
   占卜卷轴 / 感知符石。
3. 开 YESOD ⇒ 用**鉴定卷轴**、**占卜卷轴**、**感知符石**鉴定仍照常生效；**卷轴拆符石**照常；
   卷轴的**使用 / 投掷**鉴定照常。
4. 开 YESOD ⇒ 把一瓶未鉴定药水 / 一张卷轴**分解为炼金能量**：能量照给，但**不再**附带鉴定。
5. 开 YESOD ⇒ 地面掉落物一律「?」图标；长按检视显示「你不知道这里是什么。」；**金币堆**也变问号。
6. 开 YESOD ⇒ **商店**：货架图标是问号、价签仍是「物品名：价格金币」、购买 / 偷窃按钮照常可用；
   且**商店里多出一张鉴定卷轴**（关掉考验后恢复原样）。
7. 开 YESOD ⇒ **容器**（宝箱 / 上锁宝箱 / 水晶箱 / 墓穴 / 骷髅堆）图标与检视**完全不受影响**；
   背包里物品的贴图与检视也完全不受影响。
8. 开 NETZACH ⇒ 怪在 **1 格内**完全不透明、**2~3 格**半透明、**>3 格**看不见（血条一并消失）；
   考验界面描述文本应为 1 格 / 3 格。
9. 关掉两条考验 ⇒ 鉴定、商店、地面显示、淡出与旧版逐字一致。
