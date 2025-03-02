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
        context.getSource().sendFeedback(() -> Text.literal("Dimensional Roulette twist is " + status), true);
        return Command.SINGLE_SUCCESS;
    }

    // Getter for event handler to check status
    public static boolean isTwistEnabled() {
        return twistEnabled;
    }
}
