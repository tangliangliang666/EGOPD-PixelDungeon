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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SnipersMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.TenguDartTrap;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * 新星之声 —— 五阶E.G.O投掷武器（可炼金重构获得，亦登记入五阶投掷武器掉落池）。
 *
 * <p>面板套用震爆方石（ForceCube，默认公式 10+L ~ 25+5L），不卡入敌人，
 * 投掷飞行旋转角速度 360°/s（见 {@code MissileSprite}）。
 * 效果：与震爆方石一致的落地小范围 AoE 冲击，且与回旋镖一样在掷出
 * <b>一回合后</b>自动旋回投掷者手中（重型回旋镖为五回合）。</p>
 */
public class NovaVoice extends MissileWeapon {

	{
		image = ItemSpriteSheet.NOVA_VOICE;

		tier = 5;
		baseUses = 5;

		sticky = false;
	}

	@Override
	public void hitSound(float pitch) {
		//no hitsound as it never hits enemies directly（AoE 以爆炸音表现）
	}

	@Override
	public float castDelay(Char user, int cell) {
		//special rules as throwing this onto empty space or yourself does trigger it
		if (!Dungeon.level.pit[cell] && Actor.findChar(cell) == null){
			return delayFactor( user );
		} else {
			return super.castDelay(user, cell);
		}
	}

	boolean circlingBack = false;

	@Override
	protected float adjacentAccFactor(Char owner, Char target) {
		if (circlingBack){
			return 1.5f;
		}
		return super.adjacentAccFactor(owner, target);
	}

	@Override
	public float pickupDelay() {
		//pickup is instant when circling back
		return circlingBack ? 0f : super.pickupDelay();
	}

	@Override
	protected void onThrow(int cell) {
		if ((Dungeon.level.pit[cell] && Actor.findChar(cell) == null)){
			super.onThrow(cell); //坠入深渊，无法旋回
			return;
		}

		//结算本发耐久（仿 rangedHit 的损耗口径；损坏则本发损毁不旋回）
		//保留 parent 引用以便随后多次 AoE 命中的鉴定/耐久同步
		MissileWeapon parentTemp = parent;
		decrementDurability();
		parent = parentTemp;
		Dungeon.level.pressCell(cell);

		ArrayList<Char> targets = new ArrayList<>();
		Char primaryTarget;
		if (Actor.findChar(cell) != null) {
			primaryTarget = Actor.findChar(cell);
			targets.add(primaryTarget);
		} else {
			primaryTarget = null;
		}

		for (int i : PathFinder.NEIGHBOURS8){
			if (!(Dungeon.level.traps.get(cell+i) instanceof TenguDartTrap)) Dungeon.level.pressCell(cell+i);
			if (Actor.findChar(cell + i) != null) targets.add(Actor.findChar(cell + i));
		}

		//furthest to closest, mainly for elastic
		Collections.sort(targets, new Comparator<Char>() {
			@Override
			public int compare(Char a, Char b) {
				return Float.compare(Dungeon.level.trueDistance(b.pos, curUser.pos), Dungeon.level.trueDistance(a.pos, curUser.pos));
			}
		});

		for (Char target : targets){
			curUser.shoot(target, this);
			if (target == Dungeon.hero && !target.isAlive()){
				Badges.validateDeathFromFriendlyMagic();
				Dungeon.fail(this);
				GLog.n(Messages.get(this, "ondeath"));
			}
		}

		//if we're applying sniper's mark, prioritize giving it to the primary target of the attack
		if (curUser.subClass == HeroSubClass.SNIPER && primaryTarget != null && primaryTarget.isActive()){
			Actor.add(new Actor() {

				{
					actPriority = VFX_PRIO-1;
				}

				@Override
				protected boolean act() {
					SnipersMark mark = Dungeon.hero.buff(SnipersMark.class);
					if (mark != null && primaryTarget.isActive()){
						mark.object = primaryTarget.id();
					}
					Actor.remove(this);
					return true;
				}
			});
		}

		WandOfBlastWave.BlastWave.blast(cell);
		Sample.INSTANCE.play( Assets.Sounds.BLAST );

		//一回合后自动旋回（与重型回旋镖同款机制，等待回合数=1）
		parent = null;
		if (durability > 0 && !spawnedForEffect){
			Buff.append(Dungeon.hero, CircleBack.class).setup(this, cell, Dungeon.hero.pos, Dungeon.depth, Dungeon.branch);
		}
	}

	public static class CircleBack extends Buff {

		{
			revivePersists = true;
		}

		private NovaVoice novaVoice;
		private int thrownPos;
		private int returnPos;
		private int returnDepth;
		private int returnBranch;

		private int left;

		public void setup( NovaVoice novaVoice, int thrownPos, int returnPos, int returnDepth, int returnBranch){
			this.novaVoice = novaVoice;
			this.thrownPos = thrownPos;
			this.returnPos = returnPos;
			this.returnDepth = returnDepth;
			this.returnBranch = returnBranch;
			left = 1;
		}

		public int returnPos(){
			return returnPos;
		}

		public MissileWeapon cancel(){
			detach();
			return novaVoice;
		}

		public int activeDepth(){
			return returnDepth;
		}

		@Override
		public boolean act() {
			if (returnDepth == Dungeon.depth && returnBranch == Dungeon.branch){
				left--;
				if (left <= 0){
					final Char returnTarget = Actor.findChar(returnPos);
					final Char target = this.target;
					MissileSprite visual = ((MissileSprite) Dungeon.hero.sprite.parent.recycle(MissileSprite.class));
					visual.reset( thrownPos,
									returnPos,
									novaVoice,
									new Callback() {
										@Override
										public void call() {
											detach();
											novaVoice.circlingBack = true;
											if (returnTarget == target){
												if (!novaVoice.spawnedForEffect) {
													if (!(target instanceof Hero) || !novaVoice.doPickUp((Hero) target)) {
														Dungeon.level.drop(novaVoice, returnPos).sprite.drop();
													}
												}

											} else if (returnTarget != null){
												if (((Hero)target).shoot( returnTarget, novaVoice )) {
													novaVoice.decrementDurability();
												}
												if (!novaVoice.spawnedForEffect && novaVoice.durability > 0) {
													Dungeon.level.drop(novaVoice, returnPos).sprite.drop();
												}

											} else if (!novaVoice.spawnedForEffect) {
												Dungeon.level.drop(novaVoice, returnPos).sprite.drop();
											}
											novaVoice.circlingBack = false;
											CircleBack.this.next();
										}
									});
					visual.alpha(0f);
					float duration = Dungeon.level.trueDistance(thrownPos, returnPos) / 20f;
					target.sprite.parent.add(new AlphaTweener(visual, 1f, duration));
					return false;
				}
			}
			spend( TICK );
			return true;
		}

		private static final String NOVA_VOICE = "nova_voice";
		private static final String THROWN_POS = "thrown_pos";
		private static final String RETURN_POS = "return_pos";
		private static final String RETURN_DEPTH = "return_depth";
		private static final String RETURN_BRANCH = "return_branch";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(NOVA_VOICE, novaVoice);
			bundle.put(THROWN_POS, thrownPos);
			bundle.put(RETURN_POS, returnPos);
			bundle.put(RETURN_DEPTH, returnDepth);
			bundle.put(RETURN_BRANCH, returnBranch);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			novaVoice = (NovaVoice) bundle.get(NOVA_VOICE);
			thrownPos = bundle.getInt(THROWN_POS);
			returnPos = bundle.getInt(RETURN_POS);
			returnDepth = bundle.getInt(RETURN_DEPTH);
			returnBranch = bundle.contains(RETURN_BRANCH) ? bundle.getInt(RETURN_BRANCH) : 0;
		}
	}

}
