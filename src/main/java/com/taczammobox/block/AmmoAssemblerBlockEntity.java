package com.taczammobox.block;

import com.taczammobox.menu.AmmoAssemblerMenu;
import com.taczammobox.util.AssemblerRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Recipe-driven ammo assembler with no player crafting slots.
 *
 * <p>Automation feeds the nine input slots from the sides marked INPUT; the machine matches
 * the selected gun-smith-table recipe, consumes its ingredients and pushes the result into
 * the three output slots, which are extracted or pushed out of the OUTPUT faces. A lock
 * switch freezes the current ammo/recipe selection.</p>
 */
public class AmmoAssemblerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_SLOTS = 3;
    public static final int SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;

    public enum SideMode {
        INPUT,
        OUTPUT,
        DISABLED
    }

    public enum RedstoneMode {
        IGNORE,
        HIGH,
        LOW
    }

    private int selectedAmmo = -1;
    private int selectedRecipe = -1;
    private int progress;
    private boolean locked;

    private final SideMode[] sides = {SideMode.OUTPUT, SideMode.INPUT, SideMode.INPUT,
            SideMode.INPUT, SideMode.INPUT, SideMode.INPUT};
    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final IItemHandler inputView = new RangedWrapper(inventory, 0, INPUT_SLOTS) {
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
    };

    private final IItemHandler outputView = new RangedWrapper(inventory, INPUT_SLOTS, SLOTS) {
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }
    };

    private final LazyOptional<IItemHandler> inputOptional = LazyOptional.of(() -> inputView);
    private final LazyOptional<IItemHandler> outputOptional = LazyOptional.of(() -> outputView);

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> selectedAmmo;
                case 1 -> selectedRecipe;
                case 2 -> progress;
                case 3 -> locked ? 1 : 0;
                case 4, 5, 6, 7, 8, 9 -> sides[index - 4].ordinal();
                case 10 -> redstoneMode.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> selectedAmmo = value;
                case 1 -> selectedRecipe = value;
                case 2 -> progress = Math.max(0, value);
                case 3 -> locked = value != 0;
                case 4, 5, 6, 7, 8, 9 -> sides[index - 4] =
                        SideMode.values()[Math.floorMod(value, SideMode.values().length)];
                case 10 -> redstoneMode =
                        RedstoneMode.values()[Math.floorMod(value, RedstoneMode.values().length)];
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 11;
        }
    };

    public AmmoAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(com.taczammobox.registry.ModBlockEntities.ASSEMBLER.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public ContainerData getData() {
        return data;
    }

    public List<AssemblerRecipes.Group> groups() {
        return level == null ? List.of() : AssemblerRecipes.groups(level);
    }

    public boolean isLocked() {
        return locked;
    }

    public void cycleSide(Direction direction) {
        int index = direction.ordinal();
        sides[index] = SideMode.values()[(sides[index].ordinal() + 1) % SideMode.values().length];
        setChanged();
    }

    public void cycleRedstone() {
        redstoneMode = RedstoneMode.values()[(redstoneMode.ordinal() + 1) % RedstoneMode.values().length];
        setChanged();
    }

    public void toggleLock() {
        locked = !locked;
        setChanged();
    }

    public void selectAmmo(int index) {
        if (locked) {
            return;
        }
        List<AssemblerRecipes.Group> groups = groups();
        selectedAmmo = index >= 0 && index < groups.size() ? index : -1;
        selectedRecipe = -1;
        progress = 0;
        setChanged();
    }

    public void selectRecipe(int index) {
        if (locked) {
            return;
        }
        AssemblerRecipes.Group group = selectedGroup();
        if (group == null) {
            return;
        }
        selectedRecipe = index >= 0 && index < group.recipes().size() ? index : -1;
        progress = 0;
        setChanged();
    }

    @Nullable
    public AssemblerRecipes.Group selectedGroup() {
        List<AssemblerRecipes.Group> groups = groups();
        return selectedAmmo >= 0 && selectedAmmo < groups.size() ? groups.get(selectedAmmo) : null;
    }

    @Nullable
    public AssemblerRecipes.Entry selectedEntry() {
        AssemblerRecipes.Group group = selectedGroup();
        if (group == null || selectedRecipe < 0 || selectedRecipe >= group.recipes().size()) {
            return null;
        }
        return group.recipes().get(selectedRecipe);
    }

    public int maxProgress() {
        AssemblerRecipes.Entry entry = selectedEntry();
        return entry == null ? 1 : Math.max(1, entry.ticks());
    }

    private boolean redstoneAllows(Level level, BlockPos pos) {
        boolean powered = level.hasNeighborSignal(pos);
        return switch (redstoneMode) {
            case IGNORE -> true;
            case HIGH -> powered;
            case LOW -> !powered;
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AmmoAssemblerBlockEntity assembler) {
        if (!com.taczammobox.config.FeatureFlags.ammoFactory()) {
            return;
        }
        assembler.pushOutputs(level, pos);
        if (!assembler.redstoneAllows(level, pos)) {
            assembler.reset();
            return;
        }
        AssemblerRecipes.Entry entry = assembler.selectedEntry();
        if (entry == null) {
            assembler.reset();
            return;
        }
        if (!assembler.hasIngredients(entry) || !assembler.canOutput(entry.output())) {
            assembler.reset();
            return;
        }
        assembler.progress++;
        if (assembler.progress >= Math.max(1, entry.ticks())) {
            assembler.craft(entry);
            assembler.progress = 0;
            assembler.pushOutputs(level, pos);
        }
        assembler.setChanged();
    }

    private boolean hasIngredients(AssemblerRecipes.Entry entry) {
        for (AssemblerRecipes.IngredientStack requirement : entry.inputs()) {
            if (countMatching(requirement.ingredient()) < requirement.count()) {
                return false;
            }
        }
        return true;
    }

    private int countMatching(Ingredient ingredient) {
        int found = 0;
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty() && ingredient.test(stack)) {
                found += stack.getCount();
            }
        }
        return found;
    }

    private boolean canOutput(ItemStack result) {
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(new RangedWrapper(inventory, INPUT_SLOTS, SLOTS),
                result.copy(), true);
        return remainder.isEmpty();
    }

    private void craft(AssemblerRecipes.Entry entry) {
        for (AssemblerRecipes.IngredientStack requirement : entry.inputs()) {
            int remaining = requirement.count();
            for (int slot = 0; slot < INPUT_SLOTS && remaining > 0; slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty() || !requirement.ingredient().test(stack)) {
                    continue;
                }
                int taken = Math.min(remaining, stack.getCount());
                inventory.extractItem(slot, taken, false);
                remaining -= taken;
            }
        }
        ItemHandlerHelper.insertItemStacked(new RangedWrapper(inventory, INPUT_SLOTS, SLOTS),
                entry.output().copy(), false);
    }

    private void pushOutputs(Level level, BlockPos pos) {
        IItemHandlerModifiable outputs = new RangedWrapper(inventory, INPUT_SLOTS, SLOTS);
        for (Direction direction : Direction.values()) {
            if (sides[direction.ordinal()] != SideMode.OUTPUT) {
                continue;
            }
            BlockEntity neighbour = level.getBlockEntity(pos.relative(direction));
            if (neighbour == null || neighbour == this) {
                continue;
            }
            neighbour.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite())
                    .ifPresent(target -> {
                        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
                            ItemStack stack = outputs.getStackInSlot(slot);
                            if (stack.isEmpty()) {
                                continue;
                            }
                            ItemStack remainder = ItemHandlerHelper.insertItem(target, stack.copy(), false);
                            outputs.setStackInSlot(slot, remainder);
                        }
                    });
        }
    }

    private void reset() {
        if (progress != 0) {
            progress = 0;
            setChanged();
        }
    }

    public int comparatorOutput() {
        int stored = 0;
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            stored += inventory.getStackInSlot(INPUT_SLOTS + slot).getCount();
        }
        return Math.min(15, stored);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("Progress", progress);
        tag.putInt("SelectedAmmo", selectedAmmo);
        tag.putInt("SelectedRecipe", selectedRecipe);
        tag.putBoolean("Locked", locked);
        int[] sideValues = new int[6];
        for (int index = 0; index < 6; index++) {
            sideValues[index] = sides[index].ordinal();
        }
        tag.putIntArray("Sides", sideValues);
        tag.putInt("Redstone", redstoneMode.ordinal());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        progress = tag.getInt("Progress");
        selectedAmmo = tag.getInt("SelectedAmmo");
        selectedRecipe = tag.getInt("SelectedRecipe");
        locked = tag.getBoolean("Locked");
        int[] sideValues = tag.getIntArray("Sides");
        if (sideValues.length == 6) {
            for (int index = 0; index < 6; index++) {
                sides[index] = SideMode.values()[Math.floorMod(sideValues[index], SideMode.values().length)];
            }
        }
        redstoneMode = RedstoneMode.values()[Math.floorMod(tag.getInt("Redstone"), RedstoneMode.values().length)];
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.taczammobox.ammo_assembler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new AmmoAssemblerMenu(id, playerInventory, this, data);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER && side != null) {
            return switch (sides[side.ordinal()]) {
                case INPUT -> inputOptional.cast();
                case OUTPUT -> outputOptional.cast();
                case DISABLED -> LazyOptional.empty();
            };
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inputOptional.invalidate();
        outputOptional.invalidate();
    }
}
