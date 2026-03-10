package obtuseloot.analytics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EcosystemHealthGaugeAnalyzerTest {
    private final EcosystemHealthGaugeAnalyzer analyzer = new EcosystemHealthGaugeAnalyzer();

    @Test
    void computesEndFromBalancedDistribution() {
        double end = analyzer.effectiveNicheDiversity(Map.of("a", 10, "b", 10, "c", 10));
        assertEquals(3.0D, end, 0.0001D);
    }

    @Test
    void computesTntAcrossSeasons() {
        double tnt = analyzer.temporalNicheTurnover(
                Map.of("n1", 90, "n2", 10),
                Map.of("n1", 40, "n2", 60));
        assertEquals(0.5D, tnt, 0.0001D);
    }

    @Test
    void classifiesHealthyHighEndLowTnt() {
        EcosystemStatus status = analyzer.classify(4.2D, 0.18D);
        assertEquals(EcosystemStatus.HEALTHY_ECOSYSTEM, status);
    }

    @Test
    void buildsTrendSeries() {
        var result = analyzer.analyze(
                List.of(
                        Map.of("n1", 20, "n2", 20),
                        Map.of("n1", 35, "n2", 5),
                        Map.of("n1", 20, "n2", 20)
                ),
                List.of(Map.of("n1", 3, "n2", 2))
        );
        assertEquals(3, result.endTrend().size());
        assertEquals(2, result.tntTrend().size());
        assertNotNull(result.endSpecies());
    }
}
