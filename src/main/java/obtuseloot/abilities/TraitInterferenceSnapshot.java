package obtuseloot.abilities;

import java.util.List;

public record TraitInterferenceSnapshot(
        double averageModifier,
        double latentActivationBias,
        double mutationBias,
        List<String> activeEffects
) {
}
