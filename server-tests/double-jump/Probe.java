package obtuseloot.servertest;

import obtuseloot.ObtuseLoot;
import obtuseloot.artifacts.*;
import obtuseloot.events.ArtifactAirJumpEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.*;
import java.util.*;

/** Disposable-server fixture only; excluded from the production JAR. Never install on a real world. */
public final class Probe extends JavaPlugin implements Listener {
    private Path root;
    private String stage = "none", last = "";
    private int proposals, nativeVelocity, inputs;
    private long seeds = -1000;
    private PermissionAttachment attachment;
    private final List<Artifact> gear = new ArrayList<>();
    @Override public void onEnable() {
        root = getDataFolder().toPath();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskLater(this, () -> {
            if (Files.exists(root.resolve("boot-only"))) {
                try {
                    if (ObtuseLoot.get() == null || !ObtuseLoot.get().isEnabled()) throw new AssertionError("core unavailable");
                    for (String plugin : new String[]{"PlaceholderAPI", "LuckPerms", "AuraSkills", "ViaVersion", "ViaBackwards"})
                        if (getServer().getPluginManager().isPluginEnabled(plugin)) throw new AssertionError("Unexpected dependency: " + plugin);
                    Files.writeString(root.resolve("boot-result.json"), "{\"standalone_boot\":true,\"minecraft\":\"" + Bukkit.getMinecraftVersion() + "\"}\n");
                } catch (Throwable failure) { fail(failure); }
                getServer().shutdown();
            }
        }, 40);
        getServer().getScheduler().runTaskTimer(this, this::control, 1, 1);
        getServer().getScheduler().runTaskLater(this, () -> { fail(new AssertionError("Probe deadline")); getServer().shutdown(); }, 3600);
    }
    private void control() {
        try {
            if (Files.exists(root.resolve("finish"))) { getServer().shutdown(); return; }
            Path file = root.resolve("request.properties"); if (!Files.exists(file)) return;
            var request = new Properties(); try (var in = Files.newInputStream(file)) { request.load(in); }
            String id = request.getProperty("id"); if (id == null || id.equals(last)) return;
            Player player = Bukkit.getPlayerExact("OLJumpProbe"); if (player == null) return;
            last = id;
            if (request.getProperty("action").equals("setup")) setup(player, request.getProperty("stage"));
            snapshot(player, id);
        } catch (Throwable failure) { fail(failure); getServer().shutdown(); }
    }
    private void setup(Player player, String next) {
        stage = "reset";
        if (attachment != null) { player.removeAttachment(attachment); attachment = null; }
        player.setOp(true); player.setFlying(false); player.setAllowFlight(false); player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear(); gear.clear();
        var world = player.getWorld();
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) world.getBlockAt(x, 63, z).setType(Material.STONE);
        player.teleport(new Location(world, .5, 64, .5)); player.setVelocity(new org.bukkit.util.Vector());
        if (!next.equals("baseline")) {
            player.getInventory().setBoots(item(player, Material.DIAMOND_BOOTS));
            player.getInventory().setHelmet(item(player, Material.DIAMOND_HELMET));
        }
        if (next.equals("denied")) attachment = player.addAttachment(this, "obtuseloot.ability.mobility.double_jump", false);
        if (next.equals("flight")) player.setAllowFlight(true);
        if (next.equals("elytra")) player.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
        proposals = nativeVelocity = inputs = 0; stage = next;
        ObtuseLoot.get().getEquipmentAbilities().reconcile(player);
    }
    private ItemStack item(Player player, Material type) {
        var core = ObtuseLoot.get();
        for (int i = 0; i < 80; i++) {
            var artifact = new Artifact(player.getUniqueId(), type.name().toLowerCase(Locale.ROOT));
            artifact.setArtifactStorageKey(ArtifactStorageKeys.create()); artifact.setArtifactSeed(seeds--);
            core.getArtifactManager().registerItem(artifact);
            if (core.getItemAbilityManager().equippedDefinitions(player.getUniqueId(), artifact).stream().noneMatch(d -> d.id().equals("mobility.double_jump"))) continue;
            var stack = new ItemStack(type); core.getArtifactItemStorage().stampMinimalIdentity(stack, artifact); gear.add(artifact); return stack;
        }
        throw new AssertionError("Could not select double jump fixture");
    }
    private void snapshot(Player player, String id) throws Exception {
        long outcomes = 0, attempts = 0;
        for (Artifact artifact : gear) {
            var signal = ObtuseLoot.get().getArtifactUsageTracker().profileFor(artifact).utilitySignalsByMechanic().get("MOVEMENT_ECHO@ON_AIR_JUMP");
            if (signal != null) { outcomes += signal.meaningfulOutcomes(); attempts += signal.attempts(); }
        }
        String value = "{\"id\":\"" + id + "\",\"stage\":\"" + stage + "\",\"proposals\":" + proposals
                + ",\"nativeVelocity\":" + nativeVelocity + ",\"inputs\":" + inputs + ",\"outcomes\":" + outcomes
                + ",\"attempts\":" + attempts + ",\"allowFlight\":" + player.getAllowFlight() + ",\"gliding\":" + player.isGliding() + "}\n";
        Path temp = root.resolve("snapshot.tmp"); Files.writeString(temp, value);
        Files.move(temp, root.resolve("snapshot.json"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void proposal(ArtifactAirJumpEvent event) {
        proposals++;
        if (stage.equals("veto")) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void velocity(PlayerVelocityEvent event) {
        if (!stage.equals("reset") && event.getVelocity().getY() > .35) {
            nativeVelocity++;
        }
        if (stage.equals("velocity-veto") && proposals > 0) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.MONITOR) public void input(PlayerInputEvent event) {
        if (!event.getInput().isJump()) return;
        inputs++;
        if (stage.equals("swap") && !event.getPlayer().isOnGround()) {
            event.getPlayer().getInventory().setBoots(null); event.getPlayer().getInventory().setHelmet(null);
        }
    }
    private void fail(Throwable failure) {
        getLogger().log(java.util.logging.Level.SEVERE, "Probe failed", failure);
        try { Files.writeString(root.resolve("failure.txt"), failure.toString()); } catch (Exception ignored) { }
    }
}
