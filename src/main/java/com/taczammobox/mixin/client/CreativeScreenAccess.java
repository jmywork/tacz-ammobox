package com.taczammobox.mixin.client;

import org.spongepowered.asm.mixin.gen.Invoker;

/** Accessor for the private creative screen refresh hook. */
public interface CreativeScreenAccess {
    @Invoker("refreshSearchResults")
    void taczammobox$refresh();
}
