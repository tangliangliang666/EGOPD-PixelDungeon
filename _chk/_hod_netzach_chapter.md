
---

# 2026-09-26（续）考验 NETZACH（胜利）＋ HOD（荣耀）：命中 / 闪避按区递增 ＋ 距离淡出 / 150 回合精英化

## 16.1 需求原文

> 希望这两个考验的命中 / 闪避属性加成按照区域提升，例如一区 +10%，五区则 +50%（描述文本同步修改）

两条规则的完整口径（含上面这条「按区递增」的追加要求）：

| 考验 | 序号 | 规则 |
|---|---|---|
| **NETZACH**（胜利） | 6 | 所有**敌方单位**的**闪避**按区递增（一区 +10% …… 五区 +50%）；距离英雄**超过 2 格**半透明、**超过 4 格**完全不可见（纯视觉） |
| **HOD**（荣耀） | 7 | 所有**敌方单位**的**命中**按区递增（同口径）；某个敌方单位**存活超过 150 回合**后获得一次**随机精英效果**（不可叠加） |

## 16.2 定位：两条规则各自落在哪个「收口类别」

§6 / §7 / §8 已经把考验规则归纳成三类落地方式（**数值出口收口** / **事件型规则** / **挡路型规则**）。
本轮两条都属**数值出口收口**（改属性的唯一取数点），HOD 另挂一条**常驻 buff 计时**（与 CHESED 的
`ChesedMend` 同型）。所以两次实现都没有新增「入口」，只改了既有的计算点与一个渲染点。

## 16.3 共用口径：区域（1~5）与按区递增倍率

「区域」＝地牢的**五章**结构，口径直接取 `Dungeon.scalingDepth()`（不是 `depth`：升天时两者不同）：

```java
public static final float REGION_BONUS_STEP = 0.10f;
public static final int MAX_REGION          = 5;

public static int currentRegion(){                      // 深度 1~5 ⇒ 1 区 …… 21~25 ⇒ 5 区
    return Math.min( MAX_REGION, Math.max( 1, (Dungeon.scalingDepth() - 1) / 5 + 1 ) );
}
public static float regionBonusMultiplier(){            // 一区 1.10 …… 五区 1.50
    return 1f + REGION_BONUS_STEP * currentRegion();
}
```

- **唯一口径**：HOD 的命中与 NETZACH 的闪避都读这一个方法，改数值只动 `REGION_BONUS_STEP` 一处。
- **升天封顶**：`scalingDepth() == 26`（升天层）被 `MAX_REGION` 夹到五区，不会算出 1.60。
- **不加随机数**：纯算术，同种子逐位一致。

## 16.4 NETZACH（胜利）

### 收口点一（数值）：`Trials.finalEvasion` / `finalAccuracy`

⚠️ **全作有三处在算「命中 vs 闪避」，各自复刻了一整套乘区**（它们各自的 copy-pasta 注释也承认了）：

| 站点 | 用途 |
|---|---|
| `Char.hit` | 真正的命中判定 |
| `Talent.dodgeAsDamageReduction` | 把「本可闪避的概率」折算成减伤 |
| `Stone.proc`（磐岩附魔） | 同上 |

只改 `Char.hit` 会让**换算出的概率与真实命中率漂移**。因此把「读属性 + 乘考验倍率」收进两个方法，
三处一律改调它们（这就是为什么 HOD 的命中与 NETZACH 的闪避共用同一对收口）：

```java
public static float finalAccuracy( Char attacker, Char defender ){
    if (attacker == null) return 0f;
    return attacker.attackSkill( defender ) * accuracyFactor( attacker );   // HOD
}
public static float finalEvasion( Char defender, Char attacker ){
    if (defender == null) return 0f;
    return defender.defenseSkill( attacker ) * evasionFactor( defender );   // NETZACH
}
```

两个倍率函数都带同一组门控：**非 `Alignment.ENEMY` 返回 1**、**本考验未开返回 1**（与 HOKMA / CHESED 同判据）。
⇒ 收口点对英雄、对未开考验的局**逐字节等价于原版**。

### 收口点二（视觉）：`CharSprite.draw()`

距离用 `Dungeon.level.distance`（切比雪夫，与原版其余「格数」口径一致）：`≤2` 全不透明 / `3~4` 半透明（`0.5`）/ `>4` 全透明。

做法是**只在绘制这一帧**把 `am` / `aa` 临时乘上系数、`super.draw()` 之后立刻写回：

```java
float fade = Trials.enemyFade( ch );
float amBak = am, aaBak = aa;
if (fade < 1f) { am *= fade; aa *= fade; }
... // renderShadow + super.draw()
if (fade < 1f) { am = amBak; aa = aaBak; }
```

**为什么不写在 `update()` / `resetColor()`**：真正的隐形走 `AlphaTweener`（它会**持续写** alpha）、
受击闪光走 `hardlight`（**只碰颜色不碰 alpha**）⇒ 只有「只影响这一帧、不落任何持久状态」才不会与这两者打架。
`fade == 1f`（未开考验 / 不是敌方 / 贴身 / 无英雄）时两个 `if` 都不执行，零影响。

**「看不见」是给玩家的难度，不是给系统的隐身**：不改命中判定、不写 `invisible`、不碰 AI。

### 血条：`CharHealthIndicator`

`visible` 追加 `&& Trials.enemyFade( target ) > 0f` —— 否则完全淡出的单位会被一条**浮空血条**暴露位置。

## 16.5 HOD（荣耀）

### 收口点一（数值）

与 NETZACH 共用 `finalAccuracy` / `finalEvasion`（见 16.4），HOD 只负责命中侧倍率 `accuracyFactor`。

### 收口点二（计时）：常驻 buff `HodGlory`

需求是「**任意敌方单位存活超过 150 回合**」⇒ 计时器交给 `actors/buffs/HodGlory`：

- **常驻 buff**（不限期），由 `Trials.bindMobPassives` 在 `Mob.onAdd()` 里幂等重挂（与 `ChesedMend` 同型）。
  用 buff 而不是写进 `Mob.act()`，是因为**一部分单位的 `act()` 覆写不调 `super.act()`**
  （`CrystalSpire` / `Masterpiece` / `MobSpawner` / `Sheep` 等），而 buff 有自己的回合，谁都漏不掉；
  顺带它随存档保存（`Char.storeInBundle` 存的就是 buffs 列表），读档后接着原来的计时。
- **计时口径严格照「存活」**：只有在「是敌方阵营 ＋ 活着 ＋ 本考验仍开着」时才 `++turns`；
  一旦不再适用就**清零**（不再算存活），被腐蚀 / 被招募 / 被魅惑成友方后立刻停表，变回敌方再从 0 重数。
  ⇒ 每次现判 `alignment`，而不是挂 buff 时判一次。
- **数满自摘**：`++turns > HOD_ELITE_TURNS` 时调 `Trials.grantHodElite(mob)` 然后 `detach()`。
  这个 buff 的使命就是「数到 N 发一次精英」，发完即止、绝不再重掷 —— 「不可叠加」的一半保障在这里。
- ⚠️ **`type` 只能是 POSITIVE**（与 `ChesedMend` 同一条红线）：`Mob.Sleeping` 把 `type == NEGATIVE`
  的 buff 当成「被打醒」的信号 ⇒ 标成 NEGATIVE 会让**全图怪物一入场就醒来并进入警戒**。

### 收口点三（晋升）：`Trials.grantHodElite`

```java
public static boolean grantHodElite( Mob mob ){
    if (mob == null || !Dungeon.isTrialled( HOD )) return false;
    if (mob.alignment != Char.Alignment.ENEMY) return false;
    if (!mob.buffs( ChampionEnemy.class ).isEmpty()) return false;      // 不可叠加
    ChampionEnemy champ = Buff.affect( mob, ChampionEnemy.randomChampionClass() );
    if (champ != null && mob.sprite != null)
        mob.sprite.showStatus( CharSprite.POSITIVE, Messages.titleCase( champ.name() ) );
    return true;
}
```

- **「随机精英」复用原版冠军系统**的六种效果：为此把 `ChampionEnemy.rollForChampion` 里的
  `Random.Int(6)` 分支抽成 `public static Class<? extends ChampionEnemy> randomChampionClass()`，
  两个调用点共用同一张映射。抽出是**纯重构**：`rollForChampion` 的随机流位置一字未变。
- ⚠️ **必须用 `buffs(ChampionEnemy.class)` 而不是 `buff(...)`**：后者的实现是 `b.getClass() == c`
  的**精确类匹配**，对抽象基类永远返回 `null` ⇒ 会写出「明明有精英却判成没有、再发一个」的叠加 bug。
- **「不可叠加」＝ 身上已有任一 `ChampionEnemy`**（挑战 `CHAMPION_ENEMIES` 刷出来的、或本考验早先发过的）。
- **反馈**：晋升时用精英自己的名字弹状态字（原版文本键已存在，如 `actors.buffs.championenemy$blazing.name`）。
- ⚠️ **这里会消耗一次 `Random.Int(6)`**（运行时事件）。与 BINAH「不动随机流」的口径**并不冲突**：
  那条红线针对**关卡生成那一刻**（保证同种子布局逐字节一致），而本效果发生在生成之后，
  与怪物掉落 / 攻击掷点同属运行时随机 —— 不存在「开了本考验、布局就变」的问题。
- **挂载顺序**：`bindMobPassives` 里 HOD 那条**必须排在 CHESED 的 `mob.EXP <= 0` 提前返回之前**
  —— 需求是「任意敌方单位」，与「给不给经验」无关（`EXP == 0` 的怪也要数表）。

## 16.6 文本（zh ＋ en）

`trials.netzach_desc` / `trials.hod_desc` 从占位文案换成正式描述，两侧同步：

- zh：`所有_敌方单位_的闪避属性随所在_区域_提升：_一区 +10%_，每深入一区再 +10%，至_五区 +50%_。\n\n_距离英雄超过 2 格_的敌方单位会变得半透明，_超过 4 格_时_完全不可见_。`
- 换行是**字面 `\n`**（两个字符），分段用 `\n\n` —— 与全仓 `_zh.properties` 一致。

（`misc.properties` 只有 en + zh 两份，无 `_zh-hant`。）

## 16.7 落点

| 文件 | 改动 |
|---|---|
| `Trials.java` | 新增区域口径（`REGION_BONUS_STEP` / `MAX_REGION` / `currentRegion` / `regionBonusMultiplier`）；NETZACH 三常量 ＋ `evasionFactor` / `enemyFade`；HOD 常量 ＋ `accuracyFactor` / `finalAccuracy` / `finalEvasion` / `grantHodElite`；`bindMobPassives` 泛化（HOD 计时器优先挂载，不再被 CHESED 的 EXP 早返回挡住）；类注释进度更新 ＋ `HodGlory` import |
| `actors/buffs/HodGlory.java` | **新建**：POSITIVE 常驻 buff，`turns` 进存档，数满晋升并自摘 |
| `actors/buffs/ChampionEnemy.java` | 抽出 `public static randomChampionClass()`（`rollForChampion` 与 `grantHodElite` 共用） |
| `actors/Char.java` | `hit()` 改调 `Trials.finalAccuracy` / `finalEvasion` |
| `actors/hero/Talent.java` | `dodgeAsDamageReduction` 改调同两个收口 |
| `items/armor/glyphs/Stone.java` | `proc()` 改调同两个收口 ＋ `Trials` import |
| `sprites/CharSprite.java` | `draw()` 里按 `enemyFade` 临时乘 / 还原 `am`·`aa` ＋ `Trials` import |
| `ui/CharHealthIndicator.java` | `visible` 追加 `&& Trials.enemyFade( target ) > 0f` ＋ `Trials` import |
| `assets/messages/misc/misc{,_zh}.properties` | `trials.netzach_desc` / `trials.hod_desc` 换成正式描述 |

## 16.8 核验

| 项 | 结果 |
|---|---|
| `_chk/verify_hod_netzach.py`（新） | **140 条**断言 ＋ **17 条反例自测**，五层：① 源码结构（常量、`callers_of` 调用点唯一、`seq_ok` 顺序、`body_has_random` 无随机）② `javap -p` 签名 ③ `javap -c` 字节码（无 `Random`；`grantHodElite → detach` 顺序；`Char.hit` 的取数发生在 `INFINITE_EVASION` 之前；`draw` 里取系数早于 `super.draw`、`am`/`aa` 还原晚于 `super.draw`）④ 文本层（zh ＋ en 键在、非占位、CRLF 完整、字面 `\n`）⑤ 行为层（探针输出全绿、七段都跑到） |
| `_chk/HodNetzachProbe.java`（新，行为探针） | **52 条断言全过**（0 条环境跳过）：① 区域→倍率映射（含升天封顶）② 命中 / 闪避倍率门控 ③ `finalAccuracy` / `finalEvasion` 取数 ④ `enemyFade` 距离阈值（`≤2→1` / `3~4→0.5` / `>4→0`）⑤ `grantHodElite` 不可叠加 ⑥ `HodGlory` 计时边界（第 150 回合不发 / 第 151 回合发 ＋ 自摘；不适用时清零）⑦ `randomChampionClass` 恰好覆盖 6 种 |
| 单文件 `javac`（`-Xlint:all`，8 文件） | **EXIT=0**（0 错误；告警全是既有的本仓 rawtypes / lossy 等，无一落在改动行） |
| 回归 | 在范围内的核验脚本全绿：`verify_chesed_gebura` 156/0、`verify_hokma_delay` 36/0、`verify_binah_gen` 116/0、`verify_gebura_drop` 37/0、`verify_marked_kill_routes` 87/0、`verify_holy_card` 58/0、`verify_fun_challenges` 81/0、`verify_trials_tree_ui` 112、`verify_tree_trials_icons`、`verify_dwarfking_chesed` 27/0、`verify_boss_phase_scales` 49/0 |
| 行尾 | `Trials.java` / `HodGlory.java` 保持 LF；`ChampionEnemy/Char/Talent/Stone/CharSprite/CharHealthIndicator` 保持 CRLF；`misc{,_zh}.properties` 保持 CRLF 且无裸 LF |

⚠️ 探针环境三处必踩（已写进 `HodNetzachProbe` 注释）：`Game.version` 必须先赋值（脱游戏为 `null` ⇒ NPE）；
`Mob` 子类必须 `HP = HT > 0` **且** `enemySeen = true`（否则 `Mob.defenseSkill` 经 `surprisedBy` 返回 0、
`HodGlory.act` 因 `isAlive()` 为假而跳过）；手工 new 的 `Level` 要自带 `width` / `height` / `length`。

## 16.9 待人工验证

1. 开 NETZACH：一区敌方闪避 +10% ⇒ 英雄打空的比例略升；五区 +50% ⇒ 明显更常打空。
   描述文本（考验界面）应为正式文案而非占位。
2. 开 NETZACH：把怪拉到 **2 格内**完全不透明、**3~4 格**半透明、**>4 格**看不见（血条一并消失）；
   走到远处再靠近，透明度应随距离实时恢复。
3. 开 HOD：敌方命中按区递增（一区 +10% ⇒ 五区 +50%）。
4. 开 HOD：让一只怪**存活超过 150 回合**（如贴着它绕圈不打）⇒ 它应弹出一个**精英名字**的状态字并获得
   对应效果；再等下去**不会**再发第二个（已有精英的怪永不叠加）。
5. 同时开 NETZACH ＋ HOD：两个倍率叠加（闪避与命中各自独立生效）。
6. 关掉两条考验：命中 / 闪避 / 绘制 / 血条与旧版逐字一致。
