package obtuseloot.abilities;

import java.util.ArrayList;
import java.util.List;

public final class EventAbilityDispatcher {

    public List<String> dispatchIndexed(AbilityEventContext context,
                                        List<ArtifactTriggerBinding> bindings,
                                        ItemAbilityManager manager) {
        List<String> activated = new ArrayList<>(bindings.size());
        int stage = ArtifactEvolutionStage.resolveStage(context.artifact());
        for (ArtifactTriggerBinding binding : bindings) {
            AbilityDefinition def = binding.definition();
            activated.add(def.name() + " -> " + def.stageDescription(stage));
            manager.recordTriggerDispatch(def, context.trigger());
        }
        return activated;
    }

    public List<String> dispatchFullScan(AbilityEventContext context,
                                         AbilityProfile profile,
                                         ItemAbilityManager manager) {
        List<String> activated = new ArrayList<>();
        int stage = ArtifactEvolutionStage.resolveStage(context.artifact());
        for (AbilityDefinition def : profile.abilities()) {
            if (def.trigger() == context.trigger()) {
                activated.add(def.name() + " -> " + def.stageDescription(stage));
                manager.recordTriggerDispatch(def, context.trigger());
            }
        }
        return activated;
    }
}
