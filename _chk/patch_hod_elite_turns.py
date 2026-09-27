# -*- coding: utf-8 -*-
"""HOD（荣耀）精英化阈值调整：150 → 普通 300 / Boss 450。

覆盖：源码（Trials.java / HodGlory.java）、文本（misc{,_zh}.properties）、
      行为探针（HodNetzachProbe.java）、核验脚本（verify_hod_netzach.py）。

幂等：每条替换都断言「旧锚点恰好出现 1 次」；已是目标值时打 [SKIP]。
备份：首次改某文件前整份拷到 _chk/_bak_hod_elite_turns/<原相对路径>。
"""
import os, shutil

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BAK  = os.path.join(ROOT, '_chk', '_bak_hod_elite_turns')

def rd(rel):
    with open(os.path.join(ROOT, rel), 'rb') as f:
        return f.read()

def wr(rel, data):
    p = os.path.join(ROOT, rel)
    b = os.path.join(BAK, rel)
    if not os.path.exists(b):
        os.makedirs(os.path.dirname(b), exist_ok=True)
        shutil.copy2(p, b)
    with open(p, 'wb') as f:
        f.write(data)

def sub1(d, old, new, tag):
    o = old.encode('utf-8')
    n = new.encode('utf-8')
    c = d.count(o)
    if c == 0:
        if n in d:
            print('  [SKIP] %s' % tag)
            return d
        raise SystemExit('  [FAIL] %s：锚点未找到' % tag)
    if c != 1:
        raise SystemExit('  [FAIL] %s：锚点出现 %d 次（应为 1）' % (tag, c))
    print('  [OK]   %s' % tag)
    return d.replace(o, n, 1)

J = 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/'

# ---------------------------------------------------------------- 1 Trials.java
print('== 1/5 Trials.java')
F = J + 'Trials.java'
d = rd(F)
d = sub1(d,
    '\t//  · 某个敌方单位**存活超过 HOD_ELITE_TURNS 回合**即获得一次**随机精英效果**（不可叠加）。',
    '\t//  · 某个敌方单位**存活超过 HOD_ELITE_TURNS 回合**（Boss 走 HOD_ELITE_TURNS_BOSS）即获得一次\n'
    '\t//    **随机精英效果**（不可叠加）。',
    'T1 段注释：写明 Boss 走另一档')

d = sub1(d,
    '\t/** HOD：敌方存活**超过**这么多回合后获得随机精英效果。 */\n'
    '\tpublic static final int HOD_ELITE_TURNS\t= 150;',
    '\t/** HOD：**普通**敌方存活**超过**这么多回合后获得随机精英效果。 */\n'
    '\tpublic static final int HOD_ELITE_TURNS\t= 300;\n'
    '\n'
    '\t/** HOD：**Boss** 敌方（{@code Char.Property.BOSS}）存活**超过**这么多回合后获得随机精英效果。 */\n'
    '\tpublic static final int HOD_ELITE_TURNS_BOSS\t= 450;\n'
    '\n'
    '\t/**\n'
    '\t * HOD 的晋升阈值：Boss（{@code Char.Property.BOSS}）走 {@link #HOD_ELITE_TURNS_BOSS}，\n'
    '\t * 其余（MINIBOSS / 普通怪 / 召唤物）一律走 {@link #HOD_ELITE_TURNS}。\n'
    '\t * **唯一取数收口** —— 计时器 {@code HodGlory} 只调它，别在两处各写一遍分支。\n'
    '\t */\n'
    '\tpublic static int hodEliteTurns( Mob mob ){\n'
    '\t\tif (mob != null && mob.properties().contains( Char.Property.BOSS )) return HOD_ELITE_TURNS_BOSS;\n'
    '\t\treturn HOD_ELITE_TURNS;\n'
    '\t}',
    'T2 常量 150→300 ＋ 新增 BOSS 450 ＋ 收口 hodEliteTurns')
wr(F, d)

# ------------------------------------------------------------- 2 HodGlory.java
print('== 2/5 HodGlory.java')
F = J + 'actors/buffs/HodGlory.java'
d = rd(F)
d = sub1(d,
    ' * <p>计时口径严格照需求「**存活**超过 150 回合」：按回合累加，只有在「是敌方阵营 + 活着 + 本考验仍开着」\n'
    ' * 时才推进；一旦不再适用就**清零**（不再算存活）。数满 {@link Trials#HOD_ELITE_TURNS} 之后调用',
    ' * <p>计时口径严格照需求「**存活**超过 N 回合」（普通 300 / Boss 450，见 {@link Trials#hodEliteTurns}）：\n'
    ' * 按回合累加，只有在「是敌方阵营 + 活着 + 本考验仍开着」\n'
    ' * 时才推进；一旦不再适用就**清零**（不再算存活）。数满 {@link Trials#hodEliteTurns} 之后调用',
    'G1 类注释：口径更新')
d = sub1(d,
    '\t\tif (++turns > Trials.HOD_ELITE_TURNS) {',
    '\t\tif (++turns > Trials.hodEliteTurns( mob )) {',
    'G2 判据改走唯一收口')
wr(F, d)

# ------------------------------------------------ 3 misc{,_zh}.properties（CRLF）
print('== 3/5 文本 misc_zh / misc')
F = 'core/src/main/assets/messages/misc/misc_zh.properties'
d = rd(F)
d = sub1(d,
    '某个敌方单位_存活超过 150 回合_后，会获得一次_随机精英效果_（_不可叠加_）。',
    '某个敌方单位_存活超过 300 回合_后，会获得一次_随机精英效果_（_不可叠加_）；_Boss 单位_则需_存活超过 450 回合_。',
    'P1 zh 文案：300 / Boss 450')
wr(F, d)

F = 'core/src/main/assets/messages/misc/misc.properties'
d = rd(F)
d = sub1(d,
    'A hostile unit that _survives for more than 150 turns_ gains a _random champion effect_ (_does not stack_).',
    'A hostile unit that _survives for more than 300 turns_ gains a _random champion effect_ (_does not stack_); '
    '_boss units_ need _more than 450 turns_.',
    'P2 en 文案：300 / boss 450')
wr(F, d)

# ------------------------------------------------- 4 HodNetzachProbe.java
print('== 4/5 HodNetzachProbe.java')
F = '_chk/HodNetzachProbe.java'
d = rd(F)
d = sub1(d,
    '\t\t@Override public int attackSkill( Char target ){ return 20; }\n'
    '\t}\n'
    '\n'
    '\t/** 探针关卡：只为给 Dungeon.level.distance 提供 width。 */',
    '\t\t@Override public int attackSkill( Char target ){ return 20; }\n'
    '\t\t/** 打上 Boss 标记（{@code Char.Property.BOSS}），用来核验 HOD 的 Boss 阈值与普通阈值真的分流。 */\n'
    '\t\tProbeMob markBoss(){ properties.add( Char.Property.BOSS ); return this; }\n'
    '\t}\n'
    '\n'
    '\t/** 探针关卡：只为给 Dungeon.level.distance 提供 width。 */',
    'D1 ProbeMob 增加 markBoss()')

d = sub1(d,
    '\t\t\tfor (int i = 0; i < Trials.HOD_ELITE_TURNS; i++) m.buff( HodGlory.class ).act();\n'
    '\t\t\tcheck( m.buffs( ChampionEnemy.class ).isEmpty(),\n'
    '\t\t\t\t\t"恰好 150 回合 ⇒ 还不发（需求是「超过 150」）" );\n'
    '\n'
    '\t\t\tm.buff( HodGlory.class ).act();\n'
    '\t\t\teq( m.buffs( ChampionEnemy.class ).size(), 1, "第 151 回合 ⇒ 发下 1 个精英" );\n'
    '\t\t\tcheck( m.buff( HodGlory.class ) == null, "发完即自摘（本 buff 不再计时）" );\n'
    '\n'
    '\t\t\t// 不再适用 ⇒ 计时清零：先在「非敌方」下空转，再回敌方，150 回合内不应触发\n'
    '\t\t\tProbeMob m2 = new ProbeMob();\n'
    '\t\t\tHodGlory g2 = Buff.affect( m2, HodGlory.class );\n'
    '\t\t\tm2.alignment = Char.Alignment.ALLY;\n'
    '\t\t\tfor (int i = 0; i < 200; i++) g2.act();\n'
    '\t\t\tcheck( m2.buffs( ChampionEnemy.class ).isEmpty(), "非敌方阵营空转 200 回合 ⇒ 一次也不发" );\n'
    '\t\t\tm2.alignment = Char.Alignment.ENEMY;\n'
    '\t\t\tfor (int i = 0; i < Trials.HOD_ELITE_TURNS; i++) g2.act();\n'
    '\t\t\tcheck( m2.buffs( ChampionEnemy.class ).isEmpty(), "回到敌方后只数了 150 回合 ⇒ 仍未发（说明计数确实被清零过）" );\n'
    '\t\t\tg2.act();\n'
    '\t\t\teq( m2.buffs( ChampionEnemy.class ).size(), 1, "第 151 回合 ⇒ 发下精英" );',
    '\t\t\tcheck( Trials.HOD_ELITE_TURNS == 300, "普通怪阈值 HOD_ELITE_TURNS = 300" );\n'
    '\t\t\tcheck( Trials.HOD_ELITE_TURNS_BOSS == 450, "Boss 阈值 HOD_ELITE_TURNS_BOSS = 450" );\n'
    '\t\t\tcheck( Trials.hodEliteTurns( new ProbeMob() ) == 300, "hodEliteTurns(普通怪) = 300" );\n'
    '\n'
    '\t\t\tfor (int i = 0; i < Trials.HOD_ELITE_TURNS; i++) m.buff( HodGlory.class ).act();\n'
    '\t\t\tcheck( m.buffs( ChampionEnemy.class ).isEmpty(),\n'
    '\t\t\t\t\t"普通怪恰好 300 回合 ⇒ 还不发（需求是「超过 300」）" );\n'
    '\n'
    '\t\t\tm.buff( HodGlory.class ).act();\n'
    '\t\t\teq( m.buffs( ChampionEnemy.class ).size(), 1, "第 301 回合 ⇒ 发下 1 个精英" );\n'
    '\t\t\tcheck( m.buff( HodGlory.class ) == null, "发完即自摘（本 buff 不再计时）" );\n'
    '\n'
    '\t\t\t// Boss 阈值 450：数到普通怪的 300 仍不发，数满 450 才发 —— 证明两档真的分流\n'
    '\t\t\tProbeMob boss = new ProbeMob().markBoss();\n'
    '\t\t\tcheck( Trials.hodEliteTurns( boss ) == 450, "hodEliteTurns(Boss) = 450（按 Char.Property.BOSS 分流）" );\n'
    '\t\t\tHodGlory gb = Buff.affect( boss, HodGlory.class );\n'
    '\t\t\tfor (int i = 0; i < Trials.HOD_ELITE_TURNS; i++) gb.act();\n'
    '\t\t\tcheck( boss.buffs( ChampionEnemy.class ).isEmpty(),\n'
    '\t\t\t\t\t"Boss 数了 300 回合（＝普通怪阈值）⇒ 仍不发" );\n'
    '\t\t\tfor (int i = 0; i < Trials.HOD_ELITE_TURNS_BOSS - Trials.HOD_ELITE_TURNS; i++) gb.act();\n'
    '\t\t\tcheck( boss.buffs( ChampionEnemy.class ).isEmpty(), "Boss 恰好 450 回合 ⇒ 还不发（需求是「超过 450」）" );\n'
    '\t\t\tgb.act();\n'
    '\t\t\teq( boss.buffs( ChampionEnemy.class ).size(), 1, "第 451 回合 ⇒ Boss 发下 1 个精英" );\n'
    '\t\t\tcheck( boss.buff( HodGlory.class ) == null, "Boss 发完即自摘" );\n'
    '\n'
    '\t\t\t// 不再适用 ⇒ 计时清零：先在「非敌方」下空转，再回敌方，阈值内不应触发\n'
    '\t\t\tProbeMob m2 = new ProbeMob();\n'
    '\t\t\tHodGlory g2 = Buff.affect( m2, HodGlory.class );\n'
    '\t\t\tm2.alignment = Char.Alignment.ALLY;\n'
    '\t\t\tfor (int i = 0; i < 400; i++) g2.act();\n'
    '\t\t\tcheck( m2.buffs( ChampionEnemy.class ).isEmpty(), "非敌方阵营空转 400 回合 ⇒ 一次也不发（远超 300 阈值）" );\n'
    '\t\t\tm2.alignment = Char.Alignment.ENEMY;\n'
    '\t\t\tfor (int i = 0; i < Trials.HOD_ELITE_TURNS; i++) g2.act();\n'
    '\t\t\tcheck( m2.buffs( ChampionEnemy.class ).isEmpty(), "回到敌方后只数了 300 回合 ⇒ 仍未发（说明计数确实被清零过）" );\n'
    '\t\t\tg2.act();\n'
    '\t\t\teq( m2.buffs( ChampionEnemy.class ).size(), 1, "第 301 回合 ⇒ 发下精英" );',
    'D2 ⑥ 段：300/450 分流 + Boss 分支')
wr(F, d)

# ------------------------------------------------ 5 verify_hod_netzach.py
print('== 5/5 verify_hod_netzach.py')
F = '_chk/verify_hod_netzach.py'
d = rd(F)
d = sub1(d,
    '#   · HOD 的晋升侧 = 「存活 > 150 回合 ⇒ 随机精英（不可叠加）」：',
    '#   · HOD 的晋升侧 = 「存活 > 300 回合（Boss 450）⇒ 随机精英（不可叠加）」：',
    'V1 头注释：阈值')
d = sub1(d,
    '#   · 常量口径正确（按区递增 0.10、淡出阈值 1/3、150 回合）；',
    '#   · 常量口径正确（按区递增 0.10、淡出阈值 1/3、普通 300 / Boss 450 回合、Boss 判据 Char.Property.BOSS）；',
    'V2 头注释：常量清单')
d = sub1(d,
    "    chk(const_of(t, 'HOD_ELITE_TURNS') == '150', '实读 HOD_ELITE_TURNS = 150')\n"
    "    fake = re.sub(r'(public static final int HOD_ELITE_TURNS\\s*=\\s*)150', r'\\g<1>100', t, count=1)",
    "    chk(const_of(t, 'HOD_ELITE_TURNS') == '300', '实读 HOD_ELITE_TURNS = 300')\n"
    "    fake = re.sub(r'(public static final int HOD_ELITE_TURNS\\s*=\\s*)300', r'\\g<1>100', t, count=1)",
    'V3 selftest：常量断言 300')
d = sub1(d,
    "    chk(const_of(trials, 'HOD_ELITE_TURNS') == '150', 'HOD_ELITE_TURNS = 150')",
    "    chk(const_of(trials, 'HOD_ELITE_TURNS') == '300', 'HOD_ELITE_TURNS = 300（普通怪）')\n"
    "    chk(const_of(trials, 'HOD_ELITE_TURNS_BOSS') == '450', 'HOD_ELITE_TURNS_BOSS = 450（Boss）')\n"
    "    hod_et = method_span(trials, 'public static int hodEliteTurns( Mob mob ){')\n"
    "    chk(hod_et is not None, 'Trials.hodEliteTurns 方法体截取成功（唯一取数收口）')\n"
    "    if hod_et:\n"
    "        good, pos = seq_ok(hod_et, ['Char.Property.BOSS', 'HOD_ELITE_TURNS_BOSS', 'HOD_ELITE_TURNS'])\n"
    "        chk(good, 'hodEliteTurns：Boss 走 450、其余走 300（位置 %s）' % (pos,))",
    'V4 ①层：常量 300/450 + 收口方法')
d = sub1(d,
    "            '++turns > Trials.HOD_ELITE_TURNS',",
    "            '++turns > Trials.hodEliteTurns( mob )',",
    'V5 HodGlory.act 顺序锚点')
d = sub1(d,
    "                                    'trials.hod_desc': ['区域', '精英', '150', '不可叠加', '10%', '50%']}),",
    "                                    'trials.hod_desc': ['区域', '精英', '300', '450', '不可叠加', '10%', '50%']}),",
    'V6 zh 文本要素 300/450')
d = sub1(d,
    "                                 'trials.hod_desc': ['region', 'champion', '150', 'stack', '10%', '50%']})]:",
    "                                 'trials.hod_desc': ['region', 'champion', '300', '450', 'stack', '10%', '50%']})]:",
    'V7 en 文本要素 300/450')
wr(F, d)

print('\n完成。备份在 _chk/_bak_hod_elite_turns/')
