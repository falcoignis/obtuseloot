package obtuseloot.movement;

import obtuseloot.abilities.*;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import java.util.function.BiPredicate;

/** Internal physical-input envelope; only its coordinator can apply and later measure the effect. */
public final class AirJumpRequest extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final BiPredicate<AbilityDefinition, AbilityEventContext> apply;
    public AirJumpRequest(BiPredicate<AbilityDefinition, AbilityEventContext> apply) { this.apply = apply; }
    public boolean apply(AbilityDefinition definition, AbilityEventContext context) { return apply.test(definition, context); }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
