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
                if (mechanic == AbilityMechanic.TRAIL_SENSE) {
                    yield "frontier_pathfinding";
                }
                if (mechanic == AbilityMechanic.CARTOGRAPHERS_ECHO || trigger == AbilityTrigger.ON_STRUCTURE_DISCOVERY) {
                    yield "structure_chain_discovery";
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
                if (trigger == AbilityTrigger.ON_RITUAL_COMPLETION) {
                    yield "completion_ritual";
                }
                if (mechanic == AbilityMechanic.PATTERN_RESONANCE || trigger == AbilityTrigger.ON_REPEATED_BLOCK_PATTERN) {
                    yield "pattern_repeat_ritual";
                }
                if (trigger == AbilityTrigger.ON_WORLD_SCAN) {
                    yield "environment_ritual";
                }
                yield "memory_ritual";
            }
            case FARMING_WORLDKEEPING -> (mechanic == AbilityMechanic.FORAGER_MEMORY || trigger == AbilityTrigger.ON_RESOURCE_HARVEST_STREAK) ? "harvest_chain_cluster" : (trigger == AbilityTrigger.ON_BLOCK_HARVEST ? "crop_cycle" : "resource_gathering");
            case SOCIAL_WORLD_INTERACTION -> {
                if (trigger == AbilityTrigger.ON_PLAYER_GROUP_ACTION) {
                    yield "group_coordination";
                }
                if (trigger == AbilityTrigger.ON_PLAYER_TRADE) {
                    yield "trade_coordination";
                }
                if (mechanic == AbilityMechanic.WITNESS_IMPRINT || trigger == AbilityTrigger.ON_PLAYER_WITNESS) {
                    yield "witness_memory";
                }
                yield "social_presence";
            }
            case ENVIRONMENTAL_ADAPTATION -> {
                if (trigger == AbilityTrigger.ON_WEATHER_CHANGE) {
                    yield "weather_adaptation";
                }
                if (trigger == AbilityTrigger.ON_ELEVATION_CHANGE) {
                    yield "terrain_adaptation";
                }
                yield "ambient_adaptation";
            }
            case INSPECT_INFORMATION -> trigger == AbilityTrigger.ON_BLOCK_INSPECT ? "material_inspection" : "world_interpretation";
            default -> niche.name().toLowerCase() + "_general";
        };
    }
}
