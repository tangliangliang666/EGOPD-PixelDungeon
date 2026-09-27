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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MorphCombo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MorphWeapon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Image;
import com.watabou.utils.Reflection;

/**
 * Window showing the nine alternate shapes of the morph weapon as a 3x3 grid of icons.
 * <p>
 * The current form is skipped, so exactly nine icons are shown. The grid is icon-only:
 * a form's name only appears as a hover tooltip. Clicking an icon switches the weapon
 * into that form (preserving level/enchantment via {@link MorphWeapon#morphInto}).
 * <p>
 * Note: the icon cannot be created in {@code createChildren} (which runs during {@code super()},
 * before {@code this.target} is set), so it is built in the constructor after {@code super()},
 * mirroring {@link com.shatteredpixel.shatteredpixeldungeon.ui.IconButton}.
 */
public class WndMorph extends Window {

	public static final int CELL_W = 36;
	public static final int CELL_H = 36;
	public static final int GAP = 3;
	public static final int PAD = 6;

	private static final int WIDTH = 3 * CELL_W + 2 * GAP + 2 * PAD;

	public WndMorph(final Weapon weapon, final Hero hero) {

		super();

		IconTitle titlebar = new IconTitle();
		titlebar.icon(new ItemSprite(weapon.image(), null));
		titlebar.label(weapon.name() + "  --  切换为");
		titlebar.setRect(0, 0, WIDTH - 14, 0);
		add(titlebar);

		//build the 3x3 grid of the nine alternative forms (skipping the current one)
		int count = 0;
		for (Class<? extends Weapon> cls : MorphWeapon.ALL_FORMS) {
			if (cls == weapon.getClass()) continue;

			int col = count % 3;
			int row = count / 3;
			float x = PAD + col * (CELL_W + GAP);
			float y = titlebar.bottom() + PAD + row * (CELL_H + GAP);

			FormCell cell = new FormCell(weapon, hero, cls);
			cell.setRect(x, y, CELL_W, CELL_H);
			add(cell);
			count++;
		}

		//自动切换开关：开启后，用本系列武器命中敌人时会自动随机切到尚未用过的形态
		final RedButton btnAuto = new RedButton(autoSwitchLabel()) {
			@Override
			protected void onClick() {
				SPDSettings.morphAutoSwitch( !SPDSettings.morphAutoSwitch() );
				text( autoSwitchLabel() );
			}

			@Override
			protected String hoverText() {
				return "开启后，使用漆黑噤默系列武器命中敌人，会自动随机切换到一个尚未使用过的形态";
			}
		};
		btnAuto.setRect(PAD, titlebar.bottom() + PAD + 3 * (CELL_H + GAP) + 2, WIDTH - 2 * PAD, 16);
		add(btnAuto);

		RedButton btnCancel = new RedButton("取消") {
			@Override
			protected void onClick() {
				hide();
			}
		};
		btnCancel.setRect(PAD, btnAuto.bottom() + 3, WIDTH - 2 * PAD, 16);
		add(btnCancel);

		resize(WIDTH, (int) btnCancel.bottom() + PAD);
	}

	/** 自动切换开关的按钮文字（开 / 关） */
	private static String autoSwitchLabel(){
		return SPDSettings.morphAutoSwitch() ? "自动切换：开" : "自动切换：关";
	}

	private class FormCell extends Button {

		private final Weapon source;
		private final Hero hero;
		private final Class<? extends Weapon> target;
		private final String nameStr;
		/** 该形态是否已在当前连击 buff 持续期内命中过（已叠层的形态标绿点）。 */
		private final boolean used;

		private Image sprite;
		private Image usedMark;

		FormCell(Weapon source, Hero hero, Class<? extends Weapon> target) {
			super();
			this.source = source;
			this.hero = hero;
			this.target = target;

			MorphCombo combo = hero.buff(MorphCombo.class);
			this.used = combo != null && combo.hasUsed(target);

			Weapon sample = Reflection.newInstance(target);
			this.nameStr = (sample != null) ? sample.name() : target.getSimpleName();

			//build the icon now that `target` is set (i.e. after super())
			if (sample != null) {
				sprite = new ItemSprite(sample.image(), null);
				add(sprite);
			}

			//已使用的形态：左上角标一个绿点，一眼看出九种形态还差哪些没叠过层
			if (used) {
				usedMark = new Image(TextureCache.createSolid(0xFF33CC33));
				add(usedMark);
			}
		}

		@Override
		protected void layout() {
			super.layout();
			if (sprite != null) {
				sprite.x = x + (width - sprite.width()) / 2f;
				sprite.y = y + (height - sprite.height()) / 2f;
				PixelScene.align(sprite);
			}
			if (usedMark != null) {
				usedMark.scale.set(4, 4);
				usedMark.x = x + 2;
				usedMark.y = y + 2;
				PixelScene.align(usedMark);
			}
		}

		@Override
		protected void onClick() {
			super.onClick();
			MorphWeapon.morphInto(source, hero, target);
			hide();
		}

		@Override
		protected String hoverText() {
			return used ? nameStr + "（已使用）" : nameStr;
		}
	}
}
