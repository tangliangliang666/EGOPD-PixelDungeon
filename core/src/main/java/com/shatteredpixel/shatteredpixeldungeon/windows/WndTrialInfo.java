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

import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.watabou.noosa.Image;

/**
 * 单个考验的详情窗：**继承 {@link WndTitledMessage}** —— 左上角该质点的图标、名称、描述，
 * 下面再追加一个「开启该考验」的开关。
 *
 * <h3>⚠️ 开关为什么必须由本类自己构造</h3>
 * <p>{@code Button} 的 {@code PointerArea} 是在**构造时**就把自己注册进全局
 * {@code PointerEvent} 的监听表的；而那张表是 {@code new Signal<>(true)}（**stackMode**）：
 * {@code add()} 走 {@code addFirst}，{@code dispatch()} 从队首逐个问、**遇到第一个返回 true 的就
 * return**。说白了就是「**后注册者优先，且第一个命中的会吞掉后面所有人**」。</p>
 *
 * <p>而 {@code Window} 的构造会加一个**覆盖整个屏幕**的 blocker（靠它实现「点窗口外即关窗」），
 * 它照样拦截：{@code Gizmo.isActive()} 只看 {@code active && parent.isActive()}，**不看 visible**，
 * 而 blocker 恰恰是 {@code visible = false} 的那个。于是任何**早于**该 blocker 注册的控件，都会在
 * 每一次点击时被 blocker 抢先命中并吞掉 —— 症状就是「按钮点上去毫无反应」。
 * 曾经把开关写在调用方里（{@code WndTrials.openDetail} 先 {@code new CheckBox(...)} 再传进来），
 * 正是踩了这个坑（用户 2026-09-25 报的「开关无法切换」）。</p>
 *
 * <p>所以：{@code new CheckBox(...)} 必须写在下面 {@code super(...)} **之后**。
 * （SPD 里窗口内的控件一律在子类构造里 new，就是这个原因；别为了「把逻辑留在调用方」而提前构造。）</p>
 *
 * <h3>排版时序</h3>
 * <p>{@code WndTitledMessage} 的构造函数**末尾**才 {@code resize()}，描述文本折行后的高度要到那时
 * 才定下来。所以这里接在 super 后面，用当前的 {@code height}（＝描述文本的底边）继续往下排，
 * 最后再 {@code resize()} 把窗口撑高到刚好收住开关下缘。</p>
 */
public class WndTrialInfo extends WndTitledMessage {

	private static final int TOGGLE_GAP		= 2;
	private static final int TOGGLE_HEIGHT	= 16;

	/** 开关被切换时的回调，参数＝切换**之后**的新状态。勾选状态由调用方（{@code WndTrials}）保存。 */
	public interface ToggleListener {
		void onToggle( boolean checked );
	}

	/**
	 * @param toggleLabel  开关上的文案（{@code WndTrials} 传 {@code Messages.get(this, "enable")}）
	 * @param checked      开关初值（调用方「待提交」位掩码里的那一位）
	 * @param toggleActive 是否允许切换。局内查看时为 false —— {@code Button.active=false} 会经
	 *                     {@code Gizmo.isActive()} 的父链把整个命中关掉，开关就只是个指示器。
	 * @param listener     切换回调；只在**真的**发生切换时被调用（可能为 null）
	 */
	public WndTrialInfo( Image icon, String title, String message,
			String toggleLabel, boolean checked, boolean toggleActive, ToggleListener listener ) {

		super( icon, title, message );

		//⚠️ 位置有讲究：必须在 super(...) 之后 —— 见类注释（PointerArea 注册顺序 / 全屏 blocker 会吞点击）
		CheckBox toggle = new CheckBox( toggleLabel ) {
			@Override
			protected void onClick() {
				super.onClick();	//CheckBox.onClick() 里完成 checked( !checked ) 并换掉勾选图标
				if (listener != null) listener.onToggle( checked() );
			}
		};
		toggle.checked( checked );	//初值必须走 checked(boolean)：直接写字段不会换图标
		toggle.active = toggleActive;

		toggle.setRect( 0, height + 2*TOGGLE_GAP, width, TOGGLE_HEIGHT );
		add( toggle );
		resize( width, (int)toggle.bottom() );
	}
}
