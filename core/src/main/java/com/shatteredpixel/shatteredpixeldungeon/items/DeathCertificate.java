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
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.MuseumLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret.SecretDeathProofRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/**
 * 死亡证明（2026-09-09 用户需求）：使用后进入 999 层物品陈列室
 * （{@link MuseumLevel}，30×30 展厅按图鉴陈列全部物品）。
 * <p>获得渠道（用户拍板）：隐藏房（{@link SecretDeathProofRoom}）5% 概率代替
 * 原本的隐藏房型生成——整间变成「死亡证明室」，中央供奉本道具。
 * <p>使用模型：<b>一次性消耗品</b>（成功使用时即从背包消失；在陈列室内
 * 被拒绝使用、或主角已死亡时不消耗）。使用前会记住当前楼层/位置，
 * 在陈列室中拾起任意一件陈列品后自动清场并<b>送回使用前的地点</b>。
 * 本类位于 items 根包，也会被调试窗口「杂项」的浅层扫描自动收录（便于手动测试）。
 */
public class DeathCertificate extends Item {

	public static final String AC_USE = "USE";

	{
		//图标：用户手绘贴图 xy(14,37)，19×21
		image = ItemSpriteSheet.DEATH_CERTIFICATE;
		defaultAction = AC_USE;
	}

	//恒为已辨识状态，无需升级
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
		actions.add(AC_USE);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_USE)) {

			if (!hero.isAlive() || Dungeon.level == null) return;

			//陈列室内不允许再使用（避免把"返程原点"记成陈列室自身）
			if (Dungeon.depth == MuseumLevel.DEPTH) {
				GLog.w(Messages.get(DeathCertificate.class, "already_here"));
				return;
			}

			//记录"使用前的位置"，供陈列室拾取返程使用
			MuseumLevel.pendingReturnDepth = Dungeon.depth;
			MuseumLevel.pendingReturnBranch = Dungeon.branch;
			MuseumLevel.pendingReturnPos = hero.pos;

			hero.sprite.operate(hero.pos);
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
			GLog.i(Messages.get(DeathCertificate.class, "warp"));

			//一次性消耗：成功传送前即从背包移除（陈列室内拒绝使用、主角死亡时不消耗，见上方 return）
			detach(hero.belongings.backpack);

			//统一换层钩子（与楼梯/水晶逃生/隐身草等入口一致，含指令任务判定）
			Level.beforeTransition();

			InterlevelScene.mode = InterlevelScene.Mode.RETURN;
			InterlevelScene.returnDepth = MuseumLevel.DEPTH;
			InterlevelScene.returnBranch = 0;
			//正 cell 直落展厅底部中央：999 层 transitions 为空（纯死路），不能用 pos<0 的 entrance 回退
			InterlevelScene.returnPos = MuseumLevel.LANDING_CELL;
			Game.switchScene(InterlevelScene.class);

		}
	}
}
