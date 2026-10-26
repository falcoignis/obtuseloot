package obtuseloot.abilities;

import obtuseloot.analytics.NicheDiversityMetrics;
import obtuseloot.artifacts.Artifact;
import obtuseloot.evolution.*;
import obtuseloot.memory.ArtifactMemoryProfile;
import obtuseloot.reputation.ArtifactReputation;
import obtuseloot.telemetry.ArtifactRuntimeCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Closed-loop niche ecology on a synthetic clock: real generator, real usage tracker and the real
 * bifurcation tracker. Outcomes are simulated from a hidden per-seed success rate for each niche
 * tag; nothing here names a niche that must survive or a share to hit. The generator's variant
 * bias is not exercised offline (it needs the plugin singleton), so this measures the niche
 * tracker's own births and collapses plus their effect through outcome relevance.
 */
class NicheScaffoldingAblationTest {
    static final int ARTIFACTS = 48;
    static final int LINEAGES = 8;
    static final int WINDOWS = 24;
    static final long WINDOW_MS = 6_000L;
    static final long START_MS = 1_000_000_000L;
    static final long STEP_MS = 10L;
    static final int SEEDS = 16;

    /** Pre-registered research runs take minutes; run them with -Dobtuseloot.experiments=true. */
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
    @EnabledIfSystemProperty(named = "obtuseloot.experiments", matches = "true")
    private @interface Experiment { }

    record Run(List<String> windows, List<String> children, List<String> gates, List<String> competition) { }

    @Test
    void scenarioReplaysExactlyWithFreshState() {
        // Small enough for the default suite; the research run uses the full population.
        Run first = simulate(0x4E49434845L, 6, 12);
        Run second = simulate(0x4E49434845L, 6, 12);
        assertEquals(first.windows(), second.windows());
        assertEquals(first.children(), second.children());
        assertEquals(first.gates(), second.gates());
        assertEquals(first.competition(), second.competition());
        assertEquals(6, first.windows().size());
    }

    @Test
    void retiredChildDoesNotPaceItsParent() {
        AtomicLong clock = new AtomicLong(START_MS);
        var registry = new NicheBifurcationRegistry(8, 4, 60_000L, 1, 1, 0, clock::get);
        assertTrue(registry.evaluateBifurcation("NAVIGATION", 1.0D, 1.0D, 0.5D, 10, clock.get()).isPresent());
        String child = registry.dynamicNiches().iterator().next();
        clock.addAndGet(120_000L);
        registry.collapseUnderpopulatedChildren(Map.of(), Map.of(), 10);
        registry.collapseUnderpopulatedChildren(Map.of(), Map.of(), 11);
        assertFalse(registry.isDynamicNiche(child), "Both children are empty past grace and should retire");
        // Retirement no longer starts a parent cooldown: sustained pressure splits it again immediately.
        assertTrue(registry.evaluateBifurcation("NAVIGATION", 1.0D, 1.0D, 0.5D, 10, clock.get() + 1L).isPresent());
    }

    @Experiment
    @Test
    void nicheScaffoldingAblation() throws IOException {
        String arm = System.getProperty("obtuseloot.arm", "current");
        // Seeds are independent, so a run can be split across JVMs: -Dobtuseloot.seeds=FIRST-LAST.
        String[] range = System.getProperty("obtuseloot.seeds", "0-" + (SEEDS - 1)).split("-");
        int firstSeed = Integer.parseInt(range[0]);
        int lastSeed = Integer.parseInt(range[1]);
        Path out = Path.of("target", "niche-scaffolding-ablation", arm, "seeds-" + firstSeed + "-" + lastSeed);
        Files.createDirectories(out);
        List<String> windows = new ArrayList<>();
        List<String> children = new ArrayList<>();
        List<String> gates = new ArrayList<>();
        List<String> competition = new ArrayList<>();
        windows.add("seed,window,live_children,births,collapses,live_niches,simpson,effective_niches,children");
        children.add("seed,child,parent,birth_window,collapse_window");
        competition.add("seed,window,mean_competition_factor,max_competition_factor");
        gates.add("seed,window,niche,gate,saturation,specialization,share,active_artifacts,children_of_parent,live_children");
        for (int s = firstSeed; s <= lastSeed; s++) {
            long seed = 0x4E49434845L + s * 0x9E3779B97F4A7C15L;
            Run run = simulate(seed, WINDOWS, ARTIFACTS);
            windows.addAll(run.windows());
            children.addAll(run.children());
            gates.addAll(run.gates());
            competition.addAll(run.competition());
        }
        Files.write(out.resolve("windows.csv"), windows);
        Files.write(out.resolve("children.csv"), children);
        Files.write(out.resolve("gates.csv"), gates);
        Files.write(out.resolve("competition.csv"), competition);
        System.out.println("NICHE_SCAFFOLDING_ABLATION arm=" + arm + " rows=" + (windows.size() - 1) + " children=" + (children.size() - 1));
    }

    static Run simulate(long seed, int windowCount, int artifactCount) {
        Random random = new Random(seed);
        AtomicLong clock = new AtomicLong(START_MS);
        var registry = new NicheBifurcationRegistry(clock::get);
        var tracker = new NichePopulationTracker(new EcosystemRoleClassifier(), new EcosystemSaturationModel(), registry);
        var usage = new ArtifactUsageTracker(new ArtifactRuntimeCache<>(2048, 300_000L), tracker);
        var feedback = new ExperienceEvolutionEngine(usage, new ArtifactFitnessEvaluator());
        var generator = new ProceduralAbilityGenerator(new AbilityRegistry(), null, null, null, feedback,
                true, ScoringMode.PROJECTION_WITH_CACHE, new AbilityDiversityIndex());
        var taxonomy = new NicheTaxonomy();

        // Hidden environment: how often each niche tag's abilities produce a meaningful outcome.
        var successRate = new EnumMap<MechanicNicheTag, Double>(MechanicNicheTag.class);
        for (MechanicNicheTag tag : MechanicNicheTag.values()) successRate.put(tag, 0.2D + 0.7D * random.nextDouble());

        double[][] lineageBias = new double[LINEAGES][6];
        for (double[] bias : lineageBias) for (int i = 0; i < bias.length; i++) bias[i] = random.nextDouble();
        List<Artifact> artifacts = new ArrayList<>();
        List<ArtifactMemoryProfile> memories = new ArrayList<>();
        for (int i = 0; i < artifactCount; i++) {
            int lineage = random.nextInt(LINEAGES);
            double[] b = lineageBias[lineage];
            var artifact = new Artifact(new UUID(random.nextLong(), random.nextLong()), "wooden_sword");
            artifact.setArtifactSeed(random.nextLong());
            artifact.setLatentLineage("lineage-" + lineage);
            artifact.setSeedPrecisionAffinity(mix(b[0], random));
            artifact.setSeedBrutalityAffinity(mix(b[1], random));
            artifact.setSeedSurvivalAffinity(mix(b[2], random));
            artifact.setSeedMobilityAffinity(mix(b[3], random));
            artifact.setSeedChaosAffinity(mix(b[4], random));
            artifact.setSeedConsistencyAffinity(mix(b[5], random));
            artifacts.add(artifact);
            memories.add(new ArtifactMemoryProfile(4, 0.5D + random.nextDouble(), 0.5D + random.nextDouble(),
                    0.5D + random.nextDouble(), 0.5D + random.nextDouble(), 0.5D + random.nextDouble(),
                    random.nextDouble(), random.nextDouble()));
            tracker.markCreated(artifact.getArtifactSeed());
        }

        var reputation = new ArtifactReputation();
        var windows = new ArrayList<String>();
        var gates = new ArrayList<String>();
        var competition = new ArrayList<String>();
        var birthWindow = new TreeMap<String, Integer>();
        var parentOf = new TreeMap<String, String>();
        var collapseWindow = new TreeMap<String, Integer>();
        Set<String> previous = new TreeSet<>();
        int seenBifurcations = 0;
        for (int window = 0; window < windowCount; window++) {
            clock.set(START_MS + (window + 1L) * WINDOW_MS);
            for (int i = 0; i < artifacts.size(); i++) {
                Artifact artifact = artifacts.get(i);
                AbilityProfile profile = generator.generate(artifact, Math.min(5, 2 + window / 4), memories.get(i));
                for (AbilityDefinition ability : profile.abilities()) {
                    clock.addAndGet(STEP_MS);
                    double rate = taxonomy.nichesFor(ability.mechanic(), ability.trigger()).stream()
                            .mapToDouble(successRate::get).average().orElse(0.5D);
                    boolean meaningful = random.nextDouble() < rate;
                    var context = new AbilityEventContext(ability.trigger(), artifact, reputation,
                            meaningful ? 1.0D : 0.0D, "niche-scaffolding-ablation");
                    var result = new AbilityExecutionResult(ability.id(), ability.mechanic(), ability.trigger(),
                            artifact.getArtifactStorageKey(), artifact.getOwnerId(), AbilityExecutionStatus.SUCCESS,
                            meaningful ? AbilityOutcomeType.WORLD_INTERACTION : AbilityOutcomeType.FLAVOR_ONLY,
                            meaningful, "", "niche-scaffolding-simulated-outcome");
                    usage.trackAbilityExecution(artifact, context, result, ability);
                }
            }
            List<NicheBifurcation> bifurcations = registry.bifurcations();
            for (int b = seenBifurcations; b < bifurcations.size(); b++) {
                NicheBifurcation event = bifurcations.get(b);
                birthWindow.put(event.childNicheA(), window);
                birthWindow.put(event.childNicheB(), window);
                parentOf.put(event.childNicheA(), event.parentNiche());
                parentOf.put(event.childNicheB(), event.parentNiche());
            }
            int births = 2 * (bifurcations.size() - seenBifurcations);
            seenBifurcations = bifurcations.size();
            Set<String> live = new TreeSet<>(registry.dynamicNiches());
            int collapses = 0;
            for (String child : previous) {
                if (!live.contains(child)) {
                    collapseWindow.put(child, window);
                    collapses++;
                }
            }
            previous = live;
            // Which gate decided each parent evaluated in this window, with the inputs it saw.
            long windowStart = START_MS + (window + 1L) * WINDOW_MS;
            for (MechanicNicheTag tag : MechanicNicheTag.values()) {
                var observed = registry.lastGate(tag.name());
                if (observed.isEmpty() || observed.get().nowMs() < windowStart || observed.get().nowMs() >= windowStart + WINDOW_MS) continue;
                var gate = observed.get();
                gates.add(String.format(Locale.ROOT, "%d,%d,%s,%s,%.6f,%.6f,%.6f,%d,%d,%d", seed, window, tag.name(), gate.gate(),
                        gate.saturationPressure(), gate.specializationPressure(), gate.populationShare(), gate.activeArtifacts(),
                        registry.childrenOf(tag.name()).size(), live.size()));
            }
            // Competition divides each outcome's relevance; report how hard it bites per window.
            double competitionSum = 0.0D;
            double competitionMax = 0.0D;
            for (Artifact artifact : artifacts) {
                double factor = tracker.nicheCompetitionFactor(artifact.getArtifactSeed());
                competitionSum += factor;
                competitionMax = Math.max(competitionMax, factor);
            }
            competition.add(String.format(Locale.ROOT, "%d,%d,%.6f,%.6f", seed, window, competitionSum / artifacts.size(), competitionMax));
            var occupancy = new TreeMap<String, Integer>();
            for (Artifact artifact : artifacts) occupancy.merge(tracker.effectiveNicheName(artifact.getArtifactSeed()), 1, Integer::sum);
            var diversity = NicheDiversityMetrics.diversity(occupancy);
            windows.add(String.format(Locale.ROOT, "%d,%d,%d,%d,%d,%d,%.6f,%.6f,%s", seed, window, live.size(), births,
                    collapses, diversity.liveNiches(), diversity.simpsonDiversity(), diversity.effectiveNiches(),
                    String.join("|", live)));
        }
        var children = new ArrayList<String>();
        birthWindow.forEach((child, born) -> children.add(seed + "," + child + "," + parentOf.get(child) + ","
                + born + "," + collapseWindow.getOrDefault(child, -1)));
        return new Run(windows, children, gates, competition);
    }

    private static double mix(double lineageBias, Random random) {
        return 0.7D * lineageBias + 0.3D * random.nextDouble();
    }
}
