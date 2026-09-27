# 赫尔墨斯的双蛇杖 设计记录

> 《破碎的像素地牢》改版自定义武器。本文档记录设计决策，防止上下文丢失。

## 当前状态（验证阶段）

- **名称**：赫尔墨斯的双蛇杖（`HermesCaduceus`，`items/weapon/melee`）
- **等阶**：5（面板显示五阶；`min/max/STRReq` 保持默认五阶占位数值）
- **贴图**：`ItemSpriteSheet.HERMES_CADUCEUS = xy(11, 33)`（轮盘重工右侧），`assignItemRect(..., 14, 16)`（宽 14 高 16）
- **「业」buff**（`Karma`，`actors/buffs`）：
  - 装备时获得、卸下（主手副手都不在）时解除
  - 持续时间无限；层数范围 0~100（`setKarma`/`addKarma`/`karma`）
  - 图标：自定义图标 `BuffIndicator.KARMA = 86`（原版最后一个 buff `THROWN_WEP=85` 右侧；`buffs.png` 与 `large_buffs.png` 均已绘制）
  - 描述："接受指令，并将其执行。这将化为一种业，于循环往复中不断积累。"
- **武器描述**："从两段流溢出黑色液体金属的短杖。根据指令之意，液体会变化为不同武器的形状。攻击时，会随机获得不同武器的性质。"

## 随机形态机制（已实现）

每次攻击前随机选取一把原版近战武器，本次攻击完全采用该武器的数值与特殊机制。

- **两层随机**：
  1. `rollTier()`：先随机阶位 **0~6**——**按「业」层数加权**（方案 A 指数权重）+ **按力量过滤**：
     - **力量过滤**：先排除力量不足的阶位（力量需求 = `STRReq(阶位, 强化等级)` − 力量药水减需 2，需求 ≤ 当前力量才可用），避免力量惩罚（命中/攻速下降）阻碍战斗；例如力量 10 时只会随机到 0/1 阶
     - **业加权**：在可用阶位内，权重 w_k = x^k（k=0..6），**x = 10^(1 − e/50)**——业 0 → x=10 → 可用阶位中偏高阶；业 100 → x=0.1 → 偏低阶；业 50 → 均匀
     - 0 阶需求恒 ≤8，始终可用兜底
  2. 再在同一阶内随机一把武器
- **力量需求计算**：委托模板武器的 `accuracyFactor`/`delayFactor`（用模板自身 tier 与强化等级的 STRReq），力量惩罚按随机形态正确计算；`masteryPotionBonus`（力量药水减需）已同步到模板实例
- **阶位结构**：0 与 6 阶为**内置伪武器**（`FormSpec`，不创建独立武器类）：
  - **0 阶「叉勺」**：min = 0+lvl，max = 5+lvl，DLY 0.5，STRReq 8
  - **6 阶「镰刀」**：min = 6+lvl，max = 45+lvl×7，DLY 1，STRReq 20
  - 伪武器同样支持头顶弹名（`formSpecDamageRoll`）、augment、力量加成/惩罚（按自身 tier 的 STRReq）
- 1~5 阶为**原版武器**（`TIER_WEAPONS`，按 Generator 的 WEP_T1~T5 分组，排除 MagesStaff、Pickaxe）：
  - 1 阶：破旧短剑、匕首、手套、刺剑、权杖
  - 2 阶：短剑、手斧、矛、铁头棍、长匕首、短柄镰
  - 3 阶：剑、硬头锤、弯刀、圆盾、双钗、长鞭
  - 4 阶：长剑、战斧、链枷、符文之刃、暗杀之刃、十字弩、武士刀
  - 5 阶：巨剑、战锤、关刀、巨斧、巨盾、拳套、战镰
- **数值委托**：反射创建模板武器实例并 `level(buffedLvl())` 同步本武器强化等级，然后委托：
  - `damageRoll`（**模板用自己的 tier 计算 min/max，因此变形成低阶武器时伤害按低阶公式计算**；突袭强化如匕首 75%、暗杀之刃 50% 自动生效）
  - `accuracyFactor`（含模板 ACC 与力量需求惩罚）
  - `delayFactor`（含模板 DLY 与力量惩罚）
  - `reachFactor`（含模板 RCH）
  - `defenseFactor`（含模板格挡，如武士刀 3、巨盾 6+2lvl）
  - 强化 augment 传递到模板
- **挂钩点**：`reachFactor`（攻击前 canAttack 检查时重新随机；`accuracyFactor` 兜底）
- **攻击反馈**：命中时（`damageRoll`）在角色头顶以跳字方式（橙色 `CharSprite.WARNING`）显示本次随机到的武器名称

## 已知取舍（后续可完善）

- 链枷形态的"不能突袭"负面未模拟（需侵入 `Hero.canSurpriseAttack`）
- 本武器自身附魔（如 Projecting 射程加成）在委托时丢失
- 防御格挡使用最近一次攻击的形态；从未攻击时无格挡
- 十字弩形态仅套用近战面板（20+4/级），飞镖强化机制不生效
- 面板仍显示五阶占位数值（实际攻击数值随形态变化）；禁用决斗者技能与固定统计显示

## 待办

- 数值设计（武器本体数值、各形态适配）
- 「业」的效果机制
- 考虑是否加入 Generator 掉落

## 指令终端神器（2026-08 追加，验证用占位）

- **WndDebug 新增「神器」栏位**：`ItemTab(Artifact.class, PKG_ARTIFACTS)`，反射扫描 `items/artifacts` 包，列出全部神器（蓄血圣杯、丰饶号角等）
- **指令终端**（`InstructionTerminal`，`items/artifacts`）：验证用占位实现——效果套用蓄血圣杯（`ChaliceOfBlood`）：
  - **贴图**：自定义 `ItemSpriteSheet.INSTRUCTION_TERMINAL = xy(1, 34)`（漆黑噤默贴图 `xy(1,33)` 的下一行第一个，`assignItemRect(..., 16, 14)`）
  - 被动 `chaliceRegen`（缓慢回血）、`charge`、自刺升级 `AC_PRICK`（文本硬编码中文）
  - 仅 `name()`/`desc()` 改为"指令终端"（描述注明占位）
  - 后续将设计为与「指令」任务系统交互的专属神器

## 指令任务系统（2026-08 追加）

与「业」交互的限时任务玩法。**所有相关提示均为淡蓝色**（新增 `GLog.CYAN` 前缀 `"~~ "`，`GameLog` 渲染为 0x8FE3FF；`GLog.c()` 输出）。**信息栏与角色头顶同步显示**（`hero.sprite.showStatus(Instruction.LIGHT_BLUE, 文本)`，淡蓝色跳字）。

- **触发**（测试阶段）：装备赫尔墨斯的双蛇杖时立即触发（`HermesCaduceus.activate`）：
  1. 显示 `*哔哔*`（淡蓝）
  2. `Instruction.startInstruction(hero)` 显示 `致：50回合之内击杀三名敌人` 并开始计数
  - 之后改为随机触发（已注释标明）
- **任务**（`Instruction`，`actors/buffs`，不显示 buff 图标）：
  - `TURNS = 50` 回合内击杀 `KILLS = 3` 名敌人
  - 每回合递减（`act()`）；击杀计数由 `Mob.die` 的英雄击杀分支挂钩（`Instruction.onEnemyKilled`，判定 `cause == hero / Weapon / Weapon.Enchantment`）
  - 成功：显示 `_CLEAR_`（淡蓝），任务结束
  - 限时未完成：`业 + 5`（`Karma.addKarma(5)`），**不显示失败信息**，任务结束
- 支持存档（剩余回合/剩余击杀数）

## 遗骸：不进英雄遗骸（2026-09-20）

本武器是神谕代行者的**专属**武器，**不进英雄遗骸**——实例初始化块里显式 `bones = false;`。

**为什么容易漏**：`Item.bones` 默认是 `false`，但 **`EquipableItem` 把它置为 `true`**，所以武器/护甲/神器**默认都会进遗骸**（`Bones.pickItem` 按 `item.bones` 筛候选；`Bones.get` 取出后还会降到 +3 并强制诅咒）。不显式关掉，神谕代行者一死，下一局别的角色拾取遗骸就能拿到本武器。

核验：`_chk/verify_hermes_firelink.py`（含「实例块内 `bones = false`」与「代码内没有 `bones = true`」两条断言 + 反例自测）。
