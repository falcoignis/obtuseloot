package com.falcoignis.obtuseloot.obtuseengine;

import com.falcoignis.obtuseloot.awakening.AwakeningEngine;
import com.falcoignis.obtuseloot.drift.DriftEngine;
import com.falcoignis.obtuseloot.evolution.EvolutionEngine;
import com.falcoignis.obtuseloot.lore.LoreEngine;
import com.falcoignis.obtuseloot.reputation.ArtifactReputation;
import com.falcoignis.obtuseloot.reputation.ReputationManager;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ArtifactProcessor {

    public static void processKill(Player player) {
        ArtifactReputation rep = ReputationManager.get(player.getUniqueId());
        rep.recordKill();

        boolean evolved = EvolutionEngine.checkEvolution(player, rep);
        boolean drifted = false;
        if (DriftEngine.shouldDrift(rep)) {
            DriftEngine.applyDrift(player);
            drifted = true;
        }

        boolean awakened = AwakeningEngine.checkAwakening(player, rep);

        if (evolved || drifted || awakened) {
            LoreEngine.refreshLore(player, rep);
        }
    }

    public static void processCombat(Player player, EntityDamageByEntityEvent event) {
        ArtifactReputation rep = ReputationManager.get(player.getUniqueId());
        if (event.getFinalDamage() >= 10.0D) {
            rep.recordHeadshot();
        } else {
            rep.recordHit();
        }
        if (event.getFinalDamage() > 14.0D) {
            rep.recordRisk();
        }
    }
}
