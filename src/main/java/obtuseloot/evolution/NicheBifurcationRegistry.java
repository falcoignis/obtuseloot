package obtuseloot.evolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the lifecycle of dynamically bifurcated niches.
 *
 * <p>A bifurcation occurs when a niche sustains both high saturation pressure
 * and high specialization pressure across multiple consecutive evaluation
 * windows.  When triggered, the parent niche spawns two child niches (e.g.,
 * "NAVIGATION" → "NAVIGATION_A1" / "NAVIGATION_B1") which are then eligible
 * for artifact assignment and telemetry tracking.</p>
 *
 * <p>Safeguards:</p>
 * <ul>
 *   <li>Maximum total dynamic-niche count ({@link #DEFAULT_MAX_DYNAMIC_NICHES})</li>
 *   <li>Per-parent cooldown between successive bifurcations ({@link #DEFAULT_COOLDOWN_MS})</li>
 *   <li>Sustained pressure required across N consecutive windows
 *       ({@link #DEFAULT_SUSTAINED_WINDOWS})</li>
 *   <li>Minimum artifact population in the niche before bifurcation is allowed</li>
 * </ul>
 */
public class NicheBifurcationRegistry {

    // ---------- public thresholds (readable from tests) ----------

    /** saturationPenalty must reach this value to count as "high saturation". */
    public static final double SATURATION_THRESHOLD = 0.08D;

    /** specializationPressure must reach this value to count as "high specialization". */
    public static final double SPECIALIZATION_THRESHOLD = 0.00D;

    /** Minimum active-artifact count in a niche before bifurcation is considered. */
    public static final int MIN_ARTIFACT_COUNT = 2;

    /** Parent-niche share floor before bifurcation pressure can accumulate. */
    public static final double MIN_PARENT_NICHE_SHARE = 0.08D;

    /** Minimum active artifacts required for a child niche to remain viable. */
    public static final int MIN_CHILD_ARTIFACT_COUNT = 2;

    // ---------- defaults (overridable via constructor) ----------

    static final int  DEFAULT_MAX_DYNAMIC_NICHES  = 8;
    static final int  DEFAULT_MAX_DYNAMIC_NICHES_PER_PARENT = 4;
    static final long DEFAULT_COOLDOWN_MS          = 60_000L;  // 60 s
    static final int  DEFAULT_SUSTAINED_WINDOWS    = 2;        // two consecutive windows
    static final int  DEFAULT_CHILD_ZERO_WINDOWS_TO_COLLAPSE = 2;

    // ---------- configuration ----------

    private final int  maxDynamicNiches;
    private final int  maxDynamicNichesPerParent;
    private final long cooldownMs;
    private final int  sustainedWindowsRequired;
    private final int  childZeroWindowsToCollapse;

    // ---------- state ----------

    /** Ordered list of all bifurcations that have occurred. */
    private final List<NicheBifurcation> bifurcations = Collections.synchronizedList(new ArrayList<>());

    /** parent-niche-name → ordered list of child niche names. */
    private final Map<String, List<String>> childrenByParent = new ConcurrentHashMap<>();

    /** All currently-active dynamic niche names. */
    private final Set<String> dynamicNiches = ConcurrentHashMap.newKeySet();

    /** Tracks how many consecutive high-pressure windows each niche has had. */
    private final Map<String, AtomicInteger> highPressureWindowsByNiche = new ConcurrentHashMap<>();

    /** Last bifurcation timestamp per parent niche (for cooldown enforcement). */
    private final Map<String, Long> lastBifurcationTimeByNiche = new ConcurrentHashMap<>();
    private volatile long lastGlobalBifurcationTimeMs = Long.MIN_VALUE;
    private final Map<String, AtomicInteger> zeroPopulationWindowsByChild = new ConcurrentHashMap<>();

    /** Monotonically-increasing sequence counter used to produce unique child names. */
    private final AtomicInteger sequence = new AtomicInteger(0);

    // ---------- constructors ----------

    public NicheBifurcationRegistry() {
        this(DEFAULT_MAX_DYNAMIC_NICHES,
                DEFAULT_MAX_DYNAMIC_NICHES_PER_PARENT,
                DEFAULT_COOLDOWN_MS,
                DEFAULT_SUSTAINED_WINDOWS,
                DEFAULT_CHILD_ZERO_WINDOWS_TO_COLLAPSE);
    }

    public NicheBifurcationRegistry(int maxDynamicNiches, long cooldownMs, int sustainedWindowsRequired) {
        this(maxDynamicNiches,
                DEFAULT_MAX_DYNAMIC_NICHES_PER_PARENT,
                cooldownMs,
                sustainedWindowsRequired,
                DEFAULT_CHILD_ZERO_WINDOWS_TO_COLLAPSE);
    }

    public NicheBifurcationRegistry(int maxDynamicNiches,
                                    int maxDynamicNichesPerParent,
                                    long cooldownMs,
                                    int sustainedWindowsRequired,
                                    int childZeroWindowsToCollapse) {
        this.maxDynamicNiches      = Math.max(0, maxDynamicNiches);
        this.maxDynamicNichesPerParent = Math.max(2, maxDynamicNichesPerParent);
        this.cooldownMs            = Math.max(0L, cooldownMs);
        this.sustainedWindowsRequired = Math.max(1, sustainedWindowsRequired);
        this.childZeroWindowsToCollapse = Math.max(1, childZeroWindowsToCollapse);
    }

    // ---------- main API ----------

    /**
     * Evaluate whether {@code parentNiche} should bifurcate now.
     *
     * @param parentNiche           canonical niche name (e.g., "NAVIGATION")
     * @param saturationPressure    current saturationPenalty value from
     *                              {@link EcosystemSaturationModel}
     * @param specializationPressure current specializationPressure value
     * @param activeArtifacts       current artifact count in this niche
     * @param nowMs                 current wall-clock time in milliseconds
     * @return an {@link Optional} containing the bifurcation record if one was
     *         triggered, or {@link Optional#empty()} if conditions were not met
     */
    public Optional<NicheBifurcation> evaluateBifurcation(
            String parentNiche,
            double saturationPressure,
            double specializationPressure,
            double nichePopulationShare,
            int activeArtifacts,
            long nowMs) {

        // Guard: global niche cap (counts pairs, so headroom must be >= 2)
        if (dynamicNiches.size() + 2 > maxDynamicNiches) {
            return Optional.empty();
        }

        // Guard: per-parent cap
        if (childrenOf(parentNiche).size() + 2 > maxDynamicNichesPerParent) {
            return Optional.empty();
        }

        // Guard: per-parent cooldown
        Long lastTime = lastBifurcationTimeByNiche.get(parentNiche);
        if (lastTime != null && (nowMs - lastTime) < cooldownMs) {
            return Optional.empty();
        }

        // Guard: global cooldown to prevent rapid cross-niche cascades
        if (lastGlobalBifurcationTimeMs != Long.MIN_VALUE && (nowMs - lastGlobalBifurcationTimeMs) < cooldownMs) {
            return Optional.empty();
        }

        // Pressure gate
        boolean highSaturation    = saturationPressure    >= SATURATION_THRESHOLD;
        boolean highSpecialization = specializationPressure >= SPECIALIZATION_THRESHOLD;
        boolean sufficientPop      = activeArtifacts >= MIN_ARTIFACT_COUNT;
        boolean sufficientShare    = nichePopulationShare >= MIN_PARENT_NICHE_SHARE;

        if (!highSaturation || !highSpecialization || !sufficientPop || !sufficientShare) {
            // Pressure dropped — reset sustained-pressure counter
            highPressureWindowsByNiche.remove(parentNiche);
            return Optional.empty();
        }

        // Sustained-pressure gate
        int windows = highPressureWindowsByNiche
                .computeIfAbsent(parentNiche, k -> new AtomicInteger(0))
                .incrementAndGet();

        if (windows < sustainedWindowsRequired) {
            return Optional.empty();
        }

        // All conditions satisfied — bifurcate!
        highPressureWindowsByNiche.remove(parentNiche);   // reset for next cycle

        int seq = sequence.incrementAndGet();
        String childA = parentNiche + "_A" + seq;
        String childB = parentNiche + "_B" + seq;

        NicheBifurcation event = new NicheBifurcation(
                parentNiche,
                childA,
                childB,
                saturationPressure,
                specializationPressure,
                nowMs);

        bifurcations.add(event);
        childrenByParent.computeIfAbsent(parentNiche, k -> Collections.synchronizedList(new ArrayList<>())).add(childA);
        childrenByParent.computeIfAbsent(parentNiche, k -> Collections.synchronizedList(new ArrayList<>())).add(childB);
        dynamicNiches.add(childA);
        dynamicNiches.add(childB);
        zeroPopulationWindowsByChild.put(childA, new AtomicInteger(0));
        zeroPopulationWindowsByChild.put(childB, new AtomicInteger(0));
        lastBifurcationTimeByNiche.put(parentNiche, nowMs);
        lastGlobalBifurcationTimeMs = nowMs;

        return Optional.of(event);
    }

    // ---------- queries ----------

    /** All bifurcations that have occurred (in chronological order). */
    public List<NicheBifurcation> bifurcations() {
        return Collections.unmodifiableList(bifurcations);
    }

    /** The full set of currently-active dynamic niche names. */
    public Set<String> dynamicNiches() {
        return Collections.unmodifiableSet(dynamicNiches);
    }

    /** Returns {@code true} if the supplied name is a dynamically-created niche. */
    public boolean isDynamicNiche(String niche) {
        return dynamicNiches.contains(niche);
    }

    /**
     * Returns the child niches that have been spawned from the given parent,
     * in the order they were created.
     */
    public List<String> childrenOf(String parentNiche) {
        List<String> children = childrenByParent.get(parentNiche);
        return children == null ? List.of() : Collections.unmodifiableList(children);
    }

    /**
     * Assigns an artifact to one of the two most-recently-created child niches
     * of its parent, using the artifact's dominant sub-niche as a tie-breaker.
     * Returns the child niche name, or {@code null} if the parent has no children.
     */
    public String assignChildNiche(String parentNiche, String dominantSubniche) {
        List<String> children = childrenByParent.get(parentNiche);
        if (children == null || children.isEmpty()) {
            return null;
        }
        // Use the last bifurcation pair (last two children)
        int last = children.size() - 1;
        String childB = children.get(last);
        String childA = last >= 1 ? children.get(last - 1) : childB;
        // Deterministic split based on subniche name hash
        int hash = dominantSubniche == null ? 0 : dominantSubniche.hashCode();
        return (Math.abs(hash) % 2 == 0) ? childA : childB;
    }

    /** Total number of currently-registered dynamic niche names. */
    public int dynamicNicheCount() {
        return dynamicNiches.size();
    }

    /**
     * Collapses child niches that remain under-populated for sustained windows.
     */
    public void collapseUnderpopulatedChildren(Map<String, Long> dynamicPopulationByChild) {
        for (String child : Set.copyOf(dynamicNiches)) {
            long population = dynamicPopulationByChild.getOrDefault(child, 0L);
            AtomicInteger zeroWindows = zeroPopulationWindowsByChild.computeIfAbsent(child, k -> new AtomicInteger(0));
            if (population < MIN_CHILD_ARTIFACT_COUNT) {
                if (zeroWindows.incrementAndGet() >= childZeroWindowsToCollapse) {
                    retireChild(child);
                }
            } else {
                zeroWindows.set(0);
            }
        }
    }

    private void retireChild(String childNiche) {
        dynamicNiches.remove(childNiche);
        zeroPopulationWindowsByChild.remove(childNiche);
        childrenByParent.values().forEach(children -> children.remove(childNiche));
    }
}
