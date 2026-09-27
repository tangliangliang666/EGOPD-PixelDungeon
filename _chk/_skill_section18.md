## 18. 「让某单位的一层视觉跟着它一起淡 / 消失」型规则 —— **浮层不继承精灵 alpha，`visible` 未必是显示开关**

与 §7/§9/§11/§12/§13/§15/§16/§17 并列的第八类写法。触发场景：把某单位的**透明 / 隐形效果**扩展到
它身上的其它指示物（2026-09-26 NETZACH 浮层：怪物**头顶血条** ＋ **状态标记**（睡眠/警觉/搜索/迷失）一起淡出）。

**① 先分清「精灵本体」与「挂在场景上的浮层」。** `CharSprite.draw()` 里的 `am *= fade` **只**影响精灵
自己那一张 quad；血条（`CharHealthIndicator` / `TargetHealthIndicator`）、状态标记（`EmoIcon`）都是各自
`GameScene.add(…)` 挂**场景**的独立节点，而 `Group` **不会**把父节点的 alpha 往下传
（`Group.draw()` 只是遍历成员逐个 `draw()`）⇒ 每个浮层都得自己消费一次那个系数，**且不会报错**。
症状：`>3 格`「完全看不见的怪」仍被一条浮空血条 / 一个 Zzz 图标精确定位。

**② alpha 写在哪，取决于继承链里有没有 `Visual`。**
- `EmoIcon extends Image extends Visual` ⇒ 直接有 `am`/`aa`：覆写 `draw()` 本帧乘、`super.draw()` 之后还原
  （与 `CharSprite.draw()` 同构）。**别**在 `update()` 里写持久 alpha —— 会与「真隐形走 `AlphaTweener`」打架。
- `HealthBar extends Component extends Group extends Gizmo` ⇒ **在 `Visual` 体系之外**，自己没有
  `am`/`aa`/`alpha()`，`Group` 也不传播 ⇒ 必须把系数写进它内部三个 `ColorBlock`
  （`ColorBlock extends Image extends Visual`）。**给「UI 组件」整体调透明度前，先看继承链**；
  沿 `Component` 往上找 alpha 一定是白找。

**③ `visible` 不是显示开关 —— 动手前先 grep 谁在读它。** `TargetHealthIndicator.instance.isVisible()`
被 `ChaoticCenser`（混沌香炉）当「英雄当前有没有锁定目标」在读（再取 `instance.target()` 去放毒气）。
为了「藏血条」把它置 false ⇒ 香炉**静默失效**（不崩、不报错，只是再也不放气）。字段同名不同义：
`CharHealthIndicator.visible` 无人外读 ⇒ 它就可以连可见性一起关。**核验要反向钉死**：
本次断言「瞄准血条 `update` 的字节码里没有 `fcmp`」＋ 源码里不出现 `visible = fade > 0f`。

**④ 核验脚本自身的两种腐烂**（本次各踩一次，都不是被测代码的错）：
- **「二级核验」假绿**：A 脚本只**读** B 脚本的产物快照（`verify_hod_netzach.py` 读
  `_chk/_hodnetzach.out`），快照早于源码改动就一直显示绿 —— 源码只改了一半、另一半没跟着改，
  **两边都是绿的**，肉眼看不出来。判据要么挂 `mtime(产物) >= mtime(被核验源码)` 并在不符时报 FAIL，
  要么脚本自己**先重跑探针**再断言。
- **`callers_of` 被注释污染**：`token in open(p).read()` 会把注释算进去（`Trials.java` 的注释里写着
  `…走 {@code HealthBar.setAlpha(fade)}…`）⇒ 「Trials 不反向依赖 UI 类」永远为假，而人只会以为清单写错了。
  数「**代码**引用」先过**保偏移的 `strip_comments`**，并用一对自测钉住语义
  （同一个 token：raw 清单含 `Trials.java`、剥注释后不含）。⚠️ 与仓里那份 raw `callers_of` 的差异
  要写进脚本注释，否则后人会以为其中一份写错了。
