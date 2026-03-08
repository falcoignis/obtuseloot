package com.obtuseloot.debug;

import com.obtuseloot.artifacts.Artifact;
import com.obtuseloot.artifacts.ArtifactManager;
import com.obtuseloot.drift.DriftEngine;
import com.obtuseloot.reputation.ArtifactReputation;
import com.obtuseloot.reputation.ReputationManager;

import java.util.UUID;

public final class ArtifactDebugger {
    private ArtifactDebugger() {
    }

    public static String describe(UUID playerId) {
        Artifact artifact = ArtifactManager.getOrCreate(playerId);
        ArtifactReputation rep = ReputationManager.get(playerId);
        return "artifactName=\"" + artifact.getName() + "\""
                + ", artifactId=" + artifact.getSeed()
                + ", archetype=" + artifact.getArchetypePath()
                + ", evolution=" + artifact.getEvolutionPath()
                + ", fusion=" + artifact.getFusionPath()
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
