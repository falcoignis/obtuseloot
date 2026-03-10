package obtuseloot.analytics;

import java.util.*;

public class NovelStrategyEmergenceAnalyzer {
    public record Thresholds(int minimumObservations,
                             double minimumOccupancyShare,
                             int minimumPersistenceSeasons,
                             double noveltySimilarityThreshold) {
        public static Thresholds defaults() {
            return new Thresholds(3, 0.05D, 2, 0.75D);
        }
    }

    public record SeasonNser(int season,
                             int novelSignificantStrategies,
                             int totalSignificantStrategies,
                             double nser,
                             int novelSignificantArtifactStrategies,
                             int totalSignificantArtifactStrategies,
                             double nserArtifacts,
                             int novelSignificantSpeciesStrategies,
                             int totalSignificantSpeciesStrategies,
                             double nserSpecies,
                             List<String> representativeNovelStrategies,
                             List<String> persistentNovelStrategies) {}

    public record NserResult(String signatureDefinition,
                             String significanceRules,
                             Thresholds thresholds,
                             List<SeasonNser> bySeason,
                             List<Double> trend,
                             String interpretation,
                             List<String> strategyThresholdGuidance) {}

    public NserResult analyze(List<Map<String, Object>> seasonalSnapshots) {
        return analyze(seasonalSnapshots, Thresholds.defaults());
    }

    public NserResult analyze(List<Map<String, Object>> seasonalSnapshots, Thresholds thresholds) {
        if (seasonalSnapshots == null || seasonalSnapshots.isEmpty()) {
            return new NserResult(signatureDefinition(), significanceRules(thresholds), thresholds,
                    List.of(), List.of(), "No seasonal strategy signatures available.", thresholdGuidance());
        }

        List<SeasonFeatures> features = new ArrayList<>();
        for (Map<String, Object> snapshot : seasonalSnapshots) {
            features.add(extractSeasonFeatures(snapshot));
        }

        Map<String, Integer> artifactPresence = new LinkedHashMap<>();
        Map<String, Integer> speciesPresence = new LinkedHashMap<>();
        Set<String> priorSignificantArtifacts = new LinkedHashSet<>();
        Set<String> priorSignificantSpecies = new LinkedHashSet<>();

        List<SeasonNser> seasons = new ArrayList<>();
        List<Double> trend = new ArrayList<>();

        for (int i = 0; i < features.size(); i++) {
            SeasonFeatures season = features.get(i);
            int seasonNumber = season.season();

            Set<String> artifactCandidates = significantCandidates(season.artifactSignatures(), thresholds);
            Set<String> speciesCandidates = significantCandidates(season.speciesSignatures(), thresholds);

            incrementPresence(artifactPresence, artifactCandidates);
            incrementPresence(speciesPresence, speciesCandidates);

            Set<String> significantArtifacts = applyPersistence(artifactCandidates, artifactPresence, thresholds.minimumPersistenceSeasons());
            Set<String> significantSpecies = applyPersistence(speciesCandidates, speciesPresence, thresholds.minimumPersistenceSeasons());

            Set<String> novelArtifacts = novelComparedTo(significantArtifacts, priorSignificantArtifacts, thresholds.noveltySimilarityThreshold());
            Set<String> novelSpecies = novelComparedTo(significantSpecies, priorSignificantSpecies, thresholds.noveltySimilarityThreshold());

            priorSignificantArtifacts.addAll(significantArtifacts);
            priorSignificantSpecies.addAll(significantSpecies);

            Set<String> significantAll = new LinkedHashSet<>();
            significantAll.addAll(significantArtifacts);
            significantAll.addAll(significantSpecies);
            Set<String> novelAll = new LinkedHashSet<>();
            novelAll.addAll(novelArtifacts);
            novelAll.addAll(novelSpecies);

            Set<String> persistentNovel = new LinkedHashSet<>();
            for (String signature : novelAll) {
                if (artifactPresence.getOrDefault(signature, 0) >= thresholds.minimumPersistenceSeasons()
                        || speciesPresence.getOrDefault(signature, 0) >= thresholds.minimumPersistenceSeasons()) {
                    persistentNovel.add(signature);
                }
            }

            double nser = ratio(novelAll.size(), significantAll.size());
            double nserArtifacts = ratio(novelArtifacts.size(), significantArtifacts.size());
            double nserSpecies = ratio(novelSpecies.size(), significantSpecies.size());

            List<String> representativeNovel = novelAll.stream().limit(5).toList();
            List<String> persistentNovelList = persistentNovel.stream().limit(5).toList();

            seasons.add(new SeasonNser(
                    seasonNumber,
                    novelAll.size(),
                    significantAll.size(),
                    round4(nser),
                    novelArtifacts.size(),
                    significantArtifacts.size(),
                    round4(nserArtifacts),
                    novelSpecies.size(),
                    significantSpecies.size(),
                    round4(nserSpecies),
                    representativeNovel,
                    persistentNovelList));
            trend.add(round4(nser));
        }

        return new NserResult(signatureDefinition(), significanceRules(thresholds), thresholds,
                seasons, trend, interpret(trend), thresholdGuidance());
    }

    private SeasonFeatures extractSeasonFeatures(Map<String, Object> snapshot) {
        int season = snapshot.get("season") instanceof Number n ? n.intValue() : 0;
        Map<String, Integer> branches = castCount(snapshot.get("branches"));
        Map<String, Integer> triggers = castCount(snapshot.get("triggers"));
        Map<String, Integer> mechanics = castCount(snapshot.get("mechanics"));
        Map<String, Integer> profiles = castCount(snapshot.get("regulatoryProfiles"));
        Map<String, Integer> niches = castCount(snapshot.get("nicheOccupancy"));
        Map<String, Integer> speciesPerNiche = castCount(snapshot.get("speciesPerNiche"));

        String dominantProfile = top(profiles);
        String dominantTrigger = top(triggers);
        String dominantMechanic = top(mechanics);
        String dominantNiche = top(niches);
        String tendency = tendency(snapshot);

        Map<String, Integer> artifactSignatures = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> branch : branches.entrySet()) {
            String family = branch.getKey().contains(".") ? branch.getKey().substring(0, branch.getKey().indexOf('.')) : branch.getKey();
            String signature = "ART|niche=" + compact(dominantNiche)
                    + "|gate=" + compact(dominantProfile)
                    + "|trigger=" + compact(triggerClass(dominantTrigger))
                    + "|mechanic=" + compact(mechanicClass(dominantMechanic))
                    + "|branchRole=" + compact(family)
                    + "|branch=" + compact(branch.getKey())
                    + "|tendency=" + tendency;
            artifactSignatures.merge(signature, branch.getValue(), Integer::sum);
        }

        Map<String, Integer> speciesSignatures = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> speciesEntry : speciesPerNiche.entrySet()) {
            String signature = "SPC|niche=" + compact(speciesEntry.getKey())
                    + "|gate=" + compact(dominantProfile)
                    + "|trigger=" + compact(triggerClass(dominantTrigger))
                    + "|mechanic=" + compact(mechanicClass(dominantMechanic))
                    + "|branchPref=" + compact(topBranchFamily(branches))
                    + "|tendency=" + tendency;
            speciesSignatures.merge(signature, speciesEntry.getValue(), Integer::sum);
        }

        return new SeasonFeatures(season, artifactSignatures, speciesSignatures);
    }

    private Set<String> significantCandidates(Map<String, Integer> signatures, Thresholds thresholds) {
        int total = signatures.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return Set.of();
        }
        Set<String> out = new LinkedHashSet<>();
        for (Map.Entry<String, Integer> entry : signatures.entrySet()) {
            double share = entry.getValue() / (double) total;
            if (entry.getValue() >= thresholds.minimumObservations() && share >= thresholds.minimumOccupancyShare()) {
                out.add(entry.getKey());
            }
        }
        return out;
    }

    private Set<String> applyPersistence(Set<String> signatures, Map<String, Integer> presence, int minPersistence) {
        Set<String> out = new LinkedHashSet<>();
        for (String signature : signatures) {
            if (presence.getOrDefault(signature, 0) >= minPersistence) {
                out.add(signature);
            }
        }
        return out;
    }

    private Set<String> novelComparedTo(Set<String> current, Set<String> prior, double similarityThreshold) {
        Set<String> out = new LinkedHashSet<>();
        for (String signature : current) {
            double maxSimilarity = 0.0D;
            for (String previous : prior) {
                maxSimilarity = Math.max(maxSimilarity, signatureSimilarity(signature, previous));
            }
            if (maxSimilarity < similarityThreshold) {
                out.add(signature);
            }
        }
        return out;
    }

    private void incrementPresence(Map<String, Integer> presence, Set<String> signatures) {
        for (String signature : signatures) {
            presence.merge(signature, 1, Integer::sum);
        }
    }

    private String tendency(Map<String, Object> snapshot) {
        double competition = asDouble(snapshot.get("coEvolutionCompetitionPressure"));
        double support = asDouble(snapshot.get("coEvolutionSupportPressure"));
        double modifier = asDouble(snapshot.get("coEvolutionModifier"));
        if (competition - support > 0.05D || modifier < -0.01D) {
            return "COMPETITIVE";
        }
        if (support - competition > 0.05D || modifier > 0.01D) {
            return "SUPPORTIVE";
        }
        return "BALANCED";
    }

    private String topBranchFamily(Map<String, Integer> branches) {
        String topBranch = top(branches);
        if (topBranch.isBlank()) {
            return "none";
        }
        return topBranch.contains(".") ? topBranch.substring(0, topBranch.indexOf('.')) : topBranch;
    }

    private String triggerClass(String trigger) {
        String t = trigger.toLowerCase(Locale.ROOT);
        if (t.contains("movement") || t.contains("drift")) return "mobility";
        if (t.contains("low_health") || t.contains("survival") || t.contains("defensive")) return "survival";
        if (t.contains("boss") || t.contains("chain") || t.contains("combat") || t.contains("kill")) return "combat";
        if (t.contains("memory") || t.contains("awakening") || t.contains("fusion") || t.contains("lineage")) return "evolution";
        return "hybrid";
    }

    private String mechanicClass(String mechanic) {
        String m = mechanic.toLowerCase(Locale.ROOT);
        if (m.contains("field") || m.contains("zone") || m.contains("aura")) return "area-control";
        if (m.contains("threshold") || m.contains("window") || m.contains("barrier")) return "defensive";
        if (m.contains("chain") || m.contains("burst") || m.contains("strike") || m.contains("trigger")) return "offense";
        if (m.contains("memory") || m.contains("resonance") || m.contains("lineage")) return "adaptive";
        return "hybrid";
    }

    private double signatureSimilarity(String a, String b) {
        Set<String> ta = new LinkedHashSet<>(Arrays.asList(a.split("\\|")));
        Set<String> tb = new LinkedHashSet<>(Arrays.asList(b.split("\\|")));
        if (ta.isEmpty() && tb.isEmpty()) {
            return 1.0D;
        }
        Set<String> intersection = new LinkedHashSet<>(ta);
        intersection.retainAll(tb);
        Set<String> union = new LinkedHashSet<>(ta);
        union.addAll(tb);
        return union.isEmpty() ? 0.0D : intersection.size() / (double) union.size();
    }

    private String top(Map<String, Integer> map) {
        String best = "";
        int max = Integer.MIN_VALUE;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    private String signatureDefinition() {
        return "Signature = [type (artifact/species), niche identity, dominant regulatory gate profile, dominant trigger class, dominant mechanic class, branch role/preference, co-evolution tendency].";
    }

    private String significanceRules(Thresholds t) {
        return "A signature is significant when observation count >= " + t.minimumObservations()
                + ", occupancy share >= " + t.minimumOccupancyShare()
                + ", and persistence >= " + t.minimumPersistenceSeasons()
                + " seasons. Novelty additionally requires max similarity < " + t.noveltySimilarityThreshold() + " vs prior significant signatures.";
    }

    private List<String> thresholdGuidance() {
        return List.of(
                "NSER < 0.05 => no real novelty",
                "0.05 <= NSER < 0.15 => weak novelty",
                "0.15 <= NSER < 0.30 => healthy novelty",
                "0.30 <= NSER < 0.50 => strong innovation phase",
                "NSER >= 0.50 => likely over-fragmented/noisy");
    }

    private String interpret(List<Double> trend) {
        if (trend.isEmpty()) {
            return "No NSER data available.";
        }
        double latest = trend.get(trend.size() - 1);
        if (latest < 0.05D) {
            return "Ecosystem is largely recycling old strategies.";
        }
        if (latest < 0.15D) {
            return "Ecosystem shows weak novelty with mostly recycled strategy space.";
        }
        if (latest < 0.30D) {
            return "Ecosystem is producing healthy bounded novelty.";
        }
        if (latest < 0.50D) {
            return "Ecosystem is in a strong innovation phase with substantial strategy emergence.";
        }
        return "Ecosystem novelty is very high; monitor for over-fragmentation/noise.";
    }

    private String compact(String raw) {
        String value = raw == null || raw.isBlank() ? "none" : raw;
        return value.replace(" ", "").replace("[", "(").replace("]", ")");
    }

    private double ratio(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0D;
        }
        return numerator / (double) denominator;
    }

    private double asDouble(Object value) {
        return value instanceof Number n ? n.doubleValue() : 0.0D;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> castCount(Object object) {
        if (!(object instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getValue() instanceof Number n) {
                out.put(String.valueOf(entry.getKey()), n.intValue());
            }
        }
        return out;
    }

    private double round4(double value) {
        return Math.round(value * 10_000.0D) / 10_000.0D;
    }

    private record SeasonFeatures(int season,
                                  Map<String, Integer> artifactSignatures,
                                  Map<String, Integer> speciesSignatures) {}
}
