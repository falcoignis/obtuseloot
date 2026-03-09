package obtuseloot.abilities.genome;

import java.util.EnumMap;

public class GenomeResolver {
    public ArtifactGenome resolve(long seed) {
        EnumMap<GenomeTrait, Double> traits = new EnumMap<>(GenomeTrait.class);
        for (GenomeTrait trait : GenomeTrait.values()) {
            long mixed = mix64(seed ^ ((long) trait.ordinal() * 0x9E3779B97F4A7C15L));
            double normalized = (double) ((mixed >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
            traits.put(trait, normalized);
        }
        return new ArtifactGenome(seed, traits);
    }

    private long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
