package obtuseloot.abilities;

import java.util.List;

public record AbilityDispatchResult(AbilityEventContext context, List<AbilityExecutionResult> executions) {

    public List<String> presentationEffects() {
        return executions.stream()
                .filter(result -> result.status() == AbilityExecutionStatus.SUCCESS || result.status() == AbilityExecutionStatus.NO_OP)
                .map(AbilityExecutionResult::outputText)
                .filter(text -> text != null && !text.isBlank())
                .toList();
    }

    public boolean hasSuccessfulMechanic(AbilityMechanic mechanic) {
        return executions.stream().anyMatch(result -> result.mechanic() == mechanic && result.status() == AbilityExecutionStatus.SUCCESS);
    }
}
