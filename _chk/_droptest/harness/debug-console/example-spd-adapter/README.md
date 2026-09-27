# 参考适配层（EGOPD 的真实实现）

这 4 个文件是 **EGOPD 项目里真实的 SPD 适配层**，从 `com.shatteredpixel.shatteredpixeldungeon.debug`
包原样拷出来的，供你写自己项目的适配层时对照。

**注意：这个目录不在 Gradle 的源码集里**（不是 `src/main/java`），所以**不会参与编译**，
放着也不影响构建。它纯粹是参考实现。

| 文件 | 职责 | 你要照抄的部分 |
|---|---|---|
| `SpdDebugConsole.java` | 总装：装 host / 字体 / 全部页签；`isAvailable()`、`open()` | 接线方式、`isAvailable()` 的判据 |
| `SpdConsoleHost.java` | 命名（`Messages.titleCase`）、图标（`ItemSprite`/`BuffIcon`/`CharSprite`）、生成（`collect`/`Buff.affect`/选格刷怪） | **`ConsoleHost` 的 5 个方法怎么落地**，尤其是「防御性」写法 |
| `SpdConsoleProviders.java` | 10 个页签的包名配置 | `ConsoleProvider` 的写法、`ClassScanner.find` 的调用方式 |
| `SpdConsoleFonts.java` | 把 `PixelScene.pixelFont` 交给库 | 字体 SPI 的最小实现 |

## 重点看这几处

**1. 「猜着实例化」的防御性**（`SpdConsoleHost.icon` / `displayName`）

```java
try {
    Object sample = Reflection.newInstance(clazz);
    if (sample == null) return null;      // 构造失败：让该行退化成纯文字
    if (sample instanceof Item) return new ItemSprite((Item) sample);
} catch (Throwable ignored) {
    // 图标是可选品，任何失败都不该让整行消失
}
return null;
```

**2. 目标格不可用时的兜底**（`SpdConsoleHost.findSpawnCell`）

点地图拿到的格子可能是墙或已有怪物，先试原格、再试八邻格、最后回退原格。

**3. 「需要点地图」的类别怎么接管**（`SpdConsoleHost.requestPlacement`）

`hidesBeforeSpawning` 返回 `true` 的类别，会先关窗口再交给 `requestPlacement`；
后者返回 `true` 表示接管了放置流程，返回 `false` 则回落到普通 `spawn()`。

**4. 特殊类别的「加一层」便利**（`SpdConsoleHost.applyStackingConvenience`）

像「可叠层的 buff，一次点一层」这种调试便利，适合收在适配层里，
不要污染通用库——库不需要知道任何具体游戏概念。

## 一个易踩的构造顺序坑

`ConsolePanel` 的构造函数会调用 `createChildren()`。子类若在 `createChildren()` 里
访问**自己的实例字段**，那些字段此时还是 `null`（Java 的实例字段初始化在 `super()` 之后才跑）。

所以 `WndDebugConsole` 把「建页签 / 建面板」放在**自己的构造函数体内**，
而不是 `createChildren()` 里。你写自己的窗口子类时留意同样的顺序问题。
