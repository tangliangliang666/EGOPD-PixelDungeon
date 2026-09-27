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

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HeroDisguise;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;

public class HeroSprite extends CharSprite {
	
	public static final int FRAME_WIDTH	= 12;
	public static final int FRAME_HEIGHT	= 15;
	
	private static final int RUN_FRAMERATE	= 20;
	
	private static TextureFilm tiers;
	private static TextureFilm ringMasterTiers; //环指大师用 20×24 帧，独立 tier 行偏移缓存
	
	private Animation fly;
	private Animation read;

	public HeroSprite() {
		super();
		
		texture( Dungeon.hero.heroClass.spritesheet() );
		updateArmor( true );
		
		link( Dungeon.hero );

		if (ch.isAlive())
			idle();
		else
			die();
	}

	public void disguise(HeroClass cls){
		texture( cls.spritesheet() );
		updateArmor( false ); //伪装成其它职业时不套用专属皮肤
	}

	public void updateArmor() {
		updateArmor( true );
	}

	private void updateArmor( boolean allowSkin ) {

		HeroClass cls = Dungeon.hero.heroClass;
		int[] fs = frameSize( cls );
		int tier = allowSkin ? skinTier( cls, Dungeon.hero.tier() ) : Dungeon.hero.tier();
		TextureFilm film = new TextureFilm( tiers( cls ), tier, fs[0], fs[1] );
		
		idle = new Animation( 1, true );
		idle.frames( film, 0, 0, 0, 1, 0, 0, 1, 1 );
		
		run = new Animation( RUN_FRAMERATE, true );
		run.frames( film, 2, 3, 4, 5, 6, 7 );
		
		die = new Animation( 20, false );
		die.frames( film, 8, 9, 10, 11, 12, 11 );
		
		attack = new Animation( 15, false );
		attack.frames( film, 13, 14, 15, 0 );
		
		zap = attack.clone();
		
		operate = new Animation( 8, false );
		operate.frames( film, 16, 17, 16, 17 );
		
		fly = new Animation( 1, true );
		fly.frames( film, 18 );

		read = new Animation( 20, false );
		read.frames( film, 19, 20, 20, 20, 20, 20, 20, 20, 20, 19 );
		
		if (Dungeon.hero.isAlive())
			idle();
		else
			die();
	}
	
	@Override
	public void place( int p ) {
		super.place( p );
		if (Game.scene() instanceof GameScene) Camera.main.panFollow(this, 5f);
	}

	@Override
	public void move( int from, int to ) {
		super.move( from, to );
		if (ch != null && ch.flying) {
			play( fly );
		}
		Camera.main.panFollow(this, 20f);
	}

	@Override
	public void idle() {
		super.idle();
		if (ch != null && ch.flying) {
			play( fly );
		}
	}

	@Override
	public void jump( int from, int to, float height, float duration,  Callback callback ) {
		super.jump( from, to, height, duration, callback );
		play( fly );
		Camera.main.panFollow(this, 20f);
	}

	public synchronized void read() {
		animCallback = new Callback() {
			@Override
			public void call() {
				idle();
				ch.onOperateComplete();
			}
		};
		play( read );
	}

	@Override
	public void bloodBurstA(PointF from, int damage) {
		//Does nothing.

		/*
		 * This is both for visual clarity, and also for content ratings regarding violence
		 * towards human characters. The heroes are the only human or human-like characters which
		 * participate in combat, so removing all blood associated with them is a simple way to
		 * reduce the violence rating of the game.
		 */
	}

	@Override
	public void update() {
		sleeping = ch.isAlive() && ((Hero)ch).resting;
		
		super.update();
	}
	
	public void sprint( float speed ) {
		run.delay = 1f / speed / RUN_FRAMERATE;
	}
	
	//按职业返回精灵帧尺寸：原版职业 12×15，环指大师 20×24
	private static final int[] DEF_FRAME = {FRAME_WIDTH, FRAME_HEIGHT};
	private static final int[] RM_FRAME  = {20, 24};
	public static int[] frameSize( HeroClass cls ) {
		return cls == HeroClass.RING_MASTER ? RM_FRAME : DEF_FRAME;
	}

	//拇指 前二老板：皮肤「泪锋之剑」占用贴图第 8 行（索引 7）。开启后无论穿什么护甲都恒定取该行。
	public static final int VALENCINA_SKIN_TIER = 7;

	//把护甲 tier 换算成实际显示用的行索引：前二老板开启专属皮肤时恒定为皮肤行
	public static int skinTier( HeroClass cls, int armorTier ) {
		if (cls == HeroClass.VALENCINA && SPDSettings.valencinaSkin()){
			return VALENCINA_SKIN_TIER;
		}
		return armorTier;
	}

	//按职业返回 tier 行偏移纹理册：原版共享 ROGUE（布局一致），环指大师用自己的 20×24 贴图
	public static TextureFilm tiers( HeroClass cls ) {
		if (cls == HeroClass.RING_MASTER) {
			if (ringMasterTiers == null) {
				SmartTexture texture = TextureCache.get( cls.spritesheet() );
				ringMasterTiers = new TextureFilm( texture, texture.width, RM_FRAME[1] );
			}
			return ringMasterTiers;
		}
		if (tiers == null) {
			SmartTexture texture = TextureCache.get( Assets.Sprites.ROGUE );
			tiers = new TextureFilm( texture, texture.width, FRAME_HEIGHT );
		}
		return tiers;
	}

	public static Image avatar( Hero hero ){
		if (hero.buff(HeroDisguise.class) != null){
			return avatar(hero.buff(HeroDisguise.class).getDisguise(), hero.tier());
		} else {
			return avatar(hero.heroClass, skinTier(hero.heroClass, hero.tier()));
		}
	}
	
	public static Image avatar( HeroClass cl, int armorTier ) {

		RectF patch = tiers( cl ).get( armorTier );
		Image avatar = new Image( cl.spritesheet() );
		int[] fs = frameSize( cl );
		RectF frame = avatar.texture.uvRect( 1, 0, fs[0], fs[1] );
		frame.shift( patch.left, patch.top );
		avatar.frame( frame );

		return avatar;
	}
}
