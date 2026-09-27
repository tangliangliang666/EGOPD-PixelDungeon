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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.OdinsEyeOverheat;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.valencina.ValencinaSfx;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * 神器——奥丁之眼（自定义，2026-09-06）
 *
 * <p>基础框架完全套用盗贼的 {@link CloakOfShadows}（暗影斗篷）：升级=战斗中攒 exp 升级
 * （级上限 10，每次升级充能上限 +1）；充能模式=装备时每 45 回合回 1 点充能，
 * 充能上限 = min(level+3, 10)。</p>
 *
 * <p>主动技能：获得「预知眼」buff。预知眼持续期间获得无限闪避（命中判定直接判负），
 * 且每次闪避成功时角色头顶弹出嘲讽文本。每次激活（手动或自动）消耗 1 点充能，
 * 预知眼固定持续 4 回合（期间不再逐回合扣费），到期后自行消失。若在充能恰为 0
 * （用掉了最后一格充能）时让它自然消失且未手动关闭，将获得 30 回合
 * 「预知眼-过热」debuff + 最大生命 5% 的流血。</p>
 *
 * <p>在即将受到攻击前（命中判定发生处 {@code Char.hit}），若充能 &gt; 0 且当前未过热、
 * 未激活，会自动激活预知眼（等同自动使用，同样消耗 1 点充能）。</p>
 *
 * <p>贴图：常态 xy(3, 34)；「预知眼-过热」debuff 活跃期间显示过热形态 xy(4, 34)。</p>
 */
public class OdinsEye extends Artifact {

	{
		image = ItemSpriteSheet.ARTIFACT_ODINS_EYE;

		exp = 0;
		levelCap = 10;

		charge = Math.min(level()+3, 10);
		partialCharge = 0;
		chargeCap = Math.min(level()+3, 10);

		defaultAction = AC_PRECOG;

		unique = true;
		bones = false;
	}

	public static final String AC_PRECOG = "PRECOG";

	//自动模式开关（2026-09-08）：开启时保持原有的“即将受击前自动激活”行为；关闭后必须手动使用「预知」
	public static final String AC_AUTO   = "AUTO";
	public boolean autoMode = true;

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		if (isEquipped( hero )
				&& !cursed
				&& hero.buff(MagicImmune.class) == null
				&& hero.buff(OdinsEyeOverheat.class) == null        //过热期间不可使用
				&& (charge > 0 || activeBuff != null)) {
			actions.add(AC_PRECOG);
		}
		if (isEquipped( hero ) && !cursed && hero.buff(MagicImmune.class) == null){
			actions.add(AC_AUTO);
		}
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {

		super.execute(hero, action);

		if (hero.buff(MagicImmune.class) != null) return;

		if (action.equals( AC_AUTO )) {
			autoMode = !autoMode;
			GLog.i( Messages.get(this, autoMode ? "auto_on" : "auto_off") );
			Item.updateQuickslot();
			return;
		}

		if (action.equals( AC_PRECOG )) {

			if (activeBuff == null){
				if (!isEquipped(hero))                       GLog.i( Messages.get(Artifact.class, "need_to_equip") );
				else if (cursed)                             GLog.i( Messages.get(this, "cursed") );
				else if (hero.buff(OdinsEyeOverheat.class) != null) GLog.i( Messages.get(this, "overheated") );
				else if (charge <= 0)                        GLog.i( Messages.get(this, "no_charge") );
				else {
					hero.spend( 1f );
					hero.busy();
					Sample.INSTANCE.play(Assets.Sounds.MELD);
					startPrecognition(hero);
					hero.sprite.operate(hero.pos);
				}
			} else {
				//手动关闭预知眼 → 无过热惩罚
				((precognition) activeBuff).dismiss();
				hero.sprite.operate( hero.pos );
			}

		}
	}

	@Override
	public void activate(Char ch){
		super.activate(ch);
		if (activeBuff != null && activeBuff.target == null){
			activeBuff.attachTo(ch);
		}
	}

	@Override
	public boolean doUnequip(Hero hero, boolean collect, boolean single) {
		if (super.doUnequip(hero, collect, single)){
			//卸下即视为手动关闭预知眼（无过热惩罚）
			if (activeBuff != null){
				((precognition) activeBuff).dismiss();
			}
			return true;
		} else
			return false;
	}

	@Override
	protected void onDetach() {
		if (passiveBuff != null){
			passiveBuff.detach();
			passiveBuff = null;
		}
		if (activeBuff != null && !isEquipped((Hero) activeBuff.target)){
			((precognition) activeBuff).dismiss();
		}
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new eyeRecharge();
	}

	@Override
	protected ArtifactBuff activeBuff( ) {
		return new precognition();
	}

	/**
	 * 激活预知眼（手动与自动共用）。消耗 1 点充能、获得使用经验；
	 * 不 spend 回合、不 busy —— 自动触发时在战斗判定前静默生效。
	 */
	private void startPrecognition(Hero hero){
		//激活消耗 1 点充能（调用方已保证 charge > 0）
		charge = Math.max(0, charge - 1);

		//战斗中使用获得经验（对标暗影斗篷：每消耗一次充能即获得一次经验）
		int lvlDiffFromTarget = hero.lvl - (1+level()*2);
		if (level() >= 7){
			lvlDiffFromTarget -= level()-6;
		}
		if (lvlDiffFromTarget >= 0){
			exp += Math.round(10f * Math.pow(1.1f, lvlDiffFromTarget));
		} else {
			exp += Math.round(10f * Math.pow(0.75f, -lvlDiffFromTarget));
		}

		if (exp >= (level() + 1) * 50 && level() < levelCap) {
			upgrade();
			exp -= level() * 50;
			GLog.p(Messages.get(this, "levelup"));
		}

		updateQuickslot();
		activeBuff = activeBuff();
		activeBuff.attachTo(hero);

		//家族之耻分支：预知眼每次激活（手动或自动）获得 1 点剑术充能
		Talent.onPrecognitionActivated( hero );
	}

	/**
	 * 即将受到攻击前自动激活（由 {@code Char.hit} 在命中判定前调用）。
	 * @return true 表示本次因自动激活而开启了预知眼
	 */
	public static boolean autoUse( Hero defender ){
		if (defender == null || defender != Dungeon.hero) return false;
		Item art = defender.belongings.artifact;
		if (!(art instanceof OdinsEye)) art = defender.belongings.misc;
		if (!(art instanceof OdinsEye)) return false;

		OdinsEye eye = (OdinsEye) art;
		if (eye.cursed
				|| !eye.autoMode          //自动模式关闭时不再自动响应（2026-09-08）
				|| eye.charge <= 0
				|| eye.activeBuff != null
				|| defender.buff(MagicImmune.class) != null
				|| defender.buff(OdinsEyeOverheat.class) != null){
			return false;
		}

		eye.startPrecognition(defender);
		return true;
	}

	/** 过热形态显示切换：奥丁之眼在「预知眼-过热」debuff 活跃期间显示过热贴图 */
	private void updateImage(){
		boolean hot = Dungeon.hero != null && Dungeon.hero.buff(OdinsEyeOverheat.class) != null;
		image = hot ? ItemSpriteSheet.ARTIFACT_ODINS_EYE_OVERHEAT : ItemSpriteSheet.ARTIFACT_ODINS_EYE;
		Item.updateQuickslot();
	}

	/** 由 {@link OdinsEyeOverheat} attach/detach 时回调，刷新佩戴者奥丁之眼的显示形态 */
	public static void refreshImageFor(Char c){
		if (!(c instanceof Hero)) return;
		Hero hero = (Hero) c;
		Item i = hero.belongings.artifact;
		if (!(i instanceof OdinsEye)) i = hero.belongings.misc;
		if (i instanceof OdinsEye){
			((OdinsEye) i).updateImage();
		}
	}

	@Override
	public void charge(Hero target, float amount) {
		if (cursed || target.buff(MagicImmune.class) != null) return;

		if (charge < chargeCap) {
			partialCharge += 0.25f*amount;
			while (partialCharge >= 1f) {
				charge++;
				partialCharge--;
			}
			if (charge >= chargeCap){
				partialCharge = 0;
				charge = chargeCap;
			}
			updateQuickslot();
		}
	}

	/**
	 * 「狩猎一餐」（拇指 前二老板 T2）的充能恢复入口：+1 恢复 _1_ 点、+2 恢复 _1.25_ 点。
	 *
	 * <p>与 {@link #charge(Hero, float)} 的区别：那个是「充能道具通用钩子」（0.25 折算率），
	 * 这里按天赋的字面数值直接加点，语义同剑术充能的 {@code Charger.gainCharge}。</p>
	 *
	 * <p>只在奥丁之眼<b>已装备</b>（神器槽或杂项槽）时生效，否则静默跳过 ——
	 * 对照「记录一餐」在没装备复仇账簿时静默跳过的做法。</p>
	 */
	public static void restoreCharge( Hero hero, float amount ){
		if (hero == null || amount <= 0f) return;
		if (hero.buff(MagicImmune.class) != null) return;

		Item equipped = hero.belongings.artifact;
		if (!(equipped instanceof OdinsEye)) equipped = hero.belongings.misc;
		if (!(equipped instanceof OdinsEye)) return;

		OdinsEye eye = (OdinsEye) equipped;
		if (eye.cursed || eye.charge >= eye.chargeCap) return;

		eye.partialCharge += amount;
		while (eye.partialCharge >= 1f && eye.charge < eye.chargeCap){
			eye.charge++;
			eye.partialCharge--;
		}
		if (eye.charge >= eye.chargeCap){
			eye.charge = eye.chargeCap;
			eye.partialCharge = 0;
		}
		Item.updateQuickslot();
	}

	@Override
	public Item upgrade() {
		chargeCap = Math.min(chargeCap + 1, 10);
		return super.upgrade();
	}

	private static final String BUFF = "buff";
	private static final String AUTO_MODE = "auto_mode";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		if (activeBuff != null) bundle.put(BUFF, activeBuff);
		bundle.put( AUTO_MODE, autoMode );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		if (bundle.contains(BUFF)){
			activeBuff = new precognition();
			activeBuff.restoreFromBundle(bundle.getBundle(BUFF));
		}
		//旧存档无该键 → 默认保持开启（与原先的自动行为一致）
		autoMode = !bundle.contains(AUTO_MODE) || bundle.getBoolean(AUTO_MODE);
	}

	@Override
	public int value() {
		return 0;
	}

	/** 充能恢复（装备时每回合积累，~45 回合 / 点，随等级加快）——完全对标暗影斗篷 */
	public class eyeRecharge extends ArtifactBuff{
		@Override
		public boolean act() {
			if (charge < chargeCap && !cursed && target.buff(MagicImmune.class) == null) {
				if (activeBuff == null && Regeneration.regenOn()) {
					float missing = (chargeCap - charge);
					if (level() > 7) missing += 5*(level() - 7)/3f;
					float turnsToCharge = (45 - missing);
					turnsToCharge /= RingOfEnergy.artifactChargeMultiplier(target);
					float chargeToGain = (1f / turnsToCharge);
					partialCharge += chargeToGain;
				}

				while (partialCharge >= 1) {
					charge++;
					partialCharge -= 1;
					if (charge == chargeCap){
						partialCharge = 0;
					}
				}
			} else {
				partialCharge = 0;
			}

			if (cooldown > 0)
				cooldown --;

			updateQuickslot();

			spend( TICK );

			return true;
		}

	}

	/**
	 * 预知眼（奥丁之眼激活时的效果 buff）——持有者无限闪避。
	 * 固定持续 {@link #DURATION} 回合后自行消失，期间不再消耗充能（激活时已一次性扣费）。
	 * 若回合到时未被手动关闭、且奥丁之眼充能恰为 0（用掉了最后一格充能），将触发过热惩罚。
	 */
	public class precognition extends ArtifactBuff{

		//固定持续回合数
		private static final int DURATION = 4;

		{
			type = buffType.POSITIVE;
		}

		//剩余回合数
		int turnsLeft = DURATION;

		@Override
		public int icon() {
			return BuffIndicator.PRECOGNITION;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.brightness(0.7f);
		}

		@Override
		public float iconFadePercent() {
			return (DURATION - turnsLeft) / (float) DURATION;
		}

		@Override
		public String iconTextDisplay() {
			//右下角显示预知眼剩余回合数（而非剩余充能）
			return Integer.toString(Math.max(turnsLeft, 0));
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", Math.max(turnsLeft, 0));
		}

		@Override
		public boolean act(){
			turnsLeft--;

			if (turnsLeft <= 0){
				//回合到时、未被手动关闭：仅在充能恰为 0 时触发过热惩罚
				if (charge <= 0){
					overheatPenalty();
				}
				if (target == Dungeon.hero) BuffIndicator.refreshHero();
				detach();
			} else {
				//刷新 buff 图标右下角的剩余回合数
				if (target == Dungeon.hero) BuffIndicator.refreshHero();
			}

			spend( TICK );

			return true;
		}

		/** 回合到时且充能为 0、未被手动关闭时的过热惩罚 */
		private void overheatPenalty(){
			if (target == null || target.buff(OdinsEyeOverheat.class) != null) return;
			//30 回合「预知眼-过热」
			Buff.affect(target, OdinsEyeOverheat.class, 30f);
			//过热抱怨语音：必须挂在授予处（原因见 OdinsEyeOverheat 里的说明），
			//与下面的过热浮字同步响起
			ValencinaSfx.playOverheat();
			//生命值上限 5% 的流血
			Buff.affect(target, Bleeding.class).set(target.HT * 0.05f);
			if (target.sprite != null){
				target.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(this, "overheat_text"));
			}
		}

		/** 手动关闭预知眼（无过热惩罚） */
		public void dismiss(){
			detach();
		}

		@Override
		public void detach() {
			activeBuff = null;

			updateQuickslot();
			super.detach();
		}

		private static final String TURNSLEFT = "turnsLeft";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);

			bundle.put( TURNSLEFT , turnsLeft);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);

			if (bundle.contains(TURNSLEFT)){
				turnsLeft = bundle.getInt( TURNSLEFT );
			} else {
				turnsLeft = DURATION;
			}
		}
	}
}
