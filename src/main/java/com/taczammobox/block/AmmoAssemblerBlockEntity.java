package com.taczammobox.block;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.taczammobox.config.AmmoFactorySettings;
import com.taczammobox.item.PackagedAmmo;
import com.taczammobox.menu.AmmoAssemblerMenu;
import com.taczammobox.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/**
 * Turns casing + propellant + primer into loose ammo of the type set in the template slot.
 *
 * <p>The template slot is a real item used as a stencil; it is never consumed. Output
 * count and craft time come from {@code config/taczammobox/ammo_factory.json}.</p>
 */
public class AmmoAssemblerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_CASING = 0;
    public static final int SLOT_POWDER = 1;
    public static final int SLOT_PRIMER = 2;
    public static final int SLOT_TEMPLATE = 3;
    public static final int SLOT_OUTPUT = 4;
    public static final int SLOTS = 5;

    private int progress;
    private int maxProgress = 60;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_CASING -> stack.is(ModItems.CASING.get());
                case SLOT_POWDER -> stack.is(ModItems.POWDER.get());
                case SLOT_PRIMER -> stack.is(ModItems.PRIMER.get());
                case SLOT_TEMPLATE -> stack.getItem() instanceof IAmmo
                        || stack.getItem() instanceof PackagedAmmo;
                default -> false;
            };
        }

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
                case 0 -> progress;
                case 1 -> maxProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = Math.max(1, value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 2;
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, AmmoAssemblerBlockEntity assembler) {
        if (!com.taczammobox.config.FeatureFlags.ammoFactory()) {
            return;
        }
        ResourceLocation ammoId = templateAmmoId(assembler.inventory.getStackInSlot(SLOT_TEMPLATE));
        if (ammoId == null) {
            assembler.reset();
            return;
        }
        int rounds = AmmoFactorySettings.roundsFor(ammoId);
        assembler.maxProgress = AmmoFactorySettings.ticksFor(ammoId);
        if (!assembler.canConsume() || !assembler.canOutput(ammoId, rounds)) {
            assembler.reset();
            return;
        }
        assembler.progress++;
        if (assembler.progress >= assembler.maxProgress) {
            assembler.craft(ammoId, rounds);
            assembler.progress = 0;
        }
        assembler.setChanged();
    }

    private static ResourceLocation templateAmmoId(ItemStack stack) {
        if (stack.getItem() instanceof IAmmo ammo) {
            return ammo.getAmmoId(stack);
        }
        if (stack.getItem() instanceof PackagedAmmo pkg) {
            return pkg.getType().ammoId();
        }
        return null;
    }

    private boolean canConsume() {
        return inventory.getStackInSlot(SLOT_CASING).getCount() >= 1
                && inventory.getStackInSlot(SLOT_POWDER).getCount() >= 1
                && inventory.getStackInSlot(SLOT_PRIMER).getCount() >= 1;
    }

    private boolean canOutput(ResourceLocation ammoId, int rounds) {
        ItemStack result = AmmoItemBuilder.create().setId(ammoId).setCount(rounds).build();
        ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameTags(out, result) && out.getCount() + rounds <= out.getMaxStackSize();
    }

    private void craft(ResourceLocation ammoId, int rounds) {
        inventory.extractItem(SLOT_CASING, 1, false);
        inventory.extractItem(SLOT_POWDER, 1, false);
        inventory.extractItem(SLOT_PRIMER, 1, false);
        ItemStack result = AmmoItemBuilder.create().setId(ammoId).setCount(rounds).build();
        ItemStack out = inventory.getStackInSlot(SLOT_OUTPUT);
        if (out.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, result);
        } else {
            out.grow(rounds);
        }
    }

    private void reset() {
        if (progress != 0) {
            progress = 0;
            setChanged();
        }
    }

    public int comparatorOutput() {
        return maxProgress <= 0 ? 0 : Math.min(15, progress * 15 / maxProgress);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        progress = tag.getInt("Progress");
        maxProgress = Math.max(1, tag.getInt("MaxProgress"));
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
