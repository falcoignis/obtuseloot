package obtuseloot.ecology;

import org.bukkit.Location;
import java.util.Objects;

/** Measured vertical cavity span, independent of actor elevation, ability IDs and genotype. Null height means open sky. */
public record HabitatContext(Integer columnHeightBlocks) {
    public HabitatContext {
        if (columnHeightBlocks != null && columnHeightBlocks < 0) throw new IllegalArgumentException("Negative column height");
    }

    /** Read only the loaded column. The reference cell may contain a loot source or an actor. */
    public static HabitatContext capture(Location location) {
        var world = Objects.requireNonNull(location.getWorld());
        int x = location.getBlockX(), z = location.getBlockZ();
        if (!world.isChunkLoaded(x >> 4, z >> 4)) return null;
        int anchor = location.getBlockY(), minimum = world.getMinHeight(), maximum = world.getMaxHeight();
        if (anchor < minimum) return null;
        if (anchor >= maximum - 1) return new HabitatContext(null);
        Integer ceiling = null;
        for (int y = anchor + 1; y < maximum; y++) {
            if (!world.getBlockAt(x, y, z).isPassable()) { ceiling = y; break; }
        }
        if (ceiling == null) return new HabitatContext(null);
        for (int y = anchor - 1; y >= minimum; y--)
            if (!world.getBlockAt(x, y, z).isPassable()) return new HabitatContext(ceiling - y - 1);
        return null; // A roof without an observed lower boundary is unsupported, not a fabricated height.
    }

    /** Open sky and roofed columns are incomparable; finite observations use the nearest measured column height. */
    public double distanceTo(HabitatContext other) {
        if (columnHeightBlocks == null || other.columnHeightBlocks == null)
            return Objects.equals(columnHeightBlocks, other.columnHeightBlocks) ? 0 : Double.POSITIVE_INFINITY;
        return Math.abs((double) columnHeightBlocks - other.columnHeightBlocks);
    }
    public String description() { return columnHeightBlocks == null ? "open sky" : columnHeightBlocks + " passable column blocks"; }
}
