package obtuseloot.evolution;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ArtifactUsageProfile {
    private long firstSeenAt;
    private long lastSeenAt;
    private long uses;
    private long kills;
    private long discards;
    private long fusions;
    private long awakenings;
    private final Map<String, OutcomeUtilityProfile> utilityProfiles = new ConcurrentHashMap<>();
    private final ValidatedOutcomeClassifier classifier = new ValidatedOutcomeClassifier();

    public void markCreated(long now) {
        if (firstSeenAt == 0L) {
            firstSeenAt = now;
        }
        lastSeenAt = now;
    }

    public void recordUse(long now) {
        markCreated(now);
        uses++;
    }

    public void recordKill(long now) {
        markCreated(now);
        kills++;
    }

    public void recordDiscard(long now) {
        markCreated(now);
        discards++;
    }

    public void recordFusion(long now) {
        markCreated(now);
        fusions++;
    }

    public void recordAwakening(long now) {
        markCreated(now);
        awakenings++;
    }

    public double usageFrequency() {
        return perHour(uses);
    }

    public double lifetimeHours() {
        if (firstSeenAt == 0L || lastSeenAt == 0L || lastSeenAt <= firstSeenAt) {
            return 0.0D;
        }
        return (lastSeenAt - firstSeenAt) / 3_600_000.0D;
    }

    public double killParticipation() {
        if (uses <= 0L) {
            return 0.0D;
        }
        return (double) kills / (double) uses;
    }

    public double discardRate() {
        long totalOutcomes = uses + discards;
        if (totalOutcomes <= 0L) {
            return 0.0D;
        }
        return (double) discards / (double) totalOutcomes;
    }

    public double fusionParticipation() {
        if (uses <= 0L) {
            return 0.0D;
        }
        return (double) fusions / (double) uses;
    }

    public double awakeningRate() {
        if (uses <= 0L) {
            return 0.0D;
        }
        return (double) awakenings / (double) uses;
    }

    public MechanicUtilitySignal recordUtilityOutcome(UtilityOutcomeRecord outcome) {
        markCreated(outcome.occurredAt());
        String mechanicKey = outcome.mechanic().name() + "@" + outcome.trigger().name();
        OutcomeUtilityProfile profile = utilityProfiles.computeIfAbsent(mechanicKey, OutcomeUtilityProfile::new);
        return profile.ingest(outcome, classifier);
    }

    public Map<String, MechanicUtilitySignal> utilitySignalsByMechanic() {
        Map<String, MechanicUtilitySignal> snapshot = new ConcurrentHashMap<>();
        utilityProfiles.forEach((k, profile) -> snapshot.put(k, profile.snapshot()));
        return Map.copyOf(snapshot);
    }

    public double validatedUtilityScore() {
        return utilityProfiles.values().stream().map(OutcomeUtilityProfile::snapshot)
                .mapToDouble(MechanicUtilitySignal::validatedUtility)
                .sum();
    }

    public double utilityDensity() {
        double utility = utilityProfiles.values().stream().map(OutcomeUtilityProfile::snapshot)
                .mapToDouble(MechanicUtilitySignal::validatedUtility)
                .sum();
        double budget = utilityProfiles.values().stream().map(OutcomeUtilityProfile::snapshot)
                .mapToDouble(MechanicUtilitySignal::budgetConsumed)
                .sum();
        return utility / Math.max(1.0D, budget);
    }

    public double averageNoOpRate() {
        return averageSignal(MechanicUtilitySignal::noOpRate);
    }

    public double averageSpamPenalty() {
        return averageSignal(MechanicUtilitySignal::spamPenalty);
    }

    public double averageRedundancyPenalty() {
        return averageSignal(MechanicUtilitySignal::redundancyPenalty);
    }

    public double averageContextualRelevance() {
        return averageSignal(MechanicUtilitySignal::contextualRelevance);
    }

    public double meaningfulOutcomeRate() {
        long meaningful = utilityProfiles.values().stream().map(OutcomeUtilityProfile::snapshot)
                .mapToLong(MechanicUtilitySignal::meaningfulOutcomes)
                .sum();
        long attempts = utilityProfiles.values().stream().map(OutcomeUtilityProfile::snapshot)
                .mapToLong(MechanicUtilitySignal::attempts)
                .sum();
        return attempts == 0L ? 0.0D : (double) meaningful / attempts;
    }

    public double utilityBudgetEfficiency() {
        double budget = utilityProfiles.values().stream().map(OutcomeUtilityProfile::snapshot)
                .mapToDouble(MechanicUtilitySignal::budgetConsumed)
                .sum();
        return validatedUtilityScore() / Math.max(1.0D, budget);
    }

    private double averageSignal(java.util.function.ToDoubleFunction<MechanicUtilitySignal> metric) {
        if (utilityProfiles.isEmpty()) {
            return 0.0D;
        }
        return utilityProfiles.values().stream().map(OutcomeUtilityProfile::snapshot)
                .mapToDouble(metric)
                .average()
                .orElse(0.0D);
    }

    private double perHour(long count) {
        double life = Math.max(1.0D / 60.0D, lifetimeHours());
        return count / life;
    }
}
