package obtuseloot.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;

/** Protection/integration veto before an artifact changes velocity. Does not replace vanilla events. */
public final class ArtifactAirJumpEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String artifactKey, abilityId;
    private final Vector velocity;
    private final long recoveryTicks;
    private boolean cancelled;
    public ArtifactAirJumpEvent(Player player, String artifactKey, String abilityId, Vector velocity) {
        this(player, artifactKey, abilityId, velocity, 0);
    }
    public ArtifactAirJumpEvent(Player player, String artifactKey, String abilityId, Vector velocity, long recoveryTicks) {
        super(player); this.artifactKey = artifactKey; this.abilityId = abilityId; this.velocity = velocity.clone();
        if (recoveryTicks < 0) throw new IllegalArgumentException("Negative recovery");
        this.recoveryTicks = recoveryTicks;
    }
    public String getArtifactKey() { return artifactKey; }
    public String getAbilityId() { return abilityId; }
    public Vector getVelocity() { return velocity.clone(); }
    public long getRecoveryTicks() { return recoveryTicks; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
