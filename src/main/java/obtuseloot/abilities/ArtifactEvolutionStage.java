package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;

public final class ArtifactEvolutionStage {
    private ArtifactEvolutionStage() {}

    public static int resolveStage(Artifact artifact) {
        if (!"none".equalsIgnoreCase(artifact.getFusionPath())) return 5;
        if (!"dormant".equalsIgnoreCase(artifact.getAwakeningPath())) return 4;
        String path = artifact.getEvolutionPath().toLowerCase();
        if (path.contains("hybrid") || path.contains("fused") || path.contains("advanced")) return 3;
        if (path.contains("tempered") || path.contains("formed")) return 2;
        return 1;
    }
}
