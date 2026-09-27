# -*- coding: utf-8 -*-
"""预演 ArmorAbility.namedShortDesc()：逐技能判断列表里会不会多出一行「技能名」。

规则（与 Java 端一致）：
    desc = short_desc
    若 lower(desc) 含 lower(name) ⇒ 原样返回（原生技能名字已写在句子里）
    否则 ⇒ "_" + titleCase(name) + "_\\n" + desc

用法：python _chk/check_named_short_desc.py
"""
import io, re, sys

BASE = "core/src/main/assets/messages/actors/"
FILES = {"zh": BASE + "actors_zh.properties", "en": BASE + "actors.properties"}

NO_CAPS = {"a", "an", "and", "of", "by", "to", "the", "x", "for"}


def title_case_en(s):
    """模拟 Messages.titleCase（英文：除若干虚词外每词首字母大写；中文：仅首字母大写=无变化）"""
    out = ""
    for word in re.split(r'(?<=\s)', s):
        if word.strip().lower().replace(":", "").replace("0", "").replace("1", "").replace("2", "").replace("3", "").replace("4", "").replace("5", "").replace("6", "").replace("7", "").replace("8", "").replace("9", "") in NO_CAPS:
            out += word
        else:
            out += word[:1].upper() + word[1:]
    return out[:1].upper() + out[1:]


def load(path):
    d = {}
    for line in io.open(path, encoding='utf-8'):
        line = line.rstrip('\r\n')
        if not line or line.startswith('#') or '=' not in line:
            continue
        k, v = line.split('=', 1)
        d[k] = v
    return d


def unescape(v):
    """properties 里的 \\n 还原成真换行（判断用）"""
    return v.replace('\\n', '\n')


rows = []
for lang, path in FILES.items():
    props = load(path)
    for k, v in props.items():
        if '$' in k or not k.startswith('actors.hero.abilities.') or not k.endswith('.name'):
            continue
        base = k[:-len('.name')]
        abil = base[len('actors.hero.abilities.'):]
        desc = props.get(base + '.short_desc')
        if desc is None:
            rows.append((lang, abil, v, None, 'NO short_desc'))
            continue
        dup = v.lower() in desc.lower()
        rows.append((lang, abil, v, desc, '已含名字（原样）' if dup else '将补一行技能名'))

for lang in ('zh', 'en'):
    print("=" * 78)
    print("[%s] 技能列表文本预演" % lang)
    print("=" * 78)
    for l, abil, name, desc, verdict in rows:
        if l != lang:
            continue
        flag = '  ' if verdict.startswith('已含') else '＋'
        print("%s %-34s %-22s %s" % (flag, abil, name, verdict))
        if not verdict.startswith('已含'):
            print("      列表里将显示：")
            shown = '_' + (title_case_en(name) if lang == 'en' else name) + '_\n' + unescape(desc)
            for line in shown.split('\n'):
                print("        | " + line)

# 断言：中指三技能必须补名字
need = ['middlefinger.gritteeth', 'middlefinger.neverforget', 'middlefinger.instantexecution']
for lang in ('zh', 'en'):
    got = {a for l, a, _n, _d, v in rows if l == lang and not v.startswith('已含')}
    miss = [a for a in need if a not in got]
    print("\n[%s] 中指三技能是否都会补上名字：%s" % (lang, "是" if not miss else "否，缺 %s" % miss))
    if miss:
        sys.exit(1)
print("\nALL PASS")
