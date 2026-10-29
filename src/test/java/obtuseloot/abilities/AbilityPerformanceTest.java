package obtuseloot.abilities;

import obtuseloot.abilities.genome.*;
import obtuseloot.evolution.*;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AbilityPerformanceTest {
    private final AbilityPerformanceModel model = new AbilityRegistry().registrationById("mobility.double_jump").performanceModel();
    private ArtifactGenome genome(double kinetic) { return new ArtifactGenome(-42, Map.of(GenomeTrait.KINETIC_BIAS, kinetic)); }

    @Test void sameIdentityExpressesInheritedGenesInsteadOfReconstructingFromTheSeed() {
        var low = model.express(genome(0)); var mid = model.express(genome(.5)); var high = model.express(genome(1));
        assertEquals(.315, low.output(.42), 1e-12); assertEquals(30, low.recoveryTicks(60));
        assertEquals(.42, mid.output(.42), 1e-12); assertEquals(60, mid.recoveryTicks(60));
        assertEquals(.525, high.output(.42), 1e-12); assertEquals(90, high.recoveryTicks(60));
        assertEquals(high, model.express(genome(1)), "Expression does not roll new randomness");
    }
    @Test void latentExpressionAndOrdinaryMutationChangeTheSamePhysicalTraitDeterministically() {
        var original = new ArtifactGenome(-42, Map.of(GenomeTrait.KINETIC_BIAS, .25), Map.of(GenomeTrait.KINETIC_BIAS, .5), java.util.Set.of());
        var activated = original.activateLatentTrait(GenomeTrait.KINETIC_BIAS, .5);
        assertTrue(model.express(activated).outputMultiplier() > model.express(original).outputMultiplier());
        var mutation = new GenomeMutationEngine();
        var first = mutation.mutate(original, 2); var replay = mutation.mutate(original, 2);
        assertNotEquals(model.express(original), model.express(first));
        assertEquals(model.express(first), model.express(replay));
    }
    @Test void onlyDeclaredTradeoffsVaryAndRegistrationBuildersRetainTheModel() {
        var catalog = new AbilityRegistry();
        var inherited = Map.of(
                "mobility.double_jump", GenomeTrait.KINETIC_BIAS,
                "precision.material_insight", GenomeTrait.PRECISION_AFFINITY,
                "mobility.footprint_memory", GenomeTrait.MOBILITY_AFFINITY,
                "survival.weather_omen", GenomeTrait.SURVIVAL_INSTINCT,
                "survival.herd_instinct", GenomeTrait.RESONANCE,
                "engineering.redstone_sympathy", GenomeTrait.PRECISION_AFFINITY);
        for (var entry : catalog.registrations()) {
            var trait = inherited.get(entry.template().id());
            if (trait == null) {
                assertEquals(AbilityPerformance.FIXED, entry.performanceModel().express(genome(1)), entry.template().id());
                continue;
            }
            var low = entry.performanceModel().express(new ArtifactGenome(-42, Map.of(trait, 0.0)));
            var mid = entry.performanceModel().express(new ArtifactGenome(-42, Map.of(trait, 0.5)));
            var high = entry.performanceModel().express(new ArtifactGenome(-42, Map.of(trait, 1.0)));
            assertEquals(AbilityPerformance.FIXED, mid, entry.template().id());
            assertTrue(low.outputMultiplier() < mid.outputMultiplier() && mid.outputMultiplier() < high.outputMultiplier(), entry.template().id());
            assertTrue(low.recoveryMultiplier() < mid.recoveryMultiplier() && mid.recoveryMultiplier() < high.recoveryMultiplier(), entry.template().id());
        }
        var original = catalog.registrationById("mobility.double_jump");
        assertSame(original.performanceModel(), original.withLiveEffect(original.liveEffect()).performanceModel());
        var passive = original.withPassiveEffect(AbilityActivationPolicy.EQUIPMENT, (p, a, d) -> null);
        assertEquals(model.express(genome(0)), passive.performanceModel().express(genome(0)));
    }
    @Test void invalidGenesAndOverflowCannotProducePhysicalValues() {
        assertThrows(IllegalArgumentException.class, () -> model.express(genome(Double.NaN)));
        assertThrows(IllegalArgumentException.class, () -> AbilityPerformanceModel.tradeoff(GenomeTrait.KINETIC_BIAS, 1, .5));
        assertThrows(IllegalArgumentException.class, () -> new AbilityPerformance(Double.POSITIVE_INFINITY, 1));
        assertThrows(IllegalArgumentException.class, () -> model.express(genome(1)).recoveryTicks(Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> model.express(genome(1)).output(Double.MAX_VALUE));
        assertEquals(0, model.express(genome(1)).recoveryTicks(0));
    }
    @Test void actualResponseAndRecoveryCanReverseWhichTraitIsUseful() {
        // Controlled measurement fixtures, not claims about a natural live population.
        double lowOpen = utility(.6, 30), highOpen = utility(1.7, 90);
        assertTrue(highOpen > lowOpen, "Additional measured clearance can pay for a longer recovery");
        assertEquals(utility(.6, 30), utility(.6, 90), "One use is valued by its benefit; recovery costs later uses, not this one");
        assertTrue(utility(0, 30) < 0, "No observed benefit still earns a failure penalty");
        assertEquals(utility(0, 30), utility(0, 90), "Recovery never erases a failed-attempt penalty");
    }
    private double utility(double response, long cooldown) {
        var profile = new OutcomeUtilityProfile("MOVEMENT_ECHO@ON_AIR_JUMP");
        var outcome = new UtilityOutcomeRecord("fixture", AbilityMechanic.MOVEMENT_ECHO, AbilityTrigger.ON_AIR_JUMP,
                response > 0 ? AbilityExecutionStatus.SUCCESS : AbilityExecutionStatus.NO_OP,
                AbilityOutcomeType.WORLD_INTERACTION, response > 0, true, 1, 1, "measured", 0, "unique",
                new AbilityPerformanceMeasurement(response, cooldown));
        return profile.ingest(outcome, new ValidatedOutcomeClassifier()).validatedUtility();
    }
}
