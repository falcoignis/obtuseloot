package obtuseloot.persistence;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;

import java.util.Map;
import java.util.UUID;

public interface PlayerStateStore {
    void saveArtifact(UUID playerId, Artifact artifact);
    Artifact loadArtifact(UUID playerId);
    void saveReputation(UUID playerId, ArtifactReputation reputation);
    ArtifactReputation loadReputation(UUID playerId);
    void saveAll(Map<UUID, Artifact> artifacts, Map<UUID, ArtifactReputation> reputations);
}
