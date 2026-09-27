# -*- coding: utf-8 -*-
"""放大现有树渲染图，并在每个节点中心画十字，用来判断图标是否真的居中。

产出：_chk/_icon_center_zoom.png
"""
import os
from PIL import Image, ImageDraw

BASE = r'D:/PD'
SRC = os.path.join(BASE, '_chk/_tree_draft5_enabled.png')
OUT = os.path.join(BASE, '_chk/_icon_center_zoom.png')

NODES = {'Keter': (340, 84), 'Hokma': (426.6, 134), 'Binah': (253.4, 134),
         'Chesed': (426.6, 234), 'Gebura': (253.4, 234), 'Tiphereth': (340, 284),
         'Netzach': (426.6, 334), 'Hod': (253.4, 334), 'Yesod': (340, 384),
         'Malkuth': (340, 484)}
BOX = (200, 40, 480, 210)
S = 4

im = Image.open(SRC).convert('RGB')
crop = im.crop(BOX).resize(((BOX[2] - BOX[0]) * S, (BOX[3] - BOX[1]) * S), Image.NEAREST)
d = ImageDraw.Draw(crop)
for nm, (x, y) in NODES.items():
    if not (BOX[0] < x < BOX[2] and BOX[1] < y < BOX[3]):
        continue
    gx, gy = (x - BOX[0]) * S, (y - BOX[1]) * S
    d.line([(gx - 26, gy), (gx + 26, gy)], fill=(0x0F, 0x6E, 0x56), width=2)
    d.line([(gx, gy - 26), (gx, gy + 26)], fill=(0x0F, 0x6E, 0x56), width=2)
    d.text((gx + 6, gy + 6), nm, fill=(0x0F, 0x6E, 0x56))
crop.save(OUT)
print('[OK] %s  (%dx%d)  绿十字 = 节点中心（图标理论中心）' % (OUT, crop.width, crop.height))
