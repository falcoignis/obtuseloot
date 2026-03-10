package obtuseloot.simulation.worldlab;

import obtuseloot.abilities.AbilityDefinition;
import obtuseloot.abilities.AbilityProfile;
import obtuseloot.artifacts.Artifact;
import obtuseloot.species.ArtifactSpecies;

import java.util.*;

public class SpeciesNicheAnalyticsEngine {
    public record PenaltyResult(double effectiveScore, double crowdingPenalty, String nicheId, boolean applied) {}
    public record CoEvolutionPressureResult(double effectiveScore,
                                            double modifier,
                                            double competitionPressure,
                                            double supportPressure,
                                            double migrationPressure,
                                            String dominantCompetitor,
                                            String dominantSupport,
                                            boolean applied) {}

    private static final double TARGET_OCCUPANCY = 0.18D;
    private static final double BETA = 0.4D;
    private static final double MIN_PENALTY = 1.0D;
    private static final double MAX_PENALTY = 1.15D;
    private static final double MAX_COEVOLUTION_MODIFIER = 0.08D;
    private static final double MAX_NICHE_BIAS = 0.05D;

    private final Random random;
    private final Map<String, NicheProfile> niches = new LinkedHashMap<>();
    private final Map<Long, ArtifactNicheMembership> artifactMembership = new HashMap<>();
    private final Map<String, Map<String, Integer>> seasonSpeciesByNiche = new LinkedHashMap<>();
    private final Map<String, Integer> speciesBirthSeason = new LinkedHashMap<>();
    private final Map<String, Integer> speciesDeathSeason = new LinkedHashMap<>();
    private final Map<String, Integer> speciesDominantNicheShifts = new LinkedHashMap<>();
    private final Map<String, Integer> speciesCoEvolutionMigrationShifts = new LinkedHashMap<>();
    private final Map<String, String> lastSpeciesDominantNiche = new LinkedHashMap<>();
    private final Map<String, SpeciesSignal> speciesSignals = new LinkedHashMap<>();
    private final Map<String, PairSignal> pairSignals = new LinkedHashMap<>();
    private final List<Double> coEvolutionCompetitionTimeline = new ArrayList<>();
    private final List<Double> coEvolutionSupportTimeline = new ArrayList<>();
    private final List<Double> coEvolutionModifierTimeline = new ArrayList<>();
    private final List<Double> nicheMigrationPressureTimeline = new ArrayList<>();
    private final List<Double> dominantSpeciesConcentrationTimeline = new ArrayList<>();
    private final Map<Integer, Integer> nicheEmergenceEvents = new LinkedHashMap<>();
    private final List<Double> dominantNicheShareTimeline = new ArrayList<>();
    private final List<Double> penaltyActivationTimeline = new ArrayList<>();
    private final List<Integer> speciesCountTimeline = new ArrayList<>();
    private final List<Integer> nicheCountTimeline = new ArrayList<>();
    private int activePenaltyCount;
    private int penaltyEvaluationCount;
    private int coEvolutionEvaluationCount;
    private double coEvolutionCompetitionTotal;
    private double coEvolutionSupportTotal;
    private double coEvolutionModifierTotal;
    private double coEvolutionMigrationPressureTotal;

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
        String speciesId = species.speciesId();
        double[] vector = featureVector(artifact, profile, successful, crowdingPenalty);
        String nicheId = assignNiche(vector, season, speciesId);
        updatePairSignals(speciesId, nicheId, successful);
        artifactMembership.put(artifact.getArtifactSeed(), new ArtifactNicheMembership(nicheId, vector, successful));
        NicheProfile niche = niches.get(nicheId);
        niche.observe(vector, successful, speciesId);
        observeSpeciesSignal(speciesId, artifact, successful);
        speciesBirthSeason.putIfAbsent(speciesId, season);
    }

    public CoEvolutionPressureResult applyCoEvolutionPressure(Artifact artifact, double score) {
        coEvolutionEvaluationCount++;
        String speciesId = artifact.getSpeciesId() == null ? "unknown" : artifact.getSpeciesId();
        ArtifactNicheMembership membership = artifactMembership.get(artifact.getArtifactSeed());
        if (membership == null || !speciesSignals.containsKey(speciesId)) {
            return new CoEvolutionPressureResult(score, 0.0D, 0.0D, 0.0D, 0.0D, "none", "none", false);
        }

        RelationshipScore relationship = dominantRelationship(speciesId);
        double migrationPressure = migrationPressure(speciesId, membership.nicheId());
        double modifier = clamp((relationship.support() * 0.06D) - (relationship.competition() * 0.07D) - (migrationPressure * 0.03D),
                -MAX_COEVOLUTION_MODIFIER,
                MAX_COEVOLUTION_MODIFIER);
        coEvolutionCompetitionTotal += relationship.competition();
        coEvolutionSupportTotal += relationship.support();
        coEvolutionModifierTotal += modifier;
        coEvolutionMigrationPressureTotal += migrationPressure;

        return new CoEvolutionPressureResult(
                score * (1.0D + modifier),
                modifier,
                relationship.competition(),
                relationship.support(),
                migrationPressure,
                relationship.competitor(),
                relationship.supporter(),
                Math.abs(modifier) > 0.0001D);
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
        coEvolutionCompetitionTimeline.add(avg(coEvolutionCompetitionTotal, coEvolutionEvaluationCount));
        coEvolutionSupportTimeline.add(avg(coEvolutionSupportTotal, coEvolutionEvaluationCount));
        coEvolutionModifierTimeline.add(avg(coEvolutionModifierTotal, coEvolutionEvaluationCount));
        nicheMigrationPressureTimeline.add(avg(coEvolutionMigrationPressureTotal, coEvolutionEvaluationCount));
        dominantSpeciesConcentrationTimeline.add(dominantShare(speciesPopulation(artifacts)));

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("nicheOccupancy", occupancy);
        snapshot.put("speciesPerNiche", speciesByNiche);
        snapshot.put("nicheCount", niches.size());
        snapshot.put("activeSpecies", activeSpecies.size());
        snapshot.put("dominantNicheShare", dominantShare(occupancy));
        snapshot.put("crowdingPenaltyActivationRate", activePenaltyRate());
        snapshot.put("coEvolutionCompetitionPressure", avg(coEvolutionCompetitionTotal, coEvolutionEvaluationCount));
        snapshot.put("coEvolutionSupportPressure", avg(coEvolutionSupportTotal, coEvolutionEvaluationCount));
        snapshot.put("coEvolutionModifier", avg(coEvolutionModifierTotal, coEvolutionEvaluationCount));
        snapshot.put("coEvolutionMigrationPressure", avg(coEvolutionMigrationPressureTotal, coEvolutionEvaluationCount));
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
        out.put("coEvolutionCompetitionTimeline", coEvolutionCompetitionTimeline);
        out.put("coEvolutionSupportTimeline", coEvolutionSupportTimeline);
        out.put("coEvolutionModifierTimeline", coEvolutionModifierTimeline);
        out.put("coEvolutionMigrationPressureTimeline", nicheMigrationPressureTimeline);
        out.put("dominantSpeciesConcentrationTimeline", dominantSpeciesConcentrationTimeline);
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
        out.put("coEvolutionMigrationCounts", new LinkedHashMap<>(speciesCoEvolutionMigrationShifts));
        return out;
    }

    public Map<String, Object> buildCoEvolutionRelationships(List<Artifact> artifacts) {
        List<Map<String, Object>> competition = strongestPairs(true);
        List<Map<String, Object>> support = strongestPairs(false);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sampleSize", artifacts.size());
        out.put("competitiveRelationships", competition);
        out.put("supportiveRelationships", support);
        out.put("nicheMigrationPressure", avg(coEvolutionMigrationPressureTotal, coEvolutionEvaluationCount));
        out.put("averageCompetitionPressure", avg(coEvolutionCompetitionTotal, coEvolutionEvaluationCount));
        out.put("averageSupportPressure", avg(coEvolutionSupportTotal, coEvolutionEvaluationCount));
        out.put("averageModifier", avg(coEvolutionModifierTotal, coEvolutionEvaluationCount));
        out.put("speciesPersistenceDelta", persistenceDelta());
        out.put("dominantAttractorConcentration", dominantShare(speciesPopulation(artifacts)));
        out.put("speciesDiversity", shannon(speciesPopulation(artifacts)));
        out.put("coOccurrenceNetworkSize", pairSignals.size());
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

    private String assignNiche(double[] vector, int season, String speciesId) {
        if (niches.isEmpty()) {
            return createNiche(vector, season);
        }
        int targetNiches = Math.max(3, Math.min(7, (int) Math.round(Math.sqrt(artifactMembership.size() / 40.0D + 1.0D))));
        Map<String, Double> scores = new LinkedHashMap<>();
        String bestNiche = null;
        double best = -1.0D;
        for (Map.Entry<String, NicheProfile> entry : niches.entrySet()) {
            double score = cosine(vector, entry.getValue().centroid()) + nicheCoEvolutionBias(speciesId, entry.getValue());
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

    private double nicheCoEvolutionBias(String speciesId, NicheProfile nicheProfile) {
        if (speciesId == null || speciesId.isBlank()) {
            return 0.0D;
        }
        int nicheTotal = Math.max(1, nicheProfile.speciesUse.values().stream().mapToInt(Integer::intValue).sum());
        double bias = 0.0D;
        for (Map.Entry<String, Integer> entry : nicheProfile.speciesUse.entrySet()) {
            if (entry.getKey().equals(speciesId)) {
                continue;
            }
            double presence = entry.getValue() / (double) nicheTotal;
            double support = supportScore(speciesId, entry.getKey());
            double competition = competitionScore(speciesId, entry.getKey());
            bias += presence * ((support * 0.04D) - (competition * 0.05D));
        }
        return clamp(bias, -MAX_NICHE_BIAS, MAX_NICHE_BIAS);
    }

    private void observeSpeciesSignal(String speciesId, Artifact artifact, boolean successful) {
        SpeciesSignal signal = speciesSignals.computeIfAbsent(speciesId, ignored -> new SpeciesSignal());
        signal.observations++;
        if (successful) {
            signal.successes++;
        }
        addToken(signal.triggers, artifact.getLastTriggerProfile());
        addToken(signal.mechanics, artifact.getLastMechanicProfile());
        addToken(signal.branches, artifact.getLastAbilityBranchPath());
        addToken(signal.environments, artifact.getDriftAlignment() + ":" + artifact.getEvolutionPath());
    }

    private void updatePairSignals(String speciesId, String nicheId, boolean successful) {
        NicheProfile niche = niches.get(nicheId);
        if (niche == null || niche.speciesUse.isEmpty()) {
            return;
        }
        for (String other : niche.speciesUse.keySet()) {
            if (other.equals(speciesId)) {
                continue;
            }
            PairSignal pair = pairSignals.computeIfAbsent(pairKey(speciesId, other), ignored -> new PairSignal(speciesId, other));
            pair.coOccurrences++;
            if (successful) {
                pair.successesWhenTogether++;
            }
        }
    }

    private RelationshipScore dominantRelationship(String speciesId) {
        double bestCompetition = 0.0D;
        String competitor = "none";
        double bestSupport = 0.0D;
        String supporter = "none";
        for (String other : speciesSignals.keySet()) {
            if (other.equals(speciesId)) {
                continue;
            }
            double competition = competitionScore(speciesId, other);
            double support = supportScore(speciesId, other);
            if (competition > bestCompetition) {
                bestCompetition = competition;
                competitor = other;
            }
            if (support > bestSupport) {
                bestSupport = support;
                supporter = other;
            }
        }
        return new RelationshipScore(bestCompetition, bestSupport, competitor, supporter);
    }

    private double competitionScore(String speciesA, String speciesB) {
        SpeciesSignal a = speciesSignals.get(speciesA);
        SpeciesSignal b = speciesSignals.get(speciesB);
        if (a == null || b == null) {
            return 0.0D;
        }
        PairSignal pair = pairSignals.get(pairKey(speciesA, speciesB));
        double overlap = (jaccard(a.triggers, b.triggers) + jaccard(a.mechanics, b.mechanics) + jaccard(a.branches, b.branches)) / 3.0D;
        double coOccurrence = pair == null ? 0.0D : pair.coOccurrences / (double) Math.max(1, Math.min(a.observations, b.observations));
        double mixedSuccessLift = pair == null ? 0.0D : (pair.successesWhenTogether / (double) Math.max(1, pair.coOccurrences))
                - ((a.successRate() + b.successRate()) / 2.0D);
        return clamp((overlap * 0.6D) + (coOccurrence * 0.4D) - (Math.max(0.0D, mixedSuccessLift) * 0.3D), 0.0D, 1.0D);
    }

    private double supportScore(String speciesA, String speciesB) {
        SpeciesSignal a = speciesSignals.get(speciesA);
        SpeciesSignal b = speciesSignals.get(speciesB);
        if (a == null || b == null) {
            return 0.0D;
        }
        PairSignal pair = pairSignals.get(pairKey(speciesA, speciesB));
        double envOverlap = jaccard(a.environments, b.environments);
        double branchDistance = 1.0D - jaccard(a.branches, b.branches);
        double mixedSuccessLift = pair == null ? 0.0D : (pair.successesWhenTogether / (double) Math.max(1, pair.coOccurrences))
                - ((a.successRate() + b.successRate()) / 2.0D);
        double coOccurrence = pair == null ? 0.0D : pair.coOccurrences / (double) Math.max(1, Math.min(a.observations, b.observations));
        return clamp((Math.max(0.0D, mixedSuccessLift) * 0.7D) + (envOverlap * 0.15D) + (branchDistance * 0.1D) + (coOccurrence * 0.05D), 0.0D, 1.0D);
    }

    private double migrationPressure(String speciesId, String nicheId) {
        NicheProfile niche = niches.get(nicheId);
        if (niche == null) {
            return 0.0D;
        }
        int total = Math.max(1, niche.speciesUse.values().stream().mapToInt(Integer::intValue).sum());
        double occupancy = occupancyFor(nicheId);
        double pressure = 0.0D;
        for (Map.Entry<String, Integer> entry : niche.speciesUse.entrySet()) {
            if (entry.getKey().equals(speciesId)) {
                continue;
            }
            pressure += (entry.getValue() / (double) total) * competitionScore(speciesId, entry.getKey());
        }
        if (pressure > 0.35D && occupancy > TARGET_OCCUPANCY) {
            speciesCoEvolutionMigrationShifts.merge(speciesId, 1, Integer::sum);
        }
        return clamp(pressure * occupancy, 0.0D, 1.0D);
    }

    private List<Map<String, Object>> strongestPairs(boolean competition) {
        List<Map<String, Object>> relationships = new ArrayList<>();
        for (PairSignal pair : pairSignals.values()) {
            double score = competition ? competitionScore(pair.speciesA, pair.speciesB) : supportScore(pair.speciesA, pair.speciesB);
            if (score < 0.05D) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("pair", pair.speciesA + "<->" + pair.speciesB);
            item.put("score", score);
            item.put("coOccurrences", pair.coOccurrences);
            item.put("survivalTogether", pair.successesWhenTogether / (double) Math.max(1, pair.coOccurrences));
            relationships.add(item);
        }
        relationships.sort((left, right) -> Double.compare(((Number) right.get("score")).doubleValue(), ((Number) left.get("score")).doubleValue()));
        return relationships.size() > 8 ? relationships.subList(0, 8) : relationships;
    }

    private Map<String, Integer> speciesPopulation(List<Artifact> artifacts) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Artifact artifact : artifacts) {
            counts.merge(artifact.getSpeciesId() == null ? "unknown" : artifact.getSpeciesId(), 1, Integer::sum);
        }
        return counts;
    }

    private Map<String, Double> persistenceDelta() {
        Map<String, Double> deltas = new LinkedHashMap<>();
        for (String speciesId : speciesSignals.keySet()) {
            SpeciesSignal signal = speciesSignals.get(speciesId);
            double withSupport = 0.0D;
            double isolated = signal.successRate();
            int count = 0;
            for (String other : speciesSignals.keySet()) {
                if (speciesId.equals(other)) {
                    continue;
                }
                withSupport += supportScore(speciesId, other);
                count++;
            }
            deltas.put(speciesId, (count == 0 ? 0.0D : withSupport / count) - isolated);
        }
        return deltas;
    }

    private double jaccard(Map<String, Integer> left, Map<String, Integer> right) {
        Set<String> keys = new LinkedHashSet<>(left.keySet());
        keys.addAll(right.keySet());
        if (keys.isEmpty()) {
            return 0.0D;
        }
        int intersect = 0;
        for (String key : keys) {
            if (left.containsKey(key) && right.containsKey(key)) {
                intersect++;
            }
        }
        return intersect / (double) keys.size();
    }

    private void addToken(Map<String, Integer> target, String csv) {
        if (csv == null || csv.isBlank()) {
            return;
        }
        for (String raw : csv.split("[,]")) {
            String token = raw.trim();
            if (!token.isBlank()) {
                target.merge(token, 1, Integer::sum);
            }
        }
    }

    private String pairKey(String speciesA, String speciesB) {
        return speciesA.compareTo(speciesB) < 0 ? speciesA + "|" + speciesB : speciesB + "|" + speciesA;
    }

    private double shannon(Map<String, Integer> distribution) {
        int total = distribution.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return 0.0D;
        }
        double value = 0.0D;
        for (int count : distribution.values()) {
            if (count <= 0) {
                continue;
            }
            double p = count / (double) total;
            value -= p * Math.log(p);
        }
        return value;
    }

    private double avg(double total, int count) {
        return count == 0 ? 0.0D : total / count;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
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
    private static final class PairSignal {
        private final String speciesA;
        private final String speciesB;
        private int coOccurrences;
        private int successesWhenTogether;

        private PairSignal(String speciesA, String speciesB) {
            this.speciesA = speciesA;
            this.speciesB = speciesB;
        }
    }

    private static final class SpeciesSignal {
        private int observations;
        private int successes;
        private final Map<String, Integer> triggers = new LinkedHashMap<>();
        private final Map<String, Integer> mechanics = new LinkedHashMap<>();
        private final Map<String, Integer> branches = new LinkedHashMap<>();
        private final Map<String, Integer> environments = new LinkedHashMap<>();

        private double successRate() {
            return successes / (double) Math.max(1, observations);
        }
    }

    private record RelationshipScore(double competition, double support, String competitor, String supporter) {}

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
