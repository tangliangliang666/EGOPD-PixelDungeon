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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.sprites.SmilingCorpseMountainSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;

/**
 * 微笑的尸山（2026-09-09 用户需求：验证三形态换皮贴图的测试 Boss）。
 * <p>设计：
 * <ul>
 *   <li><b>血量上限 3000</b>，随 HP 区间切换外观（{@link SmilingCorpseMountainSprite} 换贴图+帧网格）：
 *       <ul>
 *         <li>HP ≥ HT×2/3 → 形态 3（m3.png，64×48 帧）；</li>
 *         <li>HP ≥ HT×1/3 → 形态 2（m2.png，48×48 帧）；</li>
 *         <li>其余        → 形态 1（m1.png，32×24 帧）。</li>
 *       </ul>
 *       阈值自 2026-09-24 起由 HT 现算：原版写死 2000 / 1000，恰为 3000 的 2/3 与 1/3，
 *       而考验 CHESED 会把 HT 抬到 3750，写死值就不再是 2/3·1/3 了。</li>
 *   <li><b>AI 暂为空闲（验证期）</b>：{@code onAdd()} 里强制 {@code PASSIVE}——不移动、不索敌、不攻击，
 *       被打也不还手（Mob.aggro 对 PASSIVE 有保护），仅作挨打换皮的木桩；
 *       后续实现真实 Boss AI 时移除该覆写即可。</li>
 *   <li>标准 Boss 待遇：{@link Property#BOSS} + 顶部 {@link BossHealthBar} 血条。</li>
 *   <li>当前未接入任何楼层刷怪池（用户拍板：从调试窗口 Mob 列表生成观察；
 *       调试窗自动扫描本包，无需额外注册）。</li>
 * </ul>
 */
public class SmilingCorpseMountain extends Mob {

	{
		HP = HT = 3000;
		EXP = 0;            //验证用：不给经验，避免干扰升级节奏
		defenseSkill = 0;   //验证用：0 防御 → 玩家必中，方便快速掉血看形态切换
		spriteClass = SmilingCorpseMountainSprite.class;

		properties.add(Property.BOSS);
	}

	/**
	 * 当前 HP 应处的形态（1~3）。
	 * 区间由 HT 现算：≥ HT×2/3 → 3；≥ HT×1/3 → 2；其余 → 1。
	 */
	public int phase(){
		if (HP >= HT * 2 / 3) return 3;
		if (HP >= HT / 3) return 2;
		return 1;
	}

	@Override
	protected void onAdd(){
		super.onAdd();
		//验证期 AI 恒空闲（压过调试窗 spawn 时强设的 WANDERING；正常刷怪路径同样生效）
		state = PASSIVE;
		//标准 Boss 顶部血条（assignBoss 幂等：重复 assign 同一只无害）
		BossHealthBar.assignBoss(this);
	}

	//受击后按剩余 HP 同步外观形态（死亡交给 die 动画，此处不做）
	@Override
	public void damage( int dmg, Object src ) {
		super.damage( dmg, src );
		if (isAlive()){
			updatePhase();
		}
	}

	//精灵 link / 读档还原 / buff 状态刷新时都会经此同步形态（CharSprite.link → ch.updateSpriteState()）
	@Override
	public synchronized void updateSpriteState() {
		super.updateSpriteState();
		updatePhase();
	}

	private void updatePhase(){
		if (sprite instanceof SmilingCorpseMountainSprite){
			((SmilingCorpseMountainSprite) sprite).setPhase( phase() );
		}
	}

}
