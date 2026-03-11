package obtuseloot.abilities.genome;

import java.util.EnumMap;

public class GenomeResolver {
    public ArtifactGenome resolve(long seed) {
        EnumMap<GenomeTrait, Double> traits = new EnumMap<>(GenomeTrait.class);
        EnumMap<GenomeTrait, Double> latent = new EnumMap<>(GenomeTrait.class);
        for (GenomeTrait trait : GenomeTrait.values()) {
            long mixed = mix64(seed ^ ((long) trait.ordinal() * 0x9E3779B97F4A7C15L));
            double normalized = (double) ((mixed >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
            traits.put(trait, normalized);

            long latentMixed = mix64((seed ^ 0xA5A5A5A5A5A5A5A5L) ^ ((long) trait.ordinal() * 0xD1B54A32D192ED03L));
            double latentNormalized = (double) ((latentMixed >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53);
            latent.put(trait, latentNormalized * 0.80D);
        }
        return new ArtifactGenome(seed, traits, latent, java.util.Set.of());
    }

    private long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
