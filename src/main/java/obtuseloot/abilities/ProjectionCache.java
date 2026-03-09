package obtuseloot.abilities;

import obtuseloot.abilities.genome.GenomeTrait;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class ProjectionCache {
    private static final int QUANTIZATION_STEPS = 128;

    private final Map<Long, Map<String, Double>> scoresByGenomeBucket;
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    public ProjectionCache(int capacity) {
        this.scoresByGenomeBucket = Collections.synchronizedMap(new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, Map<String, Double>> eldest) {
                return size() > capacity;
            }
        });
    }

    public long bucketKey(GenomeProjection projection) {
        long hash = 1469598103934665603L;
        for (GenomeTrait trait : GenomeTrait.values()) {
            int quantized = (int) Math.round(projection.component(trait.ordinal()) * QUANTIZATION_STEPS);
            hash ^= (long) quantized + (trait.ordinal() * 37L);
            hash *= 1099511628211L;
        }
        return hash;
    }

    public Map<String, Double> get(long bucketKey) {
        Map<String, Double> cached = scoresByGenomeBucket.get(bucketKey);
        if (cached == null) {
            misses.incrementAndGet();
            return Map.of();
        }
        hits.incrementAndGet();
        return cached;
    }

    public void put(long bucketKey, Map<String, Double> scores) {
        scoresByGenomeBucket.put(bucketKey, Map.copyOf(scores));
    }

    public long hits() {
        return hits.get();
    }

    public long misses() {
        return misses.get();
    }
}
