package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.event.Event;

public record AbilityEventContext(AbilityTrigger trigger, Artifact artifact, ArtifactReputation reputation,
                                  double value, String source,
                                  AbilityRuntimeContext runtimeContext, Event liveEvent, boolean liveExecution,
                                  obtuseloot.ecology.WorldPosition position, AbilityActivation activation,
                                  obtuseloot.abilities.genome.GenomeRevision genomeRevision) {
    public AbilityEventContext(AbilityTrigger trigger, Artifact artifact, ArtifactReputation reputation,
                               double value, String source, AbilityRuntimeContext runtimeContext, Event liveEvent,
                               boolean liveExecution, obtuseloot.ecology.WorldPosition position, AbilityActivation activation) {
        this(trigger, artifact, reputation, value, source, runtimeContext, liveEvent, liveExecution, position, activation,
                obtuseloot.abilities.genome.GenomeRevision.capture(artifact));
    }
    public AbilityEventContext(AbilityTrigger trigger, Artifact artifact, ArtifactReputation reputation,
                               double value, String source, AbilityRuntimeContext runtimeContext, Event liveEvent,
                               boolean liveExecution, obtuseloot.ecology.WorldPosition position) {
        this(trigger, artifact, reputation, value, source, runtimeContext, liveEvent, liveExecution, position, null);
    }
    public AbilityEventContext(AbilityTrigger trigger, Artifact artifact, ArtifactReputation reputation,
                               double value, String source, AbilityRuntimeContext runtimeContext, Event liveEvent, boolean liveExecution) {
        this(trigger, artifact, reputation, value, source, runtimeContext, liveEvent, liveExecution, null);
    }
    public obtuseloot.ecology.WorldPosition position() {
        return position != null ? position : obtuseloot.ecology.WorldPosition.fromEvent(liveEvent);
    }
    public AbilityEventContext withPosition(obtuseloot.ecology.WorldPosition location) {
        return new AbilityEventContext(trigger, artifact, reputation, value, source, runtimeContext, liveEvent, liveExecution, location, activation, genomeRevision);
    }
    public AbilityEventContext withActivation(AbilityActivation activation) {
        return new AbilityEventContext(trigger, artifact, reputation, value, source, runtimeContext, liveEvent, liveExecution, position, activation, genomeRevision);
    }
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
