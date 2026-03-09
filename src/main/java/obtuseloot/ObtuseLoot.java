package obtuseloot;

import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.awakening.AwakeningEngine;
import obtuseloot.combat.CombatContextManager;
import obtuseloot.commands.ObtuseLootCommand;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.drift.DriftEngine;
import obtuseloot.evolution.ArchetypeResolver;
import obtuseloot.evolution.EvolutionEngine;
import obtuseloot.evolution.HybridEvolutionResolver;
import obtuseloot.lore.LoreEngine;
import obtuseloot.names.NamePoolManager;
import obtuseloot.obtuseengine.EngineScheduler;
import obtuseloot.obtuseengine.ObtuseEngine;
import obtuseloot.persistence.PlayerStateStore;
import obtuseloot.persistence.YamlPlayerStateStore;
import obtuseloot.reputation.ReputationManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ObtuseLoot extends JavaPlugin {
    private static ObtuseLoot instance;
    private ObtuseEngine engine;

    private ArtifactManager artifactManager;
    private ReputationManager reputationManager;
    private CombatContextManager combatContextManager;
    private EvolutionEngine evolutionEngine;
    private DriftEngine driftEngine;
    private AwakeningEngine awakeningEngine;
    private LoreEngine loreEngine;
    private EngineScheduler engineScheduler;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        RuntimeSettings.load(getConfig());
        NamePoolManager.initialize(this);

        PlayerStateStore stateStore = new YamlPlayerStateStore(this);
        artifactManager = new ArtifactManager(stateStore);
        reputationManager = new ReputationManager(stateStore);
        ArtifactManager.initialize(artifactManager);
        ReputationManager.initialize(reputationManager);

        combatContextManager = new CombatContextManager();
        evolutionEngine = new EvolutionEngine(new ArchetypeResolver(), new HybridEvolutionResolver());
        driftEngine = new DriftEngine();
        awakeningEngine = new AwakeningEngine();
        loreEngine = new LoreEngine();
        engineScheduler = new EngineScheduler(this, artifactManager, reputationManager, combatContextManager);

        if (getCommand("obtuseloot") != null) {
            ObtuseLootCommand command = new ObtuseLootCommand(this);
            getCommand("obtuseloot").setExecutor(command);
            getCommand("obtuseloot").setTabCompleter(command);
        }

        engine = new ObtuseEngine(this);
        engine.initialize();
        engineScheduler.startAll();
    }

    @Override
    public void onDisable() {
        if (engineScheduler != null) engineScheduler.stopAll();
        if (artifactManager != null) artifactManager.saveAll();
        if (reputationManager != null) reputationManager.saveAll();
        if (engine != null) engine.shutdown();
    }

    public static ObtuseLoot get() { return instance; }
    public ArtifactManager getArtifactManager() { return artifactManager; }
    public ReputationManager getReputationManager() { return reputationManager; }
    public CombatContextManager getCombatContextManager() { return combatContextManager; }
    public EvolutionEngine getEvolutionEngine() { return evolutionEngine; }
    public DriftEngine getDriftEngine() { return driftEngine; }
    public AwakeningEngine getAwakeningEngine() { return awakeningEngine; }
    public LoreEngine getLoreEngine() { return loreEngine; }
}
