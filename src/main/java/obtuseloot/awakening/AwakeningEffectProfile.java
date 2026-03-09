package obtuseloot.awakening;

import java.util.Map;
import java.util.Set;

public record AwakeningEffectProfile(String id, Map<String, Double> biasAdjustments,
                                     Map<String, Double> reputationGainMultipliers,
                                     Set<String> traits) {
}
