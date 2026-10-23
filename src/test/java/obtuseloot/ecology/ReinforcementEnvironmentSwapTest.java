package obtuseloot.ecology;

import obtuseloot.abilities.*;
import obtuseloot.abilities.genome.*;
import obtuseloot.artifacts.*;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.ecosystem.EnvironmentPressureEngine;
import obtuseloot.evolution.*;
import obtuseloot.memory.ArtifactMemoryEngine;
import obtuseloot.persistence.*;
import obtuseloot.reputation.ArtifactReputation;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Modeled response experiment, with unrelated macro telemetry held fixed in both arms. */
class ReinforcementEnvironmentSwapTest {
    private static final int COHORT = 32, GENERATIONS = 12, SWAP_AT = 6;
    private final AbilityRegistry catalog = new AbilityRegistry();
    private final AbilityPerformanceModel model = catalog.registrationById("mobility.double_jump").performanceModel();
    private final AbilityDefinition definition = definition();
    private final List<WorldPosition> positions = List.of(new WorldPosition(new UUID(0, 800), 0, 0),
            new WorldPosition(new UUID(0, 800), 2048, 0));

    @Test void swappedConditionsChangeMeasuredSelectionWithTheBlanketTermAblated() throws Exception {
        compareAndWrite(FeedbackMode.CONTEXT_LEGACY, FeedbackMode.AGGREGATE_LEGACY,
                "reinforcement-ablation");
    }

    @Test void swappedConditionsChangeMeasuredSelectionWithEcologicalScalingAblated() throws Exception {
        compareAndWrite(FeedbackMode.ABLATED, FeedbackMode.CONTEXT_LEGACY, "ecological-gene-ablation");
    }

    private void compareAndWrite(FeedbackMode current, FeedbackMode legacy, String reportDirectory) throws Exception {
        var currentSwap = run(current, true);
        var currentSteady = run(current, false);
        var legacySwap = run(legacy, true);
        var legacySteady = run(legacy, false);
        var replay = run(current, true);
        assertEquals(currentSwap, replay, "Parentage, all genes and measurements must replay with isolated state");
        int prefix = SWAP_AT * positions.size() * COHORT;
        assertEquals(currentSteady.births.subList(0, prefix), currentSwap.births.subList(0, prefix));
        assertEquals(legacySteady.births.subList(0, prefix), legacySwap.births.subList(0, prefix));
        assertNotEquals(currentSteady.births.subList(prefix, currentSteady.births.size()),
                currentSwap.births.subList(prefix, currentSwap.births.size()), "Changed responses must reach actual parent draws");
        assertNotEquals(legacySteady.births.subList(prefix, legacySteady.births.size()),
                legacySwap.births.subList(prefix, legacySwap.births.size()));
        assertNotEquals(parentDraws(currentSteady).subList(prefix, currentSteady.births.size()),
                parentDraws(currentSwap).subList(prefix, currentSwap.births.size()),
                "Changed local conditions must change selected parent identities, not only child gene values");
        assertNotEquals(legacySwap.births, currentSwap.births, "The removed term must affect real inherited genes/parentage");

        var runs = new LinkedHashMap<String, Run>();
        runs.put("ablated_swap", currentSwap); runs.put("ablated_steady", currentSteady);
        runs.put("legacy_swap", legacySwap); runs.put("legacy_steady", legacySteady);
        var csv = new StringBuilder("mode,generation,site,headroom,mean_gene,variance,mean_response,mean_recovery,mean_fitness,lineage_simpson,founders,distinct_parents\n");
        var ancestry = new StringBuilder();
        for (var entry : runs.entrySet()) {
            for (var row : entry.getValue().rows) csv.append(String.format(Locale.ROOT,
                    "%s,%d,%d,%.4f,%.8f,%.8f,%.8f,%.3f,%.8f,%.8f,%d,%d%n", entry.getKey(), row.generation,
                    row.site, row.headroom, row.gene, row.variance, row.response, row.recovery, row.fitness,
                    row.diversity, row.founders, row.parents));
            for (var birth : entry.getValue().births) {
                ancestry.append(entry.getKey()).append(',').append(birth.generation).append(',').append(birth.site)
                        .append(',').append(birth.parentSeed).append(',').append(birth.parentRevision)
                        .append(',').append(birth.childSeed).append(',').append(birth.founder);
                for (var trait : GenomeTrait.values()) ancestry.append(',').append(trait).append('=')
                        .append(Double.toHexString(birth.genes.get(trait))).append('/')
                        .append(Double.toHexString(birth.latent.get(trait))).append('/').append(birth.activated.contains(trait));
                ancestry.append('\n');
            }
        }
        var directory = Path.of("target", reportDirectory); Files.createDirectories(directory);
        Files.writeString(directory.resolve("generations.csv"), csv);
        Files.writeString(directory.resolve("parentage.csv"), ancestry);
        Files.writeString(directory.resolve("protocol.txt"), "Modeled observations; no native gameplay claim.\n"
                + "Current transform: " + current + "; legacy transform: " + legacy + "\n"
                + "32 samples per site, 12 generations, swap at generation 6; RNG seeds 722 and 723.\n"
                + "Sites are 2048 blocks apart in the same world; actual configured chunk-radius parent filtering is used.\n"
                + "No target mean, lineage count or distribution threshold; diversity/variance are reported.\n"
                + "Unrelated macro telemetry held at the same empty state; default seeded pressure retained without advancing seasons.\n"
                + "Initial environmental modifiers: " + new EnvironmentPressureEngine().currentModifiers() + "\n"
                + "Fixed cohorts/evidence expiry are test protocol, not production population controls.\nreplay_equal=true\n");
    }

    private List<Map.Entry<Long, Long>> parentDraws(Run run) {
        return run.births.stream().map(birth -> Map.entry(birth.parentSeed, birth.parentRevision)).toList();
    }

    private Run run(FeedbackMode mode, boolean swap) {
        var store = new MemoryStore(); var manager = new ArtifactManager(store);
        var ecosystem = new LocalEcosystem(store, manager);
        var tracker = new ArtifactUsageTracker(); var evaluator = new ArtifactFitnessEvaluator();
        var environment = new EnvironmentPressureEngine();
        ExperienceEvolutionEngine feedback = switch (mode) {
            case AGGREGATE_LEGACY -> new LegacyExperienceEvolutionEngine(tracker, evaluator, environment);
            case CONTEXT_LEGACY -> new LegacyEcologicalExperienceEvolutionEngine(tracker, evaluator, environment);
            case ABLATED -> new ExperienceEvolutionEngine(tracker, evaluator, environment);
        };
        var genetics = new GenomeEvolutionEngine(catalog, new ArtifactMemoryEngine(), null, null, feedback, ScoringMode.PROJECTION_WITH_CACHE);
        var randoms = List.of(new Random(722), new Random(723));
        var cohorts = new ArrayList<List<Artifact>>(); var founders = new HashMap<String, Long>();
        for (int site = 0; site < positions.size(); site++) {
            var items = new ArrayList<Artifact>();
            for (int i = 0; i < COHORT; i++) {
                var item = ecosystem.createEquipment(new UUID(0, 801), EquipmentArchetype.ELYTRA, positions.get(site), randoms.get(site));
                assertNull(item.getParentStorageKey());
                var traits = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
                for (var trait : GenomeTrait.values()) traits.put(trait, .5);
                traits.put(GenomeTrait.KINETIC_BIAS, (double) i / (COHORT - 1));
                item.setGenome(new ArtifactGenome(item.getArtifactSeed(), traits, traits, Set.of()));
                manager.saveItem(item); items.add(item); founders.put(item.getArtifactStorageKey(), item.getArtifactSeed());
            }
            cohorts.add(items);
        }
        var rows = new ArrayList<Row>(); var births = new ArrayList<Birth>();
        for (int generation = 0; generation < GENERATIONS; generation++) {
            for (int site = 0; site < positions.size(); site++) {
                double room = headroom(site, generation, swap);
                for (var item : cohorts.get(site)) {
                    observe(item, ecosystem, positions.get(site), room, "before-" + generation);
                    item.setEvolutionPath("strider-formed");
                    assertTrue(genetics.advance(item)); assertEquals(1, item.getGenomeRevision());
                    for (int attempt = 0; attempt < 3; attempt++)
                        observe(item, ecosystem, positions.get(site), room, "after-" + generation + "-" + attempt);
                    manager.saveItem(item);
                }
            }
            ecosystem.save(); manager = new ArtifactManager(store); ecosystem = new LocalEcosystem(store, manager);
            var next = new ArrayList<List<Artifact>>();
            for (int site = 0; site < positions.size(); site++) {
                var items = cohorts.get(site);
                var allowed = new HashSet<String>(); for (var item : items) allowed.add(item.getArtifactStorageKey());
                var children = new ArrayList<Artifact>(); var parents = new HashSet<Long>(); var counts = new HashMap<Long, Integer>();
                for (int i = 0; i < COHORT; i++) {
                    var child = ecosystem.createEquipment(new UUID(0, 801), EquipmentArchetype.ELYTRA, positions.get(site), randoms.get(site));
                    assertTrue(allowed.contains(child.getParentStorageKey()), "Parent must belong to the current local cohort");
                    var parent = manager.resolveByStorageKey(child.getParentStorageKey());
                    assertEquals(parent.getArtifactSeed(), child.getParentGenomeSeed()); assertEquals(1, child.getParentGenomeRevision());
                    assertEquals(generation + 1, child.getInheritanceGeneration()); assertTrue(child.getGenomeFitnessHistory().revisions().isEmpty());
                    assertEquals(2, parent.getGenomeFitnessHistory().revisions().size());
                    long founder = founders.get(parent.getArtifactStorageKey()); founders.put(child.getArtifactStorageKey(), founder);
                    parents.add(parent.getArtifactSeed()); counts.merge(founder, 1, Integer::sum);
                    births.add(new Birth(generation, site, parent.getArtifactSeed(), child.getParentGenomeRevision(), child.getArtifactSeed(),
                            founder, child.getGenome().traits(), child.getGenome().latentTraits(), child.getGenome().activatedLatentTraits()));
                    children.add(child);
                }
                double mean = items.stream().mapToDouble(a -> a.getGenome().trait(GenomeTrait.KINETIC_BIAS)).average().orElseThrow();
                double variance = items.stream().mapToDouble(a -> Math.pow(a.getGenome().trait(GenomeTrait.KINETIC_BIAS) - mean, 2)).average().orElseThrow();
                double room = headroom(site, generation, swap);
                double response = items.stream().mapToDouble(a -> response(model.express(a.getGenome()), room)).average().orElseThrow();
                double recovery = items.stream().mapToLong(a -> model.express(a.getGenome()).recoveryTicks(60)).average().orElseThrow();
                double fitness = ecosystem.pool().candidates(positions.get(site), RuntimeSettings.get().inheritanceRadiusChunks(),
                        RuntimeSettings.get().inheritanceOutcomeWindowTicks()).stream().filter(c -> c.genomeRevision() == 1)
                        .mapToDouble(LocalInheritancePool.ParentCandidate::fitness).average().orElseThrow();
                double diversity = 1 - counts.values().stream().mapToDouble(count -> Math.pow((double) count / COHORT, 2)).sum();
                assertTrue(Double.isFinite(variance) && diversity >= 0 && diversity <= 1);
                rows.add(new Row(generation, site, room, mean, variance, response, recovery, fitness, diversity, counts.size(), parents.size()));
                next.add(children);
            }
            long window = RuntimeSettings.get().inheritanceOutcomeWindowTicks();
            for (long tick = 0; tick <= window; tick++) ecosystem.advanceTick();
            for (var position : positions) assertTrue(ecosystem.pool().candidates(position, RuntimeSettings.get().inheritanceRadiusChunks(), window).isEmpty());
            assertNotNull(store.loadArtifactProfile(cohorts.getFirst().getFirst().getArtifactStorageKey()), "Expiry cannot delete owned items");
            cohorts = next;
        }
        return new Run(List.copyOf(rows), List.copyOf(births));
    }

    private void observe(Artifact item, LocalEcosystem ecosystem, WorldPosition position, double room, String key) {
        var expressed = model.express(item.getGenome()); double benefit = response(expressed, room);
        var context = new AbilityEventContext(definition.trigger(), item, item.getItemReputation(), 1, "modeled-swap",
                AbilityRuntimeContext.intentional(AbilitySource.OTHER), null, true).withPosition(position);
        var result = AbilityExecutionResult.observed(definition, context, AbilityOutcomeType.WORLD_INTERACTION, benefit, null, "modeled")
                .withObservationKey(key).withPerformance(new AbilityPerformanceMeasurement(benefit, expressed.recoveryTicks(60)));
        var observation = new AbilityOutcomeObservation(context, result, definition, 0); assertTrue(observation.liveMeasured());
        var budget = new TriggerBudgetResolver().resolve(definition, context);
        // Use the real measured-utility classifier without populating the unrelated wall-clock macro ecology.
        item.getGenomeFitnessHistory().record(new UtilityOutcomeRecord(result.abilityId(), result.mechanic(), result.trigger(), result.status(),
                result.outcomeType(), result.meaningfulOutcome(), true, 1, budget.triggerCost() + budget.evaluationCost(),
                context.source(), ecosystem.pool().gameTick(), key, result.performance(), context.genomeRevision()));
        ecosystem.observe(observation);
    }
    private double headroom(int site, int generation, boolean swap) {
        boolean open = site == 0;
        if (swap && generation >= SWAP_AT) open = !open;
        return open ? 20 : .0784;
    }
    private double response(AbilityPerformance expressed, double room) {
        double velocity = expressed.output(.42), height = 0, peak = 0;
        for (int tick = 0; tick < 6; tick++) { height += velocity; peak = Math.max(peak, Math.min(room, height)); velocity = (velocity - .08) * .98; }
        return peak;
    }
    private AbilityDefinition definition() {
        var t = catalog.templateById("mobility.double_jump");
        return new AbilityDefinition(t.id(), t.name(), t.family(), t.trigger(), t.mechanic(), "", "", "", "", "", "",
                List.of(), List.of(), t.metadata(), "1", "2", "3", "4", "5");
    }
    private enum FeedbackMode { AGGREGATE_LEGACY, CONTEXT_LEGACY, ABLATED }
    private record Row(int generation, int site, double headroom, double gene, double variance, double response,
                       double recovery, double fitness, double diversity, int founders, int parents) { }
    private record Birth(int generation, int site, long parentSeed, long parentRevision, long childSeed, long founder,
                         Map<GenomeTrait, Double> genes, Map<GenomeTrait, Double> latent, Set<GenomeTrait> activated) { }
    private record Run(List<Row> rows, List<Birth> births) { }
    private static final class MemoryStore implements PlayerStateStore {
        final Map<String, String> profiles = new HashMap<>(); String ecology;
        public void saveArtifactProfile(Artifact item) { profiles.put(item.getArtifactStorageKey(), ArtifactProfileCodec.encode(item)); }
        public Artifact loadArtifactProfile(String key) { return profiles.containsKey(key) ? ArtifactProfileCodec.decode(key, profiles.get(key)) : null; }
        public void saveEcologyState(String state) { ecology = state; }
        public String loadEcologyState() { return ecology; }
        public void saveArtifact(UUID id, Artifact item) { throw new UnsupportedOperationException(); }
        public Artifact loadArtifact(UUID id) { return null; }
        public void saveReputation(UUID id, ArtifactReputation rep) { throw new UnsupportedOperationException(); }
        public ArtifactReputation loadReputation(UUID id) { return null; }
    }
}
