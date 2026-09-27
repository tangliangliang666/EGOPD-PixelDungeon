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
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.SelfHarmCost;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

//割腕：二阶近战武器，与长匕首（Dirk）一样有偷袭加成，武技配置套用长匕首，攻击音效与斩击有关。
//效果：参考神器"蓄血圣杯"（ChaliceOfBlood），拥有名为"割腕"的按钮，
//点击消耗自身生命值来直接强化武器（不禁用升级卷轴等其他升级方式）。
//「自伤换成长」：割腕的伤害是升级代价，绝不能被中指长兄的保命机制抹平——既不能被 T3「过人的毅力」的
//血量下限保护夹住，也不能被盔甲技能「咬紧牙关」的 0 血不死兜住（否则每次割腕都「挨完还活着 ⇒ 升级成功」，
//可无条件刷等级）——故实现 SelfHarmCost 标记接口（两处豁免共用这一个判据）。
public class WristSlit extends Dirk implements SelfHarmCost {

	public static final String AC_CUT = "CUT";

	{
		image = ItemSpriteSheet.WRIST_SLIT;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1.1f;

		tier = 2;
	}

	@Override
	public int min(int lvl) {
		return  2 +  //base
				lvl;   //level scaling
	}

	@Override
	public int max(int lvl) {
		return  12 +  //base
				3*lvl;   //level scaling
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero)
				&& !hero.isInvulnerable(getClass())
				&& hero.buff(MagicImmune.class) == null){
			actions.add(AC_CUT);
		}
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_CUT)){

			int minDmg = minCutDmg();
			int maxDmg = maxCutDmg();

			GameScene.show(
				new WndOptions(new ItemSprite(this),
						Messages.titleCase(name()),
						Messages.get(this, "cut_warn", minDmg, maxDmg),
						Messages.get(this, "yes"),
						Messages.get(this, "no")) {
					@Override
					protected void onSelect(int index) {
						if (index == 0) {
							cut(Dungeon.hero);
						}
					}
				}
			);

		}
	}

	private int minCutDmg(){
		return (int)Math.ceil(3 + 2.5f*(level()*level()));
	}

	private int maxCutDmg(){
		return (int)Math.floor(7 + 3.5f*(level()*level()));
	}

	private void cut(Hero hero){
		int damage = Random.NormalIntRange(minCutDmg(), maxCutDmg());

		damage -= hero.drRoll();
		if (damage <= 0){
			damage = 1;
		}

		hero.sprite.operate( hero.pos );
		hero.busy();
		hero.spend(Actor.TICK);
		Sample.INSTANCE.play(Assets.Sounds.CURSED);
		hero.sprite.emitter().burst( ShadowParticle.CURSE, 4+(damage/10) );

		hero.damage(damage, this);

		if (!hero.isAlive()) {
			Badges.validateDeathFromFriendlyMagic();
			Dungeon.fail( this );
			GLog.n( Messages.get(this, "ondeath") );
		} else {
			upgrade();
			GLog.p( Messages.get(this, "oncut") );
		}
	}

}
