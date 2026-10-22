package obtuseloot.abilities;

import obtuseloot.abilities.effects.WalkingSpeedEffect;
import obtuseloot.artifacts.*;
import obtuseloot.config.RuntimeSettings;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class PassiveEffectsTest {
    private final UUID holder = UUID.randomUUID();
    private final Map<NamespacedKey, AttributeModifier> modifiers = new LinkedHashMap<>();
    private RuntimeSettings.Snapshot previous;
    private GameMode mode = GameMode.SURVIVAL;
    private boolean gliding;
    private Player player;
    private AttributeInstance attribute;
    @BeforeEach void setUp() {
        previous = RuntimeSettings.get(); RuntimeSettings.load(new YamlConfiguration());
        attribute = api(AttributeInstance.class, (name, args) -> switch (name) {
            case "getModifiers" -> List.copyOf(modifiers.values());
            case "removeModifier" -> { modifiers.remove(args[0] instanceof AttributeModifier m ? m.getKey() : args[0]); yield null; }
            case "addTransientModifier" -> { var m = (AttributeModifier) args[0]; assertNull(modifiers.putIfAbsent(m.getKey(), m)); yield null; }
            default -> throw new AssertionError(name);
        });
        player = api(Player.class, (name, args) -> switch (name) {
            case "isOnline" -> true;
            case "isDead", "isFlying", "isInsideVehicle" -> false;
            case "isGliding" -> gliding;
            case "getGameMode" -> mode;
            case "getAttribute" -> attribute;
            default -> throw new AssertionError(name);
        });
    }
    @AfterEach void restore() throws Exception {
        Field f = RuntimeSettings.class.getDeclaredField("current"); f.setAccessible(true); f.set(null, previous);
    }
    @Test void strongestIsIdempotentAndRemovalPreservesForeignModifiers() {
        var foreign = new AttributeModifier(new NamespacedKey("other", "speed"), .2, AttributeModifier.Operation.ADD_SCALAR);
        modifiers.put(foreign.getKey(), foreign);
        var effect = new WalkingSpeedEffect(p -> attribute); var controller = controller();
        var first = request(effect, item()); var second = request(effect, item());
        controller.reconcile(holder, List.of(first, second));
        controller.reconcile(holder, List.of(first, second));
        assertEquals(2, modifiers.size()); assertEquals(.25, additiveSum(), 1e-9);
        controller.reconcile(holder, List.of(second)); assertEquals(2, modifiers.size());
        controller.clear(holder); assertEquals(Map.of(foreign.getKey(), foreign), modifiers);
        effect.cleanupPlayer(player); assertEquals(Map.of(foreign.getKey(), foreign), modifiers);
    }
    @Test void additiveBonusesSumAgainstBaseAndConfigSwitchReleasesOldMode() {
        var effect = new WalkingSpeedEffect(p -> attribute); var controller = controller(); var first = item(); var second = item();
        controller.reconcile(holder, List.of(request(effect, first), request(effect, second)));
        assertEquals(.05, additiveSum(), 1e-9);
        var config = new YamlConfiguration(); config.set("abilities.passive-speed.stacking", "additive"); RuntimeSettings.load(config);
        controller.reconcile(holder, List.of(request(effect, first), request(effect, second)));
        assertEquals(2, modifiers.size()); assertEquals(.10, additiveSum(), 1e-9);
        assertEquals(.11, .1 * (1 + additiveSum()), 1e-9);
        assertTrue(modifiers.values().stream().allMatch(m -> m.getOperation() == AttributeModifier.Operation.ADD_SCALAR));
        controller.clear(holder); assertTrue(modifiers.isEmpty());
    }
    @Test void flightAndGlidingDoNotAcquireSpeedOrFlightOwnership() {
        var effect = new WalkingSpeedEffect(p -> attribute); var item = item();
        mode = GameMode.CREATIVE; assertNull(effect.contribution(player, item, null));
        mode = GameMode.SPECTATOR; assertNull(effect.contribution(player, item, null));
        mode = GameMode.SURVIVAL; gliding = true; assertNull(effect.contribution(player, item, null));
        gliding = false; assertNotNull(effect.contribution(player, item, null));
    }
    @Test void failedReleaseRetainsOwnershipAndRetriesBeforeReplacement() {
        var failures = new ArrayList<RuntimeException>(); var attempts = new AtomicInteger(); var installs = new AtomicInteger();
        var controller = new PassiveEffectController(failures::add);
        var a = new PassiveAbilityEffect.Contribution("speed", .05, "a", () -> () -> {
            if (attempts.incrementAndGet() == 1) throw new IllegalStateException("test removal failure");
        });
        var b = new PassiveAbilityEffect.Contribution("speed", .1, "b", () -> { installs.incrementAndGet(); return () -> { }; });
        controller.reconcile(holder, List.of(new PassiveEffectController.Request("a", "speed", a)));
        controller.reconcile(holder, List.of(new PassiveEffectController.Request("b", "speed", b)));
        assertEquals(1, failures.size()); assertEquals(0, installs.get());
        controller.reconcile(holder, List.of(new PassiveEffectController.Request("b", "speed", b)));
        assertEquals(2, attempts.get()); assertEquals(1, installs.get());
    }
    @Test void invalidConfigDoesNotReplaceLastGoodSnapshot() {
        var before = RuntimeSettings.get(); var config = new YamlConfiguration();
        config.set("abilities.passive-speed.stacking", "multiplicative");
        assertThrows(IllegalArgumentException.class, () -> RuntimeSettings.load(config)); assertSame(before, RuntimeSettings.get());
        config.set("abilities.passive-speed.stacking", "additive"); config.set("abilities.passive-speed.bonus", Double.NaN);
        assertThrows(IllegalArgumentException.class, () -> RuntimeSettings.load(config)); assertSame(before, RuntimeSettings.get());
    }
    private double additiveSum() { return modifiers.values().stream().mapToDouble(AttributeModifier::getAmount).sum(); }
    private PassiveEffectController controller() { return new PassiveEffectController(e -> fail(e)); }
    private Artifact item() {
        var a = new Artifact(holder, "elytra"); a.setArtifactSeed(-1); a.setArtifactStorageKey(ArtifactStorageKeys.create()); return a;
    }
    private PassiveEffectController.Request request(WalkingSpeedEffect effect, Artifact item) {
        return new PassiveEffectController.Request(item.getArtifactStorageKey(), "mobility.fleetfoot", effect.contribution(player, item, null));
    }
    @FunctionalInterface private interface Call { Object call(String name, Object[] args); }
    private static <T> T api(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> switch (method.getName()) {
            case "toString" -> type.getSimpleName(); case "hashCode" -> System.identityHashCode(proxy); case "equals" -> proxy == args[0];
            default -> call.call(method.getName(), args);
        }));
    }
}
