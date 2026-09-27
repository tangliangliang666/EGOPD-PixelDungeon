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
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;

public enum Music {
	
	INSTANCE;
	
	private com.badlogic.gdx.audio.Music player;
	
	private String lastPlayed;
	private boolean looping;
	
	private boolean enabled = true;
	private float volume = 1f;

	private float fadeTime = -1f;
	private float fadeTotal = -1f;
	private Callback onFadeOut = null;

	String[] trackList;
	float[] trackChances;
	private final ArrayList<String> trackQueue = new ArrayList<>();
	boolean shuffle = false;

	//=================== 曲目替换钩子（mod 用，2026-09-18） ===================
	
	/**
	 * 曲目替换钩子。mod 层注册之后，可以在**不碰任何调用点**的前提下把要播的曲目换掉
	 * （本作用它实现「角色专属 BGM」：只要开关开着、且当前英雄有专属曲，请求的
	 * 楼层曲 / BOSS 曲 / 紧张曲统统会被换成那一首）。{@code override} 返回 {@code null} 表示不干预。
	 */
	public interface TrackOverride {
		String override( String requestedTrack );
	}
	
	private TrackOverride trackOverride;
	
	/** 注册曲目替换钩子（传 {@code null} 取消）。 */
	public synchronized void setTrackOverride( TrackOverride override ) {
		trackOverride = override;
	}
	
	/**
	 * 询问钩子本次是否要改播别的曲目。
	 * 返回的曲目与请求的相同（或无钩子）时返回 {@code null}＝照常播放请求的曲目，
	 * 于是「同一个钩子在替换后的曲目上再被问一次」是幂等的，不会打转。
	 */
	private String overridden( String requested ) {
		if (trackOverride == null) return null;
		String forced = trackOverride.override( requested );
		return (forced == null || forced.equals( requested )) ? null : forced;
	}
	
	public synchronized void play( String assetName, boolean looping ) {

		//iOS cannot play ogg, so we use an mp3 alternative instead
		if (assetName != null && DeviceCompat.isiOS()){
			assetName = assetName.replace(".ogg", ".mp3");
		}

		//曲目替换钩子（mod）：命中时改播该曲目，并按单曲循环处理
		String forced = overridden( assetName );
		if (forced != null){
			assetName = forced;
			looping = true;
		}

		if (isPlaying() && lastPlayed != null && lastPlayed.equals( assetName )) {
			player.setVolume(volumeWithFade());
			return;
		}
		
		stop();
		
		lastPlayed = assetName;
		trackList = null;

		this.looping = looping;
		this.shuffle = false;

		if (!enabled || assetName == null) {
			return;
		}

		play(assetName, null);
	}

	public synchronized void playTracks( String[] tracks, float[] chances, boolean shuffle){

		if (tracks == null || tracks.length == 0 || tracks.length != chances.length){
			stop();
			return;
		}

		//曲目替换钩子（mod）：命中时整个随机列表被「该曲目单曲循环」取代
		String forced = overridden( tracks[0] );
		if (forced != null){
			play( forced, true );
			return;
		}

		//iOS cannot play ogg, so we use an mp3 alternative instead
		if (tracks != null && DeviceCompat.isiOS()){
			for (int i = 0; i < tracks.length; i ++){
				tracks[i] = tracks[i].replace(".ogg", ".mp3");
			}
		}

		if (isPlaying() && this.trackList != null && tracks.length == trackList.length){

			//lists are considered the same if they are identical or merely shifted
			// e.g. the regular title theme and the victory theme are considered equivalent
			boolean sameList = false;
			for (int ofs = 0; ofs < tracks.length; ofs++){
				sameList = true;
				for (int j = 0; j < tracks.length; j++){
					int i = (j+ofs)%tracks.length;
					if (!tracks[i].equals(trackList[j]) || chances[i] != trackChances[j]){
						sameList = false;
						break;
					}
				}
				if (sameList) break;
			}

			if (sameList) {
				player.setVolume(volumeWithFade());
				return;
			}
		}

		stop();

		lastPlayed = null;
		trackList = tracks;
		trackChances = chances;
		trackQueue.clear();

		for (int i = 0; i < trackList.length; i++){
			if (Random.Float() < trackChances[i]){
				trackQueue.add(trackList[i]);
			}
		}

		this.looping = false;
		this.shuffle = shuffle;

		if (!enabled || trackQueue.isEmpty()){
			return;
		}

		play(trackQueue.remove(0), trackLooper);
	}

	public synchronized void fadeOut(float duration, Callback onComplete){
		if (fadeTotal == -1f) {
			fadeTotal = duration;
			fadeTime = 0f;
		} else {
			fadeTime = (fadeTime/fadeTotal) * duration;
			fadeTotal = duration;
		}
		onFadeOut = onComplete;
	}

	public synchronized void update(){
		if (fadeTotal > 0f && !paused){
			fadeTime += Game.elapsed;

			if (player != null) {
				player.setVolume(volumeWithFade());
			}

			if (fadeTime >= fadeTotal) {
				fadeTime = fadeTotal = -1f;
				if (onFadeOut != null){
					onFadeOut.call();
				}
			}
		}
	}

	private com.badlogic.gdx.audio.Music.OnCompletionListener trackLooper = new com.badlogic.gdx.audio.Music.OnCompletionListener() {
		@Override
		public void onCompletion(com.badlogic.gdx.audio.Music music) {
			//don't play the next track if we're currently in the middle of a fade
			if (fadeTotal == -1f) {
				//we do this in a separate thread to avoid graphics hitching while the music is prepared
				if (!DeviceCompat.isDesktop()) {
					new Thread() {
						@Override
						public void run() {
							playNextTrack(music);
						}
					}.start();
				} else {
					//don't use a separate thread on desktop, causes errors and makes no performance difference
					playNextTrack(music);
				}
			}
		}
	};

	private synchronized void playNextTrack(com.badlogic.gdx.audio.Music music){
		if (trackList == null || trackList.length == 0 || music != player || player.isLooping()){
			return;
		}

		Music.this.stop();

		if (trackQueue.isEmpty()) {
			for (int i = 0; i < trackList.length; i++) {
				if (Random.Float() < trackChances[i]) {
					trackQueue.add(trackList[i]);
				}
			}
			if (shuffle) Collections.shuffle(trackQueue);
		}

		if (!enabled || trackQueue.isEmpty()) {
			return;
		}

		play(trackQueue.remove(0), trackLooper);
	};

	private synchronized void play(String track, com.badlogic.gdx.audio.Music.OnCompletionListener listener){
		try {
			fadeTime = fadeTotal = -1;

			player = Gdx.audio.newMusic(Gdx.files.internal(track));
			player.setLooping(looping);
			player.setVolume(volumeWithFade());
			if (!paused) player.play();
			if (listener != null) {
				player.setOnCompletionListener(listener);
			}
		} catch (Exception e){
			Game.reportException(e);
			player = null;
		}
	}
	
	public synchronized void end() {
		lastPlayed = null;
		trackList = null;
		stop();
	}

	private boolean paused = false;

	public synchronized boolean paused(){
		return paused;
	}
	
	public synchronized void pause() {
		paused = true;
		if (player != null) {
			player.pause();
		}
	}
	
	public synchronized void resume() {
		paused = false;
		if (player != null) {
			player.play();
			player.setLooping(looping);
		}
	}

	public synchronized void stop() {
		if (player != null) {
			player.dispose();
			player = null;
		}
	}
	
	public synchronized void volume( float value ) {
		volume = value;
		if (player != null) {
			player.setVolume( volumeWithFade() );
		}
	}

	private synchronized float volumeWithFade(){
		if (fadeTotal > 0f){
			return Math.max(0, volume * ((fadeTotal - fadeTime) / fadeTotal));
		} else {
			return volume;
		}
	}
	
	public synchronized boolean isPlaying() {
		return player != null && player.isPlaying();
	}
	
	public synchronized void enable( boolean value ) {
		enabled = value;
		if (isPlaying() && !value) {
			stop();
		} else
		if (!isPlaying() && value) {
			if (trackList != null){
				playTracks(trackList, trackChances, shuffle);
			} else if (lastPlayed != null) {
				play(lastPlayed, looping);
			}
		}
	}
	
	public synchronized boolean isEnabled() {
		return enabled;
	}
	
}
