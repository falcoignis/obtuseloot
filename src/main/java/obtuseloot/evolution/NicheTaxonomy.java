package obtuseloot.evolution;

import obtuseloot.abilities.AbilityMechanic;
import obtuseloot.abilities.AbilityTrigger;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class NicheTaxonomy {
    private final Map<AbilityMechanic, Set<MechanicNicheTag>> mechanicToNiches = new EnumMap<>(AbilityMechanic.class);

    public NicheTaxonomy() {
        mechanicToNiches.put(AbilityMechanic.NAVIGATION_ANCHOR, EnumSet.of(MechanicNicheTag.NAVIGATION, MechanicNicheTag.MOBILITY_UTILITY));
        mechanicToNiches.put(AbilityMechanic.SENSE_PING, EnumSet.of(MechanicNicheTag.STRUCTURE_SENSING, MechanicNicheTag.ENVIRONMENTAL_SENSING, MechanicNicheTag.INSPECT_INFORMATION));
        mechanicToNiches.put(AbilityMechanic.INSIGHT_REVEAL, EnumSet.of(MechanicNicheTag.INSPECT_INFORMATION, MechanicNicheTag.MEMORY_HISTORY));
        mechanicToNiches.put(AbilityMechanic.HARVEST_RELAY, EnumSet.of(MechanicNicheTag.FARMING_WORLDKEEPING, MechanicNicheTag.SUPPORT_COHESION));
        mechanicToNiches.put(AbilityMechanic.RITUAL_CHANNEL, EnumSet.of(MechanicNicheTag.RITUAL_STRANGE_UTILITY, MechanicNicheTag.RARE_HIGH_COST_UTILITY));
        mechanicToNiches.put(AbilityMechanic.REVENANT_TRIGGER, EnumSet.of(MechanicNicheTag.RITUAL_STRANGE_UTILITY, MechanicNicheTag.MEMORY_HISTORY, MechanicNicheTag.RARE_HIGH_COST_UTILITY));
        mechanicToNiches.put(AbilityMechanic.SOCIAL_ATTUNEMENT, EnumSet.of(MechanicNicheTag.SOCIAL_WORLD_INTERACTION, MechanicNicheTag.SUPPORT_COHESION));
        mechanicToNiches.put(AbilityMechanic.GUARDIAN_PULSE, EnumSet.of(MechanicNicheTag.PROTECTION_WARDING, MechanicNicheTag.ENVIRONMENTAL_ADAPTATION));
        mechanicToNiches.put(AbilityMechanic.DEFENSIVE_THRESHOLD, EnumSet.of(MechanicNicheTag.PROTECTION_WARDING, MechanicNicheTag.ENVIRONMENTAL_ADAPTATION));
        mechanicToNiches.put(AbilityMechanic.RECOVERY_WINDOW, EnumSet.of(MechanicNicheTag.SUPPORT_COHESION, MechanicNicheTag.ENVIRONMENTAL_ADAPTATION));
        mechanicToNiches.put(AbilityMechanic.MOVEMENT_ECHO, EnumSet.of(MechanicNicheTag.MOBILITY_UTILITY, MechanicNicheTag.NAVIGATION));
        mechanicToNiches.put(AbilityMechanic.MEMORY_ECHO, EnumSet.of(MechanicNicheTag.MEMORY_HISTORY, MechanicNicheTag.INSPECT_INFORMATION));
        mechanicToNiches.put(AbilityMechanic.BATTLEFIELD_FIELD, EnumSet.of(MechanicNicheTag.SUPPORT_COHESION, MechanicNicheTag.PROTECTION_WARDING));
        mechanicToNiches.put(AbilityMechanic.PULSE, EnumSet.of(MechanicNicheTag.ENVIRONMENTAL_SENSING));
        mechanicToNiches.put(AbilityMechanic.MARK, EnumSet.of(MechanicNicheTag.INSPECT_INFORMATION));
        mechanicToNiches.put(AbilityMechanic.CHAIN_ESCALATION, EnumSet.of(MechanicNicheTag.SUPPORT_COHESION, MechanicNicheTag.SOCIAL_WORLD_INTERACTION));
        mechanicToNiches.put(AbilityMechanic.BURST_STATE, EnumSet.of(MechanicNicheTag.ENVIRONMENTAL_ADAPTATION));
        mechanicToNiches.put(AbilityMechanic.RETALIATION, EnumSet.of(MechanicNicheTag.PROTECTION_WARDING));
        mechanicToNiches.put(AbilityMechanic.UNSTABLE_DETONATION, EnumSet.of(MechanicNicheTag.RARE_HIGH_COST_UTILITY, MechanicNicheTag.RITUAL_STRANGE_UTILITY));
        mechanicToNiches.put(AbilityMechanic.ECOLOGICAL_PATHING, EnumSet.of(MechanicNicheTag.NAVIGATION, MechanicNicheTag.ENVIRONMENTAL_SENSING));
        mechanicToNiches.put(AbilityMechanic.BIOME_RESONANCE, EnumSet.of(MechanicNicheTag.ENVIRONMENTAL_SENSING, MechanicNicheTag.INSPECT_INFORMATION));
        mechanicToNiches.put(AbilityMechanic.CARTOGRAPHIC_ECHO, EnumSet.of(MechanicNicheTag.NAVIGATION, MechanicNicheTag.STRUCTURE_SENSING));
        mechanicToNiches.put(AbilityMechanic.FORAGING_MEMORY, EnumSet.of(MechanicNicheTag.FARMING_WORLDKEEPING, MechanicNicheTag.MEMORY_HISTORY));
        mechanicToNiches.put(AbilityMechanic.RESOURCE_ECOLOGY_SCAN, EnumSet.of(MechanicNicheTag.INSPECT_INFORMATION, MechanicNicheTag.ENVIRONMENTAL_SENSING));
        mechanicToNiches.put(AbilityMechanic.CLUSTER_INTUITION, EnumSet.of(MechanicNicheTag.FARMING_WORLDKEEPING, MechanicNicheTag.NAVIGATION));
        mechanicToNiches.put(AbilityMechanic.RITUAL_STABILIZATION, EnumSet.of(MechanicNicheTag.RITUAL_STRANGE_UTILITY, MechanicNicheTag.SUPPORT_COHESION));
        mechanicToNiches.put(AbilityMechanic.ALTAR_SIGNAL_BOOST, EnumSet.of(MechanicNicheTag.RITUAL_STRANGE_UTILITY, MechanicNicheTag.MEMORY_HISTORY));
        mechanicToNiches.put(AbilityMechanic.TEMPORAL_SPECIALIZATION, EnumSet.of(MechanicNicheTag.ENVIRONMENTAL_ADAPTATION, MechanicNicheTag.RITUAL_STRANGE_UTILITY));
        mechanicToNiches.put(AbilityMechanic.WITNESS_IMPRINT, EnumSet.of(MechanicNicheTag.SOCIAL_WORLD_INTERACTION, MechanicNicheTag.MEMORY_HISTORY));
        mechanicToNiches.put(AbilityMechanic.COLLECTIVE_RELAY, EnumSet.of(MechanicNicheTag.SOCIAL_WORLD_INTERACTION, MechanicNicheTag.SUPPORT_COHESION));
        mechanicToNiches.put(AbilityMechanic.TRADE_SCENT, EnumSet.of(MechanicNicheTag.SOCIAL_WORLD_INTERACTION, MechanicNicheTag.INSPECT_INFORMATION));
        mechanicToNiches.put(AbilityMechanic.WEATHER_ATTUNEMENT, EnumSet.of(MechanicNicheTag.ENVIRONMENTAL_ADAPTATION, MechanicNicheTag.ENVIRONMENTAL_SENSING));
        mechanicToNiches.put(AbilityMechanic.STRUCTURE_ATTUNEMENT, EnumSet.of(MechanicNicheTag.STRUCTURE_SENSING, MechanicNicheTag.ENVIRONMENTAL_SENSING));
        mechanicToNiches.put(AbilityMechanic.TERRAIN_ADAPTATION, EnumSet.of(MechanicNicheTag.MOBILITY_UTILITY, MechanicNicheTag.ENVIRONMENTAL_ADAPTATION));
    }

    public Set<MechanicNicheTag> nichesFor(AbilityMechanic mechanic, AbilityTrigger trigger) {
        EnumSet<MechanicNicheTag> tags = EnumSet.copyOf(mechanicToNiches.getOrDefault(mechanic, EnumSet.of(MechanicNicheTag.GENERALIST)));
        if (trigger == AbilityTrigger.ON_WORLD_SCAN || trigger == AbilityTrigger.ON_STRUCTURE_SENSE
                || trigger == AbilityTrigger.ON_CHUNK_ENTER || trigger == AbilityTrigger.ON_STRUCTURE_DISCOVERY
                || trigger == AbilityTrigger.ON_STRUCTURE_PROXIMITY) {
            tags.add(MechanicNicheTag.ENVIRONMENTAL_SENSING);
        }
        if (trigger == AbilityTrigger.ON_BLOCK_INSPECT) {
            tags.add(MechanicNicheTag.INSPECT_INFORMATION);
        }
        if (trigger == AbilityTrigger.ON_MEMORY_EVENT || trigger == AbilityTrigger.ON_PLAYER_WITNESS) {
            tags.add(MechanicNicheTag.MEMORY_HISTORY);
        }
        return tags;
    }
}
