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
    private boolean grounded = true;
    private double altitude = 64;
    private org.bukkit.util.Vector velocity = new org.bukkit.util.Vector(.1, -.1, .2);
    private final Set<String> movementFlags = new HashSet<>();
    private obtuseloot.config.RuntimeSettings.Snapshot previousSettings;
    private final List<Runnable> movementTasks = new ArrayList<>();
    private final List<org.bukkit.event.Event> movementEvents = new ArrayList<>();
    private final List<AbilityOutcomeObservation> movementOutcomes = new ArrayList<>();
    private obtuseloot.movement.AirJumpService jumps;
    private java.util.function.Consumer<org.bukkit.event.Event> veto = e -> { };


    @BeforeEach void setup() {
        previousSettings = obtuseloot.config.RuntimeSettings.get();
        var settings = new org.bukkit.configuration.file.YamlConfiguration(); settings.set("abilities.double-jump.enabled", true);
        obtuseloot.config.RuntimeSettings.load(settings);
        var catalog = new AbilityRegistry();
        inspect = catalog.templateById("precision.material_insight"); passive = catalog.templateById("mobility.fleetfoot");
        var registry = new AbilityRegistry(List.of(catalog.registrationById("mobility.double_jump"), new AbilityRegistration(inspect, (d, c, stage) -> {
            executions.add(c);
            return AbilityExecutionResult.observed(d, c, AbilityOutcomeType.INFORMATION, 1, null, "observed")
                    .withObservationKey("action-" + executions.size());
        }), new AbilityRegistration(passive, null).withPassiveEffect(AbilityActivationPolicy.EQUIPMENT, (p, a, d) ->
                new PassiveAbilityEffect.Contribution("test-speed-" + a.getArtifactStorageKey(), .05, "v1", () -> {
                    assertTrue(applied.add(a.getArtifactStorageKey())); return () -> applied.remove(a.getArtifactStorageKey());
                }))));
        var store = new Store(); artifacts = new ArtifactManager(store);
        abilities = new ItemAbilityManager((a, r) -> { builds.incrementAndGet(); return new AbilityProfile("test", profiles.getOrDefault(a.getArtifactStorageKey(), List.of())); }, registry, new ArtifactUsageTracker());
        abilities.setOutcomeObserver(o -> { movementOutcomes.add(o); queue.observe(o); });
        world = api(World.class, (name, args) -> switch (name) { case "isChunkLoaded" -> false;
            case "getUID" -> new UUID(0, 5); default -> unexpected(name); });
        PlayerInventory inv = api(PlayerInventory.class, (name, args) -> switch (name) { case "getItem" -> inventory.get(args[0]); case "getChestplate" -> inventory.get(EquipmentSlot.CHEST); default -> unexpected(name); });
        player = api(Player.class, (name, args) -> switch (name) {
            case "getUniqueId" -> owner; case "getInventory" -> inv; case "getWorld" -> world;
            case "getLocation" -> new Location(world, 1, altitude, 1); case "isOnline" -> online; case "isDead" -> dead;
            case "isOnGround" -> grounded; case "getGameMode" -> org.bukkit.GameMode.SURVIVAL;
            case "getAllowFlight", "isFlying", "isGliding", "isInsideVehicle", "isSwimming", "isInWater", "isInLava", "isClimbing", "isRiptiding", "isSleeping" -> movementFlags.contains(name);
            case "getActivePotionEffects" -> List.of();
            case "hasGravity" -> !movementFlags.contains("noGravity");
            case "getVelocity" -> velocity.clone(); case "setVelocity" -> { velocity = ((org.bukkit.util.Vector) args[0]).clone(); yield null; }
            case "hasPermission" -> !denied.contains(args[0]); case "isOp" -> true;
            default -> unexpected(name);
        });
        service = new EquipmentAbilityService(artifacts, abilities, new ReputationManager(store), new CombatContextManager(),
                new ArtifactUsageTracker(), item -> item == null ? null : ((Stack) item).key,
                a -> queue.flush(a, update -> { }, (id, e) -> fail(e)), Logger.getLogger("EquipmentAbilityServiceTest"), () -> List.of(player));
    }
    @AfterEach void close() throws Exception {
        service.shutdown();
        var field = obtuseloot.config.RuntimeSettings.class.getDeclaredField("current"); field.setAccessible(true); field.set(null, previousSettings);
    }
    private void movement() {
        jumps = new obtuseloot.movement.AirJumpService(service, abilities, movementTasks::add,
                e -> { movementEvents.add(e); veto.accept(e); }, () -> List.of(player));
        jumps.tick();
    }
    private void press() { jumps.input(player, false); jumps.input(player, true); }
    private void runMovementTasks() { var tasks = List.copyOf(movementTasks); movementTasks.clear(); tasks.forEach(Runnable::run); }
    @Test void doubleJumpMeasuresNativeMotionOnceAndDoesNotStackAcrossGear() {
        var template = new AbilityRegistry().templateById("mobility.double_jump");
        var first = equip(EquipmentSlot.FEET, "diamond_boots", template);
        equip(EquipmentSlot.HEAD, "diamond_helmet", template); movement();
        jumps.input(player, true); runMovementTasks(); assertTrue(movementEvents.isEmpty(), "Ordinary ground jump stays vanilla");
        grounded = false; press(); runMovementTasks();
        assertEquals(1, movementEvents.size()); assertEquals(.42, velocity.getY(), 1e-9);
        assertEquals(.1, velocity.getX(), 1e-9); assertEquals(.2, velocity.getZ(), 1e-9);
        assertEquals(0, queue.pendingCount(), "An impulse write alone earns no outcome");
        assertTrue(movementOutcomes.isEmpty(), "Pending motion must not enter outcome history");
        jumps.velocity(player, velocity, false); altitude += .2;
        for (int i = 0; i < 6; i++) jumps.tick();
        assertEquals(1, queue.pendingCount());
        press(); runMovementTasks(); assertEquals(1, movementEvents.size());
        inventory.remove(EquipmentSlot.HEAD); inventory.remove(EquipmentSlot.FEET);
        equip(EquipmentSlot.LEGS, "diamond_leggings", template); press(); runMovementTasks();
        assertEquals(1, movementEvents.size(), "A new item cannot reset the player's charge");
    }
    @Test void doubleJumpSharesTheItemsRecoveryOnTheEcologyClock() {
        var template = new AbilityRegistry().templateById("mobility.double_jump");
        var item = equip(EquipmentSlot.FEET, "diamond_boots", template); item.setAbilityReadyAt("precision.material_insight", 50);
        var clock = new java.util.concurrent.atomic.AtomicLong(49);
        jumps = new obtuseloot.movement.AirJumpService(service, abilities, movementTasks::add,
                e -> { movementEvents.add(e); veto.accept(e); }, () -> List.of(player), clock::get);
        jumps.tick(); grounded = false;
        press(); runMovementTasks(); assertTrue(movementEvents.isEmpty(), "Another ability's recovery blocks the jump");
        assertEquals(-.1, velocity.getY(), 1e-9); assertEquals(0, item.abilityReadyAt("mobility.double_jump"));
        clock.set(50); press(); runMovementTasks(); assertEquals(.42, velocity.getY(), 1e-9, "The unspent charge works once recovered");
        long recovery = ((obtuseloot.events.ArtifactAirJumpEvent) movementEvents.getFirst()).getRecoveryTicks();
        assertEquals(50 + recovery, item.abilityReadyAt("mobility.double_jump")); assertEquals(50 + recovery, item.abilitiesReadyAt());
    }
    @Test void doubleJumpHonorsVetoPermissionAndDeferredSwapWithoutSpendingACharge() {
        var template = new AbilityRegistry().templateById("mobility.double_jump");
        equip(EquipmentSlot.FEET, "diamond_boots", template); movement(); grounded = false;
        press(); inventory.remove(EquipmentSlot.FEET); runMovementTasks(); assertTrue(movementEvents.isEmpty());
        equip(EquipmentSlot.FEET, "diamond_boots", template); denied.add("obtuseloot.ability.mobility.double_jump");
        press(); runMovementTasks(); assertTrue(movementEvents.isEmpty());
        denied.clear(); veto = e -> ((obtuseloot.events.ArtifactAirJumpEvent)e).setCancelled(true);
        press(); runMovementTasks(); assertEquals(-.1, velocity.getY(), 1e-9); assertEquals(0, queue.pendingCount());
        veto = e -> { }; press(); runMovementTasks(); assertEquals(.42, velocity.getY(), 1e-9);
    }
    @Test void doubleJumpRejectsForeignFlightAndVelocityCancellationEarnsNoSuccess() {
        var template = new AbilityRegistry().templateById("mobility.double_jump");
        equip(EquipmentSlot.FEET, "diamond_boots", template); movement();
        movementFlags.add("getAllowFlight"); jumps.tick(); grounded = false; press(); runMovementTasks(); assertTrue(movementEvents.isEmpty());
        movementFlags.clear(); jumps.tick(); press(); runMovementTasks(); assertTrue(movementEvents.isEmpty(), "Losing flight midair needs a new landing");
        grounded = true; jumps.tick(); grounded = false; press(); runMovementTasks();
        jumps.velocity(player, velocity, true); altitude += .2; jumps.tick(); jumps.tick();
        assertEquals(1, queue.pendingCount(), "A measured failure may feed the existing outcome path");
        assertFalse(firstOutcomePositive());
    }
    private boolean firstOutcomePositive() { return movementOutcomes.stream().anyMatch(o -> o.liveMeasured() && o.result().observedAmount() > 0); }


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
    @Test void doubleJumpRejectsSwimmingClimbingVehiclesAndOtherMovementOwners() {
        var template = new AbilityRegistry().templateById("mobility.double_jump");
        equip(EquipmentSlot.FEET, "diamond_boots", template); movement();
        for (String flag : List.of("isFlying", "isGliding", "isInsideVehicle", "isSwimming", "isInWater", "isInLava", "isClimbing", "isRiptiding", "isSleeping", "noGravity")) {
            grounded = true; movementFlags.clear(); jumps.tick();
            movementFlags.add(flag); jumps.tick(); grounded = false; press(); runMovementTasks();
            assertTrue(movementEvents.isEmpty(), flag); assertEquals(-.1, velocity.getY(), 1e-9, flag);
        }
    }
    @Test void doubleJumpVetoCallbackCannotTransferCreditOrOverwriteForeignLaunch() {
        var template = new AbilityRegistry().templateById("mobility.double_jump");
        var artifact = equip(EquipmentSlot.FEET, "diamond_boots", template); movement(); grounded = false;
        press(); artifact.setArtifactSeed(artifact.getArtifactSeed() - 1); runMovementTasks(); assertTrue(movementEvents.isEmpty());
        veto = e -> velocity = new org.bukkit.util.Vector(0, 1, 0);
        press(); runMovementTasks(); assertEquals(1, velocity.getY(), 1e-9); assertEquals(0, queue.pendingCount());
        jumps.invalidate(player); press(); runMovementTasks(); assertEquals(1, movementEvents.size());
    }
    @Test void inheritedPerformanceChangesTheImpulseAndSharedCooldownAndRecordsItsActualCost() {
        var template = new AbilityRegistry().templateById("mobility.double_jump");
        var item = equip(EquipmentSlot.FEET, "diamond_boots", template); kinetic(item, 1);
        movement(); grounded = false; press(); runMovementTasks();
        assertEquals(.525, velocity.getY(), 1e-12);
        assertEquals(90, ((obtuseloot.events.ArtifactAirJumpEvent) movementEvents.getFirst()).getRecoveryTicks());
        jumps.velocity(player, velocity, false); altitude += .7;
        for (int i = 0; i < 6; i++) jumps.tick();
        assertEquals(1, movementOutcomes.size());
        var result = movementOutcomes.getFirst().result();
        assertEquals(.7, result.observedAmount(), 1e-10); assertEquals(90, result.performance().recoveryTicks());
        assertTrue(movementOutcomes.getFirst().utilityDelta() > 0);
        // Landing, switching to a cheaper genome/copy and changing config cannot erase an already spent cooldown.
        grounded = true; jumps.tick(); kinetic(item, 0);
        grounded = false; velocity.setY(-.1); press(); runMovementTasks(); assertEquals(1, movementEvents.size());
        for (int i = 0; i < 83; i++) jumps.tick(); // tick 91: original deadline, not the cheaper genome's deadline
        press(); runMovementTasks(); assertEquals(2, movementEvents.size()); assertEquals(.315, velocity.getY(), 1e-12);
    }
    @Test void genomeChangesInsideAProtectionCallbackOrBeforeReceiptCannotTransferCredit() {
        var template = new AbilityRegistry().templateById("mobility.double_jump");
        var item = equip(EquipmentSlot.FEET, "diamond_boots", template);
        movement(); grounded = false;
        veto = e -> kinetic(item, 1); press(); runMovementTasks();
        assertEquals(-.1, velocity.getY(), 1e-12);
        assertTrue(movementOutcomes.stream().noneMatch(AbilityOutcomeObservation::liveMeasured));
        movementOutcomes.clear(); // A suppressed classification exists, but is not measured reproductive evidence.
        veto = e -> { }; press(); runMovementTasks(); assertEquals(.525, velocity.getY(), 1e-12);
        jumps.velocity(player, velocity, false); altitude += .7; kinetic(item, 0);
        for (int i = 0; i < 6; i++) jumps.tick();
        assertTrue(movementOutcomes.isEmpty(), "The old impulse is not evidence for a new genetic profile");
    }
    @Test void capturedConfigurationAndActualCostSurviveReloadDuringVetoCallback() {
        var template = new AbilityRegistry().templateById("mobility.double_jump");
        equip(EquipmentSlot.FEET, "diamond_boots", template); movement(); grounded = false;
        veto = e -> {
            var config = new org.bukkit.configuration.file.YamlConfiguration(); config.set("abilities.double-jump.enabled", true);
            config.set("abilities.double-jump.cooldown-ticks", 1); obtuseloot.config.RuntimeSettings.load(config);
        };
        press(); runMovementTasks(); jumps.velocity(player, velocity, false); altitude += .4;
        for (int i = 0; i < 6; i++) jumps.tick();
        assertEquals(60, movementOutcomes.getFirst().result().performance().recoveryTicks());
    }
    private void kinetic(Artifact item, double value) {
        var traits = new EnumMap<>(item.getGenome().traits());
        traits.put(obtuseloot.abilities.genome.GenomeTrait.KINETIC_BIAS, value);
        item.setGenome(new obtuseloot.abilities.genome.ArtifactGenome(item.getArtifactSeed(), traits));
    }
    @Test void shortFirstTickRiseIsMeasuredEvenWhenACeilingStopsTheNextFrame() {
        var template = new AbilityRegistry().templateById("mobility.double_jump");
        equip(EquipmentSlot.FEET, "diamond_boots", template); movement(); grounded = false;
        press(); runMovementTasks(); jumps.velocity(player, velocity, false);
        altitude += .1; jumps.tick(); // Brief clearance before a ceiling/normal gravity removes it.
        altitude -= .1;
        for (int i = 0; i < 5; i++) jumps.tick();
        assertEquals(1, movementOutcomes.size());
        assertEquals(.1, movementOutcomes.getFirst().result().observedAmount(), 1e-10);
    }
    private void dispatch(EquipmentAbilityService.Token token) {
        service.dispatch(player, token, inspect.trigger(), 1, "test", AbilityRuntimeContext.intentional(AbilitySource.OTHER), null);
    }
    private Artifact equip(EquipmentSlot slot, String type, AbilityTemplate template) {
        var a = EquipmentBindingsTest.artifact(owner, type); artifacts.registerItem(a);
        kinetic(a, .5); // Existing ownership tests exercise the configured baseline phenotype.
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
