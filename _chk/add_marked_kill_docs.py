# -*- coding: utf-8 -*-
"""把 2026-09-26「带着标记倒下 + 矿洞虚空」的章节追加到 docs/features.md（幂等）。

为什么单开一个脚本：features.md 有 8550 行、纯 LF，章与章的惯例是 `---` + 一个空行 + `# <标题>`。
本脚本只做「按字节追加」，并在写之前断言文件确实以既有的尾分隔符结尾（比位置），
写完之后再断言新章标题在文件里**只出现一次**。改前副本留 `_chk/_bak_marked_kill/`。
"""
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
FEAT = os.path.join(ROOT, 'docs/features.md')
BAK = os.path.join(HERE, '_bak_marked_kill')
TITLE = '# 2026-09-26 「带着标记倒下」：两处「英雄击杀」判据改为「携带 buff 的怪死亡」'

CHAPTER = r'''# 2026-09-26 「带着标记倒下」：两处「英雄击杀」判据改为「携带 buff 的怪死亡」＋ 水晶矿洞层的虚空地形与「魔法乱流送回本层」

## 15.1 需求原文

> 现在需要你先修复两个相似的问题：拇指角色的盔甲技能"瞄准心脏"的"荣耀凯旋"在锁血时不触发；食指角色的击杀指令目标的相关指令完成要求以及天赋，在锁血时不触发；这些问题都是由于需要判定玩家击杀目标而导致的，由于它们的共性（需要击杀的目标带有对应buff），将触发条件改为"携带buff的怪物死亡"应该就可以正常避免，请你按照这个思路进行修复。随后请你改动水晶任务矿洞层的地形生成，使其中正常生成深渊（或者虚空）地形（替代地板而不是墙体）；如果玩家掉落其中，不会抵达别的层，而是传送回本层并受到坠落伤害判定，提示"一股魔法乱流把你送回了这一层某处"

## 15.2 根因：GEBURA 锁血把「死亡」推迟了

`Trials.interceptLethalDamage` 在「这一击致死」时不让它死，而是挂 `GeburaGrace`（濒死无敌 `EXP` 回合），
计时结束由 `GeburaGrace.act()` 调 `die(lethalSource)` 真正倒下。于是任何「这一击把它打死了」的判据都必然落空：

| 缺陷 | 旧判据 | 落空原因 |
|---|---|---|
| 拇指 盔甲技能「瞄准心脏」天赋「荣耀凯旋」 | `Char.attack` 里紧接 `enemy.damage(...)` 的 `!enemy.isAlive()` | 那一击进的是 `GeburaGrace`（`Mob.isAlive()` 被覆写为 `super.isAlive() \|\| geburaGrace`）⇒ `isAlive()` 仍为 true |
| 食指「击杀指令目标」的完成与天赋「神谕庇佑」 | `Mob.die` 英雄击杀分支里的 `buff(InstructionTarget.class) != null` | 真正倒下时 `cause` 是 `GeburaGrace` 记下的 `lethalSource`，不是 `Dungeon.hero`／武器；且标记 buff（`AimHeartMark` 只有 10 回合）很可能已过期 |

**用户给的方向**：两者的共性＝「要击杀的目标身上带着对应 buff」⇒ 判据改成「**携带该 buff 的怪死亡**」，
与「谁打死的」「打完立刻死没死」都解耦。

## 15.3 Part A：判据收口到 `Mob.die()` 开头

三条约束决定了收口点与写法：

1. **必须在 `super.die(cause)` 之前**。`Mob.die → super.die`（`Char.die`）`→ destroy()`（虚分派到 `Mob.destroy()`
   → `super.destroy()` → `Actor.remove(this)` → `Char.onRemove()` → 逐个 `buff.detach()`）
   ⇒ 之后 buff 全被摘掉，再判就读不到。（`PalermoFencing` 里那句同型判据本来就是这个错——`enemy.die()` 返回时
   buff 已经没了，是**死代码**。）
2. **锁血期间 live 的 buff 可能已过期** ⇒ 闸门在「致死那一击」抄一份携带状态到 `Mob` 上，
   死亡时 **live 优先、取不到才回落**（与 2026-09-24 的 `geburaLuckyProc` / `geburaWealthBonus` 同一范式）。
3. **与「英雄击杀」分支互不重叠**：新判据只跟 buff 走，旧分支只跟 `cause` 走。

```java
// Mob.die(Object cause) 的 alignment == Alignment.ENEMY 块开头
boolean aimMarked   = buff(AimHeartMark.class) != null || geburaAimMarked;
boolean instrTarget = buff(InstructionTarget.class) != null || geburaInstrTarget;
geburaAimMarked  = false;   // 一次性：抄存的携带状态随这次真实死亡消费掉
geburaInstrTarget = false;
if (aimMarked){ AimHeartMark.onKillByHero( Dungeon.hero ); }
if (instrTarget){ Talent.onOracleBlessing( Dungeon.hero ); Instruction.onTargetKilled( Dungeon.hero ); }
```

`Trials.interceptLethalDamage` 侧只加两行**纯读取**（不碰 `Random`）：

```java
mob.geburaAimMarked   = mob.buff( AimHeartMark.class ) != null;
mob.geburaInstrTarget = mob.buff( InstructionTarget.class ) != null;
```

被撤掉的旧判据：`Char.attack` 的 `aimMarkedBeforeHit` 局部变量与攻击后收尾块、
`PalermoFencing.tryExecute` 的 `onKillByHero`（死代码）、`Mob.die` 英雄击杀分支里的 `InstructionTarget` 块。
全仓 `AimHeartMark.onKillByHero` / `Instruction.onTargetKilled` 的调用点**各只剩 1 处，都在 `Mob.die`**。

⚠️ 两个留存字段**刻意不进存档**（`storeInBundle` / `restoreFromBundle` 都不碰）：推迟用掉之后一定是真死，
死者不会被写进存档。

## 15.4 Part B：水晶矿洞层的虚空地形 + 「魔法乱流把你送回本层」

**地形**（`MiningLevel.carveVoid`，从三个矿洞房间的 `paint()` 里调）：

- **替代地板、不替代墙体**：只把 `EMPTY` / `EMPTY_DECO` 换成 `Terrain.CHASM`；`WALL` / `MINE_CRYSTAL` 一格不动。
- **不封死任何人**：只在「深内区」（房间内圈再各缩 2 格）里挖 ⇒ 外面永远留一整圈地板；
  圆盘与房间自己放的关键单位（水晶守卫／水晶尖塔）保持 `radius+1` 以上切比雪夫距离
  ⇒ 它的落脚点与八邻格必定是地板。半径 1~3，最外圈 50% 保留（边缘不规则，照 `FissureRoom`）；
  `heaps` 上还有东西的格不挖。
- ⚠️ **只写 `map[]`（`Painter.set`）** 是安全的，因为 `Level.create()` 的顺序是
  `while(!build())` → **`buildFlagMaps()`** → `cleanWalls()`：`paint()`（连带 `decorate()`）全在 `build()` 里，
  画完之后 flags 会统一从 `map[]` 重算。**反过来，`create()` 之后再改地形就必须走 `Level.set(...)` /
  `updateCellFlags(...)`**，否则得到「看着是深渊、却踩得上去」的幽灵格（`pit[]` / `passable[]` 没跟上）。
- 画师 `MiningLevelPainter.decorate`：先给「房间里的虚空」拍快照，`super.decorate()`（`CavesPainter`
  会在**房间之间（原为墙体）**填深渊）之后只还原**不在快照里**的那批 ⇒ 原版「矿洞层绝不允许深渊」的口径
  只在房间之间保留。

**落坑不换层**：

- `Level` 新增 `public boolean handlesChasmFall(){ return false; }`（实例方法，按当前层多态分派）；
  `MiningLevel` 覆写为 `Blacksmith.Quest.Type() == Blacksmith.Quest.CRYSTAL`（只有水晶任务才生效）。
- `Chasm.heroFall` 在最前面加一条早返回分支：本层自理时调 `heroRiftReturn()` 后 `return`。
  ⚠️ **必须早于 `Level.beforeTransition()`** —— 那里面会给「离开这一层」的指令记账（`onFloorExit`）、
  还会把英雄的零碎回合凑整，对「其实没离开」的情况都不该发生。
- `heroRiftReturn()`：落脚点用 `Level.randomRespawnCell(hero)`（与传送卷轴同源，天然避开深渊／不可站立格／
  已有角色；额外躲开密室）→ `ScrollOfTeleportation.appear(hero, cell)`（精灵跟随／淡入／粒子／音效全齐）
  → `Dungeon.observe()` + `GameScene.updateFog()` → `GLog.i( Messages.get(Chasm.class, "rift_return") )`
  → `heroLand()`（**原版落坑的同一套落地结算**：残废 + 流血 + 伤害；喝过羽落秘药照常免伤）。
  取不到落脚点时保持原状、不做无谓位移。

文本（三语）：`levels.features.chasm.rift_return` ＝ `一股魔法乱流把你送回了这一层某处` /
`一股魔法亂流把你送回了這一層某處` / `A blast of magical turbulence warps you back to somewhere on this floor.`

## 15.5 落点

| 文件 | 改动 |
|---|---|
| `actors/mobs/Mob.java` | 新增 `public boolean geburaAimMarked` / `geburaInstrTarget`（**不进存档**）；`die()` 开头（`super.die` 之前）的「带着标记倒下」判据块；英雄击杀分支里的旧 `InstructionTarget` 块删除 |
| `Trials.java` | `interceptLethalDamage` 里两行抄写（纯读取，**无 `Random`**） |
| `actors/Char.java` | 删掉 `aimMarkedBeforeHit` 局部变量与攻击后的 `onKillByHero` 收尾块 |
| `items/weapon/melee/PalermoFencing.java` | 删掉 `tryExecute` 里那句死代码与随之无用的 import |
| `levels/Level.java` | 新增 `handlesChasmFall()`（默认 false） |
| `levels/features/Chasm.java` | `heroFall` 的早返回分支 + `heroRiftReturn()`（新）+ `ScrollOfTeleportation` import |
| `levels/MiningLevel.java` | 新增 `carveVoid(Level, Room, int)` + 覆写 `handlesChasmFall()` |
| `levels/rooms/quest/MineSmallRoom.java` | CRYSTAL 分支末尾 `carveVoid( level, this, -1 )` |
| `levels/rooms/quest/MineLargeRoom.java` | `carveVoid( level, this, level.pointToCell(p) )`（保护水晶守卫） |
| `levels/rooms/quest/MineGiantRoom.java` | `carveVoid( level, this, level.pointToCell(p) )`（保护水晶尖塔） |
| `levels/painters/MiningLevelPainter.java` | `decorate` 先快照房间虚空、`super.decorate` 后只还原房间之间那批 |
| `assets/messages/levels/levels{,_zh,_zh-hant}.properties` | `levels.features.chasm.rift_return` 三语 |

## 15.6 核验

| 项 | 结果 |
|---|---|
| `_chk/verify_marked_kill_routes.py`（新） | **78 条**断言（A~I 九组：字段／不进存档／消费、闸门抄写的位置与纯读取、旧判据撤除与全仓调用点唯一、`handlesChasmFall` 默认与覆写、`heroFall` 早返回与 `heroRiftReturn`、`carveVoid` 地形规则、房间调用点与画师、三语文本、行为层）+ **78 条反例自测（0 条抓不到错）** |
| `_chk/MarkedKillProbe.java`（新，行为探针） | **23 条断言全过**（0 条环境跳过）：① 带 `AimHeartMark` 的 `Rat` 以 **`Chasm.class`（非英雄击杀）** 为 `cause` 死亡 ⇒ 荣耀凯旋真的回血，且 `die()` 走完后 buff 已被 `onRemove` 摘掉（证明钩子跑在摘 buff **之前**）② 带 `InstructionTarget` ⇒ 神谕庇佑给盾，对照组不给 ③ 不挂 live、只置 `geburaAimMarked` ⇒ 回落分支可达且用掉即清 ④ 负向对照什么都不发生 ⑤ 真调闸门 ⇒ 两字段被抄下、`geburaGrace` 为真；关掉考验则返回 false 且字段保持初值 ⑥ 同 seed 200 个随机数逐位相同（穿插 20 次闸门调用） |
| 单文件 `javac`（11 文件，`-Xlint:all`） | **EXIT=0**（0 错误；43 条告警全是既有的 `Char.java` rawtypes/lossy 等，无一落在改动行） |
| `check_utf8_all.py` / `check_unused_imports.py` | OK（1472 文件）/ ALL PASS |
| 行尾 | `Mob.java` / `Trials.java` / `PalermoFencing.java` 保持 LF；`Char/Level/Chasm/MiningLevel/Mine*Room/MiningLevelPainter` 保持 CRLF |

⚠️ 探针环境三处必踩（已写进探针注释）：`new MiningLevel()` 会经 `RegularLevel.<clinit>` 读 `Game.version`
（脱游戏为 null ⇒ `ExceptionInInitializerError`，先赋个版本号）；`Level.mobs` 只在 `Level.create()` 里 new
（手工补 `new HashSet<Mob>()`）；脱游戏没有 `HeroSprite` ⇒ 把 `hero.lvl` 抬到 `> maxLvl` 让 `exp == 0`，
跳过 `destroy()` 里那句 `hero.sprite.showStatusWithIcon(...)`。

## 15.7 待人工验证

1. 拇指点满「荣耀凯旋」+ 学「瞄准心脏」：对带标记的怪，**在 GEBURA 考验下**把它打死 ⇒ 怪进濒死无敌，
   计时结束真倒下那一刻**应当回血**（修复前不回）。不带标记的怪不该回血。
2. 食指「击杀指令目标」（`TASK_KILL_TARGET`）任务：在 GEBURA 考验下击杀指令目标 ⇒
   指令**应当判完成**、神谕庇佑给盾（修复前不完成）。
3. 关掉 GEBURA：与旧版逐字一致（两个留存字段恒为初值）。
4. 水晶任务矿洞层：房间里应当看到**地板被挖成虚空**（墙体与矿脉完好），外圈仍有一整圈地板可绕行；
   水晶守卫／尖塔所在格及其八邻格是地板。
5. 踩进虚空：**不换层**、被传送回本层某处、扣坠落伤害并显示「一股魔法乱流把你送回了这一层某处」；
   喝过羽落秘药时不掉血。矮人任务（`GNOLL`）层的矿洞**不应**出现虚空，落坑行为与原版一致。

---

'''


def main():
    raw = open(FEAT, encoding='utf-8').read()

    if TITLE in raw:
        print('已存在该章（幂等），未改动。')
        return 0

    # 比位置：必须正好以「既有的尾分隔符」结束
    tail = '\n4. 环指大师职业 + 幸运武器 + GEBURA：素材的「幸运再判一次」应当恢复。\n\n---\n\n'
    if not raw.endswith(tail):
        print('尾锚点不匹配 ⇒ 不落盘。实际结尾：%r' % raw[-60:])
        return 2
    if '\r' in raw:
        print('features.md 出现 CR 行尾（应恒为纯 LF）⇒ 不落盘。')
        return 2

    os.makedirs(BAK, exist_ok=True)
    shutil.copy2(FEAT, os.path.join(BAK, 'features.md.bak'))

    out = raw + CHAPTER
    # 断言：新章标题只出现一次
    assert out.count(TITLE) == 1, '新章标题出现次数 != 1'
    assert '\r' not in CHAPTER, '新章混入了 CR'

    with open(FEAT, 'w', encoding='utf-8', newline='') as f:
        f.write(out)

    n = len(out.split('\n'))
    print('已追加 §15：%d 行（原 %d 行 → 现 %d 行）。改前副本 %s'
          % (len(CHAPTER.split('\n')), len(raw.split('\n')), n, BAK))
    return 0


if __name__ == '__main__':
    sys.exit(main())
