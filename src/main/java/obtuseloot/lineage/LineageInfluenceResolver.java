package obtuseloot.lineage;

import java.util.Map;

public class LineageInfluenceResolver {
    public double resolveFamilyInfluence(ArtifactLineage lineage, String family) {
        if (lineage == null) {
            return 1.0D;
        }
        double trait = lineage.lineageTraits().getOrDefault(family.toLowerCase(), 0.0D);
        return clamp(1.0D + trait);
    }

    public double resolveMutationInfluence(ArtifactLineage lineage) {
        if (lineage == null) {
            return 1.0D;
        }
        return clamp(1.0D + lineage.lineageTraits().getOrDefault("mutation", 0.0D));
    }

    public Map<String, Double> traitSnapshot(ArtifactLineage lineage) {
        return lineage == null ? Map.of() : Map.copyOf(lineage.lineageTraits());
    }

    private double clamp(double value) {
        return Math.max(0.90D, Math.min(1.10D, value));
    }
}
