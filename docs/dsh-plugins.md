# DSH 插件与开发环境接线（本机实测，2026-09-21）

> 用途：记录「本项目所在的 DSH 环境装了哪些插件、还能开哪些、开了各自解决什么」。
> **证据标记**：`[核]` = 我在本机亲自读过源码/文件确认；`[报]` = 来自子代理调研、我**未**逐条复核；
> `[网]` = 网络检索结果、**未**审源码。
> 索引：`docs/INDEX.md`。

## 1. DSH 的「插件」是什么 `[核]`

- 插件 = **Cordis Loader 的一个条目**，`name` 就是 npm 包名；在 bundle 的 `cordis.patch.yml` 里以
  `- id: … / name: …` 声明，后续层可按 `id` 覆盖或 `disabled` 禁用。
  证据：`packages/bundle/base/cordis.patch.yml`（451 行）。
- **本机 profile**：`C:\Users\14675\.dsh\profiles\web\package.json` 只有两个 bundle ——
  `@deepseek-ai/dsh-base` + `@deepseek-ai/dsh-web-app`；`dependencies: {}` ⇒ **零第三方插件**。
- **用户补丁层**（唯一该改的文件）：`C:\Users\14675\.dsh\profiles\web\cordis.patch.yml`，现为 `[]`。
  另有一层优先级更高的 `$DSH_HOME/cordis.patch.yml`（**本机不存在**）。
- **不要改** `profiles/web/cordis.yml`：每次启动都被重写成 `[]`。
- 生效顺序：各 bundle 补丁 → profile 的 `cordis.patch.yml` → `$DSH_HOME/cordis.patch.yml` → `--patch` 叠加。
  两个用户补丁文件都被**监听并热重载**。
- 只读查看已加载条目：GUI 设置 → 插件 → 插件列表（Remote `pluginInventory/list`）。
  **该服务没有安装/启停能力**，只能读 `entryId / moduleName / enabled / fiberPhase`。
- 不改动任何东西就能校验组合：

```bat
cd D:\deepseek-harness\deepseek-harness
pnpm dsh --profile web --dump-config          rem 打印合成后的树 + 每行由哪个文件提供
pnpm dsh --profile web --dump-default-config  rem 只看 bundle 层
```

- 安装插件 = 转发给 pnpm：`pnpm dsh plugin --profile web add <包>`
  （**本机 `dsh` 不在 PATH 上**，要从 checkout 里用 `pnpm dsh …`；`pnpm` 已在）。

## 2. 与本项目直接相关的既有条目

| 条目 | 现状 `[核]` | 与本项目的关系 |
|---|---|---|
| `agent-instructions` | base 里 `maxBytes: 65536` | **就是它导致 AGENTS.md 被截断**。渲染器**保留头部、截掉尾部**（`render.ts` 的 `truncateUtf8` 取 `bytes.subarray(0, end)`），且「先整份丢弃较宽的泛化文件，再截最具体的那一份」⇒ 位于**文件末尾**的 §5~§8 约 96KB 从未送达。已按「瘦身 + 索引」解决（197KB → 45.7KB） |
| ↑ 想调大预算要改哪里 | ⚠️ `[核]` base 的那行在 web 里是 `disabled: true`（`packages/bundle/web-app/cordis.patch.yml:401-402`），**真正生效的是 `standard` 预设里的同名行**：`apps/cli/config/agent-presets/standard/agent.cordis.yml:30-33`（同样是 65536） | 所以改 base 行**没用**。shipped 预设只读；要改得先 `agentPresets.copy(...)` 到 `$DSH_HOME/.agent-presets` 再改。**不建议**：调大只是把 token 成本摊到每次请求 |
| `skill` / `skill-filesystem` / `tool-skill` | 已挂载（经 `standard` 预设），`watch: true` | 技能是**本项目该用的「插件」形态**。见 §4 |
| `session-query-sqlite` | `openAt: never`、`path: ':memory:'` | 会话**全文检索默认关闭**（搜索报 `SESSION_QUERY_SEARCH_DISABLED`）。历史会话多时值得开 |
| `tool-result-pruner` / `spill-policy` | `thresholdChars: 8192`、`maxInlineBytes: 50000` | 已自动瘦身超大工具输出。⚠️ `[报]` **`read` 的结果不会被 spill** ⇒ 整读 `docs/features.md` 不会被自动裁剪，只能靠 `read offset/limit` 自律 |
| `tool-pwsh` | `[核]` 前台 `timeoutMs` 默认 **120000**（`packages/shell/bash-local/src/index.ts:107`）；**后台运行忽略 timeoutMs**（同文件 `:256`） | **对本项目关键**：`gradle.bat :core:compileJava` / `:android:assembleDebug` / `:desktop:debug` 前台跑可能超 120s 被掐 ⇒ 一律用后台任务（`run_in_background` + `job_output`），正好对应本手册 §1 的 `executionHistory.lock` 提醒 |
| `tool-fs-search` | 已挂载 | 提供 `grep`/`glob`；`[报]` glob 走 `--no-ignore --hidden`，会扫进 `build/`、`android/build/` |

## 3. 未挂载、但适合本项目的候选

**⚠️ 先看这条 `[核]`（会改变结论）**：安装回退目录 `C:\Users\14675\.dsh\profiles\node_modules\@deepseek-ai\`
里**没有** `dsh-lsp*`、`dsh-hooks-*`。`apps/cli/package.json` 的依赖闭包里也不含它们。
⇒ **裸名写 `@deepseek-ai/dsh-lsp` 的行在本机解析不了**。两条可行路径：

```bat
rem (a) 装进 profile（会写 C:\Users\14675\.dsh\profiles\web\ —— 需你确认后再做）
pnpm dsh plugin --profile web add @deepseek-ai/dsh-lsp @deepseek-ai/dsh-lsp-stdio @deepseek-ai/dsh-tool-lsp
rem (b) 在 --patch 叠加文件里用**绝对路径**指到源码 checkout（不装任何东西）
```

| 候选 | 解决什么 | 代价 / 前提 |
|---|---|---|
| **hooks 桥接** `@deepseek-ai/dsh-hooks-claude-code` | 在 `PreToolUse`（→`tools/pre-execute`）/ `PostToolUse`（→`tools/post-execute`）等生命周期跑 shell 钩子。**最合适的用法**：改完 `.java`/`.properties` 后自动跑 `python _chk/check_utf8_all.py`（本项目头号静默故障） | 包在 checkout 里但**不在安装回退**（见上）；`[报]` 桥接只支持 CC 30 个事件中的 7 个，且 `updatedInput` 会被解析但**不生效**、`allow` 不预先批准、`Stop` 无循环保护 |
| **LSP 家族**（**探索性，不建议现在做**） `lsp/lsp`+`lsp-stdio`+`tool-lsp` | 给模型 `goToDefinition` / `findReferences` / `goToImplementation` / `hover`。本项目大量工作是「找某钩子的全部消费者」，findReferences 正中要害 | ⚠️ 核实后的四条硬限制：①**只有这四种操作** —— 无 diagnostics、无 call hierarchy、无 rename/code action，也没有通用 JSON-RPC 逃生口 ⇒ 你的 `_chk/` javac 检查仍是唯一真正的诊断手段；②**仓库里不存在任何 Java 语言服务器预置** —— `packages/lsp/lsp-stdio/package.json` 的依赖只有 `typescript-language-server`，全仓无 jdtls 相关命中 ⇒ 你会是第一个把 Java + DSH LSP 跑通的人；③`[报]` 若服务器上报的 `positionEncoding` 不是 utf-16 则**协议报错**，且 `initialize.processId` 为 `null` ⇒ **硬杀 harness 会遗留孤儿语言服务器进程**；④必须用 `session.header.cwd` 当 workspace root、**无回退**（cwd 不在 `D:\PD` 时该工具直接不可用）。另需自备 jdtls 并显式钉到 JDK 21（本机 PATH 上的 `java` 是 Java 8） |
| **会话内容检索** | 改 `session-query-sqlite` 的 `openAt: never → first-search` 并给持久 `path` | 纯配置，无需装包 |
| **MCP 服务器** `dsh-mcp-client` | 已**在**安装回退里；只是没挂任何 server 行。仓库自带 `examples/mcp-memory/*.cordis.yml` 三个默认关闭的记忆服务器叠加文件 | `[报]` **只暴露 tools**：resources 与 prompts 不会被呈现（客户端上报空能力）⇒ 只提供 resources/prompts 的「知识库」型 server 在这里是隐形的。挂上后第三方 server 代码就会在本机跑 |

社区候选（`[网]`，**均未审源码，不做背书**）：`dsh-lsp-packs`（含 Java/JDT 配置）、`stalegreen`（把 test/build/lint 结论做成收据、过期就再推一步 —— 形态最贴近本项目的 `_chk/` 断言）、`dsh-doc-guard`（文档↔代码漂移审计，与本文件的索引不变量同题）、`dsh-knit`（列出工作区已存在的 md 并排序）、`dsh-fast`（会话/缓存诊断）、`dsh-memory-lite`（按需取用的记忆）、`dsh-turnsnap`（每回合 git 检查点）。
清单入口：[0xsline](https://github.com/0xsline/awesome-deepseek-harness)、[Zhiyuan-Fan](https://github.com/Zhiyuan-Fan/Awesome-DeepSeek-Harness-Plugins)、[XiaomingX](https://github.com/XiaomingX/awesome-deepseek-harness)。
**没有官方插件市场**（`[核]` checkout 内无 catalog/marketplace/registry 文件）；官方文档 <https://deepseek-harness.github.io/deepseek-harness/develop/basic/publish>。
`[网]` 另注：本机跑的是 checkout 版 **0.1.0-rc.5**，npm 上 `@deepseek-ai/dsh` 已是 **0.1.5-rc.2** ⇒ 第三方插件可能按新版写的、在本机加载不了，装完务必 `--dump-config` 复核。

## 4. ⚠️ 已发现的实际缺口：6 个被引用的 skill 根本不存在 `[核]`

`AGENTS.md` 与 `.workbuddy/memory/MEMORY.md` 引用了 6 个 skill：

```
egopd-source-verify     改完核验流水线（AGENTS.md §1、《MEMORY.md》都点名）
egopd-release-package   出包
egopd-apk-icon-name     APK 图标/应用名（AGENTS.md §6 精简表也点名）
egopd-terrain-editor    地形编辑器
egopd-terrain-tileset   新地形图集
egopd-sfx-import        音效入库
```

但**本机任何 skill 根目录里都没有**（已逐个探过，全部 `False`）：

| rank | 路径 | 存在？ |
|---|---|---|
| 100 | `D:\PD\.dsh\skills` | ✗ |
| 200 | `D:\PD\.agents\skills` | ✗ |
| 400 | `C:\Users\14675\.dsh\skills` | ✗ |
| 500 | `C:\Users\14675\.agents\skills` | ✗ |

⇒ 「流水线见 skill `egopd-source-verify`」这类指引目前是**死引用**（项目根＝最近的 `.git` 祖先＝`D:\PD`）。
补法（`skill-filesystem` 的 `watch: true` 会热发现，**不用重启**）：写成
`D:\PD\.dsh\skills\<名字>\SKILL.md`（目录 bundle，**只支持一层深**）或 `D:\PD\.dsh\skills\<名字>.md`（平铺），
frontmatter 必需 `name`（kebab-case）与 `description`，可选 `whenToUse`。
**不要把过程写进 AGENTS.md** —— 那会挤占 64KB 常驻预算；skill 是按需加载的。

## 5. 结论（按性价比排序）

1. **补齐 6 个 `egopd-*` skill** —— 零安装、热生效、把死引用变活；纯收益。
2. **Gradle 一律走后台任务**（`run_in_background`）—— 不是插件，是行为修正，直接避免前台 120s 超时被掐。
3. **开 `session-query-sqlite` 的内容检索**（纯配置），让历史会话可搜。
4. **加 hooks 桥接，把 `check_utf8_all.py` 挂到 PostToolUse** —— 但要先解决「包不在安装回退」的问题（§3 的 (a)/(b)）。
5. **要「装点什么真东西」就装第一方可选 bundle + 复制 preset**（§6），比 LSP 稳妥：同属第一方、无需第三方信任。
6. **LSP 家族 = 探索性，不建议现在做**（§3 已列四条硬限制；最大问题是仓库里根本没有 Java 语言服务器预置）。
7. 第三方插件：先审源码再装；本文件只登记线索。**保持 `agent-instructions.maxBytes = 65536`**，继续靠「瘦身 + 索引」。

## 6. 补充：装「官方可选 bundle」的通用通道 `[核]`

`apps/cli/reference/README.md:43,80` 确认：`dsh plugin --profile <name> <pnpm 参数…>` 是**通用 pnpm 转发器** ——
`add` / `remove` / `why` / `update` 及所有 pnpm 动词原样可用；每次成功后按**已安装状态**重算 `dsh.profile.bundles`
（声明了 `"dsh": { "bundle": { "patch": … } }` 的依赖自动进层栈，卸载即出栈）。相对路径按**调用目录**锚定。
⇒ 不必等某个包有专门文档，任何已发布的 bundle 都能这么装。

⚠️ **版本落差**（本机 `package.json` = `0.1.0-rc.5`；npm 上是 `0.1.5-rc.2`）：

- 上游文档举例的 `add @deepseek-ai/dsh-subagent-codex` **在本机 checkout 里不存在** ——
  `apps/cli/reference/README.md` 只有 84 行、无此命令；该包名在本机仅出现在
  `apps/cli/tests/web-agent-presets.e2e.ts:451-452` 的**测试组合行**里。
- `packages/bundle/` 本机只有 `base` / `headless` / `web-app`，上游还多 `acp-app` / `sdk-app` / `sdk-minimal`。
- ⇒ 别照上游文档的命令直接抄；先 `--dump-config` 验证。

### 可选：把 Codex / Claude Code 当子代理 `[核]`

`apps/cli/config/agent-presets/standard/agent.cordis.yml:200-220` 里有两条**默认 `disabled: true`** 的提供方行，
注释原文：

> Product providers are host-plane singletons. Copy this preset, then remove `disabled` from either ordinary tool row
> to expose that product only to agents composed from the copy.

且 `dsh-subagent-codex` / `dsh-subagent-claude-code` **不在**安装回退目录里（回退里只有 `dsh-subagent`、
`dsh-subagent-*-in-process`、`dsh-tool-subagent*`）⇒ **装包是第一步，不是全部**。完整流程四步：

1. `pnpm dsh plugin --profile web add @deepseek-ai/dsh-subagent-codex`
2. 复制一个 preset（`ctx.agentPresets.copy(from, id, name?)`，**copy-only**：拒非法 id、拒已占用、拒覆盖）
3. 在**副本**里去掉那一行的 `disabled`
4. 给**空白会话**选中它（会话一旦产出内容就 `agent-preset-locked`，不能重组）

适合你的场景：你同时在别的平台开发，这条能让 DSH 直接调 Codex / Claude Code 当子代理。
代价：要装包 + 动 preset，且属于上游较新特性，本机版本未必完全对齐。

### 稳定性与信任边界 `[网][核]`

- DSH 官方自述处于 **developer preview**，明确会有**破坏性变更** ⇒ 每次升级 DSH 后都该重跑 `--dump-config` 复核；
  锁插件版本挡不住 DSH 侧的破坏。
- `[网]` 社区清单自己的安全声明（原文大意）：**安装插件＝用你自己的权限在本机跑第三方代码，能读你的文件、用你的凭据、访问网络；
  工具审批不会沙箱化插件代码**。⇒ 只装 scope 下来的 `@deepseek-ai/*` 第一方包最稳。
- `[网]` npm 上有**名称占用**：`dsh` 是 2022 年一个无关的 JS shell，`deepseek-harness` 是第三方占位（v0.0.1），
  `dsh-hello-plugin` 是对官方教程示例名的占位 ⇒ 认准 `@deepseek-ai/` 前缀。
