package com.falcoignis.obtuseloot.awakening;

import com.falcoignis.obtuseloot.artifacts.Artifact;
import com.falcoignis.obtuseloot.artifacts.ArtifactManager;
import com.falcoignis.obtuseloot.reputation.ArtifactReputation;

import org.bukkit.entity.Player;

public final class AwakeningEngine {
    private AwakeningEngine() {
    }

    public static void checkAwakening(Player player, ArtifactReputation rep) {
        Artifact artifact = ArtifactManager.getOrCreate(player.getUniqueId());
        if (!"dormant".equals(artifact.getAwakeningPath())) {
            return;
        }

        if (rep.score() >= 500 && rep.bossKills() >= 1) {
            artifact.setAwakeningPath("Executioner's Oath");
        } else if (rep.precision() >= 120 && rep.mobility() >= 80) {
            artifact.setAwakeningPath("Stormblade");
        } else if (rep.survival() >= 120 && rep.consistency() >= 100) {
            artifact.setAwakeningPath("Last Survivor");
        }
    }
}
