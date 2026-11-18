package com.absolutenarwhal.largeoreveins.worldgen;

import com.absolutenarwhal.largeoreveins.LargeOreVeins;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class PlacedChunksSavedData extends SavedData {

    private static final String NAME = LargeOreVeins.MOD_ID + "_placed_chunks";
    private final Set<Long> placedChunks = new HashSet<>();

    public PlacedChunksSavedData() {}

    public PlacedChunksSavedData(CompoundTag tag) {
        LongArrayTag array = (LongArrayTag) tag.get("placed");
        if (array != null) {
            for (long l : array.getAsLongArray()) {
                placedChunks.add(l);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put("placed", new LongArrayTag(new ArrayList<>(placedChunks)));
        return tag;
    }

    public boolean isPlaced(ChunkPos pos) {
        return placedChunks.contains(pos.toLong());
    }

    public void markPlaced(ChunkPos pos) {
        placedChunks.add(pos.toLong());
        setDirty(); // tells Minecraft to save this data
    }

    public static PlacedChunksSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<PlacedChunksSavedData>(
                        PlacedChunksSavedData::new,
                        (tag, provider) -> new PlacedChunksSavedData(tag),
                        null
                ),
                NAME
        );
    }
}
