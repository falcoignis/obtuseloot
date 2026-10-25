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
    private final LongSupplier itemClock;
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
        final long recoveryTicks;
        final obtuseloot.abilities.genome.ArtifactGenome genome;
        double peakRise;
        boolean seen, denied;
        Motion(EquipmentAbilityService.Token token, AbilityDefinition definition, AbilityEventContext context,
               Vector velocity, double y, long tick, long recoveryTicks,
               obtuseloot.abilities.genome.ArtifactGenome genome) {
            this.token = token; this.definition = definition; this.context = context;
            this.velocity = velocity.clone(); this.y = y; this.tick = tick;
            this.recoveryTicks = recoveryTicks; this.genome = genome;
        }
    }
    public AirJumpService(ObtuseLoot plugin) {
        this(plugin.getEquipmentAbilities(), plugin.getItemAbilityManager(),
                task -> plugin.getServer().getScheduler().runTask(plugin, task),
                plugin.getServer().getPluginManager()::callEvent, () -> plugin.getServer().getOnlinePlayers(),
                () -> plugin.getLocalEcosystem() == null ? -1 : plugin.getLocalEcosystem().pool().gameTick());
    }
    public AirJumpService(EquipmentAbilityService equipment, ItemAbilityManager abilities, Consumer<Runnable> later,
                          Consumer<Event> events, Supplier<Collection<? extends Player>> online) {
        this(equipment, abilities, later, events, online, () -> -1);
    }
    /** {@code itemClock} is the persisted ecology tick that item recovery uses; negative means unavailable. */
    public AirJumpService(EquipmentAbilityService equipment, ItemAbilityManager abilities, Consumer<Runnable> later,
                          Consumer<Event> events, Supplier<Collection<? extends Player>> online, LongSupplier itemClock) {
        this.equipment = equipment; this.abilities = abilities; this.later = later; this.events = events; this.online = online;
        this.itemClock = Objects.requireNonNull(itemClock);
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
            if (motion == null || ticks - motion.tick < 1) continue;
            if (!eligible(player) || !equipment.isCurrentActivation(player, motion.token, motion.definition, motion.context)
                    || motion.context.artifact().getGenome() != motion.genome) {
                motions.remove(player.getUniqueId()); continue;
            }
            double rise = player.getLocation().getY() - motion.y;
            if (Double.isFinite(rise)) motion.peakRise = Math.max(motion.peakRise, rise);
            if (!motion.denied && ticks - motion.tick < 6) continue;
            motions.remove(player.getUniqueId());
            double observed = !motion.denied && motion.seen && motion.peakRise > 0.02 ? motion.peakRise : 0;
            var result = AbilityExecutionResult.observed(motion.definition, motion.context, AbilityOutcomeType.WORLD_INTERACTION,
                    observed, observed > 0 ? null : "movement-not-observed", null)
                    .withObservationKey(motion.token.world() + ":air-jump:" + motion.tick)
                    .withPerformance(new AbilityPerformanceMeasurement(observed, motion.recoveryTicks));
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
        // The item's shared recovery runs on the persisted ecology clock, not this service's counter.
        // Without that clock only the player's own cooldown applies.
        long itemTick = itemClock.getAsLong();
        if (itemTick >= 0 && itemTick < context.artifact().abilitiesReadyAt()) return false;
        Vector before = player.getVelocity();
        var genome = context.artifact().getGenome();
        var registration = abilities.registry().registrationById(definition.id());
        var performance = registration.performanceModel().express(genome);
        var settings = RuntimeSettings.get();
        double upward = performance.output(settings.doubleJumpVelocity());
        long recoveryTicks = performance.recoveryTicks(settings.doubleJumpCooldownTicks());
        Math.addExact(ticks, recoveryTicks); // Reject an overflowing deadline before submitting any motion.
        long itemReadyAt = itemTick >= 0 ? Math.addExact(itemTick, recoveryTicks) : 0;
        // Never reduce a stronger launch or take over somebody else's current impulse.
        if (!Double.isFinite(before.getX()) || !Double.isFinite(before.getY()) || !Double.isFinite(before.getZ()) || before.getY() >= upward) return false;
        Vector velocity = before.clone().setY(upward);
        var event = new ArtifactAirJumpEvent(player, context.artifact().getArtifactStorageKey(), definition.id(), velocity, recoveryTicks);
        events.accept(event);
        if (event.isCancelled() || !state.reserved(ticket) || !eligible(player) || player.isOnGround()
                || !equipment.isCurrentActivation(player, token, definition, context) || !before.equals(player.getVelocity())
                || context.artifact().getGenome() != genome) return false;
        var motion = new Motion(token, definition, context, velocity, player.getLocation().getY(), ticks, recoveryTicks, genome);
        motions.put(player.getUniqueId(), motion);
        try { player.setVelocity(velocity); }
        catch (RuntimeException exception) { motions.remove(player.getUniqueId(), motion); throw exception; }
        state.complete(ticket, true, ticks, recoveryTicks);
        if (itemTick >= 0) context.artifact().setAbilityReadyAt(definition.id(), itemReadyAt);
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
