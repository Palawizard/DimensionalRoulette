package com.palawi.dimensionalroulette.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;


import java.util.List;
import java.util.Random;

import com.palawi.dimensionalroulette.DimensionalRoulette;


public class DamageEventHandler {
    private static final Random RANDOM = new Random();
    private static final int SAFE_Y_MIN = 10;
    private static final int SAFE_Y_MAX = 200;
    private static final int FLOATING_PLATFORM_Y = 80;


    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, amount, afterHealth, isFatal) -> {
            if (!DimensionalRoulette.isTwistEnabled()) return;
            if (entity instanceof ServerPlayerEntity player) {
                teleportToRandomDimension(player);
                giveEscapeItems(player);
            }
        });
    }

    private static void teleportToRandomDimension(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
    
        List<RegistryKey<World>> dimensions = server.getWorldRegistryKeys().stream().toList();
        if (dimensions.isEmpty()) return;
    
        RegistryKey<World> randomDimension;
        do {
            randomDimension = dimensions.get(RANDOM.nextInt(dimensions.size()));
        } while (randomDimension.equals(player.getWorld().getRegistryKey()) || 
                DimensionalRoulette.isDimensionDisabled(randomDimension.getValue().toString()));
    
        ServerWorld targetWorld = server.getWorld(randomDimension);
        if (targetWorld == null) return;
    
        player.extinguish();
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 200, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 100, 0)); // 5 sec fire resistance
    
        Identifier dimensionId = randomDimension.getValue();
        BlockPos safePos;
        int targetX = player.getBlockPos().getX();
        int targetZ = player.getBlockPos().getZ();
    
        // ✅ Vérifier d'abord si un custom spawn existe pour cette dimension
        if (DimensionalRoulette.getCustomSpawns().containsKey(dimensionId.toString())) {
            safePos = DimensionalRoulette.getCustomSpawns().get(dimensionId.toString());
        }
        // Fixe le spawn pour `hiddenrealm:silent_bazaar`
        else if (dimensionId.equals(Identifier.of("hiddenrealm", "silent_bazaar"))) {
            safePos = new BlockPos(-8, 132, -7);
        }
        // Fixe le X = 40 si `spawnMode` est activé pour `backrooms:level_0`
        else if (dimensionId.equals(Identifier.of("backrooms", "level_0"))) {
            if (DimensionalRoulette.isSpawnMode()) {
                BlockPos spawnPos = targetWorld.getSpawnPos();
                safePos = new BlockPos(40, 2, spawnPos.getZ()); // Override X à 40
            } else {
                safePos = new BlockPos(targetX, 2, targetZ);
            }
        }
        // Fixe uniquement le Y pour ces dimensions
        else if (dimensionId.equals(Identifier.of("ceilands", "the_ceilands"))) {
            safePos = new BlockPos(targetX, FLOATING_PLATFORM_Y, targetZ);
        } else if (dimensionId.equals(Identifier.of("bro", "void"))) {
            safePos = new BlockPos(targetX, 23, targetZ);
        }
        // Mode spawn vs relative
        else {
            if (DimensionalRoulette.isSpawnMode()) {
                BlockPos spawnPos = targetWorld.getSpawnPos();
                safePos = findSafeLanding(targetWorld, spawnPos.getX(), spawnPos.getZ());
            } else {
                safePos = findSafeLanding(targetWorld, targetX, targetZ);
            }
        }
    
        // Vérification du Nether Roof (évite le TP au-dessus de Y=128)
        if (targetWorld.getRegistryKey().equals(World.NETHER) && safePos.getY() > 127) {
            safePos = new BlockPos(safePos.getX(), 32, safePos.getZ()); // Déplace le joueur plus bas
        }
    
        // Téléportation
        Vec3d spawnVec = Vec3d.ofCenter(safePos);
        player.teleport(targetWorld, spawnVec.x, spawnVec.y, spawnVec.z, player.getYaw(), player.getPitch());
    
        // Si le joueur est dans un bloc, creuser une salle de 3x3x3
        if (isPlayerInsideBlock(targetWorld, safePos)) {
            carveEscapeRoom(targetWorld, safePos);
        }
    
        // Si le joueur est dans l'air, créer une plateforme de sécurité
        if (isPlayerInAir(targetWorld, safePos)) {
            createFloatingPlatform(targetWorld, safePos.down());
        }
    
        // Supprime le bedrock sous les pieds du joueur si nécessaire
        removeBedrockUnderFeet(targetWorld, safePos);
    }    
    

    private static boolean isPlayerInAir(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos.down()).isAir();
    }
    

    private static boolean isPlayerInsideBlock(ServerWorld world, BlockPos pos) {
        return !world.getBlockState(pos).isAir() && !world.getBlockState(pos.up()).isAir();
    }

    private static void carveEscapeRoom(ServerWorld world, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos blockPos = center.add(dx, dy, dz);
                    if (!world.getBlockState(blockPos).isOf(Blocks.BEDROCK)) {
                        world.setBlockState(blockPos, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }
    }
    
    
    /**
     * Removes any bedrock **directly below** the player's feet in the chunk.
     */
    private static void removeBedrockUnderFeet(ServerWorld world, BlockPos playerPos) {
        for (int y = playerPos.getY(); y >= world.getBottomY(); y--) {
            BlockPos pos = new BlockPos(playerPos.getX(), y, playerPos.getZ());
            if (world.getBlockState(pos).isOf(Blocks.BEDROCK)) {
                world.setBlockState(pos, Blocks.STONE.getDefaultState()); // Replace bedrock with stone
            }
        }
    }      


    private static void giveEscapeItems(ServerPlayerEntity player) {
        // Define unique custom names for items to identify them later
        String flintAndSteelName = "Dimensional Spark";
        String pickaxeName = "Unstucker 3000";
        String elytraName = "Temporal Wings";
        String fireworkName = "Escape Rocket";
    
        // Check if the player already has the items
        boolean hasFlintAndSteel = hasItemWithName(player, Items.FLINT_AND_STEEL, flintAndSteelName);
        boolean hasPickaxe = hasItemWithName(player, Items.DIAMOND_PICKAXE, pickaxeName);
        boolean hasElytra = hasItemWithName(player, Items.ELYTRA, elytraName);
        boolean hasFirework = hasItemWithName(player, Items.FIREWORK_ROCKET, fireworkName);
    
        // If missing, create and give the item
        if (!hasFlintAndSteel) {
            ItemStack flintAndSteel = new ItemStack(Items.FLINT_AND_STEEL);
            flintAndSteel.setDamage(flintAndSteel.getMaxDamage() - 1);
            flintAndSteel.set(DataComponentTypes.CUSTOM_NAME, 
                Text.literal(flintAndSteelName).formatted(Formatting.RED, Formatting.ITALIC));
            dropOrReplace(player, flintAndSteel);
        }
    
        if (!hasPickaxe) {
            ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            pickaxe.setDamage(pickaxe.getMaxDamage() - 20);
            pickaxe.set(DataComponentTypes.CUSTOM_NAME, 
                Text.literal(pickaxeName).formatted(Formatting.AQUA, Formatting.BOLD));
            dropOrReplace(player, pickaxe);
        }
    
        if (!hasElytra) {
            ItemStack elytra = new ItemStack(Items.ELYTRA);
            elytra.setDamage(elytra.getMaxDamage() - 30);
            elytra.set(DataComponentTypes.CUSTOM_NAME, 
                Text.literal(elytraName).formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
            dropOrReplace(player, elytra);
        }
    
        if (!hasFirework) {
            ItemStack firework = new ItemStack(Items.FIREWORK_ROCKET);
            firework.set(DataComponentTypes.CUSTOM_NAME, 
                Text.literal(fireworkName).formatted(Formatting.YELLOW));
            dropOrReplace(player, firework);
        }
    }

    private static boolean hasItemWithName(ServerPlayerEntity player, Item itemType, String itemName) {
        for (ItemStack stack : player.getInventory().main) {
            if (stack.getItem() == itemType) {
                Text customName = stack.getOrDefault(DataComponentTypes.CUSTOM_NAME, null);
                if (customName != null && customName.getString().equals(itemName)) {
                    return true; // The player already has this item
                }
            }
        }
        return false;
    }    
    


    private static void dropOrReplace(ServerPlayerEntity player, ItemStack item) {
        if (!player.getInventory().insertStack(item)) {
            player.dropItem(item, false);
        }
    }

    private static BlockPos findSafeLanding(ServerWorld world, int x, int z) {
        for (int y = SAFE_Y_MAX; y >= SAFE_Y_MIN; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (isSafeLocation(world, pos)) {
                return pos;
            }
        }
        return world.getSpawnPos();
    }

    private static boolean isSafeLocation(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).isAir() && world.getBlockState(pos.down()).isSolidBlock(world, pos.down());
    }

    private static void createFloatingPlatform(ServerWorld world, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                world.setBlockState(pos.add(dx, 0, dz), Blocks.STONE.getDefaultState());
            }
        }
    }
}
