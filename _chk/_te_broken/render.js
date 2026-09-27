/* ============================================================================
 * SPD 地形渲染引擎（JS 版）—— 地形编辑器核心
 * ============================================================================
 * 逐层复刻以下四个类的 getTileVisual()，保证预览与游戏内画面一致：
 *   . tiles/DungeonTerrainTilemap.java   （terrain 层：角色之下）
 *   . tiles/TerrainFeaturesTilemap.java  （草叶细节 + 陷阱 + 植物）
 *   . tiles/RaisedTerrainTilemap.java    （raised 层：角色之上，下悬）
 *   . tiles/DungeonWallsTilemap.java     （walls 层：角色之上，内壁 + 上格墙檐）
 *
 * 另复刻 DungeonTileSheet 的帧号常量与缝合算法。
 * 任何一处改动都要同步核对 Java 源（行号见各处注释）。
 * ========================================================================== */
(function (global) {
	'use strict';

	// ---------------------------------------------------------------- 常量
	var WIDTH = 16;                       // 图集列数

	function xy(x, y) { return (x - 1) + WIDTH * (y - 1); }

	// ---- Terrain 常量（levels/Terrain.java）----
	var T = {
		CHASM: 0, EMPTY: 1, GRASS: 2, EMPTY_WELL: 3, WALL: 4,
		DOOR: 5, OPEN_DOOR: 6, ENTRANCE: 7, EXIT: 8, EMBERS: 9,
		LOCKED_DOOR: 10, PEDESTAL: 11, WALL_DECO: 12, BARRICADE: 13,
		EMPTY_SP: 14, HIGH_GRASS: 15, SECRET_DOOR: 16, SECRET_TRAP: 17,
		TRAP: 18, INACTIVE_TRAP: 19, EMPTY_DECO: 20, LOCKED_EXIT: 21,
		UNLOCKED_EXIT: 22, CUSTOM_DECO: 23, WELL: 24, STATUE: 25,
		STATUE_SP: 26, BOOKSHELF: 27, ALCHEMY: 28, WATER: 29,
		FURROWED_GRASS: 30, CRYSTAL_DOOR: 31, CUSTOM_DECO_EMPTY: 32,
		REGION_DECO: 33, REGION_DECO_ALT: 34, MINE_CRYSTAL: 35,
		MINE_BOULDER: 36, ENTRANCE_SP: 37, HERO_LKD_DR: 38, MINE_DIAMOND: 39
	};

	// ---- 帧号（tiles/DungeonTileSheet.java）----
	var GROUND = xy(1, 1), CHASM_T = xy(9, 2), WATER_T = xy(1, 3);
	var FLAT_WALLS = xy(1, 4), FLAT_OTHER = xy(1, 5);
	var RAISED_WALLS = xy(1, 6), RAISED_DOORS = xy(1, 8), RAISED_OTHER = xy(9, 8);
	var WALLS_INTERNAL = xy(1, 10), WALLS_OVERHANG = xy(1, 13);
	var DOOR_OVERHANG = xy(1, 15), OTHER_OVERHANG = xy(9, 15);

	var F = {
		FLOOR: GROUND + 0, FLOOR_DECO: GROUND + 1, GRASS: GROUND + 2,
		EMBERS: GROUND + 3, FLOOR_SP: GROUND + 4,
		FLOOR_ALT_1: GROUND + 6, FLOOR_DECO_ALT: GROUND + 7, GRASS_ALT: GROUND + 8,
		EMBERS_ALT: GROUND + 9, FLOOR_SP_ALT: GROUND + 10, FLOOR_ALT_2: GROUND + 12,
		ENTRANCE: GROUND + 16, EXIT: GROUND + 17, WELL: GROUND + 18,
		EMPTY_WELL: GROUND + 19, PEDESTAL: GROUND + 20, ENTRANCE_SP: GROUND + 22,

		CHASM: CHASM_T, CHASM_FLOOR: CHASM_T + 1, CHASM_FLOOR_SP: CHASM_T + 2,
		CHASM_WALL: CHASM_T + 3, CHASM_WATER: CHASM_T + 4,

		WATER: WATER_T,

		FLAT_WALL: FLAT_WALLS + 0, FLAT_WALL_DECO: FLAT_WALLS + 1,
		FLAT_BOOKSHELF: FLAT_WALLS + 2,
		FLAT_WALL_ALT: FLAT_WALLS + 4, FLAT_WALL_DECO_ALT: FLAT_WALLS + 5,
		FLAT_BOOKSHELF_ALT: FLAT_WALLS + 6,
		FLAT_DOOR: FLAT_WALLS + 8, FLAT_DOOR_OPEN: FLAT_WALLS + 9,
		FLAT_DOOR_LOCKED: FLAT_WALLS + 10, FLAT_DOOR_CRYSTAL: FLAT_WALLS + 11,
		UNLOCKED_EXIT: FLAT_WALLS + 12, LOCKED_EXIT: FLAT_WALLS + 13,

		FLAT_ALCHEMY_POT: FLAT_OTHER + 0, FLAT_BARRICADE: FLAT_OTHER + 1,
		FLAT_HIGH_GRASS: FLAT_OTHER + 2, FLAT_FURROWED_GRASS: FLAT_OTHER + 3,
		FLAT_HIGH_GRASS_ALT: FLAT_OTHER + 5, FLAT_FURROWED_ALT: FLAT_OTHER + 6,
		FLAT_STATUE: FLAT_OTHER + 8, FLAT_STATUE_SP: FLAT_OTHER + 9,
		FLAT_REGION_DECO: FLAT_OTHER + 10, FLAT_REGION_DECO_ALT: FLAT_OTHER + 11,
		FLAT_MINE_CRYSTAL: FLAT_OTHER + 12, FLAT_MINE_CRYSTAL_ALT: FLAT_OTHER + 13,
		FLAT_MINE_CRYSTAL_ALT_2: FLAT_OTHER + 14,
		FLAT_MINE_DIAMOND: 256, FLAT_MINE_DIAMOND_ALT: 257,

		RAISED_WALL: RAISED_WALLS + 0, RAISED_WALL_DECO: RAISED_WALLS + 4,
		RAISED_WALL_DOOR: RAISED_WALLS + 8, RAISED_WALL_BOOKSHELF: RAISED_WALLS + 12,
		RAISED_WALL_ALT: RAISED_WALLS + 16, RAISED_WALL_DECO_ALT: RAISED_WALLS + 20,
		RAISED_WALL_BOOKSHELF_ALT: RAISED_WALLS + 28,
		RAISED_MINE_DIAMOND: 258, RAISED_MINE_DIAMOND_ALT: 262,

		RAISED_DOOR: RAISED_DOORS + 0, RAISED_DOOR_OPEN: RAISED_DOORS + 1,
		RAISED_DOOR_LOCKED: RAISED_DOORS + 2, RAISED_DOOR_CRYSTAL: RAISED_DOORS + 3,
		RAISED_DOOR_SIDEWAYS: RAISED_DOORS + 4,

		RAISED_ALCHEMY_POT: RAISED_OTHER + 0, RAISED_BARRICADE: RAISED_OTHER + 1,
		RAISED_HIGH_GRASS: RAISED_OTHER + 3, RAISED_FURROWED_GRASS: RAISED_OTHER + 3,
		RAISED_HIGH_GRASS_ALT: RAISED_OTHER + 5, RAISED_FURROWED_ALT: RAISED_OTHER + 6,
		RAISED_STATUE: RAISED_OTHER + 8, RAISED_STATUE_SP: RAISED_OTHER + 9,
		RAISED_REGION_DECO: RAISED_OTHER + 10, RAISED_REGION_DECO_ALT: RAISED_OTHER + 11,
		RAISED_MINE_CRYSTAL: RAISED_OTHER + 12, RAISED_MINE_CRYSTAL_ALT: RAISED_OTHER + 13,
		RAISED_MINE_CRYSTAL_ALT_2: RAISED_OTHER + 14,

		WALL_INTERNAL: WALLS_INTERNAL + 0, WALL_INTERNAL_DECO: WALLS_INTERNAL + 16,
		WALL_INTERNAL_WOODEN: WALLS_INTERNAL + 32,
		WALL_INTERNAL_MINE_DIAMOND: 272,
		WALL_OVERHANG: WALLS_OVERHANG + 0, WALL_OVERHANG_DECO: WALLS_OVERHANG + 4,
		WALL_OVERHANG_WOODEN: WALLS_OVERHANG + 8,
		WALL_OVERHANG_MINE_DIAMOND: 266,
		DOOR_SIDEWAYS_OVERHANG: WALLS_OVERHANG + 16,
		DOOR_SIDEWAYS_OVERHANG_CLOSED: WALLS_OVERHANG + 20,
		DOOR_SIDEWAYS_OVERHANG_LOCKED: WALLS_OVERHANG + 24,
		DOOR_SIDEWAYS_OVERHANG_CRYSTAL: WALLS_OVERHANG + 28,

		DOOR_OVERHANG: DOOR_OVERHANG + 0, DOOR_OVERHANG_OPEN: DOOR_OVERHANG + 1,
		DOOR_OVERHANG_CRYSTAL: DOOR_OVERHANG + 2, DOOR_SIDEWAYS: DOOR_OVERHANG + 3,
		DOOR_SIDEWAYS_LOCKED: DOOR_OVERHANG + 4, DOOR_SIDEWAYS_CRYSTAL: DOOR_OVERHANG + 5,
		EXIT_UNDERHANG: DOOR_OVERHANG + 6,

		ALCHEMY_POT_OVERHANG: OTHER_OVERHANG + 0, BARRICADE_OVERHANG: OTHER_OVERHANG + 1,
		HIGH_GRASS_OVERHANG: OTHER_OVERHANG + 2, FURROWED_OVERHANG: OTHER_OVERHANG + 3,
		HIGH_GRASS_OVERHANG_ALT: OTHER_OVERHANG + 5, FURROWED_OVERHANG_ALT: OTHER_OVERHANG + 6,
		STATUE_OVERHANG: OTHER_OVERHANG + 8, STATUE_SP_OVERHANG: OTHER_OVERHANG + 9,
		REGION_DECO_OVERHANG: OTHER_OVERHANG + 10, REGION_DECO_ALT_OVERHANG: OTHER_OVERHANG + 11,
		MINE_CRYSTAL_OVERHANG: OTHER_OVERHANG + 12, MINE_CRYSTAL_OVERHANG_ALT: OTHER_OVERHANG + 13,
		MINE_CRYSTAL_OVERHANG_ALT_2: OTHER_OVERHANG + 14,
		HIGH_GRASS_UNDERHANG: OTHER_OVERHANG + 18, FURROWED_UNDERHANG: OTHER_OVERHANG + 19,
		HIGH_GRASS_UNDERHANG_ALT: OTHER_OVERHANG + 21, FURROWED_UNDERHANG_ALT: OTHER_OVERHANG + 22
	};

	// ---- 陷阱（levels/traps/Trap.java，与 tilemap 无关，单独一层叠加）----
	//
	// ⚠️ 这是本编辑器最容易误解的一处：陷阱**不是地形**。
	//    Terrain.TRAP / INACTIVE_TRAP / SECRET_TRAP 只是「这格下面有个陷阱」的占位，
	//    TerrainFeaturesTilemap.getTileVisual() 的判断顺序是：
	//        ① traps.get(pos) 命中 ⇒ 返回 (active ? color : BLACK) + shape*16
	//        ② plants.get(pos) 命中 ⇒ 返回 image + 7*16
	//        ③ 才轮到 GRASS / HIGH_GRASS / FURROWED_GRASS / EMBERS 的草叶
	//    也就是说：**陷阱是 SparseArray<Trap> 里的对象**，与 map[] 里的地形值无关。
	//    地图格写 TRAP 只是给「陷阱可以放这里」的语义，真正画什么由陷阱对象决定。
	//    ⇒ 所以只在 map 里写 TRAP 而不同时放一个陷阱对象，画面上**什么都没有**，
	//      看上去就是「陷阱地形未显示」。本编辑器用独立的 trap 层复刻这个机制。
	var TRAP_COLOR = { RED:0, ORANGE:1, YELLOW:2, GREEN:3, TEAL:4, VIOLET:5, WHITE:6, GREY:7, BLACK:8 };
	var TRAP_SHAPE = { DOTS:0, WAVES:1, GRILL:2, STARS:3, DIAMOND:4, CROSSHAIR:5, LARGE_DOT:6 };
	var TRAP_COLOR_NAME = ['RED','ORANGE','YELLOW','GREEN','TEAL','VIOLET','WHITE','GREY','BLACK'];
	var TRAP_SHAPE_NAME = ['DOTS','WAVES','GRILL','STARS','DIAMOND','CROSSHAIR','LARGE_DOT'];

	/* 全部陷阱类（levels/traps/*.java 逐一核对 color/shape）。
	 * 子类没写 color/shape 的继承父类（GnollRockfallTrap→RockfallTrap、
	 * TenguDartTrap→PoisonDartTrap），此处直接填**最终生效值**。 */
	var TRAPS = [
		{ cls:'AlarmTrap',          color:'RED',    shape:'DOTS',      zh:'警报陷阱' },
		{ cls:'BlazingTrap',        color:'ORANGE', shape:'STARS',     zh:'烈焰陷阱' },
		{ cls:'BurningTrap',        color:'ORANGE', shape:'DOTS',      zh:'燃烧陷阱' },
		{ cls:'ChillingTrap',       color:'WHITE',  shape:'DOTS',      zh:'寒冷陷阱' },
		{ cls:'ConfusionTrap',      color:'TEAL',   shape:'GRILL',     zh:'混乱陷阱' },
		{ cls:'CorrosionTrap',      color:'GREY',   shape:'GRILL',     zh:'腐蚀陷阱' },
		{ cls:'CursingTrap',        color:'VIOLET', shape:'WAVES',     zh:'诅咒陷阱' },
		{ cls:'DisarmingTrap',      color:'RED',    shape:'LARGE_DOT', zh:'卸武陷阱' },
		{ cls:'DisintegrationTrap', color:'VIOLET', shape:'CROSSHAIR', zh:'解离陷阱' },
		{ cls:'DistortionTrap',     color:'TEAL',   shape:'LARGE_DOT', zh:'扭曲陷阱' },
		{ cls:'ExplosiveTrap',      color:'ORANGE', shape:'DIAMOND',   zh:'爆炸陷阱' },
		{ cls:'FlashingTrap',       color:'GREY',   shape:'STARS',     zh:'闪光陷阱' },
		{ cls:'FlockTrap',          color:'WHITE',  shape:'WAVES',     zh:'群集陷阱' },
		{ cls:'FrostTrap',          color:'WHITE',  shape:'STARS',     zh:'冰霜陷阱' },
		{ cls:'GatewayTrap',        color:'TEAL',   shape:'CROSSHAIR', zh:'传送门陷阱' },
		{ cls:'GeyserTrap',         color:'TEAL',   shape:'DIAMOND',   zh:'喷泉陷阱' },
		{ cls:'GnollRockfallTrap',  color:'GREY',   shape:'DIAMOND',   zh:'豺狼落石陷阱' },
		{ cls:'GrimTrap',           color:'GREY',   shape:'LARGE_DOT', zh:'致命陷阱' },
		{ cls:'GrippingTrap',       color:'GREY',   shape:'DOTS',      zh:'缠绕陷阱' },
		{ cls:'GuardianTrap',       color:'RED',    shape:'STARS',     zh:'守卫陷阱' },
		{ cls:'OozeTrap',           color:'GREEN',  shape:'DOTS',      zh:'黏液陷阱' },
		{ cls:'PitfallTrap',        color:'RED',    shape:'DIAMOND',   zh:'陷坑陷阱' },
		{ cls:'PoisonDartTrap',     color:'GREEN',  shape:'CROSSHAIR', zh:'毒镖陷阱' },
		{ cls:'RockfallTrap',       color:'GREY',   shape:'DIAMOND',   zh:'落石陷阱' },
		{ cls:'ShockingTrap',       color:'YELLOW', shape:'DOTS',      zh:'电击陷阱' },
		{ cls:'StormTrap',          color:'YELLOW', shape:'STARS',     zh:'雷暴陷阱' },
		{ cls:'SummoningTrap',      color:'TEAL',   shape:'WAVES',     zh:'召唤陷阱' },
		{ cls:'TeleportationTrap',  color:'TEAL',   shape:'DOTS',      zh:'传送陷阱' },
		{ cls:'TenguDartTrap',      color:'GREEN',  shape:'CROSSHAIR', zh:'天狗毒镖陷阱' },
		{ cls:'ToxicTrap',          color:'GREEN',  shape:'GRILL',     zh:'剧毒陷阱' },
		{ cls:'WarpingTrap',        color:'TEAL',   shape:'STARS',     zh:'扭曲空间陷阱' },
		{ cls:'WeakeningTrap',      color:'GREEN',  shape:'WAVES',     zh:'虚弱陷阱' },
		{ cls:'WornDartTrap',       color:'GREY',   shape:'CROSSHAIR', zh:'老旧的毒镖陷阱' }
	];

	// ---- 植物（plants/Plant.java，image + 7*16）----
	var PLANTS = [
		{ cls:'Rotberry',       image:0,  zh:'腐浆果' },
		{ cls:'Firebloom',      image:1,  zh:'火绽花' },
		{ cls:'Swiftthistle',   image:2,  zh:'迅捷蓟' },
		{ cls:'Sungrass',       image:3,  zh:'向阳草' },
		{ cls:'Icecap',         image:4,  zh:'冰盖菇' },
		{ cls:'Stormvine',      image:5,  zh:'雷藤' },
		{ cls:'Sorrowmoss',     image:6,  zh:'悲苔' },
		{ cls:'Mageroyal',      image:7,  zh:'魔皇草' },
		{ cls:'Earthroot',      image:8,  zh:'地根' },
		{ cls:'Starflower',     image:9,  zh:'星辰花' },
		{ cls:'Fadeleaf',       image:10, zh:'影叶' },
		{ cls:'Blindweed',      image:11, zh:'盲草' },
		{ cls:'BlandfruitBush', image:12, zh:'淡果丛' }
	];
	var PLANT_BY_CLS = {}; PLANTS.forEach(function (p) { PLANT_BY_CLS[p.cls] = p; });

	// ---- 道具（items/*，第三层覆盖层）----
	//
	// ⚠️ 道具**不在 tilemap 里，也不在 terrain_features 里**。
	//    游戏里道具是 `Level.heaps`（SparseArray<Heap>）里的对象，由 Heap 自己带
	//    ItemSprite 进 GameScene 渲染组，跟地图贴图完全是两套东西。
	//
	//    编辑器**不渲染真实道具图标**（用户约定）——道具层统一用「口粮」贴图作占位：
	//      ItemSpriteSheet.RATION = FOOD + 5 = xy(1,28) + 5
	//        FOOD = xy(1,28)；xy(x,y) 是 `(x-1) + 16*(y-1)` ⇒ FOOD = 432
	//        RATION = 437 ⇒ col 5 / row 27 ⇒ 像素 (80, 432)
	//    直接从 **items.png** 里抠这一格 16×16 来画。（items.png 与楼层图集不是同一张，
	//    所以这里要单独持有 items 贴图，见 setItemSheet。）
	var RATION_FRAME = (function () {
		// 与 ItemSpriteSheet 的 xy() 保持一致的推导，逐字对齐，别手填数字
		var W = 16;                                  // TX_WIDTH / SIZE = 256 / 16
		function ixy(x, y) { return (x - 1) + W * (y - 1); }
		return ixy(1, 28) + 5;                       // FOOD + 5 = RATION
	})();

	/* 堆型容器帧（C.10）。映射对齐 **ItemSprite.view(Heap)** 的 switch：
	 *   HEAP/FOR_SALE → 道具自己的图标（编辑器 = 口粮占位）；
	 *   CHEST/LOCKED_CHEST/CRYSTAL_CHEST/TOMB → 同名容器帧；
	 *   SKELETON → BONES（注意：游戏里 SKELETON 堆画的是 BONES 帧！）；
	 *   REMAINS → REMAINS。
	 * 容器都在 CONTAINERS = xy(1,3) = 32 起的 16 连槽里：
	 *   BONES=32 / REMAINS=33 / TOMB=34 / GRAVE=35 / CHEST=36 / LOCKED_CHEST=37 /
	 *   CRYSTAL_CHEST=38 / EBONY_CHEST=39。
	 * ⚠️ 这些帧同样来自 **items.png**（不是楼层图集）。
	 * ⚠️ HEAP 不在此表 —— 它走口粮占位（下面渲染处特判），因为「道具内容本身」
	 *    才是 HEAP 堆要画的东西；容器堆（宝箱等）画的是容器、口粮反而画错。 */
	var HEAP_FRAMES = (function () {
		var W = 16;
		function ixy(x, y) { return (x - 1) + W * (y - 1); }
		var CONTAINERS = ixy(1, 3);                  // = 32
		return {
			CHEST:         CONTAINERS + 4,           // = 36
			LOCKED_CHEST:  CONTAINERS + 5,           // = 37
			CRYSTAL_CHEST: CONTAINERS + 6,           // = 38
			TOMB:          CONTAINERS + 2,           // = 34
			SKELETON:      CONTAINERS + 0,           // = 32（BONES！与 ItemSprite.view 一致）
			REMAINS:       CONTAINERS + 1            // = 33
		};
	})();

	/* 道具分类与类名清单。
	 * cls 是**真实类名**（codegen 会写 `new Xxx()` 并 import 对应包），
	 * pkg 是包路径（相对 com.shatteredpixel.shatteredpixeldungeon.items）。
	 * 只收「能直接 new 出来、放上去就合理」的类，避免把抽象类（Item/Weapon）写进去。 */
	var ITEM_GROUPS = [
		{ group: '食物', pkg: 'food', items: [
			{ cls:'Food',          zh:'口粮' },       // image = ItemSpriteSheet.RATION ⇒ 占位图就是它自己
			{ cls:'Pasty',         zh:'馅饼' },
			{ cls:'MeatPie',       zh:'全肉大饼' },
			{ cls:'Blandfruit',    zh:'无味果' },
			{ cls:'Berry',         zh:'浆果' },
			{ cls:'MysteryMeat',   zh:'神秘的肉' },
			{ cls:'ChargrilledMeat', zh:'烤肉' },
			{ cls:'StewedMeat',    zh:'炖肉' },
			{ cls:'FrozenCarpaccio', zh:'冷冻生肉片' },
			{ cls:'PhantomMeat',   zh:'幻影鱼肉' },
			{ cls:'SmallRation',   zh:'小包口粮' },
			{ cls:'SupplyRation',  zh:'备用口粮' },
			{ cls:'WineBottle',    zh:'酒水' }
		]},
		{ group: '药水', pkg: 'potions', items: [
			{ cls:'PotionOfHealing',       zh:'治疗药水' },
			{ cls:'PotionOfStrength',      zh:'力量药水' },
			{ cls:'PotionOfExperience',    zh:'经验药水' },
			{ cls:'PotionOfHaste',         zh:'疾速药水' },
			{ cls:'PotionOfInvisibility',  zh:'隐身药水' },
			{ cls:'PotionOfLiquidFlame',   zh:'液火药水' },
			{ cls:'PotionOfToxicGas',      zh:'毒气药水' },
			{ cls:'PotionOfParalyticGas',  zh:'麻痹药水' },
			{ cls:'PotionOfLevitation',    zh:'漂浮药水' },
			{ cls:'PotionOfMindVision',    zh:'心灵视界药水' },
			{ cls:'PotionOfFrost',         zh:'冰霜药水' },
			{ cls:'PotionOfPurity',        zh:'净化药水' }
		]},
		{ group: '卷轴', pkg: 'scrolls', items: [
			{ cls:'ScrollOfUpgrade',       zh:'升级卷轴' },
			{ cls:'ScrollOfIdentify',      zh:'鉴定卷轴' },
			{ cls:'ScrollOfRemoveCurse',   zh:'去诅咒卷轴' },
			{ cls:'ScrollOfMirrorImage',   zh:'镜像卷轴' },
			{ cls:'ScrollOfRecharging',    zh:'充能卷轴' },
			{ cls:'ScrollOfTeleportation', zh:'传送卷轴' },
			{ cls:'ScrollOfTerror',        zh:'恐惧卷轴' },
			{ cls:'ScrollOfLullaby',       zh:'催眠卷轴' },
			{ cls:'ScrollOfMagicMapping',  zh:'探图卷轴' },
			{ cls:'ScrollOfTransmutation', zh:'转化卷轴' },
			{ cls:'ScrollOfRage',          zh:'暴怒卷轴' },
			{ cls:'ScrollOfRetribution',   zh:'天罚卷轴' }
		]},
		/* 异界卷轴在 scrolls/exotic/ 子包里 —— pkg 写相对路径 */
		{ group: '异界卷轴', pkg: 'scrolls/exotic', items: [
			{ cls:'ScrollOfEnchantment',   zh:'附魔卷轴' },
			{ cls:'ScrollOfChallenge',     zh:'挑战卷轴' },
			{ cls:'ScrollOfForesight',     zh:'远见卷轴' },
			{ cls:'ScrollOfDivination',    zh:'占卜卷轴' },
			{ cls:'ScrollOfAntiMagic',     zh:'反魔卷轴' },
			{ cls:'ScrollOfDread',         zh:'畏惧卷轴' },
			{ cls:'ScrollOfMetamorphosis', zh:'蜕变卷轴' },
			{ cls:'ScrollOfPassage',       zh:'穿行卷轴' },
			{ cls:'ScrollOfPsionicBlast',  zh:'灵能冲击卷轴' },
			{ cls:'ScrollOfSirensSong',    zh:'海妖之歌卷轴' },
			{ cls:'ScrollOfPrismaticImage',zh:'棱光镜像卷轴' },
			{ cls:'ScrollOfMysticalEnergy',zh:'秘能卷轴' }
		]},
		{ group: '符石 / 岩石', pkg: 'stones', items: [
			{ cls:'StoneOfEnchantment',    zh:'附魔符石' },
			{ cls:'StoneOfIntuition',      zh:'直觉符石' },
			{ cls:'StoneOfBlink',          zh:'闪现符石' },
			{ cls:'StoneOfDetectMagic',    zh:'测魔符石' },
			{ cls:'StoneOfClairvoyance',   zh:'透视符石' },
			{ cls:'StoneOfFlock',          zh:'群集符石' },
			{ cls:'StoneOfShock',          zh:'电击符石' },
			{ cls:'StoneOfAggression',     zh:'暴怒符石' },
			{ cls:'StoneOfDeepSleep',      zh:'深眠符石' },
			{ cls:'StoneOfFear',           zh:'恐惧符石' },
			{ cls:'StoneOfAugmentation',   zh:'强化符石' },
			{ cls:'StoneOfBlast',          zh:'爆破符石' }
		]},
		{ group: '金币 / 钥匙', pkg: '', items: [
			{ cls:'Gold',          zh:'金币（默认量）', pkg:'' },
			{ cls:'SkeletonKey',   zh:'骷髅钥匙', pkg:'artifacts' }
		]}
	];

	/* 展平清单：项级 pkg 优先于组级 pkg（钥匙组里 IronKey 在 keys/、SkeletonKey 在 artifacts/）。
	 * ctor 只在特殊表里有；普通项留空，makeItem 里按 `new 类名()` 兜底。 */
	var ITEMS = [];
	ITEM_GROUPS.forEach(function (g) {
		g.items.forEach(function (it) {
			var pk = (it.pkg !== undefined) ? it.pkg : g.pkg;
			ITEMS.push({ cls: it.cls, zh: it.zh, pkg: pk, group: g.group, kind: 'plain' });
		});
	});

	/* 需要构造参数 / 特殊写法的道具，单独一张表。
	 * 它们的 `new` 表达式不是 `new Xxx()`，所以带一个 ctor 字段给出真实表达式。
	 *
	 * ⚠️ 钥匙类虽然也有无参构造器（new IronKey() 合法，depth 默认 0），但游戏里
	 *    一律按当前深度构造（`new IronKey(Dungeon.depth)`，见 RegularLevel 的
	 *    addItemToSpawn(new GoldenKey(Dungeon.depth))）。用 depth 更能反映真实用法，
	 *    所以这里给它们显式 ctor。 */
	var ITEMS_SPECIAL = [
		{ cls:'Torch',           zh:'火把',        pkg:'',          ctor:'new Torch()' },
		{ cls:'CeremonialCandle',zh:'仪式蜡烛',    pkg:'quest',     ctor:'new CeremonialCandle()' },
		{ cls:'TrinketCatalyst', zh:'饰品催化剂',  pkg:'trinkets',  ctor:'new TrinketCatalyst()' },
		{ cls:'CorpseDust',      zh:'尸尘',        pkg:'quest',     ctor:'new CorpseDust()' },
		{ cls:'DarkGold',        zh:'暗金（随机量）', pkg:'quest',   ctor:'new DarkGold().quantity(Random.NormalIntRange(4, 5))' },
		{ cls:'IronKey',         zh:'铁钥匙（按当前深度）',   pkg:'keys',      ctor:'new IronKey( Dungeon.depth )' },
		{ cls:'GoldenKey',       zh:'金钥匙（按当前深度）',   pkg:'keys',      ctor:'new GoldenKey( Dungeon.depth )' },
		{ cls:'CrystalKey',      zh:'水晶钥匙（按当前深度）', pkg:'keys',      ctor:'new CrystalKey( Dungeon.depth )' }
	];
	ITEMS_SPECIAL.forEach(function (it) { it.group = '特殊'; it.kind = 'special'; });
	ITEMS = ITEMS.concat(ITEMS_SPECIAL);
	/* 特殊表的 ctor 覆盖掉普通项的硬拼（IronKey 等既在组里、又在特殊表里，
	 * 合并后 ctor 必须来自特殊表 —— 用一次遍历回填，别靠合并顺序。 */
	ITEMS_SPECIAL.forEach(function (sp) {
		for (var i = 0; i < ITEMS.length; i++)
			if (ITEMS[i].cls === sp.cls && ITEMS[i].kind === 'plain') { ITEMS[i].ctor = sp.ctor; break; }
	});

	var ITEM_BY_CLS = {}; ITEMS.forEach(function (it) { ITEM_BY_CLS[it.cls] = it; });

	/* 道具质量等级（上游 Generator 的掉落语义）——
	 * 这里只作为「放上去时给什么堆型」的提示，不改变生成语句。 */
	var HEAP_TYPES = [
		{ key:'HEAP',         zh:'普通堆放' },
		{ key:'CHEST',        zh:'宝箱' },
		{ key:'LOCKED_CHEST', zh:'上锁宝箱' },
		{ key:'CRYSTAL_CHEST',zh:'水晶箱' },
		{ key:'SKELETON',     zh:'骸骨堆' },
		{ key:'TOMB',         zh:'坟墓' },
		{ key:'REMAINS',      zh:'遗留物' }
	];

	// ---- 拼接表（DungeonTileSheet）----
	var WALL_STITCH = [T.WALL, T.WALL_DECO, T.MINE_DIAMOND, T.SECRET_DOOR,
		T.LOCKED_EXIT, T.UNLOCKED_EXIT, T.BOOKSHELF, -1];
	function wallStitchable(t) { return WALL_STITCH.indexOf(t) !== -1; }

	var DOOR_TILES = [T.DOOR, T.LOCKED_DOOR, T.HERO_LKD_DR, T.CRYSTAL_DOOR, T.OPEN_DOOR];
	function doorTile(t) { return DOOR_TILES.indexOf(t) !== -1; }

	var DIRECT_VISUALS = {};
	DIRECT_VISUALS[T.EMPTY] = F.FLOOR;
	DIRECT_VISUALS[T.GRASS] = F.GRASS;
	DIRECT_VISUALS[T.EMPTY_WELL] = F.EMPTY_WELL;
	DIRECT_VISUALS[T.ENTRANCE] = F.ENTRANCE;
	DIRECT_VISUALS[T.EXIT] = F.EXIT;
	DIRECT_VISUALS[T.EMBERS] = F.EMBERS;
	DIRECT_VISUALS[T.PEDESTAL] = F.PEDESTAL;
	DIRECT_VISUALS[T.EMPTY_SP] = F.FLOOR_SP;
	DIRECT_VISUALS[T.ENTRANCE_SP] = F.ENTRANCE_SP;
	DIRECT_VISUALS[T.SECRET_TRAP] = F.FLOOR;
	DIRECT_VISUALS[T.TRAP] = F.FLOOR;
	DIRECT_VISUALS[T.INACTIVE_TRAP] = F.FLOOR;
	DIRECT_VISUALS[T.CUSTOM_DECO] = F.FLOOR;
	DIRECT_VISUALS[T.CUSTOM_DECO_EMPTY] = F.FLOOR;
	DIRECT_VISUALS[T.EMPTY_DECO] = F.FLOOR_DECO;
	DIRECT_VISUALS[T.LOCKED_EXIT] = F.LOCKED_EXIT;
	DIRECT_VISUALS[T.UNLOCKED_EXIT] = F.UNLOCKED_EXIT;
	DIRECT_VISUALS[T.WELL] = F.WELL;

	var DIRECT_FLAT = {};
	DIRECT_FLAT[T.WALL] = F.FLAT_WALL;
	DIRECT_FLAT[T.DOOR] = F.FLAT_DOOR;
	DIRECT_FLAT[T.OPEN_DOOR] = F.FLAT_DOOR_OPEN;
	DIRECT_FLAT[T.LOCKED_DOOR] = F.FLAT_DOOR_LOCKED;
	DIRECT_FLAT[T.HERO_LKD_DR] = F.FLAT_DOOR_LOCKED;
	DIRECT_FLAT[T.CRYSTAL_DOOR] = F.FLAT_DOOR_CRYSTAL;
	DIRECT_FLAT[T.WALL_DECO] = F.FLAT_WALL_DECO;
	DIRECT_FLAT[T.MINE_DIAMOND] = F.FLAT_MINE_DIAMOND;
	DIRECT_FLAT[T.BOOKSHELF] = F.FLAT_BOOKSHELF;
	DIRECT_FLAT[T.ALCHEMY] = F.FLAT_ALCHEMY_POT;
	DIRECT_FLAT[T.BARRICADE] = F.FLAT_BARRICADE;
	DIRECT_FLAT[T.HIGH_GRASS] = F.FLAT_HIGH_GRASS;
	DIRECT_FLAT[T.FURROWED_GRASS] = F.FLAT_FURROWED_GRASS;
	DIRECT_FLAT[T.STATUE] = F.FLAT_STATUE;
	DIRECT_FLAT[T.STATUE_SP] = F.FLAT_STATUE_SP;
	DIRECT_FLAT[T.REGION_DECO] = F.FLAT_REGION_DECO;
	DIRECT_FLAT[T.REGION_DECO_ALT] = F.FLAT_REGION_DECO_ALT;
	DIRECT_FLAT[T.MINE_CRYSTAL] = F.FLAT_MINE_CRYSTAL;
	DIRECT_FLAT[T.MINE_BOULDER] = F.FLAT_MINE_CRYSTAL;
	DIRECT_FLAT[T.SECRET_DOOR] = F.FLAT_WALL;

	var COMMON_ALT = {}, RARE_ALT = {};
	COMMON_ALT[F.FLOOR] = F.FLOOR_ALT_1; COMMON_ALT[F.GRASS] = F.GRASS_ALT;
	COMMON_ALT[F.FLAT_WALL] = F.FLAT_WALL_ALT; COMMON_ALT[F.EMBERS] = F.EMBERS_ALT;
	COMMON_ALT[F.FLAT_WALL_DECO] = F.FLAT_WALL_DECO_ALT;
	COMMON_ALT[F.FLAT_MINE_DIAMOND] = F.FLAT_MINE_DIAMOND_ALT;
	COMMON_ALT[F.FLOOR_SP] = F.FLOOR_SP_ALT; COMMON_ALT[F.FLOOR_DECO] = F.FLOOR_DECO_ALT;
	COMMON_ALT[F.FLAT_BOOKSHELF] = F.FLAT_BOOKSHELF_ALT;
	COMMON_ALT[F.FLAT_HIGH_GRASS] = F.FLAT_HIGH_GRASS_ALT;
	COMMON_ALT[F.FLAT_FURROWED_GRASS] = F.FLAT_FURROWED_ALT;
	COMMON_ALT[F.RAISED_WALL] = F.RAISED_WALL_ALT;
	COMMON_ALT[F.RAISED_WALL_DECO] = F.RAISED_WALL_DECO_ALT;
	COMMON_ALT[F.RAISED_WALL_BOOKSHELF] = F.RAISED_WALL_BOOKSHELF_ALT;
	COMMON_ALT[F.RAISED_HIGH_GRASS] = F.RAISED_HIGH_GRASS_ALT;
	COMMON_ALT[F.RAISED_FURROWED_GRASS] = F.RAISED_FURROWED_ALT;
	COMMON_ALT[F.HIGH_GRASS_OVERHANG] = F.HIGH_GRASS_OVERHANG_ALT;
	COMMON_ALT[F.FURROWED_OVERHANG] = F.FURROWED_OVERHANG_ALT;
	COMMON_ALT[F.HIGH_GRASS_UNDERHANG] = F.HIGH_GRASS_UNDERHANG_ALT;
	COMMON_ALT[F.FURROWED_UNDERHANG] = F.FURROWED_UNDERHANG_ALT;
	RARE_ALT[F.FLOOR] = F.FLOOR_ALT_2;

	var WATER_STITCH = [T.EMPTY, T.GRASS, T.EMPTY_WELL, T.ENTRANCE, T.EXIT, T.EMBERS,
		T.BARRICADE, T.HIGH_GRASS, T.FURROWED_GRASS, T.SECRET_TRAP, T.TRAP,
		T.INACTIVE_TRAP, T.EMPTY_DECO, T.CUSTOM_DECO, T.WELL, T.STATUE,
		T.REGION_DECO, T.ALCHEMY, T.CUSTOM_DECO_EMPTY, T.MINE_CRYSTAL,
		T.MINE_BOULDER, T.DOOR, T.OPEN_DOOR, T.LOCKED_DOOR, T.HERO_LKD_DR,
		T.CRYSTAL_DOOR];
	function waterStitchable(tile, depth) {
		if (tile === T.REGION_DECO_ALT) return depth > 20;
		return WATER_STITCH.indexOf(tile) !== -1;
	}
	var CHASM_STITCH = {};
	(function () {
		var m = {};
		[T.EMPTY, T.GRASS, T.EMBERS, T.EMPTY_WELL, T.HIGH_GRASS, T.FURROWED_GRASS,
			T.EMPTY_DECO, T.CUSTOM_DECO, T.WELL, T.STATUE, T.REGION_DECO,
			T.SECRET_TRAP, T.INACTIVE_TRAP, T.TRAP, T.BOOKSHELF, T.BARRICADE,
			T.PEDESTAL, T.CUSTOM_DECO_EMPTY, T.MINE_BOULDER, T.MINE_CRYSTAL]
			.forEach(function (t) { m[t] = F.CHASM_FLOOR; });
		m[T.EMPTY_SP] = F.CHASM_FLOOR_SP; m[T.STATUE_SP] = F.CHASM_FLOOR_SP;
		[T.WALL, T.DOOR, T.OPEN_DOOR, T.LOCKED_DOOR, T.HERO_LKD_DR, T.SECRET_DOOR,
			T.WALL_DECO].forEach(function (t) { m[t] = F.CHASM_WALL; });
		m[T.WATER] = F.CHASM_WATER;
		CHASM_STITCH = m;
	})();
	function stitchChasmTile(above, depth) {
		if (above === T.REGION_DECO_ALT) {
			if (depth <= 5) return F.CHASM_FLOOR_SP;
			if (depth <= 10) return F.CHASM;
			if (depth <= 20) return F.CHASM_FLOOR_SP;
			return F.CHASM_FLOOR;
		}
		return CHASM_STITCH[above] !== undefined ? CHASM_STITCH[above] : F.CHASM;
	}

	// ---------------------------------------------------------------- 图集
	function Sheet(img) {
		this.w = img.width; this.h = img.height;
		var c = document.createElement('canvas');
		c.width = img.width; c.height = img.height;
		var g = c.getContext('2d');
		g.drawImage(img, 0, 0);
		this.data = g.getImageData(0, 0, img.width, img.height).data;
		this.cols = this.w / 16;
	}
	Sheet.prototype.valid = function (idx) {
		if (idx === null || idx === undefined || idx < 0) return false;
		var cx = idx % this.cols, cy = Math.floor(idx / this.cols);
		return (cy + 1) * 16 <= this.h && (cx + 1) * 16 <= this.w;
	};
	/* 把某一帧画到目标 ctx 的 (dx,dy)；返回是否真的画了 */
	Sheet.prototype.draw = function (ctx, idx, dx, dy) {
		if (!this.valid(idx)) return false;
		var cx = (idx % this.cols) * 16, cy = Math.floor(idx / this.cols) * 16;
		ctx.drawImage(this._src, cx, cy, 16, 16, dx, dy, 16, 16);
		return true;
	};
	Sheet.prototype.raw = function (idx, x, y) {
		var cx = (idx % this.cols) * 16 + x, cy = Math.floor(idx / this.cols) * 16 + y;
		var o = (cy * this.w + cx) * 4;
		var d = this.data;
		return [d[o], d[o + 1], d[o + 2], d[o + 3]];
	};
	/* 整帧 RGBA 数组（用于像素级核验脚本） */
	Sheet.prototype.frame = function (idx) {
		var out = new Uint8Array(16 * 16 * 4);
		for (var y = 0; y < 16; y++)
			for (var x = 0; x < 16; x++) {
				var p = this.raw(idx, x, y), o = (y * 16 + x) * 4;
				out[o] = p[0]; out[o + 1] = p[1]; out[o + 2] = p[2]; out[o + 3] = p[3];
			}
		return out;
	};
	/* 该帧有几个 **完全不透明** 的像素（0 ⇒ 这张图集没画这一帧） */
	Sheet.prototype.solidCount = function (idx) {
		if (!this.valid(idx)) return 0;
		var n = 0;
		for (var y = 0; y < 16; y++)
			for (var x = 0; x < 16; x++)
				if (this.raw(idx, x, y)[3] === 255) n++;
		return n;
	};

	// ---------------------------------------------------------------- 状态
	var S = {
		sheet: null,       // 当前楼层图集
		features: null,    // terrain_features
		water: null,       // 水体动画帧数组 [Image×5]（32×32 平铺）
		waterFrame: 0,     // 当前显示第几帧（编辑器固定，不播放动画）
		w: 0, h: 0,        // 地图尺寸（格）
		map: null,         // Int32Array，长度 w*h
		variance: null,    // Uint8Array，每格 0~99（alt 判定）
		depth: 1,          // 用于 REGION_DECO_ALT / CHASM 缝合的深度
		featuresStage: 0,  // 草叶细节段号 0~4
		/* 道具层 / 植物层：与游戏一致，**不是**地形，而是与 map 平行的对象层。
		 * 数组长度 = w*h，元素为 null 或 {cls, color, shape, active, visible} /
		 * {cls, image}。见 TRAPS / PLANTS 上方的说明。 */
		traps: null,
		plants: null,
		/* 道具层：元素 null 或 {cls, zh, pkg, heap}（heap = Heap.Type 名）。
		 * 只影响**画什么**（统一口粮占位）与 codegen 的落点语句。 */
		items: null,
		/* 区域随机标记（C.5）：[{l,t,r,b}]，闭区间。
		 * 纯元数据 —— **不是**地形、不影响任何一格画什么，只在画布上叠一层
		 * 「斜纹 + 虚线框」提示「这片的生成顺序会被打乱」。
		 * 长度可变（不是 w*h），所以不进 resize 的搬运逻辑。 */
		regions: [],
		/* 是否显示区域标记（编辑器「显示」面板的开关） */
		showRegions: true,
		/* 尺寸模糊化（C.6）：{on, minW, maxW, minH, maxH, cat, guarded}。
		 * 纯元数据（与 regions 同理），不进 resize 搬运。
		 * 渲染用途只有一个：画**安全区参考线** —— 「无论房间取到哪个尺寸，
		 * 框内这些格一定存在」。让作者能一眼看出哪些内容可能会被切掉。 */
		fuzz: { on: false, minW: 8, maxW: 10, minH: 8, maxH: 10, cat: 'NORMAL', guarded: 1 },
		/* 是否显示安全区参考线 */
		showSafe: true,
		/* items.png（道具图集，与楼层图集不同一张）。只用来取口粮那一格当占位图。 */
		itemSheet: null
	};

	function idxOf(x, y) { return y * S.w + x; }
	function at(x, y) {
		if (x < 0 || y < 0 || x >= S.w || y >= S.h) return -1;
		return S.map[y * S.w + x];
	}

	function getVisualWithAlts(visual, pos) {
		if (visual === null || visual === undefined || visual < 0) return -1;
		var v = S.variance[pos];
		if (v >= 95 && RARE_ALT[visual] !== undefined) return RARE_ALT[visual];
		if (v >= 50 && COMMON_ALT[visual] !== undefined) return COMMON_ALT[visual];
		return visual;
	}

	// ---- DungeonTerrainTilemap.getTileVisual(pos, tile, flat=false) ----
	function terrainVisual(pos, tile) {
		if (tile < 0) return -1;
		var dv = DIRECT_VISUALS[tile];
		if (dv !== undefined) return getVisualWithAlts(dv, pos);

		var x = pos % S.w, y = Math.floor(pos / S.w);

		if (tile === T.WATER) {
			var r = 0;
			if (waterStitchable(at(x, y - 1), S.depth)) r += 1;
			if (waterStitchable(at(x + 1, y), S.depth)) r += 2;
			if (waterStitchable(at(x, y + 1), S.depth)) r += 4;
			if (waterStitchable(at(x - 1, y), S.depth)) r += 8;
			return F.WATER + r;
		}
		if (tile === T.CHASM) {
			var above = (pos > S.w) ? S.map[pos - S.w] : -1;
			return stitchChasmTile(above, S.depth);
		}

		// flat == false 分支
		if (doorTile(tile)) {
			return raisedDoorTile(tile, at(x, y - 1));
		} else if (wallStitchable(tile)) {
			return raisedWallTile(tile, pos, at(x + 1, y), at(x, y + 1), at(x - 1, y));
		} else if (tile === T.STATUE) return F.RAISED_STATUE;
		else if (tile === T.STATUE_SP) return F.RAISED_STATUE_SP;
		else if (tile === T.REGION_DECO) return F.RAISED_REGION_DECO;
		else if (tile === T.REGION_DECO_ALT) return F.RAISED_REGION_DECO_ALT;
		else if (tile === T.MINE_CRYSTAL) return getVisualWithAlts(F.RAISED_MINE_CRYSTAL, pos);
		else if (tile === T.MINE_BOULDER) return getVisualWithAlts(F.RAISED_MINE_CRYSTAL, pos);
		else if (tile === T.ALCHEMY) return F.RAISED_ALCHEMY_POT;
		else if (tile === T.BARRICADE) return F.RAISED_BARRICADE;
		else if (tile === T.HIGH_GRASS) return getVisualWithAlts(F.RAISED_HIGH_GRASS, pos);
		else if (tile === T.FURROWED_GRASS) return getVisualWithAlts(F.RAISED_FURROWED_GRASS, pos);
		return -1;
	}

	function getRaisedDoorTile(tile, below) {
		if (wallStitchable(below)) return F.RAISED_DOOR_SIDEWAYS;
		if (tile === T.DOOR) return F.RAISED_DOOR;
		if (tile === T.OPEN_DOOR) return F.RAISED_DOOR_OPEN;
		if (tile === T.LOCKED_DOOR) return F.RAISED_DOOR_LOCKED;
		if (tile === T.HERO_LKD_DR) return F.RAISED_DOOR_LOCKED;
		if (tile === T.CRYSTAL_DOOR) return F.RAISED_DOOR_CRYSTAL;
		return -1;
	}
	function raisedDoorTile(tile, below) { return getRaisedDoorTile(tile, below); }

	// ---- DungeonTileSheet.getRaisedWallTile ----
	function raisedWallTile(tile, pos, right, below, left) {
		var result;
		if (below === -1 || wallStitchable(below)) return -1;
		else if (doorTile(below)) result = F.RAISED_WALL_DOOR;
		else if (tile === T.WALL || tile === T.SECRET_DOOR) result = F.RAISED_WALL;
		else if (tile === T.WALL_DECO) result = F.RAISED_WALL_DECO;
		else if (tile === T.MINE_DIAMOND) result = F.RAISED_MINE_DIAMOND;
		else if (tile === T.BOOKSHELF) result = F.RAISED_WALL_BOOKSHELF;
		else return -1;

		result = getVisualWithAlts(result, pos);
		if (!wallStitchable(right)) result += 1;
		if (!wallStitchable(left)) result += 2;
		return result;
	}

	// ---- RaisedTerrainTilemap.getTileVisual ----
	function raisedVisual(pos, tile) {
		if (tile === T.HIGH_GRASS)
			return getVisualWithAlts(F.HIGH_GRASS_UNDERHANG, pos);
		if (tile === T.FURROWED_GRASS)
			return getVisualWithAlts(F.FURROWED_UNDERHANG, pos);
		return -1;
	}

	// ---- DungeonWallsTilemap.getTileVisual ----
	function wallsVisual(pos, tile, skipCells) {
		var x = pos % S.w, y = Math.floor(pos / S.w);
		var below = at(x, y + 1);

		if (wallStitchable(tile)) {
			if (below !== -1 && !wallStitchable(below)) {
				if (below === T.DOOR) return F.DOOR_SIDEWAYS;
				if (below === T.LOCKED_DOOR) return F.DOOR_SIDEWAYS_LOCKED;
				if (below === T.HERO_LKD_DR) return F.DOOR_SIDEWAYS_LOCKED;
				if (below === T.CRYSTAL_DOOR) return F.DOOR_SIDEWAYS_CRYSTAL;
				if (below === T.OPEN_DOOR) return -1;
				// 其余落到下面的 overhang 分支
			} else {
				return stitchInternalWallTile(tile, at(x + 1, y), at(x + 1, y + 1),
					below, at(x - 1, y + 1), at(x - 1, y));
			}
		}

		if (skipCells && skipCells[pos]) return -1;

		if (tile === T.LOCKED_EXIT || tile === T.UNLOCKED_EXIT) return F.EXIT_UNDERHANG;
		if (below !== -1 && wallStitchable(below))
			return stitchWallOverhangTile(tile, at(x + 1, y + 1), below, at(x - 1, y + 1));
		if (below === T.DOOR || below === T.LOCKED_DOOR || below === T.HERO_LKD_DR)
			return F.DOOR_OVERHANG;
		if (below === T.OPEN_DOOR) return F.DOOR_OVERHANG_OPEN;
		if (below === T.CRYSTAL_DOOR) return F.DOOR_OVERHANG_CRYSTAL;
		if (below === T.STATUE) return F.STATUE_OVERHANG;
		if (below === T.STATUE_SP) return F.STATUE_SP_OVERHANG;
		if (below === T.REGION_DECO) return F.REGION_DECO_OVERHANG;
		if (below === T.REGION_DECO_ALT) return F.REGION_DECO_ALT_OVERHANG;
		if (below === T.MINE_CRYSTAL)
			return getVisualWithAlts(F.MINE_CRYSTAL_OVERHANG, pos + S.w);
		if (below === T.MINE_BOULDER)
			return getVisualWithAlts(F.MINE_CRYSTAL_OVERHANG, pos + S.w);
		if (below === T.ALCHEMY) return F.ALCHEMY_POT_OVERHANG;
		if (below === T.BARRICADE) return F.BARRICADE_OVERHANG;
		if (below === T.HIGH_GRASS)
			return getVisualWithAlts(F.HIGH_GRASS_OVERHANG, pos + S.w);
		if (below === T.FURROWED_GRASS)
			return getVisualWithAlts(F.FURROWED_OVERHANG, pos + S.w);
		return -1;
	}

	function stitchInternalWallTile(tile, right, rightBelow, below, leftBelow, left) {
		var result;
		if (tile === T.BOOKSHELF || below === T.BOOKSHELF) result = F.WALL_INTERNAL_WOODEN;
		else if (tile === T.WALL_DECO) result = F.WALL_INTERNAL_DECO;
		else if (tile === T.MINE_DIAMOND) result = F.WALL_INTERNAL_MINE_DIAMOND;
		else result = F.WALL_INTERNAL;
		if (!wallStitchable(right)) result += 1;
		if (!wallStitchable(rightBelow)) result += 2;
		if (!wallStitchable(leftBelow)) result += 4;
		if (!wallStitchable(left)) result += 8;
		return result;
	}

	function stitchWallOverhangTile(tile, rightBelow, below, leftBelow) {
		var visual;
		if (tile === T.OPEN_DOOR) visual = F.DOOR_SIDEWAYS_OVERHANG;
		else if (tile === T.DOOR) visual = F.DOOR_SIDEWAYS_OVERHANG_CLOSED;
		else if (tile === T.LOCKED_DOOR) visual = F.DOOR_SIDEWAYS_OVERHANG_LOCKED;
		else if (tile === T.HERO_LKD_DR) visual = F.DOOR_SIDEWAYS_OVERHANG_LOCKED;
		else if (tile === T.CRYSTAL_DOOR) visual = F.DOOR_SIDEWAYS_OVERHANG_CRYSTAL;
		else if (below === T.WALL_DECO) visual = F.WALL_OVERHANG_DECO;
		else if (below === T.MINE_DIAMOND) visual = F.WALL_OVERHANG_MINE_DIAMOND;
		else if (below === T.BOOKSHELF) visual = F.WALL_OVERHANG_WOODEN;
		else visual = F.WALL_OVERHANG;
		if (!wallStitchable(rightBelow)) visual += 1;
		if (!wallStitchable(leftBelow)) visual += 2;
		return visual;
	}

	// ---- TerrainFeaturesTilemap.getTileVisual（完整复刻）----
	//
	// Java 的判断顺序（**必须严格照抄，顺序错了画面就不一样**）：
	//     if (traps.get(pos) != null) { if (!trap.visible) return -1; else return (active?color:BLACK) + shape*16; }
	//     if (plants.get(pos) != null) return plant.image + 7*16;
	//     ... 草叶分支 ...
	// 即：陷阱优先于植物，植物优先于草叶；三者同格时只有前者可见。
	function featuresVisual(pos, tile) {
		// ① 陷阱
		if (S.traps && S.traps[pos]) {
			var tp = S.traps[pos];
			if (!tp.visible) return -1;                       // 未发现 ⇒ 不画（原版行为）
			return (tp.active ? tp.color : TRAP_COLOR.BLACK) + tp.shape * 16;
		}
		// ② 植物
		if (S.plants && S.plants[pos]) {
			return S.plants[pos].image + 7 * 16;
		}
		// ③ 草叶
		var stage = S.featuresStage;
		var alt = S.variance[pos] >= 50 ? 1 : 0;
		if (tile === T.HIGH_GRASS) return 9 + 16 * stage + alt;
		if (tile === T.FURROWED_GRASS) return 11 + 16 * stage + alt;
		if (tile === T.GRASS) return 13 + 16 * stage + alt;
		if (tile === T.EMBERS) return 9 + 16 * 5 + alt;
		return -1;
	}

	// ---------------------------------------------------------------- 对外
	var API = {
		T: T, F: F,
		state: S,
		TRAPS: TRAPS, PLANTS: PLANTS, ITEMS: ITEMS, ITEM_GROUPS: ITEM_GROUPS,
		HEAP_TYPES: HEAP_TYPES,
		TRAP_COLOR: TRAP_COLOR, TRAP_SHAPE: TRAP_SHAPE,
		TRAP_COLOR_NAME: TRAP_COLOR_NAME, TRAP_SHAPE_NAME: TRAP_SHAPE_NAME,
		PLANT_BY_CLS: PLANT_BY_CLS, ITEM_BY_CLS: ITEM_BY_CLS,
		RATION_FRAME: RATION_FRAME,
		HEAP_FRAMES: HEAP_FRAMES,
		wallStitchable: wallStitchable,
		doorTile: doorTile,
		waterStitchable: waterStitchable,
		idxOf: idxOf,
		at: at,

		/* 载入图集（Image 对象）。
		 * layers 两张都可为 null —— 用户只导入楼层图集时 terrain_features 仍用内置那张，
		 * 若连内置都没有，也要能退化成「不画 features 层」而不是整个渲染崩掉。 */
		setSheets: function (tilesImg, featuresImg) {
			S.sheet = tilesImg ? new Sheet(tilesImg) : null;
			if (S.sheet) S.sheet._src = tilesImg;
			S.features = featuresImg ? new Sheet(featuresImg) : null;
			if (S.features) S.features._src = featuresImg;
		},

		/* 水体动画帧：传入 Image 数组（32×32 各一帧）。不传则水面留空。 */
		setWater: function (frames) {
			S.water = (frames && frames.length) ? frames.slice() : null;
		},
		setWaterFrame: function (i) { S.waterFrame = (i | 0); },

		/* 新建一张地图 */
		resize: function (w, h, fill) {
			var old = S.map, ow = S.w, oh = S.h;
			var oldTraps = S.traps, oldPlants = S.plants, oldItems = S.items;
			S.w = w; S.h = h;
			S.map = new Int32Array(w * h);
			S.variance = new Uint8Array(w * h);
			S.map.fill(fill === undefined ? T.WALL : fill);
			S.traps = new Array(w * h); S.traps.fill(null);
			S.plants = new Array(w * h); S.plants.fill(null);
			S.items = new Array(w * h); S.items.fill(null);
			if (old) {
				for (var y = 0; y < Math.min(oh, h); y++)
					for (var x = 0; x < Math.min(ow, w); x++) {
						S.map[y * w + x] = old[y * ow + x];
						if (oldTraps) S.traps[y * w + x] = oldTraps[y * ow + x];
						if (oldPlants) S.plants[y * w + x] = oldPlants[y * ow + x];
						if (oldItems) S.items[y * w + x] = oldItems[y * ow + x];
					}
			}
		},

		/* 陷阱 / 植物 / 道具层：整体替换（编辑器在外部维护，长度必须 = w*h） */
		setLayers: function (traps, plants, items) {
			S.traps = traps || null;
			S.plants = plants || null;
			S.items = (items === undefined) ? S.items : (items || null);
		},

		/* 区域随机标记（C.5）：[{l,t,r,b}]，闭区间。纯元数据，长度可变。
		 * 传 null/undefined ⇒ 清空。 */
		setRegions: function (list) {
			S.regions = (list || []).map(function (r) {
				return { l: r.l | 0, t: r.t | 0, r: r.r | 0, b: r.b | 0 };
			});
		},
		setShowRegions: function (v) { S.showRegions = !!v; },

		/* 尺寸模糊化配置（C.6）。传 null/undefined ⇒ 关闭。
		 * 只做**夹取**，不在这里算安全区 —— 安全区的权威算法在 editor.js 的
		 * safeRect()（那里能拿到 E.w/E.h 与 fuzz）。渲染只负责「照着画」。 */
		setFuzz: function (f) {
			if (!f) { S.fuzz = { on: false, minW: 8, maxW: 10, minH: 8, maxH: 10, cat: 'NORMAL', guarded: 1 }; return; }
			S.fuzz = { on: !!f.on, minW: f.minW | 0, maxW: f.maxW | 0,
				minH: f.minH | 0, maxH: f.maxH | 0, cat: f.cat || 'NORMAL',
				guarded: (f.guarded === undefined ? 1 : f.guarded | 0) };
		},
		setShowSafe: function (v) { S.showSafe = !!v; },
		/* 读回开关（核验脚本与 UI 同步都要用；不要直接摸 S） */
		showSafe: function () { return !!S.showSafe; },
		/* 读回 fuzz 的**副本**（渲染侧那份是只读镜像，改它不会影响编辑器） */
		fuzz: function () { return {
			on: !!S.fuzz.on, minW: S.fuzz.minW, maxW: S.fuzz.maxW,
			minH: S.fuzz.minH, maxH: S.fuzz.maxH, cat: S.fuzz.cat, guarded: S.fuzz.guarded }; },

		/* 道具图集（items.png）。道具层统一用「口粮」贴图作占位，见 RATION_FRAME。 */
		setItemSheet: function (img) {
			S.itemSheet = img ? new Sheet(img) : null;
			if (S.itemSheet) S.itemSheet._src = img;
		},
		/* 口粮占位帧在 items.png 里的帧号（供核验脚本与 UI 共用） */
		rationFrame: function () { return RATION_FRAME; },

		/* 方差数组：编辑器用固定种子，保证画面稳定可复现 */
		setVariance: function (seed) {
			var s = seed >>> 0;
			for (var i = 0; i < S.variance.length; i++) {
				s = (s * 1664525 + 1013904223) >>> 0;
				S.variance[i] = (s >>> 16) % 100;
			}
		},

		/* 四个层的帧号查询，供渲染与核验共用 */
		terrainVisual: terrainVisual,
		raisedVisual: raisedVisual,
		wallsVisual: wallsVisual,
		featuresVisual: featuresVisual,

		/* 陷阱 / 植物在 terrain_features 图集里的帧号（-1 = 该格没有 / 未发现） */
		trapFrame: function (pos) {
			var tp = S.traps && S.traps[pos];
			if (!tp || !tp.visible) return -1;
			return (tp.active ? tp.color : TRAP_COLOR.BLACK) + tp.shape * 16;
		},
		plantFrame: function (pos) {
			var pl = S.plants && S.plants[pos];
			return pl ? pl.image + 7 * 16 : -1;
		},
		/* 道具层统一用口粮占位 ⇒ 有道具就返回口粮帧号，没有返回 -1 */
		itemFrame: function (pos) {
			return (S.items && S.items[pos]) ? RATION_FRAME : -1;
		},
		/* 按类名查陷阱/植物的最终 color/shape/image（供 UI 与 codegen 共用） */
		trapByCls: function (cls) {
			for (var i = 0; i < TRAPS.length; i++) if (TRAPS[i].cls === cls) return TRAPS[i];
			return null;
		},
		plantByCls: function (cls) { return PLANT_BY_CLS[cls] || null; },
		/* 从类名造一个陷阱层条目（active 默认 true、visible 默认 true） */
		makeTrap: function (cls, opt) {
			var d = API.trapByCls(cls);
			if (!d) return null;
			opt = opt || {};
			return {
				cls: d.cls, zh: d.zh,
				color: TRAP_COLOR[d.color], shape: TRAP_SHAPE[d.shape],
				colorName: d.color, shapeName: d.shape,
				active: opt.active !== false,
				visible: opt.visible !== false
			};
		},
		makePlant: function (cls) {
			var d = API.plantByCls(cls);
			if (!d) return null;
			return { cls: d.cls, zh: d.zh, image: d.image };
		},

		/* 按类名查道具 */
		itemByCls: function (cls) { return ITEM_BY_CLS[cls] || null; },
		/* 从类名造一个道具层条目。heap 默认 'HEAP'（游戏里 level.drop 的默认堆型）。 */
		makeItem: function (cls, opt) {
			var d = API.itemByCls(cls);
			if (!d) return null;
			opt = opt || {};
			return {
				cls: d.cls, zh: d.zh, pkg: d.pkg,
				kind: d.kind || 'plain',
				/* 优先用表里给的 ctor（特殊写法：DarkGold 带 quantity、钥匙带 Dungeon.depth），
				 * 普通项则按 `new 类名()` 兜底。 */
				ctor: d.ctor || ('new ' + d.cls + '()'),
				heap: opt.heap || 'HEAP'
			};
		},

		/* 当前图集里某一帧有几个实心像素（0 ⇒ 该图集没画这一帧）。
		 * 用于「图集覆盖检查」：新手绘的图集往往只画了部分槽位，
		 * 选中没画的槽位会得到一个空白格（肉眼看着像花屏），要提前提示。 */
		solidCount: function (idx) {
			return S.sheet ? S.sheet.solidCount(idx) : 0;
		},
		featSolidCount: function (idx) {
			return S.features ? S.features.solidCount(idx) : 0;
		},
		/* 该地形在**当前图集 + 当前草叶区**下的地基帧号（-1 = 该层不画）。
		 * 取的是「非 ALT」规范帧 —— 用于图集覆盖检查（判断该图集有没有画这一帧）。
		 * 用 variance=0 的临时位置查，避开 ALT 分支。 */
		baseVisualFor: function (tile) {
			var save = S.variance;
			S.variance = new Uint8Array(Math.max(1, (S.w || 1) * (S.h || 1)));
			var v = (tile === undefined || tile === null) ? -1 : terrainVisual(0, tile);
			S.variance = save;
			return v;
		},

		/* 把整张地图按游戏 z 序渲染到 ctx（16px/格）
		 *
		 * z 序严格对齐 GameScene：terrain 组（含水面 SkinnedBlock、地形、草叶/陷阱/植物）
		 *                          → raised（高草下悬） → walls（墙内壁/墙檐）
		 */
		render: function (ctx, opts) {
			opts = opts || {};
			var scale = opts.scale || 1;
			var showFeatures = opts.showFeatures !== false;
			var showWater = opts.showWater !== false;			ctx.clearRect(0, 0, S.w * 16 * scale, S.h * 16 * scale);
			ctx.imageSmoothingEnabled = false;
			ctx.save();
			ctx.scale(scale, scale);

			var p, x, y, i;

			// ⚠️ 图集还没加载完（或加载失败）时**必须直接返回**。
			//
			//    app.js 的 boot() 是同步跑到底的：它调用 fullRefresh() → draw() → 这里，
			//    而楼层图集是 `new Image(); im.onload = ...` **异步**加载的 ——
			//    也就是说首帧一定发生在 setSheets() 之前。
			//    早期没这层防护，全靠「图集在首帧前恰好加载完」这个巧合；
			//    一旦 assets.js 变大（比如新增 items.png），首帧就会赶在加载完成之前，
			//    于是 S.sheet.draw() 直接抛
			//      TypeError: Cannot read properties of null (reading 'draw')
			//    —— 整个 boot() 中断、下拉框全空、画布 16×16、代码框空白。
			//    这是**真 bug**，不是无头浏览器的假象；加了这个 guard 才与加载快慢无关。
			//    上层（app.js 的 applySheet 回调）会在图集就绪后重绘一次。
			if (!S.sheet) return;

			// 0) 水体层：平铺 waterTex 贴图（32×32 = 2×2 格），**铺在水格下面**
			//
			// ⚠️ 关键：游戏里水面**不走 tilemap**。`DungeonTerrainTilemap.needsRender()`
			//    会跳过纯 WATER 帧（原版注释：water has no alpha, this improves performance），
			//    水体是 GameScene 里一个独立的 `SkinnedBlock`，用 `level.waterTex()` 的贴图
			//    以 32×32 为周期平铺**整张地图**，再靠地图的 alpha 把非水区域抠掉。
			//
			//    编辑器早期只照抄了 needsRender 的「跳过 WATER」，却没有铺这层 SkinnedBlock，
			//    结果水格下面什么都没有 ⇒ 露出黑底 ⇒ 和深渊看起来一模一样。这就是那个 bug。
			if (showWater && S.water && S.water.length) {
				var wimg = S.water[S.waterFrame % S.water.length];
				if (wimg) {
					// 以 32×32 为周期平铺全图，保证与游戏的地砖对齐方式一致
					for (y = 0; y < S.h * 16; y += 32)
						for (x = 0; x < S.w * 16; x += 32)
							ctx.drawImage(wimg, x, y, 32, 32);
				}
			}

			// 1) terrain 层
			for (p = 0; p < S.map.length; p++) {
				x = (p % S.w) * 16; y = Math.floor(p / S.w) * 16;
				if (S.map[p] === T.WATER) {
					// 水体上层由 tilemap 画的是**缝合边**（与相邻非水格的过渡）。
					// F.WATER + r 的 r 是四邻按 1/2/4/8 加权；r === 0 表示四面都是水，
					// 该帧在需要跳过水面本体时也该跳过（否则会在水面中间画出硬边）。
					var wv = terrainVisual(p, T.WATER);
					if (wv !== F.WATER) S.sheet.draw(ctx, wv, x, y);
					continue;
				}
				S.sheet.draw(ctx, terrainVisual(p, S.map[p]), x, y);
			}
			// 2) terrain_features 层（草叶细节 / 陷阱 / 植物）
			//    同样要防空：features 是独立于楼层图集的另一张图，可能比 S.sheet 更晚到位。
			if (showFeatures && S.features) {
				for (p = 0; p < S.map.length; p++) {
					x = (p % S.w) * 16; y = Math.floor(p / S.w) * 16;
					S.features.draw(ctx, featuresVisual(p, S.map[p]), x, y);
				}
			}
			// 3) raised 层
			for (p = 0; p < S.map.length; p++) {
				x = (p % S.w) * 16; y = Math.floor(p / S.w) * 16;
				S.sheet.draw(ctx, raisedVisual(p, S.map[p]), x, y);
			}
			// 4) walls 层
			for (p = 0; p < S.map.length; p++) {
				x = (p % S.w) * 16; y = Math.floor(p / S.w) * 16;
				S.sheet.draw(ctx, wallsVisual(p, S.map[p], null), x, y);
			}

			// 5) 道具层（独立对象层）
			//
			// ⚠️ 游戏里道具是 `Level.heaps` 里的 Heap 对象，Heap 自带 ItemSprite 进
			//    GameScene 的 **objects** 组（在 terrain 组之上、与角色同组）。
			//    所以这里画在 walls 层之后 —— 道具压在墙上会盖住墙檐，与游戏一致。
			//
			//    渲染规则（C.10）：
			//      · HEAP 堆 → 口粮占位帧（道具内容本身不逐件渲染，用户约定）；
			//      · 容器堆（宝箱/墓碑/骸骨…）→ **items.png 里的容器帧**（HEAP_FRAMES），
			//        对齐 ItemSprite.view(Heap) 的 switch —— 口粮替换掉宝箱是画错。
			//    ⚠️ 别用 `if (!drew)` 判退化 —— Sheet.draw 只要**索引合法**就返回 true，
			//       哪怕这一帧在图集里是全透明的（那样格子上什么都看不到，却不报错）。
			//       必须问 solidCount 才知道「图集里真有这一帧吗」。
			if (opts.showItems !== false && S.items) {
				var rationOK = !!(S.itemSheet && S.itemSheet.solidCount(RATION_FRAME) > 0);
				for (p = 0; p < S.map.length; p++) {
					if (!S.items[p]) continue;
					x = (p % S.w) * 16; y = Math.floor(p / S.w) * 16;
					/* 先看这格是不是容器堆，是且图集里有该帧 ⇒ 画容器；否则口粮 */
					var heapFrame = (S.items[p].heap && HEAP_FRAMES[S.items[p].heap]);
					var frame = -1;
					if (heapFrame !== undefined && S.itemSheet &&
						S.itemSheet.solidCount(heapFrame) > 0) frame = heapFrame;
					else if (rationOK) frame = RATION_FRAME;
					if (frame >= 0) {
						S.itemSheet.draw(ctx, frame, x, y);
					} else {
						// 退化标记：一个橙黄十字，位置居中
						ctx.save();
						ctx.strokeStyle = '#ffcc44'; ctx.lineWidth = 2;
						ctx.beginPath();
						ctx.moveTo(x + 8, y + 3); ctx.lineTo(x + 8, y + 13);
						ctx.moveTo(x + 3, y + 8); ctx.lineTo(x + 13, y + 8);
						ctx.stroke();
						ctx.restore();
					}
				}
			}
			// 6) 区域随机标记（C.5 元数据层，画在最上面）
			//
			// ⚠️ 它**不是**地形，也不改任何一格的画法 —— 只是让作者看见
			//    「生成时这一片的格顺序会被打乱」。
			//    画法是：斜纹底 + 蓝色虚线框 + 四角直角标记。
			//    斜纹用半透明斜线（不填充色块），这样底下的地形仍然看得清。
			if (S.showRegions && S.regions && S.regions.length) {
				ctx.save();
				ctx.lineWidth = 1;
				S.regions.forEach(function (rc) {
					var x0 = rc.l * 16, y0 = rc.t * 16;
					var w0 = (rc.r - rc.l + 1) * 16, h0 = (rc.b - rc.t + 1) * 16;

					// 斜纹底：45° 细线，只铺在区域矩形内
					ctx.save();
					ctx.beginPath();
					ctx.rect(x0, y0, w0, h0);
					ctx.clip();
					ctx.strokeStyle = 'rgba(70,140,255,0.28)';
					ctx.beginPath();
					for (var d = -h0; d < w0; d += 8) {
						ctx.moveTo(x0 + d, y0 + h0);
						ctx.lineTo(x0 + d + h0, y0);
					}
					ctx.stroke();
					ctx.restore();

					// 虚线外框
					ctx.save();
					ctx.setLineDash([5, 3]);
					ctx.strokeStyle = 'rgba(40,110,230,0.95)';
					ctx.lineWidth = 2;
					ctx.strokeRect(x0 + 1, y0 + 1, w0 - 2, h0 - 2);
					ctx.restore();

					// 四角直角标记（比虚线框更耐缩放）
					ctx.save();
					ctx.strokeStyle = '#1f5fd0';
					ctx.lineWidth = 2;
					var L = Math.min(8, Math.min(w0, h0) / 3);
					var corners = [[x0, y0, 1, 1], [x0 + w0, y0, -1, 1],
						[x0, y0 + h0, 1, -1], [x0 + w0, y0 + h0, -1, -1]];
					corners.forEach(function (c) {
						ctx.beginPath();
						ctx.moveTo(c[0] + c[2] * L, c[1]);
						ctx.lineTo(c[0], c[1]);
						ctx.lineTo(c[0], c[1] + c[3] * L);
						ctx.stroke();
					});
					ctx.restore();
				});
				ctx.restore();
			}

			// 7) 尺寸模糊化的安全区参考线（C.6 元数据层，最后画 = 在最上面）
			//
			// 模糊化开启时房间宽度会在 `[minW, maxW]` 里随机取。作者必须能看见
			// **哪些格一定会存在、哪些可能被切掉**，否则「画了却看不到」是必然的。
			//
			// 画三样东西（从上到下语义递减）：
			//   ① **安全区**：`guarded` 内缩、且右下边界取到 minW/minH 时的矩形。
			//      绿框 + 浅绿底 —— 框内任何尺寸下都存在。
			//   ② **最大尺寸边界**：maxW/maxH 对应的右下边界（若超出画布则不画）。
			//      橙虚线 —— 房间最大时到这里；比它更右/更下的内容**永远不存在**。
			//   ③ **安全区之外的一层遮罩**：淡红，提示「这圈可能被切」。
			//
			// ⚠️ 为什么右下边界要用 minW 而不是画布宽：`resize()` 只改 right/bottom，
			//    `left`/`top` 恒定 ⇒ 房间变小时**从右下往左上收**。所以「一定存在」的
			//    区域由**最小尺寸**决定，而不是当前画布尺寸。
			if (S.showSafe && S.fuzz && S.fuzz.on) {
				var f = S.fuzz;
				// 内缩量不能把矩形压反：先夹到不超过最小尺寸的一半
				var gd = Math.max(0, f.guarded || 0);
				var mw = Math.min(S.w, f.minW), mh = Math.min(S.h, f.minH);
				var sl = gd, st = gd;
				var sr = Math.max(sl, mw - 1 - gd), sb = Math.max(st, mh - 1 - gd);

				ctx.save();
				ctx.lineWidth = 1;

				// ③ 安全区之外：淡红遮罩（整块画布抠掉安全区）
				ctx.save();
				ctx.beginPath();
				ctx.rect(0, 0, S.w * 16, S.h * 16);
				ctx.rect(sl * 16, st * 16, (sr - sl + 1) * 16, (sb - st + 1) * 16);
				ctx.fillStyle = 'rgba(220,60,60,0.13)';
				ctx.fill('evenodd');
				ctx.restore();

				// ① 安全区：浅绿底 + 实线绿框
				ctx.save();
				ctx.fillStyle = 'rgba(40,190,110,0.10)';
				ctx.fillRect(sl * 16, st * 16, (sr - sl + 1) * 16, (sb - st + 1) * 16);
				ctx.strokeStyle = 'rgba(20,150,80,0.95)';
				ctx.lineWidth = 2;
				ctx.strokeRect(sl * 16 + 1, st * 16 + 1, (sr - sl + 1) * 16 - 2, (sb - st + 1) * 16 - 2);
				ctx.restore();

				// ①b 安全区的四角直角（与区域标记同款，缩放后仍认得出）
				ctx.save();
				ctx.strokeStyle = 'rgba(20,150,80,0.95)';
				ctx.lineWidth = 2;
				var sw = (sr - sl + 1) * 16, sh = (sb - st + 1) * 16;
				var SL = Math.min(10, Math.min(sw, sh) / 3);
				[[sl * 16, st * 16, 1, 1], [sl * 16 + sw, st * 16, -1, 1],
					[sl * 16, st * 16 + sh, 1, -1], [sl * 16 + sw, st * 16 + sh, -1, -1]]
					.forEach(function (c) {
						ctx.beginPath();
						ctx.moveTo(c[0] + c[2] * SL, c[1]);
						ctx.lineTo(c[0], c[1]);
						ctx.lineTo(c[0], c[1] + c[3] * SL);
						ctx.stroke();
					});
				ctx.restore();

				// ② 最大尺寸边界（橙虚线）。仅当它落在画布内才画 ——
				//    画布一般开得比 max 大，那多出来的部分由 ③ 的遮罩说明。
				ctx.save();
				ctx.setLineDash([7, 4]);
				ctx.strokeStyle = 'rgba(230,140,20,0.95)';
				ctx.lineWidth = 2;
				var mr = Math.min(S.w - 1, f.maxW - 1), mb = Math.min(S.h - 1, f.maxH - 1);
				if (mr >= 0 && mb >= 0)
					ctx.strokeRect(0.5, 0.5, (mr + 1) * 16 - 1, (mb + 1) * 16 - 1);
				ctx.restore();

				ctx.restore();
			}
			ctx.restore();
		}
	};

	global.TE_RENDER = API;
})(typeof window !== 'undefined' ? window : this);
