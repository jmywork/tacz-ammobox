package com.taczammobox.compat.jei;

import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.taczammobox.AmmoType;
import com.taczammobox.registry.AmmoTypes;
import com.taczammobox.registry.ModItems;
import com.taczammobox.util.AmmoBoxHelper;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class PackerCategory implements IRecipeCategory<PackerCategory.Recipe> {
    public static final RecipeType<Recipe> TYPE = RecipeType.create("taczammobox", "ammo_packer", Recipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public PackerCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(130, 54);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModItems.PACKER.get()));
        this.arrow = helper.getRecipeArrow();
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.taczammobox.ammo_packer");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focusGroup) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 19).addItemStack(recipe.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 109, 19).addItemStack(recipe.output());
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 52, 19);
    }

    public record Recipe(ItemStack input, ItemStack output) {
    }

    public static List<Recipe> recipes() {
        List<Recipe> recipes = new ArrayList<>();
        for (AmmoType type : AmmoTypes.all()) {
            Item boxItem = ModItems.box(type.ammoId());
            Item crateItem = ModItems.crate(type.ammoId());
            if (boxItem == null || crateItem == null) {
                continue;
            }
            int rounds = AmmoBoxHelper.roundsPerBox(type);
            ItemStack loose = AmmoItemBuilder.create()
                    .setId(type.ammoId())
                    .setCount(rounds)
                    .build();
            ItemStack box = new ItemStack(boxItem);
            ItemStack boxes = new ItemStack(boxItem, 9);
            ItemStack crate = new ItemStack(crateItem);

            recipes.add(new Recipe(loose, box));
            recipes.add(new Recipe(boxes, crate));
            recipes.add(new Recipe(crate, boxes));
            recipes.add(new Recipe(box, loose));
        }
        return recipes;
    }
}
