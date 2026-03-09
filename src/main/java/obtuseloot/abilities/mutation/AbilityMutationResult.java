package obtuseloot.abilities.mutation;

import obtuseloot.abilities.AbilityDefinition;

import java.util.List;

public record AbilityMutationResult(List<AbilityDefinition> abilities, List<AbilityMutation> mutations) {
}
