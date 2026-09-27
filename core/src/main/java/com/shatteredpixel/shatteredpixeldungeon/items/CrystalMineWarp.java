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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.MiningLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.MuseumLevel;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * 矿洞跃迁符（2026-09-26 用户需求）：调试用，反复查看<b>水晶任务矿洞层的地形生成</b>
 * （尤其是 {@link MiningLevel#carveVoid} 挖出来的虚空）。
 *
 * <p>用户拍板的模型（与 {@link TestPortal} 同一套）：<b>无限次使用不消耗</b>；
 * <b>不设掉落/商店/开局获得渠道</b>，只能从调试窗口（F2 → 杂项）调出 ——
 * 本类位于 {@code items} 根包，会被浅层扫描自动收录，无需额外注册。</p>
 *
 * <p>三个动作：</p>
 * <ul>
 *   <li>{@link #AC_WARP}「跃迁」：进入矿洞，深度在 11→12→13→14 之间轮换。
 *       同一深度的地形是<b>可复现</b>的（引擎按 {@code Dungeon.seedCurDepth()} 生成），
 *       所以适合「改完之后再进一次同一层」做前后对照。</li>
 *   <li>{@link #AC_REROLL}「重 roll」：把目标层的生成记录清掉，逼引擎<b>重新生成</b>，
 *       于是每次都能看到新地形。轮换深度的规则与「跃迁」相同。</li>
 *   <li>{@link #AC_RETURN}「返回」：回到出发前那一层的原坐标。只在矿层里出现，
 *       因为矿层唯一的出口楼梯被任务流程锁着（没镐子不让走），不给回程会卡住。</li>
 * </ul>
 *
 * <p>⚠️ 两个必须知道的副作用（都是为了让虚空真的长出来）：</p>
 * <ol>
 *   <li>虚空地形只在 {@code Blacksmith.Quest.Type() == CRYSTAL} 下生成
 *       （房间的 {@code paint()} 与 {@link MiningLevel#handlesChasmFall()} 都读它），
 *       所以本道具会<b>临时把任务种类顶成水晶</b>。「返回」时按离开前的值还原。</li>
 *   <li>「重 roll」会 {@code Dungeon.generatedLevels.remove(...)}：如果这一局里真的玩到过该矿层，
 *       那一层的已探索地图会随之作废（换成新地形）。调试道具，属预期行为。</li>
 * </ol>
 *
 * <p>跳层一律走 {@code InterlevelScene.Mode.RETURN}（同 {@link TestPortal} / 传送卷轴），
 * 并用 {@link Level#beforeTransition()} 统一换层钩子。</p>
 */
public class CrystalMineWarp extends Item {

	public static final String AC_WARP   = "WARP";
	public static final String AC_REROLL = "REROLL";
	public static final String AC_RETURN = "RETURN";

	//矿洞分支：Dungeon.newLevel() 里 branch==1 且 depth∈[11,14] ⇒ MiningLevel
	public static final int FIRST_MINE_DEPTH = 11;
	public static final int LAST_MINE_DEPTH  = 14;
	public static final int MINE_BRANCH      = 1;

	{
		//临时图标：逃脱棱晶（同为传送类道具，调试期占位，日后可换专属帧）
		image = ItemSpriteSheet.ESCAPE;
		defaultAction = AC_WARP;
	}

	/**
	 * 出发前的坐标（用于「返回」）。{@code fromDepth <= 0} 表示还没记录过。
	 * 只在矿层<b>之外</b>使用「跃迁」时才记，所以反复在矿层里跳不会把「来处」冲掉。
	 */
	public int fromDepth     = 0;
	public int fromBranch    = 0;
	public int fromPos       = -1;
	/** 出发前的任务种类，「返回」时还原（用完之后不留副作用）。 */
	public int fromQuestType = 0;

	/** 下一次「跃迁」要去的矿层深度：11→12→13→14→11 轮换。 */
	public int nextDepth = FIRST_MINE_DEPTH;

	//测试道具：恒为已辨识状态，无需升级
	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_WARP);
		actions.add(AC_REROLL);
		//回程只在矿层里给：矿层唯一的出口楼梯要「镐子 + 任务进度」，调试状态下走不通
		if (fromDepth > 0 && Dungeon.level instanceof MiningLevel){
			actions.add(AC_RETURN);
		}
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (AC_WARP.equals(action) || AC_REROLL.equals(action)) {

			//「死亡证明」陈列室内禁一切传送类道具（含本符）
			if (MuseumLevel.blockTeleport( this )) return;

			if (!hero.isAlive() || Dungeon.level == null) return;

			if (!Dungeon.interfloorTeleportAllowed()){
				GLog.w( Messages.get( ScrollOfTeleportation.class, "no_tele" ) );
				return;
			}

			//记录来处：只在矿层之外记，免得把矿层自己的坐标当成「来处」
			if (!(Dungeon.level instanceof MiningLevel)){
				fromDepth     = Dungeon.depth;
				fromBranch    = Dungeon.branch;
				fromPos       = hero.pos;
				fromQuestType = Blacksmith.Quest.Type();
			}

			//虚空地形只在「水晶任务」下生成（房间 paint() 与 handlesChasmFall() 都读它）
			Blacksmith.Quest.setType( Blacksmith.Quest.CRYSTAL );

			int depth = nextDepth;
			nextDepth = (depth >= LAST_MINE_DEPTH) ? FIRST_MINE_DEPTH : depth + 1;

			//「重 roll」：清掉该层的生成记录 ⇒ returnTo() 走 newLevel() 而不是 loadLevel()
			if (AC_REROLL.equals(action)){
				Dungeon.generatedLevels.remove( Integer.valueOf( depth + 1000 * MINE_BRANCH ) );
			}

			//无限次使用：不 detach、不消耗回合
			hero.sprite.operate(hero.pos);
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
			GLog.i( Messages.get( this, AC_REROLL.equals(action) ? "reroll" : "warp" ) );

			//统一换层钩子（与楼梯/水晶逃生/测试传送门一致，含指令任务判定）
			Level.beforeTransition();

			InterlevelScene.mode = InterlevelScene.Mode.RETURN;
			InterlevelScene.returnDepth  = depth;
			InterlevelScene.returnBranch = MINE_BRANCH;
			//-1 ⇒ 交给 Dungeon.switchLevel 回退到该层入口（矿层入口就是 MineEntrance 那格楼梯）
			InterlevelScene.returnPos    = -1;
			Game.switchScene( InterlevelScene.class );

		} else if (AC_RETURN.equals(action)) {

			if (fromDepth <= 0 || Dungeon.level == null) return;

			if (!hero.isAlive()) return;

			//还原任务种类：本道具对存档的唯一副作用，走的时候一并还回去
			Blacksmith.Quest.setType( fromQuestType );

			hero.sprite.operate(hero.pos);
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
			GLog.i( Messages.get( this, "back" ) );

			Level.beforeTransition();

			InterlevelScene.mode = InterlevelScene.Mode.RETURN;
			InterlevelScene.returnDepth  = fromDepth;
			InterlevelScene.returnBranch = fromBranch;
			InterlevelScene.returnPos    = fromPos;
			Game.switchScene( InterlevelScene.class );
		}
	}

	private static final String FROM_DEPTH  = "from_depth";
	private static final String FROM_BRANCH = "from_branch";
	private static final String FROM_POS    = "from_pos";
	private static final String FROM_QUEST  = "from_quest";
	private static final String NEXT_DEPTH  = "next_depth";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put( FROM_DEPTH,  fromDepth );
		bundle.put( FROM_BRANCH, fromBranch );
		bundle.put( FROM_POS,    fromPos );
		bundle.put( FROM_QUEST,  fromQuestType );
		bundle.put( NEXT_DEPTH,  nextDepth );
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		fromDepth     = bundle.getInt( FROM_DEPTH );
		fromBranch    = bundle.getInt( FROM_BRANCH );
		fromPos       = bundle.getInt( FROM_POS );
		fromQuestType = bundle.getInt( FROM_QUEST );
		nextDepth     = bundle.getInt( NEXT_DEPTH );
	}
}
