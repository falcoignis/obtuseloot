package obtuseloot.memory;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.eligibility.ArtifactEligibility;

public class ArtifactMemoryEngine {
    private final MemoryInfluenceResolver influenceResolver = new MemoryInfluenceResolver();

    public ArtifactMemoryProfile recordAndProfile(Artifact artifact, ArtifactMemoryEvent event) {
        if (!ArtifactEligibility.isMemoryEligible(artifact)) {
            return new ArtifactMemoryProfile(0, 0.0D, 0.0D);
        }
        artifact.getMemory().record(event);
        artifact.addNotableEvent("memory." + event.name().toLowerCase());
        return influenceResolver.profileFor(artifact.getMemory());
    }

    public ArtifactMemoryProfile profile(Artifact artifact) {
        if (!ArtifactEligibility.isMemoryEligible(artifact)) {
            return new ArtifactMemoryProfile(0, 0.0D, 0.0D);
        }
        return influenceResolver.profileFor(artifact.getMemory());
    }
}
