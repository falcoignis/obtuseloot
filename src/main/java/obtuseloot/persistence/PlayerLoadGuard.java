package obtuseloot.persistence;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps per-player storage loads so a failed read is never mistaken for "no record".
 *
 * <p>After a failure the player's load is refused, without touching storage, until a backoff
 * elapses; then the next access retries. Callers that receive {@link PlayerStateLoadException}
 * must not create or save state for that player.
 */
public final class PlayerLoadGuard {
    public static final long DEFAULT_BACKOFF_MS = 30_000L;

    private final String stateName;
    private final LongSupplier clock;
    private final long backoffMs;
    private final Supplier<Logger> logger;
    private final Map<UUID, Failure> failures = new ConcurrentHashMap<>();
    private final Set<UUID> recovered = ConcurrentHashMap.newKeySet();

    public PlayerLoadGuard(String stateName, LongSupplier clock, long backoffMs, Supplier<Logger> logger) {
        this.stateName = stateName;
        this.clock = clock;
        this.backoffMs = Math.max(0L, backoffMs);
        this.logger = logger;
    }

    /** Returns the stored value, or null when there is genuinely no record. */
    public <T> T load(UUID playerId, Function<UUID, T> loader) {
        Failure failure = failures.get(playerId);
        long now = clock.getAsLong();
        if (failure != null && now < failure.retryAtMs()) {
            throw new PlayerStateLoadException(playerId,
                    stateName + " for " + playerId + " is unavailable; next load attempt after backoff", failure.cause());
        }
        try {
            T loaded = loader.apply(playerId);
            if (failures.remove(playerId) != null) {
                recovered.add(playerId);
                logger.get().info("[Persistence] " + stateName + " for " + playerId + " loaded after "
                        + failure.attempts() + " failed attempt(s).");
            }
            return loaded;
        } catch (RuntimeException ex) {
            int attempts = failure == null ? 1 : failure.attempts() + 1;
            failures.put(playerId, new Failure(now + backoffMs, attempts, ex));
            if (attempts == 1) {
                logger.get().log(Level.SEVERE, "[Persistence] Could not load " + stateName + " for " + playerId
                        + ". Refusing to create or save it until a load succeeds; retrying every "
                        + (backoffMs / 1000L) + "s.", ex);
            } else {
                logger.get().warning("[Persistence] " + stateName + " for " + playerId + " still unavailable (attempt "
                        + attempts + "): " + ex.getMessage());
            }
            if (ex instanceof PlayerStateLoadException loadException) {
                throw loadException;
            }
            throw new PlayerStateLoadException(playerId, "Could not load " + stateName + " for " + playerId + ": " + ex.getMessage(), ex);
        }
    }

    /** True once per recovery: the first successful load after one or more failures. */
    public boolean consumeRecovered(UUID playerId) {
        return recovered.remove(playerId);
    }

    public boolean failing(UUID playerId) {
        return failures.containsKey(playerId);
    }

    public void forget(UUID playerId) {
        failures.remove(playerId);
        recovered.remove(playerId);
    }

    private record Failure(long retryAtMs, int attempts, RuntimeException cause) {
    }
}
