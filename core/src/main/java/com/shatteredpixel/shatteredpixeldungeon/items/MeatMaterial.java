package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 肉质素材：人体派作品合成材料之一。
 * 提供 Meat +10，同时降低 Weapon/Bone/Blood 各 2（过度使用单一素材会削弱其他属性）。
 * 图标：items.png 38行第1个，尺寸 16×15。
 * 名称/描述文本键 items.meatmaterial.*（类简单名全小写）。
 */
public class MeatMaterial extends BodyArtMaterial {
	{
		image = ItemSpriteSheet.MEAT_MATERIAL;
		meatValue   = 10;
		weaponValue = -2;
		boneValue   = -2;
		bloodValue  = -2;
	}

	// 肉质素材仅在骨骸编织 +2 时可直接使用
	@Override
	public boolean canWeave( Hero hero ) {
		return hero.hasTalent( Talent.BONE_WEAVING )
				&& hero.pointsInTalent( Talent.BONE_WEAVING ) >= 2;
	}
}
