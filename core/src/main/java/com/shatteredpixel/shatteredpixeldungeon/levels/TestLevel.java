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

/**
 * 调试/测试层标记（2026-09-10）。
 * <p>实现该接口的楼层：
 * <ul>
 *   <li>不计入 {@code generatedLevels} 生成记录与 {@code Statistics.deepestFloor}（见 Dungeon.newLevel）；</li>
 *   <li>不参与常规物资配额（食物/力量药水/升级卷轴/奥术刻笔等）与楼层 feeling 抽取（见 Level.create），
 *       避免调试层空耗 LimitedDrops 计数、污染存档进度。</li>
 * </ul>
 * 与 {@link DeadEndLevel} 的区别：DeadEnd 家族是"无生成逻辑的空层"，
 * 而本接口用于"有完整生成流程、但不该影响存档进度"的调试层（如 27 层贴图验证层）。
 */
public interface TestLevel {
}
