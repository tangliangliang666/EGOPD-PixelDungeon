# AGENTS.md — 开发经验手册（通用 · 常驻核心）

<!-- agents-handbook-split: v1 -->
> 本文档为**通用开发经验手册**（构建流程、文本本地化、贴图规范、SPD 框架机制、通用陷阱、通用物品/特效经验）。
> **2026-09-21 重构**：原文 197KB 超出 DSH 工作区指令预算（65536 字节）。
> 渲染器是**保留头部、截掉尾部**（`render.ts` 的 `truncateUtf8` 取 `bytes.subarray(0, end)`），
> 且分配顺序是「先整份丢弃较宽的泛化文件（如 `~/.dsh/AGENTS.md`），再截最具体的那一份」
> ⇒ 位于**文件末尾**的 §5~§8 约 96KB **从未送达模型**（会打印 `Workspace instruction budget … truncated …` 提示）。
> **预算作用于整条渲染消息**（不是每个文件各一份），所以再加一个 `CLAUDE.md` 会争同一笔 65536。
> **无文件监听**：改完 AGENTS.md 后需一次 `read`/`write`/`edit` 或会话恢复才会刷新基线。
> 现按「**常驻红线 + 目录索引**」重组：本文件只保留每次都要用的部分，长尾原文逐字迁入 `docs/handbook/`。

## 0. 阅读协议与文档地图（**先读这一节**）

**取用顺序：先用下表定位 → 再 `docfind`/`grep` 拿到 `文件:行号` → 最后 `read` 带 `offset`/`limit` 只读那一段。**
**禁止整读** `docs/features.md`（9872 行，行数以 `docs/INDEX.md` 为准）、`.workbuddy/memory/*.md`（22 个日志）、`docs/handbook/*.md` 与 `docs/archive/`。

| 内容 | 位置 | 何时读 |
|---|---|---|
| **总索引**（所有文档的章节行号锚点） | `docs/INDEX.md` | 想找「某件事写在哪」时**先读这个** |
| 跨语料关键词定位 | `python _chk/docfind.py <关键词>` | 一条命令返回 AGENTS/docs/memory/_chk 的 `文件:行` |
| §3 地图贴图、分层 tilemap、跨格拼帧 原文 | `docs/handbook/terrain-tilemap.md` | 动地形/图集/草皮/遮挡时 |
| §4 SPD 框架机制经验 原文（全量） | `docs/handbook/spd-framework.md` | 找钩子/汇聚点/框架行为时（**最常查**） |
| §6 常见陷阱速查 全量表 原文 | `docs/handbook/pitfalls.md` | 遇到怪现象、症状 —— 先查它 |
| §7 自定义武器/物品 原文 | `docs/handbook/weapon-item-dev.md` | 新武器/物品/附魔/掉落池 |
| §8 法杖（失乐园）与 buff 特效 原文 | `docs/handbook/wand-buff-fx-dev.md` | 法杖/粒子/特效/图层 |
| 非职业功能档案、逐批改动流水账 | `docs/features.md`＋`docs/index/features.md`（批次→行号） | 只想看某一批改动时 |
| 另一平台（WorkBuddy）逐日工作日志 | `.workbuddy/memory/*.md`＋`docs/index/memory.md`（日期/主题→行号） | 追溯「当时为什么这么改」时 |
| `_chk/` 核验脚本清单（用途/断言目标） | `docs/index/scripts.md` | 改完代码挑回归脚本时 |
| 各职业/功能专题档案 | 见下方 §5 | 做对应开发时 |

**旧引用解析表**：旧文（含 `docs/`、`.workbuddy/memory/`、代码注释）里的「见 §4 / §6 / §7 / §8」一律按上表解析 ——
`docs/handbook/` 里的文件**保留了原节号与原文**，`§6 第 X 行`、`§4 某条` 这类说法仍能对上。
冻结全量副本：`docs/archive/AGENTS-2026-09-21-full.md`（仅作存档，**不要读**）。
`§1/§2/§3/§5` **仍在本文件里**，原编号未变。

**同步约定**：通用机制/规范/陷阱的新发现记入本文件；长尾细节记入对应 `docs/handbook/*.md` 或职业档案（docs/ring-master.md、docs/oracle-class-design.md、docs/features.md）。
文中「现为 XXX」均为最新状态，以对应文件为准。（原引言里的「第 5 节内容地图」「第 6 节陷阱速查表」现分别指 §5 与 §6/全量表。）


## 1. 构建与运行（快速参考）

```bat
rem 编译核心模块（最快反馈，桌面调试前必跑）
set JAVA_HOME=D:\PD\tools\jdk-21.0.12.1+1
set GRADLE_USER_HOME=D:\PD\.gradle
D:\PD\tools\gradle-9.4.0\bin\gradle.bat :core:compileJava --console=plain
```

- **退出码 1 但输出 `BUILD SUCCESSFUL` = 正常**（javac 的 stderr 警告）。真正的失败会输出 `错误:` / `error:` 行。
- **AI 代理工作流**：编译成功后**不要自行启动游戏**——游戏窗口无输出可采集，游戏内内容只能人工验证；直接告知用户进行测试即可。
  - **源码级核验用单文件 javac**（不代跑 Gradle，流水线见 skill `egopd-source-verify`）。一个必踩的坑：
    classpath **必须带上 `D:/PD/services/build/classes/java/main`**。`NewsService` / `AvailableUpdateData`
    在**独立的 `services` 模块**里，漏了它会甩出 19 个「找不到符号 / 程序包 NewsService 不存在」的
    **虚假错误**，全落在 `scenes/TitleScene.java`、`services/news/News.java`、`scenes/NewsScene.java` ——
    与本次改动毫无关系，极易误判成「改坏了」。（2026-09-20）
- 桌面运行：`build-desktop.bat`（F2 打开 WndDebug 调试窗：药水/卷轴/武器/远程/防具/神器/杂项/Buff/怪物标签页，点击即生成/施加；「杂项」标签浅层扫描 items 根包，含天狗面具、矮人王冠、护身符、复活十字架等关键物品）。完整输出写入 `d:\PD\desktop-debug.log`，启动前自带数据目录写入自检。**报"游戏打不开"先查 `docs/handbook/pitfalls.md` §6"游戏静默退出"条目（通常是 Gradle 守护进程被沙箱污染，`gradle --stop` 后重试）**。
- **Android APK**：SDK 已装在 `D:\PD\tools\android-sdk`（`local.properties` 已指向）。构建：
  `gradle.bat :android:assembleDebug` → 产物 `android\build\outputs\apk\debug\android-debug.apk`（debug 签名，可直接安装；`assembleRelease` 未配置签名）。
  - 环境变量与 `build-desktop.bat` 一致：`JAVA_HOME=D:\PD\tools\jdk-21.0.12.1+1`、`GRADLE_USER_HOME=D:\PD\.gradle`。
  - **版本号**：`build.gradle` 的 `ext.appVersionCode / appVersionName`（2026-09-26 现为 936 / `0.3.6`）。编码约定
    **`900 + minor*10 + patch`**（0.2.5⇒925、0.3.0⇒930、0.3.1⇒931）；打包后 `output-metadata.json` 与 `aapt dump badging` 均可核对
    实际写入值（debug 变体 `versionName` 会加 `-INDEV` 后缀、`applicationId` 加 `.indev` 后缀）。
  - **出 EGOPD 新版本的固定三步**（2026-09-18 起）：① 根 `build.gradle` 改 `appVersionCode/appVersionName`；
    ② 在 **`ui/changelist/EGOPD_Changes.java`** 追加 `ChangeInfo c = new ChangeInfo("EGOPD vX.Y.Z", true, "")` → `changeInfos.add(c)`
    并把该版本的改动逐条 `c.addButton(...)`（**这是 EGOPD 改动历程唯一该写的地方**）；③ `:android:assembleDebug` 后拷成
    `EGOPD_<版本名>.APK`。改动栏界面的页签在 `scenes/ChangesScene.java`：`EGOPD_TAB = 0` 是 EGOPD 页且**默认选中**，
    版本页顺延为 case 1~8；该页**不显示**上游那句「改动详情仅提供英文版本」（`lang_warn` 已按页签排除）。
    页签按钮 9 个分两行排（上游 8 个正好占满一行，故 `ph` 由 `h-36` 改为 `h-55` 让出 19px）。
    - **改动栏行文风格＝极简（2026-09-24 起，用户明示）**：每条只写**一句话**，说清「改了什么」即可。
      **不要**背景铺垫、不要「原本如何 → 现在如何」的展开、不要逐条列举数值边界、不要写多段（正文里不出现 `\n` 分段）。
      详细原理与推导写在 `docs/` 档案里，不进改动栏。（`verify_lloyd_healblock.py` 有一条机械判据：v0.3.5 及以后
      的条目正文不得含 `\n`。）
  - **归档约定**：构建完把 `android-debug.apk` 复制为 `EGOPD_<版本名>.APK`（同目录）。注意 `packageDebug` 会**先清空
    output dir**，上一版的 `EGOPD_x.y.z.APK` 会被自动删掉，所以若要留旧版请先拷出。
  - **只改源码/文本后直接重新 `:android:assembleDebug` 即可**（增量，约 35s；`:core:compileJava` 会连带重编）。
  - 用户级约定：`assembleDebug` 允许 AI 执行（打包验证），但 `：core:compileJava` 等编译任务默认由用户自己跑。
  - **打包前先确认没有别的构建在跑**：桌面调试（`:desktop:debug`）会长时间持有 `.gradle\9.4.0\executionHistory\executionHistory.lock`，
    此时其它构建一律以 `executionHistory.lock (拒绝访问。)` 失败（哪怕加了 `--no-daemon`）。正确做法是等游戏退出/桌面构建结束再打包；
    **不要**用 `--project-cache-dir` 另开缓存来绕锁 —— 两个构建会互踩 `android\build\intermediates`，表现为 dexBuilder 写
    `desugar_graph\...\graph.bin (拒绝访问。)`。
  - **中断构建后的恢复三步**（杀掉客户端**不会**终止 Gradle 守护进程，它会继续跑并占住中间产物）：
    ① `gradle.bat --stop`；② 若它报 "No Gradle daemons are running" 却仍有 ~1GB 的 `java.exe`（命令行含 `GradleDaemon`）→ 直接结束该 PID；
    ③ 删 `android\build\intermediates\desugar_graph`（随后 dexBuilder 回退非增量并打印 "Fall back to non-incremental mode"，属正常，构建仍 SUCCESSFUL）。
  - **打包核验链**（本次已用）：`output-metadata.json` / `aapt dump badging` 看版本 → 确认日志里 `:android:dexBuilderDebug` 是 executed（非 UP-TO-DATE）
    → `javap -c Generator$Category.class` 确认新类字面量在数组里 → `unzip` 出 jar 内 class 与 `build/classes` 上的 md5 一致，
    证明 APK 真的含本次改动。
  - **名字/图标打包后必查**：`aapt dump badging android-debug.apk | grep -E "^package|application-label:|application-icon-480"`。
    `application-label:` 应等于 `appName`；`application-icon-*` 全部指向 `res/mipmap-anydpi-v26/ic_launcher.xml`（自适应图标，
    ⇒ 只改 `mipmap-*/ic_launcher.png` 那套扁平图真机看不到）。核对 APK 内图标是否真等于工程源文件，用 Python `zipfile` 取
    `res/mipmap*` 逐张比 md5（`res/mipmap-anydpi-v26/ic_launcher.xml` 会被 aapt2 编成**二进制 XML**，md5 必然不同，属正常）。
  - **体积变化要能解释**：`:android:assembleDebug` 产物含 `core/src/main/assets` 全部内容，所以**新增素材（音效/音乐/图集）会撑大 APK**。
    排查用「按顶层目录统计条目数与体积」对比上一版（0.2.5 由 37.5MB→50.2MB 就是新增 49 个 mp3/png 素材所致，与图标无关——图标只增约 5KB）。
  - **⚠️ 比体积前先看有没有「空洞」**（2026-09-18 踩坑）：ZipFlinger 的**增量打包**会把变小的条目原地重写，旧的更长数据留在原位成为
    垃圾空洞。实测 `EGOPD_0.2.5.APK` 有 **259 处共 898,040 字节空洞**（最大一处 293KB 在 `classes3.dex` 前），而重打一遍的 0.3.0 只剩
    502 字节（仅 PNG 的 3 字节对齐缝）⇒ **同样的 533 条 assets、压缩总量只差 223 字节，文件却小了 897KB**。
    判定「内容到底变没变」不要用文件大小，用下面的**全量资产比对**。
  - **全量资产比对（最硬的证据，2026-09-18 新增）**：用 Python `zipfile` 把 APK 内每个 `assets/*` 条目 md5 与工作区
    `core/src/main/assets/<相对路径>` 逐条比 ⇒ 能一次性回答「我改的素材到底进包没有」。本次结果 **533/533 全部逐字节一致**。
    注意 `zipfile` 对**非 UTF-8 标记**的中文名条目按 cp437 解码，要 `name.encode('cp437').decode('utf-8')` 还原后再拼路径，
    否则会误报「无源文件」。顺带一查：`assets/` 里混着 `.aseprite` 工程文件与中文名工作图（`精灵-0001.png` 等
    **9 个、1.14MB**），它们同样会被打进 APK —— 清素材时留意。
  - **新类是否真进包**：`python -c "z=zipfile.ZipFile('android-debug.apk'); dex=b''.join(z.read(n) for n in z.namelist() if n.endswith('.dex')); print(dex.count(b'EGOPD_Changes'))"`
    —— dex 里能搜到类名与新增的中文字面量即证明编译产物已进包（本次 `EGOPD_Changes` 命中 3 次、`EGOPD v0.3.0` 与中文提示各 1 次）。
- 只改文本（messages 属性文件）时无需动代码，重新打包即生效。

## 2. 文本与本地化

- **大多数文本**在 `core/src/main/assets/messages/*_zh.properties`（`actors` 怪物/buff/天赋/技能、`items` 物品、`windows` 窗口、`journal` 日志、`levels` 楼层、`misc` 杂项）。格式：`键=值`；`_xxx_` 强调色；`\n` 换行；`%s`/`%1$d` 是占位符**不可删**。
- **行内标记（`ui/RenderedTextBlock`）**：`_文字_` 或 `**文字**` = 强调色；`~~文字~~` = **删除线**（2026-09-13 新增，标记自身不渲染）。三者在**任何** `RenderedTextBlock` 文本里都生效——包括 `StyledButton` 文字、`IconTitle` 标签、`WndHeroInfo` 标题、存档槽名字、任务日志面板。删除线是**画上去的**（白色 `ColorBlock` + `hardlight`，在 `layout()` 末尾贴到词中点），因为 `pixel_font.ttf`/`droid_sans.ttf` 都**没有** U+0336 之类的组合删除线字形。**注意**：`GLog` 的青色前缀常量就是 `"~~ "`，但 `ui/GameLog` 会先 `substring` 剥掉，不会串进来；新写文本时别让正文以裸 `~~` 开头。
- **自定义职业/物品常大量使用硬编码中文**（不走 messages），改文本需改代码；各职业的硬编码文本位置清单见对应档案：神谕→docs/oracle-implementation.md、环指→docs/ring-master.md。
- `Messages.get(Class, key)` 会沿**父类链**查找键：子类未定义 `desc` 等键时继承父类文本（新武器复制短剑文本即靠此机制）。
- **「列表类」技能文本必须自带技能名**（2026-09-17，「中指长兄」三技能）：原版 `short_desc` 习惯把名字写进句子里（「战士_苦痛坚忍_，跳过…」），本 mod 的多个技能却直接从描述起笔 ⇒ `WndChooseAbility`（选择盔甲技能界面）、`WndHeroInfo` 技能页、`ClassArmor.desc()` 三处列表里只剩一串话、**认不出是哪一门技能**。现统一走 `ArmorAbility.namedShortDesc()`：**描述已含名字（不区分大小写）就原样返回**（原版技能不重复），**否则在开头补一行 `_技能名_` + `\n`**；三个消费点已全部改用它，新增技能不必各自拼名字。英文侧尤其要按「不分大小写」判：原版把名字写成句内变形（`endure` → `_Endures_`），逐字比较会漏判成「需要补名字」。预演/回归：`_chk/check_named_short_desc.py`（模拟 `titleCase` + 大小写不敏感匹配，逐技能打印列表里实际会显示的文本并断言中指三技能都补上）。
- 上述列表用的是 `RedButton`（`StyledButton`）+ `RenderedTextBlock`，两者都支持 `\n` 硬换行与 `_强调_`；`WndChooseAbility` 的按钮高度靠「先 `setSize` 触发一次布局 → 再 `reqHeight()` 取换行后真实高度」这套写法（同 `WndMonkAbilities`），所以**给按钮文字加换行不会溢出**。
- **数值类信息只写在「描述文本」里，即时消息（`GLog`）不写具体数字**（2026-09-16 用户约定）：门槛、消耗、百分比这类需要玩家**事先权衡**的信息写在物品/天赋/技能的 `info()` / `desc()`（`Messages.get(..., "desc")`）里；`GLog.i/w/p` 的即时报文只给感受性措辞（如「账簿上的仇怨还太淡，尚不足以燃起」），不写「需要 20%」。好处：战斗中不弹一堆数字，且改数值时不必逐条同步文案（`GLog` 那侧不带参 ⇒ 也不会踩格式化坑）。参考 `RevengeLedger`（`no_charge` 不带参、`desc` 写明 20%/2%）。
- **改一个机制的「行为」时，先 `grep` 全部提及它的描述键**——同一套机制通常在 **3~4 个键**里各写了一遍（招式自己的 `desc` ×2 语言、所属天赋的 `desc` ×2、职业/转职的长描述 ×2）。本作实测（2026-09-16「踏碎改成单体」）：只改 `move.stomp.desc` 会漏掉 `herosubclass.loyal_pilgrim_desc` 里的同一句话。**做法**：用机制的中文关键词（如「周围八格」「英勇之跃」）在 `core/src/main/assets/messages/` 下 `grep`，再按 `*-zh`/base 两两成对改。
- **描述文本只做功能描述，不写修饰句**（2026-09-16 用户约定，当晚已按此回改过一轮）：天赋 / 物品 / 转职的 `desc` **直接从 `_+1：_` 或效果本身起笔**，不要加开场白式的文学化铺垫（英文侧实测删掉了 `Wielding a blade well is a matter of familiarity, not of force.` / `Whatever stands in Laevateinn's way ends up as a puddle.` / `Every layer of the seal that comes off leaves the blade a little keener.` 三句）；`actors.buffs.*` 的 `desc` 同样直接写数值效果，不写「这个单位正在……化开」这类叙述。**层级标签统一 `_+1：_`（zh）/ `_+1:_`（en）——首档也要带下划线**，只写 `+1：` 会和后两档样式不一致。**英文侧要跟中文同步**：中文删了铺垫而英文留着的，属于未对齐（本作长期以 zh 为准，回改时一并处理）。
- **换行只认反斜杠 `n`**：`\n\n` 误写成 `/n/n`（正斜杠）**不会换行也不报错**，只会在面板里原样显示 `:/n/n_+1:_`。批量核验：`grep -c "/n/" core/src/main/assets/messages/ -r` 应为 0（2026-09-16 在 `actors.properties` 一次揪出 9 处，全在手掌/前二老板的 T3 天赋里）。
- **用脚本批量写 `*.properties` 时，落盘前必须把真换行转成字面量 `\n`**（2026-09-17 踩坑，「即刻处刑[莱瓦汀]」）：Python 源码里写 `'…。\n\n下一段…'` 会被解释成**真换行**，写进文件后一条 entry 断成多行——续行没有 `=`，会被解析成一堆垃圾键（表现是「文本只剩第一段」，而**加载不报任何错**）。做法：块里照常写真换行（好读），落盘前统一 `line.replace('\n', '\\n')`，并自检「有没有缺少 `=` 的非空非注释行」。核验脚本两条：`_chk/fix_newline_escape.py`（接回断行 + 期望值逐键断言）、`_chk/InstExecLocCheck.java`（真 `Properties.load` + 按真实实参类型 `String.format`，并断言**每个实参都出现在结果里** ⇒ 能抓出「值被截断成只剩第一段」）。

## 3. 贴图与图标

- **items.png 现为 256×800（50 行）**（2026-09-12 由 640 扩到 800，行 41-50 为最新扩展预留区，除 41行1列外空白；行 36-40 为自定义内容区：36行1/2=割腕/悔恨、37-40行=人体派作品武器图标，**39行7/8列=泪锋的加护两形态**，其余空白）：`ItemSpriteSheet.TX_HEIGHT` 必须等于实际高度，否则全物品图标错位（曾因 528→544、576→640、640→800 未及时同步而出过全局错乱）。PNG 高度可从 IHDR 读取（**大端**，PowerShell 读 Int32 需反转字节，宽=偏移16-19、高=偏移20-23）。
- `xy(列, 行)` 是 **1 基**（**第一参=列、第二参=行**，内部 `x-=1; y-=1`），每行 16 格（WIDTH=16）。例：GLOWING_PEBBLE=xy(8, 32) 即"32行第8个"。
- 非 16×16 图标用 `assignItemRect(常量, 宽, 高)`；超大贴图（如薄暝 20×20、失乐园 21×20）会超出格子，是预期测试内容。
- **职业护甲图标占 items.png 第 12 行**：`private static final int ARMOR = xy(1,12)`（16 格），依次 `ARMOR+0..+4` 五套普通护甲、`+5..+10` 六职业护甲、`+11` 食指西服（神谕）、`+12` 环指长袍、`+13` 拇指大衣（VALENCINA）、`+14` **中指外套（MIDDLE_FINGER，`xy(15,12)` 16×16）**。新增时：常量声明要写在下面那个 `static{}` 赋值块**之前**（静态初始化按源码顺序执行），并在块内补 `assignItemRect`。
- **自定义职业天赋图标**：为自定义职业新增天赋时，图标一律写在 talent_icons.png 对应预留行，枚举值直接写**绝对索引**并烘焙进枚举，`icon()` 内按 `case <职业>` 特判返回该索引；**禁止**按当前职业对所有天赋做全局行偏移（旧写法 `+7*32` 已废弃）——会让其他职业天赋（枚举图标 ≥64）索引越界 → `TextureFilm.get` 返 null → NPE，蜕变卷轴替换窗口必现闪退。索引位置必须与图内实际绘制一致（历史上出现过两次"整体差 1 格"错位）。各职业已占行与索引见：神谕=第八行（docs/oracle-implementation.md）、环指=第九行（docs/ring-master.md）。
- 英雄/技能图标：`ui/HeroIcon.java`（hero_icons.png 帧号）；buff 图标：`ui/BuffIndicator.java`（连续编号）。新图标需先画进贴图再引用，否则显示空/错位。各职业已分配的 hero/buff/talent 图标索引见对应档案：环指→docs/ring-master.md、神谕→docs/oracle-implementation.md。
- **动画贴图复用**：`index.png` 与 `guard.png` 同规格（256×16、12×16 帧）时直接照抄 `GuardSprite` 换 `Assets.Sprites.INDEX` 即可。
- **英雄皮肤帧尺寸**：原版 7 职业贴图均 256×128、帧 12×15、布局 21 列 × 8 行（6 tier 行 + 余量）。帧索引布局（每行 21 帧，见 `HeroSprite.updateArmor`）：idle 0,1；run 2-7；die 8-12；attack 13-15；operate 16,17；fly 18；read 19,20。`HeroSprite.tiers(HeroClass)` 按**职业**返回 tier 行偏移纹理册：原版共享 ROGUE-based 缓存（布局一致故 UV 相同），`RING_MASTER` 用自己的 20×24 贴图（`callisto.png` 420×192=21×8 帧）独立缓存 `ringMasterTiers`。`HeroSprite.frameSize(cls)` 返回帧尺寸（原版 {12,15}、RING_MASTER {20,24}）。**`tiers()` 无参版本已移除**，所有 `tiers()` 调用者（HeroSprite.updateArmor/avatar、MirrorSprite、ShadowClone.ShadowSprite、PowerOfMany.ShadowSprite.setup）均改为 `tiers(cls)`+`frameSize(cls)`。**另有两处直接切片消费者**（不走 tiers，直接按像素坐标取单帧）：`HeroSelectScene`（`new Image(spritesheet, 0, 6*fs[1], fs[0], fs[1])` 取 row6 首帧）与 `StartScene` 存档槽——存档图标**固定 12×15**（原版帧尺寸，`HeroSprite.FRAME_WIDTH/HEIGHT` 已改 public），大帧职业用 `StartScene.saveSlotIcon()` **截取帧内"中央靠底部"的 12×15 区域**（水平居中偏移 `(fs[0]-12)/2`、垂直贴底偏移 `fs[1]-15`；原版帧两偏移均为 0，行为不变）。曾试过整体等比缩放方案（scale-to-fit 22px 槽高），已弃用改为截取。绘制要点：角色脚部画在帧**底部**（CharSprite 把贴图底部锚到格子底-perspectiveRaise），放大帧多出的高度向上延伸。**自定义职业贴图的省事路线**：只要保持 **256×128、帧 12×15、21 列 × 8 行**（如 `valencina.png`、`matthias.png`），就直接复用 ROGUE 的 film，`HeroSprite.tiers()`/`frameSize()` 都**不用改**（行分配：0-5 护甲 tier、6 = ClassArmor 兼选角立绘、7 = 皮肤预留；非 RING_MASTER 职业的 film 只有 8 行 0..7）。**行 7 之后再加皮肤行就要另建独立 film**（参考 `ringMasterTiers`），否则 UV 越界。
- **音效 = `assets/sounds/*.mp3`（现 101 个，**全部 mp3**），音乐 = `assets/music/*.ogg`（流式播放）**，两者都放在 `core/src/main/assets/` 下，是**唯一真源**：Android 由 `android/build.gradle` 的 `sourceSets.main.assets.srcDirs` 直接指向它（进 APK 的 `assets/sounds/…`），桌面由 `desktop/build.gradle` 的 `processResources { from core/src/main/assets }` 倒进 jar **根目录**（所以 jar 里是 `sounds/…`、**没有 `assets/` 前缀**，核验时别照 APK 的路径找）。**新增音效三步**：①mp3 放进 `core/src/main/assets/sounds/`（小写 snake_case，如 `jelly_boing.mp3`）；②`Assets.Sounds` 里加 `public static final String XXX = "sounds/xxx.mp3";` **并且必须把这个常量加进同类的 `all[]` 数组**；③在触发点 `Sample.INSTANCE.play( Assets.Sounds.XXX )`。**漏掉第②步不会报错**：`Sample.play(id,…)` 的 `id` 就是**资源路径字符串**、同时充当地图 key（`Sample.load` 用同一字符串 put），查不到就静默 `return -1` ⇒ 表现为「改了代码、没声音、日志干净」。`ShatteredPixelDungeon.create()` 里 `Sample.INSTANCE.load(Assets.Sounds.all)` 只是**入队**，真正解码发生在 `Game.update() → Sample.update()`，**每帧只取 1 个**（101 个音效约 1.7 秒加载完），所以开局极早期播放可能还没就绪。播放 API 在 `com.watabou.noosa.audio.Sample`：`play(id[, volume[, pitch]])`、`playDelayed(id, delay, volume, pitch)`（`pitch` 是变速变调，给同一次效果做随机化最省事）；音效开关与音量由 `SPDSettings.soundFx()` / `SFXVol()`（0-10，平方曲线）在启动时灌进 `Sample.INSTANCE.enable()/volume()` ⇒ **新音效自动受设置面板控制**，不必自己判断。**格式取舍**：**扩展名必须与真实容器一致**——桌面后端（lwjgl3）是**按扩展名**挑解码器的（`com.badlogic.gdx.backends.lwjgl3.audio` 下的 `Mp3$Sound` / `Ogg$Sound` / `Wav$Sound`），Android 则由 `SoundPool` 解；一个 MP4/AAC 文件即便改名成 `.mp3` 也照样读不出声（拿 `ffmpeg -i` 一验就露馅：真身是 `mov,mp4,m4a…` + `aac (LC)`，而 `-f mp3` 强解直接 `Conversion failed`）。转码用本机现成的 ffmpeg 即可（`C:\Program Files\Kuyo\ffmpeg.exe`，gyan.dev 完整版，带 `libmp3lame`）：`ffmpeg -y -i in.mp4 -vn -ac 1 -ar 44100 -c:a libmp3lame -b:a 64k out.mp3`。本库音效统一 mp3 且体积极小（多数 <10KB、时长 <1.5s）；`Sound` 是**整段解码进内存**的，别放长音频，`.wav`/`.ogg` 虽也能加载但别用。**「独占音效」**（2026-09-17 果冻音效引入）：`Sample.playExclusive(id, volume, pitch, duration)`＝**从头**播放（先 `Sound.stop()` 掐掉正在播的同一音效，因为 `Sound.play` 是叠加式的、不 stop 会两只一起响）＋**播放期间暂停 `Music`**＋到点自动恢复；`exclusivePlaying()` 问「是不是还在播」。计时挂在 `Sample.update()`（`Game.update()` 每帧无条件调用、与当前场景无关）⇒ 中途切场景也一定会把音乐放回来；只有「暂停是本方发起」时才由本方恢复（`exclusiveOwnsMusic`），窗口失焦/切后台之类的外部暂停不会被误恢复。**注意 `Sample.update()` 里 `delayedSFX` 空时会提前 `return`**，独占计时必须写在它**之前**。自检：`python _chk/audio_format_check.py`（裸解析帧头，打印每个音效的采样率/声道/码率/时长/体积）。


- **地图贴图（分层 tilemap / 跨格拼帧 / 草皮遮挡）原文已迁至 `docs/handbook/terrain-tilemap.md`**（原 §3 地形段）。
  定位：`python _chk/docfind.py 高草`；建新地形看 `docs/terrain-creation-guide.md`，手绘速查表与逐帧核验工具在那里。
- **APK 桌面图标 / 应用名**（2026-09-18 调研，完整指南见 **`docs/android-apk-branding-guide.md`**）。三条最容易踩的：
  1. **一张 `ic_launcher.png` 决定不了任何事**：`AndroidManifest` 的 `android:icon="@mipmap/ic_launcher"` 在 **API 26+ 只解析 `res/mipmap-anydpi-v26/ic_launcher.xml`**（自适应图标 → `_background` + `_foreground` + `_monochrome` 三张分图层），`res/mipmap-*/ic_launcher.png` 那套扁平图**只在 API < 26 才被读**。铁证：`aapt dump badging` 打出来的 `application-icon-120/160/240/320/480/640` **六档全部指向同一个 `mipmap-anydpi-v26/ic_launcher.xml`**。⇒ 只重画扁平图＝真机零变化（2026-09-18 实测踩中）。
  2. **`android/src/debug/res/` 是 `main` 的整套副本且会覆盖 main** ⇒ 图标改动必须 **main + debug 各一遍**（4 种 × 5 档 × 2 套，另加两个 ldpi 扁平图）。
  3. **改「显示名」安全，改「应用身份」是灾难**：桌面名来自根 `build.gradle` 的 `appName`（→ `manifestPlaceholders` → `android:label="${appName}"`）；而 Android 存档按 **`applicationId`** 定位（`getFilesDir()`＝`/data/data/<pkg>/files/`，偏好文件名还是硬编码的 `"ShatteredPixelDungeon"`）⇒ 改 `appName` **不影响移动端存档**，改 `appPackageName` 等于换一个 App、老存档全成孤儿。另注意 debug 变体带 `applicationIdSuffix ".indev"` ⇒ debug/release 是**两个独立 App、两套独立存档**，别误判成丢档。**桌面端例外**：`DesktopLauncher` 的数据目录原本拼的是 `%APPDATA%\.mypd\<title>\`、而 `title` 就是 `appName` ⇒ 改名会让桌面老存档变孤儿；已把目录名冻结成独立的 `saveDir` 常量（与显示名解耦，改产品名不再动存档路径）。
  - **自检工具**：`_chk/png_palette_dump.py`（解 1/2/4/8 位索引 PNG + tRNS——SPD 图标的 background/foreground 是 **4 位索引 PNG**，普通 RGBA 解码器会报 `only 8-bit supported`）；`_chk/icon_palette_map.py`（两张扁平图逐像素 diff，输出「旧色→新色」及**分裂告警**）；`_chk/icon_adaptive_recolor.py`（`git show HEAD:` 取旧扁平图 → 反推几何与调色映射 → **按位置**搬运到 `_foreground`，因此能保留「同一旧色裂成多个新色」的情形，如金色钥匙孔牌面保持金色不变红；`--apply` 写入 main+debug 各 5 档，**幂等**，重跑报「换色 0 px」）。实测几何：legacy 精灵 bbox `(12,12)-(179,173)` 168×162、fg 精灵 bbox `(104,104)-(327,319)` 224×216 ⇒ **缩放 0.750000、偏移 (-66,-66)**，各调色板色像素数严格成 16:9。

## 4. SPD 框架机制经验

> **原文（全量、逐字）已迁至 `docs/handbook/spd-framework.md`**；本节只留检索入口，避免常驻指令超预算。
> 取用：`python _chk/docfind.py <关键词>` 或 `grep -n "<关键词>" docs/handbook/spd-framework.md`，再按行号定点 `read`。

本节是全书**最常查**的一库：各类钩子/汇聚点/框架行为。先用下方词表搜到行号，再定点读那一条。

<details><summary>本节词表（66 条，可直接拿去 grep）</summary>

- 永久 buff
- 每回合逻辑
- 无视护甲伤害
- 三个「对英雄生效」的宿主钩子
- 「受伤充能 / 击杀充能」两个事件钩子
- 「命中 / 未命中」两个事件钩子
- 「攻击命中」其实有两条互不重叠的路，做「挨打就记一笔」类特性必须两条都收口
- 「按最终伤害做夹取」的天赋必须先扣掉护盾，否则等于给护盾也上了保命
- 武力戒指的「+N」在伤害公式里是等级 N+1，且等级 ≤0 会被当成诅咒
- 「无法闪避」类天赋必须照抄磐岩附魔的两处结构
- 「等待时获得 X」的两种完全不同的实现——先分清要的是「护甲」还是「护盾」
- 新增职业天赋时的三重注册
- 限时数值的「覆盖式刷新」写法
- 「冷却型天赋」的标准范式
- 「以英雄为源的伤害」怎么判定
- `Buff.announced = true` 会自己弹名字，不要再手工弹一次
- 「免死」型技能状态的标准范式
- 技能想「借用」现成图标帧，用 `ArmorAbility.iconTint()`
- 「连段 + 击退 + 投掷追击」类技能的时序坑
- ① `WandOfBlastWave.throwChar` 不能用在英雄自己的回…
- ② 贴图动画一律传空回调
- 给武器加「临时等级」一律加在 `buffedLvl()`，绝不写回 `level…
- 新增转职（`HeroSubClass`）接入清单
- `ActionIndicator` 现在有「两个槽位」（2026-09-16「双…
- 两个槽位必须用不同的 `GameAction`
- 同一槽位内部
- 「对特定单位获得灵视感知」的正确钩子
- 「按时长到期、但满足条件时改为永久」的 buff
- `ActionIndicator.Action` 的按钮可以借用别人画好的帧
- 「装备 / 切换武器的回合代价」汇聚点
- `Belongings.getItem(Class)` / `Bag.itera…
- arg-less 的文本里裸 `%` 是安全的，带参文本必须写 `%%`
- 「卸下即还原形态」的类替换写法
- 把装备「锁死」在槽位里
- 决斗家式主副手对调
- `ActionIndicator` 的「同一个槽位」是互斥的
- 按钮图标是「重建时现读」，改了数据要主动刷新
- 让快捷栏点击执行自定义行为
- 物品级的「获得经验」钩子
- buff 免疫
- 存档兼容陷阱
- 实时延时
- 击退/位移
- 瞬移
- 附魔触发
- Java 语法坑
- WndDebug 武器/怪物标签
- `Talent.onPotionUsed` 签名
- Buff 图标无独立贴图时
- 物品格子右下角等级显示链路
- 等级显示可覆写钩子
- 新增英雄职业接入清单
- 新增盔甲技能接入清单
- 神器的"等级"不要覆写 `level()`
- 神器充能上限动态化的读档裁剪陷阱
- 「神器充能」buff（`ArtifactRecharge`）的唯一入口 = `A…
- 两件自定义神器的 `charge()` 现状
- 武器"附加伤害 + 命中特效"的唯一钩子 = `Weapon.proc(Char…
- 点燃的两种写法与代价
- 「最终伤害值的最后一刀」只能落在 `Hero.damage` 的 `super.…
- 「造成的伤害」与「受到的伤害」是两条互不相交的接线
- buff「层数」三件套
- 整幅贴图的「弯曲形变」（果冻/鞭状摆动）＝ 分层绘制，别指望单 quad
- 游戏 BGM 的全局替换钩子 = 引擎层的 `Music.setTrackOve…
- 武器「命中音」的唯一钩子 = `KindOfWeapon.hitSound(fl…
- 角色语音 = 一个「每角色一个入口类」+ 六条铁律

</details>

## 5. 职业与功能档案（内容已拆分至 docs/）

本文档只保留**通用开发经验**；下列档案存放各职业/功能的**具体实现细节**。做对应开发时打开对应文件（细节以档案为准，会随时间更新）：

- **docs/ring-master.md** — 环指大师（RING_MASTER）全档案：天赋系统（原 5b）、职业分支/技艺体系/自动人偶/素材箱（原 5c）、盔甲技能诱饵/走廊（原 5d）、闭馆+杰作+深度创伤（原 5e 内）、人体派作品武器/素材/大师指环合成/击杀掉落（原 §7 专属段）、图标索引档案。
- **docs/oracle-class-design.md** — 神谕代行者（ORACLE）：设计记录与早期实现历史（2026-08）。神谕实现细节另存冻结快照 docs/oracle-implementation.md（设计已完善，仅归档）。
- **docs/features.md** — 非职业专属功能：自定义挑战接线配方（通用）+「拆迁办」挑战/胎儿博士、钻石矿墙/钻石剑彩蛋（原 5e 内拆出）。
- **docs/weapon-creation-guide.md** — 自定义武器创作流程（建类→图标常量→贴图→文本键→掉落池→编译验证→归档）。
- **docs/enchantment-implementation.md** — 附魔体系学习文档：`Weapon.Enchantment` 基类结构与 proc 调用链、附魔强度（`procChanceMultiplier` / 奥术戒指）互动、`ItemSprite.Glowing` 光效、buff 型附魔样板（`Kinetic`）、束缚/扎根 debuff 现状、新增附魔 6 处登记点、文本键规范，附「欲望/蜚蠊」两附魔设计草案。
- **docs/hero-level-progression.md** — 英雄 1~30 级基础数值调研（HT / 命中 / 闪避 / 经验 / 天赋点逐级表 + 命中率实算 + 武器/护甲四轴公式 + 经验阻断落点与副作用）。数值设计（尤其「按等级切形态」的武器）先看这份；可复跑 `_chk/hero_level_curve.py`。

- **通用陷阱速查（§6）与框架机制（§4）**：表内示例可能点各类职业名，机制本身通用，开发任何内容都适用。

（原 AGENTS 5b~5e 编号与交叉引用在 docs/ring-master.md 内原样保留；环指大师作为"新增英雄职业接入清单"的完整示例仍保留在 `docs/handbook/spd-framework.md` §4。）


## 6. 常见陷阱速查（精简表；**全量表原文已迁至 `docs/handbook/pitfalls.md`**）

> 下表是最高频的静默型陷阱；全量 **117 行**表体在（随新增同步） `docs/handbook/pitfalls.md`，
> 正文用 `python _chk/docfind.py <症状词>` 定位。**症状优先：先查表，再动手。**
> **2026-09-24 已与 `.workbuddy/memory/MEMORY.md` 去重**：本表为常驻主表；MEMORY.md 只留本表**未收录**的 4 条
> （计数类图标与「自然尺寸」耦合、`buff.type` 别标 NEGATIVE、最终延迟夹取、改敌方 HT 后的 Boss 刻度）。新增陷阱先补本表。

### 高频静默陷阱（精简表）
| 症状 | 要害（一句话；细节回查 `docs/handbook/pitfalls.md` §6 与同名回归脚本） |
|---|---|
| **自包含 UI 组件别自己 new Camera** | `Camera extends Gizmo` 且**不持有成员**（成员归 `Scene`）；`camera.add(...)` 不存在，只有静态 `Camera.add(camera)`。自制窗口一律做普通 `Component` 交给宿主 scene（走 `Camera.main`）。另：`Component` 是 `Group` **不是** `Visual`，`setRect/setSize` 返回 `Component` **不能**协变覆写；`Visual` **没有** `setRect`（只有 `x/y/width/height/scale` 字段）；给「本身是 Component」的容器做命中区要 `PointerArea(x,y,w,h)` 自指向构造器 |
| **UI 组件构造即崩（NPE，编译却全过）** | Java **实例字段初始化在 `super()` 之后**才跑，而基类构造函数里通常就调了 `createChildren()` ⇒ 子类在 `createChildren()` 里摸自己的字段＝摸到 `null`。**建子元素一律放子类构造函数体**，别放 `createChildren()`。教训：UI 的生命周期只有**真 new 一个出来跑**才验得出（`_chk/verify_dropin_zip.py`） |
| 图标整体错位 | `ItemSpriteSheet` 常量必须在 `assignItemRect` 所在 `static{}` **之前**（静态初始化按源码顺序）；`TX_HEIGHT`＝items.png 真实高；同 `xy` **撞格**＝后画覆盖先画 |
| 文本显示成键名 | 键 = `items.<包路径>.<类简单名全小写>.*`，逐字核对类名 |
| 文本只剩第一段 / 带参乱码 | entry 断行缺 `=`；字面 `%` 写 `%%`；占位符与实参数不符 ⇒ **整串回退原文** |
| 读档字段变 null / 新档下楼即崩 | 改按类名存档的类名要补 `Bundle.addAlias`；新增字段按「旧档无此键」防御（`getXxxArray` 缺键返 **null**） |
| 新增常驻按钮挤掉别人／一键双触发 | `ActionIndicator` 两槽各配**独立** `GameAction`；副槽用 `SPDAction.TAG_ACTION_2` + 不绑默认键 |
| **点击物品打不开面板** | `defaultAction()` 是背包/快速背包/快捷栏点击**唯一**入口，**别拿它当功能开关**；第二功能另开入口（`verify_masterring_bind.py`） |
| 附魔/强化列表**整格变灰点不动** | 三道具共用 `ScrollOfEnchantment.enchantable()`（拿 `isUpgradable()` 当代称）⇒「刻意不吃升级」的装备要**在 `enchantable()` 里显式放行**（`verify_sealed_enchant.py`） |
| 形态切换后字段静默归零 | 切形态＝`Reflection.newInstance` 造**全新实例**；搬运面以 `Weapon.storeInBundle` 键清单为准 |
| 保命机制抹平「自伤换成长」⇒ 无条件刷等级 | 判据统一是标记接口 `items/SelfHarmCost`，**别各开名单**；`GritTeethBuff.checkBypass` 的调用点必须在 `Char.damage` 的 `HP<0→0` 与 `!isAlive()` **之间**，且只在真打到 0 血时作废免死（`verify_selfharm_exempt.py` + `verify_grit_selfharm.py`） |
| 天赋文本写 6 层、实际 +1 也到 9 层 | `Release.gainStack()` 必须走 `growthCap(hero)`（+1⇒6/+2⇒9/0 点⇒0）；调试入口 `debugGainStack()`（`verify_grit_selfharm.py`） |
| 附加伤害击杀不触发英雄击杀效果 | `Mob.die` 归属是一道**白名单**；用 buff 自身当 src 的要登记 |
| **把「致死」延后的机制 ⇒ 掉落判定落空** | 凡「只在致死那一刻存在/当时为准」的凭据（如幸运的 `LuckProc`：`act()` 第一句就 `detach()`；财富等级：英雄侧读数可能已变），延后死亡时必须**在锁血那一刻抄一份到 Mob 上**，死亡时 **live 优先、取不到才回落**（未开启该机制时逐字不变）。⚠️ 回落值要能真起作用：被调方若内部再自查一次，就得**把值当参数传进去**（`verify_gebura_drop.py` + `GeburaDropProbe.java`） |
| **「英雄击杀」类判据在锁血时不触发**（本次：拇指「荣耀凯旋」、食指「击杀指令目标」+ 神谕庇佑） | 判据**改成「携带对应 buff 的怪死亡」**并收口到 `Mob.die()` 的 `alignment==ENEMY` 块**开头**（必须在 `super.die(cause)` 之前 —— 那之后 `Actor.remove → Char.onRemove` 把 buff 全 `detach()`，再判永远读不到；`PalermoFencing` 里同型那句就是**死代码**）。限时标记（`AimHeartMark` 只 10 回合）会在锁血期间过期 ⇒ 闸门处抄一份携带状态到 `Mob`（live 优先、取不到才回落，同上一行范式）。两个留存字段**不进存档**（推迟用掉后必是真死，死者不入档）。（`verify_marked_kill_routes.py` + `MarkedKillProbe.java`） |
| **某层想让「落坑不换层」（原地传送回本层）** | `Level` 加 `public boolean handlesChasmFall()`（默认 false，**实例方法**才能按当前层多态分派）；`Chasm.heroFall` 的早返回分支必须**早于 `Level.beforeTransition()`**（那里面会给「离开本层」的指令记账、还会凑整英雄的零碎回合）。回本层复用 `ScrollOfTeleportation.appear` + `Level.randomRespawnCell`，伤害直接复用原版 `heroLand()`（羽落秘药照常免伤）。（`verify_marked_kill_routes.py` §D/§E） |
| **在关卡生成期改地形用了 `Painter.set` 只写 `map[]`** | **安全的前提是时机**：`Level.create()` ＝ `while(!build())` → **`buildFlagMaps()`** → `cleanWalls()`，而 `paint()`（连带 `decorate()`）全在 `build()` 里 ⇒ 画完之后 flags 会统一从 `map[]` 重算。**反之，`create()` 之后再改地形必须走 `Level.set(...)` / `updateCellFlags(...)`**，否则得到「画着是深渊、却踩得上去」的幽灵格（`pit[]` / `passable[]` 没跟上，且「不重叠/非空」类核验查不出）。挖深渊只吃 `EMPTY`/`EMPTY_DECO`、留外圈地板、避开关键单位邻域（见 `MiningLevel.carveVoid`）；**圆盘要显式夹在「深内区」里**（否则圆心贴角时圆盘会溢出到内墙边 ⇒ 沿墙走一步就掉），**外圈 50% 抽完还要把「四邻皆空」的孤立格收回**（否则留单格陷阱），两处都不额外消耗随机数（`_chk/MiningVoidProbe.java` ②/⑥、`verify_marked_kill_routes.py` §F16/F17/§J） |
| **改完 `X.java` 后某个静态核验脚本的「告警条数」判据突然假红** | 带 `-sourcepath` 的单文件 `javac` 会**隐式重编**依赖：只要 `X.java` 比 classpath 上的 `X.class` 新（＝刚改过、还没编译），`X` 就会从源码重编一遍、把**它自己既有的告警**也打出来 ⇒ 写死「告警恰好 5 条」的判据当场变红。判据改成「新旧告警集合一致（`wn == wo`）＋没有告警落在本批改动的文件上」，或先跑一次让新 class 落进输出目录（再跑即热启动）。**别据此以为改坏了。**（`verify_2026-09-24.py` §[7]/§[8]） |
| **反例自测报「0 条抓不到错」但其实没测到** | `--selftest` 的变异器是 `re.subn(..., flags=re.S)` 却**没带 `re.M`** ⇒ 形如 `^items\.xxx\.name=` 的**行首锚点**永远命中 0 次，脚本只打印 `[SKIP] 反例变异未命中`，而最后一行照样写「0 条抓不到错」。写新脚本时**把 `[SKIP]` 也算失败**，并给 `re.S | re.M`（`verify_crystal_mine_warp.py` 顶部注释） |
| **怀疑某个文本文件的行尾被改坏了** | 判据**别**用 `grep -c $'\r$'`（Git Bash 下会误报「全是 CRLF」）。用 Python 字节统计 `b.count(b'\r\n')` / `b.count(b'\n')`，再拿**同目录兄弟文件**对照，最后看 `git diff --numstat` 的**删除行数**（纯追加 ⇒ 0 删除，说明没有整份重写）。⚠️ 本仓 `assets/messages/items/` 下 `items_zh-hant.properties` 是 **CRLF**，而 `items.properties` / `items_zh.properties` 本来就是**纯 LF**（`_chk/_bak_2026-09-20c/` 的副本即 LF）⇒「变成 LF 了」未必是自己弄坏的 |
| **新加一类挑战后按钮/计数/页签不跟着拆** | 类别是**分类**（`Challenges.isFun/isRegular` + 掩码），位段不变；三个窗口各自只清**自己那一类**的位（`& ~regularMask()` / `& ~funMask()`），计数/页签判据改走 `activeChallenges()` / `activeFun()`（⚠️ 选人界面里 `Dungeon.challenges` 恒 0，必须用**带参**版；`MAX_CHALS` 表达「常规挑战数」）；拆分顺带能修「一类行数超过窗口高度被裁」（`verify_fun_challenges.py`） |
| 多段连段/演出看不见 | 英雄 `busy()` 期间回合线程挂起 ⇒ 做**渲染线程状态机**（挂 `hero.sprite.parent` 的透明 `Visual`），位移自己写 `pos`，装饰动画传**空回调** |
| 数值/成长型数值写错 | 力量→`Hero.STR()`、精准→`Hero.attackSkill()` 是**唯一**入口；护甲 `drRoll()`／武器 `damageRoll()`。临时等级只加 `buffedLvl()`，**绝不写回 `level()`** |
| 永久 buff 不 tick ／ 带时长编译错 | `FlavourBuff`＝限时（泛型上限即它）；`Buff`＝永久；「获得时」副作用写在**授予处**（`FlavourBuff.act()` 全生命周期只跑一次） |
| 自定义神器不充能 | `ArtifactRecharge` → `Artifact.charge(Hero,float)`，**父类空实现 ⇒ 不覆写就吃不到**；`unique` 一标三用，单项放开覆写 `sellable()`/`transmutable()` |
| **改了 APK 图标真机没变化** | API 26+ 只读 `mipmap-anydpi-v26/ic_launcher.xml`（自适应图标）；`android/src/debug/res` 是 main 整套副本 ⇒ 改 **2 套 × 4 种 × 5 档**（skill `egopd-apk-icon-name`） |
| 改应用名怕丢存档 | 移动端存档按 **`applicationId`** ⇒ 改 `appName` 安全，改 `appPackageName` 才是灾难；debug 带 `.indev` ⇒ 与 release 两套独立存档 |
| **草皮对了但草叶是别区风格** | 草叶细节＝全局唯一 `environment/terrain_features.png`；关卡要 `implements TerrainFeaturesTilemap.FeaturesTexProvider` 报「**本层 `tilesTex()` 草皮材质**所属区」——**不是**生成期借用的深度（`GEN_DEPTH` 是布局借位，按它算会报错区；27 层＝第二区）。**`GameMath.gate` 返 float 必须 `(int)` 转型**（`verify_lob_grass_stage.py`） |
| **天赋给到别职业后点满也零效果** | 天赋判据**只能**是 `hero.hasTalent(X)`／`pointsInTalent(X)>0`，**别拿 `hero.heroClass` 当替身**（蜕变卷轴会把别职业天赋真写进天赋表）；**上游那批 `heroClass != CLERIC` 别拆**（`verify_metamorph_talents.py`） |
| **依赖「两个 buff 同时存在」的效果**，复活后整段消失 | `Hero.live()` 统一 detach 非 `revivePersists` 的 buff，且被 `Dungeon.init()` 与 `resurrect()` 共用 ⇒ 在 `live()` 末尾补静态重挂入口（如 `Release.onHeroRevive`）。**别**改成 `revivePersists=true`（`verify_oracle_fixes.py`） |
| 多段连击的**击杀计数恒为 0** | 计数一律「**先 damage、再判生死**」（`wasAlive && !enemy.isAlive()`）—— 在 `enemy.damage()` **之前**判 `isAlive()` 恒为 true |
| **新武器掉不出来** / 末位武器永远抽不到 | `Generator` 取物是 `Random.chances(cat.probs)` 拿下标再 `cat.classes[i]` ⇒ **`classes` 与 `defaultProbs` 必须等长**（现 WEP_T2 = 12/12）；`probs` 是**并列**权重、不是累加 |
| **用子类替换共享 buff 来加「某装备专属的一层逻辑」** ⇒ buff 栏多一个同款图标、若干「身上有没有这 buff」判定看不见它 | `Char.buff(Class)` 是**精确类匹配** `b.getClass()==c`（**不是** `isInstance`），`Buff.affect(t,cls)` 也走它 ⇒ 已有基类实例时会**另建一份**；但 `buffs(Class)`（伤害池走的就是它）是 `isInstance` ⇒ **吸收仍正确**。优先在父类开钩子；确要换类就显式确认 `buffs()` 路径并写下取舍（先例 `DwarfKing.DKBarrior extends Barrier`）（`verify_artifact_enhance.py` §D）。⚠️ **反向坑**：拿**抽象基类**去查（如 `buff(ChampionEnemy.class)`）也永远 `null` ⇒ 「有没有某种精英」这类判定**必须**用 `buffs(Class)`（`verify_hod_netzach.py`） |
| 父类钩子与覆写方**各打一次**提示 ⇒ 弹两条一模一样的消息 | 提示统一由**父类**播出、钩子内**只算不打**（回归断言钩子体内不得出现 `GLog`/消息键） |
| **专属装备死亡后进了英雄遗骸**、被别的角色捡走（本次：双蛇杖） | `Item.bones` 默认 false，但 **`EquipableItem` 置 true** ⇒ 武器/护甲/神器**默认都进遗骸**（`Bones.get` 取出还会降 +3 并强制诅咒）；专属件在实例块显式 `bones = false;`；**跨局继承取等级一律 `trueLevel()`**（`level()` 含 `curseInfusionBonus` ⇒ 白送等级）；断言搜 `bones = true` 前先 `strip_comments`。回归 `verify_hermes_firelink.py` |
| **专属武器「锁在手上」照样能被卖掉** | `doUnequip()` 拒绝卸下**挡不住商店**：收购窗口 `WndBag` 把主手 `weapon` 与副手 `secondWep` **一起**摆进列表；而 `Item.sellable()` 默认＝`!unique \|\| stackable`，专属普通武器没标 `unique` ⇒ 默认可售。覆写 `Item.sellable()` 返回 false （放**基类一处**覆盖全系列），**别**改用 `unique`（会连坐锻造/附魔）。`verify_sealed_sellable.py` |
| **给「命中 / 闪避」加全局倍率时只改了 `Char.hit`** | 全作有**三处**在算「命中 vs 闪避」、各自复刻了一整套乘区：① `Char.hit`（真正的判定）② `Talent.dodgeAsDamageReduction` ③ `Stone.proc`（磐岩附魔）—— 只改 ① 会让 ②③ **换算出的概率与真实命中率漂移**（不报错）。收口到 `Trials.finalAccuracy(attacker, defender)` / `finalEvasion(defender, attacker)`（**参数是「防守方, 攻击方」**），三处同改（倍率自带「非敌方 / 未开考验 ⇒ 1」的门控）。「区域」用 `Dungeon.scalingDepth()` **不是** `depth`（升天 = 五区封顶）（`verify_hod_netzach.py` + `HodNetzachProbe.java`） |
| **想「距离越远越透明」而在 `update()` / `resetColor()` 里改 alpha** | 隐形走 `AlphaTweener`（**每帧持续写** `am`/`aa`）、受击闪光走 `hardlight`（只碰颜色）⇒ 持久位置改 alpha 必与隐形打架。只在 **`CharSprite.draw()`** 里 `super.draw()` **前**乘、**后**还原（只影响这一帧）；`fade==1` 时零影响。血条也要 `&& enemyFade > 0f`（否则浮空血条暴露位置）。**纯视觉**：不改命中、不写 `invisible`、不碰 AI（`verify_hod_netzach.py` ③） |
| **窗口里的按钮 / 开关点上去毫无反应**（本轮：考验详情窗的「开启该考验」） | `Button` 的 `PointerArea` 在**构造时**就注册进全局 `PointerEvent` 监听表，而那张表是 `new Signal<>(true)`（**stackMode**：`add()` 走 `addFirst`、`dispatch()` 从队首遍历、**首个返回 true 即 return**）⇒ **后注册者优先、第一个命中的吞掉其余**；`Window` 构造会加一个**覆盖全屏**的 blocker（点窗外即关窗），且它照样拦截（`Gizmo.isActive()` 只看 `active && parent.isActive()`，**不看 visible**，而 blocker 正是 `visible=false`）。⇒ 任何**早于**该 blocker **构造**的控件都被自己窗口吃掉 | **窗口内的控件一律在子类构造体、`super(...)` 之后 new**（SPD 本来就这么写）；别为了「把逻辑留在调用方」先在调用方 new 好再传进来。万不得已可用 `button.givePointerPriority()` 把它的 PointerArea 提到队首。同族坑见「UI 组件构造即崩」（都是**构造顺序**）；回归 `verify_trials_tree_ui.py` |

## 7. 自定义武器/物品开发经验（通用）

> **原文（全量、逐字）已迁至 `docs/handbook/weapon-item-dev.md`**；本节只留检索入口，避免常驻指令超预算。
> 取用：`python _chk/docfind.py <关键词>` 或 `grep -n "<关键词>" docs/handbook/weapon-item-dev.md`，再按行号定点 `read`。

新武器/物品/饰品/附魔、掉落池接线。另参 `docs/weapon-creation-guide.md`。

<details><summary>本节词表（15 条，可直接拿去 grep）</summary>

- 新增武器创作指南
- 武器类
- 入掉落池
- 物品图标
- 物品显示常驻粒子
- 受击特效
- 装备时特效
- 等待动作钩子
- 伤害浮字图标
- 自定义 buff 图标
- 消息格式
- 粒子类
- 字母制等级显示
- 装备中物品不可加入合成/强化/分解
- 引擎钩子类武器效果三例（2026-09-06 四阶E.G.O 拘束/盲目/渴望，…

</details>

## 8. 法杖类武器（失乐园）与 buff 特效开发经验

> **原文（全量、逐字）已迁至 `docs/handbook/wand-buff-fx-dev.md`**；本节只留检索入口，避免常驻指令超预算。
> 取用：`python _chk/docfind.py <关键词>` 或 `grep -n "<关键词>" docs/handbook/wand-buff-fx-dev.md`，再按行号定点 `read`。

法杖/粒子/特效、图层与回收、时间常量标定。

<details><summary>本节词表（20 条，可直接拿去 grep）</summary>

- "武器+法杖"复合结构
- 自定义施法效果
- 允许对自身施法
- 施法后结算
- 伤害成长套用
- buff 百分比属性加成
- buff 文本特殊颜色
- 多帧贴图特效
- 跟随单位的标记贴图
- 图层顺序
- 全屏屏幕空间滤镜怎么做到「压住画面但不遮 UI」
- 屏幕空间「热浪扭曲」＝换掉全局着色器，别自己开 FBO
- 屏幕空间的粒子（沿屏幕边缘铺火苗）
- 「跟随角色的持续粒子」别自己 `new Emitter`，借 `CharSpri…
- 静态 `Image` 要水平镜像只能 `scale.x = -1`
- 贴图特效里的「时间」常量要拿真实移动参数标定
- 场景销毁会递归清 `parent`，可据此判断「视觉是否随场景销毁」
- `Group.recycle(Class)` 只发槽位、不调 `reset()`
- 跨组挂载的节点要自己回收；已 `destroy()` 的组不可复用
- `Char.restoreFromBundle` 顺序 = 反序列化时已调 `r…

</details>


## 9. 文档维护（索引再生成）

- 改了任何 `.md`（新增章节 / 追加 features 批次 / 新增 memory 日志 / 新增 `_chk` 脚本）后跑一次：
  `python _chk/build_doc_index.py --apply`（幂等；先 `--check` 只看差异）。
- 常驻红线的新发现 → 写进本文件对应节（§1/§2/§3/§5 或 §6 精简表）；
  长尾细节 → 写进 `docs/handbook/*.md` **并**在 `docs/features.md` 追加一批记录。
- **本文件有 64KB 硬预算**（超了就被截断）。`build_doc_index.py --check` 会报占用；
  接近 54KB 就得把长尾搬去 `docs/handbook/`，而不是继续往本文件里堆。
- `.workbuddy/memory/` 与 `docs/features.md` 是**追加式归档**：只往后写，不回改旧条；旧结论被推翻时新开一条并写明「修正某日旧结论」。
