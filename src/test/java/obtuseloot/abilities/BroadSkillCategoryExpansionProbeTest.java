package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.lineage.ArtifactLineage;
import obtuseloot.lineage.LineageBiasDimension;
import obtuseloot.lineage.LineageInfluenceResolver;
import obtuseloot.lineage.LineageRegistry;
import obtuseloot.memory.ArtifactMemoryProfile;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BroadSkillCategoryExpansionProbeTest {

    @Test
    void expandedCategoriesAreSampledAcrossNichesAndLineages() {
        AbilityRegistry registry = new AbilityRegistry();
        ProceduralAbilityGenerator baseline = new ProceduralAbilityGenerator(registry);
        LineageRegistry lineageRegistry = new LineageRegistry();
        ProceduralAbilityGenerator lineageGenerator = new ProceduralAbilityGenerator(registry, null, lineageRegistry, new LineageInfluenceResolver());

        Map<String, Map<AbilityCategory, Double>> nicheDistribution = new LinkedHashMap<>();
        nicheDistribution.put("explorer", distributionByCategory(baseline, null, explorerProfile(), 36, 100_000L, null, 0.0D, 0.0D, 0.0D));
        nicheDistribution.put("builder", distributionByCategory(baseline, null, builderProfile(), 36, 200_000L, null, 0.0D, 0.0D, 0.0D));
        nicheDistribution.put("fighter", distributionByCategory(baseline, null, fighterProfile(), 36, 300_000L, null, 0.0D, 0.0D, 0.0D));
        nicheDistribution.put("ritualist", distributionByCategory(baseline, null, ritualProfile(), 36, 400_000L, null, 0.0D, 0.0D, 0.0D));
        nicheDistribution.put("survivor", distributionByCategory(baseline, null, survivalProfile(), 36, 500_000L, null, 0.0D, 0.0D, 0.0D));

        Map<String, Map<AbilityCategory, Double>> lineageDistribution = new LinkedHashMap<>();
        lineageDistribution.put("builder-logistics", distributionByCategory(lineageGenerator, lineageRegistry, builderProfile(), 28, 600_000L, "builder-logistics", 0.10D, -0.08D, -0.02D));
        lineageDistribution.put("builder-ritual", distributionByCategory(lineageGenerator, lineageRegistry, builderProfile(), 28, 700_000L, "builder-ritual", -0.04D, 0.28D, 0.24D));
        lineageDistribution.put("fighter-scout", distributionByCategory(lineageGenerator, lineageRegistry, fighterProfile(), 28, 800_000L, "fighter-scout", 0.26D, -0.06D, 0.10D));
        lineageDistribution.put("fighter-warden", distributionByCategory(lineageGenerator, lineageRegistry, fighterProfile(), 28, 900_000L, "fighter-warden", -0.08D, 0.04D, -0.10D));

        System.out.println("NICHE_CATEGORY_DISTRIBUTION");
        nicheDistribution.forEach((name, dist) -> System.out.println(name + " -> " + summarize(dist)));
        System.out.println("LINEAGE_CATEGORY_DISTRIBUTION");
        lineageDistribution.forEach((name, dist) -> System.out.println(name + " -> " + summarize(dist)));

        Map<AbilityCategory, Double> aggregate = new EnumMap<>(AbilityCategory.class);
        nicheDistribution.values().forEach(dist -> dist.forEach((category, value) -> aggregate.merge(category, value, Double::sum)));
        lineageDistribution.values().forEach(dist -> dist.forEach((category, value) -> aggregate.merge(category, value, Double::sum)));
        aggregate.replaceAll((category, value) -> value / (nicheDistribution.size() + lineageDistribution.size()));

        assertTrue(aggregate.values().stream().filter(v -> v > 0.02D).count() == AbilityCategory.values().length,
                "Every new category should have meaningful usage.");
        assertTrue(jensenShannon(nicheDistribution.get("explorer"), nicheDistribution.get("builder")) >= 0.08D);
        assertTrue(jensenShannon(nicheDistribution.get("fighter"), nicheDistribution.get("ritualist")) >= 0.08D);
        assertTrue(jensenShannon(lineageDistribution.get("builder-logistics"), lineageDistribution.get("builder-ritual")) >= 0.05D);
        assertTrue(jensenShannon(lineageDistribution.get("fighter-scout"), lineageDistribution.get("fighter-warden")) >= 0.05D);
        assertTrue(aggregate.values().stream().noneMatch(v -> v > 0.26D), "No single category should dominate the probe.");
    }

    private Map<AbilityCategory, Double> distributionByCategory(ProceduralAbilityGenerator generator,
                                                                LineageRegistry lineageRegistry,
                                                                ArtifactMemoryProfile memoryProfile,
                                                                int artifacts,
                                                                long seedBase,
                                                                String lineageId,
                                                                double explorationBias,
                                                                double ritualBias,
                                                                double weirdnessBias) {
        Map<AbilityCategory, Integer> counts = new EnumMap<>(AbilityCategory.class);
        int total = 0;
        for (int i = 0; i < artifacts; i++) {
            Artifact artifact = artifact(seedBase + i, lineageId);
            if (lineageRegistry != null) {
                ArtifactLineage lineage = lineageRegistry.assignLineage(artifact);
                lineage.evolutionaryBiasGenome().add(LineageBiasDimension.EXPLORATION_PREFERENCE, explorationBias);
                lineage.evolutionaryBiasGenome().add(LineageBiasDimension.RITUAL_PREFERENCE, ritualBias);
                lineage.evolutionaryBiasGenome().add(LineageBiasDimension.WEIRDNESS, weirdnessBias);
                lineage.evolutionaryBiasGenome().add(LineageBiasDimension.SUPPORT_PREFERENCE, ritualBias < 0 ? 0.22D : 0.08D);
            }
            AbilityProfile profile = generator.generate(artifact, 4, memoryProfile);
            for (AbilityDefinition ability : profile.abilities()) {
                counts.merge(categoryFor(ability.id()), 1, Integer::sum);
                total++;
            }
        }
        Map<AbilityCategory, Double> distribution = new EnumMap<>(AbilityCategory.class);
        int finalTotal = Math.max(1, total);
        for (AbilityCategory category : AbilityCategory.values()) {
            distribution.put(category, counts.getOrDefault(category, 0) / (double) finalTotal);
        }
        return distribution;
    }

    private AbilityCategory categoryFor(String id) {
        return new AbilityRegistry().templates().stream()
                .filter(t -> t.id().equals(id))
                .map(AbilityTemplate::category)
                .findFirst()
                .orElse(AbilityCategory.SENSING_INFORMATION);
    }

    private String summarize(Map<AbilityCategory, Double> distribution) {
        return distribution.entrySet().stream()
                .sorted(Map.Entry.<AbilityCategory, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(4)
                .map(e -> e.getKey().name().toLowerCase() + "=" + String.format("%.3f", e.getValue()))
                .collect(Collectors.joining(", "));
    }

    private double jensenShannon(Map<AbilityCategory, Double> left, Map<AbilityCategory, Double> right) {
        Map<AbilityCategory, Double> mean = new EnumMap<>(AbilityCategory.class);
        for (AbilityCategory category : AbilityCategory.values()) {
            mean.put(category, (left.getOrDefault(category, 0.0D) + right.getOrDefault(category, 0.0D)) / 2.0D);
        }
        return (kl(left, mean) + kl(right, mean)) / 2.0D;
    }

    private double kl(Map<AbilityCategory, Double> distribution, Map<AbilityCategory, Double> mean) {
        double sum = 0.0D;
        for (AbilityCategory category : AbilityCategory.values()) {
            double p = distribution.getOrDefault(category, 0.0D);
            double m = mean.getOrDefault(category, 0.0D);
            if (p > 0.0D && m > 0.0D) {
                sum += p * (Math.log(p / m) / Math.log(2.0D));
            }
        }
        return sum;
    }

    private Artifact artifact(long seed, String lineageId) {
        Artifact artifact = new Artifact(UUID.randomUUID());
        artifact.setArtifactSeed(seed);
        artifact.setArtifactStorageKey("artifact:" + seed);
        artifact.setLatentLineage(lineageId == null ? "common" : lineageId);
        artifact.setSeedPrecisionAffinity(0.48D);
        artifact.setSeedBrutalityAffinity(0.22D);
        artifact.setSeedSurvivalAffinity(0.56D);
        artifact.setSeedMobilityAffinity(0.56D);
        artifact.setSeedChaosAffinity(0.52D);
        artifact.setSeedConsistencyAffinity(0.54D);
        return artifact;
    }

    private ArtifactMemoryProfile explorerProfile() { return new ArtifactMemoryProfile(6, 1.0D, 0.9D, 0.8D, 0.7D, 1.8D, 0.4D, 0.2D); }
    private ArtifactMemoryProfile builderProfile() { return new ArtifactMemoryProfile(7, 1.0D, 1.4D, 0.8D, 1.0D, 0.9D, 0.5D, 0.2D); }
    private ArtifactMemoryProfile fighterProfile() { return new ArtifactMemoryProfile(6, 1.1D, 0.8D, 1.7D, 1.2D, 1.1D, 0.6D, 0.4D); }
    private ArtifactMemoryProfile ritualProfile() { return new ArtifactMemoryProfile(8, 1.2D, 1.0D, 0.8D, 0.8D, 0.9D, 1.5D, 0.5D); }
    private ArtifactMemoryProfile survivalProfile() { return new ArtifactMemoryProfile(8, 1.3D, 0.9D, 0.9D, 1.7D, 0.7D, 0.5D, 0.5D); }
}
