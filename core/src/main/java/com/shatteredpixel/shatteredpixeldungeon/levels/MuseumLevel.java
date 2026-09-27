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
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop;
import com.shatteredpixel.shatteredpixeldungeon.items.EnergyCrystal;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Honeypot;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.LiquidMetal;
import com.shatteredpixel.shatteredpixeldungeon.items.Stylus;
import com.shatteredpixel.shatteredpixeldungeon.items.Torch;
import com.shatteredpixel.shatteredpixeldungeon.items.TestPortal;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.LloydsBeacon;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.DarkGold;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.EscapeCrystal;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPassage;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.BeaconOfReturning;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.PhaseShift;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.TelekineticGrab;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfBlink;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.DwarfToken;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Embers;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.GooBlob;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.MetalShard;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.TrinketCatalyst;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.DisplacingDart;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * 999 层物品陈列室（2026-09-09 用户需求：道具「死亡证明」跳入）。
 * <p>30×30 的方形展厅（32×32 地图、四周 1 圈墙），按游戏「图鉴」（{@link Catalog}）
 * 的分类与登记顺序，把各类物品<b>一行一行依序陈列</b>（行与行之间隔开一行），全部
 * <b>已鉴定·无诅咒·+0</b>；杂项行按用户点名清单生成（金币×1000、能量×20、液金×50 等）。
 * <p>继承 {@link DeadEndLevel} 复用其安全默认：无怪、无重生点、不计入 generatedLevels /
 * deepestFloor / 通关统计；拾起任意一件陈列品即触发 {@link #onExhibitPickedUp()}：
 * 其余地面物品全部消失，并把英雄送回<b>使用「死亡证明」之前的位置</b>。
 */
public class MuseumLevel extends DeadEndLevel {

	/** 所在深度（与 Dungeon.newLevel() 的 case 999 保持一致）。 */
	public static final int DEPTH = 999;

	/** 地图尺寸：32×32，四周 1 圈墙，内部展厅 30×30（x/y 均为 1..30）。 */
	private static final int MAP_W = 32;
	private static final int MAP_H = 32;

	/** 内部展厅范围（含）。 */
	private static final int FLOOR_MIN = 1;
	private static final int FLOOR_MAX = 30;

	/** 英雄落点：展厅底部中央（第 30 行第 15 列）。 */
	private static final int LANDING_X = 15;
	private static final int LANDING_Y = 30;
	public static final int LANDING_CELL = LANDING_X + LANDING_Y * MAP_W;

	//由「死亡证明」在使用时写入；MuseumLevel 首次 createItems() 时读到实例字段存档
	public static int pendingReturnDepth = 26;
	public static int pendingReturnBranch = 0;
	public static int pendingReturnPos = -1;

	private int returnDepth = 26;
	private int returnBranch = 0;
	private int returnPos = -1;

	{
		//与 SewerLevel 相同的迷雾/地图着色（材质暂用下水道同款）
		color1 = 0x48763c;
		color2 = 0x59994a;
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_SEWERS;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_SEWERS;
	}

	@Override
	protected boolean build() {

		setSize(MAP_W, MAP_H);

		//地图默认已全部填 WALL（Level.setSize）：把内部 30×30 展区铺为普通地板
		for (int y = FLOOR_MIN; y <= FLOOR_MAX; y++) {
			for (int x = FLOOR_MIN; x <= FLOOR_MAX; x++) {
				map[y * MAP_W + x] = Terrain.EMPTY;
			}
		}

		return true;
	}

	@Override
	protected void createItems() {

		//把「死亡证明」记录的来源位置收进实例字段（供拾取返程 & 存档）
		this.returnDepth = pendingReturnDepth;
		this.returnBranch = pendingReturnBranch;
		this.returnPos = pendingReturnPos;

		//陈列布局游标：从展厅左上 (1,1) 开始，向右逐格放置，行满后下移两行（间隔一行留白）
		curX = FLOOR_MIN;
		curY = FLOOR_MIN;

		//—— 装备类（顺序对齐图鉴 equipmentCatalogs，跳过附魔/雕文/杂项装备）——
		section(Catalog.MELEE_WEAPONS);                    //近战武器
		section(Catalog.ARMOR, true);                      //盔甲（自动排除英雄专属 ClassArmor）
		sectionMissiles();                                 //投掷武器（各生成一组：默认叠数）
		section(Catalog.WANDS);                            //法杖
		section(Catalog.RINGS);                            //戒指
		section(Catalog.ARTIFACTS, true);                  //神器（含骷髅钥匙：唯一注册进神器图鉴的钥匙类）
		section(Catalog.TRINKETS);                         //饰品

		//—— 消耗品类（顺序对齐图鉴 consumableCatalogs，杂项单独处理）——
		section(Catalog.POTIONS);                          //药剂
		section(Catalog.SCROLLS);                          //卷轴
		section(Catalog.SEEDS);                            //种子
		section(Catalog.STONES);                           //符石
		section(Catalog.FOOD);                             //食物
		section(Catalog.EXOTIC_POTIONS);                   //秘药
		section(Catalog.EXOTIC_SCROLLS);                   //秘卷
		section(Catalog.BOMBS);                            //炸弹
		section(Catalog.TIPPED_DARTS);                     //涂药飞镖
		section(Catalog.BREWS_ELIXIRS);                    //合剂/灵剂
		section(Catalog.SPELLS);                           //法术

		//—— 杂项行（2026-09-09 用户点名清单；其余钥匙/任务/遗物等不放）——
		put(new Gold().quantity(1000));                    //金币 ×1000
		put(new EnergyCrystal().quantity(20));             //能量晶体 ×20
		put(new Dewdrop());                                //露珠
		put(new TrinketCatalyst());                        //魔能触媒
		put(new Stylus());                                 //奥术刻笔
		put(new DarkGold());                               //暗金
		put(new Torch());                                  //火把
		put(new Honeypot());                               //蜂蜜罐
		put(new Honeypot.ShatteredPot());                  //破碎的蜂蜜罐
		put(new Ankh());                                   //重生十字章
		put(new Embers());                                 //元素余烬
		put(new GooBlob());                                //粘咕球
		put(new MetalShard());                             //邪能碎片
		put(new LiquidMetal().quantity(50));               //液金 ×50
		put(new DwarfToken());                             //矮人徽记

		//清理静态临存，避免影响下次进入
		pendingReturnDepth = 26;
		pendingReturnBranch = 0;
		pendingReturnPos = -1;
	}

	private int curX, curY;

	//按图鉴登记顺序陈列一个分类（可带过滤）
	private void section(Catalog cat) {
		section(cat, false);
	}

	private void section(Catalog cat, boolean filter) {
		if (cat == null) return;
		for (Class<?> c : cat.items()) {
			if (filter && skip(c)) continue;
			Item it = (Item) Reflection.newInstance(c);
			if (it != null) put(it);
		}
	}

	//投掷武器：每个种类生成"一组"（默认叠数）
	private void sectionMissiles() {
		for (Class<?> c : Catalog.THROWN_WEAPONS.items()) {
			Item it = (Item) Reflection.newInstance(c);
			if (it instanceof MissileWeapon) {
				((MissileWeapon) it).quantity(((MissileWeapon) it).defaultQuantity());
			}
			if (it != null) put(it);
		}
	}

	/**
	 * 不陈列的装备类。
	 *
	 * <p>只过滤<b>英雄专属盔甲</b>（WarriorArmor / MageArmor / RogueArmor / HuntressArmor /
	 * DuelistArmor / ClericArmor）——它们挂在 {@code Generator.Category.ARMOR.classes} 里，
	 * 但对别的职业没有意义，陈列出来只占格子。</p>
	 *
	 * <p><b>{@code SkeletonKey} 不再排除</b>（2026-09-24 用户要求）：它同时注册在
	 * {@code Generator.Category.ARTIFACT.classes}（神器图鉴里有它一格），先前这里把它一起
	 * 过滤掉，于是陈列室里<b>唯独缺这一件神器</b>。它只是「长得像钥匙的神器」，本身就是普通神器，
	 * 陈列 / 拾取 / 返程流程都不受影响。</p>
	 */
	private boolean skip(Class<?> c) {
		return ClassArmor.class.isAssignableFrom(c);
	}

	private void put(Item it) {
		if (it == null) return;
		//陈列品统一：已鉴定、无诅咒、+0
		it.identify();
		it.cursed = false;

		if (curX > FLOOR_MAX) {
			curX = FLOOR_MIN;
			curY += 2;   //行与行之间间隔一行
		}
		//底部留出行走区（LANDING 在第 30 行），陈列最多铺到第 28 行
		if (curY > FLOOR_MAX - 2) {
			//极端溢出保护：压缩到仅剩的可用行（正常清单不会触发）
			curY = FLOOR_MAX - 2;
		}

		drop(it, curY * MAP_W + curX);
		curX++;
	}

	//==========================================================================
	// 传送禁用（2026-09-12 用户需求）
	//==========================================================================

	/**
	 * 陈列室内被禁用的传送类道具清单。
	 *
	 * <p>999 层是<b>死路布局</b>（无楼梯、无重生点），而「返程」完全依赖
	 * {@link com.shatteredpixel.shatteredpixeldungeon.items.DeathCertificate} 记下的原点
	 * 与「拾起展品」触发：任何把英雄或物品挪走的效果都会让落点超出预期，甚至打断返程流程。</p>
	 *
	 * <p>用 {@code instanceof} 而非类名比较，子类同样命中；清单集中在这里，日后要增删只改本方法。
	 * 覆盖范围：传送卷轴 / 秘卷·通道 / 念力结晶 / 转移结晶 / 返回晶柱 / 劳埃德信标 /
	 * 闪现符石 / 传送飞镖 / 逃脱棱晶 / 测试传送门。</p>
	 */
	public static boolean isTeleportBlocked( Item item ) {
		return item instanceof ScrollOfTeleportation
				|| item instanceof ScrollOfPassage
				|| item instanceof TelekineticGrab
				|| item instanceof PhaseShift
				|| item instanceof BeaconOfReturning
				|| item instanceof LloydsBeacon
				|| item instanceof StoneOfBlink
				|| item instanceof DisplacingDart
				|| item instanceof EscapeCrystal
				|| item instanceof TestPortal;
	}

	/**
	 * 使用入口的统一拦截：在陈列室内，传送类道具一律不可使用——<b>不消耗、不生效</b>，
	 * 只给一条提示。各道具在「开始生效之前」（卷轴阅读、法术施放、投掷、动作执行）调用本方法。
	 *
	 * @return true 表示本次使用应中止（提示已打印），调用方直接 return 即可。
	 */
	public static boolean blockTeleport( Item item ) {
		if (!(Dungeon.level instanceof MuseumLevel)) return false;
		if (item == null || !isTeleportBlocked( item )) return false;

		GLog.w( Messages.get( MuseumLevel.class, "no_teleport", item.name() ) );
		return true;
	}

	//纯死路语义：入口锚点仅用于 resetLevel 等兜底，避免空 transitions 下 entrance()==0
	@Override
	public int entrance() {
		return LANDING_CELL;
	}

	//拾起任意陈列品 → 其余地面物品全部消失，并把英雄送回使用「死亡证明」之前的位置
	public void onExhibitPickedUp() {
		//清空其余地面物品（destroy 会从 heaps 中移除自身，故先做快照）
		for (Heap h : new ArrayList<>(heaps.valueList())) {
			h.destroy();
		}
		heaps.clear();

		if (returnPos < 0) returnPos = LANDING_CELL;

		Level.beforeTransition();

		InterlevelScene.mode = InterlevelScene.Mode.RETURN;
		InterlevelScene.returnDepth = returnDepth;
		InterlevelScene.returnBranch = returnBranch;
		InterlevelScene.returnPos = returnPos;
		Game.switchScene(InterlevelScene.class);
	}

	private static final String RETURN_DEPTH  = "museum_return_depth";
	private static final String RETURN_BRANCH = "museum_return_branch";
	private static final String RETURN_POS    = "museum_return_pos";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(RETURN_DEPTH, returnDepth);
		bundle.put(RETURN_BRANCH, returnBranch);
		bundle.put(RETURN_POS, returnPos);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		returnDepth = bundle.getInt(RETURN_DEPTH);
		returnBranch = bundle.getInt(RETURN_BRANCH);
		returnPos = bundle.getInt(RETURN_POS);
	}
}
