package obtuseloot.abilities;

import obtuseloot.analytics.NicheDiversityMetrics;
import obtuseloot.analytics.RelativeDiversityHealth;
import obtuseloot.artifacts.Artifact;
import obtuseloot.evolution.*;
import obtuseloot.memory.ArtifactMemoryProfile;
import org.junit.jupiter.api.Test;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Regression scenario over the real generator and utility feedback, not proof of emergence.
 * Occupancy counts one dominant generated niche per currently present artifact per window.
 * Outcomes are simulated; this does not exercise world effects or wall-clock bifurcation.
 */
class SeededEcosystemHealthTest {
    private static final long SEED = 0x4845414C54484CL;
    private static final int ARTIFACTS = 32;
    private static final int WINDOWS = 12;
    static final int BASELINE_WINDOWS = 3;
    static final double MAXIMUM_RELATIVE_DROP = 0.25D;

    @Test
    void diversityDoesNotCollapseRelativeToEarlyRun() {
        List<Map<String, Integer>> windows = simulate();
        var health = RelativeDiversityHealth.assess(windows, BASELINE_WINDOWS, MAXIMUM_RELATIVE_DROP);
        System.out.println("SEEDED_ECOSYSTEM_HEALTH seed=" + SEED + " windows=" + windows + " assessment=" + health);
        assertTrue(health.healthy(), () -> "Diversity lost more than 25% of early-run level: " + health);

        // A collapse injected into the same run must trip the exact same gate.
        var collapsed = new ArrayList<>(windows);
        collapsed.set(WINDOWS - 1, Map.of("monoculture", ARTIFACTS));
        assertFalse(RelativeDiversityHealth.assess(collapsed, BASELINE_WINDOWS, MAXIMUM_RELATIVE_DROP).healthy());
        collapsed.set(WINDOWS - 1, Map.of());
        assertFalse(RelativeDiversityHealth.assess(collapsed, BASELINE_WINDOWS, MAXIMUM_RELATIVE_DROP).healthy());
    }

    private List<Map<String, Integer>> simulate() {
        return simulate((registry, feedback) -> new ProceduralAbilityGenerator(registry, null, null, null,
                feedback, true, ScoringMode.PROJECTION_WITH_CACHE, new AbilityDiversityIndex()));
    }

    static List<Map<String, Integer>> simulate(java.util.function.BiFunction<AbilityRegistry,
            ExperienceEvolutionEngine, ProceduralAbilityGenerator> generatorFactory) {
        Random random = new Random(SEED);
        var usage = new ArtifactUsageTracker();
        var feedback = new ExperienceEvolutionEngine(usage, new ArtifactFitnessEvaluator());
        // Per-scenario state: unrelated test order and the singleton's history cannot affect this run.
        var generator = generatorFactory.apply(new AbilityRegistry(), feedback);
        var taxonomy = new NicheTaxonomy();
        List<Artifact> artifacts = new ArrayList<>();
        List<ArtifactMemoryProfile> memories = new ArrayList<>();
        for (int i = 0; i < ARTIFACTS; i++) {
            var artifact = new Artifact(new UUID(random.nextLong(), random.nextLong()), "wooden_sword");
            artifact.setArtifactSeed(random.nextLong());
            artifact.setSeedPrecisionAffinity(random.nextDouble());
            artifact.setSeedBrutalityAffinity(random.nextDouble());
            artifact.setSeedSurvivalAffinity(random.nextDouble());
            artifact.setSeedMobilityAffinity(random.nextDouble());
            artifact.setSeedChaosAffinity(random.nextDouble());
            artifact.setSeedConsistencyAffinity(random.nextDouble());
            artifacts.add(artifact);
            memories.add(new ArtifactMemoryProfile(4, 0.5D + random.nextDouble(), 0.5D + random.nextDouble(),
                    0.5D + random.nextDouble(), 0.5D + random.nextDouble(), 0.5D + random.nextDouble(),
                    random.nextDouble(), random.nextDouble()));
        }
        var windows = new ArrayList<Map<String, Integer>>();
        for (int window = 0; window < WINDOWS; window++) {
            var occupancy = new TreeMap<String, Integer>();
            for (int i = 0; i < artifacts.size(); i++) {
                Artifact artifact = artifacts.get(i);
                AbilityProfile profile = generator.generate(artifact, Math.min(5, 2 + window / 3), memories.get(i));
                assertFalse(profile.abilities().isEmpty(), "Cannot infer health from missing generation output");
                var nicheVotes = new EnumMap<MechanicNicheTag, Integer>(MechanicNicheTag.class);
                for (AbilityDefinition ability : profile.abilities()) {
                    taxonomy.nichesFor(ability.mechanic(), ability.trigger()).forEach(tag -> nicheVotes.merge(tag, 1, Integer::sum));
                    boolean meaningful = random.nextDouble() < 0.7D;
                    usage.profileFor(artifact).recordUtilityOutcome(new UtilityOutcomeRecord(
                            ability.id(), ability.mechanic(), ability.trigger(), AbilityExecutionStatus.SUCCESS,
                            AbilityOutcomeType.INFORMATION, meaningful, true, 0.5D + random.nextDouble(),
                            1.0D, "seeded-health-simulation", 1L + window * 3_600_000L));
                }
                // Enum order breaks ties consistently. This observes generated profiles; it does not
                // place artifacts, boost underrepresented niches or require any named niche to survive.
                MechanicNicheTag dominant = nicheVotes.entrySet().stream()
                        .max(Comparator.<Map.Entry<MechanicNicheTag, Integer>>comparingInt(Map.Entry::getValue)
                                .thenComparing(e -> -e.getKey().ordinal())).orElseThrow().getKey();
                occupancy.merge(dominant.name(), 1, Integer::sum);
                artifact.setLastUtilityHistory(usage.utilityHistoryFor(artifact).encode());
            }
            assertEquals(ARTIFACTS, occupancy.values().stream().mapToInt(Integer::intValue).sum());
            var diversity = NicheDiversityMetrics.diversity(occupancy);
            assertTrue(diversity.simpsonDiversity() >= 0 && diversity.simpsonDiversity() <= 1);
            assertTrue(diversity.effectiveNiches() <= diversity.liveNiches());
            windows.add(occupancy);
        }
        return windows;
    }
}
