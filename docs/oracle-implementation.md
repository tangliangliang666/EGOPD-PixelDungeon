# 神谕代行者（ORACLE）实现细节归档（冻结快照）

> 2026-09-05 从 AGENTS.md 旧版 §5/§2/§3 迁出（AGENTS.md 已重组为通用开发经验手册，职业细节按类归档）。
> **状态**：神谕代行者的设计已完善（用户确认），本文件仅作**实现细节冻结快照**，日常不主动维护。
> 神谕代行者的设计记录与早期实现历史见 [oracle-class-design.md](oracle-class-design.md)；含神谕示例的通用陷阱仍在 `docs/handbook/pitfalls.md` §6 陷阱速查表（AGENTS.md §6 为精简表）。

## 职业配置总览（原 AGENTS §5，为迁出时最新状态）

- **职业**：开局携带赫尔墨斯的双蛇杖（`HermesCaduceus`，随机形态武器，STRReq 按 1 阶 10 算）+ 指令终端（`InstructionTerminal`，unique、无法卸下/替换/嬗变）+ 业（`Karma`，上限 100，诸业加身天赋降为 80/60/40）+ 指令标记（`InstructionTarget`）+ 指令计时器（`InstructionTimer`，150~200 回合 *哔哔*，急促蜂鸣缩短）。
- **指令任务**（`Instruction.java`）：难度 0~5（0=命运宠儿+3 特殊，1=自动完成，2=简单，3=普通，4=困难，5=无解）。每难度一个任务池随机抽取，**每个任务独立的完成条件**通过静态钩子从游戏代码回调（`onFoodEaten/onSearched/onEnemyKilled/onTargetKilled/onFloorExit/onItemUsed/onBossKilled`）。失败业 +5；完成触发加护/解放/掉落/业报消转。
- **特殊指令**：`Instruction.triggerSpecial`（背水一战=LockedFloor 锁层时、隐藏房间=进入有 SecretRoom 的楼层时）→ 待接特殊标记 `pendingSpecial` → 接取时覆盖当前任务；boss 指令时间不限、接取即 +1 指令加护、击杀 Boss 完成；隐藏指令仅提示不算任务。
- **盔甲技能**（三个已独立）：心-命运（HeartFate，20% 充能，20% 精准/闪避 buff + 金色火焰粒子）、Furioso-Replica（90% 充能，九连必中瞬移击退 + 收尾镰刀）、拼好饭（**30%** 充能，50 饱食度 + 治疗，不触发进食特效除非食之有味）。每个技能 3 个分支天赋 + 通用英勇能量（HEROIC_ENERGY，图标 26）。
- **剑刃解放（T2）**：完成指令获得 1 层解放，上限**按天赋点数取** —— `+1` = 6 层、`+2` = 9 层（`Release.growthCap`）。`+2` 时还会按「业」给保底层数（业每 10 点 1 层、90 点 = 9 层）；解放 buff 死亡清除，复活时由 `Release.onHeroRevive` 按业重挂（2026-09-20）。
- **命运弃子**：拒绝指令、*哔哔* 叠烧灼的伤口（上限 20，>10 层每回合概率自伤 1 点火伤）、苦痛技艺三招（1/2/3 层消耗）、食指代行者追杀（每层 1 只 + 自然生成 1/4→逐次减半概率、15 回合定位一次并红色提示）。
- **神的宠儿**：完成指令 +1 指令加护 + 财富等级掉落（财富等级 = (加护层数+1)/2 + 财富戒指加成，即 1/3/5/7/9 层→财富 1/2/3/4/5 级；物品掉落在角色脚下，不直接收进背包）；命运宠儿天赋控制指令难度。

## 硬编码中文文本位置清单（原 AGENTS §2）

神谕代行者的自定义内容大量使用硬编码中文（不走 messages），改文本需改代码：

- `actors/buffs/Instruction.java`（指令任务文本与 `_CLEAR_`）
- `actors/buffs/ScorchingWound.java` / `Karma.java` / `DivineBlessing.java`（buff 名/描述）
- `actors/hero/abilities/oracle/FuriosoReplica.java`（"我将再现那颗激愤之心"、结束语三选一）
- `actors/hero/abilities/oracle/PinHaoFan.java`（"苦痛啊，你便是我的唯一...."）
- `items/weapon/melee/HermesCaduceus.java`（形态名 叉勺/镰刀）
- `items/artifacts/InstructionTerminal.java`（按钮名）

## 天赋图标索引（原 AGENTS §3）

神谕代行者的天赋图标使用 talent_icons.png **第八行**，枚举值直接写第八行**绝对索引**（224~250，每行 32 列；HEROIC_ENERGY 是全职业共用天赋，在 `icon()` 内特判 case ORACLE → 250）。图标索引位置必须与图内实际绘制一致。
