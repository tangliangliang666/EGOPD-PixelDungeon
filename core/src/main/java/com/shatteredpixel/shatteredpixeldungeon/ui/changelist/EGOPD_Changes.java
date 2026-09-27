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

package com.shatteredpixel.shatteredpixeldungeon.ui.changelist;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;

import java.util.ArrayList;

/**
 * [EGOPD] EGOPD 自身版本的改动栏（改动界面里最左侧那一页）。
 *
 * 这里是记录 EGOPD 自己改动历程的**唯一**位置，刻意与上游的 v?_X_Changes 分开：
 *   - 每出一个 EGOPD 版本，加一个 major 条目：new ChangeInfo("EGOPD vX.Y.Z", true, "")
 *   - 该版本下的每条改动用 changes.addButton(new ChangeButton(图标, "标题", "正文")) 追加
 *   - 正文支持 "**强调**" / "_强调_" 与 "\n\n" 换行
 *   - 图标可以是 new ItemSprite(ItemSpriteSheet.XXX)、Icons.get(Icons.XXX)、
 *     某个怪物贴图（如 new Image(new RatSprite())）、TalentIcon 等
 *
 * 页面序号与 ChangesScene 的 case 一一对应：EGOPD 页 = ChangesScene.EGOPD_TAB（0）。
 * 注意这一页由本作作者用中文撰写，因此 ChangesScene 里对 EGOPD 页不显示
 * 上游那句「改动详情仅提供英文版本」的提示。
 *
 * <b>行文风格（2026-09-24 起）：极简。</b>每条只写<b>一句话</b>，说清「改了什么」即可，
 * 不要背景铺垫、不要多段论证、不要「原本如何→现在如何」的展开，也不必逐条列举数值边界。
 * 详尽的原理与推导属于 {@code docs/} 下的档案，不写进改动栏。
 */
public class EGOPD_Changes {

	public static void addAllChanges( ArrayList<ChangeInfo> changeInfos ){

		//[EGOPD] v0.3.6
		ChangeInfo v036 = new ChangeInfo("EGOPD v0.3.6", true, "");
		v036.hardlight(0xB3001E);
		changeInfos.add(v036);

		v036.addButton( new ChangeButton( Icons.get(Icons.TRIAL_COLOR), "考验 NETZACH（胜利）",
				"_-_ 敌方的闪避随所在区域提升，并且离英雄越远的敌人越透明。" ) );

		v036.addButton( new ChangeButton( Icons.get(Icons.TRIAL_COLOR), "考验 HOD（荣耀）",
				"_-_ 敌方受到的伤害随所在区域提升，存活过久的敌人会蜕变为精英。" ) );

		v036.addButton( new ChangeButton( Icons.get(Icons.TRIAL_COLOR), "考验 YESOD（根基）",
				"_-_ 装备无法自然鉴定，地面上的东西一律显示为问号，商店额外出售一张鉴定卷轴。" ) );

		v036.addButton( new ChangeButton( new ItemSprite(ItemSpriteSheet.CHEST), "考验 YESOD：容器也变问号",
				"_-_ 各类宝箱与骷髅堆的贴图同样显示为问号，检视与战利品提示也不再透露是什么。" ) );

		v036.addButton( new ChangeButton( Icons.get(Icons.TARGET), "考验 NETZACH：隐形更彻底",
				"_-_ 隐形敌人的头顶血条与睡眠、警觉等状态标记会一并淡出。" ) );

		v036.addButton( new ChangeButton( Icons.get(Icons.CHALLENGE_COLOR), "趣味挑战",
				"_-_ 挑战界面新增「趣味挑战」分类，拆迁办、依旧果冻人、水仙追迹与调试模式移入其中。" ) );

		v036.addButton( new ChangeButton( Icons.get(Icons.SHPX), "修复：GEBURA 的额外掉落落空",
				"_-_ 修复考验 GEBURA 的濒死无敌使幸运附魔与财富戒指的额外掉落判定落空的问题。" ) );

		v036.addButton( new ChangeButton( Icons.get(Icons.SHPX), "修复：「带着标记倒下」不算击杀",
				"_-_ 拇指的荣耀凯旋与食指的击杀指令改为按「携带标记的怪物死亡」计数。" ) );

		v036.addButton( new ChangeButton( new ItemSprite(ItemSpriteSheet.ESCAPE), "水晶矿洞：虚空地形",
				"_-_ 水晶矿洞层会生成深渊，掉进去不会抵达别的层，而是摔伤后被送回本层某处。" ) );

		v036.addButton( new ChangeButton( Icons.get(Icons.TALENT), "四项二层天赋改写",
				"_-_ 变换无常改为重复判定闪避取最高，狩猎一餐改为恢复奥丁之眼，骨骸编织改为获得 0~角色等级的护甲，纹身铭刻额外获得生命强化。" ) );

		//[EGOPD] v0.3.5
		ChangeInfo v035 = new ChangeInfo("EGOPD v0.3.5", true, "");
		v035.hardlight(0xB3001E);
		changeInfos.add(v035);

		v035.addButton( new ChangeButton( new ItemSprite(ItemSpriteSheet.LLOYD_TALISMAN), "新道具：洛伊德护符",
				"_-_ 投掷命中后使目标 50 回合无法恢复生命值；命中宝箱怪时额外使其暴露，并获得 20 回合麻痹、虚弱与致盲。" ) );

		v035.addButton( new ChangeButton( new ItemSprite(ItemSpriteSheet.HOLY_CARD), "新道具：神圣卡",
				"_-_ 使用花费一回合，获得可挡下下一次伤害的「神圣屏障」，持续到被用掉为止。" ) );

		v035.addButton( new ChangeButton( Icons.get(Icons.CHALLENGE_COLOR), "禁疗：所有回血来源失效",
				"_-_ 全仓回血统一收口后，禁疗期间药水、食物、吸血、再生与 CHESED 考验的回血一律归零。" ) );

		v035.addButton( new ChangeButton( Icons.get(Icons.SHPX), "修复：炼金釜快速配方闪退",
				"_-_ 修复炼金釜点击箭头套用快速配方时的闪退。" ) );

		v035.addButton( new ChangeButton( new ItemSprite(ItemSpriteSheet.GOLDEN_RESIN), "炼金指南：松脂系列与洛伊德护符",
				"_-_ 把焦炭松脂、黄金松脂系列与洛伊德护符补进炼金指南页。" ) );

		v035.addButton( new ChangeButton( new ItemSprite(ItemSpriteSheet.ARTIFACT_BLOOD_FEAST_CHALICE1), "神器强化：脑啡肽下调",
				"_-_ 原版神器强化形态所需脑啡肽由 60 下调为 30。" ) );

		v035.addButton( new ChangeButton( new ItemSprite(ItemSpriteSheet.LLOYD_TALISMAN), "白烟粒子缩小",
				"_-_ 洛伊德护符与禁疗的白色烟雾粒子缩小至原来的一半。" ) );

		v035.addButton( new ChangeButton( Icons.get(Icons.SHPX), "测试层草地：草叶细节改取第二区",
				"_-_ 27 层测试层的草地草叶细节改取第二区（监狱），与 tiles_lob 的监狱风格草皮对齐。" ) );

		//[EGOPD] v0.3.4
		ChangeInfo v034 = new ChangeInfo("EGOPD v0.3.4", true, "");
		v034.hardlight(0xB3001E);
		changeInfos.add(v034);

		v034.addButton( new ChangeButton( Icons.get(Icons.CHALLENGE_COLOR), "考验 GEBURA 削弱：锁血那一回合「僵直」",
				"_-_ 考验 _GEBURA（严厉）_ 的「锁血」原本有个很反直觉的手感：怪物被打成_濒死_时会进入一段_无敌_时间，但_照常出手_——于是你把它打进濒死的那一刻，反而要立刻白吃它一整套攻击。\n" +
				"\n" +
				"_-_ 现在补上_一回合僵直_：刚被打成濒死无敌的_那一回合它不行动_，愣过一个回合之后才恢复正常。无敌的计时照常走，所以它在这段无敌期里_总共少打一下_，这就是本次削弱的全部内容。\n" +
				"\n" +
				"_-_ 对_绝大多数怪物_成立（包含_矮人国王_、_DM-300_ 这类会转阶段的 Boss）。唯一例外是最终 Boss _亚戈·德泽瓦_：它自己重写了行动流程、绕开了这次改动的落点（原版它连_瘫痪_也不吃），已确认为_既定边界_、不为它开特判。\n" +
				"\n" +
				"_-_ 全程不消耗随机数；_不开本考验时行为与原版分毫不变_。" ) );

		//[EGOPD] v0.3.3
		ChangeInfo v033 = new ChangeInfo("EGOPD v0.3.3", true, "");
		v033.hardlight(0xB3001E);
		changeInfos.add(v033);

		v033.addButton( new ChangeButton( Icons.get(Icons.CHALLENGE_COLOR), "修复：矮人国王二阶段卡死",
				"_-_ 开启考验 _CHESED（慈悲）_后，第四个 Boss _矮人国王_的二阶段会_卡死、不再出怪_。这不是随机或时序问题，而是_纯算术_：二阶段的「王座屏障」用了一套写死的绝对刻度——屏障满值 = 生命上限、每击杀一名仆从削减 _1/12_、波次阈值 _200 / 100_——三者靠一个_整除_恒等式咬合。\n" +
				"\n" +
				"_-_ _CHESED_ 把敌方生命上限抬高 _25%_ 后整除被破坏：十二次击杀只能削掉 _372_ 点，屏障_永远剩 3 点_，第二波所需的 _200_ 点阈值也永远达不到；而二阶段的国王_除被它支配的仆从之外免疫一切伤害_ ⇒ 整场变成无解的死局。\n" +
				"\n" +
				"_-_ 现在屏障的_削减量_与_波次阈值_都改为_按生命上限现算_：削减量取_上取整_（击杀次数一到必定打空），波次阈值取生命上限的 _2/3_ 与 _1/3_。生命上限为原版的 _300 / 450_ 时，新算式与旧数值_逐个相同_ ⇒ 不开考验时手感分毫不变。" ) );

		v033.addButton( new ChangeButton( Icons.get(Icons.SHPX), "全仓复查：其余 Boss 的阶段刻度",
				"_-_ 顺着上面这条 bug 把全仓「有阶段转换」的单位通查了一遍，凡是_把生命上限刻度写成绝对数字_的，一律改为_按生命上限现算_：\n" +
				"\n" +
				"_-_ _亚戈·德泽瓦_：每阶段的推进量原本写死 _300_、最后一段的生命下限写死 _100_、光束数里的伤害刻度写死 _400_（恰好是它 _1000_ 点生命上限的 _3/10_、_1/10_、_2/5_）。生命上限被抬高后，四段的掉血窗口会从 _等分_ 漂成「前三段照旧、末段被拉长 3.5 倍」。\n" +
				"\n" +
				"_-_ _矮人国王_一阶段转二阶段的入场血量原本写死 _50_（强化 _100_），现在按生命上限的同一比例换算。\n" +
				"\n" +
				"_-_ _微笑的尸山_的分段换皮阈值原本写死 _2000 / 1000_（正是它 _3000_ 点生命上限的 _2/3_ 与 _1/3_），现在同样按比例现算。\n" +
				"\n" +
				"_-_ 以上每一处都保证「在_原版生命上限_下与旧数值_逐个相同_」；另有_两处刻意保留_的绝对数字——矮人国王三阶段的一句「奄奄一息」台词、以及岩石法师那固定 _25_ 点的岩石护甲，它们都_不参与阶段推进_，改了只会破坏原版行为。" ) );

		v033.addButton( new ChangeButton( new ItemSprite(ItemSpriteSheet.NARCISSUS_CROSS_SWORD), "水仙追迹：圣剑改为放背包",
				"_-_ 挑战 _水仙追迹_ 开局携带的_水仙十字圣剑_改为_放进背包_，不再替换你的初始武器装备；_财富戒指_ 仍在背包里。\n" +
				"\n" +
				"_-_ 剑在背包里就是「未装备」，所以开局不再占用快捷栏，也不再挤走该职业自己的初始快捷项。把它装上主手后，形态照旧由英雄等级决定：_1 级_取_芒性_、_30 级_取_荒性_。" ) );

		//[EGOPD] v0.3.2
		ChangeInfo v032 = new ChangeInfo("EGOPD v0.3.2", true, "");
		v032.hardlight(0xB3001E);
		changeInfos.add(v032);

		v032.addButton( new ChangeButton( Icons.get(Icons.TARGET), "考验 CHESED（慈悲）",
				"_-_ 新增考验 _CHESED（慈悲）_：所有_敌方单位_的生命上限 _+25%_，并且每 _5_ 回合恢复一次生命，每次恢复_生命上限的 10%_。\n" +
				"\n" +
				"_-_ 回血_只在血量不满时才开始计时_：满血的单位不累积计时，一旦被打断就要重新数满 5 个回合；单次回血不会超过缺失的血量。\n" +
				"\n" +
				"_-_ 只作用于_敌方_单位（与挑战「升华」同判据）；你的盟友与中立 NPC 不受影响。回血量与间隔都写在 Trials 的常量里，便于随时调整。" ) );

		v032.addButton( new ChangeButton( Icons.get(Icons.CHALLENGE_COLOR), "考验 GEBURA（严厉）",
				"_-_ 新增考验 _GEBURA（严厉）_：所有_敌方单位_即将死亡时不会立刻倒下，而是进入一段_无敌_时间；_无敌的回合数等于它给予的经验值_（给予 5 点经验的怪物会撑 5 个回合），计时结束的那一刻它才真正死亡。\n" +
				"\n" +
				"_-_ 无敌期间它_免疫一切伤害_、血量停留在 _0 点_（血条已空但仍未倒下），并且仍会照常行动。最终倒下时仍算作_原先那一击_的击杀，经验、掉落与统计与正常击杀一致。\n" +
				"\n" +
				"_-_ _豺狼暴徒_（以及同族的_装甲暴徒_）在_狂暴护盾耗尽_、战续结束之后，同样会获得这段无敌；而_矮人尸群_的「倒地待复活」完全保持原版，不触发本考验。" ) );

		v032.addButton( new ChangeButton( new ItemSprite(ItemSpriteSheet.NARCISSUS_CROSS_SWORD), "新挑战：水仙追迹",
				"_-_ 新增挑战 _水仙追迹_：_得分倍率恒为 0_（本局不计分），开局携带_水仙十字圣剑_（已装备在主手）与_财富戒指_（放在背包里）。\n" +
				"\n" +
				"_-_ 圣剑的形态仍由英雄等级决定、无法手动切换：英雄 _1 级_时取_芒性_、_30 级_时取_荒性_；两种特殊形态都会彻底阻断经验值，所以这多半是一场只能靠装备前进的旅途。\n" +
				"\n" +
				"_-_ 顺带把挑战「随机抽取」的候选池改为按挑战表生成：原先写死成「最低的若干位」，加入新挑战后会把「调试模式」也误抽进来。" ) );

		//[EGOPD] v0.3.1
		ChangeInfo v031 = new ChangeInfo("EGOPD v0.3.1", true, "");
		v031.hardlight(0xB3001E);
		changeInfos.add(v031);

		v031.addButton( new ChangeButton( Icons.get(Icons.SHPX), "Bug 修复",
				"_-_ 修复了若干已发现的 Bug。" ) );

		v031.addButton( new ChangeButton( new ItemSprite(ItemSpriteSheet.WRIST_SLIT), "中指长兄：割腕穿透无敌",
				"_-_ 中指长兄的盔甲技能在_无敌_期间，_割腕_与_蓄血圣杯_可以无限升级，等于白送成长。\n" +
				"\n" +
				"_-_ 现在，_割腕_、_蓄血圣杯_这类_以自伤换取成长_的行为会_绕过无敌的免死判定_，自伤与成长照常结算。" ) );

		v031.addButton( new ChangeButton( new ItemSprite(ItemSpriteSheet.ARTIFACT_BLOOD_FEAST_CHALICE1), "EGO 特化神器",
				"_-_ 新增 5 件_EGO 特化神器_：原版神器的强化形态，在原版效果之上各附加一条新效果——" +
				"_血宴圣杯_、_一生炖菜_、_他人之锁_、_9章2节_、_迫近之日_。\n" +
				"\n" +
				"_-_ 在_炼金台_以_原版神器 + 30 脑啡肽_（12 点炼金能量）合成，保留原神器的等级、充能与状态。" ) );

		//[EGOPD] v0.3.0：本栏目自 0.3.0 起启用，改动条目将在之后的版本里陆续写入这里。
		ChangeInfo changes = new ChangeInfo("EGOPD v0.3.0", true, "此栏目用于记录 EGOPD 自身版本的改动历程，自 v0.3.0 起启用。\n\n具体的改动条目会在之后的版本中陆续写入这里。");
		changes.hardlight(0xB3001E);
		changeInfos.add(changes);

		//[EGOPD] 待写入：往后的改动按下面这个格式追加即可
		//changes.addButton( new ChangeButton( new ItemSprite(ItemSpriteSheet.XXX), "标题", "正文" ) );
	}

}
