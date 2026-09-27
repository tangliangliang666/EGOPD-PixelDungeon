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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/**
 * 测试武器——CENSORED
 *
 * <p>贴图：{@link ItemSpriteSheet#CENSORED_WEAPON}（items.png 第9行第9列，36×9 长条形）。<br>
 * ItemSlot.item() 中会检测本类，并在 sprite.view(item) 之后自动
 * {@code sprite.originToCenter(); sprite.angle = -45f;}，利用基类 Visual 的 angle/origin
 * 字段实现静态展示的旋转。</p>
 *
 * <p>基础数值参考 Longsword（tier=5，RCH=2），不附加任何附魔/技能——纯贴图旋转验证用。</p>
 *
 * <p>命中附加效果（2026-09-06 增）：<br>
 *   • 20% 几率对目标施加 3 回合 {@link Terror}（恐惧）<br>
 *   • 每次命中固定施加 10 回合 {@link Ooze}（腐蚀淤泥）
 * </p>
 */
public class CensoredWeapon extends MeleeWeapon {

	{
		image = ItemSpriteSheet.CENSORED_WEAPON;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1f;

		tier = 5;
		RCH = 3;
	}

	@Override
	public int max(int lvl) {
		return  24 +  //base
				5*lvl;   //level scaling
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		// 攻击者抗魔（HolyWard / StoneOfMagic 等）或目标已阵亡 → 跳过附加效果。
		if (attacker.buff(MagicImmune.class) == null && defender.isAlive()) {
			// 恐惧施加时序修复（2026-09-08，同 FrostShard 之法）：
			// proc 在 Char.attack 中早于 enemy.damage(...)，而 Char.damage 入口会对带 Terror 的
			// 目标调用 recover()（-5 回合、≤0 即 detach）——直接在此 affect 会让本次攻击
			// 立刻打掉刚挂上的恐惧。解法：包装为一次性 FlavourBuff（actPriority=VFX_PRIO）
			// 延后到 enemy.damage 结算之后再施加恐惧与淤泥。
			new FlavourBuff() {
				{
					actPriority = VFX_PRIO;
				}
				public boolean act() {
					if (!target.isAlive()) {
						detach();
						return true;
					}
					// 20% 几率 3 回合恐惧。
					if (Random.Int(100) < 20) {
						Buff.affect(target, Terror.class, 3f);
					}
					// 固定施加 10 回合腐蚀淤泥（Ooze 需 2 参 affect + set(duration)，见 WandOfCorrosion）。
					Buff.affect(target, Ooze.class).set(10f);
					detach();
					return true;
				}
			}.attachTo(defender);
		}

		return damage;
	}

}
