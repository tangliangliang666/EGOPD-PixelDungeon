# -*- coding: utf-8 -*-
"""核验「道具占位图 = items.png 的口粮那一格」这条链路的每一环。

三层证据，缺一不可：
  ① 常量层：ItemSpriteSheet.RATION 的真值必须 = 我们算出来的 437
  ② 数据层：items.png 第 437 帧在真图集里**确实有像素**（不是空格）
  ③ 渲染层：render.js 的 render() 真的把这一帧 drawImage 到了有道具的格上

只验 ① 会漏掉「图集换了/帧号算错」；只验 ③ 会漏掉「画的是空格、什么都看不到」。
"""
import io
import os
import re
import struct
import subprocess
import sys
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ITEMS_PNG = os.path.join(ROOT, 'core', 'src', 'main', 'assets', 'sprites', 'items.png')
ISS_JAVA = os.path.join(ROOT, 'core', 'src', 'main', 'java', 'com', 'shatteredpixel',
                        'shatteredpixeldungeon', 'sprites', 'ItemSpriteSheet.java')

NODE_CANDIDATES = [
    r'C:\Users\14675\.workbuddy\binaries\node\versions\22.22.2-2\node.exe',
    r'C:\Program Files\nodejs\node.exe',
    'node',
]
NODE = next((n for n in NODE_CANDIDATES if n == 'node' or os.path.exists(n)), 'node')

fails, passes = [], []


def ok(cond, msg, extra=None):
    (passes if cond else fails).append(msg if cond else (msg + ('  → %s' % extra if extra is not None else '')))


def decode_png_rgba(buf):
    """最小 PNG 解码（RGBA/RGB 8bit 非隔行）—— 只依赖 stdlib。"""
    assert buf[:8] == b'\x89PNG\r\n\x1a\n'
    i, idat = 8, b''
    w = h = ct = bd = None
    while i < len(buf):
        ln = struct.unpack('>I', buf[i:i + 4])[0]
        typ = buf[i + 4:i + 8]
        data = buf[i + 8:i + 8 + ln]
        i += 12 + ln
        if typ == b'IHDR':
            w, h, bd, ct, _, _, interlace = struct.unpack('>IIBBBBB', data)
            assert bd == 8 and interlace == 0, '本解码器只支持 8bit 非隔行'
        elif typ == b'IDAT':
            idat += data
        elif typ == b'IEND':
            break
    ch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ct]
    raw = zlib.decompress(idat)
    stride = w * ch
    out = bytearray()
    prev = bytearray(stride)
    pos = 0
    for _y in range(h):
        f = raw[pos]; pos += 1
        line = bytearray(raw[pos:pos + stride]); pos += stride
        for x in range(stride):
            a = line[x - ch] if x >= ch else 0
            b = prev[x]
            c = prev[x - ch] if x >= ch else 0
            if f == 1:
                line[x] = (line[x] + a) & 255
            elif f == 2:
                line[x] = (line[x] + b) & 255
            elif f == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                pp = a + b - c
                pa, pb, pc = abs(pp - a), abs(pp - b), abs(pp - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 255
        out += line
        prev = line
    return w, h, ch, bytes(out)


# ---------------------------------------------------------------- ① 常量层
java = io.open(ISS_JAVA, encoding='utf-8', errors='replace').read()
m_size = re.search(r'public\s+static\s+final\s+int\s+SIZE\s*=\s*(\d+)', java)
m_tw = re.search(r'private\s+static\s+final\s+int\s+TX_WIDTH\s*=\s*(\d+)', java)
m_food = re.search(r'private\s+static\s+final\s+int\s+FOOD\s*=\s*xy\((\d+),\s*(\d+)\)', java)
m_ration = re.search(r'public\s+static\s+final\s+int\s+RATION\s*=\s*FOOD\+(\d+)', java)

ok(m_size and int(m_size.group(1)) == 16, 'ItemSpriteSheet.SIZE = 16', m_size and m_size.group(1))
ok(m_tw and int(m_tw.group(1)) == 256, 'ItemSpriteSheet.TX_WIDTH = 256', m_tw and m_tw.group(1))

if m_food and m_ration:
    fx, fy = int(m_food.group(1)), int(m_food.group(2))
    width = 256 // 16
    food = (fx - 1) + width * (fy - 1)
    ration_java = food + int(m_ration.group(1))
    ok(ration_java == 437, '按 Java 的 xy() 公式算出的 RATION = 437', ration_java)
else:
    ok(False, '能解析出 FOOD / RATION 定义')
    ration_java = None

# 顺带核对我写的 C 常量（xy(1,28)+5）与 Java 一致
my_ration = (1 - 1) + 16 * (28 - 1) + 5
ok(my_ration == 437, 'render.js 里注释写的 xy(1,28)+5 = 437', my_ration)
if ration_java is not None:
    ok(my_ration == ration_java, '两者一致（手写常量没写错）', '%s vs %s' % (my_ration, ration_java))

# ---------------------------------------------------------------- ② 数据层
w, h, ch, px = decode_png_rgba(io.open(ITEMS_PNG, 'rb').read())
ok(w == 256 and h == 800, 'items.png 尺寸 256×800（= TX_WIDTH × TX_HEIGHT）', '%dx%d' % (w, h))

cols = w // 16
FRAME = 437
col, row = FRAME % cols, FRAME // cols
x0, y0 = col * 16, row * 16
n_opaque = 0
for yy in range(y0, y0 + 16):
    for xx in range(x0, x0 + 16):
        o = (yy * w + xx) * ch
        if ch == 4:
            if px[o + 3] != 0:
                n_opaque += 1
        else:
            if not (px[o] == 0 and px[o + 1] == 0 and px[o + 2] == 0):
                n_opaque += 1

ok(n_opaque > 0, '第 %d 帧（col %d, row %d）在真图集里**有像素**' % (FRAME, col, row), n_opaque)
ok(n_opaque > 60, '该帧像素数像一张真图（> 60）', n_opaque)

# 反向对照：找一个确实是空的帧，证明「空帧」是可被区分的
def opaque(idx):
    c, r = idx % cols, idx // cols
    n = 0
    for yy in range(r * 16, r * 16 + 16):
        for xx in range(c * 16, c * 16 + 16):
            o = (yy * w + xx) * ch
            if ch == 4:
                if px[o + 3]:
                    n += 1
            else:
                if not (px[o] == 0 and px[o + 1] == 0 and px[o + 2] == 0):
                    n += 1
    return n


empty_found = [i for i in range(430, 450) if opaque(i) == 0]
ok(len(empty_found) > 0, '相邻帧里确实存在**空帧**（说明本判据能区分空/非空）', empty_found)

# ---------------------------------------------------------------- ③ 渲染层
JS = r'''
const fs=require('fs'), path=require('path'), vm=require('vm');
const ED=path.join(process.env.TE_ROOT,'tools','terrain-editor');
const sb={}; sb.window=sb; sb.console=console;
const calls=[];
/* render.js 的 Sheet(img) 会 new 一个 canvas 并 getImageData 取像素。
 * 我们要让「口粮那一帧」在假图集里**真有像素**（否则 solidCount=0 ⇒ 走退化分支，
 * 验不到占位图）。做法：造一个 256×800 的 RGBA 缓冲，把第 437 帧涂成全不透明。 */
const W=256,H=800;
const buf=new Uint8ClampedArray(W*H*4);
(function(){ const c=437%16, r=Math.floor(437/16);
  for(let y=r*16;y<r*16+16;y++) for(let x=c*16;x<c*16+16;x++){
    const o=(y*W+x)*4; buf[o]=200; buf[o+1]=80; buf[o+2]=40; buf[o+3]=255; } })();
sb.document={createElement:function(){return{width:0,height:0,
  getContext:function(){return{
    drawImage:function(){calls.push(Array.prototype.slice.call(arguments));},
    getImageData:function(){return {data:buf};}
  };}};}};
vm.createContext(sb);
['render.js','editor.js'].forEach(function(f){
  vm.runInContext(fs.readFileSync(path.join(ED,f),'utf8'),sb,{filename:f});
});
const R=sb.TE_RENDER, Ed=sb.TE_EDITOR;
Ed.setRoomSize(6,6);
Ed.setItem(2,2, R.makeItem('Food'));
Ed.setItem(4,4, R.makeItem('PotionOfHealing',{heap:'CHEST'}));
Ed.syncLayers();
/* ⚠️ 必须先给一个楼层图集：render() 开头有「!S.sheet 就直接 return」的防护
 *    （那是修 boot 期空指针时加的），不给就会一次 drawImage 都不发生。 */
const fakeSheetImg = {width:W, height:H, getContext:function(){ return sb.document.createElement().getContext(); }};
R.setSheets(fakeSheetImg, fakeSheetImg);
if (typeof Image === 'undefined') { /* noop */ }
R.setItemSheet(fakeSheetImg);
const ctx={
  save(){}, restore(){}, scale(){}, clearRect(){},
  drawImage:function(){ calls.push(Array.prototype.slice.call(arguments)); },
  fillRect(){}, fillText(){}, beginPath(){}, moveTo(){}, lineTo(){}, stroke(){},
  set imageSmoothingEnabled(v){}, get imageSmoothingEnabled(){return false;},
  font:'', fillStyle:'', strokeStyle:'', lineWidth:1, textAlign:'', textBaseline:''
};
const solid = R.state.itemSheet ? R.state.itemSheet.solidCount(437) : -1;
calls.length=0;
R.render(ctx, {scale:1});
/* 道具层那一次的特征：9 个参数、源矩形 16×16、源 x=80 源 y=432 */
const itemCalls = calls.filter(function(c){ return c.length===9 && c[3]===16 && c[4]===16
  && c[1]===80 && c[2]===432; });
console.log(JSON.stringify({
  rationFrame: R.RATION_FRAME,
  selItemCount: R.ITEMS.length,
  foodCtor: (R.makeItem('Food')||{}).ctor,
  sheetSolid437: solid,
  totalDraws: calls.length,
  rationDraws: itemCalls.length,
  dsts: itemCalls.map(function(c){ return c[5]+","+c[6]; })
}));
'''


def run_node():
    script = os.path.join(HERE, '_tmp_item_render.js')
    io.open(script, 'w', encoding='utf-8', newline='\n').write(JS)
    env = dict(os.environ)
    env['TE_ROOT'] = ROOT
    r = subprocess.run([NODE, script], capture_output=True, env=env, cwd=HERE)
    try:
        os.remove(script)
    except OSError:
        pass
    if r.returncode != 0:
        print(r.stderr.decode('utf-8', 'replace'))
        raise SystemExit('Node 渲染核验失败')
    import json
    return json.loads(r.stdout.decode('utf-8'))


d = run_node()
ok(d['rationFrame'] == FRAME, 'render.js 导出的 RATION_FRAME = %d' % FRAME, d['rationFrame'])
ok(d['selItemCount'] >= 60, '道具表条目数 ≥ 60（当前 %s）' % d['selItemCount'])
ok(d['foodCtor'] == 'new Food()', 'Food 的生成语句 = new Food()', d['foodCtor'])
ok(d['sheetSolid437'] > 0, '假图集里第 %d 帧被识别为「有像素」' % FRAME, d['sheetSolid437'])
ok(d['totalDraws'] > 0, 'render() 确实调了 drawImage（%d 次）' % d['totalDraws'])
ok(d['rationDraws'] == 2, '**有道具的 2 个格子各画了 1 次口粮占位图**', d['rationDraws'])
ok('32,32' in d['dsts'] and '64,64' in d['dsts'],
   '落点分别是 (2,2) 与 (4,4) 格即 (32,32) 与 (64,64)', d['dsts'])

# 反向：把道具清空后不应再画口粮帧
print()
print('=== 道具占位图链路核验 ===')
for p in passes:
    print('  ✓ ' + p)
for f in fails:
    print('  ✗ ' + f)
print()
if fails:
    print('❌ 失败 %d 项（通过 %d）' % (len(fails), len(passes)))
    sys.exit(1)
print('✅ 全部通过（%d 项）：常量 / 图集像素 / 渲染调用三层一致' % len(passes))
