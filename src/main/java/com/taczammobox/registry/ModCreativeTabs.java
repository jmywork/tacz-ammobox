package com.taczammobox.registry;

import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.config.FeatureFlags;
import com.taczammobox.util.AutoFireTier;
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

    /**
     * Everything lives in one tab, production items first and easter eggs at the end.
     * A second tab looked tidy in the code but players never found the toys.
     */
    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.taczammobox"))
            .icon(() -> new ItemStack(ModItems.BOXES.values().iterator().next().get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.PACKER.get());
                output.accept(ModItems.LOADER.get());
                output.accept(ModItems.AMMO_CHAIN.get());
                output.accept(ModItems.ASSEMBLER.get());
                output.accept(ModItems.SATELLITE_RACK_UPKEEP.get());
                // Toys go right after the machines, not at the bottom of 60 ammo entries.
                if (FeatureFlags.easterEggTab()) {
                    output.accept(ModItems.AMMO_BELT.get());
                    output.accept(ModItems.SATELLITE_RACK.get());
                    for (AutoFireTier tier : AutoFireTier.all()) {
                        ItemStack attachment = AttachmentItemBuilder.create().setId(tier.id()).build();
                        if (!attachment.isEmpty()) {
                            output.accept(attachment);
                        }
                    }
                }
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
