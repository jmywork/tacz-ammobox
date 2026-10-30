package com.taczammobox.menu;

import com.taczammobox.block.AmmoPackerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class AmmoPackerMenu extends AbstractContainerMenu {
    private final AmmoPackerBlockEntity packer;
    private final ContainerData data;

    public AmmoPackerMenu(int id, Inventory inventory, AmmoPackerBlockEntity packer) {
        super(com.taczammobox.registry.ModMenus.PACKER.get(), id);
        this.packer = packer;
        this.data = packer.getData();
        addDataSlots(data);

        ItemStackHandler handler = packer.getInventory();
        addSlot(new SlotItemHandler(handler, AmmoPackerBlockEntity.SLOT_INPUT, 56, 35));
        addSlot(new SlotItemHandler(handler, AmmoPackerBlockEntity.SLOT_OUTPUT, 116, 35) {
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

    public AmmoPackerMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, (AmmoPackerBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()));
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getMode() {
        return data.get(1);
    }

    public AmmoPackerBlockEntity.SideMode getSide(int index) {
        return AmmoPackerBlockEntity.SideMode.values()[data.get(2 + index)];
    }

    public AmmoPackerBlockEntity.SideMode getSide(net.minecraft.core.Direction direction) {
        return getSide(direction.ordinal());
    }

    public net.minecraft.core.Direction getFacing() {
        return packer.getBlockState().getValue(com.taczammobox.block.AmmoPackerBlock.FACING);
    }

    public AmmoPackerBlockEntity.RedstoneMode getRedstoneMode() {
        return AmmoPackerBlockEntity.RedstoneMode.values()[data.get(8)];
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            packer.toggleMode();
            return true;
        }
        if (id >= 1 && id <= 6) {
            packer.cycleSide(id - 1);
            return true;
        }
        if (id == 7) {
            packer.cycleRedstone();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(stack, 0, 1, false)) {
                if (index < 29) {
                    if (!moveItemStackTo(stack, 29, 38, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(stack, 2, 29, false)) {
                    return ItemStack.EMPTY;
                }
            }
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
        BlockPos pos = packer.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
