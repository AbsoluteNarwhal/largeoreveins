package com.absolutenarwhal.largeoreveins.veindata;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public record OreVeinConfig(
        ResourceLocation id,
        ResourceLocation dimension,
        Map<ResourceLocation, List<WeightedBlock>> replaceBlocks,
        int maxY,
        int minY,
        int size,
        VeinType type,
        int defaultWeight,
        Map<String, Integer> biomeWeights,
        boolean enabled
) {}
