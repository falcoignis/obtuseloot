package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.evolution.MechanicNicheTag;
import obtuseloot.lineage.ArtifactLineage;
import obtuseloot.lineage.LineageBiasDimension;
import obtuseloot.lineage.LineageInfluenceResolver;
import obtuseloot.lineage.LineageRegistry;
import obtuseloot.memory.ArtifactMemoryProfile;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AbilityNoveltyTuningProbeTest {

    @Test
    void noveltyTuningProbeReportsHealthyNoveltyWithoutDivergenceRegression() {
        AbilityRegistry registry = new AbilityRegistry();
        ProceduralAbilityGenerator baselineGenerator = new ProceduralAbilityGenerator(registry);

        Map<String, Double> nicheDivergence = Map.of(
                "explorer_vs_ritualist", jensenShannonFamilies(familyDistribution(baselineGenerator, explorerProfile(), 60, 100_000L),
                        familyDistribution(baselineGenerator, ritualProfile(), 60, 200_000L)),
                "explorer_vs_warden", jensenShannonFamilies(familyDistribution(baselineGenerator, explorerProfile(), 60, 100_000L),
                        familyDistribution(baselineGenerator, wardenProfile(), 60, 300_000L)),
                "ritualist_vs_warden", jensenShannonFamilies(familyDistribution(baselineGenerator, ritualProfile(), 60, 200_000L),
                        familyDistribution(baselineGenerator, wardenProfile(), 60, 300_000L))
        );
        LineageRegistry lineageRegistry = new LineageRegistry();
        LineageInfluenceResolver lineageResolver = new LineageInfluenceResolver();
        ProceduralAbilityGenerator lineageGenerator = new ProceduralAbilityGenerator(registry, null, lineageRegistry, lineageResolver);
        Map<AbilityMechanic, Double> lineageA = mechanicDistribution(lineageGenerator, lineageRegistry, ritualProfile(), 60, 400_000L, "ritual-lineage-a", 0.32D, 0.10D, 0.05D);
        Map<AbilityMechanic, Double> lineageB = mechanicDistribution(lineageGenerator, lineageRegistry, ritualProfile(), 60, 500_000L, "ritual-lineage-b", -0.04D, 0.30D, 0.28D);
        double lineageDivergence = jensenShannonMechanics(lineageA, lineageB);

        NoveltyProbeResult novelty = noveltyProbe(new ProceduralAbilityGenerator(registry), 180, 600_000L);

        System.out.printf("NICHE_DIVERGENCE explorer_vs_ritualist=%.4f explorer_vs_warden=%.4f ritualist_vs_warden=%.4f%n",
                nicheDivergence.get("explorer_vs_ritualist"),
                nicheDivergence.get("explorer_vs_warden"),
                nicheDivergence.get("ritualist_vs_warden"));
        System.out.printf("LINEAGE_DIVERGENCE ritual_same_niche=%.4f%n", lineageDivergence);
        System.out.printf("NOVELTY_METRICS avg_novelty=%.4f min_novelty=%.4f max_novelty=%.4f avg_similarity=%.4f intra_novelty=%.4f global_novelty=%.4f intra_similarity=%.4f global_similarity=%.4f%n",
                novelty.averageNovelty(),
                novelty.minimumNovelty(),
                novelty.maximumNovelty(),
                novelty.averageSimilarity(),
                novelty.averageIntraNicheNovelty(),
                novelty.averageGlobalNovelty(),
                novelty.averageIntraNicheSimilarity(),
                novelty.averageGlobalSimilarity());

        assertTrue(novelty.averageNovelty() >= 0.17D,
                "Average novelty should remain materially above the pre-tuning baseline.");
        assertTrue(novelty.averageSimilarity() < 0.83D,
                "Average similarity should remain below the regressed global-pressure baseline.");
        assertTrue(novelty.averageIntraNicheNovelty() > novelty.averageGlobalNovelty(),
                "Intra-niche novelty should dominate cross-niche novelty pressure.");
        assertTrue(nicheDivergence.get("explorer_vs_ritualist") >= 0.17D
                        || nicheDivergence.get("explorer_vs_warden") >= 0.17D
                        || nicheDivergence.get("ritualist_vs_warden") >= 0.17D,
                "At least one niche pair should recover meaningful separation after novelty gating.");
        assertTrue(lineageDivergence >= 0.38D,
                "Lineage divergence should remain at or above the current healthy range.");
    }

    private Map<AbilityMechanic, Double> mechanicDistribution(ProceduralAbilityGenerator generator,
                                                              ArtifactMemoryProfile memoryProfile,
                                                              int artifacts,
                                                              long seedBase) {
        return mechanicDistribution(generator, null, memoryProfile, artifacts, seedBase, null, 0.0D, 0.0D, 0.0D);
    }

    private Map<AbilityMechanic, Double> mechanicDistribution(ProceduralAbilityGenerator generator,
                                                              LineageRegistry lineageRegistry,
                                                              ArtifactMemoryProfile memoryProfile,
                                                              int artifacts,
                                                              long seedBase,
                                                              String lineageId,
                                                              double explorationBias,
                                                              double ritualBias,
                                                              double weirdnessBias) {
        Map<AbilityMechanic, Integer> counts = new EnumMap<>(AbilityMechanic.class);
        int total = 0;
        for (int i = 0; i < artifacts; i++) {
            Artifact artifact = artifact(seedBase + i, lineageId);
            if (lineageId != null) {
                ArtifactLineage lineage = lineageRegistry.assignLineage(artifact);
                lineage.evolutionaryBiasGenome().add(LineageBiasDimension.EXPLORATION_PREFERENCE, explorationBias);
                lineage.evolutionaryBiasGenome().add(LineageBiasDimension.RITUAL_PREFERENCE, ritualBias);
                lineage.evolutionaryBiasGenome().add(LineageBiasDimension.WEIRDNESS, weirdnessBias);
            }
            AbilityProfile profile = generator.generate(artifact, 4, memoryProfile);
            for (AbilityDefinition ability : profile.abilities()) {
                counts.merge(ability.mechanic(), 1, Integer::sum);
                total++;
            }
        }
        int finalTotal = Math.max(1, total);
        Map<AbilityMechanic, Double> distribution = new EnumMap<>(AbilityMechanic.class);
        counts.forEach((mechanic, count) -> distribution.put(mechanic, count / (double) finalTotal));
        return distribution;
    }

    private Map<AbilityFamily, Double> familyDistribution(ProceduralAbilityGenerator generator,
                                                          ArtifactMemoryProfile memoryProfile,
                                                          int artifacts,
                                                          long seedBase) {
        Map<AbilityFamily, Integer> counts = new EnumMap<>(AbilityFamily.class);
        int total = 0;
        for (int i = 0; i < artifacts; i++) {
            AbilityProfile profile = generator.generate(artifact(seedBase + i, null), 4, memoryProfile);
            for (AbilityDefinition ability : profile.abilities()) {
                counts.merge(ability.family(), 1, Integer::sum);
                total++;
            }
        }
        int finalTotal = Math.max(1, total);
        Map<AbilityFamily, Double> distribution = new EnumMap<>(AbilityFamily.class);
        counts.forEach((family, count) -> distribution.put(family, count / (double) finalTotal));
        return distribution;
    }

    private NoveltyProbeResult noveltyProbe(ProceduralAbilityGenerator generator, int artifacts, long seedBase) {
        AbilityDiversityIndex diversityIndex = AbilityDiversityIndex.instance();
        List<Double> noveltyScores = new ArrayList<>();
        List<Double> similarityScores = new ArrayList<>();
        List<Double> intraNoveltyScores = new ArrayList<>();
        List<Double> globalNoveltyScores = new ArrayList<>();
        List<Double> intraSimilarityScores = new ArrayList<>();
        List<Double> globalSimilarityScores = new ArrayList<>();

        for (int i = 0; i < artifacts; i++) {
            Artifact artifact = artifact(seedBase + i, null);
            ArtifactMemoryProfile memoryProfile = switch (i % 3) {
                case 0 -> explorerProfile();
                case 1 -> ritualProfile();
                default -> wardenProfile();
            };

            List<AbilityDiversityIndex.AbilitySignature> activePoolBefore = diversityIndex.activePool(artifact.getArtifactSeed());
            AbilityProfile profile = generator.generate(artifact, 4, memoryProfile);
            MechanicNicheTag dominantNiche = dominantNiche(profile);
            for (AbilityDefinition ability : profile.abilities()) {
                AbilityDiversityIndex.AbilitySignature candidate = diversityIndex.fromDefinition(
                        artifact.getArtifactSeed(),
                        artifact.getLatentLineage(),
                        dominantNiche,
                        null,
                        ability);
                double nearestSameNiche = activePoolBefore.stream()
                        .filter(existing -> existing.niche() == dominantNiche)
                        .map(existing -> AbilityDiversityIndex.similarity(candidate, existing))
                        .max(Comparator.naturalOrder())
                        .orElse(0.0D);
                double nearestGlobal = activePoolBefore.stream()
                        .map(existing -> AbilityDiversityIndex.similarity(candidate, existing))
                        .max(Comparator.naturalOrder())
                        .orElse(0.0D);
                noveltyScores.add(1.0D - nearestGlobal);
                similarityScores.add(nearestGlobal);
                intraNoveltyScores.add(1.0D - nearestSameNiche);
                globalNoveltyScores.add(1.0D - nearestGlobal);
                intraSimilarityScores.add(nearestSameNiche);
                globalSimilarityScores.add(nearestGlobal);
            }
        }

        return new NoveltyProbeResult(
                noveltyScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D),
                noveltyScores.stream().mapToDouble(Double::doubleValue).min().orElse(0.0D),
                noveltyScores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0D),
                similarityScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D),
                intraNoveltyScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D),
                globalNoveltyScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D),
                intraSimilarityScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D),
                globalSimilarityScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D)
        );
    }

    private MechanicNicheTag dominantNiche(AbilityProfile profile) {
        Map<MechanicNicheTag, Integer> counts = new EnumMap<>(MechanicNicheTag.class);
        for (AbilityDefinition ability : profile.abilities()) {
            for (MechanicNicheTag tag : new obtuseloot.evolution.NicheTaxonomy().nichesFor(ability.mechanic(), ability.trigger())) {
                counts.merge(tag, 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(MechanicNicheTag.GENERALIST);
    }

    private double jensenShannonMechanics(Map<AbilityMechanic, Double> left, Map<AbilityMechanic, Double> right) {
        return jensenShannonGeneric(left, right, AbilityMechanic.values());
    }

    private double jensenShannonFamilies(Map<AbilityFamily, Double> left, Map<AbilityFamily, Double> right) {
        return jensenShannonGeneric(left, right, AbilityFamily.values());
    }

    private <T> double jensenShannonGeneric(Map<T, Double> left, Map<T, Double> right, T[] keys) {
        Map<T, Double> mean = new HashMap<>();
        for (T key : keys) {
            double a = left.getOrDefault(key, 0.0D);
            double b = right.getOrDefault(key, 0.0D);
            mean.put(key, (a + b) / 2.0D);
        }
        return (klGeneric(left, mean, keys) + klGeneric(right, mean, keys)) / 2.0D;
    }

    private <T> double klGeneric(Map<T, Double> distribution, Map<T, Double> mean, T[] keys) {
        double sum = 0.0D;
        for (T key : keys) {
            double p = distribution.getOrDefault(key, 0.0D);
            double m = mean.getOrDefault(key, 0.0D);
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
        artifact.setLatentLineage(lineageId == null ? "unassigned" : lineageId);
        artifact.setSeedPrecisionAffinity(0.45D);
        artifact.setSeedBrutalityAffinity(0.15D);
        artifact.setSeedSurvivalAffinity(0.55D);
        artifact.setSeedMobilityAffinity(0.55D);
        artifact.setSeedChaosAffinity(0.55D);
        artifact.setSeedConsistencyAffinity(0.50D);
        return artifact;
    }

    private ArtifactMemoryProfile explorerProfile() {
        return new ArtifactMemoryProfile(7, 1.1D, 0.9D, 0.8D, 0.7D, 1.7D, 0.4D, 0.3D);
    }

    private ArtifactMemoryProfile ritualProfile() {
        return new ArtifactMemoryProfile(8, 1.2D, 1.1D, 0.7D, 0.8D, 0.9D, 1.3D, 0.4D);
    }

    private ArtifactMemoryProfile wardenProfile() {
        return new ArtifactMemoryProfile(6, 0.8D, 0.9D, 0.7D, 1.6D, 0.8D, 0.5D, 0.3D);
    }

    private record NoveltyProbeResult(double averageNovelty,
                                      double minimumNovelty,
                                      double maximumNovelty,
                                      double averageSimilarity,
                                      double averageIntraNicheNovelty,
                                      double averageGlobalNovelty,
                                      double averageIntraNicheSimilarity,
                                      double averageGlobalSimilarity) {
    }
}
