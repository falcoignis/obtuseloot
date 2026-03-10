package obtuseloot.simulation.worldlab;

import obtuseloot.abilities.AbilityDefinition;
import obtuseloot.abilities.AbilityProfile;
import obtuseloot.artifacts.Artifact;
import obtuseloot.species.ArtifactSpecies;

import java.util.*;

public class SpeciesNicheAnalyticsEngine {
    public record PenaltyResult(double effectiveScore, double crowdingPenalty, String nicheId, boolean applied) {}

    private static final double TARGET_OCCUPANCY = 0.18D;
    private static final double BETA = 0.4D;
    private static final double MIN_PENALTY = 1.0D;
    private static final double MAX_PENALTY = 1.15D;

    private final Random random;
    private final Map<String, NicheProfile> niches = new LinkedHashMap<>();
    private final Map<Long, ArtifactNicheMembership> artifactMembership = new HashMap<>();
    private final Map<String, Map<String, Integer>> seasonSpeciesByNiche = new LinkedHashMap<>();
    private final Map<String, Integer> speciesBirthSeason = new LinkedHashMap<>();
    private final Map<String, Integer> speciesDeathSeason = new LinkedHashMap<>();
    private final Map<String, Integer> speciesDominantNicheShifts = new LinkedHashMap<>();
    private final Map<String, String> lastSpeciesDominantNiche = new LinkedHashMap<>();
    private final Map<Integer, Integer> nicheEmergenceEvents = new LinkedHashMap<>();
    private final List<Double> dominantNicheShareTimeline = new ArrayList<>();
    private final List<Double> penaltyActivationTimeline = new ArrayList<>();
    private final List<Integer> speciesCountTimeline = new ArrayList<>();
    private final List<Integer> nicheCountTimeline = new ArrayList<>();
    private int activePenaltyCount;
    private int penaltyEvaluationCount;

    public SpeciesNicheAnalyticsEngine(long seed) {
        this.random = new Random(seed ^ 0xBADC0FFEE0DDF00DL);
    }

    public PenaltyResult applyCrowdingPenalty(Artifact artifact, double rawScore) {
        penaltyEvaluationCount++;
        ArtifactNicheMembership membership = artifactMembership.get(artifact.getArtifactSeed());
        if (membership == null || membership.nicheId() == null || membership.nicheId().isBlank()) {
            return new PenaltyResult(rawScore, 1.0D, "unassigned", false);
        }
        String nicheId = membership.nicheId();
        double occupancy = occupancyFor(nicheId);
        if (occupancy <= TARGET_OCCUPANCY) {
            return new PenaltyResult(rawScore, 1.0D, nicheId, false);
        }
        double penalty = 1.0D + BETA * Math.max(0.0D, occupancy - TARGET_OCCUPANCY);
        penalty = Math.max(MIN_PENALTY, Math.min(MAX_PENALTY, penalty));
        activePenaltyCount++;
        return new PenaltyResult(rawScore / penalty, penalty, nicheId, true);
    }

    public void observeArtifact(Artifact artifact,
                                ArtifactSpecies species,
                                AbilityProfile profile,
                                boolean successful,
                                int season,
                                double crowdingPenalty) {
        double[] vector = featureVector(artifact, profile, successful, crowdingPenalty);
        String nicheId = assignNiche(vector, season);
        artifactMembership.put(artifact.getArtifactSeed(), new ArtifactNicheMembership(nicheId, vector, successful));
        NicheProfile niche = niches.get(nicheId);
        niche.observe(vector, successful, species.speciesId());
        speciesBirthSeason.putIfAbsent(species.speciesId(), season);
    }

    public Map<String, Object> closeSeason(int season, List<Artifact> artifacts) {
        Map<String, Integer> occupancy = new LinkedHashMap<>();
        Map<String, Integer> speciesByNiche = new LinkedHashMap<>();
        Map<String, Set<String>> speciesSets = new LinkedHashMap<>();
        Set<String> activeSpecies = new LinkedHashSet<>();

        for (Artifact artifact : artifacts) {
            String speciesId = artifact.getSpeciesId() == null ? "unknown" : artifact.getSpeciesId();
            activeSpecies.add(speciesId);
            ArtifactNicheMembership membership = artifactMembership.get(artifact.getArtifactSeed());
            String nicheId = membership == null ? "unassigned" : membership.nicheId();
            occupancy.merge(nicheId, 1, Integer::sum);
            speciesSets.computeIfAbsent(nicheId, ignored -> new LinkedHashSet<>()).add(speciesId);
        }
        for (Map.Entry<String, Set<String>> entry : speciesSets.entrySet()) {
            speciesByNiche.put(entry.getKey(), entry.getValue().size());
        }

        for (String speciesId : new ArrayList<>(speciesBirthSeason.keySet())) {
            if (!activeSpecies.contains(speciesId) && !speciesDeathSeason.containsKey(speciesId)) {
                speciesDeathSeason.put(speciesId, season);
            }
        }

        Map<String, String> dominantBySpecies = dominantNicheBySpecies(artifacts);
        for (Map.Entry<String, String> entry : dominantBySpecies.entrySet()) {
            String prev = lastSpeciesDominantNiche.put(entry.getKey(), entry.getValue());
            if (prev != null && !prev.equals(entry.getValue())) {
                speciesDominantNicheShifts.merge(entry.getKey(), 1, Integer::sum);
            }
        }

        seasonSpeciesByNiche.put("season-" + season, speciesByNiche);
        speciesCountTimeline.add(activeSpecies.size());
        nicheCountTimeline.add(niches.size());
        dominantNicheShareTimeline.add(dominantShare(occupancy));
        penaltyActivationTimeline.add(activePenaltyRate());

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("nicheOccupancy", occupancy);
        snapshot.put("speciesPerNiche", speciesByNiche);
        snapshot.put("nicheCount", niches.size());
        snapshot.put("activeSpecies", activeSpecies.size());
        snapshot.put("dominantNicheShare", dominantShare(occupancy));
        snapshot.put("crowdingPenaltyActivationRate", activePenaltyRate());
        return snapshot;
    }

    public Map<String, Object> buildSpeciationSummary(Map<String, ArtifactSpecies> speciesRegistry,
                                                      Map<String, Integer> lineageCounts,
                                                      int seasonCount) {
        Map<String, Integer> speciesPerLineage = new LinkedHashMap<>();
        List<Double> divergenceLevels = new ArrayList<>();
        for (ArtifactSpecies species : speciesRegistry.values()) {
            speciesPerLineage.merge(species.originLineageId(), 1, Integer::sum);
            divergenceLevels.add(species.divergenceSnapshot().getOrDefault("compatibility", 0.0D));
        }
        int births = speciesBirthSeason.size();
        int extinctions = speciesDeathSeason.size();
        int totalSpecies = Math.max(1, speciesRegistry.size());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("activeSpecies", totalSpecies);
        out.put("speciesPerLineage", speciesPerLineage);
        out.put("speciesDivergenceLevels", divergenceLevels);
        out.put("speciesBirthRate", births / (double) Math.max(1, seasonCount));
        out.put("speciesExtinctionRate", extinctions / (double) Math.max(1, seasonCount));
        out.put("dominantSpeciesConcentration", dominantSpeciesConcentration(lineageCounts));
        out.put("speciesNicheOccupancy", new LinkedHashMap<>(seasonSpeciesByNiche));
        out.put("nicheCountTimeline", nicheCountTimeline);
        out.put("speciesCountTimeline", speciesCountTimeline);
        return out;
    }

    public Map<String, Object> buildSpeciesNicheMap(List<Artifact> artifacts) {
        Map<String, Map<String, Integer>> speciesNicheCounts = new LinkedHashMap<>();
        Map<String, Integer> nicheOccupancy = new LinkedHashMap<>();
        for (Artifact artifact : artifacts) {
            String speciesId = artifact.getSpeciesId() == null ? "unknown" : artifact.getSpeciesId();
            String nicheId = Optional.ofNullable(artifactMembership.get(artifact.getArtifactSeed())).map(ArtifactNicheMembership::nicheId).orElse("unassigned");
            speciesNicheCounts.computeIfAbsent(speciesId, ignored -> new LinkedHashMap<>()).merge(nicheId, 1, Integer::sum);
            nicheOccupancy.merge(nicheId, 1, Integer::sum);
        }

        Map<String, Integer> dominatingSpeciesByNiche = new LinkedHashMap<>();
        Map<String, Integer> competingSpeciesPerNiche = new LinkedHashMap<>();
        for (String nicheId : nicheOccupancy.keySet()) {
            int dominantCount = 0;
            int competitors = 0;
            for (Map<String, Integer> speciesMap : speciesNicheCounts.values()) {
                int count = speciesMap.getOrDefault(nicheId, 0);
                if (count > 0) {
                    competitors++;
                }
                dominantCount = Math.max(dominantCount, count);
            }
            dominatingSpeciesByNiche.put(nicheId, dominantCount);
            competingSpeciesPerNiche.put(nicheId, competitors);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("speciesNicheCounts", speciesNicheCounts);
        out.put("nicheOccupancy", nicheOccupancy);
        out.put("dominatingSpeciesByNiche", dominatingSpeciesByNiche);
        out.put("competingSpeciesPerNiche", competingSpeciesPerNiche);
        out.put("nicheEmergenceEvents", nicheEmergenceEvents);
        out.put("speciesMigrationCounts", new LinkedHashMap<>(speciesDominantNicheShifts));
        return out;
    }

    public Map<String, Object> buildCrowdingDistribution(List<Artifact> artifacts) {
        Map<String, Double> occupancy = new LinkedHashMap<>();
        Map<String, Double> speciesFraction = new LinkedHashMap<>();
        Map<String, Double> successFraction = new LinkedHashMap<>();
        Map<String, Integer> populationByNiche = new LinkedHashMap<>();
        Map<String, Integer> speciesByNiche = new LinkedHashMap<>();
        Map<String, Integer> successByNiche = new LinkedHashMap<>();
        Map<String, Set<String>> speciesSets = new LinkedHashMap<>();

        for (Artifact artifact : artifacts) {
            ArtifactNicheMembership membership = artifactMembership.get(artifact.getArtifactSeed());
            String nicheId = membership == null ? "unassigned" : membership.nicheId();
            populationByNiche.merge(nicheId, 1, Integer::sum);
            speciesSets.computeIfAbsent(nicheId, ignored -> new LinkedHashSet<>()).add(artifact.getSpeciesId());
            if (membership != null && membership.successful()) {
                successByNiche.merge(nicheId, 1, Integer::sum);
            }
        }
        int totalArtifacts = Math.max(1, artifacts.size());
        int totalSpecies = Math.max(1, speciesBirthSeason.size());
        int totalSuccess = Math.max(1, successByNiche.values().stream().mapToInt(Integer::intValue).sum());

        for (Map.Entry<String, Integer> entry : populationByNiche.entrySet()) {
            String nicheId = entry.getKey();
            int pop = entry.getValue();
            int species = speciesSets.getOrDefault(nicheId, Set.of()).size();
            int success = successByNiche.getOrDefault(nicheId, 0);
            occupancy.put(nicheId, pop / (double) totalArtifacts);
            speciesFraction.put(nicheId, species / (double) totalSpecies);
            successFraction.put(nicheId, success / (double) totalSuccess);
            speciesByNiche.put(nicheId, species);
        }

        long overcrowded = occupancy.values().stream().filter(v -> v > TARGET_OCCUPANCY).count();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("occupancyByNiche", occupancy);
        out.put("speciesFractionByNiche", speciesFraction);
        out.put("successFractionByNiche", successFraction);
        out.put("speciesPerNiche", speciesByNiche);
        out.put("penaltyActivationFrequency", activePenaltyRate());
        out.put("overcrowdedNicheCount", overcrowded);
        out.put("targetOccupancy", TARGET_OCCUPANCY);
        out.put("beta", BETA);
        return out;
    }

    public int nicheCount() {
        return niches.size();
    }

    public double activePenaltyRate() {
        if (penaltyEvaluationCount == 0) {
            return 0.0D;
        }
        return activePenaltyCount / (double) penaltyEvaluationCount;
    }

    private String assignNiche(double[] vector, int season) {
        if (niches.isEmpty()) {
            return createNiche(vector, season);
        }
        int targetNiches = Math.max(3, Math.min(7, (int) Math.round(Math.sqrt(artifactMembership.size() / 40.0D + 1.0D))));
        Map<String, Double> scores = new LinkedHashMap<>();
        String bestNiche = null;
        double best = -1.0D;
        for (Map.Entry<String, NicheProfile> entry : niches.entrySet()) {
            double score = cosine(vector, entry.getValue().centroid());
            scores.put(entry.getKey(), score);
            if (score > best) {
                best = score;
                bestNiche = entry.getKey();
            }
        }

        if (best < 0.72D && niches.size() < targetNiches) {
            return createNiche(vector, season);
        }
        return probabilisticPick(scores);
    }

    private String createNiche(double[] vector, int season) {
        String nicheId = "niche-" + (niches.size() + 1);
        niches.put(nicheId, new NicheProfile(nicheId, vector));
        nicheEmergenceEvents.merge(season, 1, Integer::sum);
        return nicheId;
    }

    private String probabilisticPick(Map<String, Double> scores) {
        if (scores.isEmpty()) {
            return "unassigned";
        }
        double total = 0.0D;
        Map<String, Double> weighted = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            double weight = Math.exp(Math.max(-4.0D, Math.min(4.0D, entry.getValue() * 3.0D)));
            weighted.put(entry.getKey(), weight);
            total += weight;
        }
        double roll = random.nextDouble() * total;
        double cursor = 0.0D;
        for (Map.Entry<String, Double> entry : weighted.entrySet()) {
            cursor += entry.getValue();
            if (roll <= cursor) {
                return entry.getKey();
            }
        }
        return weighted.keySet().iterator().next();
    }

    private double occupancyFor(String nicheId) {
        if (artifactMembership.isEmpty()) {
            return 0.0D;
        }
        long occupied = artifactMembership.values().stream().filter(m -> nicheId.equals(m.nicheId())).count();
        return occupied / (double) artifactMembership.size();
    }

    private Map<String, String> dominantNicheBySpecies(List<Artifact> artifacts) {
        Map<String, Map<String, Integer>> counters = new LinkedHashMap<>();
        for (Artifact artifact : artifacts) {
            String speciesId = artifact.getSpeciesId() == null ? "unknown" : artifact.getSpeciesId();
            String nicheId = Optional.ofNullable(artifactMembership.get(artifact.getArtifactSeed())).map(ArtifactNicheMembership::nicheId).orElse("unassigned");
            counters.computeIfAbsent(speciesId, ignored -> new LinkedHashMap<>()).merge(nicheId, 1, Integer::sum);
        }
        Map<String, String> dominant = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : counters.entrySet()) {
            String niche = entry.getValue().entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("unassigned");
            dominant.put(entry.getKey(), niche);
        }
        return dominant;
    }

    private double dominantSpeciesConcentration(Map<String, Integer> lineageCounts) {
        int total = lineageCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0 || lineageCounts.isEmpty()) {
            return 0.0D;
        }
        int dominant = lineageCounts.values().stream().max(Integer::compareTo).orElse(0);
        return dominant / (double) total;
    }

    private double dominantShare(Map<String, Integer> map) {
        int total = map.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0 || map.isEmpty()) {
            return 0.0D;
        }
        int dominant = map.values().stream().max(Integer::compareTo).orElse(0);
        return dominant / (double) total;
    }

    private double[] featureVector(Artifact artifact, AbilityProfile profile, boolean successful, double crowdingPenalty) {
        int triggerHash = hashTokens(artifact.getLastTriggerProfile());
        int mechanicHash = hashTokens(artifact.getLastMechanicProfile());
        int gateHash = hashTokens(artifact.getLastOpenRegulatoryGates());
        int branchHash = hashTokens(artifact.getLastAbilityBranchPath());
        int envAffinity = hashTokens(artifact.getDriftAlignment() + ":" + artifact.getEvolutionPath());
        int survival = successful ? 100 : 0;
        return new double[]{
                normalized(triggerHash),
                normalized(mechanicHash),
                normalized(gateHash),
                normalized(branchHash),
                normalized(envAffinity),
                normalized((int) Math.round(artifact.getLastSpeciesCompatibilityDistance() * 100.0D)),
                normalized(survival),
                1.0D / Math.max(1.0D, crowdingPenalty)
        };
    }

    private int hashTokens(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Math.abs(value.hashCode() % 1000);
    }

    private double normalized(int value) {
        return Math.max(0.0D, Math.min(1.0D, value / 1000.0D));
    }

    private double cosine(double[] a, double[] b) {
        double dot = 0.0D;
        double na = 0.0D;
        double nb = 0.0D;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0.0D || nb == 0.0D) {
            return 0.0D;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private record ArtifactNicheMembership(String nicheId, double[] vector, boolean successful) {}

    private static final class NicheProfile {
        private final String nicheId;
        private final double[] centroid;
        private int observations;
        private int successes;
        private final Map<String, Integer> speciesUse = new LinkedHashMap<>();

        private NicheProfile(String nicheId, double[] initial) {
            this.nicheId = nicheId;
            this.centroid = Arrays.copyOf(initial, initial.length);
        }

        private void observe(double[] vector, boolean successful, String speciesId) {
            observations++;
            if (successful) {
                successes++;
            }
            speciesUse.merge(speciesId, 1, Integer::sum);
            double alpha = 1.0D / Math.max(2, observations);
            for (int i = 0; i < centroid.length; i++) {
                centroid[i] = centroid[i] * (1.0D - alpha) + vector[i] * alpha;
            }
        }

        private double[] centroid() {
            return centroid;
        }
    }
}
