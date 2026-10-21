package obtuseloot.movement;

import java.util.UUID;

/** One charge per landing, owned by the player rather than a particular equipment copy. */
public final class AirJumpState {
    private UUID world;
    private boolean armed, used, down;
    private long serial, reserved, readyAt;
    public void frame(UUID world, boolean grounded, boolean eligible) {
        if (!world.equals(this.world)) { invalidate(); this.world = world; }
        if (!eligible) { invalidate(); return; }
        if (grounded) { armed = true; used = false; reserved = 0; }
    }
    public long input(boolean jump, boolean grounded, long tick) {
        boolean pressed = jump && !down; down = jump;
        if (!pressed || grounded || !armed || used || reserved != 0 || tick < readyAt) return 0;
        reserved = ++serial;
        return reserved;
    }
    public boolean reserved(long ticket) { return ticket != 0 && reserved == ticket && armed && !used; }
    public void complete(long ticket, boolean applied, long tick, long cooldown) {
        if (!reserved(ticket)) return;
        reserved = 0;
        if (applied) { used = true; readyAt = tick + cooldown; }
    }
    public void invalidate() { armed = false; reserved = 0; ++serial; }
}
