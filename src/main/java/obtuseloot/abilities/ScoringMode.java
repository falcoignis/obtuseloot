package obtuseloot.abilities;

public enum ScoringMode {
    BASELINE,
    PROJECTION_NO_CACHE,
    PROJECTION_WITH_CACHE;

    public static ScoringMode fromString(String raw, ScoringMode fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return ScoringMode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
