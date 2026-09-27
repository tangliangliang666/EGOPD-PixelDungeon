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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.BloodMaterial;
import com.shatteredpixel.shatteredpixeldungeon.items.BoneMaterial;
import com.shatteredpixel.shatteredpixeldungeon.items.HardMaterial;
import com.shatteredpixel.shatteredpixeldungeon.items.MeatMaterial;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.ShrapnelBomb;
import com.shatteredpixel.shatteredpixeldungeon.sprites.DollSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * 「诱饵」（环指大师盔甲技能召唤的环指诱饵人偶，2026-09-04）。
 * <p>套用环指自动人偶的贴图（{@link DollSprite}），血量为其 1.5 倍；
 * AI 不会主动移动；每隔 50 回合在它身上触发一次盛怒卷轴的效果与特效
 * （呼唤全层怪物朝它涌来），持续吸引本层的敌人。</p>
 * <p>可被「诱惑的青光」强化为持续 N 回合的众矢之的（全层嘲讽）；
 * 死亡时按「爆裂之美」天赋触发炸弹/破片炸弹爆炸；
 * 攻击它的敌人按「痴迷欣赏」天赋获得 debuff。</p>
 */
public class DecoyDoll extends Mob {

	{
		spriteClass = DollSprite.class;

		EXP = 0;
		maxLvl = 0;

		alignment = Char.Alignment.ALLY;

		properties.add(Property.INORGANIC); //人偶造物：免疫流血/中毒等生物效果
	}

	/** 盛怒嚎叫的间隔（回合）。 */
	public static final int HOWL_INTERVAL = 50;

	/** 英雄等级快照（读档恢复英雄早于楼层，因此 restoreFromBundle 时可安全取用）。 */
	private static int heroLevel() {
		return Dungeon.hero == null ? 1 : Dungeon.hero.lvl;
	}

	/** 等效环指自动人偶血量：HT=(2+level)×4；诱饵为其 1.5 倍 → (2+level)×6。 */
	private static int dollHT() {
		return (int)((2 + heroLevel()) * 4 * 1.5f);
	}

	/** 召唤 / 读档时同步 HP 与生命上限。 */
	private void syncStats() {
		HT = dollHT();
		HP = HT;
	}

	private int howlCooldown = HOWL_INTERVAL;

	/** 静态工厂：在指定格召唤诱饵人偶并加入场景。 */
	public static DecoyDoll spawn(int pos){
		DecoyDoll doll = new DecoyDoll();
		doll.pos = pos;
		doll.state = doll.PASSIVE;
		doll.syncStats();
		com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(doll);
		return doll;
	}

	@Override
	protected boolean act() {
		//诱饵不会移动：不走 AI 行为（buff 由 Actor 队列自行调度，无需手动驱动）

		//每 50 回合触发一次盛怒卷轴的效果与特效：呼唤本层所有怪物涌向诱饵
		howlCooldown--;
		if (howlCooldown <= 0){
			howlCooldown = HOWL_INTERVAL;

			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
				if (mob != this && mob.isAlive() && mob.alignment == Char.Alignment.ENEMY){
					mob.beckon( pos );
					if (Dungeon.level.heroFOV[mob.pos]){
						Buff.prolong(mob, Amok.class, 5f);
					}
				}
			}

			if (sprite != null){
				sprite.centerEmitter().start( Speck.factory( Speck.SCREAM ), 0.3f, 3 );
			}
			Sample.INSTANCE.play( Assets.Sounds.CHALLENGE );
		}

		spend( TICK );
		return true;
	}

	//========== 天赋效果 ==========

	/** 痴迷欣赏：被敌人攻击命中时，按天赋等级给攻击者 debuff（各 5 回合）。 */
	@Override
	public int defenseProc(Char enemy, int damage) {
		damage = super.defenseProc(enemy, damage);
		if (damage > 0 && enemy instanceof Mob){
			int points = Dungeon.hero != null ? Dungeon.hero.pointsInTalent(Talent.OBSESSIVE_ADMIRATION) : 0;
			if (points >= 1) Buff.prolong(enemy, Cripple.class, 5f);
			if (points >= 2) Buff.prolong(enemy, Hex.class, 5f);
			if (points >= 3) Buff.prolong(enemy, Vulnerable.class, 5f);
			if (points >= 4) Buff.prolong(enemy, Daze.class, 5f);
		}
		return damage;
	}

	/** 爆裂之美：死亡时按天赋等级触发炸弹/破片炸弹爆炸。 */
	@Override
	public void die( Object cause ) {
		int points = Dungeon.hero != null ? Dungeon.hero.pointsInTalent(Talent.EXPLOSIVE_BEAUTY) : 0;
		if (points > 0){
			boolean shrapnel;
			if (points >= 4){
				shrapnel = true;                          //+4：必定破片炸弹
			} else if (points == 3){
				shrapnel = Random.Float() < 0.5f;         //+3：50% 破片炸弹
			} else if (points == 2){
				shrapnel = false;                         //+2：必定普通炸弹
			} else {
				shrapnel = false;                         //+1：50% 概率普通炸弹
				if (Random.Float() >= 0.5f){
					super.die(cause);
					return;
				}
			}
			Bomb bomb = shrapnel ? new ShrapnelBomb() : new Bomb();
			bomb.explode( pos );
		}
		super.die( cause );
	}

	//========== 数值与文本 ==========

	@Override
	public int defenseSkill( Char enemy ) {
		//同环指自动人偶：防御 = 9 + 英雄等级（诱饵不动，仅作肉盾）
		return 9 + heroLevel();
	}

	@Override
	public String name() {
		return "环指诱饵人偶";
	}

	@Override
	public String description() {
		return "一件由环指大师制作的诱饵人偶。它纹丝不动地矗立着，却每隔一段时间便会发出刺耳的嚎叫，\n"
				+ "将本层游荡的敌人都吸引到自己身边——为大师创造出手艺施展的空间。";
	}

	private static final String HOWL_COOLDOWN = "howl_cooldown";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put( HOWL_COOLDOWN, howlCooldown );
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		howlCooldown = bundle.getInt( HOWL_COOLDOWN );
		if (howlCooldown <= 0) howlCooldown = 1;
		//读档时按当前英雄等级重算 HT，旧 HP 保留但不超上限
		int oldHP = HP;
		syncStats();
		HP = Math.min( Math.max( oldHP, 1 ), HT );
	}

	/**
	 * 「诱惑的青光」：持续 N 回合的众矢之的——每回合强迫本层所有敌人以诱饵为目标。
	 * 与「激发」Stimulated 的 8 格嘲讽不同，这是全层嘲讽。
	 */
	public static class SeductiveGlow extends Buff {

		{
			type = buffType.POSITIVE;
		}

		private int turnsLeft = 0;

		/** 施加诱惑的青光（按天赋点 10/15/20/30 回合）。 */
		public static SeductiveGlow apply( Char target ){
			Hero hero = Dungeon.hero;
			int points = hero != null ? hero.pointsInTalent( Talent.SEDUCTIVE_GLOW ) : 0;
			int duration;
			switch (points){
				case 1: default:  duration = 10; break;
				case 2:           duration = 15; break;
				case 3:           duration = 20; break;
				case 4:           duration = 30; break;
			}
			SeductiveGlow glow = Buff.affect( target, SeductiveGlow.class );
			glow.turnsLeft = duration;
			glow.spend( TICK ); //施加当回合先不结算，下回合起每回合嘲讽
			return glow;
		}

		@Override
		public boolean act() {
			if (target == null || !target.isAlive() || turnsLeft <= 0){
				detach();
				return true;
			}

			//众矢之的：每回合强迫本层所有敌人以诱饵为目标
			if (Dungeon.level != null){
				for (Char ch : Actor.chars()){
					if (ch != target
							&& ch.alignment == Char.Alignment.ENEMY
							&& ch instanceof Mob){
						((Mob) ch).aggro( target );
					}
				}
			}

			turnsLeft--;
			if (turnsLeft <= 0){
				detach();
			} else {
				spend( TICK );
			}
			return true;
		}

		@Override
		public int icon() {
			return com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.CHALLENGE;
		}

		@Override
		public void tintIcon( com.watabou.noosa.Image icon ) {
			icon.hardlight( 0.4f, 0.9f, 1f ); //青光
		}

		@Override
		public float iconFadePercent() {
			return 0;
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString( turnsLeft );
		}

		@Override
		public String name() {
			return "诱惑的青光";
		}

		@Override
		public String desc() {
			return "环指诱饵人偶周身泛起诱惑的青光，本层的敌人都被它牢牢吸引。\n\n"
					+ "剩余 " + turnsLeft + " 回合。";
		}

		private static final String TURNS_LEFT = "turns_left";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( TURNS_LEFT, turnsLeft );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			turnsLeft = bundle.getInt( TURNS_LEFT );
			if (turnsLeft <= 0) turnsLeft = 1;
		}
	}
}
