# -*- coding: utf-8 -*-
"""
一次性补丁：2026-09-20
A) HermesCaduceus: bones = false（死亡后不进英雄遗骸，避免其他角色借遗骸获取）
B) FirelinkGreatsword: 跨局继承由「只给 +0 白板」升级为「原等级/5（向下取整）+ 附魔」
   - SPDSettings 新增 firelink_level / firelink_enchant 两个键
   - items_zh / items 的 stats_desc 同步说明

约定：先做「按前缀整行替换」（行级，不改变行数），再做「块替换」；
所有 old 串必须命中恰好 1 次，否则整体放弃（原子落盘）。
"""
import os
import shutil
import sys

ROOT = r"D:\PD"
BAK = os.path.join(ROOT, "_chk", "_bak_2026-09-20c")

FILES = {
    "hermes": os.path.join(ROOT, r"core\src\main\java\com\shatteredpixel\shatteredpixeldungeon\items\weapon\melee\HermesCaduceus.java"),
    "firelink": os.path.join(ROOT, r"core\src\main\java\com\shatteredpixel\shatteredpixeldungeon\items\weapon\melee\FirelinkGreatsword.java"),
    "settings": os.path.join(ROOT, r"core\src\main\java\com\shatteredpixel\shatteredpixeldungeon\SPDSettings.java"),
    "items_zh": os.path.join(ROOT, r"core\src\main\assets\messages\items\items_zh.properties"),
    "items_en": os.path.join(ROOT, r"core\src\main\assets\messages\items\items.properties"),
}

problems = []


def read_text(path):
    with open(path, "rb") as f:
        raw = f.read()
    nl = "\r\n" if b"\r\n" in raw else "\n"
    return raw.decode("utf-8"), nl


def backup(path):
    os.makedirs(BAK, exist_ok=True)
    dst = os.path.join(BAK, os.path.basename(path))
    if not os.path.exists(dst):
        shutil.copy2(path, dst)


def sub_block(path, old, new, tag):
    """块替换：old/new 用 \n 书写，按文件实际换行落地。"""
    text, nl = read_text(path)
    o = old.replace("\n", nl)
    n = new.replace("\n", nl)
    cnt = text.count(o)
    if cnt != 1:
        problems.append("[%s] 命中 %d 次（应为 1）：%r" % (tag, cnt, o[:70]))
        return False
    backup(path)
    text = text.replace(o, n)
    if nl == "\n":
        text = text.replace("\r\n", "\n")
    with open(path, "wb") as f:
        f.write(text.encode("utf-8"))
    print("  OK 块替换 %s" % tag)
    return True


def sub_line(path, key, new_line, tag):
    """按前缀整行替换（properties 专用）：new_line 里的 \\n 必须是字面量转义。"""
    text, nl = read_text(path)
    if "\n" in new_line or "\r" in new_line:
        problems.append("[%s] 新行含真换行，会断行！" % tag)
        return False
    lines = text.split(nl)
    hits = [i for i, l in enumerate(lines) if l.startswith(key + "=")]
    if len(hits) != 1:
        problems.append("[%s] 键 %s 命中 %d 行（应为 1）" % (tag, key, len(hits)))
        return False
    i = hits[0]
    if lines[i] == new_line:
        print("  -- 行替换 %s 已是目标内容（幂等）" % tag)
        return True
    backup(path)
    print("  旧: %s" % lines[i])
    lines[i] = new_line
    print("  新: %s" % new_line)
    with open(path, "wb") as f:
        f.write(nl.join(lines).encode("utf-8"))
    print("  OK 行替换 %s" % tag)
    return True


# ============================ A. HermesCaduceus ============================
print("[A] HermesCaduceus.java")
sub_block(
    FILES["hermes"],
    """ * <p>面板仍显示五阶占位数值；实际攻击数值随随机形态变化。</p>
 */""",
    """ * <p>面板仍显示五阶占位数值；实际攻击数值随随机形态变化。</p>
 * <p>专属武器：{@code bones = false}，死亡后**不**进入英雄遗骸——否则下一局其它角色
 * 拾取遗骸就可能拿到这件神谕代行者专属武器。</p>
 */""",
    "hermes.javadoc",
)
sub_block(
    FILES["hermes"],
    """		tier = 5; //面板等阶5；攻击数值由随机形态决定
	}""",
    """		tier = 5; //面板等阶5；攻击数值由随机形态决定

		//专属武器：不进英雄遗骸（EquipableItem 默认 bones = true，这里必须显式关掉）
		bones = false;
	}""",
    "hermes.bones",
)

# ============================ B1. FirelinkGreatsword ============================
print("[B1] FirelinkGreatsword.java")
sub_block(
    FILES["firelink"],
    """ * 通关继承：胜利通关时若装备此剑，下一局开局将持有 +0 的此剑（死亡不触发）。""",
    """ * 通关继承：胜利通关时若装备此剑，下一局开局将持有此剑，并继承原剑强化等级的
 * 1/{@link #INHERIT_LEVEL_DIVISOR}（向下取整）与附魔（死亡不触发）。""",
    "firelink.javadoc",
)
sub_block(
    FILES["firelink"],
    """import com.watabou.utils.DeviceCompat;""",
    """import com.watabou.utils.DeviceCompat;
import com.watabou.utils.Reflection;""",
    "firelink.import",
)
sub_block(
    FILES["firelink"],
    """	// ---- 通关继承：胜利通关时装备 → 下一局开局持有 +0；死亡不触发 ----

	public static boolean equippedBy(Hero hero) {
		return hero.belongings.weapon() instanceof FirelinkGreatsword
				|| hero.belongings.secondWep() instanceof FirelinkGreatsword;
	}

	//胜利点（SewerLevel 上地面切换到胜利场景前）调用
	public static void markNextRun() {
		SPDSettings.firelinkNextRun(true);
	}

	//Dungeon.init 开局初始化（initHero）之后调用：有继承标记则发放 +0 并清除标记
	public static void grantAtRunStart() {
		DeviceCompat.log("FIRELINK", "grantAtRunStart: flag=" + SPDSettings.firelinkNextRun());
		if (!SPDSettings.firelinkNextRun()) return;
		SPDSettings.firelinkNextRun(false);
		Item firelink = new FirelinkGreatsword();
		firelink.identify();
		if (firelink.collect(Dungeon.hero.belongings.backpack)){
			DeviceCompat.log("FIRELINK", "grantAtRunStart: granted to backpack");
			GLog.p("上一位薪王的余烬化作新的火焰：你获得了_传火大剑_。");
		} else {
			DeviceCompat.log("FIRELINK", "grantAtRunStart: collect failed!");
		}
	}""",
    """	// ---- 通关继承：胜利通关时装备 → 下一局开局持有，并继承「原剑等级/5（向下取整）+ 附魔」；死亡不触发 ----

	//继承的强化等级 = 原剑等级 / 本常量（整数除法天然向下取整；等级非负，无负数取整问题）
	public static final int INHERIT_LEVEL_DIVISOR = 5;

	public static boolean equippedBy(Hero hero) {
		return equippedInstance(hero) != null;
	}

	//英雄当前装备的传火大剑（主手优先，其次副手）；未装备返回 null
	public static FirelinkGreatsword equippedInstance(Hero hero) {
		if (hero == null) return null;
		if (hero.belongings.weapon() instanceof FirelinkGreatsword) {
			return (FirelinkGreatsword) hero.belongings.weapon();
		}
		if (hero.belongings.secondWep() instanceof FirelinkGreatsword) {
			return (FirelinkGreatsword) hero.belongings.secondWep();
		}
		return null;
	}

	//胜利点（SewerLevel 上地面切换到胜利场景前 / Amulet 使用护符结束游戏）调用：
	//置「下一局继承」标记，并把原剑的强化等级与附魔类名一并写进设置
	public static void markNextRun() {
		FirelinkGreatsword wep = equippedInstance(Dungeon.hero);
		int srcLevel = wep != null ? wep.level() : 0;
		String enchantClass = "";
		if (wep != null && wep.enchantment != null) {
			enchantClass = wep.enchantment.getClass().getName();
		}
		SPDSettings.firelinkNextRun(true);
		SPDSettings.firelinkLevel(srcLevel);
		SPDSettings.firelinkEnchant(enchantClass);
		DeviceCompat.log("FIRELINK", "markNextRun: srcLevel=" + srcLevel + " enchant=" + enchantClass);
	}

	//Dungeon.init 开局初始化（initHero）之后调用：有继承标记则发放并清除标记
	public static void grantAtRunStart() {
		DeviceCompat.log("FIRELINK", "grantAtRunStart: flag=" + SPDSettings.firelinkNextRun());
		if (!SPDSettings.firelinkNextRun()) return;
		SPDSettings.firelinkNextRun(false);

		//取出记录并立即清零，避免残留到再下一局
		int srcLevel = SPDSettings.firelinkLevel();
		String enchantClass = SPDSettings.firelinkEnchant();
		SPDSettings.firelinkLevel(0);
		SPDSettings.firelinkEnchant("");

		FirelinkGreatsword firelink = new FirelinkGreatsword();
		int inheritLevel = srcLevel / INHERIT_LEVEL_DIVISOR;
		if (inheritLevel > 0) firelink.upgrade(inheritLevel);
		Enchantment ench = enchantFromName(enchantClass);
		if (ench != null) firelink.enchant(ench);
		firelink.identify();

		if (firelink.collect(Dungeon.hero.belongings.backpack)){
			DeviceCompat.log("FIRELINK", "grantAtRunStart: granted to backpack, lvl=" + firelink.level()
					+ " enchant=" + (firelink.enchantment != null
							? firelink.enchantment.getClass().getSimpleName() : "none"));
			GLog.p("上一位薪王的余烬化作新的火焰：你获得了_传火大剑_。");
		} else {
			DeviceCompat.log("FIRELINK", "grantAtRunStart: collect failed!");
		}
	}

	//按类名反射重建附魔；空名 / 类不存在 / 不是附魔 / 构造失败 一律返回 null（静默退回无附魔）
	private static Enchantment enchantFromName(String className) {
		if (className == null || className.isEmpty()) return null;
		Class<?> cls = Reflection.forName(className);
		if (cls == null || !Enchantment.class.isAssignableFrom(cls)) return null;
		try {
			Object o = Reflection.newInstance(cls);
			if (o instanceof Enchantment) return (Enchantment) o;
		} catch (Exception e) {
			DeviceCompat.log("FIRELINK", "enchantFromName failed: " + className);
		}
		return null;
	}""",
    "firelink.inherit",
)

# ============================ B2. SPDSettings ============================
print("[B2] SPDSettings.java")
sub_block(
    FILES["settings"],
    """	public static boolean firelinkNextRun(){
		return getBoolean( KEY_FIRELINK_NEXT_RUN, false );
	}""",
    """	public static boolean firelinkNextRun(){
		return getBoolean( KEY_FIRELINK_NEXT_RUN, false );
	}

	//通关时原剑的强化等级（继承时按「等级/5 向下取整」发放；发放后清零）
	public static final String KEY_FIRELINK_LEVEL = "firelink_level";
	public static void firelinkLevel( int value ){
		put( KEY_FIRELINK_LEVEL, value );
	}
	public static int firelinkLevel(){
		return getInt( KEY_FIRELINK_LEVEL, 0 );
	}

	//通关时原剑的附魔类全名（继承时反射重建；无附魔为空串，发放后清零）
	public static final String KEY_FIRELINK_ENCHANT = "firelink_enchant";
	public static void firelinkEnchant( String value ){
		put( KEY_FIRELINK_ENCHANT, value == null ? "" : value );
	}
	public static String firelinkEnchant(){
		return getString( KEY_FIRELINK_ENCHANT, "" );
	}""",
    "settings.keys",
)

# ============================ B3. 文本 ============================
print("[B3] stats_desc 文本")
KEY = "items.weapon.melee.firelinkgreatsword.stats_desc"
sub_line(
    FILES["items_zh"], KEY,
    KEY + "=这把武器带有_烈焰附魔_的效果。\\n\\n如果通关时装备了这把武器，那么下一局游戏开始时将持有这把武器，"
    "并继承其原先强化等级的五分之一（向下取整）与附魔。",
    "items_zh.stats_desc",
)
sub_line(
    FILES["items_en"], KEY,
    KEY + "=This weapon carries the effect of a _flaming enchantment_.\\n\\n"
    "If you win the game with this weapon equipped, you will start the next run holding it, "
    "keeping a fifth of its upgrade level (rounded down) and its enchantment.",
    "items_en.stats_desc",
)

print()
if problems:
    print("!!! 存在问题，未全部完成：")
    for p in problems:
        print("   -", p)
    sys.exit(1)
print("ALL DONE（备份目录 %s）" % BAK)
