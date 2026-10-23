package obtuseloot.ecology;

import obtuseloot.abilities.*;
import obtuseloot.abilities.genome.*;
import obtuseloot.artifacts.*;
import obtuseloot.evolution.GenomeEvolutionEngine;
import obtuseloot.persistence.*;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LocalEcosystemHealthTest {
    private final WorldPosition origin = new WorldPosition(new UUID(0, 1), 0, 0);

    @Test void reportsActualReproductiveConcentrationGenesAndMeasuredTradeoffsWithoutChangingState() {
        var store = new Store(); var manager = new ArtifactManager(store); var ecology = new LocalEcosystem(store, manager);
        var first = item(manager, 1, .2); var second = item(manager, -2, .8);
        ecology.observe(observation(first, origin, 1, 0, "first"));
        ecology.observe(observation(second, origin, 3, 0, "second"));
        var before = ecology.pool().encode(); int saves = store.saves;
        var candidates = ecology.pool().candidates(origin, 20, 6000);
        var health = ecology.healthSnapshot(origin);
        assertEquals(before, ecology.pool().encode()); assertEquals(saves, store.saves);
        assertEquals(candidates, ecology.pool().candidates(origin, 20, 6000));
        assertEquals(2, health.observedItems()); assertEquals(2, health.eligibleParents());
        assertEquals(2, health.observedLineages()); assertEquals(.5, health.lineageSimpson(), 1e-12);
        assertEquals(1.6, health.effectiveParents(), 1e-12); assertEquals(.75, health.maximumParentShare(), 1e-12);
        assertTrue(health.finiteSamplerTotal()); assertEquals(0, health.nonFiniteFitnessItems());
        var genes = health.traits().get(GenomeTrait.KINETIC_BIAS);
        assertEquals(.5, genes.mean(), 1e-12); assertEquals(.09, genes.variance(), 1e-12);
        assertEquals(.7, genes.latentMean(), 1e-12); assertEquals(0, genes.latentVariance(), 1e-12);
        var activity = health.activities().values().iterator().next();
        assertEquals(2, activity.attempts()); assertEquals(2, activity.performanceSamples());
        assertEquals(2, activity.meanBenefit(), 1e-12); assertEquals(0, activity.meanRecoveryTicks(), 1e-12);
        assertThrows(UnsupportedOperationException.class, () -> health.traits().clear());
        assertThrows(UnsupportedOperationException.class, () -> health.activities().clear());
        ecology.save();
        assertEquals(health, new LocalEcosystem(store, new ArtifactManager(store)).healthSnapshot(origin));
    }

    @Test void failuresAreObservedButCannotBreedAndOldRevisionsAreSeparate() {
        var store = new Store(); var manager = new ArtifactManager(store); var ecology = new LocalEcosystem(store, manager);
        var artifact = item(manager, 1, .5);
        ecology.observe(observation(artifact, origin, 1, 30, "old-success"));
        artifact.setEvolutionPath("strider-formed"); assertTrue(new GenomeEvolutionEngine().advance(artifact));
        var empty = ecology.healthSnapshot(origin);
        assertEquals(0, empty.observedItems()); assertEquals(1, empty.oldRevisions()); assertTrue(empty.traits().isEmpty());
        ecology.observe(observation(artifact, origin, 0, 60, "current-failure"));
        var failed = ecology.healthSnapshot(origin);
        assertEquals(1, failed.observedItems()); assertEquals(0, failed.eligibleParents()); assertEquals(1, failed.oldRevisions());
        assertEquals(0, failed.effectiveParents()); assertTrue(failed.activities().values().iterator().next().meanUtility() < 0);
        manager.saveItem(artifact); ecology.save();
        assertEquals(failed, new LocalEcosystem(store, new ArtifactManager(store)).healthSnapshot(origin));
    }

    @Test void evidenceIsLocalAndAReadOnlyShorterWindowCannotEraseLongerWindowData() {
        var store = new Store(); var manager = new ArtifactManager(store); var ecology = new LocalEcosystem(store, manager);
        var artifact = item(manager, 1, .5);
        ecology.observe(observation(artifact, origin, 1, 30, "near"));
        ecology.observe(observation(artifact, new WorldPosition(origin.worldId(), 2000, 0), 100, 30, "far"));
        ecology.observe(observation(artifact, new WorldPosition(new UUID(0, 2), 0, 0), 100, 30, "other-world"));
        var local = ecology.healthSnapshot(origin);
        assertEquals(1, local.activities().values().iterator().next().attempts());
        for (int tick = 0; tick < 11; tick++) ecology.advanceTick();
        var before = ecology.pool().encode();
        assertTrue(ecology.pool().recentEvidence(origin, 20, 10).isEmpty());
        assertEquals(before, ecology.pool().encode());
        assertEquals(1, ecology.healthSnapshot(origin).observedItems());
        assertTrue(ecology.healthSnapshot(new WorldPosition(origin.worldId(), -1000, 0)).traits().isEmpty());
    }

    @Test void profileReadFailureNeverBecomesAnEmptyOrHealthySnapshot() {
        var store = new Store(); var manager = new ArtifactManager(store); var ecology = new LocalEcosystem(store, manager);
        ecology.observe(observation(item(manager, 1, .5), origin, 1, 30, "measured")); ecology.save();
        store.profiles.clear();
        var restored = new LocalEcosystem(store, new ArtifactManager(store));
        assertThrows(IllegalStateException.class, () -> restored.healthSnapshot(origin));
    }

    @Test void nonFiniteUtilityIsReportedAsUnavailableRatherThanZero() {
        var store = new Store(); var manager = new ArtifactManager(store); var ecology = new LocalEcosystem(store, manager);
        var artifact = item(manager, 1, .5);
        ecology.observe(observation(artifact, origin, Double.MAX_VALUE, 0, "overflow"));
        var result = ecology.healthSnapshot(origin);
        assertEquals(1, result.nonFiniteFitnessItems()); assertEquals(0, result.eligibleParents());
        assertNull(result.activities().values().iterator().next().meanUtility());
    }

    @Test void evidenceForAReplacedSeedCannotDescribeTheCurrentGenome() {
        var store = new Store(); var manager = new ArtifactManager(store); var ecology = new LocalEcosystem(store, manager);
        var artifact = item(manager, 1, .5); ecology.observe(observation(artifact, origin, 1, 30, "old-seed"));
        artifact.setArtifactSeed(-99); artifact.setGenome(new GenomeResolver().resolve(-99)); manager.saveItem(artifact);
        var health = ecology.healthSnapshot(origin);
        assertEquals(1, health.oldRevisions()); assertEquals(0, health.observedItems()); assertTrue(health.traits().isEmpty());
    }

    @Test void anEmptyWindowHasNoInventedGeneOrPerformanceStatistics() {
        var health = new LocalEcosystem(new Store(), new ArtifactManager(new Store())).healthSnapshot(origin);
        assertEquals(0, health.observedItems()); assertEquals(0, health.eligibleParents());
        assertTrue(health.traits().isEmpty()); assertTrue(health.activities().isEmpty());
    }

    static Artifact item(ArtifactManager manager, long id, double gene) {
        var artifact = LocalInheritancePoolTest.item(id);
        var traits = new EnumMap<GenomeTrait, Double>(GenomeTrait.class); var latent = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
        for (var trait : GenomeTrait.values()) { traits.put(trait, .5); latent.put(trait, .7); }
        traits.put(GenomeTrait.KINETIC_BIAS, gene);
        artifact.setGenome(new ArtifactGenome(id, traits, latent, Set.of(GenomeTrait.RESONANCE)));
        manager.registerItem(artifact); return artifact;
    }
    static AbilityOutcomeObservation observation(Artifact artifact, WorldPosition position, double benefit, long recovery, String key) {
        var t = new AbilityRegistry().templateById("mobility.double_jump");
        var definition = new AbilityDefinition(t.id(), t.name(), t.family(), t.trigger(), t.mechanic(), "", "", "", "", "", "",
                List.of(), List.of(), t.metadata(), "1", "2", "3", "4", "5");
        var context = new AbilityEventContext(t.trigger(), artifact, artifact.getItemReputation(), 1, "health-fixture",
                AbilityRuntimeContext.intentional(AbilitySource.OTHER), null, true).withPosition(position);
        var result = AbilityExecutionResult.observed(definition, context, AbilityOutcomeType.WORLD_INTERACTION, benefit, null, "measured")
                .withObservationKey(key).withPerformance(new AbilityPerformanceMeasurement(benefit, recovery));
        return new AbilityOutcomeObservation(context, result, definition, 0);
    }
    static final class Store implements PlayerStateStore {
        final Map<String, String> profiles = new HashMap<>(); String ecology; int saves;
        public void saveArtifactProfile(Artifact item) { profiles.put(item.getArtifactStorageKey(), ArtifactProfileCodec.encode(item)); saves++; }
        public Artifact loadArtifactProfile(String key) { return profiles.containsKey(key) ? ArtifactProfileCodec.decode(key, profiles.get(key)) : null; }
        public void saveEcologyState(String value) { ecology = value; }
        public String loadEcologyState() { return ecology; }
        public void saveArtifact(UUID id, Artifact item) { throw new UnsupportedOperationException(); }
        public Artifact loadArtifact(UUID id) { return null; }
        public void saveReputation(UUID id, ArtifactReputation reputation) { throw new UnsupportedOperationException(); }
        public ArtifactReputation loadReputation(UUID id) { return null; }
    }
}
