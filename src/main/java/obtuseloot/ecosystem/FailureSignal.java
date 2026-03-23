package obtuseloot.ecosystem;

/**
 * Structured signal produced by {@link FailureSignalDetector} when a failure condition is detected.
 * Signals are informational only — they do not trigger automatic corrections.
 */
public record FailureSignal(Type type, String description) {

    /** Enumeration of detectable failure condition types. */
    public enum Type {
        /** One category dominates to the point of effective monoculture (≥ 90% share). */
        CATEGORY_COLLAPSE,
        /** One template dominates severely within its distribution (≥ 70% share). */
        TEMPLATE_DOMINANCE,
        /** Multiple categories in the long tail are near-zero (< 2% each, ≥ 50% of categories). */
        LONG_TAIL_DEATH,
        /** Lineage distribution is heavily locked into a single lineage (reserved for future use). */
        LINEAGE_LOCK_IN,
        /** Candidate pool has collapsed below the configured minimum threshold. */
        POOL_COLLAPSE
    }

    @Override
    public String toString() {
        return type.name() + ": " + description;
    }
}
