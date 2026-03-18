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
    public static final double SATURATION_THRESHOLD = 0.05D;

    /** specializationPressure must reach this value to count as "high specialization". */
    public static final double SPECIALIZATION_THRESHOLD = 0.00D;

    /** Minimum active-artifact count in a niche before bifurcation is considered. */
    public static final int MIN_ARTIFACT_COUNT = 2;

    /** Parent-niche share floor before bifurcation pressure can accumulate. */
    public static final double MIN_PARENT_NICHE_SHARE = 0.05D;

    /** Minimum active artifacts required for a child niche to remain viable. */
    public static final int MIN_CHILD_ARTIFACT_COUNT = 1;

    // ---------- defaults (overridable via constructor) ----------

    static final int  DEFAULT_MAX_DYNAMIC_NICHES  = 8;
    static final int  DEFAULT_MAX_DYNAMIC_NICHES_PER_PARENT = 4;
    static final long DEFAULT_COOLDOWN_MS          = 60_000L;  // 60 s
    static final int  DEFAULT_SUSTAINED_WINDOWS    = 2;        // two consecutive windows
    static final int  DEFAULT_CHILD_ZERO_WINDOWS_TO_COLLAPSE = 3;
    static final int  DEFAULT_GRACE_PERIOD_WINDOWS = 4;        // windows after birth before collapse is allowed
    static final int  ESTABLISHED_CHILD_WINDOWS = 4;
    static final int  COLLAPSE_HYSTERESIS_WINDOWS = 2;
    static final int  REENTRY_STABILIZATION_WINDOWS = 2;
    static final double STRONG_LINEAGE_AFFINITY_SUPPORT = 0.40D;
    static final double CONTINUITY_LINEAGE_SUPPORT_THRESHOLD = 0.20D;
    /** Number of recent windows averaged to produce a smoothed lineage-support signal. */
    static final int LINEAGE_SUPPORT_SMOOTHING_WINDOWS = 3;
    /** Relaxed lineage-support threshold applied to niches older than {@link #PERSISTENCE_WEIGHTED_AGE_WINDOWS}. */
    static final double PERSISTENCE_WEIGHTED_LINEAGE_THRESHOLD = 0.12D;
    /** Values below this are treated as effectively zero lineage affinity. */
    static final double LINEAGE_AFFINITY_EFFECTIVELY_ZERO = 0.005D;
    /** Window age at which persistence-weighted relaxation kicks in (2× establishment bar). */
    static final int PERSISTENCE_WEIGHTED_AGE_WINDOWS = ESTABLISHED_CHILD_WINDOWS * 2;

    // ---------- configuration ----------

    private final int  maxDynamicNiches;
    private final int  maxDynamicNichesPerParent;
    private final long cooldownMs;
    private final int  sustainedWindowsRequired;
    private final int  childZeroWindowsToCollapse;
    private final int  gracePeriodWindows;

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

    /** Tracks the evaluation window in which each child niche was born. */
    private final Map<String, Integer> birthWindowByChild = new ConcurrentHashMap<>();

    /** Tracks how many windows a child niche has remained active/non-zero. */
    private final Map<String, AtomicInteger> activeWindowsByChild = new ConcurrentHashMap<>();

    /** Tracks consecutive low-population windows to enforce collapse hysteresis. */
    private final Map<String, AtomicInteger> consecutiveLowPopulationWindowsByChild = new ConcurrentHashMap<>();
    /** Tracks short-lived stabilization after a zero-population child re-enters. */
    private final Map<String, AtomicInteger> reentryStabilizationWindowsByChild = new ConcurrentHashMap<>();

    /** Ecological identity profile for each active child niche. */
    private final Map<String, NicheVariantProfile> variantByChildNiche = new ConcurrentHashMap<>();

    /** Persistent occupancy continuity state for each child niche. */
    private final Map<String, ChildOccupancyState> occupancyStateByChild = new ConcurrentHashMap<>();

    /** Monotonically-increasing sequence counter used to produce unique child names. */
    private final AtomicInteger sequence = new AtomicInteger(0);

    // ---------- constructors ----------

    public NicheBifurcationRegistry() {
        this(DEFAULT_MAX_DYNAMIC_NICHES,
                DEFAULT_MAX_DYNAMIC_NICHES_PER_PARENT,
                DEFAULT_COOLDOWN_MS,
                DEFAULT_SUSTAINED_WINDOWS,
                DEFAULT_CHILD_ZERO_WINDOWS_TO_COLLAPSE,
                DEFAULT_GRACE_PERIOD_WINDOWS);
    }

    public NicheBifurcationRegistry(int maxDynamicNiches, long cooldownMs, int sustainedWindowsRequired) {
        this(maxDynamicNiches,
                DEFAULT_MAX_DYNAMIC_NICHES_PER_PARENT,
                cooldownMs,
                sustainedWindowsRequired,
                DEFAULT_CHILD_ZERO_WINDOWS_TO_COLLAPSE,
                DEFAULT_GRACE_PERIOD_WINDOWS);
    }

    public NicheBifurcationRegistry(int maxDynamicNiches,
                                    int maxDynamicNichesPerParent,
                                    long cooldownMs,
                                    int sustainedWindowsRequired,
                                    int childZeroWindowsToCollapse) {
        this(maxDynamicNiches,
                maxDynamicNichesPerParent,
                cooldownMs,
                sustainedWindowsRequired,
                childZeroWindowsToCollapse,
                DEFAULT_GRACE_PERIOD_WINDOWS);
    }

    public NicheBifurcationRegistry(int maxDynamicNiches,
                                    int maxDynamicNichesPerParent,
                                    long cooldownMs,
                                    int sustainedWindowsRequired,
                                    int childZeroWindowsToCollapse,
                                    int gracePeriodWindows) {
        this.maxDynamicNiches      = Math.max(0, maxDynamicNiches);
        this.maxDynamicNichesPerParent = Math.max(2, maxDynamicNichesPerParent);
        this.cooldownMs            = Math.max(0L, cooldownMs);
        this.sustainedWindowsRequired = Math.max(1, sustainedWindowsRequired);
        this.childZeroWindowsToCollapse = Math.max(1, childZeroWindowsToCollapse);
        this.gracePeriodWindows    = Math.max(0, gracePeriodWindows);
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
        boolean highSaturation     = saturationPressure    >= SATURATION_THRESHOLD;
        // High share alone (>= 0.13) can substitute for saturation pressure
        boolean highShare          = nichePopulationShare  >= 0.13D;
        boolean highSpecialization = specializationPressure >= SPECIALIZATION_THRESHOLD;
        boolean sufficientPop      = activeArtifacts >= MIN_ARTIFACT_COUNT;
        boolean sufficientShare    = nichePopulationShare >= MIN_PARENT_NICHE_SHARE;

        if ((!highSaturation && !highShare) || !highSpecialization || !sufficientPop || !sufficientShare) {
            // Pressure dropped — reset sustained-pressure counter
            highPressureWindowsByNiche.remove(parentNiche);
            return Optional.empty();
        }

        // Sustained-pressure gate
        int windows = highPressureWindowsByNiche
                .computeIfAbsent(parentNiche, k -> new AtomicInteger(0))
                .incrementAndGet();

        int effectiveSustainedWindows = nichePopulationShare >= 0.10D
                ? Math.max(1, sustainedWindowsRequired - 1)
                : sustainedWindowsRequired;

        if (windows < effectiveSustainedWindows) {
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
        activeWindowsByChild.put(childA, new AtomicInteger(0));
        activeWindowsByChild.put(childB, new AtomicInteger(0));
        consecutiveLowPopulationWindowsByChild.put(childA, new AtomicInteger(0));
        consecutiveLowPopulationWindowsByChild.put(childB, new AtomicInteger(0));
        reentryStabilizationWindowsByChild.put(childA, new AtomicInteger(0));
        reentryStabilizationWindowsByChild.put(childB, new AtomicInteger(0));
        occupancyStateByChild.put(childA, new ChildOccupancyState());
        occupancyStateByChild.put(childB, new ChildOccupancyState());
        lastBifurcationTimeByNiche.put(parentNiche, nowMs);
        lastGlobalBifurcationTimeMs = nowMs;
        occupancyStateByChild.put(childA, new ChildOccupancyState());
        occupancyStateByChild.put(childB, new ChildOccupancyState());
        storeVariants(parentNiche, childA, childB, seq);

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
     * Delegates to the window-aware overload with {@code Integer.MAX_VALUE} so
     * no child is considered to be in a grace period.
     */
    public void collapseUnderpopulatedChildren(Map<String, Long> dynamicPopulationByChild) {
        collapseUnderpopulatedChildren(dynamicPopulationByChild, Map.of(), Integer.MAX_VALUE);
    }

    /**
     * Collapses child niches that remain under-populated for sustained windows,
     * but skips the collapse counter for niches still within their grace period.
     *
     * @param dynamicPopulationByChild current per-child artifact counts
     * @param currentWindow            current evaluation-window index (from
     *                                 {@link obtuseloot.evolution.NichePopulationTracker})
     */
    public void collapseUnderpopulatedChildren(Map<String, Long> dynamicPopulationByChild, int currentWindow) {
        collapseUnderpopulatedChildren(dynamicPopulationByChild, Map.of(), currentWindow);
    }

    public void collapseUnderpopulatedChildren(Map<String, Long> dynamicPopulationByChild,
                                               Map<String, Double> lineageSupportByChild,
                                               int currentWindow) {
        for (String child : Set.copyOf(dynamicNiches)) {
            long population = dynamicPopulationByChild.getOrDefault(child, 0L);
            AtomicInteger zeroWindows = zeroPopulationWindowsByChild.computeIfAbsent(child, k -> new AtomicInteger(0));
            AtomicInteger activeWindows = activeWindowsByChild.computeIfAbsent(child, k -> new AtomicInteger(0));
            AtomicInteger lowWindows = consecutiveLowPopulationWindowsByChild.computeIfAbsent(child, k -> new AtomicInteger(0));
            AtomicInteger reentryWindows = reentryStabilizationWindowsByChild.computeIfAbsent(child, k -> new AtomicInteger(0));
            double lineageSupport = lineageSupportByChild.getOrDefault(child, 0.0D);
            if (population >= MIN_CHILD_ARTIFACT_COUNT) {
                if (zeroWindows.get() > 0) {
                    reentryWindows.set(REENTRY_STABILIZATION_WINDOWS);
                }
                activeWindows.incrementAndGet();
                zeroWindows.set(0);
                lowWindows.set(0);
                continue;
            }

            if (isInGracePeriod(child, currentWindow)) {
                zeroWindows.set(0);
                lowWindows.set(0);
                continue;
            }

            int activeWindowCount = activeWindows.get();
            boolean established = activeWindowCount >= ESTABLISHED_CHILD_WINDOWS;
            boolean strongLineageSupport = lineageSupport >= STRONG_LINEAGE_AFFINITY_SUPPORT;
            boolean stabilizedAfterReentry = reentryWindows.get() > 0;
            if (stabilizedAfterReentry) {
                reentryWindows.decrementAndGet();
                zeroWindows.set(0);
                lowWindows.set(0);
                continue;
            }
            int hysteresisTarget = established || strongLineageSupport
                    ? Math.max(COLLAPSE_HYSTERESIS_WINDOWS + 1, childZeroWindowsToCollapse)
                    : COLLAPSE_HYSTERESIS_WINDOWS;
            if (established && strongLineageSupport) {
                hysteresisTarget += 1;
            }
            int zeroTarget = childZeroWindowsToCollapse
                    + (strongLineageSupport ? 1 : 0)
                    + (established ? 1 : 0)
                    + (established && strongLineageSupport ? 1 : 0);

            int lowStreak = lowWindows.incrementAndGet();
            int zeroStreak = zeroWindows.incrementAndGet();
            if (lowStreak >= hysteresisTarget && zeroStreak >= zeroTarget) {
                retireChild(child, currentWindow);
            }
        }
    }

    // ---------- grace-period support ----------

    /**
     * Records the evaluation window in which a child niche was created so that
     * the grace period can be enforced.  Called by {@link NichePopulationTracker}
     * immediately after a bifurcation event fires.
     */
    public void setChildBirthWindow(String childNiche, int window) {
        birthWindowByChild.put(childNiche, window);
    }

    /**
     * Returns {@code true} if {@code childNiche} is still within its grace period
     * (i.e., was created fewer than {@link #gracePeriodWindows} windows ago).
     */
    public boolean isInGracePeriod(String childNiche, int currentWindow) {
        Integer birth = birthWindowByChild.get(childNiche);
        return birth != null && (currentWindow - birth) < gracePeriodWindows;
    }

    // ---------- forced bifurcation ----------

    /**
     * Forces a bifurcation on {@code parentNiche} bypassing pressure and
     * sustained-window gates, while still respecting the global niche cap,
     * per-parent cap, and cooldowns.  Used as a last-resort guarantee when no
     * organic bifurcation has occurred by window {@code N}.
     */
    public Optional<NicheBifurcation> forceBifurcation(
            String parentNiche,
            double saturationPressure,
            double specializationPressure,
            long nowMs) {

        // Guard: global niche cap
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
        // Guard: global cooldown
        if (lastGlobalBifurcationTimeMs != Long.MIN_VALUE && (nowMs - lastGlobalBifurcationTimeMs) < cooldownMs) {
            return Optional.empty();
        }

        // Pressure gates intentionally bypassed — this is the forced path
        highPressureWindowsByNiche.remove(parentNiche);

        int seq = sequence.incrementAndGet();
        String childA = parentNiche + "_A" + seq;
        String childB = parentNiche + "_B" + seq;

        NicheBifurcation event = new NicheBifurcation(
                parentNiche, childA, childB,
                saturationPressure, specializationPressure, nowMs);

        bifurcations.add(event);
        childrenByParent.computeIfAbsent(parentNiche, k -> Collections.synchronizedList(new ArrayList<>())).add(childA);
        childrenByParent.computeIfAbsent(parentNiche, k -> Collections.synchronizedList(new ArrayList<>())).add(childB);
        dynamicNiches.add(childA);
        dynamicNiches.add(childB);
        zeroPopulationWindowsByChild.put(childA, new AtomicInteger(0));
        zeroPopulationWindowsByChild.put(childB, new AtomicInteger(0));
        activeWindowsByChild.put(childA, new AtomicInteger(0));
        activeWindowsByChild.put(childB, new AtomicInteger(0));
        consecutiveLowPopulationWindowsByChild.put(childA, new AtomicInteger(0));
        consecutiveLowPopulationWindowsByChild.put(childB, new AtomicInteger(0));
        reentryStabilizationWindowsByChild.put(childA, new AtomicInteger(0));
        reentryStabilizationWindowsByChild.put(childB, new AtomicInteger(0));
        lastBifurcationTimeByNiche.put(parentNiche, nowMs);
        lastGlobalBifurcationTimeMs = nowMs;
        storeVariants(parentNiche, childA, childB, seq);

        return Optional.of(event);
    }

    // ---------- variant identity ----------

    /**
     * Returns the {@link NicheVariantProfile} for {@code childNiche}, or
     * {@code null} if no variant has been registered (e.g., for retired children
     * or the parent niche itself).
     */
    public NicheVariantProfile variantFor(String childNiche) {
        return variantByChildNiche.get(childNiche);
    }

    /** Generates and stores variant profiles for a freshly-created child pair. */
    private void storeVariants(String parentNiche, String childA, String childB, int seq) {
        NicheVariantProfile[] variants = NicheVariantProfile.generate(parentNiche, childA, childB, seq);
        variantByChildNiche.put(childA, variants[0]);
        variantByChildNiche.put(childB, variants[1]);
    }

    public boolean isEstablishedChild(String childNiche) {
        return activeWindowsByChild.getOrDefault(childNiche, new AtomicInteger(0)).get() >= ESTABLISHED_CHILD_WINDOWS;
    }

    public int activeWindowCount(String childNiche) {
        return activeWindowsByChild.getOrDefault(childNiche, new AtomicInteger(0)).get();
    }

    public boolean hasReentryStabilization(String childNiche) {
        return reentryStabilizationWindowsByChild.getOrDefault(childNiche, new AtomicInteger(0)).get() > 0;
    }

    public void recordChildOccupancy(String childNiche, long population, double lineageSupport, int currentWindow) {
        ChildOccupancyState state = occupancyStateByChild.computeIfAbsent(childNiche, ignored -> new ChildOccupancyState());

        // Advance the rolling window and capture carryover eligibility exactly once per evaluation window.
        // This ensures that eligiblePreviousWindow reflects the state at the END of the previous window,
        // even when recordChildOccupancy is called multiple times within a single window.
        if (currentWindow != state.lastEligibilityUpdateWindow) {
            state.eligiblePreviousWindow = computeEligibilityFromState(state, childNiche);
            state.lastEligibilityUpdateWindow = currentWindow;
            state.recentLineageSupportWindow[state.recentSupportWritePos % LINEAGE_SUPPORT_SMOOTHING_WINDOWS] = lineageSupport;
            state.recentSupportWritePos++;
            state.recentSupportFilled = Math.min(state.recentSupportFilled + 1, LINEAGE_SUPPORT_SMOOTHING_WINDOWS);
        }

        state.lineageSupport = lineageSupport;
        state.hasLineageSupport = state.smoothedLineageSupport() >= CONTINUITY_LINEAGE_SUPPORT_THRESHOLD;
        if (population > 0L) {
            state.lastOccupiedWindow = currentWindow;
            state.consecutiveOccupiedWindows += 1;
        } else {
            state.consecutiveOccupiedWindows = 0;
        }
        state.established = state.consecutiveOccupiedWindows >= ESTABLISHED_CHILD_WINDOWS
                || activeWindowCount(childNiche) >= ESTABLISHED_CHILD_WINDOWS;
    }

    /**
     * Computes raw eligibility from an occupancy state without the zero-window collapse guard.
     * Used only when snapshotting the previous window's eligibility for carryover tracking.
     */
    private boolean computeEligibilityFromState(ChildOccupancyState state, String childNiche) {
        if (!state.established) {
            return false;
        }
        double smoothed = state.smoothedLineageSupport();
        if (smoothed < LINEAGE_AFFINITY_EFFECTIVELY_ZERO) {
            return false;
        }
        int activeWindows = activeWindowCount(childNiche);
        double effectiveThreshold = activeWindows >= PERSISTENCE_WEIGHTED_AGE_WINDOWS
                ? PERSISTENCE_WEIGHTED_LINEAGE_THRESHOLD
                : CONTINUITY_LINEAGE_SUPPORT_THRESHOLD;
        return smoothed >= effectiveThreshold;
    }

    public int lastOccupiedWindow(String childNiche) {
        return occupancyStateByChild.getOrDefault(childNiche, ChildOccupancyState.EMPTY).lastOccupiedWindow;
    }

    public int consecutiveOccupiedWindows(String childNiche) {
        return occupancyStateByChild.getOrDefault(childNiche, ChildOccupancyState.EMPTY).consecutiveOccupiedWindows;
    }

    public boolean hasLineageSupport(String childNiche) {
        return occupancyStateByChild.getOrDefault(childNiche, ChildOccupancyState.EMPTY).hasLineageSupport;
    }

    public double lineageSupport(String childNiche) {
        return occupancyStateByChild.getOrDefault(childNiche, ChildOccupancyState.EMPTY).lineageSupport;
    }

    public boolean isEstablishedOrHistoricalChild(String childNiche) {
        ChildOccupancyState state = occupancyStateByChild.get(childNiche);
        return state != null && state.established;
    }

    public boolean eligibleForContinuityProtection(String childNiche) {
        ChildOccupancyState state = occupancyStateByChild.get(childNiche);
        if (state == null) {
            return false;
        }
        // New niches that have not yet established occupancy are never protected.
        if (!state.established) {
            return false;
        }
        // Don't extend protection past the collapse threshold — let it retire.
        AtomicInteger zeroWindows = zeroPopulationWindowsByChild.get(childNiche);
        if (zeroWindows != null && zeroWindows.get() >= childZeroWindowsToCollapse) {
            return false;
        }
        // Use the smoothed lineage-support signal to avoid flicker from single-window dips.
        double smoothed = state.smoothedLineageSupport();
        // Niches with effectively zero lineage affinity are not eligible.
        if (smoothed < LINEAGE_AFFINITY_EFFECTIVELY_ZERO) {
            return false;
        }
        // Persistence-weighted threshold: older established niches get a relaxed lineage requirement
        // so that temporary signal erosion does not drop them below the eligibility bar.
        int activeWindows = activeWindowCount(childNiche);
        double effectiveThreshold = activeWindows >= PERSISTENCE_WEIGHTED_AGE_WINDOWS
                ? PERSISTENCE_WEIGHTED_LINEAGE_THRESHOLD
                : CONTINUITY_LINEAGE_SUPPORT_THRESHOLD;
        // Primary path: smoothed support meets the (possibly relaxed) threshold.
        if (smoothed >= effectiveThreshold) {
            return true;
        }
        // Carryover path: niche was eligible last window and still has non-trivial affinity.
        // This prevents eligibility flicker across windows when support briefly dips.
        return state.eligiblePreviousWindow;
    }

    private void retireChild(String childNiche, int currentWindow) {
        String parent = parentByChild(childNiche);
        dynamicNiches.remove(childNiche);
        zeroPopulationWindowsByChild.remove(childNiche);
        activeWindowsByChild.remove(childNiche);
        consecutiveLowPopulationWindowsByChild.remove(childNiche);
        reentryStabilizationWindowsByChild.remove(childNiche);
        birthWindowByChild.remove(childNiche);
        variantByChildNiche.remove(childNiche);
        occupancyStateByChild.remove(childNiche);
        childrenByParent.values().forEach(children -> children.remove(childNiche));
        if (parent != null) {
            lastBifurcationTimeByNiche.put(parent, System.currentTimeMillis());
        }
    }

    private String parentByChild(String childNiche) {
        for (Map.Entry<String, List<String>> entry : childrenByParent.entrySet()) {
            if (entry.getValue().contains(childNiche)) {
                return entry.getKey();
            }
        }
        return null;
    }


    private static final class ChildOccupancyState {
        private static final ChildOccupancyState EMPTY = new ChildOccupancyState();

        private int lastOccupiedWindow = -1;
        private int consecutiveOccupiedWindows = 0;
        private boolean established = false;
        private boolean hasLineageSupport = false;
        private double lineageSupport = 0.0D;

        // Rolling window for smoothed lineage-support evaluation (avoids single-dip flicker)
        private final double[] recentLineageSupportWindow = new double[LINEAGE_SUPPORT_SMOOTHING_WINDOWS];
        private int recentSupportWritePos = 0;
        private int recentSupportFilled = 0;

        // Carryover eligibility: was this niche eligible for protection in the previous window?
        private boolean eligiblePreviousWindow = false;
        // Tracks which evaluation window the rolling buffer was last advanced in
        private int lastEligibilityUpdateWindow = -1;

        /** Returns the mean of the rolling lineage-support window, or the raw value if no history yet. */
        double smoothedLineageSupport() {
            if (recentSupportFilled == 0) {
                return lineageSupport;
            }
            double sum = 0.0D;
            for (int i = 0; i < recentSupportFilled; i++) {
                sum += recentLineageSupportWindow[i];
            }
            return sum / recentSupportFilled;
        }
    }
}
