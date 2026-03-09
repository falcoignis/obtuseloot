package obtuseloot;

import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.abilities.AbilityRegistry;
import obtuseloot.abilities.ItemAbilityManager;
import obtuseloot.abilities.SeededAbilityResolver;
import obtuseloot.awakening.AwakeningEngine;
import obtuseloot.combat.CombatContextManager;
import obtuseloot.commands.DashboardCommandExecutor;
import obtuseloot.commands.ObtuseLootCommand;
import obtuseloot.dashboard.DashboardService;
import obtuseloot.dashboard.DashboardWebServer;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.ecosystem.ArtifactEcosystemSelfBalancingEngine;
import obtuseloot.lineage.LineageInfluenceResolver;
import obtuseloot.lineage.LineageRegistry;
import obtuseloot.drift.DriftEngine;
import obtuseloot.evolution.ArchetypeResolver;
import obtuseloot.evolution.ArtifactFitnessEvaluator;
import obtuseloot.evolution.ArtifactUsageTracker;
import obtuseloot.evolution.ExperienceEvolutionEngine;
import obtuseloot.evolution.EvolutionEngine;
import obtuseloot.evolution.HybridEvolutionResolver;
import obtuseloot.lore.LoreEngine;
import obtuseloot.names.NamePoolManager;
import obtuseloot.obtuseengine.EngineScheduler;
import obtuseloot.obtuseengine.ObtuseEngine;
import obtuseloot.persistence.PersistenceConfig;
import obtuseloot.persistence.PersistenceManager;
import obtuseloot.persistence.PlayerStateStore;
import obtuseloot.memory.ArtifactMemoryEngine;
import obtuseloot.reputation.ReputationManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ObtuseLoot extends JavaPlugin {
    private static ObtuseLoot instance;
    private ObtuseEngine engine;

    private PersistenceManager persistenceManager;
    private PlayerStateStore playerStateStore;
    private ArtifactManager artifactManager;
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

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        RuntimeSettings.load(getConfig());
        NamePoolManager.initialize(this);

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
        reputationManager = new ReputationManager(playerStateStore);

        combatContextManager = new CombatContextManager();
        evolutionEngine = new EvolutionEngine(new ArchetypeResolver(), new HybridEvolutionResolver());
        artifactUsageTracker = new ArtifactUsageTracker();
        experienceEvolutionEngine = new ExperienceEvolutionEngine(artifactUsageTracker, new ArtifactFitnessEvaluator());
        driftEngine = new DriftEngine();
        awakeningEngine = new AwakeningEngine();
        artifactMemoryEngine = new ArtifactMemoryEngine();
        ecosystemEngine = new ArtifactEcosystemSelfBalancingEngine();
        lineageRegistry = new LineageRegistry();
        lineageInfluenceResolver = new LineageInfluenceResolver();
        itemAbilityManager = new ItemAbilityManager(new SeededAbilityResolver(new AbilityRegistry(), artifactMemoryEngine, ecosystemEngine, lineageRegistry, lineageInfluenceResolver, experienceEvolutionEngine));
        loreEngine = new LoreEngine();
        engineScheduler = new EngineScheduler(this, artifactManager, reputationManager, combatContextManager);

        dashboardService = new DashboardService(java.nio.file.Path.of("analytics"));
        int dashboardPort = getConfig().getInt("dashboard.port", 8085);
        dashboardWebServer = new DashboardWebServer(dashboardService.dashboardRoot(), dashboardPort);
        try {
            dashboardService.regenerateDashboard();
            dashboardWebServer.start();
            getLogger().info("[Dashboard] Serving ecosystem dashboard at http://localhost:" + dashboardPort + "/ecosystem-dashboard.html");
        } catch (Exception exception) {
            getLogger().warning("[Dashboard] Failed to start dashboard web server: " + exception.getMessage());
        }

        if (getCommand("obtuseloot") != null) {
            ObtuseLootCommand command = new ObtuseLootCommand(this);
            DashboardCommandExecutor dashboardCommandExecutor = new DashboardCommandExecutor(this, command, dashboardService, dashboardWebServer);
            getCommand("obtuseloot").setExecutor(dashboardCommandExecutor);
            getCommand("obtuseloot").setTabCompleter(dashboardCommandExecutor);
        }

        engine = new ObtuseEngine(this);
        engine.initialize();
        engineScheduler.startAll();
    }

    @Override
    public void onDisable() {
        if (engineScheduler != null) {
            engineScheduler.stopAll();
        }
        if (artifactManager != null) {
            artifactManager.saveAll();
        }
        if (reputationManager != null) {
            reputationManager.saveAll();
        }
        if (persistenceManager != null) {
            persistenceManager.close();
        }
        if (engine != null) {
            engine.shutdown();
        }
        if (dashboardWebServer != null) {
            dashboardWebServer.stop();
        }
    }

    public static ObtuseLoot get() { return instance; }
    public PlayerStateStore getPlayerStateStore() { return playerStateStore; }
    public PersistenceManager getPersistenceManager() { return persistenceManager; }
    public ArtifactManager getArtifactManager() { return artifactManager; }
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
    public DashboardService getDashboardService() { return dashboardService; }
}
