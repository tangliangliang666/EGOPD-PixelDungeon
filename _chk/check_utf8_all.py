# -*- coding: utf-8 -*-
"""全仓源码编码体检：揪出「Edit 工具写坏」的两类痕迹。

背景（2026-09-17 踩到）：用记事本式工具改 .java 时，工具可能
  ① 把新写入的注释整行按**平台默认编码（GBK）**落盘；
  ② 把**行尾 3 字节汉字的最后一字节**clobber 成 '?'（如 衣 E8A1A3 ⇒ E8A1 3F）。
两类都会让文件**不再是合法 UTF-8**，Gradle 的 `options.encoding='UTF-8'` 直接编译失败。

用法：python _chk/check_utf8_all.py [根目录，默认 core/src/main/java] [扩展名，逗号分隔，默认 .java,.properties]
  例：python _chk/check_utf8_all.py docs .md            # 检查 docs/*.md 的中文是否被写坏
退出码 0 = 全部合法；1 = 有文件被写坏（会打印出错字节的上下文）。
"""
import io, os, re, sys

root = sys.argv[1] if len(sys.argv) > 1 else "core/src/main/java"
if len(sys.argv) > 2:
    EXTS = tuple(e if e.startswith('.') else '.' + e for e in sys.argv[2].split(',') if e.strip())
else:
    EXTS = ('.java', '.properties')

bad, suspicious, total = [], [], 0
for dirpath, _dirs, files in os.walk(root):
    for f in files:
        if not f.endswith(EXTS):
            continue
        p = os.path.join(dirpath, f)
        total += 1
        raw = io.open(p, 'rb').read()
        try:
            text = raw.decode('utf-8')
        except UnicodeDecodeError as e:
            i = e.start
            seg = raw[max(0, i - 40):i + 12]
            bad.append((p, str(e), seg))
            continue
        if '\ufffd' in text:
            suspicious.append((p, 'U+FFFD'))
        # 行尾/词中「汉字紧跟 ?」是 ② 的残留特征（合法 UTF-8 时也能看出来）
        for m in re.finditer(r'[\u4e00-\u9fff]\?', text):
            line = text.count('\n', 0, m.start()) + 1
            ctx = text[max(0, m.start() - 24):m.end() + 12].replace('\n', '\\n').replace('\r', '')
            suspicious.append((p, 'line %d: ...%s' % (line, ctx)))

print("扫描 %d 个文件" % total)
if bad:
    print("\n[FATAL] 非法 UTF-8（必须先修复，否则编译不过）：")
    for p, e, seg in bad:
        print("  %s\n    %s\n    bytes ≈ %r" % (p, e, seg))
if suspicious:
    print("\n[WARN] 可疑（汉字后紧跟 '?'/替换字符，人工确认是否被 clobber）：")
    for p, w in suspicious[:40]:
        print("  %s | %s" % (p, w))
if not bad and not suspicious:
    print("OK：全部为合法 UTF-8，且无「汉字+?」可疑痕迹")
sys.exit(1 if bad else 0)
