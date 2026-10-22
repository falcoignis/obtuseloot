package obtuseloot.abilities;

import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;
import java.util.Objects;

/** Catalog-owned expression. Effects consume the result; timing/feedback never branch on ability IDs. */
@FunctionalInterface
public interface AbilityPerformanceModel {
    AbilityPerformanceModel FIXED = genome -> AbilityPerformance.FIXED;
    AbilityPerformance express(ArtifactGenome genome);

    /** More output costs more recovery; 0.5 expresses the configured baseline. */
    static AbilityPerformanceModel tradeoff(GenomeTrait trait, double outputSpread, double recoverySpread) {
        Objects.requireNonNull(trait);
        if (!Double.isFinite(outputSpread) || outputSpread <= 0 || outputSpread >= 1
                || !Double.isFinite(recoverySpread) || recoverySpread <= 0 || recoverySpread >= 1)
            throw new IllegalArgumentException("Invalid benefit/recovery tradeoff");
        return genome -> {
            double gene = genome.trait(trait);
            if (!Double.isFinite(gene) || gene < 0 || gene > 1)
                throw new IllegalArgumentException("Invalid performance gene");
            double centered = 2 * gene - 1;
            return new AbilityPerformance(1 + centered * outputSpread, 1 + centered * recoverySpread);
        };
    }
}
