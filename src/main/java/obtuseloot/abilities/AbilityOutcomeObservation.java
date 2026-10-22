package obtuseloot.abilities;

/** Utility change produced by one execution; classifications and unfinished effects have no measurement. */
public record AbilityOutcomeObservation(AbilityEventContext context, AbilityExecutionResult result,
                                       AbilityDefinition definition, double utilityDelta) {
    public boolean liveMeasured() {
        return context.liveExecution()
                && context.genomeRevision() != null
                && result.observedAmount() != null && Double.isFinite(result.observedAmount())
                && result.observedAmount() >= 0 && Double.isFinite(utilityDelta)
                && definition.id().equals(result.abilityId()) && definition.mechanic() == result.mechanic()
                && definition.trigger() == context.trigger() && result.trigger() == context.trigger()
                && java.util.Objects.equals(context.artifact().getArtifactStorageKey(), result.artifactStorageKey())
                && java.util.Objects.equals(context.artifact().getOwnerId(), result.holderId())
                && (result.observedAmount() > 0) == result.meaningfulOutcome()
                && (result.observedAmount() > 0) == (result.status() == AbilityExecutionStatus.SUCCESS)
                && (result.status() == AbilityExecutionStatus.SUCCESS || result.status() == AbilityExecutionStatus.NO_OP);
    }
}
