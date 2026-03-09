package obtuseloot.abilities;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TraitProjectionMatrix {
    private final Map<String, AbilityTraitVector> vectorsByAbilityId = new ConcurrentHashMap<>();

    public void register(String abilityId, AbilityTraitVector vector) {
        vectorsByAbilityId.put(abilityId, vector);
    }

    public AbilityTraitVector forAbility(String abilityId) {
        return vectorsByAbilityId.get(abilityId);
    }
}
