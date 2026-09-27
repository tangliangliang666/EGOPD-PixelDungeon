# 漆黑噤默系列武器设计定案

> 本文档记录《破碎的像素地牢》改版中"漆黑噤默"系列武器的设计决策，防止上下文丢失。
> 最后更新：本次设计评审（2026-08，DSH 会话）

## 背景

漆黑噤默（`DarkSilence`）是可变形武器：一双黑色手套，内藏九种武器，可自由切换（见 `MorphWeapon`）。
九种形态此前均为基于原版武器数据的占位实现，本次评审确定各形态的最终差异化方案。

- 世界观：月亮计划《废墟图书馆》同人设定。**命名与描述文本保持现状，不做修改。**
- 切换机制：**保持免费切换**——无消耗、无冷却，切换时保留强化等级、附魔、诅咒、鉴定状态（`MorphWeapon.morphInto` 现有实现）。

## 通用规则

1. 所有形态**面板/描述仍显示为 5 阶**（`tier` 字段保持 5）。
2. 每个形态**套用指定原版武器的完整数值与特色机制**，包括：
   - 伤害曲线（`min` / `max` 公式）
   - 力量需求（`STRReq`，按参考武器的阶位计算）
   - 特色机制（命中加成、攻击速度、射程、格挡、突袭强化等）
3. 决斗者技能（`AC_ABILITY`）对本系列禁用（`MorphWeapon` 已移除）。
4. `statsInfo()` / `abilityInfo()` 返回空，由各形态的 `desc()` 提供说明。

## 各形态定案

| 形态 | 参考模板 | 面板阶位 | 伤害（基础 ~ 每级） | 力量需求 | 特色机制 |
|---|---|---|---|---|---|
| 漆黑噤默（本体） | 镶钉手套 Gloves | 5（数值=1 阶） | 1~5，+1/级 | 10 | 2 倍速（DLY 0.5） |
| 杜兰达尔 Durandal | 长剑 Longsword | 5（数值=4 阶） | 4~25，+5/级 | 16 | 标准均衡，无负面 |
| 榉树工坊 ElmWorkshop | 硬头锤 Mace | 5（数值=3 阶） | 3~16，+4/级 | 14 | 高命中（ACC 1.28） |
| 琅琊工坊 LangyaWorkshop | 暗杀之刃 AssassinsBlade | 5（数值=4 阶） | 4~20，+5/级 | 16 | 突袭强化：伏击伤害 50%~max |
| 老男孩工坊 OldBoyWorkshop | 战锤 WarHammer | 5 | 5~24，+6/级 | 18 | 高命中（ACC 1.20） |
| 阿拉斯工坊 ArasWorkshop | 关刀 Glaive | 5 | 5~40，+6/级 | 18 | 射程 2、慢速（DLY 1.5） |
| 墨工坊 InkWorkshop | 武士刀 Katana | 5（数值=4 阶） | 4~20，+5/级 | 16 | 格挡 0~3 |
| 逻辑工作室 LogicStudio | 远程（自定义） | 5 | 1~6，+2/级 | 18 | 远程射击（射程 9；直接攻击即射击） |
| 卡莉斯塔工作室 CalistaStudio | 魔岩拳套 Gauntlet | 5 | 5~15，+3/级 | 18 | 2 倍速（DLY 0.5） |
| 轮盘重工 RouletteHeavy | 巨斧 Greataxe | 5 | 5~45，+6/级 | 20 | 高面板、高力量需求 |

## 实现方式

面板显示 5 阶、数值按参考武器，因此：

- `tier` 字段**保持 5**（影响面板阶位显示、价格、双升级天赋判定等）。
- 需要降阶数值的形态（本体、杜兰达尔、榉树、琅琊、墨）覆盖：
  - `min(int lvl)` = 参考阶位 + lvl
  - `max(int lvl)` = 参考武器的公式
  - `STRReq(int lvl)` = `STRReq(参考阶位, lvl)`（保留 `masteryPotionBonus` 减 2 逻辑）
- 阶位本身就是 5 的形态（老男孩、阿拉斯、卡莉斯塔、轮盘）仅需核对 `max` 公式与特色机制。

## 待办 / 未决事项

- **逻辑工作室（远程形态）**：射击动作与弹药投影物仍待专门评审；**弹丸贴图/朝向/水花颜色/飞行速度/直接攻击改射击已于 2026-09-10 修正**（见下）；
  **成长来源已于 2026-09-11 核查：不含「随英雄等级自动升级」，纯按武器强化等级**（见下）。
- 切换窗口（`WndMorph`）表现已于 2026-09-10 增强：已使用形态标绿点 + 头顶切换虚影 + 自动切换开关（见下）。

### 逻辑工作室弹丸（2026-09-10 修正）

- **贴图**：原占位实现借用灵能矢贴图 `ItemSpriteSheet.SPIRIT_ARROW`；改为新常量
  `ItemSpriteSheet.LOGIC_BULLET = xy(10, 2)`（2 行 10 列，10×10，实际绘制包围盒恰为 10×10 且对齐格左上角）。
  该格占用 `UNCOLLECTIBLE` 组的空号 **+7**（vanilla 声明 14 格但 +2/+7/+11~+13 未用），已核验全表无冲突。
- **朝向**：`MissileSprite` 对弹丸原先既按方向计算初始 `angle`、又以默认 720°/s 自旋，显示为错误的倾斜。
  现注册 `ANGULAR_SPEEDS.put(LogicStudio.Shot.class, 0)`，并在 `setup()` 中加特判块（与 `GnollGeomancer.Boulder` 同款）
  强制 `angle = 0; flipHorizontal = false;` —— 飞行途中恒不旋转、不翻转。
- **注**：`Shot` 是 `LogicStudio` 的**非静态内部类**，`LogicStudio.Shot.class` 作为类字面量可安全用于静态注册表。
- **落空水花**：原 `Splash.at(cell, 0xCC99FFFF, 1)` 的色值实为**8 位**，而 `Visual.color(int)` 只取低 24 位
  （按 `0xRRGGBB` 解析、忽略高字节），实际渲染出来是浅青而非预期紫色。现改为常量
  `MISS_SPLASH_COLOR = 0xDDDDDD`（**灰白**）。⚠️ 写 Splash 色值一律用 6 位 `0xRRGGBB`，多写的高位字节会被静默丢弃。
- **飞行速度**：`MissileSprite.setup()` 速度分支新增 `item instanceof LogicStudio.Shot → speed *= 2f`（默认 240 → 480）。

### 逻辑工作室：直接攻击即射击（2026-09-10 新增）

- **问题**：远程形态的快捷栏点击被"切换"占用（`LogicStudio.defaultAction() = MorphWeapon.AC_MORPH`），
  想射击必须打开背包选"射击"（`AC_SHOOT`），操作繁琐。而点按敌人走的是通用**近战结算**
  （`Hero.actAttack → sprite.attack → onAttackComplete → Char.attack`）——虽有 `RCH = 9` 能远程够到，
  但只做近战判定、不发射弹丸，观感/期望都不符。
- **改动**：`Hero.actAttack` 在通过 `isCharmedBy / isAlive / canAttack / invisible` 校验后，
  若 `belongings.attackingWeapon() instanceof LogicStudio`，则改为
  `((LogicStudio) wep).shootAt(this, attackTarget.pos)` 并**清空 `attackTarget` / `curAction`、直接 return**，
  不再执行近战分支。
  - `LogicStudio.shootAt(hero, target)` = `new Shot().cast(hero, target)`：英雄 busy、弹丸飞行、
    命中/落空结算、回合消耗（`spendAndNext(castDelay)`）全部由 `Item.cast` 内部完成——
    与 `AC_SHOOT` 完全同一条链路，`AC_SHOOT` 动作保留（仍可对空地/指定格射击）。
  - **必须清空 `curAction`**：`cast()` 的收尾是 `spendAndNext`（会 `next()`），若不清空，英雄下次 `act()`
    会再读到同一个 `HeroAction.Attack` → 无限连射。
  - 该接管位于**所有**普通攻击路径的汇聚点，因此点按敌人、键盘/自动寻路接近后攻击、其它 buff 发起的
    `HeroAction.Attack` 均生效；超出 `RCH=9` 的目标仍按原逻辑先走近再射。
- **贴脸命中修正**：`Shot.adjacentAccFactor` 覆写为「相邻返回 1x、非相邻沿用 1.5x」。
  投掷物默认对相邻目标有 0.5x 贴脸命中惩罚；若不修正，**原本稳中的近战平砍会突变成 50% 命中**，手感如 bug。
- 文案：`LogicStudio.desc()` 末尾追加"直接攻击即为射击。"。

### 逻辑工作室：成长来源核查（2026-09-11）

- **结论：无「随英雄等级自动升级」残留**。远程形态只覆写 `min(int lvl)` / `max(int lvl)`（`1+lvl` / `6+2*lvl`），
  **没有** `level()` / `buffedLvl()` 覆写，也**没有** `Dungeon.hero.lvl` 引用——成长完全来自武器自身强化等级。
- 链路核验：`Shot.damageRoll` → `LogicStudio.damageRoll` → `KindOfWeapon.damageRoll` → `min()/max()` = `min(buffedLvl())/max(buffedLvl())`
  → `Weapon.level()` → `Item.level()`（即 `+N` 强化等级，含 curseInfusion 加成）。全链路不含英雄等级。
- 作为对照，**全仓唯一**带英雄等级的武器是 `SpiritBow`：`level()` 返回 `Dungeon.hero.lvl/5`（`SpiritBow.java:272`），
  并配套 `buffedLvl()` 覆写 + `isUpgradable() = false`。这三件套是"灵能弓式成长"的标志，本形态**一个都没有**。
- **顺带清理的过时注释**（旧实现曾 `extends SpiritBow`，解耦后残留）：
  - `MorphWeapon` 类 javadoc 仍写"LogicStudio extends SpiritBow" → 已改为"直接继承 Weapon，非 SpiritBow 子类、不随英雄等级"。
  - `WndDebug` 调试台注释同样写"extends SpiritBow" → 已改为"extends Weapon directly"。
  - `LogicStudio` 的 `min/max` 处新增**护栏注释**：明确禁止在此覆写 `level()/buffedLvl()`，避免该 bug 被再次照抄进来。
- 核查范围不限于本形态：十个形态（`MorphWeapon` 全家族 + `LogicStudio`）逐一 grep，均无 `level()`/`buffedLvl()` 覆写与 `hero.lvl` 引用。
  （`MeleeWeapon.buffedLvl()` 里的双升级天赋、`chargeCap()` 里的武技充能上限虽引用 `hero.lvl`，但属原版近战机制，
  且 `LogicStudio` 直接继承 `Weapon`、不继承 `MeleeWeapon`，两者无交集。）

## 漆黑连击 buff 与 Furioso 终结技（2026-08 追加，2026-09-10 修订）

### MorphCombo buff（`actors/buffs/MorphCombo.java`）

- 显示名：**漆黑噤默**；图标套用武僧连击图标（`BuffIndicator.COMBO`），颜色随层数变化（绿→黄绿→黄→橙→红），角标显示当前层数。
- 机制：用系列中**不同**的武器**命中**敌人时 +1 层并刷新持续时间；同一武器重复命中不叠层不刷新；漆黑噤默本体不参与叠层（`MorphWeapon.proc` 中排除）。
- 上限 **9 层**（九种形态各一次）；持续 **150 回合**（`DURATION`，命中新形态时重置）。
- 已支持存档（记录已使用形态集合）。
- 触发接入：`MorphWeapon.proc`（8 种近战形态）+ `LogicStudio.proc`（远程形态，经 `Shot` 委托触发）。共用静态入口 `MorphWeapon.recordMorphCombo(weapon, attacker, defender)`。
- **已使用形态可视化（2026-09-10 新增）**：
  - `MorphCombo.desc()` 按切换窗口九宫格顺序枚举九种形态，**已使用的用 `_高亮_` 标出**，另列"尚未使用"清单；
    形态名统一走 `MorphWeapon.formName(Class)`（`Reflection.newInstance(...).name()`，失败退回类名）——与切换窗口/面板显示文字同源，不重复硬编码。
  - `MorphCombo.hasUsed(Class)` 供 UI 查询；`WndMorph` 九宫格中**已叠过层的形态左上角标绿点**，悬停文字追加"（已使用）"。
  - 中文 `_高亮_` 依赖 `splitforTextBlock` 的正则：`_` 被显式切分为独立 token（无需空格），故中文夹用同样生效。

### Furioso 终结技（漆黑噤默本体）

- 技能名：**Furioso**。
- 可用条件：装备漆黑噤默且 `MorphCombo` 层数达到 9。
- 效果：**每次攻击动画前随机闪身到目标周围的一格**（2026-09-10 由"原地连砍"改为绕目标位移，参考 `FuriosoReplica` 的瞬移写法），共 9 次；
  最后一次动画结束后造成**一次特大伤害**：9 倍武器伤害、必中（`Char.INFINITE_ACCURACY`），然后**清空全部连击层数**（buff 移除）。
- 位移实现（`DarkSilence.blinkAround` / `randomCellAround`）：
  - 在目标 8 邻域内取 `Dungeon.level.passable && Actor.findChar(cell)==null` 的格**随机**选一；
    英雄当前格单独记为"退路"，仅在四周再无可站立空地时使用（否则每次都会换位，避免"原地不动"的一击）；
    完全无可用格时本次不位移，技能照常推进（**不得中断递归，否则英雄永久 busy**）。
  - 无位移动画：`sprite.interruptMotion()` → `pos = dest` → `Dungeon.level.occupyCell(hero)` → `sprite.place(dest)` →
    `Dungeon.observe()` + `GameScene.updateFog()`；起止格各撒一团 `Speck.WOOL` 白烟 + `Sounds.PUFF`。
  - 危险格安全性：深坑 `CHASM` 的地形标志是 `AVOID | PIT`（**不含** `PASSABLE`），故 `passable` 过滤天然排除深坑，不会瞬移落坑。
- 数值：`FURIOSO_DMG_MULTI = 9f`、`FURIOSO_DMG_BONUS = 0`、`FURIOSO_HITS = 9`（`DarkSilence.java` 常量，可调）。
- 时间花费：整个技能 1 次攻击时间（`hero.spendAndNext(hero.attackDelay())`）。
- **索敌距离 9 格（2026-09-10 新增）**：`FURIOSO_RANGE = 9`，目标校验由 `hero.canAttack`（本体近战只有 1 格）
  改为 `DarkSilence.inFuriosoRange(hero, enemy)`（`Dungeon.level.distance` 切比雪夫距离 ≤ 9）。
  远处目标也照常由 `blinkAround` 闪身到其身边再出手；提示语已改为"选择Furioso的目标（9格内）"。
- **快捷栏直放（2026-09-10 新增）**：`DarkSilence.defaultAction()` 覆写——**本体在手且连击集齐时返回
  `AC_FURIOSO`**，快捷栏点击由"切换"变为"释放 Furioso"；未装备时仍返回 `AC_MORPH`（保证手动换形态的入口不被顶掉）。


### 预防性修复：漆黑噤默系列禁止嬗变（2026-09-10）

- `ScrollOfTransmutation.usableOnItem()` 顶部新增 `if (item instanceof MorphWeapon) return false;`（与既有 `BodyArtWeapon` 排除并列）。
- 理由：十种形态共用一套强化/附魔、可免费自由切换，且承载专属连击与 Furioso；嬗变随机化会把整套武器永久变成别的物品。
- 远程形态 `LogicStudio` 直接继承 `Weapon`（非 `MeleeWeapon`），本就不满足 `usableOnItem` 的任何分支 → 天然被排除，注释中已说明。

### 切换表现与自动切换（2026-09-10 新增）

#### 头顶武器虚影（`MorphWeapon.morphInto` 尾部）

- 每次成功切换（手动点九宫格 / 自动切换）后，在英雄头顶播放**所切换到形态的武器虚影**：
  复用松脂涂层、神圣武器同一套效果 `Enchanting.show(hero, replacement)`
  （`effects/Enchanting`：淡入 0.2s → 停留 1.0s → 放大淡出 0.4s，跟随角色头顶，附魔武器自带辉光染色）。
- 防御性判断：`hero.sprite != null && hero.sprite.parent != null` 才播放——`Enchanting.show` 内部直接
  `ch.sprite.parent.add(...)`，切场景（sprite 未挂到场景）时会 NPE。

#### 自动切换开关（`SPDSettings` + `WndMorph` + `MorphWeapon`）

- **开关持久化**：`SPDSettings.KEY_MORPH_AUTO_SWITCH = "morph_auto_switch"`（沿用 `KEY_FIRELINK_NEXT_RUN` 同款写法，
  `GameSettings.put/getBoolean`，默认 **关**）。放在 SPDSettings 而非武器字段上，是因为切换会**重建武器实例**、
  且远程形态 `LogicStudio` 不继承 `MorphWeapon`，放武器上要处处同步备份。
- **按钮**：`WndMorph` 九宫格下方、"取消"上方新增 `RedButton`「自动切换：开 / 关」，点击即时切换并刷新按钮文字，
  悬停提示说明机制（`hoverText()`）。
- **触发**：`MorphWeapon.recordMorphCombo` 末尾调用 `MorphWeapon.scheduleAutoSwitch(hero, weapon)`，
  因此**命中**即触发（近战形态经 `MorphWeapon.proc`、远程形态经 `LogicStudio.proc` 共用同一入口）；
  **本体 `DarkSilence` 同样生效**（便于一路挂机叠层）。
- **目标池**：`ALL_FORMS` 去掉本体 `DarkSilence`（本体不计层、也不作为目标）后，再剔除 `MorphCombo.hasUsed(cls)`
  已记录的形态；**连击一旦集齐九层，则忽略随机池、直接切回本体 `DarkSilence`**（2026-09-10 新增）——
  Furioso 只能由本体释放，这样打满九层后不必再手动翻切换窗口，且本体的快捷栏点击此时即为"释放 Furioso"
  （见 `DarkSilence.defaultAction()`）。
- **本体在手时的处理**：集齐前，持本体命中会照常随机切到未用形态（便于一路叠层）；集齐后目标即本体自身，
  `target != used.getClass()` 短路，不再切换，玩家可以连续开大。
- **为什么延后到攻击结算之后**：本方法在 `proc()` 期间被调用，而同一击随后还会走
  `Hero.attackDelay()`（读 `belongings.attackingWeapon().delayFactor`，各形态 DLY 从 0.5 到 1.5 不等）、
  `Talent.onHeroAttackLanded/Resolved`、`CombinedLethalityAbilityTracker` 判定等。若在 `proc` 内直接换武器，
  这些都会读到**新**形态，攻速与判定全部错位。故改用 `Actor.add(new Actor(){ actPriority = VFX_PRIO; ... })`
  的临时 Actor：攻击动画早已播完（`CharSprite.onComplete` 先 `idle()` 再 `onAttackComplete()`）、
  `spend(attackDelay())` 也已按原形态执行，之后才换武器 —— 与 `Hero.attackProc` 中狙击手标记的写法同源。
  Actor 内再校验 `hero.belongings.weapon()/secondWep() == used`（期间可能被手动换下），执行完 `Actor.remove(this)`。
- **无回合消耗**，与手动切换一致。

### 掉落池投入（2026-09-10）

- **只有本体 `DarkSilence` 入池**：`Generator.WEP_T5.classes` 末尾追加 `DarkSilence.class`，`defaultProbs` 同步
  追加权重 `2`（14 项对 14 项，与其余五阶武器一致）。本体系列显示为 5 阶 → 落在 `WEP_T5`。
- **九个切换形态不入池**：`Durandal / ElmWorkshop / LangyaWorkshop / OldBoyWorkshop / ArasWorkshop /
  InkWorkshop / LogicStudio / CalistaStudio / RouletteHeavy` 均为**衍生武器**，只经 `MorphWeapon.ALL_FORMS`
  在切换时重建，`Generator` 各池中一概不出现（已 grep 核验）。
- **连带效果**（符合预期，无需额外处理）：
  - `Catalog.MELEE_WEAPONS.addItems(Generator.Category.WEP_T5.classes)` → 本体自动进入图鉴，九形态仍不入；
  - `ScrollOfTransmutation.changeWeapon` 用 `wepTiers[tier-1]` + `randomUsingDefaults` 取池，因此**其它五阶武器
    嬗变时可能随机出本体**；反向（本体→其它武器）已被上节的 `instanceof MorphWeapon` 排除挡住。
  - `VaultManyScansRoom` / `VaultMultipleEnemyTreasureRoom` 的 `randomUsingDefaults(WEP_T5)` 同样会取到本体。

## 数值速查（原版参考）

- 1 阶：力量 10；Gloves `min=1+lvl`，`max=5+lvl*1`，DLY 0.5
- 3 阶：力量 14；Mace `max=16+lvl*4`，ACC 1.28
- 4 阶：力量 16；Longsword `max=25+lvl*5`；AssassinsBlade `max=20+lvl*5`（突袭 50%~max）；Katana `max=20+lvl*5`（格挡 3）
- 5 阶：力量 18；WarHammer `max=24+lvl*6`，ACC 1.20；Glaive `max=40+lvl*6`，DLY 1.5，RCH 2；Gauntlet `max=15+lvl*3`，DLY 0.5；Greataxe `max=45+lvl*6`，力量 20
