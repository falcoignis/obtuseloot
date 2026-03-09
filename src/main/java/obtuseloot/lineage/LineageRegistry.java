package obtuseloot.lineage;

import obtuseloot.artifacts.Artifact;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class LineageRegistry {
    private final Map<String, ArtifactLineage> lineages = new LinkedHashMap<>();

    public ArtifactLineage assignLineage(Artifact artifact) {
        Random random = new Random(artifact.getArtifactSeed() ^ artifact.getOwnerId().getMostSignificantBits());
        String lineageId = artifact.getLatentLineage();
        if (lineageId == null || lineageId.isBlank() || "common".equalsIgnoreCase(lineageId)) {
            lineageId = random.nextDouble() < 0.65D
                    ? "lineage-" + UUID.nameUUIDFromBytes((artifact.getOwnerId().toString() + artifact.getArtifactSeed()).getBytes())
                    : "wild-" + Long.toUnsignedString(artifact.getArtifactSeed() & 65535L);
            artifact.setLatentLineage(lineageId);
        }
        ArtifactLineage lineage = lineages.computeIfAbsent(lineageId, ArtifactLineage::new);
        lineage.addAncestor(new ArtifactAncestor(artifact.getArtifactSeed(), lineage.depth() + 1));
        return lineage;
    }

    public Map<String, ArtifactLineage> lineages() {
        return lineages;
    }
}
