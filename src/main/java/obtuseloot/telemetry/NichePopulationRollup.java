package obtuseloot.telemetry;

import java.util.Map;

public record NichePopulationRollup(long generatedAtMs, Map<String, Long> populationByNiche) {
}
