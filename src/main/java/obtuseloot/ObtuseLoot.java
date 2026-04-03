package obtuseloot;

import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.artifacts.ArtifactItemStorage;
import obtuseloot.awakening.AwakeningEngine;
import obtuseloot.bootstrap.CommandBootstrap;
import obtuseloot.bootstrap.DashboardBootstrap;
import obtuseloot.bootstrap.EngineBootstrap;
import obtuseloot.bootstrap.PersistenceBootstrap;
import obtuseloot.bootstrap.PluginPathLayout;
import obtuseloot.bootstrap.TelemetryBootstrap;
import obtuseloot.combat.CombatContextManager;
import obtuseloot.analytics.EnvironmentalPressureReporter;
import obtuseloot.analytics.TriggerSubscriptionIndexReporter;
import obtuseloot.dashboard.DashboardService;
import obtuseloot.dashboard.DashboardWebServer;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.convergence.ConvergenceEngine;
import obtuseloot.ecosystem.ArtifactEcosystemSelfBalancingEngine;
import obtuseloot.ecosystem.EcosystemMapRenderer;
import obtuseloot.drift.DriftEngine;
import obtuseloot.evolution.ArchetypeResolver;
import obtuseloot.evolution.ArtifactFitnessEvaluator;
import obtuseloot.evolution.ArtifactUsageTracker;
import obtuseloot.evolution.ExperienceEvolutionEngine;
import obtuseloot.evolution.EvolutionEngine;
import obtuseloot.evolution.HybridEvolutionResolver;
import obtuseloot.evolution.params.EvolutionParameterRegistry;
import obtuseloot.lineage.LineageInfluenceResolver;
import obtuseloot.lineage.LineageRegistry;
import obtuseloot.lore.LoreEngine;
import obtuseloot.memory.ArtifactMemoryEngine;
import obtuseloot.obtuseengine.EngineScheduler;
import obtuseloot.obtuseengine.ObtuseEngine;
import obtuseloot.persistence.PersistenceManager;
import obtuseloot.persistence.PlayerStateStore;
import obtuseloot.reputation.ReputationManager;
import obtuseloot.telemetry.EcosystemTelemetryEmitter;
import obtuseloot.telemetry.TelemetryAggregationAnalytics;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class ObtuseLoot extends JavaPlugin {
    private static ObtuseLoot instance;
    private ObtuseEngine engine;

    private PluginPathLayout paths;

    private PersistenceManager persistenceManager;
    private PlayerStateStore playerStateStore;
    private ArtifactManager artifactManager;
    private ArtifactItemStorage artifactItemStorage;
    private ReputationManager reputationManager;
    private CombatContextManager combatContextManager;
    private EvolutionEngine evolutionEngine;
    private DriftEngine driftEngine;
    private AwakeningEngine awakeningEngine;
    private ConvergenceEngine convergenceEngine;
    private LoreEngine loreEngine;
    private EngineScheduler engineScheduler;
    private obtuseloot.abilities.ItemAbilityManager itemAbilityManager;
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
        paths = PluginPathLayout.from(this);

        evolutionParameterRegistry = new EvolutionParameterRegistry();
        evolutionParameterRegistry.load(getConfig());
        var tuningProfile = evolutionParameterRegistry.profile();

        TelemetryBootstrap.Result telemetry = TelemetryBootstrap.initialize(getLogger(), tuningProfile, paths);
        ecosystemTelemetryEmitter = telemetry.emitter();
        telemetryAggregationAnalytics = telemetry.analytics();

        PersistenceBootstrap.Result persistence = PersistenceBootstrap.initialize(this, getConfig());
        if (persistence == null) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        persistenceManager = persistence.persistenceManager();
        playerStateStore = persistence.playerStateStore();
        artifactManager = persistence.artifactManager();
        artifactItemStorage = persistence.artifactItemStorage();
        reputationManager = persistence.reputationManager();

        EngineBootstrap.Result engineComponents = EngineBootstrap.initialize(this, playerStateStore, getConfig(),
                tuningProfile, ecosystemTelemetryEmitter, evolutionParameterRegistry, paths);
        combatContextManager = engineComponents.combatContextManager();
        evolutionEngine = engineComponents.evolutionEngine();
        artifactUsageTracker = engineComponents.artifactUsageTracker();
        ecosystemEngine = engineComponents.ecosystemEngine();
        experienceEvolutionEngine = engineComponents.experienceEvolutionEngine();
        driftEngine = engineComponents.driftEngine();
        awakeningEngine = engineComponents.awakeningEngine();
        convergenceEngine = engineComponents.convergenceEngine();
        artifactMemoryEngine = engineComponents.artifactMemoryEngine();
        ecosystemMapRenderer = engineComponents.ecosystemMapRenderer();
        lineageRegistry = engineComponents.lineageRegistry();
        lineageInfluenceResolver = engineComponents.lineageInfluenceResolver();
        itemAbilityManager = engineComponents.itemAbilityManager();
        loreEngine = engineComponents.loreEngine();
        engineScheduler = engineComponents.engineScheduler();

        DashboardBootstrap.Result dashboard = DashboardBootstrap.initialize(this, paths);
        dashboardService = dashboard.dashboardService();
        dashboardWebServer = dashboard.dashboardWebServer();

        CommandBootstrap.register(this, dashboardService, dashboardWebServer, paths);
        writeInitialReports();

        environmentalPressureTask = EngineBootstrap.scheduleEnvironmentalPressureTask(this, experienceEvolutionEngine,
                environmentalPressureReporter, paths.environmentPressureReportPath());
        telemetryRollupTask = TelemetryBootstrap.scheduleFlushTask(this, ecosystemTelemetryEmitter,
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
            triggerSubscriptionIndexReporter.writeReport(paths.triggerSubscriptionReportPath(), itemAbilityManager);
        } catch (Exception exception) {
            getLogger().warning("[Runtime] Failed to write trigger subscription report on disable: " + exception.getMessage());
        }
        if (ecosystemTelemetryEmitter != null) {
            ecosystemTelemetryEmitter.flushAll();
        }
    }

    private void writeInitialReports() {
        try {
            environmentalPressureReporter.writeReport(paths.environmentPressureReportPath(),
                    experienceEvolutionEngine.pressureEngine());
        } catch (Exception exception) {
            getLogger().warning("[Ecosystem] Failed to write environment pressure report: " + exception.getMessage());
        }
        try {
            triggerSubscriptionIndexReporter.writeReport(paths.triggerSubscriptionReportPath(), itemAbilityManager);
        } catch (Exception exception) {
            getLogger().warning("[Runtime] Failed to write trigger subscription report: " + exception.getMessage());
        }
    }

    public static ObtuseLoot get() { return instance; }
    public obtuseloot.ecosystem.EcosystemHealthMonitor getEcosystemHealthMonitor() { return ecosystemEngine != null ? ecosystemEngine.healthMonitor() : null; }
    public PlayerStateStore getPlayerStateStore() { return playerStateStore; }
    public PersistenceManager getPersistenceManager() { return persistenceManager; }
    public ArtifactManager getArtifactManager() { return artifactManager; }
    public ArtifactItemStorage getArtifactItemStorage() { return artifactItemStorage; }
    public ReputationManager getReputationManager() { return reputationManager; }
    public CombatContextManager getCombatContextManager() { return combatContextManager; }
    public EvolutionEngine getEvolutionEngine() { return evolutionEngine; }
    public DriftEngine getDriftEngine() { return driftEngine; }
    public AwakeningEngine getAwakeningEngine() { return awakeningEngine; }
    public ConvergenceEngine getConvergenceEngine() { return convergenceEngine; }
    public LoreEngine getLoreEngine() { return loreEngine; }
    public EngineScheduler getEngineScheduler() { return engineScheduler; }
    public obtuseloot.abilities.ItemAbilityManager getItemAbilityManager() { return itemAbilityManager; }
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
    public PluginPathLayout getPaths() { return paths; }
}
