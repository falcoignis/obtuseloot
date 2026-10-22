package obtuseloot.ecology;

import obtuseloot.abilities.genome.*;
import obtuseloot.artifacts.*;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.persistence.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LocalEcosystemIntegrationTest {
    @TempDir Path temp;
    private final UUID owner = new UUID(0, 90);
    private final WorldPosition origin = new WorldPosition(new UUID(0, 10), 0, 0);

    @Test void measuredLocalSuccessProducesDistinctInheritedLootAndSurvivesRestart() throws Exception {
        var store = store();
        var manager = new ArtifactManager(store);
        var ecosystem = new LocalEcosystem(store, manager);
        var parent = ecosystem.createEquipment(owner, EquipmentArchetype.WOODEN_SWORD, origin, new Random(1));
        var traits = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
        for (var trait : GenomeTrait.values()) traits.put(trait, 0.23);
        parent.setGenome(new ArtifactGenome(parent.getArtifactSeed(), traits, traits, Set.of(GenomeTrait.RESONANCE)));
        manager.saveItem(parent);
        ecosystem.observe(LocalInheritancePoolTest.observation(parent, origin, true, 1, 1, "local-use"));
        ecosystem.advanceTick(); ecosystem.save();
        var reopenedManager = new ArtifactManager(store);
        var reopened = new LocalEcosystem(store, reopenedManager);
        var child = reopened.createEquipment(owner, EquipmentArchetype.IRON_AXE, origin, new Random(2));
        assertNotEquals(parent.getArtifactStorageKey(), child.getArtifactStorageKey());
        assertNotEquals(parent.getArtifactSeed(), child.getArtifactSeed());
        assertEquals(parent.getArtifactStorageKey(), child.getParentStorageKey());
        assertEquals(parent.getLatentLineage(), child.getLatentLineage());
        assertEquals(1, child.getInheritanceGeneration());
        assertEquals(0, child.getItemReputation().getTotalScore());
        assertNotEquals(parent.getGenome().traits(), child.getGenome().traits());
        for (var trait : GenomeTrait.values()) assertTrue(Math.abs(child.getGenome().trait(trait) - parent.getGenome().trait(trait)) < 0.1);
        assertNotEquals(new GenomeResolver().resolve(child.getArtifactSeed()).traits(), child.getGenome().traits(), "Inherited genes must replace seed-only reconstruction");
        assertEquals(child.getGenome().traits(), store.loadArtifactProfile(child.getArtifactStorageKey()).getGenome().traits());
        assertEquals(parent.getGenome().traits(), store.loadArtifactProfile(parent.getArtifactStorageKey()).getGenome().traits());
        var model = new obtuseloot.abilities.AbilityRegistry().registrationById("mobility.double_jump").performanceModel();
        var childPerformance = model.express(child.getGenome());
        assertNotEquals(model.express(parent.getGenome()), childPerformance, "Ordinary inherited mutation changes physical performance");
        assertEquals(childPerformance, model.express(store.loadArtifactProfile(child.getArtifactStorageKey()).getGenome()));
        var replay = reopened.createEquipment(owner, EquipmentArchetype.IRON_AXE, origin, new Random(2));
        assertEquals(child.getGenome().traits(), replay.getGenome().traits());
        assertEquals(childPerformance, model.express(replay.getGenome()));
    }

    @Test void noNearbySuccessCreatesFounderAndDistantSuccessCannotLeakIn() throws Exception {
        var store = store();
        var ecosystem = new LocalEcosystem(store, new ArtifactManager(store));
        var parent = ecosystem.createEquipment(owner, EquipmentArchetype.IRON_AXE, origin, new Random(1));
        ecosystem.observe(LocalInheritancePoolTest.observation(parent, origin, true, 1, 1, "success"));
        var distant = ecosystem.createEquipment(owner, EquipmentArchetype.IRON_AXE,
                new WorldPosition(origin.worldId(), 321, 0), new Random(2));
        assertNull(distant.getParentStorageKey());
        assertEquals(0, distant.getInheritanceGeneration());
        assertNotEquals(parent.getLatentLineage(), distant.getLatentLineage());
    }

    @Test void missingRecordedParentRefusesBirthInsteadOfInventingAnUnrelatedLineage() throws Exception {
        var store = store();
        var manager = new ArtifactManager(store);
        var ecosystem = new LocalEcosystem(store, manager);
        var parent = ecosystem.createEquipment(owner, EquipmentArchetype.IRON_AXE, origin, new Random(1));
        ecosystem.observe(LocalInheritancePoolTest.observation(parent, origin, true, 1, 1, "success"));
        ecosystem.save();
        var database = database();
        try (var connection = database.openConnection(); var delete = connection.prepareStatement("DELETE FROM artifact_profiles WHERE storage_key=?")) {
            delete.setString(1, parent.getArtifactStorageKey()); delete.executeUpdate();
        }
        var reopened = new LocalEcosystem(store, new ArtifactManager(store));
        assertThrows(IllegalStateException.class, () -> reopened.createEquipment(owner, EquipmentArchetype.IRON_AXE, origin, new Random(2)));
    }

    @Test void switchingMeasuredSuccessSwitchesInheritedLineageAtTheSameLocation() throws Exception {
        var store = store();
        var manager = new ArtifactManager(store);
        var founders = new LocalEcosystem(store, manager);
        var left = founders.createEquipment(owner, EquipmentArchetype.IRON_AXE, origin, new Random(1));
        var right = founders.createEquipment(owner, EquipmentArchetype.IRON_AXE, origin, new Random(2));
        var firstEvidence = new LocalInheritancePool();
        firstEvidence.record(LocalInheritancePoolTest.observation(left, origin, true, 1, 1, "target-a"), 100);
        firstEvidence.record(LocalInheritancePoolTest.observation(right, origin, true, 0, -1, "target-b"), 100);
        var secondEvidence = new LocalInheritancePool();
        secondEvidence.record(LocalInheritancePoolTest.observation(left, origin, true, 0, -1, "target-a"), 100);
        secondEvidence.record(LocalInheritancePoolTest.observation(right, origin, true, 1, 1, "target-b"), 100);
        var firstChild = new LocalEcosystem(store, manager, firstEvidence)
                .createEquipment(owner, EquipmentArchetype.IRON_AXE, origin, new Random(42));
        var secondChild = new LocalEcosystem(store, manager, secondEvidence)
                .createEquipment(owner, EquipmentArchetype.IRON_AXE, origin, new Random(42));
        assertEquals(left.getArtifactStorageKey(), firstChild.getParentStorageKey());
        assertEquals(right.getArtifactStorageKey(), secondChild.getParentStorageKey());
        assertNotEquals(firstChild.getLatentLineage(), secondChild.getLatentLineage());
        assertEquals(0, left.getItemReputation().getKills());
        assertEquals(0, right.getItemReputation().getKills());
    }

    @Test void chunkRadiusReloadControlsActualParentEligibility() throws Exception {
        var previous = RuntimeSettings.get();
        try {
            var store = store();
            var ecosystem = new LocalEcosystem(store, new ArtifactManager(store));
            var parent = ecosystem.createEquipment(owner, EquipmentArchetype.IRON_AXE, origin, new Random(1));
            ecosystem.observe(LocalInheritancePoolTest.observation(parent, origin, true, 1, 1, "success"));
            var config = new YamlConfiguration();
            config.set("ecology.inheritance-radius-chunks", 1);
            RuntimeSettings.load(config);
            var nearby = new WorldPosition(origin.worldId(), 17, 0);
            assertNull(ecosystem.createEquipment(owner, EquipmentArchetype.IRON_AXE, nearby, new Random(2)).getParentStorageKey());
            config.set("ecology.inheritance-radius-chunks", 2); RuntimeSettings.load(config);
            assertEquals(parent.getArtifactStorageKey(), ecosystem.createEquipment(owner, EquipmentArchetype.IRON_AXE, nearby, new Random(2)).getParentStorageKey());
            config.set("ecology.inheritance-radius-chunks", 0);
            assertThrows(IllegalArgumentException.class, () -> RuntimeSettings.load(config));
            assertEquals(2, RuntimeSettings.get().inheritanceRadiusChunks());
        } finally {
            var field = RuntimeSettings.class.getDeclaredField("current"); field.setAccessible(true); field.set(null, previous);
        }
    }

    @Test void evolvingRequiresNewEvidenceAndKeepsOldFitnessAcrossTradeAndRestart() throws Exception {
        var store = store();
        var manager = new ArtifactManager(store);
        var ecosystem = new LocalEcosystem(store, manager);
        var parent = ecosystem.createEquipment(owner, EquipmentArchetype.ELYTRA, origin, new Random(1));
        var old = LocalInheritancePoolTest.observation(parent, origin, true, 1, 1, "old-target");
        var tracker = new obtuseloot.evolution.ArtifactUsageTracker();
        tracker.trackAbilityExecution(parent, old.context(), old.result(), old.definition());
        ecosystem.observe(old);
        parent.setEvolutionPath("strider-formed");
        new obtuseloot.evolution.GenomeEvolutionEngine().advance(parent);
        manager.saveItem(parent);
        assertNull(ecosystem.createEquipment(owner, EquipmentArchetype.ELYTRA, origin, new Random(2)).getParentStorageKey(),
                "Old success cannot qualify new, untested genes");
        var current = LocalInheritancePoolTest.observation(parent, origin, true, 1, 1, "new-target");
        tracker.trackAbilityExecution(parent, current.context(), current.result(), current.definition());
        ecosystem.observe(current);
        manager.activateItem(new UUID(0, 91), parent);
        manager.saveItem(parent); ecosystem.save();
        var reopened = new LocalEcosystem(store, new ArtifactManager(store));
        var child = reopened.createEquipment(owner, EquipmentArchetype.ELYTRA, origin, new Random(3));
        assertEquals(parent.getArtifactStorageKey(), child.getParentStorageKey());
        assertEquals(parent.getGenomeRevision(), child.getParentGenomeRevision());
        assertTrue(child.getGenomeFitnessHistory().revisions().isEmpty(), "Children must earn their own fitness");
        var restored = store.loadArtifactProfile(parent.getArtifactStorageKey());
        assertEquals(new UUID(0, 91), restored.getOwnerId());
        assertEquals(parent.getGenomeFitnessHistory().revisions(), restored.getGenomeFitnessHistory().revisions());
        for (var revision : parent.getGenomeFitnessHistory().revisions())
            assertEquals(parent.getGenomeFitnessHistory().signals(revision), restored.getGenomeFitnessHistory().signals(revision));
    }

    @Test void reseededItemsCannotReusePreviousSeedsReproductiveEvidence() throws Exception {
        var store = store();
        var manager = new ArtifactManager(store);
        var ecosystem = new LocalEcosystem(store, manager);
        var parent = ecosystem.createEquipment(owner, EquipmentArchetype.ELYTRA, origin, new Random(1));
        manager.activateItem(owner, parent);
        ecosystem.observe(LocalInheritancePoolTest.observation(parent, origin, true, 1, 1, "old-seed"));
        var replacement = manager.reseed(owner, -711L);
        assertNull(ecosystem.createEquipment(owner, EquipmentArchetype.ELYTRA, origin, new Random(2)).getParentStorageKey());
        ecosystem.observe(LocalInheritancePoolTest.observation(replacement, origin, true, 1, 1, "new-seed"));
        var child = ecosystem.createEquipment(owner, EquipmentArchetype.ELYTRA, origin, new Random(3));
        assertEquals(replacement.getArtifactStorageKey(), child.getParentStorageKey());
        assertEquals(replacement.getGenomeRevision(), child.getParentGenomeRevision());
        assertEquals(-711L, child.getParentGenomeSeed());
        assertEquals(child.getParentGenomeSeed(), store.loadArtifactProfile(child.getArtifactStorageKey()).getParentGenomeSeed());
        assertEquals(2, ecosystem.pool().candidates(origin, 20, 6000).size(), "Old evidence remains separately attributable");
    }

    private DatabaseManager database() { return new DatabaseManager("jdbc:sqlite:" + temp.resolve("ecosystem.db"), null, null); }
    private PlayerStateStore store() throws Exception {
        var database = database();
        try (var connection = database.openConnection()) { new SqlSchemaManager().ensureSchema(connection); }
        return new JdbcPlayerStateStore(database, null, JdbcPlayerStateStore.Dialect.SQLITE);
    }
}
