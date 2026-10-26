package obtuseloot.evolution;

import obtuseloot.abilities.AbilityExecutionStatus;
import obtuseloot.abilities.AbilityMechanic;
import obtuseloot.abilities.AbilityOutcomeType;
import obtuseloot.abilities.AbilityTrigger;
import obtuseloot.telemetry.EcosystemHistoryArchive;
import obtuseloot.telemetry.EcosystemTelemetryEmitter;
import obtuseloot.telemetry.EcosystemTelemetryEvent;
import obtuseloot.telemetry.EcosystemTelemetryEventType;
import obtuseloot.telemetry.ScheduledEcosystemRollups;
import obtuseloot.telemetry.RollupStateHydrator;
import obtuseloot.telemetry.TelemetryAggregationBuffer;
import obtuseloot.telemetry.TelemetryAggregationService;
import obtuseloot.telemetry.TelemetryRollupSnapshotStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NicheEcologySystemTest {

    // -------------------------------------------------------------------------
    // Existing tests (unchanged)
    // -------------------------------------------------------------------------

    @Test
    void classifierAssignsDominantAndMultiNicheProfile() {
        EcosystemRoleClassifier classifier = new EcosystemRoleClassifier();
        ArtifactNicheProfile profile = classifier.classify(Map.of(
                "NAVIGATION_ANCHOR@ON_WORLD_SCAN", new MechanicUtilitySignal("NAVIGATION_ANCHOR@ON_WORLD_SCAN", 3.0D, 1.0D, 0.8D, 0.1D, 0.1D, 0.1D, 5L, 4L, 3.0D),
                "SENSE_PING@ON_STRUCTURE_SENSE", new MechanicUtilitySignal("SENSE_PING@ON_STRUCTURE_SENSE", 1.2D, 0.7D, 0.7D, 0.1D, 0.1D, 0.1D, 4L, 3L, 2.0D)
        ));

        assertNotNull(profile.dominantNiche());
        assertTrue(profile.niches().contains(MechanicNicheTag.NAVIGATION));
        assertTrue(profile.niches().contains(MechanicNicheTag.STRUCTURE_SENSING));
        assertTrue(profile.specialization().specializationScore() > 0.0D);
    }

    @Test
    void saturationTrackingUsesRuntimeTelemetry() {
        ArtifactUsageTracker tracker = new ArtifactUsageTracker();
        ArtifactUsageProfile a = tracker.profileForSeed(1L);
        ArtifactUsageProfile b = tracker.profileForSeed(2L);

        a.recordUtilityOutcome(new UtilityOutcomeRecord("a", AbilityMechanic.PULSE, AbilityTrigger.ON_WORLD_SCAN,
                AbilityExecutionStatus.SUCCESS, AbilityOutcomeType.WORLD_INTERACTION, true, true, 0.8D, 1.0D, "sim", 1L));
        a.recordUtilityOutcome(new UtilityOutcomeRecord("a", AbilityMechanic.NAVIGATION_ANCHOR, AbilityTrigger.ON_WORLD_SCAN,
                AbilityExecutionStatus.SUCCESS, AbilityOutcomeType.INFORMATION, true, true, 0.9D, 1.0D, "sim", 2L));
        b.recordUtilityOutcome(new UtilityOutcomeRecord("b", AbilityMechanic.NAVIGATION_ANCHOR, AbilityTrigger.ON_WORLD_SCAN,
                AbilityExecutionStatus.NO_OP, AbilityOutcomeType.FLAVOR_ONLY, false, false, 0.4D, 1.3D, "sim", 3L));

        tracker.nichePopulationTracker().recordTelemetry(1L, a.utilitySignalsByMechanic());
        tracker.nichePopulationTracker().recordTelemetry(2L, b.utilitySignalsByMechanic());

        Map<MechanicNicheTag, NicheUtilityRollup> rollups = tracker.nichePopulationTracker().rollups();
        assertFalse(rollups.isEmpty());
        assertTrue(rollups.containsKey(MechanicNicheTag.NAVIGATION));
        assertTrue(rollups.get(MechanicNicheTag.NAVIGATION).activeArtifacts() >= 2);
    }

    @Test
    void ecologyPressureIsUtilityAwareNotFrequencyOnly() {
        EcosystemSaturationModel model = new EcosystemSaturationModel();
        NicheUtilityRollup crowdedWeak = new NicheUtilityRollup(MechanicNicheTag.NAVIGATION, 8, 40, 4, 1.5D, 30.0D);
        NicheUtilityRollup lowFrequencyUseful = new NicheUtilityRollup(MechanicNicheTag.RITUAL_STRANGE_UTILITY, 1, 12, 9, 6.0D, 10.0D);
        Map<MechanicNicheTag, NicheUtilityRollup> all = Map.of(
                crowdedWeak.niche(), crowdedWeak,
                lowFrequencyUseful.niche(), lowFrequencyUseful
        );

        RolePressureMetrics weakPressure = model.pressureFor(crowdedWeak.niche(), crowdedWeak, all);
        RolePressureMetrics usefulPressure = model.pressureFor(lowFrequencyUseful.niche(), lowFrequencyUseful, all);

        assertTrue(weakPressure.netPressure() < usefulPressure.netPressure());
        assertTrue(usefulPressure.retentionBias() > weakPressure.retentionBias());
    }

    @Test
    void endToEndEcologySignalsShowWeakCrowdedSuppressionAndUsefulLowFrequencySupportAndSpecialization() {
        NichePopulationTracker tracker = new NichePopulationTracker();

        for (int i = 0; i < 6; i++) {
            tracker.recordTelemetry(10L + i, Map.of(
                    "NAVIGATION_ANCHOR@ON_WORLD_SCAN", new MechanicUtilitySignal("NAVIGATION_ANCHOR@ON_WORLD_SCAN", 0.4D, 0.05D, 0.4D, 0.4D, 0.4D, 0.3D, 12L, 1L, 10.0D)
            ));
        }

        tracker.recordTelemetry(99L, Map.of(
                "RITUAL_CHANNEL@ON_MEMORY_EVENT", new MechanicUtilitySignal("RITUAL_CHANNEL@ON_MEMORY_EVENT", 4.2D, 0.65D, 0.8D, 0.1D, 0.1D, 0.1D, 10L, 8L, 6.0D)
        ));

        RolePressureMetrics crowdedWeakPressure = tracker.pressureFor(10L);
        RolePressureMetrics lowFrequencyUsefulPressure = tracker.pressureFor(99L);

        assertTrue(crowdedWeakPressure.netPressure() < lowFrequencyUsefulPressure.netPressure());

        ArtifactNicheProfile lowFrequencyProfile = tracker.nicheProfile(99L);
        assertNotNull(lowFrequencyProfile.specialization());
        assertTrue(lowFrequencyProfile.specialization().specializationScore() > 0.0D);
    }

    // -------------------------------------------------------------------------
    // Bifurcation tests (new)
    // -------------------------------------------------------------------------

    /**
     * Helpers: build a saturated NAVIGATION ecosystem suitable for triggering bifurcation.
     * 6 NAVIGATION artifacts with low utility + 1 RITUAL artifact with high utility.
     * This produces:
     *   NAVIGATION share ≈ 0.857 → saturationPenalty ≈ 0.45 (well above SATURATION_THRESHOLD=0.15)
     *   NAVIGATION artifacts have a single mechanic → specializationScore ≈ 1.0
     *     → meanSpecialization ≈ 1.0 (well above SPECIALIZATION_THRESHOLD=0.10)
     */
    private NichePopulationTracker buildSaturatedNavigationEcosystem(NicheBifurcationRegistry registry) {
        NichePopulationTracker tracker = new NichePopulationTracker(
                new EcosystemRoleClassifier(),
                new EcosystemSaturationModel(),
                registry);

        // 6 crowded NAVIGATION artifacts with low utility
        for (int i = 0; i < 6; i++) {
            tracker.recordTelemetry(10L + i, Map.of(
                    "NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                    new MechanicUtilitySignal("NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                            0.4D, 0.05D, 0.4D, 0.4D, 0.4D, 0.3D, 12L, 1L, 10.0D)
            ));
        }

        // 1 low-frequency RITUAL artifact with high utility (pulls mean utility up,
        // making NAVIGATION below-mean → saturationPenalty fires)
        tracker.recordTelemetry(99L, Map.of(
                "RITUAL_CHANNEL@ON_MEMORY_EVENT",
                new MechanicUtilitySignal("RITUAL_CHANNEL@ON_MEMORY_EVENT",
                        4.2D, 0.65D, 0.8D, 0.1D, 0.1D, 0.1D, 10L, 8L, 6.0D)
        ));

        return tracker;
    }

    @Test
    void bifurcationTriggersUnderSustainedHighPressure() {
        // sustainedWindowsRequired=1 so a single high-pressure evaluation fires immediately
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1);
        NichePopulationTracker tracker = buildSaturatedNavigationEcosystem(registry);

        tracker.evaluateBifurcations(System.currentTimeMillis());

        List<NicheBifurcation> bifurcations = registry.bifurcations();
        assertEquals(1, bifurcations.size(), "Exactly one bifurcation expected");

        NicheBifurcation b = bifurcations.get(0);
        assertEquals("NAVIGATION", b.parentNiche());
        assertTrue(b.childNicheA().startsWith("NAVIGATION_A"), "Child A should start with NAVIGATION_A");
        assertTrue(b.childNicheB().startsWith("NAVIGATION_B"), "Child B should start with NAVIGATION_B");
        assertTrue(b.saturationPressureAtCreation() >= NicheBifurcationRegistry.SATURATION_THRESHOLD);
        assertTrue(b.specializationPressureAtCreation() >= NicheBifurcationRegistry.SPECIALIZATION_THRESHOLD);
        assertEquals(2, registry.dynamicNicheCount(), "Two child niches should be registered");
    }

    @Test
    void bifurcationDoesNotOccurUnderLowPressure() {
        // Single artifact alone — no saturationPenalty fires (no below-mean peer) and
        // activeArtifacts=1 < MIN_ARTIFACT_COUNT=2
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1);
        NichePopulationTracker tracker = new NichePopulationTracker(
                new EcosystemRoleClassifier(), new EcosystemSaturationModel(), registry);

        tracker.recordTelemetry(1L, Map.of(
                "NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                new MechanicUtilitySignal("NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                        0.4D, 0.05D, 0.4D, 0.4D, 0.4D, 0.3D, 12L, 1L, 10.0D)
        ));

        tracker.evaluateBifurcations(System.currentTimeMillis());

        assertEquals(0, registry.bifurcations().size(), "No bifurcation expected under low pressure");
        assertEquals(0, registry.dynamicNicheCount());
    }

    @Test
    void bifurcationDoesNotOccurWhenArtifactCountBelowMinimum() {
        // Two niches but only 1 artifact in NAVIGATION → fails MIN_ARTIFACT_COUNT=2
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1);
        NichePopulationTracker tracker = new NichePopulationTracker(
                new EcosystemRoleClassifier(), new EcosystemSaturationModel(), registry);

        // 1 NAVIGATION artifact (low utility)
        tracker.recordTelemetry(1L, Map.of(
                "NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                new MechanicUtilitySignal("NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                        0.4D, 0.05D, 0.4D, 0.4D, 0.4D, 0.3D, 12L, 1L, 10.0D)
        ));
        // 1 RITUAL artifact (high utility — shifts mean up so NAVIGATION is below mean)
        tracker.recordTelemetry(2L, Map.of(
                "RITUAL_CHANNEL@ON_MEMORY_EVENT",
                new MechanicUtilitySignal("RITUAL_CHANNEL@ON_MEMORY_EVENT",
                        4.2D, 0.65D, 0.8D, 0.1D, 0.1D, 0.1D, 10L, 8L, 6.0D)
        ));

        tracker.evaluateBifurcations(System.currentTimeMillis());

        // Pressure conditions are met but only 1 NAVIGATION artifact < MIN_ARTIFACT_COUNT
        assertEquals(0, registry.bifurcations().size(),
                "No bifurcation expected when NAVIGATION has only 1 artifact");
    }

    @Test
    void newlyCreatedNichesCanReceiveArtifactAssignments() {
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1);
        NichePopulationTracker tracker = buildSaturatedNavigationEcosystem(registry);

        tracker.evaluateBifurcations(System.currentTimeMillis());

        // After bifurcation, classifier output should split assignments across child niches.
        assertEquals(1, registry.bifurcations().size());
        NicheBifurcation bifurcation = registry.bifurcations().get(0);
        String childA = bifurcation.childNicheA();
        String childB = bifurcation.childNicheB();

        int assignedToChildren = 0;
        for (long seed = 10L; seed < 16L; seed++) {
            String effective = tracker.effectiveNicheName(seed);
            if (effective.equals(childA) || effective.equals(childB)) {
                assignedToChildren++;
            } else {
                assertEquals("NAVIGATION", effective,
                        "Only parent niche should remain outside bounded child partition");
            }
        }

        int parentPopulation = 6;
        int minPartition = Math.max(1, (int) Math.ceil(parentPopulation * 0.10D));
        int maxPartition = Math.max(minPartition, (int) Math.floor(parentPopulation * 0.20D));
        assertTrue(assignedToChildren >= minPartition && assignedToChildren <= maxPartition,
                "Child partition must remain bounded to 10-20% of the parent cohort");
    }


    @Test
    void structuralPartitionPrefersLowerUtilityHalfAndRespectsCap() {
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1);
        NichePopulationTracker tracker = new NichePopulationTracker(
                new EcosystemRoleClassifier(), new EcosystemSaturationModel(), registry);

        for (int i = 0; i < 8; i++) {
            double utility = 0.10D + (i * 0.10D);
            tracker.recordTelemetry(200L + i, Map.of(
                    "NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                    new MechanicUtilitySignal("NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                            utility, 0.05D, 0.4D, 0.4D, 0.4D, 0.3D, 12L, 1L, 10.0D)
            ));
        }
        tracker.recordTelemetry(999L, Map.of(
                "RITUAL_CHANNEL@ON_MEMORY_EVENT",
                new MechanicUtilitySignal("RITUAL_CHANNEL@ON_MEMORY_EVENT",
                        4.8D, 0.70D, 0.8D, 0.1D, 0.1D, 0.1D, 10L, 8L, 6.0D)
        ));

        tracker.evaluateBifurcations(System.currentTimeMillis());

        NicheBifurcation bifurcation = registry.bifurcations().get(0);
        String childA = bifurcation.childNicheA();
        String childB = bifurcation.childNicheB();
        List<Long> childAssigned = java.util.stream.LongStream.range(200L, 208L)
                .filter(seed -> {
                    String effective = tracker.effectiveNicheName(seed);
                    return childA.equals(effective) || childB.equals(effective);
                })
                .boxed()
                .toList();

        assertEquals(1, childAssigned.size(),
                "Partition should resolve to one migrated artifact for a parent cohort of eight");
        assertTrue(childAssigned.stream().allMatch(seed -> seed <= 203L),
                "Structural partition should select from the lower-utility half of the parent cohort");
    }

    @Test
    void childNichesAreRecognizedAsValidDynamicNiches() {
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1);
        NichePopulationTracker tracker = buildSaturatedNavigationEcosystem(registry);

        tracker.evaluateBifurcations(System.currentTimeMillis());

        List<NicheBifurcation> bifurcations = registry.bifurcations();
        assertFalse(bifurcations.isEmpty());
        String childA = bifurcations.get(0).childNicheA();
        String childB = bifurcations.get(0).childNicheB();

        assertTrue(registry.isDynamicNiche(childA), childA + " should be recognized as dynamic");
        assertTrue(registry.isDynamicNiche(childB), childB + " should be recognized as dynamic");
        assertFalse(registry.isDynamicNiche("NAVIGATION"), "Static niche should not be dynamic");
        assertEquals(List.of(childA, childB), registry.childrenOf("NAVIGATION"));
    }

    @TempDir
    Path tempDir;

    @Test
    void telemetryRecordsNicheBifurcationEvents() {
        Path archivePath = tempDir.resolve("bifurcation-events.log");
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(archivePath);
        ScheduledEcosystemRollups rollups = new ScheduledEcosystemRollups(buffer, 1L);
        TelemetryAggregationService service = telemetryService(buffer, archive, rollups, 4, tempDir.resolve("bifurcation-snapshot.properties"));
        EcosystemTelemetryEmitter emitter = new EcosystemTelemetryEmitter(service);

        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1);
        NichePopulationTracker tracker = buildSaturatedNavigationEcosystem(registry);
        tracker.setTelemetryEmitter(emitter);

        tracker.evaluateBifurcations(System.currentTimeMillis());
        emitter.flushAll();

        List<EcosystemTelemetryEvent> events = archive.readAll();
        List<EcosystemTelemetryEvent> bifurcationEvents = events.stream()
                .filter(e -> e.type() == EcosystemTelemetryEventType.NICHE_BIFURCATION)
                .toList();

        assertFalse(bifurcationEvents.isEmpty(), "At least one NICHE_BIFURCATION event should be recorded");

        // The first bifurcation event describes the parent splitting into two children
        EcosystemTelemetryEvent mainEvent = bifurcationEvents.stream()
                .filter(e -> "niche_bifurcation".equals(e.attributes().get("event_type")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No main niche_bifurcation event found"));

        assertEquals("NAVIGATION", mainEvent.attributes().get("parent_niche"));
        assertNotNull(mainEvent.attributes().get("child_niche_a"));
        assertNotNull(mainEvent.attributes().get("child_niche_b"));
        assertNotEquals("na", mainEvent.attributes().get("saturation_pressure_at_creation"));
        assertNotEquals("na", mainEvent.attributes().get("specialization_pressure_at_creation"));

        // Child registration events should also exist
        long childEvents = bifurcationEvents.stream()
                .filter(e -> "niche_bifurcation_child".equals(e.attributes().get("event_type")))
                .count();
        assertEquals(2, childEvents, "Exactly 2 child registration events expected");
    }

    @Test
    void analyticsSnapshotIncludesEmergentNiches() {
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1);
        NichePopulationTracker tracker = buildSaturatedNavigationEcosystem(registry);

        tracker.evaluateBifurcations(System.currentTimeMillis());

        Map<String, Object> snapshot = tracker.analyticsSnapshot();

        @SuppressWarnings("unchecked")
        List<String> dynamicNiches = (List<String>) snapshot.get("dynamicNiches");
        assertNotNull(dynamicNiches, "dynamicNiches key should be present in analytics");
        assertFalse(dynamicNiches.isEmpty(), "Dynamic niches should appear in analytics snapshot");
        assertEquals(2, dynamicNiches.size(), "Two child niches should be listed");

        Integer bifurcationCount = (Integer) snapshot.get("bifurcationCount");
        assertEquals(1, bifurcationCount);

        @SuppressWarnings("unchecked")
        Map<String, Long> dynamicPop = (Map<String, Long>) snapshot.get("dynamicNichePopulation");
        assertNotNull(dynamicPop, "dynamicNichePopulation should be present");
        assertFalse(dynamicPop.isEmpty(), "At least one dynamic niche should have population");
    }

    @Test
    void telemetryBufferIncludesEmergentNicheInPopulationSnapshot() {
        Path archivePath = tempDir.resolve("bifurcation-pop.log");
        TelemetryAggregationBuffer buffer = new TelemetryAggregationBuffer();
        EcosystemHistoryArchive archive = new EcosystemHistoryArchive(archivePath);
        ScheduledEcosystemRollups rollupScheduler = new ScheduledEcosystemRollups(buffer, 1L);
        TelemetryAggregationService service = telemetryService(buffer, archive, rollupScheduler, 4, tempDir.resolve("bifurcation-pop-snapshot.properties"));
        EcosystemTelemetryEmitter emitter = new EcosystemTelemetryEmitter(service);

        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1);
        NichePopulationTracker tracker = buildSaturatedNavigationEcosystem(registry);
        tracker.setTelemetryEmitter(emitter);

        tracker.evaluateBifurcations(System.currentTimeMillis());
        for (long seed = 10L; seed < 16L; seed++) {
            String niche = tracker.effectiveNicheName(seed);
            emitter.emit(EcosystemTelemetryEventType.ABILITY_EXECUTION,
                    seed,
                    "lineage-test",
                    niche,
                    Map.of("ability_id", "a", "trigger", "ON_WORLD_SCAN", "mechanic", "SENSE_PING", "execution_status", "SUCCESS"));
        }
        emitter.flushAll();

        // Force a rollup to capture the current buffer state
        rollupScheduler.run(System.currentTimeMillis());

        Map<String, Long> nichePopulation = rollupScheduler.nichePopulationRollup().populationByNiche();

        NicheBifurcation bifurcation = registry.bifurcations().get(0);
        assertTrue(nichePopulation.containsKey(bifurcation.childNicheA())
                        || nichePopulation.containsKey(bifurcation.childNicheB()),
                "At least one child niche must appear in the NichePopulationRollup");
    }



    @Test
    void occupancyStateTracksEstablishedSupportedChildrenAcrossWindows() {
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1);
        long nowMs = System.currentTimeMillis();
        NicheBifurcation bifurcation = registry.evaluateBifurcation("NAVIGATION", 0.20D, 0.10D, 0.20D, 8, nowMs)
                .orElseThrow();

        for (int window = 1; window <= NicheBifurcationRegistry.ESTABLISHED_CHILD_WINDOWS; window++) {
            registry.recordChildOccupancy(bifurcation.childNicheA(), 1L, 0.25D, window);
            registry.collapseUnderpopulatedChildren(Map.of(bifurcation.childNicheA(), 1L), Map.of(bifurcation.childNicheA(), 0.25D), window);
        }

        assertEquals(NicheBifurcationRegistry.ESTABLISHED_CHILD_WINDOWS,
                registry.consecutiveOccupiedWindows(bifurcation.childNicheA()));
        assertEquals(NicheBifurcationRegistry.ESTABLISHED_CHILD_WINDOWS,
                registry.lastOccupiedWindow(bifurcation.childNicheA()));
        assertTrue(registry.isEstablishedOrHistoricalChild(bifurcation.childNicheA()));
        assertTrue(registry.hasLineageSupport(bifurcation.childNicheA()));
        assertTrue(registry.eligibleForContinuityProtection(bifurcation.childNicheA()));
    }

    @Test
    void emptiedProtectedChildIsNotRefilledByReconciliation() {
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1);
        NichePopulationTracker tracker = buildSaturatedNavigationEcosystem(registry);
        long nowMs = System.currentTimeMillis();

        tracker.evaluateBifurcations(nowMs);
        NicheBifurcation bifurcation = registry.bifurcations().get(0);
        String protectedChild = null;
        // Successful outcomes build lineage support until an occupied child is eligible for continuity protection.
        for (int window = 0; window < 20 && protectedChild == null; window++) {
            for (long seed = 10L; seed < 16L; seed++) {
                tracker.recordTelemetry(seed, "lineage-alpha", Map.of(
                        "NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                        new MechanicUtilitySignal("NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                                0.35D, 40.0D, 0.4D, 0.0D, 0.0D, 0.0D, 12L, 12L, 10.0D)
                ));
            }
            tracker.evaluateBifurcations(nowMs + 100L + window);
            protectedChild = java.util.stream.Stream.of(bifurcation.childNicheA(), bifurcation.childNicheB())
                    .filter(registry::eligibleForContinuityProtection)
                    .filter(child -> !occupants(tracker, child).isEmpty())
                    .findFirst().orElse(null);
        }
        assertNotNull(protectedChild, "Fixture must establish a protected, occupied child");

        occupants(tracker, protectedChild).forEach(tracker::markDiscarded);
        tracker.evaluateBifurcations(nowMs + 1_000L);

        // Unassigned NAVIGATION artifacts remain; only outcomes may move one into the emptied child.
        assertTrue(registry.isDynamicNiche(protectedChild), "Collapse hysteresis still holds the empty child");
        assertEquals(List.of(), occupants(tracker, protectedChild));
    }

    private static List<Long> occupants(NichePopulationTracker tracker, String child) {
        return java.util.stream.LongStream.range(10L, 16L).boxed()
                .filter(seed -> child.equals(tracker.effectiveNicheName(seed)))
                .toList();
    }

    @Test
    void establishedChildrenRequireMultipleEmptyWindowsBeforeCollapse() {
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1);
        long nowMs = System.currentTimeMillis();
        NicheBifurcation bifurcation = registry.evaluateBifurcation("NAVIGATION", 0.20D, 0.10D, 0.20D, 8, nowMs)
                .orElseThrow();
        registry.setChildBirthWindow(bifurcation.childNicheA(), 1);

        for (int window = 1; window <= NicheBifurcationRegistry.ESTABLISHED_CHILD_WINDOWS; window++) {
            registry.collapseUnderpopulatedChildren(Map.of(bifurcation.childNicheA(), 1L), Map.of(), window + 10);
        }

        registry.collapseUnderpopulatedChildren(Map.of(), Map.of(bifurcation.childNicheA(), 0.45D), 20);
        registry.collapseUnderpopulatedChildren(Map.of(), Map.of(bifurcation.childNicheA(), 0.45D), 21);
        assertTrue(registry.isDynamicNiche(bifurcation.childNicheA()), "Established child should survive short collapse streaks");

        registry.collapseUnderpopulatedChildren(Map.of(), Map.of(bifurcation.childNicheA(), 0.45D), 22);
        assertTrue(registry.isDynamicNiche(bifurcation.childNicheA()),
                "Established supported child should require an extra empty window before collapse");
        registry.collapseUnderpopulatedChildren(Map.of(), Map.of(bifurcation.childNicheA(), 0.45D), 23);
        registry.collapseUnderpopulatedChildren(Map.of(), Map.of(bifurcation.childNicheA(), 0.45D), 24);
        registry.collapseUnderpopulatedChildren(Map.of(), Map.of(bifurcation.childNicheA(), 0.45D), 25);
        assertFalse(registry.isDynamicNiche(bifurcation.childNicheA()),
                "Established child should still collapse after the extended supported streak");
    }

    @Test
    void reentryStabilizationDelaysImmediateRecollapse() {
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1);
        long nowMs = System.currentTimeMillis();
        NicheBifurcation bifurcation = registry.evaluateBifurcation("NAVIGATION", 0.20D, 0.10D, 0.20D, 8, nowMs)
                .orElseThrow();
        registry.setChildBirthWindow(bifurcation.childNicheA(), 1);

        registry.collapseUnderpopulatedChildren(Map.of(bifurcation.childNicheA(), 1L), Map.of(), 10);
        registry.collapseUnderpopulatedChildren(Map.of(), Map.of(), 11);
        registry.collapseUnderpopulatedChildren(Map.of(bifurcation.childNicheA(), 1L), Map.of(), 12);

        assertTrue(registry.hasReentryStabilization(bifurcation.childNicheA()),
                "Child re-entry should start a short stabilization interval");

        registry.collapseUnderpopulatedChildren(Map.of(), Map.of(), 13);
        assertTrue(registry.isDynamicNiche(bifurcation.childNicheA()),
                "Re-entry stabilization should prevent immediate collapse on the next empty window");
    }

    @Test
    void recentCollapseDoesNotClockGateSameParentRebifurcation() {
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1);
        long nowMs = System.currentTimeMillis();
        NicheBifurcation first = registry.evaluateBifurcation("NAVIGATION", 0.20D, 0.10D, 0.20D, 8, nowMs)
                .orElseThrow();
        registry.setChildBirthWindow(first.childNicheA(), 1);
        registry.setChildBirthWindow(first.childNicheB(), 1);

        registry.collapseUnderpopulatedChildren(Map.of(), Map.of(), 10);
        registry.collapseUnderpopulatedChildren(Map.of(), Map.of(), 11);
        registry.collapseUnderpopulatedChildren(Map.of(), Map.of(), 12);
        assertEquals(0, registry.dynamicNicheCount(), "Both children should collapse after sustained empty windows");

        assertTrue(registry.evaluateBifurcation("NAVIGATION", 0.20D, 0.10D, 0.20D, 8, nowMs + 1_000L).isPresent(),
                "A collapse must not hold the parent back on a clock; sustained pressure splits it again");
    }

    @Test
    void secondEvaluationIsNotDecidedByAClock() {
        // There is no split cooldown: the next evaluation is decided by pressure, not time.
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1);
        NichePopulationTracker tracker = buildSaturatedNavigationEcosystem(registry);

        long nowMs = System.currentTimeMillis();
        tracker.evaluateBifurcations(nowMs);
        assertEquals(1, registry.bifurcations().size(), "First bifurcation should occur");

        tracker.evaluateBifurcations(nowMs + 1L);
        String gate = registry.lastGate("NAVIGATION").orElseThrow().gate();
        assertFalse(gate.endsWith("COOLDOWN"), "Second evaluation was decided by " + gate);
    }

    @Test
    void sustainedPressureRequirementPreventsEarlyBifurcation() {
        // sustainedWindowsRequired=2 with parent-share below adaptive threshold:
        // first window builds up, second triggers.
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(2);

        long nowMs = System.currentTimeMillis();
        assertTrue(registry.evaluateBifurcation("NAVIGATION", 0.20D, 0.10D, 0.08D, 8, nowMs).isEmpty(),
                "No bifurcation after only 1 high-pressure window when parent share is below adaptive fast-path");
        assertTrue(registry.evaluateBifurcation("NAVIGATION", 0.20D, 0.10D, 0.08D, 8, nowMs + 100L).isPresent(),
                "Bifurcation should trigger after 2 sustained windows when parent share is below adaptive fast-path");
    }

    @Test
    void noNicheCapDecidesTheNextSplit() {
        // There is no niche-count cap: pressure decides the next evaluation.
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1);
        NichePopulationTracker tracker = buildSaturatedNavigationEcosystem(registry);

        long nowMs = System.currentTimeMillis();
        tracker.evaluateBifurcations(nowMs);
        assertEquals(1, registry.bifurcations().size(), "First bifurcation should occur");

        tracker.evaluateBifurcations(nowMs + 200L);
        String gate = registry.lastGate("NAVIGATION").orElseThrow().gate();
        assertFalse(gate.endsWith("_CAP"), "Second evaluation was decided by " + gate);
    }

    @Test
    void bifurcationSeedsLineageAffinityWithoutInvertingParentFitness() {
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1);
        NichePopulationTracker tracker = buildSaturatedNavigationEcosystem(registry);
        for (long seed = 10L; seed < 16L; seed++) {
            tracker.recordTelemetry(seed, seed < 13L ? "lineage-alpha" : "lineage-beta", Map.of(
                    "NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                    new MechanicUtilitySignal("NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                            0.4D, 0.05D, 0.4D, 0.4D, 0.4D, 0.3D, 12L, 1L, 10.0D)));
        }

        tracker.evaluateBifurcations(System.currentTimeMillis());
        assertFalse(registry.bifurcations().isEmpty(), "Expected a split");

        // A split must not tax the parent's remaining members or reward the child's: outcomes keep their own value.
        for (long seed = 10L; seed < 16L; seed++) {
            if ("NAVIGATION".equals(tracker.effectiveNicheName(seed))) {
                assertEquals(1.0D, tracker.nicheAdoptionFitnessMultiplier(seed), 0.0D,
                        "Parent member " + seed + " must not carry a post-split fitness inversion");
            }
        }
        assertEquals(1.0D, tracker.nicheAdoptionFitnessMultiplier(99L), 0.0D, "Unrelated niches are unaffected");

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Double>> affinity = (Map<String, Map<String, Double>>) tracker.analyticsSnapshot().get("lineageAffinity");
        assertNotNull(affinity);
        assertTrue(affinity.containsKey("NAVIGATION"), "Parent niche should include affinity map");
        assertTrue(affinity.get("NAVIGATION").keySet().stream().anyMatch(k -> k.startsWith("lineage-alpha|")));
        assertTrue(affinity.get("NAVIGATION").keySet().stream().anyMatch(k -> k.startsWith("lineage-beta|")));
    }

    @Test
    void migratedArtifactFollowsItsOutcomesImmediately() {
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1);
        NichePopulationTracker tracker = buildSaturatedNavigationEcosystem(registry);

        tracker.evaluateBifurcations(System.currentTimeMillis());
        assertEquals(1, registry.bifurcations().size(), "Expected one split");
        long migratedSeed = java.util.stream.LongStream.range(10L, 16L)
                .filter(seed -> !"NAVIGATION".equals(tracker.effectiveNicheName(seed)))
                .findFirst().orElseThrow(() -> new AssertionError("Expected at least one child member"));

        tracker.recordTelemetry(migratedSeed, Map.of(
                "RITUAL_CHANNEL@ON_MEMORY_EVENT",
                new MechanicUtilitySignal("RITUAL_CHANNEL@ON_MEMORY_EVENT",
                        4.8D, 0.75D, 0.8D, 0.1D, 0.1D, 0.1D, 10L, 8L, 6.0D)));

        // No lock holds a member in place: its niche follows its measured outcomes at once.
        assertEquals(tracker.nicheProfile(migratedSeed).dominantNiche().name(), tracker.effectiveNicheName(migratedSeed));
    }

    private TelemetryAggregationService telemetryService(TelemetryAggregationBuffer buffer,
                                                         EcosystemHistoryArchive archive,
                                                         ScheduledEcosystemRollups rollups,
                                                         int archiveBatchSize,
                                                         Path snapshotPath) {
        TelemetryRollupSnapshotStore snapshotStore = new TelemetryRollupSnapshotStore(snapshotPath);
        RollupStateHydrator hydrator = new RollupStateHydrator(snapshotStore, archive, 8);
        return new TelemetryAggregationService(buffer, archive, rollups, archiveBatchSize, snapshotStore, hydrator);
    }
}
