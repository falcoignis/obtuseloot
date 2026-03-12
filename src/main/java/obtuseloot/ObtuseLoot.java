package obtuseloot;

import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.artifacts.ArtifactItemStorage;
import obtuseloot.abilities.AbilityRegistry;
import obtuseloot.abilities.ItemAbilityManager;
import obtuseloot.abilities.SeededAbilityResolver;
import obtuseloot.awakening.AwakeningEngine;
import obtuseloot.combat.CombatContextManager;
import obtuseloot.commands.DashboardCommandExecutor;
import obtuseloot.analytics.EnvironmentalPressureReporter;
import obtuseloot.analytics.TriggerSubscriptionIndexReporter;
import obtuseloot.commands.ObtuseLootCommand;
import obtuseloot.dashboard.DashboardService;
import obtuseloot.dashboard.DashboardWebServer;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.ecosystem.ArtifactEcosystemSelfBalancingEngine;
import obtuseloot.ecosystem.EcosystemMapRenderer;
import obtuseloot.lineage.LineageInfluenceResolver;
import obtuseloot.lineage.LineageRegistry;
import obtuseloot.drift.DriftEngine;
import obtuseloot.evolution.ArchetypeResolver;
import obtuseloot.evolution.ArtifactFitnessEvaluator;
import obtuseloot.evolution.ArtifactUsageTracker;
import obtuseloot.evolution.ExperienceEvolutionEngine;
import obtuseloot.evolution.EvolutionEngine;
import obtuseloot.evolution.HybridEvolutionResolver;
import obtuseloot.evolution.params.EvolutionParameterRegistry;
import obtuseloot.lore.LoreEngine;
import obtuseloot.obtuseengine.EngineScheduler;
import obtuseloot.obtuseengine.ObtuseEngine;
import obtuseloot.persistence.PersistenceConfig;
import obtuseloot.persistence.PersistenceManager;
import obtuseloot.persistence.PlayerStateStore;
import obtuseloot.memory.ArtifactMemoryEngine;
import obtuseloot.reputation.ReputationManager;
import obtuseloot.telemetry.EcosystemHistoryArchive;
import obtuseloot.telemetry.EcosystemTelemetryEmitter;
import obtuseloot.telemetry.RollupStateHydrator;
import obtuseloot.telemetry.ScheduledEcosystemRollups;
import obtuseloot.telemetry.TelemetryAggregationAnalytics;
import obtuseloot.telemetry.TelemetryAggregationBuffer;
import obtuseloot.telemetry.TelemetryAggregationService;
import obtuseloot.telemetry.TelemetryRollupSnapshotStore;
import obtuseloot.telemetry.TelemetryFlushScheduler;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class ObtuseLoot extends JavaPlugin {
    private static ObtuseLoot instance;
    private ObtuseEngine engine;

    private PersistenceManager persistenceManager;
    private PlayerStateStore playerStateStore;
    private ArtifactManager artifactManager;
    private ArtifactItemStorage artifactItemStorage;
    private ReputationManager reputationManager;
    private CombatContextManager combatContextManager;
    private EvolutionEngine evolutionEngine;
    private DriftEngine driftEngine;
    private AwakeningEngine awakeningEngine;
    private LoreEngine loreEngine;
    private EngineScheduler engineScheduler;
    private ItemAbilityManager itemAbilityManager;
    private ArtifactMemoryEngine artifactMemoryEngine;
    private ArtifactEcosystemSelfBalancingEngine ecosystemEngine;
    private LineageRegistry lineageRegistry;
    private LineageInfluenceResolver lineageInfluenceResolver;
    private ArtifactUsageTracker artifactUsageTracker;
    private ExperienceEvolutionEngine experienceEvolutionEngine;
    private DashboardService dashboardService;
    private DashboardWebServer dashboardWebServer;
    private EcosystemMapRenderer ecosystemMapRenderer;
    private BukkitTask environmentalPressureTask;
    private BukkitTask telemetryRollupTask;
    private EcosystemTelemetryEmitter ecosystemTelemetryEmitter;
    private TelemetryAggregationAnalytics telemetryAggregationAnalytics;
    private EvolutionParameterRegistry evolutionParameterRegistry;
    private final EnvironmentalPressureReporter environmentalPressureReporter = new EnvironmentalPressureReporter();
    private final TriggerSubscriptionIndexReporter triggerSubscriptionIndexReporter = new TriggerSubscriptionIndexReporter();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        RuntimeSettings.load(getConfig());
        evolutionParameterRegistry = new EvolutionParameterRegistry();
        evolutionParameterRegistry.load(getConfig());
        var tuningProfile = evolutionParameterRegistry.profile();

        TelemetryAggregationBuffer telemetryBuffer = new TelemetryAggregationBuffer();
        EcosystemHistoryArchive telemetryArchive = new EcosystemHistoryArchive(java.nio.file.Path.of("analytics/telemetry/ecosystem-events.log"));
        ScheduledEcosystemRollups scheduledRollups = new ScheduledEcosystemRollups(telemetryBuffer, tuningProfile.telemetryRollupIntervalMs());
        TelemetryRollupSnapshotStore snapshotStore = new TelemetryRollupSnapshotStore(java.nio.file.Path.of("analytics/telemetry/rollup-snapshot.properties"));
        RollupStateHydrator hydrator = new RollupStateHydrator(snapshotStore, telemetryArchive, tuningProfile.telemetryRehydrateReplayWindowEvents());
        TelemetryAggregationService aggregationService = new TelemetryAggregationService(telemetryBuffer, telemetryArchive, scheduledRollups,
                tuningProfile.telemetryArchiveBatchSize(), snapshotStore, hydrator);
        aggregationService.initializeFromHistory();
        getLogger().info("[Telemetry] initialization mode=" + aggregationService.initialization().mode()
                + ", replayedEvents=" + aggregationService.initialization().replayedEvents()
                + ", durationMs=" + aggregationService.initialization().durationMs());
        ecosystemTelemetryEmitter = new EcosystemTelemetryEmitter(aggregationService);
        telemetryAggregationAnalytics = new TelemetryAggregationAnalytics(scheduledRollups);

        persistenceManager = new PersistenceManager(this, PersistenceConfig.from(getConfig(), getDataFolder()));
        try {
            persistenceManager.initialize();
        } catch (RuntimeException ex) {
            getLogger().severe("[Persistence] Startup aborted: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        playerStateStore = persistenceManager.stateStore();
        artifactManager = new ArtifactManager(playerStateStore);
        artifactItemStorage = new ArtifactItemStorage(this);
        reputationManager = new ReputationManager(playerStateStore);

        combatContextManager = new CombatContextManager();
        evolutionEngine = new EvolutionEngine(new ArchetypeResolver(), new HybridEvolutionResolver());
        artifactUsageTracker = new ArtifactUsageTracker();
        artifactUsageTracker.setTelemetryEmitter(ecosystemTelemetryEmitter);
        ecosystemEngine = new ArtifactEcosystemSelfBalancingEngine();
        experienceEvolutionEngine = new ExperienceEvolutionEngine(artifactUsageTracker, new ArtifactFitnessEvaluator(), ecosystemEngine.pressureEngine(), new obtuseloot.evolution.AdaptiveSupportAllocator(), evolutionParameterRegistry);
        experienceEvolutionEngine.setTelemetryEmitter(ecosystemTelemetryEmitter);
        driftEngine = new DriftEngine();
        awakeningEngine = new AwakeningEngine();
        artifactMemoryEngine = new ArtifactMemoryEngine();
        ecosystemMapRenderer = new EcosystemMapRenderer(this);
        lineageRegistry = new LineageRegistry();
        lineageRegistry.setTelemetryEmitter(ecosystemTelemetryEmitter);
        lineageRegistry.setDriftWindowDurationTicks(tuningProfile.driftWindowDurationTicks());
        lineageRegistry.restoreSpeciesSnapshot(playerStateStore.loadSpeciesSnapshot());
        lineageInfluenceResolver = new LineageInfluenceResolver();
        itemAbilityManager = new ItemAbilityManager(new SeededAbilityResolver(new AbilityRegistry(), artifactMemoryEngine, ecosystemEngine, lineageRegistry, lineageInfluenceResolver, experienceEvolutionEngine));
        itemAbilityManager.setTriggerSubscriptionIndexingEnabled(RuntimeSettings.get().triggerSubscriptionIndexing());
        loreEngine = new LoreEngine();
        engineScheduler = new EngineScheduler(this, artifactManager, reputationManager, combatContextManager);

        dashboardService = new DashboardService(java.nio.file.Path.of("analytics"));
        int dashboardPort = getConfig().getInt("dashboard.port", 8085);
        boolean dashboardWebEnabled = getConfig().getBoolean("dashboard.webServerEnabled", false);
        dashboardWebServer = new DashboardWebServer(dashboardService.dashboardRoot(), dashboardPort);
        try {
            dashboardService.regenerateDashboard();
            if (dashboardWebEnabled) {
                dashboardWebServer.start();
                getLogger().info("[Dashboard] Serving ecosystem dashboard at http://localhost:" + dashboardPort + "/ecosystem-dashboard.html");
            } else {
                getLogger().info("[Dashboard] Web server disabled; dashboard generated to local analytics/dashboard.");
            }
        } catch (Exception exception) {
            getLogger().warning("[Dashboard] Failed to initialize dashboard: " + exception.getMessage());
        }

        if (getCommand("obtuseloot") != null) {
            ObtuseLootCommand command = new ObtuseLootCommand(this);
            DashboardCommandExecutor dashboardCommandExecutor = new DashboardCommandExecutor(this, command, dashboardService, dashboardWebServer);
            getCommand("obtuseloot").setExecutor(dashboardCommandExecutor);
            getCommand("obtuseloot").setTabCompleter(dashboardCommandExecutor);
        }

        try {
            environmentalPressureReporter.writeReport(java.nio.file.Path.of("analytics/environment-pressure-report.md"),
                    experienceEvolutionEngine.pressureEngine());
        } catch (Exception exception) {
            getLogger().warning("[Ecosystem] Failed to write environment pressure report: " + exception.getMessage());
        }
        try {
            triggerSubscriptionIndexReporter.writeReport(java.nio.file.Path.of("analytics/performance/trigger-subscription-index-report.md"), itemAbilityManager);
        } catch (Exception exception) {
            getLogger().warning("[Runtime] Failed to write trigger subscription report: " + exception.getMessage());
        }


        environmentalPressureTask = getServer().getScheduler().runTaskTimer(this, () -> {
            try {
                experienceEvolutionEngine.pressureEngine().advanceSeason();
                environmentalPressureReporter.writeReport(java.nio.file.Path.of("analytics/environment-pressure-report.md"),
                        experienceEvolutionEngine.pressureEngine());
            } catch (Exception exception) {
                getLogger().warning("[Ecosystem] Failed periodic environment pressure update: " + exception.getMessage());
            }
        }, 24_000L, 24_000L);

        telemetryRollupTask = getServer().getScheduler().runTaskTimerAsynchronously(this,
                new TelemetryFlushScheduler(ecosystemTelemetryEmitter),
                tuningProfile.telemetryFlushIntervalTicks(),
                tuningProfile.telemetryFlushIntervalTicks());

        engine = new ObtuseEngine(this);
        engine.initialize();
        engineScheduler.startAll();
    }

    @Override
    public void onDisable() {
        if (engineScheduler != null) {
            engineScheduler.stopAll();
        }
        if (environmentalPressureTask != null) {
            environmentalPressureTask.cancel();
            environmentalPressureTask = null;
        }
        if (telemetryRollupTask != null) {
            telemetryRollupTask.cancel();
            telemetryRollupTask = null;
        }
        if (artifactManager != null) {
            artifactManager.saveAll();
        }
        if (reputationManager != null) {
            reputationManager.saveAll();
        }
        if (playerStateStore != null && lineageRegistry != null) {
            playerStateStore.saveSpeciesSnapshot(lineageRegistry.speciesSnapshot());
        }
        if (persistenceManager != null) {
            persistenceManager.close();
        }
        if (engine != null) {
            engine.shutdown();
        }
        if (ecosystemMapRenderer != null) {
            ecosystemMapRenderer.shutdown();
        }
        if (dashboardWebServer != null) {
            dashboardWebServer.stop();
        }
        try {
            triggerSubscriptionIndexReporter.writeReport(java.nio.file.Path.of("analytics/performance/trigger-subscription-index-report.md"), itemAbilityManager);
        } catch (Exception exception) {
            getLogger().warning("[Runtime] Failed to write trigger subscription report on disable: " + exception.getMessage());
        }
        if (ecosystemTelemetryEmitter != null) {
            ecosystemTelemetryEmitter.flushAll();
        }
    }

    public static ObtuseLoot get() { return instance; }
    public PlayerStateStore getPlayerStateStore() { return playerStateStore; }
    public PersistenceManager getPersistenceManager() { return persistenceManager; }
    public ArtifactManager getArtifactManager() { return artifactManager; }
    public ArtifactItemStorage getArtifactItemStorage() { return artifactItemStorage; }
    public ReputationManager getReputationManager() { return reputationManager; }
    public CombatContextManager getCombatContextManager() { return combatContextManager; }
    public EvolutionEngine getEvolutionEngine() { return evolutionEngine; }
    public DriftEngine getDriftEngine() { return driftEngine; }
    public AwakeningEngine getAwakeningEngine() { return awakeningEngine; }
    public LoreEngine getLoreEngine() { return loreEngine; }
    public EngineScheduler getEngineScheduler() { return engineScheduler; }
    public ItemAbilityManager getItemAbilityManager() { return itemAbilityManager; }
    public ArtifactMemoryEngine getArtifactMemoryEngine() { return artifactMemoryEngine; }
    public ArtifactEcosystemSelfBalancingEngine getEcosystemEngine() { return ecosystemEngine; }
    public LineageRegistry getLineageRegistry() { return lineageRegistry; }
    public ArtifactUsageTracker getArtifactUsageTracker() { return artifactUsageTracker; }
    public ExperienceEvolutionEngine getExperienceEvolutionEngine() { return experienceEvolutionEngine; }
    public DashboardService getDashboardService() { return dashboardService; }
    public EcosystemMapRenderer getEcosystemMapRenderer() { return ecosystemMapRenderer; }
    public EcosystemTelemetryEmitter getEcosystemTelemetryEmitter() { return ecosystemTelemetryEmitter; }
    public TelemetryAggregationAnalytics getTelemetryAggregationAnalytics() { return telemetryAggregationAnalytics; }
    public EvolutionParameterRegistry getEvolutionParameterRegistry() { return evolutionParameterRegistry; }
}
