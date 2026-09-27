# javac 日志是 GBK，直接 cat 是乱码。用法：
#   python _chk/filter_javac_log.py Icons.java [日志路径=_chk/_ie.log]
# 输出：本文件命中的告警/错误行（判据：本文件 0 行 ⇒ 通过），以及日志总行数供对照。
import sys

src = sys.argv[1]
log = sys.argv[2] if len(sys.argv) > 2 else '_chk/_ie.log'
raw = open(log, 'rb').read()
text = raw.decode('gbk', 'replace')
lines = text.splitlines()
hit = [l for l in lines if src in l]
print('日志 %s：总行数 %d，非空行 %d' % (log, len(lines), len([l for l in lines if l.strip()])))
print('命中 %s 的行数 = %d' % (src, len(hit)))
for l in hit:
    print('  ' + l)
if '错误:' in text or 'error:' in text:
    print('!! 日志里出现「错误:」/「error:」——把含该关键词的前 20 行打出来：')
    for l in lines:
        if '错误:' in l or 'error:' in l:
            print('  ' + l)
            if lines.index(l) > 0 and len([x for x in lines[:lines.index(l)] if '错误:' in x or 'error:' in x]) > 20:
                break
