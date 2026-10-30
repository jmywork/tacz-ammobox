package com.taczammobox.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.taczammobox.AmmoType;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.registry.AmmoTypes;
import com.taczammobox.registry.ModItems;
import com.taczammobox.util.AmmoBoxHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Writes the third-party box/crate recipes as a normal data pack. The recipe
 * reload listener still has a runtime injection fallback for environments where
 * this pack cannot be loaded.
 */
@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GeneratedRecipes {
    private static final String PACK_ID = "taczammobox_generated_data";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private GeneratedRecipes() {
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }
        Path root = FMLPaths.CONFIGDIR.get().resolve("taczammobox").resolve("generated_datapack");
        try {
            generate(root);
        } catch (IOException exception) {
            TaczAmmoBoxMod.LOGGER.error("Failed to generate third-party recipe data pack", exception);
            return;
        }
        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate(
                    PACK_ID,
                    Component.literal("TACZ Ammo Box generated recipes"),
                    true,
                    id -> new PathPackResources(id, root, false),
                    PackType.SERVER_DATA,
                    Pack.Position.TOP,
                    PackSource.create(component -> component, true));
            if (pack != null) {
                consumer.accept(pack);
            } else {
                TaczAmmoBoxMod.LOGGER.warn("Could not register generated recipe data pack at {}", root);
            }
        });
    }

    private static void generate(Path root) throws IOException {
        Files.createDirectories(root);
        Path data = root.resolve("data").resolve(TaczAmmoBoxMod.MOD_ID).resolve("recipes").resolve("generated");
        deleteRecursively(root.resolve("data"));
        Files.createDirectories(data);
        Files.writeString(root.resolve("pack.mcmeta"),
                "{\"pack\":{\"pack_format\":15,\"description\":\"TACZ Ammo Box generated recipes\"}}",
                StandardCharsets.UTF_8);

        int written = 0;
        for (AmmoType type : AmmoTypes.all()) {
            if (AmmoTypes.isBuiltin(type.ammoId())) {
                continue;
            }
            Item box = ModItems.box(type.ammoId());
            Item crate = ModItems.crate(type.ammoId());
            if (box == null || crate == null) {
                TaczAmmoBoxMod.LOGGER.warn("Skipping generated recipes for {}: missing box/crate item", type.ammoId());
                continue;
            }
            String name = type.itemName();
            String boxId = TaczAmmoBoxMod.MOD_ID + ":box_" + name;
            String crateId = TaczAmmoBoxMod.MOD_ID + ":crate_" + name;

            writeShapeless(data.resolve(name + "_crate.json"), crateId, 1, boxId, 9);
            writeShapeless(data.resolve(name + "_boxes.json"), boxId, 9, crateId, 1);

            JsonObject ammoResult = new JsonObject();
            ammoResult.addProperty("item", "tacz:ammo");
            ammoResult.addProperty("count", AmmoBoxHelper.roundsPerBox(type));
            ammoResult.addProperty("nbt", "{AmmoId:\"" + type.ammoId() + "\"}");
            writeShapeless(data.resolve(name + "_ammo.json"), ammoResult, boxId, 1);
            written += 3;
        }
        TaczAmmoBoxMod.LOGGER.info("Generated {} third-party recipe files", written);
    }

    private static void writeShapeless(Path file, String resultItem, int resultCount,
                                       String ingredientItem, int ingredientCount) throws IOException {
        JsonObject result = new JsonObject();
        result.addProperty("item", resultItem);
        if (resultCount != 1) {
            result.addProperty("count", resultCount);
        }
        writeShapeless(file, result, ingredientItem, ingredientCount);
    }

    private static void writeShapeless(Path file, JsonObject result,
                                       String ingredientItem, int ingredientCount) throws IOException {
        JsonArray ingredients = new JsonArray();
        for (int i = 0; i < ingredientCount; i++) {
            JsonObject ingredient = new JsonObject();
            ingredient.addProperty("item", ingredientItem);
            ingredients.add(ingredient);
        }
        JsonObject recipe = new JsonObject();
        recipe.addProperty("type", "minecraft:crafting_shapeless");
        recipe.addProperty("category", "misc");
        recipe.add("ingredients", ingredients);
        recipe.add("result", result);
        Files.writeString(file, GSON.toJson(recipe), StandardCharsets.UTF_8);
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            for (Path file : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(file);
            }
        }
    }
}
