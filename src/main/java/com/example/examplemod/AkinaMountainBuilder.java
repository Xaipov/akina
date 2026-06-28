package com.example.examplemod;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class AkinaMountainBuilder {
    private static final int BASE_RADIUS = 80;
    private static final int MOUNTAIN_HEIGHT = 55;
    private static final int ROAD_WIDTH = 10;
    private static final int MAX_ROAD_LENGTH = 1_500;
    private static final String ROAD_BLOCK_ID = "creatediselgenerators:asphalt_block";

    private AkinaMountainBuilder() {
    }

    public static int build(ServerLevel level, BlockPos center) {
        BlockState mountainBlock = Blocks.STONE.defaultBlockState();
        BlockState roadBlock = resolveRoadBlock();

        buildMountain(level, center, mountainBlock);
        return buildRoad(level, center, roadBlock, mountainBlock);
    }

    private static void buildMountain(ServerLevel level, BlockPos center, BlockState mountainBlock) {
        for (int x = -BASE_RADIUS; x <= BASE_RADIUS; x++) {
            for (int z = -BASE_RADIUS; z <= BASE_RADIUS; z++) {
                double distance = Math.sqrt((x * x) + (z * z));
                if (distance > BASE_RADIUS) {
                    continue;
                }

                double normalizedDistance = distance / BASE_RADIUS;
                int localHeight = (int) Math.round(Math.pow(1.0 - normalizedDistance, 1.6) * MOUNTAIN_HEIGHT);
                if (localHeight <= 0) {
                    continue;
                }

                int topY = center.getY() + localHeight;
                int baseY = center.getY() - 20;
                int worldX = center.getX() + x;
                int worldZ = center.getZ() + z;
                for (int y = baseY; y <= topY; y++) {
                    level.setBlockAndUpdate(new BlockPos(worldX, y, worldZ), mountainBlock);
                }
            }
        }
    }

    private static int buildRoad(ServerLevel level, BlockPos center, BlockState roadBlock, BlockState fillerBlock) {
        int halfWidth = ROAD_WIDTH / 2;
        for (int step = 0; step < MAX_ROAD_LENGTH; step++) {
            double progress = step / (double) (MAX_ROAD_LENGTH - 1);
            double angle = progress * Math.PI * 2.0 * 4.0;
            double radius = 68.0 - (progress * 43.0);
            int roadY = center.getY() + 4 + (int) Math.round(progress * 45.0);
            int roadX = center.getX() + (int) Math.round(Math.cos(angle) * radius);
            int roadZ = center.getZ() + (int) Math.round(Math.sin(angle) * radius);

            double tangentX = -Math.sin(angle);
            double tangentZ = Math.cos(angle);
            double tangentLength = Math.sqrt((tangentX * tangentX) + (tangentZ * tangentZ));
            double perpendicularX = tangentZ / tangentLength;
            double perpendicularZ = -tangentX / tangentLength;

            for (int laneOffset = -halfWidth; laneOffset < halfWidth; laneOffset++) {
                int laneX = roadX + (int) Math.round(perpendicularX * laneOffset);
                int laneZ = roadZ + (int) Math.round(perpendicularZ * laneOffset);
                BlockPos roadPos = new BlockPos(laneX, roadY, laneZ);
                level.setBlockAndUpdate(roadPos, roadBlock);
                level.setBlockAndUpdate(roadPos.above(), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(roadPos.above(2), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(roadPos.below(), fillerBlock);
                level.setBlockAndUpdate(roadPos.below(2), fillerBlock);
            }
        }
        return MAX_ROAD_LENGTH;
    }

    private static BlockState resolveRoadBlock() {
        ResourceLocation location = ResourceLocation.parse(ROAD_BLOCK_ID);
        Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(location);
        if (block.isPresent() && block.get() != Blocks.AIR) {
            return block.get().defaultBlockState();
        }

        ExampleMod.LOGGER.warn("Block {} not found, using minecraft:black_concrete as road fallback", ROAD_BLOCK_ID);
        return Blocks.BLACK_CONCRETE.defaultBlockState();
    }
}
