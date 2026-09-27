package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth;
import com.watabou.utils.Random;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

/**
 * 环指大师击杀掉落（2026-09-01，2026-09-03 适配财富戒指/幸运附魔）。
 * <p>英雄职业为环指大师时，击杀敌人按怪物类型独立判定掉落人体派素材（硬/骨/肉/血/魔质）。
 * 每条掉落为独立的概率判定，命中即掉落 1 件素材到怪物所在格；0% 的条目不入表（视为不掉）。
 * 钩子挂在 {@link Mob#die} 的英雄击杀分支（与 TinyGalaxy/SmilingBlade 同处）。
 * 掉落表随游戏更新逐怪填写，贴合各怪物设定。</p>
 * <p>财富/幸运适配（与原版普通掉落同机制）：
 * <ul>
 *   <li>每条掉落概率乘 {@link RingOfWealth#dropChanceMultiplier}（=1.2^财富等级，
 *       同 {@code Mob.lootChance()} 对普通掉落的处理）；财富戒指的额外消耗品/装备掉落
 *       由 {@code Mob.rollToDropLoot()} 原样触发，不受影响。</li>
 *   <li>击杀时目标身上带有幸运附魔触发的 LuckProc 标记，额外完整判定一次素材掉落
 *       （对应原版幸运击杀多掉一件消耗品的行为）。</li>
 * </ul></p>
 */
public class RingMasterLoot {

	/** 单条掉落：素材工厂 + 独立掉率（0~1）。 */
	private static final class Drop {
		final Supplier<Item> factory;
		final float chance;
		Drop( Supplier<Item> f, float c ){
			factory = f;
			chance  = c;
		}
	}

	private static final LinkedHashMap<Class<? extends Mob>, Drop[]> TABLE = new LinkedHashMap<>();

	static {
		// ===================== 下水道（Sewers）=====================
		put( Rat.class,            bone(.05f), meat(.05f), blood(.05f) );
		put( Albino.class,         bone(.05f), meat(.10f), blood(.05f) );
		put( Snake.class,                       meat(.10f), blood(.05f) );
		put( Crab.class,           bone(.10f), meat(.10f)              );
		put( Gnoll.class,          bone(.07f), meat(.07f), blood(.07f) );
		put( Slime.class,                                        blood(.20f) );
		put( Swarm.class,                                        blood(.03f) );
		put( FetidRat.class,       bone(.07f), meat(.07f), blood(.07f) );
		put( GreatCrab.class,      bone(.15f), meat(.15f)              );
		put( GnollTrickster.class, bone(.10f), meat(.10f), blood(.10f) );
		put( Goo.class,                                          blood(.50f) );

		// ===================== 监狱（Prison）=====================
		put( Skeleton.class,        bone(.40f)                         );
		put( Thief.class,          bone(.10f), meat(.10f), blood(.10f) );
		put( Bandit.class,         bone(.15f), meat(.15f), blood(.15f) );
		put( Guard.class,          bone(.10f), meat(.10f), blood(.10f), hard(.05f) );
		put( Necromancer.class,    bone(.15f), meat(.15f), blood(.05f), magic(.15f) );
		put( SpectralNecromancer.class,                          magic(.50f) );
		put( Tengu.class,          bone(.15f), meat(.15f), blood(.15f), hard(.15f) );

		// ===================== 洞窟（Caves）=====================
		put( Bat.class,            bone(.10f), meat(.10f), blood(.25f) );
		put( Brute.class,          bone(.15f), meat(.15f), blood(.15f) );
		put( ArmoredBrute.class,   bone(.15f), meat(.15f), blood(.15f), hard(.15f) );
		put( Shaman.RedShaman.class,    bone(.10f), meat(.10f), blood(.10f), magic(.15f) );
		put( Shaman.BlueShaman.class,   bone(.10f), meat(.10f), blood(.10f), magic(.15f) );
		put( Shaman.PurpleShaman.class, bone(.10f), meat(.10f), blood(.10f), magic(.15f) );
		put( Spinner.class,        bone(.10f), meat(.25f), blood(.10f) );
		put( FungalSpinner.class,  bone(.15f), meat(.30f), blood(.15f) );
		put( GnollGuard.class,     bone(.15f), meat(.15f), blood(.15f) );
		put( GnollSapper.class,    bone(.15f), meat(.15f), blood(.15f) );
		put( GnollGeomancer.class, bone(.15f), meat(.15f), blood(.15f), magic(.30f) );
		put( HermitCrab.class,     bone(.20f), meat(.10f)              );
		put( CrystalSpire.class,                                    magic(.60f) );
		put( DM100.class,          hard(.15f)                         );
		put( DM300.class,          hard(.80f)                         );
		put( Pylon.class,          hard(.30f)                         );

		// ===================== 矮人都市（Dwarven Metropolis）=====================
		put( Golem.class,          hard(.30f), magic(.30f) );
		put( Monk.class,           bone(.15f), meat(.20f), blood(.15f) );
		put( Senior.class,         bone(.20f), meat(.25f), blood(.20f) );
		put( Warlock.class,        bone(.15f), meat(.15f), blood(.15f), magic(.15f) );
		put( Succubus.class,       bone(.20f), meat(.20f), blood(.20f), magic(.10f) );
		put( Ghoul.class,          bone(.10f), meat(.10f), blood(.10f) );
		put( DM200.class,          hard(.30f)                         );
		put( DM201.class,          hard(.45f)                         );
		put( DwarfKing.class,      bone(.20f), meat(.20f), blood(.20f), magic(.20f) );

		// ===================== 恶魔大厅（Demon Halls）=====================
		put( RipperDemon.class,    bone(.25f), meat(.25f), blood(.20f) );
		put( Eye.class,                        meat(.15f), blood(.30f), magic(.15f) );
		put( Scorpio.class,        bone(.20f), meat(.25f), blood(.25f) );
		put( Acidic.class,         bone(.20f), meat(.25f), blood(.35f) );
		put( DemonSpawner.class,   bone(.35f), meat(.35f), blood(.35f) );
		put( Elemental.FireElemental.class,        magic(.20f) );
		put( Elemental.NewbornFireElemental.class, magic(.45f) );
		put( Elemental.FrostElemental.class,       magic(.20f) );
		put( Elemental.ShockElemental.class,       magic(.20f) );
		put( Elemental.ChaosElemental.class,       magic(.20f) );

		// ===================== 跨区域 / 特殊 =====================
		put( Mimic.class,          hard(.05f)                         );
		put( GoldenMimic.class,    hard(.15f)                         );
		put( EbonyMimic.class,     hard(.30f)                         );
		put( Statue.class,         hard(.25f), magic(.05f)             );
		put( ArmoredStatue.class,   hard(.35f), magic(.05f)             );
		put( Piranha.class,        bone(.15f), meat(.25f), blood(.05f) );
		put( PhantomPiranha.class,  bone(.15f), meat(.25f), blood(.05f) );
		put( Wraith.class,                                          magic(.05f) );
		put( TormentedSpirit.class,                                 magic(.05f) );
	}

	private static void put( Class<? extends Mob> m, Drop... drops ){
		TABLE.put( m, drops );
	}

	// 素材工厂简写：每种素材掉落 1 件
	private static Drop hard ( float c ){ return new Drop( HardMaterial::new,  c ); }
	private static Drop bone ( float c ){ return new Drop( BoneMaterial::new,  c ); }
	private static Drop meat ( float c ){ return new Drop( MeatMaterial::new,  c ); }
	private static Drop blood( float c ){ return new Drop( BloodMaterial::new, c ); }
	private static Drop magic( float c ){ return new Drop( MagicMaterial::new, c ); }

	/**
	 * 英雄击杀敌人时调用：环指大师职业按怪物类型独立判定掉落素材。
	 * @param luckyProc 击杀时目标身上是否有幸运附魔触发标记（LuckProc 在 rollToDropLoot 中被消费，需由 Mob.die 提前捕获）
	 */
	public static void onEnemyKilled( Hero hero, Mob mob, boolean luckyProc ){
		if ( hero.heroClass != HeroClass.RING_MASTER ) return;
		Drop[] drops = TABLE.get( mob.getClass() );
		if ( drops == null ) return;
		//财富戒指掉率乘数（=1.2^财富等级，无财富戒指时为 1）
		float wealthMulti = RingOfWealth.dropChanceMultiplier( hero );
		rollDrops( drops, mob.pos, wealthMulti );
		// 环指大师 T2：素材采取——击杀时额外触发一次素材生成（+1=50%概率，+2=必定）
		if ( hero.hasTalent( Talent.MATERIAL_HARVEST ) ){
			int points = hero.pointsInTalent( Talent.MATERIAL_HARVEST );
			float extraChance = points == 1 ? 0.5f : 1f;
			if ( Random.Float() < extraChance ){
				rollDrops( drops, mob.pos, wealthMulti );
			}
		}
		// 幸运附魔适配：幸运触发时额外完整判定一次素材掉落
		if ( luckyProc ){
			rollDrops( drops, mob.pos, wealthMulti );
		}
	}

	/** 对一组掉落条目逐条独立判定（概率乘财富戒指乘数），命中即掉落到指定格。 */
	private static void rollDrops( Drop[] drops, int pos, float chanceMulti ){
		for ( Drop d : drops ){
			if ( Random.Float() < d.chance * chanceMulti ){
				Item m = d.factory.get();
				if ( m != null ) Dungeon.level.drop( m, pos );
			}
		}
	}

}
