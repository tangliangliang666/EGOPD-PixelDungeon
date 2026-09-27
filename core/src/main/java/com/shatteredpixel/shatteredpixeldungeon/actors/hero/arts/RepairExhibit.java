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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RingfingerAutomaton;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.MagicMaterial;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

/**
 * 展品技艺「修整」（tier 2，天赋「画廊即时修整」+2 解锁）：
 * 花费 1 个魔质素材，选择一只环指自动人偶，使其获得随机的精英化效果
 * （四种 ChampionEnemy 变体之一，不同变体可共存，故可反复修整叠加。
 * 排除巨型精英与成长精英：巨型体型增大后无法进入狭窄空间，会限制人偶移动；
 * 成长精英的强化会随时间无限增长，作为长期陪伴的展品会逐渐失衡）。
 */
public class RepairExhibit extends RingMasterArt {

	public static final RepairExhibit INSTANCE = new RepairExhibit();

	private RepairExhibit(){}

	@Override
	public int tier() {
		return 2;
	}

	@Override
	public boolean canPerform( Hero hero ) {
		if (hero.pointsInTalent( Talent.GALLERY_TOUCHUP ) < 2){
			return false;
		}
		if (collectDolls().isEmpty()){
			return false;
		}
		MagicMaterial mat = hero.belongings.getItem( MagicMaterial.class );
		return mat != null && mat.quantity() >= 1;
	}

	@Override
	public void onPerform( Hero hero ) {
		selectDoll( "选择要修整的展品", new DollCallback() {
			@Override
			public void onDollSelected( RingfingerAutomaton doll ) {
				MagicMaterial mat = Dungeon.hero.belongings.getItem( MagicMaterial.class );
				if (mat == null || mat.quantity() < 1){
					GLog.w( "没有魔质素材。" );
					return;
				}

				//消耗 1 个魔质素材
				mat.quantity( mat.quantity() - 1 );
				if (mat.quantity() <= 0){
					//detachAll 会递归搜索背包与素材箱，移除数量归零的堆叠
					mat.detachAll( Dungeon.hero.belongings.backpack );
				}
				Item.updateQuickslot();

				//随机精英化（四种变体；不同变体可共存，可叠加。
				//排除巨型精英（体型受限难行动）与成长精英（随时间无限增强、长期陪伴会失衡））
				Class<? extends ChampionEnemy> buffCls;
				switch (Random.Int( 4 )){
					case 0: default: buffCls = ChampionEnemy.Blazing.class;     break;
					case 1:          buffCls = ChampionEnemy.Projecting.class;  break;
					case 2:          buffCls = ChampionEnemy.AntiMagic.class;   break;
					case 3:          buffCls = ChampionEnemy.Blessed.class;     break;
				}
				ChampionEnemy elite = Buff.affect( doll, buffCls );

				Dungeon.hero.spendAndNext( Actor.TICK );
				//名称取 buff 官方本地化文本（actors.buffs.championenemy$xxx.name）
				GLog.p( "魔质渗入了展品的骨架，环指自动人偶获得了_" + elite.name() + "_的效果！" );
			}
		} );
	}

	@Override
	public String name() {
		return "修整";
	}

	@Override
	public String shortDesc() {
		return "花费_1_个魔质素材，使一只展品获得随机精英化效果";
	}

	@Override
	public String desc() {
		return "画廊导师以魔质素材对展品进行精修。\n\n"
				+ "花费_1_个魔质素材，选择一只场上的环指自动人偶，使其获得_随机的精英化效果_：" +
				"烈焰精英（攻击点燃、近战伤害提升）、索敌精英（额外攻击距离、近战伤害提升）、敌法精英（受伤减半、免疫魔法）、" +
				"天佑精英（精准与闪避大幅提升）。\n\n"
				+ "不会出现_巨型精英_与_成长精英_的效果。\n\n"
				+ "_不同的精英化效果可以共存_，反复修整可以叠加多种效果。\n\n"
				+ "需要天赋「画廊即时修整」_+2_。";
	}

	@Override
	public int icon() {
		return HeroIcon.ART_REPAIR; //齿轮图案（活化图标右侧）
	}
}
