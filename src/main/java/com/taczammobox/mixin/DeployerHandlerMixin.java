package com.taczammobox.mixin;

import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optional Create compat: a deployer holding a box/crate can pour it into an
 * ammo box on a belt or depot. Not a recipe 鈥?Create's recipe ingredients can
 * neither match TACZ's NBT ammo id nor consume a whole stack.
 *
 * <p>{@code @Pseudo} keeps this silent when Create is not installed.</p>
 */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.kinetics.deployer.DeployerHandler", priority = 500)
public class DeployerHandlerMixin {

    @Inject(method = "shouldActivate", at = @At("HEAD"), cancellable = true, remap = false)
    private static void taczammobox$shouldActivate(ItemStack held, Level world, BlockPos targetPos,
                                                   Direction facing, CallbackInfoReturnable<Boolean> cir) {
        if (!(held.getItem() instanceof PackagedAmmo pkg)) {
            return;
        }
        if (world.isEmptyBlock(targetPos)) {
            cir.setReturnValue(false);
            return;
        }

        TransportedItemStackHandlerBehaviour behaviour =
                BlockEntityBehaviour.get(world, targetPos, TransportedItemStackHandlerBehaviour.TYPE);
        if (behaviour == null) {
            return;
        }

        AtomicBoolean found = new AtomicBoolean(false);
        behaviour.handleCenteredProcessingOnAllItems(0.9f, transported -> {
            if (canFill(transported.stack, pkg)) {
                found.set(true);
            }
            return TransportedItemStackHandlerBehaviour.TransportedResult.doNothing();
        });
        if (found.get()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "activate", at = @At("HEAD"), cancellable = true, remap = false)
    private static void taczammobox$activate(DeployerFakePlayer player, Vec3 vec, BlockPos clickedPos,
                                             Vec3 extensionVector, @Coerce Object mode, CallbackInfo ci) {
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof PackagedAmmo pkg)) {
            return;
        }

        TransportedItemStackHandlerBehaviour behaviour =
                BlockEntityBehaviour.get(level, clickedPos, TransportedItemStackHandlerBehaviour.TYPE);
        if (behaviour == null) {
            return;
        }

        AtomicBoolean processed = new AtomicBoolean(false);
        int rounds = roundsOf(pkg);

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

    private static int roundsOf(PackagedAmmo pkg) {
        return AmmoBoxHelper.roundsPerBox(pkg.getType()) * pkg.boxesPerItem();
    }

    private static boolean canFill(ItemStack stack, PackagedAmmo pkg) {
        if (!(stack.getItem() instanceof IAmmoBox ammoBox)) {
            return false;
        }
        if (ammoBox.isCreative(stack) || ammoBox.isAllTypeCreative(stack)) {
            return false;
        }
        AmmoType type = pkg.getType();
        ResourceLocation boxAmmo = ammoBox.getAmmoId(stack);
        boolean empty = boxAmmo.equals(DefaultAssets.EMPTY_AMMO_ID);
        if (!empty && !boxAmmo.equals(type.ammoId())) {
            return false;
        }
        int space = AmmoBoxHelper.capacity(ammoBox, stack, type) - ammoBox.getAmmoCount(stack);
        boolean waste = WasteSettings.waste();
        if (waste) {
            return space > 0;
        }
        boolean fits = space >= roundsOf(pkg);
        return fits;
    }
}
