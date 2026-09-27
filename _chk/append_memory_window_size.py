# -*- coding: utf-8 -*-
"""① 追加今日日志；② 订正 MEMORY.md 里那条过期的「考验选择界面」索引（还停在 0.70/140）。"""
import sys

LOG = '.workbuddy/memory/2026-09-25.md'
MEM = '.workbuddy/memory/MEMORY.md'

SECTION = """
---

## 窗口尺寸对齐挑战窗：`A` 70 → 47.5（本轮，用户机上验证后要求）

**触发**：用户「考验窗口的大小也需要缩小，现在占据了整个屏幕，导致上下沿都看不到了，
需要参考原先就有的普通挑战栏的窗口大小」。

**第一步是量参照物，不是改代码** —— 这一步差点走错方向：

- 我先按「9 条挑战」估成内容 120×168，那会逼着把图标从 1.40 缩回 ~1.12（因为树高 = `4a + 图标高` 被压死）。
- 实际数了 `Challenges.NAME_IDS` 是 **13 条** ⇒ `WndChallenges` 内容高 = `16 + 13×16 + 12×1` = **236**，
  外框 **132×248**（该机上**已确认完整可见**）。
- ⇒「图标翻倍」与「挑战窗大小的窗口」**可以兼得**，只要把 `a` 收窄。

**几何定案（只动一个数）**

| 量 | 旧（二版） | 新（三版） |
|---|---|---|
| `WndTrials.A` | 70f | **47.5f** |
| `WndTrials.WIDTH` | 148 | **120** |
| `ICON_SCALE` / `TOUCH` | 1.40f / 26f | **不变** |
| 树外包 | 143.24 × 302 | **104.27 × 212** |
| 内容 / 外框 | 148 × 326 / 160 × 338 | **120 × 236 / 132 × 248**（与挑战窗逐像素同规格） |

`A = 47.5` 是**高度**卡出来的上限：`4A + 最大图标显示高 22 ≤ 236 − 标题 16 − 上下留白 8 = 212`。
宽度**不是**瓶颈（`A ≤ (120−22)/√3 = 56.6`）。斜向间隙 17.45px、纵向 25.5px，比二版还宽松。
⇒ 记一条：**改敌方/界面尺寸前先量参照物，再从两个方向各写一条容量判据**。

**改动**（幂等字节级补丁，7 处，331 → 340 行；纯 LF 保住）

- `windows/WndTrials.java`：`WIDTH` 120、`A` 47.5f、`H` 注释 ≈41.14、
  新增 `<h3>窗口尺寸</h3>` 类注释节（写明「与 `WndChallenges` 逐像素同规格」及为什么是 47.5）、
  `ICON_SCALE` 注释里把判据写成**两条**（横向 / 纵向）。
- 只改这一个文件；`ICON_SCALE`/`TOUCH`/`EDGES`/`NODE_*` **一个都没动**（树是同一张图的等比缩小）。

**核验升级**（`_chk/verify_trials_tree_ui.py` 103 → **108 断言**，8 → **10 组反例**）

- 新增 `judge_window()`：窗口内容尺寸必须与 `WndChallenges` **逐像素同规格**，
  且**两边都现算**（挑战窗高 = `TTL + n×BTN + (n−1)×GAP`，`n` 从 `Challenges.NAME_IDS` 数出来）——
  不抄死数字，挑战条数变了判据跟着变。
- 另断言：树高必须**正好吃满**剩余空间（否则说明有人绕过树尺寸直接写死 `resize`）、
  `A` 的上限必须由**高度**决定而不是宽度。
- 顺手加固：核验脚本也加了 `jround()` —— Python 的银行家舍入会让 `15 × 1.5 = 22.5` 算成 22，
  **判据自己算错**就会把好源码误判成 FAIL（原先只有预览脚本有这条）。
- 新反例：① `WIDTH` 回到 148（抓到「实为 148×236」）② `A` 放大到 60（抓到「实为 120×286」）。

**预览** `_chk/render_trials_ingame.py` → `_chk/_trials_ingame.png`（1376×1546）

- `geometry()` 支持 `A` 覆盖（节点偏移按 A/H 的倍数重算，否则对照面板的树会散架）；
- 面板：① 本版初始态 ② 本版开启 CHESED/GEBURA ③ **上一版 a=70 同 zoom 对照**（可直接比大小）
  ④ 详情小窗 ⑤ **与挑战窗 1:1 的尺寸对照条**（236 / 236 / 326 三根柱子，直观看旧版高出 90）。

**核验（全绿）**：`check_utf8_all.py` 1462 文件 OK、单文件 javac **EXIT=0**（仅 2 条既有 `this-escape`）、
unused-imports ALL PASS、`verify_trials_tree_ui.py` **108 断言 PASS**、`--selftest` **10/10 PASS**。

**同步**：`docs/features.md`（「两轮尺寸」→「三轮尺寸」表 + 红线改两条 + 核验节补 ①b）、
skill `egopd-trials-tree-ui`（规格表 / 三轮教训 / 脚本表 / `jround` 备忘）、本文件上面的 MEMORY.md 索引条。

**订正的一条错误结论**：skill 旧版写「手机竖屏虚拟高通常 ≥540，够用」——**那台机上 338 高的外框就被顶出去了**。
`Window.resize()` **不夹屏幕、只居中，超了没有任何报错**。正确做法是**对齐一个已知能完整显示的窗口**，别猜屏幕。

**未做**：未升版本、未打包（用户未要求）。
"""


def main():
    # ① 追加日志（append-only）
    b = open(LOG, 'rb').read()
    nl = '\r\n' if b'\r\n' in b else '\n'
    if '窗口尺寸对齐挑战窗' in b.decode('utf-8'):
        print('  [跳过] 日志已有本节')
    else:
        with open(LOG, 'a', encoding='utf-8', newline='') as f:
            f.write(SECTION.replace('\n', nl))
        nb = open(LOG, 'rb').read()
        print('  [OK]   日志追加 +%d 行；CRLF=%d 裸LF=%d'
              % (SECTION.count('\n'), nb.count(b'\r\n'),
                 nb.count(b'\n') - nb.count(b'\r\n')))

    # ② 订正 MEMORY.md 索引
    raw = open(MEM, 'rb').read()
    txt = raw.decode('utf-8')
    old = ('- **考验选择界面＝生命之树排版**（2026-09-25 实装）：`WndTrials` 内容宽 140，蜂窝密排 '
           '`a=70`／`h=a·√3/2`、`ICON_SCALE=0.70`；')
    new = ('- **考验选择界面＝生命之树排版**（2026-09-25 实装，尺寸已三轮）：`WndTrials` 内容 '
           '**120×236**（＝与 `WndChallenges` **逐像素同规格**，外框 132×248）。⚠️ **尺寸别猜屏幕高**'
           '（`Window.resize()` 不夹屏幕、超了无报错）⇒ 对齐一个已知能完整显示的窗口。蜂窝密排 '
           '`A=47.5`（**高度卡出的上限**：`4A+最大图标高 ≤ 236−16−8`；横向判据 `A ≤ (W−22)/√3` 不是瓶颈）／'
           '`h=A·√3/2`、`ICON_SCALE=1.40`；')
    if new.split('（2026-09-25 实装')[0] in txt and old not in txt:
        print('  [跳过] MEMORY.md 索引已是新版')
    else:
        n = txt.count(old)
        if n != 1:
            print('  [FAIL] MEMORY.md 索引：命中 %d 次（要求 1 次）' % n)
            return 1
        txt = txt.replace(old, new, 1)
        for m in ['120×236', 'A=47.5', '逐像素同规格']:
            if m not in txt:
                print('  [FAIL] 落盘前自检：找不到 %r' % m)
                return 1
        open(MEM, 'w', encoding='utf-8', newline='').write(txt)
        print('  [OK]   MEMORY.md 索引订正（去掉过期的 140 / 0.70）')
        print('         现在长度 %d 字符（上限 4000）' % len(txt))
    return 0


if __name__ == '__main__':
    sys.exit(main())
