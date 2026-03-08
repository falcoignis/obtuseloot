package com.falcoignis.obtuseloot.lore;

import com.falcoignis.obtuseloot.artifacts.Artifact;
import com.falcoignis.obtuseloot.artifacts.ArtifactManager;
import com.falcoignis.obtuseloot.config.RuntimeSettings;
import com.falcoignis.obtuseloot.reputation.ArtifactReputation;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LoreEngine {
    private static final Map<UUID, Long> LAST_ACTIONBAR_UPDATE_MS = new ConcurrentHashMap<>();

    private LoreEngine() {
    }

    public static void refreshLore(Player player, ArtifactReputation rep) {
        long now = System.currentTimeMillis();
        long minIntervalMs = RuntimeSettings.get().loreMinUpdateIntervalMs();
        UUID playerId = player.getUniqueId();

        Long lastUpdate = LAST_ACTIONBAR_UPDATE_MS.get(playerId);
        if (lastUpdate != null && now - lastUpdate < minIntervalMs) {
            return;
        }

        Artifact artifact = ArtifactManager.getOrCreate(playerId);
        List<String> lore = buildLore(artifact, rep);
        if (!lore.isEmpty()) {
            player.sendActionBar(net.kyori.adventure.text.Component.text(lore.get(0)));
            LAST_ACTIONBAR_UPDATE_MS.put(playerId, now);
        }
    }

    public static void removePlayer(UUID playerId) {
        LAST_ACTIONBAR_UPDATE_MS.remove(playerId);
    }

    public static List<String> buildLore(Artifact artifact, ArtifactReputation rep) {
        List<String> lines = new ArrayList<>();
        lines.add("The artifact hums with a " + artifact.getEvolutionPath() + " resonance.");
        if (!"dormant".equals(artifact.getAwakeningPath())) {
            lines.add("It now answers the path of " + artifact.getAwakeningPath() + ".");
        }
        lines.add("Its legacy reflects precision " + rep.precision() + " and chaos " + rep.chaos() + ".");
        return lines;
    }
}
