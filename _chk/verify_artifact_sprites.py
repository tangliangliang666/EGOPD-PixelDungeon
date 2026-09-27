# -*- coding: utf-8 -*-
"""原版神器强化形态（5 件 × 13 帧）贴图核验（2026-09-20）。

用纯 Python 解码 items.png（无 PIL，解码器同 verify_narcissus_sprites.py），
对 42 行 1~10 列的 13 帧做四件事：

  ① **不透明像素数**（空图 ⇒ FAIL）—— 用户说「画好了」但实际没落盘的经典事故；
  ② **溢出到声明矩形之外**的像素数（12×15 / 13×16 / 15×16 落在 16×16 格里）；
  ③ 同组多帧（圣杯 3 帧 / 炖菜 4 帧）的 RGBA 指纹**两两不同**，防「复制后忘了改」；
  ④ **顺序陷阱硬断言**：常量声明行必须**早于**它那条 assignItemRect 所在的 `static{}` 块 ——
     静态初始化按源码顺序执行，常量写在块后 ⇒ assignItemRect 收到 0 ⇒
     **所有物品图标整体错位**，且不报错不告警。另扫全表 xy 撞格。

用法: python _chk/verify_artifact_sprites.py
行列均 1 基（与 ItemSpriteSheet.xy 一致）。
"""
import re
import struct
import sys
import zlib

PNG = "core/src/main/assets/sprites/items.png"
ITEMSPRITE = ("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon"
              "/sprites/ItemSpriteSheet.java")

# (常量名, 列, 行, w, h) —— 必须与 ItemSpriteSheet 的 assignItemRect 完全一致
RECTS = [
    ("ARTIFACT_BLOOD_FEAST_CHALICE1",   1, 42, 12, 15),
    ("ARTIFACT_BLOOD_FEAST_CHALICE2",   2, 42, 12, 15),
    ("ARTIFACT_BLOOD_FEAST_CHALICE3",   3, 42, 12, 15),
    ("ARTIFACT_LIFELONG_STEW1",         4, 42, 15, 16),
    ("ARTIFACT_LIFELONG_STEW2",         5, 42, 15, 16),
    ("ARTIFACT_LIFELONG_STEW3",         6, 42, 15, 16),
    ("ARTIFACT_LIFELONG_STEW4",         7, 42, 15, 16),
    ("ARTIFACT_CHAIN_OF_OTHERS",        8, 42, 15, 16),
    ("ARTIFACT_CHAPTER_NINE_VERSE_TWO", 9, 42, 13, 16),
    ("ARTIFACT_APPROACHING_DAY",       10, 42, 16, 16),
]

# 同组多帧必须两两不同的分组（帧是给不同等级/充能档用的，内容一样＝画了没用）
DISTINCT_GROUPS = [
    ("圣杯 3 帧（按等级）", ["ARTIFACT_BLOOD_FEAST_CHALICE1",
                            "ARTIFACT_BLOOD_FEAST_CHALICE2",
                            "ARTIFACT_BLOOD_FEAST_CHALICE3"]),
    ("炖菜 4 帧（按充能）", ["ARTIFACT_LIFELONG_STEW1", "ARTIFACT_LIFELONG_STEW2",
                            "ARTIFACT_LIFELONG_STEW3", "ARTIFACT_LIFELONG_STEW4"]),
]


def load_png(path):
    d = open(path, 'rb').read()
    assert d[:8] == b'\x89PNG\r\n\x1a\n', 'not png'
    pos, idat, w, h, bitd, ctype, interlace = 8, b'', 0, 0, 8, 6, 0
    while pos < len(d):
        ln = struct.unpack('>I', d[pos:pos + 4])[0]
        typ = d[pos + 4:pos + 8]
        data = d[pos + 8:pos + 8 + ln]
        if typ == b'IHDR':
            w, h, bitd, ctype, _, _, interlace = struct.unpack('>IIBBBBB', data)
        elif typ == b'IDAT':
            idat += data
        elif typ == b'IEND':
            break
        pos += 12 + ln
    assert bitd == 8 and interlace == 0, (bitd, interlace)
    nch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    raw = zlib.decompress(idat)
    stride = w * nch
    out = bytearray(h * stride)
    prev = bytearray(stride)
    p = 0
    for y in range(h):
        f = raw[p]; p += 1
        line = bytearray(raw[p:p + stride]); p += stride
        if f == 1:
            for i in range(nch, stride):
                line[i] = (line[i] + line[i - nch]) & 255
        elif f == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 255
        elif f == 3:
            for i in range(stride):
                a = line[i - nch] if i >= nch else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
        elif f == 4:
            for i in range(stride):
                a = line[i - nch] if i >= nch else 0
                b = prev[i]
                c = prev[i - nch] if i >= nch else 0
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        out[y * stride:(y + 1) * stride] = line
        prev = line
    return out, w, h, nch


def pixel(buf, W, nch, x, y):
    o = (y * W + x) * nch
    if nch == 4:
        return buf[o], buf[o + 1], buf[o + 2], buf[o + 3]
    if nch == 3:
        return buf[o], buf[o + 1], buf[o + 2], 255
    return buf[o], buf[o], buf[o], buf[o]


def rect_signature(buf, W, nch, col, row, w, h):
    x0, y0 = (col - 1) * 16, (row - 1) * 16
    return tuple(pixel(buf, W, nch, x, y)
                 for y in range(y0, y0 + h) for x in range(x0, x0 + w))


def static_block_of(src, name):
    """返回包含 assignItemRect(name, ...) 的那个 `static {` 的行号（常量必须早于它）。"""
    m = re.search(r"assignItemRect\(\s*" + re.escape(name) + r"\s*,", src)
    if not m:
        return None
    before = src[:m.start()]
    blocks = [b.start() for b in re.finditer(r"static\s*\{", before)]
    if not blocks:
        return None
    return before[:blocks[-1]].count("\n") + 1


def check_ordering(src):
    """恒定顺序陷阱：常量声明必须在 assignItemRect 所在 static{} 之前。"""
    problems = []
    for name, col, row, w, h in RECTS:
        dm = re.search(r"int\s+" + re.escape(name) + r"\s*=\s*xy\(", src)
        if not dm:
            problems.append("缺少常量声明 %s = xy(%d, %d)" % (name, col, row))
            continue
        decl_line = src[:dm.start()].count("\n") + 1
        blk = static_block_of(src, name)
        if blk is None:
            problems.append("缺少 assignItemRect(%s, %d, %d)" % (name, w, h))
            continue
        if decl_line > blk:
            problems.append(
                "顺序陷阱：%s 声明在第 %d 行，却晚于使用它的 static{} 第 %d 行 "
                "⇒ 静态初始化时该常量为 0，**所有物品图标会整体错位**"
                % (name, decl_line, blk))
    return problems


def check_rects_match(src):
    """assignItemRect 的 (w,h) 必须与脚本声明一致（两侧都不许单改）。"""
    problems = []
    for name, col, row, w, h in RECTS:
        m = re.search(r"assignItemRect\(\s*" + re.escape(name) + r"\s*,\s*(\d+)\s*,\s*(\d+)\s*\)", src)
        if not m:
            problems.append("缺少 assignItemRect(%s, ...)" % name)
            continue
        gw, gh = int(m.group(1)), int(m.group(2))
        if (gw, gh) != (w, h):
            problems.append("%s 的 assignItemRect=%dx%d，脚本声明 %dx%d（两边不一致）"
                            % (name, gw, gh, w, h))
    return problems


def check_collisions(src, only_rows=(42,)):
    """扫 xy 撞格。

    **只对本批新增的行（42 行）判 FAIL**：全表扫描会撞上大量**假阳性** ——
    `ItemSpriteSheet` 里嵌了若干个内部类，它们各自 `xy(1,1)`… 给自己的另一张图集编址，
    与顶层的同名坐标**不是同一个空间**（例：顶层 `RINGS = xy(1,15)` vs 内部类 `RINGS = xy(1,1)`）。
    本项目既有脚本（`verify_narcissus_sprites.py`）也是只判新增行，这里沿用同一口径。
    其余行的重复只做信息性提示，不参与判定。
    """
    decl = {}
    for m in re.finditer(r"int\s+([A-Z][A-Z0-9_]*)\s*=\s*xy\(\s*(\d+)\s*,\s*(\d+)\s*\)", src):
        decl.setdefault((int(m.group(2)), int(m.group(3))), []).append(m.group(1))

    fails, notes = [], []
    for (c, r), n in sorted(decl.items()):
        if len(n) <= 1:
            continue
        if r in only_rows:
            fails.append("xy 撞格 (%d,%d): %s" % (c, r, "/".join(n)))
        else:
            notes.append("(信息) 其它行重复 (%d,%d): %s（多为内部类自有坐标系，非真撞格）"
                         % (c, r, "/".join(n)))
    return fails, notes


def selftest():
    """反例自测：证明「顺序陷阱」与「(w,h) 不一致」的判据**不是恒真**。

    构造一段最小源码，分别喂「正确顺序」与「常量写在 static{} 之后」，
    判据必须给出「无问题」与「有问题」两种结果。
    """
    good = (
        "class T {\n"
        "  static final int A = xy(1, 42);\n"        # 第 2 行：常量在前
        "  static {\n"
        "    assignItemRect(A, 12, 15);\n"           # 第 4 行：块在后
        "  }\n"
        "}\n"
    )
    bad = (
        "class T {\n"
        "  static {\n"
        "    assignItemRect(A, 12, 15);\n"           # 块在前
        "  }\n"
        "  static final int A = xy(1, 42);\n"        # 常量在后 ⇒ 静态初始化拿到 0
        "}\n"
    )
    rect_ok = "class T { static void f(){ assignItemRect(ARTIFACT_APPROACHING_DAY, 16, 16); } }"
    rect_bad = "class T { static void f(){ assignItemRect(ARTIFACT_APPROACHING_DAY, 15, 16); } }"

    problems = []
    # 只拿单个名字去验顺序判据，所以临时把 RECTS 缩到一项
    global RECTS
    saved = RECTS
    try:
        RECTS = [("A", 1, 42, 12, 15)]
        g = check_ordering(good)
        b = check_ordering(bad)
        RECTS = [("ARTIFACT_APPROACHING_DAY", 10, 42, 16, 16)]
        r_ok = check_rects_match(rect_ok)
        r_bad = check_rects_match(rect_bad)
    finally:
        RECTS = saved

    if g:
        problems.append("正确顺序被判成有问题：%s" % g)
    if not b:
        problems.append("常量写在 static{} 之后却没被判出来（判据恒真/空转）")
    if r_ok:
        problems.append("(w,h) 一致的样例被判成不一致：%s" % r_ok)
    if not r_bad:
        problems.append("(w,h) 不一致的样例没被判出来")

    for p in problems:
        print("  自测失败：" + p)
    if not problems:
        print("  自测通过：正确顺序 PASS / 常量写在后 FAIL / (w,h) 不一致能报出")
    return 0 if not problems else 1


def main():
    if "--selftest" in sys.argv:
        return selftest()

    buf, W, H, nch = load_png(PNG)
    print("items.png = %dx%d, channels=%d" % (W, H, nch))
    ok = True

    sigs = {}
    for name, col, row, w, h in RECTS:
        x0, y0 = (col - 1) * 16, (row - 1) * 16
        opaque = overflow = 0
        for y in range(y0, y0 + 16):
            for x in range(x0, x0 + 16):
                _, _, _, a = pixel(buf, W, nch, x, y)
                if a > 0:
                    if (x - x0) < w and (y - y0) < h:
                        opaque += 1
                    else:
                        overflow += 1
        sigs[name] = rect_signature(buf, W, nch, col, row, w, h)
        flag = "OK   "
        if opaque == 0:
            flag, ok = "EMPTY", False
        if overflow:
            flag, ok = "SPILL", False
        print("  [%s] %-34s (%2d,%2d) %2dx%-2d 不透明=%3d 溢出=%d"
              % (flag, name, col, row, w, h, opaque, overflow))

    print("\n=== 同组多帧是否互不相同 ===")
    for label, names in DISTINCT_GROUPS:
        dup = [(a, b) for i, a in enumerate(names) for b in names[i + 1:]
               if sigs[a] == sigs[b]]
        if dup:
            ok = False
            for a, b in dup:
                print("  FAIL  %s：%s 与 %s 像素完全相同（复制后忘了改？）" % (label, a, b))
        else:
            print("  OK    %s：两两不同" % label)

    src = open(ITEMSPRITE, encoding='utf-8').read()

    print("\n=== ItemSpriteSheet 接线 ===")
    order_problems = check_ordering(src) + check_rects_match(src)
    for p in order_problems:
        ok = False
        print("  FAIL  " + p)
    if not order_problems:
        print("  OK    10 个常量齐备、声明均在对应 static{} 之前、(w,h) 一致")

    coll, notes = check_collisions(src)
    for p in coll:
        ok = False
        print("  FAIL  " + p)
    if not coll:
        print("  OK    42 行内无 xy 撞格")
    for n in notes:
        print("  " + n)

    print("\n" + ("ALL PASS" if ok else "FAIL"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
