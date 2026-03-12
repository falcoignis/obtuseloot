package obtuseloot.analytics.ecosystem;

import java.util.Map;

public record TuningProfileRecommendation(
        String profileName,
        Map<String, Double> parameterAdjustments,
        String rationale
) {
}
