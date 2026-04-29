package com.absolutenarwhal.largeoreveins.worldgen;

import com.absolutenarwhal.largeoreveins.Config;
import com.absolutenarwhal.largeoreveins.veindata.OreVeinConfig;
import com.absolutenarwhal.largeoreveins.veindata.OreVeinConfigLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OreVeinGenerator {
    // how many chunks away a vein can reach
    private static final int VEIN_CHUNK_RADIUS = 3;

    // How many chunks to process per tick — higher = faster generation but more lag
    private static final int CHUNKS_PER_TICK = 10;

    // Chunks waiting to be processed
    private static final Queue<Map.Entry<ResourceLocation, ChunkPos>> PENDING =
            new ConcurrentLinkedQueue<>();

    // chunks whose vein roll has already been decided (origin stored or no vein)
    private static final Map<ResourceLocation, Set<Long>> ROLLED_CHUNKS = new HashMap<>();

    // vein origins that were actually spawned
    private static final Map<ResourceLocation, Map<Long, VeinOrigin>> VEIN_ORIGINS = new HashMap<>();

    public static void clearGeneratedChunks() {
        PENDING.clear();
        ROLLED_CHUNKS.clear();
        VEIN_ORIGINS.clear();
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ResourceLocation dimension = level.dimension().location();
        ChunkPos pos = event.getChunk().getPos();

        // add chunk to queue for processing
        PENDING.add(Map.entry(dimension, pos));
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        int processed = 0;

        while (!PENDING.isEmpty() && processed < CHUNKS_PER_TICK) {
            Map.Entry<ResourceLocation, ChunkPos> entry = PENDING.poll();
            ResourceLocation dimension = entry.getKey();
            ChunkPos pos = entry.getValue();

            ServerLevel level = event.getServer().getLevel(
                    ResourceKey.create(Registries.DIMENSION, dimension)
            );

            if (level == null) continue;
            if (!level.hasChunk(pos.x, pos.z)) continue;

            processChunk(pos, dimension, level);
            processed++;
        }
    }

    private static void processChunk(ChunkPos pos, ResourceLocation dimension, ServerLevel level) {
        for (int dx = -VEIN_CHUNK_RADIUS; dx <= VEIN_CHUNK_RADIUS; dx++) {
            for (int dz = -VEIN_CHUNK_RADIUS; dz <= VEIN_CHUNK_RADIUS; dz++) {
                rollChunkIfNeeded(new ChunkPos(pos.x + dx, pos.z + dz), dimension, level);
            }
        }

        Map<Long, VeinOrigin> origins = VEIN_ORIGINS.getOrDefault(dimension, Collections.emptyMap());
        LevelChunk chunk = level.getChunk(pos.x, pos.z);

        for (int dx = -VEIN_CHUNK_RADIUS; dx <= VEIN_CHUNK_RADIUS; dx++) {
            for (int dz = -VEIN_CHUNK_RADIUS; dz <= VEIN_CHUNK_RADIUS; dz++) {
                ChunkPos nearbyPos = new ChunkPos(pos.x + dx, pos.z + dz);
                VeinOrigin origin = origins.get(nearbyPos.toLong());
                if (origin == null) continue;

                OreVeinConfig vein = OreVeinConfigLoader.getVein(origin.veinId()).orElse(null);
                if (vein == null || !vein.enabled()) continue;

                Random random = seededRandom(level.getSeed(), nearbyPos.x, nearbyPos.z, dimension);
                VeinPlacer.placeInChunk(vein, origin.origin(), chunk, random);
            }
        }
    }

    private static void rollChunkIfNeeded(ChunkPos pos, ResourceLocation dimension, ServerLevel level) {
        Set<Long> rolled = ROLLED_CHUNKS.computeIfAbsent(dimension, k -> new HashSet<>());
        if (!rolled.add(pos.toLong())) return;

        Random random = seededRandom(level.getSeed(), pos.x, pos.z, dimension);
        if (random.nextInt(Config.CHUNKS_PER_VEIN.getAsInt()) != 0) return;

        List<OreVeinConfig> candidates = OreVeinConfigLoader.getVeinsForDimension(dimension)
            .stream()
            .filter(OreVeinConfig::enabled)
            .toList();

        if (candidates.isEmpty()) return;

        OreVeinConfig selected = selectWeightedNoLevel(candidates, random);
        if (selected == null) return;

        int x = pos.getMinBlockX() + random.nextInt(16);
        int z = pos.getMinBlockZ() + random.nextInt(16);
        int y = selected.minY() + random.nextInt(Math.max(1, selected.maxY() - selected.minY()));

        VEIN_ORIGINS.computeIfAbsent(dimension, k -> new HashMap<>())
            .put(pos.toLong(), new VeinOrigin(selected.id(), new BlockPos(x, y, z)));
    }

    private static OreVeinConfig selectWeightedNoLevel(List<OreVeinConfig> candidates, Random random) {
        int totalWeight = candidates.stream()
            .mapToInt(OreVeinConfig::defaultWeight)
            .sum();

        if (totalWeight <= 0) return null;

        int roll = random.nextInt(totalWeight);
        int cursor = 0;
        for (OreVeinConfig vein : candidates) {
            cursor += vein.defaultWeight();
            if (roll < cursor) return vein;
        }
        return null;
    }

    private static Random seededRandom(long worldSeed, int chunkX, int chunkZ, ResourceLocation dimension) {
        long seed = worldSeed
            ^ ((long) chunkX * 341873128712L)
            ^ ((long) chunkZ * 132897987541L)
            ^ dimension.toString().hashCode();
        return new Random(seed);
    }
}
