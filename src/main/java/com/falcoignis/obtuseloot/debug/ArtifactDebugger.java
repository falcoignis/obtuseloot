package com.falcoignis.obtuseloot.debug;

import com.falcoignis.obtuseloot.artifacts.Artifact;
import com.falcoignis.obtuseloot.artifacts.ArtifactManager;
import com.falcoignis.obtuseloot.drift.DriftEngine;
import com.falcoignis.obtuseloot.reputation.ArtifactReputation;
import com.falcoignis.obtuseloot.reputation.ReputationManager;

import java.util.UUID;

public final class ArtifactDebugger {
    private ArtifactDebugger() {
    }

    public static String describe(UUID playerId) {
        Artifact artifact = ArtifactManager.getOrCreate(playerId);
        ArtifactReputation rep = ReputationManager.get(playerId);
        return "artifactId=" + artifact.getSeed()
                + ", evolution=" + artifact.getEvolutionPath()
                + ", driftNow=" + DriftEngine.shouldDrift(rep)
                + ", reputation={precision=" + rep.precision()
                + ", brutality=" + rep.brutality()
                + ", survival=" + rep.survival()
                + ", mobility=" + rep.mobility()
                + ", chaos=" + rep.chaos()
                + ", consistency=" + rep.consistency() + "}"
                + ", awakening=" + artifact.getAwakeningPath();
    }
}
