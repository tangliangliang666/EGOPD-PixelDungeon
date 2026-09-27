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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HealBlock;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.WhiteSmokeParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

/**
 * 「洛伊德护符」（2026-09-24 用户新道具）。
 *
 * <h3>用法</h3>
 * <p><b>投掷生效</b>（{@code defaultAction = AC_THROW}）：投到<b>任何单位</b>身上时，
 * 目标身上炸开一簇白色烟雾，并被挂上 {@value #HEAL_BLOCK_TURNS} 回合的「禁疗」
 * （{@link HealBlock}）——期间获得的一切血量恢复都被强制置 0（含 CHESED 考验的回血）。</p>
 *
 * <h3>对宝箱怪</h3>
 * <p>若目标是<b>宝箱怪</b>（{@link Mimic} 及其三个变体：黄金 / 水晶 / 黑檀），额外
 * <b>揭穿伪装</b>（退出伪装态、转为敌对并进入追击），并追加 {@value #MIMIC_DISABLE_TURNS} 回合的
 * <b>麻痹 + 虚弱 + 致盲</b>。</p>
 *
 * <h3>投空</h3>
 * <p>砸到地上（没命中任何单位）时不会生效，护符<b>掉在地上可捡回</b>（走 {@code Item.onThrow} 的默认落地）。</p>
 *
 * <h3>获得方式</h3>
 * <p>只能炼金合成，不入任何掉落池：治疗药水 ×1 ＋ 液金 ×20 ＋ <b>3</b> 炼金能量 → <b>6</b> 个
 * （见 {@link CraftRecipe}）。</p>
 *
 * <h3>贴图</h3>
 * <p>{@code ItemSpriteSheet.LLOYD_TALISMAN} = {@code xy(7,41)}，14×16（用户已绘）。</p>
 */
public class LloydTalisman extends Item {

	/** 「禁疗」持续回合数。 */
	public static final float HEAL_BLOCK_TURNS = 50f;

	/** 命中宝箱怪时追加的麻痹 / 虚弱 / 致盲回合数。 */
	public static final float MIMIC_DISABLE_TURNS = 20f;

	{
		image = ItemSpriteSheet.LLOYD_TALISMAN;

		stackable = true;
		defaultAction = AC_THROW;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		//炼金产物、效果固定：恒已识别（与松脂系列一致）
		return true;
	}

	@Override
	protected void onThrow( int cell ) {

		Char ch = Actor.findChar( cell );

		//没砸到任何单位：不生效，照常落地（可捡回）
		if (ch == null || !ch.isAlive()) {
			super.onThrow( cell );
			return;
		}

		//白色烟雾：目标身上炸开一簇 + 落点也起一团
		if (ch.sprite != null) {
			ch.sprite.emitter().burst( WhiteSmokeParticle.BURST, 12 );
		}
		CellEmitter.get( cell ).burst( WhiteSmokeParticle.FACTORY, 8 );
		Sample.INSTANCE.play( Assets.Sounds.PUFF );

		//宝箱怪：先揭穿伪装再挂 debuff。
		//顺序很重要——Mimic.add() 对 NEGATIVE buff 会自动执行「alignment=ENEMY + stopHiding()」，
		//若先挂 buff 再手动暴露，就会多播一次揭示特效（GLog + 音效 + 星光）。这里先暴露，
		//后面加 buff 时 alignment 已不是 NEUTRAL，自动分支自然跳过。
		boolean isMimic = ch instanceof Mimic;
		if (isMimic) {
			Mimic mimic = (Mimic) ch;
			mimic.stopHiding();
			if (mimic.alignment == Char.Alignment.NEUTRAL) {
				mimic.alignment = Char.Alignment.ENEMY;
			}
		}

		//禁疗：唯一判定点＝Char.heal(int)
		HealBlock.apply( ch, HEAL_BLOCK_TURNS );

		if (isMimic) {
			Buff.affect( ch, Paralysis.class, MIMIC_DISABLE_TURNS );
			Buff.affect( ch, Weakness.class, MIMIC_DISABLE_TURNS );
			Buff.affect( ch, Blindness.class, MIMIC_DISABLE_TURNS );
		}

		//命中即消耗：这里刻意**不**调 super.onThrow（那会把护符丢到地上）。
		//Item.cast() 已经把它从背包里 detach 出来（栈 >1 时给的是数量 1 的副本）,
		//不再落地就等于「用掉了」——与原版药水 shatter 的处理方式一致。
	}

	@Override
	public int value() {
		return 30 * quantity;
	}

	//==========================================================================
	// 炼金配方
	//==========================================================================

	/** 炼金配方：治疗药水 ×1 ＋ 液金 ×20 ＋ 3 能量 → 洛伊德护符 ×6。 */
	public static class CraftRecipe extends Recipe.SimpleRecipe {
		{
			inputs = new Class[]{ PotionOfHealing.class, LiquidMetal.class };
			inQuantity = new int[]{ 1, 20 };
			cost = 3;
			output = LloydTalisman.class;
			outQuantity = 6;
		}
	}
}
