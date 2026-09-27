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

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

/**
 * 自定义挑战「依旧果冻人」（{@link Challenges#JELLY_PERSON}）的视觉效果。
 *
 * <p><b>挑战本身不改动任何数值与机制</b>，唯一的接入口就是本类：把「某张贴图摆一下」这件事
 * 交给引擎侧的 {@link Image#jellyWobble}（真正的分层形变见 {@code com.watabou.noosa.JellyDraw}）。
 * 三个触发点：</p>
 * <ul>
 *   <li>单位受击 —— {@code Char.damage()} 末尾（覆盖英雄 / 友方 / 敌人 / 中立的一切单位，
 *       近战、远程、法杖、炸弹、陷阱全都在这一处汇聚）；</li>
 *   <li>单位死亡 —— {@code Char.die()} 里 {@code sprite.die()} 之前，死亡动画也照样形变；</li>
 *   <li>物品落地 —— {@code ItemSprite.update()} 里投掷/掉落动画结束、贴图归位的那一刻。</li>
 * </ul>
 *
 * <p><b>为什么要筛「直接命中」</b>：燃烧、流血、中毒、腐蚀淤泥这类持续伤害每回合都会调一次
 * {@code Char.damage}，若照单全收，着火的目标会变成一直抖个不停的果冻。所以只认
 * {@link #isHit}——伤害来源是角色、武器、附魔、道具或陷阱才算「被打到」，
 * 以 buff 自身为源的持续伤害不算。</p>
 *
 * <p>摆动参数（幅度 / 时长 / 频率）都在本类，随时可调；摆动方向默认取「背离攻击者」。<br>
 * <b>音效</b>：每次触发都响一次 {@link #SFX}，播放期间顶掉背景音乐
 * （引擎侧的独占播放见 {@code Sample#playExclusive}），没播完就再次触发则从头重播并升调。<br>
 * 已知取舍：钻石剑图标的附魔流光用的是图标原始顶点（不在形变之列），所以地面上的
 * 钻石剑摆动时流光不跟着晃。</p>
 */
public class JellyWobble {

	/**
	 * 贴图顶部的最大横向偏移（像素）。脚底始终不动，越往上摆幅按高度平方递增
	 * ⇒ 顶端甩出整幅贴图的宽度还多（默认角色帧高 15、宽 12，16 像素≈一个多身位），
	 * 中段约 1/4 幅度，看上去就是「上半身被一巴掌扇飞又弹回来」的滑稽效果。
	 */
	public static final float AMP = 16f;

	/** 竖直压扁 / 拉长的最大比例（0.3 ⇒ 最多压扁 30%、拉长 30%，以脚底为基准）。 */
	public static final float SQUASH = 0.3f;

	/** 单次摆动时长（秒）。比之前长一截，尾巴上那几下「余震」才看得清。 */
	public static final float DURATION = 1.4f;

	/**
	 * 角频率（弧度/秒）。横向来回次数 ≈ {@code DURATION × FREQ / (2π)} ≈ 3.1 次
	 * （极值点个数 = ⌊DURATION×FREQ÷π⌋+1 ≈ 7），即「甩出去 → 回摆 → 过冲 → 三下余震」，
	 * 而不是 0.1 秒抖完的「假摆动」。竖直的压扁/拉长差 1/4 周期（cos → −sin），
	 * 所以每一次横甩后面都跟着一次「压扁 → 回弹」。
	 * <p>调「次数」只动 {@code DURATION}（次数线性）或本值（频率），别动层数。</p>
	 */
	public static final float FREQ = 14f;

	/** 死亡动画的幅度倍数：比受击更夸张一点，作为「最后的挣扎」。 */
	public static final float DEATH_BOOST = 1.3f;

	//=================== 音效（2026-09-17） ===================

	/** 果冻音效素材（`core/src/main/assets/sounds/jelly.mp3`）。 */
	public static final String SFX = Assets.Sounds.JELLY;

	/**
	 * 音效素材的原始时长（秒）。<b>换素材必须同步本值</b>——它只用来判断「什么时候算播完」
	 * （播完才恢复背景音乐、音调才会复位）。用 {@code python _chk/audio_format_check.py}
	 * 可以打印素材时长。当前素材：源文件是 5.83s 的 MP4/AAC，转码成 44100Hz 单声道 mp3 后实测 5.85s
	 * （多出的 0.02 秒是编码器的首尾补零，与播放器实际占用时长一致）。
	 */
	public static final float SFX_SECONDS = 5.85f;

	/**
	 * 音效音量（仍会乘以设置里的音效音量）。
	 *
	 * <p>素材是 5 秒的音乐而不是「一击一响」的音效，录音电平本来就比原版音效高，
	 * 所以刻意压到七成——否则一挨打就把整个场景的其他声音盖住。想再轻就往 0.5 走，
	 * 想更响就到 0.9，改这一个常量即可。</p>
	 *
	 * <p><b>0.73 是配着素材电平算出来的</b>（2026-09-18 素材重转后）：素材本身
	 * {@code I = −10.34 LUFS}，乘 0.73（−2.73dB）后与上一版「−7.4dB 均值 × 0.45」的
	 * 听感完全一致——<b>换素材后必须同步本值</b>，否则音量会跟着素材一起变。</p>
	 */
	public static final float SFX_VOLUME = 0.73f;

	/** 打断重播时音调的提升倍数（1.122 ≈ 每次升 2 个半音）。 */
	public static final float PITCH_STEP = 1.122f;

	/**
	 * 音调上限。取 2 是有硬道理的：Android 的 SoundPool 播放速率合法区间就是 0.5~2.0，
	 * 再高会被系统截断（也可能只在高版本上表现为变调）⇒ 约 +12 半音封顶。
	 */
	public static final float PITCH_MAX = 2f;

	/** 当前音调（没有音效在播时复位为 1）。 */
	private static float pitch = 1f;

	private JellyWobble() {}

	/** 本局是否开启了「依旧果冻人」挑战。 */
	public static boolean enabled() {
		return Dungeon.isChallenged( Challenges.JELLY_PERSON );
	}

	/**
	 * 单位受击。由 {@code Char.damage()} 调用（伤害已结算、且该单位还活着时）。
	 *
	 * @param target 被打到的单位
	 * @param src    伤害来源（用于判断是不是「直接命中」与推摆方向）
	 */
	public static void hit( Char target, Object src ) {
		if (!enabled() || target == null || target.sprite == null || !target.isAlive()) {
			return;
		}
		if (!isHit( src )) {
			return;
		}
		swing( target.sprite, dirFrom( target, src ), 1f );
	}

	/**
	 * 单位死亡、死亡动画开始的那一刻。由 {@code Char.die()} 调用。
	 *
	 * <p>方向沿用「致命一击的来向」⇒ 死亡动画的摆动会接在受击摆动后面同向甩出去，
	 * 看着就像一击把人打散架了。英雄其实也走这条线（{@code Hero.die()} 在没有复活手段时
	 * 会 {@code super.die(cause)}），所以「死亡动画形变」对任何单位都是一视同仁的，
	 * 不需要额外判断。</p>
	 */
	public static void death( Char target, Object src ) {
		if (!enabled() || target == null || target.sprite == null) {
			return;
		}
		swing( target.sprite, dirFrom( target, src ), DEATH_BOOST );
	}

	/**
	 * 物品扔出/掉落到地面、贴图归位的那一刻。由 {@code ItemSprite.update()} 调用。
	 */
	public static void land( Image sprite ) {
		if (!enabled() || sprite == null) {
			return;
		}
		//落地方向随机：同一个格子里连着落几件东西时不会整齐划一
		swing( sprite, Random.Int( 2 ) == 0 ? -1f : 1f, 1f );
	}

	/**
	 * 是不是「直接命中」。只认角色/武器/附魔/道具/陷阱作来源的伤害，
	 * 于是燃烧、流血、中毒、腐蚀淤泥等每回合跳一次的持续伤害不会触发摆动。
	 */
	public static boolean isHit( Object src ) {
		if (src == null) {
			return false;
		}
		return src instanceof Char
				|| src instanceof Weapon
				|| src instanceof Weapon.Enchantment
				|| src instanceof Item
				|| src instanceof Trap;
	}

	/** 摆动方向：攻击者在左就从左往右甩（顶端背离攻击者），拿不到攻击者时随机。 */
	private static float dirFrom( Char target, Object src ) {
		if (src instanceof Char && Dungeon.level != null) {
			Char attacker = (Char)src;
			int w = Dungeon.level.width();
			if (attacker != target && w > 0 && attacker.pos >= 0 && target.pos >= 0) {
				int ax = attacker.pos % w;
				int tx = target.pos % w;
				if (ax != tx) {
					return ax < tx ? 1f : -1f;
				}
			}
		}
		return Random.Int( 2 ) == 0 ? -1f : 1f;
	}

	/**
	 * 落一次摆动：幅度带 ±15% 随机，避免每次受击都一模一样。
	 *
	 * @param strength 幅度倍数（受击 1、死亡 {@link #DEATH_BOOST}）
	 */
	private static void swing( Image sprite, float dir, float strength ) {
		sprite.jellyWobble(
				AMP * strength * Random.Float( 0.85f, 1.15f ),
				SQUASH * strength,
				dir,
				DURATION,
				FREQ );
		sfx();
	}

	/**
	 * 响一次果冻音效。
	 *
	 * <p><b>表现规则</b>：每次触发都从头播放；播放期间<b>顶掉背景音乐</b>
	 * （见 {@link Sample#playExclusive}），播完自动把音乐放回来。若上一次还没播完就又触发了，
	 * 则从头重播并把音调再抬高一档（可叠加，{@link #PITCH_MAX} 封顶）——
	 * 于是连续挨打时会听到音调一路往上爬；音效自然播完后音调复位。</p>
	 */
	private static void sfx() {
		if (Sample.INSTANCE.exclusivePlaying()) {
			pitch = Math.min( pitch * PITCH_STEP, PITCH_MAX );
		} else {
			pitch = 1f;
		}
		//音调越高播得越快 ⇒ 实际占用时长按比例缩短
		Sample.INSTANCE.playExclusive( SFX, SFX_VOLUME, pitch, SFX_SECONDS / pitch );
	}
}
