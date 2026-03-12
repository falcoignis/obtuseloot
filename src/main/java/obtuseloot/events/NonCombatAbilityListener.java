package obtuseloot.events;

import obtuseloot.ObtuseLoot;
import obtuseloot.abilities.AbilityMechanic;
import obtuseloot.abilities.AbilityRuntimeContext;
import obtuseloot.abilities.AbilitySource;
import obtuseloot.abilities.AbilityTrigger;
import obtuseloot.obtuseengine.ArtifactProcessor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
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
    private static final double MOVE_DISTANCE_SQ = 16.0D;

    private final Map<UUID, Long> lastStructureSense = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lastChunkKey = new ConcurrentHashMap<>();

    private final TriggerWorkCoalescer coalescer;
    private final StructureSenseService structureSenseService;

    private static final EnumSet<Material> CROPS = EnumSet.of(Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.NETHER_WART);

    public NonCombatAbilityListener() {
        this.coalescer = new TriggerWorkCoalescer(ObtuseLoot.get());
        this.structureSenseService = new StructureSenseService(new ChunkSenseCacheCodec(ObtuseLoot.get()));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getWorld() == null || event.getTo().getWorld() == null) return;
        if (event.getFrom().distanceSquared(event.getTo()) < MOVE_DISTANCE_SQ) return;
        Player player = event.getPlayer();
        if (!allowProbe(player, AbilityTrigger.ON_WORLD_SCAN, AbilitySource.MOVE_WORLD_SCAN.id(), 0.8D, false)) return;

        int chunkKey = (event.getTo().getBlockX() >> 4) * 7340033 ^ (event.getTo().getBlockZ() >> 4);
        int previous = lastChunkKey.getOrDefault(player.getUniqueId(), Integer.MIN_VALUE);
        if (previous != chunkKey) {
            lastChunkKey.put(player.getUniqueId(), chunkKey);
            scheduleChunkAwareSense(player, event.getTo().getChunk(), true);
        }
    }

    private void scheduleChunkAwareSense(Player player, Chunk chunk, boolean coalesced) {
        String workKey = "sense:" + player.getUniqueId() + ":" + chunk.getChunkKey();
        coalescer.coalesce(workKey, 4L, () -> {
            if (!player.isOnline()) {
                return;
            }
            ArtifactProcessor.processAbilityTriggerWithResult(
                    player,
                    AbilityTrigger.ON_WORLD_SCAN,
                    1.0D,
                    AbilitySource.CHUNK_WORLD_SCAN.id(),
                    AbilityRuntimeContext.chunkAware(AbilitySource.CHUNK_WORLD_SCAN, chunk.getChunkKey(), coalesced, chunk.getWorld().getName(), chunk.getWorld().getEnvironment().name())
            );
            long now = System.currentTimeMillis();
            if (now - lastStructureSense.getOrDefault(player.getUniqueId(), 0L) >= STRUCTURE_THROTTLE_MS
                    && allowProbe(player, AbilityTrigger.ON_STRUCTURE_SENSE, AbilitySource.STRUCTURE_SENSE.id(), 1.2D, false)
                    && structureSenseService.shouldTriggerStructureSense(chunk, now)) {
                lastStructureSense.put(player.getUniqueId(), now);
                ArtifactProcessor.processAbilityTriggerWithResult(
                        player,
                        AbilityTrigger.ON_STRUCTURE_SENSE,
                        1.0D,
                        AbilitySource.STRUCTURE_SENSE.id(),
                        AbilityRuntimeContext.chunkAware(AbilitySource.STRUCTURE_SENSE, chunk.getChunkKey(), coalesced, chunk.getWorld().getName(), chunk.getWorld().getEnvironment().name())
                );
            }
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            if (Tag.DOORS.isTagged(block.getType()) || Tag.TRAPDOORS.isTagged(block.getType()) || Tag.FENCE_GATES.isTagged(block.getType())) {
                ArtifactProcessor.processAbilityTriggerWithResult(player, AbilityTrigger.ON_SOCIAL_INTERACT, 1.0D, AbilitySource.SOCIAL_INTERACT.id(), AbilityRuntimeContext.intentional(AbilitySource.SOCIAL_INTERACT, player.getWorld().getName(), player.getWorld().getEnvironment().name()));
                ArtifactProcessor.processAbilityTriggerWithResult(player, AbilityTrigger.ON_RITUAL_INTERACT, 1.0D, AbilitySource.SOCIAL_INTERACT.id(), AbilityRuntimeContext.intentional(AbilitySource.SOCIAL_INTERACT, player.getWorld().getName(), player.getWorld().getEnvironment().name()));
            }
            if (block.getType() == Material.CAMPFIRE || block.getType() == Material.SOUL_CAMPFIRE) {
                ArtifactProcessor.processAbilityTriggerWithResult(player, AbilityTrigger.ON_RITUAL_INTERACT, 1.0D, AbilitySource.SOCIAL_INTERACT.id(), AbilityRuntimeContext.intentional(AbilitySource.SOCIAL_INTERACT, player.getWorld().getName(), player.getWorld().getEnvironment().name()));
            }
            ArtifactProcessor.processAbilityTriggerWithResult(player, AbilityTrigger.ON_BLOCK_INSPECT, 1.0D, AbilitySource.BLOCK_INSPECT.id(), AbilityRuntimeContext.intentional(AbilitySource.BLOCK_INSPECT, player.getWorld().getName(), player.getWorld().getEnvironment().name()));
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR && player.isSneaking()) {
            ArtifactProcessor.processAbilityTriggerWithResult(player, AbilityTrigger.ON_RITUAL_INTERACT, 1.0D, AbilitySource.RITUAL_GESTURE.id(), AbilityRuntimeContext.intentional(AbilitySource.RITUAL_GESTURE, player.getWorld().getName(), player.getWorld().getEnvironment().name()));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityInspect(PlayerInteractAtEntityEvent event) {
        ArtifactProcessor.processAbilityTriggerWithResult(event.getPlayer(), AbilityTrigger.ON_ENTITY_INSPECT, 1.0D, AbilitySource.ENTITY_INSPECT.id(), AbilityRuntimeContext.intentional(AbilitySource.ENTITY_INSPECT, event.getPlayer().getWorld().getName(), event.getPlayer().getWorld().getEnvironment().name()));
        Entity entity = event.getRightClicked();
        if (!(entity instanceof Player)) {
            ArtifactProcessor.processAbilityTriggerWithResult(event.getPlayer(), AbilityTrigger.ON_WITNESS_EVENT, 1.0D, AbilitySource.WITNESS_EVENT.id(), AbilityRuntimeContext.passive(AbilitySource.WITNESS_EVENT, event.getPlayer().getWorld().getName(), event.getPlayer().getWorld().getEnvironment().name()));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHarvest(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!CROPS.contains(block.getType())) return;
        boolean hasGentleHarvest = ArtifactProcessor.processAbilityTriggerWithResult(event.getPlayer(), AbilityTrigger.ON_BLOCK_HARVEST, 1.0D, AbilitySource.CROP_HARVEST.id(), AbilityRuntimeContext.intentional(AbilitySource.CROP_HARVEST, event.getPlayer().getWorld().getName(), event.getPlayer().getWorld().getEnvironment().name()))
                .hasSuccessfulMechanic(AbilityMechanic.HARVEST_RELAY);
        if (hasGentleHarvest) {
            Bukkit.getScheduler().runTaskLater(ObtuseLoot.get(), () -> block.getWorld().getBlockAt(block.getLocation()).setType(block.getType()), 1L);
        }
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
        var artifact = plugin.getArtifactManager().getOrCreate(player.getUniqueId());
        return plugin.getItemAbilityManager().triggerBudgetManager()
                .allowProbe(player.getUniqueId(), artifact.getArtifactStorageKey(), trigger, source, cost, intentional);
    }
}
