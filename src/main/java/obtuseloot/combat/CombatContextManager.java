package obtuseloot.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatContextManager {
    private final Map<UUID, CombatContext> contexts = new ConcurrentHashMap<>();

    public CombatContext get(UUID playerId) { return contexts.computeIfAbsent(playerId, id -> new CombatContext()); }
    public void remove(UUID playerId) { contexts.remove(playerId); }
    public Map<UUID, CombatContext> getLoadedContexts() { return contexts; }

    public void cleanupStaleContexts(long maxAgeMs) {
        long now = System.currentTimeMillis();
        contexts.entrySet().removeIf(e -> now - e.getValue().getLastCombatTimestamp() > maxAgeMs);
    }
}
