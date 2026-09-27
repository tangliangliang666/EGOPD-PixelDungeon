# -*- coding: utf-8 -*-
r"""修复 .workbuddy/memory/2026-09-23.md 的 §五：
行内 `python -c "..."` 的**反引号被 bash 当成命令替换**，把带反引号的片段整段删掉了。
这里从 §五 标题处截断，再写回正确文本。幂等：正确文本已存在则跳过。

⚠️ 教训：**永远不要**把含反引号（或 `$`、`\`）的 Python 源码塞进 `bash -c "..."` 的双引号里。
本仓既有红线是「别用 heredoc 传含转义的 Python 源码」，这次是同一类坑的另一个入口 —— 一律先 Write 成
`_chk/*.py` 再执行。
"""
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LOG = os.path.join(ROOT, '.workbuddy/memory/2026-09-23.md')

HEAD_MARK = '## 五、沉淀（skill 更新）'
GOOD_MARK = '`egopd-source-verify`'  # 正确文本的标志

ADD = u'''## 五、沉淀（skill 更新）

- **`egopd-source-verify`** 新增 5 条：
  ① boolean 谓词覆写成常量时，最硬的证据是 **`javap -c`**（覆写点只应有 `iconst_0; ireturn`，且各子类
  `javap -p` 输出里**不含**同名方法）—— 并且 `-cp` 里 `_chk/_javachk` 必须排最前，否则读到旧字节码；
  ② 重命名方法后注释里的 `{@link #旧名()}` 在 `javac` 下**完全静默**（只有 `javadoc` 任务才报，本项目不跑它）
  ⇒ 改名当轮就 `grep -rn "<旧名>"` 全仓扫一遍；
  ③ **幂等标记必须是「所有目标文件里都出现」的公共子串**（本次踩：标记只写在详细版的措辞里 ⇒
  第二个文件重跑时被重复插入一行）；
  ④ 别写「文件末尾不得有空行」这类守卫（长期追加型文档本来就留尾空行，会误报并中断追加）；
  ⑤ 文档里写死的「全量 N 行」计数会漂移（`AGENTS.md` 注记写 111、实际 106）⇒ 插入表格行的同轮顺手改成实测值。
- **`egopd-new-weapon`** 新增 §7.5「专属/锁定武器的**出口封锁**清单」：把 7 个出口（出售 / 嬗变 / 进遗骸 /
  被偷 / 掉落池 / 卸下丢弃 / 安卡清空背包）连同各自的**判据与消费点**列成表，并写清两条踩过的坑
  （「锁死在双手」挡不住出售；`unique` 一标三用、别拿它关单一出口）。速查表另加 2 行。
- **本仓新红线（补进本节自省）**：行内 `python -c "…"` 的双引号里出现**反引号**时，bash 会先做命令替换、
  把带反引号的片段**整段删掉**再交给 Python —— 本次直接把 §五 的正文啃成了 `****`。
  与「heredoc 还原反斜杠」同源：**凡是要写含 `\\` / `$` / 反引号的文本，一律先 Write 成 `_chk/*.py` 再跑。**
'''

with open(LOG, 'rb') as f:
    raw = f.read()
crlf = b'\r\n' in raw
text = raw.decode('utf-8').replace('\r\n', '\n')

if GOOD_MARK in text:
    print('SKIP：§五 已是正确文本')
else:
    idx = text.index(HEAD_MARK)
    head = text[:idx].rstrip('\n')
    assert '## 一、' in head and '## 四、' in head, '前四节不完整，需人工确认'
    assert '****' not in head, '前四节也被污染，需人工确认'
    new = head + '\n\n' + ADD.replace('\r\n', '\n')
    if not new.endswith('\n'):
        new += '\n'
    out = new.replace('\n', '\r\n') if crlf else new
    with open(LOG, 'wb') as f:
        f.write(out.encode('utf-8'))
    print('OK 已修复（%s，%d -> %d 字符）' % ('CRLF' if crlf else 'LF', len(text), len(new)))

# 自检
final = open(LOG, encoding='utf-8').read()
# ⚠️ 不能简单断言「不含 ****」：本轮正文里就有一处**故意引用**症状的字面量「被啃成 `****`」。
# 真正的判据是「没有『空标记 + 正文缺字』的形态」——即不得出现 `- **** 新增` / 行首 `****`。
assert not re.search(r'^\s*-?\s*\*\*\*\*', final, flags=re.M), '仍有「空标记」形态的残留'
assert ADD.count('`egopd-source-verify`') == 1
for s in ('## 一、', '## 二、', '## 三、', '## 四、', '## 五、'):
    assert s in final, '缺小节 ' + s
assert final.count(u'`javap -c`') >= 1, '正文里没有 javap 实证那条'
print('自检 OK：5 节齐全、无空标记残留，现 %d 字符（javap 出现 %d 次）'
      % (len(final), final.count(u'`javap -c`')))
