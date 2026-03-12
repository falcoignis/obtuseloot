package obtuseloot.evolution;

import obtuseloot.abilities.AbilityMechanic;
import obtuseloot.abilities.AbilityTrigger;
import obtuseloot.abilities.genome.ArtifactGenome;
import obtuseloot.abilities.genome.GenomeTrait;
import obtuseloot.ecosystem.EnvironmentPressureEngine;
import obtuseloot.lineage.LineageRegistry;

import java.util.EnumMap;
import java.util.Map;

public class ExperienceEvolutionEngine {
    private static final double ADJUSTMENT_CAP = 0.10D;

    private final ArtifactUsageTracker usageTracker;
    private final ArtifactFitnessEvaluator fitnessEvaluator;
    private final EnvironmentPressureEngine pressureEngine;
    private final AdaptiveSupportAllocator supportAllocator;

    public ExperienceEvolutionEngine(ArtifactUsageTracker usageTracker, ArtifactFitnessEvaluator fitnessEvaluator) {
        this(usageTracker, fitnessEvaluator, new EnvironmentPressureEngine(), new AdaptiveSupportAllocator());
    }

    public ExperienceEvolutionEngine(ArtifactUsageTracker usageTracker,
                                     ArtifactFitnessEvaluator fitnessEvaluator,
                                     EnvironmentPressureEngine pressureEngine) {
        this(usageTracker, fitnessEvaluator, pressureEngine, new AdaptiveSupportAllocator());
    }

    public ExperienceEvolutionEngine(ArtifactUsageTracker usageTracker,
                                     ArtifactFitnessEvaluator fitnessEvaluator,
                                     EnvironmentPressureEngine pressureEngine,
                                     AdaptiveSupportAllocator supportAllocator) {
        this.usageTracker = usageTracker;
        this.fitnessEvaluator = fitnessEvaluator;
        this.pressureEngine = pressureEngine;
        this.supportAllocator = supportAllocator;
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
        ArtifactUsageProfile usage = usageTracker.profileForSeed(artifactSeed);
        RolePressureMetrics pressure = usageTracker.nichePopulationTracker().pressureFor(artifactSeed);
        AdaptiveSupportAllocation support = adaptiveSupportFor(artifactSeed, lineageId, lineageRegistry);
        double fitness = fitnessEvaluator.evaluate(usage);
        double effectiveFitness = fitnessEvaluator.effectiveFitness(fitnessEvaluator.effectiveFitness(fitness, nichePopulation), pressure);
        effectiveFitness *= support.reinforcementMultiplier();
        double normalized = normalizeFitness(effectiveFitness);
        double lineageInfluence = clamp(lineageMutationInfluence, 0.75D, 1.25D) * support.mutationOpportunity();

        EnumMap<GenomeTrait, Double> adjusted = new EnumMap<>(GenomeTrait.class);
        EnumMap<GenomeTrait, Double> latentAdjusted = new EnumMap<>(GenomeTrait.class);
        for (Map.Entry<GenomeTrait, Double> entry : genome.traits().entrySet()) {
            double multiplier = traitMultiplier(entry.getKey(), normalized) * ecoTraitBias(pressure, lineageInfluence, support);
            double environmental = pressureEngine.multiplierFor(entry.getKey());
            adjusted.put(entry.getKey(), clamp01(entry.getValue() * multiplier * environmental));
            latentAdjusted.put(entry.getKey(), clamp01(genome.latentTrait(entry.getKey()) * (1.0D + ((multiplier - 1.0D) * 0.35D))));
        }
        return new ArtifactGenome(genome.seed(), adjusted, latentAdjusted, genome.activatedLatentTraits());
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
        double specializationTilt = candidate.specialization().specializationScore() * 0.12D;
        return clamp((artifactPressure.templateWeightModifier() * candidatePressure.templateWeightModifier() + specializationTilt)
                * support.retentionOpportunity(), 0.62D, 1.55D);
    }

    public EnvironmentPressureEngine pressureEngine() {
        return pressureEngine;
    }

    public double mutationEcologyPressureFor(long artifactSeed) {
        return mutationEcologyPressureFor(artifactSeed, null, null);
    }

    public double mutationEcologyPressureFor(long artifactSeed, String lineageId, LineageRegistry lineageRegistry) {
        RolePressureMetrics pressure = usageTracker.nichePopulationTracker().pressureFor(artifactSeed);
        AdaptiveSupportAllocation support = adaptiveSupportFor(artifactSeed, lineageId, lineageRegistry);
        return clamp((1.0D
                + ((pressure.specializationPressure() - 1.0D) * 0.22D)
                + (pressure.ecologicalRepulsion() * 0.24D)
                + Math.max(0.0D, 1.0D - pressure.templateWeightModifier()) * 0.30D)
                * (1.0D + Math.max(0.0D, 1.0D - support.mutationOpportunity()) * 0.35D)
                * (1.0D + (1.0D - support.diminishingReturnFactor()) * 0.18D),
                0.70D,
                1.65D);
    }

    public Map<String, Object> competitionAnalytics(LineageRegistry lineageRegistry) {
        return supportAllocator.analyticsSnapshot(usageTracker, lineageRegistry);
    }

    private double ecoTraitBias(RolePressureMetrics pressure,
                                double lineageInfluence,
                                AdaptiveSupportAllocation support) {
        double supportBlend = 0.94D + (support.retentionOpportunity() * 0.04D) + (support.diminishingReturnFactor() * 0.02D);
        return clamp((0.95D + ((pressure.retentionBias() - 1.0D) * 0.35D) + (pressure.specializationPressure() * 0.08D) - (pressure.ecologicalRepulsion() * 0.10D))
                * (1.0D + ((lineageInfluence - 1.0D) * 0.25D))
                * supportBlend, 0.90D, 1.12D);
    }

    private double traitMultiplier(GenomeTrait trait, double normalizedFitness) {
        double direction = switch (trait) {
            case PRECISION_AFFINITY, MOBILITY_AFFINITY, SURVIVAL_INSTINCT, RESONANCE, STABILITY -> 1.0D;
            case CHAOS_AFFINITY, VOLATILITY, KINETIC_BIAS, MUTATION_SENSITIVITY -> 0.5D;
        };
        double delta = ADJUSTMENT_CAP * normalizedFitness * direction;
        return 1.0D + clamp(delta, -ADJUSTMENT_CAP, ADJUSTMENT_CAP);
    }

    private double normalizeFitness(double fitness) {
        return clamp(fitness / 8.0D, -1.0D, 1.0D);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0D, 1.0D);
    }
}
