# EGOPD · 破碎的像素地牢 中文魔改版

> 基于 [Shattered Pixel Dungeon](https://github.com/00-Evan/shattered-pixel-dungeon) **v3.3.8** 源码的中文魔改版本。
> 在保留原版玩法的基础上，叠加了自定义武器、角色、道具，以及一套与官方挑战系统平行独立的
> 「卡巴拉生命之树 · 考验（Trials）」系统。

## 项目简介

EGOPD 是个人向的 Shattered Pixel Dungeon 改版，主要新增内容：

- **自定义武器与角色**：环指大师、爱慕神器、溶解之爱、粉色史莱姆等 EGO 系列内容
- **考验（Trials）系统**：与官方 Challenges 平行独立的第二套难度修饰，共 10 类，对应卡巴拉生命之树的
  十个质点（KETER / CHOKHMAH / BINAH / CHESED / GEBURA / TIPHERETH / NETZACH / HOD / YESOD / MALKUTH）
- **自定义道具与机制**：雷德之证、调试控制台，以及移植自其他改版的内容（如 7 种可食用蘑菇）
- **完整中文本地化**：`core/src/main/assets/messages/*_zh.properties`

## 编译

需要 **JDK 21** 与 **Gradle 9.4**；Android 打包另需 Android SDK，路径写在 `local.properties`
（该文件不入库，需自行创建）。

- 桌面版：运行 `build-desktop.bat`
- Android APK：`gradle.bat :android:assembleDebug`，产物位于 `android/build/outputs/apk/debug/`

> 仓库**不含**本地工具链（`tools/` 下的 JDK、Android SDK、Gradle）与任何构建产物。
> 已发布的 APK 通过 [Releases](https://github.com/tangliangliang666/EGOPD-PixelDungeon/releases) 分发。

## 目录结构

| 目录 | 说明 |
|---|---|
| `core/` | 游戏主体源码与素材，`assets/messages/` 为多语言文案 |
| `SPD-classes/` | 引擎层（noosa 渲染 + 部分 SPD 类），含本 mod 的少量改动 |
| `android/` `desktop/` `ios/` | 各平台启动器工程 |
| `docs/` | 开发档案与专题文档，总索引见 `docs/INDEX.md` |
| `_chk/` | 源码级核验脚本与验证产物 |
| `.workbuddy/memory/` | AI 协作工作日志 |

## 许可与致谢

本项目以 **GNU GPLv3** 发布（见 [`LICENSE.txt`](LICENSE.txt)），与原版保持一致。

- 原版 **Shattered Pixel Dungeon** 版权归 [Evan Debenham (00-Evan)](https://shatteredpixel.com/) 所有，
  其本身基于 [Watabou](https://watabou.itch.io/) 的 Pixel Dungeon 源码。
- 本项目是**非官方改版**，与原作者及官方团队无关，请勿将本版本的 bug 反馈给原作者。
- **第三方素材声明**：`core/src/main/assets/` 中的部分音效与音乐取自第三方游戏
  （如《边狱公司》相关音频，来源于社区 wiki），版权归各自权利人所有，仅用于个人学习与非商业用途，
  **不适用 GPLv3**。若权利人提出异议，将立即移除相关文件。
- 本仓库为个人改版备份，**不接受 Pull Request**；欢迎提交 Issue。

---

> 以下为原版 Shattered Pixel Dungeon 的 README 原文。

# Shattered Pixel Dungeon

[Shattered Pixel Dungeon](https://shatteredpixel.com/shatteredpd/) is an open-source traditional roguelike dungeon crawler with randomized levels and enemies, and hundreds of items to collect and use. It's based on the [source code of Pixel Dungeon](https://github.com/00-Evan/pixel-dungeon-gradle), by [Watabou](https://watabou.itch.io/).

Shattered Pixel Dungeon currently compiles for Android, iOS, and Desktop platforms. You can find official releases of the game on:

[![Get it on Google Play](https://shatteredpixel.com/assets/images/badges/gplay.png)](https://play.google.com/store/apps/details?id=com.shatteredpixel.shatteredpixeldungeon)
[![Download on the App Store](https://shatteredpixel.com/assets/images/badges/appstore.png)](https://apps.apple.com/app/shattered-pixel-dungeon/id1563121109)
[![Steam](https://shatteredpixel.com/assets/images/badges/steam.png)](https://store.steampowered.com/app/1769170/Shattered_Pixel_Dungeon/)<br>
[![GOG.com](https://shatteredpixel.com/assets/images/badges/gog.png)](https://www.gog.com/game/shattered_pixel_dungeon)
[![Itch.io](https://shatteredpixel.com/assets/images/badges/itch.png)](https://shattered-pixel.itch.io/shattered-pixel-dungeon)
[![Github Releases](https://shatteredpixel.com/assets/images/badges/github.png)](https://github.com/00-Evan/shattered-pixel-dungeon/releases)

If you like this game, please consider [supporting me on Patreon](https://www.patreon.com/ShatteredPixel)!

There is an official blog for this project at [ShatteredPixel.com](https://www.shatteredpixel.com/blog/).

The game also has a translation project hosted on [Transifex](https://explore.transifex.com/shattered-pixel/shattered-pixel-dungeon/).

Note that **this repository does not accept pull requests!** The code here is provided in hopes that others may find it useful for their own projects, not to allow community contribution. Issue reports of all kinds (bug reports, feature requests, etc.) are welcome.

If you'd like to work with the code, you can find the following guides in `/docs`:
- [Compiling for Android.](docs/getting-started-android.md)
    - **[If you plan to distribute on Google Play please read the end of this guide.](docs/getting-started-android.md#distributing-your-app)**
- [Compiling for desktop platforms.](docs/getting-started-desktop.md)
- [Compiling for iOS.](docs/getting-started-ios.md)
- [Recommended changes for making your own version.](docs/recommended-changes.md)