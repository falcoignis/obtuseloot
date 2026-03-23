package obtuseloot.ecosystem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Aggregates production-safety telemetry for the ecosystem engine.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Feed observation data into {@link ProductionSafetyGuards} rolling windows.</li>
 *   <li>Capture {@link ProductionSafetySnapshot}s at configured intervals.</li>
 *   <li>Run {@link FailureSignalDetector} against each snapshot.</li>
 *   <li>Maintain a bounded history of the last {@value #HISTORY_CAPACITY} snapshots.</li>
 *   <li>Provide formatted summaries for the {@code /ol ecosystem} and {@code /ol ecosystem dump}
 *       commands.</li>
 * </ul>
 *
 * <p>All operations are O(K) per evaluation where K = distinct categories (bounded constant).
 * No full-map scans occur on each tick; the rolling windows use amortised O(1) update.
 *
 * <p>Not thread-safe.  Must be accessed from the server main thread only (telemetry flush runs
 * async but only reads the already-immutable snapshots).
 */
public final class EcosystemHealthMonitor {

    private static final int HISTORY_CAPACITY = 20;

    private final ProductionSafetyGuards guards;
    private final FailureSignalDetector failureDetector;
    private final Logger logger;

    private final int snapshotIntervalEvents;
    private final boolean periodicConsoleLog;

    private int eventCount = 0;
    private ProductionSafetySnapshot lastSnapshot = ProductionSafetySnapshot.empty();
    // ArrayDeque gives O(1) addLast/pollFirst vs ArrayList.remove(0) which is O(n).
    private final Deque<ProductionSafetySnapshot> snapshotHistoryDeque = new ArrayDeque<>();

    public EcosystemHealthMonitor(
            ProductionSafetyGuards guards,
            Logger logger,
            int snapshotIntervalEvents,
            boolean periodicConsoleLog) {
        this.guards = guards;
        this.logger = logger;
        this.failureDetector = new FailureSignalDetector(logger);
        this.snapshotIntervalEvents = snapshotIntervalEvents;
        this.periodicConsoleLog = periodicConsoleLog;
    }

    // -------------------------------------------------------------------------
    // Observation recording
    // -------------------------------------------------------------------------

    /**
     * Record one ecosystem evaluation cycle.
     *
     * @param categoryShares normalised family share map (values 0–1)
     * @param templateShares normalised trigger/template share map
     * @param poolSize        observed candidate pool size, or {@code -1} to skip pool tracking
     */
    public void recordEvaluation(
            Map<String, Double> categoryShares,
            Map<String, Double> templateShares,
            int poolSize) {
        guards.recordCategoryDistribution(categoryShares);
        guards.recordTemplateDistribution(templateShares);
        if (poolSize >= 0) {
            guards.recordPoolSize(poolSize);
        }
        eventCount++;

        if (snapshotIntervalEvents > 0 && eventCount % snapshotIntervalEvents == 0) {
            captureAndStore();
        }
    }

    // -------------------------------------------------------------------------
    // Snapshot capture
    // -------------------------------------------------------------------------

    /**
     * Capture and return the current snapshot.  Also runs failure detection and may log to console.
     * The returned snapshot is also stored as {@link #lastSnapshot()}.
     */
    public ProductionSafetySnapshot captureSnapshot() {
        Map<String, Double> catShares = guards.categoryWindow().averageShares();
        Map<String, Double> tmplShares = guards.templateWindow().averageShares();

        List<String> activeGuards = new ArrayList<>();
        catShares.forEach((cat, share) -> {
            if (share > guards.config().categoryDominanceThreshold()) {
                activeGuards.add("CATEGORY_DOMINANT:" + cat);
            }
        });
        tmplShares.forEach((tmpl, share) -> {
            if (share > guards.config().templateDominanceThreshold()) {
                activeGuards.add("TEMPLATE_DOMINANT:" + tmpl);
            }
        });
        if (guards.isPoolCollapsed()) {
            activeGuards.add("POOL_COLLAPSED");
        }

        double diversity = shannonEntropy(catShares);

        // First pass: build snapshot without failure signals (detector needs the snapshot)
        ProductionSafetySnapshot snap = new ProductionSafetySnapshot(
                System.currentTimeMillis(),
                Collections.unmodifiableMap(new LinkedHashMap<>(catShares)),
                Collections.unmodifiableMap(new LinkedHashMap<>(tmplShares)),
                guards.averagePoolSize(),
                diversity,
                guards.categoryWindow().windowFill(),
                Collections.unmodifiableList(new ArrayList<>(activeGuards)),
                Collections.emptyList());

        List<FailureSignal> signals = failureDetector.detect(snap);
        if (!signals.isEmpty()) {
            List<String> signalLabels = new ArrayList<>();
            for (FailureSignal sig : signals) {
                signalLabels.add(sig.type().name() + ": " + sig.description());
            }
            snap = new ProductionSafetySnapshot(
                    snap.timestampMs(), snap.categoryShares(), snap.templateShares(),
                    snap.averageCandidatePoolSize(), snap.diversityIndex(), snap.windowFill(),
                    snap.activeGuards(), Collections.unmodifiableList(signalLabels));
        }

        lastSnapshot = snap;
        return snap;
    }

    private void captureAndStore() {
        ProductionSafetySnapshot snap = captureSnapshot();
        if (snapshotHistoryDeque.size() >= HISTORY_CAPACITY) {
            snapshotHistoryDeque.pollFirst();
        }
        snapshotHistoryDeque.addLast(snap);
        if (periodicConsoleLog) {
            logger.info(String.format(Locale.ROOT,
                    "[Ecosystem] Safety snapshot #%d: diversity=%.4f avgPool=%.2f guards=%s signals=%s",
                    eventCount, snap.diversityIndex(), snap.averageCandidatePoolSize(),
                    snap.activeGuards(), snap.activeFailureSignals()));
        }
    }

    // -------------------------------------------------------------------------
    // Command display helpers
    // -------------------------------------------------------------------------

    /**
     * Formats a human-readable in-game summary for {@code /ol ecosystem}.
     * Includes category shares, dominant templates, diversity index, pool size, and active guards.
     */
    public List<String> formatSummary() {
        ProductionSafetySnapshot snap = captureSnapshot();
        List<String> lines = new ArrayList<>();

        lines.add("§d=== Ecosystem Safety Metrics ===");
        lines.add(String.format(Locale.ROOT, "§7Diversity Index:  §f%.4f", snap.diversityIndex()));
        lines.add(String.format(Locale.ROOT, "§7Avg Pool Size:    §f%.2f", snap.averageCandidatePoolSize()));
        lines.add(String.format(Locale.ROOT, "§7Window Fill:      §f%d / %d",
                snap.windowFill(), guards.config().rollingWindowSize()));
        lines.add(String.format(Locale.ROOT, "§7Pool Collapses:   §f%d", guards.poolCollapseEventCount()));

        if (!snap.categoryShares().isEmpty()) {
            lines.add("§7Category Shares:");
            snap.categoryShares().entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .forEach(e -> {
                        double mult = guards.categoryGuardMultiplier(e.getKey());
                        String guardTag = mult < 1.0
                                ? String.format(Locale.ROOT, " §c[GUARD ×%.2f]", mult)
                                : "";
                        lines.add(String.format(Locale.ROOT,
                                "  §7%-14s §f%.1f%%%s", e.getKey() + ":", e.getValue() * 100, guardTag));
                    });
        }

        if (!snap.templateShares().isEmpty()) {
            lines.add("§7Dominant Templates (top 5):");
            snap.templateShares().entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(5)
                    .forEach(e -> {
                        double mult = guards.templateGuardMultiplier(e.getKey());
                        String guardTag = mult < 1.0
                                ? String.format(Locale.ROOT, " §c[GUARD ×%.2f]", mult)
                                : "";
                        lines.add(String.format(Locale.ROOT,
                                "  §7%-14s §f%.1f%%%s", e.getKey() + ":", e.getValue() * 100, guardTag));
                    });
        }

        if (!snap.activeGuards().isEmpty()) {
            lines.add("§cActive Guards:");
            snap.activeGuards().forEach(g -> lines.add("  §c" + g));
        }
        if (!snap.activeFailureSignals().isEmpty()) {
            lines.add("§4Failure Signals:");
            snap.activeFailureSignals().forEach(s -> lines.add("  §4" + s));
        }
        return lines;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Most recently captured snapshot (empty placeholder until first capture). */
    public ProductionSafetySnapshot lastSnapshot() {
        return lastSnapshot;
    }

    /** Snapshot history (up to last {@value #HISTORY_CAPACITY} captures), oldest-first. */
    public List<ProductionSafetySnapshot> snapshotHistory() {
        return Collections.unmodifiableList(new ArrayList<>(snapshotHistoryDeque));
    }

    /** Total evaluation cycles recorded since last reset. */
    public int eventCount() {
        return eventCount;
    }

    /** The underlying safety guards instance. */
    public ProductionSafetyGuards guards() {
        return guards;
    }

    /** Reset all rolling metrics, snapshot history, and event count. */
    public void resetMetrics() {
        guards.resetMetrics();
        eventCount = 0;
        snapshotHistoryDeque.clear();
        lastSnapshot = ProductionSafetySnapshot.empty();
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private static double shannonEntropy(Map<String, Double> shares) {
        double entropy = 0.0;
        for (double share : shares.values()) {
            if (share > 0.0) {
                entropy -= share * Math.log(share);
            }
        }
        return entropy;
    }
}
