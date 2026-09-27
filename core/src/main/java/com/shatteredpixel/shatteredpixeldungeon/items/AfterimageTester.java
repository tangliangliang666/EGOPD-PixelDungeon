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

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Afterimage;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * 测试用道具：残像纠缠层数循环器（套用暗影斗篷贴图）。
 *
 * <p>「使用」一次给英雄身上的 {@link Afterimage} <b>加一层</b>（没有则新建，从 1 层开始），
 * 1→2→3→4 层之后再用一次则<b>移除</b>，如此循环。层数＝残像个数，所以反复使用就能逐个看到
 * 紫红 / 蓝紫 / 蓝 / 深蓝 依次浮现——便于对照观察<b>颜色、透明度、图层（在角色之下）、
 * 1 像素漂移</b>，以及<b>移动时的延迟跟随</b>（贴着墙角来回走最直观）。</p>
 *
 * <p>受击冲击（残像被甩开再拉回）的验证方式：把层数调满，然后让怪物打你一下——
 * 不必真挨打也行，只要触发 {@code Hero.damage} 即可（例如踩陷阱）。</p>
 *
 * <p>纯测试道具：不设掉落/商店/开局获得渠道，仅能通过调试窗口调出——
 * 本类位于 items 根包，会被调试控制台「杂项」的浅层扫描自动收录，无需额外注册。
 * 图标为调试期占位，日后可换专属帧。</p>
 */
public class AfterimageTester extends Item {

	public static final String AC_TOGGLE = "TOGGLE";

	{
		image = ItemSpriteSheet.ARTIFACT_CLOAK; //临时图标：暗影斗篷（调试期占位，日后可换专属帧）
		defaultAction = AC_TOGGLE;              //背包内点击/快捷栏直接切换
		unique = true;                          //每局唯一
		bones = false;                          //死亡后不进入遗骨掉落
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_TOGGLE );
		return actions;
	}

	@Override
	public String actionName( String action, Hero hero ) {
		if (action.equals( AC_TOGGLE )) {
			int now = Afterimage.stacksOn( hero );
			if (now == 0) return "开启残像（1 层）";
			if (now >= Afterimage.MAX_STACKS) return "关闭残像";
			return "叠加残像（" + now + " → " + (now + 1) + " 层）";
		}
		return super.actionName( action, hero );
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute( hero, action );

		if (action.equals( AC_TOGGLE )) {
			if (Afterimage.stacksOn( hero ) >= Afterimage.MAX_STACKS) {
				Buff.detach( hero, Afterimage.class );
				GLog.i( "残像已消散" );
			} else {
				//addStack 内部：没有 buff 则新建（用无时长重载——Afterimage 覆写了 act() 常驻，
				//带时长的重载只接受 FlavourBuff 子类），有则 +1 层，并按新层数重建残像
				int n = Afterimage.addStack( hero );
				GLog.i( "残像浮现……（" + n + " 层）" );
			}
			hero.spendAndNext( hero.cooldown() );
		}
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public String name() {
		return "残像测试器";
	}

	@Override
	public String desc() {
			return "测试用道具。每次使用_增加一层残像_：1 → 2 → 3 → 4 层，再用一次即_全部消散_。\n\n"
					+ "层数就是残像的个数（最多 _4_ 层），由近及远依次取色_紫红、蓝紫、蓝、深蓝_；"
					+ "残像渲染在角色贴图_之下_，每个都比前一个多晚一拍动，走的是与你_完全相同_的轨迹，"
					+ "追上你后轻轻震荡，_挨打时会被甩开再迅速拉回_。\n\n"
					+ "（当前套用暗影斗篷贴图）";
	}
}
