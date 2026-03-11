package obtuseloot.simulation.worldlab;

import obtuseloot.abilities.AbilityDefinition;
import obtuseloot.abilities.AbilityProfile;
import obtuseloot.artifacts.Artifact;
import obtuseloot.species.ArtifactSpecies;

import java.util.*;

public class SpeciesNicheAnalyticsEngine {
    public record BehavioralProjectionConfig(boolean enabled,
                                             double traitEcologyWeight,
                                             double behaviorWeight) {
        public static BehavioralProjectionConfig defaults() {
            return new BehavioralProjectionConfig(true, 0.25D, 0.75D);
        }
    }
    public record PenaltyResult(double effectiveScore,
                                double crowdingPenalty,
                                String nicheId,
                                boolean applied,
                                double sharingLoad,
                                double sharingFactor,
                                String sharingMode) {}
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
    private static final double NICHE_ASSIGNMENT_DISTANCE_THRESHOLD = 0.17D;
    private static final double NICHE_ASSIGNMENT_MARGIN = 0.11D;
    private static final double HYSTERESIS_MARGIN = 0.10D;
    private static final int MAX_NICHES = 8;
    private static final int MIN_NICHE_OBSERVATIONS_FOR_STABILITY = 20;
    private static final int CANDIDATE_MIN_SUPPORT = 8;
    private static final int CANDIDATE_MIN_PERSISTENCE = 3;
    private static final double NICHE_MERGE_DISTANCE = 0.05D;
    private static final int NICHE_PRUNE_GRACE_SEASONS = 3;
    private static final int NICHE_VECTOR_DIMENSIONS = 12;
    private static final List<String> NICHE_DIMENSION_LABELS = List.of(
            "trigger_class_activation_distribution",
            "mechanic_usage_distribution",
            "support_action_ratio",
            "damage_action_ratio",
            "persistence_action_ratio",
            "mobility_usage_ratio",
            "environment_dependent_activation_ratio",
            "memory_driven_activation_ratio",
            "latent_trait_activation_rate",
            "activation_temporal_density",
            "encounter_persistence_behavior",
            "interaction_diversity");

    private final Random random;
    private final FitnessSharingConfig fitnessSharing;
    private final BehavioralProjectionConfig projectionConfig;
    private final AdaptiveNicheCapacityConfig adaptiveNicheCapacityConfig;
    private final Map<String, NicheProfile> niches = new LinkedHashMap<>();
    private final Map<Long, ArtifactNicheMembership> artifactMembership = new HashMap<>();
    private final Map<String, CandidateNiche> candidateNiches = new LinkedHashMap<>();
    private final Map<String, Integer> nicheLowSupportSeasons = new LinkedHashMap<>();
    private final Map<Integer, Integer> nicheMergeEvents = new LinkedHashMap<>();
    private final Map<Integer, Integer> nicheRetireEvents = new LinkedHashMap<>();
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
    private final Map<Integer, Integer> nicheExtinctionEvents = new LinkedHashMap<>();
    private final Map<String, Integer> nicheBirthSeason = new LinkedHashMap<>();
    private final Map<String, Integer> nicheDeathSeason = new LinkedHashMap<>();
    private final Map<String, Integer> nicheActiveSeasons = new LinkedHashMap<>();
    private final Map<Long, ArtifactDynamics> artifactDynamics = new HashMap<>();
    private final List<Double> nicheSeparationTimeline = new ArrayList<>();
    private final List<Double> nicheStabilityTimeline = new ArrayList<>();
    private Set<String> previousActiveNiches = new LinkedHashSet<>();
    private final List<Double> dominantNicheShareTimeline = new ArrayList<>();
    private final List<Double> penaltyActivationTimeline = new ArrayList<>();
    private final List<Double> fitnessSharingLoadTimeline = new ArrayList<>();
    private final List<Double> fitnessSharingFactorTimeline = new ArrayList<>();
    private final Map<String, Double> nicheSharingLoadByNiche = new LinkedHashMap<>();
    private final Map<String, Double> nicheSharingFactorByNiche = new LinkedHashMap<>();
    private final Map<String, Double> nicheCapacity = new LinkedHashMap<>();
    private final Map<String, List<Double>> nicheCapacityTimelineByNiche = new LinkedHashMap<>();
    private final List<Map<String, Object>> nicheCapacitySeasonAdjustments = new ArrayList<>();
    private double fitnessSharingLoadTotal;
    private double fitnessSharingFactorTotal;
    private int fitnessSharingAppliedCount;
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
        this(seed, FitnessSharingConfig.defaults(), BehavioralProjectionConfig.defaults(), AdaptiveNicheCapacityConfig.defaults());
    }

    public SpeciesNicheAnalyticsEngine(long seed, FitnessSharingConfig fitnessSharing) {
        this(seed, fitnessSharing, BehavioralProjectionConfig.defaults(), AdaptiveNicheCapacityConfig.defaults());
    }

    public SpeciesNicheAnalyticsEngine(long seed,
                                       FitnessSharingConfig fitnessSharing,
                                       BehavioralProjectionConfig projectionConfig) {
        this(seed, fitnessSharing, projectionConfig, AdaptiveNicheCapacityConfig.defaults());
    }

    public SpeciesNicheAnalyticsEngine(long seed,
                                       FitnessSharingConfig fitnessSharing,
                                       BehavioralProjectionConfig projectionConfig,
                                       AdaptiveNicheCapacityConfig adaptiveNicheCapacityConfig) {
        this.random = new Random(seed ^ 0xBADC0FFEE0DDF00DL);
        this.fitnessSharing = (fitnessSharing == null ? FitnessSharingConfig.defaults() : fitnessSharing).bounded();
        this.projectionConfig = projectionConfig == null ? BehavioralProjectionConfig.defaults() : projectionConfig;
        this.adaptiveNicheCapacityConfig = (adaptiveNicheCapacityConfig == null ? AdaptiveNicheCapacityConfig.defaults() : adaptiveNicheCapacityConfig).bounded();
    }

    public PenaltyResult applyCrowdingPenalty(Artifact artifact, double rawScore) {
        penaltyEvaluationCount++;
        ArtifactNicheMembership membership = artifactMembership.get(artifact.getArtifactSeed());
        if (membership == null || membership.nicheId() == null || membership.nicheId().isBlank()) {
            return new PenaltyResult(rawScore, 1.0D, "unassigned", false, 1.0D, 1.0D, fitnessSharing.normalizedMode());
        }
        String nicheId = membership.nicheId();
        if (!fitnessSharing.enabled()) {
            return new PenaltyResult(rawScore, 1.0D, nicheId, false, 1.0D, 1.0D, fitnessSharing.normalizedMode());
        }

        SharingComputation sharing = computeSharing(artifact, nicheId, membership);
        if (!sharing.applied()) {
            return new PenaltyResult(rawScore, 1.0D, nicheId, false, sharing.load(), sharing.factor(), sharing.mode());
        }
        activePenaltyCount++;
        fitnessSharingAppliedCount++;
        fitnessSharingLoadTotal += sharing.load();
        fitnessSharingFactorTotal += sharing.factor();
        nicheSharingLoadByNiche.merge(nicheId, sharing.load(), Double::sum);
        nicheSharingFactorByNiche.merge(nicheId, sharing.factor(), Double::sum);
        return new PenaltyResult(rawScore * sharing.factor(), 1.0D / sharing.factor(), nicheId, true, sharing.load(), sharing.factor(), sharing.mode());
    }

    private SharingComputation computeSharing(Artifact artifact, String nicheId, ArtifactNicheMembership membership) {
        String mode = fitnessSharing.normalizedMode();
        return switch (mode) {
            case "distance" -> computeDistanceSharing(artifact, nicheId, membership);
            case "niche" -> computeNicheSharing(nicheId);
            default -> computeNicheSharing(nicheId);
        };
    }

    private SharingComputation computeNicheSharing(String nicheId) {
        double occupancy = occupancyFor(nicheId);
        double effectiveOccupancy = Math.max(0.0D, occupancy - fitnessSharing.targetOccupancy());
        double load = 1.0D + (fitnessSharing.alpha() * effectiveOccupancy);
        load *= capacityLoadAdjustment(nicheId);
        return sharingFromLoad(load, occupancy > fitnessSharing.targetOccupancy(), "niche");
    }

    private SharingComputation computeDistanceSharing(Artifact artifact, String nicheId, ArtifactNicheMembership membership) {
        double radius = fitnessSharing.similarityRadius();
        double load = 1.0D;
        int neighbors = 0;
        for (Map.Entry<Long, ArtifactNicheMembership> entry : artifactMembership.entrySet()) {
            if (entry.getKey() == artifact.getArtifactSeed()) {
                continue;
            }
            ArtifactNicheMembership other = entry.getValue();
            if (other == null || other.vector() == null) {
                continue;
            }
            double distance = weightedDistance(membership.vector(), other.vector());
            if (distance <= radius) {
                double contribution = 1.0D - (distance / Math.max(0.0001D, radius));
                load += Math.max(0.0D, contribution) * fitnessSharing.alpha() * 0.5D;
                neighbors++;
            }
        }
        load *= capacityLoadAdjustment(nicheId);
        return sharingFromLoad(load, neighbors > 0, "distance");
    }

    private SharingComputation sharingFromLoad(double load, boolean active, String mode) {
        double cappedLoad = Math.max(1.0D, load);
        double rawFactor = 1.0D / cappedLoad;
        double minFactor = 1.0D - fitnessSharing.maxPenalty();
        double factor = clamp(rawFactor, minFactor, 1.0D);
        boolean applied = active && factor < 0.9999D;
        return new SharingComputation(cappedLoad, factor, mode, applied);
    }

    public void observeArtifact(Artifact artifact,
                                ArtifactSpecies species,
                                AbilityProfile profile,
                                boolean successful,
                                int season,
                                double crowdingPenalty) {
        String speciesId = species.speciesId();
        double[] vector = featureVector(artifact, profile, successful, crowdingPenalty);
        String previousNiche = Optional.ofNullable(artifactMembership.get(artifact.getArtifactSeed())).map(ArtifactNicheMembership::nicheId).orElse(null);
        String nicheId = assignNiche(vector, season, speciesId, previousNiche);
        updatePairSignals(speciesId, nicheId, successful);
        artifactMembership.put(artifact.getArtifactSeed(), new ArtifactNicheMembership(nicheId, vector, successful));
        NicheProfile niche = niches.get(nicheId);
        if (niche != null) {
            niche.observe(vector, successful, speciesId, profile, artifact, season);
        }
        observeSpeciesSignal(speciesId, artifact, successful, nicheId);
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

        performMergeAndPrune(season, occupancy);
        updateNicheCapacity(season, occupancy, speciesByNiche);
        seasonSpeciesByNiche.put("season-" + season, speciesByNiche);
        speciesCountTimeline.add(activeSpecies.size());
        nicheCountTimeline.add(niches.size());
        Set<String> activeNiches = new LinkedHashSet<>();
        for (Map.Entry<String, Integer> entry : occupancy.entrySet()) {
            String nicheId = entry.getKey();
            int count = entry.getValue();
            NicheProfile niche = niches.get(nicheId);
            if (niche == null) {
                continue;
            }
            if (count >= 5 || count >= Math.max(1, artifacts.size() / 50)) {
                activeNiches.add(nicheId);
                nicheBirthSeason.putIfAbsent(nicheId, season);
                nicheActiveSeasons.merge(nicheId, 1, Integer::sum);
            }
        }
        for (String active : activeNiches) {
            if (!previousActiveNiches.contains(active)) {
                nicheEmergenceEvents.merge(season, 1, Integer::sum);
            }
        }
        for (String prior : previousActiveNiches) {
            if (!activeNiches.contains(prior)) {
                nicheExtinctionEvents.merge(season, 1, Integer::sum);
                nicheDeathSeason.put(prior, season);
            }
        }
        previousActiveNiches = activeNiches;
        dominantNicheShareTimeline.add(dominantShare(occupancy));
        nicheSeparationTimeline.add(nicheSeparationScore());
        nicheStabilityTimeline.add(1.0D - dominantShare(occupancy));
        penaltyActivationTimeline.add(activePenaltyRate());
        fitnessSharingLoadTimeline.add(avg(fitnessSharingLoadTotal, fitnessSharingAppliedCount));
        fitnessSharingFactorTimeline.add(avg(fitnessSharingFactorTotal, fitnessSharingAppliedCount));
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
        snapshot.put("nicheSeparationScore", nicheSeparationScore());
        snapshot.put("fitnessSharingEnabled", fitnessSharing.enabled());
        snapshot.put("fitnessSharingMode", fitnessSharing.normalizedMode());
        snapshot.put("fitnessSharingAvgLoad", avg(fitnessSharingLoadTotal, fitnessSharingAppliedCount));
        snapshot.put("fitnessSharingAvgFactor", avg(fitnessSharingFactorTotal, fitnessSharingAppliedCount));
        snapshot.put("adaptiveNicheCapacityEnabled", adaptiveNicheCapacityConfig.enabled());
        snapshot.put("nicheCapacity", new LinkedHashMap<>(nicheCapacity));
        snapshot.put("nicheCapacityAvg", average(nicheCapacity.values()));
        return snapshot;
    }


    public String nicheForArtifact(long artifactSeed) {
        return Optional.ofNullable(artifactMembership.get(artifactSeed)).map(ArtifactNicheMembership::nicheId).orElse("unassigned");
    }

    public List<Double> dominantNicheShareTimeline() {
        return List.copyOf(dominantNicheShareTimeline);
    }

    public List<Double> nicheStabilityTimeline() {
        return List.copyOf(nicheStabilityTimeline);
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
        int ecologicalSpecies = Math.max(1, speciesSignals.size());
        int registrySpecies = Math.max(1, speciesRegistry.size());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("activeSpecies", ecologicalSpecies);
        out.put("registrySpecies", registrySpecies);
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
        out.put("nicheSeparationTimeline", nicheSeparationTimeline);
        out.put("nicheStabilityTimeline", nicheStabilityTimeline);
        out.put("fitnessSharingLoadTimeline", fitnessSharingLoadTimeline);
        out.put("fitnessSharingFactorTimeline", fitnessSharingFactorTimeline);
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
        out.put("nicheExtinctionEvents", new LinkedHashMap<>(nicheExtinctionEvents));
        return out;
    }

    public Map<String, Object> buildNicheQualityDiagnostics(List<Artifact> artifacts) {
        Map<String, Integer> occupancy = new LinkedHashMap<>();
        for (Artifact artifact : artifacts) {
            String nicheId = Optional.ofNullable(artifactMembership.get(artifact.getArtifactSeed())).map(ArtifactNicheMembership::nicheId).orElse("unassigned");
            occupancy.merge(nicheId, 1, Integer::sum);
        }
        double dominant = dominantShare(occupancy);
        double separation = nicheSeparationScore();
        boolean collapsed = occupancy.size() <= 1 || dominant > 0.66D || separation < 0.28D;

        Map<String, String> interpretability = new LinkedHashMap<>();
        for (NicheProfile niche : niches.values()) {
            interpretability.put(niche.nicheId, niche.interpretabilitySummary());
        }

        Map<String, Integer> branchMirror = new LinkedHashMap<>();
        Map<String, Integer> familyMirror = new LinkedHashMap<>();
        for (NicheProfile niche : niches.values()) {
            branchMirror.merge(niche.topToken(niche.branchUse), 1, Integer::sum);
            familyMirror.merge(niche.topToken(niche.familyUse), 1, Integer::sum);
        }
        boolean mirrorsBranches = dominantShare(branchMirror) > 0.65D;
        boolean mirrorsFamilies = dominantShare(familyMirror) > 0.65D;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("nicheCount", occupancy.size());
        out.put("nicheOccupancy", occupancy);
        out.put("nicheSeparationScore", separation);
        out.put("nicheCollapseWarning", collapsed ? "warning: broad niche collapse risk detected" : "none");
        out.put("nicheInterpretability", interpretability);
        out.put("mirrorsBranches", mirrorsBranches);
        out.put("mirrorsFamilies", mirrorsFamilies);
        out.put("fragmentationWarning", occupancy.size() > MAX_NICHES ? "warning: niche fragmentation detected" : "none");
        out.put("behavioralProjectionEnabled", projectionConfig.enabled());
        out.put("traitEcologyWeight", projectionConfig.traitEcologyWeight());
        out.put("behaviorWeight", projectionConfig.behaviorWeight());
        out.put("projectionDominance", projectionConfig.behaviorWeight() >= projectionConfig.traitEcologyWeight() ? "behavior-dominated" : "trait-dominated");
        out.put("topSeparationDimensions", topSeparationDimensions());
        return out;
    }

    public Map<String, Object> buildNicheStabilityMetrics() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("nicheBirthEvents", new LinkedHashMap<>(nicheEmergenceEvents));
        out.put("nicheExtinctionEvents", new LinkedHashMap<>(nicheExtinctionEvents));
        out.put("nicheLifetimes", nicheLifetimes());
        out.put("nicheStabilityTimeline", nicheStabilityTimeline);
        out.put("fitnessSharingLoadTimeline", fitnessSharingLoadTimeline);
        out.put("fitnessSharingFactorTimeline", fitnessSharingFactorTimeline);
        out.put("nicheMigrationBySpecies", new LinkedHashMap<>(speciesDominantNicheShifts));
        out.put("nicheMergeEvents", new LinkedHashMap<>(nicheMergeEvents));
        out.put("nicheRetireEvents", new LinkedHashMap<>(nicheRetireEvents));
        return out;
    }

    public Map<String, Object> buildNichePrototypeDistribution() {
        Map<String, Object> out = new LinkedHashMap<>();
        for (NicheProfile niche : niches.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("prototype", niche.centroid());
            item.put("observations", niche.observations);
            item.put("successRate", niche.successes / (double) Math.max(1, niche.observations));
            item.put("dominantBranch", niche.topToken(niche.branchUse));
            item.put("dominantFamily", niche.topToken(niche.familyUse));
            out.put(niche.nicheId, item);
        }
        return out;
    }

    public SpeciesCleanupResult cleanupCosmeticSpecies(List<Artifact> artifacts, Map<String, ArtifactSpecies> speciesRegistry) {
        Map<String, Integer> population = speciesPopulation(artifacts);
        Map<String, String> dominantNicheBySpecies = dominantNicheBySpecies(artifacts);
        Map<String, String> categories = new LinkedHashMap<>();
        Map<String, String> reasons = new LinkedHashMap<>();
        Map<String, String> mergeTargets = new LinkedHashMap<>();
        Set<String> retiredSpecies = new LinkedHashSet<>();

        for (Map.Entry<String, ArtifactSpecies> entry : speciesRegistry.entrySet()) {
            String speciesId = entry.getKey();
            ArtifactSpecies species = entry.getValue();
            SpeciesSignal signal = speciesSignals.get(speciesId);
            int observations = Math.max(population.getOrDefault(speciesId, 0), signal == null ? 0 : signal.observations);
            int nicheCount = signal == null ? 0 : signal.niches.size();
            double dominantNicheShare = dominantShare(signal == null ? Map.of() : signal.niches);
            double divergence = species.divergenceSnapshot().getOrDefault("compatibility", 0.0D);
            double nicheDivergence = species.divergenceSnapshot().getOrDefault("nicheOccupancy", 0.0D);
            boolean root = speciesId.startsWith("species-root-");

            if (root) {
                categories.put(speciesId, "valid ecological species");
                reasons.put(speciesId, "lineage root retained as canonical fallback identity");
                continue;
            }

            if (observations >= 14 && divergence >= 0.62D && nicheDivergence >= 0.16D && dominantNicheShare >= 0.55D && nicheCount >= 1) {
                categories.put(speciesId, "valid ecological species");
                reasons.put(speciesId, "persistent occupancy and multi-axis divergence from parent lineage");
            } else if (observations >= 8 && nicheDivergence >= 0.10D && (divergence >= 0.54D || (nicheCount >= 2 && dominantNicheShare <= 0.75D))) {
                categories.put(speciesId, "weak / borderline species");
                reasons.put(speciesId, "partial divergence signal but limited persistence or niche stability");
            } else if (observations < 4 || nicheDivergence < 0.08D || (divergence < 0.46D && nicheCount <= 1)) {
                categories.put(speciesId, "cosmetic species");
                String target = resolveMergeTarget(species, speciesRegistry);
                if (target != null && !target.equals(speciesId)) {
                    mergeTargets.put(speciesId, target);
                    reasons.put(speciesId, "insufficient ecological separation; merged into parent chain");
                } else {
                    retiredSpecies.add(speciesId);
                    reasons.put(speciesId, "insufficient ecological separation; retired label");
                }
            } else {
                categories.put(speciesId, "merge candidates");
                String target = resolveMergeTarget(species, speciesRegistry);
                if (target != null && !target.equals(speciesId)) {
                    mergeTargets.put(speciesId, target);
                    reasons.put(speciesId, "borderline divergence with parent-overlap dominant niche");
                } else {
                    reasons.put(speciesId, "borderline divergence with no safe parent available");
                }
            }
        }

        for (Artifact artifact : artifacts) {
            String speciesId = artifact.getSpeciesId();
            String mapped = resolveTarget(speciesId, mergeTargets);
            if (mapped != null && !mapped.equals(speciesId)) {
                artifact.setParentSpeciesId(speciesId);
                artifact.setSpeciesId(mapped);
            }
            if (retiredSpecies.contains(artifact.getSpeciesId())) {
                ArtifactSpecies species = speciesRegistry.get(artifact.getSpeciesId());
                String fallback = species == null ? null : resolveMergeTarget(species, speciesRegistry);
                artifact.setSpeciesId(fallback == null ? artifact.getSpeciesId() : fallback);
            }
        }

        retiredSpecies.addAll(mergeTargets.keySet());
        rebuildSignalsFromArtifacts(artifacts);

        Map<String, Integer> categoryCounts = new LinkedHashMap<>();
        for (String category : categories.values()) {
            categoryCounts.merge(category, 1, Integer::sum);
        }

        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("speciesCategories", categories);
        audit.put("categoryCounts", categoryCounts);
        audit.put("speciesReasons", reasons);
        audit.put("speciesDominantNiche", dominantNicheBySpecies);
        audit.put("speciesPopulation", population);
        audit.put("mergeTargets", mergeTargets);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("mergedSpecies", new LinkedHashMap<>(mergeTargets));
        summary.put("retiredSpecies", new ArrayList<>(retiredSpecies));
        summary.put("postCleanupSpeciesCount", speciesSignals.size());

        return new SpeciesCleanupResult(audit, mergeTargets, retiredSpecies, summary);
    }

    private void rebuildSignalsFromArtifacts(List<Artifact> artifacts) {
        speciesSignals.clear();
        pairSignals.clear();
        Map<String, List<Artifact>> artifactsByNiche = new LinkedHashMap<>();
        for (Artifact artifact : artifacts) {
            String speciesId = artifact.getSpeciesId() == null ? "unknown" : artifact.getSpeciesId();
            ArtifactNicheMembership membership = artifactMembership.get(artifact.getArtifactSeed());
            String nicheId = membership == null ? "unassigned" : membership.nicheId();
            boolean successful = membership != null && membership.successful();
            observeSpeciesSignal(speciesId, artifact, successful, nicheId);
            artifactsByNiche.computeIfAbsent(nicheId, ignored -> new ArrayList<>()).add(artifact);
        }

        for (Map.Entry<String, NicheProfile> entry : niches.entrySet()) {
            entry.getValue().speciesUse.clear();
        }

        for (Map.Entry<String, List<Artifact>> entry : artifactsByNiche.entrySet()) {
            String nicheId = entry.getKey();
            List<Artifact> local = entry.getValue();
            NicheProfile niche = niches.get(nicheId);
            if (niche != null) {
                for (Artifact artifact : local) {
                    niche.speciesUse.merge(artifact.getSpeciesId(), 1, Integer::sum);
                }
            }
            for (int i = 0; i < local.size(); i++) {
                Artifact a = local.get(i);
                for (int j = i + 1; j < local.size(); j++) {
                    Artifact b = local.get(j);
                    String aId = a.getSpeciesId() == null ? "unknown" : a.getSpeciesId();
                    String bId = b.getSpeciesId() == null ? "unknown" : b.getSpeciesId();
                    if (aId.equals(bId)) {
                        continue;
                    }
                    PairSignal pair = pairSignals.computeIfAbsent(pairKey(aId, bId), ignored -> new PairSignal(aId, bId));
                    pair.coOccurrences++;
                    pair.sharedNicheOccurrences++;
                    ArtifactNicheMembership am = artifactMembership.get(a.getArtifactSeed());
                    ArtifactNicheMembership bm = artifactMembership.get(b.getArtifactSeed());
                    if ((am != null && am.successful()) || (bm != null && bm.successful())) {
                        pair.successesWhenTogether++;
                    }
                }
            }
        }
    }

    private String resolveMergeTarget(ArtifactSpecies species, Map<String, ArtifactSpecies> speciesRegistry) {
        String current = species.parentSpeciesId();
        Set<String> seen = new HashSet<>();
        while (current != null && !current.isBlank() && !"none".equals(current) && seen.add(current)) {
            if (speciesRegistry.containsKey(current)) {
                return current;
            }
            ArtifactSpecies next = speciesRegistry.get(current);
            current = next == null ? null : next.parentSpeciesId();
        }
        return null;
    }

    private String resolveTarget(String speciesId, Map<String, String> mergeTargets) {
        if (speciesId == null) {
            return null;
        }
        String current = speciesId;
        Set<String> seen = new HashSet<>();
        while (mergeTargets.containsKey(current) && seen.add(current)) {
            current = mergeTargets.get(current);
        }
        return current;
    }

    private record SharingComputation(double load, double factor, String mode, boolean applied) {}

    public record SpeciesCleanupResult(Map<String, Object> audit,
                                       Map<String, String> mergedSpecies,
                                       Set<String> retiredSpecies,
                                       Map<String, Object> summary) {}

    public Map<String, Object> buildCoEvolutionRelationships(List<Artifact> artifacts) {
        List<Map<String, Object>> competition = strongestPairs(true);
        List<Map<String, Object>> support = strongestPairs(false);
        List<Map<String, Object>> suppression = strongestSuppressionPairs();
        List<Map<String, Object>> migrationPressure = strongestMigrationPressures();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sampleSize", artifacts.size());
        out.put("competitiveRelationships", competition);
        out.put("supportiveRelationships", support);
        out.put("directedCompetitiveRelationships", strongestDirectedRelationships(true));
        out.put("directedSupportiveRelationships", strongestDirectedRelationships(false));
        out.put("suppressionRelationships", suppression);
        out.put("migrationPressureRelationships", migrationPressure);
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

        long overcrowded = occupancy.values().stream().filter(v -> v > fitnessSharing.targetOccupancy()).count();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("occupancyByNiche", occupancy);
        out.put("speciesFractionByNiche", speciesFraction);
        out.put("successFractionByNiche", successFraction);
        out.put("speciesPerNiche", speciesByNiche);
        out.put("penaltyActivationFrequency", activePenaltyRate());
        out.put("overcrowdedNicheCount", overcrowded);
        out.put("targetOccupancy", fitnessSharing.targetOccupancy());
        out.put("beta", BETA);
        out.put("fitnessSharingEnabled", fitnessSharing.enabled());
        out.put("fitnessSharingMode", fitnessSharing.normalizedMode());
        out.put("fitnessSharingAlpha", fitnessSharing.alpha());
        out.put("fitnessSharingMaxPenalty", fitnessSharing.maxPenalty());
        out.put("fitnessSharingTargetOccupancy", fitnessSharing.targetOccupancy());
        out.put("averageSharingLoad", avg(fitnessSharingLoadTotal, fitnessSharingAppliedCount));
        out.put("averageSharingFactor", avg(fitnessSharingFactorTotal, fitnessSharingAppliedCount));
        out.put("nicheSharingLoad", new LinkedHashMap<>(nicheSharingLoadByNiche));
        out.put("nicheSharingFactor", new LinkedHashMap<>(nicheSharingFactorByNiche));
        out.put("adaptiveNicheCapacityEnabled", adaptiveNicheCapacityConfig.enabled());
        out.put("adaptiveNicheCapacityBounds", Map.of("min", adaptiveNicheCapacityConfig.minCapacity(), "max", adaptiveNicheCapacityConfig.maxCapacity()));
        out.put("nicheCapacity", new LinkedHashMap<>(nicheCapacity));
        out.put("nicheCapacityTimeline", new LinkedHashMap<>(nicheCapacityTimelineByNiche));
        out.put("nicheCapacitySeasonAdjustments", new ArrayList<>(nicheCapacitySeasonAdjustments));
        return out;
    }

    private double capacityLoadAdjustment(String nicheId) {
        if (!adaptiveNicheCapacityConfig.enabled()) {
            return 1.0D;
        }
        double capacity = nicheCapacity.getOrDefault(nicheId, adaptiveNicheCapacityConfig.baselineCapacity());
        double pressure = 1.0D - (capacity - 1.0D);
        return clamp(pressure, 0.85D, 1.15D);
    }

    private void initializeNicheCapacity(String nicheId) {
        nicheCapacity.putIfAbsent(nicheId, adaptiveNicheCapacityConfig.baselineCapacity());
        nicheCapacityTimelineByNiche.computeIfAbsent(nicheId, ignored -> new ArrayList<>()).add(nicheCapacity.get(nicheId));
    }

    private void updateNicheCapacity(int season, Map<String, Integer> occupancy, Map<String, Integer> speciesByNiche) {
        if (!adaptiveNicheCapacityConfig.enabled()) {
            return;
        }
        int totalArtifacts = Math.max(1, occupancy.values().stream().mapToInt(Integer::intValue).sum());
        int maxObservedSpecies = Math.max(1, speciesByNiche.values().stream().mapToInt(Integer::intValue).max().orElse(1));
        double memoryPressure = avg(coEvolutionMigrationPressureTotal, coEvolutionEvaluationCount);

        for (String nicheId : niches.keySet()) {
            initializeNicheCapacity(nicheId);
            NicheProfile profile = niches.get(nicheId);
            int nichePop = occupancy.getOrDefault(nicheId, 0);
            double occupancyShare = nichePop / (double) totalArtifacts;
            double overcrowding = clamp((occupancyShare - fitnessSharing.targetOccupancy()) / Math.max(0.001D, fitnessSharing.targetOccupancy()), 0.0D, 1.0D);
            double diversity = clamp(speciesByNiche.getOrDefault(nicheId, 1) / (double) maxObservedSpecies, 0.0D, 1.0D);
            double persistence = clamp(nicheActiveSeasons.getOrDefault(nicheId, 1) / (double) Math.max(1, season), 0.0D, 1.0D);
            double novelty = clamp((diversity * 0.6D) + (profile == null ? 0.0D : profile.successRate() * 0.4D), 0.0D, 1.0D);
            double prolongedDominanceWithoutNovelty = clamp((occupancyShare * 1.35D) * (1.0D - novelty) + (memoryPressure * 0.35D), 0.0D, 1.0D);

            double delta = (adaptiveNicheCapacityConfig.noveltyWeight() * novelty)
                    + (adaptiveNicheCapacityConfig.diversityWeight() * diversity)
                    + (adaptiveNicheCapacityConfig.persistenceWeight() * persistence)
                    - (adaptiveNicheCapacityConfig.overcrowdingWeight() * overcrowding)
                    - (adaptiveNicheCapacityConfig.stagnationWeight() * prolongedDominanceWithoutNovelty);
            delta = clamp(delta, -adaptiveNicheCapacityConfig.maxSeasonDelta(), adaptiveNicheCapacityConfig.maxSeasonDelta());

            double before = nicheCapacity.getOrDefault(nicheId, adaptiveNicheCapacityConfig.baselineCapacity());
            double after = clamp(before + delta, adaptiveNicheCapacityConfig.minCapacity(), adaptiveNicheCapacityConfig.maxCapacity());
            nicheCapacity.put(nicheId, after);
            nicheCapacityTimelineByNiche.computeIfAbsent(nicheId, ignored -> new ArrayList<>()).add(after);

            Map<String, Object> contribution = new LinkedHashMap<>();
            contribution.put("season", season);
            contribution.put("nicheId", nicheId);
            contribution.put("before", before);
            contribution.put("after", after);
            contribution.put("delta", after - before);
            contribution.put("noveltySignal", novelty);
            contribution.put("interactionDiversity", diversity);
            contribution.put("nichePersistence", persistence);
            contribution.put("chronicOvercrowding", overcrowding);
            contribution.put("prolongedDominanceWithoutNovelty", prolongedDominanceWithoutNovelty);
            nicheCapacitySeasonAdjustments.add(contribution);
        }
    }

    public int nicheCount() {
        return niches.size();
    }

    public boolean isFitnessSharingEnabled() {
        return fitnessSharing.enabled();
    }

    public String fitnessSharingMode() {
        return fitnessSharing.normalizedMode();
    }

    public double averageFitnessSharingLoad() {
        return avg(fitnessSharingLoadTotal, fitnessSharingAppliedCount);
    }

    public String adaptiveNicheCapacitySummary() {
        if (!adaptiveNicheCapacityConfig.enabled()) {
            return "disabled";
        }
        return "enabled(bounds=" + adaptiveNicheCapacityConfig.minCapacity() + ".." + adaptiveNicheCapacityConfig.maxCapacity()
                + ", avgCapacity=" + String.format(Locale.ROOT, "%.3f", average(nicheCapacity.values())) + ")";
    }

    public double activePenaltyRate() {
        if (penaltyEvaluationCount == 0) {
            return 0.0D;
        }
        return activePenaltyCount / (double) penaltyEvaluationCount;
    }

    public Map<String, Object> behavioralProjectionSummary() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", projectionConfig.enabled());
        out.put("traitEcologyWeight", projectionConfig.traitEcologyWeight());
        out.put("behaviorWeight", projectionConfig.behaviorWeight());
        out.put("mode", projectionConfig.behaviorWeight() >= projectionConfig.traitEcologyWeight() ? "behavior-dominated" : "trait-dominated");
        out.put("dimensions", NICHE_DIMENSION_LABELS);
        out.put("topSeparationDimensions", topSeparationDimensions());
        return out;
    }

    public Map<String, Object> behavioralSignatureDistribution(List<Artifact> artifacts) {
        Map<String, Object> out = new LinkedHashMap<>();
        List<Map<String, Object>> samples = new ArrayList<>();
        double[] sums = new double[NICHE_VECTOR_DIMENSIONS];
        int count = 0;
        for (Artifact artifact : artifacts) {
            ArtifactNicheMembership membership = artifactMembership.get(artifact.getArtifactSeed());
            if (membership == null || membership.vector() == null) {
                continue;
            }
            count++;
            for (int i = 0; i < NICHE_VECTOR_DIMENSIONS; i++) {
                sums[i] += membership.vector()[i];
            }
            if (samples.size() < 8) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("artifactSeed", artifact.getArtifactSeed());
                item.put("speciesId", artifact.getSpeciesId());
                item.put("nicheId", membership.nicheId());
                item.put("vector", membership.vector());
                samples.add(item);
            }
        }
        Map<String, Double> means = new LinkedHashMap<>();
        for (int i = 0; i < NICHE_VECTOR_DIMENSIONS; i++) {
            means.put(NICHE_DIMENSION_LABELS.get(i), count == 0 ? 0.0D : clamp(sums[i] / count, 0.0D, 1.0D));
        }
        out.put("dimensions", NICHE_DIMENSION_LABELS);
        out.put("count", count);
        out.put("dimensionMeans", means);
        out.put("sampledArtifacts", samples);
        out.put("projection", behavioralProjectionSummary());
        return out;
    }

    private String assignNiche(double[] vector, int season, String speciesId, String previousNiche) {
        if (niches.isEmpty()) {
            return createNiche(vector, season);
        }
        int targetNiches = Math.max(4, Math.min(MAX_NICHES, (int) Math.round(Math.sqrt(artifactMembership.size() / 36.0D + 1.0D))));
        String bestNiche = null;
        String secondBestNiche = null;
        double bestDistance = Double.MAX_VALUE;
        double secondDistance = Double.MAX_VALUE;
        for (Map.Entry<String, NicheProfile> entry : niches.entrySet()) {
            double distance = weightedDistance(vector, entry.getValue().centroid()) - nicheCoEvolutionBias(speciesId, entry.getValue());
            if (distance < bestDistance) {
                secondDistance = bestDistance;
                secondBestNiche = bestNiche;
                bestDistance = distance;
                bestNiche = entry.getKey();
            } else if (distance < secondDistance) {
                secondDistance = distance;
                secondBestNiche = entry.getKey();
            }
        }

        boolean clearWinner = bestDistance < NICHE_ASSIGNMENT_DISTANCE_THRESHOLD
                && (secondDistance - bestDistance) > NICHE_ASSIGNMENT_MARGIN;
        if (!clearWinner) {
            String promoted = registerCandidate(vector, season, targetNiches);
            if (promoted != null) {
                return promoted;
            }
            if (previousNiche != null && niches.containsKey(previousNiche)) {
                return previousNiche;
            }
            return bestDistance < (NICHE_ASSIGNMENT_DISTANCE_THRESHOLD * 0.80D) ? bestNiche : "unassigned";
        }

        if (previousNiche != null && niches.containsKey(previousNiche) && !previousNiche.equals(bestNiche)) {
            double previousDistance = weightedDistance(vector, niches.get(previousNiche).centroid());
            if ((previousDistance - bestDistance) < HYSTERESIS_MARGIN) {
                return previousNiche;
            }
        }
        return bestNiche == null ? secondBestNiche : bestNiche;
    }

    private String createNiche(double[] vector, int season) {
        String nicheId = "niche-" + (niches.size() + 1);
        niches.put(nicheId, new NicheProfile(nicheId, vector));
        nicheBirthSeason.putIfAbsent(nicheId, season);
        initializeNicheCapacity(nicheId);
        return nicheId;
    }

    private String registerCandidate(double[] vector, int season, int targetNiches) {
        String key = candidateKey(vector);
        CandidateNiche candidate = candidateNiches.computeIfAbsent(key, ignored -> new CandidateNiche(vector, season));
        candidate.observe(vector, season);
        boolean sufficientlySeparated = nearestDistance(candidate.prototype()) > (NICHE_ASSIGNMENT_DISTANCE_THRESHOLD + 0.08D);
        boolean promotable = (candidate.support >= CANDIDATE_MIN_SUPPORT
                || candidate.persistenceSeasons() >= CANDIDATE_MIN_PERSISTENCE)
                && sufficientlySeparated;
        boolean notFamilyAlias = candidate.familyPurity() < 0.92D;
        if (promotable && sufficientlySeparated && notFamilyAlias && niches.size() < targetNiches) {
            candidateNiches.remove(key);
            return createNiche(candidate.prototype(), season);
        }
        return null;
    }

    private String candidateKey(double[] vector) {
        StringBuilder key = new StringBuilder("cand");
        for (double value : vector) {
            key.append('-').append((int) Math.round(clamp(value, 0.0D, 1.0D) * 4.0D));
        }
        return key.toString();
    }

    private double nearestDistance(double[] vector) {
        double best = Double.MAX_VALUE;
        for (NicheProfile profile : niches.values()) {
            best = Math.min(best, weightedDistance(vector, profile.centroid()));
        }
        return best == Double.MAX_VALUE ? 1.0D : best;
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

    private void observeSpeciesSignal(String speciesId, Artifact artifact, boolean successful, String nicheId) {
        SpeciesSignal signal = speciesSignals.computeIfAbsent(speciesId, ignored -> new SpeciesSignal());
        signal.observations++;
        if (successful) {
            signal.successes++;
        }
        addToken(signal.triggers, artifact.getLastTriggerProfile());
        addToken(signal.mechanics, artifact.getLastMechanicProfile());
        addToken(signal.branches, artifact.getLastAbilityBranchPath());
        addToken(signal.environments, artifact.getDriftAlignment() + ":" + artifact.getEvolutionPath());
        signal.niches.merge(nicheId, 1, Integer::sum);
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
            pair.sharedNicheOccurrences++;
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
        double sameNiche = nicheOverlap(a, b);
        double directionalDisadvantage = clamp(b.successRate() - a.successRate(), 0.0D, 1.0D);
        double mixedSuccessLift = pair == null ? 0.0D : (pair.successesWhenTogether / (double) Math.max(1, pair.coOccurrences))
                - ((a.successRate() + b.successRate()) / 2.0D);
        return clamp((overlap * 0.38D) + (coOccurrence * 0.18D) + (sameNiche * 0.32D)
                + (directionalDisadvantage * 0.22D) - (Math.max(0.0D, mixedSuccessLift) * 0.24D), 0.0D, 1.0D);
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
        double nicheComplementarity = 1.0D - nicheOverlap(a, b);
        double mixedSuccessLift = pair == null ? 0.0D : (pair.successesWhenTogether / (double) Math.max(1, pair.coOccurrences))
                - ((a.successRate() + b.successRate()) / 2.0D);
        double coOccurrence = pair == null ? 0.0D : pair.coOccurrences / (double) Math.max(1, Math.min(a.observations, b.observations));
        return clamp((Math.max(0.0D, mixedSuccessLift) * 0.55D) + (envOverlap * 0.17D)
                + (branchDistance * 0.12D) + (nicheComplementarity * 0.12D) + (coOccurrence * 0.04D), 0.0D, 1.0D);
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
            double nicheCompetition = competitionScore(speciesId, entry.getKey()) * sameNicheFraction(speciesId, entry.getKey());
            pressure += (entry.getValue() / (double) total) * nicheCompetition;
        }
        if (pressure > 0.35D && occupancy > TARGET_OCCUPANCY) {
            speciesCoEvolutionMigrationShifts.merge(speciesId, 1, Integer::sum);
        }
        return clamp(pressure * occupancy, 0.0D, 1.0D);
    }

    private double nicheOverlap(SpeciesSignal a, SpeciesSignal b) {
        return jaccard(a.niches, b.niches);
    }

    private double sameNicheFraction(String speciesA, String speciesB) {
        PairSignal pair = pairSignals.get(pairKey(speciesA, speciesB));
        if (pair == null || pair.coOccurrences == 0) {
            return 0.0D;
        }
        return clamp(pair.sharedNicheOccurrences / (double) pair.coOccurrences, 0.0D, 1.0D);
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
            item.put("sharedNicheFraction", sameNicheFraction(pair.speciesA, pair.speciesB));
            item.put("survivalTogether", pair.successesWhenTogether / (double) Math.max(1, pair.coOccurrences));
            relationships.add(item);
        }
        relationships.sort((left, right) -> Double.compare(((Number) right.get("score")).doubleValue(), ((Number) left.get("score")).doubleValue()));
        return relationships.size() > 8 ? relationships.subList(0, 8) : relationships;
    }

    private List<Map<String, Object>> strongestDirectedRelationships(boolean competition) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String speciesId : speciesSignals.keySet()) {
            RelationshipScore relationship = dominantRelationship(speciesId);
            String target = competition ? relationship.competitor() : relationship.supporter();
            double score = competition ? relationship.competition() : relationship.support();
            if (target == null || "none".equals(target) || score < 0.06D) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("from", speciesId);
            row.put("to", target);
            row.put("score", score);
            row.put("sameNicheFraction", sameNicheFraction(speciesId, target));
            out.add(row);
        }
        out.sort((l, r) -> Double.compare(((Number) r.get("score")).doubleValue(), ((Number) l.get("score")).doubleValue()));
        return out.size() > 8 ? out.subList(0, 8) : out;
    }

    private List<Map<String, Object>> strongestSuppressionPairs() {
        List<Map<String, Object>> relationships = new ArrayList<>();
        for (PairSignal pair : pairSignals.values()) {
            double suppression = clamp(competitionScore(pair.speciesA, pair.speciesB)
                    * sameNicheFraction(pair.speciesA, pair.speciesB)
                    * (1.0D - supportScore(pair.speciesA, pair.speciesB)), 0.0D, 1.0D);
            if (suppression < 0.08D) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("pair", pair.speciesA + "<->" + pair.speciesB);
            item.put("score", suppression);
            item.put("sharedNicheFraction", sameNicheFraction(pair.speciesA, pair.speciesB));
            relationships.add(item);
        }
        relationships.sort((left, right) -> Double.compare(((Number) right.get("score")).doubleValue(), ((Number) left.get("score")).doubleValue()));
        return relationships.size() > 8 ? relationships.subList(0, 8) : relationships;
    }

    private List<Map<String, Object>> strongestMigrationPressures() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String speciesId : speciesSignals.keySet()) {
            SpeciesSignal signal = speciesSignals.get(speciesId);
            String dominantNiche = dominantNiche(signal.niches);
            double pressure = migrationPressure(speciesId, dominantNiche);
            if (pressure < 0.05D) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("species", speciesId);
            item.put("dominantNiche", dominantNiche);
            item.put("pressure", pressure);
            out.add(item);
        }
        out.sort((left, right) -> Double.compare(((Number) right.get("pressure")).doubleValue(), ((Number) left.get("pressure")).doubleValue()));
        return out.size() > 8 ? out.subList(0, 8) : out;
    }

    private String dominantNiche(Map<String, Integer> nicheUsage) {
        return nicheUsage.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("unassigned");
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


    private void performMergeAndPrune(int season, Map<String, Integer> occupancy) {
        mergeSimilarNiches(season);
        Set<String> toRetire = new LinkedHashSet<>();
        for (Map.Entry<String, NicheProfile> entry : niches.entrySet()) {
            int pop = occupancy.getOrDefault(entry.getKey(), 0);
            if (pop < 2) {
                int low = nicheLowSupportSeasons.merge(entry.getKey(), 1, Integer::sum);
                if (low >= NICHE_PRUNE_GRACE_SEASONS && entry.getValue().observations < MIN_NICHE_OBSERVATIONS_FOR_STABILITY) {
                    toRetire.add(entry.getKey());
                }
            } else {
                nicheLowSupportSeasons.put(entry.getKey(), 0);
            }
        }
        for (String nicheId : toRetire) {
            retireNiche(nicheId, season);
        }
    }

    private void mergeSimilarNiches(int season) {
        boolean changed = true;
        while (changed) {
            changed = false;
            List<String> ids = new ArrayList<>(niches.keySet());
            for (int i = 0; i < ids.size() && !changed; i++) {
                for (int j = i + 1; j < ids.size(); j++) {
                    String aId = ids.get(i);
                    String bId = ids.get(j);
                    NicheProfile a = niches.get(aId);
                    NicheProfile b = niches.get(bId);
                    if (a == null || b == null) {
                        continue;
                    }
                    double distance = weightedDistance(a.centroid(), b.centroid());
            if (distance <= NICHE_MERGE_DISTANCE
                    && a.topFamilyShare() > 0.90D
                    && b.topFamilyShare() > 0.90D
                    && Objects.equals(a.topToken(a.familyUse), b.topToken(b.familyUse))) {
                        a.absorb(b);
                        niches.remove(bId);
                        remapMembership(bId, aId);
                        nicheMergeEvents.merge(season, 1, Integer::sum);
                        nicheDeathSeason.put(bId, season);
                        changed = true;
                        break;
                    }
                }
            }
        }
    }

    private void retireNiche(String nicheId, int season) {
        NicheProfile removed = niches.remove(nicheId);
        if (removed == null) {
            return;
        }
        nicheRetireEvents.merge(season, 1, Integer::sum);
        nicheDeathSeason.put(nicheId, season);
        String fallback = nearestNiche(removed.centroid());
        remapMembership(nicheId, fallback);
    }

    private String nearestNiche(double[] vector) {
        String best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Map.Entry<String, NicheProfile> entry : niches.entrySet()) {
            double distance = weightedDistance(vector, entry.getValue().centroid());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entry.getKey();
            }
        }
        return best == null ? "unassigned" : best;
    }

    private void remapMembership(String fromNiche, String toNiche) {
        for (Map.Entry<Long, ArtifactNicheMembership> entry : new ArrayList<>(artifactMembership.entrySet())) {
            ArtifactNicheMembership membership = entry.getValue();
            if (fromNiche.equals(membership.nicheId())) {
                artifactMembership.put(entry.getKey(), new ArtifactNicheMembership(toNiche, membership.vector(), membership.successful()));
            }
        }
    }

    private double avg(double total, int count) {
        return count == 0 ? 0.0D : total / count;
    }

    private double average(Collection<Double> values) {
        return values == null || values.isEmpty() ? 0.0D : values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D);
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
        double[] traitEcology = traitEcologyVector(artifact, profile, successful, crowdingPenalty);
        double[] behavioral = behavioralSignatureVector(artifact, profile, successful);
        return projectNicheVector(traitEcology, behavioral);
    }

    private double[] traitEcologyVector(Artifact artifact, AbilityProfile profile, boolean successful, double crowdingPenalty) {
        Map<String, Integer> triggerTokens = tokens(artifact.getLastTriggerProfile());
        Map<String, Integer> mechanicTokens = tokens(artifact.getLastMechanicProfile());
        Map<String, Integer> gateTokens = tokens(artifact.getLastOpenRegulatoryGates());
        Map<String, Integer> branchTokens = tokens(artifact.getLastAbilityBranchPath());

        double[] vector = new double[NICHE_VECTOR_DIMENSIONS];
        vector[0] = normalizedEntropy(triggerTokens);
        vector[1] = topShare(triggerTokens);
        vector[2] = normalizedEntropy(mechanicTokens);
        vector[3] = topShare(mechanicTokens);
        vector[4] = normalizedEntropy(gateTokens);
        vector[5] = topShare(gateTokens);
        vector[6] = affinitySignal(artifact.getDriftAlignment(), artifact.getEvolutionPath(), "chaos", "volatile", "mutation");
        vector[7] = affinitySignal(artifact.getDriftAlignment(), artifact.getEvolutionPath(), "stable", "precision", "discipline");
        vector[8] = survivalStyleSignal(artifact, profile, successful, "support");
        vector[9] = survivalStyleSignal(artifact, profile, successful, "persistence");
        vector[10] = topShare(branchTokens);
        ArtifactDynamics dynamics = artifactDynamics.computeIfAbsent(artifact.getArtifactSeed(), ignored -> new ArtifactDynamics());
        dynamics.observations++;
        if (successful) dynamics.successes++;
        vector[11] = clamp((dynamics.successes / (double) Math.max(1, dynamics.observations)) * (1.0D / Math.max(1.0D, crowdingPenalty)), 0.0D, 1.0D);
        return vector;
    }

    private double[] behavioralSignatureVector(Artifact artifact, AbilityProfile profile, boolean successful) {
        Map<String, Integer> triggerTokens = tokens(artifact.getLastTriggerProfile());
        Map<String, Integer> mechanicTokens = tokens(artifact.getLastMechanicProfile());
        Map<String, Integer> gateTokens = tokens(artifact.getLastOpenRegulatoryGates());
        Map<String, Integer> branchTokens = tokens(artifact.getLastAbilityBranchPath());
        String memory = Optional.ofNullable(artifact.getLastMemoryInfluence()).orElse("").toLowerCase(Locale.ROOT);
        String activatedLatent = Optional.ofNullable(artifact.getLastActivatedLatentTraits()).orElse("").toLowerCase(Locale.ROOT);
        String triggerCsv = Optional.ofNullable(artifact.getLastTriggerProfile()).orElse("").toLowerCase(Locale.ROOT);
        String mechanicCsv = Optional.ofNullable(artifact.getLastMechanicProfile()).orElse("").toLowerCase(Locale.ROOT);

        double[] vector = new double[NICHE_VECTOR_DIMENSIONS];
        vector[0] = normalizedEntropy(triggerTokens);
        vector[1] = normalizedEntropy(mechanicTokens);
        vector[2] = supportActionRatio(triggerCsv, mechanicCsv);
        vector[3] = damageActionRatio(triggerCsv, mechanicCsv);
        vector[4] = persistenceActionRatio(triggerCsv, mechanicCsv);
        vector[5] = mobilityUsageRatio(triggerCsv, mechanicCsv, branchTokens, profile);
        vector[6] = environmentDependentActivationRatio(gateTokens, triggerCsv, mechanicCsv);
        vector[7] = memoryDrivenActivationRatio(memory, triggerCsv, mechanicCsv, gateTokens);
        vector[8] = latentTraitActivationRate(artifact, activatedLatent);
        vector[9] = activationTemporalDensity(triggerTokens, mechanicTokens, triggerCsv, mechanicCsv);
        vector[10] = encounterPersistenceBehavior(successful, triggerCsv, mechanicCsv, gateTokens);
        vector[11] = interactionDiversitySignal(profile, triggerTokens, mechanicTokens);
        return vector;
    }

    private double[] projectNicheVector(double[] traitEcology, double[] behavioral) {
        if (!projectionConfig.enabled()) {
            return Arrays.copyOf(traitEcology, traitEcology.length);
        }
        double[] projected = new double[NICHE_VECTOR_DIMENSIONS];
        double traitWeight = clamp(projectionConfig.traitEcologyWeight(), 0.0D, 1.0D);
        double behaviorWeight = clamp(projectionConfig.behaviorWeight(), 0.0D, 1.0D);
        double sum = traitWeight + behaviorWeight;
        if (sum <= 0.0001D) {
            traitWeight = 0.35D;
            behaviorWeight = 0.65D;
            sum = 1.0D;
        }
        traitWeight /= sum;
        behaviorWeight /= sum;
        for (int i = 0; i < projected.length; i++) {
            projected[i] = clamp((traitEcology[i] * traitWeight) + (behavioral[i] * behaviorWeight), 0.0D, 1.0D);
        }
        return projected;
    }

    private double topShare(Map<String, Integer> values) {
        int total = values.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return 0.0D;
        }
        int top = values.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return clamp(top / (double) total, 0.0D, 1.0D);
    }

    private double roleCombatSignal(Artifact artifact, boolean successful) {
        String token = (Optional.ofNullable(artifact.getLastTriggerProfile()).orElse("") + ","
                + Optional.ofNullable(artifact.getLastMechanicProfile()).orElse("")).toLowerCase(Locale.ROOT);
        double support = markerShare(token, "guard", "field", "pulse", "assist", "reposition");
        double combat = markerShare(token, "kill", "burst", "retaliation", "detonation", "hit");
        double role = (support * 0.6D) + ((1.0D - combat) * 0.4D);
        return successful ? clamp(role + 0.06D, 0.0D, 1.0D) : role;
    }

    private double regulatoryPressureSignal(Map<String, Integer> gateTokens) {
        if (gateTokens.isEmpty()) {
            return 0.0D;
        }
        String joined = String.join(",", gateTokens.keySet()).toLowerCase(Locale.ROOT);
        return markerShare(joined, "cooldown", "limit", "safety", "environment", "lineage");
    }

    private double markerShare(String token, String... markers) {
        int hits = 0;
        for (String marker : markers) {
            if (token.contains(marker)) {
                hits++;
            }
        }
        return hits / (double) Math.max(1, markers.length);
    }

    private Map<String, Integer> tokens(String csv) {
        Map<String, Integer> out = new LinkedHashMap<>();
        addToken(out, csv);
        return out;
    }

    private double normalizedEntropy(Map<String, Integer> values) {
        if (values.isEmpty()) {
            return 0.0D;
        }
        int total = values.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return 0.0D;
        }
        double entropy = 0.0D;
        for (int count : values.values()) {
            double p = count / (double) total;
            entropy -= p * Math.log(p);
        }
        return clamp(entropy / Math.log(Math.max(2, values.size())), 0.0D, 1.0D);
    }

    private double affinitySignal(String drift, String evolution, String... markers) {
        String token = (drift + "," + evolution).toLowerCase(Locale.ROOT);
        for (String marker : markers) {
            if (token.contains(marker)) {
                return 1.0D;
            }
        }
        return 0.0D;
    }

    private double survivalStyleSignal(Artifact artifact, AbilityProfile profile, boolean successful, String style) {
        String triggers = Optional.ofNullable(artifact.getLastTriggerProfile()).orElse("").toLowerCase(Locale.ROOT);
        String mechanics = Optional.ofNullable(artifact.getLastMechanicProfile()).orElse("").toLowerCase(Locale.ROOT);
        return switch (style) {
            case "burst" -> scorePresence(triggers, mechanics, successful, "on_kill", "on_multi_kill", "burst_state", "unstable_detonation");
            case "sustained" -> scorePresence(triggers, mechanics, successful, "on_hit", "chain_escalation", "pulse", "retaliation");
            case "support" -> scorePresence(triggers, mechanics, successful, "on_movement", "on_reposition", "battlefield_field", "guardian_pulse");
            default -> scorePresence(triggers, mechanics, successful, "on_low_health", "on_memory_event", "recovery_window", "defensive_threshold");
        };
    }

    private double scorePresence(String triggers, String mechanics, boolean successful, String... markers) {
        int hits = 0;
        for (String marker : markers) {
            if (triggers.contains(marker) || mechanics.contains(marker)) {
                hits++;
            }
        }
        double base = hits / (double) Math.max(1, markers.length);
        return successful ? clamp(base + 0.1D, 0.0D, 1.0D) : base;
    }

    private double memoryEnvironmentSignal(Artifact artifact) {
        String memory = Optional.ofNullable(artifact.getLastMemoryInfluence()).orElse("").toLowerCase(Locale.ROOT);
        String gates = Optional.ofNullable(artifact.getLastOpenRegulatoryGates()).orElse("").toLowerCase(Locale.ROOT);
        double memorySignal = memory.isBlank() ? 0.0D : 0.6D;
        double envSignal = gates.contains("environment") ? 0.4D : 0.0D;
        return clamp(memorySignal + envSignal, 0.0D, 1.0D);
    }


    private double supportActionRatio(String triggers, String mechanics) {
        return markerShare(triggers + "," + mechanics, "guard", "assist", "field", "aura", "shield", "support", "heal");
    }

    private double damageActionRatio(String triggers, String mechanics) {
        return markerShare(triggers + "," + mechanics, "kill", "burst", "detonation", "strike", "retaliation", "damage", "hit");
    }

    private double persistenceActionRatio(String triggers, String mechanics) {
        return markerShare(triggers + "," + mechanics, "survival", "recovery", "sustain", "defensive", "window", "persistence", "stabilize");
    }

    private double environmentDependentActivationRatio(Map<String, Integer> gateTokens, String triggers, String mechanics) {
        if (gateTokens.isEmpty()) {
            return markerShare(triggers + "," + mechanics, "terrain", "weather", "environment", "biome");
        }
        int environmentHits = 0;
        for (String gate : gateTokens.keySet()) {
            String token = gate.toLowerCase(Locale.ROOT);
            if (token.contains("environment") || token.contains("weather") || token.contains("terrain") || token.contains("biome")) {
                environmentHits++;
            }
        }
        return clamp(environmentHits / (double) Math.max(1, gateTokens.size()), 0.0D, 1.0D);
    }

    private double memoryDrivenActivationRatio(String memory, String triggers, String mechanics, Map<String, Integer> gateTokens) {
        double memorySignal = memory.isBlank() ? 0.0D : 0.6D;
        double triggerSignal = markerShare(triggers + "," + mechanics, "memory", "recall", "replay", "echo", "history") * 0.25D;
        double gateSignal = gateTokens.keySet().stream().map(String::toLowerCase).anyMatch(g -> g.contains("memory") || g.contains("history")) ? 0.15D : 0.0D;
        return clamp(memorySignal + triggerSignal + gateSignal, 0.0D, 1.0D);
    }

    private double mobilityUsageRatio(String triggers,
                                      String mechanics,
                                      Map<String, Integer> branchTokens,
                                      AbilityProfile profile) {
        double triggerMechanicMobility = markerShare(triggers + "," + mechanics,
                "move", "mobility", "dash", "reposition", "teleport", "blink", "phase");
        double branchMobility = markerShare(String.join(",", branchTokens.keySet()), "mobility", "scout", "skirmish", "dash");
        double abilityMobility = 0.0D;
        if (profile != null && profile.abilities() != null && !profile.abilities().isEmpty()) {
            int mobilityAbilities = 0;
            for (AbilityDefinition definition : profile.abilities()) {
                String name = definition.id().toLowerCase(Locale.ROOT);
                if (name.contains("dash") || name.contains("teleport") || name.contains("mobility") || name.contains("reposition")) {
                    mobilityAbilities++;
                }
            }
            abilityMobility = mobilityAbilities / (double) profile.abilities().size();
        }
        return clamp((triggerMechanicMobility * 0.5D) + (branchMobility * 0.2D) + (abilityMobility * 0.3D), 0.0D, 1.0D);
    }

    private double latentTraitActivationRate(Artifact artifact, String activatedLatent) {
        double storedRate = clamp(artifact.getLastLatentActivationRate(), 0.0D, 1.0D);
        double latentSignal = activatedLatent.isBlank() || "[]".equals(activatedLatent)
                ? 0.0D
                : clamp(tokens(activatedLatent).size() / 6.0D, 0.0D, 0.35D);
        return clamp(storedRate * 0.75D + latentSignal, 0.0D, 1.0D);
    }

    private double activationTemporalDensity(Map<String, Integer> triggerTokens,
                                             Map<String, Integer> mechanicTokens,
                                             String triggers,
                                             String mechanics) {
        double concentrated = (topShare(triggerTokens) + topShare(mechanicTokens)) / 2.0D;
        double eventMarkers = markerShare(triggers + "," + mechanics,
                "on_hit", "on_kill", "on_crit", "chain", "burst", "window", "loop");
        double diversityPenalty = (normalizedEntropy(triggerTokens) + normalizedEntropy(mechanicTokens)) / 2.0D;
        return clamp((concentrated * 0.5D) + (eventMarkers * 0.35D) + ((1.0D - diversityPenalty) * 0.15D), 0.0D, 1.0D);
    }

    private double encounterPersistenceBehavior(boolean successful,
                                                String triggers,
                                                String mechanics,
                                                Map<String, Integer> gateTokens) {
        double sustainMarkers = markerShare(triggers + "," + mechanics,
                "on_low_health", "recovery_window", "defensive_threshold", "survival", "shield_window", "stabilize");
        double persistenceGateRatio = 0.0D;
        if (!gateTokens.isEmpty()) {
            long persistentGates = gateTokens.keySet().stream()
                    .map(token -> token.toLowerCase(Locale.ROOT))
                    .filter(token -> token.contains("persist") || token.contains("survival") || token.contains("recovery") || token.contains("stability"))
                    .count();
            persistenceGateRatio = persistentGates / (double) gateTokens.size();
        }
        double successPersistence = successful ? 0.12D : 0.0D;
        return clamp((sustainMarkers * 0.65D) + (persistenceGateRatio * 0.23D) + successPersistence, 0.0D, 1.0D);
    }

    private double interactionDiversitySignal(AbilityProfile profile,
                                              Map<String, Integer> triggerTokens,
                                              Map<String, Integer> mechanicTokens) {
        double triggerDiversity = normalizedEntropy(triggerTokens);
        double mechanicDiversity = normalizedEntropy(mechanicTokens);
        double abilityBreadth = 0.0D;
        if (profile != null && profile.abilities() != null && !profile.abilities().isEmpty()) {
            abilityBreadth = clamp(profile.abilities().size() / 8.0D, 0.0D, 1.0D);
        }
        return clamp((triggerDiversity * 0.35D) + (mechanicDiversity * 0.35D) + (abilityBreadth * 0.30D), 0.0D, 1.0D);
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

    private double weightedDistance(double[] a, double[] b) {
        double[] w = {1.30D, 1.30D, 1.45D, 1.45D, 1.35D, 1.30D, 1.25D, 1.25D, 1.20D, 1.20D, 1.28D, 1.22D};
        double distance = 0.0D;
        double weightSum = 0.0D;
        for (int i = 0; i < a.length; i++) {
            double wi = w[i];
            distance += wi * Math.abs(a[i] - b[i]);
            weightSum += wi;
        }
        return weightSum == 0.0D ? 1.0D : clamp(distance / weightSum, 0.0D, 1.0D);
    }

    private double weightedCosine(double[] a, double[] b) {
        double[] w = {1.25D, 1.2D, 1.3D, 1.3D, 1.2D, 1.1D, 1.1D, 1.1D, 1.05D, 1.05D, 1.15D, 1.1D};
        double dot = 0.0D;
        double na = 0.0D;
        double nb = 0.0D;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i] * w[i];
            na += a[i] * a[i] * w[i];
            nb += b[i] * b[i] * w[i];
        }
        if (na == 0.0D || nb == 0.0D) {
            return 0.0D;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private double nicheSeparationScore() {
        if (niches.size() <= 1) {
            return 0.0D;
        }
        List<NicheProfile> list = new ArrayList<>(niches.values());
        double total = 0.0D;
        int pairs = 0;
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                total += weightedDistance(list.get(i).centroid(), list.get(j).centroid());
                pairs++;
            }
        }
        return pairs == 0 ? 0.0D : total / pairs;
    }


    private List<String> topSeparationDimensions() {
        if (niches.isEmpty()) {
            return List.of();
        }
        double[] mean = new double[NICHE_VECTOR_DIMENSIONS];
        for (NicheProfile niche : niches.values()) {
            for (int i = 0; i < NICHE_VECTOR_DIMENSIONS; i++) {
                mean[i] += niche.centroid()[i];
            }
        }
        for (int i = 0; i < NICHE_VECTOR_DIMENSIONS; i++) {
            mean[i] /= Math.max(1, niches.size());
        }
        Map<String, Double> varianceByDim = new LinkedHashMap<>();
        for (int i = 0; i < NICHE_VECTOR_DIMENSIONS; i++) {
            double variance = 0.0D;
            for (NicheProfile niche : niches.values()) {
                double d = niche.centroid()[i] - mean[i];
                variance += d * d;
            }
            varianceByDim.put(NICHE_DIMENSION_LABELS.get(i), variance / Math.max(1, niches.size()));
        }
        return varianceByDim.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();
    }

    private Map<String, Integer> nicheLifetimes() {
        Map<String, Integer> lifetimes = new LinkedHashMap<>();
        for (String nicheId : nicheBirthSeason.keySet()) {
            int born = nicheBirthSeason.getOrDefault(nicheId, 1);
            int died = nicheDeathSeason.getOrDefault(nicheId, born + nicheActiveSeasons.getOrDefault(nicheId, 0));
            lifetimes.put(nicheId, Math.max(1, died - born + 1));
        }
        return lifetimes;
    }

    private record ArtifactNicheMembership(String nicheId, double[] vector, boolean successful) {}
    private static final class PairSignal {
        private final String speciesA;
        private final String speciesB;
        private int coOccurrences;
        private int sharedNicheOccurrences;
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
        private final Map<String, Integer> niches = new LinkedHashMap<>();

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
        private final Map<String, Integer> branchUse = new LinkedHashMap<>();
        private final Map<String, Integer> familyUse = new LinkedHashMap<>();

        private NicheProfile(String nicheId, double[] initial) {
            this.nicheId = nicheId;
            this.centroid = Arrays.copyOf(initial, initial.length);
        }

        private void observe(double[] vector, boolean successful, String speciesId, AbilityProfile profile, Artifact artifact, int season) {
            observations++;
            if (successful) {
                successes++;
            }
            speciesUse.merge(speciesId, 1, Integer::sum);
            branchUse.merge(normalizeBranch(artifact.getLastAbilityBranchPath()), 1, Integer::sum);
            if (profile != null) {
                for (AbilityDefinition definition : profile.abilities()) {
                    familyUse.merge(definition.family().name().toLowerCase(Locale.ROOT), 1, Integer::sum);
                }
            }
            double alpha = 1.0D / Math.max(2, observations);
            for (int i = 0; i < centroid.length; i++) {
                centroid[i] = centroid[i] * (1.0D - alpha) + vector[i] * alpha;
            }
        }

        private void absorb(NicheProfile other) {
            int total = Math.max(1, observations + other.observations);
            double ownWeight = observations / (double) total;
            double otherWeight = other.observations / (double) total;
            for (int i = 0; i < centroid.length; i++) {
                centroid[i] = (centroid[i] * ownWeight) + (other.centroid[i] * otherWeight);
            }
            observations += other.observations;
            successes += other.successes;
            mergeCounts(speciesUse, other.speciesUse);
            mergeCounts(branchUse, other.branchUse);
            mergeCounts(familyUse, other.familyUse);
        }

        private void mergeCounts(Map<String, Integer> target, Map<String, Integer> source) {
            for (Map.Entry<String, Integer> entry : source.entrySet()) {
                target.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        private double[] centroid() {
            return centroid;
        }

        private double successRate() {
            return successes / (double) Math.max(1, observations);
        }

        private String interpretabilitySummary() {
            String maturity = observations >= MIN_NICHE_OBSERVATIONS_FOR_STABILITY ? "stable" : "emergent";
            return maturity + " niche with dominant branch=" + topToken(branchUse)
                    + ", dominant family=" + topToken(familyUse)
                    + ", successRate=" + String.format(Locale.ROOT, "%.2f", successes / (double) Math.max(1, observations));
        }

        private double topFamilyShare() {
            int total = familyUse.values().stream().mapToInt(Integer::intValue).sum();
            if (total <= 0) {
                return 0.0D;
            }
            int top = familyUse.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            return top / (double) total;
        }

        private String topToken(Map<String, Integer> map) {
            return map.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("none");
        }

        private String normalizeBranch(String branchPath) {
            if (branchPath == null || branchPath.isBlank()) {
                return "none";
            }
            String[] tokens = branchPath.split("[/|>\\-]");
            return tokens.length == 0 ? branchPath : tokens[0].trim().toLowerCase(Locale.ROOT);
        }
    }

    private static final class CandidateNiche {
        private final double[] prototype;
        private int support;
        private int firstSeason;
        private int lastSeason;
        private double actionDominance;

        private CandidateNiche(double[] vector, int season) {
            this.prototype = Arrays.copyOf(vector, vector.length);
            this.support = 0;
            this.firstSeason = season;
            this.lastSeason = season;
            this.actionDominance = dominantActionAxis(vector);
        }

        private void observe(double[] vector, int season) {
            support++;
            lastSeason = season;
            double alpha = 1.0D / Math.max(2, support);
            for (int i = 0; i < prototype.length; i++) {
                prototype[i] = prototype[i] * (1.0D - alpha) + vector[i] * alpha;
            }
            actionDominance = (actionDominance * (1.0D - alpha)) + (dominantActionAxis(vector) * alpha);
        }

        private int persistenceSeasons() {
            return Math.max(1, lastSeason - firstSeason + 1);
        }

        private double familyPurity() {
            return actionDominance;
        }

        private double[] prototype() {
            return Arrays.copyOf(prototype, prototype.length);
        }

        private static double dominantActionAxis(double[] vector) {
            if (vector.length <= 4) {
                return 0.5D;
            }
            return Math.max(vector[2], Math.max(vector[3], vector[4]));
        }
    }

    private static final class ArtifactDynamics {
        private int observations;
        private int successes;
    }
}
