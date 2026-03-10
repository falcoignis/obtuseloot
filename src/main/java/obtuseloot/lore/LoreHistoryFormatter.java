package obtuseloot.lore;

import obtuseloot.artifacts.Artifact;
import obtuseloot.text.ArtifactTextChannel;
import obtuseloot.text.ArtifactTextResolver;

import java.util.ArrayList;
import java.util.List;

public class LoreHistoryFormatter {
    private final ArtifactTextResolver textResolver = new ArtifactTextResolver();

    public List<String> formatHistory(Artifact artifact, int maxLines) {
        List<String> history = artifact.getLoreHistory();
        int start = Math.max(0, history.size() - maxLines);
        List<String> lines = new ArrayList<>();
        for (String entry : history.subList(start, history.size())) {
            lines.add(textResolver.compose(artifact, ArtifactTextChannel.EVENT, entry));
        }
        return lines;
    }

    public void trimHistory(Artifact artifact, int maxEntries) {
        while (artifact.getLoreHistory().size() > maxEntries) artifact.getLoreHistory().remove(0);
        while (artifact.getDriftHistory().size() > maxEntries) artifact.getDriftHistory().remove(0);
        while (artifact.getNotableEvents().size() > maxEntries) artifact.getNotableEvents().remove(0);
    }
}
