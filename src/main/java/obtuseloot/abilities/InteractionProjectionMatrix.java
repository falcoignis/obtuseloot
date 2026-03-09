package obtuseloot.abilities;

import java.util.HashMap;
import java.util.Map;

public final class InteractionProjectionMatrix {
    private final Map<String, double[][]> interactionByAbility = new HashMap<>();

    public synchronized void register(String abilityId, double[][] interactionMatrix) {
        interactionByAbility.put(abilityId, interactionMatrix);
    }

    public synchronized double interactionScore(String abilityId, GenomeVector genomeVector) {
        double[][] matrix = interactionByAbility.get(abilityId);
        if (matrix == null) {
            return 0.0D;
        }
        double score = 0.0D;
        for (int i = 0; i < genomeVector.dimensions(); i++) {
            double left = genomeVector.component(i);
            for (int j = 0; j < genomeVector.dimensions(); j++) {
                score += left * matrix[i][j] * genomeVector.component(j);
            }
        }
        return score;
    }
}
