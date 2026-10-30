package com.taczammobox.registry;

import com.taczammobox.AmmoType;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.block.AmmoLoaderBlock;
import com.taczammobox.block.AmmoPackerBlock;
import com.taczammobox.block.CrateBlock;
import com.taczammobox.block.DynamicCrateBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Decorative ammo crates. Only built-in ammo types get a block: a block needs a
 * static blockstate/model/loot table, which config-only types cannot provide.
 */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, TaczAmmoBoxMod.MOD_ID);

    public static final Map<ResourceLocation, RegistryObject<Block>> CRATES = new LinkedHashMap<>();

    public static final RegistryObject<Block> PACKER = BLOCKS.register("ammo_packer",
            () -> new AmmoPackerBlock(BlockBehaviour.Properties.of()
                    .strength(2.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> LOADER = BLOCKS.register("ammo_loader",
            () -> new AmmoLoaderBlock(BlockBehaviour.Properties.of()
                    .strength(2.0F)
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> DYNAMIC_CRATE = BLOCKS.register("crate_dynamic",
            () -> new DynamicCrateBlock(BlockBehaviour.Properties.of()
                    .strength(1.2F)
                    .sound(SoundType.WOOD)));

    static {
        for (AmmoType type : AmmoTypes.all()) {
            if (!AmmoTypes.isBuiltin(type.ammoId())) {
                continue;
            }
            CRATES.put(type.ammoId(), BLOCKS.register("crate_" + type.itemName(),
                    () -> new CrateBlock(type, BlockBehaviour.Properties.of()
                            .strength(1.2F)
                            .sound(SoundType.WOOD))));
        }
    }

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
