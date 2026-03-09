package obtuseloot.abilities.genome;

import java.util.EnumMap;
import java.util.Map;

public final class ArtifactGenome {
    private final long seed;
    private final EnumMap<GenomeTrait, Double> traits;

    public ArtifactGenome(long seed, Map<GenomeTrait, Double> traits) {
        this.seed = seed;
        this.traits = new EnumMap<>(GenomeTrait.class);
        for (GenomeTrait trait : GenomeTrait.values()) {
            this.traits.put(trait, clamp01(traits.getOrDefault(trait, 0.0D)));
        }
    }

    public long seed() {
        return seed;
    }

    public double trait(GenomeTrait trait) {
        return traits.getOrDefault(trait, 0.0D);
    }

    public Map<GenomeTrait, Double> traits() {
        return Map.copyOf(traits);
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
