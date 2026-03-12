package obtuseloot.telemetry;

import java.util.Map;

public record LineagePopulationRollup(long generatedAtMs, Map<String, Long> populationByLineage) {
}
