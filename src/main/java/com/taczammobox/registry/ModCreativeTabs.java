package com.taczammobox.registry;

import com.tacz.guns.api.item.builder.AttachmentItemBuilder;
import com.taczammobox.TaczAmmoBoxMod;
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

    /** Regular production items: machines, boxes, crates. */
    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.taczammobox"))
            .icon(() -> new ItemStack(ModItems.BOXES.values().iterator().next().get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.PACKER.get());
                output.accept(ModItems.LOADER.get());
                output.accept(ModItems.AMMO_CHAIN.get());
                ModItems.BOXES.values().forEach(box -> output.accept(box.get()));
                ModItems.CRATES.values().forEach(crate -> output.accept(crate.get()));
            })
            .build());

    /**
     * Easter eggs: unbalanced toys that deliberately have no recipe. Players are expected
     * to write their own recipes if they want them in survival.
     */
    public static final RegistryObject<CreativeModeTab> EASTER_EGG = TABS.register("easter_egg",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.taczammobox.easter_egg"))
                    .icon(() -> new ItemStack(ModItems.AMMO_BELT.get()))
                    .displayItems((parameters, output) -> {
                        if (!com.taczammobox.config.FeatureFlags.easterEggTab()) {
                            return;
                        }
                        output.accept(ModItems.AMMO_BELT.get());
                        for (AutoFireTier tier : AutoFireTier.all()) {
                            ItemStack attachment = AttachmentItemBuilder.create().setId(tier.id()).build();
                            if (!attachment.isEmpty()) {
                                output.accept(attachment);
                            }
                        }
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
