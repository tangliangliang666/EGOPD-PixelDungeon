package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

/**
 * 验证用武器：完全套用短剑（数值/贴图/武技均继承自 Shortsword），
 * 仅覆写等级显示为字母制以验证 levelDisplay 机制。
 *
 * 等级映射（0~6）：F / D / C / B / A- / A / A+
 * - 0 级显示 F（默认物品不显示，字母制下显示基础等级）
 * - 负等级（降级）回退默认 "+/-N" 显示
 * - 超过 6 级封顶显示 A+
 */
public class GradeTestSword extends Shortsword {

	@Override
	public String levelDisplay( int buffedLvl ) {
		if (buffedLvl < 0) {
			//降级走默认 -N 显示，避免字母制无法表达负数
			return super.levelDisplay( buffedLvl );
		}
		switch (buffedLvl) {
			case 0:  return "F";
			case 1:  return "D";
			case 2:  return "C";
			case 3:  return "B";
			case 4:  return "A-";
			case 5:  return "A";
			case 6:  return "A+";
			default: return buffedLvl > 6 ? "A+" : super.levelDisplay( buffedLvl );
		}
	}
}
