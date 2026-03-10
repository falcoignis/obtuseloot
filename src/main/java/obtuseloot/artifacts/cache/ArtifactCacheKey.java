package obtuseloot.artifacts.cache;

import java.util.Objects;

public record ArtifactCacheKey(String storageKey) {
    public ArtifactCacheKey {
        storageKey = Objects.requireNonNullElse(storageKey, "").trim();
    }

    public boolean isValid() {
        return !storageKey.isBlank();
    }
}
