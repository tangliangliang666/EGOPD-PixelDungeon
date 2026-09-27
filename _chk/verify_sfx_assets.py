# -*- coding: utf-8 -*-
"""
音效资源交叉校验（EGOPD）：Assets.Sounds 常量 ↔ 真实文件 ↔ all[] 注册。

用法：
    python _chk/verify_sfx_assets.py           # 全量校验
    python _chk/verify_sfx_assets.py 前缀...    # 只关注指定前缀的常量（如 MIDDLEFINGER）

为什么必须有这个脚本（三条都是**不报错的静默故障**）：
  ① 声明了常量但**没塞进 `all[]`** ⇒ `Sample.load` 不加载它 ⇒ `Sample.play` 查不到 key 直接 `return -1`
     ⇒ 表现为「改了代码、没声音、日志干净」；
  ② 常量指向的文件**不存在** ⇒ 同上，依旧静默；
  ③ `all[]` 里写了**拼错的常量名** ⇒ 编译期就报未定义符号（这条反而最容易发现），但**漏写**发现不了。
所以本脚本断言的是**三向一致**：常量数 == 文件数 == `all[]` 成员数，且逐个对得上。

历史（每次新增音效都手动跑一遍的临时脚本，2026-09-18 固化成文件）：
  神谕 7 条 / 指令 2 条 / 拇指 17 条 / 中指 7 条 / 果冻 / 四首角色 BGM。
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS_JAVA = os.path.join(ROOT, 'core', 'src', 'main', 'java',
                           'com', 'shatteredpixel', 'shatteredpixeldungeon', 'Assets.java')
ASSETS_DIR = os.path.join(ROOT, 'core', 'src', 'main', 'assets')


def sounds_block(text):
    """截出 `public static class Sounds { ... }` 的类体（到下一个 `public static class` 为止）。"""
    if 'public static class Sounds' not in text:
        raise SystemExit('Assets.java 里找不到 `public static class Sounds`')
    tail = text.split('public static class Sounds', 1)[1]
    return tail.split('public static class ', 1)[0]


def parse(block):
    consts = dict(re.findall(r'public static final String (\w+)\s*=\s*"([^"]+)";', block))
    if 'all = new String[]{' not in block:
        raise SystemExit('Sounds 里找不到 `all = new String[]{`')
    allblk = block.split('all = new String[]{', 1)[1].split('};', 1)[0]
    allblk = re.sub(r'//[^\n]*', '', allblk)          # 去掉行尾注释，免得注释里的词被判成成员
    members = list(dict.fromkeys(re.findall(r'\b([A-Z][A-Z0-9_]+)\b', allblk)))
    return consts, members


def main():
    filters = sys.argv[1:]
    text = open(ASSETS_JAVA, encoding='utf-8').read()
    consts, members = parse(sounds_block(text))

    problems = []
    for name, rel in sorted(consts.items()):
        if filters and not any(name.startswith(f) for f in filters):
            continue
        if not os.path.isfile(os.path.join(ASSETS_DIR, rel)):
            problems.append('文件缺失：%s -> %s' % (name, rel))
        if name not in members:
            problems.append('未登记 all[]：%s（%s）' % (name, rel))

    known = set(consts)
    for m in members:
        if m not in known:
            problems.append('all[] 里的名字在常量表里不存在：%s' % m)

    # 三向计数（带过滤时只报「本批」的定向结果，不做计数断言）
    dup = [m for m in members if members.count(m) > 1]

    print('Sounds 常量 = %d | all[] 成员(去重) = %d | 去重后总数 = %d' %
          (len(consts), len(set(members)), len(members)))
    print('资源根目录：%s' % ASSETS_DIR)
    if filters:
        hit = [n for n in consts if any(n.startswith(f) for f in filters)]
        print('本次关注 %d 个常量：%s' % (len(hit), ', '.join(sorted(hit))))
        # 过滤词一个都没命中 ⇒ 十有八九是前缀拼错了，否则这个脚本会「空跑还报 PASS」
        if not hit:
            problems.append('过滤前缀 %s 没命中任何常量（前缀拼错？）' % ', '.join(filters))
    if dup:
        problems.append('all[] 里有重复成员（不会报错但属于笔误）：%s' % ', '.join(sorted(set(dup))))

    if not filters and len(consts) != len(set(members)):
        problems.append('常量数 %d != all[] 成员数 %d ⇒ 有常量没登记，或有成员写错'
                        % (len(consts), len(set(members))))

    print()
    if problems:
        print('FAIL：%d 个问题' % len(problems))
        for p in problems:
            print('  - %s' % p)
        return 1
    print('ALL PASS：常量 ↔ 文件 ↔ all[] 三向一致，无重复无缺漏')
    return 0


if __name__ == '__main__':
    sys.exit(main())
