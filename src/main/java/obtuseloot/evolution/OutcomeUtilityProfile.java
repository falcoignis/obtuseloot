package obtuseloot.evolution;

import java.util.ArrayDeque;
import java.util.Deque;

public final class OutcomeUtilityProfile {
    private static final int NO_OP_WINDOW = 8;

    private final String mechanicKey;
    private long attempts;
    private long meaningfulOutcomes;
    private long noOps;
    private long suppressed;
    private long failed;
    private double validatedUtility;
    private double contextualUtility;
    private double budgetConsumed;
    private double redundancyPenalty;
    private double spamPenalty;
    private String lastOutcomeSignature;
    private int repeatedOutcomeStreak;
    private final Deque<Boolean> recentNoOps = new ArrayDeque<>();

    public OutcomeUtilityProfile(String mechanicKey) {
        this.mechanicKey = mechanicKey;
    }

    public MechanicUtilitySignal ingest(UtilityOutcomeRecord record, ValidatedOutcomeClassifier classifier) {
        attempts++;
        String signature = record.outcomeType().name() + "#" + record.status().name() + "#" + record.source();
        repeatedOutcomeStreak = signature.equals(lastOutcomeSignature) ? repeatedOutcomeStreak + 1 : 0;
        lastOutcomeSignature = signature;

        if (record.meaningfulOutcome()) {
            meaningfulOutcomes++;
        }
        if (record.status().name().equals("NO_OP")) {
            noOps++;
        }
        if (record.status().name().equals("SUPPRESSED")) {
            suppressed++;
        }
        if (record.status().name().equals("FAILED")) {
            failed++;
        }

        recentNoOps.addLast(record.status().name().equals("NO_OP"));
        if (recentNoOps.size() > NO_OP_WINDOW) {
            recentNoOps.removeFirst();
        }
        long recentNoOpCount = recentNoOps.stream().filter(Boolean::booleanValue).count();

        UtilityScoreContext context = classifier.classify(record, repeatedOutcomeStreak, recentNoOpCount);
        double utilityDelta = classifier.score(context, record.outcomeType());
        validatedUtility += utilityDelta;
        contextualUtility += context.contextualRelevance() * utilityDelta;
        budgetConsumed += context.budgetCost();
        redundancyPenalty += context.redundancyPenalty();
        spamPenalty += context.spamPenalty();

        return snapshot();
    }

    public MechanicUtilitySignal snapshot() {
        return new MechanicUtilitySignal(
                mechanicKey,
                validatedUtility,
                utilityDensity(),
                attempts == 0 ? 0.0D : contextualUtility / attempts,
                attempts == 0 ? 0.0D : (double) noOps / attempts,
                attempts == 0 ? 0.0D : spamPenalty / attempts,
                attempts == 0 ? 0.0D : redundancyPenalty / attempts,
                attempts,
                meaningfulOutcomes,
                budgetConsumed
        );
    }

    private double utilityDensity() {
        return validatedUtility / Math.max(1.0D, budgetConsumed);
    }
}

