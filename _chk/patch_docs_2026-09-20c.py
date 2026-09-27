# -*- coding: utf-8 -*-
"""文档同步（2026-09-20c）：AGENTS.md 陷阱行 + weapon-creation-guide + hermes-caduceus-design + features.md"""
import os
import shutil

ROOT = r"D:\PD"
BAK = os.path.join(ROOT, "_chk", "_bak_2026-09-20c")
problems = []


def sub(path, old, new, tag):
    p = os.path.join(ROOT, path)
    with open(p, "rb") as f:
        raw = f.read()
    nl = "\r\n" if b"\r\n" in raw else "\n"
    t = raw.decode("utf-8")
    o, n = old.replace("\n", nl), new.replace("\n", nl)
    c = t.count(o)
    if c != 1:
        problems.append("[%s] 命中 %d 次" % (tag, c))
        return
    os.makedirs(BAK, exist_ok=True)
    d = os.path.join(BAK, os.path.basename(p))
    if not os.path.exists(d):
        shutil.copy2(p, d)
    with open(p, "wb") as f:
        f.write(t.replace(o, n).encode("utf-8"))
    print("  OK %s" % tag)


def sub_line_prefix(path, prefix, new_block, tag):
    """整行替换：以 prefix 开头的整行替换为 new_block（可多行）"""
    p = os.path.join(ROOT, path)
    raw = open(p, "rb").read()
    nl = "\r\n" if b"\r\n" in raw else "\n"
    lines = raw.decode("utf-8").split(nl)
    hits = [i for i, l in enumerate(lines) if l.startswith(prefix)]
    if len(hits) != 1:
        problems.append("[%s] 前缀 %r 命中 %d 行" % (tag, prefix[:24], len(hits)))
        return
    os.makedirs(BAK, exist_ok=True)
    d = os.path.join(BAK, os.path.basename(p))
    if not os.path.exists(d):
        shutil.copy2(p, d)
    i = hits[0]
    print("  旧: %s" % lines[i][:110])
    lines[i] = new_block.replace("\n", nl)
    print("  新: %s" % new_block.split("\n")[0][:110] + (" …（共 %d 行）" % new_block.count("\n") if "\n" in new_block else ""))
    open(p, "wb").write(nl.join(lines).encode("utf-8"))
    print("  OK %s" % tag)


def append(path, block, tag):
    p = os.path.join(ROOT, path)
    raw = open(p, "rb").read()
    nl = "\r\n" if b"\r\n" in raw else "\n"
    t = raw.decode("utf-8")
    if t.endswith(nl):
        t = t[:-len(nl)]
    os.makedirs(BAK, exist_ok=True)
    d = os.path.join(BAK, os.path.basename(p))
    if not os.path.exists(d):
        shutil.copy2(p, d)
    t += nl + block.replace("\n", nl) + nl
    open(p, "wb").write(t.encode("utf-8"))
    print("  OK %s" % tag)


# ============ 1. AGENTS.md §6 陷阱表新增行 ============
print("[1] AGENTS.md")
ROW = (
    "| 角色专属武器/装备**死亡后进了英雄遗骸**、被别的角色捡走（本次：赫尔墨斯的双蛇杖被非神谕角色拿到） "
    "| `Item.bones` 默认 `false`，但 **`EquipableItem` 把它置为 `true`** ⇒ 武器/护甲/神器**默认都会进遗骸**"
    "（`Bones.pickItem` 按 `item.bones` 筛候选、`Bones.get` 取出后还会降到 +3 并强制诅咒） "
    "| 角色专属件必须在自己的**实例初始化块**里显式写 `bones = false;`（参考 `HermesCaduceus` / `LogicStudio` / "
    "`SealedSwordBase` / `DarkSilence` / `MasterRing` / `InstructionTerminal`）。**坑中坑**：注释里写「默认 `bones = true`」"
    "会让「代码内不得出现 `bones = true`」这类核验断言**假红** ⇒ 断言前先 `strip_comments` |"
)
with open(os.path.join(ROOT, "AGENTS.md"), encoding="utf-8") as f:
    t = f.read()
if "bones = false" in t and "角色专属武器/装备" in t:
    print("  -- 已存在，跳过")
else:
    marker = "| 父类钩子与覆写方**各打一次**提示 ⇒ 玩家看到**两条一模一样**的消息"
    end = t.index("\n", t.index(marker))
    t = t[:end] + "\n" + ROW + t[end:]
    shutil.copy2(os.path.join(ROOT, "AGENTS.md"), os.path.join(BAK, "AGENTS.md"))
    with open(os.path.join(ROOT, "AGENTS.md"), "w", encoding="utf-8", newline="") as f:
        f.write(t)
    print("  OK 陷阱表新增一行")

# ============ 2. docs/weapon-creation-guide.md ============
print("[2] docs/weapon-creation-guide.md")
sub_line_prefix(
    "docs/weapon-creation-guide.md",
    "- **通关继承**",
    """- **通关继承**（胜利→下一局开局持有，并继承原剑「**等级/5（向下取整）+ 附魔**」；死亡不触发。2026-09-20 由「只继承 +0 白板」加强）：
  - **记录**：两种胜利结局都要打标——①幸福结局（上地面）：`SewerLevel.activateTransition` SURFACE 分支（切 SurfaceScene 前）；②普通结局（使用护符结束）：`Amulet.execute` 的 `AC_END` 分支（切 AmuletScene 前）。两处均判断 `FirelinkGreatsword.equippedBy(hero)`（主/副手装备）→ `markNextRun()`。
  - **`markNextRun()` 存什么**（`SPDSettings` 三键）：①`firelink_next_run = true`；②原剑的**纯升级等级 `trueLevel()`** 写入 `firelink_level`（**不是** `level()`，见通用教训③）；③附魔**类全名** `enchantment.getClass().getName()` 写入 `firelink_enchant`（无附魔存空串）。
  - **发放**：`Dungeon.init()` 在 `initHero` 之后调 `grantAtRunStart()`：有标记则 → 读三键并**立即清零**（防残留到再下一局）→ `new FirelinkGreatsword()` → `upgrade(srcLevel / INHERIT_LEVEL_DIVISOR)`（`INHERIT_LEVEL_DIVISOR = 5`，整数除法即向下取整；`0` 级不加）→ `enchantFromName(...)` 反射重建附魔（`Reflection.forName` + `isAssignableFrom(Weapon.Enchantment.class)`，失败**静默退回白板**）→ `identify()` → 收进背包。
  - **向后兼容**：旧版只写过 `firelink_next_run`，新增两键缺省值 `0` / `""` ⇒ 老档照样拿到 +0 白板剑。""",
    "guide.继承段",
)
sub(
    "docs/weapon-creation-guide.md",
    "（建议统一 LF 后写入）。",
    "（建议统一 LF 后写入）；③ **跨局继承取等级一律用 `trueLevel()`**——`Weapon.level()` 含 `curseInfusionBonus`（`+1+level/6`），"
    "继承品不带该加成，用 `level()` 等于白送等级；④ 设置只存得下 `int/boolean/String`（存不了 `Bundlable` 对象）⇒ "
    "要跨局带一件**附魔/形态**过去，存**类全名**再用 `Reflection.forName` + `newInstance` 反射重建最省事，"
    "并给「类不存在/不是该类/构造失败」留 `null` 兜底。",
    "guide.通用教训",
)

# ============ 3. docs/hermes-caduceus-design.md ============
print("[3] docs/hermes-caduceus-design.md")
append(
    "docs/hermes-caduceus-design.md",
    """## 遗骸：不进英雄遗骸（2026-09-20）

本武器是神谕代行者的**专属**武器，**不进英雄遗骸**——实例初始化块里显式 `bones = false;`。

**为什么容易漏**：`Item.bones` 默认是 `false`，但 **`EquipableItem` 把它置为 `true`**，所以武器/护甲/神器**默认都会进遗骸**（`Bones.pickItem` 按 `item.bones` 筛候选；`Bones.get` 取出后还会降到 +3 并强制诅咒）。不显式关掉，神谕代行者一死，下一局别的角色拾取遗骸就能拿到本武器。

核验：`_chk/verify_hermes_firelink.py`（含「实例块内 `bones = false`」与「代码内没有 `bones = true`」两条断言 + 反例自测）。""",
    "hermes.遗骸段",
)

# ============ 4. docs/features.md ============
print("[4] docs/features.md")
append(
    "docs/features.md",
    """# 2026-09-20 赫尔墨斯的双蛇杖禁止入遗骸 + 传火大剑继承加强为「等级/5 + 附魔」

## 一、赫尔墨斯的双蛇杖：`bones = false`

**症状**：神谕代行者死亡后，双蛇杖出现在英雄遗骸里，下一局**别的角色**拾取遗骸即可获得这件专属武器。

**根因**：`Item.bones` 默认为 `false`，但 **`EquipableItem` 把它置为 `true`** ⇒ **所有武器/护甲/神器默认都会进遗骸**（`Bones.pickItem` 按 `item.bones` 筛候选；`Bones.get` 取出后还会降到 +3 并强制诅咒）。双蛇杖从没显式关掉这个开关，于是和普通武器一样进了遗骸。

**对策**：`HermesCaduceus` 的实例初始化块里显式 `bones = false;`（与既有专属件同款：`LogicStudio` / `SealedSwordBase` / `DarkSilence` / `MasterRing` / `InstructionTerminal`）。

## 二、传火大剑跨局继承：从「白板 +0」加强为「等级/5（向下取整）+ 附魔」

原实现只发一把 +0 白板（设置里只存一个布尔标记 `firelink_next_run`），本次按需求同时继承**强化等级**与**附魔**：

| 环节 | 落点 | 说明 |
|---|---|---|
| 记录等级 | `FirelinkGreatsword.markNextRun()` → `SPDSettings.firelinkLevel(int)` | 取原剑 **`trueLevel()`**（纯升级等级） |
| 记录附魔 | 同处 → `SPDSettings.firelinkEnchant(String)` | 存附魔**类全名** `enchantment.getClass().getName()`；无附魔存空串 |
| 发放 | `grantAtRunStart()`（`Dungeon.init()` 在 `initHero` 之后调用） | 读三键 → **立即清零** → 造剑 → `upgrade(srcLevel / INHERIT_LEVEL_DIVISOR)` → 反射重建附魔 → `identify()` → 收进背包 |

### 一、为什么取 `trueLevel()` 而不是 `level()`
`Weapon.level()` 在 `curseInfusionBonus` 为真时返回 `level + 1 + level/6`；而继承品**不带**该加成 ⇒ 用 `level()` 相当于白送 1~2 级。本仓已有同类铁律（形态切换同步等级一律用 `trueLevel()`）：**只要把自身等级当成另一表达式的基准，就必须区分二者**。

### 二、为什么用「类名反射」而不是序列化对象
设置里只有 `int/boolean/String`（`GameSettings.put/getInt/getString/getBoolean`），存不下 `Bundlable`。附魔类本身是**无参可构造的 public static 类**（`Weapon.Enchantment` 子类），`Reflection.forName(name)` + `isAssignableFrom(Weapon.Enchantment.class)` + `Reflection.newInstance(cls)` 即可无损还原（已实测 `Blazing` 的 `getName() → forName → newInstance` 往返一致）。任何一步失败都**静默退回白板剑**，不抛异常、不影响开局。

### 三、向后兼容
旧版只写过 `firelink_next_run`；新增的 `firelink_level` / `firelink_enchant` 缺键时分别取 `0` / `""` ⇒ 老档（或更新后第一次继承）拿到的仍是 +0 白板剑，行为不变。发放后两键清零，不残留到再下一局。

## 三、改动落点

| 文件 | 改动 |
|---|---|
| `items/weapon/melee/HermesCaduceus.java` | 实例初始化块 `bones = false;` + 类注释说明 |
| `items/weapon/melee/FirelinkGreatsword.java` | 新增 `INHERIT_LEVEL_DIVISOR = 5` / `equippedInstance(Hero)` / `enchantFromName(String)`；`markNextRun()` 存等级+附魔；`grantAtRunStart()` 按「等级/5 + 附魔」发放；`equippedBy()` 改为委托 `equippedInstance()` |
| `SPDSettings.java` | 新增 `firelink_level` / `firelink_enchant` 两键 + 助手（CRLF 文件，改后仍保持 CRLF） |
| `messages/items/items_zh.properties`、`items.properties` | `firelinkgreatsword.stats_desc` 补「继承 1/5 等级 + 附魔」；英文侧同时补上原本缺失的继承说明 |

## 四、核验（源码级，未代跑 Gradle）

- `javac -proc:none -Xlint:all`（3 文件：`HermesCaduceus` / `FirelinkGreatsword` / `SPDSettings`）**EXIT=0、0 错误**；`FirelinkGreatsword`、`SPDSettings` **0 告警**，`HermesCaduceus` 只剩两条**改动前既有**告警（`:66` `[rawtypes]` 的 `new Class[][]`、`:349` `[lossy-conversions]` 的 `delay *= Math.pow(...)`，均落在未改动行，只因新插 5 行而整体下移）。
- 新增 `_chk/verify_hermes_firelink.py`：**66 条断言 + 反例自测，ALL PASS**（Hermes 2 / Firelink+SPDSettings 26 / 文本 zh+en 14 / 断行自检 2 / 反例 5）。顺序敏感处一律 `body.index()` 比位置（先读后清、清零早于造剑、附魔早于 `identify()`）；「不得再出现旧写法」先 `strip_comments`。
  - **写脚本时踩的两个假红**（修脚本、未放宽断言）：① 赫尔墨斯的**注释里**写着「`EquipableItem` 默认 `bones = true`」，全文搜 `bones = true` 会假红 ⇒ 必须先剥注释；② 反例自测里旧文件取不到 `enchantFromName` 方法体时**提前 return**，后续断言键整体缺失、`.get()` 返 `None` ⇒ 改为「方法体缺失即置空串，每条断言各自判 False」，并引入 `before(a,b)` 兜住 `index()` 的 `ValueError`。
- 新增 `_chk/FirelinkLocCheck.java`：真 `Properties.load` 读两份 items + 真跑继承公式边界值（0/1/4/5/6/9/10/12/24/25/30 ⇒ 0/0/0/1/1/1/2/2/4/5/6）+ 真跑附魔重建链（`Blazing` 往返一致、假类名返 null、非附魔类被 `isAssignableFrom` 拦掉），**ALL PASS**。
  - **顺带发现**：`com.watabou.utils.Reflection.forName` 内部走 gdx `ClassReflection.forName`，**会引用 gdx-controllers 的类** ⇒ 单跑这个 Java 核验脚本必须把 `gdx-controllers-core` 加进 classpath（与 `egopd-source-verify` 第 1 步同款要求）；它对「类不存在」是**捕获后返回 null**（会往 stderr 打一段堆栈），所以 `enchantFromName` 里判 `cls == null` 足以兜住。
  - **真装载的段数判据要写对**：`\\n\\n` 解析成两个换行 ⇒ `split("\\n")` 切出「段1 / 空串 / 段2」**三块**，断言必须是「非空块恰好 2 个 + 中间块是空行」，写成「段数 == 2」会假红。
- `_chk/check_utf8_all.py`：1445 文件全合法 UTF-8。

## 五、待人工验证

能力范围外（游戏窗口无输出可采集）：① 非神谕角色死亡后确认遗骸里**没有**双蛇杖，而神谕角色死亡后遗骸里仍有专属遗物 `BrokenTerminal`（未受影响）；② 传火大剑 +N 通关后下一局开局确认拿到 剑(+(N/5)) 且**附魔一致**（先在原剑上贴一张附魔卷轴更容易看出来）；③ `+4` 剑（N/5=0）确认仍是白板剑；④ 连续两局验证「第二局不再返还」（键已清零）。""",
    "features.新条目",
)

print()
if problems:
    print("!!! 有问题：")
    for p in problems:
        print("   -", p)
    raise SystemExit(1)
print("ALL DONE")
