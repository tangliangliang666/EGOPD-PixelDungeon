#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成/刷新项目的文档目录索引（幂等，可反复跑）。

产物：
    docs/INDEX.md            总路由器：任务 → 读哪里；文件清单；常驻指令预算占用
    docs/index/handbook.md   AGENTS.md + docs/handbook/* 的章节行号锚点
    docs/index/docs.md       docs/ 其余文档 + 根目录设计稿 的章节行号锚点
    docs/index/features.md   docs/features.md 的批次 → 行号索引
    docs/index/memory.md     .workbuddy/memory/*.md 的日期/主题 → 行号索引
    docs/index/scripts.md    _chk/ 核验脚本清单（用途/分类/是否自带反例自测）

约定：追加式归档（features.md、memory/*.md）只往后写；写完跑本脚本刷新索引即可。
用法：
    python _chk/build_doc_index.py --check   # 只报会变动的文件，不写盘
    python _chk/build_doc_index.py --apply
"""

import argparse
import ast
import os
import re
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOCS = os.path.join(ROOT, 'docs')
INDEX_DIR = os.path.join(DOCS, 'index')
MEMORY_DIR = os.path.join(ROOT, '.workbuddy', 'memory')
CHK = os.path.join(ROOT, '_chk')

AGENTS = os.path.join(ROOT, 'AGENTS.md')
FEATURES = os.path.join(DOCS, 'features.md')

INSTRUCTION_BUDGET = 65536
HEADING_RE = re.compile(r'^(#{1,4})\s+(\S.*?)\s*$')
SKIP_DIR_PARTS = ('_bak_', '_droptest', '_mf_audio', 'build/', '.gradle', 'node_modules')


def read_text(path):
    with open(path, 'r', encoding='utf-8', errors='replace', newline='') as handle:
        return handle.read()


def stat_of(path):
    size = os.path.getsize(path)
    return size, len(read_text(path).split('\n'))


def headings(path, max_level=3, limit=500):
    """[(line_no, level, title)] for headings up to max_level."""
    out = []
    for no, line in enumerate(read_text(path).split('\n'), 1):
        match = HEADING_RE.match(line)
        if not match:
            continue
        level = len(match.group(1))
        if level > max_level:
            continue
        out.append((no, level, match.group(2)))
        if len(out) >= limit:
            out.append((no, level, '…（超出 %d 条，后续标题请用 grep 定位）' % limit))
            break
    return out


def anchor_table(path, rel, max_level=2, max_rows=70, extra_note=None):
    rows = headings(path, max_level=max_level, limit=max_rows)
    size, lines = stat_of(path)
    header = ['**`%s`** — %d 行 / %.1f KB%s\n' % (rel, lines, size / 1024.0, extra_note or ''), '']
    if not rows:
        return '\n'.join(header + ['- （无标题；整块文本，用 `grep`/`docfind` 定位）', '']) + '\n'
    out = header + ['| 行 | 级 | 标题 |', '|---|---|---|']
    for no, level, title in rows:
        title = title.replace('|', '\\|')
        out.append('| %d | h%d | %s |' % (no, level, title))
    if len(rows) >= max_rows:
        out.append('')
        out.append('> 已达 %d 行上限，更细的锚点用 `python _chk/docfind.py <关键词>`。' % max_rows)
    out.append('')
    return '\n'.join(out) + '\n'


BULLET_RE = re.compile(r'^- \*\*(.+?)\*\*')
ROW_SKIP = ('现象', '症状', '---', ':--', '处理', '原因', '要害')


def entry_table(path, rel, max_rows=500):
    """按「条目」索引：顶层 bullet（`- **X**`）与 markdown 表行（首格）。

    handbook 里的长尾原文是纯 bullet / 纯表格结构，没有小标题 ⇒ 必须按条目做锚点，
    否则只能靠 grep，失去了「目录索引」的意义。
    """
    body = read_text(path).split('\n')
    rows = []
    for no, line in enumerate(body, 1):
        match = BULLET_RE.match(line)
        if match:
            label = match.group(1).strip()
        elif line.startswith('| ') and line.count('|') >= 3:
            first = line.strip().strip('|').split('|')[0].strip()
            if not first or first.startswith(':') or any(first.startswith(s) for s in ROW_SKIP):
                continue
            label = first
        else:
            continue
        label = label.replace('|', '\\|').replace('**', '')
        if len(label) > 90:
            label = label[:90] + '…'
        rows.append((no, label))
        if len(rows) >= max_rows:
            break
    size, lines = stat_of(path)
    out = ['**`%s`** — %d 行 / %.1f KB\n' % (rel, lines, size / 1024.0), '',
           '| 行 | 条目 |', '|---|---|']
    for no, label in rows:
        out.append('| %d | %s |' % (no, label))
    if len(rows) >= max_rows:
        out.append('')
        out.append('> 已达 %d 行上限，其余用 `python _chk/docfind.py <关键词>`。' % max_rows)
    out.append('')
    return '\n'.join(out) + '\n'


# ------------------------------------------------------------------ scripts

SCRIPT_PURPOSE_NOTE = {
    'verify_': '核验（回归断言）',
    'check_': '检查/体检',
    'fix_': '一次性修补',
    'patch_': '一次性文档/源码批改',
    'import_': '素材入库/转码',
    'package_': '打包分发',
    'split_': '文档重构',
    'polish_': '文档重构',
    'build_doc_': '文档索引工具',
    'docfind': '文档检索工具',
}


def script_summary(path):
    name = os.path.basename(path)
    ext = os.path.splitext(name)[1].lower()
    text = read_text(path)
    purpose = None
    if ext == '.py':
        try:
            doc = ast.get_docstring(ast.parse(text))
        except SyntaxError:
            doc = None
        if doc:
            for line in doc.strip().split('\n'):
                line = line.strip()
                if line and not line.endswith('：'):
                    purpose = line
                    break
    if purpose is None:
        for line in text.split('\n')[:40]:
            stripped = line.strip()
            marker = None
            if stripped.startswith('//'):
                marker = stripped[2:]
            elif stripped.startswith('#') and not stripped.startswith('#!'):
                marker = stripped[1:]
            if marker and len(marker.strip()) > 6:
                purpose = marker.strip()
                break
    if purpose is None:
        purpose = '（无说明）'
    if len(purpose) > 110:
        purpose = purpose[:110] + '…'
    category = '其它'
    for prefix, label in SCRIPT_PURPOSE_NOTE.items():
        if name.startswith(prefix):
            category = label
            break
    if name.startswith('_'):
        category = '临时/日志残留'
    # 拆开字面量：否则本文件自己的源码里含有该 token，会把自己误判成「带自测」。
    selftest = '是' if ('--' + 'selftest') in text else ''
    return category, purpose, selftest


def build_scripts_index():
    out = ['# `_chk/` 脚本索引（生成物）',
           '',
           '> 由 `python _chk/build_doc_index.py --apply` 生成。**改完代码先在这里挑回归脚本**，别整个目录乱翻。',
           '> 「自测」列＝脚本自带反例自测模式（会故意喂反例、验证断言本身有效，而不只是全绿）。',
           '',
           '| 脚本 | 分类 | 自测 | 用途（取自文件头部说明） |', '|---|---|---|---|']
    count = 0
    for base, dirs, files in os.walk(CHK):
        dirs[:] = [d for d in dirs if not any(part in d for part in SKIP_DIR_PARTS)]
        for name in sorted(files):
            if os.path.splitext(name)[1].lower() not in ('.py', '.js', '.java'):
                continue
            path = os.path.join(base, name)
            rel = os.path.relpath(path, ROOT).replace('\\', '/')
            category, purpose, selftest = script_summary(path)
            out.append('| `%s` | %s | %s | %s |' % (name, category, selftest, purpose.replace('|', '\\|')))
            count += 1
    out.append('')
    out.append('共 %d 个脚本。' % count)
    out.append('')
    return '\n'.join(out)


# ------------------------------------------------------------------ memory

def build_memory_index():
    out = ['# `.workbuddy/memory/` 逐日工作日志索引（生成物）',
           '',
           '> 另一平台（WorkBuddy）的开发记录，**追加式归档**。**不要整读**：先在这里按日期/主题定位行号，再定点读。',
           '> 入口 `MEMORY.md` 是最高频红线的精简表（AGENTS.md §6 已内联同一份）。',
           '',
           '| 日期 | 文件 | 行 | KB | 主题（行号） |', '|---|---|---|---|---|']
    total = 0
    for name in sorted(os.listdir(MEMORY_DIR)):
        if not name.endswith('.md'):
            continue
        path = os.path.join(MEMORY_DIR, name)
        size, lines = stat_of(path)
        total += size
        rel = '.workbuddy/memory/' + name
        rows = headings(path, max_level=2)
        topics = '；'.join('%s（%d）' % (title.replace('|', '\\|'), no) for no, level, title in rows
                           if level == 2 and no > 1)
        if not topics:
            topics = '（无二级标题）'
        date = name[:-3]
        out.append('| %s | `%s` | %d | %.1f | %s |' % (date, rel, lines, size / 1024.0, topics))
    out.append('')
    out.append('共 %d 个日志、合计 %.0f KB。' % (len([n for n in os.listdir(MEMORY_DIR) if n.endswith('.md')]), total / 1024.0))
    out.append('')
    return '\n'.join(out)


# ------------------------------------------------------------------ features

def build_features_index():
    size, lines = stat_of(FEATURES)
    out = ['# `docs/features.md` 批次索引（生成物）',
           '',
           '> **追加式流水账，禁止整读**（%d 行 / %.0f KB）。按下面行号定点 `read offset=N limit=M`。' % (lines, size / 1024.0),
           '> 约定：每次改动在文件**末尾**追加一批（`# 日期 标题`），再跑 `build_doc_index.py` 刷新本索引。',
           '',
           '| 行 | 批次 | 子节（行号） |', '|---|---|---|']
    body = read_text(FEATURES).split('\n')
    batches = []  # (line_no, title, [(no,title)...])
    current = None
    for no, line in enumerate(body, 1):
        match = HEADING_RE.match(line)
        if not match:
            continue
        level = len(match.group(1))
        title = match.group(2).replace('|', '\\|')
        if level == 1:
            current = (no, title, [])
            batches.append(current)
        elif level == 2 and current is not None:
            current[2].append((no, title))
    for no, title, subs in batches:
        shown = subs[:8]
        subtext = '；'.join('%s（%d）' % (t, n) for n, t in shown) or '—'
        if len(subs) > len(shown):
            subtext += ' …另 %d 节' % (len(subs) - len(shown))
        out.append('| %d | **%s** | %s |' % (no, title, subtext))
    out.append('')
    out.append('共 %d 批。' % len(batches))
    out.append('')
    return '\n'.join(out)


# ------------------------------------------------------------------ router

ROUTER_HEAD = '''# 文档总索引（EGOPD）

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
'''

ROUTER_TAIL = '''
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
'''


def build_router(inventory):
    out = [ROUTER_HEAD.rstrip('\n')]
    for rel, size, lines, note in inventory:
        if rel == 'AGENTS.md':
            out.append('| `%s` | %d | 剩余 %d | %s |' % (rel, size, INSTRUCTION_BUDGET - size, note))
        else:
            out.append('| `%s` | %d | — | %s |' % (rel, size, note))
    out.append('')
    out.append('（`docs/handbook/*.md` 与 `AGENTS.md` 是**同一套内容的两半**：常驻的必须 ≤64KB，其余按需读，'
               '所以长尾放在 handbook 里不占指令预算。）')
    out.append('')
    out.append('## 3. 文件清单')
    out.append('')
    out.append('| 文件 | 行 | KB | 角色 | 何时读 |')
    out.append('|---|---|---|---|---|')
    agents_size, agents_lines = stat_of(AGENTS)
    out.append('| `AGENTS.md` | %d | %.1f | **常驻红线**（每次完整送达） | 永远 |'
               % (agents_lines, agents_size / 1024.0))
    for rel, role, when in CATALOG:
        path = os.path.join(ROOT, rel)
        if not os.path.exists(path):
            continue
        size, lines = stat_of(path)
        out.append('| `%s` | %d | %.1f | %s | %s |' % (rel, lines, size / 1024.0, role, when))
    out.append('')
    out.append('## 4. 归档（**不要整读**）')
    out.append('')
    for rel, role, when in ARCHIVE_CATALOG:
        if rel == '.workbuddy/memory/*.md':
            # 日志条数现算，避免手写数字随日志增长而失效（同名占位在 catalog 里是注释用）。
            n_mem = len([f for f in os.listdir(MEMORY_DIR)
                         if re.match(r'^\d{4}-\d{2}-\d{2}\.md$', f)])
            role = '%s（%d 个）' % (role.split('（')[0], n_mem)
        if '*' in rel or rel.endswith('/'):
            out.append('- `%s` — %s（%s）' % (rel, role, when))
            continue
        path = os.path.join(ROOT, rel)
        if not os.path.exists(path):
            continue
        size, lines = stat_of(path)
        out.append('- `%s` — %d 行 / %.1f KB。%s（%s）' % (rel, lines, size / 1024.0, role, when))
    out.append('')
    return '\n'.join(out) + ROUTER_TAIL


CATALOG = [
    ('docs/INDEX.md', '路由器（本文件）', '先读这个'),
    ('docs/index/handbook.md', 'AGENTS + handbook 章节锚点', '查行号时'),
    ('docs/index/docs.md', 'docs/ 与设计稿章节锚点', '查行号时'),
    ('docs/handbook/terrain-tilemap.md', '原 AGENTS §3 地形段原文', '动地形/图集/草皮时'),
    ('docs/handbook/spd-framework.md', '原 AGENTS §4 原文（钩子/汇聚点）', '找钩子时（最常查）'),
    ('docs/handbook/pitfalls.md', '原 AGENTS §6 全量陷阱表', '遇怪现象时'),
    ('docs/handbook/weapon-item-dev.md', '原 AGENTS §7 原文', '新武器/物品时'),
    ('docs/handbook/wand-buff-fx-dev.md', '原 AGENTS §8 原文', '法杖/特效时'),
    ('docs/features.md', '非职业功能档案＋追加式流水账', '按 docs/index/features.md 定位批次'),
    ('docs/ring-master.md', '环指大师全档案', '做环指相关开发时'),
    ('docs/oracle-class-design.md', '神谕设计记录', '做神谕相关开发时'),
    ('docs/oracle-implementation.md', '神谕实现冻结快照', '查神谕历史实现时'),
    ('docs/weapon-creation-guide.md', '武器创作流程', '新武器时'),
    ('docs/enchantment-implementation.md', '附魔体系学习文档', '动附魔时'),
    ('docs/terrain-creation-guide.md', '新地形手绘流程', '新增地形时'),
    ('docs/terrain-editor-guide.md', '地形编辑器使用与核验', '画房间地形时'),
    ('docs/android-apk-branding-guide.md', 'APK 图标/应用名完整指南', '改图标/应用名时'),
    ('docs/dsh-plugins.md', 'DSH 插件清单与本机环境接线（含 6 个缺失 skill 的缺口）', '装插件 / 改开发环境 / 找 skill 时'),
    ('docs/title-scene-structure.md', '标题界面（TitleScene）显示结构调研 + 布局复算结论', '改标题界面 / 任何 UI 场景前'),
    ('docs/boss-and-level-dev-guide.md', 'Boss 与楼层开发指南', '做 Boss/楼层时'),
    ('docs/hero-level-progression.md', '英雄 1~30 级数值调研', '数值设计时'),
    ('docs/dark-silence-design.md', '漆黑噤默设计稿', '相关设计时'),
    ('docs/hermes-caduceus-design.md', '赫尔墨斯双蛇杖设计稿', '相关设计时'),
    ('docs/recommended-changes.md', '上游建议改动清单（上游原文）', '参考'),
    ('docs/getting-started-desktop.md', '上游桌面编译指南', '环境问题时'),
    ('docs/getting-started-android.md', '上游 Android 编译指南', '环境问题时'),
    ('docs/getting-started-ios.md', '上游 iOS 编译指南', '环境问题时'),
    ('docs/desktop-build-progress.md', '桌面构建进度记录', '查构建历史时'),
    ('debug-console/README.md', '调试控制台独立库说明', '动调试库时'),
    ('README.md', '上游项目说明（上游原文）', '参考'),
    ('中指设计.txt', '中指·长兄设计稿', '相关设计时'),
    ('拇指设计.txt', '拇指·前二老板设计稿', '相关设计时'),
    ('罪种设计.txt', '罪种设计稿', '相关设计时'),
    ('EGOPD.txt', 'EGOPD 改动总览', '出改动栏时'),
]

ARCHIVE_CATALOG = [
    ('docs/archive/AGENTS-2026-09-21-full.md', '重构前 AGENTS.md 全量冻结副本', '仅考古，禁读'),
    ('docs/index/features.md', 'features.md 批次→行号索引', '定位某一批改动'),
    ('docs/index/memory.md', '另一平台逐日日志索引', '追溯当时决策'),
    ('docs/index/scripts.md', '_chk 脚本清单', '挑回归脚本'),
    ('.workbuddy/memory/MEMORY.md', '另一平台最高频红线精简表（AGENTS §6 已内联）', '入口文件'),
    ('.workbuddy/memory/*.md', '另一平台逐日工作日志', '按 docs/index/memory.md 定位'),
    ('_chk/_bak_2026-09-20c/', '**过期备份**：内含 185KB 旧 AGENTS.md 与 516KB 旧 features.md', '⚠️ 别读该目录 —— AGENTS.md 会被当成嵌套指令加载，白吃指令预算'),
]



def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true', help='只报会变动的文件')
    parser.add_argument('--apply', action='store_true', help='写盘')
    args = parser.parse_args()
    if not args.check and not args.apply:
        parser.error('需要 --check 或 --apply')

    products = {}

    parts = ['# `AGENTS.md` 与 `docs/handbook/` 章节/条目锚点（生成物）', '',
             '> 由 `python _chk/build_doc_index.py --apply` 生成。用行号做 `read offset=N limit=M`。',
             '> `docs/handbook/` 是重构前 AGENTS.md 的长尾原文，**保留了原节号**（旧文里的「见 §6」按此解析）。',
             '> handbook 是纯 bullet / 纯表格结构，所以这里按**条目**给锚点，不是按标题。', '']
    parts.append(anchor_table(AGENTS, 'AGENTS.md', max_level=2, max_rows=40))
    for name in sorted(os.listdir(os.path.join(DOCS, 'handbook'))):
        if not name.endswith('.md'):
            continue
        path = os.path.join(DOCS, 'handbook', name)
        parts.append(entry_table(path, 'docs/handbook/' + name))
    products[os.path.join(INDEX_DIR, 'handbook.md')] = '\n'.join(parts)

    docs_parts = ['# `docs/` 其余文档 + 根目录设计稿 章节锚点（生成物）', '',
                  '> 由 `python _chk/build_doc_index.py --apply` 生成。只到 h2（更细的锚点用 '
                  '`python _chk/docfind.py <关键词>`）。', '']
    for name in sorted(os.listdir(DOCS)):
        if not name.endswith('.md') or name == 'INDEX.md':
            continue
        path = os.path.join(DOCS, name)
        if os.path.isfile(path):
            docs_parts.append(anchor_table(path, 'docs/' + name))
    for name in sorted(os.listdir(ROOT)):
        if name.endswith('.txt'):
            docs_parts.append(anchor_table(os.path.join(ROOT, name), name, max_level=3, max_rows=40))
    products[os.path.join(INDEX_DIR, 'docs.md')] = '\n'.join(docs_parts)

    products[os.path.join(INDEX_DIR, 'features.md')] = build_features_index()
    products[os.path.join(INDEX_DIR, 'memory.md')] = build_memory_index()
    products[os.path.join(INDEX_DIR, 'scripts.md')] = build_scripts_index()

    inventory = [('AGENTS.md', os.path.getsize(AGENTS), stat_of(AGENTS)[1], '常驻红线（每次送达）')]
    handbook_dir = os.path.join(DOCS, 'handbook')
    for name in sorted(os.listdir(handbook_dir)):
        if not name.endswith('.md'):
            continue
        path = os.path.join(handbook_dir, name)
        size, lines = stat_of(path)
        inventory.append(('docs/handbook/' + name, size, lines, '长尾原文（按需读）'))
    products[os.path.join(DOCS, 'INDEX.md')] = build_router(inventory)

    agents_size = os.path.getsize(AGENTS)
    print('AGENTS.md 常驻指令占用：%d / %d 字节（余量 %d）' % (agents_size, INSTRUCTION_BUDGET,
                                                     INSTRUCTION_BUDGET - agents_size))
    changed = []
    for path, text in sorted(products.items()):
        rel = os.path.relpath(path, ROOT).replace('\\', '/')
        old = read_text(path) if os.path.exists(path) else None
        flag = '新增' if old is None else ('更新' if old != text else '不变')
        if old != text:
            changed.append(rel)
        print('  %-30s %s  %7d 字节' % (rel, flag, len(text.encode('utf-8'))))
    if agents_size > 55296:
        print('  !! AGENTS.md 已接近/超过 54KB 安全线，把长尾搬去 docs/handbook/')

    if args.check:
        print('\n[check] 会变动 %d 个文件：%s' % (len(changed), ', '.join(changed) or '（无）'))
        return

    for path, text in products.items():
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w', encoding='utf-8', newline='') as handle:
            handle.write(text)
    print('\n[apply] 已写入 %d 个索引文件。' % len(products))


if __name__ == '__main__':
    main()
