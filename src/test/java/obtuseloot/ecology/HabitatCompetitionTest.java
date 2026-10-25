package obtuseloot.ecology;

import obtuseloot.evolution.GenomeEvolutionEngine;
import obtuseloot.artifacts.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HabitatCompetitionTest {
    private final UUID world = new UUID(0, 10);
    private WorldPosition at(int overhead) { return new WorldPosition(world, 0, 0, new HabitatContext(overhead)); }
    private void observe(LocalInheritancePool pool, Artifact item, WorldPosition position, double benefit, String key) {
        pool.record(LocalEcosystemHealthTest.observation(item, position, benefit, 60, key), 100);
    }

    @Test void currentMeasuredConditionsKeepDifferentUsefulTradeoffsWithoutReweightingTheirUtility() {
        var pool = new LocalInheritancePool(); var low = LocalInheritancePoolTest.item(1); var high = LocalInheritancePoolTest.item(2);
        observe(pool, low, at(0), .0784, "low"); observe(pool, high, at(20), 10, "high");
        double fitness = pool.candidates(at(0), 20, 100).getFirst().fitness();
        for (int i = 0; i < 100; i++) {
            assertEquals(low.getArtifactStorageKey(), pool.choose(at(0), 20, 100, new Random(i)).storageKey());
            assertEquals(high.getArtifactStorageKey(), pool.choose(at(20), 20, 100, new Random(i)).storageKey());
        }
        assertEquals(2, pool.recentEvidence(at(0), 20, 100).size());
        assertEquals(pool.candidates(at(0), 20, 100), LocalInheritancePool.decode(pool.encode()).candidates(at(0), 20, 100));
        assertEquals(fitness, pool.candidates(at(0), 20, 100).getFirst().fitness());
    }

    @Test void failuresInTheClosestContextCannotBeHiddenBySuccessElsewhereOrByTheSameItemsHistory() {
        var pool = new LocalInheritancePool(); var item = LocalInheritancePoolTest.item(1);
        observe(pool, item, at(0), 0, "local-failure"); observe(pool, item, at(20), 100, "remote-success");
        assertNull(pool.choose(at(0), 20, 100, new Random(1)));
        assertNotNull(pool.choose(at(20), 20, 100, new Random(1)));
        assertEquals(1, pool.competitiveEvidence(at(0), 20, 100, candidate -> true).getFirst().individual().attempts());
    }

    @Test void intermediateContextsUseEquidistantEvidenceWithoutPredeterminedNicheLabelsOrEqualParentShares() {
        var pool = new LocalInheritancePool(); var a = LocalInheritancePoolTest.item(1); var b = LocalInheritancePoolTest.item(2);
        observe(pool, a, at(3), 1, "a"); observe(pool, b, at(7), 3, "b");
        var tied = pool.candidates(at(5), 20, 100);
        assertEquals(2, tied.size()); assertEquals(3, tied.getLast().fitness() / tied.getFirst().fitness(), 1e-12);
        assertEquals(a.getArtifactStorageKey(), pool.choose(at(4), 20, 100, new Random(1)).storageKey());
        assertEquals(b.getArtifactStorageKey(), pool.choose(at(6), 20, 100, new Random(1)).storageKey());
        var farther = LocalInheritancePoolTest.item(3); observe(pool, farther, at(30), 1000, "farther");
        assertEquals(tied, pool.candidates(at(5), 20, 100));
    }

    @Test void unknownAndSkyContextsAreExplicitAndNeverSubstitutedForRoofedEvidence() {
        var pool = new LocalInheritancePool(); var item = LocalInheritancePoolTest.item(1);
        observe(pool, item, at(1), 1, "roofed");
        var unknown = new WorldPosition(world, 0, 0); var sky = new WorldPosition(world, 0, 0, new HabitatContext(null));
        assertNull(pool.choose(unknown, 20, 100, new Random(1))); assertNull(pool.choose(sky, 20, 100, new Random(1)));
        observe(pool, item, sky, 2, "sky"); assertNotNull(pool.choose(sky, 20, 100, new Random(1)));
        assertNull(pool.choose(unknown, 20, 100, new Random(1)));
        observe(pool, item, unknown, 3, "unknown"); assertNotNull(pool.choose(unknown, 20, 100, new Random(1)));
        assertEquals(pool.encode(), LocalInheritancePool.decode(pool.encode()).encode());
    }

    @Test void expiredDistantAndOtherWorldEvidenceCannotDefineTheNearestHabitat() {
        var pool = new LocalInheritancePool(); var item = LocalInheritancePoolTest.item(1);
        observe(pool, item, at(0), 1, "expired"); for (int i = 0; i < 101; i++) pool.advanceTick(100);
        observe(pool, LocalInheritancePoolTest.item(2), new WorldPosition(world, 321, 0, new HabitatContext(0)), 100, "far");
        observe(pool, LocalInheritancePoolTest.item(3), new WorldPosition(new UUID(0, 20), 0, 0, new HabitatContext(0)), 100, "world");
        var nearby = LocalInheritancePoolTest.item(4); observe(pool, nearby, at(7), 1, "nearest-supported");
        assertEquals(nearby.getArtifactStorageKey(), pool.choose(at(0), 20, 100, new Random(1)).storageKey());
    }

    @Test void oldRevisionsCannotBlockCurrentEvidenceAndHealthReportsActualConditionedChancesReadOnly() {
        var store = new LocalEcosystemHealthTest.Store(); var manager = new ArtifactManager(store);
        var ecology = new LocalEcosystem(store, manager);
        var old = LocalEcosystemHealthTest.item(manager, 1, .5); var current = LocalEcosystemHealthTest.item(manager, 2, .5);
        ecology.observe(LocalEcosystemHealthTest.observation(old, at(0), 100, 60, "old"));
        old.setEvolutionPath("strider-formed"); assertTrue(new GenomeEvolutionEngine().advance(old)); manager.saveItem(old);
        ecology.observe(LocalEcosystemHealthTest.observation(current, at(3), 1, 60, "current"));
        var farther = LocalEcosystemHealthTest.item(manager, 3, .5);
        ecology.observe(LocalEcosystemHealthTest.observation(farther, at(20), 100, 60, "farther"));
        var before = ecology.pool().encode(); int writes = store.saves;
        var health = ecology.healthSnapshot(at(0));
        assertEquals(2, health.observedItems()); assertEquals(1, health.oldRevisions()); assertEquals(1, health.eligibleParents());
        assertEquals(1, health.competingItems()); assertEquals(1, health.competingAttempts());
        assertEquals(1, health.effectiveParents()); assertEquals(1, health.maximumParentShare());
        assertEquals(before, ecology.pool().encode()); assertEquals(writes, store.saves);
        var child = ecology.createEquipment(new UUID(0, 99), EquipmentArchetype.ELYTRA, at(0), new Random(1));
        assertEquals(current.getArtifactStorageKey(), child.getParentStorageKey());
        assertTrue(child.getGenomeFitnessHistory().revisions().isEmpty());
    }

    @Test void unsupportedBirthContextCreatesAFreshLineageWithoutErasingNearbyIndividuals() {
        var store = new LocalEcosystemHealthTest.Store(); var manager = new ArtifactManager(store);
        var ecology = new LocalEcosystem(store, manager); var parent = LocalEcosystemHealthTest.item(manager, 1, .5);
        ecology.observe(LocalEcosystemHealthTest.observation(parent, at(0), 100, 60, "roofed"));
        var sky = new WorldPosition(world, 0, 0, new HabitatContext(null));
        var health = ecology.healthSnapshot(sky);
        assertEquals(1, health.observedItems()); assertEquals(0, health.competingItems()); assertEquals(0, health.competingAttempts());
        var child = ecology.createEquipment(new UUID(0, 99), EquipmentArchetype.ELYTRA, sky, new Random(1));
        assertNull(child.getParentStorageKey()); assertNotEquals(parent.getLatentLineage(), child.getLatentLineage());
        assertNotNull(store.loadArtifactProfile(parent.getArtifactStorageKey()));
    }

    @Test void eventContextFreezesHabitatBeforeCallbacksOrDelayedObservationCanChangeTheWorld() {
        var calls = new ArrayList<String>();
        var reference = new java.util.concurrent.atomic.AtomicReference<>(world(calls, true, 68));
        Block block = (Block) Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[]{Block.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getWorld" -> reference.get();
                    case "getX", "getZ" -> 0;
                    case "getY" -> 64;
                    default -> throw new AssertionError(method.getName());
                });
        var item = LocalInheritancePoolTest.item(1);
        var event = new org.bukkit.event.block.BlockBreakEvent(block, null);
        var context = new obtuseloot.abilities.AbilityEventContext(obtuseloot.abilities.AbilityTrigger.ON_BLOCK_HARVEST,
                item, item.getItemReputation(), 1, "snapshot", obtuseloot.abilities.AbilityRuntimeContext.intentional(
                obtuseloot.abilities.AbilitySource.OTHER), event);
        reference.set(world(calls, true, 80));
        assertEquals(new HabitatContext(4), context.position().habitat());
        assertEquals(context.position(), context.withActivation(null).position());
        assertEquals(new HabitatContext(16), WorldPosition.fromEvent(event).habitat());
    }

    @Test void storageRequiresCapturedHabitatAndRejectsOldOrIncompleteFormats() {
        var pool = new LocalInheritancePool(); observe(pool, LocalInheritancePoolTest.item(1), at(3), 1, "one");
        String encoded = pool.encode();
        assertTrue(encoded.contains("target: none"));
        for (String invalid : List.of(encoded.replace("version: 6", "version: 5"),
                encoded.replace("target: none", "target: Not A Material"), encoded.replaceAll("(?m)^\\s*target: none\\n", ""),
                encoded.replaceAll("(?m)^\\s*holder: .*\\n", ""),
                encoded.replace("kind: roofed", "kind: bogus"), encoded.replace("column-height-blocks: 3", "column-height-blocks: -1"),
                encoded.replace("column-height-blocks: 3", "column-height-blocks: broken"), encoded.replace("kind: roofed", "kind: sky")))
            assertThrows(IllegalStateException.class, () -> LocalInheritancePool.decode(invalid));
    }

    @Test void liveProbeUsesOnlyALoadedColumnAndCapturesMoreThanABinaryRoofFlag() {
        var calls = new ArrayList<String>();
        for (int depth : new int[]{0, 1, 3, 12}) {
            World world = world(calls, true, 65 + depth);
            var captured = WorldPosition.from(new Location(world, -1.2, 64.8, 2));
            assertEquals(new HabitatContext(depth + 1), captured.habitat());
        }
        assertNull(WorldPosition.from(new Location(world(calls, false, 70), 0, 64, 0)).habitat());
        assertEquals(new HabitatContext(null), WorldPosition.from(new Location(world(calls, true, 500), 0, 64, 0)).habitat());
        assertFalse(calls.contains("loadChunk"));
    }
    private World world(List<String> calls, boolean loaded, int ceiling) {
        return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[]{World.class}, (proxy, method, args) -> {
            calls.add(method.getName());
            return switch (method.getName()) {
                case "getUID" -> world;
                case "isChunkLoaded" -> loaded;
                case "getMinHeight" -> -64;
                case "getMaxHeight" -> 320;
                case "getBlockAt" -> {
                    assertTrue(loaded); int y = (Integer) args[1];
                    yield Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[]{Block.class},
                            (block, query, ignored) -> { assertEquals("isPassable", query.getName()); return y > 63 && y < ceiling; });
                }
                default -> throw new AssertionError(method.getName());
            };
        });
    }
}
