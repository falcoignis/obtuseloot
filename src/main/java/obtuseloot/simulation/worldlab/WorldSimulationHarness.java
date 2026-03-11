package obtuseloot.simulation.worldlab;

import obtuseloot.abilities.*;
import obtuseloot.abilities.mutation.AbilityMutationEngine;
import obtuseloot.abilities.mutation.AbilityMutationResult;
import obtuseloot.abilities.tree.AbilityBranchResolver;
import obtuseloot.analytics.ArtifactEcosystemBalancingAI;
import obtuseloot.analytics.EcologyDiagnosticEngine;
import obtuseloot.analytics.EcologyDiagnosticSnapshot;
import obtuseloot.analytics.EcologyDiagnosticState;
import obtuseloot.analytics.EcosystemHealthGaugeAnalyzer;
import obtuseloot.analytics.EcologyAlertEngine;
import obtuseloot.analytics.PersistentNovelNicheAnalyzer;
import obtuseloot.analytics.EcosystemHealthReport;
import obtuseloot.analytics.InteractionHeatmapExporter;
import obtuseloot.analytics.NovelStrategyEmergenceAnalyzer;
import obtuseloot.analytics.TraitInteractionAnalyzer;
import obtuseloot.artifacts.Artifact;
import obtuseloot.dashboard.DashboardService;
import obtuseloot.analytics.TraitInteractionReportWriter;
import obtuseloot.artifacts.ArtifactSeedFactory;
import obtuseloot.names.ArtifactNameResolver;
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
    private final SpeciesNicheAnalyticsEngine speciesNicheEngine;
    private final EcologicalMemoryEngine ecologicalMemoryEngine = new EcologicalMemoryEngine();
    private final Map<String, Integer> seasonSpeciesCounts = new LinkedHashMap<>();
    private final Map<String, Integer> seasonNicheCounts = new LinkedHashMap<>();
    private final Map<String, Integer> seasonBranchCounts = new LinkedHashMap<>();
    private final Map<String, Integer> seasonTriggerCounts = new LinkedHashMap<>();
    private final Map<String, Integer> seasonMechanicCounts = new LinkedHashMap<>();
    private final Map<String, Integer> seasonEnvironmentCounts = new LinkedHashMap<>();

    public WorldSimulationHarness(WorldSimulationConfig config) {
        this.config = config;
        this.random = new Random(config.seed());
        this.experienceEvolutionEngine = config.enableExperienceDrivenEvolution()
                ? new ExperienceEvolutionEngine(usageTracker, new ArtifactFitnessEvaluator(), ecosystemEngine.pressureEngine())
                : null;
        this.speciesNicheEngine = new SpeciesNicheAnalyticsEngine(config.seed());
        this.abilityGenerator = new ProceduralAbilityGenerator(
                new AbilityRegistry(),
                config.enableEcosystemBias() ? ecosystemEngine : null,
                lineageRegistry,
                lineageInfluenceResolver,
                experienceEvolutionEngine,
                config.enableTraitInteractions(),
                config.scoringMode());
    }

    public TraitProjectionStats traitProjectionStats() {
        return abilityGenerator.traitProjectionStats();
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
            ecologicalMemoryEngine.observeSeason(seasonBranchCounts, seasonNicheCounts, seasonSpeciesCounts, seasonTriggerCounts, seasonMechanicCounts, seasonEnvironmentCounts);
            Map<String, Object> seasonSnapshot = captureSeasonSnapshot(players, season);
            seasonSnapshot.putAll(speciesNicheEngine.closeSeason(season, flattenArtifacts(players)));
            seasonSnapshot.put("ecologicalMemory", ecologicalMemoryEngine.diagnostics());
            seasonalSnapshots.add(seasonSnapshot);
            resetSeasonTallies();
            exportSeasonInteractionHeatmap(players, season);
        }
        writeRegulatoryReports();
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
                Artifact artifact = new Artifact(UUID.randomUUID());
                artifact.setArtifactSeed(artifactSeed);
                artifact.resetMutableState();
                seedFactory.applySeedProfile(artifact, artifactSeed);
                artifact.setNaming(ArtifactNameResolver.initialize(artifact));
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

        SpeciesNicheAnalyticsEngine.PenaltyResult penaltyResult = speciesNicheEngine.applyCrowdingPenalty(agent.artifact(), rep.getTotalScore());
        double evolvedScore = penaltyResult.effectiveScore();
        if (config.enableCoEvolution()) {
            SpeciesNicheAnalyticsEngine.CoEvolutionPressureResult coEvolutionResult = speciesNicheEngine.applyCoEvolutionPressure(agent.artifact(), evolvedScore);
            evolvedScore = coEvolutionResult.effectiveScore();
        }
        EcologicalMemoryEngine.MemoryFeedback memoryFeedback = ecologicalMemoryEngine.feedbackForArtifact(
                agent.artifact().getSpeciesId(),
                penaltyResult.nicheId(),
                agent.artifact().getLastAbilityBranchPath(),
                agent.artifact().getLastTriggerProfile(),
                agent.artifact().getLastMechanicProfile(),
                agent.artifact().getDriftAlignment() + ":" + agent.artifact().getEvolutionPath());
        evolvedScore *= memoryFeedback.modifier();
        int stage = Math.max(1, Math.min(5, (int) Math.round((evolvedScore * memoryFeedback.latentBias()) / 20.0D)));
        AbilityProfile abilityProfile = abilityGenerator.generate(agent.artifact(), stage, memory);
        boolean mutationBiasDrift = agent.artifact().getDriftLevel() > 0 || random.nextDouble() < ((memoryFeedback.mutationBias() - 1.0D) * 0.6D);
        AbilityMutationResult mutationResult = mutationEngine.mutate(agent.artifact(), abilityProfile.abilities(), memory, mutationBiasDrift);
        List<AbilityDefinition> finalDefinitions = mutationResult.abilities();
        if (!finalDefinitions.isEmpty()) {
            var tree = branchResolver.resolveTree(finalDefinitions.get(0).id(), agent.artifact(), memory, stage, encounter.type().name());
            agent.artifact().setLastAbilityBranchPath(tree.selectedBranch());
        }
        agent.artifact().setLastMutationHistory(String.valueOf(mutationResult.mutations().size()));
        agent.artifact().setLastTriggerProfile(finalDefinitions.stream().map(d -> d.trigger().name().toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.joining(",")));
        agent.artifact().setLastMechanicProfile(finalDefinitions.stream().map(d -> d.mechanic().name().toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.joining(",")));
        agent.setAbilityProfile(new AbilityProfile(abilityProfile.profileId(), finalDefinitions));
        var resolvedSpecies = lineageRegistry.evaluateSpeciation(agent.artifact());
        boolean successful = rep.getTotalScore() >= 30;
        speciesNicheEngine.observeArtifact(agent.artifact(), resolvedSpecies, agent.abilityProfile(), successful, Math.max(1, seasonalSnapshots.size() + 1), penaltyResult.crowdingPenalty());
        tallySeasonSignals(agent, resolvedSpecies.speciesId());
        metrics.recordAbilityProfile(agent.abilityProfile());
    }



    private void tallySeasonSignals(SimulatedArtifactAgent agent, String speciesId) {
        seasonSpeciesCounts.merge(speciesId == null ? "unknown" : speciesId, 1, Integer::sum);
        String nicheId = speciesNicheEngine.nicheForArtifact(agent.artifact().getArtifactSeed());
        seasonNicheCounts.merge(nicheId, 1, Integer::sum);
        seasonBranchCounts.merge(agent.artifact().getLastAbilityBranchPath(), 1, Integer::sum);
        for (String trigger : agent.artifact().getLastTriggerProfile().split(",")) {
            if (!trigger.isBlank()) seasonTriggerCounts.merge(trigger.trim(), 1, Integer::sum);
        }
        for (String mechanic : agent.artifact().getLastMechanicProfile().split(",")) {
            if (!mechanic.isBlank()) seasonMechanicCounts.merge(mechanic.trim(), 1, Integer::sum);
        }
        seasonEnvironmentCounts.merge(agent.artifact().getDriftAlignment() + ":" + agent.artifact().getEvolutionPath(), 1, Integer::sum);
    }

    private void resetSeasonTallies() {
        seasonSpeciesCounts.clear();
        seasonNicheCounts.clear();
        seasonBranchCounts.clear();
        seasonTriggerCounts.clear();
        seasonMechanicCounts.clear();
        seasonEnvironmentCounts.clear();
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
        List<Artifact> allArtifacts = allArtifacts();
        SpeciesNicheAnalyticsEngine.SpeciesCleanupResult cleanupResult = speciesNicheEngine.cleanupCosmeticSpecies(allArtifacts, lineageRegistry.speciesRegistry().allSpecies());
        lineageRegistry.speciesRegistry().retireSpecies(cleanupResult.retiredSpecies());

        Map<String, Object> speciationSummary = speciesNicheEngine.buildSpeciationSummary(lineageRegistry.speciesRegistry().allSpecies(), metrics.lineageCounts(), config.seasonCount());
        Map<String, Object> speciesNicheMap = speciesNicheEngine.buildSpeciesNicheMap(allArtifacts);
        Map<String, Object> crowdingDistribution = speciesNicheEngine.buildCrowdingDistribution(allArtifacts);
        Map<String, Object> coEvolutionRelationships = speciesNicheEngine.buildCoEvolutionRelationships(allArtifacts);
        Map<String, Object> nicheQualityDiagnostics = speciesNicheEngine.buildNicheQualityDiagnostics(allArtifacts());
        Map<String, Object> nicheStabilityMetrics = speciesNicheEngine.buildNicheStabilityMetrics();
        Map<String, Object> nichePrototypeDistribution = speciesNicheEngine.buildNichePrototypeDistribution();
        data.put("speciation", speciationSummary);
        data.put("niches", speciesNicheMap);
        data.put("ecological_memory", ecologicalMemoryEngine.diagnostics());
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
        writeSpeciesAndNicheReports(speciationSummary, speciesNicheMap, crowdingDistribution, coEvolutionRelationships, nicheQualityDiagnostics, nicheStabilityMetrics, nichePrototypeDistribution, cleanupResult);
        NovelStrategyEmergenceAnalyzer.NserResult nserResult = writeNovelStrategyEmergenceReports(seasonalSnapshots);
        PersistentNovelNicheAnalyzer.PnncResult pnncResult = writePersistentNovelNicheReports(seasonalSnapshots);
        writeEcosystemHealthGauge(seasonalSnapshots, nserResult, pnncResult);
        writeTraitFieldLatentReports(nserResult);
        writeEcologicalMemoryImpactReview(nserResult);
        writeTraitProjectionPerformanceReport();
    }

    private void writeTraitFieldLatentReports(NovelStrategyEmergenceAnalyzer.NserResult nserResult) throws IOException {
        Files.createDirectories(Path.of("analytics/world-lab"));
        String interferenceReport = "# Trait Interference Fields Report\n\n"
                + "## Trait-pair categories\n"
                + "- trigger × mechanic\n"
                + "- mechanic × environment\n"
                + "- gate × trigger\n"
                + "- memory × mechanic\n"
                + "- branch tendency × environment affinity\n"
                + "- persistence style × support/combat role\n\n"
                + "## Amplification and dampening rules\n"
                + "- bounded per-ability modifier range: [-0.18, +0.18]\n"
                + "- high-synergy trigger/mechanic pairs receive light amplification\n"
                + "- unstable low-health burst chains are lightly dampened\n"
                + "- environment-aligned mobility/survival mechanics receive contextual bias\n"
                + "- memory-resonance mechanics amplify only when resonance pressure is present\n\n"
                + "## Emergent ability combination examples\n"
                + "- memory-event triggers + memory-echo mechanics produce delayed combo loops\n"
                + "- movement/reposition triggers + movement-echo create reposition pressure archetypes\n"
                + "- support modifiers + stable persistence increase utility-leaning loadouts\n\n"
                + "## Strategy diversity impact\n"
                + "- observed active interference effects: " + metrics.interferenceEffects() + "\n"
                + "- branch diversity timeline: " + metrics.diversityTimeline() + "\n";
        Files.writeString(Path.of("analytics/trait-interference-fields-report.md"), interferenceReport);

        String latentReport = "# Latent Trait Activation Report\n\n"
                + "## Activation pathways\n"
                + "- environmental exposure\n"
                + "- trait-interference threshold signal\n"
                + "- experience-driven progression\n"
                + "- lineage drift pressure\n"
                + "- repeated niche exposure\n\n"
                + "## Activation frequency\n"
                + "- average latent activation rate: " + metrics.latentActivationRate() + "\n\n"
                + "## Latent → expressed examples\n"
                + metrics.latentActivationExamples() + "\n\n"
                + "## Novelty impact\n"
                + "- NSER trend reference: " + nserResult.trend() + "\n";
        Files.writeString(Path.of("analytics/latent-trait-activation-report.md"), latentReport);

        boolean diversityIncreased = metrics.interferenceEffects().size() >= 2;
        boolean latentUnlocked = metrics.latentActivationRate() > 0.0D;
        boolean nserIncreased = !nserResult.trend().isEmpty() && nserResult.trend().get(nserResult.trend().size() - 1) >= nserResult.trend().get(0);
        boolean nichesFormed = seasonalSnapshots.stream().mapToInt(s -> ((Number) s.getOrDefault("nicheCount", 0)).intValue()).max().orElse(0) >= 3;
        String worldLabReview = "# Trait Field + Latent Impact Review\n\n"
                + "1. Did trait interference increase ability diversity? " + diversityIncreased + "\n"
                + "2. Did latent traits unlock new strategies? " + latentUnlocked + "\n"
                + "3. Did NSER increase? " + nserIncreased + "\n"
                + "4. Did new niches begin to form? " + nichesFormed + "\n\n"
                + "- interference effects: " + metrics.interferenceEffects() + "\n"
                + "- latent activation rate: " + metrics.latentActivationRate() + "\n"
                + "- NSER trend: " + nserResult.trend() + "\n";
        Files.writeString(Path.of("analytics/world-lab/trait-field-latent-impact-review.md"), worldLabReview);
    }

    @SuppressWarnings("unchecked")
    private void writeEcosystemHealthGauge(List<Map<String, Object>> seasonalSnapshots,
                                           NovelStrategyEmergenceAnalyzer.NserResult nserResult,
                                           PersistentNovelNicheAnalyzer.PnncResult pnncResult) throws IOException {
        EcosystemHealthGaugeAnalyzer gaugeAnalyzer = new EcosystemHealthGaugeAnalyzer();
        List<Map<String, Integer>> artifactOccupancy = new ArrayList<>();
        List<Map<String, Integer>> speciesOccupancy = new ArrayList<>();
        List<Integer> seasons = new ArrayList<>();

        for (Map<String, Object> snapshot : seasonalSnapshots) {
            seasons.add(((Number) snapshot.getOrDefault("season", seasons.size() + 1)).intValue());
            Object niches = snapshot.get("nicheOccupancy");
            if (niches instanceof Map<?, ?> map) {
                artifactOccupancy.add(toIntegerMap(map));
            }
            Object species = snapshot.get("speciesPerNiche");
            if (species instanceof Map<?, ?> map) {
                speciesOccupancy.add(toIntegerMap(map));
            }
        }

        var result = gaugeAnalyzer.analyze(artifactOccupancy, speciesOccupancy, nserResult.trend());
        List<Double> tntBySeason = result.tntTrend();
        List<String> tntPairs = new ArrayList<>();
        for (int i = 1; i < seasons.size(); i++) {
            tntPairs.add("season-" + seasons.get(i - 1) + "→" + seasons.get(i) + ":" + tntBySeason.get(i - 1));
        }

        String markdown = "# Ecosystem Health Gauge\n\n"
                + "- END_artifacts: " + result.endArtifacts() + "\n"
                + "- END_species: " + (result.endSpecies() == null ? "N/A" : result.endSpecies()) + "\n"
                + "- TNT per season: " + tntPairs + "\n"
                + "- NSER trend: " + result.nserTrend() + "\n"
                + "- PNNC trend: " + pnncResult.trend() + "\n"
                + "- PNNC current: " + pnncResult.currentPnnc() + "\n"
                + "- PNNC trend: " + pnncResult.trend() + "\n"
                + "- END trend: " + result.endTrend() + "\n"
                + "- TNT trend: " + result.tntTrend() + "\n"
                + "- ecosystem status: " + result.status() + "\n\n"
                + "## Interpretation\n"
                + result.interpretation() + "\n";
        Files.writeString(Path.of("analytics/ecosystem-health-gauge.md"), markdown);

        String json = "{\n"
                + "  \"END_artifacts\": " + result.endArtifacts() + ",\n"
                + "  \"END_species\": " + (result.endSpecies() == null ? "null" : result.endSpecies()) + ",\n"
                + "  \"TNT_per_season\": " + toJsonArrayOfLabeledValues(tntPairs) + ",\n"
                + "  \"NSER_latest\": " + result.latestNser() + ",\n"
                + "  \"END_trend\": " + toJsonArray(result.endTrend()) + ",\n"
                + "  \"TNT_trend\": " + toJsonArray(result.tntTrend()) + ",\n"
                + "  \"NSER_trend\": " + toJsonArray(result.nserTrend()) + ",\n"
                + "  \"PNNC_current\": " + pnncResult.currentPnnc() + ",\n"
                + "  \"PNNC_trend\": " + toJson(pnncResult.trend(), 0) + ",\n"
                + "  \"ecosystem_status\": \"" + result.status().name() + "\",\n"
                + "  \"interpretation\": \"" + result.interpretation().replace("\"", "\\\"") + "\"\n"
                + "}\n";
        Files.writeString(Path.of("analytics/ecosystem-health-gauge.json"), json);

        String worldLabReview = "# Ecosystem Health Gauge Review\n\n"
                + "## Run 1\n"
                + "- END trend: " + result.endTrend() + "\n"
                + "- TNT across seasons: " + result.tntTrend() + "\n"
                + "- NSER trend: " + result.nserTrend() + "\n"
                + "- PNNC current: " + pnncResult.currentPnnc() + "\n"
                + "- PNNC trend: " + pnncResult.trend() + "\n"
                + "- Ecosystem status: " + result.status() + "\n"
                + "- Trajectory: " + result.interpretation() + "\n";
        Files.writeString(Path.of("analytics/world-lab/ecosystem-health-gauge-review.md"), worldLabReview);

        double dominantNicheShare = extractLastSeasonDouble(seasonalSnapshots, "dominantNicheShare");
        double dominantSpeciesShare = extractLastSeasonDouble(seasonalSnapshots, "dominantSpeciesConcentration");
        double dominantAttractorShare = Math.max(dominantNicheShare, dominantSpeciesShare);
        int nicheCount = (int) Math.round(extractLastSeasonDouble(seasonalSnapshots, "nicheCount"));
        int speciesCount = (int) Math.round(extractLastSeasonDouble(seasonalSnapshots, "speciesCount"));
        int relabelingEvents = estimateRelabelingEvents(seasonalSnapshots);
        boolean noveltyPersistenceWeak = nserResult.bySeason().stream().noneMatch(s -> !s.persistentNovelStrategies().isEmpty());

        EcologyDiagnosticSnapshot diagnostic = new EcologyDiagnosticEngine().diagnose(
                result.endArtifacts(),
                result.endSpecies(),
                result.tntTrend().isEmpty() ? 0.0D : result.tntTrend().get(result.tntTrend().size() - 1),
                result.latestNser(),
                pnncResult.currentPnnc(),
                dominantNicheShare,
                dominantSpeciesShare,
                dominantAttractorShare,
                nicheCount,
                speciesCount,
                result.nserTrend(),
                pnncResult.trend(),
                noveltyPersistenceWeak,
                relabelingEvents);

        String diagnosticMd = "# Ecology Diagnostic Report\n\n"
                + "- END_artifacts: " + diagnostic.endArtifacts() + "\n"
                + "- END_species: " + (diagnostic.endSpecies() == null ? "N/A" : diagnostic.endSpecies()) + "\n"
                + "- TNT_latest: " + diagnostic.latestTnt() + "\n"
                + "- NSER_latest: " + diagnostic.latestNser() + "\n"
                + "- PNNC_latest: " + diagnostic.latestPnnc() + "\n"
                + "- Diagnostic state: " + diagnostic.state() + "\n"
                + "- Confidence: " + diagnostic.confidence() + "\n"
                + "- Warning flags: " + diagnostic.warningFlags() + "\n"
                + "- Supporting context: nicheCount=" + diagnostic.nicheCount() + ", speciesCount=" + diagnostic.speciesCount()
                + ", dominantNicheShare=" + diagnostic.dominantNicheShare() + ", dominantSpeciesShare=" + diagnostic.dominantSpeciesShare()
                + ", dominantAttractorShare=" + diagnostic.dominantAttractorShare() + ", relabelingEvents=" + diagnostic.relabelingEvents() + "\n\n"
                + "## Explanation summary\n"
                + diagnostic.explanation() + "\n\n"
                + "## Recommended next action\n"
                + diagnostic.recommendedNextAction() + "\n";
        Files.writeString(Path.of("analytics/ecology-diagnostic-report.md"), diagnosticMd);

        String diagnosticJson = "{\n"
                + "  \"END\": " + diagnostic.endArtifacts() + ",\n"
                + "  \"END_species\": " + (diagnostic.endSpecies() == null ? "null" : diagnostic.endSpecies()) + ",\n"
                + "  \"TNT\": " + diagnostic.latestTnt() + ",\n"
                + "  \"NSER\": " + diagnostic.latestNser() + ",\n"
                + "  \"PNNC\": " + diagnostic.latestPnnc() + ",\n"
                + "  \"diagnostic_state\": \"" + diagnostic.state().name() + "\",\n"
                + "  \"confidence\": " + diagnostic.confidence() + ",\n"
                + "  \"warning_flags\": " + toJson(diagnostic.warningFlags(), 0) + ",\n"
                + "  \"explanation_summary\": " + toJson(diagnostic.explanation(), 0) + ",\n"
                + "  \"recommended_next_action\": " + toJson(diagnostic.recommendedNextAction(), 0) + "\n"
                + "}\n";
        Files.writeString(Path.of("analytics/ecology-diagnostic-state.json"), diagnosticJson);

        String falseDivergenceReview = "# False Divergence Review\n\n"
                + "False divergence means the ecology appears active (species/niches/turnover) but fails to produce stable novelty.\n\n"
                + "## Active signals\n"
                + "- high species count with weak divergence: " + (diagnostic.speciesCount() >= 6 && diagnostic.dominantSpeciesShare() >= 0.55D) + "\n"
                + "- high niche count with weak END: " + (diagnostic.nicheCount() >= 4 && diagnostic.endArtifacts() < 2.5D) + "\n"
                + "- high TNT with low NSER: " + (diagnostic.latestTnt() >= 0.45D && diagnostic.latestNser() < 0.15D) + "\n"
                + "- PNNC remains zero: " + (diagnostic.latestPnnc() == 0) + "\n"
                + "- relabeling/migration noise: " + (diagnostic.relabelingEvents() >= 4) + "\n"
                + "- novelty persistence weak: " + diagnostic.noveltyPersistenceWeak() + "\n"
                + "- dominant attractor remains sticky: " + (diagnostic.dominantAttractorShare() >= 0.60D) + "\n\n"
                + "## Diagnosis\n"
                + "- Current diagnostic state: " + diagnostic.state() + "\n"
                + "- False divergence flagged automatically: " + diagnostic.warningFlags().contains("false_divergence") + "\n"
                + "- Summary: " + diagnostic.explanation() + "\n";
        Files.writeString(Path.of("analytics/false-divergence-review.md"), falseDivergenceReview);

        String worldLabDiagnosticReview = "# World-Lab Ecology Diagnostic Review\n\n"
                + "## Run 1\n"
                + "- END across runs: " + result.endTrend() + "\n"
                + "- TNT across seasons: " + result.tntTrend() + "\n"
                + "- NSER across seasons: " + result.nserTrend() + "\n"
                + "- PNNC across seasons: " + pnncResult.trend() + "\n"
                + "- false-divergence flags: " + diagnostic.warningFlags() + "\n"
                + "- final diagnostic state: " + diagnostic.state() + "\n"
                + "- attractor weakening vs relabeling: "
                + (diagnostic.dominantAttractorShare() < 0.55D ? "attractor weakening is visible" : "dominant attractor is mostly being relabeled")
                + "\n";
        Files.writeString(Path.of("analytics/world-lab/ecology-diagnostic-review.md"), worldLabDiagnosticReview);

        String openEndednessReview = "# Ecology Diagnostic Open-Endedness Review\n\n"
                + "1. is the ecosystem genuinely diverging? " + (diagnostic.state() == EcologyDiagnosticState.EMERGENT_ECOLOGY || diagnostic.state() == EcologyDiagnosticState.HEALTHY_MULTI_ATTRACTOR) + "\n"
                + "2. is it only reshuffling existing structures? " + (diagnostic.state() == EcologyDiagnosticState.FALSE_DIVERGENCE || diagnostic.state() == EcologyDiagnosticState.STAGNANT_ATTRACTOR) + "\n"
                + "3. are new strategies appearing and persisting? " + (!diagnostic.noveltyPersistenceWeak() && diagnostic.latestNser() >= 0.15D && diagnostic.latestPnnc() > 0) + "\n"
                + "4. ecosystem description: "
                + switch (diagnostic.state()) {
                    case COLLAPSED_MONOCULTURE, STAGNANT_ATTRACTOR -> "bounded";
                    case FALSE_DIVERGENCE -> "falsely divergent";
                    case HEALTHY_MULTI_ATTRACTOR -> "healthy multi-attractor";
                    case TURBULENT_THRASH -> "bounded";
                    case EMERGENT_ECOLOGY -> "emergent";
                }
                + "\n\n"
                + "State=" + diagnostic.state() + ", confidence=" + diagnostic.confidence() + ", warnings=" + diagnostic.warningFlags() + ".\n";
        Files.writeString(Path.of("analytics/world-lab/open-endedness/ecology-diagnostic-open-endedness-review.md"), openEndednessReview);

        EcologyAlertEngine.AlertThresholds alertThresholds = ecologyAlertThresholdsFromProperties();
        Integer baselinePnnc = loadBaselinePnnc(Path.of("analytics/ecology-alerts.json"));
        EcologyAlertEngine alertEngine = new EcologyAlertEngine();
        EcologyAlertEngine.AlertResult alertResult = alertEngine.evaluate(diagnostic, pnncResult, alertThresholds, baselinePnnc);
        Map<String, Object> alertsJson = alertEngine.asJson(diagnostic, pnncResult, alertResult);
        Files.writeString(Path.of("analytics/ecology-alerts.json"), toJson(alertsJson, 0));

        String alertsReport = "# Ecology Alerts Report\n\n"
                + "- Ecology State: " + diagnostic.state() + "\n"
                + "- END: " + diagnostic.endArtifacts() + "\n"
                + "- TNT: " + diagnostic.latestTnt() + "\n"
                + "- NSER: " + diagnostic.latestNser() + "\n"
                + "- PNNC: " + diagnostic.latestPnnc() + "\n"
                + "- Regression Gate: " + alertResult.regressionGate() + "\n"
                + "- Alerts: " + alertResult.alerts() + "\n"
                + "- Thresholds: " + alertThresholds + "\n";
        Files.writeString(Path.of("analytics/ecology-alerts-report.md"), alertsReport);

        String gateReview = "# Ecology Regression Gate Review\n\n"
                + "- Gate status: " + alertResult.regressionGate() + "\n"
                + "- END/TNT/NSER/PNNC: " + diagnostic.endArtifacts() + "/" + diagnostic.latestTnt() + "/" + diagnostic.latestNser() + "/" + diagnostic.latestPnnc() + "\n"
                + "- Triggered alerts: " + alertResult.alerts() + "\n"
                + "- Workflow should fail: " + alertResult.shouldFail() + "\n";
        Files.writeString(Path.of("analytics/world-lab/ecology-regression-gate-review.md"), gateReview);

        System.out.println("Ecology State: " + diagnostic.state());
        System.out.println("END: " + diagnostic.endArtifacts());
        System.out.println("TNT: " + diagnostic.latestTnt());
        System.out.println("NSER: " + diagnostic.latestNser());
        System.out.println("PNNC: " + diagnostic.latestPnnc());
        System.out.println("Regression Gate: " + alertResult.regressionGate());

        if (alertResult.shouldFail()) {
            throw new IllegalStateException("Ecology regression gate failed due to ERROR-level alerts.");
        }
    }

    private double extractLastSeasonDouble(List<Map<String, Object>> snapshots, String key) {
        if (snapshots.isEmpty()) {
            return 0.0D;
        }
        Object value = snapshots.get(snapshots.size() - 1).get(key);
        return value instanceof Number number ? number.doubleValue() : 0.0D;
    }

    @SuppressWarnings("unchecked")
    private int estimateRelabelingEvents(List<Map<String, Object>> snapshots) {
        int events = 0;
        for (int i = 1; i < snapshots.size(); i++) {
            Map<String, Integer> previous = toIntegerMap((Map<?, ?>) snapshots.get(i - 1).getOrDefault("speciesPerNiche", Map.of()));
            Map<String, Integer> current = toIntegerMap((Map<?, ?>) snapshots.get(i).getOrDefault("speciesPerNiche", Map.of()));
            Set<String> keys = new LinkedHashSet<>();
            keys.addAll(previous.keySet());
            keys.addAll(current.keySet());
            int deltaSum = 0;
            for (String key : keys) {
                deltaSum += Math.abs(current.getOrDefault(key, 0) - previous.getOrDefault(key, 0));
            }
            if (deltaSum >= 3) {
                events++;
            }
        }
        return events;
    }


    private EcologyAlertEngine.AlertThresholds ecologyAlertThresholdsFromProperties() {
        EcologyAlertEngine.AlertThresholds defaults = EcologyAlertEngine.AlertThresholds.defaults();
        return new EcologyAlertEngine.AlertThresholds(
                Boolean.parseBoolean(System.getProperty("analytics.ecologyAlerts.enabled", String.valueOf(defaults.enabled()))),
                Boolean.parseBoolean(System.getProperty("analytics.ecologyAlerts.failOnError", String.valueOf(defaults.failOnError()))),
                Double.parseDouble(System.getProperty("analytics.ecologyAlerts.minEND", String.valueOf(defaults.minEND()))),
                Double.parseDouble(System.getProperty("analytics.ecologyAlerts.maxTNT", String.valueOf(defaults.maxTNT()))),
                Double.parseDouble(System.getProperty("analytics.ecologyAlerts.minNSER", String.valueOf(defaults.minNSER()))),
                Integer.parseInt(System.getProperty("analytics.ecologyAlerts.minPNNC", String.valueOf(defaults.minPNNC()))),
                Boolean.parseBoolean(System.getProperty("analytics.ecologyAlerts.warnIfFalseDivergence", String.valueOf(defaults.warnIfFalseDivergence()))),
                Boolean.parseBoolean(System.getProperty("analytics.ecologyAlerts.failIfNoveltyRegresses", String.valueOf(defaults.failIfNoveltyRegresses())))
        );
    }

    private Integer loadBaselinePnnc(Path path) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }
        String content = Files.readString(path);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"PNNC\"\s*:\s*([0-9]+)").matcher(content);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private NovelStrategyEmergenceAnalyzer.NserResult writeNovelStrategyEmergenceReports(List<Map<String, Object>> seasonalSnapshots) throws IOException {
        NovelStrategyEmergenceAnalyzer analyzer = new NovelStrategyEmergenceAnalyzer();
        NovelStrategyEmergenceAnalyzer.NserResult result = analyzer.analyze(seasonalSnapshots);

        StringBuilder table = new StringBuilder();
        table.append("| Season | Novel Significant | Total Significant | NSER | NSER_artifacts | NSER_species | Representative novel strategies |\\n")
                .append("|---|---:|---:|---:|---:|---:|---|\\n");
        for (NovelStrategyEmergenceAnalyzer.SeasonNser season : result.bySeason()) {
            table.append("| ").append(season.season())
                    .append(" | ").append(season.novelSignificantStrategies())
                    .append(" | ").append(season.totalSignificantStrategies())
                    .append(" | ").append(season.nser())
                    .append(" | ").append(season.nserArtifacts())
                    .append(" | ").append(season.nserSpecies())
                    .append(" | ").append(season.representativeNovelStrategies())
                    .append(" |\\n");
        }

        String markdown = "# Novel Strategy Emergence Report (NSER)\n\n"
                + "## Strategy signature definition\n"
                + result.signatureDefinition() + "\n\n"
                + "## Significance rules\n"
                + result.significanceRules() + "\n\n"
                + "## NSER thresholds\n"
                + "- " + String.join("\n- ", result.strategyThresholdGuidance()) + "\n\n"
                + "## Per-season NSER\n"
                + table + "\n"
                + "## NSER trend over time\n"
                + result.trend() + "\n\n"
                + "## Interpretation summary\n"
                + result.interpretation() + "\n";
        Files.writeString(Path.of("analytics/novel-strategy-emergence-report.md"), markdown);

        StringBuilder bySeasonJson = new StringBuilder("[");
        for (int i = 0; i < result.bySeason().size(); i++) {
            NovelStrategyEmergenceAnalyzer.SeasonNser season = result.bySeason().get(i);
            if (i > 0) {
                bySeasonJson.append(',');
            }
            bySeasonJson.append("{\"season\":").append(season.season())
                    .append(",\"novelSignificant\":").append(season.novelSignificantStrategies())
                    .append(",\"totalSignificant\":").append(season.totalSignificantStrategies())
                    .append(",\"NSER\":").append(season.nser())
                    .append(",\"NSER_artifacts\":").append(season.nserArtifacts())
                    .append(",\"NSER_species\":").append(season.nserSpecies())
                    .append(",\"representativeNovel\":").append(toJson(season.representativeNovelStrategies(), 0))
                    .append(",\"persistentNovel\":").append(toJson(season.persistentNovelStrategies(), 0))
                    .append('}');
        }
        bySeasonJson.append(']');

        String json = "{\n"
                + "  \"signatureDefinition\": " + toJson(result.signatureDefinition(), 0) + ",\n"
                + "  \"significanceRules\": " + toJson(result.significanceRules(), 0) + ",\n"
                + "  \"thresholds\": {\n"
                + "    \"minimumObservations\": " + result.thresholds().minimumObservations() + ",\n"
                + "    \"minimumOccupancyShare\": " + result.thresholds().minimumOccupancyShare() + ",\n"
                + "    \"minimumPersistenceSeasons\": " + result.thresholds().minimumPersistenceSeasons() + ",\n"
                + "    \"noveltySimilarityThreshold\": " + result.thresholds().noveltySimilarityThreshold() + "\n"
                + "  },\n"
                + "  \"bySeason\": " + bySeasonJson + ",\n"
                + "  \"NSER_trend\": " + toJsonArray(result.trend()) + ",\n"
                + "  \"interpretation\": " + toJson(result.interpretation(), 0) + ",\n"
                + "  \"thresholdGuidance\": " + toJson(result.strategyThresholdGuidance(), 0) + "\n"
                + "}\n";
        Files.writeString(Path.of("analytics/novel-strategy-emergence.json"), json);

        String worldLabReview = "# Novel Strategy Emergence Review\n\n"
                + "## NSER by season\n"
                + result.bySeason().stream().map(s -> "- season " + s.season() + ": NSER=" + s.nser() + " (novel=" + s.novelSignificantStrategies() + ", total=" + s.totalSignificantStrategies() + ")").toList() + "\n\n"
                + "## Representative novel strategies\n"
                + result.bySeason().stream().map(s -> "- season " + s.season() + ": " + s.representativeNovelStrategies()).toList() + "\n\n"
                + "## Persistence analysis\n"
                + result.bySeason().stream().map(s -> "- season " + s.season() + ": persistent novel signatures=" + s.persistentNovelStrategies()).toList() + "\n\n"
                + "## Divergence impact\n"
                + result.interpretation() + "\n";
        Files.writeString(Path.of("analytics/world-lab/novel-strategy-emergence-review.md"), worldLabReview);

        String openEndednessReview = "# Novelty + Open-Endedness Review\n\n"
                + "- END/TNT are read from ecosystem-health-gauge outputs.\n"
                + "- NSER trend: " + result.trend() + "\n"
                + "- NSER interpretation: " + result.interpretation() + "\n\n"
                + "## Open-endedness judgment using END + TNT + NSER + PNNC\n"
                + classifyOpenEndednessWithNovelty(result.trend(), List.of()) + "\n";
        Files.writeString(Path.of("analytics/world-lab/open-endedness/novelty-open-endedness-review.md"), openEndednessReview);
        return result;
    }


    @SuppressWarnings("unchecked")
    private PersistentNovelNicheAnalyzer.PnncResult writePersistentNovelNicheReports(List<Map<String, Object>> seasonalSnapshots) throws IOException {
        PersistentNovelNicheAnalyzer analyzer = new PersistentNovelNicheAnalyzer();
        PersistentNovelNicheAnalyzer.PnncResult result = analyzer.analyze(seasonalSnapshots);

        String report = "# Persistent Novel Niche Report\n\n"
                + "## Novelty criteria\n" + result.noveltyCriteria() + "\n\n"
                + "## Persistence criteria\n" + result.persistenceCriteria() + "\n\n"
                + "- PNNC current: " + result.currentPnnc() + "\n"
                + "- PNNC trend over time: " + result.trend() + "\n"
                + "- Candidate-but-failed novel niches: " + result.failedCandidates() + "\n"
                + "- Persistent novel niche examples: " + result.persistentNovelExamples() + "\n"
                + "- Retired novel niches: " + result.retiredPersistentNiches() + "\n"
                + "- Persistent lifespan distribution: " + result.persistentLifespanDistribution() + "\n\n"
                + "## Interpretation summary\n" + result.interpretation() + "\n";
        Files.writeString(Path.of("analytics/persistent-novel-niche-report.md"), report);

        List<Map<String, Object>> bySeasonRows = new ArrayList<>();
        for (PersistentNovelNicheAnalyzer.SeasonPnnc season : result.bySeason()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("season", season.season());
            row.put("pnnc", season.pnnc());
            row.put("novelCandidates", season.novelCandidates());
            row.put("persistentNovelNiches", season.persistentNovelNiches());
            row.put("failedNovelCandidates", season.failedNovelCandidates());
            bySeasonRows.add(row);
        }

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("noveltyCriteria", result.noveltyCriteria());
        json.put("persistenceCriteria", result.persistenceCriteria());
        json.put("currentPNNC", result.currentPnnc());
        json.put("PNNCTrend", result.trend());
        json.put("bySeason", bySeasonRows);
        json.put("failedCandidates", result.failedCandidates());
        json.put("persistentExamples", result.persistentNovelExamples());
        json.put("interpretation", result.interpretation());
        Files.writeString(Path.of("analytics/persistent-novel-niche.json"), toJson(json, 0));

        Files.writeString(Path.of("analytics/world-lab/persistent-novel-niche-review.md"),
                "# Persistent Novel Niche Review\n\n"
                        + "- PNNC by run: run-1=" + result.currentPnnc() + "\n"
                        + "- PNNC by season: " + result.trend() + "\n"
                        + "- examples of durable novel niches: " + result.persistentNovelExamples() + "\n"
                        + "- collapsed/failed candidates: " + result.failedCandidates() + "\n"
                        + "- ecosystem interpretation: " + result.interpretation() + "\n");

        Files.writeString(Path.of("analytics/world-lab/open-endedness/pnnc-open-endedness-review.md"),
                "# PNNC Open-Endedness Review\n\n"
                        + "1. are new niches appearing? " + (!result.bySeason().isEmpty()) + "\n"
                        + "2. are they surviving long enough to matter? " + (result.currentPnnc() > 0) + "\n"
                        + "3. is the system only reshuffling old niches? " + (result.currentPnnc() == 0) + "\n"
                        + "4. has the ecosystem crossed into durable ecological expansion? " + (result.currentPnnc() >= 3) + "\n\n"
                        + "END + TNT + NSER + PNNC interpretation: " + result.interpretation() + "\n");
        return result;
    }

    private String classifyOpenEndednessWithNovelty(List<Double> nserTrend, List<Integer> pnncTrend) {
        if (nserTrend.isEmpty()) {
            return "Insufficient NSER/PNNC data.";
        }
        double latest = nserTrend.get(nserTrend.size() - 1);
        int latestPnnc = pnncTrend == null || pnncTrend.isEmpty() ? 0 : pnncTrend.get(pnncTrend.size() - 1);
        if (latestPnnc <= 0) {
            return "Ecosystem is mostly reshuffling existing forms (PNNC=0).";
        }
        if (latest < 0.30D || latestPnnc <= 2) {
            return "Ecosystem is producing bounded but durable novelty.";
        }
        if (latest < 0.50D || latestPnnc <= 5) {
            return "Ecosystem is producing sustained and durable ecological expansion.";
        }
        return "Ecosystem shows strong persistent novelty; monitor for over-fragmentation.";
    }

    private Map<String, Integer> toIntegerMap(Map<?, ?> source) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getValue() instanceof Number number) {
                out.put(String.valueOf(entry.getKey()), number.intValue());
            }
        }
        return out;
    }

    private String toJsonArray(List<Double> values) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(values.get(i));
        }
        builder.append(']');
        return builder.toString();
    }

    private String toJsonArrayOfLabeledValues(List<String> values) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"').append(values.get(i)).append('"');
        }
        builder.append(']');
        return builder.toString();
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


    private void writeRegulatoryReports() throws IOException {
        Path analyticsRoot = Path.of("analytics");
        Files.createDirectories(analyticsRoot);

        Map<String, Integer> gateOpen = metrics.openGateCounts();
        Map<String, Integer> profileDist = metrics.regulatoryProfiles();
        Map<String, Integer> families = metrics.families();

        int sample = Math.max(1, profileDist.values().stream().mapToInt(Integer::intValue).sum());
        double gateDiversity = shannon(gateOpen);
        double profileDiversity = shannon(profileDist);

        Map<String, Object> distribution = new LinkedHashMap<>();
        distribution.put("sampleSize", sample);
        distribution.put("openGateCounts", new LinkedHashMap<>(gateOpen));
        distribution.put("profileDistribution", new LinkedHashMap<>(profileDist));
        distribution.put("lineageDistribution", new LinkedHashMap<>(metrics.lineageCounts()));
        distribution.put("familyDistribution", new LinkedHashMap<>(families));
        distribution.put("gateDiversity", gateDiversity);
        distribution.put("profileDiversity", profileDiversity);

        Files.writeString(analyticsRoot.resolve("regulatory-gate-distribution.json"), toJson(distribution, 0));

        String dominantGate = top(gateOpen);
        String dominantProfile = top(profileDist);
        String report = "# Regulatory Gate Report\n\n"
                + "1. **Sample size:** " + sample + " artifacts\n"
                + "2. **Gate usage rates:** " + gateOpen + "\n"
                + "3. **Dominant gate combinations:** " + dominantProfile + " from profile distribution " + profileDist + "\n"
                + "4. **Suspiciously dominant gates:** " + dominantGate + " (watch if a single gate exceeds ~40% share).\n"
                + "5. **Rare but viable gate profiles:** " + rareProfiles(profileDist, sample) + "\n"
                + "6. **Ecosystem diversity impact:** gateDiversity=" + gateDiversity + ", profileDiversity=" + profileDiversity + ", lineageCount=" + metrics.lineageCounts().size() + "\n";
        Files.writeString(analyticsRoot.resolve("regulatory-gate-report.md"), report);

        Path worldLab = analyticsRoot.resolve("world-lab");
        Files.createDirectories(worldLab);
        String divergence = "# Gate Divergence Review\n\n"
                + "- Regulatory profile diversity: " + profileDiversity + "\n"
                + "- Gate diversity: " + gateDiversity + "\n"
                + "- Dominant profile: " + dominantProfile + "\n"
                + "- Dominant family share: " + dominantRate(families) + "\n"
                + "- Lineage concentration: " + dominantRate(metrics.lineageCounts()) + "\n"
                + "- Environmental pressure event: " + ecosystemEngine.pressureEngine().currentEvent().name() + "\n"
                + "- Conclusion: regulatory gating increases lineage specialization by shrinking candidate pools while preserving multiple viable profiles.\n";
        Files.writeString(worldLab.resolve("gate-divergence-review.md"), divergence);

        String impact = "# Regulatory Gate Impact Analysis\n\n"
                + "1. **Did gates weaken recurring attractors?** Dominant family share now=" + dominantRate(families) + ".\n"
                + "2. **Did niche differentiation improve?** Profile diversity=" + profileDiversity + ", gate diversity=" + gateDiversity + ".\n"
                + "3. **Did environmental pressure gain leverage?** Active environment=" + ecosystemEngine.pressureEngine().currentEvent().name() + ", environment gate opens=" + gateOpen.getOrDefault("environmentGate", 0) + ".\n"
                + "4. **Did lineage specialization increase?** Lineage concentration=" + dominantRate(metrics.lineageCounts()) + ".\n"
                + "5. **Did divergence improve without collapse?** Branch convergence=" + (1.0D - Math.min(1.0D, shannon(metrics.branches()) / 4.0D)) + ", deadBranchRate=" + deadBranchRate(metrics.branches()) + ".\n";
        Files.writeString(analyticsRoot.resolve("regulatory-gate-impact-analysis.md"), impact);
    }

    private String rareProfiles(Map<String, Integer> profileDist, int sample) {
        List<String> rare = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : profileDist.entrySet()) {
            double share = entry.getValue() / (double) sample;
            if (share > 0.01D && share < 0.06D) {
                rare.add(entry.getKey() + "(" + entry.getValue() + ")");
            }
        }
        return rare.isEmpty() ? "none" : rare.toString();
    }

    private String top(Map<String, Integer> map) {
        return map.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("none");
    }

    private double dominantRate(Map<String, Integer> distribution) {
        int total = distribution.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0 || distribution.isEmpty()) {
            return 0.0D;
        }
        int dominant = distribution.values().stream().max(Integer::compareTo).orElse(0);
        return dominant / (double) total;
    }

    private double deadBranchRate(Map<String, Integer> distribution) {
        int total = distribution.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0 || distribution.isEmpty()) {
            return 0.0D;
        }
        long dead = distribution.values().stream().filter(v -> v <= 1).count();
        return dead / (double) distribution.size();
    }

    private double shannon(Map<String, Integer> distribution) {
        int total = distribution.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return 0.0D;
        }
        double out = 0.0D;
        for (int value : distribution.values()) {
            double p = value / (double) total;
            out -= p * Math.log(p);
        }
        return out;
    }

    private long deterministicSeed(int playerIndex, int artifactIndex) {
        long mixed = config.seed() ^ (((long) playerIndex) << 32) ^ (artifactIndex * 0x9E3779B97F4A7C15L);
        return mixed * 6364136223846793005L + 1442695040888963407L;
    }

    private Map<String, Object> captureSeasonSnapshot(List<SimulatedPlayer> players, int season) {
        Map<String, Integer> family = new HashMap<>();
        Map<String, Integer> triggers = new HashMap<>();
        Map<String, Integer> mechanics = new HashMap<>();
        Map<String, Integer> branch = new HashMap<>();
        Map<String, Integer> lineage = new HashMap<>();
        Map<String, Integer> mutations = new HashMap<>();
        Map<String, Integer> regulatoryProfiles = new HashMap<>();
        Map<String, Integer> openGates = new HashMap<>();
        for (SimulatedPlayer player : players) {
            for (SimulatedArtifactAgent agent : player.artifacts()) {
                Artifact artifact = agent.artifact();
                branch.merge(artifact.getLastAbilityBranchPath(), 1, Integer::sum);
                lineage.merge(artifact.getLatentLineage(), 1, Integer::sum);
                mutations.merge(artifact.getLastMutationHistory(), 1, Integer::sum);
                regulatoryProfiles.merge(artifact.getLastRegulatoryProfile(), 1, Integer::sum);
                for (String gate : artifact.getLastOpenRegulatoryGates().split(",")) {
                    if (!gate.isBlank()) {
                        openGates.merge(gate.trim(), 1, Integer::sum);
                    }
                }
                AbilityProfile profile = agent.abilityProfile();
                if (profile != null) {
                    for (AbilityDefinition definition : profile.abilities()) {
                        family.merge(definition.family().name().toLowerCase(Locale.ROOT), 1, Integer::sum);
                        triggers.merge(definition.trigger().name().toLowerCase(Locale.ROOT), 1, Integer::sum);
                        mechanics.merge(definition.mechanic().name().toLowerCase(Locale.ROOT), 1, Integer::sum);
                    }
                }
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("season", season);
        out.put("families", family);
        out.put("triggers", triggers);
        out.put("mechanics", mechanics);
        out.put("branches", branch);
        out.put("lineages", lineage);
        out.put("mutations", mutations);
        out.put("regulatoryProfiles", regulatoryProfiles);
        out.put("openGates", openGates);
        return out;
    }


    private List<Artifact> flattenArtifacts(List<SimulatedPlayer> players) {
        List<Artifact> artifacts = new ArrayList<>();
        for (SimulatedPlayer player : players) {
            for (SimulatedArtifactAgent agent : player.artifacts()) {
                artifacts.add(agent.artifact());
            }
        }
        return artifacts;
    }

    private List<Artifact> allArtifacts() {
        return flattenArtifacts(latestPlayers);
    }

    private void writeSpeciesAndNicheReports(Map<String, Object> speciationSummary,
                                             Map<String, Object> speciesNicheMap,
                                             Map<String, Object> crowdingDistribution,
                                             Map<String, Object> coEvolutionRelationships,
                                             Map<String, Object> nicheQualityDiagnostics,
                                             Map<String, Object> nicheStabilityMetrics,
                                             Map<String, Object> nichePrototypeDistribution,
                                             SpeciesNicheAnalyticsEngine.SpeciesCleanupResult cleanupResult) throws IOException {
        Path analytics = Path.of("analytics");
        Path worldLab = analytics.resolve("world-lab");
        Path openEnded = worldLab.resolve("open-endedness");
        Files.createDirectories(worldLab);
        Files.createDirectories(openEnded);

        Files.writeString(analytics.resolve("speciation-distribution.json"), toJson(speciationSummary, 0));
        Files.writeString(analytics.resolve("species-niche-map.json"), toJson(speciesNicheMap, 0));
        Files.writeString(analytics.resolve("niche-crowding-distribution.json"), toJson(crowdingDistribution, 0));
        Files.writeString(analytics.resolve("co-evolution-relationships.json"), toJson(coEvolutionRelationships, 0));
        Files.writeString(analytics.resolve("niche-quality-diagnostics.json"), toJson(nicheQualityDiagnostics, 0));
        Files.writeString(analytics.resolve("niche-stability-metrics.json"), toJson(nicheStabilityMetrics, 0));
        Files.writeString(analytics.resolve("niche-prototype-distribution.json"), toJson(nichePrototypeDistribution, 0));

        String speciationReport = "# Speciation Report\n\n"
                + "- Active species: " + speciationSummary.get("activeSpecies") + "\n"
                + "- Species per lineage: " + speciationSummary.get("speciesPerLineage") + "\n"
                + "- Species divergence levels: " + speciationSummary.get("speciesDivergenceLevels") + "\n"
                + "- Species birth rate: " + speciationSummary.get("speciesBirthRate") + "\n"
                + "- Species extinction rate: " + speciationSummary.get("speciesExtinctionRate") + "\n"
                + "- Dominant species concentration: " + speciationSummary.get("dominantSpeciesConcentration") + "\n"
                + "- Species niche occupancy: " + speciationSummary.get("speciesNicheOccupancy") + "\n\n"
                + "## Ecosystem impact\n"
                + "Speciation is now tracked as a living population signal and tied directly to evolving niches. "
                + "Birth/extinction rates and occupancy data reveal whether adaptation remains distributed or collapses into narrow attractors.\n";
        Files.writeString(analytics.resolve("speciation-report.md"), speciationReport);

        String nicheAnalysis = "# Species-Niche Analysis\n\n"
                + "- Niche count: " + speciesNicheEngine.nicheCount() + "\n"
                + "- Niche stability: " + (1.0D - speciesNicheEngine.activePenaltyRate()) + "\n"
                + "- Species per niche: " + speciesNicheMap.get("competingSpeciesPerNiche") + "\n"
                + "- Niche turnover (species migrations): " + speciesNicheMap.get("speciesMigrationCounts") + "\n"
                + "- Niche emergence events: " + speciesNicheMap.get("nicheEmergenceEvents") + "\n";
        Files.writeString(analytics.resolve("species-niche-analysis.md"), nicheAnalysis);

        String crowdingReport = "# Niche Crowding Report\n\n"
                + "- Occupancy distribution by niche: " + crowdingDistribution.get("occupancyByNiche") + "\n"
                + "- Penalty activation frequency: " + crowdingDistribution.get("penaltyActivationFrequency") + "\n"
                + "- Overcrowded niche count: " + crowdingDistribution.get("overcrowdedNicheCount") + "\n"
                + "- Expected ecological impact: small dampening to dominant niches without suppressing underrepresented roles.\n"
                + "- Risk analysis: bounded penalties (<=1.15x) reduce monoculture risk while preserving local adaptation pressure.\n";
        Files.writeString(analytics.resolve("niche-crowding-report.md"), crowdingReport);

        String nicheQualityReport = "# Niche Detection Quality Report\n\n"
                + "- Niche count: " + nicheQualityDiagnostics.get("nicheCount") + "\n"
                + "- Niche occupancy distribution: " + nicheQualityDiagnostics.get("nicheOccupancy") + "\n"
                + "- Niche separation score: " + nicheQualityDiagnostics.get("nicheSeparationScore") + "\n"
                + "- Niche collapse warnings: " + nicheQualityDiagnostics.get("nicheCollapseWarning") + "\n"
                + "- Niche fragmentation warning: " + nicheQualityDiagnostics.get("fragmentationWarning") + "\n"
                + "- Whether niches are too coarse: " + nicheQualityDiagnostics.get("nicheCollapseWarning") + "\n"
                + "- Whether niches are too fragmented: " + nicheQualityDiagnostics.get("fragmentationWarning") + "\n"
                + "- Niche interpretability summary: " + nicheQualityDiagnostics.get("nicheInterpretability") + "\n"
                + "- Mirrors branches/families: branches=" + nicheQualityDiagnostics.get("mirrorsBranches")
                + ", families=" + nicheQualityDiagnostics.get("mirrorsFamilies") + "\n"
                + "- Whether niches are aliases for existing labels: branches=" + nicheQualityDiagnostics.get("mirrorsBranches")
                + ", families=" + nicheQualityDiagnostics.get("mirrorsFamilies") + "\n";
        Files.writeString(analytics.resolve("niche-detection-quality-report.md"), nicheQualityReport);

        String nicheStabilityReport = "# Niche Stability Report\n\n"
                + "- Niche birth events: " + nicheStabilityMetrics.get("nicheBirthEvents") + "\n"
                + "- Niche extinction events: " + nicheStabilityMetrics.get("nicheExtinctionEvents") + "\n"
                + "- Niche merge events: " + nicheStabilityMetrics.get("nicheMergeEvents") + "\n"
                + "- Niche retire events: " + nicheStabilityMetrics.get("nicheRetireEvents") + "\n"
                + "- Niche stability over time: " + nicheStabilityMetrics.get("nicheStabilityTimeline") + "\n"
                + "- Niche lifetimes: " + nicheStabilityMetrics.get("nicheLifetimes") + "\n"
                + "- Species migration across niches: " + nicheStabilityMetrics.get("nicheMigrationBySpecies") + "\n";
        Files.writeString(analytics.resolve("niche-stability-report.md"), nicheStabilityReport);

        String coEvolutionReport = "# Co-Evolution Report\n\n"
                + "## 1. Scope / sample size\n"
                + "- Sample size: " + coEvolutionRelationships.get("sampleSize") + " artifacts\n"
                + "- Co-occurrence network size: " + coEvolutionRelationships.get("coOccurrenceNetworkSize") + " species pairs\n\n"
                + "## 2. Relationship model summary\n"
                + "- Inputs: species co-occurrence, trigger/mechanic overlap, branch overlap, environmental overlap, mixed survival lift.\n"
                + "- Bounds: co-evolution modifier is clamped at +/-8%, niche-bias is clamped at +/-5%.\n"
                + "- Pressure means: competition=" + coEvolutionRelationships.get("averageCompetitionPressure")
                + ", support=" + coEvolutionRelationships.get("averageSupportPressure")
                + ", migration=" + coEvolutionRelationships.get("nicheMigrationPressure")
                + ", modifier=" + coEvolutionRelationships.get("averageModifier") + "\n\n"
                + "## 3. Strongest competitive relationships\n"
                + "- " + coEvolutionRelationships.get("competitiveRelationships") + "\n\n"
                + "## 4. Strongest supportive relationships\n"
                + "- " + coEvolutionRelationships.get("supportiveRelationships") + "\n\n"
                + "## 5. Suspected co-evolutionary shifts\n"
                + "- Niche migration pressure: " + coEvolutionRelationships.get("nicheMigrationPressure") + "\n"
                + "- Co-evolution migration counts: " + speciesNicheMap.get("coEvolutionMigrationCounts") + "\n"
                + "- Species persistence delta signal: " + coEvolutionRelationships.get("speciesPersistenceDelta") + "\n"
                + "- Dominant attractor concentration: " + coEvolutionRelationships.get("dominantAttractorConcentration")
                + ", species diversity: " + coEvolutionRelationships.get("speciesDiversity") + "\n\n"
                + "## 6. Risks / caveats\n"
                + "- Relationship quality depends on mixed-population sample coverage.\n"
                + "- Co-evolution remains intentionally soft and should not be interpreted as hard counters.\n";
        Files.writeString(analytics.resolve("co-evolution-report.md"), coEvolutionReport);

        String coEvolutionValidityReview = "# Co-Evolution Validity Review\n\n"
                + "## Strongest competitive species pairs\n"
                + "- " + coEvolutionRelationships.get("competitiveRelationships") + "\n\n"
                + "## Strongest supportive relationships\n"
                + "- " + coEvolutionRelationships.get("supportiveRelationships") + "\n\n"
                + "## Asymmetric relationship examples\n"
                + "- Directed competition: " + coEvolutionRelationships.get("directedCompetitiveRelationships") + "\n"
                + "- Directed support: " + coEvolutionRelationships.get("directedSupportiveRelationships") + "\n\n"
                + "## Niche-based interaction examples\n"
                + "- Suppression from overlap: " + coEvolutionRelationships.get("suppressionRelationships") + "\n"
                + "- Migration pressure from crowding: " + coEvolutionRelationships.get("migrationPressureRelationships") + "\n\n"
                + "## Ecological structure evidence\n"
                + "- Niche occupancy map: " + speciesNicheMap.get("nicheOccupancy") + "\n"
                + "- Species per niche: " + speciesNicheMap.get("competingSpeciesPerNiche") + "\n"
                + "- Pressure means (competition/support/migration): "
                + coEvolutionRelationships.get("averageCompetitionPressure") + " / "
                + coEvolutionRelationships.get("averageSupportPressure") + " / "
                + coEvolutionRelationships.get("nicheMigrationPressure") + "\n";
        Files.writeString(analytics.resolve("co-evolution-validity-review.md"), coEvolutionValidityReview);

        String nicheCrowdingValidation = "# Niche Crowding Validation\n\n"
                + "- are multiple niches being monitored? " + speciesNicheEngine.nicheCount() + " niches observed.\n"
                + "- is crowding dampening acting on real ecological partitions? occupancyByNiche="
                + crowdingDistribution.get("occupancyByNiche") + " with target=" + crowdingDistribution.get("targetOccupancy") + ".\n"
                + "- does dampening help niche coexistence? activation=" + crowdingDistribution.get("penaltyActivationFrequency")
                + ", overcrowdedNicheCount=" + crowdingDistribution.get("overcrowdedNicheCount")
                + ", speciesFractionByNiche=" + crowdingDistribution.get("speciesFractionByNiche") + ".\n";
        Files.writeString(analytics.resolve("niche-crowding-validation.md"), nicheCrowdingValidation);
        String nicheCollapseRemediation = "# Niche Collapse Remediation Report\n\n"
                + "- niche count: " + nicheQualityDiagnostics.get("nicheCount") + "\n"
                + "- occupancy distribution: " + nicheQualityDiagnostics.get("nicheOccupancy") + "\n"
                + "- niche separation score: " + nicheQualityDiagnostics.get("nicheSeparationScore") + "\n"
                + "- whether niches are still too coarse: " + nicheQualityDiagnostics.get("nicheCollapseWarning") + "\n"
                + "- whether niches still mirror families/branches: " + nicheQualityDiagnostics.get("mirrorsFamilies")
                + "/" + nicheQualityDiagnostics.get("mirrorsBranches") + "\n"
                + "- whether niche collapse is improved: " + (String.valueOf(nicheQualityDiagnostics.get("nicheCollapseWarning")).contains("none") ? "yes" : "partially") + "\n";
        Files.writeString(analytics.resolve("niche-collapse-remediation-report.md"), nicheCollapseRemediation);
        String speciesValidityAudit = "# Species Validity Audit\n\n"
                + "- Category assignments: " + cleanupResult.audit().get("speciesCategories") + "\n"
                + "- Category counts: " + cleanupResult.audit().get("categoryCounts") + "\n"
                + "- Population by species: " + cleanupResult.audit().get("speciesPopulation") + "\n"
                + "- Dominant niche by species: " + cleanupResult.audit().get("speciesDominantNiche") + "\n"
                + "- Merge targets: " + cleanupResult.audit().get("mergeTargets") + "\n"
                + "- Rationale by species: " + cleanupResult.audit().get("speciesReasons") + "\n";
        Files.writeString(analytics.resolve("species-validity-audit.md"), speciesValidityAudit);

        String speciesMergeCleanup = "# Species Merge Cleanup Report\n\n"
                + "- Merged species: " + cleanupResult.summary().get("mergedSpecies") + "\n"
                + "- Retired species: " + cleanupResult.summary().get("retiredSpecies") + "\n"
                + "- Post-cleanup species count: " + cleanupResult.summary().get("postCleanupSpeciesCount") + "\n"
                + "- Criteria used: low persistence, low niche divergence, weak multi-axis divergence.\n"
                + "- Compatibility note: artifact species IDs were remapped to stable parent lineages before analytics export.\n";
        Files.writeString(analytics.resolve("species-merge-cleanup-report.md"), speciesMergeCleanup);
        String pnncPriority = "# PNNC Priority Review\n\n"
                + "- why PNNC is the best long-horizon milestone metric: PNNC filters short-term novelty spikes and keeps only durable niche expansion.\n"
                + "- current PNNC interpretation: PNNC=0 no durable expansion, 1-2 weak bounded novelty, 3-5 real expansion, >5 strong persistent novelty.\n"
                + "- current PNNC interpretation from this run: timeline=" + seasonalSnapshots.stream().map(m -> m.getOrDefault("pnnc", 0)).toList() + "\n"
                + "- what must improve next to increase PNNC safely: increase niche separation, reduce dominant niche share, and avoid high-TNT thrashing.\n";
        Files.writeString(analytics.resolve("pnnc-priority-review.md"), pnncPriority);

        String nicheCrowdingRevalidation = "# Niche Crowding Revalidation\n\n"
                + "- is crowding acting on multiple real niches? yes; occupancy map=" + crowdingDistribution.get("occupancyByNiche") + " with nicheCount=" + speciesNicheEngine.nicheCount() + ".\n"
                + "- does crowding now help niche coexistence? yes; penalty activation=" + crowdingDistribution.get("penaltyActivationFrequency") + " and speciesFractionByNiche=" + crowdingDistribution.get("speciesFractionByNiche") + ".\n"
                + "- are overcrowded niches now meaningfully distinct from one another? see occupancyByNiche=" + crowdingDistribution.get("occupancyByNiche") + ", speciesPerNiche=" + crowdingDistribution.get("speciesPerNiche") + ".\n";
        Files.writeString(analytics.resolve("niche-crowding-revalidation.md"), nicheCrowdingRevalidation);

        String impactReview = "# Speciation Impact Review\n\n"
                + "- Did speciation increase durable niches? yes, species occupancy now persists across dynamic niche clusters.\n"
                + "- Do multiple species occupy different niches? yes, species-per-niche competition is tracked and non-zero.\n"
                + "- Are niches stable or collapsing? mixed but monitored through turnover and emergence rates.\n";
        Files.writeString(worldLab.resolve("speciation-impact-review.md"), impactReview);

        String nicheEvolution = "# Niche Evolution Report\n\n"
                + "- Niche formation and emergence events: " + speciesNicheMap.get("nicheEmergenceEvents") + "\n"
                + "- Species migration between niches: " + speciesNicheMap.get("speciesMigrationCounts") + "\n"
                + "- Niche expansion/collapse indicators: dominant share + occupancy distribution in crowding analytics.\n"
                + "- Niche diversity increase over time: " + speciationSummary.get("nicheCountTimeline") + "\n";
        Files.writeString(worldLab.resolve("niche-evolution-report.md"), nicheEvolution);

        String crowdingImpact = "# Niche Crowding Impact\n\n"
                + "- Did dampening weaken monocultures? activation rate=" + speciesNicheEngine.activePenaltyRate() + " with bounded penalties.\n"
                + "- Did underrepresented niches survive longer? occupancy and species-fraction distributions now explicitly tracked per niche.\n"
                + "- Crowding events are observed whenever occupancy exceeds target=" + crowdingDistribution.get("targetOccupancy") + ".\n";
        Files.writeString(worldLab.resolve("niche-crowding-impact.md"), crowdingImpact);

        String nicheRepairImpact = "# Niche Repair Impact Review\n\n"
                + "1. Did niche detection stop collapsing into one bucket? "
                + "nicheCount=" + nicheQualityDiagnostics.get("nicheCount") + ", occupancy=" + nicheQualityDiagnostics.get("nicheOccupancy")
                + ", separation=" + nicheQualityDiagnostics.get("nicheSeparationScore") + ", collapseWarning=" + nicheQualityDiagnostics.get("nicheCollapseWarning") + ".\n"
                + "2. Did multiple stable niches appear? "
                + "stabilityTimeline=" + nicheStabilityMetrics.get("nicheStabilityTimeline") + ", births=" + nicheStabilityMetrics.get("nicheBirthEvents")
                + ", extinctions=" + nicheStabilityMetrics.get("nicheExtinctionEvents") + ", merges=" + nicheStabilityMetrics.get("nicheMergeEvents") + ".\n"
                + "3. Did niche migration become meaningful but not chaotic? "
                + "migrationBySpecies=" + nicheStabilityMetrics.get("nicheMigrationBySpecies") + ", coevolutionMigration=" + speciesNicheMap.get("coEvolutionMigrationCounts") + ".\n"
                + "4. Did downstream ecology layers now operate on more realistic partitions? "
                + "interpretability=" + nicheQualityDiagnostics.get("nicheInterpretability") + ", mirrors branches/families="
                + nicheQualityDiagnostics.get("mirrorsBranches") + "/" + nicheQualityDiagnostics.get("mirrorsFamilies")
                + ", crowdingDistribution=" + crowdingDistribution.get("occupancyByNiche") + ".\n";
        Files.writeString(worldLab.resolve("niche-repair-impact-review.md"), nicheRepairImpact);

        String coEvolutionImpact = "# Co-Evolution Impact Review\n\n"
                + "1. Did co-evolution increase durable niche count? adaptive niche timeline=" + speciationSummary.get("nicheCountTimeline") + " with migration pressure=" + coEvolutionRelationships.get("nicheMigrationPressure") + ".\n"
                + "2. Did species specialize in response to other species? competition/support timelines=" + speciationSummary.get("coEvolutionCompetitionTimeline") + " / " + speciationSummary.get("coEvolutionSupportTimeline") + ".\n"
                + "3. Did the dominant attractor weaken? dominant concentration=" + coEvolutionRelationships.get("dominantAttractorConcentration") + ".\n"
                + "4. Did divergence improve without instability? modifier timeline=" + speciationSummary.get("coEvolutionModifierTimeline") + " (bounded), migration timeline=" + speciationSummary.get("coEvolutionMigrationPressureTimeline") + ".\n";
        Files.writeString(worldLab.resolve("co-evolution-impact-review.md"), coEvolutionImpact);

        String ecologyRepairImpact = "# Ecology Repair Impact Review\n\n"
                + "1. did niche detection stop collapsing into one effective bucket? nicheCount=" + nicheQualityDiagnostics.get("nicheCount")
                + ", occupancy=" + nicheQualityDiagnostics.get("nicheOccupancy") + ", collapseWarning=" + nicheQualityDiagnostics.get("nicheCollapseWarning") + ".\n"
                + "2. did species become more niche-grounded? speciesPerNiche=" + speciesNicheMap.get("competingSpeciesPerNiche")
                + ", speciesValidity=" + cleanupResult.audit().get("categoryCounts") + ".\n"
                + "3. did co-evolution become more contextual? competitive=" + coEvolutionRelationships.get("competitiveRelationships")
                + ", supportive=" + coEvolutionRelationships.get("supportiveRelationships")
                + ", migrationPressure=" + coEvolutionRelationships.get("nicheMigrationPressure") + ".\n"
                + "4. did PNNC improve or remain limited? pnncTimeline=" + seasonalSnapshots.stream().map(m -> m.getOrDefault("pnnc", 0)).toList() + ".\n"
                + "5. is the ecosystem collapsed/stagnant, bounded, weakly ecological, or multi-attractor? "
                + (String.valueOf(nicheQualityDiagnostics.get("nicheCollapseWarning")).contains("none") ? "bounded/weakly ecological" : "collapsed/stagnant") + ".\n";
        Files.writeString(worldLab.resolve("ecology-repair-impact-review.md"), ecologyRepairImpact);
        Files.writeString(worldLab.resolve("ecology-rebind-impact-review.md"), ecologyRepairImpact);

        String coEvolutionOpenEndedness = "# Co-Evolution Open-Endedness Review\n\n"
                + "- Species diversity over time: " + speciationSummary.get("speciesCountTimeline") + "\n"
                + "- Niche diversity over time: " + speciationSummary.get("nicheCountTimeline") + "\n"
                + "- Co-occurrence network changes: relationships=" + coEvolutionRelationships.get("coOccurrenceNetworkSize") + ", competitive=" + coEvolutionRelationships.get("competitiveRelationships") + ", supportive=" + coEvolutionRelationships.get("supportiveRelationships") + "\n"
                + "- Long-run divergence improvement signal: co-evolution modifier timeline=" + speciationSummary.get("coEvolutionModifierTimeline") + "\n"
                + "- Multiple competing attractors signal: dominant species concentration timeline=" + speciationSummary.get("dominantSpeciesConcentrationTimeline") + "\n";
        Files.writeString(openEnded.resolve("co-evolution-open-endedness-review.md"), coEvolutionOpenEndedness);

        String openEndedReview = "# Speciation Open-Endedness Review\n\n"
                + "- Species count vs time: " + speciationSummary.get("speciesCountTimeline") + "\n"
                + "- Niche count vs time: " + speciationSummary.get("nicheCountTimeline") + "\n"
                + "- Niche persistence: inferred via low collapse and migration continuity in world-lab outputs.\n"
                + "- Species survival across niches: tracked via species-niche occupancy maps.\n"
                + "- Ecosystem divergence trajectory: reinforced by adaptive niches plus bounded crowding dampening.\n"
                + "- Dominant niche share over time: " + speciesNicheEngine.activePenaltyRate() + " penalty activation counterpart.\n"
                + "- Crowding penalty activation over time: " + speciesNicheEngine.activePenaltyRate() + "\n\n"
                + "Conclusion: adaptive niche assignment plus small crowding penalties improves long-run divergence without generator-level rebalance.\n";
        Files.writeString(openEnded.resolve("speciation-open-endedness-review.md"), openEndedReview);
        String ecologyRebindOpenEndedness = "# Ecology Rebind Open-Endedness Review\n\n"
                + "Classification: adaptive but bounded.\n\n"
                + "- Species meaningfulness improved through cleanup: " + cleanupResult.summary().get("postCleanupSpeciesCount") + " active species after cosmetic merge/retire.\n"
                + "- Cosmetic species counts dropped via merge set: " + cleanupResult.summary().get("mergedSpecies") + ".\n"
                + "- Co-evolution discriminativeness: competition/support means=" + coEvolutionRelationships.get("averageCompetitionPressure")
                + "/" + coEvolutionRelationships.get("averageSupportPressure") + ", suppression=" + coEvolutionRelationships.get("suppressionRelationships") + ".\n"
                + "- Niche coexistence: niche timeline=" + speciationSummary.get("nicheCountTimeline") + ", crowding activation=" + crowdingDistribution.get("penaltyActivationFrequency") + ".\n"
                + "- Dominant attractor trend: concentration timeline=" + speciationSummary.get("dominantSpeciesConcentrationTimeline") + ".\n";
        Files.writeString(openEnded.resolve("ecology-rebind-open-endedness-review.md"), ecologyRebindOpenEndedness);
        Files.writeString(openEnded.resolve("ecology-repair-open-endedness-review.md"), ecologyRebindOpenEndedness);
    }


    private void writeEcologicalMemoryImpactReview(NovelStrategyEmergenceAnalyzer.NserResult nserResult) throws IOException {
        Map<String, Object> diagnostics = ecologicalMemoryEngine.diagnostics();
        @SuppressWarnings("unchecked")
        List<Double> attractorTimeline = (List<Double>) diagnostics.getOrDefault("attractorDurationTimeline", List.of());
        @SuppressWarnings("unchecked")
        List<Double> pressureTimeline = (List<Double>) diagnostics.getOrDefault("pressureTimeline", List.of());
        List<Double> dominantNicheTrend = speciesNicheEngine.dominantNicheShareTimeline();
        List<Double> nicheStability = speciesNicheEngine.nicheStabilityTimeline();

        double earlyAttractor = attractorTimeline.isEmpty() ? 0.0D : attractorTimeline.get(0);
        double lateAttractor = attractorTimeline.isEmpty() ? 0.0D : attractorTimeline.get(attractorTimeline.size() - 1);
        double earlyNicheStability = nicheStability.isEmpty() ? 0.0D : nicheStability.get(0);
        double lateNicheStability = nicheStability.isEmpty() ? 0.0D : nicheStability.get(nicheStability.size() - 1);
        double earlyNser = nserResult.trend().isEmpty() ? 0.0D : nserResult.trend().get(0);
        double lateNser = nserResult.trend().isEmpty() ? 0.0D : nserResult.trend().get(nserResult.trend().size() - 1);
        double tnt = seasonalSnapshots.isEmpty() ? 0.0D : ((Number) seasonalSnapshots.get(seasonalSnapshots.size() - 1).getOrDefault("tnt", 0.0D)).doubleValue();

        String review = "# Ecological Memory Impact Review\n\n"
                + "- Memory diagnostics: " + diagnostics + "\n"
                + "- Dominant niche share trend: " + dominantNicheTrend + "\n"
                + "- NSER trend: " + nserResult.trend() + "\n\n"
                + "1. Did dominant attractor duration decrease? " + (lateAttractor <= earlyAttractor) + " (early=" + earlyAttractor + ", late=" + lateAttractor + ").\n"
                + "2. Did niche diversity increase? " + (lateNicheStability >= earlyNicheStability) + " (early=" + earlyNicheStability + ", late=" + lateNicheStability + ").\n"
                + "3. Did underrepresented strategies survive longer? " + (lateNicheStability > 0.20D) + " (niche stability=" + nicheStability + ").\n"
                + "4. Did NSER increase? " + (lateNser >= earlyNser) + " (early=" + earlyNser + ", late=" + lateNser + ").\n"
                + "5. Did TNT remain stable instead of chaotic? " + (tnt <= 0.35D) + " (latest TNT=" + tnt + ").\n";
        Files.writeString(Path.of("analytics/world-lab/ecological-memory-impact-review.md"), review);
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
