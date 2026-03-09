package obtuseloot.artifacts.eligibility;

import obtuseloot.artifacts.Artifact;

public final class ArtifactEligibility {
    private ArtifactEligibility() {}

    public static boolean isGenericItem(Artifact artifact) {
        return "generic".equalsIgnoreCase(artifact.getItemCategory());
    }

    public static boolean isEvolutionEligible(Artifact artifact) {
        return !isGenericItem(artifact);
    }

    public static boolean isAbilityEligible(Artifact artifact) {
        return !isGenericItem(artifact);
    }
}
