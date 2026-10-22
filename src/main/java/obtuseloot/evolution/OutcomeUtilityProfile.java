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

    public static OutcomeUtilityProfile fromSnapshot(MechanicUtilitySignal signal) {
        OutcomeUtilityProfile profile = new OutcomeUtilityProfile(signal.mechanicKey());
        profile.attempts = signal.attempts();
        profile.meaningfulOutcomes = signal.meaningfulOutcomes();
        profile.noOps = Math.round(signal.noOpRate() * signal.attempts());
        profile.validatedUtility = signal.validatedUtility();
        profile.contextualUtility = signal.contextualRelevance() * signal.attempts();
        profile.budgetConsumed = signal.budgetConsumed();
        profile.redundancyPenalty = signal.redundancyPenalty() * signal.attempts();
        profile.spamPenalty = signal.spamPenalty() * signal.attempts();
        return profile;
    }

    public MechanicUtilitySignal ingest(UtilityOutcomeRecord record, ValidatedOutcomeClassifier classifier) {
        attempts++;
        String identity = record.observationKey() == null ? record.source() : record.observationKey();
        String signature = record.outcomeType().name() + "#" + record.status().name() + "#" + identity;
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
        if (record.performance() != null) utilityDelta = record.performance().adjustUtility(utilityDelta);
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

    /** Includes repeat/no-op windows so restart cannot reset the next outcome's penalties. */
    public java.util.Map<String, Object> state() {
        var state = new java.util.LinkedHashMap<String, Object>();
        state.put("attempts", attempts); state.put("meaningful", meaningfulOutcomes);
        state.put("no-ops", noOps); state.put("suppressed", suppressed); state.put("failed", failed);
        state.put("utility", validatedUtility); state.put("contextual", contextualUtility);
        state.put("budget", budgetConsumed); state.put("redundancy", redundancyPenalty); state.put("spam", spamPenalty);
        state.put("last-signature", lastOutcomeSignature); state.put("repeat-streak", repeatedOutcomeStreak);
        state.put("recent-no-ops", java.util.List.copyOf(recentNoOps));
        return state;
    }

    public static OutcomeUtilityProfile fromState(String key, java.util.Map<String, Object> state) {
        var profile = new OutcomeUtilityProfile(key);
        profile.attempts = counter(state, "attempts"); profile.meaningfulOutcomes = counter(state, "meaningful");
        profile.noOps = counter(state, "no-ops"); profile.suppressed = counter(state, "suppressed");
        profile.failed = counter(state, "failed");
        profile.validatedUtility = number(state, "utility"); profile.contextualUtility = number(state, "contextual");
        profile.budgetConsumed = number(state, "budget"); profile.redundancyPenalty = number(state, "redundancy");
        profile.spamPenalty = number(state, "spam");
        long streak = counter(state, "repeat-streak");
        if (!(state.get("last-signature") instanceof String signature) || signature.isBlank()
                || profile.attempts == 0 || streak >= profile.attempts || streak > Integer.MAX_VALUE
                || profile.meaningfulOutcomes > profile.attempts || profile.noOps > profile.attempts
                || profile.suppressed > profile.attempts || profile.failed > profile.attempts
                || profile.budgetConsumed < 0 || profile.redundancyPenalty < 0 || profile.spamPenalty < 0
                || !(state.get("recent-no-ops") instanceof java.util.List<?> recent)
                || recent.size() != Math.min(NO_OP_WINDOW, profile.attempts)) {
            throw new IllegalArgumentException("Invalid fitness profile state");
        }
        profile.lastOutcomeSignature = signature; profile.repeatedOutcomeStreak = (int) streak;
        for (Object entry : recent) {
            if (!(entry instanceof Boolean value)) throw new IllegalArgumentException("Invalid no-op window");
            profile.recentNoOps.addLast(value);
        }
        if (profile.recentNoOps.stream().filter(Boolean::booleanValue).count() > profile.noOps)
            throw new IllegalArgumentException("Inconsistent no-op history");
        return profile;
    }

    private static long counter(java.util.Map<String, Object> state, String key) {
        Object value = state.get(key);
        if (!(value instanceof Long || value instanceof Integer) || ((Number) value).longValue() < 0)
            throw new IllegalArgumentException("Invalid fitness counter " + key);
        return ((Number) value).longValue();
    }

    private static double number(java.util.Map<String, Object> state, String key) {
        if (!(state.get(key) instanceof Number value) || !Double.isFinite(value.doubleValue()))
            throw new IllegalArgumentException("Invalid fitness value " + key);
        return value.doubleValue();
    }

    private double utilityDensity() {
        return validatedUtility / Math.max(1.0D, budgetConsumed);
    }
}
