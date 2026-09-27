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
import com.shatteredpixel.shatteredpixeldungeon.Bones;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Bat;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalWisp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.FungalSpinner;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollGuard;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Torch;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.DarkGold;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.Builder;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.FigureEightBuilder;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.MiningLevelPainter;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.MineEntrance;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.MineGiantRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.MineLargeRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.MineSecretRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.MineSmallRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.BlacksmithSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.Tilemap;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;

public class MiningLevel extends CavesLevel {

	@Override
	public String tilesTex() {
		switch (Blacksmith.Quest.Type()){
			default:
				return Assets.Environment.TILES_CAVES;
			case Blacksmith.Quest.CRYSTAL:
				return Assets.Environment.TILES_CAVES_CRYSTAL;
			case Blacksmith.Quest.GNOLL:
				return Assets.Environment.TILES_CAVES_GNOLL;
		}

	}

	@Override
	public void playLevelMusic() {
		Music.INSTANCE.play(Assets.Music.CAVES_TENSE, true);
	}

	@Override
	protected ArrayList<Room> initRooms() {
		ArrayList<Room> initRooms = new ArrayList<>();
		initRooms.add ( roomEntrance = new MineEntrance());

		//spawns 1 giant, 3 large, 6-8 small, and 2 secret cave rooms
		StandardRoom s;
		s = new MineGiantRoom();
		s.setSizeCat();
		initRooms.add(s);

		int rooms = 3;
		for (int i = 0; i < rooms; i++){
			s = new MineLargeRoom();
			s.setSizeCat();
			initRooms.add(s);
		}

		rooms = Random.NormalIntRange(6, 8);
		for (int i = 0; i < rooms; i++){
			s = new MineSmallRoom();
			s.setSizeCat();
			initRooms.add(s);
		}

		rooms = 2;
		for (int i = 0; i < rooms; i++){
			initRooms.add(new MineSecretRoom());
		}

		return initRooms;
	}

	@Override
	protected Builder builder() {
		return new FigureEightBuilder().setPathLength(0.8f, new float[]{1}).setTunnelLength(new float[]{1}, new float[]{1});
	}

	@Override
	protected boolean build() {
		if (super.build()){
			CustomTilemap vis = new BorderTopDarken();
			vis.setRect(0, 0, width, 1);
			customTiles.add(vis);

			vis = new BorderWallsDarken();
			vis.setRect(0, 0, width, height);
			customWalls.add(vis);

			return true;
		}
		return false;
	}

	@Override
	protected Painter painter() {
		return new MiningLevelPainter()
				.setDiamonds(3) //钻石矿×3（2026-09-04 彩蛋）
				.setGold(Random.NormalIntRange(45, 47))
				.setWater(Blacksmith.Quest.Type() == Blacksmith.Quest.FUNGI ? 0.1f : 0.35f, 6)
				.setGrass(Blacksmith.Quest.Type() == Blacksmith.Quest.FUNGI ? 0.65f : 0.10f, 3);
	}

	@Override
	public int mobLimit() {
		//1 fewer than usual
		return super.mobLimit()-1;
	}

	@Override
	public Mob createMob() {
		switch (Blacksmith.Quest.Type()){
			default:
				return new Bat();
			case Blacksmith.Quest.CRYSTAL:
				return new CrystalWisp();
			case Blacksmith.Quest.GNOLL:
				return new GnollGuard();
			case Blacksmith.Quest.FUNGI:
				return new FungalSpinner();
		}
	}

	@Override
	public float respawnCooldown() {
		//normal enemies respawn much more slowly here
		return 3*TIME_TO_RESPAWN;
	}

	@Override
	protected void createItems() {
		Random.pushGenerator(Random.Long());
			ArrayList<Item> bonesItems = Bones.get();
			if (bonesItems != null) {
				int cell = randomDropCell();
				if (map[cell] == Terrain.HIGH_GRASS || map[cell] == Terrain.FURROWED_GRASS) {
					map[cell] = Terrain.GRASS;
					losBlocking[cell] = false;
				}
				for (Item i : bonesItems) {
					drop(i, cell).setHauntedIfCursed().type = Heap.Type.REMAINS;
				}
			}
		Random.popGenerator();

		int cell = randomDropCell();
		if (map[cell] == Terrain.HIGH_GRASS || map[cell] == Terrain.FURROWED_GRASS) {
			map[cell] = Terrain.GRASS;
			losBlocking[cell] = false;
		}
		drop( Generator.randomUsingDefaults(Generator.Category.FOOD), cell );
		if (Blacksmith.Quest.Type() == Blacksmith.Quest.GNOLL){
			//drop a second ration for the gnoll quest type, more mining required!
			cell = randomDropCell();
			if (map[cell] == Terrain.HIGH_GRASS || map[cell] == Terrain.FURROWED_GRASS) {
				map[cell] = Terrain.GRASS;
				losBlocking[cell] = false;
			}
			drop( Generator.randomUsingDefaults(Generator.Category.FOOD), cell );
		}

		if (Dungeon.isChallenged(Challenges.DARKNESS)){
			cell = randomDropCell();
			if (map[cell] == Terrain.HIGH_GRASS || map[cell] == Terrain.FURROWED_GRASS) {
				map[cell] = Terrain.GRASS;
				losBlocking[cell] = false;
			}
			drop( new Torch(), cell );
		}
	}

	@Override
	protected int randomDropCell() {
		//avoid placing random items next to hazards
		return randomDropCell(MineSmallRoom.class);
	}

	@Override
	public String tileName( int tile ) {
		switch (tile) {
			case Terrain.MINE_CRYSTAL:
				return Messages.get(MiningLevel.class, "crystal_name");
			case Terrain.MINE_BOULDER:
				return Messages.get(MiningLevel.class, "boulder_name");
			case Terrain.MINE_DIAMOND:
				return Messages.get(MiningLevel.class, "diamond_name");
			default:
				return super.tileName( tile );
		}
	}

	@Override
	public boolean activateTransition(Hero hero, LevelTransition transition) {
		if (transition.type == LevelTransition.Type.BRANCH_ENTRANCE
				&& !Blacksmith.Quest.completed()) {

			if (hero.belongings.getItem(Pickaxe.class) == null){
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						GameScene.show(new WndTitledMessage( new BlacksmithSprite(),
								Messages.titleCase(Messages.get(Blacksmith.class, "name")),
								Messages.get(Blacksmith.class, "lost_pick")));
					}
				});
				return false;
			}

			String warnText;
			DarkGold gold = hero.belongings.getItem(DarkGold.class);
			int goldAmount = gold == null ? 0 : gold.quantity();
			if (goldAmount < 10){
				warnText = Messages.get(Blacksmith.class, "exit_warn_none");
			} else if (goldAmount < 20){
				warnText = Messages.get(Blacksmith.class, "exit_warn_low");
			} else if (goldAmount < 30){
				warnText = Messages.get(Blacksmith.class, "exit_warn_med");
			} else if (goldAmount < 40){
				warnText = Messages.get(Blacksmith.class, "exit_warn_high");
			} else {
				warnText = Messages.get(Blacksmith.class, "exit_warn_full");
			}

			if (!Blacksmith.Quest.bossBeaten()){
				switch (Blacksmith.Quest.Type()){
					case Blacksmith.Quest.CRYSTAL: warnText += "\n\n" + Messages.get(Blacksmith.class, "exit_warn_crystal"); break;
					case Blacksmith.Quest.GNOLL: warnText += "\n\n" + Messages.get(Blacksmith.class, "exit_warn_gnoll"); break;
					case Blacksmith.Quest.FUNGI: warnText += "\n\n" + Messages.get(Blacksmith.class, "exit_warn_fungi"); break;
				}
			}

			String finalWarnText = warnText;
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show(new WndOptions( new BlacksmithSprite(),
							Messages.titleCase(Messages.get(Blacksmith.class, "name")),
							finalWarnText,
							Messages.get(Blacksmith.class, "exit_yes"),
							Messages.get(Blacksmith.class, "exit_no")){
						@Override
						protected void onSelect(int index) {
							if (index == 0){
								Blacksmith.Quest.complete();
								MiningLevel.super.activateTransition(hero, transition);
							}
						}
					} );
				}
			});
			return false;

		} else {
			return super.activateTransition(hero, transition);
		}
	}

	@Override
	public String tileDesc( int tile ) {
		switch (tile) {
			case Terrain.WALL:
				return Messages.get(MiningLevel.class, "wall_desc");
			case Terrain.WALL_DECO:
				return super.tileDesc(tile) + "\n\n" +  Messages.get(MiningLevel.class, "gold_extra_desc");
			case Terrain.MINE_CRYSTAL:
				return Messages.get(MiningLevel.class, "crystal_desc");
			case Terrain.MINE_BOULDER:
				return Messages.get(MiningLevel.class, "boulder_desc");
			case Terrain.MINE_DIAMOND:
				return Messages.get(MiningLevel.class, "diamond_desc");
			case Terrain.BARRICADE:
				return Messages.get(MiningLevel.class, "barricade_desc");
			default:
				return super.tileDesc( tile );
		}
	}

	@Override
	public Group addVisuals() {
		super.addVisuals();
		visuals.clear(); //we re-add these in wall visuals
		return visuals;
	}

	@Override
	public Group addWallVisuals() {
		super.addWallVisuals();
		CavesLevel.addCavesVisuals(this, wallVisuals, true);
		return wallVisuals;
	}

	@Override
	public boolean invalidHeroPos(int tile) {
		return false; //solid tiles are fine for hero to be in here
	}

	/**
	 * 水晶任务矿洞层的「虚空」（深渊）地形（2026-09-26）。
	 * <p>在给定房间的地板上挖出一个紧凑的圆盘状深渊：</p>
	 * <ul>
	 *   <li><b>替代地板、不替代墙体</b>：只有地板（{@code EMPTY} / {@code EMPTY_DECO}）会被换成深渊，
	 *       墙体与矿脉（{@code MINE_CRYSTAL}）一格不动 —— 这是「不是墙体」这条要求的落点。</li>
	 *   <li><b>不封死任何人</b>：整块圆盘都夹在「深内区」（房间内圈再各缩 2 格）之内，外面因此永远留着
	 *       一整圈地板 ⇒ 门口内侧与绕行路线必定还通，也不会出现「贴着墙走一步就掉下去」；圆盘又与
	 *       {@code protectedCell}（房间自己放的关键单位）保持 {@code radius+1} 以上的距离 ⇒ 它的落脚点
	 *       与八邻格（切比雪夫距离 ≥ 2）一定是地板。凸圆盘也不可能把谁「圈」在里面。</li>
	 *   <li><b>单块</b>：外圈抽掉一半之后还会把「四邻皆空」的孤立格收回 ⇒ 挖出来必定是一个
	 *       4-连通块，不会在角落留下孤零零的单格陷阱。</li>
	 * </ul>
	 * <p>调用时机＝房间自己的 {@code paint()} 阶段（不是画师的 {@code decorate()}）：这样后续的
	 * 水面/草皮/陷阱/掉物绘制都看得见深渊 —— 原版那几处只往 {@code EMPTY} 上画，会自动避开，
	 * 不会出现「陷阱/战利品悬在虚空上」。</p>
	 *
	 * <p>⚠️ 这里只写 {@code map[]}（{@link Painter#set(Level,int,int)} 不碰 {@code pit[]} /
	 * {@code passable[]}），是**有意为之**且安全的：{@code Level.create()} 的顺序是
	 * {@code while(!build())} → {@code buildFlagMaps()} → {@code cleanWalls()}，
	 * 而 {@code paint()}（连带 {@code decorate()}）全在 {@code build()} 里 ⇒ 全部画完之后
	 * {@link Level#buildFlagMaps()} 会**统一**从 {@code map[]} 重算 {@code pit[]} / {@code passable[]} 等。
	 * 反过来说：**任何在 {@code create()} 之后**改地形的代码，就必须改调 {@code Level.set(...)} /
	 * {@code updateCellFlags(...)}，否则会得到「画着是深渊、却踩得上去」的幽灵格。</p>
	 *
	 * @param protectedCell 房间自己放下的关键单位所在格（没有就传 -1）：圆盘会绕开它及其邻域。
	 */
	public static void carveVoid( Level level, Room room, int protectedCell ){

		int w = level.width();
		int[] map = level.map;

		//深内区：房间内圈再各缩 2 格 ⇒ 圆盘夹在这里面，外面留着一整圈地板
		int left = room.left + 2, right = room.right - 2;
		int top = room.top + 2, bottom = room.bottom - 2;
		if (right < left || bottom < top) return;

		int radius = Math.min( right - left, bottom - top ) / 3;
		radius = Math.max( 1, Math.min( 3, radius ) );

		int px = -1, py = -1;
		if (protectedCell >= 0){
			px = protectedCell % w;
			py = protectedCell / w;
		}

		//圆心备选：深内区里、与关键单位保持 radius+1 以上距离的格（圆盘因此绝不碰到它）
		ArrayList<Integer> centers = new ArrayList<>();
		for (int y = top; y <= bottom; y++){
			for (int x = left; x <= right; x++){
				if (px >= 0 && Math.max( Math.abs( x - px ), Math.abs( y - py ) ) <= radius + 1) continue;
				centers.add( x + y * w );
			}
		}
		if (centers.isEmpty()) return;

		int center = Random.element( centers );
		int cx = center % w, cy = center / w;

		//逐格：切比雪夫距离 ≤ radius 的地板才挖（边缘不规则，照 FissureRoom 的最外圈 50% 留地板）。
		//分两趟是为了保证挖出来一定是**单一连通块**：否则外圈那两个「相邻格都被抽掉」的角落格
		//会变成孤零零的单格陷阱（FissureRoom 是在房间里拉长线，没这个问题）。
		ArrayList<Integer> fringe = new ArrayList<>();
		for (int y = cy - radius; y <= cy + radius; y++){
			for (int x = cx - radius; x <= cx + radius; x++){
				//整块圆盘夹在深内区里 ⇒ 外面必留一整圈地板：既不贴墙（沿墙走一步就掉下去），
				//也不吞掉门口内侧那格。夹掉的部分不参与随机数消耗。
				if (x < left || x > right || y < top || y > bottom) continue;
				if (!level.insideMap( x + y * w )) continue;
				int d = Math.max( Math.abs( x - cx ), Math.abs( y - cy ) );
				if (d > radius) continue;
				int cell = x + y * w;
				if (map[cell] != Terrain.EMPTY && map[cell] != Terrain.EMPTY_DECO) continue;
				if (level.heaps.get( cell ) != null) continue;
				if (d == radius){
					//外圈：一半概率留地板；抽中的先存着，等实心核挖完再判它有没有邻居
					if (Random.Int(2) == 0) continue;
					fringe.add( cell );
				} else {
					Painter.set( level, cell, Terrain.CHASM );
				}
			}
		}

		//第二趟：外圈格只有「四邻里至少有一个也是深渊」时才真的挖 ⇒ 不会留孤立碎块。
		//（地图形状保证内外各留了一圈，这里的 ±1 / ±w 一定在数组内。）
		for (int cell : fringe){
			if (map[cell - 1] == Terrain.CHASM || map[cell + 1] == Terrain.CHASM
					|| map[cell - w] == Terrain.CHASM || map[cell + w] == Terrain.CHASM){
				Painter.set( level, cell, Terrain.CHASM );
			}
		}
	}

	/**
	 * 水晶任务矿洞层：掉进深渊**不换层** —— 一股魔法乱流把英雄送回本层某处并结算坠落伤害。
	 * 见 {@code Chasm.heroFall} 里那条分支与本方法的实现。
	 */
	@Override
	public boolean handlesChasmFall(){
		return Blacksmith.Quest.Type() == Blacksmith.Quest.CRYSTAL;
	}

	public static class BorderTopDarken extends CustomTilemap {

		{
			texture = Assets.Environment.CAVES_QUEST;
		}

		@Override
		public Tilemap create() {
			Tilemap v = super.create();
			int[] data = new int[tileW*tileH];
			Arrays.fill(data, 1);
			v.map( data, tileW );
			return v;
		}

		@Override
		public Image image(int tileX, int tileY) {
			return null;
		}
	}

	public static class BorderWallsDarken extends CustomTilemap {

		{
			texture = Assets.Environment.CAVES_QUEST;
		}

		@Override
		public Tilemap create() {
			Tilemap v = super.create();
			int[] data = new int[tileW*tileH];
			for (int i = 0; i < data.length; i++){
				if (i % tileW == 0 || i % tileW == tileW-1){
					data[i] = 1;
				} else if (i + 2*tileW > data.length) {
					data[i] = 2;
				} else {
					data[i] = -1;
				}
			}
			v.map( data, tileW );
			return v;
		}

		@Override
		public Image image(int tileX, int tileY) {
			return null;
		}
	}
}
