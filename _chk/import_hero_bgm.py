# -*- coding: utf-8 -*-
"""
把四位角色专属 BGM 从素材（Downloads 下的中文名文件）转码为项目规格，
放入 core/src/main/assets/music/。

用法：python _chk/import_hero_bgm.py [--bitrate 96k] [--dry]

规格：44100Hz / 立体声 / libmp3lame，与原版 `*.ogg` 同级用途。
注意：Music 是**流式解码**（gdx Music），运行时内存占用与文件体积无关，
      这里控制的是 APK 体积与安装后占用。
"""
import os
import subprocess
import sys

# 本机 ffmpeg（gyan.dev 完整版，带 libmp3lame）；剪映自带的不能用（--disable-ffmpeg）
FFMPEG = r"C:\Program Files\Kuyo\ffmpeg.exe"
SRC_DIR = r"C:\Users\14675\Downloads"
DST_DIR = r"D:\PD\core\src\main\assets\music"

# (源文件, 目标文件) —— 目标名与 Assets.Music 中的常量一一对应
MAP = [
    ("食指.ogg.mp3",     "hero_oracle.mp3"),          # ORACLE        食指 · 神谕代行者
    ("环指.ogg.mp3",     "hero_ring_master.mp3"),     # RING_MASTER   环指大师
    ("中指.ogg.mp3",     "hero_middle_finger.mp3"),   # MIDDLE_FINGER 中指 · 长兄
    ("拇指.ogg.mp3",     "hero_valencina.mp3"),       # VALENCINA     拇指 · 前二老板
]


def probe_duration(path):
    """返回时长（秒），拿不到就返回 None。"""
    try:
        out = subprocess.run(
            [FFMPEG, "-hide_banner", "-i", path],
            capture_output=True, text=True, encoding="utf-8", errors="replace"
        ).stderr
        for line in out.splitlines():
            line = line.strip()
            if line.startswith("Duration:"):
                hh, mm, ss = line.split(",")[0].split("Duration:")[1].strip().split(":")
                return int(hh) * 3600 + int(mm) * 60 + float(ss)
    except Exception:
        pass
    return None


def main():
    args = sys.argv[1:]
    bitrate = "96k"
    dry = False
    if "--bitrate" in args:
        bitrate = args[args.index("--bitrate") + 1]
    if "--dry" in args:
        dry = True

    if not os.path.isfile(FFMPEG):
        print("!! 找不到 ffmpeg：%s" % FFMPEG)
        return 1
    os.makedirs(DST_DIR, exist_ok=True)

    print("转码规格：44100Hz / 立体声 / libmp3lame %s" % bitrate)
    print("目标目录：%s" % DST_DIR)
    print("-" * 62)

    total_src = total_dst = 0
    rows = []
    for src_name, dst_name in MAP:
        src = os.path.join(SRC_DIR, src_name)
        dst = os.path.join(DST_DIR, dst_name)
        if not os.path.isfile(src):
            print("!! 缺少素材：%s" % src)
            return 1

        if not dry:
            r = subprocess.run(
                [FFMPEG, "-y", "-hide_banner", "-loglevel", "error",
                 "-i", src, "-vn", "-ac", "2", "-ar", "44100",
                 "-c:a", "libmp3lame", "-b:a", bitrate, dst],
                capture_output=True, text=True, encoding="utf-8", errors="replace"
            )
            if r.returncode != 0:
                print("!! 转码失败 %s：%s" % (src_name, r.stderr.strip()[:200]))
                return 1

        s_size = os.path.getsize(src)
        d_size = os.path.getsize(dst) if os.path.isfile(dst) else 0
        dur = probe_duration(dst) if os.path.isfile(dst) else probe_duration(src)
        total_src += s_size
        total_dst += d_size
        rows.append((dst_name, s_size, d_size, dur))

    for name, s_size, d_size, dur in rows:
        print("%-24s %7.2f MB -> %6.2f MB   %s   %s"
              % (name, s_size / 1048576, d_size / 1048576,
                 ("%.1fs" % dur) if dur else "?", "压缩 %.0f%%" % (100 - d_size * 100.0 / s_size)))
    print("-" * 62)
    print("合计：%.2f MB -> %.2f MB（省下 %.2f MB）"
          % (total_src / 1048576, total_dst / 1048576,
             (total_src - total_dst) / 1048576))
    return 0


if __name__ == "__main__":
    sys.exit(main())
