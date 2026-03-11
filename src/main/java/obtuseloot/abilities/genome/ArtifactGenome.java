package obtuseloot.abilities.genome;

import java.util.EnumMap;
import java.util.Map;

public final class ArtifactGenome {
    private final long seed;
    private final EnumMap<GenomeTrait, Double> traits;
    private final EnumMap<GenomeTrait, Double> latentTraits;
    private final EnumMap<GenomeTrait, Boolean> activatedLatentTraits;

    public ArtifactGenome(long seed, Map<GenomeTrait, Double> traits) {
        this(seed, traits, Map.of(), java.util.Set.of());
    }

    public ArtifactGenome(long seed,
                          Map<GenomeTrait, Double> traits,
                          Map<GenomeTrait, Double> latentTraits,
                          java.util.Set<GenomeTrait> activatedLatents) {
        this.seed = seed;
        this.traits = new EnumMap<>(GenomeTrait.class);
        this.latentTraits = new EnumMap<>(GenomeTrait.class);
        this.activatedLatentTraits = new EnumMap<>(GenomeTrait.class);
        for (GenomeTrait trait : GenomeTrait.values()) {
            this.traits.put(trait, clamp01(traits.getOrDefault(trait, 0.0D)));
            this.latentTraits.put(trait, clamp01(latentTraits.getOrDefault(trait, 0.0D)));
            this.activatedLatentTraits.put(trait, activatedLatents.contains(trait));
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

    public double latentTrait(GenomeTrait trait) {
        return latentTraits.getOrDefault(trait, 0.0D);
    }

    public boolean latentActivated(GenomeTrait trait) {
        return activatedLatentTraits.getOrDefault(trait, false);
    }

    public Map<GenomeTrait, Double> latentTraits() {
        return Map.copyOf(latentTraits);
    }

    public java.util.Set<GenomeTrait> activatedLatentTraits() {
        java.util.Set<GenomeTrait> out = new java.util.HashSet<>();
        for (Map.Entry<GenomeTrait, Boolean> entry : activatedLatentTraits.entrySet()) {
            if (entry.getValue()) {
                out.add(entry.getKey());
            }
        }
        return java.util.Set.copyOf(out);
    }

    public ArtifactGenome activateLatentTrait(GenomeTrait trait, double promotionStrength) {
        if (latentActivated(trait) || latentTrait(trait) <= 0.0D) {
            return this;
        }
        EnumMap<GenomeTrait, Double> expressed = new EnumMap<>(traits);
        EnumMap<GenomeTrait, Boolean> activated = new EnumMap<>(activatedLatentTraits);
        double promoted = clamp01(trait(trait) + (latentTrait(trait) * Math.max(0.05D, Math.min(0.60D, promotionStrength))));
        expressed.put(trait, promoted);
        activated.put(trait, true);
        return new ArtifactGenome(seed, expressed, latentTraits, activated.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(java.util.stream.Collectors.toSet()));
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
