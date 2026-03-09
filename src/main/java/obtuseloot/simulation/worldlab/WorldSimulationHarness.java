package obtuseloot.simulation.worldlab;

import obtuseloot.abilities.*;
import obtuseloot.abilities.mutation.AbilityMutationEngine;
import obtuseloot.abilities.mutation.AbilityMutationResult;
import obtuseloot.abilities.tree.AbilityBranchResolver;
import obtuseloot.analytics.ArtifactEcosystemBalancingAI;
import obtuseloot.analytics.EcosystemHealthReport;
import obtuseloot.analytics.InteractionHeatmapExporter;
import obtuseloot.analytics.TraitInteractionAnalyzer;
import obtuseloot.artifacts.Artifact;
import obtuseloot.dashboard.DashboardService;
import obtuseloot.analytics.TraitInteractionReportWriter;
import obtuseloot.artifacts.ArtifactSeedFactory;
import obtuseloot.names.ArtifactNameGenerator;
import obtuseloot.awakening.AwakeningEngine;
import obtuseloot.drift.DriftEngine;
import obtuseloot.ecosystem.ArtifactEcosystemSelfBalancingEngine;
import obtuseloot.ecosystem.WorldEcosystemProfile;
import obtuseloot.evolution.ArchetypeResolver;
import obtuseloot.evolution.ArtifactFitnessEvaluator;
import obtuseloot.evolution.ArtifactUsageTracker;
import obtuseloot.evolution.EvolutionEngine;
import obtuseloot.evolution.ExperienceEvolutionEngine;
import obtuseloot.evolution.HybridEvolutionResolver;
import obtuseloot.fusion.FusionEngine;
import obtuseloot.memory.ArtifactMemoryEngine;
import obtuseloot.memory.ArtifactMemoryEvent;
import obtuseloot.lineage.LineageInfluenceResolver;
import obtuseloot.lineage.LineageMutation;
import obtuseloot.lineage.LineageRegistry;
import obtuseloot.memory.ArtifactMemoryProfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class WorldSimulationHarness {
    private final WorldSimulationConfig config;
    private final Random random;
    private final SimulationClock clock = new SimulationClock();
    private final SimulationMetricsCollector metrics = new SimulationMetricsCollector();
    private final List<Map<String, Object>> seasonalSnapshots = new ArrayList<>();
    private final List<Long> initialSeedPool = new ArrayList<>();
    private List<SimulatedPlayer> latestPlayers = List.of();

    private final EvolutionEngine evolutionEngine = new EvolutionEngine(new ArchetypeResolver(), new HybridEvolutionResolver());
    private final DriftEngine driftEngine = new DriftEngine();
    private final AwakeningEngine awakeningEngine = new AwakeningEngine();
    private final FusionEngine fusionEngine = new FusionEngine();
    private final ArtifactMemoryEngine memoryEngine = new ArtifactMemoryEngine();
    private final ArtifactEcosystemSelfBalancingEngine ecosystemEngine = new ArtifactEcosystemSelfBalancingEngine();
    private final ArtifactUsageTracker usageTracker = new ArtifactUsageTracker();
    private final ExperienceEvolutionEngine experienceEvolutionEngine;
    private final LineageRegistry lineageRegistry = new LineageRegistry();
    private final LineageInfluenceResolver lineageInfluenceResolver = new LineageInfluenceResolver();
    private final ProceduralAbilityGenerator abilityGenerator;
    private final AbilityMutationEngine mutationEngine = new AbilityMutationEngine();
    private final AbilityBranchResolver branchResolver = new AbilityBranchResolver();
    private final ArtifactSeedFactory seedFactory = new ArtifactSeedFactory();

    public WorldSimulationHarness(WorldSimulationConfig config) {
        this.config = config;
        this.random = new Random(config.seed());
        this.experienceEvolutionEngine = config.enableExperienceDrivenEvolution()
                ? new ExperienceEvolutionEngine(usageTracker, new ArtifactFitnessEvaluator(), ecosystemEngine.pressureEngine())
                : null;
        this.abilityGenerator = new ProceduralAbilityGenerator(
                new AbilityRegistry(),
                config.enableEcosystemBias() ? ecosystemEngine : null,
                lineageRegistry,
                lineageInfluenceResolver,
                experienceEvolutionEngine,
                config.enableTraitInteractions());
    }

    public void runAndWriteOutputs() throws IOException {
        List<SimulatedPlayer> players = generatePlayers();
        latestPlayers = players;
        for (int season = 1; season <= config.seasonCount(); season++) {
            for (int s = 0; s < config.sessionsPerSeason(); s++) {
                simulateRound(players);
                clock.advanceDay();
            }
            metrics.closeSeasonSnapshot();
            seasonalSnapshots.add(captureSeasonSnapshot(players, season));
            exportSeasonInteractionHeatmap(players, season);
        }
        writeReports();
    }

    private List<SimulatedPlayer> generatePlayers() {
        List<SimulatedPlayer> players = new ArrayList<>();
        for (int i = 0; i < config.playerCount(); i++) {
            SimulatedPlayer.BehaviorProfile profile = new SimulatedPlayer.BehaviorProfile(
                    random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(),
                    random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble());
            List<SimulatedArtifactAgent> artifacts = new ArrayList<>();
            for (int j = 0; j < config.artifactsPerPlayer(); j++) {
                long artifactSeed = deterministicSeed(i, j);
                initialSeedPool.add(artifactSeed);
                Artifact artifact = new Artifact(UUID.randomUUID(), ArtifactNameGenerator.generateFromSeed(artifactSeed));
                artifact.resetMutableState();
                seedFactory.applySeedProfile(artifact, artifactSeed);
                artifacts.add(new SimulatedArtifactAgent(artifact));
            }
            metrics.recordPlayerProfile(profile);
            players.add(new SimulatedPlayer(UUID.randomUUID(), profile, artifacts));
        }
        return players;
    }

    private void simulateRound(List<SimulatedPlayer> players) {
        for (SimulatedPlayer player : players) {
            SimulatedSession session = generateSession(player.profile());
            for (SimulatedArtifactAgent agent : player.artifacts()) {
                boolean hadBoss = false;
                boolean lowHealth = false;
                boolean survivedLowHealth = false;
                boolean chain = false;
                for (SimulatedEncounter encounter : session.encounters()) {
                    hadBoss |= encounter.type() == SimulatedEncounter.Type.BOSS;
                    lowHealth |= encounter.lowHealthMoment();
                    chain |= encounter.chainCombat();
                    applyEncounter(agent, player.profile(), encounter);
                    survivedLowHealth |= encounter.lowHealthMoment() && random.nextDouble() < (0.45D + player.profile().survival() * 0.4D);
                }
                metrics.recordSession(session.durationMinutes(), hadBoss, lowHealth, survivedLowHealth, chain);
                metrics.recordArtifact(agent.artifact());
            }
        }
    }

    private SimulatedSession generateSession(SimulatedPlayer.BehaviorProfile profile) {
        int duration = 10 + (int) Math.round((profile.sessionLength() * 55));
        List<SimulatedEncounter> encounters = new ArrayList<>();
        for (int i = 0; i < config.encounterDensity(); i++) {
            double bossPressure = config.bossFrequency() + (profile.bossSeeking() * 0.15D);
            SimulatedEncounter.Type type = random.nextDouble() < bossPressure ? SimulatedEncounter.Type.BOSS
                    : random.nextDouble() < 0.30D ? SimulatedEncounter.Type.MULTI_TARGET
                    : random.nextDouble() < 0.20D ? SimulatedEncounter.Type.EXPLORATION
                    : SimulatedEncounter.Type.NORMAL_COMBAT;
            boolean lowHp = random.nextDouble() < (config.lowHealthEventRate() + profile.riskTolerance() * 0.2D);
            boolean chain = type == SimulatedEncounter.Type.MULTI_TARGET || random.nextDouble() < (profile.aggression() * 0.5D);
            encounters.add(new SimulatedEncounter(type, lowHp, chain));
        }
        return new SimulatedSession(duration, encounters);
    }

    private void applyEncounter(SimulatedArtifactAgent agent, SimulatedPlayer.BehaviorProfile profile, SimulatedEncounter encounter) {
        var rep = agent.reputation();
        usageTracker.trackUse(agent.artifact());
        if (encounter.type() == SimulatedEncounter.Type.BOSS) {
            rep.recordBossKill();
            usageTracker.trackKillParticipation(agent.artifact());
            if (random.nextDouble() < config.memoryEventMultiplier()) {
                memoryEngine.recordAndProfile(agent.artifact(), ArtifactMemoryEvent.FIRST_BOSS_KILL);
            }
        }
        rep.recordKill();
        if (encounter.chainCombat()) {
            rep.recordKillChain(2 + random.nextInt(5));
            if (random.nextDouble() < config.memoryEventMultiplier()) {
                memoryEngine.recordAndProfile(agent.artifact(), ArtifactMemoryEvent.MULTIKILL_CHAIN);
            }
        }
        if (encounter.lowHealthMoment()) {
            rep.recordSurvival();
            if (random.nextDouble() < config.memoryEventMultiplier()) {
                memoryEngine.recordAndProfile(agent.artifact(), ArtifactMemoryEvent.LOW_HEALTH_SURVIVAL);
            }
        }
        if (random.nextDouble() < profile.precision()) rep.recordPrecision();
        if (random.nextDouble() < profile.aggression()) rep.recordBrutality();
        if (random.nextDouble() < profile.mobility()) rep.recordMobility();
        if (random.nextDouble() < (profile.chaos() * config.chaosEventRate())) {
            rep.recordChaos();
            memoryEngine.recordAndProfile(agent.artifact(), ArtifactMemoryEvent.CHAOS_RAMPAGE);
        }
        if (random.nextDouble() < profile.consistency()) rep.recordConsistency();

        evolutionEngine.evaluate(null, agent.artifact(), rep);
        ArtifactMemoryProfile memory = memoryEngine.profile(agent.artifact());
        if (random.nextDouble() < (0.08D * config.mutationPressureMultiplier()) || driftEngine.shouldDrift(rep)) {
            driftEngine.applyDriftSimulation(agent.artifact(), rep);
        }
        boolean awakened = awakeningEngine.evaluateSimulation(agent.artifact(), rep);
        boolean fused = fusionEngine.evaluateSimulation(agent.artifact(), rep);
        if (awakened) {
            usageTracker.trackAwakening(agent.artifact());
            memoryEngine.recordAndProfile(agent.artifact(), ArtifactMemoryEvent.AWAKENING);
            if (random.nextDouble() < 0.02D) lineageRegistry.assignLineage(agent.artifact()).applyMutation(new LineageMutation("awakening", "precision", 0.01D));
        }
        if (fused) {
            usageTracker.trackFusionParticipation(agent.artifact());
            memoryEngine.recordAndProfile(agent.artifact(), ArtifactMemoryEvent.FUSION);
            if (random.nextDouble() < 0.02D) lineageRegistry.assignLineage(agent.artifact()).applyMutation(new LineageMutation("fusion", "survival", 0.01D));
        }

        int stage = Math.max(1, Math.min(5, rep.getTotalScore() / 20));
        AbilityProfile abilityProfile = abilityGenerator.generate(agent.artifact(), stage, memory);
        AbilityMutationResult mutationResult = mutationEngine.mutate(agent.artifact(), abilityProfile.abilities(), memory, agent.artifact().getDriftLevel() > 0);
        List<AbilityDefinition> finalDefinitions = mutationResult.abilities();
        if (!finalDefinitions.isEmpty()) {
            var tree = branchResolver.resolveTree(finalDefinitions.get(0).id(), agent.artifact(), memory, stage, encounter.type().name());
            agent.artifact().setLastAbilityBranchPath(tree.selectedBranch());
        }
        agent.artifact().setLastMutationHistory(String.valueOf(mutationResult.mutations().size()));
        agent.setAbilityProfile(new AbilityProfile(abilityProfile.profileId(), finalDefinitions));
        metrics.recordAbilityProfile(agent.abilityProfile());
    }


    private void exportSeasonInteractionHeatmap(List<SimulatedPlayer> players, int season) throws IOException {
        List<Artifact> artifacts = new ArrayList<>();
        for (SimulatedPlayer player : players) {
            for (SimulatedArtifactAgent agent : player.artifacts()) {
                artifacts.add(agent.artifact());
            }
        }
        TraitInteractionAnalyzer analyzer = new TraitInteractionAnalyzer();
        var matrix = analyzer.analyze(artifacts, lineageRegistry.lineages().values());
        Path seasonDir = Path.of("analytics/visualizations/seasons", "season-" + season);
        new InteractionHeatmapExporter().export(
                matrix,
                seasonDir.resolve("trait-interaction-heatmap.png"),
                seasonDir.resolve("trait-interaction-matrix.csv"),
                seasonDir.resolve("trait-interaction-matrix.json")
        );
        DashboardService dashboardService = new DashboardService(Path.of("analytics"));
        dashboardService.generateSeasonDashboard(season);
    }

    private void writeReports() throws IOException {
        Path out = Path.of(config.outputDirectory());
        Files.createDirectories(out);
        Map<String, Object> data = metrics.asData();
        data.put("seasonal_snapshots", seasonalSnapshots);
        data.put("initial_seed_pool", initialSeedPool);
        ArtifactEcosystemBalancingAI ai = new ArtifactEcosystemBalancingAI();
        EcosystemHealthReport report = ai.evaluate(metrics.families(), metrics.branches(), metrics.mutations(), metrics.triggers(), metrics.mechanics(), metrics.memories());
        WorldEcosystemProfile profile = new WorldEcosystemProfile(
                dataRate(data, "ability", "memory_driven_ability_frequency"),
                dataRate(data, "player", "kill_chain_rate"),
                dataRate(data, "player", "boss_engagement_rate"),
                1.0D - dataRate(data, "player", "low_health_survival_rate"),
                1.0D - dataRate(data, "world", "dominant_family_rate"),
                dataRate(data, "player", "boss_engagement_rate"),
                dataRate(data, "ability", "memory_driven_ability_frequency"),
                dataRate(data, "world", "dead_branch_rate"));
        if (config.enableSelfBalancingAdjustments() || config.enableDiversityPreservation() || config.enableEnvironmentalPressure()) {
            ecosystemEngine.evaluate(profile, report, metrics);
        }
        WorldSimulationReportBuilder builder = new WorldSimulationReportBuilder();

        Files.writeString(out.resolve("world-sim-data.json"), toJson(data, 0));
        Files.writeString(out.resolve("world-sim-report.md"), builder.reportMarkdown(config, data));
        Files.writeString(out.resolve("world-sim-meta-shifts.md"), builder.metaShiftMarkdown(metrics));
        Files.writeString(out.resolve("world-sim-balance-findings.md"), builder.balanceFindings(report));

        List<Artifact> allArtifacts = new ArrayList<>();
        for (SimulatedPlayer player : latestPlayers) {
            for (SimulatedArtifactAgent agent : player.artifacts()) {
                allArtifacts.add(agent.artifact());
            }
        }
        TraitInteractionAnalyzer interactionAnalyzer = new TraitInteractionAnalyzer();
        var matrix = interactionAnalyzer.analyze(allArtifacts, lineageRegistry.lineages().values(), seasonalSnapshots);
        new InteractionHeatmapExporter().export(
                matrix,
                Path.of("analytics/visualizations/trait-interaction-heatmap.png"),
                Path.of("analytics/visualizations/trait-interaction-matrix.csv"),
                Path.of("analytics/visualizations/trait-interaction-matrix.json")
        );
        new TraitInteractionReportWriter().write(Path.of("analytics/trait-interaction-report.md"), matrix);
        DashboardService dashboardService = new DashboardService(Path.of("analytics"));
        dashboardService.regenerateDashboard();
        dashboardService.generateSeasonDashboard(1);
        dashboardService.generateSeasonDashboard(2);
        dashboardService.generateSeasonDashboard(3);

        Files.writeString(Path.of("analytics/lineage-report.md"), builder.lineageEvolutionMarkdown(data));
        Files.writeString(Path.of("analytics/lineage-distribution.json"), toJson(data.get("lineage"), 0));
        Files.writeString(Path.of("analytics/world-lab/lineage-evolution.md"), builder.lineageEvolutionMarkdown(data));
        writeTraitProjectionPerformanceReport();
    }

    private void writeTraitProjectionPerformanceReport() throws IOException {
        TraitProjectionStats stats = abilityGenerator.traitProjectionStats();
        Path output = Path.of("analytics/performance/trait-projection-report.md");
        Files.createDirectories(output.getParent());
        String report = "# Trait Projection Performance Report\n\n"
                + "1. **Scoring method used:** Vectorized genome/ability dot-product projection with bounded LRU cache.\n"
                + "2. **Cache hit rate:** " + String.format(Locale.ROOT, "%.2f%%", stats.cacheHitRate() * 100.0D) + "\n"
                + "3. **Number of scored genomes:** " + stats.scoringCalls() + "\n"
                + "4. **Estimated speed improvement:** " + String.format(Locale.ROOT, "%.2fx", stats.estimatedSpeedupX()) + "\n"
                + "5. **Bottlenecks still remaining:** branch resolution and mutation phase still run per-artifact and dominate at high lineage counts.\n\n"
                + "## Projection Metrics\n"
                + "- Optimized scoring enabled: " + stats.optimizedEnabled() + "\n"
                + "- Ability vectors loaded: " + stats.abilityVectorCount() + "\n"
                + "- Trait vector dimensionality: " + stats.dimensions() + "\n"
                + "- Cache size/capacity: " + stats.cacheSize() + "/" + stats.cacheCapacity() + "\n"
                + "- Cache hits: " + stats.cacheHits() + "\n"
                + "- Cache misses: " + stats.cacheMisses() + "\n"
                + "- Average scoring time: " + String.format(Locale.ROOT, "%.3f us", stats.averageScoringMicros()) + "\n";
        Files.writeString(output, report);
    }

    private long deterministicSeed(int playerIndex, int artifactIndex) {
        long mixed = config.seed() ^ (((long) playerIndex) << 32) ^ (artifactIndex * 0x9E3779B97F4A7C15L);
        return mixed * 6364136223846793005L + 1442695040888963407L;
    }

    private Map<String, Object> captureSeasonSnapshot(List<SimulatedPlayer> players, int season) {
        Map<String, Integer> family = new HashMap<>();
        Map<String, Integer> branch = new HashMap<>();
        Map<String, Integer> lineage = new HashMap<>();
        Map<String, Integer> mutations = new HashMap<>();
        for (SimulatedPlayer player : players) {
            for (SimulatedArtifactAgent agent : player.artifacts()) {
                Artifact artifact = agent.artifact();
                branch.merge(artifact.getLastAbilityBranchPath(), 1, Integer::sum);
                lineage.merge(artifact.getLatentLineage(), 1, Integer::sum);
                mutations.merge(artifact.getLastMutationHistory(), 1, Integer::sum);
                AbilityProfile profile = agent.abilityProfile();
                if (profile != null) {
                    for (AbilityDefinition definition : profile.abilities()) {
                        family.merge(definition.family().name().toLowerCase(Locale.ROOT), 1, Integer::sum);
                    }
                }
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("season", season);
        out.put("families", family);
        out.put("branches", branch);
        out.put("lineages", lineage);
        out.put("mutations", mutations);
        return out;
    }

    public List<Map<String, Object>> seasonalSnapshots() {
        return List.copyOf(seasonalSnapshots);
    }

    public List<Long> initialSeedPool() {
        return List.copyOf(initialSeedPool);
    }

    private double dataRate(Map<String, Object> data, String section, String key) {
        Object v = ((Map<?, ?>) data.get(section)).get(key);
        return v instanceof Number n ? n.doubleValue() : 0.0D;
    }

    @SuppressWarnings("unchecked")
    private String toJson(Object value, int indent) {
        String pad = "  ".repeat(indent);
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{\n");
            Iterator<? extends Map.Entry<?, ?>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                var e = it.next();
                sb.append(pad).append("  \"").append(e.getKey()).append("\": ").append(toJson(e.getValue(), indent + 1));
                if (it.hasNext()) sb.append(',');
                sb.append('\n');
            }
            return sb.append(pad).append('}').toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(toJson(list.get(i), indent + 1));
            }
            return sb.append(']').toString();
        }
        if (value instanceof String s) {
            return "\"" + s.replace("\"", "\\\"") + "\"";
        }
        return String.valueOf(value);
    }
}
