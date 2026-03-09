package obtuseloot.lineage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ArtifactLineage {
    private final String lineageId;
    private int depth;
    private final List<ArtifactAncestor> ancestors = new ArrayList<>();
    private final Map<String, Double> lineageTraits = new LinkedHashMap<>();

    public ArtifactLineage(String lineageId) {
        this.lineageId = lineageId;
        for (String trait : new String[]{"precision", "brutality", "survival", "mobility", "chaos", "consistency", "mutation", "memory"}) {
            lineageTraits.put(trait, 0.0D);
        }
    }

    public String lineageId() { return lineageId; }
    public int depth() { return depth; }
    public List<ArtifactAncestor> ancestors() { return ancestors; }
    public Map<String, Double> lineageTraits() { return lineageTraits; }

    public void addAncestor(ArtifactAncestor ancestor) {
        ancestors.add(ancestor);
        depth = Math.max(depth, ancestor.generationIndex());
    }

    public void applyMutation(LineageMutation mutation) {
        double current = lineageTraits.getOrDefault(mutation.trait(), 0.0D);
        lineageTraits.put(mutation.trait(), clamp(current + mutation.delta()));
    }

    private double clamp(double value) {
        return Math.max(-0.10D, Math.min(0.10D, value));
    }
}
