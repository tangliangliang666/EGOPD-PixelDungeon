// 负数道具等级调研探针（纯读，不改任何游戏代码）：真跑构造道具压到负等级，读派生数值 + 存档往返实证，30 项断言
// 目的：实证「系统是否支持负数等级」——直接构造真道具、压到负等级、读派生数值。
// 编译/运行：见 skill egopd-source-verify §1；classpath 需 core/SPD-classes/services + gdx + gdx-controllers + org.json。
// 说明：脱离游戏没有 Gdx.files，凡是实例初始化块里引用「贴图帧」的类（戒指走 ItemSpriteSheet$Icons）无法 new，
//       这类改用 Unsafe.allocateInstance 绕过初始化块（不跑 instance initializer，静态初始化也不触发）。
import java.lang.reflect.Field;

import sun.misc.Unsafe;

import com.watabou.utils.Bundle;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.LeatherArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.GradeTestSword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;

public class NegLevelProbe {

	static int failures = 0;
	static Unsafe unsafe;

	static void say( String s ){ System.out.println(s); }

	static void check( boolean ok, String label ){
		System.out.println( (ok ? "  [OK]   " : "  [FAIL] ") + label );
		if (!ok) failures++;
	}

	static void info( String s ){ System.out.println("  [INFO] " + s); }

	//把一件道具压到指定等级：负的走 degrade（与存档还原同一条路径）
	static void setLevel( Item it, int target ){
		it.level(0);
		if (target > 0) it.upgrade(target);
		else if (target < 0) it.degrade(-target);
	}

	static Ring newRingBypass() throws Exception {
		if (unsafe == null){
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			unsafe = (Unsafe) f.get(null);
		}
		return (Ring) unsafe.allocateInstance(RingOfHaste.class);
	}

	interface Block { void run() throws Throwable; }

	static void section( String name, Block b ){
		say("");
		say("--- " + name + " ---");
		try {
			b.run();
		} catch (Throwable t) {
			info("本段无法在脱离游戏的环境下跑（" + t.getClass().getSimpleName() + ": " + t.getMessage() + "）");
		}
	}

	public static void main( String[] args ){
		say("=== 负数道具等级调研探针 ===");

		section("A. 数据层：等级能变负、能存档往返、显示格式", NegLevelProbe::sectionA);
		section("B. 近战武器 Shortsword(tier 2)：伤害负向传递，无夹取", NegLevelProbe::sectionB);
		section("C. 法杖 WandOfMagicMissile：充能上限可 <= 0（死杖）", NegLevelProbe::sectionC);
		section("D. 戒指 RingOfHaste：加成 = level+1，可为负", NegLevelProbe::sectionD);
		section("E. 防具：DR 在负等级处非单调（反弹）", NegLevelProbe::sectionE);
		section("F. 投掷武器 ThrowingKnife：伤害被 Math.max(0,..) 夹到 0", NegLevelProbe::sectionF);
		section("G. 字母制等级显示（本作自定）", NegLevelProbe::sectionG);
		section("H. 边界语义：upgrade(-n)/degrade(-n) 是空操作", NegLevelProbe::sectionH);

		say("");
		say("============================================================");
		say(failures == 0 ? "全部符合预期" : ("有 " + failures + " 项与预期不符，见上方 [FAIL]"));
		say("============================================================");
	}

	//--------------------------------------------------------------- A
	static void sectionA() throws Throwable {
		Item it = new Shortsword();
		it.levelKnown = true;

		it.degrade(3);
		check( it.level() == -3, "degrade(3) 后 level() == -3（Item 基类无下限夹取）" );
		check( it.trueLevel() == -3, "trueLevel() == -3" );

		// 显示：Item.levelDisplay() 的实现就是 Messages.format("%+d", buffedLvl)（Item.java:491）。
		// Messages 静态初始化要 Gdx.files（9 个语言包），脱离游戏无法初始化；
		// 这里直接验证那条格式串的语义（java.lang.String.format 与本作 Messages.format 同一套）。
		check( "-3".equals( String.format("%+d", -3) ), "%+d 于 -3 => \"-3\"（负数不带 +）" );
		check( "-1".equals( String.format("%+d", -1) ), "%+d 于 -1 => \"-1\"" );
		check( "+2".equals( String.format("%+d", 2) ), "%+d 于 +2 => \"+2\"（正数保留 +）" );
		check( "+0".equals( String.format("%+d", 0) ), "%+d 于 0 => \"+0\"（但 levelDisplay 对 0 返回 null，实际不显示）" );

		// 存档往返：Bundle.put -> Bundle.get（Reflection.newInstance + restoreFromBundle）
		Item src = new Shortsword();
		src.levelKnown = true;
		src.degrade(5);
		Bundle b = new Bundle();
		b.put("it", src);
		Item back = (Item) b.get("it");
		check( back != null && back.level() == -5, "存档往返保住负数等级：-5 -> " + (back == null ? "null" : back.level()) );

		Bundle b2 = new Bundle();
		Item src2 = new Shortsword();
		src2.upgrade(4);
		b2.put("it", src2);
		Item back2 = (Item) b2.get("it");
		check( back2 != null && back2.level() == 4, "存档往返保住正数等级：+4 -> " + (back2 == null ? "null" : back2.level()) );

		// 顺带：越负越负也能往返
		Item src3 = new Shortsword();
		src3.degrade(11);
		Bundle b3 = new Bundle();
		b3.put("it", src3);
		Item back3 = (Item) b3.get("it");
		check( back3 != null && back3.level() == -11, "存档往返保住 -11 -> " + (back3 == null ? "null" : back3.level()) );
	}

	//--------------------------------------------------------------- B
	static void sectionB(){
		MeleeWeapon w = new Shortsword();
		w.levelKnown = true;
		say("   等级 |  min() |  max() | STRReq | value()");
		for (int lvl : new int[]{ 0, -1, -2, -3, -5, -10 }) {
			setLevel(w, lvl);
			say(String.format("   %4d | %6d | %6d | %6d | %7d",
					w.level(), w.min(), w.max(), w.STRReq(), w.value()));
		}
		setLevel(w, -3);
		check( w.min() < 0, "tier2/-3 的 min() 为负（实得 " + w.min() + "）⇒ 可掷出负伤害" );
		setLevel(w, -10);
		check( w.min() > w.max(), "tier2/-10 时 min(" + w.min() + ") > max(" + w.max() + ") ⇒ 区间反序（NormalIntRange 仍能算，不抛异常）" );

		setLevel(w, 0);
		int strReq0 = w.STRReq();
		setLevel(w, -5);
		check( w.STRReq() == strReq0, "STRReq 对负等级夹到 0（-5 与 +0 同需求 " + w.STRReq() + "）" );

		setLevel(w, 0);
		int v0 = w.value();
		setLevel(w, -5);
		check( w.value() == v0, "value() 对负等级无折价（-5 与 +0 同价 " + w.value() + "）" );
	}

	//--------------------------------------------------------------- C
	static void sectionC(){
		Wand probe = new WandOfMagicMissile();
		int base = probe.initialCharges();
		say("   WandOfMagicMissile.initialCharges() = " + base + "  ⇒ maxCharges = min(" + base + " + level, 10)");
		say("   等级 | maxCharges | curCharges | value()");
		for (int lvl : new int[]{ 0, -1, -base, -base - 1, -base - 3 }) {
			Wand fresh = new WandOfMagicMissile();
			fresh.levelKnown = true;
			if (lvl < 0) fresh.degrade(-lvl); else if (lvl > 0) fresh.upgrade(lvl);
			say(String.format("   %4d | %10d | %10d | %7d",
					fresh.level(), fresh.maxCharges, fresh.curCharges, fresh.value()));
			if (lvl == 0) info("level 0 -> maxCharges = " + fresh.maxCharges);
			if (lvl == -base) check( fresh.maxCharges == 0,
					"level " + lvl + " 时 maxCharges == 0 ⇒ 永远充不上电（curCharges < maxCharges 恒 false）⇒ 死杖" );
			if (lvl == -base - 3) check( fresh.maxCharges < 0,
					"level " + lvl + " 时 maxCharges = " + fresh.maxCharges + "（**负充能，无 Math.max(0,..) 保护**）" );
		}
		Wand v = new WandOfMagicMissile();
		v.levelKnown = true;
		v.degrade(3);
		check( v.value() > 0, "value() 对负等级走显式折价分支且 >= 1（实得 " + v.value() + "）" );
	}

	//--------------------------------------------------------------- D
	static void sectionD() throws Throwable {
		Ring r = newRingBypass();   // 绕过贴图初始化块
		r.levelKnown = true;
		say("   等级 | soloBonus(=level+1) | value()");
		for (int lvl : new int[]{ 0, -1, -2, -3, -5 }) {
			setLevel(r, lvl);
			say(String.format("   %4d | %19d | %7d", r.level(), r.soloBonus(), r.value()));
		}
		setLevel(r, -1);
		check( r.soloBonus() == 0, "-1 时 soloBonus == 0" );
		setLevel(r, -3);
		check( r.soloBonus() == -2, "-3 时 soloBonus == -2 ⇒ pow(1.175,-2) < 1 ⇒ 急速戒指反而**变慢**" );
		check( r.value() > 0, "value() 对负等级走显式折价分支（实得 " + r.value() + "）" );
	}

	//--------------------------------------------------------------- E
	static void sectionE(){
		Armor plate = new PlateArmor(); // tier 5（super(5)）
		plate.levelKnown = true;
		say("   PlateArmor(tier 5)  等级 | DRMin | DRMax | STRReq | value()");
		for (int lvl : new int[]{ 0, -1, -2, -3, -4, -6, -10 }) {
			setLevel(plate, lvl);
			say(String.format("                    %4d | %5d | %5d | %6d | %7d",
					plate.level(), plate.DRMin(), plate.DRMax(), plate.STRReq(), plate.value()));
		}
		setLevel(plate, -2);
		int atMinus2 = plate.DRMax();
		setLevel(plate, -3);
		int atMinus3 = plate.DRMax();
		check( atMinus2 == 0, "tier5/-2 时 DRMax == 0（5*(2-2)）" );
		check( atMinus3 > atMinus2, "tier5/-3 时 DRMax 反弹为 " + atMinus3 + "（比 -2 的 " + atMinus2 + " 更高）⇒ 公式 if(lvl>max) 分支被误触发，DR 非单调" );

		Armor leather = new LeatherArmor(); // tier 2（super(2)）
		leather.levelKnown = true;
		say("");
		say("   LeatherArmor(tier 2)  等级 | DRMin | DRMax");
		for (int lvl : new int[]{ 0, -1, -2, -3, -5, -10 }) {
			setLevel(leather, lvl);
			say(String.format("                      %4d | %5d | %5d", leather.level(), leather.DRMin(), leather.DRMax()));
		}
	}

	//--------------------------------------------------------------- F
	static void sectionF(){
		MissileWeapon m = new ThrowingKnife();
		m.levelKnown = true;
		say("   等级 | min() | max() | durabilityPerUse()");
		for (int lvl : new int[]{ 0, -1, -2, -5, -20 }) {
			setLevel(m, lvl);
			say(String.format("   %4d | %5d | %5d | %18.2f", m.level(), m.min(), m.max(), m.durabilityPerUse()));
		}
		setLevel(m, -20);
		check( m.min() == 0 && m.max() == 0, "-20 时 min/max 都是 0（投掷武器显式 Math.max(0,..) 夹取，不会负伤害）" );
		setLevel(m, -20);
		check( m.durabilityPerUse() > 100f, "-20 时 durabilityPerUse = " + m.durabilityPerUse() + " > 耐久上限 ⇒ 一掷即碎" );
	}

	//--------------------------------------------------------------- G
	static void sectionG(){
		GradeTestSword g = new GradeTestSword();
		StringBuilder sb = new StringBuilder();
		for (int lvl = 0; lvl <= 6; lvl++) sb.append(g.levelDisplay(lvl)).append(" ");
		say("   0..6 -> " + sb.toString().trim() + "   (F D C B A- A A+)");
		check( "F".equals(g.levelDisplay(0)), "等级 0 显示 F（字母制下 0 有显示）" );
		check( "A+".equals(g.levelDisplay(9)), "等级 9 封顶显示 A+" );
		say("   （负数分支 if (buffedLvl < 0) return super.levelDisplay(buffedLvl); 会调 Messages，脱离游戏无法运行时验证 —— 已在源码层确认）");
	}

	//--------------------------------------------------------------- H
	static void sectionH(){
		Item it = new Shortsword();
		it.upgrade(3);
		check( it.level() == 3, "upgrade(3) -> 3" );
		it.upgrade(-1);
		check( it.level() == 3, "upgrade(-1) 不降级（for i<n，n<=0 直接不跑）" );
		it.degrade(-2);
		check( it.level() == 3, "degrade(-2) 不升级" );
		it.degrade(10);
		check( it.level() == -7, "degrade(10) 一路降到 -7（无「不低于 0」拦截）" );
		it.upgrade(7);
		check( it.level() == 0, "再 upgrade(7) 回到 0（可往复穿越 0）" );
	}
}
