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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.ArcaneResin;
import com.shatteredpixel.shatteredpixeldungeon.items.CharcoalResin;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.GoldenResin;
import com.shatteredpixel.shatteredpixeldungeon.items.HolyCard;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.LiquidMetal;
import com.shatteredpixel.shatteredpixeldungeon.items.LloydTalisman;
import com.shatteredpixel.shatteredpixeldungeon.items.LooseCharcoalResin;
import com.shatteredpixel.shatteredpixeldungeon.items.LooseGoldenResin;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Blandfruit;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MeatPie;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Pasty;
import com.shatteredpixel.shatteredpixeldungeon.items.food.StewedMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.AquaBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.BlizzardBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.CausticBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.InfernalBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.ShockingBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.UnstableBrew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfAquaticRejuvenation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfArcaneArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfDragonsBlood;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfHoneyedHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfIcyTouch;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfToxicEssence;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDragonsBreath;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.Alchemize;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.BeaconOfReturning;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.CurseInfusion;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.MagicalInfusion;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.PhaseShift;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.ReclaimTrap;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.Recycle;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.SummonElemental;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.TelekineticGrab;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.UnstableSpell;
import com.shatteredpixel.shatteredpixeldungeon.items.spells.WildEnergy;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.Enkephalin;
//原版神器的强化形态（炼金指南「法术」页展示配方输入→输出）
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ApproachingDay;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.BloodFeastChalice;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChainOfOthers;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChapterNineVerseTwo;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.LifelongStew;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.UnstableSpellbook;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RedEye;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Shortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Embers;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.DimDusk;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.FirelinkGreatsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BrandWorkshop;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WaxWing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Greatsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ParadiseLost;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.scenes.AlchemyScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;

public class QuickRecipe extends Component {
	
	private ArrayList<Item> ingredients;
	
	private ArrayList<ItemSlot> inputs;
	private QuickRecipe.arrow arrow;
	private ItemSlot output;
	
	public QuickRecipe(Recipe.SimpleRecipe r){
		this(r, r.getIngredients(), r.sampleOutput(null));
	}
	
	public QuickRecipe(Recipe r, ArrayList<Item> inputs, final Item output) {
		
		ingredients = inputs;
		int cost = r.cost(inputs);
		boolean hasInputs = true;
		this.inputs = new ArrayList<>();
		for (final Item in : inputs) {
			anonymize(in);
			ItemSlot curr;
			curr = new ItemSlot(in) {
				{
					hotArea.blockLevel = PointerArea.NEVER_BLOCK;
				}

				@Override
				protected void onClick() {
					ShatteredPixelDungeon.scene().addToFront(new WndInfoItem(in));
				}
			};

			int quantity = 0;
			if (Dungeon.hero != null) {
				ArrayList<Item> similar = Dungeon.hero.belongings.getAllSimilar(in);
				for (Item sim : similar) {
					//if we are looking for a specific item, it must be IDed
					if (sim.getClass() != in.getClass() || sim.isIdentified())
						quantity += sim.quantity();
				}
				if (quantity < in.quantity()) {
					curr.sprite.alpha(0.3f);
					hasInputs = false;
				}
			} else {
				hasInputs = false;
			}

			curr.showExtraInfo(false);
			add(curr);
			this.inputs.add(curr);
		}
		
		if (cost > 0) {
			arrow = new arrow(Icons.get(Icons.ARROW), cost);
			arrow.hardlightText(0x44CCFF);
		} else {
			arrow = new arrow(Icons.get(Icons.ARROW));
		}
		if (hasInputs) {
			arrow.icon.tint(1, 1, 0, 1);
			if (!(ShatteredPixelDungeon.scene() instanceof AlchemyScene)) {
				arrow.enable(false);
			}
		} else {
			arrow.icon.color(0, 0, 0);
			arrow.enable(false);
		}
		add(arrow);
		
		anonymize(output);
		this.output = new ItemSlot(output){
			@Override
			protected void onClick() {
				ShatteredPixelDungeon.scene().addToFront(new WndInfoItem(output));
			}
		};
		if (Dungeon.hero != null && !hasInputs){
			this.output.sprite.alpha(0.3f);
		}
		this.output.showExtraInfo(false);
		add(this.output);
		
		layout();
	}
	
	@Override
	protected void layout() {
		
		height = 16;
		width = 0;

		int padding = inputs.size() == 1 ? 8 : 0;

		for (ItemSlot item : inputs){
			item.setRect(x + width + padding, y, 16, 16);
			width += 16 + padding;
		}
		
		arrow.setRect(x + width, y, 14, 16);
		width += 14;
		
		output.setRect(x + width, y, 16, 16);
		width += 16;

		width += padding;
	}
	
	//used to ensure that un-IDed items are not spoiled
	private void anonymize(Item item){
		if (item instanceof Potion){
			((Potion) item).anonymize();
		} else if (item instanceof Scroll){
			((Scroll) item).anonymize();
		}
	}
	
	public class arrow extends IconButton {
		
		BitmapText text;
		
		public arrow(){
			super();
		}
		
		public arrow( Image icon ){
			super( icon );
		}
		
		public arrow( Image icon, int count ){
			super( icon );
			hotArea.blockLevel = PointerArea.NEVER_BLOCK;

			text = new BitmapText( Integer.toString(count), PixelScene.pixelFont);
			text.measure();
			add(text);
		}
		
		@Override
		protected void layout() {
			super.layout();
			
			if (text != null){
				text.x = x;
				text.y = y;
				PixelScene.align(text);
			}
		}
		
		@Override
		protected void onPointerUp() {
			icon.brightness(1f);
		}

		@Override
		protected void onClick() {
			super.onClick();

			//「快速套用」只对炼金场景有意义：本按钮在其它场景（日志里的炼金指南等）是禁用态，
			//这里再兜一层，防止任何漏网之鱼把 scene() 强转成 AlchemyScene 直接 ClassCastException 闪退。
			if (!(ShatteredPixelDungeon.scene() instanceof AlchemyScene)) return;
			if (Dungeon.hero == null) return;

			//find the window this is inside of and close it
			Group parent = this.parent;
			while (parent != null){
				if (parent instanceof Window){
					((Window) parent).hide();
					break;
				} else {
					parent = parent.parent;
				}
			}
			
			((AlchemyScene)ShatteredPixelDungeon.scene()).populate(ingredients, Dungeon.hero.belongings);
		}
		
		public void hardlightText(int color ){
			if (text != null) text.hardlight(color);
		}
	}
	
	//gets recipes for a particular alchemy guide page
	//a null entry indicates a break in section
	public static ArrayList<QuickRecipe> getRecipes( int pageIdx ){
		ArrayList<QuickRecipe> result = new ArrayList<>();
		switch (pageIdx){
			case 0: default:
				result.add(new QuickRecipe( new Potion.SeedToPotion(), new ArrayList<>(Arrays.asList(new Plant.Seed.PlaceHolder().quantity(3))), new WndBag.Placeholder(ItemSpriteSheet.POTION_HOLDER){
					@Override
					public String name() {
						return Messages.get(Potion.SeedToPotion.class, "name");
					}

					@Override
					public String info() {
						return "";
					}
				}));
				return result;
			case 1:
				Recipe r = new Scroll.ScrollToStone();
				for (Class<?> cls : Generator.Category.SCROLL.classes){
					Scroll scroll = (Scroll) Reflection.newInstance(cls);
					if (!scroll.isKnown()) scroll.anonymize();
					ArrayList<Item> in = new ArrayList<Item>(Arrays.asList(scroll));
					result.add(new QuickRecipe( r, in, r.sampleOutput(in)));
				}
				return result;
			case 2:
				result.add(new QuickRecipe( new StewedMeat.oneMeat() ));
				result.add(new QuickRecipe( new StewedMeat.twoMeat() ));
				result.add(new QuickRecipe( new StewedMeat.threeMeat() ));
				result.add(null);
				result.add(new QuickRecipe( new MeatPie.Recipe(),
						new ArrayList<Item>(Arrays.asList(new Pasty(), new Food(), new MysteryMeat.PlaceHolder())),
						new MeatPie()));
				result.add(null);
				result.add(new QuickRecipe( new Blandfruit.CookFruit(),
						new ArrayList<>(Arrays.asList(new Blandfruit(), new Plant.Seed.PlaceHolder())),
						new Blandfruit(){

							public String name(){
								return Messages.get(Blandfruit.class, "cooked");
							}
							
							@Override
							public String info() {
								return "";
							}
						}));
				return result;
			case 3:
				r = new ExoticPotion.PotionToExotic();
				for (Class<?> cls : Generator.Category.POTION.classes){
					Potion pot = (Potion) Reflection.newInstance(cls);
					ArrayList<Item> in = new ArrayList<>(Arrays.asList(pot));
					result.add(new QuickRecipe( r, in, r.sampleOutput(in)));
				}
				return result;
			case 4:
				r = new ExoticScroll.ScrollToExotic();
				for (Class<?> cls : Generator.Category.SCROLL.classes){
					Scroll scroll = (Scroll) Reflection.newInstance(cls);
					ArrayList<Item> in = new ArrayList<>(Arrays.asList(scroll));
					result.add(new QuickRecipe( r, in, r.sampleOutput(in)));
				}
				return result;
			case 5:
				r = new Bomb.EnhanceBomb();
				int i = 0;
				for (Class<?> cls : Bomb.EnhanceBomb.validIngredients.keySet()){
					if (i == 2){
						result.add(null);
						i = 0;
					}
					Item item = (Item) Reflection.newInstance(cls);
					ArrayList<Item> in = new ArrayList<>(Arrays.asList(new Bomb(), item));
					result.add(new QuickRecipe( r, in, r.sampleOutput(in)));
					i++;
				}
				return result;
			case 6:
				result.add(new QuickRecipe( new LiquidMetal.Recipe(),
						new ArrayList<Item>(Arrays.asList(new MissileWeapon.PlaceHolder())),
						new LiquidMetal()));
				result.add(new QuickRecipe( new ArcaneResin.Recipe(),
						new ArrayList<Item>(Arrays.asList(new Wand.PlaceHolder())),
						new ArcaneResin()));
				//2026-09-24:焦炭松脂 / 黄金松脂系列（原先只登记在 Recipe 表里，炼金指南页看不到，这里补上展示）
				result.add(new QuickRecipe( new CharcoalResin.FromPotion(),
						new ArrayList<Item>(Arrays.asList(new PotionOfLiquidFlame())),
						new CharcoalResin()));
				result.add(new QuickRecipe( new LooseCharcoalResin.SplitRecipe(),
						new ArrayList<Item>(Arrays.asList(new CharcoalResin())),
						new LooseCharcoalResin().quantity(2)));
				result.add(new QuickRecipe( new GoldenResin.FromPotion(),
						new ArrayList<Item>(Arrays.asList(new ShockingBrew())),
						new GoldenResin()));
				result.add(new QuickRecipe( new LooseGoldenResin.SplitRecipe(),
						new ArrayList<Item>(Arrays.asList(new GoldenResin())),
						new LooseGoldenResin().quantity(2)));
				//2026-09-24: 洛伊德护符：治疗药水 × 1 + 液金 × 20，3 能量 → 洛伊德护符 × 6
				result.add(new QuickRecipe( new LloydTalisman.CraftRecipe(),
						new ArrayList<Item>(Arrays.asList(new PotionOfHealing(), new LiquidMetal().quantity(20))),
						new LloydTalisman().quantity(6)));
				//2026-09-24: 神圣卡：任意卷轴 × 1，13 能量 → 神圣卡 × 1
				//原料用 Scroll.PlaceHolder 展示（与上面「异域卷轴」等条目同一手法），表示「任意卷轴」
				result.add(new QuickRecipe( new HolyCard.CraftRecipe(),
						new ArrayList<Item>(Arrays.asList(new Scroll.PlaceHolder())),
						new HolyCard()));
				return result;
			case 7:
				result.add(new QuickRecipe(new UnstableBrew.Recipe(), new ArrayList<>(Arrays.asList(new Potion.PlaceHolder(), new  Plant.Seed.PlaceHolder())), new UnstableBrew()));
				result.add(new QuickRecipe(new CausticBrew.Recipe()));
				result.add(new QuickRecipe(new BlizzardBrew.Recipe()));
				result.add(new QuickRecipe(new ShockingBrew.Recipe()));
				result.add(new QuickRecipe(new InfernalBrew.Recipe()));
				result.add(new QuickRecipe(new AquaBrew.Recipe()));
				result.add(null);
				result.add(null);
				result.add(new QuickRecipe(new ElixirOfHoneyedHealing.Recipe()));
				result.add(new QuickRecipe(new ElixirOfAquaticRejuvenation.Recipe()));
				result.add(new QuickRecipe(new ElixirOfArcaneArmor.Recipe()));
				result.add(new QuickRecipe(new ElixirOfIcyTouch.Recipe()));
				result.add(new QuickRecipe(new ElixirOfToxicEssence.Recipe()));
				result.add(new QuickRecipe(new ElixirOfDragonsBlood.Recipe()));
				result.add(new QuickRecipe(new ElixirOfFeatherFall.Recipe()));
				result.add(new QuickRecipe(new ElixirOfMight.Recipe()));
				return result;
			case 8:
				result.add(new QuickRecipe(new UnstableSpell.Recipe(), new ArrayList<>(Arrays.asList(new Scroll.PlaceHolder(), new  Runestone.PlaceHolder())), new UnstableSpell()));
				result.add(new QuickRecipe(new WildEnergy.Recipe()));
				result.add(new QuickRecipe(new TelekineticGrab.Recipe()));
				result.add(new QuickRecipe(new PhaseShift.Recipe()));
				if (!PixelScene.landscape()) result.add(null);
				result.add(null);
				result.add(new QuickRecipe(new Alchemize.Recipe(), new ArrayList<>(Arrays.asList(new Plant.Seed.PlaceHolder(), new Runestone.PlaceHolder())), new Alchemize().quantity(8)));
				result.add(new QuickRecipe(new CurseInfusion.Recipe()));
				result.add(new QuickRecipe(new MagicalInfusion.Recipe()));
				result.add(new QuickRecipe(new Recycle.Recipe()));
				if (!PixelScene.landscape()) result.add(null);
				result.add(null);
				result.add(new QuickRecipe(new ReclaimTrap.Recipe()));
				result.add(new QuickRecipe(new SummonElemental.Recipe()));
				result.add(new QuickRecipe(new BeaconOfReturning.Recipe()));
				//神谕代行者：E.G.O武器分解/重构（示例为二阶配方）
				result.add(new QuickRecipe(new Enkephalin.DecomposeRecipe(),
						new ArrayList<>(Arrays.asList(new RedEye())),
						new Enkephalin().quantity(5)));
				result.add(new QuickRecipe(new Enkephalin.ReconstructRecipe(),
						new ArrayList<>(Arrays.asList(new Enkephalin().quantity(10), new Shortsword())),
						new RedEye()));
				//E.G.O 六阶合成配方（失乐园/薄暝/传火大剑，补全"提取结晶"选卡显示）
				result.add(new QuickRecipe(new ParadiseLost.CraftRecipe(),
						new ArrayList<>(Arrays.asList(new Enkephalin().quantity(30), new Greatsword(), new ScrollOfRemoveCurse())),
						new ParadiseLost()));
				result.add(new QuickRecipe(new DimDusk.CraftRecipe(),
						new ArrayList<>(Arrays.asList(new Enkephalin().quantity(30), new Greatsword(), new ScrollOfRage())),
						new DimDusk()));
				result.add(new QuickRecipe(new FirelinkGreatsword.CraftRecipe(),
						new ArrayList<>(Arrays.asList(new Embers(), new Greatsword(), new ElixirOfDragonsBlood())),
						new FirelinkGreatsword()));
				result.add(new QuickRecipe(new WaxWing.CraftRecipe(),
						new ArrayList<>(Arrays.asList(new BrandWorkshop(), new PotionOfDragonsBreath(), new ScrollOfTransmutation())),
						new WaxWing()));
				//原版神器的强化形态（2026-09-20，2026-09-24 脑啡肽 60 → 30）：原神器 + 30脑啡肽 → 强化神器，12能量
				result.add(new QuickRecipe(new BloodFeastChalice.CraftRecipe(),
						new ArrayList<>(Arrays.asList(new ChaliceOfBlood(), new Enkephalin().quantity(30))),
						new BloodFeastChalice()));
				result.add(new QuickRecipe(new LifelongStew.CraftRecipe(),
						new ArrayList<>(Arrays.asList(new HornOfPlenty(), new Enkephalin().quantity(30))),
						new LifelongStew()));
				result.add(new QuickRecipe(new ChainOfOthers.CraftRecipe(),
						new ArrayList<>(Arrays.asList(new EtherealChains(), new Enkephalin().quantity(30))),
						new ChainOfOthers()));
				result.add(new QuickRecipe(new ChapterNineVerseTwo.CraftRecipe(),
						new ArrayList<>(Arrays.asList(new UnstableSpellbook(), new Enkephalin().quantity(30))),
						new ChapterNineVerseTwo()));
				result.add(new QuickRecipe(new ApproachingDay.CraftRecipe(),
						new ArrayList<>(Arrays.asList(new TalismanOfForesight(), new Enkephalin().quantity(30))),
						new ApproachingDay()));
				return result;
		}
	}
	
}
