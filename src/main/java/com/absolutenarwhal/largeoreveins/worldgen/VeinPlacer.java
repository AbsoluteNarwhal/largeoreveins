package com.absolutenarwhal.largeoreveins.worldgen;

import com.absolutenarwhal.largeoreveins.veindata.OreVeinConfig;
import com.absolutenarwhal.largeoreveins.veindata.WeightedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.List;
import java.util.Random;

public class VeinPlacer {
    public static void placeInChunk(OreVeinConfig vein, BlockPos origin, LevelChunk chunk, Random random) {
        switch (vein.type()) {
            case VEIN  -> placeVein(vein, origin, chunk, random);
            case BLOB  -> placeBlob(vein, origin, chunk, random, vein.size());
            case PLATE -> placePlate(vein, origin, chunk, random);
        }
    }

    private static void placeVein(OreVeinConfig vein, BlockPos origin, LevelChunk chunk, Random random) {
        int branches = vein.size() / 2;
        int blobSize = vein.size() / 3;

        BlockPos current = origin;
        for (int b = 0; b < branches; b++) {
            placeBlob(vein, current, chunk, random, blobSize);
            current = current.offset(
                random.nextInt(blobSize) - blobSize / 2,
                random.nextInt(3) - 1,
                random.nextInt(blobSize) - blobSize / 2
            );
        }
    }

    private static void placeBlob(OreVeinConfig vein, BlockPos origin, LevelChunk chunk, Random random, int size) {
        float radius = size / 2.0f;
        for (int dx = -(int) radius; dx <= (int) radius; dx++) {
            for (int dy = -(int) radius; dy <= (int) radius; dy++) {
                for (int dz = -(int) radius; dz <= (int) radius; dz++) {
                    float dist = (dx * dx) / (radius * radius)
                        + (dy * dy) / (radius * 0.75f * radius * 0.75f)
                        + (dz * dz) / (radius * radius);
                    if (dist > 1.0f) continue;
                    float chance = 1.0f - (dist * 0.8f);
                    if (random.nextFloat() > chance) continue;
                    tryPlace(vein, origin.offset(dx, dy, dz), chunk, random);
                }
            }
        }
    }

    private static void placePlate(OreVeinConfig vein, BlockPos origin, LevelChunk chunk, Random random) {
        float radius = vein.size() / 2.0f;
        int thickness = 1 + random.nextInt(3);
        for (int dx = -(int) radius; dx <= (int) radius; dx++) {
            for (int dz = -(int) radius; dz <= (int) radius; dz++) {
                float dist = (dx * dx + dz * dz) / (radius * radius);
                if (dist > 1.0f) continue;
                if (random.nextFloat() > 1.0f - (dist * 0.6f)) continue;
                for (int dy = 0; dy < thickness; dy++) {
                    if (dy > 0 && random.nextFloat() > 0.7f) continue;
                    tryPlace(vein, origin.offset(dx, dy, dz), chunk, random);
                }
            }
        }
    }

    private static void tryPlace(OreVeinConfig vein, BlockPos pos, LevelChunk chunk, Random random) {
        if (pos.getY() < vein.minY() || pos.getY() > vein.maxY()) return;

        ChunkPos chunkPos = chunk.getPos();
        if (pos.getX() < chunkPos.getMinBlockX() || pos.getX() > chunkPos.getMaxBlockX()) return;
        if (pos.getZ() < chunkPos.getMinBlockZ() || pos.getZ() > chunkPos.getMaxBlockZ()) return;

        int sectionIndex = chunk.getSectionIndex(pos.getY());
        if (sectionIndex < 0 || sectionIndex >= chunk.getSectionsCount()) return;
        LevelChunkSection section = chunk.getSection(sectionIndex);

        int localX = pos.getX() & 15;
        int localY = pos.getY() & 15;
        int localZ = pos.getZ() & 15;

        BlockState existing = section.getBlockState(localX, localY, localZ);
        ResourceLocation existingId = BuiltInRegistries.BLOCK.getKey(existing.getBlock());

        List<WeightedBlock> options = vein.replaceBlocks().get(existingId);
        if (options == null || options.isEmpty()) return;

        Block block = selectWeightedBlock(options, random);
        if (block == null) return;

        section.setBlockState(localX, localY, localZ, block.defaultBlockState(), false);
    }

    private static Block selectWeightedBlock(List<WeightedBlock> options, Random random) {
        if (options.size() == 1) {
            // Fast path for the common case
            return BuiltInRegistries.BLOCK.get(options.get(0).block());
        }

        int totalWeight = options.stream().mapToInt(WeightedBlock::weight).sum();
        if (totalWeight <= 0) return null;

        int roll = random.nextInt(totalWeight);
        int cursor = 0;
        for (WeightedBlock option : options) {
            cursor += option.weight();
            if (roll < cursor) return BuiltInRegistries.BLOCK.get(option.block());
        }
        return null;
    }
}
