package com.falcoignis.obtuseloot.awakening;

import com.falcoignis.obtuseloot.artifacts.Artifact;
import com.falcoignis.obtuseloot.artifacts.ArtifactManager;
import com.falcoignis.obtuseloot.reputation.ArtifactReputation;

import org.bukkit.entity.Player;

public final class AwakeningEngine {
    private AwakeningEngine() {
    }

    public static boolean checkAwakening(Player player, ArtifactReputation rep) {
        Artifact artifact = ArtifactManager.getOrCreate(player.getUniqueId());
        String before = artifact.getAwakeningPath();

        boolean levelGate = rep.score() >= 0.7;
        boolean milestone = rep.bossKills() > 0 || rep.kills() >= 100;
        if (!levelGate || !milestone) {
            return false;
        }

        if (rep.precision() > 0.7) {
            artifact.setAwakeningPath("Executioner's Oath");
        } else if ((rep.mobility() + rep.chaos()) > 1.0) {
            artifact.setAwakeningPath("Stormblade");
        } else if (rep.survival() > 0.7) {
            artifact.setAwakeningPath("Last Survivor");
        }

        return !before.equals(artifact.getAwakeningPath());
    }
}
