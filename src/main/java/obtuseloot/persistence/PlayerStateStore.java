package obtuseloot.persistence;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;

import java.util.Map;
import java.util.UUID;

public interface PlayerStateStore extends ArtifactStore, ReputationStore {
    default void saveAll(Map<UUID, Artifact> artifacts, Map<UUID, ArtifactReputation> reputations) {
        artifacts.forEach(this::saveArtifact);
        reputations.forEach(this::saveReputation);
    }

    default void flushPendingWrites() {
    }
}
