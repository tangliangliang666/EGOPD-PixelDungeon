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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;

//icons for hero subclasses and abilities atm, maybe add classes?
public class HeroIcon extends Image {

	private static TextureFilm film;
	private static final int SIZE = 16;

	//transparent icon
	public static final int NONE    = 127;

	//subclasses
	public static final int BERSERKER   = 0;
	public static final int GLADIATOR   = 1;
	public static final int BATTLEMAGE  = 2;
	public static final int WARLOCK     = 3;
	public static final int ASSASSIN    = 4;
	public static final int FREERUNNER  = 5;
	public static final int SNIPER      = 6;
	public static final int WARDEN      = 7;
	public static final int CHAMPION    = 8;
	public static final int MONK        = 9;
	public static final int PRIEST      = 10;
	public static final int PALADIN     = 11;
	//神谕代行者的专精图标（绘制在原版牧师转职之后）
	public static final int DIVINE_FAVORITE = 12;
	public static final int FATE_FORSAKEN   = 13;
	//环指大师专精图标（已绘制：hero_icons.png 第二行第 7 列 / 第 8 列）
	public static final int GALLERY_MENTOR  = 14;
	public static final int ART_PINNACLE    = 15;
	//环指大师技艺图标（已绘制：hero_icons.png 第 15 行，索引 112~120）
	public static final int ART_CRAFT       = 112;
	public static final int ART_REINFORCE   = 113;
	public static final int ART_DECOMPOSE   = 114;
	public static final int ART_CONSTRUCT   = 115;
	public static final int ART_COMMAND     = 116;
	//画廊导师展品修整技艺（用户绘制：指挥 116 右侧依次 活化/修整/激发；装备在创作 112 正下方）
	public static final int ART_ACTIVATE    = 117;
	public static final int ART_REPAIR      = 118;
	public static final int ART_STIMULATE   = 119;
	public static final int ART_EQUIP       = 120;
	//大师指环图标（由 items.png 的 MASTER_RING 8×10 像素复制到第 16 行第 2 列；原供已废弃的 ExhibitConstruct.actionIcon() 使用，现无调用方、保留备用）
	public static final int MASTER_RING_ICON = 121;
	//拇指 前二老板的专精图标（用户已绘制：hero_icons.png 第 16 行第 3 / 4 列 = 帧 122 / 123）
	public static final int WAR_HERO      = 122;
	public static final int FAMILY_SHAME  = 123;
	//拇指 前二老板的盔甲技能图标（用户已绘制计划：第 16 行第 5 / 6 列 = 帧 124 / 125；处置/瞄准心脏）
	//「心-不光彩」按用户要求复用神谕代行者的心-命运图标 HeroIcon.HEART_FATE(35)
	public static final int DISPOSAL      = 124;
	public static final int AIM_HEART     = 125;
	//中指 长兄 转职「忠义巡礼者」专精图标（用户已绘制：hero_icons.png 倒数第二个位置 = 第 16 行第 7 列 = 帧 126）。
	//注意：第 16 行的最后一格 127 被 HeroIcon.NONE（透明图标）占用。
	public static final int LOYAL_PILGRIM = 126;
	//中指 长兄 转职「背叛家人者」专精图标（用户已绘制：hero_icons.png 第 17 行第 1 列 = 帧 128）。
	//hero_icons.png 2026-09-16 已加高一行（128×272 = 8 列 × 17 行 ⇒ 帧 0..135），
	//所以第 17 行起（128 之后）都是可用槽位，NONE 仍然留在 127。
	public static final int FAMILY_BETRAYER = 128;
	//中指 长兄 三个盔甲技能的图标槽位（2026-09-17：hero_icons.png **第 17 行第 2/3/4 列 = 帧 129/130/131**，
	//每行 8 帧 ⇒ 帧号=(行-1)×8+(列-1)）。
	//2026-09-17 定名与实装（三格专用，均不再借用其它帧）：
	//  其一「咬紧牙关」= 帧 129（曾用 104 怒气帧 + 紫滤镜，已改为专属帧）；
	//  其二「永不遗忘」= 帧 130；
	//  其三「即刻处刑[莱瓦汀]」= 帧 131。
	public static final int MIDDLE_FINGER_ABILITY_1 = 129;
	public static final int MIDDLE_FINGER_ABILITY_2 = 130;
	public static final int MIDDLE_FINGER_ABILITY_3 = 131;

	//abilities
	public static final int HEROIC_LEAP     = 16;
	public static final int SHOCKWAVE       = 17;
	public static final int ENDURE          = 18;
	public static final int ELEMENTAL_BLAST = 19;
	public static final int WILD_MAGIC      = 20;
	public static final int WARP_BEACON     = 21;
	public static final int SMOKE_BOMB      = 22;
	public static final int DEATH_MARK      = 23;
	public static final int SHADOW_CLONE    = 24;
	public static final int SPECTRAL_BLADES = 25;
	public static final int NATURES_POWER   = 26;
	public static final int SPIRIT_HAWK     = 27;
	public static final int CHALLENGE       = 28;
	public static final int ELEMENTAL_STRIKE= 29;
	public static final int FEINT           = 30;
	public static final int ASCENDED_FORM   = 31;
	public static final int TRINITY         = 32;
	public static final int POWER_OF_MANY   = 33;
	public static final int RATMOGRIFY      = 34;
	//神谕代行者的盔甲技能图标
	public static final int HEART_FATE      = 35;
	public static final int FURIOSO_REPLICA = 36;
	public static final int PIN_HAO_FAN     = 37;
	//环指大师的盔甲技能图标（用户已绘制：hero_icons.png 帧 39，位于第 5 行第 8 列）
	public static final int CLOSING_TIME    = 39;
	//环指大师的盔甲技能图标（用户已绘制：hero_icons.png 第 9 行第 4/5 列 = 帧 67/68）
	public static final int DECOY           = 67;
	public static final int CORRIDOR        = 68;

	//cleric spells
	public static final int GUIDING_LIGHT   = 40;
	public static final int HOLY_WEAPON     = 41;
	public static final int HOLY_WARD       = 42;
	public static final int HOLY_INTUITION  = 43;
	public static final int SHIELD_OF_LIGHT = 44;
	public static final int RECALL_GLYPH    = 45;
	public static final int SUNRAY          = 46;
	public static final int DIVINE_SENSE    = 47;
	public static final int BLESS           = 48;
	public static final int CLEANSE         = 49;
	public static final int RADIANCE        = 50;
	public static final int HOLY_LANCE      = 51;
	public static final int HALLOWED_GROUND = 52;
	public static final int MNEMONIC_PRAYER = 53;
	public static final int SMITE           = 54;
	public static final int LAY_ON_HANDS    = 55;
	public static final int AURA_OF_PROTECTION = 56;
	public static final int WALL_OF_LIGHT   = 57;
	public static final int DIVINE_INTERVENTION = 58;
	public static final int JUDGEMENT       = 59;
	public static final int FLASH           = 60;
	public static final int BODY_FORM       = 61;
	public static final int MIND_FORM       = 62;
	public static final int SPIRIT_FORM     = 63;
	public static final int BEAMING_RAY     = 64;
	public static final int LIFE_LINK       = 65;
	public static final int STASIS          = 66;

	//all cleric spells have a separate icon with no background for the action indicator
	public static final int SPELL_ACTION_OFFSET      = 32;

	//action indicator visuals
	public static final int BERSERK         = 104;
	public static final int COMBO           = 105;
	public static final int PREPARATION     = 106;
	public static final int MOMENTUM        = 107;
	public static final int SNIPERS_MARK    = 108;
	public static final int WEAPON_SWAP     = 109;
	public static final int MONK_ABILITIES  = 110;

	public HeroIcon(HeroSubClass subCls){
		super( Assets.Interfaces.HERO_ICONS );
		if (film == null){
			film = new TextureFilm(texture, SIZE, SIZE);
		}
		frame(film.get(subCls.icon()));
	}

	public HeroIcon(ArmorAbility abil){
		super( Assets.Interfaces.HERO_ICONS );
		if (film == null){
			film = new TextureFilm(texture, SIZE, SIZE);
		}
		frame(film.get(abil.icon()));
		//技能可以要求给图标叠一层滤镜（复用现成的图标帧时用来做区分，见 ArmorAbility.iconTint()）
		int tint = abil.iconTint();
		if (tint != ArmorAbility.NO_TINT){
			hardlight( ((tint >> 16) & 0xFF) / 255f,
					((tint >> 8) & 0xFF) / 255f,
					(tint & 0xFF) / 255f );
		}
	}

	public HeroIcon(ActionIndicator.Action action){
		super( Assets.Interfaces.HERO_ICONS );
		if (film == null){
			film = new TextureFilm(texture, SIZE, SIZE);
		}
		frame(film.get(action.actionIcon()));
	}

	public HeroIcon(ClericSpell spell){
		super( Assets.Interfaces.HERO_ICONS );
		if (film == null){
			film = new TextureFilm(texture, SIZE, SIZE);
		}
		frame(film.get(spell.icon()));
	}

	//环指大师的技艺图标（同 ClericSpell 模式，帧号由 art.icon() 提供）
	public HeroIcon(com.shatteredpixel.shatteredpixeldungeon.actors.hero.arts.RingMasterArt art){
		super( Assets.Interfaces.HERO_ICONS );
		if (film == null){
			film = new TextureFilm(texture, SIZE, SIZE);
		}
		frame(film.get(art.icon()));
	}

}
