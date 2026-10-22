package obtuseloot.equipment;

import obtuseloot.abilities.*;
import obtuseloot.artifacts.*;
import obtuseloot.combat.CombatContextManager;
import obtuseloot.evolution.*;
import obtuseloot.persistence.PlayerStateStore;
import obtuseloot.reputation.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.*;
import org.junit.jupiter.api.*;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class EquipmentAbilityServiceTest {
    private final UUID owner = UUID.randomUUID();
    private final Map<EquipmentSlot, ItemStack> inventory = new EnumMap<>(EquipmentSlot.class);
    private final Set<String> denied = new HashSet<>();
    private final Map<String, List<AbilityDefinition>> profiles = new HashMap<>();
    private final Set<String> applied = new HashSet<>();
    private final List<AbilityEventContext> executions = new ArrayList<>();
    private final AtomicInteger builds = new AtomicInteger();
    private final NonCombatEvolutionQueue queue = new NonCombatEvolutionQueue();
    private EquipmentAbilityService service;
    private ArtifactManager artifacts;
    private ItemAbilityManager abilities;
    private Player player;
    private World world;
    private AbilityTemplate inspect;
    private AbilityTemplate passive;
    private boolean online = true;
    private boolean dead;

    @BeforeEach void setup() {
        var catalog = new AbilityRegistry();
        inspect = catalog.templateById("precision.material_insight"); passive = catalog.templateById("mobility.fleetfoot");
        var registry = new AbilityRegistry(List.of(new AbilityRegistration(inspect, (d, c, stage) -> {
            executions.add(c);
            return AbilityExecutionResult.observed(d, c, AbilityOutcomeType.INFORMATION, 1, null, "observed")
                    .withObservationKey("action-" + executions.size());
        }), new AbilityRegistration(passive, null).withPassiveEffect(AbilityActivationPolicy.EQUIPMENT, (p, a, d) ->
                new PassiveAbilityEffect.Contribution("test-speed-" + a.getArtifactStorageKey(), .05, "v1", () -> {
                    assertTrue(applied.add(a.getArtifactStorageKey())); return () -> applied.remove(a.getArtifactStorageKey());
                }))));
        var store = new Store(); artifacts = new ArtifactManager(store);
        abilities = new ItemAbilityManager((a, r) -> { builds.incrementAndGet(); return new AbilityProfile("test", profiles.getOrDefault(a.getArtifactStorageKey(), List.of())); }, registry, new ArtifactUsageTracker());
        abilities.setOutcomeObserver(queue::observe);
        world = api(World.class, (name, args) -> switch (name) { case "getUID" -> new UUID(0, 5); default -> unexpected(name); });
        PlayerInventory inv = api(PlayerInventory.class, (name, args) -> switch (name) { case "getItem" -> inventory.get(args[0]); default -> unexpected(name); });
        player = api(Player.class, (name, args) -> switch (name) {
            case "getUniqueId" -> owner; case "getInventory" -> inv; case "getWorld" -> world;
            case "getLocation" -> new Location(world, 1, 64, 1); case "isOnline" -> online; case "isDead" -> dead;
            case "hasPermission" -> !denied.contains(args[0]); case "isOp" -> true;
            default -> unexpected(name);
        });
        service = new EquipmentAbilityService(artifacts, abilities, new ReputationManager(store), new CombatContextManager(),
                new ArtifactUsageTracker(), item -> item == null ? null : ((Stack) item).key,
                a -> queue.flush(a, update -> { }, (id, e) -> fail(e)), Logger.getLogger("EquipmentAbilityServiceTest"), () -> List.of(player));
    }
    @AfterEach void close() { service.shutdown(); }

    @Test void allSlotsHaveIndependentPassivesAndIdleReconciliationDoesNotReroll() {
        for (var slot : EquipmentBindings.SLOTS) equip(slot, slot == EquipmentSlot.CHEST ? "elytra" : "diamond_sword", passive);
        service.reconcile(player);
        assertEquals(6, service.equipped(owner).size()); assertEquals(6, applied.size()); assertEquals(6, builds.get());
        for (int i = 0; i < 30; i++) service.reconcile(player);
        assertEquals(6, builds.get()); assertEquals(0, queue.pendingCount());
        assertEquals("6", service.views().value(owner, "equipped_count"));
        inventory.remove(EquipmentSlot.CHEST); service.reconcile(player);
        assertEquals(5, applied.size()); assertEquals("false", service.views().value(owner, "chest_present"));
    }
    @Test void offhandSecondaryClickUsesItsOwnProfileAndBothHandsDoNotDuplicateOneAbility() {
        var off = equip(EquipmentSlot.OFF_HAND, "diamond_sword", inspect);
        var input = AbilityInput.secondary(EquipmentSlot.OFF_HAND, Action.RIGHT_CLICK_BLOCK);
        dispatch(service.capture(player, input));
        assertEquals(1, executions.size()); assertSame(off, executions.getFirst().artifact());
        assertEquals(EquipmentSlot.OFF_HAND, executions.getFirst().activation().slot());
        var main = equip(EquipmentSlot.HAND, "diamond_sword", inspect);
        service.tick(); // a new physical action in the next game tick
        dispatch(service.capture(player, AbilityInput.secondary(EquipmentSlot.HAND, Action.RIGHT_CLICK_BLOCK)));
        dispatch(service.capture(player, input));
        assertEquals(2, executions.size()); assertSame(main, executions.getLast().artifact());
        assertEquals(2, queue.pendingCount(), "Same holder retains two distinct item windows");
    }
    @Test void cancelledMainHandDoesNotConsumeTheOffhandActionOrChangeTheOriginalEvent() {
        equip(EquipmentSlot.HAND, "diamond_sword", inspect);
        var off = equip(EquipmentSlot.OFF_HAND, "diamond_sword", inspect);
        class Denied extends org.bukkit.event.Event implements org.bukkit.event.Cancellable {
            boolean cancelled = true;
            public org.bukkit.event.HandlerList getHandlers() { return new org.bukkit.event.HandlerList(); }
            public boolean isCancelled() { return cancelled; }
            public void setCancelled(boolean value) { cancelled = value; }
        }
        var deniedEvent = new Denied();
        var mainToken = service.capture(player, AbilityInput.secondary(EquipmentSlot.HAND, Action.RIGHT_CLICK_BLOCK));
        service.dispatch(player, mainToken, inspect.trigger(), 1, "test", AbilityRuntimeContext.intentional(AbilitySource.OTHER), deniedEvent);
        assertTrue(executions.isEmpty()); assertTrue(deniedEvent.isCancelled());
        dispatch(service.capture(player, AbilityInput.secondary(EquipmentSlot.OFF_HAND, Action.RIGHT_CLICK_BLOCK)));
        assertEquals(1, executions.size()); assertSame(off, executions.getFirst().artifact());
        assertTrue(deniedEvent.isCancelled(), "Never undo another plugin's protection/XP decision");
    }

    @Test void disablingDispatchIndexDoesNotReselectOrInvalidateAnEquippedAbility() {
        var item = equip(EquipmentSlot.HAND, "diamond_sword", inspect);
        abilities.setTriggerSubscriptionIndexingEnabled(false);
        var token = service.capture(player, AbilityInput.secondary(EquipmentSlot.HAND, Action.RIGHT_CLICK_BLOCK));
        int initialBuilds = builds.get();
        profiles.put(item.getArtifactStorageKey(), List.of(definition(passive)));
        dispatch(token);
        assertEquals(1, executions.size(), "Unindexed dispatch must execute the selected equipped profile, not select a different one");
        assertEquals(initialBuilds, builds.get(), "Dispatch mode must not cause an extra selection roll");
    }

    @Test void deferredActionRejectsReplacementSwapAndReseeding() {
        var original = equip(EquipmentSlot.OFF_HAND, "diamond_sword", inspect);
        var input = AbilityInput.secondary(EquipmentSlot.OFF_HAND, Action.RIGHT_CLICK_BLOCK);
        var token = service.capture(player, input);
        equip(EquipmentSlot.OFF_HAND, "diamond_sword", inspect); dispatch(token); assertTrue(executions.isEmpty());
        inventory.put(EquipmentSlot.OFF_HAND, new Stack(original.getArtifactStorageKey()));
        token = service.capture(player, input); original.setArtifactSeed(-17); dispatch(token); assertTrue(executions.isEmpty());
        token = service.capture(player, input);
        inventory.put(EquipmentSlot.HAND, inventory.remove(EquipmentSlot.OFF_HAND));
        dispatch(token); assertTrue(executions.isEmpty());
    }
    @Test void effectivePermissionDenialsReleasePassivesAndBlockActionsEvenForOp() {
        equip(EquipmentSlot.CHEST, "elytra", passive); equip(EquipmentSlot.OFF_HAND, "diamond_sword", inspect);
        service.reconcile(player); assertEquals(1, applied.size());
        denied.add("obtuseloot.ability.mobility.fleetfoot"); service.reconcile(player); assertTrue(applied.isEmpty());
        denied.clear(); service.reconcile(player); assertEquals(1, applied.size());
        denied.add("obtuseloot.abilities");
        dispatch(service.capture(player, AbilityInput.secondary(EquipmentSlot.OFF_HAND, Action.RIGHT_CLICK_BLOCK)));
        assertTrue(applied.isEmpty()); assertTrue(executions.isEmpty()); assertEquals(0, queue.pendingCount());
    }
    @Test void duplicateIdentityIsOnlyOnePassiveAndDeathDisconnectReleaseAll() {
        var a = equip(EquipmentSlot.CHEST, "elytra", passive);
        inventory.put(EquipmentSlot.OFF_HAND, new Stack(a.getArtifactStorageKey())); service.reconcile(player);
        assertEquals(1, applied.size()); assertEquals(1, service.equipped(owner).size());
        dead = true; service.suspend(player); assertTrue(applied.isEmpty());
        service.reconcile(player); assertTrue(applied.isEmpty());
        dead = false; service.resume(player); assertEquals(1, applied.size());
        online = false; service.forget(player); assertTrue(applied.isEmpty());
        assertEquals("0", service.views().value(owner, "equipped_count"));
    }
    @Test void anUnreadableTrackedSlotNeverCreatesFallbackOrDisablesHealthyGear() {
        equip(EquipmentSlot.CHEST, "elytra", passive);
        inventory.put(EquipmentSlot.HAND, new Stack(ArtifactStorageKeys.create()));
        service.reconcile(player); assertEquals(1, applied.size()); assertEquals(1, service.equipped(owner).size());
        assertNull(artifacts.getLoadedArtifacts().get(owner));
    }
    private void dispatch(EquipmentAbilityService.Token token) {
        service.dispatch(player, token, inspect.trigger(), 1, "test", AbilityRuntimeContext.intentional(AbilitySource.OTHER), null);
    }
    private Artifact equip(EquipmentSlot slot, String type, AbilityTemplate template) {
        var a = EquipmentBindingsTest.artifact(owner, type); artifacts.registerItem(a);
        profiles.put(a.getArtifactStorageKey(), List.of(definition(template)));
        inventory.put(slot, new Stack(a.getArtifactStorageKey())); return a;
    }
    private AbilityDefinition definition(AbilityTemplate t) {
        return new AbilityDefinition(t.id(), t.name(), t.family(), t.trigger(), t.mechanic(), "", "", "", "", "", "",
                List.of(), List.of(), t.metadata(), "1", "2", "3", "4", "5");
    }
    private static final class Stack extends ItemStack { final String key; Stack(String key) { super(); this.key = key; } }
    private static final class Store implements PlayerStateStore {
        final Map<String, Artifact> items = new HashMap<>();
        public Artifact loadArtifact(UUID id) { throw new AssertionError("Player profile fallback"); }
        public void saveArtifact(UUID id, Artifact a) { throw new AssertionError("Player profile fallback"); }
        public ArtifactReputation loadReputation(UUID id) { throw new AssertionError("Player reputation fallback"); }
        public void saveReputation(UUID id, ArtifactReputation r) { throw new AssertionError("Player reputation fallback"); }
        public Artifact loadArtifactProfile(String key) { return items.get(key); }
        public void saveArtifactProfile(Artifact a) { items.put(a.getArtifactStorageKey(), a); }
    }
    @FunctionalInterface private interface Call { Object call(String name, Object[] args); }
    private static <T> T api(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> switch (method.getName()) {
            case "toString" -> type.getSimpleName(); case "hashCode" -> System.identityHashCode(proxy); case "equals" -> proxy == args[0];
            default -> call.call(method.getName(), args);
        }));
    }
    private static Object unexpected(String name) { throw new AssertionError(name); }
}
