package com.taczammobox.block;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.taczammobox.AmmoType;
import com.taczammobox.config.WasteSettings;
import com.taczammobox.item.PackagedAmmo;
import com.taczammobox.registry.AmmoTypes;
import com.taczammobox.registry.ModItems;
import com.taczammobox.util.AmmoBoxHelper;
import com.taczammobox.util.AutoIoHelper;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import com.taczammobox.menu.AmmoPackerMenu;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;

public class AmmoPackerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static int ticksPerOperation() {
        return WasteSettings.packerTicks();
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

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot != SLOT_INPUT) {
                return false;
            }
            return stack.getItem() instanceof IAmmo || stack.getItem() instanceof PackagedAmmo;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != SLOT_INPUT) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
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
                case 1 -> mode;
                case 2, 3, 4, 5, 6, 7 -> sides[index - 2].ordinal();
                case 8 -> redstoneMode.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progress = value;
            } else if (index == 1) {
                mode = value;
            } else if (index >= 2 && index < 8) {
                sides[index - 2] = SideMode.values()[Math.floorMod(value, SideMode.values().length)];
            } else if (index == 8) {
                redstoneMode = RedstoneMode.values()[Math.floorMod(value, RedstoneMode.values().length)];
            }
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    private int progress;
    private int mode; // 0 = pack, 1 = unpack
    private int ioCooldown;
    private int idleCooldown;
    private static final int IDLE_TICKS = 10;
    private ItemStack cachedRawResult = ItemStack.EMPTY;
    private ItemStack cachedInput = ItemStack.EMPTY;
    private int cachedMode = -1;
    // Direction.values() order: DOWN, UP, NORTH, SOUTH, WEST, EAST.
    private final SideMode[] sides = {SideMode.OUTPUT, SideMode.INPUT, SideMode.INPUT,
            SideMode.INPUT, SideMode.INPUT, SideMode.INPUT};
    private RedstoneMode redstoneMode = RedstoneMode.IGNORE;
    private final LazyOptional<IItemHandler> handlerCap = LazyOptional.of(() -> inventory);
    private final Map<Direction, LazyOptional<IItemHandler>> sideCaps = new EnumMap<>(Direction.class);

    public AmmoPackerBlockEntity(BlockPos pos, BlockState state) {
        super(com.taczammobox.registry.ModBlockEntities.PACKER.get(), pos, state);
        for (Direction direction : Direction.values()) {
            sideCaps.put(direction, LazyOptional.of(() -> new SideHandler(direction)));
        }
    }

    public ContainerData getData() {
        return data;
    }

    public void toggleMode() {
        mode = mode == 0 ? 1 : 0;
        progress = 0;
        wake();
        if (level != null && !level.isClientSide) {
            level.setBlock(worldPosition, getBlockState().setValue(AmmoPackerBlock.MODE,
                    mode == 0 ? AmmoPackerBlock.Mode.PACK : AmmoPackerBlock.Mode.UNPACK), 3);
        }
        setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            AmmoPackerBlock.Mode target = mode == 0 ? AmmoPackerBlock.Mode.PACK : AmmoPackerBlock.Mode.UNPACK;
            if (getBlockState().getValue(AmmoPackerBlock.MODE) != target) {
                level.setBlock(worldPosition, getBlockState().setValue(AmmoPackerBlock.MODE, target), 3);
            }
        }
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
        return Component.translatable("block.taczammobox.ammo_packer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new AmmoPackerMenu(id, playerInventory, this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AmmoPackerBlockEntity be) {
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
        if (be.hasResult()) {
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
        return inventory.getStackInSlot(SLOT_INPUT).isEmpty()
                && inventory.getStackInSlot(SLOT_OUTPUT).isEmpty();
    }

    private boolean hasResult() {
        ItemStack raw = rawResult();
        return !raw.isEmpty() && canAccept(raw);
    }

    private ItemStack findResult() {
        ItemStack raw = rawResult();
        if (raw.isEmpty() || !canAccept(raw)) {
            return ItemStack.EMPTY;
        }
        return raw.copy();
    }

    private ItemStack rawResult() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (cachedMode == mode && input.getCount() == cachedInput.getCount()
                && ItemStack.isSameItemSameTags(input, cachedInput)) {
            return cachedRawResult;
        }
        cachedRawResult = buildRawResult(input);
        cachedInput = input.copy();
        cachedMode = mode;
        return cachedRawResult;
    }

    private ItemStack buildRawResult(ItemStack input) {
        if (input.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = ItemStack.EMPTY;
        if (mode == 0) {
            if (input.getItem() instanceof IAmmo ammo) {
                AmmoType type = AmmoTypes.byId(ammo.getAmmoId(input));
                if (type != null) {
                    int need = AmmoBoxHelper.roundsPerBox(type);
                    if (input.getCount() >= need) {
                        result = new ItemStack(ModItems.box(type.ammoId()));
                    }
                }
            } else if (input.getItem() instanceof PackagedAmmo pkg
                    && pkg.boxesPerItem() == 1 && input.getCount() >= 9) {
                var crate = ModItems.crate(pkg.getType().ammoId());
                if (crate != null) {
                    result = new ItemStack(crate);
                }
            }
        } else if (input.getItem() instanceof PackagedAmmo pkg) {
            if (pkg.boxesPerItem() == 9) {
                var box = ModItems.box(pkg.getType().ammoId());
                if (box != null) {
                    result = new ItemStack(box, 9);
                }
            } else {
                int rounds = AmmoBoxHelper.roundsPerBox(pkg.getType());
                result = AmmoItemBuilder.create()
                        .setId(pkg.getType().ammoId())
                        .setCount(rounds)
                        .build();
            }
        }
        return result;
    }

    private boolean canAccept(ItemStack result) {
        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameTags(output, result)) {
            return false;
        }
        int max = Math.min(output.getMaxStackSize(), 64);
        return output.getCount() + result.getCount() <= max;
    }

    private void process() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        ItemStack result = findResult();
        if (result.isEmpty()) {
            return;
        }

        if (mode == 0) {
            if (input.getItem() instanceof IAmmo ammo) {
                AmmoType type = AmmoTypes.byId(ammo.getAmmoId(input));
                if (type != null) {
                    input.shrink(AmmoBoxHelper.roundsPerBox(type));
                }
            } else {
                input.shrink(9);
            }
        } else {
            input.shrink(1);
        }

        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, result);
        } else {
            output.grow(result.getCount());
        }
    }

    private void autoIo() {
        if (level == null) {
            return;
        }
        // Pull from every configured input face.
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
        // Push to every configured output face.
        for (Direction direction : Direction.values()) {
            if (sides[direction.ordinal()] != SideMode.OUTPUT) {
                continue;
            }
            ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
            if (output.isEmpty()) {
                continue;
            }
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor == null) {
                continue;
            }
            neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).ifPresent(target -> {
                ItemStack remainder = ItemHandlerHelper.insertItem(target, output.copy(), false);
                inventory.setStackInSlot(SLOT_OUTPUT, remainder);
            });
        }
    }

    public int comparatorOutput() {
        if (!inventory.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            return 15;
        }
        if (progress <= 0) {
            return 0;
        }
        int ticks = Math.max(1, ticksPerOperation());
        return Math.max(1, Math.min(14, 1 + progress * 13 / ticks));
    }

    public boolean isUnpacking() {
        return mode == 1;
    }

    public int getProgress() {
        return progress;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("Inventory"));
        progress = tag.getInt("Progress");
        mode = tag.getInt("Mode");
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
        tag.putInt("Mode", mode);
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
            if (slot != SLOT_INPUT || mode() != SideMode.INPUT) {
                return stack;
            }
            return inventory.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != SLOT_OUTPUT || mode() != SideMode.OUTPUT) {
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
            return slot == SLOT_INPUT && mode() == SideMode.INPUT && inventory.isItemValid(slot, stack);
        }
    }
}
