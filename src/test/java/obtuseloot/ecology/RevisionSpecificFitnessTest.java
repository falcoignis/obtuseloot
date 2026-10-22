package obtuseloot.ecology;

import obtuseloot.abilities.*;
import obtuseloot.abilities.genome.GenomeRevision;
import obtuseloot.evolution.*;
import obtuseloot.persistence.ArtifactProfileCodec;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RevisionSpecificFitnessTest {
    private final WorldPosition position = new WorldPosition(new UUID(0, 10), 0, 0);

    @Test void outcomesCaptureTheProducingRevisionIncludingDelayedResultsAndContextCopies() {
        var item = LocalInheritancePoolTest.item(42);
        var old = LocalInheritancePoolTest.observation(item, position, true, 1, 1, "before");
        item.setEvolutionPath("strider-formed");
        new GenomeEvolutionEngine().advance(item);
        var current = GenomeRevision.capture(item);
        assertNotEquals(current, old.context().genomeRevision());
        assertEquals(old.context().genomeRevision(), old.context().withPosition(position).withActivation(null).genomeRevision());
        var tracker = new ArtifactUsageTracker();
        tracker.trackAbilityExecution(item, old.context(), old.result(), old.definition());
        var history = item.getGenomeFitnessHistory();
        assertTrue(history.profile(old.context().genomeRevision()).validatedUtilityScore() > 0);
        assertEquals(0, history.profile(current).validatedUtilityScore());
        var now = LocalInheritancePoolTest.observation(item, position, true, 0, -1, "after");
        tracker.trackAbilityExecution(item, now.context(), now.result(), now.definition());
        assertTrue(history.profile(current).validatedUtilityScore() < 0);
        assertTrue(history.profile(old.context().genomeRevision()).validatedUtilityScore() > 0);
        var pool = new LocalInheritancePool();
        pool.record(old, 100); pool.record(now, 100);
        assertEquals(1, pool.candidates(position, 20, 100).size());
        assertEquals(0, pool.candidates(position, 20, 100).getFirst().genomeRevision());
        assertEquals(pool.candidates(position, 20, 100), LocalInheritancePool.decode(pool.encode()).candidates(position, 20, 100));
    }

    @Test void historyAndTheNextRepeatPenaltySurviveProfileSaveReloadAndIdentityReplacement() throws Exception {
        var item = LocalInheritancePoolTest.item(42);
        var tracker = new ArtifactUsageTracker();
        for (int i = 0; i < 4; i++) {
            var observation = LocalInheritancePoolTest.observation(item, position, true, i == 2 ? 0 : 1, 1, "same");
            tracker.trackAbilityExecution(item, observation.context(), observation.result(), observation.definition());
        }
        var loaded = ArtifactProfileCodec.decode(item.getArtifactStorageKey(), ArtifactProfileCodec.encode(item));
        var restoredTracker = new ArtifactUsageTracker(); restoredTracker.hydrateFromArtifact(loaded);
        var revision = GenomeRevision.capture(item);
        assertEquals(item.getGenomeFitnessHistory().signals(revision), loaded.getGenomeFitnessHistory().signals(revision));
        for (int i = 0; i < 12; i++) {
            var next = LocalInheritancePoolTest.observation(item, position, true, 1, 1, "same");
            var restored = LocalInheritancePoolTest.observation(loaded, position, true, 1, 1, "same");
            tracker.trackAbilityExecution(item, next.context(), next.result(), next.definition());
            restoredTracker.trackAbilityExecution(loaded, restored.context(), restored.result(), restored.definition());
            assertEquals(item.getGenomeFitnessHistory().signals(revision), loaded.getGenomeFitnessHistory().signals(revision));
        }
        var replacement = LocalInheritancePoolTest.item(43);
        replacement.setArtifactStorageKey(item.getArtifactStorageKey());
        new obtuseloot.artifacts.ArtifactIdentityTransition(item, replacement, "test");
        assertSame(item.getGenomeFitnessHistory(), replacement.getGenomeFitnessHistory());
        assertEquals(item.getGenomeFitnessHistory().signals(revision), replacement.getGenomeFitnessHistory().signals(revision));
        assertEquals(0, replacement.getGenomeFitnessHistory().profile(GenomeRevision.capture(replacement)).validatedUtilityScore());
    }

    @Test void missingOrCorruptFitnessAttributionIsRefused() {
        var item = LocalInheritancePoolTest.item(42);
        var observation = LocalInheritancePoolTest.observation(item, position, true, 1, 1, "record");
        new ArtifactUsageTracker().trackAbilityExecution(item, observation.context(), observation.result(), observation.definition());
        String encoded = ArtifactProfileCodec.encode(item);
        assertThrows(obtuseloot.persistence.PlayerStateLoadException.class,
                () -> ArtifactProfileCodec.decode(item.getArtifactStorageKey(), encoded.replace("revision-count: 1", "revision-count: 2")));
        assertThrows(obtuseloot.persistence.PlayerStateLoadException.class,
                () -> ArtifactProfileCodec.decode(item.getArtifactStorageKey(), encoded.replace("recent-no-ops:", "missing-window:")));
        assertThrows(obtuseloot.persistence.PlayerStateLoadException.class,
                () -> ArtifactProfileCodec.decode(item.getArtifactStorageKey(), encoded.replace("parent-seed:", "missing-parent-seed:")));
        assertThrows(obtuseloot.persistence.PlayerStateLoadException.class,
                () -> ArtifactProfileCodec.decode(item.getArtifactStorageKey(), encoded.replace("parent-seed: 0", "parent-seed: 1")));
        var pool = new LocalInheritancePool(); pool.record(observation, 100);
        assertThrows(IllegalStateException.class, () -> LocalInheritancePool.decode(pool.encode().replace("genome-revision: 0", "genome-revision: -1")));
        assertThrows(IllegalStateException.class, () -> LocalInheritancePool.decode(pool.encode().replace("genome-revision:", "missing-revision:")));
    }

    @Test void classificationsDoNotBecomeGenomeFitness() {
        var item = LocalInheritancePoolTest.item(42);
        var offline = LocalInheritancePoolTest.observation(item, position, false, 1, 1, "classification");
        new ArtifactUsageTracker().trackAbilityExecution(item, offline.context(), offline.result(), offline.definition());
        assertTrue(item.getGenomeFitnessHistory().revisions().isEmpty());
    }
}
