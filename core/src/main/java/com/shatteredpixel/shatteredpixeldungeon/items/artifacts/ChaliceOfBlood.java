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
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.SelfHarmCost;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//「自伤换成长」：血祭的伤害是升级代价，绝不能被中指长兄的保命机制抹平——既不能被 T3「过人的毅力」的
//血量下限保护夹住，也不能被盔甲技能「咬紧牙关」的 0 血不死兜住（否则每次血祭都「挨完还活着 ⇒ 升级成功」，
//可无条件刷满 10 级）——故实现 SelfHarmCost 标记接口（两处豁免共用这一个判据）。
public class ChaliceOfBlood extends Artifact implements SelfHarmCost {

	{
		image = ItemSpriteSheet.ARTIFACT_CHALICE1;

		levelCap = 10;
	}

	public static final String AC_PRICK = "PRICK";

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		if (isEquipped( hero )
				&& level() < levelCap
				&& !cursed
				&& !hero.isInvulnerable(getClass())
				&& hero.buff(MagicImmune.class) == null)
			actions.add(AC_PRICK);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action ) {
		super.execute(hero, action);

		if (action.equals(AC_PRICK)){

			int minDmg = minPrickDmg();
			int maxDmg = maxPrickDmg();

			int totalHeroHP = hero.HP + hero.shielding() + extraPrickHP(hero);

			float deathChance = 0;

			if (totalHeroHP < maxDmg) {
				deathChance = (maxDmg - totalHeroHP) / (float) (maxDmg - minDmg);
				if (deathChance < 0.5f) {
					deathChance = (float) Math.pow(2 * deathChance, 2) / 2f;
				} else if (deathChance < 1f) {
					deathChance = 1f - deathChance;
					deathChance = (float) Math.pow(2 * deathChance, 2) / 2f;
					deathChance = 1f - deathChance;
				} else {
					deathChance = 1;
				}
			}

			GameScene.show(
				new WndOptions(new ItemSprite(this),
						Messages.titleCase(name()),
						Messages.get(this, "prick_warn", minDmg, maxDmg, Messages.decimalFormat("#.##", 100*deathChance)),
						Messages.get(this, "yes"),
						Messages.get(this, "no")) {
					@Override
					protected void onSelect(int index) {
						if (index == 0) {
							prick(Dungeon.hero);
						}
					}
				}
			);

		}
	}

	/**
	 * 计算血祭死亡概率时，额外计入的「等效血量」（强化形态覆写，如「血宴」可用来抵挡升级伤害）。
	 * <p>默认 0（原版行为）。这是**唯一**把额外抵挡量喂给那条死亡概率提示的入口 ——
	 * 不接这里的话，玩家看到的仍是未减免的概率，而这个窗口是「生死确认」窗口，不能骗人。
	 */
	protected int extraPrickHP(Hero hero){
		return 0;
	}

	protected int minPrickDmg(){
		return (int)Math.ceil(3 + 2.5f*(level()*level()));
	}

	protected int maxPrickDmg(){
		return (int)Math.floor(7 + 3.5f*(level()*level()));
	}

	/**
	 * 升级伤害的**最后一道减免**（强化形态覆写，如「血宴」按累计量抵挡）。
	 * <p>调用点在 {@code prick} 内、原本「至少造成 1 点伤害」的保底**之后** ——
	 * 所以覆写方**可以把这次升级伤害削到 0**（这正是「用血宴换取无伤升级」所要的语义）；
	 * 若把钩子放在保底之前，原版的 {@code damage <= 0 → 1} 会把减免结果重新抬回 1 点。
	 *
	 * @param damage 经防御/减免后、已被保底夹到至少 1 的伤害
	 * @return 实际要扣的伤害（0 表示这次升级完全无伤）
	 */
	protected int mitigatePrickDamage(Hero hero, int damage){
		return damage;
	}

	private void prick(Hero hero){
		int damage = Random.NormalIntRange(minPrickDmg(), maxPrickDmg());

		//need to process on-hit effects manually
		Earthroot.Armor armor = hero.buff(Earthroot.Armor.class);
		if (armor != null) {
			damage = armor.absorb(damage);
		}

		if (hero.buff(MagicImmune.class) != null && hero.buff(HolyWard.HolyArmBuff.class) != null){
			damage -= hero.subClass == HeroSubClass.PALADIN ? 3 : 1;
		}

		WandOfLivingEarth.RockArmor rockArmor = hero.buff(WandOfLivingEarth.RockArmor.class);
		if (rockArmor != null) {
			damage = rockArmor.absorb(damage);
		}

		damage -= hero.drRoll();

		hero.sprite.operate( hero.pos );
		hero.busy();
		hero.spend(Actor.TICK);
		GLog.w( Messages.get(this, "onprick") );
		if (damage <= 0){
			damage = 1;
		} else {
			Sample.INSTANCE.play(Assets.Sounds.CURSED);
			hero.sprite.emitter().burst( ShadowParticle.CURSE, 4+(damage/10) );
		}

		//强化形态的最后一道减免（见 mitigatePrickDamage 的注释：必须在保底之后，才允许削到 0）
		//提示统一在这里播报：钩子自己不要再打一次，否则同一个减免会弹两条消息
		int before = damage;
		damage = mitigatePrickDamage(hero, damage);
		if (damage < before){
			GLog.p( Messages.get(this, "prick_mitigated", before - damage) );
		}

		if (damage > 0) {
			hero.damage(damage, this);
		}

		if (!hero.isAlive()) {
			Badges.validateDeathFromFriendlyMagic();
			Dungeon.fail( this );
			GLog.n( Messages.get(this, "ondeath") );
		} else {
			upgrade();
			Catalog.countUse(getClass());
		}
	}

	/**
	 * 按等级刷新贴图。原版在 {@link #upgrade()} 与 {@link #restoreFromBundle} 里**各写了一遍**，
	 * 且两处读的不是同一个时点的等级（upgrade 读「升级前」、restore 读「升级后」）。
	 * 抽成一个方法后，强化形态只需覆写它就能换上自己的三帧，不必再去追这四处硬编码。
	 *
	 * @param lvl 用于判定帧的等级 —— 语义对齐原版：upgrade 传「升级后」的等级，restore 传当前等级
	 */
	protected void updateImage(int lvl){
		if (lvl >= 7)      image = ItemSpriteSheet.ARTIFACT_CHALICE3;
		else if (lvl >= 3) image = ItemSpriteSheet.ARTIFACT_CHALICE2;
	}

	@Override
	public Item upgrade() {
		//原版此处读的是升级**前**的 level()（阈值 6/2），换成「升级后」的等价写法，行为不变
		updateImage(level() + 1);
		return super.upgrade();
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		updateImage(level());
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new chaliceRegen();
	}
	
	@Override
	public void charge(Hero target, float amount) {
		if (cursed || target.buff(MagicImmune.class) != null) return;

		//grants 5 turns of healing up-front, if hero isn't starving
		if (target.isStarving()) return;

		float healDelay = 10f - (1.33f + level()*0.667f);
		healDelay /= amount;
		float heal = 5f/healDelay;
		//effectively 0.5/1/1.5/2/2.5 HP per turn at +0/+6/+8/+9/+10
		if (Random.Float() < heal%1){
			heal++;
		}
		if (heal >= 1f && target.HP < target.HT) {
			int healed = target.heal( (int)heal );
			if (healed > 0) {
				target.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);
			}

			if (target.HP == target.HT && target instanceof Hero) {
				((Hero) target).resting = false;
			}
		}
	}
	
	@Override
	public String desc() {
		String desc = super.desc();

		if (isEquipped (Dungeon.hero)){
			desc += "\n\n";
			if (cursed)
				desc += Messages.get(this, "desc_cursed");
			else if (level() == 0)
				desc += Messages.get(this, "desc_1");
			else if (level() < levelCap)
				desc += Messages.get(this, "desc_2");
			else
				desc += Messages.get(this, "desc_3");
		}

		return desc;
	}

	public class chaliceRegen extends ArtifactBuff {
		//see Regeneration.class for effect
	}

}
