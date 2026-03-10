package obtuseloot.abilities;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.eligibility.ArtifactEligibility;
import obtuseloot.reputation.ArtifactReputation;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;

public class ItemAbilityManager {
    private final AbilityResolver resolver;
    private final Map<String, Integer> triggerCounts = new HashMap<>();
    private final TriggerSubscriptionIndex subscriptionIndex = new TriggerSubscriptionIndex();
    private final EventAbilityDispatcher dispatcher = new EventAbilityDispatcher();

    private final LongAdder dispatchCalls = new LongAdder();
    private final LongAdder indexedDispatchCalls = new LongAdder();
    private final LongAdder fullScanDispatchCalls = new LongAdder();
    private final LongAdder totalIndexedSubscribers = new LongAdder();
    private final EnumMap<AbilityTrigger, LongAdder> dispatchByTrigger = new EnumMap<>(AbilityTrigger.class);
    private final EnumMap<AbilityTrigger, LongAdder> subscriberByTrigger = new EnumMap<>(AbilityTrigger.class);

    private volatile boolean triggerSubscriptionIndexingEnabled = true;

    public ItemAbilityManager(AbilityResolver resolver) {
        this.resolver = resolver;
        for (AbilityTrigger trigger : AbilityTrigger.values()) {
            dispatchByTrigger.put(trigger, new LongAdder());
            subscriberByTrigger.put(trigger, new LongAdder());
        }
    }

    public void setTriggerSubscriptionIndexingEnabled(boolean enabled) {
        this.triggerSubscriptionIndexingEnabled = enabled;
    }

    public boolean isTriggerSubscriptionIndexingEnabled() {
        return triggerSubscriptionIndexingEnabled;
    }

    public AbilityProfile profileFor(Artifact artifact, ArtifactReputation rep) {
        if (!ArtifactEligibility.isAbilityEligible(artifact)) {
            return new AbilityProfile("generic-baseline", List.of());
        }
        return resolver.resolve(artifact, rep);
    }

    public List<String> resolveEffects(AbilityEventContext context) {
        dispatchCalls.increment();
        dispatchByTrigger.get(context.trigger()).increment();

        UUID ownerId = context.artifact().getOwnerId();
        if (triggerSubscriptionIndexingEnabled && ownerId != null) {
            PlayerArtifactTriggerMap triggerMap = subscriptionIndex.getOrRebuild(ownerId, context.artifact(), context.reputation(), this, "lazy-event-build");
            List<ArtifactTriggerBinding> bindings = triggerMap.bindingsFor(context.trigger());
            indexedDispatchCalls.increment();
            totalIndexedSubscribers.add(bindings.size());
            subscriberByTrigger.get(context.trigger()).add(bindings.size());
            java.util.List<String> effects = dispatcher.dispatchIndexed(context, bindings, this);
            ObtuseLoot plugin = ObtuseLoot.get();
            if (plugin != null) {
                plugin.getArtifactManager().markDirty(context.artifact());
            }
            return effects;
        }

        fullScanDispatchCalls.increment();
        AbilityProfile profile = profileFor(context.artifact(), context.reputation());
        java.util.List<String> effects = dispatcher.dispatchFullScan(context, profile, this);
        ObtuseLoot plugin = ObtuseLoot.get();
        if (plugin != null) {
            plugin.getArtifactManager().markDirty(context.artifact());
        }
        return effects;
    }

    void recordTriggerDispatch(AbilityDefinition def, AbilityTrigger trigger) {
        triggerCounts.merge(def.id() + "@" + trigger, 1, Integer::sum);
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

    public void clearAllSubscriptions() {
        subscriptionIndex.clear();
        ObtuseLoot plugin = ObtuseLoot.get();
        if (plugin != null) {
            for (org.bukkit.entity.Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
                plugin.getArtifactManager().pinSubscriptions(online.getUniqueId(), false);
            }
        }
    }

    public PlayerArtifactTriggerMap triggerMap(UUID playerId) {
        return subscriptionIndex.get(playerId);
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
