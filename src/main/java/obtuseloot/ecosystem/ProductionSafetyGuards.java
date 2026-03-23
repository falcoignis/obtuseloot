package obtuseloot.ecosystem;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Applies bounded, deterministic, and reversible safety multipliers to category and template weights.
 *
 * <p>Guards operate by observing rolling distribution windows and returning a suppression multiplier
 * when a threshold is exceeded.  No global state is permanently mutated; disabling a guard is
 * achieved simply by allowing the distribution to normalise inside the window.
 *
 * <h3>Guarantees</h3>
 * <ul>
 *   <li><b>Deterministic</b> — output depends only on the recorded observation history.</li>
 *   <li><b>Reversible</b> — multipliers return to 1.0 once observations normalise.</li>
 *   <li><b>Bounded</b> — suppression factors are constants; no cascading amplification.</li>
 * </ul>
 *
 * <h3>Guards</h3>
 * <ol>
 *   <li>Category dominance — if the rolling-average share of a category exceeds
 *       {@link ProductionSafetyConfig#categoryDominanceThreshold()} the returned multiplier
 *       is {@link ProductionSafetyConfig#categorySuppressionFactor()} (e.g. ×0.85).</li>
 *   <li>Template dominance — same logic for individual templates, using their respective thresholds.</li>
 *   <li>Candidate pool collapse — when the reported pool size falls below
 *       {@link ProductionSafetyConfig#candidatePoolCollapseThreshold()}, the event is logged and
 *       {@link #isPoolCollapsed()} returns {@code true} so callers can relax eligibility.</li>
 * </ol>
 */
public final class ProductionSafetyGuards {

    private final ProductionSafetyConfig config;
    private final RollingDistributionWindow categoryWindow;
    private final RollingDistributionWindow templateWindow;
    private final Logger logger;

    private double poolSizeSum = 0.0;
    private int poolSizeCount = 0;
    private int lastRecordedPoolSize = Integer.MAX_VALUE;
    private final AtomicLong poolCollapseEventCount = new AtomicLong(0);

    public ProductionSafetyGuards(ProductionSafetyConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
        this.categoryWindow = new RollingDistributionWindow(config.rollingWindowSize());
        this.templateWindow = new RollingDistributionWindow(config.rollingWindowSize());
    }

    // -------------------------------------------------------------------------
    // Observation recording
    // -------------------------------------------------------------------------

    /**
     * Record the current category distribution snapshot (fractional shares, not counts).
     * Logs a warning if any category exceeds the dominance threshold and verbose logging is enabled.
     */
    public void recordCategoryDistribution(Map<String, Double> categoryShares) {
        categoryWindow.record(categoryShares);
        if (config.telemetryVerbose()) {
            categoryShares.forEach((cat, share) -> {
                double avgShare = categoryWindow.averageShare(cat);
                if (avgShare > config.categoryDominanceThreshold()) {
                    logger.warning(String.format(Locale.ROOT,
                            "[SafetyGuard] Category dominance: '%s' rolling-avg=%.3f > threshold=%.3f → applying ×%.2f suppression",
                            cat, avgShare, config.categoryDominanceThreshold(), config.categorySuppressionFactor()));
                }
            });
        }
    }

    /**
     * Record the current template distribution snapshot (fractional shares).
     * Logs a warning if any template exceeds the dominance threshold and verbose logging is enabled.
     */
    public void recordTemplateDistribution(Map<String, Double> templateShares) {
        templateWindow.record(templateShares);
        if (config.telemetryVerbose()) {
            templateShares.forEach((tmpl, share) -> {
                double avgShare = templateWindow.averageShare(tmpl);
                if (avgShare > config.templateDominanceThreshold()) {
                    logger.warning(String.format(Locale.ROOT,
                            "[SafetyGuard] Template dominance: '%s' rolling-avg=%.3f > threshold=%.3f → applying ×%.2f suppression",
                            tmpl, avgShare, config.templateDominanceThreshold(), config.templateSuppressionFactor()));
                }
            });
        }
    }

    /**
     * Record an observed candidate pool size.
     * Logs a warning and increments the collapse counter when below threshold.
     */
    public void recordPoolSize(int size) {
        lastRecordedPoolSize = size;
        poolSizeSum += size;
        poolSizeCount++;
        if (size < config.candidatePoolCollapseThreshold()) {
            long count = poolCollapseEventCount.incrementAndGet();
            logger.warning(String.format(Locale.ROOT,
                    "[SafetyGuard] Pool collapse: candidate pool size=%d < threshold=%d (total occurrences=%d). Eligibility relaxation active.",
                    size, config.candidatePoolCollapseThreshold(), count));
        }
    }

    // -------------------------------------------------------------------------
    // Guard multipliers (deterministic, reversible, bounded)
    // -------------------------------------------------------------------------

    /**
     * Returns the guard multiplier for a category weight.
     *
     * @return {@link ProductionSafetyConfig#categorySuppressionFactor()} if the rolling-average share
     *         exceeds the dominance threshold, otherwise 1.0.
     */
    public double categoryGuardMultiplier(String category) {
        double avgShare = categoryWindow.averageShare(category.toLowerCase(Locale.ROOT));
        if (avgShare > config.categoryDominanceThreshold()) {
            return config.categorySuppressionFactor();
        }
        return 1.0;
    }

    /**
     * Returns the guard multiplier for a template weight.
     *
     * @return {@link ProductionSafetyConfig#templateSuppressionFactor()} if the rolling-average share
     *         exceeds the dominance threshold, otherwise 1.0.
     */
    public double templateGuardMultiplier(String template) {
        double avgShare = templateWindow.averageShare(template.toLowerCase(Locale.ROOT));
        if (avgShare > config.templateDominanceThreshold()) {
            return config.templateSuppressionFactor();
        }
        return 1.0;
    }

    // -------------------------------------------------------------------------
    // Pool state
    // -------------------------------------------------------------------------

    /** Returns true if the last recorded pool size was below the collapse threshold. */
    public boolean isPoolCollapsed() {
        return lastRecordedPoolSize < config.candidatePoolCollapseThreshold();
    }

    /** Running average of all recorded candidate pool sizes. Returns 0 if none recorded. */
    public double averagePoolSize() {
        return poolSizeCount == 0 ? 0.0 : poolSizeSum / poolSizeCount;
    }

    /** Cumulative count of pool collapse events since last reset. */
    public long poolCollapseEventCount() {
        return poolCollapseEventCount.get();
    }

    // -------------------------------------------------------------------------
    // Accessors and lifecycle
    // -------------------------------------------------------------------------

    public RollingDistributionWindow categoryWindow() {
        return categoryWindow;
    }

    public RollingDistributionWindow templateWindow() {
        return templateWindow;
    }

    public ProductionSafetyConfig config() {
        return config;
    }

    /** Reset all rolling windows and pool counters. */
    public void resetMetrics() {
        categoryWindow.reset();
        templateWindow.reset();
        poolSizeSum = 0.0;
        poolSizeCount = 0;
        lastRecordedPoolSize = Integer.MAX_VALUE;
        poolCollapseEventCount.set(0);
    }
}
