package com.absolutenarwhal.largeoreveins.veindata;

import net.minecraft.resources.ResourceLocation;
import java.util.Map;

public record OreVeinConfig(
        ResourceLocation id,
        ResourceLocation dimension,
        Map<ResourceLocation, ResourceLocation> replaceBlocks,
        ResourceLocation rareBlock,
        double rareBlockChance,
        int maxY,
        int minY,
        int size,
        VeinType type,
        int defaultWeight,
        Map<String, Integer> biomeWeights,
        boolean enabled
) {}
