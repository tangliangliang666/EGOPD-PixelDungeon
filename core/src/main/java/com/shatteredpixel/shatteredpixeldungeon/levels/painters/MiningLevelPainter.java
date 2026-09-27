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

package com.shatteredpixel.shatteredpixeldungeon.levels.painters;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.DarkGold;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.MineSecretRoom;
import com.watabou.utils.Graph;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashMap;

public class MiningLevelPainter extends CavesPainter {

	@Override
	protected int padding(Level level) {
		return 3;
	}

	private int goldToAdd = 0;

	private int diamondsToAdd = 0; //钻石矿数量（2026-09-04 彩蛋）

	public RegularPainter setGold(int amount){
		goldToAdd = amount;
		return this;
	}

	public MiningLevelPainter setDiamonds(int amount){
		diamondsToAdd = amount;
		return this;
	}

	@Override
	protected void generateGold(Level level, ArrayList<Room> rooms) {
		//we start by counting all the gold purposefully made by rooms
		for (int i = 0; i < level.length(); i++){
			if (level.map[i] == Terrain.WALL_DECO) {
				goldToAdd--;
			}
		}
		for (Heap h : level.heaps.valueList()){
			for (Item i : h.items){
				if (i instanceof DarkGold) goldToAdd -= i.quantity();
			}
		}

		int[] map = level.map;
		do {
			Random.shuffle(rooms);
			for (Room r : rooms) {

				if (r instanceof MineSecretRoom) continue;

				ArrayList<Integer> goldPosCandidates = new ArrayList<>();
				for (Point p : r.getPoints()){
					int i = level.pointToCell(p);

					if (level.insideMap(i) && goldToAdd > 0 && map[i] == Terrain.WALL){

						for (int j : PathFinder.NEIGHBOURS4){
							if (level.insideMap(i+j) && map[i+j] != Terrain.WALL){
								goldPosCandidates.add(i);
								break;
							}
						}
					}
				}

				if (goldToAdd > 0 && !goldPosCandidates.isEmpty()){
					int pos = Random.element(goldPosCandidates);

					map[pos] = Terrain.WALL_DECO;
					goldToAdd--;

					if (goldToAdd > 0){
						int i = PathFinder.NEIGHBOURS4[Random.Int(4)];
						if (level.insideMap(pos+i) && map[pos+i] == Terrain.WALL){
							map[pos+i] = Terrain.WALL_DECO;
							goldToAdd--;
						}
						if (Random.Int(2) == 0){
							i = PathFinder.NEIGHBOURS4[Random.Int(4)];
							if (level.insideMap(pos+i) && map[pos+i] == Terrain.WALL){
								map[pos+i] = Terrain.WALL_DECO;
								goldToAdd--;
							}
						}
					}

				}

			}
		} while (goldToAdd > 0);

		//钻石矿（2026-09-04 彩蛋）：参考金矿逻辑，仅放置在普通房间内
		//"至少一面邻接非墙"的墙体上（即玩家能看到的墙面，不会埋进完全被墙包围的内部）
		if (diamondsToAdd > 0){
			ArrayList<Integer> diamondCandidates = new ArrayList<>();
			for (Room r : rooms){
				if (r instanceof MineSecretRoom) continue; //秘密房入口隐藏，矿放在里面很难被找到
				for (Point p : r.getPoints()){
					int i = level.pointToCell(p);
					if (level.insideMap(i) && map[i] == Terrain.WALL){
						for (int j : PathFinder.NEIGHBOURS4){
							if (level.insideMap(i+j) && map[i+j] != Terrain.WALL){
								diamondCandidates.add(i);
								break;
							}
						}
					}
				}
			}
			Random.shuffle(diamondCandidates);
			for (int i = 0; i < Math.min(diamondsToAdd, diamondCandidates.size()); i++){
				map[diamondCandidates.get(i)] = Terrain.MINE_DIAMOND;
			}
		}

	}

	@Override
	protected void paintDoors(Level l, ArrayList<Room> rooms) {
		HashMap<Room, Room> roomMerges = new HashMap<>();

		float hiddenDoorChance = 0.90f;

		//wall doors will still be wall
		//hidden doors become wall tiles a bit later in painting
		//everything else usually becomes empty, but can be wall sometimes
		for (Room r : rooms) {
			for (Room n : r.connected.keySet()) {

				Room.Door d = r.connected.get(n);
				int door = d.x + d.y * l.width();

				if (d.type == Room.Door.Type.WALL || d.type == Room.Door.Type.HIDDEN){
					l.map[door] = Terrain.WALL;
				} else {
					//some of these are randomly hidden, using the same rules as regular levels
					if (Random.Float() < hiddenDoorChance) {
						d.type = Room.Door.Type.HIDDEN;
						Graph.buildDistanceMap(rooms, r);
						if (n.distance == Integer.MAX_VALUE){
							l.map[door] = Terrain.EMPTY;
							d.type = Room.Door.Type.EMPTY;
						} else {
							l.map[door] = Terrain.WALL;
						}
					} else {
						l.map[door] = Terrain.EMPTY;
						d.type = Room.Door.Type.EMPTY;
					}

				}

				//if the door is empty, always merge the rooms
				if (l.map[door] == Terrain.EMPTY){
					if (roomMerges.get(r) == n || roomMerges.get(n) == r){
						continue;
					} else if (mergeRooms(l, r, n, r.connected.get(n), Terrain.EMPTY)) {
						roomMerges.put(r, n);
						roomMerges.put(n, r);
					}
				}

			}
		}
	}

	@Override
	protected void decorate(Level level, ArrayList<Room> rooms) {

		//「房间自己挖出来的深渊」＝ 水晶任务矿洞的虚空（2026-09-26）：只把**地板**换成深渊
		//（MiningLevel.carveVoid，在房间 paint() 里完成）。先给这批格拍张快照，用来和下面
		//CavesPainter 在**房间之间（原为墙体）**新挖的那批深渊区分开 —— 后者仍然要还原成地面。
		boolean[] roomVoid = new boolean[level.length()];
		if (Blacksmith.Quest.Type() == Blacksmith.Quest.CRYSTAL){
			for (int i = 0; i < level.length(); i++){
				roomVoid[i] = (level.map[i] == Terrain.CHASM);
			}
		}

		super.decorate(level, rooms);

		//no chasms allowed, replace with ground!（房间之间的那批；水晶任务只留房间里的虚空）
		for (int i = 0; i < level.length(); i++){
			if (level.map[i] == Terrain.CHASM && !roomVoid[i]){
				level.map[i] = Terrain.EMPTY;
			}
		}
	}
}
