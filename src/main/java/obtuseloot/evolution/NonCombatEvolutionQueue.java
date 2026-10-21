package obtuseloot.evolution;

import obtuseloot.abilities.AbilityOutcomeObservation;
import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Main-thread, game-tick windows. No ability IDs, wall-clock deadlines, idle rerolls or kill hooks. */
public final class NonCombatEvolutionQueue {
    private final Map<WindowKey, Pending> pending = new LinkedHashMap<>();
    private long ticksInWindow;

    public void observe(AbilityOutcomeObservation observation) {
        if (!observation.liveMeasured()) return;
        Artifact artifact = observation.context().artifact();
        ArtifactReputation reputation = observation.context().reputation();
        UUID owner = artifact.getOwnerId();
        if (owner == null || artifact.getArtifactSeed() == 0 || reputation == null) return;
        WindowKey key = new WindowKey(owner, artifact.getArtifactStorageKey());
        Pending update = pending.get(key);
        if (update == null || !update.matches(artifact, reputation)) {
            update = new Pending(artifact, reputation);
            pending.put(key, update);
        }
        // Record once at observation time; queued retries never replay reputation gains.
        reputation.recordUtilityExperience(observation.definition().family(), observation.utilityDelta());
    }

    public void tick(long intervalTicks, Consumer<Pending> processor,
                     BiConsumer<UUID, RuntimeException> failureHandler) {
        if (intervalTicks <= 0) throw new IllegalArgumentException("Evolution interval must be positive");
        if (++ticksInWindow >= intervalTicks) {
            ticksInWindow = 0;
            flushAll(processor, failureHandler);
        }
    }

    public void flushAll(Consumer<Pending> processor, BiConsumer<UUID, RuntimeException> failureHandler) {
        for (WindowKey key : new ArrayList<>(pending.keySet())) flush(key, processor, failureHandler);
    }

    public void flush(UUID owner, Consumer<Pending> processor, BiConsumer<UUID, RuntimeException> failureHandler) {
        for (WindowKey key : new ArrayList<>(pending.keySet())) if (key.owner.equals(owner)) flush(key, processor, failureHandler);
    }

    public void flush(Artifact artifact, Consumer<Pending> processor, BiConsumer<UUID, RuntimeException> failureHandler) {
        flush(new WindowKey(artifact.getOwnerId(), artifact.getArtifactStorageKey()), processor, failureHandler);
    }

    private void flush(WindowKey key, Consumer<Pending> processor, BiConsumer<UUID, RuntimeException> failureHandler) {
        Pending update = pending.get(key);
        if (update == null) return;
        try {
            processor.accept(update);
            pending.remove(key, update);
        } catch (RuntimeException exception) {
            // Keep the update for the next window. Already recorded reputation credit is not replayed.
            failureHandler.accept(key.owner, exception);
        }
    }

    public void forget(UUID owner) { pending.keySet().removeIf(key -> key.owner.equals(owner)); }
    private record WindowKey(UUID owner, String itemKey) { }
    public int pendingCount() { return pending.size(); }

    public static final class Pending {
        private final Artifact artifact;
        private final ArtifactReputation reputation;
        private final UUID owner;
        private final long seed;
        private final String storageKey;

        private Pending(Artifact artifact, ArtifactReputation reputation) {
            this.artifact = artifact;
            this.reputation = reputation;
            this.owner = artifact.getOwnerId();
            this.seed = artifact.getArtifactSeed();
            this.storageKey = artifact.getArtifactStorageKey();
        }

        public Artifact artifact() { return artifact; }
        public ArtifactReputation reputation() { return reputation; }
        public UUID owner() { return owner; }

        public boolean matches(Artifact current, ArtifactReputation currentReputation) {
            return artifact == current && reputation == currentReputation && current != null
                    && seed == current.getArtifactSeed() && Objects.equals(owner, current.getOwnerId())
                    && Objects.equals(storageKey, current.getArtifactStorageKey());
        }

    }
}
