package com.falcoignis.obtuseloot.reputation;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReputationManager {
    private static final Map<UUID, ArtifactReputation> REPUTATION = new ConcurrentHashMap<>();

    private ReputationManager() {
    }

    public static ArtifactReputation get(UUID playerId) {
        return REPUTATION.computeIfAbsent(playerId, id -> new ArtifactReputation());
    }

    public static void remove(UUID playerId) {
        REPUTATION.remove(playerId);
    }
}
