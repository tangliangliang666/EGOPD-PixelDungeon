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

package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import java.util.ArrayList;

/**
 * 9章2节：原版「无序魔典」的强化形态（2026-09-20）。
 *
 * <h3>新增效果</h3>
 * <p>阅读时，<b>点燃视野内所有敌方单位</b>；若某个敌人本来就处于燃烧状态，
 * 则对它额外施加 {@value #EXTRA_CRIPPLE} 回合的残废。</p>
 *
 * <h3>「本来就烧着」必须在点燃之前判</h3>
 * <p>本类先遍历一遍把「当前是否烧着」记下来，再统一点燃 —— 否则边烧边判，
 * 先被点着的敌人在同一轮里就变成「已燃烧」，所有人都会白吃一次残废。</p>
 *
 * <p>文本键 {@code items.artifacts.chapternineversetwo.*}；魔典原有的
 * {@code ac_read/ac_add/blinded/no_charge/cursed/prompt/read_empowered/desc_index/
 * desc_empowered/desc_cursed/infuse_scroll/unable_scroll/unknown_scroll} 等键
 * 沿父类链自动继承。</p>
 */
public class ChapterNineVerseTwo extends UnstableSpellbook {

	/** 对「阅读前就已燃烧」的敌人额外施加的残废回合数。 */
	public static final float EXTRA_CRIPPLE = 5f;

	{
		image = ItemSpriteSheet.ARTIFACT_CHAPTER_NINE_VERSE_TWO;
	}

	/**
	 * 父类负责掷一本随机卷轴并读出来（含「是否升级成秘卷」的选择窗口）。
	 * 本类只在其后补上「点燃视野内全体敌人」。
	 */
	@Override
	public void doReadEffect(Hero hero){
		super.doReadEffect(hero);
		igniteAllInView();
	}

	/** 点燃视野内所有敌方单位；对阅读前已处于燃烧状态的敌人附加残废。 */
	protected void igniteAllInView(){
		if (Dungeon.level == null) return;

		//先取快照：点燃/致死会改动 mob 列表，边遍历边改会踩并发修改
		ArrayList<Mob> targets = new ArrayList<>();
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
			if (mob == null || !mob.isAlive()) continue;
			if (mob.alignment != Char.Alignment.ENEMY) continue;
			//heroFOV 已含遮挡与隐形过滤，与其它「视野内」类效果口径一致
			if (!Dungeon.level.heroFOV[mob.pos]) continue;
			targets.add(mob);
		}

		for (Mob mob : targets){
			//必须在点燃之前判：否则本轮先被点着的敌人会立刻满足「已燃烧」，白吃一次残废
			boolean wasBurning = mob.buff(Burning.class) != null;

			if (!mob.isImmune(Burning.class)){
				Buff.affect(mob, Burning.class).reignite(mob);
			}
			if (wasBurning && !mob.isImmune(Cripple.class)){
				Buff.affect(mob, Cripple.class, EXTRA_CRIPPLE);
			}
		}
	}

	//==========================================================================
	// 炼金合成：无序魔典 + 30 脑啡肽，12 能量
	//==========================================================================

	public static class CraftRecipe extends ArtifactEnhanceRecipe<UnstableSpellbook, ChapterNineVerseTwo> {

		@Override
		protected Class<UnstableSpellbook> acceptedArtifact(){
			return UnstableSpellbook.class;
		}

		@Override
		protected ChapterNineVerseTwo createEnhanced(){
			return new ChapterNineVerseTwo();
		}

		@Override
		protected void transferState(UnstableSpellbook source, ChapterNineVerseTwo enhanced){
			enhanced.level( source.level() );
			enhanced.exp = source.exp;
			enhanced.chargeCap = source.chargeCap;
			enhanced.charge = source.charge;
			enhanced.partialCharge = source.partialCharge;
			enhanced.cooldown = source.cooldown;

			enhanced.cursed = source.cursed;
			enhanced.cursedKnown = source.cursedKnown;
			enhanced.levelKnown = source.levelKnown;

			//收录的卷轴清单是随机生成的，必须原样搬走（新实例的构造已经重掷过一份，先清掉）
			enhanced.scrolls.clear();
			enhanced.scrolls.addAll(source.scrolls);
		}
	}
}
