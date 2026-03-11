package obtuseloot.abilities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public final class TriggerBudgetManager {
    private static final double HOLDER_CAPACITY = 28.0D;
    private static final double HOLDER_REFILL = 11.0D;
    private static final double ARTIFACT_CAPACITY = 16.0D;
    private static final double ARTIFACT_REFILL = 6.0D;
    private static final double TRIGGER_CAPACITY = 14.0D;
    private static final double TRIGGER_REFILL = 5.0D;

    private final Map<UUID, BudgetPool> holderPools = new ConcurrentHashMap<>();
    private final Map<String, BudgetPool> artifactPools = new ConcurrentHashMap<>();
    private final Map<String, BudgetPool> triggerPools = new ConcurrentHashMap<>();
    private final Map<String, BudgetPool> abilityPools = new ConcurrentHashMap<>();
    private final Map<String, Long> antiSpamBySourceKey = new ConcurrentHashMap<>();

    private final Map<String, LongAdder> suppressionReasonCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> consumptionByAbility = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> consumptionByTrigger = new ConcurrentHashMap<>();


    public boolean allowProbe(UUID holderId, String artifactKey, AbilityTrigger trigger, String source, double cost, boolean intentional) {
        long now = System.currentTimeMillis();
        TriggerBudgetPolicy policy = intentional ? TriggerBudgetPolicy.ACTIVE_INTENTIONAL : TriggerBudgetPolicy.PASSIVE_LOW_PRIORITY;
        TriggerBudgetProfile profile = new TriggerBudgetProfile(cost, Math.max(0.1D, cost * 0.6D), 10.0D, 4.0D, 3, 1000L, intentional ? 80 : 30, policy, intentional, intentional ? 50.0D : 250.0D);
        // direct pool checks for listener-side probes
        TriggerSuppressionReason reason = denyReason(holderPool(holderId), profile.evaluationCost(), profile, now, profile.priority(), TriggerSuppressionReason.HOLDER_BUDGET_EXHAUSTED);
        if (reason != null) {
            recordSuppression(reason);
            return false;
        }
        String triggerKey = holderId + "#" + trigger.name();
        reason = denyReason(triggerPool(triggerKey), profile.evaluationCost(), profile, now, profile.priority(), TriggerSuppressionReason.TRIGGER_TYPE_BUDGET_EXHAUSTED);
        if (reason != null) {
            recordSuppression(reason);
            return false;
        }
        holderPool(holderId).consume(profile.evaluationCost(), now);
        triggerPool(triggerKey).consume(profile.evaluationCost(), now);
        if (artifactKey != null && !artifactKey.isBlank()) {
            artifactPool(artifactKey).consume(profile.evaluationCost() * 0.7D, now);
        }
        return true;
    }

    public TriggerBudgetDecision preCheck(AbilityEventContext context, AbilityDefinition ability, TriggerBudgetProfile profile) {
        long now = System.currentTimeMillis();
        UUID holderId = context.artifact().getOwnerId();
        String artifactKey = context.artifact().getArtifactStorageKey();
        String triggerKey = holderId + "#" + context.trigger().name();
        String abilityKey = artifactKey + "#" + ability.id();

        double evaluationCost = policyAdjustedEvaluationCost(profile, profile.evaluationCost());

        TriggerSuppressionReason reason = denyReason(holderPool(holderId), evaluationCost, profile, now, profile.priority(), TriggerSuppressionReason.HOLDER_BUDGET_EXHAUSTED);
        if (reason != null) return denied(reason);
        reason = denyReason(artifactPool(artifactKey), evaluationCost, profile, now, profile.priority(), TriggerSuppressionReason.ARTIFACT_BUDGET_EXHAUSTED);
        if (reason != null) return denied(reason);
        reason = denyReason(triggerPool(triggerKey), evaluationCost, profile, now, profile.priority(), TriggerSuppressionReason.TRIGGER_TYPE_BUDGET_EXHAUSTED);
        if (reason != null) return denied(reason);
        reason = denyReason(abilityPool(abilityKey), evaluationCost, profile, now, profile.priority(), TriggerSuppressionReason.ABILITY_BUDGET_EXHAUSTED);
        if (reason != null) return denied(reason);

        if (isRepeatedNoOp(context, ability, profile, now)) {
            return denied(TriggerSuppressionReason.REPEATED_NO_OP_SUPPRESSION);
        }
        if (isAntiSpamBlocked(context, ability, profile, now)) {
            return denied(TriggerSuppressionReason.ANTI_SPAM_SUPPRESSION);
        }

        holderPool(holderId).consume(evaluationCost, now);
        artifactPool(artifactKey).consume(evaluationCost * 0.8D, now);
        triggerPool(triggerKey).consume(evaluationCost, now);
        abilityPool(abilityKey).consume(evaluationCost * 0.6D, now);
        return TriggerBudgetDecision.allowed(evaluationCost, holderPool(holderId).pressure(now));
    }

    public TriggerBudgetDecision consumeActivation(AbilityEventContext context, AbilityDefinition ability, TriggerBudgetProfile profile) {
        long now = System.currentTimeMillis();
        UUID holderId = context.artifact().getOwnerId();
        String artifactKey = context.artifact().getArtifactStorageKey();
        String triggerKey = holderId + "#" + context.trigger().name();
        String abilityKey = artifactKey + "#" + ability.id();
        double triggerCost = policyAdjustedTriggerCost(profile, profile.triggerCost());

        TriggerSuppressionReason reason = denyReason(holderPool(holderId), triggerCost, profile, now, profile.priority(), TriggerSuppressionReason.HOLDER_BUDGET_EXHAUSTED);
        if (reason != null) return denied(reason);
        reason = denyReason(artifactPool(artifactKey), triggerCost, profile, now, profile.priority(), TriggerSuppressionReason.ARTIFACT_BUDGET_EXHAUSTED);
        if (reason != null) return denied(reason);
        reason = denyReason(triggerPool(triggerKey), triggerCost, profile, now, profile.priority(), TriggerSuppressionReason.TRIGGER_TYPE_BUDGET_EXHAUSTED);
        if (reason != null) return denied(reason);
        reason = denyReason(abilityPool(abilityKey), triggerCost, profile, now, profile.priority(), TriggerSuppressionReason.ABILITY_BUDGET_EXHAUSTED);
        if (reason != null) return denied(reason);

        holderPool(holderId).consume(triggerCost, now);
        artifactPool(artifactKey).consume(triggerCost * 0.9D, now);
        triggerPool(triggerKey).consume(triggerCost, now);
        abilityPool(abilityKey).consume(triggerCost * 0.7D, now);

        antiSpamBySourceKey.put(abilityKey + "#" + context.source(), now);
        consumptionByAbility.computeIfAbsent(ability.id(), ignored -> new LongAdder()).add((long) Math.ceil(triggerCost * 100));
        consumptionByTrigger.computeIfAbsent(context.trigger().name(), ignored -> new LongAdder()).add((long) Math.ceil(triggerCost * 100));
        return TriggerBudgetDecision.allowed(triggerCost, holderPool(holderId).pressure(now));
    }

    public void recordSuppression(TriggerSuppressionReason reason) {
        suppressionReasonCounts.computeIfAbsent(reason.name(), ignored -> new LongAdder()).increment();
    }

    public Map<String, Long> suppressionCounts() {
        return snapshot(suppressionReasonCounts);
    }

    public Map<String, Long> consumptionByAbility() {
        return snapshot(consumptionByAbility);
    }

    public Map<String, Long> consumptionByTrigger() {
        return snapshot(consumptionByTrigger);
    }

    public String debugSummary(UUID holderId, String artifactKey, AbilityTrigger trigger) {
        long now = System.currentTimeMillis();
        String triggerKey = holderId + "#" + trigger.name();
        return "holder=" + holderPool(holderId).debug(now)
                + ", artifact=" + artifactPool(artifactKey).debug(now)
                + ", trigger=" + triggerPool(triggerKey).debug(now);
    }

    private TriggerSuppressionReason denyReason(BudgetPool pool, double cost, TriggerBudgetProfile profile, long now, int priority, TriggerSuppressionReason exhaustedReason) {
        if (!pool.canSpend(cost, now)) {
            return exhaustedReason;
        }
        if (!pool.withinBurst(profile.burstLimit(), profile.burstWindowMs(), now)) {
            return TriggerSuppressionReason.BURST_CAP_EXCEEDED;
        }
        if (profile.policy() == TriggerBudgetPolicy.PASSIVE_LOW_PRIORITY && pool.pressure(now) > 0.8D && priority < 60) {
            return TriggerSuppressionReason.POLICY_DENIED;
        }
        return null;
    }

    private TriggerBudgetDecision denied(TriggerSuppressionReason reason) {
        recordSuppression(reason);
        return TriggerBudgetDecision.denied(reason, 1.0D);
    }

    private boolean isRepeatedNoOp(AbilityEventContext context, AbilityDefinition ability, TriggerBudgetProfile profile, long now) {
        if (profile.policy() != TriggerBudgetPolicy.PASSIVE_LOW_PRIORITY) {
            return false;
        }
        String key = context.artifact().getArtifactStorageKey() + "#" + ability.id() + "#" + context.source();
        Long prev = antiSpamBySourceKey.get(key);
        return prev != null && (now - prev) < Math.max(150L, (long) (profile.cooldownFloorMs() * 0.5D));
    }

    private boolean isAntiSpamBlocked(AbilityEventContext context, AbilityDefinition ability, TriggerBudgetProfile profile, long now) {
        String key = context.artifact().getArtifactStorageKey() + "#" + ability.id() + "#" + context.source();
        Long prev = antiSpamBySourceKey.get(key);
        long cooldownFloor = (long) profile.cooldownFloorMs();
        return prev != null && cooldownFloor > 0 && (now - prev) < cooldownFloor;
    }

    private double policyAdjustedTriggerCost(TriggerBudgetProfile profile, double base) {
        return switch (profile.policy()) {
            case STRICT -> base * 1.15D;
            case BURSTY -> base * 0.95D;
            case ACTIVE_INTENTIONAL, CRITICAL_SYSTEM -> base * 0.85D;
            case PASSIVE_LOW_PRIORITY -> base * 1.25D;
            default -> base;
        };
    }

    private double policyAdjustedEvaluationCost(TriggerBudgetProfile profile, double base) {
        return profile.policy() == TriggerBudgetPolicy.PASSIVE_LOW_PRIORITY ? base * 1.2D : base;
    }

    private BudgetPool holderPool(UUID holderId) {
        return holderPools.computeIfAbsent(holderId, ignored -> new BudgetPool(HOLDER_CAPACITY, HOLDER_REFILL));
    }

    private BudgetPool artifactPool(String artifactKey) {
        return artifactPools.computeIfAbsent(artifactKey, ignored -> new BudgetPool(ARTIFACT_CAPACITY, ARTIFACT_REFILL));
    }

    private BudgetPool triggerPool(String triggerKey) {
        return triggerPools.computeIfAbsent(triggerKey, ignored -> new BudgetPool(TRIGGER_CAPACITY, TRIGGER_REFILL));
    }

    private BudgetPool abilityPool(String abilityKey) {
        return abilityPools.computeIfAbsent(abilityKey, ignored -> new BudgetPool(8.0D, 3.8D));
    }

    private Map<String, Long> snapshot(Map<String, LongAdder> source) {
        Map<String, Long> out = new HashMap<>();
        source.forEach((k, v) -> out.put(k, v.sum()));
        return Map.copyOf(out);
    }

    private static final class BudgetPool {
        private final double capacity;
        private final double refillPerSecond;
        private double available;
        private long lastRefillMs;
        private long burstWindowStart;
        private int burstCount;

        private BudgetPool(double capacity, double refillPerSecond) {
            this.capacity = capacity;
            this.refillPerSecond = refillPerSecond;
            this.available = capacity;
            this.lastRefillMs = System.currentTimeMillis();
            this.burstWindowStart = this.lastRefillMs;
        }

        synchronized boolean canSpend(double cost, long now) {
            refill(now);
            return available >= cost;
        }

        synchronized void consume(double cost, long now) {
            refill(now);
            available = Math.max(0.0D, available - cost);
        }

        synchronized boolean withinBurst(int burstLimit, long burstWindowMs, long now) {
            if (now - burstWindowStart > burstWindowMs) {
                burstWindowStart = now;
                burstCount = 0;
            }
            burstCount++;
            return burstCount <= Math.max(1, burstLimit);
        }

        synchronized double pressure(long now) {
            refill(now);
            return 1.0D - (available / Math.max(0.0001D, capacity));
        }

        synchronized String debug(long now) {
            refill(now);
            return String.format(java.util.Locale.ROOT, "%.2f/%.2f", available, capacity);
        }

        private void refill(long now) {
            if (now <= lastRefillMs) {
                return;
            }
            long elapsed = now - lastRefillMs;
            available = Math.min(capacity, available + ((elapsed / 1000.0D) * refillPerSecond));
            lastRefillMs = now;
        }
    }
}
