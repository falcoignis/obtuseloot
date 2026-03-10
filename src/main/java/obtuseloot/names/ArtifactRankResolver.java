package obtuseloot.names;

import obtuseloot.artifacts.Artifact;

public final class ArtifactRankResolver {
    private ArtifactRankResolver() {
    }

    public static ArtifactRank resolve(Artifact artifact) {
        if (!"none".equalsIgnoreCase(artifact.getFusionPath())) {
            return ArtifactRank.FUSED;
        }
        if (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath())) {
            return ArtifactRank.AWAKENED;
        }
        String evolution = artifact.getEvolutionPath() == null ? "" : artifact.getEvolutionPath().toLowerCase();
        if (evolution.contains("myth") || evolution.contains("hybrid") || artifact.getTotalDrifts() >= 10) {
            return ArtifactRank.MYTHIC;
        }
        if (evolution.contains("temper") || artifact.getTotalDrifts() >= 3) {
            return ArtifactRank.TEMPERED;
        }
        return ArtifactRank.BASE;
    }
}
