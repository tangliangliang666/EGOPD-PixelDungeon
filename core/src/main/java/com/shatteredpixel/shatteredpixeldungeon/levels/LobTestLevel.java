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

package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.SewerPainter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.entrance.EntranceRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.exit.ExitRoom;
import com.shatteredpixel.shatteredpixeldungeon.tiles.TerrainFeaturesTilemap;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 27 层测试层（2026-09-10 由 {@code CircularTestLevel} 改造而来）。
 * <p><b>用途</b>：验证新楼层贴图 {@code assets/environment/tiles_lob.png} 的视觉效果——
 * 生成方式改为<b>与普通楼层一致</b>（随机房间 + 走廊 + 门的地形生成），仅保留地形要素。
 * <p><b>用户拍板的规则</b>：
 * <ul>
 *   <li><b>仅地形</b>：房间/走廊/门 + painter 的水域、地区装饰；<b>不刷怪、不摆物品、不放陷阱</b>
 *       （特殊房/密室自带战利品与怪物，故一并排除，见 {@link #initRooms()}）。</li>
 *   <li><b>标准房也走「纯地形白名单」</b>：{@code StandardRoom.createRoom(true)} 只从
 *       {@code StandardRoom.TERRAIN_ONLY_ROOMS}（管道/水桥/柱厅/雕像/裂缝/平台等 13 种）
 *       挑选，它们 paint() 阶段不注入 mob/item/trap；否则像 AquariumRoom（食人鱼）、
 *       SuspiciousChestRoom（Mimic）、BurnedRoom/MinefieldRoom（陷阱）、StudyRoom 等会在
 *       房间 paint 时绕过 {@link #createMobs()} / {@link #createItems()} 空覆写而混入实体。</li>
 *   <li><b>无下行楼梯</b>：生成后拆除出口（EXIT 地形与 REGULAR_EXIT transition），
 *       英雄只能靠「测试传送门」再次使用、死亡或重开离开，避免误入不存在的 28 层。</li>
 *   <li>水贴图暂沿用下水道 {@code water0.png}（用户尚未绘制新水素材）。</li>
 *   <li><b>草叶细节（{@code terrain_features.png}）报第二区（监狱）</b>：本层贴图 {@code tiles_lob.png}
 *       的草尖取自监狱材质，不能被生成期借用的 {@link #GEN_DEPTH}（下水道）带偏，见 {@link #FEATURES_STAGE}。</li>
 *   <li>实现 {@link TestLevel}：不计入通关统计/生成记录，也不空耗物资配额与楼层 feeling。</li>
 * </ul>
 * <p><b>到达方式</b>：仅限调试道具「测试传送门」跳入（InterlevelScene.RETURN + 深度 27），
 * 落点由 {@link #LANDING_POS} 交给 Dungeon.switchLevel 回退到本层入口楼梯。
 */
public class LobTestLevel extends RegularLevel implements TestLevel,
		TerrainFeaturesTilemap.FeaturesTexProvider {

	/** 所在深度（与 Dungeon.newLevel() 的 case 27 保持一致）。 */
	public static final int DEPTH = 27;

	/**
	 * 生成期借用的常规深度（真实深度 27 不在房间池的索引范围内，见 {@link #create()}）。
	 * <p>区域风格随该值变化：3~5 下水道、6~10 监狱、11~15 矿洞、16~20 都市、21~26 恶魔大厅。
	 * 现取 4——与沿用的下水道水贴图/BGM/配色一致；若想换风格，改这一行即可。
	 */
	public static final int GEN_DEPTH = 4;

	/**
	 * 草叶细节（{@code terrain_features.png}）所用的区段：<b>1</b> = 第二区（监狱）。
	 * <p>与 {@link #GEN_DEPTH} <b>无绑定关系</b>——生成期借用下水道深度只是为了房间池/水/BGM/配色
	 * 这些「布局要素」，而本层自己的贴图 {@code tiles_lob.png} 的草尖其实取自<b>监狱</b>
	 * （槽 250 与 {@code tiles_prison.png} 的同一帧逐像素相同，主色 #67933D/#7A924C/#6A723D）。
	 * 故草叶细节必须同样报第二区，否则会在监狱风格的草皮上盖出别区的草叶。
	 */
	public static final int FEATURES_STAGE = 1;

	/**
	 * 跳层落点：-1 表示交给 {@code Dungeon.switchLevel} 回退规则
	 * （{@code pos < 0} → {@code getTransition(null).cell()} → 本层入口楼梯）。
	 * <p>本层为常规生成，一定有入口 transition，因此可以安全地不指定具体 cell。
	 */
	public static final int LANDING_POS = -1;

	{
		//与下水道相同的迷雾/地图着色
		color1 = 0x48763c;
		color2 = 0x59994a;
	}

	/**
	 * 生成期临时借用常规深度与一个随机种子，生成完毕立即还原：
	 * <ul>
	 *   <li><b>深度</b>：房间/入口/出口的挑选表都是 {@code float[27][]} 且以 {@code Dungeon.depth}
	 *       为索引，真实深度 27 必然越界；同时深度 1/2 会在入口房额外放置冒险指南书页（属"摆物品"），
	 *       故取 {@link #GEN_DEPTH}（4，下水道）。</li>
	 *   <li><b>种子</b>：本层不入 generatedLevels（每次进入都会重新生成），若不换种子，
	 *       会因为"楼层种子固定"而每次进入都是同一张地图；这里临时换成随机种子以获取新布局。</li>
	 * </ul>
	 */
	@Override
	public void create() {
		int realDepth = Dungeon.depth;
		long realSeed = Dungeon.seed;
		Dungeon.depth = GEN_DEPTH;
		Dungeon.seed = Random.Long();
		try {
			super.create();
		} finally {
			Dungeon.depth = realDepth;
			Dungeon.seed = realSeed;
		}
	}

	/**
	 * 仅地形：入口房 + 出口房 + 若干标准房。
	 * <p><b>刻意不含</b>特殊房/密室/商店：它们自带战利品、怪物或陷阱，且 {@code SpecialRoom.createRoom()}
	 * 会推进本局的"特殊房队列"（影响玩家之后真实楼层），与"只验证地形"的目标冲突。
	 */
	@Override
	protected ArrayList<Room> initRooms() {
		ArrayList<Room> initRooms = new ArrayList<>();
		initRooms.add(roomEntrance = EntranceRoom.createEntrance());
		initRooms.add(roomExit = ExitRoom.createExit());

		int standards = standardRooms(false);
		for (int i = 0; i < standards; i++) {
			StandardRoom s;
			do {
				//仅地形：只从"纯地形"房间池挑选，避免房间 paint 阶段混入怪物/物品/陷阱
				s = StandardRoom.createRoom(true);
			} while (!s.setSizeCat(standards - i));
			i += s.sizeFactor() - 1;
			initRooms.add(s);
		}

		return initRooms;
	}

	@Override
	protected int standardRooms(boolean forceMax) {
		if (forceMax) return 6;
		//4~6 间，平均 5（同下水道）
		return 4 + Random.chances(new float[]{1, 3, 1});
	}

	@Override
	protected boolean build() {
		if (!super.build()) return false;

		//死路化：拆除下行出口（地形 + transition），保留入口楼梯作为传送落点
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.EXIT || map[i] == Terrain.UNLOCKED_EXIT) {
				map[i] = Terrain.EMPTY;
			}
		}
		for (int i = transitions.size() - 1; i >= 0; i--) {
			if (transitions.get(i).type == LevelTransition.Type.REGULAR_EXIT) {
				transitions.remove(i);
			}
		}

		return true;
	}

	@Override
	protected Painter painter() {
		return new SewerPainter()
				.setWater(0.30f, 5)
				.setGrass(0.20f, 4)
				.setTraps(0, null, null); //仅地形：不放置陷阱
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_LOB;
	}

	/**
	 * 草叶细节（terrain_features.png）所用的区域段号——**必须与本层贴图的材质对齐**。
	 * <p>tiles_lob.png 的草帧取自**第二区（监狱）**：槽 250（HIGH_GRASS_UNDERHANG）与
	 * tiles_prison.png 同一帧逐像素相同（主色 #67933D/#7A924C/#6A723D）。
	 * <p>而 terrain_features.png 的区段原本只按 `Dungeon.depth` 取：本层真实深度 27 会算成
	 * `(27-1)/5 = 5`，再被夹到 4 ⇒ 取**第五区（恶魔大厅）的蘑菇形草叶**，叠加在监狱风格的草皮上，
	 * 这正是「27 层草地看着像五区蘑菇」的根因。
	 * <p>这里固定报 {@link #FEATURES_STAGE}（1 ⇒ 第二区 监狱）。**不要**改成
	 * `(GEN_DEPTH - 1) / 5`：{@link #GEN_DEPTH} 只是生成期借用的<b>布局</b>深度（4 下水道），
	 * 与 tiles_lob 实际使用的草皮材质（监狱）并不是一回事，跟着它走会退回第一区。
	 */
	@Override
	public int featuresStage() {
		return FEATURES_STAGE;
	}

	//新水素材尚未绘制：暂用下水道水贴图（用户拍板）
	@Override
	public String waterTex() {
		return Assets.Environment.WATER_SEWERS;
	}

	//仅地形验证：不刷怪
	@Override
	protected void createMobs() {
	}

	//仅地形验证：不摆物品（含骨头遗物、露珠、钥匙等）
	@Override
	protected void createItems() {
	}

	//下水道同款视觉（WALL_DECO 的滴水/涟漪）
	@Override
	public Group addVisuals() {
		super.addVisuals();
		SewerLevel.addSewerVisuals(this, visuals);
		return visuals;
	}

	//REGION_DECO（桶等杂物）可燃——与下水道一致
	@Override
	public void buildFlagMaps() {
		super.buildFlagMaps();
		for (int i = 0; i < length(); i++) {
			if (map[i] == Terrain.REGION_DECO || map[i] == Terrain.REGION_DECO_ALT) {
				flamable[i] = true;
			}
		}
	}

	//烧毁 REGION_DECO 的表现——与下水道一致
	@Override
	public void destroy(int pos) {
		int terr = map[pos];
		if (terr == Terrain.REGION_DECO) {
			set(pos, Terrain.WATER);
			Splash.at(pos, 0xFF507B5D, 10);
		} else if (terr == Terrain.REGION_DECO_ALT) {
			set(pos, Terrain.EMPTY_SP);
			Splash.at(pos, 0xFF507B5D, 10);
		}
		super.destroy(pos);
	}

	//下水道同款 BGM（简化自 SewerLevel.playLevelMusic：去掉幽灵任务/护符情境分支）
	@Override
	public void playLevelMusic() {
		Music.INSTANCE.playTracks(SewerLevel.SEWER_TRACK_LIST, SewerLevel.SEWER_TRACK_CHANCES, false);
	}

}
