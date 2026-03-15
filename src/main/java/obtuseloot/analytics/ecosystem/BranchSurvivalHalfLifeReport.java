package obtuseloot.analytics.ecosystem;

import java.util.List;

public record BranchSurvivalHalfLifeReport(
        double branchSurvivalHalfLife,
        int cohortsMeasured,
        int censoredCohorts,
        List<CohortHalfLifeEstimate> cohortEstimates
) {
    public String estimateStatus() {
        if (cohortsMeasured <= 0) {
            return "none";
        }
        if (censoredCohorts <= 0) {
            return "complete";
        }
        if (censoredCohorts >= cohortsMeasured) {
            return "censored";
        }
        return "mixed";
    }
}
