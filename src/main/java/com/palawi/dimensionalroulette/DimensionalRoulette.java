package com.palawi.dimensionalroulette;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.EnumSet;
import net.minecraft.network.packet.s2c.play.PositionFlag;




import com.palawi.dimensionalroulette.events.DamageEventHandler;

public class DimensionalRoulette implements ModInitializer {
    public static final String MOD_ID = "dimensional_roulette";
    private static final Map<String, BlockPos> customSpawns = new HashMap<>();
    private static final List<String> disabledDimensions = new ArrayList<>();
    public static Map<String, BlockPos> getCustomSpawns() {
        return customSpawns;
    }    

    


    private static boolean twistEnabled = false;
    private static boolean spawnMode = false;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;

    @Override
    public void onInitialize() {
        System.out.println("Dimensional Roulette loaded!");

        // Load config from file
        loadConfig();

        // Register the damage event handler
        DamageEventHandler.register();

        // Register commands
        registerCommands();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("dimensionalroulette")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("enable").executes(this::enableTwist))
                .then(CommandManager.literal("disable").executes(this::disableTwist))
                .then(CommandManager.literal("status").executes(this::statusTwist))
                .then(CommandManager.literal("settings")
                    .then(CommandManager.literal("mode")
                        .then(CommandManager.literal("spawn").executes(this::setSpawnMode))
                        .then(CommandManager.literal("relative").executes(this::setRelativeMode))
                    )
                )
                .then(CommandManager.literal("dimensions")
                    .then(CommandManager.literal("list").executes(this::listDimensions))
                    .then(CommandManager.literal("tp")
                        .then(CommandManager.argument("dimension", StringArgumentType.string())
                            .executes(this::teleportToDimension)
                        )
                    )
                    .then(CommandManager.literal("setspawn")
                        .then(CommandManager.argument("dimension", StringArgumentType.string())
                            .executes(this::setCustomSpawn)
                        )
                    )
                    // 🔥 FIXED: "disable" and "enable" commands are now at the correct level
                    .then(CommandManager.literal("disable")
                        .then(CommandManager.argument("dimension", StringArgumentType.string())
                            .executes(this::disableDimension)
                        )
                    )
                    .then(CommandManager.literal("enable")
                        .then(CommandManager.argument("dimension", StringArgumentType.string())
                            .executes(this::enableDimension)
                        )
                    )
                )
            );
        });
    }    

    public static boolean isDimensionDisabled(String dimension) {
        return disabledDimensions.contains(dimension);
    }
    
    private int setCustomSpawn(CommandContext<ServerCommandSource> context) {
        String dimensionName = StringArgumentType.getString(context, "dimension");
        ServerPlayerEntity player = context.getSource().getPlayer();
    
        if (player == null) return 0;
    
        Identifier dimensionId = Identifier.of(dimensionName);
    
        // Sauvegarde la position du joueur comme nouveau spawn pour cette dimension
        BlockPos newSpawn = player.getBlockPos();
        customSpawns.put(dimensionId.toString(), newSpawn);
        saveConfig(); // Sauvegarde la config mise à jour
    
        context.getSource().sendFeedback(() -> Text.literal("Set custom spawn for " + dimensionName + " to " + newSpawn), true);
        return Command.SINGLE_SUCCESS;
    }    
    

    private int enableTwist(CommandContext<ServerCommandSource> context) {
        twistEnabled = true;
        saveConfig();
        context.getSource().sendFeedback(() -> Text.literal("Dimensional Roulette twist ENABLED!"), true);
        return Command.SINGLE_SUCCESS;
    }

    private int disableTwist(CommandContext<ServerCommandSource> context) {
        twistEnabled = false;
        saveConfig();
        context.getSource().sendFeedback(() -> Text.literal("Dimensional Roulette twist DISABLED!"), true);
        return Command.SINGLE_SUCCESS;
    }

    private int statusTwist(CommandContext<ServerCommandSource> context) {
        String status = twistEnabled ? "ENABLED" : "DISABLED";
        String mode = spawnMode ? "SPAWN MODE" : "RELATIVE MODE";
        context.getSource().sendFeedback(() -> Text.literal("Dimensional Roulette twist is " + status + " (" + mode + ")"), true);
        return Command.SINGLE_SUCCESS;
    }

    private int setSpawnMode(CommandContext<ServerCommandSource> context) {
        spawnMode = true;
        saveConfig();
        context.getSource().sendFeedback(() -> Text.literal("Teleport mode set to SPAWN"), true);
        return Command.SINGLE_SUCCESS;
    }

    private int setRelativeMode(CommandContext<ServerCommandSource> context) {
        spawnMode = false;
        saveConfig();
        context.getSource().sendFeedback(() -> Text.literal("Teleport mode set to RELATIVE"), true);
        return Command.SINGLE_SUCCESS;
    }

    private int listDimensions(CommandContext<ServerCommandSource> context) {
        MinecraftServer server = context.getSource().getServer();
        List<String> dimensionList = server.getWorldRegistryKeys()
            .stream()
            .map(RegistryKey::getValue)
            .map(Identifier::toString)
            .collect(Collectors.toList());

        context.getSource().sendFeedback(() -> Text.literal("Available Dimensions: " + String.join(", ", dimensionList)), false);
        return Command.SINGLE_SUCCESS;
    }

    private int teleportToDimension(CommandContext<ServerCommandSource> context) {
        String dimensionName = StringArgumentType.getString(context, "dimension");
        MinecraftServer server = context.getSource().getServer();
        ServerPlayerEntity player = context.getSource().getPlayer();
    
        if (player == null) return 0;
    
        Identifier dimensionId = Identifier.of(dimensionName);
        RegistryKey<World> targetDimension = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
        ServerWorld targetWorld = server.getWorld(targetDimension);
    
        if (targetWorld == null) {
            context.getSource().sendFeedback(() -> Text.literal("Dimension '" + dimensionName + "' not found."), false);
            return 0;
        }
    
        // Use custom spawn if it exists
        BlockPos safePos;
        if (customSpawns.containsKey(dimensionId.toString())) {
            safePos = customSpawns.get(dimensionId.toString());
        } else {
            BlockPos spawnPos = targetWorld.getSpawnPos();
            safePos = new BlockPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
        }
    
        Vec3d spawnVec = Vec3d.ofCenter(safePos);
        player.teleport(targetWorld, spawnVec.x, spawnVec.y, spawnVec.z, 
            EnumSet.noneOf(PositionFlag.class), player.getYaw(), player.getPitch(), false);

    
        // Grant Fire Resistance for 5 seconds
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 100, 0));
    
        context.getSource().sendFeedback(() -> Text.literal("Teleported to dimension: " + dimensionName), true);
        return Command.SINGLE_SUCCESS;
    }    

    private int disableDimension(CommandContext<ServerCommandSource> context) {
        String dimensionName = StringArgumentType.getString(context, "dimension");
    
        if (!disabledDimensions.contains(dimensionName)) {
            disabledDimensions.add(dimensionName);
            saveConfig();
            context.getSource().sendFeedback(() -> Text.literal("Dimension " + dimensionName + " has been DISABLED."), true);
        } else {
            context.getSource().sendFeedback(() -> Text.literal("Dimension " + dimensionName + " is already disabled."), false);
        }
        
        return Command.SINGLE_SUCCESS;
    }
    
    private int enableDimension(CommandContext<ServerCommandSource> context) {
        String dimensionName = StringArgumentType.getString(context, "dimension");
    
        if (disabledDimensions.contains(dimensionName)) {
            disabledDimensions.remove(dimensionName);
            saveConfig();
            context.getSource().sendFeedback(() -> Text.literal("Dimension " + dimensionName + " has been ENABLED."), true);
        } else {
            context.getSource().sendFeedback(() -> Text.literal("Dimension " + dimensionName + " is already enabled."), false);
        }
    
        return Command.SINGLE_SUCCESS;
    }     


    private static void loadConfig() {
        if (configFile == null) {
            configFile = new File("config/dimensional_roulette.json");
        }
    
        if (!configFile.exists()) {
            saveConfig();
            return;
        }
    
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            twistEnabled = json.has("twistEnabled") && json.get("twistEnabled").getAsBoolean();
            spawnMode = json.has("spawnMode") && json.get("spawnMode").getAsBoolean();
    
            // Charger les custom spawns
            if (json.has("customSpawns")) {
                JsonObject spawns = json.getAsJsonObject("customSpawns");
                for (String dimension : spawns.keySet()) {
                    String[] parts = spawns.get(dimension).getAsString().split(",");
                    customSpawns.put(dimension, new BlockPos(
                        Integer.parseInt(parts[0]), 
                        Integer.parseInt(parts[1]), 
                        Integer.parseInt(parts[2])
                    ));
                }
            }
    
            // Charger les dimensions désactivées
            if (json.has("disabledDimensions")) {
                disabledDimensions.clear();
                for (var element : json.getAsJsonArray("disabledDimensions")) {
                    disabledDimensions.add(element.getAsString());
                }
            }
    
        } catch (IOException e) {
            System.err.println("Failed to load config: " + e.getMessage());
        }
    }
    

    private static void saveConfig() {
        JsonObject json = new JsonObject();
        json.addProperty("twistEnabled", twistEnabled);
        json.addProperty("spawnMode", spawnMode);
    
        // Sauvegarder les custom spawns
        JsonObject spawns = new JsonObject();
        for (String dimension : customSpawns.keySet()) {
            BlockPos pos = customSpawns.get(dimension);
            spawns.addProperty(dimension, pos.getX() + "," + pos.getY() + "," + pos.getZ());
        }
        json.add("customSpawns", spawns);
    
        // Sauvegarder les dimensions désactivées
        JsonArray disabledArray = new JsonArray();
        for (String dimension : disabledDimensions) {
            disabledArray.add(dimension);
        }
        json.add("disabledDimensions", disabledArray);
    
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }
    

    public static boolean isTwistEnabled() {
        return twistEnabled;
    }

    public static boolean isSpawnMode() {
        return spawnMode;
    }
}