package com.example.examplemod;

import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.List;

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
    private static final String ROAD_BLOCK_ID = "creatediselgenerators:asphalt_block";
    private static final List<RoutePoint> DOWNHILL_ROUTE = List.of(
            new RoutePoint(0, 4, 46),      // Summit start
            new RoutePoint(18, 10, 43),    // Wide right
            new RoutePoint(8, 20, 40),     // Long left
            new RoutePoint(24, 30, 36),    // S-curve entry
            new RoutePoint(6, 38, 33),     // S-curve exit
            new RoutePoint(-24, 44, 29),   // Hairpin 1
            new RoutePoint(28, 50, 26),    // Hairpin 2
            new RoutePoint(-30, 56, 22),   // Hairpin 3
            new RoutePoint(30, 62, 18),    // Hairpin 4
            new RoutePoint(-28, 68, 14),   // Hairpin 5
            new RoutePoint(-10, 72, 10),   // Medium sweeper
            new RoutePoint(14, 74, 7),     // Final bend
            new RoutePoint(2, 76, 5)       // Base finish
    );

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
        Set<Long> paintedRoadCenters = new HashSet<>();

        for (int index = 0; index < DOWNHILL_ROUTE.size() - 1; index++) {
            RoutePoint from = DOWNHILL_ROUTE.get(index);
            RoutePoint to = DOWNHILL_ROUTE.get(index + 1);
            int deltaX = to.x - from.x;
            int deltaZ = to.z - from.z;
            int steps = Math.max(1, Math.max(Math.abs(deltaX), Math.abs(deltaZ)) * 3);

            for (int step = 0; step <= steps; step++) {
                double progress = step / (double) steps;
                int roadX = center.getX() + (int) Math.round(lerp(from.x, to.x, progress));
                int roadZ = center.getZ() + (int) Math.round(lerp(from.z, to.z, progress));
                int roadY = center.getY() + (int) Math.round(lerp(from.yOffset, to.yOffset, progress));

                paintRoadSlice(level, roadBlock, fillerBlock, halfWidth, roadX, roadY, roadZ, deltaX, deltaZ);
                paintedRoadCenters.add(BlockPos.asLong(roadX, roadY, roadZ));
            }
        }

        return paintedRoadCenters.size();
    }

    private static void paintRoadSlice(ServerLevel level, BlockState roadBlock, BlockState fillerBlock, int halfWidth, int roadX, int roadY, int roadZ, double tangentX, double tangentZ) {
        double tangentLength = Math.sqrt((tangentX * tangentX) + (tangentZ * tangentZ));
        if (tangentLength < 1.0E-4) {
            return;
        }

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

    private static double lerp(int start, int end, double progress) {
        return start + ((end - start) * progress);
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

    private record RoutePoint(int x, int z, int yOffset) {
    }
}
