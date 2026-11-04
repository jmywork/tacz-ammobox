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
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Recipe-driven ammo assembler.
 *
 * <p>Selection is two-step, matching the three-pane screen: pick an ammo type, then one of
 * its recipes. The machine matches the selected recipe's ingredients in its 3x3 grid,
 * consumes them and emits the recipe output.</p>
 */
public class AmmoAssemblerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOTS = 9;
    public static final int SLOT_OUTPUT = 9;
    public static final int SLOTS = 10;

    private int selectedAmmo = -1;
    private int selectedRecipe = -1;
    private int progress;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == SLOT_OUTPUT) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> inventory);

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> selectedAmmo;
                case 1 -> selectedRecipe;
                case 2 -> progress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> selectedAmmo = value;
                case 1 -> selectedRecipe = value;
                case 2 -> progress = Math.max(0, value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 3;
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

    public int getSelectedAmmo() {
        return selectedAmmo;
    }

    public int getSelectedRecipe() {
        return selectedRecipe;
    }

    public void selectAmmo(int index) {
        List<AssemblerRecipes.Group> groups = groups();
        selectedAmmo = index >= 0 && index < groups.size() ? index : -1;
        selectedRecipe = -1;
        progress = 0;
        setChanged();
    }

    public void selectRecipe(int index) {
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, AmmoAssemblerBlockEntity assembler) {
        if (!com.taczammobox.config.FeatureFlags.ammoFactory()) {
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
        ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameTags(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize();
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
        ItemStack result = entry.output().copy();
        ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, result);
        } else {
            out.grow(result.getCount());
        }
    }

    private void reset() {
        if (progress != 0) {
            progress = 0;
            setChanged();
        }
    }

    public int comparatorOutput() {
        int max = maxProgress();
        return max <= 0 ? 0 : Math.min(15, progress * 15 / max);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("Progress", progress);
        tag.putInt("SelectedAmmo", selectedAmmo);
        tag.putInt("SelectedRecipe", selectedRecipe);
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
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return handler.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }
}
