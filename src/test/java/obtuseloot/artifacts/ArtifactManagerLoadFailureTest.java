package obtuseloot.artifacts;

import obtuseloot.persistence.PlayerStateLoadException;
import obtuseloot.persistence.PlayerStateStore;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class ArtifactManagerLoadFailureTest {
    private static final long BACKOFF_MS = 1_000L;

    @Test
    void uncachedUnloadPersistsEvolutionHistoryAndRetainsItAfterFailedSave() throws Exception {
        var previous = obtuseloot.config.RuntimeSettings.get();
        try {
            var config = new org.bukkit.configuration.file.YamlConfiguration();
            config.set("runtime.activeArtifactCache", false);
            obtuseloot.config.RuntimeSettings.load(config);
            var owner = new UUID(0, 99);
            var store = new FlakyStore();
            var manager = new ArtifactManager(store, () -> 0L, BACKOFF_MS);
            var artifact = manager.getOrCreate(owner);
            artifact.setLastUtilityHistory("test-observed-history");
            store.failSaves = true;
            manager.unload(owner);
            assertSame(artifact, manager.getOrCreate(owner), "Failed unload must not replace unsaved measured history");
            store.failSaves = false;
            manager.unload(owner);
            assertFalse(manager.getLoadedArtifacts().containsKey(owner));
            assertEquals("test-observed-history", store.artifacts.get(owner).getLastUtilityHistory());
        } finally {
            var field = obtuseloot.config.RuntimeSettings.class.getDeclaredField("current");
            field.setAccessible(true);
            field.set(null, previous);
        }
    }

    @Test
    void unreadableStoredArtifactIsNeverReplacedOrSavedOverAndLoadsAfterBackoff() {
        UUID ownerId = UUID.randomUUID();
        FlakyStore store = new FlakyStore();
        Artifact stored = new Artifact(ownerId, "wooden_sword");
        stored.setArtifactSeed(-777L);
        store.artifacts.put(ownerId, stored);
        store.failLoads = true;
        AtomicLong clock = new AtomicLong(10_000L);
        ArtifactManager manager = new ArtifactManager(store, clock::get, BACKOFF_MS);
        AtomicInteger recoveries = new AtomicInteger();
        manager.setRecoveryListener(id -> recoveries.incrementAndGet());

        assertThrows(PlayerStateLoadException.class, () -> manager.getOrCreate(ownerId));
        assertNull(manager.tryGet(ownerId));
        assertEquals(1, store.loadCalls.get(), "Loads during the backoff window must not touch storage");

        assertThrows(PlayerStateLoadException.class, () -> manager.recreate(ownerId),
                "A replacement must abort rather than overwrite the unreadable stored artifact");
        manager.saveAll();
        manager.save(ownerId);
        assertEquals(0, store.saveCalls.get(), "Nothing may be saved for a player whose state failed to load");
        assertTrue(manager.getLoadedArtifacts().isEmpty());

        store.failLoads = false;
        clock.addAndGet(BACKOFF_MS);
        Artifact recovered = manager.getOrCreate(ownerId);

        assertSame(stored, recovered);
        assertEquals(-777L, recovered.getArtifactSeed());
        assertEquals(1, recoveries.get());
        manager.getOrCreate(ownerId);
        assertEquals(1, recoveries.get(), "Recovery is reported once");
    }

    @Test
    void missingRecordStillGeneratesAFreshArtifact() {
        FlakyStore store = new FlakyStore();
        ArtifactManager manager = new ArtifactManager(store, () -> 0L, BACKOFF_MS);

        Artifact artifact = manager.getOrCreate(UUID.randomUUID());

        assertNotNull(artifact);
        assertNotEquals(0L, artifact.getArtifactSeed());
    }

    @Test
    void failedSaveDoesNotThrowAndIsRetriedOnTheNextSave() {
        UUID ownerId = UUID.randomUUID();
        FlakyStore store = new FlakyStore();
        ArtifactManager manager = new ArtifactManager(store, () -> 0L, BACKOFF_MS);
        Artifact artifact = manager.getOrCreate(ownerId);
        manager.markDirty(ownerId);

        store.failSaves = true;
        assertDoesNotThrow(manager::saveAll);
        assertFalse(store.artifacts.containsKey(ownerId));

        store.failSaves = false;
        manager.saveAll();
        assertSame(artifact, store.artifacts.get(ownerId));
    }

    @Test
    void unloadAfterFailedSaveKeepsUnsavedArtifactForTheNextLoad() {
        UUID ownerId = UUID.randomUUID();
        FlakyStore store = new FlakyStore();
        ArtifactManager manager = new ArtifactManager(store, () -> 0L, BACKOFF_MS);
        if (!manager.cacheStats().enabled()) {
            return; // this guarantee comes from the active artifact cache
        }
        Artifact artifact = manager.getOrCreate(ownerId);
        manager.markDirty(ownerId);

        store.failSaves = true;
        manager.unload(ownerId);
        int loadsBefore = store.loadCalls.get();

        assertSame(artifact, manager.getOrCreate(ownerId), "Rejoin must see the unsaved artifact, not reload stale storage");
        assertEquals(loadsBefore, store.loadCalls.get());
    }

    @Test
    void perItemLoadFailureUsesBackoffWithoutCreatingAPlayerSubstitute() {
        UUID owner = new UUID(0, 90);
        var artifact = new Artifact(owner, "wooden_sword");
        artifact.setArtifactStorageKey("artifact:" + new UUID(0, 1));
        artifact.setArtifactSeed(-777);
        var clock = new AtomicLong();
        var calls = new AtomicInteger();
        var fail = new java.util.concurrent.atomic.AtomicBoolean(true);
        PlayerStateStore store = new PlayerStateStore() {
            public Artifact loadArtifactProfile(String key) {
                calls.incrementAndGet();
                if (fail.get()) throw new IllegalStateException("test read failure");
                return artifact;
            }
            public void saveArtifact(UUID id, Artifact item) { throw new AssertionError("No substitute may be saved"); }
            public Artifact loadArtifact(UUID id) { throw new AssertionError("No player fallback may be loaded"); }
            public void saveReputation(UUID id, ArtifactReputation rep) { throw new AssertionError(); }
            public ArtifactReputation loadReputation(UUID id) { throw new AssertionError(); }
        };
        var manager = new ArtifactManager(store, clock::get, BACKOFF_MS);
        assertThrows(PlayerStateLoadException.class, () -> manager.resolveByStorageKey(artifact.getArtifactStorageKey()));
        assertThrows(PlayerStateLoadException.class, () -> manager.resolveByStorageKey(artifact.getArtifactStorageKey()));
        assertEquals(1, calls.get());
        assertTrue(manager.getLoadedArtifacts().isEmpty());
        clock.addAndGet(BACKOFF_MS); fail.set(false);
        assertSame(artifact, manager.resolveByStorageKey(artifact.getArtifactStorageKey()));
        assertEquals(2, calls.get());
    }

    private static final class FlakyStore implements PlayerStateStore {
        private final Map<UUID, Artifact> artifacts = new HashMap<>();
        private final Map<UUID, ArtifactReputation> reputations = new HashMap<>();
        private final AtomicInteger loadCalls = new AtomicInteger();
        private final AtomicInteger saveCalls = new AtomicInteger();
        private volatile boolean failLoads;
        private volatile boolean failSaves;

        @Override
        public void saveArtifact(UUID playerId, Artifact artifact) {
            saveCalls.incrementAndGet();
            if (failSaves) {
                throw new IllegalStateException("simulated save outage");
            }
            artifacts.put(playerId, artifact);
        }

        @Override
        public Artifact loadArtifact(UUID playerId) {
            loadCalls.incrementAndGet();
            if (failLoads) {
                throw new IllegalStateException("simulated load outage");
            }
            return artifacts.get(playerId);
        }

        @Override
        public void saveReputation(UUID playerId, ArtifactReputation reputation) {
            reputations.put(playerId, reputation);
        }

        @Override
        public ArtifactReputation loadReputation(UUID playerId) {
            return reputations.get(playerId);
        }
    }
}
