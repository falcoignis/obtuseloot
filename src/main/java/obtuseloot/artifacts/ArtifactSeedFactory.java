package obtuseloot.artifacts;

import obtuseloot.abilities.genome.GenomeTrait;

import java.util.Random;
import java.util.SplittableRandom;

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

    /** Birth-only variation on the founder affinity scale; never consumes the parent-selection RNG. */
    public void inheritAffinities(Artifact child, Artifact parent) {
        var genome = parent.getGenome();
        // The existing stage-two genome mutation amplitude, scaled from [0,1] to [-1.5,1.5].
        double amplitude = 0.02D + 0.08D * genome.trait(GenomeTrait.MUTATION_SENSITIVITY)
                + 0.06D * genome.trait(GenomeTrait.VOLATILITY);
        var random = new SplittableRandom(child.getArtifactSeed() ^ 0x414646494E495459L);
        child.setSeedPrecisionAffinity(varyAffinity(parent.getSeedPrecisionAffinity(), amplitude, random));
        child.setSeedBrutalityAffinity(varyAffinity(parent.getSeedBrutalityAffinity(), amplitude, random));
        child.setSeedSurvivalAffinity(varyAffinity(parent.getSeedSurvivalAffinity(), amplitude, random));
        child.setSeedMobilityAffinity(varyAffinity(parent.getSeedMobilityAffinity(), amplitude, random));
        child.setSeedChaosAffinity(varyAffinity(parent.getSeedChaosAffinity(), amplitude, random));
        child.setSeedConsistencyAffinity(varyAffinity(parent.getSeedConsistencyAffinity(), amplitude, random));
    }

    private double varyAffinity(double value, double amplitude, SplittableRandom random) {
        return value + (random.nextDouble() * 2.0D - 1.0D) * amplitude;
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
