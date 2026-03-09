package obtuseloot.lore;

import obtuseloot.artifacts.Artifact;

import java.util.ArrayList;
import java.util.List;

public class LoreHistoryFormatter {
    public List<String> formatHistory(Artifact artifact, int maxLines) {
        List<String> history = artifact.getLoreHistory();
        int start = Math.max(0, history.size() - maxLines);
        return new ArrayList<>(history.subList(start, history.size()));
    }

    public void trimHistory(Artifact artifact, int maxEntries) {
        while (artifact.getLoreHistory().size() > maxEntries) artifact.getLoreHistory().remove(0);
        while (artifact.getDriftHistory().size() > maxEntries) artifact.getDriftHistory().remove(0);
        while (artifact.getNotableEvents().size() > maxEntries) artifact.getNotableEvents().remove(0);
    }
}
