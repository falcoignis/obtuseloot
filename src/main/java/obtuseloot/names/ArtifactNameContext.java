package obtuseloot.names;

import java.util.List;

public record ArtifactNameContext(
        long artifactSeed,
        String archetypePath,
        String evolutionPath,
        String awakeningPath,
        String fusionPath,
        String driftAlignment,
        String itemCategory,
        List<String> notableEvents,
        List<String> loreHistory
) {
    public static ArtifactNameContext minimal(long seed) {
        return new ArtifactNameContext(seed, "unformed", "base", "dormant", "none", "stable", "artifact", List.of(), List.of());
    }

    public boolean awakened() {
        return awakeningPath != null && !"dormant".equalsIgnoreCase(awakeningPath);
    }

    public boolean fused() {
        return fusionPath != null && !"none".equalsIgnoreCase(fusionPath);
    }

    public boolean storied() {
        return (notableEvents != null && notableEvents.size() >= 3) || (loreHistory != null && loreHistory.size() >= 4);
    }
}
