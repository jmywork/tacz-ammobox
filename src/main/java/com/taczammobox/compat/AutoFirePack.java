package com.taczammobox.compat;

import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.util.AutoFireTier;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Installs a minimal TACZ gun pack that adds the three auto-fire magazine tiers.
 *
 * <p>TACZ scans {@code <game dir>/tacz/} for gun packs at startup, so the pack has to
 * exist on disk before the resource reload. Files are rewritten only when their
 * content changes.</p>
 */
public final class AutoFirePack {
    /** Folder under {@code <game dir>/tacz/} that holds the generated pack. */
    public static final String PACK_DIR = "taczammobox_autofire";

    private static final String META = """
            {
              "namespace": "taczammobox"
            }
            """;

    private AutoFirePack() {
    }

    public static void install() {
        Path root = FMLPaths.GAMEDIR.get().resolve("tacz").resolve(PACK_DIR);
        write(root.resolve("gunpack.meta.json"), META);
        for (AutoFireTier tier : AutoFireTier.values()) {
            String path = tier.path();
            write(root.resolve("data/taczammobox/index/attachments/" + path + ".json"), index(tier));
            write(root.resolve("data/taczammobox/data/attachments/" + path + "_data.json"), data(tier));
            write(root.resolve("assets/taczammobox/display/attachments/" + path + "_display.json"), display(tier));
        }
    }

    private static String index(AutoFireTier tier) {
        return """
                {
                  "name": "taczammobox.attachment.%s.name",
                  "display": "taczammobox:%s_display",
                  "data": "taczammobox:%s_data",
                  "type": "extended_mag",
                  "sort": %d
                }
                """.formatted(tier.path(), tier.path(), tier.path(), 10 + tier.level());
    }

    private static String data(AutoFireTier tier) {
        return """
                {
                  "weight": %s,
                  "extended_mag_level": %d
                }
                """.formatted(switch (tier.level()) {
                    case 2 -> "0.15";
                    case 3 -> "0.3";
                    default -> "0.05";
                }, tier.level());
    }

    private static String display(AutoFireTier tier) {
        return """
                {
                  "slot": "tacz:attachment/slot/sniper_extended_mag_%d",
                  "sounds": {
                    "install": "tacz:attachments/extended_mag_install",
                    "uninstall": "tacz:attachments/extended_mag_uninstall"
                  }
                }
                """.formatted(tier.level());
    }

    private static void write(Path file, String content) {
        try {
            if (Files.exists(file) && Files.readString(file, StandardCharsets.UTF_8).equals(content)) {
                return;
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
            TaczAmmoBoxMod.LOGGER.info("Wrote TACZ gun pack file {}", file);
        } catch (Exception exception) {
            TaczAmmoBoxMod.LOGGER.error("Failed to write TACZ gun pack file {}", file, exception);
        }
    }
}
