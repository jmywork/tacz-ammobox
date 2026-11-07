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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Adds the 正常内容 / 彩蛋 rail beside the creative item grid, the same idea as TACZ's
 * gun pack filters. Vanilla rebuilds {@code menu.items} through
 * {@code refreshCurrentTabContents(Collection)}; we keep the unfiltered collection aside
 * and re-apply our filter every time that method runs.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeTabFilterMixin
        extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

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

    /** Items the tab handed to vanilla, before our filter touched them. */
    @Unique
    private final List<ItemStack> taczammobox$source = new ArrayList<>();

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
    }

    @Inject(method = "refreshCurrentTabContents", at = @At("HEAD"))
    private void taczammobox$captureSource(Collection<ItemStack> items, CallbackInfo ci) {
        this.taczammobox$source.clear();
        this.taczammobox$source.addAll(items);
    }

    @Inject(method = "refreshCurrentTabContents", at = @At("TAIL"))
    private void taczammobox$applyFilter(Collection<ItemStack> items, CallbackInfo ci) {
        try {
            taczammobox$filterNow();
        } catch (Throwable t) {
            if (!taczammobox$warned) {
                taczammobox$warned = true;
                com.taczammobox.TaczAmmoBoxMod.LOGGER.warn("[taczammobox] creative filter disabled after an error", t);
            }
        }
    }

    /** Rebuilds {@code menu.items} from the captured source according to the rail state. */
    @Unique
    private void taczammobox$filterNow() {
        boolean filterable = FeatureFlags.easterEggTab() && taczammobox$sourceHasOurItems();
        if (this.taczammobox$normalButton != null) {
            this.taczammobox$normalButton.visible = filterable;
            this.taczammobox$normalButton.setSelected(!taczammobox$showEasterEgg);
        }
        if (this.taczammobox$easterButton != null) {
            this.taczammobox$easterButton.visible = filterable;
            this.taczammobox$easterButton.setSelected(taczammobox$showEasterEgg);
        }
        if (!filterable || this.taczammobox$source.isEmpty()) {
            return;
        }
        boolean wantEaster = taczammobox$showEasterEgg;
        List<ItemStack> kept = new ArrayList<>(this.taczammobox$source.size());
        for (ItemStack stack : this.taczammobox$source) {
            if (stack.isEmpty()) {
                continue;
            }
            if (taczammobox$isEasterEgg(stack) == wantEaster) {
                kept.add(stack);
            }
        }
        // NonNullList does not support removeIf, so rebuild the list in place.
        this.menu.items.clear();
        this.menu.items.addAll(kept);
        this.menu.scrollTo(0.0F);
    }

    @Unique
    private void taczammobox$setFilter(boolean easter) {
        if (taczammobox$showEasterEgg == easter) {
            return;
        }
        taczammobox$showEasterEgg = easter;
        try {
            taczammobox$filterNow();
        } catch (Throwable t) {
            if (!taczammobox$warned) {
                taczammobox$warned = true;
                com.taczammobox.TaczAmmoBoxMod.LOGGER.warn("[taczammobox] creative filter disabled after an error", t);
            }
        }
    }

    /** The selected tab is a private static field mixin cannot shadow reliably, so the
     * captured item collection tells us whether this is our tab. */
    @Unique
    private boolean taczammobox$sourceHasOurItems() {
        String ours = com.taczammobox.TaczAmmoBoxMod.MOD_ID;
        for (ItemStack stack : this.taczammobox$source) {
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
