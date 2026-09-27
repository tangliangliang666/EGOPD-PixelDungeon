# 自定义武器创作指南（自用流程）

> 本指南供**手动创建普通新武器**时按步骤执行。普通武器（面板/图标/文本/入池）可完全自行完成；
> 需要**特殊功能**（引擎钩子、新机制）时再交给 AI 实现，并在对应档案记录。
> 通用机制/陷阱仍以根目录 AGENTS.md 为准；环指大师人体派作品等专属体系见 docs/ring-master.md。

## 0. 先想清楚三类问题

1. **定位**：几阶武器（tier 1~6）？力量需求默认由 tier 决定（tier=N 需求 N+2? 以原版为准，四阶=16）。伤害公式 min(lvl)~max(lvl) 是否用默认（tier+lvl ~ 5(tier+1)+lvl(tier+1)）？
2. **效果**：有无特殊效果？效果走什么钩子（武器 proc/精度/伤害/攻速/引擎判定层）？
3. **归属**：全局自然掉落（入 Generator 对应阶池）？还是特定来源（挑战/遗物/初始/兑换）？环指专属？是否允许附魔/升级？

## 1. 创建武器类文件

路径：`core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/weapon/melee/<ClassName>.java`

- `extends MeleeWeapon`；无特殊功能时继承即可，武技文本沿继承链回退父武器。
- 实例块：`image = ItemSpriteSheet.<ICON>;`、`tier = N;`（覆写面板时 **min/max 显式覆写**，别依赖默认公式——参考 DiamondSword/VoidDiffraction）。
- 面板例：`min(lvl)=X+lvl`、`max(lvl)=Y+Z*lvl`；`DLY/RCH/ACC/hitSound` 可在实例块覆写。
- 需要"套用某原版武技配置"可继承同武技的原版武器类（如 RedEye extends Cudgel），仅实例块覆写 image/tier/min/max。
- 若无特殊效果：到此为止即可（可自行完成）；有特殊效果：把需求交给 AI，指明钩子点与期望行为。

### 模版：可直接复制改写的普通武器类

把下面整段复制到 `melee/<类名>.java`，按 `← 填写` 注释改：换类名（帕斯卡命名，消息键=类名全小写）、填 tier、图标常量、伤害公式；不需要的覆写整段删除。文件头保留与项目其他文件一致的标准 GPL 许可证头（第 20 行"License"之前的部分照抄即可，此处省略）。

```java
package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * ← 填写：武器中文名（几阶武器，一句话定位）。
 * ← 填写：特殊效果一句话说明（无则整段删除）。
 * 面板：← 填写 min~max 公式；文本键 items.weapon.melee.<类名全小写>.name/desc/stats_desc。
 */
public class YourWeaponName extends MeleeWeapon {   // ← 类名 = 英文名，帕斯卡命名

	{
		image = ItemSpriteSheet.YOUR_ICON;        // ← 关联点：第 2 步注册的图标常量（见第 2 节"显示链路"）
		tier = 4;                                  // ← 阶位 1~6（决定力量需求与默认面板）

		//—— 手感参数（与同阶原版默认一致，需要不同才取消注释改值）——
		//hitSound = Assets.Sounds.HIT_SLASH;      // 音效：HIT_SLASH(斩)/HIT_STAB(刺)/HIT_CRUSH(砸)
		//hitSoundPitch = 1f;                      // 音高（0.9 低沉 / 1.2 尖锐）
		//DLY = 1f;                                // 攻击延迟：>1 慢速（长矛 1.5）、<1 快速（匕首 0.5）
		//RCH = 1;                                 // 攻击距离：1 近战，2/3 长柄（长鞭 3）
		//ACC = 1f;                                // 精准倍率：<1 不精准（链枷 0.8）、>1 精准（手斧 1.32）
	}

	//—— 伤害面板：下限（默认公式 = tier + lvl；与默认一致可整段删除）——
	@Override
	public int min(int lvl) {
		return 4 + lvl;                            // ← 填写：如 4+L
	}

	//—— 伤害面板：上限（默认公式 = 5×(tier+1) + lvl×(tier+1)；与默认一致可整段删除）——
	@Override
	public int max(int lvl) {
		return 15 + 5*lvl;                         // ← 填写：如 15+5L
	}

	//—— 可选：命中后特殊效果（示例=附加 1~2 流血；不需要则整段删除）——
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);
		//if (defender.isAlive()){
		//	Buff.affect(defender, Bleeding.class).set(Random.Int(1, 3));  //1~2 流血
		//}
		return damage;
	}
}
```

**复制后对照清单**（对应指南步骤）：① 类名/文件名一致；② 在 ItemSpriteSheet 注册 `YOUR_ICON` 常量 + 实际尺寸 → ③ 在 items.png 该格绘制图标 → ④ 加消息键（zh+en）→ ⑤ 需要自然掉落就进 Generator 池（prob 与 classes 两数组对齐）→ 需要 E.G.O 标注就登记进 EGOWeapons 对应阶数组 → ⑥ 编译 + F2 生成测试 → ⑦ 归档。

> 提示：伤害公式/手感请以"同阶原版武器"为参照抄数（如四阶默认 min=4+L、max=25+5L）；面板想"偏低/偏高"就在默认 max 基础上加减。看不懂默认值出处时直接问 AI 要该阶默认面板即可。

## 2. 注册图标常量并关联到武器（ItemSpriteSheet.java）

- 找 **空位**：查看 sprites/ItemSpriteSheet.java 中该行已占用列（如 36 行已用 1/2/4/5/6/12/13 列），**绝不可与已有常量重叠**（重叠=图标串图）。
- 声明：`public static final int XXX = xy(列, 行);`（xy 1 基：第一参列、第二参行，每行 16 格）+ 中文注释标明行列与尺寸。
- `static{ }` 内 `assignItemRect(XXX, 宽, 高);`——**非 16×16 必须写实际尺寸**；16×16 也建议显式写。

**如何把图标和武器关联起来（显示链路）**：注册的常量只是一个**帧索引**，武器靠它取贴图——

```
武器类实例块  image = ItemSpriteSheet.XXX;   ← 关联点（第 1 节模版已含此行）
        ↓ ItemSprite 用 Item.image 这个索引
ItemSpriteSheet.film.get(XXX)                ← 取 items.png 上该格子的 UV 矩形
        ↓ 渲染
物品在地面/背包/快捷栏/窗口显示该格贴图
```

因此**三处必须自洽**，任何一处不一致都会显示空白/错位/串图：

1. **常量声明位置**（`xy(列,行)`）必须 = 你在 items.png **实际绘制**的格子（第 3 步）；
2. **assignItemRect 宽高**必须 = 该格图标的实际像素尺寸（非 16×16 时尤其关键）；
3. **武器类里 `image = ItemSpriteSheet.XXX;`** 必须赋这个常量——漏赋则显示默认占位图标（如问号/空白），赋成别的常量则显示别的物品的图标。

> 自查口诀：**画在哪格 → xy 就写哪格；画多大 → assignItemRect 就写多大；类里 image 只认这个常量。**

## 3. 绘制贴图（items.png）

- 用像素编辑工具在 items.png **你声明的格子**上绘制图标（底部对齐惯例），保存后自查：PowerShell 逐像素扫描 alpha 可验证位置/尺寸对不对。
- 尺寸务必与 assignItemRect 一致；没画/画错位置 → 游戏里显示空白/错帧。

## 4. 文本键（messages/items/items_zh.properties + items.properties）

键名 = `items.weapon.melee.<类简单名全小写>.name/desc`（逐字核对类名！键名错=显示键名原文）。

- zh 与 en **都要加**（其余语言缺键回退英文）。
- desc 语法：`_文字_`=强调色；`\n`=换行；带参数的模板里字面 `%` 必须 `%%`。
- **desc 与 stats_desc 分工**：`desc`=氛围文本；效果/功能性文本放 `stats_desc`（`MeleeWeapon.info()` 会自动把 `statsInfo()`=`Messages.get(this,"stats_desc")` 拼在面板统计之后，盾类另有 `typical_stats_desc` 供未鉴定态）。参考 voiddiffraction（desc=氛围三段，stats_desc=效果句）。

## 5. 入掉落池（Generator.java，按需）

- `items/Generator.java` 顶部 import 类。
- 对应阶位数组（WEP_T2/3/4/5/6）`Category.WEP_TN.classes` 末尾追加 `<Class>.class`；
- `WEP_TN.defaultProbs` 同步追加同长度的概率值（原版同级=2，特强/特殊=1，**两数组下标必须一一对齐**，否则抽奖越界/错位）。`probs` 会自动从 defaultProbs clone，无需改。

### 给武器添加 E.G.O 标签（按需，勿重复造轮子）

E.G.O 是现成的"神谕自定义武器"标注机制（`EGOWeapons.java` + `MeleeWeapon.info()` 展示）。给武器打 E.G.O 标签只需一步：

- 在 `items/weapon/melee/EGOWeapons.java` 的**对应阶数组**（TIER_2~TIER_6）末尾追加 `<类>.class`（同包无需 import）。
- 效果自动获得：① `MeleeWeapon.info()` 追加 `这件武器是_E.G.O武器_。` 标注；② 可被炼金 **E.G.O 分解**（`items/Enkephalin.java`，分解为脑啡肽）；③ 可被 **E.G.O 重构**随机产出（`EGOWeapons.randomOfTier`）。若武器不应参与分解/重构，则不要打此标签。
- 注意：E.G.O 与 Generator 掉落池互不影响（2~5 阶 E.G.O 武器本就在池中）。

## 6. 编译与验证

- `:core:compileJava`（详见 AGENTS.md §1）。
- 桌面 `build-desktop.bat` 启动后 F2 → WndDebug「武器」标签会自动出现新类（classpath 扫描，无需注册），可即时生成测试；自然掉落需实际跑层验证概率。

## 7. 记录归档（同步约定）

- 无特殊效果：在对应功能档案记一行（名称/面板/图标格/来源）。
- 有特殊效果：**必须记录效果实现位置**（改了哪个引擎方法/钩子点），否则后续 AI 找不到实现、易重复改动。
- 通用陷阱/规律新增时按 `docs/handbook/pitfalls.md` §6 速查表（AGENTS.md §6 为精简表）补充。

---

## 已实现示例：虚无衍射体（VoidDiffraction，2026-09-05）

- **定位**：四阶通用武器，全局掉落（Generator WEP_T4，prob=2）。
- **面板**：`min=4+lvl`、`max=15+5*lvl`（显式覆写，比四阶默认上限 25+5L 低 → "伤害较低"）。
- **图标**：`VOID_DIFFRACTION = xy(13, 36)`，16×16（36 行第 13 格，用户已绘制）。
- **文本键**：`items.weapon.melee.voiddiffraction.name`（名称）、`.desc`（氛围文本三段）、`.stats_desc`（效果句"伤害较低/任何攻击都被视为偷袭"）；zh+en 双写。
- **E.G.O 标签**：已注册进 `EGOWeapons.TIER_4`（info 显示 `这件武器是_E.G.O武器_。`；可被 E.G.O 分解/重构机制处理）。
- **特殊效果**："即使被敌人发现，攻击也始终被视为偷袭" —— **引擎实现位置：`actors/mobs/Mob.java` 的 `surprisedBy(Char, boolean)`**。原理：偷袭判定=敌方看不见/未察觉 && 攻击者可偷袭；对持有本武器的英雄攻击，无条件放行"看不见/未察觉"这一支（防御为 0 → 必中，并触发偷袭音效/计数/Surprise 特效）。若日后需调整判定（如仅近战、排除 BOSS、改为概率），改这一处即可。

## 已实现示例：传火大剑（FirelinkGreatsword，2026-09-05，用户自建）

六阶近战（6+L ~ 35+5L），自带烈焰附魔 + 炼金合成 + 通关继承，含多处引擎接线：

- **内置烈焰能力（非自带附魔）**：`proc()` 内每击手动调 `new Blazing().proc(this, attacker, defender, damage)`（items/weapon/enchantments/Blazing.java），**不占用附魔槽**——用附魔卷轴覆盖/更换附魔后烈焰依然生效（可与其它附魔叠加）；`glowing()` 覆写：无附魔时常驻烈焰橙光、有其它附魔时优先显示该附魔辉光。早期"构造器 enchant(new Blazing())"方案已弃（覆盖附魔即失效）。
- **炼金配方（3 料）**：元素余烬(items/quest/Embers) + 巨剑(Greatsword，需鉴定且非诅咒) + 龙血秘药(ElixirOfDragonsBlood)，**12 能量**。三处接线：
  1. 配方本体：`FirelinkGreatsword.CraftRecipe`（inner class extends Recipe，brew 消耗 1 余烬 + 巨剑 + 秘药）；
  2. 炼金釜可用：登记进 `items/Recipe.java` 的 `threeIngredientRecipes` 数组；
  3. **"提取结晶"选卡显示**：`ui/QuickRecipe.java` `getRecipes(pageIdx)` 的 **case 8** 追加 QuickRecipe 卡片（连同此前缺显示的**失乐园** ParadiseLost、**薄暝** DimDusk 一起补进 case 8；三张卡对应各自 CraftRecipe）。
- **通关继承**（胜利→下一局开局持有，并继承原剑「**等级/5（向下取整）+ 附魔**」；死亡不触发。2026-09-20 由「只继承 +0 白板」加强）：
  - **记录**：两种胜利结局都要打标——①幸福结局（上地面）：`SewerLevel.activateTransition` SURFACE 分支（切 SurfaceScene 前）；②普通结局（使用护符结束）：`Amulet.execute` 的 `AC_END` 分支（切 AmuletScene 前）。两处均判断 `FirelinkGreatsword.equippedBy(hero)`（主/副手装备）→ `markNextRun()`。
  - **`markNextRun()` 存什么**（`SPDSettings` 三键）：①`firelink_next_run = true`；②原剑的**纯升级等级 `trueLevel()`** 写入 `firelink_level`（**不是** `level()`，见通用教训③）；③附魔**类全名** `enchantment.getClass().getName()` 写入 `firelink_enchant`（无附魔存空串）。
  - **发放**：`Dungeon.init()` 在 `initHero` 之后调 `grantAtRunStart()`：有标记则 → 读三键并**立即清零**（防残留到再下一局）→ `new FirelinkGreatsword()` → `upgrade(srcLevel / INHERIT_LEVEL_DIVISOR)`（`INHERIT_LEVEL_DIVISOR = 5`，整数除法即向下取整；`0` 级不加）→ `enchantFromName(...)` 反射重建附魔（`Reflection.forName` + `isAssignableFrom(Weapon.Enchantment.class)`，失败**静默退回白板**）→ `identify()` → 收进背包。
  - **向后兼容**：旧版只写过 `firelink_next_run`，新增两键缺省值 `0` / `""` ⇒ 老档照样拿到 +0 白板剑。
- 通用教训：① 炼金指南"提取结晶"等选卡内容 = `QuickRecipe.getRecipes(pageIdx)` 的对应 case（页索引与 Journal 的 ALCHEMY_GUIDE 文档页对应）；新增高级配方记得同时注册 Recipe 列表 + QuickRecipe 卡片；② `QuickRecipe.java` 等 ui 文件原为 **CRLF**，程序化改文件时注意换行（建议统一 LF 后写入）；③ **跨局继承取等级一律用 `trueLevel()`**——`Weapon.level()` 含 `curseInfusionBonus`（`+1+level/6`），继承品不带该加成，用 `level()` 等于白送等级；④ 设置只存得下 `int/boolean/String`（存不了 `Bundlable` 对象）⇒ 要跨局带一件**附魔/形态**过去，存**类全名**再用 `Reflection.forName` + `newInstance` 反射重建最省事，并给「类不存在/不是该类/构造失败」留 `null` 兜底。

## 已实现示例：次元撕裂者（DimensionalRipper，2026-09-22，用户自建）

三阶武器，**面板整套照抄弯刀**（`extends Scimitar`，不覆写 `min/max` 即严格同面板）；自带「转移」
（命中**有概率**随机传送目标或自己，概率从 `+0` 的 1/8 起随强化等级趋近 1/2）；传送后 3 回合内出现
「空间撕裂」按钮，瞬移到目标身旁并追加一击。

- **要「数值完全等于某把原版武器」，最省事的做法是 `extends` 那把武器**，不覆写 `min`/`max`
  （本项目先例：`Oblivion extends Scimitar`、`RedEye extends Cudgel`）。只覆写 `image`/`DLY` 等即可；
  武技配置与文本也沿父类链继承。
- **「命中时概率触发」用 `proc()`**（与附魔同一个钩子）：`damage = super.proc(...)` 必须在**触发判定之前**
  （先走完原版附魔/护盾结算再判转移）；概率乘 `Weapon.Enchantment.genericProcChanceMultiplier(attacker)`
  才能吃到奥术之环等加成；触发前三项照抄原版诅咒：魔法免疫 / 目标存活 / **真造成了伤害**。
- **⭐ 可复用范式：给武器配一个「限时窗口按钮」**（本项目已有机制，别重复造轮子）——
  `Buff implements ActionIndicator.Action` + 占**副槽**（`ActionIndicator.setSecondAction`，
  槽位 1，`SPDAction.TAG_ACTION_2`，默认不绑键；主槽留给职业大招），参考
  `SealedSwordBase.SwordSwap`、`MasterRing.QuickArtAction`、本作 `RiftWindow`。四件必做：
  1. **占槽**：`open()` 与每回合 `act()` 都调同一个 `keepSlot()`（`secondAction != this` 才 `setSecondAction`）
     ——别的动作可能把槽位抢走、场景重建后也要重挂；
  2. **计时**：想「触发当回合就算一格」就用 `act()` 里 `turnsLeft--` → `spend(TICK)`，
     **并在 `open()` 里补 `timeToNow()`**。注意：新挂载的 buff 由
     `Buff.attachTo → Char.add(buff) → Actor.add(buff)`（`actor.time += now`）**本就调度在 `now`**，
     所以 `timeToNow()` 对新挂载是空操作；它真正救的是**重复触发**（buff 已存在、`time` 已被
     `spend(TICK)` 推到 `now+1`，不拉回来就白送一格）；
  3. **失效判据收口成一个 `stillValid()`**（本件：武器还在主/副手 + 没换层 + 目标还活着），
     `act()` 与 `attachTo` 共用，避免两处各写一套；
  4. **收尾**：`detach()` 里 `ActionIndicator.clearAction(this)`（只在槽位确实是自己时才清）。
     若窗口锁定了某个 `Char`（引用无法随存档搬运），**刻意不重写** `storeInBundle`/`restoreFromBundle`，
     读档后自然失效即可。
- **追加一击的写法**（照 `ArtTechniques.DISSECT`）：`hero.busy()` → `ScrollOfTeleportation.appear(hero, dest)`
  → `hero.sprite.attack(victim.pos, callback)`；回调里 `hero.attack(victim, 1f, 0f, Char.INFINITE_ACCURACY)`
  + `hero.spendAndNext(hero.attackDelay())`。**落点用 `appear` 不用 `teleportToLocation`**——后者要过可达性判定，
  而这里要的正是无视距离。
- **完整决策表、失败样例与核验脚本**见 `docs/features.md`「2026-09-22 新增三阶武器：次元撕裂者」；
  源码级核验脚本 `_chk/verify_dimensional_ripper.py`（130 条断言 + 22 条反例自测）。

## 给武器加粒子特效（可复用范式：`RiftParticle`，2026-09-22）

### 1. 粒子类怎么写

`extends PixelParticle`（本质是 1×1 纯白方块 `PseudoPixel`，靠 `color()` 上色、`size()` 定边长、
`am` 控透明度）。照抄 `PinkParticle` / `SparkParticle` / 本项目已有的 `TearSwordTrailParticle`。

- 每个行为一个 `public static final Emitter.Factory`（`emitter.recycle(X.class)` 回收槽位后调自己的 `resetX()`）。
  **别**把参数塞进 Factory 字段再共享同一个实例（`Splash` 是那么写的，但那是历史包袱，多色并发会互相踩）。
- 想要「发光」就让 `lightMode()` 返回 `true`（`Emitter` 用叠加混合画整层）。
- **`revive()` 不还原透明度**：`resetX()` 里必须手动 `am = 1f`，否则复用旧槽位时重生首帧会闪一下透明。
- `update()` 里把淡出系数钳到 0（`Math.max(0, left/lifespan)`）——寿终那一帧基类已把 `left` 减到 ≤ 0，
  不钳会算出负边长。

### 2. 配色要有「唯一权威来源」，并且可被核验

- 颜色常量写成一个 `public static final int[] COLORS`，**注释里写明取自哪张贴图的哪一格**。
- 若配色来自贴图，就让核验脚本**回采那张 PNG 逐色比对**（本项目用
  `_chk/verify_sprite_frames.decode_png` + `pixel()`，纯 Python 无 PIL）。
  比「常量里有 6 个颜色」强得多——它同时钉住了「配色确实来自贴图」。
- 划分「哪些算色调」时给一条**可复算**的规则（本项目：紫 `B ≥ R`、棕 `R > B`、描边 `alpha < 255`），
  否则下次重绘贴图谁也不知道该保留哪几个。
- **不要用 `ColorMath.interpolate` 做颜色渐变**——插值会混出调色板之外的中间色。
  要「随时间变深」就**整档跳到下一个调色板项**（`COLORS[Math.min(base + 1, COLORS.length-1)]`）。

### 3. 一蓬粒子要「六色都出场」：用发射序号，别用随机

Factory 的 `emit(emitter, index, x, y)` 里 `index` 是**本轮发射计数**（`Emitter` 每次 `start` 从 0 重数），
于是 `base = index % COLORS.length` 就得到「按固定顺序轮着取色」。
**只要一次喷够 `COLORS.length` 颗，每一色必定出场**；随机取色则可能一整蓬全是深色，看不出层次。
（核验脚本据此断言 `HIT_PARTICLES ≥ 6` **且** `WARP_PARTICLES ≥ 6` ——
本项目初版命中只喷 4 颗，离线模拟一跑就发现**最深的两档紫永远看不到**，才改成 6。）

### 4. 传送的一套视觉词汇

| Factory | 挂点 | 做法 |
|---|---|---|
| 命中 | `proc()` 里 `damage > 0` 时 | 目标 `sprite.centerEmitter().burst(HIT, n)` |
| 起点 | 传送**成功之后** | `CellEmitter.get(oldPos).burst(SCATTER, n)`，向外炸散（人被打散） |
| 终点 | 同上 | `CellEmitter.center(pos).burst(IMPLODE, n)`，向心汇聚（人重新拼起来） |

- **起点坐标必须在传送前存下来**（`appear()` 会把 `pos` 改写掉）。顺序反了粒子就全出在落点上。
- **IMPLODE 的速度用「位移 ÷ 存活期」**：`speed.set(cx-x, cy-y); speed.scale(1f/lifespan);`
  ⇒ 正好在寿终那帧抵达中心，不冲过头。所以终点取 `CellEmitter.center`（一个点）而不是 `get`（整格）。
- 两端都判 `Dungeon.level.heroFOV[...]`，看不到的地方不出。
- **原版缺口**：`ScrollOfTeleportation.appear()` 只在**非英雄**传送时往起点撒光点、终点一颗不给 ⇒
  英雄自己传送是「两头全空」。自己写一个 `teleportFx(from, to)` 公共入口，武器与 buff 共用，
  两边看起来才是同一件事（本项目见 `DimensionalRipper.teleportFx`）。
- **别叠两套粒子**：接管了配色就顺手把原版的 `Speck.factory(Speck.LIGHT)` 撤掉。

### 5. 新增粒子类要补的核验

`extends` 对不对 / `COLORS` 恰好 N 色且与贴图逐色一致 / 明度单调 / 三个 Factory 都 `lightMode`
/ `index % N` 而非随机 / 代码里没有 `ColorMath` / 各 `reset` 都有 `am = 1f` /
`update()` 钳了 0 且取色只来自 `COLORS` / IMPLODE 的 `scale(1f/lifespan)` /
三处挂点的**顺序**（`damage>0` 在出粒子前、`teleportChar` 在出粒子前、记起点在 `appear` 前）。

## 给武器加「随等级成长的概率」（可复用范式，2026-09-22）

需求形态：`+0` 给一个起点值，随强化等级**趋向**某个上限（本例 1/8 → 1/2）。

```java
public static final float PROC_CHANCE_BASE  = 1/8f;   // +0
public static final float PROC_CHANCE_MAX   = 1/2f;   // 渐近上限（永不到达）
public static final float PROC_CHANCE_DECAY = 7/8f;   // 每级吃掉 1/8 的差距

public static float procChance(int lvl) {
    float gap = (PROC_CHANCE_MAX - PROC_CHANCE_BASE) * (float) Math.pow(PROC_CHANCE_DECAY, lvl);
    return Math.max(0f, PROC_CHANCE_MAX - gap);
}
```

即 `p = 上限 − (上限 − 起点) × 衰减^L`，读作「**与上限的差距每升一级乘 7/8**」。

| 等级 | +0 | +1 | +3 | +6 | +10 | +15 | +30 |
|---|---|---|---|---|---|---|---|
| 概率 | 12.5% | 17.2% | 24.9% | 33.2% | 40.1% | 44.9% | 49.3% |

**为什么指数优于线性**：低强化每级收益最大（+0→+1 一下多 4.7 个百分点）、高强化自然饱和。
线性（`起点 + (上限−起点)×L/10`）会在某级硬顶到上限，之后强化毫无收益，也失去「趋向」的语义。

### 三个必做

1. **概率收一个唯一入口** `procChance(int lvl)`，判定与面板文本共用一处
   ——别在 `proc()` 里内联算式，否则面板显示与实战概率必然对不上。
2. **等级取 `buffedLvl()`**，与 `BattleAxe` / `DaCapo` / `BlackSwan` 等原版武器一致
   （`Item.buffedLvl()` 还会额外扣掉 `Degrade` 的减级）。负等级（被诅咒的武器）会让概率低于起点，
   用 `Math.max(0f, …)` 兜底截到 0。别拿裸 `level()` 字段或 `heroClass` 之类当替身。
3. **面板文本必须动态算**：本作武器的文本入口是 `MeleeWeapon.statsInfo()`（默认实现
   `Messages.get(this, "stats_desc")`）。不覆写就永远显示写死的 `+0` 值，**强化了也看不出概率在涨**。
   照 `BlackSwan` 的范式覆写并传参：

```java
@Override
public String statsInfo() {
    return Messages.get(this, "stats_desc", Math.round(procChance(buffedLvl()) * 100));
}
```

```properties
# 字面百分号必须写成 %%，数值占位符是 %d
…命中时有_%d%%_概率…
```

- `%` 少写一个 ⇒ `String.format` 抛 `IllegalFormatException` ⇒ `Messages.format` 兜底
  **整串回退成资源键名**（游戏里表现为面板直接显示一串键名，而且不报错）。
- 显示口径用 `Math.round`：`+0` 的 12.5% 显示成 **13%**（`(int)` 截断会显示 12%）。
- 未鉴定状态若要单独文案，照 `CommonWorkC` / `Crossbow` 补一个 `typical_stats_desc` 键走
  `levelKnown` 分支；想省键就照 `BlackSwan`「不区分鉴定状态」。

### 要补的核验

**按源码里的三个常量把整条曲线复算出来**，而不是只断言常量字面量（后者对公式改动完全免疫）：
`+0` 恰为起点值 / `+0→+30` **严格递增** / 任何等级都 `< 上限`（渐近，不是封顶）/
增幅**单调递减**（证明是指数趋近）/ 足够高的等级贴近上限 / 若干实用等级落在设计区间。
再加：旧的裸定值常量**已彻底退场**（否则漏改一处会「半生效」）、喂的是 `buffedLvl()`、
文本含 `%d%%` 占位符且起点/上限值与常量一致。
反例自测至少覆盖三种错法：起点写错、`DECAY = 1`（永不成长）、`DECAY = 0`（一步到顶的「伪成长」）。
