package obtuseloot.evolution;

import obtuseloot.lineage.LineageRegistry;
import obtuseloot.telemetry.EcosystemTelemetryEmitter;
import obtuseloot.telemetry.EcosystemTelemetryEventType;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class AdaptiveSupportAllocator {
    private final EcosystemCarryingCapacityModel capacityModel = new EcosystemCarryingCapacityModel();
    private final LineageCompetitionModel lineageCompetitionModel = new LineageCompetitionModel();
    private volatile EcosystemTelemetryEmitter telemetryEmitter;
    private volatile double competitionReinforcementCurve = 1.0D;

    public void setTelemetryEmitter(EcosystemTelemetryEmitter telemetryEmitter) {
        this.telemetryEmitter = telemetryEmitter;
    }

    public void setCompetitionReinforcementCurve(double competitionReinforcementCurve) {
        this.competitionReinforcementCurve = Math.max(0.25D, competitionReinforcementCurve);
    }

    public EvolutionOpportunityPool buildPool(ArtifactUsageTracker usageTracker, LineageRegistry lineageRegistry) {
        Map<MechanicNicheTag, NicheUtilityRollup> rollups = usageTracker.nichePopulationTracker().rollups();
        AdaptiveSupportBudget budget = capacityModel.calculate(rollups);
        Map<MechanicNicheTag, NicheOpportunityAllocation> nicheAllocations = allocateNiches(rollups, budget);
        LineageMomentumPool lineagePool = lineageCompetitionModel.evaluate(
                lineageRegistry == null ? Map.of() : lineageRegistry.lineages(),
                budget);
        return new EvolutionOpportunityPool(budget, nicheAllocations, lineagePool);
    }

    public AdaptiveSupportAllocation allocateFor(long artifactSeed,
                                                 String lineageId,
                                                 ArtifactUsageTracker usageTracker,
                                                 LineageRegistry lineageRegistry) {
        EvolutionOpportunityPool pool = buildPool(usageTracker, lineageRegistry);
        ArtifactNicheProfile profile = usageTracker.nichePopulationTracker().nicheProfile(artifactSeed);
        NicheOpportunityAllocation niche = pool.nicheAllocations().getOrDefault(profile.dominantNiche(), defaultNiche(profile.dominantNiche()));
        LineageMomentumProfile lineage = lineageId == null
                ? new LineageMomentumProfile("unknown", 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D)
                : pool.lineageMomentumPool().profile(lineageId);

        double reinforcement = clamp(Math.pow(Math.max(0.001D, niche.reinforcementMultiplier() * lineage.templateSelectionWeight()), competitionReinforcementCurve), 0.55D, 1.50D);
        double mutation = clamp(niche.mutationSupport() * lineage.mutationSupportStrength(), 0.50D, 1.50D);
        double retention = clamp(niche.retentionSupport() * lineage.retentionBias(), 0.55D, 1.45D);
        double branchPersistence = clamp(lineage.branchPersistenceSupport() * (1.0D + pool.budget().turnoverPressure() * 0.20D), 0.60D, 1.55D);
        double diminishing = clamp(niche.competitionPressure() * 0.45D + (1.0D - lineage.diminishingReturns()) * 0.65D, 0.0D, 0.85D);

        AdaptiveSupportAllocation allocation = new AdaptiveSupportAllocation(
                reinforcement,
                mutation,
                retention,
                branchPersistence,
                niche.competitionPressure(),
                pool.lineageMomentumPool().displacementPressure(),
                1.0D - diminishing);
        EcosystemTelemetryEmitter emitter = telemetryEmitter;
        if (emitter != null) {
            emitter.emit(EcosystemTelemetryEventType.COMPETITION_ALLOCATION, artifactSeed, lineageId == null ? "" : lineageId, profile.dominantNiche().name(),
                    Map.of("reinforcement", String.valueOf(allocation.reinforcementMultiplier()),
                            "reinforcement_multiplier", String.valueOf(allocation.reinforcementMultiplier()),
                            "mutation", String.valueOf(allocation.mutationOpportunity()),
                            "retention", String.valueOf(allocation.retentionOpportunity()),
                            "lineage_momentum", String.valueOf(lineage.momentum()),
                            "ecology_pressure", String.valueOf(niche.competitionPressure()),
                            "opportunity_share", String.valueOf(niche.opportunityShare()),
                            "specialization_pressure", String.valueOf(niche.competitionPressure()),
                            "specialization_trajectory", String.valueOf(lineage.specializationTrajectory()),
                            "context_tags", "competition-allocation"));
        }
        return allocation;
    }

    public Map<String, Object> analyticsSnapshot(ArtifactUsageTracker usageTracker, LineageRegistry lineageRegistry) {
        EvolutionOpportunityPool pool = buildPool(usageTracker, lineageRegistry);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("carryingCapacity", pool.budget().carryingCapacity());
        out.put("capacityUtilization", pool.budget().capacityUtilization());
        out.put("saturationIndex", pool.budget().saturationIndex());
        out.put("turnoverPressure", pool.budget().turnoverPressure());
        out.put("explorationReserve", pool.budget().explorationReserve());
        out.put("nicheOpportunityAllocation", pool.nicheAllocations().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(e -> e.getKey().name(), e -> e.getValue().opportunityShare(), (a, b) -> a, LinkedHashMap::new)));
        out.put("nicheCompetitionPressure", pool.nicheAllocations().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(e -> e.getKey().name(), e -> e.getValue().competitionPressure(), (a, b) -> a, LinkedHashMap::new)));
        out.put("lineageMomentumDistribution", pool.lineageMomentumPool().momentumByLineage().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue().momentum(), (a, b) -> a, LinkedHashMap::new)));
        out.put("lineageDisplacementPressure", pool.lineageMomentumPool().displacementPressure());
        return out;
    }

    private Map<MechanicNicheTag, NicheOpportunityAllocation> allocateNiches(Map<MechanicNicheTag, NicheUtilityRollup> rollups,
                                                                              AdaptiveSupportBudget budget) {
        if (rollups.isEmpty()) {
            return Map.of(MechanicNicheTag.GENERALIST, defaultNiche(MechanicNicheTag.GENERALIST));
        }
        Map<MechanicNicheTag, Double> rawWeights = new EnumMap<>(MechanicNicheTag.class);
        double totalPopulation = rollups.values().stream().mapToInt(NicheUtilityRollup::activeArtifacts).sum();
        double avgDensity = rollups.values().stream().mapToDouble(NicheUtilityRollup::utilityDensity).average().orElse(0.0D);

        for (Map.Entry<MechanicNicheTag, NicheUtilityRollup> entry : rollups.entrySet()) {
            NicheUtilityRollup rollup = entry.getValue();
            double popShare = rollup.activeArtifacts() / Math.max(1.0D, totalPopulation);
            double utilityAdvantage = rollup.utilityDensity() - avgDensity;
            double rarityLift = clamp((0.14D - popShare) * 1.8D, 0.0D, 0.35D);
            double crowdedPenalty = clamp((popShare - 0.16D) * 1.8D, 0.0D, 0.56D);
            double specializationDiversity = clamp((1.0D - popShare) * 0.32D + rollup.outcomeYield() * 0.24D, 0.0D, 0.42D);
            double ecologicalPressure = clamp(budget.saturationIndex() * popShare * 1.05D, 0.0D, 0.50D);
            double lowYieldPenalty = clamp((0.34D - rollup.outcomeYield()) * 0.75D, 0.0D, 0.28D);
            double raw = Math.max(0.05D,
                    1.0D
                            + (utilityAdvantage * 0.80D)
                            + rarityLift
                            + specializationDiversity
                            - crowdedPenalty
                            - ecologicalPressure
                            - lowYieldPenalty);
            rawWeights.put(entry.getKey(), raw);
        }

        double rawTotal = rawWeights.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<MechanicNicheTag, NicheOpportunityAllocation> out = new EnumMap<>(MechanicNicheTag.class);
        for (Map.Entry<MechanicNicheTag, NicheUtilityRollup> entry : rollups.entrySet()) {
            double share = rawWeights.get(entry.getKey()) / Math.max(0.0001D, rawTotal);
            double popShare = entry.getValue().activeArtifacts() / Math.max(1.0D, totalPopulation);
            double pressure = clamp(Math.max(0.0D, popShare - share) + budget.saturationIndex() * 0.18D, 0.0D, 0.88D);
            double diminishing = 1.0D - (pressure / (pressure + 0.58D));
            double reinforcement = clamp((0.68D + share * 1.32D) * diminishing, 0.48D, 1.40D);
            double mutation = clamp((0.72D + share * 1.08D) * (0.85D + entry.getValue().outcomeYield() * 0.30D), 0.52D, 1.38D);
            double retention = clamp((0.76D + share * 1.05D) * diminishing + entry.getValue().utilityDensity() * 0.12D, 0.52D, 1.42D);
            out.put(entry.getKey(), new NicheOpportunityAllocation(
                    entry.getKey(),
                    share,
                    reinforcement,
                    mutation,
                    retention,
                    pressure,
                    reinforcement,
                    mutation,
                    retention));
        }
        return Map.copyOf(out);
    }

    private NicheOpportunityAllocation defaultNiche(MechanicNicheTag niche) {
        return new NicheOpportunityAllocation(niche, 1.0D, 1.0D, 1.0D, 1.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
