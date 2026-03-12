package obtuseloot.evolution;

import obtuseloot.abilities.AbilityExecutionStatus;
import obtuseloot.abilities.AbilityOutcomeType;

public final class ValidatedOutcomeClassifier {

    public UtilityScoreContext classify(UtilityOutcomeRecord outcome,
                                        int repeatedOutcomeStreak,
                                        long recentNoOpCount) {
        boolean noOp = outcome.status() == AbilityExecutionStatus.NO_OP;
        boolean suppressed = outcome.status() == AbilityExecutionStatus.SUPPRESSED;
        boolean failed = outcome.status() == AbilityExecutionStatus.FAILED;
        double noveltyFactor = 1.0D / (1.0D + (Math.max(0, repeatedOutcomeStreak) * 0.55D));
        double redundancyPenalty = Math.max(0.0D, 1.0D - noveltyFactor);
        double spamPenalty = noOp ? Math.min(1.0D, recentNoOpCount / 6.0D) : Math.min(0.6D, repeatedOutcomeStreak / 8.0D);

        return new UtilityScoreContext(
                outcome.meaningfulOutcome(),
                outcome.intentional(),
                noOp,
                suppressed,
                failed,
                contextualRelevance(outcome),
                Math.max(0.1D, outcome.budgetCost()),
                noveltyFactor,
                redundancyPenalty,
                spamPenalty
        );
    }

    public double score(UtilityScoreContext context, AbilityOutcomeType outcomeType) {
        if (context.suppressed() || context.failed()) {
            return -0.45D - (context.spamPenalty() * 0.25D);
        }
        if (context.noOp()) {
            return -0.25D - (context.spamPenalty() * 0.55D);
        }

        double outcomeWeight = switch (outcomeType) {
            case WORLD_INTERACTION, NAVIGATION_HINT, MEMORY_MARK -> 1.2D;
            case INFORMATION, STRUCTURE_SENSE, CROP_REPLANT, SOCIAL_SIGNAL -> 1.0D;
            default -> 0.7D;
        };
        double meaningful = context.meaningfulOutcome() ? 1.0D : 0.45D;
        double intentionalBoost = context.intentional() ? 1.2D : 0.9D;
        return (meaningful * outcomeWeight * intentionalBoost * context.contextualRelevance() * context.noveltyFactor())
                - (context.redundancyPenalty() * 0.25D)
                - (context.spamPenalty() * 0.20D);
    }

    private double contextualRelevance(UtilityOutcomeRecord outcome) {
        double relevance = Math.max(0.2D, Math.min(1.35D, outcome.contextualRelevance()));
        if (outcome.intentional()) {
            relevance += 0.15D;
        }
        if (outcome.meaningfulOutcome()) {
            relevance += 0.1D;
        }
        return Math.min(1.6D, relevance);
    }
}

