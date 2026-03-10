package obtuseloot.lineage;

import obtuseloot.artifacts.Artifact;

import obtuseloot.text.ArtifactTextChannel;
import obtuseloot.text.ArtifactTextResolver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class LineageRegistry {
    private final Map<String, ArtifactLineage> lineages = new LinkedHashMap<>();
    private final ArtifactTextResolver textResolver = new ArtifactTextResolver();

    public ArtifactLineage assignLineage(Artifact artifact) {
        Random random = new Random(artifact.getArtifactSeed() ^ artifact.getOwnerId().getMostSignificantBits());
        String lineageId = artifact.getLatentLineage();
        if (lineageId == null || lineageId.isBlank() || "common".equalsIgnoreCase(lineageId)) {
            lineageId = random.nextDouble() < 0.65D
                    ? "lineage-" + UUID.nameUUIDFromBytes((artifact.getOwnerId().toString() + artifact.getArtifactSeed()).getBytes())
                    : "wild-" + Long.toUnsignedString(artifact.getArtifactSeed() & 65535L);
            artifact.setLatentLineage(lineageId);
            artifact.addLoreHistory(textResolver.compose(artifact, ArtifactTextChannel.LINEAGE, lineageId));
        }
        ArtifactLineage lineage = lineages.computeIfAbsent(lineageId, ArtifactLineage::new);
        if (!lineage.ancestorSeeds().contains(artifact.getArtifactSeed())) {
            lineage.addAncestor(new ArtifactAncestor(artifact.getArtifactSeed(), lineage.generationIndex() + 1));
        }
        return lineage;
    }

    public ArtifactLineage lineageFor(String lineageId) {
        return lineages.get(lineageId);
    }

    public Map<String, ArtifactLineage> lineages() {
        return lineages;
    }
}
