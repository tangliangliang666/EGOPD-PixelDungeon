# -*- coding: utf-8 -*-
"""道具表核验（EGOPD 地形编辑器）

为什么要单独做一件事：编辑器下拉框里的类名会被 codegen 原样写成 `new Xxx()`，
所以**任何一个拼错/不存在的类名都会让用户拿到的生成代码编译失败**。
字符串断言看不出这件事，只有拿真实源码路径去查才能发现。

本脚本查四件事（全部对着真源码，不信任任何一张手写清单）：
  ① ITEMS 每个 cls 在 items/<pkg>/<cls>.java 处**确实存在**
  ② 该文件里确实声明了 `public class <cls>`，且**不是 abstract**（否则 new 不出来）
  ③ 无重复条目
  ④ makeItem() 对每条都能吐出 ctor 字符串

反例自测：`--selftest` 会往 ITEMS 里塞一个不存在的类，断言本脚本失败。
"""
import io
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ITEM_ROOT = os.path.join(ROOT, 'core', 'src', 'main', 'java',
                         'com', 'shatteredpixel', 'shatteredpixeldungeon', 'items')

NODE = None
for cand in [r'C:\Users\14675\.workbuddy\binaries\node\versions\22.22.2-2\node.exe',
             r'C:\Program Files\nodejs\node.exe', 'node']:
    if cand == 'node' or os.path.exists(cand):
        NODE = cand
        break

JS = r'''
const fs=require('fs'), path=require('path'), vm=require('vm');
const ED=path.join(__dirname,'..','tools','terrain-editor');
const sb={}; sb.window=sb; sb.console=console;
sb.document={createElement:function(){return{width:0,height:0,getContext:function(){return{drawImage(){},getImageData:(x,y,w,h)=>({data:new Uint8ClampedArray(w*h*4)})};}};}};
vm.createContext(sb);
['render.js','editor.js','codegen.js'].forEach(function(f){
  vm.runInContext(fs.readFileSync(path.join(ED,f),'utf8'),sb,{filename:f});
});
const R=sb.TE_RENDER;
if (process.env.TE_INJECT_BAD) {
  R.ITEMS.push({cls:process.env.TE_INJECT_BAD, zh:'(自测注入的假类)', pkg:'food', kind:'plain'});
}
const out={items:R.ITEMS.map(function(it){return [it.cls, it.pkg===undefined?null:it.pkg];}),
           groups:R.ITEM_GROUPS.length, heaps:R.HEAP_TYPES.length, ctors:{}};
R.ITEMS.forEach(function(it){ const m=R.makeItem(it.cls,{heap:'HEAP'}); out.ctors[it.cls]= m?m.ctor:null; });
console.log(JSON.stringify(out));
'''

SELFTEST = '--selftest' in sys.argv


def run_node(inject=None):
    script = os.path.join(HERE, '_tmp_item_table.js')
    # 用 UTF-8 写，且显式指到 tools 目录的相对路径（__dirname 会是 _chk）
    io.open(script, 'w', encoding='utf-8', newline='\n').write(JS)
    env = dict(os.environ)
    if inject:
        env['TE_INJECT_BAD'] = inject
    r = subprocess.run([NODE, script], capture_output=True, env=env, cwd=HERE)
    try:
        os.remove(script)
    except OSError:
        pass
    if r.returncode != 0:
        print(r.stderr.decode('utf-8', 'replace'))
        raise SystemExit('Node 侧加载失败')
    return __import__('json').loads(r.stdout.decode('utf-8'))


def check(data):
    fails, passes = [], []

    def ok(cond, msg, extra=None):
        (passes if cond else fails).append(msg if cond else (msg + ('  → %s' % extra if extra else '')))

    items = data['items']
    ok(len(items) >= 60, '道具表条目数 ≥ 60（当前 %d）' % len(items))
    ok(data['groups'] >= 5, '分类组数 ≥ 5（当前 %d）' % data['groups'])
    ok(data['heaps'] == 7, '堆型 = 7（当前 %d）' % data['heaps'])

    seen, dup = set(), []
    for cls, pkg in items:
        if cls in seen:
            dup.append(cls)
        seen.add(cls)
    ok(not dup, '无重复条目', dup)

    missing, notpublic, abstract = [], [], []
    for cls, pkg in items:
        p = os.path.join(ITEM_ROOT, *(([pkg] if pkg else []) + [cls + '.java']))
        if not os.path.exists(p):
            missing.append('%s (期望 %s)' % (cls, p))
            continue
        src = io.open(p, encoding='utf-8', errors='replace').read()
        m = re.search(r'public\s+(abstract\s+)?class\s+%s\b' % re.escape(cls), src)
        if not m:
            notpublic.append(cls)
        elif m.group(1):
            abstract.append(cls)
    ok(not missing, '每个 cls 都有对应的 .java 源文件', missing)
    ok(not notpublic, '每个源文件都声明了 public class <cls>', notpublic)
    ok(not abstract, '没有 abstract 类（否则 new 不出来）', abstract)

    noc = [c for c, v in data['ctors'].items() if not v]
    ok(not noc, '每个条目都能构造出 ctor 字符串', noc)

    return passes, fails


data = run_node(inject='TotallyFakeItem' if SELFTEST else None)
passes, fails = check(data)

if SELFTEST:
    print('=== 反例自测：已注入不存在的类 TotallyFakeItem ===')
    if fails:
        print('✅ 反例自测通过：本脚本确实会失败（%d 项）' % len(fails))
        for f in fails:
            print('   ✗ ' + f)
        sys.exit(0)
    print('❌ 反例自测失败：注入假类后本脚本竟然还全绿，说明源码存在性根本没被检查')
    sys.exit(1)

print('=== 道具表核验 ===')
for p in passes:
    print('  ✓ ' + p)
for f in fails:
    print('  ✗ ' + f)
print()
if fails:
    print('❌ 失败 %d 项（通过 %d）' % (len(fails), len(passes)))
    sys.exit(1)
print('✅ 全部通过（%d 项）：道具表与真实源码一一对应' % len(passes))
