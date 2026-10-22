package obtuseloot.ecology;

import obtuseloot.abilities.*;
import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LocalInheritancePoolTest {
    private final UUID world = new UUID(0, 10);
    private final WorldPosition origin = new WorldPosition(world, 0, 0);

    @Test void twentyChunksIsAnInclusiveSameWorldHorizontalCircle() {
        assertTrue(origin.within(new WorldPosition(world, 320, 0), 20));
        assertFalse(origin.within(new WorldPosition(world, 320.01, 0), 20));
        assertFalse(origin.within(new WorldPosition(world, 320, 320), 20));
        assertFalse(origin.within(new WorldPosition(new UUID(0, 11), 0, 0), 20));
        assertTrue(new WorldPosition(world, -0.1, -0.1).within(new WorldPosition(world, 0.1, 0.1), 1));
    }

    @Test void noServerWideFallbackWhenOnlyDistantOrOtherWorldParentsExist() {
        var pool = new LocalInheritancePool();
        pool.record(observation(item(1), new WorldPosition(world, 321, 0), true, 1, 1, "far"), 100);
        pool.record(observation(item(2), new WorldPosition(new UUID(0, 20), 0, 0), true, 1, 1, "other-world"), 100);
        assertNull(pool.choose(origin, 20, 100, new Random(1)));
        assertEquals(1, pool.candidates(origin, 21, 100).size());
    }

    @Test void localScoringDoesNotImportFailureOrSuccessFromOutsideTheRadius() {
        var pool = new LocalInheritancePool();
        var parent = item(1);
        pool.record(observation(parent, origin, true, 1, 1, "local"), 100);
        for (int i = 0; i < 20; i++) pool.record(observation(parent, new WorldPosition(world, 2000, 0), true, 0, -1, "far"), 100);
        assertEquals(1, pool.candidates(origin, 20, 100).size());
        assertTrue(pool.candidates(new WorldPosition(world, 2000, 0), 20, 100).isEmpty());
        assertEquals(1, pool.candidates(origin, 20, 100).getFirst().attempts());
    }

    @Test void classificationsAndMissingPositionsNeverBecomeBreeders() {
        var pool = new LocalInheritancePool();
        pool.record(observation(item(1), origin, false, 1, 1, "offline"), 100);
        pool.record(observation(item(2), null, true, 1, 1, "unknown-position"), 100);
        assertTrue(pool.candidates(origin, 20, 100).isEmpty());
    }

    @Test void reproductionFitnessIgnoresArtificialNicheBonusInTheOldUtilityDelta() {
        var first = new LocalInheritancePool();
        var second = new LocalInheritancePool();
        var artifact = item(1);
        first.record(observation(artifact, origin, true, 1, 10000, "actual-target"), 100);
        second.record(observation(artifact, origin, true, 1, -10000, "actual-target"), 100);
        assertEquals(first.candidates(origin, 20, 100), second.candidates(origin, 20, 100));
    }

    @Test void failedUtilityCanRemoveEligibilityWithoutDeletingTheItem() {
        var pool = new LocalInheritancePool();
        var parent = item(1);
        pool.record(observation(parent, origin, true, 1, 1, "success"), 100);
        assertNotNull(pool.choose(origin, 20, 100, new Random(1)));
        for (int i = 0; i < 16; i++) pool.record(observation(parent, origin, true, 0, -1, "failure"), 100);
        assertNull(pool.choose(origin, 20, 100, new Random(1)));
        assertEquals(1, parent.getArtifactSeed());
        assertEquals("artifact:" + new UUID(0, 1), parent.getArtifactStorageKey());
    }

    @Test void restartPreservesEvidenceClockAndConsecutiveRepeatPenalties() {
        var pool = new LocalInheritancePool();
        var parent = item(1);
        pool.record(observation(parent, origin, true, 1, 1, "same-target"), 10);
        for (int i = 0; i < 5; i++) pool.advanceTick(10);
        var restored = LocalInheritancePool.decode(pool.encode());
        assertEquals(5, restored.gameTick());
        assertEquals(pool.candidates(origin, 20, 10), restored.candidates(origin, 20, 10));
        for (int i = 0; i < 16; i++) {
            var outcome = observation(parent, origin, true, 1, 1, "same-target");
            pool.record(outcome, 10); restored.record(outcome, 10);
        }
        assertEquals(pool.candidates(origin, 20, 10), restored.candidates(origin, 20, 10));
        for (int i = 0; i < 10; i++) restored.advanceTick(10);
        assertTrue(restored.candidates(origin, 20, 10).isEmpty());
    }

    @Test void corruptStateCannotSilentlyResetEvolution() {
        assertThrows(IllegalStateException.class, () -> LocalInheritancePool.decode("version: 9\ngame-tick: 0\n"));
        assertThrows(IllegalStateException.class, () -> LocalInheritancePool.decode("version: 1\ngame-tick: -1\n"));
        assertThrows(IllegalStateException.class, () -> LocalInheritancePool.decode("not: [yaml"));
    }
    @Test void measuredRecoveryAffectsLocalFitnessAndSurvivesRestartWithoutImportingOldUtilityBonuses() {
        var pool = new LocalInheritancePool();
        var low = observation(item(1), origin, true, .6, 9999, "low");
        var high = observation(item(2), origin, true, .6, -9999, "high");
        pool.record(new AbilityOutcomeObservation(low.context(), low.result().withPerformance(new AbilityPerformanceMeasurement(.6, 30)), low.definition(), low.utilityDelta()), 100);
        pool.record(new AbilityOutcomeObservation(high.context(), high.result().withPerformance(new AbilityPerformanceMeasurement(.6, 90)), high.definition(), high.utilityDelta()), 100);
        var candidates = pool.candidates(origin, 20, 100);
        assertEquals(2, candidates.size()); assertTrue(candidates.get(0).fitness() > candidates.get(1).fitness());
        assertEquals(candidates, LocalInheritancePool.decode(pool.encode()).candidates(origin, 20, 100));
        assertThrows(IllegalStateException.class, () -> LocalInheritancePool.decode(pool.encode().replace("recovery-ticks: 30", "recovery-ticks: -1")));
        assertThrows(IllegalStateException.class, () -> LocalInheritancePool.decode(pool.encode().replace("benefit: 0.6", "benefit: .NaN")));
    }

    static Artifact item(long id) {
        var artifact = new Artifact(new UUID(0, 90), "wooden_sword");
        artifact.setArtifactStorageKey("artifact:" + new UUID(0, id));
        artifact.setArtifactSeed(id);
        artifact.setLatentLineage("lineage-" + id);
        return artifact;
    }

    static AbilityOutcomeObservation observation(Artifact artifact, WorldPosition position, boolean live,
                                                  double amount, double oldDelta, String key) {
        var t = new AbilityRegistry().templateById("precision.material_insight");
        var definition = new AbilityDefinition(t.id(), t.name(), t.family(), t.trigger(), t.mechanic(), "", "", "", "", "", "",
                List.of(), List.of(), t.metadata(), "1", "2", "3", "4", "5");
        var context = new AbilityEventContext(t.trigger(), artifact, new ArtifactReputation(), 1, "inspect",
                AbilityRuntimeContext.intentional(AbilitySource.BLOCK_INSPECT), null, live).withPosition(position);
        var result = AbilityExecutionResult.observed(definition, context, AbilityOutcomeType.INFORMATION, amount, null, "measured")
                .withObservationKey(key);
        return new AbilityOutcomeObservation(context, result, definition, oldDelta);
    }
}
