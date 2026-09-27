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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.NarcissusShield;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/**
 * 水仙十字圣剑 · <b>芒性</b>：英雄恰好 1 级（旅途的起始）时的状态。
 *
 * <p>面板抬到 {@code 6+L ~ 35+7L}（六阶武器的默认公式；{@code tier} 仍是 2，
 * 面板靠覆写 {@link #min}/{@link #max} 给出，力量需求与「二阶武器」的标签都不变）。</p>
 *
 * <h3>效果</h3>
 * <ol>
 *   <li><b>精准 / 闪避加成</b>：英雄到达过的最深层数每 1 层 +20%
 *       （精准走 {@link #accuracyFactor}，闪避走 {@link #evasionMultiplier} + {@code Hero.defenseSkill}）；</li>
 *   <li><b>经验阻断</b>：见 {@link #onExpGained}——经验改为等额护盾 + 经验值 ÷ 5 回合的
 *       「神器充能 / 充能 / 祝福」；</li>
 *   <li><b>受击化解</b>：{@link #tryCounter}，由 {@code Hero.defenseProc} 调用。</li>
 * </ol>
 *
 * <p>形态规则见 {@link NarcissusCrossSwordBase}。</p>
 */
public class NarcissusSwordMang extends NarcissusCrossSwordBase {

	/** 白色常驻光效（0.7 的呼吸节奏），刻意覆盖附魔光效。 */
	private static final ItemSprite.Glowing GLOW = new ItemSprite.Glowing(0xFFFFFF, 0.7f);

	{
		image = ItemSpriteSheet.NARCISSUS_SWORD_MANG;
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

	/** 白色光效：只要处于芒性就一直闪，把附魔辉光压掉。 */
	@Override
	public ItemSprite.Glowing formGlow() {
		return GLOW;
	}

	//==========================================================================
	// ① 精准 / 闪避加成
	//==========================================================================

	/**
	 * 精准倍率 ×「最深层数」加成。
	 * <p>乘在 {@code super.accuracyFactor} <b>之后</b>：父类那句先算好了力量不足的 ÷1.5ⁿ 惩罚，
	 * 两者相乘即「惩罚照吃、加成照给」。</p>
	 */
	@Override
	public float accuracyFactor(Char owner, Char target) {
		return super.accuracyFactor(owner, target) * depthMultiplier();
	}

	/** 闪避倍率 ×「最深层数」加成（消费点在 {@code Hero.defenseSkill}）。 */
	@Override
	public float evasionMultiplier() {
		return depthMultiplier();
	}

	//==========================================================================
	// ② 经验阻断：改为护盾 + 神器充能 / 充能 / 祝福
	//==========================================================================

	@Override
	public boolean blocksExp() {
		return true;
	}

	@Override
	public void onExpGained(Hero hero, int exp) {
		if (hero == null || exp <= 0) return;

		//① 等额护盾（不衰减，见 NarcissusShield）
		Buff.affect(hero, NarcissusShield.class).incShield(exp);
		if (hero.sprite != null) {
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(exp), FloatingText.SHIELDING);
		}

		//② 经验值 ÷ 5 回合的「神器充能 / 充能 / 祝福」（不足 5 点也给 1 回合，见 expToTurns）
		int turns = expToTurns(exp);
		Buff.affect(hero, ArtifactRecharge.class).extend(turns);
		Buff.affect(hero, Recharging.class, turns);
		Buff.affect(hero, Bless.class, turns);
	}

	//==========================================================================
	// ③ 受到攻击时：免受本次伤害 + 视野内全体反击 + 12 回合致盲（冷却 60 回合）
	//==========================================================================

	/**
	 * 「受到攻击时」的入口。<b>调用点：{@code Hero.defenseProc}</b>（与「黑天鹅」同一处，
	 * 也就是只对<b>被角色攻击</b>生效，陷阱 / 饥饿 / 持续伤害不触发）。
	 *
	 * @return true 表示本次攻击已被完全化解——调用方必须随即 {@code return -1}，
	 *         让 {@code Char.attack} 跳过后续的命中结算与 {@code enemy.damage()}。
	 */
	public static boolean tryCounter(Hero hero) {
		NarcissusCrossSwordBase sword = held(hero);
		if (!(sword instanceof NarcissusSwordMang)) return false;
		if (!offCooldown(hero)) return false;

		startCooldown(hero);
		((NarcissusSwordMang) sword).counter(hero);
		return true;
	}

	/** 反击本体：对视野内所有敌人各造成一次伤害，并补上 12 回合致盲。 */
	private void counter(Hero hero) {
		strikeAllInView(hero);
		blindAllInView(hero);
		GLog.i(Messages.get(NarcissusSwordMang.class, "counter"));
	}
}
