# -*- coding: utf-8 -*-
"""
音频资源格式自检（EGOPD）。

用法：
    python _chk/audio_format_check.py                 # 检查 core/src/main/assets/sounds
    python _chk/audio_format_check.py <目录>          # 检查指定目录

作用：裸解析 MP3 帧头，打印采样率 / 声道 / 码率 / 时长 / 体积，并给出与项目现有
音效规格（44.1kHz、单声道、~64kbps 以下、≤2 秒）的偏差提示。
新增「依旧果冻人」等自定义音效后跑一遍，避免放错格式（.wav/.ogg）或超大文件
（libGDX 的 Sound 是整段解码进内存的，长音频会直接吃内存）。
"""
import os
import struct
import sys

# MPEG1 Layer3 码率表 / 采样率表
BITRATES = {1: 32, 2: 40, 3: 48, 4: 56, 5: 64, 6: 80, 7: 96, 8: 112,
            9: 128, 10: 160, 11: 192, 12: 224, 13: 256, 14: 320}
SAMPLE_RATES = {0: 44100, 1: 48000, 2: 32000}

MAX_SECONDS = 2.0
OK_EXTS = ('.mp3',)


def frames_info(data):
    """返回 (偏移, 码率kbps, 采样率, 声道, 是否VBR)。找不到合法帧头返回 None。"""
    i = 0
    if data[:3] == b'ID3':
        size = (data[6] & 0x7f) << 21 | (data[7] & 0x7f) << 14 | (data[8] & 0x7f) << 7 | (data[9] & 0x7f)
        i = 10 + size
    while i < len(data) - 4:
        if data[i] == 0xFF and (data[i + 1] & 0xE0) == 0xE0:
            head = struct.unpack('>I', data[i:i + 4])[0]
            version = (head >> 19) & 3
            layer = (head >> 17) & 3
            br = (head >> 12) & 0xF
            sr = (head >> 10) & 3
            ch = (head >> 6) & 3
            if version == 3 and layer == 1 and br in BITRATES and sr in SAMPLE_RATES:
                vbr = (data.find(b'Xing', i, i + 200) != -1
                       or data.find(b'Info', i, i + 200) != -1)
                return i, BITRATES[br], SAMPLE_RATES[sr], ('mono' if ch == 3 else 'stereo'), vbr
        i += 1
    return None


def check(directory):
    if not os.path.isdir(directory):
        print('目录不存在：%s' % directory)
        return 1
    problems = []
    names = sorted(os.listdir(directory))
    print('目录：%s（共 %d 个文件）' % (directory, len(names)))
    print('%-22s %8s %7s %8s %7s %8s  %s' %
          ('文件', '采样率', '声道', '码率', '时长', '体积', '备注'))
    for name in names:
        path = os.path.join(directory, name)
        if not os.path.isfile(path):
            continue
        size = os.path.getsize(path)
        ext = os.path.splitext(name)[1].lower()
        notes = []
        if ext not in OK_EXTS:
            notes.append('扩展名不是 .mp3（本项目音效统一 mp3）')
        data = open(path, 'rb').read()
        info = frames_info(data)
        if info is None:
            notes.append('没有找到合法 MPEG1 Layer3 帧头')
            print('%-22s %8s %7s %8s %7s %8dB  %s' % (name, '-', '-', '-', '-', size, '；'.join(notes)))
            problems.append((name, notes))
            continue
        off, kbps, sr, ch, vbr = info
        duration = (len(data) - off) * 8 / (kbps * 1000.0)
        if duration > MAX_SECONDS:
            notes.append('时长 %.2fs 超过 %.1fs' % (duration, MAX_SECONDS))
        if sr != 44100:
            notes.append('采样率不是 44100')
        if ch == 'stereo':
            notes.append('立体声（本项目音效多为单声道）')
        print('%-22s %7dHz %7s %6dkbps %6.2fs %7dB  %s' %
              (name, sr, ch, kbps, duration, size, '；'.join(notes)))
        if notes:
            problems.append((name, notes))
    print()
    if problems:
        print('有 %d 个文件需要注意：' % len(problems))
        for name, notes in problems:
            print('  - %s：%s' % (name, '；'.join(notes)))
        return 1
    print('全部符合规格。')
    return 0


if __name__ == '__main__':
    target = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
        'core', 'src', 'main', 'assets', 'sounds')
    sys.exit(check(target))
