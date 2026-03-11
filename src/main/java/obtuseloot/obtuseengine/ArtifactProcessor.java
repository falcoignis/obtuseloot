package obtuseloot.obtuseengine;

import obtuseloot.ObtuseLoot;
import obtuseloot.abilities.AbilityEventContext;
import obtuseloot.abilities.AbilityDispatchResult;
import obtuseloot.abilities.AbilityTrigger;
import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.eligibility.ArtifactEligibility;
import obtuseloot.combat.CombatContext;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.drift.DriftMutation;
import obtuseloot.fusion.FusionEngine;
import obtuseloot.memory.ArtifactMemoryEvent;
import obtuseloot.names.ArtifactNameResolver;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class ArtifactProcessor {
    private static final FusionEngine FUSION_ENGINE = new FusionEngine();

    private ArtifactProcessor() {
    }

    public static void processKill(Player player) {
        ObtuseLoot plugin = ObtuseLoot.get();
        ArtifactReputation rep = plugin.getReputationManager().get(player.getUniqueId());
        Artifact artifact = plugin.getArtifactManager().getOrCreate(player.getUniqueId());
        plugin.getArtifactUsageTracker().trackUse(artifact);
        CombatContext context = plugin.getCombatContextManager().get(player.getUniqueId());

        String oldArchetype = artifact.getArchetypePath();
        String oldEvolution = artifact.getEvolutionPath();
        String oldAwakening = artifact.getAwakeningPath();
        String oldFusion = artifact.getFusionPath();
        int oldDriftLevel = artifact.getDriftLevel();

        rep.recordKill();
        plugin.getArtifactUsageTracker().trackKillParticipation(artifact);
        if (rep.getKills() == 1) { recordMemoryEvent(plugin, artifact, rep, ArtifactMemoryEvent.FIRST_KILL, "first-kill"); }
        applyContextKillBonuses(player, context, rep, artifact);

        plugin.getEvolutionEngine().evaluate(player, artifact, rep);
        triggerAbility(plugin, artifact, rep, AbilityTrigger.ON_KILL, 1D, "kill");
        if (plugin.getDriftEngine().shouldDrift(rep)) {
            DriftMutation mutation = plugin.getDriftEngine().applyDrift(player, artifact, rep);
            if (mutation.applied()) {
                plugin.getEvolutionEngine().evaluate(player, artifact, rep);
                triggerAbility(plugin, artifact, rep, AbilityTrigger.ON_DRIFT_MUTATION, 1D, mutation.profileId());
            }
        }

        if (plugin.getAwakeningEngine().evaluate(player, artifact, rep)) {
            plugin.getArtifactUsageTracker().trackAwakening(artifact);
            recordMemoryEvent(plugin, artifact, rep, ArtifactMemoryEvent.AWAKENING, "awakening");
            triggerAbility(plugin, artifact, rep, AbilityTrigger.ON_AWAKENING, 1D, artifact.getAwakeningPath());
        }
        if (FUSION_ENGINE.evaluate(player, artifact, rep)) {
            plugin.getArtifactUsageTracker().trackFusionParticipation(artifact);
            recordMemoryEvent(plugin, artifact, rep, ArtifactMemoryEvent.FUSION, "fusion");
            triggerAbility(plugin, artifact, rep, AbilityTrigger.ON_FUSION, 1D, artifact.getFusionPath());
        }

        recordStateTransitions(artifact, oldArchetype, oldEvolution, oldAwakening, oldFusion, oldDriftLevel);
        plugin.getItemAbilityManager().rebuildSubscriptions(player.getUniqueId(), artifact, rep, "kill-cycle-state-change");
        context.resetTransient();
        plugin.getLoreEngine().refreshLore(player, artifact, rep);
        plugin.getArtifactManager().markDirty(player.getUniqueId());
    }

    public static void processCombat(Player player, EntityDamageByEntityEvent event) {
        ObtuseLoot plugin = ObtuseLoot.get();
        ArtifactReputation rep = plugin.getReputationManager().get(player.getUniqueId());
        Artifact artifact = plugin.getArtifactManager().getOrCreate(player.getUniqueId());
        plugin.getArtifactUsageTracker().trackUse(artifact);
        CombatContext context = plugin.getCombatContextManager().get(player.getUniqueId());

        context.markCombat();
        rep.setLastCombatTimestamp(System.currentTimeMillis());

        if (event.getFinalDamage() >= RuntimeSettings.get().precisionThresholdDamage()) {
            applyReputationGainWithAwakening(artifact, rep, "precision");
        } else {
            applyReputationGainWithAwakening(artifact, rep, "brutality");
        }
        applyReputationGainWithAwakening(artifact, rep, "consistency");

        triggerAbility(plugin, artifact, rep, AbilityTrigger.ON_HIT, event.getFinalDamage(), "combat");
        triggerAbility(plugin, artifact, rep, AbilityTrigger.ON_CHAIN_COMBAT, 1D, "combat-chain");
        applyCombatContextBonuses(player, context, rep, artifact);
        plugin.getLoreEngine().refreshLore(player, artifact, rep);
        plugin.getArtifactManager().markDirty(player.getUniqueId());
    }

    public static void processAbilityTrigger(Player player, AbilityTrigger trigger, double value, String source) {
        processAbilityTriggerWithResult(player, trigger, value, source);
    }

    public static AbilityDispatchResult processAbilityTriggerWithResult(Player player, AbilityTrigger trigger, double value, String source) {
        ObtuseLoot plugin = ObtuseLoot.get();
        ArtifactReputation rep = plugin.getReputationManager().get(player.getUniqueId());
        Artifact artifact = plugin.getArtifactManager().getOrCreate(player.getUniqueId());
        plugin.getArtifactUsageTracker().trackUse(artifact);
        AbilityDispatchResult activated = triggerAbility(plugin, artifact, rep, trigger, value, source);
        plugin.getLoreEngine().refreshLore(player, artifact, rep);
        plugin.getArtifactManager().markDirty(player.getUniqueId());
        return activated;
    }

    public static void processSimulatedCombat(Player player, double damage) {
        ObtuseLoot plugin = ObtuseLoot.get();
        ArtifactReputation rep = plugin.getReputationManager().get(player.getUniqueId());
        Artifact artifact = plugin.getArtifactManager().getOrCreate(player.getUniqueId());
        plugin.getArtifactUsageTracker().trackUse(artifact);
        CombatContext context = plugin.getCombatContextManager().get(player.getUniqueId());

        context.markCombat();
        rep.setLastCombatTimestamp(System.currentTimeMillis());

        if (damage >= RuntimeSettings.get().precisionThresholdDamage()) {
            applyReputationGainWithAwakening(artifact, rep, "precision");
        } else {
            applyReputationGainWithAwakening(artifact, rep, "brutality");
        }
        applyReputationGainWithAwakening(artifact, rep, "consistency");

        triggerAbility(plugin, artifact, rep, AbilityTrigger.ON_HIT, damage, "combat-sim");
        triggerAbility(plugin, artifact, rep, AbilityTrigger.ON_CHAIN_COMBAT, 1D, "combat-chain");
        applyCombatContextBonuses(player, context, rep, artifact);
        plugin.getLoreEngine().refreshLore(player, artifact, rep);
        plugin.getArtifactManager().markDirty(player.getUniqueId());
    }

    public static void processSimulatedKill(Player player) {
        processKill(player);
    }

    public static void processSimulatedMultiKill(Player player, int count) {
        ObtuseLoot plugin = ObtuseLoot.get();
        Artifact artifact = plugin.getArtifactManager().getOrCreate(player.getUniqueId());
        ArtifactReputation rep = plugin.getReputationManager().get(player.getUniqueId());
        triggerAbility(plugin, artifact, rep, AbilityTrigger.ON_MULTI_KILL, count, "multi-kill");
        recordMemoryEvent(plugin, artifact, rep, ArtifactMemoryEvent.MULTIKILL_CHAIN, "multi-kill");
        for (int i = 0; i < count; i++) {
            processKill(player);
        }
    }

    public static void processSimulatedBossKill(Player player) {
        ObtuseLoot plugin = ObtuseLoot.get();
        Artifact artifact = plugin.getArtifactManager().getOrCreate(player.getUniqueId());
        ArtifactReputation rep = plugin.getReputationManager().get(player.getUniqueId());
        rep.recordBossKill();
        if (rep.getBossKills() == 1) { recordMemoryEvent(plugin, artifact, rep, ArtifactMemoryEvent.FIRST_BOSS_KILL, "boss-kill"); }
        triggerAbility(plugin, artifact, rep, AbilityTrigger.ON_BOSS_KILL, rep.getBossKills(), "boss-kill");
        processKill(player);
    }

    public static void applyCombatContextBonuses(Player player, CombatContext context, ArtifactReputation rep, Artifact artifact) {
        if (context.getRecentMovementDistance() >= RuntimeSettings.get().mobilityDistanceThreshold()) {
            applyReputationGainWithAwakening(artifact, rep, "mobility");
            triggerAbility(ObtuseLoot.get(), artifact, rep, AbilityTrigger.ON_MOVEMENT, context.getRecentMovementDistance(), "movement");
            triggerAbility(ObtuseLoot.get(), artifact, rep, AbilityTrigger.ON_REPOSITION, context.getRecentMovementDistance(), "movement");
            context.consumeMovement(RuntimeSettings.get().mobilityDistanceThreshold());
        }

        if (context.isLowHealthFlag() || player.getHealth() <= RuntimeSettings.get().lowHealthThreshold()) {
            applyReputationGainWithAwakening(artifact, rep, "survival");
            triggerAbility(ObtuseLoot.get(), artifact, rep, AbilityTrigger.ON_LOW_HEALTH, player.getHealth(), "low-health");
        }
    }

    public static void applyContextKillBonuses(Player player, CombatContext context, ArtifactReputation rep, Artifact artifact) {
        if (context.getRecentMovementDistance() >= RuntimeSettings.get().mobilityDistanceThreshold()) {
            applyReputationGainWithAwakening(artifact, rep, "mobility");
            triggerAbility(ObtuseLoot.get(), artifact, rep, AbilityTrigger.ON_MOVEMENT, context.getRecentMovementDistance(), "movement");
            triggerAbility(ObtuseLoot.get(), artifact, rep, AbilityTrigger.ON_REPOSITION, context.getRecentMovementDistance(), "movement");
            context.consumeMovement(RuntimeSettings.get().mobilityDistanceThreshold());
        }
    }

    public static void applyReputationGainWithAwakening(Artifact artifact, ArtifactReputation rep, String statKey) {
        incrementRep(rep, statKey);
        double multiplier = Math.max(1.0D, artifact.getAwakeningGainMultiplier(statKey));
        if (multiplier > 1.0D && ThreadLocalRandom.current().nextDouble() < (multiplier - 1.0D)) {
            incrementRep(rep, statKey);
        }
    }


    private static AbilityDispatchResult triggerAbility(ObtuseLoot plugin, Artifact artifact, ArtifactReputation rep, AbilityTrigger trigger, double value, String source) {
        if (!ArtifactEligibility.isAbilityEligible(artifact) || plugin.getItemAbilityManager() == null) {
            return new AbilityDispatchResult(new AbilityEventContext(trigger, artifact, rep, value, source), List.of());
        }
        return plugin.getItemAbilityManager().resolveDispatch(new AbilityEventContext(trigger, artifact, rep, value, source));
    }

    private static void recordMemoryEvent(ObtuseLoot plugin,
                                          Artifact artifact,
                                          ArtifactReputation rep,
                                          ArtifactMemoryEvent event,
                                          String source) {
        plugin.getArtifactMemoryEngine().recordAndProfile(artifact, event);
        long now = System.currentTimeMillis();
        if (plugin.getArtifactMemoryEngine().shouldEmitMemoryTrigger(artifact, event, now)) {
            triggerAbility(plugin, artifact, rep, AbilityTrigger.ON_MEMORY_EVENT, artifact.getMemory().pressure(), "memory-event:" + source + ":" + event.name().toLowerCase());
        }
    }

    private static void incrementRep(ArtifactReputation rep, String statKey) {
        switch (statKey) {
            case "precision" -> rep.recordPrecision();
            case "brutality" -> rep.recordBrutality();
            case "survival" -> rep.recordSurvival();
            case "mobility" -> rep.recordMobility();
            case "chaos" -> rep.recordChaos();
            case "consistency" -> rep.recordConsistency();
            default -> {
            }
        }
    }

    public static void recordStateTransitions(Artifact artifact,
                                              String oldArchetype,
                                              String oldEvolution,
                                              String oldAwakening,
                                              String oldFusion,
                                              int oldDriftLevel) {
        if (!oldArchetype.equals(artifact.getArchetypePath())) {
            artifact.addLoreHistory("Archetype shifted: " + oldArchetype + " -> " + artifact.getArchetypePath());
            artifact.addNotableEvent("archetype." + artifact.getArchetypePath());
        }
        if (!oldEvolution.equals(artifact.getEvolutionPath())) {
            artifact.addLoreHistory("Evolution path: " + oldEvolution + " -> " + artifact.getEvolutionPath());
            artifact.addNotableEvent("evolution." + artifact.getEvolutionPath());
        }
        if (!oldAwakening.equals(artifact.getAwakeningPath())) {
            artifact.addLoreHistory("Awakening manifested: " + artifact.getAwakeningPath());
            artifact.addNotableEvent("awakening." + artifact.getAwakeningPath().toLowerCase().replace(' ', '-'));
        }
        if (!oldFusion.equals(artifact.getFusionPath())) {
            artifact.addLoreHistory("Fusion milestone: " + artifact.getFusionPath());
            artifact.addNotableEvent("fusion." + artifact.getFusionPath());
        }
        if (oldDriftLevel != artifact.getDriftLevel()) {
            artifact.addLoreHistory("Drift level increased to " + artifact.getDriftLevel());
        }
        ArtifactNameResolver.refresh(artifact, artifact.getNaming());
    }
}
