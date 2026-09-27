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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Game;

public enum HeroSubClass {

	NONE(HeroIcon.NONE),

	BERSERKER(HeroIcon.BERSERKER),
	GLADIATOR(HeroIcon.GLADIATOR),

	BATTLEMAGE(HeroIcon.BATTLEMAGE),
	WARLOCK(HeroIcon.WARLOCK),
	
	ASSASSIN(HeroIcon.ASSASSIN),
	FREERUNNER(HeroIcon.FREERUNNER),
	
	SNIPER(HeroIcon.SNIPER),
	WARDEN(HeroIcon.WARDEN),

	CHAMPION(HeroIcon.CHAMPION),
	MONK(HeroIcon.MONK),

	PRIEST(HeroIcon.PRIEST),
	PALADIN(HeroIcon.PALADIN),

	//神谕代行者的专精（自定义图标，绘制在原版牧师转职之后）
	DIVINE_FAVORITE(HeroIcon.DIVINE_FAVORITE),
	FATE_FORSAKEN(HeroIcon.FATE_FORSAKEN),

	//环指大师的专精：画廊导师（召唤物制作方向）/ 艺术之巅（武器强化方向）
	GALLERY_MENTOR(HeroIcon.GALLERY_MENTOR),
	ART_PINNACLE(HeroIcon.ART_PINNACLE),

	//拇指 前二老板的专精（2026-09-07 新增，替代原套用的战士狂战士/角斗士）：
	//战争英雄 / 家族之耻，图标位于 hero_icons.png 第 16 行第 3 / 4 列
	WAR_HERO(HeroIcon.WAR_HERO),
	FAMILY_SHAME(HeroIcon.FAMILY_SHAME),

	//中指 长兄的专精（2026-09-16 新增）：
	//忠义巡礼者（复仇技艺方向），图标位于 hero_icons.png 第 16 行第 7 列 = 帧 126；
	//背叛家人者（莱瓦汀方向），图标位于 hero_icons.png 第 17 行第 1 列 = 帧 128
	//（帧 127 是 HeroIcon.NONE 透明图标，两者之间隔开一格）。
	LOYAL_PILGRIM(HeroIcon.LOYAL_PILGRIM),
	FAMILY_BETRAYER(HeroIcon.FAMILY_BETRAYER);

	int icon;

	HeroSubClass(int icon){
		this.icon = icon;
	}
	
	public String title() {
		return Messages.get(this, name());
	}

	public String shortDesc() {
		return Messages.get(this, name()+"_short_desc");
	}

	public String desc() {
		//Include the staff effect description in the battlemage's desc if possible
		if (this == BATTLEMAGE){
			String desc = Messages.get(this, name() + "_desc");
			if (Game.scene() instanceof GameScene){
				MagesStaff staff = Dungeon.hero.belongings.getItem(MagesStaff.class);
				if (staff != null && staff.wandClass() != null){
					desc += "\n\n" + Messages.get(staff.wandClass(), "bmage_desc");
					desc = desc.replaceAll("_", "");
				}
			}
			return desc;
		} else {
			return Messages.get(this, name() + "_desc");
		}
	}

	public int icon(){
		return icon;
	}

}
