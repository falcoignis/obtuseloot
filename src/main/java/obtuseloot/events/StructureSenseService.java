package obtuseloot.events;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.generator.structure.GeneratedStructure;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

public final class StructureSenseService {
    private static final long CHUNK_CACHE_TTL_MS = 120_000L;
    private static final Set<org.bukkit.Material> STRUCTURE_AFFINITY_BLOCKS = Set.of(
            org.bukkit.Material.CHISELED_DEEPSLATE, org.bukkit.Material.SCULK, org.bukkit.Material.SCULK_SHRIEKER, org.bukkit.Material.MOSSY_COBBLESTONE,
            org.bukkit.Material.CHISELED_STONE_BRICKS, org.bukkit.Material.POLISHED_BLACKSTONE_BRICKS, org.bukkit.Material.GILDED_BLACKSTONE,
            org.bukkit.Material.SUSPICIOUS_GRAVEL, org.bukkit.Material.CUT_COPPER, org.bukkit.Material.DEEPSLATE_BRICKS
    );

    private final ChunkSenseCacheCodec chunkSenseCacheCodec;

    public StructureSenseService(ChunkSenseCacheCodec chunkSenseCacheCodec) {
        this.chunkSenseCacheCodec = chunkSenseCacheCodec;
    }

    public boolean shouldTriggerStructureSense(Chunk chunk, long now) {
        ChunkSenseCacheCodec.StructureSenseSnapshot cached = chunkSenseCacheCodec.read(chunk);
        if (cached != null && now - cached.timestampMs() < CHUNK_CACHE_TTL_MS) {
            return cached.detected();
        }
        boolean detected = locateUsingWorldApi(chunk) || hasChunkStructureSignals(chunk);
        chunkSenseCacheCodec.write(chunk, new ChunkSenseCacheCodec.StructureSenseSnapshot(now, detected));
        return detected;
    }

    private boolean locateUsingWorldApi(Chunk chunk) {
        try {
            World world = chunk.getWorld();
            Method[] methods = world.getClass().getMethods();
            for (Method method : methods) {
                if (!"locateNearestStructure".equals(method.getName()) || method.getParameterCount() != 4) {
                    continue;
                }
                Class<?> structureType = method.getParameterTypes()[1];
                if (!structureType.isEnum()) {
                    continue;
                }
                Object[] constants = structureType.getEnumConstants();
                if (constants == null || constants.length == 0) {
                    continue;
                }
                Object nearest = method.invoke(world, new Location(world, chunk.getX() << 4, 64, chunk.getZ() << 4), constants[0], 48, false);
                if (nearest != null) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
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
