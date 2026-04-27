package com.absolutenarwhal.largeoreveins.veindata;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;

public class VeinInfo {
    // Returns all veins that produce the given block as an output
    public static List<OreVeinConfig> getVeinsForOre(Item oreItem) {
        ResourceLocation oreId = BuiltInRegistries.ITEM.getKey(oreItem);
        ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath(
                oreId.getNamespace(), oreId.getPath()
        );

        return OreVeinConfigLoader.getAllVeins().values().stream()
                .filter(OreVeinConfig::enabled)
                .filter(v -> v.replaceBlocks().containsValue(blockId))
                .toList();
    }

    // get weight as a percentage across all veins in the same dimension
    public static String formatWeight(OreVeinConfig vein) {
        int total = OreVeinConfigLoader.getAllVeins().values().stream()
                .filter(v -> v.enabled() && v.dimension().equals(vein.dimension()))
                .mapToInt(OreVeinConfig::defaultWeight)
                .sum();
        if (total == 0) return "0%";
        double pct = (vein.defaultWeight() * 100.0) / total;
        return String.format("%.1f%%", pct);
    }

    public static Component getName(OreVeinConfig vein) {
        return Component.translatable("ore_vein." + vein.id().getNamespace() + "." + vein.id().getPath());
    }
}
