# debug-console — 独立调试控制台

一个**与游戏无关**的运行时调试控制台：在游戏里开一个窗口，按分类列出「可生成的东西」，点一下就刷出来。

它原本是 EGOPD（SPD 魔改）内部的一个写死调试窗口，现已拆成独立 Gradle 模块
`:debug-console`：**不含任何游戏类型**（Item / Mob / Buff …），靠一套 SPI 接口与宿主对接，
所以可以直接搬到别的 libGDX 项目里用。

> **只想拿去用？** 直接看 [`DROP-IN.md`](DROP-IN.md)——那是给「解压即用」写的接入步骤。
> 本文件是完整的功能说明与 API 参考。

---

## 0. 分发与独立可用性

打包成 `debug-console-1.0.0.zip`（顶层目录 `debug-console/`），解压到任意项目根目录即可。

压缩包内容：

```
debug-console/
├── DROP-IN.md              解压即用接入步骤（先看这个）
├── README.md               本文件：功能说明 + API 参考
├── build.gradle            模块构建（slim 模块路径可被宿主属性接管）
├── src/main/java/com/mypd/debugconsole/...   模块本体（9 个类）
└── example-spd-adapter/    EGOPD 的真实适配层（参考实现，不参与编译）
```

**独立可用性已自动化验证**：`_chk/verify_dropin_zip.py` 会解压该 zip，
在「一个全新项目」里搭一个**名字故意不叫 `SPD-classes`** 的精简 noosa stub 层，
然后编译并**实际运行**一个端到端示例。当前结果：

```
[1/4] 解压 OK，顶层目录: ['debug-console']
[2/4] 宿主工程搭建 OK（slim 模块故意叫 :my-noosa）
[3/4] 编译 OK（模块未改一行源码）
[4/4] 运行 OK
      PASS: console built 150x200, scanning found 1 class(es)
```

即：**不改模块任何源码**，换一个 slim 模块名、换一个项目，也能直接编译运行。

---

## 1. 它做了什么

- **运行时类扫描**：给定「包名 + 基类」，反射找出所有可构造的具体子类。
  支持三种来源，自动合并去重：
  - 桌面：`java.class.path` 上的 `.class` 目录
  - 桌面/服务端：`.jar` / `.zip`
  - **Android：APK 内的 `classes*.dex`**（自带一个极简 dex 解析器，不需要 Android SDK）
- **可插拔选卡**：每个 `ConsoleProvider` 一个页签，按注册顺序排列，支持多行页签。
- **懒构建**：页签第一次被点开时才跑扫描，启动零开销；单个 provider 抛异常不会带崩整个窗口。
- **自包含 UI**：窗口底板、可滚动列表、拖拽/滚轮、页签全部自带，只依赖 libGDX + `:SPD-classes`
  的通用 noosa 层，**不依赖宿主的 Window / ScrollingListPane 等游戏 UI**。
- **无门控**：是否可用只是一个开关（`DebugConsole.setEnabled`），不再要求「调试挑战」「INDEV 版本号」之类的前提。

---

## 2. 五分钟接入

### 2.1 依赖

Gradle（同一构建内）：

```gradle
// settings.gradle
include ':debug-console'

// 你的 core 模块 build.gradle
dependencies {
    implementation project(':debug-console')
}
```

拿到 jar 后独立使用（非 Gradle 构建）也可以，只要 classpath 上有 libGDX 与 `:SPD-classes`。

### 2.1.1 外部依赖清单

模块刻意只依赖「通用引擎层」，实测 jar 内类的外部引用仅这些（可用 `jar` + 正则自行复核）：

- **libGDX**：`Gdx`、`Application`
- **noosa 通用视觉层**（`:SPD-classes`，不含任何游戏逻辑）：
  `Visual`、`Image`、`ColorBlock`、`Group`、`Gizmo`、`BitmapText`、`Game`、
  `ui.Component`、`PointerArea`、`ScrollArea`
- **noosa 输入/工具**：`PointerEvent`、`ScrollEvent`、`PointF`、`DeviceCompat`

**没有任何 `com.shatteredpixel.*` 引用**——这是拆分是否成功的硬判据。
若你的项目基于同一套 noosa 派生层，直接可用；若是纯 libGDX 项目，
只需补一个约 200 行的 noosa 兼容层（`Visual`/`Group`/`Component` 的等价物）。

### 2.2 实现两个接口

**描述「怎么生成」**——`ConsoleHost`：

```java
public class MyHost implements ConsoleHost {

    @Override public String displayName(Class<?> c) {
        // 用你自己的命名逻辑；返回 null 则回退到 Class.getSimpleName()
        return Namer.of(c);
    }

    @Override public Image icon(Class<?> c) {
        return Icons.of(c);        // 没有图标就返回 null，行会退化成纯文字
    }

    @Override public String spawn(Class<?> c) {
        return World.spawn(c);     // 返回一句提示，或 null 表示不提示
    }

    @Override public boolean hidesBeforeSpawning(Class<?> c) {
        return false;              // 若某些东西需要先关窗口再点地图，见下
    }

    @Override public boolean requestPlacement(Class<?> c, Runnable onPlaced) {
        // 需要「关窗口 → 点地图 → 放置」的类别（比如刷怪）在这里接管
        return false;              // 返回 false 表示不用接管，走普通 spawn()
    }
}
```

**描述「列出什么」**——每个页签一个 `ConsoleProvider`：

```java
public class MyWeapons implements ConsoleProvider {
    @Override public String tabName() { return "武器"; }

    @Override public List<Class<?>> classes() {
        return ClassScanner.find("com.example.items.weapon", Weapon.class);
    }
}
```

### 2.3 启动时接线

```java
DebugConsole.setHost(new MyHost());
Fonts.setProvider((text, size) -> {
    BitmapText t = new BitmapText(MyPixelFont.get());
    t.text(text);
    t.measure();
    return t;                 // 返回 Visual；返回 null 则该行只有图标
});

DebugConsole.settings().rowsPerTab(2);          // 页签较多时排两行
DebugConsole.register(new MyWeapons());
DebugConsole.register(new MyBuffs());

DebugConsole.setEnabled(true);                  // 或接你自己的设置项，见 3.1
```

### 2.4 打开窗口

```java
if (DebugConsole.isEnabled() && DebugConsole.isReady()) {
    myScene.addToFront(new WndDebugConsole());
}
```

窗口是普通 `Component`，挂到场景上即可（走场景自己的相机）。
配合一个快捷键（键盘轮询）或菜单按钮就能调出。

---

## 3. 开关与配置

### 3.1 启用开关

默认 **开启**（`DebugConsole.isEnabled()` 返回 true），所以把库丢进去就能用。

想让它跟着宿主自己的设置走时，装一个 `EnabledSource`，不要在业务代码里到处传 flag：

```java
DebugConsole.setEnabledSource(() -> MySettings.debugConsole());
```

`EnabledSource` 抛异常时会自动回退到静态开关，不会把调用方拖崩。

### 3.2 外观

`DebugConsole.settings()`：

| 方法 | 作用 |
|---|---|
| `rowsPerTab(int)` | 页签分几行（默认 2） |
| `windowSize(w, h)` | 固定窗口尺寸；传 `DebugConsole.DEFAULT_SIZE` 恢复自适应 |
| `listItemHeight(int)` | 每行高度（默认 18） |
| `iconBoxSize(int)` | 图标框边长（默认 16） |
| `labelSize(int)` / `titleLabelSize(int)` / `tabLabelSize(int)` | 条目 / 分组标题 / 页签文字逻辑字号 |
| `tabHeight(int)` | 页签高度（默认 25） |
| `panelBorder(boolean)` | 是否画控制台自己的 1px 边框（默认 true） |

窗口尺寸默认按横竖屏自适应（横屏 230×150 / 竖屏 150×200）。
要完全自定义底板与边框，继承 `ConsolePanel` 覆写 `createChrome()`。

> 读取这些值请用 `getLabelSize()` / `isPanelBorder()` 这类**带 get/is 前缀**的访问器：
> 设置器是流式的、已经占用了裸名（Java 不允许只按返回类型重载）。

---

## 4. 目录结构

```
debug-console/src/main/java/com/mypd/debugconsole/
├── DebugConsole.java          总入口：开关、provider 注册、host、设置
├── ClassScanner.java          类扫描（目录 / jar / dex）
├── WndDebugConsole.java       控制台窗口：页签 + 每页一个列表
├── spi/
│   ├── ConsoleHost.java       宿主回调：命名 / 图标 / 生成 / 放置
│   └── ConsoleProvider.java   页签数据源
└── ui/
    ├── ConsolePanel.java      自包含面板（底板 + 边框 + 居中）
    ├── ConsoleListView.java   可滚动列表（拖拽 + 滚轮 + 行点击）
    └── Fonts.java             字体 SPI + 安全访问helper
```

---

## 5. 关键约定与坑

- **`ConsoleHost` 的每个方法都必须防御性**。控制台会「猜着」枚举类并实例化它们，
  任何一层都可能抛异常——返回 `null` 或抛异常都会被吞掉，对应行自动跳过，不会白屏。
- **只收录可构造的类**：接口、抽象类、内部/匿名类（`$`）、没有无参构造器的类都会被过滤。
  这是刻意的——它保证列表里每一行都点得动。
- **图标会被 `copy()`**：宿主返回的 `Image` 不会被库持有或改写，可以放心复用同一实例。
- **`Fonts` 没装 provider 时不会崩**，只是列表变成纯图标行。字体的读取是**懒**的，
  所以 `PixelScene.pixelFont` 这类「启动后期才赋值」的字体也安全。
- **dex 类名表有缓存**（`ClassScanner.clearDexCache()` 可清）。正常 APK 运行期不会变，无需处理。
- **`shallow` 扫描**：只收包根目录下一层的类，用来扫 `items` 这种「根包里放关键物品、
  子包各自分类」的结构。桌面用目录层级判断，Android 用「类名前缀之后还有没有点」判断，
  两边结果一致。
- **`Visual` 没有 `setRect`**，`Component.setRect` 返回 `Component`：
  库内部对「监听器矩形」是直接写 `x/y/width/height` 字段的，改动这部分时留意这个 API 差异。
- **⚠️⚠️ 构造顺序坑（本模块已踩三次，头号陷阱）**：`ConsolePanel` 的构造函数里调用了
  `createChildren()`，而 Java 的**实例字段初始化在 `super()` 之后**才执行。于是子类在
  `createChildren()` 里做任何「给字段赋值 + 挂到场景」的操作，都会踩两种后果**之一**：

  | 交了什么 | 后果 | 症状 |
  |---|---|---|
  | 字段本身（如 `ArrayList panes`） | `super()` 阶段读到 `null` | 构造即 **NPE** |
  | 局部 new 出来再赋给字段（如 `bg = new ColorBlock(...)`） | 赋值被随后的字段初始化器**覆盖回 null** | **不报错**，但该字段永远是 null ⇒ `recolor()`/`layout()` 里那些 `if (x != null)` 全部静默跳过 |

  第二种最阴——**对象已经在场景里了，但拿它的字段是 null**，所以既不崩、也看不出原因。
  本模块实测栽在这上面的是：`Pane.list`（⇒ 一行都建不出来）、`Tab.bg`/`Tab.hitArea`
  （⇒ 页签底色与命中区永远不更新）、以及 `Row.icon`/`Row.label`（⇒ 每行只剩一条分隔横线，
  也就是「窗口开了但只有几条线」这个症状）。

  **唯一正确写法**：所有建子元素的动作都放进**自己的构造函数体**，`createChildren()` 留空。
  `WndDebugConsole` / `Pane` / `Tab` / `ConsoleListView.Row` 现在都是这个写法，改动时照抄。

  > 这个坑**编译期 100% 正常**（`createChildren()` 里写的代码语法完全合法）。
  > 唯一可靠的发现手段是**真跑起来并断言「子元素确实挂上了」**，见下一节的自检钩子。

- **⚠️⚠️ 字体接缝必须是 `Gizmo`，不能是 `Visual`（第二个头号陷阱，2026-09-21 踩到）**：
  `Fonts.Provider.create()` 的返回类型看着理所当然应该是 `Visual`（位图字不就是个视觉元素嘛）。
  **但那样就没法接 CJK 文本控件了。** 很多引擎把「一段文本」做成**容器**而不是单个视觉元素：
  SPD 的 `RenderedTextBlock` 就 `extends Component`（一个块内部持有若干逐词 `RenderedText`，
  外加可选删除线），因为一段文本本来就可能含多段样式与换行。

  这不是审美问题，是**能不能显示**的问题：

  | 做法 | 中日韩字形 | 结果 |
  |---|---|---|
  | `BitmapText(PixelScene.pixelFont)` | ❌ 字体按 `LATIN_FULL` 加载，注释写明 *Only latin characters supported*，缺字形落到 `get('?')` | 中文名**整列变问号** |
  | `PixelScene.renderTextBlock(7)` → `RenderedTextBlock` | ✅ 走 libGDX FreeType 管线，加载真实字体文件 | 正常显示 |

  本模块实测症状就是「**页签和图标都对了，但绝大部分名称都没有正确匹配**」——名字其实一直
  是对的（`Messages.titleCase(name())` 跟旧版一模一样），只是**没有字形可画**。因为
  `RenderedTextBlock` 是 `Component` 而不是 `Visual`，接缝一旦定成 `Visual`，
  宿主**在类型层面就不可能**把它交回来。

  **因此**：`Fonts.Provider.create` 返回 `Gizmo`；`Fonts.at/widthOf/heightOf`、
  `ConsolePanel.align`、`Row.squeezeToFit` 都同时支持两种形态——
  对 `Visual` 直接写 `x/y`，对 `Component` 走 `setPos`（**必须** `setPos`，
  否则控件自己的 `layout()` 不会跑，文字会偏位）与 `width()/height()` 访问器
  （`Component` 用 protected 字段遮蔽了 `Visual` 的公开字段，读不到）。
  缩放只对纯 `Visual` 生效，`Component` 的缩放交给它自己。

  > 命名同理：`Mob.sprite()` 就是 `Reflection.newInstance(spriteClass)`。**有些活体怪物
  > 的 `spriteClass` 直到被放进关卡才赋值**（`VaultMob` / `DemonSpawner` / `FungalCore`
  > 这类），于是这里拿到 `null`，libGDX 的 `ClassReflection.newInstance(null)` 直接抛 NPE。
  > 因为 `buildRows()` 是逐行 try/catch 的，**它不会崩整个窗口——只会把那一行静默吃掉**，
  > 玩家看到列表莫名其妙断在半途。所以宿主侧一定要兜底（SPD 侧回落到
  > `ItemSpriteSheet.MOB_HOLDER`），参考 `SpdConsoleHost.mobIcon()`。

- **自检钩子（专门为上面两个坑准备的）**：`WndDebugConsole.debugRows()` 返回当前页签的行，
  `ConsoleListView.Row.debugChildCount(CHILD_ICON|CHILD_LABEL)` 报告**该子元素是否真的
  `parent == this`**（是「真的加进去了」而不是「字段有值」——两者会背离，这正是坑的本质），
  `Row.debugLabelText()` 报告标签**实际携带的文本**（能抓到「控件在、但文本丢了」，
  也就是字体接缝接错那一类）。
  `_chk/verify_dropin_zip.py` 每次都会断言「所有行都挂上了 icon+label **且文本非空且非 ASCII 被保留**」，
  缺一即 FAIL（这个断言已用「把 provider 改成返回空文本」反测过，确认会变红）。

---

## 6. 在 EGOPD 里怎么接的（参考实现）

`:core` 侧的适配层全在 `com.shatteredpixel.shatteredpixeldungeon.debug`：

| 文件 | 职责 |
|---|---|
| `SpdDebugConsole.java` | 总装：装 host / 字体 / 全部页签；`isAvailable()`、`open()` |
| `SpdConsoleHost.java` | 命名（`Messages.titleCase`）、图标（`ItemSprite`/`BuffIcon`/`CharSprite`）、生成（`collect`/`Buff.affect`/选格刷怪） |
| `SpdConsoleProviders.java` | 10 个页签的包名配置（药水/卷轴/武器/远程/防具/神器/饰品/杂项/Buff/怪物） |
| `SpdConsoleFonts.java` | 把 `PixelScene.pixelFont` 交给库 |

入口两处（都已去掉挑战门控）：

- `ShatteredPixelDungeon.update()` —— 桌面按 **F2** 打开
- `WndGame` 菜单 —— 移动端「调试窗口」按钮

`Challenges.DEBUG_MODE` **仍然保留**，但它现在只做一件事：把本局分数倍率置 0（本局不计分）。
它**不再**控制调试窗口的可见性。

刷怪走的是「关窗口 → 提示点击地图 → 落点」的放置流程（`requestPlacement`），
其余类别直接生成。
