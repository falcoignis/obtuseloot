package obtuseloot.events;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class ChunkSenseCacheCodec {
    private final NamespacedKey lastSenseAt;
    private final NamespacedKey structureDetected;

    public ChunkSenseCacheCodec(Plugin plugin) {
        this.lastSenseAt = new NamespacedKey(plugin, "chunk-sense-last-ms");
        this.structureDetected = new NamespacedKey(plugin, "chunk-sense-has-structure");
    }

    public StructureSenseSnapshot read(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        Long last = pdc.get(lastSenseAt, PersistentDataType.LONG);
        Byte detected = pdc.get(structureDetected, PersistentDataType.BYTE);
        if (last == null || detected == null) {
            return null;
        }
        return new StructureSenseSnapshot(last, detected == (byte) 1);
    }

    public void write(Chunk chunk, StructureSenseSnapshot snapshot) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        pdc.set(lastSenseAt, PersistentDataType.LONG, snapshot.timestampMs());
        pdc.set(structureDetected, PersistentDataType.BYTE, snapshot.detected() ? (byte) 1 : (byte) 0);
    }

    public record StructureSenseSnapshot(long timestampMs, boolean detected) {}
}
