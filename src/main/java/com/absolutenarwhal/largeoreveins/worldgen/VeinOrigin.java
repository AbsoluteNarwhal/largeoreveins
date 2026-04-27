package com.absolutenarwhal.largeoreveins.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record VeinOrigin(ResourceLocation veinId, BlockPos origin) {}
