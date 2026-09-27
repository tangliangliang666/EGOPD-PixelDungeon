import os
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
for p in ['core/src/main/assets/messages/misc/misc_zh.properties',
          'core/src/main/assets/messages/misc/misc.properties']:
    raw = open(os.path.join(ROOT, p), 'rb').read()
    for k in (b'trials.chesed_desc=', b'trials.gebura_desc='):
        i = raw.find(k); j = raw.find(b'\r\n', i)
        line = raw[i:j].decode('utf-8')
        print('%-22s %-22s len=%3d  literal-bslash-n=%d  real-NL=%d  pct=%d'
              % (os.path.basename(p), k.decode(), len(line),
                 line.count(chr(92) + 'n'), line.count(chr(10)), line.count('%')))
