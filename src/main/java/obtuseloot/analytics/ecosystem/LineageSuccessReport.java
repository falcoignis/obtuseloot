package obtuseloot.analytics.ecosystem;

import java.util.List;
import java.util.Map;

public record LineageSuccessReport(
        Map<String, Double> successRateByLineage,
        Map<String, Double> branchSurvivalByLineage,
        Map<String, Double> specializationCascadeRiskByLineage,
        List<String> collapsingLineages,
        List<String> runawayLineages
) {
}
