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

package com.watabou.noosa.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.watabou.noosa.Game;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public enum Sample {

	INSTANCE;

	protected HashMap<Object, Sound> ids = new HashMap<>();

	private boolean enabled = true;
	private float globalVolume = 1f;

	public synchronized void reset() {

		//别把「独占」状态带过界：Android 上活动被销毁重建时这里会被调用，
		//若此时仍占着 Music 的暂停位，新实例里背景音乐就再也起不来了。
		releaseExclusive();

		for (Sound sound : ids.values()){
			sound.dispose();
		}
		
		ids.clear();
		delayedSFX.clear();

	}

	public synchronized void pause() {
		for (Sound sound : ids.values()) {
			sound.pause();
		}
	}

	public synchronized void resume() {
		for (Sound sound : ids.values()) {
			sound.resume();
		}
	}

	public synchronized void load( final String asset){
		if (asset != null) {
			try {
				Sound newSound = Gdx.audio.newSound(Gdx.files.internal(asset));
				ids.put(asset, newSound);
			} catch (Exception e){
				Game.reportException(e);
			}
		}
	}

	private static final LinkedList<String> loadingQueue = new LinkedList<>();

	//queues multiple assets for loading, which happens in update()
	// this prevents blocking while we load many assets
	public void load( final String[] assets ) {
		synchronized (loadingQueue) {
			for (String asset : assets) {
				if (!ids.containsKey(asset) && !loadingQueue.contains(asset)) {
					loadingQueue.add(asset);
				}
			}
		}
	}

	public synchronized void unload( Object src ) {
		if (ids.containsKey( src )) {
			ids.get( src ).dispose();
			ids.remove( src );
		}
	}

	public long play( Object id ) {
		return play( id, 1 );
	}

	public long play( Object id, float volume ) {
		return play( id, volume, volume, 1 );
	}
	
	public long play( Object id, float volume, float pitch ) {
		return play( id, volume, volume, pitch );
	}
	
	public synchronized long play( Object id, float leftVolume, float rightVolume, float pitch ) {
		float volume = Math.max(leftVolume, rightVolume);
		float pan = rightVolume - leftVolume;
		if (enabled && ids.containsKey( id )) {
			return ids.get(id).play( globalVolume*volume, pitch, pan );
		} else {
			return -1;
		}
	}

	//=================== 独占音效（播放期间顶掉背景音乐） ===================
	//某些音效是有独立演出性的（例如自定义挑战「依旧果冻人」的果冻音效），
	//播放期间不希望背景音乐混杂进来。这里的做法是：播放时暂停 Music，播完自动恢复。
	//计时必须挂在 update()（Game.update() 每帧无条件调用，与当前场景无关），
	//这样即使中途切了场景，背景音乐也一定会被放回来。

	/** 是否有独占音效正在播放。 */
	private boolean exclusive = false;
	/** 独占音效剩余播放时间（秒）。 */
	private float exclusiveLeft = 0f;
	/** 独占期间「背景音乐是不是被我们暂停的」。播放前音乐本来就暂停（如窗口失焦）时置 false，
	 *  这样收尾时不会把它错误地恢复。 */
	private boolean exclusiveOwnsMusic = false;

	/**
	 * 播放一个「独占」音效：<b>从头</b>开始播放（同 id 正在播则先掐掉），
	 * 播放期间暂停背景音乐，播放结束或被下一次调用打断后自动恢复背景音乐。
	 *
	 * @param id       已加载的音效资源（未加载 / 音效被关掉时直接忽略，不会误暂停音乐）
	 * @param volume   音量（仍会乘以设置里的音效音量）
	 * @param pitch    音调 / 播放速率
	 * @param duration 该音效在当前音调下的实际播放时长（秒）；音调越高播得越快，
	 *                 所以调用方应按 {@code 素材时长 / pitch} 传进来
	 */
	public synchronized void playExclusive( Object id, float volume, float pitch, float duration ) {
		if (!enabled || !ids.containsKey( id )) {
			return;
		}

		//已经在放同一个音效 ⇒ 从头重来（Sound.play 是叠加式的，不 stop 会变成两只一起响）
		ids.get( id ).stop();

		if (!exclusive) {
			exclusive = true;
			exclusiveOwnsMusic = !Music.INSTANCE.paused();
			Music.INSTANCE.pause();
		}

		exclusiveLeft = Math.max( 0.01f, duration );
		play( id, volume, pitch );
	}

	/** 独占音效是否仍在播放（调用方可据此判断「是不是在播同一段、要不要升调重播」）。 */
	public synchronized boolean exclusivePlaying() {
		return exclusive;
	}

	/** 收尾：结束独占、把背景音乐放回来。 */
	private void releaseExclusive() {
		if (exclusive) {
			exclusive = false;
			exclusiveLeft = 0f;
			if (exclusiveOwnsMusic) {
				Music.INSTANCE.resume();
			}
			exclusiveOwnsMusic = false;
		}
	}

	private class DelayedSoundEffect{
		Object id;
		float delay;

		float leftVol;
		float rightVol;
		float pitch;
	}

	private static final HashSet<DelayedSoundEffect> delayedSFX = new HashSet<>();

	public void playDelayed( Object id, float delay ){
		playDelayed( id, delay, 1 );
	}

	public void playDelayed( Object id, float delay, float volume ) {
		playDelayed( id, delay, volume, volume, 1 );
	}

	public void playDelayed( Object id, float delay, float volume, float pitch ) {
		playDelayed( id, delay, volume, volume, pitch );
	}

	public void playDelayed( Object id, float delay, float leftVolume, float rightVolume, float pitch ) {
		if (delay <= 0) {
			play(id, leftVolume, rightVolume, pitch);
			return;
		}
		DelayedSoundEffect sfx = new DelayedSoundEffect();
		sfx.id = id;
		sfx.delay = delay;
		sfx.leftVol = leftVolume;
		sfx.rightVol = rightVolume;
		sfx.pitch = pitch;
		synchronized (delayedSFX) {
			delayedSFX.add(sfx);
		}
	}

	public void update(){
		synchronized (loadingQueue) {
			if (!loadingQueue.isEmpty()) {
				load(loadingQueue.poll());
			}
		}

		//独占音效：期间保持背景音乐静默（自愈——应用切后台再回来时系统会恢复音乐，这里再按回去），
		//播完把音乐放回来。注意这段必须放在下面 delayedSFX 的提前 return 之前。
		if (exclusive) {
			if (exclusiveOwnsMusic && !Music.INSTANCE.paused()) {
				Music.INSTANCE.pause();
			}
			exclusiveLeft -= Game.elapsed;
			if (exclusiveLeft <= 0f) {
				releaseExclusive();
			}
		}

		synchronized (delayedSFX) {
			if (delayedSFX.isEmpty()) return;
			for (DelayedSoundEffect sfx : delayedSFX.toArray(new DelayedSoundEffect[0])) {
				sfx.delay -= Game.elapsed;
				if (sfx.delay <= 0) {
					delayedSFX.remove(sfx);
					play(sfx.id, sfx.leftVol, sfx.rightVol, sfx.pitch);
				}
			}
		}
	}

	public void enable( boolean value ) {
		enabled = value;
	}

	public void volume( float value ) {
		globalVolume = value;
	}

	public boolean isEnabled() {
		return enabled;
	}
	
}