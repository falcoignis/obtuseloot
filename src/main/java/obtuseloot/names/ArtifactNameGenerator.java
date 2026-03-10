package obtuseloot.names;

import obtuseloot.artifacts.Artifact;

import java.util.UUID;

public final class ArtifactNameGenerator {
    private ArtifactNameGenerator() {
    }

    public static String generate(UUID ownerId) {
        Artifact artifact = new Artifact(ownerId);
        artifact.setArtifactSeed(ownerId.getMostSignificantBits() ^ ownerId.getLeastSignificantBits());
        artifact.setNaming(ArtifactNameResolver.initialize(artifact));
        return artifact.getDisplayName();
    }

    public static String generateFromSeed(long artifactSeed) {
        Artifact artifact = new Artifact(UUID.nameUUIDFromBytes(Long.toString(artifactSeed).getBytes()));
        artifact.setArtifactSeed(artifactSeed);
        artifact.setNaming(ArtifactNameResolver.initialize(artifact));
        return artifact.getDisplayName();
    }
}
