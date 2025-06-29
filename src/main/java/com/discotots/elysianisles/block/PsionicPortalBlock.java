package com.discotots.elysianisles.block;

import com.discotots.elysianisles.init.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

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
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction.Axis axis = state.getValue(AXIS);
        if (direction.getAxis() != axis && !this.canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    private boolean canSurvive(BlockState state, LevelAccessor level, BlockPos pos) {
        PsionicPortalShape shape = new PsionicPortalShape(level, pos, state.getValue(AXIS));
        return shape.isValid();
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!entity.isPassenger() && !entity.isVehicle() && entity.canChangeDimensions()) {
            if (entity.isOnPortalCooldown()) {
                entity.setPortalCooldown();
            } else {
                if (!level.isClientSide) {
                    // Handle teleportation logic here
                    this.handlePortalTeleport(entity, pos);
                }
            }
        }
    }

    private void handlePortalTeleport(Entity entity, BlockPos pos) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            ServerLevel destinationLevel;

            if (serverLevel.dimension() == ModDimensions.ELYSIAN_LEVEL_KEY) {
                // Teleport to Overworld
                destinationLevel = serverLevel.getServer().getLevel(Level.OVERWORLD);
            } else {
                // Teleport to Elysian Dimension
                destinationLevel = serverLevel.getServer().getLevel(ModDimensions.ELYSIAN_LEVEL_KEY);
            }

            if (destinationLevel != null) {
                // Simple teleportation - we'll enhance this later
                entity.changeDimension(destinationLevel);
                entity.playSound(SoundEvents.PORTAL_TRAVEL, 1.0F, 1.0F);
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

        // Add red particle effects - similar to nether portal but red
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

            // Use crimson spore particles for red effect
            level.addParticle(net.minecraft.core.particles.ParticleTypes.CRIMSON_SPORE,
                    x, y, z, velX, velY, velZ);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }
}
