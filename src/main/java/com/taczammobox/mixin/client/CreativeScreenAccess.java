package com.taczammobox.mixin.client;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Accessor for the private creative screen refresh hook. */
@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeScreenAccess {
    @Invoker("refreshSearchResults")
    void taczammobox$refresh();
}
