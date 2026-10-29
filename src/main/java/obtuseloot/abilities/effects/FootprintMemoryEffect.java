package obtuseloot.abilities.effects;

import obtuseloot.abilities.*;
import obtuseloot.artifacts.ArtifactWaypoint;
import org.bukkit.entity.Player;
import org.bukkit.block.BlockFace;
import org.bukkit.Bukkit;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Locale;
import java.util.Objects;

/** Sneak-right-click a block top to mark, or another face to read a same-world return bearing. */
public final class FootprintMemoryEffect extends MeasuredUtilityEffect {
    public FootprintMemoryEffect() { super("footprint-memory", 256, 60); }
    @Override public boolean applicable(AbilityEventContext context) {
        return context.liveEvent() instanceof PlayerInteractEvent event && event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.getClickedBlock() != null && Bukkit.isPrimaryThread() && event.getPlayer().isSneaking();
    }
    @Override protected Report prepare(AbilityDefinition definition, AbilityEventContext context, Player player, double output) {
        if (!(context.liveEvent() instanceof PlayerInteractEvent event) || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null || !event.getClickedBlock().getWorld().getUID().equals(player.getWorld().getUID())) return null;
        var location = player.getLocation();
        var item = context.artifact();
        var previous = item.abilityWaypoint(definition.id());
        if (event.getBlockFace() == BlockFace.UP) {
            var point = new ArtifactWaypoint(player.getWorld().getUID(), location.getX(), location.getY(), location.getZ());
            return new Report("Footprint Memory: return point remembered", "mark:" + point,
                    1, AbilityOutcomeType.MEMORY_MARK,
                    () -> item.setAbilityWaypoint(definition.id(), point),
                    () -> { if (Objects.equals(item.abilityWaypoint(definition.id()), point)) item.setAbilityWaypoint(definition.id(), previous); });
        }
        if (previous == null || !previous.world().equals(player.getWorld().getUID())) return null;
        double dx = previous.x() - location.getX(), dz = previous.z() - location.getZ();
        double distance = Math.hypot(dx, dz);
        if (distance > output) return null;
        String[] directions = {"east", "south-east", "south", "south-west", "west", "north-west", "north", "north-east"};
        int index = Math.floorMod((int) Math.round(Math.atan2(dz, dx) / (Math.PI / 4)), 8);
        long elevation = Math.round(previous.y() - location.getY());
        String text = String.format(Locale.ROOT, "Footprint Memory: %s, %.0f blocks; %d blocks %s",
                distance < 1 ? "here" : directions[index], distance, Math.abs(elevation), elevation < 0 ? "down" : "up");
        return new Report(text, "recall:" + previous, 1, AbilityOutcomeType.NAVIGATION_HINT);
    }
}
