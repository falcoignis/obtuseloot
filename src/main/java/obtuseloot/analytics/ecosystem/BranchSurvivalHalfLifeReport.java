package obtuseloot.analytics.ecosystem;

import java.util.List;

public record BranchSurvivalHalfLifeReport(
        double branchSurvivalHalfLife,
        int cohortsMeasured,
        int censoredCohorts,
        List<CohortHalfLifeEstimate> cohortEstimates
) {
}

