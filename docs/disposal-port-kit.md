# 盔甲技能「处置」（Disposal）移植包

> **用途**：把 EGOPD 里「拇指 · 前二老板（瓦伦希娜 / VALENCINA）」的第二个盔甲技能
> **「处置」** 完整搬运到其它项目做联动复现。
>
> **整理日期**：2026-09-24｜**源码版本**：0.3.4 / 934
> **设计原文**：`拇指设计.txt` L72~L93（用户原始需求，见 §1.2）

---

## 1. 规格速览

### 1.1 机制一句话

消耗 **80% 盔甲充能**，对 **3 格内**一个敌方目标连续施展 **4 次巴勒莫剑术**；
**每次命中后把目标击退 1 格，然后追及贴身**再打下一段；
**前三段耗时减半、第四段完整**（总耗时 = **2.5 × 攻击延迟**）。

### 1.2 原始设计文本（`拇指设计.txt`）

```
处置：前二老板施展伯特纳利家族的巴勒莫剑术，对目标敌人施展4次连续的巴勒莫剑术，
      每次施展后都会击退并追及目标。
充能消耗：80

爆碎收尾:
+1 处置攻击结束后额外造成一段伤害，伤害等同于基础巴勒莫剑术伤害(武器攻击伤害/攻击延迟)的一半。
+2 额外伤害提升为基础巴勒莫剑术的伤害。
+3 除+1、+2外，额外伤害会击退目标3格。
+4 除+1、+2外，额外伤害会击退目标3格，并造成眩晕和麻痹。

枪拼刺杀：
+1 处置施展的前_1_次巴勒莫剑术不会消耗加速的未来。
+2 处置施展的前_2_次巴勒莫剑术不会消耗加速的未来。
+3 处置施展的前_3_次巴勒莫剑术不会消耗加速的未来。
+4 处置施展的前_4_次巴勒莫剑术不会消耗加速的未来。

毫无悬念：
+1 若使用处置击杀了敌人，则立刻恢复_20%_的盔甲充能。
+2 若使用处置击杀了敌人，则立刻恢复_40%_的盔甲充能。
+3 若使用处置击杀了敌人，则立刻恢复_60%_的盔甲充能。
+4 若使用处置击杀了敌人，则立刻恢复_80%_的盔甲充能。

（实现要求：前三段攻击的释放速度要加快，但第四段不变）
```

### 1.3 数值表

| 项 | 值 | 代码位置 |
|---|---|---|
| 充能消耗 | `baseChargeUse = 80f`（百分比，`ClassArmor.charge` 是 0~100 的 float） | `Disposal.java:47` |
| 目标范围 | `RANGE = 3` 格（曼哈顿距离 `Dungeon.level.distance`） | `PalermoFencing.java:80` |
| 段数 | 固定 `4` 段（硬编码在 `idx + 1 < 4`） | `PalermoFencing.java:637` |
| 每段伤害倍率 | `1f / baseDelay`（= 武器基础伤害 ÷ 攻击延迟，**不做下限钳制**） | `PalermoFencing.java:582` |
| 命中倍率 | 加速的未来 ≥1 层 → `Char.INFINITE_ACCURACY`（必中），否则普通判定 | `PalermoFencing.java:583` |
| 耗时累计 | 前 3 段各 `baseDelay × 0.5`，第 4 段 `baseDelay` ⇒ 总计 `2.5 × baseDelay` | `PalermoFencing.java:578` |
| 击退 | 每段命中后推 1 格（Boss/MiniBoss/IMMOVABLE 免疫） | `PalermoFencing.java:625` |
| 层数联动 | ≥3 层：麻痹 5 回合；≥5 层：再点燃 | `PalermoFencing.java:597-602` |
| 扣费时机 | **全部前置校验通过、连击已启动**后由调用方扣（失败不白扣） | `Disposal.java:71-75` |

---

## 2. 文件与依赖地图

```
actors/hero/abilities/valencina/Disposal.java        ← 技能外壳（ArmorAbility 子类，80 行）
            └─ calls ─► items/weapon/melee/PalermoFencing.java
                            ├─ disposal()        入口 + 校验 + 起手
                            ├─ disposalStrike()  第 idx 段（递归，sprite.jump 回调续段）
                            ├─ disposalFinish()  收尾：耗时/返还/爆碎收尾/毫无悬念
                            ├─ knockBack()       击退（含 pushAwayCell）
                            ├─ landingPos()      起手落点（从剑术本体复用）
                            ├─ canLand()         落点合法性（复用）
                            ├─ randomLanding()   段间随机落点（复用）
                            └─ tryExecute()      家族之耻「宰杀处置」斩杀（复用）
            └─ calls ─► actors/hero/abilities/valencina/ValencinaSfx.java
                            ├─ playDisposalVoice()  施放语音 + 头顶台词（成对随机）
                            └─ playPalermoHit()     命中音效（八选一）
```

| 依赖符号 | 类型 | 目标项目替代方案 |
|---|---|---|
| `ClassArmor` / `ArmorAbility` | 上游 SPD 基类 | 若目标项目非 SPD，需自建「充能槽 + 技能槽」抽象 |
| `AcceleratingFuture`（`getStacks/gain/lose/suppressStrike`） | 本 mod 专属 buff「加速的未来」 | 换成任意「按层数结算的资源」；无此系统时**整块删掉层数联动**（见 §5.2） |
| `Talent.BURST_FINISH / LUNGE_STAB / NO_SUSPENSE` | 本 mod 四阶天赋（各 4 点） | 换成技能自身的等级参数 / 配置常量 |
| `Talent.ValencinaSwordRail` | 家族之耻专精 T2「叠加剑轨」buff | 可整块删除（仅影响分支强化） |
| `Talent.VALENCINA_T3_SHAME_2/3` + `HeroSubClass.FAMILY_SHAME` | 家族之耻分支 | 可整块删除 |
| `OdinsEyeOverheat` | 预知眼过热 buff（多个分支加成乘子） | 可整块删除 |
| `ValencinaSfx` | 音效集中入口 | **建议保留结构、清空实现**（语音素材版权相关） |
| `MeleeWeapon.beforeAbilityUsed / afterAbilityUsed / onAbilityKill` | 武技公共钩子 | 上游/本 mod 均有；复现时至少保留扣充能与击杀回调 |
| `AttackIndicator.target / Invisibility.dispel / hero.busy / spendAndNext` | 表现与回合结算 | SPD 原生 |
| `Paralysis / Burning / Vertigo` | SPD 原生 buff | 直接可用 |
| `Roots / Cripple / Haste / Swiftthistle.TimeBubble` | SPD 原生 buff（极速追杀用） | 可删 |
| `Char.INFINITE_ACCURACY` | 本 mod 新增常量（必中倍率） | 换成 `Float.MAX_VALUE` 或目标项目的必中实现 |
| `HeroIcon.DISPOSAL(=124)` | 图标帧号 | 换成目标项目空闲帧 |
| `Talent.BURST_FINISH(308)/LUNGE_STAB(309)/NO_SUSPENSE(310)` | 天赋枚举，各 4 点 | 换成参数 |

---

## 3. 完整源码

### 3.1 `actors/hero/abilities/valencina/Disposal.java`（全文，88 行）

```java
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.valencina;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.PalermoFencing;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/**
 * 拇指 前二老板的盔甲技能「处置」（2026-09-08）。
 * <p>消耗 80% 盔甲充能，对 3 格内目标连续施展 4 次巴勒莫剑术，每次命中后击退目标 1 格并追及；
 * 前 3 次耗时减半、第 4 次完整（总耗时 = 2.5 × 攻击延迟）。</p>
 * <p>对应天赋：爆碎收尾（收尾伤害/击退/眩晕麻痹）、枪拼刺杀（前 N 次不消耗「加速的未来」）、
 * 毫无悬念（击杀恢复盔甲充能）。</p>
 */
public class Disposal extends ArmorAbility {

	{
		baseChargeUse = 80f; //消耗 80% 盔甲充能
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {
		if (target == null) return;

		Char enemy = Actor.findChar( target );
		if (enemy == null || enemy == hero || hero.isCharmedBy( enemy )){
			GLog.w( Messages.get(this, "no_target") );
			return;
		}

		if (!(hero.belongings.attackingWeapon() instanceof MeleeWeapon)){
			GLog.w( Messages.get(this, "no_weapon") );
			return;
		}

		//校验全部通过（disposal 内部完成距离/扎根/落点等检查）后才扣充能，失败不白扣
		if (PalermoFencing.disposal( hero, target, (MeleeWeapon) hero.belongings.attackingWeapon(), armor )){
			armor.charge -= chargeUse( hero );
			armor.updateQuickslot();
			Invisibility.dispel();
		}
	}

	@Override
	public int icon() {
		return HeroIcon.DISPOSAL;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.BURST_FINISH, Talent.LUNGE_STAB, Talent.NO_SUSPENSE, Talent.HEROIC_ENERGY};
	}
}
```

> **要点**：`talents()` 返回的 4 个天赋（前 3 个专属 + 第 4 个通用 `HEROIC_ENERGY`）
> 就是盔甲技能页签里显示的天赋槽，这是 SPD 的既定约定，移植时保持「3 专属 + 1 通用」结构即可。

---

### 3.2 `items/weapon/melee/PalermoFencing.java` — 处置相关切片

#### 3.2.1 施放级静态标志（`PalermoFencing.java:79-109` 节选）

```java
	/** 巴勒莫剑术的目标选取范围（格）。 */
	public static final int RANGE = 3;

	//施放上下文（攻击钩子用）：标记“当前 hero.attack 是否为巴勒莫剑术的一段”，以及本次武技开始时的加速的未来层数
	private static boolean fencingSegment = false;
	private static int castStacks = 0;

	//震颤-灼热（战争英雄）施放级一次性标志：一次巴勒莫剑术只施加一次层数，多段斩不重复叠加
	private static boolean tremorApplied = false;

	//宰杀处置（家族之耻）施放级标志：本次施放触发斩杀 → 整套剑术结束时额外 +3 层加速的未来
	private static boolean executeBonusPending = false;

	/** 当前攻击是否为巴勒莫剑术的一段（供 Talent 命中钩子识别）。 */
	public static boolean isFencingSegment() {
		return fencingSegment;
	}

	/** 本次武技开始时的「加速的未来」层数（段内恒定，供层数限定天赋使用）。 */
	public static int fencingCastStacks() {
		return castStacks;
	}
```

> `fencingSegment` 是**全局 static 标志**，用 try/finally 包住 `hero.attack(...)`。
> 它的作用是让 `Talent` 的命中钩子知道「这一击属于巴勒莫剑术」——移植时若目标项目没有这套钩子，
> 这个字段及 `isFencingSegment()` 可以整块删掉（处置本身不读它）。

#### 3.2.2 入口 `disposal()`（`PalermoFencing.java:491-569`，含原文注释）

```java
	/**
	 * 「处置」：对目标连续施展 4 次巴勒莫剑术，每次命中后击退目标 1 格并追及。
	 * 前 3 次耗时减半、第 4 次完整（总耗时 = 2.5 × 当前攻击延迟）。
	 * 「枪拼刺杀」豁免前 N 次剑术对「加速的未来」的消耗（首次非豁免段结束后才清空层数）。
	 * 「爆碎收尾」在整套结束后追加一段收尾伤害；「毫无悬念」在处置击杀敌人后恢复盔甲充能。
	 *
	 * @return true 表示连击已成功启动（调用方此时才扣充能）
	 */
	public static boolean disposal( final Hero hero, Integer target, final MeleeWeapon wep, final ClassArmor armor ){
		if (target == null || hero == null) return false;
		final Char enemy = Actor.findChar( target );
		if (enemy == null || enemy == hero || hero.isCharmedBy( enemy )){
			GLog.w( Messages.get( PalermoFencing.class, "no_target" ) );
			return false;
		}
		if (hero.rooted){
			GLog.w( Messages.get( PalermoFencing.class, "blocked" ) );
			return false;
		}
		int dist = Dungeon.level.distance( hero.pos, enemy.pos );
		if (dist > RANGE){
			GLog.w( Messages.get( PalermoFencing.class, "out_of_range" ) );
			return false;
		}

		final int dest;
		if (dist <= 1){
			dest = hero.pos;
		} else {
			dest = landingPos( hero, enemy );
			if (dest == -1){
				GLog.w( Messages.get( PalermoFencing.class, "blocked" ) );
				return false;
			}
		}

		final float baseDelay = Math.max( 0.01f, hero.attackDelay() );
		final int exempt = hero.pointsInTalent( Talent.LUNGE_STAB ); //枪拼刺杀：前 N 次不消耗

		//可变状态
		final float[] total = {0f};
		final boolean[] consumed = {false};
		final int[] spent = {0};
		final boolean[] killed = {false};

		//语音 + 头顶台词：本次「处置」的两条台词成对随机取用（防止显示 A、念的却是 B）
		ValencinaSfx.playDisposalVoice( hero );

		hero.busy();
		AttackIndicator.target( enemy );
		wep.beforeAbilityUsed( hero, enemy );

		//整套连击期间抑制「加速的未来」获取（与单次剑术一致）
		AcceleratingFuture.suppressStrike( true );
		tremorApplied = false;
		executeBonusPending = false;

		Callback strikeStart = new Callback() {
			@Override
			public void call() {
				disposalStrike( hero, enemy, wep, armor, baseDelay, exempt, 0, total, consumed, spent, killed );
			}
		};

		if (dest == hero.pos){
			hero.sprite.attack( enemy.pos, strikeStart );
		} else {
			hero.sprite.jump( hero.pos, dest, new Callback() {
				@Override
				public void call() {
					hero.move( dest );
					Dungeon.level.occupyCell( hero );
					Dungeon.observe();
					GameScene.updateFog();
					hero.sprite.attack( enemy.pos, strikeStart );
				}
			} );
		}
		//所有前置校验均通过、连击已开始 → 返回 true（调用方才扣充能）
		return true;
	}
```

#### 3.2.3 单段 `disposalStrike()`（`PalermoFencing.java:571-659`）

```java
	/** 处置的第 idx 段（0~3）。 */
	private static void disposalStrike( final Hero hero, final Char enemy, final MeleeWeapon wep, final ClassArmor armor,
	                                    final float baseDelay, final int exempt, final int idx,
	                                    final float[] total, final boolean[] consumed, final int[] spent,
	                                    final boolean[] killed ){

		//耗时：前三段减半，第四段完整
		total[0] += (idx < 3) ? baseDelay * 0.5f : baseDelay;

		AcceleratingFuture af = hero.buff( AcceleratingFuture.class );
		int stacks = (af != null) ? af.getStacks() : 0;
		float dmgMulti = (baseDelay > 0f) ? 1f / baseDelay : 1f;
		float accMulti = (stacks >= 1) ? Char.INFINITE_ACCURACY : 1f;

		boolean hit;
		fencingSegment = true;
		try {
			hit = hero.attack( enemy, dmgMulti, 0, accMulti );
		} finally {
			fencingSegment = false;
		}

		if (hit){
			//攻击音效：与单次剑术同一组（技能3 前八条随机其一，取代原版 HIT_STRONG）
			ValencinaSfx.playPalermoHit();
			//旧通用特效（同单次剑术）
			if (stacks >= 3){
				Buff.affect( enemy, Paralysis.class, 5f );
			}
			if (stacks >= 5){
				Buff.affect( enemy, Burning.class ).reignite( enemy );
			}
			if (!enemy.isAlive()){
				MeleeWeapon.onAbilityKill( hero, enemy );
				killed[0] = true;
			}
		}

		//家族之耻「宰杀处置」：命中后照常尝试斩杀（斩杀奖励在整套结束时结算）
		if (hit && enemy.isAlive()
				&& hero.subClass == HeroSubClass.FAMILY_SHAME
				&& hero.hasTalent( Talent.VALENCINA_T3_SHAME_3 )){
			if (tryExecute( hero, enemy )){
				executeBonusPending = true;
				killed[0] = true;
			}
		}

		if (!enemy.isAlive() || !hero.isAlive()){
			disposalFinish( hero, enemy, wep, armor, total, consumed, spent, killed );
			return;
		}

		//每次施展后击退目标 1 格
		knockBack( hero, enemy, 1 );

		//「枪拼刺杀」：前 N 段不消耗；首次非豁免段结束后清空「加速的未来」（返还等结算于结束时）
		if (!consumed[0] && idx >= exempt){
			af = hero.buff( AcceleratingFuture.class );
			if (af != null && af.getStacks() > 0){
				consumed[0] = true;
				spent[0] = af.getStacks();
				AcceleratingFuture.lose( hero );
			}
		}

		if (idx + 1 < 4 && hero.isAlive() && enemy.isAlive()){
			if (!hero.rooted){
				int land = randomLanding( hero, enemy );
				if (land != -1 && land != hero.pos){
					final int next = idx + 1;
					hero.sprite.jump( hero.pos, land, new Callback() {
						@Override
						public void call() {
							hero.move( land );
							Dungeon.level.occupyCell( hero );
							Dungeon.observe();
							GameScene.updateFog();
							disposalStrike( hero, enemy, wep, armor, baseDelay, exempt, next, total, consumed, spent, killed );
						}
					} );
					return;
				}
			}
			disposalStrike( hero, enemy, wep, armor, baseDelay, exempt, idx + 1, total, consumed, spent, killed );
		} else {
			disposalFinish( hero, enemy, wep, armor, total, consumed, spent, killed );
		}
	}
```

> **为什么用 `float[]`/`boolean[]`/`int[]` 做参数**：整条连击是**递归 + 精灵回调**式的异步流程
> （`sprite.attack` / `sprite.jump` 的 `Callback`），局部变量无法跨回调存活，故用「单元素数组」当可变容器。
> 移植到其它引擎时，可换成独立的 `DisposalState` 对象，语义更清晰。

#### 3.2.4 收尾 `disposalFinish()`（`PalermoFencing.java:661-744`）

```java
	/** 处置整套结束：结算耗时、奖励、返还与充能恢复。 */
	private static void disposalFinish( Hero hero, Char enemy, MeleeWeapon wep, ClassArmor armor,
	                                    float[] total, boolean[] consumed, int[] spent, boolean[] killed ){
		AcceleratingFuture.suppressStrike( false );

		boolean familyShame = hero.subClass == HeroSubClass.FAMILY_SHAME;

		//家族之耻「叠加剑轨」：整套处置计为一次剑术施放 → +1 层
		if (familyShame && hero.hasTalent( Talent.VALENCINA_T3_SHAME_2 )){
			Talent.ValencinaSwordRail.addStack( hero );
		}

		//家族之耻核心返还：被清零层数的一半（只在确实发生消耗时）
		if (consumed[0] && spent[0] > 0 && familyShame){
			AcceleratingFuture.gain( hero, spent[0] / 2 );
		}
		//家族之耻「宰杀处置」：处置中触发斩杀 → +3 层
		if (familyShame && executeBonusPending){
			executeBonusPending = false;
			AcceleratingFuture.gain( hero, 3 );
		}

		//「爆碎收尾」：整套结束后对仍存活的目标追加一段收尾伤害
		//（+1 为一次基础剑术伤害的一半，+2 起为全额；+3/+4 附带击退 3 格，+4 再眩晕并麻痹）
		if (hero.isAlive() && enemy != null && enemy.isAlive() && !killed[0]){
			int burst = hero.pointsInTalent( Talent.BURST_FINISH );
			if (burst > 0 && Dungeon.level.distance( hero.pos, enemy.pos ) <= RANGE){
				//第 4 段已把目标击退：先追及贴身；被定身或无处落脚则放弃本次收尾
				//（注意：不能在此 return——必须走到末尾 spendAndNext，否则英雄会永久卡死）
				boolean burstReady = true;
				if (Dungeon.level.distance( hero.pos, enemy.pos ) > 1){
					if (hero.rooted){
						burstReady = false; //无法追及（rooted，正常不会出现）
					} else {
						int land = randomLanding( hero, enemy );
						if (land == -1 || land == hero.pos){
							burstReady = false;
						} else {
							hero.move( land );
							Dungeon.level.occupyCell( hero );
							Dungeon.observe();
							GameScene.updateFog();
						}
					}
				}
				if (burstReady){
					float burstDelay = Math.max( 0.01f, hero.attackDelay() );
					float burstDmg = ((burst >= 2) ? 1f : 0.5f) / burstDelay;
					boolean burstHit;
					fencingSegment = true;
					try {
						burstHit = hero.attack( enemy, burstDmg, 0, Char.INFINITE_ACCURACY );
					} finally {
						fencingSegment = false;
					}
					if (burstHit && enemy.isAlive()){
						if (burst >= 3){
							knockBack( hero, enemy, 3 );
						}
						if (burst >= 4){
							Buff.affect( enemy, Vertigo.class, 5f );
							Buff.affect( enemy, Paralysis.class, 5f );
						}
					}
				}
			}
		}

		fencingSegment = false;
		castStacks = 0;
		tremorApplied = false;

		//「毫无悬念」：处置击杀恢复盔甲充能 20/40/60/80%
		int noSus = hero.pointsInTalent( Talent.NO_SUSPENSE );
		if (killed[0] && noSus > 0 && armor != null){
			armor.charge = Math.min( 100f, armor.charge + 20f * noSus );
			armor.updateQuickslot();
		}

		Invisibility.dispel();
		if (!hero.isAlive()) return;
		hero.spendAndNext( total[0] );
		wep.afterAbilityUsed( hero );
	}
```

#### 3.2.5 击退与推格（`PalermoFencing.java:789-832`）

```java
	/** 击退：把目标沿远离施放者的方向推动 tiles 格（撞墙/被占/不可移动/Boss 时停在原地）。 */
	private static void knockBack( Hero hero, Char enemy, int tiles ){
		if (enemy == null || !enemy.isAlive() || enemy == hero) return;
		if (Char.hasProp( enemy, Char.Property.BOSS )
				|| Char.hasProp( enemy, Char.Property.MINIBOSS )
				|| Char.hasProp( enemy, Char.Property.IMMOVABLE )) return;

		int cur = enemy.pos;
		boolean moved = false;
		for (int i = 0; i < tiles; i++){
			int next = pushAwayCell( hero.pos, cur );
			if (next == -1) break;
			cur = next;
			moved = true;
		}
		if (moved){
			int from = enemy.pos;
			enemy.pos = cur;
			Dungeon.level.occupyCell( enemy );
			if (enemy.sprite != null){
				enemy.sprite.place( cur );
			}
			if (Dungeon.level.heroFOV != null && Dungeon.level.heroFOV.length > Math.max(from, cur)){
				GameScene.updateFog();
			}
		}
	}

	/** 计算把 cur 处的单位沿远离 from 的方向推 1 格的目标格（不可行返回 -1）。 */
	private static int pushAwayCell( int from, int cur ){
		int w = Dungeon.level.width();
		int fx = from % w;
		int fy = from / w;
		int cx = cur % w;
		int cy = cur / w;
		int dx = (int)Math.signum( cx - fx );
		int dy = (int)Math.signum( cy - fy );
		if (dx == 0 && dy == 0) return -1;
		int n = cur + dx + dy * w;
		if (!Dungeon.level.insideMap( n )) return -1;
		if (!Dungeon.level.passable[n]) return -1;
		if (Actor.findChar( n ) != null) return -1;
		return n;
	}
```

> ⚠️ `knockBack` **直接改 `enemy.pos`**，不走 `Actor.move`，所以不会触发移动相关的 buff/AI 状态。
> 目标若有自己的移动逻辑，需评估是否需要补 `enemy.sprite.jump` 之类的表现。

#### 3.2.6 复用的辅助方法（同样必须一起搬）

```java
	/** 计算突进落点：优先目标正前方（直线路径紧邻目标的一格），否则绕到目标侧面最近的一格；无合适落点返回 -1。 */
	private static int landingPos( Hero hero, Char enemy ){

		//1. 直线突进：目标正前方一格
		Ballistica line = new Ballistica( hero.pos, enemy.pos, Ballistica.PROJECTILE );
		if (line.collisionPos == enemy.pos && line.dist > 1){
			int front = line.path.get( line.dist - 1 );
			if (canLand( hero, front )){
				return front;
			}
		}

		//2. 绕侧：目标周围、从英雄处直线可达的空位中最近的一格
		int best = -1;
		int bestDist = Integer.MAX_VALUE;
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++){
			int n = enemy.pos + PathFinder.NEIGHBOURS8[i];
			if (!Dungeon.level.insideMap( n ) || !canLand( hero, n )){
				continue;
			}
			Ballistica side = new Ballistica( hero.pos, n, Ballistica.PROJECTILE );
			if (side.collisionPos != n){
				continue; //中间被挡，跳过去会被墙体拦住
			}
			int d = Dungeon.level.distance( hero.pos, n );
			if (d < bestDist){
				bestDist = d;
				best = n;
			}
		}
		return best;
	}

	/** 落点是否可站：可通行（或飞行者落在坑洞上空）且没有其它角色占据。 */
	private static boolean canLand( Hero hero, int cell ){
		if (Actor.findChar( cell ) != null) return false;
		return Dungeon.level.passable[cell] || (hero.flying && Dungeon.level.avoid[cell]);
	}

	/** 多段斩的段间落点：目标 8 邻格的随机空位；无空位返回 -1（保持原位）。 */
	private static int randomLanding( Hero hero, Char enemy ){
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++){
			int n = enemy.pos + PathFinder.NEIGHBOURS8[i];
			if (Dungeon.level.insideMap( n ) && canLand( hero, n )){
				candidates.add( n );
			}
		}
		if (candidates.isEmpty()) return -1;
		return Random.element( candidates );
	}
```

**「宰杀处置」斩杀**（家族之耻分支用；不需要分支强化时可整块删除）：

```java
	/** 家族之耻「宰杀处置」：目标非 boss/miniboss 且生命低于斩杀线（(10+10×点数)%，过热时 +10）时处决之。
	 *  处决本身不结算为剑术普通击杀（不占用命中后的普通死亡路径）。 */
	private static boolean tryExecute( Hero hero, Char enemy ){
		int points = hero.pointsInTalent( Talent.VALENCINA_T3_SHAME_3 );
		if (points <= 0) return false;
		if (Char.hasProp( enemy, Char.Property.BOSS ) || Char.hasProp( enemy, Char.Property.MINIBOSS )) return false;

		boolean overheated = hero.buff( OdinsEyeOverheat.class ) != null;
		float threshold = 0.1f * (points + 1 + (overheated ? 1 : 0)); //+1/2/3 → 20/30/40%，过热 +10%
		if (enemy.HP / (float) enemy.HT >= threshold) return false;

		enemy.HP = 0;
		if (enemy.buff( Brute.BruteRage.class ) != null){
			enemy.buff( Brute.BruteRage.class ).detach();
		}
		if (!enemy.isAlive()) {
			enemy.die( hero );
		} else {
			//helps with triggering any on-damage effects that need to activate
			enemy.damage( -1, hero );
			DeathMark.processFearTheReaper( enemy );
		}
		if (enemy.sprite != null){
			enemy.sprite.showStatus( CharSprite.NEGATIVE, Messages.get( PalermoFencing.class, "executed" ) );
		}
		MeleeWeapon.onAbilityKill( hero, enemy );
		//瞄准心脏「荣耀凯旋」：处决同样结算击杀被标记目标的恢复
		if (enemy.buff( AimHeartMark.class ) != null){
			AimHeartMark.onKillByHero( hero );
		}
		return true;
	}
```

---

### 3.3 `actors/hero/abilities/valencina/ValencinaSfx.java` — 处置的两条播放口

```java
	/**
	 * 「处置」（盔甲技能2）施放时的语音与台词。
	 * <p>⚠️ 必须成对取用：文本与语音各自独立随机会出现「显示 A、念的却是 B」的错配。</p>
	 */
	private static final String[][] DISPOSAL_LINES = {
			{ "我爱恨就恨！我恨所有那该死的家族，你们，还有券券！", Assets.Sounds.VALENCINA_DISPOSAL_VOICE_1 },
			{ "烦死了，烦死了啊！你们和券券，全都烦死了！！！", Assets.Sounds.VALENCINA_DISPOSAL_VOICE_2 },
	};

	/** 台词浮字的颜色（沿用原作台词页的暖米色）。 */
	private static final int LINE_COLOR = 0xE5CAA5;

	/** 巴勒莫剑术命中时的攻击音效（八选一）。 */
	private static final String[] PALERMO_HITS = {
			Assets.Sounds.VALENCINA_PALERMO_HIT_1, Assets.Sounds.VALENCINA_PALERMO_HIT_2,
			Assets.Sounds.VALENCINA_PALERMO_HIT_3, Assets.Sounds.VALENCINA_PALERMO_HIT_4,
			Assets.Sounds.VALENCINA_PALERMO_HIT_5, Assets.Sounds.VALENCINA_PALERMO_HIT_6,
			Assets.Sounds.VALENCINA_PALERMO_HIT_7, Assets.Sounds.VALENCINA_PALERMO_HIT_8,
	};

	/** 这些语音只在拇指（前二老板）身上播放。 */
	public static boolean isValencina() {
		return Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.VALENCINA;
	}

	/** 「处置」施放时的语音 + 头顶同步台词（成对随机）。 */
	public static void playDisposalVoice( Hero hero ) {
		if (!isValencina()) return;

		String[] line = Random.element( DISPOSAL_LINES );
		Sample.INSTANCE.play( line[1] );
		if (hero != null && hero.sprite != null) {
			hero.sprite.showStatus( LINE_COLOR, line[0] );
		}
	}

	/** 巴勒莫剑术命中时的攻击音效（八选一）。 */
	public static void playPalermoHit() {
		Sample.INSTANCE.play( Random.element( PALERMO_HITS ) );
	}
```

> **移植建议**：把 `isValencina()` 的判定换成目标项目的「当前角色 == 本技能持有者」，
> 或直接删掉判定。**两条台词与语音必须成对从同一数组取**——若各自独立随机，会出现
> 「显示 A 却念 B」的错配（这是踩过的坑，已在原注释里标出）。

---

## 4. 接线点（复现时必改的 6 处）

| # | 文件 | 位置 | 内容 |
|---|---|---|---|
| 1 | `actors/hero/HeroClass.java:523` | `armorAbilities()` | `case VALENCINA: return new ArmorAbility[]{new IgnominiousHeart(), new Disposal(), new AimHeart()};`（**技能槽顺序 = 存档索引，不要插在中间**） |
| 2 | `actors/hero/HeroClass.java:309` | `initValencina()` | 初始武器 `new WornTwinSword()` + `hero.belongings.weapon.activate(hero)`（挂 Charger，否则开局放不出武技） |
| 3 | `items/armor/ThumbCoat.java` | 全文 38 行 | `ClassArmor` 子类，只设 `image = ItemSpriteSheet.ARMOR_VALENCINA`（第 12 行第 14 列，14×16） |
| 4 | `ui/HeroIcon.java:77` | 常量 | `public static final int DISPOSAL = 124;`（`hero_icons.png` 第 16 行第 5 列） |
| 5 | `actors/hero/Talent.java:321` | 枚举 | `BURST_FINISH(308, 4), LUNGE_STAB(309, 4), NO_SUSPENSE(310, 4),`（末位数字 = 可加点上限 4） |
| 6 | `messages/actors/actors_{zh,en}.properties` | 文本键 | 见 §4.1 |

### 4.1 文本键（zh 原文）

```properties
actors.hero.abilities.valencina.disposal.name=处置
actors.hero.abilities.valencina.disposal.short_desc=施展_4次连续_的巴勒莫剑术，每次施展后都会_击退并追及_目标。
actors.hero.abilities.valencina.disposal.desc=前二老板施展伯特纳利家族的巴勒莫剑术，对目标敌人施展_4次连续_的巴勒莫剑术，每次施展后都会_击退并追及_目标。
actors.hero.abilities.valencina.disposal.prompt=选择巴勒莫剑术的目标（3格内）
actors.hero.abilities.valencina.disposal.no_target=这里没有可以施展巴勒莫剑术的目标！
actors.hero.abilities.valencina.disposal.no_weapon=你当前没有装备可以施展巴勒莫剑术的近战武器！
```

剑术本体消息键（`PalermoFencing` 复用，`items_zh.properties`）：

```properties
items.weapon.melee.palermofencing.prompt=选择巴勒莫剑术的目标（3格内）
items.weapon.melee.palermofencing.no_target=这里没有可以施展巴勒莫剑术的目标！
items.weapon.melee.palermofencing.out_of_range=目标超出了巴勒莫剑术的攻击范围！
items.weapon.melee.palermofencing.blocked=无法接近目标！路径被阻挡，或目标周围没有落脚之处。
items.weapon.melee.palermofencing.executed=斩杀
```

### 4.2 音效常量（`Assets.Sounds`，需同时加入 `ASSET_LIST`）

```java
public static final String VALENCINA_DISPOSAL_VOICE_1 = "sounds/valencina_disposal_voice_1.mp3";
public static final String VALENCINA_DISPOSAL_VOICE_2 = "sounds/valencina_disposal_voice_2.mp3";
public static final String VALENCINA_PALERMO_HIT_1..8  = "sounds/valencina_palermo_hit_N.mp3";
```

> **踩坑**：SPD 的 `Assets` 里所有要预载的音效必须**同时**出现在 `ASSET_LIST` 数组中
> （`Assets.java:325-333`），只声明常量不加进列表 → 运行时静默无声（不报错）。

---

## 5. 移植步骤（顺序执行）

### 5.1 五步走

1. **先搬纯逻辑内核**：`landingPos / canLand / randomLanding / knockBack / pushAwayCell`
   —— 这 5 个方法只依赖 `Dungeon.level` / `Actor` / `PathFinder` / `Ballistica`，无 mod 依赖，先让它编译通过。
2. **再搬 `disposal / disposalStrike / disposalFinish`**：此时会报一堆「找不到符号」，
   按 §5.2 的降级表逐个替换。
3. **建技能外壳**：`Disposal extends ArmorAbility`，`baseChargeUse = 80f`，
   `icon()` 指向一个空闲帧，`talents()` 返回 4 个（前 3 可先用占位枚举）。
4. **接线**：`HeroClass.armorAbilities()` 加进数组；补文本键（zh/en 双份）；
   音效常量 + `ASSET_LIST`。
5. **核验**：见 §5.3。

### 5.2 依赖降级表（无对应系统时怎么砍）

| 原依赖 | 降级做法 | 影响 |
|---|---|---|
| `AcceleratingFuture` 层数联动 | 把 `stacks` 恒置 0 | 自动失去「必中/麻痹/点燃」与家族之耻返还；**核心 4 连打 + 击退追及完全保留** |
| `Talent.LUNGE_STAB` | 换 `final int exempt = 0;` 或配置字段 | 枪拼刺杀失效 |
| `Talent.BURST_FINISH` | 换 `int burst = 0;` | 爆碎收尾失效 |
| `Talent.NO_SUSPENSE` | 换 `int noSus = 0;` | 充能恢复失效 |
| `HeroSubClass.FAMILY_SHAME` 整块 | 全部 `familyShame = false` | 剑轨/返还/斩杀失效 |
| `ValencinaSfx` | 保留类名，方法体置空（或 `if (false) return;`） | 无语音 |
| `fencingSegment` / `isFencingSegment()` | 整块删（处置不读） | 无影响 |
| `Char.INFINITE_ACCURACY` | `Float.MAX_VALUE` | 无影响 |

> **最小可运行内核**：只保留 `disposal() + disposalStrike() + disposalFinish()` 中
> 「4 段攻击 → 击退 → 追及 → `spendAndNext(2.5×delay)`」这条主线，
> 其余全砍后约 120 行，是本技能真正的「联动点」。

### 5.3 移植后核验清单

- [ ] **扣费语义**：故意让落点被占（`blocked`），确认充能**没被扣**（原实现的关键设计）。
- [ ] **免费不卡死**：连击途中把目标打死 / 英雄被反伤打死，确认英雄回合正常结束（`spendAndNext` 被调用）。
- [ ] **总耗时**：用攻击延迟 1.0 的武器，确认动作总耗时 = 2.5 回合（前 3 段各 0.5 + 第 4 段 1.0）。
- [ ] **快武器不被压回 ×1**：用延迟 0.5 的武器，确认单段伤害 ≈ 2× 基础伤害
      （**绝不能**把 `delay` 钳制到 ≥1，原注释明确警告过这一点）。
- [ ] **击退免疫**：对 Boss/MiniBoss/`IMMOVABLE` 目标施放，确认原地不动但伤害照常。
- [ ] **落点全封**：目标四周被墙/单位围死时，确认提示 `blocked` 且不消耗充能；能贴到侧面时走「绕侧」分支。
- [ ] **灼烧/麻痹**（若保留层数联动）：≥3 层麻痹 5 回合、≥5 层点燃。

---

## 6. 实现不变量与踩坑记录

1. **扣充能在成功后**（`Disposal.java:71-75`）：`disposal()` 返回 `boolean`，
   内部做完目标/距离/扎根/落点全部校验才返回 `true`，调用方才扣 80%。
   ⚠️ 不要改成「先扣后调」——会出现「点错格子白掉 80% 充能」。
2. **`delay` 不做下限钳制**（`PalermoFencing.java:582`）：伤害倍率 = `1f / baseDelay`，
   快武器（0.5 延迟）需要 ×2 放大。原实现曾用 `max(delay,1f)`，会把快武器压回 ×1、慢武器缩小，
   已删除并留注释警告。
3. **`spendAndNext` 绝不能提前 return**（`PalermoFencing.java:689`）：
   爆碎收尾分支里若「无法追及」就直接 `return`，英雄会**永久卡死**（动作不结束）。
   原实现用 `burstReady` 标志位绕开，保证必然走到末尾。
4. **`suppressStrike` 必须成对**（`true` 于 `disposal()`，`false` 于 `disposalFinish()`）：
   抑制「加速的未来」在连击期间被本武技自己的攻击刷层。
   ⚠️ 若中途异常抛出，`false` 不会被调用 → 建议包 try/finally（原实现靠流程保证）。
5. **语音与台词成对取**（`ValencinaSfx.playDisposalVoice`）：两条台词与两条语音必须来自**同一次随机**。
6. **`fencingSegment` 用 try/finally 复位**：`hero.attack()` 抛异常时标志不能残留，
   否则后续所有攻击都会被 `Talent` 误判为剑术段。
7. **`knockBack` 直接改 `pos`**：不走 `Actor.move`，因此不触发移动 buff；
   但同时必须手动 `Dungeon.level.occupyCell()` + `sprite.place()` + `GameScene.updateFog()`，
   三者缺一会导致「格子占用错乱 / 显示残影 / 迷雾不刷新」。
8. **`RANGE` 是共享常量**：处置的「3 格内」与巴勒莫剑术本体、AimHeart 共享 `PalermoFencing.RANGE = 3`；
   若要给处置单独调范围，需另开常量而**不要改这个**（会连带改剑术与瞄准心脏）。

---

## 7. 附：与本技能相关的其它文件（联动时需要，但非「处置」本体）

| 文件 | 与本技能的关系 |
|---|---|
| `items/weapon/melee/MeleeWeapon.java:186-192` | `dispatchAbility()`：VALENCINA 角色点武器武技时固定走 `PalermoFencing.cast()` |
| `items/weapon/melee/MeleeWeapon.java:194-213` | `beforeAbilityUsed()`：扣 Charger 充能 + 记录 `abilityWeapon` |
| `actors/buffs/AcceleratingFuture.java` | 「加速的未来」资源池：`DURATION=3f`、`getStacks/gain/lose/suppressStrike` |
| `actors/hero/Talent.java:1514-1563` | `ValencinaSwordRail`（叠加剑轨 buff，上限 5 层） |
| `actors/hero/Talent.java:1599+` | `onHeroDodgedEnemyAttack`（家族之耻 T1 免费反击，内部调 `PalermoFencing.freeSwing`） |
| `actors/hero/abilities/valencina/AimHeart.java:115` | 瞄准心脏「碾杀虫豸」→ `PalermoFencing.freeSwing()`（免回合免费剑术） |
| `actors/hero/Talent.java:979-983` | 非剑术攻击清空剑轨（读 `PalermoFencing.isFencingSegment()`） |
