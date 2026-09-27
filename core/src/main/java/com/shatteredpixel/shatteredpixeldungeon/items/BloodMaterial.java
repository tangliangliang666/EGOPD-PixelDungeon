package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 血质素材：人体派作品合成材料之一。
 * 提供 Blood +10，同时降低 Weapon/Bone/Meat 各 2（过度使用单一素材会削弱其他属性）。
 * 图标：items.png 38行第2个，尺寸 14×16。
 * 名称/描述文本键 items.bloodmaterial.*（类简单名全小写）。
 */
public class BloodMaterial extends BodyArtMaterial {
	{
		image = ItemSpriteSheet.BLOOD_MATERIAL;
		bloodValue  = 10;
		weaponValue = -2;
		boneValue   = -2;
		meatValue   = -2;
	}
}
