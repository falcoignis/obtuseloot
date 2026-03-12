package obtuseloot.telemetry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ArtifactRuntimeCache<V> {
    private final Map<Long, CacheValue<V>> values = new ConcurrentHashMap<>();
    private final int maxEntries;
    private final long idleExpireMs;

    public ArtifactRuntimeCache(int maxEntries, long idleExpireMs) {
        this.maxEntries = maxEntries;
        this.idleExpireMs = idleExpireMs;
    }

    public V getOrCompute(long artifactSeed, java.util.function.Supplier<V> supplier) {
        long now = System.currentTimeMillis();
        cleanup(now);
        CacheValue<V> existing = values.get(artifactSeed);
        if (existing != null && now - existing.lastAccessMs < idleExpireMs) {
            existing.lastAccessMs = now;
            return existing.value;
        }
        if (values.size() >= maxEntries) {
            cleanup(now);
        }
        V value = supplier.get();
        values.put(artifactSeed, new CacheValue<>(value, now));
        return value;
    }

    private void cleanup(long now) {
        values.entrySet().removeIf(entry -> now - entry.getValue().lastAccessMs > idleExpireMs);
    }

    private static class CacheValue<V> {
        private final V value;
        private volatile long lastAccessMs;

        private CacheValue(V value, long lastAccessMs) {
            this.value = value;
            this.lastAccessMs = lastAccessMs;
        }
    }
}
