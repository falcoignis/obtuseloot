package obtuseloot.abilities;

import java.util.ArrayList;
import java.util.List;

public final class EventAbilityDispatcher {
    private final AbilityExecutor executor = new AbilityExecutor();

    public AbilityDispatchResult dispatchIndexed(AbilityEventContext context,
                                                 List<ArtifactTriggerBinding> bindings,
                                                 ItemAbilityManager manager) {
        List<AbilityExecutionResult> executions = new ArrayList<>(bindings.size());
        int stage = ArtifactEvolutionStage.resolveStage(context.artifact());
        for (ArtifactTriggerBinding binding : bindings) {
            AbilityDefinition def = binding.definition();
            AbilityExecutionResult result = executor.execute(def, context, stage);
            executions.add(result);
            manager.recordExecution(result);
        }
        return new AbilityDispatchResult(context, List.copyOf(executions));
    }

    public AbilityDispatchResult dispatchFullScan(AbilityEventContext context,
                                                  AbilityProfile profile,
                                                  ItemAbilityManager manager) {
        List<AbilityExecutionResult> executions = new ArrayList<>();
        int stage = ArtifactEvolutionStage.resolveStage(context.artifact());
        for (AbilityDefinition def : profile.abilities()) {
            if (def.trigger() == context.trigger()) {
                AbilityExecutionResult result = executor.execute(def, context, stage);
                executions.add(result);
                manager.recordExecution(result);
            }
        }
        return new AbilityDispatchResult(context, List.copyOf(executions));
    }
}
