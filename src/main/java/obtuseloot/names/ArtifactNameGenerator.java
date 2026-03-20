package obtuseloot.names;

import obtuseloot.artifacts.Artifact;

import java.util.UUID;

public final class ArtifactNameGenerator {
    private ArtifactNameGenerator() {
    }

    public static String generate(UUID ownerId, String itemCategory) {
        Artifact artifact = new Artifact(ownerId, itemCategory);
        artifact.setArtifactSeed(ownerId.getMostSignificantBits() ^ ownerId.getLeastSignificantBits());
        artifact.setNaming(ArtifactNameResolver.initialize(artifact));
        return artifact.getDisplayName();
    }

    public static String generateFromSeed(long artifactSeed, String itemCategory) {
        Artifact artifact = new Artifact(UUID.nameUUIDFromBytes(Long.toString(artifactSeed).getBytes()), itemCategory);
        artifact.setArtifactSeed(artifactSeed);
        artifact.setNaming(ArtifactNameResolver.initialize(artifact));
        return artifact.getDisplayName();
    }
}
