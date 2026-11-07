package com.taczammobox.mixin.client;

import com.tacz.guns.api.item.IAttachment;
import com.taczammobox.config.FeatureFlags;
import com.taczammobox.registry.ModCreativeTabs;
import com.taczammobox.registry.ModItems;
import com.taczammobox.util.AutoFireTier;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the 正常内容 / 彩蛋 filter rail next to the creative item grid, the same idea as
 * TACZ's gun pack filters. Our tab carries both normal items and easter eggs, and the
 * two buttons decide which half the grid shows.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeTabFilterMixin
        extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu>
        implements CreativeScreenAccess {

    @Shadow
    private static CreativeModeTab selectedTab;

    @Unique
    private static boolean taczammobox$showEasterEgg;

    @Unique
    private Button taczammobox$normalButton;

    @Unique
    private Button taczammobox$easterButton;

    protected CreativeTabFilterMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void taczammobox$addFilterButtons(CallbackInfo ci) {
        int x = Math.max(2, this.leftPos - 58);
        int y = this.topPos + 6;
        this.taczammobox$normalButton = Button.builder(
                        Component.translatable("gui.taczammobox.creative.normal"), b -> taczammobox$setFilter(false))
                .bounds(x, y, 54, 20).build();
        this.taczammobox$easterButton = Button.builder(
                        Component.translatable("gui.taczammobox.creative.easter"), b -> taczammobox$setFilter(true))
                .bounds(x, y + 22, 54, 20).build();
        this.addRenderableWidget(this.taczammobox$normalButton);
        this.addRenderableWidget(this.taczammobox$easterButton);
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
    private static boolean taczammobox$warned;

    @Unique
    private void taczammobox$filterNow() {
        boolean ours = selectedTab == ModCreativeTabs.MAIN.get();
        boolean filterable = ours && FeatureFlags.easterEggTab();
        if (this.taczammobox$normalButton != null) {
            this.taczammobox$normalButton.visible = filterable;
            this.taczammobox$normalButton.active = taczammobox$showEasterEgg;
        }
        if (this.taczammobox$easterButton != null) {
            this.taczammobox$easterButton.visible = filterable;
            this.taczammobox$easterButton.active = !taczammobox$showEasterEgg;
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

    @Unique
    private static boolean taczammobox$isEasterEgg(ItemStack stack) {
        if (stack.is(ModItems.AMMO_BELT.get())) {
            return true;
        }
        return stack.getItem() instanceof IAttachment attachment
                && AutoFireTier.of(attachment.getAttachmentId(stack)) != null;
    }
}
