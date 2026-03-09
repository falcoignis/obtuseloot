package obtuseloot.obtuseengine;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.Artifact;
import obtuseloot.combat.CombatContext;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.drift.DriftMutation;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class ArtifactProcessor {
    private ArtifactProcessor() {
    }

    public static void processKill(Player player) {
        ObtuseLoot plugin = ObtuseLoot.get();
        ArtifactReputation rep = plugin.getReputationManager().getReputation(player.getUniqueId());
        Artifact artifact = plugin.getArtifactManager().getOrCreateArtifact(player.getUniqueId());
        CombatContext context = plugin.getCombatContextManager().get(player.getUniqueId());

        long now = System.currentTimeMillis();
        context.addKillTimestamp(now);
        rep.recordKill();
        rep.recordKillChain(context.countKillsWithinWindow(now, RuntimeSettings.get().killChainWindowMs()));

        plugin.getEvolutionEngine().evaluate(player, artifact, rep);
        if (plugin.getDriftEngine().shouldDrift(rep)) {
            DriftMutation mutation = plugin.getDriftEngine().applyDrift(player, artifact, rep);
            if (mutation.causedEvolutionRecheck()) {
                plugin.getEvolutionEngine().evaluate(player, artifact, rep);
            }
        }
        plugin.getAwakeningEngine().evaluate(player, artifact, rep);
        plugin.getLoreEngine().refreshLore(player, artifact, rep);
    }

    public static void processCombat(Player player, EntityDamageByEntityEvent event) {
        ObtuseLoot plugin = ObtuseLoot.get();
        ArtifactReputation rep = plugin.getReputationManager().getReputation(player.getUniqueId());
        Artifact artifact = plugin.getArtifactManager().getOrCreateArtifact(player.getUniqueId());
        CombatContext context = plugin.getCombatContextManager().get(player.getUniqueId());

        context.markCombat();
        rep.setLastCombatTimestamp(System.currentTimeMillis());

        if (event.getFinalDamage() > 8.0D) rep.recordBrutality();
        else rep.recordPrecision();
        rep.recordConsistency();

        plugin.getLoreEngine().refreshLore(player, artifact, rep);
    }
}
