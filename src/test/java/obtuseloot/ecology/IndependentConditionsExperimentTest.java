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

/** Controlled input experiment, not native play or procedurally selected loadouts. */
class IndependentConditionsExperimentTest {
    private static final int SITES = 4, COHORT = 16, GENERATIONS = 8, ROUNDS = 12, STEP = 20;
    private final AbilityRegistry catalog = new AbilityRegistry();
    private final AbilityDefinition jumpDefinition = definition("mobility.double_jump");
    private final AbilityPerformanceModel jump = catalog.registrationById(jumpDefinition.id()).performanceModel();
    private enum Arm { BOTH_REGULAR, VARIED_REGULAR, VARIED_OPPORTUNITIES }
    private enum Loadout {
        INSIGHT(true, false), JUMP(false, true), BOTH(true, true);
        final boolean insight, jump;
        Loadout(boolean insight, boolean jump) { this.insight = insight; this.jump = jump; }
    }
    private record Conditions(boolean rich, boolean low) { }
    private record Run(List<String> generations, List<String> activities, List<String> births) { }

    @Test void crossedConditionsAndIndependentSchedulesAreReplayable() {
        for (int generation = 0; generation < GENERATIONS; generation++) {
            var cells = new HashSet<Conditions>();
            for (int site = 0; site < SITES; site++) cells.add(conditions(site, generation, true));
            assertEquals(4, cells.size(), "Every phase retains the full target/ceiling factorial");
        }
        for (int site = 0; site < SITES; site++) {
            var beforeTarget = conditions(site, 3, true); var afterTarget = conditions(site, 4, true);
            assertNotEquals(beforeTarget.rich, afterTarget.rich); assertEquals(beforeTarget.low, afterTarget.low);
            var beforeCeiling = conditions(site, 5, true); var afterCeiling = conditions(site, 6, true);
            assertEquals(beforeCeiling.rich, afterCeiling.rich); assertNotEquals(beforeCeiling.low, afterCeiling.low);
        }
        var loadouts = new HashSet<Loadout>(); var offered = new HashSet<Boolean>();
        for (long seed = 1; seed <= 64; seed++) {
            loadouts.add(loadout(seed, Arm.VARIED_REGULAR));
            for (int round = 0; round < ROUNDS; round++) {
                boolean value = offered(seed, round, 0, Arm.VARIED_OPPORTUNITIES);
                assertEquals(value, offered(seed, round, 0, Arm.VARIED_OPPORTUNITIES)); offered.add(value);
            }
        }
        assertEquals(Set.of(Loadout.values()), loadouts); assertEquals(Set.of(true, false), offered);
    }

    @Test void frequentOpportunitiesExposeActualRecoveryAndColumnOnlyContextAliasing() throws Exception {
        try (var f = new InformationFixture()) {
            var low = f.item(1, 0); var high = f.item(2, 1); int lowCount = 0, highCount = 0;
            for (int round = 0; round < ROUNDS; round++) {
                f.x = round;
                if (f.inspect(low, round * STEP).meaningfulOutcome()) lowCount++;
                if (f.inspect(high, round * STEP).meaningfulOutcome()) highCount++;
            }
            assertEquals(6, lowCount); assertEquals(3, highCount);
        }
        var crop = counterexample(true, false); var stone = counterexample(false, false); var pooled = counterexample(true, true);
        assertTrue(crop[1] > crop[0], "Five facts can pay for longer recovery on the controlled crop");
        assertTrue(stone[0] > stone[1], "Identical stone facts favor shorter recovery");
        assertEquals(pooled[0], pooled[2]); assertEquals(pooled[1], pooled[3]);
        var output = Path.of("target", "independent-conditions"); Files.createDirectories(output);
        Files.writeString(output.resolve("context-counterexample.csv"),
                "case,low_precision_weight,high_precision_weight\n"
                + "crop_only," + crop[0] + "," + crop[1] + "\nstone_only," + stone[0] + "," + stone[1]
                + "\npooled_crop_origin," + pooled[0] + "," + pooled[1]
                + "\npooled_stone_origin," + pooled[2] + "," + pooled[3] + "\n");
    }

    @Test void compareCrossedTargetsCeilingsLoadoutsAndOpportunities() throws Exception {
        var generations = new StringBuilder("seed,arm,selection,changing,generation,site,rich,low,precision_mean,precision_variance,kinetic_mean,kinetic_variance,eligible_parents,pool_fitness,effective_parents,max_parent_share,newborn_founder_simpson,newborn_founders,distinct_parents,cross_target_parents,cross_ceiling_parents,founder_births,unobserved_current_items\n");
        var activities = new StringBuilder("seed,arm,selection,changing,generation,site,item_seed,input_seed,revision,loadout,activity,precision,kinetic,opportunities,unavailable,attempts,cooldown_suppressions,successes,benefit_total,benefit_mean,recovery_mean,unique_reports,revision_utility\n");
        var births = new StringBuilder("seed,arm,selection,changing,generation,site,parent_site,parent_seed,parent_revision,parent_loadout,child_seed,child_loadout,founder,inheritance_generation,genome\n");
        for (long seed : new long[]{722, 1722, 2722, 3722}) for (var arm : Arm.values()) for (boolean neutral : List.of(false, true)) {
            var steady = run(seed, arm, neutral, false); var changed = run(seed, arm, neutral, true);
            assertEquals(steady.births.subList(0, SITES * COHORT * 4), changed.births.subList(0, SITES * COHORT * 4), "Paired prefixes must match before any condition changes");
            if (seed == 722 && arm == Arm.VARIED_OPPORTUNITIES && !neutral)
                assertEquals(changed, run(seed, arm, false, true), "Independent state must replay every recorded row");
            for (boolean changing : List.of(false, true)) {
                var result = changing ? changed : steady;
                String prefix = seed + "," + arm + "," + (neutral ? "neutral" : "measured") + "," + changing + ",";
                for (var row : result.generations) generations.append(prefix).append(row).append('\n');
                for (var row : result.activities) activities.append(prefix).append(row).append('\n');
                for (var row : result.births) births.append(prefix).append(row).append('\n');
            }
        }
        var output = Path.of("target", "independent-conditions"); Files.createDirectories(output);
        Files.writeString(output.resolve("generations.csv"), generations); Files.writeString(output.resolve("activities.csv"), activities); Files.writeString(output.resolve("parentage.csv"), births);
        Files.writeString(output.resolve("protocol.txt"), "4 paired seeds; 4 crossed crop/stone x low/sky sites; 16 founders/site; 8 generations.\n"
                + "Target swap at generation 4, ceiling swap at generation 6; paired steady control and exact independent replay.\n"
                + "Three input arms: both abilities/regular opportunities; varied loadouts/regular; varied loadouts/varied opportunities.\n"
                + "12 rounds at 20 ticks; first before explicit revision transition, remaining 11 afterwards.\n"
                + "Loadouts and opportunities use a separate stream keyed to study seed/site/generation/birth slot, aligned across paired runs; no gene input or desired population distribution.\n"
                + "Loadouts are assigned test inputs, not production procedural profiles; armor/slot selection and native jump execution are not exercised.\n"
                + "Material Insight uses the real handler with API doubles; jump response and cooldown are modeled.\n"
                + "Different report targets and reinspections occur; original repeat penalties and actual inspection recovery remain.\n"
                + "Only actual measured successes/no-ops enter production revision fitness and the unchanged local selector. Suppressions/missing abilities remain separate.\n"
                + "Two ceiling classes share target types; every site lies inside the default same-world 20-chunk radius.\n"
                + "Neutral maps one selector draw to equal current eligible-parent chances. Empty breeding pools use the real fresh-founder fallback.\n"
                + "Ordinary mutation, genetics, revision attribution, codecs and evidence expiry execute; owned profiles are retained.\n"
                + "Output/performance describe observed parent cohorts; founder ancestry describes resulting births. Missing mean/utility fields are empty.\n"
                + "Facts, modeled meters, availability, recovery and existing utility are separate; facts are not proof of learning.\n"
                + "Fixed cohorts and scripted conditions are not natural extinction, open-ended niches or ecosystem-health certification.\n"
                + "No production change, coefficient tuning, forced niche, diversity floor or wild mutation.\nreplay_equal=true\n");
    }

    private Run run(long seed, Arm arm, boolean neutral, boolean changing) throws Exception {
        try (var f = new InformationFixture()) {
            var store = new MemoryStore(); var manager = new ArtifactManager(store); var ecosystem = new LocalEcosystem(store, manager);
            var feedback = new ExperienceEvolutionEngine(new ArtifactUsageTracker(), new ArtifactFitnessEvaluator(), new obtuseloot.ecosystem.EnvironmentPressureEngine());
            var genetics = new GenomeEvolutionEngine(catalog, new obtuseloot.memory.ArtifactMemoryEngine(), null, null, feedback, ScoringMode.PROJECTION_WITH_CACHE);
            var randoms = new ArrayList<Random>(); var cohorts = new ArrayList<List<Artifact>>(); var founders = new HashMap<String, Long>(); var inputSeeds = new HashMap<String, Long>();
            for (int site = 0; site < SITES; site++) {
                randoms.add(new Random(seed + site)); var items = new ArrayList<Artifact>();
                for (int i = 0; i < COHORT; i++) {
                    var item = ecosystem.createEquipment(f.holder, EquipmentArchetype.DIAMOND_AXE, position(site, conditions(site, 0, changing)), randoms.get(site));
                    assertNull(item.getParentStorageKey()); var genes = new EnumMap<GenomeTrait, Double>(GenomeTrait.class);
                    for (var trait : GenomeTrait.values()) genes.put(trait, .5);
                    genes.put(GenomeTrait.PRECISION_AFFINITY, (i / 4) / 3.0); genes.put(GenomeTrait.KINETIC_BIAS, (i % 4) / 3.0);
                    item.setGenome(new ArtifactGenome(item.getArtifactSeed(), genes, genes, Set.of()));
                    item.setLatentLineage("founder-" + item.getArtifactSeed()); manager.saveItem(item); items.add(item); founders.put(item.getArtifactStorageKey(), item.getArtifactSeed()); inputSeeds.put(item.getArtifactStorageKey(), inputSeed(seed, site, 0, i));
                }
                cohorts.add(items);
            }
            var rows = new ArrayList<String>(); var activityRows = new ArrayList<String>(); var births = new ArrayList<String>();
            for (int generation = 0; generation < GENERATIONS; generation++) {
                var stats = new HashMap<String, Stats[]>(); var jumpReady = new HashMap<String, Long>();
                for (var cohort : cohorts) for (var item : cohort) stats.put(item.getArtifactStorageKey(), new Stats[]{new Stats(), new Stats()});
                for (int round = 0; round < ROUNDS; round++) {
                    if (round > 0) for (int tick = 0; tick < STEP; tick++) ecosystem.advanceTick();
                    for (int site = 0; site < SITES; site++) {
                        var condition = conditions(site, generation, changing); f.rich = condition.rich;
                        for (var item : cohorts.get(site)) {
                            if (round == 1) { item.setEvolutionPath("strider-formed"); assertTrue(genetics.advance(item)); assertEquals(1, item.getGenomeRevision()); }
                            long schedule = inputSeeds.get(item.getArtifactStorageKey());
                            var loadout = loadout(schedule, arm); var counters = stats.get(item.getArtifactStorageKey());
                            for (int activity = 0; activity < 2; activity++) {
                                if (!offered(schedule, round, activity, arm)) continue;
                                var counter = round == 0 ? new Stats() : counters[activity]; counter.opportunities++;
                                if (!(activity == 0 ? loadout.insight : loadout.jump)) { counter.unavailable++; continue; }
                                counter.attempts++;
                                long tick = ecosystem.pool().gameTick();
                                if (activity == 0) {
                                    boolean newTarget = new Random(schedule ^ (round * 11939L)).nextBoolean();
                                    f.x = site * 80 + (newTarget ? round + 1 : 0);
                                    var context = f.context(item, tick, f.event()).withPosition(new WorldPosition(InformationFixture.WORLD, f.x, 0, habitat(condition)));
                                    var result = f.executor.execute(f.definition, context, 1);
                                    if (result.status() == AbilityExecutionStatus.SUPPRESSED) { counter.suppressed++; assertNull(result.observedAmount()); continue; }
                                    assertEquals(AbilityExecutionStatus.SUCCESS, result.status()); counter.add(result); record(ecosystem, f.definition, context, result);
                                } else {
                                    if (tick < jumpReady.getOrDefault(item.getArtifactStorageKey(), 0L)) { counter.suppressed++; continue; }
                                    var performance = jump.express(item.getGenome()); double meters = response(performance, condition.low ? .0784 : 20);
                                    long recovery = performance.recoveryTicks(60); jumpReady.put(item.getArtifactStorageKey(), Math.addExact(tick, recovery));
                                    var context = new AbilityEventContext(jumpDefinition.trigger(), item, item.getItemReputation(), 1, "modeled-independent-jump",
                                            AbilityRuntimeContext.intentional(AbilitySource.OTHER), null, true).withPosition(position(site, condition)).withGameTick(tick);
                                    var result = AbilityExecutionResult.observed(jumpDefinition, context, AbilityOutcomeType.WORLD_INTERACTION, meters, null, "modeled jump")
                                            .withObservationKey("jump-" + generation + "-" + round).withPerformance(new AbilityPerformanceMeasurement(meters, recovery));
                                    counter.add(result); record(ecosystem, jumpDefinition, context, result);
                                }
                            }
                        }
                    }
                }
                for (var cohort : cohorts) for (var item : cohort) manager.saveItem(item);
                ecosystem.save(); manager = new ArtifactManager(store); ecosystem = new LocalEcosystem(store, manager);
                var sitesByKey = new HashMap<String, Integer>(); for (int site = 0; site < SITES; site++) for (var item : cohorts.get(site)) sitesByKey.put(item.getArtifactStorageKey(), site);
                var next = new ArrayList<List<Artifact>>();
                for (int site = 0; site < SITES; site++) {
                    var condition = conditions(site, generation, changing); var origin = position(site, condition); var items = cohorts.get(site);
                    var eligible = eligible(ecosystem, manager, origin); var health = ecosystem.healthSnapshot(origin);
                    var children = new ArrayList<Artifact>(); var parents = new HashSet<String>(); var counts = new TreeMap<Long,Integer>();
                    int crossTarget = 0, crossCeiling = 0, fresh = 0;
                    for (int i = 0; i < COHORT; i++) {
                        var child = ecosystem.createEquipment(f.holder, EquipmentArchetype.DIAMOND_AXE, origin, parentRandom(ecosystem, manager, origin, randoms.get(site), neutral));
                        assertTrue(child.abilityRecoverySnapshot().isEmpty()); assertTrue(child.getGenomeFitnessHistory().revisions().isEmpty());
                        int parentSite = -1; long founder; String parentSeed = "", parentLoadout = "";
                        if (child.getParentStorageKey() == null) { fresh++; founder = child.getArtifactSeed(); assertEquals(0, child.getInheritanceGeneration()); }
                        else {
                            assertTrue(sitesByKey.containsKey(child.getParentStorageKey())); parentSite = sitesByKey.get(child.getParentStorageKey());
                            var parent = manager.resolveByStorageKey(child.getParentStorageKey()); assertEquals(parent.getArtifactSeed(), child.getParentGenomeSeed());
                            assertEquals(1, child.getParentGenomeRevision()); assertEquals(parent.getInheritanceGeneration()+1, child.getInheritanceGeneration());
                            assertTrue(position(parentSite, conditions(parentSite,generation,changing)).within(origin, RuntimeSettings.get().inheritanceRadiusChunks()));
                            var parentCondition = conditions(parentSite, generation, changing);
                            if (condition.rich != parentCondition.rich) crossTarget++; if (condition.low != parentCondition.low) crossCeiling++;
                            founder = founders.get(parent.getArtifactStorageKey()); parents.add(parent.getArtifactStorageKey()); parentSeed = Long.toString(parent.getArtifactSeed()); parentLoadout = loadout(inputSeeds.get(parent.getArtifactStorageKey()),arm).name();
                        }
                        inputSeeds.put(child.getArtifactStorageKey(), inputSeed(seed, site, generation + 1, i));
                        founders.put(child.getArtifactStorageKey(), founder); counts.merge(founder,1,Integer::sum);
                        births.add(csv(generation, site, parentSite, parentSeed, child.getParentGenomeRevision(), parentLoadout,
                                child.getArtifactSeed(), loadout(inputSeeds.get(child.getArtifactStorageKey()), arm), founder,
                                child.getInheritanceGeneration(), genome(child))); children.add(child);
                    }
                    double precision = mean(items,GenomeTrait.PRECISION_AFFINITY), kinetic = mean(items,GenomeTrait.KINETIC_BIAS);
                    double diversity = 1-counts.values().stream().mapToDouble(n->Math.pow(n/(double)COHORT,2)).sum();
                    long unobserved = items.stream().filter(a->a.getGenomeFitnessHistory().signals(GenomeRevision.capture(a)).isEmpty()).count();
                    rows.add(csv(generation, site, condition.rich, condition.low, precision,
                            variance(items, GenomeTrait.PRECISION_AFFINITY, precision), kinetic,
                            variance(items, GenomeTrait.KINETIC_BIAS, kinetic), eligible.size(), averageFitness(eligible),
                            health.effectiveParents(), health.maximumParentShare(), diversity, counts.size(), parents.size(),
                            crossTarget, crossCeiling, fresh, unobserved));
                    for (var item : items) for (int activity=0;activity<2;activity++) {
                        var stat=stats.get(item.getArtifactStorageKey())[activity];var definition=activity==0?f.definition:jumpDefinition;
                        var utility=item.getGenomeFitnessHistory().signals(GenomeRevision.capture(item)).get(definition.mechanic()+"@"+definition.trigger());
                        activityRows.add(csv(generation, site, item.getArtifactSeed(), inputSeeds.get(item.getArtifactStorageKey()),
                                item.getGenomeRevision(), loadout(inputSeeds.get(item.getArtifactStorageKey()), arm), definition.id(),
                                item.getGenome().trait(GenomeTrait.PRECISION_AFFINITY), item.getGenome().trait(GenomeTrait.KINETIC_BIAS),
                                stat.opportunities, stat.unavailable, stat.attempts, stat.suppressed, stat.successes, stat.benefit,
                                stat.meanBenefit(), stat.meanRecovery(), stat.reports.size(), utility == null ? "" : utility.validatedUtility()));
                    }
                    next.add(children);
                }
                long window=RuntimeSettings.get().inheritanceOutcomeWindowTicks();for(long tick=0;tick<=window;tick++)ecosystem.advanceTick();
                for(int site=0;site<SITES;site++)assertTrue(ecosystem.pool().candidates(position(site,conditions(site,generation,changing)),20,window).isEmpty());
                assertNotNull(store.loadArtifactProfile(cohorts.getFirst().getFirst().getArtifactStorageKey()));cohorts=next;
            }
            return new Run(List.copyOf(rows),List.copyOf(activityRows),List.copyOf(births));
        }
    }

    private double[] counterexample(boolean rich, boolean pooled) throws Exception {
        try(var f=new InformationFixture()) {
            var store=new MemoryStore();var manager=new ArtifactManager(store);var ecosystem=new LocalEcosystem(store,manager);
            var low=f.item(1,0);var high=f.item(2,1);manager.registerItem(low);manager.registerItem(high);
            for(int target=0;target<(pooled?2:1);target++) {
                f.rich=pooled?target==0:rich;f.x=target*80;
                for(int tick=0;tick<100;tick++)ecosystem.advanceTick();
                var position=new WorldPosition(InformationFixture.WORLD,f.x,0,new HabitatContext(3));
                for(var item:List.of(low,high)) {var context=f.context(item,ecosystem.pool().gameTick(),f.event()).withPosition(position);record(ecosystem,f.definition,context,f.executor.execute(f.definition,context,1));}
            }
            var first=ecosystem.pool().candidates(new WorldPosition(InformationFixture.WORLD,0,0,new HabitatContext(3)),20,6000);
            var second=ecosystem.pool().candidates(new WorldPosition(InformationFixture.WORLD,80,0,new HabitatContext(3)),20,6000);
            return new double[]{fitness(first,1),fitness(first,2),fitness(second,1),fitness(second,2)};
        }
    }
    private double fitness(List<LocalInheritancePool.ParentCandidate> candidates, long seed) {
        return candidates.stream().filter(c -> c.seed() == seed).findFirst().orElseThrow().fitness();
    }
    private List<LocalInheritancePool.ParentCandidate> eligible(LocalEcosystem e, ArtifactManager manager, WorldPosition origin) {
        var settings = RuntimeSettings.get();
        return e.pool().competitiveEvidence(origin, settings.inheritanceRadiusChunks(), settings.inheritanceOutcomeWindowTicks(), c -> {
            var item = manager.resolveByStorageKey(c.storageKey());
            return item.getArtifactSeed() == c.seed() && item.getGenomeRevision() == c.genomeRevision();
        }).stream().map(LocalInheritancePool.LocalEvidence::individual)
                .filter(c -> Double.isFinite(c.fitness()) && c.fitness() > 0).toList();
    }
    private RandomGenerator parentRandom(LocalEcosystem e, ArtifactManager manager, WorldPosition origin, Random random, boolean neutral) {
        if (!neutral) return random;
        return new RandomGenerator() {
            public long nextLong() { return random.nextLong(); }
            public double nextDouble() {
                var candidates = eligible(e, manager, origin); assertFalse(candidates.isEmpty());
                int selected = Math.min(candidates.size() - 1, (int) (random.nextDouble() * candidates.size()));
                double total = candidates.stream().mapToDouble(LocalInheritancePool.ParentCandidate::fitness).sum();
                double prefix = candidates.subList(0, selected).stream().mapToDouble(LocalInheritancePool.ParentCandidate::fitness).sum();
                return (prefix + candidates.get(selected).fitness() * .5) / total;
            }
        };
    }
    private void record(LocalEcosystem e, AbilityDefinition definition, AbilityEventContext context, AbilityExecutionResult result) {
        var observation = new AbilityOutcomeObservation(context, result, definition, 0); assertTrue(observation.liveMeasured());
        var budget = new TriggerBudgetResolver().resolve(definition, context);
        context.artifact().getGenomeFitnessHistory().record(new UtilityOutcomeRecord(result.abilityId(), result.mechanic(), result.trigger(),
                result.status(), result.outcomeType(), result.meaningfulOutcome(), true, 1, budget.triggerCost() + budget.evaluationCost(),
                context.source(), e.pool().gameTick(), result.observationKey(), result.performance(), context.genomeRevision()));
        e.observe(observation);
    }
    private static Conditions conditions(int site, int generation, boolean changing) {
        return new Conditions((site % 2 == 0) ^ (changing && generation >= 4), (site < 2) ^ (changing && generation >= 6));
    }
    private static HabitatContext habitat(Conditions c) { return new HabitatContext(c.low ? 3 : null); }
    private static WorldPosition position(int site, Conditions c) {
        return new WorldPosition(InformationFixture.WORLD, site * 80, 0, habitat(c));
    }
    private static long inputSeed(long studySeed, int site, int generation, int slot) {
        return new Random(studySeed ^ 0x55bc742591c45d73L ^ (site * 0x9e3779b97f4a7c15L)
                ^ (generation * 0x632be59bd9b4e019L) ^ (slot * 0x61c8864680b583ebL)).nextLong();
    }
    private static Loadout loadout(long inputSeed, Arm arm) {
        return arm == Arm.BOTH_REGULAR ? Loadout.BOTH : Loadout.values()[new Random(inputSeed ^ 0x61c8864680b583ebL).nextInt(3)];
    }
    private static boolean offered(long inputSeed, int round, int activity, Arm arm) {
        if (arm != Arm.VARIED_OPPORTUNITIES) return true;
        var bias = new Random(inputSeed ^ 0x4f1bbcdc6762c5a3L).nextInt(3);
        double chance = activity == 0 ? (bias == 0 ? .25 : bias == 1 ? .75 : .5) : (bias == 0 ? .75 : bias == 1 ? .25 : .5);
        return new Random(inputSeed ^ (round * 0x9e3779b97f4a7c15L) ^ (activity * 0x632be59bd9b4e019L)).nextDouble() < chance;
    }
    private double response(AbilityPerformance p, double room) {
        double velocity = p.output(.42), height = 0, peak = 0;
        for (int t = 0; t < 6; t++) { height += velocity; peak = Math.max(peak, Math.min(room, height)); velocity = (velocity - .08) * .98; }
        return peak;
    }
    private double mean(List<Artifact> items, GenomeTrait trait) {
        return items.stream().mapToDouble(a -> a.getGenome().trait(trait)).average().orElseThrow();
    }
    private double variance(List<Artifact> items, GenomeTrait trait, double mean) {
        return items.stream().mapToDouble(a -> Math.pow(a.getGenome().trait(trait) - mean, 2)).average().orElseThrow();
    }
    private String averageFitness(List<LocalInheritancePool.ParentCandidate> candidates) {
        return candidates.isEmpty() ? "" : Double.toString(candidates.stream().mapToDouble(LocalInheritancePool.ParentCandidate::fitness).average().orElseThrow());
    }
    private String genome(Artifact item) {
        var result = new StringBuilder();
        for (var trait : GenomeTrait.values()) result.append(trait).append('=').append(Double.toHexString(item.getGenome().trait(trait)))
                .append('/').append(Double.toHexString(item.getGenome().latentTrait(trait))).append('/')
                .append(item.getGenome().activatedLatentTraits().contains(trait)).append(';');
        return result.toString();
    }
    private AbilityDefinition definition(String id) {
        var t = catalog.templateById(id);
        return new AbilityDefinition(t.id(), t.name(), t.family(), t.trigger(), t.mechanic(), "", "", "", "", "", "",
                List.of(), List.of(), t.metadata(), "1", "2", "3", "4", "5");
    }
    private String csv(Object... values) {
        var result = new StringJoiner(",");
        for (var value : values) {
            var text = value == null ? "" : value.toString();
            if (text.contains(",") || text.contains("\n")) throw new IllegalArgumentException("Unexpected CSV delimiter");
            result.add(text);
        }
        return result.toString();
    }
    private static final class Stats {
        long opportunities, unavailable, attempts, suppressed, successes, recovery;
        double benefit;
        final Set<String> reports = new HashSet<>();
        void add(AbilityExecutionResult result) {
            successes++; benefit += result.observedAmount(); recovery += result.performance().recoveryTicks(); reports.add(result.observationKey());
        }
        String meanBenefit() { return successes == 0 ? "" : Double.toString(benefit / successes); }
        String meanRecovery() { return successes == 0 ? "" : Double.toString((double) recovery / successes); }
    }
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
