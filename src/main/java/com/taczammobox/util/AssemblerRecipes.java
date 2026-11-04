package com.taczammobox.util;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.taczammobox.AmmoType;
import com.taczammobox.config.AmmoFactorySettings;
import com.taczammobox.registry.AmmoTypes;
import com.taczammobox.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The list of recipes the ammo assembler can execute.
 *
 * <p>Two sources: every vanilla crafting recipe whose result is a TACZ ammo item (gun packs,
 * datapacks and KubeJS recipes included), plus one generated recipe per scanned ammo type
 * built from casing + propellant + primer. The list is sorted deterministically so the
 * client and server agree on indices.</p>
 */
public final class AssemblerRecipes {
    /** One ingredient requirement: a predicate plus how many are needed. */
    public record IngredientStack(Ingredient ingredient, int count) {
    }

    /** A selectable recipe. */
    public record Entry(ResourceLocation id, ItemStack output, List<IngredientStack> inputs,
                        int ticks, boolean generated) {
    }

    private static List<Entry> cache;

    private AssemblerRecipes() {
    }

    public static void invalidate() {
        cache = null;
    }

    public static List<Entry> all(Level level) {
        if (cache == null) {
            cache = build(level);
        }
        return cache;
    }

    private static List<Entry> build(Level level) {
        List<Entry> entries = new ArrayList<>();
        if (level == null) {
            return entries;
        }
        try {
            var manager = level.getRecipeManager();
            for (CraftingRecipe recipe : manager.getAllRecipesFor(RecipeType.CRAFTING)) {
                ItemStack result = recipe.getResultItem(level.registryAccess());
                if (!(result.getItem() instanceof IAmmo) || result.isEmpty()) {
                    continue;
                }
                List<IngredientStack> inputs = new ArrayList<>();
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (!ingredient.isEmpty()) {
                        inputs.add(new IngredientStack(ingredient, 1));
                    }
                }
                if (inputs.isEmpty()) {
                    continue;
                }
                entries.add(new Entry(recipe.getId(), result.copy(), List.copyOf(inputs),
                        AmmoFactorySettings.ticksFor(ammoIdOf(result)), false));
            }
        } catch (Throwable throwable) {
            com.taczammobox.TaczAmmoBoxMod.LOGGER.warn("[taczammobox] could not enumerate crafting ammo recipes", throwable);
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
            entries.add(new Entry(new ResourceLocation("taczammobox", "generated/" + ammoId.getPath()),
                    output, inputs, AmmoFactorySettings.ticksFor(ammoId), true));
        }

        entries.sort(Comparator.comparing(Entry::generated)
                .thenComparing(entry -> entry.id().toString()));
        return entries;
    }

    private static ResourceLocation ammoIdOf(ItemStack stack) {
        if (stack.getItem() instanceof IAmmo ammo) {
            return ammo.getAmmoId(stack);
        }
        return new ResourceLocation("taczammobox", "unknown");
    }
}
