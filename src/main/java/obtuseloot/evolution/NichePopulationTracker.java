package obtuseloot.evolution;

import obtuseloot.telemetry.EcosystemTelemetryEmitter;
import obtuseloot.telemetry.EcosystemTelemetryEventType;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NichePopulationTracker {
    private final EcosystemRoleClassifier classifier;
    private final EcosystemSaturationModel saturationModel;
    private final Map<Long, ArtifactNicheProfile> nicheProfilesByArtifact = new ConcurrentHashMap<>();
    private final EcosystemCarryingCapacityModel carryingCapacityModel = new EcosystemCarryingCapacityModel();
    private final Map<Long, Map<String, MechanicUtilitySignal>> signalsByArtifact = new ConcurrentHashMap<>();
    private final Map<Long, Double> specializationScoreByArtifact = new ConcurrentHashMap<>();
    private final Set<Long> activeArtifacts = ConcurrentHashMap.newKeySet();
    private volatile EcosystemTelemetryEmitter telemetryEmitter;

    public NichePopulationTracker() {
        this(new EcosystemRoleClassifier(), new EcosystemSaturationModel());
    }

    public NichePopulationTracker(EcosystemRoleClassifier classifier, EcosystemSaturationModel saturationModel) {
        this.classifier = classifier;
        this.saturationModel = saturationModel;
    }

    public void setTelemetryEmitter(EcosystemTelemetryEmitter telemetryEmitter) {
        this.telemetryEmitter = telemetryEmitter;
    }

    public void markCreated(long artifactSeed) {
        activeArtifacts.add(artifactSeed);
    }

    public void markDiscarded(long artifactSeed) {
        activeArtifacts.remove(artifactSeed);
        specializationScoreByArtifact.remove(artifactSeed);
    }

    public void recordTelemetry(long artifactSeed, Map<String, MechanicUtilitySignal> signals) {
        if (signals == null || signals.isEmpty()) {
            return;
        }
        activeArtifacts.add(artifactSeed);
        ArtifactNicheProfile previous = nicheProfilesByArtifact.get(artifactSeed);
        signalsByArtifact.put(artifactSeed, Map.copyOf(signals));
        ArtifactNicheProfile next = classifier.classify(signals);
        nicheProfilesByArtifact.put(artifactSeed, next);
        double previousSpecialization = specializationScoreByArtifact.getOrDefault(artifactSeed, next.specialization().specializationScore());
        double specializationTrajectory = next.specialization().specializationScore() - previousSpecialization;
        specializationScoreByArtifact.put(artifactSeed, next.specialization().specializationScore());
        if (previous != null && previous.dominantNiche() != next.dominantNiche() && telemetryEmitter != null) {
            telemetryEmitter.emit(EcosystemTelemetryEventType.NICHE_CLASSIFICATION_CHANGE,
                    artifactSeed,
                    "",
                    next.dominantNiche().name(),
                    Map.of("from", previous.dominantNiche().name(),
                            "to", next.dominantNiche().name(),
                            "subniche", next.specialization().dominantSubniche(),
                            "specialization_pressure", String.valueOf(next.specialization().specializationScore()),
                            "specialization_trajectory", String.valueOf(specializationTrajectory),
                            "context_tags", "niche-reclassification"));
        }
    }

    public ArtifactNicheProfile nicheProfile(long artifactSeed) {
        return nicheProfilesByArtifact.getOrDefault(artifactSeed,
                new ArtifactNicheProfile(MechanicNicheTag.GENERALIST, Set.of(MechanicNicheTag.GENERALIST), Map.of(MechanicNicheTag.GENERALIST, 1.0D),
                        new NicheSpecializationProfile(MechanicNicheTag.GENERALIST, "unspecialized", 0.0D, 0.0D)));
    }

    public Map<MechanicNicheTag, NicheUtilityRollup> rollups() {
        Map<MechanicNicheTag, MutableRollup> mutable = new EnumMap<>(MechanicNicheTag.class);
        for (Long seed : activeArtifacts) {
            ArtifactNicheProfile profile = nicheProfilesByArtifact.get(seed);
            Map<String, MechanicUtilitySignal> signalMap = signalsByArtifact.get(seed);
            if (profile == null || signalMap == null || signalMap.isEmpty()) {
                continue;
            }
            for (MechanicNicheTag niche : profile.niches()) {
                MutableRollup rollup = mutable.computeIfAbsent(niche, ignored -> new MutableRollup());
                rollup.activeArtifacts++;
                for (MechanicUtilitySignal signal : signalMap.values()) {
                    rollup.attempts += signal.attempts();
                    rollup.meaningful += signal.meaningfulOutcomes();
                    rollup.validated += signal.validatedUtility();
                    rollup.budget += signal.budgetConsumed();
                }
            }
        }
        Map<MechanicNicheTag, NicheUtilityRollup> out = new EnumMap<>(MechanicNicheTag.class);
        mutable.forEach((niche, r) -> out.put(niche, new NicheUtilityRollup(niche, r.activeArtifacts, r.attempts, r.meaningful, r.validated, r.budget)));
        return out;
    }

    public RolePressureMetrics pressureFor(long artifactSeed) {
        Map<MechanicNicheTag, NicheUtilityRollup> all = rollups();
        ArtifactNicheProfile profile = nicheProfile(artifactSeed);
        NicheUtilityRollup nicheRollup = all.getOrDefault(profile.dominantNiche(), new NicheUtilityRollup(profile.dominantNiche(), 1, 0L, 0L, 0.0D, 1.0D));
        return saturationModel.pressureFor(profile.dominantNiche(), nicheRollup, all.isEmpty() ? Map.of(profile.dominantNiche(), nicheRollup) : all);
    }

    public Map<String, Object> analyticsSnapshot() {
        Map<MechanicNicheTag, NicheUtilityRollup> rollups = rollups();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("nichePopulation", rollups.entrySet().stream().collect(java.util.stream.Collectors.toMap(e -> e.getKey().name(), e -> e.getValue().activeArtifacts(), (a, b) -> a, LinkedHashMap::new)));
        out.put("nicheMeaningfulOutcomeYield", rollups.entrySet().stream().collect(java.util.stream.Collectors.toMap(e -> e.getKey().name(), e -> e.getValue().outcomeYield(), (a, b) -> a, LinkedHashMap::new)));
        out.put("nicheUtilityDensity", rollups.entrySet().stream().collect(java.util.stream.Collectors.toMap(e -> e.getKey().name(), e -> e.getValue().utilityDensity(), (a, b) -> a, LinkedHashMap::new)));
        out.put("saturationPressure", rollups.entrySet().stream().collect(java.util.stream.Collectors.toMap(e -> e.getKey().name(), e -> saturationModel.pressureFor(e.getKey(), e.getValue(), rollups).saturationPenalty(), (a, b) -> a, LinkedHashMap::new)));
        out.put("scarcityBonus", rollups.entrySet().stream().collect(java.util.stream.Collectors.toMap(e -> e.getKey().name(), e -> saturationModel.pressureFor(e.getKey(), e.getValue(), rollups).scarcityBonus(), (a, b) -> a, LinkedHashMap::new)));
        AdaptiveSupportBudget budget = carryingCapacityModel.calculate(rollups);
        out.put("specializationTrends", nicheProfilesByArtifact.values().stream().collect(java.util.stream.Collectors.groupingBy(v -> v.specialization().dominantSubniche(), LinkedHashMap::new, java.util.stream.Collectors.counting())));
        out.put("carryingCapacity", budget.carryingCapacity());
        out.put("capacityUtilization", budget.capacityUtilization());
        out.put("saturationIndex", budget.saturationIndex());
        out.put("turnoverPressure", budget.turnoverPressure());
        return out;
    }

    private static class MutableRollup {
        private int activeArtifacts;
        private long attempts;
        private long meaningful;
        private double validated;
        private double budget;
    }
}
