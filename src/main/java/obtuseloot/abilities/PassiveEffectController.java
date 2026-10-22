package obtuseloot.abilities;

import java.util.*;
import java.util.function.Consumer;

/** Main-thread, strongest contributor per declared group. Failed removals remain owned and retryable. */
public final class PassiveEffectController {
    public record Request(String itemKey, String abilityId, PassiveAbilityEffect.Contribution contribution) { }
    private record Active(Request request, PassiveAbilityEffect.Lease lease) { }
    private final Map<UUID, Map<String, Active>> byPlayer = new HashMap<>();
    private final Consumer<RuntimeException> failure;
    public PassiveEffectController(Consumer<RuntimeException> failure) { this.failure = failure; }
    public void reconcile(UUID player, List<Request> requests) {
        Map<String, Request> desired = new LinkedHashMap<>();
        for (Request request : requests) {
            String group = request.contribution.stackingGroup();
            if (request.contribution.stacking() == PassiveAbilityEffect.Stacking.ADDITIVE) group += ":" + request.itemKey + "@" + request.abilityId;
            Request previous = desired.get(group);
            if (previous == null || request.contribution.strength() > previous.contribution.strength()) desired.put(group, request);
        }
        Map<String, Active> active = byPlayer.computeIfAbsent(player, ignored -> new LinkedHashMap<>());
        for (String group : new ArrayList<>(active.keySet())) {
            Active existing = active.get(group);
            if (same(existing.request, desired.get(group))) continue;
            try { existing.lease.release(); active.remove(group); }
            catch (RuntimeException exception) { failure.accept(exception); }
        }
        for (var entry : desired.entrySet()) {
            if (active.containsKey(entry.getKey())) continue;
            try {
                var lease = Objects.requireNonNull(entry.getValue().contribution.apply().get());
                active.put(entry.getKey(), new Active(entry.getValue(), lease));
            } catch (RuntimeException exception) { failure.accept(exception); }
        }
        if (active.isEmpty()) byPlayer.remove(player);
    }
    private boolean same(Request a, Request b) {
        return b != null && a.itemKey.equals(b.itemKey) && a.abilityId.equals(b.abilityId)
                && a.contribution.strength() == b.contribution.strength()
                && a.contribution.revision().equals(b.contribution.revision());
    }
    public Set<String> activeAbilities(UUID player) {
        Set<String> result = new HashSet<>();
        for (Active entry : byPlayer.getOrDefault(player, Map.of()).values()) result.add(entry.request.itemKey + "@" + entry.request.abilityId);
        return Set.copyOf(result);
    }
    public void clear(UUID player) { reconcile(player, List.of()); }
    public void clearAll() { for (UUID player : new ArrayList<>(byPlayer.keySet())) clear(player); }
}
