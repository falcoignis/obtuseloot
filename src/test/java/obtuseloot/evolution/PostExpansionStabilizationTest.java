package obtuseloot.evolution;

import obtuseloot.abilities.*;
import obtuseloot.artifacts.Artifact;
import obtuseloot.lineage.ArtifactLineage;
import obtuseloot.lineage.EvolutionaryBiasGenome;
import obtuseloot.lineage.InheritanceBranchingHeuristics;
import obtuseloot.memory.ArtifactMemoryProfile;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class PostExpansionStabilizationTest {

    @Test
    void postExpansionNicheDistributionIsAuditableAndNotMonolithic() {
        AbilityRegistry registry = new AbilityRegistry();
        NicheTaxonomy taxonomy = new NicheTaxonomy();
        Map<MechanicNicheTag, Long> counts = registry.templates().stream()
                .flatMap(t -> taxonomy.nichesFor(t.mechanic(), t.trigger()).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        assertEquals(88, registry.templates().size());
        assertTrue(counts.getOrDefault(MechanicNicheTag.ENVIRONMENTAL_SENSING, 0L) >= 10L);
        assertTrue(counts.getOrDefault(MechanicNicheTag.ENVIRONMENTAL_SENSING, 0L) <= 28L,
                "Environmental sensing is expected to be broad but should not expand uncontrollably.");
        assertTrue(counts.getOrDefault(MechanicNicheTag.NAVIGATION, 0L) >= 5L);
        assertTrue(counts.getOrDefault(MechanicNicheTag.RITUAL_STRANGE_UTILITY, 0L) >= 5L);
    }

    @Test
    void newBatchAbilitiesAreReachableWithoutFloodingGeneration() {
        ProceduralAbilityGenerator generator = new ProceduralAbilityGenerator(new AbilityRegistry());
        Map<String, Integer> byPrefix = new HashMap<>();

        for (int i = 0; i < 320; i++) {
            Artifact artifact = artifact(10_000L + i);
            ArtifactMemoryProfile memory = profileFor(i % 5);
            AbilityProfile profile = generator.generate(artifact, 4, memory);
            for (AbilityDefinition ability : profile.abilities()) {
                String prefix = ability.id().substring(0, ability.id().indexOf('.'));
                byPrefix.merge(prefix, 1, Integer::sum);
            }
        }

        int total = byPrefix.values().stream().mapToInt(Integer::intValue).sum();
        int expanded = sum(byPrefix, "exploration", "gathering", "ritual", "social", "environment");
        int legacy = sum(byPrefix, "precision", "mobility", "survival", "chaos", "consistency");

        assertTrue(expanded > 0, "Expanded pool must be selectable.");
        assertTrue(legacy > 0, "Legacy pool must remain selectable.");
        assertTrue((double) expanded / Math.max(1, total) < 0.68D, "Expanded pool should not flood early evolution.");
        assertTrue((double) legacy / Math.max(1, total) > 0.22D, "Legacy pool should retain meaningful opportunity.");
    }

    @Test
    void mutationAndBranchThresholdsRemainStableAfterPoolExpansion() {
        ArtifactLineage lineage = new ArtifactLineage("stability-lineage");
        InheritanceBranchingHeuristics heuristics = new InheritanceBranchingHeuristics();

        EvolutionaryBiasGenome mild = new EvolutionaryBiasGenome();
        mild.add(obtuseloot.lineage.LineageBiasDimension.EXPLORATION_PREFERENCE, 0.10D);

        for (int i = 0; i < 7; i++) {
            lineage.registerDescendantBias(50L + i, mild, 1.05D, 1.0D, 0.03D, 0.55D, heuristics);
        }
        assertTrue(lineage.branches().isEmpty(), "Short low-divergence windows should not immediately explode branches.");

        EvolutionaryBiasGenome divergent = new EvolutionaryBiasGenome();
        divergent.add(obtuseloot.lineage.LineageBiasDimension.EXPLORATION_PREFERENCE, 0.42D);
        divergent.add(obtuseloot.lineage.LineageBiasDimension.RITUAL_PREFERENCE, 0.32D);

        for (int i = 0; i < 10; i++) {
            lineage.registerDescendantBias(90L + i, divergent, 1.20D, 1.05D, 0.03D, 0.72D, heuristics);
        }
        assertFalse(lineage.branches().isEmpty(), "Sustained divergence should still branch.");
    }

    @Test
    void lowYieldCrowdedNichesReceiveLowerSupportThanUsefulNiches() {
        ArtifactUsageTracker tracker = new ArtifactUsageTracker();
        for (int i = 0; i < 12; i++) {
            tracker.nichePopulationTracker().recordTelemetry(1_000L + i, Map.of(
                    "SENSE_PING@ON_WORLD_SCAN", new MechanicUtilitySignal("SENSE_PING@ON_WORLD_SCAN", 0.35D, 0.04D, 0.32D, 0.45D, 0.41D, 0.38D, 14L, 1L, 14.0D)
            ));
        }
        tracker.nichePopulationTracker().recordTelemetry(2_000L, Map.of(
                "HARVEST_RELAY@ON_BLOCK_HARVEST", new MechanicUtilitySignal("HARVEST_RELAY@ON_BLOCK_HARVEST", 2.8D, 0.62D, 0.75D, 0.10D, 0.10D, 0.10D, 10L, 7L, 6.0D)
        ));

        AdaptiveSupportAllocator allocator = new AdaptiveSupportAllocator();
        EvolutionOpportunityPool pool = allocator.buildPool(tracker, null);

        NicheOpportunityAllocation env = pool.nicheAllocations().get(MechanicNicheTag.ENVIRONMENTAL_SENSING);
        NicheOpportunityAllocation farm = pool.nicheAllocations().get(MechanicNicheTag.FARMING_WORLDKEEPING);

        assertNotNull(env);
        assertNotNull(farm);
        assertTrue(env.mutationSupport() < farm.mutationSupport());
        assertTrue(env.reinforcementMultiplier() < farm.reinforcementMultiplier());
    }

    @Test
    void behaviorDifferentiationBoostsSpecializedOpportunityWeights() {
        ArtifactUsageTracker tracker = new ArtifactUsageTracker();
        ExperienceEvolutionEngine engine = new ExperienceEvolutionEngine(tracker, new ArtifactFitnessEvaluator());

        double explorer = engine.ecologyModifierFor(10L, AbilityMechanic.ECOLOGICAL_PATHING, AbilityTrigger.ON_CHUNK_ENTER);
        double ritual = engine.ecologyModifierFor(10L, AbilityMechanic.RITUAL_STABILIZATION, AbilityTrigger.ON_RITUAL_COMPLETION);
        double social = engine.ecologyModifierFor(10L, AbilityMechanic.COLLECTIVE_RELAY, AbilityTrigger.ON_PLAYER_GROUP_ACTION);

        assertNotEquals(explorer, ritual);
        assertNotEquals(ritual, social);
        assertTrue(Math.abs(explorer - ritual) > 0.01D || Math.abs(ritual - social) > 0.01D);
        assertTrue(social >= 0.60D && social <= 1.52D);
        assertTrue(ritual >= 0.60D && ritual <= 1.52D);
        assertTrue(explorer >= 0.60D && explorer <= 1.52D);
    }

    @Test
    void simulationOrientedStabilizationRunShowsNicheSpreadWithoutFlood() {
        AbilityRegistry registry = new AbilityRegistry();
        ProceduralAbilityGenerator generator = new ProceduralAbilityGenerator(registry);
        ArtifactUsageTracker tracker = new ArtifactUsageTracker();

        for (int i = 0; i < 140; i++) {
            Artifact artifact = artifact(30_000L + i);
            ArtifactMemoryProfile memory = profileFor(i % 5);
            AbilityProfile profile = generator.generate(artifact, 4, memory);
            ArtifactUsageProfile usage = tracker.profileForSeed(artifact.getArtifactSeed());
            long t = i * 100L;
            for (AbilityDefinition ability : profile.abilities()) {
                double yield = metadataFor(registry, ability.id()).ecologicalYieldScore();
                boolean meaningful = yield >= 0.62D;
                usage.recordUtilityOutcome(new UtilityOutcomeRecord(
                        ability.id(),
                        ability.mechanic(),
                        ability.trigger(),
                        AbilityExecutionStatus.SUCCESS,
                        meaningful ? AbilityOutcomeType.WORLD_INTERACTION : AbilityOutcomeType.FLAVOR_ONLY,
                        meaningful,
                        true,
                        meaningful ? 0.75D : 0.30D,
                        meaningful ? 1.0D : 1.35D,
                        "sim",
                        ++t
                ));
            }
            tracker.nichePopulationTracker().recordTelemetry(artifact.getArtifactSeed(), usage.utilitySignalsByMechanic());
        }

        Map<MechanicNicheTag, NicheUtilityRollup> rollups = tracker.nichePopulationTracker().rollups();
        int totalArtifacts = rollups.values().stream().mapToInt(NicheUtilityRollup::activeArtifacts).sum();
        double dominantShare = rollups.values().stream().mapToDouble(r -> r.activeArtifacts() / Math.max(1.0D, totalArtifacts)).max().orElse(1.0D);

        assertTrue(rollups.size() >= 6, "Expanded ecosystem should show multiple active niches.");
        assertTrue(dominantShare < 0.50D, "No single niche should fully flood telemetry.");
        assertTrue(rollups.values().stream().anyMatch(r -> r.outcomeYield() > 0.30D));
    }

    @Test
    void analyticsSnapshotIncludesExpandedSubnicheLabels() {
        NichePopulationTracker tracker = new NichePopulationTracker();
        tracker.recordTelemetry(991L, Map.of(
                "COLLECTIVE_RELAY@ON_PLAYER_GROUP_ACTION", new MechanicUtilitySignal("COLLECTIVE_RELAY@ON_PLAYER_GROUP_ACTION", 1.8D, 0.55D, 0.82D, 0.12D, 0.1D, 0.1D, 5L, 4L, 3.0D)
        ));

        Map<String, Object> snapshot = tracker.analyticsSnapshot();
        @SuppressWarnings("unchecked")
        Map<String, Long> specialization = (Map<String, Long>) snapshot.get("specializationTrends");
        assertTrue(specialization.containsKey("group_coordination"));
    }


    @Test
    void crowdedLowYieldNicheTemplatesArePenalizedAgainstHigherYieldPeers() {
        AbilityRegistry registry = new AbilityRegistry();
        ProceduralAbilityGenerator generator = new ProceduralAbilityGenerator(registry);

        int environmentHits = 0;
        int gatheringHits = 0;
        int runs = 280;
        for (int i = 0; i < runs; i++) {
            Artifact artifact = artifact(77_777L + i);
            ArtifactMemoryProfile memory = profileFor(i % 5);
            AbilityProfile profile = generator.generate(artifact, 2, memory);
            for (AbilityDefinition ability : profile.abilities()) {
                if (ability.id().startsWith("environment.")) {
                    environmentHits++;
                }
                if (ability.id().startsWith("gathering.") || ability.id().equals("survival.gentle_harvest")) {
                    gatheringHits++;
                }
            }
        }

        assertTrue(environmentHits > 0, "Expanded environmental abilities should remain reachable.");
        assertTrue(gatheringHits > 0, "Gathering-support abilities should remain reachable.");
        assertTrue(environmentHits < gatheringHits * 1.40D,
                "Crowded, lower-yield environmental templates should not overrun gathering-oriented utility generation.");
    }

    private int sum(Map<String, Integer> byPrefix, String... prefixes) {
        return Arrays.stream(prefixes).mapToInt(prefix -> byPrefix.getOrDefault(prefix, 0)).sum();
    }

    private AbilityMetadata metadataFor(AbilityRegistry registry, String id) {
        return registry.templates().stream().filter(t -> t.id().equals(id)).findFirst().orElseThrow().metadata();
    }

    private ArtifactMemoryProfile profileFor(int mode) {
        return switch (mode) {
            case 0 -> new ArtifactMemoryProfile(7, 1.1D, 0.9D, 0.8D, 0.7D, 1.7D, 0.4D, 0.3D); // explorer
            case 1 -> new ArtifactMemoryProfile(8, 1.2D, 1.1D, 0.7D, 0.8D, 0.9D, 1.3D, 0.4D); // ritual
            case 2 -> new ArtifactMemoryProfile(6, 0.7D, 0.8D, 0.6D, 1.5D, 0.8D, 0.5D, 0.2D); // gather
            case 3 -> new ArtifactMemoryProfile(5, 0.6D, 0.9D, 1.6D, 0.7D, 0.8D, 0.6D, 0.2D); // social
            default -> new ArtifactMemoryProfile(6, 0.9D, 1.0D, 1.0D, 1.0D, 1.0D, 0.8D, 0.3D); // mixed
        };
    }

    private Artifact artifact(long seed) {
        Artifact artifact = new Artifact(UUID.randomUUID(), "wooden_sword");
        artifact.setArtifactSeed(seed);
        artifact.setSeedPrecisionAffinity(0.5D);
        artifact.setSeedBrutalityAffinity(0.1D);
        artifact.setSeedSurvivalAffinity(0.5D);
        artifact.setSeedMobilityAffinity(0.5D);
        artifact.setSeedChaosAffinity(0.5D);
        artifact.setSeedConsistencyAffinity(0.5D);
        return artifact;
    }
}
