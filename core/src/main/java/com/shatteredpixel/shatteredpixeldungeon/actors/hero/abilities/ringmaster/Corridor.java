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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ringmaster;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Foresight;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSight;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MindVision;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

/**
 * 环指大师的盔甲技能「走廊」（2026-09-04）。
 * <p>使用后弹出「设置 / 返回」两个选项（参考返回信标 BeaconOfReturning 的双模式）：
 * <b>设置</b>不消耗充能，记录当前所在位置；<b>返回</b>消耗
 * {@code baseChargeUse=30}% 盔甲充能（若同层且点出「近距穿梭」则减免），
 * 瞬间回到之前设定的地点——若不在同一层则直接切层返回。</p>
 * <p>对应三个四阶分支天赋：身形隐匿（返回后隐身）、廊中视野（返回后魔能透视/灵视/危险预知）、
 * 近距穿梭（同层返回减少充能消耗）。</p>
 */
public class Corridor extends ArmorAbility {

	{
		baseChargeUse = 30f; //消耗30%盔甲充能
	}

	@Override
	protected void activate(final ClassArmor armor, final Hero hero, Integer target) {

		CorridorTracker tracker = hero.buff(CorridorTracker.class);

		final boolean canReturn = tracker != null && tracker.returnDepth != -1;

		String wndBody;
		if (canReturn){
			wndBody = Messages.get(this, "wnd_body", tracker.returnDepth);
		} else {
			wndBody = Messages.get(this, "wnd_body_none");
		}

		GameScene.show(new WndOptions( new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite(com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.SCROLL_ISAZ),
				Messages.titleCase( name() ),
				wndBody,
				Messages.get(this, "wnd_set"),
				Messages.get(this, "wnd_return")){
			@Override
			protected void onSelect(int index) {
				if (index == 0){
					//设置：不消耗充能，记录当前位置（占一回合）
					if (Dungeon.bossLevel() || !Dungeon.interfloorTeleportAllowed()){
						GLog.w( Messages.get(Corridor.class, "preventing") );
						return;
					}
					CorridorTracker t = Buff.affect(hero, CorridorTracker.class);
					t.returnDepth  = Dungeon.depth;
					t.returnBranch = Dungeon.branch;
					t.returnPos    = hero.pos;
					hero.busy();
					hero.sprite.operate( hero.pos );
					Sample.INSTANCE.play( Assets.Sounds.BEACON );
					GLog.i( Messages.get(Corridor.class, "set") );
					PixelScene.shake(1, 0.5f);
					hero.spendAndNext( 1f );

				} else if (index == 1){
					//返回：消耗充能（同层且点出近距穿梭则减免）
					CorridorTracker t = hero.buff(CorridorTracker.class);
					if (t == null || t.returnDepth == -1){
						GLog.w( Messages.get(Corridor.class, "no_return") );
						return;
					}

					float cost = chargeUse(hero);
					//近距穿梭：返回点与英雄同层时减免 10%/20%/30%/40%
					if (t.returnDepth == Dungeon.depth && t.returnBranch == Dungeon.branch){
						int points = hero.pointsInTalent(Talent.CLOSE_RANGE_SHUTTLE);
						cost *= (1f - 0.1f * points);
					}

					if (armor.charge < cost){
						GLog.w( Messages.get(ClassArmor.class, "low_charge") );
						return;
					}
					armor.charge -= cost;
					armor.updateQuickslot();

					if (t.returnDepth == Dungeon.depth && t.returnBranch == Dungeon.branch){
						//同层：直接瞬移回设定地点
						if (ScrollOfTeleportation.teleportToLocation(hero, t.returnPos)){
							hero.spendAndNext( 1f );
						} else {
							return;
						}
					} else {
						//跨层：切换楼层返回
						if (!Dungeon.interfloorTeleportAllowed()){
							GLog.w( Messages.get(Corridor.class, "preventing") );
							return;
						}
						//不能回到采矿层内部
						if (t.returnDepth >= 11 && t.returnDepth <= 14 && t.returnBranch == 1){
							GLog.w( Messages.get(ScrollOfTeleportation.class, "no_tele") );
							return;
						}
						Level.beforeTransition();
						Invisibility.dispel();
						InterlevelScene.mode = InterlevelScene.Mode.RETURN;
						InterlevelScene.returnDepth = t.returnDepth;
						InterlevelScene.returnBranch = t.returnBranch;
						InterlevelScene.returnPos = t.returnPos;
						Game.switchScene( InterlevelScene.class );
					}

					afterReturn( hero );
				}
			}
		});
	}

	//返回后触发的天赋效果（身形隐匿 / 廊中视野）
	private void afterReturn( Hero hero ){
		Invisibility.dispel();

		//身形隐匿：返回后隐身 10/20/30/40 回合
		int vanish = hero.pointsInTalent(Talent.VANISHING_FORM);
		if (vanish > 0){
			Buff.prolong( hero, Invisibility.class, 10f * vanish );
		}

		//廊中视野：返回后获得魔能透视与灵视（+3 追加危险预知）
		int vision = hero.pointsInTalent(Talent.CORRIDOR_VISION);
		if (vision > 0){
			float sightDur = vision >= 3 ? 20f : 10f;
			Buff.prolong( hero, MagicalSight.class, sightDur );
			if (vision >= 2){
				Buff.prolong( hero, MindVision.class, sightDur );
			}
			if (vision >= 4){
				Buff.prolong( hero, Foresight.class, sightDur );
			}
		}
	}

	@Override
	public int icon() {
		return HeroIcon.CORRIDOR; //hero_icons.png 第9行第5列（帧 68）
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.VANISHING_FORM, Talent.CORRIDOR_VISION, Talent.CLOSE_RANGE_SHUTTLE, Talent.HEROIC_ENERGY};
	}

	/** 走廊返回点记录 buff（随英雄存档持久化）。 */
	public static class CorridorTracker extends Buff {

		{
			revivePersists = true;
		}

		public int returnDepth  = -1;
		public int returnBranch = 0;
		public int returnPos;

		private static final String DEPTH   = "depth";
		private static final String BRANCH  = "branch";
		private static final String POS     = "pos";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( DEPTH, returnDepth );
			bundle.put( BRANCH, returnBranch );
			if (returnDepth != -1) {
				bundle.put( POS, returnPos );
			}
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			returnDepth  = bundle.getInt( DEPTH );
			returnBranch = bundle.getInt( BRANCH );
			returnPos    = bundle.getInt( POS );
		}
	}
}
