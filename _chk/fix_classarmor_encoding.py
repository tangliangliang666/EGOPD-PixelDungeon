# -*- coding: utf-8 -*-
"""修复 ClassArmor.java：Edit 工具写坏的三处（两处注释末字被写成 E8 A1 3F，一处新注释被写成 GBK）。

策略：整文件按「UTF-8 + surrogateescape」读取（无损保留非法字节），只替换出问题的那几行，
再按 UTF-8 写回。逐行断言，改完必须再跑编码核验。
"""
import io, re

P = "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/items/armor/ClassArmor.java"
raw = io.open(P, 'rb').read()
print("原文件 UTF-8 可解吗:", end=" ")
try:
    raw.decode('utf-8'); print("可以（无需修复）")
except Exception as e:
    print("否 ⇒", e)

text = raw.decode('utf-8', 'surrogateescape')

FIXES = [
    # (定位用的唯一前缀, 期望的完整正确行)
    ('classArmor = new ThumbCoat(); //',
     '\t\t\t\tclassArmor = new ThumbCoat(); //拇指 前二老板职业护甲：拇指大衣'),
    ('classArmor = new MiddleFingerCoat(); //',
     '\t\t\t\tclassArmor = new MiddleFingerCoat(); //中指 长兄职业护甲：中指外套'),
    ('desc += "\\n\\n" + ability.namedShortDesc();',
     '\t\t\t\t//带上技能名，否则光看描述认不出装的是哪一门技能（见 ArmorAbility.namedShortDesc()）\r\n'
     '\t\t\t\tdesc += "\\n\\n" + ability.namedShortDesc();'),
]

lines = text.split('\r\n')
for prefix, want in FIXES:
    hits = [i for i, L in enumerate(lines) if prefix in L]
    assert len(hits) == 1, "定位不唯一：%r ⇒ %r" % (prefix, hits)
    i = hits[0]
    if prefix.startswith('desc +='):
        # 上一行是坏掉的注释行：直接改注释
        j = i - 1
        assert 'ArmorAbility.namedShortDesc()' in lines[j], "第 %d 行不是预期注释：%r" % (j + 1, lines[j])
        lines[j] = want.split('\r\n')[0]
        print("修复注释行 %d ⇒ %s" % (j + 1, lines[j]))
    else:
        print("修复第 %d 行：\n  旧 = %r\n  新 = %r" % (i + 1, lines[i], want))
        lines[i] = want

out = '\r\n'.join(lines)
data = out.encode('utf-8', 'surrogateescape')

# 写回前最后一道闸：必须完全可解，且不再有代理字符
try:
    data.decode('utf-8')
except Exception as e:
    raise SystemExit("仍然非法：%s" % e)
assert not re.search(r'[\udc80-\udcff]', out), "还有残留非法字节"

io.open(P, 'wb').write(data)
print("已写回，%d 字节" % len(data))

# 断言关键内容
chk = io.open(P, encoding='utf-8').read()
for s in ('拇指大衣', '中指外套', 'namedShortDesc()', 'ArmorAbility.namedShortDesc()'):
    assert s in chk, "缺少 %s" % s
print("关键内容断言通过")
