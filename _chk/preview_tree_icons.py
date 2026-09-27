# 生成 tree.png 十帧的标注验收图（_chk/_tree_trials_icons_preview.png）
#   每格：图标本体（放大 NEAREST）+ 序号 + 质点名 + 实测包围盒 + Trials.ICON_W/H 声明值
import re

from PIL import Image, ImageDraw

TREE = 'core/src/main/assets/interfaces/tree.png'
JAVA = 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/Trials.java'

NAMES = ['KETER', 'HOKMA', 'BINAH', 'CHESED', 'GEBURA',
         'TIPHERETH', 'NETZACH', 'HOD', 'YESOD', 'MALKUTH']


def read_java():
    src = open(JAVA, encoding='utf-8').read()

    def arr(name):
        m = re.search(r'ICON_' + name + r'\s*=\s*\{([^}]*)\}', src)
        return [int(v.strip()) for v in m.group(1).split(',') if v.strip()]

    return arr('W'), arr('H')


def measure(im, fw=16, fh=16):
    out = []
    for c in range(im.width // fw):
        box = im.crop((c * fw, 0, c * fw + fw, fh))
        bb = box.getchannel('A').getbbox()      # (l, t, r, b) 不含 b/r
        cnt = sum(1 for p in box.get_flattened_data() if p[3] > 0)
        out.append((bb[2] - bb[0], bb[3] - bb[1], bb[0], bb[1], cnt))
    return out


im = Image.open(TREE).convert('RGBA')
jw, jh = read_java()
meas = measure(im)

S = 12          # 图标放大倍数
CW, CH = 16 * S, 16 * S + 34
PAD = 8
COLS = 5
ROWS = 2
W = COLS * CW + (COLS + 1) * PAD
H = ROWS * CH + (ROWS + 1) * PAD

out = Image.new('RGBA', (W, H), (32, 34, 40, 255))
d = ImageDraw.Draw(out)

ok = True
for i, name in enumerate(NAMES):
    c, r = i % COLS, i // COLS
    x = PAD + c * (CW + PAD)
    y = PAD + r * (CH + PAD)

    tile = im.crop((i * 16, 0, i * 16 + 16, 16)).resize((16 * S, 16 * S), Image.NEAREST)
    out.paste(tile, (x, y), tile)

    mw, mh, mx, my, _ = meas[i]
    good = (mw == jw[i] and mh == jh[i])
    ok = ok and good and mx == 0 and my == 0
    flag = 'OK' if good else 'MISMATCH'

    d.text((x, y + 16 * S + 2), '#%d %s' % (i, name), fill=(255, 230, 150, 255))
    d.text((x, y + 16 * S + 14), '实测 %dx%d  声明 %dx%d  %s'
           % (mw, mh, jw[i], jh[i], flag),
           fill=(150, 240, 150, 255) if good else (255, 120, 120, 255))
    d.rectangle([x - 1, y - 1, x + 16 * S, y + 16 * S], outline=(90, 96, 110, 255))

out.save('_chk/_tree_trials_icons_preview.png')
print('saved _chk/_tree_trials_icons_preview.png', out.size)
print('全部一致:', ok)
for i, n in enumerate(NAMES):
    mw, mh, mx, my, cnt = meas[i]
    print('%-10s #%d  实测 %2dx%-2d (起点 %d,%d, 不透明 %d)  声明 %2dx%-2d  %s'
          % (n, i, mw, mh, mx, my, cnt, jw[i], jh[i],
             'OK' if (mw == jw[i] and mh == jh[i]) else 'MISMATCH'))
