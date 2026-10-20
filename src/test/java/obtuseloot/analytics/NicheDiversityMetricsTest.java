package obtuseloot.analytics;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class NicheDiversityMetricsTest {
    @Test
    void measuresLiveOccupancyWithoutCountingExtinctKeys() {
        var metrics = NicheDiversityMetrics.diversity(Map.of("a", 10L, "b", 10L, "dead", 0L));
        assertEquals(2, metrics.liveNiches());
        assertEquals(0.5D, metrics.simpsonDiversity(), 1.0E-12D);
        assertEquals(2.0D, metrics.effectiveNiches(), 1.0E-12D);
        assertEquals(metrics, NicheDiversityMetrics.diversity(Map.of("a", 1, "b", 1)));
    }

    @Test
    void handlesEmptyAndMonocultureWindows() {
        assertEquals(new NicheDiversityMetrics.Diversity(0, 0, 0), NicheDiversityMetrics.diversity(Map.of()));
        assertEquals(new NicheDiversityMetrics.Diversity(1, 0, 1), NicheDiversityMetrics.diversity(Map.of("a", 30)));
        assertThrows(IllegalArgumentException.class, () -> NicheDiversityMetrics.diversity(Map.of("a", -1)));
        assertThrows(IllegalArgumentException.class, () -> NicheDiversityMetrics.diversity(Map.of("a", Double.NaN)));
    }

    @Test
    void separatesNicheBirthAndExtinctionFromPopulationRedistribution() {
        var transition = NicheDiversityMetrics.transition(Map.of("a", 10, "b", 10), Map.of("b", 10, "c", 10, "a", 0));
        assertEquals(1, transition.births());
        assertEquals(1, transition.extinctions());
        assertEquals(2.0D / 3.0D, transition.turnover(), 1.0E-12D);
        assertEquals(0.5D, transition.populationShift(), 1.0E-12D);
        var stableMembership = NicheDiversityMetrics.transition(Map.of("a", 90, "b", 10), Map.of("a", 40, "b", 60));
        assertEquals(0, stableMembership.turnover());
        assertEquals(0.5D, stableMembership.populationShift(), 1.0E-12D);
        assertEquals(1, NicheDiversityMetrics.transition(Map.of("a", 10), Map.of()).populationShift());
        assertEquals(0, NicheDiversityMetrics.transition(Map.of(), Map.of()).populationShift());
    }

    @Test
    void healthUsesOnlyRelativeLossAndRejectsMissingEvidence() {
        assertTrue(RelativeDiversityHealth.assess(List.of(Map.of("a", 10, "b", 10), Map.of("x", 10, "y", 10)), 1, 0.25).healthy());
        assertFalse(RelativeDiversityHealth.assess(List.of(Map.of("a", 10, "b", 10), Map.of("a", 20)), 1, 0.25).healthy());
        assertFalse(RelativeDiversityHealth.assess(List.of(Map.of("a", 10, "b", 10), Map.of()), 1, 0.25).healthy());
        assertThrows(IllegalArgumentException.class, () -> RelativeDiversityHealth.assess(List.of(Map.of("a", 10), Map.of("a", 10)), 1, 0.25));
        assertThrows(IllegalArgumentException.class, () -> RelativeDiversityHealth.assess(List.of(Map.of("a", 1, "b", 1)), 1, 0.25));
    }
}
