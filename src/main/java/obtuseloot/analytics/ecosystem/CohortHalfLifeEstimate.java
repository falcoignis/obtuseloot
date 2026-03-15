package obtuseloot.analytics.ecosystem;

import java.util.List;

public record CohortHalfLifeEstimate(
        int cohortWindow,
        int cohortSize,
        double halfLifeWindows,
        boolean censored,
        List<Integer> activeByWindow,
        List<Integer> inactiveOrDeadByWindow
) {
}
