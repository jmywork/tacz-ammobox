package com.taczammobox.menu;

import com.taczammobox.block.AmmoAssemblerBlockEntity;
import com.taczammobox.util.AssemblerRecipes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;
import java.util.List;

public class AmmoAssemblerMenu extends AbstractContainerMenu {
    public static final int GRID_X = 8;
    public static final int GRID_Y = 104;
    public static final int RECIPE_BUTTON_OFFSET = 1000;

    private final AmmoAssemblerBlockEntity assembler;
    private final ContainerData data;

    public AmmoAssemblerMenu(int id, Inventory inventory, AmmoAssemblerBlockEntity assembler,
                             ContainerData data) {
        super(com.taczammobox.registry.ModMenus.ASSEMBLER.get(), id);
        this.assembler = assembler;
        this.data = data;
        addDataSlots(data);

        ItemStackHandler handler = assembler.getInventory();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotItemHandler(handler, col + row * 3,
                        GRID_X + col * 18, GRID_Y + row * 18));
            }
        }
        addSlot(new SlotItemHandler(handler, AmmoAssemblerBlockEntity.SLOT_OUTPUT, 78, 122) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 168 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 226));
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

    public List<AssemblerRecipes.Group> groups() {
        return assembler.groups();
    }

    public int getSelectedAmmo() {
        return data.get(0);
    }

    public int getSelectedRecipe() {
        return data.get(1);
    }

    @Nullable
    public AssemblerRecipes.Group selectedGroup() {
        List<AssemblerRecipes.Group> groups = groups();
        int index = getSelectedAmmo();
        return index >= 0 && index < groups.size() ? groups.get(index) : null;
    }

    @Nullable
    public AssemblerRecipes.Entry selectedEntry() {
        AssemblerRecipes.Group group = selectedGroup();
        if (group == null) {
            return null;
        }
        int index = getSelectedRecipe();
        return index >= 0 && index < group.recipes().size() ? group.recipes().get(index) : null;
    }

    public int getProgress() {
        return data.get(2);
    }

    public int getMaxProgress() {
        AssemblerRecipes.Entry entry = selectedEntry();
        return entry == null ? 1 : Math.max(1, entry.ticks());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= RECIPE_BUTTON_OFFSET) {
            assembler.selectRecipe(id - RECIPE_BUTTON_OFFSET);
        } else {
            assembler.selectAmmo(id);
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < 10) {
            if (!moveItemStackTo(stack, 10, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 9, false)) {
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
