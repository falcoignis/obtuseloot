package obtuseloot.events;

import obtuseloot.abilities.AbilityMechanic;
import obtuseloot.abilities.AbilityTrigger;
import obtuseloot.obtuseengine.ArtifactProcessor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Collection;
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
    private final Map<Long, StructureSenseSnapshot> structureSenseCache = new ConcurrentHashMap<>();


    private static final EnumSet<Material> CROPS = EnumSet.of(Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS, Material.NETHER_WART);
    private static final Set<Material> STRUCTURE_AFFINITY_BLOCKS = Set.of(
            Material.CHISELED_DEEPSLATE, Material.SCULK, Material.SCULK_SHRIEKER, Material.MOSSY_COBBLESTONE,
            Material.CHISELED_STONE_BRICKS, Material.POLISHED_BLACKSTONE_BRICKS, Material.GILDED_BLACKSTONE,
            Material.SUSPICIOUS_GRAVEL, Material.CUT_COPPER, Material.DEEPSLATE_BRICKS
    );

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getWorld() == null || event.getTo().getWorld() == null) return;
        if (event.getFrom().distanceSquared(event.getTo()) < MOVE_DISTANCE_SQ) return;
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        if (!allowProbe(player, AbilityTrigger.ON_WORLD_SCAN, "move-world-scan-probe", 0.8D, false)) return;

        int chunkKey = (event.getTo().getBlockX() >> 4) * 7340033 ^ (event.getTo().getBlockZ() >> 4);
        int previous = lastChunkKey.getOrDefault(player.getUniqueId(), Integer.MIN_VALUE);
        if (previous != chunkKey) {
            lastChunkKey.put(player.getUniqueId(), chunkKey);
            ArtifactProcessor.processAbilityTrigger(player, AbilityTrigger.ON_WORLD_SCAN, 1.0D, "move-chunk");
            if (now - lastStructureSense.getOrDefault(player.getUniqueId(), 0L) >= STRUCTURE_THROTTLE_MS
                    && allowProbe(player, AbilityTrigger.ON_STRUCTURE_SENSE, "move-structure-probe", 1.2D, false)) {
                lastStructureSense.put(player.getUniqueId(), now);
                if (shouldSenseStructures(event.getTo().getChunk(), now)) {
                    ArtifactProcessor.processAbilityTrigger(player, AbilityTrigger.ON_STRUCTURE_SENSE, 1.0D, "structure-cache-hit");
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block block = event.getClickedBlock();
            if (Tag.DOORS.isTagged(block.getType()) || Tag.TRAPDOORS.isTagged(block.getType()) || Tag.FENCE_GATES.isTagged(block.getType())) {
                ArtifactProcessor.processAbilityTrigger(player, AbilityTrigger.ON_SOCIAL_INTERACT, 1.0D, "quiet-passage");
                ArtifactProcessor.processAbilityTrigger(player, AbilityTrigger.ON_RITUAL_INTERACT, 1.0D, "quiet-passage");
            }
            if (block.getType() == Material.CAMPFIRE || block.getType() == Material.SOUL_CAMPFIRE) {
                ArtifactProcessor.processAbilityTrigger(player, AbilityTrigger.ON_RITUAL_INTERACT, 1.0D, "campfire");
            }
            ArtifactProcessor.processAbilityTrigger(player, AbilityTrigger.ON_BLOCK_INSPECT, 1.0D, "inspect-block");
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR && player.isSneaking()) {
            ArtifactProcessor.processAbilityTrigger(player, AbilityTrigger.ON_RITUAL_INTERACT, 1.0D, "gesture-anchor");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityInspect(PlayerInteractAtEntityEvent event) {
        ArtifactProcessor.processAbilityTrigger(event.getPlayer(), AbilityTrigger.ON_ENTITY_INSPECT, 1.0D, "inspect-entity");
        Entity entity = event.getRightClicked();
        if (!(entity instanceof Player)) {
            ArtifactProcessor.processAbilityTrigger(event.getPlayer(), AbilityTrigger.ON_WITNESS_EVENT, 1.0D, "entity-witness");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHarvest(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!CROPS.contains(block.getType())) return;
        boolean hasGentleHarvest = ArtifactProcessor.processAbilityTriggerWithResult(event.getPlayer(), AbilityTrigger.ON_BLOCK_HARVEST, 1.0D, "crop-harvest")
                .hasSuccessfulMechanic(AbilityMechanic.HARVEST_RELAY);
        if (hasGentleHarvest) {
            block.getWorld().getBlockAt(block.getLocation()).setType(block.getType());
        }
    }
    private boolean shouldSenseStructures(Chunk chunk, long now) {
        long chunkKey = chunk.getChunkKey();
        StructureSenseSnapshot cached = structureSenseCache.get(chunkKey);
        if (cached != null && now - cached.timestamp() < 120_000L) {
            return cached.detected();
        }

        boolean detected = hasStructureSignals(chunk);
        structureSenseCache.put(chunkKey, new StructureSenseSnapshot(now, detected));
        return detected;
    }

    private boolean hasStructureSignals(Chunk chunk) {
        Collection<GeneratedStructure> generatedStructures = chunk.getStructures();
        if (generatedStructures != null && !generatedStructures.isEmpty()) {
            return true;
        }
        for (org.bukkit.block.BlockState tile : chunk.getTileEntities(true)) {
            if (STRUCTURE_AFFINITY_BLOCKS.contains(tile.getType())) {
                return true;
            }
        }
        return false;
    }

    private boolean allowProbe(Player player, AbilityTrigger trigger, String source, double cost, boolean intentional) {
        var plugin = obtuseloot.ObtuseLoot.get();
        if (plugin == null || plugin.getItemAbilityManager() == null) return true;
        var artifact = plugin.getArtifactManager().getOrCreate(player.getUniqueId());
        return plugin.getItemAbilityManager().triggerBudgetManager()
                .allowProbe(player.getUniqueId(), artifact.getArtifactStorageKey(), trigger, source, cost, intentional);
    }

    private record StructureSenseSnapshot(long timestamp, boolean detected) {
    }

}
