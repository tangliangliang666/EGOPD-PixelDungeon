# APK 图标与应用名（Android 打包指南）

> 适用：EGOPD（`D:\PD`）。定位：**改 APK 桌面图标 / 应用名时照这篇做，并知道哪些改动会动到存档。**
> 相关：`docs/terrain-creation-guide.md`（图集贴图）、`AGENTS.md` §1（构建）、§3（贴图与图标）。

---

## 0. 结论速览

| 问题 | 原因 | 处置 |
|---|---|---|
| 重画了图标，装上还是原版金绿 | **只改了 legacy 扁平图 `ic_launcher.png`**；Android 8.0+ 只认自适应图标 `mipmap-anydpi-v26/ic_launcher.xml` → `_background` + `_foreground` | 必须同时改 4 张分图层（见 §2） |
| 想把桌面名改成 EGOPD | 名字来自 `build.gradle` 的 `appName` → manifest 占位符 `${appName}` | 改 `appName` 即可，**移动端存档不受影响**（见 §4） |
| 担心改名丢存档 | Android 存档按 **applicationId** 定位，与显示名无关 | 安全。但桌面版存档按 `title` 定位，已解耦冻结（见 §4.3） |

---

## 1. 为什么「改了图标却不生效」

引用链只有一条：

```
AndroidManifest.xml
  android:icon="@mipmap/ic_launcher"
        │
        ├── res/mipmap-anydpi-v26/ic_launcher.xml   ← 【API 26+ 只走这条】
        │     <background @mipmap/ic_launcher_background>
        │     <foreground @mipmap/ic_launcher_foreground>
        │     <monochrome @mipmap/ic_launcher_monochrome>
        │
        └── res/mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.png  ← 【只在 API < 26 用】
```

**实测证据**（`aapt dump badging`，2026-09-18 本次核验）：

```
application-label:'EGOPD'
application-icon-120:'res/mipmap-anydpi-v26/ic_launcher.xml'
application-icon-160:'res/mipmap-anydpi-v26/ic_launcher.xml'
...
application-icon-65534:'res/mipmap-anydpi-v26/ic_launcher.xml'
```

六档密度**全部**指向同一个自适应图标 XML —— 密密麻麻的 `ic_launcher.png` 一张都不会被现代启动器读取。所以只重画那张 192×192 的扁平图，真机上一点变化都看不到。

> 自适应图标是「两张分图层叠起来再套遮罩」：`background` 铺满 108dp 画布（会被放大裁切，要画成可平铺/无接缝的纹理或纯色），`foreground` 只有中心 **72dp 安全区**内的内容保证可见（画布 108×108，四边各 18dp 会被各厂遮罩切掉）。SPD 原版把精灵画在中心约 52% 的范围内。

---

## 2. 图标资源清单（要改哪些文件）

### 2.1 每个密度一张，共 4 种 × 5 档

| 文件 | 边长（mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi） | 内容 |
|---|---|---|
| `ic_launcher.png` | 48 / 72 / 96 / 144 / 192 | legacy 扁平图（含背景，整张） |
| `ic_launcher_background.png` | 108 / 162 / 216 / 324 / 432 | 背景层，铺满、无透明 |
| `ic_launcher_foreground.png` | 108 / 162 / 216 / 324 / 432 | 前景层，透明底 + 精灵居中 |
| `ic_launcher_monochrome.png` | 108 / 162 / 216 / 324 / 432 | 单色剪影（Android 13+ 主题图标用），仅 alpha 有意义 |

另有 `mipmap-ldpi/ic_launcher.png`（36×36）—— 只有扁平图，没有分图层；ldpi 设备必然是 API < 26，够用。

### 2.2 ⚠️ 同一个文件要改两遍

本工程 `android/src/debug/res/` 下有一套 **与 main 完全重复** 的图标（upstream 原本给 debug 变体做了「INDEV」版图标，这里已被同步成一样）。debug 变体资源**覆盖** main，所以：

> **改图标必须 main + debug 各改一遍，共 2 套 × 4 种 × 5 档。** 漏掉 debug 会导致「debug APK 图标不更新、release 更新了」这类鬼打墙。

核验两套是否一致（md5 相同即一致）：

```bash
python _chk/png_palette_dump.py android/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png \
                               android/src/debug/res/mipmap-xxxhdpi/ic_launcher_foreground.png
```

---

## 3. 把「扁平图的重上色」同步到自适应前景

如果新版图标只是**换了配色、形状没动**，不必重画 `foreground`——它和 legacy 扁平图出自同一套像素画，可以逐像素搬运。

### 3.1 实测几何关系（本工程）

```
legacy 精灵 bbox (12, 12)-(179, 173)   168 x 162     ← 192 画布
fg     精灵 bbox (104,104)-(327,319)   224 x 216     ← 432 画布
缩放 = 168/224 = 162/216 = 0.750000      偏移 = (-66, -66)
```

各调色板色的像素数在两张图里严格成 **16:9**（= 0.75²），所以可以精确反查「fg 的每个像素对应 legacy 的哪个像素」。

### 3.2 为什么必须「按位置」而不是「按颜色」全局换

原版前景调色板里有**同一个旧色裂成多个新色**的情况：

| 旧色 | 新版去向 | 说明 |
|---|---|---|
| `#DD9800` 金 | **`#B3001E` 红（6264px）** 与 **保持金（2376px）** | 外框染红，但**钥匙孔牌面仍留金色** |
| `#0E4F0E` 绿 | `#1C1C1C`（67%）与 `#272727`（33%） | 相邻两级灰 |

纯调色板映射会把钥匙孔一并染红，与原图不符。按位置搬运才能保留。

### 3.3 工具

```bash
# 预览（不改文件）：导出 _chk/_icon/preview_before_after.png 等对照图
python _chk/icon_adaptive_recolor.py

# 确认无误后写入（main + debug 各 5 档，共 10 张）
python _chk/icon_adaptive_recolor.py --apply
```

脚本自动做三件事：① 用 `git show HEAD:` 取旧版扁平图；② 与工作区新版逐像素 diff，反推几何参数与调色映射；③ 套用到 `foreground` 各密度（**只换颜色、不重采样**，像素网格不动）。

**幂等**：重复执行第二次会报告「换色 0 px」，可用来确认写入正确。

**交叉验证**：xxhdpi(324) 前景的变色像素数应**恰好等于** legacy(192) 的变色像素数（本次均为 **19368**）—— 因为 432×0.75=324、224×0.75=168，两者精灵的像素尺寸相同。

---

## 4. 应用名 `appName`：作用链与安全边界

### 4.1 作用链

```
build.gradle (根，allprojects.ext)
    appName = 'EGOPD'
        ├─ android/build.gradle : manifestPlaceholders = [appName:appName]
        │     └─ AndroidManifest.xml  android:label="${appName}"   → 桌面名 / 最近任务名
        ├─ desktop/build.gradle : systemProperty 'Specification-Title', appName
        │     └─ DesktopLauncher  → 窗口标题 / 崩溃弹窗标题
        └─ desktop/build.gradle : imageName = appName             → jpackage 产物目录名
```

**不影响游戏内标题**：标题界面用的是 `assets/interfaces/title.png` 之类的图片 logo，与 `appName` 无关（全仓 grep `My Pixel Dungeon` 在 `core/` 里 0 命中）。

### 4.2 移动端存档：改名安全 ✅

Android 侧三个关卡都不看显示名：

| 关卡 | 取值 | 受 `appName` 影响？ |
|---|---|---|
| 应用身份 `applicationId` | `com.mypd.mypixeldungeon`（debug 加后缀 `.indev`） | ❌ 无关 |
| 存档目录 | `getFilesDir()` = `/data/data/<applicationId>/files/` | ❌ 无关 |
| 偏好文件名 | 硬编码 `"ShatteredPixelDungeon"`（源码里写死） | ❌ 无关 |

**红线**：可以改 `appName`，**绝不要改 `appPackageName`**——那是应用身份，改了等于换一个 App，老存档会被系统留在旧包名目录下、新包读不到。

**另一个易踩的点**：debug 变体有 `applicationIdSuffix ".indev"`，所以 debug APK 与 release APK 是**两个独立 App、两套独立存档**。玩家装 release、你测 debug，两边存档互不相通，别误判成「丢档」。

### 4.3 桌面端存档：已刻意解耦 ✅

桌面版数据目录是拼出来的：`%APPDATA%\.mypd\<title>\`，而 `title` 就是 `appName`。**如果放任不管，改 `appName` 会让桌面版老存档变成读不到的孤儿目录**（文件还在磁盘上，游戏却当自己没档）。

已在 `DesktopLauncher.java` 中冻结：

```java
//[MyPD] 存档目录名与产品显示名(title / build.gradle 的 appName)刻意解耦。
final String saveDir = "My Pixel Dungeon";
```

四处路径（Win XP / Win / macOS / Linux）全部改用 `saveDir`。因此：

- 桌面窗口标题、崩溃弹窗、jar/exe 名字 → 已是 **EGOPD**
- 桌面存档目录 → **仍是 `.mypd/My Pixel Dungeon/`**，老存档原地可用
- 以后再改产品名 → 存档目录不会再跟着动

> 若哪天想把目录也正式改成 `EGOPD`，做法是：改 `saveDir` 常量 + 把老目录整体改名。**两件事必须同时做**，只做一件就丢档。

---

## 5. 核验流水线（每次改完图标/名字跑一遍）

```bash
# 1) 打包（凭据/环境见 AGENTS.md §1）
JAVA_HOME=D:/PD/tools/jdk-21.0.12.1+1 GRADLE_USER_HOME=D:/PD/.gradle \
  tools/gradle-9.4.0/bin/gradle.bat :android:assembleDebug --console=plain

# 2) 名字 + 实际生效的图标（关键：看 application-icon-* 指向谁）
tools/android-sdk/build-tools/36.0.0/aapt.exe dump badging \
  android/build/outputs/apk/debug/android-debug.apk | grep -E "^package|application-label:|application-icon-480"

# 3) APK 内图标是否真的等于工程源文件（md5 逐张比对）
#    见 §7 的 _chk 脚本；注意 res/mipmap-anydpi-v26/ic_launcher.xml 会被 aapt2
#    编译成二进制 XML，md5 必然与源文件不同，属正常。
```

预期输出：

```
package: name='com.mypd.mypixeldungeon.indev' versionCode='925' versionName='0.2.5-INDEV'
application-label:'EGOPD'
application-icon-480:'res/mipmap-anydpi-v26/ic_launcher.xml'
```

### 5.1 体积变化要能解释

本次 0.2.5 从 37.5MB 涨到 50.2MB，原因是**新增了 49 个素材**（`music/hero_*.mp3`、`sounds/indexfather_*.mp3`、`environment/tiles_lob_1/2.png`）——上次打包时它们还没进仓。图标本身的改动只增加 **约 5KB**。

用顶层目录体积对比可快速定位：

```
assets    484项 39.6MB  →  533项 50.9MB     <<< +49 个素材
lib         8项  3.9MB  →    8项  3.9MB
```

---

## 6. 已知坑

1. **`android/src/debug/res` 是 main 的整套副本** —— 任何图标改动都要做两遍。长期建议：把 debug 那份删掉（或用脚本同步），减少一处漏改面；删除前先确认没有依赖「debug 专属图标」的地方。
2. **启动器图标缓存** —— 同名同 `versionCode` 覆盖安装时，部分启动器（MIUI / EMUI / 部分 Pixel Launcher 版本）会继续显示缓存图标。
   **验证时请先卸载再安装**，不要只覆盖安装。要发版给玩家，记得递增 `appVersionCode`。
3. **`monochrome` 只在 Android 13+ 开启「主题图标」时使用** —— 此时系统只用它的 **alpha 剪影**并重新上色，配色改动不影响它；只有**形状**变了才需要重画。
4. **`ic_launcher_background` 会被放大裁切** —— 别把关键图案放在边缘 18dp 内，也别用它当「第三层前景」。
5. **改完图标必须重新打包** —— 图标是编译期资源，`assets/` 里的游戏内贴图改了也要重打包，别只重启游戏。
6. **同一时间只跑一个构建** —— 桌面调试（`:desktop:debug`）持有 `executionHistory.lock`，此时打包会失败；详见 `AGENTS.md` §1。

---

## 7. 行号 / 工具速查

| 位置 | 作用 |
|---|---|
| `build.gradle`（根）`allprojects.ext` | `appName` / `appPackageName` / 版本号（唯一来源） |
| `android/build.gradle` `defaultConfig` | `manifestPlaceholders`、`applicationId`、debug 的 `.indev` 后缀 |
| `android/src/main/AndroidManifest.xml` | `android:icon="@mipmap/ic_launcher"`、`android:label="${appName}"` |
| `android/src/{main,debug}/res/mipmap-*/` | 图标资源（4 种 × 5 档 × 2 套） |
| `desktop/src/main/java/.../DesktopLauncher.java` | `title`（显示名）与 `saveDir`（存档目录，已解耦冻结） |
| `android/src/main/java/.../AndroidBackupHandler.java` | 备份范围，走 `getFilesDir()` |

| 工具 | 用途 |
|---|---|
| `_chk/png_palette_dump.py` | 解码 PNG（**支持 1/2/4/8 位索引与 tRNS**，SPD 图标的 background/foreground 就是 4 位索引）+ 主色统计 |
| `_chk/icon_palette_map.py` | 逐像素 diff 两张扁平图，输出「旧色 → 新色」调色映射与分裂告警 |
| `_chk/icon_adaptive_recolor.py` | 反推几何 + 位置搬运换色；预览 / `--apply` 写入 main+debug 各 5 档 |
