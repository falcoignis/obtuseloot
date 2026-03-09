package obtuseloot.artifacts;

import java.util.Random;

public class ArtifactSeedFactory {
    public void applySeedProfile(Artifact artifact) {
        applySeedProfile(artifact, artifact.getArtifactSeed());
    }

    public void applySeedProfile(Artifact artifact, long seed) {
        artifact.setArtifactSeed(seed);
        Random random = createSeededRandom(seed);

        artifact.setSeedPrecisionAffinity(randomAffinity(random));
        artifact.setSeedBrutalityAffinity(randomAffinity(random));
        artifact.setSeedSurvivalAffinity(randomAffinity(random));
        artifact.setSeedMobilityAffinity(randomAffinity(random));
        artifact.setSeedChaosAffinity(randomAffinity(random));
        artifact.setSeedConsistencyAffinity(randomAffinity(random));
        artifact.setLatentLineage(rollLineage(random));
        artifact.setDriftAlignment(rollAlignment(random));
    }

    public void regenerateFromSeed(Artifact artifact, long seed) {
        applySeedProfile(artifact, seed);
    }

    private Random createSeededRandom(long seed) {
        return new Random(seed);
    }

    private double randomAffinity(Random random) {
        return -1.5D + (3.0D * random.nextDouble());
    }

    private String rollLineage(Random random) {
        double roll = random.nextDouble();
        if (roll < 0.08D) return "ashen";
        if (roll < 0.16D) return "stormbound";
        if (roll < 0.23D) return "graveborn";
        if (roll < 0.30D) return "gilded";
        if (roll < 0.35D) return "mirrored";
        return "common";
    }

    private String rollAlignment(Random random) {
        double roll = random.nextDouble();
        if (roll < 0.25D) return "volatile";
        if (roll < 0.50D) return "predatory";
        if (roll < 0.75D) return "ascetic";
        return "paradox";
    }
}
