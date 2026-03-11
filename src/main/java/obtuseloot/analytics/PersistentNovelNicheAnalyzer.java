package obtuseloot.analytics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PersistentNovelNicheAnalyzer {

    public record Thresholds(int minimumPersistenceSeasons,
                             double minimumOccupancyShare,
                             int minimumSpeciesSupport,
                             double noveltyDistanceThreshold) {
        public static Thresholds defaults() {
            return new Thresholds(3, 0.05D, 2, 0.35D);
        }
    }

    public record SeasonPnnc(int season,
                             int pnnc,
                             int novelCandidates,
                             List<String> persistentNovelNiches,
                             List<String> failedNovelCandidates) {
    }

    public record PnncResult(Thresholds thresholds,
                             List<String> noveltyCriteria,
                             List<String> persistenceCriteria,
                             List<SeasonPnnc> bySeason,
                             int currentPnnc,
                             List<Integer> trend,
                             List<String> persistentNovelExamples,
                             List<String> failedCandidates,
                             List<String> retiredPersistentNiches,
                             List<Integer> persistentLifespanDistribution,
                             String interpretation) {
    }

    private static final class CandidateState {
        int firstSeason;
        int lastSeason;
        int seasonsObserved;
        int maxSpeciesSupport;
        double maxOccupancy;
        double noveltyDistance;
        Map<String, Double> signature;
        boolean persistent;
    }

    public PnncResult analyze(List<Map<String, Object>> snapshots) {
        return analyze(snapshots, Thresholds.defaults());
    }

    @SuppressWarnings("unchecked")
    public PnncResult analyze(List<Map<String, Object>> snapshots, Thresholds thresholds) {
        Map<String, CandidateState> candidates = new LinkedHashMap<>();
        Set<String> persistentNovel = new LinkedHashSet<>();
        Set<String> retiredPersistent = new LinkedHashSet<>();
        List<Map<String, Double>> knownSignatures = new ArrayList<>();
        List<SeasonPnnc> bySeason = new ArrayList<>();
        List<Integer> trend = new ArrayList<>();
        List<Integer> lifespans = new ArrayList<>();

        for (Map<String, Object> snapshot : snapshots) {
            int season = ((Number) snapshot.getOrDefault("season", bySeason.size() + 1)).intValue();
            Map<String, Integer> nicheOccupancy = (Map<String, Integer>) snapshot.getOrDefault("nicheOccupancy", Map.of());
            Map<String, Integer> speciesPerNiche = (Map<String, Integer>) snapshot.getOrDefault("speciesPerNiche", Map.of());
            int total = Math.max(1, nicheOccupancy.values().stream().mapToInt(Integer::intValue).sum());

            List<String> failedThisSeason = new ArrayList<>();
            int seasonCandidates = 0;
            for (Map.Entry<String, Integer> niche : nicheOccupancy.entrySet()) {
                String nicheId = niche.getKey();
                double occupancyShare = niche.getValue() / (double) total;
                int speciesSupport = speciesPerNiche.getOrDefault(nicheId, 1);
                Map<String, Double> signature = buildSignature(snapshot, occupancyShare, speciesSupport);
                double noveltyDistance = minDistance(signature, knownSignatures);
                boolean candidateNovel = noveltyDistance >= thresholds.noveltyDistanceThreshold() && !candidates.containsKey(nicheId) && !persistentNovel.contains(nicheId);

                CandidateState state = candidates.computeIfAbsent(nicheId, ignored -> {
                    CandidateState s = new CandidateState();
                    s.firstSeason = season;
                    s.signature = signature;
                    s.noveltyDistance = noveltyDistance;
                    return s;
                });
                state.lastSeason = season;
                state.seasonsObserved++;
                state.maxOccupancy = Math.max(state.maxOccupancy, occupancyShare);
                state.maxSpeciesSupport = Math.max(state.maxSpeciesSupport, speciesSupport);

                if (candidateNovel) {
                    seasonCandidates++;
                }

                if (!state.persistent
                        && state.seasonsObserved >= thresholds.minimumPersistenceSeasons()
                        && state.maxOccupancy >= thresholds.minimumOccupancyShare()
                        && state.maxSpeciesSupport >= thresholds.minimumSpeciesSupport()
                        && state.noveltyDistance >= thresholds.noveltyDistanceThreshold()) {
                    state.persistent = true;
                    persistentNovel.add(nicheId);
                    lifespans.add(state.seasonsObserved);
                    knownSignatures.add(state.signature);
                } else if (!state.persistent && season - state.firstSeason >= thresholds.minimumPersistenceSeasons() && state.maxOccupancy < thresholds.minimumOccupancyShare()) {
                    failedThisSeason.add(nicheId + "(low_occupancy)");
                } else if (!state.persistent && season - state.firstSeason >= thresholds.minimumPersistenceSeasons() && state.maxSpeciesSupport < thresholds.minimumSpeciesSupport()) {
                    failedThisSeason.add(nicheId + "(low_species_support)");
                }
            }

            for (String persistentId : persistentNovel) {
                if (!nicheOccupancy.containsKey(persistentId)) {
                    retiredPersistent.add(persistentId);
                }
            }

            List<String> persistentList = persistentNovel.stream().sorted().toList();
            bySeason.add(new SeasonPnnc(season, persistentList.size(), seasonCandidates, persistentList, failedThisSeason));
            trend.add(persistentList.size());
        }

        List<String> failedCandidates = new ArrayList<>();
        for (Map.Entry<String, CandidateState> entry : candidates.entrySet()) {
            CandidateState state = entry.getValue();
            if (state.persistent) {
                continue;
            }
            if (state.noveltyDistance < thresholds.noveltyDistanceThreshold()) {
                failedCandidates.add(entry.getKey() + "(insufficient_novelty_distance=" + fmt(state.noveltyDistance) + ")");
            } else if (state.seasonsObserved < thresholds.minimumPersistenceSeasons()) {
                failedCandidates.add(entry.getKey() + "(short_lived=" + state.seasonsObserved + " seasons)");
            } else if (state.maxOccupancy < thresholds.minimumOccupancyShare()) {
                failedCandidates.add(entry.getKey() + "(occupancy=" + fmt(state.maxOccupancy) + ")");
            } else if (state.maxSpeciesSupport < thresholds.minimumSpeciesSupport()) {
                failedCandidates.add(entry.getKey() + "(species_support=" + state.maxSpeciesSupport + ")");
            }
        }

        int current = trend.isEmpty() ? 0 : trend.get(trend.size() - 1);
        return new PnncResult(
                thresholds,
                noveltyCriteria(),
                persistenceCriteria(thresholds),
                bySeason,
                current,
                trend,
                persistentNovel.stream().limit(5).toList(),
                failedCandidates,
                retiredPersistent.stream().toList(),
                lifespans,
                interpretation(current));
    }

    private Map<String, Double> buildSignature(Map<String, Object> snapshot, double occupancyShare, int speciesSupport) {
        Map<String, Double> signature = new LinkedHashMap<>();
        signature.put("occupancy", occupancyShare);
        signature.put("speciesSupport", Math.min(1.0D, speciesSupport / 8.0D));
        signature.put("triggerProfile", hashFeature(labelMax(snapshot.get("triggers"))));
        signature.put("mechanicProfile", hashFeature(labelMax(snapshot.get("mechanics"))));
        signature.put("regulatoryGateProfile", hashFeature(labelMax(snapshot.get("openGates"))));
        signature.put("environmentAffinity", hashFeature(labelMax(snapshot.get("lineages"))));
        signature.put("persistenceStyle", hashFeature(labelMax(snapshot.get("mutations"))));
        signature.put("supportCombatRole", supportCombatRatio(snapshot.get("families")));
        signature.put("memoryEnvironmentRole", memoryRole(snapshot));
        signature.put("branchTendency", hashFeature(labelMax(snapshot.get("branches"))));
        return signature;
    }

    @SuppressWarnings("unchecked")
    private String labelMax(Object raw) {
        if (!(raw instanceof Map<?, ?> map) || map.isEmpty()) {
            return "none";
        }
        String best = "none";
        int max = Integer.MIN_VALUE;
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
            int value = entry.getValue() instanceof Number n ? n.intValue() : 0;
            if (value > max) {
                max = value;
                best = String.valueOf(entry.getKey());
            }
        }
        return best;
    }

    private double supportCombatRatio(Object rawFamilies) {
        if (!(rawFamilies instanceof Map<?, ?> map) || map.isEmpty()) {
            return 0.5D;
        }
        double support = 0.0D;
        double combat = 0.0D;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey()).toLowerCase(Locale.ROOT);
            double value = entry.getValue() instanceof Number n ? n.doubleValue() : 0.0D;
            if (key.contains("support") || key.contains("utility") || key.contains("heal")) {
                support += value;
            } else {
                combat += value;
            }
        }
        if (support + combat <= 0.0D) {
            return 0.5D;
        }
        return support / (support + combat);
    }

    @SuppressWarnings("unchecked")
    private double memoryRole(Map<String, Object> snapshot) {
        Map<String, Integer> triggers = (Map<String, Integer>) snapshot.getOrDefault("triggers", Map.of());
        Map<String, Integer> mechanics = (Map<String, Integer>) snapshot.getOrDefault("mechanics", Map.of());
        double mem = 0.0D;
        double total = 0.0D;
        for (Map.Entry<String, Integer> entry : triggers.entrySet()) {
            total += entry.getValue();
            if (entry.getKey().contains("memory")) {
                mem += entry.getValue();
            }
        }
        for (Map.Entry<String, Integer> entry : mechanics.entrySet()) {
            total += entry.getValue();
            if (entry.getKey().contains("memory") || entry.getKey().contains("echo")) {
                mem += entry.getValue();
            }
        }
        return total <= 0.0D ? 0.0D : mem / total;
    }

    private double minDistance(Map<String, Double> signature, List<Map<String, Double>> known) {
        if (known.isEmpty()) {
            return 1.0D;
        }
        double min = Double.MAX_VALUE;
        for (Map<String, Double> candidate : known) {
            double sum = 0.0D;
            for (String key : signature.keySet()) {
                sum += Math.abs(signature.getOrDefault(key, 0.0D) - candidate.getOrDefault(key, 0.0D));
            }
            min = Math.min(min, sum / Math.max(1, signature.size()));
        }
        return min;
    }

    private double hashFeature(String key) {
        int hash = Math.abs(key.hashCode() % 1000);
        return hash / 1000.0D;
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private List<String> noveltyCriteria() {
        return List.of(
                "Novel niche signatures are compared to previously accepted niche signatures via normalized signature distance.",
                "Signature dimensions: trigger profile, mechanic profile, regulatory gate profile, environmental affinity, persistence style, support-vs-combat role, memory/environment role, branch tendency, occupancy share, species support.",
                "A niche is treated as novel only when minimum signature distance exceeds the novelty threshold; tiny label churn is filtered out.");
    }

    private List<String> persistenceCriteria(Thresholds t) {
        return List.of(
                "minimum persistence seasons >= " + t.minimumPersistenceSeasons(),
                "minimum occupancy share >= " + t.minimumOccupancyShare(),
                "minimum species support >= " + t.minimumSpeciesSupport());
    }

    private String interpretation(int pnnc) {
        if (pnnc <= 0) return "PNNC=0: ecosystem is generating temporary novelty but no durable novel niches.";
        if (pnnc <= 2) return "PNNC=1-2: weak bounded novelty with limited durable ecological expansion.";
        if (pnnc <= 5) return "PNNC=3-5: genuine durable ecological expansion is present.";
        return "PNNC>5: strong persistent novelty; open-ended ecological dynamics are plausible.";
    }
}
