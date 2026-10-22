package obtuseloot.evolution;

import obtuseloot.abilities.AbilityMechanic;
import obtuseloot.abilities.AbilityTrigger;
import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;
import obtuseloot.ecosystem.EnvironmentPressureEngine;
import obtuseloot.evolution.params.EvolutionParameterRegistry;
import obtuseloot.evolution.params.EcosystemTuningProfile;
import obtuseloot.lineage.LineageRegistry;

import java.util.EnumMap;
import java.util.Map;

public class ExperienceEvolutionEngine {
    private final ArtifactUsageTracker usageTracker;
    private final EnvironmentPressureEngine pressureEngine;
    private final AdaptiveSupportAllocator supportAllocator;
    private final EvolutionParameterRegistry parameterRegistry;

    public ExperienceEvolutionEngine(ArtifactUsageTracker usageTracker, ArtifactFitnessEvaluator fitnessEvaluator) {
        this(usageTracker, fitnessEvaluator, new EnvironmentPressureEngine(), new AdaptiveSupportAllocator(), new EvolutionParameterRegistry());
    }

    public ExperienceEvolutionEngine(ArtifactUsageTracker usageTracker,
                                     ArtifactFitnessEvaluator fitnessEvaluator,
                                     EnvironmentPressureEngine pressureEngine) {
        this(usageTracker, fitnessEvaluator, pressureEngine, new AdaptiveSupportAllocator(), new EvolutionParameterRegistry());
    }

    public ExperienceEvolutionEngine(ArtifactUsageTracker usageTracker,
                                     ArtifactFitnessEvaluator fitnessEvaluator,
                                     EnvironmentPressureEngine pressureEngine,
                                     AdaptiveSupportAllocator supportAllocator) {
        this(usageTracker, fitnessEvaluator, pressureEngine, supportAllocator, new EvolutionParameterRegistry());
    }

    public ExperienceEvolutionEngine(ArtifactUsageTracker usageTracker,
                                     ArtifactFitnessEvaluator fitnessEvaluator,
                                     EnvironmentPressureEngine pressureEngine,
                                     AdaptiveSupportAllocator supportAllocator,
                                     EvolutionParameterRegistry parameterRegistry) {
        this.usageTracker = usageTracker;
        this.pressureEngine = pressureEngine;
        this.supportAllocator = supportAllocator;
        this.parameterRegistry = parameterRegistry;
        this.supportAllocator.setCompetitionReinforcementCurve(parameterRegistry.profile().competitionReinforcementCurve());
    }

    public ArtifactGenome applyExperienceFeedback(ArtifactGenome genome, long artifactSeed) {
        ArtifactNicheProfile nicheProfile = usageTracker.nichePopulationTracker().nicheProfile(artifactSeed);
        int nichePopulation = usageTracker.nichePopulationTracker().rollups()
                .getOrDefault(nicheProfile.dominantNiche(), new NicheUtilityRollup(nicheProfile.dominantNiche(), 1, 0L, 0L, 0.0D, 1.0D))
                .activeArtifacts();
        return applyExperienceFeedback(genome, artifactSeed, nichePopulation);
    }

    public ArtifactGenome applyExperienceFeedback(ArtifactGenome genome, long artifactSeed, int nichePopulation) {
        return applyExperienceFeedback(genome, artifactSeed, nichePopulation, 1.0D, null, null);
    }

    public ArtifactGenome applyExperienceFeedback(ArtifactGenome genome,
                                                  long artifactSeed,
                                                  int nichePopulation,
                                                  double lineageMutationInfluence) {
        return applyExperienceFeedback(genome, artifactSeed, nichePopulation, lineageMutationInfluence, null, null);
    }

    public ArtifactGenome applyExperienceFeedback(ArtifactGenome genome,
                                                  long artifactSeed,
                                                  int nichePopulation,
                                                  double lineageMutationInfluence,
                                                  String lineageId,
                                                  LineageRegistry lineageRegistry) {
        return applyExperienceFeedback(genome, artifactSeed, nichePopulation, lineageMutationInfluence,
                lineageId, lineageRegistry, usageTracker.profileForSeed(artifactSeed));
    }

    public ArtifactGenome applyExperienceFeedback(ArtifactGenome genome, long artifactSeed, int nichePopulation,
                                                   double lineageMutationInfluence, String lineageId,
                                                   LineageRegistry lineageRegistry, ArtifactUsageProfile usage) {
        EnumMap<GenomeTrait, Double> adjusted = new EnumMap<>(GenomeTrait.class);
        for (Map.Entry<GenomeTrait, Double> entry : genome.traits().entrySet()) {
            // Local outcomes select parents; macro context cannot identify a direction for every gene.
            // The separate trait-specific environmental transform remains for its own evaluation.
            double environmental = pressureEngine.multiplierFor(entry.getKey());
            adjusted.put(entry.getKey(), clamp01(entry.getValue() * environmental));
        }
        return new ArtifactGenome(genome.seed(), adjusted, genome.latentTraits(), genome.activatedLatentTraits());
    }

    public AdaptiveSupportAllocation adaptiveSupportFor(long artifactSeed, String lineageId, LineageRegistry lineageRegistry) {
        if (lineageRegistry == null) {
            return AdaptiveSupportAllocation.neutral();
        }
        return supportAllocator.allocateFor(artifactSeed, lineageId, usageTracker, lineageRegistry);
    }

    public double ecologyModifierFor(long artifactSeed, AbilityMechanic mechanic, AbilityTrigger trigger) {
        return ecologyModifierFor(artifactSeed, mechanic, trigger, null, null);
    }

    public double ecologyModifierFor(long artifactSeed,
                                     AbilityMechanic mechanic,
                                     AbilityTrigger trigger,
                                     String lineageId,
                                     LineageRegistry lineageRegistry) {
        EcosystemTuningProfile profile = parameterRegistry.profile();
        RolePressureMetrics artifactPressure = usageTracker.nichePopulationTracker().pressureFor(artifactSeed);
        AdaptiveSupportAllocation support = adaptiveSupportFor(artifactSeed, lineageId, lineageRegistry);
        EcosystemRoleClassifier classifier = new EcosystemRoleClassifier();
        ArtifactNicheProfile candidate = classifier.classify(Map.of(
                mechanic.name() + "@" + trigger.name(),
                new MechanicUtilitySignal(mechanic.name() + "@" + trigger.name(), 0.8D, 0.7D, 0.7D, 0.1D, 0.1D, 0.1D, 1L, 1L, 1.0D)
        ));
        Map<MechanicNicheTag, NicheUtilityRollup> rollups = usageTracker.nichePopulationTracker().rollups();
        NicheUtilityRollup candidateRollup = rollups.getOrDefault(candidate.dominantNiche(), new NicheUtilityRollup(candidate.dominantNiche(), 1, 1L, 1L, 0.8D, 1.0D));
        RolePressureMetrics candidatePressure = new EcosystemSaturationModel().pressureFor(
                candidate.dominantNiche(),
                candidateRollup,
                rollups.isEmpty() ? Map.of(candidate.dominantNiche(), candidateRollup) : rollups);
        double specializationTilt = candidate.specialization().specializationScore() * 0.14D;
        double behaviorTilt = behaviorOpportunityTilt(candidate.dominantNiche(), mechanic, trigger);
        return clamp((artifactPressure.templateWeightModifier() * candidatePressure.templateWeightModifier() + specializationTilt)
                * support.retentionOpportunity() * profile.nicheSaturationSensitivity() * behaviorTilt, 0.60D, 1.52D);
    }

    public EnvironmentPressureEngine pressureEngine() {
        return pressureEngine;
    }

    public void setTelemetryEmitter(obtuseloot.telemetry.EcosystemTelemetryEmitter telemetryEmitter) {
        supportAllocator.setTelemetryEmitter(telemetryEmitter);
    }

    public double mutationEcologyPressureFor(long artifactSeed) {
        return mutationEcologyPressureFor(artifactSeed, null, null);
    }

    public double mutationEcologyPressureFor(long artifactSeed, String lineageId, LineageRegistry lineageRegistry) {
        EcosystemTuningProfile profile = parameterRegistry.profile();
        RolePressureMetrics pressure = usageTracker.nichePopulationTracker().pressureFor(artifactSeed);
        AdaptiveSupportAllocation support = adaptiveSupportFor(artifactSeed, lineageId, lineageRegistry);
        return clamp((1.0D
                + ((pressure.specializationPressure() - 1.0D) * 0.22D)
                + (pressure.ecologicalRepulsion() * 0.24D)
                + Math.max(0.0D, 1.0D - pressure.templateWeightModifier()) * 0.30D)
                * (1.0D + Math.max(0.0D, 1.0D - support.mutationOpportunity()) * 0.35D)
                * (1.0D + (1.0D - support.diminishingReturnFactor()) * 0.18D),
                profile.mutationAmplitudeMin(),
                profile.mutationAmplitudeMax());
    }

    public Map<String, Object> competitionAnalytics(LineageRegistry lineageRegistry) {
        return supportAllocator.analyticsSnapshot(usageTracker, lineageRegistry);
    }

    private double behaviorOpportunityTilt(MechanicNicheTag niche, AbilityMechanic mechanic, AbilityTrigger trigger) {
        double tilt = switch (niche) {
            case NAVIGATION -> (trigger == AbilityTrigger.ON_CHUNK_ENTER || trigger == AbilityTrigger.ON_STRUCTURE_DISCOVERY) ? 1.10D : 1.04D;
            case FARMING_WORLDKEEPING -> (trigger == AbilityTrigger.ON_BLOCK_HARVEST || trigger == AbilityTrigger.ON_RESOURCE_HARVEST_STREAK) ? 1.10D : 1.03D;
            case RITUAL_STRANGE_UTILITY -> (trigger == AbilityTrigger.ON_RITUAL_INTERACT || trigger == AbilityTrigger.ON_RITUAL_COMPLETION || trigger == AbilityTrigger.ON_REPEATED_BLOCK_PATTERN) ? 1.11D : 1.03D;
            case SOCIAL_WORLD_INTERACTION -> (trigger == AbilityTrigger.ON_PLAYER_GROUP_ACTION || trigger == AbilityTrigger.ON_PLAYER_TRADE || trigger == AbilityTrigger.ON_PLAYER_WITNESS) ? 1.12D : 1.04D;
            case ENVIRONMENTAL_ADAPTATION, ENVIRONMENTAL_SENSING -> (trigger == AbilityTrigger.ON_WEATHER_CHANGE || trigger == AbilityTrigger.ON_ELEVATION_CHANGE || trigger == AbilityTrigger.ON_BIOME_CHANGE) ? 1.10D : 1.02D;
            default -> 1.0D;
        };
        if (mechanic == AbilityMechanic.TRADE_SCENT || mechanic == AbilityMechanic.COLLECTIVE_RELAY || mechanic == AbilityMechanic.RITUAL_STABILIZATION
                || mechanic == AbilityMechanic.TRAIL_SENSE || mechanic == AbilityMechanic.FORAGER_MEMORY
                || mechanic == AbilityMechanic.PATTERN_RESONANCE || mechanic == AbilityMechanic.CARTOGRAPHERS_ECHO
                || mechanic == AbilityMechanic.WITNESS_IMPRINT) {
            tilt += 0.02D;
        }
        return clamp(tilt, 0.96D, 1.14D);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0D, 1.0D);
    }
}
