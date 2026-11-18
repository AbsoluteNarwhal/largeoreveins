package com.absolutenarwhal.largeoreveins.veindata;

import com.absolutenarwhal.largeoreveins.LargeOreVeins;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;

import java.util.*;

public class OreVeinConfigLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FOLDER = "ore_veins";

    private static final Map<ResourceLocation, OreVeinConfig> VEINS = new HashMap<>();

    public OreVeinConfigLoader() {
        super(GSON, FOLDER);
    }

    public static Map<ResourceLocation, OreVeinConfig> getAllVeins() {
        return Collections.unmodifiableMap(VEINS);
    }

    public static Optional<OreVeinConfig> getVein(ResourceLocation id) {
        return Optional.ofNullable(VEINS.get(id));
    }

    public static List<OreVeinConfig> getVeinsForDimension(ResourceLocation dimension) {
        return VEINS.values().stream()
            .filter(v -> v.enabled() && v.dimension().equals(dimension))
            .toList();
    }

    @Override
    protected void apply(
        Map<ResourceLocation, JsonElement> resourceLocationJsonElementMap,
        ResourceManager resourceManager,
        ProfilerFiller profilerFiller
    ) {
        VEINS.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : resourceLocationJsonElementMap.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            try {
                OreVeinConfig config = parse(fileId, entry.getValue().getAsJsonObject());
                // allows datapack to override mod vein by using the same id
                VEINS.put(config.id(), config);
                LargeOreVeins.LOGGER.debug("Loaded large ore vein: {}", config.id());
            } catch (Exception e) {
                LargeOreVeins.LOGGER.error("Failed to load large ore vein from {}: {}", fileId, e.getMessage());
            }
        }

        LargeOreVeins.LOGGER.info("Loaded {} large ore vein configs", VEINS.size());
    }

    private static OreVeinConfig parse(ResourceLocation fileId, JsonObject json) {
        // id
        ResourceLocation id = ResourceLocation.parse(
            json.get("id").getAsString()
        );

        // dimension
        ResourceLocation dimension = ResourceLocation.parse(
            json.get("dimension").getAsString()
        );

        // ore blocks to use
        Map<ResourceLocation, List<WeightedBlock>> replaceBlocks = new LinkedHashMap<>();
        JsonObject rb = json.getAsJsonObject("replace_blocks");
        for (Map.Entry<String, JsonElement> e : rb.entrySet()) {
            ResourceLocation sourceBlock = ResourceLocation.parse(e.getKey());
            JsonElement value = e.getValue();

            if (value.isJsonArray()) {
                // New format: [{"block": "minecraft:iron_ore", "weight": 19}, ...]
                List<WeightedBlock> options = new ArrayList<>();
                for (JsonElement element : value.getAsJsonArray()) {
                    JsonObject obj = element.getAsJsonObject();
                    ResourceLocation block = ResourceLocation.parse(obj.get("block").getAsString());
                    int weight = obj.has("weight") ? obj.get("weight").getAsInt() : 1;
                    options.add(new WeightedBlock(block, weight));
                }
                replaceBlocks.put(sourceBlock, options);
            } else {
                // convert to a single-entry list with weight 1
                replaceBlocks.put(sourceBlock, List.of(
                        new WeightedBlock(ResourceLocation.parse(value.getAsString()), 1)
                ));
            }
        }

        // vein sizes
        int maxY = json.has("maxY")
                ? json.get("maxY").getAsInt()
                : 60;

        int minY = json.has("minY")
                ? json.get("minY").getAsInt()
                : 0;

        int size = json.has("size")
                ? json.get("size").getAsInt()
                : 25;

        // vein types
        VeinType type = json.has("type")
            ? VeinType.valueOf(json.get("type").getAsString().toUpperCase())
            : VeinType.BLOB;

        // weight
        int defaultWeight = json.has("default_weight")
            ? json.get("default_weight").getAsInt()
            : 1;

        // biome weights
        Map<String, Integer> biomeWeights = new LinkedHashMap<>();
        if (json.has("biome_weights")) {
            JsonObject bw = json.getAsJsonObject("biome_weights");
            for (Map.Entry<String, JsonElement> e : bw.entrySet()) {
                biomeWeights.put(e.getKey(), e.getValue().getAsInt());
            }
        }

        // enabled
        boolean enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();

        return new OreVeinConfig(
            id, dimension, replaceBlocks,
            maxY, minY, size, type,
            defaultWeight, biomeWeights, enabled
        );
    }
}
