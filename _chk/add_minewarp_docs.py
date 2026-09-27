# -*- coding: utf-8 -*-
"""把「矿洞虚空地形两条收紧 + 新调试道具矿洞跃迁符」追加到 docs/features.md（幂等）。

约定（见 AGENTS.md / skill egopd-source-verify）：
  * 字节级 IO，保留原行尾（features.md 是**纯 LF**）；
  * 追加前断言「尾部锚点」与「全文件无裸 CR」；
  * 已经追加过就直接跳过（幂等）；
  * 改前副本留 _chk/_bak_minewarp/。
用法：python _chk/add_minewarp_docs.py
"""
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
DOC = os.path.join(ROOT, 'docs', 'features.md')
BAK_DIR = os.path.join(HERE, '_bak_minewarp')

TITLE = '# 2026-09-26（补）矿洞虚空地形两条收紧 ＋ 新调试道具「矿洞跃迁符」'

# 追加前文件必须以这个结尾（'\n---\n\n'）。锚点取自本文件最后一条 2026-09-26 章节的收尾分隔线。
TAIL_ANCHOR = '\n---\n\n'

CHAPTER = TITLE + '''

> 上一条「带着标记倒下 ＋ 虚空地形」见本文件的 `2026-09-26` 章节。这一条是它落地后的**实测补记**：
> 用行为探针真建关卡跑 `carveVoid` 之后，发现两处与注释宣称不符（或不够好）的地方，收紧；
> 同时为「反复看地形」加了一个调试道具。

## 一、`MiningLevel.carveVoid` 的两条收紧

### 1. 圆盘之前是**没有夹取**的 ⇒ 「外面留一整圈地板」其实不成立

原写法是「圆心在深内区（房间内圈再各缩 2 格）、半径 1~3 的切比雪夫圆盘」，
但**圆盘本身没有被夹在深内区里**。圆心落在深内区角落时，圆盘会往房间内墙方向溢出 `radius` 格：
20×20 探针房 20 次采样里有 **146 格**深渊落在深内区之外，最远直接贴到房间内墙那一圈。

后果是玩家**沿墙走一步就可能掉下去**（水晶任务下只是被送回本层 + 摔伤，但仍然很别扭），
而注释里写的「不封死任何人」并没有依据。

**修法**：逐格加一句夹取，整块圆盘落进深内区：

```java
if (x < left || x > right || y < top || y > bottom) continue;
```

夹掉的部分**不参与随机数消耗**（放在 `Random.Int(2)` 之前），所以「同 seed 逐格可复现」依旧成立。

### 2. 外圈 50% 抽取会留下**孤零零的单格陷阱**

外圈（`d == radius`）按 50% 概率保留是照 `FissureRoom` 的边缘不规则写法。但 `FissureRoom` 是在房间里拉长线，
这里是个圆盘 ⇒ 「相邻两格都被抽掉」的角落格会变成一个**不属于任何连通块**的单格深渊。
20 次采样里 17 次出现多块（最多 4 个孤立角格）。

**修法**：分两趟。第一趟挖实心核（`d < radius`）并把外圈候选存进 `fringe`；
第二趟只有「四邻里至少一个也是深渊」的外圈格才真挖：

```java
for (int cell : fringe){
    if (map[cell - 1] == Terrain.CHASM || map[cell + 1] == Terrain.CHASM
            || map[cell - w] == Terrain.CHASM || map[cell + w] == Terrain.CHASM){
        Painter.set( level, cell, Terrain.CHASM );
    }
}
```

`±1` / `±w` 一定在数组内：地图形状保证深内区外面还有「房间内圈 + 边界墙」。
`Random.Int(2)` 的**消耗顺序与格序完全没变**，同 seed 仍逐格可复现。

### 3. ⚠️ 一条**不是** bug 的现象：矿脉会把虚空切成几块

`MineSmallRoom.paint` 是**先撒矿脉再挖虚空**，而挖虚空只吃 `EMPTY` / `EMPTY_DECO`
⇒ 留在原地的矿脉（`MINE_CRYSTAL`）就成了虚空里的**踏脚石**，一块圆盘因此可能被切成几块。

这是「只吃地板、不动矿脉」这条规则的正确结果，**不是**回归。所以探针把「单块性」拆成两段证：
无阻挡的干净地板上必须恰好 1 块；带矿脉/墙体的场景只证「只吃地板」与「不越界不贴墙」。

## 二、行为探针 `_chk/MiningVoidProbe.java`（26 条断言）

静态结构断言证不了下面这些事，所以真建关卡跑（`Game.version` / `Level.mobs·heaps·blobs` 要手工补，
否则一开头就 NPE）：

| 段 | 证什么 |
|---|---|
| ① | `handlesChasmFall()` 只对 `CRYSTAL` 为真；`GNOLL` / `FUNGI` / 未开始 / 基类 `Level` 全 false |
| ② | 只吃地板 ／ 贴内墙那圈永远是地板 ／ 整块夹在深内区 ／ 干净地板上恰好 1 块 ／ 与内墙至少隔 1 格 |
| ③ | `protectedCell` 及其八邻格一次都没被挖，同时仍挖出了虚空（不是「因保护而干脆不挖」） |
| ④ | 深内区全铺墙体 ⇒ `map[]` 逐格不变（不产生半截挖掘） |
| ⑤ | 深内区为空 ⇒ 早退不动地形；深内区仅 2×2 ⇒ 夹紧后仍挖得出且不越界 |
| ⑥ | ⭐ **「只写 `map[]` 安全」的前提实证**：不调 `buildFlagMaps()` 时深渊格 `pit[]` 全 false（幽灵格风险真实存在），调完之后逐格变 true、`passable[]` 变 false、普通地板一格没被误标 |
| ⑦ | 同 seed 两次 `carveVoid` 的 `map[]` 逐格相同 |
| ⑧ | 从房间内环出发能走遍所有非墙非深渊格 ⇒ 虚空不会把房间切成孤岛 |

探针输出接进 `_chk/verify_marked_kill_routes.py` 的 **J 组**（`_miningvoid.out`，含反例自测）；
该脚本 F 组同期补了 F16（夹取）/ F17（孤立格回收）两条结构判据。

## 三、新调试道具「矿洞跃迁符」`items/CrystalMineWarp.java`

需求原话：*「创作一个道具，能快速传送到水晶任务矿洞层，方便测试地形生成。」*

放在 **`items` 根包** ⇒ F2 调试窗「杂项」与调试控制台 `ScanProvider.shallow("杂项", Item.class, PKG + "items")`
都会自动收录，**不需要注册**；同时它**不进任何掉落池 / 商店 / 开局奖励**
（判据里钉死「全仓除本文件外零引用」）。与既有的 `TestPortal` 同属「调试道具」家族：
`isIdentified() = true`、`isUpgradable() = false`、无限次使用不消耗。

### 三个动作

| 动作 | 行为 |
|---|---|
| `WARP`（跃迁，默认动作） | 进矿洞；目标深度在 11→12→13→14 之间轮换，**保留**该层地形（同深度可复现 ⇒ 适合改完代码做前后对照） |
| `REROLL`（重 roll） | 同样轮换深度，但先 `Dungeon.generatedLevels.remove(...)` 清掉生成记录 ⇒ 逼引擎走 `newLevel()` 重新生成，每按一次都是新地形 |
| `RETURN`（返回） | 回到出发前那一层的**原坐标**；只在矿层里出现 —— 矿层唯一的出口楼梯要「镐子 ＋ 任务进度」，不给回程会把人卡在矿里 |

### 三处必须踩准的地方

1. **虚空地形只在 `Blacksmith.Quest.Type() == CRYSTAL` 下生成**（房间 `paint()` 与 `handlesChasmFall()` 都读它）
   ⇒ 道具会**临时把任务种类顶成水晶**，并在「返回」时按离开前的值还原。
   `Blacksmith.Quest.setType(int)` 是本次新加的钩子：只开放这一个 setter，`type` 字段仍是 `private static`。
2. **目标深度必须显式给 11~14**：`Dungeon.newLevel()` 里只有 `branch == 1 && depth ∈ [11,14]` 才产出 `MiningLevel`，
   否则掉到 `DeadEndLevel`。所以哪怕玩家正站在 7 层（监狱）用符，也照样落到真正的矿层。
3. **跳层走既有的 `InterlevelScene.Mode.RETURN`**（与 `TestPortal` / 传送卷轴 / 灯芯草同一条路），
   落点 `returnPos = -1` 交给 `Dungeon.switchLevel` 回退到该层入口（矿层入口就是 `MineEntrance` 那格楼梯），
   并先调 `Level.beforeTransition()`。
   **没有**去伪造 `LevelTransition`——那条路要求目标层存在对应类型的过渡点，而「从任意层跳进任务分支」并不满足这个前提。

### 已知副作用（都是刻意的）

- 「重 roll」会洗掉该矿层已探索的地图（调试道具，属预期行为）；
- 任务种类会被顶成水晶、离开时还原；但若玩家**没按返回**就退出，任务种类会保留为水晶。

### 核验

`_chk/verify_crystal_mine_warp.py`（32 条断言 ＋ 反例自测），覆盖：道具本体与调试契约、
`actions()` 里「返回」的门控、跃迁/重 roll 路径的七个必踩点、返回路径的还原与落点、
`setType` 钩子的只读性、三份 `items*.properties` 文本键、以及「两个调试入口仍是 items 根包浅层扫描 ＋ 零掉落渠道」。
'''

data = open(DOC, 'rb').read()
text = data.decode('utf-8')

# ── 前置断言 ──
if b'\r' in data:
    print('ABORT: docs/features.md 含裸 CR（本文件约定纯 LF）')
    sys.exit(2)
if TITLE in text:
    print('SKIP: 该章节已在 docs/features.md 里（幂等）')
    sys.exit(0)
if not text.endswith(TAIL_ANCHOR):
    print('ABORT: 尾部锚点不匹配。期望结尾为 %r，实得末尾 40 字符 %r'
          % (TAIL_ANCHOR, text[-40:]))
    sys.exit(2)

# ── 备份 ──
os.makedirs(BAK_DIR, exist_ok=True)
shutil.copy2(DOC, os.path.join(BAK_DIR, 'features.md'))

before_lines = text.count('\n')
new_text = text + CHAPTER.rstrip('\n') + '\n'
new_data = new_text.encode('utf-8')

assert b'\r' not in new_data, 'ABORT: 追加后出现裸 CR'
open(DOC, 'wb').write(new_data)

after_lines = new_text.count('\n')
print('OK: docs/features.md %d 行 → %d 行（+%d）；副本 _chk/_bak_minewarp/features.md'
      % (before_lines, after_lines, after_lines - before_lines))
