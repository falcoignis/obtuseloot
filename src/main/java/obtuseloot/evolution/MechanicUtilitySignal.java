package obtuseloot.evolution;

public record MechanicUtilitySignal(
        String mechanicKey,
        double validatedUtility,
        double utilityDensity,
        double contextualRelevance,
        double noOpRate,
        double spamPenalty,
        double redundancyPenalty,
        long attempts,
        long meaningfulOutcomes,
        double budgetConsumed
) {
}

