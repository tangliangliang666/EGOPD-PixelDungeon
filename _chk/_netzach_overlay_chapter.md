
---

# 2026-09-26（续五）考验 NETZACH 浮层淡出：怪物头顶血条 ＋ 状态标记（睡眠 / 警觉 / 搜索 / 迷失）

> 需求原文：「现在需要你检查 NETZACH 考验的隐形效果，希望将怪物头顶的血条、以及右上角的标识符号
> （标记睡眠状态等）一并变为半透明或完全透明。」

## 20.1 问题：NETZACH 原来只淡化了「精灵本体」

上一版（§16.4）把淡化做在 `CharSprite.draw()` 里 —— 每帧在绘制前 `am *= fade`、画完立刻还原。
但**挂在怪身上 / 旁边的这几样都不继承它**：它们不是精灵的子节点，而是各自
`GameScene.add(...)` 挂到**场景**上的独立节点。

| 浮层 | 类 | 挂载方式 | 「看不见的怪」被它暴露成什么 |
|---|---|---|---|
| 血条（常驻） | `ui/CharHealthIndicator` | 构造器里 `GameScene.add(this)` | 怪物头顶一条浮空血条 |
| 血条（瞄准中） | `ui/TargetHealthIndicator` | `GameScene` 里 `add(new TargetHealthIndicator())`（单例） | 同上，而且它专门标出「你正瞄着谁」 |
| 状态标记 | `effects/EmoIcon`（`Sleep` / `Alert` / `Investigate` / `Lost`） | 构造器里 `GameScene.add(this)` | 睡眠 Zzz / 警觉 ! / 搜索 ? / 迷失 —— 精确定位 |

⇒ `>3 格`「完全看不见的怪」仍能被一条血条 + 一个 Zzz 图标指名道姓。

## 20.2 血条：`HealthBar` 自己**没有** alpha，必须写进三个色块

⚠️ 继承链是 `HealthBar extends Component extends Group extends Gizmo` —— 它在 `Visual`
**之外**，所以既没有 `am` / `aa` 字段、也没有 `alpha(float)` 方法；而 `Group.draw()` 只是
遍历成员逐个 `draw()`，**不会把父节点的 alpha 往下传**。

真正被画出来的是它内部三个 `ColorBlock`（`Bg` / `Shld` / `Hp`）：

```java
// ui/HealthBar.java（新增）
public void setAlpha( float value ){
    if (value < 0f) value = 0f;
    if (value > 1f) value = 1f;
    Bg.alpha( value );
    Shld.alpha( value );
    Hp.alpha( value );
}
```

`ColorBlock extends Image extends Visual` ⇒ 有 `alpha(v)`（= `am = v; aa = 0`）；颜色是烘在
`TextureCache.createSolid` 的 texture 里、由 `am` 当乘子 ⇒ 改 `am` 就是整体半透明。
`setAlpha` **不碰几何、不碰 `visible`**（那是 `layout()` 与 `update()` 的职责）。

两条血条的统一口径：

```java
float fade = Trials.enemyFade( target );
setAlpha( fade );
```

## 20.3 两条血条的 `visible` 处理**刻意不同**

| 类 | `visible` | 理由 |
|---|---|---|
| `CharHealthIndicator`（常驻） | `= (HP<HT \|\| shield>0) && fade > 0f` | 它的 `visible` **只服务自己**（全作没有第二个消费者）⇒ 全透明时连可见性一起关，最干净 |
| `TargetHealthIndicator`（瞄准中） | `= true`（**原样不动**） | ⚠️ 它的 `visible` 已经被**玩法逻辑**当语义读走了 |

⚠️ **`TargetHealthIndicator.instance.isVisible()` 不是「血条在不在屏幕上」**：
`items/trinkets/ChaoticCenser`（混沌香炉）拿它当「英雄当前有没有锁定目标」在读
（`if (instance != null && instance.isVisible())` ⇒ 取 `instance.target()` 去放毒气）。
若为了「藏血条」把它置 false，香炉就在 NETZACH 下**静默失效** —— 那是玩法改动，
违反 NETZACH「纯视觉、不碰任何游戏内状态」的设计（见 §16.4）。
`alpha` 压到 0 已经是「完全透明」，玩家同样看不到，而行为与改动前**逐位相同**。

⇒ 教训：**「`visible` 这个字段未必是显示开关」**，改 UI 显隐前先 grep 谁在读它。

## 20.4 状态标记：`EmoIcon` 在 `draw()` 里乘、画完还原

`EmoIcon extends Image extends Visual` ⇒ 直接有 `am` / `aa`。做法与 `CharSprite.draw()` 同构：

```java
// effects/EmoIcon.java（新增覆写）
@Override
public void draw() {
    float fade = (owner != null) ? Trials.enemyFade( owner.ch ) : 1f;
    float amBak = am, aaBak = aa;
    if (fade < 1f) { am *= fade; aa *= fade; }
    super.draw();
    if (fade < 1f) { am = amBak; aa = aaBak; }
}
```

- **为什么在 `draw()` 而不是 `update()`**：`update()` 只负责摆动缩放与定位（`owner.x + owner.width() - center.x`，
  即「怪物右上角」）；写持久 alpha 会与「真正的隐形走 `AlphaTweener`」打架 —— 与 §16.4 给 `CharSprite`
  的理由完全一致。`fade == 0` 时自然变成「画了但看不见」，不需要额外分支。
- **为什么只覆写基类**：`Sleep` / `Alert` / `Investigate` / `Lost` 四个子类**都不各自覆写** `draw()`，
  继承同一份 ⇒ 不会出现「改了基类漏了子类」。核验里专门 `javap -p` 四个 `EmoIcon$Xxx` 钉死这一点
  （且先断言「找得到这个类」，否则「不覆写」会退化成假通过）。
- `owner.ch` 对英雄时 `Trials.enemyFade(Dungeon.hero)` 立即返回 1 ⇒ 英雄自己的睡眠标记零影响。

## 20.5 仍未收口的浮层（刻意留着）

同属「挂在场景上、不继承精灵 alpha」，但本轮**没动**（改动面大，且都不是「状态标记」语义）：

| 未收口 | 出现条件 |
|---|---|
| `ShieldHalo shield` | `State.SHIELDED`（护盾） |
| `Flare aura` | `State.AURA`（部分单位自带光环） |
| `IceBlock` / `DarkBlock` / `GlowBlock` | `FROZEN` / `DARKENED` / `ILLUMINATED` |
| `TorchHalo light` | 光照 |
| `Emitter` 系列（`burning` / `chilled` / `marked` / `levitation` / `healing` / `hearts`） | 对应各状态粒子 |
| `FloatingText` | 伤害 / 治疗 / 升级漂浮字 |

⇒ 现状：开 NETZACH 时 `2~3 格`的半透明怪若顶着护盾 / 冰封，那层光环仍是**全亮**的。
要收口建议**统一走一个「挂到精灵时登记」的小工具**（登记表里存 `(Visual, Char)`，
在 `GameScene` 的绘制阶段统一乘系数），而不是逐个 `draw()` 手写 —— 否则以后每加一个
sprite 子效果都要记得来补一处。

## 20.6 落点

| 文件 | 改动 |
|---|---|
| `ui/HealthBar.java` | 新增 `setAlpha(float)`（夹取 0~1 ＋ 写三个 `ColorBlock`） |
| `ui/CharHealthIndicator.java` | `update()`：取 `fade` → `setAlpha(fade)` → `visible = … && fade > 0f` |
| `ui/TargetHealthIndicator.java` | `update()`：取 `fade` → `setAlpha(fade)`；`visible` **保持 `true` 不动**（香炉依赖） |
| `effects/EmoIcon.java` | 新增 `draw()` 覆写（本帧乘 `am`/`aa`、`super.draw()` 后还原）＋ `Trials` import |
| `Trials.java` | NETZACH 注释补「浮层收口点」清单 ＋「仍未收口」清单（**只改注释**，常量与 `enemyFade()` 一字未动） |
| `_chk/HodNetzachProbe.java` | 修上一轮遗留：④ 的距离阈值断言仍是旧的 `≤2 / 3~4 / >4` |
| `_chk/verify_hod_netzach.py` | `FADE_CALLERS` 由 2 个扩到 4 个；血条断言改成新形态 |
| `_chk/verify_netzach_overlay.py`（新） | 本轮专用核验（见 §19.7） |
| `_chk/_build_netzach_overlay.sh`（新） | 把这 5 个文件编进 `_chk/_javachk2`（供 `javap` 层核验） |

**没有**改动的：`CharSprite.draw()`（本体淡化照旧）、NETZACH 三个常量、`enemyFade()` 判定、
任何文本（`.properties` 无新增 / 无改动）。

## 20.7 核验

- `_chk/verify_netzach_overlay.py`（新）：**88 条断言全过、0 SKIP、0 假通过**（其中含 16 条反例自测）。
  五层：
  ① 源码结构（`setAlpha` 的三个色块 ＋ 不碰 `visible`/几何；两条血条的顺序；`EmoIcon.draw` 的
  「取系数 → 乘 → `super.draw()` → 还原」；4 个子类仍在；**剥注释后**的引用清单唯一性）
  ② `javap -p` 签名 ③ `javap -c` 字节码（`setAlpha` 恰好 3 次 `ColorBlock.alpha` 且顺序 Bg→Shld→Hp
  ＋ 零 `putfield`；两条血条「取数早于 `setAlpha`」；`EmoIcon.draw` 的 `am/aa` 各写两次且**还原晚于
  `super.draw()`**；`TargetHealthIndicator` 里没有 `fcmp` ⇒ 确实没拿 `fade` 去算 `visible`；
  四个 `EmoIcon$Xxx` 都不覆写 `draw`）④ 文本层（描述没被带坏）⑤ 探针输出层。
- `_chk/_build_netzach_overlay.sh`（新）：`javac -Xlint:all` 编 5 个文件 **EXIT=0**（只剩上游既有的
  `[this-escape]` 警告）。
- **本轮顺手修掉一处「假绿」**：上一轮把 `NETZACH_FADE_NEAR/FAR` 从 2/4 收紧到 1/3 时，
  `_chk/HodNetzachProbe.java` ④ 的断言忘了同步（仍是「距离 2 ⇒ 不透明」「距离 4 ⇒ 半透明」）；
  而 `verify_hod_netzach.py` 只是**读** `_chk/_hodnetzach.out`，那份输出文件的时间戳（18:41）
  早于 `Trials.java` 的改动（21:38）⇒ 一直显示绿。重跑立刻暴露 3 条 FAIL，
  改断言（`≤1 ⇒ 1.0` / `2~3 ⇒ 0.5` / `≥4 ⇒ 0`）后 **52 条全过**。
  ⇒ 教训见 `docs/handbook/pitfalls.md`：「核验脚本读产物快照」必须配套时间戳或每次重跑。
- 回归全绿：`verify_yesod`(131)、`verify_hod_netzach`、`verify_chesed_gebura`、`verify_hokma_delay`、
  `verify_binah_gen`、`verify_gebura_drop`(37)、`verify_fun_challenges`(81)、`verify_holy_card`、
  `verify_trials_tree_ui`、`verify_tree_trials_icons`、`verify_boss_phase_scales`(49)、
  `verify_marked_kill_routes`(87)、`verify_dwarfking_chesed`(27)；
  `_chk/YesodProbe` 37/0；`check_utf8_all` 1474 文件 OK；`check_unused_imports` ALL PASS。

## 20.8 待人工验证

1. 开 NETZACH，把一只怪拉到 **2~3 格**：怪半透明 ⇒ **头顶血条也应是半透明**（先打它一下让血条出现）。
2. 拉到 **>3 格**：怪消失 ⇒ **血条也消失**；用睡眠卷轴 / 让怪警觉 / 搜索 / 迷失时，
   **右上角的标记同样不可见**。
3. 用远程武器 / 法术瞄准 >3 格的怪：瞄准血条不可见（alpha=0）。
4. 带**混沌香炉**（`ChaoticCenser`）开 NETZACH：锁定 >3 格的怪时香炉应照旧会放毒气
   （这条正是不动 `TargetHealthIndicator.visible` 的原因）。
5. **不开** NETZACH 时逐项对照：血条、状态标记、瞄准血条应与改动前**完全一致**（`enemyFade` 恒 1 ⇒
   `setAlpha(1)` 即原样）。
