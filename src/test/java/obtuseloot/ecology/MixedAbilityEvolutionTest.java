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
import java.util.random.RandomGenerator;
import static org.junit.jupiter.api.Assertions.*;

/** Actual inspection handler with Bukkit doubles + modeled jump physics; never a native-health verdict. */
class MixedAbilityEvolutionTest {
    private static final int COHORT = 16, GENERATIONS = 8, SWAP_AT = 4;
    private final AbilityRegistry catalog = new AbilityRegistry();
    private final AbilityPerformanceModel jump = catalog.registrationById("mobility.double_jump").performanceModel();
    private final AbilityDefinition jumpDefinition = definition("mobility.double_jump");

    @Test void compareTwoInheritedTradeoffsAcrossGenerationsAndChangingConditions() throws Exception {
        var csv = new StringBuilder("seed,mode,generation,site,rich,column_height,precision_mean,precision_variance,kinetic_mean,kinetic_variance,facts_mean,insight_recovery,jump_meters_mean,jump_recovery,pool_fitness,founder_simpson,founders,distinct_parents,effective_parents,max_parent_share\n");
        var ancestry = new StringBuilder("seed,mode,generation,site,parent_seed,parent_revision,child_seed,founder,genome\n");
        for (long seed : new long[]{722, 1722, 2722, 3722}) for (boolean neutral : List.of(false, true)) for (boolean swap : List.of(false, true)) {
            String mode = (neutral ? "neutral" : "measured") + (swap ? "_swap" : "_steady");
            var result = run(seed, neutral, swap);
            if (seed == 722 && !neutral && swap) assertEquals(result, run(seed, false, true), "Independent replay must match");
            for (String row : result.rows) csv.append(seed).append(',').append(mode).append(',').append(row).append('\n');
            for (String birth : result.births) ancestry.append(seed).append(',').append(mode).append(',').append(birth).append('\n');
        }
        var output = Path.of("target", "mixed-ability-evolution"); Files.createDirectories(output);
        Files.writeString(output.resolve("generations.csv"), csv);
        Files.writeString(output.resolve("parentage.csv"), ancestry);
        Files.writeString(output.resolve("protocol.txt"), "4 seed pairs; 16 items/site; 8 generations; sites 160 blocks apart inside the unchanged 20-chunk radius.\n"
                + "Steady and swapped at generation 4; rich crop/3-block column vs plain stone/sky.\n"
                + "Two-dimensional 4x4 founder precision/kinetic grid; other genes .5.\n"
                + "One observation/ability before evolution, three afterwards, 100-tick gaps.\n"
                + "Actual Material Insight handler through Bukkit API doubles; jump six-tick physics capped at .0784 or 20 meters.\n"
                + "Production measured classifier, current-revision habitat selector, ordinary inheritance/mutation, codecs and evidence expiry.\n"
                + "Neutral control remaps the same selector draw to equal eligible-current-parent chances.\n"
                + "Physical item UUIDs and names excluded from trace; seeds, revisions and all expressed/latent/activated genes retained.\n"
                + "Newborns have no recovery or fitness; old owned profiles retained after evidence expiry.\n"
                + "Facts are server-delivered properties, not player learning. Meters and facts are different units; combined existing fitness is not balanced subjective value.\n"
                + "Fixed cohorts, scheduled opportunities and two prescribed contexts do not establish natural niches, extinction or ecosystem health.\n"
                + "No fitness coefficient changes, distribution thresholds, quotas or wild mutation.\nreplay_equal=true\n");
    }
    private Run run(long seed, boolean neutral, boolean swap) throws Exception {
        try (var f = new InformationFixture()) {
            var store = new MemoryStore(); var manager = new ArtifactManager(store); var ecosystem = new LocalEcosystem(store, manager);
            var tracker = new ArtifactUsageTracker();
            var feedback = new ExperienceEvolutionEngine(tracker, new ArtifactFitnessEvaluator(), new obtuseloot.ecosystem.EnvironmentPressureEngine());
            var genetics = new GenomeEvolutionEngine(catalog, new obtuseloot.memory.ArtifactMemoryEngine(), null, null, feedback, ScoringMode.PROJECTION_WITH_CACHE);
            var randoms = List.of(new Random(seed), new Random(seed + 1));
            var cohorts = new ArrayList<List<Artifact>>(); var founders = new HashMap<String, Long>();
            for (int site = 0; site < 2; site++) {
                var items = new ArrayList<Artifact>();
                for (int i = 0; i < COHORT; i++) {
                    var item = ecosystem.createEquipment(f.holder, EquipmentArchetype.DIAMOND_AXE, position(site, site == 0), randoms.get(site));
                    assertNull(item.getParentStorageKey());
                    var genes = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
                    for (var trait : GenomeTrait.values()) genes.put(trait, .5);
                    genes.put(GenomeTrait.PRECISION_AFFINITY, (i / 4) / 3.0); genes.put(GenomeTrait.KINETIC_BIAS, (i % 4) / 3.0);
                    item.setGenome(new ArtifactGenome(item.getArtifactSeed(), genes, genes, Set.of()));
                    item.setLatentLineage("founder-" + item.getArtifactSeed()); manager.saveItem(item);
                    items.add(item); founders.put(item.getArtifactStorageKey(), item.getArtifactSeed());
                }
                cohorts.add(items);
            }
            var rows = new ArrayList<String>(); var births = new ArrayList<String>();
            for (int generation = 0; generation < GENERATIONS; generation++) {
                var measured = new ArrayList<List<Sample>>(); measured.add(new ArrayList<>()); measured.add(new ArrayList<>());
                for (int attempt = 0; attempt < 4; attempt++) {
                    if (attempt > 0) for (int tick = 0; tick < 100; tick++) ecosystem.advanceTick();
                    for (int site = 0; site < 2; site++) {
                        boolean rich = (site == 0) ^ (swap && generation >= SWAP_AT);
                        f.rich = rich; f.x = site * 160 + attempt;
                        var position = position(site, rich);
                        for (var item : cohorts.get(site)) {
                            if (attempt == 1) { item.setEvolutionPath("strider-formed"); assertTrue(genetics.advance(item)); assertEquals(1, item.getGenomeRevision()); }
                            var context = f.context(item, ecosystem.pool().gameTick(), f.event()).withPosition(position);
                            var information = f.executor.execute(f.definition, context, 1);
                            assertEquals(AbilityExecutionStatus.SUCCESS, information.status());
                            record(item, ecosystem, f.definition, context, information);
                            double response = response(jump.express(item.getGenome()), rich ? .0784 : 20);
                            var jumpContext = new AbilityEventContext(jumpDefinition.trigger(), item, item.getItemReputation(), 1,
                                    "modeled-mixed-jump", AbilityRuntimeContext.intentional(AbilitySource.OTHER), null, true)
                                    .withPosition(position).withGameTick(ecosystem.pool().gameTick());
                            var movement = AbilityExecutionResult.observed(jumpDefinition, jumpContext, AbilityOutcomeType.WORLD_INTERACTION,
                                    response, null, "modeled jump").withObservationKey("jump-" + generation + "-" + attempt)
                                    .withPerformance(new AbilityPerformanceMeasurement(response, jump.express(item.getGenome()).recoveryTicks(60)));
                            record(item, ecosystem, jumpDefinition, jumpContext, movement);
                            if (attempt > 0) measured.get(site).add(new Sample(information.performance(), movement.performance()));
                            manager.saveItem(item);
                        }
                    }
                }
                ecosystem.save(); manager = new ArtifactManager(store); ecosystem = new LocalEcosystem(store, manager);
                var allowed = new HashSet<String>(); for (var cohort : cohorts) for (var item : cohort) allowed.add(item.getArtifactStorageKey());
                var next = new ArrayList<List<Artifact>>();
                for (int site = 0; site < 2; site++) {
                    boolean rich = (site == 0) ^ (swap && generation >= SWAP_AT); var origin = position(site, rich);
                    var health = ecosystem.healthSnapshot(origin); var parents = new HashSet<Long>(); var counts = new TreeMap<Long, Integer>();
                    var children = new ArrayList<Artifact>();
                    for (int i = 0; i < COHORT; i++) {
                        // Pooled selection keeps this historical baseline and its neutral list aligned; holder context is in IndependentConditionsExperimentTest.
                        var child = ecosystem.createEquipment(f.holder, EquipmentArchetype.DIAMOND_AXE, origin,
                                parentRandom(ecosystem, manager, origin, randoms.get(site), neutral), null);
                        assertTrue(allowed.contains(child.getParentStorageKey()));
                        var parent = manager.resolveByStorageKey(child.getParentStorageKey());
                        assertEquals(parent.getArtifactSeed(), child.getParentGenomeSeed()); assertEquals(1, child.getParentGenomeRevision());
                        assertEquals(generation + 1, child.getInheritanceGeneration()); assertTrue(child.getGenomeFitnessHistory().revisions().isEmpty());
                        assertTrue(child.abilityRecoverySnapshot().isEmpty(), "Recovery is item state, not inherited performance");
                        assertTrue(parent.abilityReadyAt(f.definition.id()) > ecosystem.pool().gameTick());
                        assertEquals(2, parent.getGenomeFitnessHistory().revisions().size());
                        long founder = founders.get(parent.getArtifactStorageKey()); founders.put(child.getArtifactStorageKey(), founder);
                        parents.add(parent.getArtifactSeed()); counts.merge(founder, 1, Integer::sum);
                        var birth = new StringBuilder().append(generation).append(',').append(site).append(',').append(parent.getArtifactSeed())
                                .append(',').append(child.getParentGenomeRevision()).append(',').append(child.getArtifactSeed()).append(',').append(founder).append(',');
                        for (var trait : GenomeTrait.values()) birth.append(trait).append('=').append(Double.toHexString(child.getGenome().trait(trait)))
                                .append('/').append(Double.toHexString(child.getGenome().latentTraits().get(trait))).append('/')
                                .append(child.getGenome().activatedLatentTraits().contains(trait)).append(';');
                        births.add(birth.toString()); children.add(child);
                    }
                    var items = cohorts.get(site); double precision = mean(items, GenomeTrait.PRECISION_AFFINITY), kinetic = mean(items, GenomeTrait.KINETIC_BIAS);
                    var samples = measured.get(site);
                    double fitness = eligible(ecosystem, manager, origin).stream().mapToDouble(LocalInheritancePool.ParentCandidate::fitness).average().orElseThrow();
                    double diversity = 1 - counts.values().stream().mapToDouble(n -> Math.pow(n / (double) COHORT, 2)).sum();
                    rows.add(String.format(Locale.ROOT, "%d,%d,%s,%s,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%.8f,%d,%d,%.8f,%.8f",
                            generation, site, rich, rich ? "3" : "sky", precision, variance(items, GenomeTrait.PRECISION_AFFINITY, precision),
                            kinetic, variance(items, GenomeTrait.KINETIC_BIAS, kinetic), samples.stream().mapToDouble(s -> s.information.benefit()).average().orElseThrow(),
                            samples.stream().mapToLong(s -> s.information.recoveryTicks()).average().orElseThrow(), samples.stream().mapToDouble(s -> s.jump.benefit()).average().orElseThrow(),
                            samples.stream().mapToLong(s -> s.jump.recoveryTicks()).average().orElseThrow(), fitness, diversity, counts.size(), parents.size(), health.effectiveParents(), health.maximumParentShare()));
                    next.add(children);
                }
                long window = RuntimeSettings.get().inheritanceOutcomeWindowTicks(); for (long tick = 0; tick <= window; tick++) ecosystem.advanceTick();
                for (int site = 0; site < 2; site++) assertTrue(ecosystem.pool().candidates(position(site, site == 0), 20, window).isEmpty());
                assertNotNull(store.loadArtifactProfile(cohorts.getFirst().getFirst().getArtifactStorageKey()), "Owned profiles must survive evidence expiry");
                cohorts = next;
            }
            return new Run(List.copyOf(rows), List.copyOf(births));
        }
    }
    private List<LocalInheritancePool.ParentCandidate> eligible(LocalEcosystem ecosystem, ArtifactManager manager, WorldPosition origin) {
        var settings = RuntimeSettings.get();
        return ecosystem.pool().competitiveEvidence(origin, settings.inheritanceRadiusChunks(), settings.inheritanceOutcomeWindowTicks(), c -> {
            var item = manager.resolveByStorageKey(c.storageKey()); return item.getArtifactSeed() == c.seed() && item.getGenomeRevision() == c.genomeRevision();
        }).stream().map(LocalInheritancePool.LocalEvidence::individual).filter(c -> Double.isFinite(c.fitness()) && c.fitness() > 0).toList();
    }
    private RandomGenerator parentRandom(LocalEcosystem ecosystem, ArtifactManager manager, WorldPosition origin, Random random, boolean neutral) {
        if (!neutral) return random;
        return new RandomGenerator() {
            public long nextLong() { return random.nextLong(); }
            public double nextDouble() {
                var candidates = eligible(ecosystem, manager, origin); assertFalse(candidates.isEmpty());
                int selected = Math.min(candidates.size() - 1, (int) (random.nextDouble() * candidates.size()));
                double total = candidates.stream().mapToDouble(LocalInheritancePool.ParentCandidate::fitness).sum();
                double prefix = candidates.subList(0, selected).stream().mapToDouble(LocalInheritancePool.ParentCandidate::fitness).sum();
                return (prefix + candidates.get(selected).fitness() * .5) / total;
            }
        };
    }
    private void record(Artifact item, LocalEcosystem ecosystem, AbilityDefinition definition, AbilityEventContext context, AbilityExecutionResult result) {
        var observation = new AbilityOutcomeObservation(context, result, definition, 0); assertTrue(observation.liveMeasured());
        var budget = new TriggerBudgetResolver().resolve(definition, context);
        item.getGenomeFitnessHistory().record(new UtilityOutcomeRecord(result.abilityId(), result.mechanic(), result.trigger(), result.status(), result.outcomeType(),
                result.meaningfulOutcome(), true, 1, budget.triggerCost() + budget.evaluationCost(), context.source(), ecosystem.pool().gameTick(),
                result.observationKey(), result.performance(), context.genomeRevision()));
        ecosystem.observe(observation);
    }
    private WorldPosition position(int site, boolean rich) { return new WorldPosition(InformationFixture.WORLD, site * 160, 0, new HabitatContext(rich ? 3 : null)); }
    private double mean(List<Artifact> items, GenomeTrait trait) { return items.stream().mapToDouble(a -> a.getGenome().trait(trait)).average().orElseThrow(); }
    private double variance(List<Artifact> items, GenomeTrait trait, double mean) { return items.stream().mapToDouble(a -> Math.pow(a.getGenome().trait(trait) - mean, 2)).average().orElseThrow(); }
    private double response(AbilityPerformance expressed, double room) {
        double velocity = expressed.output(.42), height = 0, peak = 0;
        for (int tick = 0; tick < 6; tick++) { height += velocity; peak = Math.max(peak, Math.min(room, height)); velocity = (velocity - .08) * .98; }
        return peak;
    }
    private AbilityDefinition definition(String id) {
        var t = catalog.templateById(id); return new AbilityDefinition(t.id(), t.name(), t.family(), t.trigger(), t.mechanic(), "", "", "", "", "", "",
                List.of(), List.of(), t.metadata(), "1", "2", "3", "4", "5");
    }
    private record Sample(AbilityPerformanceMeasurement information, AbilityPerformanceMeasurement jump) { }
    private record Run(List<String> rows, List<String> births) { }
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
