package obtuseloot.ecosystem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 8.1 — Production Hardening & Safeguards.
 *
 * Verifies:
 * <ul>
 *   <li>Category dominance guard activates above threshold and is absent under normal distribution.</li>
 *   <li>Template dominance guard activates above threshold and is absent under normal distribution.</li>
 *   <li>Candidate pool collapse guard fires when pool is below threshold.</li>
 *   <li>Telemetry outputs consistent values across repeated observations.</li>
 *   <li>Snapshot/replay is deterministic — same inputs produce identical snapshots.</li>
 *   <li>Rolling window correctly evicts old observations and normalises over time.</li>
 *   <li>Failure signal detector reports correctly for each failure type.</li>
 * </ul>
 */
class ProductionSafetyTest {

    private static final Logger NO_OP_LOGGER = Logger.getLogger("test-noop");

    private ProductionSafetyConfig config;
    private ProductionSafetyGuards guards;
    private EcosystemHealthMonitor monitor;

    @BeforeEach
    void setUp() {
        config = ProductionSafetyConfig.defaults();
        guards = new ProductionSafetyGuards(config, NO_OP_LOGGER);
        monitor = new EcosystemHealthMonitor(guards, NO_OP_LOGGER,
                config.snapshotIntervalEvents(), config.periodicConsoleLog());
    }

    // -------------------------------------------------------------------------
    // RollingDistributionWindow
    // -------------------------------------------------------------------------

    @Test
    void rollingWindowAverageShareIsAccurate() {
        RollingDistributionWindow window = new RollingDistributionWindow(4);
        window.record(Map.of("a", 1.0, "b", 0.0));
        window.record(Map.of("a", 0.5, "b", 0.5));
        window.record(Map.of("a", 0.0, "b", 1.0));

        // After 3 snapshots: a sums = 1.5, b sums = 1.5
        assertEquals(0.5, window.averageShare("a"), 1e-9);
        assertEquals(0.5, window.averageShare("b"), 1e-9);
    }

    @Test
    void rollingWindowEvictsOldestOnOverflow() {
        RollingDistributionWindow window = new RollingDistributionWindow(2);
        window.record(Map.of("chaos", 1.0));   // snapshot 1
        window.record(Map.of("chaos", 1.0));   // snapshot 2 — window full
        window.record(Map.of("chaos", 0.0));   // snapshot 3 evicts snapshot 1

        // Window should now contain snapshots 2 and 3: average = 0.5
        assertEquals(0.5, window.averageShare("chaos"), 1e-9);
    }

    @Test
    void rollingWindowResetClearsState() {
        RollingDistributionWindow window = new RollingDistributionWindow(10);
        window.record(Map.of("chaos", 0.9));
        window.reset();
        assertEquals(0, window.windowFill());
        assertEquals(0.0, window.averageShare("chaos"), 1e-9);
    }

    @Test
    void rollingWindowReturnsZeroWhenEmpty() {
        RollingDistributionWindow window = new RollingDistributionWindow(10);
        assertEquals(0.0, window.averageShare("precision"), 1e-9);
        assertTrue(window.averageShares().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Category dominance guard
    // -------------------------------------------------------------------------

    @Test
    void categoryGuardActivatesWhenDominant() {
        // Fill window with dominant chaos distribution (70% > 65% threshold)
        Map<String, Double> dominant = Map.of(
                "chaos", 0.70, "precision", 0.15, "survival", 0.10, "mobility", 0.05);
        for (int i = 0; i < config.rollingWindowSize(); i++) {
            guards.recordCategoryDistribution(dominant);
        }
        double multiplier = guards.categoryGuardMultiplier("chaos");
        assertEquals(config.categorySuppressionFactor(), multiplier, 1e-9,
                "Guard must fire for chaos at 70% (threshold 65%)");
        assertTrue(multiplier < 1.0, "Suppression multiplier must be less than 1.0");
    }

    @Test
    void categoryGuardDoesNotTriggerUnderNormalDistribution() {
        // Evenly distributed — no single category exceeds 65%
        Map<String, Double> balanced = Map.of(
                "chaos", 0.20, "precision", 0.20, "survival", 0.20,
                "mobility", 0.20, "consistency", 0.10, "brutality", 0.10);
        for (int i = 0; i < config.rollingWindowSize(); i++) {
            guards.recordCategoryDistribution(balanced);
        }
        for (String cat : balanced.keySet()) {
            assertEquals(1.0, guards.categoryGuardMultiplier(cat), 1e-9,
                    "No guard should fire for category: " + cat);
        }
    }

    @Test
    void categoryGuardIsReversibleAfterWindowNormalises() {
        // Saturate with dominant chaos
        Map<String, Double> dominant = Map.of("chaos", 0.80, "precision", 0.20);
        for (int i = 0; i < config.rollingWindowSize(); i++) {
            guards.recordCategoryDistribution(dominant);
        }
        assertTrue(guards.categoryGuardMultiplier("chaos") < 1.0,
                "Guard should be active after window saturation");

        // Fill window with balanced distribution — guard should deactivate
        Map<String, Double> balanced = Map.of("chaos", 0.30, "precision", 0.30,
                "survival", 0.20, "mobility", 0.20);
        for (int i = 0; i < config.rollingWindowSize(); i++) {
            guards.recordCategoryDistribution(balanced);
        }
        assertEquals(1.0, guards.categoryGuardMultiplier("chaos"), 1e-9,
                "Guard must revert once distribution normalises");
    }

    @Test
    void categoryGuardMultiplierIsBounded() {
        // Even extreme dominance should not produce a multiplier below the configured factor
        Map<String, Double> totalDomination = Map.of("chaos", 1.0);
        for (int i = 0; i < config.rollingWindowSize(); i++) {
            guards.recordCategoryDistribution(totalDomination);
        }
        double mult = guards.categoryGuardMultiplier("chaos");
        assertEquals(config.categorySuppressionFactor(), mult, 1e-9,
                "Multiplier must equal suppression factor — no further degradation");
    }

    // -------------------------------------------------------------------------
    // Template dominance guard
    // -------------------------------------------------------------------------

    @Test
    void templateGuardActivatesWhenDominant() {
        Map<String, Double> dominant = Map.of("on_kill", 0.60, "on_hit", 0.20, "on_move", 0.20);
        for (int i = 0; i < config.rollingWindowSize(); i++) {
            guards.recordTemplateDistribution(dominant);
        }
        double multiplier = guards.templateGuardMultiplier("on_kill");
        assertEquals(config.templateSuppressionFactor(), multiplier, 1e-9,
                "Template guard must fire for on_kill at 60% (threshold 55%)");
    }

    @Test
    void templateGuardDoesNotTriggerUnderNormalDistribution() {
        Map<String, Double> balanced = Map.of(
                "on_kill", 0.25, "on_hit", 0.25, "on_move", 0.25, "on_low_hp", 0.25);
        for (int i = 0; i < config.rollingWindowSize(); i++) {
            guards.recordTemplateDistribution(balanced);
        }
        for (String tmpl : balanced.keySet()) {
            assertEquals(1.0, guards.templateGuardMultiplier(tmpl), 1e-9,
                    "No template guard should fire: " + tmpl);
        }
    }

    // -------------------------------------------------------------------------
    // Candidate pool guard
    // -------------------------------------------------------------------------

    @Test
    void poolGuardFiresWhenBelowThreshold() {
        guards.recordPoolSize(1);
        assertTrue(guards.isPoolCollapsed(), "Pool size 1 is below collapse threshold 2");
        assertEquals(1, guards.poolCollapseEventCount());
    }

    @Test
    void poolGuardDoesNotFireAtThreshold() {
        guards.recordPoolSize(config.candidatePoolCollapseThreshold());
        assertFalse(guards.isPoolCollapsed(),
                "Pool exactly at threshold must not trigger collapse");
        assertEquals(0, guards.poolCollapseEventCount());
    }

    @Test
    void poolGuardDoesNotFireAboveThreshold() {
        guards.recordPoolSize(5);
        assertFalse(guards.isPoolCollapsed());
        assertEquals(0, guards.poolCollapseEventCount());
    }

    @Test
    void poolGuardAveragePoolSizeIsAccurate() {
        guards.recordPoolSize(4);
        guards.recordPoolSize(6);
        assertEquals(5.0, guards.averagePoolSize(), 1e-9);
    }

    // -------------------------------------------------------------------------
    // EcosystemHealthMonitor and snapshots
    // -------------------------------------------------------------------------

    @Test
    void snapshotContainsCategoryAndTemplateShares() {
        Map<String, Double> cats = Map.of("chaos", 0.50, "precision", 0.30, "survival", 0.20);
        Map<String, Double> tmpls = Map.of("on_kill", 0.40, "on_hit", 0.60);
        monitor.recordEvaluation(cats, tmpls, 5);

        ProductionSafetySnapshot snap = monitor.captureSnapshot();
        assertFalse(snap.categoryShares().isEmpty());
        assertFalse(snap.templateShares().isEmpty());
        assertEquals(1, snap.windowFill());
    }

    @Test
    void snapshotDiversityIndexIsPositiveForMultipleCategories() {
        Map<String, Double> cats = Map.of(
                "chaos", 0.25, "precision", 0.25, "survival", 0.25, "mobility", 0.25);
        for (int i = 0; i < 10; i++) {
            monitor.recordEvaluation(cats, Map.of("on_kill", 0.5, "on_hit", 0.5), -1);
        }
        ProductionSafetySnapshot snap = monitor.captureSnapshot();
        assertTrue(snap.diversityIndex() > 0.0, "Diversity index must be positive for multi-category distribution");
    }

    @Test
    void snapshotIsImmutableAndConsistent() {
        Map<String, Double> cats = Map.of("chaos", 0.60, "precision", 0.40);
        monitor.recordEvaluation(cats, Map.of(), -1);

        ProductionSafetySnapshot snap1 = monitor.captureSnapshot();
        ProductionSafetySnapshot snap2 = monitor.captureSnapshot();

        assertEquals(snap1.diversityIndex(), snap2.diversityIndex(), 1e-9,
                "Same observation state must produce same diversity index");
        assertEquals(snap1.categoryShares(), snap2.categoryShares(),
                "Snapshot category shares must be identical across consecutive captures");
    }

    @Test
    void snapshotReplayIsDeterministic() {
        // Record the same observations in two separate guard instances
        ProductionSafetyGuards guards2 = new ProductionSafetyGuards(config, NO_OP_LOGGER);
        EcosystemHealthMonitor monitor2 = new EcosystemHealthMonitor(guards2, NO_OP_LOGGER, 0, false);

        Map<String, Double> cats = Map.of("chaos", 0.40, "survival", 0.35, "mobility", 0.25);
        Map<String, Double> tmpls = Map.of("on_kill", 0.60, "on_move", 0.40);

        for (int i = 0; i < 20; i++) {
            monitor.recordEvaluation(cats, tmpls, 3);
            monitor2.recordEvaluation(cats, tmpls, 3);
        }

        ProductionSafetySnapshot snap1 = monitor.captureSnapshot();
        ProductionSafetySnapshot snap2 = monitor2.captureSnapshot();

        assertEquals(snap1.diversityIndex(), snap2.diversityIndex(), 1e-9,
                "Two monitors with identical inputs must produce identical diversity index");
        assertEquals(snap1.categoryShares(), snap2.categoryShares(),
                "Two monitors with identical inputs must produce identical category shares");
        assertEquals(snap1.activeGuards(), snap2.activeGuards(),
                "Active guards must match between replayed monitors");
    }

    @Test
    void resetMetricsClearsAllState() {
        Map<String, Double> dominant = Map.of("chaos", 0.90, "precision", 0.10);
        for (int i = 0; i < config.rollingWindowSize(); i++) {
            monitor.recordEvaluation(dominant, Map.of(), 1);
        }
        monitor.resetMetrics();

        assertEquals(0, monitor.eventCount());
        assertEquals(0, guards.categoryWindow().windowFill());
        ProductionSafetySnapshot snap = monitor.captureSnapshot();
        assertTrue(snap.activeGuards().isEmpty(), "No guards should be active after reset");
        assertTrue(snap.categoryShares().isEmpty(), "No category shares after reset");
    }

    // -------------------------------------------------------------------------
    // Failure signal detection
    // -------------------------------------------------------------------------

    @Test
    void failureDetectorReportsCategoryCollapse() {
        FailureSignalDetector detector = new FailureSignalDetector(NO_OP_LOGGER);
        ProductionSafetySnapshot snap = new ProductionSafetySnapshot(
                System.currentTimeMillis(),
                Map.of("chaos", 0.95, "precision", 0.05),
                Map.of(),
                5.0, 0.1, 10,
                List.of(), List.of());

        List<FailureSignal> signals = detector.detect(snap);
        assertTrue(signals.stream().anyMatch(s -> s.type() == FailureSignal.Type.CATEGORY_COLLAPSE),
                "Category collapse must be detected when category ≥ 90%");
    }

    @Test
    void failureDetectorReportsLongTailDeath() {
        FailureSignalDetector detector = new FailureSignalDetector(NO_OP_LOGGER);
        ProductionSafetySnapshot snap = new ProductionSafetySnapshot(
                System.currentTimeMillis(),
                Map.of("chaos", 0.92, "precision", 0.01, "survival", 0.01, "mobility", 0.01,
                        "consistency", 0.04, "brutality", 0.01),
                Map.of(),
                5.0, 0.1, 10,
                List.of(), List.of());

        List<FailureSignal> signals = detector.detect(snap);
        assertTrue(signals.stream().anyMatch(s -> s.type() == FailureSignal.Type.LONG_TAIL_DEATH),
                "Long-tail death must be detected when majority of categories are near-zero");
    }

    @Test
    void failureDetectorReportsPoolCollapse() {
        FailureSignalDetector detector = new FailureSignalDetector(NO_OP_LOGGER);
        ProductionSafetySnapshot snap = new ProductionSafetySnapshot(
                System.currentTimeMillis(),
                Map.of("chaos", 1.0),
                Map.of(),
                1.5, 0.0, 10,
                List.of(), List.of());

        List<FailureSignal> signals = detector.detect(snap);
        assertTrue(signals.stream().anyMatch(s -> s.type() == FailureSignal.Type.POOL_COLLAPSE),
                "Pool collapse must be detected when average pool size < 2.0");
    }

    @Test
    void failureDetectorIsQuietUnderHealthyConditions() {
        FailureSignalDetector detector = new FailureSignalDetector(NO_OP_LOGGER);
        ProductionSafetySnapshot snap = new ProductionSafetySnapshot(
                System.currentTimeMillis(),
                Map.of("chaos", 0.20, "precision", 0.20, "survival", 0.20,
                        "mobility", 0.20, "consistency", 0.10, "brutality", 0.10),
                Map.of("on_kill", 0.30, "on_hit", 0.30, "on_move", 0.40),
                6.5, 1.5, 100,
                List.of(), List.of());

        List<FailureSignal> signals = detector.detect(snap);
        assertTrue(signals.isEmpty(), "No failure signals expected under healthy distribution");
    }

    // -------------------------------------------------------------------------
    // Snapshot comparison and JSON
    // -------------------------------------------------------------------------

    @Test
    void snapshotCompareToProducesReadableDelta() {
        ProductionSafetySnapshot before = new ProductionSafetySnapshot(
                1000L,
                Map.of("chaos", 0.40, "precision", 0.60),
                Map.of(), 5.0, 0.8, 50, List.of(), List.of());
        ProductionSafetySnapshot after = new ProductionSafetySnapshot(
                2000L,
                Map.of("chaos", 0.70, "precision", 0.30),
                Map.of(), 4.0, 0.6, 50, List.of(), List.of());

        String delta = after.compareTo(before);
        assertTrue(delta.contains("chaos"), "Delta must mention the changed category");
        assertTrue(delta.contains("+"), "Delta must show the positive change");
    }

    @Test
    void snapshotToJsonContainsRequiredFields() {
        ProductionSafetySnapshot snap = new ProductionSafetySnapshot(
                System.currentTimeMillis(),
                Map.of("chaos", 0.6, "precision", 0.4),
                Map.of("on_kill", 0.5, "on_hit", 0.5),
                4.0, 0.693, 20,
                List.of("CATEGORY_DOMINANT:chaos"),
                List.of());

        String json = snap.toJson();
        assertTrue(json.contains("\"timestampMs\""), "JSON must contain timestampMs");
        assertTrue(json.contains("\"diversityIndex\""), "JSON must contain diversityIndex");
        assertTrue(json.contains("\"categoryShares\""), "JSON must contain categoryShares");
        assertTrue(json.contains("\"templateShares\""), "JSON must contain templateShares");
        assertTrue(json.contains("\"activeGuards\""), "JSON must contain activeGuards");
        assertTrue(json.contains("chaos"), "JSON must contain chaos category");
    }

    // -------------------------------------------------------------------------
    // ProductionSafetyConfig defaults
    // -------------------------------------------------------------------------

    @Test
    void defaultConfigMatchesDocumentedSafeValues() {
        ProductionSafetyConfig d = ProductionSafetyConfig.defaults();
        assertEquals(0.65, d.categoryDominanceThreshold(), 1e-9);
        assertEquals(0.85, d.categorySuppressionFactor(), 1e-9);
        assertEquals(0.55, d.templateDominanceThreshold(), 1e-9);
        assertEquals(0.90, d.templateSuppressionFactor(), 1e-9);
        assertEquals(2, d.candidatePoolCollapseThreshold());
        assertEquals(100, d.rollingWindowSize());
        assertFalse(d.telemetryVerbose());
        assertFalse(d.periodicConsoleLog());
    }
}
