package com.discotots.elysianisles.world.portal;

import com.discotots.elysianisles.ElysianIslesMod;
import com.discotots.elysianisles.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PortalManager extends SavedData {
    private static final String DATA_NAME = ElysianIslesMod.MOD_ID + "_portal_data";
    private final Map<String, BlockPos> portalPositions = new HashMap<>();

    public PortalManager() {}

    public PortalManager(CompoundTag tag) {
        CompoundTag portalsTag = tag.getCompound("portals");
        for (String key : portalsTag.getAllKeys()) {
            portalPositions.put(key, NbtUtils.readBlockPos(portalsTag.getCompound(key)));
        }
    }

    public static PortalManager get(ServerLevel level) {
        // The manager is stored in the Overworld's data to be globally accessible
        DimensionDataStorage storage = level.getServer().overworld().getDataStorage();
        return storage.computeIfAbsent(PortalManager::new, PortalManager::new, DATA_NAME);
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag) {
        CompoundTag portalsTag = new CompoundTag();
        portalPositions.forEach((key, pos) -> portalsTag.put(key, NbtUtils.writeBlockPos(pos)));
        compoundTag.put("portals", portalsTag);
        return compoundTag;
    }

    public BlockPos getPortalPos(ResourceKey<Level> dimension) {
        return portalPositions.get(dimension.location().toString());
    }

    public void setPortalPos(ResourceKey<Level> dimension, BlockPos pos) {
        portalPositions.put(dimension.location().toString(), pos);
        setDirty(); // Mark this SavedData as needing to be written to disk
    }

    public void removePortal(ResourceKey<Level> dimension) {
        portalPositions.remove(dimension.location().toString());
        setDirty();
    }

    public void destroyOldPortal(ServerLevel level) {
        BlockPos oldPos = getPortalPos(level.dimension());
        if (oldPos == null || !level.isLoaded(oldPos)) return;

        // Use a flood-fill search to find and remove all connected portal blocks
        Set<BlockPos> toRemove = new HashSet<>();
        Set<BlockPos> toCheck = new HashSet<>();
        toCheck.add(oldPos);

        int maxPortalBlocks = 21 * 21; // Safety limit
        while (!toCheck.isEmpty() && toRemove.size() < maxPortalBlocks) {
            BlockPos checking = toCheck.iterator().next();
            toCheck.remove(checking);

            if (toRemove.contains(checking)) continue;

            if (level.getBlockState(checking).is(ModBlocks.PSIONIC_PORTAL.get())) {
                toRemove.add(checking);
                for (BlockPos neighbor : BlockPos.withinManhattan(checking, 1, 1, 1)) {
                    if (!toRemove.contains(neighbor)) {
                        toCheck.add(neighbor.immutable());
                    }
                }
            }
        }

        for (BlockPos posToRemove : toRemove) {
            level.setBlock(posToRemove, Blocks.AIR.defaultBlockState(), 3);
        }

        removePortal(level.dimension());
        ElysianIslesMod.LOGGER.info("Deactivated old portal at: {}", oldPos);
    }
}