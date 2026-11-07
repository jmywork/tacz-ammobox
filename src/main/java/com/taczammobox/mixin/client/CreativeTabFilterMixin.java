package com.taczammobox.mixin.client;

import com.tacz.guns.api.item.IAttachment;
import com.taczammobox.client.ui.CreativeFilterButton;
import com.taczammobox.config.FeatureFlags;
import com.taczammobox.registry.ModItems;
import com.taczammobox.util.AutoFireTier;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the 正常内容 / 彩蛋 rail beside the creative item grid, the same idea as TACZ's
 * gun pack filters. Our tab carries both ordinary items and easter eggs; the rail swaps
 * which half the grid shows.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeTabFilterMixin
        extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu>
        implements CreativeScreenAccess {

    /** Rail geometry: square 20px buttons just left of the panel, 2px apart. */
    private static final int BUTTON_SIZE = 20;
    private static final int BUTTON_STEP = 22;
    private static final int RAIL_GAP = 4;

    @Unique
    private static boolean taczammobox$showEasterEgg;

    @Unique
    private static boolean taczammobox$warned;

    @Unique
    private CreativeFilterButton taczammobox$normalButton;

    @Unique
    private CreativeFilterButton taczammobox$easterButton;

    protected CreativeTabFilterMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void taczammobox$addFilterRail(CallbackInfo ci) {
        int x = Math.max(2, this.leftPos - BUTTON_SIZE - RAIL_GAP);
        int y = this.topPos + 8;
        this.taczammobox$normalButton = new CreativeFilterButton(x, y,
                new ItemStack(ModItems.PACKER.get()),
                Component.translatable("gui.taczammobox.creative.normal"),
                () -> taczammobox$setFilter(false));
        this.taczammobox$easterButton = new CreativeFilterButton(x, y + BUTTON_STEP,
                new ItemStack(ModItems.AMMO_BELT.get()),
                Component.translatable("gui.taczammobox.creative.easter"),
                () -> taczammobox$setFilter(true));
        this.addRenderableWidget(this.taczammobox$normalButton);
        this.addRenderableWidget(this.taczammobox$easterButton);
        taczammobox$filterNow();
    }

    @Inject(method = "refreshSearchResults", at = @At("TAIL"))
    private void taczammobox$applyFilter(CallbackInfo ci) {
        try {
            taczammobox$filterNow();
        } catch (Throwable t) {
            if (!taczammobox$warned) {
                taczammobox$warned = true;
                com.taczammobox.TaczAmmoBoxMod.LOGGER.warn("[taczammobox] creative filter disabled after an error", t);
            }
        }
    }

    @Unique
    private void taczammobox$filterNow() {
        boolean filterable = FeatureFlags.easterEggTab() && taczammobox$showsOurItems();
        if (this.taczammobox$normalButton != null) {
            this.taczammobox$normalButton.visible = filterable;
            this.taczammobox$normalButton.setSelected(!taczammobox$showEasterEgg);
        }
        if (this.taczammobox$easterButton != null) {
            this.taczammobox$easterButton.visible = filterable;
            this.taczammobox$easterButton.setSelected(taczammobox$showEasterEgg);
        }
        if (!filterable) {
            return;
        }
        boolean wantEaster = taczammobox$showEasterEgg;
        // NonNullList does not support removeIf, so rebuild the list in place.
        java.util.List<ItemStack> kept = new java.util.ArrayList<>(this.menu.items.size());
        for (ItemStack stack : this.menu.items) {
            if (taczammobox$isEasterEgg(stack) == wantEaster) {
                kept.add(stack);
            }
        }
        this.menu.items.clear();
        this.menu.items.addAll(kept);
    }

    @Unique
    private void taczammobox$setFilter(boolean easter) {
        if (taczammobox$showEasterEgg == easter) {
            return;
        }
        taczammobox$showEasterEgg = easter;
        this.taczammobox$refresh();
    }

    /**
     * The selected tab lives in a private static field that mixin cannot shadow reliably
     * here, so recognise our tab by its items instead. Both halves always hold at least
     * one of our items, so the rail stays put while switching filters.
     */
    @Unique
    private boolean taczammobox$showsOurItems() {
        String ours = com.taczammobox.TaczAmmoBoxMod.MOD_ID;
        for (ItemStack stack : this.menu.items) {
            if (stack.isEmpty()) {
                continue;
            }
            if (ours.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static boolean taczammobox$isEasterEgg(ItemStack stack) {
        if (stack.is(ModItems.AMMO_BELT.get())) {
            return true;
        }
        return stack.getItem() instanceof IAttachment attachment
                && AutoFireTier.of(attachment.getAttachmentId(stack)) != null;
    }
}
