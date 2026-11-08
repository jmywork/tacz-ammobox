package com.taczammobox.compat;

import com.taczammobox.TaczAmmoBoxMod;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Temporary probe: tells us whether a satellite bullet ever lands a hit at all.
 */
@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID)
public final class SatelliteHitProbe {
    private static int logged;

    private SatelliteHitProbe() {
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getDirectEntity() instanceof EntityKineticBullet bullet && logged < 20) {
            logged++;
            LivingEntity victim = event.getEntity();
            TaczAmmoBoxMod.LOGGER.info("sat hit: victim={} gun={} dmg={} attacker={}",
                    victim.getType().builtInRegistryHolder().key().location(),
                    bullet.getGunId(),
                    event.getAmount(),
                    event.getSource().getEntity() == null ? "null"
                            : event.getSource().getEntity().getClass().getSimpleName());
        }
    }
}
