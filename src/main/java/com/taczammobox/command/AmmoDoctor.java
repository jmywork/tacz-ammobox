package com.taczammobox.command;

import com.taczammobox.AmmoType;
import com.taczammobox.compat.AmmoScanner;
import com.taczammobox.registry.AmmoTypes;
import com.taczammobox.registry.ModItems;
import com.taczammobox.config.WasteSettings;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Lightweight, read-only health checks for the ammo config and generated assets. */
public final class AmmoDoctor {
    private AmmoDoctor() {
    }

    public static List<String> report() {
        List<String> lines = new ArrayList<>();
        List<AmmoType> types = AmmoTypes.all();
        int boxes = 0;
        int crates = 0;
        List<String> missing = new ArrayList<>();
        for (AmmoType type : types) {
            if (ModItems.box(type.ammoId()) != null) {
                boxes++;
            } else {
                missing.add("box " + type.ammoId());
            }
            if (ModItems.crate(type.ammoId()) != null) {
                crates++;
            } else {
                missing.add("crate " + type.ammoId());
            }
        }
        lines.add("[doctor] ammo types: " + types.size() + ", boxes: " + boxes + ", crates: " + crates);
        if (!missing.isEmpty()) {
            lines.add("[doctor] missing items: " + String.join(", ", missing));
        }

        Set<String> capacityKeys = WasteSettings.capacityKeys();
        Set<String> multiplierKeys = WasteSettings.multiplierKeys();
        List<String> malformed = new ArrayList<>();
        for (String key : capacityKeys) {
            validateKey(key, malformed);
        }
        for (String key : multiplierKeys) {
            validateKey(key, malformed);
        }
        lines.add("[doctor] capacity overrides: " + capacityKeys.size()
                + ", multipliers: " + multiplierKeys.size());
        if (!malformed.isEmpty()) {
            lines.add("[doctor] malformed keys: " + String.join(", ", malformed));
        }

        Path pack = FMLPaths.CONFIGDIR.get().resolve("taczammobox")
                .resolve("generated_datapack").resolve("pack.mcmeta");
        lines.add("[doctor] generated recipe pack: " + (Files.isRegularFile(pack) ? "OK" : "missing"));
        lines.add("[doctor] DCSharp: " + (ModList.get().isLoaded("dcsharp") ? "detected" : "not installed"));
        var orphans = AmmoScanner.findOrphans();
        lines.add("[doctor] orphan ammo entries: " + orphans.size()
                + (orphans.isEmpty() ? "" : " (" + String.join(", ", orphans.stream()
                .map(ResourceLocation::toString).toList()) + ")"));
        lines.add("features: " + com.taczammobox.config.FeatureFlags.describe());
        lines.add("guards: " + com.taczammobox.util.FeatureGuard.describe());
        return lines;
    }

    private static void validateKey(String key, List<String> malformed) {
        String[] parts = key.split("#", -1);
        if (parts.length < 1 || parts.length > 3 || ResourceLocation.tryParse(parts[0]) == null) {
            malformed.add(key);
            return;
        }
        if (parts.length >= 2) {
            try {
                int level = Integer.parseInt(parts[1]);
                if (level < 0) {
                    malformed.add(key);
                }
            } catch (NumberFormatException exception) {
                malformed.add(key);
                return;
            }
        }
        if (parts.length == 3 && ResourceLocation.tryParse(parts[2]) == null) {
            malformed.add(key);
        }
    }
}
