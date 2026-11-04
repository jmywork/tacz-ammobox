package com.taczammobox.menu;

import com.tacz.guns.api.item.IAmmo;
import com.taczammobox.block.AmmoAssemblerBlockEntity;
import com.taczammobox.item.PackagedAmmo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class AmmoAssemblerMenu extends AbstractContainerMenu {
    private final AmmoAssemblerBlockEntity assembler;
    private final ContainerData data;

    public AmmoAssemblerMenu(int id, Inventory inventory, AmmoAssemblerBlockEntity assembler,
                             ContainerData data) {
        super(com.taczammobox.registry.ModMenus.ASSEMBLER.get(), id);
        this.assembler = assembler;
        this.data = data;
        addDataSlots(data);

        ItemStackHandler handler = assembler.getInventory();
        addSlot(new SlotItemHandler(handler, AmmoAssemblerBlockEntity.SLOT_CASING, 26, 17));
        addSlot(new SlotItemHandler(handler, AmmoAssemblerBlockEntity.SLOT_POWDER, 44, 17));
        addSlot(new SlotItemHandler(handler, AmmoAssemblerBlockEntity.SLOT_PRIMER, 62, 17));
        addSlot(new SlotItemHandler(handler, AmmoAssemblerBlockEntity.SLOT_TEMPLATE, 26, 41));
        addSlot(new SlotItemHandler(handler, AmmoAssemblerBlockEntity.SLOT_OUTPUT, 116, 29) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public AmmoAssemblerMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, readAssembler(inventory, buffer.readBlockPos()));
    }

    private AmmoAssemblerMenu(int id, Inventory inventory, AmmoAssemblerBlockEntity assembler) {
        this(id, inventory, assembler, assembler.getData());
    }

    private static AmmoAssemblerBlockEntity readAssembler(Inventory inventory, net.minecraft.core.BlockPos pos) {
        if (inventory.player.level().getBlockEntity(pos) instanceof AmmoAssemblerBlockEntity assembler) {
            return assembler;
        }
        throw new IllegalStateException("Missing ammo assembler at " + pos);
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getMaxProgress() {
        return Math.max(1, data.get(1));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < 5) {
            if (!moveItemStackTo(stack, 5, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(com.taczammobox.registry.ModItems.CASING.get())) {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(com.taczammobox.registry.ModItems.POWDER.get())) {
            if (!moveItemStackTo(stack, 1, 2, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(com.taczammobox.registry.ModItems.PRIMER.get())) {
            if (!moveItemStackTo(stack, 2, 3, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof IAmmo || stack.getItem() instanceof PackagedAmmo) {
            if (!moveItemStackTo(stack, 3, 4, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        var pos = assembler.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
