package com.falcoignis.obtuseloot.drift;

import com.falcoignis.obtuseloot.reputation.ArtifactReputation;

import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public final class DriftEngine {
    private DriftEngine() {}

    public static boolean shouldDrift(ArtifactReputation reputation) {
        int score = Math.max(1, reputation.score());
        double chance = Math.min(0.35D, score / 1000.0D);
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    public static void applyDrift(Player player, ArtifactReputation reputation) {
        player.sendMessage("Your artifact drifts with unstable intent...");
        reputation.decay();
    }
}
