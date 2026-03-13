package obtuseloot.lineage;

import obtuseloot.artifacts.Artifact;
import obtuseloot.telemetry.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BranchCollapsePressureLifecycleTest {

    @TempDir
    Path tempDir;

    @Test
    void branchMaintenanceCostRisesForWeakStagnantBranches() {
        ArtifactLineage lineage = new ArtifactLineage("lineage-cost");
        seedBranch(lineage, biased(LineageBiasDimension.EXPLORATION_PREFERENCE, 0.36D), 0.62D, 1.05D, 18);
        LineageBranchProfile branch = firstBranch(lineage);
        double before = branch.lastMaintenanceCost();

        for (int i = 0; i < 6; i++) {
            lineage.registerDescendantBias(10_000L + i, neutral(), 1.36D, 1.0D, 0.03D, 0.08D, new InheritanceBranchingHeuristics());
        }

        assertTrue(firstBranch(lineage).lastMaintenanceCost() > before);
    }

    @Test
    void branchSurvivalScoreRespondsToRecentUtilityAndEcology() {
        ArtifactLineage healthy = new ArtifactLineage("lineage-healthy");
        ArtifactLineage weak = new ArtifactLineage("lineage-weak");
        seedBranch(healthy, biased(LineageBiasDimension.SUPPORT_PREFERENCE, 0.34D), 0.70D, 1.00D, 18);
        seedBranch(weak, biased(LineageBiasDimension.SUPPORT_PREFERENCE, 0.34D), 0.70D, 1.00D, 18);

        for (int i = 0; i < 10; i++) {
            healthy.registerDescendantBias(20_000L + i, neutral(), 1.02D, 1.0D, 0.03D, 0.72D, new InheritanceBranchingHeuristics());
            weak.registerDescendantBias(30_000L + i, neutral(), 1.38D, 1.0D, 0.03D, 0.06D, new InheritanceBranchingHeuristics());
        }

        assertTrue(firstBranch(healthy).lastSurvivalScore() > firstBranch(weak).lastSurvivalScore());
    }

    @Test
    void branchesTransitionFromStableToUnstableToCollapsingOrCollapse() {
        ArtifactLineage lineage = new ArtifactLineage("lineage-transitions");
        seedBranch(lineage, biased(LineageBiasDimension.RITUAL_PREFERENCE, 0.35D), 0.60D, 1.00D, 18);

        boolean sawUnstable = false;
        for (int i = 0; i < 20; i++) {
            lineage.registerDescendantBias(40_000L + i, neutral(), 1.45D, 1.0D, 0.03D, 0.04D, new InheritanceBranchingHeuristics());
            sawUnstable |= lineage.consumeRecentBranchTransitions().stream().anyMatch(t -> t.to() == BranchLifecycleState.UNSTABLE);
        }

        assertTrue(sawUnstable);
        assertTrue(lineage.branchCollapses() > 0 || lineage.branches().values().stream().anyMatch(b -> b.lifecycleState() == BranchLifecycleState.COLLAPSING));
    }

    @Test
    void unstableBranchesCanRecoverWithinGraceWindow() {
        ArtifactLineage lineage = new ArtifactLineage("lineage-recovery");
        EvolutionaryBiasGenome support = biased(LineageBiasDimension.SUPPORT_PREFERENCE, 0.33D);
        seedBranch(lineage, support, 0.62D, 1.02D, 18);

        for (int i = 0; i < 12 && firstBranch(lineage).lifecycleState() == BranchLifecycleState.STABLE; i++) {
            lineage.registerDescendantBias(50_000L + i, neutral(), 1.30D, 1.0D, 0.03D, 0.08D, new InheritanceBranchingHeuristics());
        }
        assertTrue(firstBranch(lineage).lifecycleState() != BranchLifecycleState.STABLE);

        for (int i = 0; i < 8; i++) {
            lineage.registerDescendantBias(51_000L + i, support, 1.01D, 1.0D, 0.03D, 0.78D, new InheritanceBranchingHeuristics());
        }

        assertFalse(lineage.branches().isEmpty());
        assertEquals(BranchLifecycleState.STABLE, firstBranch(lineage).lifecycleState());
        assertTrue(lineage.branchCollapses() < lineage.branchBirths());
    }

    @Test
    void overBranchingPressureIncreasesWeakBranchMaintenanceCost() {
        ArtifactLineage crowded = new ArtifactLineage("lineage-crowded");
        seedBranch(crowded, biased(LineageBiasDimension.SUPPORT_PREFERENCE, 0.34D), 0.50D, 1.08D, 16);
        seedBranch(crowded, biased(LineageBiasDimension.EXPLORATION_PREFERENCE, 0.34D), 0.50D, 1.08D, 16);
        seedBranch(crowded, biased(LineageBiasDimension.RITUAL_PREFERENCE, 0.34D), 0.50D, 1.08D, 16);

        ArtifactLineage sparse = new ArtifactLineage("lineage-sparse");
        seedBranch(sparse, biased(LineageBiasDimension.SUPPORT_PREFERENCE, 0.34D), 0.50D, 1.08D, 16);

        for (int i = 0; i < 8; i++) {
            crowded.registerDescendantBias(60_000L + i, neutral(), 1.34D, 1.0D, 0.03D, 0.07D, new InheritanceBranchingHeuristics());
            sparse.registerDescendantBias(61_000L + i, neutral(), 1.34D, 1.0D, 0.03D, 0.07D, new InheritanceBranchingHeuristics());
        }

        double crowdedCost = crowded.branches().values().stream().mapToDouble(LineageBranchProfile::lastMaintenanceCost).average().orElse(0.0D);
        double sparseCost = sparse.branches().values().stream().mapToDouble(LineageBranchProfile::lastMaintenanceCost).average().orElse(0.0D);
        assertTrue(crowdedCost > sparseCost);
    }

    @Test
    void usefulDifferentiatedBranchesCanStillSurvive() {
        ArtifactLineage lineage = new ArtifactLineage("lineage-diverse");
        EvolutionaryBiasGenome support = biased(LineageBiasDimension.SUPPORT_PREFERENCE, 0.36D);
        EvolutionaryBiasGenome explorer = biased(LineageBiasDimension.EXPLORATION_PREFERENCE, 0.36D);

        seedBranch(lineage, support, 0.74D, 1.00D, 16);
        seedBranch(lineage, explorer, 0.74D, 1.00D, 16);

        for (int i = 0; i < 12; i++) {
            lineage.registerDescendantBias(70_000L + i, i % 2 == 0 ? support : explorer, 1.02D, 1.0D, 0.03D, 0.76D, new InheritanceBranchingHeuristics());
        }

        long stable = lineage.branches().values().stream().filter(b -> b.lifecycleState() == BranchLifecycleState.STABLE).count();
        assertTrue(stable >= 1);
    }

    @Test
    void collapseLifecycleSignalsAreEmittedToTelemetry() {
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(tempDir.resolve("branch-lifecycle-events.log"));
        ScheduledEcosystemRollups rollups = new ScheduledEcosystemRollups(buffer, 1L);
        TelemetryAggregationService service = new TelemetryAggregationService(buffer, archive, rollups, 256);
        EcosystemTelemetryEmitter emitter = new EcosystemTelemetryEmitter(service);

        LineageRegistry registry = new LineageRegistry();
        registry.setTelemetryEmitter(emitter);
        Artifact artifact = artifact(1L, "lineage-telemetry");
        EvolutionaryBiasGenome branchBias = biased(LineageBiasDimension.EXPLORATION_PREFERENCE, 0.36D);

        for (int i = 0; i < 9; i++) {
            artifact.setLastUtilityHistory("v1|ud=0.500000");
            registry.recordDescendantBias(artifact, branchBias, 1.08D, 1.0D);
        }
        for (int i = 0; i < 22; i++) {
            artifact.setLastUtilityHistory("v1|ud=0.020000");
            registry.recordDescendantBias(artifact, neutral(), 1.48D, 1.0D);
        }
        emitter.flushAll();

        List<EcosystemTelemetryEvent> events = archive.readAll();
        assertTrue(events.stream().anyMatch(e -> "branch-collapsed".equalsIgnoreCase(e.attributes().get("event"))));
        assertTrue(events.stream().anyMatch(e -> e.attributes().containsKey("survival_score") && e.attributes().containsKey("maintenance_cost")
                && e.attributes().containsKey("lifecycle_state") && e.attributes().containsKey("grace_window_remaining")));
    }

    @Test
    void integrationScenarioShowsBirthsUnstableCollapsesAndSurvivorsWithoutCascade() {
        ArtifactLineage lineage = new ArtifactLineage("lineage-integration");
        EvolutionaryBiasGenome support = biased(LineageBiasDimension.SUPPORT_PREFERENCE, 0.36D);
        EvolutionaryBiasGenome explorer = biased(LineageBiasDimension.EXPLORATION_PREFERENCE, 0.36D);

        seedBranch(lineage, support, 0.70D, 1.02D, 16);
        seedBranch(lineage, explorer, 0.70D, 1.02D, 16);

        for (int i = 0; i < 24; i++) {
            EvolutionaryBiasGenome selected = i % 2 == 0 ? support : neutral();
            double utility = i % 2 == 0 ? 0.76D : 0.05D;
            double eco = i % 2 == 0 ? 1.02D : 1.42D;
            lineage.registerDescendantBias(90_000L + i, selected, eco, 1.0D, 0.03D, utility, new InheritanceBranchingHeuristics());
        }

        assertTrue(lineage.branchBirths() > 0);
        assertTrue(lineage.branchCollapses() > 0);
        assertTrue(lineage.branchSurvivors() > 0);
        assertTrue(lineage.branchCollapses() < lineage.branchBirths());
    }

    private void seedBranch(ArtifactLineage lineage, EvolutionaryBiasGenome bias, double utility, double ecology, int iterations) {
        for (int i = 0; i < iterations && lineage.branches().isEmpty(); i++) {
            EvolutionaryBiasGenome signal = i % 2 == 0 ? bias : stronglyDivergent();
            lineage.registerDescendantBias(1_000L + lineage.descendantsObserved() + i, signal, Math.max(1.20D, ecology), 1.0D, 0.03D, Math.max(0.72D, utility), new InheritanceBranchingHeuristics());
        }
        for (int i = 0; i < 4 && !lineage.branches().isEmpty(); i++) {
            lineage.registerDescendantBias(5_000L + i, bias, 1.01D, 1.0D, 0.03D, 0.80D, new InheritanceBranchingHeuristics());
        }
        assertFalse(lineage.branches().isEmpty(), "seed should produce at least one branch");
    }

    private EvolutionaryBiasGenome stronglyDivergent() {
        EvolutionaryBiasGenome genome = new EvolutionaryBiasGenome();
        genome.add(LineageBiasDimension.EXPLORATION_PREFERENCE, 0.48D);
        genome.add(LineageBiasDimension.RITUAL_PREFERENCE, -0.35D);
        genome.add(LineageBiasDimension.SUPPORT_PREFERENCE, -0.33D);
        genome.add(LineageBiasDimension.WEIRDNESS, 0.34D);
        return genome;
    }

    private LineageBranchProfile firstBranch(ArtifactLineage lineage) {
        return lineage.branches().values().stream().findFirst().orElseThrow();
    }

    private EvolutionaryBiasGenome biased(LineageBiasDimension dimension, double value) {
        EvolutionaryBiasGenome genome = new EvolutionaryBiasGenome();
        genome.add(dimension, value);
        genome.add(LineageBiasDimension.WEIRDNESS, 0.22D);
        genome.add(LineageBiasDimension.SPECIALIZATION, 0.20D);
        return genome;
    }

    private EvolutionaryBiasGenome neutral() {
        EvolutionaryBiasGenome genome = new EvolutionaryBiasGenome();
        genome.add(LineageBiasDimension.WEIRDNESS, 0.01D);
        return genome;
    }

    private Artifact artifact(long seed, String lineage) {
        Artifact artifact = new Artifact(UUID.randomUUID());
        artifact.setArtifactSeed(seed);
        artifact.setLatentLineage(lineage);
        artifact.setEvolutionPath("SCOUT");
        artifact.setLastUtilityHistory("v1|ud=0.450000");
        return artifact;
    }
}
