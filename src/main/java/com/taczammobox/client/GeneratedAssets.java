package com.taczammobox.client;

import com.taczammobox.AmmoType;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.registry.AmmoTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.zip.ZipFile;

/**
 * Generates a built-in resource pack for ammo types added through the config.
 *
 * <p>It writes the missing item models (the model loader otherwise logs a
 * FileNotFoundException and shows the purple/black missing model) and, when the
 * gun pack's {@code textures/ammo/slot/<id>.png} can be found, composites that
 * icon onto the box/crate top.</p>
 */
@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GeneratedAssets {
    private static final String PACK_ID = "taczammobox_generated";
    private static final String STAMP_FILE = ".generated-stamp";

    private GeneratedAssets() {
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        Path root = FMLPaths.CONFIGDIR.get().resolve("taczammobox").resolve("generated_resourcepack");
        try {
            generate(root);
        } catch (IOException exception) {
            TaczAmmoBoxMod.LOGGER.error("Failed to generate resource pack for config ammo types", exception);
            return;
        }

        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate(
                    PACK_ID,
                    Component.literal("TACZ Ammo Box generated assets"),
                    true,
                    id -> new PathPackResources(id, root, false),
                    PackType.CLIENT_RESOURCES,
                    Pack.Position.TOP,
                    PackSource.create(component -> component, true));
            if (pack != null) {
                consumer.accept(pack);
            } else {
                TaczAmmoBoxMod.LOGGER.warn("Could not register generated resource pack at {}", root);
            }
        });
    }

    private static void generate(Path root) throws IOException {
        Files.createDirectories(root);
        Path assets = root.resolve("assets").resolve(TaczAmmoBoxMod.MOD_ID);
        Path stampPath = root.resolve(STAMP_FILE);
        String stamp = buildStamp();

        if (Files.exists(stampPath)
                && Files.exists(root.resolve("pack.mcmeta"))
                && Files.isDirectory(assets.resolve("models").resolve("item"))
                && Files.exists(assets.resolve(".complete"))
                && stamp.equals(Files.readString(stampPath, StandardCharsets.UTF_8).trim())) {
            TaczAmmoBoxMod.LOGGER.info("Generated assets are up to date; skipping rebuild");
            return;
        }

        Files.deleteIfExists(stampPath);
        deleteRecursively(root.resolve("assets"));
        Files.createDirectories(assets.resolve("models").resolve("item"));
        Files.createDirectories(assets.resolve("textures").resolve("item").resolve("top"));

        Files.writeString(root.resolve("pack.mcmeta"),
                "{\"pack\":{\"pack_format\":15,\"description\":\"TACZ Ammo Box generated assets\"}}",
                StandardCharsets.UTF_8);

        Map<String, byte[]> icons = new HashMap<>();
        collectIcons(icons);

        int models = 0;
        int iconsUsed = 0;
        for (AmmoType type : AmmoTypes.all()) {
            if (AmmoTypes.isBuiltin(type.ammoId())) {
                continue;
            }
            String name = type.itemName();
            String iconName = type.ammoId().getPath() + ".png";
            byte[] icon = icons.get(iconName);
            if (icon == null) {
                icon = icons.get(name + ".png");
            }
            if (icon == null) {
                TaczAmmoBoxMod.LOGGER.warn("No ammo icon found for {} (looked for {})", type.ammoId(), iconName);
            }

            boolean boxIcon = writeTop(assets, "box", name, icon, "/assets/taczammobox/textures/item/box_top.png");
            boolean crateIcon = writeTop(assets, "crate", name, icon, "/assets/taczammobox/textures/item/crate_top.png");
            if (boxIcon) {
                iconsUsed++;
            }
            if (crateIcon) {
                iconsUsed++;
            }

            writeLabel(assets, "box", type);
            writeLabel(assets, "crate", type);

            String boxTop = boxIcon
                    ? "taczammobox:item/top/box_" + name
                    : "taczammobox:item/top/box_generic";
            String crateTop = crateIcon
                    ? "taczammobox:item/top/crate_" + name
                    : "taczammobox:item/top/crate_generic";

            writeModel(assets, "box_" + name, "taczammobox:item/box_base",
                    "taczammobox:item/label/box_" + name, boxTop);
            writeModel(assets, "crate_" + name, "taczammobox:item/crate_base",
                    "taczammobox:item/label/crate_" + name, crateTop);
            models += 2;
        }
        Files.writeString(stampPath, stamp, StandardCharsets.UTF_8);
        Files.writeString(assets.resolve(".complete"), "ok", StandardCharsets.UTF_8);
        TaczAmmoBoxMod.LOGGER.info("Generated {} item models and {} top textures for config ammo types",
                models, iconsUsed);
    }

    /**
     * Cheap metadata fingerprint: config types, mod version, TACZ jar, gun-pack
     * archives and loose ammo icons. If none changed, the whole rebuild is skipped.
     */
    private static String buildStamp() {
        StringBuilder data = new StringBuilder();
        data.append("mod=").append(ModList.get().getModContainerById(TaczAmmoBoxMod.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString()).orElse("dev")).append('\n');

        for (AmmoType type : AmmoTypes.all()) {
            if (AmmoTypes.isBuiltin(type.ammoId())) {
                continue;
            }
            data.append(type.ammoId()).append('|')
                    .append(type.itemName()).append('|')
                    .append(type.color()).append('|')
                    .append(type.displayName().getString()).append('\n');
        }

        var taczFile = ModList.get().getModFileById("tacz");
        if (taczFile != null) {
            appendFileMeta(data, taczFile.getFile().getFilePath());
        }

        Path[] gunPackDirs = {
                Minecraft.getInstance().gameDirectory.toPath().resolve("tacz"),
                FMLPaths.GAMEDIR.get().resolve("tacz"),
                FMLPaths.CONFIGDIR.get().resolve("tacz")
        };
        for (Path taczDir : gunPackDirs) {
            if (!Files.isDirectory(taczDir)) {
                data.append("missing=").append(taczDir).append('\n');
                continue;
            }
            try (var stream = Files.walk(taczDir, 10)) {
                for (Path path : stream.sorted().toList()) {
                    String normalized = path.toString().replace('\\', '/');
                    if (Files.isRegularFile(path)
                            && (normalized.endsWith(".zip")
                            || (normalized.endsWith(".png") && normalized.contains("/textures/ammo/slot/")))) {
                        appendFileMeta(data, path);
                    }
                }
            } catch (IOException exception) {
                data.append("unreadable=").append(taczDir).append('\n');
            }
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static void appendFileMeta(StringBuilder data, Path path) {
        try {
            FileTime modified = Files.getLastModifiedTime(path);
            data.append(path).append('|').append(Files.size(path)).append('|')
                    .append(modified.toMillis()).append('\n');
        } catch (IOException exception) {
            data.append("unreadable=").append(path).append('\n');
        }
    }

    private static void writeModel(Path assets, String name, String parent, String label, String top) throws IOException {
        String json = """
                {
                  "parent": "%s",
                  "textures": {
                    "label": "%s",
                    "top": "%s"
                  }
                }
                """.formatted(parent, label, top);
        Files.writeString(assets.resolve("models").resolve("item").resolve(name + ".json"),
                json, StandardCharsets.UTF_8);
    }

    private static void writeLabel(Path assets, String kind, AmmoType type) throws IOException {
        int size = 32;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(new Color(46, 50, 54));
        graphics.fillRect(0, 0, size, size);
        graphics.setColor(new Color(90, 95, 99));
        graphics.drawRect(1, 1, size - 3, size - 3);
        graphics.setColor(new Color(type.color()));
        graphics.fillRect(3, size - 6, size - 6, 3);

        String text = type.displayName().getString();
        if (text.isBlank()) {
            text = type.itemName().toUpperCase();
        }
        int fontSize = 8;
        java.awt.Font font = new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, fontSize);
        while (fontSize > 4 && graphics.getFontMetrics(font).stringWidth(text) > size - 6) {
            fontSize--;
            font = new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, fontSize);
        }
        graphics.setFont(font);
        int width = graphics.getFontMetrics(font).stringWidth(text);
        graphics.setColor(Color.WHITE);
        graphics.drawString(text, (size - width) / 2, size / 2 + graphics.getFontMetrics(font).getAscent() / 2 - 2);
        graphics.dispose();

        Path labelDir = assets.resolve("textures").resolve("item").resolve("label");
        Files.createDirectories(labelDir);
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        ImageIO.write(image, "PNG", output);
        Files.write(labelDir.resolve(kind + "_" + type.itemName() + ".png"), output.toByteArray());
    }

    private static boolean writeTop(Path assets, String kind, String name, byte[] icon, String basePath) throws IOException {
        if (icon == null) {
            return false;
        }
        byte[] composed = composite(basePath, icon);
        if (composed == null) {
            return false;
        }
        Files.write(assets.resolve("textures").resolve("item").resolve("top").resolve(kind + "_" + name + ".png"), composed);
        return true;
    }

    private static byte[] composite(String basePath, byte[] iconBytes) throws IOException {
        try (InputStream baseStream = GeneratedAssets.class.getResourceAsStream(basePath)) {
            if (baseStream == null) {
                return null;
            }
            BufferedImage base = ImageIO.read(baseStream);
            BufferedImage icon = ImageIO.read(new ByteArrayInputStream(iconBytes));
            if (base == null || icon == null) {
                return null;
            }
            Graphics2D graphics = base.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setColor(new Color(46, 50, 54));
            graphics.fillRect(2, 2, 28, 28);
            graphics.drawImage(icon, 2, 2, 28, 28, null);
            graphics.dispose();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(base, "PNG", output);
            return output.toByteArray();
        }
    }

    private static void collectIcons(Map<String, byte[]> icons) {
        // Only the TACZ mod jar and the gun-pack directory are relevant; walking
        // every installed mod jar here was unnecessary startup work.
        var taczFile = ModList.get().getModFileById("tacz");
        if (taczFile != null) {
            scanZip(taczFile.getFile().getFilePath(), icons);
        }

        Path[] gunPackDirs = {
                Minecraft.getInstance().gameDirectory.toPath().resolve("tacz"),
                FMLPaths.GAMEDIR.get().resolve("tacz"),
                FMLPaths.CONFIGDIR.get().resolve("tacz")
        };
        for (Path taczDir : gunPackDirs) {
            if (!Files.isDirectory(taczDir)) {
                continue;
            }
            try (var stream = Files.walk(taczDir, 10)) {
                for (Path path : stream.toList()) {
                    if (Files.isRegularFile(path) && path.toString().endsWith(".zip")) {
                        scanZip(path, icons);
                    } else if (Files.isRegularFile(path) && path.toString().endsWith(".png")
                            && path.toString().replace('\\', '/').contains("/textures/ammo/slot/")) {
                        icons.putIfAbsent(path.getFileName().toString(), Files.readAllBytes(path));
                    }
                }
            } catch (IOException exception) {
                TaczAmmoBoxMod.LOGGER.warn("Failed to scan {} for ammo icons", taczDir, exception);
            }
        }
        TaczAmmoBoxMod.LOGGER.info("Indexed {} TACZ ammo icons from gun packs", icons.size());
    }

    private static void scanZip(Path path, Map<String, byte[]> icons) {
        try (ZipFile zip = new ZipFile(path.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                String entryName = entry.getName();
                if (entry.isDirectory() || !entryName.endsWith(".png")
                        || !entryName.contains("/textures/ammo/slot/")) {
                    continue;
                }
                String fileName = entryName.substring(entryName.lastIndexOf('/') + 1);
                if (icons.containsKey(fileName)) {
                    continue;
                }
                try (InputStream stream = zip.getInputStream(entry)) {
                    icons.put(fileName, stream.readAllBytes());
                }
            }
        } catch (IOException ignored) {
            // Not every mod file is a readable zip; skip it.
        }
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
