# 附魔实现学习文档（武器附魔 / 护甲雕文）

> **用途**：新增自定义附魔前的调研结论汇总。涵盖原版附魔的实现方式、附魔强度（含奥术戒指）的互动机制、光效显示、buff 型附魔写法、注册链路与文本键规范，并附两个待实现附魔（欲望 / 蜚蠊）的落地设计草案。
>
> **状态**：本文件为**调研文档**，对应代码尚未实现。实现落地后请把"设计草案"一节改为"实现记录"，并同步 `docs/features.md`。
>
> 撰写日期：2026-09-10

---

## 0. 一句话结论（先看这个）

| 问题 | 结论 |
|---|---|
| 新附魔写在哪 | `core/.../items/weapon/enchantments/`，`extends Weapon.Enchantment` |
| 必须实现什么 | `proc(Weapon, Char, Char, int)` 与 `glowing()` 两个抽象方法 |
| 附魔强度在哪拿 | `procChanceMultiplier(attacker)` → 内含 `RingOfArcana.enchantPowerMultiplier` 等 9 项加成 |
| 光效怎么显示 | 覆写 `glowing()` 返回 `ItemSprite.Glowing(color[, period])`，物品图标脉冲高亮该色 |
| 附带 debuff | 直接 `Buff.affect(defender, Cripple.class, 10f)`，无需自定义 buff |
| 需要"随时间积累"的效果 | 仿 `Kinetic.ConservedDamage`：自定义一个内部/顶层 Buff 覆写 `act()` |
| 新附魔要不要登记 | **要**——不入 `Weapon.Enchantment.common/uncommon/rare` 就不会出现在随机池/卷轴三选一/图鉴 |
| 本项目现状 | **尚无任何自定义附魔**，这是首创；但已有可复用的先例（`Binding` 武器的束缚→扎根逻辑） |

---

## 1. 附魔体系的三层结构

| 层 | 基类 | 位置 | proc 签名 |
|---|---|---|---|
| 武器附魔 | `Weapon.Enchantment` | `items/weapon/Weapon.java:547` | `proc(Weapon weapon, Char attacker, Char defender, int damage)` |
| 武器诅咒附魔 | 同上（`curse()` 返回 true） | `items/weapon/curses/` | 同上 |
| 护甲雕文 | `Armor.Glyph` | `items/armor/Armor.java:792` | `proc(Armor armor, Char attacker, Char defender, int damage)` |

两者结构高度对称：都有 `common/uncommon/rare/curses` 四个类数组 + `typeChances = {50, 40, 10}`（→ 12.5% / 6.67% / 3.33% 单种）、都有 `random*()` 工厂、都有 `procChanceMultiplier()`、都有 `glowing()`、都实现 `Bundlable`（**默认空实现，附魔自身不存字段**）。

> 本项目的护甲雕文目录 `items/armor/glyphs/` 也全是原版，13 种。本次需求是**武器附魔**，护甲雕文仅在需要时参考。

### 1.1 原版武器附魔清单（13 种 + 8 诅咒）

| 类 | 分组 | 光效色 | 触发效果 | 用到的 buff |
|---|---|---|---|---|
| `Blazing` | common | `0xFF4400`（橙） | 点燃 8 回合 + 溢出转直伤 | `Burning` |
| `Chilling` | common | `0x00FFFF`（青） | 3 回合冻伤，上限 6 回合 | `Chill` |
| `Kinetic` | common | `0xFFFF00`（黄） | 击杀溢出伤害储存，下次攻击释放 | `KineticTracker` / `ConservedDamage` |
| `Shocking` | common | `0xFFFFFF`，period 0.5 | 电弧连锁邻近敌人 | — |
| `Blocking` | uncommon | `0x0000FF`（蓝） | 攻击后概率得护盾 | `Blocking.BlockBuff` |
| `Blooming` | uncommon | `0x008800`（绿） | 催生植物/高草 | — |
| `Elastic` | uncommon | `0xFF00FF`（品红） | 击退 | — |
| `Lucky` | uncommon | `0x00FF00`（亮绿） | 击杀额外掉落 | — |
| `Projecting` | uncommon | `0x8844CC`（紫） | 近战 +1 射程 | — （无 on-hit 效果） |
| `Unstable` | uncommon | `0x999999`（灰） | 每击视作随机另一种附魔 | — |
| `Corrupting` | rare | `0x440066`（暗紫） | 击杀概率腐化 | `Corruption` |
| `Grim` | rare | `0x000000`（黑） | 低血概率斩杀 | `Grim.GrimTracker` |
| `Vampiric` | rare | `0x660022`（暗红） | 吸血 | — |

诅咒（`curse()`==true，光效全为 `0x000000`，除 `Explosive` 带 period 0.5）：
`Annoying` / `Displacing` / `Dazzling` / `Explosive` / `Sacrificial` / `Wayward` / `Polarized` / `Friendly`，全在 `items/weapon/curses/`。

---

## 2. 核心基类 `Weapon.Enchantment`

源码：`core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/weapon/Weapon.java:547-710`

```java
public static abstract class Enchantment implements Bundlable {

    public static final Class<?>[] common   = {Blazing.class, Chilling.class, Kinetic.class, Shocking.class};
    public static final Class<?>[] uncommon = {Blocking.class, Blooming.class, Elastic.class,
                                               Lucky.class, Projecting.class, Unstable.class};
    public static final Class<?>[] rare     = {Corrupting.class, Grim.class, Vampiric.class};
    public static final Class<?>[] curses   = {Annoying.class, Displacing.class, ...};
    public static final float[] typeChances = {50, 40, 10};

    public abstract int proc( Weapon weapon, Char attacker, Char defender, int damage );

    protected float procChanceMultiplier( Char attacker );              // → 见第 3 节
    public static float genericProcChanceMultiplier( Char attacker );   // 静态版，供非附魔代码调用

    public String name();                                  // "附魔名 + 武器名"
    public String name( String weaponName );
    public String desc();                                  // 从 messages 读
    public boolean curse();                                // 默认 false
    public abstract ItemSprite.Glowing glowing();

    public static Enchantment random(...);                 // 按 typeChances 抽 组
    public static Enchantment randomCommon(...);
    public static Enchantment randomUncommon(...);
    public static Enchantment randomRare(...);
    public static Enchantment randomCurse(...);
}
```

要点：

1. **`proc` 返回 int**——是"伤害修正"链的一环，返回 `damage` 表示不改变本次伤害；返回 `damage + n` 加伤。**注意：附加 debuff 类附魔只做副作用，最后 `return damage;`**（Chilling/Blazing/Blooming/Elastic 都是这样）。
2. **`name()` 的字符串拼接**：非诅咒时 `name(Messages.get(this, "enchant")) + weaponName`，即 `%s` 占位在武器名前；诅咒时用 `Messages.get(Item.class, "curse")`（"诅咒的"）。
3. **`Bundlable` 默认空实现**——附魔本身不含字段时无需覆写。**一旦附魔要存字段（如积累层数），必须覆写 `storeInBundle`/`restoreFromBundle`**。
4. `glowing()` 不可为 null（`Weapon.glowing()` 会直接返回它）。

---

## 3. 附魔强度：`procChanceMultiplier` 与奥术戒指

### 3.1 调用链

```
Enchantment.procChanceMultiplier(attacker)          // protected，实例方法
    └─ Enchantment.genericProcChanceMultiplier(attacker)   // public static，唯一逻辑本体
           ├─ RingOfArcana.enchantPowerMultiplier(attacker)   // 基础倍率
           ├─ + Berserk rage.enchantFactor(multi)             // 战士狂怒
           ├─ + RunicBlade.RunicSlashTracker.boost            // 符文刃（消耗后 detach）
           ├─ + 3f  if Smite.SmiteTracker                     // 决斗家重击
           ├─ + ElementalStrike.DirectedPowerTracker.enchBoost// 元素打击导向（消耗后 detach）
           ├─ + 0.1f if Talent.SpiritBlades 满 4 点 + tracker
           ├─ + 0.2f if Talent.StrikingWave 满 4 点 + tracker
           ├─ + FuriosoReplica.EnchantBoostTracker.boost      // 神的技艺（本项目）
           └─ + 0.6f（烙印工坊 BrandWorkshop）/ +2f（蜡翼 WaxWing）且当前武器带烈焰附魔（本项目）
```

`procChanceMultiplier` 是 **protected 实例方法**，只能在自己附魔的 `proc()` 内调用；`genericProcChanceMultiplier` 是 **public static**，任何代码（如 `Weapon.proc` 里的圣宣补刀、`Char.damage` 里的恒动结算）都能用。

### 3.2 奥术戒指（Ring of Arcana）

`items/rings/RingOfArcana.java:60`：

```java
public static float enchantPowerMultiplier(Char target) {
    return (float)Math.pow(1.175f, getBuffedBonus(target, Arcana.class));
}
```

- 每 +1 强化等级 → ×1.175（即 **+17.5%**）；戒指面板显示的就是 `100 × (1.175^(lvl+1) − 1) %`。
- 对**全部**武器附魔与护甲雕文统一生效（走同一个 `genericProcChanceMultiplier`）。
- **诅咒戒指**：`upgradeStat1()` 里 `if (cursed) level = Math.min(-1, level-3)` → 负等级让倍率 < 1。

### 3.3 强度如何被附魔消费（原版两种范式）

**范式 A：强度 → 触发概率（最常见）**

```java
int level = Math.max( 0, weapon.buffedLvl() );
// lvl 0 = 25%、lvl 1 = 40%、lvl 2 = 50% ...（不同附魔分母不同）
float procChance = (level+1f)/(level+4f) * procChanceMultiplier(attacker);
if (Random.Float() < procChance) {
    float powerMulti = Math.max(1f, procChance);   // ★ 概率同时兼任"强度"
    ...  // 用 powerMulti 放大效果量（回合数 / 伤害）
}
return damage;
```

- `Chilling`：`durationToAdd = 3f * powerMulti`，且**总量上限也用 powerMulti 放大**（`6f*powerMulti`）。
- `Blazing`：先点燃（吃 1 点 powerMulti），剩余 `powerMulti` 转成直伤（`burnDamage × 0.67f × powerMulti`）。

**范式 B：强度 → 伤害（不做概率判定）**

- `Kinetic` 的储存伤害在 `Char.damage` 里乘 `genericProcChanceMultiplier`（`Char.java:1175`）。
- `Weapon.reachBonus()` 也用 `genericProcChanceMultiplier` 加射程（`Weapon.java:366`）。

> **本项目落地建议**：需求写的是"**有概率**给对面上束缚，**附魔强度够高的话**会上扎根"——天然对应范式 A：`procChance` 决定是否触发，`powerMulti`（或 `procChance` 本身）决定是否升级为扎根。阈值可定义为 `powerMulti >= X` 或 `procChance >= Y`。

---

## 4. `proc` 的实际调用链（附魔生效时机）

### 4.1 完整链路

```
Char.attack(enemy)
  ├─ enemy.defenseProc(this, damage)         // 含护甲雕文 proc（Char.java:932）
  ├─ effectiveDamage = attackProc(enemy, dmg)
  │      └─ Hero.attackProc (Hero.java:1640)
  │             ├─ Talent.onAttackProc(...)
  │             ├─ wep.proc(this, enemy, damage)      ← ★ 武器附魔在这里 (Hero.java:1653)
  │             │      └─ Weapon.proc (Weapon.java:137)
  │             │             ├─ 松脂涂层分支：涂层附魔 proc 覆盖原附魔
  │             │             ├─ 圣宣分支：额外加伤
  │             │             ├─ 普通分支：enchantment.proc(...)   ← ★ 你的附魔
  │             │             ├─ 三一魂附（BodyForm）附加附魔 proc
  │             │             └─ Smite 补刀
  │             └─ Sniper / 天赋追加逻辑
  └─ enemy.damage(effectiveDamage, this)
         └─ Char.damage：恒动 KineticTracker 结算（Char.java:1171）
```

其他入口（也走 `proc`）：
- 影分身 `ShadowClone.attackProc` → `Dungeon.hero.belongings.weapon().proc(this, enemy, damage)`（`ShadowClone.java:222`）
- 天赋 `Talent.java:1431`：`bow.proc(hero, enemy, dmg)`（灵弓）
- 元素打击 `ElementalStrike.java:447`：`ench.proc((Weapon) w, hero, ch, w.damageRoll(hero))`
- 神的技艺 `FuriosoReplica.java:163`：`new Unstable().proc(...)`

### 4.2 `Weapon.proc` 的三个分支（`Weapon.java:137-198`）

1. `attacker.buff(MagicImmune.class) != null` → **整段跳过**（抗魔时附魔不触发）。**新附魔的副作用也建议照抄这个门控**：让抗魔单位不吃 debuff。
2. **松脂涂层**（`CharcoalResinBuff` / `GoldenResinBuff`）：用涂层附魔 **取代**武器原附魔。
3. **圣宣**（`HolyWeapon.HolyWepBuff`）：圣骑士职业或诅咒附魔时才触发原附魔。
4. **普通**：直接 `enchantment.proc(this, attacker, defender, damage)`。

> **重要**：`Weapon.proc` 会在 `proc` 返回后再检查 `defender.alignment == ALLY` 是否被改变（腐化类附魔把敌人变成盟友时中断后续）。附加 debuff 的附魔不需要关心这一点。

### 4.3 `BodyArtWeapon` 的附魔叠加（本项目特有）

`items/weapon/melee/BodyArtWeapon.java:99-108`：主附魔结算后，**依次**结算 `extraEnchants` 列表：

```java
@Override
public int proc( Char attacker, Char defender, int damage ) {
    int dmg = super.proc( attacker, defender, damage );
    for (Weapon.Enchantment e : extraEnchants.toArray( new Weapon.Enchantment[0] )){
        dmg = e.proc( this, attacker, defender, dmg );
    }
    return dmg;
}
```

- `extraEnchants` 存**类名数组**存档，读档用 `Reflection.newInstance`（`BodyArtWeapon.java:56-91`）。
- **含字段的附魔若被叠加到人体派作品上，字段不会随武器存档保存**（只存类名！）。这是既有设计的限制，新附魔若带状态需要特别注意——要么把状态放进**角色身上的 buff**（推荐，buff 自己有存档），要么扩展 `BodyArtWeapon` 的序列化。

---

## 5. 光效显示（`ItemSprite.Glowing`）

### 5.1 数据结构

`sprites/ItemSprite.java:552-574`：

```java
public static class Glowing {
    public int color;
    public float red, green, blue;   // 从 color 拆出
    public float period;             // 脉冲周期（秒），默认 1f
    public Glowing( int color ) { this(color, 1f); }
    public Glowing( int color, float period ) { ... }
}
```

### 5.2 渲染方式

`ItemSprite.draw()`（`ItemSprite.java:522-541`）：可见且 `glowing != null` 时，按 `phase / period` 计算 `value ∈ [0, 0.6]`，把物品图标的 RGB 乘子压到 `1-value`，再叠加 `glowing.color × value` —— 即 **周期性把图标整体染色并向该色脉冲**。`period` 越小脉冲越快。

### 5.3 门控：什么时候显示

`Weapon.glowing()`（`Weapon.java:540-543`）：

```java
if (... 帕拉丁圣剑特例 ...) return HOLY;
else return enchantment != null && (cursedKnown || !enchantment.curse()) ? enchantment.glowing() : null;
```

即：**未鉴定的诅咒武器不显示光效**，其余情况按附魔色显示。所以新附魔只需保证 `glowing()` 返回非 null 实例即可；无需自己处理可见性。

### 5.4 推荐做法

照抄原版模式，定义 `private static final ItemSprite.Glowing XXX = new ItemSprite.Glowing( 0xRRGGBB );` 作为**静态单例**，`glowing()` 直接返回它。**不要每次 new**（会破坏渲染缓存的同步语义，且原版全部是静态字段）。

⚠️ 冲突提醒：挑色时避开上表已占颜色，尤其是 common 组（橙 `FF4400` / 青 `00FFFF` / 黄 `FFFF00` / 白 `FFFFFF`）。

---

## 6. buff 型附魔写法（"随时间积累"必读）

`Kinetic` 是唯一的原版先例，也是**蜚蠊附魔的直接模板**。

### 6.1 Kinetic 的两个 Buff

| Buff | 角色 | 关键实现 |
|---|---|---|
| `Kinetic.KineticTracker` | **一次性标记**，用于在 `Weapon.proc` 里"登记本次攻击"，好让 `Char.damage` 知道"这一击是恒动武器打的" | `actPriority = Actor.VFX_PRIO`，`act()` 里立刻 `detach(); return true;` |
| `Kinetic.ConservedDamage` | **真正的储存状态**，挂在攻击者身上 | `act()` 每回合衰减；`iconTextDisplay()` 显示数值；`tintIcon()` 按数值变色；`setBonus()/damageBonus()` |

`ConservedDamage` 关键代码（`Kinetic.java:73-146`）：

```java
public static class ConservedDamage extends Buff {
    { type = buffType.POSITIVE; }
    @Override public int icon() { return BuffIndicator.WEAPON; }      // 复用武器图标
    @Override public String iconTextDisplay() { return Integer.toString(damageBonus()); }
    private float preservedDamage;
    public void setBonus(int bonus){ preservedDamage = bonus; }
    public int damageBonus(){ return (int)Math.ceil(preservedDamage); }
    @Override public boolean act() {
        preservedDamage -= Math.max(preservedDamage*.025f, 0.1f);   // 每回合衰减
        if (preservedDamage <= 0) detach();
        spend(TICK); return true;
    }
    public void delay( float value ){ spend(value); }
    // 存档：PRESERVED_DAMAGE 键；restore 时若缺键用 cooldown()/10 兜底
}
```

### 6.2 积累型 buff 的三条路径

| 模式 | 触发时机 | 原版先例 |
|---|---|---|
| **每回合积累** | `act()` 里 `count += X; spend(TICK); return true;` | 无原版武器附魔先例，但 `Charge`/`MonkEnergy` 等 buff 都是这个写法 |
| **击杀时积累** | `Char.damage` 检测 `KineticTracker` | `Kinetic` |
| **每次攻击积累** | `proc()` 里给攻击者 `Buff.affect(attacker, XXX.class).add(1)` | `PalermoSwordBuff`（本项目连斩） |

> **蜚蠊附魔需求"随着时间进行会积累"→ 用模式一**：自定义 `RoachTracker` buff，挂攻击者身上，`act()` 每回合 +N 层，设上限；`proc()` 时读出层数、转成额外伤害、清空层数。

### 6.3 Buff 的关键钩子清单

`actors/buffs/Buff.java`：

| 方法 | 行 | 用途 |
|---|---|---|
| `act()` | 89 | 默认 `diactivate()`（**只走一次就消失**）。要持续/每回合生效必须覆写并 `spend(TICK); return true;` |
| `icon()` | 94 | 返回 `BuffIndicator.XXX` 常量 |
| `tintIcon(Image)` | 99 | 图标染色（按层数变色） |
| `iconFadePercent()` | 104 | 图标环形进度（剩余时长比例，FlavourBuff 常覆写） |
| `iconTextDisplay()` | 109 | 图标角标数字 |
| `desc()` | 131 | 从 messages 读，可传参 |
| `attachTo(Char)` | — | 返回 false 可阻止附加（`Roots` 用来对飞行单位免疫） |
| `detach()` | — | 清理副作用（`Roots` 里复位 `target.rooted`） |

`FlavourBuff` 是带**剩余时长**的子类，`visualcooldown()` 返回剩余回合。

### 6.4 `Buff.affect` 的两个重载（`Buff.java:175-188`）

```java
public static <T extends Buff> T affect( Char target, Class<T> buffClass )                  // 任意 Buff，不带时长
public static <T extends FlavourBuff> T affect( Char target, Class<T> buffClass, float d ) // 仅 FlavourBuff，带时长
```

- 有**固定时长**的 debuff（`Cripple` / `Roots` / `Chill` / `Paralysis` / `Burning` 都是 FlavourBuff）→ 用 3 参版本：`Buff.affect(defender, Cripple.class, 10f)`。
- **非 FlavourBuff**（自定义积累 buff、`Kinetic.ConservedDamage` 这种自己管 `act()` 的）→ 只能用 2 参版本：`Buff.affect(attacker, RoachTracker.class)`，然后自己 `.setXxx()`。
- 想要"刷新/延长"而非"多层并存"→ 用 `Buff.prolong(...)`（见 `Chasm.java:146`）。

### 6.5 Buff 图标编号分配（**新增前必查**）

`ui/BuffIndicator.java`：连续编号，`NONE = 127`（隐藏图标）。

当前占用（**2026-09-10 错位修正后**实况）：`0 ~ 110` 已被占用，**最大为 `PALERMO_SWORD = 110`**。

- 本项目新增的 **`ATTACK_VERMIN = 111`**（蜚蠊附魔「攻击害虫」）即下一个空号，**帧待用户绘制**。
- **其余可用空号**：`112 ~ 126`；另有历史跳号 `93`、`94`（`HEART_FATE=92` 与 `INSPIRATION=95` 之间）。
- 图标实际帧必须已在 `buffs.png` / `large_buffs.png` 中绘制，否则显示空/错位。
- **网格规格（Pillow 实测，勿凭记忆）**：小图 `buffs.png` 128×64 → `SIZE_SMALL=7`，**18 列 × 9 行**（162 帧）；大图 `large_buffs.png` 256×128 → `SIZE_LARGE=16`，**16 列 × 8 行**（128 帧，127 正好是 `NONE`）。两图**各自独立排版**，同一索引在两张图里落在不同行列。
- **🐞 2026-09-10 错位修正实录**：「瞄准心脏 / 焦炭松脂 / 黄金松脂 / 连斩」4 个图标**实际画在帧 107/108/109/110**，而代码写的是 108/109/110/111（旧注释"107 决定跳过"与贴图不符），整体多算 1 格 → 已在 `BuffIndicator` 统一 **-1**。

> 两个新附魔的图标需求：欲望复用现有 `CRIPPLE(23)` / `ROOTS(11)`（debuff 挂在敌人身上，原生图标即可，**不需要新帧**）；蜚蠊的「攻击害虫」需要专属帧，已取号 **111**。

### 6.6 buff 文本键命名

- 附魔的**内部类**：`items.weapon.enchantments.<附魔类名小写>$<内部类名小写>.{name,desc}`
  例：`items.weapon.enchantments.kinetic$conserveddamage.name=伤害储存`
- **独立顶层** buff：`actors.buffs.<类名小写>.{name,desc}`
  例：`actors.buffs.cripple.name=残废`

### 6.7 存档坑（本项目已踩过，务必遵守）

1. `Bundle.getFloat/getInt` 对**缺失键返回 0** → 读档字段必须给安全默认值（`Kinetic` 的 `preservedDamage` 兜底写法可参考）。
2. **带时长的惩罚类独立 buff 必须放独立顶层类**（`extends FlavourBuff`），否则存读档即丢失。
3. `ArtifactBuff` 类内部类无 no-arg 构造时会被静默跳过——自定义 buff 若要塞进 `Char.buffs` 并被正常恢复，**保证有无参构造**（匿名/静态内部类默认可）。

---

## 7. 束缚 / 扎根类 debuff 现有实现

> 需求中的"束缚""扎根"在项目里已有对应物，**不需要新造 buff**（除非要让它们带有全新语义）。

| 中文名 | 类 | 时长常量 | 效果 | 图标 | 源码 |
|---|---|---|---|---|---|
| 残废 | `Cripple` | `DURATION = 10f` | `Char.speed()` 里 `speed /= 2f`（移动速度减半） | `BuffIndicator.CRIPPLE = 23` | `actors/buffs/Cripple.java` |
| 缠绕 / 扎根 | `Roots` | `DURATION = 5f` | `attachTo` 里 `target.rooted = true` → **禁移动**（飞行单位免疫；`detach` 复位） | `BuffIndicator.ROOTS = 11` | `actors/buffs/Roots.java` |
| 迟缓 | `Slow` | `DURATION = 10f` | 减速（`TIME` 图标） | `BuffIndicator.TIME = 7` | `actors/buffs/Slow.java` |

**命名冲突提示**：项目里 `Roots` 的 buff 名文本是"**缠绕**"（`actors.buffs.roots.name=缠绕`），但 `sandalsofnature.ac_root` 与 `Binding` 武器的文案用了"**扎根**"。两个叫法混用，写新文案时建议统一为"扎根"（与本次需求一致），必要时只改文案键，不动类名。

### 7.1 强参考先例：`Binding`（拘束）武器

`items/weapon/melee/Binding.java:66-97` —— **诉求几乎完全相同**（攻击时施加束缚、进阶为扎根），可直接把 `restrain()` 逻辑搬成附魔：

```java
@Override
public int proc(Char attacker, Char defender, int damage) {
    damage = super.proc(attacker, defender, damage);
    if (attacker.buff(MagicImmune.class) == null && defender.isAlive()){
        restrain(attacker);
        if (defender.isAlive()) restrain(defender);
        if (defender.isAlive() && defender.buff(Roots.class) != null
                && Random.Int(100) < 20 + buffedLvl()){
            Buff.affect(defender, Paralysis.class, 3f);
        }
    }
    return damage;
}

private static void restrain(Char ch){
    if (ch.buff(Cripple.class) != null){
        Buff.detach(ch, Cripple.class);
        Buff.detach(ch, Roots.class);
        Buff.affect(ch, Roots.class, 5f);       // 已有束缚 → 进阶扎根（刷新，不叠加）
    } else {
        Buff.affect(ch, Cripple.class, 10f);    // 无束缚 → 施加束缚
    }
}
```

区别只在：`Binding` 是**武器固定效果**（必触发），而需求要的是**附魔**（概率触发 + 强度门控）。把 `restrain` 的调用放进 `if (Random.Float() < procChance)` 内、并用 `powerMulti` 决定是否升级即可。

⚠️ `Buff.detach(ch, Roots.class)` 后再 `affect` 是"刷新"语义；若想"叠加/延长"改用 `Buff.prolong(ch, Roots.class, 5f)`。`Roots` 对**飞行单位免疫**（`attachTo` 返回 false），无需自己判飞行。

---

## 8. 新增附魔必须登记的 6 个点

| # | 位置 | 必要性 | 说明 |
|---|---|---|---|
| 1 | `Weapon.Enchantment.common` / `uncommon` / `rare` | **必须**（否则只能靠代码硬给） | 决定随机分布、`ScrollOfEnchantment` 三选一、图鉴收录 |
| 2 | 新建类文件 `items/weapon/enchantments/Xxx.java` | 必须 | 实现 `proc` + `glowing` |
| 3 | `items_zh.properties` + `items.properties` | 必须 | `.name` / `.desc`（zh/en 双份） |
| 4 | `EnchantArt.RECIPES`（`actors/hero/arts/EnchantArt.java:83`） | 视需要 | 艺术之巅「附魔」技艺的配方物品→附魔映射；`enchantTier()` 自动按分组返回 1/2/3，**放进数组即自动支持**，无需另配 |
| 5 | `Unstable.randomEnchants`（`enchantments/Unstable.java:34`） | 视需要 | 想让"紊乱"附魔能随机到新附魔就把类加进这个数组 |
| 6 | `ElementalStrike.effectTypes`（`abilities/duelist/ElementalStrike.java:105`） | 视需要 | 决斗家"元素打击"的锥形特效类型。**未注册不会崩**——`effectTypes.get(enchCls)` 返回 null，`MagicMissile.reset(null, ...)` 走默认 `MAGIC_MISS_CONE`，只是没有专属颜色特效 |

自动生效、无需改动的部分：

- **图鉴**：`journal/Catalog.java:204-207` 用 `ENCHANTMENTS.addItems(Weapon.Enchantment.common/uncommon/rare/curses)` → 进数组即自动进图鉴。
- **附魔卷轴**：`ScrollOfEnchantment.java:128-130` 直接调 `randomCommon/randomUncommon/random` → 进数组即自动进入三选一。
- **应用对象限制**：`ScrollOfEnchantment.enchantable()` 已放行 `Weapon`/`Armor`/`SpiritBow`/`BodyArtWeapon`（钻石剑除外）。

---

## 9. 文本键规范

在 `core/src/main/assets/messages/items/items_zh.properties`（及 `items.properties`）中：

```properties
items.weapon.enchantments.<附魔类名小写>.name=<附魔名>%s
items.weapon.enchantments.<附魔类名小写>.desc=<一句话效果说明>
items.weapon.enchantments.<附魔类名小写>.elestrike_desc=<元素打击专属说明，可选>
items.weapon.enchantments.<附魔类名小写>$<内部buff小写>.name=<buff 名>
items.weapon.enchantments.<附魔类名小写>$<内部buff小写>.desc=<buff 说明>
```

规则与坑：

1. **`.name` 里的 `%s` 是武器名占位符**（`Enchantment.name(weaponName)` 会 format 它），删掉会抛 `MissingFormatArgumentException`。若不想显示武器名，可改为不含 `%s` 的纯名称。
2. 换行写 **`\n`（单反斜杠）**；写成 `\\n` 会显示字面 "\n"。自检：`grep -c '\\\\n' 文件` 应为 0。
3. 带**参数**的 `desc`（代码里 `Messages.get(cls, key, args)`）中的字面百分号必须写 **`%%`**；无参数调用裸 `%` 无害。
4. 继承规则：`Messages.get(Class, key)` 沿**父类链**查找——所以**只覆写 `glowing()` 而不写文本键会继承父类文案**（`Weapon.Enchantment` 无对应键时会显示 `!` 或原始 key）。**新附魔必须自带 name/desc**。
5. zh / en **双份同改**（项目惯例，`items.properties` 目前有 43 条 `items.weapon.enchantments.*`）。

---

## 10. 两个待实现附魔 —— 落地设计草案

> **2026-09-10 已实现**：`Desire.java` 与 `Roach.java`（见 `docs/features.md` 同日条目）。下方保留设计推演过程，**最终数值以源码为准**，与草案的差异已就地标注为"最终实现值"。

### 10.1 欲望附魔（Desire）

**需求**：攻击时有概率给对面上"束缚"；附魔强度够高时上"扎根"。

建议方案（照 `Binding.restrain` + `Chilling` 概率范式）：

```java
public class Desire extends Weapon.Enchantment {
    private static final ItemSprite.Glowing ROSE = new ItemSprite.Glowing( 0xCC0066 ); // 待定，避开已占色

    @Override
    public int proc( Weapon weapon, Char attacker, Char defender, int damage ) {
        int level = Math.max( 0, weapon.buffedLvl() );
        float procChance = (level+1f)/(level+3f) * procChanceMultiplier(attacker);  // 分母待定
        if (Random.Float() < procChance) {
            float powerMulti = Math.max(1f, procChance);
            // 强度门控：够高 → 扎根；否则 → 束缚
            if (powerMulti >= ROOT_THRESHOLD && defender.buff(Roots.class) == null) {
                Buff.affect(defender, Roots.class, ROOT_DURATION);
            } else {
                Buff.prolong(defender, Cripple.class, CRIPPLE_DURATION);
            }
            // 可选：Splash / CellEmitter 特效
        }
        return damage;
    }
    @Override public ItemSprite.Glowing glowing() { return PINK; }
}
```

**待用户确认的设计点**：

| # | 问题 | 候选 |
|---|---|---|
| 1 | "束缚"是复用 `Cripple`（残废，速度减半）还是新建 buff？ | 建议复用 `Cripple`（`Binding` 先例 + 无需新图标/新帧） |
| 2 | "强度够高"如何判定？ | `powerMulti >= 1.5` / `procChance >= 0.6` / 戒指等级阈值 |
| 3 | 束缚/扎根的叠加语义 | `prolong`（延长） vs `detach+affect`（刷新） vs `affect`（保留已有） |
| 4 | 是否对已扎根目标追加效果（`Binding` 会加麻痹） | 需求未提，默认不加 |
| 5 | 分组（common/uncommon/rare）与是否入 `EnchantArt.RECIPES` | 待定 |

### 10.2 蜚蠊附魔（Roach）

**需求**：随时间积累"攻击害虫"，下一次攻击时造成类似**恒动（Kinetic）**的额外伤害。

建议方案（照 `Kinetic.KineticTracker` + `ConservedDamage`）：

```java
public class Roach extends Weapon.Enchantment {
    private static final ItemSprite.Glowing BROWN = new ItemSprite.Glowing( 0x554433 );

    @Override
    public int proc( Weapon weapon, Char attacker, Char defender, int damage ) {
        int bonus = 0;
        RoachSwarm swarm = attacker.buff(RoachSwarm.class);
        if (swarm != null) {
            bonus = swarm.damageBonus();          // 读出积累的害虫
            swarm.detach();                       // 一次性释放
        }
        // 用 tracker 让 Char.damage 知道"这一击是蜚蠊武器"，便于结算（如击杀溢出）
        Buff.affect(attacker, RoachTracker.class).conservedDamage = bonus;
        return damage + bonus;
    }

    @Override public ItemSprite.Glowing glowing() { return BROWN; }

    // 积累 buff（挂攻击者，每回合 +N，带上限）
    public static class RoachSwarm extends Buff {
        { type = buffType.POSITIVE; }
        private float stacks;
        @Override public int icon() {
            return damageBonus() > 0 ? BuffIndicator.ATTACK_VERMIN : BuffIndicator.NONE; } // 111，需用户画帧
        @Override public String iconTextDisplay() { return Integer.toString(damageBonus()); }
        @Override public boolean act() {
            // 最终实现值：0.2×强度 / 回合，上限 10×强度
            damage = Math.min( BASE_CAP * power, damage + BASE_RATE * power );
            spend(TICK); return true;
        }
        public int damageBonus(){ return Math.round(stacks * MULT); }
        // store/restore 必须写，缺键给安全默认值 0
    }
}
```

**定稿（2026-09-10）**：

| # | 问题 | 结论 |
|---|---|---|
| 1 | 积累速率与上限 | 速度 **0.2×附魔强度 / 回合**、上限 **10×附魔强度**；无附魔强度时＝每 5 回合 1 点、最多 10 点 |
| 2 | 附魔强度从哪取 | `procChanceMultiplier(attacker)`，**在命中 proc 时取一次并写回 buff**（不在 `act()` 里取，见下） |
| 3 | 伤害换算 | 1 点害虫 = 1 点伤害（强度已作用在积累端，释放端不再重复乘） |
| 4 | 积累触发条件 | 无条件每回合积累；命中释放后**清零并继续积累**（buff 常驻，不 detach） |
| 5 | buff 图标 | 专属帧 **`ATTACK_VERMIN = 111`**（0~110 已占，待用户绘制）；仅存量 ≥1 时显示 |
| 6 | 是否需要 `KineticTracker` 式伤害钩子 | **不需要**——额外伤害直接加在 `proc` 返回值上 |
| 7 | 分组 / `EnchantArt.RECIPES` | **common** / 未登记（同欲望） |

> ⚠️ **实现踩坑（务必保留）**：不要在 buff 的 `act()` 里调用 `Weapon.Enchantment.genericProcChanceMultiplier(target)`。该函数会**消耗** `RunicBlade.RunicSlashTracker` 与 `ElementalStrike.DirectedPowerTracker`（读到即 `detach()`）——逐回合调用会把本该留给下一次攻击的天赋加成提前吃掉。正确做法：只在 `proc()` 里取一次，把值存进 buff 字段。

> ⚠️ **叠加附魔的覆盖问题**：若蜚蠊附魔作为 `extraEnchants` 叠在人体派作品上，`RoachSwarm` 若挂在**武器**上会丢存档（只存类名）；**挂角色身上（`Buff.affect(attacker, ...)`）才是安全做法**，如上草案所示。

---

## 11. 快速检查清单（实现时照打勾）

- [ ] 新类 `extends Weapon.Enchantment`，实现 `proc` + `glowing`
- [ ] `proc` 内 `MagicImmune` 门控（照 `Weapon.proc` 的分支语义）
- [ ] 触发概率用 `procChanceMultiplier(attacker)`（自动吃奥术戒指 + 全部天赋加成）
- [ ] `glowing()` 返回 `static final` 的 `ItemSprite.Glowing`，配色不与已占色冲突
- [ ] 加进 `common`/`uncommon`/`rare` 数组（否则不入随机池/图鉴/卷轴）
- [ ]（可选）`Unstable.randomEnchants`、`ElementalStrike.effectTypes`、`EnchantArt.RECIPES`
- [ ] `items_zh.properties` + `items.properties` 双份 name/desc（`%s` 保留、`\n` 单反斜杠）
- [ ] 若带状态：状态放**角色身上的 buff**，buff 写 `storeInBundle/restoreFromBundle` 并给缺键安全默认值
- [ ] 若新增 buff 图标：`BuffIndicator` 从 **111** 起取号（2026-09-10 修正后）+ 用户绘制帧
- [ ] `docs/features.md` 归档 + 当日 `memory/YYYY-MM-DD.md` 记录

---

## 12. 关键文件索引

| 文件 | 用途 |
|---|---|
| `items/weapon/Weapon.java:547` | `Weapon.Enchantment` 基类（分组数组 / proc / glowing / random*） |
| `items/weapon/Weapon.java:137` | `Weapon.proc`（附魔实际调用点 + 松脂/圣宣/三一分支） |
| `items/weapon/Weapon.java:540` | `Weapon.glowing()`（光效门控） |
| `items/weapon/enchantments/Kinetic.java` | 附魔 + 追踪器 + 积累 buff 的**完整样板** |
| `items/weapon/enchantments/Chilling.java` | 概率 + 强度放大 + debuff 的最简样板 |
| `items/weapon/melee/Binding.java:66-97` | 束缚→扎根递进逻辑（`restrain`） |
| `items/armor/Armor.java:792` | `Armor.Glyph` 基类（护甲侧对照） |
| `items/rings/RingOfArcana.java:60` | `enchantPowerMultiplier`（奥术戒指倍率公式） |
| `actors/Char.java:954` | `Cripple` 减速落点（`speed /= 2f`） |
| `actors/buffs/Roots.java` | 扎根 buff（`rooted = true`，飞行免疫） |
| `actors/buffs/Buff.java:175-188` | `Buff.affect` 两个重载 + `affect/prolong/append/count/detach` |
| `ui/BuffIndicator.java:53-180` | buff 图标编号分配表（新增从这里取号） |
| `journal/Catalog.java:204-207` | 附魔图鉴自动收录（进分组数组即生效） |
| `items/scrolls/exotic/ScrollOfEnchantment.java:99-152` | 附魔卷轴三选一（进分组数组即生效） |
| `actors/hero/arts/EnchantArt.java:83-155` | 艺术之巅「附魔」技艺的配方表与级别判定 |
| `windows/WndBodyEnchant.java` | 本项目自定义附魔窗口（配方→附魔应用） |
| `core/src/main/assets/messages/items/items_zh.properties:1813` | 附魔文本键区段 |
