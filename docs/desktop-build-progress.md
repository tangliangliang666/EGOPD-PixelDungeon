# 桌面端构建进度

> 目标：构筑可直接游玩的桌面端版本（Windows）
> 更新时间：本次会话（jpackage 构建因用户要求暂时中止）

## ✅ 已完成

**桌面版可分发 JAR（可运行）**
- 构建命令：`gradle.bat :desktop:release`（JAVA_HOME=JDK21）
- 产物：**`D:\PD\desktop\build\libs\desktop-0.1.0.jar`**（51.6 MB，fat jar，含主类清单）
- 运行方式：`java -jar desktop-0.1.0.jar`（需系统装有 Java，本机 `D:\PD\tools\jdk-21.0.12.1+1`）
- 也可用 `build-desktop.bat` 直接以调试模式运行游戏（`:desktop:debug`）

## ⏸️ 未完成（下次继续）

**jpackage 独立程序（免装 Java 双击 .exe）**
- 任务：`:desktop:jpackageImage`
- 当前状态：**JDK 17 下载被中断**——`desktop/build/jdks/win/jdk.zip` 仅 6.5 MB（完整约 190 MB，来自 adoptium GitHub），运行时镜像未生成
- 预期产物：`desktop/build/jpackage/My Pixel Dungeon/`（含 .exe 与捆绑运行库）

**继续步骤**：
1. 重新执行：`gradle.bat :desktop:jpackageImage`（beryx 插件会继续/重新下载 JDK 17 到 `desktop/build/jdks/win/`，随后自动构建镜像）
2. 完成后将 `My Pixel Dungeon` 目录整体拷贝即可分发游玩
3. 若 GitHub 下载慢/失败，可手动把 Temurin JDK 17 zip 放到 `desktop/build/jdks/win/jdk.zip` 再重试

## 备注
- 桌面端与 Android 共用同一份核心代码，当前所有新内容（神谕代行者、胎儿博士、烈日之心等）均已包含
- `build-desktop.bat` 已配置好 JDK21 环境；`jpackage` 镜像内部捆绑的是 JDK 17（与游戏运行要求匹配）
