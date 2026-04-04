package obtuseloot;

import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.artifacts.ArtifactItemStorage;
import obtuseloot.awakening.AwakeningEngine;
import obtuseloot.bootstrap.BootstrapContext;
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
import obtuseloot.evolution.ArtifactUsageTracker;
import obtuseloot.evolution.ExperienceEvolutionEngine;
import obtuseloot.evolution.EvolutionEngine;
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

        BootstrapContext context = new BootstrapContext();
        context.register(ObtuseLoot.class, this);
        context.register(org.bukkit.configuration.file.FileConfiguration.class, getConfig());
        context.register(java.util.logging.Logger.class, getLogger());
        context.register(PluginPathLayout.class, paths);
        context.register(obtuseloot.evolution.params.EcosystemTuningProfile.class, tuningProfile);
        context.register(EvolutionParameterRegistry.class, evolutionParameterRegistry);

        TelemetryBootstrap.initialize(context);
        if (!PersistenceBootstrap.initialize(context)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        EngineBootstrap.initialize(context);
        DashboardBootstrap.initialize(context);
        CommandBootstrap.register(context);
        assignBootstrappedComponents(context);
        writeInitialReports();

        environmentalPressureTask = EngineBootstrap.scheduleEnvironmentalPressureTask(this, experienceEvolutionEngine,
                environmentalPressureReporter, paths.environmentPressureReportPath());
        telemetryRollupTask = TelemetryBootstrap.scheduleFlushTask(this, ecosystemTelemetryEmitter,
                tuningProfile.telemetryFlushIntervalTicks());

        engine = new ObtuseEngine(this);
        engine.initialize();
        engineScheduler.startAll();
    }

    private void assignBootstrappedComponents(BootstrapContext context) {
        persistenceManager = context.require(PersistenceManager.class);
        playerStateStore = context.require(PlayerStateStore.class);
        artifactManager = context.require(ArtifactManager.class);
        artifactItemStorage = context.require(ArtifactItemStorage.class);
        reputationManager = context.require(ReputationManager.class);

        combatContextManager = context.require(CombatContextManager.class);
        evolutionEngine = context.require(EvolutionEngine.class);
        artifactUsageTracker = context.require(ArtifactUsageTracker.class);
        ecosystemEngine = context.require(ArtifactEcosystemSelfBalancingEngine.class);
        experienceEvolutionEngine = context.require(ExperienceEvolutionEngine.class);
        driftEngine = context.require(DriftEngine.class);
        awakeningEngine = context.require(AwakeningEngine.class);
        convergenceEngine = context.require(ConvergenceEngine.class);
        artifactMemoryEngine = context.require(ArtifactMemoryEngine.class);
        ecosystemMapRenderer = context.require(EcosystemMapRenderer.class);
        lineageRegistry = context.require(LineageRegistry.class);
        lineageInfluenceResolver = context.require(LineageInfluenceResolver.class);
        itemAbilityManager = context.require(obtuseloot.abilities.ItemAbilityManager.class);
        loreEngine = context.require(LoreEngine.class);
        engineScheduler = context.require(EngineScheduler.class);

        dashboardService = context.require(DashboardService.class);
        dashboardWebServer = context.require(DashboardWebServer.class);
        ecosystemTelemetryEmitter = context.require(EcosystemTelemetryEmitter.class);
        telemetryAggregationAnalytics = context.require(TelemetryAggregationAnalytics.class);
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
