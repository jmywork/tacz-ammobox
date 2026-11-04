package com.taczammobox.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmoBox;
import com.taczammobox.TaczAmmoBoxMod;
import com.taczammobox.compat.AmmoScanner;
import com.taczammobox.config.WasteSettings;
import com.taczammobox.network.ModNetwork;
import com.taczammobox.registry.AmmoTypes;
import com.taczammobox.util.AmmoBoxHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.StringJoiner;

@Mod.EventBusSubscriber(modid = TaczAmmoBoxMod.MOD_ID)
public final class AmmoBoxCommand {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private AmmoBoxCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("taczammobox")
                .then(Commands.literal("help")
                        .executes(context -> showHelp(context.getSource())))
                .then(Commands.literal("pack")
                        .executes(context -> pack(context.getSource())))
                .then(Commands.literal("packtoggle")
                        .executes(context -> showPackToggle(context.getSource()))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> setPackToggle(context.getSource(),
                                        BoolArgumentType.getBool(context, "value")))))
                .then(Commands.literal("priority")
                        .executes(context -> showPriority(context.getSource()))
                        .then(Commands.argument("order", StringArgumentType.greedyString())
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> setPriority(context.getSource(),
                                        StringArgumentType.getString(context, "order")))))
                .then(Commands.literal("dump")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> dump(context.getSource())))
                .then(Commands.literal("scan")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> scan(context.getSource())))
                .then(Commands.literal("clean")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> clean(context.getSource())))
                .then(Commands.literal("cleanup")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> cleanup(context.getSource(), 64))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                                .executes(context -> cleanup(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "radius")))))
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal("doctor")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> doctor(context.getSource())))
                .then(Commands.literal("reloadrecipes")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> reloadRecipes(context.getSource())))
                .then(Commands.literal("capacity")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("held")
                                .then(Commands.argument("capacity", IntegerArgumentType.integer(1, 1_000_000))
                                        .executes(context -> setHeldCapacity(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "capacity")))))
                        .then(Commands.literal("heldlevel")
                                .then(Commands.argument("capacity", IntegerArgumentType.integer(1, 1_000_000))
                                        .executes(context -> setHeldLevelCapacity(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "capacity")))))
                        .then(Commands.literal("heldmult")
                                .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0.001, 1000.0))
                                        .executes(context -> setHeldMultiplier(context.getSource(),
                                                DoubleArgumentType.getDouble(context, "multiplier")))))
                        .then(Commands.literal("mult")
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
                                                .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0.001, 1000.0))
                                                        .executes(context -> setMultiplier(context.getSource(),
                                                                ItemArgument.getItem(context, "item").getItem(),
                                                                IntegerArgumentType.getInteger(context, "level"),
                                                                DoubleArgumentType.getDouble(context, "multiplier")))))))
                        .then(Commands.literal("multall")
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0.001, 1000.0))
                                                .executes(context -> setAllMultiplier(context.getSource(),
                                                        ItemArgument.getItem(context, "item").getItem(),
                                                        DoubleArgumentType.getDouble(context, "multiplier"))))))
                        .then(Commands.literal("setammomult")
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
                                                .then(Commands.argument("ammo", ResourceLocationArgument.id())
                                                        .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0.001, 1000.0))
                                                                .executes(context -> setAmmoMultiplier(context.getSource(),
                                                                        ItemArgument.getItem(context, "item").getItem(),
                                                                        IntegerArgumentType.getInteger(context, "level"),
                                                                        ResourceLocationArgument.getId(context, "ammo"),
                                                                        DoubleArgumentType.getDouble(context, "multiplier"))))))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
                                                .then(Commands.argument("capacity", IntegerArgumentType.integer(1, 1_000_000))
                                                        .executes(context -> setCapacity(context.getSource(),
                                                                ItemArgument.getItem(context, "item").getItem(),
                                                                IntegerArgumentType.getInteger(context, "level"),
                                                                IntegerArgumentType.getInteger(context, "capacity")))))))
                        .then(Commands.literal("setall")
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .then(Commands.argument("capacity", IntegerArgumentType.integer(1, 1_000_000))
                                                .executes(context -> setAllCapacity(context.getSource(),
                                                        ItemArgument.getItem(context, "item").getItem(),
                                                        IntegerArgumentType.getInteger(context, "capacity"))))))
                        .then(Commands.literal("setammo")
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
                                                .then(Commands.argument("ammo", ResourceLocationArgument.id())
                                                        .then(Commands.argument("capacity", IntegerArgumentType.integer(1, 1_000_000))
                                                                .executes(context -> setAmmoCapacity(context.getSource(),
                                                                        ItemArgument.getItem(context, "item").getItem(),
                                                                        IntegerArgumentType.getInteger(context, "level"),
                                                                        ResourceLocationArgument.getId(context, "ammo"),
                                                                        IntegerArgumentType.getInteger(context, "capacity"))))))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
                                                .executes(context -> clearCapacity(context.getSource(),
                                                        ItemArgument.getItem(context, "item").getItem(),
                                                        IntegerArgumentType.getInteger(context, "level"))))))
                        .then(Commands.literal("clearammo")
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
                                                .then(Commands.argument("ammo", ResourceLocationArgument.id())
                                                        .executes(context -> clearAmmoCapacity(context.getSource(),
                                                                ItemArgument.getItem(context, "item").getItem(),
                                                                IntegerArgumentType.getInteger(context, "level"),
                                                                ResourceLocationArgument.getId(context, "ammo")))))))
                        .then(Commands.literal("clearall")
                                .then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                                        .executes(context -> clearAllCapacity(context.getSource(),
                                                ItemArgument.getItem(context, "item").getItem()))))
                        .then(Commands.literal("list")
                                .executes(context -> listCapacity(context.getSource())))
                        .then(Commands.literal("rounding")
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .executes(context -> setRounding(context.getSource(),
                                                StringArgumentType.getString(context, "mode"))))))
                .then(Commands.literal("autorefill")
                        .executes(context -> showAutoRefill(context.getSource()))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> setAutoRefill(context.getSource(),
                                        BoolArgumentType.getBool(context, "value")))))
                .then(Commands.literal("waste")
                        .executes(context -> showWaste(context.getSource()))
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> setWaste(context.getSource(),
                                        BoolArgumentType.getBool(context, "value"))))));
    }

    private static int showHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.taczammobox.help.header"), false);
        source.sendSuccess(() -> Component.translatable("command.taczammobox.help.config"), false);
        source.sendSuccess(() -> Component.translatable("command.taczammobox.help.mult"), false);
        source.sendSuccess(() -> Component.translatable("command.taczammobox.help.absolute"), false);
        source.sendSuccess(() -> Component.translatable("command.taczammobox.help.list"), false);
        source.sendSuccess(() -> Component.translatable("command.taczammobox.help.docs"), false);
        source.sendSuccess(() -> Component.translatable("command.taczammobox.help.sort"), false);
        source.sendSuccess(() -> Component.translatable("command.taczammobox.help.priority"), false);
        source.sendSuccess(() -> Component.translatable("command.taczammobox.help.doctor"), false);
        source.sendSuccess(() -> Component.translatable("command.taczammobox.help.reloadrecipes"), false);
        source.sendSuccess(() -> Component.translatable("command.taczammobox.help.cleanup"), false);
        source.sendSuccess(() -> Component.translatable("command.taczammobox.help.clean"), false);
        source.sendSuccess(() -> Component.translatable("command.taczammobox.help.packtoggle"), false);
        return 1;
    }

    private static int pack(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.taczammobox.capacity.player_only"));
            return 0;
        }
        int packed = AmmoBoxHelper.packLooseAmmo(player.level(), player);
        if (player.containerMenu != null) {
            player.containerMenu.broadcastChanges();
        }
        TaczAmmoBoxMod.LOGGER.info("Pack command by {}: {} boxes", player.getGameProfile().getName(), packed);
        source.sendSuccess(() -> Component.literal("pack result: " + packed + " boxes"), false);
        return 1;
    }

    private static int showPriority(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.taczammobox.priority",
                String.join(", ", WasteSettings.boxPriority())), false);
        return 1;
    }

    private static int setPriority(CommandSourceStack source, String raw) {
        if (!WasteSettings.setBoxPriority(raw)) {
            source.sendFailure(Component.translatable("command.taczammobox.priority.invalid"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.taczammobox.priority",
                String.join(", ", WasteSettings.boxPriority())), true);
        return 1;
    }

    private static int showAutoRefill(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable(
                "command.taczammobox.autorefill", autoRefillState()), false);
        return 1;
    }

    private static int showPackToggle(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable(
                "command.taczammobox.packtoggle", autoRefillState()), false);
        return 1;
    }

    private static int setPackToggle(CommandSourceStack source, boolean value) {
        WasteSettings.setOneKeyPack(value);
        source.sendSuccess(() -> Component.translatable(
                "command.taczammobox.packtoggle", autoRefillState()), true);
        return 1;
    }

    private static int setAutoRefill(CommandSourceStack source, boolean value) {
        WasteSettings.setAutoRefill(value);
        source.sendSuccess(() -> Component.translatable(
                "command.taczammobox.autorefill", autoRefillState()), true);
        return 1;
    }

    private static Component autoRefillState() {
        return Component.translatable(WasteSettings.autoRefill()
                ? "command.taczammobox.state.on"
                : "command.taczammobox.state.off");
    }

    private static int reload(CommandSourceStack source) {
        WasteSettings.load();
        com.taczammobox.config.FeatureFlags.load();
        com.taczammobox.util.AutoFireTier.init();
        com.taczammobox.config.AmmoFactorySettings.load();
        com.taczammobox.compat.CompatSelfCheck.run();
        if (com.taczammobox.config.FeatureFlags.autoFire()) {
            com.taczammobox.compat.AutoFirePack.install();
        }
        AmmoBoxHelper.clearCapacityCache();
        com.taczammobox.compat.DCSharpCompat.clearCache();
        if (source.getServer() != null) {
            for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                ModNetwork.sendSettings(player);
            }
        }
        source.sendSuccess(() -> Component.literal("settings.json + features.json reloaded"), true);
        return 1;
    }

    private static int doctor(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("[doctor] TACZ Ammo Box"), false);
        for (String line : AmmoDoctor.report()) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int reloadRecipes(CommandSourceStack source) {
        var server = source.getServer();
        if (server == null) {
            source.sendFailure(Component.translatable("command.taczammobox.capacity.player_only"));
            return 0;
        }
        server.reloadResources(server.getPackRepository().getSelectedIds())
                .thenRun(() -> source.sendSuccess(
                        () -> Component.translatable("command.taczammobox.reloadrecipes.done"), true))
                .exceptionally(exception -> {
                    source.sendFailure(Component.translatable("command.taczammobox.reloadrecipes.failed"));
                    return null;
                });
        return 1;
    }

    private static int setHeldCapacity(CommandSourceStack source, int capacity) {
        return setHeldCapacity(source, capacity, true);
    }

    private static int setHeldLevelCapacity(CommandSourceStack source, int capacity) {
        return setHeldCapacity(source, capacity, false);
    }

    private static int setHeldMultiplier(CommandSourceStack source, double multiplier) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.taczammobox.capacity.player_only"));
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof IAmmoBox box)) {
            source.sendFailure(Component.translatable("command.taczammobox.capacity.not_box"));
            return 0;
        }
        String key = itemId(stack.getItem()) + "#" + box.getAmmoLevel(stack);
        WasteSettings.setBoxCapacityMultiplier(key, multiplier);
        AmmoBoxHelper.clearCapacityCache();
        source.sendSuccess(() -> Component.translatable(
                "command.taczammobox.capacity.multiplier.set", key, multiplier), true);
        return 1;
    }

    private static int setMultiplier(CommandSourceStack source, Item item, int level, double multiplier) {
        String key = itemId(item) + "#" + level;
        WasteSettings.setBoxCapacityMultiplier(key, multiplier);
        AmmoBoxHelper.clearCapacityCache();
        source.sendSuccess(() -> Component.translatable(
                "command.taczammobox.capacity.multiplier.set", key, multiplier), true);
        return 1;
    }

    private static int setAllMultiplier(CommandSourceStack source, Item item, double multiplier) {
        String key = itemId(item);
        WasteSettings.setBoxCapacityMultiplier(key, multiplier);
        AmmoBoxHelper.clearCapacityCache();
        source.sendSuccess(() -> Component.translatable(
                "command.taczammobox.capacity.multiplier.set", key, multiplier), true);
        return 1;
    }

    private static int setAmmoMultiplier(CommandSourceStack source, Item item, int level,
                                         ResourceLocation ammo, double multiplier) {
        String key = itemId(item) + "#" + level + "#" + ammo;
        WasteSettings.setBoxCapacityMultiplier(key, multiplier);
        AmmoBoxHelper.clearCapacityCache();
        source.sendSuccess(() -> Component.translatable(
                "command.taczammobox.capacity.multiplier.set", key, multiplier), true);
        return 1;
    }

    private static int setHeldCapacity(CommandSourceStack source, int capacity, boolean includeAmmo) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.taczammobox.capacity.player_only"));
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof IAmmoBox box)) {
            source.sendFailure(Component.translatable("command.taczammobox.capacity.not_box"));
            return 0;
        }
        ResourceLocation ammo = box.getAmmoId(stack);
        boolean hasAmmo = includeAmmo && ammo != null && !ammo.equals(DefaultAssets.EMPTY_AMMO_ID);
        String key = itemId(stack.getItem()) + "#" + box.getAmmoLevel(stack) + (hasAmmo ? "#" + ammo : "");
        WasteSettings.setBoxCapacity(key, capacity);
        AmmoBoxHelper.clearCapacityCache();
        source.sendSuccess(() -> Component.translatable("command.taczammobox.capacity.set", key, capacity), true);
        return 1;
    }

    private static int setCapacity(CommandSourceStack source, Item item, int level, int capacity) {
        String key = itemId(item) + "#" + level;
        WasteSettings.setBoxCapacity(key, capacity);
        AmmoBoxHelper.clearCapacityCache();
        source.sendSuccess(() -> Component.translatable("command.taczammobox.capacity.set", key, capacity), true);
        return 1;
    }

    private static int setAmmoCapacity(CommandSourceStack source, Item item, int level,
                                       ResourceLocation ammo, int capacity) {
        String key = itemId(item) + "#" + level + "#" + ammo;
        WasteSettings.setBoxCapacity(key, capacity);
        AmmoBoxHelper.clearCapacityCache();
        source.sendSuccess(() -> Component.translatable("command.taczammobox.capacity.set", key, capacity), true);
        return 1;
    }

    private static int setAllCapacity(CommandSourceStack source, Item item, int capacity) {
        String key = itemId(item);
        WasteSettings.setBoxCapacity(key, capacity);
        AmmoBoxHelper.clearCapacityCache();
        source.sendSuccess(() -> Component.translatable("command.taczammobox.capacity.set", key, capacity), true);
        return 1;
    }

    private static int clearCapacity(CommandSourceStack source, Item item, int level) {
        return clearCapacity(source, itemId(item) + "#" + level);
    }

    private static int clearAmmoCapacity(CommandSourceStack source, Item item, int level, ResourceLocation ammo) {
        return clearCapacity(source, itemId(item) + "#" + level + "#" + ammo);
    }

    private static int clearAllCapacity(CommandSourceStack source, Item item) {
        String key = itemId(item);
        int removed = WasteSettings.removeBoxCapacitiesForItem(key);
        if (removed <= 0) {
            source.sendFailure(Component.translatable("command.taczammobox.capacity.not_found", key));
            return 0;
        }
        AmmoBoxHelper.clearCapacityCache();
        source.sendSuccess(() -> Component.translatable("command.taczammobox.capacity.clear", key), true);
        return 1;
    }

    private static int clearCapacity(CommandSourceStack source, String key) {
        if (!WasteSettings.removeBoxCapacity(key)) {
            source.sendFailure(Component.translatable("command.taczammobox.capacity.not_found", key));
            return 0;
        }
        AmmoBoxHelper.clearCapacityCache();
        source.sendSuccess(() -> Component.translatable("command.taczammobox.capacity.clear", key), true);
        return 1;
    }

    private static int setRounding(CommandSourceStack source, String mode) {
        if (!WasteSettings.setCapacityRounding(mode)) {
            source.sendFailure(Component.translatable("command.taczammobox.capacity.rounding.invalid"));
            return 0;
        }
        AmmoBoxHelper.clearCapacityCache();
        source.sendSuccess(() -> Component.translatable(
                "command.taczammobox.capacity.rounding.set", mode), true);
        return 1;
    }

    private static int listCapacity(CommandSourceStack source) {        Map<String, Integer> capacities = WasteSettings.boxCapacities();
        Map<String, Double> multipliers = WasteSettings.boxMultipliers();
        if (capacities.isEmpty() && multipliers.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.taczammobox.capacity.list_empty"), false);
            return 1;
        }
        StringJoiner joiner = new StringJoiner(", ");
        capacities.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> joiner.add(entry.getKey() + "=" + entry.getValue()));
        multipliers.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> joiner.add(entry.getKey() + "=x" + entry.getValue()));
        source.sendSuccess(() -> Component.translatable("command.taczammobox.capacity.list", joiner.toString()), false);
        return 1;
    }

    private static String itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static int scan(CommandSourceStack source) {
        int added = AmmoScanner.appendMissing();
        if (added < 0) {
            source.sendFailure(Component.literal("扫描失败，请查看日志"));
            return 0;
        }
        if (added == 0) {
            source.sendSuccess(() -> Component.literal("没有发现缺失的弹种"), true);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "配置已追加 " + added + " 个弹种；重启游戏后生效"), true);
        }
        return 1;
    }

    private static int clean(CommandSourceStack source) {
        int removed = AmmoScanner.pruneMissing();
        if (removed < 0) {
            source.sendFailure(Component.literal("清理失败，请查看日志"));
            return 0;
        }
        if (removed == 0) {
            source.sendSuccess(() -> Component.literal("没有失效的弹种条目"), true);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "已清理 " + removed + " 个失效条目；重启后对应盒子/箱子会变成未知物品"), true);
        }
        return 1;
    }

    private static int cleanup(CommandSourceStack source, int radius) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.taczammobox.capacity.player_only"));
            return 0;
        }
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            source.sendFailure(Component.literal("只能在服务器世界执行"));
            return 0;
        }
        OrphanCleanup.Result result = OrphanCleanup.run(level, player, radius, true);
        source.sendSuccess(() -> Component.literal(
                "已清理 " + result.stacks() + " 组 / " + result.items()
                        + " 个孤儿盒/箱，涉及 " + result.locations() + " 个位置"), true);
        return 1;
    }

    private static int showWaste(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("waste=" + WasteSettings.waste()), false);
        return 1;
    }

    private static int setWaste(CommandSourceStack source, boolean value) {
        WasteSettings.setWaste(value);
        source.sendSuccess(() -> Component.literal("waste = " + value), true);
        return 1;
    }

    private static int dump(CommandSourceStack source) {
        Path file = AmmoTypes.configDir().resolve("detected_ammo.json");
        JsonObject root = new JsonObject();
        JsonArray entries = new JsonArray();

        TimelessAPI.getAllCommonAmmoIndex().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry -> {
                    JsonObject object = new JsonObject();
                    String ammoId = entry.getKey().toString();
                    object.addProperty("ammo", ammoId);
                    object.addProperty("id", AmmoTypes.sanitizeItemName(entry.getKey().getPath()));
                    String nameKey = entry.getValue().getPojo().getName();
                    if (nameKey != null && !nameKey.isBlank()) {
                        object.addProperty("nameKey", nameKey);
                    }
                    entries.add(object);
                });

        root.addProperty("builtin", true);
        root.add("entries", entries);

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
            source.sendSuccess(() -> Component.literal(
                    "Wrote " + entries.size() + " ammo entries to " + file), false);
            return entries.size();
        } catch (IOException exception) {
            TaczAmmoBoxMod.LOGGER.error("Failed to write {}", file, exception);
            source.sendFailure(Component.literal("Failed to write " + file + ": " + exception.getMessage()));
            return 0;
        }
    }
}
