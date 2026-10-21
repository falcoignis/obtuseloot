package obtuseloot.abilities;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactArchetypeValidator;
import obtuseloot.artifacts.eligibility.ArtifactEligibility;
import obtuseloot.evolution.MechanicUtilitySignal;
import obtuseloot.evolution.ArtifactUsageTracker;
import obtuseloot.reputation.ArtifactReputation;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public class ItemAbilityManager {
    private final AbilityResolver resolver;
    private final AbilityRegistry registry;
    public AbilityRegistry registry() { return registry; }
    private final ArtifactUsageTracker usageTracker;
    private java.util.function.Consumer<AbilityOutcomeObservation> outcomeObserver = observation -> { };
    private final Map<String, Integer> triggerCounts = new HashMap<>();
    private final Map<String, Integer> triggerSourceCounts = new HashMap<>();
    private final Map<String, LongAdder> coalescedExecutionByTrigger = new HashMap<>();
    private final TriggerSubscriptionIndex subscriptionIndex = new TriggerSubscriptionIndex();
    private final EventAbilityDispatcher dispatcher;
    private final TriggerBudgetManager triggerBudgetManager = new TriggerBudgetManager();

    private final LongAdder dispatchCalls = new LongAdder();
    private final LongAdder indexedDispatchCalls = new LongAdder();
    private final LongAdder fullScanDispatchCalls = new LongAdder();
    private final LongAdder totalIndexedSubscribers = new LongAdder();
    private final EnumMap<AbilityExecutionStatus, LongAdder> executionStatusCounts = new EnumMap<>(AbilityExecutionStatus.class);
    private final Map<String, LongAdder> executionStatusByMechanicTrigger = new HashMap<>();
    private final Map<String, LongAdder> meaningfulOutcomeByMechanicTrigger = new HashMap<>();
    private final Map<String, LongAdder> noOpByMechanicTrigger = new HashMap<>();
    private final Map<String, LongAdder> suppressionReasonCounts = new HashMap<>();
    private final Map<String, LongAdder> outcomeTypeCounts = new HashMap<>();
    private final EnumMap<AbilityTrigger, LongAdder> dispatchByTrigger = new EnumMap<>(AbilityTrigger.class);
    private final EnumMap<AbilityTrigger, LongAdder> subscriberByTrigger = new EnumMap<>(AbilityTrigger.class);

    private volatile boolean triggerSubscriptionIndexingEnabled = true;

    public ItemAbilityManager(AbilityResolver resolver) {
        this(resolver, new AbilityRegistry());
    }

    public ItemAbilityManager(AbilityResolver resolver, AbilityRegistry registry) {
        this(resolver, registry, null);
    }

    public ItemAbilityManager(AbilityResolver resolver, AbilityRegistry registry, ArtifactUsageTracker usageTracker) {
        this.resolver = resolver;
        this.registry = registry;
        this.usageTracker = usageTracker;
        this.dispatcher = new EventAbilityDispatcher(registry);
        for (AbilityTrigger trigger : AbilityTrigger.values()) {
            dispatchByTrigger.put(trigger, new LongAdder());
            subscriberByTrigger.put(trigger, new LongAdder());
        }
        for (AbilityExecutionStatus status : AbilityExecutionStatus.values()) {
            executionStatusCounts.put(status, new LongAdder());
        }
    }

    public void setOutcomeObserver(java.util.function.Consumer<AbilityOutcomeObservation> observer) {
        this.outcomeObserver = java.util.Objects.requireNonNull(observer);
    }

    public void setTriggerSubscriptionIndexingEnabled(boolean enabled) {
        this.triggerSubscriptionIndexingEnabled = enabled;
    }

    public boolean isTriggerSubscriptionIndexingEnabled() {
        return triggerSubscriptionIndexingEnabled;
    }

    public AbilityProfile profileFor(Artifact artifact, ArtifactReputation rep) {
        ArtifactArchetypeValidator.requireValid(artifact, "ability profiling");
        return resolver.resolve(artifact, rep);
    }

    public AbilityDispatchResult resolveDispatch(AbilityEventContext context) {
        dispatchCalls.increment();
        dispatchByTrigger.get(context.trigger()).increment();
        String sourceId = context.runtimeContext() == null || context.runtimeContext().source() == null
                ? context.source()
                : context.runtimeContext().source().id();
        triggerSourceCounts.merge(context.trigger()+"#"+sourceId, 1, Integer::sum);
        if (context.runtimeContext() != null && context.runtimeContext().coalesced()) {
            coalescedExecutionByTrigger.computeIfAbsent(context.trigger().name(), ignored -> new LongAdder()).increment();
        }
        executionStatusCounts.get(AbilityExecutionStatus.TRIGGER_SEEN).increment();

        UUID ownerId = context.artifact().getOwnerId();
        if (triggerSubscriptionIndexingEnabled && ownerId != null) {
            PlayerArtifactTriggerMap triggerMap = subscriptionIndex.getOrRebuild(ownerId, context.artifact(), context.reputation(), this, "lazy-event-build");
            List<ArtifactTriggerBinding> bindings = triggerMap.bindingsFor(context.trigger());
            Map<String, AbilityDefinition> definitionsById = bindings.stream()
                    .map(ArtifactTriggerBinding::definition)
                    .collect(Collectors.toMap(AbilityDefinition::id, d -> d, (a, b) -> a));
            indexedDispatchCalls.increment();
            totalIndexedSubscribers.add(bindings.size());
            subscriberByTrigger.get(context.trigger()).add(bindings.size());
            AbilityDispatchResult dispatchResult = dispatcher.dispatchIndexed(context, bindings, this);
            recordUtilityOutcomes(context, dispatchResult, definitionsById);
            ObtuseLoot plugin = ObtuseLoot.get();
            if (plugin != null) {
                plugin.getArtifactManager().markDirty(context.artifact());
            }
            return dispatchResult;
        }

        fullScanDispatchCalls.increment();
        AbilityProfile profile = context.liveExecution() && context.activation() != null && ownerId != null
                ? new AbilityProfile("equipped", equippedDefinitions(ownerId, context.artifact()))
                : profileFor(context.artifact(), context.reputation());
        Map<String, AbilityDefinition> definitionsById = profile.abilities().stream()
                .collect(Collectors.toMap(AbilityDefinition::id, d -> d, (a, b) -> a));
        AbilityDispatchResult dispatchResult = dispatcher.dispatchFullScan(context, profile, this);
        recordUtilityOutcomes(context, dispatchResult, definitionsById);
        ObtuseLoot plugin = ObtuseLoot.get();
        if (plugin != null) {
            plugin.getArtifactManager().markDirty(context.artifact());
        }
        return dispatchResult;
    }

    public List<String> resolveEffects(AbilityEventContext context) {
        return resolveDispatch(context).presentationEffects();
    }

    void recordExecution(AbilityExecutionResult result) {
        executionStatusCounts.get(result.status()).increment();
        String mechanicTriggerKey = result.mechanic().name() + "@" + result.trigger();
        executionStatusByMechanicTrigger.computeIfAbsent(mechanicTriggerKey + "#" + result.status(), ignored -> new LongAdder()).increment();
        outcomeTypeCounts.computeIfAbsent(result.mechanic().name() + "@" + result.trigger() + "#" + result.outcomeType(), ignored -> new LongAdder()).increment();
        if (result.meaningfulOutcome()) {
            meaningfulOutcomeByMechanicTrigger.computeIfAbsent(mechanicTriggerKey, ignored -> new LongAdder()).increment();
        }
        if (result.status() == AbilityExecutionStatus.NO_OP) {
            noOpByMechanicTrigger.computeIfAbsent(mechanicTriggerKey, ignored -> new LongAdder()).increment();
        }
        if (result.suppressionReason() != null && !result.suppressionReason().isBlank()) {
            suppressionReasonCounts.computeIfAbsent(result.suppressionReason(), ignored -> new LongAdder()).increment();
        }
        if (result.status() == AbilityExecutionStatus.SUCCESS) {
            triggerCounts.merge(result.mechanic().name() + "@" + result.trigger(), 1, Integer::sum);
        }
    }

    public void rebuildSubscriptions(UUID playerId, Artifact artifact, ArtifactReputation reputation, String reason) {
        if (playerId == null || artifact == null || reputation == null) {
            return;
        }
        subscriptionIndex.rebuild(playerId, artifact, reputation, this, reason);
        ObtuseLoot plugin = ObtuseLoot.get();
        if (plugin != null) {
            plugin.getArtifactManager().pinSubscriptions(playerId, true);
        }
    }

    public void clearSubscriptions(UUID playerId) {
        if (playerId != null) {
            subscriptionIndex.remove(playerId);
            ObtuseLoot plugin = ObtuseLoot.get();
            if (plugin != null) {
                plugin.getArtifactManager().pinSubscriptions(playerId, false);
            }
        }
    }

    public void clearSubscriptions(UUID playerId, String itemKey) { subscriptionIndex.remove(playerId, itemKey); }

    public void clearAllSubscriptions() {
        subscriptionIndex.clear();
        ObtuseLoot plugin = ObtuseLoot.get();
        if (plugin != null) {
            for (org.bukkit.entity.Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
                plugin.getArtifactManager().pinSubscriptions(online.getUniqueId(), false);
            }
        }
    }

    public PlayerArtifactTriggerMap triggerMap(UUID playerId, Artifact artifact) {
        return subscriptionIndex.get(playerId, artifact.getArtifactStorageKey());
    }
    public PlayerArtifactTriggerMap triggerMap(UUID playerId) {
        return subscriptionIndex.get(playerId);
    }
    public List<AbilityDefinition> equippedDefinitions(UUID player, Artifact artifact) {
        return subscriptionIndex.getOrRebuild(player, artifact, artifact.getItemReputation(), this, "equipped-item")
                .asMap().values().stream().flatMap(List::stream).map(ArtifactTriggerBinding::definition).toList();
    }

    public TriggerSubscriptionIndexStats indexStats() {
        return new TriggerSubscriptionIndexStats(
                triggerSubscriptionIndexingEnabled,
                subscriptionIndex.playerCount(),
                subscriptionIndex.rebuildCount(),
                subscriptionIndex.averageRebuildMicros(),
                dispatchCalls.sum(),
                indexedDispatchCalls.sum(),
                fullScanDispatchCalls.sum(),
                averageIndexedSubscribers()
        );
    }

    public Map<AbilityTrigger, Double> averageSubscribersPerTrigger() {
        EnumMap<AbilityTrigger, Double> averages = new EnumMap<>(AbilityTrigger.class);
        for (AbilityTrigger trigger : AbilityTrigger.values()) {
            long dispatches = dispatchByTrigger.get(trigger).sum();
            long subscribers = subscriberByTrigger.get(trigger).sum();
            averages.put(trigger, dispatches == 0 ? 0.0D : (double) subscribers / dispatches);
        }
        return Map.copyOf(averages);
    }

    private double averageIndexedSubscribers() {
        long indexedCalls = indexedDispatchCalls.sum();
        if (indexedCalls == 0) {
            return 0.0D;
        }
        return (double) totalIndexedSubscribers.sum() / indexedCalls;
    }

    public Map<String, Integer> triggerCounts() {
        return Map.copyOf(triggerCounts);
    }

    public Map<String, Integer> triggerSourceCounts() {
        return Map.copyOf(triggerSourceCounts);
    }

    public Map<AbilityExecutionStatus, Long> executionStatusCounts() {
        EnumMap<AbilityExecutionStatus, Long> snapshot = new EnumMap<>(AbilityExecutionStatus.class);
        for (Map.Entry<AbilityExecutionStatus, LongAdder> entry : executionStatusCounts.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().sum());
        }
        return Map.copyOf(snapshot);
    }

    public Map<String, Long> executionStatusByAbilityTrigger() {
        return executionStatusByMechanicTrigger();
    }

    public Map<String, Long> meaningfulOutcomeByAbilityTrigger() {
        return meaningfulOutcomeByMechanicTrigger();
    }

    public Map<String, Long> executionStatusByMechanicTrigger() {
        return snapshotLongAdders(executionStatusByMechanicTrigger);
    }

    public Map<String, Long> meaningfulOutcomeByMechanicTrigger() {
        return snapshotLongAdders(meaningfulOutcomeByMechanicTrigger);
    }

    public Map<String, Long> noOpByMechanicTrigger() {
        return snapshotLongAdders(noOpByMechanicTrigger);
    }

    public Map<String, Long> suppressionReasonCounts() {
        Map<String, Long> merged = new HashMap<>(snapshotLongAdders(suppressionReasonCounts));
        triggerBudgetManager.suppressionCounts().forEach((k, v) -> merged.merge("budget:" + k, v, Long::sum));
        return Map.copyOf(merged);
    }

    public Map<String, Long> outcomeTypeCounts() {
        return snapshotLongAdders(outcomeTypeCounts);
    }

    public Map<String, Long> coalescedExecutionByTrigger() {
        return snapshotLongAdders(coalescedExecutionByTrigger);
    }

    public Map<String, MechanicUtilitySignal> utilitySignalsForArtifact(Artifact artifact) {
        ObtuseLoot plugin = ObtuseLoot.get();
        ArtifactUsageTracker tracker = usageTracker != null ? usageTracker
                : plugin == null ? null : plugin.getArtifactUsageTracker();
        return tracker == null || artifact == null ? Map.of()
                : tracker.profileFor(artifact).utilitySignalsByMechanic();
    }

    /** One completed physical effect; callers own once-only receipts and current equipment validation. */
    public void observeDeferred(AbilityDefinition definition, AbilityEventContext context, AbilityExecutionResult result) {
        if (!new AbilityOutcomeObservation(context, result, definition, 0).liveMeasured())
            throw new IllegalArgumentException("Invalid deferred observation");
        recordExecution(result);
        recordUtilityOutcomes(context, new AbilityDispatchResult(context, List.of(result)), Map.of(definition.id(), definition));
    }

    private void recordUtilityOutcomes(AbilityEventContext context,
                                       AbilityDispatchResult dispatchResult,
                                       Map<String, AbilityDefinition> definitionsById) {
        ObtuseLoot plugin = ObtuseLoot.get();
        ArtifactUsageTracker tracker = usageTracker != null ? usageTracker
                : plugin == null ? null : plugin.getArtifactUsageTracker();
        if (tracker == null) {
            return;
        }
        dispatchResult.executions().forEach(result -> {
            if (result.status() == AbilityExecutionStatus.PENDING) return;
            AbilityDefinition definition = definitionsById.get(result.abilityId());
            if (definition != null) {
                double delta = tracker.trackAbilityExecution(context.artifact(), context, result, definition);
                outcomeObserver.accept(new AbilityOutcomeObservation(context, result, definition, delta));
            }
        });
    }

    private Map<String, Long> snapshotLongAdders(Map<String, LongAdder> source) {
        Map<String, Long> snapshot = new HashMap<>();
        source.forEach((key, value) -> snapshot.put(key, value.sum()));
        return Map.copyOf(snapshot);
    }


    public TriggerBudgetManager triggerBudgetManager() {
        return triggerBudgetManager;
    }

    public Map<String, Long> triggerBudgetConsumptionByAbility() {
        return triggerBudgetManager.consumptionByAbility();
    }

    public Map<String, Long> triggerBudgetConsumptionByTrigger() {
        return triggerBudgetManager.consumptionByTrigger();
    }

    public TraitProjectionStats traitProjectionStats() {
        if (resolver instanceof SeededAbilityResolver seeded) {
            return seeded.traitProjectionStats();
        }
        return new TraitProjectionStats(false, ScoringMode.BASELINE, 0, 0, 0, 0, 0, 0, 0, 0, 0.0D, 1.0D);
    }

    public record TriggerSubscriptionIndexStats(
            boolean enabled,
            int indexedPlayers,
            long rebuildCount,
            double averageRebuildMicros,
            long dispatchCalls,
            long indexedDispatchCalls,
            long fallbackFullScanCalls,
            double averageIndexedSubscribers
    ) {
    }
}
