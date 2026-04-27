package com.absolutenarwhal.largeoreveins.worldgen;

import com.absolutenarwhal.largeoreveins.veindata.OreVeinConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

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
                    tryPlace(vein, origin.offset(dx, dy, dz), chunk);
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
                    tryPlace(vein, origin.offset(dx, dy, dz), chunk);
                }
            }
        }
    }

    private static void tryPlace(OreVeinConfig vein, BlockPos pos, LevelChunk chunk) {
        if (pos.getY() < vein.minY() || pos.getY() > vein.maxY()) return;

        // no accessing neighbour chunk
        ChunkPos chunkPos = chunk.getPos();
        if (pos.getX() < chunkPos.getMinBlockX() || pos.getX() > chunkPos.getMaxBlockX()) return;
        if (pos.getZ() < chunkPos.getMinBlockZ() || pos.getZ() > chunkPos.getMaxBlockZ()) return;

        BlockState existing = chunk.getBlockState(pos);
        ResourceLocation existingId = BuiltInRegistries.BLOCK.getKey(existing.getBlock());

        ResourceLocation replacement = vein.replaceBlocks().get(existingId);
        if (replacement == null) return;

        Block replacementBlock = BuiltInRegistries.BLOCK.get(replacement);
        chunk.setBlockState(pos, replacementBlock.defaultBlockState(), false);
    }
}
