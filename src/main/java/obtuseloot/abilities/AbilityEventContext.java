package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.event.Event;

public record AbilityEventContext(AbilityTrigger trigger, Artifact artifact, ArtifactReputation reputation,
                                  double value, String source,
                                  AbilityRuntimeContext runtimeContext, Event liveEvent, boolean liveExecution) {
    public AbilityEventContext(AbilityTrigger trigger, Artifact artifact, ArtifactReputation reputation,
                               double value, String source, AbilityRuntimeContext runtimeContext, Event liveEvent) {
        this(trigger, artifact, reputation, value, source, runtimeContext, liveEvent, true);
    }
    public AbilityEventContext(AbilityTrigger trigger, Artifact artifact, ArtifactReputation reputation,
                               double value, String source, AbilityRuntimeContext runtimeContext) {
        this(trigger, artifact, reputation, value, source, runtimeContext, null, false);
    }
    public AbilityEventContext(AbilityTrigger trigger, Artifact artifact, ArtifactReputation reputation,
                               double value, String source) {
        this(trigger, artifact, reputation, value, source, AbilityRuntimeContext.passive(AbilitySource.OTHER));
    }
}
