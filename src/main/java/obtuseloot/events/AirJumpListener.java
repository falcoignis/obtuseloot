package obtuseloot.events;

import obtuseloot.movement.AirJumpService;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import java.util.function.Consumer;

/** Native input only. Vanilla flight, movement, velocity and XP events remain owned by the server. */
public final class AirJumpListener implements Listener {
    private final AirJumpService jumps;
    private final Consumer<Runnable> later;
    private final java.util.Map<PlayerMoveEvent, org.bukkit.Location> destinations = new java.util.WeakHashMap<>();
    public AirJumpListener(AirJumpService jumps, Consumer<Runnable> later) { this.jumps = jumps; this.later = later; }
    @EventHandler(priority = EventPriority.MONITOR)
    public void input(PlayerInputEvent event) { jumps.input(event.getPlayer(), event.getInput().isJump()); }
    @EventHandler(priority = EventPriority.LOWEST)
    public void beforeMove(PlayerMoveEvent event) { if (event.getTo() != null) destinations.put(event, event.getTo().clone()); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void move(PlayerMoveEvent event) {
        var original = destinations.remove(event);
        if (event.isCancelled() || original != null && !original.equals(event.getTo())) jumps.invalidate(event.getPlayer());
        if (jumps.pending(event.getPlayer())) later.accept(() -> {
            if (event.isCancelled() || original != null && !original.equals(event.getTo())) jumps.invalidate(event.getPlayer());
        });
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void velocity(PlayerVelocityEvent event) {
        if (event.isCancelled()) jumps.invalidate(event.getPlayer());
        if (jumps.pending(event.getPlayer())) later.accept(() -> jumps.velocity(event.getPlayer(), event.getVelocity(), event.isCancelled()));
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void teleport(PlayerTeleportEvent event) { jumps.invalidate(event.getPlayer()); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void world(PlayerChangedWorldEvent event) { jumps.invalidate(event.getPlayer()); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void death(PlayerDeathEvent event) { jumps.invalidate(event.getEntity()); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void mode(PlayerGameModeChangeEvent event) { jumps.invalidate(event.getPlayer()); }
    @EventHandler(priority = EventPriority.MONITOR)
    public void quit(PlayerQuitEvent event) { jumps.forget(event.getPlayer()); }
}
