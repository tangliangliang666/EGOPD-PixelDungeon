package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BoneWeaving;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BodyArtWeapon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * 人体派素材基类：环指大师「创作」合成系统的材料。
 * <p>持有 4 个内置标记数值（Weapon/Bone/Meat/Blood），合成时累加到产出武器。
 * 4 个值默认均为 0，由子类在实例块中设定各自的非零数值。
 * 素材可堆叠（stackable=true）、不可升级、默认已鉴定；可丢弃/投掷（普通物品行为）。</p>
 * <p>骨骸编织天赋：骨质/肉质素材可直接使用，消耗 1 份获得临时护甲增益 buff
 * —— 该 buff <b>不存数值、没有层数</b>，减伤时由 {@code Char.drRoll()} 实时按
 * 「0~角色等级」取上界（见 {@code BoneWeaving}）。</p>
 */
public abstract class BodyArtMaterial extends Item {

	// 内置标记数值：合成时累加到产出武器的对应字段
	public int weaponValue = 0;
	public int boneValue   = 0;
	public int meatValue   = 0;
	public int bloodValue  = 0;

	public static final String AC_WEAVE = "WEAVE";

	{
		stackable = true;
		unique    = false;
		bones     = false;
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
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		if (hero.heroClass == HeroClass.RING_MASTER
				&& hero.hasTalent( Talent.BONE_WEAVING )
				&& canWeave( hero )) {
			actions.add( AC_WEAVE );
		}
		return actions;
	}

	@Override
	public String actionName( String action, Hero hero ) {
		if (action.equals( AC_WEAVE )) {
			return "编织";
		}
		return super.actionName( action, hero );
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute( hero, action );
		if (action.equals( AC_WEAVE )) {
			if (hero.heroClass != HeroClass.RING_MASTER || !hero.hasTalent( Talent.BONE_WEAVING )) {
				GLog.w( "需要骨骸编织天赋才能使用此素材。" );
				return;
			}
			if (!canWeave( hero )) {
				GLog.w( "这项素材当前无法用于骨骸编织。" );
				return;
			}
			int points = hero.pointsInTalent( Talent.BONE_WEAVING );
			float duration = points == 1 ? 50f : 100f;
			BoneWeaving bw = Buff.prolong( hero, BoneWeaving.class, duration );
			// 这里不再掷点、也不往 buff 里写数值：护甲上界由 Char.drRoll() 实时按角色等级取，
			// buff 只负责「存在」与「还剩多久」，因此图标上没有层数。
			// 续期语义是 postpone（只会变成更长的那个），进度弧基准同样只能跟着涨，
			// 否则先用 +2（100）再用 +1（50）会让弧按 50 算，直接显示成已过期。
			bw.maxDuration = Math.max( bw.maxDuration, duration );
			detach( hero.belongings.backpack );
			hero.spendAndNext( 1f );
			// 即时报文不带数字（AGENTS §2）：具体数值写在天赋 desc 与本 buff 的 desc 里
			GLog.p( "骨骸编织：素材编入躯体，你的护甲得到强化。" );
			Item.updateQuickslot();
		}
	}

	/** 是否可用于骨骸编织：骨质素材始终可用，肉质素材需 +2。 */
	public boolean canWeave( Hero hero ) {
		return false;
	}

	@Override
	public String info() {
		// 在描述末尾追加数值摘要，方便测试阶段核对（魔质素材等无数值材料覆写 valueSummary 改为说明文本）
		return super.info() + "\n\n" + valueSummary();
	}

	/** 数值摘要：默认显示四项内置值；魔质素材等无数值材料覆写为说明文本。 */
	protected String valueSummary() {
		return "武器值:_" + weaponValue + "_  骨值:_" + boneValue + "_\n" +
				"肉值:_" + meatValue + "_  血值:_" + bloodValue + "_";
	}

	/** 是否可放入「创作」合成格；仅供强化使用的素材（如魔质素材）覆写为 false。 */
	public boolean usableInCraft() {
		return true;
	}
}
