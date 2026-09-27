package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Locale;

/**
 * 人体派作品武器基类：环指大师（RING_MASTER）专属近战武器。
 * - 数值用 MeleeWeapon 默认阶位公式（min=tier+lvl, max=5*(tier+1)+lvl*(tier+1)）
 * - 等级显示为字母制：0~6级 → F/D/C/B/A-/A/A+，负级回退默认 -N，超过 6 级封顶 A+
 * - 文本末尾固定追加"这是一件_人体派作品_"标注（参考 E.G.O 武器标注方式）
 * - KindOfWeapon 的装备入口会拒绝环指大师装备非本类武器
 * - 持有 4 个内置标记数值（Weapon/Bone/Meat/Blood），由「创作」合成时素材累加得到，
 *   需序列化存档否则读档归零（参考 AGENTS.md 存档兼容陷阱）。
 * - 四项数值影响（双曲递减公式，封顶 2.5×）：
 *   * Bone → 攻击倍率（乘算）：min/max ×（1 + dimReturn(bone)），封顶 2.5×
 *   * Meat → 攻击延迟（乘算）：baseDelay ×（1 /（1 + dimReturn(meat))），延迟下限 0.4× base
 *   * Blood → 精准（乘算）：accuracyFactor ×（1 + dimReturn(blood)），封顶 2.5×
 *   * Weapon → 仅决定合成等阶（见 WndBodyCraft），不影响战斗数值
 *   * dimReturn(v) = 1.5 × v / (|v| + 100)：边际收益递减，正数渐近 +1.5（封顶 2.5×）、负数渐近 -1.5
 */
public abstract class BodyArtWeapon extends MeleeWeapon {

	// 内置标记数值（合成时由素材相加得到）
	public int weaponValue = 0;
	public int boneValue   = 0;
	public int meatValue   = 0;
	public int bloodValue  = 0;

	// 附加附魔列表（艺术之巅「附魔」技艺叠加用）：主附魔槽（enchantment）之外，
	// 每次攻击在主附魔结算后依次触发这些附加附魔
	public final ArrayList<Weapon.Enchantment> extraEnchants = new ArrayList<>();

	private static final String WEAPON_VALUE = "weapon_value";
	private static final String BONE_VALUE   = "bone_value";
	private static final String MEAT_VALUE   = "meat_value";
	private static final String BLOOD_VALUE  = "blood_value";
	private static final String EXTRA_ENCHANTS = "extra_enchants";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( WEAPON_VALUE, weaponValue );
		bundle.put( BONE_VALUE,   boneValue );
		bundle.put( MEAT_VALUE,   meatValue );
		bundle.put( BLOOD_VALUE,  bloodValue );
		//附加附魔存类名数组（Enchantment 无自身字段，无需 Bundlable 序列化）
		String[] enchClasses = new String[extraEnchants.size()];
		for (int i = 0; i < extraEnchants.size(); i++){
			enchClasses[i] = extraEnchants.get(i).getClass().getName();
		}
		bundle.put( EXTRA_ENCHANTS, enchClasses );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		weaponValue = bundle.getInt( WEAPON_VALUE );
		boneValue   = bundle.getInt( BONE_VALUE );
		meatValue   = bundle.getInt( MEAT_VALUE );
		bloodValue  = bundle.getInt( BLOOD_VALUE );
		// 读档后重算等级：super.restoreFromBundle 会经 upgrade(level) 调用本类 upgrade()，
		// 在四项数值还是默认 0 时加了 +10×level 的 Weapon 值并调 syncReinforceLevel()，
		// 随后上面才读取真实存档值覆盖——此时等级仍是错误值，必须用真实 Weapon 值重算。
		syncReinforceLevel();
		//恢复附加附魔（旧存档/遗骸无此键时 getStringArray 返回 null，需跳过——否则下楼读遗骸 NPE）
		extraEnchants.clear();
		String[] enchClasses = bundle.getStringArray( EXTRA_ENCHANTS );
		if (enchClasses != null){
			for (String clsName : enchClasses){
				try {
					Class<?> cls = Class.forName( clsName );
					if (Weapon.Enchantment.class.isAssignableFrom( cls )){
						@SuppressWarnings("unchecked")
						Class<? extends Weapon.Enchantment> enchCls = (Class<? extends Weapon.Enchantment>) cls;
						extraEnchants.add( Reflection.newInstance( enchCls ) );
					}
				} catch (Exception e){
					GLog.w( "读档警告：附加附魔 %s 恢复失败。", clsName );
				}
			}
		}
	}

	/** 追加一个附加附魔（附魔叠加：主附魔之外同时生效）。 */
	public void addExtraEnchant( Weapon.Enchantment ench ){
		extraEnchants.add( ench );
	}

	@Override
	public int proc( Char attacker, Char defender, int damage ) {
		//主附魔（Weapon.proc 原版逻辑）
		int dmg = super.proc( attacker, defender, damage );
		//附加附魔依次结算（可叠加）
		for (Weapon.Enchantment e : extraEnchants.toArray( new Weapon.Enchantment[0] )){
			dmg = e.proc( this, attacker, defender, dmg );
		}
		return dmg;
	}

	// === 强化等级系统 ===
	// 升级卷轴等对人体派作品改为：+10 Weapon / +2 其他，不直接加等级
	// （super.upgrade 保留 Weapon 的附魔硬化/诅咒移除副作用，其 level++ 会被 sync 覆写）
	@Override
	public Item upgrade() {
		weaponValue += 10;
		boneValue   += 2;
		meatValue   += 2;
		bloodValue  += 2;
		super.upgrade();
		syncReinforceLevel();
		return this;
	}

	// 当前等阶的强化阈值 = 该阶创作时的起始 Weapon 值
	// （1/2阶30、3阶60、4阶90、5阶120、6阶150；Weapon 超过此值即开始累积等级）
	public int reinforceThreshold() {
		switch (tier) {
			case 1:
			case 2:  return 30;
			case 3:  return 60;
			case 4:  return 90;
			case 5:  return 120;
			case 6:  return 150;
			default: return Integer.MAX_VALUE;
		}
	}

	// 当前 Weapon 值应得的强化等级奖励（每超阈值 10 点 +1 级，下限 0）
	public int reinforceBonus() {
		return Math.max( 0, weaponValue - reinforceThreshold() ) / 10;
	}

	// 重新计算并应用强化等级：level = min(reinforceBonus, 6)（A+ 封顶）
	// 创作后、强化后、卷轴升级后调用，等级随 Weapon 值实时升降
	public void syncReinforceLevel() {
		int target = Math.min( reinforceBonus(), 6 );
		level( target );
	}

	// 满级（A+，等级上限 6）后视为不可升级：升级卷轴/魔法注能、铁匠升级与 WndUpgrade 多次升级
	// 均经 isUpgradable() 判定→不再选中，避免超出等级上限浪费升级资源。
	// 附魔（卷轴/符石）在 ScrollOfEnchantment.enchantable 中显式放行 BodyArtWeapon，与升级独立、不受影响。
	@Override
	public boolean isUpgradable() {
		if (level() >= 6) return false;
		return super.isUpgradable();
	}

	// === 边际收益递减公式（双曲函数）===
	// bonus = CAP × value / (|value| + K)
	// CAP=1.5（封顶 +150%）、K=100（value=100 时 bonus=0.75，与旧线性 1+v/100 在 v=150 处交叉）
	// 正数：收益随数值增长但渐近 CAP（封顶 2.5×）
	// 负数：惩罚也渐近 -CAP（不会无限恶化，下限 0.1× 钳制兜底）
	private static final float DIM_CAP = 1.5f;
	private static final float DIM_K   = 100f;

	private float dimReturn( int value ) {
		return DIM_CAP * value / ( Math.abs( value ) + DIM_K );
	}

	// Bone 值作为攻击倍率（乘算）作用于 min/max
	@Override
	public int min( int lvl ) {
		return Math.round( super.min( lvl ) * boneMultiplier() );
	}

	@Override
	public int max( int lvl ) {
		return Math.round( super.max( lvl ) * boneMultiplier() );
	}

	// 攻击倍率：双曲递减，封顶 2.5×（+150%）；下限 0.1× 避免负/零伤害
	// protected：具体子类覆写 min/max/damageRoll 时须手动套用（否则绕过基类倍率包装，
	// 曾导致"劣作-拼接骨矛"等自定义面板的 max 不受骨值加成）
	protected float boneMultiplier() {
		return Math.max( 0.1f, 1f + dimReturn( boneValue ) );
	}

	// Meat 攻速修正（双曲递减，封顶 2.5× 攻速）→ 转换为延迟乘数（1/攻速修正）
	private float meatDelayMultiplier() {
		float meatSpeedMod = 1f + dimReturn( meatValue );
		return 1f / meatSpeedMod;
	}

	// Meat 值降低攻击延迟（乘算），艺术批评 20% 为加法减法形式
	@Override
	protected float baseDelay( Char owner ) {
		float base = super.baseDelay( owner );
		float meatMod = meatDelayMultiplier();   // 乘算：base × meatMod
		float extraDelayMod = 0f;                 // 预留加法修正（默认0，供后续天赋/物品使用）
		float result = base * meatMod + extraDelayMod;
		// 艺术批评 +1：使用 F/D/C/B（0~3级）作品时 -20% base 的延迟（加法减法）
		if (owner instanceof Hero && artCritiqueApplies((Hero) owner, 0, 3)){
			result -= base * 0.2f;
		}
		return Math.max( 0.1f, result );
	}

	// Blood 值提升精准（双曲递减，封顶 2.5×）
	@Override
	public float accuracyFactor( Char owner, Char target ) {
		float base = super.accuracyFactor( owner, target );
		float factor = 1f + dimReturn( bloodValue );
		float result = base * factor;
		// 艺术批评 +1：使用 F/D/C/B（0~3级）作品时获得 20% 精准加成
		if (owner instanceof Hero && artCritiqueApplies((Hero) owner, 0, 3)){
			result *= 1.2f;
		}
		return result;
	}

	// 艺术批评 +2：使用 A-/A/A+（4~6级）作品时获得 20% 攻击伤害加成
	@Override
	public int damageRoll( Char owner ) {
		int dmg = super.damageRoll( owner );
		if (owner instanceof Hero && artCritiqueApplies((Hero) owner, 4, 6)){
			// 仅在 +2（2点天赋）时生效
			if (((Hero) owner).pointsInTalent(Talent.ART_CRITIQUE) >= 2){
				dmg = Math.round( dmg * 1.2f );
			}
		}
		return dmg;
	}

	/**
	 * 艺术批评天赋生效判定：英雄拥有该天赋、当前武器等级在 [min,max] 区间内。
	 * 评级对应：0=F、1=D、2=C、3=B、4=A-、5=A、6=A+
	 */
	private boolean artCritiqueApplies( Hero hero, int minLvl, int maxLvl ){
		if (!hero.hasTalent(Talent.ART_CRITIQUE)) return false;
		int lvl = level();
		return lvl >= minLvl && lvl <= maxLvl;
	}

	@Override
	public String info() {
		String info = super.info();
		// 追加攻击倍率/攻击延迟/精准修正显示（基础伤害已由 MeleeWeapon 显示，此处展示倍率来源）
		if (Dungeon.hero != null) {
			info += "\n\n" + "攻击倍率：_" + fmt( boneMultiplier() ) + "x_  攻击延迟：_" + fmt( baseDelay( Dungeon.hero ) ) + "x_  精准修正：_" + fmt( accuracyFactor( Dungeon.hero, null ) ) + "x_";
			info += "\n武器值:_" + weaponValue + "_  骨值:_" + boneValue + "_  肉值:_" + meatValue + "_  血值:_" + bloodValue + "_";
		}
		//附加附魔（附魔技艺叠加）显示，主附魔已由武器名称/原版 info 展示
		if (!extraEnchants.isEmpty()){
			StringBuilder sb = new StringBuilder( "附加附魔：_" );
			for (int i = 0; i < extraEnchants.size(); i++){
				if (i > 0) sb.append( "、");
				sb.append( extraEnchants.get(i).name( "" ) );
			}
			sb.append( "_" );
			info += "\n" + sb;
		}
		info += "\n\n" + "这是一件_人体派作品_";
		return info;
	}

	private String fmt( float val ) {
		return String.format( Locale.US, "%.2f", val );
	}

	@Override
	public String levelDisplay( int buffedLvl ) {
		if (buffedLvl < 0) {
			//降级走默认 -N 显示，字母制无法表达负数
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
