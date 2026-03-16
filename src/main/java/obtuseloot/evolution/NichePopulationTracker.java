package obtuseloot.evolution;

import obtuseloot.telemetry.EcosystemTelemetryEmitter;
import obtuseloot.telemetry.EcosystemTelemetryEventType;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    // ---------- bifurcation support ----------
    private final NicheBifurcationRegistry bifurcationRegistry;

    /**
     * Dynamic niche assignment for individual artifacts.
     * When a parent niche has bifurcated, an artifact is assigned to one of its
     * child niches and that assignment is stored here so that telemetry events
     * emitted on its behalf use the child niche name rather than the parent enum name.
     */
    private final Map<Long, String> dynamicNicheByArtifact = new ConcurrentHashMap<>();

    /**
     * Throttle: bifurcation evaluation is only run when at least this many
     * milliseconds have elapsed since the last check.
     */
    private static final long BIFURCATION_EVAL_INTERVAL_MS = 5_000L;
    private volatile long lastBifurcationEvalMs = 0L;

    public NichePopulationTracker() {
        this(new EcosystemRoleClassifier(), new EcosystemSaturationModel(), new NicheBifurcationRegistry());
    }

    public NichePopulationTracker(EcosystemRoleClassifier classifier, EcosystemSaturationModel saturationModel) {
        this(classifier, saturationModel, new NicheBifurcationRegistry());
    }

    public NichePopulationTracker(EcosystemRoleClassifier classifier,
                                   EcosystemSaturationModel saturationModel,
                                   NicheBifurcationRegistry bifurcationRegistry) {
        this.classifier           = classifier;
        this.saturationModel      = saturationModel;
        this.bifurcationRegistry  = bifurcationRegistry;
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
        dynamicNicheByArtifact.remove(artifactSeed);
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

        // Refresh dynamic-niche assignment whenever the artifact's dominant niche changes
        refreshDynamicNicheAssignment(artifactSeed, next);

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

        // Throttled bifurcation evaluation
        long nowMs = System.currentTimeMillis();
        if (nowMs - lastBifurcationEvalMs >= BIFURCATION_EVAL_INTERVAL_MS) {
            lastBifurcationEvalMs = nowMs;
            evaluateBifurcations(nowMs);
        }
    }

    /**
     * Evaluate all currently-occupied niches for bifurcation conditions and
     * emit {@link EcosystemTelemetryEventType#NICHE_BIFURCATION} events when
     * one is triggered.
     *
     * <p>Two independent signals are combined:</p>
     * <ol>
     *   <li><b>saturationPenalty</b> – niche is overcrowded with below-mean utility.</li>
     *   <li><b>meanSpecialization</b> – the average artifact-level specialization score
     *       inside the niche, indicating that occupants have become differentiated.
     *       This signal is independent of saturationPenalty's utility-density gate,
     *       so both can be simultaneously non-zero.</li>
     * </ol>
     *
     * <p>This method is also exposed publicly so that integration tests can
     * drive bifurcation directly without depending on real-time waits.</p>
     */
    public void evaluateBifurcations(long nowMs) {
        Map<MechanicNicheTag, NicheUtilityRollup> allRollups = rollups();
        if (allRollups.isEmpty()) {
            return;
        }

        int totalPopulation = allRollups.values().stream().mapToInt(NicheUtilityRollup::activeArtifacts).sum();
        boolean bifurcatedThisCycle = false;
        for (Map.Entry<MechanicNicheTag, NicheUtilityRollup> entry : allRollups.entrySet()) {
            if (bifurcatedThisCycle) {
                continue;
            }
            MechanicNicheTag nicheTag = entry.getKey();
            NicheUtilityRollup nicheRollup = entry.getValue();
            String nicheName = nicheTag.name();
            double nicheShare = totalPopulation <= 0 ? 0.0D : nicheRollup.activeArtifacts() / (double) totalPopulation;

            RolePressureMetrics pressure = saturationModel.pressureFor(nicheTag, nicheRollup, allRollups);
            // Use mean artifact specialization score as the "specialization" dimension.
            // This is independent of saturationPenalty and can be simultaneously high
            // even when the niche has below-mean utility density.
            double meanSpecialization = meanSpecializationFor(nicheTag);

            Optional<NicheBifurcation> maybeBifurcation = bifurcationRegistry.evaluateBifurcation(
                    nicheName,
                    pressure.saturationPenalty(),
                    meanSpecialization,
                    nicheShare,
                    nicheRollup.activeArtifacts(),
                    nowMs);

            maybeBifurcation.ifPresent(bifurcation -> {
                emitBifurcationEvent(bifurcation);
                // Re-assign all artifacts in the parent niche to one of the two children
                for (Long seed : activeArtifacts) {
                    ArtifactNicheProfile profile = nicheProfilesByArtifact.get(seed);
                    if (profile != null && profile.dominantNiche() == nicheTag) {
                        refreshDynamicNicheAssignment(seed, profile);
                    }
                }
            });
            if (maybeBifurcation.isPresent()) {
                bifurcatedThisCycle = true;
            }
        }

        Map<String, Long> dynamicPopulation = new LinkedHashMap<>();
        dynamicNicheByArtifact.forEach((seed, childNiche) -> dynamicPopulation.merge(childNiche, 1L, Long::sum));
        bifurcationRegistry.collapseUnderpopulatedChildren(dynamicPopulation);
        dynamicNicheByArtifact.entrySet().removeIf(entry -> !bifurcationRegistry.isDynamicNiche(entry.getValue()));
    }

    /**
     * Computes the mean specialization score of all active artifacts whose
     * dominant niche is {@code nicheTag}.  Returns 0.0 if no artifacts are found.
     */
    private double meanSpecializationFor(MechanicNicheTag nicheTag) {
        double total = 0.0D;
        int count = 0;
        for (Long seed : activeArtifacts) {
            ArtifactNicheProfile profile = nicheProfilesByArtifact.get(seed);
            if (profile != null && profile.dominantNiche() == nicheTag) {
                total += profile.specialization().specializationScore();
                count++;
            }
        }
        return count == 0 ? 0.0D : total / count;
    }

    // ---------- dynamic-niche assignment ----------

    /**
     * If the artifact's parent niche has bifurcated, assign (or re-assign)
     * the artifact to one of the child niches based on its dominant subniche.
     */
    private void refreshDynamicNicheAssignment(long artifactSeed, ArtifactNicheProfile profile) {
        String parentName = profile.dominantNiche().name();
        if (bifurcationRegistry.childrenOf(parentName).isEmpty()) {
            // No bifurcation for this niche yet — clear any stale assignment
            dynamicNicheByArtifact.remove(artifactSeed);
            return;
        }
        String child = bifurcationRegistry.assignChildNiche(parentName, profile.specialization().dominantSubniche());
        if (child != null) {
            dynamicNicheByArtifact.put(artifactSeed, child);
        }
    }

    /**
     * Returns the effective telemetry niche name for an artifact.
     * If the artifact has been assigned to a dynamic child niche, that name is
     * returned; otherwise the canonical enum name is used.
     */
    public String effectiveNicheName(long artifactSeed) {
        String dynamic = dynamicNicheByArtifact.get(artifactSeed);
        if (dynamic != null) {
            return dynamic;
        }
        return nicheProfile(artifactSeed).dominantNiche().name();
    }

    // ---------- existing API (unchanged) ----------

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
        // Dynamic-niche summary
        out.put("dynamicNiches", List.copyOf(bifurcationRegistry.dynamicNiches()));
        out.put("bifurcationCount", bifurcationRegistry.bifurcations().size());
        Map<String, Long> dynamicPop = new LinkedHashMap<>();
        dynamicNicheByArtifact.forEach((seed, childNiche) -> dynamicPop.merge(childNiche, 1L, Long::sum));
        out.put("dynamicNichePopulation", dynamicPop);
        return out;
    }

    /** Exposes the bifurcation registry for inspection (e.g., in tests). */
    public NicheBifurcationRegistry bifurcationRegistry() {
        return bifurcationRegistry;
    }

    // ---------- internal helpers ----------

    private void emitBifurcationEvent(NicheBifurcation bifurcation) {
        EcosystemTelemetryEmitter emitter = telemetryEmitter;
        if (emitter == null) {
            return;
        }
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("event_type", "niche_bifurcation");
        attrs.put("parent_niche", bifurcation.parentNiche());
        attrs.put("child_niche_a", bifurcation.childNicheA());
        attrs.put("child_niche_b", bifurcation.childNicheB());
        attrs.put("saturation_pressure_at_creation", String.valueOf(bifurcation.saturationPressureAtCreation()));
        attrs.put("specialization_pressure_at_creation", String.valueOf(bifurcation.specializationPressureAtCreation()));
        attrs.put("timestamp_ms", String.valueOf(bifurcation.timestampMs()));
        attrs.put("context_tags", "niche-bifurcation lifecycle");

        // Emit with parent niche as the niche field to register in existing analytics
        emitter.emit(EcosystemTelemetryEventType.NICHE_BIFURCATION,
                0L,
                "",
                bifurcation.parentNiche(),
                attrs);

        // Emit a lightweight registration event for each child so they appear
        // in the buffer's nicheArtifactCounts from the moment of creation.
        Map<String, String> childAttrsA = Map.of(
                "event_type", "niche_bifurcation_child",
                "parent_niche", bifurcation.parentNiche(),
                "context_tags", "niche-bifurcation lifecycle");
        emitter.emit(EcosystemTelemetryEventType.NICHE_BIFURCATION,
                0L,
                "",
                bifurcation.childNicheA(),
                childAttrsA);

        Map<String, String> childAttrsB = Map.of(
                "event_type", "niche_bifurcation_child",
                "parent_niche", bifurcation.parentNiche(),
                "context_tags", "niche-bifurcation lifecycle");
        emitter.emit(EcosystemTelemetryEventType.NICHE_BIFURCATION,
                0L,
                "",
                bifurcation.childNicheB(),
                childAttrsB);
    }

    private static class MutableRollup {
        private int activeArtifacts;
        private long attempts;
        private long meaningful;
        private double validated;
        private double budget;
    }
}
