package obtuseloot.obtuseengine;

import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.ArtifactManager;
import obtuseloot.combat.CombatContextManager;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.reputation.ReputationManager;
import obtuseloot.abilities.ItemAbilityManager;
import obtuseloot.evolution.EvolutionEngine;
import obtuseloot.evolution.NonCombatEvolutionQueue;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class EngineScheduler {
    private final Plugin plugin;
    private final ArtifactManager artifactManager;
    private final ReputationManager reputationManager;
    private final CombatContextManager combatContextManager;
    private final obtuseloot.ecology.LocalEcosystem localEcosystem;
    private final EvolutionEngine evolutionEngine;
    private final ItemAbilityManager itemAbilityManager;
    private final NonCombatEvolutionQueue evolutionQueue = new NonCombatEvolutionQueue();

    private BukkitTask autosaveTask;
    private BukkitTask decayTask;
    private BukkitTask combatCleanupTask;
    private BukkitTask instabilityCleanupTask;
    private BukkitTask evolutionTask;

    public EngineScheduler(Plugin plugin, ArtifactManager artifactManager, ReputationManager reputationManager,
                           CombatContextManager combatContextManager, EvolutionEngine evolutionEngine,
                           ItemAbilityManager itemAbilityManager) {
        this(plugin, artifactManager, reputationManager, combatContextManager, evolutionEngine, itemAbilityManager, null);
    }

    public EngineScheduler(Plugin plugin, ArtifactManager artifactManager, ReputationManager reputationManager,
                           CombatContextManager combatContextManager, EvolutionEngine evolutionEngine,
                           ItemAbilityManager itemAbilityManager, obtuseloot.ecology.LocalEcosystem localEcosystem) {
        this.localEcosystem = localEcosystem;
        this.plugin = plugin;
        this.artifactManager = artifactManager;
        this.reputationManager = reputationManager;
        this.combatContextManager = combatContextManager;
        this.evolutionEngine = evolutionEngine;
        this.itemAbilityManager = itemAbilityManager;
        itemAbilityManager.setOutcomeObserver(observation -> {
            evolutionQueue.observe(observation);
            if (localEcosystem != null) localEcosystem.observe(observation);
        });
    }

    public void startAll() {
        stopAll();
        startAutosaveTask();
        startDecayTask();
        startCombatCleanupTask();
        startInstabilityCleanupTask();
        startEvolutionTask();
    }

    public void stopAll() {
        cancelTask(autosaveTask);
        cancelTask(decayTask);
        cancelTask(combatCleanupTask);
        cancelTask(instabilityCleanupTask);
        cancelTask(evolutionTask);
        evolutionQueue.flushAll(update -> applyEvolution(update, false), this::logEvolutionFailure);
        autosaveTask = null;
        decayTask = null;
        combatCleanupTask = null;
        instabilityCleanupTask = null;
        evolutionTask = null;
    }

    public void startAutosaveTask() {
        if (autosaveTask != null && !autosaveTask.isCancelled()) return;
        long ticks = RuntimeSettings.get().autosaveIntervalSeconds() * 20L;
        autosaveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            artifactManager.saveAll();
            reputationManager.saveAll();
            if (localEcosystem != null) {
                try { localEcosystem.save(); }
                catch (RuntimeException exception) {
                    plugin.getLogger().log(Level.SEVERE, "Failed saving local ecology; retaining state for the next autosave", exception);
                }
            }
        }, ticks, ticks);
    }

    public void startDecayTask() {
        if (decayTask != null && !decayTask.isCancelled()) return;
        long ticks = RuntimeSettings.get().volatileDecayIntervalSeconds() * 20L;
        decayTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            var active = new java.util.HashSet<>(reputationManager.getLoadedReputations().values());
            artifactManager.equippedProfiles().forEach(a -> active.add(a.getItemReputation()));
            active.forEach(rep -> { rep.decayVolatileStats(RuntimeSettings.get().volatileDecayFactor()); rep.applySoftFloor(); });
        }, ticks, ticks);
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
            var active = new java.util.HashSet<>(artifactManager.getLoadedArtifacts().values());
            active.addAll(artifactManager.equippedProfiles());
            for (Artifact artifact : active) {
                if (artifact != null && artifact.isInstabilityExpired(now)) {
                    artifact.clearInstability();
                    artifact.addLoreHistory("Instability faded.");
                    artifactManager.markDirty(artifact);
                }
            }
        }, 100L, 100L);
    }

    public void startEvolutionTask() {
        if (evolutionTask != null && !evolutionTask.isCancelled()) return;
        evolutionTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (localEcosystem != null) localEcosystem.advanceTick();
            if (plugin instanceof obtuseloot.ObtuseLoot main && main.getEquipmentAbilities() != null)
                main.getEquipmentAbilities().tick();
            if (plugin instanceof obtuseloot.ObtuseLoot main && main.getAirJumps() != null) main.getAirJumps().tick();
            evolutionQueue.tick(RuntimeSettings.get().noncombatUpdateIntervalTicks(),
                    update -> applyEvolution(update, true), this::logEvolutionFailure);
        }, 1L, 1L);
    }

    /** Called before unload/save. No extra selection roll or UI update on quit. */
    public void flushEvolution(UUID owner) {
        evolutionQueue.flush(owner, update -> applyEvolution(update, false), this::logEvolutionFailure);
        evolutionQueue.forget(owner);
    }
    public void flushEvolution(Artifact artifact) {
        evolutionQueue.flush(artifact, update -> applyEvolution(update, false), this::logEvolutionFailure);
    }

    private void applyEvolution(NonCombatEvolutionQueue.Pending update, boolean refreshAbilities) {
        Artifact current = artifactManager.getLoadedArtifacts().get(update.owner());
        var reputation = reputationManager.getLoadedReputations().get(update.owner());
        if (obtuseloot.artifacts.ArtifactStorageKeys.isItem(update.artifact().getArtifactStorageKey())) {
            current = artifactManager.resolveByStorageKey(update.artifact().getArtifactStorageKey());
            reputation = current == null ? null : current.getItemReputation();
        }
        if (!update.matches(current, reputation)) return; // stale, reset, rerolled or refused state
        artifactManager.markDirty(current);
        String oldArchetype = current.getArchetypePath();
        String oldEvolution = current.getEvolutionPath();
        String oldAwakening = current.getAwakeningPath();
        String oldConvergence = current.getConvergencePath();
        int oldDrift = current.getDriftLevel();
        evolutionEngine.evaluate(current, reputation);
        ArtifactProcessor.recordStateTransitions(current, oldArchetype, oldEvolution, oldAwakening, oldConvergence, oldDrift);
        if (refreshAbilities) {
            var player = Bukkit.getPlayer(update.owner());
            if (player != null && player.isOnline()) {
                itemAbilityManager.rebuildSubscriptions(update.owner(), current, reputation, "measured-outcome-window");
            }
        }
    }

    private void logEvolutionFailure(UUID owner, RuntimeException exception) {
        plugin.getLogger().log(Level.SEVERE, "[Evolution] Outcome update failed for " + owner + "; earned reputation is not replayed on retry.", exception);
    }
    private void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }
}
