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

package com.shatteredpixel.shatteredpixeldungeon.tiles;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.LastShopLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.watabou.noosa.Image;
import com.watabou.noosa.tweeners.ScaleTweener;
import com.watabou.utils.GameMath;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;
import com.watabou.utils.SparseArray;

public class TerrainFeaturesTilemap extends DungeonTilemap {

	/**
	 * 由「自带楼层贴图、且材质区域与真实深度不一致」的关卡实现，
	 * 声明 {@code terrain_features.png}（草叶细节）该取哪一段。
	 * <p>返回值 0~4 依次为 下水道 / 监狱 / 矿洞 / 都市 / 恶魔大厅。
	 */
	public interface FeaturesTexProvider {
		int featuresStage();
	}

	private static TerrainFeaturesTilemap instance;

	private SparseArray<Plant> plants;
	private SparseArray<Trap> traps;

	public TerrainFeaturesTilemap(SparseArray<Plant> plants, SparseArray<Trap> traps) {
		super(Assets.Environment.TERRAIN_FEATURES);

		this.plants = plants;
		this.traps = traps;

		if (Dungeon.level != null) {
			map(Dungeon.level.map, Dungeon.level.width());
		}

		instance = this;
	}

	/**
	 * 草叶细节（长草尖/草簇阴影）所用的区域段号 stage。
	 * <p>{@code terrain_features.png} 是**全局唯一**图集，每 16 个帧槽一组对应一个区域：
	 * 槽 9/11/13 + 16*stage 依次是 下水道 / 监狱 / 矿洞 / 都市 / 恶魔大厅。
	 * <p><b>默认</b>按真实深度分区段（原版逻辑）：{@code (depth-1)/5}，21 层若为恶魔商店再退一档。
	 * <p><b>例外</b>：自带楼层贴图的关卡（如 27 层 {@code LobTestLevel} 的 tiles_lob）材质并不属于
	 * 本层真实深度所在的区域。此类关卡实现 {@link FeaturesTexProvider} 声明自己该用哪一段，
	 * 否则真实深度 27 会被 {@code Math.min(...,4)} 夹到第 5 段，出现「tiles_lob 的监狱风格草地
	 * 上盖着恶魔大厅的蘑菇形草叶」这种错配。
	 */
	static int featuresStage(){
		Level level = Dungeon.level;
		if (level instanceof FeaturesTexProvider){
			return (int)GameMath.gate(0, ((FeaturesTexProvider)level).featuresStage(), 4);
		}

		int stage = (Dungeon.depth-1)/5;
		if (Dungeon.depth == 21 && level instanceof LastShopLevel) stage--;
		return Math.min(stage, 4);
	}

	protected int getTileVisual(int pos, int tile, boolean flat){
		if (traps.get(pos) != null){
			Trap trap = traps.get(pos);
			if (!trap.visible)
				return -1;
			else
				return (trap.active ? trap.color : Trap.BLACK) + (trap.shape * 16);
		}

		if (plants.get(pos) != null){
			return plants.get(pos).image + 7*16;
		}

		int stage = featuresStage();
		if (tile == Terrain.HIGH_GRASS){
			return 9 + 16*stage + (DungeonTileSheet.tileVariance[pos] >= 50 ? 1 : 0);
		} else if (tile == Terrain.FURROWED_GRASS){
			return 11 + 16*stage + (DungeonTileSheet.tileVariance[pos] >= 50 ? 1 : 0);
		} else if (tile == Terrain.GRASS) {
			return 13 + 16*stage + (DungeonTileSheet.tileVariance[pos] >= 50 ? 1 : 0);
		} else if (tile == Terrain.EMBERS) {
			return 9 + (16*5) + (DungeonTileSheet.tileVariance[pos] >= 50 ? 1 : 0);
		}

		return -1;
	}

	public static Image getTrapVisual( Trap trap ){
		if (instance == null) instance = new TerrainFeaturesTilemap(null, null);

		RectF uv = instance.tileset.get((trap.active ? trap.color : Trap.BLACK) + (trap.shape * 16));
		if (uv == null) return null;

		Image img = new Image( instance.texture );
		img.frame(uv);
		return img;
	}

	public static Image getPlantVisual( Plant plant ){
		if (instance == null) instance = new TerrainFeaturesTilemap(null, null);

		RectF uv = instance.tileset.get(plant.image + 7*16);
		if (uv == null) return null;

		Image img = new Image( instance.texture );
		img.frame(uv);
		return img;
	}

	public static Image tile(int pos, int tile ) {
		RectF uv = instance.tileset.get( instance.getTileVisual( pos, tile, true ) );
		if (uv == null) return null;
		
		Image img = new Image( instance.texture );
		img.frame(uv);
		return img;
	}

	public void growPlant( final int pos ){
		final Image plant = tile( pos, map[pos] );
		if (plant == null) return;
		
		plant.origin.set( 8, 12 );
		plant.scale.set( 0 );
		plant.point( DungeonTilemap.tileToWorld( pos ) );

		parent.add( plant );

		parent.add( new ScaleTweener( plant, new PointF(1, 1), 0.2f ) {
			protected void onComplete() {
				plant.killAndErase();
				killAndErase();
				updateMapCell(pos);
			}
		} );
	}

}
