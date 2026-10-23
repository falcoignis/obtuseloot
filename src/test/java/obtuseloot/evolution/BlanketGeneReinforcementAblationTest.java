package obtuseloot.evolution;

import obtuseloot.abilities.*;
import obtuseloot.abilities.genome.*;
import obtuseloot.artifacts.*;
import obtuseloot.ecosystem.EnvironmentPressureEngine;
import obtuseloot.memory.ArtifactMemoryEngine;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BlanketGeneReinforcementAblationTest {
    @Test void fitnessSignDoesNotDirectlyRaiseOrLowerEveryGeneWithMacroContextFixed() {
        var tracker = new ArtifactUsageTracker();
        var engine = new ExperienceEvolutionEngine(tracker, new ArtifactFitnessEvaluator());
        var input = genome();
        var empty = engine.applyExperienceFeedback(input, 77, 1, 1, null, null, new ArtifactUsageProfile());
        for (boolean success : List.of(true, false)) {
            var actual = engine.applyExperienceFeedback(input, 77, 1, 1, null, null, usage(success));
            assertEquals(empty.traits(), actual.traits(), "Aggregate fitness must not prescribe the direction of every expressed trait");
            assertEquals(empty.latentTraits(), actual.latentTraits(), "Aggregate fitness must not prescribe latent genes either");
            assertEquals(input.activatedLatentTraits(), actual.activatedLatentTraits());
        }
    }

    @Test void frozenControlsDemonstrateTheEarlierAggregateFitnessAblation() {
        var tracker = new ArtifactUsageTracker(); var evaluator = new ArtifactFitnessEvaluator();
        var environment = new EnvironmentPressureEngine();
        var legacy = new LegacyExperienceEvolutionEngine(tracker, evaluator, environment);
        var previous = new LegacyEcologicalExperienceEvolutionEngine(tracker, evaluator, environment);
        var positive = legacy.applyExperienceFeedback(genome(), 77, 1, 1, null, null, usage(true));
        var negative = legacy.applyExperienceFeedback(genome(), 77, 1, 1, null, null, usage(false));
        for (var trait : GenomeTrait.values()) {
            assertTrue(positive.trait(trait) > negative.trait(trait));
            assertTrue(positive.latentTrait(trait) > negative.latentTrait(trait));
        }
        var empty = new ArtifactUsageProfile();
        assertGenomeEquals(legacy.applyExperienceFeedback(genome(), 77, 1, 1, null, null, empty),
                previous.applyExperienceFeedback(genome(), 77, 1, 1, null, null, empty));
    }

    @Test void realTransitionsKeepMutationAndRevisionHistoryWithoutFitnessDirectedGenes() {
        var registry = new AbilityRegistry();
        var items = new ArrayList<Artifact>();
        for (boolean success : List.of(true, false)) {
            var item = new Artifact(new UUID(0, 77), "elytra");
            item.setArtifactStorageKey("artifact:" + new UUID(0, 77)); item.setArtifactSeed(77); item.setGenome(genome());
            var revision = GenomeRevision.capture(item);
            item.getGenomeFitnessHistory().record(outcome(success, revision));
            item.setEvolutionPath("strider-formed");
            var engine = new GenomeEvolutionEngine(registry, new ArtifactMemoryEngine(), null, null,
                    new ExperienceEvolutionEngine(new ArtifactUsageTracker(), new ArtifactFitnessEvaluator()), ScoringMode.PROJECTION_WITH_CACHE);
            assertTrue(engine.advance(item)); assertEquals(1, item.getGenomeRevision());
            assertNotEquals(genome().traits(), item.getGenome().traits(), "Ordinary transition variation remains");
            assertFalse(engine.advance(item), "The same transition must not replay");
            assertTrue(item.getGenomeFitnessHistory().revisions().contains(revision));
            items.add(item);
        }
        assertGenomeEquals(items.getFirst().getGenome(), items.getLast().getGenome());
    }

    private ArtifactGenome genome() {
        var traits = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
        for (var trait : GenomeTrait.values()) traits.put(trait, .4);
        return new ArtifactGenome(77, traits, traits, Set.of(GenomeTrait.RESONANCE));
    }
    private void assertGenomeEquals(ArtifactGenome expected, ArtifactGenome actual) {
        assertEquals(expected.seed(), actual.seed());
        assertEquals(expected.traits(), actual.traits());
        assertEquals(expected.latentTraits(), actual.latentTraits());
        assertEquals(expected.activatedLatentTraits(), actual.activatedLatentTraits());
    }
    private ArtifactUsageProfile usage(boolean success) {
        var result = new ArtifactUsageProfile(); result.recordUtilityOutcome(outcome(success, new GenomeRevision(77, 0))); return result;
    }
    private UtilityOutcomeRecord outcome(boolean success, GenomeRevision revision) {
        var t = new AbilityRegistry().templateById("mobility.double_jump");
        return new UtilityOutcomeRecord(t.id(), t.mechanic(), t.trigger(), success ? AbilityExecutionStatus.SUCCESS : AbilityExecutionStatus.NO_OP,
                AbilityOutcomeType.WORLD_INTERACTION, success, true, 1, 1, "controlled", 100, "target", null, revision);
    }
}
