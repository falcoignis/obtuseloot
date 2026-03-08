package com.falcoignis.obtuseloot.drift;

import com.falcoignis.obtuseloot.reputation.ArtifactReputation;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public final class DriftEngine {
    private DriftEngine() {
    }

    public static boolean shouldDrift(ArtifactReputation rep) {
        double base = 0.03;
        double driftChance = base + (rep.chaos() * 0.01) - (rep.consistency() * 0.005);
        double clamped = Math.max(0.0, Math.min(0.60, driftChance));
        return ThreadLocalRandom.current().nextDouble() < clamped;
    }

    public static void applyDrift(Player player) {
        player.sendMessage("§5Your artifact drifts into a new unstable form...");
    }

    public static void periodicDriftCheck() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("Artifact drift stirs..."));
        }
    }
}
