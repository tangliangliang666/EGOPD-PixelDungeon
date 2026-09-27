"""
四项二层天赋改写 · 源码级核验（2026-09-26）

背景：这一批改的是四个**已经上线**的二层天赋的效果口径（不是新增技能），
所以最怕的两类错是「旧口径没拆干净」和「新口径只落了一半」：
  - 神谕代行者 T2「变换无常」：从「闪避数值 ×1.1/×1.2」改成「闪避掷点额外重复 1/2 次取最高」。
    这是一个**比大小**机制，无法用闪避乘区等价表达 ⇒ 必须真的把乘区分支从
    `Hero.defenseSkill()` 拆掉、把掷点搬到 `Char.hit()` 里重复。只改一处＝数值变成双重加成。
  - 拇指 T2「狩猎一餐」：恢复对象从「武技（MeleeWeapon.Charger）」换成「奥丁之眼」。
    奥丁之眼原本没有「外部补充能」的入口（`charge()` 自带 0.25 换算率），故新增
    `OdinsEye.restoreCharge()`；若忘了删旧分支＝两种充能一起发。
  - 环指 T2「骨骸编织」：**两次返工**。第一版把区间从 2~8 改成 0~角色等级，但仍是「消耗素材时
    掷一次、把结果存进 buff」—— 等于把一个随机值冻结在消耗那一刻，而且在图标上表现为「层数」。
    第二版（本版）推翻整个方案：buff 不存数值、没有层数，减伤时由 `Char.drRoll()` **实时**按
    「0~角色等级」取上界（照抄 `Barkskin`）。⇒ 判据盯的是「数值有没有被存起来」。
  - 中指 T2「纹身铭刻」：增强，额外给「生命强化」＝当前生命上限的 25%/50%。
    复用既有 `ElixirOfMight.HTBoost`，走新增的 `explicitBonus` 通道（默认公式表达不了「HT 的百分比」）。

判据表覆盖：
  A. 变换无常：掷点搬家是否**完整**（乘区只留一份、旧乘区分支确实消失、比较用的是取最高后的值）。
  B. 狩猎一餐：旧分支（武技充能）消失 + 新入口 `OdinsEye.restoreCharge` 的守卫齐全。
  C. 骨骸编织：无存档数值、无层数显示、drRoll 实时取 maxArmor、旧 `2~8` 全仓不留、时长公式未动。
  D. 纹身铭刻：天赋取百分比 → 神器侧写入 → buff 侧生效 → 存档字段齐全 → GC 文本键存在。
  E. 文本（zh + en 双份）：四个 desc 的语义关键词都换了，「2~8」「剑术充能」一类旧口径不得残留。

用法：`python _chk/verify_talent_rework.py`（加 `--selftest` 跑反例自测；反例 FAIL **不计总账**）。
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)
# 剥注释用既有的单趟状态机（**别用正则去 /* */ **——`//**` 会一路吞到文件末尾，见 skill §10）
from check_unused_imports import strip_comments

SRC = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon')
MSG = os.path.join(ROOT, 'core/src/main/assets/messages')

FILES = {
    'char':    os.path.join(SRC, 'actors/Char.java'),
    'hero':    os.path.join(SRC, 'actors/hero/Hero.java'),
    'talent':  os.path.join(SRC, 'actors/hero/Talent.java'),
    'eye':     os.path.join(SRC, 'items/artifacts/OdinsEye.java'),
    'ledger':  os.path.join(SRC, 'items/artifacts/RevengeLedger.java'),
    'bodyart': os.path.join(SRC, 'items/BodyArtMaterial.java'),
    'bw':      os.path.join(SRC, 'actors/buffs/BoneWeaving.java'),
    'elixir':  os.path.join(SRC, 'items/potions/elixirs/ElixirOfMight.java'),
}

TEXT = {
    'act_zh': os.path.join(MSG, 'actors/actors_zh.properties'),
    'act_en': os.path.join(MSG, 'actors/actors.properties'),
    'it_zh':  os.path.join(MSG, 'items/items_zh.properties'),
    'it_en':  os.path.join(MSG, 'items/items.properties'),
}

ok = True
results = []


def chk(cond, msg):
    global ok
    print(('  [OK]   ' if cond else '  [FAIL] ') + msg)
    if not cond:
        ok = False
    results.append(bool(cond))


def sq(t):
    """压掉空白，便于跨行/跨行尾做子串判定。"""
    return " ".join(t.split())


def has(text, snippet):
    return sq(snippet) in sq(text)


def index_of(name, text, snippet):
    """取片段位置（已压白），取不到返回 -1。"""
    return sq(text).find(sq(snippet))


def body_of(text, signature):
    """按大括号配平取方法主体（签名必须唯一，且已剥注释）。"""
    idx = text.find(signature)
    if idx < 0:
        return None
    if text.find(signature, idx + 1) >= 0:
        raise AssertionError('签名不唯一：%s' % signature)
    start = text.find("{", idx + len(signature) - 1)
    if start < 0:
        return None
    depth = 0
    for i in range(start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[start:i + 1]
    return None


def load():
    """源码读成 {name: {'raw','code'}}；文本文件读成 {name: {'raw','code'}}（文本也留 raw）。"""
    S = {}
    for k, p in FILES.items():
        raw = open(p, encoding='utf-8').read()
        S[k] = {'raw': raw, 'code': strip_comments(raw)}
    for k, p in TEXT.items():
        raw = open(p, encoding='utf-8').read()
        S[k] = {'raw': raw, 'code': raw}
    return S


def body(S, key, signature):
    b = body_of(S[key]['code'], signature)
    if b is None:
        raise AssertionError('取不到主体：%s @ %s' % (key, signature))
    return b


def line_of(S, key, name):
    """取 `name=...` 那一整行（不含换行），用于文本键断言。"""
    m = re.search(r'^' + re.escape(name) + r'=([^\r\n]*)$', S[key]['raw'], re.M)
    return m.group(1) if m else None


# ============================================================ 判据表
CASES = []


def case(cid, desc, mutator=None):
    def deco(fn):
        CASES.append((cid, desc, fn, mutator))
        return fn
    return deco


def mut_sub(key, pattern, repl, count=1):
    """反例变异器工厂：在 raw 上按正则替换，再重算剥注释版本。

    ⚠️ `re.S | re.M` 一起给：`re.S` 让 `[^\\n]*\\r?\\n` 之类跨行写法可用，
    `re.M` 让 `^键名=` 这种**行首锚点**能用（否则 `^` 只匹配文件开头，
    E 组那几条「改坏文本键」的反例永远命中 0 次、被 SKIP 掉）。"""
    def f(S):
        S[key]['raw'], n = re.subn(pattern, repl, S[key]['raw'], count=count, flags=re.S | re.M)
        assert n == count, '反例变异没命中（%d 次）：%s' % (n, pattern)
        if key in FILES:
            S[key]['code'] = strip_comments(S[key]['raw'])
        else:
            S[key]['code'] = S[key]['raw']
    return f


def mut_drop(key, pattern, count=1):
    """把命中的片段删掉（用于「本该存在」的判据）。"""
    return mut_sub(key, pattern, '', count=count)


HIT_SIG = 'public static boolean hit( Char attacker, Char defender, float accMulti, boolean magic ) {'
EVASION_SIG = 'private static float evasionRoll( Char defender, float defStat ){'
FOOD_SIG = 'public static void onFoodEaten( Hero hero, float foodVal, Item foodSource ){'
RESTORE_SIG = 'public static void restoreCharge( Hero hero, float amount ){'
ENGRAVE_SIG = 'private void engrave(Hero hero) {'
TATTOO_PCT_SIG = 'public static int tattooHealthBoostPercent( Hero hero ){'
SET_EXPL_SIG = 'public void setExplicitBonus( int amount ){'
BOOST_SIG = 'public int boost(){'
RESET_SIG = 'public void reset(){'


# ---------------------------------------------------------------- A 组：变换无常
@case('A1', 'Char.hit：闪避掷点改走 evasionRoll()，不再是内联 Random.Float',
      mut_sub('char', r'float defRoll = evasionRoll\( defender, defStat \);',
              'float defRoll = Random.Float( defStat );'))
def a1(S):
    return has(S['char']['code'], 'float defRoll = evasionRoll( defender, defStat );')


@case('A2', 'Char.hit：重复次数取自 pointsInTalent(SHIFTING_FATE)（+1=1 次 / +2=2 次）',
      mut_sub('char', r'pointsInTalent\( Talent\.SHIFTING_FATE \)', 'pointsInTalent( Talent.BLESS )'))
def a2(S):
    return has(S['char']['code'],
               'int rerolls = ((Hero) defender).pointsInTalent( Talent.SHIFTING_FATE );')


@case('A3', 'Char.hit：每次重掷与当前值取 max（「取最高值」而不是取最后一次）',
      mut_sub('char', r'defRoll = Math\.max\( defRoll, evasionRoll\( defender, defStat \) \);',
              'defRoll = evasionRoll( defender, defStat );'))
def a3(S):
    return has(S['char']['code'],
               'defRoll = Math.max( defRoll, evasionRoll( defender, defStat ) );')


@case('A4', 'Char.hit：重掷段被 instanceof Hero 门控（怪物不吃这个英雄天赋）',
      mut_sub('char', r'if \(defender instanceof Hero\)\{', 'if (true){'))
def a4(S):
    return has(S['char']['code'], 'if (defender instanceof Hero){')


@case('A5', 'Char.hit：重掷段**位于**比大小判定之前（否则改了也白改）',
      mut_drop('char', r'int rerolls = \(\(Hero\) defender\)\.pointsInTalent\( Talent\.SHIFTING_FATE \);'))
def a5(S):
    b = body(S, 'char', HIT_SIG)
    i_reroll = index_of('char', b, 'int rerolls = ((Hero) defender).pointsInTalent( Talent.SHIFTING_FATE );')
    i_cmp = index_of('char', b, 'if (acuRoll >= defRoll){')
    return i_reroll >= 0 and i_cmp >= 0 and i_reroll < i_cmp


@case('A6', 'Char.hit：乘区只有一份 —— 全文件 `Random.Float( defStat )` 恰好出现 1 次（防复制漂移）',
      mut_sub('char', r'float defRoll = Random\.Float\( defStat \);', 'float defRoll = 0f;'))
def a6(S):
    return S['char']['code'].count('Random.Float( defStat )') == 1


@case('A7', 'evasionRoll()：签名与可见性（private static，仅 hit 内部使用）',
      mut_sub('char', r'private static float evasionRoll\( Char defender, float defStat \)\{',
              'public static float evasionRoll( Char defender, float defStat ){'))
def a7(S):
    return has(S['char']['code'], 'private static float evasionRoll( Char defender, float defStat ){')


@case('A8', 'evasionRoll()：九条既有乘区一条不少（Bless/Hex/Daze/冠军/升天/祝福天赋/雪貂/沉默代价）',
      mut_drop('char', r'if \(defender\.buff\(Bless\.class\) != null\) defRoll \*= 1\.25f;'))
def a8(S):
    b = sq(body(S, 'char', EVASION_SIG))
    parts = [
        'float defRoll = Random.Float( defStat );',
        'if (defender.buff(Bless.class) != null) defRoll *= 1.25f;',
        'if (defender.buff( Hex.class) != null) defRoll *= 0.8f;',
        'if (defender.buff( Daze.class) != null) defRoll *= 0.5f;',
        'for (ChampionEnemy buff : defender.buffs(ChampionEnemy.class)){',
        'defRoll *= buff.evasionAndAccuracyFactor();',
        'defRoll *= AscensionChallenge.statModifier(defender);',
        'defRoll *= 1.01f + 0.02f*Dungeon.hero.pointsInTalent(Talent.BLESS);',
        'defRoll *= FerretTuft.evasionMultiplier();',
        'defRoll *= SilentPrice.heroEvasionMultiplier();',
        'return defRoll;',
    ]
    return all(p in b for p in parts)


@case('A9', 'Hero.java：旧「闪避数值乘区」分支已彻底拆除（全文件再无 SHIFTING_FATE）',
      mut_sub('hero', r'public int defenseSkill\( Char enemy \) \{',
              'public int defenseSkill( Char enemy ) {\n\t\tDungeon.hero.pointsInTalent( Talent.SHIFTING_FATE );'))
def a9(S):
    # ⚠️ 反例锚点必须落在 Hero.java 里真实存在的串上（INFINITE_EVASION 其实声明在 Char.java，
    #    拿它当锚点会永远命中 0 次、自测被 SKIP 掉）。
    return 'SHIFTING_FATE' not in S['hero']['code']


@case('A10', 'Hero.defenseSkill：仍保留「变化无常」的说明注释（避免后人以为漏改）',
      mut_drop('hero', r'//变化无常：不再走闪避数值乘区[^\r\n]*\r?\n[^\r\n]*\r?\n[^\r\n]*'))
def a10(S):
    return has(S['hero']['raw'], '变化无常') and has(S['hero']['raw'], '刻意为之')


@case('A11', 'Char.hit：defStat 仍由 Trials.finalEvasion 派生（没连累考验/预知眼的无限闪避）',
      mut_sub('char', r'float defStat = Trials\.finalEvasion\( defender, attacker \);',
              'float defStat = defender.defenseSkill( attacker, this );'))
def a11(S):
    return has(S['char']['code'], 'float defStat = Trials.finalEvasion( defender, attacker );')


# ---------------------------------------------------------------- B 组：狩猎一餐
@case('B1', 'Talent.onFoodEaten：HUNTING_MEAL 分支改为补奥丁之眼充能',
      mut_sub('talent', r'OdinsEye\.restoreCharge\( hero, eyeCharge \);',
              'Buff.affect(hero, MeleeWeapon.Charger.class).gainCharge(eyeCharge);'))
def b1(S):
    b = body(S, 'talent', FOOD_SIG)
    return has(b, 'OdinsEye.restoreCharge( hero, eyeCharge );')


@case('B2', 'Talent.onFoodEaten：+1 = 1 点 / +2 = 1.25 点（点数取自 pointsInTalent）',
      mut_sub('talent', r'float eyeCharge = hero\.pointsInTalent\(HUNTING_MEAL\) == 2 \? 1\.25f : 1f;',
              'float eyeCharge = 1f;'))
def b2(S):
    return has(S['talent']['code'],
               'float eyeCharge = hero.pointsInTalent(HUNTING_MEAL) == 2 ? 1.25f : 1f;')


@case('B3', 'Talent：旧口径「武技充能」在 HUNTING_MEAL 分支里已删净（同方法内 FOCUSED_MEAL 的武技充能不受影响）',
      mut_sub('talent', r'(float eyeCharge = hero\.pointsInTalent\(HUNTING_MEAL\) == 2 \? 1\.25f : 1f;)',
              r'\1\n\t\t\tBuff.affect(hero, MeleeWeapon.Charger.class).gainCharge(eyeCharge);'))
def b3(S):
    # ⚠️ 不能只是「整个 onFoodEaten 里没有 Charger」—— 同一个方法里的 LOYALIST T2
    #    FOCUSED_MEAL 本来就要充武技，那样写会永远 FAIL。必须把范围收敛到 HUNTING_MEAL 这一段。
    b = body_of(S['talent']['code'], 'if (hero.hasTalent(HUNTING_MEAL)){')
    return b is not None and 'Charger' not in sq(b)


@case('B4', 'OdinsEye.restoreCharge：public static，签名 (Hero, float)',
      mut_sub('eye', r'public static void restoreCharge\( Hero hero, float amount \)\{',
              'private void restoreCharge( Hero hero, float amount ){'))
def b4(S):
    return has(S['eye']['code'], 'public static void restoreCharge( Hero hero, float amount ){')


@case('B5', 'OdinsEye.restoreCharge：入参守卫（null / amount<=0 → 静默跳过）',
      mut_drop('eye', r'if \(hero == null \|\| amount <= 0f\) return;'))
def b5(S):
    b = sq(body(S, 'eye', RESTORE_SIG))
    return 'if (hero == null || amount <= 0f) return;' in b


@case('B6', 'OdinsEye.restoreCharge：魔免时不给充能（与神器自身的自动充能口径一致）',
      # ⚠️ 这条守卫在 OdinsEye 里出现两次（`charge()` 与 `restoreCharge()` 各一次），故 count=2：
      #    只删一处的话判据仍会 PASS，反例自测会误报「判据抓不到错」。
      mut_drop('eye', r'if \(hero\.buff\(MagicImmune\.class\) != null\) return;', count=2))
def b6(S):
    return has(body(S, 'eye', RESTORE_SIG), 'if (hero.buff(MagicImmune.class) != null) return;')


@case('B7', 'OdinsEye.restoreCharge：装备位查找 artifact → misc（两格都认）',
      mut_drop('eye', r'if \(!\(equipped instanceof OdinsEye\)\) equipped = hero\.belongings\.misc;'))
def b7(S):
    b = sq(body(S, 'eye', RESTORE_SIG))
    return 'hero.belongings.artifact' in b and 'hero.belongings.misc' in b


@case('B8', 'OdinsEye.restoreCharge：未装备 / 被诅咒 / 已满 时静默跳过（不报错、不溢出）',
      mut_drop('eye', r'if \(eye\.cursed \|\| eye\.charge >= eye\.chargeCap\) return;'))
def b8(S):
    b = sq(body(S, 'eye', RESTORE_SIG))
    return 'if (!(equipped instanceof OdinsEye)) return;' in b \
        and 'if (eye.cursed || eye.charge >= eye.chargeCap) return;' in b


@case('B9', 'OdinsEye.restoreCharge：小数结转（partialCharge 累加 → while 进位 → 封顶清零）',
      mut_drop('eye', r'while \(eye\.partialCharge >= 1f && eye\.charge < eye\.chargeCap\)\{'))
def b9(S):
    b = sq(body(S, 'eye', RESTORE_SIG))
    return 'eye.partialCharge += amount;' in b \
        and 'while (eye.partialCharge >= 1f && eye.charge < eye.chargeCap){' in b \
        and 'eye.charge = eye.chargeCap;' in b


@case('B10', 'OdinsEye.restoreCharge：末尾刷新快捷栏（否则充能变了但图标不更新）',
      mut_sub('eye', r'eye\.partialCharge = 0;\s*\}\s*Item\.updateQuickslot\(\);',
              'eye.partialCharge = 0;\n\t\t}'))
def b10(S):
    return has(body(S, 'eye', RESTORE_SIG), 'Item.updateQuickslot();')


@case('B11', 'Talent.java：确实 import 了 OdinsEye（否则是编译不过的假象）',
      mut_drop('talent', r'import com\.shatteredpixel\.shatteredpixeldungeon\.items\.artifacts\.OdinsEye;'))
def b11(S):
    return has(S['talent']['raw'],
               'import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.OdinsEye;')


# ---------------------------------------------------------------- C 组：骨骸编织
# 2026-09-26 二次返工：上一版把区间从 2~8 改成 0~角色等级，但仍沿用「消耗素材时掷一次、
# 把结果写进 buff」的老方案 —— 那等于把一个随机值**冻结**在消耗那一刻，而且在图标上表现为
# 「层数」。本版推翻该方案：buff 不存任何数值、没有层数，减伤时由 Char.drRoll() **实时**按
# 「0~角色等级」取上界（照抄 Barkskin 的写法）。⇒ 判据重点从「区间是多少」变成
# 「数值有没有被存起来 / 上界是不是实时取的」。
@case('C1', 'BodyArtMaterial：不再往 buff 写数值（armorLevel 整条链路拆净）',
      mut_sub('bodyart', r'(bw\.maxDuration = Math\.max\( bw\.maxDuration, duration \);)',
              r'\1\n\t\t\tbw.armorLevel = 1;'))
def c1(S):
    return 'armorLevel' not in sq(S['bodyart']['code'])


@case('C2', 'BodyArtMaterial：消耗素材时不再掷点（连 Random 依赖一并摘净，不留 unused import）',
      mut_sub('bodyart', r'(import com\.shatteredpixel\.shatteredpixeldungeon\.utils\.GLog;)',
              r'import com.watabou.utils.Random;\n\1'))
def c2(S):
    return 'Random' not in sq(S['bodyart']['code'])


@case('C3', 'BodyArtMaterial：时长仍按天赋点数（+1=50 回合 / +2=100 回合），未被误改',
      mut_sub('bodyart', r'float duration = points == 1 \? 50f : 100f;',
              'float duration = points == 1 ? 100f : 50f;'))
def c3(S):
    return has(S['bodyart']['code'], 'float duration = points == 1 ? 50f : 100f;')


@case('C4', 'BodyArtMaterial：进度弧基准取 Math.max —— 续期只会变长，先 +2 后 +1 不能把弧算成已过期',
      mut_sub('bodyart', r'bw\.maxDuration = Math\.max\( bw\.maxDuration, duration \);',
              'bw.maxDuration = duration;'))
def c4(S):
    return has(S['bodyart']['code'], 'bw.maxDuration = Math.max( bw.maxDuration, duration );')


@case('C5', 'Char.drRoll：护甲上界**实时**取 BoneWeaving.maxArmor(this)，不再读 buff 上的字段',
      mut_sub('char', r'BoneWeaving\.maxArmor\(this\)', 'buff(BoneWeaving.class).armorLevel'))
def c5(S):
    return has(body(S, 'char', 'public int drRoll() {'),
               'dr += Random.NormalIntRange( 0 , BoneWeaving.maxArmor(this) );')


@case('C6', 'Char.drRoll：null 守卫必须在 —— 没有该 buff 时不该多掷随机数（否则挪动全局随机流）',
      mut_sub('char', r'if \(buff\(BoneWeaving\.class\) != null\)\{', 'if (true){'))
def c6(S):
    b = sq(body(S, 'char', 'public int drRoll() {'))
    i_guard = b.find(sq('if (buff(BoneWeaving.class) != null){'))
    i_roll  = b.find(sq('dr += Random.NormalIntRange( 0 , BoneWeaving.maxArmor(this) );'))
    return i_guard >= 0 and i_roll >= 0 and i_guard < i_roll


@case('C7', 'BoneWeaving：**不覆写 iconTextDisplay()** ⇒ 图标走 FlavourBuff 默认的「剩余回合数」，没有层数',
      mut_sub('bw', r'public int icon\(\) \{',
              'public String iconTextDisplay(){ return "9"; }\\n\\n\\t@Override\\n\\tpublic int icon() {'))
def c7(S):
    c = sq(S['bw']['code'])
    return 'iconTextDisplay' not in c and 'armorLevel' not in c and 'ARMOR_LEVEL' not in c


@case('C8', 'BoneWeaving.maxArmor：public static + instanceof Hero 守卫（非英雄返回 0，不硬转）',
      mut_sub('bw', r'return ch instanceof Hero \? \(\(Hero\) ch\)\.lvl : 0;',
              'return ((Hero) ch).lvl;'))
def c8(S):
    c = S['bw']['code']
    return has(c, 'public static int maxArmor( Char ch )') \
        and has(body(S, 'bw', 'public static int maxArmor( Char ch ) {'),
               'return ch instanceof Hero ? ((Hero) ch).lvl : 0;')


@case('C9', 'BoneWeaving.desc()：护甲读数实时取 maxArmor(target)；存档只留 max_duration，不留数值',
      mut_sub('bw', r'\+ "当前护甲加成：_0~" \+ maxArmor\( target \) \+ "_',
              '+ "当前护甲加成：_5_'))
def c9(S):
    c = S['bw']['code']
    return has(body(S, 'bw', 'public String desc() {'),
               '+ "当前护甲加成：_0~" + maxArmor( target ) + "_') \
        and has(c, 'bundle.put( MAX_DURATION, maxDuration );') \
        and 'ARMOR_LEVEL' not in c


# ---------------------------------------------------------------- D 组：纹身铭刻
@case('D1', 'Talent.tattooHealthBoostPercent：public static，(Hero) 签名',
      mut_sub('talent', r'public static int tattooHealthBoostPercent\( Hero hero \)',
              'private int tattooHealthBoostPercent( Hero hero )'))
def d1(S):
    return has(S['talent']['code'], 'public static int tattooHealthBoostPercent( Hero hero ){')


@case('D2', 'Talent.tattooHealthBoostPercent：未点天赋 → 0；+1 → 25；+2 → 50',
      # ⚠️ 同表方法 `tattooChargePercent` 返回**同一个表达式**（表是刻意共用的），
      #    故 count=2；count=1 只会改到前者，判据仍 PASS、自测会误报。
      mut_sub('talent', r'return hero\.pointsInTalent\( TATTOO_ENGRAVING \) >= 2 \? 50 : 25;',
              'return hero.pointsInTalent( TATTOO_ENGRAVING ) >= 2 ? 25 : 25;', count=2))
def d2(S):
    b = sq(body(S, 'talent', TATTOO_PCT_SIG))
    return 'if (hero == null || !hero.hasTalent( TATTOO_ENGRAVING )) return 0;' in b \
        and 'return hero.pointsInTalent( TATTOO_ENGRAVING ) >= 2 ? 50 : 25;' in b


@case('D3', 'RevengeLedger.engrave：取百分比 → 按当前 HT 算出具体值（先算后写，不滚雪球）',
      mut_sub('ledger', r'int htGain = Math\.round\(hero\.HT \* \(htPercent / 100f\)\);',
              'int htGain = htPercent;'))
def d3(S):
    b = sq(body(S, 'ledger', ENGRAVE_SIG))
    return 'int htPercent = Talent.tattooHealthBoostPercent(hero);' in b \
        and 'if (htPercent > 0){' in b \
        and 'int htGain = Math.round(hero.HT * (htPercent / 100f));' in b


@case('D4', 'RevengeLedger.engrave：复用既有 ElixirOfMight.HTBoost（不新造一个「生命强化」）',
      mut_sub('ledger', r'ElixirOfMight\.HTBoost boost = Buff\.affect\(hero, ElixirOfMight\.HTBoost\.class\);',
              'Buff.affect(hero, Barkskin.class);\n\t\t\tObject boost = null;'))
def d4(S):
    return has(body(S, 'ledger', ENGRAVE_SIG),
               'ElixirOfMight.HTBoost boost = Buff.affect(hero, ElixirOfMight.HTBoost.class);')


@case('D5', 'RevengeLedger.engrave：走 setExplicitBonus 通道（覆盖式，不叠加）',
      mut_drop('ledger', r'boost\.setExplicitBonus\(htGain\);'))
def d5(S):
    return has(body(S, 'ledger', ENGRAVE_SIG), 'boost.setExplicitBonus(htGain);')


@case('D6', 'RevengeLedger.engrave：写入后立刻 updateHT(true)（上限与当前生命同步抬）',
      mut_sub('ledger', r'hero\.updateHT\(true\);', 'hero.updateHT(false);'))
def d6(S):
    return has(body(S, 'ledger', ENGRAVE_SIG), 'hero.updateHT(true);')


@case('D7', 'RevengeLedger.engrave：给玩家可见反馈（漂浮字 + GLog 文本键 tattoo_ht）',
      mut_drop('ledger', r'GLog\.p\(Messages\.get\(this, "tattoo_ht", htPercent, htGain\)\);'))
def d7(S):
    b = sq(body(S, 'ledger', ENGRAVE_SIG))
    return 'FloatingText.HEALING' in b and 'GLog.p(Messages.get(this, "tattoo_ht", htPercent, htGain));' in b


@case('D8', 'RevengeLedger.engrave：仍占 1 回合（新增效果不该把行动成本吞掉）',
      mut_drop('ledger', r'hero\.spendAndNext\(1f\); //刻入纹身占 1 回合'))
def d8(S):
    return 'spendAndNext(1f)' in sq(body(S, 'ledger', ENGRAVE_SIG))


@case('D9', 'RevengeLedger：三个新依赖都有 import（FloatingText / ElixirOfMight / CharSprite）',
      mut_drop('ledger', r'import com\.shatteredpixel\.shatteredpixeldungeon\.sprites\.CharSprite;'))
def d9(S):
    r = S['ledger']['raw']
    return all(has(r, 'import ' + x + ';') for x in (
        'com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText',
        'com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight',
        'com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite',
    ))


@case('D10', 'HTBoost：新增 explicitBonus 字段（默认 0 → 老存档/普通艾利克斯仍走原公式）',
      mut_sub('elixir', r'private int explicitBonus = 0;', 'private int explicitBonu = 0;'))
def d10(S):
    return has(S['elixir']['code'], 'private int explicitBonus = 0;')


@case('D11', 'HTBoost.reset()：清回公式形态（explicitBonus 必须一起清，否则 reset 后仍吃显式值）',
      mut_sub('elixir', r'left = 5;\s*explicitBonus = 0;', 'left = 5;'))
def d11(S):
    b = sq(body(S, 'elixir', RESET_SIG))
    return 'left = 5;' in b and 'explicitBonus = 0;' in b


@case('D12', 'HTBoost.setExplicitBonus：覆盖式写入 + 剩余等级刷回 5（＝复用「5 次升级后消失」）',
      mut_sub('elixir', r'explicitBonus = Math\.max\( 0, amount \);',
              'explicitBonus += Math.max( 0, amount );'))
def d12(S):
    b = sq(body(S, 'elixir', SET_EXPL_SIG))
    return 'left = 5;' in b and 'explicitBonus = Math.max( 0, amount );' in b


@case('D13', 'HTBoost.boost()：显式值优先，否则回落原公式（不影响其他来源的「生命强化」）',
      mut_sub('elixir', r'if \(explicitBonus > 0\)\{\s*return explicitBonus;\s*\}',
              'if (false){ return explicitBonus; }'))
def d13(S):
    b = sq(body(S, 'elixir', BOOST_SIG))
    return 'if (explicitBonus > 0){' in b and 'return explicitBonus;' in b \
        and 'return Math.round(left*boost(15 + 5*((Hero)target).lvl)/5f);' in b


@case('D14', 'HTBoost：存档读写带 EXPLICIT_BONUS（否则读档后加成丢失/变回公式值）',
      mut_drop('elixir', r'bundle\.put\( EXPLICIT_BONUS, explicitBonus \);'))
def d14(S):
    c = sq(S['elixir']['code'])
    return 'bundle.put( EXPLICIT_BONUS, explicitBonus );' in c \
        and 'explicitBonus = bundle.getInt(EXPLICIT_BONUS);' in c


# ---------------------------------------------------------------- E 组：文本 zh / en
@case('E1', 'zh「变换无常」：改为「额外随机 1/2 次，取最高」',
      mut_sub('act_zh', r'(actors\.hero\.talent\.shifting_fate\.desc=)[^\r\n]*',
              r'\1_+1：_ 闪避值提升 _10%_。'))
def e1(S):
    d = line_of(S, 'act_zh', 'actors.hero.talent.shifting_fate.desc')
    return d is not None and '额外随机_1次_' in d and '额外随机_2次_' in d and '最高' in d


@case('E2', 'zh「骨骸编织」：写明「不叠加层数」＋上界随等级实时变化，旧 2~8 不得残留',
      mut_sub('act_zh', r'(actors\.hero\.talent\.bone_weaving\.desc=)[^\r\n]*',
              r'\1_+1：_ 增加 _2~8_ 的护甲值。'))
def e2(S):
    d = line_of(S, 'act_zh', 'actors.hero.talent.bone_weaving.desc')
    return d is not None and '0~当前角色等级' in d and '不叠加层数' in d \
        and '实时' in d and '2~8' not in d


@case('E3', 'zh「纹身铭刻」：补上 25%/50% 当前生命上限的「生命强化」',
      mut_drop('act_zh', r'actors\.hero\.talent\.tattoo_engraving\.desc=[^\r\n]*\r?\n'))
def e3(S):
    d = line_of(S, 'act_zh', 'actors.hero.talent.tattoo_engraving.desc')
    return d is not None and '25%' in d and '50%' in d and '生命强化' in d \
        and '当前生命上限' in d


@case('E4', 'zh「狩猎一餐」：恢复对象改成奥丁之眼，旧「剑术充能」不得残留',
      mut_sub('act_zh', r'(actors\.hero\.talent\.hunting_meal\.desc=)[^\r\n]*',
              r'\1_+1：_ 进食为剑术恢复充能。'))
def e4(S):
    d = line_of(S, 'act_zh', 'actors.hero.talent.hunting_meal.desc')
    return d is not None and '奥丁之眼' in d and '1.25点' in d and '剑术' not in d


@case('E5', 'en 四键齐备且语义同步（shifting_fate / bone_weaving / tattoo_engraving / hunting_meal）',
      mut_drop('act_en', r'actors\.hero\.talent\.shifting_fate\.desc=[^\r\n]*\r?\n'))
def e5(S):
    d1 = line_of(S, 'act_en', 'actors.hero.talent.shifting_fate.desc')
    d2 = line_of(S, 'act_en', 'actors.hero.talent.bone_weaving.desc')
    d3 = line_of(S, 'act_en', 'actors.hero.talent.tattoo_engraving.desc')
    d4 = line_of(S, 'act_en', 'actors.hero.talent.hunting_meal.desc')
    return all(x is not None for x in (d1, d2, d3, d4)) \
        and 'extra time' in d1 and "Odin's Eye" in d4 and 'max health boost' in d3 \
        and 'hero level' in d2 and 'no stacks' in d2 and 'recalculated live' in d2


@case('E6', 'en：shifting_fate / bone_weaving 两个**标题键**也在（原本只有 zh，易漏）',
      mut_drop('act_en', r'actors\.hero\.talent\.bone_weaving\.title=[^\r\n]*\r?\n'))
def e6(S):
    return line_of(S, 'act_en', 'actors.hero.talent.shifting_fate.title') is not None \
        and line_of(S, 'act_en', 'actors.hero.talent.bone_weaving.title') is not None


@case('E7', 'zh+en：新增 GLog 键 revengeledger.tattoo_ht（两个 %d 占位符都要在）',
      mut_drop('it_zh', r'items\.artifacts\.revengeledger\.tattoo_ht=[^\r\n]*\r?\n'))
def e7(S):
    zh = line_of(S, 'it_zh', 'items.artifacts.revengeledger.tattoo_ht')
    en = line_of(S, 'it_en', 'items.artifacts.revengeledger.tattoo_ht')
    return zh is not None and en is not None \
        and '%1$d' in zh and '%2$d' in zh and '%1$d' in en and '%2$d' in en


@case('E8', '四个 desc 都保留了字面 \\n\\n 分段（换行只走转义，不写真换行）',
      mut_sub('act_zh', r'(actors\.hero\.talent\.bone_weaving\.desc=)[^\r\n]*',
              r'\1单段无分段'))
def e8(S):
    for nm in ('actors.hero.talent.shifting_fate.desc',
               'actors.hero.talent.bone_weaving.desc',
               'actors.hero.talent.tattoo_engraving.desc',
               'actors.hero.talent.hunting_meal.desc'):
        d = line_of(S, 'act_zh', nm)
        if d is None or '\\n\\n' not in d:
            return False
    return True


# ============================================================ 跑
def run_cases(S, only=None):
    for cid, desc, fn, _mut in CASES:
        if only is not None and cid not in only:
            continue
        try:
            r = fn(S)
        except Exception:
            r = False
        chk(bool(r), '[%s] %s' % (cid, desc))


def main():
    print('=' * 78)
    print('四项二层天赋改写 · 核验（2026-09-26）')
    print('=' * 78)

    groups = [
        ('A. 神谕代行者 T2「变换无常」＝闪避掷点重掷取最高', 'A'),
        ('B. 拇指 T2「狩猎一餐」＝改补奥丁之眼充能', 'B'),
        ('C. 环指 T2「骨骸编织」＝无层数护盾 buff（0~角色等级，实时取）', 'C'),
        ('D. 中指 T2「纹身铭刻」＝附赠生命强化', 'D'),
        ('E. 文本 zh / en 双份同步', 'E'),
    ]
    S = load()
    for title, g in groups:
        print('\n-- %s --' % title)
        run_cases(S, only={c[0] for c in CASES if c[0].startswith(g)})

    print('\n' + '=' * 78)
    if ok:
        print('全部核验通过（%d 条）。' % len(results))
    else:
        print('有 %d 条与预期不符。' % sum(1 for r in results if not r))
    print('=' * 78)

    if '--selftest' in sys.argv:
        print('\n反例自测（每条判据喂一个「改坏」的变体，必须判 FAIL；不计总账）')
        bad = 0
        skipped = 0
        for cid, desc, fn, _mut in CASES:
            if _mut is None:
                print('  [SKIP] %s 无反例（全局扫描类）' % cid)
                skipped += 1
                continue
            S2 = load()
            try:
                _mut(S2)
            except AssertionError as e:
                print('  [SKIP] %s 反例变异未命中（%s）' % (cid, e))
                skipped += 1
                continue
            try:
                r = fn(S2)
            except Exception:
                r = False
            if r:
                print('  [FAIL] %s 反例仍判 PASS ⇒ 判据抓不到这类错' % cid)
                bad += 1
            else:
                print('  [OK]   %s 反例已判 FAIL（判据有效）' % cid)
        print('\n反例自测：%d 条判据，%d 条无反例，%d 条抓不到错。'
              % (len(CASES), skipped, bad))
        if bad:
            sys.exit(2)

    if not ok:
        sys.exit(1)


if __name__ == '__main__':
    main()
