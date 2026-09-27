# -*- coding: utf-8 -*-
"""从 javac 日志里按文件统计诊断条数（本批改动文件的告警要看清）。"""
import re, sys, collections

LOG = r"D:\PD\_chk\_ie_killfix.log"
t = open(LOG, "rb").read().decode("gbk", "replace")

files = collections.OrderedDict()
cur = None
for l in t.splitlines():
    m = re.match(r"^(\S+\.java):(\d+):\s*(警告|错误)", l)
    if m:
        cur = (m.group(1).replace("\\", "/").split("/")[-1], m.group(3))
        files.setdefault(cur[0], []).append((m.group(2), m.group(3), l.strip()))
    elif cur and l.strip().startswith(("缺少", "其中", "T扩展")):
        pass

if not files:
    print("没有『警告/错误』级别的诊断行")
for k, v in files.items():
    kinds = collections.Counter(x[1] for x in v)
    print("%-28s %s" % (k, dict(kinds)))
    for ln, kind, raw in v[:6]:
        print("      %s %s: %s" % (kind, ln, raw))
