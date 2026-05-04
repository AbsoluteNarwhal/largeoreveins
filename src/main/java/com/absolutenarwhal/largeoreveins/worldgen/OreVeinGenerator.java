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
import java.util.concurrent.atomic.AtomicInteger;

public class OreVeinGenerator {
    private static final int VEIN_CHUNK_RADIUS = 3;

    private static final Map<ResourceLocation, Set<Long>> ROLLED_CHUNKS = new HashMap<>();
    private static final Map<ResourceLocation, Set<Long>> PLACED_CHUNKS = new HashMap<>();
    private static final Map<ResourceLocation, Map<Long, VeinOrigin>> VEIN_ORIGINS = new HashMap<>();

    private static final Queue<Map.Entry<ResourceLocation, ChunkPos>> PENDING =
            new ConcurrentLinkedQueue<>();

    public static void clearGeneratedChunks() {
        PENDING.clear();
        ROLLED_CHUNKS.clear();
        VEIN_ORIGINS.clear();
        PLACED_CHUNKS.clear();
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ResourceLocation dimension = level.dimension().location();
        ChunkPos pos = event.getChunk().getPos();

        if (PLACED_CHUNKS.getOrDefault(dimension, Collections.emptySet()).contains(pos.toLong())) return;

        PENDING.add(Map.entry(dimension, pos));
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        int processed = 0;
        while (!PENDING.isEmpty() && processed < Config.CHUNKS_PER_TICK.getAsInt()) {
            Map.Entry<ResourceLocation, ChunkPos> entry = PENDING.poll();
            if (entry == null) break;

            ResourceLocation dimension = entry.getKey();
            ChunkPos pos = entry.getValue();

            // skip if placed since being enqueued
            if (PLACED_CHUNKS.getOrDefault(dimension, Collections.emptySet()).contains(pos.toLong())) continue;

            ServerLevel level = event.getServer().getLevel(
                    ResourceKey.create(Registries.DIMENSION, dimension)
            );
            if (level == null) continue;

            // if chunk unloaded since being enqueued, don't drop it —
            // it will re-enqueue itself via onChunkLoad when it reloads
            if (!level.hasChunk(pos.x, pos.z)) continue;

            processChunk(pos, dimension, level);
            processed++;
        }
    }

    private static void processChunk(ChunkPos pos, ResourceLocation dimension, ServerLevel level) {
        List<OreVeinConfig> candidates = OreVeinConfigLoader.getVeinsForDimension(dimension)
            .stream()
            .filter(OreVeinConfig::enabled)
            .toList();

        // configs not loaded yet, re-queue for next tick
        if (candidates.isEmpty()) {
            PENDING.add(Map.entry(dimension, pos));
            return;
        }

        // roll all neighbours so their origins exist before lookup
        for (int dx = -VEIN_CHUNK_RADIUS; dx <= VEIN_CHUNK_RADIUS; dx++) {
            for (int dz = -VEIN_CHUNK_RADIUS; dz <= VEIN_CHUNK_RADIUS; dz++) {
                rollChunkIfNeeded(new ChunkPos(pos.x + dx, pos.z + dz), dimension, level, candidates);
            }
        }

        // place after rolling so all nearby origins are populated
        LevelChunk chunk = level.getChunk(pos.x, pos.z);
        Map<Long, VeinOrigin> origins = VEIN_ORIGINS.getOrDefault(dimension, Collections.emptyMap());
        boolean anyPlaced = false;

        for (int dx = -VEIN_CHUNK_RADIUS; dx <= VEIN_CHUNK_RADIUS; dx++) {
            for (int dz = -VEIN_CHUNK_RADIUS; dz <= VEIN_CHUNK_RADIUS; dz++) {
                ChunkPos nearbyPos = new ChunkPos(pos.x + dx, pos.z + dz);
                VeinOrigin origin = origins.get(nearbyPos.toLong());
                if (origin == null) continue;

                OreVeinConfig vein = OreVeinConfigLoader.getVein(origin.veinId()).orElse(null);
                if (vein == null || !vein.enabled()) continue;

                Random random = seededRandom(level.getSeed(), nearbyPos.x, nearbyPos.z, dimension);
                VeinPlacer.placeInChunk(vein, origin.origin(), chunk, random);
                anyPlaced = true;
            }
        }

        if (anyPlaced || !origins.isEmpty()) {
            PLACED_CHUNKS.computeIfAbsent(dimension, k -> new HashSet<>()).add(pos.toLong());
        }
    }

    private static void rollChunkIfNeeded(ChunkPos pos, ResourceLocation dimension, ServerLevel level, List<OreVeinConfig> candidates) {
        if (!ROLLED_CHUNKS.computeIfAbsent(dimension, k -> new HashSet<>()).add(pos.toLong())) return;

        Random random = seededRandom(level.getSeed(), pos.x, pos.z, dimension);
        if (random.nextInt(Config.CHUNKS_PER_VEIN.getAsInt()) != 0) return;

        OreVeinConfig selected = selectWeighted(candidates, random);
        if (selected == null) return;

        int x = pos.getMinBlockX() + random.nextInt(16);
        int z = pos.getMinBlockZ() + random.nextInt(16);
        int y = selected.minY() + random.nextInt(Math.max(1, selected.maxY() - selected.minY()));

        VEIN_ORIGINS
            .computeIfAbsent(dimension, k -> new HashMap<>())
            .put(pos.toLong(), new VeinOrigin(selected.id(), new BlockPos(x, y, z)));
    }

    private static OreVeinConfig selectWeighted(List<OreVeinConfig> candidates, Random random) {
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
