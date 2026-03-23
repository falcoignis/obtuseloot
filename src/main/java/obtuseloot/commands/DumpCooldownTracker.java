package obtuseloot.commands;

/**
 * Tracks the last execution time of the {@code /ol ecosystem dump} command and
 * enforces a minimum cooldown between invocations.
 *
 * <p>All state is held in a single {@code long} field — no threads, no randomness,
 * no Bukkit dependencies.  The clock source is injected so tests can control time.
 */
public final class DumpCooldownTracker {

    /** Functional interface for a clock source (injectable for testing). */
    @FunctionalInterface
    public interface Clock {
        long currentTimeMs();
    }

    private final long cooldownMs;
    private final Clock clock;
    /** Sentinel that ensures the first call always succeeds regardless of clock value. */
    private long lastDumpTimeMs = Long.MIN_VALUE / 2;

    public DumpCooldownTracker(long cooldownMs, Clock clock) {
        this.cooldownMs = cooldownMs;
        this.clock = clock;
    }

    /**
     * Attempt to execute a dump.
     *
     * @return {@code true} if the cooldown has elapsed and the dump should proceed
     *         (also records the current time); {@code false} if still on cooldown.
     */
    public boolean tryExecute() {
        long now = clock.currentTimeMs();
        if (now - lastDumpTimeMs < cooldownMs) {
            return false;
        }
        lastDumpTimeMs = now;
        return true;
    }

    /** Milliseconds remaining until the cooldown expires, or 0 if already elapsed. */
    public long remainingMs() {
        long elapsed = clock.currentTimeMs() - lastDumpTimeMs;
        return Math.max(0L, cooldownMs - elapsed);
    }
}
