package obtuseloot.abilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TraitProjectionMatrix {
    private final List<String> abilityIds = new ArrayList<>();
    private final List<AbilityTraitVector> vectors = new ArrayList<>();
    private final Map<String, Integer> indexByAbilityId = new HashMap<>();

    public synchronized void register(String abilityId, AbilityTraitVector vector) {
        Integer existing = indexByAbilityId.get(abilityId);
        if (existing == null) {
            indexByAbilityId.put(abilityId, vectors.size());
            abilityIds.add(abilityId);
            vectors.add(vector);
            return;
        }
        vectors.set(existing, vector);
    }

    public synchronized AbilityTraitVector forAbility(String abilityId) {
        Integer index = indexByAbilityId.get(abilityId);
        return index == null ? null : vectors.get(index);
    }

    public synchronized Map<String, Double> scoreAll(GenomeVector genomeVector) {
        Map<String, Double> scores = new HashMap<>(vectors.size());
        for (int i = 0; i < vectors.size(); i++) {
            scores.put(abilityIds.get(i), genomeVector.dot(vectors.get(i)));
        }
        return scores;
    }

    public synchronized int abilityCount() {
        return vectors.size();
    }

    public synchronized int dimensions() {
        return vectors.isEmpty() ? 0 : vectors.get(0).dimensions();
    }
}
