package obtuseloot.evolution;

import obtuseloot.abilities.AbilityMechanic;
import obtuseloot.abilities.AbilityTrigger;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class EcosystemRoleClassifier {
    private final NicheTaxonomy taxonomy;
    private final SubnicheClassifier subnicheClassifier;
    private final RoleDifferentiationHeuristics differentiationHeuristics;

    public EcosystemRoleClassifier() {
        this(new NicheTaxonomy(), new SubnicheClassifier(), new RoleDifferentiationHeuristics());
    }

    public EcosystemRoleClassifier(NicheTaxonomy taxonomy,
                                   SubnicheClassifier subnicheClassifier,
                                   RoleDifferentiationHeuristics differentiationHeuristics) {
        this.taxonomy = taxonomy;
        this.subnicheClassifier = subnicheClassifier;
        this.differentiationHeuristics = differentiationHeuristics;
    }

    public ArtifactNicheProfile classify(Map<String, MechanicUtilitySignal> signals) {
        Map<MechanicNicheTag, Double> nicheScores = new EnumMap<>(MechanicNicheTag.class);
        Map<String, Double> subnicheScores = new LinkedHashMap<>();
        Set<MechanicNicheTag> niches = EnumSet.noneOf(MechanicNicheTag.class);

        signals.forEach((key, signal) -> {
            AbilityMechanic mechanic = parseMechanic(key);
            AbilityTrigger trigger = parseTrigger(key);
            Set<MechanicNicheTag> tags = taxonomy.nichesFor(mechanic, trigger);
            double utilityMass = signal.validatedUtility() + (signal.utilityDensity() * Math.max(1.0D, signal.attempts() * 0.15D));
            for (MechanicNicheTag tag : tags) {
                niches.add(tag);
                nicheScores.merge(tag, utilityMass, Double::sum);
                subnicheScores.merge(subnicheClassifier.classify(tag, mechanic, trigger), utilityMass, Double::sum);
            }
        });

        if (niches.isEmpty()) {
            niches.add(MechanicNicheTag.GENERALIST);
            nicheScores.put(MechanicNicheTag.GENERALIST, 1.0D);
            subnicheScores.put("unspecialized", 1.0D);
        }

        MechanicNicheTag dominant = nicheScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(MechanicNicheTag.GENERALIST);
        NicheSpecializationProfile specialization = differentiationHeuristics.specializationFor(nicheScores, subnicheScores, dominant);
        return new ArtifactNicheProfile(dominant, Set.copyOf(niches), Map.copyOf(nicheScores), specialization);
    }

    private AbilityMechanic parseMechanic(String key) {
        String[] parts = key.split("@");
        if (parts.length == 0) {
            return AbilityMechanic.PULSE;
        }
        try {
            return AbilityMechanic.valueOf(parts[0]);
        } catch (IllegalArgumentException ex) {
            return AbilityMechanic.PULSE;
        }
    }

    private AbilityTrigger parseTrigger(String key) {
        String[] parts = key.split("@");
        if (parts.length < 2) {
            return AbilityTrigger.ON_WORLD_SCAN;
        }
        try {
            return AbilityTrigger.valueOf(parts[1]);
        } catch (IllegalArgumentException ex) {
            return AbilityTrigger.ON_WORLD_SCAN;
        }
    }
}
