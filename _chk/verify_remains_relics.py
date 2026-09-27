# -*- coding: utf-8 -*-
"""两位职业遗物「破损义眼 / 账簿残页」的接线核验（2026-09-18）。

跨 6 个文件一次断言：
  ① ItemSpriteSheet —— BROKEN_EYE = xy(11,3) / LEDGER_PAGE = xy(12,3) 两个常量，
     **声明必须落在容器段那个 `static{}` 之前**（Java 静态初始化按源码顺序执行，
     常量写在 static 块之后 ⇒ assignItemRect 读到 0 ⇒ 所有物品贴图整体错位）；
     并顺便扫全文件：不允许两个常量用同一个 xy 坐标（撞格 ⇒ 后画的覆盖先画的）。
  ② RemainsItem.get —— `case VALENCINA` → BrokenEye、`case MIDDLE_FINGER` → LedgerPage，
     且这两支里**不得再出现 SealShard**（占位残留正是本次要修的东西）。
  ③ BrokenEye —— extends RemainsItem / image = BROKEN_EYE / doEffect 调 `StoneOfClairvoyance.reveal(hero.pos)`。
  ④ LedgerPage —— extends RemainsItem / image = LEDGER_PAGE / BONUS=1、TURNS=100 /
     doEffect 调 `TempStrength.apply(hero, BONUS, TURNS)`。
  ⑤ StoneOfClairvoyance.reveal —— 公开静态入口，内部**复用** activate（不另写一份地图揭露逻辑）；
     Runestone.activate 仍是 protected abstract（没被顺手改成 public）。
  ⑥ Catalog.MISC_CONSUMABLES —— 三个遗物类都注册（未注册时 countUse 静默丢弃，图鉴里不出现）。

运行: python _chk/verify_remains_relics.py
"""

import os
import re
import sys

ROOT = "D:/PD"
SRC = os.path.join(ROOT, "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon")

ITEM_SPRITE_SHEET = os.path.join(SRC, "sprites/ItemSpriteSheet.java")
REMAINS_ITEM = os.path.join(SRC, "items/remains/RemainsItem.java")
BROKEN_EYE = os.path.join(SRC, "items/remains/BrokenEye.java")
LEDGER_PAGE = os.path.join(SRC, "items/remains/LedgerPage.java")
CLAIRVOYANCE = os.path.join(SRC, "items/stones/StoneOfClairvoyance.java")
RUNESTONE = os.path.join(SRC, "items/stones/Runestone.java")
CATALOG = os.path.join(SRC, "journal/Catalog.java")

fails = []
checks = 0


def ok(cond, msg):
    global checks
    checks += 1
    if not cond:
        fails.append(msg)
    return cond


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def strip_comments(text):
    """先 /* */ 再去 //（顺序不能反：文件头 GPL 注释里有 http://）。"""
    text = re.sub(r"/\*.*?\*/", " ", text, flags=re.S)
    text = re.sub(r"//[^\n]*", " ", text)
    return text


def body_of(text, signature):
    """按大括号配平取方法体（含签名行）。取不到返回 None。"""
    i = text.find(signature)
    if i < 0:
        return None
    j = text.find("{", i)
    if j < 0:
        return None
    depth = 0
    k = j
    while k < len(text):
        c = text[k]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
            if depth == 0:
                return text[i:k + 1]
        k += 1
    return None


def code_of(text, signature):
    """方法体 + 剥注释 —— 本项目注释习惯引用被修表达式原文，只 body_of 不剥注释会假绿。"""
    b = body_of(text, signature)
    return None if b is None else strip_comments(b)


# ---------------------------------------------------------------- ① 贴图常量
iss = read(ITEM_SPRITE_SHEET)
iss_code = strip_comments(iss)

ok(re.search(r"public\s+static\s+final\s+int\s+BROKEN_EYE\s*=\s*xy\(11\s*,\s*3\s*\)\s*;", iss_code),
   "ItemSpriteSheet 缺少 BROKEN_EYE = xy(11, 3)")
ok(re.search(r"public\s+static\s+final\s+int\s+LEDGER_PAGE\s*=\s*xy\(12\s*,\s*3\s*\)\s*;", iss_code),
   "ItemSpriteSheet 缺少 LEDGER_PAGE = xy(12, 3)")
ok(re.search(r"assignItemRect\(\s*BROKEN_EYE\s*,\s*12\s*,\s*14\s*\)\s*;", iss_code),
   "ItemSpriteSheet 缺少 assignItemRect(BROKEN_EYE, 12, 14)")
ok(re.search(r"assignItemRect\(\s*LEDGER_PAGE\s*,\s*11\s*,\s*13\s*\)\s*;", iss_code),
   "ItemSpriteSheet 缺少 assignItemRect(LEDGER_PAGE, 11, 13)")

# 常量必须声明在静态块之前（否则 xy() 尚未求值，贴图整体错位）
pos_eye = iss_code.find("public static final int BROKEN_EYE")
pos_page = iss_code.find("public static final int LEDGER_PAGE")
pos_static = iss_code.find("static{", pos_eye if pos_eye >= 0 else 0)
ok(pos_eye > 0 and pos_static > 0 and pos_eye < pos_static,
   "BROKEN_EYE 必须声明在 assignItemRect 所在的 static{} 之前（静态初始化顺序）")
ok(pos_page > 0 and pos_static > 0 and pos_page < pos_static,
   "LEDGER_PAGE 必须声明在 assignItemRect 所在的 static{} 之前（静态初始化顺序）")

# 撞格扫描：同一个 xy(col,row) 不允许被两个常量占用
coords = {}
dup = []
for m in re.finditer(r"public\s+static\s+final\s+int\s+(\w+)\s*=\s*xy\(\s*(\d+)\s*,\s*(\d+)\s*\)\s*;", iss_code):
    name, x, y = m.group(1), int(m.group(2)), int(m.group(3))
    key = (x, y)
    if key in coords:
        dup.append("%s 与 %s 都用 xy(%d,%d)" % (coords[key], name, x, y))
    else:
        coords[key] = name
ok(not dup, "ItemSpriteSheet 存在撞格: " + "; ".join(dup))

# ---------------------------------------------------------------- ② RemainsItem.get
ri = read(REMAINS_ITEM)
get_body = code_of(ri, "public static RemainsItem get(HeroClass cls)")
ok(get_body is not None, "RemainsItem 里取不到 get(HeroClass) 方法体")
if get_body:
    ok(re.search(r"case\s+VALENCINA\s*:\s*return\s+new\s+BrokenEye\s*\(\s*\)\s*;", get_body),
       "RemainsItem.get 的 case VALENCINA 未指向 new BrokenEye()")
    ok(re.search(r"case\s+MIDDLE_FINGER\s*:\s*return\s+new\s+LedgerPage\s*\(\s*\)\s*;", get_body),
       "RemainsItem.get 的 case MIDDLE_FINGER 未指向 new LedgerPage()")
    # 两个改过的 case 之后到下一个 case 之间不得再出现 SealShard 占位
    for name, nxt in (("VALENCINA", "MIDDLE_FINGER"), ("MIDDLE_FINGER", None)):
        i = get_body.find("case " + name)
        j = get_body.find("case " + nxt) if nxt else len(get_body)
        seg = get_body[i:j]
        ok("SealShard" not in seg, "case %s 仍残留 SealShard 占位" % name)

# ---------------------------------------------------------------- ③ BrokenEye
eye = read(BROKEN_EYE)
eye_code = strip_comments(eye)
ok(re.search(r"class\s+BrokenEye\s+extends\s+RemainsItem", eye_code), "BrokenEye 未 extends RemainsItem")
ok(re.search(r"image\s*=\s*ItemSpriteSheet\.BROKEN_EYE\s*;", eye_code), "BrokenEye 未使用 BROKEN_EYE 贴图")
eye_fx = code_of(eye, "protected void doEffect(Hero hero)")
ok(eye_fx is not None, "BrokenEye 取不到 doEffect 方法体")
if eye_fx:
    ok(re.search(r"StoneOfClairvoyance\.reveal\(\s*hero\.pos\s*\)", eye_fx),
       "BrokenEye.doEffect 未调用 StoneOfClairvoyance.reveal(hero.pos)")
    ok("activate(" not in eye_fx, "BrokenEye.doEffect 直接调了 activate（应走 reveal 入口，别绕开）")

# ---------------------------------------------------------------- ④ LedgerPage
page = read(LEDGER_PAGE)
page_code = strip_comments(page)
ok(re.search(r"class\s+LedgerPage\s+extends\s+RemainsItem", page_code), "LedgerPage 未 extends RemainsItem")
ok(re.search(r"image\s*=\s*ItemSpriteSheet\.LEDGER_PAGE\s*;", page_code), "LedgerPage 未使用 LEDGER_PAGE 贴图")
ok(re.search(r"public\s+static\s+final\s+int\s+BONUS\s*=\s*1\s*;", page_code), "LedgerPage BONUS 不是 1")
ok(re.search(r"public\s+static\s+final\s+int\s+TURNS\s*=\s*100\s*;", page_code), "LedgerPage TURNS 不是 100")
page_fx = code_of(page, "protected void doEffect(Hero hero)")
ok(page_fx is not None, "LedgerPage 取不到 doEffect 方法体")
if page_fx:
    ok(re.search(r"TempStrength\.apply\(\s*hero\s*,\s*BONUS\s*,\s*TURNS\s*\)", page_fx),
       "LedgerPage.doEffect 未调用 TempStrength.apply(hero, BONUS, TURNS)")

# ---------------------------------------------------------------- ⑤ 明示符石入口
cl = read(CLAIRVOYANCE)
rev = code_of(cl, "public static void reveal")
ok(rev is not None, "StoneOfClairvoyance 缺少 public static void reveal(...)")
if rev:
    ok(re.search(r"new\s+StoneOfClairvoyance\s*\(\s*\)\s*\.\s*activate\s*\(", rev),
       "reveal 未复用 activate（别另写第二份地图揭露逻辑）")
rs = strip_comments(read(RUNESTONE))
ok("protected abstract void activate(int cell);" in rs,
   "Runestone.activate 签名被改动（应仍为 protected abstract）")

# ---------------------------------------------------------------- ⑥ 图鉴注册
cat = code_of(read(CATALOG), "MISC_CONSUMABLES.addItems")
ok(cat is not None, "Catalog 里找不到 MISC_CONSUMABLES.addItems 调用")
if cat:
    for cls in ("BrokenEye.class", "LedgerPage.class", "BrokenBoneBlade.class"):
        ok(cls in cat, "Catalog.MISC_CONSUMABLES 未注册 %s" % cls)

# ---------------------------------------------------------------- 反例自测
def selftest():
    """把「旧写法」喂给同一批判据，确认会判 FAIL（防恒真空测）。"""
    bad_cases = []

    # 反例 1：常量落在 static{} 之后
    t = "static{\n}\npublic static final int BROKEN_EYE = xy(11, 3);"
    pos_st = t.find("static{")
    pos_b = t.find("public static final int BROKEN_EYE")
    bad_cases.append(("常量在 static{} 之后应判 FAIL", not (pos_b < pos_st)))

    # 反例 2：占位残留
    seg = "case VALENCINA: return new SealShard(); //占位复用战士遗物"
    bad_cases.append(("SealShard 占位应判 FAIL", not ("SealShard" not in seg)))

    # 反例 3：绕过 reveal 直接调 activate
    fx = "protected void doEffect(Hero hero){ new StoneOfClairvoyance().activate(hero.pos); }"
    bad_cases.append(("绕开 reveal 应判 FAIL",
                      not re.search(r"StoneOfClairvoyance\.reveal\(\s*hero\.pos\s*\)", fx)))

    # 反例 4：数值写错
    fx2 = "protected void doEffect(Hero hero){ TempStrength.apply( hero, 2, 100 ); }"
    bad_cases.append(("参数写死数字应判 FAIL",
                      not re.search(r"TempStrength\.apply\(\s*hero\s*,\s*BONUS\s*,\s*TURNS\s*\)", fx2)))

    # 反例 5：剥注释的重要性（注释里引用正确写法仍是错的）
    fx3 = "protected void doEffect(Hero hero){\n// 正确写法: TempStrength.apply( hero, BONUS, TURNS )\nTempStrength.apply( hero, 1, 50 );\n}"
    bad_cases.append(("仅注释里正确应判 FAIL（须先剥注释）",
                      not re.search(r"TempStrength\.apply\(\s*hero\s*,\s*BONUS\s*,\s*TURNS\s*\)",
                                    strip_comments(fx3))))

    # 反例 6：坐标撞格
    dup_test = [("A", (11, 3)), ("B", (11, 3))]
    seen, hit = {}, False
    for n, k in dup_test:
        if k in seen:
            hit = True
        seen[k] = n
    bad_cases.append(("撞格应判 FAIL", hit))

    bad = [m for m, judged_fail in bad_cases if not judged_fail]
    print("\n--- 反例自测（每条都必须判 FAIL）---")
    for m, judged_fail in bad_cases:
        print("  %s  %s" % ("OK  " if judged_fail else "BAD ", m))
    return bad


print("=== 遗物接线核验 ===")
print("断言数 = %d，失败 = %d" % (checks, len(fails)))
for f in fails:
    print("  FAIL " + f)

bad = selftest()
for b in bad:
    fails.append("反例自测失效: " + b)

print("\n" + ("ALL PASS" if not fails else "共 %d 处问题" % len(fails)))
sys.exit(0 if not fails else 1)
