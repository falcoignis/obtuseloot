package obtuseloot.abilities;

import java.util.ArrayList;
import java.util.List;

public final class EventAbilityDispatcher {
    private final AbilityExecutor executor = new AbilityExecutor();
    private final TriggerBudgetResolver budgetResolver = new TriggerBudgetResolver();

    public AbilityDispatchResult dispatchIndexed(AbilityEventContext context,
                                                 List<ArtifactTriggerBinding> bindings,
                                                 ItemAbilityManager manager) {
        List<AbilityExecutionResult> executions = new ArrayList<>(bindings.size());
        int stage = ArtifactEvolutionStage.resolveStage(context.artifact());
        for (ArtifactTriggerBinding binding : bindings) {
            manager.recordExecution(attempted(binding.definition(), context));
            AbilityExecutionResult result = executeWithBudget(binding.definition(), context, stage, manager);
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
                manager.recordExecution(attempted(def, context));
                AbilityExecutionResult result = executeWithBudget(def, context, stage, manager);
                executions.add(result);
                manager.recordExecution(result);
            }
        }
        return new AbilityDispatchResult(context, List.copyOf(executions));
    }


    private AbilityExecutionResult attempted(AbilityDefinition definition,
                                             AbilityEventContext context) {
        return new AbilityExecutionResult(
                definition.id(),
                definition.mechanic(),
                context.trigger(),
                context.artifact().getArtifactStorageKey(),
                context.artifact().getOwnerId(),
                AbilityExecutionStatus.ATTEMPTED,
                AbilityOutcomeType.FLAVOR_ONLY,
                false,
                null,
                null
        );
    }

    private AbilityExecutionResult executeWithBudget(AbilityDefinition definition,
                                                     AbilityEventContext context,
                                                     int stage,
                                                     ItemAbilityManager manager) {
        TriggerBudgetProfile profile = budgetResolver.resolve(definition, context);
        TriggerBudgetDecision precheck = manager.triggerBudgetManager().preCheck(context, definition, profile);
        if (!precheck.allowed()) {
            return suppressed(definition, context, precheck.suppressionReason());
        }

        TriggerBudgetDecision activation = manager.triggerBudgetManager().consumeActivation(context, definition, profile);
        if (!activation.allowed()) {
            return suppressed(definition, context, activation.suppressionReason());
        }
        return executor.execute(definition, context, stage);
    }

    private AbilityExecutionResult suppressed(AbilityDefinition definition,
                                              AbilityEventContext context,
                                              TriggerSuppressionReason reason) {
        String suppression = reason == null ? "trigger-budget-unknown" : "trigger-budget-" + reason.name().toLowerCase();
        return new AbilityExecutionResult(
                definition.id(),
                definition.mechanic(),
                context.trigger(),
                context.artifact().getArtifactStorageKey(),
                context.artifact().getOwnerId(),
                AbilityExecutionStatus.SUPPRESSED,
                AbilityOutcomeType.FLAVOR_ONLY,
                false,
                suppression,
                null
        );
    }
}
