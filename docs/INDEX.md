# 文档总索引（EGOPD）

> **生成物**：`python _chk/build_doc_index.py --apply`（改完任何 `.md` 后重跑，幂等）。
> **用法**：在这里找到 `文件 → 行号`，再用 `read` 的 `offset`/`limit` 定点读；
> 或一条命令跨语料搜：`python _chk/docfind.py <关键词>`。
> **常驻红线**在 `AGENTS.md`（≤64KB，每次都完整送达模型）；**长尾原文**在 `docs/handbook/`，按需读。

## 1. 速查：我要做的事 → 读哪里

| 我要做的事 | 先读 |
|---|---|
| 编译 / 打包 / 跑桌面调试 | `AGENTS.md` §1 |
| 出 EGOPD 新版本（固定三步） | `AGENTS.md` §1「出 EGOPD 新版本的固定三步」 |
| 改文本 / 本地化 / 描述文本风格约定 | `AGENTS.md` §2 |
| 新物品图标 / 贴图 / 音效（三步） | `AGENTS.md` §3 |
| 地图贴图 / 分层 tilemap / 草皮遮挡 | `docs/handbook/terrain-tilemap.md` |
| 画房间地形（可视化编辑器） | `docs/terrain-editor-guide.md` |
| 新增一种地形（手绘流程 + 逐帧核验） | `docs/terrain-creation-guide.md` |
| 找钩子 / 汇聚点（buff、伤害、命中、存档、回合…） | `docs/handbook/spd-framework.md` |
| 新职业 / 转职 / 盔甲技能「接入清单」 | `docs/handbook/spd-framework.md`（搜「接入清单」） |
| 新武器 / 物品 / 附魔 / 掉落池 | `docs/handbook/weapon-item-dev.md`、`docs/weapon-creation-guide.md`、`docs/enchantment-implementation.md` |
| 法杖 / 粒子 / 特效 / 图层与回收 | `docs/handbook/wand-buff-fx-dev.md` |
| 环指大师（RING_MASTER） | `docs/ring-master.md` |
| 神谕代行者（ORACLE） | `docs/oracle-class-design.md`、`docs/oracle-implementation.md` |
| 遇到怪现象 / 崩溃 / 静默失效 | `AGENTS.md` §6 精简表 → `docs/handbook/pitfalls.md` 全量表 |
| APK 图标 / 应用名 / 存档路径 | `AGENTS.md` §3「APK 桌面图标」、`docs/android-apk-branding-guide.md` |
| 英雄 1~30 级数值 / 曲线 | `docs/hero-level-progression.md`（可复跑 `_chk/hero_level_curve.py`） |
| **负数道具等级**能不能用 / 各品类怎么表现 | `docs/negative-item-levels.md`（可复跑 `_chk/NegLevelProbe.java`） |
| 挑战 / Boss / 楼层 / 彩蛋 | `docs/index/features.md` 定位批次 → `docs/features.md`；另 `docs/boss-and-level-dev-guide.md` |
| 设计稿（中指 / 拇指 / 罪种 / EGOPD 改动） | 根目录 `中指设计.txt`、`拇指设计.txt`、`罪种设计.txt`、`EGOPD.txt` |
| 某次改动「当时为什么这么改」 | `docs/index/memory.md` → `.workbuddy/memory/<日期>.md` |
| 挑回归 / 核验脚本 | `docs/index/scripts.md` |
| 装 DSH 插件 / 改开发环境 / 找 skill | `docs/dsh-plugins.md` |
| 改标题界面 / 加 UI 控件 | `docs/title-scene-structure.md`（含 `_chk/title_layout_calc.py` 布局复算器） |
| 一条命令搜遍所有文档 | `python _chk/docfind.py <关键词>` |
| 调试控制台（WndDebug / 独立库） | `AGENTS.md` §3、`debug-console/README.md`、`_chk/verify_wnddebug_restore.py` |

## 2. 常驻指令预算占用

| 文件 | 字节 | 预算 | 说明 |
|---|---|---|---|
| `AGENTS.md` | 54731 | 剩余 10805 | 常驻红线（每次送达） |
| `docs/handbook/pitfalls.md` | 108274 | — | 长尾原文（按需读） |
| `docs/handbook/spd-framework.md` | 64562 | — | 长尾原文（按需读） |
| `docs/handbook/terrain-tilemap.md` | 12291 | — | 长尾原文（按需读） |
| `docs/handbook/wand-buff-fx-dev.md` | 14349 | — | 长尾原文（按需读） |
| `docs/handbook/weapon-item-dev.md` | 7315 | — | 长尾原文（按需读） |

（`docs/handbook/*.md` 与 `AGENTS.md` 是**同一套内容的两半**：常驻的必须 ≤64KB，其余按需读，所以长尾放在 handbook 里不占指令预算。）

## 3. 文件清单

| 文件 | 行 | KB | 角色 | 何时读 |
|---|---|---|---|---|
| `AGENTS.md` | 355 | 53.4 | **常驻红线**（每次完整送达） | 永远 |
| `docs/INDEX.md` | 117 | 8.9 | 路由器（本文件） | 先读这个 |
| `docs/index/handbook.md` | 285 | 20.6 | AGENTS + handbook 章节锚点 | 查行号时 |
| `docs/index/docs.md` | 468 | 18.1 | docs/ 与设计稿章节锚点 | 查行号时 |
| `docs/handbook/terrain-tilemap.md` | 31 | 12.0 | 原 AGENTS §3 地形段原文 | 动地形/图集/草皮时 |
| `docs/handbook/spd-framework.md` | 151 | 63.0 | 原 AGENTS §4 原文（钩子/汇聚点） | 找钩子时（最常查） |
| `docs/handbook/pitfalls.md` | 147 | 105.7 | 原 AGENTS §6 全量陷阱表 | 遇怪现象时 |
| `docs/handbook/weapon-item-dev.md` | 37 | 7.1 | 原 AGENTS §7 原文 | 新武器/物品时 |
| `docs/handbook/wand-buff-fx-dev.md` | 33 | 14.0 | 原 AGENTS §8 原文 | 法杖/特效时 |
| `docs/features.md` | 9945 | 811.9 | 非职业功能档案＋追加式流水账 | 按 docs/index/features.md 定位批次 |
| `docs/ring-master.md` | 188 | 68.3 | 环指大师全档案 | 做环指相关开发时 |
| `docs/oracle-class-design.md` | 105 | 11.9 | 神谕设计记录 | 做神谕相关开发时 |
| `docs/oracle-implementation.md` | 31 | 4.0 | 神谕实现冻结快照 | 查神谕历史实现时 |
| `docs/weapon-creation-guide.md` | 323 | 25.0 | 武器创作流程 | 新武器时 |
| `docs/enchantment-implementation.md` | 588 | 34.0 | 附魔体系学习文档 | 动附魔时 |
| `docs/terrain-creation-guide.md` | 548 | 28.5 | 新地形手绘流程 | 新增地形时 |
| `docs/terrain-editor-guide.md` | 1170 | 72.3 | 地形编辑器使用与核验 | 画房间地形时 |
| `docs/android-apk-branding-guide.md` | 237 | 11.9 | APK 图标/应用名完整指南 | 改图标/应用名时 |
| `docs/dsh-plugins.md` | 149 | 13.5 | DSH 插件清单与本机环境接线（含 6 个缺失 skill 的缺口） | 装插件 / 改开发环境 / 找 skill 时 |
| `docs/title-scene-structure.md` | 627 | 45.1 | 标题界面（TitleScene）显示结构调研 + 布局复算结论 | 改标题界面 / 任何 UI 场景前 |
| `docs/boss-and-level-dev-guide.md` | 324 | 26.4 | Boss 与楼层开发指南 | 做 Boss/楼层时 |
| `docs/hero-level-progression.md` | 182 | 9.8 | 英雄 1~30 级数值调研 | 数值设计时 |
| `docs/dark-silence-design.md` | 204 | 17.7 | 漆黑噤默设计稿 | 相关设计时 |
| `docs/hermes-caduceus-design.md` | 93 | 7.0 | 赫尔墨斯双蛇杖设计稿 | 相关设计时 |
| `docs/recommended-changes.md` | 73 | 8.1 | 上游建议改动清单（上游原文） | 参考 |
| `docs/getting-started-desktop.md` | 82 | 7.7 | 上游桌面编译指南 | 环境问题时 |
| `docs/getting-started-android.md` | 62 | 6.6 | 上游 Android 编译指南 | 环境问题时 |
| `docs/getting-started-ios.md` | 41 | 3.8 | 上游 iOS 编译指南 | 环境问题时 |
| `docs/desktop-build-progress.md` | 29 | 1.6 | 桌面构建进度记录 | 查构建历史时 |
| `debug-console/README.md` | 318 | 15.3 | 调试控制台独立库说明 | 动调试库时 |
| `README.md` | 27 | 2.4 | 上游项目说明（上游原文） | 参考 |
| `中指设计.txt` | 73 | 3.0 | 中指·长兄设计稿 | 相关设计时 |
| `拇指设计.txt` | 118 | 6.5 | 拇指·前二老板设计稿 | 相关设计时 |
| `罪种设计.txt` | 39 | 1.2 | 罪种设计稿 | 相关设计时 |
| `EGOPD.txt` | 53 | 1.8 | EGOPD 改动总览 | 出改动栏时 |

## 4. 归档（**不要整读**）

- `docs/archive/AGENTS-2026-09-21-full.md` — 446 行 / 192.9 KB。重构前 AGENTS.md 全量冻结副本（仅考古，禁读）
- `docs/index/features.md` — 118 行 / 37.1 KB。features.md 批次→行号索引（定位某一批改动）
- `docs/index/memory.md` — 33 行 / 19.5 KB。另一平台逐日日志索引（追溯当时决策）
- `docs/index/scripts.md` — 261 行 / 33.0 KB。_chk 脚本清单（挑回归脚本）
- `.workbuddy/memory/MEMORY.md` — 42 行 / 9.0 KB。另一平台最高频红线精简表（AGENTS §6 已内联）（入口文件）
- `.workbuddy/memory/*.md` — 另一平台逐日工作日志（22 个）（按 docs/index/memory.md 定位）
- `_chk/_bak_2026-09-20c/` — **过期备份**：内含 185KB 旧 AGENTS.md 与 516KB 旧 features.md（⚠️ 别读该目录 —— AGENTS.md 会被当成嵌套指令加载，白吃指令预算）

## 5. 各语料索引

| 索引 | 覆盖 |
|---|---|
| `docs/index/handbook.md` | `AGENTS.md` + `docs/handbook/*.md` 的章节行号锚点 |
| `docs/index/docs.md` | `docs/` 其余文档 + 根目录设计稿 的章节行号锚点 |
| `docs/index/features.md` | `docs/features.md` 批次 → 行号（**禁整读**） |
| `docs/index/memory.md` | `.workbuddy/memory/*.md` 日期/主题 → 行号（**禁整读**） |
| `docs/index/scripts.md` | `_chk/` 脚本清单（分类 / 自测 / 用途） |

## 6. 维护

- 追加式归档（`docs/features.md`、`.workbuddy/memory/*.md`）**只往后写**，写完跑 `python _chk/build_doc_index.py --apply`。
- `AGENTS.md` 有 64KB 硬预算（超了 DSH 会截断、长尾静默丢失）：`build_doc_index.py --check` 会报占用；
  接近 54KB 就把长尾搬去 `docs/handbook/` 并在 `docs/INDEX.md` §2 里登记。
- 冻结存档 `docs/archive/` 内文件**不要读**（仅供考古）。
