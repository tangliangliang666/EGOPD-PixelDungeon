# 核验考验「NETZACH（胜利）」的**浮层淡出**改动：怪头顶的血条与状态标记也要一起变淡/消失。
#
# 为什么需要它：NETZACH 的淡化原来是**只给精灵本体**做的（CharSprite.draw 里本帧乘 alpha），
# 而挂在怪身上/旁边的浮层都是**独立挂在场景上**的节点，不继承精灵的 alpha：
#   · 血条  —— ui/CharHealthIndicator（常驻）与 ui/TargetHealthIndicator（瞄准中）
#   · 状态标记 —— effects/EmoIcon（睡眠 Zzz / 警觉 ! / 搜索 ? / 迷失）
# 于是「>3 格看不见的怪」仍会被一条浮空血条 + 一个 Zzz 图标**精确定位**。
# 修法有两处细节，都是静默失败类型，所以逐条钉死：
#   ① 半透明要真的生效：HealthBar 是 Group、本身没有 alpha，必须把系数写进三个 ColorBlock；
#   ② 全透明时要连 visible 一起关：只把 alpha 压到 0 仍会留一条不可见的浮空元素；
#   ③ EmoIcon 必须在 **draw() 里乘、画完还原**（与 CharSprite 同构），不许写持久 alpha ——
#      否则会与「真正的隐形 AlphaTweener」打架。
#
# 反向判据（刻意不做的事）：
#   · Trials.java 不反向依赖 UI 类（只出现在注释里）；HealthBar 除了 setAlpha 之外不许碰 alpha；
#   · 4 个 EmoIcon 子类不各自覆写 draw（避免「改了基类漏了子类」）；
#   · `TargetHealthIndicator.visible` **不许**被这条视觉改动碰上 —— `ChaoticCenser`（混沌香炉）
#     拿 `instance.isVisible()` 当「英雄当前是否锁定了目标」在读，掐掉它会让香炉静默失效。
#
# 用法：
#   bash _chk/_build_netzach_overlay.sh        # 先把这 5 个文件编进 _chk/_javachk2
#   bash _chk/_build_hod_netzach.sh run        # 跑 NETZACH 行为探针 → _chk/_hodnetzach.out
#   python _chk/verify_netzach_overlay.py
#   python _chk/verify_netzach_overlay.py --selftest

import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SRC = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon')
CLASSES = os.path.join(HERE, '_javachk2')
JAVAP = 'D:/PD/tools/jdk-21.0.12.1+1/bin/javap'
PROBE_OUT = os.path.join(HERE, '_hodnetzach.out')
MSG = os.path.join(ROOT, 'core/src/main/assets/messages/misc')

# javap 的 classpath 要**顺延到构建产物**：_javachk2 放最前（本轮新编的类优先），
# 后面几个是父类/依赖类所在处（HealthBar 的父类 Group、CharSprite 的父类 MovieClip 等）。
JAVAP_CP = os.pathsep.join([
    CLASSES,
    'D:/PD/core/build/classes/java/main',
    'D:/PD/SPD-classes/build/classes/java/main',
    'D:/PD/services/build/classes/java/main',
])

PKG = 'com.shatteredpixel.shatteredpixeldungeon'
TRIALS = PKG + '.Trials'
HEALTHBAR = PKG + '.ui.HealthBar'
CHARHI = PKG + '.ui.CharHealthIndicator'
TARGETHI = PKG + '.ui.TargetHealthIndicator'
EMOICON = PKG + '.effects.EmoIcon'
SPRITE = PKG + '.sprites.CharSprite'

# `Trials.enemyFade(` 的**全部代码引用处**（剥掉注释后数）。少一处 = 有浮层没接上。
FADE_CALLERS = sorted([
    'effects/EmoIcon.java',
    'sprites/CharSprite.java',
    'ui/CharHealthIndicator.java',
    'ui/TargetHealthIndicator.java',
])
# `setAlpha(` 的**全部代码引用处**：定义处 + 两条血条。
# ⚠️ Trials.java 的 NETZACH 注释里写到了 `HealthBar.setAlpha(fade)` —— 这里必须被剥掉，
#    否则「引用清单」会被一句注释污染（selftest 里专门钉了这一对）。
SETALPHA_CALLERS = sorted([
    'ui/CharHealthIndicator.java',
    'ui/HealthBar.java',
    'ui/TargetHealthIndicator.java',
])
EMO_SUBCLASSES = ['Sleep', 'Alert', 'Investigate', 'Lost']

ok = True


def chk(cond, msg):
    global ok
    print(('  [OK]   ' if cond else '  [FAIL] ') + msg)
    if not cond:
        ok = False


def read(rel):
    return open(os.path.join(SRC, rel), encoding='utf-8').read()


# ---------------------------------------------------------------- 通用工具
def strip_comments(src):
    """把注释换成等长空格（保留换行与偏移）。单趟状态机；正则会在 `//**` 处吞到文件尾。"""
    out = []
    state = None
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        nxt = src[i + 1] if i + 1 < n else ''
        if state is None:
            if c == '/' and nxt == '/':
                state = 'line'; out.append('  '); i += 2; continue
            if c == '/' and nxt == '*':
                state = 'block'; out.append('  '); i += 2; continue
            if c == '"':
                state = 'str'; out.append(c); i += 1; continue
            if c == "'":
                state = 'chr'; out.append(c); i += 1; continue
            out.append(c); i += 1; continue
        if state == 'line':
            state = None if c == '\n' else 'line'
            out.append(c if c == '\n' else ' ')
            i += 1; continue
        if state == 'block':
            if c == '*' and nxt == '/':
                state = None; out.append('  '); i += 2; continue
            out.append('\n' if c == '\n' else ' ')
            i += 1; continue
        if state in ('str', 'chr'):
            if c == '\\':
                out.append(c); out.append(nxt); i += 2; continue
            if (state == 'str' and c == '"') or (state == 'chr' and c == "'"):
                state = None
            out.append(c); i += 1; continue
    return ''.join(out)


def read_j(rel):
    raw = read(rel)
    assert '"""' not in raw, '出现 Java 文本块，strip_comments 需要升级'
    return strip_comments(raw)


def method_span(src, sig):
    """截出方法体：从签名起到顶格一层缩进的收尾 `\\n\\t}` 为止。找不到返回 None。"""
    i = src.find(sig)
    if i == -1:
        return None
    j = src.find('\n\t}', i)
    if j == -1:
        return None
    return src[i:j]


def seq_ok(s, anchors):
    """anchors 按期望先后给出；缺失锚点（-1）直接 False（不允许「缺项也算通过」）。"""
    pos = [s.find(a) for a in anchors]
    if any(p == -1 for p in pos):
        return False, pos
    return all(pos[i] < pos[i + 1] for i in range(len(pos) - 1)), pos


def const_of(src, name):
    m = re.search(r'public static final \w+ ' + re.escape(name) + r'\s*=\s*([^;]+);', src)
    return m.group(1).strip() if m else None


def raw_callers_of(token):
    """含该字面量的文件（**不剥注释**）—— 用来演示/自测「注释污染」这件事。"""
    hits = []
    for dirpath, _d, files in os.walk(SRC):
        for fn in files:
            if fn.endswith('.java'):
                p = os.path.join(dirpath, fn)
                if token in open(p, encoding='utf-8', errors='replace').read():
                    hits.append(os.path.relpath(p, SRC).replace('\\', '/'))
    return sorted(set(hits))


def code_callers_of(token):
    """含该字面量的文件，但**先剥注释**。

    与本仓其余核验脚本（如 verify_hod_netzach.py 的 callers_of）刻意不同：
    那边数的是「注释里写也算」，这里必须剥 —— 因为 Trials.java 的 NETZACH 注释里
    就写着 `HealthBar.setAlpha(fade)`，不剥的话「引用清单」会多出 Trials.java，
    让「Trials 不反向依赖 UI 类」这条判据永远为假。
    """
    hits = []
    for dirpath, _d, files in os.walk(SRC):
        for fn in files:
            if fn.endswith('.java'):
                p = os.path.join(dirpath, fn)
                try:
                    txt = strip_comments(open(p, encoding='utf-8', errors='replace').read())
                except Exception:
                    txt = open(p, encoding='utf-8', errors='replace').read()
                if token in txt:
                    hits.append(os.path.relpath(p, SRC).replace('\\', '/'))
    return sorted(set(hits))


def javap(args):
    r = subprocess.run([JAVAP] + args, capture_output=True, text=True, encoding='utf-8',
                       errors='replace')
    return r.stdout + r.stderr


def method_body(disasm, header_prefix):
    """从 javap -c 输出截出某方法体的指令列表 [(offset, opcode, rest)]。"""
    lines = disasm.splitlines()
    start = None
    for i, l in enumerate(lines):
        s = l.strip()
        if s.startswith(header_prefix) and s.endswith(';'):
            if i > 0 and lines[i - 1].strip().startswith('//'):
                continue
            start = i
            break
    if start is None:
        return None
    ins = []
    for l in lines[start + 1:]:
        s = l.strip()
        if not s:
            continue
        if s.endswith(';') and '(' in s and not s[0].isdigit():
            break
        m = re.match(r'^(\d+):\s+(\S+)(.*)$', s)
        if m:
            ins.append((int(m.group(1)), m.group(2), m.group(3)))
    return ins


def invokes(ins, needle):
    """指令 rest 里含 needle 的偏移列表（含 getfield/putfield/invoke*，只要 rest 命中）。"""
    return [off for (off, _op, rest) in (ins or []) if needle in rest]


def ops_with(ins, opcode, needle=''):
    """限定操作码 + rest 命中 needle 的偏移列表。"""
    return [off for (off, op, rest) in (ins or [])
            if op == opcode and (needle == '' or needle in rest)]


def fade_draw_ok(ins):
    """合取判据：EmoIcon.draw 是「取系数(1 次) → super.draw()(1 次) → 还原 am/aa(各 2 次写入)」，
    且**还原晚于 super.draw()**（否则等于没乘）。layer_bytecode 与 selftest 用同一个函数，
    这样自测里的反例样本才真的在验这条判据、而不是验另写的一份。"""
    ef = invokes(ins, 'Trials.enemyFade')
    sup = invokes(ins, 'com/watabou/noosa/Image.draw')
    am_put = ops_with(ins, 'putfield', 'Field am:F')
    aa_put = ops_with(ins, 'putfield', 'Field aa:F')
    return (len(ef) == 1 and len(sup) == 1 and len(am_put) == 2 and len(aa_put) == 2
            and ef[0] < sup[0] < am_put[1] and sup[0] < aa_put[1])


# ---------------------------------------------------------------- ① 源码层
def layer_source():
    print('== ① 源码结构层 ==')
    t, t_raw = read_j('Trials.java'), read('Trials.java')
    for name, want in [('NETZACH_FADE_NEAR', '1'), ('NETZACH_FADE_FAR', '3'),
                       ('NETZACH_FADE_ALPHA', '0.5f')]:
        chk(const_of(t, name) == want, 'Trials.%s == %s（实测 %s）' % (name, want, const_of(t, name)))
    fade = method_span(t, 'public static float enemyFade( Char ch ){')
    chk(fade is not None, 'Trials.enemyFade 方法体截取成功')
    if fade:
        good, pos = seq_ok(fade, ['<= NETZACH_FADE_NEAR', '<= NETZACH_FADE_FAR',
                                  'NETZACH_FADE_ALPHA', 'return 0f'])
        chk(good, 'enemyFade 阈值顺序：≤NEAR ⇒ 1.0 / ≤FAR ⇒ ALPHA / 其余 ⇒ 0（pos=%s）' % pos)
    chk('仍未收口' in t_raw and 'docs/features.md §20.5' in t_raw,
        'NETZACH 注释记录了「已接浮层 + 仍未收口的浮层」清单（原文读，不看剥注释版）')

    # --- HealthBar：新增整体透明度，且只碰 alpha ---
    hb = read_j('ui/HealthBar.java')
    sa = method_span(hb, 'public void setAlpha( float value ){')
    chk(sa is not None, 'HealthBar.setAlpha 方法体截取成功')
    if sa:
        chk('if (value < 0f) value = 0f;' in sa and 'if (value > 1f) value = 1f;' in sa,
            'setAlpha 对越界值做了夹取（0~1）')
        for f in ['Bg.alpha( value );', 'Shld.alpha( value );', 'Hp.alpha( value );']:
            chk(f in sa, 'setAlpha 覆盖了 %s' % f.strip())
        chk(sa.count('.alpha( value );') == 3, 'setAlpha 恰好 3 处 alpha 写入')
        chk('visible' not in sa and '.size(' not in sa,
            'setAlpha 不碰 visible / 几何（只管透明度，显隐与布局仍归 update/layout）')

    # --- 两条血条：取系数 → setAlpha → 用同一个系数管 visible ---
    chi = read_j('ui/CharHealthIndicator.java')
    good, pos = seq_ok(chi, ['level( target );',
                             'float fade = Trials.enemyFade( target );',
                             'setAlpha( fade );',
                             'visible = (target.HP < target.HT || target.shielding() > 0) && fade > 0f;'])
    chk(good, 'CharHealthIndicator：定位 → level → 取系数 → setAlpha → visible 复用同一系数（pos=%s）' % pos)
    chk(chi.count('Trials.enemyFade(') == 1, 'CharHealthIndicator 每帧只取一次系数（实测 %d）' % chi.count('Trials.enemyFade('))

    th = read_j('ui/TargetHealthIndicator.java')
    good, pos = seq_ok(th, ['level( target );',
                            'float fade = Trials.enemyFade( target );',
                            'setAlpha( fade );',
                            'visible = true;'])
    chk(good, 'TargetHealthIndicator：同一口径压 alpha（pos=%s）' % pos)
    chk('visible = fade > 0f' not in th,
        'TargetHealthIndicator **不动 visible**（ChaoticCenser 把它当「有没有锁定目标」在读）')
    chk(th.count('Trials.enemyFade(') == 1, 'TargetHealthIndicator 每帧只取一次系数')

    # --- EmoIcon：本帧乘、画完还原 ---
    emo = read_j('effects/EmoIcon.java')
    draw = method_span(emo, 'public void draw() {')
    chk(draw is not None, 'EmoIcon.draw 方法体截取成功')
    if draw:
        good, pos = seq_ok(draw, ['float fade = (owner != null) ? Trials.enemyFade( owner.ch ) : 1f;',
                                  'am *= fade;',
                                  'aa *= fade;',
                                  'super.draw();',
                                  'am = amBak;',
                                  'aa = aaBak;'])
        chk(good, 'draw 顺序：取系数 → 乘 am/aa → super.draw() → 还原（只影响这一帧）（pos=%s）' % pos)
        chk(draw.count('Trials.enemyFade(') == 1, 'draw 里系数每帧只取一次')
        chk('alpha(' not in draw, 'draw 里不动 alpha()（持久状态会与隐形的 AlphaTweener 打架）')
        chk(draw.count('am = amBak;') == 1 and draw.count('aa = aaBak;') == 1,
            'am/aa 的备份-还原各恰好一次')
    chk(emo.count('public void draw()') == 1, '只有基类覆写 draw（子类继承同一份，实测 %d 处）' % emo.count('public void draw()'))
    for sub in EMO_SUBCLASSES:
        chk(('public static class %s extends EmoIcon' % sub) in emo,
            '子类 EmoIcon.%s 仍在（四个状态标记一个都不能少）' % sub)

    # --- 引用清单唯一性 ---
    chk(code_callers_of('Trials.enemyFade(') == FADE_CALLERS,
        'enemyFade 的代码引用处恰为「精灵 + 两条血条 + 状态标记」（实测 %s）'
        % code_callers_of('Trials.enemyFade('))
    chk(code_callers_of('setAlpha(') == SETALPHA_CALLERS,
        'setAlpha 的代码引用处恰为「定义处 + 两条血条」（实测 %s）' % code_callers_of('setAlpha('))
    chk('Trials.java' not in code_callers_of('setAlpha('),
        'Trials.java 不反向依赖 UI 类（只在注释里提到 setAlpha，已被剥掉）')


# ---------------------------------------------------------------- ② 签名层
def layer_sig():
    print('\n== ② 编译产物签名层（javap -p）==')
    for cls, sig, desc in [
            (HEALTHBAR, 'public void setAlpha(float);', 'HealthBar.setAlpha(float)'),
            (CHARHI, 'public void update();', 'CharHealthIndicator.update()'),
            (TARGETHI, 'public void update();', 'TargetHealthIndicator.update()'),
            (EMOICON, 'public void draw();', 'EmoIcon.draw()（覆写 Image.draw）'),
            (TRIALS, 'public static float enemyFade(',
             'Trials.enemyFade(Char)（源码层解析会被注释骗，这里看真产物）')]:
        p = javap(['-p', '-cp', JAVAP_CP, cls])
        chk(sig in p, '字节码里有 %s' % desc)
    p = javap(['-p', '-cp', JAVAP_CP, EMOICON])
    chk(p.count('void draw();') == 1, 'EmoIcon 只声明一份 draw()（实测 %d）' % p.count('void draw();'))


# ---------------------------------------------------------------- ③ 字节码层
def layer_bytecode():
    print('\n== ③ 字节码层（javap -c）==')
    bt = javap(['-c', '-p', '-cp', JAVAP_CP, HEALTHBAR])
    b = method_body(bt, 'public void setAlpha(float);')
    chk(b is not None, 'HealthBar.setAlpha 方法体截取成功')
    if b:
        al = invokes(b, 'ColorBlock.alpha')
        chk(len(al) == 3, 'setAlpha 恰好调 3 次 ColorBlock.alpha（Bg/Shld/Hp 各一次，实测 %d）' % len(al))
        ob, os_, oh = (invokes(b, 'Field Bg:'), invokes(b, 'Field Shld:'), invokes(b, 'Field Hp:'))
        chk(bool(ob) and bool(os_) and bool(oh) and ob[0] < os_[0] < oh[0],
            '三个色块的顺序是 Bg → Shld → Hp（%s）' % [ob[:1], os_[:1], oh[:1]])
        chk(not ops_with(b, 'putfield'), 'setAlpha 不写任何字段（不透支几何 / visible）')
    chk(bt.count('ColorBlock.alpha') == 3,
        '整个 HealthBar 里只有 setAlpha 碰 alpha（level/layout 一个都不碰）')

    for cls, rel, keeps_visible in [(CHARHI, 'ui/CharHealthIndicator.java', False),
                                    (TARGETHI, 'ui/TargetHealthIndicator.java', True)]:
        d = javap(['-c', '-p', '-cp', JAVAP_CP, cls])
        u = method_body(d, 'public void update();')
        chk(u is not None, '%s.update 方法体截取成功' % rel)
        if not u:
            continue
        ef, sa = invokes(u, 'Trials.enemyFade'), invokes(u, 'setAlpha')
        chk(len(ef) == 1, '%s：每帧只取一次淡出系数（实测 %d）' % (rel, len(ef)))
        chk(len(sa) == 1, '%s：每帧只 setAlpha 一次（实测 %d）' % (rel, len(sa)))
        chk(bool(ef) and bool(sa) and ef[0] < sa[0],
            '%s：**先取系数再写 alpha**（顺序颠倒就会把上一帧的值落上去）' % rel)
        chk(len(ops_with(u, 'putfield', 'Field visible:Z')) == 2,
            '%s：visible 的两条写入路径都在（显示 / 隐藏）' % rel)
        if keeps_visible:
            # 只压 alpha、不动 visible ⇒ 字节码里不会出现「拿 fade 与 0f 比大小」
            chk(len(ops_with(u, 'fcmpl')) == 0 and len(ops_with(u, 'fcmpg')) == 0,
                '%s：没有 fcmp/浮点比较 ⇒ 确实没拿 fade 去算 visible' % rel)

    ed = javap(['-c', '-p', '-cp', JAVAP_CP, EMOICON])
    e = method_body(ed, 'public void draw();')
    chk(e is not None, 'EmoIcon.draw 方法体截取成功')
    if e:
        ef = invokes(e, 'Trials.enemyFade')
        sup = invokes(e, 'com/watabou/noosa/Image.draw')
        am_put = ops_with(e, 'putfield', 'Field am:F')
        aa_put = ops_with(e, 'putfield', 'Field aa:F')
        chk(len(ef) == 1, '每帧只取一次淡出系数（实测 %d）' % len(ef))
        chk(bool(ef) and bool(sup) and ef[0] < sup[0], '取系数早于 super.draw()')
        chk(len(sup) == 1, 'super.draw() 恰好一次（实测 %d）' % len(sup))
        chk(len(am_put) == 2 and len(aa_put) == 2,
            'am/aa 各被写两次 =「本帧乘」+「画完还原」（实测 am=%d aa=%d）' % (len(am_put), len(aa_put)))
        chk(bool(sup) and len(am_put) == 2 and sup[0] < am_put[1],
            '**还原发生在 super.draw() 之后**（否则等于没乘）')
        chk(not ops_with(e, 'putfield', 'Field visible:Z'),
            'draw 不碰 visible（显隐仍归 CharSprite.updateSpriteState 管）')
        chk(fade_draw_ok(e), '合取判据 fade_draw_ok(EmoIcon.draw) 通过（自测里用同一函数跑反例）')

    for sub in EMO_SUBCLASSES:
        p = javap(['-p', '-cp', JAVAP_CP, '%s$%s' % (EMOICON, sub)])
        found = ('EmoIcon$%s' % sub) in p
        chk(found, '找得到 EmoIcon$%s 的字节码（找不到的话下面那条会变成假通过）' % sub)
        chk(found and 'void draw();' not in p,
            'EmoIcon$%s 不自己覆写 draw（继承基类那一份）' % sub)

    # 反向：CharSprite 那一处没被本轮改动带偏
    spr = read_j('sprites/CharSprite.java')
    sdraw = method_span(spr, 'public void draw() {')
    chk(sdraw is not None and 'float fade = Trials.enemyFade( ch );' in sdraw
        and 'am *= fade;' in sdraw and 'am = amBak;' in sdraw,
        'CharSprite.draw 的本体淡化仍是「本帧乘 + 画完还原」')
    sd = javap(['-c', '-p', '-cp', JAVAP_CP, SPRITE])
    sb = method_body(sd, 'public void draw();')
    chk(sb is not None and invokes(sb, 'Field am:F') and len(ops_with(sb, 'putfield', 'Field am:F')) == 2,
        'CharSprite.draw 字节码同样只写两次 am（乘 + 还原）')


# ---------------------------------------------------------------- ④ 文本层
def layer_text():
    print('\n== ④ 文本层（NETZACH 描述没被本轮带坏）==')
    for fname, needles in [('misc_zh.properties', ['1 格', '3 格']),
                           ('misc.properties', ['1 tile', '3 tiles'])]:
        raw = open(os.path.join(MSG, fname), 'rb').read()
        chk(raw.count(b'\r\n') > 0 and raw.count(b'\n') == raw.count(b'\r\n'),
            '%s 仍是纯 CRLF（无裸 LF）' % fname)
        # 按 \r\n 切 ⇒ 每行不带行尾符，下面「换行必须是字面 \n」的判据才有意义
        lines = raw.decode('utf-8').split('\r\n')
        key = 'trials.netzach_desc'
        got = [l[len(key) + 1:] for l in lines if l.startswith(key + '=')]
        chk(len(got) == 1, '%s: %s 恰好 1 条（实测 %d）' % (fname, key, len(got)))
        if len(got) != 1:
            continue
        body = got[0]
        chk('占位' not in body and 'Placeholder' not in body, '%s: 不是占位文案' % fname)
        missing = [n for n in needles if n not in body]
        chk(not missing, '%s: 淡出阈值仍是 %s' % (fname, needles))
        chk('\r' not in body and '\n' not in body, '%s: 换行是**字面 \\n**（不是真换行）' % fname)


# ---------------------------------------------------------------- ⑤ 行为层
def layer_probe():
    print('\n== ⑤ 行为层（NETZACH 探针的实证输出）==')
    if not os.path.exists(PROBE_OUT):
        chk(False, '探针输出 %s 不存在（先跑 bash _chk/_build_hod_netzach.sh run）' % os.path.basename(PROBE_OUT))
        return
    out = open(PROBE_OUT, encoding='utf-8', errors='replace').read()
    chk('[FAIL]' not in out, '探针输出里没有任何 [FAIL]')
    chk('[INFO]' not in out, '探针没有被 try/catch 吞掉的段（无 [INFO]）')
    m = re.search(r'全部符合预期（(\d+) 条断言）', out)
    chk(m is not None, '探针是「全部符合预期」结尾')
    if m:
        chk(int(m.group(1)) >= 52, '探针断言数 %s ≥ 52' % m.group(1))
    for needle in ['距离 1 ⇒ 完全不透明（阈值含 1）', '距离 2 ⇒ 半透明 0.5',
                   '距离 3 ⇒ 半透明 0.5（阈值含 3）', '距离 4 ⇒ 完全不可见 0']:
        chk(needle in out, '探针实测：%s' % needle)


# ---------------------------------------------------------------- 反例自测
def selftest():
    print('== selftest：比位置判据不恒真 ==')
    good, _ = seq_ok('A_B_C', ['A', 'B', 'C'])
    chk(good, '正例：A→B→C 判为真')
    bad, _ = seq_ok('A_C_B', ['A', 'B', 'C'])
    chk(not bad, '反例：B/C 互换 ⇒ 判为假（抓得住顺序颠倒）')
    miss, pos = seq_ok('A_C', ['A', 'B', 'C'])
    chk(not miss and -1 in pos, '反例：缺锚点 ⇒ 判为假（不允许缺项通过）')

    print('== selftest：strip_comments 保偏移、不吞后续 ==')
    s = 'int a = 1; // 注释 //** 里\nint b = 2; /* 块 */ int c = 3;'
    st = strip_comments(s)
    chk(len(st) == len(s), '剥注释后长度不变（保偏移）')
    chk('int b = 2;' in st and 'int c = 3;' in st, '`//**` 不会吞到文件末尾')

    print('== selftest：字节码顺序判据不恒真（用 layer_bytecode 的同一个函数）==')
    def _ins(off, op, rest):
        return (off, op, rest)

    def _sample(restore_before_draw):
        """造一段 EmoIcon.draw 的合成指令流。restore_before_draw=True 时把「还原」提到
        super.draw() 之前（乘了又立刻还原，等于白乘）—— 这是本判据要抓的那个错。"""
        seq = [_ins(0, 'invokestatic',
                    '  #94  // Method ' + PKG + '/Trials.enemyFade:(L' + PKG + '/actors/Char;)F'),
               _ins(3, 'getfield', '      #100  // Field am:F'),
               _ins(6, 'getfield', '      #103  // Field aa:F'),
               _ins(9, 'fconst_1', ''), _ins(10, 'fcmpl', ''), _ins(11, 'ifge', '          31'),
               _ins(14, 'getfield', '      #100  // Field am:F'),
               _ins(17, 'putfield', '      #100  // Field am:F'),   # ← 本帧「乘」
               _ins(20, 'getfield', '      #103  // Field aa:F'),
               _ins(23, 'putfield', '      #103  // Field aa:F')]
        if restore_before_draw:
            seq += [_ins(26, 'putfield', '      #100  // Field am:F'),   # ← 还原（提前）
                    _ins(29, 'putfield', '      #103  // Field aa:F'),
                    _ins(32, 'invokespecial', '  #106  // Method com/watabou/noosa/Image.draw:()V'),
                    _ins(35, 'return', '')]
        else:
            seq += [_ins(26, 'invokespecial', '  #106  // Method com/watabou/noosa/Image.draw:()V'),
                    _ins(29, 'putfield', '      #100  // Field am:F'),   # ← 画完「还原」
                    _ins(32, 'putfield', '      #103  // Field aa:F'),
                    _ins(35, 'return', '')]
        return seq

    fwd, rev = _sample(False), _sample(True)
    chk(len(ops_with(fwd, 'putfield', 'Field am:F')) == 2,
        'ops_with 数得对：乘 + 还原 = 2 处 putfield am')
    chk(len(ops_with(fwd, 'putfield', 'Field aa:F')) == 2,
        '样本带 aa 的两次写入（不是凭空造出来的假数据）')
    chk(fade_draw_ok(fwd), '正例：取系数 → 乘 → super.draw() → 还原 ⇒ 判为真')
    chk(not fade_draw_ok(rev), '反例：还原提到 super.draw() 之前 ⇒ 同一判据判为假')
    lost = [t for t in fwd if t[1] != 'invokestatic']
    chk(not fade_draw_ok(lost), '反例：丢掉「取系数」那一条 ⇒ 判为假（缺项不算通过）')

    print('== selftest：调用点清单不是恒真（注释污染必须被剥掉）==')
    chk('Trials.java' in raw_callers_of('setAlpha('),
        '不剥注释时 Trials.java 会混进清单（注释里写了 HealthBar.setAlpha(fade)）')
    chk('Trials.java' not in code_callers_of('setAlpha('),
        '剥注释后 Trials.java 落回清单之外 ⇒ 证明剥离真的生效、不是恒真')
    chk(code_callers_of('setAlpha(') != SETALPHA_CALLERS + ['Trials.java'],
        '反例：把 Trials.java 硬塞进清单 ⇒ 判为不等')

    print('== selftest：常量断言真的读源码、不是写死 ==')
    t = read_j('Trials.java')
    chk(const_of(t, 'NETZACH_FADE_FAR') == '3', '实读 NETZACH_FADE_FAR = 3')
    fake = re.sub(r'(public static final int NETZACH_FADE_FAR\s*=\s*)3', r'\g<1>9', t, count=1)
    chk(fake != t and const_of(fake, 'NETZACH_FADE_FAR') == '9',
        '反例：改成 9 ⇒ 断言随之读到 9（非写死）')


# ---------------------------------------------------------------- 主流程
def main():
    print('NETZACH 浮层淡出核验（血条 + 状态标记）')
    print('=' * 62)
    layer_source()
    layer_sig()
    layer_bytecode()
    layer_text()
    layer_probe()
    print('\n' + '=' * 62)
    if ok:
        print('全部核验通过。')
    else:
        print('存在失败项，见上方 [FAIL]。')
    print('=' * 62)
    return 0 if ok else 1


if __name__ == '__main__':
    if '--selftest' in sys.argv:
        selftest()
        print('\n自测%s。' % ('全过' if ok else '有失败'))
        sys.exit(0 if ok else 1)
    sys.exit(main())
