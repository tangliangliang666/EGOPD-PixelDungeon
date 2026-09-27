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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.valencina;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

/**
 * 拇指·前二老板（瓦伦希娜）专属音效的集中入口（2026-09-18）。
 *
 * <p>把「哪条语音、在什么条件下播」收在这一处，调用方只管调一句。
 * 好处与 {@code Instruction.playBuzz()} 同理：条件与去抖只写一次，
 * 也便于日后按角色开关（所有语音都要求当前英雄是拇指，见 {@link #isValencina()}）。</p>
 *
 * <p>素材来自边狱公司灰机 wiki「敌方单位/1314」，统一转码为
 * 44100Hz / 单声道 / 64kbps。语音类做过限幅 + 响度归一：
 * 均值约 <b>−15dB</b>（比项目音效的 −20dB 高一档——人声内含自然停顿，
 * 按音效目标归一会导致说话段听感偏轻，2026-09-18 用户反馈后上调）。</p>
 */
public class ValencinaSfx {

	private ValencinaSfx() {}

	//==========================================================================
	// 素材分组
	//==========================================================================

	/** 巴勒莫剑术施放时随机播出的语音（四选一）。 */
	private static final String[] PALERMO_VOICES = {
			Assets.Sounds.VALENCINA_PALERMO_VOICE_1,
			Assets.Sounds.VALENCINA_PALERMO_VOICE_2,
			Assets.Sounds.VALENCINA_PALERMO_VOICE_3,
			Assets.Sounds.VALENCINA_PALERMO_VOICE_4,
	};

	/** 上表各条的时长（秒，转码后实测），用于「上一条未播完则不重复播放」。 */
	private static final float[] PALERMO_VOICE_DUR = { 2.09f, 0.89f, 1.33f, 1.96f };

	/** 巴勒莫剑术命中时的攻击音效（八选一）。 */
	private static final String[] PALERMO_HITS = {
			Assets.Sounds.VALENCINA_PALERMO_HIT_1,
			Assets.Sounds.VALENCINA_PALERMO_HIT_2,
			Assets.Sounds.VALENCINA_PALERMO_HIT_3,
			Assets.Sounds.VALENCINA_PALERMO_HIT_4,
			Assets.Sounds.VALENCINA_PALERMO_HIT_5,
			Assets.Sounds.VALENCINA_PALERMO_HIT_6,
			Assets.Sounds.VALENCINA_PALERMO_HIT_7,
			Assets.Sounds.VALENCINA_PALERMO_HIT_8,
	};

	/**
	 * 「处置」（盔甲技能2）施放时的语音与台词。
	 * <p>⚠️ 必须成对取用：文本与语音各自独立随机会出现「显示 A、念的却是 B」的错配。</p>
	 */
	private static final String[][] DISPOSAL_LINES = {
			{ "我爱恨就恨！我恨所有那该死的家族，你们，还有券券！", Assets.Sounds.VALENCINA_DISPOSAL_VOICE_1 },
			{ "烦死了，烦死了啊！你们和券券，全都烦死了！！！", Assets.Sounds.VALENCINA_DISPOSAL_VOICE_2 },
	};

	/** 台词浮字的颜色（沿用原作台词页的暖米色）。 */
	private static final int LINE_COLOR = 0xE5CAA5;

	/** 换弹声在攻击结束后的延迟（秒）——太短会与攻击音效糊在一起。 */
	private static final float RELOAD_DELAY = 0.4f;

	//==========================================================================
	// 状态
	//==========================================================================

	private static int lastVoiceIdx = -1;
	private static float lastVoiceAt = -999f;

	/** 这些语音只在拇指（前二老板）身上播放。 */
	public static boolean isValencina() {
		return Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.VALENCINA;
	}

	//==========================================================================
	// 播放口
	//==========================================================================

	/** 闪避成功头顶弹出「怎么，打不中吗？」时的嘲讽语音（其他台词不出声）。 */
	public static void playDodgeTaunt() {
		if (!isValencina()) return;
		Sample.INSTANCE.play( Assets.Sounds.VALENCINA_DODGE_TAUNT );
	}

	/** 预知眼过热：过热期间第一次回合开始的抱怨语音。 */
	public static void playOverheat() {
		if (!isValencina()) return;
		Sample.INSTANCE.play( Assets.Sounds.VALENCINA_OVERHEAT );
	}

	/**
	 * 巴勒莫剑术施放时的语音（四选一）。
	 * <p>若上一条尚未播完则本次跳过，避免连续施放时语音叠成一团。</p>
	 */
	public static void playPalermoVoice() {
		if (!isValencina()) return;

		//上一条还没播完 → 不重复播放。用 Game.timeTotal（累计秒）；
		//前半条件防 Game 重启后 timeTotal 归零、而静态字段仍是旧值时误判为「正在播放」。
		if (lastVoiceIdx >= 0 && Game.timeTotal > lastVoiceAt
				&& Game.timeTotal - lastVoiceAt < PALERMO_VOICE_DUR[lastVoiceIdx]) {
			return;
		}

		lastVoiceIdx = Random.Int( PALERMO_VOICES.length );
		lastVoiceAt = Game.timeTotal;
		Sample.INSTANCE.play( PALERMO_VOICES[lastVoiceIdx] );
	}

	/** 巴勒莫剑术命中时的攻击音效（八选一）。 */
	public static void playPalermoHit() {
		Sample.INSTANCE.play( Random.element( PALERMO_HITS ) );
	}

	/** 「处置」施放时的语音 + 头顶同步台词（成对随机）。 */
	public static void playDisposalVoice( Hero hero ) {
		if (!isValencina()) return;

		String[] line = Random.element( DISPOSAL_LINES );
		Sample.INSTANCE.play( line[1] );
		if (hero != null && hero.sprite != null) {
			hero.sprite.showStatus( LINE_COLOR, line[0] );
		}
	}

	/**
	 * 加速弹状态下攻击结束的换弹声。
	 * <p>延后 {@link #RELOAD_DELAY} 秒播放，否则会与本次攻击的音效重合。</p>
	 */
	public static void playReload() {
		if (!isValencina()) return;
		Sample.INSTANCE.playDelayed( Assets.Sounds.VALENCINA_RELOAD, RELOAD_DELAY );
	}
}
