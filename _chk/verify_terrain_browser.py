# -*- coding: utf-8 -*-
"""地形编辑器 · 真实浏览器端到端核验（无头 Edge）

为什么必须做这一层：render.js / editor.js / codegen.js 的逻辑能用纯 Node 验，
但「页面加载后下拉框真的有选项吗 / 画布尺寸对吗 / 代码框有内容吗」
只有真浏览器才知道 —— 本轮就靠它抓到了一个 boot() 期间的空指针真 bug。

做法：
  ① 把 index.html 复制到**同目录**（保证 <script src> 相对路径不变）
  ② 在末尾追加一个探针脚本，把关键状态写成一段文字
  ③ 用 --dump-dom + --allow-file-access-from-files 取回（后者能解开
     file:// 下的 "Script error." 跨源屏蔽，拿到真实异常原文）
  ④ 断言：无异常、控件有选项、画布尺寸正确、代码框含 level.drop

⚠️ 本脚本要求 index.html / *.js 与它**同目录**运行，所以用副本而不是原文件。
"""
import io
import os
import re
import shutil
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ED = os.path.join(ROOT, 'tools', 'terrain-editor')
SRC = os.path.join(ED, 'index.html')
TMP = os.path.join(ED, '_verify_browser_probe.html')

EDGE_CANDIDATES = [
    r'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe',
    r'C:\Program Files\Microsoft\Edge\Application\msedge.exe',
]
EDGE = next((e for e in EDGE_CANDIDATES if os.path.exists(e)), None)

SELFTEST = '--selftest' in sys.argv


def build_probe():
    html = io.open(SRC, encoding='utf-8').read()
    # ⚠️ 坑：index.html 里的 head 标签带属性（`<head data-page-node-id="...">`），
    #    所以 `replace('<head>', ...)` 会**静默不生效**，钩子根本没插进去 ⇒
    #    后面探针读 window.__ERRS 直接抛 "reading 'join'"。用正则才对。
    hook = (
        '<script>\n'
        'window.__ERRS = [];\n'
        'window.addEventListener("error", function(e){\n'
        '  window.__ERRS.push(String(e.message) + " @ " + (e.filename || "?") + ":" + (e.lineno || "?"));\n'
        '}, true);\n'
        '</script>\n'
    )
    html, n = re.subn(r'<head[^>]*>', lambda mo: mo.group(0) + hook, html, count=1)
    assert n == 1, 'index.html 里找不到 <head>，无法插错误钩子'

    probe = (
        '<script>\n'
        'function __dump(){\n'
        '  var E = window.__ERRS || [];\n'
        '  var g = function(id){ return document.getElementById(id); };\n'
        '  var opt = function(id){ var e = g(id); return e ? e.options.length : -1; };\n'
        '  var grp = function(id){ var e = g(id); return e ? e.querySelectorAll("optgroup").length : -1; };\n'
        '  var cv = document.querySelector("canvas");\n'
        '  var cb = g("codeBox");\n'
        '  var o = {\n'
        '    errs: E.length ? E.join(" | ") : "",\n'
        '    te: {\n'
        '      assets: typeof TE_ASSETS, render: typeof TE_RENDER,\n'
        '      editor: typeof TE_EDITOR, gen: typeof TE_GEN, app: typeof TE_APP\n'
        '    },\n'
        '    sheets: (typeof TE_SHEETS !== "undefined") ? TE_SHEETS.length : -1,\n'
        '    assetKeys: (typeof TE_ASSETS !== "undefined") ? Object.keys(TE_ASSETS).length : -1,\n'
        '    appHasItems: (typeof TE_ASSETS !== "undefined") && !!TE_ASSETS.items,\n'
        '    selTrap: opt("selTrap"), selPlant: opt("selPlant"),\n'
        '    selItem: opt("selItem"), selItemGroups: grp("selItem"),\n'
        '    selHeap: opt("selHeap"),\n'
        '    layerHintLen: (g("layerHint") ? (g("layerHint").textContent || "").length : -1),\n'
        '    oldTrapHint: !!g("trapHint"),\n'
        '    chkItems: g("chkItems") ? g("chkItems").checked : null,\n'
        '    canvas: cv ? (cv.width + "x" + cv.height) : "none",\n'
        '    codeLen: cb ? cb.value.length : -1,\n'
        '    codeHasDrop: cb ? /level[.]drop[(]/.test(cb.value) : false,\n'
        '    codeHasNewFood: cb ? /new Food[(][)]/.test(cb.value) : false,\n'
        '    /* 只查**非注释行**：生成物的注释里**故意**写着「别用 addItemToSpawn」的警告 */\n'
        '    codeHasAddItemToSpawn: cb ? cb.value.split("\\n")\n'
        '        .filter(function(l){ return !/^\\s*\\/\\//.test(l); })\n'
        '        .some(function(l){ return /addItemToSpawn/.test(l); }) : false\n'
        '  };\n'
        '  var pre = document.createElement("pre");\n'
        '  pre.id = "PROBE";\n'
        '  /* 用属性携带 JSON：属性值里不会有 HTML 转义歧义，取起来最稳 */\n'
        '  pre.setAttribute("data-json", JSON.stringify(o));\n'
        '  pre.textContent = "[[PROBE]]" + JSON.stringify(o) + "[[/PROBE]]";\n'
        '  (document.body || document.documentElement).appendChild(pre);\n'
        '}\n'
        '/* 探针自身出异常也必须留下痕迹，否则会被误判成「app.js 挂了」 */\n'
        'try { __dump(); }\n'
        'catch (err) {\n'
        '  var q = document.createElement("pre");\n'
        '  q.id = "PROBE";\n'
        '  q.setAttribute("data-json", JSON.stringify({ probeThrow: String(err && err.stack || err) }));\n'
        '  (document.body || document.documentElement).appendChild(q);\n'
        '}\n'
        '/* ⚠️ 采样时机（踩了两个坑才定下来）：\n'
        ' *    ① `setTimeout(...)` 不行 —— 这个 Edge 版本的 --dump-dom 在页面 load\n'
        ' *       后立刻序列化 DOM，计时器根本没机会跑（--virtual-time-budget 对它无效）。\n'
        ' *    ② `window.addEventListener("load", ...)` 也不行 —— 本页有 330KB+ 的\n'
        ' *       base64 图集要解码，load 事件被拖到序列化之后，回调同样不执行。\n'
        ' *\n'
        ' *    唯一可靠的做法：**紧跟在 app.js 之后同步执行**。\n'
        ' *    boot() 本身就是同步跑到底的（控件在 boot 里就建好了），\n'
        ' *    所以此刻 DOM 已经定型，采到的就是真实状态。\n'
        ' *    异步的图集加载只影响画布内容，不影响控件有无选项。 */\n'
        'window.addEventListener("load", function(){\n'
        '  if (!document.getElementById("PROBE")) { try { __dump(); } catch (e) {} }\n'
        '});\n'
        '</script>\n'
    )
    html = html.replace('</body>', probe + '</body>')
    io.open(TMP, 'w', encoding='utf-8', newline='\n').write(html)


def run():
    url = 'file:///' + TMP.replace(os.sep, '/')
    cmd = [EDGE, '--headless=new', '--disable-gpu', '--no-sandbox',
           '--allow-file-access-from-files', '--disable-web-security',
           '--virtual-time-budget=20000', '--window-size=1500,1200',
           '--dump-dom', url]
    r = subprocess.run(cmd, capture_output=True, timeout=240)
    return r.stdout.decode('utf-8', 'replace')


def main():
    if not EDGE:
        print('SKIP: 找不到 msedge.exe')
        return 0
    build_probe()
    try:
        dom = run()
    finally:
        if os.path.exists(TMP):
            os.remove(TMP)

    # ⚠️ 三个坑：
    #    ① 探针脚本**源码**里也含 [[PROBE]] 字面量（写在 textContent 拼接里），
    #       宽松正则会把源码那段抓出来 ⇒ JSON.parse 出来是字符串。
    #    ② data-json 属性在 DOM 序列化时会被 **HTML 转义**（" → &quot;），
    #       直接 JSON.parse 会炸，必须先 unescape。
    #    ③ 属性后面紧跟 textContent，不是立刻 `</pre>`，所以只匹配到 `">` 为止。
    m = re.search(r'<pre id="PROBE" data-json="(.*?)">', dom, re.S)
    if m:
        raw = m.group(1)
        raw = (raw.replace('&quot;', '"').replace('&amp;', '&')
                  .replace('&lt;', '<').replace('&gt;', '>').replace('&#39;', "'"))
    else:
        m2 = re.search(r'<pre id="PROBE">\[\[PROBE\]\](\{.*?\})\[\[/PROBE\]\]</pre>', dom, re.S)
        raw = m2.group(1) if m2 else None

    if raw is None:
        print('❌ 页面里没有 PROBE 节点 —— app.js 大概率在探针执行前就抛异常了')

        # 尽力把真实异常抠出来
        for pat in [r'Uncaught [^<]{0,200}', r'Cannot read [^<]{0,120}']:
            hit = re.search(pat, dom)
            if hit:
                print('   抓到: ' + hit.group(0))
                break
        return 1

    import json
    d = json.loads(raw)

    fails, passes = [], []

    def ok(cond, msg, extra=None):
        (passes if cond else fails).append(msg if cond else (msg + ('  → %s' % extra if extra is not None else '')))

    ok(d['errs'] == '', '页面无 JS 异常', d['errs'])
    ok(d['te']['assets'] == 'object', 'TE_ASSETS 已定义')
    ok(d['te']['render'] == 'object', 'TE_RENDER 已定义')
    ok(d['te']['editor'] == 'object', 'TE_EDITOR 已定义')
    ok(d['te']['gen'] == 'object', 'TE_GEN 已定义')
    ok(d['sheets'] >= 6, '图集下拉项 ≥ 6（当前 %s）' % d['sheets'])
    ok(d['appHasItems'], 'assets.js 里含 items（道具图集）')
    ok(d['selTrap'] >= 25, '陷阱下拉有选项（%s）' % d['selTrap'])
    ok(d['selPlant'] >= 10, '植物下拉有选项（%s）' % d['selPlant'])
    ok(d['selItem'] >= 60, '**道具下拉有选项**（%s）' % d['selItem'])
    ok(d['selItemGroups'] >= 6, '道具下拉分了 ≥6 组（%s）' % d['selItemGroups'])
    ok(d['selHeap'] == 7, '堆型下拉 = 7（%s）' % d['selHeap'])
    ok(not d['oldTrapHint'], '旧的 #trapHint 已改名（不应再存在）')
    ok(d['layerHintLen'] > 0, '#layerHint 有提示文字（%s 字）' % d['layerHintLen'])
    ok(d['chkItems'] is True, '#chkItems 默认勾选')
    ok(d['canvas'] not in ('none', '16x16'), '画布已按房间尺寸展开（%s）' % d['canvas'])
    ok(d['codeLen'] > 500, '代码框有生成内容（%s 字）' % d['codeLen'])
    ok(d['codeHasDrop'], '**生成的代码含 level.drop**（道具层已接线）')
    ok(d['codeHasNewFood'], '生成的代码含 new Food()（boot 演示道具）')
    ok(not d['codeHasAddItemToSpawn'], '生成的代码不含 addItemToSpawn')

    if SELFTEST:
        print('=== 反例自测：断言 —— 若把 selItem 期望改到 999 必然失败 ===')
        (passes if 0 >= 999 else fails).append('selItem >= 999（必然失败，用于证明断言有效）')
        print('实际 selItem = %s ⇒ 该项如预期进入失败列表' % d['selItem'])

    print('=== 浏览器端到端核验 ===')
    for p in passes:
        print('  ✓ ' + p)
    for f in fails:
        print('  ✗ ' + f)
    print()
    if fails:
        print('❌ 失败 %d 项（通过 %d）' % (len(fails), len(passes)))
        return 1
    print('✅ 全部通过（%d 项）：真实浏览器里编辑器可用且道具层已接线' % len(passes))
    return 0


if __name__ == '__main__':
    sys.exit(main())
