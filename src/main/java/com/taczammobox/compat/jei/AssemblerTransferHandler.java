package com.taczammobox.compat.jei;

import com.taczammobox.block.AmmoAssemblerBlockEntity;
import com.taczammobox.menu.AmmoAssemblerMenu;
import com.taczammobox.network.ModNetwork;
import com.taczammobox.util.AssemblerRecipes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferContext;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Lets JEI push the ingredients of an assembler recipe into the open machine.
 * The client only checks availability; the server performs the item move.
 */
public final class AssemblerTransferHandler
        implements IRecipeTransferHandler<AmmoAssemblerMenu, AssemblerCategory.Recipe> {

    private final IRecipeTransferHandlerHelper helper;

    public AssemblerTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @Override
    public Class<? extends AmmoAssemblerMenu> getContainerClass() {
        return AmmoAssemblerMenu.class;
    }

    @Override
    public Optional<MenuType<AmmoAssemblerMenu>> getMenuType() {
        return Optional.of(com.taczammobox.registry.ModMenus.ASSEMBLER.get());
    }

    @Override
    public RecipeType<AssemblerCategory.Recipe> getRecipeType() {
        return AssemblerCategory.TYPE;
    }

    @Override
    public IRecipeTransferError transferRecipe(IRecipeTransferContext<AssemblerCategory.Recipe, AmmoAssemblerMenu> context,
                                               boolean doTransfer) {
        return attempt(context.getContainer(), context.getRecipe(), context.getPlayer(),
                context.isMaxTransfer(), doTransfer);
    }

    @Override
    @SuppressWarnings("removal")
    public IRecipeTransferError transferRecipe(AmmoAssemblerMenu container, AssemblerCategory.Recipe recipe,
                                               IRecipeSlotsView slots, Player player,
                                               boolean maxTransfer, boolean doTransfer) {
        return attempt(container, recipe, player, maxTransfer, doTransfer);
    }

    @Nullable
    private IRecipeTransferError attempt(AmmoAssemblerMenu container, AssemblerCategory.Recipe recipe,
                                         Player player, boolean maxTransfer, boolean doTransfer) {
        if (container.isLocked() && (container.selectedEntry() == null
                || !container.selectedEntry().id().equals(recipe.id()))) {
            return helper.createUserErrorWithTooltip(
                    Component.translatable("jei.taczammobox.assembler.locked"));
        }

        for (AssemblerRecipes.IngredientStack request : recipe.inputs()) {
            int target = maxTransfer ? 64 : Math.max(1, request.count());
            if (countIn(player, request.ingredient()) + countIn(container, request.ingredient()) < target) {
                return helper.createUserErrorWithTooltip(Component.translatable(
                        "jei.taczammobox.assembler.missing", nameOf(request.ingredient())));
            }
        }

        if (doTransfer) {
            ModNetwork.sendAssemblerTransfer(container.getPos(), recipe.id(), maxTransfer);
        }
        return null;
    }

    private static int countIn(Player player, Ingredient ingredient) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && ingredient.test(stack)) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty() && ingredient.test(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static int countIn(AmmoAssemblerMenu container, Ingredient ingredient) {
        int total = 0;
        for (int slot = 0; slot < Math.min(AmmoAssemblerBlockEntity.INPUT_SLOTS, container.slots.size()); slot++) {
            ItemStack stack = container.slots.get(slot).getItem();
            if (!stack.isEmpty() && ingredient.test(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static Component nameOf(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        return items.length == 0 ? Component.empty() : items[0].getHoverName();
    }
}
