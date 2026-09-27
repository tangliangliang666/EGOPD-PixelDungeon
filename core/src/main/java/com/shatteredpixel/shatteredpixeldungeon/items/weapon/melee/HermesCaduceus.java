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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Karma;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * 赫尔墨斯的双蛇杖。
 * <p>五阶武器。装备时获得常驻 buff「业」（卸下时解除）。</p>
 * <p>攻击前会随机选取一种武器形态，本次攻击完全采用该形态的数值与特殊机制。
 * 随机分两层：先随机阶位（当前各阶等概率，后续将根据「业」的层数调整各阶概率），
 * 再在同一阶内随机。阶位范围为 0~6：
 * 1~5 阶为原版近战武器（反射委托其数值与机制）；0 阶与 6 阶为内置伪武器
 * （「叉勺」「镰刀」，无需独立武器类，见 {@link FormSpec}）。</p>
 * <p>面板仍显示五阶占位数值；实际攻击数值随随机形态变化。</p>
 * <p>专属武器：{@code bones = false}，死亡后**不**进入英雄遗骸——否则下一局其它角色
 * 拾取遗骸就可能拿到这件神谕代行者专属武器。</p>
 */
public class HermesCaduceus extends MeleeWeapon {

	//========== 随机形态池 ==========

	//原版近战武器按阶分组（1~5 阶，排除 MagesStaff、Pickaxe 等不可作为随机形态的特殊件）
	@SuppressWarnings("unchecked")
	private static final Class<? extends MeleeWeapon>[][] TIER_WEAPONS = new Class[][]{
			//1阶
			{WornShortsword.class, Dagger.class, Gloves.class, Rapier.class, Cudgel.class},
			//2阶
			{Shortsword.class, HandAxe.class, Spear.class, Quarterstaff.class, Dirk.class, Sickle.class},
			//3阶
			{Sword.class, Mace.class, Scimitar.class, RoundShield.class, Sai.class, Whip.class},
			//4阶（含 Crossbow，其近战面板可正常套用）
			{Longsword.class, BattleAxe.class, Flail.class, RunicBlade.class, AssassinsBlade.class, Crossbow.class, Katana.class},
			//5阶
			{Greatsword.class, WarHammer.class, Glaive.class, Greataxe.class, Greatshield.class, Gauntlet.class, WarScythe.class}
	};

	/**
	 * 内置伪武器的形态数据（0 阶与 6 阶，不创建独立武器类）。
	 * 伤害公式：min = baseMin + lvl，max = baseMax + maxPerLvl * lvl。
	 */
	private static class FormSpec {
		int tier;
		String name;
		int baseMin;
		int baseMax;
		int maxPerLvl;
		float dly = 1f;
		float acc = 1f;
		int rch = 1;
		int def = 0;

		FormSpec(int tier, String name, int baseMin, int baseMax, int maxPerLvl, float dly){
			this.tier = tier;
			this.name = name;
			this.baseMin = baseMin;
			this.baseMax = baseMax;
			this.maxPerLvl = maxPerLvl;
			this.dly = dly;
		}
	}

	//0阶伪武器：叉勺（min = 0+lvl，max = 5+lvl，DLY 0.5）
	private static final FormSpec TIER0_FORKSPOON = new FormSpec(0, "叉勺", 0, 5, 1, 0.5f);
	//6阶伪武器：镰刀（min = 6+lvl，max = 45+lvl×7）
	private static final FormSpec TIER6_SCYTHE    = new FormSpec(6, "镰刀", 6, 45, 7, 1f);

	//当前（最近一次）随机到的形态：要么是伪武器（formSpec），要么是原版武器（formClass/formInstance）
	private FormSpec formSpec;
	private Class<? extends MeleeWeapon> formClass;
	private MeleeWeapon formInstance;

	//Furioso-Replica 第9次攻击强制镰刀形态的标记（一次性，下次 rollForm 时消耗）
	private boolean forceScythe = false;

	{
		image = ItemSpriteSheet.HERMES_CADUCEUS;
		//命中音不走 hitSound 字段，而是覆写 hitSound(float) 随机三选一（见下）
		hitSoundPitch = 1f;

		tier = 5; //面板等阶5；攻击数值由随机形态决定

		//专属武器：不进英雄遗骸（EquipableItem 默认 bones = true，这里必须显式关掉）
		bones = false;
	}

	//========== 攻击音效 ==========

	/** 技能1 的三条攻击音，每次命中随机播一条。 */
	private static final String[] HIT_SOUNDS = {
			Assets.Sounds.INDEXFATHER_SKILL1_1,
			Assets.Sounds.INDEXFATHER_SKILL1_2,
			Assets.Sounds.INDEXFATHER_SKILL1_3
	};

	/**
	 * 覆写命中音，每次命中从 {@link #HIT_SOUNDS} 里随机播一条。
	 * <p>为什么不只是给 {@code hitSound} 字段赋值：</p>
	 * <ol>
	 *   <li>字段只能填**一条固定音**，拿不到「每次随机」；</li>
	 *   <li>引擎是在 {@code belongings.attackingWeapon()} 上**多态**调用本方法的，
	 *       而本武器会变形 ⇒ 有些路径下 {@code attackingWeapon()} 根本不是本武器
	 *       （最典型的是武技窗口，见 {@link #useAbility}），那时改字段等于没改。</li>
	 * </ol>
	 * <p>普通近战攻击走这条路：{@code Hero.hitSound} → {@code attackingWeapon().hitSound()}。
	 * 本武器在普通攻击中**始终**是 {@code attackingWeapon()} —— 随机出的形态武器只被用来
	 * 算伤害/命中/攻速/射程（{@code formInstance} 是私有字段，从不进 thrownWeapon /
	 * abilityWeapon）⇒ 覆写本方法即可稳定生效。</p>
	 */
	@Override
	public void hitSound(float pitch) {
		Sample.INSTANCE.play(Random.element(HIT_SOUNDS), 1, pitch * hitSoundPitch);
	}

	//本体力量需求按 1 阶（10）计算，避免前期力量不足
	@Override
	public int STRReq(int lvl) {
		int req = STRReq(1, lvl);
		if (masteryPotionBonus){
			req -= 2;
		}
		return req;
	}

	//========== 随机形态机制 ==========

	/**
	 * 根据「业」（{@link Karma}）的层数随机一个阶位（0~6）。
	 * <p>采用指数权重（softmax 族）：各阶权重 w_k = x^k（k=0..6），x = 10^(1 - e/50)。
	 * <ul>
	 *   <li>业 e=0 → x=10 → 高阶概率 ≈ 90%；</li>
	 *   <li>业 e=100 → x=0.1 → 0 阶概率 ≈ 90%；</li>
	 *   <li>业 e=50 → x=1 → 各阶均匀。</li>
	 * </ul>
	 * 业越低越容易 roll 到高阶，业越高越容易 roll 到低阶。</p>
	 * <p>同时根据当前力量与武器强化等级，**排除力量不足的阶位**（避免力量惩罚
	 * 降低命中/攻速而阻碍战斗）；在可用阶位内按上述业加权随机。</p>
	 */
	private int rollTier(){
		int karma = 0;
		if (Dungeon.hero != null){
			Karma k = Dungeon.hero.buff(Karma.class);
			if (k != null) karma = k.karma();
		}

		//排除力量不足的阶位：力量需求 = STRReq(阶位, 强化等级) - 力量药水减需
		int str = Dungeon.hero != null ? Dungeon.hero.STR() : 8;
		int lvl = buffedLvl();
		boolean[] usable = new boolean[7];
		for (int i = 0; i < 7; i++){
			int req = STRReq(i, lvl) - (masteryPotionBonus ? 2 : 0);
			usable[i] = req <= str;
		}

		double x = Math.pow(10, 1 - karma / 50.0);
		float[] weights = new float[7]; //0~6 阶
		double w = 1; //w_k = x^k
		for (int i = 0; i < weights.length; i++){
			weights[i] = usable[i] ? (float) w : 0f;
			w *= x;
		}
		int tier = Random.chances(weights);
		//0 阶需求恒 ≤8，正常情况下始终可用；兜底避免 -1
		return tier < 0 ? 0 : tier;
	}

	/**
	 * 攻击前随机选取本次攻击的武器形态：先随机阶位，再在同一阶内随机。
	 * 0/6 阶使用内置伪武器（{@link FormSpec}）；1~5 阶反射创建原版武器模板实例，
	 * 并继承本武器的强化等级，从而用其自身阶位计算伤害。
	 */
	private void rollForm(){
		if (forceScythe){
			forceScythe = false; //一次性
			formSpec = TIER6_SCYTHE;
			formClass = null;
			formInstance = null;
			return;
		}
		int tier = rollTier();

		if (tier == 0){
			formSpec = TIER0_FORKSPOON;
			formClass = null;
			formInstance = null;
		} else if (tier == 6){
			formSpec = TIER6_SCYTHE;
			formClass = null;
			formInstance = null;
		} else {
			formSpec = null;
			Class<? extends MeleeWeapon> cls = Random.element(TIER_WEAPONS[tier-1]);
			if (cls != formClass){
				formClass = cls;
				formInstance = Reflection.newInstance(cls);
			}
			if (formInstance != null){
				//让模板武器使用本武器的强化等级计算伤害（min/max 公式内部使用模板自身的 tier）
				formInstance.level(buffedLvl());
				//同步力量药水减需，保证力量需求计算一致
				formInstance.masteryPotionBonus = masteryPotionBonus;
			}
		}
	}

	/** 返回当前随机形态的模板武器实例；若为伪武器或创建失败返回 null。 */
	private MeleeWeapon form(){
		return formInstance;
	}

	//Furioso-Replica 专用：攻击前手动重新随机形态（该技能的伤害计算不经过 accuracyFactor，故需要显式调用）
	public void rollFormForAttack(){
		rollForm();
	}

	//Furioso-Replica 专用：强制下一次攻击以镰刀（6阶伪武器）形态进行
	public void forceScytheForm(){
		forceScythe = true;
		formSpec = TIER6_SCYTHE;
		formClass = null;
		formInstance = null;
	}

	//攻击流程中首个被调用的武器方法：每次攻击尝试前重新随机形态
	@Override
	public int reachFactor(Char owner) {
		rollForm();
		if (formSpec != null) return formSpec.rch;
		MeleeWeapon f = form();
		return f != null ? f.reachFactor(owner) : super.reachFactor(owner);
	}

	@Override
	public float accuracyFactor(Char owner, Char target) {
		//每次攻击（命中判定）前都重新随机形态。
		//注意：攻击相邻目标时 Hero.canAttack 会短路直接返回（不调用 canReach/reachFactor），
		//因此不能依赖 reachFactor 作为随机时机；accuracyFactor 是每次攻击必经的调用点。
		rollForm();
		if (formSpec != null) return formSpec.acc;
		MeleeWeapon f = form();
		return f != null ? f.accuracyFactor(owner, target) : super.accuracyFactor(owner, target);
	}

	@Override
	public int defenseFactor(Char owner) {
		if (formSpec != null) return formSpec.def;
		MeleeWeapon f = form();
		return f != null ? f.defenseFactor(owner) : super.defenseFactor(owner);
	}

	@Override
	public int damageRoll(Char owner) {
		if (formSpec != null){
			return formSpecDamageRoll(owner);
		}
		MeleeWeapon f = form();
		if (f == null) return super.damageRoll(owner);

		//在角色头顶弹出本次攻击随机到的武器名称（类似伤害跳字）
		if (owner instanceof Hero && owner.sprite != null){
			owner.sprite.showStatus(CharSprite.WARNING, Messages.get(formClass, "name"));
		}

		//模板武器沿用本武器的强化（augment）设置
		Augment prev = f.augment;
		f.augment = this.augment;
		int dmg = f.damageRoll(owner);
		f.augment = prev;
		return dmg;
	}

	//伪武器形态的伤害计算（与 MeleeWeapon.damageRoll 相同的结构）
	private int formSpecDamageRoll(Char owner){
		//在角色头顶弹出本次攻击随机到的武器名称（类似伤害跳字）
		if (owner instanceof Hero && owner.sprite != null){
			owner.sprite.showStatus(CharSprite.WARNING, formSpec.name);
		}

		int min = formSpec.baseMin + buffedLvl();
		int max = formSpec.baseMax + formSpec.maxPerLvl * buffedLvl();

		int damage = augment.damageFactor(Hero.heroDamageIntRange(min, max));

		if (owner instanceof Hero){
			int exStr = ((Hero)owner).STR() - STRReq(formSpec.tier, buffedLvl());
			if (exStr > 0){
				damage += Hero.heroDamageIntRange(0, exStr);
			}
		}
		return damage;
	}

	@Override
	public float delayFactor(Char owner) {
		if (formSpec != null){
			return formSpecDelay(owner);
		}
		MeleeWeapon f = form();
		return f != null ? f.delayFactor(owner) : super.delayFactor(owner);
	}

	//伪武器形态的攻击间隔（与 Weapon.baseDelay/speedMultiplier 相同的结构）
	private float formSpecDelay(Char owner){
		float delay = augment.delayFactor(formSpec.dly);
		if (owner instanceof Hero){
			int encumbrance = STRReq(formSpec.tier, buffedLvl()) - ((Hero)owner).STR();
			if (encumbrance > 0){
				delay *= Math.pow(1.2, encumbrance);
			}
		}
		return delay * (1f / speedMultiplier(owner));
	}

	//「业」buff 已改为绑定神谕代行者角色（开局获得），与是否装备本武器无关

	@Override
	public void activate(Char ch) {
		super.activate(ch);
		//神谕代行者：武技充能系统（仿造的父辈）
		if (ch instanceof Hero && ((Hero)ch).heroClass == HeroClass.ORACLE){
			Buff.affect(ch, Charger.class);
		}
	}

	@Override
	public boolean doUnequip(Hero hero, boolean collect, boolean single) {
		return super.doUnequip(hero, collect, single);
	}

	//========== 神谕代行者武技（仿造的父辈） ==========

	//武技按钮：神谕代行者装备双蛇杖且点了仿造的父辈天赋时可用
	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (hero.heroClass == HeroClass.ORACLE && isEquipped(hero)
				&& hero.hasTalent(Talent.FALSE_FATHER)){
			actions.add(AC_ABILITY);
		}
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_ABILITY) && hero.heroClass == HeroClass.ORACLE){
			return "武技";
		}
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_ABILITY) && hero.heroClass == HeroClass.ORACLE){
			useAbility(hero);
		}
	}

	//随机使用一种武器的武技（随机逻辑与攻击相同）
	private void useAbility(Hero hero){
		if (!isEquipped(hero)){
			GLog.w("需要装备赫尔墨斯的双蛇杖");
			return;
		}
		rollForm(); //先随机阶位，再随机武器
		Charger charger = Buff.affect(hero, Charger.class);
		if (charger.chargeCap() == 0){
			GLog.w("武技尚未解锁");
			return;
		}
		if (formSpec != null){
			//伪武器武技：叉勺 / 镰刀
			if (formSpec == TIER0_FORKSPOON){
				useForkSpoonAbility(hero, charger);
			} else {
				useScytheAbility(hero, charger);
			}
		} else if (formInstance != null){
			//原版武器的武技（同包 protected 可访问）
			if (charger.charges + charger.partialCharge < formInstance.abilityChargeUse(hero, null)){
				GLog.w("武技充能不足");
				return;
			}
			//把本武器的攻击音同步给形态武器。
			//原因：MeleeWeapon.beforeAbilityUsed 会把 formInstance 塞进
			//belongings.abilityWeapon ⇒ 武技窗口内 attackingWeapon() 返回的是它，
			//它自己的命中音会被播放，本武器的 HIT_SOUNDS 被整体绕过。
			//（同步后这段时间听到的仍是本武器的音效；形态实例是复用缓存对象，
			// 留着这个音也不影响普通攻击——那时 attackingWeapon() 是本武器。）
			formInstance.setHitSound(Random.element(HIT_SOUNDS));
			if (formInstance.targetingPrompt() == null){
				formInstance.duelistAbility(hero, hero.pos);
			} else {
				final MeleeWeapon wep = formInstance;
				GameScene.selectCell(new CellSelector.Listener() {
					@Override
					public void onSelect(Integer cell) {
						if (cell != null) wep.duelistAbility(hero, cell);
					}
					@Override
					public String prompt() {
						return wep.targetingPrompt();
					}
				});
			}
		}
	}

	//叉勺武技：1 充能，1 回合，恢复 50 饱食度（降低饥饿）并恢复 5 点血量
	private void useForkSpoonAbility(Hero hero, Charger charger){
		if (charger.charges + charger.partialCharge < 1){
			GLog.w("武技充能不足");
			return;
		}
		charger.partialCharge -= 1;
		while (charger.partialCharge < 0 && charger.charges > 0){
			charger.charges--;
			charger.partialCharge++;
		}
		Hunger hunger = hero.buff(Hunger.class);
		if (hunger != null) hunger.satisfy(50); //饱食度随时间增加，恢复=降低饥饿值
		if (hero.heal( 5 ) > 0){
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, "+5", FloatingText.HEALING);
		}
		hero.spendAndNext(1f);
	}

	//镰刀武技：3 充能，1 回合，选择目标，瞬间移动到目标周围并造成一次无视防御的 300% 倍率伤害
	private void useScytheAbility(Hero hero, Charger charger){
		if (charger.charges + charger.partialCharge < 3){
			GLog.w("武技充能不足");
			return;
		}
		final Charger c = charger;
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect(Integer cell) {
				if (cell == null) return;
				final Char enemy = Actor.findChar(cell);
				if (enemy == null || enemy == hero || !Dungeon.level.heroFOV[cell] || hero.isCharmedBy(enemy)){
					GLog.w("无效的目标");
					return;
				}
				//消耗 3 充能
				c.partialCharge -= 3;
				while (c.partialCharge < 0 && c.charges > 0){
					c.charges--;
					c.partialCharge++;
				}
				//瞬间移动到目标周围
				int dest = findAdjacentCell(hero, enemy);
				if (dest != -1){
					hero.pos = dest;
					Dungeon.level.occupyCell(hero);
					hero.sprite.place(dest);
				}
				//无视防御的 300% 倍率伤害（Char.damage 不结算护甲）
				int dmg = Math.round(hero.damageRoll() * 3f);
				enemy.damage(dmg, HermesCaduceus.this);
				enemy.sprite.bloodBurstA(hero.sprite.center(), dmg);
				enemy.sprite.flash();
				hero.spendAndNext(1f);
			}
			@Override
			public String prompt() {
				return "选择镰刀武技的目标";
			}
		});
	}

	//目标周围可行走的相邻格
	private int findAdjacentCell(Hero hero, Char enemy){
		int best = -1;
		for (int i : PathFinder.NEIGHBOURS8){
			int c = enemy.pos + i;
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null){
				if (best == -1 || Dungeon.level.trueDistance(c, enemy.pos) < Dungeon.level.trueDistance(best, enemy.pos)){
					best = c;
				}
			}
		}
		return best;
	}

	//========== 文本 ==========

	//攻击数值随随机形态变化，不显示固定的武器统计/技能
	@Override
	public String statsInfo() {
		return "";
	}

	@Override
	public String abilityInfo() {
		return "";
	}

	@Override
	public String name() {
		return "赫尔墨斯的双蛇杖";
	}

	@Override
	public String desc() {
		return "从两端流溢出黑色液体金属的短杖。根据指令之意，液体会变化为不同武器的形状。攻击时，会随机获得不同武器的性质。";
	}
}
