package obtuseloot.evolution;

import obtuseloot.telemetry.EcosystemTelemetryEmitter;
import obtuseloot.telemetry.EcosystemTelemetryEventType;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NichePopulationTracker {
    private static final double INVERSION_PARENT_MULTIPLIER = 0.88D;
    private static final double INVERSION_CHILD_MULTIPLIER = 1.30D;
    private static final int INVERSION_DECAY_WINDOWS = 5;
    private static final long INVERSION_WINDOW_MS = 5_000L;
    private static final double INVERSION_SATURATION_GATE = NicheBifurcationRegistry.SATURATION_THRESHOLD;
    private static final double COMPETITION_SHARE_WEIGHT = 3.0D;
    private static final double COMPETITION_SATURATION_WEIGHT = 1.0D;
    private static final double COMPETITION_DENSITY_WEIGHT = 0.60D;

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
    private final Map<Long, String> lineageByArtifact = new ConcurrentHashMap<>();
    private final Map<String, String> parentByChildNiche = new ConcurrentHashMap<>();
    private final Map<String, Long> birthMsByChildNiche = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Double>> lineageAffinityByParent = new ConcurrentHashMap<>();
    private final Random migrationRandom = new Random(0xC0FFEE1234ABCDEFL);
    private static final double STRUCTURAL_CHILD_PARTITION_TARGET = 0.15D;
    private static final double STRUCTURAL_CHILD_PARTITION_MIN = 0.10D;
    private static final double STRUCTURAL_CHILD_PARTITION_MAX = 0.20D;
    private static final double STRUCTURAL_ACTIVATION_SATURATION_GATE = NicheBifurcationRegistry.SATURATION_THRESHOLD * 0.90D;
    private static final int NICHE_LOCK_MIN_WINDOWS = 4;
    private static final int NICHE_LOCK_MAX_WINDOWS = 6;
    private static final double NICHE_LOCK_UTILITY_BOOST = 1.30D;
    private final Map<Long, NicheLockState> lockStateByArtifact = new ConcurrentHashMap<>();

    /**
     * Throttle: bifurcation evaluation is only run when at least this many
     * milliseconds have elapsed since the last check.
     */
    private static final long BIFURCATION_EVAL_INTERVAL_MS = 5_000L;
    private volatile long lastBifurcationEvalMs = 0L;

    // ---------- guaranteed bifurcation support ----------

    /** Monotonically-increasing count of evaluation windows (each call to evaluateBifurcations). */
    private volatile int evaluationWindowCount = 0;
    /** Set once a forced bifurcation has been successfully recorded to prevent repeats. */
    private volatile boolean forcedBifurcationDone = false;
    /** Trigger forced bifurcation if no organic one has occurred by this window. */
    private static final int FORCED_BIFURCATION_WINDOW = 3;
    /** Minimum niche share required to be a forced-bifurcation candidate. */
    private static final double FORCED_BIFURCATION_MIN_SHARE = 0.10D;
    /** Minimum total active artifacts before attempting forced bifurcation. */
    private static final int FORCED_BIFURCATION_MIN_ARTIFACTS = 5;

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
        lineageByArtifact.remove(artifactSeed);
        lockStateByArtifact.remove(artifactSeed);
    }

    public void recordTelemetry(long artifactSeed, Map<String, MechanicUtilitySignal> signals) {
        recordTelemetry(artifactSeed, null, signals);
    }

    public void recordTelemetry(long artifactSeed, String lineageId, Map<String, MechanicUtilitySignal> signals) {
        if (signals == null || signals.isEmpty()) {
            return;
        }
        if (lineageId != null && !lineageId.isBlank()) {
            lineageByArtifact.put(artifactSeed, lineageId);
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
        decayNicheLocks();
        // Advance the window counter on every evaluation call
        evaluationWindowCount++;

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
                registerChildNicheMetadata(bifurcation, nowMs);
                seedLineageAffinityForBifurcation(nicheTag, bifurcation);
                // Record birth window for grace-period tracking
                bifurcationRegistry.setChildBirthWindow(bifurcation.childNicheA(), evaluationWindowCount);
                bifurcationRegistry.setChildBirthWindow(bifurcation.childNicheB(), evaluationWindowCount);
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

        // Guarantee at least one bifurcation: if none has occurred by window FORCED_BIFURCATION_WINDOW
        // and there is a dominant niche eligible, force one now (once per scenario only).
        if (!forcedBifurcationDone
                && bifurcationRegistry.bifurcations().isEmpty()
                && evaluationWindowCount >= FORCED_BIFURCATION_WINDOW
                && totalPopulation >= FORCED_BIFURCATION_MIN_ARTIFACTS) {
            tryForcedBifurcation(allRollups, totalPopulation, nowMs);
        }

        Map<String, Long> dynamicPopulation = new LinkedHashMap<>();
        dynamicNicheByArtifact.forEach((seed, childNiche) -> dynamicPopulation.merge(childNiche, 1L, Long::sum));
        applyForcedDisplacement();
        dynamicPopulation.clear();
        dynamicNicheByArtifact.forEach((seed, childNiche) -> dynamicPopulation.merge(childNiche, 1L, Long::sum));
        applySoftMigrationPressure(dynamicPopulation);
        // Re-apply structural partition bounds so soft migration cannot stack child occupancy past cap.
        applyForcedDisplacement();
        dynamicPopulation.clear();
        dynamicNicheByArtifact.forEach((seed, childNiche) -> dynamicPopulation.merge(childNiche, 1L, Long::sum));
        // Pass current window so grace-period children are not prematurely collapsed
        bifurcationRegistry.collapseUnderpopulatedChildren(dynamicPopulation, evaluationWindowCount);
        dynamicNicheByArtifact.entrySet().removeIf(entry -> !bifurcationRegistry.isDynamicNiche(entry.getValue()));
        lockStateByArtifact.entrySet().removeIf(entry -> !bifurcationRegistry.isDynamicNiche(entry.getValue().lockedNiche()));
    }

    /**
     * Attempts a forced bifurcation on the niche with the highest population share
     * that meets the minimum share threshold, bypassing pressure gates.
     */
    private void tryForcedBifurcation(Map<MechanicNicheTag, NicheUtilityRollup> allRollups,
                                      int totalPopulation,
                                      long nowMs) {
        MechanicNicheTag topNiche = null;
        double topShare = 0.0D;
        for (Map.Entry<MechanicNicheTag, NicheUtilityRollup> entry : allRollups.entrySet()) {
            double share = totalPopulation <= 0 ? 0.0D
                    : entry.getValue().activeArtifacts() / (double) totalPopulation;
            if (share >= FORCED_BIFURCATION_MIN_SHARE && share > topShare) {
                topShare = share;
                topNiche = entry.getKey();
            }
        }
        if (topNiche == null) {
            return;
        }
        MechanicNicheTag finalTopNiche = topNiche;
        String nicheName = topNiche.name();
        NicheUtilityRollup nicheRollup = allRollups.get(topNiche);
        RolePressureMetrics pressure = saturationModel.pressureFor(topNiche, nicheRollup, allRollups);

        Optional<NicheBifurcation> maybeBifurcation = bifurcationRegistry.forceBifurcation(
                nicheName,
                pressure.saturationPenalty(),
                meanSpecializationFor(topNiche),
                nowMs);

        maybeBifurcation.ifPresent(bifurcation -> {
            forcedBifurcationDone = true;
            emitBifurcationEvent(bifurcation);
            registerChildNicheMetadata(bifurcation, nowMs);
            seedLineageAffinityForBifurcation(finalTopNiche, bifurcation);
            bifurcationRegistry.setChildBirthWindow(bifurcation.childNicheA(), evaluationWindowCount);
            bifurcationRegistry.setChildBirthWindow(bifurcation.childNicheB(), evaluationWindowCount);
            for (Long seed : activeArtifacts) {
                ArtifactNicheProfile profile = nicheProfilesByArtifact.get(seed);
                if (profile != null && profile.dominantNiche() == finalTopNiche) {
                    refreshDynamicNicheAssignment(seed, profile);
                }
            }
        });
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
        NicheLockState lockState = lockStateByArtifact.get(artifactSeed);
        if (lockState != null) {
            if (bifurcationRegistry.isDynamicNiche(lockState.lockedNiche())) {
                dynamicNicheByArtifact.put(artifactSeed, lockState.lockedNiche());
                return;
            }
            lockStateByArtifact.remove(artifactSeed);
        }
        String parentName = profile.dominantNiche().name();
        List<String> children = bifurcationRegistry.childrenOf(parentName);
        String existingChild = dynamicNicheByArtifact.get(artifactSeed);
        if (existingChild != null) {
            // Keep the assignment unless the child niche was retired or the artifact's
            // current parent niche has children of its own (meaning the artifact belongs
            // to a different bifurcated family and can be assigned there instead).
            boolean childStillExists = bifurcationRegistry.isDynamicNiche(existingChild);
            String existingChildParent = parentByChildNiche.get(existingChild);
            boolean childBelongsToCurrentParent = parentName.equals(existingChildParent);
            if (!childStillExists || (childBelongsToCurrentParent && !children.contains(existingChild))) {
                // Child retired, or current parent's children no longer include this child
                dynamicNicheByArtifact.remove(artifactSeed);
            }
            // In all other cases keep the existing assignment
            return;
        }
        if (children.isEmpty()) {
            // No bifurcation for this niche yet — nothing to assign
            return;
        }
    }

    private void applyForcedDisplacement() {
        Map<MechanicNicheTag, NicheUtilityRollup> allRollups = rollups();
        if (allRollups.isEmpty()) {
            return;
        }
        for (MechanicNicheTag parentTag : MechanicNicheTag.values()) {
            String parentName = parentTag.name();
            List<String> children = bifurcationRegistry.childrenOf(parentName);
            if (children.size() < 2) {
                continue;
            }
            NicheUtilityRollup parentRollup = allRollups.get(parentTag);
            if (parentRollup == null) {
                continue;
            }
            RolePressureMetrics pressure = saturationModel.pressureFor(parentTag, parentRollup, allRollups);
            // During the grace period, bypass the saturation gate so child niches are
            // seeded even in low-saturation scenarios (e.g. explorer-heavy).
            boolean anyChildInGrace = children.stream()
                    .anyMatch(c -> bifurcationRegistry.isInGracePeriod(c, evaluationWindowCount));
            if (!anyChildInGrace && pressure.saturationPenalty() < STRUCTURAL_ACTIVATION_SATURATION_GATE) {
                continue;
            }
            List<Long> parentArtifacts = activeArtifacts.stream()
                    .filter(seed -> {
                        ArtifactNicheProfile profile = nicheProfilesByArtifact.get(seed);
                        return profile != null && profile.dominantNiche() == parentTag;
                    })
                    .toList();
            if (parentArtifacts.isEmpty()) {
                continue;
            }
            List<Long> lowUtilityOrdered = parentArtifacts.stream()
                    .sorted(java.util.Comparator.comparingDouble(this::utilityDensityFor))
                    .toList();
            if (lowUtilityOrdered.isEmpty()) {
                continue;
            }

            int minPartition = Math.max(1, (int) Math.ceil(parentArtifacts.size() * STRUCTURAL_CHILD_PARTITION_MIN));
            int maxPartition = Math.max(minPartition, (int) Math.floor(parentArtifacts.size() * STRUCTURAL_CHILD_PARTITION_MAX));
            int targetPartition = (int) Math.round(parentArtifacts.size() * STRUCTURAL_CHILD_PARTITION_TARGET);
            int boundedTarget = Math.max(minPartition, Math.min(maxPartition, targetPartition));

            java.util.Set<Long> parentArtifactSet = java.util.Set.copyOf(parentArtifacts);
            List<Long> assignedChildren = parentArtifacts.stream()
                    .filter(seed -> {
                        String child = dynamicNicheByArtifact.get(seed);
                        return child != null && children.contains(child);
                    })
                    .toList();

            if (assignedChildren.size() < boundedTarget) {
                int lowerHalfCutoff = Math.max(1, (int) Math.ceil(lowUtilityOrdered.size() * 0.5D));
                List<Long> lowerHalf = lowUtilityOrdered.subList(0, lowerHalfCutoff);
                int needed = boundedTarget - assignedChildren.size();
                for (Long artifactSeed : lowerHalf) {
                    if (needed <= 0) {
                        break;
                    }
                    if (!parentArtifactSet.contains(artifactSeed)) {
                        continue;
                    }
                    String existing = dynamicNicheByArtifact.get(artifactSeed);
                    if (existing != null && children.contains(existing)) {
                        continue;
                    }
                    ArtifactNicheProfile profile = nicheProfilesByArtifact.get(artifactSeed);
                    if (profile == null) {
                        continue;
                    }
                    String lineageId = lineageByArtifact.get(artifactSeed);
                    String child = chooseBalancedChildNiche(parentName, profile.specialization().dominantSubniche(), lineageId, artifactSeed, children);
                    if (child != null) {
                        dynamicNicheByArtifact.put(artifactSeed, child);
                        applyMigrationLock(artifactSeed, child);
                        needed--;
                    }
                }
            }

            List<Long> currentAssignedChildren = parentArtifacts.stream()
                    .filter(seed -> {
                        String child = dynamicNicheByArtifact.get(seed);
                        return child != null && children.contains(child);
                    })
                    .toList();

            if (currentAssignedChildren.size() > maxPartition) {
                List<Long> demotionOrder = currentAssignedChildren.stream()
                        .sorted(java.util.Comparator.comparingDouble(this::utilityDensityFor).reversed())
                        .toList();
                int toDemote = currentAssignedChildren.size() - maxPartition;
                for (int i = 0; i < toDemote && i < demotionOrder.size(); i++) {
                    Long artifactSeed = demotionOrder.get(i);
                    dynamicNicheByArtifact.remove(artifactSeed);
                    lockStateByArtifact.remove(artifactSeed);
                }
            }

            for (Long artifactSeed : lowUtilityOrdered) {
                String assigned = dynamicNicheByArtifact.get(artifactSeed);
                if (assigned != null && children.contains(assigned)) {
                    continue;
                }
                lockStateByArtifact.remove(artifactSeed);
                ArtifactNicheProfile profile = nicheProfilesByArtifact.get(artifactSeed);
                if (profile != null) {
                    refreshDynamicNicheAssignment(artifactSeed, profile);
                }
            }
        }
    }

    private String chooseBalancedChildNiche(String parentName,
                                            String dominantSubniche,
                                            String lineageId,
                                            long artifactSeed,
                                            List<String> children) {
        String baseline = chooseChildNiche(parentName, dominantSubniche, lineageId, artifactSeed);
        String childA = children.get(children.size() - 2);
        String childB = children.get(children.size() - 1);
        long populationA = dynamicNicheByArtifact.values().stream().filter(childA::equals).count();
        long populationB = dynamicNicheByArtifact.values().stream().filter(childB::equals).count();
        if (Math.abs(populationA - populationB) <= 1L) {
            return baseline;
        }
        String underrepresented = populationA < populationB ? childA : childB;
        return migrationRandom.nextDouble() < 0.85D ? underrepresented : baseline;
    }

    private String chooseChildNiche(String parentName, String dominantSubniche, String lineageId, long artifactSeed) {
        String baseline = bifurcationRegistry.assignChildNiche(parentName, dominantSubniche);
        List<String> children = bifurcationRegistry.childrenOf(parentName);
        if (children.size() < 2 || lineageId == null || lineageId.isBlank()) {
            return baseline;
        }
        Map<String, Double> affinity = lineageAffinityByParent
                .getOrDefault(parentName, Map.of())
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().startsWith(lineageId + "|"))
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (affinity.isEmpty()) {
            return baseline;
        }
        String childA = children.get(children.size() - 2);
        String childB = children.get(children.size() - 1);
        double affinityA = affinity.getOrDefault(lineageId + "|" + childA, 0.0D);
        double affinityB = affinity.getOrDefault(lineageId + "|" + childB, 0.0D);
        if (Math.abs(affinityA - affinityB) < 0.10D) {
            return baseline;
        }
        String preferred = affinityA >= affinityB ? childA : childB;
        double confidence = clamp(0.55D + (Math.abs(affinityA - affinityB) * 0.45D), 0.55D, 0.90D);
        long seed = Math.abs((artifactSeed * 31L) ^ preferred.hashCode());
        double roll = (seed % 10_000L) / 10_000.0D;
        return roll < confidence ? preferred : baseline;
    }

    private void registerChildNicheMetadata(NicheBifurcation bifurcation, long nowMs) {
        parentByChildNiche.put(bifurcation.childNicheA(), bifurcation.parentNiche());
        parentByChildNiche.put(bifurcation.childNicheB(), bifurcation.parentNiche());
        birthMsByChildNiche.putIfAbsent(bifurcation.childNicheA(), nowMs);
        birthMsByChildNiche.putIfAbsent(bifurcation.childNicheB(), nowMs);
    }

    private void seedLineageAffinityForBifurcation(MechanicNicheTag parent, NicheBifurcation bifurcation) {
        String parentName = parent.name();
        Map<String, Integer> lineagePopulation = new LinkedHashMap<>();
        Map<String, Integer> lineageToChildA = new LinkedHashMap<>();
        for (Long seed : activeArtifacts) {
            ArtifactNicheProfile profile = nicheProfilesByArtifact.get(seed);
            if (profile == null || profile.dominantNiche() != parent) {
                continue;
            }
            String lineageId = lineageByArtifact.get(seed);
            if (lineageId == null || lineageId.isBlank()) {
                continue;
            }
            lineagePopulation.merge(lineageId, 1, Integer::sum);
            String candidate = bifurcationRegistry.assignChildNiche(parentName, profile.specialization().dominantSubniche());
            if (bifurcation.childNicheA().equals(candidate)) {
                lineageToChildA.merge(lineageId, 1, Integer::sum);
            }
        }
        if (lineagePopulation.isEmpty()) {
            return;
        }
        Map<String, Double> affinity = lineageAffinityByParent.computeIfAbsent(parentName, ignored -> new ConcurrentHashMap<>());
        for (Map.Entry<String, Integer> entry : lineagePopulation.entrySet()) {
            String lineageId = entry.getKey();
            int total = entry.getValue();
            double aShare = lineageToChildA.getOrDefault(lineageId, 0) / (double) Math.max(1, total);
            double bShare = 1.0D - aShare;
            affinity.put(lineageId + "|" + bifurcation.childNicheA(), aShare);
            affinity.put(lineageId + "|" + bifurcation.childNicheB(), bShare);
        }
    }

    private void applySoftMigrationPressure(Map<String, Long> dynamicPopulation) {
        if (activeArtifacts.size() < 10 || dynamicPopulation.isEmpty()) {
            return;
        }
        Map<String, Double> meanDensityByParent = new LinkedHashMap<>();
        for (MechanicNicheTag parent : MechanicNicheTag.values()) {
            String parentName = parent.name();
            List<String> children = bifurcationRegistry.childrenOf(parentName);
            if (children.isEmpty()) {
                continue;
            }
            double totalDensity = 0.0D;
            int count = 0;
            for (Long seed : activeArtifacts) {
                ArtifactNicheProfile profile = nicheProfilesByArtifact.get(seed);
                if (profile != null && profile.dominantNiche() == parent) {
                    totalDensity += utilityDensityFor(seed);
                    count++;
                }
            }
            if (count > 0) {
                meanDensityByParent.put(parentName, totalDensity / count);
            }
        }
        int migrated = 0;
        int migrationCap = Math.max(1, (int) Math.round(activeArtifacts.size() * 0.08D));
        for (Long seed : activeArtifacts) {
            if (migrated >= migrationCap) {
                break;
            }
            ArtifactNicheProfile profile = nicheProfilesByArtifact.get(seed);
            if (profile == null) {
                continue;
            }
            String parentName = profile.dominantNiche().name();
            List<String> children = bifurcationRegistry.childrenOf(parentName);
            if (children.size() < 2) {
                continue;
            }
            long parentPopulation = activeArtifacts.stream()
                    .filter(id -> {
                        ArtifactNicheProfile p = nicheProfilesByArtifact.get(id);
                        return p != null && p.dominantNiche().name().equals(parentName);
                    })
                    .count();
            double parentShare = parentPopulation / (double) Math.max(1, activeArtifacts.size());
            if (parentShare < 0.17D) {
                continue;
            }
            double meanUtility = meanDensityByParent.getOrDefault(parentName, 0.0D);
            double utility = utilityDensityFor(seed);
            if (utility >= meanUtility) {
                continue;
            }
            String currentChild = dynamicNicheByArtifact.get(seed);
            String weakerChild = children.get(children.size() - 2);
            String strongerChild = children.get(children.size() - 1);
            if (dynamicPopulation.getOrDefault(weakerChild, 0L) > dynamicPopulation.getOrDefault(strongerChild, 0L)) {
                String swap = weakerChild;
                weakerChild = strongerChild;
                strongerChild = swap;
            }
            if (weakerChild.equals(currentChild)) {
                continue;
            }
            double migrationChance = clamp((parentShare - 0.17D) * 1.9D + (meanUtility - utility) * 0.50D, 0.04D, 0.30D);
            if (migrationRandom.nextDouble() < migrationChance) {
                dynamicNicheByArtifact.put(seed, weakerChild);
                applyMigrationLock(seed, weakerChild);
                dynamicPopulation.merge(weakerChild, 1L, Long::sum);
                if (currentChild != null) {
                    dynamicPopulation.merge(currentChild, -1L, Long::sum);
                }
                migrated++;
            }
        }
    }

    private double utilityDensityFor(long artifactSeed) {
        Map<String, MechanicUtilitySignal> signals = signalsByArtifact.get(artifactSeed);
        if (signals == null || signals.isEmpty()) {
            return 0.0D;
        }
        double total = 0.0D;
        for (MechanicUtilitySignal signal : signals.values()) {
            total += signal.utilityDensity();
        }
        return total / signals.size();
    }

    /**
     * Returns the effective telemetry niche name for an artifact.
     * If the artifact has been assigned to a dynamic child niche, that name is
     * returned; otherwise the canonical enum name is used.
     */
    public String effectiveNicheName(long artifactSeed) {
        NicheLockState lockState = lockStateByArtifact.get(artifactSeed);
        if (lockState != null && bifurcationRegistry.isDynamicNiche(lockState.lockedNiche())) {
            return lockState.lockedNiche();
        }
        String dynamic = dynamicNicheByArtifact.get(artifactSeed);
        if (dynamic != null) {
            return dynamic;
        }
        return nicheProfile(artifactSeed).dominantNiche().name();
    }

    public double nicheAdoptionFitnessMultiplier(long artifactSeed) {
        NicheLockState lockState = lockStateByArtifact.get(artifactSeed);
        if (lockState != null && bifurcationRegistry.isDynamicNiche(lockState.lockedNiche())) {
            String parent = parentByChildNiche.get(lockState.lockedNiche());
            NicheVariantProfile variant = bifurcationRegistry.variantFor(lockState.lockedNiche());
            ArtifactNicheProfile profile = nicheProfilesByArtifact.get(artifactSeed);
            double variantBoost = (variant != null && profile != null)
                    ? variant.adoptionBoostFor(profile.specialization().dominantSubniche()) : 1.0D;
            return clamp(inversionMultiplier(parent, true) * NICHE_LOCK_UTILITY_BOOST * variantBoost, 1.10D, 1.40D);
        }
        String childNiche = dynamicNicheByArtifact.get(artifactSeed);
        if (childNiche != null && bifurcationRegistry.isDynamicNiche(childNiche)) {
            String parent = parentByChildNiche.get(childNiche);
            NicheVariantProfile variant = bifurcationRegistry.variantFor(childNiche);
            ArtifactNicheProfile profile = nicheProfilesByArtifact.get(artifactSeed);
            double variantBoost = (variant != null && profile != null)
                    ? variant.adoptionBoostFor(profile.specialization().dominantSubniche()) : 1.0D;
            return clamp(inversionMultiplier(parent, true) * variantBoost, 1.00D, 1.40D);
        }

        ArtifactNicheProfile profile = nicheProfilesByArtifact.get(artifactSeed);
        if (profile == null) {
            return 1.0D;
        }

        String parent = profile.dominantNiche().name();
        if (bifurcationRegistry.childrenOf(parent).isEmpty()) {
            return 1.0D;
        }
        return inversionMultiplier(parent, false);
    }

    /**
     * Returns the {@link NicheVariantProfile} for the effective child niche of the
     * given artifact, or {@code null} if the artifact is not currently assigned to
     * a dynamic child niche.
     */
    public NicheVariantProfile variantFor(long artifactSeed) {
        String effective = effectiveNicheName(artifactSeed);
        if (!bifurcationRegistry.isDynamicNiche(effective)) {
            return null;
        }
        return bifurcationRegistry.variantFor(effective);
    }

    public double nicheCompetitionFactor(long artifactSeed) {
        ArtifactNicheProfile profile = nicheProfilesByArtifact.get(artifactSeed);
        if (profile == null) {
            return 1.0D;
        }
        Map<MechanicNicheTag, NicheUtilityRollup> allRollups = rollups();
        NicheUtilityRollup nicheRollup = allRollups.get(profile.dominantNiche());
        if (nicheRollup == null) {
            return 1.0D;
        }
        int totalPopulation = Math.max(1, allRollups.values().stream().mapToInt(NicheUtilityRollup::activeArtifacts).sum());
        double nicheShare = nicheRollup.activeArtifacts() / (double) totalPopulation;
        RolePressureMetrics pressure = saturationModel.pressureFor(profile.dominantNiche(), nicheRollup, allRollups);
        double saturationPressure = clamp(pressure.saturationPenalty(), 0.0D, 1.0D);
        double artifactDensity = clamp(nicheRollup.attempts() / Math.max(1.0D, nicheRollup.activeArtifacts() * 12.0D), 0.0D, 1.0D);
        double competitionFactor = 1.0D
                + (nicheShare * COMPETITION_SHARE_WEIGHT)
                + (saturationPressure * COMPETITION_SATURATION_WEIGHT)
                + (artifactDensity * COMPETITION_DENSITY_WEIGHT);
        return clamp(competitionFactor, 1.0D, 5.0D);
    }

    private double inversionMultiplier(String parentNiche, boolean childArtifact) {
        if (parentNiche == null || !isParentSaturationHigh(parentNiche)) {
            return 1.0D;
        }
        NicheBifurcation latest = latestBifurcation(parentNiche);
        if (latest == null) {
            return 1.0D;
        }
        long ageMs = Math.max(0L, System.currentTimeMillis() - latest.timestampMs());
        long decayHorizonMs = INVERSION_DECAY_WINDOWS * INVERSION_WINDOW_MS;
        double decayFactor = clamp(1.0D - (ageMs / (double) Math.max(1L, decayHorizonMs)), 0.0D, 1.0D);
        if (decayFactor <= 0.0D) {
            return 1.0D;
        }
        double target = childArtifact ? INVERSION_CHILD_MULTIPLIER : INVERSION_PARENT_MULTIPLIER;
        double multiplier = 1.0D + ((target - 1.0D) * decayFactor);
        return childArtifact
                ? clamp(multiplier, 1.10D, 1.32D)
                : clamp(multiplier, 0.86D, 0.95D);
    }

    private boolean isParentSaturationHigh(String parentNiche) {
        MechanicNicheTag tag = nicheTagByName(parentNiche);
        if (tag == null) {
            return false;
        }
        Map<MechanicNicheTag, NicheUtilityRollup> allRollups = rollups();
        NicheUtilityRollup parentRollup = allRollups.get(tag);
        if (parentRollup == null) {
            return false;
        }
        RolePressureMetrics pressure = saturationModel.pressureFor(tag, parentRollup, allRollups);
        return pressure.saturationPenalty() >= INVERSION_SATURATION_GATE;
    }

    private NicheBifurcation latestBifurcation(String parentNiche) {
        List<NicheBifurcation> events = bifurcationRegistry.bifurcations();
        for (int i = events.size() - 1; i >= 0; i--) {
            NicheBifurcation event = events.get(i);
            if (parentNiche.equals(event.parentNiche())) {
                return event;
            }
        }
        return null;
    }

    private MechanicNicheTag nicheTagByName(String name) {
        for (MechanicNicheTag tag : MechanicNicheTag.values()) {
            if (tag.name().equals(name)) {
                return tag;
            }
        }
        return null;
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
        Map<String, Long> lockedPop = new LinkedHashMap<>();
        lockStateByArtifact.forEach((seed, lockState) -> lockedPop.merge(lockState.lockedNiche(), 1L, Long::sum));
        out.put("lockedNichePopulation", lockedPop);
        out.put("lockedArtifactCount", lockStateByArtifact.size());
        out.put("lineageAffinity", Map.copyOf(lineageAffinityByParent));
        return out;
    }

    private void applyMigrationLock(long artifactSeed, String childNiche) {
        if (childNiche == null || !bifurcationRegistry.isDynamicNiche(childNiche)) {
            lockStateByArtifact.remove(artifactSeed);
            return;
        }
        int duration = NICHE_LOCK_MIN_WINDOWS + migrationRandom.nextInt((NICHE_LOCK_MAX_WINDOWS - NICHE_LOCK_MIN_WINDOWS) + 1);
        lockStateByArtifact.put(artifactSeed, new NicheLockState(childNiche, duration));
    }

    private void decayNicheLocks() {
        lockStateByArtifact.replaceAll((seed, lockState) -> lockState.decayed());
        lockStateByArtifact.entrySet().removeIf(entry -> entry.getValue().remainingWindows() <= 0);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
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
        // Also include the variant identity profile so it is inspectable in telemetry.
        NicheVariantProfile variantA = bifurcationRegistry.variantFor(bifurcation.childNicheA());
        NicheVariantProfile variantB = bifurcationRegistry.variantFor(bifurcation.childNicheB());

        Map<String, String> childAttrsA = new LinkedHashMap<>();
        childAttrsA.put("event_type", "niche_bifurcation_child");
        childAttrsA.put("parent_niche", bifurcation.parentNiche());
        childAttrsA.put("variant_type", "ALPHA");
        if (variantA != null) {
            childAttrsA.put("variant_mutation_bias", String.valueOf(variantA.mutationBias()));
            childAttrsA.put("variant_retention_bias", String.valueOf(variantA.retentionBias()));
            childAttrsA.put("variant_reinforcement_bias", String.valueOf(variantA.reinforcementBias()));
            childAttrsA.put("variant_subniche_affinity", variantA.subNicheAffinityParity() == 0 ? "EVEN_HASH" : "ODD_HASH");
        }
        childAttrsA.put("context_tags", "niche-bifurcation lifecycle niche-identity");
        emitter.emit(EcosystemTelemetryEventType.NICHE_BIFURCATION,
                0L,
                "",
                bifurcation.childNicheA(),
                childAttrsA);

        Map<String, String> childAttrsB = new LinkedHashMap<>();
        childAttrsB.put("event_type", "niche_bifurcation_child");
        childAttrsB.put("parent_niche", bifurcation.parentNiche());
        childAttrsB.put("variant_type", "BETA");
        if (variantB != null) {
            childAttrsB.put("variant_mutation_bias", String.valueOf(variantB.mutationBias()));
            childAttrsB.put("variant_retention_bias", String.valueOf(variantB.retentionBias()));
            childAttrsB.put("variant_reinforcement_bias", String.valueOf(variantB.reinforcementBias()));
            childAttrsB.put("variant_subniche_affinity", variantB.subNicheAffinityParity() == 0 ? "EVEN_HASH" : "ODD_HASH");
        }
        childAttrsB.put("context_tags", "niche-bifurcation lifecycle niche-identity");
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

    private record NicheLockState(String lockedNiche, int remainingWindows) {
        private NicheLockState decayed() {
            return new NicheLockState(lockedNiche, remainingWindows - 1);
        }
    }
}
