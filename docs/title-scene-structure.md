# 标题界面（`TitleScene`）显示结构调研

> 调研时间 2026-09-21，为「下一步改标题界面」做准备。全部结论附 `文件:行号`。
> 索引：`docs/INDEX.md`。复算工具：`python _chk/title_layout_calc.py`。
> 并行调研已全部归档；本文不再含待填占位。

## 0. 一句话结论

`TitleScene` 是**扁平手工排版**：一个 `Scene` 子类，所有元素在 `create()` 里按固定顺序 `add()`，
位置全部用 `Camera.main.width/height` 现场算 —— **没有布局容器、没有约束系统、没有 `layout()` 回调**。

所以改动的风险不在「画不出来」，而在三处硬约束：

1. **z 序 = `add()` 顺序**（没有 z 字段可调）。
2. **`updateFade()` 必须手工登记每一个新元素** —— 漏了就「收起 UI」收不干净。
3. **竖屏最矮机型只剩 14px 余量** —— 再加一行按钮就溢出；横屏则宽裕（见 §3.4）。

## 1. 文件与继承

| 角色 | 位置 |
|---|---|
| 场景本体 | `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/scenes/TitleScene.java`（492 行） |
| 基类 | `scenes/PixelScene.java`（502 行，`extends Scene`） |
| 引擎基类 | `SPD-classes/src/main/java/com/watabou/noosa/Scene.java` |
| 背景 | `ui/TitleBackground.java` |
| 标题贴图 | `effects/BannerSprites.java` |
| 火把 | `effects/Fireball.java` |

生命周期：`create()`（一次性排版）→ 每帧 `update()` → 切场景时 `destroy()`。
**`TitleScene` 自己没有 `update()`** —— 所有逐帧行为都由按钮子类覆写（§6）。

内部有 **4 个私有静态子类**，各自覆写 `onClick()` / `update()`：
`NewsButton`(355)、`ChangesButton`(395)、`SettingsButton`(449)、`SupportButton`(479)。

⚠️ 文本键是按**嵌套类**区分的：`scenes.titlescene$changesbutton.title` 这种带 `$` 的键（§7）。

### 1.1 进入路径：标题界面**不是**首屏

冷启动的初始场景**永远是 `WelcomeScene`**（`ShatteredPixelDungeon.java:53`：`super(sceneClass == null ? WelcomeScene.class : sceneClass, platform)`）。
`WelcomeScene` 与 `TitleScene` 是**兄弟**关系（都 `extends PixelScene`），不是父子：

| 情形 | 路径 |
|---|---|
| 首次运行（`previousVersion == 0`） | `WelcomeScene` 显示开场 → 无存档且无排行榜时 `switchScene(HeroSelectScene)`（`:163`），否则 `switchScene(TitleScene)`（`:161`） |
| 同版本、`intro` 已关（绝大多数日常启动） | `WelcomeScene:85` 直接 `switchNoFade(TitleScene.class)` —— **WelcomeScene 一帧都不画** |
| 版本升级后 | `WelcomeScene` 显示更新说明 → `switchScene(TitleScene)`（`:166-167`） |

⇒ **改 `TitleScene` 不会影响开场界面**；反过来，若你的改动想让「首启」也生效，得同时看 `WelcomeScene`。
`WelcomeScene` 还**借用**了本场景的两个文本键（`Messages.get(TitleScene.class, "changes"/"enter")`，`:177`/`:192`）
—— **改这两个键的中文会连带改动开场界面上的按钮**。

场景切换语义（`Game.java`）：

- `switchScene(Class)`（`Game.java:214-222`）只是**登记请求**；真正切时 `Reflection.newInstance(sceneClass)`
  （`:232-245`）⇒ **永远是全新实例**，不复用；随后 `Camera.reset()` → `scene.destroy()` → `scene.create()`，
  并把 `Game.elapsed = 0`（`:251-269`）。
- `switchNoFade(Class)`（`ShatteredPixelDungeon.java:136-139`）只是把 **`PixelScene.noFade = true`**，
  它是一个**静态一次性开关**，由下一个 `fadeIn()` 消费并清除（`PixelScene.java:365-372`）。
  两种写法的差别仅为「目标场景要不要黑色淡入」。
  ⚠️ 陷阱：`WelcomeScene.create()` **不调用 `fadeIn()`** ⇒ 若对它用 `switchNoFade`，这个标志会**泄漏给再下一个场景**。
- `ShatteredPixelDungeon.switchScene()` 会额外 `restoreWindows()`（`:154-160`）。
- `Game.scene()` 返回**活着的实例**（不是类），首次 `step()` 前为 `null`。

### 1.2 基类 `PixelScene` 的要害（改动时必看）

- `create()` 里**必须**先 `super.create()`（`PixelScene:96-98`；本场景 `:85-87` 已遵守）。
- `PixelScene` **不定义** `uiAlpha` / `updateFade()` —— 这两个是 **`TitleScene` 自己的**（`:308`/`:310`），
  不是覆写任何东西。别以为有基类兜底。
- `create()` **不调用 `fadeIn()`**，由子类自己决定（本场景在 `:305` 调）。
- `uiCamera` 由基类创建且**初始可见**（`:149-151`）；本场景把它关掉（`:94` `uiCamera.visible = false;`）。
- `pixelFont` = `fonts/pixel_font.png`，`LATIN_FULL`（`:153-157`）——**只有拉丁字符**。
- `getCommonInsets()`（`:413-421`）先取 `INSET_ALL`，再用 `INSET_BLK` 覆盖左右边，最后 `scale(1f/defaultZoom)`。
  **13 个场景都在用它**（Title/Start/Welcome/Surface/Supporter/Rankings/News/Journal/Game/Changes/Amulet/Alchemy/About）
  ⇒ **改它会波及全仓**。
- 从游戏内回到菜单时会清缓存：`TextureCache.clear() + TitleBackground.reset() + Holiday.clearCachedHoliday()`（`:103-109`）。
- `destroy()` 会 `PointerEvent.clearListeners()`（`:313-316`）⇒ 子类覆写 `destroy()` 必须调 `super`。
- `landscape()`（`:325-327`）= `SPDSettings.interfaceSize() > 0 || Game.width > Game.height`。

## 2. 元素清单与 z 序

**z 序 = `add()` 的先后**，先加的在下层。`create()` 里的实际顺序：

| # | 字段 | 类型 | 内容 | 源码行 | 真的 add 了吗 |
|---|---|---|---|---|---|
| 1 | `BG` | `TitleBackground` | 全屏背景（深色砖墙 + 火光） | 101–102 | ✅ |
| 2 | `title` | `Image` | 标题贴图（竖 139×100 / 横 240×**57**） | 107–115 | ✅ |
| 3 | `leftFB` | `Fireball` | 左火把（`placeTorch()` 内部 add） | 118/121, 345–353 | ✅ |
| 4 | `rightFB` | `Fireball` | 右火把 | 119/122 | ✅ |
| 5 | `signs` | `Image`（匿名子类） | 标题发光层，**自己算 alpha**、`setLightMode()` 混色 | 125–143 | ✅ |
| 6 | `btnPlay` | `StyledButton` | 进入地牢 | 147–172 | ✅ |
| 7 | `btnSupport` | `SupportButton` | 支持游戏开发 | 174–175 | ✅ |
| 8 | `btnRankings` | `StyledButton` | 排行榜 | 177–184 | ✅ |
| 9 | `btnJournal` | `StyledButton` | 日志 | 187–194 | ✅ |
| 10 | `btnNews` | `NewsButton` | 游戏新闻 | 196–199 | ❌ **被注释掉**（见 §9） |
| 11 | `btnChanges` | `ChangesButton` | 改动 | 201–203 | ✅ |
| 12 | `btnSettings` | `SettingsButton` | 设置 | 205–206 | ✅ |
| 13 | `btnAbout` | `StyledButton` | 关于 | 208–215 | ✅ |
| 14 | `version` | `BitmapText` | 右下角 `vX.Y.Z`，`hardlight(0x888888)` | 246–251 | ✅ |
| 15 | `btnFade` | `IconButton` | 底部中央的下折箭头，图标旋转 180° | 253–271 | ✅ |
| 16 | `fadeResetter` | `PointerArea` | **全屏**点击区，用来把 UI 收回来 | 273–291 | ✅ |
| 17 | `btnExit` | `ExitButton` | 右上角退出（**仅桌面**） | 293–297 | ✅（`DeviceCompat.isDesktop()`） |
| 18 | — | `WndVictoryCongrats` | 首次通关祝贺弹窗（`Badges.loadGlobal()` 后判定） | 299–303 | ✅（条件） |

另有一个**不在 `add()` 列表里**但状态相关的量：`uiAlpha`（`TitleScene:308`），见 §4。

> **加新元素时的位置选择**：想压在按钮**下面**就得在 6–13 之前 `add()`。
> ⚠️ 但**点击**的先后跟这个顺序**相反**，见 §2.1 —— 这是加 UI 时最容易判断错的一件事。

### 2.1 画序 ≠ 事件序（**关键，与本项目此前的写法相反**）

先说结论：**绘制**按 `add()` 顺序（后加的在上层）；**指针事件**却按**创建顺序的倒序**分发
（后 new 出来的**先**拿到事件）。两者不是一回事，判断「谁挡住谁」必须用后者。

- `Scene extends Group`（`SPD-classes/.../noosa/Scene.java:29`）；`Group.draw()` 按下标迭代 ⇒ **add 序 = 画序**，后加在上（`Group.java:72-79`）。
- 但命中检测**不是遍历 `Group.members`**：`Group.hitTest` / `Scene.ripple` / `TouchArea` **在本仓都不存在**。
  真正的机制是一条**全局信号**：
  - `PointerEvent.java:100` — `private static Signal<PointerEvent> pointerSignal = new Signal<>( true );` ← `stackMode = true`
  - `Signal.java:40-48` — `stackMode` 时 `listeners.addFirst(listener)` ⇒ **后注册的排在最前**
  - `Signal.java:67-81` — 依次调用，**谁先返回 `true` 就 `return`**（后续监听器收不到）
- 注册发生在 **`PointerArea` 的构造函数**里（`PointerArea.java:47-54` 的 `PointerEvent.addPointerListener(this)`），
  **不是**在 `Group.add()` 时 ⇒ **决定事件序的是 `new` 的先后，不是 `add()` 的先后**。
- 于是本场景的实际事件序（先拿到事件的在前）：

  `btnExit`(`:294`) 与 `WndVictoryCongrats`(`:302`) → 各按钮自己的 `hotArea`（`Button.createChildren()` 里建，`:147–215`）
  → `btnFade`(`:253`) → **`fadeResetter`(`:273`)**

  ⇒ **后创建的全屏 `PointerArea` 确实能吞掉先创建按钮的点击**，而「在画面上更靠上层」的直觉在这里是**反的**。
- **`return false` = 「我没处理」**，分发**继续**给后面的监听器（`Signal.java:74-77`），不是「悄悄消费掉」。

**本场景为什么目前是安全的**：`fadeResetter`（`:273–291`）**整个覆写了 `onSignal` 且从不调 `super`**，
结尾 `return false`（`:288`）⇒ 它**结构上不可能阻挡任何事件**。代价是它的副作用——
`if (event.type == UP && !btnPlay.active)`（`:276`）——会在**任何** UP 事件上触发，包括落在按钮上的那些。

⚠️ **给你自己加 UI 时的判据**：
- 想「只在空白处响应、且不挡按钮」→ 照抄 `fadeResetter`：覆写 `onSignal`、结尾 `return false`。
- 想要「挡住下层」（模态遮罩）→ **必须 `return true`**（或像 `ui/Window.java:68-80` 那样建默认
  `PointerArea` 做 blocker；默认实现会消费 DOWN，所以每个 `Window` 都能挡住整个标题界面）。
- **默认 `onSignal` 是「会挡」的**（`PointerArea.java:57-63,105`；`blockLevel` 默认 `BLOCK_WHEN_ACTIVE`），
  别以为 `visible=false` 就不挡——`PointerArea` 构造时就是 `visible = false` 但**照样**参与分发。
- `PixelScene.destroy()` 会 `PointerEvent.clearListeners()`（`PixelScene.java:313-316`）⇒ 监听器栈每个场景重建，
  顺序完全由**该场景内的创建顺序**决定，跨场景不残留。

**一个既有的隐蔽副作用**：`btnNews` 虽未 `add()`，但它的 `hotArea` 在构造时就**已注册且活着**；
之所以无害，是因为它从未参与布局、矩形一直是 `0×0`（`Button.layout()` 同步 `hotArea`，而 `layout()` 只对已挂载组件跑）。
⇒ 反过来说：**只要给未显示的控件设了 rect，它就会开始吃点击**。

## 3. 布局数学（可复算）

### 3.1 相机尺寸从哪来

`Camera.main` 不是常数，`PixelScene.create()`（111–145）决定：

```java
if (SPDSettings.interfaceSize() > 0) { minWidth=MIN_WIDTH_FULL(360); minHeight=MIN_HEIGHT_FULL(200); scaleFactor=3.75f; }
else if (landscape()) { minWidth=MIN_WIDTH_L(240); minHeight=MIN_HEIGHT_L(160); scaleFactor=2.5f; }
else                  { minWidth=MIN_WIDTH_P(135); minHeight=MIN_HEIGHT_P(225); scaleFactor=2.5f; }

maxDefaultZoom = max(2, (int)min(屏幕宽/minWidth, 屏幕高/minHeight));
defaultZoom    = SPDSettings.scale()，越界则 gate(2, ceil(density*scaleFactor), maxDefaultZoom);
```

`landscape()` = `SPDSettings.interfaceSize() > 0 || Game.width > Game.height`（`PixelScene:325-327`）——
**「界面尺寸」设置一开就强制走横屏排版**。

⇒ `Camera.main = 屏幕像素 / zoom`，且**必不小于**各模式的 `(minWidth, minHeight)`，随设备长宽比变大。
`TitleScene` 用的就是这两个值（96–97 行）。

### 3.2 排版公式（逐字来自 `TitleScene:110–271`）

```java
topRegion = max(title.height - 6, h * 0.45f);            // :110
GAP = (int)(h - topRegion - (landscape()?3:4)*20) / 3;   // :217-218  BTN_HEIGHT=20
GAP /= landscape() ? 3 : 5;                              // :219
GAP = max(GAP, 2);                                       // :220
buttonAreaWidth = landscape() ? 240-6 : 135-2;           // :222
btnAreaLeft     = insets.left + (w - buttonAreaWidth)/2;  // :223
```

标题贴图尺寸（`BannerSprites:41–52` 的 `uvRect` 写死；`uvRect(x0,y0,x1,y1)` 是**像素矩形**，
尺寸 = `(x1-x0) × (y1-y0)`）：

| | uvRect | 真实尺寸 | 标题左上角 |
|---|---|---|---|
| 竖屏 `TITLE_PORT` | `(0, 0, 139, 100)` | **139×100** | `x = left+(w-139)/2`，`y = top+2+(topRegion-100)/2` |
| 横屏 `TITLE_LAND` | `(0, 100, 240, 157)` | **240×57** ⚠️ | `x = left+(w-240)/2`，`y = top+2+(topRegion-57)/2` |

> ⚠️ **本文件初稿在这里算错过一次，记录备查**：`uvRect(0,100,240,157)` 的**后两个参数是右下角坐标**，
> 横屏横幅高 **57** 而非 157。因为 `topRegion = max(标题高-6, h*0.45)` 直接吃这个数，
> 差 100px 会把横屏布局算法整个带偏（初稿据此得出了「横屏名义最小值装不下」的**错误结论**）。
> 改动时若改了 `banners.png`，**必须**同步改这里的 uvRect，并按 §3.4 复算。

发光层 `TITLE_GLOW_*` 尺寸与对应 `TITLE_*` **完全相同**（`(139,0,278,100)` / `(240,100,480,157)`），
所以 `signs` 能与 `title` 严丝合缝地叠上去（`TitleScene:141-142` 只居中、不缩放）。

另一个**已知的小出血**在竖屏最窄处：`MIN_WIDTH_P = 135` 而竖屏横幅宽 **139** ⇒ 在 135 宽的相机下
`title.x = (135-139)/2 = **-2**`，左右各被裁掉 2px。上游即如此。

`banners.png` 实际 **512×256**，而 uvRect 最大用到 `(480,157)` ⇒ 有富余，**放大横幅不必换图**；
但 `uvRect` 是写死的字面量，改图不改 uvRect 会静默错位。

### 3.3 两条排布链（**news 槽位参与了定位**）

**横屏**（3 列 × 3 行，`btnAreaLeft` 起）：

```
row0:  btnPlay(左半, 234/2-1=116 宽)          btnSupport(右半, 116)
row1:  btnRankings(col1, 77)  btnJournal(col2, 77)  btnNews(col3, 77)   ← 未显示
row2:  btnSettings(col1, 77)  btnChanges(col2, 77)  btnAbout(col3, 77)
```
关键：`btnChanges` 是 `setRect(btnSettings.right()+2, btnSettings.top())` 定位的，
而 `btnSettings` 的 y 又是 `btnRankings.bottom()+GAP` —— **与 news 无关**，所以横屏只丢一个格。

**竖屏**（2 列 × 5 行）：

```
row0:  btnPlay(全宽 133)
row1:  btnSupport(全宽 133)
row2:  btnRankings(左半 65.5)   btnJournal(右半 65.5)
row3:  btnNews(左半) ← 未显示    btnChanges(右半)  ← x = btnNews.right()+2
row4:  btnSettings(左半) ← x = btnNews.left()      btnAbout(右半)
```
**`btnChanges` / `btnSettings` / `btnAbout` 的 x 全部由 `btnNews` 的矩形推出来**
（`TitleScene:241–243`）。news 不显示时，**竖屏第 3 行左半是空洞**，
「改动」孤零零挂在右半。这是当前最显眼的排版缺陷。

### 3.4 量化结论：当前排版**两种朝向都装得下**

用 `_chk/title_layout_calc.py` 复算（该脚本逐字复刻上述公式）：

| 模式 | 实际画出行数 | GAP 预算按几行算 | 所需最小**相机高** | 名义最小相机高 | 结论 |
|---|---|---|---|---|---|
| 横屏 | 3 | 3 | **119 px** | 160 px | ✅ 余量 19~48px |
| 竖屏 | 5 | **4** ⚠️ | **203 px** | 225 px | ✅ 但最矮竖屏只剩 **14px** 余量 |

实测三档：

| 相机 | 朝向 | `topRegion` | `GAP` | 已显示按钮最底边 | 余量 |
|---|---|---|---|---|---|
| 426×240 | 横 | 108 | 8 | 192 | **+48** ✅ |
| 400×180 | 横 | 81 | 4 | 153 | **+27** ✅ |
| 240×160 | 横 | 72 | 3 | 141 | **+19** ✅ |
| 180×320 | 竖 | 144 | 6 | 274 | **+46** ✅ |
| 135×225 | 竖 | 101 | 2 | 211 | **+14** ⚠️ |

⚠️ **你要动标题界面时，真正会咬人的是这两个数**：

1. **竖屏最矮机型只剩 14px** ⇒ **再加一行按钮（20px + 2px 间距＝22px）就会溢出**。
   横屏则宽裕（哪怕加两行也还够）。
2. **竖屏的 GAP 预算少算了一行**：源码写的是 `(landscape()?3:4)`（`TitleScene:218`），
   而竖屏实际画 **5** 行 ⇒ 间距一直被算小。屏越矮越贴边。想彻底解决就把它改成实际行数（横 3 / 竖 5）。
3. **`topRegion` 由标题贴图高度主导**（横屏 `max(51, 0.45h)`、竖屏 `max(94, 0.45h)`）——
   放大横幅会同时抬高按钮区起点，直接吃掉上面那点余量。

复算方式：

```bat
python _chk/title_layout_calc.py                    rem 一组典型尺寸
python _chk/title_layout_calc.py 400 180 --ascii     rem 指定相机宽高 + ASCII 排版图
python _chk/title_layout_calc.py 180 320            rem 竖屏
python _chk/title_layout_calc.py 426 240 --insets 24,0,0,8
```

## 4. 淡入淡出：**新增元素必须登记**

- `uiAlpha`（`:308`）是唯一的透明度源；`updateFade()`（`:310–343`）把它分发到各元素。
- **收起**：点 `btnFade` → `enable(false)` → `Tweener(parent, 0.5f)` 把 `uiAlpha` 从 1 拉到 0（`:253–267`）。
- **恢复**：`fadeResetter.onSignal` 在 `UP` 事件且 `!btnPlay.active` 时反向拉回，并把 `btnFade` 重新 `enable(true)`（`:273–291`）。
- `updateFade()` **逐元素手写**：`title.am`、`leftFB.am`、`rightFB.am`、8 个按钮的 `enable()`+`alpha()`、
  `version.alpha()`、`btnFade.icon().alpha()`、`btnExit`（判空）。

⚠️ **三个必踩点**：

1. **新加的可视元素如果不写进 `updateFade()`，收起 UI 后它会赖在屏幕上**（既不报错也不闪）。
2. `signs.am` **故意不在 `updateFade()` 里**（`:316` 注释 `handles this itself`）——它自己在 `update()` 里算正弦。
   照着它的样式给新元素写「自己管 alpha」是可以的，但要清楚代价：**它不受 `uiAlpha` 控制**。
3. `updateFade()` 没有对 `btnNews` 做任何处理（`btnNews.enable/alpha` 在 `:322/:331`）——
   被注释掉的 `add()` 不妨碍这些调用，因为对象存在；但如果你**删掉** `btnNews` 字段，这几行会编译失败。

### 4.1 `enable(false)` 与 `alpha(0)` 不是一回事（**改收起逻辑必读**）

`updateFade()` 对每个按钮**同时**调 `enable(alpha != 0)` 和 `alpha(alpha)`（`:318-334`），这不是冗余：

| 调用 | 效果 | `active` | 还点得动吗 |
|---|---|---|---|
| `enable(false)` | 可见但**压到 30% 亮度**（`StyledButton:113-117`：`text.alpha(0.3f); icon.alpha(0.3f);`） | `false` | ❌ 不能（含悬停、拖拽、键盘） |
| `alpha(0)` | **完全不可见** | 不变 | ✅ **仍然能点、仍有 tooltip** |

⇒ **只写 `alpha(0)` 会在屏幕上留下一个看不见但吃点击的按钮**。
`Button.update()`（`:155`）只把 `hotArea.active = visible`，而 `PointerArea.onSignal` 还会查 `isActive()`
（`PointerArea.java:61-63`），`isActive()` = `active && parent.isActive()` ⇒ `enable(false)` 才真正封住输入。
项目里 `InterlevelScene:318-323` 是**只用 alpha** 的反例，别照抄到标题界面。

（`IconButton` 没有自己的 `alpha()`；`ExitButton` 的宽度是 20，横屏非桌面时为 40，高 20。）

## 5. 交互流程

| 触发 | 动作 | 目标 | 行号 |
|---|---|---|---|
| `btnPlay` 点击（**无存档**） | 清职业选择、`curSlot=1` | `switchScene(HeroSelectScene)` | 149–153 |
| `btnPlay` 点击（**有存档**） | — | `switchNoFade(StartScene)` | 154–156 |
| `btnPlay` **长按**（仅 debug 构建） | 同上走选人 | `HeroSelectScene` | 159–169 |
| `btnSupport` | — | `switchNoFade(SupporterScene)` | 487–490 |
| `btnRankings` | 顺带清 `Dungeon.daily/dailyReplay` | `switchNoFade(RankingsScene)` | 179–185 |
| `btnJournal` | — | `switchNoFade(JournalScene)` | 189–191 |
| `btnNews` | — | `switchNoFade(NewsScene)` | 389–392（**当前不可达**） |
| `btnChanges`（有更新） | 弹 `WndOptions`「更新 / 近期更新界面」 | `Updates.launchUpdate()` 或 `ChangesScene` | 419–445 |
| `btnChanges`（无更新） | 直接进 | `switchNoFade(ChangesScene)`（设 `changesSelected=0`） | 441–444 |
| `btnSettings`（语言未完成） | 先设 `WndSettings.last_index = 5` | `scene().add(new WndSettings())` | 471–476 |
| `btnAbout` | — | `switchScene(AboutScene)` | 210–212 |
| `btnExit` | — | 退出游戏（仅桌面） | `ui/ExitButton.java` |
| `fadeResetter` | 见 §4 | — | 273–291 |

注意：`switchScene` 有淡入淡出，`switchNoFade` 没有。`btnPlay` 无存档走的是 `switchScene`，
其余一律 `switchNoFade`——**改跳转目标时别顺手把这两个搞混**，表现为「切场景闪一下」。

## 6. 动态行为（都靠按钮子类覆写 `update()`）

| 元素 | 行为 | 触发条件 | 行号 |
|---|---|---|---|
| `NewsButton` | 拉取新闻；首次进入记 `newsLastRead`；显示未读数 `(N)`，上限 9；未读时标题色呼吸闪烁 | `SPDSettings.news()` | 355–393（当前**不可达**，未 `add`） |
| `ChangesButton` | 拉取更新；有更新时把文字换成 `update` 键并呼吸闪烁 | `SPDSettings.updates()` | 395–447 ✅ |
| `SettingsButton` | 语言未完成时图标换成 `LANGS` 并 `hardlight(1.5f,0,0)`（红）+ 文字呼吸闪烁；点击时预置设置页签 5 | `Messages.lang().status() == X_UNFINISH` | 449–477 ✅ |
| `SupportButton` | 固定 `Icons.GOLD` + `Window.TITLE_COLOR` 文字色 | — | 479–491 ✅ |
| `signs`（发光层） | 自己算 `am = max(0, sin(t))`，且 `min(am, title.am)`；`t` 每 1.5π 归零 | 每帧 | 125–140 ✅ |
| `version` | 静态文本，`hardlight(0x888888)` | — | 246–251 ✅ |

呼吸闪烁统一写法：`ColorMath.interpolate(0xFFFFFF, 目标色, 0.5f + sin(Game.timeTotal*5)/2f)`。

## 7. 文本键（**类名小写且不加下划线**）

键前缀是 `scenes.titlescene.` —— 是 `TitleScene` 小写，**不是** `title_scene`。
文件：`core/src/main/assets/messages/scenes/scenes.properties`（en）与 `scenes_zh.properties`（zh）。

| 键（zh 行号 / en 行号） | 中文 | 英文 | 用途 |
|---|---|---|---|
| `scenes.titlescene.play`（129/129） | 开始 | Play | **当前代码未引用**（遗留键） |
| `.enter`（130/130） | 进入地牢 | Enter the Dungeon | `btnPlay` |
| `.rankings`（131/131） | 排行榜 | Rankings | `btnRankings` |
| `.journal`（132/132） | 日志 | Journal | `btnJournal` |
| `.news`（133/133） | 游戏新闻 | News | `btnNews` |
| `.changes`（134/134） | 改动 | Changes | `btnChanges` |
| `.update`（135/135） | 更新 | Update | 有更新时的按钮文字 |
| `.install`（136/136） | 安装 | Install | 更新流程用；**本场景内无消费点**（见 §13） |
| `.settings`（137/137） | 设置 | Settings | `btnSettings` |
| `.about`（138/138） | 关于 | About | `btnAbout` |
| `.support`（139/139） | 支持游戏开发 | Support the Game | `btnSupport` |
| `.patreon_body`（145/145） | 长文 | — | `SupporterScene` 用（非本场景） |
| `.patreon_button`（146/146） | Patreon赞助页面 | Patreon Page | 同上 |
| `scenes.titlescene$changesbutton.title`（140/140） | 检测到新版本！ | An Update is Available! | 更新弹窗标题 |
| `…$changesbutton.versioned_title`（141/141） | 最新版本：%s | Update Available: %s | 带参——**字面 `%` 要写 `%%`** |
| `…$changesbutton.desc`（142/142） | 多段长文 | — | 更新说明 |
| `…$changesbutton.update`（143/143） | 前往更新详情页 | Go to Update Page | 弹窗按钮 |
| `…$changesbutton.changes`（144/144） | 近期更新界面 | Current Changes Screen | 弹窗按钮 |

键是怎么拼出来的（`messages/Messages.java:128-133`）：
`c.getName()` 去掉包名前缀 → 拼 `"." + k` → **整体 `toLowerCase(Locale.ENGLISH)`**。
所以 `TitleScene` → `scenes.titlescene.enter`，嵌套类 `TitleScene$ChangesButton` → `scenes.titlescene$changesbutton.*`。
键缺失时会**沿父类链上找**（`:141-142`）。

⚠️ **本场景是"全本地化"的，与 mod 的自定义职业完全不同**：
`TitleScene.java`、`PixelScene`、`TitleBackground`、`BannerSprites`、`Fireball`、`StyledButton`、`IconButton`、
`ExitButton`、`Button`、`Chrome` **全部零中文硬编码**；`messages/scenes/` 里也没有任何 `EGOPD`/`egopd` 键
（那 5 处中文只在 `Icons.java` 的注释里）。⇒ 改标题界面文字，**只动 properties，不动代码**。

新增文本要加 **三份**，各自独立维护、行号并不对齐（实测每份都有 **18** 个 `scenes.titlescene.*` 键）：

| 文件 | 用途 |
|---|---|
| `messages/scenes/scenes.properties` | 英文 base（缺键时的最终回退） |
| `messages/scenes/scenes_zh.properties` | 简体 |
| `messages/scenes/scenes_zh-hant.properties` | 繁体（`enter=進入地下城`、`changes=改動`） |

`\n` 只能是反斜杠 n（写 `/n/` 不报错也不换行）；
带参文本（如 `versioned_title` 的 `%s`）字面 `%` 要写 `%%`。
**中文文本不要用 `BitmapText(pixelFont)`**（`pixelFont` 只有拉丁字形，`PixelScene:89`）
—— 用 `PixelScene.renderTextBlock(size)`，它返回 `RenderedTextBlock`（是个 `Component`，
定位要走 `setPos`，尺寸要读 `width()/height()` 方法）。`version` 那个 `BitmapText` 之所以能用，
是因为内容 `"v" + Game.version` 全是拉丁字符。

## 8. 资源依赖

| 资源 | 常量 | 实际文件与尺寸 |
|---|---|---|
| 标题 + 发光贴图 | `Assets.Interfaces.BANNERS` | `assets/interfaces/banners.png`，**512×256**，uvRect 写死 |
| 按钮 9-patch | `Chrome.Type.GREY_BUTTON_TR` | `assets/interfaces/chrome.png`，**128×64** |
| 图标 | `Assets.Interfaces.ICONS` | `assets/interfaces/icons.png`，**256×128** |
| 标题音乐 | `Assets.Music.THEME_1/2` | `music/theme_1.ogg`（403 KB）、`theme_2.ogg`（375 KB），**均为真 Ogg Vorbis**（`OggS` 魔数已验证） |
| 位图字体 | `Assets.Fonts.PIXELFONT` | `fonts/pixel_font.png`，**1024×8**，`LATIN_FULL` |
| 背景视差 | `splashes/title/{archs,back_clusters,mid_mixed,front_small}.png` | 333×100 / 450×250 / 273×242 / 112×116（`TitleBackground:47-65`） |
| 火把 | `effects/fireball-tall.png` / `fireball-short.png` | 横屏 **61×61**/帧（256×512）、竖屏 **47×47**/帧（256×256），各 24 帧 @24fps 循环 |

细节：

- **`Chrome.Type.GREY_BUTTON_TR`**：`Chrome:56-58` 返回 `new NinePatch(Asset, 20, 9, 9, 9, 4)` ——
  9×9 的源矩形 + **四边各 4px margin** ⇒ 按钮**每轴至少 8px** 才画得出九宫格。`BTN_HEIGHT = 20` 满足。
  未在 enum 中列出的 `Chrome.Type` 会返回 `null`（`:81-82`）⇒ 用错直接 NPE。
- **`Icons` 不是 `TextureFilm` 帧号，而是硬编码像素矩形**：每个常量在 `Icons.get()` 里写
  `icon.frame( icon.texture.uvRectBySize(x, y, w, h) )`，**枚举 ordinal 不参与渲染**。本场景用到：

  | 常量 | 声明/分支行 | 矩形 | 尺寸 |
  |---|---|---|---|
  | `ENTER` | `Icons.java:37/149-151` | `(0,0,16,16)` | 16×16 |
  | `GOLD`（支持按钮） | `:38/152-154` | `(17,0,17,16)` | 17×16 |
  | `RANKINGS` | `:39/155-157` | `(34,0,17,16)` | 17×16 |
  | `NEWS` | `:41/161-163` | `(68,0,16,15)` | 16×15 |
  | `CHANGES` | `:42/164-166` | `(85,0,15,15)` | 15×15 |
  | `PREFS` | `:43/167-169` | `(102,0,14,14)` | 14×14 |
  | `SHPX`（关于） | `:44/170-172` | `(119,0,16,16)` | 16×16 |
  | `JOURNAL` | `:45/173-175` | `(136,0,17,15)` | 17×15 |
  | `LANGS` | `:54/198-200` | `(80,16,14,11)` | 14×11 |
  | `CHEVRON`（下折箭头） | `:64/228-230` | `(240,16,13,10)` | 13×10 |

- **新增一个标题界面按钮图标很便宜**：`icons.png` 里 **`x = 154..255, y = 0..15` 是整片空白**
  （0 个非透明像素），紧挨 `JOURNAL` 右缘（终点 x=152）⇒ 按 17px 间距还能放约 6 个。
  加图标的固定三步：① 在 enum 里加常量（顺序随意，ordinal 不读）；② 在 `get(Icons)` 加一个 `case`
  写绝对像素矩形；③ 在 `icons.png` 的**同一坐标**画出图案。标题行附近的间距惯例见 `Icons.java:36` 的注释。
- **`TitleBackground` 是会动的**：6 层视差 + 一层渐变遮罩，`update()` 里按 `Game.elapsed * SCROLL_SPEED(15)`
  逐层向下滚并加速（`shift *= 1.33/1.5`），出屏后回收换帧。竖屏时 `scale /= 1.5f`、`shift /= 1.5f`。
  它有**静态缓存**，`reset()` 由 `PixelScene:106` 在「从游戏内回菜单」时调用 ⇒ 改它要考虑缓存失效。
> **当前状态（2026-09-21）**：两张火焰图已改为**整体亮红色** （`--mode ramp`，暗端 (96,6,8) → 亮端 (255,74,52)）。此前试过的「黑色为主 + 掺杂金黄」方案**已回退**（先 restore 回上游原版再改红）。
> 原图备份 `_chk/_bak_fireball/`；还原 `python _chk/verify_fireball.py --restore`；核验 `python _chk/verify_fireball.py`（19 项，按 `RECOLORED.txt` 里的模式断言）。
> 注意 `Fireball` 也被 `WelcomeScene:240` 使用 ⇒ 开场界面的火把一起变红。**横幅与滚动背景本次未动。** 完整记录见 `docs/features.md`。
- **`Fireball` 是 `MovieClip`**，无参构造靠一个静态 `second` 让相邻两只**交替镜像**（`Fireball:30-35`），
  第二只故意把 `curFrame` 从 12 起播。`placeTorch()` 把 X 居中、**底边**锚在 `(x,y)`。
- **角色专属 BGM 不影响本场景**：`HeroBgm.track()` 在 `Dungeon.hero == null` 时返回 `null`，
  标题界面天然走原版 `THEME_1/THEME_2`。改动标题音乐时别去动 `HeroBgm`。

## 9. EGOPD 相对上游的**唯一**改动（`git diff HEAD` 实证）

```diff
 		btnNews = new NewsButton(GREY_TR, Messages.get(this, "news"));
 		btnNews.icon(Icons.get(Icons.NEWS));
-		add(btnNews);
+		//btnNews disabled in this custom build
+		//add(btnNews);
```

全文件相对上游 SPD v3.3.8 **只差这一处**（2 增 1 删）。后果：

1. 新闻按钮不可见 ⇒ 「游戏新闻」条目消失（`NewsScene` 变成不可达）。
2. 但按钮对象仍在、`updateFade()` 仍在对它调 `enable()/alpha()`（`:322/:331`），**不会崩**。
3. `NewsButton` 构造器里的 `News.checkForNews()` **仍然执行**（`:359`，构造函数在 `add` 之前跑）
   ⇒ 网络请求照发，只是界面不体现。
4. **竖屏第 3 行左半变空洞**，横屏第 2 行第 3 列变空洞（§3.3）。

## 10. 改动风险清单

| 风险 | 现象 | 护栏 |
|---|---|---|
| 新元素忘写进 `updateFade()` | 收起 UI 后它仍显示 | 改完 grep `updateFade` 对照元素清单 |
| **只用 `alpha(0)` 收起、没 `enable(false)`** | 按钮看不见但**还能点**、还有 tooltip | 两者配对调用，见 §4.1 |
| 新增一行按钮 | **竖屏最矮机型只剩 14px 余量**，加一行（22px）即溢出 | 改前改后各跑 `_chk/title_layout_calc.py`（横屏宽裕，竖屏吃紧） |
| 放大标题横幅 | `topRegion` 抬升 ⇒ 直接吃掉按钮余量 | 同上；`uvRect` 是写死的，改图必须同步改 |
| 删掉 `btnNews` 字段 | 编译失败（`updateFade` 与布局链都引用它） | 要么保留字段，要么同步清 3 处引用（`:230`、`:241–243`） |
| 移动 `btnChanges/btnSettings/btnAbout` | 竖屏 x 由 `btnNews` 矩形推导，改一个牵动三个 | 改成绝对坐标，别继续挂在 news 上 |
| `add()` 顺序调错 | 被别的元素盖住（无 z 字段可救） | z 序 = add 顺序（但**点击序相反**，见 §2.1） |
| **新加默认 `PointerArea` 做全屏遮罩** | 把**所有**按钮的点击吞掉（它比按钮先拿到事件） | 照抄 `fadeResetter`：覆写 `onSignal` 并 `return false`；或明确就是要模态才 `return true` |
| 给「未显示」的控件设了 rect | 它**开始吃点击**（`btnNews` 只因矩形恒 0×0 才无害） | 隐藏控件别给尺寸，或保持 `enable(false)` |
| 用 `Component` 做容器 | 见 §11 的铁律（本项目在 UI 上栽过三次） | 优先用 `Visual`；新容器不写 `createChildren()` |
| 用 `BitmapText(pixelFont)` 显示中文 | 中文变空白/方块（`LATIN_FULL` 无字形） | 用 `PixelScene.renderTextBlock(size)` |
| 对 `WelcomeScene` 用 `switchNoFade` | `PixelScene.noFade` 静态标志泄漏给下一个场景 | `WelcomeScene` 不调 `fadeIn()`，别对它用 noFade |
| 改 `getCommonInsets()` / `landscape()` | 13 个场景连带受影响 | 标题界面内自己算，别改基类 |

## 11. 本项目 UI 的三条铁律（来自 `docs/handbook/pitfalls.md` §6，改动前必读）

1. **自包含 UI 组件别自己 `new Camera`** —— `Camera` 成员归 `Scene`；
   只有静态 `Camera.add(camera)`，没有 `camera.add(...)`。自制窗口一律做普通 `Component` 交给宿主 scene。
2. **`Component` 是 `Group` 不是 `Visual`** —— `setRect/setSize` 返回 `Component`、**不能协变覆写**；
   `Visual` **没有** `setRect`（只有 `x/y/width/height/scale` 字段）。
3. **UI 组件构造即崩（NPE，编译却全过）** —— Java 实例字段初始化在 `super()` **之后**才跑，
   而基类构造函数里就调了 `createChildren()` ⇒ 在 `createChildren()` 里摸自己的字段＝摸到 `null`。
   **建子元素一律放子类构造函数体。** 静态回归：`python _chk/verify_ui_ctor_order.py <目录>`
   （注意它默认只扫 `debug-console/src`，改 core 时要显式传目录）。

## 12. 常见改动方向 → 要动哪里（供你圈定）

| 若要做 | 落点 |
|---|---|
| 让「改动」占回 news 的空位 | `TitleScene:241–243`（竖屏那条链）+ `:230–233`（横屏），把 `btnChanges` 改挂 `btnJournal` |
| 干脆启用新闻按钮 | 取消 `:198–199` 注释；注意 `News` 在 **独立 `services` 模块**，源码级核验时 classpath 必须带 `services/build/classes/java/main`（否则甩 19 个假错误，见 `AGENTS.md` §1） |
| 新增一个按钮 | ① 加字段 + `add()`（位置见 §2）；② 写进 `updateFade()`；③ 进竖/横两条排布链；④ 跑复算器确认不溢出；⑤ 需要新图标见 §8（`icons.png` 的 `x=154..255, y=0..15` 是空白区） |
| 改标题贴图尺寸 | 只能改 `interfaces/banners.png`；`uvRect` 写死在 `BannerSprites:41–52`，**改了图必须同步改 uvRect** |
| 整屏重排 | 建议顺手把 `GAP` 的 `(landscape()?3:4)` 改成实际行数（横 3 / 竖 5），否则竖屏间距一直偏紧 |
| 加平台/分辨率适配 | 现有代码全靠 `Camera.main` + `getCommonInsets()`；`getCommonInsets()` 会 `scale(1f/defaultZoom)`，见 `PixelScene:413–421` |

## 13. 未验证 / 待实测

- **真实机型上的 `Camera.main` 尺寸**：§3.4 的 119px / 203px 与三档实测值都是公式推导 + `_chk/title_layout_calc.py` 复算，
  **没有在真机/桌面上量过**。要确认，最省事的办法是在 `create()` 里临时打一行
  `GLog.i("camera %dx%d", Camera.main.width, Camera.main.height)` 跑一次桌面。
  （`_chk/title_layout_calc.py` 接受任意尺寸，量到之后直接喂给它即可。）
- `btnFade` 的 y 用的是 `camera.main.height - 16 - insets.bottom`（`:270`），**没有减 top inset**，
  与其余元素用的 `insets.top + h`（`:97–113`）口径不同 —— 有顶栏刘海时可能偏低。待实机确认。
- `btnExit.setPos(w - btnExit.width(), 0)`（`:295`）用的是**减去左右 inset 后的 `w`** 当绝对坐标，
  有左右安全区时可能贴边。待实机确认。
- `StartScene.SaveSlotButton.createChildren()`（`StartScene:193-205`）**在 `createChildren()` 里给字段赋值**
  （`bg = Chrome.get(...)`、`name = ...`）—— 正是 §11 第 3 条铁律描述的「形态②」。
  **是否真被初始化器覆盖成 `null` 未做运行时验证**（不过它现在能正常工作，说明至少当前无害）。
  若你之后动 `StartScene`，这是第一个要断言的点（判据用 `Gizmo.parent == this`，不能只判字段非空）。
- `HeroSelectScene:381-382` 有个 `new PointerArea(0, Camera.main.width - insets.bottom, Camera.main.width, insets.bottom)`
  —— **`Camera.main.width` 被当成 y 传进去了**，看着像笔误。是否故意（用于顶住底部安全区条）未验证。
- `scenes.titlescene.install`（`scenes_zh.properties:136`）**的消费点未找到**（不在 `TitleScene` 内）；用 grep `"install"` 全仓确认后再改它。
- `btnNews` 的 `hotArea` 虽已注册但因矩形恒 `0×0` 而无害 —— 这是**读源码推出**的结论，未运行时验证。

## 14. 已有文档里的相关记载（避免重复踩）

| 记载 | 位置 | 要点 |
|---|---|---|
| `TitleScene` 的 javac 假错误 | `AGENTS.md` §1 | 源码级核验时 classpath 漏 `services/build/classes/java/main` 会甩 19 个「找不到符号」，全落在 `scenes/TitleScene.java` 等三处 |
| 上游本就建议注释掉按钮 | `docs/recommended-changes.md:35,57` | 上游「自制改版建议」里**自己**就写了注释掉 `add(btnSupport);` / `add(btnNews);` —— 本项目的 `btnNews` 改动**照抄的是上游建议**，不是自创 |
| 存档槽/排行榜职业图标 | `docs/features.md:3676`、`docs/handbook/spd-framework.md:47,112` | 走 `ui/Icons.get(HeroClass)`，**与 `TitleScene` 无关**（`TitleScene` 只用了 `Icons.get(Icons.*)` 那种枚举重载） |
| 标题界面保持原版 BGM | `docs/features.md:4004-4006` | 因为这些界面都把 `Dungeon.hero` 置 `null`，`HeroBgm` 自然不介入 |
| UI 三条铁律 | `docs/handbook/pitfalls.md:49`、§6 精简表 | `Camera` 归属 / `Component` 非 `Visual` / `createChildren()` 构造顺序 |
| 中文标签别用 `BitmapText` | `docs/handbook/pitfalls.md:84` | 用 `PixelScene.renderTextBlock(size)` |
| `Group.draw()` 按 add 顺序 | `docs/handbook/wand-buff-fx-dev.md:23` | 只有这一处提到绘制顺序；**指针分发顺序全仓无记载**（§2.1 是首次记录） |
| 回归脚本 | — | **`_chk/` 里没有任何脚本断言 `TitleScene`**（`grep TitleScene _chk/` 仅命中一个过期备份文档）；`_chk/verify_ui_ctor_order.py` 默认只扫 `debug-console/src`，**不覆盖 core** |
| ⚠️ 现有文档里的一处**文件误名** | `docs/android-apk-branding-guide.md:135` | 那里写「标题界面用的是 `assets/interfaces/title.png` 之类的图片 logo」，但**仓里没有 `title.png`** —— 标题横幅实际取自 `interfaces/banners.png` 的硬编码 uvRect（见 §8）。改标题图时别去找 `title.png` |
| 场景重启也会回到 WelcomeScene | `Game.java:200` | `destroy()` 里 `sceneClass = null` ⇒ `finish()`/重启后同样先走 `WelcomeScene`，不是直接进标题 |
| `add()` 会复用空槽 | `Group.java:99-122` | 「后加的永远在末尾」只在没有空槽时成立；且 `resetScene()` 重跑的是**最后一次 switch 的目标类**（`Game.java:210-212`） |

## 15. 改「横幅 + 发光图 + 滚动背景」的落点清单

### 15.1 横幅 + 发光图

| 要动的东西 | 文件 | 说明 |
|---|---|---|
| **美术** | `core/src/main/assets/interfaces/banners.png` | **512×256**，四块标题/发光图是**同一张图**里的四个区域 |
| **代码（裁切区）** | `effects/BannerSprites.java:41–52` | 四个 `uvRect` 是**写死的像素坐标**，改版面就要改这里 |
| **代码（摆放/尺寸联动）** | `scenes/TitleScene.java:107–143` | `topRegion = max(标题高-6, h*0.45)` 直接吃横幅高度，改尺寸会推挤按钮区（见 §3.4） |

`banners.png` 里四个标题区域的**确切坐标**（尺寸 = 宽×高，注意后两参是右下角）：

> **当前版面（2026-09-21 用户手工重排，本节已同步为最新值）**：闪烁帧**保留但置空**。

| Type | uvRect | 尺寸 | 用在哪 |
|---|---|---|---|
| `TITLE_PORT` | `(0, 0, 169, 94)` | **169×94** | 竖屏标题（`TitleScene:107`） |
| `TITLE_LAND` | `(176, 0, 427, 65)` | **251×65** | 横屏标题 |
| `TITLE_GLOW_PORT` | `(253, 66, 422, 160)` | 169×94 | **空帧**（指向实测全透明区；尺寸与标题帧一致，便于日后叠加） |
| `TITLE_GLOW_LAND` | `(253, 66, 504, 131)` | 251×65 | **空帧** |

⚠️ **两个待定夺的实测问题**（详见 `docs/features.md` 同日批次）：
1. **横幅变宽会被裁**：`TitleScene:112` 是 `x = left + (w - 横幅宽)/2`，**相机宽 ≥ 横幅宽才不裁**。
   新版 169（竖）/ 251（横）都大于各模式最小相机宽 135 / 240；常见手机竖屏相机宽约 144~160
   ⇒ **会被裁 9~25 px**（旧版 139 宽不会）。要避免需把竖屏画面宽压到 ≤ ~144。
2. **两块标题画面底边比帧多 1 行**（满宽、alpha 255）：补上则 y1 各 +1 →
   `(0,0,169,95)` / `(176,0,427,66)`。

自检：`python _chk/verify_banners.py`（18 项，关键断言是「空帧区域必须真透明」）；
预览：`python _chk/render_title_preview.py --devices`（模拟渲染，供人工看效果）。

⚠️ **这张图不是标题界面独占的**，同图另有两块被游戏内界面使用：

| Type | uvRect | 尺寸 | 用在哪 |
|---|---|---|---|
| `BOSS_SLAIN` | `(0, 157, 127, 225)` | 127×68 | `GameScene:1646` —— **击杀 Boss 的横幅** |
| `GAME_OVER` | `(128, 157, 256, 192)` | 128×35 | `GameScene:1592` —— **游戏结束的横幅** |

⇒ **重绘整张 `banners.png` 会连带改掉游戏内的 Boss 击杀/游戏结束横幅**。
标题四块只占 `x=0..480, y=0..157`；重绘时必须把 `BOSS_SLAIN`/`GAME_OVER` 那两块原样保留。
另有 `x=480..512`（32px）与 `y>192` 的边角是空的，可用。

⚠️ **横幅也用在开场界面**：`WelcomeScene:109,127` 用的是**同样这四种 Type**
⇒ 改动会同时影响冷启动的开场界面（见 §1.1）。

**判定**：只重绘、位置与尺寸都不动 ⇒ **只改 PNG**；要挪位置或改尺寸 ⇒
必须同步改 `BannerSprites` 的 uvRect，并跑 `_chk/title_layout_calc.py` 确认按钮区没被挤出去。

### 15.2 滚动背景（4 张图 + 一处代码）
> **已执行（2026-09-21）**：这 4 张图已改为深红双色调，横幅未动；原图备份在 `_chk/_bak_title_bg/`，还原用 `python _chk/verify_title_bg.py --restore`。核验 `_chk/verify_title_bg.py`（39 项）。完整记录见 `docs/features.md`。

美术四张全在 `core/src/main/assets/splashes/title/`。**帧尺寸写死在 `TitleBackground.java` 的四个 `TextureFilm` 里**，
实测尺寸与网格如下（`cols = texWidth / 帧宽`，**整数除**）：

| PNG | 实测尺寸 | 帧尺寸（写死行号） | 网格 | 可用帧 | **实际用到的帧** |
|---|---|---|---|---|---|
| `archs.png` | 1024×256 | 333×100（`:47`） | 3×2 | 6 | **6**（用满） |
| `back_clusters.png` | 512×512 | 450×250（`:52`） | 1×2 | 2 | **2**（用满） |
| `mid_mixed.png` | 2048×1024 | 273×242（`:65`） | 7×4 | 28 | **24** |
| `front_small.png` | 1024×512 | 112×116（`:60`） | 9×4 | 36 | **20** |

「用到的帧」来自四个权重数组，**数组长度就是帧号取值范围**：

| 数组 | 行号 | 长度 | 含义 |
|---|---|---|---|
| `INIT_ARCH_CHANCES = {5,5,2,2,2,2}` | `:284` | 6 | 拱门层，6 帧各有权重 |
| `INIT_CLUSTER_CHANCES = {2,2}` | `:343` | 2 | 远景簇层，2 帧 |
| `INIT_MID_CHANCES`（24 个 `1`） | `:470` | 24 | 中景层，24 帧 |
| `INIT_SMALL_CHANCES`（20 个 `1`） | `:606` | 20 | 近景小物层，20 帧 |

取帧逻辑是「按权重随机取，取过就减权重，减完重新填满」⇒ 一组内所有帧都会轮到，
所以**美术必须把这 N 帧都画出来**，只画前几帧会让画面单薄（但不报错）。

**代码侧要改的文件与行**：

| 文件 | 行 | 何时必须改 |
|---|---|---|
| `ui/TitleBackground.java` | `:47`、`:52`、`:60`、`:65` | **换了 PNG 尺寸或改了帧尺寸就必改**（四个 `TextureFilm` 的帧宽高） |
| `ui/TitleBackground.java` | `:284`、`:343`、`:470`、`:606` | 帧**数量**变了就必改（数组长度 = 帧数） |
| `ui/TitleBackground.java` | `:42` `SCROLL_SPEED = 15f`、`:267–277` 的 `*1.33/ *1.5` | 想调滚动速度/视差层次时（不是必须） |
| `Assets.java` | `:362–365` | **仅当改文件名/路径**（`Splashes.Title.ARCHS/BACK_CLUSTERS/MID_MIXED/FRONT_SMALL`） |
| `ui/TitleBackground.java` | `:124–129` 的暗化渐变 | 是**程序生成**的（`TextureCache.createGradient`），不走图片文件 |

⚠️⚠️ **帧数不匹配 = 运行期 NPE（编译期全过）**：
`TextureFilm.get(index)` 查不到就返回 `null`，而 `Image.frame(RectF)` 第一句就是 `frame.width()`
⇒ 解引用 null。触发点在 `update()` 的图层回收里（`arch.frame(getArchFrame())` 之类），
表现为「跑一会儿才崩」。**`INIT_*_CHANCES` 的数组长度绝不能超过实际帧数。**

⚠️ **网格是不规则的，别按「整齐网格」的直觉画**：
`archs.png` 的 1024/333 = **3**（右边 25px 用不到）、`back_clusters.png` 的 512/450 = **1**（右边 62px 用不到）。
索引映射是**行优先** `index = 行 × cols + 列`（`TextureFilm:62-70`）。
最稳的做法是**保持原尺寸不变、只重绘内容**；要改尺寸就让宽高是帧尺寸的整数倍。

⚠️ **`TitleBackground` 是 10 个界面共用的滚动背景**，不只标题界面：

```
TitleScene:101   WelcomeScene:100   StartScene:70（存档槽）  RankingsScene:79（排行榜）
JournalScene:85  NewsScene:67       ChangesScene:90           AboutScene:55
SupporterScene:59  SurfaceScene:104
```
另有 `TitleBackground.reset()` 由 `PixelScene:106`（从游戏内回菜单）与 `InterlevelScene:170` 调用。
⇒ **换这套图 = 同时换掉上面 10 个界面的背景。**
若只想改标题界面，得**新建一套资源 + 新组件**（照抄 `TitleBackground` 改资产路径），只替换 `TitleScene:101` 那一处。

> 侧面好处：这套图是**静态缓存**的（`archs/clusters/...` 都是 `static`），所以在菜单之间来回切时背景是连续的；
> 新开一套组件时若也想要这种连续性，得照抄它的 static + `reset()` 机制。
