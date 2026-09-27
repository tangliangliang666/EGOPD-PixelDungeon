/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite.Glowing;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * 蜚蠊附魔（2026-09-10 用户新增）。
 * <p>棕色光效。随时间在攻击者身上积累「{@link AttackVermin 攻击害虫}」，
 * 下次命中时把积累的数值当作额外伤害一次性释放（机制同恒动 {@code Kinetic} 的伤害储存）。</p>
 * <p><b>积累速度与上限同时受两个因素影响</b>（2026-09-12 用户补充：此前只算附魔强度、漏了武器等级）：</p>
 * <ul>
 *   <li><b>附魔强度</b>（{@code procChanceMultiplier}，含奥术戒指与各天赋加成）<b>等比例</b>放大两者；</li>
 *   <li><b>武器等级</b>（{@code weapon.buffedLvl()}）照原版附魔的「强度 ×（基准 + 等级）」范式参与：
 *       速度 = {@code 0.2 × 强度 × (1 + 等级/5)}、上限 = {@code 强度 × (10 + 2 × 等级)}。
 *       两者<b>同步</b>提升 ⇒ 攒满时间恒定在 ~50 回合，等级改变的是「一次释放能存下多少」：
 *       无等级 = 每 5 回合 1 点 / 上限 10，+5 = 上限 20，+10 = 上限 30（最后再乘附魔强度）。</li>
 * </ul>
 * <p><b>状态必须挂在角色身上的 buff 里</b>（而非武器字段）：人体派作品叠加附魔时
 * {@code extraEnchants} 只序列化类名，挂在武器上的字段会丢档。</p>
 */
public class Roach extends Weapon.Enchantment {

	private static final Glowing BROWN = new ItemSprite.Glowing( 0x8B5A2B );

	/** 无附魔强度、无等级加成时的积累速度：每 5 回合 1 点。 */
	public static final float BASE_RATE = 1f / 5f;
	/** 无附魔强度、无等级加成时的积累上限：10 点。 */
	public static final float BASE_CAP = 10f;
	/** 速度的等级因子：每本值级让速度翻倍，即 {@code speed × (1 + 等级/本值)}。 */
	public static final float RATE_LEVEL_DIVISOR = 5f;
	/** 上限的等级加成：每 1 级 +本值点（乘附魔强度之前）。 */
	public static final int CAP_LEVEL_STEP = 2;

	@Override
	public int proc( Weapon weapon, Char attacker, Char defender, int damage ) {

		AttackVermin vermin = Buff.affect( attacker, AttackVermin.class );
		int bonus = vermin.damageBonus();

		//释放并清零，同时把当前附魔强度与武器等级写回 buff，供下一轮积累使用
		vermin.release( procChanceMultiplier( attacker ), Math.max( 0, weapon.buffedLvl() ) );

		return damage + bonus;
	}

	@Override
	public Glowing glowing() {
		return BROWN;
	}

	/**
	 * 攻击害虫：挂攻击者身上的伤害储存槽。
	 * <p>每回合积累（速度 = {@code 0.2 × 附魔强度 × (1 + 武器等级/5)}，上限 =
	 * {@code 附魔强度 × (10 + 2 × 武器等级)}），存满后保持；被蜚蠊附魔的攻击命中时一次性释放并清零。</p>
	 * <p>附魔强度与武器等级都由 {@link Roach#proc} 在每次命中时写入（见 {@link #release}）——
	 * <b>刻意不在 {@code act()} 里现取</b>：{@code genericProcChanceMultiplier} 会消耗
	 * {@code RunicSlashTracker}/{@code DirectedPowerTracker}（detach），逐回合调用会把一次性天赋加成提前吃掉。</p>
	 * <p>图标编号 {@link BuffIndicator#ATTACK_VERMIN}，仅在存量 ≥ 1 时显示。</p>
	 */
	public static class AttackVermin extends Buff {

		{
			type = buffType.POSITIVE;
		}

		private static final String DAMAGE = "damage";
		private static final String POWER  = "power";
		private static final String LEVEL  = "weapon_level";

		/** 已积累的伤害（可为小数，显示时向下取整）。 */
		private float damage = 0f;
		/** 附魔强度倍率，由 {@link Roach#proc} 在命中时写入；读档缺失时兜底为 1。 */
		private float power = 1f;
		/**
		 * 命中时那把武器的等级（{@code buffedLvl()}），同样由 {@link Roach#proc} 写入。
		 * 积累速度与上限都随它提高，所以<b>换一把等级不同的蜚蠊武器</b>后，下一次命中就会用新等级重算。
		 */
		private int level = 0;

		@Override
		public int icon() {
			return damageBonus() > 0 ? BuffIndicator.ATTACK_VERMIN : BuffIndicator.NONE;
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString( damageBonus() );
		}

		@Override
		public void tintIcon( Image icon ) {
			//空载浅褐 → 满载深棕
			float pct = Math.min( 1f, damage / cap() );
			icon.hardlight( 1f - 0.45f * pct, 0.85f - 0.5f * pct, 0.65f - 0.5f * pct );
		}

		/** 当前的积累速度（点/回合）：{@code 0.2 × 附魔强度 × (1 + 武器等级/5)}。 */
		public float rate() {
			return BASE_RATE * Math.max( 1f, power ) * (1f + level / RATE_LEVEL_DIVISOR);
		}

		/** 当前的积累上限：{@code 附魔强度 × (10 + 2 × 武器等级)}。 */
		public float cap() {
			return (BASE_CAP + CAP_LEVEL_STEP * level) * Math.max( 1f, power );
		}

		/** 释放用的伤害数值（向下取整）。 */
		public int damageBonus() {
			return (int)Math.floor( damage );
		}

		/** 释放储存伤害（清零），并记录本次命中的附魔强度与武器等级（后续积累按它们重算）。 */
		public void release( float newPower, int weaponLevel ) {
			damage = 0f;
			power = Math.max( 1f, newPower );
			level = Math.max( 0, weaponLevel );
			BuffIndicator.refreshHero();
		}

		@Override
		public boolean act() {
			int before = damageBonus();
			//速度与上限都随附魔强度、武器等级变化（见 rate() / cap()）
			damage = Math.min( cap(), damage + rate() );

			//整数显示值变化时才刷图标（0 → 1 出现、释放后消失）
			if (damageBonus() != before) {
				BuffIndicator.refreshHero();
			}

			spend( TICK );
			return true;
		}

		@Override
		public String desc() {
			return Messages.get( this, "desc", damageBonus(), (int)cap() );
		}

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( DAMAGE, damage );
			bundle.put( POWER, power );
			bundle.put( LEVEL, level );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			//Bundle 对缺失键返回 0，必须给安全默认值
			damage = bundle.getFloat( DAMAGE );
			power = bundle.contains( POWER ) ? bundle.getFloat( POWER ) : 1f;
			//旧档无此键 → 0 → 退化成"不计武器等级"的版本，下次命中即被真实等级覆盖
			level = bundle.getInt( LEVEL );
		}
	}
}
