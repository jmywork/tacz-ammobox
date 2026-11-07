package com.taczammobox.registry;

import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.entity.SatelliteGunEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TaczAmmoBoxMod.MOD_ID);

    public static final RegistryObject<EntityType<SatelliteGunEntity>> SATELLITE_GUN =
            ENTITIES.register("satellite_gun", () -> EntityType.Builder
                    .<SatelliteGunEntity>of(SatelliteGunEntity::new, MobCategory.MISC)
                    .sized(0.7F, 0.7F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("taczammobox:satellite_gun"));

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}
