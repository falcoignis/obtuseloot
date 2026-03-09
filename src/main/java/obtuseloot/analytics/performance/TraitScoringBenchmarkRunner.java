package obtuseloot.analytics.performance;

import obtuseloot.abilities.*;
import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeResolver;
import obtuseloot.simulation.worldlab.WorldSimulationConfig;
import obtuseloot.simulation.worldlab.WorldSimulationHarness;

import java.util.*;

public class TraitScoringBenchmarkRunner {
    private final TraitScoringBenchmarkConfig config;
    private final AbilityRegistry registry = new AbilityRegistry();
    private final List<AbilityTemplate> templates = registry.templates();
    private final GenomeResolver genomeResolver = new GenomeResolver();

    public TraitScoringBenchmarkRunner(TraitScoringBenchmarkConfig config) {
        this.config = config;
    }

    public TraitScoringBenchmarkResult runAll() throws Exception {
        List<Long> seedPool = seedPool();
        Map<Integer, List<TraitScoringBenchmarkResult.ModeRun>> byWorkload = new LinkedHashMap<>();
        for (int workload : config.workloads()) {
            List<ArtifactGenome> genomes = genomesForWorkload(seedPool, workload);
            List<TraitScoringBenchmarkResult.ModeRun> runs = new ArrayList<>();
            for (ScoringMode mode : config.scoringModes()) {
                runs.add(runMode(mode, genomes));
            }
            byWorkload.put(workload, runs);
        }

        List<ArtifactGenome> parityGenomes = genomesForWorkload(seedPool, config.paritySampleSize());
        List<TraitScoringBenchmarkResult.ParityResult> parityResults = runParity(parityGenomes);
        List<TraitScoringBenchmarkResult.WorldSimResult> world = runWorldSimBenchmarks();
        return new TraitScoringBenchmarkResult(config, byWorkload, parityResults, world);
    }

    private TraitScoringBenchmarkResult.ModeRun runMode(ScoringMode mode, List<ArtifactGenome> genomes) {
        TraitInterferenceResolver resolver = new TraitInterferenceResolver(templates);
        resolver.setScoringMode(mode);
        resolver.resetStats();

        long startUsed = usedMemory();
        long start = System.nanoTime();
        Map<String, Integer> topAbilities = new HashMap<>();
        Map<String, Integer> topFamilies = new HashMap<>();
        double spreadAcc = 0.0D;
        for (ArtifactGenome genome : genomes) {
            Map<String, Double> scores = resolver.scoresByMode(genome, mode);
            List<Map.Entry<String, Double>> ranked = scores.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                    .limit(config.picks())
                    .toList();
            if (ranked.isEmpty()) continue;
            String topId = ranked.getFirst().getKey();
            topAbilities.merge(topId, 1, Integer::sum);
            AbilityTemplate template = templates.stream().filter(t -> t.id().equals(topId)).findFirst().orElse(null);
            if (template != null) {
                topFamilies.merge(template.family().name(), 1, Integer::sum);
            }
            spreadAcc += ranked.size() >= 3 ? (ranked.get(0).getValue() - ranked.get(2).getValue()) : 0.0D;
        }
        long nanos = System.nanoTime() - start;
        long endUsed = usedMemory();
        TraitProjectionStats stats = resolver.statsSnapshot();

        double avg = nanos / (double) genomes.size();
        double throughput = genomes.size() / (nanos / 1_000_000_000.0D);
        long cacheEntryEstimate = 128L;
        long cacheMem = stats.cacheSize() * cacheEntryEstimate;
        return new TraitScoringBenchmarkResult.ModeRun(
                mode,
                genomes.size(),
                nanos,
                avg,
                throughput,
                stats.cacheHits(),
                stats.cacheMisses(),
                stats.cacheHitRate(),
                stats.cacheSize(),
                stats.cacheCapacity(),
                stats.cacheEvictions(),
                Math.max(0L, endUsed - startUsed),
                cacheMem,
                topAbilities,
                topFamilies,
                spreadAcc / Math.max(1, genomes.size())
        );
    }

    private List<TraitScoringBenchmarkResult.ParityResult> runParity(List<ArtifactGenome> genomes) {
        List<TraitScoringBenchmarkResult.ParityResult> out = new ArrayList<>();
        Map<ScoringMode, List<List<String>>> rankedByMode = new LinkedHashMap<>();
        Map<ScoringMode, List<Double>> spreadByMode = new LinkedHashMap<>();
        for (ScoringMode mode : config.scoringModes()) {
            TraitInterferenceResolver resolver = new TraitInterferenceResolver(templates);
            resolver.setScoringMode(mode);
            List<List<String>> rankings = new ArrayList<>();
            List<Double> spreads = new ArrayList<>();
            for (ArtifactGenome genome : genomes) {
                List<Map.Entry<String, Double>> ranked = resolver.scoresByMode(genome, mode).entrySet().stream()
                        .sorted(Map.Entry.<String, Double>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                        .limit(3)
                        .toList();
                rankings.add(ranked.stream().map(Map.Entry::getKey).toList());
                spreads.add(ranked.size() >= 3 ? ranked.get(0).getValue() - ranked.get(2).getValue() : 0.0D);
            }
            rankedByMode.put(mode, rankings);
            spreadByMode.put(mode, spreads);
        }

        List<List<String>> baseline = rankedByMode.get(ScoringMode.BASELINE);
        for (ScoringMode mode : config.scoringModes()) {
            if (mode == ScoringMode.BASELINE) continue;
            int top1 = 0;
            int top3Exact = 0;
            int top3Set = 0;
            int ordering = 0;
            double spreadDelta = 0.0D;
            List<List<String>> candidate = rankedByMode.get(mode);
            for (int i = 0; i < baseline.size(); i++) {
                List<String> b = baseline.get(i);
                List<String> c = candidate.get(i);
                if (!b.isEmpty() && !c.isEmpty() && b.getFirst().equals(c.getFirst())) top1++;
                if (b.equals(c)) {
                    top3Exact++;
                }
                if (new HashSet<>(b).equals(new HashSet<>(c))) top3Set++;
                if (b.size() == c.size() && b.size() == 3) {
                    int inOrder = 0;
                    for (int j = 0; j < 3; j++) if (b.get(j).equals(c.get(j))) inOrder++;
                    if (inOrder >= 2) ordering++;
                }
                spreadDelta += Math.abs(spreadByMode.get(ScoringMode.BASELINE).get(i) - spreadByMode.get(mode).get(i));
            }
            int n = baseline.size();
            out.add(new TraitScoringBenchmarkResult.ParityResult(
                    ScoringMode.BASELINE,
                    mode,
                    n,
                    top1 / (double) n,
                    top3Exact / (double) n,
                    top3Set / (double) n,
                    ordering / (double) n,
                    spreadDelta / n
            ));
        }
        return out;
    }

    private List<TraitScoringBenchmarkResult.WorldSimResult> runWorldSimBenchmarks() throws Exception {
        List<TraitScoringBenchmarkResult.WorldSimResult> out = new ArrayList<>();
        WorldSimulationConfig d = WorldSimulationConfig.defaults();
        for (ScoringMode mode : config.scoringModes()) {
            WorldSimulationConfig cfg = new WorldSimulationConfig(
                    d.seed(),
                    config.worldPlayers(),
                    d.artifactsPerPlayer(),
                    config.worldSessionsPerSeason(),
                    config.worldSeasons(),
                    d.bossFrequency(),
                    d.encounterDensity(),
                    d.chaosEventRate(),
                    d.lowHealthEventRate(),
                    d.mutationPressureMultiplier(),
                    d.memoryEventMultiplier(),
                    "analytics/world-lab",
                    d.enableExperienceDrivenEvolution(),
                    d.enableEcosystemBias(),
                    d.enableDiversityPreservation(),
                    d.enableSelfBalancingAdjustments(),
                    d.enableEnvironmentalPressure(),
                    d.enableTraitInteractions(),
                    mode
            );
            long start = System.currentTimeMillis();
            WorldSimulationHarness harness = new WorldSimulationHarness(cfg);
            harness.runAndWriteOutputs();
            long elapsed = System.currentTimeMillis() - start;
            TraitProjectionStats stats = harness.traitProjectionStats();
            double scoringMillis = (stats.averageScoringMicros() * stats.scoringCalls()) / 1000.0D;
            out.add(new TraitScoringBenchmarkResult.WorldSimResult(
                    mode,
                    config.worldPlayers(),
                    config.worldSeasons(),
                    elapsed,
                    stats.scoringCalls(),
                    stats.averageScoringMicros(),
                    scoringMillis,
                    stats.cacheHits(),
                    stats.cacheMisses(),
                    stats.cacheHitRate(),
                    stats.cacheSize(),
                    stats.cacheEvictions()
            ));
        }
        return out;
    }

    private List<Long> seedPool() {
        Random random = new Random(config.seed());
        List<Long> out = new ArrayList<>(config.seedPoolSize());
        for (int i = 0; i < config.seedPoolSize(); i++) out.add(random.nextLong());
        return out;
    }

    private List<ArtifactGenome> genomesForWorkload(List<Long> seedPool, int workload) {
        Random random = new Random(config.seed() ^ workload);
        List<ArtifactGenome> out = new ArrayList<>(workload);
        for (int i = 0; i < workload; i++) {
            long seed = seedPool.get(random.nextInt(seedPool.size()));
            out.add(genomeResolver.resolve(seed));
        }
        return out;
    }

    private long usedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
