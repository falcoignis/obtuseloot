package obtuseloot.equipment;

import obtuseloot.ObtuseLoot;
import obtuseloot.abilities.*;
import obtuseloot.artifacts.*;
import obtuseloot.ecology.WorldPosition;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;
import java.util.*;
import java.util.logging.Level;

/** Server-thread equipment reconciliation, dispatch and passive leases share the same catalog. */
public final class EquipmentAbilityService {
    public record Token(UUID player, Map<EquipmentSlot, EquippedArtifact> bindings, AbilityInput input, UUID world, long tick) { }
    private final ArtifactManager artifacts;
    private final ItemAbilityManager abilities;
    private final obtuseloot.reputation.ReputationManager reputations;
    private final obtuseloot.combat.CombatContextManager combat;
    private final obtuseloot.evolution.ArtifactUsageTracker usage;
    private final java.util.function.Function<org.bukkit.inventory.ItemStack, String> keyReader;
    private final java.util.function.Consumer<Artifact> flush;
    private final java.util.logging.Logger logger;
    private final java.util.function.Supplier<Collection<? extends Player>> online;
    private final EquipmentBindings bindings = new EquipmentBindings();
    private final EquipmentViews views = new EquipmentViews();
    private final PassiveEffectController passives;
    private record ActionKey(UUID player, long tick, AbilityTrigger trigger, String target) { }
    private final Map<ActionKey, Set<String>> secondaryActions = new HashMap<>();
    private long ticks;
    private final Set<UUID> initialized = new HashSet<>();
    private final Set<UUID> suspended = new HashSet<>();
    public EquipmentAbilityService(ObtuseLoot plugin) {
        this(plugin.getArtifactManager(), plugin.getItemAbilityManager(), plugin.getReputationManager(),
                plugin.getCombatContextManager(), plugin.getArtifactUsageTracker(), plugin.getArtifactItemStorage()::readStorageKey,
                plugin.getEngineScheduler()::flushEvolution, plugin.getLogger(), () -> plugin.getServer().getOnlinePlayers());
    }
    public EquipmentAbilityService(ArtifactManager artifacts, ItemAbilityManager abilities,
            obtuseloot.reputation.ReputationManager reputations, obtuseloot.combat.CombatContextManager combat,
            obtuseloot.evolution.ArtifactUsageTracker usage,
            java.util.function.Function<org.bukkit.inventory.ItemStack, String> keyReader,
            java.util.function.Consumer<Artifact> flush, java.util.logging.Logger logger,
            java.util.function.Supplier<Collection<? extends Player>> online) {
        this.artifacts = artifacts; this.abilities = abilities; this.reputations = reputations;
        this.combat = combat; this.usage = usage; this.keyReader = keyReader; this.flush = flush;
        this.logger = logger; this.online = online;
        passives = new PassiveEffectController(e -> logger.log(Level.SEVERE, "Passive lifecycle failed; retaining ownership for retry", e));
    }
    public void tick() {
        ++ticks;
        secondaryActions.keySet().removeIf(key -> key.tick < ticks - 2);
        if (ticks % 5 == 0) for (Player player : online.get()) {
            try { reconcile(player); }
            catch (RuntimeException exception) { logger.log(Level.SEVERE, "Equipment reconciliation failed for " + player.getUniqueId(), exception); }
        }
    }
    public EquipmentViews views() { return views; }
    public Map<EquipmentSlot, EquippedArtifact> equipped(UUID player) { return bindings.get(player); }
    public Artifact mainHand(Player player) {
        reconcile(player);
        var binding = equipped(player.getUniqueId()).get(EquipmentSlot.HAND);
        return binding == null ? null : binding.artifact();
    }
    public static boolean permitted(Player player, AbilityRegistration registration) {
        return player.hasPermission("obtuseloot.abilities") && player.hasPermission(registration.permission());
    }
    public void reconcile(Player player) {
        UUID id = player.getUniqueId();
        if (!player.isOnline() || suspended.contains(id)) { release(id); return; }
        if (initialized.add(id)) for (var registration : abilities.registry().registrations())
            if (registration.passiveEffect() != null) registration.passiveEffect().cleanupPlayer(player);
        var manager = artifacts;
        var previous = equipped(id);
        var candidates = new EnumMap<EquipmentSlot, Artifact>(EquipmentSlot.class);
        var keys = new HashSet<String>();
        for (var slot : EquipmentBindings.SLOTS) {
            String key = keyReader.apply(player.getInventory().getItem(slot));
            if (ArtifactStorageKeys.isItem(key)) keys.add(key);
        }
        manager.pinEquipment(id, keys);
        for (var slot : EquipmentBindings.SLOTS) {
            var stack = player.getInventory().getItem(slot);
            String itemKey = keyReader.apply(stack);
            if (!ArtifactStorageKeys.isItem(itemKey)) continue;
            try {
                Artifact artifact = artifacts.resolveByStorageKey(itemKey);
                if (artifact != null) candidates.put(slot, artifact);
            } catch (obtuseloot.persistence.PlayerStateLoadException unavailable) {
                // The shared load guard reports the failure and applies retry backoff.
            } catch (RuntimeException exception) {
                logger.log(Level.SEVERE, "Refusing equipped artifact in " + slot + " for " + id, exception);
            }
        }
        var current = bindings.reconcile(id, candidates);
        for (var old : previous.values()) {
            if (current.values().stream().anyMatch(old::sameIdentity)) continue;
            flush.accept(old.artifact());
            abilities.clearSubscriptions(id, old.key());
            try { manager.saveItem(old.artifact()); }
            catch (RuntimeException exception) { logger.log(Level.SEVERE, "Unequipped artifact save failed; retaining dirty profile", exception); }
        }
        manager.pinEquipment(id, current.values().stream().map(EquippedArtifact::key).collect(java.util.stream.Collectors.toSet()));
        for (var binding : current.values()) manager.attachEquippedItem(id, binding.artifact());
        bindMainHand(player, current.get(EquipmentSlot.HAND));
        var requests = new ArrayList<PassiveEffectController.Request>();
        var names = new EnumMap<EquipmentSlot, List<String>>(EquipmentSlot.class);
        for (var slot : EquipmentBindings.SLOTS) {
            var binding = current.get(slot);
            if (binding == null) continue;
            var display = new ArrayList<String>();
            for (var definition : definitions(id, binding.artifact())) {
                var registration = abilities.registry().registrationById(definition.id());
                if (registration == null || !registration.supports(definition)
                        || !registration.activation().slots().contains(slot) || !permitted(player, registration)) continue;
                display.add(definition.name());
                if (registration.passiveEffect() == null || player.isDead()) continue;
                try {
                    var contribution = registration.passiveEffect().contribution(player, binding.artifact(), definition);
                    if (contribution != null) requests.add(new PassiveEffectController.Request(binding.key(), definition.id(), contribution));
                } catch (RuntimeException exception) { logger.log(Level.SEVERE, "Passive evaluation failed for " + definition.id(), exception); }
            }
            names.put(slot, List.copyOf(display));
        }
        passives.reconcile(id, requests);
        views.publish(id, current, names, passives.activeAbilities(id));
    }
    private List<AbilityDefinition> definitions(UUID player, Artifact artifact) {
        return abilities.equippedDefinitions(player, artifact);
    }
    private void bindMainHand(Player player, EquippedArtifact binding) {
        UUID id = player.getUniqueId(); var manager = artifacts;
        Artifact current = manager.getLoadedArtifacts().get(id);
        Artifact next = binding == null ? null : binding.artifact();
        if (current == next && (next == null || reputations.getLoadedReputations().get(id) == next.getItemReputation())) return;
        if (current != null) { flush.accept(current); manager.unload(id); }
        reputations.unbindItem(id);
        combat.remove(id);
        if (next != null) { manager.activateItem(id, next); reputations.bindItem(id, next); }
    }
    public Token capture(Player player, AbilityInput input) {
        reconcile(player);
        return new Token(player.getUniqueId(), equipped(player.getUniqueId()), input, player.getWorld().getUID(), ticks);
    }
    public AbilityDispatchResult dispatch(Player player, Token token, AbilityTrigger trigger, double value,
                                          String source, AbilityRuntimeContext runtime, Event event) {
        if (event instanceof org.bukkit.event.Cancellable cancellable && cancellable.isCancelled()) return empty();
        if (event instanceof org.bukkit.event.player.PlayerInteractEvent interaction
                && (interaction.useInteractedBlock() == Event.Result.DENY || interaction.useItemInHand() == Event.Result.DENY)) return empty();
        if (!player.isOnline() || player.isDead() || !player.getUniqueId().equals(token.player) || !player.getWorld().getUID().equals(token.world)) return empty();
        reconcile(player);
        Set<String> handled = token.input.kind() == AbilityInput.Kind.SECONDARY_CLICK
                ? secondaryActions.computeIfAbsent(new ActionKey(token.player, token.tick, trigger, target(event, token)), ignored -> new HashSet<>())
                : Set.of();
        var results = new ArrayList<AbilityExecutionResult>();
        var claimedAbilities = new HashSet<String>();
        AbilityEventContext first = null;
        for (var slot : EquipmentBindings.SLOTS) {
            var binding = equipped(token.player).get(slot);
            if (binding == null || !binding.sameIdentity(token.bindings.get(slot))) continue;
            var allowed = new HashSet<String>();
            for (var definition : definitions(token.player, binding.artifact())) {
                var registration = abilities.registry().registrationById(definition.id());
                if (definition.trigger() != trigger || registration == null || registration.liveEffect() == null
                        || !registration.supports(definition) || !registration.activation().accepts(slot, token.input)
                        || !permitted(player, registration)) continue;
                if (handled.contains(definition.id())) continue;
                if (claimedAbilities.add(definition.id())) allowed.add(definition.id());
            }
            if (allowed.isEmpty()) continue;
            var artifact = binding.artifact();
            var context = new AbilityEventContext(trigger, artifact, artifact.getItemReputation(), value, source, runtime, event, true)
                    .withActivation(new AbilityActivation(slot, token.input, allowed));
            if (context.position() == null) context = context.withPosition(WorldPosition.from(player.getLocation()));
            if (first == null) first = context;
            usage.trackUse(artifact);
            var dispatched = abilities.resolveDispatch(context);
            results.addAll(dispatched.executions());
            if (token.input.kind() == AbilityInput.Kind.SECONDARY_CLICK) for (var result : dispatched.executions()) {
                if (result.status() == AbilityExecutionStatus.SUCCESS || result.status() == AbilityExecutionStatus.NO_OP) handled.add(result.abilityId());
            }
            artifacts.markDirty(artifact);
        }
        return new AbilityDispatchResult(first, List.copyOf(results));
    }
    private String target(Event event, Token token) {
        if (event instanceof org.bukkit.event.player.PlayerInteractAtEntityEvent interaction)
            return "entity:" + interaction.getRightClicked().getUniqueId();
        if (event instanceof org.bukkit.event.player.PlayerInteractEvent interaction && interaction.getClickedBlock() != null) {
            var block = interaction.getClickedBlock();
            return "block:" + token.world + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ() + ":" + token.input.action();
        }
        var position = WorldPosition.fromEvent(event);
        return token.world + ":" + token.input.action() + ":" + (position == null ? "input" : position.x() + ":" + position.z());
    }
    private static AbilityDispatchResult empty() { return new AbilityDispatchResult(null, List.of()); }
    public void suspend(Player player) { suspended.add(player.getUniqueId()); release(player.getUniqueId()); }
    public void resume(Player player) { suspended.remove(player.getUniqueId()); reconcile(player); }
    public void release(UUID player) {
        for (var binding : equipped(player).values()) {
            flush.accept(binding.artifact());
            try { artifacts.saveItem(binding.artifact()); }
            catch (RuntimeException exception) { logger.log(Level.SEVERE, "Equipment release save failed; retaining dirty profile", exception); }
        }
        bindings.clear(player); passives.clear(player); views.remove(player);
        secondaryActions.keySet().removeIf(key -> key.player.equals(player));
        artifacts.pinEquipment(player, Set.of());
        abilities.clearSubscriptions(player);
    }
    public void forget(Player player) {
        release(player.getUniqueId()); suspended.remove(player.getUniqueId()); initialized.remove(player.getUniqueId());
    }
    public void shutdown() {
        for (Player player : online.get()) forget(player);
        passives.clearAll(); views.clear();
    }
}
