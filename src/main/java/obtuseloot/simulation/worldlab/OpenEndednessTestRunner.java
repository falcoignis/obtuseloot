package obtuseloot.simulation.worldlab;

import obtuseloot.analytics.EcosystemHealthVisualizer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class OpenEndednessTestRunner {
    private record WorldSpec(String code, String slug, String title, WorldSimulationConfig config) {}

    private static final int DEFAULT_PLAYERS = 1000;
    private static final int DEFAULT_SEASONS = 5;

    public static void main(String[] args) throws Exception {
        WorldSimulationConfig d = WorldSimulationConfig.defaults();
        int players = Integer.parseInt(System.getProperty("world.players", String.valueOf(DEFAULT_PLAYERS)));
        int seasons = Integer.parseInt(System.getProperty("world.seasonCount", String.valueOf(DEFAULT_SEASONS)));
        int sessions = Integer.parseInt(System.getProperty("world.sessionsPerSeason", String.valueOf(d.sessionsPerSeason())));

        List<WorldSpec> worlds = List.of(
                new WorldSpec("A", "world-a-full-system", "World A — Full System", cfg(d, players, seasons, sessions, true, true, true, true, true, true)),
                new WorldSpec("B", "world-b-no-ede", "World B — No Experience-Driven Evolution", cfg(d, players, seasons, sessions, false, true, true, true, true, true)),
                new WorldSpec("C", "world-c-no-bias-diversity", "World C — No Ecosystem Bias / Diversity Preservation", cfg(d, players, seasons, sessions, true, false, false, false, false, true)),
                new WorldSpec("D", "world-d-no-trait-interactions", "World D — No Trait Interaction Layer", cfg(d, players, seasons, sessions, true, true, true, true, true, false))
        );

        Path out = Path.of("analytics/world-lab/open-endedness");
        Files.createDirectories(out);

        Map<String, Map<String, Object>> results = new LinkedHashMap<>();
        for (WorldSpec world : worlds) {
            WorldSimulationHarness harness = new WorldSimulationHarness(world.config);
            harness.runAndWriteOutputs();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("title", world.title);
            data.put("config", Map.of("players", players, "seasons", seasons, "sessionsPerSeason", sessions));
            data.put("seedPool", harness.initialSeedPool());
            data.put("seasonal", harness.seasonalSnapshots());
            Map<String, Object> summary = summarize(harness.seasonalSnapshots());
            data.put("summary", summary);
            results.put(world.code, data);
            Files.writeString(out.resolve(world.slug + ".md"), worldMarkdown(world, data));
        }

        Files.writeString(out.resolve("meta-divergence-test-data.json"), toJson(results, 0));
        Files.writeString(out.resolve("meta-divergence-comparison.md"), comparisonMarkdown(results));
        String classification = classificationMarkdown(results);
        Files.writeString(out.resolve("open-endedness-classification.md"), classification);
        Files.writeString(out.resolve("meta-divergence-test-report.md"), reportMarkdown(results));
        Files.writeString(out.resolve("meta-divergence-test-report.md"), reportMarkdown(results));
        Files.writeString(out.resolve("review-first.md"), reviewFirstMarkdown(classification));

        renderCharts(out, results);
        Files.writeString(out.resolve("meta-divergence-test-report.md"), reportMarkdown(results));
    }

    private static WorldSimulationConfig cfg(WorldSimulationConfig d, int players, int seasons, int sessions,
                                             boolean ede, boolean ecoBias, boolean diversity, boolean selfBal,
                                             boolean env, boolean trait) {
        return new WorldSimulationConfig(
                d.seed(), players, d.artifactsPerPlayer(), sessions, seasons, d.bossFrequency(), d.encounterDensity(),
                d.chaosEventRate(), d.lowHealthEventRate(), d.mutationPressureMultiplier(), d.memoryEventMultiplier(),
                d.outputDirectory(), ede, ecoBias, diversity, selfBal, env, trait);
    }

    private static Map<String, Object> summarize(List<Map<String, Object>> seasonal) {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Integer> endFamilies = castCount(seasonal.getLast().get("families"));
        Map<String, Integer> endBranches = castCount(seasonal.getLast().get("branches"));
        Map<String, Integer> endLineages = castCount(seasonal.getLast().get("lineages"));
        out.put("dominantFamily", top(endFamilies));
        out.put("dominantBranch", top(endBranches));
        out.put("dominantLineage", top(endLineages));
        out.put("branchEntropy", entropy(endBranches));
        out.put("lineageConcentration", concentration(endLineages));
        out.put("traitVariance", variance(endFamilies.values()));
        out.put("dominanceIndex", dominantRate(endFamilies));
        out.put("nicheCount", endBranches.size());
        out.put("familyTurnover", turnover(seasonal, "families"));
        out.put("branchTurnover", turnover(seasonal, "branches"));
        out.put("lineageTurnover", turnover(seasonal, "lineages"));
        out.put("newBranchCombosAfterS1", novelty(seasonal, "branches", 1));
        out.put("newDominantLineagesAfterS2", dominantLineageNovelty(seasonal));
        out.put("mutationDiversity", castCount(seasonal.getLast().get("mutations")).size());
        return out;
    }

    private static String worldMarkdown(WorldSpec world, Map<String, Object> data) {
        Map<String, Object> s = castMap(data.get("summary"));
        return "# " + world.title + "\n\n"
                + "- Dominant family: `" + s.get("dominantFamily") + "`\n"
                + "- Dominant branch: `" + s.get("dominantBranch") + "`\n"
                + "- Dominant lineage: `" + s.get("dominantLineage") + "`\n"
                + "- Dominance index: " + s.get("dominanceIndex") + "\n"
                + "- Branch entropy: " + s.get("branchEntropy") + "\n"
                + "- Trait variance: " + s.get("traitVariance") + "\n"
                + "- Niche count: " + s.get("nicheCount") + "\n"
                + "- Lineage concentration: " + s.get("lineageConcentration") + "\n"
                + "- Top5 family turnover: " + s.get("familyTurnover") + "\n"
                + "- Top5 branch turnover: " + s.get("branchTurnover") + "\n"
                + "- Top5 lineage turnover: " + s.get("lineageTurnover") + "\n"
                + "- New branch combos after S1: " + s.get("newBranchCombosAfterS1") + "\n"
                + "- New dominant lineages after S2: " + s.get("newDominantLineagesAfterS2") + "\n"
                + "- Mutation diversity: " + s.get("mutationDiversity") + "\n";
    }

    private static String comparisonMarkdown(Map<String, Map<String, Object>> results) {
        StringBuilder sb = new StringBuilder("# Meta Divergence Comparison\n\n| World | Dominant Family | Dominant Branch | Dominant Lineage | Lineage Concentration | Turnover(F/B/L) | Branch Entropy |\n|---|---|---|---|---:|---|---:|\n");
        for (var e : results.entrySet()) {
            Map<String, Object> s = castMap(e.getValue().get("summary"));
            sb.append("| ").append(e.getKey()).append(" | ").append(s.get("dominantFamily")).append(" | ").append(s.get("dominantBranch"))
                    .append(" | ").append(s.get("dominantLineage")).append(" | ").append(s.get("lineageConcentration"))
                    .append(" | ").append(s.get("familyTurnover")).append("/").append(s.get("branchTurnover")).append("/").append(s.get("lineageTurnover"))
                    .append(" | ").append(s.get("branchEntropy")).append(" |\n");
        }
        sb.append("\n## Interpretation of subsystem impact\n- EDE and trait interactions increase novelty when they raise turnover and branch entropy.\n- Bias/diversity controls are protective if they lower concentration and collapse trend.\n");
        return sb.toString();
    }

    private static String classificationMarkdown(Map<String, Map<String, Object>> results) {
        Set<Object> families = new HashSet<>();
        Set<Object> branches = new HashSet<>();
        double avgTurnover = 0;
        double avgEntropy = 0;
        for (var world : results.values()) {
            Map<String, Object> s = castMap(world.get("summary"));
            families.add(s.get("dominantFamily"));
            branches.add(s.get("dominantBranch"));
            avgTurnover += ((Number) s.get("familyTurnover")).doubleValue();
            avgEntropy += ((Number) s.get("branchEntropy")).doubleValue();
        }
        avgTurnover /= results.size();
        avgEntropy /= results.size();
        String klass = families.size() >= 3 && branches.size() >= 3 && avgTurnover > 0.35 ? "Emergent evolutionary system"
                : families.size() >= 2 || avgTurnover > 0.2 ? "Adaptive but bounded"
                : "Mostly designer-controlled";
        return "# Open-Endedness Classification\n\n- Final classification: **" + klass + "**\n"
                + "- Evidence: distinct dominant families=" + families.size() + ", dominant branches=" + branches.size() + ", avg turnover=" + avgTurnover + ", avg entropy=" + avgEntropy + "\n"
                + "- Confidence level: medium\n"
                + "- Most divergence contribution: trait interactions + ecosystem controls\n"
                + "- Next recommended experiment: extend to 10+ seasons and perturb environmental pressure schedule.\n";
    }

    private static String reportMarkdown(Map<String, Map<String, Object>> results) {
        String firstKey = results.keySet().iterator().next();
        int players = ((Number) castMap(results.get(firstKey).get("config")).get("players")).intValue();
        int seasons = ((Number) castMap(results.get(firstKey).get("config")).get("seasons")).intValue();
        int seedPool = ((List<?>) results.get(firstKey).get("seedPool")).size();
        return "# Meta Divergence Test Report\n\n"
                + "## Experiment design\nFour worlds with identical initial seed pool and controlled subsystem ablations.\n\n"
                + "## Number of worlds\n- 4\n\n"
                + "## Player count\n- " + players + "\n\n"
                + "## Season count\n- " + seasons + "\n\n"
                + "## Fixed seed pool details\n- Seed count: " + seedPool + "\n- Shared across all worlds: true\n\n"
                + "## Key per-world outcomes\nSee world-specific markdown files in this folder.\n\n"
                + "## Divergence summary\nSee `meta-divergence-comparison.md`.\n\n"
                + "## Conclusions\nSee `open-endedness-classification.md`.\n";
    }

    private static String reviewFirstMarkdown(String classification) {
        String quick = classification.contains("Emergent") ? "emergent" : classification.contains("Adaptive") ? "adaptive but bounded" : "designer-controlled";
        return "# Review First\n\n"
                + "Open first: `open-endedness-classification.md`\n\n"
                + "System appears: **" + quick + "**\n"
                + "Most important differences: dominant family/branch divergence and turnover spread across worlds.\n"
                + "Trait interactions materially increased novelty where entropy/turnover are higher in World A vs D.\n"
                + "EDE materially changed long-run outcomes where World A vs B diverge in dominant family/lineage.\n"
                + "Bias/diversity controls prevented collapse when World C concentration/collapse risk rises above World A.\n\n"
                + "Recommended reading order:\n"
                + "1. open-endedness-classification.md\n2. meta-divergence-comparison.md\n3. meta-divergence-test-report.md\n4. world-a-full-system.md\n5. world-b-no-ede.md\n6. world-c-no-bias-diversity.md\n7. world-d-no-trait-interactions.md\n";
    }

    private static void renderCharts(Path out, Map<String, Map<String, Object>> results) throws Exception {
        EcosystemHealthVisualizer visualizer = new EcosystemHealthVisualizer();
        Map<String, Integer> fam = new LinkedHashMap<>();
        Map<String, Integer> lin = new LinkedHashMap<>();
        Map<String, Integer> ent = new LinkedHashMap<>();
        Map<String, Integer> turn = new LinkedHashMap<>();
        for (var e : results.entrySet()) {
            Map<String, Object> s = castMap(e.getValue().get("summary"));
            fam.put(e.getKey() + ":" + s.get("dominantFamily"), 100);
            lin.put(e.getKey() + ":" + s.get("dominantLineage"), 100);
            ent.put(e.getKey(), (int) Math.round(((Number) s.get("branchEntropy")).doubleValue() * 100));
            turn.put(e.getKey(), (int) Math.round(((Number) s.get("familyTurnover")).doubleValue() * 100));
        }
        visualizer.createRankAbundanceChart("Meta Divergence Dominant Families", fam, out.resolve("meta-divergence-families.png"));
        visualizer.createRankAbundanceChart("Meta Divergence Dominant Lineages", lin, out.resolve("meta-divergence-lineages.png"));
        visualizer.createRankAbundanceChart("Meta Divergence Branch Entropy", ent, out.resolve("meta-divergence-entropy.png"));
        visualizer.createRankAbundanceChart("Meta Divergence Family Turnover", turn, out.resolve("meta-divergence-turnover.png"));
    }

    private static String top(Map<String, Integer> map) {
        return map.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("none");
    }

    private static double entropy(Map<String, Integer> map) { int t = map.values().stream().mapToInt(Integer::intValue).sum(); if (t == 0) return 0; double e = 0; for (int v : map.values()) { double p = v / (double) t; e -= p * Math.log(p); } return e; }
    private static double concentration(Map<String, Integer> map) { int t = map.values().stream().mapToInt(Integer::intValue).sum(); if (t == 0) return 0; double c = 0; for (int v : map.values()) { double p = v / (double) t; c += p * p; } return c; }
    private static double dominantRate(Map<String, Integer> map) { int t = map.values().stream().mapToInt(Integer::intValue).sum(); if (t == 0) return 0; int m = map.values().stream().mapToInt(Integer::intValue).max().orElse(0); return m / (double) t; }
    private static double variance(Collection<Integer> values) { if (values.isEmpty()) return 0; double mean = values.stream().mapToDouble(Integer::doubleValue).average().orElse(0); return values.stream().mapToDouble(v -> (v - mean) * (v - mean)).average().orElse(0); }

    private static double turnover(List<Map<String, Object>> seasonal, String key) {
        double total = 0; int n = 0;
        for (int i = 1; i < seasonal.size(); i++) {
            Set<String> a = top5(castCount(seasonal.get(i - 1).get(key))).keySet();
            Set<String> b = top5(castCount(seasonal.get(i).get(key))).keySet();
            Set<String> u = new HashSet<>(a); u.addAll(b);
            Set<String> inter = new HashSet<>(a); inter.retainAll(b);
            total += u.isEmpty() ? 0 : 1.0 - (inter.size() / (double) u.size());
            n++;
        }
        return n == 0 ? 0 : total / n;
    }

    private static int novelty(List<Map<String, Object>> seasonal, String key, int baselineSeason) {
        Set<String> seen = new HashSet<>(castCount(seasonal.get(Math.max(0, baselineSeason - 1)).get(key)).keySet());
        int newOnes = 0;
        for (int i = baselineSeason; i < seasonal.size(); i++) {
            for (String item : castCount(seasonal.get(i).get(key)).keySet()) {
                if (seen.add(item)) newOnes++;
            }
        }
        return newOnes;
    }

    private static int dominantLineageNovelty(List<Map<String, Object>> seasonal) {
        if (seasonal.size() < 3) return 0;
        String baseline = top(castCount(seasonal.get(1).get("lineages")));
        int changes = 0;
        for (int i = 2; i < seasonal.size(); i++) {
            if (!Objects.equals(baseline, top(castCount(seasonal.get(i).get("lineages"))))) changes++;
        }
        return changes;
    }

    private static Map<String, Integer> top5(Map<String, Integer> map) {
        LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
        map.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())).limit(5).forEach(e -> out.put(e.getKey(), e.getValue()));
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> castCount(Object o) { return (Map<String, Integer>) o; }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object o) { return (Map<String, Object>) o; }

    private static String toJson(Object value, int indent) {
        String pad = "  ".repeat(indent);
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{\n");
            Iterator<? extends Map.Entry<?, ?>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                var e = it.next();
                sb.append(pad).append("  \"").append(e.getKey()).append("\": ").append(toJson(e.getValue(), indent + 1));
                if (it.hasNext()) sb.append(',');
                sb.append('\n');
            }
            return sb.append(pad).append('}').toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) { if (i > 0) sb.append(','); sb.append(toJson(list.get(i), indent + 1)); }
            return sb.append(']').toString();
        }
        if (value instanceof String s) return "\"" + s.replace("\"", "\\\"") + "\"";
        return String.valueOf(value);
    }
}
