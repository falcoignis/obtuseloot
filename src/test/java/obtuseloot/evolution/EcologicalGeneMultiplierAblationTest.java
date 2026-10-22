package obtuseloot.evolution;

import obtuseloot.abilities.genome.*;
import obtuseloot.ecosystem.EnvironmentPressureEngine;
import obtuseloot.lineage.LineageRegistry;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EcologicalGeneMultiplierAblationTest {
    @Test void neutralContextPreservesUnmodifiedGenesAndEveryLatentGeneAcrossSeasons() {
        var environment = new EnvironmentPressureEngine();
        var engine = new ExperienceEvolutionEngine(new ArtifactUsageTracker(), new ArtifactFitnessEvaluator(), environment);
        var input = genome();
        for (int season = 0; season < 24; season++) {
            var actual = engine.applyExperienceFeedback(input, input.seed());
            for (var trait : GenomeTrait.values()) {
                assertEquals(Math.min(1, input.trait(trait) * environment.multiplierFor(trait)), actual.trait(trait), 1e-15,
                        "Only the separately retained environmental modifier may alter expressed genes: " + trait);
            }
            assertEquals(input.latentTraits(), actual.latentTraits(), "Macro context cannot scale latent genes");
            assertEquals(input.activatedLatentTraits(), actual.activatedLatentTraits());
            assertEquals(input.seed(), actual.seed());
            environment.advanceSeason();
        }
    }

    @Test void pressureSupportAndLineageInfluenceNoLongerPrescribeGeneDirection() {
        var pressure = new RolePressureMetrics(.8, .1, .2, .7, .9, .6, 1.4);
        var tracker = new ArtifactUsageTracker() {
            private final NichePopulationTracker niches = new NichePopulationTracker() {
                @Override public RolePressureMetrics pressureFor(long seed) { return pressure; }
            };
            @Override public NichePopulationTracker nichePopulationTracker() { return niches; }
        };
        var environment = new EnvironmentPressureEngine();
        var support = new AdaptiveSupportAllocation(.8, .6, .7, .9, .8, .7, .6);
        var current = new ExperienceEvolutionEngine(tracker, new ArtifactFitnessEvaluator(), environment) {
            @Override public AdaptiveSupportAllocation adaptiveSupportFor(long seed, String lineage, LineageRegistry registry) { return support; }
        };
        var legacy = new LegacyEcologicalExperienceEvolutionEngine(tracker, new ArtifactFitnessEvaluator(), environment) {
            @Override public AdaptiveSupportAllocation adaptiveSupportFor(long seed, String lineage, LineageRegistry registry) { return support; }
        };
        for (double influence : List.of(.5, 1.0, 2.0)) {
            var input = genome();
            var actual = current.applyExperienceFeedback(input, input.seed(), 100, influence, "fixture", null, new ArtifactUsageProfile());
            var old = legacy.applyExperienceFeedback(input, input.seed(), 100, influence, "fixture", null, new ArtifactUsageProfile());
            for (var trait : GenomeTrait.values())
                assertEquals(input.trait(trait) * environment.multiplierFor(trait), actual.trait(trait), 1e-15);
            assertEquals(input.latentTraits(), actual.latentTraits());
            assertNotEquals(old.traits(), actual.traits(), "Control must exercise the removed context transform");
            assertNotEquals(old.latentTraits(), actual.latentTraits());
        }
    }

    @Test void frozenControlRetainsTheFormerNeutralDecay() {
        var environment = new EnvironmentPressureEngine();
        var legacy = new LegacyEcologicalExperienceEvolutionEngine(new ArtifactUsageTracker(), new ArtifactFitnessEvaluator(), environment);
        var input = genome(); var actual = legacy.applyExperienceFeedback(input, input.seed());
        for (var trait : GenomeTrait.values()) {
            assertEquals(input.trait(trait) * .95 * environment.multiplierFor(trait), actual.trait(trait), 1e-15);
            assertEquals(input.latentTrait(trait) * .9825, actual.latentTrait(trait), 1e-15);
        }
    }

    private ArtifactGenome genome() {
        var traits = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
        var latent = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
        for (var trait : GenomeTrait.values()) { traits.put(trait, .4); latent.put(trait, .7); }
        return new ArtifactGenome(-77, traits, latent, Set.of(GenomeTrait.RESONANCE));
    }
}
