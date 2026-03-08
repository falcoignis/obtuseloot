package obtuseloot.artifacts;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArtifactManager {
    private static final Map<UUID, Artifact> ARTIFACTS = new ConcurrentHashMap<>();

    private ArtifactManager() {
    }

    public static Artifact getOrCreate(UUID playerId) {
        return ARTIFACTS.computeIfAbsent(playerId, ArtifactGenerator::generateFor);
    }

    public static void remove(UUID playerId) {
        ARTIFACTS.remove(playerId);
    }
}
