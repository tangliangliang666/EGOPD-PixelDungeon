# 负数道具等级：系统支持度调研（2026-09-23）

> **为什么调研**：为接下来的「考验」（TRIALS）设计做准备 —— 若某条考验要「把道具等级压到负数」
> （如「所有起始装备 -1」），需要先确认引擎/游戏是否承受得住。
> **核验方式**：源码逐点确认 + **真跑探针**（`_chk/NegLevelProbe.java`，30 项断言全过，输出存 `_chk/_negprobe_out.txt`）。

---

## 0. 结论（先看这段）

**支持。** 数据层、存档层、UI 层**原生**支持负数等级 —— 这不是「碰巧没崩」，而是**上游 SPD 就把它当作合法状态**，
代码里有 7 处**显式**分支在为负等级服务（见 §3），连「降级后换贴图」「降级后折价」都写好了。

**但各品类的数值公式对「负等级」的语义并不统一**，分三档：

| 档 | 行为 | 代表 | 后果 |
|---|---|---|---|
| **A 显式支持** | 有明确定义（折价 / 换形象 / 中性值） | 法杖·戒指·神器·**饰品** | 可直接用 |
| **B 静默夹 0** | `Math.max(0, lvl)`，负等级 = 当 +0 用 | 力量需求·投掷武器·附魔字形·植物 | 安全，但「压负数」**没有效果** |
| **C 负向传递** | 无保护地算进公式 | **近战伤害·防具 DR·法杖充能·戒指加成** | 表现反直觉，甚至有**反向增益**（§4） |

⇒ **能做「负等级」机制，但必须逐品类决定语义并自己补夹取**；只改等级、不管品类，会得到一堆怪现象。

---

## 1. 三层支持度总览（都有实证）

| 层 | 支持 | 证据（文件:行） |
|---|---|---|
| 运行时字段 | ✅ | `Item.java:86` `private int level = 0;`；`:446-453` `upgrade()` 只 `level++`、`:463-468` `degrade()` 只 `level--` —— **全无下限夹取** |
| `upgrade(n)/degrade(n)` 的 n | ✅ 安全 | `Item.java:455-461 / 470-476` 是 `for(i=0;i<n;i++)` ⇒ **n ≤ 0 直接不跑**（既是空操作，也让 `degrade(level-3)` 这类「归到某档」的写法天然安全，见 `Bones.java:229-233`） |
| 存档 | ✅ | `Item.java:646` 原样 `put(LEVEL, level)`；`:663-668` 还原 `if (level>0) upgrade(level); else if (level<0) degrade(-level);` |
| 跨物品搬运 | ✅ | `ScrollOfTransmutation.java:266-270`（武器）、`:301-305`（戒指）同样按正负分支搬等级 |
| 显示 · 格子右下角 | ✅ | `ItemSlot.java:311` 走 `levelDisplay`；**`:328-329` 专门有 `buffedLvl < 0` → `DEGRADED` 配色分支** |
| 显示 · 名称后缀 | ✅ | `Item.java:489-492` `levelDisplay` = `Messages.format("%+d", lvl)` ⇒ `-3` 显示成 `-3`（**不是 `+-3`**）；`:542` 由 `title()` 拼上 |
| 临时降级 buff | ✅ | `Degrade.java:57-61`：`if (level <= 0){ //zero or negative levels are unaffected  return level; }` |
| 模组自定「字母制等级」 | ✅ | `GradeTestSword.java:16-18`、`BodyArtWeapon.java:273-275` 都**先判 `buffedLvl < 0` 回退数字制** —— 本作自己写的新机制已经考虑过负数 |

---

## 2. 各品类实测数据（真跑，不是推算）

### 2.1 近战武器（`Shortsword`，tier 2）— **C 档：负向传递**

`MeleeWeapon.java:274-283`：`min(lvl) = tier + lvl`，`max(lvl) = 5*(tier+1) + lvl*(tier+1)`

| 等级 | `min()` | `max()` | `STRReq` | `value()` |
|---|---|---|---|---|
| +0 | 2 | 15 | 12 | 40 |
| -1 | 1 | 12 | 12 | 40 |
| -2 | 0 | 9 | 12 | 40 |
| -3 | **-1** | 6 | 12 | 40 |
| -5 | -3 | 0 | 12 | 40 |
| -10 | -8 | **-15** | 12 | 40 |

- **`min()` 变负 ⇒ 可掷出负伤害**（`KindOfWeapon.java:264-280` 直接交给 `Hero.heroDamageIntRange`）。
- 到 **-10 时 `min(-8) > max(-15)`，区间反序**；`Random.NormalIntRange`（`Random.java` 里 `min + (int)((Float()+Float())*(max-min+1)/2f)`）**仍能算，不抛异常**。
- `STRReq` **夹到 0**（`Weapon.java:390` `lvl = Math.max(0, lvl)`）⇒ 负等级**不会**让武器更好拿（顺带保护了 `Math.sqrt(8*lvl+1)` 不产生 NaN）。
- `value()` **对负等级无折价** ⇒ `-5` 与 `+0` 同价（`MeleeWeapon.java:440` 只有 `level() > 0` 一个分支）。

### 2.2 投掷武器（`ThrowingKnife`）— **B 档：静默夹 0**

`MissileWeapon.java:98-126` 的 `min()/max()` 外层全部包了 `Math.max(0, …)`：

| 等级 | `min()` | `max()` | `durabilityPerUse()` |
|---|---|---|---|
| +0 | 2 | 6 | 20 |
| -1 | 1 | 4 | 33 |
| -2 | 0 | 2 | 50 |
| -5 | **0** | **0** | 100 |
| -20 | 0 | 0 | **Infinity** |

- 伤害**恒 ≥ 0**（写死夹取）。
- `durabilityPerUse()` = `MAX_DURABILITY / (baseUses * 1.5^lvl)`（`:445-...`）⇒ 等级越负**单次消耗越小**；`-20` 时 `1.5^-20` 下溢到 0 ⇒ 除零得 `Infinity` ⇒ **一掷即碎**（float 除零不抛异常）。

### 2.3 法杖（`WandOfMagicMissile`）— **C 档：充能上限可为负**

`Wand.java:427-430`：`maxCharges = Math.min(initialCharges() + level(), 10); curCharges = Math.min(curCharges, maxCharges);`
（本次实测 `WandOfMagicMissile.initialCharges() == 3`，见 `WandOfMagicMissile.java:90-92`；普通法杖是 2，`Wand.java:432-434`）

| 等级 | `maxCharges` | `curCharges` | `value()` |
|---|---|---|---|
| +0 | 3 | 3 | 75 |
| -1 | 2 | 2 | 37 |
| **-3** | **0** | 0 | 18 |
| -4 | **-1** | -1 | 15 |
| -6 | -3 | -3 | 10 |

- ⚠️ **`= -initialCharges()` 时 `maxCharges == 0` ⇒ 死杖**：`act()` 的 `if (curCharges < maxCharges && …) recharge()` 与 `while (partialCharge >= 1 && curCharges < maxCharges)` 条件恒假 ⇒ **永远充不上电、也永远放不出**（放技能要求 `curCharges >= chargesPerCast()`）。
- ⚠️ **无 `Math.max(0, …)` 保护** ⇒ 更负就是**负充能**，UI 会显示 `-1/-1` 这种读数（`Wand.java:339`）。
- 好消息：`recharge()` 内部 `missingCharges = Math.max(0, missingCharges)`（`:843-844`）⇒ `Math.pow` 不会拿到负指数导致 NaN。
- `value()` **对负等级有显式折价分支**（`:584-586` `price /= (1 - level())`）。

### 2.4 戒指（`RingOfHaste`）— **C 档：加成可为负**

`Ring.java:389-404` `soloBonus()` = 未诅咒时 `level() + 1`。

| 等级 | `soloBonus` | `value()` |
|---|---|---|
| +0 | 1 | 75 |
| -1 | 0 | 37 |
| -2 | -1 | 25 |
| -3 | **-2** | 18 |
| -5 | -4 | 12 |

- ⚠️ 所有戒指公式都是 `Math.pow(base, bonus)` ⇒ **负加成 ⇒ 乘数 < 1 ⇒ 效果反转**：
  `RingOfHaste.java:61` → 急速戒指**让你变慢**；`RingOfElements.java:89-93` → 元素戒指**让你更容易受伤**；
  `RingOfMight.java:110,113` → **扣力量、扣最大生命**（`HTMultiplier = 1.035^bonus`）。
- 例外：`RingOfForce.java:116` 有 `Math.max(…, 0)`，力场戒指免疫。
- `value()` **对负等级有显式折价分支**（`Ring.java:299-302`）。

### 2.5 防具 — **C 档：DR 在负等级处「反弹」（非单调！）**

`Armor.java:379-407`：
```java
DRMax(lvl) = tier*(2+lvl) + augment.defenseFactor(lvl);   // factor: NONE=0 / DEFENSE=1 / EVASION=-1
if (lvl > max) return ((lvl - max)+1)/2;                  // ← 本意是「高等级软上限」，负等级会被误触发
else           return max;
DRMin(lvl) = 若 lvl >= max 则 lvl - max，否则 lvl;
```

| `PlateArmor`(tier 5) 等级 | `DRMin` | `DRMax` | | `LeatherArmor`(tier 2) 等级 | `DRMin` | `DRMax` |
|---|---|---|---|---|---|---|
| +0 | 0 | 10 | | +0 | 0 | 4 |
| -1 | -1 | 5 | | -1 | -1 | 2 |
| -2 | -2 | **0** | | -2 | -2 | **0** |
| -3 | -3 | **1** ← 反弹 | | -3 | -3 | **-2** |
| -4 | -4 | 3 | | -5 | -5 | **1** ← 反弹 |
| -10 | -10 | **15** | | -10 | -10 | 3 |

- ⚠️ **`DRMax` 非单调**：越负反而越高（`-2 → 0`，`-3 → 1`，`-10 → 15`）。到 `-10` 时一件「极度降级的板甲」随机区间是 `[-10, 15]` —— 掷出正数时**相当于一件好护甲**。
- `STRReq` 与武器一样**夹到 0**（`Armor.java:699`）；`value()` **无折价分支**（`:716` 只判 `level() > 0`）。
- 好在 `Hero.drRoll()` 有 `if (armDr > 0) dr += armDr;`（`Hero.java:793`）⇒ **负 DR 不会被当成易伤**，只是「不生效」。

### 2.6 神器 / 饰品 — **A 档（显式支持）**

- 神器：`SandalsOfNature.java:225-227` **为负等级专门换贴图**（`if (level() < 0) image = ARTIFACT_SANDALS;` 降级变回「凉鞋」形象）；`Artifact.java:123-126` `visiblyUpgraded() = round(level*10/levelCap)`，负等级显示为负档。
- **饰品（trinkets）把「负等级」当成正式的「不持有 / 未启用」哨兵值**：`Trinket.java:48-60` `trinketLevel()` 在「没这个饰品」时返回 **-1**，于是所有饰品公式**都**先写 `if (level < 0) return 中性值`：
  `EyeOfNewt.java:57,69`（视距 ×1 / 心视 0）、`SilentPrice.java:80,86,92`（加成 ×1、空闲秒数 -1=禁用）、`ShardOfOblivion.java:197`。
  ⇒ **饰品是「负等级语义」写得最完整的一档，可直接照抄这个范式。**

### 2.7 附魔 / 字形 / 植物 — **B 档：静默夹 0**

`Armor.glyphs` 的 `Affection.java:41`、`Thorns.java:38`、`Viscosity.java:78`、武器附魔 `Roach.java:144`、
法杖异类 `WandOfRegrowth.java:232`、植物 `Earthroot.java:128` / `Sungrass.java:137` —— 全部 `Math.max(0, …)`。
⇒ 这些子系统**感知不到负等级**，会当 +0 处理。

---

## 3. 上游「显式支持负等级」的证据清单（说明这不是意外容错）

| # | 位置 | 写法 |
|---|---|---|
| 1 | `Item.java:663-668` | `else if (level < 0) degrade(-level);`（存档还原） |
| 2 | `ScrollOfTransmutation.java:268` | 同上（武器变形） |
| 3 | `ScrollOfTransmutation.java:303` | 同上（戒指变形） |
| 4 | `Wand.java:584-586` | `else if (level() < 0) price /= (1 - level());` |
| 5 | `Ring.java:301-302` | 同上（戒指折价） |
| 6 | `Degrade.java:57-61` | `if (level <= 0) return level;` + 注释「zero or negative levels are unaffected」 |
| 7 | `ItemSlot.java:328-329` | `else if (buffedLvl < 0) level.hardlight(DEGRADED);` |
| 8 | `SandalsOfNature.java:226` | 负等级换贴图 |
| 9 | `Char.attack:559-562` | `//do not trigger on-hit logic if defenseProc returned a negative value` + `if (effectiveDamage >= 0)` |
| 10 | `Hero.java:793 / :800` | `if (armDr > 0) dr += armDr;` / `if (wepDr > 0) …` |
| 11 | `Trinket.java:48-60` | 用 -1 当「未持有」哨兵，全品类公式按此中性化 |

**这是上游为旧版「Degradation（装备劣化）」机制留下的地基**，该机制后来被移除，但**负等级语义被完整保留**。

---

## 4. 危险点清单（会「表现异常」，但都不会崩）

| 现象 | 触发 | 位置 |
|---|---|---|
| 近战**负伤害**（打不动人，也不治疗） | 近战武器 ≤ `-(tier)` 起 `min()` 变负 | `MeleeWeapon.java:274-277` |
| 伤害区间**反序** | 近战 ≤ -10（tier 2） | `Random.NormalIntRange` 能算不崩 |
| **死杖**（永远不能放） | 法杖 = `-initialCharges()` | `Wand.java:427-430` |
| **负充能读数** `-1/-1` | 法杖更负 | `Wand.java:339` |
| 投掷武器**一掷即碎** | 投掷 ≤ ≈-17 | `MissileWeapon.durabilityPerUse` |
| 戒指**效果反转**（急速→变慢、元素→更痛、力量→扣血） | 戒指 ≤ -2 | 各 `Math.pow(base, bonus)` |
| 护甲 **DR 反弹**、区间含正数 | 护甲 ≤ -3 | `Armor.java:384-389` |
| 名称/格子显示 `-N` 与降级配色 | 任意 ≤ -1 | 正常行为（不是 bug） |

### ⚠️ 唯一一处「本该崩」的地方（已被上游守卫挡住）
以等级作**数组下标**的全库只有 **1 处**：`ChaoticCenser.java:161` `GAS_CAT_CHANCES[level]`（数组 4 元素 `0..3`），
而 `:155` 正好有 `if (level < 0 || level > 3) return false;` —— **这个守卫就是防越界的**。
⇒ 新写「按等级取表」的代码时**必须**照抄这个守卫（负等级 = 越界，`ArrayIndexOutOfBounds`）。

---

## 5. 为什么不会崩：三道现存护城河

1. `Char.damage:1051-1055` —— `if (!isAlive() || dmg < 0) { return; }` ⇒ **负伤害既不扣血、也绝不治疗**（不会「打怪回血」）。
2. `Char.attack:559-575` —— `int effectiveDamage = enemy.defenseProc(this, Math.round(dmg)); if (effectiveDamage >= 0) { …on-hit 全部内容… }`
   ⇒ 负伤害时**跳过全部附加效果**（附魔、状态、叠层），只在 `:599` 调一次必然早退的 `damage()`。
3. `Hero.drRoll:789-801` —— `if (armDr > 0) dr += armDr;` ⇒ 负 DR 不会被当成「易伤」。
4. 全库**没有**「以等级为循环上界」的循环、**没有**「以等级为数组下标」的地方（除上面那一处且有守卫）、**没有** `Math.max/min` 之外的整数除零。

---

## 6. 若要为「考验」加「负等级」效果：落点建议

1. **「归到某一档」用 `degrade(n)`，「设成某个确定值」用 `level(int)`** —— 两者都**会**刷新派生状态，
   但只有后者不消耗随机数。（本节原写「不要用 `level(int)`、它会跳过刷新」，**2026-09-23 实测推翻**：
   `Wand.java:258` 与 `RingOfMight.java:67` 都**覆写了** `level(int)` 分别转调 `updateLevel()` /
   `updateTargetHT()`，`HornOfPlenty.java:227` 同样覆写。`_chk/BinahGenProbe.java` 实测 +2 法杖
   `level(0)` 后 `maxCharges` 由 5 回到 3（= `initialCharges()`）、`curCharges` 同步被夹。
   反过来，走 `upgrade()/degrade()` 的循环会**消耗随机数**：`Wand.upgrade()` 与 `Ring.upgrade()`
   里各有 `Random.Int(3)` ⇒ 用在「生成时」这类不能动随机流的地方会挪动关卡布局。）
   （`ScrollOfTransmutation` 用「先 `level(0)` 再 `upgrade/degrade`」也无妨，那是它自己的语义。）
2. **要「下限」就在唯一汇聚点夹**：见 skill `egopd-source-verify` §7「改某个全局数值：先找唯一终局」。
3. **需要判「负等级语义」时，先选档**：
   - 想让负等级**当 +0 用** → 自己 `Math.max(0, …)`（与 B 档一致，玩家看到 `-N` 但数值等同 +0）。
   - 想让负等级**真的有惩罚** → 要逐品类写清语义（参考 C 档现状 / A 档饰品的「负 = 中性」范式）。
4. **注意 `0 / speed` 那类短路**：`0` 乘除不会因为「等级夹取」而改变，夹取要落在**最终值**上（HOKMA 已踩过，见 `docs/features.md` 考验章 §6）。
5. 若有意做「死杖」/「一掷即碎」，那是**特性**不是 bug，但要在文本里说清，否则玩家会当成故障。
6. **⚠️「生成之后还有人在改等级/诅咒」是最容易漏的一类出口**（2026-09-23 补）：
   只收口「生成那一刻」的规则，会被**事后脚本**绕开。已实测两种形态：
   - **NPC 赠礼**：四处任务 NPC 都在 `Generator.random()` 之后自己 `upgrade()` / `level(n)`
     ⇒ 需在各自加完之后另调压平钩子（见 `docs/features.md` §7）。
   - **特殊房间的事后诅咒**：`SacrificeRoom`（祭坛）/ `CryptRoom`（墓室）在生成后执行
     `if (!prize.cursed){ prize.upgrade(); } prize.cursed = true;`
     ⇒ 「必定诅咒」这个诅咒**在生成出口还看不到**，于是「因诅咒取负」对约七成赠品不触发，
     反而被白送的 +1 顶成正数。修法是加第三个钩子 `Trials.curseReverseLevel`（**只对正等级取负**）。
   排查手法：全库搜同时出现 `Generator.random` 与 `.upgrade(` / `.level(` 的文件，逐个看是否在生成之后再改。
   ⚠️ 补钩子时**不要**复用「一律归 0」的压平（会把已算好的负等级一起抹掉），
   也不要**二次**调用取负（−1 → +1）⇒ 需要「只处理正等级」的独立语义。

---

## 7. 核验工具（可复跑）

- `_chk/NegLevelProbe.java` —— **真跑探针**（不是读源码推算）：构造真道具 → 压到各负等级 → 读 `min/max/STRReq/value/maxCharges/soloBonus/DRMin/DRMax/durabilityPerUse`，
  并做 `Bundle` 存档往返实证（`-5 / -11` 均原样还原）。**30 项断言，全过**；输出存档见 `_chk/_negprobe_out.txt`。
  运行（classpath 与本 skill 的单文件 javac 一致，另需 **org.json**）：

  ```bash
  JDK=tools/jdk-21.0.12.1+1/bin
  GDX=.../gdx-1.14.0.jar ; GDXCTRL=.../gdx-controllers-core-2.2.4.jar ; JSON=.../json-20170516.jar
  CP="D:/PD/core/build/classes/java/main;D:/PD/SPD-classes/build/classes/java/main;D:/PD/services/build/classes/java/main;$GDX;$GDXCTRL;$JSON"
  "$JDK/javac.exe" -proc:none -encoding UTF-8 -cp "$CP" -sourcepath "D:/PD/core/src/main/java" -d _chk/_negprobe _chk/NegLevelProbe.java
  "$JDK/java.exe" -Dstdout.encoding=UTF-8 -cp "D:/PD/_chk/_negprobe;$CP" NegLevelProbe
  ```

  ⚠️ 三个**脱离游戏必须知道**的坑（本次都踩过）：
  - **`org.json` 必须在 classpath**：`Bundle.put/get` 走 `JSONObject`，缺了会甩 `NoClassDefFoundError: org/json/JSONException`
    （长得像「存档实现坏了」，其实只缺件）。
  - **`Messages` 无法初始化**：它的静态块要 `Gdx.files`（9 个语言包）⇒ `levelDisplay()` / `title()` 一类**跨不出游戏**。
    `%+d` 的语义改用 `java.lang.String.format` 等价验证（本作的 `Messages.format` 就是它的薄封装）。
  - **有些类 `new` 不出来**：实例初始化块里引用 `ItemSpriteSheet$Icons`（贴图帧）的类（如戒指）会因 `Gdx.files` 为空而崩；
    本次用 `Unsafe.allocateInstance(Class)` **绕过实例初始化块**（不跑 instance initializer，也不触发静态初始化）拿到实例，
    只测纯算术方法。探针里每段都包了 try/catch，单段失败不影响其余。
  - 顺带：`java` 重定向输出时中文/箭头会变 `?`（Windows 默认 GBK）⇒ 加 `-Dstdout.encoding=UTF-8`。

- 若只是要「查某处是否夹了负等级」，一条命令足够：
  `python _chk/docfind.py "Math.max(0"` 或直接
  `grep -rn "Math.max( *0 *, *.*[lL]evel" core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/`.
