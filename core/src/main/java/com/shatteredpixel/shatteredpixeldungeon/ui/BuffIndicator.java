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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoBuff;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.GameMath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

public class BuffIndicator extends Component {
	
	//transparent icon
	public static final int NONE    = 127;

	//FIXME this is becoming a mess, should do a big cleaning pass on all of these
	//and think about tinting options
	public static final int MIND_VISION = 0;
	public static final int LEVITATION  = 1;
	public static final int FIRE        = 2;
	public static final int POISON      = 3;
	public static final int PARALYSIS   = 4;
	public static final int HUNGER      = 5;
	public static final int STARVATION  = 6;
	public static final int TIME        = 7;
	public static final int OOZE        = 8;
	public static final int AMOK        = 9;
	public static final int TERROR      = 10;
	public static final int ROOTS       = 11;
	public static final int INVISIBLE   = 12;
	public static final int SHADOWS     = 13;
	public static final int WEAKNESS    = 14;
	public static final int FROST       = 15;
	public static final int BLINDNESS   = 16;
	public static final int COMBO       = 17;
	public static final int FURY        = 18;
	public static final int HERB_HEALING= 19;
	public static final int ARMOR       = 20;
	public static final int HEART       = 21;
	public static final int LIGHT       = 22;
	public static final int CRIPPLE     = 23;
	public static final int BARKSKIN    = 24;
	public static final int IMMUNITY    = 25;
	public static final int BLEEDING    = 26;
	public static final int MARK        = 27;
	public static final int DEFERRED    = 28;
	public static final int DROWSY      = 29;
	public static final int MAGIC_SLEEP = 30;
	public static final int THORNS      = 31;
	public static final int FORESIGHT   = 32;
	public static final int VERTIGO     = 33;
	public static final int RECHARGING  = 34;
	public static final int LOCKED_FLOOR= 35;
	public static final int CORRUPT     = 36;
	public static final int BLESS       = 37;
	public static final int RAGE        = 38;
	public static final int SACRIFICE   = 39;
	public static final int BERSERK     = 40;
	public static final int HASTE       = 41;
	public static final int PREPARATION = 42;
	public static final int WELL_FED    = 43;
	public static final int HEALING     = 44;
	public static final int WEAPON      = 45;
	public static final int VULNERABLE  = 46;
	public static final int HEX         = 47;
	public static final int DEGRADE     = 48;
	public static final int PINCUSHION  = 49;
	public static final int UPGRADE     = 50;
	public static final int MOMENTUM    = 51;
	public static final int ANKH        = 52;
	public static final int NOINV       = 53;
	public static final int TARGETED    = 54;
	public static final int IMBUE       = 55;
	public static final int ENDURE      = 56;
	public static final int INVERT_MARK = 57;
	public static final int NATURE_POWER= 58;
	public static final int AMULET      = 59;
	public static final int DUEL_CLEAVE = 60;
	public static final int DUEL_GUARD  = 61;
	public static final int DUEL_SPIN   = 62;
	public static final int DUEL_EVASIVE= 63;
	public static final int DUEL_DANCE  = 64;
	public static final int DUEL_BRAWL  = 65;
	public static final int DUEL_XBOW   = 66;
	public static final int CHALLENGE   = 67;
	public static final int MONK_ENERGY = 68;
	public static final int DUEL_COMBO  = 69;
	public static final int DAZE        = 70;
	public static final int DISGUISE    = 71;
	public static final int WAND        = 72;
	public static final int HOLY_WEAPON = 73;
	public static final int HOLY_ARMOR  = 74;
	public static final int SPELL_FOOD  = 75;
	public static final int LIGHT_SHIELD= 76;
	public static final int HOLY_SIGHT  = 77;
	public static final int GLYPH_RECALL= 78;
	public static final int ASCEND      = 79;
	public static final int PROT_AURA   = 80;
	public static final int ILLUMINATED = 81;
	public static final int TRINITY_FORM= 82;
	public static final int MANY_POWER  = 83;
	public static final int SEAL_SHIELD = 84;
	public static final int THROWN_WEP  = 85;

	//custom icon for the 「业」(Karma) buff, drawn right of the last vanilla buff icon
	public static final int KARMA       = 86;
	//custom icon for the instruction target mark, drawn right of the Karma icon
	public static final int INSTRUCTION_TARGET = 87;
	//custom icon for the pending instruction buff, drawn right of the instruction target icon
	public static final int PENDING_INSTRUCTION = 88;
	//custom icon for the release (剑刃解放) buff, drawn right of the pending instruction icon
	public static final int RELEASE = 89;
	//custom icon for the divine blessing (指令加护) buff, drawn right of the release icon
	public static final int DIVINE_BLESSING = 90;
	//custom icon for the scorching wound (烧灼的伤口) buff, drawn right of the divine blessing icon
	public static final int SCORCHING_WOUND = 91;
	//custom icon for the heart-fate (心-命运) buff, drawn right of the scorching wound icon
	public static final int HEART_FATE = 92;
	//custom icon for the inspiration (灵感) buff, drawn right of the Blessing icon (HEART_FATE+2)
	public static final int INSPIRATION   = 95;
	//custom icon for the bone-weaving (骨骸编织) buff, drawn right of the inspiration icon
	public static final int BONE_WEAVING  = 96;
	//custom icon for the body-theater (人体观剧) buff, drawn right of the bone-weaving icon
	public static final int BODY_THEATER  = 97;
	//custom icon for the deep-trauma (深度创伤) buff, drawn at frame 98 (用户已绘制)
	//注：97(BODY_THEATER) 之后 98 被跳过，说明编号超前 1 格，此段整体 -1 修正
	public static final int DEEP_TRAUMA   = 98;
	//custom icons for the Odin's Eye artifact (奥丁之眼) buffs, drawn at frames 99-100 (用户已绘制)
	public static final int PRECOGNITION  = 99; //预知眼
	public static final int ODINS_EYE_OVERHEAT = 100; //预知眼-过热
	//custom icon for the accelerating-future (加速的未来) buff, drawn at frame 101 (用户已绘制)
	public static final int ACCELERATING_FUTURE = 101; //加速的未来
	//custom icons for 拇指 前二老板 T2 系统 buffs, frames 102-104 (用户已绘制)
	public static final int ACCEL_AMMO = 102;      //加速弹（备弹后的减延迟状态）
	public static final int HUNTING_TARGET = 103;  //狩猎目标（标记在敌人身上）
	public static final int PREMONITION = 104;     //预感（未来视角 +2，周围有隐藏门/陷阱）
	public static final int TREMOR_SCORCH = 105;    //震颤-灼热（战争英雄，帧 105 在预感下一位）
	public static final int SWORD_RAIL = 106;       //叠加剑轨（家族之耻，帧 106 在震颤-灼热下一位）
	//瞄准心脏标记（拇指 前二老板盔甲技能，帧 107；心-不光彩 buff 复用 92 心-命运图标）
	//注：2026-09-10 修正——「瞄准心脏 + 两张松脂涂层 + 连斩」4 个图标此前整体后错 1 格
	//（原注释写"107 决定跳过"，但贴图实际是连续绘制的），此段统一 -1 与贴图对齐。
	public static final int AIM_HEART_MARK = 107;
	//custom icons for resin coatings (松脂涂层), frames 108-109 (用户已绘制)
	public static final int CHARCOAL_RESIN = 108; //焦炭松脂（烈焰涂层）
	public static final int GOLDEN_RESIN   = 109; //黄金松脂（电击涂层）
	//custom icon for 巴勒莫对剑「连斩」叠层 buff, frame 110 (用户已绘制)
	public static final int PALERMO_SWORD  = 110; //连斩（巴勒莫对剑，每次攻击叠1层、每层-10%攻击延迟）
	//custom icon for 蜚蠊附魔「攻击害虫」积累 buff, frame 111（待用户绘制）
	public static final int ATTACK_VERMIN  = 111; //攻击害虫（蜚蠊附魔，随时间积累、下次攻击释放）
	//custom icon for 「泪剑」叠层 buff, frame 112（紧接攻击害虫；111=攻击害虫已占，故取 112）
	public static final int TEAR_SWORD    = 112; //泪剑（可叠层，每层在角色周围环绕一柄泪剑）
	//custom icon for 「仇怨」buff of the Revenge Ledger artifact, frame 113（紧接泪剑）
	public static final int RANCOR        = 113; //仇怨（复仇账簿激活时：力量+12，每回合消耗 2% 充能）
	//custom icon for 「夸耀」buff of 中指长兄 T1「仔细看好！」, frame 114（待用户绘制）
	public static final int BRAG          = 114; //夸耀（每层 +5% 精准，最高 8 层，未命中清空）
	//custom icon for 「怨恨标记」of 中指长兄 转职「忠义巡礼者」, frame 115（用户已绘制：夸耀的下一位）
	public static final int GRUDGE_MARK   = 115; //怨恨标记（敌人攻击命中中指长兄/友方单位时叠 1 层，供复仇技艺消耗）
	//custom icon for 「融化」of 中指长兄 转职「背叛家人者」天赋「融化而死」, frame 116（用户已绘制：怨恨标记的下一位）
	public static final int MELTING       = 116; //融化（莱瓦汀系列命中后施加、持续 8 回合；受到的火焰伤害提高）
	//custom icon for 「神圣屏障」of 神圣卡, frame 117（用户已绘制：融化的下一位）
	public static final int HOLY_BARRIER  = 117; //神圣屏障（神圣卡使用后的一次性无敌；挡下一次伤害后消失）

	//--------------------------------------------------------------------------
	// 【惯例】还没有自绘图标的 buff：一律「借原版帧」，**不要**为它预留帧号常量
	//--------------------------------------------------------------------------
	//本作里凡是尚未画专属图标的 buff，都不要在这里写 `public static final int XXX = 帧号;`——
	//直接让它的 icon() 返回一个够用的原版帧即可（等同于原版中多个 buff 共用一个图标的做法）。
	//原因：预留的低位帧号「其实没有任何代码在用」（icon() 借的是别的帧），
	//却会把后续自绘图标想按顺序往下排的位子占掉，每次都逼着整段顺延
	//（2026-09-16 已为此返工两次：怨恨标记顶掉临时力量、融化顶掉犯规）。
	//所以下面这三个「借帧」的 buff 只在各自类里注释说明借了哪一帧；
	//等真的画出来了，再按**当时的空帧顺序**新增常量并把 icon() 改过来：
	//  临时力量 TempStrength → 借原版帧 UPGRADE(50)
	//  犯规     CheatGuard   → 借原版帧 ARMOR(20)（护盾通用图标）
	//  残像纠缠 Afterimage   → 借原版帧 UPGRADE(50)
	//  背叛之力 FamilyBetrayal → 借原版帧 UPGRADE(50)（靠 hardlight 暗红与临时力量区分）

	public static final int SIZE_SMALL  = 7;
	public static final int SIZE_LARGE  = 16;
	
	private static BuffIndicator heroInstance;
	private static BuffIndicator bossInstance;
	
	private LinkedHashMap<Buff, BuffButton> buffButtons = new LinkedHashMap<>();
	private boolean needsRefresh;
	private Char ch;

	private boolean large = false;

	//in some cases we want to limit some rows but not all by just reducing width
	public float[] rowWidthLimits = new float[9]; //0 = no limit
	//sometimes we also need to slightly lower a row, to avoid having to cut off width
	public float[] rowHeightAdjusts = new float[9]; //0 = default adjust of 1
	
	public BuffIndicator( Char ch, boolean large ) {
		super();
		
		this.ch = ch;
		this.large = large;
		if (ch == Dungeon.hero) {
			heroInstance = this;
		}
	}
	
	@Override
	public void destroy() {
		super.destroy();
		
		if (this == heroInstance) {
			heroInstance = null;
		}
	}

	@Override
	public synchronized void update() {
		super.update();
		if (needsRefresh){
			needsRefresh = false;
			layout();
		}
	}

	private boolean buffsHidden = false;
	public int maxBuffs = 14; //by default

	@Override
	protected void layout() {

		ArrayList<Buff> newBuffs = new ArrayList<>();
		for (Buff buff : ch.buffs()) {
			if (buff.icon() != NONE) {
				newBuffs.add(buff);
			}
		}

		int size = large ? SIZE_LARGE : SIZE_SMALL;

		//remove any icons no longer present
		for (Buff buff : buffButtons.keySet().toArray(new Buff[0])){
			if (!newBuffs.contains(buff)){
				Image icon = buffButtons.get( buff ).icon;
				icon.originToCenter();
				icon.alpha(0.6f);
				add( icon );
				add( new AlphaTweener( icon, 0, 0.6f ) {
					@Override
					protected void updateValues( float progress ) {
						super.updateValues( progress );
						image.scale.set( 1 + 5 * progress );
					}
					
					@Override
					protected void onComplete() {
						image.killAndErase();
					}
				} );
				
				buffButtons.get( buff ).destroy();
				remove(buffButtons.get( buff ));
				buffButtons.remove( buff );
			}
		}
		
		//add new icons
		for (Buff buff : newBuffs) {
			if (!buffButtons.containsKey(buff)) {
				BuffButton icon = new BuffButton(buff, large);
				add(icon);
				buffButtons.put( buff, icon );
			}
		}

		//layout
		int row = 1;
		float rowTop = 0;
		int pos = 0;
		float lastIconRight = 0;
		int total = 0;
		for (BuffButton icon : buffButtons.values()){
			if (total >= maxBuffs){
				icon.visible = false;
				continue;
			}
			icon.visible = true;

			//offset is needed to handle adjusting oversized click boxes on multiple rows
			icon.topOffset = (row > 1 && !large) ? -1 : 0;
			icon.updateIcon();
			//button areas are slightly oversized, especially on small buttons
			icon.setRect(x + pos * (size + 1), y + rowTop-icon.topOffset, size + 1, size + (large ? 0 : 5));
			PixelScene.align(icon);
			pos++;

			lastIconRight = icon.right()-1;

			//if we're out of overall width but have more height, or this row has hits its limit
			if ((rowTop+2*size+2 <= height && (pos * (size + 1) + size > width))
					|| (rowWidthLimits[row] != 0 && pos * (size + 1) + size > rowWidthLimits[row])){
				row++;
				rowTop += size+1 + rowHeightAdjusts[row];
				pos = 0;
			}
			total++;
		}

		buffsHidden = false;
		//squish buff icons together if there isn't enough room
		float excessWidth = lastIconRight - right();

		if (excessWidth > 0) {
			//if multiple rows, only compress last row
			ArrayList<BuffButton> buttons = new ArrayList<>();
			float lastRowY = PixelScene.align(y + rowTop);
			int i = 1;
			for (BuffButton button : buffButtons.values()){
				if (i > maxBuffs){
					button.visible = false;
					buffsHidden = true;
					continue;
				}
				if (button.top()+button.topOffset == lastRowY){
					buttons.add(button);
				}
				i++;
			}

			float leftAdjust = excessWidth/(buttons.size()-1);
			//can't squish by more than 50%
			if (leftAdjust >= size*0.48f) leftAdjust = size*0.5f;
			float cumulativeAdjust = leftAdjust * (buttons.size()-1);

			Collections.reverse(buttons);
			for (BuffButton icon : buttons) {
				icon.setPos(icon.left() - cumulativeAdjust, icon.top());
				icon.visible = icon.right() <= right()+1;
				if (!icon.visible) buffsHidden = true;
				PixelScene.align(icon);
				bringToFront(icon);
				icon.givePointerPriority();
				cumulativeAdjust -= leftAdjust;
			}
		}

		if (this == heroInstance && buffButtons.size() >= 10){
			Badges.validateManyBuffs();
		}
	}

	public boolean allBuffsVisible(){
		return !buffsHidden;
	}

	private static class BuffButton extends IconButton {

		private Buff buff;

		private boolean large;
		private int topOffset = 0;

		public Image grey; //only for small
		public BitmapText text; //only for large

		public BuffButton( Buff buff, boolean large ){
			super( new BuffIcon(buff, large));
			this.buff = buff;
			this.large = large;

			bringToFront(grey);
			bringToFront(text);
		}

		@Override
		protected void createChildren() {
			super.createChildren();
			grey = new Image( TextureCache.createSolid(0xCC666666));
			add( grey );

			text = new BitmapText(PixelScene.pixelFont);
			add( text );
		}

		public void updateIcon(){
			((BuffIcon)icon).refresh(buff);
			//round up to the nearest pixel if <50% faded, otherwise round down
			if (!large || buff.iconTextDisplay().isEmpty()) {
				text.visible = false;
				grey.visible = true;
				float fadeHeight = GameMath.gate(0, buff.iconFadePercent(), 1) * icon.height();
				float zoom = (camera() != null) ? camera().zoom : 1;
				if (fadeHeight < icon.height() / 2f) {
					grey.scale.set(icon.width(), (float) Math.ceil(zoom * fadeHeight) / zoom);
				} else {
					grey.scale.set(icon.width(), (float) Math.floor(zoom * fadeHeight) / zoom);
				}
			} else if (!buff.iconTextDisplay().isEmpty()) {
				text.visible = true;
				grey.visible = false;
				if (buff.type == Buff.buffType.POSITIVE)        text.hardlight(CharSprite.POSITIVE);
				else if (buff.type == Buff.buffType.NEGATIVE)   text.hardlight(CharSprite.NEGATIVE);
				text.alpha(0.7f);

				text.text(buff.iconTextDisplay());
				text.measure();
			}
		}

		@Override
		protected void layout() {
			super.layout();
			grey.x = icon.x = this.x + (large ? 0 : 1);
			grey.y = icon.y = this.y + (large ? 0 : 2) + topOffset;

			if (text.width > width()){
				text.scale.set(PixelScene.align(0.5f));
			} else {
				text.scale.set(1f);
			}
			text.x = this.x + width() - text.width() - 1;
			text.y = this.y + width() - text.baseLine() - 2;
		}

		@Override
		protected void onClick() {
			if (buff.icon() != NONE) GameScene.show(new WndInfoBuff(buff));
		}

		@Override
		protected void onPointerDown() {
			//don't affect buff color
			Sample.INSTANCE.play( Assets.Sounds.CLICK );
		}

		@Override
		protected void onPointerUp() {
			//don't affect buff color
		}

		@Override
		protected String hoverText() {
			return Messages.titleCase(buff.name());
		}
	}
	
	public static void refreshHero() {
		if (heroInstance != null) {
			heroInstance.needsRefresh = true;
		}
	}

	public static void refreshBoss(){
		if (bossInstance != null) {
			bossInstance.needsRefresh = true;
		}
	}

	public static void setBossInstance(BuffIndicator boss){
		bossInstance = boss;
	}
}
