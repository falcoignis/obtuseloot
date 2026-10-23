package obtuseloot.ecology;

import org.bukkit.Location;
import java.util.Objects;

/** A measured column descriptor, independent of ability IDs and genotype. Null ceiling means open sky. */
public record HabitatContext(Integer overheadBlocks) {
    public HabitatContext {
        if (overheadBlocks != null && overheadBlocks < 0) throw new IllegalArgumentException("Negative overhead space");
    }

    /** Read only the already-loaded column; never load/generate a chunk to collect reproductive evidence. */
    public static HabitatContext capture(Location location) {
        var world = Objects.requireNonNull(location.getWorld());
        int x = location.getBlockX(), z = location.getBlockZ();
        if (!world.isChunkLoaded(x >> 4, z >> 4)) return null;
        int start = Math.max(world.getMinHeight(), location.getBlockY() + 1);
        if (start >= world.getMaxHeight()) return new HabitatContext(null);
        for (int y = start; y < world.getMaxHeight(); y++)
            if (!world.getBlockAt(x, y, z).isPassable()) return new HabitatContext(y - start);
        return new HabitatContext(null);
    }

    /** Open sky and roofed columns are incomparable; finite observations interpolate by nearest measured depth. */
    public double distanceTo(HabitatContext other) {
        if (overheadBlocks == null || other.overheadBlocks == null)
            return Objects.equals(overheadBlocks, other.overheadBlocks) ? 0 : Double.POSITIVE_INFINITY;
        return Math.abs((double) overheadBlocks - other.overheadBlocks);
    }
    public String description() { return overheadBlocks == null ? "open sky" : overheadBlocks + " passable overhead blocks"; }
}
