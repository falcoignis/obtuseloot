package obtuseloot.ecosystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Detects predefined failure conditions in the ecosystem from a {@link ProductionSafetySnapshot}.
 *
 * <p>Detection is purely observational — no corrections are applied.  Each detected condition is
 * logged as a structured warning and returned as a {@link FailureSignal} list for command display
 * and further analysis.
 *
 * <h3>Conditions detected</h3>
 * <ul>
 *   <li><b>Category collapse</b> — any category ≥ {@value #COLLAPSE_THRESHOLD_PCT}%</li>
 *   <li><b>Template dominance</b> — any template ≥ {@value #TEMPLATE_SEVERE_THRESHOLD_PCT}%</li>
 *   <li><b>Long-tail death</b> — ≥ 50% of categories below {@value #LONG_TAIL_FLOOR_PCT}% each</li>
 *   <li><b>Pool collapse</b> — average candidate pool size below 2.0</li>
 * </ul>
 */
public final class FailureSignalDetector {

    private static final double COLLAPSE_THRESHOLD = 0.90;
    private static final double TEMPLATE_SEVERE_THRESHOLD = 0.70;
    private static final double LONG_TAIL_FLOOR = 0.02;
    private static final int LONG_TAIL_MIN_CATEGORIES = 3;

    // For javadoc constants
    private static final int COLLAPSE_THRESHOLD_PCT = 90;
    private static final int TEMPLATE_SEVERE_THRESHOLD_PCT = 70;
    private static final int LONG_TAIL_FLOOR_PCT = 2;

    private final Logger logger;

    public FailureSignalDetector(Logger logger) {
        this.logger = logger;
    }

    /**
     * Run all detectors against the provided snapshot and return the list of active signals.
     * Each detected signal is also logged as a structured warning.
     */
    public List<FailureSignal> detect(ProductionSafetySnapshot snapshot) {
        List<FailureSignal> signals = new ArrayList<>();

        detectCategoryCollapse(snapshot, signals);
        detectTemplateDominance(snapshot, signals);
        detectLongTailDeath(snapshot, signals);
        detectPoolCollapse(snapshot, signals);

        for (FailureSignal signal : signals) {
            logger.warning(String.format(Locale.ROOT,
                    "[FailureDetector] %s | %s | diversityIndex=%.4f windowFill=%d",
                    signal.type().name(), signal.description(),
                    snapshot.diversityIndex(), snapshot.windowFill()));
        }

        return signals;
    }

    private void detectCategoryCollapse(ProductionSafetySnapshot snap, List<FailureSignal> out) {
        snap.categoryShares().forEach((cat, share) -> {
            if (share >= COLLAPSE_THRESHOLD) {
                out.add(new FailureSignal(FailureSignal.Type.CATEGORY_COLLAPSE,
                        String.format(Locale.ROOT,
                                "category '%s' at %.1f%% (severe threshold %.0f%%)",
                                cat, share * 100, COLLAPSE_THRESHOLD * 100)));
            }
        });
    }

    private void detectTemplateDominance(ProductionSafetySnapshot snap, List<FailureSignal> out) {
        snap.templateShares().forEach((tmpl, share) -> {
            if (share >= TEMPLATE_SEVERE_THRESHOLD) {
                out.add(new FailureSignal(FailureSignal.Type.TEMPLATE_DOMINANCE,
                        String.format(Locale.ROOT,
                                "template '%s' at %.1f%% (severe threshold %.0f%%)",
                                tmpl, share * 100, TEMPLATE_SEVERE_THRESHOLD * 100)));
            }
        });
    }

    private void detectLongTailDeath(ProductionSafetySnapshot snap, List<FailureSignal> out) {
        int total = snap.categoryShares().size();
        if (total < LONG_TAIL_MIN_CATEGORIES) return;
        long deadCount = snap.categoryShares().values().stream()
                .filter(share -> share < LONG_TAIL_FLOOR)
                .count();
        if (deadCount >= 2 && deadCount * 2 >= total) {
            out.add(new FailureSignal(FailureSignal.Type.LONG_TAIL_DEATH,
                    String.format(Locale.ROOT,
                            "%d of %d categories below %.0f%% share",
                            deadCount, total, LONG_TAIL_FLOOR * 100)));
        }
    }

    private void detectPoolCollapse(ProductionSafetySnapshot snap, List<FailureSignal> out) {
        double pool = snap.averageCandidatePoolSize();
        if (pool > 0.0 && pool < 2.0) {
            out.add(new FailureSignal(FailureSignal.Type.POOL_COLLAPSE,
                    String.format(Locale.ROOT, "average pool size %.2f (minimum 2)", pool)));
        }
    }
}
