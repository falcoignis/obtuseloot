package obtuseloot.evolution;

import obtuseloot.lineage.ArtifactLineage;
import obtuseloot.lineage.EvolutionaryBiasGenome;
import obtuseloot.lineage.InheritanceBranchingHeuristics;
import obtuseloot.lineage.LineageRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AdaptiveSupportAllocatorTest {

    @Test
    void limitedOpportunityAllocationUnderHighSaturation() {
        ArtifactUsageTracker tracker = new ArtifactUsageTracker();
        for (int i = 0; i < 60; i++) {
            tracker.nichePopulationTracker().recordTelemetry(i + 1L, Map.of(
                    "NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                    new MechanicUtilitySignal("n" + i, 0.35D, 0.04D, 0.4D, 0.4D, 0.3D, 0.2D, 20L, 2L, 9.0D)));
        }
        AdaptiveSupportBudget budget = new EcosystemCarryingCapacityModel().calculate(tracker.nichePopulationTracker().rollups());
        assertTrue(budget.saturationIndex() > 0.20D);
        assertTrue(budget.totalBudget() < budget.carryingCapacity());
    }

    @Test
    void crowdedNicheLosesOpportunityAndRareUsefulNicheGainsIt() {
        ArtifactUsageTracker tracker = new ArtifactUsageTracker();
        for (int i = 0; i < 20; i++) {
            tracker.nichePopulationTracker().recordTelemetry(100L + i, Map.of(
                    "NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                    new MechanicUtilitySignal("weak" + i, 0.25D, 0.03D, 0.3D, 0.5D, 0.4D, 0.3D, 18L, 1L, 8.5D)));
        }
        tracker.nichePopulationTracker().recordTelemetry(999L, Map.of(
                "RITUAL_CHANNEL@ON_MEMORY_EVENT",
                new MechanicUtilitySignal("rare", 6.8D, 0.72D, 0.9D, 0.05D, 0.05D, 0.05D, 10L, 8L, 5.0D)));

        AdaptiveSupportAllocator allocator = new AdaptiveSupportAllocator();
        EvolutionOpportunityPool pool = allocator.buildPool(tracker, new LineageRegistry());
        NicheOpportunityAllocation crowded = pool.nicheAllocations().get(MechanicNicheTag.NAVIGATION);
        NicheOpportunityAllocation rare = pool.nicheAllocations().get(MechanicNicheTag.RITUAL_STRANGE_UTILITY);

        assertNotNull(crowded);
        assertNotNull(rare);
        assertTrue(crowded.reinforcementMultiplier() < rare.reinforcementMultiplier());
        assertTrue(crowded.competitionPressure() > rare.competitionPressure());
    }

    @Test
    void lineageMomentumInfluencesMutationSupportAndDiminishingReturns() {
        LineageRegistry registry = new LineageRegistry();
        ArtifactLineage dominant = seedLineage("dominant", 0.82D, 1.22D, 24);
        ArtifactLineage emerging = seedLineage("emerging", 0.78D, 1.02D, 10);
        registry.lineages().put("dominant", dominant);
        registry.lineages().put("emerging", emerging);

        AdaptiveSupportBudget budget = new AdaptiveSupportBudget(2.0D, 1.5D, 2.4D, 0.9D, 0.9D, 0.5D, 0.2D);
        LineageMomentumPool pool = new LineageCompetitionModel().evaluate(registry.lineages(), budget);

        LineageMomentumProfile dominantProfile = pool.profile("dominant");
        LineageMomentumProfile emergingProfile = pool.profile("emerging");

        assertTrue(dominantProfile.momentum() > 0.0D);
        assertTrue(emergingProfile.momentum() > 0.0D);
        assertTrue(dominantProfile.diminishingReturns() <= 1.0D);
        assertTrue(dominantProfile.mutationSupportStrength() <= 1.35D);
        assertTrue(pool.displacementPressure() > 0.0D);
    }

    @Test
    void turnoverEmergesOverSimulatedGenerationsWithCompetition() {
        ArtifactUsageTracker tracker = new ArtifactUsageTracker();
        LineageRegistry registry = new LineageRegistry();
        registry.lineages().put("alpha", seedLineage("alpha", 0.85D, 1.18D, 22));
        registry.lineages().put("beta", seedLineage("beta", 0.62D, 1.03D, 12));
        registry.lineages().put("gamma", seedLineage("gamma", 0.55D, 0.95D, 8));

        for (int generation = 0; generation < 18; generation++) {
            int crowdCount = Math.max(6, 24 - generation);
            for (int i = 0; i < crowdCount; i++) {
                tracker.nichePopulationTracker().recordTelemetry(1_000L + generation * 100L + i, Map.of(
                        "NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                        new MechanicUtilitySignal("crowd" + generation + ":" + i, 0.30D + (generation * 0.01D), 0.04D, 0.35D, 0.4D, 0.35D, 0.3D, 15L, 2L, 7.5D)));
            }
            tracker.nichePopulationTracker().recordTelemetry(9_000L + generation, Map.of(
                    "RITUAL_CHANNEL@ON_MEMORY_EVENT",
                    new MechanicUtilitySignal("rare" + generation, 5.5D + (generation * 0.10D), 0.65D, 0.85D, 0.08D, 0.08D, 0.08D, 9L, 7L, 4.5D)));
        }

        AdaptiveSupportAllocator allocator = new AdaptiveSupportAllocator();
        EvolutionOpportunityPool pool = allocator.buildPool(tracker, registry);
        NicheOpportunityAllocation navigation = pool.nicheAllocations().get(MechanicNicheTag.NAVIGATION);
        NicheOpportunityAllocation ritual = pool.nicheAllocations().get(MechanicNicheTag.RITUAL_STRANGE_UTILITY);

        assertNotNull(navigation);
        assertNotNull(ritual);
        assertTrue(pool.budget().turnoverPressure() > 0.0D);
        assertTrue(ritual.opportunityShare() > 0.0D);
        assertTrue(ritual.reinforcementMultiplier() >= navigation.reinforcementMultiplier());
    }

    @Test
    void endToEndCompetitionScenarioShowsEarlyDominanceThenDiminishingAndEmergence() {
        ArtifactUsageTracker tracker = new ArtifactUsageTracker();
        LineageRegistry registry = new LineageRegistry();
        registry.lineages().put("lineage-a", seedLineage("lineage-a", 0.92D, 1.24D, 30));
        registry.lineages().put("lineage-b", seedLineage("lineage-b", 0.70D, 1.00D, 10));
        registry.lineages().put("lineage-c", seedLineage("lineage-c", 0.66D, 0.96D, 9));

        for (int i = 0; i < 30; i++) {
            tracker.nichePopulationTracker().recordTelemetry(20_000L + i, Map.of(
                    "NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                    new MechanicUtilitySignal("dom" + i, 0.38D, 0.05D, 0.35D, 0.4D, 0.35D, 0.30D, 16L, 2L, 8.0D)));
        }
        for (int i = 0; i < 8; i++) {
            tracker.nichePopulationTracker().recordTelemetry(30_000L + i, Map.of(
                    "HARVEST_RELAY@ON_BLOCK_HARVEST",
                    new MechanicUtilitySignal("new" + i, 3.8D, 0.42D, 0.75D, 0.12D, 0.10D, 0.10D, 11L, 7L, 5.0D)));
        }

        AdaptiveSupportAllocator allocator = new AdaptiveSupportAllocator();
        EvolutionOpportunityPool pool = allocator.buildPool(tracker, registry);
        LineageMomentumProfile dominant = pool.lineageMomentumPool().profile("lineage-a");
        LineageMomentumProfile emerging = pool.lineageMomentumPool().profile("lineage-b");

        assertTrue(dominant.momentum() > emerging.momentum());
        assertTrue(dominant.diminishingReturns() < 1.0D);
        assertTrue(pool.lineageMomentumPool().displacementPressure() > 0.0D);
        assertTrue(pool.budget().explorationReserve() >= 0.08D);
    }


    @Test
    void competitionPressureInteractsWithEcologyAndLineage() {
        ArtifactUsageTracker tracker = new ArtifactUsageTracker();
        tracker.nichePopulationTracker().recordTelemetry(501L, Map.of(
                "NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                new MechanicUtilitySignal("weak", 0.3D, 0.04D, 0.35D, 0.35D, 0.3D, 0.3D, 14L, 2L, 7.0D)));
        tracker.nichePopulationTracker().recordTelemetry(777L, Map.of(
                "RITUAL_CHANNEL@ON_MEMORY_EVENT",
                new MechanicUtilitySignal("useful", 4.5D, 0.55D, 0.82D, 0.10D, 0.10D, 0.10D, 10L, 7L, 4.3D)));

        LineageRegistry registry = new LineageRegistry();
        registry.lineages().put("lineage-useful", seedLineage("lineage-useful", 0.8D, 1.01D, 14));

        AdaptiveSupportAllocation allocation = new AdaptiveSupportAllocator().allocateFor(
                777L, "lineage-useful", tracker, registry);

        assertTrue(allocation.reinforcementMultiplier() >= 0.45D);
        assertTrue(allocation.mutationOpportunity() >= 0.45D);
        assertTrue(allocation.retentionOpportunity() >= 0.45D);
    }

    private ArtifactLineage seedLineage(String id, double utilityDensity, double ecologicalPressure, int samples) {
        ArtifactLineage lineage = new ArtifactLineage(id);
        InheritanceBranchingHeuristics heuristics = new InheritanceBranchingHeuristics();
        for (int i = 0; i < samples; i++) {
            EvolutionaryBiasGenome bias = new EvolutionaryBiasGenome();
            bias.add(obtuseloot.lineage.LineageBiasDimension.SPECIALIZATION, 0.12D + (i * 0.002D));
            bias.add(obtuseloot.lineage.LineageBiasDimension.UTILITY_DENSITY_PREFERENCE, utilityDensity * 0.25D);
            lineage.registerDescendantBias(i + 1L, bias, ecologicalPressure, 1.05D, 0.03D, utilityDensity, heuristics);
        }
        return lineage;
    }
}
