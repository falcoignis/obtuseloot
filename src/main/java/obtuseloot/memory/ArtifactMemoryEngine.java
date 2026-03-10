package obtuseloot.memory;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.eligibility.ArtifactEligibility;
import obtuseloot.text.ArtifactTextChannel;
import obtuseloot.text.ArtifactTextResolver;

public class ArtifactMemoryEngine {
    private final MemoryInfluenceResolver influenceResolver = new MemoryInfluenceResolver();
    private final ArtifactTextResolver textResolver = new ArtifactTextResolver();

    public ArtifactMemoryProfile recordAndProfile(Artifact artifact, ArtifactMemoryEvent event) {
        if (!ArtifactEligibility.isMemoryEligible(artifact)) {
            return new ArtifactMemoryProfile(0, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        artifact.getMemory().record(event);
        artifact.addNotableEvent("memory." + event.name().toLowerCase());
        artifact.addLoreHistory(textResolver.compose(artifact, ArtifactTextChannel.MEMORY, event.name().toLowerCase()));
        return influenceResolver.profileFor(artifact.getMemory());
    }

    public ArtifactMemoryProfile profile(Artifact artifact) {
        if (!ArtifactEligibility.isMemoryEligible(artifact)) {
            return new ArtifactMemoryProfile(0, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        return influenceResolver.profileFor(artifact.getMemory());
    }
}
