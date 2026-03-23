package obtuseloot.ecosystem;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ring buffer that tracks the rolling average share of each key across the last N distribution snapshots.
 *
 * Each call to {@link #record(Map)} supplies one snapshot of fractional shares (values should sum to ~1.0).
 * Average share per key is maintained as an incremental running sum, making per-record cost O(K) where
 * K = distinct keys and per-query cost O(1) — suitable for per-tick use on the server main thread.
 *
 * Not thread-safe; callers must synchronize externally if accessed from multiple threads.
 */
public final class RollingDistributionWindow {

    private final int capacity;
    @SuppressWarnings("unchecked")
    private final Map<String, Double>[] buffer;
    private int head = 0;
    private int count = 0;
    private final Map<String, Double> runningSum = new HashMap<>();

    @SuppressWarnings("unchecked")
    public RollingDistributionWindow(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be >= 1");
        this.capacity = capacity;
        this.buffer = new Map[capacity];
    }

    /**
     * Record a new distribution snapshot. Evicts the oldest snapshot when the buffer is full.
     *
     * @param distribution map of key → fractional share (need not sum to exactly 1.0)
     */
    public void record(Map<String, Double> distribution) {
        if (count == capacity) {
            Map<String, Double> evicted = buffer[head];
            if (evicted != null) {
                evicted.forEach((k, v) -> runningSum.merge(k, -v, Double::sum));
            }
        } else {
            count++;
        }
        Map<String, Double> copy = new HashMap<>(distribution);
        buffer[head] = copy;
        copy.forEach((k, v) -> runningSum.merge(k, v, Double::sum));
        head = (head + 1) % capacity;
    }

    /**
     * Returns the average share of {@code key} across all snapshots currently in the window.
     * Returns 0.0 if the window is empty or the key has never been observed.
     */
    public double averageShare(String key) {
        if (count == 0) return 0.0;
        return Math.max(0.0, runningSum.getOrDefault(key, 0.0) / count);
    }

    /**
     * Returns a snapshot of average shares for all currently tracked keys.
     * Keys with zero or negative running sum are omitted.
     */
    public Map<String, Double> averageShares() {
        if (count == 0) return Collections.emptyMap();
        Map<String, Double> result = new LinkedHashMap<>();
        runningSum.forEach((k, v) -> {
            double avg = v / count;
            if (avg > 0.0) result.put(k, avg);
        });
        return result;
    }

    /** Number of snapshots currently held in the window (≤ capacity). */
    public int windowFill() {
        return count;
    }

    /** Configured maximum window size. */
    public int capacity() {
        return capacity;
    }

    /** Reset all observations, clearing the ring buffer and running sums. */
    public void reset() {
        for (int i = 0; i < capacity; i++) {
            buffer[i] = null;
        }
        head = 0;
        count = 0;
        runningSum.clear();
    }
}
