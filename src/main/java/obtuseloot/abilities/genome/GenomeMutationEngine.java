package obtuseloot.abilities.genome;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

public class GenomeMutationEngine {
    public ArtifactGenome mutate(ArtifactGenome baseGenome, int evolutionStage) {
        if (evolutionStage <= 1) {
            return baseGenome;
        }

        EnumMap<GenomeTrait, Double> mutated = new EnumMap<>(GenomeTrait.class);
        EnumMap<GenomeTrait, Double> latentMutated = new EnumMap<>(GenomeTrait.class);
        Random random = new Random(baseGenome.seed() ^ ((long) evolutionStage * 0x9E3779B97F4A7C15L));
        double sensitivity = baseGenome.trait(GenomeTrait.MUTATION_SENSITIVITY);
        double volatility = baseGenome.trait(GenomeTrait.VOLATILITY);
        double amplitude = (0.02D + (0.08D * sensitivity) + (0.06D * volatility)) * Math.min(1.0D, evolutionStage / 6.0D);

        for (Map.Entry<GenomeTrait, Double> entry : baseGenome.traits().entrySet()) {
            double delta = (random.nextDouble() - 0.5D) * 2.0D * amplitude;
            mutated.put(entry.getKey(), clamp01(entry.getValue() + delta));

            double latentDelta = (random.nextDouble() - 0.5D) * 2.0D * amplitude * 0.35D;
            latentMutated.put(entry.getKey(), clamp01(baseGenome.latentTrait(entry.getKey()) + latentDelta));
        }
        return new ArtifactGenome(baseGenome.seed(), mutated, latentMutated, baseGenome.activatedLatentTraits());
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
