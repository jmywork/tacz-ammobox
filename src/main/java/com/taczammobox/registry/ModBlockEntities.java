package com.taczammobox.registry;

import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.block.AmmoLoaderBlockEntity;
import com.taczammobox.block.AmmoPackerBlockEntity;
import com.taczammobox.block.DynamicCrateBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TaczAmmoBoxMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<AmmoPackerBlockEntity>> PACKER =
            BLOCK_ENTITIES.register("ammo_packer",
                    () -> BlockEntityType.Builder.of(AmmoPackerBlockEntity::new, ModBlocks.PACKER.get()).build(null));

    public static final RegistryObject<BlockEntityType<AmmoLoaderBlockEntity>> LOADER =
            BLOCK_ENTITIES.register("ammo_loader",
                    () -> BlockEntityType.Builder.of(AmmoLoaderBlockEntity::new, ModBlocks.LOADER.get()).build(null));

    public static final RegistryObject<BlockEntityType<DynamicCrateBlockEntity>> DYNAMIC_CRATE =
            BLOCK_ENTITIES.register("crate_dynamic",
                    () -> BlockEntityType.Builder.of(DynamicCrateBlockEntity::new, ModBlocks.DYNAMIC_CRATE.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
