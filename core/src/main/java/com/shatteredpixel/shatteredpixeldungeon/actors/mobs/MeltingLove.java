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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DirectableAlly;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MeltingLoveSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * 爱慕神器的召唤物「溶解之爱」（友方，跟随英雄、自动攻击敌人）。
 * <p>数值套蜜蜂公式，等效等级 = 神器等级 × 3 + 5（随神器升级实时成长）。
 * 攻击命中目标施加 20 回合腐蚀淤泥；击杀目标时在目标处生成一只友方粉色史莱姆。
 * 每 (15 - 神器等级) 回合恢复 1 点生命。
 * 若英雄在它存活时摘下「爱慕」（需先祛除诅咒）且未在 10 回合内重新装备，它会反噬英雄。</p>
 */
public class MeltingLove extends DirectableAlly {

	{
		spriteClass = MeltingLoveSprite.class;

		EXP = 0;
	}

	private int artLevel = 0;       //神器等级（决定等效蜜蜂等级与回血间隔）
	private int beeLevel = 5;       //等效蜜蜂等级 = artLevel×3+5
	private int regenCounter = 0;   //回血计数

	//图像组（0/1/2 = lovenew 的第一/二/三组；赠予嬗变卷轴在三种图像间随机切换）与"是否已反噬（loveanger 形态）"。
	//二者都由精灵读取，且必须随存档/重新召唤保留——故存在怪物身上（imageGroup 的源头是神器 Admiration.lovePalette）。
	private int imageGroup = MeltingLoveSprite.PALETTE_A;
	private boolean angry = false;

	public MeltingLove() {
		super();
		refreshForLevel(0);
	}

	//========== 数值（蜜蜂公式） ==========

	private int effHT() {
		return (2 + beeLevel) * 4;
	}

	/** 按神器等级重算：等效蜜蜂等级 = 等级×3+5，尽量保留当前 HP 百分比（召唤时 HP 置满）。 */
	public void refreshForLevel(int artLevel) {
		this.artLevel = Math.max(0, artLevel);
		this.beeLevel = this.artLevel * 3 + 5;

		float pct = (HP > 0 && HT > 0) ? (float) HP / HT : 1f;
		HT = effHT();
		if (HP <= 0) HP = HT;                    //新召唤/首次：满血
		else HP = Math.max(1, Math.round(HT * pct));

		defenseSkill = 9 + beeLevel;
	}

	@Override
	public int attackSkill(Char target) {
		return 9 + beeLevel;
	}

	@Override
	public int damageRoll() {
		int eff = effHT();
		return Random.NormalIntRange(eff / 10, eff / 4);
	}

	//每回合：先按 (15 - 神器等级) 回合的间隔恢复 1 点生命，再执行正常行动
	@Override
	protected boolean act() {
		if (isAlive() && HP < HT){
			regenCounter++;
			int interval = Math.max(1, 15 - artLevel);   //n=神器等级：15/14/…/5
			if (regenCounter >= interval){
				regenCounter = 0;
				heal( 1 );
			}
		}
		return super.act();
	}

	//命中目标：施加 20 回合腐蚀淤泥
	@Override
	public int attackProc(Char enemy, int damage) {
		damage = super.attackProc(enemy, damage);
		if (enemy.isAlive()) {
			Ooze ooze = Buff.affect(enemy, Ooze.class);
			if (ooze != null) ooze.set(20f);
		}
		return damage;
	}

	@Override
	public String description() {
		return "一只人形的粉色粘液生物。她紧紧地贴着你，一刻也不愿意分离。";
	}

	//========== 图像形态 / 表情（精灵侧实现，见 MeltingLoveSprite） ==========

	/** 当前图像组（0/1/2 = lovenew 的第一/二/三组）。 */
	public int palette() {
		return imageGroup;
	}

	/** 等效蜜蜂等级（= 神器等级×3+5），供药水效果等按等级取值的场合使用。 */
	public int beeLevel() {
		return beeLevel;
	}

	/** 设置图像组（赠予嬗变卷轴时切换；死亡重新召唤时由神器把记录值传回）。越界值归一到第一组。 */
	public void setPalette(int palette) {
		imageGroup = MeltingLoveSprite.normalizePalette(palette);
		if (sprite instanceof MeltingLoveSprite) {
			((MeltingLoveSprite) sprite).setPalette(imageGroup);
		}
	}

	/** 已反噬（loveanger 形态）？ */
	public boolean isAngry() {
		return angry;
	}

	/** 播放一次性表情（{@link MeltingLoveSprite#EMO_EMBARRASSED} / {@link MeltingLoveSprite#EMO_HAPPY}）。 */
	public void playEmote(int emote) {
		if (sprite instanceof MeltingLoveSprite) {
			((MeltingLoveSprite) sprite).showEmote(emote);
		}
	}

	/** 进入"焦虑"持续表情（摘下爱慕神器、反噬倒计时开始）；可重复调用。 */
	public void beginAnxious() {
		if (sprite instanceof MeltingLoveSprite) {
			((MeltingLoveSprite) sprite).beginAnxious();
		}
	}

	/** 结束"焦虑"持续表情（重新装备爱慕 / 倒计时取消）。 */
	public void endAnxious() {
		if (sprite instanceof MeltingLoveSprite) {
			((MeltingLoveSprite) sprite).clearAnxious();
		}
	}

	//========== 生成 / 查找 / 击杀生成史莱姆 / 反噬 ==========

	/** 在英雄身边空位召唤（由神器调用；artLevel 为当前神器等级，palette 为神器记录的图像组）。 */
	public static MeltingLove summon(Hero hero, int cell, int artLevel, int palette) {
		MeltingLove love = new MeltingLove();
		//神器上记录的值可能来自旧档：归一化后再存（精灵 link() 时会从这里读）
		love.imageGroup = MeltingLoveSprite.normalizePalette(palette);
		love.pos = cell;
		GameScene.add(love);
		ScrollOfTeleportation.appear(love, cell);
		love.refreshForLevel(artLevel);
		return love;
	}

	/** 查找当前层仍忠诚（友方、存活）的溶解之爱（同层最多一只）。 */
	public static MeltingLove findLoyalAlly() {
		if (Dungeon.level == null) return null;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (m instanceof MeltingLove
					&& m.alignment == Char.Alignment.ALLY
					&& m.isAlive()) {
				return (MeltingLove) m;
			}
		}
		return null;
	}

	/** 查找当前层任何阵营的存活溶解之爱（友方或已反噬敌对）。用于解除"脱下神器后失同步"bug。 */
	public static MeltingLove findAnyAlive() {
		if (Dungeon.level == null) return null;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (m instanceof MeltingLove && m.isAlive()) {
				return (MeltingLove) m;
			}
		}
		return null;
	}

	/** 击杀目标（由 Mob.die 在 super.die 之后回调）：在目标格生成一只粉色史莱姆。 */
	public void onKill(int cell) {
		PinkSlime slime = new PinkSlime();
		slime.pos = cell;
		GameScene.add(slime);
		ScrollOfTeleportation.appear(slime, cell);
	}

	/** 反噬：摘下神器超时未戴回 → 变为敌对并攻击英雄；所有由任何溶解之爱生成的粉色史莱姆也一起变为敌对。 */
	public void betray(Hero hero) {
		// 所有在场的粉色史莱姆对齐到本溶解之爱的"反噬"阵营（2026-09-05 修复）：
		// 不再保留对英雄的 ALLY 立场，立即仇恨英雄。
		if (Dungeon.level != null) {
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
				if (m instanceof PinkSlime && m.isAlive()) {
					m.alignment = Char.Alignment.ENEMY;
					m.aggro(hero);
				}
			}
		}
		clearDefensingPos();
		alignment = Char.Alignment.ENEMY;
		aggro(hero);

		//表现：焦虑（倒计时期间由 BetrayalTimer 持续施加）→ 黑化两帧 → 切换到 loveanger.png 的反噬形态
		angry = true;
		if (sprite instanceof MeltingLoveSprite) {
			((MeltingLoveSprite) sprite).darkenThenAnger();
		}
	}

	//========== 存档 ==========

	private static final String ART_LEVEL    = "art_level";
	private static final String BEE_LEVEL    = "bee_level";
	private static final String REGEN_COUNT  = "regen_counter";
	private static final String IMAGE_GROUP  = "image_group";
	private static final String ANGER        = "anger";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(ART_LEVEL, artLevel);
		bundle.put(BEE_LEVEL, beeLevel);
		bundle.put(REGEN_COUNT, regenCounter);
		bundle.put(IMAGE_GROUP, imageGroup);
		bundle.put(ANGER, angry);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		artLevel = bundle.getInt(ART_LEVEL);
		beeLevel = bundle.getInt(BEE_LEVEL);
		regenCounter = bundle.getInt(REGEN_COUNT);
		//旧档无此键 → 0 = 第一组；异常值同样由 normalizePalette 归一
		imageGroup = MeltingLoveSprite.normalizePalette(bundle.getInt(IMAGE_GROUP));
		angry = bundle.getBoolean(ANGER);
		if (beeLevel < 1) beeLevel = 5;
		int oldHP = HP;
		HT = effHT();
		defenseSkill = 9 + beeLevel;
		HP = Math.min(Math.max(oldHP, 1), HT);
	}

}
