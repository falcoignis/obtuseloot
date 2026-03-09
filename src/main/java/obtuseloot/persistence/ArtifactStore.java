package obtuseloot.persistence;

import obtuseloot.artifacts.Artifact;

import java.util.UUID;

public interface ArtifactStore {
    void saveArtifact(UUID playerId, Artifact artifact);
    Artifact loadArtifact(UUID playerId);
}
