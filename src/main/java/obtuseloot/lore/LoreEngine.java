package obtuseloot.lore;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LoreEngine {
    private final LoreFragmentGenerator fragmentGenerator = new LoreFragmentGenerator();
    private final LoreHistoryFormatter historyFormatter = new LoreHistoryFormatter();

    public void refreshLore(Player player, Artifact artifact, ArtifactReputation reputation) {
        player.sendActionBar(net.kyori.adventure.text.Component.text(buildActionBarSummary(artifact, reputation)));
        historyFormatter.trimHistory(artifact, 50);
    }

    public String buildActionBarSummary(Artifact artifact, ArtifactReputation reputation) {
        return artifact.getArchetypePath() + " | " + artifact.getEvolutionPath() + " | score " + reputation.getTotalScore();
    }

    public List<String> buildLoreLines(Artifact artifact, ArtifactReputation reputation) {
        List<String> lines = new ArrayList<>();
        lines.add(fragmentGenerator.lineageFragment(artifact));
        lines.add(fragmentGenerator.driftFragment(artifact));
        lines.add(fragmentGenerator.awakeningFragment(artifact));
        lines.add(fragmentGenerator.instabilityFragment(artifact));
        lines.add("Stats P" + reputation.getPrecision() + " B" + reputation.getBrutality() + " S" + reputation.getSurvival());
        lines.addAll(historyFormatter.formatHistory(artifact, 4));
        return lines;
    }

    public void recordLoreEvent(Artifact artifact, String eventId) {
        artifact.addLoreHistory("Event:" + eventId);
        artifact.addNotableEvent(eventId);
    }
}
