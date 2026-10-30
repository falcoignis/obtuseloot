package obtuseloot.abilities.effects;

import obtuseloot.abilities.*;
import obtuseloot.abilities.genome.GenomeTrait;
import obtuseloot.analytics.NicheDiversityMetrics;
import obtuseloot.artifacts.*;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.ecology.*;
import obtuseloot.evolution.*;
import obtuseloot.persistence.*;
import obtuseloot.reputation.ArtifactReputation;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

/** Generated profiles and actual utility effects; prescribed API inputs, not native ecology. */
class GeneratedUtilityEcologyExperimentTest {
    private static final Set<String> ADDED = Set.of(UtilityAbilityEffectsTest.FOOT, UtilityAbilityEffectsTest.WEATHER,
            UtilityAbilityEffectsTest.HERD, UtilityAbilityEffectsTest.REDSTONE);
    private static final int SITES = 4, ROUNDS = 12, STEP = 20;
    private static final long[] SEEDS = {4822, 5822, 6822, 7822};
    private record Cohort(List<Artifact> items, Map<String, AbilityProfile> profiles) { }
    private record Run(List<String> inputs, List<String> activities, List<String> births, List<String> generations) { }
    private static final class Activity {
        int opportunities, attempts, suppressed, success, noop;
        double benefit; long recovery;
    }

    @Test void smallGeneratedScenarioReplaysAndUsesOnlySupportedEffects() throws Exception {
        var first = run(4822, true, false, true, 4, 2);
        assertEquals(first, run(4822, true, false, true, 4, 2));
        assertEquals(2 * 5 * SITES, first.births.size());
        assertFalse(first.activities.isEmpty());
    }

    @Test @EnabledIfSystemProperty(named = "obtuseloot.experiments", matches = "true")
    void measureGeneratedUtilityCohorts() throws Exception {
        boolean expanded = System.getProperty("obtuseloot.catalog", "expanded").equals("expanded");
        boolean neutral = System.getProperty("obtuseloot.selection", "measured").equals("neutral");
        int generations = Integer.getInteger("obtuseloot.generations", 16);
        int size = Integer.getInteger("obtuseloot.cohort", 8);
        var inputs = new ArrayList<String>(List.of("seed,changing,generation,site,slot,round,kind,available,rich,x"));
        var activities = new ArrayList<String>(List.of("seed,changing,generation,site,slot,item_seed,ability,opportunities,attempts,suppressed,successes,noops,benefit,recovery,result_digest"));
        var births = new ArrayList<String>(List.of("seed,changing,generation,site,slot,storage_key,item_seed,parent_key,parent_seed,parent_revision,lineage,abilities,genome"));
        var population = new ArrayList<String>(List.of("seed,changing,generation,site,eligible,effective_parents,max_parent_share,simpson,distinct_sets,largest_set,lineages,fallbacks,mobility,precision,survival,resonance"));
        for (long seed : SEEDS) for (boolean changing : List.of(false, true)) {
            var result = run(seed, expanded, neutral, changing, generations, size);
            String prefix = seed + "," + changing + ",";
            result.inputs.forEach(row -> inputs.add(prefix + row));
            result.activities.forEach(row -> activities.add(prefix + row));
            result.births.forEach(row -> births.add(prefix + row));
            result.generations.forEach(row -> population.add(prefix + row));
        }
        Path out = Path.of("target/generated-utility-ecology"); Files.createDirectories(out);
        Files.write(out.resolve("inputs.csv"), inputs); Files.write(out.resolve("activities.csv"), activities);
        Files.write(out.resolve("births.csv"), births); Files.write(out.resolve("generations.csv"), population);
    }

    private Run run(long seed, boolean expanded, boolean neutral, boolean changing, int generations, int size) throws Exception {
        var historyField = AbilityDiversityIndex.class.getDeclaredField("recent"); historyField.setAccessible(true);
        @SuppressWarnings("unchecked") var history = (Deque<Object>) historyField.get(AbilityDiversityIndex.instance());
        var previousHistory = new ArrayList<>(history); history.clear();
        try (var fixture = new UtilityAbilityEffectsTest.Fixture()) {
            var full = new AbilityRegistry();
            var registry = new AbilityRegistry(full.registrations().stream()
                    .filter(r -> r.implemented() && (r.liveEffect() == null || r.liveEffect().enabledForSelection()))
                    .filter(r -> expanded || !ADDED.contains(r.template().id()))
                    .map(r -> r.template().id().equals(UtilityAbilityEffectsTest.WEATHER)
                            ? r.withLiveEffect(new WeatherOmenEffect(world -> fixture.cycle)) : r).toList());
            assertEquals(expanded ? 7 : 3, registry.templates().size());
            var resolver = new SeededAbilityResolver(registry, new obtuseloot.memory.ArtifactMemoryEngine(),
                    new obtuseloot.ecosystem.ArtifactEcosystemSelfBalancingEngine(), new obtuseloot.lineage.LineageRegistry(),
                    new obtuseloot.lineage.LineageInfluenceResolver(), null);
            var executor = new AbilityExecutor(registry);
            var store = new MemoryStore(); var manager = new ArtifactManager(store);
            var ecosystem = ReplayEcosystemFixture.create(store, manager, seed);
            var randoms = new ArrayList<Random>(); for (int site = 0; site < SITES; site++) randoms.add(new Random(seed + site));
            var cohorts = new ArrayList<Cohort>(); var births = new ArrayList<String>();
            for (int site = 0; site < SITES; site++) cohorts.add(births(ecosystem, manager, resolver, randoms.get(site), site, 0, size, false, births));
            var inputs = new ArrayList<String>(); var activities = new ArrayList<String>();
            var population = new ArrayList<String>();
            for (int generation = 0; generation < generations; generation++) {
                var counters = new HashMap<String, Activity>(); var digests = new HashMap<String, MessageDigest>();
                for (int round = 0; round < ROUNDS; round++) {
                    if (round > 0) for (int tick = 0; tick < STEP; tick++) ecosystem.advanceTick();
                    fixture.worldTime = ecosystem.pool().gameTick();
                    fixture.weatherTimer = 10_000 - round * STEP; fixture.thunderTimer = 20_000 - round * STEP;
                    for (int site = 0; site < SITES; site++) {
                        boolean rich = (site % 2 == 0) ^ (changing && generation >= generations / 2);
                        fixture.holder = holder(site); fixture.x = site * 64 + generation * 440 + round * 40;
                        fixture.storm = rich; fixture.cycle = rich; fixture.adult = !rich; fixture.age = -1000 + round * STEP;
                        Block block = target(fixture, rich);
                        var cohort = cohorts.get(site);
                        for (int slot = 0; slot < size; slot++) {
                            var item = cohort.items.get(slot); var profile = cohort.profiles.get(item.getArtifactStorageKey());
                            for (int offset = 0; offset < 4; offset++) {
                                int kind = (site + offset) % 4;
                                boolean offered = new SplittableRandom(seed ^ ((long) generation << 32) ^ ((long) site << 24)
                                        ^ ((long) slot << 16) ^ (round * 4L + kind)).nextDouble() < .75;
                                inputs.add(generation + "," + site + "," + slot + "," + round + "," + kind + "," + offered + "," + rich + "," + (long) fixture.x);
                                for (var definition : profile.abilities()) {
                                    if (kind(definition) != kind) continue;
                                    String key = item.getArtifactStorageKey() + "/" + definition.id();
                                    var activity = counters.computeIfAbsent(key, ignored -> new Activity());
                                    var digest = digests.computeIfAbsent(key, ignored -> digest());
                                    if (!offered) continue;
                                    activity.opportunities++;
                                    fixture.sneaking = kind == 0;
                                    Event event = kind == 2 ? fixture.animalEvent() : kind == 3 ? null
                                            : new PlayerInteractEvent(fixture.player, Action.RIGHT_CLICK_BLOCK, null, block,
                                                    kind == 0 && round != 0 ? BlockFace.NORTH : BlockFace.UP, EquipmentSlot.HAND);
                                    var runtime = kind == 3 ? AbilityRuntimeContext.chunkAware(AbilitySource.CHUNK_WORLD_SCAN, (long) fixture.x >> 4, true)
                                            : AbilityRuntimeContext.intentional(AbilitySource.OTHER);
                                    var context = new AbilityEventContext(definition.trigger(), item, item.getItemReputation(), 1,
                                            "generated-utility", runtime, event, true, position(site, generation * 440 + round * 40))
                                            .withGameTick(ecosystem.pool().gameTick());
                                    if (!executor.eligible(definition, context)) continue;
                                    activity.attempts++;
                                    var result = executor.execute(definition, context, 1);
                                    assertNotEquals(AbilityExecutionStatus.FAILED, result.status());
                                    if (result.status() == AbilityExecutionStatus.SUPPRESSED) activity.suppressed++;
                                    else {
                                        var observation = new AbilityOutcomeObservation(context, result, definition, 0);
                                        assertTrue(observation.liveMeasured(), definition.id());
                                        var budget = new TriggerBudgetResolver().resolve(definition, context);
                                        item.getGenomeFitnessHistory().record(new UtilityOutcomeRecord(result.abilityId(), result.mechanic(), result.trigger(), result.status(),
                                                result.outcomeType(), result.meaningfulOutcome(), runtime.intentional(), 1, budget.triggerCost() + budget.evaluationCost(),
                                                context.source(), ecosystem.pool().gameTick(), result.observationKey(), result.performance(), context.genomeRevision()));
                                        ecosystem.observe(observation);
                                        if (result.meaningfulOutcome()) activity.success++; else activity.noop++;
                                        activity.benefit += result.observedAmount();
                                        if (result.performance() != null) activity.recovery += result.performance().recoveryTicks();
                                    }
                                    digest.update((round + ":" + result.status() + ":" + result.observationKey() + ":" + result.observedAmount() + ":" + result.outputText() + "\n")
                                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                }
                            }
                            fixture.messages.clear(); manager.saveItem(item);
                        }
                    }
                }
                var next = new ArrayList<Cohort>();
                for (int site = 0; site < SITES; site++) {
                    var cohort = cohorts.get(site); var sets = new TreeMap<String, Integer>(); var lineages = new HashSet<String>();
                    var occupancy = new TreeMap<String, Integer>(); var taxonomy = new NicheTaxonomy();
                    for (int slot = 0; slot < size; slot++) {
                        var item = cohort.items.get(slot); var profile = cohort.profiles.get(item.getArtifactStorageKey());
                        sets.merge(abilitySet(profile), 1, Integer::sum); lineages.add(item.getLatentLineage());
                        var votes = new EnumMap<MechanicNicheTag, Integer>(MechanicNicheTag.class);
                        for (var definition : profile.abilities()) {
                            taxonomy.nichesFor(definition.mechanic(), definition.trigger()).forEach(tag -> votes.merge(tag, 1, Integer::sum));
                            String key = item.getArtifactStorageKey() + "/" + definition.id();
                            var a = counters.getOrDefault(key, new Activity());
                            String hash = HexFormat.of().formatHex(digests.getOrDefault(key, digest()).digest());
                            activities.add(generation + "," + site + "," + slot + "," + item.getArtifactSeed() + "," + definition.id()
                                    + "," + a.opportunities + "," + a.attempts + "," + a.suppressed + "," + a.success + "," + a.noop + "," + a.benefit + "," + a.recovery + "," + hash);
                        }
                        var dominant = votes.entrySet().stream().max(Comparator.<Map.Entry<MechanicNicheTag, Integer>>comparingInt(Map.Entry::getValue)
                                .thenComparing(e -> -e.getKey().ordinal())).orElseThrow().getKey();
                        occupancy.merge(dominant.name(), 1, Integer::sum);
                    }
                    var origin = position(site, generation * 440 + 440); var health = ecosystem.healthSnapshot(origin);
                    assertTrue(health.finiteSamplerTotal()); assertEquals(0, health.nonFiniteFitnessItems());
                    var newborns = births(ecosystem, manager, resolver, randoms.get(site), site, generation + 1, size, neutral, births);
                    long fallbacks = newborns.items.stream().filter(item -> item.getParentStorageKey() == null).count();
                    population.add(generation + "," + site + "," + health.eligibleParents() + "," + health.effectiveParents() + "," + health.maximumParentShare()
                            + "," + NicheDiversityMetrics.diversity(occupancy).simpsonDiversity() + "," + sets.size() + "," + Collections.max(sets.values())
                            + "," + lineages.size() + "," + fallbacks + "," + mean(cohort.items, GenomeTrait.MOBILITY_AFFINITY) + "," + mean(cohort.items, GenomeTrait.PRECISION_AFFINITY)
                            + "," + mean(cohort.items, GenomeTrait.SURVIVAL_INSTINCT) + "," + mean(cohort.items, GenomeTrait.RESONANCE));
                    next.add(newborns);
                }
                ecosystem.save();
                // Codec round trips retain item-owned anchors/recovery and fitness, unlike fresh offspring.
                for (var cohort : cohorts) for (var item : cohort.items) {
                    var restored = ArtifactProfileCodec.decode(item.getArtifactStorageKey(), ArtifactProfileCodec.encode(item));
                    assertEquals(item.abilityWaypointsSnapshot(), restored.abilityWaypointsSnapshot());
                    assertEquals(item.abilityRecoverySnapshot(), restored.abilityRecoverySnapshot());
                }
                long window = RuntimeSettings.get().inheritanceOutcomeWindowTicks();
                for (long tick = 0; tick <= window; tick++) ecosystem.advanceTick();
                assertTrue(ecosystem.pool().candidates(position(0, generation * 440 + 440), 20, window).isEmpty());
                cohorts = next;
            }
            return new Run(inputs, activities, births, population);
        } finally { history.clear(); history.addAll(previousHistory); }
    }

    private Cohort births(LocalEcosystem ecosystem, ArtifactManager manager, SeededAbilityResolver resolver, Random random,
                          int site, int generation, int size, boolean neutral, List<String> rows) {
        var items = new ArrayList<Artifact>(); var profiles = new LinkedHashMap<String, AbilityProfile>();
        for (int slot = 0; slot < size; slot++) {
            var origin = position(site, generation * 440);
            var settings = RuntimeSettings.get();
            var candidates = ecosystem.pool().selectionCandidates(origin, holder(site), settings.inheritanceRadiusChunks(), settings.inheritanceOutcomeWindowTicks(),
                    c -> { var parent = manager.resolveByStorageKey(c.storageKey()); return parent.getArtifactSeed() == c.seed() && parent.getGenomeRevision() == c.genomeRevision(); });
            RandomGenerator sampler = !neutral ? random : new RandomGenerator() {
                public long nextLong() { return random.nextLong(); }
                public double nextDouble() {
                    assertFalse(candidates.isEmpty());
                    int selected = Math.min(candidates.size() - 1, (int) (random.nextDouble() * candidates.size()));
                    double prefix = candidates.subList(0, selected).stream().mapToDouble(LocalInheritancePool.ParentCandidate::fitness).sum();
                    double total = candidates.stream().mapToDouble(LocalInheritancePool.ParentCandidate::fitness).sum();
                    return (prefix + candidates.get(selected).fitness() * .5) / total;
                }
            };
            var item = ecosystem.createEquipment(holder(site), EquipmentArchetype.DIAMOND_AXE, origin, sampler);
            assertTrue(item.abilityWaypointsSnapshot().isEmpty()); assertTrue(item.abilityRecoverySnapshot().isEmpty());
            assertTrue(item.getGenomeFitnessHistory().revisions().isEmpty());
            if (item.getParentStorageKey() != null) assertTrue(candidates.stream().anyMatch(c -> c.storageKey().equals(item.getParentStorageKey())));
            var profile = resolver.resolve(item, item.getItemReputation());
            assertFalse(profile.abilities().isEmpty());
            profiles.put(item.getArtifactStorageKey(), profile); items.add(item); manager.saveItem(item);
            if (rows != null) rows.add((generation - 1) + "," + site + "," + slot + "," + item.getArtifactStorageKey() + "," + item.getArtifactSeed() + ","
                    + Objects.toString(item.getParentStorageKey(), "") + "," + item.getParentGenomeSeed() + "," + item.getParentGenomeRevision() + ","
                    + item.getLatentLineage() + "," + abilitySet(profile) + "," + item.getGenome().traits().toString().replace(',', ';'));
        }
        return new Cohort(items, profiles);
    }
    private int kind(AbilityDefinition definition) {
        if (definition.id().equals(UtilityAbilityEffectsTest.FOOT)) return 0;
        return switch (definition.trigger()) { case ON_BLOCK_INSPECT -> 1; case ON_ENTITY_INSPECT -> 2; case ON_WORLD_SCAN -> 3; default -> -1; };
    }
    private Block target(UtilityAbilityEffectsTest.Fixture f, boolean rich) {
        var plain = UtilityAbilityEffectsTest.Fixture.api(BlockData.class, (name, args) -> UtilityAbilityEffectsTest.Fixture.unexpected(name));
        return UtilityAbilityEffectsTest.Fixture.api(Block.class, (name, args) -> switch (name) {
            case "getWorld" -> f.world; case "getType" -> rich ? Material.REPEATER : Material.STONE;
            case "getX" -> (int) f.x; case "getY" -> 64; case "getZ" -> 0; case "getLightLevel" -> (byte) (rich ? 12 : 2);
            case "getBlockData" -> rich ? f.block.getBlockData() : plain;
            default -> UtilityAbilityEffectsTest.Fixture.unexpected(name);
        });
    }
    private static UUID holder(int site) { return new UUID(0, 9000 + site); }
    private static WorldPosition position(int site, int distance) { return new WorldPosition(UtilityAbilityEffectsTest.Fixture.WORLD, site * 64 + distance, 0, new HabitatContext(3)); }
    private static String abilitySet(AbilityProfile profile) { return profile.abilities().stream().map(AbilityDefinition::id).sorted().reduce((a,b) -> a + "+" + b).orElseThrow(); }
    private static double mean(List<Artifact> items, GenomeTrait trait) { return items.stream().mapToDouble(item -> item.getGenome().trait(trait)).average().orElseThrow(); }
    private static MessageDigest digest() { try { return MessageDigest.getInstance("SHA-256"); } catch (Exception failure) { throw new IllegalStateException(failure); } }
    private static final class MemoryStore implements PlayerStateStore {
        final Map<String,String> profiles = new HashMap<>(); String ecology;
        public void saveArtifactProfile(Artifact item) { profiles.put(item.getArtifactStorageKey(), ArtifactProfileCodec.encode(item)); }
        public Artifact loadArtifactProfile(String key) { return profiles.containsKey(key) ? ArtifactProfileCodec.decode(key, profiles.get(key)) : null; }
        public void saveEcologyState(String state) { ecology = state; } public String loadEcologyState() { return ecology; }
        public void saveArtifact(UUID id, Artifact item) { throw new UnsupportedOperationException(); } public Artifact loadArtifact(UUID id) { return null; }
        public void saveReputation(UUID id, ArtifactReputation reputation) { throw new UnsupportedOperationException(); } public ArtifactReputation loadReputation(UUID id) { return null; }
    }
}
