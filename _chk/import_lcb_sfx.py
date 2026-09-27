# -*- coding: utf-8 -*-
"""
边狱 wiki 音效入库流水线（EGOPD）。

用法：
    python _chk/import_lcb_sfx.py                # 处理 _chk/_lcb_audio/*.ogg
    python _chk/import_lcb_sfx.py --dry          # 只测算，不写盘

流程（每个文件）：
    1. silencedetect 定位尾部静音（-50dB / ≥0.1s），裁到「静音起点 + TAIL_S」，
       保留一小段自然衰减尾，避免"啪"地切断。开头静音不裁（实测本批都没有）。
       裁尾的收益：1_1-2 由 3.13s → ~1.9s，回到项目「≤2s」规格内，同时减重。
    2. 解码为 44.1kHz / 单声道 wav。
    3. volumedetect 测 GAIN 前的 mean_volume / max_volume。
    4. 计算线性增益 gain = min(TARGET_MEAN - mean, PEAK_CEIL - max)
       —— 双重约束：均值拉到 TARGET_MEAN（与其他音效听感一致），
          同时峰值不超过 PEAK_CEIL（防削峰、留安全余量）。
    5. 以该增益编码为 mp3，规格对齐项目现有音效：44.1kHz / 单声道 / 64kbps。
    6. **闭环修正**：有损编码会产生过冲（intersample peak），实测出现过一个文件
       WAV 峰值 -1.5dB、解码后却到 -0.1dB。因此编码后**以解码后实测为准**再校一次：
       若 peak > PEAK_LIMIT（或均值偏离目标），按差值追加衰减并重编码，最多 MAX_PASSES 轮。

为什么必须做响度归一：本批素材来自不同录制源，均值散布在 -14.4 ~ -20.1 dB
（相差近 6 dB）。技能1 那三条是**同一事件随机三选一**，不做归一会让每次攻击
忽大忽小；技能8-3 更是比最轻的重 5.7 dB。
"""
import argparse
import os
import re
import subprocess
import sys

FFMPEG = r"C:\Program Files\Kuyo\ffmpeg.exe"

SRC_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_lcb_audio")
DST_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                       "core", "src", "main", "assets", "sounds")

# ---- 归一目标（对齐项目现有音效：mean -18.9~-22.6dB / peak -2.5~-3.8dB）----
TARGET_MEAN = -20.0   # 均值目标，dB
PEAK_CEIL = -1.5      # WAV 阶段峰值上限，dB（编码前）
PEAK_LIMIT = -1.0     # 解码后峰值上限，dB（硬红线，超了就追加衰减）
MAX_PASSES = 3        # 闭环修正最多轮数

# ---- 峰值顶格补救（限幅器）----
# 有些素材（打击音尤其常见）本身录到满刻度 0dB，此时 gain 被峰值约束卡死、
# 均值根本到不了目标（实测 Thumbfather_3_1-1 只能到 -23.5dB，比同组其他音轻 3.5dB）。
# 解法：先用 alimiter 压掉瞬态峰换取增益余量。alimiter 自带补偿增益，
# 设 limit 为阈值、level_out 为输出峰值，一次即可把均值抬到目标附近。
CHAIN_TRIGGER = 0.3   # 「目标均值增益」比「峰值允许增益」大出此值即判定被卡住
CHAIN_START = 0.75    # 限幅阈值起始（线性）
CHAIN_OUT = 0.84      # 限幅后输出峰值（≈ -1.5dB，对齐 PEAK_CEIL）
CHAIN_PASSES = 5      # 阈值最多下调次数（每次 -0.1）
CHAIN_CREST = 1.0     # 压缩后允许的 crest 余量（dB）

TAIL_S = 0.15         # 裁尾后保留的衰减尾，秒
SILENCE_DB = -50      # 静音判定门限
SILENCE_MIN = 0.1     # 静音最短时长

BITRATE = "64k"
SAMPLE_RATE = 44100

# 源文件名 -> 入库文件名
MAP = {
    "9SV-BAT17-03.ogg":    "indexfather_liberation_1.mp3",
    "9SV-BAT17-04.ogg":    "indexfather_liberation_2.mp3",
    "9SV-BAT17-05.ogg":    "indexfather_liberation_3.mp3",
    "Indexfather_1_0.ogg": "indexfather_skill1_1.mp3",
    "Indexfather_1_1-1.ogg": "indexfather_skill1_2.mp3",
    "Indexfather_1_1-2.ogg": "indexfather_skill1_3.mp3",
    "Indexfather_8_3.ogg": "indexfather_skill8_3.mp3",
}


def run(args):
    return subprocess.run([FFMPEG, "-hide_banner", "-nostdin"] + args,
                          capture_output=True, text=True, encoding="utf-8",
                          errors="replace")


def probe_duration(path):
    m = re.search(r"Duration:\s*(\d+):(\d+):([\d.]+)", run(["-i", path]).stderr)
    if not m:
        return None
    return int(m.group(1)) * 3600 + int(m.group(2)) * 60 + float(m.group(3))


def last_silence_start(path):
    """返回最后一个「一直持续到文件末尾」的静音起点；没有则 None。"""
    txt = run(["-i", path, "-af",
               "silencedetect=noise=%ddB:d=%s" % (SILENCE_DB, SILENCE_MIN),
               "-f", "null", "-"]).stderr
    starts = [float(x) for x in re.findall(r"silence_start:\s*([\d.]+)", txt)]
    duration = probe_duration(path)
    if not starts or duration is None:
        return None
    # 只有最后一处静音贴近文件末尾（无 silence_end，或 end≈duration）才算尾部静音
    ends = [float(x) for x in re.findall(r"silence_end:\s*([\d.]+)", txt)]
    if ends and abs(ends[-1] - duration) > 0.05 and len(ends) >= len(starts):
        return None
    return starts[-1]


def volume_stats(path):
    txt = run(["-i", path, "-af", "volumedetect", "-f", "null", "-"]).stderr
    mean = re.search(r"mean_volume:\s*(-?[\d.]+) dB", txt)
    peak = re.search(r"max_volume:\s*(-?[\d.]+) dB", txt)
    if not mean or not peak:
        return None, None
    return float(mean.group(1)), float(peak.group(1))


def main():
    global TARGET_MEAN    # 允许 --target-mean 按批覆盖归一目标（见下方注释）

    ap = argparse.ArgumentParser()
    ap.add_argument("--dry", action="store_true", help="只测算不写盘")
    ap.add_argument("--src-dir", default=None,
                    help="source dir, default _chk/_lcb_audio")
    ap.add_argument("--map", action="append", default=[], metavar="SRC=DST",
                    help="custom mapping (repeatable), replaces built-in MAP")
    ap.add_argument("--target-mean", type=float, default=TARGET_MEAN,
                    help="mean volume target in dB (default %(default)s)")
    opt = ap.parse_args()

    # 语音类素材的均值目标要更高（实测 -15dB 合适）：人声内部有自然停顿，
    # 那些静音段会把 mean_volume 拉低 —— 按音效的 -20dB 归一后，说话段的
    # 实际电平明显低于音效，玩家听感就是「语音偏小」。故允许按批覆盖目标。
    TARGET_MEAN = opt.target_mean

    if not os.path.isfile(FFMPEG):
        sys.exit("找不到 ffmpeg: %s" % FFMPEG)

    src_dir = opt.src_dir or SRC_DIR
    if opt.map:
        mapping = {}
        for kv in opt.map:
            s, d = kv.split("=", 1)
            mapping[s] = d
    else:
        mapping = dict(MAP)
    if not os.path.isdir(src_dir):
        sys.exit("找不到源目录: %s" % src_dir)

    print("%-24s %-9s %-9s %-9s %-9s %s" %
          ("源文件", "原时长", "裁后时长", "增益dB", "裁后峰值", "入库名"))
    print("-" * 96)

    for src, dst in mapping.items():
        path = os.path.join(src_dir, src)
        if not os.path.isfile(path):
            print("%-24s 缺失，跳过" % src)
            continue

        raw_dur = probe_duration(path) or 0.0
        cut = last_silence_start(path)
        out_dur = min(raw_dur, cut + TAIL_S) if cut else raw_dur

        tmp_wav = os.path.join(src_dir, "_tmp_" + dst.replace(".mp3", ".wav"))
        r = run(["-y", "-i", path, "-t", "%.3f" % out_dur,
                 "-ar", str(SAMPLE_RATE), "-ac", "1", "-c:a", "pcm_s16le", tmp_wav])
        if not os.path.isfile(tmp_wav):
            print("%-24s 解码失败: %s" % (src, r.stderr.strip().splitlines()[-1:]))
            continue

        mean, peak = volume_stats(tmp_wav)
        if mean is None:
            print("%-24s 测响度失败" % src)
            os.remove(tmp_wav)
            continue

        # 峰值已顶格 → 单纯衰减到不了目标均值（gain 被峰值约束卡死）。
        # 先用限幅器压掉瞬态峰，换取增益余量；alimiter 自带补偿增益，故一步到位。
        chained = False
        if TARGET_MEAN - mean > PEAK_CEIL - peak + CHAIN_TRIGGER:
            lim_wav = os.path.join(src_dir, "_lim_" + dst.replace(".mp3", ".wav"))
            lim_set = CHAIN_START
            ok = False
            for _ in range(CHAIN_PASSES):
                run(["-y", "-i", tmp_wav, "-af",
                     "alimiter=limit=%.2f:level_out=%.2f:attack=2:release=60"
                     % (lim_set, CHAIN_OUT),
                     "-ar", str(SAMPLE_RATE), "-ac", "1", "-c:a", "pcm_s16le", lim_wav])
                m2, p2 = volume_stats(lim_wav)
                if m2 is None:
                    break
                ok = True
                if (p2 - m2) <= (PEAK_CEIL - TARGET_MEAN) + CHAIN_CREST:
                    break                     # 动态已压够，可以归一到目标均值
                lim_set = round(lim_set - 0.1, 2)
            if ok:
                os.remove(tmp_wav)
                os.replace(lim_wav, tmp_wav)
                mean, peak = volume_stats(tmp_wav)
                chained = True

        gain = min(TARGET_MEAN - mean, PEAK_CEIL - peak)
        dst_path = os.path.join(DST_DIR, dst)

        if not opt.dry:
            # 闭环：以「解码后实测」为准反复修正，抵消有损编码的过冲
            applied = gain
            passes = 0
            while passes < MAX_PASSES:
                passes += 1
                r = run(["-y", "-i", tmp_wav, "-af", "volume=%.2fdB" % applied,
                         "-ar", str(SAMPLE_RATE), "-ac", "1",
                         "-c:a", "libmp3lame", "-b:a", BITRATE, dst_path])
                if not os.path.isfile(dst_path):
                    print("%-24s 编码失败: %s" % (src, r.stderr.strip()[-200:]))
                    break
                m2, p2 = volume_stats(dst_path)
                if m2 is None:
                    break
                extra = min(TARGET_MEAN - m2, PEAK_LIMIT - p2)
                if extra >= -0.3:   # 已达标（-0.3dB 内不折腾）
                    break
                applied += extra
            gain = applied
            if passes > 1:
                dst += "  (%d轮)" % passes
            fm, fp = volume_stats(dst_path)
            if fm is not None:
                mean, peak = fm, fp   # 报表改为展示解码后实测
        os.remove(tmp_wav)

        print("%-24s %-9.2f %-9.2f %-9.2f %-9.2f %s%s" %
              (src, raw_dur, out_dur, gain,
               peak if not opt.dry else peak + gain,
               dst, "  [限幅]" if chained else ""))

    print("-" * 96)
    print("目标: 均值 %.0f dB / WAV峰值上限 %.1f / 解码后峰值红线 %.1f   "
          "(项目现有音效: 均值 -18.9~-22.6 / 峰值 -2.5~-3.8)" %
          (TARGET_MEAN, PEAK_CEIL, PEAK_LIMIT))


if __name__ == "__main__":
    main()
