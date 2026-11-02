package com.taczammobox.command;

import com.tacz.guns.api.TimelessAPI;
import com.taczammobox.compat.CuriosCompat;
import com.taczammobox.item.PackagedAmmo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Removes boxes/crates whose TACZ ammo type is no longer loaded. */
public final class OrphanCleanup {
    public record Result(int stacks, int items, int locations) {
    }

    private OrphanCleanup() {
    }

    public static Result run(ServerLevel level, ServerPlayer center, int radius, boolean remove) {
        int[] counts = new int[2];
        Set<String> locations = new HashSet<>();

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            String key = "player:" + player.getUUID();
            clean(new InvWrapper(player.getInventory()), remove, counts, locations, key);
            List<IItemHandler> curios = new ArrayList<>();
            CuriosCompat.addEquipped(player, curios);
            for (IItemHandler handler : curios) {
                clean(handler, remove, counts, locations, key);
            }
        }

        int radiusSq = radius * radius;
        ChunkPos centerChunk = new ChunkPos(center.blockPosition());
        int chunkRadius = (radius + 15) >> 4;
        for (int cx = centerChunk.x - chunkRadius; cx <= centerChunk.x + chunkRadius; cx++) {
            for (int cz = centerChunk.z - chunkRadius; cz <= centerChunk.z + chunkRadius; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos pos = blockEntity.getBlockPos();
                    if (pos.distSqr(center.blockPosition()) > radiusSq) {
                        continue;
                    }
                    IItemHandler handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER)
                            .resolve().orElse(null);
                    if (handler != null) {
                        clean(handler, remove, counts, locations, "block:" + pos);
                    }
                }
            }
        }

        for (var entity : level.getAllEntities()) {
            if (!(entity instanceof ItemEntity itemEntity)) {
                continue;
            }
            if (itemEntity.distanceToSqr(center) > radiusSq) {
                continue;
            }
            ItemStack stack = itemEntity.getItem();
            if (isOrphan(stack)) {
                counts[0]++;
                counts[1] += stack.getCount();
                locations.add("drop:" + itemEntity.getUUID());
                if (remove) {
                    itemEntity.discard();
                }
            }
        }
        return new Result(counts[0], counts[1], locations.size());
    }

    private static void clean(IItemHandler handler, boolean remove, int[] counts,
                              Set<String> locations, String location) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!isOrphan(stack)) {
                continue;
            }
            counts[0]++;
            counts[1] += stack.getCount();
            locations.add(location);
            if (!remove) {
                continue;
            }
            if (handler instanceof IItemHandlerModifiable modifiable) {
                modifiable.setStackInSlot(slot, ItemStack.EMPTY);
            } else {
                handler.extractItem(slot, stack.getCount(), false);
            }
        }
    }

    private static boolean isOrphan(ItemStack stack) {
        if (!(stack.getItem() instanceof PackagedAmmo pkg)) {
            return false;
        }
        return TimelessAPI.getCommonAmmoIndex(pkg.getType().ammoId()).isEmpty();
    }
}
