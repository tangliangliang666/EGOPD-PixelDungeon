# 接入说明（DROP-IN）

把 `debug-console/` 整个目录放进你的项目即可。**不需要改本模块的任何源码**。

---

## 第 1 步：放进项目

解压后目录结构：

```
debug-console/
├── README.md              ← 完整功能说明与 API 参考
├── DROP-IN.md             ← 本文件
├── build.gradle
└── src/main/java/com/mypd/debugconsole/...
```

把这个 `debug-console/` 目录放到你项目根目录下（和 `settings.gradle` 同级即可）。

---

## 第 2 步：改两处构建文件

### `settings.gradle` —— 加一行

```gradle
include ':debug-console'
```

### 你的 core 模块 `build.gradle` —— 加一行依赖

```gradle
dependencies {
    implementation project(':debug-console')
}
```

### 如果这个模块报 `appJavaCompatibility` / `gdxVersion` 找不到

`build.gradle` 用了两个变量。大多数 SPD 派生工程在根 `build.gradle` 的
`allprojects { ext { ... } }` 里已经定义了它们（`gdxVersion`、`appJavaCompatibility`），
所以通常无需处理。

**若你的项目用了别的变量名**，在根 `build.gradle` 里补两个别名即可（推荐，别去改模块文件）：

```gradle
allprojects {
    ext {
        // 你项目里已有的名字 → 供 debug-console 使用
        gdxVersion           = gdxVersion           // 已有就删掉这行
        appJavaCompatibility = JavaVersion.VERSION_11  // 按你的实际值
    }
}
```

---

## 第 3 步：它依赖的「slim / noosa 层」怎么找

模块需要一套通用的 noosa 视觉层（`Visual`、`Group`、`Component`、`BitmapText`、
`PointerArea`、`ScrollArea`、`ColorBlock`、`Game`、`PointF`、`DeviceCompat`，
以及 `PointerEvent`/`ScrollEvent`）。在 EGOPD 里这是 `:SPD-classes` 模块。

`build.gradle` 按这个顺序解析它：

1. **构建属性**（推荐）—— 在你的 `gradle.properties` 里指定你自己的模块路径：

   ```properties
   debugConsoleSlimProject=:my-noosa-module
   ```

2. **默认值** `:SPD-classes` —— 如果你的项目里正好也叫这个名，什么都不用做。

3. **都找不到** —— 回退成「只依赖 libGDX」，编译本模块自身仍可过，
   但**运行时必须由你的项目提供 noosa 层**，否则会 `NoClassDefFoundError`。

> 这套顺序意味着：**同样的压缩包在 EGOPD 和别的 SPD 派生工程里都能直接编译**，
> 不需要为每个项目改模块源码。

---

## 第 4 步：写你自己的适配层（约 100 行）

模块本身不知道任何游戏类型，需要你实现两个接口。把下面两个文件照抄进你的项目、改掉里面的类名即可。

### 4.1 宿主：`ConsoleHost`

负责「命名 / 图标 / 生成」。

```java
import com.mypd.debugconsole.spi.ConsoleHost;

public class MyConsoleHost implements ConsoleHost {

    @Override public String displayName(Class<?> c) {
        // 返回 null 会自动回退到 Class.getSimpleName()
        return MyNames.of(c);
    }

    @Override public com.watabou.noosa.Image icon(Class<?> c) {
        return MyIcons.of(c);   // 返回 null → 该行只有文字
    }

    @Override public String spawn(Class<?> c) {
        MyWorld.spawn(c);       // 返回值会作为提示；返回 null 则不提示
        return null;
    }

    @Override public boolean hidesBeforeSpawning(Class<?> c) {
        return false;           // 需要「关窗→点地图」的类别才返回 true
    }

    @Override public boolean requestPlacement(Class<?> c, Runnable onPlaced) {
        return false;           // 见 4.3
    }
}
```

> **每个方法都必须防御性**：控制台会猜着枚举并实例化类，任何一层都可能抛异常。
> 返回 `null` 或抛异常都会被吞掉，对应行自动跳过，不会白屏。

### 4.2 选卡：`ConsoleProvider`

负责「列出什么」。一个 provider = 一个页签。

```java
import com.mypd.debugconsole.ClassScanner;
import com.mypd.debugconsole.spi.ConsoleProvider;

import java.util.List;

public class MyWeaponsTab implements ConsoleProvider {

    @Override public String tabName() { return "武器"; }

    @Override public List<Class<?>> classes() {
        // 运行时扫描：找出指定包下所有可构造的 Weapon 子类
        return ClassScanner.find("com.example.items.weapon", Weapon.class);
    }
}
```

`ClassScanner.find` 的三种形态：

```java
ClassScanner.find(String pkg, Class<?> base);                 // 单包，递归
ClassScanner.find(String[] pkgs, Class<?> base);              // 多包合并
ClassScanner.find(String pkg, Class<?> base, boolean shallow);// shallow=只扫包根目录
```

### 4.3 需要「点地图放置」的东西（比如刷怪）

```java
@Override
public boolean hidesBeforeSpawning(Class<?> c) {
    return Mob.class.isAssignableFrom(c);   // 怪物必须选格子，所以要先关窗
}

@Override
public boolean requestPlacement(final Class<?> c, final Runnable onPlaced) {
    if (!Mob.class.isAssignableFrom(c)) return false;   // 其它类型走普通 spawn()

    GameScene.selectCell(new CellSelector.Listener() {
        @Override public void onSelect(Integer cell) {
            if (cell != null) spawnMobAt(c.asSubclass(Mob.class), cell);
            onPlaced.run();
        }
        @Override public String prompt() { return "点击地图放置"; }
    });
    return true;   // 返回 true = 我接管了
}
```

---

## 第 5 步：启动时接线

```java
// 1) 字体（模块自身不带字体，必须由宿主提供）
Fonts.setProvider((text, size) -> {
    BitmapText t = new BitmapText(MyPixelFont.get());
    t.text(text);
    t.measure();
    return t;            // 返回 Visual
});

// 2) 宿主 + 选卡
DebugConsole.setHost(new MyConsoleHost());
DebugConsole.register(new MyWeaponsTab());
DebugConsole.register(new MyMobsTab());

// 3) 开关（默认已开启，这行可省）
DebugConsole.setEnabled(true);

// 4) 可选：页签多时排两行
DebugConsole.settings().rowsPerTab(2);
```

打开窗口（窗口是普通 `Component`，挂到场景上即可）：

```java
myScene.addToFront(new WndDebugConsole());
```

---

## 关于「必须在测试挑战下才生效」的限制

**压缩包里的版本已经完全去掉这个限制。** 是否可用只看：

- `DebugConsole.isEnabled()`（默认 `true`）
- 至少装了一个 host + 一个 provider

不再需要任何「调试模式挑战」「INDEV 版本号」之类的前提。
想接自己的设置项就装一个 `EnabledSource`：

```java
DebugConsole.setEnabledSource(() -> MySettings.debugConsole());
```

---

## 快速自检清单

| 现象 | 原因 |
|---|---|
| 编译报找不到 `com.watabou.noosa.*` | slim 模块没接上，见第 3 步 |
| 编译报 `appJavaCompatibility` 找不到 | 根 `build.gradle` 缺变量，见第 2 步 |
| 窗口能开但列表是空的 | `ConsoleHost` 没装，或 provider 的 `classes()` 返回空 |
| 列表有项目但没有文字 | `Fonts.setProvider` 没调（会退化成纯图标行） |
| 点条目没反应 | `spawn()` 返回前抛异常了——检查你的实例化逻辑 |
