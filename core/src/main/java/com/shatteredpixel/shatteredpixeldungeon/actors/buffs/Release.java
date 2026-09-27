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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * 剑刃解放的「解放」层数 buff。
 * <p>完成指令后获得 1 层，上限按 T2「剑刃解放」的点数取：+1 时 6 层、+2 时 9 层
 * （见 {@link #growthCap}）。
 * 拥有 X 层时，攻击额外造成 1~X 点伤害，受到攻击时额外减少 1~X 受到的伤害。</p>
 * <p>天赋 +2 时，最低层数随「业」积累而提升：业每 10 点保底 1 层，业 90 时保底 9 层。
 * 达到 9 层时飘出淡蓝与白色粒子。</p>
 */
public class Release extends Buff {

	/** T2 剑刃解放 +2 时的叠加上限（也是全局上限）。 */
	public static final int MAX_STACKS = 9;

	/** T2 剑刃解放 +1 时的叠加上限。 */
	public static final int MAX_STACKS_1 = 6;

	{
		type = buffType.POSITIVE;
	}

	private int stacks = 0;

	public int stacks(){
		return stacks;
	}

	/**
	 * 完成一条指令时能叠到的上限，由 T2「剑刃解放」的点数决定：
	 * {@code +1 ⇒ 6 层}、{@code +2 ⇒ 9 层}（0 点＝没这门天赋，返回 0，不叠）。
	 *
	 * <p>与 {@link Talent#BLADE_RELEASE} 的描述严格对齐——「解放最高叠加到 6 层 / 9 层」。
	 * 之前这里直接写死 {@link #MAX_STACKS}，导致 +1 时同样能叠到 9 层、与文本不符。</p>
	 */
	public static int growthCap( Hero hero ){
		if (hero == null) return 0;
		int points = hero.pointsInTalent( Talent.BLADE_RELEASE );
		if (points >= 2) return MAX_STACKS;
		if (points >= 1) return MAX_STACKS_1;
		return 0;
	}

	/** 本 buff 持有者（优先用 target，退化到当前英雄）。 */
	private Hero ownerHero(){
		if (target instanceof Hero) return (Hero) target;
		return Dungeon.hero;
	}

	public void gainStack(){
		//上限按天赋点数取（+1＝6 层 / +2＝9 层），不能一律用 MAX_STACKS
		if (stacks < growthCap( ownerHero() )){
			stacks++;
			BuffIndicator.refreshHero();
			checkParticles(); //达到 9 层时飘出淡蓝/白色粒子
		}
	}

	/**
	 * <b>调试专用</b>：无视天赋点数，直接叠 1 层（上限仍是 {@link #MAX_STACKS}）。
	 *
	 * <p>{@code SpdConsoleHost} 的「附加 buff」逻辑靠它把解放叠到 9 层看粒子/层数显示——
	 * 调试台不要求英雄真点出 T2，所以这里刻意<b>不走</b> {@link #growthCap}。
	 * 游戏内获得层数的唯一入口仍是 {@link #gainStack}。</p>
	 */
	public void debugGainStack(){
		if (stacks < MAX_STACKS){
			stacks++;
			BuffIndicator.refreshHero();
			checkParticles();
		}
	}

	/**
	 * +2 天赋下「业」提供的保底解放层数：业每 10 点 1 层，上限 {@link #MAX_STACKS}。
	 * 未点满 2 点（或身上没有「业」）时返回 0。
	 */
	public static int karmaFloor( Hero hero ){
		if (hero == null || hero.pointsInTalent(Talent.BLADE_RELEASE) < 2){
			return 0;
		}
		Karma karma = hero.buff(Karma.class);
		if (karma == null){
			return 0;
		}
		return Math.min(MAX_STACKS, karma.karma() / 10);
	}

	//有效层数：+2 时随业保底层数
	public int effectiveStacks(){
		return Math.max(stacks, karmaFloor(Dungeon.hero));
	}

	/**
	 * 复活后按「业」重新挂上解放 buff（神谕代行者 T2「剑刃解放」+2）。
	 *
	 * <p>解放 buff 在死亡时会被清除 —— {@code Hero.live()} 会 detach 所有
	 * {@code revivePersists == false} 的 buff；但「业」（{@link Karma}）是复活保留的。
	 * 若复活后不重新挂上，{@link #effectiveStacks()} 的保底层数就无从生效，
	 * 表现为「解放 buff 不再出现」、攻击附带伤害与受击减伤一并消失。</p>
	 *
	 * <p>由 {@code Hero.live()} 调用。新开局时英雄尚无天赋、保底为 0，这里是空操作。</p>
	 */
	public static void onHeroRevive( Hero hero ){
		if (hero == null || hero.buff(Release.class) != null){
			return;
		}
		if (karmaFloor(hero) > 0){
			Buff.affect(hero, Release.class);
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.RELEASE;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(effectiveStacks());
	}

	@Override
	public String name() {
		return "解放";
	}

	@Override
	public String desc() {
		return "拥有 " + effectiveStacks() + " 层时，攻击额外造成 1~" + effectiveStacks()
				+ " 点伤害，受到攻击时额外减少 1~" + effectiveStacks() + " 受到的伤害。";
	}

	//达到 9 层时飘出淡蓝与白色粒子（由 Talent.onAttackProc/伤害处理处调用刷新）
	public void checkParticles(){
		if (effectiveStacks() >= MAX_STACKS && target != null && target.sprite != null){
			if (target.sprite.emitter() != null){
				target.sprite.emitter().start(ReleaseParticle.FACTORY, 0.15f, 0);
			}
		}
	}

	//淡蓝/白色交替的粒子
	public static class ReleaseParticle extends PixelParticle {

		public static final Emitter.Factory FACTORY = new Emitter.Factory() {
			@Override
			public void emit(Emitter emitter, int index, float x, float y) {
				ReleaseParticle p = new ReleaseParticle();
				emitter.add(p);
				p.reset(x, y);
			}
		};

		public ReleaseParticle(){
			super();
			//淡蓝与白色交替
			if (Random.Int(2) == 0){
				color(0x8FE3FF);
			} else {
				color(0xFFFFFF);
			}
			lifespan = 0.6f + Random.Float(0.3f);
			speed.set(0, -20 - Random.Float(20));
			acc.set(0, 20);
			size = 1.5f + Random.Float(1f);
			angle = Random.Float(360);
		}

		public void reset(float x, float y){
			this.x = x;
			this.y = y;
			left = lifespan;
		}
	}

	private static final String STACKS = "stacks";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(STACKS, stacks);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		stacks = bundle.getInt(STACKS);
	}

}
