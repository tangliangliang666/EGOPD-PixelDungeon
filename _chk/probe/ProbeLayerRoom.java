package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard;

import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.watabou.utils.Point;
import com.watabou.utils.Random;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.AlarmTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.FrostTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.PoisonDartTrap;
import com.shatteredpixel.shatteredpixeldungeon.plants.BlandfruitBush;
import com.shatteredpixel.shatteredpixeldungeon.plants.Firebloom;
import com.shatteredpixel.shatteredpixeldungeon.plants.Sungrass;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.GoldenKey;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.DarkGold;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.watabou.utils.Random;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;

/**
 * 由 SPD 地形编辑器生成。
 * 房间内部尺寸 10 × 8（不含外圈墙），舞台图集：sewers。
 * 编辑器随机种子 12345（仅供复现视角，不影响生成逻辑）。
 *
 * 注意：left / top / right / bottom 与本房间在关卡里的坐标一致，
 *      其中 right / bottom 都是**闭区间**，宽度 = right - left + 1。
 */
public class ProbeLayerRoom extends StandardRoom {


	@Override
	public void paint( Level level ) {

		// 1) 骨架：整间铺墙，再把内部掏空 —— 与上游每个 StandardRoom 完全一致
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY );

		// 3) 内部地形（按面积从大到小，已自动合并成矩形填充）
		// 陷阱（可见） × 4 格
		Painter.set( level, left + 1, top + 1, Terrain.TRAP );
		Painter.set( level, left + 2, top + 2, Terrain.TRAP );
		Painter.set( level, left + 3, top + 3, Terrain.TRAP );
		Painter.set( level, left + 7, top + 5, Terrain.TRAP );

		// 草地 × 3 格
		Painter.set( level, left + 7, top + 2, Terrain.GRASS );
		Painter.set( level, left + 5, top + 5, Terrain.GRASS );
		Painter.set( level, left + 8, top + 6, Terrain.GRASS );

		// 隐藏陷阱 × 1 格
		Painter.set( level, left + 5, top + 6, Terrain.SECRET_TRAP );

		// ══ 区域随机排列（共 2 片）══
		// 语义：把区域内格子的地形值序列**打乱后写回** —— 多重集不变，
		//       「几格草、几格水」的比例被完整保留，只是位置随机。
		// ⚠️ 必须在**所有 Painter 填地形之后**执行，否则会被后面的填充覆盖。
		// ⚠️ 区域之间互不相交（编辑器在标记时就已合并），所以顺序不影响结果。
		// ⚠️ 当前**只打乱地形**：区域内的陷阱/植物/道具不会跟着移动。
		//    若区域里放了覆盖层对象，请在编辑器里勾上「连同覆盖层一起打乱」。

		// ── 区域 1：相对房间 (left + 1, top + 1) 起 3 × 3，共 9 格 ──
		// 行优先收集地形值（多重集不变：只换位置，不换配比）
		int[] r0 = new int[]{
			18, 1, 1, 1, 18, 1, 1, 1, 18
		};
		Random.shuffle( r0 );
		// 按同一顺序写回（行优先，与收集顺序一致）
		int[] r0Pos = new int[]{
			level.pointToCell( new Point( left + 1, top + 1 ) ), level.pointToCell( new Point( left + 2, top + 1 ) ), level.pointToCell( new Point( left + 3, top + 1 ) ), level.pointToCell( new Point( left + 1, top + 2 ) ), level.pointToCell( new Point( left + 2, top + 2 ) ), level.pointToCell( new Point( left + 3, top + 2 ) ), level.pointToCell( new Point( left + 1, top + 3 ) ), level.pointToCell( new Point( left + 2, top + 3 ) ), level.pointToCell( new Point( left + 3, top + 3 ) )
		};
		for (int k = 0; k < r0.length; k++)
			Painter.set( level, r0Pos[k], r0[k] );

		// ── 区域 2：相对房间 (left + 5, top + 4) 起 4 × 3，共 12 格 ──
		// 行优先收集地形值（多重集不变：只换位置，不换配比）
		int[] r1 = new int[]{
			1, 1, 1, 1, 2, 1, 18, 1, 17, 1, 1, 2
		};
		Random.shuffle( r1 );
		// 按同一顺序写回（行优先，与收集顺序一致）
		int[] r1Pos = new int[]{
			level.pointToCell( new Point( left + 5, top + 4 ) ), level.pointToCell( new Point( left + 6, top + 4 ) ), level.pointToCell( new Point( left + 7, top + 4 ) ), level.pointToCell( new Point( left + 8, top + 4 ) ), level.pointToCell( new Point( left + 5, top + 5 ) ), level.pointToCell( new Point( left + 6, top + 5 ) ), level.pointToCell( new Point( left + 7, top + 5 ) ), level.pointToCell( new Point( left + 8, top + 5 ) ), level.pointToCell( new Point( left + 5, top + 6 ) ), level.pointToCell( new Point( left + 6, top + 6 ) ), level.pointToCell( new Point( left + 7, top + 6 ) ), level.pointToCell( new Point( left + 8, top + 6 ) )
		};
		for (int k = 0; k < r1.length; k++)
			Painter.set( level, r1Pos[k], r1[k] );

		// 5) 陷阱：陷阱**不是地形**。只写 Terrain.TRAP 只会得到一块地板皮，
		//    真正让陷阱存在的是下面的 level.setTrap(...)。
		//    setTrap 内部已做 traps.put + GameScene.updateMap，无需再 Painter.set。
		// 毒镖陷阱（PoisonDartTrap：色 GREEN / 形 CROSSHAIR）
		level.setTrap( new PoisonDartTrap(), level.pointToCell( new Point( left + 1, top + 1 ) ) );

		// 冰霜陷阱（FrostTrap：色 WHITE / 形 STARS）
		level.setTrap( new FrostTrap(), level.pointToCell( new Point( left + 2, top + 2 ) ) );

		// 冰霜陷阱（FrostTrap：色 WHITE / 形 STARS）
		level.setTrap( new FrostTrap(), level.pointToCell( new Point( left + 3, top + 3 ) ) );

		// 冰霜陷阱（FrostTrap：色 WHITE / 形 STARS）
		level.setTrap( new FrostTrap(), level.pointToCell( new Point( left + 7, top + 5 ) ) );

		// 警报陷阱（AlarmTrap：色 RED / 形 DOTS）
		level.setTrap( new AlarmTrap(), level.pointToCell( new Point( left + 5, top + 6 ) ) );
		// ↑ 该陷阱在编辑器里标记为「未发现」⇒ 地形为 Terrain.SECRET_TRAP，
		//   生成时保持隐藏；踩上去才会触发并显现。

		// 6) 植物：入参是 Plant.Seed，Level.plant() 内部会 couch() 出 Plant 并补地形。
		//    上游 13 种植物都带同名内部 Seed 类（Rotberry.Seed / Firebloom.Seed …）。
		// 火绽花（Firebloom）
		level.plant( new Firebloom.Seed(), level.pointToCell( new Point( left + 2, top + 2 ) ) );

		// 淡果丛（BlandfruitBush）
		level.plant( new BlandfruitBush.Seed(), level.pointToCell( new Point( left + 7, top + 2 ) ) );

		// 向阳草（Sungrass）
		level.plant( new Sungrass.Seed(), level.pointToCell( new Point( left + 5, top + 5 ) ) );

		// 火绽花（Firebloom）
		level.plant( new Firebloom.Seed(), level.pointToCell( new Point( left + 8, top + 6 ) ) );

		// 7) 道具：道具是独立对象层（Level.heaps 里的 Heap），**地图格不用改**。
		//    level.drop(item, cell) 是唯一入口，落点精确可控。
		//    ⚠️ 别改成 addItemToSpawn()：那条路会进 RegularLevel.createItems()
		//       的随机落点队列（randomDropCell），位置就不可控了。
		//    返回值是 Heap，可链式设堆型（.type = Heap.Type.CHEST 等）。
		// 口粮（Food）
		level.drop( new Food(), level.pointToCell( new Point( left + 6, top + 1 ) ) );

		// 口粮（Food）
		level.drop( new Food(), level.pointToCell( new Point( left + 1, top + 3 ) ) );

		// 升级卷轴（ScrollOfUpgrade）
		level.drop( new ScrollOfUpgrade(), level.pointToCell( new Point( left + 4, top + 4 ) ) ).type = Heap.Type.CHEST;

		// 铁钥匙（按当前深度）（IronKey）
		level.drop( new IronKey( Dungeon.depth ), level.pointToCell( new Point( left + 8, top + 4 ) ) );

		// 暗金（随机量）（DarkGold）
		level.drop( new DarkGold().quantity(Random.NormalIntRange(4, 5)), level.pointToCell( new Point( left + 2, top + 6 ) ) );

		// 金钥匙（按当前深度）（GoldenKey）
		level.drop( new GoldenKey( Dungeon.depth ), level.pointToCell( new Point( left + 6, top + 6 ) ) );

		// 8) 统一设置门的类型（可按需改为 Door.Type.LOCKED 等）
		for (Room.Door door : connected.values()) {
			door.set( Room.Door.Type.REGULAR );
		}
	}
}