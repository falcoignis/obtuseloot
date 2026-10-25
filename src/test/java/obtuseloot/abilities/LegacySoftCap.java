package obtuseloot.abilities;

/** Test-only copy of the mean-relative soft cap removed from production; ablation controls restore it. */
final class LegacySoftCap {
    static final double RATIO = 1.28D;
    private LegacySoftCap() { }

    /** The removed {@code applyMeanRelativeSoftTopCap}, unchanged. */
    static double[] apply(double[] scores, double meanScore) {
        if (scores.length <= 1 || meanScore <= 0.0D || !Double.isFinite(meanScore)) return scores;
        double capRange = Math.max(meanScore * (RATIO - 1.0D), 1.0E-9D);
        double[] capped = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            double score = Math.max(0.0001D, scores[i]);
            capped[i] = score <= meanScore ? score : meanScore + (capRange * Math.tanh((score - meanScore) / capRange));
        }
        return capped;
    }

    /** The removed sampling preparation: the same guards, then the cap at the original-score mean. */
    static double[] samplingScores(double[] scores) {
        if (scores.length <= 1) return scores;
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY, sum = 0.0D;
        for (double score : scores) { min = Math.min(min, score); max = Math.max(max, score); sum += score; }
        if (!Double.isFinite(min) || !Double.isFinite(max) || max <= min) return scores;
        return apply(scores, sum / scores.length);
    }
}
