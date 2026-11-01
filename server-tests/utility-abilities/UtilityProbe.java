package obtuseloot.servertest;

import com.google.gson.GsonBuilder;
import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.*;
import obtuseloot.abilities.genome.*;
import org.bukkit.*;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.*;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/** Generates synthetic gear, animals, circuits and terrain only in an empty disposable server. Never deploy this probe. */
public final class UtilityProbe extends JavaPlugin implements Listener {
    private static final String NAME = "OLUtilityProbe";
    private static final String FOOTPRINT = "mobility.footprint_memory", WEATHER = "survival.weather_omen",
            HERD = "survival.herd_instinct", REDSTONE = "engineering.redstone_sympathy";
    private static final List<String> ABILITIES = List.of(FOOTPRINT, WEATHER, HERD, REDSTONE);
    private static final List<String> WEATHER_ROLES = List.of("weather-low", "weather-high", "weather-forced",
            "weather-storm", "weather-paused", "weather-permission");
    private static final List<EquipmentArchetype> HELD = List.of(EquipmentArchetype.DIAMOND_AXE, EquipmentArchetype.DIAMOND_SWORD,
            EquipmentArchetype.IRON_AXE, EquipmentArchetype.IRON_SWORD, EquipmentArchetype.NETHERITE_AXE, EquipmentArchetype.NETHERITE_SWORD);
    private Path root; private String last = "", role = "", ability = ""; private Artifact current; private Entity animal;
    private final Map<String, Artifact> items = new LinkedHashMap<>();
    private final Map<String, Object> fixtures = new LinkedHashMap<>();
    private final List<Map<String, Object>> interactions = new ArrayList<>();
    private long seed = -41000; private PermissionAttachment permission;
    private ObtuseLoot core() { return ObtuseLoot.get(); }
    private void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private void write(String name, Object value) throws Exception {
        var file = root.resolve(name + ".tmp"); Files.writeString(file, new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(value));
        Files.move(file, root.resolve(name), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
    @Override public void onEnable() {
        root = getDataFolder().toPath(); getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskLater(this, () -> {
            try {
                Files.createDirectories(root); require(Bukkit.getMinecraftVersion().equals("26.2"), "Wrong target");
                if (Files.exists(root.resolve("boot-only"))) {
                    optionalAbsent(); var registry = core().getItemAbilityManager().registry();
                    for (var id : ABILITIES) require(registry.registrationById(id) != null && registry.registrationById(id).liveEffect() != null, "Missing live effect " + id);
                    var world = Bukkit.getWorlds().getFirst();
                    write("standalone.json", Map.of("passed", true, "minecraft", Bukkit.getMinecraftVersion(),
                            "weatherCycleRule", String.valueOf(world.getGameRuleValue(GameRule.DO_WEATHER_CYCLE))));
                    getServer().shutdown();
                } else if (Files.exists(root.resolve("restart-only"))) {
                    optionalAbsent(); var expected = new YamlConfiguration(); expected.load(root.resolve("expected.yml").toFile());
                    var item = core().getArtifactManager().resolveByStorageKey(expected.getString("key"));
                    require(item.getArtifactSeed() == expected.getLong("seed"), "Lost identity");
                    require(String.valueOf(item.abilityWaypoint(FOOTPRINT)).equals(expected.getString("waypoint")), "Waypoint changed on restart");
                    require(item.getGenome().traits().toString().equals(expected.getString("genes")), "Genes changed on restart");
                    var copy = obtuseloot.persistence.ArtifactProfileCodec.decode(item.getArtifactStorageKey(),
                            obtuseloot.persistence.ArtifactProfileCodec.encode(item));
                    require(Objects.equals(copy.abilityWaypoint(FOOTPRINT), item.abilityWaypoint(FOOTPRINT)), "Codec round trip lost waypoint");
                    write("restart.json", Map.of("passed", true, "waypoint", String.valueOf(item.abilityWaypoint(FOOTPRINT)),
                            "seed", item.getArtifactSeed()));
                    getServer().shutdown();
                }
            } catch (Throwable failure) { fail(failure); }
        }, 1);
        getServer().getScheduler().runTaskTimer(this, this::control, 1, 1);
        getServer().getScheduler().runTaskLater(this, () -> fail(new AssertionError("Probe deadline")), 9000);
    }
    private void optionalAbsent() { for (String name : List.of("ViaVersion", "ViaBackwards", "LuckPerms", "PlaceholderAPI", "AuraSkills")) require(!Bukkit.getPluginManager().isPluginEnabled(name), "Unexpected optional plugin " + name); }
    private boolean probe(Player player) { return player.getName().equals(NAME); }
    private void platform(World world, int fromX, int toX) {
        for (int x = fromX; x <= toX; x++) for (int z = -3; z <= 3; z++) {
            world.getBlockAt(x, 63, z).setType(Material.STONE);
            for (int y = 64; y <= 70; y++) world.getBlockAt(x, y, z).setType(Material.AIR);
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void protectBlock(PlayerInteractEvent event) {
        if (!probe(event.getPlayer())) return;
        if (role.equals("redstone-deny-block")) event.setUseInteractedBlock(Event.Result.DENY);
        if (role.equals("redstone-deny-item")) event.setUseItemInHand(Event.Result.DENY);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void protectEntity(PlayerInteractEntityEvent event) { if (probe(event.getPlayer()) && role.equals("herd-deny")) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void protectEntityAt(PlayerInteractAtEntityEvent event) { if (probe(event.getPlayer()) && role.equals("herd-deny")) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void blockRow(PlayerInteractEvent event) {
        if (!probe(event.getPlayer())) return;
        var row = row("block:" + event.getClass().getSimpleName(), event.getPlayer(), String.valueOf(event.getHand()), event.isCancelled());
        row.put("action", event.getAction().name()); row.put("face", String.valueOf(event.getBlockFace()));
        row.put("blockUse", event.useInteractedBlock().name()); row.put("itemUse", event.useItemInHand().name());
        row.put("blockBefore", event.getClickedBlock() == null ? null : event.getClickedBlock().getBlockData().getAsString()); interactions.add(row);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void entityRow(PlayerInteractEntityEvent event) {
        if (probe(event.getPlayer())) interactions.add(row("entity:" + event.getClass().getSimpleName(), event.getPlayer(), String.valueOf(event.getHand()), event.isCancelled()));
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void entityAtRow(PlayerInteractAtEntityEvent event) {
        if (probe(event.getPlayer())) interactions.add(row("entityAt:" + event.getClass().getSimpleName(), event.getPlayer(), String.valueOf(event.getHand()), event.isCancelled()));
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void moveRow(PlayerMoveEvent event) {
        if (!probe(event.getPlayer()) || event.getTo() == null) return;
        int from = event.getFrom().getBlockX() >> 4, to = event.getTo().getBlockX() >> 4;
        if (from == to) return;
        var row = row("move:" + event.getClass().getSimpleName(), event.getPlayer(), "none", event.isCancelled());
        row.put("fromChunk", from); row.put("toChunk", to); interactions.add(row);
    }
    private Map<String, Object> row(String kind, Player player, String hand, boolean cancelled) {
        var row = new LinkedHashMap<String, Object>(); row.put("role", role); row.put("kind", kind); row.put("hand", hand);
        row.put("cancelled", cancelled); row.put("sneaking", player.isSneaking()); row.put("tick", core().getLocalEcosystem().pool().gameTick()); return row;
    }
    private void control() {
        try {
            var file = root.resolve("request.properties"); if (!Files.exists(file)) return;
            var request = new Properties(); try (var input = Files.newInputStream(file)) { request.load(input); }
            String id = request.getProperty("id"); if (id == null || id.equals(last)) return;
            var player = Bukkit.getPlayerExact(NAME); if (player == null) return; last = id;
            switch (request.getProperty("action")) {
                case "setup" -> setup(player, request.getProperty("role"));
                case "offhand" -> { var stack = player.getInventory().getItemInMainHand().clone(); player.getInventory().setItemInOffHand(stack); player.getInventory().setItemInMainHand(new ItemStack(Material.AIR)); core().getEquipmentAbilities().reconcile(player); }
                case "goto" -> {
                    int x = Integer.parseInt(request.getProperty("x")); var world = player.getWorld();
                    platform(world, x - 3, x + 4); world.getBlockAt(x + 2, 64, 0).setType(Material.STONE);
                    player.teleport(new Location(world, x + .5, 64, .5, -90, 0));
                }
                case "permit" -> permission.setPermission("obtuseloot.ability." + ability, true);
                case "snapshot" -> { }
                case "finish" -> {
                    var item = items.get("fp-high"); require(item != null && item.abilityWaypoint(FOOTPRINT) != null, "No persisted waypoint fixture");
                    core().getArtifactManager().saveItem(item); core().getLocalEcosystem().save();
                    var expected = new YamlConfiguration(); expected.set("key", item.getArtifactStorageKey()); expected.set("seed", item.getArtifactSeed());
                    expected.set("genes", item.getGenome().traits().toString()); expected.set("waypoint", String.valueOf(item.abilityWaypoint(FOOTPRINT)));
                    expected.save(root.resolve("expected.yml").toFile()); snapshot(id); getServer().shutdown(); return;
                }
                default -> throw new AssertionError("Unknown request");
            }
            snapshot(id);
        } catch (Throwable failure) { fail(failure); }
    }
    private void setup(Player player, String name) {
        role = name; player.setOp(true); player.setGameMode(GameMode.SURVIVAL); player.setFlying(false); player.setAllowFlight(false);
        ability = name.startsWith("fp-") ? FOOTPRINT : name.startsWith("weather-") ? WEATHER : name.startsWith("herd-") ? HERD : REDSTONE;
        var trait = switch (ability) { case FOOTPRINT -> GenomeTrait.MOBILITY_AFFINITY; case WEATHER -> GenomeTrait.SURVIVAL_INSTINCT;
            case HERD -> GenomeTrait.RESONANCE; default -> GenomeTrait.PRECISION_AFFINITY; };
        double gene = name.endsWith("-low") ? 0 : 1;
        if (permission != null && permission.getPermissible() == player) player.removeAttachment(permission); permission = player.addAttachment(this);
        for (var id : ABILITIES) permission.setPermission("obtuseloot.ability." + id, !(id.equals(ability) && name.endsWith("-permission")));
        player.getInventory().clear(); var world = player.getWorld();
        if (animal != null) { animal.remove(); animal = null; }
        platform(world, -5, 6); world.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
        double startX = .5;
        if (ability.equals(WEATHER)) {
            int index = WEATHER_ROLES.indexOf(name); require(index >= 0, "Unknown weather role"); int start = 32 * index;
            platform(world, start - 3, start + 40); startX = start + .5;
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, !name.equals("weather-paused"));
            boolean storm = name.equals("weather-storm"); world.setStorm(storm); world.setThundering(storm);
            world.setWeatherDuration(12000); world.setThunderDuration(24000);
            world.setClearWeatherDuration(name.equals("weather-forced") ? 6000 : 0);
        }
        if (ability.equals(HERD)) animal = world.spawn(new Location(world, 2.5, 64, .5), Cow.class, cow -> {
            cow.setAI(false); cow.setSilent(true); cow.setPersistent(true); if (name.equals("herd-young")) cow.setBaby(); else cow.setAdult(); });
        if (ability.equals(REDSTONE)) world.getBlockAt(2, 64, 0).setBlockData(Bukkit.createBlockData(switch (name) {
            case "redstone-lever" -> "minecraft:lever[face=floor,facing=east,powered=false]";
            case "redstone-repeater" -> "minecraft:repeater[delay=1,facing=east,locked=false,powered=false]";
            case "redstone-wire" -> "minecraft:redstone_wire";
            case "redstone-stone" -> "minecraft:stone";
            default -> "minecraft:piston[extended=false,facing=up]";
        }), false);
        player.teleport(new Location(world, startX, 64, .5, -90, 0));
        current = null; int tries = 0; String archetypeName = null;
        var registry = core().getItemAbilityManager().registry();
        search:
        for (var archetype : HELD) for (int i = 0; i < 400; i++) {
            tries++;
            var item = new Artifact(player.getUniqueId(), archetype); item.setArtifactStorageKey(ArtifactStorageKeys.create()); item.setArtifactSeed(seed--);
            var traits = new EnumMap<>(item.getGenome().traits()); traits.put(trait, gene); item.setGenome(new ArtifactGenome(item.getArtifactSeed(), traits));
            core().getArtifactManager().registerItem(item);
            var live = core().getItemAbilityManager().equippedDefinitions(player.getUniqueId(), item).stream()
                    .filter(d -> registry.registrationById(d.id()) != null && registry.registrationById(d.id()).liveEffect() != null)
                    .map(d -> d.id()).collect(Collectors.toSet());
            if (live.equals(Set.of(ability))) { current = item; archetypeName = archetype.name(); break search; }
        }
        require(current != null, "No single-ability fixture for " + name + " after " + tries);
        fixtures.put(name, Map.of("archetype", archetypeName, "tries", tries, "seed", current.getArtifactSeed(), "gene", gene, "trait", trait.name()));
        items.put(role, current);
        var stack = new ItemStack(Material.valueOf(archetypeName)); core().getArtifactItemStorage().stampMinimalIdentity(stack, current);
        player.getInventory().setItemInMainHand(stack); core().getEquipmentAbilities().reconcile(player);
    }
    private void snapshot(String id) throws Exception {
        var world = Bukkit.getWorlds().getFirst(); var player = Bukkit.getPlayerExact(NAME);
        var samples = new ArrayList<Map<String, Object>>();
        for (var entry : items.entrySet()) for (var evidence : core().getLocalEcosystem().pool().recentEvidence(
                new obtuseloot.ecology.WorldPosition(world.getUID(), 2, 0), 20, 24000)) {
            if (!evidence.individual().storageKey().equals(entry.getValue().getArtifactStorageKey())) continue;
            for (var sample : evidence.performance().entrySet()) {
                var row = new LinkedHashMap<String, Object>(); row.put("role", entry.getKey()); row.put("key", sample.getKey());
                row.put("count", sample.getValue().samples()); row.put("benefit", sample.getValue().meanBenefit()); row.put("recovery", sample.getValue().meanRecoveryTicks()); samples.add(row);
            }
        }
        var result = new LinkedHashMap<String, Object>(); result.put("id", id); result.put("role", role); result.put("ability", ability);
        result.put("tick", core().getLocalEcosystem().pool().gameTick());
        result.put("readyAt", current == null ? 0 : current.abilitiesReadyAt());
        result.put("waypoint", current == null ? null : String.valueOf(current.abilityWaypoint(FOOTPRINT)));
        result.put("sneaking", player != null && player.isSneaking());
        result.put("position", player == null ? null : List.of(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()));
        result.put("entityId", animal == null ? null : animal.getEntityId());
        result.put("block", world.getBlockAt(2, 64, 0).getBlockData().getAsString());
        var weather = new LinkedHashMap<String, Object>(); weather.put("storm", world.hasStorm()); weather.put("thunder", world.isThundering());
        weather.put("cycle", String.valueOf(world.getGameRuleValue(GameRule.DO_WEATHER_CYCLE))); weather.put("clear", world.getClearWeatherDuration());
        weather.put("weather", world.getWeatherDuration()); weather.put("thunderTimer", world.getThunderDuration()); result.put("weather", weather);
        result.put("samples", samples); result.put("interactions", interactions); result.put("fixtures", fixtures);
        write("snapshot.json", result);
    }
    private void fail(Throwable failure) {
        getLogger().severe(failure.toString()); failure.printStackTrace();
        try { Files.createDirectories(root); Files.writeString(root.resolve("failure.txt"), failure.toString()); } catch (Exception ignored) { }
        getServer().shutdown();
    }
}
