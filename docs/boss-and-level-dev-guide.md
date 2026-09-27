# Boss 与楼层/区域开发参考手册

> 勘察日期：2026-09-09 ｜ 适用代码库：`D:\PD`（core 模块，SPD 引擎 fork）
> 用途：为**新 Boss 设计**、**新 Boss 层 / 新楼层 / 新区域开发**提供前置知识。所有结论均来自源码实读，附文件与行号便于回溯。
> 行号以本次勘察当日为准，后续改动可能漂移；引用时以"类名 + 方法名"为准。

---

## 0. 速览结论（TL;DR）

| 问题 | 一句话答案 | 详见 |
|---|---|---|
| Boss 动画需要多少状态 | **必选 4 态**：`idle / run / attack / die`；远程/技能追加 `zap`（或 `operate`）；Boss 可自定义任意**专属动画字段**（Goo `pump`、DM300 `charge/slam`） | §1 |
| Boss 层如何固定地形 | 每个区域一个 `XxxBossLevel extends XxxLevel`，用**专用房间包**拼固定布局 + 随机竞技场变体；门/出口用 `seal/unseal` + `LOCKED_EXIT/WornKey` 锁 | §2 |
| 新增楼层/区域要做什么 | 建区域 Level 类（覆写 10+ 个方法）、区域 Painter、房间风格包、美术资源、在 `Dungeon.newLevel()` 挂分支；见 §3 清单 | §3 |
| Boss AI 怎么配 | 直接继承 `Mob`：`act()` 主循环 + 覆写 `damageRoll/attackSkill/defenseSkill/attackProc/canAttack/doAttack/die/notice` 等钩子；不需要改寻路 | §4.1-4.3 |
| 伤害/buff/免疫判定 | 伤害用 `damageRoll()`；命中附 buff 在 `attackProc` 里 `Buff.affect`；免疫走 `Char.isImmune/resist/isInvulnerable`（自身字段 + `Property` 表 + Buff 表三通道） | §4.3-4.4 |
| 多阶段 Boss | 三种现成模式：① HP 阈值 + `phase` 字段（DwarfKing/Yog）② 环境/关卡状态驱动（Tengu/DM300+pylon）③ 多实体合战（Yog+六拳、DwarfKing 召唤） | §4.5 |

---

## 1. Boss 动画帧配置

### 1.1 三个基本文件与接线

做一只新 Boss 的"皮"需要三处：

| 文件类型 | 内容 | 样例 |
|---|---|---|
| `actors/mobs/XxxBoss.java` | 角色逻辑；数据块里写 `spriteClass = XxxBossSprite.class;` | Goo 数据块 `actors/mobs/Goo.java:57` |
| `sprites/XxxBossSprite.java` | 精灵类：贴图 + 帧表 + 动画定义 | `sprites/GooSprite.java` |
| `Assets.java` 的 `Assets.Sprites` | 贴图资源常量：`GOO = "sprites/goo.png"` | `Assets.java:296`（KING:291、DM300:288、TENGU:301、YOG:313、FISTS:312、LARVA:314） |

- Mob→精灵映射：`Mob.sprite()` 用反射实例化 `spriteClass`（`actors/mobs/Mob.java:229`）。所有角色（含普通怪）都是这一套。
- 帧表：`TextureFilm frames = new TextureFilm(texture, 帧宽, 帧高);` —— 把整张贴图按**等宽高等的网格**切分，帧序号 = 网格单元从左到右、从上到下的线性序号。
- 动画：`Animation(帧间隔, 是否循环)` + `.frames(frames, 帧号...)`。

### 1.2 六个内置动画槽位（`CharSprite`）

`sprites/CharSprite.java:90-95` 定义了六个 `protected Animation` 槽位：

```java
protected Animation idle;     //待机（循环）
protected Animation run;      //移动（循环）
protected Animation attack;   //近战攻击（一次性）
protected Animation operate;  //操作/施法（一次性，怪物里少用）
protected Animation zap;      //远程施法（一次性）
protected Animation die;      //死亡（一次性，播放中不可被 play() 打断）
```

对应触发方法：`idle()/move()/attack(cell)/operate(cell)/zap(cell)/die()`（`CharSprite.java:213-357`）。**子类精灵不设置某槽位 = 该动作发生时没有动画**（会直接异常或表现残缺），所以新 Boss 至少要配齐其行为会用到的全部槽位。

**经验下限**：近战 Boss 至少 `idle/run/attack/die` 四态；远程/弹道 Boss 加 `zap`；有特殊大招的 Boss 再加专属态（见下）。

### 1.3 面向 Boss 的专属状态与"动画↔逻辑"回调

Boss 精灵的看点是**自定义 Animation 字段** + **onComplete 回连**：

- **Goo**（`GooSprite.java:42-84`）：在四基础态之外定义 `pump`（蓄力循环）与 `pumpAttack`（蓄力砸地一次性，末帧=爆发帧 7）。业务逻辑（`Goo.pumpUp(...)` / 蓄力攻击）通过 `sprite.pumpUp()` / `sprite.pumpAttack()` 驱动动画；动画播完在 `onComplete` 里回连回合：
  ```java
  if (anim == pumpAttack) { triggerEmitters(); idle(); ch.onAttackComplete(); }  //GooSprite.java:210-222
  ```
- **DM300**（`DM300Sprite.java`）：额外 `charge`（自转蓄电）与 `slam`（震地，clone attack）；`zap` 覆写发出毒雾弹 `MagicMissile` 后回调 `((DM300)ch).onZapComplete()`；`onComplete` 里 `slam` 结束调 `onSlamComplete()`；死亡 = 20 帧闪烁后 `BlastParticle` 爆炸 + `killAndErase`。
- **多形态/狂暴换皮**：DM300 `updateChargeState(boolean enraged)`（`DM300Sprite.java:52-84`）在狂暴时把 idle/run/attack 的帧整体**偏移一行**（`c = enraged ? 10 : 0`，同一张贴图两行分别是普通/狂暴动作），并开关周身电弧粒子。半血以下 `link()` 自动切狂暴、`blood()` 覆写溅血颜色。
- **演出辅助**：Emitter 粒子（Goo 黑水喷雾 `spray`、DM300 电火花）、`blood()` 血颜色、投掷弹道（Tengu 的 `attack()` 在非相邻时改用 `MissileSprite` 扔手里剑，`TenguSprite.java:93-112`）。
- **同 Boss 多阶段、不同帧尺寸换皮（本 mod 先例 2026-09-09，`SmilingCorpseMountain`）**：阶段间帧网格尺寸不同（64×48 / 48×48 / 32×24）时，DM300 的"同贴图换帧行"不可用——改为在**单个精灵类内**换整张 `texture` 并按新尺寸重建 `TextureFilm` + 四个动画槽（`protected` 槽位可重赋值，参照 DM300Sprite 模式），随后**先 `play(idle)` 再 `place(ch.pos)`**。mob 侧由 HP 区间驱动 `sprite.setPhase(p)`（幂等），触发入口：`damage()` 之后 与 `updateSpriteState()`（`CharSprite.link`→`ch.updateSpriteState()`，link/读档自动校正形态）。
  - ⚠️ **换皮对位大坑（2026-09-09 实测修复）**：顺序绝不能反。`Image.texture()` 会把 `width/height` 临时置为**整张贴图**尺寸，只有 `MovieClip.play()` 才把帧矩形（与宽高）切到**单帧**；而 `CharSprite.place()`→`worldToCamera()` 按当前 `width()/height()` 做水平居中与底边贴地。若先 `place` 后 `play`，对位基准是整图/旧帧尺寸 → 换皮后精灵与头顶血条（`CharHealthIndicator` 每帧读 `sprite.x/y/width`）整体大幅偏移且永不回正（PASSIVE/待机不移动则不再触发 place）。同理：任何"运行期换不同尺寸贴图/帧"后需要重新对位时，都必须保证帧（宽高）已切到位再 `place`。

### 1.4 Boss 精灵制作 Checklist

1. 准备贴图 `assets/sprites/xxx.png`，确认**帧网格尺寸**（如 Goo 20×14、DM300 25×22、Tengu 14×16）。
2. `Assets.java` 注册 `Assets.Sprites.XXX` 常量。
3. 新建 `XxxBossSprite extends MobSprite`：`texture()` → `TextureFilm` → 逐态 `new Animation(...).frames(...)` → `play(idle)`。
4. 如有大招：自定义字段 + 对外方法（供 mob 调）+ `onComplete` 回连 `ch.onXxxComplete()`。
5. 特殊表现：死亡特效、粒子、狂暴换行、`blood()`。
6. Boss mob 数据块设 `spriteClass = XxxBossSprite.class`。

---

## 2. Boss 层地形与生成

### 2.1 楼层布局总览（主线 depth → Level 类）

工厂：`Dungeon.newLevel()`，`Dungeon.java:302-380`：

| 深度 | Level 类 | 区域/Boss |
|---|---|---|
| 1–4 | `SewerLevel` | 下水道 |
| **5** | `SewerBossLevel` | **Goo** |
| 6–9 | `PrisonLevel` | 监狱 |
| **10** | `PrisonBossLevel` | **Tengu**（981 行，环境大战） |
| 11–14 | `CavesLevel` | 矿洞 |
| **15** | `CavesBossLevel` | **DM300**（桩塔战） |
| 16–19 | `CityLevel` | 城市 |
| **20** | `CityBossLevel` | **DwarfKing** |
| 21–24 | `HallsLevel` | 地狱回廊 |
| **25** | `HallsBossLevel` | **YogDzewa + 六拳** |
| 26 | `LastLevel` | 安卡放置层（`drop(new Amulet(), AMULET_POS)`，`LastLevel.java:169`） |
| 其他 | `DeadEndLevel` | 兜底 |

分支（branch=1）：11–14 → `MiningLevel`（矿坑支线）、16–19 → `VaultLevel`（宝库）；其余分支 → `DeadEndLevel`。**加楼层 = 改这张表 + 边界语义**（详见 §3）。

### 2.2 Boss 层标准套路（以 `SewerBossLevel` 为范本，232 行）

`SewerBossLevel extends SewerLevel`（区域主题全部继承），覆写了这些方法：

| 方法 | 作用 |
|---|---|
| `initRooms()`（:85-104） | 布局 = 专用入口房 `SewerBossEntranceRoom` + 专用出口房 `SewerBossExitRoom` + **3 个强制小标准房**（`setSizeCat(0,0)`）+ **随机 Goo 竞技场** `GooBossRoom.randomGooRoom()` 作为 `FigureEightBuilder` 的 **landmark 房** + 秘密 `RatKingRoom` |
| `standardRooms()`（:107） | 数量收敛到 2–3 间，避免杂房干扰 |
| `builder()`（:113） | `FigureEightBuilder` 环路生成器（入场→竞技场→出口成环） |
| `painter()`（:121） | 区域 Painter（水面/草丛/陷阱参数） |
| `createMobs()`（:133） | **空**——Boss 不进"常规刷怪"，由竞技场房间或 seal 生成 |
| `createItems()` / `addRespawner()=null` | 只放玩家遗物；禁刷怪器 |
| `randomRespawnCell()`（:157） | 复活点限在入口房内 |
| `seal()/unseal()`（:177/197） | 入口房淹水关门 / 战后复原（覆写父类 `Level.seal/unseal`） |
| `playLevelMusic()`（:62） | `locked` 或 Goo 存活 → boss BGM；否则区域 BGM |
| `addVisuals()` | 出口两侧火把等装饰 |
| `restoreFromBundle()` | 读档后 `roomExit = roomEntrance` 兜底 |

### 2.3 "固定地形 + 差异化变体"机制（关键答案）

- **每个 Boss 层都有专属房间包**：如 `levels/rooms/sewerboss/`（Goo 系：`GooBossRoom` 抽象基类 + 变体 `DiamondGooRoom / WalledGooRoom / ThinPillarsGooRoom / ThickPillarsGooRoom`），外加 `XxxBossEntranceRoom / XxxBossExitRoom`（继承 `standard/entrance|exit` 的基类改画）。
- **固定骨架 + 随机变体**：入口、出口、连通关系固定（保证可玩性与演出），**竞技场本体随机抽一种变体**：
  ```java
  public static GooBossRoom randomGooRoom(){ ... }  //GooBossRoom.java:46-56，5 种里加权选
  ```
  每个变体只覆写 `paint(Level)`（钻石场/立柱场/围墙场…… 的画法不同，`DiamondGooRoom.java:33-67`），战斗逻辑零改动。
- **Boss 生成（两种时机，任选）**：
  1. **随房间 paint 生成**：变体房 `paint()` 里 `Goo boss = new Goo(); boss.pos = center; level.mobs.add(boss);`（`DiamondGooRoom.java:64-66`）。Boss 默认 `SLEEPING`，玩家走近由常规"惊醒"机制激活。
  2. **seal 脚本生成**：进入战斗触发时才 new 并 `GameScene.add`，显式设 `boss.state = boss.WANDERING` 并挑 `openSpace` 位置（`CavesBossLevel.java:328-333`、`HallsBossLevel.java:263`）。Tengu 由 `PrisonBossLevel` 持有 `tengu` 字段独立管理（`PrisonBossLevel.java:590`）。

### 2.4 出口封锁与战斗封锁的语义

两条独立机制，容易混，拆开记：

**A. 出口（下楼梯）封锁——地形 + 钥匙**
- 出口房把楼梯位画成 **`Terrain.LOCKED_EXIT`**（`Terrain.java:53`，flags=SOLID 不可走；对应 `UNLOCKED_EXIT`=22 可走，`:113-114`）。例：`SewerBossExitRoom.java:63`。
- 解锁动作发生在**英雄交互**：靠近 `LOCKED_EXIT` 执行开锁，**消耗 `WornKey(该深度)`** 后原地改写为 `UNLOCKED_EXIT`（`Hero.java:2573-2577`）。
- 因此每个 Boss 死亡都掉 **`WornKey(Dungeon.depth)`**（Goo 掉钥匙见 `Goo.java` die 段），玩家"杀 Boss → 捡钥匙 → 开楼下门"。
- 另见变体：Caves/Halls 不用钥匙，Boss 死后由 `unseal()` 直接程序化开门（把 gate 行改写为 EMPTY / 把 exit 位改写为 EXIT，`CavesBossLevel.java:350-357`、`HallsBossLevel.java:292-296`）。

**B. 战斗封锁（进场关门）——`Level.seal/unseal` + `LockedFloor`**
- `Level.seal()`（`Level.java:638-646`）：`locked=true` + 给英雄挂 `LockedFloor`（**期间禁自然回血**，见 `Regeneration.java:85` 检查）。
- `Level.unseal()`：`locked=false` 并摘除 `LockedFloor`。
- 触发时机由 Boss/关卡自行决定，三类现成写法：
  1. Boss 醒来即封：Goo `act()` 里 `if (state != SLEEPING) Dungeon.level.seal();`（`Goo.java:134-136`）；
  2. 玩家靠近桩塔/深入竞技场：覆写 `Level.occupyCell()`（`CavesBossLevel.java:278-291` 距离 ≤3 触发、`HallsBossLevel.java:245-252` 离入口 ≥2 触发）；
  3. Boss 首次 `notice()` 唤醒时同时 `BossHealthBar.assignBoss(this)`（见 §4.6）。
- seal 的视觉演出在各 BossLevel 的 `seal()/unseal()` 覆写里做（入口淹水 `SewerBossLevel.java:184`、塌方封门 + `PixelScene.shake` `CavesBossLevel.java:294-326`、火焰封门 `HallsBossLevel.java:253`）。

### 2.5 Boss 战完整时序（下水道局为例）

```
下楼 → SewerBossLevel 构建(固定房+随机竞技场+Goo 沉睡在场心)
→ 穿过入口房/走廊逼近 → Goo 惊醒(notice) → act() 发现已醒 → level.seal()
→ 入口淹水(不能折返) + LockedFloor(禁回血) + boss BGM + BossHealthBar 出现
→ 战斗中：Goo 蓄力(pump) → 玩家见警示走位 → 爆发 AoE
→ Goo 死亡：super.die(cause) → level.unseal() → 入口复原 → 掉 WornKey/战利品 → 台词
→ 英雄下楼口处交互 LOCKED_EXIT 消耗 WornKey → 变 UNLOCKED_EXIT → 下楼
```

### 2.6 其余 Boss 层形态（设计参考）

| 层 | 形态 |
|---|---|
| `PrisonBossLevel`（981 行） | **环境战**：持有 `State`（阶段状态机）+ `tengu` 引用；Tengu 阶段 1 用飞镖+三系场地技（`BombAbility/FireAbility/ShockerAbility` 都是 Buff/Blob），阶段切换台词驱动（`Tengu.java:137-196`） |
| `CavesBossLevel` | **桩塔战**：pylon 布点 + `PylonEnergy` blob + 石墙 gate；桩塔供能 → DM300 无敌蓄能，打桩 → 破防（`DM300.java` `pylonsActivated/supercharged`） |
| `HallsBossLevel` | Yog + 六拳最终战（出场于 `exit()+width*3` 竞技场） |
| `LastLevel` | 非 RegularLevel 的自定义 `Level`，放安卡（胜利流程） |

---

## 3. 新增楼层 / 新区域：工作清单

> 分两种情况：**A. 只加一个普通楼层**（改深度表）；**B. 加一个完整"区域"**（风格化多层 + Boss）。以下以 B 为主线，A 是 B 的降级版。

### 3.1 必改清单（代码面）

| # | 改动 | 落点 / 样例 | 说明 |
|---|---|---|---|
| 1 | **楼层类** | `levels/XxxAreaLevel.java`，模板照抄 `SewerLevel.java`（300 行内） | 需覆写（已核验）：
- `initRooms()`（含区域特色房间池）
- `standardRooms()/specialRooms()`
- `painter()`（区域 Painter + 水/草/陷阱参数）
- `tilesTex()/waterTex()`（区域贴图纹理名）
- `trapChances()`、`createMobs()`（刷怪表）
- `playLevelMusic()`、`addVisuals()`
- 可选：`tileName/tileDesc`（自定义地形名）、`occupyCell/activateTransition` 特判 | 
| 2 | **区域 Painter** | `levels/painters/XxxPainter.java`（`SewerPainter/PrisonPainter` 风格） | 负责用区域地形素材铺地、水、装饰 |
| 3 | **房间风格包** | `levels/rooms/standard/Xxx*Room`（entrance/exit 变体 + 区域化 StandardRoom） | 参考 `standard/entrance|exit` 大量变体文件 |
| 4 | **美术资源** | 区域 tileset / water 图 + `Assets.Environment`/贴图常量 | 纹理名与 `tilesTex()` 对应 |
| 5 | **深度表** | `Dungeon.newLevel()` switch（`Dungeon.java:309-360`） | 插分支；注意 26 之后的兜底是 `DeadEndLevel` |
| 6 | **周边联动（需二次排查清单）** | `Dungeon`/`Statistics.deepestFloor`、胜负判定、Boss 计分数组 `bossScores[]`、`STRONGER_BOSSES` 挑战、商店/任务房出现深度、各 `XxxLevel instanceof` 特判点、区域 BGM 常量与清单 | 加第 27+ 层或通关后区域时尤其要看胜负判定与 Badges |

### 3.2 若做"新 Boss 层"，额外改动

1. `levels/XxxBossLevel extends XxxAreaLevel`（模板 `SewerBossLevel`，§2.2 的 11 个覆写点）。
2. 新建 `levels/rooms/<area>boss/` 房间包：入口房 / 出口房 / 竞技场抽象基类 + N 个随机变体（模板 `rooms/sewerboss/`）。
3. 新 Boss mob + 精灵（§1）+ `BossHealthBar` 挂接（§4.6）+ 死亡掉钥匙与 `unseal()` 覆写。

### 3.3 楼层类职责速查（SewerLevel/PrisonLevel 实测方法集）

除 RegularLevel 提供的能力外，区域子类实际覆写的表面见 §3.1 表；再对照 `RegularLevel.java`（969 行）的 `build()→initRooms→builder→painter→createMobs→createItems` 装配顺序，即可定位任何一步想定制的位置。

---

## 4. Boss AI 与机制设计

### 4.1 AI 状态机（谁在驱动行为）

`Mob` 内部用 **AiState 接口**（`Mob.java:1119`）实现六种状态：

```java
public AiState SLEEPING / HUNTING / INVESTIGATING / WANDERING / FLEEING / PASSIVE;  //Mob.java:121-126
public AiState state = SLEEPING;
```

- 每种状态是一个内部类（`Sleeping:1123`、`Wandering:1214`、`Hunting:1281`、`Investigating extends Wandering:1372`、`Fleeing:1397`、`Passive:1449`），核心实现 `act(boolean enemyInFOV, boolean justAlerted)`。
- **默认 Boss 不需要碰寻路**。`Mob.act()`（:234）负责状态分派：唤醒→选敌→追敌→攻击的通用流程都内置。
- **两条定制路径**：
  1. **覆写行为钩子**（最常用）：`canAttack/getCloser/getFurther/doAttack/attackProc/defenseProc/damage/die/notice/chooseEnemy`。
  2. **换掉某个状态的 AI**：`Tengu` 自定义 `private class Hunting extends Mob.Hunting { ... }`（`Tengu.java:385`），把特定阶段的追猎/走位逻辑整体替换。
- 选敌优先级 `chooseEnemy()`（`Mob.java:284+`）：Dread/Terror 来源 → Aggression 标记目标 → 无目标/目标死亡/Amok/Charm 时重选 FOV 内最近威胁。

### 4.2 面板数值与伤害判定

**数据块**（实例初始化块内声明，Goo 为范本 `Goo.java:53-62`）：

```java
{
    HP = HT = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 120 : 100;  //挑战加成在此做
    EXP = 10;
    defenseSkill = 8;
    spriteClass = GooSprite.class;
    properties.add(Property.BOSS);   //身份即免疫（见 §4.4）
    properties.add(Property.DEMONIC / ACIDIC ...);  //按种族特性补充
}
```

**伤害相关覆写面**（全部可选，都有默认值）：
- `damageRoll()`：攻击骰，Boss 半血狂化常数类变化（`Goo.java:68-81`：半血 max 8→12；蓄力×3 且附带计分惩罚）。
- `attackSkill(target)/defenseSkill(enemy)/drRoll()`：命中/闪避/减伤（`Goo.java:84-99` 半血全面增强）。
- 引擎主流程：`Mob.act → Hunting → canAttack → attack/enemy.damage`，命中结算在 `Char.attack()`（含暴击/背刺/附伤）。

### 4.3 攻击伤害、buff 施加的挂点

| 想做的事 | 覆写/调用点 | 样例 |
|---|---|---|
| 改变单次攻击骰 | `damageRoll()` | Goo 半血/蓄力 |
| 攻击**命中后**给敌人上 debuff | `attackProc(enemy, dmg)` | Goo 33% 概率 `Buff.affect(enemy, Ooze).set(...)`（`Goo.java:155-160`） |
| 特殊招式 / 大招判定 | `canAttack` + `doAttack` 覆写 | Goo `pumpUp` 蓄力蓄满后 `doAttack` 走区域爆发（AoE 通过 `sprite.pumpAttack` + 弹道/范围内结算） |
| 给自己上 buff / 阶段性强化 | `act()` 或 `damage()` 内 | DM300 破盾回充 `Buff.affect(this, Barrier).setShield(30+(HT-HP)/10)`（`DM300.java:335-343`） |
| 场地/范围持续伤害 | Buff + Blob（Tengu `FireAbility/FireBlob/ShockerBlob`） | 走地形污染而非单次命中 |
| 被击中的特殊处理 | `defenseProc` / 覆写 `damage(int,Object)` | DwarfKing 按 phase 改承伤逻辑（延迟伤害 `Viscosity.DeferedDamage`） |

### 4.4 免疫 / 抗性 / 无敌判定（三通道）

角色级判定入口在 `Char`（`Char.java`）：
- `isImmune(Class effect)`（:1537）— **buff/异常能否上身**；
- `resist(Class effect)`（:1517）— 伤害/效果的抗性折减（命中类 0.5×/个）；
- `isInvulnerable(Class effect)`（:1567）— 免疫 **一切**伤害类（Boss 用它做"阶段无敌窗口"）。

三者都聚合**三条来源**：
1. **角色自身字段** `immunities/resistances`（`Char.java:1513/1535`，protected HashSet）——Boss 直接 `immunities.add(Roots.class)` 加（Tengu `Tengu.java:350-353` 免 Roots/Blindness/Dread/Terror）；
2. **Property 表**（`Char.Property` 枚举，`Char.java:1582-1607`）：每个属性自带 `resistances/immunities` 类表，遍历 `properties()` 合并。**`BOSS` 属性默认 = 抗即死类（Grim/GrimTrap/ScrollOfRetribution/ScrollOfPsionicBlast）+ 免疫 `AllyBuff`/`Dread`**；`MINIBOSS` 免疫同款但无即死抗；还有 `UNDEAD/DEMONIC/INORGANIC/FIERY/ICY/ACIDIC/ELECTRIC/IMMOVABLE/STATIC` 等；
3. **Buff 免疫**：角色身上的 buff 若覆写 `immunities()/resistances()` 也会叠加（`Char.java:1522-1524/1550-1552`）。

> ⚠️ 本 fork 已有"破免疫"先例：`AimHeartMark`（瞄准心脏）使带标记单位失去燃烧/麻痹免疫（`Char.java:1538-1544`）——自研 Boss 机制可参考"给目标加标记 → 在 `isImmune` 入口特判"的写法。

### 4.5 多阶段 Boss：三种现成模式

**模式 A：HP 阈值 + `phase` 字段自管理（最通用）**
- 代表：DwarfKing、YogDzewa。
- 写法：`private int phase;` 存 bundle（`DwarfKing.java:108/121-141`）；在 **`damage()` 覆写里推进阶段**；`act()` 里按 `phase` 分脚本；不同 phase 可动态增删属性/UI（DwarfKing phase2 加 `IMMOVABLE`、phase3 `BossHealthBar.bleed(true)`，`DwarfKing.java:147-150`）。
- DwarfKing 节奏：phase1 纯战 → 血线触发 phase2：套 `DKBarrior` 全量护盾并召唤两波小弟（`phase==2` 免疫源伤害）→ **盾破**进 phase3：承伤延迟（`Viscosity.DeferedDamage`）+ 血条进入红血区间 + 无限小怪。
- YogDzewa 节奏：`phase 0..5` + 六拳（`YogDzewa.java:87/112-114`）。阶段推进条件=累计扣血跨过 `HT-300*phase`（`YogDzewa.java:396-413`）；**拳在场时本体不可伤**（`damage()` 里 `if (phase==0 || findFist()!=null) return;`，:394）；每死一拳推进、最后一拳死→finale（yell + 换终章 BGM）；随 phase 视野收缩、`summonCooldown` 变短（高阶段召唤更频繁）。

**模式 B：环境 / 关卡状态驱动（场地就是"阶段"）**
- 代表：Tengu（`PrisonBossLevel.State` + 三系能力 Buff/Blob，阶段台词后能力切换 `Tengu.java:137-196`）；DM300（`CavesBossLevel` 桩塔 + `occupyCell` 触发 + `DM300.supercharged` 布尔 + Barrier，`DM300.java:124-155/335-343`）。
- 写法：**阶段状态放在关卡对象**（不是 mob 内部），mob 通过 `((PrisonBossLevel)Dungeon.level).state()` 查询自己该用哪套行为（`Tengu.java:137`）；关卡持有场地几何（pylonPositions/gate/mainArena……）并负责 seal/unseal 与 BGM 切换。

**模式 C：多实体合战（Boss + 爪牙 / 分身）**
- 代表：YogDzewa + `YogFist`（抽象基类 + Burning/Soiled/Rotting/Rusted/Bright/Dark 六个子类，`actors/mobs/YogFist.java:65`）+ YogEye/YogScorpio 小怪；DwarfKing 召唤 `DKMonk/DKWarlock`。
- 细节：爪牙类动态加 `Property.BOSS_MINION`（`DwarfKing.java:606+`、`YogDzewa.java:651+`），Yog 死亡时 `die()` 里连带清算所有在场爪牙（`YogDzewa.java:520-544`）。

> 自研建议：**开场先做模式 A（一个 phase 字段 + 两档血线），再按演出需要加场地机制（模式 B）或小怪（模式 C）**，三者可混用（DwarfKing 就是 A+C）。

### 4.6 Boss 通用基建（血条 / 台词 / 演出）

- **Boss 血条**：不是自动的——Boss 唤醒/进场时自己 `BossHealthBar.assignBoss(this)`（`Goo.java:263-264`、`DM300.java:154/356/476`、`DwarfKing.java:149/435`）；半血档位 `BossHealthBar.bleed(true)` 提示红血区间（同句各 Boss）；特殊怪（GnollGeomancer、CrystalSpire）也能当"半 Boss"用同一 API。
- **唤醒/台词**：`notice()`（首见台词 `yell`，Goo `Goo.java:312-317`）；死亡台词 `die()` 内 `yell`；BOSS 战内文本用 `GLog`/`Messages.get`。
- **挑战与计分**：`Statistics.qualifiedForBossChallengeBadge`、`Statistics.bossScores[i]`（无伤挑战扣分，`Goo.java:73-76`）、`STRONGER_BOSSES` 挑战数值放大（§4.2）。
- **存档**：`storeInBundle/restoreFromBundle` 持久化 `phase/supercharged/pumpedUp` 等战斗状态（DM300 `DM300.java:124-147`、DwarfKing phase）。
- **死亡统一收尾**：`super.die(cause)` → `level.unseal()` → 掉 `WornKey(depth)` 与专属战利品 → 台词/演出（Goo 见 §2.5 时序）。极少数 Boss 覆写 `die` 以先清算爪牙（Yog）。

---

## 5. 现役 Boss 档案速查表

| Boss | 深度/层 | 阶段/特性 | 精灵要点 | 免疫要点 | 入口机制 |
|---|---|---|---|---|---|
| Goo | 5 `SewerBossLevel` | 蓄力爆发（pump），半血强化 | GooSprite：pump/pumpAttack、半血喷黑雾 | Property.BOSS + ACIDIC + DEMONIC | 房间 paint 生成（沉睡），惊醒即 seal |
| Tengu | 10 `PrisonBossLevel` | 关卡 State 驱动的两阶段环境战（火/电/炸弹能力） | TenguSprite：飞镖远程 + 传送 | BOSS + 自加免 Roots/Blindness/Dread/Terror | 楼层持有 `tengu` 引用按阶段入场 |
| DM300 | 15 `CavesBossLevel` | 桩塔→蓄能→破防循环；supercharged + Barrier | DM300Sprite：charge/slam/zap、狂暴换帧行 | BOSS + INORGANIC（推测） | `occupyCell` 靠近桩塔 → seal 塌方 + 出场 |
| DwarfKing | 20 `CityBossLevel` | phase 1→2(护盾+召唤)→3(延迟伤+红血)，召唤 DKMonk/DKWarlock | KingSprite | BOSS；phase2 无敌窗口（覆写 isInvulnerable） | 楼层内生成 |
| YogDzewa | 25 `HallsBossLevel` | phase 0-5 + 六拳（YogFist×6）；拳在场本体无敌 | YogSprite + FistSprite（合贴图 `yog_fists.png`） | BOSS；`phase==0` 或拳存活时 `isInvulnerable` | seal 时于竞技场出场 |
| （安卡） | 26 `LastLevel` | 非战斗，放置 Amulet | — | — | — |
| 半 Boss 参考 | GreatCrab / GnollGeomancer / CrystalSpire | `Property.MINIBOSS` / 局部 `assignBoss` 战 | 常规 MobSprite | MINIBOSS | 事件/支线触发 |

---

## 附：关键文件索引

```
动画：sprites/CharSprite.java(六槽位+play/die守卫) / MobSprite.java / GooSprite.java / DM300Sprite.java / TenguSprite.java / FistSprite.java / Assets.java(Sprites 常量)
AI：actors/mobs/Mob.java(AiState 接口 1119、六状态 121-126、act 234、chooseEnemy 284、sprite 229)
     actors/mobs/Goo.java / Tengu.java / DM300.java / DwarfKing.java / YogDzewa.java / YogFist.java
免疫：actors/Char.java(Property 1582、isImmune 1537、resist 1517、isInvulnerable 1567)
楼层：Dungeon.java(newLevel 302)、levels/{Sewer,Prison,Caves,City,Halls}Level.java、XxxBossLevel.java、LastLevel.java、RegularLevel.java、Level.java(seal 638)
房间：levels/rooms/sewerboss/（Boss 房范式）、levels/rooms/standard/entrance|exit、levels/rooms/quest/、special/
UI：ui/BossHealthBar.java
```
