package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;

public record AbilityEventContext(AbilityTrigger trigger, Artifact artifact, ArtifactReputation reputation,
                                  double value, String source,
                                  AbilityRuntimeContext runtimeContext) {
    public AbilityEventContext(AbilityTrigger trigger, Artifact artifact, ArtifactReputation reputation,
                               double value, String source) {
        this(trigger, artifact, reputation, value, source, AbilityRuntimeContext.passive(AbilitySource.OTHER));
    }
}
