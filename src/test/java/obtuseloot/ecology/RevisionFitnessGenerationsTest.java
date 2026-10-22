package obtuseloot.ecology;

import obtuseloot.abilities.*;
import obtuseloot.abilities.genome.*;
import obtuseloot.artifacts.*;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.evolution.*;
import obtuseloot.persistence.*;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Controlled modeled observations, not live physics or proof of a naturally emergent ecosystem. */
class RevisionFitnessGenerationsTest {
    private static final int COHORT = 32, GENERATIONS = 8;
    private final AbilityRegistry catalog = new AbilityRegistry();
    private final AbilityPerformanceModel model = catalog.registrationById("mobility.double_jump").performanceModel();
    private final AbilityDefinition definition = definition();

    @Test void differentLocalResponsesChangeActualParentageAcrossGenerationsAndReplayExactly() throws Exception {
        var open = run(20, 0);
        var ceiling = run(.0784, 2048);
        var replay = run(20, 0);
        assertEquals(open, replay, "Fixed inputs must reproduce parent seeds, revisions, genes and measurements");
        assertNotEquals(open.births, ceiling.births, "Changing only observed responses must change parentage");
        assertEquals(GENERATIONS * COHORT, open.births.size());
        assertEquals(GENERATIONS * COHORT, ceiling.births.size());
        Files.createDirectories(Path.of("target/revision-fitness"));
        Files.writeString(Path.of("target/revision-fitness/generations.csv"),
                "condition,generation,mean_kinetic_gene,variance,mean_response,mean_recovery_ticks,distinct_parents\n"
                        + rows("open", open) + rows("low_ceiling", ceiling));
        Files.writeString(Path.of("target/revision-fitness/parentage.txt"),
                "Controlled modeled observations; fixed cohort size is a test protocol, not a production quota.\n"
                        + "open=" + open.births + "\nlow_ceiling=" + ceiling.births + "\nreplay_equal=true\n");
    }

    private Run run(double headroom, double x) {
        var store = new MemoryStore();
        var manager = new ArtifactManager(store);
        var ecosystem = new LocalEcosystem(store, manager);
        var position = new WorldPosition(new UUID(0, 700), x, 0);
        var rng = new Random(722);
        var cohort = new ArrayList<Artifact>();
        for (int i = 0; i < COHORT; i++) {
            var item = ecosystem.createEquipment(new UUID(0, 701), EquipmentArchetype.ELYTRA, position, rng);
            var traits = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
            for (var trait : GenomeTrait.values()) traits.put(trait, .5);
            traits.put(GenomeTrait.KINETIC_BIAS, (double) i / (COHORT - 1));
            item.setGenome(new ArtifactGenome(item.getArtifactSeed(), traits, traits, Set.of()));
            manager.saveItem(item); cohort.add(item);
        }
        var rows = new ArrayList<Summary>();
        var births = new ArrayList<Birth>();
        for (int generation = 0; generation < GENERATIONS; generation++) {
            var tracker = new ArtifactUsageTracker();
            double sum = 0, squared = 0, responseSum = 0, recoverySum = 0;
            for (var item : cohort) {
                // Leave positive evidence for the discarded revision in the same local pool.
                observe(item, ecosystem, tracker, position, headroom, "before-" + generation);
                item.setEvolutionPath("strider-formed");
                assertTrue(new GenomeEvolutionEngine().advance(item));
                assertEquals(1, item.getGenomeRevision());
                double gene = item.getGenome().trait(GenomeTrait.KINETIC_BIAS);
                var expressed = model.express(item.getGenome());
                sum += gene; squared += gene * gene;
                responseSum += response(expressed, headroom); recoverySum += expressed.recoveryTicks(60);
                for (int attempt = 0; attempt < 3; attempt++)
                    observe(item, ecosystem, tracker, position, headroom, "after-" + generation + "-" + attempt);
                manager.saveItem(item);
            }
            ecosystem.save();
            manager = new ArtifactManager(store);
            ecosystem = new LocalEcosystem(store, manager); // Exercise the real codecs every generation.
            var children = new ArrayList<Artifact>();
            var parentSeeds = new HashSet<Long>();
            for (int i = 0; i < COHORT; i++) {
                var child = ecosystem.createEquipment(new UUID(0, 701), EquipmentArchetype.ELYTRA, position, rng);
                assertNotNull(child.getParentStorageKey());
                var parent = manager.resolveByStorageKey(child.getParentStorageKey());
                assertEquals(1, child.getParentGenomeRevision(), "Old revisions must never enter the parent draw");
                assertEquals(parent.getArtifactSeed(), child.getParentGenomeSeed());
                assertEquals(generation + 1, child.getInheritanceGeneration());
                assertTrue(child.getGenomeFitnessHistory().revisions().isEmpty());
                assertEquals(2, parent.getGenomeFitnessHistory().revisions().size());
                parentSeeds.add(parent.getArtifactSeed());
                births.add(new Birth(parent.getArtifactSeed(), child.getParentGenomeRevision(), child.getArtifactSeed(), child.getGenome().traits()));
                children.add(child);
            }
            rows.add(new Summary(generation, sum / COHORT, squared / COHORT - Math.pow(sum / COHORT, 2),
                    responseSum / COHORT, recoverySum / COHORT, parentSeeds.size()));
            long window = RuntimeSettings.get().inheritanceOutcomeWindowTicks();
            for (long tick = 0; tick <= window; tick++) ecosystem.advanceTick();
            assertTrue(ecosystem.pool().candidates(position, 20, window).isEmpty());
            assertNotNull(store.loadArtifactProfile(cohort.getFirst().getArtifactStorageKey()), "Expired evidence must not delete owned items");
            cohort = children;
        }
        return new Run(List.copyOf(rows), List.copyOf(births));
    }

    private void observe(Artifact item, LocalEcosystem ecosystem, ArtifactUsageTracker tracker,
                         WorldPosition position, double headroom, String key) {
        var expressed = model.express(item.getGenome());
        double response = response(expressed, headroom);
        var context = new AbilityEventContext(definition.trigger(), item, item.getItemReputation(), 1,
                "controlled-modeled-jump", AbilityRuntimeContext.intentional(AbilitySource.OTHER), null, true).withPosition(position);
        var result = AbilityExecutionResult.observed(definition, context, AbilityOutcomeType.WORLD_INTERACTION, response, null, "modeled")
                .withObservationKey(key).withPerformance(new AbilityPerformanceMeasurement(response, expressed.recoveryTicks(60)));
        double delta = tracker.trackAbilityExecution(item, context, result, definition);
        ecosystem.observe(new AbilityOutcomeObservation(context, result, definition, delta));
    }

    private double response(AbilityPerformance expressed, double headroom) {
        double velocity = expressed.output(.42), height = 0, peak = 0;
        for (int tick = 0; tick < 6; tick++) {
            height += velocity; peak = Math.max(peak, Math.min(headroom, height));
            velocity = (velocity - .08) * .98;
        }
        return peak;
    }
    private AbilityDefinition definition() {
        var t = catalog.templateById("mobility.double_jump");
        return new AbilityDefinition(t.id(), t.name(), t.family(), t.trigger(), t.mechanic(), "", "", "", "", "", "",
                List.of(), List.of(), t.metadata(), "1", "2", "3", "4", "5");
    }
    private String rows(String condition, Run run) {
        var text = new StringBuilder();
        for (var row : run.rows) text.append(String.format(Locale.ROOT, "%s,%d,%.8f,%.8f,%.8f,%.3f,%d%n",
                condition, row.generation, row.gene, row.variance, row.response, row.recovery, row.parents));
        return text.toString();
    }
    private record Summary(int generation, double gene, double variance, double response, double recovery, int parents) { }
    private record Birth(long parentSeed, long parentRevision, long childSeed, Map<GenomeTrait, Double> genes) { }
    private record Run(List<Summary> rows, List<Birth> births) { }

    /** Real profile/ecology serialization without SQL latency; no raw Artifact references survive a load. */
    private static final class MemoryStore implements PlayerStateStore {
        final Map<String, String> profiles = new HashMap<>(); String ecology;
        public void saveArtifactProfile(Artifact item) { profiles.put(item.getArtifactStorageKey(), ArtifactProfileCodec.encode(item)); }
        public Artifact loadArtifactProfile(String key) { return profiles.containsKey(key) ? ArtifactProfileCodec.decode(key, profiles.get(key)) : null; }
        public void saveEcologyState(String state) { ecology = state; }
        public String loadEcologyState() { return ecology; }
        public void saveArtifact(UUID id, Artifact item) { throw new UnsupportedOperationException(); }
        public Artifact loadArtifact(UUID id) { return null; }
        public void saveReputation(UUID id, ArtifactReputation reputation) { throw new UnsupportedOperationException(); }
        public ArtifactReputation loadReputation(UUID id) { return null; }
    }
}
