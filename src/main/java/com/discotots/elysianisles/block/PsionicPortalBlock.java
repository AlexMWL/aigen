package com.discotots.elysianisles.block;

import com.discotots.elysianisles.ElysianIslesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.network.chat.Component;

public class PsionicPortalBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    protected static final VoxelShape X_AABB = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    protected static final VoxelShape Z_AABB = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);

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
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canBeReplaced(BlockState state, net.minecraft.world.item.context.BlockPlaceContext context) {
        return false;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // === MAXIMUM PROTECTION AGAINST DESTRUCTION ===

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        ElysianIslesMod.LOGGER.debug("Player {} tried to break portal block at {}", player.getName().getString(), pos);
        return -1.0F; // Indestructible - no progress ever
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        ElysianIslesMod.LOGGER.info("Blocked destruction attempt by player {} at {}", player.getName().getString(), pos);
        return false; // Never allow destruction
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntity blockEntity, ItemStack stack) {
        ElysianIslesMod.LOGGER.info("Blocked playerDestroy by {} at {}", player.getName().getString(), pos);
        // Override to prevent ANY destruction
    }

    @Override
    public void destroy(LevelAccessor level, BlockPos pos, BlockState state) {
        ElysianIslesMod.LOGGER.info("Blocked destroy call at {}", pos);
        // Override to prevent destruction
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return false; // Cannot be harvested by anyone
    }

    public ItemStack getCloneItemStack(Level level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY; // No item when middle-clicked
    }

    @Override
    public float getExplosionResistance() {
        return Float.MAX_VALUE; // Maximum possible explosion resistance
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        ElysianIslesMod.LOGGER.debug("Portal block at {} resisted explosion", pos);
        // Do absolutely nothing - completely explosion proof
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK; // Cannot be pushed by pistons
    }

    // Block all player interactions that could lead to breaking
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Allow portal functionality but prevent any breaking
        if (!level.isClientSide) {
            ElysianIslesMod.LOGGER.debug("Player {} interacted with portal at {}", player.getName().getString(), pos);
        }
        return InteractionResult.PASS; // Don't consume the interaction
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        // Block left-click attacks
        if (!level.isClientSide) {
            ElysianIslesMod.LOGGER.debug("Player {} attacked portal block at {} - blocked", player.getName().getString(), pos);
        }
    }

    // Block any attempts to remove the block
    public boolean canSurvive(BlockState state, LevelAccessor level, BlockPos pos) {
        return true; // Always survives
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // Never change state due to neighbor updates
        return state;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!entity.isPassenger() && !entity.isVehicle() && entity.canChangeDimensions()) {
            if (entity.isOnPortalCooldown()) {
                entity.setPortalCooldown();
            } else {
                if (!level.isClientSide) {
                    // Simple message for now - actual teleportation will be added later
                    if (entity instanceof Player player) {
                        ElysianIslesMod.LOGGER.debug("Player {} is inside portal at {}", player.getName().getString(), pos);
                        player.displayClientMessage(
                                Component.literal("Portal activated! (Teleportation system coming soon...)"),
                                true);
                    }
                }
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D,
                    (double)pos.getZ() + 0.5D, SoundEvents.PORTAL_AMBIENT,
                    SoundSource.BLOCKS, 0.5F, random.nextFloat() * 0.4F + 0.8F, false);
        }

        // Red particle effects
        for(int i = 0; i < 4; ++i) {
            double x = (double)pos.getX() + random.nextDouble();
            double y = (double)pos.getY() + random.nextDouble();
            double z = (double)pos.getZ() + random.nextDouble();
            double velX = (random.nextFloat() - 0.5D) * 0.5D;
            double velY = (random.nextFloat() - 0.5D) * 0.5D;
            double velZ = (random.nextFloat() - 0.5D) * 0.5D;
            int j = random.nextInt(2) * 2 - 1;

            if (!level.getBlockState(pos.west()).is(this) && !level.getBlockState(pos.east()).is(this)) {
                x = (double)pos.getX() + 0.5D + 0.25D * (double)j;
                velX = (double)(random.nextFloat() * 2.0F * (float)j);
            } else {
                z = (double)pos.getZ() + 0.5D + 0.25D * (double)j;
                velZ = (double)(random.nextFloat() * 2.0F * (float)j);
            }

            level.addParticle(ParticleTypes.CRIMSON_SPORE, x, y, z, velX, velY, velZ);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
}