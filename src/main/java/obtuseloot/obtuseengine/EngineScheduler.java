package obtuseloot.obtuseengine;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.combat.CombatContextManager;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.reputation.ReputationManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class EngineScheduler {
    private final Plugin plugin;
    private final ArtifactManager artifactManager;
    private final ReputationManager reputationManager;
    private final CombatContextManager combatContextManager;

    private BukkitTask autosaveTask;
    private BukkitTask decayTask;
    private BukkitTask combatCleanupTask;
    private BukkitTask instabilityCleanupTask;

    public EngineScheduler(Plugin plugin, ArtifactManager artifactManager, ReputationManager reputationManager,
                           CombatContextManager combatContextManager) {
        this.plugin = plugin;
        this.artifactManager = artifactManager;
        this.reputationManager = reputationManager;
        this.combatContextManager = combatContextManager;
    }

    public void startAll() {
        stopAll();
        startAutosaveTask();
        startDecayTask();
        startCombatCleanupTask();
        startInstabilityCleanupTask();
    }

    public void stopAll() {
        cancelTask(autosaveTask);
        cancelTask(decayTask);
        cancelTask(combatCleanupTask);
        cancelTask(instabilityCleanupTask);
        autosaveTask = null;
        decayTask = null;
        combatCleanupTask = null;
        instabilityCleanupTask = null;
    }

    public void startAutosaveTask() {
        if (autosaveTask != null && !autosaveTask.isCancelled()) return;
        long ticks = RuntimeSettings.get().autosaveIntervalSeconds() * 20L;
        autosaveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            artifactManager.saveAll();
            reputationManager.saveAll();
        }, ticks, ticks);
    }

    public void startDecayTask() {
        if (decayTask != null && !decayTask.isCancelled()) return;
        long ticks = RuntimeSettings.get().volatileDecayIntervalSeconds() * 20L;
        decayTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> reputationManager.getLoadedReputations().values().forEach(rep -> {
            rep.decayVolatileStats(RuntimeSettings.get().volatileDecayFactor());
            rep.applySoftFloor();
        }), ticks, ticks);
    }

    public void startCombatCleanupTask() {
        if (combatCleanupTask != null && !combatCleanupTask.isCancelled()) return;
        long ticks = RuntimeSettings.get().contextCleanupSeconds() * 20L;
        combatCleanupTask = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> combatContextManager.cleanupStaleContexts(RuntimeSettings.get().combatWindowMs() * 2), ticks, ticks);
    }

    public void startInstabilityCleanupTask() {
        if (instabilityCleanupTask != null && !instabilityCleanupTask.isCancelled()) return;
        instabilityCleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Artifact artifact : artifactManager.getLoadedArtifacts().values()) {
                if (artifact != null && artifact.isInstabilityExpired(now)) {
                    artifact.clearInstability();
                    artifact.addLoreHistory("Instability faded.");
                }
            }
        }, 100L, 100L);
    }
    private void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }
}
