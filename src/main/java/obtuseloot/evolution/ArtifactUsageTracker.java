package obtuseloot.evolution;

import obtuseloot.abilities.AbilityDefinition;
import obtuseloot.abilities.AbilityEventContext;
import obtuseloot.abilities.AbilityExecutionResult;
import obtuseloot.abilities.TriggerBudgetProfile;
import obtuseloot.abilities.TriggerBudgetResolver;
import obtuseloot.artifacts.Artifact;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

public class ArtifactUsageTracker {
    private final Map<Long, ArtifactUsageProfile> profiles = new ConcurrentHashMap<>();
    private final TriggerBudgetResolver budgetResolver = new TriggerBudgetResolver();

    public ArtifactUsageProfile profileFor(Artifact artifact) {
        return profileForSeed(artifact.getArtifactSeed());
    }

    public ArtifactUsageProfile profileForSeed(long artifactSeed) {
        return profiles.computeIfAbsent(artifactSeed, k -> new ArtifactUsageProfile());
    }

    public void trackCreated(Artifact artifact) {
        profileFor(artifact).markCreated(System.currentTimeMillis());
    }

    public void trackUse(Artifact artifact) {
        profileFor(artifact).recordUse(System.currentTimeMillis());
    }

    public void trackKillParticipation(Artifact artifact) {
        profileFor(artifact).recordKill(System.currentTimeMillis());
    }

    public void trackDiscard(Artifact artifact) {
        profileFor(artifact).recordDiscard(System.currentTimeMillis());
    }

    public void trackFusionParticipation(Artifact artifact) {
        profileFor(artifact).recordFusion(System.currentTimeMillis());
    }

    public void trackAwakening(Artifact artifact) {
        profileFor(artifact).recordAwakening(System.currentTimeMillis());
    }

    public void trackAbilityExecution(Artifact artifact,
                                      AbilityEventContext context,
                                      AbilityExecutionResult result,
                                      AbilityDefinition definition) {
        if (artifact == null || context == null || result == null || definition == null) {
            return;
        }
        TriggerBudgetProfile budgetProfile = budgetResolver.resolve(definition, context);
        boolean intentional = (context.runtimeContext() != null && context.runtimeContext().intentional())
                || budgetProfile.intentionalPreferred();
        double relevance = contextualRelevance(definition, result, intentional);
        double budgetCost = budgetProfile.triggerCost() + budgetProfile.evaluationCost();

        profileFor(artifact).recordUtilityOutcome(new UtilityOutcomeRecord(
                result.abilityId(),
                result.mechanic(),
                result.trigger(),
                result.status(),
                result.outcomeType(),
                result.meaningfulOutcome(),
                intentional,
                relevance,
                budgetCost,
                context.source(),
                System.currentTimeMillis()
        ));
    }

    private double contextualRelevance(AbilityDefinition definition,
                                       AbilityExecutionResult result,
                                       boolean intentional) {
        if (definition.metadata() == null) {
            return intentional ? 1.0D : 0.8D;
        }
        double metadataValue = (definition.metadata().discoveryValue()
                + definition.metadata().explorationValue()
                + definition.metadata().informationValue()
                + definition.metadata().ritualValue()
                + definition.metadata().socialValue()
                + definition.metadata().worldUtilityValue()) / 6.0D;
        double outcomeFactor = result.meaningfulOutcome() ? 1.1D : 0.75D;
        return Math.max(0.2D, metadataValue * outcomeFactor + (intentional ? 0.1D : 0.0D));
    }

    public Map<String, MechanicUtilitySignal> utilitySignalRollup() {
        Map<String, MechanicUtilitySignal> merged = new ConcurrentHashMap<>();
        profiles.values().forEach(profile -> profile.utilitySignalsByMechanic().forEach((k, signal) ->
                merged.merge(k, signal, this::mergeSignal)));
        return Map.copyOf(merged);
    }

    public Map<String, MechanicUtilitySignal> highVolumeLowValueSignals() {
        return utilitySignalRollup().entrySet().stream()
                .filter(entry -> entry.getValue().attempts() >= 8 && entry.getValue().utilityDensity() < 0.05D)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private MechanicUtilitySignal mergeSignal(MechanicUtilitySignal a, MechanicUtilitySignal b) {
        long attempts = a.attempts() + b.attempts();
        long meaningful = a.meaningfulOutcomes() + b.meaningfulOutcomes();
        double budget = a.budgetConsumed() + b.budgetConsumed();
        double validated = a.validatedUtility() + b.validatedUtility();
        return new MechanicUtilitySignal(
                a.mechanicKey(),
                validated,
                validated / Math.max(1.0D, budget),
                weightedAverage(a.contextualRelevance(), a.attempts(), b.contextualRelevance(), b.attempts()),
                weightedAverage(a.noOpRate(), a.attempts(), b.noOpRate(), b.attempts()),
                weightedAverage(a.spamPenalty(), a.attempts(), b.spamPenalty(), b.attempts()),
                weightedAverage(a.redundancyPenalty(), a.attempts(), b.redundancyPenalty(), b.attempts()),
                attempts,
                meaningful,
                budget
        );
    }

    private double weightedAverage(double av, long aw, double bv, long bw) {
        long weight = aw + bw;
        if (weight == 0L) {
            return 0.0D;
        }
        return ((av * aw) + (bv * bw)) / weight;
    }
}
