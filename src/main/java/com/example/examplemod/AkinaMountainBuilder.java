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
    private static final int ROAD_WIDTH = 8;
    private static final String ROAD_BLOCK_ID = "creatediselgenerators:asphalt_block";
    private static final List<RoutePoint> DOWNHILL_ROUTE = List.of(
            new RoutePoint(84, 8, 64),
            new RoutePoint(72, 4, 63),
            new RoutePoint(62, 0, 62),
            new RoutePoint(54, 10, 60),
            new RoutePoint(40, 4, 58),
            new RoutePoint(28, -6, 56),
            new RoutePoint(10, -2, 54),
            new RoutePoint(-6, 8, 52),
            new RoutePoint(-20, 0, 50),
            new RoutePoint(-30, -12, 48),
            new RoutePoint(-20, -22, 47),
            new RoutePoint(-8, -26, 46),
            new RoutePoint(8, -24, 45),
            new RoutePoint(22, -18, 44),
            new RoutePoint(16, -8, 43),
            new RoutePoint(2, -4, 42),
            new RoutePoint(-10, 2, 41),
            new RoutePoint(-22, 10, 40),
            new RoutePoint(-34, 18, 39),
            new RoutePoint(-42, 28, 37),
            new RoutePoint(-48, 40, 35),
            new RoutePoint(-42, 48, 33),
            new RoutePoint(-52, 56, 31),
            new RoutePoint(-44, 64, 29),
            new RoutePoint(-56, 72, 27),
            new RoutePoint(-46, 80, 25),
            new RoutePoint(-48, 92, 23),
            new RoutePoint(-44, 104, 22),
            new RoutePoint(-34, 112, 20),
            new RoutePoint(-18, 118, 19),
            new RoutePoint(0, 120, 18),
            new RoutePoint(20, 116, 17),
            new RoutePoint(34, 110, 16),
            new RoutePoint(24, 102, 15),
            new RoutePoint(8, 98, 14),
            new RoutePoint(-10, 100, 13),
            new RoutePoint(-28, 106, 12),
            new RoutePoint(-46, 112, 11),
            new RoutePoint(-60, 120, 10),
            new RoutePoint(-72, 132, 9),
            new RoutePoint(-68, 144, 8),
            new RoutePoint(-52, 146, 7),
            new RoutePoint(-38, 142, 6),
            new RoutePoint(-24, 146, 5),
            new RoutePoint(-8, 142, 4),
            new RoutePoint(8, 146, 3),
            new RoutePoint(22, 140, 2),
            new RoutePoint(30, 132, 1),
            new RoutePoint(26, 122, 0),
            new RoutePoint(14, 118, -1),
            new RoutePoint(-2, 120, -2),
            new RoutePoint(-18, 126, -3),
            new RoutePoint(-30, 136, -4),
            new RoutePoint(-40, 148, -5),
            new RoutePoint(-52, 160, -6),
            new RoutePoint(-64, 172, -7),
            new RoutePoint(-76, 184, -8),
            new RoutePoint(-68, 194, -9),
            new RoutePoint(-58, 186, -10),
            new RoutePoint(-64, 174, -11),
            new RoutePoint(-74, 182, -12),
            new RoutePoint(-82, 198, -13),
            new RoutePoint(-78, 214, -14),
            new RoutePoint(-66, 224, -15),
            new RoutePoint(-56, 236, -16),
            new RoutePoint(-50, 250, -17),
            new RoutePoint(-56, 264, -18),
            new RoutePoint(-64, 276, -19),
            new RoutePoint(-70, 290, -20),
            new RoutePoint(-76, 304, -21),
            new RoutePoint(-82, 320, -22)
    );

    private AkinaMountainBuilder() {
    }

    public static int build(ServerLevel level, BlockPos center) {
        BlockState roadBlock = resolveRoadBlock();
        BlockState fillerBlock = Blocks.STONE.defaultBlockState();
        return buildRoad(level, center, roadBlock, fillerBlock);
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
