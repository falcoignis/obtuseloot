package obtuseloot.artifacts;

import obtuseloot.names.ArtifactNameGenerator;

import java.util.UUID;

public final class ArtifactGenerator {
    private static ArtifactSeedFactory seedFactory = new ArtifactSeedFactory();

    private ArtifactGenerator() {
    }

    public static void setSeedFactory(ArtifactSeedFactory factory) {
        seedFactory = factory;
    }

    public static Artifact generateFor(UUID ownerId) {
        Artifact artifact = new Artifact(ownerId, ArtifactNameGenerator.generate(ownerId));
        artifact.setArchetypePath("unformed");
        artifact.setEvolutionPath("base");
        artifact.setAwakeningPath("dormant");
        artifact.setFusionPath("none");
        seedFactory.applySeed(artifact);
        return artifact;
    }
}
