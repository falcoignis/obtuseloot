package obtuseloot.evolution;

import java.util.Map;

public record LineageMomentumPool(
        Map<String, LineageMomentumProfile> momentumByLineage,
        double totalMomentum,
        double dominanceShare,
        double displacementPressure
) {
    public LineageMomentumProfile profile(String lineageId) {
        return momentumByLineage.getOrDefault(lineageId, new LineageMomentumProfile(
                lineageId,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                1.0D,
                1.0D,
                1.0D,
                1.0D));
    }
}
