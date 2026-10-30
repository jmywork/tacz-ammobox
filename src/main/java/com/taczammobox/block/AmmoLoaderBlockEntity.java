package com.taczammobox.block;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.taczammobox.AmmoType;
import com.taczammobox.config.WasteSettings;
import com.taczammobox.item.PackagedAmmo;
import com.taczammobox.menu.AmmoLoaderMenu;
import com.taczammobox.registry.AmmoTypes;
import com.taczammobox.util.AmmoBoxHelper;
import com.taczammobox.util.AutoIoHelper;
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
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public class AmmoLoaderBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_BOX_IN = 0;
    public static final int SLOT_SOURCE = 1;
    public static final int SLOT_BOX_OUT = 2;
    public static int ticksPerOperation() {
        return WasteSettings.loaderTicks();
    }

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

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case SLOT_BOX_IN, SLOT_BOX_OUT -> stack.getItem() instanceof IAmmoBox;
                case SLOT_SOURCE -> stack.getItem() instanceof IAmmo || stack.getItem() instanceof PackagedAmmo;
                default -> false;
            };
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == SLOT_BOX_OUT) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == SLOT_SOURCE ? 64 : 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            wake();
            setChanged();
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1, 2, 3, 4, 5, 6 -> sides[index - 1].ordinal();
                case 7 -> redstoneMode.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progress = value;
            } else if (index >= 1 && index < 7) {
                sides[index - 1] = SideMode.values()[Math.floorMod(value, SideMode.values().length)];
            } else if (index == 7) {
                redstoneMode = RedstoneMode.values()[Math.floorMod(value, RedstoneMode.values().length)];
            }
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    private int progress;
    private int ioCooldown;
    private int idleCooldown;
    private static final int IDLE_TICKS = 10;
    // Direction.values() order: DOWN, UP, NORTH, SOUTH, WEST, EAST.
    private final SideMode[] sides = {SideMode.OUTPUT, SideMode.INPUT, SideMode.INPUT,
            SideMode.INPUT, SideMode.INPUT, SideMode.INPUT};
    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;
    private final LazyOptional<IItemHandler> handlerCap = LazyOptional.of(() -> inventory);
    private final Map<Direction, LazyOptional<IItemHandler>> sideCaps = new EnumMap<>(Direction.class);

    public AmmoLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(com.taczammobox.registry.ModBlockEntities.LOADER.get(), pos, state);
        for (Direction direction : Direction.values()) {
            sideCaps.put(direction, LazyOptional.of(() -> new SideHandler(direction)));
        }
    }

    public ContainerData getData() {
        return data;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public SideMode sideMode(Direction direction) {
        return sides[direction.ordinal()];
    }

    public void cycleSide(int index) {
        if (index < 0 || index >= sides.length) {
            return;
        }
        sides[index] = SideMode.values()[(sides[index].ordinal() + 1) % SideMode.values().length];
        wake();
        setChanged();
    }

    public RedstoneMode getRedstoneMode() {
        return redstoneMode;
    }

    public void cycleRedstone() {
        redstoneMode = RedstoneMode.values()[(redstoneMode.ordinal() + 1) % RedstoneMode.values().length];
        wake();
        setChanged();
    }

    public void wake() {
        idleCooldown = 0;
        ioCooldown = 0;
    }

    private boolean canRun() {
        if (level == null) {
            return false;
        }
        boolean powered = level.hasNeighborSignal(worldPosition);
        return switch (redstoneMode) {
            case IGNORE -> true;
            case HIGH -> powered;
            case LOW -> !powered;
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.taczammobox.ammo_loader");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new AmmoLoaderMenu(id, playerInventory, this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AmmoLoaderBlockEntity be) {
        if (level.isClientSide) {
            return;
        }
        if (be.idleCooldown > 0) {
            be.idleCooldown--;
            return;
        }
        if (!be.canRun()) {
            be.progress = 0;
            be.idleCooldown = IDLE_TICKS;
            return;
        }
        if (be.isIdle()) {
            if (--be.ioCooldown <= 0) {
                be.autoIo();
                be.ioCooldown = IDLE_TICKS;
            }
            if (be.isIdle()) {
                be.idleCooldown = IDLE_TICKS;
            }
            return;
        }
        if (be.inputBoxIsFull()) {
            be.moveBoxToOutput();
            be.setChanged();
        }
        if (be.canProcess()) {
            be.progress++;
            if (be.progress >= ticksPerOperation()) {
                be.process();
                be.progress = 0;
                be.setChanged();
            }
        } else {
            be.progress = 0;
        }

        if (--be.ioCooldown <= 0) {
            be.autoIo();
            be.ioCooldown = 2;
        }
    }

    private boolean isIdle() {
        return inventory.getStackInSlot(SLOT_BOX_IN).isEmpty()
                && inventory.getStackInSlot(SLOT_SOURCE).isEmpty()
                && inventory.getStackInSlot(SLOT_BOX_OUT).isEmpty();
    }

    private static AmmoType typeOf(ItemStack source) {
        if (source.getItem() instanceof PackagedAmmo pkg) {
            return pkg.getType();
        }
        if (source.getItem() instanceof IAmmo ammo) {
            return AmmoTypes.byId(ammo.getAmmoId(source));
        }
        return null;
    }

    private static boolean boxAccepts(IAmmoBox box, ItemStack stack, AmmoType type) {
        ResourceLocation boxAmmo = box.getAmmoId(stack);
        return boxAmmo.equals(DefaultAssets.EMPTY_AMMO_ID) || boxAmmo.equals(type.ammoId());
    }

    private boolean canProcess() {
        ItemStack boxStack = inventory.getStackInSlot(SLOT_BOX_IN);
        ItemStack source = inventory.getStackInSlot(SLOT_SOURCE);
        if (boxStack.isEmpty() || source.isEmpty() || !(boxStack.getItem() instanceof IAmmoBox box)) {
            return false;
        }
        if (box.isCreative(boxStack) || box.isAllTypeCreative(boxStack)) {
            return false;
        }
        AmmoType type = typeOf(source);
        if (type == null || !boxAccepts(box, boxStack, type)) {
            return false;
        }
        int capacity = AmmoBoxHelper.capacity(box, boxStack, type);
        int current = box.getAmmoCount(boxStack);
        int space = capacity - current;
        if (space <= 0) {
            return false;
        }

        ItemStack output = inventory.getStackInSlot(SLOT_BOX_OUT);
        if (source.getItem() instanceof PackagedAmmo pkg) {
            int rounds = AmmoBoxHelper.roundsPerBox(type) * pkg.boxesPerItem();
            if (!WasteSettings.waste() && space < rounds) {
                return false;
            }
            int toAdd = Math.min(space, rounds);
            if (toAdd <= 0) {
                return false;
            }
            boolean willFill = current + toAdd >= capacity;
            return !willFill || output.isEmpty();
        }

        int toAdd = Math.min(space, source.getCount());
        if (toAdd <= 0) {
            return false;
        }
        boolean willFill = current + toAdd >= capacity;
        return !willFill || output.isEmpty();
    }

    private void process() {
        ItemStack boxStack = inventory.getStackInSlot(SLOT_BOX_IN);
        ItemStack source = inventory.getStackInSlot(SLOT_SOURCE);
        IAmmoBox box = (IAmmoBox) boxStack.getItem();
        AmmoType type = typeOf(source);
        if (type == null) {
            return;
        }
        int current = box.getAmmoCount(boxStack);
        int capacity = AmmoBoxHelper.capacity(box, boxStack, type);
        int space = capacity - current;

        if (source.getItem() instanceof PackagedAmmo pkg) {
            int rounds = AmmoBoxHelper.roundsPerBox(type) * pkg.boxesPerItem();
            int toAdd = Math.min(space, rounds);
            box.setAmmoId(boxStack, type.ammoId());
            box.setAmmoCount(boxStack, current + toAdd);
            source.shrink(1);
            if (current + toAdd >= capacity) {
                moveBoxToOutput();
            }
            return;
        }

        int toAdd = Math.min(space, source.getCount());
        box.setAmmoId(boxStack, type.ammoId());
        box.setAmmoCount(boxStack, current + toAdd);
        source.shrink(toAdd);
        if (current + toAdd >= capacity) {
            moveBoxToOutput();
        }
    }

    private boolean inputBoxIsFull() {
        ItemStack boxStack = inventory.getStackInSlot(SLOT_BOX_IN);
        if (!(boxStack.getItem() instanceof IAmmoBox box)) {
            return false;
        }
        AmmoType type = AmmoTypes.byId(box.getAmmoId(boxStack));
        return type != null && box.getAmmoCount(boxStack) >= AmmoBoxHelper.capacity(box, boxStack, type);
    }

    private void moveBoxToOutput() {
        ItemStack boxStack = inventory.getStackInSlot(SLOT_BOX_IN);
        if (inventory.getStackInSlot(SLOT_BOX_OUT).isEmpty()) {
            inventory.setStackInSlot(SLOT_BOX_OUT, boxStack.copy());
            inventory.setStackInSlot(SLOT_BOX_IN, ItemStack.EMPTY);
        }
    }

    private void autoIo() {
        if (level == null) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (sides[direction.ordinal()] != SideMode.INPUT) {
                continue;
            }
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor == null) {
                continue;
            }
            neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).ifPresent(source -> {
                AutoIoHelper.pull(inventory, source);
            });
        }
        for (Direction direction : Direction.values()) {
            if (sides[direction.ordinal()] != SideMode.OUTPUT) {
                continue;
            }
            ItemStack output = inventory.getStackInSlot(SLOT_BOX_OUT);
            if (output.isEmpty()) {
                continue;
            }
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor == null) {
                continue;
            }
            neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).ifPresent(target -> {
                ItemStack remainder = ItemHandlerHelper.insertItem(target, output.copy(), false);
                inventory.setStackInSlot(SLOT_BOX_OUT, remainder);
            });
        }
    }

    public int comparatorOutput() {
        if (!inventory.getStackInSlot(SLOT_BOX_OUT).isEmpty()) {
            return 15;
        }
        if (progress <= 0) {
            return 0;
        }
        int ticks = Math.max(1, ticksPerOperation());
        return Math.max(1, Math.min(14, 1 + progress * 13 / ticks));
    }

    public int getProgress() {
        return progress;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        progress = tag.getInt("Progress");
        int[] savedSides = tag.getIntArray("Sides");
        if (savedSides.length == sides.length) {
            for (int i = 0; i < sides.length; i++) {
                sides[i] = SideMode.values()[Math.floorMod(savedSides[i], SideMode.values().length)];
            }
        }
        redstoneMode = RedstoneMode.values()[Math.floorMod(tag.getInt("Redstone"), RedstoneMode.values().length)];
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("Progress", progress);
        int[] savedSides = new int[sides.length];
        for (int i = 0; i < sides.length; i++) {
            savedSides[i] = sides[i].ordinal();
        }
        tag.putIntArray("Sides", savedSides);
        tag.putInt("Redstone", redstoneMode.ordinal());
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == null) {
                return handlerCap.cast();
            }
            return sideCaps.get(side).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handlerCap.invalidate();
        sideCaps.values().forEach(LazyOptional::invalidate);
    }

    private class SideHandler implements IItemHandler {
        private final Direction direction;

        private SideHandler(Direction direction) {
            this.direction = direction;
        }

        private SideMode mode() {
            return sides[direction.ordinal()];
        }

        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (mode() != SideMode.INPUT || (slot != SLOT_BOX_IN && slot != SLOT_SOURCE)) {
                return stack;
            }
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (mode() != SideMode.OUTPUT || slot != SLOT_BOX_OUT) {
                return ItemStack.EMPTY;
            }
            return inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return mode() == SideMode.INPUT && (slot == SLOT_BOX_IN || slot == SLOT_SOURCE)
                    && inventory.isItemValid(slot, stack);
        }
    }
}
