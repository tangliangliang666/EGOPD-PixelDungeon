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

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.TitleScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.WelcomeScene;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.PlatformSupport;

public class ShatteredPixelDungeon extends Game {

	//rankings from v1.2.3 and older use a different score formula, so this reference is kept
	public static final int v1_2_3 = 628;

	//edge-detection state for the debug F2 hotkey in update()
	private boolean f2WasDown = false;

	//savegames from versions older than v2.5.4 are no longer supported, and data from them is ignored
	public static final int v2_5_4 = 802;

	public static final int v3_0_2 = 833;
	public static final int v3_1_1 = 850;
	public static final int v3_2_5 = 877;
	public static final int v3_3_0 = 883;
	
	public ShatteredPixelDungeon( PlatformSupport platform ) {
		super( sceneClass == null ? WelcomeScene.class : sceneClass, platform );

		//pre-v3.3.0
		com.watabou.utils.Bundle.addAlias(
				com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey.class,
				"com.shatteredpixel.shatteredpixeldungeon.items.keys.SkeletonKey" );

		//中指长兄的盔甲技能：占位类改名成正式名（2026-09-17）。
		//hero.armorAbility 是「按类名」存档的（Bundle.put(Bundlable)），而 Bundle 的还原走
		//aliases 表，所以旧存档里的占位类名必须在启动时登记成新类，否则读档时那一格会变成 null。
		com.watabou.utils.Bundle.addAlias(
				com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.middlefinger.GritTeeth.class,
				"com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.middlefinger.MiddleFingerAbilityOne" );
		com.watabou.utils.Bundle.addAlias(
				com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.middlefinger.NeverForget.class,
				"com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.middlefinger.MiddleFingerAbilityTwo" );
		com.watabou.utils.Bundle.addAlias(
				com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.middlefinger.InstantExecution.class,
				"com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.middlefinger.MiddleFingerAbilityThree" );

	}
	
	@Override
	public void create() {
		super.create();

		updateSystemUI();
		SPDAction.loadBindings();
		
		Music.INSTANCE.enable( SPDSettings.music() );
		Music.INSTANCE.volume( SPDSettings.musicVol()*SPDSettings.musicVol()/100f );
		Sample.INSTANCE.enable( SPDSettings.soundFx() );
		Sample.INSTANCE.volume( SPDSettings.SFXVol()*SPDSettings.SFXVol()/100f );

		Sample.INSTANCE.load( Assets.Sounds.all );

		//四位自定义角色的「专属 BGM」：把替换钩子接到引擎的音乐链路上（原版曲目不动，
		//只有开关开着且当前英雄有专属曲时才顶替）。见 HeroBgm。
		HeroBgm.register();

		//调试窗口：游戏内原生 windows/WndDebug（WndTabbed + ScrollingListPane）。
		//门控（是否可开）见 debug/SpdDebugConsole —— 只在本局开启「调试模式」挑战时可用。
		//不再依赖独立的 :debug-console 库。

	}

	@Override
	protected void update() {
		super.update();

		//Debug console hotkey: poll F2 directly via libGDX, bypassing the "bound keys only" filter.
		//Edge-detects so we only open once per press.
		//Gating: dev build + an active run + the DEBUG_MODE challenge (see SpdDebugConsole).
		if (com.shatteredpixel.shatteredpixeldungeon.debug.SpdDebugConsole.hotkeyAllowed()) {
			boolean f2Down = Gdx.input.isKeyPressed(Input.Keys.F2);
			if (f2Down && !f2WasDown) {
				if (com.shatteredpixel.shatteredpixeldungeon.debug.SpdDebugConsole.isAvailable()) {
					//SPD-native window (WndTabbed + ScrollingListPane), shown like any other window
					GameScene.show(new com.shatteredpixel.shatteredpixeldungeon.windows.WndDebug());
				} else if (Dungeon.hero == null || Dungeon.level == null) {
					DeviceCompat.log("DEBUG", "调试窗口需在进入一局游戏后按 F2 打开");
				} else {
					DeviceCompat.log("DEBUG", "调试窗口仅在本局开启了「调试模式」挑战时可用（F2 已禁用）");
				}
			}
			f2WasDown = f2Down;
		}
	}

	@Override
	public void finish() {
		if (!DeviceCompat.isiOS()) {
			super.finish();
		} else {
			//can't exit on iOS (Apple guidelines), so just go to title screen
			switchScene(TitleScene.class);
		}
	}

	public static void switchNoFade(Class<? extends PixelScene> c){
		switchNoFade(c, null);
	}

	public static void switchNoFade(Class<? extends PixelScene> c, SceneChangeCallback callback) {
		PixelScene.noFade = true;
		switchScene( c, callback );
	}
	
	public static void seamlessResetScene(SceneChangeCallback callback) {
		if (scene() instanceof PixelScene){
			((PixelScene) scene()).saveWindows();
			switchNoFade((Class<? extends PixelScene>) sceneClass, callback );
		} else {
			resetScene();
		}
	}
	
	public static void seamlessResetScene(){
		seamlessResetScene(null);
	}
	
	@Override
	protected void switchScene() {
		super.switchScene();
		if (scene instanceof PixelScene){
			((PixelScene) scene).restoreWindows();
		}
	}
	
	@Override
	public void resize( int width, int height ) {
		if (width == 0 || height == 0){
			return;
		}

		if (scene instanceof PixelScene &&
				(height != Game.height || width != Game.width)) {
			PixelScene.noFade = true;
			((PixelScene) scene).saveWindows();
		}

		super.resize( width, height );

		updateDisplaySize();

	}
	
	@Override
	public void destroy(){
		super.destroy();
		GameScene.endActorThread();
	}
	
	public void updateDisplaySize(){
		platform.updateDisplaySize();
	}

	public static void updateSystemUI() {
		platform.updateSystemUI();
	}
}