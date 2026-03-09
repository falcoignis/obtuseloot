package obtuseloot.debug;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.Artifact;
import obtuseloot.drift.DriftEngine;
import obtuseloot.reputation.ArtifactReputation;

import java.util.UUID;

public final class ArtifactDebugger {
    private ArtifactDebugger() {
    }

    public static String describe(UUID playerId) {
        Artifact artifact = ObtuseLoot.get().getArtifactManager().getOrCreate(playerId);
        ArtifactReputation rep = ObtuseLoot.get().getReputationManager().get(playerId);
        return "artifactName=\"" + artifact.getName() + "\""
                + ", artifactSeed=" + artifact.getArtifactSeed()
                + ", archetype=" + artifact.getArchetypePath()
                + ", evolution=" + artifact.getEvolutionPath()
                + ", fusion=" + artifact.getFusionPath()
                + ", driftNow=" + new DriftEngine().shouldDrift(rep)
                + ", reputation={precision=" + rep.precision()
                + ", brutality=" + rep.brutality()
                + ", survival=" + rep.survival()
                + ", mobility=" + rep.mobility()
                + ", chaos=" + rep.chaos()
                + ", consistency=" + rep.consistency() + "}"
                + ", awakening=" + artifact.getAwakeningPath();
    }
}
