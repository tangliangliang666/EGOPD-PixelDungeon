package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 硬质素材：人体派作品合成材料之一。
 * 提供 Weapon +10，同时降低 Bone/Meat/Blood 各 2（过度使用单一素材会削弱其他属性）。
 * 图标：items.png 37行第1个，尺寸 13×14。
 * 名称/描述文本键 items.hardmaterial.*（类简单名全小写）。
 */
public class HardMaterial extends BodyArtMaterial {
	{
		image = ItemSpriteSheet.HARD_MATERIAL;
		weaponValue = 10;
		boneValue   = -2;
		meatValue   = -2;
		bloodValue  = -2;
	}
}
