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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ringmaster;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DeepTrauma;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Masterpiece;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.BloodMaterial;
import com.shatteredpixel.shatteredpixeldungeon.items.BoneMaterial;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.MeatMaterial;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 环指大师的盔甲技能「闭馆」（2026-09-04，环指大师当前唯一盔甲技能）。
 * <p>消耗 {@code baseChargeUse=50}% 盔甲充能。使用后对视野内所有敌人发动一次
 * <b>必中攻击</b>（完整攻击链：命中必中、结算武器 proc 与命中类天赋），
 * 并在身旁召唤一件「杰作」{@link Masterpiece}（固定不动的中立单位，50 回合后自行消失）。</p>
 * <p>对应三个四阶分支天赋：请尽情欣赏吧（目睹杰作恐惧系 debuff，由 Masterpiece 每回合施加）、
 * 诸位散落的血肉（闭馆击杀掉落随机素材）、将化为观众席（闭馆命中施加深度创伤）。</p>
 */
public class ClosingTime extends ArmorAbility {

	{
		baseChargeUse = 50f; //消耗50%盔甲充能
	}

	@Override
	protected void activate(final ClassArmor armor, final Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();
		Invisibility.dispel();

		hero.busy();

		//视野内的存活敌对单位（快照，逐个结算时列表不变）
		final ArrayList<Char> enemies = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (ch != hero && ch.isAlive() && ch.alignment == Char.Alignment.ENEMY
					&& Dungeon.level.heroFOV[ch.pos] && !hero.isCharmedBy(ch)){
				enemies.add(ch);
			}
		}

		if (enemies.isEmpty()){
			finish(armor, hero);
			return;
		}

		//播一次挥击动画，动画回调中依次结算全部必中攻击与击杀/命中效果
		hero.sprite.attack(enemies.get(0).pos, new Callback() {
			@Override
			public void call() {
				for (Char ch : enemies){
					if (!ch.isAlive()) continue;
					int beforeHP = ch.HP;
					boolean hit = hero.attack( ch, 1f, 0f, Char.INFINITE_ACCURACY );
					if (!ch.isAlive()){
						onKill( hero, ch );       //被闭馆击杀
					} else if (hit){
						onHit( hero, ch );        //闭馆命中（未死）
					}
				}
				finish(armor, hero);
			}
		});
	}

	//将化为观众席：闭馆命中 → 深度创伤 5/10/15/20 回合
	private void onHit( Hero hero, Char ch ){
		int points = hero.pointsInTalent(Talent.BECOME_THE_AUDIENCE);
		if (points > 0){
			Buff.prolong( ch, DeepTrauma.class, 5f * points );
		}
	}

	//诸位散落的血肉：闭馆击杀 → 掉落随机素材（血质/肉质/骨质）
	private void onKill( Hero hero, Char ch ){
		int points = hero.pointsInTalent(Talent.SCATTERED_FLESH);
		if (points <= 0 || !(ch instanceof Mob)) return;
		Mob mob = (Mob) ch;

		int drops = 0;
		if (points == 1){
			if (Random.Float() < 0.5f) drops = 1;                 //+1：50% 掉 1
		} else {
			drops = 1;                                            //+2/+3/+4：必定掉 1
			if (points == 3){
				if (Random.Float() < 0.5f) drops += 1;            //+3：50% 额外掉 1
			} else if (points >= 4){
				drops += 1;                                       //+4：必定掉 2
			}
		}

		for (int i = 0; i < drops; i++){
			Dungeon.level.drop( randomFleshMaterial(), mob.pos ).sprite.drop();
		}
	}

	//随机一件「散落的血肉」素材
	private static Item randomFleshMaterial(){
		switch (Random.Int(3)){
			case 0: default: return new BloodMaterial();
			case 1:          return new MeatMaterial();
			case 2:          return new BoneMaterial();
		}
	}

	//收尾：召唤「杰作」单位（固定不动，50 回合后自行消失）并消耗一回合
	private void finish( final ClassArmor armor, final Hero hero ){
		if (Masterpiece.exhibit( hero ) == null){
			GLog.w( "周围没有可以陈列杰作的空地。" );
		}
		hero.spendAndNext( Actor.TICK );
	}

	@Override
	public int icon() {
		return HeroIcon.CLOSING_TIME;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.ENJOY_THE_SHOW, Talent.SCATTERED_FLESH, Talent.BECOME_THE_AUDIENCE, Talent.HEROIC_ENERGY};
	}
}
