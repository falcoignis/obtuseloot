package obtuseloot.abilities;

import java.util.List;

public record AbilityTemplate(
        String id,
        String name,
        AbilityCategory category,
        AbilityFamily family,
        AbilityTrigger trigger,
        AbilityMechanic mechanic,
        String effectPattern,
        String evolutionVariant,
        String driftVariant,
        String awakeningVariant,
        String convergenceVariant,
        String memoryVariant,
        List<AbilityModifier> supportModifiers,
        AbilityMetadata metadata
) {
}
