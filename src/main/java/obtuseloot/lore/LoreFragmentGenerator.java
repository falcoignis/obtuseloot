package obtuseloot.lore;

import obtuseloot.artifacts.Artifact;
import obtuseloot.text.ArtifactTextChannel;
import obtuseloot.text.ArtifactTextResolver;

public class LoreFragmentGenerator {
    private final ArtifactTextResolver textResolver = new ArtifactTextResolver();

    public String driftFragment(Artifact artifact) {
        return textResolver.compose(artifact, ArtifactTextChannel.DRIFT, artifact.getDriftAlignment());
    }

    public String awakeningFragment(Artifact artifact) {
        return textResolver.compose(artifact, ArtifactTextChannel.AWAKENING, artifact.getAwakeningPath());
    }

    public String lineageFragment(Artifact artifact) {
        return textResolver.compose(artifact, ArtifactTextChannel.LINEAGE, artifact.getLatentLineage());
    }

    public String identifyFragment(Artifact artifact) {
        return textResolver.compose(artifact, ArtifactTextChannel.IDENTIFY, artifact.getArchetypePath());
    }

    public String memoryFragment(Artifact artifact) {
        String latest = artifact.getNotableEvents().isEmpty() ? "" : artifact.getNotableEvents().getLast();
        return textResolver.compose(artifact, ArtifactTextChannel.MEMORY, latest);
    }

    public String fusionFragment(Artifact artifact) {
        return textResolver.compose(artifact, ArtifactTextChannel.FUSION, artifact.getFusionPath());
    }

    public String instabilityFragment(Artifact artifact) {
        if (!artifact.hasInstability()) {
            return "Stable.";
        }
        return textResolver.compose(artifact, ArtifactTextChannel.DRIFT, artifact.getCurrentInstabilityState());
    }
}
