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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlessingMarker;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 「祝福」buff：失乐园施法对自身/友方目标施加。
 * <p>持续13回合，效果为精准、闪避与最大生命值各 +5%
 * （对英雄通过 updateHT 统一计算；对其他单位直接调整 HT 字段）。
 * 持有期间在单位贴图下方显示静态标记贴图（见 {@link BlessingMarker}）。</p>
 */
public class Blessing extends FlavourBuff {

	public static final float DURATION = 13f;

	//仅对非英雄单位生效的直接 HT 加成值（英雄走 updateHT 计算，不存储）
	private int htBonus = 0;

	//单位贴图下方的标记贴图
	private BlessingMarker marker;

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	//精准加成倍数
	public float accuracyMultiplier(){
		return 1.05f;
	}

	//闪避加成倍数
	public float evasionMultiplier(){
		return 1.05f;
	}

	//最大生命值加成倍数（Hero.updateHT 中调用）
	public float HTMultiplier(){
		return 1.05f;
	}

	//单位贴图下方的静态标记（attach 时挂载、detach 时移除）
	@Override
	public void fx( boolean on ) {
		if (on){
			if (marker == null && target != null && target.sprite != null){
				marker = BlessingMarker.attach( target );
			}
		} else {
			if (marker != null){
				marker.remove();
				marker = null;
			}
		}
	}

	@Override
	public boolean attachTo( Char target ) {
		if (super.attachTo( target )){
			if (target instanceof Hero){
				//英雄走 updateHT 统一计算（含 buff 检查）
				((Hero) target).updateHT( true );
			} else {
				//其他单位直接调整 HT 字段
				htBonus = Math.round( target.HT * 0.05f );
				target.HT += htBonus;
				target.HP += htBonus;
			}
			return true;
		}
		return false;
	}

	@Override
	public void detach() {
		//移除单位贴图下方的标记贴图
		if (marker != null){
			marker.remove();
			marker = null;
		}
		Char t = target;
		super.detach();
		if (t != null){
			if (t instanceof Hero){
				//buff 已移除，updateHT 不再计入加成
				((Hero) t).updateHT( false );
			} else if (htBonus > 0){
				t.HT = Math.max( 1, t.HT - htBonus );
				t.HP = Math.min( t.HP, t.HT );
				htBonus = 0;
			}
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.HEART_FATE + 2; //花矢(HEART_FATE+1)的后一位，图标已绘制
	}

	@Override
	public String name() {
		return "祝福";
	}

	@Override
	public String desc() {
		return "吾会治愈所有的疾病，你将重获新生。\n\n"
				+ "持有期间精准、闪避和最大生命值各提升_5%_。\n\n"
				+ "剩余时长：" + dispTurns( visualcooldown() ) + "回合";
	}

	private static final String HT_BONUS = "ht_bonus";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( HT_BONUS, htBonus );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		htBonus = bundle.getInt( HT_BONUS );
	}
}
