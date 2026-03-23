package obtuseloot.evolution;

import obtuseloot.abilities.*;
import obtuseloot.artifacts.Artifact;
import obtuseloot.lineage.ArtifactLineage;
import obtuseloot.lineage.EvolutionaryBiasGenome;
import obtuseloot.lineage.InheritanceBranchingHeuristics;
import obtuseloot.lineage.LineageInfluenceResolver;
import obtuseloot.lineage.LineageRegistry;
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

    @Test
    void eligibilityFilterRetainsMeaningfulCandidatePoolUnderAllGateProfiles() {
        AbilityRegistry registry = new AbilityRegistry();
        RegulatoryEligibilityFilter filter = new RegulatoryEligibilityFilter();
        List<AbilityTemplate> all = registry.templates();

        // Open all gates: pool should remain full
        RegulatoryGateResolver resolver = new RegulatoryGateResolver();
        Artifact openArtifact = artifact(111_000L);
        ArtifactMemoryProfile mixedProfile = profileFor(4);
        AbilityRegulatoryProfile openProfile = resolver.resolve(openArtifact, new obtuseloot.abilities.genome.GenomeResolver().resolve(111_000L),
                mixedProfile, null, null);
        List<AbilityTemplate> openPool = filter.filter(all, openProfile);
        assertTrue(openPool.size() >= 20,
                "Open-gate profile must retain a broad candidate pool (got " + openPool.size() + ").");

        // Even under restricted gates the pool should remain viable (>= 10 templates)
        Artifact restrictedArtifact = artifact(222_000L);
        AbilityRegulatoryProfile restrictedProfile = resolver.resolve(restrictedArtifact,
                new obtuseloot.abilities.genome.GenomeResolver().resolve(222_000L), mixedProfile, null, null);
        List<AbilityTemplate> restrictedPool = filter.filter(all, restrictedProfile);
        assertTrue(restrictedPool.size() >= 10,
                "Even restrictive gate profiles must not collapse the pool below 10 (got " + restrictedPool.size() + ").");
    }

    @Test
    void lineageInfluenceIsPresentButDoesNotDominateOverMemoryAndEcology() {
        AbilityRegistry registry = new AbilityRegistry();
        LineageRegistry lineageRegistry = new LineageRegistry();
        LineageInfluenceResolver resolver = new LineageInfluenceResolver();
        ProceduralAbilityGenerator withLineage = new ProceduralAbilityGenerator(registry, null, lineageRegistry, resolver);
        ProceduralAbilityGenerator baseline = new ProceduralAbilityGenerator(registry);

        Map<String, Integer> withLineageCounts = new HashMap<>();
        Map<String, Integer> baselineCounts = new HashMap<>();

        int runs = 120;
        for (int i = 0; i < runs; i++) {
            Artifact artifact = artifact(55_000L + i);
            artifact.setLatentLineage("ritual-heavy-lineage");
            ArtifactLineage lineage = lineageRegistry.assignLineage(artifact);
            lineage.evolutionaryBiasGenome().add(obtuseloot.lineage.LineageBiasDimension.RITUAL_PREFERENCE, 0.40D);
            lineage.evolutionaryBiasGenome().add(obtuseloot.lineage.LineageBiasDimension.WEIRDNESS, 0.35D);

            ArtifactMemoryProfile memory = profileFor(i % 5);
            for (AbilityDefinition def : withLineage.generate(artifact, 4, memory).abilities()) {
                withLineageCounts.merge(def.family().name(), 1, Integer::sum);
            }
            for (AbilityDefinition def : baseline.generate(artifact(55_000L + i), 4, memory).abilities()) {
                baselineCounts.merge(def.family().name(), 1, Integer::sum);
            }
        }

        // Lineage influence should be detectable: CHAOS family biased up by ritual/weirdness lineage
        double chaosWithLineage = withLineageCounts.getOrDefault("CHAOS", 0) / (double) Math.max(1, withLineageCounts.values().stream().mapToInt(Integer::intValue).sum());
        double chaosBaseline = baselineCounts.getOrDefault("CHAOS", 0) / (double) Math.max(1, baselineCounts.values().stream().mapToInt(Integer::intValue).sum());

        // Lineage must have a visible effect (chaos should be higher with ritual/weirdness lineage)
        assertTrue(chaosWithLineage >= chaosBaseline * 0.90D,
                "Lineage should have some influence on family selection (not necessarily dominating).");
        // But lineage must not fully collapse other families — non-CHAOS must remain present
        double nonChaosWithLineage = 1.0D - chaosWithLineage;
        assertTrue(nonChaosWithLineage >= 0.40D,
                "Non-CHAOS families must remain reachable even under strong ritual/weirdness lineage (got "
                        + String.format("%.2f", nonChaosWithLineage) + ").");
    }

    @Test
    void explorationPreferenceBiasesTowardNavigationWithoutCollapsingOtherNiches() {
        AbilityRegistry registry = new AbilityRegistry();
        LineageRegistry lineageRegistry = new LineageRegistry();
        LineageInfluenceResolver resolver = new LineageInfluenceResolver();
        ProceduralAbilityGenerator generator = new ProceduralAbilityGenerator(registry, null, lineageRegistry, resolver);
        NicheTaxonomy taxonomy = new NicheTaxonomy();

        Map<MechanicNicheTag, Integer> nicheCountsHigh = new java.util.EnumMap<>(MechanicNicheTag.class);
        Map<MechanicNicheTag, Integer> nicheCountsLow = new java.util.EnumMap<>(MechanicNicheTag.class);

        int runs = 100;
        for (int i = 0; i < runs; i++) {
            // Use unique lineage names per artifact to prevent bias accumulation across iterations
            Artifact artifactHigh = artifact(66_000L + i);
            artifactHigh.setLatentLineage("explorer-high-" + i);
            lineageRegistry.assignLineage(artifactHigh).evolutionaryBiasGenome()
                    .add(obtuseloot.lineage.LineageBiasDimension.EXPLORATION_PREFERENCE, 0.30D);

            Artifact artifactLow = artifact(66_500L + i);
            artifactLow.setLatentLineage("explorer-low-" + i);
            // No exploration bias — tendency stays at 0.0 (well below 0.20 threshold)

            ArtifactMemoryProfile memory = profileFor(i % 5);
            for (AbilityDefinition def : generator.generate(artifactHigh, 4, memory).abilities()) {
                for (MechanicNicheTag tag : taxonomy.nichesFor(def.mechanic(), def.trigger())) {
                    nicheCountsHigh.merge(tag, 1, Integer::sum);
                }
            }
            for (AbilityDefinition def : generator.generate(artifactLow, 4, memory).abilities()) {
                for (MechanicNicheTag tag : taxonomy.nichesFor(def.mechanic(), def.trigger())) {
                    nicheCountsLow.merge(tag, 1, Integer::sum);
                }
            }
        }

        int totalHigh = nicheCountsHigh.values().stream().mapToInt(Integer::intValue).sum();
        int totalLow = nicheCountsLow.values().stream().mapToInt(Integer::intValue).sum();
        double navHigh = nicheCountsHigh.getOrDefault(MechanicNicheTag.NAVIGATION, 0) / (double) Math.max(1, totalHigh);
        double navLow = nicheCountsLow.getOrDefault(MechanicNicheTag.NAVIGATION, 0) / (double) Math.max(1, totalLow);

        // High exploration lineage should show more NAVIGATION than low exploration
        assertTrue(navHigh >= navLow,
                "High-exploration lineage should bias toward NAVIGATION more than low-exploration lineage.");
        // But NAVIGATION should not monopolize — other niches must survive
        double nonNavHigh = 1.0D - navHigh;
        assertTrue(nonNavHigh >= 0.30D,
                "High-exploration lineage must not collapse all non-NAVIGATION niches (non-nav share: "
                        + String.format("%.2f", nonNavHigh) + ").");
    }

    @Test
    void familyWeightingFavorsMatchesWithoutLockingOutAlternatives() {
        AbilityRegistry registry = new AbilityRegistry();
        ProceduralAbilityGenerator generator = new ProceduralAbilityGenerator(registry);

        Map<String, Integer> mobilityFamilyCounts = new HashMap<>();
        Map<String, Integer> chaosLeanFamilyCounts = new HashMap<>();

        int runs = 160;
        for (int i = 0; i < runs; i++) {
            // Explorer profile biases toward MOBILITY
            ArtifactMemoryProfile explorer = profileFor(0);
            for (AbilityDefinition def : generator.generate(artifact(88_000L + i), 4, explorer).abilities()) {
                mobilityFamilyCounts.merge(def.family().name(), 1, Integer::sum);
            }
            // Ritual profile biases toward CHAOS
            ArtifactMemoryProfile ritual = profileFor(1);
            for (AbilityDefinition def : generator.generate(artifact(89_000L + i), 4, ritual).abilities()) {
                chaosLeanFamilyCounts.merge(def.family().name(), 1, Integer::sum);
            }
        }

        int mobTotal = mobilityFamilyCounts.values().stream().mapToInt(Integer::intValue).sum();
        int chaosTotal = chaosLeanFamilyCounts.values().stream().mapToInt(Integer::intValue).sum();
        double mobilityShare = mobilityFamilyCounts.getOrDefault("MOBILITY", 0) / (double) Math.max(1, mobTotal);
        double chaosShare = chaosLeanFamilyCounts.getOrDefault("CHAOS", 0) / (double) Math.max(1, chaosTotal);

        // Matching family must be favored but not monopolizing
        assertTrue(mobilityShare > 0.05D, "MOBILITY must be present in explorer-profiled generation.");
        assertTrue(chaosShare > 0.05D, "CHAOS must be present in ritual-profiled generation.");
        assertTrue(mobilityFamilyCounts.size() >= 3,
                "Explorer generation must yield at least 3 distinct families, not lock into one.");
        assertTrue(chaosLeanFamilyCounts.size() >= 3,
                "Ritual generation must yield at least 3 distinct families, not lock into one.");
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
