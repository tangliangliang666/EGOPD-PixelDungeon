# 过滤 javac 日志（GBK）中属于本次改动文件的行
import sys

FILES = [
    'Trials.java', 'WndTrials.java', 'Assets.java', 'Icons.java', 'SPDSettings.java',
    'Dungeon.java', 'GamesInProgress.java', 'Rankings.java', 'MenuPane.java', 'WndGame.java',
    'WndGameInProgress.java', 'WndVictoryCongrats.java', 'WndRanking.java', 'HeroSelectScene.java',
]

log = open('_chk/_ie.log', 'rb').read().decode('gbk', 'replace')
lines = log.splitlines()
print('日志总行数 =', len(lines))
hit = [l for l in lines if any(f in l for f in FILES)]
print('本批文件相关行数 =', len(hit))
for l in hit:
    print('  ' + l)
err = [l for l in lines if ('错误' in l or 'error:' in l)]
print('全日志错误行数 =', len(err))
for l in err[:20]:
    print('  E ' + l)
