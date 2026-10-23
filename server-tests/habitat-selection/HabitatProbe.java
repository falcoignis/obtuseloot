package obtuseloot.servertest;

import com.google.gson.GsonBuilder;
import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.*;
import obtuseloot.abilities.genome.*;
import obtuseloot.ecology.*;
import org.bukkit.*;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootTables;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import java.nio.file.*;
import java.util.*;

/** Empty disposable server only. Generates controlled test gear/terrain; never ship this in the plugin. */
public final class HabitatProbe extends JavaPlugin implements Listener {
    private Path root;
    private String last = "", role = "";
    private final Map<String, Artifact> parents = new LinkedHashMap<>();
    private final List<Map<String, Object>> lootEvents = new ArrayList<>();
    private long seed = -22000;
    private boolean collectingLoot;
    private String lootGroup = "";
    private final List<Map<String, Object>> proposals = new ArrayList<>();
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void proposal(obtuseloot.events.ArtifactAirJumpEvent event) {
        var row = habitat(WorldPosition.from(event.getPlayer().getLocation())); row.put("role", role);
        row.put("y", event.getPlayer().getLocation().getY()); row.put("impulse", event.getVelocity().getY()); row.put("recovery", event.getRecoveryTicks()); proposals.add(row);
    }
    private ObtuseLoot core() { return ObtuseLoot.get(); }
    private void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private void write(String name, Object value) throws Exception {
        Path temp = root.resolve(name + ".tmp");
        Files.writeString(temp, new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(value) + "\n");
        Files.move(temp, root.resolve(name), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
    @Override public void onEnable() {
        root = getDataFolder().toPath(); getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskLater(this, () -> {
            try {
                Files.createDirectories(root); require(core() != null && core().isEnabled(), "Core unavailable");
                require(Bukkit.getMinecraftVersion().equals("26.2"), "Wrong Minecraft target");
                if (Files.exists(root.resolve("boot-only"))) {
                    for (String name : List.of("ViaVersion", "ViaBackwards", "PlaceholderAPI", "LuckPerms", "AuraSkills"))
                        require(!getServer().getPluginManager().isPluginEnabled(name), "Unexpected optional plugin " + name);
                    terrain(); sensorGrid(); timings(); write("standalone.json", Map.of("passed", true, "minecraft", Bukkit.getMinecraftVersion()));
                    getServer().shutdown();
                } else if (Files.exists(root.resolve("restart-only"))) {
                    var expected = new YamlConfiguration(); expected.load(root.resolve("expected.yml").toFile());
                    var actual = new YamlConfiguration(); actual.loadFromString(core().getLocalEcosystem().pool().encode());
                    require(Objects.equals(expected.getString("parents"), actual.getConfigurationSection("parents").getValues(true).toString()), "Reproductive evidence changed across restart");
                    for (String name : expected.getStringList("roles")) {
                        Artifact item = core().getArtifactManager().resolveByStorageKey(expected.getString(name + ".key"));
                        require(item != null && item.getArtifactSeed() == expected.getLong(name + ".seed") && item.getGenomeRevision() == expected.getLong(name + ".revision"), "Lost producing genes " + name);
                        require(item.getGenome().traits().toString().equals(expected.getString(name + ".genes")), "Genes changed on restart " + name);
                    }
                    for (int i = 0; i < expected.getInt("children-count"); i++) {
                        String base = "children." + i + "."; var child = core().getArtifactManager().resolveByStorageKey(expected.getString(base + "key"));
                        require(child != null && Objects.equals(child.getParentStorageKey(), expected.getString(base + "parent"))
                                && child.getParentGenomeSeed() == expected.getLong(base + "parent-seed")
                                && child.getParentGenomeRevision() == expected.getLong(base + "parent-revision"), "Lost native child ancestry");
                    }
                    write("restart.json", Map.of("passed", true, "priorGameTick", expected.getLong("game-tick"), "currentGameTick", actual.getLong("game-tick")));
                    getServer().shutdown();
                }
            } catch (Throwable failure) { fail(failure); }
        }, 40);
        getServer().getScheduler().runTaskTimer(this, this::control, 1, 1);
        getServer().getScheduler().runTaskLater(this, () -> fail(new AssertionError("Probe deadline")), 5000);
    }
    private Location at(int site) { return new Location(Bukkit.getWorlds().getFirst(), site + .5, 64, .5); }
    private int site(String name) { return name.startsWith("near") ? 160 : name.startsWith("sky") ? 240 : 0; }
    private Map<String, Object> habitat(WorldPosition position) {
        var result = new LinkedHashMap<String, Object>(); result.put("x", position.x()); result.put("z", position.z());
        result.put("kind", position.habitat() == null ? "unknown" : position.habitat().columnHeightBlocks() == null ? "sky" : "roofed");
        result.put("columnHeightBlocks", position.habitat() == null ? null : position.habitat().columnHeightBlocks()); return result;
    }
    private void roof(int site, int height) {
        World world = at(site).getWorld();
        for (int x = site - 4; x <= site + 4; x++) for (int z = -4; z <= 4; z++) {
            world.getBlockAt(x, 63, z).setType(Material.STONE);
            for (int y = 64; y <= 72; y++) world.getBlockAt(x, y, z).setType(y == height ? Material.STONE : Material.AIR);
        }
    }
    private void terrain() { roof(0, 67); roof(160, 68); roof(240, -1); }
    private void sensorGrid() throws Exception {
        var rows = new ArrayList<Object>();
        for (int site : new int[]{0, 160, 240}) for (double height : new double[]{64, 64.5, 65.1}) {
            var position = at(site); position.setY(height); var row = habitat(WorldPosition.from(position)); row.put("y", height); rows.add(row);
        }
        write("sensor-grid.json", rows);
    }
    private void timings() throws Exception {
        var far = new Location(at(0).getWorld(), 1_000_000, 64, 1_000_000);
        require(!far.getWorld().isChunkLoaded(far.getBlockX() >> 4, far.getBlockZ() >> 4), "Fixture far chunk already loaded");
        require(HabitatContext.capture(far) == null, "Unloaded column fabricated evidence");
        require(!far.getWorld().isChunkLoaded(far.getBlockX() >> 4, far.getBlockZ() >> 4), "Capture loaded a chunk");
        write("unloaded-column.json", Map.of("passed", true));
        var results = new ArrayList<Object>();
        for (int site : new int[]{0, 160, 240}) {
            var location = at(site); for (int i = 0; i < 100; i++) HabitatContext.capture(location);
            long[] nanos = new long[1000];
            for (int i = 0; i < nanos.length; i++) { long start = System.nanoTime(); HabitatContext.capture(location); nanos[i] = System.nanoTime() - start; }
            Arrays.sort(nanos); results.add(Map.of("site", site, "captures", nanos.length, "medianNanos", nanos[500], "p95Nanos", nanos[950], "maxNanos", nanos[999]));
        }
        write("sampling-cost.json", results);
    }
    private void control() {
        try {
            if (!Files.isDirectory(root)) return;
            var requestFile = root.resolve("request.properties"); if (!Files.exists(requestFile)) return;
            var request = new Properties(); try (var in = Files.newInputStream(requestFile)) { request.load(in); }
            String id = request.getProperty("id"); if (id == null || id.equals(last)) return;
            Player player = Bukkit.getPlayerExact("OLHabitatProbe"); if (player == null) return;
            last = id;
            switch (request.getProperty("action")) {
                case "setup" -> setup(player, request.getProperty("role"));
                case "loot" -> { lootGroup = request.getProperty("role", ""); nativeLoot(); }
                case "change" -> change();
                case "finish" -> { persist(player); write("snapshot.json", Map.of("id", id, "finished", true)); getServer().shutdown(); return; }
                case "snapshot" -> { }
                default -> throw new AssertionError("Unknown probe action");
            }
            snapshot(player, id);
        } catch (Throwable failure) { fail(failure); }
    }
    private void setup(Player player, String name) {
        role = name; player.setOp(true); player.setFlying(false); player.setAllowFlight(false); player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear(); player.teleport(at(site(name))); player.setVelocity(new org.bukkit.util.Vector());
        double kinetic = name.endsWith("cheap") ? 0 : 1;
        Artifact selected = null;
        for (int i = 0; i < 100; i++) {
            var item = new Artifact(player.getUniqueId(), "diamond_boots"); item.setArtifactStorageKey(ArtifactStorageKeys.create()); item.setArtifactSeed(seed--);
            var traits = new EnumMap<>(item.getGenome().traits()); traits.put(GenomeTrait.KINETIC_BIAS, kinetic);
            item.setGenome(new ArtifactGenome(item.getArtifactSeed(), traits)); core().getArtifactManager().registerItem(item);
            if (core().getItemAbilityManager().equippedDefinitions(player.getUniqueId(), item).stream().anyMatch(d -> d.id().equals("mobility.double_jump"))) { selected = item; break; }
        }
        require(selected != null, "Missing Double Jump fixture"); parents.put(name, selected);
        var stack = new ItemStack(Material.DIAMOND_BOOTS); core().getArtifactItemStorage().stampMinimalIdentity(stack, selected);
        player.getInventory().setBoots(stack); core().getEquipmentAbilities().reconcile(player);
    }
    private List<Map<String, Object>> samples() throws Exception {
        var yaml = new YamlConfiguration(); yaml.loadFromString(core().getLocalEcosystem().pool().encode());
        var result = new ArrayList<Map<String, Object>>(); var section = yaml.getConfigurationSection("parents");
        for (String parent : section == null ? Set.<String>of() : section.getKeys(false)) {
            String base = "parents." + parent + ".";
            String label = parents.entrySet().stream().filter(e -> e.getValue().getArtifactStorageKey().equals(yaml.getString(base + "key"))).map(Map.Entry::getKey).findFirst().orElse("other");
            var entries = yaml.getConfigurationSection(base + "samples");
            for (String sample : entries.getKeys(false)) {
                String path = base + "samples." + sample + ".";
                var row = new LinkedHashMap<String, Object>(); row.put("role", label); row.put("seed", yaml.getLong(base + "seed")); row.put("revision", yaml.getLong(base + "genome-revision"));
                row.put("habitat", yaml.getString(path + "habitat.kind")); row.put("columnHeightBlocks", yaml.get(path + "habitat.column-height-blocks"));
                row.put("benefit", yaml.getDouble(path + "performance.benefit")); row.put("recovery", yaml.getLong(path + "performance.recovery-ticks")); row.put("tick", yaml.getLong(path + "tick")); result.add(row);
            }
        }
        return result;
    }
    private void snapshot(Player player, String id) throws Exception {
        var value = new LinkedHashMap<String, Object>(); value.put("id", id); value.put("role", role); value.put("playerY", player.getLocation().getY());
        value.put("samples", samples()); value.put("proposals", proposals); value.put("lootEvents", lootEvents); value.put("birthContext", habitat(WorldPosition.from(at(0).add(2, .5, 0))));
        var health = core().getLocalEcosystem().healthSnapshot(WorldPosition.from(at(0).add(2, .5, 0)));
        var settings = obtuseloot.config.RuntimeSettings.get();
        var competition = core().getLocalEcosystem().pool().competitiveEvidence(WorldPosition.from(at(0).add(2, .5, 0)),
                settings.inheritanceRadiusChunks(), settings.inheritanceOutcomeWindowTicks(), candidate -> {
                    var item = core().getArtifactManager().resolveByStorageKey(candidate.storageKey());
                    return item != null && item.getArtifactSeed() == candidate.seed() && item.getGenomeRevision() == candidate.genomeRevision();
                });
        value.put("competition", competition.stream().map(local -> Map.of("role", parents.entrySet().stream()
                .filter(e -> e.getValue().getArtifactStorageKey().equals(local.individual().storageKey())).map(Map.Entry::getKey).findFirst().orElse("other"),
                "fitness", local.individual().fitness(), "attempts", local.individual().attempts())).toList());
        value.put("eligibleParents", health.eligibleParents()); value.put("supportItems", health.competingItems()); value.put("supportAttempts", health.competingAttempts());
        write("snapshot.json", value);
    }
    private void nativeLoot() {
        int before = lootEvents.size(); collectingLoot = true;
        try {
            for (int trial = 0; trial < 8 && lootEvents.size() == before; trial++) {
                var block = at(0).add(2, 0, 0).getBlock(); block.setType(Material.AIR); block.setType(Material.CHEST);
                Chest chest = (Chest) block.getState(); chest.setLootTable(Bukkit.getLootTable(LootTables.END_CITY_TREASURE.getKey())); chest.setSeed(782 + trial); chest.update();
                ((Chest) block.getState()).getBlockInventory().getContents();
            }
        } finally { collectingLoot = false; }
        require(lootEvents.size() > before, "Native loot table did not emit stamped equipment");
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void loot(LootGenerateEvent event) {
        if (!collectingLoot) return;
        require(!event.isPlugin(), "Loot event was plugin-generated");
        for (ItemStack stack : event.getLoot()) if (core().getArtifactItemStorage().isArtifactItem(stack)) {
            Artifact item = core().getArtifactItemStorage().resolve(stack, new UUID(0, 0));
            var row = new LinkedHashMap<String, Object>(); row.put("group", lootGroup); row.put("child", item.getArtifactStorageKey()); row.put("parent", item.getParentStorageKey());
            row.put("parentRole", parents.entrySet().stream().filter(e -> e.getValue().getArtifactStorageKey().equals(item.getParentStorageKey())).map(Map.Entry::getKey).findFirst().orElse("none"));
            row.put("parentSeed", item.getParentGenomeSeed()); row.put("parentRevision", item.getParentGenomeRevision()); row.put("pluginGenerated", event.isPlugin()); row.put("position", habitat(WorldPosition.from(event.getLootContext().getLocation()))); lootEvents.add(row);
        }
    }
    private void change() throws Exception {
        var before = samples(); var block = at(0).getWorld().getBlockAt(2, 67, 0); block.setType(Material.AIR);
        at(0).getWorld().getBlockAt(2, 68, 0).setType(Material.STONE);
        var after = samples(); require(before.equals(after), "Changing the roof rewrote captured evidence");
        write("ceiling-change.json", Map.of("priorSamples", before, "currentBirthContext", habitat(WorldPosition.from(at(0).add(2, .5, 0))), "evidenceUnchanged", true));
    }
    private void persist(Player player) throws Exception {
        player.getInventory().clear(); core().getEquipmentAbilities().reconcile(player); core().getArtifactManager().saveAll(); core().getLocalEcosystem().save();
        var pool = new YamlConfiguration(); pool.loadFromString(core().getLocalEcosystem().pool().encode());
        var expected = new YamlConfiguration(); expected.set("parents", pool.getConfigurationSection("parents").getValues(true).toString()); expected.set("game-tick", pool.getLong("game-tick")); expected.set("roles", new ArrayList<>(parents.keySet()));
        for (var entry : parents.entrySet()) { var item = entry.getValue(); String key = entry.getKey() + "."; expected.set(key + "key", item.getArtifactStorageKey()); expected.set(key + "seed", item.getArtifactSeed()); expected.set(key + "revision", item.getGenomeRevision()); expected.set(key + "genes", item.getGenome().traits().toString()); }
        expected.set("children-count", lootEvents.size());
        for (int i = 0; i < lootEvents.size(); i++) {
            var row = lootEvents.get(i); String base = "children." + i + ".";
            Artifact child = core().getArtifactManager().resolveByStorageKey((String) row.get("child"));
            expected.set(base + "key", child.getArtifactStorageKey()); expected.set(base + "parent", child.getParentStorageKey());
            expected.set(base + "parent-seed", child.getParentGenomeSeed()); expected.set(base + "parent-revision", child.getParentGenomeRevision());
        }
        expected.save(root.resolve("expected.yml").toFile()); write("final-measurements.json", Map.of("samples", samples(), "lootEvents", lootEvents));
    }
    private void fail(Throwable failure) {
        getLogger().log(java.util.logging.Level.SEVERE, "Habitat probe failed", failure);
        try { Files.createDirectories(root); Files.writeString(root.resolve("failure.txt"), failure.toString()); } catch (Exception ignored) { }
        getServer().shutdown();
    }
}
