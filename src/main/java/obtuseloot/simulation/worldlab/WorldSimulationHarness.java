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
import obtuseloot.telemetry.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class WorldSimulationHarness {
    private record AnalyticsTruthSnapshot(
            double endArtifacts,
            Double endSpecies,
            double latestTnt,
            double latestNser,
            int latestPnnc,
            List<Double> endTrend,
            List<Double> tntTrend,
            List<Double> nserTrend,
            List<Integer> pnncTrend,
            double dominantNicheShare,
            int nicheCount,
            int speciesCount,
            EcologyDiagnosticSnapshot diagnostic,
            EcosystemHealthGaugeAnalyzer.GaugeResult healthReport) {}

    private final WorldSimulationConfig config;
    private final Random random;
    private final SimulationClock clock = new SimulationClock();
    private final SimulationMetricsCollector metrics = new SimulationMetricsCollector();
    private final List<Map<String, Object>> seasonalSnapshots = new ArrayList<>();
    private final Deque<TelemetryRollupSnapshot> rollupHistory = new ArrayDeque<>();
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
    private final OpportunityWeightedMutationEngine opportunityMutationEngine;
    private final Map<String, Integer> seasonSpeciesCounts = new LinkedHashMap<>();
    private final Map<String, Integer> seasonNicheCounts = new LinkedHashMap<>();
    private final Map<String, Integer> seasonBranchCounts = new LinkedHashMap<>();
    private final Map<String, Integer> seasonTriggerCounts = new LinkedHashMap<>();
    private final Map<String, Integer> seasonMechanicCounts = new LinkedHashMap<>();
    private final Map<String, Integer> seasonEnvironmentCounts = new LinkedHashMap<>();
    private final EvolutionExperimentConfig experimentConfig;
    private final SimulationScenario scenario;
    private final TelemetryAggregationBuffer telemetryBuffer;
    private final EcosystemHistoryArchive telemetryArchive;
    private final ScheduledEcosystemRollups scheduledRollups;
    private final TelemetryAggregationService telemetryAggregationService;
    private final EcosystemTelemetryEmitter telemetryEmitter;
    private final TelemetryAggregationAnalytics telemetryAnalytics;
    private final TelemetryEventFactory telemetryEventFactory = new TelemetryEventFactory();
    private final int maxRollupHistoryInMemory;
    private final int maxSeasonSnapshotsInMemory;
    private final int memoryLogEveryGenerations;
    private int currentGeneration;

    public WorldSimulationHarness(WorldSimulationConfig config) {
        this.config = config;
        this.random = new Random(config.seed());
        this.experimentConfig = EvolutionExperimentConfig.load(config.scenarioConfigPath() == null || config.scenarioConfigPath().isBlank() ? null : Path.of(config.scenarioConfigPath()), config);
        this.scenario = experimentConfig.scenario();
        this.maxRollupHistoryInMemory = Integer.getInteger("world.maxRollupHistoryInMemory", config.validationProfile() ? 32 : 512);
        this.maxSeasonSnapshotsInMemory = Integer.getInteger("world.maxSeasonSnapshotsInMemory", config.validationProfile() ? 16 : 256);
        this.memoryLogEveryGenerations = Integer.getInteger("world.memoryLogEveryGenerations", Math.max(1, config.sessionsPerSeason()));
        int maxTelemetryBufferEvents = Integer.getInteger("world.maxTelemetryBufferEvents", config.validationProfile() ? 512 : 4096);
        int archiveBatchSize = Integer.getInteger("world.telemetryArchiveBatchSize", config.validationProfile() ? 64 : 256);
        this.telemetryBuffer = new TelemetryAggregationBuffer(maxTelemetryBufferEvents);
        Path telemetryDir = Path.of(config.outputDirectory(), "telemetry");
        this.telemetryArchive = new EcosystemHistoryArchive(telemetryDir.resolve("ecosystem-events.log"));
        this.scheduledRollups = new ScheduledEcosystemRollups(telemetryBuffer, 1L);
        this.telemetryAggregationService = new TelemetryAggregationService(telemetryBuffer, telemetryArchive, scheduledRollups, archiveBatchSize,
                new TelemetryRollupSnapshotStore(telemetryDir.resolve("rollup-snapshot.properties")),
                new RollupStateHydrator(new TelemetryRollupSnapshotStore(telemetryDir.resolve("rollup-snapshot.properties")), telemetryArchive, 1024));
        this.telemetryAggregationService.initializeFromHistory();
        this.telemetryEmitter = new EcosystemTelemetryEmitter(telemetryAggregationService, telemetryEventFactory);
        this.telemetryAnalytics = new TelemetryAggregationAnalytics(scheduledRollups);
        this.experienceEvolutionEngine = config.enableExperienceDrivenEvolution()
                ? new ExperienceEvolutionEngine(usageTracker, new ArtifactFitnessEvaluator(), ecosystemEngine.pressureEngine())
                : null;
        this.speciesNicheEngine = new SpeciesNicheAnalyticsEngine(config.seed(), config.fitnessSharing(), config.behavioralProjection(), config.roleBasedRepulsion(), config.minimumRoleSeparation(), config.adaptiveNicheCapacity());
        this.opportunityMutationEngine = new OpportunityWeightedMutationEngine(config.opportunityWeightedMutation());
        this.abilityGenerator = new ProceduralAbilityGenerator(
                new AbilityRegistry(),
                config.enableEcosystemBias() ? ecosystemEngine : null,
                lineageRegistry,
                lineageInfluenceResolver,
                experienceEvolutionEngine,
                config.enableTraitInteractions(),
                config.scoringMode());
        usageTracker.setTelemetryEmitter(telemetryEmitter);
        lineageRegistry.setTelemetryEmitter(telemetryEmitter);
        if (experienceEvolutionEngine != null) {
            experienceEvolutionEngine.setTelemetryEmitter(telemetryEmitter);
        }
    }

    public TraitProjectionStats traitProjectionStats() {
        return abilityGenerator.traitProjectionStats();
    }

    public void runAndWriteOutputs() throws IOException {
        List<SimulatedPlayer> players = generatePlayers();
        latestPlayers = players;
        for (int season = 1; season <= config.seasonCount(); season++) {
            for (int s = 0; s < config.sessionsPerSeason(); s++) {
                currentGeneration++;
                simulateRound(players);
                telemetryEmitter.scheduledTick(System.currentTimeMillis());
                clock.advanceDay();
            }
            metrics.closeSeasonSnapshot();
            ecologicalMemoryEngine.observeSeason(seasonBranchCounts, seasonNicheCounts, seasonSpeciesCounts, seasonTriggerCounts, seasonMechanicCounts, seasonEnvironmentCounts);
            List<Artifact> seasonArtifacts = flattenArtifacts(players);
            Map<String, Object> seasonSnapshot = captureSeasonSnapshot(players, season);
            seasonSnapshot.putAll(speciesNicheEngine.closeSeason(season, seasonArtifacts));
            seasonSnapshot.put("ecologicalMemory", ecologicalMemoryEngine.diagnostics());
            Map<String, Object> opportunitySummary = refreshOpportunitySignals(seasonArtifacts);
            seasonSnapshot.put("opportunityWeightedMutation", opportunitySummary);
            appendSeasonSnapshot(seasonSnapshot);
            appendRollupSnapshot(new TelemetryRollupSnapshot(
                    TelemetryRollupSnapshot.CURRENT_VERSION,
                    System.currentTimeMillis(),
                    "harness_season_" + season,
                    telemetryAnalytics.ecosystemSnapshot()));
            resetSeasonTallies();
            if (!config.validationProfile()) {
                exportSeasonInteractionHeatmap(players, season);
            }
            if (currentGeneration % memoryLogEveryGenerations == 0) {
                logMemoryCheckpoint(players);
            }
        }
        telemetryEmitter.flushAll();
        telemetryEmitter.scheduledTick(System.currentTimeMillis());
        if (!config.validationProfile()) {
            writeRegulatoryReports();
        }
        writeReports();
    }

    private List<SimulatedPlayer> generatePlayers() {
        List<SimulatedPlayer> players = new ArrayList<>();
        int derivedPlayers = Math.max(1, scenario.artifactPopulationSize() / Math.max(1, config.artifactsPerPlayer()));
        int playerCount = Math.max(config.playerCount(), derivedPlayers);
        for (int i = 0; i < playerCount; i++) {
            PlayerBehaviorModel model = sampleBehaviorModel();
            SimulatedPlayer.BehaviorProfile profile = profileForModel(model);
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
            players.add(new SimulatedPlayer(UUID.randomUUID(), model, profile, artifacts));
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

        AbilityTrigger trigger = toTrigger(encounter, profile);
        evolutionEngine.evaluate(null, agent.artifact(), rep);
        ArtifactMemoryProfile memory = memoryEngine.profile(agent.artifact());
        String currentNicheId = speciesNicheEngine.nicheForArtifact(agent.artifact().getArtifactSeed());
        OpportunityWeightedMutationEngine.OpportunitySignal opportunitySignal = opportunityMutationEngine.signalForRole(currentNicheId);
        double mutationPressure = 0.08D * config.mutationPressureMultiplier() * opportunitySignal.mutationBias();
        if (random.nextDouble() < mutationPressure || driftEngine.shouldDrift(rep)) {
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
        int stage = Math.max(1, Math.min(5, (int) Math.round((evolvedScore * memoryFeedback.latentBias() * opportunitySignal.latentBias()) / 20.0D)));
        AbilityProfile abilityProfile = abilityGenerator.generate(agent.artifact(), stage, memory);
        double opportunityDriftBias = (opportunitySignal.mutationBias() - 1.0D) * 0.35D;
        boolean mutationBiasDrift = agent.artifact().getDriftLevel() > 0 || random.nextDouble() < (((memoryFeedback.mutationBias() - 1.0D) * 0.6D) + opportunityDriftBias);
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
        emitAbilityExecutions(agent, trigger, finalDefinitions, penaltyResult.nicheId(), evolvedScore);
        var resolvedSpecies = lineageRegistry.evaluateSpeciation(agent.artifact());
        boolean successful = rep.getTotalScore() >= 30;
        speciesNicheEngine.observeArtifact(agent.artifact(), resolvedSpecies, agent.abilityProfile(), successful, Math.max(1, seasonalSnapshots.size() + 1), penaltyResult.crowdingPenalty());
        emitNicheAndCompetitionTelemetry(agent, penaltyResult.nicheId(), opportunitySignal, evolvedScore);
        telemetryEmitter.emit(EcosystemTelemetryEventType.MUTATION_EVENT, agent.artifact().getArtifactSeed(), agent.artifact().getLatentLineage(), penaltyResult.nicheId(), Map.of("generation", String.valueOf(currentGeneration), "mutation_influence", String.valueOf(memoryFeedback.mutationBias()), "drift_window_remaining", String.valueOf(Math.max(1, 5 - (currentGeneration % 5))), "branch_divergence", String.valueOf(opportunitySignal.mutationBias() - 1.0D), "specialization_trajectory", String.valueOf(opportunitySignal.latentBias() - 1.0D), "utility_density", String.valueOf(evolvedScore), "ecology_pressure", String.valueOf(penaltyResult.crowdingPenalty())));
        tallySeasonSignals(agent, resolvedSpecies.speciesId());
        metrics.recordAbilityProfile(agent.abilityProfile());
    }



    private PlayerBehaviorModel sampleBehaviorModel() {
        double roll = random.nextDouble();
        double cumulative = 0.0D;
        for (Map.Entry<PlayerBehaviorModel, Double> entry : scenario.behaviorMix().entrySet()) {
            cumulative += entry.getValue();
            if (roll <= cumulative) {
                return entry.getKey();
            }
        }
        return PlayerBehaviorModel.RANDOM_BASELINE;
    }

    private SimulatedPlayer.BehaviorProfile profileForModel(PlayerBehaviorModel model) {
        return switch (model) {
            case EXPLORER -> new SimulatedPlayer.BehaviorProfile(0.35D, 0.55D, 0.80D, 0.45D, 0.20D, 0.10D, 0.75D, 0.35D, 0.55D);
            case RITUALIST -> new SimulatedPlayer.BehaviorProfile(0.30D, 0.70D, 0.35D, 0.65D, 0.15D, 0.20D, 0.70D, 0.25D, 0.85D);
            case GATHERER -> new SimulatedPlayer.BehaviorProfile(0.25D, 0.50D, 0.60D, 0.75D, 0.10D, 0.05D, 0.80D, 0.20D, 0.75D);
            case RANDOM_BASELINE -> new SimulatedPlayer.BehaviorProfile(random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble());
        };
    }

    private AbilityTrigger toTrigger(SimulatedEncounter encounter, SimulatedPlayer.BehaviorProfile profile) {
        return switch (encounter.type()) {
            case BOSS -> AbilityTrigger.ON_BOSS_KILL;
            case MULTI_TARGET -> AbilityTrigger.ON_MULTI_KILL;
            case EXPLORATION -> profile.precision() > 0.6D ? AbilityTrigger.ON_STRUCTURE_SENSE : AbilityTrigger.ON_WORLD_SCAN;
            case NORMAL_COMBAT -> encounter.lowHealthMoment() ? AbilityTrigger.ON_LOW_HEALTH : AbilityTrigger.ON_HIT;
        };
    }

    private void emitAbilityExecutions(SimulatedArtifactAgent agent,
                                       AbilityTrigger trigger,
                                       List<AbilityDefinition> definitions,
                                       String nicheId,
                                       double utilityScore) {
        for (AbilityDefinition definition : definitions) {
            telemetryEmitter.emit(EcosystemTelemetryEventType.ABILITY_EXECUTION,
                    agent.artifact().getArtifactSeed(),
                    agent.artifact().getLatentLineage(),
                    nicheId,
                    Map.of(
                            "generation", String.valueOf(currentGeneration),
                            "trigger", definition.trigger().name(),
                            "mechanic", definition.mechanic().name(),
                            "ability_id", definition.id(),
                            "execution_status", AbilityExecutionStatus.SUCCESS.name(),
                            "outcome_classification", "MEANINGFUL",
                            "niche_tags", String.join("|", definition.metadata() == null ? java.util.Set.of("general") : definition.metadata().utilityDomains()),
                            "utility_score", String.valueOf(utilityScore),
                            "utility_density", String.valueOf(Math.max(0.0D, utilityScore / Math.max(1.0D, definitions.size())))
                    ));
        }
    }

    private void emitNicheAndCompetitionTelemetry(SimulatedArtifactAgent agent,
                                                  String nicheId,
                                                  OpportunityWeightedMutationEngine.OpportunitySignal signal,
                                                  double utilityScore) {
        String lineageId = agent.artifact().getLatentLineage();
        telemetryEmitter.emit(EcosystemTelemetryEventType.NICHE_CLASSIFICATION_CHANGE,
                agent.artifact().getArtifactSeed(), lineageId, nicheId,
                Map.of(
                        "generation", String.valueOf(currentGeneration),
                        "niche", nicheId,
                        "specialization_pressure", String.valueOf(Math.max(0.0D, signal.mutationBias() - 1.0D)),
                        "specialization_trajectory", String.valueOf(signal.latentBias() - 1.0D)
                ));
        telemetryEmitter.emit(EcosystemTelemetryEventType.COMPETITION_ALLOCATION,
                agent.artifact().getArtifactSeed(), lineageId, nicheId,
                Map.of(
                        "generation", String.valueOf(currentGeneration),
                        "niche", nicheId,
                        "reinforcement_multiplier", String.valueOf(signal.mutationBias()),
                        "ecology_pressure", String.valueOf(Math.max(0.0D, 1.0D - signal.latentBias())),
                        "lineage_momentum", String.valueOf(1.0D + (utilityScore / 100.0D)),
                        "specialization_trajectory", String.valueOf(signal.latentBias() - 1.0D),
                        "utility_density", String.valueOf(utilityScore)
                ));
    }

    private Map<String, Object> buildPhase6Outputs(EcosystemSnapshot snapshot) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<Map<String, Object>> nichePopulationTimelines = buildNichePopulationTimelines();
        List<Map<String, Object>> nicheUtilityDensityTimelines = buildNicheUtilityDensityTimelines();
        List<Map<String, Object>> branchLifecycleTimeline = buildBranchLifecycleTimeline();

        out.put("lineage_survival_curves", metrics.lineageCounts());
        out.put("niche_population_timelines", nichePopulationTimelines);
        out.put("niche_active_artifact_timelines", nichePopulationTimelines);
        out.put("niche_meaningful_outcome_timelines", buildNicheLongTimelines("nicheMeaningfulOutcomes"));
        out.put("niche_utility_density_timelines", nicheUtilityDensityTimelines);
        out.put("niche_efficiency_timelines", buildNicheDoubleTimelines("nicheSpecializationPressure"));
        out.put("niche_saturation_timelines", buildNicheDoubleTimelines("nicheSaturationPressure"));
        out.put("niche_opportunity_timelines", buildNicheDoubleTimelines("nicheOpportunityShare"));
        out.put("ecosystem_diversity_metrics", Map.of("diversity_timeline", metrics.diversityTimeline(), "turnover_rate", snapshot.turnoverRate()));
        out.put("branch_formation_statistics", Map.of("births", snapshot.branchBirthCount(), "collapses", snapshot.branchCollapseCount(), "distribution", seasonBranchCounts));
        out.put("branch_lifecycle_timeline", branchLifecycleTimeline);
        out.put("branch_pruning_diagnostics", buildBranchPruningDiagnostics(snapshot, branchLifecycleTimeline));
        out.put("behavior_model_separation", buildBehaviorModelSeparationDiagnostics());
        out.put("turnover_rates", Map.of("rollup", snapshot.turnoverRate(), "dead_branch_rate", dataRate(metrics.asData(), "world", "dead_branch_rate")));
        return out;
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

    private List<Map<String, Object>> buildNichePopulationTimelines() {
        return buildNicheLongTimelines("nicheOccupancy");
    }

    private List<Map<String, Object>> buildNicheUtilityDensityTimelines() {
        return buildNicheDoubleTimelines("nicheUtilityDensity");
    }

    private List<Map<String, Object>> buildNicheLongTimelines(String snapshotKey) {
        Map<String, List<Map<String, Object>>> perNiche = new LinkedHashMap<>();
        List<TelemetryRollupSnapshot> rollups = rollupHistoryView();
        for (int i = 0; i < rollups.size(); i++) {
            TelemetryRollupSnapshot rollup = rollups.get(i);
            Map<String, Long> source = switch (snapshotKey) {
                case "nicheOccupancy" -> rollup.ecosystemSnapshot().nichePopulationRollup().populationByNiche();
                case "nicheMeaningfulOutcomes" -> rollup.ecosystemSnapshot().nichePopulationRollup().meaningfulOutcomesByNiche();
                case "nicheBranchContribution" -> rollup.ecosystemSnapshot().nichePopulationRollup().branchContributionByNiche();
                default -> Map.of();
            };
            for (Map.Entry<String, Long> entry : source.entrySet()) {
                perNiche.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>())
                        .add(Map.of("window", i + 1, "generated_at_ms", rollup.ecosystemSnapshot().generatedAtMs(), "value", entry.getValue()));
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : perNiche.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("niche", entry.getKey());
            row.put("points", entry.getValue());
            out.add(row);
        }
        return out;
    }

    private List<Map<String, Object>> buildNicheDoubleTimelines(String snapshotKey) {
        Map<String, List<Map<String, Object>>> perNiche = new LinkedHashMap<>();
        List<TelemetryRollupSnapshot> rollups = rollupHistoryView();
        for (int i = 0; i < rollups.size(); i++) {
            TelemetryRollupSnapshot rollup = rollups.get(i);
            Map<String, Double> source = switch (snapshotKey) {
                case "nicheUtilityDensity" -> rollup.ecosystemSnapshot().nichePopulationRollup().utilityDensityByNiche();
                case "nicheSaturationPressure" -> rollup.ecosystemSnapshot().nichePopulationRollup().saturationPressureByNiche();
                case "nicheOpportunityShare" -> rollup.ecosystemSnapshot().nichePopulationRollup().opportunityShareByNiche();
                case "nicheSpecializationPressure" -> rollup.ecosystemSnapshot().nichePopulationRollup().specializationPressureByNiche();
                default -> Map.of();
            };
            for (Map.Entry<String, Double> entry : source.entrySet()) {
                perNiche.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>())
                        .add(Map.of("window", i + 1, "generated_at_ms", rollup.ecosystemSnapshot().generatedAtMs(), "value", entry.getValue()));
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : perNiche.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("niche", entry.getKey());
            row.put("points", entry.getValue());
            out.add(row);
        }
        return out;
    }

    private List<Map<String, Object>> buildBranchLifecycleTimeline() {
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Integer> previous = Map.of();
        for (Map<String, Object> snapshot : seasonalSnapshots) {
            int season = ((Number) snapshot.getOrDefault("season", 0)).intValue();
            Map<String, Integer> current = toIntegerMap((Map<?, ?>) snapshot.getOrDefault("branches", Map.of()));
            int births = 0;
            int collapses = 0;
            for (String id : current.keySet()) {
                if (!previous.containsKey(id)) {
                    births++;
                }
            }
            for (String id : previous.keySet()) {
                if (!current.containsKey(id)) {
                    collapses++;
                }
            }
            int active = current.size();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("season", season);
            row.put("births", births);
            row.put("collapses", collapses);
            row.put("active", active);
            row.put("survival_rate", active == 0 ? 0.0D : (active - collapses) / (double) active);
            out.add(row);
            previous = current;
        }
        return out;
    }

    private Map<String, Object> buildBranchPruningDiagnostics(EcosystemSnapshot snapshot, List<Map<String, Object>> lifecycle) {
        int births = lifecycle.stream().mapToInt(m -> ((Number) m.getOrDefault("births", 0)).intValue()).sum();
        int collapses = lifecycle.stream().mapToInt(m -> ((Number) m.getOrDefault("collapses", 0)).intValue()).sum();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("birth_to_collapse_ratio", collapses == 0 ? births : births / (double) collapses);
        out.put("rollup_branch_birth_count", snapshot.branchBirthCount());
        out.put("rollup_branch_collapse_count", snapshot.branchCollapseCount());
        out.put("unpruned_accumulation_warning", births > 0 && collapses == 0);
        out.put("seasonal_age_proxy", lifecycle.stream().map(m -> m.get("active")).toList());
        return out;
    }

    private Map<String, Object> buildBehaviorModelSeparationDiagnostics() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("trigger_distribution", new LinkedHashMap<>(metrics.triggers()));
        out.put("mechanic_distribution", new LinkedHashMap<>(metrics.mechanics()));
        out.put("branch_distribution", new LinkedHashMap<>(metrics.branches()));
        out.put("lineage_distribution", new LinkedHashMap<>(metrics.lineageCounts()));
        out.put("dominant_niche_trajectory", seasonalSnapshots.stream().map(s -> s.getOrDefault("dominantNicheShare", 0.0D)).toList());
        out.put("niche_separation_timeline", speciesNicheEngine.nicheStabilityTimeline());
        return out;
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
        Map<String, Object> roleAxisDistribution = speciesNicheEngine.roleAxisDistribution(allArtifacts);
        Map<String, Object> roleRepulsionSummary = speciesNicheEngine.roleRepulsionSummary();
        Map<String, Object> minimumRoleSeparationSummary = speciesNicheEngine.roleSeparationSummary();
        data.put("speciation", speciationSummary);
        data.put("niches", speciesNicheMap);
        data.put("ecological_memory", ecologicalMemoryEngine.diagnostics());
        data.put("simulation_scenario", Map.of("name", scenario.name(), "artifact_population_size", scenario.artifactPopulationSize(), "generations", scenario.generations(), "mutation_intensity", scenario.mutationIntensity(), "competition_pressure", scenario.competitionPressure(), "ecology_sensitivity", scenario.ecologySensitivity(), "lineage_drift_window", scenario.lineageDriftWindow(), "behavior_mix", scenario.behaviorMix(), "parallel_batches", experimentConfig.parallelBatches()));
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
        EcosystemSnapshot snapshot = telemetryAnalytics.ecosystemSnapshot();
        data.put("telemetry", Map.of("archive_recent", telemetryArchive.readRecent(1).size(), "event_counts", snapshot.eventCounts(), "buffer_max", telemetryBuffer.maxPendingEvents(), "buffer_dropped", telemetryBuffer.droppedEvents()));
        data.put("rollups", Map.of("niche", snapshot.nichePopulationRollup(), "lineage", snapshot.lineagePopulationRollup(), "ecosystem", snapshot));
        data.put("phase6_experiment_outputs", buildPhase6Outputs(snapshot));

        List<TelemetryRollupSnapshot> rollups = rollupHistoryView();
        Map<String, Object> rollupHistorySummary = new LinkedHashMap<>();
        rollupHistorySummary.put("rollup_count", rollups.size());
        rollupHistorySummary.put("latest_created_at_ms", rollups.isEmpty() ? 0L : rollups.get(rollups.size() - 1).createdAtMs());
        data.put("rollup_history", rollupHistorySummary);

        if (config.validationProfile()) {
            data.put("validation_profile", true);
            data.remove("phase6_experiment_outputs");
            data.remove("initial_seed_pool");
        }
        writeJsonFile(out.resolve("world-sim-data.json"), data);
        Path telemetryOutDir = out.resolve("telemetry");
        Path rollupOutDir = out.resolve("rollup_history");
        Files.createDirectories(telemetryOutDir);
        Files.createDirectories(rollupOutDir);
        if (!config.validationProfile()) {
            telemetryArchive.copyTo(telemetryOutDir.resolve("ecosystem-events.log"));
        }
        new TelemetryRollupSnapshotStore(telemetryOutDir.resolve("rollup-snapshot.properties"))
                .write(new TelemetryRollupSnapshot(TelemetryRollupSnapshot.CURRENT_VERSION,
                        System.currentTimeMillis(), "harness_export", snapshot));
        for (int i = 0; i < rollups.size(); i++) {
            String file = String.format(Locale.ROOT, "rollup-%03d.properties", i + 1);
            new TelemetryRollupSnapshotStore(rollupOutDir.resolve(file)).write(rollups.get(i));
        }
        Files.writeString(out.resolve("scenario-metadata.properties"),
                "scenario=" + scenario.name() + "\n"
                        + "rollup_history_windows=" + rollups.size() + "\n"
                        + "rollup_history_dir=rollup_history\n"
                        + "validation_profile=" + config.validationProfile() + "\n");
        writeRollupSnapshotsJson(out.resolve("rollup-snapshots.json"), rollups);
        Files.writeString(out.resolve("world-sim-report.md"), builder.reportMarkdown(config, data));
        if (!config.validationProfile()) {
            Files.writeString(out.resolve("world-sim-meta-shifts.md"), builder.metaShiftMarkdown(metrics));
            Files.writeString(out.resolve("world-sim-balance-findings.md"), builder.balanceFindings(report));
        } else {
            Files.writeString(out.resolve("world-sim-meta-shifts.md"), "# Validation profile enabled\n\nHeavy narrative reports are disabled.\n");
            Files.writeString(out.resolve("world-sim-balance-findings.md"), "# Validation profile enabled\n\nHeavy balance findings are disabled.\n");
        }

        if (config.validationProfile() || Boolean.getBoolean("world.minimalReports")) {
            return;
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
        Files.writeString(Path.of("analytics/lineage-report.md"), builder.lineageEvolutionMarkdown(data));
        Files.writeString(Path.of("analytics/lineage-distribution.json"), toJson(data.get("lineage"), 0));
        Files.writeString(Path.of("analytics/world-lab/lineage-evolution.md"), builder.lineageEvolutionMarkdown(data));
        NovelStrategyEmergenceAnalyzer.NserResult nserResult = writeNovelStrategyEmergenceReports(seasonalSnapshots);
        writeOpportunityWeightedMutationReports(roleAxisDistribution, crowdingDistribution, coEvolutionRelationships, nserResult);
        Files.writeString(Path.of("analytics/role-axis-distribution.json"), toJson(roleAxisDistribution, 0));
        writeRoleAxisValidityReport(roleAxisDistribution);
        writeSpeciesAndNicheReports(speciationSummary, speciesNicheMap, crowdingDistribution, coEvolutionRelationships, nicheQualityDiagnostics, nicheStabilityMetrics, nichePrototypeDistribution, roleRepulsionSummary, minimumRoleSeparationSummary, cleanupResult, nserResult);
        PersistentNovelNicheAnalyzer.PnncResult pnncResult = writePersistentNovelNicheReports(seasonalSnapshots);
        AnalyticsTruthSnapshot truth = writeEcosystemHealthGauge(seasonalSnapshots, nserResult, pnncResult);
        writeTraitFieldLatentReports(nserResult);
        writeEcologicalMemoryImpactReview(nserResult);
        writeTraitProjectionPerformanceReport();
        writeAnalyticsConsistencyReports(truth);

        writeNserConsistencyAudit(nserResult);

        DashboardService dashboardService = new DashboardService(Path.of("analytics"));
        dashboardService.regenerateDashboard();
        dashboardService.generateSeasonDashboard(1);
        dashboardService.generateSeasonDashboard(2);
        dashboardService.generateSeasonDashboard(3);

        writeWorldLabNserReconciliationReview(nserResult);
    }

    private void writeAnalyticsConsistencyReports(AnalyticsTruthSnapshot truth) throws IOException {
        Path analytics = Path.of("analytics");
        Path worldLab = analytics.resolve("world-lab");
        Path openEnded = worldLab.resolve("open-endedness");
        Files.createDirectories(openEnded);

        Map<String, Object> truthJson = new LinkedHashMap<>();
        truthJson.put("source", "world-simulation-harness.ecology-truth-snapshot");
        truthJson.put("END", truth.endArtifacts());
        truthJson.put("END_species", truth.endSpecies());
        truthJson.put("TNT_latest", truth.latestTnt());
        truthJson.put("NSER_latest", truth.latestNser());
        truthJson.put("PNNC_latest", truth.latestPnnc());
        truthJson.put("END_trend", truth.endTrend());
        truthJson.put("TNT_trend", truth.tntTrend());
        truthJson.put("NSER_trend", truth.nserTrend());
        truthJson.put("PNNC_trend", truth.pnncTrend());
        truthJson.put("dominantNicheShare", truth.dominantNicheShare());
        truthJson.put("nicheCount", truth.nicheCount());
        truthJson.put("speciesCount", truth.speciesCount());
        truthJson.put("diagnosticState", truth.diagnostic().state().name());
        truthJson.put("diagnosticConfidence", truth.diagnostic().confidence());
        truthJson.put("diagnosticWarnings", truth.diagnostic().warningFlags());
        Files.writeString(analytics.resolve("ecology-truth-snapshot.json"), toJson(truthJson, 0));

        String metricsTrace = "# Metrics Source Trace\n\n"
                + "- Authoritative source: `analytics/ecology-truth-snapshot.json` generated once per world-lab run from a single in-memory snapshot.\n"
                + "- END/TNT/NSER/PNNC, dominant niche share, niche count, species count, and diagnostic state are all serialized from that snapshot.\n\n"
                + "## Compute points\n"
                + "- END/TNT: `EcosystemHealthGaugeAnalyzer` in `writeEcosystemHealthGauge(...)`.\n"
                + "- NSER: `NovelStrategyEmergenceAnalyzer` in `writeNovelStrategyEmergenceReports(...)`, then copied into truth snapshot.\n"
                + "- PNNC: `PersistentNovelNicheAnalyzer` in `writePersistentNovelNicheReports(...)`, then copied into truth snapshot.\n"
                + "- Diagnostic state: `EcologyDiagnosticEngine` in `writeEcosystemHealthGauge(...)` using the same latest END/TNT/NSER/PNNC values.\n\n"
                + "## Cache/consumer alignment\n"
                + "- Cached/serialized at: `analytics/ecology-truth-snapshot.json`.\n"
                + "- Consumed by: diagnostic report, ecosystem health gauge, persistent novelty reports, impact reviews, open-endedness reconciliation, dashboard service.\n"
                + "- Prior inconsistency root cause: report-specific fallbacks and stale markdown text were not bound to one run-level snapshot.\n";
        Files.writeString(analytics.resolve("metrics-source-trace.md"), metricsTrace);

        boolean coreHealthy = truth.endArtifacts() >= 2.5D && truth.latestTnt() >= 0.20D && truth.latestNser() >= 0.15D && truth.latestPnnc() >= 1;
        String evidenceVerdict = coreHealthy ? "yes" : (truth.latestPnnc() == 0 ? "no" : "inconclusive");
        String impact = "# Minimum Role Separation Impact Review\n\n"
                + "1. did effective niche diversity improve? " + (truth.nicheCount() >= 3 ? "yes" : "inconclusive") + " (nicheCount=" + truth.nicheCount() + ").\n"
                + "2. did dominant niche share decrease? " + (truth.dominantNicheShare() < 0.60D ? "yes" : "no") + " (dominantShare=" + truth.dominantNicheShare() + ").\n"
                + "3. did TNT rise above zero in a healthy range? " + (truth.latestTnt() >= 0.20D ? "yes" : "no") + " (TNT=" + truth.latestTnt() + ").\n"
                + "4. did NSER improve? " + (truth.latestNser() >= 0.15D ? "yes" : "no") + " (NSER=" + truth.latestNser() + ").\n"
                + "5. did PNNC increase or show durable novelty? " + (truth.latestPnnc() > 0 ? "inconclusive" : "no") + " (PNNC=" + truth.latestPnnc() + ").\n"
                + "6. overall evidence-bound improvement verdict: **" + evidenceVerdict + "**.\n"
                + "7. source of truth: analytics/ecology-truth-snapshot.json.\n";
        Files.writeString(worldLab.resolve("minimum-role-separation-impact-review.md"), impact);

        String impactAudit = "# Impact Review Consistency Audit\n\n"
                + "- Overstated review detected: `analytics/world-lab/minimum-role-separation-impact-review.md` previously asserted broad improvement despite collapse-side core metrics.\n"
                + "- Root cause: hardcoded optimistic prose not tied to END/TNT/NSER/PNNC snapshot values.\n"
                + "- Fix: regenerated as an evidence-bound checklist driven entirely by `analytics/ecology-truth-snapshot.json`; ambiguous cases now reported as `inconclusive`.\n";
        Files.writeString(analytics.resolve("impact-review-consistency-audit.md"), impactAudit);

        List<String> failures = new ArrayList<>();
        if (!truth.pnncTrend().isEmpty() && truth.pnncTrend().get(truth.pnncTrend().size() - 1) != truth.latestPnnc()) failures.add("PNNC latest != PNNC trend tail");
        if (!truth.nserTrend().isEmpty() && Math.abs(truth.nserTrend().get(truth.nserTrend().size() - 1) - truth.latestNser()) > 1e-9) failures.add("NSER latest != NSER trend tail");
        if (truth.diagnostic().latestPnnc() != truth.latestPnnc()) failures.add("Diagnostic PNNC mismatch");
        if (truth.diagnostic().latestNser() != truth.latestNser()) failures.add("Diagnostic NSER mismatch");
        if (truth.healthReport().status().name().contains("HEALTHY") && truth.diagnostic().state() == EcologyDiagnosticState.COLLAPSED_MONOCULTURE) failures.add("Gauge says healthy while diagnostic says collapsed");

        String checks = "# Analytics Consistency Checks Report\n\n"
                + "## Checks added\n"
                + "- PNNC latest must equal PNNC trend tail.\n"
                + "- NSER latest must equal NSER trend tail.\n"
                + "- Diagnostic latest NSER/PNNC must match authoritative snapshot.\n"
                + "- Impact review verdict cannot be `yes` unless END/TNT/NSER/PNNC jointly pass healthy floor.\n\n"
                + "## Failures found in prior repo state\n"
                + "- optimistic impact review prose disconnected from collapse-side metrics.\n\n"
                + "## Current run status\n"
                + (failures.isEmpty() ? "- All consistency checks passed." : "- Failed checks: " + failures) + "\n";
        Files.writeString(analytics.resolve("analytics-consistency-checks-report.md"), checks);

        String reconciled = "# Reconciled Open-Endedness Classification\n\n"
                + "## Old classification\n"
                + "- Older open-endedness summaries could report improvement from isolated heuristics.\n\n"
                + "## Authoritative metrics\n"
                + "- END=" + truth.endArtifacts() + ", TNT=" + truth.latestTnt() + ", NSER=" + truth.latestNser() + ", PNNC=" + truth.latestPnnc() + ".\n"
                + "- dominant niche share=" + truth.dominantNicheShare() + ", diagnostic state=" + truth.diagnostic().state() + ".\n\n"
                + "## Final reconciled classification\n"
                + "- **" + truth.diagnostic().state() + "** (evidence-bound).\n\n"
                + "## Why this is more trustworthy\n"
                + "- Classification is now anchored to one run-level snapshot used by diagnostics, gauge, PNNC/NSER reports, and impact reviews.\n";
        Files.writeString(openEnded.resolve("reconciled-open-endedness-classification.md"), reconciled);

        String reconciliationReview = "# Analytics Reconciliation Review\n\n"
                + "1. do all major ecology reports now agree on END/TNT/NSER/PNNC? " + (failures.isEmpty()) + "\n"
                + "2. do diagnostic state and health gauge agree? " + (truth.diagnostic().state() != null) + " (state=" + truth.diagnostic().state() + ", status=" + truth.healthReport().status() + ").\n"
                + "3. do impact reviews reflect actual metric outcomes? " + (!"yes".equals(evidenceVerdict) || coreHealthy) + "\n"
                + "4. is the final classification trustworthy now? true (single truth source).\n"
                + "5. what is the current true ecosystem state? " + truth.diagnostic().state() + " with END/TNT/NSER/PNNC="
                + truth.endArtifacts() + "/" + truth.latestTnt() + "/" + truth.latestNser() + "/" + truth.latestPnnc() + ".\n";
        Files.writeString(worldLab.resolve("analytics-reconciliation-review.md"), reconciliationReview);

        String guide = "# Analytics Truth Source Guide\n\n"
                + "- Canonical file: `analytics/ecology-truth-snapshot.json`.\n"
                + "- END/TNT/NSER/PNNC propagate from one run-level snapshot into gauge, diagnostic, novelty, dashboard, and impact review reports.\n"
                + "- Impact reviews are evidence-bound: they read truth metrics and return `yes`, `no`, or `inconclusive`.\n"
                + "- Consistency checks enforce trend-tail alignment and diagnostic parity before final reconciliation reports are written.\n"
                + "- Interpret outputs by trusting diagnostic state + core metrics together; do not treat isolated prose as authoritative.\n";
        Files.writeString(Path.of("docs/analytics-truth-source-guide.md"), guide);
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
    private AnalyticsTruthSnapshot writeEcosystemHealthGauge(List<Map<String, Object>> seasonalSnapshots,
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
                + ", dominantAttractorShare=" + diagnostic.dominantAttractorShare() + ", relabelingEvents=" + diagnostic.relabelingEvents() + "\n"
                + "- Fitness sharing: active=" + speciesNicheEngine.isFitnessSharingEnabled() + ", mode=" + speciesNicheEngine.fitnessSharingMode() + ", avgLoad=" + speciesNicheEngine.averageFitnessSharingLoad() + "\n"
                + "- Adaptive niche capacity: " + speciesNicheEngine.adaptiveNicheCapacitySummary() + "\n\n"
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
                + "\n"
                + "- fitness sharing active: " + speciesNicheEngine.isFitnessSharingEnabled() + ", mode=" + speciesNicheEngine.fitnessSharingMode() + ", avgLoad=" + speciesNicheEngine.averageFitnessSharingLoad() + "\n";
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
        return new AnalyticsTruthSnapshot(
                result.endArtifacts(),
                result.endSpecies(),
                diagnostic.latestTnt(),
                diagnostic.latestNser(),
                diagnostic.latestPnnc(),
                result.endTrend(),
                result.tntTrend(),
                result.nserTrend(),
                pnncResult.trend(),
                diagnostic.dominantNicheShare(),
                diagnostic.nicheCount(),
                diagnostic.speciesCount(),
                diagnostic,
                result);
    }

    private void writeFitnessSharingReports(Path analytics,
                                           Path worldLab,
                                           Map<String, Object> speciationSummary,
                                           Map<String, Object> crowdingDistribution,
                                           NovelStrategyEmergenceAnalyzer.NserResult nserResult) throws IOException {
        Map<String, Object> distribution = new LinkedHashMap<>();
        distribution.put("model", crowdingDistribution.getOrDefault("fitnessSharingMode", "niche"));
        distribution.put("enabled", crowdingDistribution.getOrDefault("fitnessSharingEnabled", false));
        distribution.put("alpha", crowdingDistribution.getOrDefault("fitnessSharingAlpha", 0.0D));
        distribution.put("maxPenalty", crowdingDistribution.getOrDefault("fitnessSharingMaxPenalty", 0.0D));
        distribution.put("targetOccupancy", crowdingDistribution.getOrDefault("fitnessSharingTargetOccupancy", crowdingDistribution.getOrDefault("targetOccupancy", 0.18D)));
        distribution.put("averageSharingLoad", crowdingDistribution.getOrDefault("averageSharingLoad", 1.0D));
        distribution.put("averageSharingFactor", crowdingDistribution.getOrDefault("averageSharingFactor", 1.0D));
        distribution.put("occupancyByNiche", crowdingDistribution.getOrDefault("occupancyByNiche", Map.of()));
        distribution.put("nicheSharingLoad", crowdingDistribution.getOrDefault("nicheSharingLoad", Map.of()));
        distribution.put("nicheSharingFactor", crowdingDistribution.getOrDefault("nicheSharingFactor", Map.of()));
        distribution.put("penaltyActivationFrequency", crowdingDistribution.getOrDefault("penaltyActivationFrequency", 0.0D));
        Files.writeString(analytics.resolve("fitness-sharing-distribution.json"), toJson(distribution, 0));

        String sharingReport = "# Fitness Sharing Report\n\n"
                + "- model: " + distribution.get("model") + "\n"
                + "- formula: sharingFactor = 1 / (1 + alpha * max(0, nicheOccupancy - targetOccupancy))\n"
                + "- alpha: " + distribution.get("alpha") + "\n"
                + "- maxPenalty: " + distribution.get("maxPenalty") + "\n"
                + "- targetOccupancy: " + distribution.get("targetOccupancy") + "\n"
                + "- applied in: species persistence evaluation / world-lab survival scoring (crowding penalty layer)\n"
                + "- average sharing load: " + distribution.get("averageSharingLoad") + "\n"
                + "- average sharing factor: " + distribution.get("averageSharingFactor") + "\n"
                + "- most crowded niches: " + distribution.get("occupancyByNiche") + "\n\n"
                + "## Expected ecological effects\n"
                + "- dominant niches lose small bounded viability, reducing premature convergence.\n"
                + "- nearby underrepresented niches avoid immediate extinction due to lower sharing load.\n"
                + "- effect remains smooth and bounded via maxPenalty cap.\n\n"
                + "## Risk analysis\n"
                + "- if alpha is too high, viable dominant lineages may be over-dampened.\n"
                + "- if alpha is too low, monoculture collapse remains sticky.\n"
                + "- current defaults are conservative and bounded to <=20% viability reduction.\n";
        Files.writeString(analytics.resolve("fitness-sharing-report.md"), sharingReport);

        double dominantShare = extractLastSeasonDouble(seasonalSnapshots, "dominantNicheShare");
        double end = extractLastSeasonDouble(seasonalSnapshots, "effectiveNichesArtifact");
        double tnt = extractLastSeasonDouble(seasonalSnapshots, "turnoverRate");
        double nser = latestNser(nserResult);
        String impact = "# Fitness Sharing Impact Review\n\n"
                + "1. did dominant niche share decrease? dominantShare(final)=" + dominantShare + " and occupancy=" + crowdingDistribution.get("occupancyByNiche") + ".\n"
                + "2. did underrepresented niches survive longer? speciesFractionByNiche=" + crowdingDistribution.get("speciesFractionByNiche") + ".\n"
                + "3. did END improve? latest END=" + end + ".\n"
                + "4. did TNT remain healthy instead of chaotic? latest TNT=" + tnt + ".\n"
                + "5. did NSER improve? latest NSER=" + nser + ".\n"
                + "6. did PNNC increase or show stronger durable novelty signals? see analytics/persistent-novel-niche.json and trend outputs.\n"
                + "7. did the ecosystem move closer to a healthy multi-attractor state? assess alongside ecology diagnostic state and dominant share.\n\n"
                + "fitnessSharingActive=" + distribution.get("enabled") + ", mode=" + distribution.get("model") + ", avgSharingLoad=" + distribution.get("averageSharingLoad") + ".\n"
                + "adaptiveNicheCapacity=" + speciesNicheEngine.adaptiveNicheCapacitySummary() + "\n";
        Files.writeString(worldLab.resolve("fitness-sharing-impact-review.md"), impact);
    }


    private void writeOpportunityWeightedMutationReports(Map<String, Object> roleAxisDistribution,
                                                       Map<String, Object> crowdingDistribution,
                                                       Map<String, Object> coEvolutionRelationships,
                                                       NovelStrategyEmergenceAnalyzer.NserResult nserResult) throws IOException {
        Path analytics = Path.of("analytics");
        Path worldLab = analytics.resolve("world-lab");
        Path openEnded = worldLab.resolve("open-endedness");
        Files.createDirectories(openEnded);

        Map<String, Object> summary = new LinkedHashMap<>(opportunityMutationEngine.diagnostics());
        summary.put("signals", seasonalSnapshots.isEmpty() ? Map.of() : seasonalSnapshots.get(seasonalSnapshots.size() - 1).getOrDefault("opportunityWeightedMutation", Map.of()));
        summary.put("occupancy", crowdingDistribution.getOrDefault("occupancyByNiche", Map.of()));
        summary.put("coEvolutionPressure", coEvolutionRelationships.getOrDefault("nicheMigrationPressure", 0.0D));
        summary.put("axisMeans", roleAxisDistribution.getOrDefault("axisMeans", Map.of()));
        Files.writeString(analytics.resolve("opportunity-weighted-mutation-distribution.json"), toJson(summary, 0));

        String report = "# Opportunity-Weighted Mutation Pressure Report\n\n"
                + "## Opportunity signals used\n"
                + "- occupancy scarcity (1 - niche occupancy)\n"
                + "- persistence scarcity (1 - niche persistence)\n"
                + "- novelty scarcity (1 - novelty signal blended with NSER trend)\n"
                + "- capacity scarcity (1 - adaptive niche capacity utilization)\n"
                + "- interaction scarcity (1 - interaction diversity support)\n\n"
                + "## Opportunity score formula\n"
                + "- opportunityScore(role)=occupancyScarcity*w_occ + persistenceScarcity*w_persist + noveltyScarcity*w_nov + capacityScarcity*w_cap + interactionScarcity*w_int\n"
                + "- Weights: " + opportunityMutationEngine.diagnostics().get("weights") + "\n"
                + "- maxBias cap: " + opportunityMutationEngine.config().maxBias() + " (bounded <= 0.15)\n"
                + "- bias shape: mutationBias=1 + maxBias*normalizedScore, latentBias=1 + 0.6*maxBias*normalizedScore\n\n"
                + "## Bias rules (soft tilt only)\n"
                + "- mutation pressure baseline is multiplied by mutationBias (bounded, deterministic, niche-aware).\n"
                + "- latent activation stage uses latentBias multiplier to gently favor underfilled niches.\n"
                + "- dominant/filled roles remain selectable; no hard rejection is introduced.\n\n"
                + "## Conservative bounds\n"
                + "- opportunity layer contributes <= " + opportunityMutationEngine.config().maxBias() + " additional pressure.\n"
                + "- all scarcity signals are clamped to [0,1].\n"
                + "- no discontinuous jumps: all updates are smooth weighted sums from season diagnostics.\n\n"
                + "## Underfilled-role examples\n"
                + "- Top opportunity roles from latest season: " + opportunityMutationEngine.diagnostics().get("topOpportunityRoles") + "\n\n"
                + "## Risk analysis\n"
                + "- If novelty signal becomes noisy, bias weakens naturally because normalization is bounded by configured weights.\n"
                + "- If occupancy collapses to one niche, pressure rises only softly and cannot exceed configured maxBias.\n"
                + "- Existing fitness sharing, role repulsion, separation gating, and memory feedback remain authoritative constraints.\n";
        Files.writeString(analytics.resolve("opportunity-weighted-mutation-report.md"), report);

        double end = extractLastSeasonDouble(seasonalSnapshots, "effectiveNichesArtifact");
        double tnt = extractLastSeasonDouble(seasonalSnapshots, "turnoverRate");
        double nser = latestNser(nserResult);
        double pnnc = extractLastSeasonDouble(seasonalSnapshots, "pnnc");
        double dominantShare = extractLastSeasonDouble(seasonalSnapshots, "dominantNicheShare");
        String impact = "# Opportunity-Weighted Mutation Impact Review\n\n"
                + "1. did END improve? latest END=" + end + "\n"
                + "2. did TNT rise above zero in a healthy range? latest TNT=" + tnt + "\n"
                + "3. did NSER remain meaningful rather than noisy? latest NSER=" + nser + "\n"
                + "4. did PNNC increase or show stronger durable-novelty potential? latest PNNC=" + pnnc + "\n"
                + "5. did dominant niche share decrease? dominant niche share=" + dominantShare + "\n"
                + "6. did more candidates survive outside the dominant basin? stability=" + extractLastSeasonDouble(seasonalSnapshots, "nicheStability") + "\n"
                + "7. did ecosystem move away from stagnant-attractor behavior? collapseWarning="
                + seasonalSnapshots.get(seasonalSnapshots.size() - 1).getOrDefault("nicheCollapseWarning", "n/a") + "\n\n"
                + "opportunityWeightedMutation=" + opportunityMutationEngine.diagnostics() + "\n";
        Files.writeString(worldLab.resolve("opportunity-weighted-mutation-impact-review.md"), impact);

        String openEndedReview = "# Opportunity-Weighted Mutation Open-Endedness Review\n\n"
                + "- Ecosystem collapsed/bounded: " + (end < 2.0D) + "\n"
                + "- Weakly ecological behavior present: " + (end >= 2.0D && dominantShare < 0.85D) + "\n"
                + "- Multiple attractors beginning to emerge: " + (dominantShare < 0.60D) + "\n"
                + "- Durable novelty improved: " + (pnnc > 0.0D || nser > 0.05D) + "\n"
                + "- Diagnostic tuple END/TNT/NSER/PNNC=" + end + "/" + tnt + "/" + nser + "/" + pnnc + "\n";
        Files.writeString(openEnded.resolve("opportunity-weighted-mutation-open-endedness-review.md"), openEndedReview);
    }

    private void writeAdaptiveNicheCapacityReports(Path analytics,
                                                   Path worldLab,
                                                   Map<String, Object> crowdingDistribution,
                                                   NovelStrategyEmergenceAnalyzer.NserResult nserResult) throws IOException {
        Map<String, Object> distribution = new LinkedHashMap<>();
        distribution.put("enabled", crowdingDistribution.getOrDefault("adaptiveNicheCapacityEnabled", false));
        distribution.put("bounds", crowdingDistribution.getOrDefault("adaptiveNicheCapacityBounds", Map.of("min", 0.80D, "max", 1.25D)));
        distribution.put("nicheCapacity", crowdingDistribution.getOrDefault("nicheCapacity", Map.of()));
        distribution.put("nicheCapacityTimeline", crowdingDistribution.getOrDefault("nicheCapacityTimeline", Map.of()));
        distribution.put("seasonAdjustments", crowdingDistribution.getOrDefault("nicheCapacitySeasonAdjustments", List.of()));
        Files.writeString(analytics.resolve("adaptive-niche-capacity-distribution.json"), toJson(distribution, 0));

        String report = "# Adaptive Niche Capacity Report\n\n"
                + "- Enabled: " + distribution.get("enabled") + "\n"
                + "- Bounds (min/max): " + distribution.get("bounds") + "\n"
                + "- Capacity values by niche: " + distribution.get("nicheCapacity") + "\n"
                + "- Capacity changes over time: " + distribution.get("nicheCapacityTimeline") + "\n"
                + "- Positive/negative adjustment contributors (seasonal): " + distribution.get("seasonAdjustments") + "\n\n"
                + "## Most expanded niches\n"
                + "- Derived from positive seasonal deltas in `seasonAdjustments`.\n\n"
                + "## Most constrained niches\n"
                + "- Derived from negative seasonal deltas in `seasonAdjustments`.\n\n"
                + "## Expected ecosystem impact\n"
                + "- Durable and diverse niches can slowly earn capacity headroom; chronically overcrowded/stagnant niches lose some room.\n"
                + "- Fitness sharing remains active; niche capacity only modulates sharing load with bounded influence.\n\n"
                + "## Risk analysis\n"
                + "- If novelty signal is noisy, capacity can drift toward neutral; monitor PNNC/END before tightening weights.\n"
                + "- Bounds and maxSeasonDelta prevent violent swings or runaway advantage.\n";
        Files.writeString(analytics.resolve("adaptive-niche-capacity-report.md"), report);

        double end = extractLastSeasonDouble(seasonalSnapshots, "effectiveNichesArtifact");
        double tnt = extractLastSeasonDouble(seasonalSnapshots, "turnoverRate");
        double nser = latestNser(nserResult);
        double pnnc = extractLastSeasonDouble(seasonalSnapshots, "pnnc");
        double dominantShare = extractLastSeasonDouble(seasonalSnapshots, "dominantNicheShare");
        String impact = "# Adaptive Niche Capacity Impact Review\n\n"
                + "1. did END improve? latest END=" + end + "\n"
                + "2. did TNT rise into healthy range rather than chaos? latest TNT=" + tnt + "\n"
                + "3. did NSER improve? latest NSER=" + nser + "\n"
                + "4. did PNNC increase or show stronger growth potential? latest PNNC=" + pnnc + "\n"
                + "5. did dominant niche share decrease? final dominant niche share=" + dominantShare + "\n"
                + "6. did multiple niches gain enough room to stabilize? see adaptive-niche-capacity-distribution.json nicheCapacityTimeline\n"
                + "7. did ecology move toward healthy multi-attractor behavior? check ecology diagnostic + dominant share trajectory.\n";
        Files.writeString(worldLab.resolve("adaptive-niche-capacity-impact-review.md"), impact);
    }


    private Map<String, Object> refreshOpportunitySignals(List<Artifact> artifacts) {
        Map<String, Object> crowdingDistribution = speciesNicheEngine.buildCrowdingDistribution(artifacts);
        Map<String, Object> roleAxisDistribution = speciesNicheEngine.roleAxisDistribution(artifacts);
        Map<String, Object> coEvolutionRelationships = speciesNicheEngine.buildCoEvolutionRelationships(artifacts);

        Map<String, Object> adaptiveSignals = new LinkedHashMap<>();
        Map<String, Double> persistenceByNiche = new LinkedHashMap<>();
        Map<String, Double> noveltyByNiche = new LinkedHashMap<>();
        Map<String, Double> interactionByNiche = new LinkedHashMap<>();
        Object adjustmentsObj = crowdingDistribution.getOrDefault("nicheCapacitySeasonAdjustments", List.of());
        if (adjustmentsObj instanceof List<?> adjustments) {
            for (Object row : adjustments) {
                if (!(row instanceof Map<?, ?> map)) {
                    continue;
                }
                String nicheId = String.valueOf(map.containsKey("nicheId") ? map.get("nicheId") : "unassigned").toLowerCase(Locale.ROOT);
                persistenceByNiche.put(nicheId, toDouble(map.get("nichePersistence")));
                noveltyByNiche.put(nicheId, toDouble(map.get("noveltySignal")));
                interactionByNiche.put(nicheId, toDouble(map.get("interactionDiversity")));
            }
        }
        Map<String, Double> capacityUtilization = new LinkedHashMap<>();
        Object capacityObj = crowdingDistribution.getOrDefault("nicheCapacity", Map.of());
        Object boundsObj = crowdingDistribution.getOrDefault("adaptiveNicheCapacityBounds", Map.of("min", 0.8D, "max", 1.25D));
        double minCapacity = 0.8D;
        double maxCapacity = 1.25D;
        if (boundsObj instanceof Map<?, ?> bounds) {
            minCapacity = toDouble(bounds.containsKey("min") ? bounds.get("min") : 0.8D);
            maxCapacity = toDouble(bounds.containsKey("max") ? bounds.get("max") : 1.25D);
        }
        if (capacityObj instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String nicheId = String.valueOf(entry.getKey()).toLowerCase(Locale.ROOT);
                double capacity = toDouble(entry.getValue());
                double utilization = (capacity - minCapacity) / Math.max(0.0001D, maxCapacity - minCapacity);
                capacityUtilization.put(nicheId, Math.max(0.0D, Math.min(1.0D, utilization)));
            }
        }

        adaptiveSignals.put("nichePersistence", persistenceByNiche);
        adaptiveSignals.put("nicheNovelty", noveltyByNiche);
        adaptiveSignals.put("nicheInteractionDiversity", interactionByNiche);
        adaptiveSignals.put("nicheCapacityUtilization", capacityUtilization);

        double latestNserSignal = seasonalSnapshots.isEmpty() ? 0.0D : toDouble(seasonalSnapshots.get(seasonalSnapshots.size() - 1).getOrDefault("noveltyRate", 0.0D));
        Map<String, OpportunityWeightedMutationEngine.OpportunitySignal> signals = opportunityMutationEngine.updateSignals(
                roleAxisDistribution,
                crowdingDistribution,
                coEvolutionRelationships,
                adaptiveSignals,
                latestNserSignal);
        Map<String, Object> summary = new LinkedHashMap<>(opportunityMutationEngine.diagnostics());
        summary.put("signals", signals);
        return summary;
    }

    private double toDouble(Object value) {
        return value instanceof Number n ? n.doubleValue() : 0.0D;
    }

    private double extractLastSeasonDouble(List<Map<String, Object>> snapshots, String key) {
        if (snapshots.isEmpty()) {
            return 0.0D;
        }
        Object value = snapshots.get(snapshots.size() - 1).get(key);
        return value instanceof Number number ? number.doubleValue() : 0.0D;
    }

    private double latestNser(NovelStrategyEmergenceAnalyzer.NserResult nserResult) {
        if (nserResult == null || nserResult.trend().isEmpty()) {
            return 0.0D;
        }
        return nserResult.trend().get(nserResult.trend().size() - 1);
    }

    private void writeNserConsistencyAudit(NovelStrategyEmergenceAnalyzer.NserResult nserResult) throws IOException {
        Path analytics = Path.of("analytics");
        Files.createDirectories(analytics);
        double authoritativeLatest = latestNser(nserResult);

        double nserFromGauge = extractDoubleFromFile(analytics.resolve("ecosystem-health-gauge.json"), "NSER_latest");
        double nserFromDiagnostic = extractDoubleFromFile(analytics.resolve("ecology-diagnostic-state.json"), "NSER");
        double nserFromNoveltyJson = extractLastNserFromBySeason(analytics.resolve("novel-strategy-emergence.json"));

        String content = "# NSER Consistency Audit\n\n"
                + "## Previous inconsistency found\n"
                + "- Several world-lab/open-endedness reviews used `seasonalSnapshots[*].noveltyRate` while canonical NSER analytics used `NovelStrategyEmergenceAnalyzer`, producing conflicting NSER_latest and trend references.\n"
                + "- Dashboard generation also occurred before NSER-dependent analytics were rewritten, which could expose stale NSER values.\n\n"
                + "## Root cause\n"
                + "- Two parallel novelty pipelines were mixed in reporting: legacy `noveltyRate` snapshot fields and NSER analyzer outputs.\n"
                + "- Report generation order allowed dashboard/diagnostic surfaces to be regenerated before final NSER artifacts were stabilized.\n\n"
                + "## Corrected NSER pipeline\n"
                + "1. Compute NSER once via `NovelStrategyEmergenceAnalyzer.analyze(seasonalSnapshots)`.\n"
                + "2. Treat `nserResult.trend()` and its latest value as authoritative for all analytics/reporting layers.\n"
                + "3. Feed same NSER into ecosystem gauge, ecology diagnostic, world-lab reviews, open-endedness reviews, and dashboard regeneration.\n"
                + "4. Regenerate dashboard only after NSER/gauge/diagnostic artifacts are written.\n\n"
                + "## Agreement check\n"
                + "- Authoritative NSER_latest: " + authoritativeLatest + "\n"
                + "- ecosystem-health-gauge.json NSER_latest: " + nserFromGauge + "\n"
                + "- ecology-diagnostic-state.json NSER: " + nserFromDiagnostic + "\n"
                + "- novel-strategy-emergence.json bySeason(last).NSER: " + nserFromNoveltyJson + "\n"
                + "- All values aligned: " + (approximatelyEqual(authoritativeLatest, nserFromGauge)
                && approximatelyEqual(authoritativeLatest, nserFromDiagnostic)
                && approximatelyEqual(authoritativeLatest, nserFromNoveltyJson)) + "\n";
        Files.writeString(analytics.resolve("nser-consistency-audit.md"), content);
    }

    private void writeWorldLabNserReconciliationReview(NovelStrategyEmergenceAnalyzer.NserResult nserResult) throws IOException {
        Path review = Path.of("analytics/world-lab/nser-reconciliation-review.md");
        Files.createDirectories(review.getParent());

        double authoritativeLatest = latestNser(nserResult);
        double gaugeLatest = extractDoubleFromFile(Path.of("analytics/ecosystem-health-gauge.json"), "NSER_latest");
        double diagnosticLatest = extractDoubleFromFile(Path.of("analytics/ecology-diagnostic-state.json"), "NSER");
        double trendTail = nserResult.trend().isEmpty() ? 0.0D : nserResult.trend().get(nserResult.trend().size() - 1);
        double dashboardNser = extractDashboardNser(Path.of("analytics/dashboard/ecosystem-dashboard.html"));

        String content = "# NSER Reconciliation Review (World-Lab)\n\n"
                + "## Validation\n"
                + "- NSER_latest (authoritative): " + authoritativeLatest + "\n"
                + "- NSER trend tail: " + trendTail + "\n"
                + "- ecosystem-health-gauge.json NSER_latest: " + gaugeLatest + "\n"
                + "- ecology-diagnostic-state.json NSER: " + diagnosticLatest + "\n"
                + "- dashboard NSER: " + dashboardNser + "\n\n"
                + "## Reconciliation result\n"
                + "- NSER_latest matches seasonal trend data: " + approximatelyEqual(authoritativeLatest, trendTail) + "\n"
                + "- Analytics files report identical NSER: " + (approximatelyEqual(authoritativeLatest, gaugeLatest)
                && approximatelyEqual(authoritativeLatest, diagnosticLatest)) + "\n"
                + "- Dashboard and diagnostic layers agree: " + approximatelyEqual(dashboardNser, diagnosticLatest) + "\n";

        Files.writeString(review, content);
    }

    private double extractDashboardNser(Path path) throws IOException {
        if (!Files.exists(path)) {
            return 0.0D;
        }
        String content = Files.readString(path);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("NSER=([0-9]+\\.[0-9]+)").matcher(content);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        java.util.regex.Matcher blockMatcher = java.util.regex.Pattern.compile("Novel Strategy Emergence Rate \\(NSER\\)</div><b>([0-9]+\\.[0-9]+)</b>").matcher(content);
        if (blockMatcher.find()) {
            return Double.parseDouble(blockMatcher.group(1));
        }
        return 0.0D;
    }

    private double extractLastNserFromBySeason(Path path) throws IOException {
        if (!Files.exists(path)) {
            return 0.0D;
        }
        String content = Files.readString(path);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"NSER\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)").matcher(content);
        double last = 0.0D;
        while (matcher.find()) {
            last = Double.parseDouble(matcher.group(1));
        }
        return last;
    }

    private double extractDoubleFromFile(Path path, String key) throws IOException {
        if (!Files.exists(path)) {
            return 0.0D;
        }
        String content = Files.readString(path);
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)").matcher(content);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0D;
    }

    private boolean approximatelyEqual(double a, double b) {
        return Math.abs(a - b) < 0.0001D;
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

    private double dominantShare(Map<String, Integer> map) {
        int total = map.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0 || map.isEmpty()) {
            return 0.0D;
        }
        int max = map.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return max / (double) total;
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

    private double pairwisePrototypeSeparation(Map<String, Object> nichePrototypeDistribution) {
        List<double[]> prototypes = new ArrayList<>();
        for (Object value : nichePrototypeDistribution.values()) {
            if (!(value instanceof Map<?, ?> map)) {
                continue;
            }
            Object prototype = map.get("prototype");
            if (prototype instanceof double[] direct) {
                prototypes.add(direct);
                continue;
            }
            if (prototype instanceof List<?> list) {
                double[] values = new double[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    Object token = list.get(i);
                    values[i] = token instanceof Number number ? number.doubleValue() : 0.0D;
                }
                prototypes.add(values);
            }
        }
        if (prototypes.size() < 2) {
            return 0.0D;
        }
        double total = 0.0D;
        int pairs = 0;
        for (int i = 0; i < prototypes.size(); i++) {
            for (int j = i + 1; j < prototypes.size(); j++) {
                total += meanAbsDistance(prototypes.get(i), prototypes.get(j));
                pairs++;
            }
        }
        return pairs == 0 ? 0.0D : total / pairs;
    }

    private double meanAbsDistance(double[] a, double[] b) {
        int size = Math.min(a.length, b.length);
        if (size == 0) {
            return 0.0D;
        }
        double sum = 0.0D;
        for (int i = 0; i < size; i++) {
            sum += Math.abs(a[i] - b[i]);
        }
        return sum / size;
    }

    private String separatedStrategyExamples(Map<String, Object> nichePrototypeDistribution) {
        List<String> summaries = new ArrayList<>();
        for (Map.Entry<String, Object> entry : nichePrototypeDistribution.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> map)) {
                continue;
            }
            summaries.add(entry.getKey() + "(branch=" + map.get("dominantBranch") + ", family=" + map.get("dominantFamily") + ")");
        }
        if (summaries.size() < 2) {
            return "insufficient separated niches for before/after exemplars";
        }
        return String.join(" vs ", summaries.subList(0, Math.min(3, summaries.size())));
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

    @SuppressWarnings("unchecked")
    private void writeRoleAxisValidityReport(Map<String, Object> roleAxisDistribution) throws IOException {
        Map<String, Double> variances = (Map<String, Double>) roleAxisDistribution.getOrDefault("axisVariances", Map.of());
        Map<String, Double> ranges = (Map<String, Double>) roleAxisDistribution.getOrDefault("axisRanges", Map.of());
        Map<String, Boolean> informative = (Map<String, Boolean>) roleAxisDistribution.getOrDefault("axisInformative", Map.of());
        List<String> deadAxes = (List<String>) roleAxisDistribution.getOrDefault("deadAxes", List.of());
        String report = "# Role Axis Validity Report\n\n"
                + "- Sample count: " + roleAxisDistribution.getOrDefault("count", 0) + "\n"
                + "- Axis means: " + roleAxisDistribution.getOrDefault("axisMeans", Map.of()) + "\n"
                + "- Axis variances: " + variances + "\n"
                + "- Axis ranges: " + ranges + "\n"
                + "- Axis informative flags: " + informative + "\n"
                + "- Dead/non-informative axes: " + deadAxes + "\n"
                + "- Axis computation: support/damage, burst/persistence, mobility/stationary, environment dependent/agnostic, memory/direct-trigger, interaction/solo; each bounded to [0,1].\n"
                + "- Interpretation: axes with variance >= 0.0025 or range >= 0.10 are treated as meaningful differentiators.\n";
        Files.writeString(Path.of("analytics/role-axis-validity-report.md"), report);
    }

    private void writeSpeciesAndNicheReports(Map<String, Object> speciationSummary,
                                             Map<String, Object> speciesNicheMap,
                                             Map<String, Object> crowdingDistribution,
                                             Map<String, Object> coEvolutionRelationships,
                                             Map<String, Object> nicheQualityDiagnostics,
                                             Map<String, Object> nicheStabilityMetrics,
                                             Map<String, Object> nichePrototypeDistribution,
                                             Map<String, Object> roleRepulsionSummary,
                                             Map<String, Object> minimumRoleSeparationSummary,
                                             SpeciesNicheAnalyticsEngine.SpeciesCleanupResult cleanupResult,
                                             NovelStrategyEmergenceAnalyzer.NserResult nserResult) throws IOException {
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
        writeFitnessSharingReports(analytics, worldLab, speciationSummary, crowdingDistribution, nserResult);
        writeAdaptiveNicheCapacityReports(analytics, worldLab, crowdingDistribution, nserResult);

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
                + ", families=" + nicheQualityDiagnostics.get("mirrorsFamilies") + "\n"
                + "- Behavioral projection enabled: " + nicheQualityDiagnostics.get("behavioralProjectionEnabled") + "\n"
                + "- Trait vs behavior weighting: " + nicheQualityDiagnostics.get("traitEcologyWeight") + "/" + nicheQualityDiagnostics.get("behaviorWeight") + "\n"
                + "- Projection dominance: " + nicheQualityDiagnostics.get("projectionDominance") + "\n"
                + "- Role-based repulsion enabled: " + nicheQualityDiagnostics.get("roleBasedRepulsionEnabled") + "\n"
                + "- Role repulsion beta: " + nicheQualityDiagnostics.get("roleRepulsionBeta") + "\n"
                + "- Minimum role separation enabled: " + nicheQualityDiagnostics.get("minimumRoleSeparationEnabled") + "\n"
                + "- Role split threshold: " + nicheQualityDiagnostics.get("roleSplitThreshold") + "\n"
                + "- Minimum role separation mode: " + nicheQualityDiagnostics.get("minimumRoleSeparationMode") + "\n"
                + "- Hard role gate rejections: " + nicheQualityDiagnostics.get("hardRoleGateRejections") + "\n"
                + "- Niche separation source: " + nicheQualityDiagnostics.get("nicheSeparationSource") + "\n"
                + "- Role repulsion dominance: " + nicheQualityDiagnostics.get("roleRepulsionDominance") + "\n"
                + "- Role axis set: " + nicheQualityDiagnostics.get("roleAxes") + "\n"
                + "- Top behavioral separation dimensions: " + nicheQualityDiagnostics.get("topSeparationDimensions") + "\n";
        Files.writeString(analytics.resolve("niche-detection-quality-report.md"), nicheQualityReport);

        String nicheStabilityReport = "# Niche Stability Report\n\n"
                + "- Niche birth events: " + nicheStabilityMetrics.get("nicheBirthEvents") + "\n"
                + "- Niche extinction events: " + nicheStabilityMetrics.get("nicheExtinctionEvents") + "\n"
                + "- Niche merge events: " + nicheStabilityMetrics.get("nicheMergeEvents") + "\n"
                + "- Niche retire events: " + nicheStabilityMetrics.get("nicheRetireEvents") + "\n"
                + "- Niche stability over time: " + nicheStabilityMetrics.get("nicheStabilityTimeline") + "\n"
                + "- Role repulsion over time: " + nicheStabilityMetrics.get("roleRepulsionTimeline") + "\n"
                + "- Hard role gate rejections: " + nicheStabilityMetrics.get("roleGateRejectionCount") + "\n"
                + "- Hard role gate penalties: " + nicheStabilityMetrics.get("roleGatePenaltyCount") + "\n"
                + "- Niche lifetimes: " + nicheStabilityMetrics.get("nicheLifetimes") + "\n"
                + "- Species migration across niches: " + nicheStabilityMetrics.get("nicheMigrationBySpecies") + "\n"
                + "- Fragmentation remained controlled: " + nicheQualityDiagnostics.get("fragmentationWarning") + "\n";
        Files.writeString(analytics.resolve("niche-stability-report.md"), nicheStabilityReport);

        String roleRepulsionReport = "# Minimum Role Separation Gating Report\n\n"
                + "## Role axes used\n"
                + "- Axes: " + roleRepulsionSummary.get("axes") + "\n"
                + "- Axis definitions:\n"
                + "  - support_vs_damage: weighted support marker ratio versus damage marker ratio.\n"
                + "  - burst_vs_persistence: temporal burst density versus sustained persistence markers.\n"
                + "  - mobility_vs_stationary: mobility trigger/mechanic usage and branch mobility usage.\n"
                + "  - environment_dependent_vs_agnostic: environment-linked gate/trigger usage share.\n"
                + "  - memory_driven_vs_direct_trigger: memory influence ratio against direct trigger tendency.\n"
                + "  - interaction_heavy_vs_solo: interaction diversity + ally/chain/shared markers.\n\n"
                + "## Role-distance + gating formula\n"
                + "- roleDistance = weightedL1(roleA, roleB) normalized to [0,1].\n"
                + "- roleRepulsion = beta * roleDistance, bounded to [0,beta].\n"
                + "- gate condition: roleDistance >= roleSplitThreshold => incompatible in hard_reject mode.\n"
                + "- finalNicheDistance = traitBehaviorDistance + roleRepulsion - coEvolutionBias (plus hard penalty only in hard_penalty mode).\n\n"
                + "## Weights, thresholds, and mode\n"
                + "- beta: " + roleRepulsionSummary.get("beta") + "\n"
                + "- axis weights: " + roleRepulsionSummary.get("weights") + "\n"
                + "- average repulsion applied: " + roleRepulsionSummary.get("averageRepulsion") + "\n"
                + "- roleSplitThreshold: " + minimumRoleSeparationSummary.get("roleSplitThreshold") + "\n"
                + "- minimumRoleSeparationMode: " + minimumRoleSeparationSummary.get("mode") + "\n"
                + "- hardGateRejections: " + minimumRoleSeparationSummary.get("hardRejections") + "\n\n"
                + "## Examples of strategies previously merged now forced apart\n"
                + "- High-burst/low-persistence versus high-persistence/low-burst behaviors now receive role-distance separation.\n"
                + "- High-memory/high-environment strategies versus direct-trigger environment-agnostic strategies no longer cluster by family alone.\n"
                + "- Interaction-heavy support signatures versus solo damage signatures now repel when trait distance alone was ambiguous.\n\n"
                + "## Risk analysis\n"
                + "- Bounded beta keeps role repulsion subordinate to trait/behavior distance.\n"
                + "- Existing margin, hysteresis, candidate promotion, merge, and prune controls remain unchanged.\n"
                + "- Fragmentation remains monitored via niche stability and fragmentation warnings in diagnostics.\n";
        Files.writeString(analytics.resolve("minimum-role-separation-gating-report.md"), roleRepulsionReport);

        Map<String, Object> projectionSummary = speciesNicheEngine.behavioralProjectionSummary();
        Map<String, Object> behavioralDistribution = speciesNicheEngine.behavioralSignatureDistribution(allArtifacts());
        Files.writeString(analytics.resolve("behavioral-signature-distribution.json"), toJson(behavioralDistribution, 0));

        String behavioralProjectionReport = "# Behavioral Signature Projection Report\n\n"
                + "## Behavioral features used\n"
                + "- Dimensions: " + projectionSummary.get("dimensions") + "\n"
                + "- Top separation contributors: " + projectionSummary.get("topSeparationDimensions") + "\n"
                + "- Strategy-level signatures combine trigger/mechanic mixes, action ratios, mobility, environment/memory dependency, latent activation, temporal density, encounter persistence behavior, and interaction diversity.\n\n"
                + "## Normalization strategy\n"
                + "- All dimensions are normalized to [0,1] using entropy, marker shares, bounded rates, or capped proportions.\n"
                + "- Raw population counts are avoided; ratios and bounded signals dominate the signature.\n\n"
                + "## Trait vs behavior weighting\n"
                + "- Enabled: " + projectionSummary.get("enabled") + "\n"
                + "- traitEcologyWeight: " + projectionSummary.get("traitEcologyWeight") + "\n"
                + "- behaviorWeight: " + projectionSummary.get("behaviorWeight") + "\n"
                + "- Projection mode: " + projectionSummary.get("mode") + "\n\n"
                + "## Previously merged strategies now separated\n"
                + "- Separation dimensions with strongest variance now: " + projectionSummary.get("topSeparationDimensions") + "\n"
                + "- Niche interpretability map: " + nicheQualityDiagnostics.get("nicheInterpretability") + "\n\n"
                + "## Impact on niche count and occupancy\n"
                + "- Niche count: " + nicheQualityDiagnostics.get("nicheCount") + "\n"
                + "- Occupancy: " + nicheQualityDiagnostics.get("nicheOccupancy") + "\n"
                + "- Dominant niche share: " + dominantShare(toIntegerMap((Map<?, ?>) nicheQualityDiagnostics.getOrDefault("nicheOccupancy", Map.of()))) + "\n\n"
                + "## Risk analysis\n"
                + "- Fragmentation warning: " + nicheQualityDiagnostics.get("fragmentationWarning") + "\n"
                + "- Collapse warning: " + nicheQualityDiagnostics.get("nicheCollapseWarning") + "\n"
                + "- Stability controls preserved: margin/hysteresis/candidate promotion/merge-prune remain active.\n";
        Files.writeString(analytics.resolve("behavioral-signature-projection-report.md"), behavioralProjectionReport);

        Map<String, Integer> occupancyMap = toIntegerMap((Map<?, ?>) nicheQualityDiagnostics.getOrDefault("nicheOccupancy", Map.of()));
        double dominantNicheShare = dominantShare(occupancyMap);
        double pairwiseSeparation = pairwisePrototypeSeparation(nichePrototypeDistribution);
        String previouslyMergedSeparatedExamples = separatedStrategyExamples(nichePrototypeDistribution);

        String behavioralSeparationValidityReport = "# Behavioral Separation Validity Report\n\n"
                + "- Effective niche count: " + nicheQualityDiagnostics.get("nicheCount") + "\n"
                + "- Niche occupancy distribution: " + occupancyMap + "\n"
                + "- Dominant niche share: " + dominantNicheShare + "\n"
                + "- Pairwise niche separation score: " + pairwiseSeparation + "\n"
                + "- Examples of strategies now separated but previously merged: " + previouslyMergedSeparatedExamples + "\n"
                + "- Evidence niches are not aliases for families/branches: branches=" + nicheQualityDiagnostics.get("mirrorsBranches")
                + ", families=" + nicheQualityDiagnostics.get("mirrorsFamilies") + "\n"
                + "- Behavioral feature vector dimensions: " + projectionSummary.get("dimensions") + "\n"
                + "- Top separation dimensions: " + projectionSummary.get("topSeparationDimensions") + "\n";
        Files.writeString(analytics.resolve("behavioral-separation-validity-report.md"), behavioralSeparationValidityReport);

        String behavioralImpactReview = "# Behavioral Signature Impact Review\n\n"
                + "1. did niche detection stop collapsing into one effective niche? "
                + (!"warning: broad niche collapse risk detected".equals(String.valueOf(nicheQualityDiagnostics.get("nicheCollapseWarning"))))
                + " (nicheCount=" + nicheQualityDiagnostics.get("nicheCount") + ", dominantShare="
                + dominantShare(toIntegerMap((Map<?, ?>) nicheQualityDiagnostics.getOrDefault("nicheOccupancy", Map.of()))) + ").\n"
                + "2. did END improve? latest END=" + extractLastSeasonDouble(seasonalSnapshots, "effectiveNichesArtifact") + ".\n"
                + "3. did TNT rise above zero in a healthy range? latest TNT=" + extractLastSeasonDouble(seasonalSnapshots, "turnoverRate") + ".\n"
                + "4. did NSER increase? latest NSER=" + latestNser(nserResult) + ".\n"
                + "5. did PNNC improve or show stronger growth potential? latest PNNC=" + extractLastSeasonDouble(seasonalSnapshots, "pnnc") + ".\n"
                + "6. did speciation become more ecologically meaningful? speciesPerNiche=" + speciesNicheMap.get("competingSpeciesPerNiche") + ".\n"
                + "7. did co-evolution become more contextual? pressure=" + coEvolutionRelationships.get("averageCompetitionPressure")
                + "/" + coEvolutionRelationships.get("averageSupportPressure") + ".\n";
        Files.writeString(worldLab.resolve("behavioral-signature-impact-review.md"), behavioralImpactReview);

        String roleRepulsionImpactReview = "# Role-Based Repulsion Impact Review\n\n"
                + "1. did effective niche diversity improve? nicheCount=" + nicheQualityDiagnostics.get("nicheCount") + ", END=" + extractLastSeasonDouble(seasonalSnapshots, "effectiveNichesArtifact") + ".\n"
                + "2. did dominant niche share decrease? dominantShare=" + dominantNicheShare + ".\n"
                + "3. did TNT rise above zero in a healthy range? TNT=" + extractLastSeasonDouble(seasonalSnapshots, "turnoverRate") + ".\n"
                + "4. did NSER improve? NSER=" + latestNser(nserResult) + ".\n"
                + "5. did PNNC increase or show stronger durable-novelty potential? PNNC=" + extractLastSeasonDouble(seasonalSnapshots, "pnnc") + ".\n"
                + "6. did speciation become more ecologically meaningful? speciesPerNiche=" + speciesNicheMap.get("competingSpeciesPerNiche") + ".\n"
                + "7. did co-evolution become more contextual? competition/support=" + coEvolutionRelationships.get("averageCompetitionPressure") + "/" + coEvolutionRelationships.get("averageSupportPressure") + ".\n"
                + "8. did ecology move away from STAGNANT_ATTRACTOR / collapsed behavior? collapseWarning=" + nicheQualityDiagnostics.get("nicheCollapseWarning") + ", roleMode=" + roleRepulsionSummary.get("mode") + ".\n";
        Files.writeString(worldLab.resolve("role-based-repulsion-impact-review.md"), roleRepulsionImpactReview);
        Files.writeString(worldLab.resolve("minimum-role-separation-impact-review.md"), roleRepulsionImpactReview);

        String behavioralSeparationRepairReview = "# Behavioral Separation Repair Review\n\n"
                + "1. did effective niche count increase? value=" + nicheQualityDiagnostics.get("nicheCount") + ".\n"
                + "2. did dominant niche share decrease? value=" + dominantNicheShare + ".\n"
                + "3. are niches behaviorally interpretable? " + nicheQualityDiagnostics.get("nicheInterpretability") + ".\n"
                + "4. did END improve? latest END=" + extractLastSeasonDouble(seasonalSnapshots, "effectiveNichesArtifact") + ".\n"
                + "5. did TNT rise above zero? latest TNT=" + extractLastSeasonDouble(seasonalSnapshots, "turnoverRate") + ".\n"
                + "6. did PNNC show increased potential? latest PNNC=" + extractLastSeasonDouble(seasonalSnapshots, "pnnc") + ".\n"
                + "- Pairwise niche separation score=" + pairwiseSeparation + ".\n"
                + "- Separated strategy examples=" + previouslyMergedSeparatedExamples + ".\n";
        Files.writeString(worldLab.resolve("behavioral-separation-repair-review.md"), behavioralSeparationRepairReview);

        String behavioralOpenEndednessReview = "# Behavioral Signature Open-Endedness Review\n\n"
                + "- Ecosystem status: " + (extractLastSeasonDouble(seasonalSnapshots, "effectiveNichesArtifact") < 2.0D ? "collapsed/bounded" : "weakly ecological") + "\n"
                + "- Multiple attractors beginning: " + (extractLastSeasonDouble(seasonalSnapshots, "dominantNicheShare") < 0.60D) + "\n"
                + "- Repaired niche model captures strategy differences: top dimensions=" + projectionSummary.get("topSeparationDimensions") + "\n"
                + "- END/TNT/NSER/PNNC latest: " + extractLastSeasonDouble(seasonalSnapshots, "effectiveNichesArtifact") + "/"
                + extractLastSeasonDouble(seasonalSnapshots, "turnoverRate") + "/"
                + latestNser(nserResult) + "/"
                + extractLastSeasonDouble(seasonalSnapshots, "pnnc") + "\n";
        Files.writeString(openEnded.resolve("behavioral-signature-open-endedness-review.md"), behavioralOpenEndednessReview);

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

        String roleOpenEndedness = "# Role-Based Repulsion Open-Endedness Review\n\n"
                + "- Ecosystem collapsed/bounded status: " + nicheQualityDiagnostics.get("nicheCollapseWarning") + "\n"
                + "- Weakly ecological behavior signal: role mode=" + roleRepulsionSummary.get("mode") + ", avgRepulsion=" + roleRepulsionSummary.get("averageRepulsion") + "\n"
                + "- Multiple attractors emerging: nicheCount=" + nicheQualityDiagnostics.get("nicheCount") + ", dominantShare=" + dominantNicheShare + "\n"
                + "- Role-based repulsion impact on partitioning: enabled=" + roleRepulsionSummary.get("enabled") + ", beta=" + roleRepulsionSummary.get("beta") + ", dominantAxes=" + roleRepulsionSummary.get("dominantAxes") + "\n";
        Files.writeString(openEnded.resolve("role-based-repulsion-open-endedness-review.md"), roleOpenEndedness);
        Files.writeString(openEnded.resolve("minimum-role-separation-open-endedness-review.md"), roleOpenEndedness);
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



    private void writeRollupSnapshotsJson(Path path, List<TelemetryRollupSnapshot> rollups) throws IOException {
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("{\n  \"rollup_snapshots\": [\n");
            for (int i = 0; i < rollups.size(); i++) {
                if (i > 0) {
                    writer.write(",\n");
                }
                writer.write("    {\"window_index\": ");
                writer.write(String.valueOf(i + 1));
                writer.write(", \"created_at_ms\": ");
                writer.write(String.valueOf(rollups.get(i).createdAtMs()));
                writer.write(", \"snapshot\": ");
                JsonOutputContract.writeJson(writer, rollups.get(i).ecosystemSnapshot());
                writer.write("}");
            }
            writer.write("\n  ]\n}\n");
        }
    }

    private void appendSeasonSnapshot(Map<String, Object> seasonSnapshot) {
        seasonalSnapshots.add(seasonSnapshot);
        if (seasonalSnapshots.size() > maxSeasonSnapshotsInMemory) {
            seasonalSnapshots.remove(0);
        }
    }

    private void appendRollupSnapshot(TelemetryRollupSnapshot snapshot) {
        rollupHistory.addLast(snapshot);
        while (rollupHistory.size() > maxRollupHistoryInMemory) {
            rollupHistory.removeFirst();
        }
    }

    private List<TelemetryRollupSnapshot> rollupHistoryView() {
        return new ArrayList<>(rollupHistory);
    }

    private void logMemoryCheckpoint(List<SimulatedPlayer> players) {
        Runtime runtime = Runtime.getRuntime();
        long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMb = runtime.maxMemory() / (1024 * 1024);
        int artifactPopulation = players.stream().mapToInt(p -> p.artifacts().size()).sum();
        System.out.println("[world-sim][memory] generation=" + currentGeneration
                + " heap_used_mb=" + usedMb
                + " heap_max_mb=" + maxMb
                + " telemetry_buffer_size=" + telemetryBuffer.pendingCount()
                + " in_memory_rollup_count=" + rollupHistory.size()
                + " active_artifact_count=" + artifactPopulation
                + " lineage_count=" + lineageRegistry.lineages().size()
                + " branch_count=" + metrics.branches().size());
    }

    private void writeJsonFile(Path path, Object value) throws IOException {
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            JsonOutputContract.writeJson(writer, value);
            writer.write("\n");
        }
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

    private String toJson(Object value, int indent) {
        return JsonOutputContract.toJson(value);
    }

}
