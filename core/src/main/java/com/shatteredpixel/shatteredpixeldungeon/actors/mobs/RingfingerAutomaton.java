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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DirectableAlly;
import com.shatteredpixel.shatteredpixeldungeon.items.BloodMaterial;
import com.shatteredpixel.shatteredpixeldungeon.items.BoneMaterial;
import com.shatteredpixel.shatteredpixeldungeon.items.HardMaterial;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.MeatMaterial;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BodyArtWeapon;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.DollSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * 画廊导师的召唤物「环指自动人偶」。
 * <p>AI 继承 {@link DirectableAlly}（可指挥友方框架，同枯萎玫瑰的悲伤幽灵 GhostHero）：
 * 支持跟随英雄 / 驻守点位 / 攻击指定目标三种指令，由「指挥」展品技艺下达。</p>
 * <p>基础数值公式参考蜜蜂（Bee），但驱动参数由「蜜蜂等级」换成「英雄等级」：
 * 人偶的数值随画廊导师的等级成长。</p>
 * <p>设定中人偶不能说话，接收指令时只有「咔哒咔哒」的机械音 + 状态陈述。</p>
 * <p>专属贴图 doll.png（{@link DollSprite}，13×14 帧）；
 * 借「作品交叉展览」天赋可装备人体派作品作为武器（模式同 GhostHero 借武器），死亡时武器掉落。</p>
 */
public class RingfingerAutomaton extends DirectableAlly {

	//召唤配方（2026-09-03 调整，共 5 件）：2 硬质 + 1 肉质 + 1 血质 + 1 骨质
	public static final int REQ_HARD  = 2;
	public static final int REQ_MEAT  = 1;
	public static final int REQ_BLOOD = 1;
	public static final int REQ_BONE  = 1;

	/** 配方文本（供展品技艺列表展示）。 */
	public static String reqText() {
		return REQ_HARD + "硬质、" + REQ_MEAT + "肉质、" + REQ_BLOOD + "血质、" + REQ_BONE + "骨质素材";
	}

	{
		spriteClass = DollSprite.class;

		EXP = 0;

		properties.add(Property.INORGANIC); //人偶造物：免疫流血/中毒等生物效果
	}

	public RingfingerAutomaton() {
		super();
		syncStats();
	}

	//========== 借用人体系派作品作为武器（模式同 DriedRose.GhostHero） ==========

	/** 人偶当前装备的人体派作品（脱离背包、随人偶存档，死亡时掉落）。 */
	private BodyArtWeapon weapon = null;

	public BodyArtWeapon weapon(){
		return weapon;
	}

	/** 装备一件人体派作品（调用前物品已从背包 detach）。 */
	public void setWeapon( BodyArtWeapon w ){
		this.weapon = w;
	}

	@Override
	public int attackSkill( Char target ) {
		//蜜蜂公式：命中 = 防御，实时跟随英雄等级；装备作品时乘武器精准因子
		int acc = 9 + heroLevel();
		if (weapon != null){
			acc *= weapon.accuracyFactor( this, target );
		}
		return acc;
	}

	@Override
	public float attackDelay() {
		float delay = super.attackDelay();
		if (weapon != null){
			delay *= weapon.delayFactor( this );
		}
		return delay;
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		return super.canAttack( enemy ) || (weapon != null && weapon.canReach( this, enemy.pos ));
	}

	@Override
	public int damageRoll() {
		//装备作品时使用武器伤害（同 GhostHero）；空手时使用蜜蜂公式：正态分布(HT/10 ~ HT/4)
		if (weapon != null){
			return weapon.damageRoll( this );
		}
		int eff = effHT();
		return Random.NormalIntRange( eff / 10, eff / 4 );
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		if (weapon != null){
			damage = weapon.proc( this, enemy, damage );
		}
		return damage;
	}

	@Override
	public int drRoll() {
		int dr = super.drRoll();
		if (weapon != null){
			dr += Random.NormalIntRange( 0, weapon.defenseFactor( this ) );
		}
		return dr;
	}

	@Override
	public void die( Object cause ) {
		//死亡时掉落装备的人体派作品
		if (weapon != null){
			Dungeon.level.drop( weapon, pos ).sprite.drop( pos );
			weapon = null;
		}
		super.die( cause );
	}

	private static final String WEAPON = "weapon";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		if (weapon != null) bundle.put( WEAPON, weapon );
	}

	//========== 指令响应（机械音 + 状态陈述；人偶不能说话） ==========

	@Override
	public void defendPos(int cell) {
		yell( Random.element( new String[]{"咔哒咔哒……", "咔哒、咔哒……", "咔……咔哒……"} )
				+ "环指自动人偶正在驻守此处。" );
		super.defendPos(cell);
	}

	@Override
	public void followHero() {
		yell( Random.element( new String[]{"咔哒咔哒……", "咔哒、咔哒……", "咔……咔哒……"} )
				+ "环指自动人偶正在向您靠拢。" );
		super.followHero();
	}

	@Override
	public void targetChar(Char ch) {
		yell( Random.element( new String[]{"咔哒咔哒！", "咔哒！咔哒！", "咔……咔哒咔哒！"} )
				+ "环指自动人偶正在攻击目标。" );
		super.targetChar(ch);
	}

	//========== 基础数值（公式参考 Bee.spawn(int level)，level = 英雄等级） ==========

	/** 英雄等级快照（读档恢复英雄早于楼层，因此 restoreFromBundle 时可安全取用）。 */
	private static int heroLevel() {
		return Dungeon.hero == null ? 1 : Dungeon.hero.lvl;
	}

	/** 等效蜜蜂等级值：HT = (2 + level) × 4。 */
	private static int effHT() {
		return (2 + heroLevel()) * 4;
	}

	/** 召唤 / 读档时同步 HP 与生命上限（防御、命中、伤害为动态计算）。 */
	private void syncStats() {
		HT = effHT();
		HP = HT;
		defenseSkill = 9 + heroLevel();
	}

	@Override
	protected boolean act() {
		//「展品自动维护」：英雄有该天赋时，人偶常驻维护 buff（天赋后学也能补挂）
		if (Dungeon.hero != null
				&& Dungeon.hero.heroClass == HeroClass.RING_MASTER
				&& Dungeon.hero.hasTalent( Talent.EXHIBIT_MAINTENANCE )
				&& buff( ExhibitMaintenance.class ) == null){
			ExhibitMaintenance.apply( this ); //挂上当回合先不结算
		}
		return super.act();
	}

	@Override
	public void damage(int dmg, Object src) {
		//「激发」效果持续期间不会死亡：致命伤害至少保留 1 点生命
		if (buff( Stimulated.class ) != null && dmg >= HP){
			dmg = Math.max( HP - 1, 0 );
		}
		super.damage( dmg, src );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		if (bundle.contains( WEAPON )) weapon = (BodyArtWeapon) bundle.get( WEAPON );
		//读档时按当前英雄等级重算 HT（升级后的存档人偶同步成长），旧 HP 保留但不超上限
		int oldHP = HP;
		syncStats();
		HP = Math.min( Math.max( oldHP, 1 ), HT );
	}

	@Override
	public int defenseSkill( Char enemy ) {
		//动态计算：升级后已召唤的人偶防御也实时跟随英雄等级
		return 9 + heroLevel();
	}

	@Override
	public String name() {
		return "环指自动人偶";
	}

	@Override
	public String description() {
		String desc = "一件由环指大师亲手搭建的展品，以硬质材料为骨架。\n\n" +
				"它不会说话，只会以「咔哒」的机械音回应指令，忠诚地服从画廊导师的每一个指令，" +
				"其性能会随画廊导师的成长而一同精进。";
		if (weapon != null){
			desc += "\n\n它的手中握着一件人体派作品：_" + weapon.name() + "_。";
		}
		return desc;
	}

	//========== 召唤材料检查与消耗（供 ConstructExhibit 调用） ==========

	/** 单种素材的持有量是否达标（getItem 会递归搜索背包与素材箱）。 */
	private static boolean hasEnough( Class<? extends Item> cls, int req ) {
		Item mat = Dungeon.hero.belongings.getItem( cls );
		return mat != null && mat.quantity() >= req;
	}

	/** 背包（含素材箱）中是否有足够的召唤材料（4 种全部达标）。 */
	public static boolean hasMaterials() {
		return hasEnough( HardMaterial.class,  REQ_HARD )
				&& hasEnough( MeatMaterial.class,  REQ_MEAT )
				&& hasEnough( BloodMaterial.class, REQ_BLOOD )
				&& hasEnough( BoneMaterial.class,  REQ_BONE );
	}

	/** 单种素材不足时的提示文本。 */
	private static String matName( Class<? extends Item> cls ) {
		if (cls == HardMaterial.class)  return "硬质";
		if (cls == MeatMaterial.class)  return "肉质";
		if (cls == BloodMaterial.class) return "血质";
		if (cls == BoneMaterial.class)  return "骨质";
		return "未知";
	}

	/** 消耗召唤材料（4 种全部达标才消耗），不足时提示并返回 false。 */
	public static boolean consumeMaterials() {
		@SuppressWarnings("unchecked")
		Class<? extends Item>[] reqCls = new Class[]{ HardMaterial.class, MeatMaterial.class, BloodMaterial.class, BoneMaterial.class };
		int[] reqAmt = { REQ_HARD, REQ_MEAT, REQ_BLOOD, REQ_BONE };

		for (int i = 0; i < reqCls.length; i++){
			Item mat = Dungeon.hero.belongings.getItem( reqCls[i] );
			if (mat == null || mat.quantity() < reqAmt[i]){
				GLog.w( "材料不足：需要 _" + reqText() + "_（缺" + matName( reqCls[i] ) + "素材）。" );
				return false;
			}
		}
		for (int i = 0; i < reqCls.length; i++){
			Item mat = Dungeon.hero.belongings.getItem( reqCls[i] );
			mat.quantity( mat.quantity() - reqAmt[i] );
			if (mat.quantity() <= 0){
				//detachAll 会递归搜索背包与素材箱，移除数量归零的堆叠
				mat.detachAll( Dungeon.hero.belongings.backpack );
			}
		}
		Item.updateQuickslot();
		return true;
	}

	//========== 展品相关 buff（画廊导师天赋效果） ==========

	/**
	 * 「展品自动维护」：周期性恢复 1 点生命（天赋 +1/_20 回合、+2/_10 回合、+3/_5 回合）。
	 * <p>常驻 buff：英雄没有该天赋时自行分离；每回合 spend(TICK) 保持存活。</p>
	 */
	public static class ExhibitMaintenance extends Buff {

		{
			type = buffType.POSITIVE;
			revivePersists = true;
		}

		private int cooldown = 0;

		/** 施加维护 buff（挂上当回合先不结算，下回合起计数）。 */
		public static ExhibitMaintenance apply( Char target ){
			ExhibitMaintenance b = Buff.affect( target, ExhibitMaintenance.class );
			b.spend( TICK );
			return b;
		}

		private static int interval( int points ){
			if (points >= 3) return 5;
			if (points == 2) return 10;
			return 20;
		}

		@Override
		public boolean act() {
			if (target == null || !target.isAlive()){
				detach();
				return true;
			}
			Hero hero = Dungeon.hero;
			int points = hero != null ? hero.pointsInTalent( Talent.EXHIBIT_MAINTENANCE ) : 0;
			if (points <= 0){
				detach();
				spend( TICK );
				return true;
			}

			if (cooldown <= 0) cooldown = interval( points );
			cooldown--;
			if (cooldown <= 0){
				if (target.HP < target.HT){
					if (target.heal( 1 ) > 0 && target.sprite != null && target.sprite.visible){
						target.sprite.showStatus( CharSprite.POSITIVE, "1" );
					}
				}
				cooldown = interval( points );
			}

			spend( TICK );
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.HEALING;
		}

		@Override
		public String name() {
			return "展品自动维护";
		}

		@Override
		public String desc() {
			return "环指自动人偶正在被画廊导师的天赋自动维护，周期性地恢复生命。";
		}

		private static final String COOLDOWN = "cooldown";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( COOLDOWN, cooldown );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			cooldown = bundle.getInt( COOLDOWN );
		}
	}

	/**
	 * 「激发」效果（画廊即时修整 +3）：10 回合的_众矢之的_（每回合嘲讽附近敌人）+ 期间不会死亡
	 * （死亡免疫在 {@link RingfingerAutomaton#damage(int, Object)} 中钳制致命伤害）。
	 */
	public static class Stimulated extends Buff {

		{
			type = buffType.POSITIVE;
		}

		public static final int DURATION = 10;

		/** 嘲讽半径（格）。 */
		private static final int TAUNT_RANGE = 8;

		private int turnsLeft = 0;

		/** 施加激发效果（固定 10 回合）。 */
		public static Stimulated apply( Char target ){
			Stimulated s = Buff.affect( target, Stimulated.class );
			s.turnsLeft = DURATION;
			s.spend( TICK ); //施加当回合先不结算，下回合起每回合嘲讽
			return s;
		}

		@Override
		public boolean act() {
			if (target == null || !target.isAlive() || turnsLeft <= 0){
				detach();
				return true;
			}

			//众矢之的：周期性强迫附近的敌人以人偶为目标
			if (Dungeon.level != null){
				for (Char ch : Actor.chars()){
					if (ch != target
							&& ch.alignment == Char.Alignment.ENEMY
							&& ch instanceof Mob
							&& Dungeon.level.distance( ch.pos, target.pos ) <= TAUNT_RANGE){
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
			return BuffIndicator.CHALLENGE;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight( 1f, 0.2f, 0.2f );
		}

		@Override
		public float iconFadePercent() {
			return Math.max( 0, (DURATION - turnsLeft) / (float) DURATION );
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString( turnsLeft );
		}

		@Override
		public String name() {
			return "众矢之的";
		}

		@Override
		public String desc() {
			return "环指自动人偶被「激发」了：它吸引着附近所有敌人的攻击，并且在效果持续期间不会死亡。\n\n"
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
		}
	}
}
