package obtuseloot.evolution;

/**
 * Records a single niche bifurcation event: a parent niche splitting into two
 * child niches under sustained ecological pressure.
 */
public record NicheBifurcation(
        String parentNiche,
        String childNicheA,
        String childNicheB,
        double saturationPressureAtCreation,
        double specializationPressureAtCreation,
        long timestampMs
) {
}
