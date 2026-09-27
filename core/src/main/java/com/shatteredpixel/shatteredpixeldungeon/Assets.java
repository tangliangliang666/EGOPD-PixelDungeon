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

package com.shatteredpixel.shatteredpixeldungeon;

public class Assets {

	public static class Effects {
		public static final String EFFECTS      = "effects/effects.png";
		public static final String FIREBALL     = "effects/fireball.png";
		public static final String SPECKS       = "effects/specks.png";
		public static final String SPELL_ICONS  = "effects/spell_icons.png";
		public static final String TEXT_ICONS   = "effects/text_icons.png";
	}

	public static class Environment {
		public static final String TERRAIN_FEATURES = "environment/terrain_features.png";

		public static final String VISUAL_GRID  = "environment/visual_grid.png";
		public static final String WALL_BLOCKING= "environment/wall_blocking.png";

		public static final String TILES_SEWERS = "environment/tiles_sewers.png";
		public static final String TILES_PRISON = "environment/tiles_prison.png";
		public static final String TILES_CAVES  = "environment/tiles_caves.png";
		public static final String TILES_CITY   = "environment/tiles_city.png";
		public static final String TILES_HALLS  = "environment/tiles_halls.png";
		//27 层测试层专用楼层贴图（用户手绘，2026-09-10；尺寸/布局与其他 tiles_*.png 一致）
		public static final String TILES_LOB    = "environment/tiles_lob.png";

		public static final String TILES_CAVES_CRYSTAL  = "environment/tiles_caves_crystal.png";
		public static final String TILES_CAVES_GNOLL    = "environment/tiles_caves_gnoll.png";

		public static final String WATER_SEWERS = "environment/water0.png";
		public static final String WATER_PRISON = "environment/water1.png";
		public static final String WATER_CAVES  = "environment/water2.png";
		public static final String WATER_CITY   = "environment/water3.png";
		public static final String WATER_HALLS  = "environment/water4.png";

		public static final String WEAK_FLOOR       = "environment/custom_tiles/weak_floor.png";
		public static final String SEWER_BOSS       = "environment/custom_tiles/sewer_boss.png";
		public static final String PRISON_QUEST     = "environment/custom_tiles/prison_quest.png";
		public static final String PRISON_EXIT      = "environment/custom_tiles/prison_exit.png";
		public static final String CAVES_QUEST      = "environment/custom_tiles/caves_quest.png";
		public static final String CAVES_BOSS       = "environment/custom_tiles/caves_boss.png";
		public static final String CITY_QUEST        = "environment/custom_tiles/city_quest.png";
		public static final String CITY_BOSS        = "environment/custom_tiles/city_boss.png";
		public static final String HALLS_SP         = "environment/custom_tiles/halls_special.png";
	}
	
	//TODO include other font assets here? Some are platform specific though...
	public static class Fonts {
		public static final String PIXELFONT= "fonts/pixel_font.png";
	}

	public static class Interfaces {
		public static final String ARCS_BG  = "interfaces/arcs1.png";
		public static final String ARCS_FG  = "interfaces/arcs2.png";

		public static final String BANNERS  = "interfaces/banners.png";
		public static final String BADGES   = "interfaces/badges.png";
		public static final String LOCKED   = "interfaces/locked_badge.png";

		public static final String CHROME   = "interfaces/chrome.png";
		public static final String ICONS    = "interfaces/icons.png";
		//考验系统图标（质点之树）：一行 _10_ 帧、每帧 _16x16_，帧内图标实际尺寸不一，见 Trials.ICON_W/ICON_H
		public static final String TRIALS   = "interfaces/tree.png";
		public static final String STATUS   = "interfaces/status_pane.png";
		public static final String MENU     = "interfaces/menu_pane.png";
		public static final String MENU_BTN = "interfaces/menu_button.png";
		public static final String TOOLBAR  = "interfaces/toolbar.png";
		public static final String SHADOW   = "interfaces/shadow.png";
		public static final String BOSSHP   = "interfaces/boss_hp.png";

		public static final String SURFACE  = "interfaces/surface.png";

		public static final String BUFFS_SMALL      = "interfaces/buffs.png";
		public static final String BUFFS_LARGE      = "interfaces/large_buffs.png";

		public static final String TALENT_ICONS     = "interfaces/talent_icons.png";
		public static final String TALENT_BUTTON    = "interfaces/talent_button.png";

		public static final String HERO_ICONS       = "interfaces/hero_icons.png";

		public static final String RADIAL_MENU      = "interfaces/radial_menu.png";
	}

	//these points to resource bundles, not raw asset files
	public static class Messages {
		public static final String ACTORS   = "messages/actors/actors";
		public static final String ITEMS    = "messages/items/items";
		public static final String JOURNAL  = "messages/journal/journal";
		public static final String LEVELS   = "messages/levels/levels";
		public static final String MISC     = "messages/misc/misc";
		public static final String PLANTS   = "messages/plants/plants";
		public static final String SCENES   = "messages/scenes/scenes";
		public static final String UI       = "messages/ui/ui";
		public static final String WINDOWS  = "messages/windows/windows";
	}

	public static class Music {
		public static final String THEME_1              = "music/theme_1.ogg";
		public static final String THEME_2              = "music/theme_2.ogg";
		public static final String THEME_FINALE         = "music/theme_finale.ogg";

		public static final String SEWERS_1             = "music/sewers_1.ogg";
		public static final String SEWERS_2             = "music/sewers_2.ogg";
		public static final String SEWERS_3             = "music/sewers_3.ogg";
		public static final String SEWERS_TENSE         = "music/sewers_tense.ogg";
		public static final String SEWERS_BOSS          = "music/sewers_boss.ogg";

		public static final String PRISON_1             = "music/prison_1.ogg";
		public static final String PRISON_2             = "music/prison_2.ogg";
		public static final String PRISON_3             = "music/prison_3.ogg";
		public static final String PRISON_TENSE         = "music/prison_tense.ogg";
		public static final String PRISON_BOSS          = "music/prison_boss.ogg";

		public static final String CAVES_1              = "music/caves_1.ogg";
		public static final String CAVES_2              = "music/caves_2.ogg";
		public static final String CAVES_3              = "music/caves_3.ogg";
		public static final String CAVES_TENSE          = "music/caves_tense.ogg";
		public static final String CAVES_BOSS           = "music/caves_boss.ogg";
		public static final String CAVES_BOSS_FINALE    = "music/caves_boss_finale.ogg";

		public static final String CITY_1               = "music/city_1.ogg";
		public static final String CITY_2               = "music/city_2.ogg";
		public static final String CITY_3               = "music/city_3.ogg";
		public static final String CITY_TENSE           = "music/city_tense.ogg";
		public static final String CITY_BOSS            = "music/city_boss.ogg";
		public static final String CITY_BOSS_FINALE     = "music/city_boss_finale.ogg";

		public static final String HALLS_1              = "music/halls_1.ogg";
		public static final String HALLS_2              = "music/halls_2.ogg";
		public static final String HALLS_3              = "music/halls_3.ogg";
		public static final String HALLS_TENSE          = "music/halls_tense.ogg";
		public static final String HALLS_BOSS           = "music/halls_boss.ogg";
		public static final String HALLS_BOSS_FINALE    = "music/halls_boss_finale.ogg";

		//四位自定义角色的专属 BGM（mod，2026-09-18）。素材是 mp3（全平台通吃，iOS 那条
		//「.ogg 换 .mp3」的替换对它们不生效，正好），由 HeroBgm 在开启开关时顶替原版曲目。
		public static final String HERO_ORACLE          = "music/hero_oracle.mp3";        //食指 · 神谕代行者
		public static final String HERO_RING_MASTER     = "music/hero_ring_master.mp3";   //环指大师
		public static final String HERO_MIDDLE_FINGER   = "music/hero_middle_finger.mp3"; //中指 · 长兄
		public static final String HERO_VALENCINA       = "music/hero_valencina.mp3";     //拇指 · 前二老板
	}

	public static class Sounds {
		public static final String CLICK    = "sounds/click.mp3";
		public static final String BADGE    = "sounds/badge.mp3";
		public static final String GOLD     = "sounds/gold.mp3";

		public static final String OPEN     = "sounds/door_open.mp3";
		public static final String UNLOCK   = "sounds/unlock.mp3";
		public static final String ITEM     = "sounds/item.mp3";
		public static final String DEWDROP  = "sounds/dewdrop.mp3";
		public static final String STEP     = "sounds/step.mp3";
		public static final String WATER    = "sounds/water.mp3";
		public static final String GRASS    = "sounds/grass.mp3";
		public static final String TRAMPLE  = "sounds/trample.mp3";
		public static final String STURDY   = "sounds/sturdy.mp3";

		public static final String HIT              = "sounds/hit.mp3";
		public static final String MISS             = "sounds/miss.mp3";
		public static final String HIT_SLASH        = "sounds/hit_slash.mp3";
		public static final String HIT_STAB         = "sounds/hit_stab.mp3";
		public static final String HIT_CRUSH        = "sounds/hit_crush.mp3";
		public static final String HIT_MAGIC        = "sounds/hit_magic.mp3";
		public static final String HIT_STRONG       = "sounds/hit_strong.mp3";
		public static final String HIT_PARRY        = "sounds/hit_parry.mp3";
		public static final String HIT_ARROW        = "sounds/hit_arrow.mp3";
		public static final String ATK_SPIRITBOW    = "sounds/atk_spiritbow.mp3";
		public static final String ATK_CROSSBOW     = "sounds/atk_crossbow.mp3";
		public static final String HEALTH_WARN      = "sounds/health_warn.mp3";
		public static final String HEALTH_CRITICAL  = "sounds/health_critical.mp3";

		public static final String DESCEND  = "sounds/descend.mp3";
		public static final String EAT      = "sounds/eat.mp3";
		public static final String READ     = "sounds/read.mp3";
		public static final String LULLABY  = "sounds/lullaby.mp3";
		public static final String DRINK    = "sounds/drink.mp3";
		public static final String SHATTER  = "sounds/shatter.mp3";
		public static final String ZAP      = "sounds/zap.mp3";
		public static final String LIGHTNING= "sounds/lightning.mp3";
		public static final String LEVELUP  = "sounds/levelup.mp3";
		public static final String DEATH    = "sounds/death.mp3";
		public static final String CHALLENGE= "sounds/challenge.mp3";
		public static final String CURSED   = "sounds/cursed.mp3";
		public static final String TRAP     = "sounds/trap.mp3";
		public static final String EVOKE    = "sounds/evoke.mp3";
		public static final String TOMB     = "sounds/tomb.mp3";
		public static final String ALERT    = "sounds/alert.mp3";
		public static final String MELD     = "sounds/meld.mp3";
		public static final String BOSS     = "sounds/boss.mp3";
		public static final String BLAST    = "sounds/blast.mp3";
		public static final String PLANT    = "sounds/plant.mp3";
		public static final String RAY      = "sounds/ray.mp3";
		public static final String BEACON   = "sounds/beacon.mp3";
		public static final String TELEPORT = "sounds/teleport.mp3";
		public static final String CHARMS   = "sounds/charms.mp3";
		public static final String MASTERY  = "sounds/mastery.mp3";
		public static final String PUFF     = "sounds/puff.mp3";
		public static final String ROCKS    = "sounds/rocks.mp3";
		public static final String BURNING  = "sounds/burning.mp3";
		public static final String FALLING  = "sounds/falling.mp3";
		public static final String GHOST    = "sounds/ghost.mp3";
		public static final String SECRET   = "sounds/secret.mp3";
		public static final String BONES    = "sounds/bones.mp3";
		public static final String BEE      = "sounds/bee.mp3";
		public static final String DEGRADE  = "sounds/degrade.mp3";
		public static final String MIMIC    = "sounds/mimic.mp3";
		public static final String DEBUFF   = "sounds/debuff.mp3";
		public static final String CHARGEUP = "sounds/chargeup.mp3";
		public static final String GAS      = "sounds/gas.mp3";
		public static final String CHAINS   = "sounds/chains.mp3";
		public static final String SCAN     = "sounds/scan.mp3";
		public static final String SHEEP    = "sounds/sheep.mp3";
		public static final String MINE    = "sounds/mine.mp3";
		//「依旧果冻人」挑战的果冻音效（2026-09-17）
		public static final String JELLY    = "sounds/jelly.mp3";

		//食指·神谕代行者（里恩）专属音效（2026-09-18）
		//解放 I/II/III 的结束语语音，与 Furioso-Replica 的结束语文本一一对应
		public static final String INDEXFATHER_LIBERATION_1 = "sounds/indexfather_liberation_1.mp3";
		public static final String INDEXFATHER_LIBERATION_2 = "sounds/indexfather_liberation_2.mp3";
		public static final String INDEXFATHER_LIBERATION_3 = "sounds/indexfather_liberation_3.mp3";
		//赫尔墨斯的双蛇杖：每次命中的随机攻击音（三选一）
		public static final String INDEXFATHER_SKILL1_1 = "sounds/indexfather_skill1_1.mp3";
		public static final String INDEXFATHER_SKILL1_2 = "sounds/indexfather_skill1_2.mp3";
		public static final String INDEXFATHER_SKILL1_3 = "sounds/indexfather_skill1_3.mp3";
		//Furioso-Replica 第九次攻击的特殊攻击音
		public static final String INDEXFATHER_SKILL8_3 = "sounds/indexfather_skill8_3.mp3";

		//指令终端提示音（2026-09-18）：与信息栏文本同步播放
		public static final String INSTRUCTION_BUZZ  = "sounds/instruction_buzz.mp3";  //*哔哔*
		public static final String INSTRUCTION_CLEAR = "sounds/instruction_clear.mp3"; //_CLEAR_

		//拇指·前二老板（瓦伦希娜）专属音效（2026-09-18）
		//闪避成功时的嘲讽语音（仅当头顶弹出「怎么，打不中吗？」时播放，见 Char.attack 未命中分支）
		public static final String VALENCINA_DODGE_TAUNT = "sounds/valencina_dodge_taunt.mp3";
		//预知眼过热：过热期间首次回合开始的抱怨语音
		public static final String VALENCINA_OVERHEAT = "sounds/valencina_overheat.mp3";
		//巴勒莫剑术施放时随机播出的语音（四选一）
		public static final String VALENCINA_PALERMO_VOICE_1 = "sounds/valencina_palermo_voice_1.mp3";
		public static final String VALENCINA_PALERMO_VOICE_2 = "sounds/valencina_palermo_voice_2.mp3";
		public static final String VALENCINA_PALERMO_VOICE_3 = "sounds/valencina_palermo_voice_3.mp3";
		public static final String VALENCINA_PALERMO_VOICE_4 = "sounds/valencina_palermo_voice_4.mp3";
		//巴勒莫剑术命中时的攻击音效（八选一，替代原版的 HIT_STRONG）
		public static final String VALENCINA_PALERMO_HIT_1 = "sounds/valencina_palermo_hit_1.mp3";
		public static final String VALENCINA_PALERMO_HIT_2 = "sounds/valencina_palermo_hit_2.mp3";
		public static final String VALENCINA_PALERMO_HIT_3 = "sounds/valencina_palermo_hit_3.mp3";
		public static final String VALENCINA_PALERMO_HIT_4 = "sounds/valencina_palermo_hit_4.mp3";
		public static final String VALENCINA_PALERMO_HIT_5 = "sounds/valencina_palermo_hit_5.mp3";
		public static final String VALENCINA_PALERMO_HIT_6 = "sounds/valencina_palermo_hit_6.mp3";
		public static final String VALENCINA_PALERMO_HIT_7 = "sounds/valencina_palermo_hit_7.mp3";
		public static final String VALENCINA_PALERMO_HIT_8 = "sounds/valencina_palermo_hit_8.mp3";
		//「处置」（盔甲技能2）施放时随机播出的语音（二选一，同步头顶台词）
		public static final String VALENCINA_DISPOSAL_VOICE_1 = "sounds/valencina_disposal_voice_1.mp3";
		public static final String VALENCINA_DISPOSAL_VOICE_2 = "sounds/valencina_disposal_voice_2.mp3";
		//加速弹状态下攻击结束的换弹声（延后播放，避开攻击音效）
		public static final String VALENCINA_RELOAD = "sounds/valencina_reload.mp3";

		//中指·长兄专属语音（2026-09-18，素材取自灰机 wiki「敌方单位/1327 中指父辈-马蒂亚斯」）
		//封印之剑每推进一段形态的台词，与头顶浮字一一对应（见 SealedSwordBase.unsealInto）
		public static final String MIDDLEFINGER_UNSEAL_1 = "sounds/middlefinger_unseal_1.mp3";
		public static final String MIDDLEFINGER_UNSEAL_2 = "sounds/middlefinger_unseal_2.mp3";
		public static final String MIDDLEFINGER_UNSEAL_3 = "sounds/middlefinger_unseal_3.mp3";
		//「忠义巡礼者」每次使用复仇技艺时
		public static final String MIDDLEFINGER_COUNTER = "sounds/middlefinger_counter.mp3";
		//复仇账簿达到最大充能的回合开始
		public static final String MIDDLEFINGER_LEDGER_FULL = "sounds/middlefinger_ledger_full.mp3";
		//受击时（15% 概率）
		public static final String MIDDLEFINGER_HIT = "sounds/middlefinger_hit.mp3";
		//盔甲技能「即刻处刑[莱瓦汀]」连段收尾时
		public static final String MIDDLEFINGER_EXECUTION = "sounds/middlefinger_execution.mp3";
		//「忠义巡礼者」五式复仇技艺命中时的攻击音效（wiki 的「技能0」三条，随机播一条）
		public static final String MIDDLEFINGER_STRIKE_1 = "sounds/middlefinger_strike_1.mp3";
		public static final String MIDDLEFINGER_STRIKE_2 = "sounds/middlefinger_strike_2.mp3";
		public static final String MIDDLEFINGER_STRIKE_3 = "sounds/middlefinger_strike_3.mp3";

		//环指大师·击杀播报（2026-09-18，素材取自灰机 wiki「敌方单位/1331 卡利斯托」）
		//四条常驻 + 两条仅在装备「一生之作-提比娅」时才进入候选池（见 RingMasterVoice）
		public static final String RINGMASTER_KILL_1 = "sounds/ringmaster_kill_1.mp3";
		public static final String RINGMASTER_KILL_2 = "sounds/ringmaster_kill_2.mp3";
		public static final String RINGMASTER_KILL_3 = "sounds/ringmaster_kill_3.mp3";
		public static final String RINGMASTER_KILL_4 = "sounds/ringmaster_kill_4.mp3";
		public static final String RINGMASTER_KILL_TIBIA_1 = "sounds/ringmaster_kill_tibia_1.mp3";
		public static final String RINGMASTER_KILL_TIBIA_2 = "sounds/ringmaster_kill_tibia_2.mp3";

		public static final String[] all = new String[]{
				CLICK, BADGE, GOLD,

				OPEN, UNLOCK, ITEM, DEWDROP, STEP, WATER, GRASS, TRAMPLE, STURDY,

				HIT, MISS, HIT_SLASH, HIT_STAB, HIT_CRUSH, HIT_MAGIC, HIT_STRONG, HIT_PARRY,
				HIT_ARROW, ATK_SPIRITBOW, ATK_CROSSBOW, HEALTH_WARN, HEALTH_CRITICAL,

				DESCEND, EAT, READ, LULLABY, DRINK, SHATTER, ZAP, LIGHTNING, LEVELUP, DEATH,
				CHALLENGE, CURSED, TRAP, EVOKE, TOMB, ALERT, MELD, BOSS, BLAST, PLANT, RAY, BEACON,
				TELEPORT, CHARMS, MASTERY, PUFF, ROCKS, BURNING, FALLING, GHOST, SECRET, BONES,
				BEE, DEGRADE, MIMIC, DEBUFF, CHARGEUP, GAS, CHAINS, SCAN, SHEEP, MINE, JELLY,

				INDEXFATHER_LIBERATION_1, INDEXFATHER_LIBERATION_2, INDEXFATHER_LIBERATION_3,
				INDEXFATHER_SKILL1_1, INDEXFATHER_SKILL1_2, INDEXFATHER_SKILL1_3,
				INDEXFATHER_SKILL8_3,

				INSTRUCTION_BUZZ, INSTRUCTION_CLEAR,

				VALENCINA_DODGE_TAUNT, VALENCINA_OVERHEAT,
				VALENCINA_PALERMO_VOICE_1, VALENCINA_PALERMO_VOICE_2,
				VALENCINA_PALERMO_VOICE_3, VALENCINA_PALERMO_VOICE_4,
				VALENCINA_PALERMO_HIT_1, VALENCINA_PALERMO_HIT_2,
				VALENCINA_PALERMO_HIT_3, VALENCINA_PALERMO_HIT_4,
				VALENCINA_PALERMO_HIT_5, VALENCINA_PALERMO_HIT_6,
				VALENCINA_PALERMO_HIT_7, VALENCINA_PALERMO_HIT_8,
				VALENCINA_DISPOSAL_VOICE_1, VALENCINA_DISPOSAL_VOICE_2,
				VALENCINA_RELOAD,

				MIDDLEFINGER_UNSEAL_1, MIDDLEFINGER_UNSEAL_2, MIDDLEFINGER_UNSEAL_3,
				MIDDLEFINGER_COUNTER, MIDDLEFINGER_LEDGER_FULL,
				MIDDLEFINGER_HIT, MIDDLEFINGER_EXECUTION,
				MIDDLEFINGER_STRIKE_1, MIDDLEFINGER_STRIKE_2, MIDDLEFINGER_STRIKE_3,

				RINGMASTER_KILL_1, RINGMASTER_KILL_2, RINGMASTER_KILL_3, RINGMASTER_KILL_4,
				RINGMASTER_KILL_TIBIA_1, RINGMASTER_KILL_TIBIA_2
		};
	}

	public static class Splashes {
		public static final String WARRIOR  = "splashes/warrior.jpg";
		public static final String MAGE     = "splashes/mage.jpg";
		public static final String ROGUE    = "splashes/rogue.jpg";
		public static final String HUNTRESS = "splashes/huntress.jpg";
		public static final String DUELIST  = "splashes/duelist.jpg";
		public static final String CLERIC   = "splashes/cleric.jpg";
		public static final String RIEN     = "splashes/rien.jpg"; //神谕代行者的立绘
		public static final String CALLISTO = "splashes/callisto.jpg"; //环指大师的立绘
		public static final String VALENCINA = "splashes/valencina.jpg"; //拇指 前二老板的立绘
		public static final String MATTHIAS = "splashes/matthias.jpg"; //中指 长兄的立绘

		public static final String SEWERS   = "splashes/sewers.jpg";
		public static final String PRISON   = "splashes/prison.jpg";
		public static final String CAVES    = "splashes/caves.jpg";
		public static final String CITY     = "splashes/city.jpg";
		public static final String HALLS    = "splashes/halls.jpg";

		public static class Title {
			public static final String ARCHS         = "splashes/title/archs.png";
			public static final String BACK_CLUSTERS = "splashes/title/back_clusters.png";
			public static final String MID_MIXED     = "splashes/title/mid_mixed.png";
			public static final String FRONT_SMALL   = "splashes/title/front_small.png";
		}
	}

	public static class Sprites {
		public static final String ITEMS        = "sprites/items.png";
		public static final String ITEM_ICONS   = "sprites/item_icons.png";

		public static final String WARRIOR  = "sprites/warrior.png";
		public static final String MAGE     = "sprites/mage.png";
		public static final String ROGUE    = "sprites/rogue.png";
		public static final String HUNTRESS = "sprites/huntress.png";
		public static final String DUELIST  = "sprites/duelist.png";
		public static final String CLERIC   = "sprites/cleric.png";
		public static final String RIEN     = "sprites/rien.png"; //神谕代行者的皮肤
		public static final String CALLISTO = "sprites/callisto.png"; //环指大师的皮肤（20×24帧，420×192）
		public static final String VALENCINA = "sprites/valencina.png"; //拇指 前二老板的皮肤（12×15帧，256×128，与战士同布局）
		public static final String MATTHIAS = "sprites/matthias.png"; //中指 长兄的皮肤（12×15帧，256×128，与战士同布局）
		public static final String AVATARS  = "sprites/avatars.png";
		public static final String PET      = "sprites/pet.png";
		public static final String AMULET   = "sprites/amulet.png";
		public static final String ENCHANTED_GLINT = "sprites/enchanted_item_glint.png"; //钻石剑专属 MC 附魔流光贴图

		public static final String RAT      = "sprites/rat.png";
		public static final String DOLL     = "sprites/doll.png"; //环指自动人偶（13×14 帧，共 8 帧：默认/攻击×2/移动×2/死亡×3）
		public static final String MASTERPIECE = "sprites/masterpiece.png"; //环指大师盔甲技能「闭馆」生成的杰作视觉（80×64）
		public static final String BRUTE    = "sprites/brute.png";
		public static final String SPINNER  = "sprites/spinner.png";
		public static final String DM300    = "sprites/dm300.png";
		public static final String WRAITH   = "sprites/wraith.png";
		public static final String UNDEAD   = "sprites/undead.png";
		public static final String KING     = "sprites/king.png";
		public static final String PIRANHA  = "sprites/piranha.png";
		public static final String EYE      = "sprites/eye.png";
		public static final String GNOLL    = "sprites/gnoll.png";
		public static final String CRAB     = "sprites/crab.png";
		public static final String GOO      = "sprites/goo.png";
		public static final String SWARM    = "sprites/swarm.png";
		public static final String SKELETON = "sprites/skeleton.png";
		public static final String SHAMAN   = "sprites/shaman.png";
		public static final String THIEF    = "sprites/thief.png";
		public static final String TENGU    = "sprites/tengu.png";
		public static final String SHEEP    = "sprites/sheep.png";
		public static final String KEEPER   = "sprites/shopkeeper.png";
		public static final String BAT      = "sprites/bat.png";
		public static final String ELEMENTAL= "sprites/elemental.png";
		public static final String MONK     = "sprites/monk.png";
		public static final String WARLOCK  = "sprites/warlock.png";
		public static final String GOLEM    = "sprites/golem.png";
		public static final String STATUE   = "sprites/statue.png";
		public static final String SUCCUBUS = "sprites/succubus.png";
		public static final String SCORPIO  = "sprites/scorpio.png";
		public static final String FISTS    = "sprites/yog_fists.png";
		public static final String YOG      = "sprites/yog.png";
		public static final String LARVA    = "sprites/larva.png";
		public static final String GHOST    = "sprites/ghost.png";
		public static final String MAKER    = "sprites/wandmakernew.png";
		public static final String TROLL    = "sprites/blacksmith.png";
		public static final String IMP      = "sprites/demon.png";
		public static final String RATKING  = "sprites/ratking.png";
		public static final String BEE      = "sprites/bee.png";
		public static final String MIMIC    = "sprites/mimic.png";
		public static final String ROT_LASH = "sprites/rot_lasher.png";
		public static final String ROT_HEART= "sprites/rot_heart.png";
		public static final String GUARD    = "sprites/guard.png";
		public static final String INDEX    = "sprites/index.png"; //食指代行者（命运弃子的追杀者，动画同监狱守卫）
		public static final String WARDS    = "sprites/wards.png";
		public static final String GUARDIAN = "sprites/guardian.png";
		public static final String SLIME    = "sprites/slime.png";
		//爱慕神器召唤物「溶解之爱」：主贴图（160×96，帧 16×24，10 列 × 4 行 = 两组，每组前 15 帧有效）
		public static final String LOVENEW   = "sprites/lovenew.png";
		//溶解之爱·反噬形态贴图（128×32，帧 20×24，单行 6 帧）：1 默认 / 2 移动 / 3-4 攻击 / 5-6 死亡
		public static final String LOVEANGER = "sprites/loveanger.png";
		//「泪剑」buff 的环绕剑贴图（16×16 单帧，剑身自左上剑柄斜向右下剑尖绘制；
		//显示时逆时针旋转 45° 使剑锋水平指向角色朝向，见 effects/TearSwordVisual）
		public static final String TEARSWORD = "sprites/tearsword.png";
		public static final String PINKSLIME = "sprites/pinkslime.png"; //粉色史莱姆（与史莱姆同帧布局的换色）
		public static final String SNAKE    = "sprites/snake.png";
		public static final String NECRO    = "sprites/necromancer.png";
		public static final String GHOUL    = "sprites/ghoul.png";
		public static final String RIPPER   = "sprites/ripper.png";
		public static final String SPAWNER  = "sprites/spawner.png";
		public static final String DM100    = "sprites/dm100.png";
		public static final String PYLON    = "sprites/pylon.png";
		public static final String DM200    = "sprites/dm200.png";
		public static final String LOTUS    = "sprites/lotus.png";
		public static final String NINJA_LOG        = "sprites/ninja_log.png";
		public static final String SPIRIT_HAWK      = "sprites/spirit_hawk.png";
		public static final String RED_SENTRY       = "sprites/red_sentry.png";
		public static final String CRYSTAL_WISP     = "sprites/crystal_wisp.png";
		public static final String CRYSTAL_GUARDIAN = "sprites/crystal_guardian.png";
		public static final String CRYSTAL_SPIRE    = "sprites/crystal_spire.png";
		public static final String GNOLL_GUARD      = "sprites/gnoll_guard.png";
		public static final String GNOLL_SAPPER     = "sprites/gnoll_sapper.png";
		public static final String GNOLL_GEOMANCER  = "sprites/gnoll_geomancer.png";
		public static final String FUNGAL_SPINNER   = "sprites/fungal_spinner.png";
		public static final String FUNGAL_SENTRY    = "sprites/fungal_sentry.png";
		public static final String FUNGAL_CORE      = "sprites/fungal_core.png";

		public static final String HEAVEN          = "sprites/heaven.png"; //失乐园命中特效（长矛贯穿）

		//微笑的尸山（2026-09-09 验证 Boss 的三形态贴图，帧尺寸各异）
		public static final String SMILE_CORPSE_M1 = "sprites/m1.png"; //形态1：帧 32×24，6 帧
		public static final String SMILE_CORPSE_M2 = "sprites/m2.png"; //形态2：帧 48×48，5 帧
		public static final String SMILE_CORPSE_M3 = "sprites/m3.png"; //形态3：帧 64×48，5 帧
	}
}
