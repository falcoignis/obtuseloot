package obtuseloot.artifacts;

import obtuseloot.names.ArtifactNameResolver;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class ArtifactGenerator {
    private static ArtifactSeedFactory seedFactory = new ArtifactSeedFactory();

    private ArtifactGenerator() {
    }

    public static void setSeedFactory(ArtifactSeedFactory factory) {
        seedFactory = factory;
    }

    public static Artifact generateFor(UUID ownerId) {
        long artifactSeed = ThreadLocalRandom.current().nextLong();
        Artifact artifact = new Artifact(ownerId);
        artifact.setArtifactSeed(artifactSeed);
        artifact.resetMutableState();
        seedFactory.applySeedProfile(artifact, artifactSeed);
        artifact.setItemCategory(resolveCategory(artifactSeed));
        artifact.setNaming(ArtifactNameResolver.initialize(artifact));
        return artifact;
    }

    public static String resolveCategory(long artifactSeed) {
        Random random = new Random(artifactSeed ^ 0xC6BC279692B5C323L);
        return random.nextDouble() < 0.25D ? "generic" : "artifact";
    }
}
