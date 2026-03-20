package obtuseloot.abilities;

import obtuseloot.artifacts.Artifact;
import obtuseloot.lineage.ArtifactLineage;
import obtuseloot.lineage.LineageBiasDimension;
import obtuseloot.lineage.LineageInfluenceResolver;
import obtuseloot.lineage.LineageRegistry;
import obtuseloot.memory.ArtifactMemoryProfile;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ColdCompletionDeepValidationTest {

    private static final int NORMAL_GENERATIONS_PER_SCENARIO = 1_000;
    private static final int FORCED_GENERATIONS_PER_CATEGORY = 500;
    private static final Path OUTPUT = Path.of("docs/deep-cold-completion-validation-final.md");

    @Test
    void generateDeepColdCompletionValidationReport() throws Exception {
        AbilityRegistry registry = new AbilityRegistry();
        Map<AbilityCategory, Integer> templateTotals = registry.templates().stream()
                .collect(Collectors.groupingBy(AbilityTemplate::category,
                        () -> new EnumMap<>(AbilityCategory.class),
                        Collectors.summingInt(template -> 1)));

        NormalProbeResult normal = runNormalProbe(registry);
        ForcedProbeResult forced = runForcedCategoryProbe(registry);
        boolean success = passesStrictSuccessCriteria(templateTotals, forced.categoryMetrics());
        String report = renderReport(templateTotals, normal, forced, success);
        Files.writeString(OUTPUT, report);
        assertTrue(Files.exists(OUTPUT), "Validation report should be written.");
    }

    private NormalProbeResult runNormalProbe(AbilityRegistry registry) throws Exception {
        List<ScenarioSpec> scenarios = List.of(
                new ScenarioSpec("explorer", false, explorerProfile(), null, 0.0D, 0.0D, 0.0D),
                new ScenarioSpec("builder", false, builderProfile(), null, 0.0D, 0.0D, 0.0D),
                new ScenarioSpec("fighter", false, fighterProfile(), null, 0.0D, 0.0D, 0.0D),
                new ScenarioSpec("ritualist", true, ritualProfile(), "deep-cold-ritualist", 0.04D, 0.30D, 0.24D),
                new ScenarioSpec("survivor", false, survivalProfile(), null, 0.0D, 0.0D, 0.0D)
        );
        Map<String, ScenarioSummary> scenarioSummaries = new LinkedHashMap<>();
        Map<AbilityCategory, Map<String, Integer>> categoryTemplateCounts = emptyCategoryTemplateCounts(registry);
        int totalGenerations = 0;

        for (int s = 0; s < scenarios.size(); s++) {
            ScenarioSpec spec = scenarios.get(s);
            Map<AbilityCategory, Integer> categoryHits = new EnumMap<>(AbilityCategory.class);
            for (int i = 0; i < NORMAL_GENERATIONS_PER_SCENARIO; i++) {
                resetDiversityIndex();
                LineageRegistry lineageRegistry = spec.withLineage() ? new LineageRegistry() : null;
                ProceduralAbilityGenerator generator = spec.withLineage()
                        ? new ProceduralAbilityGenerator(registry, null, lineageRegistry, new LineageInfluenceResolver())
                        : new ProceduralAbilityGenerator(registry);
                Artifact artifact = artifact(10_000_000L + (s * 100_000L) + i, spec.lineageId());
                if (lineageRegistry != null) {
                    ArtifactLineage lineage = lineageRegistry.assignLineage(artifact);
                    lineage.evolutionaryBiasGenome().add(LineageBiasDimension.EXPLORATION_PREFERENCE, spec.explorationBias());
                    lineage.evolutionaryBiasGenome().add(LineageBiasDimension.RITUAL_PREFERENCE, spec.ritualBias());
                    lineage.evolutionaryBiasGenome().add(LineageBiasDimension.WEIRDNESS, spec.weirdnessBias());
                    lineage.evolutionaryBiasGenome().add(LineageBiasDimension.SUPPORT_PREFERENCE, 0.12D);
                }
                AbilityProfile profile = generator.generate(artifact, 3, spec.memoryProfile());
                for (AbilityDefinition ability : profile.abilities()) {
                    AbilityCategory category = categoryFor(registry, ability.id());
                    categoryTemplateCounts.get(category).merge(ability.id(), 1, Integer::sum);
                    categoryHits.merge(category, 1, Integer::sum);
                }
                totalGenerations++;
            }
            scenarioSummaries.put(spec.name(), new ScenarioSummary(spec.name(), categoryHits));
        }
        return new NormalProbeResult(totalGenerations, scenarioSummaries, buildMetrics(templateTotals(registry), categoryTemplateCounts));
    }

    private ForcedProbeResult runForcedCategoryProbe(AbilityRegistry registry) throws Exception {
        Map<AbilityCategory, Map<String, Integer>> categoryTemplateCounts = emptyCategoryTemplateCounts(registry);
        for (AbilityCategory category : AbilityCategory.values()) {
            ProceduralAbilityGenerator generator = new ProceduralAbilityGenerator(registry);
            Method weighted = ProceduralAbilityGenerator.class.getDeclaredMethod(
                    "weightedTemplateSelection",
                    List.class, Artifact.class, ArtifactMemoryProfile.class, int.class,
                    obtuseloot.evolution.UtilityHistoryRollup.class,
                    obtuseloot.lineage.ArtifactLineage.class,
                    obtuseloot.evolution.AdaptiveSupportAllocation.class,
                    obtuseloot.evolution.ArtifactNicheProfile.class,
                    obtuseloot.evolution.NicheVariantProfile.class,
                    List.class,
                    AbilityDiversityIndex.AbilitySignature.class,
                    List.class,
                    Random.class);
            weighted.setAccessible(true);
            Method resolveScoringNicheProfile = ProceduralAbilityGenerator.class.getDeclaredMethod(
                    "resolveScoringNicheProfile",
                    obtuseloot.evolution.UtilityHistoryRollup.class,
                    ArtifactMemoryProfile.class);
            resolveScoringNicheProfile.setAccessible(true);
            Method recordRecentSelections = ProceduralAbilityGenerator.class.getDeclaredMethod("recordRecentSelections", List.class);
            recordRecentSelections.setAccessible(true);

            ArtifactMemoryProfile profile = categoryProfile(category);
            List<AbilityTemplate> templates = registry.templates().stream()
                    .filter(template -> template.category() == category)
                    .toList();
            for (int i = 0; i < FORCED_GENERATIONS_PER_CATEGORY; i++) {
                resetDiversityIndex();
                Artifact artifact = artifact(20_000_000L + (category.ordinal() * 100_000L) + i, "forced-" + category.name().toLowerCase());
                Object utilityHistory = obtuseloot.evolution.UtilityHistoryRollup.parse(artifact.getLastUtilityHistory());
                Object nicheProfile = resolveScoringNicheProfile.invoke(generator, utilityHistory, profile);
                Random random = new Random(artifact.getArtifactSeed() ^ profile.pressure() ^ category.ordinal());
                AbilityTemplate selected = (AbilityTemplate) weighted.invoke(
                        generator,
                        templates,
                        artifact,
                        profile,
                        3,
                        utilityHistory,
                        null,
                        obtuseloot.evolution.AdaptiveSupportAllocation.neutral(),
                        nicheProfile,
                        null,
                        List.of(),
                        null,
                        List.of(),
                        random);
                categoryTemplateCounts.get(category).merge(selected.id(), 1, Integer::sum);
                recordRecentSelections.invoke(generator, List.of(selected));
            }
        }
        return new ForcedProbeResult(FORCED_GENERATIONS_PER_CATEGORY * AbilityCategory.values().length,
                buildMetrics(templateTotals(registry), categoryTemplateCounts));
    }

    private Map<AbilityCategory, Integer> templateTotals(AbilityRegistry registry) {
        return registry.templates().stream()
                .collect(Collectors.groupingBy(AbilityTemplate::category,
                        () -> new EnumMap<>(AbilityCategory.class),
                        Collectors.summingInt(template -> 1)));
    }

    private Map<AbilityCategory, CategoryMetrics> buildMetrics(Map<AbilityCategory, Integer> templateTotals,
                                                               Map<AbilityCategory, Map<String, Integer>> counts) {
        Map<AbilityCategory, CategoryMetrics> metrics = new EnumMap<>(AbilityCategory.class);
        for (AbilityCategory category : AbilityCategory.values()) {
            Map<String, Integer> distribution = counts.getOrDefault(category, Map.of());
            int totalHits = distribution.values().stream().mapToInt(Integer::intValue).sum();
            int templateCount = templateTotals.getOrDefault(category, 0);
            Set<String> allTemplates = new AbilityRegistry().templates().stream()
                    .filter(template -> template.category() == category)
                    .map(AbilityTemplate::id)
                    .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
            List<String> zeroHit = allTemplates.stream().filter(id -> !distribution.containsKey(id)).toList();
            List<Integer> sorted = distribution.values().stream().sorted(Comparator.reverseOrder()).toList();
            double topShare = sorted.isEmpty() ? 0.0D : sorted.get(0) / (double) Math.max(1, totalHits);
            double topThreeShare = sorted.stream().limit(3).mapToInt(Integer::intValue).sum() / (double) Math.max(1, totalHits);
            long multiHitCount = distribution.values().stream().filter(count -> count > 1).count();
            double uniformTopShare = templateCount == 0 ? 0.0D : 1.0D / templateCount;
            boolean noUniformFlattening = topShare > (uniformTopShare + 0.02D);
            metrics.put(category, new CategoryMetrics(category, totalHits, distribution.size(), templateCount, zeroHit, topShare, topThreeShare, multiHitCount, noUniformFlattening, distribution));
        }
        return metrics;
    }

    private boolean passesStrictSuccessCriteria(Map<AbilityCategory, Integer> templateTotals,
                                                Map<AbilityCategory, CategoryMetrics> forcedMetrics) {
        for (AbilityCategory category : AbilityCategory.values()) {
            CategoryMetrics metrics = forcedMetrics.get(category);
            if (metrics == null) {
                return false;
            }
            if (metrics.reachedTemplates() != templateTotals.getOrDefault(category, 0)) {
                return false;
            }
            if (!metrics.zeroHitTemplates().isEmpty()) {
                return false;
            }
            if (metrics.topShare() > 0.50D) {
                return false;
            }
            if (!metrics.noUniformFlattening()) {
                return false;
            }
            if (metrics.multiHitTemplates() != templateTotals.getOrDefault(category, 0)) {
                return false;
            }
        }
        return forcedMetrics.get(AbilityCategory.STEALTH_TRICKERY_DISRUPTION).reachedTemplates() == 9
                && forcedMetrics.get(AbilityCategory.DEFENSE_WARDING).reachedTemplates() == 8
                && forcedMetrics.get(AbilityCategory.RITUAL_STRANGE_UTILITY).reachedTemplates() == 13;
    }

    private String renderReport(Map<AbilityCategory, Integer> templateTotals,
                                NormalProbeResult normal,
                                ForcedProbeResult forced,
                                boolean success) {
        StringBuilder out = new StringBuilder();
        out.append("# Deep Cold Completion Validation Final\n\n");
        out.append("## SECTION 1: HIGH-VOLUME PROBE RESULTS\n\n");
        out.append("- Total generations: ").append(normal.totalGenerations()).append("\n");
        out.append("- Scenarios: ").append(String.join(", ", normal.scenarioSummaries().keySet())).append("\n\n");
        out.append("### Scenario category exposure snapshots\n\n");
        for (ScenarioSummary summary : normal.scenarioSummaries().values()) {
            out.append("- **").append(summary.name()).append("**: ")
                    .append(summary.categoryHits().entrySet().stream()
                            .sorted(Map.Entry.<AbilityCategory, Integer>comparingByValue(Comparator.reverseOrder()))
                            .limit(4)
                            .map(entry -> entry.getKey().label() + "=" + entry.getValue())
                            .collect(Collectors.joining(", ")))
                    .append("\n");
        }
        out.append("\n### Normal-probe category metrics\n\n");
        out.append(metricsTable(normal.categoryMetrics()));

        out.append("\n## SECTION 2: CATEGORY-FORCED RESULTS\n\n");
        out.append("- Total forced generations: ").append(forced.totalGenerations()).append("\n");
        out.append("- Generations per category: ").append(FORCED_GENERATIONS_PER_CATEGORY).append("\n\n");
        out.append(metricsTable(forced.categoryMetrics()));

        out.append("\n## SECTION 3: REACHABILITY MATRIX\n\n");
        out.append("| Category | Templates | Normal reached | Forced reached | Zero-hit forced templates |\n");
        out.append("| --- | ---: | ---: | ---: | --- |\n");
        for (AbilityCategory category : AbilityCategory.values()) {
            CategoryMetrics normalMetrics = normal.categoryMetrics().get(category);
            CategoryMetrics forcedMetrics = forced.categoryMetrics().get(category);
            out.append("| ").append(category.label()).append(" | ")
                    .append(templateTotals.get(category)).append(" | ")
                    .append(normalMetrics.reachedTemplates()).append("/").append(normalMetrics.templateCount()).append(" | ")
                    .append(forcedMetrics.reachedTemplates()).append("/").append(forcedMetrics.templateCount()).append(" | ")
                    .append(forcedMetrics.zeroHitTemplates().isEmpty() ? "none" : String.join(", ", forcedMetrics.zeroHitTemplates()))
                    .append(" |\n");
        }

        out.append("\n## SECTION 4: DISTRIBUTION ANALYSIS\n\n");
        for (AbilityCategory category : AbilityCategory.values()) {
            CategoryMetrics metrics = forced.categoryMetrics().get(category);
            out.append("### ").append(category.label()).append("\n\n");
            out.append("- Total hits: ").append(metrics.totalHits()).append("\n");
            out.append("- Templates reached: ").append(metrics.reachedTemplates()).append(" / ").append(metrics.templateCount()).append("\n");
            out.append("- Zero-hit templates: ").append(metrics.zeroHitTemplates().isEmpty() ? "none" : String.join(", ", metrics.zeroHitTemplates())).append("\n");
            out.append("- Top template share: ").append(percent(metrics.topShare())).append("\n");
            out.append("- Top-3 share: ").append(percent(metrics.topThreeShare())).append("\n");
            out.append("- Templates with >1 hit: ").append(metrics.multiHitTemplates()).append("\n");
            out.append("- No uniform flattening: ").append(metrics.noUniformFlattening() ? "PASS" : "FAIL").append("\n");
            out.append("- Distribution: ").append(summarizeDistribution(metrics.templateDistribution())).append("\n\n");
        }

        out.append("## SECTION 5: FINAL JUDGMENT\n\n");
        out.append("- Reachability across all categories: ").append(success ? "PASS" : "FAIL").append("\n");
        out.append("- Stealth reachable: ")
                .append(forced.categoryMetrics().get(AbilityCategory.STEALTH_TRICKERY_DISRUPTION).reachedTemplates()).append("/9\n");
        out.append("- Defense reachable: ")
                .append(forced.categoryMetrics().get(AbilityCategory.DEFENSE_WARDING).reachedTemplates()).append("/8\n");
        out.append("- Ritual reachable: ")
                .append(forced.categoryMetrics().get(AbilityCategory.RITUAL_STRANGE_UTILITY).reachedTemplates()).append("/13\n");
        out.append("- No template above 50%: ")
                .append(forced.categoryMetrics().values().stream().noneMatch(metrics -> metrics.topShare() > 0.50D) ? "PASS" : "FAIL").append("\n");
        out.append("- No uniform flattening: ")
                .append(forced.categoryMetrics().values().stream().allMatch(CategoryMetrics::noUniformFlattening) ? "PASS" : "FAIL").append("\n");
        out.append("- Long-tail templates appear multiple times: ")
                .append(forced.categoryMetrics().entrySet().stream().allMatch(entry -> entry.getValue().multiHitTemplates() == templateTotals.get(entry.getKey())) ? "PASS" : "FAIL").append("\n\n");
        out.append("COLD_COMPLETION_RESULT: ").append(success ? "SUCCESS" : "FAILED").append("\n");
        return out.toString();
    }

    private String metricsTable(Map<AbilityCategory, CategoryMetrics> metrics) {
        StringBuilder out = new StringBuilder();
        out.append("| Category | Total hits | Reached | Zero-hit templates | Top template % | Top-3 % | Templates with >1 hit |\n");
        out.append("| --- | ---: | ---: | --- | ---: | ---: | ---: |\n");
        for (AbilityCategory category : AbilityCategory.values()) {
            CategoryMetrics metric = metrics.get(category);
            out.append("| ").append(category.label()).append(" | ")
                    .append(metric.totalHits()).append(" | ")
                    .append(metric.reachedTemplates()).append("/").append(metric.templateCount()).append(" | ")
                    .append(metric.zeroHitTemplates().isEmpty() ? "none" : String.join(", ", metric.zeroHitTemplates())).append(" | ")
                    .append(percent(metric.topShare())).append(" | ")
                    .append(percent(metric.topThreeShare())).append(" | ")
                    .append(metric.multiHitTemplates()).append(" |\n");
        }
        return out.toString();
    }

    private String summarizeDistribution(Map<String, Integer> distribution) {
        int total = Math.max(1, distribution.values().stream().mapToInt(Integer::intValue).sum());
        return distribution.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .map(entry -> entry.getKey() + "=" + percent(entry.getValue() / (double) total))
                .collect(Collectors.joining(", "));
    }

    private String percent(double value) {
        return String.format("%.1f%%", value * 100.0D);
    }

    private Map<AbilityCategory, Map<String, Integer>> emptyCategoryTemplateCounts(AbilityRegistry registry) {
        Map<AbilityCategory, Map<String, Integer>> counts = new EnumMap<>(AbilityCategory.class);
        for (AbilityCategory category : AbilityCategory.values()) {
            counts.put(category, new LinkedHashMap<>());
        }
        return counts;
    }

    private AbilityCategory categoryFor(AbilityRegistry registry, String id) {
        return registry.templates().stream()
                .filter(template -> template.id().equals(id))
                .map(AbilityTemplate::category)
                .findFirst()
                .orElseThrow();
    }

    private Artifact artifact(long seed, String lineageId) {
        Artifact artifact = new Artifact(UUID.randomUUID(), "wooden_sword");
        artifact.setArtifactSeed(seed);
        artifact.setArtifactStorageKey("artifact:" + seed);
        artifact.setLatentLineage(lineageId == null ? "deep-cold" : lineageId);
        artifact.setSeedPrecisionAffinity(0.54D);
        artifact.setSeedBrutalityAffinity(0.28D);
        artifact.setSeedSurvivalAffinity(0.62D);
        artifact.setSeedMobilityAffinity(0.60D);
        artifact.setSeedChaosAffinity(0.58D);
        artifact.setSeedConsistencyAffinity(0.60D);
        return artifact;
    }

    private void resetDiversityIndex() throws Exception {
        java.lang.reflect.Field recentField = AbilityDiversityIndex.class.getDeclaredField("recent");
        recentField.setAccessible(true);
        ((java.util.Deque<?>) recentField.get(AbilityDiversityIndex.instance())).clear();
    }

    private ArtifactMemoryProfile categoryProfile(AbilityCategory category) {
        return switch (category) {
            case TRAVERSAL_MOBILITY -> explorerProfile();
            case SENSING_INFORMATION -> sensingProfile();
            case SURVIVAL_ADAPTATION -> survivalProfile();
            case COMBAT_TACTICAL_CONTROL -> fighterProfile();
            case DEFENSE_WARDING -> defenseProfile();
            case RESOURCE_FARMING_LOGISTICS -> builderProfile();
            case CRAFTING_ENGINEERING_AUTOMATION -> builderProfile();
            case SOCIAL_SUPPORT_COORDINATION -> socialProfile();
            case RITUAL_STRANGE_UTILITY -> ritualProfile();
            case STEALTH_TRICKERY_DISRUPTION -> stealthProfile();
        };
    }

    private ArtifactMemoryProfile explorerProfile() { return new ArtifactMemoryProfile(6, 1.0D, 0.9D, 0.8D, 0.7D, 1.8D, 0.4D, 0.2D); }
    private ArtifactMemoryProfile builderProfile() { return new ArtifactMemoryProfile(7, 1.0D, 1.4D, 0.8D, 1.0D, 0.9D, 0.5D, 0.2D); }
    private ArtifactMemoryProfile fighterProfile() { return new ArtifactMemoryProfile(6, 1.1D, 0.8D, 1.7D, 1.2D, 1.1D, 0.6D, 0.4D); }
    private ArtifactMemoryProfile ritualProfile() { return new ArtifactMemoryProfile(8, 1.2D, 1.1D, 0.8D, 0.9D, 0.9D, 1.6D, 0.5D); }
    private ArtifactMemoryProfile survivalProfile() { return new ArtifactMemoryProfile(8, 1.3D, 0.9D, 0.9D, 1.7D, 0.7D, 0.5D, 0.5D); }
    private ArtifactMemoryProfile defenseProfile() { return new ArtifactMemoryProfile(8, 1.2D, 1.4D, 1.0D, 1.8D, 0.9D, 0.7D, 0.4D); }
    private ArtifactMemoryProfile stealthProfile() { return new ArtifactMemoryProfile(7, 1.0D, 1.1D, 1.0D, 0.9D, 1.7D, 0.9D, 0.5D); }
    private ArtifactMemoryProfile sensingProfile() { return new ArtifactMemoryProfile(8, 1.4D, 1.2D, 0.8D, 1.0D, 1.6D, 0.6D, 0.3D); }
    private ArtifactMemoryProfile socialProfile() { return new ArtifactMemoryProfile(7, 1.1D, 1.0D, 0.9D, 1.0D, 1.0D, 1.0D, 0.4D); }

    private record ScenarioSpec(String name,
                                boolean withLineage,
                                ArtifactMemoryProfile memoryProfile,
                                String lineageId,
                                double explorationBias,
                                double ritualBias,
                                double weirdnessBias) {}

    private record ScenarioSummary(String name, Map<AbilityCategory, Integer> categoryHits) {}

    private record NormalProbeResult(int totalGenerations,
                                     Map<String, ScenarioSummary> scenarioSummaries,
                                     Map<AbilityCategory, CategoryMetrics> categoryMetrics) {}

    private record ForcedProbeResult(int totalGenerations,
                                     Map<AbilityCategory, CategoryMetrics> categoryMetrics) {}

    private record CategoryMetrics(AbilityCategory category,
                                   int totalHits,
                                   int reachedTemplates,
                                   int templateCount,
                                   List<String> zeroHitTemplates,
                                   double topShare,
                                   double topThreeShare,
                                   long multiHitTemplates,
                                   boolean noUniformFlattening,
                                   Map<String, Integer> templateDistribution) {}
}
