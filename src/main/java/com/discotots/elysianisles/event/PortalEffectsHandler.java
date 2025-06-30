package com.discotots.elysianisles.event;

import com.discotots.elysianisles.ElysianIslesMod;
import com.discotots.elysianisles.block.PsionicPortalBlock;
import com.discotots.elysianisles.block.PsionicPortalShape;
import com.discotots.elysianisles.init.ModBlocks;
import com.discotots.elysianisles.init.ModDimensions;
import com.discotots.elysianisles.world.portal.PortalTeleporter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PortalEffectsHandler {

    private static final String PORTAL_COOLDOWN_TAG = "elysian_portal_cooldown";
    private static final int PORTAL_COOLDOWN_TIME = 10; // Ticks to prevent multiple teleports

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Entity player = event.player;
            Level level = player.level();

            // Handle cooldown
            int cooldown = player.getPersistentData().getInt(PORTAL_COOLDOWN_TAG);
            if (cooldown > 0) {
                player.getPersistentData().putInt(PORTAL_COOLDOWN_TAG, cooldown - 1);
                return; // Skip portal detection while on cooldown
            }

            // Check if the player is intersecting with any portal blocks
            boolean isInPortal = isPlayerInPortal(player);

            if (isInPortal && !level.isClientSide()) {
                ElysianIslesMod.LOGGER.info("Player {} entered portal, teleporting immediately",
                        player.getName().getString());
                handleTeleport(player);
            }

            // Play portal ambient effects on client side
            if (isInPortal && level.isClientSide && player == Minecraft.getInstance().player) {
                if (level.random.nextInt(100) == 0) {
                    level.playSound(player, player.blockPosition(), SoundEvents.PORTAL_TRIGGER,
                            SoundSource.PLAYERS, 0.5F, level.random.nextFloat() * 0.4F + 0.8F);
                }
            }
        }
    }

    /**
     * Check if the player's bounding box intersects with any portal blocks
     */
    private boolean isPlayerInPortal(Entity player) {
        Level level = player.level();

        // Get player's center position
        double centerX = player.getX();
        double centerY = player.getY() + (player.getBbHeight() / 2.0);
        double centerZ = player.getZ();

        // Check a 3x3x3 area around the player's center
        BlockPos centerPos = new BlockPos((int) Math.floor(centerX), (int) Math.floor(centerY), (int) Math.floor(centerZ));

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos checkPos = centerPos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(checkPos);

                    if (state.is(ModBlocks.PSIONIC_PORTAL.get())) {
                        // Double-check with actual bounding box intersection
                        AABB blockAABB = new AABB(checkPos.getX(), checkPos.getY(), checkPos.getZ(),
                                checkPos.getX() + 1, checkPos.getY() + 1, checkPos.getZ() + 1);
                        AABB playerAABB = player.getBoundingBox();

                        if (blockAABB.intersects(playerAABB)) {
                            return true;
                        }
                    }
                }
            }
        }

        // Also check the player's exact foot position
        BlockPos feetPos = player.blockPosition();
        if (level.getBlockState(feetPos).is(ModBlocks.PSIONIC_PORTAL.get())) {
            return true;
        }

        // Check one block above feet (for when player is jumping)
        BlockPos aboveFeet = feetPos.above();
        if (level.getBlockState(aboveFeet).is(ModBlocks.PSIONIC_PORTAL.get())) {
            return true;
        }

        return false;
    }

    private void handleTeleport(Entity entity) {
        ElysianIslesMod.LOGGER.info("handleTeleport called for {}", entity.getName().getString());

        if (!(entity instanceof ServerPlayer)) {
            ElysianIslesMod.LOGGER.info("Entity is not a ServerPlayer, skipping teleport");
            return;
        }

        // Set cooldown to prevent multiple rapid teleports
        entity.getPersistentData().putInt(PORTAL_COOLDOWN_TAG, PORTAL_COOLDOWN_TIME);

        ServerLevel currentLevel = (ServerLevel) entity.level();
        ElysianIslesMod.LOGGER.info("Current level: {}", currentLevel.dimension().location());

        // Determine destination dimension
        ResourceKey<Level> destinationKey = currentLevel.dimension() == ModDimensions.ELYSIAN_LEVEL_KEY
                ? Level.OVERWORLD
                : ModDimensions.ELYSIAN_LEVEL_KEY;

        ElysianIslesMod.LOGGER.info("Destination level: {}", destinationKey.location());

        ServerLevel destinationLevel = currentLevel.getServer().getLevel(destinationKey);

        if (destinationLevel != null) {
            ElysianIslesMod.LOGGER.info("Destination level found, looking for portal block");

            // Find the portal the player is currently in
            BlockPos portalPos = findPortalBlockNearPlayer(entity);
            if (portalPos != null) {
                ElysianIslesMod.LOGGER.info("Found portal block at {}", portalPos);

                BlockState portalState = currentLevel.getBlockState(portalPos);

                // Create portal shape to get dimensions
                PsionicPortalShape shape = new PsionicPortalShape(currentLevel, portalPos,
                        portalState.getValue(PsionicPortalBlock.AXIS));

                if (shape.isValid()) {
                    ElysianIslesMod.LOGGER.info("Portal shape is valid: {}x{}",
                            shape.getWidth(), shape.getHeight());

                    // Play teleportation sound
                    currentLevel.playSound(null, entity.blockPosition(), SoundEvents.PORTAL_TRAVEL,
                            SoundSource.PLAYERS, 0.5F, 1.0F);

                    // Teleport the entity
                    ElysianIslesMod.LOGGER.info("Calling changeDimension...");
                    entity.changeDimension(destinationLevel,
                            new PortalTeleporter(destinationLevel, shape.getAxis(), shape.getWidth(), shape.getHeight()));

                    ElysianIslesMod.LOGGER.info("Teleported {} from {} to {}",
                            entity.getName().getString(),
                            currentLevel.dimension().location(),
                            destinationLevel.dimension().location());
                } else {
                    ElysianIslesMod.LOGGER.warn("Portal shape is invalid");
                }
            } else {
                ElysianIslesMod.LOGGER.warn("Could not find portal block near player");
            }
        } else {
            ElysianIslesMod.LOGGER.error("Destination level is null!");
        }
    }

    /**
     * Find a portal block near the player's position using multiple methods
     */
    private BlockPos findPortalBlockNearPlayer(Entity player) {
        Level level = player.level();

        // Method 1: Check player's center position
        BlockPos centerPos = new BlockPos((int) Math.floor(player.getX()),
                (int) Math.floor(player.getY() + player.getBbHeight() / 2.0),
                (int) Math.floor(player.getZ()));
        if (level.getBlockState(centerPos).is(ModBlocks.PSIONIC_PORTAL.get())) {
            return centerPos;
        }

        // Method 2: Check around player in a 3x3x3 area
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos checkPos = centerPos.offset(dx, dy, dz);
                    if (level.getBlockState(checkPos).is(ModBlocks.PSIONIC_PORTAL.get())) {
                        return checkPos;
                    }
                }
            }
        }

        // Method 3: Check player's exact bounding box
        AABB playerBounds = player.getBoundingBox();
        for (BlockPos pos : BlockPos.betweenClosed(
                (int) Math.floor(playerBounds.minX),
                (int) Math.floor(playerBounds.minY),
                (int) Math.floor(playerBounds.minZ),
                (int) Math.floor(playerBounds.maxX),
                (int) Math.floor(playerBounds.maxY),
                (int) Math.floor(playerBounds.maxZ))) {

            if (level.getBlockState(pos).is(ModBlocks.PSIONIC_PORTAL.get())) {
                return pos;
            }
        }

        ElysianIslesMod.LOGGER.info("No portal block found near player at {}", player.position());
        return null;
    }
}