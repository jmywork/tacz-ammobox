package com.taczammobox.network;

import com.taczammobox.block.AmmoAssemblerBlockEntity;
import com.taczammobox.menu.AmmoAssemblerMenu;
import com.taczammobox.util.AssemblerRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.RangedWrapper;
import net.minecraftforge.items.wrapper.InvWrapper;

import java.util.List;

/**
 * JEI recipe transfer: the client asks the server to move the ingredients of one assembler
 * recipe out of the player's inventory and into the machine. Never trusts the client with
 * item movement - the client only names the recipe.
 */
public record AssemblerTransferPacket(BlockPos pos, ResourceLocation recipeId, boolean maxTransfer) {

    public static void encode(AssemblerTransferPacket message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeResourceLocation(message.recipeId);
        buffer.writeBoolean(message.maxTransfer);
    }

    public static AssemblerTransferPacket decode(FriendlyByteBuf buffer) {
        return new AssemblerTransferPacket(buffer.readBlockPos(), buffer.readResourceLocation(), buffer.readBoolean());
    }

    public void handle(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (!(player.containerMenu instanceof AmmoAssemblerMenu menu) || !menu.getPos().equals(pos)) {
            return;
        }
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return;
        }
        if (!(player.level().getBlockEntity(pos) instanceof AmmoAssemblerBlockEntity assembler)) {
            return;
        }

        List<AssemblerRecipes.Group> groups = assembler.groups();
        int groupIndex = -1;
        int recipeIndex = -1;
        AssemblerRecipes.Entry entry = null;
        for (int group = 0; group < groups.size() && entry == null; group++) {
            List<AssemblerRecipes.Entry> recipes = groups.get(group).recipes();
            for (int index = 0; index < recipes.size(); index++) {
                if (recipes.get(index).id().equals(recipeId)) {
                    groupIndex = group;
                    recipeIndex = index;
                    entry = recipes.get(index);
                    break;
                }
            }
        }
        if (entry == null) {
            return;
        }

        IItemHandler inputs = new RangedWrapper(assembler.getInventory(), 0, AmmoAssemblerBlockEntity.INPUT_SLOTS);
        IItemHandler playerInv = new InvWrapper(player.getInventory());
        int moved = 0;
        for (AssemblerRecipes.IngredientStack request : entry.inputs()) {
            int target = maxTransfer ? 64 : Math.max(1, request.count());
            int need = target - count(inputs, request.ingredient());
            if (need <= 0) {
                continue;
            }
            moved += pull(playerInv, inputs, request.ingredient(), need);
        }

        if (moved > 0) {
            if (!assembler.isLocked()) {
                assembler.selectAmmo(groupIndex);
                assembler.selectRecipe(recipeIndex);
            }
            assembler.setChanged();
        }
        player.containerMenu.broadcastChanges();
        player.displayClientMessage(Component.translatable(moved > 0
                ? "message.taczammobox.assembler.transfer_done"
                : "message.taczammobox.assembler.transfer_empty"), true);
    }

    private static int count(IItemHandler handler, Ingredient ingredient) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty() && ingredient.test(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static int pull(IItemHandler playerInv, IItemHandler inputs, Ingredient ingredient, int wanted) {
        int moved = 0;
        for (int slot = 0; slot < playerInv.getSlots() && moved < wanted; slot++) {
            ItemStack stack = playerInv.getStackInSlot(slot);
            if (stack.isEmpty() || !ingredient.test(stack)) {
                continue;
            }
            int take = Math.min(wanted - moved, stack.getCount());
            ItemStack probe = ItemHandlerHelper.insertItemStacked(inputs, stack.copyWithCount(take), true);
            int accepted = take - probe.getCount();
            if (accepted <= 0) {
                continue;
            }
            ItemHandlerHelper.insertItemStacked(inputs, stack.copyWithCount(accepted), false);
            playerInv.extractItem(slot, accepted, false);
            moved += accepted;
        }
        return moved;
    }
}
