package com.falcoignis.obtuseloot.awakening;

import com.falcoignis.obtuseloot.artifacts.Artifact;
import com.falcoignis.obtuseloot.artifacts.ArtifactManager;
import com.falcoignis.obtuseloot.reputation.ArtifactReputation;

import org.bukkit.entity.Player;

public final class AwakeningEngine {
    private AwakeningEngine() {}

    public static void checkAwakening(Player player, ArtifactReputation reputation) {
        if (reputation.score() < 500) {
            return;
        }

        Artifact artifact = ArtifactManager.getOrCreate(player.getUniqueId());
        if (!artifact.isAwakened()) {
            artifact.setAwakened(true);
            player.sendMessage("Your artifact has awakened.");
        }
    }
}
