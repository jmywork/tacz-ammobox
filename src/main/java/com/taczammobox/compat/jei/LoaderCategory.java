package com.taczammobox.compat.jei;

import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.config.sync.SyncConfig;
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
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class LoaderCategory implements IRecipeCategory<LoaderCategory.Recipe> {
    public static final RecipeType<Recipe> TYPE = RecipeType.create("taczammobox", "ammo_loader", Recipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;

    public LoaderCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(160, 54);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModItems.LOADER.get()));
        this.arrow = helper.getRecipeArrow();
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.taczammobox.ammo_loader");
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
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 19).addItemStack(recipe.source());
        builder.addSlot(RecipeIngredientRole.INPUT, 31, 19).addItemStack(recipe.box());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 139, 19).addItemStack(recipe.output());
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 65, 19);
    }

    public record Recipe(ItemStack source, ItemStack box, ItemStack output) {
    }

    public static List<Recipe> recipes() {
        List<Recipe> recipes = new ArrayList<>();
        ItemStack emptyBox = new ItemStack(com.tacz.guns.init.ModItems.AMMO_BOX.get());
        for (AmmoType type : AmmoTypes.all()) {
            int rounds = AmmoBoxHelper.roundsPerBox(type);
            ItemStack loose = AmmoItemBuilder.create()
                    .setId(type.ammoId())
                    .setCount(rounds)
                    .build();
            ItemStack output = emptyBox.copy();
            if (!(output.getItem() instanceof IAmmoBox box)) {
                continue;
            }
            box.setAmmoId(output, type.ammoId());
            box.setAmmoCount(output, rounds * SyncConfig.AMMO_BOX_STACK_SIZE.get());
            recipes.add(new Recipe(loose, emptyBox.copy(), output));
        }
        return recipes;
    }
}
