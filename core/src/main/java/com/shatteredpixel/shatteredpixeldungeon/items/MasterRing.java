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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.arts.RingMasterArt;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndRingMasterArts;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * 大师指环：环指大师专属合成道具。
 * <p>戴在无名指上的三重指环，身为环指大师的身份象征。
 * 仅保留一个「展品技艺」按钮，打开 {@link WndRingMasterArts} 技艺列表窗口
 * （创作/强化/分解 等指环技艺与 构建/指挥/强化 等展品技艺集中在窗口内按层级选择）。</p>
 * <p>快速绑定（参考法典 setQuickSpell 机制）：在技艺列表中长按某项技艺将其绑定到指环，
 * 绑定后<b>屏幕右侧</b>出现该技艺的专属按钮（{@link QuickArtAction}，占 {@code ActionIndicator} 副槽），
 * 点击按钮即可直接执行；再次长按同一技艺取消绑定。
 * 绑定状态随存档持久化（存 class 名，恢复时按 {@link RingMasterArt#getAllArts()} 匹配）。</p>
 * <p><b>2026-09-18 修复</b>：此前「点击指环直接执行绑定的技艺」是塞在 {@link #defaultAction()} 里的，
 * 于是<b>一旦绑定成功，点击指环就再也打不开技艺窗口</b>——而「长按技艺绑定 / 取消绑定」只能在窗口里做
 * ⇒ 玩家无法重新绑定。现在点击指环<b>恒定</b>打开技艺窗口，「一键执行」交给屏幕右侧的副槽按钮。</p>
 */
public class MasterRing extends Item {

	//打开技艺列表窗口
	public static final String AC_ARTS = "ARTS";
	//快速执行绑定的技艺（屏幕右侧副槽按钮 / 物品窗口里的「执行：X」按钮）
	public static final String AC_QUICK = "QUICK_ART";

	{
		image = ItemSpriteSheet.MASTER_RING;

		defaultAction = AC_ARTS;

		// 唯一物品：不被嬗变、不可堆叠
		unique = true;
		// 死亡后不进入遗骨掉落
		bones = false;
	}

	//当前绑定的快速技艺（null = 未绑定）
	private RingMasterArt quickArt = null;

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	/**
	 * 默认动作<b>恒定</b>是「展品技艺」＝打开技艺窗口。
	 *
	 * <p>「点击指环直接执行绑定的技艺」曾经写在这里（已绑定时返回 {@link #AC_QUICK}），
	 * 那正是 2026-09-18 那个 bug 的根源：绑定之后点击指环只会执行技艺、不再开窗，
	 * 而绑定 / 取消绑定只能在技艺窗口里做 ⇒ 玩家<b>回不到窗口、无法重新绑定</b>。</p>
	 *
	 * <p>现在执行动作独立成常驻按钮 {@link QuickArtAction}（屏幕右侧副槽），
	 * 点击指环则永远打开窗口（同牧师法典：点击法典永远打开法术列表）。</p>
	 */
	@Override
	public String defaultAction() {
		return AC_ARTS;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = new ArrayList<>();
		// 仅环指大师能使用技艺按钮
		if (hero.heroClass == HeroClass.RING_MASTER) {
			actions.add( AC_ARTS );
			// 已绑定时额外给一个「执行：X」入口（背包里长按指环即可看到）
			if (quickArt != null) {
				actions.add( AC_QUICK );
			}
		}
		// 不提供丢弃/投掷：大师指环是身份象征，无法被丢弃
		return actions;
	}

	@Override
	public String actionName( String action, Hero hero ) {
		if (action.equals( AC_ARTS )) {
			return "展品技艺";
		} else if (action.equals( AC_QUICK )) {
			return quickArt != null ? "执行：" + quickArt.name() : "展品技艺";
		}
		return super.actionName( action, hero );
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute( hero, action );

		if (hero.heroClass != HeroClass.RING_MASTER) {
			GLog.w( "只有环指大师才能使用大师指环。" );
			return;
		}

		if (action.equals( AC_ARTS )) {
			//顺手补挂副槽按钮：绑定状态变更时已挂过，这里是兜底（修复前存的档没有这个 buff）
			ensureQuickArtButton( hero );
			GameScene.show( new WndRingMasterArts( hero ) );
		} else if (action.equals( AC_QUICK )) {
			//防御：绑定技艺丢失（如存档异常）时退回打开窗口
			if (quickArt == null){
				GameScene.show( new WndRingMasterArts( hero ) );
				return;
			}
			if (!quickArt.canPerform( hero )){
				GLog.w( "当前无法执行「_" + quickArt.name() + "_」。" );
				return;
			}
			quickArt.onPerform( hero );
		}
	}

	/** 当前绑定到指环的快速技艺（null = 未绑定）。 */
	public RingMasterArt quickArt() {
		return quickArt;
	}

	/** 设置/取消快速绑定：传入当前已绑定的技艺则取消，否则替换绑定。 */
	public void setQuickArt( RingMasterArt art ) {
		if (art == quickArt){
			quickArt = null; //再长按同一技艺取消绑定
		} else {
			quickArt = art;
		}
		Item.updateQuickslot();
		//绑定 / 解绑都补挂一次：副槽按钮由 QuickArtAction.act() 按「还有没有绑定」自行去留
		ensureQuickArtButton( Dungeon.hero );
	}

	/** 确保「一键执行绑定技艺」的副槽按钮已挂在英雄身上（无英雄 / 无绑定时什么也不做）。 */
	private void ensureQuickArtButton( Hero hero ) {
		if (hero == null || quickArt == null) return;
		Buff.affect( hero, QuickArtAction.class );
	}

	@Override
	public String desc() {
		String base = Messages.get( this, "desc" );
		if (quickArt != null){
			base += "\n\n当前绑定的快速技艺：_" + quickArt.name()
					+ "_（点击屏幕右侧的按钮直接执行；点击指环打开技艺窗口可更换绑定）";
		}
		return base;
	}

	private static final String QUICK_ART = "quick_art";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		if (quickArt != null) {
			bundle.put( QUICK_ART, quickArt.getClass() );
		}
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		if (bundle.contains( QUICK_ART )) {
			Class<?> artCls = bundle.getClass( QUICK_ART );
			for (RingMasterArt art : RingMasterArt.getAllArts()) {
				if (art.getClass() == artCls){
					quickArt = art;
				}
			}
		}
	}

	//==========================================================================
	// 一键执行绑定的技艺：屏幕右侧的副槽按钮（槽位 1）
	//==========================================================================

	/**
	 * 「一键执行绑定的技艺」按钮的载体，占 {@code ActionIndicator} 的<b>副槽</b>
	 * （写法照 {@code SealedSwordBase.SwordSwap}）——主槽留给职业技能 / 盔甲技能按钮，
	 * 两个槽位在 {@code GameScene.layoutTags()} 里各自占一行、互不顶掉。
	 *
	 * <p><b>为什么单独做成一个 buff</b>：执行动作不能占用 {@link #defaultAction()}——那正是
	 * 「绑定后无法重新绑定」的成因（默认动作被占用 ⇒ 点击指环进不了技艺窗口）。
	 * 独立按钮还能顺带<b>省下一个快捷栏位置</b>：指环不用放进快捷栏也随时可一键执行。</p>
	 *
	 * <p>生命周期由 {@link #act()} 每回合自检：英雄还带着指环且仍有绑定 ⇒ 占住副槽；
	 * 否则自我摘除（按钮随之消失）。{@code revivePersists} 让它跨安卡复活保留；
	 * 绑定的技艺换人时经 {@code refreshSecond()} 重建按钮的名字与图标。</p>
	 */
	public static class QuickArtAction extends Buff implements ActionIndicator.Action {

		{
			//安卡复活后按钮仍在
			revivePersists = true;
		}

		/** 上一次画在按钮上的技艺，用来判断「绑定的技艺换了，图标 / 名字要重建」。 */
		private RingMasterArt shownArt;

		/** 英雄身上的大师指环（非环指大师 / 指环不在身上 ⇒ null）。 */
		private MasterRing ring() {
			if (!(target instanceof Hero)) return null;
			return ((Hero) target).belongings.getItem( MasterRing.class );
		}

		@Override
		public boolean act() {
			MasterRing ring = ring();
			//没有指环、或绑定已被取消：自我回收，按钮随之消失
			if (ring == null || ring.quickArt() == null) {
				detach();
				return true;
			}
			//常时占副槽：只有槽位不是自己时才占位——已经是自己就什么都不做，
			//否则每回合都会触发一次按钮重建（与主槽上的职业 / 盔甲技能按钮互不干扰）
			if (ActionIndicator.secondAction != this) {
				ActionIndicator.setSecondAction( this );
			}
			//绑定的技艺换了 ⇒ 按钮的名字与图标要重建
			if (shownArt != ring.quickArt()) {
				shownArt = ring.quickArt();
				if (ActionIndicator.secondAction == this) {
					ActionIndicator.refreshSecond();
				}
			}
			spend( TICK );
			return true;
		}

		@Override
		public boolean attachTo( Char target ) {
			if (super.attachTo( target )) {
				shownArt = null;
				if (ActionIndicator.secondAction != this) {
					ActionIndicator.setSecondAction( this );
				}
				return true;
			}
			return false;
		}

		@Override
		public void fx( boolean on ) {
			//场景重建 / 英雄精灵刷新后把按钮重新挂回右侧（照 SwordSwap / MeleeWeapon.Charger 的写法）
			if (on) {
				shownArt = null;
				if (ActionIndicator.secondAction != this) {
					ActionIndicator.setSecondAction( this );
				}
			}
		}

		@Override
		public void detach() {
			super.detach();
			ActionIndicator.clearAction( this );
		}

		@Override
		public String actionName() {
			MasterRing ring = ring();
			if (ring == null || ring.quickArt() == null) return "展品技艺";
			return ring.quickArt().name();
		}

		@Override
		public int actionIcon() {
			MasterRing ring = ring();
			if (ring == null || ring.quickArt() == null) return HeroIcon.ART_CRAFT;
			return ring.quickArt().icon();
		}

		@Override
		public int indicatorColor() {
			//大师指环＝金：与艺术之巅的「艺术紫」(0xBB66DD) 及各种技能按钮都不同色
			return 0xCC9900;
		}

		@Override
		public void doAction() {
			MasterRing ring = ring();
			if (ring == null) return;
			//复用指环自己的执行分支：内含「未绑定退回开窗」与 canPerform 校验两道防御
			ring.execute( Dungeon.hero, AC_QUICK );
		}
	}
}
