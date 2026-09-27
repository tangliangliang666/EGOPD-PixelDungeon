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

package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

/**
 * 血宴圣杯：原版「蓄血圣杯」的强化形态（2026-09-20）。
 *
 * <h3>新增效果</h3>
 * <ol>
 *   <li><b>记录血宴</b>：装备期间，英雄与敌人受到的<b>每一跳流血伤害</b>都累积成「血宴」。
 *       取点在 {@code Bleeding.act()}（流血伤害的唯一结算处，见 {@link #onBleedTick}）。</li>
 *   <li><b>血宴抵挡升级伤害</b>：血祭时可以拿血宴按 1:1 抵消这次的升级伤害，
 *       抵消完还有富余则<b>整次升级零伤害</b>（见 {@link #mitigatePrickDamage}）。</li>
 *   <li><b>与「渴望」联动</b>：同时装备武器「渴望」时，渴望把流血转化出的护盾在衰减时
 *       按 1/5 的比例回血（实现在 {@code actors/buffs/ThirstBloodBarrier}）。</li>
 * </ol>
 *
 * <h3>两个钩子的位置为什么关键</h3>
 * <ul>
 *   <li>{@link #extraPrickHP} 喂给的是**死亡概率提示窗口**。那是「生死确认」窗口，
 *       不把血宴的抵挡量算进去就是在骗玩家 —— 明明不会死，提示却写着必死。</li>
 *   <li>{@link #mitigatePrickDamage} 必须在原版「至少造成 1 点伤害」的保底**之后**调用
 *       （父类已经这样接好了），否则保底会把减免结果重新抬回 1 点，永远做不到零伤升级。</li>
 * </ul>
 *
 * <p>文本键 {@code items.artifacts.bloodfeastchalice.*}；原版那批功能文本
 * （{@code desc_1/2/3}、{@code prick_warn}、{@code onprick}…）沿 {@code Messages.get}
 * 的父类链自动继承，无需复制。</p>
 */
public class BloodFeastChalice extends ChaliceOfBlood {

	{
		image = ItemSpriteSheet.ARTIFACT_BLOOD_FEAST_CHALICE1;
	}

	/** 已积攒的血宴。 */
	protected int bloodFeast = 0;

	//==========================================================================
	// 血宴的累积
	//==========================================================================

	/**
	 * 装备在身上的血宴圣杯（artifact 槽或 misc 槽；只认<b>已装备</b>，背包里的不算）。
	 */
	public static BloodFeastChalice equipped(Hero hero){
		if (hero == null || hero.belongings == null) return null;
		if (hero.belongings.artifact instanceof BloodFeastChalice) {
			return (BloodFeastChalice) hero.belongings.artifact;
		}
		//第二神器栏：artifact 槽被占时神器会落进 misc 槽
		if (hero.belongings.misc instanceof BloodFeastChalice) {
			return (BloodFeastChalice) hero.belongings.misc;
		}
		return null;
	}

	/** 「渴望护盾衰减回血」的开关判据，供 {@code ThirstBloodBarrier} 调用。 */
	public static boolean linkActive(Char ch){
		return ch instanceof Hero && equipped((Hero) ch) != null;
	}

	/**
	 * 记录一跳流血伤害（{@code Bleeding.act()} 的唯一调用点）。
	 *
	 * <p>只计「自身与敌人」：友方 / 中立单位的流血不算（需求原文即「自身和敌人」）。
	 * 英雄未装备血宴圣杯时整段空转。</p>
	 *
	 * @param victim 这一跳流血打在谁身上
	 * @param amount 这一跳的流血伤害
	 */
	public static void onBleedTick(Char victim, int amount){
		if (amount <= 0 || victim == null || Dungeon.hero == null) return;
		if (victim != Dungeon.hero && victim.alignment != Char.Alignment.ENEMY) return;

		BloodFeastChalice chalice = equipped(Dungeon.hero);
		if (chalice != null) chalice.gainBloodFeast(amount);
	}

	protected void gainBloodFeast(int amount){
		if (amount <= 0) return;
		bloodFeast += amount;
	}

	//==========================================================================
	// 血宴的消耗
	//==========================================================================

	/** 死亡概率提示里把血宴算作等效血量（最多只需抵掉一次最大伤害，多余的不影响概率）。 */
	@Override
	protected int extraPrickHP(Hero hero){
		return Math.min(bloodFeast, maxPrickDmg());
	}

	/**
	 * 拿血宴按 1:1 抵消这次升级伤害。父类在本方法返回后才扣血，所以返回 0 即「零伤升级」。
	 *
	 * <p><b>这里不要自己打提示</b>：父类 {@code prick()} 已经在钩子返回后按
	 * 「减免了多少」统一播报（键 {@code prick_mitigated}），在这里再打一次会弹两条一模一样的消息。
	 */
	@Override
	protected int mitigatePrickDamage(Hero hero, int damage){
		if (damage <= 0 || bloodFeast <= 0) return damage;

		int used = Math.min(bloodFeast, damage);
		bloodFeast -= used;

		return damage - used;
	}

	//==========================================================================
	// 贴图 / 描述
	//==========================================================================

	/** 换成本形态的三帧（帧的时刻语义与父类完全一致，见 {@code ChaliceOfBlood.updateImage}）。 */
	@Override
	protected void updateImage(int lvl){
		if (lvl >= 7)      image = ItemSpriteSheet.ARTIFACT_BLOOD_FEAST_CHALICE3;
		else if (lvl >= 3) image = ItemSpriteSheet.ARTIFACT_BLOOD_FEAST_CHALICE2;
	}

	@Override
	public String desc() {
		//父类会先取本类的 desc（金色圣杯那句），装备时再追加原来那三段 desc_1/2/3 —— 都保留
		return super.desc() + "\n\n" + Messages.get(this, "desc_enhanced", bloodFeast);
	}

	//==========================================================================
	// 存档
	//==========================================================================

	private static final String BLOOD_FEAST = "blood_feast";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put( BLOOD_FEAST, bloodFeast );
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		//父类的 restoreFromBundle 会调 updateImage(level())，虚分派到本类的实现，顺序无碍
		super.restoreFromBundle(bundle);
		//旧档没有这个键时返回 0（防御：强化形态是本次新增的类，不排除有人把普通圣杯改档过来）
		bloodFeast = bundle.getInt( BLOOD_FEAST );
	}

	//==========================================================================
	// 炼金合成：蓄血圣杯 + 30 脑啡肽，12 能量
	//==========================================================================

	public static class CraftRecipe extends ArtifactEnhanceRecipe<ChaliceOfBlood, BloodFeastChalice> {

		@Override
		protected Class<ChaliceOfBlood> acceptedArtifact(){
			return ChaliceOfBlood.class;
		}

		@Override
		protected BloodFeastChalice createEnhanced(){
			return new BloodFeastChalice();
		}

		@Override
		protected void transferState(ChaliceOfBlood source, BloodFeastChalice enhanced){
			//artifact 的 charge/partialCharge/chargeCap/exp/cooldown 都是 protected，
			//而本类与 ChaliceOfBlood 同包同继承链 ⇒ 可以直接读写，不需要给原版加 getter
			enhanced.level( source.level() );
			enhanced.exp = source.exp;
			enhanced.chargeCap = source.chargeCap;
			enhanced.charge = source.charge;
			enhanced.partialCharge = source.partialCharge;
			enhanced.cooldown = source.cooldown;

			enhanced.cursed = source.cursed;
			enhanced.cursedKnown = source.cursedKnown;
			enhanced.levelKnown = source.levelKnown;

			enhanced.updateImage( enhanced.level() );
		}
	}
}
