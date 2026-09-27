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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.watabou.noosa.audio.Music;

/**
 * 四位自定义角色的「专属 BGM」（2026-09-18）。
 *
 * <p>食指·神谕代行者（{@link HeroClass#ORACLE}）、环指大师（{@link HeroClass#RING_MASTER}）、
 * 中指·长兄（{@link HeroClass#MIDDLE_FINGER}）、拇指·前二老板（{@link HeroClass#VALENCINA}）
 * 各有一首专属曲目。开关是<b>一个总开关</b>（{@link SPDSettings#heroBgm()}），
 * 开在哪位角色身上就放哪首——换句话说：开关管的是「要不要用专属曲」，
 * 用哪首由<b>当前英雄的职业</b>决定。</p>
 *
 * <p><b>怎么做到「替换全部 BGM」而不用改几十个调用点</b>：楼层、BOSS、紧张曲的播放散落在
 * 十几处（各 {@code Level.playLevelMusic()}、{@code YogDzewa} / {@code DwarfKing} 的中途变奏等），
 * 逐个改既容易漏又会污染原版代码。所以替换点在<b>引擎层</b>：{@link Music#setTrackOverride}——
 * 任何一次 {@code Music.INSTANCE.play(...)} / {@code playTracks(...)} 在真正解码之前都会先问一遍
 * 本类「这一首要不要换掉」。于是这里返回什么，整个游戏就放什么。</p>
 *
 * <p><b>什么时候不干预</b>（{@link #track()} 返回 {@code null}）：开关关着、当前没有英雄
 * （标题画面 {@code Dungeon.hero == null}）、或者英雄不属于上面四位。<br>
 * 最后一条同时也是「非专属角色不出现该选项」的依据：设置界面用
 * {@link #hasBgm(HeroClass)} 判断要不要把这一项画出来。</p>
 */
public class HeroBgm {

	private HeroBgm() {}

	/**
	 * 把本类接到引擎的音乐播放链路上。应在游戏启动时调用一次
	 * （见 {@code ShatteredPixelDungeon.create()}）。
	 */
	public static void register() {
		Music.INSTANCE.setTrackOverride( new Music.TrackOverride() {
			@Override
			public String override( String requestedTrack ) {
				return track();
			}
		} );
	}

	/**
	 * 当前应当强制播放的曲目；{@code null} = 不干预，照原样播放请求的曲目。
	 * <p>这个方法会在<b>每次</b>音乐播放请求时被调用，所以只做几个字段判断、不加日志。</p>
	 */
	public static String track() {
		if (!SPDSettings.heroBgm()) {
			return null;
		}
		return trackFor( activeClass() );
	}

	/** 指定职业的专属曲目；没有专属曲目（含 {@code null}）时返回 {@code null}。 */
	public static String trackFor( HeroClass cls ) {
		if (cls == null) {
			return null;
		}
		switch (cls) {
			case ORACLE:        return Assets.Music.HERO_ORACLE;
			case RING_MASTER:   return Assets.Music.HERO_RING_MASTER;
			case MIDDLE_FINGER: return Assets.Music.HERO_MIDDLE_FINGER;
			case VALENCINA:     return Assets.Music.HERO_VALENCINA;
			default:            return null;
		}
	}

	/** 该职业是否有专属曲目（设置界面据此决定要不要显示开关）。 */
	public static boolean hasBgm( HeroClass cls ) {
		return trackFor( cls ) != null;
	}

	/** 当前英雄的职业；没有英雄（标题画面、选人界面等）时为 {@code null}。 */
	public static HeroClass activeClass() {
		return Dungeon.hero != null ? Dungeon.hero.heroClass : null;
	}

	/**
	 * 开关被拨动之后调用，让新设置立刻生效（不必等下一次进楼层）。
	 *
	 * <p>做法是让当前关卡重新走一遍「这一层该放什么曲」的判定：{@code playLevelMusic()} 里的请求
	 * 会再次经过替换钩子，于是开 → 切成专属曲、关 → 切回原版曲。已经在放同一首时
	 * {@link Music#play} 会直接返回，所以不会把音乐打断重来。</p>
	 */
	public static void refresh() {
		if (Dungeon.level == null) {
			return;
		}
		Dungeon.level.playLevelMusic();
	}
}
