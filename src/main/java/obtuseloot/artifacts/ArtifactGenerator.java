package obtuseloot.artifacts;

import obtuseloot.names.ArtifactNameGenerator;

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
        Artifact artifact = new Artifact(ownerId, "");
        artifact.setArtifactSeed(artifactSeed);
        artifact.resetMutableState();
        seedFactory.applySeedProfile(artifact, artifactSeed);
        artifact.setGeneratedName(ArtifactNameGenerator.generateFromSeed(artifactSeed));
        return artifact;
    }
}
