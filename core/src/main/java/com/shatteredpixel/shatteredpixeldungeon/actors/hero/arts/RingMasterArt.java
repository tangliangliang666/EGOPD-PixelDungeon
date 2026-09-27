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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.arts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RingfingerAutomaton;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * 环指大师技艺：通过大师指环（AC_ARTS → WndRingMasterArts）调用的操作单元
 * （实现模式参考牧师的 {@link com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell}，
 * 但无充能消耗——可用性仅由材料/场上单位决定）。
 * <p>所有技艺集中在 {@link com.shatteredpixel.shatteredpixeldungeon.windows.WndRingMasterArts}
 * 列表窗口内按层级分组展示，并可长按绑定到大师指环（点击指环直接执行绑定的技艺，
 * 同法典的快速施法 setQuickSpell 机制）。</p>
 * <p>层级划分（参考法典 tier）：
 * tier 1「指环技艺」= 创作/强化/分解，所有环指大师可用（大师指环自带）；
 * tier 2「专精技艺」= 画廊导师的构建/指挥/活化/修整/激发/装备、艺术之巅的附魔，按天赋点解锁。</p>
 */
public abstract class RingMasterArt {

	/** 执行该技艺（点击列表条目或点击已绑定的大师指环时调用，可用性已由 canPerform 校验）。 */
	public abstract void onPerform( Hero hero );

	/** 可用性判定（不满足时列表条目置灰，指环快捷执行时 GLog 提示）。 */
	public boolean canPerform( Hero hero ){
		return true;
	}

	/** 层级（1 = 指环技艺；2 = 专精技艺），窗口按此分组展示。 */
	public abstract int tier();

	/** 层级名称（窗口分组标题）。 */
	public static String tierName( int tier ){
		return tier == 2 ? "专精技艺" : "指环技艺";
	}

	/** 技艺名称（硬编码中文，不走 messages）。 */
	public String name(){
		return "未命名技艺";
	}

	/** 一句话描述（列表 hover 提示）。 */
	public String shortDesc(){
		return "";
	}

	/** 完整描述（info 模式查看）。 */
	public String desc(){
		return "";
	}

	/** 图标帧号（hero_icons.png，需为该技艺绘制专属图标后替换）。 */
	public int icon(){
		return com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon.GALLERY_MENTOR;
	}

	/** 收集当前英雄可用的技艺列表（按层级顺序）。 */
	public static ArrayList<RingMasterArt> getArtList( Hero hero ){
		ArrayList<RingMasterArt> arts = new ArrayList<>();
		if (hero.heroClass != HeroClass.RING_MASTER){
			return arts;
		}
		// tier 1：指环技艺（所有环指大师，大师指环自带）
		arts.add(CraftArt.INSTANCE);
		arts.add(ReinforceArt.INSTANCE);
		arts.add(DecomposeArt.INSTANCE);
		// tier 2：专精技艺（画廊导师：构建/指挥/活化/修整/激发/装备；艺术之巅：附魔）
		if (hero.subClass == HeroSubClass.GALLERY_MENTOR){
			arts.add(ConstructExhibit.INSTANCE);
			arts.add(CommandExhibit.INSTANCE);
			//「画廊即时修整」：+1 活化、+2 修整、+3 激发
			int touchup = hero.pointsInTalent( Talent.GALLERY_TOUCHUP );
			if (touchup >= 1) arts.add( ActivateExhibit.INSTANCE );
			if (touchup >= 2) arts.add( RepairExhibit.INSTANCE );
			if (touchup >= 3) arts.add( StimulateExhibit.INSTANCE );
			//「作品交叉展览」：+1 起解锁装备
			if (hero.pointsInTalent( Talent.CROSS_EXHIBITION ) >= 1) arts.add( EquipExhibit.INSTANCE );
		} else if (hero.subClass == HeroSubClass.ART_PINNACLE){
			//「学习未知之物」：+1 起解锁附魔
			if (hero.pointsInTalent( Talent.LEARN_UNKNOWN ) >= 1) arts.add( EnchantArt.INSTANCE );
		}
		return arts;
	}

	/** 全量技艺列表（读档时按 class 恢复指环绑定的快速技艺用，同 ClericSpell.getAllSpells）。 */
	public static ArrayList<RingMasterArt> getAllArts(){
		ArrayList<RingMasterArt> arts = new ArrayList<>();
		arts.add(CraftArt.INSTANCE);
		arts.add(ReinforceArt.INSTANCE);
		arts.add(DecomposeArt.INSTANCE);
		arts.add(ConstructExhibit.INSTANCE);
		arts.add(CommandExhibit.INSTANCE);
		arts.add(ActivateExhibit.INSTANCE);
		arts.add(RepairExhibit.INSTANCE);
		arts.add(StimulateExhibit.INSTANCE);
		arts.add(EquipExhibit.INSTANCE);
		arts.add(EnchantArt.INSTANCE);
		return arts;
	}

	//========== 展品（环指自动人偶）选择共享工具（活化/修整/激发/装备等选人偶的技艺复用） ==========

	/** 收集场上所有存活的环指自动人偶。 */
	public static ArrayList<RingfingerAutomaton> collectDolls() {
		ArrayList<RingfingerAutomaton> dolls = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof RingfingerAutomaton && m.isAlive()){
				dolls.add((RingfingerAutomaton) m);
			}
		}
		return dolls;
	}

	/** 选人偶回调（选中一只存活人偶后执行对应技艺效果）。 */
	public interface DollCallback {
		void onDollSelected( RingfingerAutomaton doll );
	}

	/**
	 * 选择一只场上的环指自动人偶：仅一只时直接选中；多只时弹出选择窗口。
	 * 场上无人偶时提示并返回 false（技艺不消耗回合）。
	 */
	public static boolean selectDoll( String title, DollCallback callback ){
		ArrayList<RingfingerAutomaton> dolls = collectDolls();
		if (dolls.isEmpty()){
			GLog.w( "场上没有环指自动人偶。" );
			return false;
		}
		if (dolls.size() == 1){
			callback.onDollSelected( dolls.get(0) );
			return true;
		}
		GameScene.show( new DollListWindow( dolls, title, callback ) );
		return true;
	}

	/** 人偶选择列表窗口（场上有多只人偶时使用，按钮显示生命与装备作品以区分）。 */
	public static class DollListWindow extends Window {

		private static final int WIDTH_P = 130;
		private static final int WIDTH_L = 180;
		private static final int MARGIN = 2;

		public DollListWindow( ArrayList<RingfingerAutomaton> dolls, String titleText, final DollCallback callback ){
			super();

			int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

			float pos = MARGIN;
			RenderedTextBlock title = PixelScene.renderTextBlock( titleText, 9 );
			title.hardlight( TITLE_COLOR );
			title.setPos( (width - title.width()) / 2, pos );
			title.maxWidth( width - MARGIN * 2 );
			add( title );

			pos = title.bottom() + 3 * MARGIN;

			for (final RingfingerAutomaton doll : dolls){
				String label = "_环指自动人偶:_  生命 " + doll.HP + "/" + doll.HT;
				if (doll.weapon() != null){
					label += "\n装备：_" + doll.weapon().name() + "_";
				}
				RedButton dollBtn = new RedButton( label ) {
					@Override
					protected void onClick() {
						super.onClick();
						hide();
						callback.onDollSelected( doll );
					}
				};
				dollBtn.leftJustify = true;
				dollBtn.multiline = true;
				dollBtn.setSize( width, dollBtn.reqHeight() );
				dollBtn.setRect( 0, pos, width, dollBtn.reqHeight() );
				add( dollBtn );
				pos = dollBtn.bottom() + MARGIN;
			}

			resize( width, (int) pos );
		}
	}
}
