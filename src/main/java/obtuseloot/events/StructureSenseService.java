package obtuseloot.events;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.generator.structure.GeneratedStructure;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

public final class StructureSenseService {
    private static final long HIT_CACHE_TTL_MS = 180_000L;
    private static final long MISS_CACHE_TTL_MS = 600_000L;
    private static final long LOCATE_FAILURE_BACKOFF_MS = 300_000L;
    private static final Set<org.bukkit.Material> STRUCTURE_AFFINITY_BLOCKS = Set.of(
            org.bukkit.Material.CHISELED_DEEPSLATE, org.bukkit.Material.SCULK, org.bukkit.Material.SCULK_SHRIEKER, org.bukkit.Material.MOSSY_COBBLESTONE,
            org.bukkit.Material.CHISELED_STONE_BRICKS, org.bukkit.Material.POLISHED_BLACKSTONE_BRICKS, org.bukkit.Material.GILDED_BLACKSTONE,
            org.bukkit.Material.SUSPICIOUS_GRAVEL, org.bukkit.Material.CUT_COPPER, org.bukkit.Material.DEEPSLATE_BRICKS
    );

    private static final Method LOCATE_NEAREST_STRUCTURE_METHOD = resolveLocateMethod();
    private static final Object DEFAULT_STRUCTURE_TYPE = resolveDefaultStructureType();

    private final ChunkSenseCacheCodec chunkSenseCacheCodec;
    private final Map<String, Long> worldLocateBackoffUntil = new ConcurrentHashMap<>();

    public StructureSenseService(ChunkSenseCacheCodec chunkSenseCacheCodec) {
        this.chunkSenseCacheCodec = chunkSenseCacheCodec;
    }

    public boolean shouldTriggerStructureSense(Chunk chunk, long now) {
        ChunkSenseCacheCodec.StructureSenseSnapshot cached = chunkSenseCacheCodec.read(chunk);
        if (cached != null && now - cached.timestampMs() < cacheTtl(cached.detected())) {
            return cached.detected();
        }
        boolean detected = hasChunkStructureSignals(chunk) || locateUsingWorldApi(chunk, now);
        chunkSenseCacheCodec.write(chunk, new ChunkSenseCacheCodec.StructureSenseSnapshot(now, detected));
        return detected;
    }

    private long cacheTtl(boolean detected) {
        return detected ? HIT_CACHE_TTL_MS : MISS_CACHE_TTL_MS;
    }

    private boolean locateUsingWorldApi(Chunk chunk, long now) {
        if (LOCATE_NEAREST_STRUCTURE_METHOD == null || DEFAULT_STRUCTURE_TYPE == null) {
            return false;
        }
        String worldKey = chunk.getWorld().getUID().toString();
        long backoffUntil = worldLocateBackoffUntil.getOrDefault(worldKey, 0L);
        if (backoffUntil > now) {
            return false;
        }
        try {
            World world = chunk.getWorld();
            Object nearest = LOCATE_NEAREST_STRUCTURE_METHOD.invoke(
                    world,
                    new Location(world, chunk.getX() << 4, 64, chunk.getZ() << 4),
                    DEFAULT_STRUCTURE_TYPE,
                    48,
                    false
            );
            worldLocateBackoffUntil.remove(worldKey);
            return nearest != null;
        } catch (Exception ignored) {
            worldLocateBackoffUntil.put(worldKey, now + LOCATE_FAILURE_BACKOFF_MS);
            return false;
        }
    }

    private static Method resolveLocateMethod() {
        for (Method method : World.class.getMethods()) {
            if (!"locateNearestStructure".equals(method.getName()) || method.getParameterCount() != 4) {
                continue;
            }
            Class<?> structureType = method.getParameterTypes()[1];
            if (structureType.isEnum()) {
                return method;
            }
        }
        return null;
    }

    private static Object resolveDefaultStructureType() {
        if (LOCATE_NEAREST_STRUCTURE_METHOD == null) {
            return null;
        }
        Object[] constants = LOCATE_NEAREST_STRUCTURE_METHOD.getParameterTypes()[1].getEnumConstants();
        if (constants == null || constants.length == 0) {
            return null;
        }
        return constants[0];
    }

    private boolean hasChunkStructureSignals(Chunk chunk) {
        Collection<GeneratedStructure> generatedStructures = chunk.getStructures();
        if (generatedStructures != null && !generatedStructures.isEmpty()) {
            return true;
        }
        for (BlockState tile : chunk.getTileEntities(true)) {
            if (STRUCTURE_AFFINITY_BLOCKS.contains(tile.getType())) {
                return true;
            }
        }
        return false;
    }
}
