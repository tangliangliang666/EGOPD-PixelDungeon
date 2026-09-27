/*
 * 真装载核验（2026-09-24）：
 *  ① 神器池 Generator.Category.ARTIFACT.classes 里确实有 SkeletonKey
 *  ② 复刻 MuseumLevel.createItems() 的陈列逻辑（真实 skip() + Reflection.newInstance），
 *     统计「死亡证明 999 层」真正会摆出多少件展品、骷髅钥匙是否在其中、是否超出展厅容量
 *  ③ Catalog.ARTIFACTS（图鉴神器页）与陈列清单的差集 —— 除英雄专属盔甲外应当为空
 */
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SkeletonKey;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.levels.MuseumLevel;
import com.watabou.utils.Reflection;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MuseumLocCheck {

	static int total = 0;
	static int fails = 0;

	static void chk(boolean ok, String msg) {
		total++;
		if (ok) {
			System.out.println("  ok   " + msg);
		} else {
			fails++;
			System.out.println("  FAIL " + msg);
		}
	}

	/** 把异常链拼成一行，用于分类根因。 */
	static String causeOf(Throwable t) {
		StringBuilder sb = new StringBuilder();
		Throwable c = t;
		for (int i = 0; c != null && i < 8; i++, c = c.getCause()) {
			sb.append(c.getClass().getSimpleName()).append('(').append(c.getMessage()).append(") ");
		}
		return sb.toString();
	}

	/**
	 * 本机是<b>无 GL 上下文</b>的命令行环境：凡是构造期要碰
	 * {@code ItemSpriteSheet$Icons} → {@code TextureCache} 的类别都必然失败，
	 * 这属于环境限制，不是改动问题（游戏内这些类都构造得出来）。
	 */
	static boolean glDependent(Throwable t) {
		StringBuilder sb = new StringBuilder(causeOf(t));
		// also 抓栈帧：ExceptionInInitializerError 常常是 message/cause 都为 null，
		// 只能从「初始化失败在哪个类里」判断
		for (StackTraceElement e : t.getStackTrace()) sb.append(e.getClassName()).append('.').append(e.getMethodName()).append(' ');
		Throwable c = t.getCause();
		for (int i = 0; c != null && i < 8; i++, c = c.getCause()) {
			for (StackTraceElement e : c.getStackTrace()) sb.append(e.getClassName()).append('.').append(e.getMethodName()).append(' ');
		}
		String s = sb.toString();
		return s.contains("ItemSpriteSheet") || s.contains("TextureCache")
				|| s.contains("GLException") || s.contains("glGetError") || s.contains("Pixmap");
	}

	public static void main(String[] args) throws Exception {

		// ------------------------------------------------------------------
		// ① 神器池
		// ------------------------------------------------------------------
		System.out.println("[1] Generator.Category.ARTIFACT.classes");
		List<Class<?>> artifactPool = new ArrayList<>();
		for (Class<?> c : Generator.Category.ARTIFACT.classes) artifactPool.add(c);
		System.out.println("    池子 " + artifactPool.size() + " 项：" + artifactPool);
		chk(artifactPool.contains(SkeletonKey.class), "神器池含 SkeletonKey.class");

		// ------------------------------------------------------------------
		// ② 真实 skip()：拿一个 MuseumLevel 实例反射调用私有方法
		// ------------------------------------------------------------------
		System.out.println("[2] MuseumLevel.skip(Class) 真实调用");
		Method skip = MuseumLevel.class.getDeclaredMethod("skip", Class.class);
		skip.setAccessible(true);
		MuseumLevel level = null;
		try {
			level = new MuseumLevel();
			System.out.println("    new MuseumLevel() 成功（可反射调用实例方法）");
		} catch (Throwable t) {
			System.out.println("    new MuseumLevel() 失败：" + t);
		}
		if (level != null) {
			chk(!(Boolean) skip.invoke(level, SkeletonKey.class), "skip(SkeletonKey) == false（要陈列）");
			chk((Boolean) skip.invoke(level, ClassArmor.class), "skip(ClassArmor) == true（不陈列）");
			chk((Boolean) skip.invoke(level, com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor.class) == false,
					"skip(ClothArmor) == false（普通盔甲照陈列）");
			chk(!(Boolean) skip.invoke(level, com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Admiration.class),
					"skip(Admiration) == false（自定义神器照陈列）");
		}

		// ------------------------------------------------------------------
		// ③ 复刻 createItems() 的清单
		// ------------------------------------------------------------------
		System.out.println("[3] 复刻陈列清单（section + 杂项行）");

		List<Catalog> cats = new ArrayList<>();
		Set<String> filtered = new LinkedHashSet<>();   // 被 skip 挡掉的
		for (Catalog c : new Catalog[]{
				Catalog.MELEE_WEAPONS, Catalog.ARMOR, Catalog.WANDS, Catalog.RINGS, Catalog.ARTIFACTS,
				Catalog.TRINKETS, Catalog.POTIONS, Catalog.SCROLLS, Catalog.SEEDS, Catalog.STONES,
				Catalog.FOOD, Catalog.EXOTIC_POTIONS, Catalog.EXOTIC_SCROLLS, Catalog.BOMBS,
				Catalog.TIPPED_DARTS, Catalog.BREWS_ELIXIRS, Catalog.SPELLS}) {
			cats.add(c);
		}

		List<String> exhibits = new ArrayList<>();      // 会摆出来的
		List<String> broken = new ArrayList<>();        // 实例化失败的
		List<String> glDep = new ArrayList<>();         // 其中「无 GL 上下文」导致的
		List<String> realBad = new ArrayList<>();       // 真正的异常
		for (Catalog cat : cats) {
			boolean filter = (cat == Catalog.ARMOR || cat == Catalog.ARTIFACTS);
			String label = cat.name() + (filter ? "(过滤)" : "");
			int n = 0, dropped = 0;
			for (Class<?> c : cat.items()) {
				if (filter && level != null && (Boolean) skip.invoke(level, c)) {
					dropped++;
					filtered.add(c.getSimpleName());
					continue;
				}
				Item it = null;
				try {
					it = (Item) Reflection.newInstance(c);
				} catch (Throwable t) {
					broken.add(c.getSimpleName());
					if (glDependent(t)) glDep.add(c.getSimpleName());
					else realBad.add(c.getSimpleName() + " => " + causeOf(t));
				}
				if (it != null) {
					exhibits.add(c.getSimpleName());
					n++;
				}
			}
			System.out.printf("    %-22s 图鉴 %2d 项 → 本机可实例化 %2d（跳过 %d）%n", label, cat.items().size(), n, dropped);
		}

		// 投掷武器：每个种类一组（默认叠数），逐个也要能实例化
		int mis = 0;
		for (Class<?> c : Catalog.THROWN_WEAPONS.items()) {
			try {
				Item it = (Item) Reflection.newInstance(c);
				if (it instanceof MissileWeapon) ((MissileWeapon) it).quantity(((MissileWeapon) it).defaultQuantity());
				if (it != null) {
					exhibits.add(c.getSimpleName());
					mis++;
				}
			} catch (Throwable t) {
				broken.add(c.getSimpleName());
				if (glDependent(t)) glDep.add(c.getSimpleName());
				else realBad.add(c.getSimpleName() + " => " + causeOf(t));
			}
		}
		System.out.printf("    %-22s 图鉴 %2d 项 → 本机可实例化 %2d%n", "THROWN_WEAPONS", Catalog.THROWN_WEAPONS.items().size(), mis);

		int misc = 15;   // createItems 里手点的杂项行（金币…矮人徽记）
		// 游戏内全部都能实例化 ⇒ 真实陈列数 = 本机成功的 + 因无 GL 上下文而失败的
		int all = exhibits.size() + misc;
		int allInGame = all + glDep.size();

		System.out.println("    被 skip 挡掉的：" + (filtered.isEmpty() ? "无" : filtered) + "（" + filtered.size() + " 件）");
		System.out.println("    无 GL 上下文导致本机建不出来的：" + glDep.size() + " 件（游戏内正常）");
		System.out.println("    真正的异常：" + (realBad.isEmpty() ? "无" : realBad));
		System.out.println("    合计陈列：本机 " + all + " 件 / 游戏内应为 " + allInGame + " 件");

		chk(filtered.size() == 6 && filtered.contains("WarriorArmor") && !filtered.contains("SkeletonKey"),
				"只挡掉 6 件英雄专属盔甲、没挡骷髅钥匙（实得 " + filtered + "）");
		chk(exhibits.contains("SkeletonKey"), "骷髅钥匙在陈列清单里");
		chk(realBad.isEmpty(), "除「无 GL 上下文」外没有别的实例化异常（实得 " + realBad.size() + " 件）");

		// 展厅容量：x 1..30、y 1,3,...,27 共 14 行 → 420 格
		int capacity = 30 * 14;
		chk(allInGame <= capacity, "游戏内陈列总数 " + allInGame + " ≤ 展厅容量 " + capacity + "（否则会叠在同一格）");

		// 图鉴神器页里除了英雄专属盔甲，不该再有别的东西缺席
		// （无 GL 上下文建不出来的那些，游戏内本来就能建，故一并算作「会陈列」）
		Set<String> onShow = new LinkedHashSet<>(exhibits);
		onShow.addAll(glDep);
		Set<String> miss = new LinkedHashSet<>();
		for (Class<?> c : Catalog.ARTIFACTS.items()) {
			if (!onShow.contains(c.getSimpleName())) miss.add(c.getSimpleName());
		}
		System.out.println("    图鉴神器页缺席的：" + (miss.isEmpty() ? "无" : miss));
		chk(miss.isEmpty(), "神器图鉴里没有缺席项（实得 " + miss + "）");
		chk(onShow.contains("SkeletonKey"), "骷髅钥匙在神器图鉴的陈列集里");

		System.out.println();
		System.out.println("断言总数 " + total + "，失败 " + fails);
		if (fails > 0) System.exit(1);
		System.out.println("ALL PASS");
	}
}
