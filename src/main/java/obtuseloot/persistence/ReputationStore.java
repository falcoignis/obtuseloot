package obtuseloot.persistence;

import obtuseloot.reputation.ArtifactReputation;

import java.util.UUID;

public interface ReputationStore {
    void saveReputation(UUID playerId, ArtifactReputation reputation);
    ArtifactReputation loadReputation(UUID playerId);
}
