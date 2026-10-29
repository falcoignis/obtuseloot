package obtuseloot.ecology;

import obtuseloot.abilities.AbilityDefinition;
import obtuseloot.abilities.AbilityRegistry;
import obtuseloot.abilities.SeededAbilityResolver;
import obtuseloot.artifacts.Artifact;
import obtuseloot.artifacts.EquipmentArchetype;
import obtuseloot.persistence.DatabaseManager;
import obtuseloot.persistence.SqlSchemaManager;
import obtuseloot.persistence.JdbcPlayerStateStore;
import obtuseloot.persistence.PlayerStateStore;
import obtuseloot.artifacts.ArtifactManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/**
 * Measures how often items duplicate, through the live birth path: LocalEcosystem.createEquipment (founders and
 * local inheritance with ordinary mutation) and SeededAbilityResolver over the implemented-only registry the plugin
 * uses. Each item reports a measured outcome so later births can inherit from it. Nothing here tunes generation.
 */
class ItemUniquenessMeasurementTest {
    @TempDir Path temp;

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
    @EnabledIfSystemProperty(named = "obtuseloot.experiments", matches = "true")
    private @interface Experiment { }

    @Experiment
    @Test
    void measureItemDuplication() throws Exception {
        int births = Integer.getInteger("obtuseloot.births", 3000);
        long runSeed = Long.getLong("obtuseloot.uniquenessSeed", 0x554E495155L);
        var database = new DatabaseManager("jdbc:sqlite:" + temp.resolve("uniqueness.db"), null, null);
        try (var connection = database.openConnection()) { new SqlSchemaManager().ensureSchema(connection); }
        PlayerStateStore store = new JdbcPlayerStateStore(database, null, JdbcPlayerStateStore.Dialect.SQLITE);
        // Identity affects founder lineage IDs and therefore lineage drift. Control it as a separate
        // input stream, without consuming birth/parent-choice draws or changing live UUID creation.
        Random identities = new Random(runSeed ^ 0x4944454E54495459L);
        var ecosystem = new LocalEcosystem(store, new ArtifactManager(store), new LocalInheritancePool(),
                () -> "artifact:" + new UUID(identities.nextLong(), identities.nextLong()));
        // -Dobtuseloot.extraAbilities=id,id makes catalog templates selectable as if implemented (identity only:
        // they carry no live effect here). Without it this is exactly the plugin's implemented-only registry.
        Set<String> extra = new TreeSet<>(Arrays.asList(System.getProperty("obtuseloot.extraAbilities", "").split(",")));
        extra.remove("");
        var full = new AbilityRegistry();
        var registry = extra.isEmpty() ? full.implementedOnly() : new AbilityRegistry(full.registrations().stream()
                .filter(r -> (r.implemented() && (r.liveEffect() == null || r.liveEffect().enabledForSelection()))
                        || extra.contains(r.template().id()))
                .toList());
        var resolver = new SeededAbilityResolver(registry, new obtuseloot.memory.ArtifactMemoryEngine(),
                new obtuseloot.ecosystem.ArtifactEcosystemSelfBalancingEngine(), new obtuseloot.lineage.LineageRegistry(),
                new obtuseloot.lineage.LineageInfluenceResolver(), null);

        Random random = new Random(runSeed);
        UUID world = new UUID(0, 77);
        EquipmentArchetype[] archetypes = EquipmentArchetype.values();
        List<String> rows = new ArrayList<>();
        rows.add("index,seed,archetype,generation,name,abilities,genome,affinities,storage_key,lineage,parent_key,parent_seed,parent_revision,observed_amount,legacy_delta");
        List<Map<String, String>> items = new ArrayList<>();
        for (int i = 0; i < births; i++) {
            // Eight areas far apart (beyond the 20-chunk radius), so lineages stay local within each.
            var origin = new WorldPosition(world, (i % 8) * 4_000, 0);
            var archetype = archetypes[random.nextInt(archetypes.length)];
            Artifact item = ecosystem.createEquipment(new UUID(1, i), archetype, origin, random);
            double amount = 1.0D + random.nextDouble() * 9.0D;
            double legacyDelta = random.nextDouble();
            ecosystem.observe(LocalInheritancePoolTest.observation(item, origin, true, amount, legacyDelta, "measured-" + i));
            var profile = resolver.resolve(item, item.getItemReputation());
            Map<String, String> row = new LinkedHashMap<>();
            row.put("seed", Long.toString(item.getArtifactSeed()));
            row.put("archetype", archetype.name());
            row.put("generation", Integer.toString(item.getInheritanceGeneration()));
            row.put("name", item.getDisplayName());
            row.put("abilities", profile.abilities().stream().map(AbilityDefinition::id).sorted().reduce((a, b) -> a + "+" + b).orElse(""));
            row.put("genome", item.getGenome().traits().toString() + item.getGenome().latentTraits());
            row.put("affinities", String.format(Locale.ROOT, "%s/%s/%s/%s/%s/%s", item.getSeedPrecisionAffinity(), item.getSeedBrutalityAffinity(),
                    item.getSeedSurvivalAffinity(), item.getSeedMobilityAffinity(), item.getSeedChaosAffinity(), item.getSeedConsistencyAffinity()));
            row.put("storage_key", item.getArtifactStorageKey());
            row.put("lineage", item.getLatentLineage());
            row.put("parent_key", Objects.toString(item.getParentStorageKey(), ""));
            row.put("parent_seed", Long.toString(item.getParentGenomeSeed()));
            row.put("parent_revision", Long.toString(item.getParentGenomeRevision()));
            row.put("observed_amount", Double.toString(amount));
            row.put("legacy_delta", Double.toString(legacyDelta));
            items.add(row);
            rows.add(i + "," + String.join(",", row.values().stream().map(v -> v.replace(",", ";")).toList()));
        }

        List<String> summary = new ArrayList<>();
        summary.add("births=" + births + " selectable_templates=" + registry.templates().size() + " extra=" + extra
                + " inherited=" + items.stream().filter(r -> !r.get("generation").equals("0")).count());
        Map<String, Function<Map<String, String>, String>> keys = new LinkedHashMap<>();
        keys.put("seed", r -> r.get("seed"));
        keys.put("name", r -> r.get("name"));
        keys.put("abilities", r -> r.get("abilities"));
        keys.put("archetype+abilities", r -> r.get("archetype") + "|" + r.get("abilities"));
        keys.put("name+abilities", r -> r.get("name") + "|" + r.get("abilities"));
        keys.put("genome", r -> r.get("genome"));
        keys.put("affinities", r -> r.get("affinities"));
        keys.put("expressed_identity(name+archetype+abilities+affinities)", r -> r.get("name") + "|"
                + r.get("archetype") + "|" + r.get("abilities") + "|" + r.get("affinities"));
        keys.put("full(name+archetype+abilities+genome+affinities)", r -> r.get("name") + "|" + r.get("archetype") + "|"
                + r.get("abilities") + "|" + r.get("genome") + "|" + r.get("affinities"));
        for (var entry : keys.entrySet()) {
            Map<String, Integer> counts = new HashMap<>();
            for (var r : items) counts.merge(entry.getValue().apply(r), 1, Integer::sum);
            long duplicated = items.stream().filter(r -> counts.get(entry.getValue().apply(r)) > 1).count();
            int largest = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            long equalPairs = counts.values().stream().mapToLong(n -> (long) n * (n - 1) / 2).sum();
            double pairProbability = equalPairs / ((double) births * (births - 1) / 2.0D);
            summary.add(String.format(Locale.ROOT, "%s: distinct=%d items_sharing_a_value=%d (%.2f%%) largest_group=%d equal_pairs=%d pair_probability=%.9g",
                    entry.getKey(), counts.size(), duplicated, 100.0D * duplicated / births, largest, equalPairs, pairProbability));
        }
        Path out = Path.of("target", "item-uniqueness", extra.isEmpty() ? "baseline" : "plus-" + extra.size());
        Files.createDirectories(out);
        Files.write(out.resolve("items.csv"), rows);
        Files.write(out.resolve("summary.txt"), summary);
        summary.forEach(line -> System.out.println("UNIQUENESS " + line));
    }
}
