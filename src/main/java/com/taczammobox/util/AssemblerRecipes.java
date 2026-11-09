package com.taczammobox.util;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModRecipe;
import com.taczammobox.config.AmmoFactorySettings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ammo groups and their gun-smith-table recipes for the assembler.
 *
 * <p>The assembler only mirrors recipes TACZ already ships: it never adds new items or
 * recipes. Entries are grouped by ammo id and sorted deterministically so client and server
 * agree on selection indices.</p>
 */
public final class AssemblerRecipes {
    public record IngredientStack(Ingredient ingredient, int count) {
    }

    public record Entry(ResourceLocation id, ItemStack output, List<IngredientStack> inputs, int ticks) {
    }

    public record Group(ResourceLocation ammoId, ItemStack display, List<Entry> recipes) {
    }

    private static List<Group> cache;

    private AssemblerRecipes() {
    }

    public static void invalidate() {
        cache = null;
    }

    public static List<Group> groups(Level level) {
        if (cache == null) {
            cache = build(level);
        }
        return cache;
    }

    /** Uncached snapshot: JEI runs on the client and must not share the server-side cache. */
    public static List<Group> collect(Level level) {
        return build(level);
    }

    private static List<Group> build(Level level) {
        Map<ResourceLocation, List<Entry>> byAmmo = new LinkedHashMap<>();
        if (level != null) {
            try {
                var manager = level.getRecipeManager();
                for (GunSmithTableRecipe recipe : manager.getAllRecipesFor(
                        ModRecipe.GUN_SMITH_TABLE_CRAFTING.get())) {
                    ItemStack output = recipe.getOutput();
                    if (output.isEmpty() || !(output.getItem() instanceof IAmmo ammo)) {
                        continue;
                    }
                    ResourceLocation ammoId = ammo.getAmmoId(output);
                    List<IngredientStack> inputs = new ArrayList<>();
                    for (GunSmithTableIngredient input : recipe.getInputs()) {
                        if (input.getIngredient() != null && !input.getIngredient().isEmpty()) {
                            inputs.add(new IngredientStack(input.getIngredient(),
                                    Math.max(1, input.getCount())));
                        }
                    }
                    if (inputs.isEmpty()) {
                        continue;
                    }
                    byAmmo.computeIfAbsent(ammoId, key -> new ArrayList<>())
                            .add(new Entry(recipe.getId(), output.copy(), List.copyOf(inputs),
                                    AmmoFactorySettings.ticksFor(ammoId)));
                }
            } catch (Throwable throwable) {
                com.taczammobox.TaczAmmoBoxMod.LOGGER.warn(
                        "[taczammobox] could not enumerate gun-smith-table ammo recipes", throwable);
            }
        }

        List<Group> groups = new ArrayList<>();
        for (Map.Entry<ResourceLocation, List<Entry>> entry : byAmmo.entrySet()) {
            List<Entry> recipes = new ArrayList<>(entry.getValue());
            recipes.sort(Comparator.comparing(recipe -> recipe.id().toString()));
            ItemStack display = recipes.isEmpty() ? ItemStack.EMPTY : recipes.get(0).output().copyWithCount(1);
            groups.add(new Group(entry.getKey(), display, List.copyOf(recipes)));
        }
        groups.sort(Comparator.comparing(group -> group.ammoId().toString()));
        return groups;
    }
}
