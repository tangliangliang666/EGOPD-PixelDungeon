# -*- coding: utf-8 -*-
"""修复 gritteeth 系列文本在落盘时被「真换行」污染的问题（2026-09-20）。

背景：`.properties` 里的多段文本必须写成**字面量 `\\n`**（反斜杠 + n），
不能写真换行——真换行会让 `entry` 断在中间，渲染时只剩第一段。
用 heredoc 传 Python 源码时，shell/JSON 层会把 `\\\\n` 还原成真换行，
所以这类改写一律走本脚本（文件落盘，不经 shell 转义）。

本脚本：把 `key=` 之后的**续行**合并回去，续行之间补回字面量 `\\n`。
幂等——已经是单行的条目不会被改动。

运行：python _chk/fix_grit_text_escape.py
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MSG = os.path.join(ROOT, "core", "src", "main", "assets", "messages", "actors")

KEYS = [
    "actors.hero.abilities.middlefinger.gritteeth.short_desc",
    "actors.hero.abilities.middlefinger.gritteeth.desc",
    "actors.buffs.gritteethbuff.desc",
]

# 一个新的属性键（或注释）→ 视为上一条目结束
NEW_ENTRY = re.compile(r"^(?:#|[A-Za-z_][A-Za-z0-9_.\[\]-]*=)")

EXPECT = {
    "actors_zh.properties": {
        "actors.hero.abilities.middlefinger.gritteeth.short_desc":
            "血量归零后 _5回合_ 内_不会死亡_；_解离射线_，以及_血祭_、_割腕_这类_自伤换成长_的行为，都可以击穿这一判定。",
        "actors.hero.abilities.middlefinger.gritteeth.desc":
            "中指长兄咬紧牙关：_5回合_内血量归零也_不会死亡_。\\n\\n"
            "_解离射线_（邪能魔眼的射线、最终boss 的射线等）可以击穿这一免疫死亡的判定；"
            "_血祭_、_割腕_这类以自伤为代价换取成长的行为同样可以击穿它。",
        "actors.buffs.gritteethbuff.desc":
            "血量归零也_不会死亡_；_解离射线_，以及_血祭_、_割腕_这类_自伤换成长_的行为，都可以击穿这一判定。\\n\\n"
            "剩余回合：%s",
    },
    "actors.properties": {
        "actors.hero.abilities.middlefinger.gritteeth.short_desc":
            "For _5 turns_ you _cannot die_ at _0 HP_; _disintegration rays_ pierce this, as do acts of "
            "_self-harm for growth_ such as _blood rites_ and _wrist-slitting_.",
        "actors.hero.abilities.middlefinger.gritteeth.desc":
            "The Middle Finger clenches his teeth: for _5 turns_ you _cannot die_, even at _0 HP_.\\n\\n"
            "_Disintegration rays_ (the Evil Eye's ray, the final boss's rays, and the like) pierce this "
            "death immunity; so do acts of _self-harm for growth_, such as _blood rites_ and _wrist-slitting_.",
        "actors.buffs.gritteethbuff.desc":
            "You _cannot die_, even at _0 HP_; _disintegration rays_ pierce this, as do acts of "
            "_self-harm for growth_ such as _blood rites_ and _wrist-slitting_.\\n\\n"
            "Turns remaining: %s.",
    },
}

fails = []


def fail(msg):
    fails.append(msg)
    print("FAIL  " + msg)


for fname, table in EXPECT.items():
    path = os.path.join(MSG, fname)
    raw = io.open(path, encoding="utf-8", newline="").read()
    assert "\r\n" not in raw, fname + " 应为 LF"
    lines = raw.split("\n")
    changed = 0

    for key, want in table.items():
        idx = [i for i, l in enumerate(lines) if l.startswith(key + "=")]
        if len(idx) != 1:
            fail("%s: 键 %s 命中 %d 次" % (fname, key, len(idx)))
            continue
        i = idx[0]
        # 吃掉续行（真换行污染的产物）：扫到下一条目为止，再把尾部的空行还原回去
        e = i + 1
        while e < len(lines) and not NEW_ENTRY.match(lines[e]):
            e += 1
        j = e
        while j > i + 1 and not lines[j - 1].strip():
            j -= 1
        merged = "\\n".join(lines[i:j])
        if merged != key + "=" + want:
            lines[i:j] = [key + "=" + want]
            changed += 1
            print("修复 %-22s %s（合并 %d 行）" % (fname, key, j - i))
        else:
            print("OK   %-22s %s" % (fname, key))

    if changed:
        io.open(path, "w", encoding="utf-8", newline="").write("\n".join(lines))

    # 复核
    raw = io.open(path, encoding="utf-8", newline="").read()
    for key, want in table.items():
        hit = [l for l in raw.split("\n") if l.startswith(key + "=")]
        if len(hit) != 1 or hit[0] != key + "=" + want:
            fail("%s: %s 落盘后仍不符" % (fname, key))
    bad = [l for l in raw.split("\n")
           if l.strip() and not l.lstrip().startswith("#") and "=" not in l]
    if bad:
        fail("%s: 仍存在缺 = 的断行 %r" % (fname, bad[:3]))

print("\n结果：" + ("ALL PASS" if not fails else "FAIL %d 条" % len(fails)))
sys.exit(1 if fails else 0)
