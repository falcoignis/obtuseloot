package obtuseloot.obtuseengine;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.Artifact;
import obtuseloot.combat.CombatContext;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.drift.DriftMutation;
import obtuseloot.fusion.FusionEngine;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.concurrent.ThreadLocalRandom;

public final class ArtifactProcessor {
    private static final FusionEngine FUSION_ENGINE = new FusionEngine();

    private ArtifactProcessor() {
    }

    public static void processKill(Player player) {
        ObtuseLoot plugin = ObtuseLoot.get();
        ArtifactReputation rep = plugin.getReputationManager().get(player.getUniqueId());
        Artifact artifact = plugin.getArtifactManager().getOrCreate(player.getUniqueId());
        CombatContext context = plugin.getCombatContextManager().get(player.getUniqueId());

        String oldArchetype = artifact.getArchetypePath();
        String oldEvolution = artifact.getEvolutionPath();
        String oldAwakening = artifact.getAwakeningPath();
        String oldFusion = artifact.getFusionPath();
        int oldDriftLevel = artifact.getDriftLevel();

        rep.recordKill();
        applyContextKillBonuses(player, context, rep, artifact);

        plugin.getEvolutionEngine().evaluate(player, artifact, rep);
        if (plugin.getDriftEngine().shouldDrift(rep)) {
            DriftMutation mutation = plugin.getDriftEngine().applyDrift(player, artifact, rep);
            if (mutation.applied()) {
                plugin.getEvolutionEngine().evaluate(player, artifact, rep);
            }
        }

        plugin.getAwakeningEngine().evaluate(player, artifact, rep);
        FUSION_ENGINE.evaluate(player, artifact, rep);

        recordStateTransitions(artifact, oldArchetype, oldEvolution, oldAwakening, oldFusion, oldDriftLevel);
        context.resetTransient();
        plugin.getLoreEngine().refreshLore(player, artifact, rep);
    }

    public static void processCombat(Player player, EntityDamageByEntityEvent event) {
        ObtuseLoot plugin = ObtuseLoot.get();
        ArtifactReputation rep = plugin.getReputationManager().get(player.getUniqueId());
        Artifact artifact = plugin.getArtifactManager().getOrCreate(player.getUniqueId());
        CombatContext context = plugin.getCombatContextManager().get(player.getUniqueId());

        context.markCombat();
        rep.setLastCombatTimestamp(System.currentTimeMillis());

        if (event.getFinalDamage() >= RuntimeSettings.get().precisionThresholdDamage()) {
            applyReputationGainWithAwakening(artifact, rep, "precision");
        } else {
            applyReputationGainWithAwakening(artifact, rep, "brutality");
        }
        applyReputationGainWithAwakening(artifact, rep, "consistency");

        applyCombatContextBonuses(player, context, rep, artifact);
        plugin.getLoreEngine().refreshLore(player, artifact, rep);
    }

    public static void applyCombatContextBonuses(Player player, CombatContext context, ArtifactReputation rep, Artifact artifact) {
        if (context.getRecentMovementDistance() >= RuntimeSettings.get().mobilityDistanceThreshold()) {
            applyReputationGainWithAwakening(artifact, rep, "mobility");
            context.consumeMovement(RuntimeSettings.get().mobilityDistanceThreshold());
        }

        if (context.isLowHealthFlag() || player.getHealth() <= RuntimeSettings.get().lowHealthThreshold()) {
            applyReputationGainWithAwakening(artifact, rep, "survival");
        }
    }

    public static void applyContextKillBonuses(Player player, CombatContext context, ArtifactReputation rep, Artifact artifact) {
        if (context.getRecentMovementDistance() >= RuntimeSettings.get().mobilityDistanceThreshold()) {
            applyReputationGainWithAwakening(artifact, rep, "mobility");
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
    }
}
