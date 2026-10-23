package obtuseloot.servertest;

import com.google.gson.GsonBuilder;
import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.*;
import obtuseloot.abilities.genome.*;
import org.bukkit.*;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import java.nio.file.*;
import java.util.*;

/** Generates synthetic gear and terrain only in an empty disposable server. Never deploy this probe. */
public final class InsightProbe extends JavaPlugin implements Listener {
    private static final String ABILITY = "precision.material_insight";
    private Path root; private String last = "", role = ""; private Artifact current;
    private final Map<String, Artifact> items = new LinkedHashMap<>();
    private final List<Map<String, Object>> interactions = new ArrayList<>();
    private long seed = -32000; private PermissionAttachment permission;
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
                    optionalAbsent(); terrain(); write("standalone.json", Map.of("passed", true, "minecraft", Bukkit.getMinecraftVersion())); getServer().shutdown();
                } else if (Files.exists(root.resolve("restart-only"))) {
                    optionalAbsent(); var expected = new YamlConfiguration(); expected.load(root.resolve("expected.yml").toFile());
                    var item = core().getArtifactManager().resolveByStorageKey(expected.getString("key"));
                    require(item.getArtifactSeed() == expected.getLong("seed"), "Lost identity");
                    require(item.abilityReadyAt(ABILITY) == expected.getLong("ready"), "Lost committed deadline");
                    require(item.abilityReadyAt(ABILITY) > core().getLocalEcosystem().pool().gameTick(), "Recovery expired/reset during restart");
                    require(item.getGenome().traits().toString().equals(expected.getString("genes")), "Genes changed on restart");
                    write("restart.json", Map.of("passed", true, "readyAt", item.abilityReadyAt(ABILITY), "currentTick", core().getLocalEcosystem().pool().gameTick(), "priorTick", expected.getLong("tick")));
                    getServer().shutdown();
                }
            } catch (Throwable failure) { fail(failure); }
        }, 1);
        getServer().getScheduler().runTaskTimer(this, this::control, 1, 1);
        getServer().getScheduler().runTaskLater(this, () -> fail(new AssertionError("Probe deadline")), 5000);
    }
    private void optionalAbsent() { for (String name : List.of("ViaVersion", "ViaBackwards", "LuckPerms", "PlaceholderAPI", "AuraSkills")) require(!Bukkit.getPluginManager().isPluginEnabled(name), "Unexpected optional plugin " + name); }
    private void terrain() {
        var world = Bukkit.getWorlds().getFirst();
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
            world.getBlockAt(x,63,z).setType(Material.STONE);
            for (int y = 64; y <= 73; y++) world.getBlockAt(x,y,z).setType(Material.AIR);
        }
        world.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void protection(PlayerInteractEvent event) {
        if (!event.getPlayer().getName().equals("OLInsightProbe")) return;
        if (role.equals("deny-block")) event.setUseInteractedBlock(Event.Result.DENY);
        if (role.equals("deny-item")) event.setUseItemInHand(Event.Result.DENY);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void interaction(PlayerInteractEvent event) {
        if (!event.getPlayer().getName().equals("OLInsightProbe")) return;
        var row = new LinkedHashMap<String,Object>(); row.put("role", role); row.put("action", event.getAction().name());
        row.put("hand", String.valueOf(event.getHand())); row.put("blockUse", event.useInteractedBlock().name());
        row.put("itemUse", event.useItemInHand().name()); row.put("cancelled", event.isCancelled()); interactions.add(row);
    }
    private void control() {
        try {
            var file = root.resolve("request.properties"); if (!Files.exists(file)) return;
            var request = new Properties(); try (var input = Files.newInputStream(file)) { request.load(input); }
            String id = request.getProperty("id"); if (id == null || id.equals(last)) return;
            var player = Bukkit.getPlayerExact("OLInsightProbe"); if (player == null) return; last = id;
            switch (request.getProperty("action")) {
                case "setup" -> setup(player, request.getProperty("role"));
                case "offhand" -> { var stack = player.getInventory().getItemInMainHand().clone(); player.getInventory().setItemInOffHand(stack); player.getInventory().setItemInMainHand(new ItemStack(Material.AIR)); core().getEquipmentAbilities().reconcile(player); }
                case "snapshot" -> { }
                case "finish" -> {
                    require(current != null && current.abilityReadyAt(ABILITY) > core().getLocalEcosystem().pool().gameTick(), "Finish must retain actual active recovery");
                    var replacement = new Artifact(player.getUniqueId(), EquipmentArchetype.DIAMOND_AXE);
                    replacement.setArtifactStorageKey(current.getArtifactStorageKey()); replacement.setArtifactSeed(seed--);
                    ArtifactIdentityTransition.regenerate(current, replacement);
                    var restored = obtuseloot.persistence.ArtifactProfileCodec.decode(replacement.getArtifactStorageKey(),
                            obtuseloot.persistence.ArtifactProfileCodec.encode(replacement));
                    require(restored.abilityReadyAt(ABILITY) == current.abilityReadyAt(ABILITY), "Same-item recreation reset recovery");
                    write("continuity.json", Map.of("passed",true,"oldSeed",current.getArtifactSeed(),"newSeed",restored.getArtifactSeed(),"readyAt",restored.abilityReadyAt(ABILITY)));
                    core().getArtifactManager().saveItem(current); core().getLocalEcosystem().save();
                    var expected = new YamlConfiguration(); expected.set("key", current.getArtifactStorageKey()); expected.set("seed", current.getArtifactSeed());
                    expected.set("genes", current.getGenome().traits().toString()); expected.set("ready", current.abilityReadyAt(ABILITY)); expected.set("tick", core().getLocalEcosystem().pool().gameTick());
                    expected.save(root.resolve("expected.yml").toFile()); snapshot(id); getServer().shutdown(); return;
                }
                default -> throw new AssertionError("Unknown request");
            }
            snapshot(id);
        } catch (Throwable failure) { fail(failure); }
    }
    private void setup(Player player, String name) {
        role = name; player.setOp(true); player.setGameMode(GameMode.SURVIVAL); player.setFlying(false); player.setAllowFlight(false);
        if (permission != null && permission.getPermissible() == player) player.removeAttachment(permission); permission = player.addAttachment(this);
        permission.setPermission("obtuseloot.ability.precision.material_insight", !name.equals("permission"));
        player.getInventory().clear(); terrain(); var world = player.getWorld();
        if (name.equals("sparse")) world.getBlockAt(2,64,0).setType(Material.STONE);
        else {
            var soil = world.getBlockAt(2,63,0); soil.setType(Material.FARMLAND); var moisture = (Farmland)soil.getBlockData(); moisture.setMoisture(7); soil.setBlockData(moisture,false);
            var crop = world.getBlockAt(2,64,0); crop.setType(Material.WHEAT); var age = (Ageable)crop.getBlockData(); age.setAge(4); crop.setBlockData(age,false);
        }
        player.teleport(new Location(world,.5,64,.5,-90,0));
        double precision = name.equals("low") ? 0 : 1; current = null;
        for (int i = 0; i < 200; i++) {
            var item = new Artifact(player.getUniqueId(), EquipmentArchetype.DIAMOND_AXE); item.setArtifactStorageKey(ArtifactStorageKeys.create()); item.setArtifactSeed(seed--);
            var traits = new EnumMap<>(item.getGenome().traits()); traits.put(GenomeTrait.PRECISION_AFFINITY, precision); item.setGenome(new ArtifactGenome(item.getArtifactSeed(),traits));
            core().getArtifactManager().registerItem(item);
            if (core().getItemAbilityManager().equippedDefinitions(player.getUniqueId(), item).stream().anyMatch(d -> d.id().equals(ABILITY))) { current=item; break; }
        }
        require(current != null, "No Material Insight fixture"); items.put(role,current);
        var stack = new ItemStack(Material.DIAMOND_AXE); core().getArtifactItemStorage().stampMinimalIdentity(stack,current);
        player.getInventory().setItemInMainHand(stack); core().getEquipmentAbilities().reconcile(player);
    }
    private void snapshot(String id) throws Exception {
        var samples = new ArrayList<Map<String,Object>>();
        for (var entry : items.entrySet()) for (var evidence : core().getLocalEcosystem().pool().recentEvidence(
                new obtuseloot.ecology.WorldPosition(Bukkit.getWorlds().getFirst().getUID(),2,0),20,6000)) {
            if (!evidence.individual().storageKey().equals(entry.getValue().getArtifactStorageKey())) continue;
            for (var sample : evidence.performance().entrySet()) {
                var row = new LinkedHashMap<String,Object>(); row.put("role",entry.getKey()); row.put("key",sample.getKey());
                row.put("count",sample.getValue().samples()); row.put("facts",sample.getValue().meanBenefit()); row.put("recovery",sample.getValue().meanRecoveryTicks()); samples.add(row);
            }
        }
        var result = new LinkedHashMap<String,Object>(); result.put("id",id); result.put("role",role); result.put("tick",core().getLocalEcosystem().pool().gameTick());
        result.put("readyAt", current==null?0:current.abilityReadyAt(ABILITY)); result.put("samples",samples); result.put("interactions",interactions);
        result.put("crop", Bukkit.getWorlds().getFirst().getBlockAt(2,64,0).getBlockData().getAsString());
        write("snapshot.json",result);
    }
    private void fail(Throwable failure) {
        getLogger().severe(failure.toString()); failure.printStackTrace();
        try { Files.createDirectories(root); Files.writeString(root.resolve("failure.txt"),failure.toString()); } catch(Exception ignored) { }
        getServer().shutdown();
    }
}
