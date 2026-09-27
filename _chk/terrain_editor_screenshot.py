# -*- coding: utf-8 -*-
"""用系统 Edge 无头截屏工具栏（纯 Node 逻辑核验抓不到 UI 层问题）"""
import os, subprocess, sys, time
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EDGE = r'C:/Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
if not os.path.exists(EDGE):
    EDGE = r'C:/Program Files\Microsoft\Edge\Application\msedge.exe'
URL = 'file:///' + os.path.join(ROOT, 'tools', 'terrain-editor', 'index.html').replace('\\', '/')
OUT = os.path.join(ROOT, '_chk', '_te_shot.png')
if os.path.exists(OUT):
    os.remove(OUT)
cmd = [EDGE, '--headless=new', '--disable-gpu', '--no-sandbox', '--hide-scrollbars',
       '--virtual-time-budget=9000', '--window-size=1500,1250',
       '--screenshot=' + OUT, URL]
r = subprocess.run(cmd, capture_output=True, timeout=120)
print('edge rc =', r.returncode)
if os.path.exists(OUT):
    print('截图已生成 %s (%d B)' % (OUT, os.path.getsize(OUT)))
else:
    print('❌ 没生成截图'); print(r.stderr.decode('utf-8', 'replace')[:2000]); sys.exit(1)
