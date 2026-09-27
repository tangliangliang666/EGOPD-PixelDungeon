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

/**
 * 「自伤换成长」道具的标记接口（2026-09-18）。
 *
 * <p>实现本接口的道具，会对<b>持有者自己</b>造成伤害——而那笔伤害不是「挨打」，是换取道具
 * <b>升级/成长</b>的代价：挨完这一下还活着，道具就升级。目前的实现者：</p>
 * <ul>
 *   <li>「蓄血圣杯」{@code items.artifacts.ChaliceOfBlood}：菜单里的「血祭」，每次 +1 级（上限 10）；</li>
 *   <li>「割腕」{@code items.weapon.melee.WristSlit}：菜单里的「割腕」，每次 +1 级。</li>
 * </ul>
 *
 * <p><b>为什么需要这个标记</b>：中指长兄的 T3「过人的毅力」（见 {@code Talent.perseveranceCap}）
 * 会把受到的伤害夹进「血量下限」里——血量已经压在下限之内时，单次伤害最多 1 点。这对普通战斗
 * 是保命，落到上面这两件道具身上却等于<b>无条件、无限制地刷等级</b>：血祭/割腕的升级判据只是
 * 「挨完这一下还活着」，而下限保护让人永远死不掉 ⇒ 每一下都成功升级、代价还被抹平。</p>
 *
 * <p>该标记目前有<b>两个</b>取用点，都是「别让保命机制把升级代价抹平」：</p>
 * <ol>
 *   <li><b>血量下限保护</b>——{@code Talent.perseveranceCap}（中指长兄 T3「过人的毅力」）对本接口的
 *       实现者整段跳过：它们造成的伤害一律原样放行（该疼就疼、该死就死）。</li>
 *   <li><b>免死</b>——{@code GritTeethBuff.checkBypass}（中指长兄盔甲技能「咬紧牙关」的 0 血不死）
 *       对本接口的实现者整段作废：这一下真的打到 0 血时，「免死」立即失效、照常死亡。
 *       否则血祭/割腕会被「0 血不死」兜住 ⇒ 每一下都升级成功、代价还被抹平，同样是无条件刷满。</li>
 * </ol>
 * <p>两处共用本接口这一个判据，<b>别</b>在任一处另开 {@code instanceof} 名单。</p>
 *
 * <p>注意这两条<b>只</b>豁免上述两种保命机制：护甲减伤、{@code RingOfTenacity} 之类的通用减伤照常生效。</p>
 *
 * <p>以后新增「自伤换升级」的道具，<b>实现本接口即可自动获得两处豁免</b>——不要在
 * {@code Talent.perseveranceCap} 或 {@code GritTeethBuff} 里逐个加 {@code instanceof} 分支。</p>
 */
public interface SelfHarmCost {
}
