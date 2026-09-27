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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/**
 * 水仙十字圣剑 · <b>荒性</b>：英雄达到 30 级（旅途的终点）时的状态。
 *
 * <p>面板抬到 {@code 6+L ~ 35+7L}（六阶武器的默认公式；{@code tier} 仍是 2，
 * 面板靠覆写 {@link #min}/{@link #max} 给出）。</p>
 *
 * <h3>效果</h3>
 * <ol>
 *   <li><b>经验阻断</b>：经验改为等额回合数的祝福，见 {@link #onExpGained}；</li>
 *   <li><b>攻击扩张</b>：{@link #proc}——攻击时对视野内所有敌人同时造成伤害 + 自己获得 12 回合灵视，
 *       冷却 60 回合。</li>
 * </ol>
 *
 * <p>形态规则见 {@link NarcissusCrossSwordBase}。</p>
 */
public class NarcissusSwordHuang extends NarcissusCrossSwordBase {

	/** 深蓝紫色的常驻光效（0.7 的呼吸节奏），刻意覆盖附魔光效。 */
	private static final ItemSprite.Glowing GLOW = new ItemSprite.Glowing(0x8A6BFF, 0.7f);

	{
		image = ItemSpriteSheet.NARCISSUS_SWORD_HUANG;
	}

	/** 面板下限 6+L（六阶默认）。 */
	@Override
	public int min(int lvl) {
		return 6 + lvl;
	}

	/** 面板上限 35+7L（六阶默认）。 */
	@Override
	public int max(int lvl) {
		return 35 + 7 * lvl;
	}

	/** 深蓝紫色光效：只要处于荒性就一直闪，把附魔辉光压掉。 */
	@Override
	public ItemSprite.Glowing formGlow() {
		return GLOW;
	}

	//==========================================================================
	// ① 经验阻断：改为等额回合数的祝福
	//==========================================================================

	@Override
	public boolean blocksExp() {
		return true;
	}

	@Override
	public void onExpGained(Hero hero, int exp) {
		if (hero == null || exp <= 0) return;
		//「获得相同数额回合的祝福」：一笔经验 = 这么多回合的祝福（叠加上去）
		Buff.affect(hero, Bless.class, exp);
	}

	//==========================================================================
	// ② 造成攻击时：视野内全体伤害 + 12 回合灵视（冷却 60 回合）
	//==========================================================================

	/**
	 * 近战命中钩子（挂在 {@code Weapon.proc} 上，调用点在 {@code Hero.attackProc} 里、
	 * 主伤害 {@code enemy.damage()} <b>之前</b>）。
	 *
	 * <p>两条守则与本作其它武器一致：</p>
	 * <ul>
	 *   <li>只有「<b>英雄握着这把剑</b>打出去」才触发——幻影 / 影分身拿着英雄的武器攻击时不算
	 *       （与 {@code SealedSwordBase.proc}、{@code Weapon.proc} 里的松脂涂层判定同款）；</li>
	 *   <li>先 {@code super.proc} 走完附魔 / 松脂，再算自己的效果。</li>
	 * </ul>
	 *
	 * <p><b>注意</b>：本击的主要目标也会被这次「视野内全体伤害」再打一下（它当然在视野内）。
	 * 若主目标被这一下打死，{@code Char.attack} 会在 {@code attackProc} 之后发现目标已死并提前收尾，
	 * 于是本击的直伤不再结算——这是既有链路的行为，不是 bug。</p>
	 */
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		if (attacker instanceof Hero && isEquipped((Hero) attacker)) {
			Hero hero = (Hero) attacker;
			if (offCooldown(hero)) {
				startCooldown(hero);
				strikeAllInView(hero);
				grantVision(hero);
				GLog.i(Messages.get(NarcissusSwordHuang.class, "hollow_strike"));
			}
		}

		return damage;
	}
}
