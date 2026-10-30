package com.taczammobox.compat;

import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.taczammobox.AmmoType;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.registry.AmmoTypes;
import com.taczammobox.registry.ModItems;
import com.taczammobox.util.AmmoBoxHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adds the standard box/crate recipes for ammo types registered through the
 * config. Built-in types already ship JSON recipes; third-party types cannot,
 * so their recipes are injected into the recipe manager after each reload.
 */
@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID)
public final class ThirdPartyRecipes {
    private ThirdPartyRecipes() {
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void value, ResourceManager resourceManager, ProfilerFiller profiler) {
                try {
                    inject(event.getServerResources().getRecipeManager());
                } catch (Exception exception) {
                    TaczAmmoBoxMod.LOGGER.error("Third-party recipe injection failed; generated data pack remains the fallback",
                            exception);
                }
            }
        });
    }

    private static void inject(RecipeManager manager) {
        List<Recipe<?>> all = new ArrayList<>(manager.getRecipes());
        if (all.size() < 50) {
            // The recipe manager has not applied datapack recipes yet; do not
            // replace it with our short generated list.
            TaczAmmoBoxMod.LOGGER.warn("Recipe manager not ready, skipping third-party recipe injection");
            return;
        }
        Set<ResourceLocation> ids = new HashSet<>();
        for (Recipe<?> recipe : all) {
            ids.add(recipe.getId());
        }

        int added = 0;
        for (AmmoType type : AmmoTypes.all()) {
            if (AmmoTypes.isBuiltin(type.ammoId())) {
                continue;
            }
            try {
                Item box = ModItems.box(type.ammoId());
                Item crate = ModItems.crate(type.ammoId());
                if (box == null || crate == null) {
                    TaczAmmoBoxMod.LOGGER.warn("Skipping runtime recipes for {}: missing box/crate item", type.ammoId());
                    continue;
                }

                ResourceLocation crateId = new ResourceLocation(TaczAmmoBoxMod.MOD_ID,
                        "generated/" + type.itemName() + "_crate");
                if (!ids.contains(crateId)) {
                    NonNullList<Ingredient> ingredients = NonNullList.create();
                    for (int i = 0; i < 9; i++) {
                        ingredients.add(Ingredient.of(box));
                    }
                    all.add(new ShapelessRecipe(crateId, "taczammobox", CraftingBookCategory.MISC,
                            new ItemStack(crate), ingredients));
                    ids.add(crateId);
                    added++;
                }

                ResourceLocation boxesId = new ResourceLocation(TaczAmmoBoxMod.MOD_ID,
                        "generated/" + type.itemName() + "_boxes");
                if (!ids.contains(boxesId)) {
                    NonNullList<Ingredient> ingredients = NonNullList.create();
                    ingredients.add(Ingredient.of(crate));
                    all.add(new ShapelessRecipe(boxesId, "taczammobox", CraftingBookCategory.MISC,
                            new ItemStack(box, 9), ingredients));
                    ids.add(boxesId);
                    added++;
                }

                ResourceLocation ammoId = new ResourceLocation(TaczAmmoBoxMod.MOD_ID,
                        "generated/" + type.itemName() + "_ammo");
                if (!ids.contains(ammoId)) {
                    NonNullList<Ingredient> ingredients = NonNullList.create();
                    ingredients.add(Ingredient.of(box));
                    ItemStack ammo = AmmoItemBuilder.create()
                            .setId(type.ammoId())
                            .setCount(AmmoBoxHelper.roundsPerBox(type))
                            .build();
                    all.add(new ShapelessRecipe(ammoId, "taczammobox", CraftingBookCategory.MISC,
                            ammo, ingredients));
                    ids.add(ammoId);
                    added++;
                }
            } catch (RuntimeException exception) {
                TaczAmmoBoxMod.LOGGER.error("Failed to build runtime recipes for {}", type.ammoId(), exception);
            }
        }

        if (added > 0) {
            try {
                manager.replaceRecipes(all);
                TaczAmmoBoxMod.LOGGER.info("Injected {} recipes for third-party ammo types", added);
            } catch (RuntimeException exception) {
                TaczAmmoBoxMod.LOGGER.error("Could not replace recipe manager; generated data pack remains the fallback",
                        exception);
            }
        }
    }
}
