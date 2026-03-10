package obtuseloot.abilities;

public record ArtifactTriggerBinding(
        long artifactSeed,
        String artifactName,
        String abilityId,
        String abilityName,
        AbilityTrigger trigger,
        AbilityDefinition definition
) {
}
