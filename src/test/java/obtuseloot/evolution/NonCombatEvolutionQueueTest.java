package obtuseloot.evolution;

import obtuseloot.abilities.*;
import obtuseloot.artifacts.Artifact;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NonCombatEvolutionQueueTest {
    @Test
    void measuredLiveEffectsDoNotRequireAnAbilitySpecificEventAdapterInTheQueue() {
        var queue = new NonCombatEvolutionQueue();
        var rep = new ArtifactReputation();
        var measured = observation(artifact(), rep, 1.2, true, 1.0);
        var c = measured.context();
        var context = new AbilityEventContext(c.trigger(), c.artifact(), c.reputation(), c.value(), c.source(),
                c.runtimeContext(), null, true);
        queue.observe(new AbilityOutcomeObservation(context, measured.result(), measured.definition(), measured.utilityDelta()));
        assertEquals(1, queue.pendingCount());
        assertEquals(1, rep.getPrecision());
    }
    @Test
    void measuredObservationsCoalesceUntilGameTickBoundaryAndIdleTicksDoNothing() {
        var queue = new NonCombatEvolutionQueue();
        Artifact artifact = artifact();
        var reputation = new ArtifactReputation();
        queue.observe(observation(artifact, reputation, 0.7, true, 1.0));
        queue.observe(observation(artifact, reputation, 0.7, true, 1.0));
        assertEquals(1, reputation.getPrecision());
        assertEquals(1, queue.pendingCount());
        var updates = new ArrayList<NonCombatEvolutionQueue.Pending>();
        for (int tick = 0; tick < 99; tick++) queue.tick(100, updates::add, this::unexpectedFailure);
        assertTrue(updates.isEmpty());
        queue.tick(100, updates::add, this::unexpectedFailure);
        assertEquals(1, updates.size());
        assertTrue(updates.getFirst().matches(artifact, reputation));
        for (int tick = 0; tick < 200; tick++) queue.tick(100, updates::add, this::unexpectedFailure);
        assertEquals(1, updates.size());
        assertEquals(0, reputation.getKills());
    }

    @Test
    void classificationsAndUnimplementedEffectsNeverQueueOrGrantCredit() {
        var queue = new NonCombatEvolutionQueue();
        var reputation = new ArtifactReputation();
        queue.observe(observation(artifact(), reputation, 10, false, 1.0));
        queue.observe(observation(artifact(), reputation, 10, true, null));
        assertEquals(0, queue.pendingCount());
        assertEquals(0, reputation.getTotalScore());
    }

    @Test
    void noOpPenaltyOffsetsFutureReputationCredit() {
        var queue = new NonCombatEvolutionQueue();
        var artifact = artifact();
        var reputation = new ArtifactReputation();
        queue.observe(observation(artifact, reputation, 0.7, true, 1.0));
        queue.observe(observation(artifact, reputation, -0.5, true, 0.0));
        queue.observe(observation(artifact, reputation, 0.7, true, 1.0));
        assertEquals(0, reputation.getTotalScore());
        queue.observe(observation(artifact, reputation, 0.2, true, 1.0));
        assertEquals(1, reputation.getPrecision());
    }

    @Test
    void processorFailureRetriesWithoutReplayingEarnedReputation() {
        var queue = new NonCombatEvolutionQueue();
        var artifact = artifact();
        var reputation = new ArtifactReputation();
        queue.observe(observation(artifact, reputation, 1.5, true, 1.0));
        var errors = new ArrayList<RuntimeException>();
        queue.flushAll(update -> { throw new IllegalStateException("test failure"); }, (owner, error) -> errors.add(error));
        assertEquals(1, queue.pendingCount());
        assertEquals(1, errors.size());
        queue.flushAll(update -> { }, this::unexpectedFailure);
        assertEquals(0, queue.pendingCount());
        assertEquals(1, reputation.getPrecision());
    }

    @Test
    void replacementIdentityDoesNotMatchAnOldPendingWindow() {
        var queue = new NonCombatEvolutionQueue();
        var artifact = artifact();
        var reputation = new ArtifactReputation();
        queue.observe(observation(artifact, reputation, 0.2, true, 1.0));
        artifact.setArtifactSeed(-2);
        queue.flushAll(update -> assertFalse(update.matches(artifact, reputation)), this::unexpectedFailure);
    }

    @Test
    void ownerFlushDoesNotConsumeAnotherPlayersWindow() {
        var queue = new NonCombatEvolutionQueue();
        var first = artifact();
        var second = new Artifact(new UUID(0, 2), "wooden_sword");
        second.setArtifactSeed(2);
        queue.observe(observation(first, new ArtifactReputation(), 0.1, true, 1.0));
        queue.observe(observation(second, new ArtifactReputation(), 0.1, true, 1.0));
        queue.flush(first.getOwnerId(), update -> assertSame(first, update.artifact()), this::unexpectedFailure);
        assertEquals(1, queue.pendingCount());
        queue.forget(second.getOwnerId());
        assertEquals(0, queue.pendingCount());
    }

    private void unexpectedFailure(UUID owner, RuntimeException error) { fail(error); }

    private Artifact artifact() {
        var artifact = new Artifact(new UUID(0, 1), "wooden_sword");
        artifact.setArtifactSeed(-1);
        return artifact;
    }

    private AbilityOutcomeObservation observation(Artifact artifact, ArtifactReputation reputation,
                                                  double delta, boolean live, Double amount) {
        var t = new AbilityRegistry().templateById("precision.material_insight");
        var definition = new AbilityDefinition(t.id(), t.name(), t.family(), t.trigger(), t.mechanic(), "", "", "", "", "", "",
                List.of(), List.of(), t.metadata(), "1", "2", "3", "4", "5");
        Event event = new Event() { @Override public HandlerList getHandlers() { return new HandlerList(); } };
        var context = new AbilityEventContext(t.trigger(), artifact, reputation, 1, "test",
                AbilityRuntimeContext.intentional(AbilitySource.OTHER), event, live);
        var result = amount == null
                ? new AbilityExecutionResult(t.id(), t.mechanic(), t.trigger(), artifact.getArtifactStorageKey(), artifact.getOwnerId(),
                    AbilityExecutionStatus.NO_OP, AbilityOutcomeType.INFORMATION, false, "effect-not-implemented", null)
                : AbilityExecutionResult.observed(definition, context, AbilityOutcomeType.INFORMATION, amount, null, null);
        return new AbilityOutcomeObservation(context, result, definition, delta);
    }
}
