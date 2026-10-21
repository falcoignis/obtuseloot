package obtuseloot.artifacts;

import java.util.UUID;

/** Item identities never encode the current holder or an arbitrary filesystem path. */
public final class ArtifactStorageKeys {
    private ArtifactStorageKeys() { }
    public static String create() { return "artifact:" + UUID.randomUUID(); }
    public static boolean isItem(String key) { return key != null && key.startsWith("artifact:"); }
    public static UUID itemId(String key) {
        if (!isItem(key)) throw new IllegalArgumentException("Not a per-item artifact key");
        UUID id = UUID.fromString(key.substring("artifact:".length()));
        if (!key.equals("artifact:" + id)) throw new IllegalArgumentException("Noncanonical artifact key");
        return id;
    }
}
