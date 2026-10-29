package obtuseloot.events;

import obtuseloot.ObtuseLoot;
import obtuseloot.abilities.AbilityRuntimeContext;
import obtuseloot.abilities.AbilitySource;
import obtuseloot.abilities.AbilityTrigger;
import obtuseloot.obtuseengine.ArtifactProcessor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NonCombatAbilityListener implements Listener {
    private static final long STRUCTURE_THROTTLE_MS = 3500L;

    private final Map<UUID, Long> lastStructureSense = new ConcurrentHashMap<>();
    private record WorldChunk(UUID world, int x, int z) { }
    private final Map<UUID, WorldChunk> lastChunkKey = new ConcurrentHashMap<>();

    private final TriggerWorkCoalescer coalescer;
    private final StructureSenseService structureSenseService;

    private static final EnumSet<Material> CROPS = EnumSet.of(Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.NETHER_WART);

    public NonCombatAbilityListener() {
        this.coalescer = new TriggerWorkCoalescer(ObtuseLoot.get());
        this.structureSenseService = new StructureSenseService(new ChunkSenseCacheCodec(ObtuseLoot.get()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        if (!crossesChunk(event.getFrom(), event.getTo())) return;
        Player player = event.getPlayer();
        var chunkKey = new WorldChunk(event.getTo().getWorld().getUID(), event.getTo().getBlockX() >> 4, event.getTo().getBlockZ() >> 4);
        if (!chunkKey.equals(lastChunkKey.get(player.getUniqueId()))
                && allowProbe(player, AbilityTrigger.ON_WORLD_SCAN, AbilitySource.MOVE_WORLD_SCAN.id(), 0.8D, false)) {
            lastChunkKey.put(player.getUniqueId(), chunkKey);
            scheduleChunkAwareSense(player, event.getTo().getChunk(), true, event);
        }
    }

    static boolean crossesChunk(Location from, Location to) {
        return from != null && to != null && from.getWorld() != null && to.getWorld() != null
                && from.getWorld().getUID().equals(to.getWorld().getUID())
                && (from.getBlockX() >> 4 != to.getBlockX() >> 4 || from.getBlockZ() >> 4 != to.getBlockZ() >> 4);
    }

    private void scheduleChunkAwareSense(Player player, Chunk chunk, boolean coalesced, PlayerMoveEvent event) {
        var service = ObtuseLoot.get().getEquipmentAbilities();
        var token = service.capture(player, obtuseloot.abilities.AbilityInput.event());
        var capturedChunk = new WorldChunk(chunk.getWorld().getUID(), chunk.getX(), chunk.getZ());
        String workKey = "sense:" + player.getUniqueId() + ":" + capturedChunk;
        coalescer.coalesce(workKey, 4L, () -> {
            if (!player.isOnline() || event.isCancelled() || !player.getWorld().getUID().equals(chunk.getWorld().getUID())) {
                lastChunkKey.remove(player.getUniqueId(), capturedChunk);
                return;
            }
            service.dispatch(
                    player, token,
                    AbilityTrigger.ON_WORLD_SCAN,
                    1.0D,
                    AbilitySource.CHUNK_WORLD_SCAN.id(),
                    AbilityRuntimeContext.chunkAware(AbilitySource.CHUNK_WORLD_SCAN, chunk.getChunkKey(), coalesced, chunk.getWorld().getName(), chunk.getWorld().getEnvironment().name()), event
            );
            long now = System.currentTimeMillis();
            if (now - lastStructureSense.getOrDefault(player.getUniqueId(), 0L) >= STRUCTURE_THROTTLE_MS
                    && allowProbe(player, AbilityTrigger.ON_STRUCTURE_SENSE, AbilitySource.STRUCTURE_SENSE.id(), 1.2D, false)
                    && structureSenseService.shouldTriggerStructureSense(chunk, now)) {
                lastStructureSense.put(player.getUniqueId(), now);
                service.dispatch(
                        player, token,
                        AbilityTrigger.ON_STRUCTURE_SENSE,
                        1.0D,
                        AbilitySource.STRUCTURE_SENSE.id(),
                        AbilityRuntimeContext.chunkAware(AbilitySource.STRUCTURE_SENSE, chunk.getChunkKey(), coalesced, chunk.getWorld().getName(), chunk.getWorld().getEnvironment().name()), event
                );
            }
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == null || event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
        var plugin = ObtuseLoot.get();
        var service = plugin.getEquipmentAbilities();
        Player player = event.getPlayer();
        var token = service.capture(player, obtuseloot.abilities.AbilityInput.secondary(event.getHand(), event.getAction()));
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline() || event.useInteractedBlock() == org.bukkit.event.Event.Result.DENY
                    || event.useItemInHand() == org.bukkit.event.Event.Result.DENY) return;
            var runtime = AbilityRuntimeContext.intentional(AbilitySource.BLOCK_INSPECT, player.getWorld().getName(), player.getWorld().getEnvironment().name());
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
                Block block = event.getClickedBlock();
                boolean socialBlock = Tag.DOORS.isTagged(block.getType()) || Tag.TRAPDOORS.isTagged(block.getType()) || Tag.FENCE_GATES.isTagged(block.getType());
                if (socialBlock) {
                    service.dispatch(player, token, AbilityTrigger.ON_SOCIAL_INTERACT, 1, AbilitySource.SOCIAL_INTERACT.id(), runtime, event);
                }
                boolean existingRitual = socialBlock || block.getType() == Material.CAMPFIRE || block.getType() == Material.SOUL_CAMPFIRE;
                service.dispatch(player, token, AbilityTrigger.ON_RITUAL_INTERACT, 1,
                        existingRitual ? AbilitySource.SOCIAL_INTERACT.id() : AbilitySource.RITUAL_GESTURE.id(), runtime, event);
                service.dispatch(player, token, AbilityTrigger.ON_BLOCK_INSPECT, 1, AbilitySource.BLOCK_INSPECT.id(), runtime, event);
            } else if (player.isSneaking()) {
                service.dispatch(player, token, AbilityTrigger.ON_RITUAL_INTERACT, 1, AbilitySource.RITUAL_GESTURE.id(), runtime, event);
            }
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityInspect(PlayerInteractEntityEvent event) {
        var plugin = ObtuseLoot.get(); var service = plugin.getEquipmentAbilities(); var player = event.getPlayer();
        var token = service.capture(player, obtuseloot.abilities.AbilityInput.secondary(event.getHand(), null));
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.isCancelled() || !player.isOnline()) return;
            var runtime = AbilityRuntimeContext.intentional(AbilitySource.ENTITY_INSPECT, player.getWorld().getName(), player.getWorld().getEnvironment().name());
            service.dispatch(player, token, AbilityTrigger.ON_ENTITY_INSPECT, 1, AbilitySource.ENTITY_INSPECT.id(), runtime, event);
            if (!(event.getRightClicked() instanceof Player))
                service.dispatch(player, token, AbilityTrigger.ON_WITNESS_EVENT, 1, AbilitySource.WITNESS_EVENT.id(), runtime, event);
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHarvest(BlockDropItemEvent event) {
        if (!CROPS.contains(event.getBlockState().getType())) return;
        var plugin = ObtuseLoot.get(); var service = plugin.getEquipmentAbilities(); var player = event.getPlayer();
        var token = service.capture(player, obtuseloot.abilities.AbilityInput.event());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.isCancelled() || !player.isOnline()) return;
            service.dispatch(player, token, AbilityTrigger.ON_BLOCK_HARVEST, 1, AbilitySource.CROP_HARVEST.id(),
                    AbilityRuntimeContext.intentional(AbilitySource.CROP_HARVEST, player.getWorld().getName(), player.getWorld().getEnvironment().name()), event);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        lastChunkKey.remove(playerId);
        lastStructureSense.remove(playerId);
        coalescer.cancelByPrefix("sense:" + playerId + ":");
    }

    private boolean allowProbe(Player player, AbilityTrigger trigger, String source, double cost, boolean intentional) {
        var plugin = ObtuseLoot.get();
        if (plugin == null || plugin.getItemAbilityManager() == null) return true;
        plugin.getEquipmentAbilities().reconcile(player);
        if (plugin.getEquipmentAbilities().equipped(player.getUniqueId()).isEmpty()) return false;
        return plugin.getItemAbilityManager().triggerBudgetManager()
                .allowProbe(player.getUniqueId(), "equipment:" + player.getUniqueId(), trigger, source, cost, intentional);
    }
}
