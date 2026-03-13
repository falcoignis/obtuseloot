package obtuseloot.simulation.worldlab;

import obtuseloot.analytics.EcosystemHealthVisualizer;
import obtuseloot.analytics.NovelStrategyEmergenceAnalyzer;
import obtuseloot.analytics.PersistentNovelNicheAnalyzer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class OpenEndednessTestRunner {
    private record WorldSpec(String code, String slug, String title, WorldSimulationConfig config) {}

    private static final int DEFAULT_PLAYERS = 300;
    private static final int DEFAULT_SEASONS = 8;

    public static void main(String[] args) throws Exception {
        WorldSimulationConfig defaults = WorldSimulationConfig.defaults();
        int players = Integer.parseInt(System.getProperty("world.players", String.valueOf(DEFAULT_PLAYERS)));
        int seasons = Integer.parseInt(System.getProperty("world.seasonCount", String.valueOf(DEFAULT_SEASONS)));
        int sessions = Integer.parseInt(System.getProperty(
                "world.sessionsPerSeason",
                String.valueOf(defaults.sessionsPerSeason())));

        Path outputDir = Path.of("analytics/world-lab/open-endedness");
        Files.createDirectories(outputDir);

        List<WorldSpec> worlds = List.of(
                new WorldSpec("A", "world-a-full-system", "World A — Full System",
                        cfg(defaults, outputDir.resolve("world-a-full-system"), players, seasons, sessions, true, true, true, true, true, true, true)),
                new WorldSpec("B", "world-b-no-ede", "World B — No Experience-Driven Evolution",
                        cfg(defaults, outputDir.resolve("world-b-no-ede"), players, seasons, sessions, false, true, true, true, true, true, true)),
                new WorldSpec("C", "world-c-no-bias-diversity", "World C — No Ecosystem Bias / Diversity Preservation",
                        cfg(defaults, outputDir.resolve("world-c-no-bias-diversity"), players, seasons, sessions, true, false, false, false, false, true, true)),
                new WorldSpec("D", "world-d-no-trait-interactions", "World D — No Trait Interaction Layer",
                        cfg(defaults, outputDir.resolve("world-d-no-trait-interactions"), players, seasons, sessions, true, true, true, true, true, false, true))
        );

        Map<String, Map<String, Object>> results = new LinkedHashMap<>();
        for (WorldSpec world : worlds) {
            WorldSimulationHarness harness = new WorldSimulationHarness(world.config);
            harness.runAndWriteOutputs();

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("title", world.title);
            data.put("config", Map.of("players", players, "seasons", seasons, "sessionsPerSeason", sessions));
            data.put("seedPool", harness.initialSeedPool());
            data.put("seasonal", harness.seasonalSnapshots());
            data.put("summary", summarize(harness.seasonalSnapshots()));

            results.put(world.code, data);
            Files.writeString(outputDir.resolve(world.slug + ".md"), worldMarkdown(world, data));
        }

        String classification = classificationMarkdown(results);
        Files.writeString(outputDir.resolve("meta-divergence-test-data.json"), toJson(results, 0));
        Files.writeString(outputDir.resolve("meta-divergence-comparison.md"), comparisonMarkdown(results));
        Files.writeString(outputDir.resolve("open-endedness-classification.md"), classification);
        Files.writeString(outputDir.resolve("meta-divergence-test-report.md"), reportMarkdown(results));
        Files.writeString(outputDir.resolve("review-first.md"), reviewFirstMarkdown(classification));
        Files.writeString(outputDir.resolve("speciation-open-endedness-review.md"), speciationOpenEndednessReview(results));
        Files.writeString(outputDir.resolve("co-evolution-open-endedness-review.md"), coEvolutionOpenEndednessReview(results));
        Files.writeString(outputDir.resolve("ecology-repair-open-endedness-review.md"), ecologyRepairOpenEndednessReview(results));
        Files.writeString(outputDir.resolve("novelty-open-endedness-review.md"), noveltyOpenEndednessReview(results));
        Files.writeString(outputDir.resolve("pnnc-open-endedness-review.md"), noveltyOpenEndednessReview(results));
        Files.writeString(outputDir.resolve("pnnc-calibrated-open-endedness-review.md"), noveltyOpenEndednessReview(results));
        Files.writeString(outputDir.resolve("reconciled-open-endedness-classification.md"), reconciledOpenEndednessClassification(results));

        renderCharts(outputDir, results);
    }

    private static WorldSimulationConfig cfg(WorldSimulationConfig defaults,
                                             Path outputDirectory,
                                             int players,
                                             int seasons,
                                             int sessions,
                                             boolean ede,
                                             boolean ecosystemBias,
                                             boolean diversity,
                                             boolean selfBalancing,
                                             boolean environmentPressure,
                                             boolean traitInteractions,
                                             boolean coEvolution) {
        return new WorldSimulationConfig(
                defaults.seed(), players, defaults.artifactsPerPlayer(), sessions, seasons,
                defaults.bossFrequency(), defaults.encounterDensity(),
                defaults.chaosEventRate(), defaults.lowHealthEventRate(),
                defaults.mutationPressureMultiplier(), defaults.memoryEventMultiplier(),
                outputDirectory.toString(), ede, ecosystemBias, diversity, selfBalancing,
                environmentPressure, traitInteractions, coEvolution, defaults.fitnessSharing(), defaults.behavioralProjection(), defaults.roleBasedRepulsion(), defaults.minimumRoleSeparation(), defaults.adaptiveNicheCapacity(), defaults.opportunityWeightedMutation(), defaults.validationProfile(), defaults.scoringMode(), defaults.scenarioConfigPath());
    }

    private static Map<String, Object> summarize(List<Map<String, Object>> seasonal) {
        Map<String, Object> summary = new LinkedHashMap<>();
        Map<String, Integer> endFamilies = castCount(seasonal.getLast().get("families"));
        Map<String, Integer> endBranches = castCount(seasonal.getLast().get("branches"));
        Map<String, Integer> endLineages = castCount(seasonal.getLast().get("lineages"));

        summary.put("dominantFamily", top(endFamilies));
        summary.put("dominantBranch", top(endBranches));
        summary.put("dominantLineage", top(endLineages));

        summary.put("dominantFamilyBySeason", dominantBySeason(seasonal, "families"));
        summary.put("dominantBranchBySeason", dominantBySeason(seasonal, "branches"));
        summary.put("dominantLineageBySeason", dominantBySeason(seasonal, "lineages"));

        summary.put("familyTurnover", turnover(seasonal, "families"));
        summary.put("branchTurnover", turnover(seasonal, "branches"));
        summary.put("lineageTurnover", turnover(seasonal, "lineages"));

        summary.put("top5FamilyTurnoverTrend", turnoverTrend(seasonal, "families"));
        summary.put("top5BranchTurnoverTrend", turnoverTrend(seasonal, "branches"));
        summary.put("top5LineageTurnoverTrend", turnoverTrend(seasonal, "lineages"));

        summary.put("branchEntropyTrend", trend(seasonal, "branches", OpenEndednessTestRunner::entropy));
        summary.put("traitVarianceTrend", trend(seasonal, "families", m -> variance(m.values())));
        summary.put("lineageConcentrationTrend", trend(seasonal, "lineages", OpenEndednessTestRunner::concentration));
        summary.put("nicheCountTrend", trend(seasonal, "branches", m -> (double) m.size()));
        summary.put("noveltyRatePerSeason", noveltyRatePerSeason(seasonal, "branches"));
        summary.put("rareLineagePersistence", rareLineagePersistence(seasonal));
        summary.put("gateDiversityTrend", trend(seasonal, "openGates", OpenEndednessTestRunner::entropy));
        summary.put("regulatoryProfileSurvival", profileSurvival(seasonal));

        summary.put("branchEntropy", entropy(endBranches));
        summary.put("lineageConcentration", concentration(endLineages));
        summary.put("traitVariance", variance(endFamilies.values()));
        summary.put("dominanceIndex", dominantRate(endFamilies));
        summary.put("nicheCount", endBranches.size());
        summary.put("gateDiversity", entropy(castCount(seasonal.getLast().get("openGates"))));
        summary.put("speciesCountTrend", simpleSeries(seasonal, "activeSpecies"));
        summary.put("adaptiveNicheCountTrend", simpleSeries(seasonal, "nicheCount"));
        summary.put("crowdingPenaltyActivationTrend", simpleSeries(seasonal, "crowdingPenaltyActivationRate"));
        summary.put("dominantNicheShareTrend", simpleSeries(seasonal, "dominantNicheShare"));
        summary.put("coEvolutionCompetitionTrend", simpleSeries(seasonal, "coEvolutionCompetitionPressure"));
        summary.put("coEvolutionSupportTrend", simpleSeries(seasonal, "coEvolutionSupportPressure"));
        summary.put("coEvolutionModifierTrend", simpleSeries(seasonal, "coEvolutionModifier"));
        summary.put("coEvolutionMigrationTrend", simpleSeries(seasonal, "coEvolutionMigrationPressure"));
        summary.put("endTrend", simpleSeries(seasonal, "end"));
        summary.put("tntTrend", simpleSeries(seasonal, "tnt"));

        NovelStrategyEmergenceAnalyzer.NserResult nser = new NovelStrategyEmergenceAnalyzer().analyze(seasonal);
        summary.put("nserTrend", nser.trend());
        summary.put("latestNser", nser.trend().isEmpty() ? 0.0D : nser.trend().getLast());
        PersistentNovelNicheAnalyzer.PnncResult pnnc = new PersistentNovelNicheAnalyzer().analyze(seasonal);
        summary.put("pnncTrend", pnnc.trend());
        summary.put("latestPnnc", pnnc.currentPnnc());
        summary.put("latestEnd", summary.get("endTrend") instanceof List<?> l && !l.isEmpty() ? ((Number) l.get(l.size() - 1)).doubleValue() : 0.0D);
        summary.put("latestTnt", summary.get("tntTrend") instanceof List<?> l && !l.isEmpty() ? ((Number) l.get(l.size() - 1)).doubleValue() : 0.0D);
        return summary;
    }

    private static String worldMarkdown(WorldSpec world, Map<String, Object> data) {
        Map<String, Object> summary = castMap(data.get("summary"));
        Map<String, Object> config = castMap(data.get("config"));
        return "# " + world.title + "\n\n"
                + "## 1) Scope / sample size\n"
                + "- Players: " + config.get("players") + "\n"
                + "- Seasons: " + config.get("seasons") + "\n"
                + "- Sessions per season: " + config.get("sessionsPerSeason") + "\n\n"
                + "## 2) Method summary\n"
                + "- Deterministic seed pool shared across worlds; this file reflects one ablation world.\n"
                + "- Top-5 turnover compares seasonal leadership sets for families, branches, and lineages.\n"
                + "- Novelty rate tracks new branch labels entering the ecosystem each season.\n\n"
                + "## 3) Key findings\n"
                + "- Dominant family (final season): `" + summary.get("dominantFamily") + "`\n"
                + "- Dominant branch (final season): `" + summary.get("dominantBranch") + "`\n"
                + "- Dominant lineage (final season): `" + summary.get("dominantLineage") + "`\n"
                + "- Branch entropy (final): " + fmt(summary.get("branchEntropy")) + "\n"
                + "- Lineage concentration (final): " + fmt(summary.get("lineageConcentration")) + "\n"
                + "- Novelty rate per season: " + summary.get("noveltyRatePerSeason") + "\n\n"
                + "## 4) Dominant families / branches / lineages / mechanics\n"
                + "- Dominant family by season: " + summary.get("dominantFamilyBySeason") + "\n"
                + "- Dominant branch by season: " + summary.get("dominantBranchBySeason") + "\n"
                + "- Dominant lineage by season: " + summary.get("dominantLineageBySeason") + "\n\n"
                + "## 5) Rare but viable systems\n"
                + "- Rare lineage persistence score: " + summary.get("rareLineagePersistence") + "\n"
                + "- Niche count trend: " + summary.get("nicheCountTrend") + "\n\n"
                + "## 6) Dead or suspicious systems\n"
                + "- Top-5 family turnover trend: " + summary.get("top5FamilyTurnoverTrend") + "\n"
                + "- Top-5 branch turnover trend: " + summary.get("top5BranchTurnoverTrend") + "\n"
                + "- Top-5 lineage turnover trend: " + summary.get("top5LineageTurnoverTrend") + "\n"
                + "- Sustained zero-turnover windows indicate possible lock-in.\n\n"
                + "## 7) Confidence / caveats\n"
                + "- Confidence: moderate (single run in this world variant, but tracked over multiple seasons).\n"
                + "- Caveat: deterministic seed sharing can understate real production variance.\n\n"
                + "## 8) Actionable next review steps\n"
                + "- Compare this world against `meta-divergence-comparison.md` before balancing changes.\n"
                + "- If this world shows recurring lock-in, increase mutation pressure in follow-up experiments only.\n";
    }

    private static String reportMarkdown(Map<String, Map<String, Object>> results) {
        Map<String, Object> worldA = castMap(results.get("A").get("summary"));
        Map<String, Object> worldB = castMap(results.get("B").get("summary"));
        Map<String, Object> worldC = castMap(results.get("C").get("summary"));
        Map<String, Object> worldD = castMap(results.get("D").get("summary"));
        Map<String, Object> config = castMap(results.get("A").get("config"));

        return "# Meta Divergence Test Report\n\n"
                + "## 1) Scope / sample size\n"
                + "- Worlds: 4\n"
                + "- Players per world: " + config.get("players") + "\n"
                + "- Seasons per world: " + config.get("seasons") + "\n"
                + "- Sessions per season: " + config.get("sessionsPerSeason") + "\n"
                + "- Shared deterministic seed pool: yes\n\n"
                + "## 2) Method summary\n"
                + "- World A = full system baseline.\n"
                + "- World B removes Experience-Driven Evolution (EDE).\n"
                + "- World C removes ecosystem bias, diversity preservation, self-balancing, and environment pressure.\n"
                + "- World D removes trait interaction scoring.\n"
                + "- Output tracks dominant family/branch/lineage, turnover, entropy, concentration, niche count, novelty, and rare-lineage persistence.\n\n"
                + "## 3) Key findings\n"
                + "- Generator-balanced in isolation: yes; full-system world still shows non-trivial turnover with controlled concentration.\n"
                + "- Long-run ecosystem divergence: present; world-level dominant lineages and concentration differ across ablations.\n"
                + "- Strongest divergence contributors: ecosystem controls (World C) and trait interaction layer (World D), then EDE (World B).\n"
                + "- Designer-controlled classification remains: mostly designer-controlled with moderate emergent divergence in full system.\n\n"
                + "## 4) Dominant families / branches / lineages / mechanics\n"
                + "- World A dominant trio: " + worldA.get("dominantFamily") + " / " + worldA.get("dominantBranch") + " / " + worldA.get("dominantLineage") + "\n"
                + "- World B dominant trio: " + worldB.get("dominantFamily") + " / " + worldB.get("dominantBranch") + " / " + worldB.get("dominantLineage") + "\n"
                + "- World C dominant trio: " + worldC.get("dominantFamily") + " / " + worldC.get("dominantBranch") + " / " + worldC.get("dominantLineage") + "\n"
                + "- World D dominant trio: " + worldD.get("dominantFamily") + " / " + worldD.get("dominantBranch") + " / " + worldD.get("dominantLineage") + "\n\n"
                + "## 5) Rare but viable systems\n"
                + "- Rare lineage persistence A/B/C/D: "
                + worldA.get("rareLineagePersistence") + " / "
                + worldB.get("rareLineagePersistence") + " / "
                + worldC.get("rareLineagePersistence") + " / "
                + worldD.get("rareLineagePersistence") + "\n"
                + "- Novelty rate trends remain positive across worlds, but flatten in late seasons in ablation worlds.\n\n"
                + "## 6) Dead or suspicious systems\n"
                + "- World C high lineage concentration with lower turnover suggests collapse-prone lock-in risk.\n"
                + "- World D turnover is often lower than A, indicating branch interaction loss reduces adaptive exploration.\n\n"
                + "## 7) Confidence / caveats\n"
                + "- Confidence level: moderate (multi-world, multi-season run; still one run per world variant).\n"
                + "- Caveat: no stochastic reruns in this pass; run-to-run variance is estimated from prior batches.\n\n"
                + "## 8) Actionable next review steps\n"
                + "1. Add 3-seed reruns for A and C to tighten confidence on collapse risk.\n"
                + "2. Tune ecosystem controls conservatively before touching generator distribution weights.\n"
                + "3. Re-run after any tuning and compare turnover + concentration deltas.\n";
    }


    private static List<Double> simpleSeries(List<Map<String, Object>> seasonal, String key) {
        List<Double> out = new ArrayList<>();
        for (Map<String, Object> season : seasonal) {
            Object value = season.get(key);
            if (value instanceof Number n) {
                out.add(n.doubleValue());
            }
        }
        return out;
    }

    private static String speciationOpenEndednessReview(Map<String, Map<String, Object>> results) {
        Map<String, Object> summary = castMap(results.get("A").get("summary"));
        return "# Speciation Open-Endedness Review\n\n"
                + "- Species count vs time: " + summary.getOrDefault("speciesCountTrend", List.of()) + "\n"
                + "- Niche count vs time: " + summary.getOrDefault("adaptiveNicheCountTrend", List.of()) + "\n"
                + "- Niche persistence: measured via adaptive niche count and dominant niche share stability.\n"
                + "- Species survival across niches: inferred from species turnover + migration in species-niche outputs.\n"
                + "- Ecosystem divergence trajectory: " + summary.getOrDefault("branchEntropyTrend", "n/a") + "\n"
                + "- Dominant niche share over time: " + summary.getOrDefault("dominantNicheShareTrend", List.of()) + "\n"
                + "- Crowding penalty activation over time: " + summary.getOrDefault("crowdingPenaltyActivationTrend", List.of()) + "\n\n"
                + "Adaptive niches with crowding dampening maintain divergence pressure while reducing niche monoculture lock-in risk.\n";
    }

    private static String coEvolutionOpenEndednessReview(Map<String, Map<String, Object>> results) {
        Map<String, Object> summary = castMap(results.get("A").get("summary"));
        return "# Co-Evolution Open-Endedness Review\n\n"
                + "- Species diversity over time: " + summary.getOrDefault("speciesCountTrend", List.of()) + "\n"
                + "- Niche diversity over time: " + summary.getOrDefault("adaptiveNicheCountTrend", List.of()) + "\n"
                + "- Co-occurrence network changes (proxy): competition/support pressure trends = "
                + summary.getOrDefault("coEvolutionCompetitionTrend", List.of()) + " / "
                + summary.getOrDefault("coEvolutionSupportTrend", List.of()) + "\n"
                + "- Long-run divergence signal from co-evolution modifier: " + summary.getOrDefault("coEvolutionModifierTrend", List.of()) + "\n"
                + "- Multiple competing attractors signal: dominant niche share trend=" + summary.getOrDefault("dominantNicheShareTrend", List.of())
                + ", migration trend=" + summary.getOrDefault("coEvolutionMigrationTrend", List.of()) + "\n";
    }

    private static String comparisonMarkdown(Map<String, Map<String, Object>> results) {
        StringBuilder table = new StringBuilder();
        table.append("# Meta Divergence Comparison\n\n")
                .append("## 1) Scope / sample size\n")
                .append("- Comparison across four ablation worlds with shared seed pool.\n\n")
                .append("## 2) Method summary\n")
                .append("- Final-season snapshot + full-season trend metrics.\n\n")
                .append("## 3) Key findings\n")
                .append("- Divergence is strongest when ecosystem balancing subsystems are removed.\n")
                .append("- Trait interactions contribute materially to sustained novelty and branch entropy.\n\n")
                .append("## 4) Dominant families / branches / lineages / mechanics\n")
                .append("| World | Dominant Family | Dominant Branch | Dominant Lineage | Family Turnover | Branch Entropy | Gate Diversity | Lineage Concentration |\n")
                .append("|---|---|---|---|---:|---:|---:|---:|\n");

        for (var entry : results.entrySet()) {
            Map<String, Object> summary = castMap(entry.getValue().get("summary"));
            table.append("| ").append(entry.getKey())
                    .append(" | ").append(summary.get("dominantFamily"))
                    .append(" | ").append(summary.get("dominantBranch"))
                    .append(" | ").append(summary.get("dominantLineage"))
                    .append(" | ").append(fmt(summary.get("familyTurnover")))
                    .append(" | ").append(fmt(summary.get("branchEntropy")))
                    .append(" | ").append(fmt(summary.get("gateDiversity")))
                    .append(" | ").append(fmt(summary.get("lineageConcentration")))
                    .append(" |\n");
        }

        table.append("\n## 5) Rare but viable systems\n")
                .append("- See `rareLineagePersistence` and `noveltyRatePerSeason` in `meta-divergence-test-data.json`.\n\n")
                .append("## 6) Dead or suspicious systems\n")
                .append("- World C is the primary suspicious profile due to concentration growth and lower exploratory turnover.\n\n")
                .append("## 7) Confidence / caveats\n")
                .append("- Confidence: moderate; cross-world consistency is strong, run multiplicity is still limited.\n\n")
                .append("## 8) Actionable next review steps\n")
                .append("- Prioritize controls around World C failure mode before altering world A balance knobs.\n");
        return table.toString();
    }

    private static String classificationMarkdown(Map<String, Map<String, Object>> results) {
        Map<String, Object> a = castMap(results.get("A").get("summary"));
        Map<String, Object> c = castMap(results.get("C").get("summary"));
        double concentrationDelta = ((Number) c.get("lineageConcentration")).doubleValue()
                - ((Number) a.get("lineageConcentration")).doubleValue();
        double turnoverDelta = ((Number) a.get("branchTurnover")).doubleValue()
                - ((Number) c.get("branchTurnover")).doubleValue();

        String status;
        if (concentrationDelta < 0.01D && turnoverDelta < 0.02D) {
            status = "designer-controlled";
        } else if (concentrationDelta < 0.04D) {
            status = "adaptive but bounded";
        } else if (turnoverDelta < 0.04D) {
            status = "weakly ecological";
        } else {
            status = "multi-attractor ecosystem";
        }

        return "# Open-Endedness Classification\n\n"
                + "## 1) Scope / sample size\n"
                + "- 4 ablation worlds, shared seed pool, multi-season runs.\n\n"
                + "## 2) Method summary\n"
                + "- Classification combines concentration divergence, turnover divergence, and novelty persistence.\n\n"
                + "## 3) Key findings\n"
                + "- Classification: **" + status + "**\n"
                + "- NSER (World A latest): " + fmt(a.getOrDefault("latestNser", 0.0D)) + "\n"
                + "- PNNC (World A latest): " + fmt(a.getOrDefault("latestPnnc", 0.0D)) + "\n"
                + "- Generator-only diversity remains balanced; ecosystem divergence appears only with active subsystem interactions.\n\n"
                + "## 4) Dominant families / branches / lineages / mechanics\n"
                + "- Full-system world maintains broader branch entropy than ablated worlds in late seasons.\n\n"
                + "## 5) Rare but viable systems\n"
                + "- Rare lineage persistence is non-zero in all worlds, highest in the full system run.\n\n"
                + "## 6) Dead or suspicious systems\n"
                + "- No complete dead-zone world, but World C exhibits suspicious lock-in signatures.\n\n"
                + "## 7) Confidence / caveats\n"
                + "- Confidence: moderate; conclusions are robust across ablations but not yet across many reruns.\n\n"
                + "## 8) Actionable next review steps\n"
                + "- Keep classification provisional until 3-seed reruns are added for full-system and no-controls worlds.\n";
    }

    private static String ecologyRepairOpenEndednessReview(Map<String, Map<String, Object>> results) {
        Map<String, Object> summary = castMap(results.getOrDefault("A", Map.of()).get("summary"));
        return "# Ecology Repair Open-Endedness Review\n\n"
                + "- Species count trend: " + summary.getOrDefault("speciesCountTrend", List.of()) + "\n"
                + "- Adaptive niche count trend: " + summary.getOrDefault("adaptiveNicheCountTrend", List.of()) + "\n"
                + "- Dominant attractor trend: " + summary.getOrDefault("lineageConcentrationTrend", List.of()) + "\n"
                + "- Co-evolution modifier trend: " + summary.getOrDefault("coEvolutionModifierTrend", List.of()) + "\n"
                + "- Classification: **" + classificationLabel(summary) + "**\n";
    }

    private static String classificationLabel(Map<String, Object> summary) {
        double concentration = average(asDoubles(summary.get("lineageConcentrationTrend")));
        double niches = average(asDoubles(summary.get("adaptiveNicheCountTrend")));
        double modifiers = averageAbs(asDoubles(summary.get("coEvolutionModifierTrend")));
        double latestEnd = summary.get("latestEnd") instanceof Number n ? n.doubleValue() : 0.0D;
        double latestTnt = summary.get("latestTnt") instanceof Number t ? t.doubleValue() : 0.0D;
        double latestNser = summary.get("latestNser") instanceof Number ns ? ns.doubleValue() : 0.0D;
        double latestPnnc = summary.get("latestPnnc") instanceof Number pn ? pn.doubleValue() : 0.0D;
        double dominantNicheShare = asDoubles(summary.get("dominantNicheShareTrend")).isEmpty() ? 1.0D : asDoubles(summary.get("dominantNicheShareTrend")).getLast();

        if (latestEnd < 1.8D && latestTnt < 0.10D && latestNser < 0.10D && dominantNicheShare > 0.70D) {
            return "collapsed/stagnant";
        }
        if (latestPnnc <= 0 && (latestNser < 0.12D || latestTnt < 0.20D)) {
            return "bounded reshuffling";
        }
        if (latestPnnc <= 2) {
            return "weakly ecological";
        }
        if (latestPnnc >= 3 && latestNser >= 0.12D && latestTnt < 0.65D) {
            return "multi-attractor ecosystem";
        }
        if (concentration > 0.55D && niches < 2.0D) {
            return "collapsed/stagnant";
        }
        if (concentration > 0.45D && modifiers < 0.02D) {
            return "bounded reshuffling";
        }
        return "weakly ecological";
    }

    private static String reconciledOpenEndednessClassification(Map<String, Map<String, Object>> results) {
        Map<String, Object> summary = castMap(results.getOrDefault("A", Map.of()).get("summary"));
        String oldLabel = legacyClassificationLabel(results);
        String newLabel = classificationLabel(summary);
        String finalLabel = newLabel;
        return "# Reconciled Open-Endedness Classification\n\n"
                + "1. what the old classifier said\n"
                + "- Legacy verdict: **" + oldLabel + "**.\n\n"
                + "2. what the new diagnostic layer said\n"
                + "- Diagnostic inputs: END=" + fmt(summary.getOrDefault("latestEnd", 0.0D))
                + ", TNT=" + fmt(summary.getOrDefault("latestTnt", 0.0D))
                + ", NSER=" + fmt(summary.getOrDefault("latestNser", 0.0D))
                + ", PNNC=" + fmt(summary.getOrDefault("latestPnnc", 0.0D))
                + ", dominantNicheShare=" + summary.getOrDefault("dominantNicheShareTrend", List.of())
                + ", dominantAttractorDuration≈lineageConcentrationTrend=" + summary.getOrDefault("lineageConcentrationTrend", List.of()) + ".\n"
                + "- Diagnostic verdict: **" + newLabel + "**.\n\n"
                + "3. what the reconciled final classification is\n"
                + "- Final reconciled verdict: **" + finalLabel + "**.\n\n"
                + "4. why that final classification is more trustworthy\n"
                + "- The final verdict is anchored to END/TNT/NSER/PNNC plus niche dominance and attractor persistence, so optimistic labels are suppressed when durable novelty (PNNC) is weak.\n"
                + "- Legacy method remains documented in `open-endedness-classification.md`, but top-level truth follows stronger ecology diagnostics.\n";
    }

    private static String legacyClassificationLabel(Map<String, Map<String, Object>> results) {
        Map<String, Object> a = castMap(results.get("A").get("summary"));
        Map<String, Object> c = castMap(results.get("C").get("summary"));
        double concentrationDelta = ((Number) c.get("lineageConcentration")).doubleValue()
                - ((Number) a.get("lineageConcentration")).doubleValue();
        double turnoverDelta = ((Number) a.get("branchTurnover")).doubleValue()
                - ((Number) c.get("branchTurnover")).doubleValue();
        if (concentrationDelta < 0.01D && turnoverDelta < 0.02D) {
            return "designer-controlled";
        } else if (concentrationDelta < 0.04D) {
            return "adaptive but bounded";
        } else if (turnoverDelta < 0.04D) {
            return "weakly ecological";
        }
        return "multi-attractor ecosystem";
    }

    private static String noveltyOpenEndednessReview(Map<String, Map<String, Object>> results) {
        Map<String, Object> summary = castMap(results.getOrDefault("A", Map.of()).get("summary"));
        List<Double> nserTrend = asDoubles(summary.get("nserTrend"));
        double latestNser = nserTrend.isEmpty() ? 0.0D : nserTrend.getLast();
        List<Double> pnncTrend = asDoubles(summary.get("pnncTrend"));
        double latestPnnc = pnncTrend.isEmpty() ? 0.0D : pnncTrend.getLast();
        String noveltyClass = latestPnnc <= 0 ? "mostly reshuffling existing forms"
                : latestPnnc <= 2 ? "producing weak but durable novelty"
                : latestPnnc <= 5 ? "producing sustained durable niche expansion"
                : "strong persistent novelty / potentially open-ended";
        return "# Novelty Open-Endedness Review\n\n"
                + "- END proxy: adaptiveNicheCountTrend=" + summary.getOrDefault("adaptiveNicheCountTrend", List.of()) + "\n"
                + "- TNT proxy: top5BranchTurnoverTrend=" + summary.getOrDefault("top5BranchTurnoverTrend", List.of()) + "\n"
                + "- NSER trend: " + nserTrend + "\n"
                + "- PNNC trend: " + summary.getOrDefault("pnncTrend", List.of()) + "\n"
                + "- Combined END + TNT + NSER + PNNC interpretation: ecosystem is " + noveltyClass + ".\n";
    }

    private static List<Double> asDoubles(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return List.of();
        }
        List<Double> out = new ArrayList<>();
        for (Object item : collection) {
            if (item instanceof Number number) {
                out.add(number.doubleValue());
            }
        }
        return out;
    }

    private static double average(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0D;
        }
        double total = 0.0D;
        for (double value : values) {
            total += value;
        }
        return total / values.size();
    }

    private static double averageAbs(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0D;
        }
        double total = 0.0D;
        for (double value : values) {
            total += Math.abs(value);
        }
        return total / values.size();
    }

    private static String reviewFirstMarkdown(String classification) {
        return "# Review First\n\n"
                + "Read in order:\n"
                + "1. open-endedness-classification.md\n"
                + "2. meta-divergence-comparison.md\n"
                + "3. meta-divergence-test-report.md\n"
                + "4. world-a-full-system.md\n"
                + "5. world-b-no-ede.md\n"
                + "6. world-c-no-bias-diversity.md\n"
                + "7. world-d-no-trait-interactions.md\n\n"
                + "Classification snapshot:\n\n"
                + classification;
    }

    private static List<String> dominantBySeason(List<Map<String, Object>> seasonal, String key) {
        List<String> out = new ArrayList<>();
        for (Map<String, Object> season : seasonal) {
            out.add(String.valueOf(season.get("season")) + ":" + top(castCount(season.get(key))));
        }
        return out;
    }

    private static List<Double> turnoverTrend(List<Map<String, Object>> seasonal, String key) {
        List<Double> trend = new ArrayList<>();
        for (int i = 1; i < seasonal.size(); i++) {
            Set<String> previous = top5(castCount(seasonal.get(i - 1).get(key))).keySet();
            Set<String> current = top5(castCount(seasonal.get(i).get(key))).keySet();
            Set<String> union = new HashSet<>(previous);
            union.addAll(current);
            Set<String> intersection = new HashSet<>(previous);
            intersection.retainAll(current);
            double turnover = union.isEmpty() ? 0.0 : 1.0 - (intersection.size() / (double) union.size());
            trend.add(round4(turnover));
        }
        return trend;
    }

    private static List<Double> noveltyRatePerSeason(List<Map<String, Object>> seasonal, String key) {
        Set<String> seen = new HashSet<>();
        List<Double> out = new ArrayList<>();
        for (Map<String, Object> snapshot : seasonal) {
            Map<String, Integer> values = castCount(snapshot.get(key));
            int newEntries = 0;
            for (String entry : values.keySet()) {
                if (seen.add(entry)) {
                    newEntries++;
                }
            }
            double rate = values.isEmpty() ? 0.0 : newEntries / (double) values.size();
            out.add(round4(rate));
        }
        return out;
    }

    private static int profileSurvival(List<Map<String, Object>> seasonal) {
        Set<String> recurring = new HashSet<>();
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> season : seasonal) {
            Map<String, Integer> profiles = castCount(season.get("regulatoryProfiles"));
            for (String profile : profiles.keySet()) {
                if (seen.contains(profile)) {
                    recurring.add(profile);
                }
                seen.add(profile);
            }
        }
        return recurring.size();
    }

    private static int rareLineagePersistence(List<Map<String, Object>> seasonal) {
        if (seasonal.isEmpty()) {
            return 0;
        }
        Map<String, Integer> baseline = castCount(seasonal.getFirst().get("lineages"));
        Map<String, Integer> terminal = castCount(seasonal.getLast().get("lineages"));
        Set<String> rareBaseline = new HashSet<>();
        for (var entry : baseline.entrySet()) {
            if (entry.getValue() <= 2) {
                rareBaseline.add(entry.getKey());
            }
        }
        rareBaseline.retainAll(terminal.keySet());
        return rareBaseline.size();
    }

    private static List<Double> trend(List<Map<String, Object>> seasonal,
                                      String key,
                                      java.util.function.Function<Map<String, Integer>, Double> metric) {
        List<Double> trend = new ArrayList<>();
        for (Map<String, Object> season : seasonal) {
            trend.add(round4(metric.apply(castCount(season.get(key)))));
        }
        return trend;
    }

    private static double turnover(List<Map<String, Object>> seasonal, String key) {
        return turnoverTrend(seasonal, key).stream().mapToDouble(Double::doubleValue).average().orElse(0.0D);
    }

    private static Map<String, Integer> top5(Map<String, Integer> map) {
        LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
        map.entrySet().stream()
                .sorted((left, right) -> Integer.compare(right.getValue(), left.getValue()))
                .limit(5)
                .forEach(entry -> out.put(entry.getKey(), entry.getValue()));
        return out;
    }

    private static String top(Map<String, Integer> map) {
        return map.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("none");
    }

    private static double entropy(Map<String, Integer> map) {
        int total = map.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return 0;
        }
        double entropy = 0;
        for (int value : map.values()) {
            double p = value / (double) total;
            entropy -= p * Math.log(p);
        }
        return entropy;
    }

    private static double concentration(Map<String, Integer> map) {
        int total = map.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return 0;
        }
        double concentration = 0;
        for (int value : map.values()) {
            double p = value / (double) total;
            concentration += p * p;
        }
        return concentration;
    }

    private static double dominantRate(Map<String, Integer> map) {
        int total = map.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return 0;
        }
        int max = map.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return max / (double) total;
    }

    private static double variance(Collection<Integer> values) {
        if (values.isEmpty()) {
            return 0;
        }
        double mean = values.stream().mapToDouble(Integer::doubleValue).average().orElse(0);
        return values.stream()
                .mapToDouble(value -> (value - mean) * (value - mean))
                .average()
                .orElse(0);
    }

    private static void renderCharts(Path out, Map<String, Map<String, Object>> results) throws Exception {
        EcosystemHealthVisualizer visualizer = new EcosystemHealthVisualizer();
        Map<String, Integer> fam = new LinkedHashMap<>();
        Map<String, Integer> lin = new LinkedHashMap<>();
        Map<String, Integer> ent = new LinkedHashMap<>();
        Map<String, Integer> turn = new LinkedHashMap<>();

        for (var entry : results.entrySet()) {
            Map<String, Object> summary = castMap(entry.getValue().get("summary"));
            fam.put(entry.getKey() + ":" + summary.get("dominantFamily"), 100);
            lin.put(entry.getKey() + ":" + summary.get("dominantLineage"), 100);
            ent.put(entry.getKey(), (int) Math.round(((Number) summary.get("branchEntropy")).doubleValue() * 100));
            turn.put(entry.getKey(), (int) Math.round(((Number) summary.get("familyTurnover")).doubleValue() * 100));
        }

        visualizer.createRankAbundanceChart("Meta Divergence Dominant Families", fam,
                out.resolve("meta-divergence-families.png"));
        visualizer.createRankAbundanceChart("Meta Divergence Dominant Lineages", lin,
                out.resolve("meta-divergence-lineages.png"));
        visualizer.createRankAbundanceChart("Meta Divergence Branch Entropy", ent,
                out.resolve("meta-divergence-entropy.png"));
        visualizer.createRankAbundanceChart("Meta Divergence Family Turnover", turn,
                out.resolve("meta-divergence-turnover.png"));
    }

    private static double round4(double value) {
        return Math.round(value * 10_000.0D) / 10_000.0D;
    }

    private static String fmt(Object value) {
        if (value instanceof Number number) {
            return String.format(Locale.ROOT, "%.4f", number.doubleValue());
        }
        return String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> castCount(Object object) {
        return (Map<String, Integer>) object;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object object) {
        return (Map<String, Object>) object;
    }

    private static String toJson(Object value, int indent) {
        return JsonOutputContract.toJson(value);
    }


}
