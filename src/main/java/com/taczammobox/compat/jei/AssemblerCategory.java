package com.taczammobox.compat.jei;

import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.registry.ModItems;
import com.taczammobox.util.AssemblerRecipes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JEI page for the ammo assembler: the gun-smith-table recipe it mirrors, its ingredients
 * and how long one craft takes. Ingredients are shown with their required counts.
 */
public final class AssemblerCategory implements IRecipeCategory<AssemblerCategory.Recipe> {
    public static final RecipeType<Recipe> TYPE =
            RecipeType.create(TaczAmmoBoxMod.MOD_ID, "ammo_assembler", Recipe.class);

    private static final int COLUMNS = 4;
    private static final int MAX_SLOTS = 12;

    private final IDrawable icon;
    private final IDrawable arrow;

    public AssemblerCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(ModItems.ASSEMBLER.get()));
        this.arrow = helper.getRecipeArrow();
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.taczammobox.ammo_assembler");
    }

    @Override
    public int getWidth() {
        return 156;
    }

    @Override
    public int getHeight() {
        return 56;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focusGroup) {
        int shown = Math.min(recipe.inputs().size(), MAX_SLOTS);
        for (int index = 0; index < shown; index++) {
            AssemblerRecipes.IngredientStack input = recipe.inputs().get(index);
            int x = 1 + (index % COLUMNS) * 18;
            int y = 1 + (index / COLUMNS) * 18;
            builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                    .addItemStacks(withCount(input.ingredient(), input.count()));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 98, 19).addItemStack(recipe.output());
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 78, 19);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("jei.taczammobox.assembler.time",
                String.format(Locale.ROOT, "%.1f", recipe.ticks() / 20.0)), 122, 23, 0x404040, false);
    }

    private static List<ItemStack> withCount(Ingredient ingredient, int count) {
        ItemStack[] items = ingredient.getItems();
        List<ItemStack> stacks = new ArrayList<>(items.length);
        for (ItemStack stack : items) {
            stacks.add(stack.copyWithCount(Math.max(1, count)));
        }
        return stacks;
    }

    public record Recipe(ResourceLocation id, ItemStack output,
                         List<AssemblerRecipes.IngredientStack> inputs, int ticks) {
    }

    public static List<Recipe> recipes() {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return List.of();
        }
        List<Recipe> recipes = new ArrayList<>();
        try {
            for (AssemblerRecipes.Group group : AssemblerRecipes.collect(level)) {
                for (AssemblerRecipes.Entry entry : group.recipes()) {
                    recipes.add(new Recipe(entry.id(), entry.output(), entry.inputs(), entry.ticks()));
                }
            }
        } catch (Throwable throwable) {
            TaczAmmoBoxMod.LOGGER.warn("[taczammobox] could not build assembler JEI pages", throwable);
        }
        return recipes;
    }
}
