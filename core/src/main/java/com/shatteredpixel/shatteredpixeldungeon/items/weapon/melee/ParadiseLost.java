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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blessing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.HeavenStrike;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Enkephalin;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndUseItem;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 失乐园：六阶E.G.O武器（超大贴图21×20）。
 * <p>面板待定。参考老魔杖：作为近战武器的同时拥有法杖充能、可以施法
 * （施法暂时套用魔弹法杖，不包含灌注法杖能力）。
 * 装备时取消自然回血；命中时恢复2点生命。</p>
 * <p>不加入生成池，仅可通过炼金合成：30脑啡肽 + 一把五阶武器 + 净化卷轴。</p>
 */
public class ParadiseLost extends MeleeWeapon {

	private Wand wand;

	public static final String AC_ZAP = "ZAP";

	private static final float STAFF_SCALE_FACTOR = 0.75f;

	{
		image = ItemSpriteSheet.PARADISE_LOST;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.1f;

		tier = 6;

		defaultAction = AC_ZAP;
		usesTargeting = true;
	}

	public ParadiseLost() {
		wand = new WandOfMagicMissile();
		wand.identify();
		wand.cursed = false;
		updateWand(false);
	}

	@Override
	public String defaultAction() {
		return AC_ZAP;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (wand != null && wand.curCharges > 0) {
			actions.add( AC_ZAP );
		}
		return actions;
	}

	@Override
	public void activate( Char ch ) {
		super.activate(ch);
		applyWandChargeBuff(ch);
	}

	@Override
	public boolean collect( Bag container ) {
		if (super.collect(container)) {
			if (container.owner != null) {
				applyWandChargeBuff(container.owner);
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onDetach( ) {
		if (wand != null) wand.stopCharging();
	}

	@Override
	public int targetingPos(Hero user, int dst) {
		if (wand != null) {
			return wand.targetingPos(user, dst);
		} else {
			return super.targetingPos(user, dst);
		}
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_ZAP)){

			if (wand == null) {
				GameScene.show(new WndUseItem(null, this));
				return;
			}

			if (cursed || hasCurseEnchant()) wand.cursed = true;
			else                             wand.cursed = false;
			curUser = hero;
			curItem = this;
			GameScene.selectCell(zapper);
		}
	}

	//施法目标选择：允许选择自身/友方/敌方/空地格，按目标类型分流效果
	protected static CellSelector.Listener zapper = new CellSelector.Listener() {

		@Override
		public void onSelect( Integer target ) {

			if (target != null) {

				final ParadiseLost curPL;
				if (curItem instanceof ParadiseLost){
					curPL = (ParadiseLost) curItem;
				} else {
					return;
				}

				if (curUser.buff(MagicImmune.class) != null){
					GLog.w( Messages.get(Wand.class, "no_magic") );
					return;
				}

				if (curPL.wand.curCharges < 1){
					GLog.w( Messages.get(Wand.class, "fizzles") );
					return;
				}

				//对自身施法：不走弹道（参考法师"储能护盾"允许对自身施法的方式），直接施加祝福+护盾
				if (target == curUser.pos){
					curUser.sprite.zap(curUser.pos);
					curPL.onZap( null );
					curPL.wandUsed();
					return;
				}

				final Ballistica shot = new Ballistica( curUser.pos, target, Ballistica.MAGIC_BOLT );
				int cell = shot.collisionPos;

				curUser.sprite.zap(cell);

				curUser.busy();

				//直接结算效果（无弹道发射特效；命中敌人时由目标自身的受击逻辑播放长矛贯穿特效）
				curPL.onZap(shot);
				curPL.wandUsed();
				Sample.INSTANCE.play( Assets.Sounds.ZAP );
			}
		}

		@Override
		public String prompt() {
			return Messages.get(ParadiseLost.class, "zap_prompt");
		}
	};

	//施法效果：敌方/空地 → 范围伤害；友方/自身 → 祝福buff + 护盾/回血
	public void onZap( Ballistica bolt ) {

		int cell;
		Char ch;
		if (bolt == null){
			//对自身施法
			cell = curUser.pos;
			ch = curUser;
		} else {
			cell = bolt.collisionPos;
			ch = Actor.findChar( cell );
		}

		int lvl = wand.buffedLvl();

		//敌方单位或空地格：对目标格及周围8格造成范围伤害（雷电法杖伤害成长 5+lvl ~ 10+5lvl），不伤害自己和友方
		if (ch == null || ch.alignment == Char.Alignment.ENEMY){

			int dmg = Random.NormalIntRange( 5 + lvl, 10 + 5*lvl );

			ArrayList<Char> targets = new ArrayList<>();
			Char t = Actor.findChar( cell );
			if (t != null && t != curUser && t.alignment != Char.Alignment.ALLY){
				targets.add( t );
			}
			for (int i : PathFinder.NEIGHBOURS8){
				if (!Dungeon.level.insideMap( cell + i )) continue;
				t = Actor.findChar( cell + i );
				if (t != null && t != curUser && t.alignment != Char.Alignment.ALLY && !targets.contains( t )){
					targets.add( t );
				}
			}

			for (Char tc : targets){
				//远程施法命中：由命中目标播放"长矛贯穿"特效（先播放再结算伤害，避免目标死亡后精灵不可用）
				HeavenStrike.hit( tc );
				tc.damage( dmg, this );
				tc.sprite.flash();
			}

		//友方单位或自身：祝福buff + 自身护盾/友方回血
		} else {

			//施加祝福buff（13回合）
			Buff.prolong( ch, Blessing.class, Blessing.DURATION );

			//自身：施加护盾（注魂法杖数据：5+等级）
			if (ch == curUser){
				int shield = 5 + lvl;
				Buff.affect( curUser, Barrier.class ).setShield( shield );
				curUser.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING );

			//友方：恢复血量（注魂法杖数据：5%自身最大生命 + 3×等级，溢出转护盾）
			} else {
				int selfDmg = Math.round( curUser.HT * 0.05f );
				int healing = selfDmg + 3*lvl;
				int shielding = (ch.HP + healing) - ch.HT;
				if (shielding > 0){
					healing -= shielding;
					Buff.affect( ch, Barrier.class ).setShield( shielding );
				} else {
					shielding = 0;
				}
				int healed = ch.heal( healing );
				ch.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 2 + lvl/2 );
				if (healed > 0){
					ch.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING );
				}
				if (shielding > 0){
					ch.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(shielding), FloatingText.SHIELDING );
				}
			}

			//白色和金色相间的特效（类型类似净化卷轴的Flare）
			new Flare( 6, 32 ).show( ch.sprite, 2f );
			new Flare( 6, 32 ).color( 0xFFD700, true ).show( ch.sprite, 2f );
			Sample.INSTANCE.play( Assets.Sounds.READ );
		}
	}

	//施法后消耗充能并结束回合（复用魔弹法杖的充能结算逻辑）
	public void wandUsed(){
		if (wand != null){
			wand.wandUsed();
		}
	}

	public void applyWandChargeBuff(Char owner){
		if (wand != null){
			wand.charge(owner, STAFF_SCALE_FACTOR);
		}
	}

	public void updateWand(boolean levelled){
		if (wand != null) {
			int curCharges = wand.curCharges;
			wand.level(level());
			//gives the wand one additional max charge
			wand.maxCharges = Math.min(wand.maxCharges + 1, 10);
			wand.curCharges = Math.min(curCharges + (levelled ? 1 : 0), wand.maxCharges);
			updateQuickslot();
		}
	}

	@Override
	public Item upgrade(boolean enchant) {
		super.upgrade( enchant );
		updateWand(true);
		return this;
	}

	@Override
	public String status() {
		if (wand == null) return super.status();
		else return wand.status();
	}

	//命中时恢复2点生命（长矛贯穿特效由目标自身的受击逻辑 defenseProc 触发，仅对敌方单位）
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		if (attacker.isAlive() && attacker.HP < attacker.HT){
			if (attacker.heal( 2 ) > 0){
				attacker.sprite.showStatusWithIcon( CharSprite.POSITIVE, "2", FloatingText.HEALING );
			}
		}

		return damage;
	}

	@Override
	public String name() {
		return "失乐园";
	}

	private static final String WAND = "wand";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(WAND, wand);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		wand = (Wand) bundle.get(WAND);
		if (wand != null) {
			wand.maxCharges = Math.min(wand.maxCharges + 1, 10);
		}
	}

	//合成配方：30脑啡肽 + 五阶武器 + 净化卷轴
	public static class CraftRecipe extends Recipe {

		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			if (ingredients.size() != 3) return false;
			Enkephalin e = null;
			MeleeWeapon weapon = null;
			Scroll scroll = null;
			for (Item it : ingredients){
				if (it instanceof Enkephalin){
					e = (Enkephalin) it;
				} else if (it instanceof MeleeWeapon && it.isIdentified() && !it.cursed
						&& ((MeleeWeapon) it).tier == 5){
					weapon = (MeleeWeapon) it;
				} else if (it instanceof ScrollOfRemoveCurse && !it.cursed){
					scroll = (Scroll) it;
				} else {
					return false;
				}
			}
			return e != null && weapon != null && scroll != null && e.quantity() >= 30;
		}

		@Override
		public int cost(ArrayList<Item> ingredients) {
			return 0;
		}

		@Override
		public Item brew(ArrayList<Item> ingredients) {
			if (!testIngredients(ingredients)) return null;
			Enkephalin e = null;
			MeleeWeapon weapon = null;
			Item scroll = null;
			for (Item it : ingredients){
				if (it instanceof Enkephalin) e = (Enkephalin) it;
				else if (it instanceof MeleeWeapon) weapon = (MeleeWeapon) it;
				else scroll = it;
			}
			e.quantity(e.quantity() - 30);
			weapon.quantity(0);
			scroll.quantity(0);
			return new ParadiseLost();
		}

		@Override
		public Item sampleOutput(ArrayList<Item> ingredients) {
			return new ParadiseLost();
		}
	}

}
