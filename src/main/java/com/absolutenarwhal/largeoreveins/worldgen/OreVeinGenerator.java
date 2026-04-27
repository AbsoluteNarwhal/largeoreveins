package com.absolutenarwhal.largeoreveins.worldgen;

import com.absolutenarwhal.largeoreveins.Config;
import com.absolutenarwhal.largeoreveins.veindata.OreVeinConfig;
import com.absolutenarwhal.largeoreveins.veindata.OreVeinConfigLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.*;

public class OreVeinGenerator {
    // how many chunks away a vein can reach
    private static final int VEIN_CHUNK_RADIUS = 3;

    // chunks whose vein roll has already been decided (origin stored or no vein)
    private static final Map<ResourceLocation, Set<Long>> ROLLED_CHUNKS = new HashMap<>();

    // vein origins that were actually spawned
    private static final Map<ResourceLocation, Map<Long, VeinOrigin>> VEIN_ORIGINS = new HashMap<>();

    public static void clearGeneratedChunks() {
        ROLLED_CHUNKS.clear();
        VEIN_ORIGINS.clear();
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk chunk)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ChunkPos pos = chunk.getPos();
        ResourceLocation dimension = level.dimension().location();

        // ensure this chunk and all chunks within VEIN_CHUNK_RADIUS have been rolled for vein origins
        for (int dx = -VEIN_CHUNK_RADIUS; dx <= VEIN_CHUNK_RADIUS; dx++) {
            for (int dz = -VEIN_CHUNK_RADIUS; dz <= VEIN_CHUNK_RADIUS; dz++) {
                ChunkPos nearbyPos = new ChunkPos(pos.x + dx, pos.z + dz);
                rollChunkIfNeeded(nearbyPos, dimension, level);
            }
        }

        // collect all vein origins from nearby chunks and place the parts of each vein that fall within this chunk
        Map<Long, VeinOrigin> origins = VEIN_ORIGINS.getOrDefault(dimension, Collections.emptyMap());

        for (int dx = -VEIN_CHUNK_RADIUS; dx <= VEIN_CHUNK_RADIUS; dx++) {
            for (int dz = -VEIN_CHUNK_RADIUS; dz <= VEIN_CHUNK_RADIUS; dz++) {
                ChunkPos nearbyPos = new ChunkPos(pos.x + dx, pos.z + dz);
                VeinOrigin origin = origins.get(nearbyPos.toLong());
                if (origin == null) continue;

                OreVeinConfig vein = OreVeinConfigLoader.getVein(origin.veinId()).orElse(null);
                if (vein == null || !vein.enabled()) continue;

                // seed is from the origin chunk
                Random random = seededRandom(
                        level.getSeed(),
                        nearbyPos.x, nearbyPos.z,
                        dimension
                );

                VeinPlacer.placeInChunk(vein, origin.origin(), chunk, random);
            }
        }
    }

    private static void rollChunkIfNeeded(ChunkPos pos, ResourceLocation dimension, ServerLevel level) {
        Set<Long> rolled = ROLLED_CHUNKS.computeIfAbsent(dimension, k -> new HashSet<>());
        if (!rolled.add(pos.toLong())) return; // already rolled

        Random random = seededRandom(level.getSeed(), pos.x, pos.z, dimension);
        if (random.nextInt(Config.CHUNKS_PER_VEIN.getAsInt()) != 0) return; // no vein this chunk

        List<OreVeinConfig> candidates = OreVeinConfigLoader.getVeinsForDimension(dimension)
                .stream()
                .filter(OreVeinConfig::enabled)
                .toList();

        if (candidates.isEmpty()) return;

        OreVeinConfig selected = selectWeighted(candidates, pos, level, random);
        if (selected == null) return;

        // Pick origin within the chunk
        int x = pos.getMinBlockX() + random.nextInt(16);
        int z = pos.getMinBlockZ() + random.nextInt(16);
        int y = selected.minY() + random.nextInt(Math.max(1, selected.maxY() - selected.minY()));

        VEIN_ORIGINS
                .computeIfAbsent(dimension, k -> new HashMap<>())
                .put(pos.toLong(), new VeinOrigin(selected.id(), new BlockPos(x, y, z)));
    }

    private static OreVeinConfig selectWeighted(
            List<OreVeinConfig> candidates,
            ChunkPos pos,
            ServerLevel level,
            Random random
    ) {
        // sample the biome at chunk centre, mid-height
        BlockPos centre = pos.getMiddleBlockPosition(level.getSeaLevel());
        Holder<Biome> biome = level.getBiome(centre);

        int totalWeight = candidates.stream()
                .mapToInt(v -> resolveWeight(v, biome, level))
                .sum();

        if (totalWeight <= 0) return null;

        int roll = random.nextInt(totalWeight);
        int cursor = 0;

        for (OreVeinConfig vein : candidates) {
            cursor += resolveWeight(vein, biome, level);
            if (roll < cursor) return vein;
        }

        return null;
    }

    private static int resolveWeight(
            OreVeinConfig vein,
            Holder<Biome> biome,
            ServerLevel level
    ) {
        for (Map.Entry<String, Integer> entry : vein.biomeWeights().entrySet()) {
            String key = entry.getKey();

            if (key.startsWith("#")) {
                // biome tag
                ResourceLocation tagId = ResourceLocation.parse(key.substring(1));
                TagKey<Biome> tag = TagKey.create(Registries.BIOME, tagId);
                if (biome.is(tag)) return entry.getValue();
            } else {
                // biome ID
                ResourceLocation biomeId = ResourceLocation.parse(key);
                if (biome.is(ResourceKey.create(Registries.BIOME, biomeId))) {
                    return entry.getValue();
                }
            }
        }

        return vein.defaultWeight();
    }

    private static Random seededRandom(long worldSeed, int chunkX, int chunkZ, ResourceLocation dimension) {
        long seed = worldSeed
            ^ ((long) chunkX * 341873128712L)
            ^ ((long) chunkZ * 132897987541L)
            ^ dimension.toString().hashCode();
        return new Random(seed);
    }
}
