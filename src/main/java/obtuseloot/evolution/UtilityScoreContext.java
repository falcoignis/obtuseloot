package obtuseloot.evolution;

public record UtilityScoreContext(
        boolean meaningfulOutcome,
        boolean intentional,
        boolean noOp,
        boolean suppressed,
        boolean failed,
        double contextualRelevance,
        double budgetCost,
        double noveltyFactor,
        double redundancyPenalty,
        double spamPenalty
) {
}

