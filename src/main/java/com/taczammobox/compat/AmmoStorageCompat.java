package com.taczammobox.compat;

import com.tacz.guns.block.AmmoStorageBlock;
import com.tacz.guns.block.entity.AmmoStorageBlockEntity;
import com.tacz.guns.util.AmmoStorageHelper;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.item.PackagedAmmo;
import com.taczammobox.util.AmmoBoxHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Lets our ammo boxes and crates pour their rounds into TACZ 1.2.0's ammo
 * storage blocks (iron / gold / diamond). TACZ only recognises its own
 * {@code IAmmoBox} items, so our packages need their own entry point.
 *
 * <p>Right-click the storage with a box in hand: whole boxes are poured in;
 * if the storage runs out of room mid-box the leftover rounds come back as
 * loose ammo instead of being voided.</p>
 */
@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID)
public final class AmmoStorageCompat {
    private AmmoStorageCompat() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());
        if (!(held.getItem() instanceof PackagedAmmo packaged)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AmmoStorageBlock)) {
            return;
        }
        // Sneaking keeps our "place the box as a pile" gesture working.
        if (player.isShiftKeyDown()) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof AmmoStorageBlockEntity storage)) {
            return;
        }

        event.setCanceled(true);
        if (level.isClientSide) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        event.setCancellationResult(InteractionResult.CONSUME);

        int perItem = AmmoBoxHelper.roundsPerBox(packaged.getType()) * packaged.boxesPerItem();
        if (perItem <= 0) {
            return;
        }
        int total = perItem * held.getCount();
        ItemStack probe = AmmoStorageHelper.createAmmoStack(packaged.getType().ammoId(), total);
        int canInsert = storage.insertAmmo(probe, true);
        if (canInsert <= 0) {
            player.displayClientMessage(Component.translatable("message.taczammobox.storage.full"), true);
            return;
        }
        int inserted = storage.insertAmmo(
                AmmoStorageHelper.createAmmoStack(packaged.getType().ammoId(), Math.min(canInsert, total)), false);
        if (inserted <= 0) {
            player.displayClientMessage(Component.translatable("message.taczammobox.storage.full"), true);
            return;
        }

        int leftoverRounds = total - inserted;
        int leftoverItems = leftoverRounds / perItem;
        int looseRounds = leftoverRounds % perItem;

        if (!player.getAbilities().instabuild) {
            held.setCount(leftoverItems);
        }
        if (looseRounds > 0 && !player.getAbilities().instabuild) {
            ItemStack loose = AmmoStorageHelper.createAmmoStack(packaged.getType().ammoId(), looseRounds);
            ItemHandlerHelper.giveItemToPlayer(player, loose);
        }

        player.displayClientMessage(Component.translatable("message.taczammobox.storage.filled",
                inserted, packaged.getType().displayName()), true);
        level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.8F,
                0.8F + level.getRandom().nextFloat() * 0.4F);
    }
}
