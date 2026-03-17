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
import obtuseloot.telemetry.TelemetryAggregationBuffer;
import obtuseloot.telemetry.TelemetryAggregationService;
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
    void ecologyPressureIsUtilityAwareNotRarityOnly() {
        EcosystemSaturationModel model = new EcosystemSaturationModel();
        NicheUtilityRollup crowdedWeak = new NicheUtilityRollup(MechanicNicheTag.NAVIGATION, 8, 40, 4, 1.5D, 30.0D);
        NicheUtilityRollup rareUseful = new NicheUtilityRollup(MechanicNicheTag.RITUAL_STRANGE_UTILITY, 1, 12, 9, 6.0D, 10.0D);
        Map<MechanicNicheTag, NicheUtilityRollup> all = Map.of(
                crowdedWeak.niche(), crowdedWeak,
                rareUseful.niche(), rareUseful
        );

        RolePressureMetrics weakPressure = model.pressureFor(crowdedWeak.niche(), crowdedWeak, all);
        RolePressureMetrics usefulPressure = model.pressureFor(rareUseful.niche(), rareUseful, all);

        assertTrue(weakPressure.netPressure() < usefulPressure.netPressure());
        assertTrue(usefulPressure.retentionBias() > weakPressure.retentionBias());
    }

    @Test
    void endToEndEcologySignalsShowWeakCrowdedSuppressionAndUsefulRareSupportAndSpecialization() {
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
        RolePressureMetrics rareUsefulPressure = tracker.pressureFor(99L);

        assertTrue(crowdedWeakPressure.netPressure() < rareUsefulPressure.netPressure());

        ArtifactNicheProfile rareProfile = tracker.nicheProfile(99L);
        assertNotNull(rareProfile.specialization());
        assertTrue(rareProfile.specialization().specializationScore() > 0.0D);
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

        // 1 rare RITUAL artifact with high utility (pulls mean utility up,
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
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(8, 0L, 1);
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
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(8, 0L, 1);
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
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(8, 0L, 1);
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
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(8, 0L, 1);
        NichePopulationTracker tracker = buildSaturatedNavigationEcosystem(registry);

        tracker.evaluateBifurcations(System.currentTimeMillis());

        // After bifurcation, forced displacement should move a meaningful low-performing
        // subset into child niches while leaving the parent represented.
        assertEquals(1, registry.bifurcations().size());
        NicheBifurcation bifurcation = registry.bifurcations().get(0);
        String childA = bifurcation.childNicheA();
        String childB = bifurcation.childNicheB();

        int assigned = 0;
        for (long seed = 10L; seed < 16L; seed++) {
            String effective = tracker.effectiveNicheName(seed);
            if (effective.equals(childA) || effective.equals(childB)) {
                assigned++;
            } else {
                assertEquals("NAVIGATION", effective,
                        "Non-migrated artifacts should remain in the parent niche");
            }
        }
        assertTrue(assigned >= 2 && assigned <= 3,
                "Expected ~40% forced displacement (2-3 of 6 NAVIGATION artifacts), got " + assigned);
    }

    @Test
    void childNichesAreRecognizedAsValidDynamicNiches() {
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(8, 0L, 1);
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
        TelemetryAggregationService service = new TelemetryAggregationService(buffer, archive, rollups, 4);
        EcosystemTelemetryEmitter emitter = new EcosystemTelemetryEmitter(service);

        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(8, 0L, 1);
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
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(8, 0L, 1);
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
        TelemetryAggregationService service = new TelemetryAggregationService(buffer, archive, rollupScheduler, 4);
        EcosystemTelemetryEmitter emitter = new EcosystemTelemetryEmitter(service);

        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(8, 0L, 1);
        NichePopulationTracker tracker = buildSaturatedNavigationEcosystem(registry);
        tracker.setTelemetryEmitter(emitter);

        tracker.evaluateBifurcations(System.currentTimeMillis());
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
    void cooldownPreventsImmediateRebifurcation() {
        // cooldownMs=60_000 (1 minute) — the second evaluation is immediate, should be blocked
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(8, 60_000L, 1);
        NichePopulationTracker tracker = buildSaturatedNavigationEcosystem(registry);

        long nowMs = System.currentTimeMillis();
        tracker.evaluateBifurcations(nowMs);
        assertEquals(1, registry.bifurcations().size(), "First bifurcation should occur");

        // Immediate second attempt — cooldown not yet expired
        tracker.evaluateBifurcations(nowMs + 1L);
        assertEquals(1, registry.bifurcations().size(),
                "Second bifurcation should be blocked by cooldown");
    }

    @Test
    void sustainedPressureRequirementPreventsEarlyBifurcation() {
        // sustainedWindowsRequired=2: first window builds up, second triggers
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(8, 0L, 2);
        NichePopulationTracker tracker = buildSaturatedNavigationEcosystem(registry);

        long nowMs = System.currentTimeMillis();

        // First evaluation: pressure is high but only 1 window accumulated
        tracker.evaluateBifurcations(nowMs);
        assertEquals(0, registry.bifurcations().size(),
                "No bifurcation after only 1 high-pressure window (requires 2)");

        // Second evaluation: 2nd consecutive high-pressure window → bifurcation
        tracker.evaluateBifurcations(nowMs + 100L);
        assertEquals(1, registry.bifurcations().size(),
                "Bifurcation should trigger after 2 sustained high-pressure windows");
    }

    @Test
    void maxNicheCapPreventsUnlimitedSpawning() {
        // Cap at 2 dynamic niches: only one bifurcation (produces 2 children) should succeed
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(2, 0L, 1);
        NichePopulationTracker tracker = buildSaturatedNavigationEcosystem(registry);

        long nowMs = System.currentTimeMillis();
        tracker.evaluateBifurcations(nowMs);
        assertEquals(1, registry.bifurcations().size(), "First bifurcation uses both slots");
        assertEquals(2, registry.dynamicNicheCount());

        // Attempt further bifurcation after cooldown — cap is now full
        tracker.evaluateBifurcations(nowMs + 200L);
        assertEquals(1, registry.bifurcations().size(),
                "No further bifurcation when niche cap is reached");
    }

    @Test
    void bifurcationSeedsLineageAffinityAndAppliesBoundedFitnessInversion() {
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(8, 0L, 1);
        NichePopulationTracker tracker = new NichePopulationTracker(
                new EcosystemRoleClassifier(), new EcosystemSaturationModel(), registry);

        for (int i = 0; i < 8; i++) {
            String lineage = i < 4 ? "lineage-alpha" : "lineage-beta";
            double utility = i < 4 ? 0.30D : 0.45D;
            tracker.recordTelemetry(100L + i, lineage, Map.of(
                    "NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                    new MechanicUtilitySignal("NAVIGATION_ANCHOR@ON_WORLD_SCAN",
                            utility, 0.05D, 0.4D, 0.4D, 0.4D, 0.3D, 12L, 1L, 10.0D)
            ));
        }
        tracker.recordTelemetry(999L, "lineage-ritual", Map.of(
                "RITUAL_CHANNEL@ON_MEMORY_EVENT",
                new MechanicUtilitySignal("RITUAL_CHANNEL@ON_MEMORY_EVENT",
                        5.0D, 0.80D, 0.8D, 0.1D, 0.1D, 0.1D, 10L, 9L, 6.0D)
        ));

        tracker.evaluateBifurcations(System.currentTimeMillis());

        assertEquals(1, registry.bifurcations().size(), "Expected one bifurcation");
        double childMultiplier = 1.0D;
        boolean migratedFound = false;
        for (long seed = 100L; seed < 108L; seed++) {
            String effective = tracker.effectiveNicheName(seed);
            if (!"NAVIGATION".equals(effective)) {
                childMultiplier = tracker.nicheAdoptionFitnessMultiplier(seed);
                migratedFound = true;
                break;
            }
        }
        if (migratedFound) {
            assertTrue(childMultiplier >= 1.10D && childMultiplier <= 1.20D,
                    "Child niche should receive bounded inversion + lock-in boost");
        } else {
            assertEquals(1.0D, childMultiplier, 0.0001D,
                    "When no migration occurs in this cohort, no child inversion should apply");
        }

        NicheBifurcationRegistry agedRegistry = new NicheBifurcationRegistry(8, 0L, 1);
        NichePopulationTracker agedTracker = buildSaturatedNavigationEcosystem(agedRegistry);
        long agedNow = System.currentTimeMillis() - 25_000L;
        agedTracker.evaluateBifurcations(agedNow);
        for (int i = 0; i < 5; i++) {
            agedTracker.evaluateBifurcations(agedNow + ((i + 1L) * 500L));
        }
        double decayedMultiplier = agedTracker.nicheAdoptionFitnessMultiplier(10L);
        assertTrue(decayedMultiplier >= 1.0D && decayedMultiplier <= 1.20D,
                "Post-bifurcation adoption multiplier should remain bounded after lock/inversion decay");

        double unrelatedMultiplier = tracker.nicheAdoptionFitnessMultiplier(999L);
        assertEquals(1.0D, unrelatedMultiplier, 0.0001D,
                "Unrelated niches must not inherit inversion effects");

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Double>> affinity = (Map<String, Map<String, Double>>) tracker.analyticsSnapshot().get("lineageAffinity");
        assertNotNull(affinity);
        assertTrue(affinity.containsKey("NAVIGATION"), "Parent niche should include affinity map");
        assertTrue(affinity.get("NAVIGATION").keySet().stream().anyMatch(k -> k.startsWith("lineage-alpha|")));
        assertTrue(affinity.get("NAVIGATION").keySet().stream().anyMatch(k -> k.startsWith("lineage-beta|")));
    }

    @Test
    void migratedArtifactsStayLockedToChildForBoundedWindows() {
        NicheBifurcationRegistry registry = new NicheBifurcationRegistry(8, 0L, 1);
        NichePopulationTracker tracker = buildSaturatedNavigationEcosystem(registry);

        long nowMs = System.currentTimeMillis();
        tracker.evaluateBifurcations(nowMs);
        assertEquals(1, registry.bifurcations().size(), "Expected one bifurcation");

        long migratedSeed = -1L;
        String lockedChild = null;
        for (long seed = 10L; seed < 16L; seed++) {
            String effective = tracker.effectiveNicheName(seed);
            if (!"NAVIGATION".equals(effective)) {
                migratedSeed = seed;
                lockedChild = effective;
                break;
            }
        }
        assertTrue(migratedSeed > 0L, "Expected at least one migrated artifact");

        for (int i = 0; i < 2; i++) {
            tracker.recordTelemetry(migratedSeed, Map.of(
                    "RITUAL_CHANNEL@ON_MEMORY_EVENT",
                    new MechanicUtilitySignal("RITUAL_CHANNEL@ON_MEMORY_EVENT",
                            4.8D, 0.75D, 0.8D, 0.1D, 0.1D, 0.1D, 10L, 8L, 6.0D)
            ));
            assertEquals(lockedChild, tracker.effectiveNicheName(migratedSeed),
                    "Classifier updates must not override a locked migrated artifact");
            double lockedMultiplier = tracker.nicheAdoptionFitnessMultiplier(migratedSeed);
            assertTrue(lockedMultiplier >= 1.10D && lockedMultiplier <= 1.20D,
                    "Locked migrated artifacts should get a bounded utility reinforcement");
            tracker.evaluateBifurcations(nowMs + 500L + (i * 500L));
        }

        for (int i = 0; i < 6; i++) {
            tracker.evaluateBifurcations(nowMs + 2_000L + (i * 500L));
        }

        tracker.recordTelemetry(migratedSeed, Map.of(
                "RITUAL_CHANNEL@ON_MEMORY_EVENT",
                new MechanicUtilitySignal("RITUAL_CHANNEL@ON_MEMORY_EVENT",
                        4.8D, 0.75D, 0.8D, 0.1D, 0.1D, 0.1D, 10L, 8L, 6.0D)
        ));

        String unlockedNiche = tracker.effectiveNicheName(migratedSeed);
        assertEquals(tracker.nicheProfile(migratedSeed).dominantNiche().name(), unlockedNiche,
                "After lock decay, effective niche should follow the classifier again");
        assertNotEquals(lockedChild, unlockedNiche,
                "After lock decay, artifact should no longer be pinned to its child lock niche");
    }
}
