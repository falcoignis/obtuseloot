package obtuseloot.analytics;

import java.util.Map;

public record LineageSpecializationTracker(
        Map<String, Double> specializationCurrent,
        Map<String, Double> specializationTrend,
        Map<String, Double> adaptationVsEcology
) {
}

