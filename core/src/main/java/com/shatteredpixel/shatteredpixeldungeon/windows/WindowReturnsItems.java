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

package com.shatteredpixel.shatteredpixeldungeon.windows;

/**
 * 持有"已放入物品"的窗口标记接口（环指大师 创作/强化/分解/附魔 工作窗实现）。
 * <p>这类窗口会把背包物品 detach 到 UI 格子里暂存；若在保存/场景重建/销毁前不退回，
 * 物品既不在背包也不可序列化——移动端切后台（GameScene.onPause → saveAll）或进程被杀、
 * 旋转/尺寸变化触发场景重建（Window.destroy）后即永久丢失。</p>
 * <p>约定：实现类须在自身 {@link com.shatteredpixel.shatteredpixeldungeon.ui.Window#destroy()}
 * 中先退回所有物品（覆盖一切销毁路径）；{@link com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene#onPause()}
 * 保存前会对当前场景中所有实现本接口的窗口调用 {@link #returnItemsAndClose()} 统一收尾，
 * 保证存档内容不丢。</p>
 */
public interface WindowReturnsItems {

	//把窗口内所有已放入物品退回英雄背包（背包满则掉在英雄脚下），并关闭窗口
	void returnItemsAndClose();

}
