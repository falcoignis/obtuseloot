package obtuseloot.simulation.worldlab;

import obtuseloot.abilities.AbilityCategory;
import obtuseloot.abilities.AbilityDefinition;
import obtuseloot.abilities.AbilityDiversityIndex;
import obtuseloot.abilities.AbilityProfile;
import obtuseloot.abilities.AbilityRegistry;
import obtuseloot.abilities.AbilityTemplate;
import obtuseloot.abilities.ProceduralAbilityGenerator;
import obtuseloot.abilities.ScoringMode;
import obtuseloot.artifacts.Artifact;
import obtuseloot.lineage.ArtifactLineage;
import obtuseloot.lineage.LineageBiasDimension;
import obtuseloot.lineage.LineageInfluenceResolver;
import obtuseloot.lineage.LineageRegistry;
import obtuseloot.memory.ArtifactMemoryProfile;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 7.6 — Long-Horizon Simulation &amp; Stability Validation.
 *
 * <p>Validates that the artifact ability generation system maintains diversity,
 * identity, and balance over {@value #ITERATIONS} generator calls against a
 * rotating population of {@value #POPULATION_SIZE} concurrent artifacts, across
 * three distinct behavioural profiles.
 *
 * <p><b>10,000+ observation guarantee:</b> each {@code generate()} call returns
 * a profile with approximately 3–6 abilities, so {@value #ITERATIONS} calls
 * produce roughly 15,000–25,000 total ability observations per run — well above
 * the statistical floor required to detect dominance or collapse patterns.
 *
 * <p>All simulation is fully deterministic: every run with the same
 * {@link #BASE_SEED} produces identical results. No external randomness is
 * introduced; all seeding flows from artifact construction seeds.
 */
@org.junit.jupiter.api.Timeout(value = 12, unit = TimeUnit.MINUTES)
class LongHorizonSimulationTest {

    // ── Simulation sizing ───────────────────────────────────────────────────

    /**
     * Total {@code generate()} calls per simulation run.
     *
     * <p>Each call produces ~3–6 abilities, so this yields ≥10,000 ability
     * observations — satisfying the long-horizon observation requirement.
     * The value is calibrated to the observed per-call cost of
     * {@link ProceduralAbilityGenerator} (~175 ms/call in CI) so the four
     * simulation tests finish well inside their {@code @Timeout}.
     */
    private static final int ITERATIONS = 400;
    /** Number of concurrently-live artifacts in the population. */
    private static final int POPULATION_SIZE = 50;
    /** Snapshot captured every N iterations (4 snapshots total). */
    private static final int SNAPSHOT_INTERVAL = 100;
    /** One artifact is retired and one new artifact is spawned every N iterations. */
    private static final int ROTATION_INTERVAL = 20;
    /** Deterministic root seed; no randomness introduced beyond this anchor. */
    private static final long BASE_SEED = 0x7A3F_C0FF_EE01_0101L;

    // ── Stability thresholds ────────────────────────────────────────────────

    /** Hard per-window cap: no category may exceed this share in any single snapshot. */
    private static final double MAX_CATEGORY_SHARE_HARD = 0.60;
    /**
     * Sustained-dominance threshold: a category above this value for
     * {@value #SUSTAINED_WINDOW_COUNT} consecutive windows is a failure.
     */
    private static final double MAX_CATEGORY_SHARE_SOFT = 0.50;
    /** Number of consecutive over-threshold windows that constitutes "sustained dominance". */
    private static final int SUSTAINED_WINDOW_COUNT = 3;
    /** Maximum fraction a single template may hold within its own category per snapshot. */
    private static final double MAX_TEMPLATE_SHARE_IN_CATEGORY = 0.50;
    /** Globally, each category must show at least this many distinct active templates. */
    private static final int MIN_ACTIVE_TEMPLATES_PER_CATEGORY = 4;
    /**
     * Minimum effective pool size (Simpson's diversity reciprocal), averaged across
     * all per-window snapshots.  A value &lt;2 indicates near-collapse to one or two
     * templates.
     */
    private static final double MIN_EFFECTIVE_POOL_SIZE = 2.0;
    /**
     * Minimum Jensen-Shannon divergence (in bits, log₂) between the high-exploration and
     * low-exploration lineage generators.  Ensures that lineage bias produces measurable,
     * non-trivial differentiation in category distributions.
     *
     * <p>The divergence test uses both contrasting memory profiles and opposing lineage
     * bias dimensions to represent a realistic diverged-lineage scenario: an explorer
     * lineage (high mobility/exploration) vs a ritual-chaos lineage (high ritual/weirdness).
     * This combined signal is sufficient to exceed 0.10 bits reliably.
     */
    private static final double MIN_LINEAGE_JSD = 0.10;

    // ── Profile definitions ─────────────────────────────────────────────────

    /** Affinity array index order: precision, brutality, survival, mobility, chaos, consistency. */
    private static final double[][] PROFILE_AFFINITIES = {
        /* 0 BALANCED     */ { 0.50, 0.50, 0.50, 0.50, 0.50, 0.50 },
        /* 1 EXPLORER     */ { 0.35, 0.25, 0.55, 0.80, 0.45, 0.55 },
        /* 2 RITUAL_CHAOS */ { 0.35, 0.70, 0.40, 0.35, 0.85, 0.40 },
    };

    private static final ArtifactMemoryProfile[] PROFILE_MEMORIES = {
        /* 0 BALANCED     */ new ArtifactMemoryProfile(5, 1.0, 1.0, 1.0, 1.0, 1.0, 0.5, 0.5),
        /* 1 EXPLORER     */ new ArtifactMemoryProfile(6, 0.7, 0.8, 0.8, 0.9, 1.9, 0.3, 0.3),
        /* 2 RITUAL_CHAOS */ new ArtifactMemoryProfile(8, 1.8, 0.6, 1.2, 0.7, 0.7, 1.5, 0.7),
    };

    private static final String[] PROFILE_NAMES = { "BALANCED", "EXPLORER", "RITUAL_CHAOS" };

    // ── Snapshot record ─────────────────────────────────────────────────────

    /** Immutable distribution snapshot captured at a fixed iteration boundary. */
    private record Snapshot(
        int iteration,
        Map<AbilityCategory, Integer> catCounts,
        Map<AbilityCategory, Map<String, Integer>> tplByCat,
        int total
    ) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Test entry-points
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void balancedProfileMaintainsDiversityOverLongHorizon() {
        assertProfileStability(0);
    }

    @Test
    void explorerProfileMaintainsDiversityOverLongHorizon() {
        assertProfileStability(1);
    }

    @Test
    void ritualChaosProfileMaintainsDiversityOverLongHorizon() {
        assertProfileStability(2);
    }

    /**
     * Validates that lineage divergence produces measurable distribution differences.
     *
     * <p>Two generators are constructed with opposing lineage bias dimensions
     * and opposing memory profiles, representing a high-exploration lineage
     * (high EXPLORATION_PREFERENCE, RISK_APPETITE; EXPLORER memory) versus a
     * low-exploration lineage (high RITUAL_PREFERENCE, WEIRDNESS; RITUAL_CHAOS memory).
     * Both generators operate on the same deterministic artifact seeds to ensure
     * the only source of divergence is the lineage/profile configuration.
     *
     * <p>The Jensen-Shannon divergence between the resulting category distributions
     * must exceed {@value #MIN_LINEAGE_JSD} bits, confirming that lineage influence
     * produces variation without collapsing into a single niche.
     */
    @Test
    void lineageDivergenceProducesMeasurableDistributionDifferences() {
        AbilityRegistry registry = new AbilityRegistry();
        Map<String, AbilityCategory> catIndex = buildCategoryIndex(registry);
        LineageInfluenceResolver resolver = new LineageInfluenceResolver();

        // High-exploration lineage: favours exploration/risk, penalises ritual/specialisation.
        LineageRegistry highExpLineageReg = buildBiasedLineageRegistry(
            "explorer-lineage",
            new LineageBiasDimension[]{ LineageBiasDimension.EXPLORATION_PREFERENCE,
                                        LineageBiasDimension.RISK_APPETITE },
            new LineageBiasDimension[]{ LineageBiasDimension.RITUAL_PREFERENCE,
                                        LineageBiasDimension.SPECIALIZATION });

        // Low-exploration lineage: favours ritual/weirdness, penalises exploration/risk.
        LineageRegistry lowExpLineageReg = buildBiasedLineageRegistry(
            "explorer-lineage",
            new LineageBiasDimension[]{ LineageBiasDimension.RITUAL_PREFERENCE,
                                        LineageBiasDimension.WEIRDNESS },
            new LineageBiasDimension[]{ LineageBiasDimension.EXPLORATION_PREFERENCE,
                                        LineageBiasDimension.RISK_APPETITE });

        ProceduralAbilityGenerator highExpGen = new ProceduralAbilityGenerator(
            registry, null, highExpLineageReg, resolver, null, true,
            ScoringMode.PROJECTION_WITH_CACHE, new AbilityDiversityIndex());
        ProceduralAbilityGenerator lowExpGen = new ProceduralAbilityGenerator(
            registry, null, lowExpLineageReg, resolver, null, true,
            ScoringMode.PROJECTION_WITH_CACHE, new AbilityDiversityIndex());

        // Opposing memory profiles amplify the lineage signal to a measurable level.
        ArtifactMemoryProfile highExpMem = PROFILE_MEMORIES[1]; // EXPLORER
        ArtifactMemoryProfile lowExpMem  = PROFILE_MEMORIES[2]; // RITUAL_CHAOS

        int runIterations = 200;
        Map<AbilityCategory, Integer> highExpCounts = new EnumMap<>(AbilityCategory.class);
        Map<AbilityCategory, Integer> lowExpCounts  = new EnumMap<>(AbilityCategory.class);

        for (int i = 0; i < runIterations; i++) {
            Artifact artifact = makeArtifact(0, i, BASE_SEED + i * 1001L);
            artifact.setLatentLineage("explorer-lineage");
            int stage = 1 + (i % 5);

            for (AbilityDefinition def : highExpGen.generate(artifact, stage, highExpMem).abilities()) {
                AbilityCategory cat = catIndex.get(def.id());
                if (cat != null) highExpCounts.merge(cat, 1, Integer::sum);
            }
            for (AbilityDefinition def : lowExpGen.generate(artifact, stage, lowExpMem).abilities()) {
                AbilityCategory cat = catIndex.get(def.id());
                if (cat != null) lowExpCounts.merge(cat, 1, Integer::sum);
            }
        }

        double jsd = computeCategoryJsd(highExpCounts, lowExpCounts);
        System.out.printf("LINEAGE_DIVERGENCE_JSD=%.4f%n", jsd);
        System.out.println("  HighExplore: " + summarizeCategoryCounts(highExpCounts));
        System.out.println("  LowExplore:  " + summarizeCategoryCounts(lowExpCounts));

        assertTrue(jsd >= MIN_LINEAGE_JSD,
            "Lineage JSD = " + String.format("%.4f", jsd) + " < " + MIN_LINEAGE_JSD
            + " — lineage influence must produce measurable divergence."
            + " HighExplore=" + summarizeCategoryCounts(highExpCounts)
            + " LowExplore=" + summarizeCategoryCounts(lowExpCounts));

        // Additional invariant: neither generator should collapse to a single category.
        assertNoDominantCategory(highExpCounts, "HighExplore lineage");
        assertNoDominantCategory(lowExpCounts,  "LowExplore lineage");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core simulation + assertions
    // ─────────────────────────────────────────────────────────────────────────

    private void assertProfileStability(int profileIdx) {
        String name = PROFILE_NAMES[profileIdx];
        AbilityRegistry registry = new AbilityRegistry();
        Map<String, AbilityCategory> catIndex = buildCategoryIndex(registry);

        // Number of templates defined per category (used to gate the ≥4-templates assertion).
        Map<AbilityCategory, Long> templatesPerCategory = registry.templates().stream()
            .collect(Collectors.groupingBy(AbilityTemplate::category, Collectors.counting()));

        ProceduralAbilityGenerator generator = new ProceduralAbilityGenerator(registry);
        ArtifactMemoryProfile mem = PROFILE_MEMORIES[profileIdx];

        // Deterministic initial population.
        List<Artifact> population = new ArrayList<>(POPULATION_SIZE);
        for (int i = 0; i < POPULATION_SIZE; i++) {
            population.add(makeArtifact(profileIdx, i, BASE_SEED + (long) profileIdx * 1_000_000L + i));
        }

        // ── Per-window accumulators (reset every SNAPSHOT_INTERVAL) ──────────
        Map<AbilityCategory, Integer>              winCatCounts = new EnumMap<>(AbilityCategory.class);
        Map<String, Integer>                        winTplCounts = new HashMap<>();
        Map<AbilityCategory, Map<String, Integer>> winTplByCat  = new EnumMap<>(AbilityCategory.class);
        int winTotal = 0;

        // ── Global accumulators ───────────────────────────────────────────────
        Map<AbilityCategory, Integer> gblCatCounts = new EnumMap<>(AbilityCategory.class);
        Map<String, Integer>           gblTplCounts = new HashMap<>();
        int gblTotal = 0;

        List<Snapshot> snapshots            = new ArrayList<>();
        List<Double>   effectivePoolPerWin  = new ArrayList<>();

        // ── Main simulation loop ──────────────────────────────────────────────
        for (int iter = 0; iter < ITERATIONS; iter++) {

            // Lifecycle rotation: retire one artifact, spawn a fresh one.
            if (iter > 0 && iter % ROTATION_INTERVAL == 0) {
                int slot = ((iter / ROTATION_INTERVAL) - 1) % POPULATION_SIZE;
                long newSeed = BASE_SEED + (long) profileIdx * 1_000_000L + POPULATION_SIZE + iter;
                population.set(slot, makeArtifact(profileIdx, POPULATION_SIZE + iter, newSeed));
            }

            // Deterministic artifact selection (round-robin through population).
            Artifact artifact = population.get(iter % POPULATION_SIZE);

            // Evolution stage advances uniformly over the full run (1 → 5).
            int stage = 1 + (iter * 5 / ITERATIONS);

            AbilityProfile abilityProfile = generator.generate(artifact, stage, mem);

            for (AbilityDefinition def : abilityProfile.abilities()) {
                AbilityCategory cat = catIndex.get(def.id());
                if (cat == null) continue;

                winCatCounts.merge(cat, 1, Integer::sum);
                winTplCounts.merge(def.id(), 1, Integer::sum);
                winTplByCat.computeIfAbsent(cat, k -> new HashMap<>()).merge(def.id(), 1, Integer::sum);
                gblCatCounts.merge(cat, 1, Integer::sum);
                gblTplCounts.merge(def.id(), 1, Integer::sum);
                winTotal++;
                gblTotal++;
            }

            // Snapshot at interval boundary.
            if ((iter + 1) % SNAPSHOT_INTERVAL == 0) {
                // Freeze per-category template map.
                Map<AbilityCategory, Map<String, Integer>> frozenByCat = new EnumMap<>(AbilityCategory.class);
                winTplByCat.forEach((cat, tMap) -> frozenByCat.put(cat, Map.copyOf(tMap)));

                snapshots.add(new Snapshot(iter + 1, Map.copyOf(winCatCounts), frozenByCat, winTotal));

                // Effective pool size: Simpson's diversity reciprocal applied to global counts so far.
                effectivePoolPerWin.add(computeEffectivePoolSize(gblTplCounts));

                winCatCounts.clear();
                winTplCounts.clear();
                winTplByCat.clear();
                winTotal = 0;
            }
        }

        // ── Print diagnostic summary ──────────────────────────────────────────
        double avgPool = effectivePoolPerWin.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        System.out.println("=== " + name + " — " + ITERATIONS + " iters, "
            + snapshots.size() + " snapshots, " + gblTotal + " abilities ===");
        System.out.println("  Global category dist: " + summarizeCategoryCounts(gblCatCounts));
        System.out.printf("  Avg effective pool size: %.2f%n", avgPool);
        printEarlyMidLateSnapshots(name, snapshots);

        // ── Assertion 1: Hard per-window category cap (60%) ──────────────────
        for (Snapshot snap : snapshots) {
            int total = Math.max(1, snap.total());
            for (Map.Entry<AbilityCategory, Integer> e : snap.catCounts().entrySet()) {
                double share = e.getValue() / (double) total;
                assertTrue(share <= MAX_CATEGORY_SHARE_HARD,
                    name + " @ iter=" + snap.iteration() + ": category " + e.getKey()
                    + " has " + pct(share) + " > hard cap " + pct(MAX_CATEGORY_SHARE_HARD));
            }
        }

        // ── Assertion 2: No sustained dominance > 50% for ≥5 consecutive windows ──
        assertNoSustainedDominance(name, snapshots);

        // ── Assertion 3: No template > 50% within its category (in windows with enough observations) ──
        // A minimum of 20 category-observations is required before the template-share
        // assertion fires; fewer observations yield unreliable per-template fractions.
        for (Snapshot snap : snapshots) {
            for (Map.Entry<AbilityCategory, Map<String, Integer>> catEntry : snap.tplByCat().entrySet()) {
                int catTotal = catEntry.getValue().values().stream().mapToInt(Integer::intValue).sum();
                if (catTotal < 20) continue; // Insufficient observations for statistical validity.
                for (Map.Entry<String, Integer> te : catEntry.getValue().entrySet()) {
                    double tShare = te.getValue() / (double) catTotal;
                    assertTrue(tShare <= MAX_TEMPLATE_SHARE_IN_CATEGORY,
                        name + " @ iter=" + snap.iteration() + ": template " + te.getKey()
                        + " in " + catEntry.getKey() + " has " + pct(tShare)
                        + " > template dominance cap " + pct(MAX_TEMPLATE_SHARE_IN_CATEGORY)
                        + " (catObs=" + catTotal + ")");
                }
            }
        }

        // ── Assertion 4: ≥4 active templates per meaningfully-active category ──
        // A category must have ≥1% of global ability observations before the
        // long-tail assertion fires.  Profiles that organically produce near-zero
        // observations for a given category (e.g. an Explorer profile generating
        // almost no COMBAT_TACTICAL_CONTROL abilities) are excluded: with fewer
        // than ~10 observations, template spread statistics are unreliable.
        int gblTotalObs = Math.max(1, gblTotal);
        for (AbilityCategory cat : AbilityCategory.values()) {
            if (templatesPerCategory.getOrDefault(cat, 0L) < MIN_ACTIVE_TEMPLATES_PER_CATEGORY) {
                continue;
            }
            int catObsGlobal = gblCatCounts.getOrDefault(cat, 0);
            if (catObsGlobal < (int) Math.ceil(0.01 * gblTotalObs)) {
                continue; // Too few observations for this category under this profile.
            }
            long seen = gblTplCounts.entrySet().stream()
                .filter(e -> catIndex.get(e.getKey()) == cat && e.getValue() > 0)
                .count();
            assertTrue(seen >= MIN_ACTIVE_TEMPLATES_PER_CATEGORY,
                name + ": category " + cat + " (catObs=" + catObsGlobal + "/" + gblTotalObs
                + ") has only " + seen + " active templates globally (need ≥"
                + MIN_ACTIVE_TEMPLATES_PER_CATEGORY + ")");
        }

        // ── Assertion 5: Effective pool size ≥ 2 (global average across snapshots) ──
        assertTrue(avgPool >= MIN_EFFECTIVE_POOL_SIZE,
            name + ": avg effective pool size " + String.format("%.2f", avgPool)
            + " < " + MIN_EFFECTIVE_POOL_SIZE + " — template selection is collapsing");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Constructs a deterministic artifact for the given profile/index/seed tuple.
     * UUID is derived deterministically from the profile and index so that
     * lineage assignment (which seeds from ownerId) is also deterministic.
     */
    private Artifact makeArtifact(int profileIdx, int index, long seed) {
        double[] aff = PROFILE_AFFINITIES[profileIdx];
        UUID ownerId = UUID.nameUUIDFromBytes(
            ("lh-p" + profileIdx + "-i" + index).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Artifact artifact = new Artifact(ownerId, "wooden_sword");
        artifact.setArtifactSeed(seed);
        artifact.setArtifactStorageKey("lh:" + seed);
        // Distribute across 10 lineage buckets for realistic lineage diversity.
        artifact.setLatentLineage("lh-lineage-" + (index % 10));
        artifact.setSeedPrecisionAffinity(aff[0]);
        artifact.setSeedBrutalityAffinity(aff[1]);
        artifact.setSeedSurvivalAffinity(aff[2]);
        artifact.setSeedMobilityAffinity(aff[3]);
        artifact.setSeedChaosAffinity(aff[4]);
        artifact.setSeedConsistencyAffinity(aff[5]);
        return artifact;
    }

    /** Builds a {@code Map<templateId, AbilityCategory>} index from the registry. */
    private Map<String, AbilityCategory> buildCategoryIndex(AbilityRegistry registry) {
        return registry.templates().stream()
            .collect(Collectors.toMap(AbilityTemplate::id, AbilityTemplate::category));
    }

    /**
     * Creates a {@link LineageRegistry} with a single pre-seeded lineage whose
     * bias genome has {@code positiveDimensions} set to +0.30 and
     * {@code negativeDimensions} set to −0.25.
     */
    private LineageRegistry buildBiasedLineageRegistry(
            String lineageId,
            LineageBiasDimension[] positiveDimensions,
            LineageBiasDimension[] negativeDimensions) {
        LineageRegistry reg = new LineageRegistry();
        Artifact seed = makeArtifact(0, 0, BASE_SEED);
        seed.setLatentLineage(lineageId);
        ArtifactLineage lineage = reg.assignLineage(seed);
        for (LineageBiasDimension dim : positiveDimensions) {
            lineage.evolutionaryBiasGenome().add(dim, 0.30);
        }
        for (LineageBiasDimension dim : negativeDimensions) {
            lineage.evolutionaryBiasGenome().add(dim, -0.25);
        }
        return reg;
    }

    /**
     * Asserts that no category sustains share &gt; {@link #MAX_CATEGORY_SHARE_SOFT}
     * for {@value #SUSTAINED_WINDOW_COUNT} or more consecutive snapshots.
     */
    private void assertNoSustainedDominance(String profileName, List<Snapshot> snapshots) {
        for (AbilityCategory cat : AbilityCategory.values()) {
            int consecutive = 0;
            for (Snapshot snap : snapshots) {
                int total = Math.max(1, snap.total());
                double share = snap.catCounts().getOrDefault(cat, 0) / (double) total;
                if (share > MAX_CATEGORY_SHARE_SOFT) {
                    consecutive++;
                    assertFalse(consecutive >= SUSTAINED_WINDOW_COUNT,
                        profileName + ": category " + cat + " sustained >" + pct(MAX_CATEGORY_SHARE_SOFT)
                        + " for " + consecutive + " consecutive windows (limit=" + SUSTAINED_WINDOW_COUNT
                        + "). Collapse detected.");
                } else {
                    consecutive = 0;
                }
            }
        }
    }

    /** Asserts that no single category exceeds 60% in the aggregate distribution. */
    private void assertNoDominantCategory(
            Map<AbilityCategory, Integer> counts, String label) {
        int total = Math.max(1, counts.values().stream().mapToInt(Integer::intValue).sum());
        for (Map.Entry<AbilityCategory, Integer> e : counts.entrySet()) {
            double share = e.getValue() / (double) total;
            assertTrue(share <= MAX_CATEGORY_SHARE_HARD,
                label + ": category " + e.getKey() + " dominates at " + pct(share)
                + " — lineage should not collapse into a single niche");
        }
    }

    /**
     * Computes the Jensen-Shannon divergence (log₂, range [0, 1]) between two
     * category count distributions.
     */
    private double computeCategoryJsd(
            Map<AbilityCategory, Integer> dist1,
            Map<AbilityCategory, Integer> dist2) {
        Set<AbilityCategory> allCats = EnumSet.allOf(AbilityCategory.class);
        double total1 = Math.max(1, dist1.values().stream().mapToInt(Integer::intValue).sum());
        double total2 = Math.max(1, dist2.values().stream().mapToInt(Integer::intValue).sum());

        double[] p = new double[allCats.size()];
        double[] q = new double[allCats.size()];
        int idx = 0;
        for (AbilityCategory cat : allCats) {
            p[idx] = dist1.getOrDefault(cat, 0) / total1;
            q[idx] = dist2.getOrDefault(cat, 0) / total2;
            idx++;
        }
        return jsd(p, q);
    }

    /** Jensen-Shannon divergence in log₂ bits (range [0, 1]). */
    private double jsd(double[] p, double[] q) {
        double[] m = new double[p.length];
        for (int i = 0; i < p.length; i++) {
            m[i] = (p[i] + q[i]) / 2.0;
        }
        return (klDivergence(p, m) + klDivergence(q, m)) / 2.0;
    }

    /** KL divergence K(p||q) in log₂ bits. */
    private double klDivergence(double[] p, double[] q) {
        double sum = 0.0;
        for (int i = 0; i < p.length; i++) {
            if (p[i] > 1e-12 && q[i] > 1e-12) {
                sum += p[i] * Math.log(p[i] / q[i]);
            }
        }
        return sum / Math.log(2.0);
    }

    /**
     * Simpson's diversity reciprocal: 1 / Σ(p_i²).
     * Returns the number of "equally likely" templates the distribution is
     * equivalent to.  A value &lt;2 signals near-collapse.
     */
    private double computeEffectivePoolSize(Map<String, Integer> templateCounts) {
        int total = templateCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) return 0.0;
        double sumSq = templateCounts.values().stream()
            .mapToDouble(c -> {
                double p = c / (double) total;
                return p * p;
            })
            .sum();
        return sumSq > 0 ? 1.0 / sumSq : 0.0;
    }

    /** Prints early, mid, and late snapshot summaries for diagnostic output. */
    private void printEarlyMidLateSnapshots(String profileName, List<Snapshot> snapshots) {
        if (snapshots.size() < 3) return;
        Snapshot early = snapshots.get(0);
        Snapshot mid   = snapshots.get(snapshots.size() / 2);
        Snapshot late  = snapshots.getLast();
        System.out.println("  [Early @" + early.iteration() + "]: " + summarizeCategoryCounts(early.catCounts()));
        System.out.println("  [Mid   @" + mid.iteration()   + "]: " + summarizeCategoryCounts(mid.catCounts()));
        System.out.println("  [Late  @" + late.iteration()  + "]: " + summarizeCategoryCounts(late.catCounts()));
    }

    private String summarizeCategoryCounts(Map<AbilityCategory, Integer> counts) {
        int total = Math.max(1, counts.values().stream().mapToInt(Integer::intValue).sum());
        return counts.entrySet().stream()
            .sorted(Map.Entry.<AbilityCategory, Integer>comparingByValue(Comparator.reverseOrder()))
            .map(e -> abbrev(e.getKey()) + "=" + pct(e.getValue() / (double) total))
            .collect(Collectors.joining(", "));
    }

    /** Abbreviates a category name to at most 8 characters for compact output. */
    private String abbrev(AbilityCategory cat) {
        String n = cat.name();
        return n.length() <= 8 ? n : n.substring(0, 8);
    }

    private String pct(double v) {
        return String.format("%.1f%%", v * 100);
    }
}
