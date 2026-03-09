package obtuseloot.abilities;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class ProjectionCache {
    private final int capacity;
    private final Map<ProjectionCacheKey, GenomeProjection> projectionByKey;
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    public ProjectionCache(int capacity) {
        this.capacity = Math.max(1_000, capacity);
        this.projectionByKey = Collections.synchronizedMap(new LinkedHashMap<>(this.capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ProjectionCacheKey, GenomeProjection> eldest) {
                return size() > ProjectionCache.this.capacity;
            }
        });
    }

    public Optional<GenomeProjection> get(ProjectionCacheKey key) {
        GenomeProjection projection = projectionByKey.get(key);
        if (projection == null) {
            misses.incrementAndGet();
            return Optional.empty();
        }
        hits.incrementAndGet();
        return Optional.of(projection);
    }

    public void put(GenomeProjection projection) {
        projectionByKey.put(projection.key(), projection);
    }

    public long hits() {
        return hits.get();
    }

    public long misses() {
        return misses.get();
    }

    public int size() {
        return projectionByKey.size();
    }

    public int capacity() {
        return capacity;
    }
}
