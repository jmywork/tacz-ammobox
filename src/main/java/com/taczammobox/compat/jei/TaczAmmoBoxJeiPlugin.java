package com.taczammobox.compat.jei;

import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class TaczAmmoBoxJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID =
            new ResourceLocation(TaczAmmoBoxMod.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new PackerCategory(helper), new LoaderCategory(helper),
                new AutoFireCategory(helper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(PackerCategory.TYPE, PackerCategory.recipes());
        registration.addRecipes(LoaderCategory.TYPE, LoaderCategory.recipes());
        if (com.taczammobox.config.FeatureFlags.autoFire()) {
            try {
                registration.addRecipes(AutoFireCategory.TYPE, AutoFireCategory.recipes());
            } catch (Throwable throwable) {
                TaczAmmoBoxMod.LOGGER.warn("[taczammobox] auto-fire JEI page disabled", throwable);
            }
        }
        registration.addIngredientInfo(ModItems.AMMO_CHAIN.get(),
                net.minecraft.network.chat.Component.translatable("jei.taczammobox.ammo_chain.info1"),
                net.minecraft.network.chat.Component.translatable("jei.taczammobox.ammo_chain.info2"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(PackerCategory.TYPE, ModItems.PACKER.get());
        registration.addRecipeCatalysts(LoaderCategory.TYPE, ModItems.LOADER.get());
    }
}
