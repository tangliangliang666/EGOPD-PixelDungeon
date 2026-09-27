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
import com.shatteredpixel.shatteredpixeldungeon.levels.LobTestLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.MuseumLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/**
 * 测试传送门（2026-09-09 用户需求）：使用后立刻抵达 27 层测试层
 * （{@link LobTestLevel}——2026-09-10 起改为<b>普通楼层式地形生成</b> + 新楼层贴图 tiles_lob，仅地形）。
 * <p>用户拍板的模型：<b>无限次使用不消耗</b>；<b>不设掉落/商店/开局获得渠道</b>，
 * 仅能通过调试窗口（F2 / 「调试模式」挑战菜单 → 杂项选卡）调出——
 * 本类位于 items 根包，会被调试控制台「杂项」的浅层扫描自动收录，无需额外注册。
 * <p>跳层走 InterlevelScene.RETURN（同 Fadeleaf / LloydsBeacon 模式），
 * 落点用 {@link LobTestLevel#LANDING_POS}（-1）交给引擎回退到本层入口楼梯，
 * 并使用 Level.beforeTransition() 统一换层钩子。
 */
public class TestPortal extends Item {

	public static final String AC_USE = "USE";

	{
		//临时图标：水晶钥匙（调试期占位，日后可换专属帧）
		image = ItemSpriteSheet.CRYSTAL_KEY;
		defaultAction = AC_USE;
	}

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
		actions.add(AC_USE);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_USE)) {

			//「死亡证明」陈列室内禁一切传送类道具（含本测试传送门）
			if (MuseumLevel.blockTeleport( this )) return;

			if (!hero.isAlive() || Dungeon.level == null) return;

			//无限次使用：不 detach、不消耗回合，使用后立即跳层
			hero.sprite.operate(hero.pos);
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
			GLog.i(Messages.get(TestPortal.class, "warp", LobTestLevel.DEPTH));

			//统一换层钩子（与楼梯/水晶逃生/隐身草等入口一致，含指令任务判定）
			Level.beforeTransition();

			InterlevelScene.mode = InterlevelScene.Mode.RETURN;
			InterlevelScene.returnDepth = LobTestLevel.DEPTH;
			InterlevelScene.returnBranch = 0;
			//27 层为常规生成（有入口 transition）：-1 交由 Dungeon.switchLevel 回退到入口楼梯，
			//无需再固定落点 cell（旧的圆形单房间层 transitions 为空才必须直落圆心）
			InterlevelScene.returnPos = LobTestLevel.LANDING_POS;
			Game.switchScene(InterlevelScene.class);

		}
	}
}
