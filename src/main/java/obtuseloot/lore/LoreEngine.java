package obtuseloot.lore;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;
import obtuseloot.significance.ArtifactSignificanceResolver;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LoreEngine {
    private static final long ACTIONBAR_MIN_INTERVAL_MS = 500L;
    private final Map<UUID, Long> lastActionBarUpdate = new ConcurrentHashMap<>();
    private final LoreFragmentGenerator fragmentGenerator = new LoreFragmentGenerator();
    private final LoreHistoryFormatter historyFormatter = new LoreHistoryFormatter();
    private final ArtifactSignificanceResolver significanceResolver = new ArtifactSignificanceResolver();

    public void refreshLore(Player player, Artifact artifact, ArtifactReputation reputation) {
        long now = System.currentTimeMillis();
        Long lastUpdate = lastActionBarUpdate.get(player.getUniqueId());
        if (lastUpdate == null || now - lastUpdate >= ACTIONBAR_MIN_INTERVAL_MS) {
            player.sendActionBar(net.kyori.adventure.text.Component.text(buildActionBarSummary(artifact, reputation)));
            lastActionBarUpdate.put(player.getUniqueId(), now);
        }
        historyFormatter.trimHistory(artifact, 80);
    }

    public String buildActionBarSummary(Artifact artifact, ArtifactReputation reputation) {
        return artifact.getArchetypePath() + "|" + artifact.getEvolutionPath() + " D" + artifact.getDriftLevel()
                + " S" + reputation.getTotalScore();
    }

    public List<String> buildLoreLines(Artifact artifact, ArtifactReputation reputation) {
        List<String> lines = new ArrayList<>();
        lines.add(significanceResolver.resolve(artifact).format());
        lines.add(fragmentGenerator.epithetFragment(artifact, reputation));
        lines.add(fragmentGenerator.loreFragment(artifact, reputation));
        return lines;
    }

    public void recordLoreEvent(Artifact artifact, String eventId) {
        artifact.addLoreHistory(eventId);
        artifact.addNotableEvent(eventId);
    }

    public void removePlayer(UUID playerId) {
        lastActionBarUpdate.remove(playerId);
    }
}
