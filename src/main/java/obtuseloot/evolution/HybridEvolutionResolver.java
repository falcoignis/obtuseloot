package obtuseloot.evolution;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;

public class HybridEvolutionResolver {
    public String resolve(Artifact artifact, ArtifactReputation reputation) {
        String base = artifact.getArchetypePath();
        String lineage = artifact.getLatentLineage();
        String drift = artifact.getDriftAlignment();

        if (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath())) {
            return base + "-awakened-hybrid";
        }
        if ("stormbound".equalsIgnoreCase(lineage) || "tempest".equalsIgnoreCase(drift)) {
            return base + "-tempest-hybrid";
        }
        if ("graveborn".equalsIgnoreCase(lineage) || reputation.getChaos() > reputation.getConsistency()) {
            return base + "-void-hybrid";
        }
        return base + "-equilibrium-hybrid";
    }
}
