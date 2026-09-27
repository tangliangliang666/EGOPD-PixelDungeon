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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.IndexAgentSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

/**
 * 食指代行者：命运弃子专精的「追杀者」。
 * <p>仅命运弃子专精时生成：每层初始固定生成 1 只（不以沉睡生成），并会以
 * 普通怪物 1/4 的概率加入后续的自然生成（见 {@link com.shatteredpixel.shatteredpixeldungeon.levels.Level#spawnMob(int)}）。
 * 不受「精英强敌」挑战影响（生成时不经过精英判定）。</p>
 * <p>数值参考活化石像随楼层线性成长；掉落参考强盗精英（首次必定掉落随机装备，之后概率逐次衰减为 1/3）。
 * 每隔 {@link #LOCATE_INTERVAL} 回合定位一次英雄位置（跨视野直线追杀），并在消息栏发出红色警告。</p>
 */
public class IndexAgent extends Mob {

	//每隔多少回合定位一次英雄位置
	private static final int LOCATE_INTERVAL = 15;

	//距离下次定位的回合数（出生后第一回合立即定位）
	private int locateCooldown = 1;

	{
		spriteClass = IndexAgentSprite.class;

		//高概率掉落随机物品（参考强盗精英：首次必定掉落，之后每次掉落概率衰减为1/3）
		loot = Random.oneOf(Generator.Category.WEAPON, Generator.Category.ARMOR,
				Generator.Category.RING, Generator.Category.ARTIFACT);
		lootChance = 1f;

		//不以沉睡生成，永不入眠
		state = WANDERING;
	}

	public IndexAgent(){
		super();
		//数值参考活化石像：随楼层线性成长（以监狱守卫为基底模板）
		HP = HT = 15 + Dungeon.depth * 5;
		defenseSkill = 4 + Dungeon.depth;
		EXP = 7 + Dungeon.depth / 2;
		maxLvl = Dungeon.depth + 5;

		//出生即锁定英雄为追杀目标
		if (Dungeon.hero != null){
			enemy = Dungeon.hero;
			target = Dungeon.hero.pos;
		}
	}

	@Override
	protected boolean act() {
		//每若干回合定位一次英雄位置（跨视野追杀）
		if (--locateCooldown <= 0){
			locateCooldown = LOCATE_INTERVAL;
			locateHero();
		}
		return super.act();
	}

	//定位英雄：将其设为追击目标，并在消息栏发出红色警告
	private void locateHero(){
		if (Dungeon.hero != null && Dungeon.hero.isAlive()){
			enemy = Dungeon.hero;
			target = Dungeon.hero.pos;
			if (state != HUNTING && state != FLEEING){
				state = HUNTING;
			}
			//红色警告（与燃烧等负面警告同色的 GLog.w）
			GLog.w(Messages.get(this, "tracking"));
		}
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(4, 12) + Dungeon.depth;
	}

	@Override
	public int attackSkill( Char target ) {
		return 9 + Dungeon.depth;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, Dungeon.depth);
	}

	@Override
	public float lootChance() {
		//每次掉落后概率衰减为1/3：100% → 33% → 11% → ...
		return super.lootChance() * (float)Math.pow(1/3f, Dungeon.LimitedDrops.INDEX_AGENT_EQUIP.count);
	}

	@Override
	public Item createLoot() {
		Dungeon.LimitedDrops.INDEX_AGENT_EQUIP.count++;
		return super.createLoot();
	}

	@Override
	public float spawningWeight() {
		//不参与普通怪物的生成权重（由专精生成逻辑控制）
		return 0;
	}

}
