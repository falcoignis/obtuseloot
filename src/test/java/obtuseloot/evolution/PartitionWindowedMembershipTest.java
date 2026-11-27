package obtuseloot.evolution;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class PartitionWindowedMembershipTest {
    private static final String ROUTE = "NAVIGATION_ANCHOR@ON_WORLD_SCAN";
    private static final String FRONTIER = "TRAIL_SENSE@ON_WORLD_SCAN";

    private final AtomicLong clock = new AtomicLong(0L);
    private final NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1, 1, 0, clock::get);
    private final NichePopulationTracker tracker = new NichePopulationTracker(
            new EcosystemRoleClassifier(), new EcosystemSaturationModel(), registry);

    @Test
    void oneObservedSubnicheNeverSplitsEvenUnderSustainedPressure() {
        for (int window = 0; window < 5; window++) {
            assertTrue(registry.evaluateBifurcation("NAVIGATION", 1.0D, 1.0D, 0.5D, 10,
                    window, Set.of("route_guidance")).isEmpty());
        }
        assertEquals("INSUFFICIENT_SUBNICHES", registry.lastGate("NAVIGATION").orElseThrow().gate());
        assertEquals(0, registry.dynamicNicheCount());
        tracker.recordTelemetry(1L, Map.of(ROUTE, signal(ROUTE, 1.0D, 10.0D)));
        tracker.recordTelemetry(2L, Map.of(ROUTE, signal(ROUTE, 1.0D, 10.0D)));
        tracker.evaluateBifurcations(1L);
        assertTrue(registry.bifurcations().isEmpty(), "Tracker must pass observed identities rather than invent a pair");
    }

    @Test
    void partitionMakesBothChildrenReachableEvenForSameHashParity() {
        assertEquals("a".hashCode() % 2, "c".hashCode() % 2);
        NicheBifurcation split = split(Set.of("a", "c"));
        assertEquals(split.childNicheA(), registry.assignChildNiche("NAVIGATION", "a"));
        assertEquals(split.childNicheB(), registry.assignChildNiche("NAVIGATION", "c"));
        assertNull(registry.assignChildNiche("NAVIGATION", "unseen"));
    }

    @Test
    void retiringEitherSiblingDoesNotRepartitionAcrossSurvivingPairs() {
        for (boolean retireA : new boolean[]{false, true}) {
            NicheBifurcation first = split(Set.of("a", "c"));
            NicheBifurcation latest = split(Set.of("a", "c"));
            String retired = retireA ? latest.childNicheA() : latest.childNicheB();
            String survivor = retireA ? latest.childNicheB() : latest.childNicheA();
            Map<String, Long> population = Map.of(first.childNicheA(), 1L, first.childNicheB(), 1L, survivor, 1L);
            registry.collapseUnderpopulatedChildren(population, Map.of(), 10);
            registry.collapseUnderpopulatedChildren(population, Map.of(), 11);
            assertFalse(registry.isDynamicNiche(retired));
            assertTrue(registry.isDynamicNiche(survivor));
            assertEquals(latest.childNicheA(), registry.assignChildNiche("NAVIGATION", "a"));
            assertEquals(latest.childNicheB(), registry.assignChildNiche("NAVIGATION", "c"));
        }
    }

    @Test
    void survivingChildRemainsEligibleAndRetiredSiblingDoesNotRedirectItsSignals() {
        record(1L, 4.0D, 1.0D, 10.0D, 10.0D);
        record(2L, 4.0D, 1.0D, 10.0D, 10.0D);
        tracker.evaluateBifurcations(1L);
        String survivor = registry.assignChildNiche("NAVIGATION", "route_guidance");
        String retired = registry.assignChildNiche("NAVIGATION", "frontier_pathfinding");
        record(1L, 8.0D, 2.0D, 20.0D, 20.0D);
        record(2L, 8.0D, 2.0D, 20.0D, 20.0D);
        tracker.evaluateBifurcations(2L);
        assertFalse(registry.isDynamicNiche(retired));
        assertEquals(java.util.List.of(survivor), registry.childrenOf("NAVIGATION"));
        record(1L, 12.0D, 3.0D, 30.0D, 30.0D);
        record(2L, 12.0D, 3.0D, 30.0D, 30.0D);
        tracker.evaluateBifurcations(3L);
        assertEquals(survivor, tracker.effectiveNicheName(1L));
        record(1L, 12.0D, 13.0D, 40.0D, 40.0D);
        tracker.evaluateBifurcations(4L);
        assertEquals("NAVIGATION", tracker.effectiveNicheName(1L));
        assertEquals(retired, registry.assignChildNiche("NAVIGATION", "frontier_pathfinding"));
    }

    @Test
    void oppositeAdvantagesOccupyBothChildrenAndTiesStayInParent() {
        record(1L, 4.0D, 1.0D, 10.0D, 10.0D);
        record(2L, 1.0D, 4.0D, 10.0D, 10.0D);
        record(3L, 2.0D, 2.0D, 10.0D, 10.0D);
        tracker.evaluateBifurcations(1L);
        assertEquals(registry.assignChildNiche("NAVIGATION", "route_guidance"), tracker.effectiveNicheName(1L));
        assertEquals(registry.assignChildNiche("NAVIGATION", "frontier_pathfinding"), tracker.effectiveNicheName(2L));
        assertNotEquals(tracker.effectiveNicheName(1L), tracker.effectiveNicheName(2L));
        assertEquals("NAVIGATION", tracker.effectiveNicheName(3L));
    }

    @Test
    void positiveBudgetIsRequiredOnBothSides() {
        record(1L, 4.0D, 1.0D, 10.0D, 0.0D);
        record(2L, 1.0D, 4.0D, 0.0D, 10.0D);
        tracker.evaluateBifurcations(1L);
        assertEquals("NAVIGATION", tracker.effectiveNicheName(1L));
        assertEquals("NAVIGATION", tracker.effectiveNicheName(2L));
    }

    @Test
    void intervalTieRemovesMembershipDespiteLargeLifetimeAdvantage() {
        record(1L, 1000.0D, 1.0D, 10.0D, 10.0D);
        record(2L, 1000.0D, 1.0D, 10.0D, 10.0D);
        tracker.evaluateBifurcations(1L);
        assertTrue(registry.isDynamicNiche(tracker.effectiveNicheName(1L)));
        record(1L, 1001.0D, 2.0D, 20.0D, 20.0D);
        tracker.evaluateBifurcations(2L);
        assertEquals("NAVIGATION", tracker.effectiveNicheName(1L));
        assertEquals("NAVIGATION", tracker.effectiveNicheName(2L), "No new evidence must also release membership");
    }

    @Test
    void unchangedSnapshotCannotReusePreviousIntervalEvidence() {
        record(1L, 4.0D, 1.0D, 10.0D, 10.0D);
        record(2L, 4.0D, 1.0D, 10.0D, 10.0D);
        tracker.evaluateBifurcations(1L);
        assertTrue(registry.isDynamicNiche(tracker.effectiveNicheName(1L)));
        record(1L, 4.0D, 1.0D, 10.0D, 10.0D);
        tracker.evaluateBifurcations(2L);
        assertEquals("NAVIGATION", tracker.effectiveNicheName(1L));
    }

    @Test
    void discardingAndReactivatingAnItemDoesNotReplayItsLifetimeEvidence() {
        record(1L, 4.0D, 1.0D, 10.0D, 10.0D);
        record(2L, 4.0D, 1.0D, 10.0D, 10.0D);
        tracker.evaluateBifurcations(1L);
        tracker.markDiscarded(1L);
        record(1L, 4.0D, 1.0D, 10.0D, 10.0D);
        tracker.evaluateBifurcations(2L);
        assertEquals("NAVIGATION", tracker.effectiveNicheName(1L));
    }

    @Test
    void comparisonUsesSumOfUtilityOverSumOfBudgetRatherThanMeanSignalDensities() {
        // Two route signals: mean density is 5.0, but pooled utility/budget is only 0.1.
        // Frontier density 0.2 must win. Stored density fields are deliberately misleading.
        Map<String, MechanicUtilitySignal> signals = Map.of(
                ROUTE, signal(ROUTE, 10.0D, 1.0D),
                "NAVIGATION_ANCHOR@ON_CHUNK_ENTER", signal("NAVIGATION_ANCHOR@ON_CHUNK_ENTER", 0.0D, 99.0D),
                FRONTIER, signal(FRONTIER, 2.0D, 10.0D));
        tracker.recordTelemetry(1L, signals);
        tracker.recordTelemetry(2L, signals);
        tracker.evaluateBifurcations(1L);
        assertEquals(registry.assignChildNiche("NAVIGATION", "frontier_pathfinding"), tracker.effectiveNicheName(1L));
    }

    @Test
    void triggeringObservationBelongsOnlyToNextIntervalAndCanFlipMembership() {
        record(1L, 9.0D, 1.0D, 10.0D, 10.0D);
        record(2L, 9.0D, 1.0D, 10.0D, 10.0D);
        clock.set(5_000L);
        record(1L, 9.0D, 11.0D, 20.0D, 20.0D);
        String routeChild = registry.assignChildNiche("NAVIGATION", "route_guidance");
        assertEquals(routeChild, tracker.effectiveNicheName(1L), "Boundary observation cannot affect the interval it closes");
        clock.set(10_000L);
        record(2L, 9.0D, 1.0D, 10.0D, 10.0D);
        assertEquals(registry.assignChildNiche("NAVIGATION", "frontier_pathfinding"), tracker.effectiveNicheName(1L));
        assertNotEquals(routeChild, tracker.effectiveNicheName(1L));
        assertEquals("NAVIGATION", tracker.effectiveNicheName(2L));
    }

    @Test
    void multipleSnapshotsInAnIntervalCountOnlyTheirDeltas() {
        record(1L, 1.0D, 1.0D, 10.0D, 10.0D);
        record(2L, 1.0D, 1.0D, 10.0D, 10.0D);
        tracker.evaluateBifurcations(1L);
        record(1L, 101.0D, 1.0D, 110.0D, 10.0D);
        record(1L, 101.0D, 201.0D, 110.0D, 110.0D);
        tracker.evaluateBifurcations(2L);
        assertEquals(registry.assignChildNiche("NAVIGATION", "frontier_pathfinding"), tracker.effectiveNicheName(1L),
                "Frontier has rate 2 versus route rate 1, despite the intermediate route advantage");
    }

    private NicheBifurcation split(Set<String> observed) {
        return registry.evaluateBifurcation("NAVIGATION", 1.0D, 1.0D, 0.5D, 10,
                clock.get(), observed).orElseThrow();
    }

    private void record(long seed, double routeUtility, double frontierUtility, double routeBudget, double frontierBudget) {
        tracker.recordTelemetry(seed, Map.of(ROUTE, signal(ROUTE, routeUtility, routeBudget),
                FRONTIER, signal(FRONTIER, frontierUtility, frontierBudget)));
    }

    private static MechanicUtilitySignal signal(String key, double utility, double budget) {
        return new MechanicUtilitySignal(key, utility, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D,
                (long) budget, 1L, budget);
    }
}
