package obtuseloot.persistence;

import obtuseloot.artifacts.Artifact;

import java.util.UUID;

public interface ArtifactStore {
    void saveArtifact(UUID playerId, Artifact artifact);
    Artifact loadArtifact(UUID playerId);

    default void saveArtifactProfile(Artifact artifact) {
        throw new UnsupportedOperationException("Per-item artifact persistence is not implemented");
    }

    default Artifact loadArtifactProfile(String storageKey) {
        throw new UnsupportedOperationException("Per-item artifact persistence is not implemented");
    }
}
