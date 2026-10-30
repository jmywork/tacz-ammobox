package com.taczammobox.mixin;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmoBox;
import com.taczammobox.AmmoType;
import com.taczammobox.config.WasteSettings;
import com.taczammobox.item.PackagedAmmo;
import com.taczammobox.util.AmmoBoxHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles package -> ammo box filling directly in the deployer block entity,
 * before it calls {@code DeployerHandler.activate}. This bypasses other mods
 * (e.g. the Sentry Mechanical Arm) that cancel the handler for non-loose ammo.
 */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity")
public abstract class DeployerBlockEntityMixin {

    @Shadow
    protected DeployerFakePlayer player;

    @Inject(method = "activate", at = @At("HEAD"), cancellable = true, remap = false)
    private void taczammobox$fillFromPackage(CallbackInfo ci) {
        if (player == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof PackagedAmmo pkg)) {
            return;
        }

        BlockEntity self = (BlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState state = self.getBlockState();
        Direction facing = state.getValue(DirectionalKineticBlock.FACING);
        BlockPos target = self.getBlockPos().relative(facing, 2);

        TransportedItemStackHandlerBehaviour behaviour =
                BlockEntityBehaviour.get(level, target, TransportedItemStackHandlerBehaviour.TYPE);
        if (behaviour == null) {
            return;
        }

        AtomicBoolean processed = new AtomicBoolean(false);
        int rounds = AmmoBoxHelper.roundsPerBox(pkg.getType()) * pkg.boxesPerItem();

        behaviour.handleCenteredProcessingOnAllItems(0.9f, transported -> {
            if (processed.get()) {
                return TransportedItemStackHandlerBehaviour.TransportedResult.doNothing();
            }
            ItemStack stack = transported.stack;
            if (!canFill(stack, pkg)) {
                return TransportedItemStackHandlerBehaviour.TransportedResult.doNothing();
            }

            IAmmoBox ammoBox = (IAmmoBox) stack.getItem();
            ItemStack filled = stack.copy();
            int current = ammoBox.getAmmoCount(stack);
            int capacity = AmmoBoxHelper.capacity(ammoBox, stack, pkg.getType());
            int toAdd = Math.min(rounds, capacity - current);
            ammoBox.setAmmoId(filled, pkg.getType().ammoId());
            ammoBox.setAmmoCount(filled, current + toAdd);

            if (!player.isCreative()) {
                held.shrink(1);
            }

            TransportedItemStack newTransported = transported.copy();
            newTransported.stack = filled;
            processed.set(true);
            return TransportedItemStackHandlerBehaviour.TransportedResult.convertTo(newTransported);
        });

        if (processed.get()) {
            ci.cancel();
        }
    }

    private static boolean canFill(ItemStack stack, PackagedAmmo pkg) {
        if (!(stack.getItem() instanceof IAmmoBox ammoBox)) {
            return false;
        }
        if (ammoBox.isCreative(stack) || ammoBox.isAllTypeCreative(stack)) {
            return false;
        }
        AmmoType type = pkg.getType();
        var boxAmmo = ammoBox.getAmmoId(stack);
        boolean empty = boxAmmo.equals(DefaultAssets.EMPTY_AMMO_ID);
        if (!empty && !boxAmmo.equals(type.ammoId())) {
            return false;
        }
        int space = AmmoBoxHelper.capacity(ammoBox, stack, type) - ammoBox.getAmmoCount(stack);
        boolean waste = WasteSettings.waste();
        if (waste) {
            return space > 0;
        }
        return space >= AmmoBoxHelper.roundsPerBox(type) * pkg.boxesPerItem();
    }
}
