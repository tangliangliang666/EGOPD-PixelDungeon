---

# 2026-09-27 考验 HOD（荣耀）精英化阈值调整：150 → 普通 300 / Boss 450

> 本章取代 §16.1 表格 / §16.5 里「某个敌方单位**存活超过 150 回合**」的旧口径。
> §16 按流水账惯例**保留原文不改**（与 §17 收紧 NETZACH 淡出阈值时的处理一致）。

## 22.1 需求原文

> 修改 HOD 挑战的数值设置，使得普通怪物获得精英效果所需时间改为 300 回合、boss 怪物获得精英效果所需时间改为 450 回合。

## 22.2 落点

| 文件 | 改动 |
|---|---|
| `Trials.java` | `HOD_ELITE_TURNS` **150 → 300**（普通）；新增 `HOD_ELITE_TURNS_BOSS = 450`（Boss）；新增唯一取数收口 `hodEliteTurns( Mob )` |
| `actors/buffs/HodGlory.java` | 判据 `++turns > Trials.HOD_ELITE_TURNS` → `++turns > Trials.hodEliteTurns( mob )`；类注释口径同步 |
| `assets/messages/misc/misc{,_zh}.properties` | `trials.hod_desc` 同步为「普通 300 / Boss 450」 |

```java
public static final int HOD_ELITE_TURNS      = 300;   // 普通
public static final int HOD_ELITE_TURNS_BOSS = 450;   // Boss

public static int hodEliteTurns( Mob mob ){
    if (mob != null && mob.properties().contains( Char.Property.BOSS )) return HOD_ELITE_TURNS_BOSS;
    return HOD_ELITE_TURNS;
}
```

## 22.3 Boss 判据＝`Char.Property.BOSS`

SPD **没有** `Mob.boss` 字段，boss 身份由 `Char.Property.BOSS` 表达（`Char.properties()` 返回 `HashSet<Char.Property>`）。

- **走 450**：`Goo` / `Tengu` / `DM300` / `DwarfKing` / `YogDzewa` / `YogFist` / `CrystalSpire` /
  `FungalCore` / `GnollGeomancer` / `SmilingCorpseMountain`。
- **仍走 300**：`MINIBOSS` 一族（`CrystalGuardian` / `FetidRat` / `GreatCrab` / `GnollTrickster` /
  `GnollSapper` / `FungalSentry` / `Pylon` / `RotHeart` / `RotLasher` / `DemonSpawner` / 元素体），
  以及全部普通怪与召唤物。

⚠️ **MINIBOSS 有意排除在 450 之外**（需求只说「boss 怪物」）。若日后要一并纳入，**只改 `hodEliteTurns` 一处**。

## 22.4 为什么收成一个方法

阈值现在有两条分支（300 / 450）。若把 `if (Boss) … else …` 直接写进 `HodGlory.act()`，将来任何别处要读这个阈值
（例如考验界面显示「还需 N 回合」）就会**长出第二份分支** ⇒ 两处悄悄漂移。收口后唯一取数点是
`Trials.hodEliteTurns(mob)`，`HodGlory` 只调它。

计时口径**未变**：仍只在「是敌方阵营 ＋ 活着 ＋ 本考验仍开着」时 `++turns`；不适用即清零；数满晋升
（`grantHodElite`）后自摘。

⚠️ 两个阈值都是 `public static final int` **编译期常量**，会被**内联进调用点** ⇒ 改完必须重新编译调用方。
`_chk/HodNetzachProbe` 这类探针若不重编（只跑 `run` 而不跑 `all`），会继续用旧值，表现为「断言数与预期不符」
或直接 `找不到符号`。

## 22.5 核验

| 项 | 结果 |
|---|---|
| `_chk/HodNetzachProbe.java` | **60 条断言全过**。⑥ 段新增：`HOD_ELITE_TURNS == 300` / `HOD_ELITE_TURNS_BOSS == 450` / `hodEliteTurns(普通怪) == 300` / `hodEliteTurns(Boss) == 450`；普通怪「300 不发、301 发」；Boss「数到 300 不发、450 不发、451 才发」。新增 `ProbeMob.markBoss()`（在子类内部 `properties.add( Char.Property.BOSS )`，绕开 protected 字段的跨类可见性） |
| `_chk/verify_hod_netzach.py` | **145 条 [OK]，0 失败**。常量断言改 300 ＋ 新增 450；新增 `hodEliteTurns` 方法体顺序断言；`HodGlory.act` 的 `seq_ok` 锚点改为 `++turns > Trials.hodEliteTurns( mob )`；文本层要素补 `300` / `450` |
| 单文件 `javac -Xlint:all`（8 文件） | **EXIT=0** |
| `check_utf8_all.py` | 1474 文件全绿 |
| 行尾 | `Trials.java` / `HodGlory.java` 保持 LF；`misc{,_zh}.properties` 保持纯 CRLF 且无裸 LF |

⚠️ **核验陷阱**（本次踩到）：`seq_ok` 的 token **不能互为前缀**。`HOD_ELITE_TURNS` 是 `HOD_ELITE_TURNS_BOSS`
的子串，于是两者在方法体里命中**同一个偏移**（`[92, 121, 121]`），而 `seq_ok` 要求严格递增 ⇒ 判失败。
改用不互为前缀的锚点（`return HOD_ELITE_TURNS_BOSS;` / `return HOD_ELITE_TURNS;`）即可。

## 22.6 待人工验证

1. 开 HOD，让一只**普通怪**存活 **301 回合**（贴着绕圈不打）⇒ 弹出精英名字的状态字。
2. 开 HOD，在 Boss 层让 **Boss** 数到 **300~450 回合** ⇒ **不应**发精英；超过 **450 回合** ⇒ 才发。
3. 考验界面 HOD 描述应为「……存活超过 300 回合……；Boss 单位则需存活超过 450 回合」。
