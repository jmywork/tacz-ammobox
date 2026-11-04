package com.taczammobox.util;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.init.ModRecipe;
import com.taczammobox.AmmoType;
import com.taczammobox.config.AmmoFactorySettings;
import com.taczammobox.registry.AmmoTypes;
import com.taczammobox.registry.ModItems;
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
 * Ammo groups and their recipes for the assembler.
 *
 * <p>Primary source is TACZ's gun-smith-table recipe type — the same recipes JEI shows as
 * "ammo on the left, ingredients on the right". A generated casing/propellant/primer recipe
 * is added per scanned ammo type as a fallback so every ammo stays craftable even when its
 * gun pack ships no table recipe.</p>
 */
public final class AssemblerRecipes {
    public record IngredientStack(Ingredient ingredient, int count) {
    }

    public record Entry(ResourceLocation id, ItemStack output, List<IngredientStack> inputs,
                        int ticks, boolean generated) {
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
                            inputs.add(new IngredientStack(input.getIngredient(), Math.max(1, input.getCount())));
                        }
                    }
                    if (inputs.isEmpty()) {
                        continue;
                    }
                    byAmmo.computeIfAbsent(ammoId, key -> new ArrayList<>())
                            .add(new Entry(recipe.getId(), output.copy(), List.copyOf(inputs),
                                    AmmoFactorySettings.ticksFor(ammoId), false));
                }
            } catch (Throwable throwable) {
                com.taczammobox.TaczAmmoBoxMod.LOGGER.warn(
                        "[taczammobox] could not enumerate gun-smith-table ammo recipes", throwable);
            }
        }

        for (AmmoType type : AmmoTypes.all()) {
            ResourceLocation ammoId = type.ammoId();
            ItemStack output = AmmoItemBuilder.create().setId(ammoId)
                    .setCount(AmmoFactorySettings.roundsFor(ammoId)).build();
            if (output.isEmpty()) {
                continue;
            }
            List<IngredientStack> inputs = List.of(
                    new IngredientStack(Ingredient.of(ModItems.CASING.get()), 1),
                    new IngredientStack(Ingredient.of(ModItems.POWDER.get()), 1),
                    new IngredientStack(Ingredient.of(ModItems.PRIMER.get()), 1));
            byAmmo.computeIfAbsent(ammoId, key -> new ArrayList<>())
                    .add(new Entry(new ResourceLocation("taczammobox", "generated/" + ammoId.getPath()),
                            output, inputs, AmmoFactorySettings.ticksFor(ammoId), true));
        }

        List<Group> groups = new ArrayList<>();
        for (Map.Entry<ResourceLocation, List<Entry>> entry : byAmmo.entrySet()) {
            List<Entry> recipes = entry.getValue();
            recipes.sort(Comparator.comparing(Entry::generated)
                    .thenComparing(recipe -> recipe.id().toString()));
            ItemStack display = AmmoItemBuilder.create().setId(entry.getKey()).setCount(1).build();
            groups.add(new Group(entry.getKey(), display, List.copyOf(recipes)));
        }
        groups.sort(Comparator.comparing(group -> group.ammoId().toString()));
        return groups;
    }
}
