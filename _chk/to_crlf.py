# -*- coding: utf-8 -*-
"""把指定文本文件统一成 CRLF（幂等）；顺带报告是否含裸 LF 与是否 UTF-8 合法。
用法：python to_crlf.py <file> [file...]"""
import sys


def main(argv):
    for p in argv[1:]:
        b = open(p, "rb").read()
        crlf0 = b.count(b"\r\n")
        lf0 = b.count(b"\n") - crlf0
        # 先统一成 LF 再统一成 CRLF，避免出现 \r\r\n
        nb = b.replace(b"\r\n", b"\n").replace(b"\n", b"\r\n")
        if nb != b:
            open(p, "wb").write(nb)
        crlf1 = nb.count(b"\r\n")
        lf1 = nb.count(b"\n") - crlf1
        try:
            nb.decode("utf-8")
            enc = "utf8-ok"
        except Exception as e:
            enc = "UTF8-BAD:%s" % e
        print("%s  CRLF %d->%d  bareLF %d->%d  %s" % (p, crlf0, crlf1, lf0, lf1, enc))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
