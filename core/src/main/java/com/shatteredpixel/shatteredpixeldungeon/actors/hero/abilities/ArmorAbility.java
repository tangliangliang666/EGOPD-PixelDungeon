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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

public abstract class ArmorAbility implements Bundlable {

	protected float baseChargeUse = 35;

	public void use( ClassArmor armor, Hero hero ){
		if (targetingPrompt() == null){
			activate(armor, hero, hero.pos);
		} else {
			GameScene.selectCell(new CellSelector.Listener() {
				@Override
				public void onSelect(Integer cell) {
					activate(armor, hero, cell);
				}

				@Override
				public String prompt() {
					return targetingPrompt();
				}
			});
		}
	}

	//leave null for no targeting
	public String targetingPrompt(){
		return null;
	}

	public boolean useTargeting(){
		return targetingPrompt() != null;
	}

	public int targetedPos( Char user, int dst ){
		return new Ballistica( user.pos, dst, Ballistica.PROJECTILE ).collisionPos;
	}

	public float chargeUse( Hero hero ){
		float chargeUse = baseChargeUse;
		if (hero.hasTalent(Talent.HEROIC_ENERGY)){
			//reduced charge use by 12%/23%/32%/40%
			switch (hero.pointsInTalent(Talent.HEROIC_ENERGY)){
				case 1: default:
					chargeUse *= 0.88f;
					break;
				case 2:
					chargeUse *= 0.77f;
					break;
				case 3:
					chargeUse *= 0.68f;
					break;
				case 4:
					chargeUse *= 0.6f;
					break;
			}
		}
		return chargeUse;
	}

	protected abstract void activate( ClassArmor armor, Hero hero, Integer target );

	public String name(){
		return Messages.get(this, "name");
	}

	public String shortDesc(){
		return Messages.get(this, "short_desc");
	}

	/**
	 * 列表里用的「技能名 + 短描述」：供<b>盔甲技能选择界面</b>（{@code WndChooseAbility}）、
	 * 英雄图鉴的技能页（{@code WndHeroInfo}）与盔甲物品描述（{@code ClassArmor.desc()}）使用。
	 *
	 * <p>原生技能的 {@code short_desc} 习惯把技能名写进句子里（如「战士_苦痛坚忍_，跳过若干回合…」），
	 * 而本 mod 自制的技能大多直接从描述起笔，于是这些列表里就只剩一串话、认不出是哪一门技能。</p>
	 *
	 * <p>这里统一保证名字出现在描述中：<b>描述已经含名字就原样返回</b>（不区分大小写，避免原生技能重复显示），
	 * 否则在开头补一行高亮的技能名。返回文本带 {@code _强调_} 标记与 {@code \n} 换行，
	 * 交给 {@code RenderedTextBlock} 渲染（按钮与图鉴文本块都支持）。</p>
	 */
	public String namedShortDesc(){
		String desc = shortDesc();
		//不区分大小写：英文原版把名字写成句内变形（endure ⇒ _Endures_），逐字比较会漏判
		if (Messages.lowerCase(desc).contains( Messages.lowerCase(name()) )){
			return desc;
		}
		return "_" + Messages.titleCase( name() ) + "_\n" + desc;
	}

	public String desc(){
		return Messages.get(this, "desc") + "\n\n" + Messages.get(this, "cost", (int)baseChargeUse);
	}

	public int icon(){
		return HeroIcon.NONE;
	}

	/** {@link #iconTint()} 的默认值：不加滤镜。 */
	public static final int NO_TINT = 0xFFFFFF;

	/**
	 * 图标要叠的滤镜颜色（0xRRGGBB，默认 {@link #NO_TINT} = 不加）。
	 *
	 * <p>给「借用现成图标帧」的技能用来做出区分：{@code HeroIcon(ArmorAbility)} 会把这个颜色
	 * 以 {@code hardlight} 的方式乘到帧上（每个通道各自归一化到 0~1，所以 {@code 0xFF5AFF}
	 * 就是「保留红与蓝、压掉绿」的紫色滤镜）。</p>
	 */
	public int iconTint(){
		return NO_TINT;
	}

	public abstract Talent[] talents();

	@Override
	public void storeInBundle(Bundle bundle) {
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
	}
}
