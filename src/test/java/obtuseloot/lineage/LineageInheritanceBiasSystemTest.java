package obtuseloot.lineage;

import obtuseloot.abilities.AbilityMetadata;
import obtuseloot.analytics.LineageInheritanceAnalytics;
import obtuseloot.artifacts.Artifact;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LineageInheritanceBiasSystemTest {

    @Test
    void lineageInheritanceProfileCreationInitializesBiasGenome() {
        ArtifactLineage lineage = new ArtifactLineage("lineage-memory-watch");
        assertNotNull(lineage.evolutionaryBiasGenome());
        assertEquals(LineageBiasDimension.values().length, lineage.evolutionaryBiasGenome().tendencies().size());
    }

    @Test
    void localTraitsAndLineageTendenciesRemainDistinct() {
        Artifact artifact = artifact(77L, "lineage-split");
        artifact.setSeedChaosAffinity(0.95D);

        ArtifactLineage lineage = new ArtifactLineage("lineage-split");
        lineage.evolutionaryBiasGenome().add(LineageBiasDimension.RELIABILITY, 0.25D);

        assertEquals(0.95D, artifact.getSeedChaosAffinity(), 1.0E-9D);
        assertTrue(lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.RELIABILITY) > 0.20D);
        assertNotEquals(artifact.getSeedChaosAffinity(), lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.RELIABILITY));
    }

    @Test
    void lineageBiasAffectsSelectionWeighting() {
        ArtifactLineage lineage = new ArtifactLineage("lineage-memory");
        lineage.evolutionaryBiasGenome().add(LineageBiasDimension.MEMORY_REACTIVITY, 0.30D);
        LineageInfluenceResolver resolver = new LineageInfluenceResolver();

        AbilityMetadata memoryTemplate = AbilityMetadata.of(Set.of("memory-history"), Set.of("history-event"), Set.of("memory", "support"),
                0.80D, 0.70D, 0.75D, 0.65D, 0.30D, 0.45D);
        AbilityMetadata plainTemplate = AbilityMetadata.of(Set.of("navigation"), Set.of("region-cache"), Set.of("exploration"),
                0.50D, 0.55D, 0.45D, 0.20D, 0.10D, 0.30D);

        double memoryWeight = resolver.resolveTemplateInfluence(lineage, memoryTemplate);
        double plainWeight = resolver.resolveTemplateInfluence(lineage, plainTemplate);

        assertTrue(memoryWeight > plainWeight, "Memory-reactive lineage should favor memory affinity templates.");
    }

    @Test
    void mutationAndDriftPreventDeterministicCloning() {
        ArtifactLineage lineage = new ArtifactLineage("lineage-drift");
        EvolutionaryBiasGenome observed = new EvolutionaryBiasGenome();
        observed.add(LineageBiasDimension.WEIRDNESS, 0.30D);

        double before = lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.WEIRDNESS);
        for (int i = 0; i < 4; i++) {
            lineage.registerDescendantBias(100L + i, observed, 1.0D, new InheritanceBranchingHeuristics());
        }
        double after = lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.WEIRDNESS);

        assertNotEquals(before, after);
        assertTrue(Math.abs(after - observed.tendency(LineageBiasDimension.WEIRDNESS)) > 0.0001D,
                "Lineage should drift/adapt toward tendency, not clone exact descendant bias.");
    }

    @Test
    void ecologicalPressureCanRedirectInheritanceStrength() {
        ArtifactLineage lowPressure = new ArtifactLineage("lineage-low");
        ArtifactLineage highPressure = new ArtifactLineage("lineage-high");
        EvolutionaryBiasGenome observed = new EvolutionaryBiasGenome();
        observed.add(LineageBiasDimension.SUPPORT_PREFERENCE, 0.30D);

        lowPressure.registerDescendantBias(201L, observed, 1.0D, new InheritanceBranchingHeuristics());
        highPressure.registerDescendantBias(202L, observed, 1.4D, new InheritanceBranchingHeuristics());

        double lowDelta = Math.abs(lowPressure.evolutionaryBiasGenome().tendency(LineageBiasDimension.SUPPORT_PREFERENCE));
        double highDelta = Math.abs(highPressure.evolutionaryBiasGenome().tendency(LineageBiasDimension.SUPPORT_PREFERENCE));

        assertTrue(lowDelta > highDelta, "Higher ecological pressure should reduce direct inheritance lock-in.");
    }

    @Test
    void repeatedDivergenceFormsSublineageBranches() {
        ArtifactLineage lineage = new ArtifactLineage("lineage-branch");
        EvolutionaryBiasGenome divergent = new EvolutionaryBiasGenome();
        divergent.add(LineageBiasDimension.EXPLORATION_PREFERENCE, 0.34D);
        divergent.add(LineageBiasDimension.RITUAL_PREFERENCE, -0.30D);
        divergent.add(LineageBiasDimension.SUPPORT_PREFERENCE, -0.25D);
        divergent.add(LineageBiasDimension.WEIRDNESS, 0.30D);

        for (int i = 0; i < 8; i++) {
            lineage.registerDescendantBias(400L + i, divergent, 1.2D, new InheritanceBranchingHeuristics());
        }

        assertFalse(lineage.branches().isEmpty());
        assertTrue(lineage.dominantBranchId().contains("explorer") || lineage.dominantBranchId().contains("adaptive"));
    }

    @Test
    void lineageAnalyticsExposeInheritedTrends() {
        ArtifactLineage lineage = new ArtifactLineage("lineage-analytics");
        lineage.evolutionaryBiasGenome().add(LineageBiasDimension.WEIRDNESS, 0.20D);
        EvolutionaryBiasGenome observed = new EvolutionaryBiasGenome();
        observed.add(LineageBiasDimension.WEIRDNESS, 0.30D);
        for (int i = 0; i < 5; i++) {
            lineage.registerDescendantBias(600L + i, observed, 1.1D, new InheritanceBranchingHeuristics());
        }

        Artifact artifact = artifact(603L, "lineage-analytics");
        artifact.setLastUtilityHistory("v1|vu=2.0|ud=0.650000|mr=0.6|nr=0.1|be=0.8|at=8|signals=");

        Map<String, Object> summary = new LineageInheritanceAnalytics().summarize(List.of(artifact), List.of(lineage));

        Map<?, ?> utility = (Map<?, ?>) summary.get("lineageUtilityDensity");
        Map<?, ?> branches = (Map<?, ?>) summary.get("lineageBranchCounts");
        assertEquals(0.65D, (Double) utility.get("lineage-analytics"), 1.0E-9D);
        assertTrue((Integer) branches.get("lineage-analytics") >= 0);
    }

    @Test
    void endToEndLineageBiasDriftBranchingAndAnalyticsScenario() {
        LineageRegistry registry = new LineageRegistry();
        Artifact artifact = artifact(900L, "lineage-e2e");
        ArtifactLineage lineage = registry.assignLineage(artifact);

        EvolutionaryBiasGenome initial = new EvolutionaryBiasGenome();
        initial.add(LineageBiasDimension.MEMORY_REACTIVITY, 0.30D);
        initial.add(LineageBiasDimension.PATIENCE, 0.22D);

        for (int i = 0; i < 10; i++) {
            EvolutionaryBiasGenome descendantBias = initial.copy();
            if (i > 4) {
                descendantBias.add(LineageBiasDimension.EXPLORATION_PREFERENCE, 0.30D);
                descendantBias.add(LineageBiasDimension.WEIRDNESS, 0.25D);
                descendantBias.add(LineageBiasDimension.SUPPORT_PREFERENCE, -0.20D);
            }
            registry.recordDescendantBias(artifact(901L + i, "lineage-e2e"), descendantBias, 1.15D);
        }

        assertTrue(lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.MEMORY_REACTIVITY) > 0.0D);
        assertTrue(lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.EXPLORATION_PREFERENCE) > -0.2D);
        assertFalse(lineage.branches().isEmpty(), "Divergent descendants should form a branch under sustained pressure.");

        Artifact utilityProbe = artifact(999L, "lineage-e2e");
        utilityProbe.setLastUtilityHistory("v1|vu=1.8|ud=0.720000|mr=0.55|nr=0.15|be=0.82|at=9|signals=");
        Map<String, Object> analytics = new LineageInheritanceAnalytics().summarize(List.of(utilityProbe), registry.lineages().values());

        assertTrue(((Map<?, ?>) analytics.get("lineageUtilityDensity")).containsKey("lineage-e2e"));
        assertTrue((Long) analytics.get("branchingLineages") >= 1L);
    }

    private Artifact artifact(long seed, String lineage) {
        Artifact artifact = new Artifact(UUID.randomUUID());
        artifact.setArtifactSeed(seed);
        artifact.setLatentLineage(lineage);
        return artifact;
    }
}
