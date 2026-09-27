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

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.noosa.TextureFilm;

public class ItemSpriteSheet {

	public static final int SIZE = 16;

	private static final int TX_WIDTH = 256;
	//items.png is now 800px tall (50 rows): 32 vanilla rows + custom rows (33-40, incl. the
	//two-form artifact at row 39 cols 7/8), plus rows 41-50 reserved for future expansion.
	//Keep this value equal to the PNG's real height or every item icon shifts.
	private static final int TX_HEIGHT = 800;

	private static final int WIDTH = TX_WIDTH / SIZE;

	public static TextureFilm film = new TextureFilm( TX_WIDTH, TX_HEIGHT, SIZE, SIZE );

	private static int xy(int x, int y){
		x -= 1; y -= 1;
		return x + WIDTH*y;
	}

	private static void assignItemRect( int item, int width, int height ){
		int x = (item % WIDTH) * SIZE;
		int y = (item / WIDTH) * SIZE;
		film.add( item, x, y, x+width, y+height);
	}

	private static final int PLACEHOLDERS   =                               xy(1, 1);   //18 slots
	//SOMETHING is the default item sprite at position 0. May show up ingame if there are bugs.
	public static final int SOMETHING       = PLACEHOLDERS+0;
	public static final int WEAPON_HOLDER   = PLACEHOLDERS+1;
	public static final int ARMOR_HOLDER    = PLACEHOLDERS+2;
	public static final int MISSILE_HOLDER  = PLACEHOLDERS+3;
	public static final int WAND_HOLDER     = PLACEHOLDERS+4;
	public static final int RING_HOLDER     = PLACEHOLDERS+5;
	public static final int ARTIFACT_HOLDER = PLACEHOLDERS+6;
	public static final int TRINKET_HOLDER  = PLACEHOLDERS+7;
	public static final int FOOD_HOLDER     = PLACEHOLDERS+8;
	public static final int BOMB_HOLDER     = PLACEHOLDERS+9;
	public static final int POTION_HOLDER   = PLACEHOLDERS+10;
	public static final int SEED_HOLDER     = PLACEHOLDERS+11;
	public static final int SCROLL_HOLDER   = PLACEHOLDERS+12;
	public static final int STONE_HOLDER    = PLACEHOLDERS+13;
	public static final int ELIXIR_HOLDER   = PLACEHOLDERS+14;
	public static final int SPELL_HOLDER    = PLACEHOLDERS+15;
	public static final int MOB_HOLDER      = PLACEHOLDERS+16;
	public static final int DOCUMENT_HOLDER = PLACEHOLDERS+17;
	static{
		assignItemRect(SOMETHING,       8,  13);
		assignItemRect(WEAPON_HOLDER,   14, 14);
		assignItemRect(ARMOR_HOLDER,    14, 12);
		assignItemRect(MISSILE_HOLDER,  15, 15);
		assignItemRect(WAND_HOLDER,     14, 14);
		assignItemRect(RING_HOLDER,     8,  10);
		assignItemRect(ARTIFACT_HOLDER, 15, 15);
		assignItemRect(TRINKET_HOLDER,  16, 11);
		assignItemRect(FOOD_HOLDER,     15, 11);
		assignItemRect(BOMB_HOLDER,     10, 13);
		assignItemRect(POTION_HOLDER,   12, 14);
		assignItemRect(SEED_HOLDER,     10, 10);
		assignItemRect(SCROLL_HOLDER,   15, 14);
		assignItemRect(STONE_HOLDER,    14, 12);
		assignItemRect(ELIXIR_HOLDER,   12, 14);
		assignItemRect(SPELL_HOLDER,    8,  16);
		assignItemRect(MOB_HOLDER,      15, 14);
		assignItemRect(DOCUMENT_HOLDER, 10, 11);
	}

	private static final int UNCOLLECTIBLE  =                               xy(3, 2);   //14 slots
	public static final int GOLD            = UNCOLLECTIBLE+0;
	public static final int ENERGY          = UNCOLLECTIBLE+1;

	public static final int DEWDROP         = UNCOLLECTIBLE+3;
	public static final int PETAL           = UNCOLLECTIBLE+4;
	public static final int SANDBAG         = UNCOLLECTIBLE+5;
	public static final int SPIRIT_ARROW    = UNCOLLECTIBLE+6;
	//脑啡肽（E.G.O武器分解/重构材料，绘制于23行第15个，12×13）
	public static final int ENKEPHALIN      =                            xy(15, 23);
	//逻辑工作室子弹（漆黑噤默远程形态「逻辑工作室」射出的弹丸，绘制于2行第10个，10×10；占用 UNCOLLECTIBLE 组的空号 +7）
	public static final int LOGIC_BULLET    =                            xy(10, 2);
	
	public static final int TENGU_BOMB      = UNCOLLECTIBLE+8;
	public static final int TENGU_SHOCKER   = UNCOLLECTIBLE+9;
	public static final int GEO_BOULDER     = UNCOLLECTIBLE+10;
	//「根基」考验（YESOD）用的未知物品贴图：绘制于第2行第16列（10×15 的问号，即 UNCOLLECTIBLE 组再往右两个空号）。
	//⚠️ 不能用 UNCOLLECTIBLE+11 —— 那样算出的是第2行第14列的 29 号（空的），而图形画在 31 号（xy(16,2)）。
	public static final int UNKNOWN_ITEM    =                            xy(16, 2);
	static{
		assignItemRect(GOLD,        15, 13);
		assignItemRect(ENERGY,      16, 16);

		assignItemRect(DEWDROP,     10, 10);
		assignItemRect(PETAL,       8,  8);
		assignItemRect(SANDBAG,     10, 10);
		assignItemRect(SPIRIT_ARROW,11, 11);
		assignItemRect(ENKEPHALIN,  12, 13);
		assignItemRect(LOGIC_BULLET,10, 10);
		
		assignItemRect(TENGU_BOMB,      10, 10);
		assignItemRect(TENGU_SHOCKER,   10, 10);
		assignItemRect(GEO_BOULDER,     16, 14);
		assignItemRect(UNKNOWN_ITEM,    10, 15);
	}

	private static final int CONTAINERS     =                               xy(1, 3);   //16 slots
	public static final int BONES           = CONTAINERS+0;
	public static final int REMAINS         = CONTAINERS+1;
	public static final int TOMB            = CONTAINERS+2;
	public static final int GRAVE           = CONTAINERS+3;
	public static final int CHEST           = CONTAINERS+4;
	public static final int LOCKED_CHEST    = CONTAINERS+5;
	public static final int CRYSTAL_CHEST   = CONTAINERS+6;
	public static final int EBONY_CHEST     = CONTAINERS+7;
	//破损终端（神谕代行者遗物），绘制于第3行第9列（容器行右侧），14×14
	public static final int BROKEN_TERMINAL =                                xy(9, 3);
	//断裂骨刃（环指大师遗物），绘制于第3行第10列（破损终端右侧），13×9
	public static final int BROKEN_BONE_BLADE =                              xy(10, 3);
	//破损义眼（拇指·前二老板遗物），绘制于第3行第11列（断裂骨刃右侧），12×14
	public static final int BROKEN_EYE =                                     xy(11, 3);
	//账簿残页（中指长兄遗物），绘制于第3行第12列（破损义眼右侧），11×13
	public static final int LEDGER_PAGE =                                    xy(12, 3);
	static{
		assignItemRect(BONES,           14, 11);
		assignItemRect(REMAINS,         14, 11);
		assignItemRect(TOMB,            14, 15);
		assignItemRect(GRAVE,           14, 15);
		assignItemRect(CHEST,           16, 14);
		assignItemRect(LOCKED_CHEST,    16, 14);
		assignItemRect(CRYSTAL_CHEST,   16, 14);
		assignItemRect(EBONY_CHEST,     16, 14);
		assignItemRect(BROKEN_TERMINAL, 14, 14);
		assignItemRect(BROKEN_BONE_BLADE, 13, 9);
		assignItemRect(BROKEN_EYE, 12, 14);
		assignItemRect(LEDGER_PAGE, 11, 13);
	}

	private static final int MISC_CONSUMABLE =                              xy(1, 4);   //32 slots
	public static final int ANKH            = MISC_CONSUMABLE +0;
	public static final int STYLUS          = MISC_CONSUMABLE +1;
	public static final int SEAL            = MISC_CONSUMABLE +2;
	public static final int TORCH           = MISC_CONSUMABLE +3;
	public static final int BEACON          = MISC_CONSUMABLE +4;
	public static final int HONEYPOT        = MISC_CONSUMABLE +5;
	public static final int SHATTPOT        = MISC_CONSUMABLE +6;
	public static final int IRON_KEY        = MISC_CONSUMABLE +7;
	public static final int GOLDEN_KEY      = MISC_CONSUMABLE +8;
	public static final int CRYSTAL_KEY     = MISC_CONSUMABLE +9;
	public static final int WORN_KEY        = MISC_CONSUMABLE +10;
	public static final int MASK            = MISC_CONSUMABLE +11;
	public static final int CROWN           = MISC_CONSUMABLE +12;
	public static final int AMULET          = MISC_CONSUMABLE +13;
	public static final int MASTERY         = MISC_CONSUMABLE +14;
	public static final int KIT             = MISC_CONSUMABLE +15;
	public static final int SEAL_SHARD      = MISC_CONSUMABLE +16;
	public static final int BROKEN_STAFF    = MISC_CONSUMABLE +17;
	public static final int CLOAK_SCRAP     = MISC_CONSUMABLE +18;
	public static final int BOW_FRAGMENT    = MISC_CONSUMABLE +19;
	public static final int BROKEN_HILT     = MISC_CONSUMABLE +20;
	public static final int TORN_PAGE       = MISC_CONSUMABLE +21;
	public static final int TRINKET_CATA    = MISC_CONSUMABLE +22;

	static{
		assignItemRect(ANKH,            10, 16);
		assignItemRect(STYLUS,          12, 13);
		
		assignItemRect(SEAL,            13, 13);
		assignItemRect(TORCH,           12, 15);
		assignItemRect(BEACON,          16, 15);
		
		assignItemRect(HONEYPOT,        14, 12);
		assignItemRect(SHATTPOT,        14, 12);
		assignItemRect(IRON_KEY,        8,  14);
		assignItemRect(GOLDEN_KEY,      8,  14);
		assignItemRect(CRYSTAL_KEY,     8,  14);
		assignItemRect(WORN_KEY,        8,  14);
		assignItemRect(MASK,            11,  9);
		assignItemRect(CROWN,           13,  7);
		assignItemRect(AMULET,          16, 16);
		assignItemRect(MASTERY,         13, 16);
		assignItemRect(KIT,             16, 15);

		assignItemRect(SEAL_SHARD,      12, 12);
		assignItemRect(BROKEN_STAFF,    14, 10);
		assignItemRect(CLOAK_SCRAP,      9,  9);
		assignItemRect(BOW_FRAGMENT,    12,  9);
		assignItemRect(BROKEN_HILT,      9,  9);
		assignItemRect(TORN_PAGE,       11, 13);

		assignItemRect(TRINKET_CATA,    12, 11);
	}
	
	private static final int BOMBS          =                               xy(1, 6);   //16 slots
	public static final int BOMB            = BOMBS+0;
	public static final int DBL_BOMB        = BOMBS+1;
	public static final int FIRE_BOMB       = BOMBS+2;
	public static final int FROST_BOMB      = BOMBS+3;
	public static final int REGROWTH_BOMB   = BOMBS+4;
	public static final int SMOKE_BOMB      = BOMBS+5;
	public static final int FLASHBANG       = BOMBS+6;
	public static final int HOLY_BOMB       = BOMBS+7;
	public static final int WOOLY_BOMB      = BOMBS+8;
	public static final int NOISEMAKER      = BOMBS+9;
	public static final int ARCANE_BOMB     = BOMBS+10;
	public static final int SHRAPNEL_BOMB   = BOMBS+11;
	
	static{
		assignItemRect(BOMB,            10, 13);
		assignItemRect(DBL_BOMB,        14, 13);
		assignItemRect(FIRE_BOMB,       13, 12);
		assignItemRect(FROST_BOMB,      13, 12);
		assignItemRect(REGROWTH_BOMB,   13, 12);
		assignItemRect(SMOKE_BOMB,      13, 12);
		assignItemRect(FLASHBANG,       10, 13);
		assignItemRect(HOLY_BOMB,       10, 13);
		assignItemRect(WOOLY_BOMB,      10, 13);
		assignItemRect(NOISEMAKER,      10, 13);
		assignItemRect(ARCANE_BOMB,     10, 13);
		assignItemRect(SHRAPNEL_BOMB,   10, 13);
	}

	private static final int WEP_TIER1      =                               xy(1, 7);   //8 slots
	public static final int WORN_SHORTSWORD = WEP_TIER1+0;
	public static final int CUDGEL          = WEP_TIER1+1;
	public static final int GLOVES          = WEP_TIER1+2;
	public static final int RAPIER          = WEP_TIER1+3;
	public static final int DAGGER          = WEP_TIER1+4;
	public static final int MAGES_STAFF     = WEP_TIER1+5;
	static{
		assignItemRect(WORN_SHORTSWORD, 13, 13);
		assignItemRect(CUDGEL,          15, 15);
		assignItemRect(GLOVES,          12, 16);
		assignItemRect(RAPIER,          13, 14);
		assignItemRect(DAGGER,          12, 13);
		assignItemRect(MAGES_STAFF,     15, 14);
	}

	private static final int WEP_TIER2      =                               xy(9, 7);   //8 slots
	public static final int SHORTSWORD      = WEP_TIER2+0;
	public static final int HAND_AXE        = WEP_TIER2+1;
	public static final int SPEAR           = WEP_TIER2+2;
	public static final int QUARTERSTAFF    = WEP_TIER2+3;
	public static final int DIRK            = WEP_TIER2+4;
	public static final int SICKLE          = WEP_TIER2+5;
	static{
		assignItemRect(SHORTSWORD,      13, 13);
		assignItemRect(HAND_AXE,        12, 14);
		assignItemRect(SPEAR,           16, 16);
		assignItemRect(QUARTERSTAFF,    16, 16);
		assignItemRect(DIRK,            13, 14);
		assignItemRect(SICKLE,          15, 15);
	}

	private static final int WEP_TIER3      =                               xy(1, 8);   //8 slots
	public static final int SWORD           = WEP_TIER3+0;
	public static final int MACE            = WEP_TIER3+1;
	public static final int SCIMITAR        = WEP_TIER3+2;
	public static final int ROUND_SHIELD    = WEP_TIER3+3;
	public static final int SAI             = WEP_TIER3+4;
	public static final int WHIP            = WEP_TIER3+5;
	static{
		assignItemRect(SWORD,           14, 14);
		assignItemRect(MACE,            15, 15);
		assignItemRect(SCIMITAR,        13, 16);
		assignItemRect(ROUND_SHIELD,    16, 16);
		assignItemRect(SAI,             16, 16);
		assignItemRect(WHIP,            14, 14);
	}

	private static final int WEP_TIER4      =                               xy(9, 8);   //8 slots
	public static final int LONGSWORD       = WEP_TIER4+0;
	public static final int BATTLE_AXE      = WEP_TIER4+1;
	public static final int FLAIL           = WEP_TIER4+2;
	public static final int RUNIC_BLADE     = WEP_TIER4+3;
	public static final int ASSASSINS_BLADE = WEP_TIER4+4;
	public static final int CROSSBOW        = WEP_TIER4+5;
	public static final int KATANA          = WEP_TIER4+6;
	static{
		assignItemRect(LONGSWORD,       15, 15);
		assignItemRect(BATTLE_AXE,      16, 16);
		assignItemRect(FLAIL,           14, 14);
		assignItemRect(RUNIC_BLADE,     14, 14);
		assignItemRect(ASSASSINS_BLADE, 14, 15);
		assignItemRect(CROSSBOW,        15, 15);
		assignItemRect(KATANA,          15, 16);
	}

	private static final int WEP_TIER5      =                               xy(1, 9);   //8 slots
	public static final int GREATSWORD      = WEP_TIER5+0;
	public static final int WAR_HAMMER      = WEP_TIER5+1;
	public static final int GLAIVE          = WEP_TIER5+2;
	public static final int GREATAXE        = WEP_TIER5+3;
	public static final int GREATSHIELD     = WEP_TIER5+4;
	public static final int GAUNTLETS       = WEP_TIER5+5;
	public static final int WAR_SCYTHE      = WEP_TIER5+6;
	//《是谁嗯嗯在我的头上》（神谕代行者初始投掷武器，绘本），绘制于9行15列，13×12
	public static final int POOP_STORY_BOOK =                               xy(15, 9);
	//芝诺的龟螺旋（5阶投掷武器），绘制于9行14列，15×16
	public static final int ZENOS_TURTLE_SPIRAL =                           xy(14, 9);
	//锋锐骨刺（环指大师初始投掷武器），绘制于9行16列，10×11
	public static final int SHARP_BONE_SPIKE =                              xy(16, 9);
	//葬花楔（四阶E.G.O投掷武器，2026-09-09 用户新武器，绘制于9行12列，16×16）
	public static final int BURYING_WEDGE =                                  xy(12, 9);
	//新星之声（五阶E.G.O投掷武器，2026-09-09 用户新武器，绘制于9行13列，13×13）
	public static final int NOVA_VOICE =                                     xy(13, 9);
	//CENSORED 测试武器（9行9列，36×9 长条形贴图；ItemSlot.item() 中检测并自动旋转 -45°）
	public static final int CENSORED_WEAPON =                               xy(9, 9);
	//死亡证明（37行14列，19×21，陈列室传送道具）
	public static final int DEATH_CERTIFICATE =                             xy(14, 37);
	static{
		assignItemRect(GREATSWORD,  16, 16);
		assignItemRect(WAR_HAMMER,  16, 16);
		assignItemRect(GLAIVE,      16, 16);
		assignItemRect(GREATAXE,    12, 16);
		assignItemRect(GREATSHIELD, 12, 16);
		assignItemRect(GAUNTLETS,   13, 15);
		assignItemRect(WAR_SCYTHE,  14, 15);
		//《是谁嗯嗯在我的头上》绘本（9行15列，13×12）
		assignItemRect(POOP_STORY_BOOK, 13, 12);
		//锋锐骨刺（9行16列，10×11）
		assignItemRect(SHARP_BONE_SPIKE, 10, 11);
		//芝诺的龟螺旋（9行14列，15×16）
		assignItemRect(ZENOS_TURTLE_SPIRAL, 15, 16);
		//葬花楔（9行12列，16×16）
		assignItemRect(BURYING_WEDGE, 16, 16);
		//新星之声（9行13列，13×13）
		assignItemRect(NOVA_VOICE, 13, 13);
		//CENSORED 测试武器（9行9列，36×9 长条形贴图）
		assignItemRect(CENSORED_WEAPON, 36, 9);
		//死亡证明（37行14列，19×21）
		assignItemRect(DEATH_CERTIFICATE, 19, 21);
	}

	//custom debug test-weapon icon, drawn on the 33rd row after extending items.png to 528px tall
	//the art is 12x16 at pixels (0,512)-(11,527); reading just 12x16 (like the Gloves icon) lets the
	//item sprite center it in the 16-pixel slot, which shifts the display ~2px to the right.
	public static final int TEST_WEAPON     =                               xy(1, 33);
	static{
		assignItemRect(TEST_WEAPON,    12, 16);
	}

	//nine alternate-form icons of the morph weapon, drawn on the 33rd row,
	//one per 16x16 cell (cells 1-9), each art aligned to its cell's top-left corner.
	public static final int DURANDAL         =                              xy(2, 33);
	public static final int ELM_WORKSHOP     =                              xy(3, 33);
	public static final int LANGYA_WORKSHOP  =                              xy(4, 33);
	public static final int OLDBOY_WORKSHOP  =                              xy(5, 33);
	public static final int ARAS_WORKSHOP    =                              xy(6, 33);
	public static final int INK_WORKSHOP     =                              xy(7, 33);
	public static final int LOGIC_STUDIO     =                              xy(8, 33);
	public static final int CALISTA_STUDIO   =                              xy(9, 33);
	public static final int ROULETTE_HEAVY   =                              xy(10, 33);
	//Hermes' caduceus, drawn on the 33rd row in the cell right of Roulette Heavy
	public static final int HERMES_CADUCEUS =                               xy(11, 33);
	//Instruction terminal, drawn on the 34th row, first cell (right below the morph weapon's base form)
	public static final int INSTRUCTION_TERMINAL =                          xy(1, 34);
	//Instruction target mark, drawn on the 34th row, right of the instruction terminal
	public static final int TARGET_MARK =                                   xy(2, 34);
	//Blessing mark (祝福), drawn at pixel region (32,560)-(47,575), 15x15
	public static final int BLESSING_MARK =                                  xy(3, 36);
	static{
		assignItemRect(DURANDAL,        15, 15);
		assignItemRect(ELM_WORKSHOP,    16, 15);
		assignItemRect(LANGYA_WORKSHOP, 14, 15);
		assignItemRect(OLDBOY_WORKSHOP, 16, 16);
		assignItemRect(ARAS_WORKSHOP,   16, 16);
		assignItemRect(INK_WORKSHOP,    15, 16);
		assignItemRect(LOGIC_STUDIO,    15, 15);
		assignItemRect(CALISTA_STUDIO,  16, 16);
		assignItemRect(ROULETTE_HEAVY,  16, 16);
		assignItemRect(HERMES_CADUCEUS, 14, 16);
		assignItemRect(INSTRUCTION_TERMINAL, 16, 14);
		assignItemRect(TARGET_MARK, 16, 16);
		//祝福标记：像素区域 (32,560)-(47,575)，16x16
		assignItemRect(BLESSING_MARK, 16, 16);
	}

	//十四件短剑复刻测试武器（占位，属性同短剑，仅名称/图标不同），图标绘制于33/34行。
	//33行：第12-16个（第13个原提比娅已废弃移除）
	public static final int PALERMO_SWORD     =                            xy(12, 33);
	public static final int DAMP_SICKLE       =                            xy(14, 33);
	public static final int COMET_KNIFE       =                            xy(15, 33);
	public static final int COMET_KNIFE_DRAWN =                            xy(16, 33);
	//34行：第5-13个与第15个（第14个留空）
	public static final int SEALED_SWORD      =                            xy(5, 34);
	public static final int UNSEALED_SWORD    =                            xy(6, 34);
	public static final int UNSEALED_SWORD_2  =                            xy(7, 34);
	public static final int LAEVATEINN        =                            xy(8, 34);
	public static final int MIMICRY           =                            xy(9, 34);
	public static final int SMILING_BLADE     =                            xy(10, 34);
	public static final int DA_CAPO           =                            xy(11, 34);
	public static final int HOLY_DECREE       =                            xy(12, 34);
	//薄暝与失乐园为超出16x16的超大贴图（20x20 / 21x20）
	public static final int DIM_DUSK          =                            xy(13, 34);
	public static final int PARADISE_LOST     =                            xy(15, 34);
	//胎儿博士（新四阶武器，绘制于35行第1个——指令终端贴图的下一行第一个）
	public static final int FETAL_DOCTOR      =                            xy(1, 35);
	//余香/黑天鹅/猩红创痕（四阶新武器，绘制于35行第3/4/5个）
	public static final int LINGERING_SCENT   =                            xy(3, 35);
	public static final int BLACK_SWAN        =                            xy(4, 35);
	public static final int CRIMSON_SCAR      =                            xy(5, 35);
	//血之渴望/霜之碎片/忘却/小小银河（三阶新武器，绘制于35行第6/7/8/9个）
	public static final int BLOOD_THIRST      =                            xy(6, 35);
	public static final int FROST_SHARD       =                            xy(7, 35);
	public static final int OBLIVION          =                            xy(8, 35);
	public static final int TINY_GALAXY       =                            xy(9, 35);
	//正义裁决者（五阶新武器，绘制于35行第10个）
	public static final int JUSTICE_ARBITER   =                            xy(10, 35);
	//发光的鹅卵石（小小银河的击杀掉落物，绘制于32行第8个）
	public static final int GLOWING_PEBBLE    =                            xy(8, 32);
	//赤瞳/终末之光（二阶新武器，绘制于35行第11/12个）
	public static final int RED_EYE           =                            xy(11, 35);
	public static final int LAST_LIGHT        =                            xy(12, 35);
	//割腕/悔恨（二阶新武器，绘制于36行第1/2个）
	public static final int WRIST_SLIT        =                            xy(1, 36);
	public static final int REMORSE           =                            xy(2, 36);
	//试作-解体刀（环指大师1阶人体派作品武器，绘制于36行第4个，尺寸 12×13）
	public static final int TRIAL_DISSECTION_KNIFE =                            xy(4, 36);
	//大师指环（环指大师专属合成道具，绘制于36行第6个，尺寸 8×10）
	public static final int MASTER_RING       =                            xy(6, 36);
	//魔质素材（环指大师专属强化材料，绘制于36行第5个，尺寸 15×14；无数值、不可入创作，仅强化用）
	public static final int MAGIC_MATERIAL    =                            xy(5, 36);
	//钻石剑（四阶武器，2026-09-04 MC 彩蛋，绘制于36行第12个，尺寸 16×16）
	public static final int DIAMOND_SWORD     =                            xy(12, 36);
	//虚无衍射体（四阶武器，2026-09-05，绘制于36行第13个，尺寸 16×16；低伤害+必偷袭）
	public static final int VOID_DIFFRACTION  =                            xy(13, 36);
	//拘束/盲目/渴望（四阶E.G.O武器，2026-09-06，绘制于36行第14/15/16个）
	public static final int BINDING           =                            xy(14, 36);
	public static final int BLINDNESS         =                            xy(15, 36);
	public static final int THIRST            =                            xy(16, 36);
	//磨损对剑（一阶近战，2026-09-07 用户新武器，绘制于36行第7个，尺寸 14×15）
	public static final int WORN_TWIN_SWORD  =                            xy(7, 36);
	//空酒瓶（一阶投掷，2026-09-07 用户新武器，绘制于36行第8个，尺寸 16×16）
	public static final int EMPTY_BOTTLE     =                            xy(8, 36);
	//沉默的代价（饰物，2026-09-08 用户新建，绘制于19行第2个，尺寸 13×13）
	public static final int SILENT_PRICE     =                            xy(2, 19);
	//意志之刃（四阶近战，2026-09-07 用户新武器，绘制于37行第6个，尺寸 16×16）
	public static final int WILL_BLADE       =                            xy(6, 37);
	//烙印工坊（三阶近战，2026-09-08 用户新武器，绘制于37行第7个，尺寸 16×16）
	public static final int BRAND_WORKSHOP   =                            xy(7, 37);
	//蜡翼（五阶E.G.O，2026-09-08 用户新武器，绘制于37行第8个，尺寸 16×16）
	public static final int WAX_WING         =                            xy(8, 37);
	//传火大剑（六阶近战，用户自建，绘制于37行第12个，24×24 超出单格属预期）
	public static final int FIRELINK_GREATSWORD =                         xy(12, 37);
	//し协会的暗杀刃（四阶近战，2026-09-09 用户新武器，绘制于32行第13个，尺寸 15×16；偷袭+最大伤害爆发）
	public static final int SHI_ASSASSINS_BLADE =                         xy(13, 32);
	//人体派素材（环指大师合成材料，37/38行第1/2个）
	public static final int HARD_MATERIAL     =                             xy(1, 37);
	public static final int BONE_MATERIAL      =                            xy(2, 37);
	public static final int MEAT_MATERIAL      =                            xy(1, 38);
	public static final int BLOOD_MATERIAL     =                            xy(2, 38);
	//人体派作品（环指大师专属武器，绘制于37-40行）
	//37行：劣作（二阶），第3/4/5个
	public static final int CRUDE_WORK_1      =                            xy(3, 37);
	public static final int CRUDE_WORK_2      =                            xy(4, 37);
	public static final int CRUDE_WORK_3      =                            xy(5, 37);
	//38行：凡作（三阶），第3/4/5个
	public static final int COMMON_WORK_1     =                            xy(3, 38);
	public static final int COMMON_WORK_2     =                            xy(4, 38);
	public static final int COMMON_WORK_3     =                            xy(5, 38);
	//39行：良作（四阶），第3/4/5个；第1个为一生之作-提比娅（六阶，31x30超大贴图）
	public static final int LIFEWORK_TIBIA    =                            xy(1, 39);
	public static final int FINE_WORK_1       =                            xy(3, 39);
	public static final int FINE_WORK_2       =                            xy(4, 39);
	public static final int FINE_WORK_3       =                            xy(5, 39);
	//40行：名作（五阶），第3/4/5个
	public static final int FAMED_WORK_1      =                            xy(3, 40);
	public static final int FAMED_WORK_2      =                            xy(4, 40);
	public static final int FAMED_WORK_3      =                            xy(5, 40);
	//焦炭松脂/散装焦炭松脂/黄金松脂/散装黄金松脂（炼金消耗品，2026-09-08 用户新道具，绘制于40行第6-9个）
	//整块松脂 16×16，散装松脂 16×15
	public static final int CHARCOAL_RESIN       =                         xy(6, 40);
	public static final int LOOSE_CHARCOAL_RESIN =                         xy(7, 40);
	public static final int GOLDEN_RESIN         =                         xy(8, 40);
	public static final int LOOSE_GOLDEN_RESIN   =                         xy(9, 40);
	//指虎（一阶近战快武器，2026-09-13 用户新武器，绘制于扩展预留区 41 行第 2 个，尺寸 15×10）
	public static final int KNUCKLE_DUSTER       =                         xy(2, 41);
	static{
		assignItemRect(KNUCKLE_DUSTER,    15, 10);
	}
	static{
		assignItemRect(PALERMO_SWORD,     16, 16);
		assignItemRect(DAMP_SICKLE,       15, 15);
		assignItemRect(COMET_KNIFE,       16, 16);
		assignItemRect(COMET_KNIFE_DRAWN, 16, 16);
		assignItemRect(SEALED_SWORD,      15, 16);
		assignItemRect(UNSEALED_SWORD,    15, 16);
		assignItemRect(UNSEALED_SWORD_2,  15, 15);
		assignItemRect(LAEVATEINN,        16, 14);
		assignItemRect(MIMICRY,           16, 16);
		assignItemRect(SMILING_BLADE,     16, 15);
		assignItemRect(DA_CAPO,           14, 16);
		assignItemRect(HOLY_DECREE,       15, 15);
		assignItemRect(DIM_DUSK,          20, 20);
		assignItemRect(PARADISE_LOST,     21, 20);
		assignItemRect(FETAL_DOCTOR,      11, 16);
		assignItemRect(LINGERING_SCENT,   15, 15);
		assignItemRect(BLACK_SWAN,        15, 16);
		assignItemRect(CRIMSON_SCAR,      13, 14);
		assignItemRect(BLOOD_THIRST,      11, 14);
		assignItemRect(FROST_SHARD,       16, 16);
		assignItemRect(OBLIVION,          13, 12);
		assignItemRect(TINY_GALAXY,       15, 15);
		assignItemRect(JUSTICE_ARBITER,   16, 16);
		assignItemRect(GLOWING_PEBBLE,    7,  7);
		assignItemRect(RED_EYE,           16, 16);
		assignItemRect(LAST_LIGHT,        16, 16);
		assignItemRect(WRIST_SLIT,        11, 12);
		assignItemRect(REMORSE,           16, 16);
		assignItemRect(TRIAL_DISSECTION_KNIFE, 12, 13);
		assignItemRect(MASTER_RING,        8, 10);
		assignItemRect(DIAMOND_SWORD,     16, 16);
		assignItemRect(VOID_DIFFRACTION,  16, 16);
		assignItemRect(BINDING,           16, 16);
		assignItemRect(BLINDNESS,         16, 16);
		assignItemRect(THIRST,            16, 16);
		assignItemRect(WORN_TWIN_SWORD,   14, 15);
		assignItemRect(EMPTY_BOTTLE,      16, 16);
		assignItemRect(SILENT_PRICE,      13, 13);
		assignItemRect(WILL_BLADE,        16, 16);
		assignItemRect(BRAND_WORKSHOP,    14, 14);
		assignItemRect(WAX_WING,          15, 15);
		assignItemRect(HARD_MATERIAL,     13, 14);
		assignItemRect(BONE_MATERIAL,     16, 15);
		assignItemRect(MEAT_MATERIAL,     16, 15);
		assignItemRect(BLOOD_MATERIAL,    14, 16);
		assignItemRect(MAGIC_MATERIAL,    15, 14);
		assignItemRect(CRUDE_WORK_1,      14, 14);
		assignItemRect(CRUDE_WORK_2,      15, 15);
		assignItemRect(CRUDE_WORK_3,      16, 16);
		assignItemRect(COMMON_WORK_1,     14, 14);
		assignItemRect(COMMON_WORK_2,     13, 16);
		assignItemRect(COMMON_WORK_3,     16, 16);
		assignItemRect(FINE_WORK_1,       16, 16);
		assignItemRect(FINE_WORK_2,       14, 16);
		assignItemRect(FINE_WORK_3,       14, 14);
		assignItemRect(FAMED_WORK_1,      16, 16);
		assignItemRect(FAMED_WORK_2,      16, 16);
		assignItemRect(FAMED_WORK_3,      14, 15);
		assignItemRect(LIFEWORK_TIBIA,    31, 30);
		assignItemRect(FIRELINK_GREATSWORD, 24, 24);
		assignItemRect(SHI_ASSASSINS_BLADE, 15, 16);
		assignItemRect(CHARCOAL_RESIN,      16, 16);
		assignItemRect(LOOSE_CHARCOAL_RESIN,16, 15);
		assignItemRect(GOLDEN_RESIN,        16, 16);
		assignItemRect(LOOSE_GOLDEN_RESIN,  16, 15);
	}

	                                                                                    //8 free slots

	private static final int MISSILE_WEP    =                               xy(1, 10);  //16 slots. 3 per tier + bow
	public static final int SPIRIT_BOW      = MISSILE_WEP+0;
	
	public static final int THROWING_SPIKE  = MISSILE_WEP+1;
	public static final int THROWING_KNIFE  = MISSILE_WEP+2;
	public static final int THROWING_STONE  = MISSILE_WEP+3;
	
	public static final int FISHING_SPEAR   = MISSILE_WEP+4;
	public static final int SHURIKEN        = MISSILE_WEP+5;
	public static final int THROWING_CLUB   = MISSILE_WEP+6;
	
	public static final int THROWING_SPEAR  = MISSILE_WEP+7;
	public static final int BOLAS           = MISSILE_WEP+8;
	public static final int KUNAI           = MISSILE_WEP+9;
	
	public static final int JAVELIN         = MISSILE_WEP+10;
	public static final int TOMAHAWK        = MISSILE_WEP+11;
	public static final int BOOMERANG       = MISSILE_WEP+12;
	
	public static final int TRIDENT         = MISSILE_WEP+13;
	public static final int THROWING_HAMMER = MISSILE_WEP+14;
	public static final int FORCE_CUBE      = MISSILE_WEP+15;
	
	static{
		assignItemRect(SPIRIT_BOW,      16, 16);
		
		assignItemRect(THROWING_SPIKE,  11, 10);
		assignItemRect(THROWING_KNIFE,  12, 13);
		assignItemRect(THROWING_STONE,  12, 10);
		
		assignItemRect(FISHING_SPEAR,   11, 11);
		assignItemRect(SHURIKEN,        12, 12);
		assignItemRect(THROWING_CLUB,   12, 12);
		
		assignItemRect(THROWING_SPEAR,  13, 13);
		assignItemRect(BOLAS,           15, 14);
		assignItemRect(KUNAI,           15, 15);
		
		assignItemRect(JAVELIN,         16, 16);
		assignItemRect(TOMAHAWK,        13, 13);
		assignItemRect(BOOMERANG,       14, 14);
		
		assignItemRect(TRIDENT,         16, 16);
		assignItemRect(THROWING_HAMMER, 12, 12);
		assignItemRect(FORCE_CUBE,      11, 12);
	}
	
	public static final int DARTS    =                                      xy(1, 11);  //16 slots
	public static final int DART            = DARTS+0;
	public static final int ROT_DART        = DARTS+1;
	public static final int INCENDIARY_DART = DARTS+2;
	public static final int ADRENALINE_DART = DARTS+3;
	public static final int HEALING_DART    = DARTS+4;
	public static final int CHILLING_DART   = DARTS+5;
	public static final int SHOCKING_DART   = DARTS+6;
	public static final int POISON_DART     = DARTS+7;
	public static final int CLEANSING_DART  = DARTS+8;
	public static final int PARALYTIC_DART  = DARTS+9;
	public static final int HOLY_DART       = DARTS+10;
	public static final int DISPLACING_DART = DARTS+11;
	public static final int BLINDING_DART   = DARTS+12;
	static {
		for (int i = DARTS; i < DARTS+16; i++)
			assignItemRect(i, 15, 15);
	}
	
	private static final int ARMOR          =                               xy(1, 12);  //16 slots
	public static final int ARMOR_CLOTH     = ARMOR+0;
	public static final int ARMOR_LEATHER   = ARMOR+1;
	public static final int ARMOR_MAIL      = ARMOR+2;
	public static final int ARMOR_SCALE     = ARMOR+3;
	public static final int ARMOR_PLATE     = ARMOR+4;
	public static final int ARMOR_WARRIOR   = ARMOR+5;
	public static final int ARMOR_MAGE      = ARMOR+6;
	public static final int ARMOR_ROGUE     = ARMOR+7;
	public static final int ARMOR_HUNTRESS  = ARMOR+8;
	public static final int ARMOR_DUELIST   = ARMOR+9;
	public static final int ARMOR_CLERIC    = ARMOR+10;
	public static final int ARMOR_ORACLE    = ARMOR+11; //食指西服（第12行第12个）
	public static final int ARMOR_RINGMASTER= ARMOR+12; //环指长袍（第12行第13个）
	public static final int ARMOR_VALENCINA = ARMOR+13; //拇指大衣（第12行第14个，2026-09-05 新增：拇指 前二老板职业护甲）
	public static final int ARMOR_MIDDLE_FINGER = ARMOR+14; //中指外套（第12行第15个，2026-09-13 新增：中指 长兄职业护甲）
	static{
		assignItemRect(ARMOR_CLOTH,     15, 12);
		assignItemRect(ARMOR_LEATHER,   14, 13);
		assignItemRect(ARMOR_MAIL,      14, 12);
		assignItemRect(ARMOR_SCALE,     14, 11);
		assignItemRect(ARMOR_PLATE,     12, 12);
		assignItemRect(ARMOR_WARRIOR,   12, 12);
		assignItemRect(ARMOR_MAGE,      15, 15);
		assignItemRect(ARMOR_ROGUE,     14, 12);
		assignItemRect(ARMOR_HUNTRESS,  13, 15);
		assignItemRect(ARMOR_DUELIST,   12, 13);
		assignItemRect(ARMOR_CLERIC,    13, 14);
		assignItemRect(ARMOR_ORACLE,    13, 13);
		assignItemRect(ARMOR_RINGMASTER,12, 16);
		assignItemRect(ARMOR_VALENCINA, 14, 16); //拇指大衣 第12行第14个，14×16
		assignItemRect(ARMOR_MIDDLE_FINGER, 16, 16); //中指外套 第12行第15个，16×16
	}

	                                                                                    //16 free slots

	private static final int WANDS              =                           xy(1, 14);  //16 slots
	public static final int WAND_MAGIC_MISSILE  = WANDS+0;
	public static final int WAND_FIREBOLT       = WANDS+1;
	public static final int WAND_FROST          = WANDS+2;
	public static final int WAND_LIGHTNING      = WANDS+3;
	public static final int WAND_DISINTEGRATION = WANDS+4;
	public static final int WAND_PRISMATIC_LIGHT= WANDS+5;
	public static final int WAND_CORROSION      = WANDS+6;
	public static final int WAND_LIVING_EARTH   = WANDS+7;
	public static final int WAND_BLAST_WAVE     = WANDS+8;
	public static final int WAND_CORRUPTION     = WANDS+9;
	public static final int WAND_WARDING        = WANDS+10;
	public static final int WAND_REGROWTH       = WANDS+11;
	public static final int WAND_TRANSFUSION    = WANDS+12;
	//旭日之心（新法杖，绘制于14行第14个）
	public static final int WAND_BLAZING_HEART  = WANDS+13;
	static {
		for (int i = WANDS; i < WANDS+16; i++)
			assignItemRect(i, 14, 14);
		//烈日之心的贴图为 16×16（覆盖默认 14×14）
		assignItemRect(WAND_BLAZING_HEART, 16, 16);
	}

	private static final int RINGS          =                               xy(1, 15);  //16 slots
	public static final int RING_GARNET     = RINGS+0;
	public static final int RING_RUBY       = RINGS+1;
	public static final int RING_TOPAZ      = RINGS+2;
	public static final int RING_EMERALD    = RINGS+3;
	public static final int RING_ONYX       = RINGS+4;
	public static final int RING_OPAL       = RINGS+5;
	public static final int RING_TOURMALINE = RINGS+6;
	public static final int RING_SAPPHIRE   = RINGS+7;
	public static final int RING_AMETHYST   = RINGS+8;
	public static final int RING_QUARTZ     = RINGS+9;
	public static final int RING_AGATE      = RINGS+10;
	public static final int RING_DIAMOND    = RINGS+11;
	static {
		for (int i = RINGS; i < RINGS+16; i++)
			assignItemRect(i, 8, 10);
	}

	private static final int ARTIFACTS          =                            xy(1, 16);  //32 slots
	public static final int ARTIFACT_CLOAK      = ARTIFACTS+0;
	public static final int ARTIFACT_ARMBAND    = ARTIFACTS+1;
	public static final int ARTIFACT_CAPE       = ARTIFACTS+2;
	public static final int ARTIFACT_TALISMAN   = ARTIFACTS+3;
	public static final int ARTIFACT_HOURGLASS  = ARTIFACTS+4;
	public static final int ARTIFACT_TOOLKIT    = ARTIFACTS+5;
	public static final int ARTIFACT_SPELLBOOK  = ARTIFACTS+6;
	public static final int ARTIFACT_BEACON     = ARTIFACTS+7;
	public static final int ARTIFACT_CHAINS     = ARTIFACTS+8;
	public static final int ARTIFACT_HORN1      = ARTIFACTS+9;
	public static final int ARTIFACT_HORN2      = ARTIFACTS+10;
	public static final int ARTIFACT_HORN3      = ARTIFACTS+11;
	public static final int ARTIFACT_HORN4      = ARTIFACTS+12;
	public static final int ARTIFACT_CHALICE1   = ARTIFACTS+13;
	public static final int ARTIFACT_CHALICE2   = ARTIFACTS+14;
	public static final int ARTIFACT_CHALICE3   = ARTIFACTS+15;
	public static final int ARTIFACT_SANDALS    = ARTIFACTS+16;
	public static final int ARTIFACT_SHOES      = ARTIFACTS+17;
	public static final int ARTIFACT_BOOTS      = ARTIFACTS+18;
	public static final int ARTIFACT_GREAVES    = ARTIFACTS+19;
	public static final int ARTIFACT_ROSE1      = ARTIFACTS+20;
	public static final int ARTIFACT_ROSE2      = ARTIFACTS+21;
	public static final int ARTIFACT_ROSE3      = ARTIFACTS+22;
	public static final int ARTIFACT_TOME       = ARTIFACTS+23;
	public static final int ARTIFACT_KEY        = ARTIFACTS+24;
	//爱慕（自定义神器，2026-09-05）：四档充能贴图，绘制于 29 行第 11~14 列（16×16，用户已绘）
	//注意：29 行为 HOLIDAY_FOOD 区（+0~+9 已用），本组占用其空余的 +10~+13 格；日后若新增节日食物需避开
	public static final int ARTIFACT_ADMIRATION1 =                            xy(11, 29);
	public static final int ARTIFACT_ADMIRATION2 =                            xy(12, 29);
	public static final int ARTIFACT_ADMIRATION3 =                            xy(13, 29);
	public static final int ARTIFACT_ADMIRATION4 =                            xy(14, 29);
	//奥丁之眼（自定义神器，2026-09-06）：常态 xy(3, 34)、过热形态 xy(4, 34)（16×16，用户已绘）
	//注意：34 行 1/2 列=INSTRUCTION_TERMINAL/TARGET_MARK，5~13/15 列=武器区，本组占用 3/4 列空位
	public static final int ARTIFACT_ODINS_EYE         =                            xy(3, 34);
	public static final int ARTIFACT_ODINS_EYE_OVERHEAT =                            xy(4, 34);
	//泪锋的加护（自定义神器，2026-09-12）：加护形态 xy(7, 39)、绝望形态 xy(8, 39)（14×16，用户已绘）
	//注意：39 行属「向下扩展预留区」（行 36~40），本组占用 7/8 列空位；日后新增该行贴图需避开这两格
	public static final int ARTIFACT_TEAR_BLESSING =                            xy(7, 39);
	public static final int ARTIFACT_TEAR_DESPAIR  =                            xy(8, 39);
	//复仇账簿（自定义神器，2026-09-13）：xy(1, 41)，13×16（用户已绘，位于扩展预留区 41 行第 1 个）
	public static final int ARTIFACT_REVENGE_LEDGER =                            xy(1, 41);
	//水仙十字圣剑（自定义二阶武器，2026-09-20）：三形态各占一格，均 14×14（用户已绘）
	//常态 xy(3, 41)、芒性 xy(4, 41)、荒性 xy(5, 41)；41 行 1/2 列已被复仇账簿与指虎占用，本组接在 3~5 列
	public static final int NARCISSUS_CROSS_SWORD     =                          xy(3, 41);
	public static final int NARCISSUS_SWORD_MANG      =                          xy(4, 41);
	public static final int NARCISSUS_SWORD_HUANG     =                          xy(5, 41);
	//次元撕裂者（三阶自定义武器，2026-09-22）：xy(6, 41)，13×15（用户已绘，紧接水仙三形态之后）
	public static final int DIMENSIONAL_RIPPER        =                          xy(6, 41);
	//洛伊德护符（自定义投掷消耗品，2026-09-24）：xy(7, 41)，14×16（用户已绘，紧接次元撕裂者之后）
	public static final int LLOYD_TALISMAN            =                          xy(7, 41);
	//神圣卡（自定义消耗品，2026-09-24）：xy(8, 41)，15×15（用户已绘，紧接洛伊德护符之后）
	public static final int HOLY_CARD                 =                          xy(8, 41);
	//「原版神器的强化形态」五件（2026-09-20）：整行 42 行此前完全空闲，本组占用 1~10 列（用户已绘）
	//注意：帧数与原版一一对应 —— 圣杯 3 帧（按等级）、炖菜 4 帧（按充能），其余各 1 帧
	public static final int ARTIFACT_BLOOD_FEAST_CHALICE1   =                    xy(1, 42);
	public static final int ARTIFACT_BLOOD_FEAST_CHALICE2   =                    xy(2, 42);
	public static final int ARTIFACT_BLOOD_FEAST_CHALICE3   =                    xy(3, 42);
	public static final int ARTIFACT_LIFELONG_STEW1         =                    xy(4, 42);
	public static final int ARTIFACT_LIFELONG_STEW2         =                    xy(5, 42);
	public static final int ARTIFACT_LIFELONG_STEW3         =                    xy(6, 42);
	public static final int ARTIFACT_LIFELONG_STEW4         =                    xy(7, 42);
	public static final int ARTIFACT_CHAIN_OF_OTHERS        =                    xy(8, 42);
	public static final int ARTIFACT_CHAPTER_NINE_VERSE_TWO =                    xy(9, 42);
	public static final int ARTIFACT_APPROACHING_DAY        =                    xy(10, 42);
	static{
		assignItemRect(ARTIFACT_REVENGE_LEDGER,     13, 16);
		assignItemRect(NARCISSUS_CROSS_SWORD,       14, 14);
		assignItemRect(NARCISSUS_SWORD_MANG,        14, 14);
		assignItemRect(NARCISSUS_SWORD_HUANG,       14, 14);
		assignItemRect(DIMENSIONAL_RIPPER,          13, 15);
		assignItemRect(LLOYD_TALISMAN,              14, 16);
		assignItemRect(HOLY_CARD,                   15, 15);
		//原版神器强化形态（42 行，1~10 列）
		assignItemRect(ARTIFACT_BLOOD_FEAST_CHALICE1,   12, 15);
		assignItemRect(ARTIFACT_BLOOD_FEAST_CHALICE2,   12, 15);
		assignItemRect(ARTIFACT_BLOOD_FEAST_CHALICE3,   12, 15);
		assignItemRect(ARTIFACT_LIFELONG_STEW1,         15, 16);
		assignItemRect(ARTIFACT_LIFELONG_STEW2,         15, 16);
		assignItemRect(ARTIFACT_LIFELONG_STEW3,         15, 16);
		assignItemRect(ARTIFACT_LIFELONG_STEW4,         15, 16);
		assignItemRect(ARTIFACT_CHAIN_OF_OTHERS,        15, 16);
		assignItemRect(ARTIFACT_CHAPTER_NINE_VERSE_TWO, 13, 16);
		assignItemRect(ARTIFACT_APPROACHING_DAY,        16, 16);
	}
	static{
		assignItemRect(ARTIFACT_CLOAK,      9,  15);
		assignItemRect(ARTIFACT_ARMBAND,    16, 13);
		assignItemRect(ARTIFACT_CAPE,       16, 14);
		assignItemRect(ARTIFACT_TALISMAN,   15, 13);
		assignItemRect(ARTIFACT_HOURGLASS,  13, 16);
		assignItemRect(ARTIFACT_TOOLKIT,    15, 13);
		assignItemRect(ARTIFACT_SPELLBOOK,  13, 16);
		assignItemRect(ARTIFACT_BEACON,     16, 16);
		assignItemRect(ARTIFACT_CHAINS,     16, 16);
		assignItemRect(ARTIFACT_HORN1,      15, 15);
		assignItemRect(ARTIFACT_HORN2,      15, 15);
		assignItemRect(ARTIFACT_HORN3,      15, 15);
		assignItemRect(ARTIFACT_HORN4,      15, 15);
		assignItemRect(ARTIFACT_CHALICE1,   12, 15);
		assignItemRect(ARTIFACT_CHALICE2,   12, 15);
		assignItemRect(ARTIFACT_CHALICE3,   12, 15);
		assignItemRect(ARTIFACT_SANDALS,    16, 6 );
		assignItemRect(ARTIFACT_SHOES,      16, 6 );
		assignItemRect(ARTIFACT_BOOTS,      16, 9 );
		assignItemRect(ARTIFACT_GREAVES,    16, 14);
		assignItemRect(ARTIFACT_ROSE1,      14, 14);
		assignItemRect(ARTIFACT_ROSE2,      14, 14);
		assignItemRect(ARTIFACT_ROSE3,      14, 14);
		assignItemRect(ARTIFACT_TOME,       14, 16);
		assignItemRect(ARTIFACT_KEY,        8,  16);
		assignItemRect(ARTIFACT_ADMIRATION1,10, 9);
		assignItemRect(ARTIFACT_ADMIRATION2,11, 11);
		assignItemRect(ARTIFACT_ADMIRATION3,13, 13);
		assignItemRect(ARTIFACT_ADMIRATION4,15, 15);
		assignItemRect(ARTIFACT_ODINS_EYE,          16, 16);
		assignItemRect(ARTIFACT_ODINS_EYE_OVERHEAT, 16, 16);
		assignItemRect(ARTIFACT_TEAR_BLESSING,      14, 16);
		assignItemRect(ARTIFACT_TEAR_DESPAIR,       14, 16);
	}

	private static final int TRINKETS        =                               xy(1, 18);  //32 slots
	public static final int RAT_SKULL       = TRINKETS+0;
	public static final int PARCHMENT_SCRAP = TRINKETS+1;
	public static final int PETRIFIED_SEED  = TRINKETS+2;
	public static final int EXOTIC_CRYSTALS = TRINKETS+3;
	public static final int MOSSY_CLUMP     = TRINKETS+4;
	public static final int SUNDIAL         = TRINKETS+5;
	public static final int CLOVER          = TRINKETS+6;
	public static final int TRAP_MECHANISM  = TRINKETS+7;
	public static final int MIMIC_TOOTH     = TRINKETS+8;
	public static final int WONDROUS_RESIN  = TRINKETS+9;
	public static final int EYE_OF_NEWT     = TRINKETS+10;
	public static final int SALT_CUBE       = TRINKETS+11;
	public static final int BLOOD_VIAL      = TRINKETS+12;
	public static final int OBLIVION_SHARD  = TRINKETS+13;
	public static final int CHAOTIC_CENSER  = TRINKETS+14;
	public static final int FERRET_TUFT     = TRINKETS+15;
	public static final int SPYGLASS        = TRINKETS+16;
	static{
		assignItemRect(RAT_SKULL,       16, 11);
		assignItemRect(PARCHMENT_SCRAP, 10, 14);
		assignItemRect(PETRIFIED_SEED,  10, 10);
		assignItemRect(EXOTIC_CRYSTALS, 14, 13);
		assignItemRect(MOSSY_CLUMP,     12, 11);
		assignItemRect(SUNDIAL,         16, 12);
		assignItemRect(CLOVER,          11, 15);
		assignItemRect(TRAP_MECHANISM,  13, 15);
		assignItemRect(MIMIC_TOOTH,     8,  15);
		assignItemRect(WONDROUS_RESIN,  12, 11);
		assignItemRect(EYE_OF_NEWT,     12, 12);
		assignItemRect(SALT_CUBE,       12, 13);
		assignItemRect(BLOOD_VIAL,      6,  15);
		assignItemRect(OBLIVION_SHARD,  7,  14);
		assignItemRect(CHAOTIC_CENSER,  13, 15);
		assignItemRect(FERRET_TUFT,     16, 15);
		assignItemRect(SPYGLASS,        15, 15);
	}

	private static final int SCROLLS        =                               xy(1, 20);  //16 slots
	public static final int SCROLL_KAUNAN   = SCROLLS+0;
	public static final int SCROLL_SOWILO   = SCROLLS+1;
	public static final int SCROLL_LAGUZ    = SCROLLS+2;
	public static final int SCROLL_YNGVI    = SCROLLS+3;
	public static final int SCROLL_GYFU     = SCROLLS+4;
	public static final int SCROLL_RAIDO    = SCROLLS+5;
	public static final int SCROLL_ISAZ     = SCROLLS+6;
	public static final int SCROLL_MANNAZ   = SCROLLS+7;
	public static final int SCROLL_NAUDIZ   = SCROLLS+8;
	public static final int SCROLL_BERKANAN = SCROLLS+9;
	public static final int SCROLL_ODAL     = SCROLLS+10;
	public static final int SCROLL_TIWAZ    = SCROLLS+11;

	public static final int ARCANE_RESIN    = SCROLLS+13;
	static {
		for (int i = SCROLLS; i < SCROLLS+16; i++)
			assignItemRect(i, 15, 14);
		assignItemRect(ARCANE_RESIN   , 12, 11);
	}
	
	private static final int EXOTIC_SCROLLS =                               xy(1, 21);  //16 slots
	public static final int EXOTIC_KAUNAN   = EXOTIC_SCROLLS+0;
	public static final int EXOTIC_SOWILO   = EXOTIC_SCROLLS+1;
	public static final int EXOTIC_LAGUZ    = EXOTIC_SCROLLS+2;
	public static final int EXOTIC_YNGVI    = EXOTIC_SCROLLS+3;
	public static final int EXOTIC_GYFU     = EXOTIC_SCROLLS+4;
	public static final int EXOTIC_RAIDO    = EXOTIC_SCROLLS+5;
	public static final int EXOTIC_ISAZ     = EXOTIC_SCROLLS+6;
	public static final int EXOTIC_MANNAZ   = EXOTIC_SCROLLS+7;
	public static final int EXOTIC_NAUDIZ   = EXOTIC_SCROLLS+8;
	public static final int EXOTIC_BERKANAN = EXOTIC_SCROLLS+9;
	public static final int EXOTIC_ODAL     = EXOTIC_SCROLLS+10;
	public static final int EXOTIC_TIWAZ    = EXOTIC_SCROLLS+11;
	static {
		for (int i = EXOTIC_SCROLLS; i < EXOTIC_SCROLLS+16; i++)
			assignItemRect(i, 15, 14);
	}
	
	private static final int STONES             =                           xy(1, 22);  //16 slots
	public static final int STONE_AGGRESSION    = STONES+0;
	public static final int STONE_AUGMENTATION  = STONES+1;
	public static final int STONE_FEAR          = STONES+2;
	public static final int STONE_BLAST         = STONES+3;
	public static final int STONE_BLINK         = STONES+4;
	public static final int STONE_CLAIRVOYANCE  = STONES+5;
	public static final int STONE_SLEEP         = STONES+6;
	public static final int STONE_DETECT        = STONES+7;
	public static final int STONE_ENCHANT       = STONES+8;
	public static final int STONE_FLOCK         = STONES+9;
	public static final int STONE_INTUITION     = STONES+10;
	public static final int STONE_SHOCK         = STONES+11;
	static {
		for (int i = STONES; i < STONES+16; i++)
			assignItemRect(i, 14, 12);
	}

	private static final int POTIONS        =                               xy(1, 23);  //16 slots
	public static final int POTION_CRIMSON  = POTIONS+0;
	public static final int POTION_AMBER    = POTIONS+1;
	public static final int POTION_GOLDEN   = POTIONS+2;
	public static final int POTION_JADE     = POTIONS+3;
	public static final int POTION_TURQUOISE= POTIONS+4;
	public static final int POTION_AZURE    = POTIONS+5;
	public static final int POTION_INDIGO   = POTIONS+6;
	public static final int POTION_MAGENTA  = POTIONS+7;
	public static final int POTION_BISTRE   = POTIONS+8;
	public static final int POTION_CHARCOAL = POTIONS+9;
	public static final int POTION_SILVER   = POTIONS+10;
	public static final int POTION_IVORY    = POTIONS+11;

	public static final int LIQUID_METAL    = POTIONS+13;
	static {
		for (int i = POTIONS; i < POTIONS+16; i++)
			assignItemRect(i, 12, 14);
		assignItemRect(LIQUID_METAL,    8, 15);
	}
	
	private static final int EXOTIC_POTIONS =                               xy(1, 24);  //16 slots
	public static final int EXOTIC_CRIMSON  = EXOTIC_POTIONS+0;
	public static final int EXOTIC_AMBER    = EXOTIC_POTIONS+1;
	public static final int EXOTIC_GOLDEN   = EXOTIC_POTIONS+2;
	public static final int EXOTIC_JADE     = EXOTIC_POTIONS+3;
	public static final int EXOTIC_TURQUOISE= EXOTIC_POTIONS+4;
	public static final int EXOTIC_AZURE    = EXOTIC_POTIONS+5;
	public static final int EXOTIC_INDIGO   = EXOTIC_POTIONS+6;
	public static final int EXOTIC_MAGENTA  = EXOTIC_POTIONS+7;
	public static final int EXOTIC_BISTRE   = EXOTIC_POTIONS+8;
	public static final int EXOTIC_CHARCOAL = EXOTIC_POTIONS+9;
	public static final int EXOTIC_SILVER   = EXOTIC_POTIONS+10;
	public static final int EXOTIC_IVORY    = EXOTIC_POTIONS+11;
	static {
		for (int i = EXOTIC_POTIONS; i < EXOTIC_POTIONS+16; i++)
			assignItemRect(i, 12, 13);
	}

	private static final int SEEDS              =                           xy(1, 25);  //16 slots
	public static final int SEED_ROTBERRY       = SEEDS+0;
	public static final int SEED_FIREBLOOM      = SEEDS+1;
	public static final int SEED_SWIFTTHISTLE   = SEEDS+2;
	public static final int SEED_SUNGRASS       = SEEDS+3;
	public static final int SEED_ICECAP         = SEEDS+4;
	public static final int SEED_STORMVINE      = SEEDS+5;
	public static final int SEED_SORROWMOSS     = SEEDS+6;
	public static final int SEED_MAGEROYAL = SEEDS+7;
	public static final int SEED_EARTHROOT      = SEEDS+8;
	public static final int SEED_STARFLOWER     = SEEDS+9;
	public static final int SEED_FADELEAF       = SEEDS+10;
	public static final int SEED_BLINDWEED      = SEEDS+11;
	static{
		for (int i = SEEDS; i < SEEDS+16; i++)
			assignItemRect(i, 10, 10);
	}
	
	private static final int BREWS          =                               xy(1, 26);  //8 slots
	public static final int BREW_INFERNAL   = BREWS+0;
	public static final int BREW_BLIZZARD   = BREWS+1;
	public static final int BREW_SHOCKING   = BREWS+2;
	public static final int BREW_CAUSTIC    = BREWS+3;
	public static final int BREW_AQUA       = BREWS+4;
	public static final int BREW_UNSTABLE   = BREWS+5;
	
	private static final int ELIXIRS        =                               xy(9, 26);  //8 slots
	public static final int ELIXIR_HONEY    = ELIXIRS+0;
	public static final int ELIXIR_AQUA     = ELIXIRS+1;
	public static final int ELIXIR_MIGHT    = ELIXIRS+2;
	public static final int ELIXIR_DRAGON   = ELIXIRS+3;
	public static final int ELIXIR_TOXIC    = ELIXIRS+4;
	public static final int ELIXIR_ICY      = ELIXIRS+5;
	public static final int ELIXIR_ARCANE   = ELIXIRS+6;
	public static final int ELIXIR_FEATHER  = ELIXIRS+7;
	static{
		for (int i = BREWS; i < BREWS+16; i++)
			assignItemRect(i, 12, 14);

		assignItemRect(BREW_AQUA, 9, 11);
	}
	
	private static final int SPELLS         =                               xy(1, 27);  //16 slots
	public static final int WILD_ENERGY     = SPELLS+0;
	public static final int PHASE_SHIFT     = SPELLS+1;
	public static final int TELE_GRAB       = SPELLS+2;
	public static final int UNSTABLE_SPELL  = SPELLS+3;

	public static final int CURSE_INFUSE    = SPELLS+5;
	public static final int MAGIC_INFUSE    = SPELLS+6;
	public static final int ALCHEMIZE       = SPELLS+7;
	public static final int RECYCLE         = SPELLS+8;

	public static final int RECLAIM_TRAP    = SPELLS+10;
	public static final int RETURN_BEACON   = SPELLS+11;
	public static final int SUMMON_ELE      = SPELLS+12;

	static{
		assignItemRect(WILD_ENERGY,     12, 11);
		assignItemRect(PHASE_SHIFT,     12, 11);
		assignItemRect(TELE_GRAB,       12, 11);
		assignItemRect(UNSTABLE_SPELL,  12, 13);

		assignItemRect(CURSE_INFUSE,    10, 15);
		assignItemRect(MAGIC_INFUSE,    10, 15);
		assignItemRect(ALCHEMIZE,       10, 15);
		assignItemRect(RECYCLE,         10, 15);

		assignItemRect(RECLAIM_TRAP,     8, 16);
		assignItemRect(RETURN_BEACON,    8, 16);
		assignItemRect(SUMMON_ELE,       8, 16);
	}
	
	private static final int FOOD       =                                   xy(1, 28);  //16 slots
	public static final int MEAT            = FOOD+0;
	public static final int STEAK           = FOOD+1;
	public static final int STEWED          = FOOD+2;
	public static final int OVERPRICED      = FOOD+3;
	public static final int CARPACCIO       = FOOD+4;
	public static final int RATION          = FOOD+5;
	public static final int PASTY           = FOOD+6;
	public static final int MEAT_PIE        = FOOD+7;
	public static final int BLANDFRUIT      = FOOD+8;
	public static final int BLAND_CHUNKS    = FOOD+9;
	public static final int BERRY           = FOOD+10;
	public static final int PHANTOM_MEAT    = FOOD+11;
	public static final int SUPPLY_RATION   = FOOD+12;
	//酒水（拇指 前二老板「备用酒水」掉落物，绘制于第28行第14列，13×14）
	public static final int WINE_BOTTLE     =                                   xy(14, 28);
	//加速弹（拇指 前二老板 T2「加速弹药」弹药，绘制于第36行第9列，13×13）
	public static final int ACCELERATION_ROUND =                                xy(9, 36);
	static{
		assignItemRect(MEAT,            15, 11);
		assignItemRect(STEAK,           15, 11);
		assignItemRect(STEWED,          15, 11);
		assignItemRect(OVERPRICED,      14, 11);
		assignItemRect(CARPACCIO,       15, 11);
		assignItemRect(RATION,          16, 12);
		assignItemRect(PASTY,           16, 11);
		assignItemRect(MEAT_PIE,        16, 12);
		assignItemRect(BLANDFRUIT,      9,  12);
		assignItemRect(BLAND_CHUNKS,    14,  6);
		assignItemRect(BERRY,           9,  11);
		assignItemRect(PHANTOM_MEAT,    15, 11);
		assignItemRect(SUPPLY_RATION,   16, 12);
		assignItemRect(WINE_BOTTLE,     13, 14);
		assignItemRect(ACCELERATION_ROUND, 13, 13);
	}

	private static final int HOLIDAY_FOOD   =                               xy(1, 29);  //16 slots
	public static final int STEAMED_FISH    = HOLIDAY_FOOD+0;
	public static final int FISH_LEFTOVER   = HOLIDAY_FOOD+1;
	public static final int CHOC_AMULET     = HOLIDAY_FOOD+2;
	public static final int EASTER_EGG      = HOLIDAY_FOOD+3;
	public static final int RAINBOW_POTION  = HOLIDAY_FOOD+4;
	public static final int SHATTERED_CAKE  = HOLIDAY_FOOD+5;
	public static final int PUMPKIN_PIE     = HOLIDAY_FOOD+6;
	public static final int VANILLA_CAKE    = HOLIDAY_FOOD+7;
	public static final int CANDY_CANE      = HOLIDAY_FOOD+8;
	public static final int SPARKLING_POTION= HOLIDAY_FOOD+9;
	static{
		assignItemRect(STEAMED_FISH,    16, 12);
		assignItemRect(FISH_LEFTOVER,   16, 12);
		assignItemRect(CHOC_AMULET,     16, 16);
		assignItemRect(EASTER_EGG,      12, 14);
		assignItemRect(RAINBOW_POTION,  12, 14);
		assignItemRect(SHATTERED_CAKE,  14, 13);
		assignItemRect(PUMPKIN_PIE,     16, 12);
		assignItemRect(VANILLA_CAKE,    14, 13);
		assignItemRect(CANDY_CANE,      13, 16);
		assignItemRect(SPARKLING_POTION, 7, 16);
	}

	private static final int QUEST  =                                       xy(1, 30);  //16 slots
	public static final int DUST    = QUEST+1;
	public static final int CANDLE  = QUEST+2;
	public static final int EMBER   = QUEST+3;
	public static final int PICKAXE = QUEST+4;
	public static final int ORE     = QUEST+5;
	public static final int TOKEN   = QUEST+6;
	public static final int BLOB    = QUEST+7;
	public static final int SHARD   = QUEST+8;
	public static final int ESCAPE  = QUEST+9;
	//钻石（30行11列，2026-09-04 彩蛋矿物，供挖掘掉落/后续钻石剑）
	public static final int DIAMOND = QUEST+10;
	static{
		assignItemRect(DUST,    12, 11);
		assignItemRect(CANDLE,  12, 12);
		assignItemRect(EMBER,   12, 11);
		assignItemRect(PICKAXE, 14, 14);
		assignItemRect(ORE,     15, 15);
		assignItemRect(TOKEN,   12, 12);
		assignItemRect(BLOB,    10,  9);
		assignItemRect(SHARD,    8, 10);
		assignItemRect(ESCAPE,   8, 16);
		assignItemRect(DIAMOND, 12, 13);
	}

	private static final int BAGS       =                                   xy(1, 31);  //16 slots
	public static final int WATERSKIN   = BAGS+0;
	public static final int BACKPACK    = BAGS+1;
	public static final int POUCH       = BAGS+2;
	public static final int HOLDER      = BAGS+3;
	public static final int BANDOLIER   = BAGS+4;
	public static final int HOLSTER     = BAGS+5;
	public static final int VIAL        = BAGS+6;
	public static final int MATERIAL_BOX= BAGS+7;
	static{
		assignItemRect(WATERSKIN,   16, 14);
		assignItemRect(BACKPACK,    16, 16);
		assignItemRect(POUCH,       14, 15);
		assignItemRect(HOLDER,      16, 16);
		assignItemRect(BANDOLIER,   15, 16);
		assignItemRect(HOLSTER,     15, 16);
		assignItemRect(VIAL,        12, 12);
		assignItemRect(MATERIAL_BOX, 15, 15);
	}

	private static final int DOCUMENTS  =                                   xy(1, 32);  //16 slots
	public static final int GUIDE_PAGE  = DOCUMENTS+0;
	public static final int ALCH_PAGE   = DOCUMENTS+1;
	public static final int SEWER_PAGE  = DOCUMENTS+2;
	public static final int PRISON_PAGE = DOCUMENTS+3;
	public static final int CAVES_PAGE  = DOCUMENTS+4;
	public static final int CITY_PAGE   = DOCUMENTS+5;
	public static final int HALLS_PAGE  = DOCUMENTS+6;
	static{
		assignItemRect(GUIDE_PAGE,  10, 11);
		assignItemRect(ALCH_PAGE,   10, 11);
		assignItemRect(SEWER_PAGE,  10, 11);
		assignItemRect(PRISON_PAGE, 10, 11);
		assignItemRect(CAVES_PAGE,  10, 11);
		assignItemRect(CITY_PAGE,   10, 11);
		assignItemRect(HALLS_PAGE,  10, 11);
	}

	//==========================================================================
	// 可食用蘑菇（移植自 Easily Sprouted PD，2026-09-25）
	// 7 个图标位于 items.png 第 45 行第 1~7 格（该行原为预留空行，本组为首次占用）。
	// 用户搬运时已裁掉透明边、贴齐格子左上角 ⇒ assignItemRect 的宽高直接取实测包围盒，
	// 四个值必须与贴图逐像素一致，否则图标会被放大留边或被裁掉一角。
	//==========================================================================
	//灯笼蘑菇（1列，12×13）
	public static final int MUSHROOM_LANTERN      =                          xy(1, 45);
	//地球之星蘑菇（2列，16×15）
	public static final int MUSHROOM_EARTHSTAR    =                          xy(2, 45);
	//地衣蘑菇（3列，12×9；仅占位贴图，无实际效果）
	public static final int MUSHROOM_LICHEN       =                          xy(3, 45);
	//死亡之帽蘑菇（4列，11×12）
	public static final int MUSHROOM_DEATHCAP     =                          xy(4, 45);
	//蓝牛奶蘑菇（5列，11×12）
	public static final int MUSHROOM_BLUEMILK     =                          xy(5, 45);
	//金色果冻蘑菇（6列，12×11）
	public static final int MUSHROOM_GOLDENJELLY  =                          xy(6, 45);
	//太阳伞菇（7列，12×12）
	public static final int MUSHROOM_PIXIEPARASOL =                          xy(7, 45);
	static{
		assignItemRect(MUSHROOM_LANTERN,      12, 13);
		assignItemRect(MUSHROOM_EARTHSTAR,    16, 15);
		assignItemRect(MUSHROOM_LICHEN,       12,  9);
		assignItemRect(MUSHROOM_DEATHCAP,     11, 12);
		assignItemRect(MUSHROOM_BLUEMILK,     11, 12);
		assignItemRect(MUSHROOM_GOLDENJELLY,  12, 11);
		assignItemRect(MUSHROOM_PIXIEPARASOL, 12, 12);
	}

	//for smaller 8x8 icons that often accompany an item sprite
	public static class Icons {

		private static final int WIDTH = 16;
		public static final int SIZE = 8;

		public static TextureFilm film = new TextureFilm( Assets.Sprites.ITEM_ICONS, SIZE, SIZE );

		private static int xy(int x, int y){
			x -= 1; y -= 1;
			return x + WIDTH*y;
		}

		private static void assignIconRect( int item, int width, int height ){
			int x = (item % WIDTH) * SIZE;
			int y = (item / WIDTH) * SIZE;
			film.add( item, x, y, x+width, y+height);
		}

		private static final int RINGS          =                            xy(1, 1);  //16 slots
		public static final int RING_ACCURACY   = RINGS+0;
		public static final int RING_ARCANA     = RINGS+1;
		public static final int RING_ELEMENTS   = RINGS+2;
		public static final int RING_ENERGY     = RINGS+3;
		public static final int RING_EVASION    = RINGS+4;
		public static final int RING_FORCE      = RINGS+5;
		public static final int RING_FUROR      = RINGS+6;
		public static final int RING_HASTE      = RINGS+7;
		public static final int RING_MIGHT      = RINGS+8;
		public static final int RING_SHARPSHOOT = RINGS+9;
		public static final int RING_TENACITY   = RINGS+10;
		public static final int RING_WEALTH     = RINGS+11;
		static {
			assignIconRect( RING_ACCURACY,      7, 7 );
			assignIconRect( RING_ARCANA,        7, 7 );
			assignIconRect( RING_ELEMENTS,      7, 7 );
			assignIconRect( RING_ENERGY,        7, 5 );
			assignIconRect( RING_EVASION,       7, 7 );
			assignIconRect( RING_FORCE,         5, 6 );
			assignIconRect( RING_FUROR,         7, 6 );
			assignIconRect( RING_HASTE,         6, 6 );
			assignIconRect( RING_MIGHT,         7, 7 );
			assignIconRect( RING_SHARPSHOOT,    7, 7 );
			assignIconRect( RING_TENACITY,      6, 6 );
			assignIconRect( RING_WEALTH,        7, 6 );
		}

		                                                                                //16 free slots

		private static final int SCROLLS        =                            xy(1, 3);  //16 slots
		public static final int SCROLL_UPGRADE  = SCROLLS+0;
		public static final int SCROLL_IDENTIFY = SCROLLS+1;
		public static final int SCROLL_REMCURSE = SCROLLS+2;
		public static final int SCROLL_MIRRORIMG= SCROLLS+3;
		public static final int SCROLL_RECHARGE = SCROLLS+4;
		public static final int SCROLL_TELEPORT = SCROLLS+5;
		public static final int SCROLL_LULLABY  = SCROLLS+6;
		public static final int SCROLL_MAGICMAP = SCROLLS+7;
		public static final int SCROLL_RAGE     = SCROLLS+8;
		public static final int SCROLL_RETRIB   = SCROLLS+9;
		public static final int SCROLL_TERROR   = SCROLLS+10;
		public static final int SCROLL_TRANSMUTE= SCROLLS+11;
		static {
			assignIconRect( SCROLL_UPGRADE,     7, 7 );
			assignIconRect( SCROLL_IDENTIFY,    4, 7 );
			assignIconRect( SCROLL_REMCURSE,    7, 7 );
			assignIconRect( SCROLL_MIRRORIMG,   7, 5 );
			assignIconRect( SCROLL_RECHARGE,    7, 5 );
			assignIconRect( SCROLL_TELEPORT,    7, 7 );
			assignIconRect( SCROLL_LULLABY,     7, 6 );
			assignIconRect( SCROLL_MAGICMAP,    7, 7 );
			assignIconRect( SCROLL_RAGE,        6, 6 );
			assignIconRect( SCROLL_RETRIB,      5, 6 );
			assignIconRect( SCROLL_TERROR,      5, 7 );
			assignIconRect( SCROLL_TRANSMUTE,   7, 7 );
		}

		private static final int EXOTIC_SCROLLS =                            xy(1, 4);  //16 slots
		public static final int SCROLL_ENCHANT  = EXOTIC_SCROLLS+0;
		public static final int SCROLL_DIVINATE = EXOTIC_SCROLLS+1;
		public static final int SCROLL_ANTIMAGIC= EXOTIC_SCROLLS+2;
		public static final int SCROLL_PRISIMG  = EXOTIC_SCROLLS+3;
		public static final int SCROLL_MYSTENRG = EXOTIC_SCROLLS+4;
		public static final int SCROLL_PASSAGE  = EXOTIC_SCROLLS+5;
		public static final int SCROLL_SIREN    = EXOTIC_SCROLLS+6;
		public static final int SCROLL_FORESIGHT= EXOTIC_SCROLLS+7;
		public static final int SCROLL_CHALLENGE= EXOTIC_SCROLLS+8;
		public static final int SCROLL_PSIBLAST = EXOTIC_SCROLLS+9;
		public static final int SCROLL_DREAD    = EXOTIC_SCROLLS+10;
		public static final int SCROLL_METAMORPH= EXOTIC_SCROLLS+11;
		static {
			assignIconRect( SCROLL_ENCHANT,     7, 7 );
			assignIconRect( SCROLL_DIVINATE,    7, 6 );
			assignIconRect( SCROLL_ANTIMAGIC,   7, 7 );
			assignIconRect( SCROLL_PRISIMG,     5, 7 );
			assignIconRect( SCROLL_MYSTENRG,    7, 5 );
			assignIconRect( SCROLL_PASSAGE,     5, 7 );
			assignIconRect( SCROLL_SIREN,       7, 6 );
			assignIconRect( SCROLL_FORESIGHT,   7, 5 );
			assignIconRect( SCROLL_CHALLENGE,   7, 7 );
			assignIconRect( SCROLL_PSIBLAST,    5, 6 );
			assignIconRect( SCROLL_DREAD,       5, 7 );
			assignIconRect( SCROLL_METAMORPH,   7, 7 );
		}

		                                                                                //16 free slots

		private static final int POTIONS        =                            xy(1, 6);  //16 slots
		public static final int POTION_STRENGTH = POTIONS+0;
		public static final int POTION_HEALING  = POTIONS+1;
		public static final int POTION_MINDVIS  = POTIONS+2;
		public static final int POTION_FROST    = POTIONS+3;
		public static final int POTION_LIQFLAME = POTIONS+4;
		public static final int POTION_TOXICGAS = POTIONS+5;
		public static final int POTION_HASTE    = POTIONS+6;
		public static final int POTION_INVIS    = POTIONS+7;
		public static final int POTION_LEVITATE = POTIONS+8;
		public static final int POTION_PARAGAS  = POTIONS+9;
		public static final int POTION_PURITY   = POTIONS+10;
		public static final int POTION_EXP      = POTIONS+11;
		static {
			assignIconRect( POTION_STRENGTH,    7, 7 );
			assignIconRect( POTION_HEALING,     6, 7 );
			assignIconRect( POTION_MINDVIS,     7, 5 );
			assignIconRect( POTION_FROST,       7, 7 );
			assignIconRect( POTION_LIQFLAME,    5, 7 );
			assignIconRect( POTION_TOXICGAS,    7, 7 );
			assignIconRect( POTION_HASTE,       6, 6 );
			assignIconRect( POTION_INVIS,       5, 7 );
			assignIconRect( POTION_LEVITATE,    6, 7 );
			assignIconRect( POTION_PARAGAS,     7, 7 );
			assignIconRect( POTION_PURITY,      5, 7 );
			assignIconRect( POTION_EXP,         7, 7 );
		}

		private static final int EXOTIC_POTIONS =                            xy(1, 7);  //16 slots
		public static final int POTION_MASTERY  = EXOTIC_POTIONS+0;
		public static final int POTION_SHIELDING= EXOTIC_POTIONS+1;
		public static final int POTION_MAGISIGHT= EXOTIC_POTIONS+2;
		public static final int POTION_SNAPFREEZ= EXOTIC_POTIONS+3;
		public static final int POTION_DRGBREATH= EXOTIC_POTIONS+4;
		public static final int POTION_CORROGAS = EXOTIC_POTIONS+5;
		public static final int POTION_STAMINA  = EXOTIC_POTIONS+6;
		public static final int POTION_SHROUDFOG= EXOTIC_POTIONS+7;
		public static final int POTION_STRMCLOUD= EXOTIC_POTIONS+8;
		public static final int POTION_EARTHARMR= EXOTIC_POTIONS+9;
		public static final int POTION_CLEANSE  = EXOTIC_POTIONS+10;
		public static final int POTION_DIVINE   = EXOTIC_POTIONS+11;
		static {
			assignIconRect( POTION_MASTERY,     7, 7 );
			assignIconRect( POTION_SHIELDING,   6, 6 );
			assignIconRect( POTION_MAGISIGHT,   7, 5 );
			assignIconRect( POTION_SNAPFREEZ,   7, 7 );
			assignIconRect( POTION_DRGBREATH,   7, 7 );
			assignIconRect( POTION_CORROGAS,    7, 7 );
			assignIconRect( POTION_STAMINA,     6, 6 );
			assignIconRect( POTION_SHROUDFOG,   7, 7 );
			assignIconRect( POTION_STRMCLOUD,   7, 7 );
			assignIconRect( POTION_EARTHARMR,   6, 6 );
			assignIconRect( POTION_CLEANSE,     7, 7 );
			assignIconRect( POTION_DIVINE,      7, 7 );
		}

		                                                                                //16 free slots

	}

}
