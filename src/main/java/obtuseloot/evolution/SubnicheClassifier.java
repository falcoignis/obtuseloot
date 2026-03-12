package obtuseloot.evolution;

import obtuseloot.abilities.AbilityMechanic;
import obtuseloot.abilities.AbilityTrigger;

public class SubnicheClassifier {

    public String classify(MechanicNicheTag niche, AbilityMechanic mechanic, AbilityTrigger trigger) {
        return switch (niche) {
            case NAVIGATION -> {
                if (mechanic == AbilityMechanic.MEMORY_ECHO || trigger == AbilityTrigger.ON_MEMORY_EVENT) {
                    yield "recall_navigation";
                }
                if (mechanic == AbilityMechanic.NAVIGATION_ANCHOR) {
                    yield "route_guidance";
                }
                yield "exploration_navigation";
            }
            case STRUCTURE_SENSING, ENVIRONMENTAL_SENSING -> {
                if (trigger == AbilityTrigger.ON_STRUCTURE_SENSE) {
                    yield "structure_awareness";
                }
                if (trigger == AbilityTrigger.ON_WITNESS_EVENT || mechanic == AbilityMechanic.SOCIAL_ATTUNEMENT) {
                    yield "social_awareness";
                }
                yield "terrain_awareness";
            }
            case RITUAL_STRANGE_UTILITY -> {
                if (mechanic == AbilityMechanic.REVENANT_TRIGGER || trigger == AbilityTrigger.ON_MEMORY_EVENT) {
                    yield "lineage_reactive_ritual";
                }
                if (trigger == AbilityTrigger.ON_WORLD_SCAN) {
                    yield "environment_ritual";
                }
                yield "memory_ritual";
            }
            default -> niche.name().toLowerCase() + "_general";
        };
    }
}
