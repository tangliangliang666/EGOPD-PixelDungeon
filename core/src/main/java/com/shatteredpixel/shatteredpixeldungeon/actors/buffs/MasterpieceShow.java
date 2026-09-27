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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Point;

/**
 * 「杰作」展示 buff（已废弃，2026-09-04）。
 * <p>旧方案用纯 Image 挂在 floorEmitters/角色层做视觉展示，实测完全不可见；
 * 现改为召唤真正的单位 {@link com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Masterpiece}
 * （有贴图、固定不动、免疫伤害、50 回合消失）。</p>
 * <p>本类保留仅为旧测试存档兼容：读档后旧的展示 buff 在首个 act() 回合自行分离清理。</p>
 */
public class MasterpieceShow extends Buff {

	public static final float DURATION = 50f; //陈列回合数

	protected int pos;    //陈列格
	protected int depth;  //陈列楼层（换层即结束）
	protected float turnsLeft = DURATION;

	private transient Image visual; //80×64 视觉（不存档）

	/**
	 * 在 hero 当前层陈列一件杰作（挂在英雄身上的管理 buff）。
	 * 重复使用「闭馆」会先结束旧的展示。
	 */
	public static MasterpieceShow exhibit( Hero hero, int pos ){
		MasterpieceShow old = hero.buff(MasterpieceShow.class);
		if (old != null) old.detach();

		MasterpieceShow show = Buff.affect(hero, MasterpieceShow.class);
		show.pos = pos;
		show.depth = Dungeon.depth;
		show.turnsLeft = DURATION;
		show.ensureVisual();
		show.applyToWitnesses();
		return show;
	}

	{
		type = buffType.NEUTRAL;
	}

	@Override
	public boolean act() {
		if (target instanceof Hero && Dungeon.hero == target && Dungeon.depth == depth){
			turnsLeft--;
			if (turnsLeft > 0){
				ensureVisual();
				applyToWitnesses();
				spend( TICK );
				return true;
			}
		}
		//时间到 / 换层 / 目标异常：清理并结束
		detach();
		return true;
	}

	//对视野内（展厅灯光下，heroFOV 已含遮挡与隐形过滤）目睹杰作的敌人施加恐惧系状态
	private void applyToWitnesses(){
		Hero hero = (Hero)target;
		int points = hero.pointsInTalent(Talent.ENJOY_THE_SHOW);
		if (points <= 0) return;

		for (Char ch : Actor.chars().toArray( new Char[0] )){
			if (ch == hero || !ch.isAlive() || ch.alignment != Char.Alignment.ENEMY) continue;
			if (!Dungeon.level.heroFOV[ch.pos]) continue;

			if (points >= 1) Buff.affect( ch, Terror.class, Terror.DURATION );
			if (points >= 2) Buff.affect( ch, Daze.class, Daze.DURATION );
			if (points >= 3 && ch.buff(Doom.class) == null) Buff.affect( ch, Doom.class );
			if (points >= 4){
				//Dread 会覆盖 Terror（原版行为：dread overrides terror）
				Dread dread = ch.buff(Dread.class);
				if (dread == null) Buff.affect( ch, Dread.class );
			}
		}
	}

	//创建/重建视觉（80×64 贴图，水平居中、底边对齐格底，挂在角色同层：hero.sprite.parent 即 mobs 组）
	private void ensureVisual(){
		if (visual != null && visual.parent != null) return;
		if (Dungeon.depth != depth) return;
		if (!(target instanceof Hero)) return;
		Hero hero = (Hero) target;
		if (hero.sprite == null || hero.sprite.parent == null) return; //场景未就绪，下回合再建

		Image img = new Image(Assets.Sprites.MASTERPIECE);
		Point p = Dungeon.level.cellToPoint(pos);
		img.x = p.x + (16 - 80) / 2f;
		img.y = p.y + 16 - 64;
		hero.sprite.parent.add( img ); //与角色精灵同层
		visual = img;
	}

	@Override
	public void detach() {
		if (visual != null){
			if (visual.parent != null) visual.killAndErase();
			visual = null;
		}
		super.detach();
	}

	//纯展示 buff：不在状态栏显示图标
	@Override
	public int icon() {
		return BuffIndicator.NONE;
	}

	private static final String POS         = "pos";
	private static final String DEPTH       = "depth";
	private static final String TURNS_LEFT  = "turns_left";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( POS, pos );
		bundle.put( DEPTH, depth );
		bundle.put( TURNS_LEFT, turnsLeft );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		pos = bundle.getInt( POS );
		depth = bundle.getInt( DEPTH );
		turnsLeft = bundle.getFloat( TURNS_LEFT );
		if (turnsLeft <= 0) turnsLeft = 1; //防御：缺键/异常值至少再展示一回合以完成清理
	}
}
