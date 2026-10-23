package obtuseloot.ecology;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Objects;
import java.util.UUID;

/** Horizontal distance in one world, independent of chunk-grid boundaries. */
public record WorldPosition(UUID worldId, double x, double z, HabitatContext habitat) {
    /** Explicit unknown context, used by offline callers; it cannot stand in for a measured habitat. */
    public WorldPosition(UUID worldId, double x, double z) { this(worldId, x, z, null); }
    public WorldPosition {
        Objects.requireNonNull(worldId);
        if (!Double.isFinite(x) || !Double.isFinite(z)) throw new IllegalArgumentException("Invalid position");
    }
    public boolean within(WorldPosition other, int radiusChunks) {
        if (radiusChunks <= 0) throw new IllegalArgumentException("Inheritance radius must be positive chunks");
        double radius = radiusChunks * 16.0;
        double dx = x - other.x, dz = z - other.z;
        return worldId.equals(other.worldId) && dx * dx + dz * dz <= radius * radius;
    }
    public static WorldPosition from(Location location) {
        return new WorldPosition(Objects.requireNonNull(location.getWorld()).getUID(), location.getX(), location.getZ(), HabitatContext.capture(location));
    }
    public static WorldPosition from(Block block) {
        return from(new Location(block.getWorld(), block.getX() + 0.5, block.getY() + 1.0, block.getZ() + 0.5));
    }
    public static WorldPosition fromEvent(Event event) {
        if (event instanceof PlayerInteractEvent interaction && interaction.getClickedBlock() != null) return from(interaction.getClickedBlock());
        if (event instanceof BlockEvent block) return from(block.getBlock());
        if (event instanceof PlayerEvent player) return from(player.getPlayer().getLocation());
        return null;
    }
}
