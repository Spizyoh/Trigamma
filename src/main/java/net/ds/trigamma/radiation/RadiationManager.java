package net.ds.trigamma.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores a radiation level (float, in rads) per chunk position for a given dimension.
 * Persisted via SavedData so values survive server restarts.
 *
 * Radiation sources (radioactive blocks, reactor cores, waste barrels, etc.) should call
 * {@link #addRadiation(ChunkPos, float)} to contribute their radiation to nearby chunks.
 */
public class RadiationManager extends SavedData {

    private static final String DATA_KEY = "trigamma_radiation";

    // Chunk -> radiation level (rads)
    private final Map<ChunkPos, Float> chunkRadiation = new HashMap<>();

    // ── Updated Factory Retrieval ─────────────────────────────────────────────
    public static RadiationManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                // Added DataFixTypes.LEVEL (or similar) to the factory
                new SavedData.Factory<>(
                        RadiationManager::new,
                        RadiationManager::load,
                        DataFixTypes.LEVEL
                ),
                DATA_KEY
        );
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** Returns the radiation level at the chunk containing the given block position. */
    public float getRadiation(BlockPos pos) {
        return chunkRadiation.getOrDefault(new ChunkPos(pos), 0f);
    }

    public float getRadiation(ChunkPos chunk) {
        return chunkRadiation.getOrDefault(chunk, 0f);
    }

    /**
     * Sets an absolute radiation level for a chunk.
     * Use this when placing/removing radioactive blocks to recalculate the chunk level.
     */
    public void setRadiation(ChunkPos chunk, float rads) {
        if (rads <= 0f) {
            chunkRadiation.remove(chunk);
        } else {
            chunkRadiation.put(chunk, Math.min(rads, 10000f));
        }
        setDirty();
    }

    /**
     * Adds radiation to a chunk (cumulative from multiple sources).
     * Negative values will reduce it (use to simulate decay of temporary sources).
     */
    public void addRadiation(ChunkPos chunk, float delta) {
        float current = chunkRadiation.getOrDefault(chunk, 0f);
        setRadiation(chunk, current + delta);
    }

    /**
     * Radiates outward from a center chunk, falling off with chunk distance.
     * Useful for block-placed radioactive sources that affect neighbours.
     *
     * @param center   chunk at the source
     * @param strength base rads contributed to the center chunk
     * @param radius   how many chunks out to spread (each step halves the strength)
     */
    public void spreadRadiation(ChunkPos center, float strength, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int dist = Math.abs(dx) + Math.abs(dz); // Manhattan distance in chunks
                if (dist > radius) continue;
                float falloff = strength / (1 + dist * dist);
                addRadiation(new ChunkPos(center.x + dx, center.z + dz), falloff);
            }
        }
    }

    // ── Serialisation ─────────────────────────────────────────────────────────

    /** Updated to include HolderLookup.Provider */
    public static RadiationManager load(CompoundTag tag, HolderLookup.Provider registries) {
        RadiationManager mgr = new RadiationManager();
        ListTag list = tag.getList("chunks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            ChunkPos pos = new ChunkPos(c.getInt("x"), c.getInt("z"));
            float rads = c.getFloat("rads");
            mgr.chunkRadiation.put(pos, rads);
        }
        return mgr;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<ChunkPos, Float> entry : chunkRadiation.entrySet()) {
            CompoundTag c = new CompoundTag();
            c.putInt("x", entry.getKey().x);
            c.putInt("z", entry.getKey().z);
            c.putFloat("rads", entry.getValue());
            list.add(c);
        }
        tag.put("chunks", list);
        return tag;
    }
}