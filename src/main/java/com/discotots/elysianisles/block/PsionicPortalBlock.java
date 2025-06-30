package com.discotots.elysianisles.block;

import com.discotots.elysianisles.ElysianIslesMod;
import com.discotots.elysianisles.init.ModDimensions;
import com.discotots.elysianisles.world.portal.PortalTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PsionicPortalBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    protected static final VoxelShape X_AABB = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    protected static final VoxelShape Z_AABB = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);

    private static final String PORTAL_COOLDOWN_TAG = "elysian_portal_cooldown";
    private static final int PORTAL_COOLDOWN_TIME = 20; // 1 second cooldown

    public PsionicPortalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        switch (state.getValue(AXIS)) {
            case Z:
                return Z_AABB;
            case X:
            default:
                return X_AABB;
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty(); // No collision - players can walk through
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty(); // No visual collision
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // Handle instant teleportation when entity enters portal
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            // Check cooldown to prevent rapid teleportation
            int cooldown = player.getPersistentData().getInt(PORTAL_COOLDOWN_TAG);
            if (cooldown > 0) {
                return;
            }

            ElysianIslesMod.LOGGER.info("Player {} entered portal block at {}, initiating teleport",
                    player.getName().getString(), pos);

            // Set cooldown
            player.getPersistentData().putInt(PORTAL_COOLDOWN_TAG, PORTAL_COOLDOWN_TIME);

            // Determine destination
            ServerLevel currentLevel = (ServerLevel) level;
            ResourceKey<Level> destinationKey = currentLevel.dimension() == ModDimensions.ELYSIAN_LEVEL_KEY
                    ? Level.OVERWORLD
                    : ModDimensions.ELYSIAN_LEVEL_KEY;

            ServerLevel destinationLevel = currentLevel.getServer().getLevel(destinationKey);

            if (destinationLevel != null) {
                // Create portal shape to get dimensions
                PsionicPortalShape shape = new PsionicPortalShape(level, pos, state.getValue(AXIS));

                if (shape.isValid()) {
                    // Play teleportation sound
                    level.playSound(null, pos, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5F, 1.0F);

                    // Teleport the player
                    player.changeDimension(destinationLevel,
                            new PortalTeleporter(destinationLevel, shape.getAxis(), shape.getWidth(), shape.getHeight()));

                    ElysianIslesMod.LOGGER.info("Successfully teleported {} from {} to {}",
                            player.getName().getString(),
                            currentLevel.dimension().location(),
                            destinationLevel.dimension().location());
                } else {
                    ElysianIslesMod.LOGGER.warn("Portal shape invalid for teleportation");
                }
            } else {
                ElysianIslesMod.LOGGER.error("Destination level not found!");
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Ambient portal sound
        if (random.nextInt(100) == 0) {
            level.playLocalSound(
                    (double)pos.getX() + 0.5D,
                    (double)pos.getY() + 0.5D,
                    (double)pos.getZ() + 0.5D,
                    SoundEvents.PORTAL_AMBIENT,
                    SoundSource.BLOCKS,
                    0.5F,
                    random.nextFloat() * 0.4F + 0.8F,
                    false
            );
        }

        // Portal particles
        for(int i = 0; i < 4; ++i) {
            double x = (double)pos.getX() + random.nextDouble();
            double y = (double)pos.getY() + random.nextDouble();
            double z = (double)pos.getZ() + random.nextDouble();
            double velX = (random.nextFloat() - 0.5D) * 0.5D;
            double velY = (random.nextFloat() - 0.5D) * 0.5D;
            double velZ = (random.nextFloat() - 0.5D) * 0.5D;
            int j = random.nextInt(2) * 2 - 1;

            // Adjust particle position based on portal orientation
            if (!level.getBlockState(pos.west()).is(this) && !level.getBlockState(pos.east()).is(this)) {
                x = (double)pos.getX() + 0.5D + 0.25D * (double)j;
                velX = (double)(random.nextFloat() * 2.0F * (float)j);
            } else {
                z = (double)pos.getZ() + 0.5D + 0.25D * (double)j;
                velZ = (double)(random.nextFloat() * 2.0F * (float)j);
            }

            level.addParticle(ParticleTypes.PORTAL, x, y, z, velX, velY, velZ);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
        return false;
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return -1.0F; // Unbreakable
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        return false; // Prevent breaking
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntity blockEntity, ItemStack stack) {
        // Do nothing - prevent breaking
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        // Do nothing - prevent breaking
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public float getExplosionResistance() {
        return Float.MAX_VALUE;
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        // Do nothing - immune to explosions
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        // Do nothing - prevent breaking
    }

    public boolean canSurvive(BlockState state, LevelAccessor level, BlockPos pos) {
        return true;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state;
    }
}