package obtuseloot.abilities;

import java.util.List;

public record AbilityTemplate(
        String id,
        String name,
        AbilityFamily family,
        AbilityTrigger trigger,
        AbilityMechanic mechanic,
        String effectPattern,
        String evolutionVariant,
        String driftVariant,
        String awakeningVariant,
        String fusionVariant,
        String memoryVariant,
        List<AbilityModifier> supportModifiers
) {
}
