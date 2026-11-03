package com.taczammobox.compat.jei;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.registry.ModItems;
import com.taczammobox.util.AutoFireTier;
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

import java.util.ArrayList;
import java.util.List;

/** JEI page: which gun a tier magazine fits, and what it does to capacity / fire rate. */
public final class AutoFireCategory implements IRecipeCategory<AutoFireCategory.Recipe> {
    public static final RecipeType<Recipe> TYPE = RecipeType.create("taczammobox", "auto_fire", Recipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public AutoFireCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(150, 44);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModItems.AMMO_BELT.get()));
    }

    @Override
    public RecipeType<Recipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.taczammobox.auto_fire");
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
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 14).addItemStack(recipe.gun());
        builder.addSlot(RecipeIngredientRole.INPUT, 23, 14).addItemStack(recipe.magazine());
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, Component.translatable("jei.taczammobox.auto_fire.capacity", recipe.capacity()),
                48, 10, 0x404040, false);
        graphics.drawString(font, Component.translatable("jei.taczammobox.auto_fire.rpm", recipe.rpm()),
                48, 26, 0x404040, false);
    }

    public record Recipe(ItemStack gun, ItemStack magazine, int capacity, int rpm) {
    }

    public static List<Recipe> recipes() {
        List<Recipe> recipes = new ArrayList<>();
        ItemStack gun = sampleBoltGun();
        if (gun.isEmpty()) {
            return recipes;
        }
        for (AutoFireTier tier : AutoFireTier.all()) {
            ItemStack magazine = AttachmentItemBuilder.create().setId(tier.id()).build();
            if (magazine.isEmpty()) {
                continue;
            }
            recipes.add(new Recipe(gun, magazine, tier.capacity(), tier.rpm()));
        }
        return recipes;
    }

    /** First loaded manual-action gun, so JEI shows a real item instead of a placeholder. */
    private static ItemStack sampleBoltGun() {
        try {
            for (var entry : TimelessAPI.getAllCommonGunIndex()) {
                ResourceLocation id = entry.getKey();
                if (entry.getValue().getGunData().getBolt() == Bolt.MANUAL_ACTION) {
                    return GunItemBuilder.create().setId(id).build();
                }
            }
        } catch (Throwable throwable) {
            TaczAmmoBoxMod.LOGGER.warn("[taczammobox] could not pick a sample bolt gun for JEI", throwable);
        }
        return ItemStack.EMPTY;
    }
}
