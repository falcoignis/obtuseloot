package obtuseloot.evolution;

/**
 * Encodes the distinct ecological identity of a bifurcated child niche.
 *
 * <p>When a parent niche splits into two children (A and B), each child receives
 * a deterministically-generated variant profile that biases its behavior along
 * three dimensions:</p>
 * <ul>
 *   <li><b>Mutation vs. Retention emphasis</b> – A-variants are mutation-heavy
 *       (exploring new mechanics); B-variants are retention-heavy (consolidating
 *       proven mechanics).</li>
 *   <li><b>Reinforcement bias</b> – A-variants apply softer reinforcement pressure;
 *       B-variants apply stronger reinforcement, locking in successful patterns.</li>
 *   <li><b>Sub-niche affinity</b> – Each variant favors a complementary partition of
 *       the parent's sub-niches (even-hashed vs. odd-hashed), so A and B develop
 *       different mechanic preferences over time.</li>
 * </ul>
 *
 * <p>All values are deterministically generated from the parent niche name and
 * bifurcation sequence number so that identity is stable and reproducible across
 * runs without manual configuration.</p>
 */
public record NicheVariantProfile(
        String childNiche,
        String parentNiche,
        boolean isAlphaVariant,
        double mutationBias,
        double retentionBias,
        double reinforcementBias,
        int subNicheAffinityParity   // 0 = favors even-hashed sub-niches; 1 = favors odd-hashed
) {
    /** Adoption-fitness boost when an artifact's sub-niche matches this variant's affinity. */
    static final double FAVORED_SUBNICHE_BOOST = 1.12D;
    /** Slight adoption-fitness penalty when sub-niche does not match this variant's affinity. */
    static final double UNFAVORED_SUBNICHE_PENALTY = 0.93D;

    /**
     * Generates a pair of complementary variant profiles for a freshly-bifurcated
     * child pair.  The generation is deterministic: given the same parentNiche and
     * sequence number the result is always identical.
     *
     * @param parentNiche name of the bifurcating niche (e.g. "RITUAL_STRANGE_UTILITY")
     * @param childA      name of the A-child (e.g. "RITUAL_STRANGE_UTILITY_A1")
     * @param childB      name of the B-child
     * @param seq         monotonic bifurcation sequence number
     * @return a two-element array: [0] = A-variant, [1] = B-variant
     */
    public static NicheVariantProfile[] generate(String parentNiche, String childA, String childB, int seq) {
        // Deterministic hash seed derived from parent + sequence
        int h = parentNiche.hashCode() ^ (seq * 0x9E3779B9);
        int ha = Math.abs(h);

        // A-variant: mutation-heavy, even sub-niche affinity
        double mutA   = 1.10D + ((ha         % 8) * 0.01D);  // [1.10, 1.17]
        double retA   = 0.86D + (((ha >>> 4) % 6) * 0.01D);  // [0.86, 0.91]
        double reinfA = 0.90D + (((ha >>> 8) % 5) * 0.01D);  // [0.90, 0.94]
        NicheVariantProfile alpha = new NicheVariantProfile(
                childA, parentNiche, true, mutA, retA, reinfA, 0);

        // B-variant: retention-heavy, odd sub-niche affinity
        double mutB   = 0.84D + (((ha >>> 2) % 6) * 0.01D);  // [0.84, 0.89]
        double retB   = 1.13D + (((ha >>> 6) % 8) * 0.01D);  // [1.13, 1.20]
        double reinfB = 1.07D + (((ha >>> 10) % 5) * 0.01D); // [1.07, 1.11]
        NicheVariantProfile beta = new NicheVariantProfile(
                childB, parentNiche, false, mutB, retB, reinfB, 1);

        return new NicheVariantProfile[]{alpha, beta};
    }

    /**
     * Returns an adoption-fitness multiplier based on whether {@code subNiche}
     * matches this variant's sub-niche affinity parity.
     *
     * <p>A-variants reward artifacts whose dominant sub-niche has an even hash code;
     * B-variants reward those with an odd hash code.  This naturally splits the
     * sub-niche space without requiring a hardcoded mapping.</p>
     */
    public double adoptionBoostFor(String subNiche) {
        if (subNiche == null || subNiche.isBlank()) {
            return 1.0D;
        }
        int parity = Math.abs(subNiche.hashCode()) % 2;
        return parity == subNicheAffinityParity ? FAVORED_SUBNICHE_BOOST : UNFAVORED_SUBNICHE_PENALTY;
    }

    /** Human-readable one-liner for telemetry and analytics output. */
    public String summary() {
        return String.format(
                "type=%s mut=%.2f ret=%.2f reinf=%.2f affinity=%s",
                isAlphaVariant ? "ALPHA" : "BETA",
                mutationBias, retentionBias, reinforcementBias,
                subNicheAffinityParity == 0 ? "EVEN_HASH" : "ODD_HASH");
    }
}
