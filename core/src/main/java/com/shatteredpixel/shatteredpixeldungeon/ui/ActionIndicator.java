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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.input.GameAction;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Visual;

/**
 * 屏幕右侧那一竖列「标签按钮」里的<b>动作按钮</b>（原版全局只有一个）。
 *
 * <h3>本作扩展：两个槽位</h3>
 * <p>原版 {@code action} 是一个全局单例槽位，任何时刻只能有一个候选动作显示，
 * 两个动作（例如「狂暴」与封印之剑的「切换主副」）会<b>互相顶掉</b>——谁最后写谁占位，
 * 另一个静默失效。</p>
 *
 * <p>这里把它扩成<b>两个并列的槽位</b>，各自一个 {@link ActionIndicator} 实例、各自一个 Tag：</p>
 *
 * <table>
 *   <tr><th>槽位</th><th>静态字段</th><th>实例</th><th>谁在用</th></tr>
 *   <tr><td>0（主槽）</td><td>{@link #action}</td><td>{@link #instance}</td>
 *       <td>原版全部候选动作：狂暴 / 连击 / 武技 / 苦痛技艺 / 灵气 / 交换武器… 以及
 *           忠义巡礼者的复仇技艺 {@code VengeanceArts}</td></tr>
 *   <tr><td>1（副槽）</td><td>{@link #secondAction}</td><td>{@link #secondInstance}</td>
 *       <td>目前只有封印之剑的 {@code SealedSwordBase.SwordSwap}（「切换主副」）</td></tr>
 * </table>
 *
 * <p><b>向后兼容</b>：主槽的静态 API（{@link #setAction}、{@link #clearAction}、
 * {@link #refresh}、字段 {@code action}/{@code instance}）语义与原来<b>完全一致</b>，
 * 原版代码一行都不用改；副槽是纯新增。两个槽位在 {@code GameScene.layoutTags()}
 * 里各自占一行、自上而下排开。</p>
 *
 * <p><b>编号约定</b>：{@code slot} 是"第几个槽位"，只在本类和 {@code GameScene} 里用；
 * 新增槽位就沿用这套写法（{@code SLOTS} 常量、{@code xxxAction} 字段、多一个 Tag 字段）。</p>
 */
public class ActionIndicator extends Tag {

	Visual primaryVis;
	Visual secondVis;

	/** 槽位总数（{@code GameScene} 按这个数量为每个槽位建一个 Tag 并参与竖向排版）。 */
	public static final int SLOTS = 2;

	/** 主槽（槽位 0）当前的动作——原版唯一的那个全局槽位。 */
	public static Action action;
	/** 副槽（槽位 1）当前的动作——本作扩展。 */
	public static Action secondAction;

	/** 主槽的界面实例。 */
	public static ActionIndicator instance;
	/** 副槽的界面实例。 */
	public static ActionIndicator secondInstance;

	/** 本实例负责的槽位序号（0 = 主槽，1 = 副槽）。 */
	private final int slot;

	public ActionIndicator() {
		this( 0 );
	}

	public ActionIndicator( int slot ) {
		super( 0 );

		this.slot = slot;
		if (slot == 0)      instance = this;
		else                secondInstance = this;

		setSize( SIZE, SIZE );
		visible = false;
	}

	/** 本实例所在槽位当前的动作（无动作时为 null）。 */
	private Action current(){
		return slot == 0 ? action : secondAction;
	}
	
	/**
	 * 槽位 0 用原版的 {@link SPDAction#TAG_ACTION}（默认键 X）；槽位 1 用本作新增的
	 * {@link SPDAction#TAG_ACTION_2}（默认不绑键）。
	 *
	 * <p><b>为什么必须分开</b>：{@code Button} 的按键监听是「{@code getActionForKey(event) ==
	 * keyAction()} 就触发」，两个 Tag 若共用同一个 GameAction，按一次键会把两个按钮一起按下
	 * （既开技艺列表又换武器）。默认不绑键则 {@code getActionForKey} 只会返回 {@code NONE}，
	 * 永远不相等、也画不出按键提示。</p>
	 */
	@Override
	public GameAction keyAction() {
		return slot == 0 ? SPDAction.TAG_ACTION : SPDAction.TAG_ACTION_2;
	}
	
	@Override
	public void destroy() {
		super.destroy();
		//只清掉自己那一个槽位的实例引用（另一个槽位的 Tag 还活着）
		if (instance == this)       instance = null;
		if (secondInstance == this) secondInstance = null;
	}
	
	@Override
	protected synchronized void layout() {
		super.layout();
		
		if (primaryVis != null){
			if (!flipped)   primaryVis.x = x + (SIZE - primaryVis.width()) / 2f + 1;
			else            primaryVis.x = x + width - (SIZE + primaryVis.width()) / 2f - 1;
			primaryVis.y = y + (height - primaryVis.height()) / 2f;
			PixelScene.align(primaryVis);
			if (secondVis != null){
				if (secondVis.width() > 16) secondVis.x = primaryVis.center().x - secondVis.width()/2f;
				else                        secondVis.x = primaryVis.center().x + 8 - secondVis.width();
				if (secondVis instanceof BitmapText){
					//need a special case here for text unfortunately
					secondVis.y = primaryVis.center().y + 8 - ((BitmapText) secondVis).baseLine();
				} else {
					secondVis.y = primaryVis.center().y + 8 - secondVis.height();
				}
				PixelScene.align(secondVis);
			}
		}
	}
	
	private boolean needsRefresh = false;
	
	@Override
	public void update() {
		super.update();

		synchronized (ActionIndicator.class) {
			Action act = current();

			if (!visible && act != null) {
				visible = true;
				needsRefresh = true;
				flash();
			} else {
				visible = act != null;
			}

			if (needsRefresh) {
				if (primaryVis != null) {
					primaryVis.destroy();
					primaryVis.killAndErase();
					primaryVis = null;
				}
				if (secondVis != null) {
					secondVis.destroy();
					secondVis.killAndErase();
					secondVis = null;
				}
				if (act != null) {
					primaryVis = act.primaryVisual();
					add(primaryVis);

					secondVis = act.secondaryVisual();
					if (secondVis != null) {
						add(secondVis);
					}

					setColor(act.indicatorColor());
				}

				layout();
				needsRefresh = false;
			}

			if (!Dungeon.hero.ready) {
				if (primaryVis != null) primaryVis.alpha(0.5f);
				if (secondVis != null) secondVis.alpha(0.5f);
			} else {
				if (primaryVis != null) primaryVis.alpha(1f);
				if (secondVis != null) secondVis.alpha(1f);
			}
		}

	}

	@Override
	protected void onClick() {
		super.onClick();
		Action act = current();
		if (act != null && Dungeon.hero.ready) {
			act.doAction();
		}
	}

	@Override
	protected String hoverText() {
		Action act = current();
		String text = (act == null ? null : act.actionName());
		if (text != null){
			return Messages.titleCase(text);
		} else {
			return null;
		}
	}

	//==========================================================================
	// 主槽（原版语义，保持不变）
	//==========================================================================

	public static void setAction(Action action){
		synchronized (ActionIndicator.class) {
			ActionIndicator.action = action;
			refresh();
		}
	}

	public static void clearAction(){
		clearAction(null);
	}

	public static void clearAction(Action action){
		synchronized (ActionIndicator.class) {
			//action == null ⇒ 两个槽位一起清（新开一局 / 重开场景时用）
			if (action == null || ActionIndicator.action == action)       ActionIndicator.action = null;
			if (action == null || ActionIndicator.secondAction == action) ActionIndicator.secondAction = null;
		}
	}

	/** 重建主槽的按钮图标（原版语义）。 */
	public static void refresh(){
		synchronized (ActionIndicator.class) {
			if (instance != null) {
				instance.needsRefresh = true;
			}
		}
	}

	//==========================================================================
	// 副槽（本作扩展）
	//==========================================================================

	/** 副槽占位。 */
	public static void setSecondAction(Action action){
		synchronized (ActionIndicator.class) {
			ActionIndicator.secondAction = action;
			refreshSecond();
		}
	}

	/** 重建副槽的按钮图标。 */
	public static void refreshSecond(){
		synchronized (ActionIndicator.class) {
			if (secondInstance != null) {
				secondInstance.needsRefresh = true;
			}
		}
	}

	/**
	 * 两个槽位的按钮图标一起重建。
	 * <p>用在「两只手里的武器换了、两个按钮上画的物品都不作数了」这种场合——
	 * 比起在两个槽位各猜一次，一次性全刷更省心。</p>
	 */
	public static void refreshAll(){
		refresh();
		refreshSecond();
	}

	public interface Action {

		String actionName();

		default int actionIcon(){
			return HeroIcon.NONE;
		}

		//usually just a static icon, unless overridden
		default Visual primaryVisual(){
			return new HeroIcon(this);
		}

		//a smaller visual on the bottom-right, usually a tiny icon or bitmap text
		default Visual secondaryVisual(){
			return null; //no second visual by default
		}

		int indicatorColor();

		void doAction();

	}

}
