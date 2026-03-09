package obtuseloot.artifacts;

import java.util.concurrent.ThreadLocalRandom;

public class ArtifactSeedFactory {
    public void applySeed(Artifact artifact) {
        artifact.setSeedPrecisionAffinity(randomAffinity());
        artifact.setSeedBrutalityAffinity(randomAffinity());
        artifact.setSeedSurvivalAffinity(randomAffinity());
        artifact.setSeedMobilityAffinity(randomAffinity());
        artifact.setSeedChaosAffinity(randomAffinity());
        artifact.setSeedConsistencyAffinity(randomAffinity());
        artifact.setLatentLineage(rollLineage());
        artifact.setDriftAlignment(rollAlignment());
    }

    private double randomAffinity() {
        return ThreadLocalRandom.current().nextDouble(-1.5D, 1.5D);
    }

    private String rollLineage() {
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < 0.08) return "ashen";
        if (roll < 0.16) return "stormbound";
        if (roll < 0.23) return "graveborn";
        if (roll < 0.30) return "gilded";
        if (roll < 0.35) return "mirrored";
        return "common";
    }

    private String rollAlignment() {
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < 0.25) return "volatile";
        if (roll < 0.50) return "predatory";
        if (roll < 0.75) return "ascetic";
        return "paradox";
    }
}
