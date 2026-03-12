package obtuseloot.lineage;

import obtuseloot.abilities.AbilityMetadata;

import java.util.LinkedHashMap;
import java.util.Map;

public class LineageInfluenceResolver {
    public double resolveFamilyInfluence(ArtifactLineage lineage, String family) {
        if (lineage == null) {
            return 1.0D;
        }
        double trait = lineage.lineageTraits().getOrDefault(family.toLowerCase(), 0.0D);
        double bias = switch (family.toLowerCase()) {
            case "precision", "consistency" -> lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.RELIABILITY);
            case "mobility" -> lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.EXPLORATION_PREFERENCE);
            case "survival" -> lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.SUPPORT_PREFERENCE);
            case "chaos" -> lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.WEIRDNESS);
            case "brutality" -> lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.RISK_APPETITE);
            default -> 0.0D;
        };
        return clamp(1.0D + trait + (bias * 0.45D));
    }

    public double resolveMutationInfluence(ArtifactLineage lineage) {
        if (lineage == null) {
            return 1.0D;
        }
        double mutationBias = lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.WEIRDNESS)
                + lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.RARITY_APPETITE);
        return clamp(1.0D + lineage.lineageTraits().getOrDefault("mutation", 0.0D) + (mutationBias * 0.25D));
    }

    public double resolveTemplateInfluence(ArtifactLineage lineage, AbilityMetadata metadata) {
        if (lineage == null || metadata == null) {
            return 1.0D;
        }
        EvolutionaryBiasGenome biasGenome = lineage.evolutionaryBiasGenome();
        double memoryBias = metadata.hasAffinity("memory") ? biasGenome.tendency(LineageBiasDimension.MEMORY_REACTIVITY) * 0.45D : 0.0D;
        double ritualBias = metadata.utilityDomains().contains("ritual-utility") ? biasGenome.tendency(LineageBiasDimension.RITUAL_PREFERENCE) * 0.40D : 0.0D;
        double supportBias = metadata.affinities().contains("support") ? biasGenome.tendency(LineageBiasDimension.SUPPORT_PREFERENCE) * 0.35D : 0.0D;
        double explorationBias = metadata.affinities().contains("exploration") ? biasGenome.tendency(LineageBiasDimension.EXPLORATION_PREFERENCE) * 0.35D : 0.0D;
        double utilityDensityBias = biasGenome.tendency(LineageBiasDimension.UTILITY_DENSITY_PREFERENCE) * (metadata.triggerEfficiency() - 1.0D) * 0.18D;
        return clamp(1.0D + memoryBias + ritualBias + supportBias + explorationBias + utilityDensityBias);
    }

    public double resolveEcologicalCorrection(ArtifactLineage lineage, double ecologyModifier) {
        if (lineage == null) {
            return 1.0D;
        }
        double sensitivity = lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.ENVIRONMENTAL_SENSITIVITY);
        double correction = (ecologyModifier - 1.0D) * (0.55D + (sensitivity * 0.35D));
        return clamp(1.0D + correction);
    }

    public double resolveDriftWindow(ArtifactLineage lineage) {
        if (lineage == null) {
            return 0.035D;
        }
        double reliability = lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.RELIABILITY);
        double weirdness = lineage.evolutionaryBiasGenome().tendency(LineageBiasDimension.WEIRDNESS);
        return 0.03D + Math.max(0.0D, weirdness * 0.04D) + Math.max(0.0D, -reliability * 0.03D);
    }

    public Map<String, Double> traitSnapshot(ArtifactLineage lineage) {
        if (lineage == null) {
            return Map.of();
        }
        Map<String, Double> snapshot = new LinkedHashMap<>(lineage.lineageTraits());
        lineage.evolutionaryBiasGenome().tendencies().forEach((dimension, value) -> snapshot.put("bias." + dimension.name().toLowerCase(), value));
        return Map.copyOf(snapshot);
    }

    private double clamp(double value) {
        return Math.max(0.72D, Math.min(1.28D, value));
    }
}
