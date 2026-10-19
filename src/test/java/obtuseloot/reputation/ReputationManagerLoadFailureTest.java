package obtuseloot.reputation;

import obtuseloot.artifacts.Artifact;
import obtuseloot.persistence.PlayerStateLoadException;
import obtuseloot.persistence.PlayerStateStore;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class ReputationManagerLoadFailureTest {
    private static final long BACKOFF_MS = 1_000L;

    @Test
    void unreadableStoredReputationIsNeverReplacedOrSavedOverAndLoadsAfterBackoff() {
        UUID playerId = UUID.randomUUID();
        FlakyStore store = new FlakyStore();
        ArtifactReputation stored = new ArtifactReputation();
        stored.setKills(42);
        store.reputations.put(playerId, stored);
        store.failLoads = true;
        AtomicLong clock = new AtomicLong(5_000L);
        ReputationManager manager = new ReputationManager(store, clock::get, BACKOFF_MS);

        assertThrows(PlayerStateLoadException.class, () -> manager.get(playerId));
        assertNull(manager.tryGet(playerId));
        assertEquals(1, store.loadCalls.get());
        manager.saveAll();
        assertEquals(0, store.saveCalls.get());

        store.failLoads = false;
        clock.addAndGet(BACKOFF_MS);
        assertSame(stored, manager.get(playerId));
        assertEquals(42, manager.get(playerId).getKills());
    }

    @Test
    void unloadKeepsReputationInMemoryWhenItsSaveFails() {
        UUID playerId = UUID.randomUUID();
        FlakyStore store = new FlakyStore();
        ReputationManager manager = new ReputationManager(store, () -> 0L, BACKOFF_MS);
        ArtifactReputation reputation = manager.get(playerId);
        reputation.setKills(3);

        store.failSaves = true;
        manager.unload(playerId);
        assertSame(reputation, manager.getLoadedReputations().get(playerId));

        store.failSaves = false;
        manager.saveAll();
        assertSame(reputation, store.reputations.get(playerId));
    }

    private static final class FlakyStore implements PlayerStateStore {
        private final Map<UUID, ArtifactReputation> reputations = new HashMap<>();
        private final AtomicInteger loadCalls = new AtomicInteger();
        private final AtomicInteger saveCalls = new AtomicInteger();
        private volatile boolean failLoads;
        private volatile boolean failSaves;

        @Override
        public void saveArtifact(UUID playerId, Artifact artifact) {
        }

        @Override
        public Artifact loadArtifact(UUID playerId) {
            return null;
        }

        @Override
        public void saveReputation(UUID playerId, ArtifactReputation reputation) {
            saveCalls.incrementAndGet();
            if (failSaves) {
                throw new IllegalStateException("simulated save outage");
            }
            reputations.put(playerId, reputation);
        }

        @Override
        public ArtifactReputation loadReputation(UUID playerId) {
            loadCalls.incrementAndGet();
            if (failLoads) {
                throw new IllegalStateException("simulated load outage");
            }
            return reputations.get(playerId);
        }
    }
}
