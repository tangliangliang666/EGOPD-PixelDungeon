<!-- 本文件由 AGENTS.md §7 逐字迁出（§7）。 -->
<!-- 保留原节号，旧文里的“见 §7”仍可解析；索引见 docs/INDEX.md。 -->

# 自定义武器/物品开发经验（原 AGENTS.md §7）

> 定位：`python _chk/docfind.py <关键词>`；或 `grep -n` 本文件。
> 本文件是长尾原文，**按需定点读取，不要整读**。

## 7. 自定义武器/物品开发经验（通用）

> 原 §7 中环指大师专属的"人体派作品武器 14 把/素材/大师指环合成/击杀掉落"大段已移至 docs/ring-master.md（本节的通用条目与"装备中物品不可加入合成/强化/分解"防复制修复保留）。

- **新增武器创作指南**：手动创建普通新武器的步骤式流程（类文件/空位图标/绘制/消息键 zh+en/入掉落池/编译验证/归档）见 docs/weapon-creation-guide.md；纯面板+图标可自行完成，特殊功能（引擎钩子类效果）交 AI 实现并必须记录实现位置。

- **武器类**：继承同阶/同武技的原版武器类即可"套用武技配置"（如 `RedEye extends Cudgel`、`LingeringScent extends Crossbow`），`tier` 在实例块覆写；`min/max` 显式给出公式；`DLY`（延迟）/`RCH`（距离）/`ACC`（命中）/`hitSound` 在实例块覆写。武技文本沿继承链回退到父武器。
- **入掉落池**：`Generator.java` 的 `WEP_T2/3/4.classes` + `defaultProbs` 显式列表（prob=2 与原版同级武器同权，胎儿博士为 1）。
- **物品图标**：`ItemSpriteSheet` 用 `xy(行,列)`（1 基）+ `assignItemRect(常量,宽,高)`；非 16×16 用实际尺寸。贴图未画/画错位置→游戏里显示空白（可用 PowerShell System.Drawing 逐像素扫描验证）。
- **物品显示常驻粒子**（如附魔光效）：重写 `Item.emitter()` 返回 `new Emitter()` + `pos(偏移)` + `fillTarget=false` + `pour(粒子工厂, 间隔秒)`——`ItemSprite` 会在地面/背包/快捷栏显示时自动挂载并管理生命周期（`killAndErase`）。参考 `SpiritBow` 箭矢的树叶粒子。
- **受击特效**：命中时在目标身上播放的粒子，触发点放 `Char.defenseProc`（目标自身方法，攻击链最前），`enemy` 参数即攻击者（`enemy instanceof Hero` 后查其 `belongings.weapon()/secondWep()`）。圣宣蝴蝶即此方案成功。
- **装备时特效**：`Item.activate(Char)` 挂 buff（equip 时调用，副手也调）、覆写 `doUnequip` 卸下时移除；buff `act()` 里用常驻发射器按血量/状态调整 `emitter.start(factory, interval, 0)`。
- **等待动作钩子**：`Hero.rest(boolean fullRest)`（余香"等待获得花矢"）；**飞镖耐久**：`TippedDart.decrementDurability()` 覆写（触发一次免费使用后移除 buff）。
- **伤害浮字图标**：`Char.damage` 按 `src.getClass()` 决定图标（`AntiMagic.RESISTS` → `MAGIC_DMG` 等）；按击切换可用武器上的静态标记：`Char.attack` 置位 → `Char.damage` 图标判断 → 伤害结算后复位。法杖类伤害=直接 `ch.damage()` 绕过护甲（无"伤害类型"系统），武器等效实现为 `dr=0`。
- **自定义 buff 图标**：`BuffIndicator.HEART_FATE+1`（花矢=93）等连续编号，需先在 buff_icons.png 绘制。
- **消息格式**：氛围文本（`desc`）与功能性文本（`stats_desc`/`typical_stats_desc` 等）**分开写入**；带占位符的 `stats_desc` 用 `_%1$d_` 且**字面 % 必须 `%%`**；`typical_stats_desc` 供未鉴定时显示（可覆写 `statsInfo()` 不区分鉴定状态）。
- **粒子类**：自定义 `PixelParticle` 子类（模型参考 `HeartFateParticle`/`PinkParticle`）；可复用 `Assets.Effects.SPECKS` 贴图（7×7 帧网格，`TextureFilm(texture,7,7)`，帧16/17=黑/白蝴蝶已绘制）。
- **字母制等级显示**（验证完成，参考 `GradeTestSword extends Shortsword`；已正式用于人体派作品武器，基类 `BodyArtWeapon.levelDisplay`）：只需覆写 `Item.levelDisplay(int buffedLvl)` 返回映射字符串（如 0→"F"、4→"A-"、6→"A+"），**无需动字体/渲染**（pixel_font 已含字母与 `+`/`-`）。要点：①数值/贴图/武技全继承父武器（短剑）即可套用；②负等级（降级）建议回退 `super.levelDisplay()` 走默认 `-N`，因字母制难表达负数；③超出映射区间封顶或回退默认；④未鉴定时 `buffedVisiblyUpgraded` 返回 0，会触发 levelDisplay(0) 显示"F"——会泄露"这是字母制武器"，正式版需在 levelDisplay 里按 `levelKnown` 区分（验证场景可接受）；⑤右下角格子与**名称后缀**（`Item.title()`）都已走 levelDisplay 钩子，自动一致；仍用数字的显示点：WndUpgrade 升级界面（各自 `"+N"` 拼接），需要时另行同步。

- **装备中物品不可加入合成/强化/分解**（2026-09-01 防 bug 修复）：`WndBag`（经 `GameScene.selectItem` 打开）**会展示英雄已装备的 weapon/armor/artifact/misc/ring**（[WndBag.java#L243](file:///d:/PD/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndBag.java#L243) 的 `placeItems`）。若 `itemSelector.itemSelectable` 仅检查 `instanceof` 不排除装备中物品，则已装备武器（如初始 试作-解体刀 BodyArtWeapon）可被选中 → `onSelect` 里 `item.detach(backpack)` 因装备中物品不在背包返 null → 走 fallback `detachAll`+`material=item` → **物品既在装备槽又在合成格=复制**。**修复**：三个窗口的 `itemSelector.itemSelectable` 首行统一加 `if (item.isEquipped(Dungeon.hero)) return false;`（`KindOfWeapon.isEquipped` 检 `belongings.weapon()/secondWep()==this`、`Armor` 检 `belongings.armor()==this` 等，各装备类各自覆写）。要强化/分解已装备武器须先卸下再放入。

- **引擎钩子类武器效果三例（2026-09-06 四阶E.G.O 拘束/盲目/渴望，实现位置备忘）**：
  ①「盲目」装备时视野+1：通用标记 buff `actors/buffs/ExtraVision`（无行为），生效点 = `Level.updateFieldOfView` 的 Hero 视野分支（`ShadowCaster.castShadow` 前，视野重算的唯一消费点，天然覆盖换层/火炬等重置场景）；武器 `activate(Char)` 挂载、覆写 `doUnequip` 移除。
  ②「渴望」装备时流血转化：特判位于 `Char.damage` 的护盾吸收段（`ShieldBuff.processDamage` 之前）：`src instanceof Bleeding && this==hero && buff(Thirst.ThirstPact)!=null` → 绕过护盾直接扣血（`HP -= min(dmg, HP-1)`，保底 1 血不致死）+ `Buff.affect(this,Barrier.class).incShield(dmg)`（奥术屏障=Barrier）。特判须放在 WarriorShield 触发之后、`int shielded=dmg` 声明处重构为 if/else。
  ③「盲目」无法偷袭 = `Hero.canSurpriseAttack` 加 `w instanceof Blindness`（同 Flail 的集中判定，勿在武器类里覆写）；渴望命中流血 = proc 内对双方 `Buff.affect(x, Bleeding.class).set(round(0.2×damage))`。
  - 装备型被动 buff 通用写法：`activate` 挂 + `doUnequip` 卸 + 若需兜底可在 `act()` 里自校验装备仍在（参考 `DimDusk.DimDuskAura`）。buff 类与引擎文件撞名时（如 Level 已用 buffs.Blindness）把标记 buff 独立放 `actors/buffs` 包避免 import 冲突。
  - CENSORED（五阶）已于 2026-09-06 补入 Generator `WEP_T5`（prob 2）。

