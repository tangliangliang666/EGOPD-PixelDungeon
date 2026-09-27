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

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.Rankings;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.OptionSlider;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndChallenges;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndFunChallenges;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTrials;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndHeroInfo;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndKeyBindings;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndMessage;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndVictoryCongrats;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.watabou.gltextures.TextureCache;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.SkinnedBlock;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.GameMath;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;
import com.watabou.utils.RectF;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class HeroSelectScene extends PixelScene {

	private Image background;
	private Image fadeLeft, fadeRight;
	private IconButton btnFade; //only on landscape

	//fading UI elements
	private RenderedTextBlock title;
	private ArrayList<StyledButton> heroBtns = new ArrayList<>();
	private RenderedTextBlock heroName; //only on landscape
	private RenderedTextBlock heroDesc; //only on landscape
	private StyledButton startBtn;
	private IconButton infoButton;
	private IconButton btnOptions;
	private GameOptions optionsPane;
	private IconButton btnExit;
	private CheckBox skinCheck; //拇指 前二老板：皮肤「泪锋之剑」开关（仅选中该职业时显示）

	private RectF insets;

	private static boolean heroWasRandomized = true;
	private static boolean chalWasRandomized = false;

	@Override
	public void create() {
		super.create();

		Dungeon.hero = null;

		Badges.loadGlobal();
		Journal.loadGlobal();

		insets = Game.platform.getSafeInsets(PlatformSupport.INSET_BLK).scale(1f/defaultZoom);

		float w = (Camera.main.width - insets.left - insets.right);
		float h = (Camera.main.height - insets.top - insets.bottom);

		background = new Image(TextureCache.createSolid(0xFF2d2f31), 0, 0, 800, 450){
			@Override
			public void update() {
				if (GamesInProgress.selectedClass != null) {
					if (rm > 1f) {
						rm -= Game.elapsed;
						gm = bm = rm;
					} else {
						rm = gm = bm = 1;
					}
				}
			}
		};
		background.scale.set(Camera.main.height/background.height);

		background.x = (Camera.main.width - background.width())/2f;
		background.y = (Camera.main.height - background.height())/2f;
		PixelScene.align(background);
		add(background);

		fadeLeft = new Image(TextureCache.createGradient(0xFF000000, 0xFF000000, 0x00000000));
		fadeLeft.x = background.x-2;
		fadeLeft.scale.set(3, background.height());
		add(fadeLeft);

		fadeRight = new Image(fadeLeft);
		fadeRight.x = background.x + background.width() + 2;
		fadeRight.y = background.y + background.height();
		fadeRight.angle = 180;
		add(fadeRight);

		title = PixelScene.renderTextBlock(Messages.get(this, "title"), 12);
		title.hardlight(Window.TITLE_COLOR);
		PixelScene.align(title);
		add(title);

		startBtn = new StyledButton(Chrome.Type.GREY_BUTTON_TR, ""){
			@Override
			protected void onClick() {
				super.onClick();

				if (GamesInProgress.selectedClass == null) return;

				Dungeon.hero = null;
				Dungeon.daily = Dungeon.dailyReplay = false;
				Dungeon.initSeed();
				ActionIndicator.clearAction();
				InterlevelScene.mode = InterlevelScene.Mode.DESCEND;

				Game.switchScene( InterlevelScene.class );
			}
		};
		startBtn.icon(Icons.get(Icons.ENTER));
		startBtn.setSize(80, 21);
		startBtn.textColor(Window.TITLE_COLOR);
		add(startBtn);
		startBtn.visible = startBtn.active = false;

		infoButton = new IconButton(Icons.get(Icons.INFO)){
			@Override
			protected void onClick() {
				super.onClick();
				HeroClass cls = GamesInProgress.selectedClass;
				if (cls != null) {
					Window info = new WndHeroInfo(GamesInProgress.selectedClass);
					if (landscape()) {
						info.offset((int)(w / 6), 0);
					}
					ShatteredPixelDungeon.scene().addToFront(info);
				}
			}

			@Override
			protected String hoverText() {
				return Messages.titleCase(Messages.get(WndKeyBindings.class, "hero_info"));
			}
		};
		infoButton.visible = infoButton.active = false;
		infoButton.setSize(20, 21);
		add(infoButton);

		for (HeroClass cl : HeroClass.values()){
			HeroBtn button = new HeroBtn(cl);
			add(button);
			heroBtns.add(button);
		}

		//拇指 前二老板：皮肤「泪锋之剑」开关（开启后游戏内恒定显示贴图第 8 行的皮肤）
		skinCheck = new CheckBox(Messages.get(this, "valencina_skin")){
			@Override
			public void checked(boolean value) {
				super.checked(value);
				SPDSettings.valencinaSkin(value);
				refreshHeroIcons();
			}
		};
		skinCheck.setSize(70, 16);
		skinCheck.checked(SPDSettings.valencinaSkin());
		skinCheck.visible = skinCheck.active = false;
		add(skinCheck);

		optionsPane = new GameOptions();
		optionsPane.visible = optionsPane.active = false;
		optionsPane.layout();
		add(optionsPane);

		btnOptions = new IconButton(Icons.get(Icons.PREFS)){
			@Override
			protected void onClick() {
				super.onClick();
				optionsPane.visible = !optionsPane.visible;
				optionsPane.active = !optionsPane.active;
			}

			@Override
			protected void onPointerDown() {
				super.onPointerDown();
			}

			@Override
			protected void onPointerUp() {
				updateOptionsColor();
			}

			@Override
			protected String hoverText() {
				return Messages.get(HeroSelectScene.class, "options");
			}
		};
		updateOptionsColor();
		btnOptions.visible = false;

		if(!SPDSettings.intro()){
			add(btnOptions);
		}

		if (!Badges.isUnlocked(Badges.Badge.VICTORY) && !DeviceCompat.isDebug()){
			Dungeon.challenges = 0;
			SPDSettings.challenges(0);
			SPDSettings.customSeed("");
		}

		if (landscape()){
			float leftArea = Math.max(100, w/3f);
			float uiHeight = Math.min(h-20, 300);
			float uiSpacing = (uiHeight-120)/2f;

			if (uiHeight >= 160) uiSpacing -= 5;
			if (uiHeight >= 180) uiSpacing -= 6;

			background.x += insets.left + leftArea/6f;

			float fadeLeftScale = 47 * (leftArea - background.x)/leftArea;
			fadeLeft.scale = new PointF(3 + Math.max(0, fadeLeftScale), background.height());

			title.setPos(insets.left + (leftArea - title.width())/2f, (h-uiHeight)/2f);
			align(title);

			int btnWidth = HeroBtn.MIN_WIDTH + 15;
			int btnHeight = HeroBtn.HEIGHT;
			if (uiHeight >= 180){
				btnHeight += 6;
			}

			int cols = (int)Math.ceil(heroBtns.size()/2f);
			float curX = insets.left + (leftArea - btnWidth * cols + (cols-1))/2f;
			float curY = title.bottom() + uiSpacing;

			int count = 0;
			for (StyledButton button : heroBtns){
				button.setRect(curX, curY, btnWidth, btnHeight);
				align(button);
				curX += btnWidth+1;
				count++;
				if (count >= (1+heroBtns.size())/2){
					curX -= btnWidth*count + count;
					curY += btnHeight+1;
					if (heroBtns.size()%2 != 0){
						curX += btnWidth/2f;
					}
					count = 0;
				}
			}

			heroName = renderTextBlock(9);
			heroName.setPos(insets.left, heroBtns.get(heroBtns.size()-1).bottom()+5);
			add(heroName);

			if (uiHeight >= 160){
				heroDesc = renderTextBlock(6);
			} else {
				heroDesc = renderTextBlock(5);
			}
			heroDesc.align(RenderedTextBlock.CENTER_ALIGN);
			heroDesc.setPos(insets.left, heroName.bottom()+5);
			add(heroDesc);

			startBtn.text(Messages.titleCase(Messages.get(this, "start")));
			startBtn.setSize(startBtn.reqWidth()+8, 21);
			startBtn.setPos(insets.left + (leftArea - startBtn.width())/2f, title.top() + uiHeight - startBtn.height());
			align(startBtn);

			btnFade = new IconButton(Icons.CHEVRON.get()){
				@Override
				protected void onClick() {
					enable(false);
					parent.add(new Tweener(parent, 0.5f) {
						@Override
						protected void updateValues(float progress) {
							uiAlpha = 1 - progress;
							updateFade();
						}
					});
				}
			};
			btnFade.icon().originToCenter();
			btnFade.icon().angle = 270f;
			btnFade.visible = btnFade.active = false;
			btnFade.setRect(startBtn.left()-20, startBtn.top(), 20, 21);
			align(btnFade);
			add(btnFade);

			btnOptions.setRect(startBtn.right(), startBtn.top(), 20, 21);
			optionsPane.setPos(btnOptions.right(), btnOptions.top() - optionsPane.height() - 2);
			align(optionsPane);
		} else {
			background.visible = false;

			int btnWidth = HeroBtn.MIN_WIDTH;
			int btnCount = heroBtns.size();
			//移动端竖屏：角色按钮分两行排列（角色较多时单行会超出画面）
			int perRow = (btnCount + 1) / 2;
			int secondRowCount = btnCount - perRow;

			float curX = insets.left + (w - btnWidth * perRow) / 2f;
			if (curX > 0) {
				btnWidth += Math.min(curX / (perRow / 2f), 15);
				curX = insets.left + (w - btnWidth * perRow) / 2f;
			}
			float rowH = HeroBtn.HEIGHT + 1;
			//底行 Y 与原单行一致；顶行在底行正上方
			float bottomY = insets.top + h - HeroBtn.HEIGHT + 3;
			float topY = bottomY - rowH;

			int count = 0;
			for (StyledButton button : heroBtns) {
				if (count < perRow) {
					//顶行
					if (count == 0) curX = insets.left + (w - btnWidth * perRow) / 2f;
					button.setRect(curX, topY, btnWidth, HeroBtn.HEIGHT);
				} else {
					//底行（延伸进底部安全区）
					if (count == perRow) curX = insets.left + (w - btnWidth * secondRowCount) / 2f;
					button.setRect(curX, bottomY, btnWidth, HeroBtn.HEIGHT + insets.bottom);
				}
				curX += btnWidth;
				count++;
			}

			//add a darkening bar along bottom
			if (insets.bottom > 0){
				SkinnedBlock bar = new SkinnedBlock(Camera.main.width, insets.bottom, TextureCache.createSolid(0xAA000000));
				bar.y = h + insets.top;
				add(bar);

				PointerArea blocker = new PointerArea(0, Camera.main.width - insets.bottom, Camera.main.width, insets.bottom);
				add(blocker);
			}

			title.setPos(insets.left + (w - title.width()) / 2f, heroBtns.get(0).top() - title.height() - 4);

			btnOptions.setRect(heroBtns.get(0).left() + 16, heroBtns.get(0).top() - 16, 20, 21);
			optionsPane.setPos(heroBtns.get(0).left(), 0);
		}

		btnExit = new ExitButton();
		int ofs = PixelScene.landscape() ? 0 : 4;
		btnExit.setPos( Camera.main.width - btnExit.width() - ofs, ofs );
		add( btnExit );
		btnExit.visible = btnExit.active = !SPDSettings.intro();

		PointerArea fadeResetter = new PointerArea(0, 0, Camera.main.width, Camera.main.height){
			@Override
			public boolean onSignal(PointerEvent event) {
				if (event != null && event.type == PointerEvent.Type.UP){
					if (uiAlpha == 0 && landscape()){
						parent.add(new Tweener(parent, 0.5f) {
							@Override
							protected void updateValues(float progress) {
								uiAlpha = progress;
								updateFade();
							}

							@Override
							protected void onComplete() {
								resetFade();
							}
						});
					} else {
						resetFade();
					}
				}
				return false;
			}
		};
		add(fadeResetter);
		resetFade();

		if (GamesInProgress.selectedClass != null){
			setSelectedHero(GamesInProgress.selectedClass);
		}

		if (Badges.isUnlocked(Badges.Badge.VICTORY) && !SPDSettings.victoryNagged()) {
			SPDSettings.victoryNagged(true);
			add(new WndVictoryCongrats());
		}

		fadeIn();

	}

	private void updateOptionsColor(){
		if (!SPDSettings.customSeed().isEmpty()){
			btnOptions.icon().hardlight(1f, 1.5f, 0.67f);
		} else if (SPDSettings.challenges() != 0){
			btnOptions.icon().hardlight(2f, 1.33f, 0.5f);
		} else {
			btnOptions.icon().resetColor();
		}
	}

	private void setSelectedHero(HeroClass cl){
		GamesInProgress.selectedClass = cl;
		GamesInProgress.randomizedClass = false;

		try {
			//loading these big jpgs fails sometimes, so we have a catch for it
			background.texture(cl.splashArt());
		} catch (Exception e){
			Game.reportException(e);
			background.texture(TextureCache.createSolid(0xFF2d2f31));
			background.frame(0, 0, 800, 450);
		}
		background.visible = true;
		background.hardlight(1.5f,1.5f,1.5f);

		float leftPortion = Math.max(100, (Camera.main.width - insets.left - insets.right)/3f);

		if (landscape()) {

			heroName.text(Messages.titleCase(cl.title()));
			heroName.hardlight(Window.TITLE_COLOR);
			heroName.setPos(insets.left + (leftPortion - heroName.width() - 20)/2f, heroName.top());
			align(heroName);

			heroDesc.text(cl.shortDesc());
			heroDesc.maxWidth(80);
			heroDesc.setPos(insets.left +(leftPortion - heroDesc.width())/2f, heroName.bottom() + 5);
			align(heroDesc);

			while(startBtn.top() < heroDesc.bottom()){
				heroDesc.maxWidth(heroDesc.maxWidth()+10);
				heroDesc.setPos(Math.max(insets.left, (leftPortion - heroDesc.width())/2f), heroName.bottom() + 5);
				align(heroDesc);
			}

			btnFade.visible = btnFade.active = true;

			startBtn.visible = startBtn.active = true;

			infoButton.visible = infoButton.active = true;
			infoButton.setPos(heroName.right(), heroName.top() + (heroName.height() - infoButton.height())/2f);
			align(infoButton);

			btnOptions.visible = btnOptions.active = !SPDSettings.intro();

		} else {
			title.visible = false;

			startBtn.visible = startBtn.active = true;
			startBtn.text(Messages.titleCase(cl.title()));
			startBtn.setSize(startBtn.reqWidth() + 8, 21);

			startBtn.setPos((Camera.main.width - startBtn.width())/2f, (heroBtns.get(0).top() - 2 - startBtn.height()));
			PixelScene.align(startBtn);

			infoButton.visible = infoButton.active = true;
			infoButton.setPos(startBtn.right(), startBtn.top());

			btnOptions.visible = btnOptions.active = !SPDSettings.intro();
			btnOptions.setPos(startBtn.left()-btnOptions.width(), startBtn.top());

			optionsPane.setPos(heroBtns.get(0).left(), startBtn.top() - optionsPane.height() - 2);
			align(optionsPane);
		}

		layoutSkinCheck(cl, leftPortion);

		updateOptionsColor();
	}

	//拇指 前二老板：皮肤开关的显隐与定位（仅该职业显示；横屏放职业描述与开始按钮之间，竖屏放开始按钮上方）
	private void layoutSkinCheck(HeroClass cl, float leftPortion){
		boolean show = (cl == HeroClass.VALENCINA);
		skinCheck.visible = skinCheck.active = show;
		if (!show) return;

		float cw = skinCheck.width();
		float ch = skinCheck.height();

		if (landscape()){
			float avail = startBtn.top() - heroDesc.bottom();
			float cy = heroDesc.bottom() + Math.max(2, (avail - ch) / 2f);
			cy = Math.min(cy, startBtn.top() - ch); //描述过长时不压住开始按钮
			skinCheck.setRect(insets.left + (leftPortion - cw) / 2f, cy, cw, ch);
		} else {
			skinCheck.setRect((Camera.main.width - cw) / 2f, startBtn.top() - ch - 4, cw, ch);
		}
		align(skinCheck);
	}

	//皮肤开关变化后刷新各职业按钮的图标（仅前二老板会实际变化）
	private void refreshHeroIcons(){
		for (StyledButton b : heroBtns){
			if (b instanceof HeroBtn) ((HeroBtn) b).updateIcon();
		}
	}

	private float uiAlpha;

	@Override
	public void update() {
		super.update();
		if (SPDSettings.intro() && Rankings.INSTANCE.totalNumber > 0){
			SPDSettings.intro(false);
		}
		btnExit.visible = btnExit.active = !SPDSettings.intro();
		//do not fade when a window is open
		for (Object v : members){
			if (v instanceof Window) resetFade();
		}
		if (!PixelScene.landscape() && GamesInProgress.selectedClass != null) {
			if (uiAlpha > 0f){
				uiAlpha -= Game.elapsed/4f;
			}
			updateFade();
		}
	}

	private void updateFade(){
		float alpha = GameMath.gate(0f, uiAlpha, 1f);
		title.alpha(alpha);
		for (StyledButton b : heroBtns){
			b.enable(alpha != 0);
			b.alpha(alpha);
		}
		if (heroName != null){
			heroName.alpha(alpha);
			heroDesc.alpha(alpha);
			btnFade.enable(alpha != 0);
			btnFade.icon().alpha(alpha);
		}
		startBtn.enable(alpha != 0);
		startBtn.alpha(alpha);
		btnExit.enable(btnExit.visible && alpha != 0);
		btnExit.icon().alpha(alpha);
		optionsPane.active = optionsPane.visible && alpha != 0;
		optionsPane.alpha(alpha);
		btnOptions.enable(alpha != 0);
		btnOptions.icon().alpha(alpha);
		infoButton.enable(alpha != 0);
		infoButton.icon().alpha(alpha);

		if (skinCheck != null){
			skinCheck.active = skinCheck.visible && alpha != 0;
			skinCheck.alpha(alpha);
		}

		if (landscape()){

			int w = (int)(Camera.main.width - insets.left - insets.right);

			background.x = insets.left + (w - background.width())/2f;

			float leftPortion = Math.max(100, w/3f);

			background.x += (leftPortion/2f)*alpha;

			float fadeLeftScale = 47 * (leftPortion - (background.x - insets.left))/leftPortion;
			fadeLeft.scale.x = 3 + Math.max(fadeLeftScale, 0)*alpha;
			fadeLeft.x = background.x-4;
			fadeRight.x = background.x + background.width() + 4;
		}

		fadeLeft.x = background.x-5;
		fadeRight.x = background.x + background.width() + 5;

		fadeLeft.visible = background.x > 0 || (alpha > 0 && landscape());
		fadeRight.visible = background.x + background.width() < Camera.main.width;
	}

	private void resetFade(){
		//starts fading after 4 seconds, fades over 4 seconds.
		uiAlpha = 2f;
		updateFade();
	}

	@Override
	protected void onBackPressed() {
		if (btnExit.active){
			ShatteredPixelDungeon.switchScene(TitleScene.class);
		} else {
			super.onBackPressed();
		}
	}

	private class HeroBtn extends StyledButton {

		private HeroClass cl;

		private static final int MIN_WIDTH = 20;
		private static final int HEIGHT = 24;

		HeroBtn ( HeroClass cl ){
			super(Chrome.Type.GREY_BUTTON_TR, "");

			this.cl = cl;

			updateIcon();

		}

		//选角按钮图标取 row6 的首帧，按职业帧尺寸切（环指大师 20×24，原版 12×15）；
		//拇指 前二老板开启「泪锋之剑」皮肤后改用皮肤行（第 8 行）
		void updateIcon(){
			int[] fs = HeroSprite.frameSize( cl );
			int row = (cl == HeroClass.VALENCINA && SPDSettings.valencinaSkin())
					? HeroSprite.VALENCINA_SKIN_TIER : 6;
			icon(new Image(cl.spritesheet(), 0, row*fs[1], fs[0], fs[1]));
		}

		@Override
		public void update() {
			super.update();
			if (cl != GamesInProgress.selectedClass){
				if (!cl.isUnlocked()){
					icon.brightness(0.1f);
				} else {
					icon.brightness(0.6f);
				}
			} else {
				icon.brightness(1f);
			}
		}

		@Override
		protected void onClick() {
			super.onClick();

			if( !cl.isUnlocked() ){
				ShatteredPixelDungeon.scene().addToFront( new WndMessage(cl.unlockMsg()));
			} else if (GamesInProgress.selectedClass == cl) {
				Window w = new WndHeroInfo(cl);
				if (landscape()){
					w.offset(Camera.main.width/6, 0);
				}
				ShatteredPixelDungeon.scene().addToFront(w);
			} else {
				setSelectedHero(cl);
			}
		}

		@Override
		protected void layout() {
			super.layout();
			//if we're super tall (i.e. rendering into display inset) then put hero at the top
			if (height > 30) {
				icon.y = y + (HEIGHT - icon.height()) / 2f;
			}
		}
	}

	private class GameOptions extends Component {

		private NinePatch bg;

		private ArrayList<StyledButton> buttons;
		private ArrayList<ColorBlock> spacers;

		protected StyledButton challengeButton;

		protected StyledButton trialsButton;

		protected StyledButton funButton;

		@Override
		protected void createChildren() {

			bg = Chrome.get(Chrome.Type.GREY_BUTTON_TR);
			add(bg);

			buttons = new ArrayList<>();
			spacers = new ArrayList<>();
			StyledButton seedButton = new StyledButton(Chrome.Type.BLANK, Messages.get(HeroSelectScene.class, "custom_seed"), 6){
				@Override
				protected void onClick() {
					if (!Badges.isUnlocked(Badges.Badge.VICTORY) && !DeviceCompat.isDebug()){
						ShatteredPixelDungeon.scene().addToFront( new WndTitledMessage(
								Icons.get(Icons.SEED),
								Messages.get(HeroSelectScene.class, "custom_seed"),
								Messages.get(HeroSelectScene.class, "custom_seed_nowin"))
						);
						return;
					}

					String existingSeedtext = SPDSettings.customSeed();
					ShatteredPixelDungeon.scene().addToFront( new WndTextInput(Messages.get(HeroSelectScene.class, "custom_seed_title"),
							Messages.get(HeroSelectScene.class, "custom_seed_desc"),
							existingSeedtext,
							20,
							false,
							Messages.get(HeroSelectScene.class, "custom_seed_set"),
							Messages.get(HeroSelectScene.class, "custom_seed_clear")){
						@Override
						public void onSelect(boolean positive, String text) {
							text = DungeonSeed.formatText(text);
							long seed = DungeonSeed.convertFromText(text);

							if (positive && seed != -1){

								for (GamesInProgress.Info info : GamesInProgress.checkAll()){
									if (info.customSeed.isEmpty() && info.seed == seed){
										SPDSettings.customSeed("");
										icon.resetColor();
										ShatteredPixelDungeon.scene().addToFront(new WndMessage(Messages.get(HeroSelectScene.class, "custom_seed_duplicate")));
										return;
									}
								}

								SPDSettings.customSeed(text);
								icon.hardlight(1f, 1.5f, 0.67f);
							} else {
								SPDSettings.customSeed("");
								icon.resetColor();
							}
							updateOptionsColor();
						}
					});
				}
			};
			seedButton.leftJustify = true;
			seedButton.icon(Icons.get(Icons.SEED));
			if (!SPDSettings.customSeed().isEmpty()) seedButton.icon().hardlight(1f, 1.5f, 0.67f);;
			buttons.add(seedButton);
			add(seedButton);

			StyledButton dailyButton = new StyledButton(Chrome.Type.BLANK, Messages.get(HeroSelectScene.class, "daily"), 6){

				private static final long SECOND = 1000;
				private static final long MINUTE = 60 * SECOND;
				private static final long HOUR = 60 * MINUTE;
				private static final long DAY = 24 * HOUR;

				@Override
				protected void onClick() {
					super.onClick();

					if (!Badges.isUnlocked(Badges.Badge.VICTORY) && !DeviceCompat.isDebug()){
						ShatteredPixelDungeon.scene().addToFront( new WndTitledMessage(
								Icons.get(Icons.CALENDAR),
								Messages.get(HeroSelectScene.class, "daily"),
								Messages.get(HeroSelectScene.class, "daily_nowin"))
						);
						return;
					}

					long diff = (SPDSettings.lastDaily() + DAY) - Game.realTime;
					if (diff > 24*HOUR){
						ShatteredPixelDungeon.scene().addToFront(new WndMessage(Messages.get(HeroSelectScene.class, "daily_unavailable_long", (diff / DAY)+1)));
						return;
					}

					for (GamesInProgress.Info game : GamesInProgress.checkAll()){
						if (game.daily){
							ShatteredPixelDungeon.scene().addToFront(new WndMessage(Messages.get(HeroSelectScene.class, "daily_existing")));
							return;
						}
					}

					Image icon = Icons.get(Icons.CALENDAR);
					if (diff <= 0)  icon.hardlight(0.5f, 1f, 2f);
					else            icon.hardlight(1f, 0.5f, 2f);
					ShatteredPixelDungeon.scene().addToFront(new WndOptions(
							icon,
							Messages.get(HeroSelectScene.class, "daily"),
							diff > 0 ?
								Messages.get(HeroSelectScene.class, "daily_repeat") :
								Messages.get(HeroSelectScene.class, "daily_desc"),
							Messages.get(HeroSelectScene.class, "daily_yes"),
							Messages.get(HeroSelectScene.class, "daily_no")){
						@Override
						protected void onSelect(int index) {
							if (index == 0){
								if (diff <= 0) {
									long time = Game.realTime - (Game.realTime % DAY);

									//earliest possible daily for v3.0.1 is Mar 01 2025
									//which is 20,148 days days after Jan 1 1970
									time = Math.max(time, 20_148 * DAY);

									SPDSettings.lastDaily(time);
									Dungeon.dailyReplay = false;
								} else {
									Dungeon.dailyReplay = true;
								}

								Dungeon.hero = null;
								Dungeon.daily = true;
								Dungeon.initSeed();
								ActionIndicator.clearAction();
								InterlevelScene.mode = InterlevelScene.Mode.DESCEND;

								Game.switchScene( InterlevelScene.class );
							}
						}
					});
				}

				private long timeToUpdate = 0;

				private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.ROOT);
				{
					dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				}

				@Override
				public void update() {
					super.update();

					if (Game.realTime > timeToUpdate && visible){
						long diff = (SPDSettings.lastDaily() + DAY) - Game.realTime;

						if (diff > 0){
							if (diff > 30*HOUR){
								text("30:00:00+");
							} else {
								text(dateFormat.format(new Date(diff)));
							}
							timeToUpdate = Game.realTime + SECOND;
						} else {
							text(Messages.get(HeroSelectScene.class, "daily"));
							timeToUpdate = Long.MAX_VALUE;
						}
					}

				}
			};
			dailyButton.leftJustify = true;
			dailyButton.icon(Icons.get(Icons.CALENDAR));
			add(dailyButton);
			buttons.add(dailyButton);

			challengeButton = new StyledButton(Chrome.Type.BLANK, Messages.get(WndChallenges.class, "title"), 6){
				@Override
				protected void onClick() {
					if (!Badges.isUnlocked(Badges.Badge.VICTORY) && !DeviceCompat.isDebug()){
						ShatteredPixelDungeon.scene().addToFront( new WndTitledMessage(
								Icons.get(Icons.CHALLENGE_GREY),
								Messages.get(WndChallenges.class, "title"),
								Messages.get(HeroSelectScene.class, "challenges_nowin")
						));
						return;
					}

					ShatteredPixelDungeon.scene().addToFront(new WndChallenges(SPDSettings.challenges(), true) {
						public void onBackPressed() {
							super.onBackPressed();
							icon(Icons.get(SPDSettings.challenges() > 0 ? Icons.CHALLENGE_COLOR : Icons.CHALLENGE_GREY));
							updateOptionsColor();
						}
					} );
				}
			};
			challengeButton.leftJustify = true;
			challengeButton.icon(Icons.get(SPDSettings.challenges() > 0 ? Icons.CHALLENGE_COLOR : Icons.CHALLENGE_GREY));
			add(challengeButton);
			buttons.add(challengeButton);

			//考验：与挑战互相独立、可同时开；刻意不套用挑战那道「必须先通关一次」的门控，
			//方便随时进游戏验证（要门控的话照抄上面 challengeButton 的 Badges.isUnlocked 分支即可）
			trialsButton = new StyledButton(Chrome.Type.BLANK, Messages.get(WndTrials.class, "title"), 6){
				@Override
				protected void onClick() {
					ShatteredPixelDungeon.scene().addToFront(new WndTrials(SPDSettings.trials(), true) {
						public void onBackPressed() {
							super.onBackPressed();
							icon(Icons.get(SPDSettings.trials() > 0 ? Icons.TRIAL_COLOR : Icons.TRIAL_GREY));
						}
					} );
				}
			};
			trialsButton.leftJustify = true;
			trialsButton.icon(Icons.get(SPDSettings.trials() > 0 ? Icons.TRIAL_COLOR : Icons.TRIAL_GREY));
			add(trialsButton);
			buttons.add(trialsButton);

			//趣味挑战：与常规挑战/考验都互相独立，但**与常规挑战共用同一个位域**（SPDSettings.challenges）；
			//套用挑战的按钮样式，只是单独入口、单独图标（icons.png 的 FUN_GREY / FUN_COLOR）。
			//⚠️ 判据用 activeFun(SPDSettings.challenges())——选人界面里 Dungeon.challenges 还是 0，不能用无参版
			funButton = new StyledButton(Chrome.Type.BLANK, Messages.get(WndFunChallenges.class, "title"), 6){
				@Override
				protected void onClick() {
					ShatteredPixelDungeon.scene().addToFront(new WndFunChallenges(SPDSettings.challenges(), true) {
						public void onBackPressed() {
							super.onBackPressed();
							icon(Icons.get(Challenges.activeFun(SPDSettings.challenges()) > 0 ? Icons.FUN_COLOR : Icons.FUN_GREY));
							updateOptionsColor();
						}
					} );
				}
			};
			funButton.leftJustify = true;
			funButton.icon(Icons.get(Challenges.activeFun(SPDSettings.challenges()) > 0 ? Icons.FUN_COLOR : Icons.FUN_GREY));
			add(funButton);
			buttons.add(funButton);

			int unlockedCount = 0;
			for (HeroClass cls : HeroClass.values()){
				if (cls.isUnlocked()) unlockedCount++;
			}

			if (unlockedCount >= 2) {
				StyledButton randomButton = new StyledButton(Chrome.Type.BLANK, Messages.get(HeroSelectScene.class, "randomize"), 6) {
					@Override
					protected void onClick() {

						if (Badges.isUnlocked(Badges.Badge.VICTORY) || DeviceCompat.isDebug()){
							ShatteredPixelDungeon.scene().addToFront(new WndRandomize());
						} else {

							HeroClass randomCls;
							do {
								randomCls = Random.oneOf(HeroClass.values());
							} while (!randomCls.isUnlocked());
							setSelectedHero(randomCls);
							GamesInProgress.randomizedClass = true;
						}
					}
				};
				randomButton.leftJustify = true;
				randomButton.icon(Icons.SHUFFLE.get());
				buttons.add(randomButton);
				add(randomButton);
			}

			for (int i = 1; i < buttons.size(); i++){
				ColorBlock spc = new ColorBlock(1, 1, 0xFF000000);
				add(spc);
				spacers.add(spc);
			}
		}

		private class WndRandomize extends Window {

			CheckBox chkHero;
			CheckBox chkChals;
			OptionSlider optChals;

			public WndRandomize(){
				super();

				chkHero = new CheckBox(Messages.get(HeroSelectScene.class, "randomize_hero")){
					@Override
					public void checked(boolean value) {
						super.checked(value);
						heroWasRandomized = value;
					}
				};
				chkHero.setRect(0, 0, 120, 16);
				chkHero.checked(heroWasRandomized);
				add(chkHero);

				chkChals = new CheckBox(Messages.get(HeroSelectScene.class, "randomize_chals")){
					@Override
					public void checked(boolean value) {
						super.checked(value);
						optChals.enable(value);
						chalWasRandomized = value;
					}
				};
				chkChals.setRect(0, 20, 120, 16);
				add(chkChals);

				int max = Challenges.MAX_CHALS;
				optChals = new OptionSlider(Messages.get(HeroSelectScene.class, "randomize_chals_title"), "0", Integer.toString(max), 0, max) {
					@Override
					protected void onChange() {
						//do nothing immediately
					}
				};
				optChals.enable(false);
				optChals.setSelectedValue(Challenges.activeChallenges(SPDSettings.challenges()));
				optChals.setRect(0, 38, 120, 22);
				add(optChals);

				chkChals.checked(chalWasRandomized);

				RedButton btnCancel = new RedButton(Messages.get(HeroSelectScene.class, "randomize_cancel")){
					@Override
					protected void onClick() {
						super.onClick();
						hide();
					}
				};
				btnCancel.setRect(61, 64, 60, 16);
				add(btnCancel);

				RedButton btnConfirm = new RedButton(Messages.get(HeroSelectScene.class, "randomize_confirm")){
					@Override
					protected void onClick() {
						super.onClick();
						hide();

						if (chkChals.checked()){
							int chals = optChals.getSelectedValue();
							ArrayList<Integer> chalMasks = new ArrayList<>();
							//随机池 = 常规挑战（排除调试模式这类特殊挑战），取自 Challenges 的权威顺序；
							//不要再写 for (i < MAX_CHALS) 1<<i —— 那要求常规挑战刚好占满低若干位
							for (int ch : Challenges.regularMasks()){
								chalMasks.add(ch);
							}
							Random.shuffle(chalMasks);
							int mask = 0;
							for (int i = 0; i < chals; i++){
								mask += chalMasks.remove(0);
							}
							SPDSettings.challenges(mask);
							challengeButton.icon(Icons.get(SPDSettings.challenges() > 0 ? Icons.CHALLENGE_COLOR : Icons.CHALLENGE_GREY));
							ShatteredPixelDungeon.scene().addToFront(new WndChallenges(mask, false));
						}

						if (chkHero.checked()){
							HeroClass randomCls;
							do {
								randomCls = Random.oneOf(HeroClass.values());
							} while (!randomCls.isUnlocked());
							setSelectedHero(randomCls);
							GamesInProgress.randomizedClass = true;
						} else {
							setSelectedHero(GamesInProgress.selectedClass);
						}
					}
				};
				btnConfirm.setRect(0, 64, 60, 16);
				add(btnConfirm);

				resize(120, (int)btnConfirm.bottom());

			}

		}

		@Override
		protected void layout() {
			super.layout();

			bg.x = x;
			bg.y = y;

			int width = 0;
			for (StyledButton btn : buttons){
				if (width < btn.reqWidth()) width = (int)btn.reqWidth();
			}
			width += bg.marginHor();

			int top = (int)y + bg.marginTop() - 1;
			int i = 0;
			for (StyledButton btn : buttons){
				btn.setRect(x+bg.marginLeft(), top, width - bg.marginHor(), 16);
				top = (int)btn.bottom();
				if (i < spacers.size()) {
					spacers.get(i).size(btn.width(), 1);
					spacers.get(i).x = btn.left();
					spacers.get(i).y = PixelScene.align(btn.bottom()-0.5f);
					i++;
				}
			}

			this.width = width;
			this.height = top+bg.marginBottom()-y-1;
			bg.size(this.width, this.height);

		}

		private void alpha( float value ){
			bg.alpha(value);

			for (StyledButton btn : buttons){
				btn.alpha(value);
			}

			for (ColorBlock spc : spacers){
				spc.alpha(value);
			}
		}
	}

}
