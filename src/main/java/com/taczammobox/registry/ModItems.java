package com.taczammobox.registry;

import com.taczammobox.AmmoType;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.item.AmmoBoxItem;
import com.taczammobox.item.CrateBlockItem;
import com.taczammobox.item.DynamicCrateBlockItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TaczAmmoBoxMod.MOD_ID);

    /** Resolved by {@link AmmoTypes#init()} before this class is first touched. */
    public static final List<AmmoType> TYPES = AmmoTypes.all();

    public static final Map<ResourceLocation, RegistryObject<Item>> BOXES = new LinkedHashMap<>();
    public static final Map<ResourceLocation, RegistryObject<Item>> CRATES = new LinkedHashMap<>();

    /**
     * Hidden model anchors. Ammo types added through the config have no static
     * model file; the client fallback copies these baked models onto them.
     */
    public static final RegistryObject<Item> BOX_GENERIC =
            ITEMS.register("box_generic", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CRATE_GENERIC =
            ITEMS.register("crate_generic", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> PACKER =
            ITEMS.register("ammo_packer", () -> new BlockItem(ModBlocks.PACKER.get(), new Item.Properties()));

    public static final RegistryObject<Item> LOADER =
            ITEMS.register("ammo_loader", () -> new BlockItem(ModBlocks.LOADER.get(), new Item.Properties()));

    static {
        for (AmmoType type : TYPES) {
            BOXES.put(type.ammoId(), ITEMS.register("box_" + type.itemName(),
                    () -> new AmmoBoxItem(type, new Item.Properties().stacksTo(60))));
            if (ModBlocks.CRATES.containsKey(type.ammoId())) {
                CRATES.put(type.ammoId(), ITEMS.register("crate_" + type.itemName(),
                        () -> new CrateBlockItem(ModBlocks.CRATES.get(type.ammoId()).get(), type,
                                new Item.Properties().stacksTo(16))));
            } else {
                // Config-added types share one dynamic crate block; the type is stored in its block entity.
                CRATES.put(type.ammoId(), ITEMS.register("crate_" + type.itemName(),
                        () -> new DynamicCrateBlockItem(ModBlocks.DYNAMIC_CRATE.get(), type,
                                new Item.Properties().stacksTo(16))));
            }
        }
    }

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static Item box(ResourceLocation ammoId) {
        RegistryObject<Item> item = BOXES.get(ammoId);
        return item == null ? null : item.get();
    }

    public static Item crate(ResourceLocation ammoId) {
        RegistryObject<Item> item = CRATES.get(ammoId);
        return item == null ? null : item.get();
    }
}
