package obtuseloot.bootstrap;

import obtuseloot.ObtuseLoot;
import obtuseloot.abilities.AbilityRegistry;
import obtuseloot.abilities.ItemAbilityManager;
import obtuseloot.abilities.SeededAbilityResolver;
import obtuseloot.analytics.EnvironmentalPressureReporter;
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
import obtuseloot.telemetry.EcosystemTelemetryEmitter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.nio.file.Path;

public final class EngineBootstrap {
    private EngineBootstrap() {
    }

    public static Result initialize(ObtuseLoot plugin,
                                    PlayerStateStore playerStateStore,
                                    FileConfiguration config,
                                    obtuseloot.evolution.params.EcosystemTuningProfile tuningProfile,
                                    EcosystemTelemetryEmitter ecosystemTelemetryEmitter,
                                    EvolutionParameterRegistry evolutionParameterRegistry,
                                    PluginPathLayout paths) {
        CombatContextManager combatContextManager = new CombatContextManager();
        EvolutionEngine evolutionEngine = new EvolutionEngine(new ArchetypeResolver(), new HybridEvolutionResolver());
        ArtifactUsageTracker artifactUsageTracker = new ArtifactUsageTracker();
        artifactUsageTracker.setTelemetryEmitter(ecosystemTelemetryEmitter);

        ArtifactEcosystemSelfBalancingEngine ecosystemEngine = new ArtifactEcosystemSelfBalancingEngine();
        ecosystemEngine.configure(ProductionSafetyConfig.from(config), plugin.getLogger());
        ecosystemEngine.setEnvironmentalPressureReportPath(paths.environmentPressureReport());

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
                plugin.getArtifactManager(),
                plugin.getReputationManager(),
                combatContextManager);

        return new Result(combatContextManager, evolutionEngine, artifactUsageTracker, ecosystemEngine,
                experienceEvolutionEngine, driftEngine, awakeningEngine, convergenceEngine,
                artifactMemoryEngine, ecosystemMapRenderer, lineageRegistry, lineageInfluenceResolver,
                itemAbilityManager, loreEngine, engineScheduler);
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

    public record Result(CombatContextManager combatContextManager,
                         EvolutionEngine evolutionEngine,
                         ArtifactUsageTracker artifactUsageTracker,
                         ArtifactEcosystemSelfBalancingEngine ecosystemEngine,
                         ExperienceEvolutionEngine experienceEvolutionEngine,
                         DriftEngine driftEngine,
                         AwakeningEngine awakeningEngine,
                         ConvergenceEngine convergenceEngine,
                         ArtifactMemoryEngine artifactMemoryEngine,
                         EcosystemMapRenderer ecosystemMapRenderer,
                         LineageRegistry lineageRegistry,
                         LineageInfluenceResolver lineageInfluenceResolver,
                         ItemAbilityManager itemAbilityManager,
                         LoreEngine loreEngine,
                         EngineScheduler engineScheduler) {
    }
}
