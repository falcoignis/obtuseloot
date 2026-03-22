package obtuseloot.abilities;

import java.util.ArrayList;
import java.util.List;

public final class RegulatoryEligibilityFilter {
    private static final double MIN_ELIGIBILITY = 0.25D;

    public List<AbilityTemplate> filter(List<AbilityTemplate> candidates, AbilityRegulatoryProfile profile) {
        List<AbilityTemplate> eligible = new ArrayList<>();
        for (AbilityTemplate template : candidates) {
            if (eligibilityWeight(template, profile) >= MIN_ELIGIBILITY) {
                eligible.add(template);
            }
        }
        return eligible;
    }

    public double eligibilityWeight(AbilityTemplate template, AbilityRegulatoryProfile profile) {
        double weight = 1.0D;
        weight *= gateWeight(profile, RegulatoryGate.RESONANCE,
                template.family() == AbilityFamily.PRECISION || template.family() == AbilityFamily.CONSISTENCY,
                template.mechanic() == AbilityMechanic.MEMORY_ECHO || template.mechanic() == AbilityMechanic.INSIGHT_REVEAL);
        weight *= gateWeight(profile, RegulatoryGate.VOLATILITY,
                template.family() == AbilityFamily.CHAOS || template.family() == AbilityFamily.BRUTALITY,
                template.mechanic() == AbilityMechanic.REVENANT_TRIGGER || template.trigger() == AbilityTrigger.ON_WITNESS_EVENT);
        weight *= gateWeight(profile, RegulatoryGate.MEMORY,
                template.trigger() == AbilityTrigger.ON_MEMORY_EVENT,
                template.mechanic() == AbilityMechanic.MEMORY_ECHO || template.mechanic() == AbilityMechanic.INSIGHT_REVEAL || template.mechanic() == AbilityMechanic.RECOVERY_WINDOW);
        weight *= gateWeight(profile, RegulatoryGate.ENVIRONMENT,
                template.trigger() == AbilityTrigger.ON_STRUCTURE_SENSE || template.trigger() == AbilityTrigger.ON_WORLD_SCAN,
                template.trigger() == AbilityTrigger.ON_REPOSITION);
        weight *= gateWeight(profile, RegulatoryGate.MOBILITY,
                template.family() == AbilityFamily.MOBILITY,
                template.trigger() == AbilityTrigger.ON_MOVEMENT || template.trigger() == AbilityTrigger.ON_REPOSITION || template.trigger() == AbilityTrigger.ON_WORLD_SCAN);
        weight *= gateWeight(profile, RegulatoryGate.SURVIVAL,
                template.family() == AbilityFamily.SURVIVAL,
                template.trigger() == AbilityTrigger.ON_BLOCK_HARVEST || template.mechanic() == AbilityMechanic.HARVEST_RELAY || template.mechanic() == AbilityMechanic.RITUAL_CHANNEL);
        weight *= gateWeight(profile, RegulatoryGate.DISCIPLINE,
                template.family() == AbilityFamily.CONSISTENCY || template.family() == AbilityFamily.PRECISION,
                template.mechanic() == AbilityMechanic.INSIGHT_REVEAL || template.mechanic() == AbilityMechanic.NAVIGATION_ANCHOR);
        weight *= gateWeight(profile, RegulatoryGate.LINEAGE_MILESTONE,
                template.trigger() == AbilityTrigger.ON_RITUAL_INTERACT || template.trigger() == AbilityTrigger.ON_MEMORY_EVENT,
                template.trigger() == AbilityTrigger.ON_STRUCTURE_SENSE || template.mechanic() == AbilityMechanic.REVENANT_TRIGGER);
        return weight;
    }

    private double gateWeight(AbilityRegulatoryProfile profile, RegulatoryGate gate, boolean primaryMatch, boolean secondaryMatch) {
        if (!primaryMatch && !secondaryMatch) {
            return 1.0D;
        }
        if (profile.isOpen(gate)) {
            return primaryMatch ? 1.22D : 1.10D;
        }
        return primaryMatch ? 0.58D : 0.78D;
    }
}
