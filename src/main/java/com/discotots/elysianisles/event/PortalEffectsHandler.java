package com.discotots.elysianisles.event;

import com.discotots.elysianisles.block.PsionicPortalBlock;
import com.discotots.elysianisles.block.PsionicPortalShape;
import com.discotots.elysianisles.init.ModBlocks;
import com.discotots.elysianisles.init.ModDimensions;
import com.discotots.elysianisles.world.portal.PortalTeleporter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PortalEffectsHandler {

    private static final String PORTAL_TIMER_TAG = "elysian_portal_timer";
    private static final int PORTAL_WAIT_TIME = 80; // Ticks player must stand in portal (4 seconds)

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Entity player = event.player;
            Level level = player.level();

            // Check if the player is inside our portal block
            BlockState blockState = level.getBlockState(player.blockPosition());
            boolean isInPortal = blockState.is(ModBlocks.PSIONIC_PORTAL.get());

            if (isInPortal) {
                // Increment our custom timer stored in the player's NBT data
                int timer = player.getPersistentData().getInt(PORTAL_TIMER_TAG);
                player.getPersistentData().putInt(PORTAL_TIMER_TAG, timer + 1);

                // Play the portal sound effect, which also triggers the screen shake
                if (level.isClientSide && player == Minecraft.getInstance().player) {
                    if (level.random.nextInt(100) == 0) {
                        level.playSound(player, player.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.5F, level.random.nextFloat() * 0.4F + 0.8F);
                    }
                }

                // Check if the timer has reached the teleport threshold
                if (timer >= PORTAL_WAIT_TIME && !level.isClientSide()) {
                    handleTeleport(player, blockState);
                }

            } else {
                // If the player is not in a portal, reset their timer.
                if (player.getPersistentData().contains(PORTAL_TIMER_TAG)) {
                    player.getPersistentData().remove(PORTAL_TIMER_TAG);
                }
            }
        }
    }

    private static void handleTeleport(Entity entity, BlockState portalState) {
        ServerLevel currentLevel = (ServerLevel) entity.level();
        ResourceKey<Level> destinationKey = currentLevel.dimension() == ModDimensions.ELYSIAN_LEVEL_KEY ? Level.OVERWORLD : ModDimensions.ELYSIAN_LEVEL_KEY;
        ServerLevel destinationLevel = currentLevel.getServer().getLevel(destinationKey);

        if (destinationLevel != null) {
            // We find the shape of the portal the player is LEAVING from.
            PsionicPortalShape shape = new PsionicPortalShape(currentLevel, entity.blockPosition(), portalState.getValue(PsionicPortalBlock.AXIS));
            if (shape.isValid()) {
                // We tell the entity to change dimensions, and we give it our PortalTeleporter.
                // The teleporter will handle all the logic for the destination.
                entity.changeDimension(destinationLevel, new PortalTeleporter(destinationLevel, shape.getAxis(), shape.getWidth(), shape.getHeight()));
                entity.getPersistentData().remove(PORTAL_TIMER_TAG);
            }
        }
    }

}