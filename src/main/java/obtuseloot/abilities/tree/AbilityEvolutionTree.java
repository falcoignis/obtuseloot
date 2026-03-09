package obtuseloot.abilities.tree;

import java.util.List;

public record AbilityEvolutionTree(String abilityId, String selectedBranch, List<AbilityBranch> branches) {
}
