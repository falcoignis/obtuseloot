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

/** Modeled multi-seed diagnostic: ancestry drift and retained local performance are separate. */
class LocalEcosystemHealthBaselineTest {
    private static final int COHORT = 32, GENERATIONS = 12, SWAP_AT = 6;
    private final AbilityRegistry catalog = new AbilityRegistry();
    private final AbilityPerformanceModel model = catalog.registrationById("mobility.double_jump").performanceModel();
    private final AbilityDefinition definition = definition();

    @Test void neutralMappingGivesEqualParentChancesWithoutReplacingTheProductionSelector() {
        var store = new MemoryStore(); var manager = new ArtifactManager(store); var ecosystem = new LocalEcosystem(store, manager);
        var position = new WorldPosition(new UUID(0, 800), 0, 0);
        var first = LocalEcosystemHealthTest.item(manager, 1, 0);
        var second = LocalEcosystemHealthTest.item(manager, 2, 1);
        observe(first, ecosystem, position, .0784, "first"); observe(second, ecosystem, position, .0784, "second");
        var candidates = ecosystem.pool().candidates(position, 20, 6000);
        assertEquals(2, candidates.size()); assertNotEquals(candidates.getFirst().fitness(), candidates.getLast().fitness());
        var counts = new HashMap<String, Integer>();
        var grid = new Random(42) {
            int draw;
            @Override public double nextDouble() { return (draw++ + .5) / 20; }
        };
        for (int i = 0; i < 20; i++) {
            var child = ecosystem.createEquipment(new UUID(0, 801), EquipmentArchetype.ELYTRA, position,
                    parentRandom(ecosystem, manager, position, grid, true));
            counts.merge(child.getParentStorageKey(), 1, Integer::sum);
        }
        assertEquals(Map.of(first.getArtifactStorageKey(), 10, second.getArtifactStorageKey(), 10), counts);
    }

    @Test void compareMeasuredSelectionWithNeutralSamplingInSeparatedAndOverlappingHabitats() throws Exception {
        var csv = new StringBuilder("seed,mode,generation,site,headroom,mean_gene,variance,mean_response,mean_recovery,mean_pool_fitness,founder_simpson,founders,distinct_parents,mean_local_fitness,effective_parents,max_parent_share,cross_site_parents\n");
        var ancestry = new StringBuilder();
        for (long seed : new long[]{722, 1722, 2722, 3722, 4722, 5722, 6722, 7722}) {
            for (boolean neutral : List.of(false, true)) for (int separation : new int[]{160, 2048}) {
                String mode = (neutral ? "neutral" : "measured") + (separation == 160 ? "_overlap" : "_separate");
                var run = run(seed, separation, neutral);
                if (seed == 722 && !neutral && separation == 160)
                    assertEquals(run, run(seed, separation, false), "The diagnostic must replay with independent state");
                for (var row : run.rows) csv.append(String.format(Locale.ROOT,
                        "%d,%s,%d,%d,%.4f,%.8f,%.8f,%.8f,%.3f,%.8f,%.8f,%d,%d,%.8f,%.8f,%.8f,%d%n", seed, mode,
                        row.generation, row.site, row.headroom, row.gene, row.variance, row.response, row.recovery,
                        row.fitness, row.diversity, row.founders, row.parents, row.localFitness, row.effectiveParents, row.maximumShare, row.crossSiteParents));
                for (var birth : run.births) {
                    ancestry.append(seed).append(',').append(mode).append(',').append(birth.generation)
                            .append(',').append(birth.site).append(',').append(birth.parentSeed)
                            .append(',').append(birth.parentRevision).append(',').append(birth.childSeed)
                            .append(',').append(birth.founder);
                    for (var trait : GenomeTrait.values()) ancestry.append(',').append(trait).append('=')
                            .append(Double.toHexString(birth.genes.get(trait))).append('/')
                            .append(Double.toHexString(birth.latent.get(trait))).append('/').append(birth.activated.contains(trait));
                    ancestry.append('\n');
                }
            }
        }
        var directory = Path.of("target/local-ecosystem-health"); Files.createDirectories(directory);
        Files.writeString(directory.resolve("generations.csv"), csv);
        Files.writeString(directory.resolve("parentage.csv"), ancestry);
        Files.writeString(directory.resolve("protocol.txt"), "Modeled observations, not native gameplay.\n"
                + "8 seed pairs; 32 items per site; 12 rounds; open/low-headroom conditions stay fixed.\n"
                + "Sites separated by 160 or 2048 blocks; production radius remains 20 chunks.\n"
                + "Neutral control maps one RNG draw onto equal chances among eligible current revisions.\n"
                + "The real production selector, inheritance, mutations, revisions, codecs and measured utility execute.\n"
                + "No diversity threshold, distribution target or coefficient tuning.\n"
                + "Fixed cohort replacement is a diagnostic protocol, not production extinction.\nreplay_equal=true\n");
    }

    private java.util.random.RandomGenerator parentRandom(LocalEcosystem ecosystem, ArtifactManager manager,
            WorldPosition origin, Random random, boolean neutral) {
        if (!neutral) return random;
        return new java.util.random.RandomGenerator() {
            @Override public long nextLong() { return random.nextLong(); }
            @Override public double nextDouble() {
                var candidates = ecosystem.pool().candidates(origin, RuntimeSettings.get().inheritanceRadiusChunks(),
                        RuntimeSettings.get().inheritanceOutcomeWindowTicks()).stream().filter(candidate -> {
                    var parent = manager.resolveByStorageKey(candidate.storageKey());
                    return parent.getArtifactSeed() == candidate.seed() && parent.getGenomeRevision() == candidate.genomeRevision();
                }).toList();
                assertFalse(candidates.isEmpty());
                double draw = random.nextDouble();
                int selected = Math.min(candidates.size() - 1, (int) (draw * candidates.size()));
                double total = candidates.stream().mapToDouble(LocalInheritancePool.ParentCandidate::fitness).sum();
                double prefix = candidates.subList(0, selected).stream().mapToDouble(LocalInheritancePool.ParentCandidate::fitness).sum();
                return (prefix + candidates.get(selected).fitness() * .5) / total;
            }
        };
    }

    private Run run(long seed, int separation, boolean neutral) {
        var positions = List.of(new WorldPosition(new UUID(0, 800), 0, 0),
                new WorldPosition(new UUID(0, 800), separation, 0));
        var store = new MemoryStore(); var manager = new ArtifactManager(store);
        var ecosystem = new LocalEcosystem(store, manager);
        var tracker = new ArtifactUsageTracker(); var evaluator = new ArtifactFitnessEvaluator();
        var environment = new EnvironmentPressureEngine();
        var feedback = new ExperienceEvolutionEngine(tracker, evaluator, environment);
        var genetics = new GenomeEvolutionEngine(catalog, new ArtifactMemoryEngine(), null, null, feedback, ScoringMode.PROJECTION_WITH_CACHE);
        var randoms = List.of(new Random(seed), new Random(seed + 1));
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
                double room = headroom(site, generation, false);
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
                var allowed = new HashMap<String, WorldPosition>();
                for (int parentSite = 0; parentSite < cohorts.size(); parentSite++)
                    for (var item : cohorts.get(parentSite)) allowed.put(item.getArtifactStorageKey(), positions.get(parentSite));
                var children = new ArrayList<Artifact>(); var parents = new HashSet<Long>(); var counts = new HashMap<Long, Integer>();
                var localKeys = new HashSet<String>(); for (var item : items) localKeys.add(item.getArtifactStorageKey());
                var health = ecosystem.healthSnapshot(positions.get(site)); int crossSiteParents = 0;
                for (int i = 0; i < COHORT; i++) {
                    var child = ecosystem.createEquipment(new UUID(0, 801), EquipmentArchetype.ELYTRA, positions.get(site),
                            parentRandom(ecosystem, manager, positions.get(site), randoms.get(site), neutral));
                    assertTrue(allowed.containsKey(child.getParentStorageKey()), "Parent must belong to a current cohort");
                    assertTrue(allowed.get(child.getParentStorageKey()).within(positions.get(site), RuntimeSettings.get().inheritanceRadiusChunks()),
                            "The production draw must respect the actual chunk radius and world");
                    var parent = manager.resolveByStorageKey(child.getParentStorageKey());
                    if (!localKeys.contains(parent.getArtifactStorageKey())) crossSiteParents++;
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
                double room = headroom(site, generation, false);
                double response = items.stream().mapToDouble(a -> response(model.express(a.getGenome()), room)).average().orElseThrow();
                double recovery = items.stream().mapToLong(a -> model.express(a.getGenome()).recoveryTicks(60)).average().orElseThrow();
                double fitness = ecosystem.pool().candidates(positions.get(site), RuntimeSettings.get().inheritanceRadiusChunks(),
                        RuntimeSettings.get().inheritanceOutcomeWindowTicks()).stream().filter(c -> c.genomeRevision() == 1)
                        .mapToDouble(LocalInheritancePool.ParentCandidate::fitness).average().orElseThrow();
                double diversity = 1 - counts.values().stream().mapToDouble(count -> Math.pow((double) count / COHORT, 2)).sum();
                assertTrue(Double.isFinite(variance) && diversity >= 0 && diversity <= 1);
                double localFitness = ecosystem.pool().candidates(positions.get(site), RuntimeSettings.get().inheritanceRadiusChunks(),
                        RuntimeSettings.get().inheritanceOutcomeWindowTicks()).stream().filter(c -> c.genomeRevision() == 1 && localKeys.contains(c.storageKey()))
                        .mapToDouble(LocalInheritancePool.ParentCandidate::fitness).average().orElseThrow();
                rows.add(new Row(generation, site, room, mean, variance, response, recovery, fitness, diversity, counts.size(), parents.size(),
                        localFitness, health.effectiveParents(), health.maximumParentShare(), crossSiteParents));
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
    private record Row(int generation, int site, double headroom, double gene, double variance, double response,
                       double recovery, double fitness, double diversity, int founders, int parents,
                       double localFitness, double effectiveParents, double maximumShare, int crossSiteParents) { }
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
