package obtuseloot.movement;

import obtuseloot.ObtuseLoot;
import obtuseloot.abilities.*;
import obtuseloot.config.RuntimeSettings;
import obtuseloot.equipment.EquipmentAbilityService;
import obtuseloot.events.ArtifactAirJumpEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.Vector;
import java.util.*;
import java.util.function.*;

/** Server-thread coordinator. Input/charges belong to the player; effects and credit belong to selected gear. */
public final class AirJumpService {
    private final EquipmentAbilityService equipment;
    private final ItemAbilityManager abilities;
    private final Consumer<Runnable> later;
    private final Consumer<Event> events;
    private final Supplier<Collection<? extends Player>> online;
    private final Map<UUID, AirJumpState> states = new HashMap<>();
    private final Map<UUID, Motion> motions = new HashMap<>();
    private long ticks;
    private static final class Motion {
        final EquipmentAbilityService.Token token;
        final AbilityDefinition definition;
        final AbilityEventContext context;
        final Vector velocity;
        final double y;
        final long tick;
        boolean seen, denied;
        Motion(EquipmentAbilityService.Token token, AbilityDefinition definition, AbilityEventContext context,
               Vector velocity, double y, long tick) {
            this.token = token; this.definition = definition; this.context = context;
            this.velocity = velocity.clone(); this.y = y; this.tick = tick;
        }
    }
    public AirJumpService(ObtuseLoot plugin) {
        this(plugin.getEquipmentAbilities(), plugin.getItemAbilityManager(),
                task -> plugin.getServer().getScheduler().runTask(plugin, task),
                plugin.getServer().getPluginManager()::callEvent, () -> plugin.getServer().getOnlinePlayers());
    }
    public AirJumpService(EquipmentAbilityService equipment, ItemAbilityManager abilities, Consumer<Runnable> later,
                          Consumer<Event> events, Supplier<Collection<? extends Player>> online) {
        this.equipment = equipment; this.abilities = abilities; this.later = later; this.events = events; this.online = online;
    }
    public static boolean eligible(Player player) {
        return RuntimeSettings.get().doubleJumpEnabled() && player.isOnline() && !player.isDead()
                && (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)
                && !player.getAllowFlight() && !player.isFlying() && !player.isGliding() && !player.isInsideVehicle()
                && !player.isSwimming() && !player.isInWater() && !player.isInLava() && !player.isClimbing()
                && !player.isRiptiding() && !player.isSleeping() && player.hasGravity()
                && player.getActivePotionEffects().stream().noneMatch(effect -> effect.getType().key().value().equals("levitation"))
                && (player.getInventory().getChestplate() == null
                    || player.getInventory().getChestplate().getType() != Material.ELYTRA);
    }
    private AirJumpState frame(Player player) {
        var state = states.computeIfAbsent(player.getUniqueId(), id -> new AirJumpState());
        state.frame(player.getWorld().getUID(), player.isOnGround(), eligible(player));
        return state;
    }
    public void tick() {
        ++ticks;
        for (Player player : online.get()) {
            frame(player);
            Motion motion = motions.get(player.getUniqueId());
            if (motion == null || ticks - motion.tick < 2) continue;
            if (!eligible(player) || !equipment.isCurrentActivation(player, motion.token, motion.definition, motion.context)) {
                motions.remove(player.getUniqueId()); continue;
            }
            double rise = player.getLocation().getY() - motion.y;
            if (!motion.denied && (!motion.seen || rise <= 0.02) && ticks - motion.tick < 6) continue;
            motions.remove(player.getUniqueId());
            double observed = !motion.denied && motion.seen && rise > 0.02 ? 1 : 0;
            var result = AbilityExecutionResult.observed(motion.definition, motion.context, AbilityOutcomeType.WORLD_INTERACTION,
                    observed, observed > 0 ? null : "movement-not-observed", null)
                    .withObservationKey(motion.token.world() + ":air-jump:" + motion.tick);
            abilities.observeDeferred(motion.definition, motion.context, result);
        }
    }
    public void input(Player player, boolean down) {
        AirJumpState state = frame(player);
        long ticket = state.input(down, player.isOnGround(), ticks);
        if (ticket == 0) return;
        var token = equipment.capture(player, AbilityInput.jump());
        later.accept(() -> {
            if (!state.reserved(ticket)) return;
            try {
                var request = new AirJumpRequest((definition, context) -> apply(player, state, ticket, token, definition, context));
                equipment.dispatch(player, token, AbilityTrigger.ON_AIR_JUMP, 1, "native-jump-input", null, request);
            } finally { state.complete(ticket, false, ticks, 0); }
        });
    }
    private boolean apply(Player player, AirJumpState state, long ticket, EquipmentAbilityService.Token token,
                          AbilityDefinition definition, AbilityEventContext context) {
        if (!state.reserved(ticket) || !eligible(player) || player.isOnGround()
                || !equipment.isCurrentActivation(player, token, definition, context)) return false;
        Vector before = player.getVelocity();
        double upward = RuntimeSettings.get().doubleJumpVelocity();
        // Never reduce a stronger launch or take over somebody else's current impulse.
        if (!Double.isFinite(before.getX()) || !Double.isFinite(before.getY()) || !Double.isFinite(before.getZ()) || before.getY() >= upward) return false;
        Vector velocity = before.clone().setY(upward);
        var event = new ArtifactAirJumpEvent(player, context.artifact().getArtifactStorageKey(), definition.id(), velocity);
        events.accept(event);
        if (event.isCancelled() || !state.reserved(ticket) || !eligible(player) || player.isOnGround()
                || !equipment.isCurrentActivation(player, token, definition, context) || !before.equals(player.getVelocity())) return false;
        var motion = new Motion(token, definition, context, velocity, player.getLocation().getY(), ticks);
        motions.put(player.getUniqueId(), motion);
        try { player.setVelocity(velocity); }
        catch (RuntimeException exception) { motions.remove(player.getUniqueId(), motion); throw exception; }
        state.complete(ticket, true, ticks, RuntimeSettings.get().doubleJumpCooldownTicks());
        return true;
    }
    /** Observe the server's ordinary velocity event; never emit, modify or uncancel one ourselves. */
    public void velocity(Player player, Vector velocity, boolean cancelled) {
        Motion motion = motions.get(player.getUniqueId());
        if (motion == null) return;
        if (cancelled || motion.velocity.distanceSquared(velocity) > 1.0e-10) motion.denied = true;
        else motion.seen = true;
    }
    public void invalidate(Player player) {
        AirJumpState state = states.get(player.getUniqueId());
        if (state != null) state.invalidate();
        motions.remove(player.getUniqueId());
    }
    public void forget(Player player) { invalidate(player); states.remove(player.getUniqueId()); }
    public boolean pending(Player player) { return motions.containsKey(player.getUniqueId()); }
    public void clear() { states.values().forEach(AirJumpState::invalidate); states.clear(); motions.clear(); }
}
