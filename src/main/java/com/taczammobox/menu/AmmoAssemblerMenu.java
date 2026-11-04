package com.taczammobox.menu;

import com.taczammobox.block.AmmoAssemblerBlockEntity;
import com.taczammobox.util.AssemblerRecipes;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;
import java.util.List;

/** No machine slots: inputs and outputs are handled by automation, the GUI only selects. */
public class AmmoAssemblerMenu extends AbstractContainerMenu {
    public static final int AMMO_BUTTON_OFFSET = 1000;
    public static final int RECIPE_BUTTON_OFFSET = 100000;
    public static final int BUTTON_REDSTONE = 7;
    public static final int BUTTON_LOCK = 8;

    private final AmmoAssemblerBlockEntity assembler;
    private final ContainerData data;

    public AmmoAssemblerMenu(int id, Inventory inventory, AmmoAssemblerBlockEntity assembler,
                             ContainerData data) {
        super(com.taczammobox.registry.ModMenus.ASSEMBLER.get(), id);
        this.assembler = assembler;
        this.data = data;
        addDataSlots(data);

        // nine hidden read-only slots so the client sees the machine's input contents
        var machineInventory = assembler.getInventory();
        for (int slot = 0; slot < AmmoAssemblerBlockEntity.INPUT_SLOTS; slot++) {
            addSlot(new SlotItemHandler(machineInventory, slot, -2000, -2000) {
                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 200));
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

    public boolean isLocked() {
        return data.get(3) != 0;
    }

    public net.minecraft.core.Direction getFacing() {
        return assembler.getBlockState().getValue(com.taczammobox.block.AmmoAssemblerBlock.FACING);
    }

    public int countAvailable(Ingredient ingredient) {
        int found = 0;
        for (int slot = 0; slot < AmmoAssemblerBlockEntity.INPUT_SLOTS; slot++) {
            ItemStack stack = slots.get(slot).getItem();
            if (!stack.isEmpty() && ingredient.test(stack)) {
                found += stack.getCount();
            }
        }
        return found;
    }

    public AmmoAssemblerBlockEntity.SideMode getSide(Direction direction) {
        return AmmoAssemblerBlockEntity.SideMode.values()[data.get(4 + direction.ordinal())];
    }

    public AmmoAssemblerBlockEntity.RedstoneMode getRedstoneMode() {
        return AmmoAssemblerBlockEntity.RedstoneMode.values()[data.get(10)];
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
        } else if (id >= AMMO_BUTTON_OFFSET) {
            assembler.selectAmmo(id - AMMO_BUTTON_OFFSET);
        } else if (id == BUTTON_LOCK) {
            assembler.toggleLock();
        } else if (id == BUTTON_REDSTONE) {
            assembler.cycleRedstone();
        } else if (id >= 1 && id <= 6) {
            assembler.cycleSide(Direction.values()[id - 1]);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        var pos = assembler.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
