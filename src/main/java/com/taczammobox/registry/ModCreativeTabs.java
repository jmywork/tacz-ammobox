package com.taczammobox.registry;

import com.taczammobox.TaczAmmoBoxMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TaczAmmoBoxMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.taczammobox"))
            .icon(() -> new ItemStack(ModItems.BOXES.values().iterator().next().get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.PACKER.get());
                output.accept(ModItems.LOADER.get());
                ModItems.BOXES.values().forEach(box -> output.accept(box.get()));
                ModItems.CRATES.values().forEach(crate -> output.accept(crate.get()));
            })
            .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
