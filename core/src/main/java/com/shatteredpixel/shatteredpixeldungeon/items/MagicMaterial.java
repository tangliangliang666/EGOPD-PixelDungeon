package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 魔质素材：环指大师专属强化材料（2026-09-01）。
 * <p>不含 Weapon/Bone/Meat/Blood 四项数值，无法用于「创作」合成（{@link #usableInCraft()} 返 false）；
 * 仅可放入「强化」作为材料——当前强化中无数值效果（四值全 0），将在后续强化配方中发挥作用。
 * 图标：items.png 36 行第 5 个，尺寸 15×14。名称/描述文本键 items.magicmaterial.*。</p>
 */
public class MagicMaterial extends BodyArtMaterial {

	{
		image = ItemSpriteSheet.MAGIC_MATERIAL;
		// 四项数值保持默认 0（不添加任何值）
	}

	@Override
	public boolean usableInCraft() {
		// 魔质素材不可用于「创作」合成，仅作为强化材料
		return false;
	}

	@Override
	protected String valueSummary() {
		// 无四项数值，改显示说明而非 0/0/0/0
		return "_（不含 Weapon/Bone/Meat/Blood 数值；仅供强化，将在后续强化配方中发挥作用）_";
	}
}
