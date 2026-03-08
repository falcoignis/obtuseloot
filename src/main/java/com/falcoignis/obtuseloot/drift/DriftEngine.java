package com.falcoignis.obtuseloot.drift;

import com.falcoignis.obtuseloot.reputation.ArtifactReputation;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public final class DriftEngine {
    private DriftEngine() {
    }

    public static double driftChance(ArtifactReputation rep) {
        double base = 0.01;
        return Math.max(0.0, Math.min(1.0, base + rep.chaos() * 0.2 - rep.consistency() * 0.1));
    }

    public static boolean shouldDrift(ArtifactReputation rep) {
        return ThreadLocalRandom.current().nextDouble() < driftChance(rep);
    }

    public static void applyDrift(Player player) {
        player.sendMessage("§5Your artifact drifts: stats and affinity subtly mutate.");
    }

    public static void periodicDriftCheck() {
        // Event-driven design: scheduler only performs lightweight decay and optional notifications.
        Bukkit.getOnlinePlayers().forEach(player -> {
            // no-op heavy processing; placeholder for future drift queues
        });
    }
}
