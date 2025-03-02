package com.palawi.dimensionalroulette;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import com.palawi.dimensionalroulette.events.DamageEventHandler;

public class DimensionalRoulette implements ModInitializer {
    public static final String MOD_ID = "dimensional_roulette";
    private static boolean twistEnabled = false; // Default: Disabled
    private static boolean spawnMode = false; // Default: Relative Mode (true = spawn, false = relative)

    @Override
    public void onInitialize() {
        System.out.println("Dimensional Roulette loaded!");

        // Register the damage event handler
        DamageEventHandler.register();

        // Register commands
        registerCommands();
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("dimensionalroulette")
                .then(CommandManager.literal("enable")
                    .executes(this::enableTwist))
                .then(CommandManager.literal("disable")
                    .executes(this::disableTwist))
                .then(CommandManager.literal("status")
                    .executes(this::statusTwist))
                .then(CommandManager.literal("settings")
                    .then(CommandManager.literal("mode")
                        .then(CommandManager.literal("spawn").executes(this::setSpawnMode))
                        .then(CommandManager.literal("relative").executes(this::setRelativeMode))
                    )
                )
            );
        });
    }

    private int enableTwist(CommandContext<ServerCommandSource> context) {
        twistEnabled = true;
        context.getSource().sendFeedback(() -> Text.literal("Dimensional Roulette twist ENABLED!"), true);
        return Command.SINGLE_SUCCESS;
    }

    private int disableTwist(CommandContext<ServerCommandSource> context) {
        twistEnabled = false;
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
        context.getSource().sendFeedback(() -> Text.literal("Teleport mode set to SPAWN"), true);
        return Command.SINGLE_SUCCESS;
    }

    private int setRelativeMode(CommandContext<ServerCommandSource> context) {
        spawnMode = false;
        context.getSource().sendFeedback(() -> Text.literal("Teleport mode set to RELATIVE"), true);
        return Command.SINGLE_SUCCESS;
    }

    // Getter methods for event handling
    public static boolean isTwistEnabled() {
        return twistEnabled;
    }

    public static boolean isSpawnMode() {
        return spawnMode;
    }
}
