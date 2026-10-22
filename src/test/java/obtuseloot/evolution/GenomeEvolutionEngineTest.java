package obtuseloot.evolution;

import obtuseloot.abilities.*;
import obtuseloot.abilities.genome.*;
import obtuseloot.artifacts.*;
import obtuseloot.awakening.AwakeningEngine;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.drift.DriftEngine;
import obtuseloot.lineage.*;
import obtuseloot.memory.*;
import obtuseloot.persistence.ArtifactProfileCodec;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GenomeEvolutionEngineTest {
    private Artifact item() {
        var artifact = new Artifact(new UUID(0, 42), "elytra");
        artifact.setArtifactSeed(-19);
        artifact.setArtifactStorageKey(ArtifactStorageKeys.create());
        return artifact;
    }

    @Test void realPathTransitionsChangeStoredPerformanceOnceIncludingSameStageTransitions() {
        var artifact = item();
        var genetics = new GenomeEvolutionEngine();
        var evolution = new EvolutionEngine(new ArchetypeResolver(), new HybridEvolutionResolver(), genetics);
        var model = new AbilityRegistry().registrationById("mobility.double_jump").performanceModel();
        var initial = artifact.getGenome();
        assertFalse(genetics.advance(artifact));
        while (artifact.getItemReputation().getTotalScore() < RuntimeSettings.get().archetypeThreshold()) {
            artifact.getItemReputation().recordMobility();
        }
        evolution.evaluate(artifact, artifact.getItemReputation());
        assertEquals(1, artifact.getGenomeRevision());
        var formed = artifact.getGenome();
        assertNotEquals(initial.traits(), formed.traits());
        assertNotEquals(model.express(initial), model.express(formed));
        evolution.evaluate(artifact, artifact.getItemReputation());
        assertSame(formed, artifact.getGenome());
        while (artifact.getItemReputation().getTotalScore() < RuntimeSettings.get().temperedThreshold()) {
            artifact.getItemReputation().recordMobility();
        }
        evolution.evaluate(artifact, artifact.getItemReputation());
        assertEquals(2, ArtifactEvolutionStage.resolveStage(artifact));
        assertEquals(2, artifact.getGenomeRevision());
        assertNotEquals(formed.traits(), artifact.getGenome().traits());
        assertFalse(genetics.advance(artifact));
    }

    @Test void repeatedProfileInspectionUsesStoredGenesWithoutActivatingOrBlendingThem() {
        var artifact = item();
        artifact.setEvolutionPath("strider-advanced");
        artifact.setDriftLevel(8);
        var genes = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
        for (var trait : GenomeTrait.values()) genes.put(trait, 0.8D);
        artifact.setGenome(new ArtifactGenome(artifact.getArtifactSeed(), genes, genes, Set.of()));
        var registry = new AbilityRegistry();
        var memory = new ArtifactMemoryEngine() {
            @Override public ArtifactMemoryProfile profile(Artifact ignored) {
                return new ArtifactMemoryProfile(12, 1, 1, 1, 4, 1, 1, 1);
            }
        };
        var lineages = new LineageRegistry();
        var lineageResolver = new LineageInfluenceResolver();
        var genetics = new GenomeEvolutionEngine(registry, memory, lineages, lineageResolver, null, ScoringMode.PROJECTION_WITH_CACHE);
        assertTrue(genetics.advance(artifact));
        assertFalse(artifact.getGenome().activatedLatentTraits().isEmpty(), "Fixture must activate latent genes at a transition");
        var stored = artifact.getGenome();
        long revision = artifact.getGenomeRevision();
        var lineage = lineages.assignLineage(artifact);
        int ancestry = lineage.generationIndex();
        assertEquals(stored.traits(), lineage.genomeTraits());
        var resolver = new SeededAbilityResolver(registry, memory, null, lineages, lineageResolver, null);
        for (int i = 0; i < 10; i++) {
            resolver.resolve(artifact, artifact.getItemReputation());
            assertSame(stored, artifact.getGenome());
            assertEquals(revision, artifact.getGenomeRevision());
            assertEquals(ancestry, lineage.generationIndex(), "Reading an existing item must not add ancestry");
            assertEquals(stored.traits(), lineage.genomeTraits());
            var expected = new RegulatoryGateResolver().resolve(artifact, stored, memory.profile(artifact),
                    lineages.assignLineage(artifact), null);
            assertEquals(expected.profileKey(), artifact.getLastRegulatoryProfile());
        }
        var loaded = ArtifactProfileCodec.decode(artifact.getArtifactStorageKey(), ArtifactProfileCodec.encode(artifact));
        assertEquals(stored.traits(), loaded.getGenome().traits());
        assertEquals(stored.latentTraits(), loaded.getGenome().latentTraits());
        assertEquals(stored.activatedLatentTraits(), loaded.getGenome().activatedLatentTraits());
        assertFalse(genetics.advance(loaded));
    }

    @Test void driftAndAwakeningPreserveAndAdvanceTheSamePhysicalItemsGenes() {
        var artifact = item();
        artifact.setArchetypePath("strider");
        artifact.setEvolutionPath("strider-formed");
        var genetics = new GenomeEvolutionEngine();
        genetics.advance(artifact);
        var formed = artifact.getGenome();
        new DriftEngine(genetics).applyDriftSimulation(artifact, artifact.getItemReputation());
        assertEquals(2, artifact.getGenomeRevision());
        assertNotEquals(formed.traits(), artifact.getGenome().traits());
        var transition = new AwakeningEngine(genetics).forceAwakening(null, artifact, artifact.getItemReputation());
        assertNotNull(transition);
        var awakened = transition.replacement();
        assertEquals(artifact.getArtifactStorageKey(), awakened.getArtifactStorageKey());
        assertEquals(3, awakened.getGenomeRevision());
        assertNotEquals(artifact.getGenome().traits(), awakened.getGenome().traits());
        assertFalse(genetics.advance(awakened));
        assertNull(new AwakeningEngine(genetics).evaluateSimulation(awakened, awakened.getItemReputation()));
    }

    @Test void experienceFeedbackCommitsAtTransitionsAndIsNeverReappliedByProfileReads() {
        var item = item();
        var registry = new AbilityRegistry();
        var memory = new ArtifactMemoryEngine();
        var lineages = new LineageRegistry();
        var influence = new LineageInfluenceResolver();
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var lastExperienceSeed = new java.util.concurrent.atomic.AtomicLong();
        var experience = new ExperienceEvolutionEngine(new ArtifactUsageTracker(), new ArtifactFitnessEvaluator()) {
            @Override public ArtifactGenome applyExperienceFeedback(ArtifactGenome genome, long seed, int population,
                    double mutationInfluence, String lineageId, LineageRegistry registry) {
                calls.incrementAndGet();
                lastExperienceSeed.set(seed);
                var changed = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
                changed.putAll(genome.traits());
                changed.put(GenomeTrait.KINETIC_BIAS, 0.95D);
                return new ArtifactGenome(genome.seed(), changed, genome.latentTraits(), genome.activatedLatentTraits());
            }
        };
        var genetics = new GenomeEvolutionEngine(registry, memory, lineages, influence, experience, ScoringMode.PROJECTION_WITH_CACHE);
        var resolver = new SeededAbilityResolver(registry, memory, null, lineages, influence, experience);
        resolver.resolve(item, item.getItemReputation());
        assertEquals(0, calls.get());
        item.setEvolutionPath("strider-formed");
        assertTrue(genetics.advance(item));
        assertEquals(1, calls.get());
        var committed = item.getGenome();
        assertEquals(0.95D, committed.trait(GenomeTrait.KINETIC_BIAS));
        for (int i = 0; i < 5; i++) resolver.resolve(item, item.getItemReputation());
        assertEquals(1, calls.get());
        assertSame(committed, item.getGenome());
        var loaded = ArtifactProfileCodec.decode(item.getArtifactStorageKey(), ArtifactProfileCodec.encode(item));
        assertFalse(genetics.advance(loaded));
        item.setEvolutionPath("strider-tempered");
        loaded.setEvolutionPath("strider-tempered");
        assertTrue(genetics.advance(item));
        assertTrue(genetics.advance(loaded));
        assertEquals(item.getGenome().traits(), loaded.getGenome().traits(), "Restart must retain the next deterministic mutation draw");
        item.setArchetypePath("strider");
        var awakening = new AwakeningEngine(genetics).forceAwakening(null, item, item.getItemReputation());
        assertNotNull(awakening);
        assertNotEquals(item.getArtifactSeed(), awakening.replacement().getArtifactSeed());
        assertEquals(item.getArtifactSeed(), lastExperienceSeed.get(), "Identity transitions must use the existing item's measured experience");
        assertEquals(awakening.replacement().getArtifactSeed(), awakening.replacement().getGenome().seed());
    }

    @Test void corruptOrMissingEvolutionReceiptsFailClosed() {
        var item = item();
        String encoded = ArtifactProfileCodec.encode(item);
        assertThrows(obtuseloot.persistence.PlayerStateLoadException.class,
                () -> ArtifactProfileCodec.decode(item.getArtifactStorageKey(), encoded.replace("revision: 0", "revision: -1")));
        assertThrows(obtuseloot.persistence.PlayerStateLoadException.class,
                () -> ArtifactProfileCodec.decode(item.getArtifactStorageKey(), encoded.replace("evolution-state:", "missing-state:")));
    }

    @Test void explicitBaselineResetsDoNotSuppressTheNextOccurrenceOfTheSameEvolutionPath() {
        var original = item();
        original.setArchetypePath("strider");
        original.setEvolutionPath("strider-formed");
        var genetics = new GenomeEvolutionEngine();
        genetics.advance(original);
        for (boolean regenerate : List.of(false, true)) {
            var fresh = item();
            fresh.setArtifactStorageKey(original.getArtifactStorageKey());
            fresh.setArtifactSeed(90);
            var transition = regenerate ? ArtifactIdentityTransition.regenerate(original, fresh)
                    : ArtifactIdentityTransition.recreate(original, fresh);
            assertEquals(1, fresh.getGenomeRevision());
            assertEquals(original.getGenome().traits(), fresh.getGenome().traits());
            assertFalse(genetics.advance(transition.replacement()), "Administrative baseline creation must not mutate genes");
            fresh.setArchetypePath("strider");
            fresh.setEvolutionPath("strider-formed");
            assertTrue(genetics.advance(fresh));
            assertEquals(2, fresh.getGenomeRevision());
            fresh.resetMutableState();
            assertFalse(genetics.advance(fresh));
            fresh.setEvolutionPath("strider-formed");
            assertTrue(genetics.advance(fresh));
            assertEquals(3, fresh.getGenomeRevision());
        }
    }
}
