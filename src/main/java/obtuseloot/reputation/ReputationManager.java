package obtuseloot.reputation;

import obtuseloot.ObtuseLoot;
import obtuseloot.persistence.PlayerLoadGuard;
import obtuseloot.persistence.PlayerStateLoadException;
import obtuseloot.persistence.PlayerStateStore;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReputationManager {
    private final PlayerStateStore stateStore;
    private final Map<UUID, obtuseloot.artifacts.Artifact> itemBindings = new ConcurrentHashMap<>();
    private final Map<UUID, ArtifactReputation> loadedReputations = new ConcurrentHashMap<>();
    private final PlayerLoadGuard loadGuard;
    private volatile Consumer<UUID> recoveryListener = id -> { };

    public ReputationManager(PlayerStateStore stateStore) {
        this(stateStore, System::currentTimeMillis, PlayerLoadGuard.DEFAULT_BACKOFF_MS);
    }

    ReputationManager(PlayerStateStore stateStore, LongSupplier clock, long loadRetryBackoffMs) {
        this.stateStore = stateStore;
        this.loadGuard = new PlayerLoadGuard("reputation", clock, loadRetryBackoffMs, ReputationManager::logger);
    }

    /**
     * Returns the player's reputation, starting a fresh one only when storage has no record.
     *
     * @throws PlayerStateLoadException if stored reputation exists but cannot be read; nothing is cached for the player.
     */
    public ArtifactReputation get(UUID playerId) {
        ArtifactReputation reputation = loadedReputations.computeIfAbsent(playerId, id -> {
            ArtifactReputation loaded = loadGuard.load(id, stateStore::loadReputation);
            return loaded != null ? loaded : new ArtifactReputation();
        });
        if (loadGuard.consumeRecovered(playerId)) {
            recoveryListener.accept(playerId);
        }
        return reputation;
    }

    /** Like {@link #get(UUID)}, but returns null instead of throwing while the player's stored reputation is unreadable. */
    public ArtifactReputation tryGet(UUID playerId) {
        try {
            return get(playerId);
        } catch (PlayerStateLoadException ex) {
            return null;
        }
    }

    /** Called with the player id when a previously failing load succeeds, so deferred join work can run. */
    public void setRecoveryListener(Consumer<UUID> listener) {
        this.recoveryListener = listener == null ? id -> { } : listener;
    }

    public void bindItem(UUID holder, obtuseloot.artifacts.Artifact artifact) {
        itemBindings.put(holder, artifact);
        loadedReputations.put(holder, artifact.getItemReputation());
    }

    public void unbindItem(UUID holder) {
        if (itemBindings.remove(holder) != null) loadedReputations.remove(holder);
    }

    /** Returns true if the reputation was written (or there was nothing loaded to write). */
    public boolean save(UUID playerId) {
        ArtifactReputation rep = loadedReputations.get(playerId);
        if (rep == null) {
            return true;
        }
        try {
            var item = itemBindings.get(playerId);
            if (item != null) stateStore.saveArtifactProfile(item);
            else stateStore.saveReputation(playerId, rep);
            return true;
        } catch (RuntimeException ex) {
            logger().log(Level.SEVERE, "[Persistence] Failed to save reputation for " + playerId + "; will retry on next save.", ex);
            return false;
        }
    }

    public void saveAll() {
        loadedReputations.keySet().forEach(this::save);
        stateStore.retryFailedWrites();
    }

    public void unload(UUID playerId) {
        loadGuard.forget(playerId);
        // Keep unsaved reputation in memory so the next save cycle retries it instead of dropping it.
        if (save(playerId)) {
            loadedReputations.remove(playerId);
            itemBindings.remove(playerId);
        }
    }

    public Map<UUID, ArtifactReputation> getLoadedReputations() {
        return loadedReputations;
    }

    public ArtifactReputation reset(UUID playerId) {
        loadGuard.forget(playerId);
        ArtifactReputation fresh = new ArtifactReputation();
        loadedReputations.put(playerId, fresh);
        var item = itemBindings.get(playerId);
        if (item != null) item.setItemReputation(fresh);
        return fresh;
    }

    private static Logger logger() {
        return ObtuseLoot.get() != null ? ObtuseLoot.get().getLogger() : Logger.getLogger("ObtuseLoot");
    }
}
