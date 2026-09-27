# -*- coding: utf-8 -*-
r"""2026-09-24 补丁：

① 「背叛家人者」预支账簿充能只免了精准惩罚、没免攻击延迟惩罚。
   根因：FamilyBetrayal 的力量补足 buff 在 Talent.onHeroAttackResolved 里就被摘掉，
   而攻击延迟（Weapon.baseDelay 读 Hero.STR()）要等它之后的
   Hero.onAttackComplete → spend(attackDelay()) 才算 ⇒ ×1.2ⁿ 延迟惩罚漏下来。
   对策：消费点挪到 Hero.spend(float)（super.spend 之后），并在 onAttackStarted 里兜残留。

② 陈列室（死亡证明 999 层）缺神器「骷髅钥匙」：MuseumLevel.skip() 把它过滤掉了。
   对策：skip() 只保留英雄专属盔甲过滤。

幂等：所有替换都是「精确旧文 → 新文」，已应用则整体跳过。
"""
import os
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BAK = os.path.join(ROOT, '_chk', '_bak_2026-09-24')

FAMILY = 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/buffs/FamilyBetrayal.java'
HERO = 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Hero.java'
TALENT = 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/hero/Talent.java'
MUSEUM = 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels/MuseumLevel.java'


def load(rel):
    p = os.path.join(ROOT, rel)
    with open(p, 'rb') as f:
        raw = f.read()
    crlf = b'\r\n' in raw
    assert b'\r' not in raw.replace(b'\r\n', b''), rel + ' 有裸 CR'
    #统一成 \n 再匹配（写回时按原样还原）
    return p, crlf, raw.decode('utf-8').replace('\r\n', '\n')


def write(p, crlf, text):
    out = text.replace('\n', '\r\n') if crlf else text
    with open(p, 'wb') as f:
        f.write(out.encode('utf-8'))


def backup(rel):
    os.makedirs(BAK, exist_ok=True)
    src = os.path.join(ROOT, rel)
    dst = os.path.join(BAK, os.path.basename(rel))
    if not os.path.exists(dst):
        shutil.copyfile(src, dst)
        print('  备份 -> _chk/_bak_2026-09-24/%s' % os.path.basename(rel))
    else:
        print('  备份已存在（保留最早版本）: %s' % os.path.basename(rel))


def sub_once(text, old, new, tag):
    if old not in text:
        assert new in text, '%s：旧文与新文都没命中（改动被破坏？）' % tag
        print('  跳过（已应用）: %s' % tag)
        return text
    n = text.count(old)
    assert n == 1, '%s：旧文命中 %d 次（期望 1）' % (tag, n)
    return text.replace(old, new, 1)


# ===========================================================================
# ① FamilyBetrayal.java —— 生命周期改到 Hero.spend
# ===========================================================================
print('[1] FamilyBetrayal.java')
p, crlf, fam = load(FAMILY)
backup(FAMILY)

OLD_LIFECYCLE = (
    ' * <p><b>生命周期</b>：施加在 {@code Talent.onHeroAttackStarted}（{@code Char.attack} 里、\n'
    ' * 命中判定与 {@code damageRoll} 之前）、解除在 {@code Talent.onHeroAttackResolved}\n'
    ' * （命中与落空两条路都会走到；只有「目标无敌，攻击根本没打出去」那条早退路径不走，\n'
    ' * 所以那条路上压根不收费、也不挂 buff）。即使某条路径漏了解除，{@value #DURATION} 回合的时长也会兜底自灭。</p>'
)

NEW_LIFECYCLE = (
    ' * <p><b>生命周期</b>：施加在 {@code Talent.onHeroAttackStarted}（{@code Char.attack} 里、\n'
    ' * 命中判定与 {@code damageRoll} 之前），解除在 {@code Hero.spend(float)} —— 也就是<b>本击的回合\n'
    ' * 成本已经结算完</b>的那一刻。中途有两条保险：{@link #onAttackStarted} 开头先摘掉上一击的残留；\n'
    ' * {@value #DURATION} 回合的时长兜底自灭。</p>\n'
    ' *\n'
    ' * <p><b>为什么不能提前到 {@code Talent.onHeroAttackResolved} 摘</b>（2026-09-24 修的 bug）：\n'
    ' * 那个钩子跑在 {@code Char.attack} 的尾巴上，而<b>攻击延迟要等它之后</b>才由\n'
    ' * {@code Hero.onAttackComplete → spend(attackDelay())} 算出来 —— {@code Weapon.baseDelay()}\n'
    ' * 读的正是 {@code Hero.STR()}。在那里摘掉，本击的精准惩罚（÷1.5ⁿ）确实没了，\n'
    ' * 但 {@code ×1.2ⁿ} 的延迟惩罚会原样留下。把消费点挪到 {@code Hero.spend} 之后，\n'
    ' * 本击的力量补足对<b>精准 / 伤害 / 攻击延迟 / 偷袭资格</b>四处同时生效，且仍然只活一次攻击。</p>\n'
    ' *\n'
    ' * <p>{@code Hero.spend(float)} 是英雄<b>所有</b>回合成本的唯一出口：平砍走\n'
    ' * {@code Hero.onAttackComplete}，连击 / 武技 / buff 驱动的一击也都在各自结尾调\n'
    ' * {@code spendAndNext(hero.attackDelay())}，所以这一个点就覆盖全部攻击路径；\n'
    ' * 延迟为 0 的一击（手起刀落）走 {@code next()}，它内部同样落到这里。</p>'
)

fam = sub_once(fam, OLD_LIFECYCLE, NEW_LIFECYCLE, '类注释·生命周期')

OLD_START_DOC = (
    '	 * <p>调用点在 {@code Char.attack} 的命中判定与 {@code damageRoll} 之前\n'
    '	 * （{@code Talent.onHeroAttackStarted}），所以这次补足同时作用于<b>本击的精准</b>\n'
    '	 * （{@code Weapon.accuracyFactor} 读 {@code STRReq() - STR()}）与<b>本击的伤害</b>。</p>\n'
    '	 *\n'
    '	 * <p><b>无敌目标要提前退出</b>：{@code Char.attack} 在 {@code enemy.isInvulnerable(...)}\n'
    '	 * 分支里直接 {@code return false}，<b>不</b>会走到剥除钩子（{@code onHeroAttackResolved}）。\n'
    '	 * 若在那里付了钱，玩家就会白白损失 {@value #CHARGE_COST}% 充能却什么都没做——所以\n'
    '	 * 「这一击根本打不出去」时一分不花（判据与 {@code Char.attack} 首段完全一致）。</p>'
)

NEW_START_DOC = (
    '	 * <p>调用点在 {@code Char.attack} 的命中判定与 {@code damageRoll} 之前\n'
    '	 * （{@code Talent.onHeroAttackStarted}），所以这次补足同时作用于<b>本击的精准</b>\n'
    '	 * （{@code Weapon.accuracyFactor} 读 {@code STRReq() - STR()}）与<b>本击的伤害</b>；\n'
    '	 * 它一直活到本击的回合成本结算完（{@link #consumeAfterAttack}），所以\n'
    '	 * <b>攻击延迟</b>（{@code Weapon.baseDelay}）与<b>偷袭资格</b>\n'
    '	 * （{@code Hero.canSurpriseAttack()}）这两处力量惩罚也一并免掉。</p>\n'
    '	 *\n'
    '	 * <p><b>无敌目标要提前退出</b>：{@code Char.attack} 在 {@code enemy.isInvulnerable(...)}\n'
    '	 * 分支里直接 {@code return false}，整场结算都不会发生。若在那里付了钱，玩家就会白白损失\n'
    '	 * {@value #CHARGE_COST}% 充能却什么都没做——所以「这一击根本打不出去」时一分不花\n'
    '	 * （判据与 {@code Char.attack} 首段完全一致）。</p>'
)

fam = sub_once(fam, OLD_START_DOC, NEW_START_DOC, 'onAttackStarted 注释')

OLD_RANCOR_GATE = (
    '		//已经燃着「仇怨」：力量本来就够，不收费也不再补\n'
    '		if (RevengeLedger.rancorActive( hero )) return;\n'
    '\n'
    '		SealedSwordBase sword = (SealedSwordBase) hero.belongings.attackingWeapon();'
)

NEW_RANCOR_GATE = (
    '		//已经燃着「仇怨」：力量本来就够，不收费也不再补\n'
    '		if (RevengeLedger.rancorActive( hero )) return;\n'
    '\n'
    '		//先摘掉上一击的残留（正常已由 Hero.spend 收走，这里只兜「收了 buff 却一次都没 spend」的早退路径）：\n'
    '		//残留会垫着力量让下面的 need 恒为 0 ⇒ 变成「付一次钱、永久挥得动」\n'
    '		consumeAfterAttack( hero );\n'
    '\n'
    '		SealedSwordBase sword = (SealedSwordBase) hero.belongings.attackingWeapon();'
)

fam = sub_once(fam, OLD_RANCOR_GATE, NEW_RANCOR_GATE, 'onAttackStarted 兜残留')

OLD_CONSUME_DOC = (
    '	/**\n'
    '	 * 攻击<b>结算后</b>：这一次攻击的力量补足到此为止（命中与落空都会走到）。\n'
    '	 *\n'
    '	 * <p>必须每击都摘掉，否则它会在下一次「不需要补足」的攻击里继续生效——\n'
    '	 * 那就不再是「一次性」，而变成常驻力量了。</p>\n'
    '	 */'
)

NEW_CONSUME_DOC = (
    '	/**\n'
    '	 * 本击的回合成本结算完 ⇒ 这一次攻击的力量补足到此为止\n'
    '	 * （命中、落空、目标中途死亡三条路都会走到）。\n'
    '	 *\n'
    '	 * <p><b>消费点＝{@code Hero.spend(float)}</b>（在 {@code super.spend(time)} 之后调用），\n'
    '	 * <b>不是</b> {@code Talent.onHeroAttackResolved}：后者跑在 {@code Char.attack} 的尾巴上，\n'
    '	 * 而攻击延迟要等它之后才由 {@code Hero.onAttackComplete → spend(attackDelay())} 算出来\n'
    '	 * （{@code Weapon.baseDelay} 读的是 {@code Hero.STR()}）。在那里摘，\n'
    '	 * 攻击延迟的 {@code ×1.2ⁿ} 惩罚就会漏下来。详见类注释的「生命周期」一节。</p>\n'
    '	 *\n'
    '	 * <p>必须每击都摘掉，否则它会在下一次「不需要补足」的攻击里继续生效——\n'
    '	 * 那就不再是「一次性」，而变成常驻力量了。{@code Hero.spend} 是英雄所有回合成本的唯一出口，\n'
    '	 * 所以平砍、连击、武技、buff 驱动的一击全都覆盖得到；没挂 buff 时本方法只是空操作，\n'
    '	 * 放在这条热路径上没有副作用。</p>\n'
    '	 */'
)

fam = sub_once(fam, OLD_CONSUME_DOC, NEW_CONSUME_DOC, 'consumeAfterAttack 注释')

assert '{@code Hero.spend(float)}' in fam, '类注释没写上新的消费点'
assert fam.count('{@link #consumeAfterAttack}') >= 1
write(p, crlf, fam)
print('  OK 已改写（%s，现 %d 字符）' % ('CRLF' if crlf else 'LF', len(fam)))

# ===========================================================================
# ① Hero.java —— spend() 消费
# ===========================================================================
print('[2] Hero.java')
p, crlf, hero = load(HERO)
backup(HERO)

OLD_SPEND = (
    '	@Override\n'
    '	public void spend( float time ) {\n'
    '		super.spend(time);\n'
    '		//沉默的代价："操作"的唯一定义是消耗回合时间的动作（移动/攻击/使用道具/等待均经由本方法），\n'
    '		//因此任何一次时间消耗都使饰物的空闲自动等待时钟从头计算（打开背包等不计，见 SilentPrice）。\n'
    '		SilentPrice.resetIdleClock();\n'
    '	}'
)

NEW_SPEND = (
    '	@Override\n'
    '	public void spend( float time ) {\n'
    '		super.spend(time);\n'
    '		//沉默的代价："操作"的唯一定义是消耗回合时间的动作（移动/攻击/使用道具/等待均经由本方法），\n'
    '		//因此任何一次时间消耗都使饰物的空闲自动等待时钟从头计算（打开背包等不计，见 SilentPrice）。\n'
    '		SilentPrice.resetIdleClock();\n'
    '		//中指长兄 转职「背叛家人者」：本击的回合成本（含攻击延迟）刚刚结算完，\n'
    '		//账簿预支的力量补足到这里才摘——攻击延迟由 Weapon.baseDelay() 读 Hero.STR() 算，\n'
    '		//而它在 Char.attack 之后、本方法之前；提前到 onHeroAttackResolved 摘会漏掉 ×1.2ⁿ 的延迟惩罚。\n'
    '		//本方法是英雄所有回合成本的唯一出口（平砍/连击/武技/buff 驱动的一击都经此），\n'
    '		//没挂该 buff 时是空操作。详见 FamilyBetrayal 类注释「生命周期」。\n'
    '		FamilyBetrayal.consumeAfterAttack( this );\n'
    '	}'
)

hero = sub_once(hero, OLD_SPEND, NEW_SPEND, 'Hero.spend')
write(p, crlf, hero)
print('  OK 已改写（%s，现 %d 字符）' % ('CRLF' if crlf else 'LF', len(hero)))

# ===========================================================================
# ① Talent.java —— 摘掉 onHeroAttackResolved 里的提前消费
# ===========================================================================
print('[3] Talent.java')
p, crlf, talent = load(TALENT)
backup(TALENT)

OLD_RESOLVED = (
    '		//中指长兄 转职「背叛家人者」：这一击打完了，力量补足到此为止——\n'
    '		//放进来的那一笔只在一次攻击里生效，命中与落空两条路都要摘掉\n'
    '		FamilyBetrayal.consumeAfterAttack( hero );'
)

NEW_RESOLVED = (
    '		//中指长兄 转职「背叛家人者」：这一击的力量补足【不在这里摘】——\n'
    '		//攻击延迟（Weapon.baseDelay 读 Hero.STR()）发生在本钩子之后的\n'
    '		//Hero.onAttackComplete → spend(attackDelay())，在这里摘会让 ×1.2ⁿ 的延迟惩罚漏下来。\n'
    '		//真正的消费点＝Hero.spend()（见 FamilyBetrayal 类注释「生命周期」）。'
)

talent = sub_once(talent, OLD_RESOLVED, NEW_RESOLVED, 'onHeroAttackResolved')
write(p, crlf, talent)
print('  OK 已改写（%s，现 %d 字符）' % ('CRLF' if crlf else 'LF', len(talent)))

# ===========================================================================
# ② MuseumLevel.java —— 放行骷髅钥匙
# ===========================================================================
print('[4] MuseumLevel.java')
p, crlf, museum = load(MUSEUM)
backup(MUSEUM)

OLD_SECTION = '		section(Catalog.ARTIFACTS, true);                  //神器（排除钥匙类）'
NEW_SECTION = '		section(Catalog.ARTIFACTS, true);                  //神器（含骷髅钥匙：唯一注册进神器图鉴的钥匙类）'
museum = sub_once(museum, OLD_SECTION, NEW_SECTION, 'createItems ARTIFACTS 注释')

OLD_SKIP = (
    '	private boolean skip(Class<?> c) {\n'
    '		//英雄专属盔甲（WarriorArmor/MageArmor/RogueArmor/HuntressArmor/DuelistArmor/ClericArmor）\n'
    '		if (ClassArmor.class.isAssignableFrom(c)) return true;\n'
    '		//钥匙类掉落（SkeletonKey 是唯一注册进神器图鉴的钥匙类）\n'
    '		if (SkeletonKey.class.isAssignableFrom(c)) return true;\n'
    '		return false;\n'
    '	}'
)

NEW_SKIP = (
    '	/**\n'
    '	 * 不陈列的装备类。\n'
    '	 *\n'
    '	 * <p>只过滤<b>英雄专属盔甲</b>（WarriorArmor / MageArmor / RogueArmor / HuntressArmor /\n'
    '	 * DuelistArmor / ClericArmor）——它们挂在 {@code Generator.Category.ARMOR.classes} 里，\n'
    '	 * 但对别的职业没有意义，陈列出来只占格子。</p>\n'
    '	 *\n'
    '	 * <p><b>{@code SkeletonKey} 不再排除</b>（2026-09-24 用户要求）：它同时注册在\n'
    '	 * {@code Generator.Category.ARTIFACT.classes}（神器图鉴里有它一格），先前这里把它一起\n'
    '	 * 过滤掉，于是陈列室里<b>唯独缺这一件神器</b>。它只是「长得像钥匙的神器」，本身就是普通神器，\n'
    '	 * 陈列 / 拾取 / 返程流程都不受影响。</p>\n'
    '	 */\n'
    '	private boolean skip(Class<?> c) {\n'
    '		return ClassArmor.class.isAssignableFrom(c);\n'
    '	}'
)

museum = sub_once(museum, OLD_SKIP, NEW_SKIP, 'skip()')

OLD_IMPORT = 'import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SkeletonKey;\n'
if OLD_IMPORT in museum:
    museum = museum.replace(OLD_IMPORT, '', 1)
    print('  已删除 SkeletonKey import')
else:
    print('  跳过（import 已删除）')

assert 'SkeletonKey;' not in museum, '仍残留 SkeletonKey import'
assert 'SkeletonKey.class' not in museum, 'skip() 里仍有 SkeletonKey.class 判据'
#只剩注释里的 {@code SkeletonKey} 提及
assert museum.count('SkeletonKey') == museum.count('{@code SkeletonKey}')
write(p, crlf, museum)
print('  OK 已改写（%s，现 %d 字符）' % ('CRLF' if crlf else 'LF', len(museum)))

print()
print('全部替换完成。')
