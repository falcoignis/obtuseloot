package obtuseloot.evolution;

import obtuseloot.abilities.AbilityDefinition;
import obtuseloot.abilities.AbilityEventContext;
import obtuseloot.abilities.AbilityExecutionResult;
import obtuseloot.abilities.TriggerBudgetProfile;
import obtuseloot.abilities.TriggerBudgetResolver;
import obtuseloot.artifacts.Artifact;
import obtuseloot.telemetry.ArtifactRuntimeCache;
import obtuseloot.telemetry.EcosystemTelemetryEmitter;
import obtuseloot.telemetry.EcosystemTelemetryEventType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ArtifactUsageTracker {
    private final Map<Long, ArtifactUsageProfile> profiles = new ConcurrentHashMap<>();
    private final TriggerBudgetResolver budgetResolver = new TriggerBudgetResolver();
    private final NichePopulationTracker nichePopulationTracker = new NichePopulationTracker();
    private final ArtifactRuntimeCache<Map<String, MechanicUtilitySignal>> signalCache;
    private volatile EcosystemTelemetryEmitter telemetryEmitter;

    public ArtifactUsageTracker() {
        this(new ArtifactRuntimeCache<>(2048, 300_000L));
    }

    public ArtifactUsageTracker(ArtifactRuntimeCache<Map<String, MechanicUtilitySignal>> signalCache) {
        this.signalCache = signalCache;
    }

    public void setTelemetryEmitter(EcosystemTelemetryEmitter telemetryEmitter) {
        this.telemetryEmitter = telemetryEmitter;
        nichePopulationTracker.setTelemetryEmitter(telemetryEmitter);
    }

    public ArtifactUsageProfile profileFor(Artifact artifact) {
        return profileForSeed(artifact.getArtifactSeed());
    }

    public ArtifactUsageProfile profileForSeed(long artifactSeed) {
        return profiles.computeIfAbsent(artifactSeed, k -> new ArtifactUsageProfile());
    }

    public void trackCreated(Artifact artifact) {
        profileFor(artifact).markCreated(System.currentTimeMillis());
        nichePopulationTracker.markCreated(artifact.getArtifactSeed());
    }

    public void trackUse(Artifact artifact) {
        profileFor(artifact).recordUse(System.currentTimeMillis());
    }

    public void trackKillParticipation(Artifact artifact) {
        profileFor(artifact).recordKill(System.currentTimeMillis());
    }

    public void trackDiscard(Artifact artifact) {
        profileFor(artifact).recordDiscard(System.currentTimeMillis());
        nichePopulationTracker.markDiscarded(artifact.getArtifactSeed());
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

        ArtifactUsageProfile profile = profileFor(artifact);
        profile.recordUtilityOutcome(new UtilityOutcomeRecord(
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
        Map<String, MechanicUtilitySignal> signals = signalCache.getOrCompute(artifact.getArtifactSeed(), profile::utilitySignalsByMechanic);
        nichePopulationTracker.recordTelemetry(artifact.getArtifactSeed(), artifact.getLatentLineage(), signals);

        EcosystemTelemetryEmitter emitter = telemetryEmitter;
        if (emitter != null) {
            // Use effectiveNicheName so artifacts assigned to dynamic child niches
            // are attributed to those child niches in telemetry.
            String effectiveNiche = nichePopulationTracker.effectiveNicheName(artifact.getArtifactSeed());
            String outcomeClassification = result.meaningfulOutcome() ? "MEANINGFUL"
                    : (result.outcomeType() == obtuseloot.abilities.AbilityOutcomeType.FLAVOR_ONLY ? "FLAVOR_ONLY" : "NO_OP");
            String nicheTags = definition.metadata() == null ? "general" : String.join("|", definition.metadata().utilityDomains());
            Map<String, String> attributes = new HashMap<>(Map.ofEntries(
                    Map.entry("abilityId", result.abilityId()),
                    Map.entry("ability_id", result.abilityId()),
                    Map.entry("mechanic", result.mechanic().name()),
                    Map.entry("trigger", result.trigger().name()),
                    Map.entry("status", result.status().name()),
                    Map.entry("execution_status", result.status().name()),
                    Map.entry("meaningful", String.valueOf(result.meaningfulOutcome())),
                    Map.entry("budget_cost", String.valueOf(budgetCost)),
                    Map.entry("utility_score", String.valueOf(relevance)),
                    Map.entry("utility_density", String.valueOf(Math.max(0.0D, relevance / Math.max(1.0D, budgetCost)))),
                    Map.entry("player_id", artifact.getOwnerId() == null ? "na" : artifact.getOwnerId().toString()),
                    Map.entry("chunk", context.runtimeContext() != null && context.runtimeContext().chunkKey() != null
                            ? String.valueOf(context.runtimeContext().chunkKey()) : "na"),
                    Map.entry("world", context.runtimeContext() != null && context.runtimeContext().world() != null
                            ? context.runtimeContext().world() : "na"),
                    Map.entry("dimension", context.runtimeContext() != null && context.runtimeContext().dimension() != null
                            ? context.runtimeContext().dimension() : "na"),
                    Map.entry("context_tags", "ability-execution"),
                    Map.entry("outcome_classification", outcomeClassification),
                    Map.entry("niche_tags", nicheTags)
            ));
            attributes.putAll(abilityTelemetrySignals(result, context));
            emitter.emit(EcosystemTelemetryEventType.ABILITY_EXECUTION,
                    artifact.getArtifactSeed(),
                    artifact.getLatentLineage(),
                    effectiveNiche,
                    attributes);
        }
    }

    private Map<String, String> abilityTelemetrySignals(AbilityExecutionResult result,
                                                         AbilityEventContext context) {
        Map<String, String> fields = new HashMap<>();
        double value = Math.max(0.0D, context.value());
        switch (result.mechanic()) {
            case TRAIL_SENSE -> {
                fields.put("exploration_chain_length", String.valueOf(1 + Math.round(value * 4.0D)));
                fields.put("distance_traveled", String.valueOf(value * 24.0D));
                fields.put("uncharted_chunk_entries", result.meaningfulOutcome() ? "1" : "0");
                fields.put("niche", "EXPLORATION");
            }
            case FORAGER_MEMORY -> {
                fields.put("harvest_chain_length", String.valueOf(1 + Math.round(value * 5.0D)));
                fields.put("cluster_detection_events", result.meaningfulOutcome() ? "1" : "0");
                fields.put("resource_density", String.valueOf(Math.min(1.0D, value)));
                fields.put("niche", "GATHERING");
            }
            case PATTERN_RESONANCE -> {
                fields.put("ritual_pattern_frequency", String.valueOf(Math.round(value * 10.0D)));
                fields.put("ritual_activation_count", result.meaningfulOutcome() ? "1" : "0");
                fields.put("pattern_repeat_interval", String.valueOf(Math.max(1.0D, 12.0D - (value * 8.0D))));
                fields.put("niche", "RITUAL");
            }
            case WITNESS_IMPRINT -> {
                fields.put("witness_interactions", result.meaningfulOutcome() ? "1" : "0");
                fields.put("co_presence_density", String.valueOf(Math.min(1.0D, value)));
                fields.put("artifact_visibility_events", result.meaningfulOutcome() ? "1" : "0");
                fields.put("niche", "SOCIAL");
            }
            case CARTOGRAPHERS_ECHO -> {
                fields.put("structure_chain_discovery", result.meaningfulOutcome() ? "1" : "0");
                fields.put("structure_proximity_events", String.valueOf(Math.round(value * 3.0D)));
                fields.put("exploration_success_rate", String.valueOf(result.meaningfulOutcome() ? Math.min(1.0D, value) : 0.0D));
                fields.put("niche", "EXPLORATION");
            }
            default -> {
            }
        }
        return fields;
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

    public UtilityHistoryRollup utilityHistoryFor(Artifact artifact) {
        if (artifact == null) {
            return UtilityHistoryRollup.empty();
        }
        return UtilityHistoryRollup.fromProfile(profileFor(artifact));
    }

    public void hydrateFromArtifact(Artifact artifact) {
        if (artifact == null) {
            return;
        }
        UtilityHistoryRollup rollup = UtilityHistoryRollup.parse(artifact.getLastUtilityHistory());
        profileFor(artifact).restoreUtilitySignals(rollup.signalByMechanicTrigger());
        nichePopulationTracker.recordTelemetry(artifact.getArtifactSeed(), rollup.signalByMechanicTrigger());
    }

    public NichePopulationTracker nichePopulationTracker() {
        return nichePopulationTracker;
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
