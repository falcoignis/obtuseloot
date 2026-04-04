package obtuseloot.bootstrap;

import obtuseloot.ObtuseLoot;
import obtuseloot.abilities.AbilityRegistry;
import obtuseloot.abilities.ItemAbilityManager;
import obtuseloot.abilities.SeededAbilityResolver;
import obtuseloot.analytics.EnvironmentalPressureReporter;
import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.awakening.AwakeningEngine;
import obtuseloot.combat.CombatContextManager;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.convergence.ConvergenceEngine;
import obtuseloot.drift.DriftEngine;
import obtuseloot.ecosystem.ArtifactEcosystemSelfBalancingEngine;
import obtuseloot.ecosystem.EcosystemMapRenderer;
import obtuseloot.ecosystem.ProductionSafetyConfig;
import obtuseloot.evolution.AdaptiveSupportAllocator;
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
import obtuseloot.persistence.PlayerStateStore;
import obtuseloot.reputation.ReputationManager;
import obtuseloot.telemetry.EcosystemTelemetryEmitter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.nio.file.Path;

public final class EngineBootstrap {
    private EngineBootstrap() {
    }

    public static void initialize(BootstrapContext context) {
        ObtuseLoot plugin = context.require(ObtuseLoot.class);
        PlayerStateStore playerStateStore = context.require(PlayerStateStore.class);
        FileConfiguration config = context.require(FileConfiguration.class);
        obtuseloot.evolution.params.EcosystemTuningProfile tuningProfile =
                context.require(obtuseloot.evolution.params.EcosystemTuningProfile.class);
        EcosystemTelemetryEmitter ecosystemTelemetryEmitter = context.require(EcosystemTelemetryEmitter.class);
        EvolutionParameterRegistry evolutionParameterRegistry = context.require(EvolutionParameterRegistry.class);
        PluginPathLayout paths = context.require(PluginPathLayout.class);

        CombatContextManager combatContextManager = new CombatContextManager();
        EvolutionEngine evolutionEngine = new EvolutionEngine(new ArchetypeResolver(), new HybridEvolutionResolver());
        ArtifactUsageTracker artifactUsageTracker = new ArtifactUsageTracker();
        artifactUsageTracker.setTelemetryEmitter(ecosystemTelemetryEmitter);

        ArtifactEcosystemSelfBalancingEngine ecosystemEngine = new ArtifactEcosystemSelfBalancingEngine();
        ecosystemEngine.configure(ProductionSafetyConfig.from(config), plugin.getLogger());
        ecosystemEngine.setEnvironmentalPressureReportPath(paths.environmentPressureReportPath());

        ExperienceEvolutionEngine experienceEvolutionEngine = new ExperienceEvolutionEngine(
                artifactUsageTracker,
                new ArtifactFitnessEvaluator(),
                ecosystemEngine.pressureEngine(),
                new AdaptiveSupportAllocator(),
                evolutionParameterRegistry);
        experienceEvolutionEngine.setTelemetryEmitter(ecosystemTelemetryEmitter);

        DriftEngine driftEngine = new DriftEngine();
        AwakeningEngine awakeningEngine = new AwakeningEngine();
        ConvergenceEngine convergenceEngine = new ConvergenceEngine();
        ArtifactMemoryEngine artifactMemoryEngine = new ArtifactMemoryEngine();
        EcosystemMapRenderer ecosystemMapRenderer = new EcosystemMapRenderer(plugin);

        LineageRegistry lineageRegistry = new LineageRegistry();
        lineageRegistry.setTelemetryEmitter(ecosystemTelemetryEmitter);
        lineageRegistry.setDriftWindowDurationTicks(tuningProfile.driftWindowDurationTicks());
        lineageRegistry.restoreSpeciesSnapshot(playerStateStore.loadSpeciesSnapshot());

        LineageInfluenceResolver lineageInfluenceResolver = new LineageInfluenceResolver();
        ItemAbilityManager itemAbilityManager = new ItemAbilityManager(
                new SeededAbilityResolver(new AbilityRegistry(), artifactMemoryEngine, ecosystemEngine,
                        lineageRegistry, lineageInfluenceResolver, experienceEvolutionEngine));
        itemAbilityManager.setTriggerSubscriptionIndexingEnabled(RuntimeSettings.get().triggerSubscriptionIndexing());

        LoreEngine loreEngine = new LoreEngine();
        EngineScheduler engineScheduler = new EngineScheduler(plugin,
                context.require(ArtifactManager.class),
                context.require(ReputationManager.class),
                combatContextManager);

        context.register(CombatContextManager.class, combatContextManager);
        context.register(EvolutionEngine.class, evolutionEngine);
        context.register(ArtifactUsageTracker.class, artifactUsageTracker);
        context.register(ArtifactEcosystemSelfBalancingEngine.class, ecosystemEngine);
        context.register(ExperienceEvolutionEngine.class, experienceEvolutionEngine);
        context.register(DriftEngine.class, driftEngine);
        context.register(AwakeningEngine.class, awakeningEngine);
        context.register(ConvergenceEngine.class, convergenceEngine);
        context.register(ArtifactMemoryEngine.class, artifactMemoryEngine);
        context.register(EcosystemMapRenderer.class, ecosystemMapRenderer);
        context.register(LineageRegistry.class, lineageRegistry);
        context.register(LineageInfluenceResolver.class, lineageInfluenceResolver);
        context.register(ItemAbilityManager.class, itemAbilityManager);
        context.register(LoreEngine.class, loreEngine);
        context.register(EngineScheduler.class, engineScheduler);
    }

    public static BukkitTask scheduleEnvironmentalPressureTask(ObtuseLoot plugin,
                                                               ExperienceEvolutionEngine experienceEvolutionEngine,
                                                               EnvironmentalPressureReporter environmentalPressureReporter,
                                                               Path reportPath) {
        return plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            try {
                experienceEvolutionEngine.pressureEngine().advanceSeason();
                environmentalPressureReporter.writeReport(reportPath, experienceEvolutionEngine.pressureEngine());
            } catch (Exception exception) {
                plugin.getLogger().warning("[Ecosystem] Failed periodic environment pressure update: " + exception.getMessage());
            }
        }, 24_000L, 24_000L);
    }

}
