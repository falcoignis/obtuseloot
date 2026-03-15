package obtuseloot.analytics.ecosystem;

public record CohortHalfLifeEstimate(
        int cohortWindow,
        int cohortSize,
        double halfLifeWindows,
        boolean censored
) {
}

