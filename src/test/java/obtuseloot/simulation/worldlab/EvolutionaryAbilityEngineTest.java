package obtuseloot.simulation.worldlab;

import obtuseloot.abilities.*;
import obtuseloot.artifacts.Artifact;
import obtuseloot.lineage.ArtifactLineage;
import obtuseloot.lineage.EvolutionaryBiasGenome;
import obtuseloot.lineage.InheritanceBranchingHeuristics;
import obtuseloot.lineage.LineageBiasDimension;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EvolutionaryAbilityEngineTest {

    @Test
    void abilitiesTriggerAndEmitTelemetryWithEffectsAndTradeoffs() {
        EvolutionaryAbilityEngine engine = new EvolutionaryAbilityEngine();
        SimulatedArtifactAgent agent = new SimulatedArtifactAgent(seedArtifact(1234L));
        ArtifactLineage lineage = new ArtifactLineage("lin-evo");
        for (int i = 0; i < 6; i++) {
            EvolutionaryBiasGenome bias = EvolutionaryBiasGenome.seeded(99L + i);
            bias.add(LineageBiasDimension.EXPLORATION_PREFERENCE, 0.20D);
            lineage.registerDescendantBias(1234L + i, bias, 1.12D, 1.0D, 4.0D, 0.42D, new InheritanceBranchingHeuristics());
        }

        // Build ritual and niche stability streaks
        List<AbilityDefinition> prep = List.of(def(EvolutionaryAbilityEngine.RITUAL_AMPLIFIER), def(EvolutionaryAbilityEngine.NICHE_ARCHITECT));
        for (int i = 0; i < 4; i++) {
            engine.apply(i, agent, prep, lineage, "niche-a", 1.05D, null);
        }

        List<AbilityDefinition> defs = List.of(
                def(EvolutionaryAbilityEngine.ENTROPY_PULSE),
                def(EvolutionaryAbilityEngine.RESOURCE_PARASITISM),
                def(EvolutionaryAbilityEngine.RITUAL_AMPLIFIER),
                def(EvolutionaryAbilityEngine.LINEAGE_FORTIFICATION),
                def(EvolutionaryAbilityEngine.NICHE_ARCHITECT)
        );
        EvolutionaryAbilityEngine.AbilityEffects effects = engine.apply(10L, agent, defs, lineage, "niche-a", 1.2D, null);

        assertTrue(effects.mutationDriftBias() != 0.0D || effects.utilityTradeoffPenalty() > 0.0D);
        assertTrue(effects.utilityMultiplier() >= 1.0D);
        assertTrue(effects.crowdingPressureDelta() >= 0.0D);
        assertTrue(effects.nicheUtilityDelta() >= 0.0D);
    }

    @Test
    void abilitiesAreRegisteredInAbilityRegistry() {
        AbilityRegistry registry = new AbilityRegistry();
        Map<String, AbilityTemplate> byId = registry.templates().stream().collect(java.util.stream.Collectors.toMap(AbilityTemplate::id, t -> t));
        assertTrue(byId.containsKey(EvolutionaryAbilityEngine.ENTROPY_PULSE));
        assertTrue(byId.containsKey(EvolutionaryAbilityEngine.RESOURCE_PARASITISM));
        assertTrue(byId.containsKey(EvolutionaryAbilityEngine.RITUAL_AMPLIFIER));
        assertTrue(byId.containsKey(EvolutionaryAbilityEngine.LINEAGE_FORTIFICATION));
        assertTrue(byId.containsKey(EvolutionaryAbilityEngine.NICHE_ARCHITECT));
    }

    private AbilityDefinition def(String id) {
        return new AbilityDefinition(id, id, AbilityFamily.CHAOS, AbilityTrigger.ON_MEMORY_EVENT,
                AbilityMechanic.RITUAL_STABILIZATION, "effect", "e", "d", "a", "f", "m", List.of(),
                List.of(new AbilityEffect("x", AbilityEffectType.TRIGGERED_BEHAVIOR, 0.01D)),
                AbilityMetadata.of(java.util.Set.of("ritual-utility"), java.util.Set.of("x"), java.util.Set.of("ritual"), 0.4D, 0.4D, 0.4D, 0.4D, 0.4D, 0.4D),
                "s1", "s2", "s3", "s4", "s5");
    }

    private Artifact seedArtifact(long seed) {
        Artifact artifact = new Artifact(UUID.randomUUID(), "wooden_sword");
        artifact.setArtifactSeed(seed);
        artifact.setLatentLineage("lin-evo");
        return artifact;
    }
}
