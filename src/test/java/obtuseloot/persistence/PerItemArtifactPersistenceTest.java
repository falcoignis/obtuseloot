package obtuseloot.persistence;

import obtuseloot.abilities.AbilityFamily;
import obtuseloot.abilities.genome.*;
import obtuseloot.artifacts.*;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class PerItemArtifactPersistenceTest {
    @TempDir Path temp;
    private final UUID owner = new UUID(0, 90);

    @Test void sqliteKeepsTwoItemsAndTradeHistoryIndependent() throws Exception {
        var database = new DatabaseManager("jdbc:sqlite:" + temp.resolve("items.db"), null, null);
        try (var connection = database.openConnection()) { new SqlSchemaManager().ensureSchema(connection); }
        roundTrip(new JdbcPlayerStateStore(database, null, JdbcPlayerStateStore.Dialect.SQLITE));
    }

    @Test void yamlKeepsTwoItemsAndTradeHistoryIndependent() {
        var store = new YamlPlayerStateStore(plugin());
        roundTrip(store);
        store.flushPendingWrites();
    }

    @Test void corruptProfileIsRefusedWithoutGeneratingASubstitute() {
        var artifact = item(1, -12);
        String encoded = ArtifactProfileCodec.encode(artifact);
        assertThrows(PlayerStateLoadException.class, () -> ArtifactProfileCodec.decode(artifact.getArtifactStorageKey(),
                encoded.replace("profile-version: 4", "profile-version: 90")));
        assertThrows(PlayerStateLoadException.class, () -> ArtifactProfileCodec.decode(artifact.getArtifactStorageKey(),
                encoded.replace("SURVIVAL_INSTINCT: 0.23", "SURVIVAL_INSTINCT: .NaN")));
        assertThrows(IllegalArgumentException.class, () -> ArtifactStorageKeys.itemId("artifact:../../unsafe"));
        assertThrows(IllegalArgumentException.class, () -> ArtifactStorageKeys.itemId("artifact:00000000000000000000000000000001"));
    }

    @Test void inactiveCacheIsBoundedAndFailedSavesCannotEvictUnsavedHistory() throws Exception {
        var previous = obtuseloot.config.RuntimeSettings.get();
        try {
            var config = new org.bukkit.configuration.file.YamlConfiguration();
            config.set("runtime.activeArtifactCacheMaxEntries", 16);
            obtuseloot.config.RuntimeSettings.load(config);
            var database = new DatabaseManager("jdbc:sqlite:" + temp.resolve("cache.db"), null, null);
            try (var connection = database.openConnection()) { new SqlSchemaManager().ensureSchema(connection); }
            var delegate = new JdbcPlayerStateStore(database, null, JdbcPlayerStateStore.Dialect.SQLITE);
            var refuseFirst = new java.util.concurrent.atomic.AtomicBoolean();
            String firstKey = item(1, 1).getArtifactStorageKey();
            var store = new PlayerStateStore() {
                public void saveArtifact(UUID id, Artifact artifact) { delegate.saveArtifact(id, artifact); }
                public Artifact loadArtifact(UUID id) { return delegate.loadArtifact(id); }
                public void saveReputation(UUID id, obtuseloot.reputation.ArtifactReputation rep) { delegate.saveReputation(id, rep); }
                public obtuseloot.reputation.ArtifactReputation loadReputation(UUID id) { return delegate.loadReputation(id); }
                public Artifact loadArtifactProfile(String key) { return delegate.loadArtifactProfile(key); }
                public void saveArtifactProfile(Artifact artifact) {
                    if (firstKey.equals(artifact.getArtifactStorageKey()) && refuseFirst.get()) throw new IllegalStateException("test write failure");
                    delegate.saveArtifactProfile(artifact);
                }
            };
            var manager = new ArtifactManager(store);
            var first = item(1, 1);
            manager.registerItem(first);
            first.setLastUtilityHistory("unsaved-observed-history");
            first.setEvolutionPath("strider-formed");
            new obtuseloot.evolution.GenomeEvolutionEngine().advance(first);
            refuseFirst.set(true);
            assertThrows(IllegalStateException.class, () -> manager.saveItem(first));
            for (int i = 2; i < 40; i++) manager.registerItem(item(i, i));
            assertTrue(manager.cachedItemProfiles() <= 16);
            assertSame(first, manager.resolveByStorageKey(firstKey), "Dirty state must remain in the retry buffer");
            refuseFirst.set(false); manager.saveAll();
            assertEquals("unsaved-observed-history", delegate.loadArtifactProfile(firstKey).getLastUtilityHistory());
            var saved = delegate.loadArtifactProfile(firstKey);
            assertEquals(first.getGenome().traits(), saved.getGenome().traits());
            assertEquals(first.getGenomeRevision(), saved.getGenomeRevision());
            assertFalse(new obtuseloot.evolution.GenomeEvolutionEngine().advance(saved));
            assertEquals(39, countProfiles(database), "Memory eviction must never delete persisted items");
        } finally {
            var field = obtuseloot.config.RuntimeSettings.class.getDeclaredField("current"); field.setAccessible(true); field.set(null, previous);
        }
    }

    private int countProfiles(DatabaseManager database) throws Exception {
        try (var connection = database.openConnection(); var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*) FROM artifact_profiles")) {
            assertTrue(result.next()); return result.getInt(1);
        }
    }

    private void roundTrip(PlayerStateStore store) {
        var first = item(1, -12);
        var second = item(2, Long.MIN_VALUE);
        first.getItemReputation().recordUtilityExperience(AbilityFamily.SURVIVAL, 1.75);
        first.addLoreHistory("A paid harvest");
        first.setParentStorageKey("artifact:" + new UUID(0, 8));
        first.setParentGenomeRevision(2);
        first.setParentGenomeSeed(-123L);
        first.setInheritanceGeneration(4);
        first.setEvolutionPath("strider-formed");
        new obtuseloot.evolution.GenomeEvolutionEngine().advance(first);
        var manager = new ArtifactManager(store);
        manager.registerItem(first);
        manager.registerItem(second);
        var reloaded = store.loadArtifactProfile(first.getArtifactStorageKey());
        assertEquals(-12, reloaded.getArtifactSeed());
        assertEquals(first.getGenome().traits(), reloaded.getGenome().traits());
        assertEquals(first.getItemReputation().encodeUtilityExperience(), reloaded.getItemReputation().encodeUtilityExperience());
        assertEquals(1, reloaded.getItemReputation().getSurvival());
        assertEquals(0, store.loadArtifactProfile(second.getArtifactStorageKey()).getItemReputation().getTotalScore());
        UUID buyer = new UUID(0, 91);
        var freshManager = new ArtifactManager(store);
        var traded = freshManager.resolveByStorageKey(first.getArtifactStorageKey());
        freshManager.activateItem(buyer, traded);
        freshManager.saveAll();
        traded = store.loadArtifactProfile(first.getArtifactStorageKey());
        assertEquals(buyer, traded.getOwnerId());
        assertEquals(first.getArtifactSeed(), traded.getArtifactSeed());
        assertEquals(first.getParentStorageKey(), traded.getParentStorageKey());
        assertEquals(4, traded.getInheritanceGeneration());
        assertTrue(traded.getLoreHistory().contains("A paid harvest"));
        assertEquals(first.getGenome().traits(), traded.getGenome().traits());
        assertEquals(first.getGenome().latentTraits(), traded.getGenome().latentTraits());
        assertEquals(first.getGenome().activatedLatentTraits(), traded.getGenome().activatedLatentTraits());
        assertEquals(first.getGenomeRevision(), traded.getGenomeRevision());
        assertEquals(first.getGenomeEvolutionState(), traded.getGenomeEvolutionState());
        assertFalse(new obtuseloot.evolution.GenomeEvolutionEngine().advance(traded), "A trade/restart cannot replay evolution");
        freshManager.activateItem(buyer, traded);
        traded.setArchetypePath("strider");
        var transition = new obtuseloot.awakening.AwakeningEngine().forceAwakening(null, traded, traded.getItemReputation());
        assertNotNull(transition);
        var transitionedGenome = transition.replacement().getGenome();
        var replacement = freshManager.replaceIdentity(buyer, transition);
        assertSame(transitionedGenome, replacement.getGenome(), "Manager must not overwrite the newly evolved genome");
        freshManager.saveAll();
        var persistedReplacement = store.loadArtifactProfile(replacement.getArtifactStorageKey());
        assertEquals(transitionedGenome.traits(), persistedReplacement.getGenome().traits());
        assertEquals(replacement.getGenomeRevision(), persistedReplacement.getGenomeRevision());
        assertThrows(IllegalStateException.class, () -> freshManager.replaceIdentity(buyer, transition));
        assertSame(replacement, freshManager.getLoadedArtifacts().get(buyer), "Rejected identity replay cannot change loaded state");
        assertEquals(owner, store.loadArtifactProfile(second.getArtifactStorageKey()).getOwnerId());
        assertThrows(PlayerStateLoadException.class, () -> freshManager.resolveByStorageKey("artifact:" + new UUID(0, 99)));
    }

    private Artifact item(long id, long seed) {
        var artifact = new Artifact(owner, "wooden_sword");
        artifact.setArtifactStorageKey("artifact:" + new UUID(0, id));
        artifact.setArtifactSeed(seed);
        var traits = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
        for (var trait : GenomeTrait.values()) traits.put(trait, 0.23);
        artifact.setGenome(new ArtifactGenome(seed, traits, traits, Set.of(GenomeTrait.RESONANCE)));
        return artifact;
    }

    private Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getDataFolder" -> temp.toFile();
            case "getLogger" -> Logger.getLogger("PerItemArtifactPersistenceTest");
            default -> null;
        });
    }
}
