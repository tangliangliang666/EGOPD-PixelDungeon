#!/bin/bash
# 考验 HOD（荣耀）/ NETZACH（胜利）实装 —— 编译改动文件 → _chk/_javachk2，并可跑行为探针
# 用法：bash _chk/_build_hod_netzach.sh [probe|run|all]
#   probe  只编译 8 个改动文件
#   run    编译并运行探针 HodNetzachProbe（区域换算 / 门控 / 淡出阈值 / 精英叠加 / 计时边界）
#   all    1 + run（默认）
set -u
cd /d/PD || exit 2

JDK="D:/PD/tools/jdk-21.0.12.1+1/bin"
GDX="C:/Users/14675/.gradle/caches/modules-2/files-2.1/com.badlogicgames.gdx/gdx/1.14.0/8accfce8d9313d9ddd23a2c9e315179783085ef2/gdx-1.14.0.jar"
GDXCTRL="C:/Users/14675/.gradle/caches/modules-2/files-2.1/com.badlogicgames.gdx-controllers/gdx-controllers-core/2.2.4/d8a18ff371fb01c1763e9fe5ea050e39d2d66437/gdx-controllers-core-2.2.4.jar"

C="D:/PD/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon"
CP="D:/PD/core/build/classes/java/main;D:/PD/SPD-classes/build/classes/java/main;D:/PD/services/build/classes/java/main;$GDX;$GDXCTRL"

OUT="D:/PD/_chk/_javachk2"
MODE="${1:-all}"

mkdir -p "$OUT"

if [ "$MODE" = "probe" ] || [ "$MODE" = "all" ]; then
  echo "=== [1/2] 编译 8 个改动文件 → _chk/_javachk2 ==="
  "$JDK/javac" -proc:none -Xlint:all -encoding UTF-8 \
    -cp "$CP" -sourcepath "D:/PD/core/src/main/java" -d "$OUT" \
    "$C/Trials.java" \
    "$C/actors/buffs/HodGlory.java" \
    "$C/actors/buffs/ChampionEnemy.java" \
    "$C/actors/Char.java" \
    "$C/actors/hero/Talent.java" \
    "$C/items/armor/glyphs/Stone.java" \
    "$C/sprites/CharSprite.java" \
    "$C/ui/CharHealthIndicator.java" \
    2> _chk/_ie_hn.log
  echo "EXIT=$?  （日志 _chk/_ie_hn.log，GBK）"

  # 旧核验脚本共用的 _chk/_javachk 也要同步刷一遍（否则它们 javap 到的是改动前的 Trials/Char）
  echo "=== [1b/2] 同一批文件也刷进 _chk/_javachk（旧核验共用）==="
  mkdir -p _chk/_javachk
  "$JDK/javac" -proc:none -encoding UTF-8 \
    -cp "D:/PD/_chk/_javachk;$CP" -sourcepath "D:/PD/core/src/main/java" -d "D:/PD/_chk/_javachk" \
    "$C/Trials.java" \
    "$C/actors/buffs/HodGlory.java" \
    "$C/actors/buffs/ChampionEnemy.java" \
    "$C/actors/Char.java" \
    "$C/actors/hero/Talent.java" \
    "$C/items/armor/glyphs/Stone.java" \
    "$C/sprites/CharSprite.java" \
    "$C/ui/CharHealthIndicator.java" \
    2> _chk/_ie_hn_javachk.log
  echo "EXIT=$?  （日志 _chk/_ie_hn_javachk.log，GBK）"
fi

if [ "$MODE" = "run" ] || [ "$MODE" = "all" ]; then
  echo "=== [2/2] 编译并运行探针（_chk/_javachk2 排最前，覆盖旧产物）==="
  "$JDK/javac" -proc:none -encoding UTF-8 \
    -cp "D:/PD/_chk/_javachk2;$CP" -sourcepath "D:/PD/_chk" -d "D:/PD/_chk/_javachk2" \
    _chk/HodNetzachProbe.java 2> _chk/_hnprobe_javac.log
  echo "编译 EXIT=$?  （日志 _chk/_hnprobe_javac.log，GBK）"

  # -Dfile.encoding / -Dstdout.encoding / -Dstderr.encoding 全带：输出里的中文与 ⇒ 才不会是 '?'
  "$JDK/java" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 \
    -cp "D:/PD/_chk/_javachk2;$CP" HodNetzachProbe > _chk/_hodnetzach.out 2> _chk/_hodnetzach.err
  echo "运行 EXIT=$?   （输出 _chk/_hodnetzach.out）"
  tail -n 4 _chk/_hodnetzach.out
fi
