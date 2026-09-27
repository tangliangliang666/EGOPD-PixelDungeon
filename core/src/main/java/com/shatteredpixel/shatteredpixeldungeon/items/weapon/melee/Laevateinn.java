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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Melting;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 莱瓦汀：系列的全解形态（力量需求 22），也是「中指 长兄」这条解封链的终点。
 *
 * <p>规则（解封门槛、锁手、落进副手即封回等）见 {@link SealedSwordBase}：全解后不再提供「解封」；
 * <b>一旦被切换到副手就会还原为封印之剑</b>，需要重新逐级解封。</p>
 *
 * <h3>面板（2026-09-15 起）</h3>
 * <p>伤害抬到 <b>7+L ~ 40+8L</b>（其余三形态保持六阶的 6+L ~ 35+7L）。
 * 这里的 <b>L 就是 {@link SealedSwordBase#level()}</b>——2026-09-16 起改由「英雄等级 ÷ 5」给出
 * （照女猎人的灵能弓），所以本剑<b>不能再用升级卷轴强化，但会跟着英雄一起变强</b>。</p>
 * <p>这里覆写 {@code min/max} 而<b>不去改 {@code tier}</b>：{@code tier} 还牵动力量需求、定价、
 * 「六阶」标签与决斗家天赋的比较，而本系列的力量需求本来就已被
 * {@link SealedSwordBase#STRReq(int)} 固定成 22（并且不随等级下降）。</p>
 *
 * <h3>火焰</h3>
 * <p>攻击附带本次伤害 <b>50%</b> 的火焰伤害（{@link #fireDamagePercent}）；并且<b>只要它在主手，
 * 每回合</b>都会点燃自身与周围 5×5 圆形范围（半径 2）内的地块——见 {@link #burnAura}。</p>
 */
public class Laevateinn extends SealedSwordBase {

	/** 每回合铺火时写给地块的火焰体积（与 {@code Burning} 点燃地面用的 4 保持一致）。 */
	public static final int FIRE_VOLUME = 4;

	/** 火场半径（格）：2 ⇒ 外框 5×5、取圆形内切 ⇒ 13 格。 */
	public static final int AURA_RADIUS = 2;

	{
		image = ItemSpriteSheet.LAEVATEINN;
	}

	/** 面板 7+L（其余形态为六阶的 6+L）。 */
	@Override
	public int min(int lvl) {
		return 7 + lvl;
	}

	/** 面板 40+8L（其余形态为六阶的 35+7L）。 */
	@Override
	public int max(int lvl) {
		return 40 + lvl * 8;
	}

	/** 攻击附带本次伤害 50% 的火焰伤害。 */
	@Override
	public float fireDamagePercent() {
		return 0.50f;
	}

	/**
	 * 莱瓦汀的常驻火场：点燃<b>自身</b>与周围 {@link #AURA_RADIUS} 格（5×5 圆形）内的所有地块，
	 * 并把这一圈里的<b>敌人</b>拖进「融化」（背叛家人者专精天赋「融化而死」，见 {@link Melting}）。
	 *
	 * <p>由 {@code SwordSwap.act()} 每回合调一次——那个 buff 只要剑在手上就一定存在、
	 * 每回合必然 act 一次，正好当火场的心跳。</p>
	 *
	 * <p>两个关键细节：</p>
	 * <ul>
	 *   <li>{@code Blob.seed} 是<b>累加</b>体积的（{@code cur[cell] += amount}），每回合无脑撒火会让
	 *       火势无限叠加、几回合内烧穿整层。所以只在 {@code Blob.volumeAt(cell, Fire.class) == 0}
	 *       时才补一次（照 {@code Burning.act()} 点燃地面的写法）：火永远烧不尽，也不会滚雪球。</li>
	 *   <li>自身走 {@code Buff.affect(Burning).reignite()}：{@code reignite} 只刷新剩余时长、
	 *       不会重新挂一遍 buff，因此不会每回合弹一次状态名。</li>
	 * </ul>
	 *
	 * <p><b>代价</b>：一直着火意味着持续掉血；而且 {@code Burning} 从第 4 回合起会按概率烧掉背包里的
	 * 卷轴 / 生肉。这正是中指长兄 T2「会很烫的！」（残血免疫燃烧伤害）要配套解决的问题。</p>
	 */
	public static void burnAura(Hero hero) {
		if (hero == null || Dungeon.level == null) return;
		//只在「主手握莱瓦汀」时燃烧（副手形态恒为封印之剑，不会走到这里）
		if (!(hero.belongings.weapon instanceof Laevateinn)) return;

		//点燃自身
		if (!hero.isImmune(Burning.class)) {
			Buff.affect(hero, Burning.class).reignite(hero);
		}

		//点燃周围地块：5×5 外框内取圆形（dx² + dy² ≤ R²）
		int w = Dungeon.level.width();
		int cx = hero.pos % w;
		int cy = hero.pos / w;
		for (int dy = -AURA_RADIUS; dy <= AURA_RADIUS; dy++) {
			for (int dx = -AURA_RADIUS; dx <= AURA_RADIUS; dx++) {
				if (dx * dx + dy * dy > AURA_RADIUS * AURA_RADIUS) continue;
				int x = cx + dx;
				int y = cy + dy;
				if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) continue;
				int cell = y * w + x;
				//墙壁与水面不铺火（水里本来也烧不起来）
				if (Dungeon.level.solid[cell] || Dungeon.level.water[cell]) continue;
				if (Blob.volumeAt(cell, Fire.class) == 0) {
					GameScene.add(Blob.seed(cell, FIRE_VOLUME, Fire.class));
				}
			}
		}

		//「融化而死」：莱瓦汀形态下，火场圈子里的敌人直接进入「融化」。
		//只用英雄与目标的距离判一次（与上面铺火的形状同源：AURA_RADIUS 格以内的圆形），
		//不打墙、不打水——站在火场圈里就是「在莱瓦汀的烈焰中」。未点天赋时 Melting.apply 内部直接返回。
		for (Char ch : Actor.chars()) {
			if (ch == null || ch == hero || !ch.isAlive()) continue;
			if (ch.alignment != Char.Alignment.ENEMY) continue;
			if (Dungeon.level.distance(hero.pos, ch.pos) > AURA_RADIUS) continue;
			Melting.apply(ch);
		}
	}
}
