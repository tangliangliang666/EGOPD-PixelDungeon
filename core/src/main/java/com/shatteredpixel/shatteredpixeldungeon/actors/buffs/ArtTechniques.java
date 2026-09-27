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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.BloodMaterial;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BodyArtWeapon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndArtSkill;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 艺术之巅专精特性「艺术技艺」（无需加点的分支特性，2026-09-03 实现）。
 * <p>永久 buff（转职艺术之巅时由天狗面具施加），承载右下角 {@link ActionIndicator} 技能按钮
 * （实现模式照抄命运弃子的 {@link ScorchingWound}）。点击后打开 {@link WndArtSkill}
 * 选择招数，三招各消耗手中人体派作品的 10 点数值（血/骨/肉）：</p>
 * <ul>
 *   <li>{@link ArtMove#BATHED_BLOOD} 材料获取-浴血之物（10 血值，攻击范围）：
 *       目标累积等同于武器伤害值的流血，掉落 1 个血质素材；天赋 +1 追加 3 回合麻痹。</li>
 *   <li>{@link ArtMove#SIMPLE_RIB} 装置艺术-简易肋骨（10 骨值，8 格）：
 *       必中攻击 + 3 回合人体观剧；天赋 +2 延长为 10 回合。</li>
 *   <li>{@link ArtMove#DISSECT} 以被解剖之物解剖未解剖之物（10 肉值，8 格）：
 *       瞬移到主目标身边，主目标 200% 伤害、周围 3 格 100% 伤害；
 *       天赋 +3 对主目标追加伤害值四分之一的流血。</li>
 * </ul>
 */
public class ArtTechniques extends Buff implements ActionIndicator.Action {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	//三招的统一消耗
	public static final int COST = 10;

	//永久 buff：不计时、不分离
	@Override
	public boolean act() {
		spend( TICK );
		return true;
	}

	//========== 右下角技能入口（参考 ScorchingWound 模式） ==========

	private boolean shouldShowAction(){
		return target instanceof Hero
				&& ((Hero) target).subClass == HeroSubClass.ART_PINNACLE;
	}

	@Override
	public String actionName() {
		return "艺术技艺";
	}

	@Override
	public int actionIcon() {
		return HeroIcon.ART_PINNACLE;
	}

	@Override
	public int indicatorColor() {
		return 0xBB66DD; //艺术紫
	}

	@Override
	public void doAction() {
		GameScene.show( new WndArtSkill( this ) );
	}

	//绑定目标后（含读档 attachTo）注册右下角技能入口
	@Override
	public boolean attachTo( Char target ) {
		if (super.attachTo( target )){
			if (shouldShowAction()){
				ActionIndicator.setAction( this );
			}
			return true;
		}
		return false;
	}

	@Override
	public void detach() {
		super.detach();
		ActionIndicator.clearAction( this );
	}

	@Override
	public String name() {
		return "艺术技艺";
	}

	@Override
	public String desc() {
		return "艺术之巅可以消耗手中人体派作品的数值发动特殊技艺。\n\n"
				+ "点击右下角的按钮（或按对应快捷键）打开技艺列表。";
	}

	//========== 招数定义 ==========

	public enum ArtMove {
		BATHED_BLOOD,  //材料获取-浴血之物（10 血值）
		SIMPLE_RIB,    //装置艺术-简易肋骨（10 骨值）
		DISSECT;       //以被解剖之物解剖未解剖之物（10 肉值）

		public String title(){
			switch (this){
				case BATHED_BLOOD: default: return "材料获取-浴血之物";
				case SIMPLE_RIB: return "装置艺术-简易肋骨";
				case DISSECT: return "以被解剖之物解剖未解剖之物";
			}
		}

		public String costName(){
			switch (this){
				case BATHED_BLOOD: default: return "血值";
				case SIMPLE_RIB: return "骨值";
				case DISSECT: return "肉值";
			}
		}

		public String desc(){
			switch (this){
				case BATHED_BLOOD: default:
					return "消耗手中作品的_10_点血值：对_攻击范围内_的目标立刻累积等同于当前武器伤害值的_流血_，并使其掉落_1_个血质素材。";
				case SIMPLE_RIB:
					return "消耗手中作品的_10_点骨值：对_8格内_的目标进行一次_必中攻击_，并使其陷入_3_回合的_人体观剧_状态（无法移动和攻击）。";
				case DISSECT:
					return "消耗手中作品的_10_点肉值：选定_8格内_的主目标，立刻移动到其身边，对主目标造成_200%_伤害，并对其周围_3格_内的所有目标造成_100%_伤害。";
			}
		}
	}

	/** 招数是否可用：手中持有人体派作品且对应数值 ≥ 10。 */
	public boolean canUse( ArtMove move ){
		return costWeapon( (Hero) target, move ) != null;
	}

	//取满足消耗的武器；不满足返回 null
	private static BodyArtWeapon costWeapon( Hero hero, ArtMove move ){
		if (!(hero.belongings.attackingWeapon() instanceof BodyArtWeapon)) return null;
		BodyArtWeapon w = (BodyArtWeapon) hero.belongings.attackingWeapon();
		int v;
		switch (move){
			case BATHED_BLOOD: default: v = w.bloodValue; break;
			case SIMPLE_RIB:     v = w.boneValue;  break;
			case DISSECT:        v = w.meatValue;  break;
		}
		return v >= COST ? w : null;
	}

	//扣减消耗（血/骨/肉值不影响强化等级——等级仅由武器值决定，无需 syncReinforceLevel）
	private static void payCost( BodyArtWeapon w, ArtMove move ){
		switch (move){
			case BATHED_BLOOD: default: w.bloodValue -= COST; break;
			case SIMPLE_RIB:     w.boneValue  -= COST; break;
			case DISSECT:        w.meatValue  -= COST; break;
		}
	}

	//选择目标并使用招数（由 WndArtSkill 调用）
	public void useMove( final ArtMove move ){
		if (!(target instanceof Hero)) return;
		final Hero hero = (Hero) target;
		GameScene.selectCell( new CellSelector.Listener() {
			@Override
			public void onSelect( Integer cell ) {
				if (cell == null) return;
				Char enemy = Actor.findChar( cell );
				if (enemy == null || enemy == hero || !Dungeon.level.heroFOV[cell] || hero.isCharmedBy( enemy )){
					GLog.w( "无效的目标" );
					return;
				}
				int dist = Dungeon.level.distance( hero.pos, enemy.pos );
				BodyArtWeapon w = costWeapon( hero, move );
				if (w == null){
					GLog.w( "手中的作品数值不足（需要_10_点" + move.costName() + "）" );
					return;
				}
				switch (move){
					case BATHED_BLOOD:
						if (!hero.canAttack( enemy )){
							GLog.w( "目标不在攻击范围内" );
							return;
						}
						break;
					case SIMPLE_RIB:
						if (dist > 8){
							GLog.w( "目标过远" );
							return;
						}
						break;
					case DISSECT:
						if (dist > 8){
							GLog.w( "目标过远" );
							return;
						}
						if (emptyCellAround( enemy ) == -1){
							GLog.w( "目标周围没有可站立的空地" );
							return;
						}
						break;
				}
				useMoveOnTarget( hero, w, enemy, move );
			}
			@Override
			public String prompt() {
				return "选择艺术技艺的目标";
			}
		});
	}

	//对选定目标执行招数
	private void useMoveOnTarget( final Hero hero, final BodyArtWeapon w, final Char enemy, final ArtMove move ){
		final int talent = hero.pointsInTalent( Talent.BONE_TENDON_MELODY );

		//瞬移招数先找落点
		final int blinkCell;
		if (move == ArtMove.DISSECT){
			blinkCell = emptyCellAround( enemy );
			if (blinkCell == -1){
				GLog.w( "目标周围没有可站立的空地" );
				return;
			}
		} else {
			blinkCell = -1;
		}

		//扣减数值消耗
		payCost( w, move );
		hero.busy();

		switch (move){
			case BATHED_BLOOD: {
				//材料获取-浴血之物：攻击动作 → 累积等同武器伤害值的流血 + 掉落血质素材（不直接造成伤害）
				hero.sprite.attack( enemy.pos, new Callback() {
					@Override
					public void call() {
						AttackIndicator.target( enemy );
						if (enemy.isAlive()){
							int bleedAmount = w.damageRoll( hero );
							Bleeding bleed = Buff.affect( enemy, Bleeding.class );
							bleed.set( bleedAmount );
							//天赋 +1：3 回合麻痹
							if (talent >= 1){
								Buff.prolong( enemy, Paralysis.class, 3f );
							}
						}
						//掉落 1 个血质素材
						Dungeon.level.drop( new BloodMaterial(), enemy.pos ).sprite.drop();
						Sample.INSTANCE.play( Assets.Sounds.HIT_SLASH );
						hero.spendAndNext( hero.attackDelay() );
					}
				});
				break;
			}
			case SIMPLE_RIB: {
				//装置艺术-简易肋骨：必中攻击 + 人体观剧（天赋 +2 延长为 10 回合）
				hero.sprite.attack( enemy.pos, new Callback() {
					@Override
					public void call() {
						AttackIndicator.target( enemy );
						if (hero.attack( enemy, 1f, 0, Char.INFINITE_ACCURACY )){
							Sample.INSTANCE.play( Assets.Sounds.HIT_STRONG );
						}
						if (enemy.isAlive()){
							float duration = talent >= 2 ? 10f : 3f;
							BodyTheater bt = Buff.affect( enemy, BodyTheater.class, duration );
							if (bt != null) bt.maxDuration = duration;
						}
						hero.spendAndNext( hero.attackDelay() );
					}
				});
				break;
			}
			case DISSECT: {
				//以被解剖之物解剖未解剖之物：瞬移到主目标身边 → 主目标 200% → 周围 3 格 100%
				ScrollOfTeleportation.appear( hero, blinkCell );
				hero.sprite.attack( enemy.pos, new Callback() {
					@Override
					public void call() {
						AttackIndicator.target( enemy );
						final int beforeHP = enemy.HP;
						if (hero.attack( enemy, 2f, 0, Char.INFINITE_ACCURACY )){
							Sample.INSTANCE.play( Assets.Sounds.HIT_STRONG );
						}
						//天赋 +3：主目标追加伤害值四分之一的流血
						if (talent >= 3 && enemy.isAlive()){
							int dealt = Math.max( 0, beforeHP - enemy.HP );
							if (dealt > 0){
								Buff.affect( enemy, Bleeding.class ).set( dealt / 4f );
							}
						}
						//主目标周围 3 格内的所有敌方单位受到 100% 伤害
						for (Mob ch : Dungeon.level.mobs.toArray( new Mob[0] )){
							if (ch == enemy || !ch.isAlive() || ch.alignment != Char.Alignment.ENEMY) continue;
							if (!Dungeon.level.heroFOV[ch.pos]) continue;
							if (Dungeon.level.distance( enemy.pos, ch.pos ) <= 3){
								AttackIndicator.target( ch );
								if (hero.attack( ch, 1f, 0, Char.INFINITE_ACCURACY )){
									Sample.INSTANCE.play( Assets.Sounds.HIT_STRONG );
								}
							}
						}
						hero.spendAndNext( hero.attackDelay() );
					}
				});
				break;
			}
		}
	}

	//寻找目标周围一个可站立的空地（8 邻域随机），没有则返回 -1
	private static int emptyCellAround( Char target ){
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8){
			int c = target.pos + i;
			if (Dungeon.level.insideMap( c ) && Dungeon.level.passable[c] && Actor.findChar( c ) == null){
				candidates.add( c );
			}
		}
		if (candidates.isEmpty()) return -1;
		return Random.element( candidates );
	}

	//无自有字段；保留空存档方法以明确该 buff 随存档持久化（基类已处理）
	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		//读档兜底注册右下角按钮：Hero.restoreFromBundle 先恢复 buffs（attachTo）后恢复 subClass，
		//attachTo 里依赖 subClass 的判断必然失败；而本 buff 只会在转职 ART_PINNACLE 时被施加，
		//buff 存在即代表子类正确，故在此直接注册（同官方 Berserk.restoreFromBundle 模式）
		ActionIndicator.setAction( this );
	}
}
