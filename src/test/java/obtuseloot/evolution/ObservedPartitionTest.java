package obtuseloot.evolution;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class ObservedPartitionTest {
    private static final String ROUTE = "NAVIGATION_ANCHOR@ON_WORLD_SCAN";
    private static final String FRONTIER = "TRAIL_SENSE@ON_WORLD_SCAN";

    private final AtomicLong clock = new AtomicLong(0L);
    private final NicheBifurcationRegistry registry = new NicheBifurcationRegistry(1, 1, 0, clock::get);
    private final NichePopulationTracker tracker = new NichePopulationTracker(
            new EcosystemRoleClassifier(), new EcosystemSaturationModel(), registry);

    @Test
    void noObservedSubnichesNeverSplits() {
        assertTrue(registry.evaluateBifurcation("NAVIGATION", 1.0D, 1.0D, 0.5D, 10,
                clock.get(), Set.of()).isEmpty());
        assertEquals(0, registry.dynamicNicheCount());
    }

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
    void trackerPartitionsNonDominantObservedSubniches() {
        record(1L, 10.0D, 1.0D, 10.0D, 10.0D);
        record(2L, 10.0D, 1.0D, 10.0D, 10.0D);
        assertEquals("terrain_awareness", tracker.nicheProfile(1L).specialization().dominantSubniche());
        assertEquals("terrain_awareness", tracker.nicheProfile(2L).specialization().dominantSubniche());

        tracker.evaluateBifurcations(1L);

        assertEquals(1, registry.bifurcations().size());
        NicheBifurcation split = registry.bifurcations().get(0);
        assertEquals(split.childNicheA(), registry.assignChildNiche("NAVIGATION", "frontier_pathfinding"));
        assertEquals(split.childNicheB(), registry.assignChildNiche("NAVIGATION", "route_guidance"));
        assertNull(registry.assignChildNiche("NAVIGATION", "terrain_awareness"),
                "An observed subniche belonging only to another parent must not enter this partition");
    }

    @Test
    void discardedItemsDoNotSupplyObservedSubniches() {
        tracker.recordTelemetry(1L, Map.of(ROUTE, signal(ROUTE, 1.0D, 10.0D)));
        tracker.recordTelemetry(2L, Map.of(ROUTE, signal(ROUTE, 1.0D, 10.0D)));
        tracker.recordTelemetry(3L, Map.of(FRONTIER, signal(FRONTIER, 1.0D, 10.0D)));
        tracker.markDiscarded(3L);

        tracker.evaluateBifurcations(1L);

        assertTrue(registry.bifurcations().isEmpty());
        assertEquals("INSUFFICIENT_SUBNICHES", registry.lastGate("NAVIGATION").orElseThrow().gate());
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
