# 把「容器不受影响」这半句从 YESOD 考验描述的**英文**版里删掉。
#
# 背景（2026-09-26）：用户手工重写过 trials.yesod_desc 的中文版，删掉了
# 「_宝箱、骷髅堆等容器_与_背包内_的显示不受影响。」这半句（因为容器现在也变问号了），
# 但英文那半句当时没跟着删 —— 英文环境下的考验面板会显示一句与实际行为相反的说明。
# 本脚本＝把用户已经做过的那次改动，原样补到英文版上。只动英文，中文一字不碰。
#
# 幂等：若目标句已不存在且新句已在，直接跳过。
# 行尾：只有字符串内部替换，不产生任何换行 ⇒ CRLF 数量必须逐个不变（脚本自己断言）。
# 备份：_chk/_bak_yesoddescen/misc.properties.bak

import io
import os
import shutil

ROOT = 'D:/PD'
EN = os.path.join(ROOT, 'core/src/main/assets/messages/misc/misc.properties')
BAK_DIR = os.path.join(ROOT, '_chk/_bak_yesoddescen')

OLD = '_Containers (chests, skull piles and the like)_ and the _inventory_ are unaffected.'
NEW = '_Items in the inventory_ are unaffected.'


def main():
    raw = io.open(EN, 'rb').read()
    crlf_before = raw.count(b'\r\n')
    txt = raw.decode('utf-8')

    n_old = txt.count(OLD)
    n_new = txt.count(NEW)
    print('替换前：OLD 出现 %d 次 / NEW 出现 %d 次' % (n_old, n_new))
    if n_old == 0 and n_new == 0:
        raise AssertionError('旧句与新句都不在 —— 文件被人改过，停下来人工看一眼')
    if n_old == 0 and n_new == 1:
        print('[skip] 已是目标状态（幂等复跑）')
        return
    assert n_old == 1, '旧句出现 %d 次（应为恰好 1 次）' % n_old
    assert n_new == 0, '新句已经存在，别重复插'

    # 只允许在 trials.yesod_desc 那一行里替换
    line = [l for l in txt.split('\r\n') if l.startswith('trials.yesod_desc=')]
    assert len(line) == 1, 'trials.yesod_desc 出现 %d 次' % len(line)
    assert OLD in line[0], '旧句不在 trials.yesod_desc 这一行里（可能在别处）'

    os.makedirs(BAK_DIR, exist_ok=True)
    bak = os.path.join(BAK_DIR, 'misc.properties.bak')
    if not os.path.exists(bak):
        shutil.copyfile(EN, bak)
        print('[OK] 备份 → %s' % bak)

    out = txt.replace(OLD, NEW, 1)
    assert out.count(NEW) == 1
    assert out.count(OLD) == 0

    data = out.encode('utf-8')
    assert data.count(b'\r\n') == crlf_before, \
        'CRLF 数变了（%d → %d）' % (crlf_before, data.count(b'\r\n'))
    assert data.count(b'\n') - data.count(b'\r\n') == 0, '出现裸 LF'
    assert not data.startswith(b'\xef\xbb\xbf'), '出现 BOM'

    io.open(EN, 'wb').write(data)
    print('[OK] %s：CRLF %d 不变；字节 %d → %d' % (EN, crlf_before, len(raw), len(data)))
    for l in out.split('\r\n'):
        if l.startswith('trials.yesod_desc='):
            print('     新值：%s' % l)


if __name__ == '__main__':
    main()
