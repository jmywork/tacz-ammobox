package com.taczammobox.mixin.client;

import com.taczammobox.client.ui.CreativeFilterButton;
import com.taczammobox.config.FeatureFlags;
import com.taczammobox.registry.ModItems;
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

    /** Rail geometry: 32x26 tabs stacked flush against the left edge of the panel. */
    private static final int BUTTON_WIDTH = 32;
    private static final int BUTTON_STEP = 26;
    private static final int RAIL_GAP = 0;

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
        int x = Math.max(2, this.leftPos - BUTTON_WIDTH - RAIL_GAP);
        int y = this.topPos + 6;
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

    /**
     * TACZ's own creative mixin refills the grid from its cached pack list after we have
     * filtered, which silently undoes the rail. Re-applying the filter right before the
     * frame is drawn keeps our selection authoritative no matter who filled the list last.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void taczammobox$keepFiltered(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY,
            float partialTick, CallbackInfo ci) {
        try {
            if (!FeatureFlags.easterEggTab() || this.taczammobox$source.isEmpty()) {
                return;
            }
            if (taczammobox$gridMatchesSelection()) {
                return;
            }
            taczammobox$filterNow();
        } catch (Throwable t) {
            if (!taczammobox$warned) {
                taczammobox$warned = true;
                com.taczammobox.TaczAmmoBoxMod.LOGGER.warn("[taczammobox] creative filter disabled after an error", t);
            }
        }
    }

    /** True when every visible stack already matches the rail's current selection. */
    @Unique
    private boolean taczammobox$gridMatchesSelection() {
        boolean wantEaster = taczammobox$showEasterEgg;
        boolean seen = false;
        for (ItemStack stack : this.menu.items) {
            if (stack.isEmpty()) {
                continue;
            }
            seen = true;
            if (taczammobox$isEasterEgg(stack) != wantEaster) {
                return false;
            }
        }
        return seen;
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
        return stack.is(ModItems.AMMO_BELT.get()) || FeatureFlags.isTierMagazine(stack);
    }
}
