package com.absolutenarwhal.largeoreveins;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue CHUNKS_PER_VEIN = BUILDER
            .comment("How many chunks per large ore vein, e.g. if this is 5, then each chunk has a 1/5 chance of containing a vein.")
            .defineInRange("chunksPerVein", 3, 1, Integer.MAX_VALUE);

    static final ModConfigSpec SPEC = BUILDER.build();
}
