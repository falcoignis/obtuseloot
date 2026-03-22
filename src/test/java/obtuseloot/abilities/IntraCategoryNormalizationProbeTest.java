package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.evolution.AdaptiveSupportAllocation;
import obtuseloot.evolution.ArtifactNicheProfile;
import obtuseloot.evolution.NicheVariantProfile;
import obtuseloot.evolution.UtilityHistoryRollup;
import obtuseloot.memory.ArtifactMemoryProfile;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntraCategoryNormalizationProbeTest {

    @Test
    void smallCategoryNormalizationKeepsStealthReachableWithoutRunawayLeader() {
        AbilityRegistry registry = new AbilityRegistry();
        ProceduralAbilityGenerator generator = new ProceduralAbilityGenerator(registry);

        DistributionProbe stealth = probeCategory(generator, registry, AbilityCategory.STEALTH_TRICKERY_DISRUPTION, stealthProfile(), 80, 910_000L);
        DistributionProbe survival = probeCategory(new ProceduralAbilityGenerator(registry), registry, AbilityCategory.SURVIVAL_ADAPTATION, survivalProfile(), 80, 920_000L);
        DistributionProbe sensing = probeCategory(new ProceduralAbilityGenerator(registry), registry, AbilityCategory.SENSING_INFORMATION, sensingProfile(), 100, 930_000L);

        System.out.println("STEALTH_NORMALIZATION_PROBE hits=" + stealth.totalHits + " dist=" + summarize(stealth.distribution));
        System.out.println("SURVIVAL_NORMALIZATION_PROBE hits=" + survival.totalHits + " dist=" + summarize(survival.distribution));
        System.out.println("SENSING_NORMALIZATION_PROBE hits=" + sensing.totalHits + " dist=" + summarize(sensing.distribution));

        assertTrue(stealth.totalHits >= 25, "Stealth probe should generate enough observations for reachability auditing.");
        assertEquals(categoryTemplateCount(registry, AbilityCategory.STEALTH_TRICKERY_DISRUPTION), stealth.distribution.size(),
                "All stealth templates must remain reachable.");
        assertTrue(stealth.topShare() < 0.50D, "Stealth normalization must prevent single-template dominance above 50%.");
        assertTrue(stealth.topThreeShare() < 0.80D, "Stealth top-3 share must leave meaningful probability mass for tail templates.");

        assertTrue(survival.totalHits >= 25, "Survival probe should remain active.");
        assertTrue(survival.topThreeShare() < 0.66D, "Survival top-3 should remain below 66%.");

        assertTrue(sensing.totalHits >= 18, "Sensing probe should remain active.");
        assertTrue(sensing.topShare() <= 0.35D, "Sensing should remain balanced without a single runaway leader.");
        assertTrue(sensing.topThreeShare() < 0.65D, "Sensing should retain balanced spread.");
    }


    @Test
    void underSampledApplicabilityStaysBoundedAndTargetsComplexTriggerFamilies() {
        AbilityRegistry registry = new AbilityRegistry();
        ProceduralAbilityGenerator generator = new ProceduralAbilityGenerator(registry);

        try {
            Method underSampledApplicabilityBoost = ProceduralAbilityGenerator.class.getDeclaredMethod(
                    "underSampledApplicabilityBoost", AbilityTemplate.class, ArtifactNicheProfile.class, ArtifactMemoryProfile.class);
            Method adjacentTriggerFamily = ProceduralAbilityGenerator.class.getDeclaredMethod(
                    "adjacentTriggerFamily", AbilityTrigger.class);

            underSampledApplicabilityBoost.setAccessible(true);
            adjacentTriggerFamily.setAccessible(true);

            AbilityTemplate traceFold = registry.templates().stream().filter(template -> template.id().equals("stealth.trace_fold")).findFirst().orElseThrow();
            AbilityTemplate hushwire = registry.templates().stream().filter(template -> template.id().equals("stealth.hushwire")).findFirst().orElseThrow();
            ArtifactMemoryProfile stealthProfile = stealthProfile();

            double traceFoldBoost = (double) underSampledApplicabilityBoost.invoke(generator, traceFold, null, stealthProfile);
            double hushwireBoost = (double) underSampledApplicabilityBoost.invoke(generator, hushwire, null, stealthProfile);
            @SuppressWarnings("unchecked")
            java.util.Set<AbilityTrigger> traceFoldFamily = (java.util.Set<AbilityTrigger>) adjacentTriggerFamily.invoke(generator, AbilityTrigger.ON_BLOCK_INSPECT);

            assertTrue(traceFoldBoost > 1.0D, "Under-sampled complex templates should receive a bounded applicability lift.");
            assertTrue(traceFoldBoost <= 1.16D, "Applicability expansion must remain capped.");
            assertTrue(hushwireBoost > 1.0D, "Structure-proximity stealth templates should also gain bounded reachability help.");
            assertTrue(traceFoldFamily.contains(AbilityTrigger.ON_STRUCTURE_SENSE), "Inspect-family expansion should admit nearby structure-reading triggers.");
            assertTrue(traceFoldFamily.contains(AbilityTrigger.ON_STRUCTURE_DISCOVERY), "Inspect-family expansion should admit nearby structure-discovery triggers.");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to inspect under-sampled applicability helpers.", e);
        }
    }

    @Test
    void conditionalCompressionOnlyTriggersForHighTopThreeCategories() {
        AbilityRegistry registry = new AbilityRegistry();
        ProceduralAbilityGenerator generator = new ProceduralAbilityGenerator(registry);

        WeightedCategoryProbe crafting = probeNormalizedWeights(generator, registry,
                AbilityCategory.CRAFTING_ENGINEERING_AUTOMATION, artifact(940_001L), craftingProfile());
        WeightedCategoryProbe traversal = probeNormalizedWeights(generator, registry,
                AbilityCategory.TRAVERSAL_MOBILITY, artifact(950_001L), traversalProfile());
        WeightedCategoryProbe ritual = probeNormalizedWeights(generator, registry,
                AbilityCategory.RITUAL_STRANGE_UTILITY, artifact(960_001L), ritualProfile());

        System.out.println("CRAFTING_WEIGHT_COMPRESSION_PROBE top3=" + String.format("%.3f", crafting.topThreeShare()) + " weights=" + summarizeWeights(crafting.weights()));
        System.out.println("TRAVERSAL_WEIGHT_COMPRESSION_PROBE top3=" + String.format("%.3f", traversal.topThreeShare()) + " weights=" + summarizeWeights(traversal.weights()));
        System.out.println("RITUAL_WEIGHT_COMPRESSION_PROBE top3=" + String.format("%.3f", ritual.topThreeShare()) + " weights=" + summarizeWeights(ritual.weights()));

        assertTrue(crafting.topThreeShare() < 0.70D, "Crafting top-3 should be compressed below the threshold.");
        assertTrue(traversal.topThreeShare() < 0.70D, "Traversal top-3 should be compressed below the threshold.");
        assertTrue(ritual.topThreeShare() < 0.55D, "Healthy ritual spread should remain stable.");
    }

    private DistributionProbe probeCategory(ProceduralAbilityGenerator generator,
                                            AbilityRegistry registry,
                                            AbilityCategory category,
                                            ArtifactMemoryProfile memoryProfile,
                                            int artifacts,
                                            long seedBase) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        int hits = 0;
        for (int i = 0; i < artifacts; i++) {
            AbilityProfile profile = generator.generate(artifact(seedBase + i), 4, memoryProfile);
            for (AbilityDefinition ability : profile.abilities()) {
                AbilityCategory actualCategory = categoryFor(registry, ability.id());
                if (actualCategory != category) {
                    continue;
                }
                counts.merge(ability.id(), 1, Integer::sum);
                hits++;
            }
        }
        return new DistributionProbe(counts, hits);
    }

    private int categoryTemplateCount(AbilityRegistry registry, AbilityCategory category) {
        return (int) registry.templates().stream().filter(template -> template.category() == category).count();
    }

    private AbilityCategory categoryFor(AbilityRegistry registry, String id) {
        return registry.templates().stream()
                .filter(template -> template.id().equals(id))
                .map(AbilityTemplate::category)
                .findFirst()
                .orElseThrow();
    }

    private String summarize(Map<String, Integer> distribution) {
        int total = Math.max(1, distribution.values().stream().mapToInt(Integer::intValue).sum());
        return distribution.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .map(entry -> entry.getKey() + "=" + String.format("%.2f", entry.getValue() / (double) total))
                .collect(Collectors.joining(", "));
    }

    private String summarizeWeights(Map<String, Double> distribution) {
        double total = Math.max(0.0001D, distribution.values().stream().mapToDouble(Double::doubleValue).sum());
        return distribution.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .map(entry -> entry.getKey() + "=" + String.format("%.3f", entry.getValue() / total))
                .collect(Collectors.joining(", "));
    }

    private WeightedCategoryProbe probeNormalizedWeights(ProceduralAbilityGenerator generator,
                                                         AbilityRegistry registry,
                                                         AbilityCategory category,
                                                         Artifact artifact,
                                                         ArtifactMemoryProfile memoryProfile) {
        try {
            Method compositeTemplateScore = ProceduralAbilityGenerator.class.getDeclaredMethod(
                    "compositeTemplateScore",
                    AbilityTemplate.class,
                    Artifact.class,
                    ArtifactMemoryProfile.class,
                    int.class,
                    UtilityHistoryRollup.class,
                    obtuseloot.lineage.ArtifactLineage.class,
                    AdaptiveSupportAllocation.class,
                    ArtifactNicheProfile.class,
                    NicheVariantProfile.class,
                    List.class,
                    AbilityDiversityIndex.AbilitySignature.class,
                    List.class);
            Method normalizeCategoryTemplateScores = ProceduralAbilityGenerator.class.getDeclaredMethod(
                    "normalizeCategoryTemplateScores", List.class, double[].class);
            Method applyCategoryLocalColdBalancing = ProceduralAbilityGenerator.class.getDeclaredMethod(
                    "applyCategoryLocalColdBalancing", List.class, double[].class);
            Method applyConditionalTopThreeCompression = ProceduralAbilityGenerator.class.getDeclaredMethod(
                    "applyConditionalTopThreeCompression", double[].class, double.class);

            compositeTemplateScore.setAccessible(true);
            normalizeCategoryTemplateScores.setAccessible(true);
            applyCategoryLocalColdBalancing.setAccessible(true);
            applyConditionalTopThreeCompression.setAccessible(true);

            List<AbilityTemplate> templates = registry.templates().stream()
                    .filter(template -> template.category() == category)
                    .toList();
            double[] rawScores = new double[templates.size()];
            UtilityHistoryRollup utilityHistory = UtilityHistoryRollup.parse(null);
            for (int i = 0; i < templates.size(); i++) {
                rawScores[i] = Math.max(0.0001D, (double) compositeTemplateScore.invoke(
                        generator,
                        templates.get(i),
                        artifact,
                        memoryProfile,
                        4,
                        utilityHistory,
                        null,
                        AdaptiveSupportAllocation.neutral(),
                        null,
                        null,
                        List.of(),
                        null,
                        List.of()));
            }

            double[] samplingScores = (double[]) normalizeCategoryTemplateScores.invoke(generator, templates, rawScores);
            applyCategoryLocalColdBalancing.invoke(generator, templates, samplingScores);

            double[] weights = new double[templates.size()];
            double total = 0.0D;
            for (int i = 0; i < templates.size(); i++) {
                double weight = Math.sqrt(samplingScores[i]);
                if (templates.size() <= 6) {
                    double min = java.util.Arrays.stream(samplingScores).min().orElse(samplingScores[i]);
                    double max = java.util.Arrays.stream(samplingScores).max().orElse(samplingScores[i]);
                    double span = Math.max(max - min, 1.0E-9D);
                    double relative = (samplingScores[i] - min) / span;
                    double smallCategoryWeight = 0.97D + (relative * (1.03D - 0.97D));
                    weight = Math.pow(Math.max(samplingScores[i], 0.0001D), 0.20D);
                    weight *= smallCategoryWeight;
                    weight = weight + ((1.0D - weight) * 0.45D);
                }
                weights[i] = weight;
                total += weight;
            }
            total = (double) applyConditionalTopThreeCompression.invoke(generator, weights, total);

            Map<String, Double> byTemplate = new LinkedHashMap<>();
            for (int i = 0; i < templates.size(); i++) {
                byTemplate.put(templates.get(i).id(), weights[i]);
            }
            return new WeightedCategoryProbe(byTemplate, total);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to inspect normalized category weights.", e);
        }
    }

    private Artifact artifact(long seed) {
        Artifact artifact = new Artifact(UUID.randomUUID(), "wooden_sword");
        artifact.setArtifactSeed(seed);
        artifact.setArtifactStorageKey("artifact:" + seed);
        artifact.setLatentLineage("normalization-probe");
        artifact.setSeedPrecisionAffinity(0.50D);
        artifact.setSeedBrutalityAffinity(0.24D);
        artifact.setSeedSurvivalAffinity(0.58D);
        artifact.setSeedMobilityAffinity(0.58D);
        artifact.setSeedChaosAffinity(0.54D);
        artifact.setSeedConsistencyAffinity(0.56D);
        return artifact;
    }

    private ArtifactMemoryProfile stealthProfile() {
        return new ArtifactMemoryProfile(7, 1.0D, 1.1D, 1.1D, 0.9D, 1.7D, 0.9D, 0.5D);
    }

    private ArtifactMemoryProfile survivalProfile() {
        return new ArtifactMemoryProfile(8, 1.3D, 0.9D, 0.9D, 1.8D, 0.8D, 0.5D, 0.4D);
    }

    private ArtifactMemoryProfile sensingProfile() {
        return new ArtifactMemoryProfile(8, 1.4D, 1.2D, 0.8D, 1.0D, 1.6D, 0.6D, 0.3D);
    }

    private ArtifactMemoryProfile craftingProfile() {
        return new ArtifactMemoryProfile(8, 1.5D, 0.8D, 0.9D, 1.0D, 0.8D, 0.5D, 0.2D);
    }

    private ArtifactMemoryProfile traversalProfile() {
        return new ArtifactMemoryProfile(8, 1.0D, 0.8D, 0.8D, 1.0D, 1.8D, 0.4D, 0.2D);
    }

    private ArtifactMemoryProfile ritualProfile() {
        return new ArtifactMemoryProfile(8, 1.1D, 1.2D, 0.8D, 0.9D, 0.9D, 1.5D, 0.3D);
    }

    private record DistributionProbe(Map<String, Integer> distribution, int totalHits) {
        private double topShare() {
            return distribution.values().stream().mapToInt(Integer::intValue).max().orElse(0) / (double) Math.max(1, totalHits);
        }

        private double topThreeShare() {
            return distribution.values().stream()
                    .sorted(Comparator.reverseOrder())
                    .limit(3)
                    .mapToInt(Integer::intValue)
                    .sum() / (double) Math.max(1, totalHits);
        }
    }

    private record WeightedCategoryProbe(Map<String, Double> weights, double totalWeight) {
        private double topThreeShare() {
            return weights.values().stream()
                    .sorted(Comparator.reverseOrder())
                    .limit(3)
                    .mapToDouble(Double::doubleValue)
                    .sum() / Math.max(0.0001D, totalWeight);
        }
    }
}
