package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 骨质素材：人体派作品合成材料之一。
 * 提供 Bone +10，同时降低 Weapon/Meat/Blood 各 2（过度使用单一素材会削弱其他属性）。
 * 图标：items.png 37行第2个，尺寸 16×15。
 * 名称/描述文本键 items.bonematerial.*（类简单名全小写）。
 */
public class BoneMaterial extends BodyArtMaterial {
	{
		image = ItemSpriteSheet.BONE_MATERIAL;
		boneValue   = 10;
		weaponValue = -2;
		meatValue   = -2;
		bloodValue  = -2;
	}

	// 骨质素材在骨骸编织天赋 +1/+2 下均可直接使用
	@Override
	public boolean canWeave( Hero hero ) {
		return true;
	}
}
