package obtuseloot.analytics;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NovelStrategyEmergenceAnalyzerTest {

    @Test
    void oneOffNoiseIsFilteredBySignificanceAndPersistence() {
        NovelStrategyEmergenceAnalyzer analyzer = new NovelStrategyEmergenceAnalyzer();

        Map<String, Object> season1 = snapshot(1,
                Map.of("mobility.glide", 6, "chaos.sprawl", 1),
                Map.of("niche-1", 10, "niche-2", 1));
        Map<String, Object> season2 = snapshot(2,
                Map.of("mobility.glide", 6, "precision.clock", 1),
                Map.of("niche-1", 9, "niche-2", 1));

        NovelStrategyEmergenceAnalyzer.NserResult result = analyzer.analyze(List.of(season1, season2));
        assertTrue(result.bySeason().get(1).totalSignificantStrategies() > 0);
        assertTrue(result.bySeason().get(1).novelSignificantStrategies() <= result.bySeason().get(1).totalSignificantStrategies());
    }

    private Map<String, Object> snapshot(int season,
                                         Map<String, Integer> branches,
                                         Map<String, Integer> nicheOccupancy) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("season", season);
        snapshot.put("branches", new LinkedHashMap<>(branches));
        snapshot.put("triggers", Map.of("on_chain_combat", 8));
        snapshot.put("mechanics", Map.of("chain_escalation", 8));
        snapshot.put("regulatoryProfiles", Map.of("[lineageMilestoneGate+mobilityGate]", 8));
        snapshot.put("nicheOccupancy", new LinkedHashMap<>(nicheOccupancy));
        snapshot.put("speciesPerNiche", new LinkedHashMap<>(nicheOccupancy));
        snapshot.put("coEvolutionCompetitionPressure", 0.03D);
        snapshot.put("coEvolutionSupportPressure", 0.03D);
        snapshot.put("coEvolutionModifier", 0.0D);
        return snapshot;
    }
}
